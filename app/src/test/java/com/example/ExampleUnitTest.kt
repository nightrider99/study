package com.example

import com.example.data.model.FlashcardEntity
import com.example.data.repository.SrsEngine
import com.example.data.repository.SrsRating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testSm2Engine_AgainResetsRepetitions() {
        val initial = FlashcardEntity(
            deckId = "test_deck",
            front = "Front",
            back = "Back",
            repetitions = 4,
            intervalDays = 12,
            easeFactor = 2.5f
        )
        val reviewed = SrsEngine.calculateNextReview(initial, SrsRating.AGAIN)
        assertEquals(0, reviewed.repetitions)
        assertEquals(1, reviewed.intervalDays)
        assertEquals("LEARNING", reviewed.state)
    }

    @Test
    fun testSm2Engine_GoodIncrementsInterval() {
        val initial = FlashcardEntity(
            deckId = "test_deck",
            front = "Front",
            back = "Back",
            repetitions = 1,
            intervalDays = 3,
            easeFactor = 2.5f
        )
        val reviewed = SrsEngine.calculateNextReview(initial, SrsRating.GOOD)
        assertEquals(2, reviewed.repetitions)
        assertTrue(reviewed.intervalDays >= 3)
    }
}
