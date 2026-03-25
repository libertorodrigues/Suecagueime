package com.example.sueca

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card as MaterialCard
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.sp
import com.example.sueca.game.AppScreen
import com.example.sueca.game.Card as GameCard
import com.example.sueca.game.PlayedCard
import com.example.sueca.game.SuecaGameEngine
import com.example.sueca.game.SuecaUiState
import com.example.sueca.game.Suit
import com.example.sueca.ui.theme.SuecaTheme

private val ScreenBackground = Color(0xFF123524)
private val TableBackground = Color(0xFF1B5E20)
private val PanelBackground = Color(0xFFF8FAFC)
private val PanelBorder = Color(0xFFD9E2EC)
private val PrimaryText = Color(0xFF102A43)
private val SecondaryText = Color(0xFF334E68)
private val AccentGold = Color(0xFFFFC857)
private val TeamPlayerColor = Color(0xFF1565C0)
private val TeamBotsColor = Color(0xFFC62828)
private val SuccessColor = Color(0xFF2E7D32)

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
            .background(ScreenBackground)
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
    val legalCards = remember(state.playerHands, state.currentTrick) {
        SuecaGameEngine().legalCards(state.playerHands[0], state.currentTrick.firstOrNull()?.card?.suit)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TopScoreboard(state)
        TableArea(
            state = state,
            modifier = Modifier.weight(if (state.isRoundFinished || state.isMatchFinished) 1f else 1.3f),
        )
        RoundInfoBar(state)
        HumanHand(
            state = state,
            legalCards = legalCards,
            onPlayCard = onPlayCard,
        )
        if (state.isRoundFinished || state.isMatchFinished) {
            ActionArea(state = state, onNextRound = onNextRound, onRestart = onRestart)
        }
    }
}

@Composable
private fun TopScoreboard(state: SuecaUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ScoreCard(
            modifier = Modifier.weight(1f),
            title = "Jogo ${state.roundNumber}",
            subtitle = "Vazas ${state.completedTricks}/10",
            value = "${state.matchScore.teamPlayerPartner} - ${state.matchScore.teamOpponents}",
            footer = "Partida · Nós vs Bots",
        )
        TeamPointsCard(
            modifier = Modifier.weight(1f),
            teamName = "Nós",
            points = state.roundScore.teamPlayerPartner,
            accent = TeamPlayerColor,
            extra = "Parceiro incluído",
        )
        TeamPointsCard(
            modifier = Modifier.weight(1f),
            teamName = "Bots",
            points = state.roundScore.teamOpponents,
            accent = TeamBotsColor,
            extra = "Equipa adversária",
        )
    }
}

@Composable
private fun ScoreCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    value: String,
    footer: String,
) {
    InfoPanel(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, color = PrimaryText, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = SecondaryText, fontSize = 12.sp)
            Text(text = value, color = PrimaryText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(text = footer, color = SecondaryText, fontSize = 12.sp)
        }
    }
}

@Composable
private fun TeamPointsCard(
    modifier: Modifier = Modifier,
    teamName: String,
    points: Int,
    accent: Color,
    extra: String,
) {
    InfoPanel(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = teamName, color = accent, fontWeight = FontWeight.Bold)
            Text(
                text = points.toString(),
                color = PrimaryText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(text = "Pontos no jogo atual", color = SecondaryText, fontSize = 12.sp)
            Text(text = extra, color = SecondaryText, fontSize = 11.sp)
        }
    }
}

@Composable
private fun TableArea(state: SuecaUiState, modifier: Modifier = Modifier) {
    val playedCards = state.currentTrick.associateBy { it.playerIndex }
    val playOrder = state.currentTrick.mapIndexed { index, playedCard -> playedCard.playerIndex to (index + 1) }.toMap()

    InfoPanel(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TableBackground, RoundedCornerShape(24.dp))
                .padding(12.dp),
        ) {
            PlayerBadge(
                name = "Parceiro",
                handSize = state.playerHands[2].size,
                isCurrent = state.currentPlayer == 2,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            PlayerBadge(
                name = "Esquerda",
                handSize = state.playerHands[1].size,
                isCurrent = state.currentPlayer == 1,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            PlayerBadge(
                name = "Direita",
                handSize = state.playerHands[3].size,
                isCurrent = state.currentPlayer == 3,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
            PlayerBadge(
                name = "Tu",
                handSize = state.playerHands[0].size,
                isCurrent = state.currentPlayer == 0,
                modifier = Modifier.align(Alignment.BottomCenter),
            )

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.86f)
                    .fillMaxHeight(0.84f)
                    .background(Color(0xFF2E7D32), RoundedCornerShape(26.dp)),
            ) {
                playedCards[2]?.let {
                    TrickSeatCard(order = playOrder[it.playerIndex] ?: 0, playedCard = it, modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp))
                }
                playedCards[1]?.let {
                    TrickSeatCard(order = playOrder[it.playerIndex] ?: 0, playedCard = it, modifier = Modifier.align(Alignment.CenterStart).padding(start = 18.dp))
                }
                playedCards[3]?.let {
                    TrickSeatCard(order = playOrder[it.playerIndex] ?: 0, playedCard = it, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 18.dp))
                }
                playedCards[0]?.let {
                    TrickSeatCard(order = playOrder[it.playerIndex] ?: 0, playedCard = it, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp))
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (state.currentTrick.isEmpty()) {
                        Text(
                            text = "A jogada aparecerá aqui por ordem.",
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                    } else {
                        state.currentTrick.forEachIndexed { index, playedCard ->
                            TrickTimelineCard(
                                playedCard = playedCard,
                                order = index + 1,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerBadge(
    name: String,
    handSize: Int,
    isCurrent: Boolean,
    modifier: Modifier = Modifier,
) {
    MaterialCard(
        modifier = modifier.widthIn(min = 84.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) AccentGold else Color.White.copy(alpha = 0.94f),
        ),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = name, color = PrimaryText, fontWeight = FontWeight.Bold)
            Text(text = "$handSize cartas", color = SecondaryText, fontSize = 12.sp)
        }
    }
}

@Composable
private fun TrickSeatCard(order: Int, playedCard: PlayedCard, modifier: Modifier = Modifier) {

    Box(modifier = modifier) {
        MaterialCard(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF2)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.55f)),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(text = seatName(playedCard.playerIndex), color = PrimaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = playedCard.card.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = suitColor(playedCard.card.suit),
                )
                Text(text = "Ordem $order", color = SecondaryText, fontSize = 11.sp)
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(22.dp)
                .background(AccentGold, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = order.toString(), color = PrimaryText, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun TrickTimelineCard(
    playedCard: PlayedCard,
    order: Int,
    modifier: Modifier = Modifier,
) {
    MaterialCard(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF2)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = "${order}º", color = SecondaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                text = playedCard.card.displayName,
                color = suitColor(playedCard.card.suit),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
            )
            Text(text = seatName(playedCard.playerIndex), color = SecondaryText, fontSize = 10.sp)
        }
    }
}

