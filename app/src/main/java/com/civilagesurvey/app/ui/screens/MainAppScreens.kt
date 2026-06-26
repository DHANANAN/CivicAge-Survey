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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.civilagesurvey.app.config.Config
import com.civilagesurvey.app.data.database.*
import com.civilagesurvey.app.data.model.*
import com.civilagesurvey.app.ui.theme.GreenSuccess
import com.civilagesurvey.app.ui.theme.OrangeWarning
import com.civilagesurvey.app.ui.theme.RedError
import com.civilagesurvey.app.ui.viewmodel.SurveyViewModel
import java.text.SimpleDateFormat
import java.util.*

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
            if (currentScreen != "login" && currentScreen != "runner" && currentScreen != "consent") {
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
                        Triple("Instruments", Icons.AutoMirrored.Filled.Assignment, "instruments"),
                        Triple("Records", Icons.Default.History, "records"),
                        Triple("QA", Icons.Default.FactCheck, "qa"),
                        Triple("Export", Icons.Default.CloudUpload, "export"),
                        Triple("Settings", Icons.Default.Settings, "settings")
                    )
                    items.forEach { (label, icon, screen) ->
                        NavigationBarItem(
                            selected = currentScreen == screen,
                            onClick = { currentScreen = screen },
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
                    Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "You are offline. Data will sync automatically later.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "ScreenTransition"
                    ) { screen ->
                        when {
                            screen == "login" -> LoginScreen(viewModel) { currentScreen = "home" }
                            screen == "home" -> HomeScreen(viewModel, onNavigate = { currentScreen = it })
                            screen == "instruments" -> InstrumentSelectorScreen(viewModel) { currentScreen = "consent" }
                            screen == "consent" -> ConsentFlowScreen(viewModel) { currentScreen = "runner" }
                            screen == "runner" -> InstrumentRunnerScreen(viewModel) { currentScreen = "home" }
                            screen == "qa" -> QAScreen(viewModel)
                            screen == "records" -> RecordsScreen(viewModel)
                            screen == "export" -> ExportScreen(viewModel)
                            screen == "settings" -> SettingsScreen(viewModel)
                            screen == "profile" -> ProfileScreen(viewModel) { currentScreen = "login" }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(viewModel: SurveyViewModel, onLogin: () -> Unit) {
    var email by remember { mutableStateOf(viewModel.surveyorEmail.value) }
    var name by remember { mutableStateOf(viewModel.surveyorName.value) }
    var id by remember { mutableStateOf(viewModel.surveyorId.value) }
    var phone by remember { mutableStateOf(viewModel.surveyorPhone.value) }

    var subvName by remember { mutableStateOf(viewModel.survivorName.value) }
    var subvAge by remember { mutableStateOf(viewModel.survivorAge.value) }
    var subvEmail by remember { mutableStateOf(viewModel.survivorEmail.value) }
    var subvPhone by remember { mutableStateOf(viewModel.survivorPhone.value) }

    var cgName by remember { mutableStateOf(viewModel.caregiverName.value) }
    var cgAge by remember { mutableStateOf(viewModel.caregiverAge.value) }
    var cgEmail by remember { mutableStateOf(viewModel.caregiverEmail.value) }
    var cgPhone by remember { mutableStateOf(viewModel.caregiverPhone.value) }
    var cgRelation by remember { mutableStateOf(viewModel.caregiverRelation.value) }

    var activeTab by remember { mutableStateOf(0) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.LocationCity, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(12.dp))
        Text("DCA", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
        Text("Data Collection Application", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))

        TabRow(selectedTabIndex = activeTab, modifier = Modifier.fillMaxWidth()) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("1. Surveyor", fontSize = 12.sp) })
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("2. Survivor", fontSize = 12.sp) })
            Tab(selected = activeTab == 2, onClick = { activeTab = 2 }, text = { Text("3. Respondent", fontSize = 12.sp) })
        }

        Spacer(modifier = Modifier.height(24.dp))

        when (activeTab) {
            0 -> {
                Text("Surveyor Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Surveyor Full Name *") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Surveyor Email *") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = id, onValueChange = { id = it }, label = { Text("Surveyor ID *") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Surveyor Phone") }, modifier = Modifier.fillMaxWidth())
            }
            1 -> {
                Text("Survivor (Participant) Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = subvName, onValueChange = { subvName = it }, label = { Text("Survivor Full Name *") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = subvAge, onValueChange = { subvAge = it }, label = { Text("Survivor Age *") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = subvEmail, onValueChange = { subvEmail = it }, label = { Text("Survivor Contact Email") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = subvPhone, onValueChange = { subvPhone = it }, label = { Text("Survivor Mobile Number") }, modifier = Modifier.fillMaxWidth())
            }
            2 -> {
                Text("Caregiver / Respondent Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = cgName, onValueChange = { cgName = it }, label = { Text("Respondent Full Name *") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = cgAge, onValueChange = { cgAge = it }, label = { Text("Respondent Age *") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = cgEmail, onValueChange = { cgEmail = it }, label = { Text("Respondent Contact Email") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = cgPhone, onValueChange = { cgPhone = it }, label = { Text("Respondent Mobile Number") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = cgRelation, onValueChange = { cgRelation = it }, label = { Text("Relation to Survivor (e.g. Spouse/Child/Paid Caregiver) *") }, modifier = Modifier.fillMaxWidth())
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = {
            if (activeTab < 2) {
                activeTab++
            } else {
                if (name.isNotBlank() && email.contains("@") && id.isNotBlank() &&
                    subvName.isNotBlank() && subvAge.isNotBlank() &&
                    cgName.isNotBlank() && cgAge.isNotBlank() && cgRelation.isNotBlank()) {
                    
                    viewModel.saveSurvivorProfile(subvName, subvAge, subvEmail, subvPhone)
                    viewModel.saveCaregiverProfile(cgName, cgAge, cgEmail, cgPhone, cgRelation)
                    
                    viewModel.signInOrSignUp(email, "", name, id, phone, false) { _, _ -> onLogin() }
                } else {
                    Toast.makeText(context, "Please fill all required (*) fields", Toast.LENGTH_LONG).show()
                }
            }
        }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(if (activeTab < 2) "Next Profile" else "Access Dashboard", fontWeight = FontWeight.Bold)
        }
        if (activeTab > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = { activeTab-- }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("Previous Profile", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: SurveyViewModel, onNavigate: (String) -> Unit) {
    val allSurveys by viewModel.allSurveys.collectAsState()
    val completedCount by viewModel.completedCount.collectAsState()
    val draftCount by viewModel.draftCount.collectAsState()
    val syncedCount by viewModel.syncedCount.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("DCA Dashboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text("Smart Cities, Silent Citizens | ICSSR 2025-26", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        
        Spacer(modifier = Modifier.height(24.dp))
        DashboardStats(allSurveys.size, draftCount, completedCount, syncedCount)
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Quick Actions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionCard("Start Instrument", Icons.Default.Add, { onNavigate("instruments") }, Modifier.weight(1f))
            QuickActionCard("View Records", Icons.Default.History, { onNavigate("records") }, Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionCard("Sync Data", Icons.Default.Sync, { onNavigate("export") }, Modifier.weight(1f))
            QuickActionCard("Profile", Icons.Default.AccountCircle, { onNavigate("profile") }, Modifier.weight(1f))
        }
    }
}

@Composable
fun DashboardStats(total: Int, drafts: Int, completed: Int, synced: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardCard(Modifier.weight(1f), "Total Records", total.toString(), Icons.Default.Analytics, MaterialTheme.colorScheme.primary)
            DashboardCard(Modifier.weight(1f), "Drafts", drafts.toString(), Icons.Default.EditNote, OrangeWarning)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardCard(Modifier.weight(1f), "Completed", completed.toString(), Icons.Default.CheckCircle, GreenSuccess)
            DashboardCard(Modifier.weight(1f), "Synced", synced.toString(), Icons.Default.CloudDone, MaterialTheme.colorScheme.tertiary)
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
    Card(modifier = modifier.height(100.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun InstrumentSelectorScreen(viewModel: SurveyViewModel, onNavigate: () -> Unit) {
    val instruments = InstrumentBank.instruments
    var selectedCity by remember { mutableStateOf("ND") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Select City", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        CitySelector(selectedCity) { selectedCity = it; viewModel.selectedCity.value = it }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Research Instruments", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(instruments) { instrument ->
                InstrumentCard(instrument) {
                    viewModel.startDcaSession(instrument)
                    onNavigate()
                }
            }
        }
    }
}

@Composable
fun CitySelector(selected: String, onSelect: (String) -> Unit) {
    val cities = listOf("ND" to "New Delhi", "SG" to "Srinagar", "AZ" to "Aizawl", "CH" to "Chennai", "GN" to "Gandhinagar", "IN" to "Indore")
    Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
        cities.forEach { (code, name) ->
            FilterChip(selected = selected == code, onClick = { onSelect(code) }, label = { Text(name) }, modifier = Modifier.padding(end = 8.dp))
        }
    }
}

@Composable
fun InstrumentCard(instrument: Instrument, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(instrument.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(instrument.stakeholder, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Box(modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(instrument.code, fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
fun QAScreen(viewModel: SurveyViewModel) {
    val allSurveys by viewModel.allSurveys.collectAsState()
    val pendingQA = allSurveys.filter { it.status == "COMPLETED" && !it.isSynced }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Quality Assurance Queue", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        if (pendingQA.isEmpty()) {
            EmptyStateCard("No records pending QA review.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(pendingQA) { s -> QAItemCard(s) }
            }
        }
    }
}

@Composable
fun QAItemCard(s: SurveyEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("ID: ${s.surveyUid}", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Button(onClick = { /* Approve */ }, modifier = Modifier.weight(1f)) { Text("Approve") }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = { /* Flag */ }, modifier = Modifier.weight(1f)) { Text("Flag Concern") }
            }
        }
    }
}

@Composable
fun RecordsScreen(viewModel: SurveyViewModel) {
    val surveys by viewModel.filteredSurveys.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val filteredSurveys = surveys.filter { it.participantName.contains(searchQuery, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Survey Records", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search records...") }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) })
        Spacer(modifier = Modifier.height(16.dp))

        if (filteredSurveys.isEmpty()) {
            EmptyStateCard("No records found.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filteredSurveys) { s -> SurveyRecordCard(s, onDelete = { viewModel.deleteSurvey(s) }) }
            }
        }
    }
}

