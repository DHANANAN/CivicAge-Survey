# Frontend.md

## CivicAge Survey (CAS) Frontend Specification

**Project:** Smart Cities, Silent Citizens - ICSSR Major Research Project 2025-26  
**App:** CivicAge Survey (CAS)  
**Platform:** Native Android  
**Primary users:** Field investigators, supervisors, project administrators, transcribers, and analysts  
**Research participants:** Oldest old persons aged 75+, caregivers, institutional representatives, experts, planners, ICCC operators, NGO workers, geriatricians, and legal scholars

This document converts the full ICSSR data collection framework into a native Android frontend workflow.

---

## 1. Frontend Goals

The app must support fieldwork with very old participants while also helping field investigators follow strict research ethics. The interface should not feel like a fast commercial survey app. It should feel slow, clear, readable, consent-based, and safe.

Core goals:

1. Reduce participant fatigue.
2. Make consent impossible to skip.
3. Keep every instrument modular.
4. Support read-aloud administration.
5. Work offline during fieldwork.
6. Use large fonts and simple navigation.
7. Avoid hidden validation errors.
8. Support multiple city languages.
9. Integrate QA checks before final sync/export.
10. Make the app usable by trained field investigators, not by participants directly.

---

## 2. App Information Architecture

### 2.1 Main bottom navigation

```text
Home | Instruments | Records | Sync | QA | Settings | Profile
```

### 2.2 Home dashboard cards

```text
Total Records
Pending Drafts
Completed Sessions
Pending Sync
Pending QA
Audio Pending Deletion
City Assignment
Today’s Fieldwork Plan
```

### 2.3 Instrument launcher

```text
Start New Session
  -> Select City
  -> Select Stakeholder
  -> Select Consent Type
  -> Complete Consent
  -> Select Instrument
  -> Run Instrument
  -> Session Close
  -> FI Observation / Reflexivity Log
  -> Save Draft or Complete
```

---

## 3. Native Android UI Standards

### 3.1 Visual style

| Element | Standard |
|---|---|
| Body text | Minimum 18sp for read-aloud question screen; 16sp minimum elsewhere. |
| Question text | 20sp to 22sp, bold only for key terms. |
| Response options | Large tap targets, minimum 48dp height, preferably 56dp. |
| Buttons | Full-width primary buttons at bottom. |
| Colour | High contrast. Avoid pale grey text. |
| Icons | Use simple icons only where they clarify meaning. |
| Screen density | One major question per screen for OOP tools. |
| Error messages | Plain language, not technical. |

### 3.2 Interaction rules

- Use `Next`, `Back`, `Pause`, `Save Draft`, and `End Session` buttons consistently.
- Avoid swipe-only navigation.
- Always show progress by section, not by raw question count.
- Use confirmation dialogs before ending or submitting a session.
- Never auto-submit after the final question without FI review.

---

## 4. Field Investigator Workflow

### 4.1 Login screen

Fields:

```text
Full Name
Email
Surveyor ID
City Assignment
Role
```

Validation:

- Email must be valid.
- Surveyor ID must match assigned city prefix where possible.
- App should display the user role after login.

### 4.2 Home screen quick actions

```text
Start OOP Questionnaire
Start OOP Interview
Start Caregiver Tool
Start Expert / Key Informant Tool
Start Observation
Start FGD
Start Doctrinal Review
Continue Draft
Open QA Queue
Sync Pending Records
```

---

## 5. Consent Workflows

Consent must be the first active workflow for every participant or institution.

### 5.1 Consent selector

```text
Who is giving consent?
  [ ] Oldest Old Participant - Written Consent
  [ ] Oldest Old Participant - Oral Consent
  [ ] Institution / Old Age Home / Community Organisation - NOC
  [ ] Expert / Key Informant
```

### 5.2 Written consent - OOP

Screens:

1. Study title and field investigator introduction.
2. What the study is about.
3. What the participant will be asked to do.
4. Privacy and participant code explanation.
5. Audio recording choice.
6. Risks and right to skip.
7. Voluntary participation and withdrawal rights.
8. Signature / thumb impression capture.
9. Consent summary and lock.

Fields:

```text
Participant Code
Age
Gender
City
Consent Language
Signature or Left Thumb Impression
Consent to Audio Recording: Yes / No
Consent to Geo-tagging: Yes / No
Field Investigator Name and Code
Consent Timestamp
```

### 5.3 Oral consent - low literacy / cognitive impairment version

Screens:

