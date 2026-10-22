/**
 * CivicAge Survey Web Application - Instruments Bank
 * Complete Definition of All 15 Research Instruments
 * Project: Smart Cities, Silent Citizens (ICSSR Major Research Project 2025-26)
 */

// Helper for Digital Anxiety Scale items (D1 to D10)
function _getDigitalAnxietyText(index) {
  const map = {
    1: "I feel nervous or worried when I am asked to use a government app, website, or digital kiosk to do something.",
    2: "I am afraid that I will make a mistake and something bad will happen - like losing money or important data - when I use digital services.",
    3: "When I am not able to complete a task on a digital device, I feel ashamed or embarrassed in front of others.",
    4: "I have stopped going to certain public places or stopped trying to access certain services because I cannot use the digital systems required.",
    5: "I feel that the new digital systems in this city were not designed with people like me in mind.",
    6: "When a family member or neighbour has to help me complete a digital task, I feel like a burden to them.",
    7: "I feel more disconnected from city life than I did five years ago, because so many things have moved to apps and online systems.",
    8: "I feel angry or frustrated when a service I used to access easily at an office has moved to an app or online-only system.",
    9: "Thinking about having to use a touchscreen or digital device for something important makes me feel stressed.",
    10: "I feel that my ability to live independently in this city has been reduced by digital systems that I cannot use."
  };
  return map[index] || "";
}

// Helper for Dignity Perception Scale items (E7 to E12)
function _getDignityPerceptionText(index) {
  const map = {
    7: "I feel that this city respects my ability to participate in public life independently, as an older person.",
    8: "I feel that my personal dignity is affected when I cannot access services that others access easily through phones or apps.",
    9: "I feel the government has taken adequate steps to ensure that older people like me are not left behind by new technology.",
    10: "Having to constantly depend on others for tasks I should be able to do myself makes me feel less respected as a person.",
    11: "I feel that digital systems in this city have taken away some of my freedom and independence.",
    12: "I feel that the people who design city services understand the needs and limitations of older persons like me."
  };
  return map[index] || "";
}

