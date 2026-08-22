package com.civilagesurvey.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.civilagesurvey.app.data.model.*
import com.civilagesurvey.app.ui.viewmodel.SurveyViewModel
import kotlinx.coroutines.delay

@Composable
fun InstrumentRunnerScreen(viewModel: SurveyViewModel, onFinish: () -> Unit) {
    val instrument by viewModel.currentInstrument.collectAsState()
    val sectionIndex by viewModel.currentSectionIndex.collectAsState()
    val questionIndex by viewModel.currentQuestionIndex.collectAsState()
    val interviewIndex by viewModel.currentInterviewIndex.collectAsState()
    
    var showBreakAlert by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Simple 25-minute break logic for SQ-OOP
        if (instrument?.code == "SQ-OOP-v1.0") {
            delay(25 * 60 * 1000L) 
            showBreakAlert = true
        }
    }

    if (instrument == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No instrument selected", style = MaterialTheme.typography.titleLarge)
        }
        return
    }

    val showThemes by viewModel.showThemesPhase.collectAsState()

    val savedAnswers by viewModel.savedAnswers.collectAsState()
    val isLast = remember(instrument, sectionIndex, questionIndex, interviewIndex, showThemes, savedAnswers) {
        !viewModel.hasNextStep()
    }

    Scaffold(
        topBar = {
            InstrumentTopBar(
                instrument = instrument!!,
                sectionIndex = sectionIndex,
                interviewIndex = interviewIndex,
                isQualitative = showThemes,
                onPause = { viewModel.stopSpeaking() }
            )
        },
        bottomBar = {
            InstrumentBottomBar(
                onBack = {
                    viewModel.moveToPreviousStep()
                },
                onNext = {
                    if (isLast) {
                        viewModel.stopSpeaking()
                        viewModel.finalizeDcaSession()
                        onFinish()
                    } else {
                        viewModel.moveToNextStep()
                    }
                },
                isLast = isLast
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (showThemes) {
                val currentTheme = instrument!!.themes.getOrNull(interviewIndex)
                if (currentTheme != null) {
                    ThemeView(currentTheme, interviewIndex, viewModel)
                } else {
                    Text("End of themes", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                val currentSection = instrument!!.sections.getOrNull(sectionIndex)
                val currentQuestion = currentSection?.questions?.getOrNull(questionIndex)

                if (currentQuestion != null) {
                    QuestionView(currentQuestion, viewModel)
                } else {
                    Text("End of section or instrument", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }

    if (showBreakAlert) {
        BreakAlertDialog(onDismiss = { showBreakAlert = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstrumentTopBar(
    instrument: Instrument,
    sectionIndex: Int,
    interviewIndex: Int,
    isQualitative: Boolean,
    onPause: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(instrument.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    text = if (isQualitative) "Theme ${instrument.themes.getOrNull(interviewIndex)?.code ?: ""}"
                           else "Section ${instrument.sections.getOrNull(sectionIndex)?.code ?: ""}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        actions = {
            IconButton(onClick = onPause) {
                Icon(Icons.Default.Pause, contentDescription = "Pause / Stop Speech")
            }
        }
    )
}

@Composable
fun InstrumentBottomBar(onBack: () -> Unit, onNext: () -> Unit, isLast: Boolean) {
    Surface(tonalElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(56.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Back")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = onNext, modifier = Modifier.weight(1f).height(56.dp)) {
                Text(if (isLast) "Submit" else "Next")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
fun QuestionView(question: Question, viewModel: SurveyViewModel) {
    var skipReason by remember { mutableStateOf<String?>(null) }
    var showSkipDialog by remember { mutableStateOf(false) }
    val savedAnswers by viewModel.savedAnswers.collectAsState()
    val currentAnswer = savedAnswers[question.id]

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = question.text,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 32.sp
            ),
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { viewModel.speakAloud(question.text) }) {
            Icon(Icons.Default.VolumeUp, contentDescription = "Read Aloud", tint = MaterialTheme.colorScheme.primary)
        }
    }
    Spacer(modifier = Modifier.height(32.dp))

    // Handle initial skip check from loaded answers
    LaunchedEffect(currentAnswer) {
        if (currentAnswer == "S" && skipReason == null) {
            skipReason = "Skipped"
        } else if (currentAnswer != "S" && currentAnswer != null) {
            skipReason = null
        }
    }

    if (skipReason != null) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Skipped question", color = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    skipReason = null
                    viewModel.saveAnswer(question.id, "")
                }) { Text("Undo") }
            }
        }
    } else {
        when (question.inputType) {
            InputType.SINGLE_CHOICE, InputType.LIKERT -> {
                question.options.forEach { option ->
                    SelectableOption(option, option == currentAnswer) {
                        viewModel.saveAnswer(question.id, option)
                    }
                }
            }
            InputType.MULTI_CHOICE -> {
                val selectedOptions = remember(currentAnswer) {
                    currentAnswer?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
                }
                question.options.forEach { option ->
                    val isChecked = selectedOptions.contains(option)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .heightIn(min = 56.dp),
                        onClick = {
                            val newSelection = if (isChecked) {
                                selectedOptions - option
                            } else {
                                selectedOptions + option
                            }
                            viewModel.saveAnswer(question.id, newSelection.joinToString("|"))
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isChecked) null else BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isChecked, onCheckedChange = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(option, fontSize = 18.sp, fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
            InputType.TABLE -> {
                SubjectiveAppReviewView(question, viewModel, savedAnswers)
            }
            InputType.NUMBER -> {
                var textValue by remember(currentAnswer) { mutableStateOf(currentAnswer ?: "") }
                OutlinedTextField(
                    value = textValue,
                    onValueChange = {
                        textValue = it
                        viewModel.saveAnswer(question.id, it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter number...") }
                )
            }
            InputType.TEXT, InputType.FREE_TEXT -> {
                var textValue by remember(currentAnswer) { mutableStateOf(currentAnswer ?: "") }
                val isRecording by viewModel.isRecording.collectAsState()
                val recordingDuration by viewModel.recordingDuration.collectAsState()

                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = {
                            textValue = it
                            viewModel.saveAnswer(question.id, it)
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                        placeholder = { Text("Enter response or tap voice assistant below to transcribe...") }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Voice transcription assistant for text questions
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isRecording) Icons.Default.Mic else Icons.Default.MicOff,
                                    contentDescription = null,
                                    tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isRecording) "Listening... (${recordingDuration}s)" else "Voice Transcription",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (!isRecording) {
                                OutlinedButton(
                                    onClick = { viewModel.startVoiceAnswer() },
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Speak", fontSize = 13.sp)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        viewModel.stopVoiceAnswer { transcribed ->
                                            if (transcribed.isNotBlank()) {
                                                val newText = if (textValue.isBlank()) transcribed else "$textValue\n$transcribed"
                                                textValue = newText
                                                viewModel.saveAnswer(question.id, newText)
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Done & Insert", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                Text("Input type ${question.inputType} not implemented yet", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
    
    Spacer(modifier = Modifier.height(24.dp))
    
    // Skip Logic Button
    if (question.isSkippable && skipReason == null) {
        TextButton(onClick = { showSkipDialog = true }) {
            Text("Skip Question (S)", color = Color.Gray, fontSize = 16.sp)
        }
    }

    if (showSkipDialog) {
        SkipReasonDialog(
            onDismiss = { showSkipDialog = false },
            onConfirm = { reason ->
                skipReason = reason
                viewModel.saveAnswer(question.id, "S") // Mark as skipped
                showSkipDialog = false
            }
        )
    }
}

@Composable
fun ThemeView(theme: InterviewTheme, index: Int, viewModel: SurveyViewModel) {
    val interviewAnswers by viewModel.interviewAnswers.collectAsState()
    val answerText = interviewAnswers[index] ?: ""
    val activeSurvey by viewModel.activeSurvey.collectAsState()
    val hasAudioConsent = activeSurvey?.audioConsent == true

    val isRecording by viewModel.isRecording.collectAsState()
    val recordingDuration by viewModel.recordingDuration.collectAsState()
    val recordingState by viewModel.recordingState.collectAsState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Theme ${theme.code}: ${theme.title}",
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { viewModel.speakAloud("${theme.title}. ${theme.openingQuestion}") }) {
            Icon(Icons.Default.VolumeUp, contentDescription = "Read Aloud", tint = MaterialTheme.colorScheme.primary)
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = theme.openingQuestion,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 18.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Medium
        ),
        modifier = Modifier.fillMaxWidth()
    )

    if (theme.probes.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Follow-up Probes (Select & Answer):", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(8.dp))
                theme.probes.forEachIndexed { pIndex, probe ->
                    val probeSelectedKey = "probe_selected_${theme.code}_$pIndex"
                    val probeResponseKey = "probe_response_${theme.code}_$pIndex"
                    
                    val answers by viewModel.savedAnswers.collectAsState()
                    val isSelected = answers[probeSelectedKey] == "true"
                    val probeResponse = answers[probeResponseKey] ?: ""

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                val nextSelectedState = if (isSelected) "false" else "true"
                                viewModel.saveAnswer(probeSelectedKey, nextSelectedState)
                            }
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    viewModel.saveAnswer(probeSelectedKey, if (checked) "true" else "false")
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = probe,
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.speakAloud(probe) }) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Read Probe", tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                            }
                        }
                        if (isSelected) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = probeResponse,
                                onValueChange = { viewModel.saveAnswer(probeResponseKey, it) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Notes for this probe") },
                                placeholder = { Text("Type specific response notes for this probe...") },
                                singleLine = false,
                                maxLines = 4
                            )
                        }
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    OutlinedTextField(
        value = answerText,
        onValueChange = { viewModel.saveInterviewAnswer(index, it) },
        modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
        label = { Text("Response Notes") },
        placeholder = { Text("Type interview responses and observation notes here...") }
    )

    if (hasAudioConsent) {
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isRecording) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = null,
                        tint = if (isRecording) MaterialTheme.colorScheme.error else Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRecording) "Recording Voice... (${recordingDuration}s)" else "Voice Assistant ($recordingState)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (!isRecording) {
                        Button(
                            onClick = { viewModel.startVoiceAnswer() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Start Recording")
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.stopVoiceAnswer { text ->
                                    val currentText = answerText
                                    val updatedText = if (currentText.isBlank()) text else "$currentText\n$text"
                                    viewModel.saveInterviewAnswer(index, updatedText)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Stop & Transcribe")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SkipReasonDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val reasons = listOf("Fatigue", "Discomfort", "Memory issue", "Not applicable", "Refusal", "Time constraint")
    var selectedReason by remember { mutableStateOf(reasons[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reason for skipping") },
        text = {
            Column {
                reasons.forEach { reason ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { selectedReason = reason }.padding(vertical = 8.dp)
                    ) {
                        RadioButton(selected = selectedReason == reason, onClick = { selectedReason = reason })
                        Text(reason)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedReason) }) { Text("Confirm Skip") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SelectableOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .heightIn(min = 56.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = if (selected) null else BorderStroke(1.dp, Color.LightGray)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.CenterStart) {
            Text(text, fontSize = 18.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@Composable
fun BreakAlertDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null) },
        title = { Text("Mandatory Break") },
        text = { Text("It has been 25 minutes. Please pause and offer the participant a break.") },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Continue") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("End Session") }
        }
    )
}

@Composable
fun SubjectiveAppReviewView(
    question: Question,
    viewModel: SurveyViewModel,
    savedAnswers: Map<String, String>
) {
    val countKey = "${question.id}_app_count"
    val appCount = (savedAnswers[countKey]?.toIntOrNull() ?: 1).coerceAtLeast(1)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Digital App / Service Review (Subjective)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Enter the name of each civic app/service tried or used, and record subjective experience notes.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))

        for (index in 0 until appCount) {
            val appNameKey = "${question.id}_app_${index}_name"
            val appReviewKey = "${question.id}_app_${index}_review"
            val appStatusKey = "${question.id}_app_${index}_status"

            val appName = savedAnswers[appNameKey] ?: ""
            val appReview = savedAnswers[appReviewKey] ?: ""
            val appStatus = savedAnswers[appStatusKey] ?: ""

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "App / Service #${index + 1}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = appName,
                        onValueChange = { viewModel.saveAnswer(appNameKey, it) },
                        label = { Text("App or Digital Service Name") },
                        placeholder = { Text("e.g. Health App, Metro Ticket App, Grievance Portal...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Usage Experience:", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))

                    val statuses = listOf("Heard Only", "Tried", "Used Alone", "Used with Help", "Could Not Use")
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        statuses.forEach { status ->
                            val isSelected = appStatus == status
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.saveAnswer(appStatusKey, if (isSelected) "" else status) },
                                label = { Text(status, fontSize = 13.sp) },
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = appReview,
                        onValueChange = { viewModel.saveAnswer(appReviewKey, it) },
                        label = { Text("Subjective Review & Experience Notes") },
                        placeholder = { Text("Record detailed feedback, difficulties, or observation notes...") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = { viewModel.saveAnswer(countKey, (appCount + 1).toString()) }
            ) {
                Text("+ Add Another App Review")
            }

            if (appCount > 1) {
                TextButton(
                    onClick = { viewModel.saveAnswer(countKey, (appCount - 1).toString()) }
                ) {
                    Text("Remove Last", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

