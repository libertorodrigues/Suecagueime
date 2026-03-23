package com.example.sueca.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
