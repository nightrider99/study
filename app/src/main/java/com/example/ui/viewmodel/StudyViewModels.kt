package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChatMessageEntity
import com.example.data.model.FlashcardDeckEntity
import com.example.data.model.FlashcardEntity
import com.example.data.model.NoteEntity
import com.example.data.model.QuizEntity
import com.example.data.model.QuizQuestionEntity
import com.example.data.model.StudyProgressEntity
import com.example.data.repository.SrsRating
import com.example.data.repository.StudyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray

class StudyViewModel(application: Application) : AndroidViewModel(application) {
    val repository = StudyRepository(application)

    // Current navigation tab
    private val _currentTab = MutableStateFlow(NavTab.DASHBOARD)
    val currentTab: StateFlow<NavTab> = _currentTab.asStateFlow()

    // Streak
    private val _streak = MutableStateFlow(0)
    val streak: StateFlow<Int> = _streak.asStateFlow()

    // Global Snackbar / status
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // --- Notes State ---
    val notes: StateFlow<List<NoteEntity>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _noteSearchQuery = MutableStateFlow("")
    val noteSearchQuery: StateFlow<String> = _noteSearchQuery.asStateFlow()

    private val _editingNote = MutableStateFlow<NoteEntity?>(null)
    val editingNote: StateFlow<NoteEntity?> = _editingNote.asStateFlow()

    private val _isAiWorkingOnNote = MutableStateFlow(false)
    val isAiWorkingOnNote: StateFlow<Boolean> = _isAiWorkingOnNote.asStateFlow()

    // --- Flashcards State ---
    val decks: StateFlow<List<FlashcardDeckEntity>> = repository.allDecks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dueCards: StateFlow<List<FlashcardEntity>> = repository.dueCards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _studyDeck = MutableStateFlow<FlashcardDeckEntity?>(null)
    val studyDeck: StateFlow<FlashcardDeckEntity?> = _studyDeck.asStateFlow()

    private val _studyCards = MutableStateFlow<List<FlashcardEntity>>(emptyList())
    val studyCards: StateFlow<List<FlashcardEntity>> = _studyCards.asStateFlow()

    private val _currentCardIndex = MutableStateFlow(0)
    val currentCardIndex: StateFlow<Int> = _currentCardIndex.asStateFlow()

    private val _isCardFlipped = MutableStateFlow(false)
    val isCardFlipped: StateFlow<Boolean> = _isCardFlipped.asStateFlow()

    private val _showHint = MutableStateFlow(false)
    val showHint: StateFlow<Boolean> = _showHint.asStateFlow()

    private val _isStudySessionFinished = MutableStateFlow(false)
    val isStudySessionFinished: StateFlow<Boolean> = _isStudySessionFinished.asStateFlow()

    private val _sessionRatingCounts = MutableStateFlow(mutableMapOf<String, Int>())
    val sessionRatingCounts: StateFlow<Map<String, Int>> = _sessionRatingCounts.asStateFlow()

    private val _isGeneratingDeck = MutableStateFlow(false)
    val isGeneratingDeck: StateFlow<Boolean> = _isGeneratingDeck.asStateFlow()

    // --- Quiz State ---
    val quizzes: StateFlow<List<QuizEntity>> = repository.allQuizzes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeQuiz = MutableStateFlow<QuizEntity?>(null)
    val activeQuiz: StateFlow<QuizEntity?> = _activeQuiz.asStateFlow()

    private val _quizQuestions = MutableStateFlow<List<QuizQuestionEntity>>(emptyList())
    val quizQuestions: StateFlow<List<QuizQuestionEntity>> = _quizQuestions.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _selectedOptionIndex = MutableStateFlow<Int?>(null)
    val selectedOptionIndex: StateFlow<Int?> = _selectedOptionIndex.asStateFlow()

    private val _isAnswerRevealed = MutableStateFlow(false)
    val isAnswerRevealed: StateFlow<Boolean> = _isAnswerRevealed.asStateFlow()

    private val _correctAnswersCount = MutableStateFlow(0)
    val correctAnswersCount: StateFlow<Int> = _correctAnswersCount.asStateFlow()

    private val _isQuizCompleted = MutableStateFlow(false)
    val isQuizCompleted: StateFlow<Boolean> = _isQuizCompleted.asStateFlow()

    private val _isGeneratingQuiz = MutableStateFlow(false)
    val isGeneratingQuiz: StateFlow<Boolean> = _isGeneratingQuiz.asStateFlow()

