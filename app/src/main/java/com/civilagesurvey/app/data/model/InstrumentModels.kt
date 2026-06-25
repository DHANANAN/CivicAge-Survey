package com.civilagesurvey.app.data.model

data class Instrument(
    val code: String,
    val title: String,
    val stakeholder: String,
    val type: InstrumentType,
    val sections: List<InstrumentSection> = emptyList(),
    val themes: List<InterviewTheme> = emptyList(),
    val consentCode: String? = null
)

enum class InstrumentType {
    QUANTITATIVE,
    QUALITATIVE,
    MIXED,
    OBSERVATIONAL,
    DOCTRINAL,
    ADMINISTRATIVE
}

data class InstrumentSection(
    val code: String,
    val title: String,
    val questions: List<Question>
)

data class Question(
    val id: String,
    val text: String,
    val inputType: InputType,
    val options: List<String> = emptyList(),
    val isRequired: Boolean = true,
    val isSkippable: Boolean = true,
    val sectionCode: String,
    val instrumentCode: String,
    val citySpecific: Boolean = false,
    val hint: String? = null
)

enum class InputType {
    NUMBER,
    TEXT,
    SINGLE_CHOICE,
    MULTI_CHOICE,
    LIKERT,
    FREE_TEXT,
    DATE,
    TIME,
    TABLE,
    SIGNATURE,
    THUMB_IMPRESSION,
    AUDIO_ASSENT
}

data class InterviewTheme(
    val code: String,
    val title: String,
    val openingQuestion: String,
    val probes: List<String>,
    val citySpecificProbes: Map<String, List<String>> = emptyMap(),
    val fieldInvestigatorNote: String? = null,
    val isCompulsory: Boolean = false
)