const INSTRUMENT_BANK = {
  getDigitalAnxietyText: _getDigitalAnxietyText,
  getDignityPerceptionText: _getDignityPerceptionText,

  instruments: [
    // ── 1. SQ-OOP-v1.0 (Quantitative) ──────────────────────────────────────
    {
      code: "SQ-OOP-v1.0",
      title: "Structured Questionnaire - Oldest Old Persons",
      stakeholder: "Oldest Old Persons (75+)",
      type: "QUANTITATIVE",
      consentCode: "CONSENT-OOP-W",
      description: "Comprehensive quantitative survey exploring digital device ownership, smart city service usage, digital anxiety, dependency frequency, and dignity perceptions.",
      sections: [
        {
          code: "A",
          title: "Socio-Demographic Profile",
          questions: [
            { id: "A1", text: "What is your current age?", inputType: "NUMBER", isRequired: true },
            { id: "A2", text: "What is your gender?", inputType: "SINGLE_CHOICE", options: ["Male", "Female", "Transgender", "Prefer not to say"], isRequired: true },
            { id: "A3", text: "What is your mother tongue / primary language?", inputType: "TEXT", isRequired: true },
            { id: "A4", text: "What is the highest level of formal education you have completed?", inputType: "SINGLE_CHOICE", options: ["No formal education", "Primary only (Class 1-5)", "Middle school (Class 6-8)", "Secondary (Class 9-10)", "Higher Secondary (Class 11-12)", "Graduation or above", "Religious / informal education only"], isRequired: true },
            { id: "A5", text: "What is your current living arrangement?", inputType: "SINGLE_CHOICE", options: ["Living alone", "With spouse only", "With adult children / grandchildren", "With extended family (joint household)", "Old Age Home - government", "Old Age Home - private / NGO", "Other"], isRequired: true },
            { id: "A6", text: "What is your primary source of monthly income?", inputType: "SINGLE_CHOICE", options: ["Government pension", "Private / employer pension", "Family financial support", "Own savings / interest / rental income", "Government welfare scheme", "No regular income", "Other"], isRequired: true },
            { id: "A7", text: "What is your estimated total monthly household income (including all sources)?", inputType: "SINGLE_CHOICE", options: ["Below Rs. 5,000", "Rs. 5,001 to Rs. 10,000", "Rs. 10,001 to Rs. 20,000", "Rs. 20,001 to Rs. 50,000", "Above Rs. 50,000", "Don't know / Prefer not to say"], isRequired: true },
            { id: "A8", text: "How long have you been living in this city?", inputType: "SINGLE_CHOICE", options: ["Less than 5 years", "5 to 15 years", "15 to 30 years", "More than 30 years", "Born and raised here"], isRequired: true },
            { id: "A9", text: "How would you describe your general health these days?", inputType: "SINGLE_CHOICE", options: ["1 = Very good", "2 = Good", "3 = Fair / moderate", "4 = Poor", "5 = Very poor"], isRequired: true },
            { id: "A10", text: "Do you currently experience any of the following? (Select all that apply)", inputType: "MULTI_CHOICE", options: ["Difficulty seeing (even with spectacles)", "Difficulty hearing (even with hearing aid)", "Hand tremors or difficulty with fine finger movements", "Memory difficulties or frequent forgetfulness", "Difficulty walking or limited mobility", "Chronic pain affecting daily activities", "None of the above"], isRequired: true },
            { id: "A11", text: "Do you currently receive regular caregiving support from a paid or unpaid caregiver?", inputType: "SINGLE_CHOICE", options: ["Yes - full-time paid caregiver", "Yes - part-time paid caregiver", "Yes - unpaid family member as caregiver", "No - I manage independently", "Institutional care only (old age home staff)"], isRequired: true }
          ]
        },
        {
          code: "B",
          title: "Digital Device Ownership and Exposure Profile",
          questions: [
            { id: "B1", text: "Do you personally own a mobile phone?", inputType: "SINGLE_CHOICE", options: ["Yes - basic phone", "Yes - smartphone", "No, I do not own any phone", "Shared household phone"], isRequired: true },
            { id: "B2", text: "If you have access to any phone, how do you primarily use it?", inputType: "MULTI_CHOICE", options: ["Making or receiving phone calls", "Sending or receiving SMS", "WhatsApp messaging or calls", "Video calls", "Watching videos", "Government apps", "Banking/UPI", "News/Entertainment", "I do not use any phone"], isRequired: true },
            { id: "B3", text: "For digital tasks on a phone or computer, who usually operates the device for you?", inputType: "SINGLE_CHOICE", options: ["I do it all myself", "My spouse", "My adult son or daughter", "My grandchild", "Paid caregiver", "Neighbour or friend", "Volunteer/NGO worker", "Shop owner/Agent", "Does not apply"], isRequired: true },
            { id: "B4", text: "Do you have access to a computer, laptop, or tablet at home?", inputType: "SINGLE_CHOICE", options: ["Yes - I use it myself independently", "Yes - but only other family members use it", "No access to these devices at home", "Sometimes use at a cyber cafe or library"], isRequired: true },
            { id: "B5", text: "How often do you PERSONALLY attempt to use any digital device without asking for help first?", inputType: "SINGLE_CHOICE", options: ["Every day", "A few times a week (3 to 5 times)", "Once a week", "A few times a month", "Rarely - once in a few months", "Never - I always ask for help or avoid it"], isRequired: true },
            { id: "B6", text: "Before your retirement or in your working life, did you ever regularly use any digital device or technology?", inputType: "SINGLE_CHOICE", options: ["Yes - regularly used computer or internet at work", "Yes - used basic technology (early mobile phone, desktop)", "Yes - some limited use", "No - I have never used digital devices in my life"], isRequired: true },
            { id: "B7", text: "On a scale of 1 to 5, how confident do you feel right now in using a smartphone or digital kiosk?", inputType: "SINGLE_CHOICE", options: ["1 = Not at all confident", "2 = Mostly not confident", "3 = Somewhat confident", "4 = Fairly confident", "5 = Very confident"], isRequired: true },
            { id: "B8", text: "Is there a working internet connection available in your home or old age home?", inputType: "SINGLE_CHOICE", options: ["Yes - broadband or Wi-Fi connection", "Yes - mobile data connection", "Sometimes - unreliable connection", "No internet access at all"], isRequired: true }
          ]
        },
        {
          code: "C",
          title: "Awareness and Usage of Smart City Mission Services",
          questions: [
            { id: "C_GRID", text: "Smart City Service Usage Grid (Heard / Tried / Used Alone / Used with Help / Never)", inputType: "TABLE", citySpecific: true, isRequired: true },
            { id: "C9", text: "For the services you TRIED but could NOT use successfully, what was the PRIMARY reason? (Select up to 3)", inputType: "MULTI_CHOICE", options: ["Did not know how to use the device or app", "Did not have the required device", "Service was only available online - no offline option", "Language barrier - interface not in my language", "Screen was too small to read clearly", "Could not understand the steps", "Afraid of making a mistake with money or data", "No one available nearby to help me", "Physical difficulty (tremors, vision, hearing)", "Long queue at the kiosk or service point", "Required OTP on a phone I cannot operate", "Service point was not accessible in my area", "Other"], isRequired: true },
            { id: "C10", text: "In the PAST ONE MONTH, how many times did you need someone else’s help to access a government or civic service?", inputType: "SINGLE_CHOICE", options: ["0 times - I managed fully on my own", "1 to 2 times", "3 to 5 times", "6 to 10 times", "More than 10 times"], isRequired: true },
            { id: "C11", text: "Have you ever been told by a government office or service provider that a service is ‘only available online’?", inputType: "SINGLE_CHOICE", options: ["Yes - this has happened more than once", "Yes - this happened once", "No - this has never happened to me", "Don't know / Don't remember"], isRequired: true },
            { id: "C12", text: "If YES to C11 - what did you do after that? (Select all that apply)", inputType: "MULTI_CHOICE", options: ["Gave up and went without the service", "Went home and asked a family member to help", "Hired a local agent or middleman to do it for me", "Went to a cyber cafe and paid for help", "Found an alternative physical office or method", "Filed a formal complaint", "Other"], isRequired: false },
            { id: "C13", text: "In the PAST SIX MONTHS, have you missed or significantly delayed any of the following due to digital barriers?", inputType: "MULTI_CHOICE", options: ["Medical appointment (could not book online)", "Government benefit or pension (online process incomplete)", "Utility payment (online-only - led to default or penalty)", "Travel or public transport (could not use digital ticketing)", "Accessing a public park or facility (QR or app required for entry)", "Government scheme registration", "None of these situations have occurred", "Other"], isRequired: true },
            { id: "C14", text: "Have you stopped visiting or using any public place in this city because it requires a digital device/app?", inputType: "SINGLE_CHOICE", options: ["Yes - I have stopped going to specific places", "No - I still go everywhere I want to", "I still go but always ask someone for help at the digital gate"], isRequired: true },
            { id: "C15", text: "In the past 6 months, did you pay money to a middleman, agent, or cyber cafe operator for a civic task?", inputType: "SINGLE_CHOICE", options: ["Yes - I paid once", "Yes - I paid more than once", "No - I have not paid anyone for this", "Don't remember"], isRequired: true }
          ]
        },
        {
          code: "D",
          title: "Digital Anxiety Scale (10-Item Likert)",
          questions: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map(i => ({
            id: `D${i}`,
            text: _getDigitalAnxietyText(i),
            inputType: "LIKERT",
            options: ["Strongly Disagree", "Disagree", "Neither", "Agree", "Strongly Agree"],
            isRequired: true
          }))
        },
        {
          code: "E1",
          title: "Digital Dependency Frequency Index",
          questions: [
            { id: "E1", text: "In a typical week, for how many different tasks or services requiring digital access do you need help?", inputType: "SINGLE_CHOICE", options: ["0 - I manage fully independently", "1 to 2 tasks per week", "3 to 5 tasks per week", "More than 5 tasks per week", "Every single task requiring digital access"], isRequired: true },
            { id: "E2", text: "Who do you MOST OFTEN turn to for digital help?", inputType: "SINGLE_CHOICE", options: ["My spouse", "My adult son or daughter", "My grandchild", "Paid caregiver or domestic helper", "Neighbour or friend", "Local shop owner or agent", "NGO volunteer or social worker", "Old age home staff member", "No one - I attempt alone or simply give up"], isRequired: true },
            { id: "E3", text: "Have you MISSED or DELAYED a medical appointment in the past 6 months due to online booking barriers?", inputType: "SINGLE_CHOICE", options: ["Yes - this happened more than once", "Yes - this happened once", "No - this has not happened to me", "I do not need to book medical appointments online"], isRequired: true },
            { id: "E4", text: "Have you MISSED receiving a government benefit or pension in the past 12 months due to online process barriers?", inputType: "SINGLE_CHOICE", options: ["Yes - happened more than once", "Yes - happened once", "No", "Not aware of any such schemes I should be enrolled in", "Someone else manages all this for me"], isRequired: true },
            { id: "E5", text: "In your assessment, are you MORE DEPENDENT on other people for daily activities now than 5 years ago?", inputType: "SINGLE_CHOICE", options: ["Much more dependent on others now", "Somewhat more dependent now", "About the same as 5 years ago", "More independent now than before"], isRequired: true },
            { id: "E6", text: "If MORE DEPENDENT: to what extent have digital systems in the city contributed to this increased dependence?", inputType: "SINGLE_CHOICE", options: ["Digital systems are a MAJOR cause of my increased dependence", "Digital systems are a MINOR cause - health is the bigger factor", "Digital systems are NOT a cause - it is entirely about health", "Not applicable - I am not more dependent"], isRequired: false }
          ]
        },
        {
          code: "E2",
          title: "Dignity Perception Scale (6-Item Likert)",
          questions: [7, 8, 9, 10, 11, 12].map(i => ({
            id: `E${i}`,
            text: _getDignityPerceptionText(i),
            inputType: "LIKERT",
            options: ["Strongly Disagree", "Disagree", "Neither", "Agree", "Strongly Agree"],
            isRequired: true
          }))
        },
        {
          code: "F",
          title: "Social Isolation and Intergenerational Impact",
          questions: [
            { id: "F1", text: "How often do you leave your home or old age home to visit public places?", inputType: "SINGLE_CHOICE", options: ["Every day", "3 to 5 times per week", "Once a week", "Once or twice a month", "Rarely - once in a few months", "Almost never"], isRequired: true },
            { id: "F2", text: "Compared to 5 years ago, how has your frequency of going to public places in the city changed?", inputType: "SINGLE_CHOICE", options: ["I go out much more often now", "About the same as before", "I go out somewhat less often now", "I go out much less often now"], isRequired: true },
            { id: "F3", text: "Do you feel lonely or isolated from the life of the city around you?", inputType: "SINGLE_CHOICE", options: ["Often / frequently", "Sometimes", "Rarely", "Never"], isRequired: true },
            { id: "F4", text: "Do you feel that the technology gap has affected your relationship with younger family members?", inputType: "SINGLE_CHOICE", options: ["Yes - it has created noticeable distance or sometimes causes conflict", "Yes - they are sometimes impatient with me", "Somewhat - very minor effect on the relationship", "No - it has not affected our relationship at all", "I have no children or grandchildren"], isRequired: true },
            { id: "F5", text: "Are there other older people your age in your area who face similar digital difficulties?", inputType: "SINGLE_CHOICE", options: ["Yes - many of us face the same situation", "Yes - a few others face it", "I am not sure", "No - most other older people I know manage fine"], isRequired: true }
          ]
        },
        {
          code: "G",
          title: "Digital Literacy Exposure and Scheme Awareness",
          questions: [
            { id: "G1", text: "Have you ever attended a digital literacy programme, class, or training session?", inputType: "SINGLE_CHOICE", options: ["Yes - government programme", "Yes - NGO or private organization programme", "Yes - a family member informally taught me", "No - I have never attended any such training"], isRequired: true },
            { id: "G2", text: "After attending training, are you now able to do any of the following independently?", inputType: "MULTI_CHOICE", options: ["Make or receive phone calls independently", "Send WhatsApp messages independently", "Use a government app or online portal independently", "Make a digital payment independently", "Book an appointment or avail a service online independently", "None - the training did not help enough", "Not applicable"], isRequired: false },
            { id: "G3", text: "Are you aware of the Atal Vayo Abhyudaya Yojana or any other scheme for older persons?", inputType: "SINGLE_CHOICE", options: ["Yes - I am aware and currently enrolled", "Yes - I am aware but not enrolled", "I have heard of it but do not know the details", "No - I have never heard of this scheme"], isRequired: true },
            { id: "G4", text: "Have you ever tried to access government welfare benefits through their online portals or apps?", inputType: "SINGLE_CHOICE", options: ["Yes - I did it successfully on my own", "Yes - I did it with a family member's help", "Yes - I did it through a paid agent or middleman", "Yes - I tried but failed to complete the process", "No - I have never tried; I do not know how", "No - I have never tried; there was no need"], isRequired: true },
            { id: "G5", text: "Do you feel digital literacy programmes are designed specifically for people your age?", inputType: "SINGLE_CHOICE", options: ["Yes - they are well-designed for people like me", "Somewhat - they could be improved for older people", "No - they are too fast-paced and not suitable for me", "No - I have no access to any such programme", "I have not attended any to judge"], isRequired: true }
          ]
        },
        {
          code: "H",
          title: "Field Investigator Observation Log",
          questions: [
            { id: "H1", text: "Participant’s apparent reading ability based on observation during session", inputType: "SINGLE_CHOICE", options: ["Reads confidently and independently", "Reads but with some visible difficulty", "Could not read - relied entirely on oral administration", "Unable to assess accurately"], isRequired: true },
            { id: "H2", text: "Visual impairment indicators observed during session", inputType: "SINGLE_CHOICE", options: ["None observed - no apparent vision difficulty", "Squinting or holding printed card very close or very far", "Participant requested larger print or brighter light", "Appeared to struggle significantly with visual material"], isRequired: true },
            { id: "H3", text: "Hand tremors or motor difficulty observed", inputType: "SINGLE_CHOICE", options: ["None observed", "Mild - some difficulty holding pen or pointing accurately", "Moderate - had significant difficulty marking or pointing", "Severe - FI had to record all responses from verbal only"], isRequired: true },
            { id: "H4", text: "Overall comprehension level during session", inputType: "SINGLE_CHOICE", options: ["Understood all questions without needing re-explanation", "Required re-explanation of some questions", "Frequently required simplified re-wording throughout", "Serious comprehension concerns - flag for PI review immediately"], isRequired: true },
            { id: "H5", text: "Was anyone else present during any part of the session?", inputType: "SINGLE_CHOICE", options: ["No - session was fully private throughout", "Yes - briefly present", "Yes - present for significant part of session"], isRequired: true },
            { id: "H6", text: "Signs of emotional distress, anxiety, or strong emotional response during session", inputType: "SINGLE_CHOICE", options: ["None observed", "Mild - slight visible discomfort on specific questions", "Moderate - long pause, visible tears, or agitation at some point", "Significant distress - session modified or stopped early"], isRequired: true },
            { id: "H7", text: "Session completion status", inputType: "SINGLE_CHOICE", options: ["Fully completed - all sections done", "Partially completed", "Terminated early by participant", "Terminated by FI"], isRequired: true },
            { id: "H8", text: "FI’s overall assessment of data reliability from this session", inputType: "SINGLE_CHOICE", options: ["High quality - confident responses throughout; data reliable", "Moderate quality - some reliability concerns noted", "Low quality - significant concerns; flag for PI decision on inclusion"], isRequired: true },
            { id: "H9", text: "Contextual notes: neighbourhood type, physical setting, any other relevant observations", inputType: "FREE_TEXT", isRequired: false }
          ]
        }
      ]
    },

    // ── 2. SSI-OOP-v1.0 (Qualitative) ──────────────────────────────────────
    {
      code: "SSI-OOP-v1.0",
      title: "Semi-Structured Interview - Oldest Old Persons",
      stakeholder: "Oldest Old Persons (75+)",
      type: "QUALITATIVE",
      consentCode: "CONSENT-OOP-W",
      description: "In-depth qualitative interview exploring life before and after digitalization, lived experiences with Smart City services, digital anxiety, family dynamics, and rights consciousness.",
      themes: [
        {
          code: "T1",
          title: "Life Before and After Digital Transformation",
          openingQuestion: "Could you tell me a little about yourself - how long you have lived in this city, and what daily life was like for you here about ten to fifteen years ago?",
          probes: [
            "Where did you go to pay your bills, visit a doctor, or collect your pension in those days?",
            "Were those places easy for you to reach? Did you go alone or with someone?",
            "What were the things you could do completely independently in those days that gave you a sense of confidence or pride?"
          ]
        },
        {
          code: "T2",
          title: "Specific Experiences with Smart City Services",
          openingQuestion: "I would like to hear about specific experiences you have had with city services here. Have you ever tried to use or access city-specific SCM services?",
          probes: [
            "Where were you exactly when you tried to use it? Were you alone, or was someone with you?",
            "At what point did you find it difficult, if you did? What specifically was the difficulty?",
            "How did you feel at that exact moment?",
            "Did you have to ask someone for help? Who was it? How did that interaction feel?"
          ]
        },
        {
          code: "T3",
          title: "Emotional Experience, Digital Anxiety, and Mental Well-being",
          openingQuestion: "How do you feel emotionally when you are unable to complete something that your children, grandchildren, or younger neighbours can easily do on their phones?",
          probes: [
            "Can you describe that feeling in more detail - is it frustration, sadness, embarrassment, helplessness, or something else?",
            "Is there a particular incident that stands out where you felt especially distressed because of a digital barrier?",
            "Have you ever felt afraid of making a serious mistake - like losing money - while using a device?",
            "Do you ever feel that you are somehow ‘too old’ for this city now?",
            "Has any of this affected your sleep, appetite, or overall mood?",
            "Do you feel like a burden to your family or caregivers when you need help with digital tasks?"
          ]
        },
        {
          code: "T4",
          title: "Social Connections, Family Dynamics, and Intergenerational Bonds",
          openingQuestion: "Who do you usually turn to when you need help with something digital? How often does that happen and how does that interaction feel?",
          probes: [
            "How does that person typically respond when you ask for help - are they patient, impatient, or indifferent?",
            "Have there been times when you felt reluctant to ask for help again from the same person? Why?",
            "Do you feel the technology gap has changed the way younger family members interact with you?",
            "Are there other older people your age in your area who face similar difficulties? Do you discuss this?"
          ],
          citySpecificProbes: {
            "AZ": ["In Mizoram, community bonds through church and YMA are very strong. Does your church community or local council actively help older members navigate city services, including digital ones?"],
            "SG": ["Do mohalla committees or mosque-based community structures in your area help older residents access government services?"],
            "ND": ["This city has a very large population of elderly migrants. Do you feel connected to any community network from your place of origin that helps you here?"]
          }
        },
        {
          code: "T5",
          title: "Rights Awareness, Dignity, and Policy Consciousness",
          openingQuestion: "Do you feel that as an older citizen who has contributed to this country, you have the right to access all city services in a way that works for you, including offline alternatives?",
          probes: [
            "Do you think the government has a responsibility to make sure services are accessible to people who cannot use digital devices?",
            "Have you ever made a formal complaint about being excluded from a service because of a digital barrier?",
            "Have you heard of the Maintenance and Welfare of Parents and Senior Citizens Act?",
            "Do you think that law should be updated to protect older people from being excluded by digital city infrastructure?"
          ]
        }
      ]
    },

    // ── 3. SSI-MoHUA/SPV-v1.0 (Qualitative) ──────────────────────────────────
    {
      code: "SSI-MoHUA/SPV-v1.0",
      title: "MoHUA Officials and SPV Officers",
      stakeholder: "Government / Policy Makers",
      type: "QUALITATIVE",
      consentCode: "CONSENT-KI",
      description: "Semi-structured interview with Ministry of Housing and Urban Affairs and Smart City Special Purpose Vehicle officials on universal design, geriatric usability, and age-friendly urban governance.",
      themes: [
        {
          code: "T1",
          title: "Design Philosophy and Stakeholder Consultation",
          openingQuestion: "Could you walk us through how a new Smart City project is conceptualized, designed, and approved in your city or ministry - from initial idea to implementation?",
          probes: [
            "At which stages are end-users consulted, and who formally represents elderly or differently-abled residents?",
            "Is there a mandatory checklist or criterion related to accessibility or universal design that every project must satisfy?",
            "Has the age profile of potential users ever been a formal input into design specifications?",
            "Are you aware of any SCM project that was retrofitted after deployment because elderly users found it inaccessible?"
          ]
        },
        {
          code: "T2",
          title: "Understanding and Data on Digital Exclusion",
          openingQuestion: "From your professional observation, how do elderly residents (above 75) interact with digital services introduced under SCM in your city?",
          probes: [
            "Has your office ever conducted a formal user survey or needs assessment specifically focused on elderly residents and digital access?",
            "Do you receive complaints or grievances from elderly residents regarding inability to access digital services? How are they handled?",
            "Does your ICCC capture age-disaggregated usage data for any digital services?"
          ]
        },
        {
          code: "T3",
          title: "Legal and Policy Framework - Convergence",
          openingQuestion: "How does your city or ministry coordinate with the National Policy for Older Persons and the Senior Citizens Act in practice?",
          probes: [
            "Is there any formal convergence mechanism between MoHUA and the Ministry of Social Justice and Empowerment?",
            "How does SCM policy engage with Article 21 (right to dignity) and Article 41 (public assistance) regarding digital gatekeeping?",
            "Do you think the statutory definition of 'senior citizen' is adequate to capture the distinct vulnerabilities of the oldest old (75-85)?"
          ]
        },
        {
          code: "T4",
          title: "Monitoring, Accountability, and Data Systems",
          openingQuestion: "What mechanisms exist within your city or ministry to systematically verify whether smart city services reach all population groups, including the oldest old?",
          probes: [
            "What happens institutionally when a project is found to functionally exclude a specific population group?",
            "What data would you need - that you currently do not have - to make genuinely age-inclusive design decisions?"
          ]
        },
        {
          code: "T5",
          title: "Forward Vision - Age-Centric Redesign",
          openingQuestion: "If you were given a specific mandate and resources to redesign your city's SCM projects with the 75-85 age cohort as primary personas, what would change most significantly?",
          probes: [
            "What institutional or budgetary barriers currently prevent age-inclusive design?",
            "What single policy change at the central government level would most effectively drive cities toward age-inclusive digital infrastructure?"
          ]
        }
      ]
    },

    // ── 4. SSI-UP-v1.0 (Qualitative) ────────────────────────────────────────
    {
      code: "SSI-UP-v1.0",
      title: "Urban Planners and Interface Designers",
      stakeholder: "Urban Planners & UI/UX Designers",
      type: "QUALITATIVE",
      consentCode: "CONSENT-KI",
      description: "In-depth technical interview examining accessibility standards, WCAG compliance, geriatric usability testing, and universal interface design in smart city projects.",
      themes: [
        {
          code: "T1",
          title: "Accessibility and Heuristic Usability",
          openingQuestion: "Please start by telling me about your professional role and which specific SCM projects in your city you have been involved in designing or implementing.",
          probes: [
            "What were the stated user requirements when you began designing the project, and who defined them?",
            "Were explicit accessibility standards (WCAG, RPWD Act provisions) required to be followed?",
            "Were elderly users above 70 specifically discussed, or was geriatric usability testing conducted?",
            "If a 78-year-old with mild vision impairment and tremors attempts to use your interface today, what friction points do they encounter?",
            "What prevented more age-inclusive design from being incorporated - budget, client brief, timeline pressure?"
          ]
        }
      ]
    },

    // ── 5. SSI-ICCC-v1.0 (Qualitative) ──────────────────────────────────────
    {
      code: "SSI-ICCC-v1.0",
      title: "ICCC Operators and Technology Service Providers",
      stakeholder: "ICCC Engineers & Tech Providers",
      type: "QUALITATIVE",
      consentCode: "CONSENT-KI",
      description: "Operational review of Integrated Command and Control Center monitoring, telemetry, interaction failure rates, biometric gatekeeping, and offline backup pathways.",
      themes: [
        {
          code: "T1",
          title: "ICCC Monitoring and Digital Gatekeeping",
          openingQuestion: "Please describe your role at the ICCC and which city services the ICCC manages or monitors in your city.",
          probes: [
            "Does the ICCC capture demographic or age-related data on who interacts with city digital services?",
            "When a citizen is unable to complete a digital interaction (at a kiosk, portal, or app), what alternative pathway are they directed to?",
            "Are there AI, voice recognition, or biometric systems with known failure points or exclusion patterns for older residents?",
            "In your informed estimate, what percentage of public-facing SCM services currently have NO viable offline backup option?"
          ]
        }
      ]
    },

    // ── 6. FI-MCO-v1.0 (Qualitative) ────────────────────────────────────────
    {
      code: "FI-MCO-v1.0",
      title: "Municipal Ward-Level Officials",
      stakeholder: "Municipal Ward Officers & Staff",
      type: "QUALITATIVE",
      consentCode: "CONSENT-KI",
      description: "Field interview on last-mile municipal delivery, manual backup assistance, counter interactions, and frontline challenges faced by elderly citizens.",
      themes: [
        {
          code: "T1",
          title: "Last-Mile Delivery and Municipal Assistance",
          openingQuestion: "Which key city services in your ward are now primarily or exclusively accessed online or through digital systems?",
          probes: [
            "How often do elderly residents come to your ward office because they cannot access a required service online?",
            "What do ward staff actually do for a resident who cannot use the online system? Is a manual process always available?",
            "In your honest assessment, are SCM digital services in your ward genuinely accessible to a 78-year-old living alone?",
            "What would make the biggest difference for your elderly residents to make municipal services more accessible?"
          ]
        }
      ]
    },

    // ── 7. SSI-OAH-Admin-v1.0 (Mixed) ──────────────────────────────────────
    {
      code: "SSI-OAH-Admin-v1.0",
      title: "Old Age Home Administrators",
      stakeholder: "Old Age Home Administrators",
      type: "MIXED",
      consentCode: "CONSENT-KI",
      description: "Institutional assessment of resident digital capabilities, proxy assistance burden on staff, pension/benefit delivery barriers, and institutional digital needs.",
      themes: [
        {
          code: "T1",
          title: "Resident Profile and Digital Capability Baseline",
          openingQuestion: "Could you tell me about your residents above 75 years - their background, prior exposure to technology, and current digital capabilities?",
          probes: [
            "What proportion of residents above 75 have had prior exposure to digital devices in their working or personal lives?",
            "What are the most common physical or cognitive barriers you observe among oldest residents that affect digital interactions?"
          ]
        },
        {
          code: "T2",
          title: "Digital Access to City Services and Smart City Interaction",
          openingQuestion: "How do your residents access city services - particularly services that are now primarily digital or online?",
          probes: [
            "Who assists residents with accessing pensions, e-health, or government schemes, and how systematically is support provided?",
            "Has any resident in this institution missed a benefit payment because they could not complete an online verification or biometric check?",
            "Does the institution itself require residents to interact with digital systems (biometric attendance, online registration)?"
          ]
        },
        {
          code: "T3",
          title: "Emotional and Psychological Impact on Residents",
          openingQuestion: "From your daily interaction with residents, have you noticed any emotional or psychological effects of digital frustration or exclusion?",
          probes: [
            "Have residents expressed frustration, shame, or distress regarding their inability to use digital systems?",
            "In your observation, does digital exclusion contribute to feelings of isolation, loss of agency, or cognitive withdrawal?"
          ]
        },
        {
          code: "T4",
          title: "Institutional Needs, Recommendations, and Policy Perspectives",
          openingQuestion: "What resources and policy changes would be most helpful for genuinely improving digital access and well-being for your oldest residents?",
          probes: [
            "What specific resources from government or NGOs would meaningfully help your institution?",
            "If you could recommend one legislative or policy change to address digital exclusion in institutional care, what would it be?"
          ]
        }
      ]
    },

    // ── 8. SSI-DLT-v1.0 (Mixed) ────────────────────────────────────────────
    {
      code: "SSI-DLT-v1.0",
      title: "Digital Literacy Trainers",
      stakeholder: "Digital Literacy Trainers & Educators",
      type: "MIXED",
      consentCode: "CONSENT-KI",
      description: "Evaluation of geriatric digital curriculum, pedagogical adaptations for cognitive/motor decline, retention rates, and real-world independent usability.",
      themes: [
        {
          code: "T1",
          title: "Programme Design, Curriculum, and Content",
          openingQuestion: "Tell me about the digital literacy programme you run - who designed it, and was it specifically tailored for older persons (75+)?",
          probes: [
            "What specific skills does the curriculum teach? Does it cover specific local SCM apps, kiosks, and portals?",
            "Does the curriculum account for cognitive and physical limitations (tremors, vision, slower processing)?"
          ]
        },
        {
          code: "T2",
          title: "Barriers to Learning Among Oldest Old Participants",
          openingQuestion: "What have you observed when training participants above 75 years old in your sessions?",
          probes: [
            "Which interface elements (OTPs, touch gestures, CAPTCHAs, multi-step navigation) do oldest old participants find most difficult?",
            "What adaptations in teaching methods have been most effective?",
            "What triggers anxiety or distress during training sessions, and what is your dropout rate for the 75+ age group?"
          ]
        },
        {
          code: "T3",
          title: "Programme Outcomes and Real-World Usability Assessment",
          openingQuestion: "After completing your programme, are participants above 75 actually able to independently access digital services in their daily lives?",
          probes: [
            "Do participants retain the skills learned 3 or 6 months later?",
            "Are city digital interfaces realistically compatible with the skill level achieved at the end of your programme?"
          ]
        },
        {
          code: "T4",
          title: "Recommendations for Ideal Programme Design and System Change",
          openingQuestion: "What would a genuinely effective digital literacy programme for people between 75 and 85 years look like in this city?",
          probes: [
            "What would need to change in the city's own digital infrastructure for training to translate into genuine independent usage?"
          ]
        }
      ]
    },

    // ── 9. SSI-NGO-v1.0 (Qualitative) ──────────────────────────────────────
    {
      code: "SSI-NGO-v1.0",
      title: "NGOs and Civil Society",
      stakeholder: "NGO Leaders & Elder Care Activists",
      type: "QUALITATIVE",
      consentCode: "CONSENT-KI",
      description: "Civil society investigation into field case documentation of digital exclusion, proxy exploitation, advocacy efforts, and legal reform requirements.",
      themes: [
        {
          code: "T1",
          title: "NGO Advocacy, Case Support, and Field Experience",
          openingQuestion: "Tell me about your organisation's work with elderly residents in this city and how digitalization has affected them over the past 5 years.",
          probes: [
            "Has your organisation documented specific cases where oldest old persons were denied a service or benefit due to a digital barrier?",
            "Does your organisation actively help older persons navigate digital civic services? With what resources?",
            "Have you observed differences in digital exclusion across gender, literacy, migration status, or income levels?",
            "Has your organisation engaged with the Smart City SPV or municipal corporation on digital accessibility? What was the outcome?",
            "What do you believe is most critically missing from current law to protect older persons from digital exclusion in public urban spaces?"
          ]
        }
      ]
    },

    // ── 10. SEI-Geri-v1.0 (Qualitative) ────────────────────────────────────
    {
      code: "SEI-Geri-v1.0",
      title: "Geriatricians and Mental Health Professionals",
      stakeholder: "Geriatricians & Psychiatrists",
      type: "QUALITATIVE",
      consentCode: "CONSENT-KI",
      description: "Clinical investigation of geriatric cognitive decline, digital anxiety manifestations, psycho-emotional stress, and interface cognitive load.",
      themes: [
        {
          code: "T1",
          title: "Clinical and Cognitive Impact of Digitalization",
          openingQuestion: "From a clinical and research perspective, how do digital systems and technology-dense environments affect the cognitive and emotional well-being of persons aged 75-85?",
          probes: [
            "What primary cognitive and neuromotor changes affect the ability to interact with modern digital interfaces (touchscreens, QR codes, kiosks)?",
            "How does 'digital anxiety' manifest clinically among oldest patients?",
            "Have you observed cases of psychological deterioration (depression, withdrawal, loss of agency) linked to inability to navigate public digital services?",
            "What design features in digital interfaces are most cognitively or motorically harmful for the oldest old?",
            "What recommendations would you make from a geriatric public health standpoint to policymakers designing digital city infrastructure?"
          ]
        }
      ]
    },

    // ── 11. EI-Legal-Ac-v1.0 (Qualitative) ──────────────────────────────────
    {
      code: "EI-Legal-Ac-v1.0",
      title: "Constitutional Law and Elder Rights Scholars",
      stakeholder: "Legal Scholars & Jurists",
      type: "QUALITATIVE",
      consentCode: "CONSENT-KI",
      description: "Jurisprudential inquiry into Article 21 substantive dignity, Article 41 public assistance, disparate impact doctrine, and statutory amendments under elder law.",
      themes: [
        {
          code: "T1",
          title: "Constitutional Mandates and Legal Rights Frameworks",
          openingQuestion: "How do you interpret the scope of Article 21 (right to life with dignity) and Article 41 (public assistance) regarding an oldest old citizen excluded by mandatory digital gatekeeping?",
          probes: [
            "Can the doctrine of substantive dignity under Article 21 be extended to encompass equitable access to public infrastructure?",
            "Does 'digital-by-default' governance without offline alternatives constitute indirect discrimination under Articles 14 and 21?",
            "What are the precise legislative gaps in the Senior Citizens Act 2007 and the 2019 Amendment Bill regarding digital exclusion?",
            "How do international frameworks (UNCRPD, WHO Age-Friendly Cities) inform Indian constitutional interpretation in this domain?"
          ]
        }
      ]
    },

    // ── 12. SQ-CG + SSI-CG-v1.0 (Mixed) ────────────────────────────────────
    {
      code: "SQ-CG + SSI-CG-v1.0",
      title: "Caregivers and Family Members",
      stakeholder: "Primary Caregivers & Family",
      type: "MIXED",
      consentCode: "CONSENT-KI",
      description: "Dual-method instrument capturing structured caregiver assistance frequency, time burden, relational friction, and lived narrative observations.",
      sections: [
        {
          code: "CG_A",
          title: "Primary Caregiver Profile & Assistance Details",
          questions: [
            { id: "CG1", text: "What is your relationship to the oldest old participant you are being asked about?", inputType: "SINGLE_CHOICE", options: ["Adult son or daughter", "Spouse", "Grandchild", "Sibling or other relative", "Paid caregiver or domestic worker", "Neighbour or friend", "NGO volunteer or social worker", "Other"], isRequired: true },
            { id: "CG2", text: "How many times per week, on average, do you help the older person with digital tasks?", inputType: "SINGLE_CHOICE", options: ["Every day (approx 7 times per week)", "3 to 5 times per week", "1 to 2 times per week", "Rarely - less than once per week", "Never - I do not help with digital tasks"], isRequired: true },
            { id: "CG3", text: "What types of digital tasks do you most commonly assist with? (Select all that apply)", inputType: "MULTI_CHOICE", options: ["Making or receiving phone calls or WhatsApp", "Online bill payment (electricity, water, etc.)", "Government portal use or online form submission", "Booking a medical appointment online", "Banking, UPI, or other digital payment", "Travel or public transport booking", "Accessing Smart City services or apps", "Filing a complaint or grievance online", "Government scheme registration or renewal", "Other"], isRequired: true },
            { id: "CG4", text: "How much total time do you typically spend per week on digital assistance for this person?", inputType: "SINGLE_CHOICE", options: ["Less than 30 minutes per week", "30 to 60 minutes per week", "1 to 3 hours per week", "More than 3 hours per week"], isRequired: true },
            { id: "CG5", text: "How often does the older person express frustration, anxiety, or emotional distress regarding digital systems?", inputType: "SINGLE_CHOICE", options: ["Very often - almost daily or weekly", "Sometimes - a few times per month", "Rarely - once in a while", "Never - they do not express this to me", "They do not discuss technology frustrations with me"], isRequired: true },
            { id: "CG6", text: "In your observation, has the older person reduced the frequency of going out because of digital barriers?", inputType: "SINGLE_CHOICE", options: ["Yes - significantly reduced outings because of digital barriers", "Yes - to some extent reduced outings", "No - I have not noticed a change related to this", "Not sure"], isRequired: true },
            { id: "CG7", text: "Has the older person ever missed a service, benefit, or appointment specifically due to digital barriers?", inputType: "SINGLE_CHOICE", options: ["Yes - this has happened multiple times", "Yes - this happened once", "No - I do not believe this has happened", "Not sure"], isRequired: true },
            { id: "CG8", text: "How does the older person's reliance on you for digital tasks affect your relationship with them?", inputType: "SINGLE_CHOICE", options: ["No effect - it is just a normal part of our relationship", "I sometimes feel frustrated by repeated requests for the same help", "It has occasionally caused tension or conflict between us", "It has brought us closer - a positive shared activity", "I feel sad that they cannot do this independently", "Other"], isRequired: true }
          ]
        }
      ],
      themes: [
        {
          code: "CG_B",
          title: "Primary Caregiver Lived Experience Narrative",
          openingQuestion: "Based on your regular interaction, how do digital systems in this city affect the older person's daily life and your caregiving role?",
          probes: [
            "Can you describe a specific recent incident where you had to step in to help with a digital task they could not manage?",
            "How does the older person react emotionally when encountering a digital system they cannot use?",
            "Do you notice changes in their mood, self-confidence, or independence that you attribute to digital exclusion?",
            "In your honest view, do the Smart City services in your city work for people of their age and background?"
          ]
        }
      ]
    },

    // ── 13. FO-OOP-v1.0 (Observational) ────────────────────────────────────
    {
      code: "FO-OOP-v1.0",
      title: "Field Observation and Shadowing",
      stakeholder: "Oldest Old Citizens (Naturalistic Shadowing)",
      type: "OBSERVATIONAL",
      consentCode: "CONSENT-OOP-W",
      description: "Direct shadowing protocol recording physical mobility friction, facility navigation, digital kiosk encounters, QR scanning, and heuristic interface usability.",
      themes: [
        {
          code: "T1",
          title: "Naturalistic Shadowing of SCM Interactions",
          openingQuestion: "Observe and systematically record details of physical mobility, route choices, SCM gatekeeping, and digital interface encounters.",
          probes: [
            "Physical Mobility: Walking pace, walking aids, uneven surfaces, steps, ramps.",
            "Facility Accessibility: Entrance steps, ramps, signage, digital gate check (QR code, biometric scanner).",
            "Staff Interaction: Staff presence, helpfulness, insistence on app/digital vs manual alternatives.",
            "Digital Interfaces: Engage kiosk, QR scan, app, elapsed interaction time, emotional reaction, helper involvement.",
            "Heuristic Assessment: Heuristic scoring of text size, local language suitability, touchscreen responsiveness, offline fallback options."
          ]
        }
      ]
    },

    // ── 14. FGD-OOP/CG-v1.0 (Qualitative Focus Group) ──────────────────────
    {
      code: "FGD-OOP/CG-v1.0",
      title: "Focus Group Discussion - Older Persons & Caregivers",
      stakeholder: "Older Citizens & Caregivers (Combined Group)",
      type: "QUALITATIVE",
      consentCode: "CONSENT-OOP-W",
      description: "Structured group discussion format covering collective perceptions of smart urban transformation, shared exclusion experiences, and co-designed solutions.",
      themes: [
        {
          code: "T1",
          title: "Ground Rules & Ice-Breaker",
          openingQuestion: "Thank you all for coming today. We are here to listen and learn about how life in this city has been changing for older people.",
          probes: [
            "Ground Rules: One person speaks at a time, what is shared stays here, all perspectives are valued.",
            "Ice-Breaker: First name, years living in this city, and one activity you personally enjoy doing here."
          ]
        },
        {
          code: "T2",
          title: "Transition Questions",
          openingQuestion: "When you need to do something ordinary - pay a bill, visit a doctor, take transport, visit a park - what does that actually involve these days?",
          probes: [
            "What specific services have moved online or to apps or digital machines?",
            "Was there a particular moment when you first realized things had changed significantly?",
            "Has anything become easier for you personally because of these changes?"
          ]
        },
        {
          code: "T3",
          title: "Key Discussion Questions & Co-Design",
          openingQuestion: "Which services feel most difficult or impossible to access these days? Why do you think that is?",
          probes: [
            "Emotional impact: how do you feel when you are unable to do something on your own?",
            "Social connections: does the tech gap affect relationships with younger generations?",
            "Solutions: what would a truly age-friendly smart city look and feel like to you?"
          ]
        }
      ]
    },

    // ── 15. DOCTRINAL-v1.0 (Doctrinal Analysis) ────────────────────────────
    {
      code: "DOCTRINAL-v1.0",
      title: "Legal and Policy Document Analysis",
      stakeholder: "Principal Investigators / Legal Research Associates",
      type: "DOCTRINAL",
      consentCode: "CONSENT-KI",
      description: "Rigorous doctrinal analysis matrix evaluating statutes, judicial precedents, policy circulars, Smart City guidelines, and Article 21/41 constitutional mandates.",
      themes: [
        {
          code: "T1",
          title: "Substantive Policy Analysis Matrix",
          openingQuestion: "Conduct structural doctrinal analysis of constitutional provisions, statutes, amendment bills, judgments, and executive policies.",
          probes: [
            "Definition of senior citizen / oldest old (75+) in legislation and urban planning guidelines.",
            "Definition of welfare and dignity (coverage of public space and digital access).",
            "Obligations of the state vs private concessionaires for accessible public service delivery.",
            "Doctrinal analysis of digital exclusion as a violation of fundamental rights under Articles 14, 19, and 21.",
            "Enforcement mechanisms, accessibility of grievance redressal, and proposed statutory amendments."
          ]
        }
      ]
    }
  ],

  // Helper: Find instrument by code
  getInstrument(code) {
    if (!code) return null;
    const clean = code.trim();
    return this.instruments.find(inst => inst.code === clean || inst.code.startsWith(clean)) || null;
  }
};

// Freeze instrument bank to ensure structural integrity
Object.freeze(INSTRUMENT_BANK);
if (typeof window !== 'undefined') window.INSTRUMENT_BANK = INSTRUMENT_BANK;