    // --- Chat State ---
    val chatMessages: StateFlow<List<ChatMessageEntity>> = repository.chatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _chatInput = MutableStateFlow("")
    val chatInput: StateFlow<String> = _chatInput.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    // --- Progress State ---
    val recentProgress: StateFlow<List<StudyProgressEntity>> = repository.recentProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            refreshStreak()
        }
    }

    fun setTab(tab: NavTab) {
        _currentTab.value = tab
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun refreshStreak() {
        viewModelScope.launch {
            _streak.value = repository.calculateStreak()
        }
    }

    // --- Notes Actions ---
    fun setNoteSearchQuery(query: String) {
        _noteSearchQuery.value = query
    }

    fun startNewNote() {
        _editingNote.value = NoteEntity(title = "", content = "", subject = "General")
    }

    fun editNote(note: NoteEntity) {
        _editingNote.value = note
    }

    fun dismissNoteEditor() {
        _editingNote.value = null
    }

    fun saveEditingNote(title: String, content: String, subject: String) {
        val current = _editingNote.value ?: NoteEntity(title = title, content = content, subject = subject)
        val updated = current.copy(
            title = title.ifBlank { "Untitled Note" },
            content = content,
            subject = subject.ifBlank { "General" },
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.saveNote(updated)
            _editingNote.value = null
            showToast("Note saved successfully")
            refreshStreak()
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            repository.deleteNote(id)
            showToast("Note deleted")
        }
    }

    fun summarizeCurrentNote(note: NoteEntity) {
        viewModelScope.launch {
            _isAiWorkingOnNote.value = true
            val result = repository.summarizeNoteWithAi(note.id)
            _isAiWorkingOnNote.value = false
            result.onSuccess { updated ->
                _editingNote.value = updated
                showToast("Note summarized with AI!")
            }.onFailure { err ->
                showToast("AI Summary failed: ${err.message}")
            }
        }
    }

    fun generateFlashcardsFromNote(note: NoteEntity) {
        viewModelScope.launch {
            _isAiWorkingOnNote.value = true
            val result = repository.generateFlashcardsWithAi(
                deckTitle = "Cards: ${note.title}",
                topic = note.subject,
                context = "${note.title}\n\n${note.content}",
                count = 6
            )
            _isAiWorkingOnNote.value = false
            result.onSuccess { deck ->
                showToast("Generated deck '${deck.title}'!")
                setTab(NavTab.FLASHCARDS)
            }.onFailure { err ->
                showToast("Failed to generate cards: ${err.message}")
            }
        }
    }

    fun generateQuizFromNote(note: NoteEntity) {
        viewModelScope.launch {
            _isAiWorkingOnNote.value = true
            val result = repository.generateQuizWithAi(
                title = "Quiz: ${note.title}",
                topic = note.subject,
                context = "${note.title}\n\n${note.content}",
                count = 5
            )
            _isAiWorkingOnNote.value = false
            result.onSuccess { quiz ->
                showToast("Created quiz '${quiz.title}'!")
                setTab(NavTab.QUIZ)
            }.onFailure { err ->
                showToast("Failed to generate quiz: ${err.message}")
            }
        }
    }

    // --- Flashcards Actions ---
    fun startStudySession(deck: FlashcardDeckEntity) {
        viewModelScope.launch {
            _studyDeck.value = deck
            val due = repository.getDueCardsForDeck(deck.id)
            val cardsToStudy = if (due.isNotEmpty()) due else repository.getCardsForDeckOnce(deck.id)
            _studyCards.value = cardsToStudy
            _currentCardIndex.value = 0
            _isCardFlipped.value = false
            _showHint.value = false
            _isStudySessionFinished.value = false
            _sessionRatingCounts.value = mutableMapOf()
        }
    }

    fun startDueCardsSession() {
        viewModelScope.launch {
            val allDue = dueCards.value
            if (allDue.isEmpty()) {
                showToast("No cards currently due for review!")
                return@launch
            }
            _studyDeck.value = FlashcardDeckEntity(title = "All Due Flashcards", description = "SRS Spaced Repetition Review")
            _studyCards.value = allDue
            _currentCardIndex.value = 0
            _isCardFlipped.value = false
            _showHint.value = false
            _isStudySessionFinished.value = false
            _sessionRatingCounts.value = mutableMapOf()
        }
    }

    fun flipCard() {
        _isCardFlipped.value = !_isCardFlipped.value
    }

    fun toggleHint() {
        _showHint.value = !_showHint.value
    }

    fun rateCard(rating: SrsRating) {
        val cards = _studyCards.value
        val idx = _currentCardIndex.value
        if (idx !in cards.indices) return

        val currentCard = cards[idx]
        viewModelScope.launch {
            repository.reviewCard(currentCard, rating)
            refreshStreak()

            val counts = _sessionRatingCounts.value.toMutableMap()
            counts[rating.label] = (counts[rating.label] ?: 0) + 1
            _sessionRatingCounts.value = counts

            if (idx + 1 < cards.size) {
                _currentCardIndex.value = idx + 1
                _isCardFlipped.value = false
                _showHint.value = false
            } else {
                _isStudySessionFinished.value = true
            }
        }
    }

    fun closeStudySession() {
        _studyDeck.value = null
        _studyCards.value = emptyList()
        _isStudySessionFinished.value = false
    }

    fun generateDeckWithAi(title: String, topic: String, count: Int = 6) {
        viewModelScope.launch {
            _isGeneratingDeck.value = true
            val result = repository.generateFlashcardsWithAi(
                deckTitle = title.ifBlank { "Deck: $topic" },
                topic = topic,
                count = count
            )
            _isGeneratingDeck.value = false
            result.onSuccess { deck ->
                showToast("Created deck '${deck.title}'!")
            }.onFailure { err ->
                showToast("AI Deck generation failed: ${err.message}")
            }
        }
    }

    fun createManualDeck(title: String, description: String, subject: String) {
        viewModelScope.launch {
            repository.createDeck(title, description, subject)
            showToast("Deck created")
        }
    }

    fun addManualCard(deckId: String, front: String, back: String, hint: String?) {
        viewModelScope.launch {
            repository.addCardToDeck(deckId, front, back, hint)
            showToast("Card added")
        }
    }

    fun deleteDeck(deck: FlashcardDeckEntity) {
        viewModelScope.launch {
            repository.deleteDeck(deck)
            showToast("Deck deleted")
        }
    }

    // --- Quiz Actions ---
    fun startQuiz(quiz: QuizEntity) {
        viewModelScope.launch {
            _activeQuiz.value = quiz
            _quizQuestions.value = repository.getQuestionsForQuiz(quiz.id)
            _currentQuestionIndex.value = 0
            _selectedOptionIndex.value = null
            _isAnswerRevealed.value = false
            _correctAnswersCount.value = 0
            _isQuizCompleted.value = false
        }
    }

    fun selectQuizOption(index: Int) {
        if (_isAnswerRevealed.value) return
        _selectedOptionIndex.value = index
        _isAnswerRevealed.value = true

        val currentQ = _quizQuestions.value.getOrNull(_currentQuestionIndex.value)
        if (currentQ != null && index == currentQ.correctIndex) {
            _correctAnswersCount.value = _correctAnswersCount.value + 1
        }
    }

    fun nextQuizQuestion() {
        val total = _quizQuestions.value.size
        val nextIdx = _currentQuestionIndex.value + 1
        if (nextIdx < total) {
            _currentQuestionIndex.value = nextIdx
            _selectedOptionIndex.value = null
            _isAnswerRevealed.value = false
        } else {
            // Completed quiz! Record attempt & score percentage
            val correct = _correctAnswersCount.value
            val pct = if (total > 0) ((correct.toFloat() / total) * 100).toInt() else 0
            _isQuizCompleted.value = true
            _activeQuiz.value?.let { q ->
                viewModelScope.launch {
                    repository.recordQuizAttempt(q.id, pct)
                    refreshStreak()
                }
            }
        }
    }

    fun closeQuiz() {
        _activeQuiz.value = null
        _quizQuestions.value = emptyList()
        _isQuizCompleted.value = false
    }

    fun generateQuizWithAi(title: String, topic: String, count: Int = 5) {
        viewModelScope.launch {
            _isGeneratingQuiz.value = true
            val result = repository.generateQuizWithAi(
                title = title.ifBlank { "Quiz: $topic" },
                topic = topic,
                count = count
            )
            _isGeneratingQuiz.value = false
            result.onSuccess { q ->
                showToast("Created quiz '${q.title}'!")
            }.onFailure { err ->
                showToast("AI Quiz generation failed: ${err.message}")
            }
        }
    }

    fun deleteQuiz(quizId: String) {
        viewModelScope.launch {
            repository.deleteQuiz(quizId)
            showToast("Quiz deleted")
        }
    }

    // --- Chat Actions ---
    fun updateChatInput(text: String) {
        _chatInput.value = text
    }

    fun sendChatMessage(presetMessage: String? = null) {
        val messageToSend = presetMessage ?: _chatInput.value.trim()
        if (messageToSend.isBlank() || _isChatLoading.value) return

        if (presetMessage == null) {
            _chatInput.value = ""
        }

        viewModelScope.launch {
            _isChatLoading.value = true
            val result = repository.sendChatMessage(messageToSend)
            _isChatLoading.value = false
            result.onFailure { err ->
                showToast("Tutor response error: ${err.message}")
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChatHistory()
            showToast("Chat cleared")
        }
    }
}

enum class NavTab(val title: String) {
    DASHBOARD("Home"),
    NOTES("Notes"),
    FLASHCARDS("Flashcards"),
    QUIZ("Quizzes"),
    CHAT("AI Tutor"),
    PROGRESS("Progress")
}
