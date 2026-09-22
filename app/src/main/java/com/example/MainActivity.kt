package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AiLoadingDialog
import com.example.ui.components.StudyBottomNavigationBar
import com.example.ui.components.StudyTopAppBar
import com.example.ui.chat.ChatScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.flashcards.DeckStudyScreen
import com.example.ui.flashcards.FlashcardsScreen
import com.example.ui.notes.NoteEditorDialog
import com.example.ui.notes.NotesScreen
import com.example.ui.progress.ProgressScreen
import com.example.ui.quiz.QuizRunnerScreen
import com.example.ui.quiz.QuizScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.NavTab
import com.example.ui.viewmodel.StudyViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: StudyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
                val streak by viewModel.streak.collectAsStateWithLifecycle()
                val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

                // Notes
                val notes by viewModel.notes.collectAsStateWithLifecycle()
                val noteSearchQuery by viewModel.noteSearchQuery.collectAsStateWithLifecycle()
                val editingNote by viewModel.editingNote.collectAsStateWithLifecycle()
                val isAiWorkingOnNote by viewModel.isAiWorkingOnNote.collectAsStateWithLifecycle()

                // Flashcards
                val decks by viewModel.decks.collectAsStateWithLifecycle()
                val dueCards by viewModel.dueCards.collectAsStateWithLifecycle()
                val studyDeck by viewModel.studyDeck.collectAsStateWithLifecycle()
                val studyCards by viewModel.studyCards.collectAsStateWithLifecycle()
                val currentCardIndex by viewModel.currentCardIndex.collectAsStateWithLifecycle()
                val isCardFlipped by viewModel.isCardFlipped.collectAsStateWithLifecycle()
                val showHint by viewModel.showHint.collectAsStateWithLifecycle()
                val isStudySessionFinished by viewModel.isStudySessionFinished.collectAsStateWithLifecycle()
                val sessionRatingCounts by viewModel.sessionRatingCounts.collectAsStateWithLifecycle()
                val isGeneratingDeck by viewModel.isGeneratingDeck.collectAsStateWithLifecycle()

                // Quizzes
                val quizzes by viewModel.quizzes.collectAsStateWithLifecycle()
                val activeQuiz by viewModel.activeQuiz.collectAsStateWithLifecycle()
                val quizQuestions by viewModel.quizQuestions.collectAsStateWithLifecycle()
                val currentQuestionIndex by viewModel.currentQuestionIndex.collectAsStateWithLifecycle()
                val selectedOptionIndex by viewModel.selectedOptionIndex.collectAsStateWithLifecycle()
                val isAnswerRevealed by viewModel.isAnswerRevealed.collectAsStateWithLifecycle()
                val correctAnswersCount by viewModel.correctAnswersCount.collectAsStateWithLifecycle()
                val isQuizCompleted by viewModel.isQuizCompleted.collectAsStateWithLifecycle()
                val isGeneratingQuiz by viewModel.isGeneratingQuiz.collectAsStateWithLifecycle()

                // Chat
                val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
                val chatInput by viewModel.chatInput.collectAsStateWithLifecycle()
                val isChatLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()

                // Progress
                val recentProgress by viewModel.recentProgress.collectAsStateWithLifecycle()
                val totalCards by viewModel.repository.totalCardCount.collectAsStateWithLifecycle(initialValue = 0)

                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(toastMessage) {
                    toastMessage?.let {
                        snackbarHostState.showSnackbar(it)
                        viewModel.clearToast()
                    }
                }

                // AI Working Progress Dialogs
                when {
                    isGeneratingDeck -> AiLoadingDialog(message = "Synthesizing spaced repetition flashcards from topic...")
                    isGeneratingQuiz -> AiLoadingDialog(message = "Crafting multiple-choice questions & explanations...")
                    isAiWorkingOnNote -> AiLoadingDialog(message = "Analyzing note and synthesizing insights...")
                }

                // Sub-Screen: Active Flashcard Study Session
                val activeStudyDeck = studyDeck
                if (activeStudyDeck != null) {
                    DeckStudyScreen(
                        deckTitle = activeStudyDeck.title,
                        cards = studyCards,
                        currentIndex = currentCardIndex,
                        isFlipped = isCardFlipped,
                        showHint = showHint,
                        isFinished = isStudySessionFinished,
                        ratingCounts = sessionRatingCounts,
                        onFlip = { viewModel.flipCard() },
                        onToggleHint = { viewModel.toggleHint() },
                        onRate = { rating -> viewModel.rateCard(rating) },
                        onClose = { viewModel.closeStudySession() }
                    )
                    return@MyApplicationTheme
                }

                // Sub-Screen: Active Quiz Runner
                val currentActiveQuiz = activeQuiz
                if (currentActiveQuiz != null) {
                    QuizRunnerScreen(
                        quiz = currentActiveQuiz,
                        questions = quizQuestions,
                        currentIndex = currentQuestionIndex,
                        selectedOptionIndex = selectedOptionIndex,
                        isAnswerRevealed = isAnswerRevealed,
                        correctAnswersCount = correctAnswersCount,
                        isQuizCompleted = isQuizCompleted,
                        onSelectOption = { opt -> viewModel.selectQuizOption(opt) },
                        onNextQuestion = { viewModel.nextQuizQuestion() },
                        onClose = { viewModel.closeQuiz() },
                        onRetry = { viewModel.startQuiz(currentActiveQuiz) }
                    )
                    return@MyApplicationTheme
                }

                // Sub-Screen: Note Editor Dialog
                editingNote?.let { noteToEdit ->
                    NoteEditorDialog(
                        note = noteToEdit,
                        onDismiss = { viewModel.dismissNoteEditor() },
                        onSave = { title, content, subject ->
                            viewModel.saveEditingNote(title, content, subject)
                        },
                        onAiSummarize = { viewModel.summarizeCurrentNote(noteToEdit) },
                        onAiGenerateFlashcards = { viewModel.generateFlashcardsFromNote(noteToEdit) },
                        onAiGenerateQuiz = { viewModel.generateQuizFromNote(noteToEdit) },
                        isAiWorking = isAiWorkingOnNote
                    )
                }

                // Sub-Screen: Progress Screen
                if (currentTab == NavTab.PROGRESS) {
                    ProgressScreen(
                        streak = streak,
                        progressHistory = recentProgress,
                        totalCards = totalCards,
                        totalNotes = notes.size,
                        totalQuizzes = quizzes.size,
                        onBack = { viewModel.setTab(NavTab.DASHBOARD) }
                    )
                    return@MyApplicationTheme
                }

                // Primary App Scaffold
                Scaffold(
                    topBar = {
                        StudyTopAppBar(
                            currentTab = currentTab,
                            streak = streak,
                            onProgressClick = { viewModel.setTab(NavTab.PROGRESS) }
                        )
                    },
                    bottomBar = {
                        StudyBottomNavigationBar(
                            currentTab = currentTab,
                            onTabSelected = { tab -> viewModel.setTab(tab) },
                            dueCardsCount = dueCards.size
                        )
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "tabTransition"
                        ) { targetTab ->
                            when (targetTab) {
                                NavTab.DASHBOARD -> DashboardScreen(
                                    streak = streak,
                                    dueCards = dueCards,
                                    notes = notes,
                                    decks = decks,
                                    quizzes = quizzes,
                                    onNavigateTab = { tab -> viewModel.setTab(tab) },
                                    onStartDueSession = { viewModel.startDueCardsSession() },
                                    onOpenNote = { note -> viewModel.editNote(note) },
                                    onStartQuiz = { quiz -> viewModel.startQuiz(quiz) },
                                    onStartDeckStudy = { deck -> viewModel.startStudySession(deck) },
                                    onNewNoteClick = { viewModel.startNewNote() }
                                )
                                NavTab.NOTES -> NotesScreen(
                                    notes = notes,
                                    searchQuery = noteSearchQuery,
                                    onSearchQueryChange = { q -> viewModel.setNoteSearchQuery(q) },
                                    onNoteClick = { note -> viewModel.editNote(note) },
                                    onNewNoteClick = { viewModel.startNewNote() },
                                    onDeleteNote = { id -> viewModel.deleteNote(id) }
                                )
                                NavTab.FLASHCARDS -> FlashcardsScreen(
                                    decks = decks,
                                    dueCards = dueCards,
                                    onStudyDeck = { deck -> viewModel.startStudySession(deck) },
                                    onStudyDueCards = { viewModel.startDueCardsSession() },
                                    onGenerateDeckAi = { title, topic, count ->
                                        viewModel.generateDeckWithAi(title, topic, count)
                                    },
                                    onCreateManualDeck = { title, desc, subject ->
                                        viewModel.createManualDeck(title, desc, subject)
                                    },
                                    onAddCardToDeck = { deckId, front, back, hint ->
                                        viewModel.addManualCard(deckId, front, back, hint)
                                    },
                                    onDeleteDeck = { deck -> viewModel.deleteDeck(deck) },
                                    isGenerating = isGeneratingDeck
                                )
                                NavTab.QUIZ -> QuizScreen(
                                    quizzes = quizzes,
                                    onStartQuiz = { quiz -> viewModel.startQuiz(quiz) },
                                    onGenerateQuizAi = { title, topic, count ->
                                        viewModel.generateQuizWithAi(title, topic, count)
                                    },
                                    onDeleteQuiz = { id -> viewModel.deleteQuiz(id) },
                                    isGenerating = isGeneratingQuiz
                                )
                                NavTab.CHAT -> ChatScreen(
                                    messages = chatMessages,
                                    chatInput = chatInput,
                                    isLoading = isChatLoading,
                                    onInputChange = { text -> viewModel.updateChatInput(text) },
                                    onSendMessage = { preset -> viewModel.sendChatMessage(preset) },
                                    onClearChat = { viewModel.clearChat() }
                                )
                                NavTab.PROGRESS -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}