1. Local greeting and plain-language introduction.
2. Read-aloud script.
3. Permission question: “Do I have your permission to ask questions today?”
4. Verbal assent capture.
5. Proxy consent marker if required.
6. Audio recording choice for interview.
7. Consent lock.

Controls:

```text
Start Assent Recording
Stop Assent Recording
Mark Verbal Yes
Mark No / Confused / Reluctant
Do Not Proceed
```

Rules:

- If participant says “No”, appears confused, or appears reluctant, the app must show `Do Not Proceed`.
- Oral assent audio ID must be attached to the consent record.
- Proxy consent must be flagged, not hidden in notes.

### 5.4 Institutional NOC

Screens:

1. Institution details.
2. Permission scope.
3. Conditions.
4. Authorised signatory.
5. Institutional seal/photo/document attachment, if permitted.
6. Anonymisation choice.

Institution NOC must not unlock individual participant tools unless individual participant consent is separately completed.

### 5.5 Expert / key informant consent

Applicable to:

```text
MoHUA / SPV officials
Urban planners
Interface designers
ICCC operators
Municipal officials
Old age home administrators
Digital literacy trainers
NGO representatives
Geriatricians
Mental health professionals
Legal scholars
```

Fields:

```text
Name optional
Designation
Organisation
City
Audio Recording Consent
Permission to Name Institution: Yes / No
Permission to Quote with Attribution: Yes / No
Signature
Interview Code
```

Default setting: anonymise identity and institutional affiliation unless explicit written permission is recorded.

---

## 6. Questionnaire Modules - SQ-OOP-v1.0

The oldest old questionnaire must be read aloud by the field investigator. Self-administration should not be offered for OOP respondents.

### 6.1 Opening metadata screen

Fields:

```text
Participant ID
Date of Interview
City
Start Time
Field Investigator Code
Location Type: Community-dwelling / OAH-Government / OAH-Private / Other
Neighbourhood / Ward
Break Given at 25 Minutes: Yes / No
```

### 6.2 Section A - Socio-demographic profile

UI type: mostly single-choice and short text.

Topics:

```text
Age
Gender
Mother tongue / primary language
Education
Living arrangement
Monthly income source
Household income - optional / skippable to avoid fatigue
Duration of residence in city
General health - optional / skippable
Sensory / mobility / memory indicators - optional / skippable
Caregiving support - optional / skippable
```

Design requirement:

- Use “Prefer not to say” wherever sensitive.
- Add `S - Skipped` button with reason.
- Do not force income or health answers.

### 6.3 Section B - Digital device ownership and exposure

UI requirement:

- Show digital device reference card with pictures of basic phone, smartphone, computer, tablet, kiosk.
- Do not assume the participant understands “smartphone”.

Topics:

```text
Personal phone ownership
Phone use patterns
Who operates devices for participant
Computer / laptop / tablet access
Frequency of independent digital use
Past technology exposure
Confidence in using smartphone or kiosk
Internet connection at home / old age home
```

### 6.4 Section C - Smart City Mission service awareness and usage

City-specific service cards must be loaded before field deployment.

Example service mapping:

| City | Example service cards |
|---|---|
| New Delhi | Delhi Metro app, Delhi One app, EDMC e-services, Mohalla Clinic app, smart parking. |
| Srinagar | Smart bus shelters, Wi-Fi hotspots, ICCC grievance portal, smart waste management. |
| Aizawl | Smart solid waste tracking, sewerage monitoring portal, ICCC civic services. |
| Chennai | Smart road services, e-bus tracking app, digital health centres, smart parks. |
| Gandhinagar | iNagar app, GIFT City infrastructure services, smart health kiosks. |
| Indore | 311 Smart City app, BRTS digital booking, ICCC grievance system, digital waste compliance. |

Service grid fields:

```text
Heard of it?
Tried to use it?
Used successfully alone?
Used with someone’s help?
Never used or gave up?
```

Additional items:

```text
Primary reason for failed use
Help needed in past month
Online-only denial of service
Response after denial
Missed / delayed service in past six months
Avoided public place due to digital barrier
Paid middleman / cyber cafe for free civic service
```

---

## 7. Remaining OOP Questionnaire Sections

Although the immediate frontend build may start with Sections A-C, the final CAS workflow must support all SQ-OOP sections.

