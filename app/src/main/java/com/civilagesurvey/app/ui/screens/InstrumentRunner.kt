package com.civilagesurvey.app.ui.screens

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    val sessionTime by remember { mutableStateOf(0L) } // Mock session timer
    
    var showBreakAlert by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Simple 25-minute break logic for SQ-OOP
        if (instrument?.code == "SQ-OOP-v1.0") {
            delay(25 * 60 * 1000L) 
            showBreakAlert = true
        }
    }

    if (instrument == null) {
        Text("No instrument selected")
        return
    }

    Scaffold(
        topBar = {
            InstrumentTopBar(instrument!!, sectionIndex, onPause = {})
        },
        bottomBar = {
            InstrumentBottomBar(
                onBack = { viewModel.moveToPreviousQuestion() },
                onNext = { viewModel.moveToNextQuestion() },
                isLast = false // Logic for last question
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
            val currentSection = instrument!!.sections.getOrNull(sectionIndex)
            val currentQuestion = currentSection?.questions?.getOrNull(questionIndex)

            if (currentQuestion != null) {
                QuestionView(currentQuestion, viewModel)
            } else {
                Text("End of section or instrument")
            }
        }
    }

    if (showBreakAlert) {
        BreakAlertDialog(onDismiss = { showBreakAlert = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstrumentTopBar(instrument: Instrument, sectionIndex: Int, onPause: () -> Unit) {
    TopAppBar(
        title = {
            Column {
                Text(instrument.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text("Section ${instrument.sections.getOrNull(sectionIndex)?.code ?: ""}", style = MaterialTheme.typography.bodySmall)
            }
        },
        actions = {
            IconButton(onClick = onPause) {
                Icon(Icons.Default.Pause, contentDescription = "Pause")
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
                Text(if (isLast) "Finish" else "Next")
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

    Text(
        text = question.text,
        style = MaterialTheme.typography.headlineMedium.copy(
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 32.sp
        )
    )
    Spacer(modifier = Modifier.height(32.dp))

    if (skipReason != null) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Skipped: $skipReason", color = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { skipReason = null }) { Text("Undo") }
            }
        }
    } else {
        when (question.inputType) {
            InputType.SINGLE_CHOICE, InputType.LIKERT -> {
                question.options.forEach { option ->
                    SelectableOption(option, false) { viewModel.saveAnswer(question.id, option) }
                }
            }
            InputType.TABLE -> {
                if (question.citySpecific) {
                    val services = viewModel.getCitySpecificServices()
                    services.forEach { service ->
                        Text(service, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
                        // Simplified grid representation
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            listOf("Heard", "Tried", "Used", "Help", "Never").forEach { opt ->
                                FilterChip(selected = false, onClick = {}, label = { Text(opt) }, modifier = Modifier.padding(end = 4.dp))
                            }
                        }
                    }
                }
            }
            InputType.TEXT, InputType.FREE_TEXT -> {
                OutlinedTextField(
                    value = "",
                    onValueChange = { viewModel.saveAnswer(question.id, it) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                    placeholder = { Text("Enter response here...") }
                )
            }
            else -> {
                Text("Input type ${question.inputType} not implemented yet")
            }
        }
    }
    
    Spacer(modifier = Modifier.height(24.dp))
    
    // Skip Logic Button
    if (question.isSkippable && skipReason == null) {
        TextButton(onClick = { showSkipDialog = true }) {
            Text("Skip Question (S)", color = Color.Gray)
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
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
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
