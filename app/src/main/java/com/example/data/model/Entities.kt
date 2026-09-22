package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val summary: String? = null,
    val keyPointsJson: String? = null,
    val subject: String = "General",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "flashcard_decks")
data class FlashcardDeckEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val subject: String = "General",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "flashcards",
    foreignKeys = [
        ForeignKey(
            entity = FlashcardDeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("deckId")]
)
data class FlashcardEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val deckId: String,
    val front: String,
    val back: String,
    val hint: String? = null,
    // Spaced Repetition (SM-2 Algorithm parameters)
    val repetitions: Int = 0,
    val intervalDays: Int = 1,
    val easeFactor: Float = 2.5f,
    val nextReviewDate: Long = System.currentTimeMillis(),
    val lastReviewedAt: Long? = null,
    val state: String = "NEW" // "NEW", "LEARNING", "REVIEW", "MASTERED"
)

@Entity(tableName = "quizzes")
data class QuizEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val topic: String = "General",
    val questionCount: Int = 5,
    val bestScore: Int? = null,
    val totalAttempts: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "quiz_questions",
    foreignKeys = [
        ForeignKey(
            entity = QuizEntity::class,
            parentColumns = ["id"],
            childColumns = ["quizId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("quizId")]
)
data class QuizQuestionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val quizId: String,
    val questionText: String,
    val optionsJson: String, // JSON array string e.g. ["Option A", "Option B", ...]
    val correctIndex: Int,
    val explanation: String = "",
    val orderIndex: Int = 0
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sender: String, // "USER" or "AI"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_progress")
data class StudyProgressEntity(
    @PrimaryKey val dateKey: String, // e.g. "2026-09-22"
    val cardsReviewed: Int = 0,
    val quizzesCompleted: Int = 0,
    val notesCreated: Int = 0,
    val studyMinutes: Int = 0,
    val lastActiveTimestamp: Long = System.currentTimeMillis()
)
