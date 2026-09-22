package com.example.data.repository

import com.example.data.model.FlashcardEntity
import kotlin.math.max
import kotlin.math.min

enum class SrsRating(val label: String, val quality: Int) {
    AGAIN("Again", 1),
    HARD("Hard", 3),
    GOOD("Good", 4),
    EASY("Easy", 5)
}

object SrsEngine {
    private const val ONE_DAY_MS = 24L * 60L * 60L * 1000L

    fun calculateNextReview(card: FlashcardEntity, rating: SrsRating, now: Long = System.currentTimeMillis()): FlashcardEntity {
        val q = rating.quality
        var newRepetitions = card.repetitions
        var newInterval: Int
        var newEase = card.easeFactor
        val newState: String

        // SM-2 Ease Factor calculation: EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
        val efDelta = 0.1f - (5 - q) * (0.08f + (5 - q) * 0.02f)
        newEase = max(1.3f, min(3.0f, newEase + efDelta))

        when (rating) {
            SrsRating.AGAIN -> {
                newRepetitions = 0
                newInterval = 1
                newState = "LEARNING"
            }
            SrsRating.HARD -> {
                newRepetitions += 1
                newInterval = max(1, (card.intervalDays * 1.2f).toInt())
                newState = if (newRepetitions >= 3) "REVIEW" else "LEARNING"
            }
            SrsRating.GOOD -> {
                newInterval = when (newRepetitions) {
                    0 -> 1
                    1 -> 3
                    else -> max(1, (card.intervalDays * newEase).toInt())
                }
                newRepetitions += 1
                newState = if (newRepetitions >= 5) "MASTERED" else "REVIEW"
            }
            SrsRating.EASY -> {
                newInterval = when (newRepetitions) {
                    0 -> 2
                    1 -> 5
                    else -> max(1, (card.intervalDays * newEase * 1.3f).toInt())
                }
                newRepetitions += 1
                newState = if (newRepetitions >= 3) "MASTERED" else "REVIEW"
            }
        }

        val nextReview = now + (newInterval * ONE_DAY_MS)

        return card.copy(
            repetitions = newRepetitions,
            intervalDays = newInterval,
            easeFactor = newEase,
            nextReviewDate = nextReview,
            lastReviewedAt = now,
            state = newState
        )
    }
}
