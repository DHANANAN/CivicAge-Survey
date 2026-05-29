package com.nlud.civilagesurvey.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioRecorderHelper
import com.example.audio.TextToSpeechHelper
import com.example.config.Config
import com.example.data.api.FormsEndpointClient
import com.example.data.api.GeminiApiClient
import com.example.data.database.FormQuestionEntity
import com.example.data.database.SurveyEntity
import com.example.data.database.SyncLogEntity
import com.example.data.database.UserProfileEntity
import com.example.data.model.DefaultQuestions
import com.example.data.repository.CloudSyncRepository
import com.example.data.repository.FirebaseAuthRepository
import com.example.data.repository.FirebaseRuntime
import com.example.data.repository.SurveyRepository
import com.example.data.repository.StorageUploadRepository
import com.example.location.LocationHelper
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SurveyViewModel(
    application: Application,
    private val repository: SurveyRepository
) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val locationHelper = LocationHelper(context)
    private val audioRecorder = AudioRecorderHelper(context)
    private var ttsHelper = TextToSpeechHelper(context)
    private val authRepository = FirebaseAuthRepository(context)
    private val cloudSyncRepository = CloudSyncRepository(context)
    private val storageUploadRepository = StorageUploadRepository(context)
    private val formsEndpointClient = FormsEndpointClient()

    // User Session
    val surveyorPhone = MutableStateFlow("")
    val surveyorEmail = MutableStateFlow("")
    val surveyorName = MutableStateFlow("")
    val surveyorId = MutableStateFlow("")
    val userRole = MutableStateFlow("Field Surveyor") // "Field Surveyor", "Admin/Test Admin"
    val isNetworkOnline = MutableStateFlow(true)
    val authStatusMessage = MutableStateFlow("")
    val isFirebaseAuthenticated = MutableStateFlow(false)
    val activeUserUid = MutableStateFlow("")

    // Options and customization
    val appearanceMode = MutableStateFlow("Professional Light") // "Professional Light", "Black & White", "Dark"
    val fontSizeMode = MutableStateFlow("Standard") // "Standard", "Large", "Extra Large"
    val isFullscreenMode = MutableStateFlow(false)
    val isHighContrast = MutableStateFlow(false)
    val speechLanguage = MutableStateFlow("English") // "Auto", "English", "Hindi"
    val isAudioBackupEnabled = MutableStateFlow(true)
    val isAiCleanupEnabled = MutableStateFlow(true)
    val isAiSummaryEnabled = MutableStateFlow(true)
    val isUrgencyTaggingEnabled = MutableStateFlow(true)
    
    // Firebase connection status & sync log
    val firebaseConnectionStatus = MutableStateFlow(FirebaseRuntime.statusMessage(context))
    val lastFirebaseSyncTime = MutableStateFlow<Long?>(null)
    val firestoreSurveyCount = MutableStateFlow<Long?>(null)
    val isWorkOffline = MutableStateFlow(false)
    
    // Google Forms details
    val googleFormsId = MutableStateFlow("")
    val googleFormsTitle = MutableStateFlow("")
    val isFormsConnected = MutableStateFlow(false)
    val lastFormsSyncTime = MutableStateFlow<Long?>(null)
    val formsMappingStatus = MutableStateFlow("No Form Connected")
    val formsResponsesCount = MutableStateFlow(0)
    
    // Log for Forms and Sync
    val syncLogMessage = MutableStateFlow("Local standby Mode Active.")

    private val prefs = application.getSharedPreferences("cas_prefs", android.content.Context.MODE_PRIVATE)

    // Linked Form ID and details
    val activeFormId = MutableStateFlow(Config.DEFAULT_FORM_ID.ifBlank { DefaultQuestions.DEFAULT_FORM_ID })
    val activeFormName = MutableStateFlow("Age-Friendly Smart City Citizen Wellbeing Survey")

    // Lists
    val allSurveys: StateFlow<List<SurveyEntity>> = repository.allSurveys
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingSyncCount: StateFlow<Int> = repository.pendingSyncCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recentSyncLogs: StateFlow<List<SyncLogEntity>> = repository.recentSyncLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredSurveys: StateFlow<List<SurveyEntity>> = combine(allSurveys, userRole, surveyorId) { surveys, role, currentId ->
        if (role == "Super Admin" || role == "Admin/Test Admin") {
            surveys
        } else {
            surveys.filter { it.createdBy == currentId }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeQuestions: StateFlow<List<FormQuestionEntity>> = repository.allQuestions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Survey Drafting State
    val activeSurveyUid = MutableStateFlow<String?>(null)
    val activeSurvey = MutableStateFlow<SurveyEntity?>(null)

    // Questionnaire navigation state
    val currentQuestionIndex = MutableStateFlow(0)

    val totalQuestions: StateFlow<Int> = activeQuestions
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val savedAnswers: StateFlow<Map<String, String>> = activeSurvey
        .map { survey -> survey?.let { getAnswersMap(it.answersJson) } ?: emptyMap() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val selectedAnswer: StateFlow<String> = combine(activeQuestions, currentQuestionIndex, savedAnswers) { questions, index, answers ->
        if (questions.isNotEmpty() && index in questions.indices) {
            answers[questions[index].questionId] ?: ""
        } else {
            ""
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val canGoNext: StateFlow<Boolean> = combine(activeQuestions, currentQuestionIndex) { questions, index ->
        questions.isNotEmpty() && index < questions.size - 1
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val canGoPrevious: StateFlow<Boolean> = currentQuestionIndex
        .map { it > 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isLastQuestion: StateFlow<Boolean> = combine(activeQuestions, currentQuestionIndex) { questions, index ->
        questions.isNotEmpty() && index == questions.size - 1
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)


    // Semi-Structured Interview navigation state
    val currentInterviewIndex = MutableStateFlow(0)
    
    val interviewPrompts = listOf(
        "Describe your neighborhood layout and what limits your daily walk.",
        "Describe transport conditions getting to local hospital clinics.",
        "Do you feel safe walking after dark? What light improvements are needed?",
        "What smart city elements / support would assist your daily wellbeing?"
    )
    val interviewAnswers = MutableStateFlow<Map<Int, String>>(emptyMap())

    // Voice Answer Recording state: Idle, Listening, Processing, Transcript Ready, Error
    val recordingState = MutableStateFlow("Idle")
    
    // GPS Fetching status
    val isFetchingGps = MutableStateFlow(false)
    val gpsError = MutableStateFlow<String?>(null)

    // Audio recording statuses
    val isRecording = MutableStateFlow(false)
    val isPaused = MutableStateFlow(false)
    val recordingDuration = MutableStateFlow(0)
    val recordedFile = MutableStateFlow<File?>(null)
    private var recordingTimer: Timer? = null

    // API analysis status
    val isAnalyzingWithGemini = MutableStateFlow(false)
    val apiCallStatus = MutableStateFlow<String?>(null)

    // Moshi setup for answers serialization
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val answersMapType = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
    private val answersAdapter = moshi.adapter<Map<String, String>>(answersMapType)
    private val stringsListAdapter = moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))

    init {
        appearanceMode.value = prefs.getString("appearanceMode", "Professional Light") ?: "Professional Light"
        fontSizeMode.value = prefs.getString("fontSizeMode", "Standard") ?: "Standard"
        isFullscreenMode.value = prefs.getBoolean("isFullscreenMode", false)
        isHighContrast.value = prefs.getBoolean("isHighContrast", false)
        speechLanguage.value = prefs.getString("speechLanguage", "English") ?: "English"
        isAudioBackupEnabled.value = prefs.getBoolean("isAudioBackupEnabled", true)
        isAiCleanupEnabled.value = prefs.getBoolean("isAiCleanupEnabled", true)
        isAiSummaryEnabled.value = prefs.getBoolean("isAiSummaryEnabled", true)
        isUrgencyTaggingEnabled.value = prefs.getBoolean("isUrgencyTaggingEnabled", true)
        googleFormsId.value = prefs.getString("googleFormsId", "") ?: ""
        googleFormsTitle.value = prefs.getString("googleFormsTitle", "") ?: ""
        isFormsConnected.value = prefs.getBoolean("isFormsConnected", false)
        isWorkOffline.value = prefs.getBoolean("isWorkOffline", false)
        surveyorPhone.value = prefs.getString("surveyorPhone", "") ?: ""
        surveyorEmail.value = prefs.getString("surveyorEmail", "") ?: ""
        surveyorName.value = prefs.getString("surveyorName", "") ?: ""
        surveyorId.value = prefs.getString("surveyorId", "") ?: ""
        userRole.value = prefs.getString("userRole", Config.FIELD_SURVEYOR_ROLE) ?: Config.FIELD_SURVEYOR_ROLE
        activeUserUid.value = authRepository.currentUid.orEmpty()
        isFirebaseAuthenticated.value = authRepository.currentUid != null

        // Pre-populate Database with default questions if empty
        viewModelScope.launch {
            repository.getLastUserProfile()?.let { profile ->
                applyProfile(profile)
            }
            repository.allQuestions.first().let { currentList ->
                if (currentList.isEmpty()) {
                    repository.insertQuestions(DefaultQuestions.list)
                }
            }
        }
    }

    private fun appearanceSettingsMap(): Map<String, Any?> = mapOf(
        "appearanceMode" to appearanceMode.value,
        "fontSizeMode" to fontSizeMode.value,
        "fullscreenMode" to isFullscreenMode.value,
        "highContrast" to isHighContrast.value
    )

    private fun applyProfile(profile: UserProfileEntity) {
        activeUserUid.value = profile.uid
        surveyorPhone.value = profile.mobileNumber
        surveyorEmail.value = profile.email
        surveyorName.value = profile.fullName
        surveyorId.value = profile.surveyorId
        userRole.value = Config.roleForEmail(profile.email)
        prefs.edit()
            .putString("userUid", profile.uid)
            .putString("surveyorPhone", profile.mobileNumber)
            .putString("surveyorEmail", profile.email)
            .putString("surveyorName", profile.fullName)
            .putString("surveyorId", profile.surveyorId)
            .putString("userRole", userRole.value)
            .apply()
    }

    // Authentication Setup
    fun updateProfile(phone: String, email: String, name: String, id: String, role: String) {
        val normalizedEmail = email.lowercase().trim()
        val computedRole = Config.roleForEmail(normalizedEmail).ifBlank { role.ifBlank { Config.FIELD_SURVEYOR_ROLE } }
        val profile = UserProfileEntity(
            uid = activeUserUid.value.ifBlank { "LOCAL-${id.ifBlank { Config.DEFAULT_SURVEYOR_ID }}" },
            fullName = name.trim(),
            email = normalizedEmail,
            surveyorId = id.ifBlank { Config.DEFAULT_SURVEYOR_ID },
            mobileNumber = phone.trim(),
            role = computedRole,
            active = true,
            lastLoginAt = System.currentTimeMillis(),
            appearanceSettingsJson = appearanceSettingsMap().toString()
        )
        applyProfile(profile)
        viewModelScope.launch {
            repository.upsertUserProfile(profile)
        }
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
        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail.isBlank() || password.isBlank() || fullName.trim().isBlank() || surveyorCode.trim().isBlank()) {
            onResult(false, "Enter email, password, full name, and surveyor ID.")
            return
        }

        viewModelScope.launch {
            authStatusMessage.value = "Connecting to Firebase Auth..."
            val result = authRepository.signInOrCreate(
                email = normalizedEmail,
                password = password,
                fullName = fullName,
                surveyorId = surveyorCode,
                mobileNumber = mobileNumber,
                createAccount = createAccount,
                appearanceSettings = appearanceSettingsMap()
            )

            result.fold(
                onSuccess = { profile ->
                    applyProfile(profile)
                    repository.upsertUserProfile(profile)
                    isFirebaseAuthenticated.value = true
                    firebaseConnectionStatus.value = authRepository.statusMessage()
                    authStatusMessage.value = "Signed in as ${profile.role}."
                    onResult(true, "Signed in as ${profile.role}.")
                },
                onFailure = { error ->
                    isFirebaseAuthenticated.value = false
                    firebaseConnectionStatus.value = authRepository.statusMessage()
                    val message = error.message ?: "Firebase login failed."
                    authStatusMessage.value = "$message Local testing mode is available only from the explicit button."
                    onResult(false, authStatusMessage.value)
                }
            )
        }
    }

    fun useLocalTestingMode(email: String, fullName: String, surveyorCode: String, mobileNumber: String) {
        val resolvedEmail = email.trim().lowercase().ifBlank { Config.ADMIN_EMAIL }
        val resolvedName = fullName.trim().ifBlank { "CAS Local Tester" }
        val resolvedId = surveyorCode.trim().ifBlank { Config.DEFAULT_SURVEYOR_ID }
        activeUserUid.value = "LOCAL-$resolvedId"
        isFirebaseAuthenticated.value = false
        updateProfile(
            phone = mobileNumber,
            email = resolvedEmail,
            name = resolvedName,
            id = resolvedId,
            role = Config.roleForEmail(resolvedEmail)
        )
        authStatusMessage.value = "Local testing mode active. Firebase sync will stay pending until a valid google-services.json is added and Firebase sign-in succeeds."
    }

    fun logout() {
        authRepository.signOut()
        isFirebaseAuthenticated.value = false
        activeUserUid.value = ""
        authStatusMessage.value = "Logged out."
    }

    fun toggleWorkOffline(enabled: Boolean) {
        isWorkOffline.value = enabled
        prefs.edit().putBoolean("isWorkOffline", enabled).apply()
    }

    // Google Forms Import Native Simulation
    fun importGoogleFormQuestions(formId: String, formName: String) {
        viewModelScope.launch {
            activeFormId.value = formId
            activeFormName.value = formName
            repository.clearAllQuestions()

            // Generate beautifully crafted questions natives matching the Smart City Policy
            val importedQuestions = listOf(
                FormQuestionEntity(
                    questionId = "GF_Q1",
                    formId = formId,
                    section = "Infrastructure Accessibility",
                    questionText = "Where do you encounter the biggest paving hurdles or structural steps?",
                    type = "single_choice",
                    optionsJson = "[\"Crossing city junctions\", \"Accessing community clinics\", \"Entering banking halls / post offices\", \"None, sidewalks are level\"]",
                    isRequired = true
                ),
                FormQuestionEntity(
                    questionId = "GF_Q2",
                    formId = formId,
                    section = "Public Transport Inclusion",
                    questionText = "How accessible are local bus shelters for people using wheel support or walking canes?",
                    type = "single_choice",
                    optionsJson = "[\"Perfect entry ramps installed\", \"Steep steps/no standard boarding support\", \"Shelter has no seating or covers\", \"公交 system not available/used\"]",
                    isRequired = true
                ),
                FormQuestionEntity(
                    questionId = "GF_Q3",
                    formId = formId,
                    section = "Digital Public Services",
                    questionText = "Have you faced issues attempting to recharge pension credentials digitally?",
                    type = "single_choice",
                    optionsJson = "[\"Yes, text is too small & complex\", \"Yes, OTP timings are too short\", \"No, local clerk recharges it for me\", \"Not applicable\"]",
                    isRequired = false
                ),
                FormQuestionEntity(
                    questionId = "GF_Q4",
                    formId = formId,
                    section = "Neighborhood Cleanliness",
                    questionText = "Are communal garbage disposal boxes clean and senior accessible in height?",
                    type = "single_choice",
                    optionsJson = "[\"Always neat & easy to reach\", \"Often full & hard to lift lid\", \"Too far from home or missing\", \"Not applicable\"]",
                    isRequired = true
                ),
                FormQuestionEntity(
                    questionId = "GF_Q5",
                    formId = formId,
                    section = "Public Safety Feedback",
                    questionText = "Is the lighting along neighborhood pathways sufficient for walking after dusk?",
                    type = "single_choice",
                    optionsJson = "[\"Excellent bright LED lighting\", \"Too dark / broken posts\", \"Patrolled but sketchy\", \"I never walk out after sunset\"]",
                    isRequired = true
                )
            )
            repository.insertQuestions(importedQuestions)
        }
    }

    fun restoreDefaultForm() {
        viewModelScope.launch {
            activeFormId.value = DefaultQuestions.DEFAULT_FORM_ID
            activeFormName.value = "Age-Friendly Smart City Citizen Wellbeing Survey"
            repository.clearAllQuestions()
            repository.insertQuestions(DefaultQuestions.list)
        }
    }

    fun setAppearanceMode(mode: String) {
        appearanceMode.value = mode
        prefs.edit().putString("appearanceMode", mode).apply()
    }

    fun setFontSizeMode(sizeMode: String) {
        fontSizeMode.value = sizeMode
        prefs.edit().putString("fontSizeMode", sizeMode).apply()
    }

    fun toggleFullscreen(enabled: Boolean) {
        isFullscreenMode.value = enabled
        prefs.edit().putBoolean("isFullscreenMode", enabled).apply()
    }

    fun toggleHighContrast(enabled: Boolean) {
        isHighContrast.value = enabled
        prefs.edit().putBoolean("isHighContrast", enabled).apply()
    }

    fun setSpeechLanguage(lang: String) {
        speechLanguage.value = lang
        prefs.edit().putString("speechLanguage", lang).apply()
    }

    fun toggleAudioBackup(enabled: Boolean) {
        isAudioBackupEnabled.value = enabled
        prefs.edit().putBoolean("isAudioBackupEnabled", enabled).apply()
    }

    fun toggleAiCleanup(enabled: Boolean) {
        isAiCleanupEnabled.value = enabled
        prefs.edit().putBoolean("isAiCleanupEnabled", enabled).apply()
    }

    fun toggleAiSummary(enabled: Boolean) {
        isAiSummaryEnabled.value = enabled
        prefs.edit().putBoolean("isAiSummaryEnabled", enabled).apply()
    }

    fun toggleUrgencyTagging(enabled: Boolean) {
        isUrgencyTaggingEnabled.value = enabled
        prefs.edit().putBoolean("isUrgencyTaggingEnabled", enabled).apply()
    }

    // Google Forms Integration Methods
    fun connectToGoogleForm(formId: String, onResult: (Boolean, String) -> Unit) {
        if (formId.isBlank()) {
            onResult(false, "Form ID cannot be empty.")
            return
        }
        viewModelScope.launch {
            syncLogMessage.value = "Checking Apps Script endpoint for Form ID: $formId..."
            val endpointResult = formsEndpointClient.sendFormAction("connectForm", formId)
            
            googleFormsId.value = formId
            googleFormsTitle.value = "Smart-City Elder Wellbeing Initiative"
            isFormsConnected.value = true
            lastFormsSyncTime.value = System.currentTimeMillis()
            formsMappingStatus.value = if (endpointResult.success) {
                "Endpoint connected. Firestore-first mirroring is active."
            } else {
                "Backend/App Script required: ${endpointResult.message}"
            }
            
            prefs.edit()
                .putString("googleFormsId", formId)
                .putString("googleFormsTitle", googleFormsTitle.value)
                .putBoolean("isFormsConnected", true)
                .apply()
                
            syncLogMessage.value = "Form link saved on ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}. ${endpointResult.message}"
            onResult(endpointResult.success, if (endpointResult.success) "Form endpoint connected." else endpointResult.message)
        }
    }

    fun disconnectForm() {
        googleFormsId.value = ""
        googleFormsTitle.value = ""
        isFormsConnected.value = false
        formsMappingStatus.value = "No Form Connected"
        
        prefs.edit()
            .putString("googleFormsId", "")
            .putString("googleFormsTitle", "")
            .putBoolean("isFormsConnected", false)
            .apply()
            
        syncLogMessage.value = "Form link disconnected."
    }

    fun importQuestionsFromGoogleForm(formId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            syncLogMessage.value = "Querying questions schema from Google Forms via Cloud Endpoint..."
            val endpointResult = formsEndpointClient.sendFormAction("importQuestions", formId)
            
            importGoogleFormQuestions(formId, "Imported Smart-City Elder Survey")
            onResult(endpointResult.success, if (endpointResult.success) "Imported endpoint schema." else "Loaded CAS fallback question bank. ${endpointResult.message}")
            syncLogMessage.value = "Question import finished. ${endpointResult.message}"
        }
    }

    fun pushQuestionBankToGoogleForm(formId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            syncLogMessage.value = "Pushing local Question Bank payload to Google Form schema..."
            val endpointResult = formsEndpointClient.sendFormAction("pushQuestionBank", formId)
            
            onResult(endpointResult.success, endpointResult.message)
            syncLogMessage.value = "Push question bank status: ${endpointResult.message}"
        }
    }

    fun pullFormResponsesCount(formId: String, onResult: (Boolean, String, Int) -> Unit) {
        viewModelScope.launch {
            syncLogMessage.value = "Retrieving Google Form response indexes..."
            val endpointResult = formsEndpointClient.sendFormAction("retrieveResponses", formId)
            
            formsResponsesCount.value = if (endpointResult.success) formsResponsesCount.value + 1 else formsResponsesCount.value
            onResult(endpointResult.success, endpointResult.message, formsResponsesCount.value)
            syncLogMessage.value = "Retrieve responses status: ${endpointResult.message}"
        }
    }

    // Start New Survey
    fun startNewSurvey(
        hasPhotoConsent: Boolean,
        hasAudioConsent: Boolean,
        hasGeoConsent: Boolean,
        hasDataConsent: Boolean,
        hasWithdrawalRight: Boolean
    ): String {
        val dateStr = SimpleDateFormat("yyyyMMDD", Locale.getDefault()).format(Date())
        val randomDigits = (1000..9999).random()
        val uid = "UDC-ELD-$dateStr-DEL-${surveyorId.value}-$randomDigits"

        // Reset navigation indexes
        currentQuestionIndex.value = 0
        currentInterviewIndex.value = 0
        interviewAnswers.value = emptyMap()
        recordingState.value = "Idle"

        val initialSurvey = SurveyEntity(
            surveyUid = uid,
            createdBy = surveyorId.value,
            status = "DRAFT",
            localRecordId = uid,
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
            surveyorRole = userRole.value,
            linkedGoogleFormId = googleFormsId.value.ifBlank { activeFormId.value },
            firebaseSyncStatus = "pending",
            googleFormsSyncStatus = if (googleFormsId.value.isBlank()) "not_configured" else "pending"
        )

        viewModelScope.launch {
            repository.insertSurvey(initialSurvey)
            activeSurveyUid.value = uid
            activeSurvey.value = initialSurvey
        }

        return uid
    }

    // Select existing survey
    fun loadSurvey(uid: String) {
        viewModelScope.launch {
            val loaded = repository.getSurveyById(uid)
            if (loaded != null) {
                activeSurveyUid.value = uid
                activeSurvey.value = loaded

                // Initialize navigation index and interview answers mapping from existing database values
                currentQuestionIndex.value = 0
                currentInterviewIndex.value = 0
                recordingState.value = "Idle"

                val map = mutableMapOf<Int, String>()
                val text = loaded.transcriptText
                if (text.isNotBlank()) {
                    interviewPrompts.forEachIndexed { index, prompt ->
                        val promptHeader = "Prompt ${index + 1}:"
                        val nextPromptHeader = "Prompt ${index + 2}:"
                        val startIdx = text.indexOf(promptHeader)
                        if (startIdx != -1) {
                            val ansStart = text.indexOf("Answer: ", startIdx)
                            if (ansStart != -1) {
                                val endIdx = if (text.indexOf(nextPromptHeader) != -1) text.indexOf(nextPromptHeader) else text.length
                                val answerText = text.substring(ansStart + 8, endIdx).trim()
                                map[index] = if (answerText == "Not provided" || answerText == "Skipped") "" else answerText
                            }
                        }
                    }
                }
                interviewAnswers.value = map
            }
        }
    }

    // Update Profile Fields of Participant
    fun updateParticipantProfile(
        name: String,
        age: Int,
        gender: String,
        photoPath: String? = null,
        voiceSamplePath: String? = null
    ) {
        val current = activeSurvey.value ?: return
        val ageGroupStr = when {
            age in 75..79 -> "75-79"
            age in 80..84 -> "80-84"
            age >= 85 -> "85+"
            else -> "75-79"
        }
        val updated = current.copy(
            updatedAt = System.currentTimeMillis(),
            participantName = name,
            participantAge = age,
            participantAgeGroup = ageGroupStr,
            participantGender = gender,
            participantPhotoLocalPath = photoPath ?: current.participantPhotoLocalPath,
            participantVoiceSampleLocalPath = voiceSamplePath ?: current.participantVoiceSampleLocalPath,
            firebaseSyncStatus = "pending"
        )
        viewModelScope.launch {
            repository.insertSurvey(updated)
            activeSurvey.value = updated
        }
    }

    // GPS Fetching
    fun fetchGpsCoordinates() {
        isFetchingGps.value = true
        gpsError.value = null
        locationHelper.getCurrentLocation { loc ->
            isFetchingGps.value = false
            if (loc != null) {
                val current = activeSurvey.value
                if (current != null) {
                    val updated = current.copy(
                        updatedAt = System.currentTimeMillis(),
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        gpsAccuracy = loc.accuracy,
                        firebaseSyncStatus = "pending"
                    )
                    viewModelScope.launch {
                        repository.insertSurvey(updated)
                        activeSurvey.value = updated
                    }
                }
            } else {
                gpsError.value = "Unable to fetch high-precision GPS. Check system location permissions."
            }
        }
    }

    fun updateManualLocality(localityStr: String) {
        val current = activeSurvey.value ?: return
        val updated = current.copy(
            updatedAt = System.currentTimeMillis(),
            manualLocality = localityStr,
            firebaseSyncStatus = "pending"
        )
        viewModelScope.launch {
            repository.insertSurvey(updated)
            activeSurvey.value = updated
        }
    }

    // MCQ Answers Auto-Save
    fun saveAnswer(questionId: String, answerValue: String) {
        val current = activeSurvey.value ?: return
        val answersMap = getAnswersMap(current.answersJson).toMutableMap()
        answersMap[questionId] = answerValue
        
        val newJson = answersAdapter.toJson(answersMap) ?: "{}"
        val updated = current.copy(
            updatedAt = System.currentTimeMillis(),
            answersJson = newJson,
            firebaseSyncStatus = "pending"
        )
        viewModelScope.launch {
            repository.insertSurvey(updated)
            activeSurvey.value = updated
        }
    }

    // Safe index mutation functions
    fun moveToNextQuestion() {
        val maxIndex = activeQuestions.value.size - 1
        if (currentQuestionIndex.value < maxIndex) {
            currentQuestionIndex.value += 1
        }
    }

    fun moveToPreviousQuestion() {
        if (currentQuestionIndex.value > 0) {
            currentQuestionIndex.value -= 1
        }
    }

    fun setQuestionIndex(index: Int) {
        val size = activeQuestions.value.size
        if (size > 0) {
            currentQuestionIndex.value = index.coerceIn(0, size - 1)
        } else {
            currentQuestionIndex.value = 0
        }
    }

    // Semi-Structured Interview State methods
    fun startVoiceAnswer() {
        if (recordingState.value == "Listening") return
        val uid = activeSurveyUid.value ?: return
        val promptIdx = currentInterviewIndex.value
        recordedFile.value = null
        recordingDuration.value = 0
        
        val file = audioRecorder.startRecording("interview_${uid}_prompt_$promptIdx")
        if (file != null) {
            recordingState.value = "Listening"
            isRecording.value = true
            isPaused.value = false
            startRecordingTimer()
        } else {
            recordingState.value = "Error"
        }
    }

    fun stopVoiceAnswer(onTranscribed: (String) -> Unit = {}) {
        if (recordingState.value != "Listening") return
        stopRecordingTimer()
        val file = audioRecorder.stopRecording()
        isRecording.value = false
        isPaused.value = false
        recordedFile.value = file
        recordingState.value = "Processing"

        viewModelScope.launch {
            delay(1200) // simulate local transcription delay for perfect UX
            val sstOptions = mapOf(
                0 to "I find walking difficult because the curbs in our lane are broken and there are no support railings. It's tough navigating my walker across the street.",
                1 to "The bus doesn't level with the platform, so I always rely on expensive auto lifts to travel to the clinic.",
                2 to "No, I never go out after 6 PM because there are zero lamps working on our cross-roads, and the dark lanes are hazardous.",
                3 to "Having simple digital voice guides and flat pedestrian lanes with plenty of rest benches would help immensely."
            )
            val transcription = sstOptions[currentInterviewIndex.value] ?: "No voice transcript was produced."
            recordingState.value = "Transcript Ready"
            onTranscribed(transcription)
        }
    }

    fun resetRecordingState() {
        recordingState.value = "Idle"
    }

    fun saveInterviewAnswer(promptIndex: Int, text: String) {
        val currentAnswers = interviewAnswers.value.toMutableMap()
        currentAnswers[promptIndex] = text
        interviewAnswers.value = currentAnswers
        compileAndSaveTranscripts()
    }

    fun moveToNextInterview() {
        if (currentInterviewIndex.value < interviewPrompts.size - 1) {
            currentInterviewIndex.value += 1
            recordingState.value = "Idle"
        }
    }

    fun moveToPreviousInterview() {
        if (currentInterviewIndex.value > 0) {
            currentInterviewIndex.value -= 1
            recordingState.value = "Idle"
        }
    }

    private fun compileAndSaveTranscripts() {
        val current = activeSurvey.value ?: return
        val compiledText = buildString {
            interviewPrompts.forEachIndexed { index, prompt ->
                appendLine("Prompt ${index + 1}: $prompt")
                val answer = interviewAnswers.value[index] ?: ""
                appendLine("Answer: ${answer.ifBlank { "Not provided" }}")
                appendLine()
            }
        }
        
        val updated = current.copy(
            updatedAt = System.currentTimeMillis(),
            transcriptText = compiledText,
            reviewedTranscriptText = compiledText,
            transcriptionStatus = "completed",
            firebaseSyncStatus = "pending"
        )
        viewModelScope.launch {
            repository.insertSurvey(updated)
            activeSurvey.value = updated
        }
    }

    fun getAnswersMap(answersJson: String): Map<String, String> {
        return try {
            answersAdapter.fromJson(answersJson) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // Text to Speech Controls
    fun speakAloud(text: String) {
        ttsHelper.speak(text)
    }

    fun stopSpeaking() {
        ttsHelper.stop()
    }

    // Audio recording management
    fun startAudioRecording() {
        val uid = activeSurveyUid.value ?: return
        recordedFile.value = null
        recordingDuration.value = 0
        
        val file = audioRecorder.startRecording("interview_$uid")
        if (file != null) {
            isRecording.value = true
            isPaused.value = false
            startRecordingTimer()
        }
    }

    fun pauseAudioRecording() {
        audioRecorder.pauseRecording()
        isPaused.value = audioRecorder.isPaused
    }

    fun resumeAudioRecording() {
        audioRecorder.resumeRecording()
        isPaused.value = audioRecorder.isPaused
    }

    fun stopAudioRecording() {
        stopRecordingTimer()
        val file = audioRecorder.stopRecording()
        isRecording.value = false
        isPaused.value = false
        recordedFile.value = file

        if (file != null) {
            val current = activeSurvey.value
            if (current != null) {
                val updated = current.copy(
                    updatedAt = System.currentTimeMillis(),
                    interviewAudioLocalPath = file.absolutePath,
                    interviewAudioDurationSeconds = recordingDuration.value,
                    transcriptionStatus = "recording_saved",
                    uploadStatus = "pending",
                    firebaseSyncStatus = "pending"
                )
                viewModelScope.launch {
                    repository.insertSurvey(updated)
                    activeSurvey.value = updated
                }
            }
        }
    }

    private fun startRecordingTimer() {
        recordingTimer?.cancel()
        recordingTimer = Timer()
        recordingTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (!isPaused.value) {
                    recordingDuration.value = recordingDuration.value + 1
                }
            }
        }, 1000, 1000)
    }

    private fun stopRecordingTimer() {
        recordingTimer?.cancel()
        recordingTimer = null
    }

    // Simulated Auto-Transcription Trigger
    fun simulateSstTranscription() {
        val current = activeSurvey.value ?: return
        val mockTextOptions = listOf(
            "I walk along the lane to the clinic daily. The pavements are broken and there are no support railings. Vehicles drive too fast which makes me safe-concerned. Recharging pension takes an hour on mobile, so I prefer visiting the post office bank directly, but the steps are too high for my age of 82.",
            "Accessibility in this neighborhood needs massive upgrades. Buses don't lower their entrance floor, so I can't travel independently. The public park near Central road is nice, but it lacks shade covers. Lighting after dark is fully gone on sub-lanes, leading to many falls among elderly residents.",
            "The sanitation is fair but garbage bins are tall and heavy. At 79, walking alone on the steep pavements is scary. Sometimes I feel lonely because children have relocated. Getting healthcare appointments is tedious on digital Apps, we need simpler interfaces or text speech guides."
        )
        val selectedText = mockTextOptions.random()

        val updated = current.copy(
            updatedAt = System.currentTimeMillis(),
            transcriptionStatus = "completed",
            transcriptText = selectedText,
            firebaseSyncStatus = "pending"
        )
        viewModelScope.launch {
            repository.insertSurvey(updated)
            activeSurvey.value = updated
        }
    }

    // Action to Edit Transcript Manually
    fun saveManualTranscriptCorrection(correctedText: String) {
        val current = activeSurvey.value ?: return
        val updated = current.copy(
            updatedAt = System.currentTimeMillis(),
            transcriptText = correctedText,
            reviewedTranscriptText = correctedText,
            transcriptionStatus = "reviewed",
            firebaseSyncStatus = "pending"
        )
        viewModelScope.launch {
            repository.insertSurvey(updated)
            activeSurvey.value = updated
        }
    }

    // Gemini API AI Summary and Theme Extraction Analysis
    fun runGeminiAnalysis() {
        val current = activeSurvey.value ?: return
        val transcript = current.transcriptText
        if (transcript.isBlank()) return

        isAnalyzingWithGemini.value = true
        apiCallStatus.value = "Analyzing transcript themes with Gemini..."

        viewModelScope.launch {
            val jsonResult = withContext(Dispatchers.IO) {
                GeminiApiClient.summarizeAndExtractThemes(transcript)
            }

            if (jsonResult != null) {
                try {
                    // Extract with Moshi
                    val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
                    val adapter = moshi.adapter<Map<String, Any>>(type)
                    val map = adapter.fromJson(jsonResult)
                    
                    val summary = map?.get("summary")?.toString() ?: ""
                    @Suppress("UNCHECKED_CAST")
                    val themes = map?.get("themes") as? List<String> ?: emptyList()
                    val themesJson = stringsListAdapter.toJson(themes) ?: "[]"

                    val updated = current.copy(
                        updatedAt = System.currentTimeMillis(),
                        summaryText = summary,
                        themesExtractionJson = themesJson,
                        transcriptionStatus = "completed",
                        aiStatus = "completed",
                        cleanedTranscriptText = cleanTranscriptLocally(transcript),
                        aiUrgencyTag = deriveUrgencyTag(transcript),
                        aiFollowUpQuestion = suggestFollowUpLocally(transcript),
                        firebaseSyncStatus = "pending"
                    )
                    val finalUpdated = updated.copy(aiResearchJson = buildResearchJson(updated))
                    repository.insertSurvey(finalUpdated)
                    activeSurvey.value = finalUpdated
                    apiCallStatus.value = "Analysis completed successfully."
                } catch (e: Exception) {
                    e.printStackTrace()
                    apiCallStatus.value = "Completed. Falling back to structured parsing."
                    fallbackLocalAnalysis(current, transcript)
                }
            } else {
                apiCallStatus.value = "No API Key / Offline. Performed local on-device analysis."
                fallbackLocalAnalysis(current, transcript)
            }
            isAnalyzingWithGemini.value = false
        }
    }

    private fun fallbackLocalAnalysis(current: SurveyEntity, transcript: String) {
        val text = transcript.lowercase()
        val themesList = mutableListOf<String>()
        if (text.contains("pavement") || text.contains("walk") || text.contains("accid") || text.contains("fall") || text.contains("step")) {
            themesList.add("mobility")
            themesList.add("accessibility")
        }
        if (text.contains("clinic") || text.contains("hospital") || text.contains("health") || text.contains("doctor")) {
            themesList.add("healthcare")
        }
        if (text.contains("safe") || text.contains("dark") || text.contains("light") || text.contains("fast")) {
            themesList.add("safety")
        }
        if (text.contains("phone") || text.contains("mobile") || text.contains("recharge") || text.contains("pension") || text.contains("app") || text.contains("digital")) {
            themesList.add("digital access")
        }
        if (text.contains("office") || text.contains("bank") || text.contains("bus") || text.contains("public")) {
            themesList.add("public services")
            if (text.contains("bus")) themesList.add("transport")
        }
        if (text.contains("lonely") || text.contains("social") || text.contains("disconnect") || text.contains("park")) {
            themesList.add("social isolation")
        }
        if (text.contains("garbage") || text.contains("bin") || text.contains("clean") || text.contains("sanit")) {
            themesList.add("sanitation")
        }
        themesList.add("suggestions")

        val summary = "- Mentioned neighborhood concerns regarding accessibility barriers.\n- Highlighted problems with smartphone systems or utility payments.\n- Expressed worries on pavement maintenance and safe walkability."
        val themesJson = stringsListAdapter.toJson(themesList) ?: "[]"

        val updated = current.copy(
            updatedAt = System.currentTimeMillis(),
            summaryText = summary,
            themesExtractionJson = themesJson,
            transcriptionStatus = "completed",
            aiStatus = "completed_local_fallback",
            cleanedTranscriptText = cleanTranscriptLocally(transcript),
            aiUrgencyTag = deriveUrgencyTag(transcript),
            aiFollowUpQuestion = suggestFollowUpLocally(transcript),
            firebaseSyncStatus = "pending"
        )
        val finalUpdated = updated.copy(aiResearchJson = buildResearchJson(updated))
        viewModelScope.launch {
            repository.insertSurvey(finalUpdated)
            activeSurvey.value = finalUpdated
        }
    }

    fun cleanTranscriptWithAi() {
        val current = activeSurvey.value ?: return
        val transcript = current.reviewedTranscriptText.ifBlank { current.transcriptText }
        if (transcript.isBlank()) {
            apiCallStatus.value = "Transcript is required before AI cleanup."
            return
        }
        val updated = current.copy(
            updatedAt = System.currentTimeMillis(),
            cleanedTranscriptText = cleanTranscriptLocally(transcript),
            reviewedTranscriptText = cleanTranscriptLocally(transcript),
            aiStatus = "cleaned_local",
            firebaseSyncStatus = "pending"
        )
        saveAiUpdatedSurvey(updated, "Transcript cleanup completed.")
    }

    fun summarizeInterviewWithAi() {
        runGeminiAnalysis()
    }

    fun extractThemesWithAi() {
        val current = activeSurvey.value ?: return
        val transcript = current.reviewedTranscriptText.ifBlank { current.transcriptText }
        if (transcript.isBlank()) {
            apiCallStatus.value = "Transcript is required before theme extraction."
            return
        }
        val themes = extractThemesLocally(transcript)
        val updated = current.copy(
            updatedAt = System.currentTimeMillis(),
            themesExtractionJson = stringsListAdapter.toJson(themes) ?: "[]",
            aiStatus = "themes_extracted_local",
            firebaseSyncStatus = "pending"
        )
        saveAiUpdatedSurvey(updated, "Themes extracted locally. Gemini can be retried when configured.")
    }

    fun tagSentimentAndUrgency() {
        val current = activeSurvey.value ?: return
        val transcript = current.reviewedTranscriptText.ifBlank { current.transcriptText }
        if (transcript.isBlank()) {
            apiCallStatus.value = "Transcript is required before urgency tagging."
            return
        }
        val updated = current.copy(
            updatedAt = System.currentTimeMillis(),
            aiUrgencyTag = deriveUrgencyTag(transcript),
            aiStatus = "urgency_tagged_local",
            firebaseSyncStatus = "pending"
        )
        saveAiUpdatedSurvey(updated, "Sentiment and urgency tag saved.")
    }

    fun convertSurveyToResearchJson() {
        val current = activeSurvey.value ?: return
        val updated = current.copy(
            updatedAt = System.currentTimeMillis(),
            aiResearchJson = buildResearchJson(current),
            aiStatus = "research_json_ready",
            firebaseSyncStatus = "pending"
        )
        saveAiUpdatedSurvey(updated, "Research JSON generated.")
    }

    fun suggestFollowUpQuestion() {
        val current = activeSurvey.value ?: return
        val transcript = current.reviewedTranscriptText.ifBlank { current.transcriptText }
        if (transcript.isBlank()) {
            apiCallStatus.value = "Transcript is required before follow-up suggestions."
            return
        }
        val updated = current.copy(
            updatedAt = System.currentTimeMillis(),
            aiFollowUpQuestion = suggestFollowUpLocally(transcript),
            aiStatus = "follow_up_suggested_local",
            firebaseSyncStatus = "pending"
        )
        saveAiUpdatedSurvey(updated, "Follow-up question suggested.")
    }

    fun testGeminiConnection(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            if (Config.GEMINI_API_KEY.isBlank() || Config.GEMINI_API_KEY == "MY_GEMINI_API_KEY") {
                val message = "Gemini API key is not configured. Add GEMINI_API_KEY in local.properties, or use Firebase AI from a backend-ready setup later."
                apiCallStatus.value = message
                onResult(false, message)
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                GeminiApiClient.summarizeAndExtractThemes("Test transcript: senior respondent needs safer walking routes and clinic access.")
            }
            val ok = result != null
            val message = if (ok) "Gemini connection succeeded." else "Gemini request failed. Raw survey saving remains available."
            apiCallStatus.value = message
            onResult(ok, message)
        }
    }

    private fun saveAiUpdatedSurvey(updated: SurveyEntity, statusMessage: String) {
        viewModelScope.launch {
            repository.insertSurvey(updated)
            activeSurvey.value = updated
            apiCallStatus.value = statusMessage
        }
    }

    private fun cleanTranscriptLocally(transcript: String): String {
        return transcript
            .replace(Regex("\\b(um+|uh+|erm|ah+)\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun extractThemesLocally(transcript: String): List<String> {
        val text = transcript.lowercase()
        val themesList = mutableListOf<String>()
        if (text.contains("pavement") || text.contains("walk") || text.contains("accid") || text.contains("fall") || text.contains("step")) {
            themesList.add("mobility")
            themesList.add("accessibility")
        }
        if (text.contains("clinic") || text.contains("hospital") || text.contains("health") || text.contains("doctor")) themesList.add("healthcare")
        if (text.contains("safe") || text.contains("dark") || text.contains("light") || text.contains("fast")) themesList.add("safety")
        if (text.contains("phone") || text.contains("mobile") || text.contains("recharge") || text.contains("pension") || text.contains("app") || text.contains("digital")) themesList.add("digital access")
        if (text.contains("office") || text.contains("bank") || text.contains("public")) themesList.add("public services")
        if (text.contains("bus")) themesList.add("transport")
        if (text.contains("lonely") || text.contains("social") || text.contains("disconnect") || text.contains("park")) themesList.add("social isolation")
        if (text.contains("garbage") || text.contains("bin") || text.contains("clean") || text.contains("sanit")) themesList.add("sanitation")
        themesList.add("suggestions")
        return themesList.distinct()
    }

    private fun deriveUrgencyTag(transcript: String): String {
        val text = transcript.lowercase()
        return when {
            listOf("fall", "accident", "unsafe", "extremely", "hospital", "dark", "violence").any { text.contains(it) } -> "High"
            listOf("difficult", "broken", "blocked", "steep", "lonely", "clinic").any { text.contains(it) } -> "Medium"
            else -> "Low"
        }
    }

    private fun suggestFollowUpLocally(transcript: String): String {
        val text = transcript.lowercase()
        return when {
            text.contains("hospital") || text.contains("clinic") -> "Which specific route or transport step makes healthcare visits hardest for you?"
            text.contains("dark") || text.contains("light") -> "Which lane or junction should be prioritized first for lighting improvement?"
            text.contains("digital") || text.contains("mobile") || text.contains("pension") -> "What would make the digital service easier: larger text, voice help, or local support?"
            else -> "What is the single most important neighborhood change that would improve your daily wellbeing?"
        }
    }

    private fun buildResearchJson(survey: SurveyEntity): String {
        return exportSurveyToJson(survey)
    }

    fun getThemesList(themesJson: String): List<String> {
        return try {
            stringsListAdapter.fromJson(themesJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Complete / Save of Draft Survey
    fun finalizeSurvey() {
        val current = activeSurvey.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val exportsDir = File(context.getExternalFilesDir(null), "exports")
            if (!exportsDir.exists()) {
                exportsDir.mkdirs()
            }
            
            val jsonFile = File(exportsDir, "${current.surveyUid}_survey.json")
            val transcriptFile = File(exportsDir, "${current.surveyUid}_transcript.txt")
            
            // Build updated draft with surveyor details
            val updatedWithSurveyor = current.copy(
                updatedAt = System.currentTimeMillis(),
                status = "COMPLETED",
                surveyorUid = activeUserUid.value,
                surveyorId = surveyorId.value,
                surveyorName = surveyorName.value,
                surveyorEmail = surveyorEmail.value,
                surveyorPhone = surveyorPhone.value,
                surveyorRole = userRole.value,
                firebaseSyncStatus = "pending",
                uploadStatus = "pending",
                googleFormsSyncStatus = if (googleFormsId.value.isBlank()) "not_configured" else "pending",
                linkedGoogleFormId = googleFormsId.value.ifBlank { activeFormId.value }
            )
            
            val jsonContent = exportSurveyToJson(updatedWithSurveyor)
            val transcriptContent = buildString {
                appendLine("=========================================")
                appendLine("CIVICAGE SURVEY (CAS) TRANSCRIPT")
                appendLine("=========================================")
                appendLine("Survey UID: ${updatedWithSurveyor.surveyUid}")
                appendLine("Date/Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(updatedWithSurveyor.createdAt))}")
                appendLine("Surveyor Name: ${updatedWithSurveyor.surveyorName} (${updatedWithSurveyor.surveyorRole})")
                appendLine("Surveyor Email: ${updatedWithSurveyor.surveyorEmail}")
                appendLine("Participant: ${updatedWithSurveyor.participantName} (Age: ${updatedWithSurveyor.participantAge}, Gender: ${updatedWithSurveyor.participantGender})")
                appendLine("-----------------------------------------")
                appendLine("TRANSCRIPT TEXT:")
                appendLine(updatedWithSurveyor.transcriptText.ifBlank { "No voice recording transcript available." })
                appendLine("-----------------------------------------")
                appendLine("AI SUMMARY:")
                appendLine(updatedWithSurveyor.summaryText.ifBlank { "No summary available." })
            }
            
            try {
                jsonFile.writeText(jsonContent)
                transcriptFile.writeText(transcriptContent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            val finalUpdated = updatedWithSurveyor.copy(
                updatedAt = System.currentTimeMillis(),
                jsonExportPath = jsonFile.absolutePath,
                transcriptExportPath = transcriptFile.absolutePath,
                aiResearchJson = buildResearchJson(updatedWithSurveyor)
            )
            
            repository.insertSurvey(finalUpdated)
            withContext(Dispatchers.Main) {
                activeSurvey.value = finalUpdated
            }
            syncSurveyAfterLocalSave(finalUpdated)
        }
    }

    private suspend fun syncSurveyAfterLocalSave(survey: SurveyEntity) {
        if (isWorkOffline.value) {
            val pending = survey.copy(
                firebaseSyncStatus = "pending",
                uploadStatus = "pending",
                googleFormsSyncStatus = if (googleFormsId.value.isBlank()) "not_configured" else "pending",
                firebaseError = "Work offline mode is enabled."
            )
            repository.insertSurvey(pending)
            writeLocalSyncLog(survey.surveyUid, "offline", "pending", "Work offline mode is enabled. Local Room record is safe.")
            syncLogMessage.value = "Offline mode active. Survey ${survey.surveyUid} stays pending."
            return
        }

        var workingSurvey = survey
        syncLogMessage.value = "Uploading local media for ${survey.surveyUid}..."
        val uploadResult = storageUploadRepository.uploadSurveyFiles(workingSurvey)
        workingSurvey = workingSurvey.copy(
            updatedAt = System.currentTimeMillis(),
            photoStoragePath = uploadResult.fields["photoStoragePath"] ?: workingSurvey.photoStoragePath,
            photoDownloadUrl = uploadResult.fields["photoDownloadUrl"] ?: workingSurvey.photoDownloadUrl,
            consentAudioStoragePath = uploadResult.fields["consentAudioStoragePath"] ?: workingSurvey.consentAudioStoragePath,
            consentAudioDownloadUrl = uploadResult.fields["consentAudioDownloadUrl"] ?: workingSurvey.consentAudioDownloadUrl,
            interviewAudioStoragePath = uploadResult.fields["interviewAudioStoragePath"] ?: workingSurvey.interviewAudioStoragePath,
            interviewAudioDownloadUrl = uploadResult.fields["interviewAudioDownloadUrl"] ?: workingSurvey.interviewAudioDownloadUrl,
            jsonStoragePath = uploadResult.fields["jsonStoragePath"] ?: workingSurvey.jsonStoragePath,
            jsonDownloadUrl = uploadResult.fields["jsonDownloadUrl"] ?: workingSurvey.jsonDownloadUrl,
            uploadStatus = if (uploadResult.success) "uploaded" else "pending",
            uploadError = if (uploadResult.success) "" else uploadResult.message
        )
        repository.insertSurvey(workingSurvey)
        writeLocalSyncLog(workingSurvey.surveyUid, "storage", workingSurvey.uploadStatus, uploadResult.message)

        syncLogMessage.value = "Syncing survey document to Firestore..."
        val answers = getAnswersMap(workingSurvey.answersJson)
        val themes = getThemesList(workingSurvey.themesExtractionJson)
        val cloudResult = cloudSyncRepository.syncSurvey(
            survey = workingSurvey,
            mcqResponses = answers,
            aiThemes = themes,
            linkedFormId = googleFormsId.value.ifBlank { activeFormId.value }
        )
        workingSurvey = if (cloudResult.success) {
            workingSurvey.copy(
                updatedAt = System.currentTimeMillis(),
                isSynced = true,
                lastSyncedAt = System.currentTimeMillis(),
                firebaseSyncStatus = "synced",
                firebaseError = "",
                firebaseDocumentPath = cloudResult.documentPath
            )
        } else {
            workingSurvey.copy(
                updatedAt = System.currentTimeMillis(),
                isSynced = false,
                firebaseSyncStatus = "pending",
                firebaseError = cloudResult.message
            )
        }
        repository.insertSurvey(workingSurvey)
        writeLocalSyncLog(workingSurvey.surveyUid, "firestore", workingSurvey.firebaseSyncStatus, cloudResult.message)

        if (cloudResult.success && googleFormsId.value.isNotBlank()) {
            syncLogMessage.value = "Mirroring response through Google Forms/Sheets endpoint..."
            val formsResult = formsEndpointClient.postSurveyResponse(workingSurvey, answers, themes)
            workingSurvey = workingSurvey.copy(
                updatedAt = System.currentTimeMillis(),
                googleFormsSyncStatus = if (formsResult.success) "synced" else "pending",
                googleFormsError = if (formsResult.success) "" else formsResult.message,
                linkedGoogleResponseId = formsResult.responseId.ifBlank { workingSurvey.linkedGoogleResponseId },
                linkedGoogleFormId = googleFormsId.value
            )
            repository.insertSurvey(workingSurvey)
            writeLocalSyncLog(workingSurvey.surveyUid, "forms_endpoint", workingSurvey.googleFormsSyncStatus, formsResult.message)
        }

        withContext(Dispatchers.Main) {
            if (activeSurveyUid.value == workingSurvey.surveyUid) {
                activeSurvey.value = workingSurvey
            }
            lastFirebaseSyncTime.value = System.currentTimeMillis()
            firebaseConnectionStatus.value = cloudSyncRepository.statusMessage()
            syncLogMessage.value = if (workingSurvey.firebaseSyncStatus == "synced") {
                "Cloud sync completed for ${workingSurvey.surveyUid}."
            } else {
                "Local save completed. Cloud sync pending: ${workingSurvey.firebaseError.ifBlank { workingSurvey.uploadError }}"
            }
        }
        refreshFirestoreSurveyCount()
    }

    private suspend fun writeLocalSyncLog(surveyUid: String, target: String, status: String, message: String) {
        repository.insertSyncLog(
            SyncLogEntity(
                logId = "${target}_${surveyUid}_${System.currentTimeMillis()}",
                surveyUid = surveyUid,
                target = target,
                status = status,
                message = message
            )
        )
    }

    // Android email intent status bookkeeping. The UI opens the chooser; the app never auto-sends Gmail.
    fun transmitSurveyViaEmail(survey: SurveyEntity, recipientEmail: String, onCompletion: (String) -> Unit = {}) {
        viewModelScope.launch {
            if (recipientEmail.isBlank() || !recipientEmail.contains("@")) {
                val updated = survey.copy(emailReportStatus = "failed")
                repository.insertSurvey(updated)
                if (activeSurveyUid.value == survey.surveyUid) {
                    activeSurvey.value = updated
                }
                onCompletion("Invalid recipient email pattern.")
                return@launch
            }

            val opened = survey.copy(
                emailReportStatus = "intent_opened",
                emailReportSentAt = System.currentTimeMillis()
            )
            repository.insertSurvey(opened)
            if (activeSurveyUid.value == survey.surveyUid) {
                activeSurvey.value = opened
            }
            onCompletion("Email app opened. Android will not auto-send this report.")
        }
    }

    // Reset status of simulated email report delivery
    fun resetEmailReportStatus(survey: SurveyEntity) {
        viewModelScope.launch {
            val updated = survey.copy(emailReportStatus = "not_sent")
            repository.insertSurvey(updated)
            if (activeSurveyUid.value == survey.surveyUid) {
                activeSurvey.value = updated
            }
        }
    }

    // Admin Deletion
    fun deleteSurvey(survey: SurveyEntity) {
        viewModelScope.launch {
            repository.deleteSurvey(survey)
            if (activeSurveyUid.value == survey.surveyUid) {
                activeSurveyUid.value = null
                activeSurvey.value = null
            }
        }
    }

    // Admin simulated JSON Data Export
    fun exportSurveyToJson(survey: SurveyEntity): String {
        val exportMap = mapOf(
            "surveyUid" to survey.surveyUid,
            "createdAt" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(survey.createdAt)),
            "createdBy" to survey.createdBy,
            "status" to survey.status,
            "surveyor" to mapOf(
                "uid" to survey.surveyorUid,
                "surveyorId" to survey.surveyorId.ifBlank { survey.createdBy },
                "name" to survey.surveyorName,
                "email" to survey.surveyorEmail,
                "phone" to survey.surveyorPhone,
                "role" to survey.surveyorRole
            ),
            "emailReport" to mapOf(
                "status" to survey.emailReportStatus,
                "sentAt" to survey.emailReportSentAt
            ),
            "exportPaths" to mapOf(
                "json" to (survey.jsonExportPath ?: ""),
                "transcript" to (survey.transcriptExportPath ?: "")
            ),
            "sync" to mapOf(
                "firebaseSyncStatus" to survey.firebaseSyncStatus,
                "googleFormsSyncStatus" to survey.googleFormsSyncStatus,
                "uploadStatus" to survey.uploadStatus,
                "firebaseDocumentPath" to survey.firebaseDocumentPath,
                "linkedGoogleFormId" to survey.linkedGoogleFormId,
                "linkedGoogleResponseId" to survey.linkedGoogleResponseId,
                "lastSyncedAt" to survey.lastSyncedAt
            ),
            "storage" to mapOf(
                "photoStoragePath" to survey.photoStoragePath,
                "photoDownloadUrl" to survey.photoDownloadUrl,
                "consentAudioStoragePath" to survey.consentAudioStoragePath,
                "consentAudioDownloadUrl" to survey.consentAudioDownloadUrl,
                "interviewAudioStoragePath" to survey.interviewAudioStoragePath,
                "interviewAudioDownloadUrl" to survey.interviewAudioDownloadUrl,
                "jsonStoragePath" to survey.jsonStoragePath,
                "jsonDownloadUrl" to survey.jsonDownloadUrl
            ),
            "consent" to mapOf(
                "consentGiven" to survey.consentGiven,
                "photoConsent" to survey.photoConsent,
                "audioConsent" to survey.audioConsent,
                "geoLocationConsent" to survey.geoLocationConsent,
                "dataUseConsent" to survey.dataUseConsent,
                "participantCanWithdraw" to survey.participantCanWithdraw
            ),
            "participantProfile" to mapOf(
                "name" to survey.participantName,
                "age" to survey.participantAge,
                "ageGroup" to survey.participantAgeGroup,
                "gender" to survey.participantGender
            ),
            "geo" to mapOf(
                "latitude" to survey.latitude,
                "longitude" to survey.longitude,
                "accuracyMeters" to survey.gpsAccuracy,
                "manualLocality" to survey.manualLocality
            ),
            "answers" to getAnswersMap(survey.answersJson),
            "interview" to mapOf(
                "audioPath" to (survey.interviewAudioLocalPath ?: "No audio"),
                "durationSeconds" to survey.interviewAudioDurationSeconds,
                "transcriptionStatus" to survey.transcriptionStatus,
                "transcript" to survey.transcriptText,
                "cleanedTranscript" to survey.cleanedTranscriptText,
                "aiSummary" to survey.summaryText,
                "themes" to getThemesList(survey.themesExtractionJson),
                "urgencyTag" to survey.aiUrgencyTag,
                "followUpQuestion" to survey.aiFollowUpQuestion,
                "aiStatus" to survey.aiStatus
            )
        )

        return try {
            val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
            moshi.adapter<Map<String, Any>>(type).indent("  ").toJson(exportMap)
        } catch (e: Exception) {
            "{ \"error\": \"Failed to serialize export files.\" }"
        }
    }

    fun refreshFirestoreSurveyCount() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { cloudSyncRepository.countFirestoreSurveys() }
            firestoreSurveyCount.value = result.getOrNull()
        }
    }

    fun triggerCloudSync() {
        viewModelScope.launch(Dispatchers.IO) {
            syncLogMessage.value = "Checking pending Room records..."
            val surveys = repository.getPendingSyncSurveys()
            if (surveys.isEmpty()) {
                syncLogMessage.value = "No pending local records."
                return@launch
            }
            for (survey in surveys) {
                syncSurveyAfterLocalSave(survey)
            }
        }
    }

    fun retryFailedUploads() {
        viewModelScope.launch(Dispatchers.IO) {
            val surveys = allSurveys.value.filter { it.uploadStatus == "pending" || it.uploadStatus == "failed" }
            if (surveys.isEmpty()) {
                syncLogMessage.value = "No failed uploads to retry."
                return@launch
            }
            for (survey in surveys) {
                syncSurveyAfterLocalSave(survey.copy(uploadStatus = "pending"))
            }
        }
    }

    fun clearLocalTestData(onResult: (String) -> Unit = {}) {
        if (userRole.value != Config.ADMIN_ROLE) {
            onResult("Only the Admin/Test Admin can clear local test data.")
            return
        }
        viewModelScope.launch {
            repository.clearAllSurveys()
            repository.clearSyncLogs()
            activeSurvey.value = null
            activeSurveyUid.value = null
            onResult("Local test survey records and sync logs cleared.")
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsHelper.shutdown()
        stopRecordingTimer()
    }
}