@Composable
fun SurveyRecordCard(s: SurveyEntity, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(s.participantName.ifBlank { "Unnamed Respondent" }, fontWeight = FontWeight.Bold)
            Text(s.surveyUid, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(s.status, color = if (s.status == "COMPLETED") GreenSuccess else OrangeWarning, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = null, tint = RedError) }
            }
        }
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
        
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Sync Status", fontWeight = FontWeight.Bold)
                Text("Synced: $syncedCount | Pending: $pendingSyncCount")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { Toast.makeText(context, "Syncing...", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth()) { Text("Sync Now") }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { /* Export JSON */ }, modifier = Modifier.fillMaxWidth()) { Text("Export All to JSON") }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = { /* Email Export */ }, modifier = Modifier.fillMaxWidth()) { Text("Email Records to Admin") }
    }
}

@Composable
fun SettingsScreen(viewModel: SurveyViewModel) {
    val appearance by viewModel.appearanceMode.collectAsState()
    val fontSize by viewModel.fontSizeMode.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Theme Appearance")
        Row {
            listOf("Light", "Dark", "B&W").forEach { mode ->
                FilterChip(selected = appearance.contains(mode), onClick = { viewModel.setAppearanceMode(mode) }, label = { Text(mode) })
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(100.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(email, color = Color.Gray)
        Text("ID: $id")
        Spacer(modifier = Modifier.height(40.dp))
        Button(onClick = { viewModel.logout(); onLogout() }, colors = ButtonDefaults.buttonColors(containerColor = RedError)) { Text("Logout") }
    }
}

@Composable
fun EmptyStateCard(message: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), border = BorderStroke(1.dp, Color.LightGray)) {
        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, textAlign = TextAlign.Center, color = Color.Gray)
        }
    }
}
