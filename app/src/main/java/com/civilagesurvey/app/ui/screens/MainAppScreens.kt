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
    var name     by remember { mutableStateOf("") }
    var id       by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context  = LocalContext.current

    // v3.4: Master login from login screen
    var showMasterLogin by remember { mutableStateOf(false) }
    var masterPassword  by remember { mutableStateOf("") }

    val inputColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor       = Color.Black,
        unfocusedTextColor     = Color.Black,
        focusedLabelColor      = Color(0xFF1E3A8A),
        unfocusedLabelColor    = Color(0xFF374151),
        focusedBorderColor     = Color(0xFF1E3A8A),
        unfocusedBorderColor   = Color(0xFF4B5563),
        focusedContainerColor  = Color.White,
        unfocusedContainerColor= Color.White
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 28.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Icon(
            Icons.Default.LocationCity,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = Color(0xFF1E3A8A)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "DCA",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black
        )
        Text(
            "Smart Cities Responsible Citizens",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF1E3A8A),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(36.dp))

        // Name (optional)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name (optional)") },
            singleLine = true,
            colors = inputColors,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(14.dp))

        // ID (required)
        OutlinedTextField(
            value = id,
            onValueChange = { id = it },
            label = { Text("Surveyor ID  e.g. SC0101") },
            singleLine = true,
            colors = inputColors,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(14.dp))

        // Password (required)
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = inputColors,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val trimId = id.trim()
                val trimPass = password.trim()
                if (trimId.isBlank()) {
                    Toast.makeText(context, "Please enter your Surveyor ID", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (trimPass.isBlank()) {
                    Toast.makeText(context, "Please enter your password", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                viewModel.signInWithIdPassword(trimId, trimPass, name) { success, msg ->
                    if (success) {
                        onLogin()
                    } else {
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            Text("Access Dashboard", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── v3.4: Master Login from Login Screen ───────────────────────────
        OutlinedButton(
            onClick = { showMasterLogin = !showMasterLogin },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF7C3AED))
        ) {
            Icon(
                Icons.Default.AdminPanelSettings,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (showMasterLogin) "Cancel Master Login" else "Login as Master")
        }

        if (showMasterLogin) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF3E8FF)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C3AED))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Master Access",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5B21B6)
                    )
                    Text(
                        "Enter the Master password to log in with full record review and approval access.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6D28D9)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = masterPassword,
                        onValueChange = { masterPassword = it },
                        label = { Text("Master Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            // Log in directly as MASTER via the master account
                            viewModel.signInWithIdPassword("MASTER", masterPassword, "Master Administrator") { success, msg ->
                                if (success) {
                                    onLogin()
                                } else {
                                    Toast.makeText(context, "Incorrect master password", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                    ) {
                        Text("Enter as Master", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
        // ──────────────────────────────────────────────────────────────────

        Spacer(modifier = Modifier.height(40.dp))
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
        if (userRole == Config.ROLE_MASTER || userRole == Config.ADMIN_ROLE) {
            Text("DCA Master Control", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text("Smart Cities Responsible Citizens Initiative | Records Reviewer", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(24.dp))

            DashboardStats(allSurveys.size, draftCount, completedCount, syncedCount)
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("records") },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Review & Approve Records",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Inspect submitted surveys, view answers, and approve for Google Drive & Google Sheets sync.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        } else {
            Text("DCA Field Portal", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text("Smart Cities Responsible Citizens Initiative | Field Surveyor", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(24.dp))

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
                        "Click here to select an instrument and begin a high-accuracy geo-tagged survey session.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("records") },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("My Submitted Records", fontWeight = FontWeight.Bold)
                        Text("View past submitted sessions and sync status", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
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
                                        viewModel.approveInstrument(survey.surveyUid, "Approved by QA") { success, docUrl ->
                                            if (success) {
                                                Toast.makeText(context, "Approved! Record appended to Doc.", Toast.LENGTH_SHORT).show()
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
    var passwordInput by remember { mutableStateOf("") }

    // v3.4: Countdown timer state
    val secondsRemaining by viewModel.masterSecondsRemaining.collectAsState()
    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    val timerLabel = "%d:%02d".format(minutes, seconds)
    val isExpiringSoon = secondsRemaining <= 60

    if (!isAuth) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.AdminPanelSettings,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Master Access",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Enter the Master password to switch your session to Master role. This gives you full record review and approval access across all devices.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Session lasts 5 minutes after activation.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7C3AED),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(28.dp))

            OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                label = { Text("Master Password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (viewModel.switchToMasterRole(passwordInput)) {
                        Toast.makeText(context, "Master role activated! Session expires in 5 min.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Incorrect password", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Switch to Master Role", fontWeight = FontWeight.Bold)
            }
        }
    } else {
        // ── Authenticated Master Control Panel ─────────────────────────────
        Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {

            // v3.4: Session countdown banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isExpiringSoon) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isExpiringSoon) Color(0xFFEF5350) else Color(0xFF66BB6A)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isExpiringSoon) Icons.Default.Warning else Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (isExpiringSoon) Color(0xFFEF5350) else Color(0xFF388E3C),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                if (isExpiringSoon) "Session expiring soon!" else "Master Session Active",
                                fontWeight = FontWeight.Bold,
                                color = if (isExpiringSoon) Color(0xFFEF5350) else Color(0xFF388E3C),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Time remaining: $timerLabel",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isExpiringSoon) Color(0xFFEF5350) else Color(0xFF388E3C)
                            )
                        }
                    }
                    // Logout Now / Logout Master button
                    OutlinedButton(
                        onClick = {
                            viewModel.logoutMaster()
                            Toast.makeText(context, "Master session ended", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF5350))
                    ) {
                        Text(if (isExpiringSoon) "Logout Now" else "Logout Master", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Master Control Services", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Advanced system controls and Drive sync diagnostics", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Real-Time Master Cloud Data Tunnel", fontWeight = FontWeight.Bold)
                    Text("Automatically syncs & pulls all survey records submitted from any device (SC0101-SC0125 & Guest mode).", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.syncCloudRecordsToMaster { count ->
                                Toast.makeText(context, "Cloud sync complete: $count records updated on Master Board.", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sync All Cloud Records Now", fontWeight = FontWeight.Bold)
                    }
                }
            }

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
                        onClick = { if (isServerRunning) viewModel.stopSyncServer() else viewModel.startSyncServer() },
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

            Spacer(modifier = Modifier.height(24.dp))

            // v3.4: Prominent Logout Master button at bottom
            Button(
                onClick = {
                    viewModel.logoutMaster()
                    Toast.makeText(context, "Master session ended. Re-enter password to switch again.", Toast.LENGTH_LONG).show()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout Master Session", fontWeight = FontWeight.Bold)
            }
        }
        // ──────────────────────────────────────────────────────────────────
    }
}


@Composable
fun RecordsScreen(viewModel: SurveyViewModel) {
    val surveys by viewModel.filteredSurveys.collectAsState()
    val archivedUids by viewModel.archivedSurveyUids.collectAsState()
    val showHint by viewModel.showSwipeHintOverlay.collectAsState()
    val context = LocalContext.current
    val userRole by viewModel.userRole.collectAsState()
    val isMaster = userRole == Config.ROLE_MASTER || userRole == Config.ADMIN_ROLE
    var searchQuery by remember { mutableStateOf("") }

    val activeSurveys = surveys.filter { !archivedUids.contains(it.surveyUid) && it.participantName.contains(searchQuery, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        if (isMaster) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Master Board Live Auto-Sync", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("Live cloud tunnel streaming surveys from all devices & Guest mode", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.syncCloudRecordsToMaster { count ->
                                Toast.makeText(context, "Synced $count records from cloud to Master Board!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync Cloud Records")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync Cloud", fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
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
                        isMaster = isMaster,
                        onDelete = { viewModel.deleteSurvey(s) },
                        onArchive = { viewModel.archiveSurvey(s.surveyUid) },
                        onApprove = {
                            if (isMaster) {
                                // Master only: POST to Apps Script → appends to Google Doc → open Doc
                                viewModel.approveInstrument(s.surveyUid, "Approved by Master") { success, docUrl ->
                                    if (success) {
                                        Toast.makeText(context, "Approved! Opening Google Doc...", Toast.LENGTH_LONG).show()
                                        if (docUrl != null) {
                                            val intent = android.content.Intent(
                                                android.content.Intent.ACTION_VIEW,
                                                android.net.Uri.parse(docUrl)
                                            )
                                            context.startActivity(intent)
                                        }
                                    } else {
                                        Toast.makeText(context, "Sync failed. Check network.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                // Student / Surveyor: Submit & Sync to Cloud & Doc
                                viewModel.approveInstrument(s.surveyUid, "Submitted by Surveyor") { success, _ ->
                                    if (success) {
                                        Toast.makeText(context, "Record submitted & synced successfully!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Submission failed. Check network.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        onView = {
                            // Any user: share via email
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
    isMaster: Boolean = false,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onApprove: () -> Unit = {},
    onView: () -> Unit = {}
) {
    var offsetX by remember { mutableStateOf(0f) }
    val draggableState = rememberDraggableState { delta ->
        offsetX = (offsetX + delta).coerceIn(-250f, 250f)
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(survey.participantName.ifBlank { "Respondent Record" }, fontWeight = FontWeight.Bold)
            Text("Record ID: ${survey.surveyUid}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text("Instrument: ${survey.instrumentType}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Status: ${survey.status}", color = if (survey.status == "approved") GreenSuccess else OrangeWarning, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("GPS ±${survey.gpsAccuracy.roundToInt()}m", fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                // View button — visible to all users
                OutlinedButton(
                    onClick = onView,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("View", fontSize = 12.sp)
                }

                // Submit / Sync to Doc button — available to students/surveyors to push/re-push to cloud & doc
                if (!isMaster) {
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (survey.isSynced) "Submitted" else "Submit", fontSize = 12.sp)
                    }
                }

                // Approve & Doc button — Master only
                if (isMaster) {
                    Button(
                        onClick = onApprove,
                        enabled = survey.status != "approved",
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (survey.status == "approved") Color.Gray else GreenSuccess
                        )
                    ) {
                        Text(
                            if (survey.status == "approved") "✓ Approved" else "Approve & Doc",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
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

                    val targetIndex = instruments.indexOf(targetInstrument)
                    if (!isSpecialInstrument) {
                        OutlinedTextField(
                            value = respondentName,
                            onValueChange = { respondentName = it },
                            label = { Text("Respondent Name (Optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (targetIndex <= 1) {
                            OutlinedTextField(
                                value = respondentAge,
                                onValueChange = { respondentAge = it },
                                label = { Text("Respondent Age (Optional)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
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

                        viewModel.selectedCity.value = selectedCity
                        viewModel.startDcaSession(
                            instrument = targetInstrument!!,
                            rName = if (isSpecialInstrument || respondentName.isBlank()) "Anonymous Respondent" else respondentName,
                            rAge = if (isSpecialInstrument || respondentAge.isBlank()) "0" else respondentAge,
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
        Spacer(modifier = Modifier.height(8.dp))

        if (userRole == Config.ROLE_MASTER || userRole == Config.ADMIN_ROLE) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            ) {
                Text(
                    "Master Reviewer Mode: Master users review and approve submitted records under the Records screen, and cannot take new instruments directly.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

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
    val appearanceMode by viewModel.appearanceMode.collectAsState()
    val fontSizeMode by viewModel.fontSizeMode.collectAsState()

    val appearanceOptions = listOf("Professional Light", "Dark Mode", "Warm Sepia", "Modern Indigo", "High Contrast")
    val fontSizeOptions = listOf("Standard", "Large", "Extra Large")

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        // ─── Appearance ───────────────────────────────────────────────
        Text("Appearance", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Theme Mode", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    appearanceOptions.forEach { option ->
                        val isSelected = appearanceMode == option
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.appearanceMode.value = option },
                            label = {
                                Text(
                                    when (option) {
                                        "Professional Light" -> "Light"
                                        "Dark Mode" -> "Dark"
                                        "Warm Sepia" -> "Warm Sepia"
                                        "Modern Indigo" -> "Modern Indigo"
                                        else -> "High Contrast"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ─── Font Size ───────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Font Size", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    fontSizeOptions.forEach { option ->
                        FilterChip(
                            selected = fontSizeMode == option,
                            onClick = { viewModel.fontSizeMode.value = option },
                            label = { Text(option, fontSize = 12.sp) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ─── General ─────────────────────────────────────────────────
        Text("General", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Language", fontWeight = FontWeight.SemiBold)
                Text("English (Locale en-IN)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }

        // Master Control Panel - accessible to ALL users to allow role switching
        Spacer(modifier = Modifier.height(24.dp))
        Text("Advanced", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateMasterControl() },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = RedError)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Master Control Panel", fontWeight = FontWeight.Bold)
                    Text("Enter master password to switch to Master role", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp))
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
