package com.civilagesurvey.app.data.model

import com.civilagesurvey.app.data.database.FormQuestionEntity

object DefaultQuestions {
    const val DEFAULT_FORM_ID = "CAS-DEFAULT-001"

    val list = listOf(
        FormQuestionEntity(
            questionId = "Q1",
            formId = DEFAULT_FORM_ID,
            section = "Mobility",
            questionText = "Do you find the local pavements easy to navigate with a walker or cane?",
            type = "single_choice",
            optionsJson = "[\"Very Easy\", \"Somewhat Easy\", \"Difficult\", \"Impossible\"]",
            isRequired = true
        ),
        FormQuestionEntity(
            questionId = "Q2",
            formId = DEFAULT_FORM_ID,
            section = "Safety",
            questionText = "How safe do you feel walking in your neighborhood after sunset?",
            type = "single_choice",
            optionsJson = "[\"Very Safe\", \"Somewhat Safe\", \"Unsafe\", \"Very Unsafe\"]",
            isRequired = true
        ),
        FormQuestionEntity(
            questionId = "Q3",
            formId = DEFAULT_FORM_ID,
            section = "Public Transport",
            questionText = "How often do you use public transport (bus/train)?",
            type = "single_choice",
            optionsJson = "[\"Daily\", \"Weekly\", \"Rarely\", \"Never\"]",
            isRequired = true
        ),
        FormQuestionEntity(
            questionId = "Q4",
            formId = DEFAULT_FORM_ID,
            section = "Digital Access",
            questionText = "Do you use any digital apps for healthcare or banking?",
            type = "single_choice",
            optionsJson = "[\"Yes, independently\", \"Yes, with help\", \"No\", \"Not interested\"]",
            isRequired = true
        )
    )
}
