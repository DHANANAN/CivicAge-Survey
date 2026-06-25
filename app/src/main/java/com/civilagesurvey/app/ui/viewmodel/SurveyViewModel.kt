package com.civilagesurvey.app.ui.viewmodel

import android.app.Application
import android.os.Environment
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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

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

    // User Session
    val surveyorPhone = MutableStateFlow("")
    val surveyorEmail = MutableStateFlow("")
    val surveyorName = MutableStateFlow("")
    val surveyorId = MutableStateFlow("")
    val userRole = MutableStateFlow("Field Surveyor")
    val isNetworkOnline = MutableStateFlow(true)
    val authStatusMessage = MutableStateFlow("")
    val activeUserUid = MutableStateFlow("")

    // Google Forms State
    val googleFormSources: StateFlow<List<GoogleFormSourceEntity>> = repository.allGoogleFormSources
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val mirrorResult = MutableStateFlow<GoogleFormsRepository.MirrorResult?>(null)
    val isFetchingForm = MutableStateFlow(false)

    // Options and customization
    val appearanceMode = MutableStateFlow("Professional Light")
    val fontSizeMode = MutableStateFlow("Standard")
    val isFullscreenMode = MutableStateFlow(false)
    val isHighContrast = MutableStateFlow(false)
    val speechLanguage = MutableStateFlow("English")
    
    // Lists
    val allSurveys: StateFlow<List<SurveyEntity>> = repository.allSurveys
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredSurveys: StateFlow<List<SurveyEntity>> = combine(allSurveys, userRole, surveyorId) { surveys, role, currentId ->
        if (role == Config.ADMIN_ROLE) {
            surveys
        } else {
            surveys.filter { it.createdBy == currentId }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeQuestions: StateFlow<List<FormQuestionEntity>> = repository.allQuestions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard Stats
    val completedCount = allSurveys.map { it.count { s -> s.status == "COMPLETED" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    val draftCount = allSurveys.map { it.count { s -> s.status == "DRAFT" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    val syncedCount = allSurveys.map { it.count { s -> s.isSynced } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    val offlineCount = allSurveys.map { it.count { s -> !s.isSynced } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Active Survey Drafting State
    val activeSurveyUid = MutableStateFlow<String?>(null)
    val activeSurvey = MutableStateFlow<SurveyEntity?>(null)

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

        viewModelScope.launch {
            repository.allQuestions.first().let { currentList ->
                if (currentList.isEmpty()) {
                    repository.insertQuestions(DefaultQuestions.list)
                }
            }
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
        // Simple local login logic as requested
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

        onResult(true, "Signed in locally as $role")
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
        surveyorEmail.value = ""
        surveyorName.value = ""
        surveyorId.value = ""
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

    fun startDcaSession(instrument: Instrument) {
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
        
        val uid = "$pCode-${instrument.code}-S${System.currentTimeMillis()}"
        
        val initialSurvey = SurveyEntity(
            surveyUid = uid,
            createdBy = surveyorId.value,
            status = "DRAFT",
            surveyorId = surveyorId.value,
            surveyorName = surveyorName.value,
            participantAgeGroup = selectedStakeholder.value,
            manualLocality = selectedCity.value // Temporarily using locality for city code
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
        locationHelper.getCurrentLocation { loc ->
            isFetchingGps.value = false
            if (loc != null) {
                val current = activeSurvey.value ?: return@getCurrentLocation
                val updated = current.copy(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    gpsAccuracy = loc.accuracy,
                    updatedAt = System.currentTimeMillis()
                )
                viewModelScope.launch {
                    repository.insertSurvey(updated)
                    activeSurvey.value = updated
                }
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
        }
    }

    fun moveToNextQuestion() {
        val instrument = currentInstrument.value ?: return
        val currentSection = instrument.sections.getOrNull(currentSectionIndex.value) ?: return
        
        if (currentQuestionIndex.value < currentSection.questions.size - 1) {
            currentQuestionIndex.value++
        } else if (currentSectionIndex.value < instrument.sections.size - 1) {
            currentSectionIndex.value++
            currentQuestionIndex.value = 0
        }
    }

    fun moveToPreviousQuestion() {
        if (currentQuestionIndex.value > 0) {
            currentQuestionIndex.value--
        } else if (currentSectionIndex.value > 0) {
            currentSectionIndex.value--
            val instrument = currentInstrument.value ?: return
            currentQuestionIndex.value = (instrument.sections[currentSectionIndex.value].questions.size - 1).coerceAtLeast(0)
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
        val compiled = buildString {
            interviewPrompts.forEachIndexed { index, prompt ->
                appendLine("Q: $prompt")
                appendLine("A: ${interviewAnswers.value[index] ?: "No response"}")
                appendLine()
            }
        }
        val updated = current.copy(transcriptText = compiled, reviewedTranscriptText = compiled, updatedAt = System.currentTimeMillis())
        viewModelScope.launch {
            repository.insertSurvey(updated)
            activeSurvey.value = updated
        }
    }

    fun moveToNextInterview() {
        if (currentInterviewIndex.value < interviewPrompts.size - 1) {
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
            // Anonymization: Ensure no names/phones in the export data
            val anonymizedSurvey = current.copy(
                participantName = "[Participant]",
                participantAge = 0, // Age group is preserved in another field
                surveyorName = "[Surveyor]",
                surveyorEmail = "[Redacted]",
                surveyorPhone = "[Redacted]",
                status = "COMPLETED",
                updatedAt = System.currentTimeMillis()
            )
            
            // Generate local files
            val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val jsonFile = File(docsDir, "${current.surveyUid}_anonymized.json")
            val txtFile = File(docsDir, "${current.surveyUid}_report.txt")
            
            val jsonContent = exportSurveyToJson(anonymizedSurvey)
            val txtContent = buildReportText(anonymizedSurvey)
            
            jsonFile.writeText(jsonContent)
            txtFile.writeText(txtContent)
            
            val finalSurvey = anonymizedSurvey.copy(
                jsonExportPath = jsonFile.absolutePath,
                transcriptExportPath = txtFile.absolutePath,
                isSynced = false // Mark for sync
            )
            repository.insertSurvey(finalSurvey)
            
            // Linkage file (Non-anonymized, encrypted - simplified here as local-only)
            // In production, this would be encrypted and stored separately.

            withContext(Dispatchers.Main) {
                activeSurvey.value = finalSurvey
            }
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

    fun startVoiceAnswer() {
        if (audioRecorder.startRecording("CAS_AUDIO_${activeSurveyUid.value}") != null) {
            recordingState.value = "Listening"
            isRecording.value = true
            startTimer()
        }
    }

    fun stopVoiceAnswer(onTranscribed: (String) -> Unit) {
        audioRecorder.stopRecording()
        isRecording.value = false
        stopTimer()
        recordingState.value = "Transcript Ready"
        onTranscribed("Sample local transcription.")
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

    override fun onCleared() {
        super.onCleared()
        ttsHelper.shutdown()
        stopTimer()
    }
}
