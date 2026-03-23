package com.example.sueca

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card as MaterialCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sueca.game.AppScreen
import com.example.sueca.game.Card as GameCard
import com.example.sueca.game.PlayedCard
import com.example.sueca.game.SuecaGameEngine
import com.example.sueca.game.SuecaUiState
import com.example.sueca.game.Suit
import com.example.sueca.ui.theme.SuecaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SuecaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SuecaApp()
                }
            }
        }
    }
}

@Composable
fun SuecaApp() {
    val engine = remember { SuecaGameEngine() }
    var state by remember { mutableStateOf(engine.initialState()) }

    when (state.screen) {
        AppScreen.MENU -> MainMenu(onPlayBots = { state = engine.startMatch() })
        AppScreen.GAME -> GameScreen(
            state = state,
            onPlayCard = { card -> state = engine.playHumanCard(state, card) },
            onNextRound = { state = engine.nextRound(state) },
            onRestart = { state = engine.startMatch() },
        )
    }
}

@Composable
private fun MainMenu(onPlayBots: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF073B1A))
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Sueca",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Joga contra 3 bots. Ganha 4 jogos primeiro para venceres a partida.",
                textAlign = TextAlign.Center,
                color = Color.White,
            )
            Button(onClick = onPlayBots) {
                Text("Jogar com bots")
            }
        }
    }
}

@Composable
private fun GameScreen(
    state: SuecaUiState,
    onPlayCard: (GameCard) -> Unit,
    onNextRound: () -> Unit,
    onRestart: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B4F26))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Sueca - Jogo ${state.roundNumber}",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )

        MatchStatus(state)
        TablePlayers(state)
        TrickArea(state.currentTrick)
        HumanHand(state = state, onPlayCard = onPlayCard)
        ActionArea(state = state, onNextRound = onNextRound, onRestart = onRestart)
    }
}

@Composable
private fun MatchStatus(state: SuecaUiState) {
    InfoPanel {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Trunfo: ${state.trumpSuit?.label ?: "-"} (dono: ${seatName(state.trumpHolder)})",
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = "Turno: ${seatName(state.currentPlayer)}")
            Text(text = "Vazas concluídas: ${state.completedTricks}/10")
            Text(text = "Jogo atual: Tu+Parceiro ${state.roundScore.teamPlayerPartner} • Bots ${state.roundScore.teamOpponents}")
            Text(text = "Partida: Tu+Parceiro ${state.matchScore.teamPlayerPartner} • Bots ${state.matchScore.teamOpponents}")
            Text(text = state.message)
            state.winnerMessage?.let {
                Text(text = it, fontWeight = FontWeight.Bold, color = Color(0xFF8BC34A))
            }
        }
    }
}

@Composable
private fun TablePlayers(state: SuecaUiState) {
    InfoPanel {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PlayerSummary(name = "Bot à esquerda", handSize = state.playerHands[1].size, isCurrent = state.currentPlayer == 1)
            PlayerSummary(name = "Teu parceiro", handSize = state.playerHands[2].size, isCurrent = state.currentPlayer == 2)
            PlayerSummary(name = "Bot à direita", handSize = state.playerHands[3].size, isCurrent = state.currentPlayer == 3)
        }
    }
}

@Composable
private fun PlayerSummary(name: String, handSize: Int, isCurrent: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = name, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal)
        Text(text = "$handSize cartas")
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun TrickArea(cards: List<PlayedCard>) {
    InfoPanel {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Mesa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (cards.isEmpty()) {
                Text("Nenhuma carta na mesa.")
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    cards.forEach { playedCard ->
                        MiniPlayedCard(playedCard)
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun MiniPlayedCard(playedCard: PlayedCard) {
    MaterialCard(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = seatName(playedCard.playerIndex), style = MaterialTheme.typography.labelMedium)
            Text(
                text = playedCard.card.displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = suitColor(playedCard.card.suit),
            )
        }
    }
}

@Composable
private fun HumanHand(state: SuecaUiState, onPlayCard: (GameCard) -> Unit) {
    val legalCards = SuecaGameEngine().legalCards(state.playerHands[0], state.currentTrick.firstOrNull()?.card?.suit)

    InfoPanel {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "As tuas cartas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Suit.entries.forEach { suit ->
                val suitCards = state.playerHands[0].filter { it.suit == suit }
                if (suitCards.isNotEmpty()) {
                    Text(text = suit.label, color = suitColor(suit), fontWeight = FontWeight.SemiBold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                    ) {
                        items(suitCards) { card ->
                            val canPlay = state.currentPlayer == 0 && !state.isRoundFinished && !state.isMatchFinished &&
                                card in legalCards
                            PlayingCard(card = card, enabled = canPlay, onClick = { onPlayCard(card) })
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0x33222222))
                }
            }
        }
    }
}

@Composable
private fun PlayingCard(card: GameCard, enabled: Boolean, onClick: () -> Unit) {
    MaterialCard(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) Color.White else Color(0xFFDDDDDD),
        ),
        modifier = Modifier.size(width = 72.dp, height = 108.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = card.rank.label, color = suitColor(card.suit), fontWeight = FontWeight.Bold)
            Text(
                text = card.suit.shortName,
                color = suitColor(card.suit),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = card.rank.label,
                color = suitColor(card.suit),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ActionArea(state: SuecaUiState, onNextRound: () -> Unit, onRestart: () -> Unit) {
    InfoPanel {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.isRoundFinished && !state.isMatchFinished) {
                Button(onClick = onNextRound, modifier = Modifier.fillMaxWidth()) {
                    Text("Próximo jogo")
                }
            }
            if (state.isMatchFinished) {
                Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
                    Text("Nova partida")
                }
            }
            if (!state.isRoundFinished && state.currentPlayer != 0) {
                Text("Os bots estão a jogar automaticamente.")
            }
            if (!state.isRoundFinished && state.currentPlayer == 0) {
                Text("Escolhe uma carta válida para jogar.")
            }
        }
    }
}

@Composable
private fun InfoPanel(content: @Composable () -> Unit) {
    MaterialCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

private fun seatName(index: Int): String = when (index) {
    0 -> "Tu"
    1 -> "Esquerda"
    2 -> "Parceiro"
    else -> "Direita"
}

private fun suitColor(suit: Suit): Color = when (suit) {
    Suit.HEARTS, Suit.DIAMONDS -> Color(0xFFB71C1C)
    Suit.CLUBS, Suit.SPADES -> Color(0xFF1B1B1B)
}

@Preview(showBackground = true)
@Composable
private fun MenuPreview() {
    SuecaTheme {
        MainMenu(onPlayBots = {})
    }
}
