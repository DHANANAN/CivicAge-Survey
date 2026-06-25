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
                        Question("A6", "What is your primary source of monthly income?", InputType.SINGLE_CHOICE, options = listOf("Government pension", "Private / employer pension", "Family financial support", "Own savings / interest / rental income", "Government welfare scheme", "No regular income", "Other"), sectionCode = "A", instrumentCode = "SQ-OOP-v1.0"),
                        Question("A7", "What is your estimated total monthly household income (including all sources)?", InputType.SINGLE_CHOICE, options = listOf("Below Rs. 5,000", "Rs. 5,001 to Rs. 10,000", "Rs. 10,001 to Rs. 20,000", "Rs. 20,001 to Rs. 50,000", "Above Rs. 50,000", "Don't know / Prefer not to say"), sectionCode = "A", instrumentCode = "SQ-OOP-v1.0"),
                        Question("A8", "How long have you been living in this city?", InputType.SINGLE_CHOICE, options = listOf("Less than 5 years", "5 to 15 years", "15 to 30 years", "More than 30 years", "Born and raised here"), sectionCode = "A", instrumentCode = "SQ-OOP-v1.0"),
                        Question("A9", "How would you describe your general health these days?", InputType.SINGLE_CHOICE, options = listOf("1 = Very good", "2 = Good", "3 = Fair / moderate", "4 = Poor", "5 = Very poor"), sectionCode = "A", instrumentCode = "SQ-OOP-v1.0"),
                        Question("A10", "Do you currently experience any of the following? (Select all that apply)", InputType.MULTI_CHOICE, options = listOf("Difficulty seeing (even with spectacles)", "Difficulty hearing (even with hearing aid)", "Hand tremors or difficulty with fine finger movements", "Memory difficulties or frequent forgetfulness", "Difficulty walking or limited mobility", "Chronic pain affecting daily activities", "None of the above"), sectionCode = "A", instrumentCode = "SQ-OOP-v1.0"),
                        Question("A11", "Do you currently receive regular caregiving support from a paid or unpaid caregiver?", InputType.SINGLE_CHOICE, options = listOf("Yes - full-time paid caregiver", "Yes - part-time paid caregiver", "Yes - unpaid family member as caregiver", "No - I manage independently", "Institutional care only (old age home staff)"), sectionCode = "A", instrumentCode = "SQ-OOP-v1.0")
                    )
                ),
                InstrumentSection(
                    code = "B",
                    title = "Digital Device Ownership and Exposure Profile",
                    questions = listOf(
                        Question("B1", "Do you personally own a mobile phone?", InputType.SINGLE_CHOICE, options = listOf("Yes - basic phone", "Yes - smartphone", "No, I do not own any phone", "Shared household phone"), sectionCode = "B", instrumentCode = "SQ-OOP-v1.0"),
                        Question("B2", "If you have access to any phone, how do you primarily use it?", InputType.MULTI_CHOICE, options = listOf("Making or receiving phone calls", "Sending or receiving SMS", "WhatsApp messaging or calls", "Video calls", "Watching videos", "Government apps", "Banking/UPI", "News/Entertainment", "I do not use any phone"), sectionCode = "B", instrumentCode = "SQ-OOP-v1.0"),
                        Question("B3", "For digital tasks on a phone or computer, who usually operates the device for you?", InputType.SINGLE_CHOICE, options = listOf("I do it all myself", "My spouse", "My adult son or daughter", "My grandchild", "Paid caregiver", "Neighbour or friend", "Volunteer/NGO worker", "Shop owner/Agent", "Does not apply"), sectionCode = "B", instrumentCode = "SQ-OOP-v1.0"),
                        Question("B4", "Do you have access to a computer, laptop, or tablet at home?", InputType.SINGLE_CHOICE, options = listOf("Yes - I use it myself independently", "Yes - but only other family members use it", "No access to these devices at home", "Sometimes use at a cyber cafe or library"), sectionCode = "B", instrumentCode = "SQ-OOP-v1.0"),
                        Question("B5", "How often do you PERSONALLY attempt to use any digital device - phone, computer, digital kiosk - without asking for help first?", InputType.SINGLE_CHOICE, options = listOf("Every day", "A few times a week (3 to 5 times)", "Once a week", "A few times a month", "Rarely - once in a few months", "Never - I always ask for help or avoid it"), sectionCode = "B", instrumentCode = "SQ-OOP-v1.0"),
                        Question("B6", "Before your retirement or in your working life, did you ever regularly use any digital device or technology?", InputType.SINGLE_CHOICE, options = listOf("Yes - regularly used computer or internet at work", "Yes - used basic technology (early mobile phone, desktop)", "Yes - some limited use", "No - I have never used digital devices in my life"), sectionCode = "B", instrumentCode = "SQ-OOP-v1.0"),
                        Question("B7", "On a scale of 1 to 5, how confident do you feel right now in using a smartphone or a digital kiosk to complete a simple task?", InputType.SINGLE_CHOICE, options = listOf("1 = Not at all confident", "2 = Mostly not confident", "3 = Somewhat confident", "4 = Fairly confident", "5 = Very confident"), sectionCode = "B", instrumentCode = "SQ-OOP-v1.0"),
                        Question("B8", "Is there a working internet connection available in your home or old age home?", InputType.SINGLE_CHOICE, options = listOf("Yes - broadband or Wi-Fi connection", "Yes - mobile data connection", "Sometimes - unreliable connection", "No internet access at all"), sectionCode = "B", instrumentCode = "SQ-OOP-v1.0")
                    )
                ),
                InstrumentSection(
                    code = "C",
                    title = "Awareness and Usage of Smart City Mission Services",
                    questions = listOf(
                        Question("C_GRID", "Smart City Service Usage Grid", InputType.TABLE, sectionCode = "C", instrumentCode = "SQ-OOP-v1.0", citySpecific = true),
                        Question("C9", "For the services you TRIED but could NOT use successfully, what was the PRIMARY reason? (Select up to 3)", InputType.MULTI_CHOICE, options = listOf("Did not know how to use the device or app", "Did not have the required device", "Service was only available online - no offline option", "Language barrier - interface not in my language", "Screen was too small to read clearly", "Could not understand the steps", "Afraid of making a mistake with money or data", "No one available nearby to help me", "Physical difficulty (tremors, vision, hearing)", "Long queue at the kiosk or service point", "Required OTP on a phone I cannot operate", "Service point was not accessible in my area", "Other"), sectionCode = "C", instrumentCode = "SQ-OOP-v1.0"),
                        Question("C10", "In the PAST ONE MONTH, how many times did you need someone else’s help to access a government or civic service that you believe you should have been able to access independently?", InputType.SINGLE_CHOICE, options = listOf("0 times - I managed fully on my own", "1 to 2 times", "3 to 5 times", "6 to 10 times", "More than 10 times"), sectionCode = "C", instrumentCode = "SQ-OOP-v1.0"),
                        Question("C11", "Have you ever been told by a government office or service provider that a service is ‘only available online’ when you needed it, and you were unable to access it at that time?", InputType.SINGLE_CHOICE, options = listOf("Yes - this has happened more than once", "Yes - this happened once", "No - this has never happened to me", "Don't know / Don't remember"), sectionCode = "C", instrumentCode = "SQ-OOP-v1.0"),
                        Question("C12", "If YES to C11 - what did you do after that? (Select all that apply)", InputType.MULTI_CHOICE, options = listOf("Gave up and went without the service", "Went home and asked a family member to help", "Hired a local agent or middleman to do it for me", "Went to a cyber cafe and paid for help", "Found an alternative physical office or method", "Filed a formal complaint", "Other"), sectionCode = "C", instrumentCode = "SQ-OOP-v1.0"),
                        Question("C13", "In the PAST SIX MONTHS, have you missed or significantly delayed any of the following because of digital access difficulties? (Select all that apply)", InputType.MULTI_CHOICE, options = listOf("Medical appointment (could not book online)", "Government benefit or pension (online process incomplete)", "Utility payment (online-only - led to default or penalty)", "Travel or public transport (could not use digital ticketing)", "Accessing a public park or facility (QR or app required for entry)", "Government scheme registration", "None of these situations have occurred", "Other"), sectionCode = "C", instrumentCode = "SQ-OOP-v1.0"),
                        Question("C14", "Have you stopped visiting or using any public place or civic facility in this city because it requires a digital device or app that you cannot use?", InputType.SINGLE_CHOICE, options = listOf("Yes - I have stopped going to specific places", "No - I still go everywhere I want to", "I still go but always ask someone for help at the digital gate"), sectionCode = "C", instrumentCode = "SQ-OOP-v1.0"),
                        Question("C15", "In the past 6 months, did you pay money to a middleman, agent, or cyber cafe operator to complete a government or civic task that should have been free to access directly?", InputType.SINGLE_CHOICE, options = listOf("Yes - I paid once", "Yes - I paid more than once", "No - I have not paid anyone for this", "Don't remember"), sectionCode = "C", instrumentCode = "SQ-OOP-v1.0")
                    )
                ),
                InstrumentSection(
                    code = "D",
                    title = "Digital Anxiety Scale",
                    questions = (1..10).map { i ->
                        Question("D$i", getDigitalAnxietyText(i), InputType.LIKERT, options = listOf("Strongly Disagree", "Disagree", "Neither", "Agree", "Strongly Agree"), sectionCode = "D", instrumentCode = "SQ-OOP-v1.0")
                    }
                ),
                InstrumentSection(
                    code = "E1",
                    title = "Digital Dependency Frequency Index",
                    questions = listOf(
                        Question("E1", "In a typical week, for how many different tasks or services requiring digital access do you need someone else’s help?", InputType.SINGLE_CHOICE, options = listOf("0 - I manage fully independently", "1 to 2 tasks per week", "3 to 5 tasks per week", "More than 5 tasks per week", "Every single task requiring digital access"), sectionCode = "E1", instrumentCode = "SQ-OOP-v1.0"),
                        Question("E2", "Who do you MOST OFTEN turn to for digital help?", InputType.SINGLE_CHOICE, options = listOf("My spouse", "My adult son or daughter", "My grandchild", "Paid caregiver or domestic helper", "Neighbour or friend", "Local shop owner or agent", "NGO volunteer or social worker", "Old age home staff member", "No one - I attempt alone or simply give up"), sectionCode = "E1", instrumentCode = "SQ-OOP-v1.0"),
                        Question("E3", "Have you MISSED or DELAYED a medical appointment in the past 6 months because you could not book it online or through an app?", InputType.SINGLE_CHOICE, options = listOf("Yes - this happened more than once", "Yes - this happened once", "No - this has not happened to me", "I do not need to book medical appointments online"), sectionCode = "E1", instrumentCode = "SQ-OOP-v1.0"),
                        Question("E4", "Have you MISSED receiving a government benefit, pension payment, or welfare scheme entitlement in the past 12 months because you could not complete an online process?", InputType.SINGLE_CHOICE, options = listOf("Yes - happened more than once", "Yes - happened once", "No", "Not aware of any such schemes I should be enrolled in", "Someone else manages all this for me"), sectionCode = "E1", instrumentCode = "SQ-OOP-v1.0"),
                        Question("E5", "In your own assessment, are you MORE DEPENDENT on other people for daily activities now than you were 5 years ago?", InputType.SINGLE_CHOICE, options = listOf("Much more dependent on others now", "Somewhat more dependent now", "About the same as 5 years ago", "More independent now than before"), sectionCode = "E1", instrumentCode = "SQ-OOP-v1.0"),
                        Question("E6", "If you said MORE DEPENDENT (E5): to what extent have digital systems in the city contributed to this increased dependence?", InputType.SINGLE_CHOICE, options = listOf("Digital systems are a MAJOR cause of my increased dependence", "Digital systems are a MINOR cause - health is the bigger factor", "Digital systems are NOT a cause - it is entirely about health", "Not applicable - I am not more dependent"), sectionCode = "E1", instrumentCode = "SQ-OOP-v1.0")
                    )
                ),
                InstrumentSection(
                    code = "E2",
                    title = "Dignity Perception Scale",
                    questions = (7..12).map { i ->
                        Question("E$i", getDignityPerceptionText(i), InputType.LIKERT, options = listOf("Strongly Disagree", "Disagree", "Neither", "Agree", "Strongly Agree"), sectionCode = "E2", instrumentCode = "SQ-OOP-v1.0")
                    }
                ),
                InstrumentSection(
                    code = "F",
                    title = "Social Isolation and Intergenerational Impact",
                    questions = listOf(
                        Question("F1", "How often do you leave your home - or the old age home premises - to visit any public place such as a market, park, temple or mosque or church, government office, or hospital?", InputType.SINGLE_CHOICE, options = listOf("Every day", "3 to 5 times per week", "Once a week", "Once or twice a month", "Rarely - once in a few months", "Almost never"), sectionCode = "F", instrumentCode = "SQ-OOP-v1.0"),
                        Question("F2", "Compared to 5 years ago, how has your frequency of going to public places in the city changed?", InputType.SINGLE_CHOICE, options = listOf("I go out much more often now", "About the same as before", "I go out somewhat less often now", "I go out much less often now"), sectionCode = "F", instrumentCode = "SQ-OOP-v1.0"),
                        Question("F3", "Do you feel lonely or isolated from the life of the city around you?", InputType.SINGLE_CHOICE, options = listOf("Often / frequently", "Sometimes", "Rarely", "Never"), sectionCode = "F", instrumentCode = "SQ-OOP-v1.0"),
                        Question("F4", "Do you feel that the gap between how you use technology and how your children or grandchildren use it has affected your relationship with them?", InputType.SINGLE_CHOICE, options = listOf("Yes - it has created noticeable distance or sometimes causes conflict", "Yes - they are sometimes impatient with me", "Somewhat - very minor effect on the relationship", "No - it has not affected our relationship at all", "I have no children or grandchildren"), sectionCode = "F", instrumentCode = "SQ-OOP-v1.0"),
                        Question("F5", "Are there other older people your age in your neighbourhood or this old age home who face similar difficulties with digital systems in this city?", InputType.SINGLE_CHOICE, options = listOf("Yes - many of us face the same situation", "Yes - a few others face it", "I am not sure", "No - most other older people I know manage fine"), sectionCode = "F", instrumentCode = "SQ-OOP-v1.0")
                    )
                ),
                InstrumentSection(
                    code = "G",
                    title = "Digital Literacy Programme Exposure and Awareness of Welfare Schemes",
                    questions = listOf(
                        Question("G1", "Have you ever attended a digital literacy programme, class, or training session organized by anyone?", InputType.SINGLE_CHOICE, options = listOf("Yes - government programme", "Yes - NGO or private organization programme", "Yes - a family member informally taught me", "No - I have never attended any such training"), sectionCode = "G", instrumentCode = "SQ-OOP-v1.0"),
                        Question("G2", "After attending the training, are you now able to independently do any of the following? (Select all that apply)", InputType.MULTI_CHOICE, options = listOf("Make or receive phone calls independently", "Send WhatsApp messages independently", "Use a government app or online portal independently", "Make a digital payment independently", "Book an appointment or avail a service online independently", "None - the training did not help enough", "Not applicable"), sectionCode = "G", instrumentCode = "SQ-OOP-v1.0"),
                        Question("G3", "Are you aware of the Atal Vayo Abhyudaya Yojana or any other government scheme specifically for older persons in your city?", InputType.SINGLE_CHOICE, options = listOf("Yes - I am aware and currently enrolled", "Yes - I am aware but not enrolled", "I have heard of it but do not know the details", "No - I have never heard of this scheme"), sectionCode = "G", instrumentCode = "SQ-OOP-v1.0"),
                        Question("G4", "Have you ever tried to access your government welfare benefits or schemes through their online portals or apps?", InputType.SINGLE_CHOICE, options = listOf("Yes - I did it successfully on my own", "Yes - I did it with a family member's help", "Yes - I did it through a paid agent or middleman", "Yes - I tried but failed to complete the process", "No - I have never tried; I do not know how", "No - I have never tried; there was no need"), sectionCode = "G", instrumentCode = "SQ-OOP-v1.0"),
                        Question("G5", "Do you feel that digital literacy programmes available in this city are designed specifically for people your age and your level of technology experience?", InputType.SINGLE_CHOICE, options = listOf("Yes - they are well-designed for people like me", "Somewhat - they could be improved for older people", "No - they are too fast-paced and not suitable for me", "No - I have no access to any such programme", "I have not attended any to judge"), sectionCode = "G", instrumentCode = "SQ-OOP-v1.0")
                    )
                ),
                InstrumentSection(
                    code = "H",
                    title = "Field Investigator Observation Log",
                    questions = listOf(
                        Question("H1", "Participant’s apparent reading ability based on observation during session", InputType.SINGLE_CHOICE, options = listOf("Reads confidently and independently", "Reads but with some visible difficulty", "Could not read - relied entirely on oral administration", "Unable to assess accurately"), sectionCode = "H", instrumentCode = "SQ-OOP-v1.0"),
                        Question("H2", "Visual impairment indicators observed during session", InputType.SINGLE_CHOICE, options = listOf("None observed - no apparent vision difficulty", "Squinting or holding printed card very close or very far", "Participant requested larger print or brighter light", "Appeared to struggle significantly with visual material"), sectionCode = "H", instrumentCode = "SQ-OOP-v1.0"),
                        Question("H3", "Hand tremors or motor difficulty observed", InputType.SINGLE_CHOICE, options = listOf("None observed", "Mild - some difficulty holding pen or pointing accurately", "Moderate - had significant difficulty marking or pointing", "Severe - FI had to record all responses from verbal only"), sectionCode = "H", instrumentCode = "SQ-OOP-v1.0"),
                        Question("H4", "Overall comprehension level during session", InputType.SINGLE_CHOICE, options = listOf("Understood all questions without needing re-explanation", "Required re-explanation of some questions", "Frequently required simplified re-wording throughout", "Serious comprehension concerns - flag for PI review immediately"), sectionCode = "H", instrumentCode = "SQ-OOP-v1.0"),
                        Question("H5", "Was anyone else present during any part of the session?", InputType.SINGLE_CHOICE, options = listOf("No - session was fully private throughout", "Yes - briefly present", "Yes - present for significant part of session"), sectionCode = "H", instrumentCode = "SQ-OOP-v1.0"),
                        Question("H6", "Signs of emotional distress, anxiety, or strong emotional response during session", InputType.SINGLE_CHOICE, options = listOf("None observed", "Mild - slight visible discomfort on specific questions", "Moderate - long pause, visible tears, or agitation at some point", "Significant distress - session modified or stopped early"), sectionCode = "H", instrumentCode = "SQ-OOP-v1.0"),
                        Question("H7", "Session completion status", InputType.SINGLE_CHOICE, options = listOf("Fully completed - all sections done", "Partially completed", "Terminated early by participant", "Terminated by FI"), sectionCode = "H", instrumentCode = "SQ-OOP-v1.0"),
                        Question("H8", "FI’s overall assessment of data reliability from this session", InputType.SINGLE_CHOICE, options = listOf("High quality - confident responses throughout; data reliable", "Moderate quality - some reliability concerns noted", "Low quality - significant concerns; flag for PI decision on inclusion"), sectionCode = "H", instrumentCode = "SQ-OOP-v1.0"),
                        Question("H9", "Contextual notes: neighbourhood type, physical setting, any other relevant observations", InputType.FREE_TEXT, sectionCode = "H", instrumentCode = "SQ-OOP-v1.0")
                    )
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
                ),
                InterviewTheme(
                    code = "T3",
                    title = "Emotional Experience, Digital Anxiety, and Mental Well-being",
                    openingQuestion = "I would like to ask you now about how all of this makes you feel - emotionally. How do you feel when you are unable to complete something that your children, grandchildren, or younger neighbours can easily do on their phones?",
                    probes = listOf(
                        "Can you describe that feeling in more detail - is it frustration? Sadness? Embarrassment? Helplessness? Something else?",
                        "Is there a particular incident that stands out in your memory where you felt especially distressed because of a digital barrier?",
                        "Have you ever felt afraid of making a serious mistake - like losing money - while using a device for something important?",
                        "Do you ever feel that you are somehow ‘too old’ for this city now? What does that mean to you when you feel that?",
                        "Has any of this affected your sleep, your appetite, or your overall mood or emotional state?",
                        "Some older people we have spoken with told us that they now feel they need permission or help from family members to do things they used to do on their own. Is that true for you? How does that feel?",
                        "Do you feel like a burden to your family or caregivers when you need help with digital tasks?",
                        "Has your sense of independence changed over recent years? In what way, and what has caused that change in your view?",
                        "How do you cope emotionally when something goes wrong digitally - do you have any way of managing that feeling?",
                        "Some people told us they have started avoiding going to certain places because of digital barriers. Has that happened to you? Tell me about that."
                    )
                ),
                InterviewTheme(
                    code = "T4",
                    title = "Social Connections, Family Dynamics, and Intergenerational Bonds",
                    openingQuestion = "Let us talk about your relationships - with your family, your neighbours, and other people around you. Who do you usually turn to when you need help with something digital? How often does that happen?",
                    probes = listOf(
                        "How does that person typically respond when you ask for help - are they patient, impatient, or indifferent?",
                        "Have there been times when you felt reluctant to ask for help again from the same person? Why?",
                        "Do you feel the technology gap between you and your younger family members has changed the way they interact with you, or how they see you?",
                        "Are there other older people your age in your neighbourhood or old age home who face similar difficulties? Do you discuss this among yourselves?",
                        "Do younger neighbours, volunteers, or community members ever help older people in your area with digital tasks?"
                    ),
                    citySpecificProbes = mapOf(
                        "AZ" to listOf("In Mizoram, community bonds through the church and YMA are known to be very strong. Does your church community or local council actively help older members navigate city services, including digital ones?"),
                        "SG" to listOf("Do mohalla committees or mosque-based community structures in your area help older residents access government services?"),
                        "ND" to listOf("This city has a very large population of elderly migrants from other states. Do you feel connected to any community network from your place of origin that helps you here?")
                    )
                ),
                InterviewTheme(
                    code = "T5",
                    title = "Rights Awareness, Dignity, and Policy - Constitutional and Legal Consciousness",
                    openingQuestion = "Do you feel that as an older citizen - someone who has contributed to this country and this city - you have the right to access all city services in a way that works for you, including digital services?",
                    probes = listOf(
                        "Do you think the government has a responsibility to make sure that city services are accessible to people who cannot use digital devices?",
                        "Have you ever made a formal complaint about being excluded from a service because of a digital barrier? To whom? What happened?",
                        "Are you aware of any law or government policy that specifically protects the rights and welfare of older persons in India?",
                        "Has any government official, social worker, or NGO representative ever explained your rights to you as an older person?",
                        "Have you heard of the Maintenance and Welfare of Parents and Senior Citizens Act?",
                        "Do you know what rights that law gives you?",
                        "Do you think that law should be updated to also protect older people from being excluded by digital city infrastructure and services?"
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

    private fun getDignityPerceptionText(index: Int): String = when (index) {
        7 -> "I feel that this city respects my ability to participate in public life independently, as an older person."
        8 -> "I feel that my personal dignity is affected when I cannot access services that others access easily through phones or apps."
        9 -> "I feel the government has taken adequate steps to ensure that older people like me are not left behind by new technology."
        10 -> "Having to constantly depend on others for tasks I should be able to do myself makes me feel less respected as a person."
        11 -> "I feel that digital systems in this city have taken away some of my freedom and independence."
        12 -> "I feel that the people who design city services understand the needs and limitations of older persons like me."
        else -> ""
    }
}
