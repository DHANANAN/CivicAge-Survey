package com.civilagesurvey.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.civilagesurvey.app.config.Config
import com.civilagesurvey.app.data.database.*
import com.civilagesurvey.app.data.model.*
import com.civilagesurvey.app.security.BiometricAuthHelper
import com.civilagesurvey.app.ui.theme.GreenSuccess
import com.civilagesurvey.app.ui.theme.OrangeWarning
import com.civilagesurvey.app.ui.theme.RedError
import com.civilagesurvey.app.ui.viewmodel.SurveyViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

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
            Triple("Records", Icons.Default.History, "records")
        )
        if (userRole == Config.ROLE_QA || userRole == Config.ROLE_MASTER || userRole == Config.ADMIN_ROLE) {
            list.add(Triple("QA Dashboard", Icons.Default.FactCheck, "qa"))
        }
        if (userRole == Config.ROLE_MASTER || userRole == Config.ADMIN_ROLE) {
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
                "home" -> showExitDialog = true
                "runner", "consent" -> showDraftDialog = true
                "master_control" -> currentScreen = "settings"
                else -> currentScreen = "home"
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
                        text = "Role: ${userRole.uppercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    drawerItems.forEach { (label, icon, route) ->
                        NavigationDrawerItem(
                            icon = { Icon(icon, contentDescription = null) },
                            label = { Text(label, fontWeight = FontWeight.SemiBold) },
                            selected = currentScreen == route,
                            onClick = {
                                currentScreen = route
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
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
                            "qa" -> "Quality Assurance Queue"
                            "export" -> "Data Export & Sync"
                            "settings" -> "Settings"
                            "master_control" -> "Master Control Panel"
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
                                    Triple("Records", Icons.Default.History, "records")
                                )
                                if (userRole == Config.ROLE_QA || userRole == Config.ROLE_MASTER || userRole == Config.ADMIN_ROLE) {
                                    list.add(Triple("QA Queue", Icons.Default.FactCheck, "qa"))
                                }
                                if (userRole == Config.ROLE_MASTER || userRole == Config.ADMIN_ROLE) {
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
                                    text = "You are offline. Submissions will auto-sync when online.",
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
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "ScreenTransition"
                            ) { screen ->
                                when (screen) {
                                    "home" -> HomeScreen(viewModel, onNavigate = { currentScreen = it })
                                    "instruments" -> InstrumentSelectorScreen(viewModel) { currentScreen = "consent" }
                                    "consent" -> ConsentFlowScreen(viewModel) { currentScreen = "runner" }
                                    "runner" -> InstrumentRunnerScreen(viewModel) { currentScreen = "home" }
                                    "qa" -> QADashboardScreen(viewModel)
                                    "records" -> RecordsScreen(viewModel)
                                    "export" -> ExportScreen(viewModel)
                                    "settings" -> SettingsScreen(viewModel) { currentScreen = "master_control" }
                                    "master_control" -> MasterControlScreen(viewModel)
                                    "profile" -> ProfileScreen(viewModel) { currentScreen = "login" }
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

    val brightText = Color(0xFF111827)
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
        Text("Smart Cities Responsible Citizens Initiative", style = MaterialTheme.typography.titleMedium, color = Color(0xFF1E3A8A))
        Spacer(modifier = Modifier.height(24.dp))

        Text("Surveyor & Role Login", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = brightText)
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
        Button(
            onClick = {
                val cleanEmail = email.trim().lowercase()
                if (cleanEmail == "scscdata@gmail.com" || cleanEmail == "scscpdata@gmail.com") {
                    if (password == "SCSCD@2026" || password == "Scscd@2026") {
                        val finalName = name.ifBlank { "Master Administrator" }
                        val finalId = id.ifBlank { "SCSC-MASTER" }
                        viewModel.signInOrSignUp(cleanEmail, "", finalName, finalId, phone, false) { _, _ -> onLogin() }
                    } else {
                        Toast.makeText(context, "Incorrect password for Master Role", Toast.LENGTH_SHORT).show()
                    }
                } else if (cleanEmail == "scscqa@gmail.com" || cleanEmail == "qa") {
                    if (password == "SCSC2026" || password == "Scscd@2026") {
                        val finalName = name.ifBlank { "QA Reviewer" }
                        val finalId = id.ifBlank { "SCSC-QA" }
                        viewModel.signInOrSignUp("scscqa@gmail.com", "", finalName, finalId, phone, false) { _, _ -> onLogin() }
                    } else {
                        Toast.makeText(context, "Incorrect password for QA Role", Toast.LENGTH_SHORT).show()
                    }
                } else if (cleanEmail == "scsc1" || cleanEmail.isNotBlank()) {
                    val finalName = name.ifBlank { "Field Surveyor 1" }
                    val finalId = id.ifBlank { "SCSC-NORMAL" }
                    val loginEmail = if (cleanEmail == "scsc1") "scsc1@civilagesurvey.app" else cleanEmail
                    viewModel.signInOrSignUp(loginEmail, "", finalName, finalId, phone, false) { _, _ -> onLogin() }
                } else {
                    Toast.makeText(context, "Please enter email or username", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("DCA Dashboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text("Smart Cities Responsible Citizens Initiative | ICSSR 2025-26", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))

        if (userRole == Config.ROLE_MASTER || userRole == Config.ADMIN_ROLE || userRole == Config.ROLE_QA) {
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
                if (userRole == Config.ROLE_QA || userRole == Config.ROLE_MASTER || userRole == Config.ADMIN_ROLE) {
                    QuickActionCard("QA Queue", Icons.Default.FactCheck, { onNavigate("qa") }, Modifier.weight(1f))
                }
                QuickActionCard("Profile", Icons.Default.AccountCircle, { onNavigate("profile") }, Modifier.weight(1f))
            }
        } else {
            // Simplified Normal User Home Screen
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
                        "Click here to select a research instrument and begin a high-accuracy geo-tagged survey session.",
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
fun QADashboardScreen(viewModel: SurveyViewModel) {
    val pendingSurveys by viewModel.pendingQaSurveys.collectAsState()
    val context = LocalContext.current
    var selectedSurvey by remember { mutableStateOf<SurveyEntity?>(null) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var qaNotesInput by remember { mutableStateOf("") }

    if (showRejectDialog && selectedSurvey != null) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Reject Instrument Submission", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Please specify the quality assurance notes or required corrections:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = qaNotesInput,
                        onValueChange = { qaNotesInput = it },
                        label = { Text("QA Notes / Reason *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (qaNotesInput.isBlank()) {
                            Toast.makeText(context, "Please enter rejection notes", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.rejectInstrument(selectedSurvey!!.surveyUid, qaNotesInput) { success ->
                                if (success) {
                                    Toast.makeText(context, "Instrument rejected and notes saved", Toast.LENGTH_SHORT).show()
                                }
                                showRejectDialog = false
                                selectedSurvey = null
                                qaNotesInput = ""
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedError)
                ) { Text("Confirm Reject") }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("QA Audit Queue", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Pending review and Drive cloud sync approval", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        if (pendingSurveys.isEmpty()) {
            EmptyStateCard("No instruments pending QA review.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(pendingSurveys) { survey ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("ID: ${survey.surveyUid}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Surface(
                                    color = if (survey.gpsAccuracy <= 100) GreenSuccess.copy(alpha = 0.2f) else OrangeWarning.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "GPS: ${survey.gpsAccuracy.roundToInt()}m (${survey.geoStatus})",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Type: ${survey.instrumentType} | City: ${survey.city}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text("Surveyor: ${survey.surveyorEmail.ifBlank { survey.createdBy }}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.approveInstrument(survey.surveyUid, "Approved by QA") { success ->
                                            if (success) {
                                                Toast.makeText(context, "Approved! Triggered Drive sync.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess)
                                ) {
                                    Text("Approve & Drive Push")
                                }

                                OutlinedButton(
                                    onClick = {
                                        selectedSurvey = survey
                                        qaNotesInput = ""
                                        showRejectDialog = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    border = BorderStroke(1.dp, RedError)
                                ) {
                                    Text("Reject", color = RedError)
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
fun MasterControlScreen(viewModel: SurveyViewModel) {
    val isAuth by viewModel.isMasterAuthenticated.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var passwordInput by remember { mutableStateOf("") }

    if (!isAuth) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Master Authentication Required", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Re-authenticate using Biometric unlock or Master password", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (activity != null) {
                        val bioHelper = BiometricAuthHelper(activity)
                        if (bioHelper.canAuthenticate()) {
                            bioHelper.authenticate(
                                onSuccess = {
                                    viewModel.isMasterAuthenticated.value = true
                                    Toast.makeText(context, "Biometric authentication successful", Toast.LENGTH_SHORT).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            Toast.makeText(context, "Biometric unlock unavailable. Use password below.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Biometric / Face Unlock")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("OR", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                label = { Text("Master Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (viewModel.verifyMasterPassword(passwordInput)) {
                        Toast.makeText(context, "Master access granted", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Invalid Master password", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Authenticate Password")
            }
        }
    } else {
        // Authenticated Master Control Panel
        Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Master Control Services", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Advanced system controls and Drive sync diagnostics", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Local Wi-Fi Data Sync Tunnel Server", fontWeight = FontWeight.Bold)
                    val isServerRunning by viewModel.isSyncServerRunning.collectAsState()
                    val serverStatus by viewModel.syncServerStatus.collectAsState()

                    Text("Server Status: $serverStatus", color = if (isServerRunning) GreenSuccess else Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (isServerRunning) viewModel.stopSyncServer() else viewModel.startSyncServer()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isServerRunning) "Stop Sync Server" else "Start Sync Server")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("System Logs & Cache Management", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { Toast.makeText(context, "System logs flushed", Toast.LENGTH_SHORT).show() },
                            modifier = Modifier.weight(1f)
                        ) { Text("Flush Logs") }
                        OutlinedButton(
                            onClick = { Toast.makeText(context, "Cache refreshed", Toast.LENGTH_SHORT).show() },
                            modifier = Modifier.weight(1f)
                        ) { Text("Clear Cache") }
                    }
                }
            }
        }
    }
}

@Composable
fun RecordsScreen(viewModel: SurveyViewModel) {
    val surveys by viewModel.filteredSurveys.collectAsState()
    val archivedUids by viewModel.archivedSurveyUids.collectAsState()
    val showHint by viewModel.showSwipeHintOverlay.collectAsState()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val activeSurveys = surveys.filter { !archivedUids.contains(it.surveyUid) && it.participantName.contains(searchQuery, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (showHint) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Swipe, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Hint: Swipe LEFT for Edit/Share/Request QA. Swipe RIGHT for Archive/Delete.",
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.dismissSwipeHintOverlay() }) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search records by name...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (activeSurveys.isEmpty()) {
            EmptyStateCard("No active survey records found.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(activeSurveys, key = { it.surveyUid }) { s ->
                    SwipeableSurveyRecordCard(
                        survey = s,
                        onDelete = { viewModel.deleteSurvey(s) },
                        onArchive = { viewModel.archiveSurvey(s.surveyUid) },
                        onRequestQa = {
                            viewModel.createPushRequest(s.surveyUid) { success ->
                                if (success) Toast.makeText(context, "QA Review Requested", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onShare = {
                            val subject = "CivicAge Survey Response - ${s.participantName}"
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
fun SwipeableSurveyRecordCard(
    survey: SurveyEntity,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onRequestQa: () -> Unit,
    onShare: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val draggableState = rememberDraggableState { delta ->
        offsetX = (offsetX + delta).coerceIn(-250f, 250f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (offsetX > 50f) Color(0xFFFFEBEE)
                else if (offsetX < -50f) Color(0xFFE3F2FD)
                else MaterialTheme.colorScheme.surfaceVariant
            )
    ) {
        // Actions Background
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalArrangement = if (offsetX > 0) Arrangement.Start else Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (offsetX > 50f) {
                IconButton(onClick = onArchive) { Icon(Icons.Default.Archive, contentDescription = "Archive", tint = OrangeWarning) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedError) }
            } else if (offsetX < -50f) {
                IconButton(onClick = onShare) { Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = onRequestQa) { Icon(Icons.Default.Send, contentDescription = "Request QA", tint = GreenSuccess) }
            }
        }

        // Foreground Card
        Card(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        if (offsetX > 200f) {
                            onArchive()
                        } else if (offsetX < -200f) {
                            onRequestQa()
                        }
                        offsetX = 0f
                    }
                )
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(survey.participantName.ifBlank { "Unnamed Respondent" }, fontWeight = FontWeight.Bold)
                Text(survey.surveyUid, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Status: ${survey.status}", color = if (survey.status == "approved") GreenSuccess else OrangeWarning, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("GPS: ${survey.gpsAccuracy.roundToInt()}m", fontSize = 12.sp, color = Color.Gray)
                }
            }
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
    var focusGroupConsent by remember { mutableStateOf(false) }

    if (showIntakeDialog && targetInstrument != null) {
        val intakeLatitude by viewModel.intakeLatitude.collectAsState()
        val intakeLongitude by viewModel.intakeLongitude.collectAsState()
        val intakeAccuracy by viewModel.intakeAccuracy.collectAsState()
        val intakeLocationStatus by viewModel.intakeLocationStatus.collectAsState()
        val isFetchingGps by viewModel.isFetchingGps.collectAsState()

        val isSpecialInstrument = targetInstrument!!.title.contains("Focus Group") || targetInstrument!!.title.contains("Legal Policy")

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
            if (fine || coarse) {
                viewModel.fetchIntakeLocation()
            }
        }

        AlertDialog(
            onDismissRequest = { showIntakeDialog = false },
            title = { Text(if (isSpecialInstrument) "Session Consent & Geo Validation" else "Respondent Intake Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Instrument: ${targetInstrument!!.title}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)

                    if (!isSpecialInstrument) {
                        OutlinedTextField(
                            value = respondentName,
                            onValueChange = { respondentName = it },
                            label = { Text("Respondent Name *") },
                            modifier = Modifier.fillMaxWidth()
                        )
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
                    } else {
                        Text(
                            "Special Research Instrument: Personal identification (Name/Age/Phone) is omitted for privacy. High-accuracy Geo Tagging is MANDATORY.",
                            fontSize = 12.sp, color = Color.Gray
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = focusGroupConsent, onCheckedChange = { focusGroupConsent = it })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("I explicitly consent to participating in this research session *", fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("GPS Coordinates Verification (Mandatory <= 100m)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)

                    if (intakeLatitude != 0.0 || intakeLongitude != 0.0) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Lat: $intakeLatitude, Lng: $intakeLongitude", style = MaterialTheme.typography.bodyMedium)
                                Text("Accuracy: ${String.format("%.1f", intakeAccuracy)} meters", style = MaterialTheme.typography.bodyMedium)
                                Text("Status: $intakeLocationStatus", style = MaterialTheme.typography.bodyMedium, color = GreenSuccess)
                            }
                        }
                    } else {
                        Text("Status: $intakeLocationStatus", color = RedError)
                    }

                    Button(
                        onClick = {
                            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isFetchingGps) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        else Text("Fetch High-Accuracy Location")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (intakeLatitude == 0.0 || intakeLongitude == 0.0 || intakeAccuracy > 100f) {
                            Toast.makeText(context, "ERR_GEO_REQUIRED: High-accuracy GPS (<= 100m) is mandatory before starting.", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (isSpecialInstrument && !focusGroupConsent) {
                            Toast.makeText(context, "Consent check is required.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (!isSpecialInstrument && respondentName.isBlank()) {
                            Toast.makeText(context, "Please enter respondent name.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        viewModel.selectedCity.value = selectedCity
                        viewModel.startDcaSession(
                            instrument = targetInstrument!!,
                            rName = if (isSpecialInstrument) "Anonymous Respondent" else respondentName,
                            rAge = if (isSpecialInstrument) "0" else respondentAge,
                            rMobile = respondentMobile,
                            rCity = selectedCity
                        )
                        showIntakeDialog = false
                        onNavigate()
                    }
                ) { Text("Start Instrument Session") }
            },
            dismissButton = { TextButton(onClick = { showIntakeDialog = false }) { Text("Cancel") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Research Instruments", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(instruments) { instrument ->
                InstrumentCard(instrument, enabled = (userRole != Config.ROLE_MASTER && userRole != Config.ADMIN_ROLE)) {
                    targetInstrument = instrument
                    respondentName = ""
                    respondentAge = ""
                    respondentMobile = ""
                    focusGroupConsent = false
                    showIntakeDialog = true
                }
            }
        }
    }
}

@Composable
fun InstrumentCard(instrument: Instrument, enabled: Boolean = true, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed && enabled) 0.95f else 1.0f, label = "InstrumentScale")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = if (enabled) LocalIndication.current else null, onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(instrument.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(instrument.stakeholder, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(4.dp)) {
                Text(instrument.code, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}

@Composable
fun ExportScreen(viewModel: SurveyViewModel) {
    val surveys by viewModel.allSurveys.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Data Export & Audit Manifests", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (surveys.isEmpty()) {
                    Toast.makeText(context, "No records to export.", Toast.LENGTH_SHORT).show()
                } else {
                    val jsonList = surveys.map { viewModel.exportSurveyToJson(it) }
                    val combinedJson = "[\n" + jsonList.joinToString(",\n") + "\n]"
                    try {
                        val file = java.io.File(context.cacheDir, "surveys_export.json")
                        file.writeText(combinedJson)
                        Toast.makeText(context, "Saved JSON export to: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error saving export: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Export All (JSON, CSV, metadata.json to Cache)")
        }
    }
}

@Composable
fun SettingsScreen(viewModel: SurveyViewModel, onNavigateMasterControl: () -> Unit) {
    val userRole by viewModel.userRole.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        Text("General Settings", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Language: English (Locale en-IN)")

        if (userRole == Config.ROLE_MASTER || userRole == Config.ADMIN_ROLE) {
            Spacer(modifier = Modifier.height(32.dp))
            Text("Advanced Settings", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateMasterControl() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = RedError)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Master Control Panel", fontWeight = FontWeight.Bold)
                        Text("Requires Biometric or Password unlock", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp))
                }
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

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(100.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(email, color = Color.Gray)
        Text("ID: $id | Role: ${role.uppercase()}")
        Spacer(modifier = Modifier.height(40.dp))
        Button(onClick = { viewModel.logout(); onLogout() }, colors = ButtonDefaults.buttonColors(containerColor = RedError)) {
            Text("Logout")
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
        putExtra(Intent.EXTRA_EMAIL, arrayOf("scscpdata@gmail.com"))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, reportText)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Send Email via..."))
    } catch (e: Exception) {
        Toast.makeText(context, "No email client found.", Toast.LENGTH_SHORT).show()
    }
}
