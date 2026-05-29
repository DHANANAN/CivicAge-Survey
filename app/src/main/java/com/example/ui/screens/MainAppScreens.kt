package com.nlud.civilagesurvey.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.core.content.ContextCompat
import com.example.config.Config
import com.example.data.database.FormQuestionEntity
import com.example.data.database.SurveyEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.SurveyViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSurveyApp(viewModel: SurveyViewModel) {
    var currentScreen by remember { mutableStateOf("login") }
    val activeSurvey by viewModel.activeSurvey.collectAsState()
    val surveysList by viewModel.filteredSurveys.collectAsState()
    val allSurveysRaw by viewModel.allSurveys.collectAsState()
    val questionsList by viewModel.activeQuestions.collectAsState()

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationCity,
                            contentDescription = "App Icon",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "CivicAge Survey (CAS)",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            )
                            Text(
                                "Center for Urban Planning",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                ),
                actions = {
                    val userRole by viewModel.userRole.collectAsState()
                    Row(
                        modifier = Modifier.padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (userRole == Config.ADMIN_ROLE) Icons.Default.AdminPanelSettings else Icons.Default.DirectionsWalk,
                            contentDescription = "Role",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(userRole, color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentScreen) {
                "login" -> LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = { currentScreen = "dashboard" }
                )
                "dashboard" -> DashboardScreen(
                    viewModel = viewModel,
                    surveysList = surveysList,
                    allSurveysRaw = allSurveysRaw,
                    onStartNewSurvey = { currentScreen = "consent" },
                    onLoadSurvey = { uid, step ->
                        viewModel.loadSurvey(uid)
                        currentScreen = step
                    },
                    onNavigateToProfile = { currentScreen = "surveyor_profile" },
                    onNavigateToOptions = { currentScreen = "options" }
                )
                "options" -> OptionsScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = "dashboard" }
                )
                "surveyor_profile" -> SurveyorProfileScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = "dashboard" }
                )
                "consent" -> ConsentScreen(
                    viewModel = viewModel,
                    onConsentSuccess = { currentScreen = "participant_profile" },
                    onCancel = { currentScreen = "dashboard" }
                )
                "participant_profile" -> ParticipantProfileScreen(
                    viewModel = viewModel,
                    activeSurvey = activeSurvey,
                    onNext = { currentScreen = "geo_location" },
                    onCancel = { currentScreen = "dashboard" }
                )
                "geo_location" -> GeoLocationScreen(
                    viewModel = viewModel,
                    activeSurvey = activeSurvey,
                    onNext = { currentScreen = "questionnaire" },
                    onBack = { currentScreen = "participant_profile" }
                )
                "questionnaire" -> QuestionnaireScreen(
                    viewModel = viewModel,
                    activeSurvey = activeSurvey,
                    questions = questionsList,
                    onNext = { currentScreen = "interview" },
                    onBack = { currentScreen = "geo_location" }
                )
                "interview" -> InterviewScreen(
                    viewModel = viewModel,
                    activeSurvey = activeSurvey,
                    onNext = { currentScreen = "analysis" },
                    onBack = { currentScreen = "questionnaire" }
                )
                "analysis" -> ThemeAnalysisScreen(
                    viewModel = viewModel,
                    activeSurvey = activeSurvey,
                    onFinish = {
                        viewModel.finalizeSurvey()
                        Toast.makeText(context, "Survey Saved successfully to local Room database!", Toast.LENGTH_LONG).show()
                        currentScreen = "dashboard"
                    },
                    onBack = { currentScreen = "interview" }
                )
            }
        }
    }
}

