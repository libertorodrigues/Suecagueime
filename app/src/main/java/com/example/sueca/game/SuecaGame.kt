package com.example.sueca.game

import kotlin.random.Random

enum class Suit(val label: String, val shortName: String) {
    CLUBS("Paus", "♣"),
    DIAMONDS("Ouros", "♦"),
    HEARTS("Copas", "♥"),
    SPADES("Espadas", "♠");
}

enum class Rank(
    val label: String,
    val displayValue: Int,
    val trickStrength: Int,
    val points: Int,
) {
    TWO("2", 2, 1, 0),
    THREE("3", 3, 2, 0),
    FOUR("4", 4, 3, 0),
    FIVE("5", 5, 4, 0),
    SIX("6", 6, 5, 0),
    QUEEN("D", 7, 8, 2),
    JACK("J", 8, 9, 3),
    KING("K", 9, 10, 4),
    SEVEN("7", 10, 11, 10),
    ACE("A", 11, 12, 11),
}

data class Card(val suit: Suit, val rank: Rank) {
    val displayName: String = "${rank.label}${suit.shortName}"
}

data class PlayedCard(val playerIndex: Int, val card: Card)

data class TrickResult(
    val winnerIndex: Int,
    val pointsWon: Int,
)

data class MatchScore(
    val teamPlayerPartner: Int = 0,
    val teamOpponents: Int = 0,
)

data class RoundScore(
    val teamPlayerPartner: Int = 0,
    val teamOpponents: Int = 0,
)

enum class AppScreen {
    MENU,
    GAME,
}

data class SuecaUiState(
    val screen: AppScreen = AppScreen.MENU,
    val playerHands: List<List<Card>> = List(4) { emptyList() },
    val currentTrick: List<PlayedCard> = emptyList(),
    val completedTricks: Int = 0,
    val currentPlayer: Int = 0,
    val startingPlayer: Int = 0,
    val trumpHolder: Int = 0,
    val trumpSuit: Suit? = null,
    val trickLeader: Int = 0,
    val lastTrickWinner: Int? = null,
    val roundScore: RoundScore = RoundScore(),
    val matchScore: MatchScore = MatchScore(),
    val roundNumber: Int = 1,
    val message: String = "",
    val isRoundFinished: Boolean = false,
    val isMatchFinished: Boolean = false,
    val winnerMessage: String? = null,
)

