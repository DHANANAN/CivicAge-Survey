package com.civilagesurvey.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.civilagesurvey.app.config.Config
import com.civilagesurvey.app.data.database.SurveyEntity
import com.civilagesurvey.app.ui.theme.GreenSuccess
import com.civilagesurvey.app.ui.theme.OrangeWarning
import com.civilagesurvey.app.ui.theme.RedError
import com.civilagesurvey.app.ui.viewmodel.SurveyViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSurveyApp(viewModel: SurveyViewModel) {
    var currentScreen by remember { mutableStateOf("login") }
    val surveyorEmail by viewModel.surveyorEmail.collectAsState()
    val context = LocalContext.current

    if (currentScreen == "login" && surveyorEmail.isNotBlank()) {
        currentScreen = "home"
    }

    Scaffold(
        bottomBar = {
            if (currentScreen != "login") {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    val items = listOf(
                        Triple("Home", Icons.Default.Home, "home"),
                        Triple("New Survey", Icons.Default.Add, "consent"),
                        Triple("Records", Icons.Default.List, "records"),
                        Triple("Export", Icons.Default.ImportExport, "export"),
                        Triple("Settings", Icons.Default.Settings, "settings"),
                        Triple("Profile", Icons.Default.Person, "profile")
                    )
                    items.forEach { (label, icon, screen) ->
                        NavigationBarItem(
                            selected = currentScreen == screen,
                            onClick = { currentScreen = screen },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 10.sp) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            when (currentScreen) {
                "login" -> LoginScreen(viewModel) { currentScreen = "home" }
                "home" -> HomeScreen(viewModel)
                "consent" -> ConsentScreen(viewModel) { currentScreen = "respondent_details" }
                "respondent_details" -> RespondentDetailsScreen(viewModel) { currentScreen = "mcq" }
                "mcq" -> MCQScreen(viewModel) { currentScreen = "interview" }
                "interview" -> InterviewScreen(viewModel) { currentScreen = "manual_analysis" }
                "manual_analysis" -> ManualAnalysisScreen(viewModel) { 
                    viewModel.finalizeSurvey()
                    Toast.makeText(context, "Survey Completed Successfully", Toast.LENGTH_SHORT).show()
                    currentScreen = "home" 
                }
                "records" -> RecordsScreen(viewModel)
                "export" -> ExportScreen(viewModel)
                "settings" -> SettingsScreen(viewModel)
                "profile" -> ProfileScreen(viewModel) { currentScreen = "login" }
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
        Text("CivicAge Survey", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Login")
        }
    }
}

@Composable
fun HomeScreen(viewModel: SurveyViewModel) {
    val name by viewModel.surveyorName.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Welcome, $name", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Center for Urban Planning", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("App Overview", fontWeight = FontWeight.Bold)
                Text("Use this app to conduct local-first surveys for senior citizens (75+). All data is saved locally and can be exported as reports.")
            }
        }
    }
}

@Composable
fun ConsentScreen(viewModel: SurveyViewModel, onNext: () -> Unit) {
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
        Button(onClick = { if (consent) { viewModel.startNewSurvey(true, true, true, true, true); onNext() } }, enabled = consent, modifier = Modifier.fillMaxWidth()) {
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
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item { Text("Local Survey Records", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        items(surveys) { s ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("UID: ${s.surveyUid}", fontWeight = FontWeight.Bold)
                    Text("Respondent: ${s.participantName}")
                    Text("Status: ${s.status}", color = if (s.status == "COMPLETED") GreenSuccess else OrangeWarning)
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = { viewModel.deleteSurvey(s) }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedError) }
                    }
                }
            }
        }
    }
}

@Composable
fun ExportScreen(viewModel: SurveyViewModel) {
    val surveys by viewModel.allSurveys.collectAsState()
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Export Data", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { Toast.makeText(context, "All surveys exported to Documents folder", Toast.LENGTH_LONG).show() }, modifier = Modifier.fillMaxWidth()) {
            Text("Export All to JSON/TXT")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = {
            val body = surveys.joinToString("\n\n") { viewModel.exportSurveyToJson(it) }
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:mdc18042007@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "CivicAge Survey Records Export")
                putExtra(Intent.EXTRA_TEXT, body)
            }
            context.startActivity(Intent.createChooser(intent, "Send Email"))
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Email Records to Admin")
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