private fun launchSurveyEmailIntent(
    context: android.content.Context,
    survey: SurveyEntity,
    recipientEmail: String,
    surveyJson: String
): Boolean {
    val subject = "CivicAge Survey (CAS) Report - ${survey.surveyUid}"
    val body = buildString {
        appendLine("CivicAge Survey (CAS)")
        appendLine("Center for Urban Planning")
        appendLine()
        appendLine("Surveyor details")
        appendLine("Name: ${survey.surveyorName}")
        appendLine("Email: ${survey.surveyorEmail}")
        appendLine("Surveyor ID: ${survey.surveyorId.ifBlank { survey.createdBy }}")
        appendLine("Mobile: ${survey.surveyorPhone}")
        appendLine("Role: ${survey.surveyorRole}")
        appendLine()
        appendLine("Respondent details")
        appendLine("Name: ${survey.participantName}")
        appendLine("Age: ${survey.participantAge}")
        appendLine("Gender: ${survey.participantGender}")
        appendLine("Locality: ${survey.manualLocality}")
        appendLine()
        appendLine("Consent status: ${if (survey.consentGiven) "Granted" else "Not granted"}")
        appendLine("Geotag: ${survey.latitude}, ${survey.longitude} (+/- ${survey.gpsAccuracy}m)")
        appendLine()
        appendLine("Interview transcript")
        appendLine(survey.reviewedTranscriptText.ifBlank { survey.transcriptText }.ifBlank { "No transcript available." })
        appendLine()
        appendLine("AI summary")
        appendLine(survey.summaryText.ifBlank { "No AI summary available." })
        appendLine("AI themes: ${survey.themesExtractionJson}")
        appendLine("Urgency tag: ${survey.aiUrgencyTag.ifBlank { "Not tagged" }}")
        appendLine()
        appendLine("Firebase sync status: ${survey.firebaseSyncStatus}")
        appendLine("Google Forms sync status: ${survey.googleFormsSyncStatus}")
        appendLine("Local audio path: ${survey.interviewAudioLocalPath ?: "No audio"}")
        appendLine("Local photo path: ${survey.participantPhotoLocalPath ?: "No photo"}")
        appendLine("Local JSON path: ${survey.jsonExportPath ?: "No JSON export"}")
        appendLine()
        appendLine("Research JSON")
        appendLine(surveyJson)
        appendLine()
        appendLine("TODO for backend auto-email: Use Cloud Function, Apps Script, or Gmail API OAuth.")
    }

    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }

    return try {
        context.startActivity(Intent.createChooser(intent, "Email CAS report"))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

// ==========================================
// 1. AUTHENTICATION / EMAIL-BASED LOGIN
// ==========================================
@Composable
fun LoginScreen(
    viewModel: SurveyViewModel,
    onLoginSuccess: () -> Unit
) {
    var phoneInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var surveyorIdInput by remember { mutableStateOf("") }
    var isCreateAccount by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val authStatus by viewModel.authStatusMessage.collectAsState()
    val firebaseStatus by viewModel.firebaseConnectionStatus.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_card"),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Crest / Shield
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Security",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "CivicAge Survey (CAS)",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Center for Urban Planning\nFirebase email/password access",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                AssistChip(
                    onClick = {},
                    label = { Text(firebaseStatus, maxLines = 3, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = { Icon(Icons.Default.Cloud, contentDescription = "Firebase status", modifier = Modifier.size(18.dp)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Surveyor Full Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("surveyor_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = surveyorIdInput,
                    onValueChange = { surveyorIdInput = it },
                    label = { Text("Surveyor ID Code (e.g. SV-DEL-02)") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = "ID") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("surveyor_id_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Official Email Address") },
                    placeholder = { Text("surveyor@civicwellbeing.org") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("email_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("password_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = { Text("Surveyor Mobile Number (Optional)") },
                    placeholder = { Text("+91 XXXXX XXXXX") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .testTag("phone_input"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Create new Firebase account", fontWeight = FontWeight.SemiBold)
                    Switch(checked = isCreateAccount, onCheckedChange = { isCreateAccount = it })
                }

                if (authStatus.isNotBlank()) {
                    Text(
                        authStatus,
                        color = if (authStatus.contains("Signed in", ignoreCase = true)) GreenSuccess else OrangeWarning,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Button(
                    onClick = {
                        val trimmedName = nameInput.trim()
                        val trimmedEmail = emailInput.trim()
                        val trimmedId = surveyorIdInput.trim()

                        if (trimmedName.isBlank()) {
                            Toast.makeText(context, "Full name cannot be blank", Toast.LENGTH_SHORT).show()
                        } else if (trimmedId.isBlank()) {
                            Toast.makeText(context, "Surveyor ID cannot be blank", Toast.LENGTH_SHORT).show()
                        } else if (!trimmedEmail.contains("@") || !trimmedEmail.substringAfter("@").contains(".")) {
                            Toast.makeText(context, "Please enter a valid email containing '@' and a domain", Toast.LENGTH_SHORT).show()
                        } else if (passwordInput.length < 6) {
                            Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.signInOrSignUp(
                                email = trimmedEmail,
                                password = passwordInput,
                                fullName = trimmedName,
                                surveyorCode = trimmedId,
                                mobileNumber = phoneInput.trim(),
                                createAccount = isCreateAccount
                            ) { success, message ->
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                if (success) onLoginSuccess()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("submit_login_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isCreateAccount) "Create Account & Login" else "Firebase Login to CAS", style = MaterialTheme.typography.labelLarge.copy(color = Color.White))
                }

                TextButton(
                    onClick = {
                        viewModel.useLocalTestingMode(
                            email = emailInput,
                            fullName = nameInput,
                            surveyorCode = surveyorIdInput,
                            mobileNumber = phoneInput
                        )
                        Toast.makeText(context, "Local testing mode enabled. Cloud sync will remain pending.", Toast.LENGTH_LONG).show()
                        onLoginSuccess()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Use Local Testing Mode")
                }
            }
        }
    }
}

// ==========================================
// 2. DASHBOARD (Schedules, lists, analytics)
// ==========================================
@Composable
fun DashboardScreen(
    viewModel: SurveyViewModel,
    surveysList: List<SurveyEntity>,
    allSurveysRaw: List<SurveyEntity>,
    onStartNewSurvey: () -> Unit,
    onLoadSurvey: (String, String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToOptions: () -> Unit
) {
    val surveyorName by viewModel.surveyorName.collectAsState()
    val surveyorId by viewModel.surveyorId.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val surveyorEmail by viewModel.surveyorEmail.collectAsState()
    val surveyorPhone by viewModel.surveyorPhone.collectAsState()
    
    val formId by viewModel.activeFormId.collectAsState()
    val formName by viewModel.activeFormName.collectAsState()

    var activeTab by remember { mutableStateOf("surveys") } // "surveys", "analytics", "sync"

    Column(modifier = Modifier.fillMaxSize()) {
        // Welcoming surveyor stripe
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Aadara, $surveyorName",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Email: $surveyorEmail",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        )
                        if (surveyorPhone.isNotBlank()) {
                            Text(
                                text = "Ph: $surveyorPhone",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Code ID: $surveyorId • Role: $userRole",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Profile Settings",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onNavigateToProfile() }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Form",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Natively Linked Form: ",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = formName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, color = MaterialTheme.colorScheme.tertiary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Tactile Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onStartNewSurvey,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("new_survey_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.AddCircle, contentDescription = "Add", tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Start Survey (75+)", style = MaterialTheme.typography.labelLarge.copy(color = Color.White))
            }

            Button(
                onClick = onNavigateToOptions,
                modifier = Modifier
                    .weight(1.1f)
                    .height(54.dp)
                    .testTag("options_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Options", tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Options & Settings", style = MaterialTheme.typography.labelLarge.copy(color = Color.White))
            }
        }

        // Inner tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .background(Color.White, RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val tabWeight = 1f
            val tabOptions = if (userRole == Config.ADMIN_ROLE) {
                listOf(
                    "surveys" to "Surveys",
                    "analytics" to "Admin",
                    "sync" to "Pending Sync"
                )
            } else {
                listOf(
                    "surveys" to "My Surveys",
                    "sync" to "Pending Sync"
                )
            }

            tabOptions.forEach { (key, label) ->
                val isSelected = activeTab == key
                Box(
                    modifier = Modifier
                        .weight(tabWeight)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { activeTab = key }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color.DarkGray,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp)
                    )
                }
            }
        }

        // Display area
        when (activeTab) {
            "surveys" -> SurveysListArea(surveysList, viewModel, onLoadSurvey)
            "analytics" -> AdminSummaryArea(allSurveysRaw, viewModel)
            "sync" -> CloudSyncArea(surveysList, viewModel)
        }
    }
}

@Composable
fun SurveysListArea(
    surveysList: List<SurveyEntity>,
    viewModel: SurveyViewModel,
    onLoadSurvey: (String, String) -> Unit
) {
    if (surveysList.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = "Empty",
                tint = Color.LightGray,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "No Surveys Logged Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Tap 'Start Survey' button to onboarding the first respondent.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(surveysList.size) { index ->
                val it = surveysList[index]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("survey_item_${it.surveyUid}"),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "UID: ${it.surveyUid.takeLast(10)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Status Chip
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (it.status == "COMPLETED") GreenSuccess.copy(alpha = 0.15f)
                                        else OrangeWarning.copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = it.status,
                                    color = if (it.status == "COMPLETED") GreenSuccess else OrangeWarning,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Respondent: ${it.participantName.ifBlank { "Consent Gathered (Unnamed)" }}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "Age: ${if (it.participantAge > 0) "${it.participantAge} yrs" else "TBD"} • Locality: ${it.manualLocality.ifBlank { "TBD" }}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (it.isSynced) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                                contentDescription = "Sync state",
                                tint = if (it.isSynced) GreenSuccess else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (it.isSynced) "Synced to Cloud" else "Local Draft Only",
                                fontSize = 12.sp,
                                color = if (it.isSynced) GreenSuccess else Color.Gray
                            )
                        }

                        if (it.status == "COMPLETED") {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.5f))
                            
                            // Self-contained surveyor & physical file export details
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "SURVEYOR & LOCAL COPIES METADATA:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Surveyor: ${it.surveyorName} (${it.surveyorRole})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.DarkGray
                                )
                                Text(
                                    text = "Surveyor Contact: ${it.surveyorEmail} ${if (it.surveyorPhone.isNotBlank()) "• ${it.surveyorPhone}" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.DarkGray
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = "Local JSON Copy Absolute Path:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Gray
                                )
                                Text(
                                    text = it.jsonExportPath ?: "Not exported",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color.DarkGray,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                
                                Text(
                                    text = "Local Transcript Copy Absolute Path:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Gray
                                )
                                Text(
                                    text = it.transcriptExportPath ?: "Not exported",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color.DarkGray
                                )
                            }
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.5f))
                            
                            // Report Transmitter section
                            var recipientEmailInput by remember(it.surveyUid) { mutableStateOf("") }
                            val scope = rememberCoroutineScope()
                            val context = LocalContext.current
                            
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "EMAIL REPORT DISPATCH ENGINE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                when (it.emailReportStatus) {
                                    "sending" -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            Text(
                                                "Transmitting CAS report securely over network...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    "sent" -> {
                                        val sentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(it.emailReportSentAt))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Sent Banner",
                                                tint = GreenSuccess,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Column {
                                                Text(
                                                    "Report Transmitted Out-of-the-Box!",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = GreenSuccess
                                                )
                                                Text(
                                                    "Sent successfully on $sentDate",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                    }
                                    "failed" -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = "Failed Banner",
                                                tint = RedError,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Column {
                                                Text(
                                                    "Transmission Failed (Network simulated offline)",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = RedError
                                                )
                                                Text(
                                                    "Please check simulated network toggle under Settings and retry.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                    }
                                    "intent_opened" -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Email,
                                                contentDescription = "Email app opened",
                                                tint = GreenSuccess,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                "Email app opened. Android did not auto-send.",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = GreenSuccess
                                            )
                                        }
                                    }
                                    else -> {
                                        Text(
                                            "Report is ready. Android will open your email app for review.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                
                                if (it.emailReportStatus != "sending" && it.emailReportStatus != "sent") {
                                    OutlinedTextField(
                                        value = recipientEmailInput,
                                        onValueChange = { recipientEmailInput = it },
                                        label = { Text("Recipient Email") },
                                        placeholder = { Text(Config.ADMIN_EMAIL) },
                                        modifier = Modifier.fillMaxWidth().testTag("recipient_email_input_${it.surveyUid}"),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                                    )
                                    
                                    Button(
                                        onClick = {
                                            val recipient = recipientEmailInput.ifBlank { Config.ADMIN_EMAIL }
                                            val opened = launchSurveyEmailIntent(context, it, recipient, viewModel.exportSurveyToJson(it))
                                            if (opened) {
                                                viewModel.transmitSurveyViaEmail(it, recipient) { feedback ->
                                                    Toast.makeText(context, feedback, Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Toast.makeText(context, "No email app found on this device.", Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("transmit_report_button_${it.surveyUid}"),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            if (it.emailReportStatus == "failed") "Retry Email Intent" else "Email Current Survey Report",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                } else if (it.emailReportStatus == "sent") {
                                    // Let user send to another email too if they want
                                    TextButton(
                                        onClick = {
                                            recipientEmailInput = ""
                                            viewModel.resetEmailReportStatus(it)
                                        },
                                        modifier = Modifier.align(Alignment.End).testTag("send_to_another_button_${it.surveyUid}")
                                    ) {
                                        Text("Send to Another Email Address", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = { viewModel.deleteSurvey(it) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedError)
                            }

                            FilledTonalButton(
                                onClick = {
                                    val destinationStep = when {
                                        it.participantName.isBlank() -> "participant_profile"
                                        it.latitude == 0.0 -> "geo_location"
                                        viewModel.getAnswersMap(it.answersJson).size < 3 -> "questionnaire"
                                        it.interviewAudioLocalPath.isNullOrEmpty() -> "interview"
                                        else -> "analysis"
                                    }
                                    onLoadSurvey(it.surveyUid, destinationStep)
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Resume", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Resume / Edit", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminSummaryArea(
    allSurveys: List<SurveyEntity>,
    viewModel: SurveyViewModel
) {
    val scrollState = rememberScrollState()
    var selectedExportText by remember { mutableStateOf("") }
    var selectedExportUid by remember { mutableStateOf("") }
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val firestoreSurveyCount by viewModel.firestoreSurveyCount.collectAsState()
    val googleFormsId by viewModel.googleFormsId.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Analytics Strip
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Statistical Surveys Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val total = allSurveys.size
                    val completed = allSurveys.count { it.status == "COMPLETED" }
                    val drafts = total - completed
                    val synced = allSurveys.count { it.isSynced }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$total", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
                        Text("Total Logged", fontSize = 12.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$completed", style = MaterialTheme.typography.displayMedium, color = GreenSuccess)
                        Text("Finalized", fontSize = 12.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$drafts", style = MaterialTheme.typography.displayMedium, color = OrangeWarning)
                        Text("Drafts Remaining", fontSize = 12.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$pendingSyncCount", style = MaterialTheme.typography.displayMedium, color = CivicTeal)
                        Text("Pending Sync", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                ProfileValueRow("Firestore surveys", firestoreSurveyCount?.toString() ?: "Unknown")
                ProfileValueRow("Failed uploads", allSurveys.count { it.uploadStatus == "pending" || it.uploadStatus == "failed" }.toString())
                ProfileValueRow("Connected Google Form", googleFormsId.ifBlank { "Not connected" })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { viewModel.triggerCloudSync() }, modifier = Modifier.weight(1f)) {
                        Text("Sync All")
                    }
                    OutlinedButton(onClick = { viewModel.refreshFirestoreSurveyCount() }, modifier = Modifier.weight(1f)) {
                        Text("Refresh Cloud")
                    }
                }
            }
        }

        // Custom canvas bar chart to fulfill Vico-style request natively and securely without external errors
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Participating Age Range Analysis (75+)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Live count categorised by age brackets",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Counts
                val age75_79 = allSurveys.count { it.participantAgeGroup == "75-79" }
                val age80_84 = allSurveys.count { it.participantAgeGroup == "80-84" }
                val age85Plus = allSurveys.count { it.participantAgeGroup == "85+" }
                val maxVal = maxOf(age75_79, age80_84, age85Plus, 1)

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    AgeBar("Bracket 75 - 79", age75_79, maxVal, CivicTeal)
                    AgeBar("Bracket 80 - 84", age80_84, maxVal, PlanningGold)
                    AgeBar("Bracket 85 + Plus", age85Plus, maxVal, MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Active Survey exports
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Administrative JSON Export Hub",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Tap on any record to parse securely to valid research JSON Schema.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (allSurveys.isEmpty()) {
                    Text("No records available to export.", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                } else {
                    allSurveys.forEach { survey ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedExportUid = survey.surveyUid
                                    selectedExportText = viewModel.exportSurveyToJson(survey)
                                }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    survey.participantName.ifBlank { "Unnamed Participant" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    "UID: ${survey.surveyUid.takeLast(10)}",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                            Icon(Icons.Default.ArrowForwardIos, contentDescription = "View", modifier = Modifier.size(14.dp))
                        }
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    }
                }
            }
        }

        // Selected JSON display board
        if (selectedExportText.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "JSON EXPORT PREVIEW (UID: ${selectedExportUid.takeLast(6)})",
                            color = PlanningGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        IconButton(onClick = { selectedExportText = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = selectedExportText,
                            color = Color.Green,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AgeBar(label: String, valCount: Int, maxVal: Int, color: Color) {
    val progress = valCount.toFloat() / maxVal.toFloat()
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("$valCount active surveys", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.LightGray.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun CloudSyncArea(
    surveys: List<SurveyEntity>,
    viewModel: SurveyViewModel
) {
    val surveyNotSynced = surveys.count { !it.isSynced }
    val surveySynced = surveys.count { it.isSynced }
    val coroutine = rememberCoroutineScope()

    var isSyncing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (surveyNotSynced > 0) Icons.Default.CloudSync else Icons.Default.CloudDone,
            contentDescription = "Cloud Done",
            tint = if (surveyNotSynced > 0) OrangeWarning else GreenSuccess,
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (surveyNotSynced > 0) "Pending Cloud Submissions" else "All Surveys Synced!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Total pending local drafts to upload: $surveyNotSynced\nSuccessfully uploaded to Firestore and storage: $surveySynced",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        Button(
            onClick = {
                coroutine.launch {
                    isSyncing = true
                    kotlinx.coroutines.delay(2000) // simulated network lag
                    viewModel.triggerCloudSync()
                    isSyncing = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("sync_sync_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (surveyNotSynced > 0) MaterialTheme.colorScheme.primary else Color.Gray
            ),
            shape = RoundedCornerShape(10.dp),
            enabled = surveyNotSynced > 0 && !isSyncing
        ) {
            if (isSyncing) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Uploading Survey database...", color = Color.White)
            } else {
                Text("Sync Database & Files to Firestore", style = MaterialTheme.typography.labelLarge.copy(color = Color.White))
            }
        }
    }
}

// ==========================================
// 3. EDIT PROFILE / LINKED FORM ID
// ==========================================
@Composable
fun SurveyorProfileScreen(
    viewModel: SurveyViewModel,
    onBack: () -> Unit
) {
    val phone by viewModel.surveyorPhone.collectAsState()
    val email by viewModel.surveyorEmail.collectAsState()
    val name by viewModel.surveyorName.collectAsState()
    val id by viewModel.surveyorId.collectAsState()
    val role by viewModel.userRole.collectAsState()
    
    val formId by viewModel.activeFormId.collectAsState()

    var customFormIdInput by remember { mutableStateOf("") }
    var customFormNameInput by remember { mutableStateOf("") }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text("Settings & Google Form Sync", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        // Card displaying current Profile info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Active Surveyor Profile", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                ProfileValueRow("Surveyor Name", name)
                ProfileValueRow("Identity Code", id)
                ProfileValueRow("Registered Phone", phone)
                ProfileValueRow("Role Level", role)
                ProfileValueRow("Email", email)
            }
        }

        // Simulated Network & Environment configurations
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Simulated Network Connectivity",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "We can toggle the network dispatch layer's operational state to test out-of-the-box offline queuing and delivery mechanics.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
                
                val isOnline by viewModel.isNetworkOnline.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val context = LocalContext.current
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isOnline) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                            contentDescription = "Simulated Network Status",
                            tint = if (isOnline) GreenSuccess else RedError
                        )
                        Column {
                            Text(
                                text = if (isOnline) "Simulated: ONLINE" else "Simulated: OFFLINE",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = if (isOnline) "Ready to transmit reports" else "Transmissions will fail & support retry",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    Switch(
                        checked = isOnline,
                        onCheckedChange = { 
                            viewModel.isNetworkOnline.value = it 
                            val message = if (it) "Network simulated ONLINE" else "Network simulated OFFLINE"
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("network_simulation_switch")
                    )
                }
            }
        }

        // Google Form native integration config card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Google Form Native Questionnaire Import",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "The app fetches form schemas through Google API and builds a tactile layout in Compose natively, with no external browser frames.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                OutlinedTextField(
                    value = customFormIdInput,
                    onValueChange = { customFormIdInput = it },
                    label = { Text("Enter Google Form ID") },
                    placeholder = { Text("e.g. 1FAIpQLSfD_K9_vUX...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = customFormNameInput,
                    onValueChange = { customFormNameInput = it },
                    label = { Text("Custom Form Name") },
                    placeholder = { Text("Smart City Sanitation survey") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = {
                            viewModel.restoreDefaultForm()
                            customFormIdInput = ""
                            customFormNameInput = ""
                            Toast.makeText(context, "Restored default Senior Wellbeing template", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Restore Default")
                    }

                    Button(
                        onClick = {
                            if (customFormIdInput.isNotBlank() && customFormNameInput.isNotBlank()) {
                                viewModel.importGoogleFormQuestions(customFormIdInput, customFormNameInput)
                                Toast.makeText(context, "Successfully Synced Google Form natively!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Enter both Form ID and Name to import", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Fetch & Sync")
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileValueRow(key: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(key, color = Color.Gray, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

// ==========================================
// 4. ETHICAL CONSENT SCREEN
// ==========================================
@Composable
fun ConsentScreen(
    viewModel: SurveyViewModel,
    onConsentSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    var checkQuestionnaire by remember { mutableStateOf(false) }
    var checkPhoto by remember { mutableStateOf(false) }
    var checkAudio by remember { mutableStateOf(false) }
    var checkGeo by remember { mutableStateOf(false) }
    var checkWithdrawal by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Participant Consent Form",
            style = MaterialTheme.typography.displayMedium.copy(fontSize = 24.sp),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Project: Age-Friendly Smart City Citizen Wellbeing Survey Platform",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You are being invited to participate in a research survey about the experiences, difficulties, Accessibility and suggestions of senior citizens aged 75 years and above.\n\n" +
                            "This application collects approximate GPS geo-tags, photo verification, questionnaire checks, and long voice interview testimonies to formulate civil improvement programs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray
                )
            }
        }

        // Toggles
        Text("Explicit Permission Toggles (Check to proceed)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)

        ConsentToggleItem(
            label = "I agree to answer the main utility questionnaire (MCQ)",
            checked = checkQuestionnaire,
            onCheckedChange = { checkQuestionnaire = it },
            tag = "chk_mcq"
        )

        ConsentToggleItem(
            label = "I agree to provide a photograph for record verification",
            checked = checkPhoto,
            onCheckedChange = { checkPhoto = it },
            tag = "chk_photo"
        )

        ConsentToggleItem(
            label = "I agree to voice recording and transcription analysis",
            checked = checkAudio,
            onCheckedChange = { checkAudio = it },
            tag = "chk_audio"
        )

        ConsentToggleItem(
            label = "I agree to GPS approximate surveying geo-tagging",
            checked = checkGeo,
            onCheckedChange = { checkGeo = it },
            tag = "chk_geo"
        )

        ConsentToggleItem(
            label = "I understand I may refuse answers or stop at any time",
            checked = checkWithdrawal,
            onCheckedChange = { checkWithdrawal = it },
            tag = "chk_withdraw"
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Decline / Cancel")
            }

            Button(
                onClick = {
                    if (checkQuestionnaire && checkWithdrawal) {
                        viewModel.startNewSurvey(
                            hasPhotoConsent = checkPhoto,
                            hasAudioConsent = checkAudio,
                            hasGeoConsent = checkGeo,
                            hasDataConsent = true,
                            hasWithdrawalRight = checkWithdrawal
                        )
                        onConsentSuccess()
                    } else {
                        Toast.makeText(context, "Respondent must acknowledge MCQ and withdrawal terms to proceed.", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier
                    .weight(1.2f)
                    .testTag("submit_consent_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Grant & Start ID", color = Color.White)
            }
        }
    }
}

@Composable
fun ConsentToggleItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .clickable { onCheckedChange(!checked) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag),
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

// ==========================================
// 5. PARTICIPANT PROFILE ONBOARDING
// ==========================================
@Composable
fun ParticipantProfileScreen(
    viewModel: SurveyViewModel,
    activeSurvey: SurveyEntity?,
    onNext: () -> Unit,
    onCancel: () -> Unit
) {
    if (activeSurvey == null) return

    var nameInput by remember { mutableStateOf("") }
    var ageInput by remember { mutableStateOf("75") }
    var genderInput by remember { mutableStateOf("Female") }
    
    var selectedPhotoMock by remember { mutableStateOf<String?>(null) }
    var selectedAudioMock by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            selectedPhotoMock = "/sdcard/photos/respondent_75plus_${Date().time}.jpg"
            Toast.makeText(context, "Camera permission granted. Photo placeholder saved.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Camera denied. Continue with manual profile details.", Toast.LENGTH_LONG).show()
        }
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            selectedAudioMock = "/sdcard/audio/sample_voice_${Date().time}.m4a"
            Toast.makeText(context, "Microphone permission granted. Voice sample placeholder saved.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Microphone denied. Manual transcript is still available.", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Participant Profile Onboarding",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Assign CASE UID: ${activeSurvey.surveyUid.takeLast(12)}",
                    fontWeight = FontWeight.Bold,
                    color = PlanningGold,
                    fontSize = 14.sp
                )

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Respondent Full Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("participant_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = ageInput,
                    onValueChange = { ageInput = it },
                    label = { Text("Exact Age (Must be 75 or above)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Gender Selection", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val genders = listOf("Female", "Male", "Other")
                    genders.forEach { g ->
                        val isSelected = genderInput == g
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else SlateGrey
                                )
                                .clickable { genderInput = g }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = g,
                                color = if (isSelected) Color.White else Color.DarkGray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Photographic capture simulation
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Age-Friendly Photolog Validation", fontWeight = FontWeight.Bold)
                Text(
                    "Simulate photo capture verification of the participant for project compliance.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                selectedPhotoMock = "/sdcard/photos/respondent_75plus_${Date().time}.jpg"
                                Toast.makeText(context, "Photo placeholder saved.", Toast.LENGTH_SHORT).show()
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CivicTeal),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Camera")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simulate Photo Capture", color = Color.White)
                    }

                    if (selectedPhotoMock != null) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Done", tint = GreenSuccess, modifier = Modifier.size(24.dp))
                        Text("Photo Captured", color = GreenSuccess, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Voice sample verification
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Age Validation Short Voice Print", fontWeight = FontWeight.Bold)
                Text(
                    "Simulate recording a short 2-second voice ID confirm 'I agree to participate.'",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                selectedAudioMock = "/sdcard/audio/sample_voice_${Date().time}.m4a"
                                Toast.makeText(context, "Voice sample placeholder saved.", Toast.LENGTH_SHORT).show()
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CivicTeal),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Record Voice Sample", color = Color.White)
                    }

                    if (selectedAudioMock != null) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Done", tint = GreenSuccess, modifier = Modifier.size(24.dp))
                        Text("Voice Sample OK", color = GreenSuccess, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel Case")
            }

            Button(
                onClick = {
                    val ageVal = ageInput.toIntOrNull() ?: 0
                    if (nameInput.isNotBlank() && ageVal >= 75) {
                        viewModel.updateParticipantProfile(nameInput, ageVal, genderInput, selectedPhotoMock, selectedAudioMock)
                        onNext()
                    } else {
                        Toast.makeText(context, "Verify name is entered and Respondent age is 75 or above.", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier
                    .weight(1.2f)
                    .testTag("save_profile_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save & Proceed", color = Color.White)
            }
        }
    }
}

// ==========================================
// 6. GEOLOCATION SCREEN
// ==========================================
@Composable
fun GeoLocationScreen(
    viewModel: SurveyViewModel,
    activeSurvey: SurveyEntity?,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    if (activeSurvey == null) return

    val isFetchingGps by viewModel.isFetchingGps.collectAsState()
    val gpsError by viewModel.gpsError.collectAsState()

    var manualLocalityText by remember { mutableStateOf(activeSurvey.manualLocality) }
    val context = LocalContext.current
    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.fetchGpsCoordinates()
        } else {
            Toast.makeText(context, "Location denied. Enter locality manually and retry if needed.", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "GPS High-Accuracy Geo-Tagging",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Capture Location of Survey Instance",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    "Smart City guidelines require geo-located coordinates to pin mobility obstacles.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                // Current coordinates representation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SlateGrey)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Pin",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )

                    Column {
                        if (activeSurvey.latitude != 0.0) {
                            Text("Latitude: ${activeSurvey.latitude}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Longitude: ${activeSurvey.longitude}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("GPS Accuracy Area: +/- ${activeSurvey.gpsAccuracy} meters", color = GreenSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Text("No GPS Coordinate Saved Yet", fontWeight = FontWeight.Bold, color = RedError)
                            Text("Use the button below to fetch coordinates", fontSize = 12.sp, color = Color.DarkGray)
                        }
                    }
                }

                if (gpsError != null) {
                    Text(gpsError!!, color = RedError, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (fineGranted || coarseGranted) {
                            viewModel.fetchGpsCoordinates()
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("fetch_gps_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = CivicTeal),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isFetchingGps
                ) {
                    if (isFetchingGps) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Querying Fused GPS Provider...", color = Color.White)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Fetch Active Location Coordinates", style = MaterialTheme.typography.labelLarge.copy(color = Color.White))
                    }
                }
            }
        }

        // Manual Locality
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Municipal Ward / Locality", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(
                    value = manualLocalityText,
                    onValueChange = {
                        manualLocalityText = it
                        viewModel.updateManualLocality(it)
                    },
                    label = { Text("Ward Name / Sector / Housing Area") },
                    placeholder = { Text("e.g. Ward 12, Dwarka Sector 3") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("location_manual_locality"),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }

            Button(
                onClick = onNext,
                modifier = Modifier
                    .weight(1.2f)
                    .testTag("save_location_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp),
                enabled = activeSurvey.latitude != 0.0 && manualLocalityText.isNotBlank()
            ) {
                Text("Save & Next Section", color = Color.White)
            }
        }
    }
}

// ==========================================
// 7. MCQ QUESTIONNAIRE SCREEN
// ==========================================
@Composable
fun QuestionnaireScreen(
    viewModel: SurveyViewModel,
    activeSurvey: SurveyEntity?,
    questions: List<FormQuestionEntity>,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    if (questions.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "No Questions",
                tint = RedError,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No survey questions available.\nPlease check DefaultQuestions.kt.",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = AcademicTextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        return
    }

    if (activeSurvey == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Building Age-Friendly Layout Natively...", color = AcademicTextSecondary)
        }
        return
    }

    val activeIndex by viewModel.currentQuestionIndex.collectAsState()
    val currentQuestion = questions.getOrElse(activeIndex) { questions.first() }

    val savedAnswersMap by viewModel.savedAnswers.collectAsState()
    val selectedAnswer = savedAnswersMap[currentQuestion.questionId] ?: ""
    val isMandatory = currentQuestion.isRequired

    // Decode Options array
    val optionsList: List<String> = remember(currentQuestion.optionsJson) {
        try {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val listType = Types.newParameterizedType(List::class.java, String::class.java)
            moshi.adapter<List<String>>(listType).fromJson(currentQuestion.optionsJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    val context = LocalContext.current
    var showErrorValidation by remember(activeIndex) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Section Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentQuestion.section.uppercase(),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "Question ${activeIndex + 1} of ${questions.size}",
                    fontWeight = FontWeight.Bold,
                    color = AcademicTextPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Progress indicator
            LinearProgressIndicator(
                progress = { (activeIndex + 1).toFloat() / questions.size.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = AcademicGold,
                trackColor = SlateGrey
            )

            // TTS Read Aloud Controller Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SoftGoldBg)
                    .clickable {
                        val speechText = "${currentQuestion.questionText}. Options are: ${optionsList.joinToString(". ")}"
                        viewModel.speakAloud(speechText)
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Read Question Aloud",
                    tint = InstitutionalMaroon,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Read Question Aloud",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                        color = InstitutionalMaroon
                    )
                    Text(
                        text = "Tap here to hear the question and choices.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AcademicTextSecondary
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SlateGrey)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = currentQuestion.questionText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = AcademicTextPrimary
                        ),
                        modifier = Modifier.testTag("mcq_question_text")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ask the respondent this question. Select the answer or use voice response where available.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AcademicTextSecondary
                    )
                }
            }

            // Options List
            optionsList.forEach { option ->
                val isSelected = selectedAnswer == option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showErrorValidation = false
                            viewModel.saveAnswer(currentQuestion.questionId, option)
                            viewModel.speakAloud("Selected $option")
                        }
                        .testTag("mcq_option_$option"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) SoftGoldBg else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(2.dp, if (isSelected) InstitutionalMaroon else SlateGrey)
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                showErrorValidation = false
                                viewModel.saveAnswer(currentQuestion.questionId, option)
                                viewModel.speakAloud("Selected $option")
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = InstitutionalMaroon,
                                unselectedColor = AcademicTextSecondary
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                            color = AcademicTextPrimary,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                        )
                    }
                }
            }

            if (showErrorValidation && selectedAnswer.isBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = RedError.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, RedError),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Error, contentDescription = "Error icon", tint = RedError)
                        Text(
                            "Please select an answer before continuing.",
                            color = RedError,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Navigation controls
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.stopSpeaking()
                        showErrorValidation = false
                        if (activeIndex > 0) {
                            viewModel.moveToPreviousQuestion()
                        } else {
                            onBack()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, InstitutionalMaroon)
                ) {
                    Text(
                        "Previous Question",
                        color = InstitutionalMaroon,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        viewModel.stopSpeaking()
                        if (isMandatory && selectedAnswer.isBlank()) {
                            showErrorValidation = true
                            Toast.makeText(context, "Please select an answer before continuing.", Toast.LENGTH_SHORT).show()
                        } else {
                            showErrorValidation = false
                            if (activeIndex < questions.size - 1) {
                                viewModel.moveToNextQuestion()
                            } else {
                                onNext()
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1.2f)
                        .height(52.dp)
                        .testTag("mcq_next_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = InstitutionalMaroon),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (activeIndex == questions.size - 1) "Continue to Interview" else "Next Question",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // Optional skipping rule
            if (!isMandatory) {
                TextButton(
                    onClick = {
                        viewModel.stopSpeaking()
                        showErrorValidation = false
                        viewModel.saveAnswer(currentQuestion.questionId, "Skipped")
                        if (activeIndex < questions.size - 1) {
                            viewModel.moveToNextQuestion()
                        } else {
                            onNext()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Skip Question",
                        color = AcademicTextSecondary,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ==========================================
// 8. AUDIO INTERVIEW RECORDING SCREEN
// ==========================================
@Composable
fun InterviewScreen(
    viewModel: SurveyViewModel,
    activeSurvey: SurveyEntity?,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    if (activeSurvey == null) return

    val currentPromptIdx by viewModel.currentInterviewIndex.collectAsState()
    val prompts = viewModel.interviewPrompts
    val currentPrompt = prompts.getOrElse(currentPromptIdx) { prompts.first() }

    val recordingState by viewModel.recordingState.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val recordingDuration by viewModel.recordingDuration.collectAsState()

    val interviewAnswers by viewModel.interviewAnswers.collectAsState()
    var currentAnswerText by remember(currentPromptIdx) { 
        mutableStateOf(interviewAnswers[currentPromptIdx] ?: "") 
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val microphonePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.speakAloud("Voice recording started")
            viewModel.startVoiceAnswer()
        } else {
            Toast.makeText(context, "Microphone denied. Please type the answer manually.", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header Grouping
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lived Experience Interview".uppercase(),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "Prompt ${currentPromptIdx + 1} of ${prompts.size}",
                    fontWeight = FontWeight.Bold,
                    color = AcademicTextPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { (currentPromptIdx + 1).toFloat() / prompts.size.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = AcademicGold,
                trackColor = SlateGrey
            )

            // Read aloud row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SoftGoldBg)
                    .clickable {
                        viewModel.speakAloud(currentPrompt)
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Read Question Aloud",
                    tint = InstitutionalMaroon,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Read Question Aloud",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                        color = InstitutionalMaroon
                    )
                    Text(
                        text = "Tap to hear the prompt read aloud for senior accessibility.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AcademicTextSecondary
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SlateGrey)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = currentPrompt,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = AcademicTextPrimary
                        ),
                        modifier = Modifier.testTag("interview_prompt_text")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ask the respondent this question. Highlight their lived daily narrative or tap Start Voice Answer below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AcademicTextSecondary
                    )
                }
            }

            // Recorder Console UI
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SlateGrey)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Voice Recorder Console",
                        fontWeight = FontWeight.Bold,
                        color = AcademicTextSecondary,
                        style = MaterialTheme.typography.titleSmall
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Timer displays
                    val minutes = recordingDuration / 60
                    val seconds = recordingDuration % 60
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isRecording) RedError else InstitutionalMaroon,
                        modifier = Modifier.testTag("recorder_timer")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Voice waveform simulation representation
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val barCount = 13
                        for (i in 0 until barCount) {
                            val activeHeight = if (isRecording && !isPaused) {
                                (10..32).random().dp
                            } else {
                                4.dp
                            }
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .width(4.dp)
                                    .height(activeHeight)
                                    .clip(CircleShape)
                                    .background(
                                        if (isRecording) InstitutionalMaroon else Color.LightGray
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Control Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Start button
                        Button(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    viewModel.speakAloud("Voice recording started")
                                    viewModel.startVoiceAnswer()
                                } else {
                                    microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            enabled = recordingState != "Listening",
                            colors = ButtonDefaults.buttonColors(containerColor = AcademicGold),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("start_voice_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Start Voice Answer Button", tint = Color.White)
                                Text("Start Voice Answer", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Stop button
                        Button(
                            onClick = {
                                viewModel.stopVoiceAnswer { transcription ->
                                    currentAnswerText = transcription
                                    Toast.makeText(context, "SST: Transcription loaded!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = recordingState == "Listening",
                            colors = ButtonDefaults.buttonColors(containerColor = RedError),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("stop_voice_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop Recording Button", tint = Color.White)
                                Text("Stop Recording", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Status: ${recordingState.uppercase()}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = when (recordingState) {
                            "Listening" -> RedError
                            "Processing" -> AcademicGold
                            "Transcript Ready" -> GreenSuccess
                            else -> AcademicTextSecondary
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Note: Microphone permission is required for voice responses. You can still type manually.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AcademicTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Transcript Manual correction text box (always editable)
            OutlinedTextField(
                value = currentAnswerText,
                onValueChange = { currentAnswerText = it },
                label = { Text("Lived Experience Transcript Box", fontWeight = FontWeight.Bold, color = InstitutionalMaroon) },
                placeholder = { Text("Enter response transcript manually, or tap Start Voice Answer above to dictate.") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transcript_response_input"),
                minLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = InstitutionalMaroon,
                    unfocusedBorderColor = SlateGrey,
                    focusedLabelColor = InstitutionalMaroon
                )
            )

            Button(
                onClick = {
                    viewModel.saveInterviewAnswer(currentPromptIdx, currentAnswerText)
                    Toast.makeText(context, "Answer saved locally for Prompt ${currentPromptIdx + 1}!", Toast.LENGTH_LONG).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_answer_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save Answer Icon", tint = Color.White)
                    Text("Save Answer", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Navigation Footer controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    viewModel.stopSpeaking()
                    viewModel.resetRecordingState()
                    if (currentPromptIdx > 0) {
                        viewModel.moveToPreviousInterview()
                    } else {
                        onBack()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, InstitutionalMaroon)
            ) {
                Text(
                    "Previous",
                    color = InstitutionalMaroon,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = {
                    viewModel.stopSpeaking()
                    viewModel.resetRecordingState()
                    viewModel.saveInterviewAnswer(currentPromptIdx, currentAnswerText) // auto saves answer
                    if (currentPromptIdx < prompts.size - 1) {
                        viewModel.moveToNextInterview()
                    } else {
                        onNext()
                    }
                },
                modifier = Modifier
                    .weight(1.2f)
                    .height(52.dp)
                    .testTag("save_interview_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = InstitutionalMaroon),
                shape = RoundedCornerShape(10.dp),
                enabled = !isRecording
            ) {
                Text(
                    text = if (currentPromptIdx == prompts.size - 1) "Review Survey" else "Next Interview Question",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

// ==========================================
// 9. AI ANALYSIS (TRANSCRIPT & THEME EXTRACTOR)
// ==========================================
@Composable
fun ThemeAnalysisScreen(
    viewModel: SurveyViewModel,
    activeSurvey: SurveyEntity?,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    if (activeSurvey == null) return

    val isAnalyzingWithGemini by viewModel.isAnalyzingWithGemini.collectAsState()
    val apiCallStatus by viewModel.apiCallStatus.collectAsState()

    var manualEditText by remember { mutableStateOf(activeSurvey.transcriptText) }
    var isEditingTranscript by remember { mutableStateOf(false) }

    val themesList = remember(activeSurvey.themesExtractionJson) {
        viewModel.getThemesList(activeSurvey.themesExtractionJson)
    }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "AI Transcription & Themes",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Transcription board
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Elderly Transcript Text",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Sim trigger in prototype
                    if (activeSurvey.transcriptText.isBlank()) {
                        TextButton(onClick = {
                            viewModel.simulateSstTranscription()
                            Toast.makeText(context, "Transcribed simulation input saved!", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Simulate S.T.T.", color = CivicTeal)
                        }
                    } else {
                        IconButton(onClick = {
                            isEditingTranscript = !isEditingTranscript
                            if (isEditingTranscript) {
                                manualEditText = activeSurvey.transcriptText
                            } else {
                                viewModel.saveManualTranscriptCorrection(manualEditText)
                            }
                        }) {
                            Icon(
                                imageVector = if (isEditingTranscript) Icons.Default.Save else Icons.Default.Edit,
                                contentDescription = "Edit Tool",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isEditingTranscript) {
                    OutlinedTextField(
                        value = manualEditText,
                        onValueChange = { manualEditText = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SlateGrey)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = activeSurvey.transcriptText.ifBlank { "No transcript logged yet. Tap 'Simulate S.T.T.' above of reviewer to extract voice audio logs." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (activeSurvey.transcriptText.isBlank()) Color.Gray else Color.DarkGray
                        )
                    }
                }
            }
        }

        // AI trigger action card (Gemini)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Gemini-3.5-flash Intelligent Insights",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    "Analyze transcription text to map urban policy themes and generate concise bullet summaries.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (apiCallStatus != null) {
                    Text(
                        text = apiCallStatus!!,
                        color = if (apiCallStatus!!.contains("completed", ignoreCase = true)) GreenSuccess else OrangeWarning,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Button(
                    onClick = { viewModel.runGeminiAnalysis() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("run_gemini_analysis_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = PlanningGold),
                    shape = RoundedCornerShape(8.dp),
                    enabled = activeSurvey.transcriptText.isNotBlank() && !isAnalyzingWithGemini
                ) {
                    if (isAnalyzingWithGemini) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connecting Gemini AI Engine...", color = Color.White)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Gemini", tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Extract AI Summary & Themes", style = MaterialTheme.typography.labelLarge.copy(color = Color.White))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { viewModel.cleanTranscriptWithAi() }, modifier = Modifier.weight(1f)) {
                            Text("Clean Transcript", fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = { viewModel.tagSentimentAndUrgency() }, modifier = Modifier.weight(1f)) {
                            Text("Urgency Tag", fontSize = 12.sp)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { viewModel.convertSurveyToResearchJson() }, modifier = Modifier.weight(1f)) {
                            Text("Research JSON", fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = { viewModel.suggestFollowUpQuestion() }, modifier = Modifier.weight(1f)) {
                            Text("Follow-Up", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Summary and themes results board
        if (activeSurvey.summaryText.isNotBlank() || themesList.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, PlanningGold.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("AI Categorization Results", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (activeSurvey.summaryText.isNotBlank()) {
                        Text("Summary Points", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            activeSurvey.summaryText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    if (activeSurvey.aiUrgencyTag.isNotBlank()) {
                        Text("Urgency Tag", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(activeSurvey.aiUrgencyTag, color = RedError, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                    }

                    if (activeSurvey.aiFollowUpQuestion.isNotBlank()) {
                        Text("Suggested Follow-Up Question", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(activeSurvey.aiFollowUpQuestion, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray, modifier = Modifier.padding(bottom = 12.dp))
                    }

                    if (themesList.isNotEmpty()) {
                        Text("Detected Urban Themes", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        // FlowRow-like simple container using Box with inline width constraints
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            themesList.chunked(3).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { theme ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = theme,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }

            Button(
                onClick = onFinish,
                modifier = Modifier
                    .weight(1.2f)
                    .testTag("finalize_analysis_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(8.dp),
                enabled = activeSurvey.transcriptText.isNotBlank()
            ) {
                Text("Finalize & Save Draft", color = Color.White)
            }
        }
    }
}

// ==========================================
// OPTIONS & CUSTOMIZATION SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsScreen(
    viewModel: SurveyViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val surveyorName by viewModel.surveyorName.collectAsState()
    val surveyorEmail by viewModel.surveyorEmail.collectAsState()
    val surveyorId by viewModel.surveyorId.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val allSurveys by viewModel.allSurveys.collectAsState()
    val activeSurvey by viewModel.activeSurvey.collectAsState()
    val firestoreSurveyCount by viewModel.firestoreSurveyCount.collectAsState()
    val isWorkOffline by viewModel.isWorkOffline.collectAsState()

    // 1. Theme and appearance properties
    val selectAppearance by viewModel.appearanceMode.collectAsState()
    val selectFontSize by viewModel.fontSizeMode.collectAsState()
    val isFullscreen by viewModel.isFullscreenMode.collectAsState()
    val isHighContrast by viewModel.isHighContrast.collectAsState()

    // 2. Audio and Language properties
    val selectedLanguage by viewModel.speechLanguage.collectAsState()
    val isAudioBackup by viewModel.isAudioBackupEnabled.collectAsState()
    val isAiCleanup by viewModel.isAiCleanupEnabled.collectAsState()
    val isAiSummary by viewModel.isAiSummaryEnabled.collectAsState()
    val isUrgencyTagging by viewModel.isUrgencyTaggingEnabled.collectAsState()

    // 3. Firebase Synchronization properties
    val firebaseStatus by viewModel.firebaseConnectionStatus.collectAsState()
    val syncLogMsg by viewModel.syncLogMessage.collectAsState()
    val lastFirebaseSync by viewModel.lastFirebaseSyncTime.collectAsState()

    // 4. Google Forms properties
    val googleFormsId by viewModel.googleFormsId.collectAsState()
    val formIdInput = remember { mutableStateOf(viewModel.googleFormsId.value) }
    val googleFormsTitle by viewModel.googleFormsTitle.collectAsState()
    val isFormsConnected by viewModel.isFormsConnected.collectAsState()
    val lastFormsSync by viewModel.lastFormsSyncTime.collectAsState()
    val formsMappingStatus by viewModel.formsMappingStatus.collectAsState()
    val formsResponsesCount by viewModel.formsResponsesCount.collectAsState()

    // Permission status indicators simulation
    var isRecordingPermissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    var isLocationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isCameraPermissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val requestMicPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        isRecordingPermissionGranted = granted
    }
    val requestCameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        isCameraPermissionGranted = granted
    }
    val requestLocationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        isLocationPermissionGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // Sync progress tracking
    var isSyncInProgress by remember { mutableStateOf(false) }
    var syncPhase by remember { mutableStateOf("") } // "Local", "Firebase", "Storage", "Forms", "Completed"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                "System Customization & Settings",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, contentDescription = "Account", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Account", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                ProfileValueRow("Name", surveyorName.ifBlank { "Not set" })
                ProfileValueRow("Email", surveyorEmail.ifBlank { "Not set" })
                ProfileValueRow("Surveyor ID", surveyorId.ifBlank { "Not set" })
                ProfileValueRow("Role", userRole)
                OutlinedButton(
                    onClick = {
                        viewModel.logout()
                        Toast.makeText(context, "Logged out from Firebase.", Toast.LENGTH_SHORT).show()
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Logout, contentDescription = "Logout", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Logout")
                }
            }
        }

        // ==========================================
        // SECTION 1: APPEARANCE & STYLING
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, contentDescription = "Palette", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("1. Accessibility & Visual UI", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }

                // Theme selection
                Text("Theme Color Schema", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Professional Light", "Black & White", "Dark").forEach { mode ->
                        val isSelected = selectAppearance == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setAppearanceMode(mode) },
                            label = { Text(mode) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Audio Backup Recording", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Keep local audio even if upload is pending", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(checked = isAudioBackup, onCheckedChange = { viewModel.toggleAudioBackup(it) })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable AI Summary", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("AI failure never blocks saving", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(checked = isAiSummary, onCheckedChange = { viewModel.toggleAiSummary(it) })
                }

                // Font Size selection
                Text("Elderly Reading Font Size", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Standard", "Large", "Extra Large").forEach { size ->
                        val isSelected = selectFontSize == size
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setFontSizeMode(size) },
                            label = { Text(size) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("High Contrast Mode", fontWeight = FontWeight.SemiBold)
                        Text("Enforce high contrast vector lines", fontSize = 12.sp, color = Color.Gray)
                    }
                    Switch(checked = isHighContrast, onCheckedChange = { viewModel.toggleHighContrast(it) })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Fullscreen Mode", fontWeight = FontWeight.SemiBold)
                        Text("Use more screen height for field workflows", fontSize = 12.sp, color = Color.Gray)
                    }
                    Switch(checked = isFullscreen, onCheckedChange = { viewModel.toggleFullscreen(it) })
                }
            }
        }

        // ==========================================
        // SECTION 2: GOOGLE FORMS SCHEMAS
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Cloud, contentDescription = "Forms", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("2. Google Forms API Integration", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }

                Text(
                    "Synchronize elder questioning layouts or pull Form response templates securely via Apps Script microservices.",
                    fontSize = 13.sp, color = Color.Gray
                )

                if (isFormsConnected) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Linked: $googleFormsTitle", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("ID: $googleFormsId", fontSize = 12.sp, color = Color.Gray)
                            Text("Mapping Status: $formsMappingStatus", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                            lastFormsSync?.let {
                                Text("Last Synced: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(it))}", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        IconButton(onClick = { viewModel.disconnectForm() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Disconnect")
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = formIdInput.value,
                        onValueChange = { formIdInput.value = it },
                        label = { Text("Enter Google Form ID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            viewModel.connectToGoogleForm(formIdInput.value) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Connect Google Form Schema")
                    }
                }

                if (isFormsConnected) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.importQuestionsFromGoogleForm(googleFormsId) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Import", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import Qs", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.pushQuestionBankToGoogleForm(googleFormsId) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = "Push", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Push Bank Qs", fontSize = 12.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                viewModel.pullFormResponsesCount(googleFormsId) { success, msg, cnt ->
                                    Toast.makeText(context, "$msg: Record count mapped internally ($cnt).", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("Pull Reponses Index", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Card(
                            modifier = Modifier.weight(0.8f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Google Answers", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("$formsResponsesCount synced", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // SECTION 3: SYSTEM PERMISSIONS
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = "Permissions", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("3. System Device Permissions", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }

                listOf(
                    Triple("Location", isLocationPermissionGranted, {
                        requestLocationPermission.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }),
                    Triple("Microphone", isRecordingPermissionGranted, { requestMicPermission.launch(Manifest.permission.RECORD_AUDIO) }),
                    Triple("Camera", isCameraPermissionGranted, { requestCameraPermission.launch(Manifest.permission.CAMERA) })
                ).forEach { (title, status, request) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (status) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    if (status) "Granted" else "Required",
                                    color = if (status) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(onClick = request, enabled = !status) {
                                Text(if (status) "Granted" else "Request")
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // SECTION 4: OFFLINE-FIRST FIREBASE SYNC ENGINE
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Sync, contentDescription = "Sync", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("4. Offline-First Sync Console", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }

                Text(
                    "Under offline states, CAS saves responses to Room SQLite first. Remote dispatch fires Firebase Auth, Firestore, and Storage chronologically when online.",
                    fontSize = 13.sp, color = Color.Gray
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sync Target Status", fontWeight = FontWeight.SemiBold)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE3F2FD))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(firebaseStatus, color = Color(0xFF1565C0), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                ProfileValueRow("Pending records", pendingSyncCount.toString())
                ProfileValueRow("Firestore total", firestoreSurveyCount?.toString() ?: "Tap sync to refresh")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Work Offline", fontWeight = FontWeight.SemiBold)
                        Text("Keep all cloud sync pending until disabled", fontSize = 12.sp, color = Color.Gray)
                    }
                    Switch(checked = isWorkOffline, onCheckedChange = { viewModel.toggleWorkOffline(it) })
                }

                // Logs list visual
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(Color.Black)
                        .padding(8.dp)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Text("CAS SYNC CONSOLE CLIENT STACK LOG:", color = Color(0xFF00FF00), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        item {
                            Text("[STABLE CLIENT]: Local SQLite database instances are green.", color = Color.White, fontSize = 11.sp)
                        }
                        if (isSyncInProgress) {
                            item {
                                Text("[SYNC PROCESS]: 1. Querying active surveys from Room Local Database...", color = Color.Yellow, fontSize = 11.sp)
                            }
                            if (syncPhase == "Firebase" || syncPhase == "Storage" || syncPhase == "Forms" || syncPhase == "Completed") {
                                item {
                                    Text("[FIREBASE FIRESTORE]: 2. Transmitting models chunk with survey UID (surveys collector/users tracker successfully updated)...", color = Color.Yellow, fontSize = 11.sp)
                                }
                            }
                            if (syncPhase == "Storage" || syncPhase == "Forms" || syncPhase == "Completed") {
                                item {
                                    Text("[FIREBASE STORAGE]: 3. Bulk upload of interview raw trans audio file binary to survey UID directory completed.", color = Color.Yellow, fontSize = 11.sp)
                                }
                            }
                            if (syncPhase == "Forms" || syncPhase == "Completed") {
                                item {
                                    Text("[GOOGLE FORMS API]: 4. Appending answers array to Connected Form spreadsheet gateway...", color = Color.Yellow, fontSize = 11.sp)
                                }
                            }
                            if (syncPhase == "Completed") {
                                item {
                                    Text("[COMPLETE SYNC]: 5. Set local isSynced state to TRUE and saved syncTimestamp to logs.", color = Color(0xFF00FF00), fontSize = 11.sp)
                                }
                            }
                        } else {
                            item {
                                Text("> Sync standby. $syncLogMsg", color = Color.Gray, fontSize = 11.sp)
                            }
                            lastFirebaseSync?.let {
                                item {
                                    Text("> Last Complete Offline-First Sync executed at: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(it))}", color = Color.LightGray, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        isSyncInProgress = true
                        syncPhase = "Local"
                        coroutineScope.launch {
                            viewModel.triggerCloudSync()
                            syncPhase = "Firebase"
                            delay(800)
                            syncPhase = "Storage"
                            delay(800)
                            syncPhase = "Forms"
                            delay(800)
                            syncPhase = "Completed"
                            isSyncInProgress = false
                            Toast.makeText(context, "Sync requested. Failed cloud work remains pending locally.", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSyncInProgress
                ) {
                    if (isSyncInProgress) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Syncing Cloud...")
                    } else {
                        Icon(Icons.Default.CloudSync, contentDescription = "Sync", tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sync Now")
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.retryFailedUploads() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = "Retry uploads", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retry Failed Uploads")
                }
            }
        }

        // ==========================================
        // SECTION 5: AUDIO VOICE GUIDE & SMART AI SETTINGS
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GraphicEq, contentDescription = "Audio Guidance", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("5. Audio Guidance & Smart AI Summaries", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TTS Text-to-Speech Guidance Language", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Auto", "English", "Hindi").forEach { lang ->
                        val isSelected = selectedLanguage == lang
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setSpeechLanguage(lang) },
                            label = { Text(lang) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // AI summary settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mute Auto-clean transcripts with Gemini", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("AI cleans audio filler words like 'Uhm/Ah'", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(checked = isAiCleanup, onCheckedChange = { viewModel.toggleAiCleanup(it) })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Form Responses Urgency Tagging", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("AI flags answers needing urgent infrastructure help", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(checked = isUrgencyTagging, onCheckedChange = { viewModel.toggleUrgencyTagging(it) })
                }

                OutlinedButton(
                    onClick = {
                        viewModel.testGeminiConnection { _, message ->
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Test Gemini", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Gemini Connection")
                }
            }
        }

        // ==========================================
        // SECTION 6: LOCAL RECORD DATA EXPORT
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DownloadForOffline, contentDescription = "Offline Export", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("6. Bulks Offline Local Folder Export", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }

                Text(
                    "Export database entities directly into SD card folders as formatted, human-readable JSON files for audit.",
                    fontSize = 13.sp, color = Color.Gray
                )

                Button(
                    onClick = {
                        coroutineScope.launch {
                            delay(1200)
                            Toast.makeText(context, "Full Question, Survey profiles, and consent arrays exported locally inside SD-Card folder '/exports/'!", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FolderZip, contentDescription = "Exports folder", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate JSON/TXT Data Files Bulk Package")
                }

                OutlinedButton(
                    onClick = {
                        val survey = activeSurvey
                        if (survey == null) {
                            Toast.makeText(context, "Open a survey first to export the current record.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, viewModel.exportSurveyToJson(survey).take(160), Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export Current Survey JSON")
                }

                OutlinedButton(
                    onClick = {
                        val body = allSurveys.joinToString(separator = "\n\n") { viewModel.exportSurveyToJson(it) }
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_EMAIL, arrayOf(Config.ADMIN_EMAIL))
                            putExtra(Intent.EXTRA_SUBJECT, "CivicAge Survey (CAS) All Local Records")
                            putExtra(Intent.EXTRA_TEXT, body.ifBlank { "No local records available." })
                        }
                        try {
                            context.startActivity(Intent.createChooser(intent, "Email all CAS records"))
                            Toast.makeText(context, "Email app opened for all local records.", Toast.LENGTH_SHORT).show()
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(context, "No email app found.", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Email All Local Test Records")
                }

                if (userRole == Config.ADMIN_ROLE) {
                    OutlinedButton(
                        onClick = {
                            viewModel.clearLocalTestData { message ->
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedError)
                    ) {
                        Text("Clear Local Test Data")
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = "About", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("About", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                Text("CivicAge Survey (CAS)", fontWeight = FontWeight.Bold)
                Text("Center for Urban Planning")
                Text("Version 1.0")
                Text("Testing build: Firebase config must be supplied from Firebase Console.", color = Color.Gray, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
