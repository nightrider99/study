package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.ChatMessageEntity
import com.example.data.model.FlashcardDeckEntity
import com.example.data.model.FlashcardEntity
import com.example.data.model.NoteEntity
import com.example.data.model.QuizEntity
import com.example.data.model.QuizQuestionEntity
import com.example.data.model.StudyProgressEntity
import com.example.data.remote.GeminiClient
import com.example.data.remote.GeneratedCard
import com.example.data.remote.GeneratedQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class StudyRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val noteDao = db.noteDao()
    private val flashcardDao = db.flashcardDao()
    private val quizDao = db.quizDao()
    private val chatDao = db.chatDao()
    private val progressDao = db.progressDao()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun todayKey(): String = dateFormat.format(Date())

    // --- Notes ---
    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()

    suspend fun getNoteById(id: String): NoteEntity? = withContext(Dispatchers.IO) {
        noteDao.getNoteById(id)
    }

    suspend fun saveNote(note: NoteEntity) = withContext(Dispatchers.IO) {
        noteDao.insertNote(note)
        recordStudyActivity(notesAdded = 1)
    }

    suspend fun deleteNote(id: String) = withContext(Dispatchers.IO) {
        noteDao.deleteNoteById(id)
    }

    suspend fun summarizeNoteWithAi(noteId: String): Result<NoteEntity> = withContext(Dispatchers.IO) {
        val note = noteDao.getNoteById(noteId) ?: return@withContext Result.failure(Exception("Note not found"))
        val result = GeminiClient.summarizeNote(note.title, note.content)
        result.mapCatching { (summary, keyPoints) ->
            val pointsJson = JSONArray(keyPoints).toString()
            val updated = note.copy(
                summary = summary,
                keyPointsJson = pointsJson,
                updatedAt = System.currentTimeMillis()
            )
            noteDao.insertNote(updated)
            updated
        }
    }

    // --- Flashcards & SRS ---
    val allDecks: Flow<List<FlashcardDeckEntity>> = flashcardDao.getAllDecks()
    val dueCards: Flow<List<FlashcardEntity>> = flashcardDao.getAllDueCards(System.currentTimeMillis())
    val totalCardCount: Flow<Int> = flashcardDao.getTotalCardCount()

    fun getCardsForDeck(deckId: String): Flow<List<FlashcardEntity>> = flashcardDao.getCardsForDeck(deckId)

    suspend fun getDueCardsForDeck(deckId: String): List<FlashcardEntity> = withContext(Dispatchers.IO) {
        flashcardDao.getDueCardsForDeck(deckId, System.currentTimeMillis())
    }

    suspend fun getDeckById(deckId: String): FlashcardDeckEntity? = withContext(Dispatchers.IO) {
        flashcardDao.getDeckById(deckId)
    }

    suspend fun getCardsForDeckOnce(deckId: String): List<FlashcardEntity> = withContext(Dispatchers.IO) {
        flashcardDao.getCardsForDeckOnce(deckId)
    }

    suspend fun createDeck(title: String, description: String, subject: String): FlashcardDeckEntity = withContext(Dispatchers.IO) {
        val deck = FlashcardDeckEntity(
            title = title,
            description = description,
            subject = subject
        )
        flashcardDao.insertDeck(deck)
        deck
    }

    suspend fun deleteDeck(deck: FlashcardDeckEntity) = withContext(Dispatchers.IO) {
        flashcardDao.deleteDeck(deck)
    }

    suspend fun addCardToDeck(deckId: String, front: String, back: String, hint: String? = null): FlashcardEntity = withContext(Dispatchers.IO) {
        val card = FlashcardEntity(
            deckId = deckId,
            front = front,
            back = back,
            hint = hint
        )
        flashcardDao.insertCard(card)
        card
    }

    suspend fun reviewCard(card: FlashcardEntity, rating: SrsRating): FlashcardEntity = withContext(Dispatchers.IO) {
        val updatedCard = SrsEngine.calculateNextReview(card, rating)
        flashcardDao.updateCard(updatedCard)
        recordStudyActivity(cardsReviewedDelta = 1, minutesDelta = 1)
        updatedCard
    }

    suspend fun generateFlashcardsWithAi(
        deckTitle: String,
        topic: String,
        context: String? = null,
        count: Int = 6,
        existingDeckId: String? = null
    ): Result<FlashcardDeckEntity> = withContext(Dispatchers.IO) {
        val aiResult = GeminiClient.generateFlashcards(topic, context, count)
        aiResult.mapCatching { cards ->
            val deckId = existingDeckId ?: run {
                val newDeck = FlashcardDeckEntity(
                    title = deckTitle,
                    description = "AI generated deck on $topic",
                    subject = topic.split(" ").firstOrNull() ?: "General"
                )
                flashcardDao.insertDeck(newDeck)
                newDeck.id
            }

            val cardEntities = cards.map { c ->
                FlashcardEntity(
                    deckId = deckId,
                    front = c.front,
                    back = c.back,
                    hint = c.hint
                )
            }
            flashcardDao.insertCards(cardEntities)
            flashcardDao.getDeckById(deckId) ?: FlashcardDeckEntity(id = deckId, title = deckTitle)
        }
    }

    // --- Quizzes ---
    val allQuizzes: Flow<List<QuizEntity>> = quizDao.getAllQuizzes()

    suspend fun getQuizById(quizId: String): QuizEntity? = withContext(Dispatchers.IO) {
        quizDao.getQuizById(quizId)
    }

    suspend fun getQuestionsForQuiz(quizId: String): List<QuizQuestionEntity> = withContext(Dispatchers.IO) {
        quizDao.getQuestionsForQuiz(quizId)
    }

    suspend fun recordQuizAttempt(quizId: String, scorePercentage: Int) = withContext(Dispatchers.IO) {
        val quiz = quizDao.getQuizById(quizId) ?: return@withContext
        val newBest = maxOf(quiz.bestScore ?: 0, scorePercentage)
        val updated = quiz.copy(
            bestScore = newBest,
            totalAttempts = quiz.totalAttempts + 1
        )
        quizDao.updateQuiz(updated)
        recordStudyActivity(quizzesCompletedDelta = 1, minutesDelta = 5)
    }

    suspend fun deleteQuiz(quizId: String) = withContext(Dispatchers.IO) {
        quizDao.deleteQuiz(quizId)
    }

    suspend fun generateQuizWithAi(
        title: String,
        topic: String,
        context: String? = null,
        count: Int = 5
    ): Result<QuizEntity> = withContext(Dispatchers.IO) {
        val aiResult = GeminiClient.generateQuiz(topic, context, count)
        aiResult.mapCatching { questions ->
            val quiz = QuizEntity(
                title = title,
                topic = topic,
                questionCount = questions.size
            )
            quizDao.insertQuiz(quiz)

            val questionEntities = questions.mapIndexed { index, q ->
                QuizQuestionEntity(
                    quizId = quiz.id,
                    questionText = q.questionText,
                    optionsJson = JSONArray(q.options).toString(),
                    correctIndex = q.correctIndex,
                    explanation = q.explanation,
                    orderIndex = index
                )
            }
            quizDao.insertQuestions(questionEntities)
            quiz
        }
    }

    // --- Chat Tutor ---
    val chatMessages: Flow<List<ChatMessageEntity>> = chatDao.getAllMessages()

    suspend fun sendChatMessage(userText: String): Result<String> = withContext(Dispatchers.IO) {
        // Save user message
        val userMsg = ChatMessageEntity(
            sender = "USER",
            content = userText
        )
        chatDao.insertMessage(userMsg)

        // Gather history
        val messages = chatDao.getAllMessages().firstOrNull() ?: emptyList()
        val historyPairs = messages.map { it.sender to it.content }

        // Call Gemini
        val result = GeminiClient.chat(historyPairs, userText)
        result.onSuccess { reply ->
            val aiMsg = ChatMessageEntity(
                sender = "AI",
                content = reply
            )
            chatDao.insertMessage(aiMsg)
            recordStudyActivity(minutesDelta = 2)
        }
    }

    suspend fun clearChatHistory() = withContext(Dispatchers.IO) {
        chatDao.clearHistory()
    }

    // --- Progress & Streaks ---
    val recentProgress: Flow<List<StudyProgressEntity>> = progressDao.getRecentProgress()

    private suspend fun recordStudyActivity(
        cardsReviewedDelta: Int = 0,
        quizzesCompletedDelta: Int = 0,
        notesAdded: Int = 0,
        minutesDelta: Int = 0
    ) {
        val today = todayKey()
        val current = progressDao.getProgressForDate(today) ?: StudyProgressEntity(dateKey = today)
        val updated = current.copy(
            cardsReviewed = current.cardsReviewed + cardsReviewedDelta,
            quizzesCompleted = current.quizzesCompleted + quizzesCompletedDelta,
            notesCreated = current.notesCreated + notesAdded,
            studyMinutes = current.studyMinutes + minutesDelta,
            lastActiveTimestamp = System.currentTimeMillis()
        )
        progressDao.insertOrUpdateProgress(updated)
    }

    suspend fun calculateStreak(): Int = withContext(Dispatchers.IO) {
        val progressList = progressDao.getAllProgress().firstOrNull() ?: emptyList()
        if (progressList.isEmpty()) return@withContext 0

        var streak = 0
        val cal = java.util.Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Check if studied today
        val todayStr = sdf.format(cal.time)
        val hasStudiedToday = progressList.any { it.dateKey == todayStr && (it.cardsReviewed > 0 || it.quizzesCompleted > 0 || it.studyMinutes > 0) }

        if (!hasStudiedToday) {
            // Check yesterday
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = sdf.format(cal.time)
            val hasStudiedYesterday = progressList.any { it.dateKey == yesterdayStr && (it.cardsReviewed > 0 || it.quizzesCompleted > 0 || it.studyMinutes > 0) }
            if (!hasStudiedYesterday) return@withContext 0
        } else {
            streak++
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        }

        while (true) {
            val dateStr = sdf.format(cal.time)
            val dayRecord = progressList.firstOrNull { it.dateKey == dateStr }
            if (dayRecord != null && (dayRecord.cardsReviewed > 0 || dayRecord.quizzesCompleted > 0 || dayRecord.studyMinutes > 0)) {
                streak++
                cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        streak
    }

    // --- Preload / Seed Initial Content ---
    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val existingDecks = flashcardDao.getAllDecks().firstOrNull()
        if (!existingDecks.isNullOrEmpty()) return@withContext

        // Seed 1st Deck: Cognitive Psychology & Active Recall
        val deck1 = FlashcardDeckEntity(
            title = "Cognitive Science & Memory",
            description = "High-yield concepts for learning optimization and brain retention",
            subject = "Psychology"
        )
        flashcardDao.insertDeck(deck1)
        flashcardDao.insertCards(
            listOf(
                FlashcardEntity(
                    deckId = deck1.id,
                    front = "What is the Spacing Effect in cognitive learning?",
                    back = "The phenomenon whereby learning is greater when studying is spread out over time, as opposed to studying the same amount of time in a single session (cramming).",
                    hint = "Opposite of cramming before exams",
                    intervalDays = 1,
                    state = "NEW"
                ),
                FlashcardEntity(
                    deckId = deck1.id,
                    front = "How does the Testing Effect (Retrieval Practice) boost long-term memory?",
                    back = "Actively retrieving information from memory produces stronger and more durable neural pathways than passive review or re-reading.",
                    hint = "Think about active recall vs passive reading",
                    intervalDays = 1,
                    state = "NEW"
                ),
                FlashcardEntity(
                    deckId = deck1.id,
                    front = "What are the core parameters of the SM-2 Spaced Repetition Algorithm?",
                    back = "Repetition count (n), Ease Factor (EF, default 2.5), and Interval (I, in days), adjusted according to recall quality (0 to 5).",
                    hint = "EF, n, and interval",
                    intervalDays = 1,
                    state = "NEW"
                ),
                FlashcardEntity(
                    deckId = deck1.id,
                    front = "What is the Feynman Technique for concept mastery?",
                    back = "A four-step mental model: 1. Choose a concept, 2. Teach it to a 12-year-old child in simple terms, 3. Identify knowledge gaps, 4. Review and simplify language.",
                    hint = "Teaching a concept to a beginner",
                    intervalDays = 1,
                    state = "NEW"
                )
            )
        )

        // Seed 2nd Deck: Computer Science Algorithms
        val deck2 = FlashcardDeckEntity(
            title = "Core Data Structures & Complexity",
            description = "Big-O complexities, data structures, and algorithmic paradigms",
            subject = "Computer Science"
        )
        flashcardDao.insertDeck(deck2)
        flashcardDao.insertCards(
            listOf(
                FlashcardEntity(
                    deckId = deck2.id,
                    front = "What is the average and worst-case time complexity of QuickSort?",
                    back = "Average case: O(n log n). Worst case: O(n²) when the chosen pivot is always the smallest or largest element.",
                    hint = "Divide and conquer pivot choice",
                    intervalDays = 1,
                    state = "NEW"
                ),
                FlashcardEntity(
                    deckId = deck2.id,
                    front = "What makes a Hash Map lookup O(1) amortized?",
                    back = "Direct memory indexing using a hash function. Collisions are handled via separate chaining or open addressing.",
                    hint = "Key-to-bucket array indexing",
                    intervalDays = 1,
                    state = "NEW"
                ),
                FlashcardEntity(
                    deckId = deck2.id,
                    front = "What is the difference between BFS and DFS traversal?",
                    back = "BFS explores neighbor nodes layer-by-layer using a Queue (FIFO). DFS explores as deep as possible along each branch using a Stack or recursion (LIFO).",
                    hint = "Queue vs Stack",
                    intervalDays = 1,
                    state = "NEW"
                )
            )
        )

        // Seed Starter Note
        val starterNote = NoteEntity(
            title = "Mastering Active Recall & The Forgetting Curve",
            content = "Hermann Ebbinghaus discovered the forgetting curve, showing that humans lose roughly 70% of new information within 24 hours if no active review takes place.\n\nKey strategies to counter this decay:\n1. Active Retrieval: Close the book and quiz yourself or write from memory.\n2. Spaced Intervals: Review at expanding intervals (Day 1, Day 3, Day 7, Day 14, Day 30).\n3. Interleaving: Mix related topics rather than blocking single subjects.\n4. Dual Coding: Combine verbal explanations with spatial diagrams.\n\nApplying these methods with AI generated flashcards and adaptive quizzes yields over 2x retention compared to highlighted re-reading.",
            summary = "Ebbinghaus's forgetting curve shows exponential memory loss without review. Active recall, spaced intervals, and interleaving overcome this decay, doubling retention.",
            keyPointsJson = JSONArray(
                listOf(
                    "70% of unreviewed material is lost within 24 hours",
                    "Active retrieval builds durable synaptic pathways",
                    "Expanding intervals solidify long-term storage",
                    "Interleaving distinct subjects strengthens conceptual discrimination"
                )
            ).toString(),
            subject = "Study Techniques"
        )
        noteDao.insertNote(starterNote)

        // Seed Starter Quiz
        val starterQuiz = QuizEntity(
            title = "Learning Sciences & Retention Check",
            topic = "Study Science",
            questionCount = 3
        )
        quizDao.insertQuiz(starterQuiz)
        quizDao.insertQuestions(
            listOf(
                QuizQuestionEntity(
                    quizId = starterQuiz.id,
                    questionText = "Which study technique has the strongest empirical support for long-term retention?",
                    optionsJson = JSONArray(
                        listOf(
                            "Re-reading textbooks multiple times",
                            "Highlighting key phrases in fluorescent colors",
                            "Spaced active recall (practice testing)",
                            "Summarizing chapters in verbatim prose"
                        )
                    ).toString(),
                    correctIndex = 2,
                    explanation = "Practice testing and spaced retrieval have consistently demonstrated the highest utility across hundreds of cognitive psychology studies.",
                    orderIndex = 0
                ),
                QuizQuestionEntity(
                    quizId = starterQuiz.id,
                    questionText = "In the SM-2 algorithm, what does a higher Ease Factor (EF) indicate?",
                    optionsJson = JSONArray(
                        listOf(
                            "The card is difficult and requires daily reviews",
                            "The item is easier, so subsequent review intervals grow faster",
                            "The card will be deleted from the deck",
                            "The card has been reset to learning status"
                        )
                    ).toString(),
                    correctIndex = 1,
                    explanation = "A higher Ease Factor multiplies the previous interval by a larger number, expanding the time between reviews more rapidly for easily recalled cards.",
                    orderIndex = 1
                ),
                QuizQuestionEntity(
                    quizId = starterQuiz.id,
                    questionText = "What is the primary objective of the Feynman Technique?",
                    optionsJson = JSONArray(
                        listOf(
                            "To memorize formulas through mechanical rote repetition",
                            "To identify knowledge gaps by explaining concepts simply without jargon",
                            "To speed-read 100 pages per hour",
                            "To take photographic memory snapshots"
                        )
                    ).toString(),
                    correctIndex = 1,
                    explanation = "Richard Feynman emphasized that if you cannot explain a concept in simple, plain language to a beginner, you don't truly understand it yet.",
                    orderIndex = 2
                )
            )
        )

        // Seed Starter Progress
        val progress = StudyProgressEntity(
            dateKey = todayKey(),
            cardsReviewed = 4,
            quizzesCompleted = 1,
            notesCreated = 1,
            studyMinutes = 15,
            lastActiveTimestamp = System.currentTimeMillis()
        )
        progressDao.insertOrUpdateProgress(progress)

        // Seed Starter Chat Message
        chatDao.insertMessage(
            ChatMessageEntity(
                sender = "AI",
                content = "👋 Hello! I'm your StudyMind AI tutor. Ask me to explain any difficult concept, break down a topic, generate mnemonics, or test your understanding with practice questions!"
            )
        )
    }
}
