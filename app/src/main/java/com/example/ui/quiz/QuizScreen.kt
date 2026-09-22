package com.example.ui.quiz

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QuizEntity
import com.example.data.model.QuizQuestionEntity
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.theme.StudyError
import com.example.ui.theme.StudyPrimary
import com.example.ui.theme.StudySuccess
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    quizzes: List<QuizEntity>,
    onStartQuiz: (QuizEntity) -> Unit,
    onGenerateQuizAi: (title: String, topic: String, count: Int) -> Unit,
    onDeleteQuiz: (String) -> Unit,
    isGenerating: Boolean,
    modifier: Modifier = Modifier
) {
    var showAiGenDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAiGenDialog = true },
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                text = { Text("AI Quiz Gen") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("ai_quiz_gen_fab")
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Active Recall Quizzes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                TextButton(onClick = { showAiGenDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Generate Quiz")
                }
            }

            if (quizzes.isEmpty()) {
                EmptyPlaceholder(
                    icon = Icons.Outlined.Quiz,
                    title = "No Quizzes Yet",
                    subtitle = "Use Gemini AI to instantly generate active recall multiple-choice quizzes on any topic or study note.",
                    actionButtonText = "Generate Quiz with AI",
                    onActionClick = { showAiGenDialog = true }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(quizzes, key = { it.id }) { quiz ->
                        QuizCardItem(
                            quiz = quiz,
                            onStart = { onStartQuiz(quiz) },
                            onDelete = { onDeleteQuiz(quiz.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAiGenDialog) {
        AiQuizGenDialog(
            onDismiss = { showAiGenDialog = false },
            onGenerate = { title, topic, count ->
                showAiGenDialog = false
                onGenerateQuizAi(title, topic, count)
            }
        )
    }
}

@Composable
fun QuizCardItem(
    quiz: QuizEntity,
    onStart: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onStart() }
            .testTag("quiz_item_${quiz.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = quiz.topic,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete quiz",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = quiz.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${quiz.questionCount} Questions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (quiz.bestScore != null) {
                    Text(
                        text = "•  Best: ${quiz.bestScore}%",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = StudySuccess
                        )
                    )
                }
                if (quiz.totalAttempts > 0) {
                    Text(
                        text = "•  ${quiz.totalAttempts} attempts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onStart,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("start_quiz_btn_${quiz.id}")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (quiz.totalAttempts > 0) "Retake Quiz" else "Start Quiz")
            }
        }
    }
}

@Composable
fun AiQuizGenDialog(
    onDismiss: () -> Unit,
    onGenerate: (title: String, topic: String, count: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }
    var count by remember { mutableFloatStateOf(5f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StudyPrimary)
                Text("Generate AI Quiz")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Gemini AI will formulate high-yield multiple-choice questions with comprehensive answer explanations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = topic,
                    onValueChange = {
                        topic = it
                        if (title.isBlank()) title = "Quiz: $it"
                    },
                    label = { Text("Topic or Subject") },
                    placeholder = { Text("e.g. World History, Neural Networks, Organic Chemistry") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("ai_quiz_topic_input")
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Quiz Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Column {
                    Text(
                        text = "Question count: ${count.toInt()} questions",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Slider(
                        value = count,
                        onValueChange = { count = it },
                        valueRange = 3f..10f,
                        steps = 6
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onGenerate(title.ifBlank { "Quiz: $topic" }, topic, count.toInt()) },
                enabled = topic.isNotBlank(),
                modifier = Modifier.testTag("submit_ai_quiz_gen")
            ) {
                Text("Generate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun QuizRunnerScreen(
    quiz: QuizEntity,
    questions: List<QuizQuestionEntity>,
    currentIndex: Int,
    selectedOptionIndex: Int?,
    isAnswerRevealed: Boolean,
    correctAnswersCount: Int,
    isQuizCompleted: Boolean,
    onSelectOption: (Int) -> Unit,
    onNextQuestion: () -> Unit,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isQuizCompleted || questions.isEmpty()) {
        val total = questions.size
        val percentage = if (total > 0) ((correctAnswersCount.toFloat() / total) * 100).toInt() else 0
        QuizResultView(
            quizTitle = quiz.title,
            correctCount = correctAnswersCount,
            totalCount = total,
            percentage = percentage,
            onClose = onClose,
            onRetry = onRetry,
            modifier = modifier
        )
        return
    }

    val currentQuestion = questions.getOrNull(currentIndex) ?: return
    val options = remember(currentQuestion.optionsJson) {
        try {
            val arr = JSONArray(currentQuestion.optionsJson)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    val progress = (currentIndex + 1).toFloat() / questions.size

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Exit Quiz")
                    }
                    Text(
                        text = quiz.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${currentIndex + 1} / ${questions.size}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                )
            }
        },
        bottomBar = {
            if (isAnswerRevealed) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onNextQuestion,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(48.dp)
                            .testTag("quiz_next_btn")
                    ) {
                        Text(if (currentIndex + 1 < questions.size) "Next Question" else "See Results")
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Question Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "QUESTION ${currentIndex + 1}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = currentQuestion.questionText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 28.sp
                        )
                    )
                }
            }

            // Options List
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                options.forEachIndexed { index, optionText ->
                    val isSelected = selectedOptionIndex == index
                    val isCorrect = currentQuestion.correctIndex == index

                    val containerColor = when {
                        !isAnswerRevealed -> if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        isCorrect -> StudySuccess.copy(alpha = 0.15f)
                        isSelected && !isCorrect -> StudyError.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.surface
                    }

                    val borderColor = when {
                        !isAnswerRevealed -> if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        isCorrect -> StudySuccess
                        isSelected && !isCorrect -> StudyError
                        else -> Color.Transparent
                    }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = containerColor),
                        border = if (borderColor != Color.Transparent) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(borderColor), width = 2.dp) else null,
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isAnswerRevealed) { onSelectOption(index) }
                            .testTag("quiz_option_$index")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = when {
                                    !isAnswerRevealed -> if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    isCorrect -> StudySuccess
                                    isSelected && !isCorrect -> StudyError
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isAnswerRevealed && isCorrect) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    } else if (isAnswerRevealed && isSelected && !isCorrect) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    } else {
                                        Text(
                                            text = ('A' + index).toString(),
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Text(
                                text = optionText,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isSelected || (isAnswerRevealed && isCorrect)) FontWeight.Bold else FontWeight.Normal
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Explanation Box
            if (isAnswerRevealed && currentQuestion.explanation.isNotBlank()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().testTag("quiz_explanation_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Explanation",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentQuestion.explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuizResultView(
    quizTitle: String,
    correctCount: Int,
    totalCount: Int,
    percentage: Int,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isGreat = percentage >= 80
    val isGood = percentage in 50..79

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(28.dp)
            .testTag("quiz_result_view")
    ) {
        Surface(
            shape = CircleShape,
            color = if (isGreat) StudySuccess.copy(alpha = 0.15f) else StudyPrimary.copy(alpha = 0.15f),
            modifier = Modifier.size(100.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isGreat) StudySuccess else StudyPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = when {
                isGreat -> "Outstanding Mastery!"
                isGood -> "Great Effort!"
                else -> "Keep Practicing!"
            },
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "You answered $correctCount out of $totalCount questions correctly on '$quizTitle'.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("quiz_retry_btn")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Try Again")
            }

            Button(
                onClick = onClose,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("quiz_finish_btn")
            ) {
                Text("Done")
            }
        }
    }
}