@Composable
private fun RoundInfoBar(state: SuecaUiState) {
    val winnerColor = if (state.winnerMessage != null) SuccessColor else PrimaryText
    InfoPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Trunfo: ${state.trumpSuit?.label ?: "-"} · Turno: ${seatName(state.currentPlayer)}",
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold,
                )
                Text(text = state.message, color = SecondaryText)
                state.winnerMessage?.let {
                    Text(text = it, color = winnerColor, fontWeight = FontWeight.ExtraBold)
                }
            }
            if (state.lastTrickWinner != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Última vaza", color = SecondaryText, fontSize = 12.sp)
                    Text(text = seatName(state.lastTrickWinner), color = PrimaryText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HumanHand(
    state: SuecaUiState,
    legalCards: List<GameCard>,
    onPlayCard: (GameCard) -> Unit,
) {
    InfoPanel {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "As tuas cartas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryText)
                Text(text = "Ordenadas por naipe", color = SecondaryText, fontSize = 12.sp)
            }
            CompactHandRow(
                cards = state.playerHands[0],
                legalCards = legalCards,
                enabled = state.currentPlayer == 0 && !state.isRoundFinished && !state.isMatchFinished,
                onPlayCard = onPlayCard,
            )
        }
    }
}

@Composable
private fun CompactHandRow(
    cards: List<GameCard>,
    legalCards: List<GameCard>,
    enabled: Boolean,
    onPlayCard: (GameCard) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val spacing = 4.dp
        val cardCount = cards.size.coerceAtLeast(1)
        val cardWidth = ((maxWidth - spacing * (cardCount - 1)) / cardCount).coerceIn(28.dp, 42.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
            cards.forEach { card ->
                val canPlay = enabled && card in legalCards
                PlayingCard(
                    card = card,
                    enabled = canPlay,
                    modifier = Modifier.width(cardWidth).aspectRatio(0.62f),
                    onClick = { onPlayCard(card) },
                )
            }
        }
    }
}

@Composable
private fun PlayingCard(
    card: GameCard,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    MaterialCard(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) Color.White else Color(0xFFE7ECF2),
            disabledContainerColor = Color(0xFFE7ECF2),
        ),
        border = BorderStroke(
            width = if (enabled) 2.dp else 1.dp,
            color = if (enabled) AccentGold else PanelBorder,
        ),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = card.rank.label,
                color = suitColor(card.suit),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
            )
            Text(
                text = card.suit.shortName,
                color = suitColor(card.suit),
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = card.rank.label,
                color = suitColor(card.suit),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ActionArea(state: SuecaUiState, onNextRound: () -> Unit, onRestart: () -> Unit) {
    InfoPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                when {
                    state.isMatchFinished -> Text("A partida terminou. Podes começar outra.", color = PrimaryText, fontWeight = FontWeight.Bold)
                    state.isRoundFinished -> Text("Este jogo terminou. Avança para o próximo.", color = PrimaryText, fontWeight = FontWeight.Bold)
                    state.currentPlayer == 0 -> Text("É a tua vez: toca numa carta destacada a dourado.", color = PrimaryText, fontWeight = FontWeight.Bold)
                    else -> Text("Os bots jogam automaticamente até voltar a tua vez.", color = PrimaryText, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = if (state.currentPlayer == 0 || state.isRoundFinished) {
                        "Objetivo: manter toda a informação visível sem precisar de scroll."
                    } else {
                        "A mesa central mostra quem já jogou e a ordem da vaza."
                    },
                    color = SecondaryText,
                    fontSize = 12.sp,
                )
            }

            when {
                state.isRoundFinished && !state.isMatchFinished -> Button(onClick = onNextRound) { Text("Próximo jogo") }
                state.isMatchFinished -> Button(onClick = onRestart) { Text("Nova partida") }
                else -> Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

@Composable
private fun InfoPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    MaterialCard(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = PanelBackground),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, PanelBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
    Suit.CLUBS, Suit.SPADES -> Color(0xFF102A43)
}

@Preview(showBackground = true)
@Composable
private fun MenuPreview() {
    SuecaTheme {
        MainMenu(onPlayBots = {})
    }
}
