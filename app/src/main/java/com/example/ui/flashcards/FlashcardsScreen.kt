package com.example.ui.flashcards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.FlashcardDeckEntity
import com.example.data.model.FlashcardEntity
import com.example.data.repository.SrsRating
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.theme.StudyError
import com.example.ui.theme.StudyPrimary
import com.example.ui.theme.StudySuccess
import com.example.ui.theme.StudyTertiary
import com.example.ui.theme.StudyWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsScreen(
    decks: List<FlashcardDeckEntity>,
    dueCards: List<FlashcardEntity>,
    onStudyDeck: (FlashcardDeckEntity) -> Unit,
    onStudyDueCards: () -> Unit,
    onGenerateDeckAi: (title: String, topic: String, count: Int) -> Unit,
    onCreateManualDeck: (title: String, description: String, subject: String) -> Unit,
    onAddCardToDeck: (deckId: String, front: String, back: String, hint: String?) -> Unit,
    onDeleteDeck: (FlashcardDeckEntity) -> Unit,
    isGenerating: Boolean,
    modifier: Modifier = Modifier
) {
    var showAiGenDialog by remember { mutableStateOf(false) }
    var showCreateDeckDialog by remember { mutableStateOf(false) }
    var deckToAddCardTo by remember { mutableStateOf<FlashcardDeckEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAiGenDialog = true },
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                text = { Text("AI Deck Gen") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("ai_deck_gen_fab")
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header Action Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Spaced Repetition Decks",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                OutlinedButton(
                    onClick = { showCreateDeckDialog = true },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Deck")
                }
            }

            // Due Cards Review Banner (if any cards due)
            if (dueCards.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { onStudyDueCards() }
                        .testTag("due_cards_banner")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${dueCards.size}",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Ready for SRS Review",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Review all scheduled cards now",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (decks.isEmpty()) {
                EmptyPlaceholder(
                    icon = Icons.Outlined.Style,
                    title = "No Flashcard Decks",
                    subtitle = "Create your own deck or use Gemini AI to generate flashcards from any topic or study note in seconds.",
                    actionButtonText = "Generate with AI",
                    onActionClick = { showAiGenDialog = true }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(decks, key = { it.id }) { deck ->
                        DeckListItem(
                            deck = deck,
                            onStudy = { onStudyDeck(deck) },
                            onAddCard = { deckToAddCardTo = deck },
                            onDelete = { onDeleteDeck(deck) }
                        )
                    }
                }
            }
        }
    }

    // AI Deck Generator Dialog
    if (showAiGenDialog) {
        AiDeckGenDialog(
            onDismiss = { showAiGenDialog = false },
            onGenerate = { title, topic, count ->
                showAiGenDialog = false
                onGenerateDeckAi(title, topic, count)
            }
        )
    }

    // Create Manual Deck Dialog
    if (showCreateDeckDialog) {
        CreateDeckDialog(
            onDismiss = { showCreateDeckDialog = false },
            onCreate = { title, desc, subject ->
                showCreateDeckDialog = false
                onCreateManualDeck(title, desc, subject)
            }
        )
    }

    // Add Card to Deck Dialog
    deckToAddCardTo?.let { deck ->
        AddCardDialog(
            deckTitle = deck.title,
            onDismiss = { deckToAddCardTo = null },
            onAdd = { front, back, hint ->
                onAddCardToDeck(deck.id, front, back, hint)
                deckToAddCardTo = null
            }
        )
    }
}

