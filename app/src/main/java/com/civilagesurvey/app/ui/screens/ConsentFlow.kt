package com.civilagesurvey.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.civilagesurvey.app.ui.viewmodel.SurveyViewModel

@Composable
fun ConsentFlowScreen(viewModel: SurveyViewModel, onConsentComplete: () -> Unit) {
    var step by remember { mutableStateOf(1) }
    val stakeholder by viewModel.selectedStakeholder.collectAsState()

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
            3 -> ConsentCheckboxesStep(viewModel) { step = 4 }
            4 -> SignatureStep { onConsentComplete() }
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
fun ConsentCheckboxesStep(viewModel: SurveyViewModel, onNext: () -> Unit) {
    var mainConsent by remember { mutableStateOf(false) }
    var audioConsent by remember { mutableStateOf(false) }
    var geoConsent by remember { mutableStateOf(false) }
    var withdrawalExplained by remember { mutableStateOf(false) }

    Text(
        text = "Consent Options",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(16.dp))

    ConsentRow("I voluntarily agree to participate", mainConsent) { mainConsent = it }
    ConsentRow("I consent to audio recording (Optional)", audioConsent) { audioConsent = it }
    ConsentRow("I consent to geo-tagging (Optional)", geoConsent) { geoConsent = it }
    ConsentRow("Withdrawal rights have been explained", withdrawalExplained) { withdrawalExplained = it }

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
    
    // Placeholder for Signature Canvas
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color.LightGray.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Text("Signature Canvas Area", color = Color.Gray)
    }
    
    Spacer(modifier = Modifier.height(24.dp))
    Button(onClick = onComplete, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Icon(Icons.Default.Check, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Lock Consent & Start")
    }
}