class SuecaGameEngine(
    private val random: Random = Random.Default,
) {
    private var matchScore = MatchScore()
    private var roundNumber = 1
    private var trumpHolder = 0

    fun initialState(): SuecaUiState = SuecaUiState()

    fun startMatch(): SuecaUiState {
        matchScore = MatchScore()
        roundNumber = 1
        trumpHolder = 0
        return startRound()
    }

    fun playHumanCard(state: SuecaUiState, card: Card): SuecaUiState {
        if (state.screen != AppScreen.GAME || state.isRoundFinished || state.isMatchFinished) return state
        if (state.currentPlayer != 0) return state

        val hand = state.playerHands[0]
        if (card !in hand) return state
        if (!canPlayCard(hand, state.currentTrick.firstOrNull()?.card?.suit, card)) return state

        var updatedState = playCard(state, 0, card)
        updatedState = resolveBotTurns(updatedState)
        return updatedState
    }

    fun nextRound(state: SuecaUiState): SuecaUiState {
        if (!state.isRoundFinished || state.isMatchFinished) return state
        trumpHolder = (trumpHolder + 1) % 4
        roundNumber += 1
        return startRound()
    }

    private fun startRound(): SuecaUiState {
        val deck = buildDeck().shuffled(random)
        val hands = List(4) { index ->
            deck.subList(index * 10, (index + 1) * 10).sortedWith(cardDisplayComparator())
        }
        val trumpSuit = hands[trumpHolder].last().suit
        val startingPlayer = trumpHolder
        var state = SuecaUiState(
            screen = AppScreen.GAME,
            playerHands = hands,
            currentPlayer = startingPlayer,
            startingPlayer = startingPlayer,
            trumpHolder = trumpHolder,
            trumpSuit = trumpSuit,
            trickLeader = startingPlayer,
            roundScore = RoundScore(),
            matchScore = matchScore,
            roundNumber = roundNumber,
            message = roundIntroMessage(trumpHolder, trumpSuit),
        )
        state = resolveBotTurns(state)
        return state
    }

    private fun resolveBotTurns(state: SuecaUiState): SuecaUiState {
        var currentState = state
        while (!currentState.isRoundFinished && !currentState.isMatchFinished && currentState.currentPlayer != 0) {
            val player = currentState.currentPlayer
            val hand = currentState.playerHands[player]
            val leadingSuit = currentState.currentTrick.firstOrNull()?.card?.suit
            val chosenCard = chooseBotCard(hand, leadingSuit)
            currentState = playCard(currentState, player, chosenCard)
        }
        return currentState
    }

    private fun chooseBotCard(hand: List<Card>, leadingSuit: Suit?): Card {
        val legalCards = legalCards(hand, leadingSuit)
        return legalCards.minWith(compareBy<Card>({ it.rank.trickStrength }, { it.suit.ordinal }))
    }

    private fun playCard(state: SuecaUiState, playerIndex: Int, card: Card): SuecaUiState {
        val updatedHands = state.playerHands.mapIndexed { index, cards ->
            if (index == playerIndex) cards.toMutableList().apply { remove(card) }.sortedWith(cardDisplayComparator()) else cards
        }
        val updatedTrick = state.currentTrick + PlayedCard(playerIndex, card)

        val nextPlayer = (playerIndex + 1) % 4
        var updatedState = state.copy(
            playerHands = updatedHands,
            currentTrick = updatedTrick,
            currentPlayer = nextPlayer,
            message = describePlay(playerIndex, card, state.trumpSuit),
        )

        if (updatedTrick.size == 4) {
            updatedState = finishTrick(updatedState)
        }

        return updatedState
    }

    private fun finishTrick(state: SuecaUiState): SuecaUiState {
        val result = determineTrickWinner(state.currentTrick, state.trumpSuit ?: error("Trump required"))
        val updatedRoundScore = updateRoundScore(state.roundScore, result.winnerIndex, result.pointsWon)
        val tricksCompleted = state.completedTricks + 1
        val allHandsEmpty = state.playerHands.all { it.isEmpty() }

        if (allHandsEmpty) {
            val updatedMatchScore = updateMatchScore(matchScore, updatedRoundScore)
            matchScore = updatedMatchScore
            val winningTeam = when {
                updatedMatchScore.teamPlayerPartner >= 4 -> "Jogador + Parceiro"
                updatedMatchScore.teamOpponents >= 4 -> "Bots adversários"
                else -> null
            }
            return state.copy(
                currentTrick = emptyList(),
                completedTricks = tricksCompleted,
                currentPlayer = result.winnerIndex,
                trickLeader = result.winnerIndex,
                lastTrickWinner = result.winnerIndex,
                roundScore = updatedRoundScore,
                matchScore = updatedMatchScore,
                message = roundEndMessage(updatedRoundScore, updatedMatchScore, winningTeam),
                isRoundFinished = true,
                isMatchFinished = winningTeam != null,
                winnerMessage = winningTeam?.let { "Vencedores da partida: $it" },
            )
        }

        return state.copy(
            currentTrick = emptyList(),
            completedTricks = tricksCompleted,
            currentPlayer = result.winnerIndex,
            trickLeader = result.winnerIndex,
            lastTrickWinner = result.winnerIndex,
            roundScore = updatedRoundScore,
            message = "Vaza ganha por ${playerName(result.winnerIndex)} (+${result.pointsWon} pontos).",
        )
    }

    private fun updateRoundScore(score: RoundScore, winnerIndex: Int, pointsWon: Int): RoundScore {
        return if (winnerIndex % 2 == 0) {
            score.copy(teamPlayerPartner = score.teamPlayerPartner + pointsWon)
        } else {
            score.copy(teamOpponents = score.teamOpponents + pointsWon)
        }
    }

    private fun updateMatchScore(matchScore: MatchScore, roundScore: RoundScore): MatchScore {
        return if (roundScore.teamPlayerPartner >= roundScore.teamOpponents) {
            matchScore.copy(teamPlayerPartner = matchScore.teamPlayerPartner + 1)
        } else {
            matchScore.copy(teamOpponents = matchScore.teamOpponents + 1)
        }
    }

    fun determineTrickWinner(cards: List<PlayedCard>, trumpSuit: Suit): TrickResult {
        val leadSuit = cards.first().card.suit
        val winningCard = cards.maxWith(
            compareBy<PlayedCard> {
                when {
                    it.card.suit == trumpSuit -> 2
                    it.card.suit == leadSuit -> 1
                    else -> 0
                }
            }.thenBy { it.card.rank.trickStrength }
        )
        return TrickResult(
            winnerIndex = winningCard.playerIndex,
            pointsWon = cards.sumOf { it.card.rank.points },
        )
    }

    fun legalCards(hand: List<Card>, leadingSuit: Suit?): List<Card> {
        if (leadingSuit == null) return hand
        val matchingSuit = hand.filter { it.suit == leadingSuit }
        return if (matchingSuit.isNotEmpty()) matchingSuit else hand
    }

    fun canPlayCard(hand: List<Card>, leadingSuit: Suit?, card: Card): Boolean {
        return card in legalCards(hand, leadingSuit)
    }

    private fun buildDeck(): List<Card> = Suit.entries.flatMap { suit ->
        Rank.entries.map { rank -> Card(suit, rank) }
    }

    private fun cardDisplayComparator(): Comparator<Card> =
        compareBy<Card>({ it.suit.ordinal }, { it.rank.displayValue })

    private fun playerName(index: Int): String = when (index) {
        0 -> "Tu"
        1 -> "Bot à esquerda"
        2 -> "Teu parceiro"
        else -> "Bot à direita"
    }

    private fun describePlay(playerIndex: Int, card: Card, trumpSuit: Suit?): String {
        val trumpText = trumpSuit?.let { " | Trunfo: ${it.label}" } ?: ""
        return "${playerName(playerIndex)} jogou ${card.displayName}$trumpText"
    }

    private fun roundIntroMessage(trumpHolder: Int, trumpSuit: Suit): String {
        return "Jogo $roundNumber: trunfo de ${playerName(trumpHolder)} em ${trumpSuit.label}."
    }

    private fun roundEndMessage(
        roundScore: RoundScore,
        matchScore: MatchScore,
        winningTeam: String?,
    ): String {
        val roundWinner = if (roundScore.teamPlayerPartner >= roundScore.teamOpponents) {
            "Jogador + Parceiro"
        } else {
            "Bots adversários"
        }
        val summary = "Fim do jogo $roundNumber: $roundWinner venceu ${roundScore.teamPlayerPartner}-${roundScore.teamOpponents}."
        val matchSummary = " Marcador da partida: ${matchScore.teamPlayerPartner}-${matchScore.teamOpponents}."
        val winnerSummary = winningTeam?.let { " $it venceu a partida à melhor de 7." } ?: ""
        return summary + matchSummary + winnerSummary
    }
}
