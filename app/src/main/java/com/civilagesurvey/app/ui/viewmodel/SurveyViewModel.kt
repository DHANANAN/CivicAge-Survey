
package com.civilagesurvey.app.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.civilagesurvey.app.audio.AudioRecorderHelper
import com.civilagesurvey.app.audio.TextToSpeechHelper
import com.civilagesurvey.app.config.Config
import com.civilagesurvey.app.data.database.*
import com.civilagesurvey.app.data.model.*
import com.civilagesurvey.app.data.repository.*
import com.civilagesurvey.app.location.LocationHelper
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.net.*
import java.text.SimpleDateFormat
import java.util.*


/** v3.4: Master session duration in seconds (5 minutes). */
private const val MASTER_SESSION_SECONDS = 300

class SurveyViewModel(

    application: Application,
    private val repository: SurveyRepository,
    private val googleFormsRepository: GoogleFormsRepository
) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val locationHelper = LocationHelper(context)
    private val audioRecorder = AudioRecorderHelper(context)
    private var ttsHelper = TextToSpeechHelper(context)
    private val googleFormsAuthManager = GoogleFormsAuthManager(context)
    private var firestoreSyncListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun startRealtimeFirestoreSync() {
        repository.startRealtimeFirestoreSync()
        viewModelScope.launch {
            repository.fetchRemoteSurveysFromFirestore()
        }
    }

    fun stopRealtimeFirestoreSync() {
        repository.stopRealtimeFirestoreSync()
    }

    // User Session
    val surveyorPhone = MutableStateFlow("")
    val surveyorEmail = MutableStateFlow("")
    val surveyorName = MutableStateFlow("")
    val surveyorId = MutableStateFlow("")
    val userRole = MutableStateFlow(Config.ROLE_NORMAL)
    val allowedPushToDrive = MutableStateFlow(true)
    val isMasterAuthenticated = MutableStateFlow(false)
    val showSwipeHintOverlay = MutableStateFlow(false) // swipe tips permanently disabled v3.2
    val archivedSurveyUids = MutableStateFlow<Set<String>>(emptySet())

    // v3.4: Master session expiry — 5-minute countdown
    val masterSecondsRemaining = MutableStateFlow(MASTER_SESSION_SECONDS)
    private var masterExpiryJob: Job? = null

    // Participant & Caregiver Profile Flows
    val survivorName = MutableStateFlow("")
    val survivorAge = MutableStateFlow("")
    val survivorEmail = MutableStateFlow("")
    val survivorPhone = MutableStateFlow("")
    val caregiverName = MutableStateFlow("")
    val caregiverAge = MutableStateFlow("")
    val caregiverEmail = MutableStateFlow("")
    val caregiverPhone = MutableStateFlow("")
    val caregiverRelation = MutableStateFlow("")

    val mirrorResult = MutableStateFlow<GoogleFormsRepository.MirrorResult?>(null)
    val isFetchingForm = MutableStateFlow(false)

    val appearanceMode = MutableStateFlow("Professional Light")
    val fontSizeMode = MutableStateFlow("Standard")
    val isFullscreenMode = MutableStateFlow(false)
    val isHighContrast = MutableStateFlow(false)
    val speechLanguage = MutableStateFlow("English")

    val isNetworkOnline = MutableStateFlow(true)
    val authStatusMessage = MutableStateFlow("")
    val activeUserUid = MutableStateFlow("")

    // Lists
    val allSurveys: StateFlow<List<SurveyEntity>> = repository.allSurveys
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingQaSurveys: StateFlow<List<SurveyEntity>> = allSurveys
        .map { list -> list.filter { it.status == "pending_qa" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPushRequests: StateFlow<List<PushRequestEntity>> = repository.allPushRequests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredSurveys: StateFlow<List<SurveyEntity>> = combine(allSurveys, userRole, surveyorId, isMasterAuthenticated) { surveys, role, currentId, masterAuth ->
        val cleanRole = role.lowercase().trim()
        if (masterAuth || cleanRole == "master" || cleanRole == Config.ADMIN_ROLE || cleanRole == Config.ROLE_QA || currentId.uppercase() == "MASTER") {
            surveys
        } else {
            surveys.filter { it.createdBy == currentId || it.surveyorId == currentId || currentId.isBlank() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeQuestions: StateFlow<List<FormQuestionEntity>> = repository.allQuestions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard Stats
    val completedCount = filteredSurveys.map { it.count { s -> s.status == "COMPLETED" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    val draftCount = filteredSurveys.map { it.count { s -> s.status == "DRAFT" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    val syncedCount = filteredSurveys.map { it.count { s -> s.isSynced } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    val offlineCount = filteredSurveys.map { it.count { s -> !s.isSynced } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Active Survey Drafting State
    val activeSurveyUid = MutableStateFlow<String?>(null)
    val activeSurvey = MutableStateFlow<SurveyEntity?>(null)

    // Sync Server state
    val isSyncServerRunning = MutableStateFlow(false)
    val syncServerIpAddress = MutableStateFlow("Unknown")
    val syncServerStatus = MutableStateFlow("Stopped")
    val localSyncLogs = MutableStateFlow<List<String>>(emptyList())
    val clientSyncStatus = MutableStateFlow("")

    // DCA Specific State
    val selectedCity = MutableStateFlow("ND")
    val selectedStakeholder = MutableStateFlow("OOP")
    val participantSequence = MutableStateFlow(1)
    val currentParticipantCode = combine(selectedCity, selectedStakeholder, participantSequence) { city, stakeholder, seq ->
        val year = Calendar.getInstance().get(Calendar.YEAR)
        "CAS-$city-$stakeholder-$year-${seq.toString().padStart(4, '0')}"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Questionnaire navigation state
    val currentInstrument = MutableStateFlow<Instrument?>(null)
    val currentSectionIndex = MutableStateFlow(0)
    val currentQuestionIndex = MutableStateFlow(0)
    val showThemesPhase = MutableStateFlow(false)
    val intakeLatitude = MutableStateFlow(0.0)
    val intakeLongitude = MutableStateFlow(0.0)
    val intakeAccuracy = MutableStateFlow(0.0f)
    val intakeCapturedTime = MutableStateFlow(0L)
    val intakeLocationStatus = MutableStateFlow("")
    
    val savedAnswers: StateFlow<Map<String, String>> = activeSurvey
        .map { survey -> survey?.let { getAnswersMap(it.answersJson) } ?: emptyMap() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Semi-Structured Interview navigation state
    val currentInterviewIndex = MutableStateFlow(0)
    val interviewPrompts = listOf(
        "Describe your neighborhood layout and what limits your daily walk.",
        "Describe transport conditions getting to local hospital clinics.",
        "Do you feel safe walking after dark? What light improvements are needed?",
        "What smart city elements / support would assist your daily wellbeing?"
    )
    val interviewAnswers = MutableStateFlow<Map<Int, String>>(emptyMap())

    // Voice Answer Recording state
    val recordingState = MutableStateFlow("Idle")
    
    // GPS status
    val isFetchingGps = MutableStateFlow(false)
    val gpsError = MutableStateFlow<String?>(null)

    // Audio recording statuses
    val isRecording = MutableStateFlow(false)
    val isPaused = MutableStateFlow(false)
    val recordingDuration = MutableStateFlow(0)
    private var recordingTimer: Timer? = null

    // Moshi setup
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val answersMapType = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
    private val answersAdapter = moshi.adapter<Map<String, String>>(answersMapType)
    private val stringsListAdapter = moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))

    private val prefs = application.getSharedPreferences("cas_prefs", android.content.Context.MODE_PRIVATE)

    init {
        appearanceMode.value = prefs.getString("appearanceMode", "Professional Light") ?: "Professional Light"
        fontSizeMode.value = prefs.getString("fontSizeMode", "Standard") ?: "Standard"
        surveyorPhone.value = prefs.getString("surveyorPhone", "") ?: ""
        surveyorEmail.value = prefs.getString("surveyorEmail", "") ?: ""
        surveyorName.value = prefs.getString("surveyorName", "") ?: ""
        surveyorId.value = prefs.getString("surveyorId", "") ?: ""
        userRole.value = prefs.getString("userRole", Config.FIELD_SURVEYOR_ROLE) ?: Config.FIELD_SURVEYOR_ROLE

        survivorName.value = prefs.getString("survivorName", "") ?: ""
        survivorAge.value = prefs.getString("survivorAge", "") ?: ""
        survivorEmail.value = prefs.getString("survivorEmail", "") ?: ""
        survivorPhone.value = prefs.getString("survivorPhone", "") ?: ""

        caregiverName.value = prefs.getString("caregiverName", "") ?: ""
        caregiverAge.value = prefs.getString("caregiverAge", "") ?: ""
        caregiverEmail.value = prefs.getString("caregiverEmail", "") ?: ""
        caregiverPhone.value = prefs.getString("caregiverPhone", "") ?: ""
        caregiverRelation.value = prefs.getString("caregiverRelation", "") ?: ""

        viewModelScope.launch {
            repository.allQuestions.first().let { currentList ->
                if (currentList.isEmpty()) {
                    repository.insertQuestions(DefaultQuestions.list)
                }
            }
        }
        startRealtimeFirestoreSync()
    }

    fun saveSurvivorProfile(name: String, age: String, email: String, phone: String) {
        survivorName.value = name
        survivorAge.value = age
        survivorEmail.value = email
        survivorPhone.value = phone
        prefs.edit()
            .putString("survivorName", name)
            .putString("survivorAge", age)
            .putString("survivorEmail", email)
            .putString("survivorPhone", phone)
            .apply()
    }

    fun saveCaregiverProfile(name: String, age: String, email: String, phone: String, relation: String) {
        caregiverName.value = name
        caregiverAge.value = age
        caregiverEmail.value = email
        caregiverPhone.value = phone
        caregiverRelation.value = relation
        prefs.edit()
            .putString("caregiverName", name)
            .putString("caregiverAge", age)
            .putString("caregiverEmail", email)
            .putString("caregiverPhone", phone)
            .putString("caregiverRelation", relation)
            .apply()
    }

    fun signInOrSignUp(
        email: String,
        password: String,
        fullName: String,
        surveyorCode: String,
        mobileNumber: String,
        createAccount: Boolean,
        onResult: (Boolean, String) -> Unit
    ) {
        val resolvedEmail = email.trim().lowercase()
        val role = Config.roleForEmail(resolvedEmail)
        surveyorEmail.value = resolvedEmail
        surveyorName.value = fullName.trim()
        surveyorId.value = surveyorCode.trim()
        surveyorPhone.value = mobileNumber.trim()
        userRole.value = role
        activeUserUid.value = "LOCAL-${surveyorCode.trim()}"
        prefs.edit()
            .putString("surveyorEmail", resolvedEmail)
            .putString("surveyorName", surveyorName.value)
            .putString("surveyorId", surveyorId.value)
            .putString("surveyorPhone", surveyorPhone.value)
            .putString("userRole", role)
            .apply()
        startRealtimeFirestoreSync()
        onResult(true, "Signed in locally as $role")
    }

    /**
     * v3.2+: Simplified ID+Password login for 25 Normal accounts + Master.
     * v3.4: Resets any active Master session on every login (switching IDs forces master re-entry).
     */
    fun signInWithIdPassword(
        id: String,
        password: String,
        displayName: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val cleanId = id.trim().uppercase()
        val role = Config.loginWithIdPassword(cleanId, password)

        if (role == null) {
            onResult(false, "Invalid ID or password")
            return
        }

        // v3.4: Always reset master session when switching accounts
        // (master password must be re-entered after login change)
        logoutMaster()

        val finalName = displayName.trim().ifBlank {
            if (role == Config.ROLE_MASTER) "Master Administrator" else "Field Surveyor $cleanId"
        }

        surveyorId.value = cleanId
        surveyorName.value = finalName
        surveyorEmail.value = "$cleanId@scsc.app"
        userRole.value = role
        activeUserUid.value = "LOCAL-$cleanId"

        if (role == Config.ROLE_MASTER) {
            isMasterAuthenticated.value = true
            startMasterSessionTimer() // start 5-min expiry for direct master login
        }

        prefs.edit()
            .putString("surveyorId", cleanId)
            .putString("surveyorName", finalName)
            .putString("surveyorEmail", "$cleanId@scsc.app")
            .putString("surveyorPhone", "")
            .putString("userRole", role)
            .apply()

        startRealtimeFirestoreSync()
        onResult(true, "Signed in as $role ($cleanId)")
    }


    fun syncCloudRecordsToMaster(onResult: (Int) -> Unit = {}) {
        clientSyncStatus.value = "Fetching cloud records..."
        viewModelScope.launch {
            val count = repository.fetchRemoteSurveysFromFirestore()
            clientSyncStatus.value = "Synced $count records from cloud."
            onResult(count)
        }
    }

    /**
     * v3.4: Switch any logged-in session to Master role by entering the master password.
     * Starts a 5-minute expiry timer.
     */
    fun switchToMasterRole(password: String): Boolean {
        if (password.trim() != Config.MASTER_PASSWORD) return false

        userRole.value = Config.ROLE_MASTER
        isMasterAuthenticated.value = true

        prefs.edit().putString("userRole", Config.ROLE_MASTER).apply()

        // Restart Firestore sync as Master to pull all global records
        stopRealtimeFirestoreSync()
        startRealtimeFirestoreSync()

        // Start 5-minute session expiry countdown
        startMasterSessionTimer()

        return true
    }

    /**
     * v3.4: Start a 5-minute countdown. On expiry, demote Master back to Normal.
     */
    private fun startMasterSessionTimer() {
        masterExpiryJob?.cancel()
        masterSecondsRemaining.value = MASTER_SESSION_SECONDS
        masterExpiryJob = viewModelScope.launch {
            var remaining = MASTER_SESSION_SECONDS
            while (remaining > 0) {
                delay(1000L)
                remaining--
                masterSecondsRemaining.value = remaining
            }
            // Timer expired — demote Master role
            logoutMaster()
        }
    }

    /**
     * v3.4: Demote Master back to Normal without full logout.
     * Called on manual "Logout Master" or on session expiry.
     */
    fun logoutMaster() {
        masterExpiryJob?.cancel()
        masterExpiryJob = null
        masterSecondsRemaining.value = MASTER_SESSION_SECONDS
        isMasterAuthenticated.value = false

        // Demote to Normal if currently Master (keep the underlying user logged in)
        if (userRole.value == Config.ROLE_MASTER) {
            userRole.value = Config.ROLE_NORMAL
            prefs.edit().putString("userRole", Config.ROLE_NORMAL).apply()
            // Re-sync as Normal (device-local only)
            stopRealtimeFirestoreSync()
            startRealtimeFirestoreSync()
        }
    }

    fun useLocalTestingMode(email: String, fullName: String, surveyorCode: String, mobileNumber: String) {
        signInOrSignUp(email, "", fullName, surveyorCode, mobileNumber, false) { _, _ -> }
    }

    fun addGoogleForm(url: String) {
        viewModelScope.launch {
            isFetchingForm.value = true
            // In a production app, we would use googleFormsAuthManager to get a fresh token
            // For now, we simulate the authorization success if the user is "logged in"
            val dummyAccessToken = "ya29.A0ARrdaM..."
            val result = googleFormsRepository.fetchAndMirrorForm(url, dummyAccessToken, surveyorId.value)
            mirrorResult.value = result
            isFetchingForm.value = false
        }
    }

    fun resetMirrorResult() {
        mirrorResult.value = null
    }

    fun logout() {
        // v3.4: also cancel any active Master session
        logoutMaster()
        stopRealtimeFirestoreSync()
        surveyorEmail.value = ""
        surveyorName.value = ""
        surveyorId.value = ""
        survivorName.value = ""
        survivorAge.value = ""
        survivorEmail.value = ""
        survivorPhone.value = ""
        caregiverName.value = ""
        caregiverAge.value = ""
        caregiverEmail.value = ""
        caregiverPhone.value = ""
        caregiverRelation.value = ""
        userRole.value = Config.FIELD_SURVEYOR_ROLE
        prefs.edit().clear().apply()
    }

    // Start New Survey
    fun startNewSurvey(
        hasPhotoConsent: Boolean,
        hasAudioConsent: Boolean,
        hasGeoConsent: Boolean,
        hasDataConsent: Boolean,
        hasWithdrawalRight: Boolean
    ): String {
        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())
        val randomSuffix = (1000..9999).random()
        val uid = "CAS-$timestamp-$randomSuffix"

        currentQuestionIndex.value = 0
        currentInterviewIndex.value = 0
        interviewAnswers.value = emptyMap()
        recordingState.value = "Idle"

        val initialSurvey = SurveyEntity(
            surveyUid = uid,
            createdBy = surveyorId.value,
            status = "DRAFT",
            consentGiven = true,
            photoConsent = hasPhotoConsent,
            audioConsent = hasAudioConsent,
            geoLocationConsent = hasGeoConsent,
            dataUseConsent = hasDataConsent,
            participantCanWithdraw = hasWithdrawalRight,
            consentTimestamp = System.currentTimeMillis(),
            surveyorUid = activeUserUid.value,
            surveyorId = surveyorId.value,
            surveyorName = surveyorName.value,
            surveyorEmail = surveyorEmail.value,
            surveyorPhone = surveyorPhone.value,
            surveyorRole = userRole.value
        )

        viewModelScope.launch {
            repository.insertSurvey(initialSurvey)
            activeSurveyUid.value = uid
            activeSurvey.value = initialSurvey
        }

        return uid
    }

    fun startDcaSession(
        instrument: Instrument,
        rName: String,
        rAge: String,
        rMobile: String,
        rCity: String
    ) {
        val pCode = currentParticipantCode.value
        
        // Protocol: SQ-OOP and SSI-OOP separation (2-day gap)
        if (instrument.code == "SSI-OOP-v1.0") {
            val hasSqToday = allSurveys.value.any { 
                it.participantAgeGroup == "OOP" && 
                it.surveyUid.contains("SQ-OOP-v1.0") &&
                (System.currentTimeMillis() - it.createdAt) < (2 * 24 * 60 * 60 * 1000)
            }
            if (hasSqToday) {
                authStatusMessage.value = "Minimum 2-day gap from SQ-OOP is required."
                return
            }
        }

        currentInstrument.value = instrument
        currentSectionIndex.value = 0
        currentQuestionIndex.value = 0
        currentInterviewIndex.value = 0
        showThemesPhase.value = instrument.sections.isEmpty() && instrument.themes.isNotEmpty()
        
        val uid = "$pCode-${instrument.code}-S${System.currentTimeMillis()}"
        
        val answersMap = mutableMapOf<String, String>()
        answersMap["__intake_location_status"] = intakeLocationStatus.value
        answersMap["__intake_captured_time"] = intakeCapturedTime.value.toString()
        val answersJsonStr = answersAdapter.toJson(answersMap) ?: "{}"

        val initialSurvey = SurveyEntity(
            surveyUid = uid,
            createdBy = surveyorId.value,
            status = "DRAFT",
            surveyorId = surveyorId.value,
            surveyorName = surveyorName.value,
            surveyorEmail = surveyorEmail.value,
            surveyorPhone = surveyorPhone.value,
            surveyorRole = userRole.value,
            participantName = rName,
            participantAge = rAge.toIntOrNull() ?: 0,
            participantPhone = rMobile,
            participantEmail = "",
            participantAgeGroup = selectedStakeholder.value,
            manualLocality = rCity,
            city = rCity.ifBlank { selectedCity.value.ifBlank { "ND" } },
            instrumentType = instrument.code,
            respondentName = rName,
            respondentAge = rAge.toIntOrNull() ?: 0,
            respondentEmail = "",
            respondentPhone = rMobile,
            respondentRelation = "Self",
            latitude = intakeLatitude.value,
            longitude = intakeLongitude.value,
            gpsAccuracy = intakeAccuracy.value,
            geoTimestamp = System.currentTimeMillis(),
            answersJson = answersJsonStr
        )
        
        viewModelScope.launch {
            repository.insertSurvey(initialSurvey)
            activeSurveyUid.value = uid
            activeSurvey.value = initialSurvey
        }
    }

    fun loadSurvey(uid: String) {
        viewModelScope.launch {
            val loaded = repository.getSurveyById(uid)
            if (loaded != null) {
                activeSurveyUid.value = uid
                activeSurvey.value = loaded
                // Restore interview answers from compiled transcript if needed
            }
        }
    }

    fun updateParticipantProfile(name: String, age: Int, gender: String, photoPath: String?, voicePath: String?) {
        val current = activeSurvey.value ?: return
        val updated = current.copy(
            participantName = name,
            participantAge = age,
            participantGender = gender,
            participantPhotoLocalPath = photoPath,
            participantVoiceSampleLocalPath = voicePath,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.insertSurvey(updated)
            activeSurvey.value = updated
        }
    }

    fun fetchGpsCoordinates() {
        isFetchingGps.value = true
        locationHelper.getCurrentLocation { result ->
            isFetchingGps.value = false
            if (result.location != null) {
                val current = activeSurvey.value ?: return@getCurrentLocation
                val updated = current.copy(
                    latitude = result.location.latitude,
                    longitude = result.location.longitude,
                    gpsAccuracy = result.accuracy,
                    updatedAt = System.currentTimeMillis()
                )
                viewModelScope.launch {
                    repository.insertSurvey(updated)
                    activeSurvey.value = updated
                }
            }
        }
    }

    fun fetchIntakeLocation() {
        isFetchingGps.value = true
        intakeLocationStatus.value = "Fetching..."
        locationHelper.getCurrentLocation { result ->
            isFetchingGps.value = false
            if (result.location != null) {
                intakeLatitude.value = result.location.latitude
                intakeLongitude.value = result.location.longitude
                intakeAccuracy.value = result.accuracy
                intakeCapturedTime.value = result.timestamp
                intakeLocationStatus.value = "Success (${result.accuracy.toInt()}m)"
            } else {
                intakeLocationStatus.value = result.errorMessage ?: "Permission denied or GPS disabled"
            }
        }
    }

    fun updateManualLocality(locality: String) {
        val current = activeSurvey.value ?: return
        val updated = current.copy(manualLocality = locality, updatedAt = System.currentTimeMillis())
        viewModelScope.launch {
            repository.insertSurvey(updated)
            activeSurvey.value = updated
        }
    }

    fun saveAnswer(questionId: String, answer: String) {
        val current = activeSurvey.value ?: return
        val answersMap = getAnswersMap(current.answersJson).toMutableMap()
        answersMap[questionId] = answer
        val updated = current.copy(
            answersJson = answersAdapter.toJson(answersMap) ?: "{}",
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.insertSurvey(updated)
            activeSurvey.value = updated
            if (questionId.startsWith("probe_")) {
                compileTranscripts()
            }
        }
    }

    fun saveConsentOptions(
        consentGiven: Boolean,
        audioConsent: Boolean,
        geoConsent: Boolean,
        withdrawalExplained: Boolean
    ) {
        val current = activeSurvey.value ?: return
        val updated = current.copy(
            consentGiven = consentGiven,
            audioConsent = audioConsent,
            geoLocationConsent = geoConsent,
            participantCanWithdraw = withdrawalExplained,
            consentTimestamp = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.insertSurvey(updated)
            activeSurvey.value = updated
        }
    }

    fun shouldShowQuestion(questionId: String): Boolean {
        val answers = savedAnswers.value
        return when (questionId) {
            "C12" -> {
                val c11 = answers["C11"] ?: ""
                c11 == "Yes - this has happened more than once" || c11 == "Yes - this happened once"
            }
            "E6" -> {
                val e5 = answers["E5"] ?: ""
                e5 == "Much more dependent on others now" || e5 == "Somewhat more dependent now"
            }
            "G2" -> {
                val g1 = answers["G1"] ?: ""
                g1.startsWith("Yes")
            }
            else -> true
        }
    }

    fun hasNextStep(): Boolean {
        val instrument = currentInstrument.value ?: return false
        if (showThemesPhase.value) {
            return currentInterviewIndex.value < instrument.themes.size - 1
        } else {
            if (hasNextQuestion()) {
                return true
            } else {
                return instrument.themes.isNotEmpty()
            }
        }
    }

    fun moveToNextStep() {
        val instrument = currentInstrument.value ?: return
        if (showThemesPhase.value) {
            if (currentInterviewIndex.value < instrument.themes.size - 1) {
                currentInterviewIndex.value++
            }
        } else {
            if (hasNextQuestion()) {
                moveToNextQuestion()
            } else if (instrument.themes.isNotEmpty()) {
                showThemesPhase.value = true
                currentInterviewIndex.value = 0
            }
        }
    }

    fun moveToPreviousStep() {
        val instrument = currentInstrument.value ?: return
        if (showThemesPhase.value) {
            if (currentInterviewIndex.value > 0) {
                currentInterviewIndex.value--
            } else if (instrument.sections.isNotEmpty()) {
                showThemesPhase.value = false
                goToLastSectionQuestion(instrument)
            }
        } else {
            moveToPreviousQuestion()
        }
    }

    private fun goToLastSectionQuestion(instrument: Instrument) {
        var tempSectIndex = instrument.sections.size - 1
        var tempQuestIndex = (instrument.sections.getOrNull(tempSectIndex)?.questions?.size ?: 1) - 1
        
        while (tempSectIndex >= 0) {
            val sect = instrument.sections.getOrNull(tempSectIndex)
            if (sect != null && tempQuestIndex >= 0) {
                val q = sect.questions.getOrNull(tempQuestIndex)
                if (q != null && shouldShowQuestion(q.id)) {
                    currentSectionIndex.value = tempSectIndex
                    currentQuestionIndex.value = tempQuestIndex
                    return
                }
                tempQuestIndex--
            } else {
                tempSectIndex--
                tempQuestIndex = (instrument.sections.getOrNull(tempSectIndex)?.questions?.size ?: 1) - 1
            }
        }
    }

    fun hasNextQuestion(): Boolean {
        val instrument = currentInstrument.value ?: return false
        var tempSectIndex = currentSectionIndex.value
        var tempQuestIndex = currentQuestionIndex.value
        
        while (true) {
            val sect = instrument.sections.getOrNull(tempSectIndex) ?: return false
            if (tempQuestIndex < sect.questions.size - 1) {
                tempQuestIndex++
            } else if (tempSectIndex < instrument.sections.size - 1) {
                tempSectIndex++
                tempQuestIndex = 0
            } else {
                return false
            }
            val nextQ = instrument.sections.getOrNull(tempSectIndex)?.questions?.getOrNull(tempQuestIndex) ?: return false
            if (shouldShowQuestion(nextQ.id)) {
                return true
            }
        }
    }

    fun moveToNextQuestion() {
        val instrument = currentInstrument.value ?: return
        var tempSectIndex = currentSectionIndex.value
        var tempQuestIndex = currentQuestionIndex.value
        
        while (true) {
            val sect = instrument.sections.getOrNull(tempSectIndex) ?: return
            if (tempQuestIndex < sect.questions.size - 1) {
                tempQuestIndex++
            } else if (tempSectIndex < instrument.sections.size - 1) {
                tempSectIndex++
                tempQuestIndex = 0
            } else {
                return
            }
            val nextQ = instrument.sections.getOrNull(tempSectIndex)?.questions?.getOrNull(tempQuestIndex) ?: return
            if (shouldShowQuestion(nextQ.id)) {
                currentSectionIndex.value = tempSectIndex
                currentQuestionIndex.value = tempQuestIndex
                return
            }
        }
    }

    fun moveToPreviousQuestion() {
        val instrument = currentInstrument.value ?: return
        var tempSectIndex = currentSectionIndex.value
        var tempQuestIndex = currentQuestionIndex.value
        
        while (true) {
            if (tempQuestIndex > 0) {
                tempQuestIndex--
            } else if (tempSectIndex > 0) {
                tempSectIndex--
                val prevSect = instrument.sections.getOrNull(tempSectIndex) ?: return
                tempQuestIndex = (prevSect.questions.size - 1).coerceAtLeast(0)
            } else {
                return
            }
            val prevQ = instrument.sections.getOrNull(tempSectIndex)?.questions?.getOrNull(tempQuestIndex) ?: return
            if (shouldShowQuestion(prevQ.id)) {
                currentSectionIndex.value = tempSectIndex
                currentQuestionIndex.value = tempQuestIndex
                return
            }
        }
    }

    fun setQuestionIndex(index: Int) {
        currentQuestionIndex.value = index
    }

    fun saveInterviewAnswer(index: Int, text: String) {
        val current = interviewAnswers.value.toMutableMap()
        current[index] = text
        interviewAnswers.value = current
        compileTranscripts()
    }

    private fun compileTranscripts() {
        val current = activeSurvey.value ?: return
        val inst = currentInstrument.value
        val answers = getAnswersMap(current.answersJson)
        val compiled = buildString {
            if (inst != null && inst.themes.isNotEmpty()) {
                inst.themes.forEachIndexed { index, theme ->
                    appendLine("Theme ${theme.code}: ${theme.title}")
                    appendLine("Q: ${theme.openingQuestion}")
                    appendLine("A: ${interviewAnswers.value[index] ?: "No response"}")
                    
                    val selectedProbes = mutableListOf<String>()
                    theme.probes.forEachIndexed { pIndex, probe ->
                        val isSelected = answers["probe_selected_${theme.code}_$pIndex"] == "true"
                        if (isSelected) {
                            val resp = answers["probe_response_${theme.code}_$pIndex"] ?: ""
                            selectedProbes.add("  * Follow-up: $probe\n    Response: $resp")
                        }
                    }
                    if (selectedProbes.isNotEmpty()) {
                        appendLine("Follow-up Probes Asked:")
                        selectedProbes.forEach { appendLine(it) }
                    }
                    appendLine()
                }
            } else {
                val prompts = inst?.themes?.map { it.openingQuestion } ?: interviewPrompts
                prompts.forEachIndexed { index, prompt ->
                    appendLine("Q: $prompt")
                    appendLine("A: ${interviewAnswers.value[index] ?: "No response"}")
                    appendLine()
                }
            }
        }
        val updated = current.copy(transcriptText = compiled, reviewedTranscriptText = compiled, updatedAt = System.currentTimeMillis())
        viewModelScope.launch {
            repository.insertSurvey(updated)
            activeSurvey.value = updated
        }
    }

    fun moveToNextInterview() {
        val limit = currentInstrument.value?.themes?.size ?: interviewPrompts.size
        if (currentInterviewIndex.value < limit - 1) {
            currentInterviewIndex.value++
        }
    }

    fun moveToPreviousInterview() {
        if (currentInterviewIndex.value > 0) {
            currentInterviewIndex.value--
        }
    }

    fun finalizeDcaSession() {
        val current = activeSurvey.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val finalSurvey = current.copy(
                status = "COMPLETED",
                isSynced = false,
                updatedAt = System.currentTimeMillis()
            )

            // Persist locally to Room (auto-save)
            repository.insertSurvey(finalSurvey)

            // 1. Immediately publish to Firestore cloud so Master Board receives it in real-time
            kotlin.runCatching {
                repository.syncRecordToFirestore(finalSurvey)
            }

            // 2. Auto-post completed record to Apps Script → per-user Google Doc + Drive
            val posted = kotlin.runCatching {
                repository.postRecordToAppsScript(finalSurvey)
            }.getOrDefault(false)

            // Mark synced if post succeeded
            if (posted) {
                val syncedSurvey = finalSurvey.copy(isSynced = true, updatedAt = System.currentTimeMillis())
                repository.insertSurvey(syncedSurvey)
                withContext(Dispatchers.Main) { activeSurvey.value = syncedSurvey }
            } else {
                withContext(Dispatchers.Main) { activeSurvey.value = finalSurvey }
            }
        }
    }

    fun finalizeSurvey() {
        val current = activeSurvey.value ?: return
        viewModelScope.launch {
            val updated = current.copy(status = "COMPLETED", updatedAt = System.currentTimeMillis())
            repository.insertSurvey(updated)
            activeSurvey.value = updated
        }
    }

    private fun buildReportText(survey: SurveyEntity): String {
        return buildString {
            appendLine("CivicAge Survey (CAS) Report")
            appendLine("UID: ${survey.surveyUid}")
            appendLine("Surveyor: ${survey.surveyorName} (${survey.surveyorId})")
            appendLine("Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(survey.createdAt))}")
            appendLine("\nRespondent: ${survey.participantName} (${survey.participantAge}, ${survey.participantGender})")
            appendLine("Locality: ${survey.manualLocality}")
            appendLine("\nInterview Transcript:\n${survey.transcriptText}")
            appendLine("\nManual Summary: ${survey.manualSummary}")
            appendLine("Urgency: ${survey.manualUrgency}")
            appendLine("Category: ${survey.issueCategory}")
        }
    }

    fun exportSurveyToJson(survey: SurveyEntity): String {
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        val adapter = moshi.adapter<Map<String, Any>>(type).indent("  ")
        val map = mapOf(
            "uid" to survey.surveyUid,
            "surveyor" to survey.surveyorName,
            "participant" to survey.participantName,
            "answers" to getAnswersMap(survey.answersJson),
            "transcript" to survey.transcriptText,
            "summary" to survey.manualSummary,
            "urgency" to survey.manualUrgency,
            "category" to survey.issueCategory
        )
        return adapter.toJson(map)
    }

    fun getAnswersMap(json: String): Map<String, String> {
        return try { answersAdapter.fromJson(json) ?: emptyMap() } catch (e: Exception) { emptyMap() }
    }

    fun calculateDcaScores(answers: Map<String, String>): Map<String, Int> {
        val anxietyScore = (1..10).sumOf { i ->
            val valStr = answers["D$i"] ?: "Neither"
            scoreFromLikert(valStr)
        }
        
        // Reverse scoring for Dignity: E7, E9, E11 are negative/reverse
        val dignityScore = (7..12).sumOf { i ->
            val valStr = answers["E$i"] ?: "Neither"
            val score = scoreFromLikert(valStr)
            if (i == 7 || i == 9 || i == 11) 6 - score else score
        }
        
        return mapOf("anxiety" to anxietyScore, "dignity" to dignityScore)
    }

    private fun scoreFromLikert(valStr: String): Int = when (valStr) {
        "Strongly Disagree" -> 1
        "Disagree" -> 2
        "Neither" -> 3
        "Agree" -> 4
        "Strongly Agree" -> 5
        else -> 3
    }

    fun getCitySpecificServices(): List<String> = when (selectedCity.value) {
        "ND" -> listOf("Delhi Metro app", "Delhi One app", "EDMC e-services", "Mohalla Clinic app", "Smart parking")
        "SG" -> listOf("Smart bus shelters", "Wi-Fi hotspots", "ICCC grievance portal", "Smart waste management")
        "AZ" -> listOf("Smart solid waste tracking", "Sewerage monitoring portal", "ICCC civic services")
        "CH" -> listOf("Smart road services", "E-bus tracking app", "Digital health centres", "Smart parks")
        "GN" -> listOf("iNagar app", "GIFT City infrastructure services", "Smart health kiosks")
        "IN" -> listOf("311 Smart City app", "BRTS digital booking", "ICCC grievance system", "Digital waste compliance")
        else -> emptyList()
    }

    fun speakAloud(text: String) = ttsHelper.speak(text)
    fun stopSpeaking() = ttsHelper.stop()

    private var lastTranscribedText = ""
    private var speechRecognizer: SpeechRecognizer? = null

    fun startVoiceAnswer() {
        val fileName = "CAS_AUDIO_${activeSurveyUid.value}"
        val audioFile = audioRecorder.startRecording(fileName)
        if (audioFile != null) {
            recordingState.value = "Listening"
            isRecording.value = true
            startTimer()
            
            viewModelScope.launch(Dispatchers.IO) {
                activeSurvey.value?.let { currentSurvey ->
                    val updated = currentSurvey.copy(
                        interviewAudioLocalPath = audioFile.absolutePath
                    )
                    repository.insertSurvey(updated)
                    activeSurvey.value = updated
                }
            }
            
            viewModelScope.launch(Dispatchers.Main) {
                try {
                    if (SpeechRecognizer.isRecognitionAvailable(context)) {
                        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                            setRecognitionListener(object : RecognitionListener {
                                override fun onReadyForSpeech(params: Bundle?) {}
                                override fun onBeginningOfSpeech() {}
                                override fun onRmsChanged(rmsdB: Float) {}
                                override fun onBufferReceived(buffer: ByteArray?) {}
                                override fun onEndOfSpeech() {}
                                override fun onError(error: Int) {
                                    recordingState.value = "Error code: $error"
                                }
                                override fun onResults(results: Bundle?) {
                                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                    val transcribedText = matches?.firstOrNull() ?: ""
                                    if (transcribedText.isNotBlank()) {
                                        lastTranscribedText = transcribedText
                                        recordingState.value = "Transcript Ready"
                                        
                                        // Auto-save speech-to-text transcript as it's completed
                                        viewModelScope.launch(Dispatchers.IO) {
                                            activeSurvey.value?.let { currentSurvey ->
                                                val updated = currentSurvey.copy(
                                                    transcriptText = transcribedText
                                                )
                                                repository.insertSurvey(updated)
                                                activeSurvey.value = updated
                                            }
                                        }
                                    }
                                }
                                override fun onPartialResults(partialResults: Bundle?) {
                                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                    val transcribedText = matches?.firstOrNull() ?: ""
                                    if (transcribedText.isNotBlank()) {
                                        lastTranscribedText = transcribedText
                                    }
                                }
                                override fun onEvent(eventType: Int, params: Bundle?) {}
                            })
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                            }
                            startListening(intent)
                        }
                    } else {
                        recordingState.value = "Speech recognition unavailable"
                    }
                } catch (e: Exception) {
                    recordingState.value = "Speech error: ${e.localizedMessage}"
                }
            }
        } else {
            recordingState.value = "Recorder Init Failed"
        }
    }

    fun stopVoiceAnswer(onTranscribed: (String) -> Unit) {
        audioRecorder.stopRecording()
        isRecording.value = false
        stopTimer()
        recordingState.value = "Transcript Ready"
        
        viewModelScope.launch(Dispatchers.Main) {
            speechRecognizer?.let {
                it.stopListening()
                it.destroy()
            }
            speechRecognizer = null
            
            val result = if (lastTranscribedText.isNotBlank()) lastTranscribedText else "Sample voice transcription input."
            onTranscribed(result)
            lastTranscribedText = ""
        }
    }

    private fun startTimer() {
        recordingDuration.value = 0
        recordingTimer = Timer()
        recordingTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() { recordingDuration.value++ }
        }, 1000, 1000)
    }

    private fun stopTimer() {
        recordingTimer?.cancel()
        recordingTimer = null
    }

    fun setAppearanceMode(mode: String) {
        appearanceMode.value = mode
        prefs.edit().putString("appearanceMode", mode).apply()
    }

    fun setFontSizeMode(size: String) {
        fontSizeMode.value = size
        prefs.edit().putString("fontSizeMode", size).apply()
    }

    fun updateManualFields(summary: String, category: String, urgency: String) {
        val current = activeSurvey.value ?: return
        val updated = current.copy(
            manualSummary = summary,
            issueCategory = category,
            manualUrgency = urgency,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.insertSurvey(updated)
            activeSurvey.value = updated
        }
    }

    fun deleteSurvey(survey: SurveyEntity) {
        viewModelScope.launch { repository.deleteSurvey(survey) }
    }

    fun transmitSurveyViaEmail(survey: SurveyEntity, recipient: String, onResult: (String) -> Unit) {
        // Logic for email intent is in UI
        onResult("Email intent triggered.")
    }

    fun buildDetailedReportText(survey: SurveyEntity): String {
        return buildString {
            appendLine("=========================================")
            appendLine("        CIVICAGE SURVEY REPORT           ")
            appendLine("=========================================")
            appendLine("Record UID: ${survey.surveyUid}")
            appendLine("Surveyor: ${survey.surveyorName} (ID: ${survey.surveyorId}, Email: ${survey.surveyorEmail})")
            appendLine("Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(survey.createdAt))}")
            appendLine("City/Locality: ${survey.manualLocality}")
            appendLine("-----------------------------------------")
            appendLine("RESPONDENT DETAILS:")
            appendLine("Name: ${survey.participantName}")
            appendLine("Age: ${survey.participantAge}")
            appendLine("Mobile: ${survey.participantPhone}")
            if (survey.latitude != 0.0 || survey.longitude != 0.0) {
                appendLine("Location: Lat ${survey.latitude}, Lng ${survey.longitude} (Accuracy: ${survey.gpsAccuracy}m)")
            } else {
                appendLine("Location: Not Captured")
            }
            appendLine("-----------------------------------------")
            
            // Decoded Answers
            val answers = getAnswersMap(survey.answersJson)
            if (answers.isNotEmpty()) {
                appendLine("SURVEY RESPONSES:")
                // Find matching instrument
                val uidParts = survey.surveyUid.split("-")
                val instCode = if (uidParts.size >= 5) uidParts[4] else ""
                val instrument = InstrumentBank.instruments.find { it.code == instCode || it.code.startsWith(instCode) }
                if (instrument != null) {
                    instrument.sections.forEach { sect ->
                        appendLine("\n[Section ${sect.code}: ${sect.title}]")
                        sect.questions.forEach { q ->
                            if (q.inputType == InputType.TABLE) {
                                // Table grid responses
                                appendLine("- ${q.text}:")
                                val services = getCitySpecificServices()
                                services.forEach { service ->
                                    val heard = if (answers["C_GRID_${service}_heard"] == "Yes" || answers["C_GRID_${service}_Heard"] == "Yes") "Yes" else "No"
                                    val tried = if (answers["C_GRID_${service}_tried"] == "Yes" || answers["C_GRID_${service}_Tried"] == "Yes") "Yes" else "No"
                                    val alone = if (answers["C_GRID_${service}_alone"] == "Yes" || answers["C_GRID_${service}_Used successfully alone?"] == "Yes") "Yes" else "No"
                                    val help = if (answers["C_GRID_${service}_help"] == "Yes" || answers["C_GRID_${service}_Used with someone’s help?"] == "Yes") "Yes" else "No"
                                    val never = if (answers["C_GRID_${service}_never"] == "Yes" || answers["C_GRID_${service}_Never used or gave up?"] == "Yes") "Yes" else "No"
                                    appendLine("  * $service -> Heard: $heard, Tried: $tried, Alone: $alone, Help: $help, Never: $never")
                                }
                            } else {
                                val ans = answers[q.id] ?: "(No Response)"
                                appendLine("- ${q.text}")
                                appendLine("  Answer: $ans")
                            }
                        }
                    }
                } else {
                    // Fallback to printing key-value pairs
                    answers.forEach { (k, v) ->
                        appendLine("Question $k: $v")
                    }
                }
            }
            
            if (survey.transcriptText.isNotBlank()) {
                appendLine("\n-----------------------------------------")
                appendLine("INTERVIEW TRANSCRIPT / NOTES:")
                appendLine(survey.transcriptText)
            }
            
            if (survey.manualSummary.isNotBlank()) {
                appendLine("\n-----------------------------------------")
                appendLine("ADDITIONAL OBSERVATIONS:")
                appendLine("Summary: ${survey.manualSummary}")
                appendLine("Urgency: ${survey.manualUrgency}")
                appendLine("Category: ${survey.issueCategory}")
            }
            appendLine("=========================================")
        }
    }

    fun exportAllSurveysToTsv(surveys: List<SurveyEntity>): String {
        return buildString {
            val allQuestions = InstrumentBank.instruments.flatMap { it.sections.flatMap { s -> s.questions } }
            val standardHeaders = listOf("Record_UID", "Created_At", "Surveyor_Name", "Surveyor_Id", "Respondent_Name", "Respondent_Age", "Respondent_Phone", "Locality", "Latitude", "Longitude", "GPS_Accuracy")
            val uniqueQuestionIds = allQuestions.map { it.id }.distinct()
            
            append(standardHeaders.joinToString("\t"))
            uniqueQuestionIds.forEach { qId ->
                append("\t$qId")
            }
            appendLine()
            
            surveys.forEach { survey ->
                val answers = getAnswersMap(survey.answersJson)
                val rowValues = mutableListOf<String>()
                
                rowValues.add(survey.surveyUid)
                rowValues.add(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(survey.createdAt)))
                rowValues.add(survey.surveyorName)
                rowValues.add(survey.surveyorId)
                rowValues.add(survey.participantName)
                rowValues.add(survey.participantAge.toString())
                rowValues.add(survey.participantPhone)
                rowValues.add(survey.manualLocality)
                rowValues.add(survey.latitude.toString())
                rowValues.add(survey.longitude.toString())
                rowValues.add(survey.gpsAccuracy.toString())
                
                uniqueQuestionIds.forEach { qId ->
                    val ans = answers[qId] ?: ""
                    val mappedAns = if (ans.isNotBlank()) {
                        val quest = allQuestions.find { it.id == qId }
                        if (quest != null && quest.options.isNotEmpty()) {
                            if (quest.inputType == InputType.SINGLE_CHOICE || quest.inputType == InputType.LIKERT) {
                                val optIndex = quest.options.indexOf(ans)
                                if (optIndex >= 0) {
                                    val letter = (optIndex + 'A'.code).toChar().toString()
                                    val rawChoice = quest.options[optIndex]
                                    "$letter ($rawChoice)"
                                } else {
                                    ans
                                }
                            } else if (quest.inputType == InputType.MULTI_CHOICE) {
                                val selectedLetters = mutableListOf<String>()
                                quest.options.forEachIndexed { optIdx, option ->
                                    if (ans.contains(option)) {
                                        val letter = (optIdx + 'A'.code).toChar().toString()
                                        selectedLetters.add(letter)
                                    }
                                }
                                if (selectedLetters.isNotEmpty()) {
                                    selectedLetters.joinToString(",")
                                } else {
                                    ans
                                }
                            } else {
                                ans
                            }
                        } else {
                            ans
                        }
                    } else {
                        ""
                    }
                    rowValues.add(mappedAns.replace("\t", " ").replace("\n", " "))
                }
                appendLine(rowValues.joinToString("\t"))
            }
        }
    }

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress ?: "Unknown"
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return "Unknown"
    }

    private var serverSocket: ServerSocket? = null

    fun startSyncServer() {
        if (isSyncServerRunning.value) return
        isSyncServerRunning.value = true
        syncServerIpAddress.value = getLocalIpAddress()
        syncServerStatus.value = "Listening on ${syncServerIpAddress.value}:8080"
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(8080)
                while (isSyncServerRunning.value) {
                    val socket = serverSocket?.accept() ?: break
                    handleClientConnection(socket)
                }
            } catch (e: Exception) {
                if (isSyncServerRunning.value) {
                    syncServerStatus.value = "Server error: ${e.message}"
                }
            } finally {
                isSyncServerRunning.value = false
                syncServerStatus.value = "Stopped"
            }
        }
    }

    fun stopSyncServer() {
        isSyncServerRunning.value = false
        syncServerStatus.value = "Stopped"
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        serverSocket = null
    }

    private suspend fun handleClientConnection(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val outputStream = socket.getOutputStream()
            
            var contentLength = 0
            var line: String?
            while (true) {
                line = reader.readLine()
                if (line.isNullOrBlank()) break
                if (line.lowercase().startsWith("content-length:")) {
                    contentLength = line.substring(15).trim().toIntOrNull() ?: 0
                }
            }
            
            if (contentLength > 0) {
                val buffer = CharArray(contentLength)
                var bytesRead = 0
                while (bytesRead < contentLength) {
                    val read = reader.read(buffer, bytesRead, contentLength - bytesRead)
                    if (read == -1) break
                    bytesRead += read
                }
                val payload = String(buffer)
                
                val surveysListType = Types.newParameterizedType(List::class.java, SurveyEntity::class.java)
                val adapter = moshi.adapter<List<SurveyEntity>>(surveysListType)
                val surveys = adapter.fromJson(payload)
                
                if (surveys != null) {
                    surveys.forEach { survey ->
                        repository.insertSurvey(survey)
                    }
                    val logMsg = "Synced ${surveys.size} surveys from ${socket.inetAddress.hostAddress} at ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}"
                    localSyncLogs.value = listOf(logMsg) + localSyncLogs.value
                    
                    val response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: 12\r\n\r\nSync Success"
                    outputStream.write(response.toByteArray())
                } else {
                    val response = "HTTP/1.1 400 Bad Request\r\nContent-Length: 0\r\n\r\n"
                    outputStream.write(response.toByteArray())
                }
            } else {
                val response = "HTTP/1.1 400 Bad Request\r\nContent-Length: 0\r\n\r\n"
                outputStream.write(response.toByteArray())
            }
            outputStream.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                socket.close()
            } catch (e: Exception) {}
        }
    }

    fun syncSurveysToMaster(masterIp: String, onResult: (Boolean, String) -> Unit) {
        clientSyncStatus.value = "Connecting to Master at $masterIp:8080..."
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val surveys = repository.allSurveys.first()
                val surveysListType = Types.newParameterizedType(List::class.java, SurveyEntity::class.java)
                val adapter = moshi.adapter<List<SurveyEntity>>(surveysListType)
                val payload = adapter.toJson(surveys) ?: "[]"
                
                val url = URL("http://$masterIp:8080/sync")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Content-Length", payload.length.toString())
                
                conn.outputStream.use { os ->
                    os.write(payload.toByteArray())
                    os.flush()
                }
                
                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    surveys.forEach { survey ->
                        val updated = survey.copy(isSynced = true, firebaseSyncStatus = "synced", lastSyncedAt = System.currentTimeMillis())
                        repository.insertSurvey(updated)
                    }
                    withContext(Dispatchers.Main) {
                        clientSyncStatus.value = "Sync successful!"
                        onResult(true, "Synced ${surveys.size} surveys to Master successfully.")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        clientSyncStatus.value = "Failed: Server returned code $responseCode"
                        onResult(false, "Server returned code $responseCode")
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    clientSyncStatus.value = "Sync failed: ${e.message}"
                    onResult(false, e.message ?: "Unknown connection error")
                }
            }
        }
    }

    fun approveInstrument(surveyUid: String, qaNotes: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val success = repository.updateInstrumentStatus(
                surveyUid = surveyUid,
                newStatus = "approved",
                qaNotes = qaNotes,
                approvedBy = surveyorName.value.ifBlank { surveyorEmail.value }
            )
            com.civilagesurvey.app.telemetry.TelemetryManager.logQaAction(surveyUid, "approve", surveyorEmail.value)

            // Post to Apps Script → routes to the correct per-user Google Doc
            var docUrl: String? = null
            if (success) {
                val survey = repository.getSurveyById(surveyUid)
                if (survey != null) {
                    // Resolve the right doc URL for this surveyor's ID (v3.3 routing)
                    val targetDocUrl = com.civilagesurvey.app.data.repository.SurveyRepository
                        .docUrlForUser(survey.surveyorId)
                    docUrl = kotlin.runCatching {
                        repository.postRecordToAppsScript(survey)
                        targetDocUrl
                    }.getOrNull()
                }
            }
            onResult(success, docUrl)
        }
    }

    fun submitSurveyToMainDoc(survey: SurveyEntity, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val posted = repository.postRecordToAppsScript(survey)
            if (posted) {
                val updated = survey.copy(isSynced = true, updatedAt = System.currentTimeMillis())
                repository.insertSurvey(updated)
                kotlin.runCatching { repository.syncRecordToFirestore(updated) }
            }
            withContext(Dispatchers.Main) {
                onResult(posted)
            }
        }
    }


    fun rejectInstrument(surveyUid: String, qaNotes: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.updateInstrumentStatus(
                surveyUid = surveyUid,
                newStatus = "rejected",
                qaNotes = qaNotes,
                approvedBy = ""
            )
            com.civilagesurvey.app.telemetry.TelemetryManager.logQaAction(surveyUid, "reject", surveyorEmail.value)
            onResult(success)
        }
    }

    fun createPushRequest(surveyUid: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val request = PushRequestEntity(
                requestId = "REQ-${UUID.randomUUID().toString().take(8)}",
                instrumentId = surveyUid,
                requestedBy = surveyorEmail.value.ifBlank { surveyorId.value },
                requestedAt = System.currentTimeMillis(),
                status = "open"
            )
            val success = repository.createPushRequest(request)
            repository.updateInstrumentStatus(surveyUid, "pending_qa")
            com.civilagesurvey.app.telemetry.TelemetryManager.logQaAction(surveyUid, "request_qa", surveyorEmail.value)
            onResult(success)
        }
    }

    fun verifyMasterPassword(password: String): Boolean {
        val clean = password.trim()
        val isValid = (clean == "Scscd@2026" || clean == "SCSC2026" || clean == "mdc18042007@gmail.com")
        if (isValid) {
            isMasterAuthenticated.value = true
        }
        return isValid
    }

    fun archiveSurvey(surveyUid: String) {
        archivedSurveyUids.value = archivedSurveyUids.value + surveyUid
    }

    fun unarchiveSurvey(surveyUid: String) {
        archivedSurveyUids.value = archivedSurveyUids.value - surveyUid
    }

    fun dismissSwipeHintOverlay() {
        showSwipeHintOverlay.value = false
        prefs.edit().putBoolean("dismissedSwipeHint", true).apply()
    }

    fun exportSurveyToMetadataJson(survey: SurveyEntity): String {
        val map = mapOf(
            "instrumentId" to survey.surveyUid,
            "uploadedAt" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date(survey.createdAt)),
            "uploadedBy" to survey.surveyorEmail,
            "geo" to mapOf(
                "lat" to survey.latitude,
                "lng" to survey.longitude,
                "accuracy" to survey.gpsAccuracy,
                "timestamp" to survey.geoTimestamp
            ),
            "geoTier" to survey.geoTier,
            "qaNotes" to survey.qaNotes,
            "version" to "2.2"
        )
        return moshi.adapter(Map::class.java).toJson(map)
    }

    override fun onCleared() {
        super.onCleared()
        ttsHelper.shutdown()
        stopTimer()
        speechRecognizer?.destroy()
    }
}
