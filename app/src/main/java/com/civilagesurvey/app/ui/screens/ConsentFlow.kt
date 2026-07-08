package com.civilagesurvey.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
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
import com.civilagesurvey.app.ui.theme.GreenSuccess
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
                onWithdrawalChange = { withdrawalExplained = it },
                viewModel = viewModel
            ) {
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
        text = "This is an academic research project studying how new digital services introduced in Smart Cities affect the daily lives, independence, and well-being of older persons.",
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
    viewModel: SurveyViewModel,
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

    if (geoConsent) {
        val lat by viewModel.intakeLatitude.collectAsState()
        val lng by viewModel.intakeLongitude.collectAsState()
        val acc by viewModel.intakeAccuracy.collectAsState()
        val status by viewModel.intakeLocationStatus.collectAsState()
        val isFetching by viewModel.isFetchingGps.collectAsState()
        val context = LocalContext.current

        val hasFineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
            if (fineGranted || coarseGranted) {
                viewModel.fetchIntakeLocation()
            }
        }

        Column(modifier = Modifier.padding(start = 32.dp, top = 8.dp, bottom = 8.dp)) {
            if (lat != 0.0 || lng != 0.0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Latitude: $lat", style = MaterialTheme.typography.bodyMedium)
                        Text("Longitude: $lng", style = MaterialTheme.typography.bodyMedium)
                        Text("Accuracy: ${String.format("%.1f", acc)} meters", style = MaterialTheme.typography.bodyMedium)
                        Text("Status: $status", style = MaterialTheme.typography.bodySmall, color = GreenSuccess)
                    }
                }
            } else {
                Text("Coordinates not captured yet.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (hasFineLocation || hasCoarseLocation) {
                        viewModel.fetchIntakeLocation()
                    } else {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isFetching) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("Capture GPS Coordinates")
                }
            }
        }
    }

    ConsentRow("Withdrawal rights have been explained", withdrawalExplained, onWithdrawalChange)

    Spacer(modifier = Modifier.height(32.dp))
    Button(
        onClick = onNext,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        enabled = mainConsent && withdrawalExplained
    ) {
        Text("Confirm & Start Survey")
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

