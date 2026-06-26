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

    private val ssiMohua = Instrument(
        code = "SSI-MoHUA/SPV-v1.0",
        title = "MoHUA Officials and SPV Officers",
        stakeholder = "Government",
        type = InstrumentType.QUALITATIVE,
        themes = listOf(
            InterviewTheme(
                code = "T1",
                title = "Design Philosophy and Stakeholder Consultation",
                openingQuestion = "Could you begin by walking us through how a new Smart City project is conceptualized, designed, and approved in your city or ministry - from the initial idea through to implementation?",
                probes = listOf(
                    "At which specific stages of this process are end-users consulted? Who formally represents elderly or differently-abled residents in these consultations?",
                    "Is there a mandatory checklist or formal criterion related to accessibility or universal design that every project must satisfy before approval?",
                    "Has the age profile of potential users ever been a formal input into any project's design specifications? Can you give a concrete example from your city?",
                    "Are you aware of any SCM project in your city that was modified or retrofitted after deployment because elderly users found it inaccessible? What was the trigger for that change?",
                    "Does your SPV or Ministry have any specific standard operating procedure related to universal design, age-friendly design, or digital accessibility?"
                )
            ),
            InterviewTheme(
                code = "T2",
                title = "Understanding and Data on Digital Exclusion",
                openingQuestion = "From your professional observation over the years, how do elderly residents - particularly those above 75 years of age - interact with the digital services introduced under the Smart Cities Mission in your city?",
                probes = listOf(
                    "Has your office or city ever conducted a formal user survey or needs assessment specifically focused on elderly residents and their digital access? If yes, what did it reveal?",
                    "Do you receive complaints or grievances from elderly residents about their inability to access digital services? How are those handled institutionally - what is the process?",
                    "Does your ICCC or any city-level data system capture age-disaggregated usage data for any of your digital services? If yes, would you share that data formally with our research?",
                    "In your professional assessment, what proportion of your city's oldest residents - those between 75 and 85 years - can independently and successfully use the key digital services you have deployed?",
                    "What does 'digital inclusion' mean, in practical terms, for a 78-year-old who grew up and worked entirely before computers existed? Is that definition reflected anywhere in how your city currently plans its services?"
                )
            ),
            InterviewTheme(
                code = "T3",
                title = "Legal and Policy Framework - Convergence",
                openingQuestion = "Smart City Mission operates alongside several other policy frameworks, including the National Policy for Older Persons 2011, the Maintenance and Welfare of Parents and Senior Citizens Act 2007, and the Atal Vayo Abhyudaya Yojana. How does your city or ministry coordinate with these frameworks in practice?",
                probes = listOf(
                    "Is there any formal convergence mechanism between the Ministry of Housing and Urban Affairs and the Ministry of Social Justice and Empowerment on the specific issue of elderly digital inclusion in smart cities?",
                    "Are you aware of the constitutional mandate under Article 21 (right to life with dignity) and Article 41 (right to public assistance) as they may apply to digital access for elderly citizens? How does SCM policy currently engage with these constitutional provisions?",
                    "Has any court order or legal challenge ever influenced the design or modification of a smart city digital service to be more accessible? Are you aware of any such case?",
                    "Do you think the current statutory definition of 'senior citizen' in Indian law is adequate to capture the distinct vulnerabilities of the oldest old population (75-85 years) in tech-dense urban environments?",
                    "Is there any existing policy mechanism through which an oldest old resident who cannot access a digital service could realistically seek redressal?"
                )
            ),
            InterviewTheme(
                code = "T4",
                title = "Monitoring, Accountability, and Data Systems",
                openingQuestion = "What mechanisms exist within your city or ministry to systematically know whether smart city services are actually reaching and being effectively used by all population groups, including the oldest old?",
                probes = listOf(
                    "Is there any systematic monitoring of service usage disaggregated by age or ability? If yes, what does that data show?",
                    "What happens institutionally when a project is found to be functionally excluding a specific population group?",
                    "What data would you need - that you currently do not have - to make genuinely age-inclusive design decisions for your city's SCM services?",
                    "Would you be willing to share the following with our research formally? [Note response]: city-level SCM project completion reports, user satisfaction surveys, ICCC dashboard or usage data, age-disaggregated interaction data, design specifications for key completed projects."
                )
            ),
            InterviewTheme(
                code = "T5",
                title = "Forward Vision - Age-Centric Redesign",
                openingQuestion = "If you were given a specific mandate and adequate resources to redesign your city's SCM projects with the needs of the oldest old population - those between 75 and 85 - as the primary design criterion, what would change most significantly?",
                probes = listOf(
                    "What institutional, financial, or political barriers currently prevent more age-inclusive design in Smart City projects?",
                    "Are you aware of any international Smart City model - whether in Europe, East Asia, or elsewhere - that has addressed elderly digital inclusion effectively? What specific lessons do you think are applicable to the Indian context?",
                    "What single policy change at the central government level - whether a funding condition, a mandatory guideline, or a legislative amendment - would most effectively drive cities toward age-inclusive digital infrastructure design?",
                    "Would you be willing to review the policy recommendations from this research before publication and provide feedback?"
                )
            )
        ),
        consentCode = "CONSENT-KI"
    )

    private val ssiUp = Instrument(
        code = "SSI-UP-v1.0",
        title = "Urban Planners and Interface Designers",
        stakeholder = "Planner",
        type = InstrumentType.QUALITATIVE,
        themes = listOf(
            InterviewTheme(
                code = "T1",
                title = "Accessibility and Heuristic Usability",
                openingQuestion = "Please start by telling me about your professional role and which specific SCM projects in your city you have been involved in designing or implementing.",
                probes = listOf(
                    "What were the stated user requirements when you began designing the project? Who defined those requirements?",
                    "Were there any explicit accessibility standards - Indian or international, such as WCAG or the RPWD Act provisions - that you were required to follow for this project?",
                    "At any point during the design process, were elderly users above 70 years specifically discussed? Was there any geriatric usability testing or older-user assessment conducted?",
                    "What assumptions were made about the typical digital literacy level of the primary user population for this project?",
                    "If you walk through the completed project today knowing that a 78-year-old with mild vision impairment and slight hand tremors will try to use it - what specific friction points do you think they would encounter?",
                    "What does universal design for oldest old users look like in the specific context of your project? Is it achievable within standard budgets and timelines?",
                    "What prevented more age-inclusive design from being incorporated in this project - budget constraints, client brief, technical limitations, timeline pressure, or something else?",
                    "Are you aware of WCAG 2.1 or 2.2 accessibility guidelines? Were any of their principles formally applied to the digital interfaces of this project?",
                    "If you were redesigning this project tomorrow with the oldest old as the primary user persona, what would you change first?"
                )
            )
        ),
        consentCode = "CONSENT-KI"
    )

    private val ssiIccc = Instrument(
        code = "SSI-ICCC-v1.0",
        title = "ICCC Operators and Technology Service Providers",
        stakeholder = "Technology",
        type = InstrumentType.QUALITATIVE,
        themes = listOf(
            InterviewTheme(
                code = "T1",
                title = "ICCC Monitoring and Digital Gatekeeping",
                openingQuestion = "Please describe your role at the ICCC and which city services the ICCC manages or monitors in your city.",
                probes = listOf(
                    "Does the ICCC capture data on who uses the city's digital services? Is there any age-related or demographic data in this system?",
                    "When a citizen is unable to complete a digital interaction - with a kiosk, a service portal, or a smart system - what alternative pathway are they directed to? Is there always an offline backup option for every service?",
                    "Do you receive or observe complaints related to elderly residents not being able to access digital services? What are the most commonly reported issues?",
                    "Are there any AI-based, voice recognition, biometric, or machine-learning-enabled services in this city that elderly residents must interact with? Are there known failure points or exclusion patterns for older users in those systems?",
                    "Does your system flag or track when service interactions fail or are abandoned? Can you provide anonymized data on interaction failure rates for key services?",
                    "In your informed estimate, what percentage of the city’s public-facing SCM services currently have NO viable offline backup option for people who cannot use the digital interface?"
                )
            )
        ),
        consentCode = "CONSENT-KI"
    )

    private val fiMco = Instrument(
        code = "FI-MCO-v1.0",
        title = "Municipal Ward-Level Officials",
        stakeholder = "Government",
        type = InstrumentType.QUALITATIVE,
        themes = listOf(
            InterviewTheme(
                code = "T1",
                title = "Last-Mile Delivery and Municipal Assistance",
                openingQuestion = "I would like to understand how city services actually reach residents in your ward - specifically elderly residents above 75 years. Which key city services in your ward are now primarily or exclusively accessed online or through digital systems?",
                probes = listOf(
                    "How often do elderly residents come to your ward office because they cannot access a required service online? What are the most common services they come for?",
                    "What do you actually tell or do for a resident who says they cannot use the online system? Is there always a manual or in-person process available to them?",
                    "Do you receive any directives from the SPV or the municipal corporation about making services accessible specifically to elderly residents?",
                    "In your honest assessment, are the digital services deployed under the Smart Cities Mission in your ward genuinely accessible to the oldest residents - say, a 78-year-old woman living alone?",
                    "What is the single biggest problem that elderly residents in your ward face when trying to access city services?",
                    "What make the biggest difference for your elderly residents in terms of making city services more accessible?"
                )
            )
        ),
        consentCode = "CONSENT-KI"
    )

    private val ssiOahAdmin = Instrument(
        code = "SSI-OAH-Admin-v1.0",
        title = "Old Age Home Administrators",
        stakeholder = "OAH",
        type = InstrumentType.MIXED,
        themes = listOf(
            InterviewTheme(
                code = "T1",
                title = "Resident Profile and Digital Capability Baseline",
                openingQuestion = "Could you tell me about your residents above 75 years - their background, their prior experience with technology before coming here, and their current digital capabilities?",
                probes = listOf(
                    "What proportion of your residents above 75 years have had any prior exposure to digital devices - computers, mobile phones - in their working or personal lives?",
                    "Do you have any residents in this age group who actively and independently use a smartphone or digital services? What proportion would you estimate?",
                    "Are residents’ cognitive abilities considered when designing any digital activities or assistance programmes?",
                    "What are the most common physical or cognitive barriers you observe among your oldest residents that affect their ability to interact with digital devices?"
                )
            ),
            InterviewTheme(
                code = "T2",
                title = "Digital Access to City Services and Smart City Interaction",
                openingQuestion = "Let me ask about how your residents access city services - particularly the services that are now primarily digital or online. Does this institution facilitate that access in any formal way?",
                probes = listOf(
                    "Are residents able to access their pensions, government benefits, or health services through online systems? Who helps them with this, and how systematically is that support provided?",
                    "Has any resident in this institution ever missed a government benefit payment or welfare entitlement because they could not complete the required online process? How frequently does this occur?",
                    "Are there any specific Smart City services in your city that your residents have tried to use or have needed to use? What has been their experience with those services?",
                    "Does the institution itself require residents to interact with digital systems - for example, biometric attendance, online registration for government inspections, e-health portals? Do residents find these difficult or distressing?",
                    "What is the most common digital barrier you personally observe or hear about among your oldest residents on a day-to-day basis?"
                )
            ),
            InterviewTheme(
                code = "T3",
                title = "Emotional and Psychological Impact on Residents",
                openingQuestion = "From your daily interaction with and observation of residents, have you noticed any emotional or psychological effects of digital exclusion or digital frustration on residents above 75?",
                probes = listOf(
                    "Have residents ever expressed frustration, shame, sadness, or distress specifically about their inability to use digital systems?",
                    "Have you observed any residents withdrawing from group activities, social interaction, or general engagement related to their difficulties with technology?",
                    "In your professional observation, do you think digital exclusion in the wider city contributes to isolation, loss of agency, or cognitive decline among your oldest residents?",
                    "Are there residents who have become noticeably more dependent on institution staff or on family for basic city service access in recent years? Is digital exclusion a contributing factor in your view?"
                )
            ),
            InterviewTheme(
                code = "T4",
                title = "Institutional Needs, Recommendations, and Policy Perspectives",
                openingQuestion = "Finally, based on your experience running this institution and your deep knowledge of your residents - what would be most helpful for genuinely improving digital access and digital well-being for your oldest residents?",
                probes = listOf(
                    "What specific resources - from government, NGOs, or technology providers - would most meaningfully help your institution improve digital access for residents above 75?",
                    "Do you believe current Indian law and policy on senior citizens adequately protects your residents from digital exclusion in public spaces and city services? What is missing?",
                    "Have you ever engaged directly with your city’s Smart City SPV or municipal corporation on the issue of digital accessibility for your residents? If yes, what was the outcome?",
                    "If you could recommend one specific legislative or policy change to address digital exclusion for oldest old residents in institutional care, what would it be?",
                    "Are you aware of any other institution in India or internationally that you think does an exemplary job of facilitating digital access for the oldest old?"
                )
            )
        ),
        consentCode = "CONSENT-KI"
    )

    private val ssiDlt = Instrument(
        code = "SSI-DLT-v1.0",
        title = "Digital Literacy Trainers",
        stakeholder = "DLT",
        type = InstrumentType.MIXED,
        themes = listOf(
            InterviewTheme(
                code = "T1",
                title = "Programme Design, Curriculum, and Content",
                openingQuestion = "Tell me about the digital literacy programme you run - who designed it, when was it developed, and what is its stated primary goal?",
                probes = listOf(
                    "Was this programme specifically designed for elderly participants, or is it a general digital literacy programme that has been adapted for them?",
                    "What specific digital skills or tasks does the curriculum teach? Does it cover the specific SCM services available in this city - the apps, the kiosks, the online portals?",
                    "Are the training materials available in the local language of this area? Are they designed for participants with low literacy?",
                    "Does the curriculum account for the cognitive and physical limitations common in the 75-85 age group - slower processing speed, tremors, vision issues?",
                    "How long is the programme, how many sessions, and what is the format? Has the format been changed based on participant feedback?"
                )
            ),
            InterviewTheme(
                code = "T2",
                title = "Barriers to Learning Among Oldest Old Participants",
                openingQuestion = "Now I would like to understand what it is actually like to train participants above 75 years old. What have you observed in your sessions?",
                probes = listOf(
                    "What are the most common and persistent barriers you encounter when training participants above 75 years?",
                    "Which specific tasks or interface elements do oldest old participants find most difficult? For example: entering OTPs, touchscreen navigation, CAPTCHA systems, small text, multiple steps?",
                    "Do participants in this age group learn differently from younger seniors in terms of pace, retention, or confidence? In what specific ways?",
                    "Have you adapted your training methods specifically for cognitive or physical limitations in the 75+ group? What adaptations have been most effective?",
                    "Do participants become anxious or distressed during training sessions? What typically triggers that, and how do you handle it?",
                    "What is your dropout rate specifically for participants above 75, and what are the main reasons they discontinue?"
                )
            ),
            InterviewTheme(
                code = "T3",
                title = "Programme Outcomes and Real-World Usability Assessment",
                openingQuestion = "After participants above 75 complete your programme, are they actually able to independently access the digital services that matter to them in their daily lives in this city?",
                probes = listOf(
                    "What is your programme’s ‘success’ definition and metric? How do you measure or assess whether the training has actually worked?",
                    "Do you have any follow-up data from 3 or 6 months after training on whether participants retain the skills they learned and continue using them?",
                    "In your honest professional assessment - not just the official numbers - can participants above 75 independently use the key Smart City services in your city after completing your programme?",
                    "Do you think the digital interfaces of SCM services in this city are actually compatible with the skill level your participants realistically achieve at the end of your programme?",
                    "Can you share a specific example of a participant above 75 who successfully and independently used a smart city service after your training? What made that possible?",
                    "Conversely, can you share a specific example of a participant who completed training but still cannot navigate city services independently? What were the barriers?"
                )
            ),
            InterviewTheme(
                code = "T4",
                title = "Recommendations for Ideal Programme Design and System Change",
                openingQuestion = "Based on everything you have learned from your direct experience - what would a genuinely effective digital literacy programme specifically for people between 75 and 85 years old in this city look like?",
                probes = listOf(
                    "What content would it include? What would it deliberately exclude or defer?",
                    "What format, duration, and delivery method would work best for this age group?",
                    "Who should deliver it - government trainers, NGO workers, youth volunteers, intergenerational family mentors?",
                    "Critically - what would need to change in the city’s own digital infrastructure for your programme's outcomes to result in genuine independent usage by this age group?",
                    "What single policy change or funding mechanism would most strengthen the work you are doing?"
                )
            )
        ),
        consentCode = "CONSENT-KI"
    )

    private val ssiNgo = Instrument(
        code = "SSI-NGO-v1.0",
        title = "NGOs and Civil Society",
        stakeholder = "NGO",
        type = InstrumentType.QUALITATIVE,
        themes = listOf(
            InterviewTheme(
                code = "T1",
                title = "NGO Advocacy, Case Support, and Field Experience",
                openingQuestion = "Tell me about your organisation's work with elderly residents in your city - the services you provide, who your primary beneficiaries are, and how long you have been working in this space.",
                probes = listOf(
                    "In your field experience and case documentation, how have older residents - particularly those above 75 - been affected by the increasing digitalization of city services over the past five years?",
                    "Has your organisation documented specific cases where oldest old persons were denied a service, a welfare benefit, or access to a public space because of a digital barrier? Can you describe some of those cases without identifying individuals?",
                    "Does your organisation actively help elderly people navigate digital city services? In what specific ways, and with what resources?",
                    "What are the most common complaints, distress situations, or cases of abandonment (giving up on a service) that you see among oldest old persons related to digital city services?",
                    "Do you observe significant differences in how different groups of oldest old experience digital exclusion - for example, women versus men, migrants versus long-term residents, literate versus illiterate, those with family support versus those without?",
                    "Has your organisation engaged formally with the city’s Smart City SPV or municipal corporation on the issue of elderly digital inclusion? What was the result of that engagement?",
                    "What advocacy work has your organisation undertaken, or are you planning, specifically related to digital inclusion for oldest old persons in smart cities?",
                    "What do you believe is most critically missing from current law and policy - at the central or state level - that would meaningfully protect oldest old persons from digital exclusion in public urban spaces?",
                    "What data, case records, or evidence does your organisation hold on this issue that you would be willing to share formally with this research?"
                )
            )
        ),
        consentCode = "CONSENT-KI"
    )

    private val seiGeri = Instrument(
        code = "SEI-Geri-v1.0",
        title = "Geriatricians and Mental Health Professionals",
        stakeholder = "Geriatricians",
        type = InstrumentType.QUALITATIVE,
        themes = listOf(
            InterviewTheme(
                code = "T1",
                title = "Clinical and Cognitive Impact of Digitalization",
                openingQuestion = "Your expertise is in geriatrics/gerontology/elderly mental health. I would like to understand, from a clinical and research perspective, how digital systems and technology-dense environments affect the cognitive, emotional, and physical well-being of persons aged between 75 and 85 years.",
                probes = listOf(
                    "What are the primary cognitive changes occurring in persons aged 75-85 that would affect their ability to successfully interact with modern digital interfaces - touchscreens, apps, kiosks, QR codes?",
                    "Is what we might call 'digital anxiety' - psycho-emotional stress induced by anticipated or actual failure in a digital interaction - a phenomenon you observe or recognise clinically among your oldest patients? What are its typical manifestations?",
                    "Do you have clinical observations or documented cases of patients in the 75-85 age group who have experienced measurable deterioration in psychological well-being - depression, social withdrawal, or loss of confidence and agency - that is linked to their inability to navigate digital public services?",
                    "Are there documented clinical studies or published evidence on the cognitive load imposed by modern digital interfaces on persons above 75? What does the available evidence indicate?",
                    "What specific design features in digital interfaces - whether kiosk, mobile app, or web portal - are most cognitively and motorically harmful or exclusionary for the oldest old from a clinical standpoint?",
                    "Conversely, what specific design features or interface characteristics would make digital interactions genuinely usable and cognitively safe for your oldest patients?",
                    "Is there clinical or epidemiological evidence to support the argument that digital exclusion in public urban spaces - being functionally locked out of city services - contributes to or accelerates cognitive decline, depression, or social isolation in the oldest old?",
                    "From a public health and geriatric medicine standpoint, what specific recommendations would you make to policymakers designing digital city infrastructure to prevent harm to the oldest old?"
                )
            )
        ),
        consentCode = "CONSENT-KI"
    )

    private val eiLegalAc = Instrument(
        code = "EI-Legal-Ac-v1.0",
        title = "Constitutional Law and Elder Rights Scholars",
        stakeholder = "Legal",
        type = InstrumentType.QUALITATIVE,
        themes = listOf(
            InterviewTheme(
                code = "T1",
                title = "Constitutional Mandates and Legal Rights Frameworks",
                openingQuestion = "I would like to discuss the constitutional and legal dimensions of digital exclusion for the oldest old in India's Smart Cities. To begin - how do you interpret the scope of Article 21 (right to life with dignity and personal liberty) and Article 41 (right to public assistance) in relation to an oldest old person who is functionally unable to access state-provided public services because of mandatory digital gatekeeping?",
                probes = listOf(
                    "Is there existing jurisprudence - from the Supreme Court or High Courts - on the positive obligation of the state to ensure accessible delivery of public services for vulnerable or marginalised populations?",
                    "Can the doctrine of 'substantive dignity' under Article 21 be doctrinally extended to encompass access to digital public infrastructure? What would be the strongest constitutional argument for this position?",
                    "In your legal view, does ‘digital-by-default’ governance - where the state provides no viable non-digital alternative for a service - constitute a violation of an oldest old person's fundamental rights under Articles 14, 21, or 41?",
                    "How does the Maintenance and Welfare of Parents and Senior Citizens Act 2007 and the proposed 2019 Amendment Bill engage with or fail to engage with state-imposed digital barriers? What is the precise legislative gap?",
                    "What specific legislative intervention - whether an amendment, a new provision, or a new statute - would most effectively protect oldest old persons from digital exclusion in public spaces?",
                    "Is there any doctrine of indirect discrimination or disparate impact in Indian constitutional law that might be applied to age-based digital exclusion?",
                    "How do international frameworks - including the UNCRPD, the UN Principles for Older Persons, and WHO age-friendly cities guidance - inform Indian constitutional interpretation in this legal space?",
                    "What is your considered view on redefining ‘welfare’ and ‘dignity’ under Indian elder law to explicitly encompass digital autonomy and infrastructure equity?",
                    "Are there any pending cases or recent judgments - at any court level - that touch on these issues that we should be examining?"
                )
            )
        ),
        consentCode = "CONSENT-KI"
    )

    private val sqCgSsiCg = Instrument(
        code = "SQ-CG + SSI-CG-v1.0",
        title = "Caregivers and Family Members",
        stakeholder = "Caregivers",
        type = InstrumentType.MIXED,
        sections = listOf(
            InstrumentSection(
                code = "CG_A",
                title = "Primary Caregiver Profile & Assistance Details",
                questions = listOf(
                    Question("CG1", "What is your relationship to the oldest old participant you are being asked about?", InputType.SINGLE_CHOICE, options = listOf("Adult son or daughter", "Spouse", "Grandchild", "Sibling or other relative", "Paid caregiver or domestic worker", "Neighbour or friend", "NGO volunteer or social worker", "Other"), sectionCode = "CG_A", instrumentCode = "SQ-CG + SSI-CG-v1.0"),
                    Question("CG2", "How many times per week, on average, do you help the older person with digital tasks?", InputType.SINGLE_CHOICE, options = listOf("Every day (approximately 7 times per week)", "3 to 5 times per week", "1 to 2 times per week", "Rarely - less than once per week", "Never - I do not help with digital tasks"), sectionCode = "CG_A", instrumentCode = "SQ-CG + SSI-CG-v1.0"),
                    Question("CG3", "What types of digital tasks do you most commonly assist the older person with? (Select all that apply)", InputType.MULTI_CHOICE, options = listOf("Making or receiving phone calls or WhatsApp", "Online bill payment (electricity, water, etc.)", "Government portal use or online form submission", "Booking a medical appointment online", "Banking, UPI, or other digital payment", "Travel or public transport booking", "Accessing Smart City services or apps", "Filing a complaint or grievance online", "Government scheme registration or renewal", "Other"), sectionCode = "CG_A", instrumentCode = "SQ-CG + SSI-CG-v1.0"),
                    Question("CG4", "How much total time do you typically spend per week on digital assistance for this older person?", InputType.SINGLE_CHOICE, options = listOf("Less than 30 minutes per week", "30 to 60 minutes per week", "1 to 3 hours per week", "More than 3 hours per week"), sectionCode = "CG_A", instrumentCode = "SQ-CG + SSI-CG-v1.0"),
                    Question("CG5", "How often does the older person express frustration, anxiety, or emotional distress specifically about digital systems?", InputType.SINGLE_CHOICE, options = listOf("Very often - almost daily or weekly", "Sometimes - a few times per month", "Rarely - once in a while", "Never - they do not express this to me", "They do not discuss technology frustrations with me"), sectionCode = "CG_A", instrumentCode = "SQ-CG + SSI-CG-v1.0"),
                    Question("CG6", "In your direct observation, has the older person reduced the frequency of going out to public places because of digital barriers?", InputType.SINGLE_CHOICE, options = listOf("Yes - significantly reduced outings because of digital barriers", "Yes - to some extent reduced outings", "No - I have not noticed a change related to this", "Not sure"), sectionCode = "CG_A", instrumentCode = "SQ-CG + SSI-CG-v1.0"),
                    Question("CG7", "In your observation, has the older person ever missed a service, benefit, appointment, or government entitlement specifically because of digital access barriers?", InputType.SINGLE_CHOICE, options = listOf("Yes - this has happened multiple times", "Yes - this happened once", "No - I do not believe this has happened", "Not sure"), sectionCode = "CG_A", instrumentCode = "SQ-CG + SSI-CG-v1.0"),
                    Question("CG8", "How does the older person's reliance on you (or others) for digital tasks affect your relationship with them?", InputType.SINGLE_CHOICE, options = listOf("No effect - it is just a normal part of our relationship", "I sometimes feel frustrated by repeated requests for the same help", "It has occasionally caused tension or conflict between us", "It has brought us closer - a positive shared activity", "I feel sad that they cannot do this independently", "Other"), sectionCode = "CG_A", instrumentCode = "SQ-CG + SSI-CG-v1.0")
                )
            )
        ),
        themes = listOf(
            InterviewTheme(
                code = "CG_B",
                title = "Primary Caregiver Lived Experience Narrative",
                openingQuestion = "Based on your close and regular interaction with the participant, I would like to understand how digital systems in this city affect their daily life from your personal perspective and observation.",
                probes = listOf(
                    "Can you describe a specific and recent situation where you had to step in to help them with a digital task they could not manage? What exactly happened, step by step?",
                    "How does the older person typically react - emotionally and behaviorally - when they encounter a digital system they cannot use at a kiosk, hospital counter, park entry, or government office?",
                    "Has there been an incident where they were turned away from a service, denied access to a facility, or lost an entitlement specifically because of a digital requirement? Tell me about that in detail.",
                    "Do you notice any changes in their mood, self-confidence, social behavior, or overall sense of independence that you believe are related to their difficulties with digital systems?",
                    "In your honest view, do the Smart City services in your city work for people of their age and background? What specific changes would make those services work better for them?",
                    "Do you feel that the government and city planners have genuinely considered the needs and limitations of the oldest old population when designing and rolling out these digital systems?",
                    "What has been the biggest impact - positive or negative - that digital city services have had on this person's life and on your role as their caregiver or support person?"
                )
            )
        ),
        consentCode = "CONSENT-KI"
    )

    private val foOop = Instrument(
        code = "FO-OOP-v1.0",
        title = "Field Observation and Shadowing",
        stakeholder = "OOP",
        type = InstrumentType.OBSERVATIONAL,
        themes = listOf(
            InterviewTheme(
                code = "T1",
                title = "Naturalistic Shadowing of SCM Interactions",
                openingQuestion = "Observe and record details about the participant's physical mobility, route choices, SCM gatekeeping, and digital interface encounters.",
                probes = listOf(
                    "Physical Mobility: Walking pace, walking aids, uneven surfaces, steps, or slopes.",
                    "Route Navigation: Seeking directions, using map apps, confusion, wrong turns.",
                    "Facility Accessibility: Entrance steps, ramps, signage, digital entry check (QR code, biometric).",
                    "Staff Interaction: Staff presence, helpfulness, digital insistence, manual alternatives.",
                    "Digital Interfaces: Engage kiosk, QR scan, app, biometric, elapsed time, verbal/emotional response, helper involvement.",
                    "Heuristic Assessment: Heuristic scoring of text size, local language suitability, availability of help buttons, touchscreen responsiveness, offline options."
                )
            )
        ),
        consentCode = "CONSENT-OOP-W"
    )

    private val fgdOopCg = Instrument(
        code = "FGD-OOP/CG-v1.0",
        title = "Focus Group Discussion",
        stakeholder = "OOP+CG",
        type = InstrumentType.QUALITATIVE,
        themes = listOf(
            InterviewTheme(
                code = "T1",
                title = "Ground Rules & Ice-Breaker",
                openingQuestion = "Thank you all for coming today. We are here to listen and learn about how life in this city has been changing for older people.",
                probes = listOf(
                    "Ground Rules: One person speaks at a time, what is shared here stays here, everyone's voice matters.",
                    "Ice-Breaker: First name, years in city, and one thing you personally enjoy doing in this city."
                )
            ),
            InterviewTheme(
                code = "T2",
                title = "Transition Questions",
                openingQuestion = "When you need to do something ordinary - pay a bill, visit a doctor, take transport, visit a park - what does that actually involve these days?",
                probes = listOf(
                    "What specific services have moved online or to apps or digital machines?",
                    "Was there a particular moment when you first realized things had changed?",
                    "Has anything become easier for you personally because of these changes?"
                )
            ),
            InterviewTheme(
                code = "T3",
                title = "Key Discussion Questions",
                openingQuestion = "Which services feel most difficult or impossible to access these days? Why do you think that is?",
                probes = listOf(
                    "Emotional impact: how do you feel when you are unable to do something on your own?",
                    "Social connections & intergenerational impact: does the tech gap affect relationships?",
                    "Rights, government responsibility & policy: does the state have a duty to ensure non-digital options?",
                    "Solutions & recommendations: what would a truly age-friendly smart city look and feel like to you?"
                )
            )
        ),
        consentCode = "CONSENT-OOP-W"
    )

    private val doctrinal = Instrument(
        code = "DOCTRINAL-v1.0",
        title = "Legal and Policy Document Analysis",
        stakeholder = "PI/RA",
        type = InstrumentType.DOCTRINAL,
        themes = listOf(
            InterviewTheme(
                code = "T1",
                title = "Substantive Policy Analysis Matrix",
                openingQuestion = "Conduct structural analysis of constitutional provisions, statutes, amendment bills, judgments, and policies.",
                probes = listOf(
                    "Definition of senior citizen / oldest old (75+).",
                    "Definition of welfare and dignity (public space and digital access coverage).",
                    "Obligations of the state vs private actors for service delivery.",
                    "Technology, digital access, and digital exclusion rights violation analysis.",
                    "Enforcement mechanisms, remedies, and accessibility of grievance redressal.",
                    "Gaps related to digital gatekeeping and potential precedents under Article 21/41."
                )
            )
        ),
        consentCode = "CONSENT-KI"
    )

    private val qaTools = Instrument(
        code = "QA-v1.0",
        title = "Quality Assurance Checklists",
        stakeholder = "All",
        type = InstrumentType.ADMINISTRATIVE,
        themes = listOf(
            InterviewTheme(
                code = "T1",
                title = "Daily Field Supervisors & RAs Checklist",
                openingQuestion = "Systematic review of daily diaries, session quality tracking, pilot testing evaluations, and inter-rater reliability coding.",
                probes = listOf(
                    "Check consistency of Participant IDs across all session forms.",
                    "Confirm presence and date of signed/oral consent prior to session data collection.",
                    "Verify session duration limits (SQ: max 55 min, SSI: max 75 min).",
                    "Perform data quality assessments, transcript requests, and backup verification."
                )
            )
        ),
        consentCode = null
    )

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
