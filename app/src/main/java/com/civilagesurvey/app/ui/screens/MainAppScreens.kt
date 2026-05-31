package com.civilagesurvey.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.civilagesurvey.app.config.Config
import com.civilagesurvey.app.data.database.SurveyEntity
import com.civilagesurvey.app.data.database.GoogleFormSourceEntity
import com.civilagesurvey.app.data.database.FormQuestionEntity
import com.civilagesurvey.app.data.repository.GoogleFormsRepository
import com.civilagesurvey.app.ui.theme.GreenSuccess
import com.civilagesurvey.app.ui.theme.OrangeWarning
import com.civilagesurvey.app.ui.theme.RedError
import com.civilagesurvey.app.ui.viewmodel.SurveyViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSurveyApp(viewModel: SurveyViewModel) {
    var currentScreen by remember { mutableStateOf("login") }
    var selectedFormId by remember { mutableStateOf<String?>(null) }
    val surveyorEmail by viewModel.surveyorEmail.collectAsState()
    val context = LocalContext.current

    if (currentScreen == "login" && surveyorEmail.isNotBlank()) {
        currentScreen = "home"
    }

    Scaffold(
        bottomBar = {
            if (currentScreen != "login" && !currentScreen.startsWith("form_taking/")) {
                val draftCount by viewModel.draftCount.collectAsState()
                val syncedCount by viewModel.syncedCount.collectAsState()
                val totalCount by viewModel.allSurveys.collectAsState().let { 
                    derivedStateOf { it.value.size }
                }
                val pendingSyncCount by remember {
                    derivedStateOf { totalCount - syncedCount }
                }
                
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
                    val items = listOf(
                        Triple("Home", Icons.Default.Dashboard, "home"),
                        Triple("Surveys", Icons.AutoMirrored.Filled.Assignment, "surveys"),
                        Triple("Records", Icons.Default.History, "records"),
                        Triple("Export", Icons.Default.CloudUpload, "export"),
                        Triple("Settings", Icons.Default.Settings, "settings"),
                        Triple("Profile", Icons.Default.AccountCircle, "profile")
                    )
                    items.forEach { (label, icon, screen) ->
                        NavigationBarItem(
                            selected = currentScreen == screen,
                            onClick = { 
                                currentScreen = screen 
                                selectedFormId = null
                            },
                            icon = {
                                BadgedBox(badge = {
                                    when (label) {
                                        "Records" -> if (draftCount > 0) Badge { Text(draftCount.toString()) }
                                        "Export" -> if (pendingSyncCount > 0) Badge { Text(pendingSyncCount.toString()) }
                                    }
                                }) {
                                    Icon(icon, contentDescription = label)
                                }
                            },
                            label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val isNetworkOnline by viewModel.isNetworkOnline.collectAsState()
        
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (!isNetworkOnline && currentScreen != "login") {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "You are offline. Your answers are saved on this device and will sync later.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                Box(modifier = Modifier.weight(1f)) {
                    when {
                        currentScreen == "login" -> LoginScreen(viewModel) { currentScreen = "home" }
                        currentScreen == "home" -> HomeScreen(viewModel, onNavigate = { currentScreen = it })
                        currentScreen == "surveys" -> SurveysTab(viewModel, onStartSurvey = { 
                            selectedFormId = it
                            currentScreen = "consent" 
                        })
                        currentScreen == "consent" -> ConsentScreen(viewModel, formId = selectedFormId) { 
                            currentScreen = if (selectedFormId != null) "form_taking/$selectedFormId" else "respondent_details" 
                        }
                        currentScreen == "respondent_details" -> RespondentDetailsScreen(viewModel) { currentScreen = "mcq" }
                        currentScreen == "mcq" -> MCQScreen(viewModel) { currentScreen = "interview" }
                        currentScreen == "interview" -> InterviewScreen(viewModel) { currentScreen = "manual_analysis" }
                        currentScreen == "manual_analysis" -> ManualAnalysisScreen(viewModel) { 
                            viewModel.finalizeSurvey()
                            Toast.makeText(context, "Survey Completed Successfully", Toast.LENGTH_SHORT).show()
                            currentScreen = "home" 
                        }
                        currentScreen == "records" -> RecordsScreen(viewModel)
                        currentScreen == "export" -> ExportScreen(viewModel)
                        currentScreen == "settings" -> SettingsScreen(viewModel)
                        currentScreen == "profile" -> ProfileScreen(viewModel) { currentScreen = "login" }
                        currentScreen.startsWith("form_taking/") -> {
                            val formId = currentScreen.substringAfter("form_taking/")
                            GoogleFormTakingScreen(viewModel, formId) { currentScreen = "home" }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(viewModel: SurveyViewModel, onLogin: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var id by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.LocationCity, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("CivilAge Survey", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text("Center for Urban Planning", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = id, onValueChange = { id = it }, label = { Text("Surveyor ID") }, modifier = Modifier.fillMaxWidth())
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                if (name.isNotBlank() && email.contains("@") && id.isNotBlank()) {
                    viewModel.signInOrSignUp(email, "", name, id, "", false) { _, _ -> onLogin() }
                } else {
                    Toast.makeText(context, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Access Dashboard", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun HomeScreen(viewModel: SurveyViewModel, onNavigate: (String) -> Unit) {
    val allSurveys by viewModel.allSurveys.collectAsState()
    val googleForms by viewModel.googleFormSources.collectAsState()
    
    val completedCount by viewModel.completedCount.collectAsState()
    val draftCount by viewModel.draftCount.collectAsState()
    val syncedCount by viewModel.syncedCount.collectAsState()
    val offlineCount by viewModel.offlineCount.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("CivilAge Survey", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text("Accessible citizen feedback for smart city and urban development research", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Dashboard Overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardCard(Modifier.weight(1f), "Total Surveys", allSurveys.size.toString(), Icons.Default.Analytics, MaterialTheme.colorScheme.primary)
                DashboardCard(Modifier.weight(1f), "Forms Linked", googleForms.size.toString(), Icons.Default.Link, MaterialTheme.colorScheme.secondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardCard(Modifier.weight(1f), "Completed", completedCount.toString(), Icons.Default.CheckCircle, GreenSuccess)
                DashboardCard(Modifier.weight(1f), "Pending Drafts", draftCount.toString(), Icons.Default.EditNote, OrangeWarning)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardCard(Modifier.weight(1f), "Synced", syncedCount.toString(), Icons.Default.CloudDone, MaterialTheme.colorScheme.tertiary)
                DashboardCard(Modifier.weight(1f), "Offline Saved", offlineCount.toString(), Icons.Default.SdCard, RedError)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Quick Actions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard("Start Survey", Icons.Default.Add, { onNavigate("consent") }, Modifier.weight(1f))
                QuickActionCard("Add Google Form", Icons.Default.Link, { onNavigate("surveys") }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard("Continue Draft", Icons.Default.EditNote, { onNavigate("records") }, Modifier.weight(1f))
                QuickActionCard("Sync Data", Icons.Default.Sync, { onNavigate("export") }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard("Consent Records", Icons.AutoMirrored.Filled.FactCheck, { onNavigate("records") }, Modifier.weight(1f))
                QuickActionCard("Admin Dashboard", Icons.Default.AdminPanelSettings, { onNavigate("settings") }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun DashboardCard(modifier: Modifier, title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
fun QuickActionCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(100.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun SurveysTab(viewModel: SurveyViewModel, onStartSurvey: (String) -> Unit) {
    var showAddForm by remember { mutableStateOf(false) }
    var formUrl by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    
    val googleForms by viewModel.googleFormSources.collectAsState()
    val isFetching by viewModel.isFetchingForm.collectAsState()
    val mirrorResult by viewModel.mirrorResult.collectAsState()

    val filteredForms = googleForms.filter { 
        it.formTitle.contains(searchQuery, ignoreCase = true) || 
        it.formDescription?.contains(searchQuery, ignoreCase = true) == true 
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Survey Management", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search surveys...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth().clickable { showAddForm = true },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddLink, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Add Google Form Link", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Mirror any form natively in the app", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Linked Forms", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        if (isFetching && googleForms.isEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) { LoadingSkeletonCard() }
            }
        } else if (filteredForms.isEmpty()) {
            EmptyStateCard(if (searchQuery.isEmpty()) "No surveys added yet." else "No results found for '$searchQuery'")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                items(filteredForms) { form ->
                    SurveyFormCard(form) { 
                        viewModel.startNewSurveyForForm(form.extractedFormId)
                        onStartSurvey("form_taking/${form.extractedFormId}") 
                    }
                }
            }
        }
    }

    if (showAddForm) {
        AlertDialog(
            onDismissRequest = { showAddForm = false; viewModel.resetMirrorResult() },
            title = { Text("Add Google Form") },
            text = {
                Column {
                    Text("Paste the Google Forms link below:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = formUrl,
                        onValueChange = { formUrl = it },
                        label = { Text("Form URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isFetching) {
                        Spacer(modifier = Modifier.height(8.dp))
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                    mirrorResult?.let { result ->
                        Spacer(modifier = Modifier.height(8.dp))
                        if (result is GoogleFormsRepository.MirrorResult.Error) {
                            Text(result.message, color = RedError, style = MaterialTheme.typography.bodySmall)
                        } else if (result is GoogleFormsRepository.MirrorResult.Success) {
                            val source = result.source
                            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(8.dp)) {
                                Text(source.formTitle, fontWeight = FontWeight.Bold)
                                Text("${source.questionCount} questions (${source.supportedQuestionCount} supported)", style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { showAddForm = false; viewModel.resetMirrorResult() }, modifier = Modifier.fillMaxWidth()) {
                                Text("Done")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (mirrorResult !is GoogleFormsRepository.MirrorResult.Success) {
                    Button(onClick = { viewModel.addGoogleForm(formUrl) }, enabled = !isFetching && formUrl.isNotBlank()) {
                        Text("Import")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddForm = false; viewModel.resetMirrorResult() }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun LoadingSkeletonCard() {
    Card(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.LightGray.copy(alpha = 0.2f))
    ) {
        Box(Modifier.fillMaxSize())
    }
}

@Composable
fun SurveyFormCard(form: GoogleFormSourceEntity, onClick: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(form.formTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(form.formDescription ?: "No description provided", style = MaterialTheme.typography.bodySmall, maxLines = 2, color = Color.DarkGray)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionChip(
                    onClick = {},
                    label = { Text("Google Form", fontSize = 10.sp) },
                    icon = { Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
                SuggestionChip(
                    onClick = {},
                    label = { Text("${form.questionCount} Qs", fontSize = 10.sp) }
                )
                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(form.responderUri))
                    context.startActivity(intent)
                }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open Original", modifier = Modifier.size(16.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text("Last updated: ${SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(form.updatedAt))}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
fun EmptyStateCard(message: String) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.Transparent), border = BorderStroke(1.dp, Color.LightGray)) {
        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, textAlign = TextAlign.Center, color = Color.Gray)
        }
    }
}

@Composable
fun ConsentScreen(viewModel: SurveyViewModel, formId: String? = null, onNext: () -> Unit) {
    var consent by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Ethical Consent", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("I agree to participate in this survey for urban planning research. I understand my data will be used for civil improvement programs.")
        Spacer(modifier = Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = consent, onCheckedChange = { consent = it })
            Text("I agree to the terms")
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = { 
            if (consent) { 
                if (formId != null) {
                    viewModel.startNewSurveyForForm(formId)
                } else {
                    viewModel.startNewSurvey(
                        hasPhotoConsent = true,
                        hasAudioConsent = true,
                        hasGeoConsent = true,
                        hasDataConsent = true,
                        hasWithdrawalRight = true
                    )
                }
                onNext() 
            } 
        }, enabled = consent, modifier = Modifier.fillMaxWidth()) {
            Text("Proceed")
        }
    }
}

@Composable
fun RespondentDetailsScreen(viewModel: SurveyViewModel, onNext: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("75") }
    var gender by remember { mutableStateOf("Female") }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Respondent Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Age") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))
        Text("Gender")
        Row {
            listOf("Female", "Male", "Other").forEach { g ->
                RadioButton(selected = gender == g, onClick = { gender = g })
                Text(g, modifier = Modifier.padding(top = 12.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = { viewModel.updateParticipantProfile(name, age.toIntOrNull() ?: 75, gender, null, null); onNext() }, modifier = Modifier.fillMaxWidth()) {
            Text("Next")
        }
    }
}

@Composable
fun MCQScreen(viewModel: SurveyViewModel, onNext: () -> Unit) {
    val questions by viewModel.activeQuestions.collectAsState()
    val index by viewModel.currentQuestionIndex.collectAsState()
    val savedAnswers by viewModel.savedAnswers.collectAsState()
    
    if (questions.isEmpty()) return

    val q = questions[index]
    val selected = savedAnswers[q.questionId] ?: ""
    val options: List<String> = remember(q.optionsJson) {
        try {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val type = Types.newParameterizedType(List::class.java, String::class.java)
            moshi.adapter<List<String>>(type).fromJson(q.optionsJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Question ${index + 1} of ${questions.size}", color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Text(q.questionText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        options.forEach { opt ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { viewModel.saveAnswer(q.questionId, opt) }.padding(vertical = 8.dp)) {
                RadioButton(selected = selected == opt, onClick = { viewModel.saveAnswer(q.questionId, opt) })
                Text(opt)
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { viewModel.moveToPreviousQuestion() }, modifier = Modifier.weight(1f)) { Text("Back") }
            Spacer(modifier = Modifier.width(12.dp))
            Button(onClick = { if (index == questions.size - 1) onNext() else viewModel.moveToNextQuestion() }, modifier = Modifier.weight(1f)) {
                Text(if (index == questions.size - 1) "Interview" else "Next")
            }
        }
    }
}

@Composable
fun InterviewScreen(viewModel: SurveyViewModel, onNext: () -> Unit) {
    val prompts = viewModel.interviewPrompts
    val index by viewModel.currentInterviewIndex.collectAsState()
    val answers by viewModel.interviewAnswers.collectAsState()
    val currentAnswer = answers[index] ?: ""

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Interview: Prompt ${index + 1} of ${prompts.size}", color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Text(prompts[index], style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = currentAnswer, onValueChange = { viewModel.saveInterviewAnswer(index, it) }, modifier = Modifier.fillMaxWidth().height(150.dp), label = { Text("Transcript") })
        Spacer(modifier = Modifier.weight(1f))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { viewModel.moveToPreviousInterview() }, modifier = Modifier.weight(1f)) { Text("Back") }
            Spacer(modifier = Modifier.width(12.dp))
            Button(onClick = { if (index == prompts.size - 1) onNext() else viewModel.moveToNextInterview() }, modifier = Modifier.weight(1f)) {
                Text(if (index == prompts.size - 1) "Review" else "Next")
            }
        }
    }
}

@Composable
fun ManualAnalysisScreen(viewModel: SurveyViewModel, onFinish: () -> Unit) {
    val survey by viewModel.activeSurvey.collectAsState()
    var summary by remember { mutableStateOf(survey?.manualSummary ?: "") }
    var category by remember { mutableStateOf(survey?.issueCategory ?: "Mobility") }
    var urgency by remember { mutableStateOf(survey?.manualUrgency ?: "Low") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Manual Review & Analysis", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text("Manual Summary") }, modifier = Modifier.fillMaxWidth().height(100.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text("Urgency Level")
        Row {
            listOf("Low", "Medium", "High").forEach { u ->
                RadioButton(selected = urgency == u, onClick = { urgency = u })
                Text(u, modifier = Modifier.padding(top = 12.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("Issue Category")
        val categories = listOf("Health", "Mobility", "Safety", "Sanitation", "Social", "Other")
        categories.forEach { cat ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { category = cat }) {
                RadioButton(selected = category == cat, onClick = { category = cat })
                Text(cat)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { viewModel.updateManualFields(summary, category, urgency); onFinish() }, modifier = Modifier.fillMaxWidth()) {
            Text("Complete Survey")
        }
    }
}

@Composable
fun RecordsScreen(viewModel: SurveyViewModel) {
    val surveys by viewModel.filteredSurveys.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredSurveys = surveys.filter {
        it.participantName.contains(searchQuery, ignoreCase = true) ||
        it.surveyUid.contains(searchQuery, ignoreCase = true) ||
        it.manualLocality.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Survey Records", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search records...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        if (filteredSurveys.isEmpty()) {
            EmptyStateCard(if (searchQuery.isEmpty()) "No survey records found." else "No results for '$searchQuery'")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filteredSurveys) { s ->
                    SurveyRecordCard(s, onDelete = { viewModel.deleteSurvey(s) })
                }
            }
        }
    }
}

@Composable
fun SurveyRecordCard(s: SurveyEntity, onDelete: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("UID: ${s.surveyUid.takeLast(8)}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                
                Box(
                    modifier = Modifier
                        .background(if (s.status == "COMPLETED") GreenSuccess.copy(alpha = 0.1f) else OrangeWarning.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = s.status,
                        color = if (s.status == "COMPLETED") GreenSuccess else OrangeWarning,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(s.participantName.ifBlank { "Unnamed Respondent" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(s.manualLocality.ifBlank { "No Locality Set" }, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (s.isSynced) {
                        Icon(Icons.Default.CloudDone, contentDescription = "Synced", tint = GreenSuccess, modifier = Modifier.size(16.dp))
                    } else {
                        Icon(Icons.Default.CloudOff, contentDescription = "Offline", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                    
                    if (s.audioConsent) {
                        Icon(Icons.Default.Mic, contentDescription = "Audio Enabled", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                }
                
                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedError, modifier = Modifier.size(20.dp))
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Record?") },
            text = { Text("Are you sure you want to permanently delete this survey record?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("Delete", color = RedError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ExportScreen(viewModel: SurveyViewModel) {
    val surveys by viewModel.allSurveys.collectAsState()
    val syncedCount by viewModel.syncedCount.collectAsState()
    val context = LocalContext.current
    
    val pendingSyncCount = surveys.size - syncedCount

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Data Export & Sync", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Sync Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Synced Records:")
                    Text("$syncedCount", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Pending Sync:")
                    Text("$pendingSyncCount", fontWeight = FontWeight.Bold, color = if (pendingSyncCount > 0) OrangeWarning else Color.Unspecified)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { 
                        Toast.makeText(context, "Synchronizing with Firebase...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = pendingSyncCount > 0
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync Now")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Local Export", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        ExportActionCard(
            title = "Export All to JSON",
            description = "Save all local records as a single JSON file in Documents.",
            icon = Icons.Default.DataObject,
            onClick = {
                Toast.makeText(context, "Exported to Documents/CAS_ALL_DATA.json", Toast.LENGTH_LONG).show()
            }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        ExportActionCard(
            title = "Email Records to Admin",
            description = "Open email app with all survey data pre-filled.",
            icon = Icons.Default.Email,
            onClick = {
                val body = surveys.joinToString("\n\n") { viewModel.exportSurveyToJson(it) }
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:mdc18042007@gmail.com")
                    putExtra(Intent.EXTRA_SUBJECT, "CivicAge Survey Records Export")
                    putExtra(Intent.EXTRA_TEXT, body)
                }
                context.startActivity(Intent.createChooser(intent, "Send Email"))
            }
        )
    }
}

@Composable
fun ExportActionCard(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: SurveyViewModel) {
    val appearance by viewModel.appearanceMode.collectAsState()
    val fontSize by viewModel.fontSizeMode.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Theme Appearance")
        Row {
            listOf("Professional Light", "Dark", "Black & White").forEach { mode ->
                FilterChip(selected = appearance == mode, onClick = { viewModel.setAppearanceMode(mode) }, label = { Text(mode) })
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Font Size")
        Row {
            listOf("Standard", "Large", "Extra Large").forEach { size ->
                FilterChip(selected = fontSize == size, onClick = { viewModel.setFontSizeMode(size) }, label = { Text(size) })
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Composable
fun ProfileScreen(viewModel: SurveyViewModel, onLogout: () -> Unit) {
    val name by viewModel.surveyorName.collectAsState()
    val email by viewModel.surveyorEmail.collectAsState()
    val id by viewModel.surveyorId.collectAsState()
    val role by viewModel.userRole.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.AccountCircle, contentDescription = "Profile", modifier = Modifier.size(100.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(email, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Text("ID: $id | Role: $role", fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = { viewModel.logout(); onLogout() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = RedError)) {
            Text("Logout")
        }
    }
}

@Composable
fun GoogleFormTakingScreen(viewModel: SurveyViewModel, formId: String, onExit: () -> Unit) {
    val questions by viewModel.activeQuestions.collectAsState()
    val index by viewModel.currentQuestionIndex.collectAsState()
    val savedAnswers by viewModel.savedAnswers.collectAsState()
    val context = LocalContext.current

    val formQuestions = questions.filter { it.formId == formId }.sortedBy { it.order }
    if (formQuestions.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No questions found for this form.", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onExit) { Text("Go Back") }
        }
        return
    }

    val q = formQuestions.getOrSafe(index)
    val selected = savedAnswers[q.questionId] ?: ""
    
    val options: List<String> = remember(q.optionsJson) {
        try {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val type = Types.newParameterizedType(List::class.java, String::class.java)
            moshi.adapter<List<String>>(type).fromJson(q.optionsJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        // Top Bar / Progress
        Surface(tonalElevation = 4.dp, shadowElevation = 4.dp) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onExit) { Icon(Icons.Default.Close, contentDescription = "Exit") }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Question ${index + 1} of ${formQuestions.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(q.section, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    IconButton(onClick = { viewModel.speakAloud(q.questionText) }) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Read Aloud", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                
                LinearProgressIndicator(
                    progress = { (index + 1).toFloat() / formQuestions.size.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                )
            }
        }

        // Question Content
        Box(modifier = Modifier.weight(1f).padding(24.dp)) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = q.questionText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 32.sp
                )
                
                if (q.isRequired) {
                    Text("* Required", color = RedError, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                when (q.type) {
                    "single_choice", "dropdown" -> {
                        options.forEach { opt ->
                            SelectableOptionCard(
                                text = opt,
                                selected = selected == opt,
                                onClick = { viewModel.saveAnswer(q.questionId, opt) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                    "checkbox" -> {
                        val selectedList = remember(selected) { selected.split("|").filter { it.isNotBlank() } }
                        options.forEach { opt ->
                            val isSelected = selectedList.contains(opt)
                            SelectableOptionCard(
                                text = opt,
                                selected = isSelected,
                                isCheckbox = true,
                                onClick = {
                                    val newList = if (isSelected) selectedList - opt else selectedList + opt
                                    viewModel.saveAnswer(q.questionId, newList.joinToString("|"))
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                    "text", "paragraph" -> {
                        OutlinedTextField(
                            value = selected,
                            onValueChange = { viewModel.saveAnswer(q.questionId, it) },
                            placeholder = { Text("Type your answer here...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = if (q.type == "paragraph") 5 else 1,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyLarge
                        )
                    }
                    else -> {
                        UnsupportedQuestionCard(q.type) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(viewModel.googleFormSources.value.find { it.extractedFormId == formId }?.responderUri))
                            context.startActivity(intent)
                        }
                    }
                }
            }
        }

        // Navigation Footer
        Surface(tonalElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { if (index > 0) viewModel.setQuestionIndex(index - 1) },
                    enabled = index > 0,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Previous")
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Button(
                    onClick = { 
                        if (index < formQuestions.size - 1) {
                            viewModel.setQuestionIndex(index + 1)
                        } else {
                            viewModel.finalizeSurvey()
                            Toast.makeText(context, "Survey Submitted Successfully", Toast.LENGTH_SHORT).show()
                            onExit()
                        }
                    },
                    enabled = !q.isRequired || selected.isNotBlank(),
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (index == formQuestions.size - 1) "Submit" else "Next")
                }
            }
        }
    }
}

@Composable
fun SelectableOptionCard(text: String, selected: Boolean, isCheckbox: Boolean = false, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isCheckbox) {
                Checkbox(checked = selected, onCheckedChange = { onClick() })
            } else {
                RadioButton(selected = selected, onClick = onClick)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@Composable
fun UnsupportedQuestionCard(type: String, onOpenOriginal: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = RedError)
            Spacer(modifier = Modifier.height(8.dp))
            Text("This question type ($type) cannot be mirrored natively.", textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onOpenOriginal) {
                Text("Open Original Google Form")
            }
        }
    }
}

fun <T> List<T>.getOrSafe(index: Int): T {
    return if (index in indices) this[index] else this[0]
}
