package com.example.data.model

import com.example.data.database.FormQuestionEntity

object DefaultQuestions {
    const val DEFAULT_FORM_ID = "URBAN_WELLBEING_V1"

    val list = listOf(
        FormQuestionEntity(
            questionId = "Q1",
            formId = DEFAULT_FORM_ID,
            section = "Mobility and Walkability",
            questionText = "How safe do you feel walking alone in your neighborhood during the day?",
            type = "single_choice",
            optionsJson = "[\"Very safe\", \"Somewhat safe\", \"Difficult / Feeling unsafe\", \"Extremely unsafe\"]",
            isRequired = true
        ),
        FormQuestionEntity(
            questionId = "Q2",
            formId = DEFAULT_FORM_ID,
            section = "Mobility and Walkability",
            questionText = "Are the pavements/sidewalks in your neighborhood easy for seniors to use without tripping?",
            type = "single_choice",
            optionsJson = "[\"Yes, very even & flat\", \"Somewhat even but has broken steps\", \"No, broken and blocked\", \"Not applicable\"]",
            isRequired = true
        ),
        FormQuestionEntity(
            questionId = "Q3",
            formId = DEFAULT_FORM_ID,
            section = "Healthcare Access",
            questionText = "How easily can you travel to your preferred healthcare center or hospital?",
            type = "single_choice",
            optionsJson = "[\"Very easy (within walking distance)\", \"Somewhat easy (short auto/bus lift)\", \"Difficult (long travel, steep climbs)\", \"Extremely difficult\"]",
            isRequired = true
        ),
        FormQuestionEntity(
            questionId = "Q4",
            formId = DEFAULT_FORM_ID,
            section = "Public Spaces and Social Wellbeing",
            questionText = "Are there senior-friendly seating areas or parks nearby where you can relax and talk with others?",
            type = "single_choice",
            optionsJson = "[\"Yes, ample benches and green spots\", \"Benches are available but in poor state\", \"No welcoming parks or benches exist\", \"I can rarely leave the house\"]",
            isRequired = true
        ),
        FormQuestionEntity(
            questionId = "Q5",
            formId = DEFAULT_FORM_ID,
            section = "Smart Utility & Digital Systems",
            questionText = "How comfortable do you feel accessing public information or smart utility services via smartphone?",
            type = "single_choice",
            optionsJson = "[\"Very comfortable (independent)\", \"Somewhat comfortable (need minor guidance)\", \"Struggling / Need help from surveyor\", \"Never used smartphone utility\"]",
            isRequired = true
        ),
        FormQuestionEntity(
            questionId = "Q6",
            formId = DEFAULT_FORM_ID,
            section = "Loneliness and Community Support",
            questionText = "How often do you feel lonely or isolated from community social activities?",
            type = "single_choice",
            optionsJson = "[\"Rarely or never\", \"Occasionally (some weeks)\", \"Often / Regular feeling\", \"Constantly, feeling completely disconnected\"]",
            isRequired = true
        )
    )
}
