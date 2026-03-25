package com.example.sueca.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SuecaGameEngineTest {
    private val engine = SuecaGameEngine()

    @Test
    fun `must follow suit when possible`() {
        val hand = listOf(
            Card(Suit.HEARTS, Rank.ACE),
            Card(Suit.SPADES, Rank.TWO),
        )

        val legal = engine.legalCards(hand, Suit.HEARTS)

        assertEquals(1, legal.size)
        assertEquals(Suit.HEARTS, legal.first().suit)
        assertTrue(engine.canPlayCard(hand, Suit.HEARTS, hand.first()))
        assertTrue(!engine.canPlayCard(hand, Suit.HEARTS, hand.last()))
    }

    @Test
    fun `trump wins over higher lead suit`() {
        val result = engine.determineTrickWinner(
            listOf(
                PlayedCard(0, Card(Suit.HEARTS, Rank.ACE)),
                PlayedCard(1, Card(Suit.HEARTS, Rank.SEVEN)),
                PlayedCard(2, Card(Suit.SPADES, Rank.TWO)),
                PlayedCard(3, Card(Suit.HEARTS, Rank.KING)),
            ),
            trumpSuit = Suit.SPADES,
        )

        assertEquals(2, result.winnerIndex)
        assertEquals(25, result.pointsWon)
    }

    @Test
    fun `round wins scale with points`() {
        assertEquals(1, engine.roundWinsForPoints(90))
        assertEquals(2, engine.roundWinsForPoints(91))
        assertEquals(4, engine.roundWinsForPoints(120))
    }

    @Test
    fun `trump card is chosen from dealer hand and dealer rotates`() {
        val seededEngine = SuecaGameEngine(Random(1234))
        var state = seededEngine.startMatch()

        val firstDealer = state.startingPlayer
        val firstTrumpCard = state.trumpCard
        assertEquals(0, firstDealer)
        assertTrue(firstTrumpCard in state.playerHands[firstDealer])

        state = playRoundToEnd(seededEngine, state)
        state = seededEngine.nextRound(state)

        val secondDealer = state.startingPlayer
        val secondTrumpCard = state.trumpCard
        assertEquals((firstDealer + 1) % 4, secondDealer)
        assertTrue(secondTrumpCard in state.playerHands[secondDealer])
        assertNotEquals(firstDealer, secondDealer)
    }

    private fun playRoundToEnd(engine: SuecaGameEngine, state: SuecaUiState): SuecaUiState {
        var current = state
        while (!current.isRoundFinished) {
            if (current.isCollectingTrick) {
                current = engine.collectTrick(current)
                continue
            }
            if (current.currentPlayer == 0) {
                val hand = current.playerHands[0]
                val legalCards = engine.legalCards(hand, current.currentTrick.firstOrNull()?.card?.suit)
                current = engine.playHumanCard(current, legalCards.first())
            }
        }
        return current
    }
}