| Section | Module name | UI type |
|---|---|---|
| D | Digital Anxiety Scale | 5-point Likert with read-aloud scale card. |
| E1 | Digital Dependency Frequency Index | Single-choice frequency and dependency items. |
| E2 | Dignity Perception Scale | 5-point Likert with reverse scoring in analysis only. |
| F | Social Isolation and Intergenerational Impact | Frequency and perception items. |
| G | Digital Literacy Programme Exposure and Welfare Scheme Awareness | Yes/no, multiple-choice, programme exposure. |
| H | FI Observation Log | Completed by FI after session, not in front of participant. |

---

## 8. Break Alerts and Fatigue Detection

### 8.1 Mandatory break alert

Trigger:

```text
At 25 minutes from session start for SQ-OOP.
```

UI copy:

```text
It has been 25 minutes. Please pause and offer the participant a break.

Options:
[Break Accepted]
[Participant Wants to Continue]
[Pause Session]
[End Session]
```

### 8.2 Maximum duration warning

Trigger:

```text
At 55 minutes for SQ-OOP.
At 60 minutes for SSI-OOP.
At 75 minutes for SSI-OOP hard warning.
```

### 8.3 Fatigue detection checklist

After each section and whenever the FI taps `Participant looks tired`, show:

```text
Observed sign:
[ ] Long silence
[ ] Confusion
[ ] Repeated re-explanation needed
[ ] Visible distress
[ ] Participant requests pause
[ ] Participant looks physically tired
[ ] Irritation / anxiety
[ ] Hearing or visual strain
```

App action:

```text
Offer break -> Resume / Skip sensitive section / End session / Escalate to PI if distress persists
```

---

## 9. Interview Modules

### 9.1 SSI-OOP-v1.0

Workflow:

```text
Confirm 2-day gap from SQ-OOP
Confirm consent
Confirm audio choice
Pre-interview checklist
Theme 1: Life before and after digital transformation
Theme 2: Specific experiences with smart city services
Theme 3: Emotional experience, digital anxiety, and mental well-being
Theme 4: Social connections, family dynamics, intergenerational bonds
Theme 5: Rights awareness, dignity, and policy
Closing protocol
Post-interview reflexivity and quality log
```

UI format:

- Interview guide screen with expandable probes.
- `Mark Asked`, `Mark Skipped`, `Add Note`, `Add Quote`, and `Flag Distress` controls.
- Optional audio recorder only if consent exists.
- 60-minute alert.

### 9.2 Expert and key informant interviews

Tool groups:

```text
SSI-MoHUA/SPV-v1.0
SSI-UP-v1.0
SSI-ICCC-v1.0
FI-MCO-v1.0
SSI-OAH-Admin-v1.0
SSI-DLT-v1.0
SSI-NGO-v1.0
SEI-Geri-v1.0
EI-Legal-Ac-v1.0
```

UI format:

- Expert profile page.
- Consent page.
- Interview theme pages.
- Document request checklist.
- Audio consent toggle.
- Post-interview notes.

---

## 10. Caregiver Module - SQ-CG + SSI-CG-v1.0

Rules:

- Caregiver tool must be administered separately from OOP sessions.
- OOP participant must not be present during caregiver interview.
- Caregiver must not be present during OOP interview.

Workflow:

```text
Select linked OOP participant code
Confirm separate consent
Part A: Caregiver structured questionnaire
Part B: Short semi-structured interview
Save relationship and digital support burden indicators
```

Main structured topics:

```text
Relationship to OOP participant
Frequency of digital help
Types of digital tasks assisted
Time spent weekly
Observed frustration / anxiety
Reduced outings due to digital barriers
Missed service or entitlement
Effect on relationship
```

---

## 11. FGD Module

Workflow:

```text
Create FGD code
Select city
Select group type: OOP / Caregiver / Mixed
Record venue and moderator details
Attach individual consent records
Run phased guide
Capture activity outputs
Record group findings
Complete moderator QA note
```

UI requirements:

- Do not display individual names in FGD screen after consent.
- Use participant codes only.
- Add group note and “do not attribute to individual” warning.
- Audio recording allowed only if all participants consent.

---

## 12. Observation and Shadowing Module - FO-OOP-v1.0

Workflow:

```text
Separate observation consent
Confirm real destination and real purpose
Pre-observation checklist
Route and physical mobility observation
Digital interaction log
Staff interaction log
Accessibility heuristic
Session close and FI reflection
```

Controls:

```text
Start Timer
Add Interface Encounter
Add Staff Interaction
Mark Participant Distress
Mark FI Intervention
End Observation
```

Ethical alert:

```text
The FI must not intervene unless the participant asks to stop or there is physical danger.
```

---

## 13. Doctrinal Template Module

