package com.civilagesurvey.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSurveyApp(viewModel: SurveyViewModel) {
    var currentScreen by remember { mutableStateOf("login") }
    val surveyorEmail by viewModel.surveyorEmail.collectAsState()
    val context = LocalContext.current

    if (currentScreen == "login" && surveyorEmail.isNotBlank()) {
        currentScreen = "home"
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val userRole by viewModel.userRole.collectAsState()

    val drawerItems = remember(userRole) {
        val list = mutableListOf(
            Triple("Home", Icons.Default.Dashboard, "home"),
            Triple("Instruments", Icons.AutoMirrored.Filled.Assignment, "instruments"),
            Triple("Records", Icons.Default.History, "records"),
            Triple("QA", Icons.Default.FactCheck, "qa")
        )
        if (userRole == Config.ADMIN_ROLE) {
            list.add(Triple("Export", Icons.Default.CloudUpload, "export"))
        }
        list.add(Triple("Settings", Icons.Default.Settings, "settings"))
        list.add(Triple("Profile", Icons.Default.AccountCircle, "profile"))
        list
    }

    var showExitDialog by remember { mutableStateOf(false) }
    var showDraftDialog by remember { mutableStateOf(false) }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit App") },
            text = { Text("Do you want to exit the CivicAge Survey application?") },
            confirmButton = {
                Button(onClick = {
                    showExitDialog = false
                    (context as? android.app.Activity)?.finish()
                }) { Text("Exit") }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDraftDialog) {
        AlertDialog(
            onDismissRequest = { showDraftDialog = false },
            title = { Text("Exit Survey Session") },
            text = { Text("Would you like to save this survey as a draft before leaving?") },
            confirmButton = {
                Button(onClick = {
                    showDraftDialog = false
                    viewModel.stopSpeaking()
                    currentScreen = "home"
                }) { Text("Save Draft") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        showDraftDialog = false
                        viewModel.stopSpeaking()
                        viewModel.activeSurvey.value?.let { viewModel.deleteSurvey(it) }
                        currentScreen = "home"
                    }) { Text("Discard", color = RedError) }
                    TextButton(onClick = { showDraftDialog = false }) { Text("Stay") }
                }
            }
        )
    }

    if (currentScreen != "login") {
        BackHandler {
            when (currentScreen) {
                "home" -> {
                    showExitDialog = true
                }
                "runner" -> {
                    val showThemes = viewModel.showThemesPhase.value
                    val sectIndex = viewModel.currentSectionIndex.value
                    val questIndex = viewModel.currentQuestionIndex.value
                    val themeIndex = viewModel.currentInterviewIndex.value
                    val inst = viewModel.currentInstrument.value
                    
                    val isAtStart = if (showThemes) {
                        themeIndex == 0 && (inst?.sections?.isEmpty() ?: true)
                    } else {
                        sectIndex == 0 && questIndex == 0
                    }
                    
                    if (isAtStart) {
                        showDraftDialog = true
                    } else {
                        viewModel.moveToPreviousStep()
                    }
                }
                "consent" -> {
                    showDraftDialog = true
                }
                "instruments" -> {
                    currentScreen = "home"
                }
                else -> {
                    currentScreen = "home"
                }
            }
        }
    }

    if (currentScreen == "login") {
        LoginScreen(viewModel) { currentScreen = "home" }
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "CivicAge Survey",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Smart Cities, Silent Citizens",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    drawerItems.forEach { (label, icon, screen) ->
                        NavigationDrawerItem(
                            icon = { Icon(icon, contentDescription = null) },
                            label = { Text(label, fontWeight = FontWeight.SemiBold) },
                            selected = currentScreen == screen,
                            onClick = {
                                currentScreen = screen
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
                        label = { Text("Logout", fontWeight = FontWeight.Bold, color = RedError) },
                        selected = false,
                        onClick = {
                            viewModel.logout()
                            currentScreen = "login"
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        ) {
            Scaffold(
                topBar = {
                    if (currentScreen != "runner" && currentScreen != "consent") {
                        val title = when (currentScreen) {
                            "home" -> "DCA Dashboard"
                            "instruments" -> "Research Instruments"
                            "records" -> "Survey Records"
                            "qa" -> "Quality Assurance"
                            "export" -> "Data Export & Sync"
                            "settings" -> "Settings"
                            "profile" -> "Surveyor Profile"
                            else -> "CivicAge DCA"
                        }
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    text = title,
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Open navigation drawer"
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                },
                bottomBar = {
                    if (currentScreen != "runner" && currentScreen != "consent") {
                        val draftCount by viewModel.draftCount.collectAsState()
                        val syncedCount by viewModel.syncedCount.collectAsState()
                        val totalCount by viewModel.allSurveys.collectAsState().let { 
                            derivedStateOf { it.value.size }
                        }
                        val pendingSyncCount by remember {
                            derivedStateOf { totalCount - syncedCount }
                        }
                        
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
                            val items = remember(userRole) {
                                val list = mutableListOf(
                                    Triple("Home", Icons.Default.Dashboard, "home"),
                                    Triple("Instruments", Icons.AutoMirrored.Filled.Assignment, "instruments"),
                                    Triple("Records", Icons.Default.History, "records"),
                                    Triple("QA", Icons.Default.FactCheck, "qa")
                                )
                                if (userRole == Config.ADMIN_ROLE) {
                                    list.add(Triple("Export", Icons.Default.CloudUpload, "export"))
                                }
                                list.add(Triple("Settings", Icons.Default.Settings, "settings"))
                                list
                            }
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
    }
}

@Composable
fun LoginScreen(viewModel: SurveyViewModel, onLogin: () -> Unit) {
    var email by remember { mutableStateOf(viewModel.surveyorEmail.value) }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf(viewModel.surveyorName.value) }
    var id by remember { mutableStateOf(viewModel.surveyorId.value) }
    var phone by remember { mutableStateOf(viewModel.surveyorPhone.value) }
    val context = LocalContext.current

    val brightText = Color(0xFF111827) // Near-black for high glare readability
    val inputColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        focusedLabelColor = Color(0xFF1E3A8A),
        unfocusedLabelColor = Color(0xFF374151),
        focusedBorderColor = Color(0xFF1E3A8A),
        unfocusedBorderColor = Color(0xFF4B5563),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.LocationCity, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFF1E3A8A))
        Spacer(modifier = Modifier.height(12.dp))
        Text("DCA", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = Color.Black)
        Text("Data Collection Application", style = MaterialTheme.typography.titleMedium, color = Color(0xFF1E3A8A))
        Spacer(modifier = Modifier.height(24.dp))

        Text("Surveyor Profile Login", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = brightText)
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(
            value = email, 
            onValueChange = { email = it }, 
            label = { Text("Email / Username *") }, 
            colors = inputColors,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = password, 
            onValueChange = { password = it }, 
            label = { Text("Password *") }, 
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = inputColors,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text("Optional Surveyor Profile Info", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF374151))
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Surveyor Full Name") }, colors = inputColors, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = id, onValueChange = { id = it }, label = { Text("Surveyor ID / FI Code") }, colors = inputColors, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Surveyor Phone") }, colors = inputColors, modifier = Modifier.fillMaxWidth())
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = {
            val cleanEmail = email.trim().lowercase()
            if (cleanEmail == "scscdata@gmail.com") {
                if (password == "SCSCD@2026") {
                    val finalName = name.ifBlank { "Master Administrator" }
                    val finalId = id.ifBlank { "SCSC-MASTER" }
                    viewModel.signInOrSignUp(cleanEmail, "", finalName, finalId, phone, false) { _, _ -> onLogin() }
                } else {
                    Toast.makeText(context, "Incorrect password for Master Role", Toast.LENGTH_SHORT).show()
                }
            } else if (cleanEmail == "scsc1") {
                if (password == "SCSC2026") {
                    val finalName = name.ifBlank { "Field Surveyor 1" }
                    val finalId = id.ifBlank { "SCSC-NORMAL" }
                    viewModel.signInOrSignUp("scsc1@civilagesurvey.app", "", finalName, finalId, phone, false) { _, _ -> onLogin() }
                } else {
                    Toast.makeText(context, "Incorrect password for Normal Role", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Access Denied: Invalid credentials", Toast.LENGTH_LONG).show()
            }
        }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Access Dashboard", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun HomeScreen(viewModel: SurveyViewModel, onNavigate: (String) -> Unit) {
    val allSurveys by viewModel.filteredSurveys.collectAsState()
    val completedCount by viewModel.completedCount.collectAsState()
    val draftCount by viewModel.draftCount.collectAsState()
    val syncedCount by viewModel.syncedCount.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("DCA Dashboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text("Smart Cities, Silent Citizens | ICSSR 2025-26", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        
        if (userRole == Config.ADMIN_ROLE) {
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

            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Master Control Panel", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("Local Data Sync Server (Wi-Fi Tunnel)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    val isServerRunning by viewModel.isSyncServerRunning.collectAsState()
                    val serverStatus by viewModel.syncServerStatus.collectAsState()
                    
                    Text("Status: $serverStatus", style = MaterialTheme.typography.bodyMedium, color = if (isServerRunning) GreenSuccess else Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (isServerRunning) viewModel.stopSyncServer() else viewModel.startSyncServer()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isServerRunning) "Stop Sync Server" else "Start Sync Server")
                        }
                    }
                    
                    val logs by viewModel.localSyncLogs.collectAsState()
                    if (logs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Connection Logs:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Box(modifier = Modifier.height(80.dp).fillMaxWidth().background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(4.dp)).padding(4.dp)) {
                            LazyColumn {
                                items(logs) { log ->
                                    Text(log, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Xbox Control Services", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    var mockXboxTracker by remember { mutableStateOf(true) }
                    var tunnelOverride by remember { mutableStateOf(false) }
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = mockXboxTracker, onCheckedChange = { mockXboxTracker = it })
                        Text("Mock Xbox Services Tracker", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = tunnelOverride, onCheckedChange = { tunnelOverride = it })
                        Text("Network Tunnel Force Override", fontSize = 13.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { Toast.makeText(context, "System Logs flushed successfully", Toast.LENGTH_SHORT).show() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("System Logs")
                        }
                        OutlinedButton(
                            onClick = { Toast.makeText(context, "User Management is synced with Cloud Portal", Toast.LENGTH_SHORT).show() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("User Management")
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("instruments") },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.AddCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Start a Research Instrument",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Click here to select a survey instrument and begin a participant interview session.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                }
            }
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
    val context = LocalContext.current
    var selectedCity by remember { mutableStateOf(viewModel.selectedCity.value) }
    val userRole by viewModel.userRole.collectAsState()

    var showIntakeDialog by remember { mutableStateOf(false) }
    var targetInstrument by remember { mutableStateOf<Instrument?>(null) }

    var respondentName by remember { mutableStateOf("") }
    var respondentAge by remember { mutableStateOf("") }
    var respondentMobile by remember { mutableStateOf("") }
    var respondentCity by remember { mutableStateOf(selectedCity) }

    if (showIntakeDialog && targetInstrument != null) {
        val intakeLatitude by viewModel.intakeLatitude.collectAsState()
        val intakeLongitude by viewModel.intakeLongitude.collectAsState()
        val intakeAccuracy by viewModel.intakeAccuracy.collectAsState()
        val intakeLocationStatus by viewModel.intakeLocationStatus.collectAsState()
        val isFetchingGps by viewModel.isFetchingGps.collectAsState()

        val hasFineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
            if (fineGranted || coarseGranted) {
                viewModel.fetchIntakeLocation()
            } else {
                Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        AlertDialog(
            onDismissRequest = { showIntakeDialog = false },
            title = { Text("Respondent Intake Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Instrument: ${targetInstrument!!.title} (${targetInstrument!!.code})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    val isFirstTwo = targetInstrument?.let { it.code == "SQ-OOP" || it.code == "SSI-OOP" } ?: false

                    OutlinedTextField(
                        value = respondentName,
                        onValueChange = { respondentName = it },
                        label = { Text("Respondent Name *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (isFirstTwo) {
                        OutlinedTextField(
                            value = respondentAge,
                            onValueChange = { respondentAge = it },
                            label = { Text("Respondent Age *") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = respondentMobile,
                            onValueChange = { respondentMobile = it },
                            label = { Text("Mobile Number (Optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // City defaults to "ND" silently
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("GPS Coordinates Verification", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    
                    if (intakeLatitude != 0.0 || intakeLongitude != 0.0) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Latitude: $intakeLatitude", style = MaterialTheme.typography.bodyMedium)
                                Text("Longitude: $intakeLongitude", style = MaterialTheme.typography.bodyMedium)
                                Text("Accuracy: ${String.format("%.1f", intakeAccuracy)} meters", style = MaterialTheme.typography.bodyMedium)
                                Text("Status: $intakeLocationStatus", style = MaterialTheme.typography.bodyMedium, color = GreenSuccess)
                            }
                        }
                    } else {
                        Text(
                            text = "Status: $intakeLocationStatus",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (intakeLocationStatus.contains("Fetching")) MaterialTheme.colorScheme.primary else RedError
                        )
                    }
 
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
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        if (isFetchingGps) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onSecondary)
                        } else {
                            Text("Locate Location")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val isFirstTwo = targetInstrument?.let { it.code == "SQ-OOP" || it.code == "SSI-OOP" } ?: false
                        
                        if (respondentName.isBlank()) {
                            Toast.makeText(context, "Please enter respondent name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        if (isFirstTwo) {
                            if (respondentAge.isBlank()) {
                                Toast.makeText(context, "Please enter respondent age", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val ageInt = respondentAge.toIntOrNull()
                            if (targetInstrument!!.code.contains("OOP") && (ageInt == null || ageInt < 75)) {
                                Toast.makeText(context, "Warning: Participant age is below 75 years (Target Population)", Toast.LENGTH_LONG).show()
                            }
                            if (respondentMobile.isNotBlank() && !respondentMobile.matches(Regex("^[0-9+() -]+$"))) {
                                Toast.makeText(context, "Please enter a valid mobile number", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                        }
                        
                        val finalAge = if (isFirstTwo) respondentAge else "0"
                        val finalMobile = if (isFirstTwo) respondentMobile else ""
                        
                        viewModel.selectedCity.value = respondentCity
                        viewModel.startDcaSession(
                            instrument = targetInstrument!!,
                            rName = respondentName,
                            rAge = finalAge,
                            rMobile = finalMobile,
                            rCity = respondentCity
                        )
                        showIntakeDialog = false
                        onNavigate()
                    }
                ) { Text("Start Survey") }
            },
            dismissButton = {
                TextButton(onClick = { showIntakeDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Research Instruments", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(instruments) { instrument ->
                InstrumentCard(instrument, enabled = (userRole != Config.ADMIN_ROLE)) {
                    targetInstrument = instrument
                    respondentName = ""
                    respondentAge = ""
                    respondentMobile = ""
                    viewModel.intakeLatitude.value = 0.0
                    viewModel.intakeLongitude.value = 0.0
                    viewModel.intakeAccuracy.value = 0f
                    viewModel.intakeLocationStatus.value = "Not Captured"
                    showIntakeDialog = true
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
fun InstrumentCard(instrument: Instrument, enabled: Boolean = true, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.95f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "InstrumentCardScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = if (enabled) LocalIndication.current else null,
                onClick = onClick
            )
    ) {
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
        OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search records...") }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) })
        Spacer(modifier = Modifier.height(16.dp))

        if (filteredSurveys.isEmpty()) {
            EmptyStateCard("No records found.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filteredSurveys) { s -> 
                    val context = LocalContext.current
                    SurveyRecordCard(
                        s = s,
                        onDelete = { viewModel.deleteSurvey(s) },
                        onEmail = {
                            val subject = "CivicAge Survey Response – ${s.participantName} – ${s.surveyUid.split("-").getOrNull(4) ?: ""}"
                            val body = viewModel.buildDetailedReportText(s)
                            shareSurveyReportViaEmail(context, body, subject)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SurveyRecordCard(s: SurveyEntity, onDelete: () -> Unit, onEmail: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(s.participantName.ifBlank { "Unnamed Respondent" }, fontWeight = FontWeight.Bold)
            Text(s.surveyUid, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(s.status, color = if (s.status == "COMPLETED") GreenSuccess else OrangeWarning, fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = onEmail) { Icon(Icons.Default.Email, contentDescription = "Email Report", tint = MaterialTheme.colorScheme.primary) }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = null, tint = RedError) }
                }
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
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Sync Status", fontWeight = FontWeight.Bold)
                Text("Synced: $syncedCount | Pending: $pendingSyncCount")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { Toast.makeText(context, "Syncing...", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth()) { Text("Sync Now") }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                if (surveys.isEmpty()) {
                    Toast.makeText(context, "No records to export.", Toast.LENGTH_SHORT).show()
                } else {
                    val jsonList = surveys.map { viewModel.exportSurveyToJson(it) }
                    val combinedJson = "[\n" + jsonList.joinToString(",\n") + "\n]"
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("CivicAge Survey JSON Export", combinedJson)
                    clipboard.setPrimaryClip(clip)
                    
                    try {
                        val file = java.io.File(context.cacheDir, "surveys_export.json")
                        file.writeText(combinedJson)
                        Toast.makeText(context, "Copied JSON to clipboard and saved to: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Copied to clipboard, but error saving to cache: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Export All to JSON (Copy & Save to Cache)")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                if (surveys.isEmpty()) {
                    Toast.makeText(context, "No records to export.", Toast.LENGTH_SHORT).show()
                } else {
                    val tsvData = viewModel.exportAllSurveysToTsv(surveys)
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("CivicAge Survey TSV Export", tsvData)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Copied combined MCQ TSV (Excel-ready) data to clipboard!", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Export All to Excel/TSV (Copy to Clipboard)")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                if (surveys.isEmpty()) {
                    Toast.makeText(context, "No survey records available to email.", Toast.LENGTH_SHORT).show()
                } else {
                    val subject = "CivicAge Combined Survey Records"
                    val reportBuilder = StringBuilder()
                    reportBuilder.appendLine("CIVICAGE SURVEY COMBINED REPORT")
                    reportBuilder.appendLine("Total Records: ${surveys.size}")
                    reportBuilder.appendLine("Generated At: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
                    reportBuilder.appendLine("=========================================\n")
                    surveys.forEachIndexed { idx, s ->
                        reportBuilder.appendLine("RECORD #${idx + 1}")
                        reportBuilder.appendLine(viewModel.buildDetailedReportText(s))
                        reportBuilder.appendLine("\n-----------------------------------------\n")
                    }
                    shareSurveyReportViaEmail(context, reportBuilder.toString(), subject)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
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
        Spacer(modifier = Modifier.height(24.dp))
        Text("Theme Appearance")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Light", "Dark", "B&W").forEach { mode ->
                FilterChip(
                    selected = appearance.contains(mode),
                    onClick = { viewModel.setAppearanceMode(mode) },
                    label = { Text(mode, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) },
                    modifier = Modifier.fillMaxWidth()
                )
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

fun shareSurveyReportViaEmail(context: android.content.Context, reportText: String, subject: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf("mdc18042007@gmail.com"))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, reportText)
    }
    
    val gmailIntent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:mdc18042007@gmail.com")
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, reportText)
        setPackage("com.google.android.gm")
    }

    try {
        context.startActivity(gmailIntent)
    } catch (e: Exception) {
        try {
            context.startActivity(Intent.createChooser(intent, "Send Email via..."))
        } catch (ex: Exception) {
            Toast.makeText(context, "No email client found.", Toast.LENGTH_SHORT).show()
        }
    }
}

fun shareSurveyReportViaMasterEmail(context: android.content.Context, reportText: String, subject: String, targetEmail: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(targetEmail))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, reportText)
    }
    
    val gmailIntent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:$targetEmail")
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, reportText)
        setPackage("com.google.android.gm")
    }

    try {
        context.startActivity(gmailIntent)
    } catch (e: Exception) {
        try {
            context.startActivity(Intent.createChooser(intent, "Send Email via..."))
        } catch (ex: Exception) {
            Toast.makeText(context, "No email client found.", Toast.LENGTH_SHORT).show()
        }
    }
}
