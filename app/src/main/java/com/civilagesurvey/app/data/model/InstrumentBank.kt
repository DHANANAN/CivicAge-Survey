package com.civilagesurvey.app.data.model

object InstrumentBank {

    private val sqOop: Instrument
        get() = Instrument(
            code = "SQ-OOP-v1.0",
            title = "Structured Questionnaire - Oldest Old Persons",
            stakeholder = "Oldest Old Persons",
            type = InstrumentType.QUANTITATIVE,
            sections = listOf(
                InstrumentSection(
                    code = "A",
                    title = "Socio-Demographic Profile",
                    questions = listOf(
                        Question("A1", "What is your current age?", InputType.NUMBER, sectionCode = "A", instrumentCode = "SQ-OOP-v1.0"),
                        Question("A2", "What is your gender?", InputType.SINGLE_CHOICE, options = listOf("Male", "Female", "Transgender", "Prefer not to say"), sectionCode = "A", instrumentCode = "SQ-OOP-v1.0"),
                        Question("A3", "What is your mother tongue / primary language?", InputType.TEXT, sectionCode = "A", instrumentCode = "SQ-OOP-v1.0"),
                        Question("A4", "What is the highest level of formal education you have completed?", InputType.SINGLE_CHOICE, options = listOf("No formal education", "Primary only (Class 1-5)", "Middle school (Class 6-8)", "Secondary (Class 9-10)", "Higher Secondary (Class 11-12)", "Graduation or above", "Religious / informal education only"), sectionCode = "A", instrumentCode = "SQ-OOP-v1.0"),
                        Question("A5", "What is your current living arrangement?", InputType.SINGLE_CHOICE, options = listOf("Living alone", "With spouse only", "With adult children / grandchildren", "With extended family (joint household)", "Old Age Home - government", "Old Age Home - private / NGO", "Other"), sectionCode = "A", instrumentCode = "SQ-OOP-v1.0"),
                        Question("A6", "What is your primary source of monthly income?", InputType.SINGLE_CHOICE, options = listOf("Government pension", "Private / employer pension", "Family financial support", "Own savings / interest / rental income", "Government welfare scheme", "No regular income", "Other"), sectionCode = "A", instrumentCode = "SQ-OOP-v1.0")
                    )
                ),
                InstrumentSection(
                    code = "B",
                    title = "Digital Device Ownership and Exposure Profile",
                    questions = listOf(
                        Question("B1", "Do you personally own a mobile phone?", InputType.SINGLE_CHOICE, options = listOf("Yes - basic phone", "Yes - smartphone", "No, I do not own any phone", "Shared household phone"), sectionCode = "B", instrumentCode = "SQ-OOP-v1.0"),
                        Question("B2", "If you have access to any phone, how do you primarily use it?", InputType.MULTI_CHOICE, options = listOf("Making or receiving phone calls", "Sending or receiving SMS", "WhatsApp messaging or calls", "Video calls", "Watching videos", "Government apps", "Banking/UPI", "News/Entertainment", "I do not use any phone"), sectionCode = "B", instrumentCode = "SQ-OOP-v1.0"),
                        Question("B3", "For digital tasks on a phone or computer, who usually operates the device for you?", InputType.SINGLE_CHOICE, options = listOf("I do it all myself", "My spouse", "My adult son or daughter", "My grandchild", "Paid caregiver", "Neighbour or friend", "Volunteer/NGO worker", "Shop owner/Agent", "Does not apply"), sectionCode = "B", instrumentCode = "SQ-OOP-v1.0")
                    )
                ),
                InstrumentSection(
                    code = "C",
                    title = "Awareness and Usage of Smart City Mission Services",
                    questions = listOf(
                        Question("C_GRID", "Smart City Service Usage Grid", InputType.TABLE, sectionCode = "C", instrumentCode = "SQ-OOP-v1.0", citySpecific = true)
                    )
                ),
                InstrumentSection(
                    code = "D",
                    title = "Digital Anxiety Scale",
                    questions = (1..10).map { i ->
                        Question("D$i", getDigitalAnxietyText(i), InputType.LIKERT, options = listOf("Strongly Disagree", "Disagree", "Neither", "Agree", "Strongly Agree"), sectionCode = "D", instrumentCode = "SQ-OOP-v1.0")
                    }
                )
            ),
            consentCode = "CONSENT-OOP-W"
        )