Used by PI/RA, not field investigators.

Workflow:

```text
Select document type
Add citation/source details
Summarise legal/policy provision
Map to research objective
Add elder-rights relevance
Add smart-city governance relevance
Add accessibility obligation
Add synthesis memo
```

Document types:

```text
Statute
Policy
Scheme
Guideline
Court judgment
Government report
International framework
Municipal document
SPV document
```

---

## 14. QA Module

QA should be visible to supervisors and admins.

QA queue filters:

```text
City
Field Investigator
Instrument Type
Consent Missing
Audio Pending Deletion
Break Not Recorded
Fatigue Flag
Transcript Pending Review
Low Data Reliability
Protocol Violation
```

QA checklist actions:

```text
Approve
Return for clarification
Escalate to PI
Exclude from analysis
Mark withdrawal processed
```

---

## 15. Multilingual Adaptation

### 15.1 Required city languages

| City | Primary app language support |
|---|---|
| New Delhi | Hindi, English, Urdu optional. |
| Srinagar | Kashmiri, Urdu, Hindi, English. |
| Aizawl | Mizo, English, Hindi optional. |
| Chennai | Tamil, English. |
| Gandhinagar | Gujarati, Hindi, English. |
| Indore | Hindi, English. |

### 15.2 Language implementation

Store strings in Android resources:

```text
res/values/strings.xml
res/values-hi/strings.xml
res/values-ur/strings.xml
res/values-ta/strings.xml
res/values-gu/strings.xml
res/values-miz/strings.xml
res/values-ks/strings.xml
```

### 15.3 Language metadata

Every record must store:

```json
{
  "instrumentLanguage": "Hindi",
  "participantPreferredLanguage": "Hindi",
  "translatorPresent": false,
  "translatorCode": null
}
```

---

## 16. Accessibility Features

### 16.1 Participant accessibility

- Large print mode.
- Read-aloud question mode.
- Audio playback of consent script.
- Visual response cards.
- High contrast theme.
- Thumb-print signing.
- Proxy consent flag.
- Skip option on sensitive questions.
- No small checkboxes for OOP screens; use large cards.

### 16.2 Field investigator accessibility

- Offline drafts.
- Resume interrupted session.
- Section-level progress.
- Protocol warnings.
- In-app glossary of tool codes.
- City-specific service cards.
- Quick distress protocol.

---

## 17. Thumb-print Signing

Screen requirements:

```text
Participant Code
Consent Type
Signature / Thumb Impression Canvas
Mark type: Signature / Left Thumb Impression / Unable to sign
Witness required: Yes / No
Field Investigator confirmation
Timestamp
```

Storage:

- Save as local encrypted image until sync.
- Upload to restricted consent signatures storage path.
- Do not include signature image in analysis export.

---

## 18. Proxy Consent Handling

Proxy consent should be available only when ethically required and approved in field protocol.

Fields:

```text
Proxy consent required: Yes / No
Proxy name
Proxy relation
Reason proxy consent is required
Participant verbal assent present: Yes / No
Field investigator note
```

Rule:

```text
Proxy consent cannot replace the participant’s own assent where the participant can clearly assent or refuse.
```

---

## 19. Offline Mode

Offline banner copy:

```text
You are offline. Records are saved securely on this device and will sync when internet is available.
```

Offline capabilities:

- Start and complete consent.
- Start and complete questionnaires.
- Record notes.
- Save audio locally if consent exists.
- Save observation logs.
- Queue sync.

Online-only capabilities:

- Cloud transcription.
- Admin export.
- Final dashboard refresh.
- Cloud QA assignment.

---

## 20. Frontend Acceptance Checklist

- [ ] Consent cannot be bypassed.
- [ ] Written, oral, institutional NOC, and expert consent workflows exist.
- [ ] OOP questionnaire is read-aloud and large-font.
- [ ] Section A-C modules are city-adapted.
- [ ] Remaining Sections D-H are represented.
- [ ] Mandatory 25-minute break alert appears.
- [ ] Fatigue and distress can be flagged at any point.
- [ ] Audio recording is optional and consent-linked.
- [ ] SQ-OOP and SSI-OOP are not presented in the same session.
- [ ] Caregiver tool is separate from OOP tool.
- [ ] FGD supports individual consent tracking.
- [ ] Observation protocol includes non-intervention warning.
- [ ] QA queue exists.
- [ ] Six-city multilingual support is planned in resource files.
- [ ] Signature, thumb impression, and proxy consent workflows are available.
