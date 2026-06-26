package com.civilagesurvey.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.civilagesurvey.app.ui.viewmodel.SurveyViewModel

@Composable
fun ConsentFlowScreen(viewModel: SurveyViewModel, onConsentComplete: () -> Unit) {
    var step by remember { mutableStateOf(1) }
    val stakeholder by viewModel.selectedStakeholder.collectAsState()

    var mainConsent by remember { mutableStateOf(false) }
    var audioConsent by remember { mutableStateOf(false) }
    var geoConsent by remember { mutableStateOf(false) }
    var withdrawalExplained by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Step $step: Informed Consent",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        when (step) {
            1 -> ConsentIntroStep(stakeholder) { step = 2 }
            2 -> PrivacyAndWithdrawalStep { step = 3 }
            3 -> ConsentCheckboxesStep(
                mainConsent = mainConsent,
                onMainConsentChange = { mainConsent = it },
                audioConsent = audioConsent,
                onAudioConsentChange = { audioConsent = it },
                geoConsent = geoConsent,
                onGeoConsentChange = { geoConsent = it },
                withdrawalExplained = withdrawalExplained,
                onWithdrawalChange = { withdrawalExplained = it }
            ) { step = 4 }
            4 -> SignatureStep {
                viewModel.saveConsentOptions(
                    consentGiven = mainConsent,
                    audioConsent = audioConsent,
                    geoConsent = geoConsent,
                    withdrawalExplained = withdrawalExplained
                )
                onConsentComplete()
            }
        }
    }
}

@Composable
fun ConsentIntroStep(stakeholder: String, onNext: () -> Unit) {
    Text(
        text = "What is this study about?",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "This is an academic research project studying how new digital services introduced in Smart Cities affect the daily lives, independence, and well-being of older persons above the age of 75.",
        fontSize = 18.sp,
        lineHeight = 26.sp
    )
    Spacer(modifier = Modifier.height(24.dp))
    Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Text("Continue")
    }
}

@Composable
fun PrivacyAndWithdrawalStep(onNext: () -> Unit) {
    Text(
        text = "Privacy & Withdrawal",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Your name will never appear on any research document. You will be assigned a participant code number. You may withdraw at any time without any consequence.",
        fontSize = 18.sp,
        lineHeight = 26.sp
    )
    Spacer(modifier = Modifier.height(24.dp))
    Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Text("I Understand")
    }
}

@Composable
fun ConsentCheckboxesStep(
    mainConsent: Boolean,
    onMainConsentChange: (Boolean) -> Unit,
    audioConsent: Boolean,
    onAudioConsentChange: (Boolean) -> Unit,
    geoConsent: Boolean,
    onGeoConsentChange: (Boolean) -> Unit,
    withdrawalExplained: Boolean,
    onWithdrawalChange: (Boolean) -> Unit,
    onNext: () -> Unit
) {
    Text(
        text = "Consent Options",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(16.dp))

    ConsentRow("I voluntarily agree to participate", mainConsent, onMainConsentChange)
    ConsentRow("I consent to audio recording (Optional)", audioConsent, onAudioConsentChange)
    ConsentRow("I consent to geo-tagging (Optional)", geoConsent, onGeoConsentChange)
    ConsentRow("Withdrawal rights have been explained", withdrawalExplained, onWithdrawalChange)

    Spacer(modifier = Modifier.height(32.dp))
    Button(
        onClick = onNext,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        enabled = mainConsent && withdrawalExplained
    ) {
        Text("Proceed to Signature")
    }
}

@Composable
fun ConsentRow(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 16.sp)
    }
}

@Composable
fun SignatureStep(onComplete: () -> Unit) {
    Text(
        text = "Participant Signature / Thumb Impression",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(16.dp))

    val lines = remember { mutableStateListOf<List<Offset>>() }
    val currentLine = remember { mutableStateOf<List<Offset>>(emptyList()) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentLine.value = listOf(offset)
                            lines.add(currentLine.value)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val newLine = currentLine.value + change.position
                            currentLine.value = newLine
                            if (lines.isNotEmpty()) {
                                lines[lines.lastIndex] = newLine
                            }
                        }
                    )
                }
        ) {
            lines.forEach { line ->
                if (line.size > 1) {
                    for (i in 0 until line.size - 1) {
                        drawLine(
                            color = Color.Black,
                            start = line[i],
                            end = line[i + 1],
                            strokeWidth = 4.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                } else if (line.isNotEmpty()) {
                    drawCircle(
                        color = Color.Black,
                        radius = 2.dp.toPx(),
                        center = line[0]
                    )
                }
            }
        }

        if (lines.isEmpty()) {
            Text(
                text = "Sign or place thumb impression here",
                color = Color.Gray.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        IconButton(
            onClick = {
                lines.clear()
                currentLine.value = emptyList()
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Icon(Icons.Default.Clear, contentDescription = "Clear Signature", tint = MaterialTheme.colorScheme.error)
        }
    }
    
    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = onComplete,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        enabled = lines.isNotEmpty()
    ) {
        Icon(Icons.Default.Check, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Lock Consent & Start")
    }
}