@Composable
fun DeckListItem(
    deck: FlashcardDeckEntity,
    onStudy: () -> Unit,
    onAddCard: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onStudy() }
            .testTag("deck_item_${deck.id}")
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
                        text = deck.subject,
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
                        contentDescription = "Delete deck",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = deck.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (deck.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = deck.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onAddCard,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Card", style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = onStudy,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("study_deck_btn_${deck.id}")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Study Now", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun AiDeckGenDialog(
    onDismiss: () -> Unit,
    onGenerate: (title: String, topic: String, count: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }
    var count by remember { mutableFloatStateOf(6f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StudyPrimary)
                Text("Generate AI Deck")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Gemini AI will synthesize active-recall flashcards formatted for spaced repetition.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = topic,
                    onValueChange = {
                        topic = it
                        if (title.isBlank()) title = "Cards: $it"
                    },
                    label = { Text("Study Topic or Subject") },
                    placeholder = { Text("e.g. Cognitive Biases, Cellular Respiration") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("ai_deck_topic_input")
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Deck Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Column {
                    Text(
                        text = "Card count: ${count.toInt()} cards",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Slider(
                        value = count,
                        onValueChange = { count = it },
                        valueRange = 4f..12f,
                        steps = 7
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onGenerate(title.ifBlank { "Cards: $topic" }, topic, count.toInt()) },
                enabled = topic.isNotBlank(),
                modifier = Modifier.testTag("submit_ai_deck_gen")
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
fun CreateDeckDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, description: String, subject: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("General") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Deck") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Deck Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject / Field") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(title, description, subject) },
                enabled = title.isNotBlank()
            ) {
                Text("Create")
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
fun AddCardDialog(
    deckTitle: String,
    onDismiss: () -> Unit,
    onAdd: (front: String, back: String, hint: String?) -> Unit
) {
    var front by remember { mutableStateOf("") }
    var back by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Card to $deckTitle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = front,
                    onValueChange = { front = it },
                    label = { Text("Front (Prompt / Question)") },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("card_front_input")
                )
                OutlinedTextField(
                    value = back,
                    onValueChange = { back = it },
                    label = { Text("Back (Answer / Definition)") },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("card_back_input")
                )
                OutlinedTextField(
                    value = hint,
                    onValueChange = { hint = it },
                    label = { Text("Hint (Optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(front, back, hint.takeIf { it.isNotBlank() }) },
                enabled = front.isNotBlank() && back.isNotBlank(),
                modifier = Modifier.testTag("submit_add_card")
            ) {
                Text("Add Card")
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
fun DeckStudyScreen(
    deckTitle: String,
    cards: List<FlashcardEntity>,
    currentIndex: Int,
    isFlipped: Boolean,
    showHint: Boolean,
    isFinished: Boolean,
    ratingCounts: Map<String, Int>,
    onFlip: () -> Unit,
    onToggleHint: () -> Unit,
    onRate: (SrsRating) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isFinished || cards.isEmpty()) {
        StudySessionFinishedView(
            deckTitle = deckTitle,
            ratingCounts = ratingCounts,
            totalReviewed = cards.size,
            onClose = onClose,
            modifier = modifier
        )
        return
    }

    val currentCard = cards.getOrNull(currentIndex) ?: return
    val progress = (currentIndex + 1).toFloat() / cards.size

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "cardFlip"
    )

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
                        Icon(Icons.Default.Close, contentDescription = "Exit Study")
                    }
                    Text(
                        text = deckTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${currentIndex + 1} / ${cards.size}",
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
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
        ) {
            // Interactive 3D Flip Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFlipped) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp)
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 12f * density
                    }
                    .clickable { onFlip() }
                    .testTag("study_flashcard")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(28.dp)
                ) {
                    if (rotation <= 90f) {
                        // Front View (Question)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "QUESTION",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Text(
                                text = currentCard.front,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 32.sp
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (currentCard.hint != null) {
                                    if (showHint) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        ) {
                                            Text(
                                                text = "💡 ${currentCard.hint}",
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    } else {
                                        TextButton(onClick = onToggleHint) {
                                            Text("Show Hint", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                                Text(
                                    text = "Tap card to flip",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        // Back View (Answer) - inverted rotationY so text is not mirrored
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { rotationY = 180f }
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = StudyPrimary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "ANSWER",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = StudyPrimary
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Text(
                                text = currentCard.back,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 30.sp
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )

                            Text(
                                text = "Rate recall ease below",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // SM-2 Spaced Repetition Rating Buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isFlipped) "How easily did you recall this?" else "Flip card to reveal answer & rate recall",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SrsRatingButton(
                        rating = SrsRating.AGAIN,
                        intervalText = "<1 d",
                        containerColor = StudyError,
                        enabled = isFlipped,
                        onClick = { onRate(SrsRating.AGAIN) },
                        modifier = Modifier.weight(1f)
                    )
                    SrsRatingButton(
                        rating = SrsRating.HARD,
                        intervalText = "2 d",
                        containerColor = StudyWarning,
                        enabled = isFlipped,
                        onClick = { onRate(SrsRating.HARD) },
                        modifier = Modifier.weight(1f)
                    )
                    SrsRatingButton(
                        rating = SrsRating.GOOD,
                        intervalText = "4 d",
                        containerColor = StudySuccess,
                        enabled = isFlipped,
                        onClick = { onRate(SrsRating.GOOD) },
                        modifier = Modifier.weight(1f)
                    )
                    SrsRatingButton(
                        rating = SrsRating.EASY,
                        intervalText = "7+ d",
                        containerColor = StudyPrimary,
                        enabled = isFlipped,
                        onClick = { onRate(SrsRating.EASY) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun SrsRatingButton(
    rating: SrsRating,
    intervalText: String,
    containerColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
        modifier = modifier.testTag("srs_btn_${rating.name.lowercase()}")
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = rating.label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = intervalText,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
            )
        }
    }
}

@Composable
fun StudySessionFinishedView(
    deckTitle: String,
    ratingCounts: Map<String, Int>,
    totalReviewed: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(28.dp)
            .testTag("session_finished_view")
    ) {
        Surface(
            shape = CircleShape,
            color = StudySuccess.copy(alpha = 0.15f),
            modifier = Modifier.size(90.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = StudySuccess,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Study Session Complete!",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Great work! You reviewed $totalReviewed flashcards in '$deckTitle'. Spaced repetition intervals have been recalculated.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                SessionStatPill("Again", ratingCounts["Again"] ?: 0, StudyError)
                SessionStatPill("Hard", ratingCounts["Hard"] ?: 0, StudyWarning)
                SessionStatPill("Good", ratingCounts["Good"] ?: 0, StudySuccess)
                SessionStatPill("Easy", ratingCounts["Easy"] ?: 0, StudyPrimary)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onClose,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("finish_session_btn")
        ) {
            Text("Back to Decks")
        }
    }
}

@Composable
fun SessionStatPill(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