    private val ssiOop: Instrument
        get() = Instrument(
            code = "SSI-OOP-v1.0",
            title = "Semi-Structured Interview - Oldest Old Persons",
            stakeholder = "Oldest Old Persons",
            type = InstrumentType.QUALITATIVE,
            themes = listOf(
                InterviewTheme(
                    code = "T1",
                    title = "Life Before and After Digital Transformation",
                    openingQuestion = "Could you tell me a little about yourself - how long you have lived in this city, and what daily life was like for you here about ten to fifteen years ago?",
                    probes = listOf(
                        "Where did you go to pay your bills, visit a doctor, or collect your pension in those days?",
                        "Were those places easy for you to reach? Did you go alone or with someone?",
                        "What were the things you could do completely independently in those days that gave you a sense of confidence or pride?"
                    )
                ),
                InterviewTheme(
                    code = "T2",
                    title = "Specific Experiences with Smart City Services",
                    openingQuestion = "I would like to hear about specific experiences you have had with city services here. Have you ever tried to use or access city-specific SCM services?",
                    probes = listOf(
                        "Where were you exactly when you tried to use it? Were you alone, or was someone with you?",
                        "At what point did you find it difficult, if you did? What specifically was the difficulty?",
                        "How did you feel at that exact moment?",
                        "Did you have to ask someone for help? Who was it? How did that interaction feel?"
                    )
                )
            ),
            consentCode = "CONSENT-OOP-W"
        )

    private val ssiMohua = Instrument("SSI-MoHUA/SPV-v1.0", "MoHUA Officials and SPV Officers", "Government", InstrumentType.QUALITATIVE)
    private val ssiUp = Instrument("SSI-UP-v1.0", "Urban planners and interface designers", "Planner", InstrumentType.QUALITATIVE)
    private val ssiIccc = Instrument("SSI-ICCC-v1.0", "ICCC operators and technology service providers", "Technology", InstrumentType.QUALITATIVE)
    private val fiMco = Instrument("FI-MCO-v1.0", "Municipal ward-level officials", "Government", InstrumentType.QUALITATIVE)
    private val ssiOahAdmin = Instrument("SSI-OAH-Admin-v1.0", "Old age home administrators", "OAH", InstrumentType.MIXED)
    private val ssiDlt = Instrument("SSI-DLT-v1.0", "Digital literacy trainers", "DLT", InstrumentType.MIXED)
    private val ssiNgo = Instrument("SSI-NGO-v1.0", "NGOs and civil society", "NGO", InstrumentType.QUALITATIVE)
    private val seiGeri = Instrument("SEI-Geri-v1.0", "Geriatricians and mental health professionals", "Geriatricians", InstrumentType.QUALITATIVE)
    private val eiLegalAc = Instrument("EI-Legal-Ac-v1.0", "Constitutional law and elder rights scholars", "Legal", InstrumentType.QUALITATIVE)
    private val sqCgSsiCg = Instrument("SQ-CG + SSI-CG-v1.0", "Caregivers and family members", "Caregivers", InstrumentType.MIXED)
    private val foOop = Instrument("FO-OOP-v1.0", "Field observation and shadowing", "OOP", InstrumentType.OBSERVATIONAL)
    private val fgdOopCg = Instrument("FGD-OOP/CG-v1.0", "Focus group discussion", "OOP+CG", InstrumentType.QUALITATIVE)
    private val doctrinal = Instrument("DOCTRINAL-v1.0", "Legal and policy document analysis", "PI/RA", InstrumentType.DOCTRINAL)
    private val qaTools = Instrument("QA-v1.0", "Quality assurance checklists", "All", InstrumentType.ADMINISTRATIVE)

    val instruments = listOf(
        sqOop,
        ssiOop,
        ssiMohua,
        ssiUp,
        ssiIccc,
        fiMco,
        ssiOahAdmin,
        ssiDlt,
        ssiNgo,
        seiGeri,
        eiLegalAc,
        sqCgSsiCg,
        foOop,
        fgdOopCg,
        doctrinal,
        qaTools
    )

    private fun getDigitalAnxietyText(index: Int): String = when (index) {
        1 -> "I feel nervous or worried when I am asked to use a government app, website, or digital kiosk to do something."
        2 -> "I am afraid that I will make a mistake and something bad will happen - like losing money or important data - when I use digital services."
        3 -> "When I am not able to complete a task on a digital device, I feel ashamed or embarrassed in front of others."
        4 -> "I have stopped going to certain public places or stopped trying to access certain services because I cannot use the digital systems required."
        5 -> "I feel that the new digital systems in this city were not designed with people like me in mind."
        6 -> "When a family member or neighbour has to help me complete a digital task, I feel like a burden to them."
        7 -> "I feel more disconnected from city life than I did five years ago, because so many things have moved to apps and online systems."
        8 -> "I feel angry or frustrated when a service I used to access easily at an office has moved to an app or online-only system."
        9 -> "Thinking about having to use a touchscreen or digital device for something important makes me feel stressed."
        10 -> "I feel that my ability to live independently in this city has been reduced by digital systems that I cannot use."
        else -> ""
    }
}
