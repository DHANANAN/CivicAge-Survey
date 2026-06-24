# Frontend.md

## CivicAge Survey (CAS) Frontend Specification

**Project:** Smart Cities, Silent Citizens - ICSSR Major Research Project 2025-26  
**Platform:** Native Android  
**Purpose:** Convert all ICSSR data collection tools into a safe, accessible, multilingual fieldwork workflow for the oldest old population and related stakeholders.

---

## 1. Core UI Principles

1. **Slow by design:** The app must support read-aloud administration and should never rush an oldest old participant.
2. **Large and readable:** Use minimum 18sp for OOP-facing question text, 16sp minimum elsewhere, high contrast, and large tap targets.
3. **Consent-first:** No instrument can open until the correct consent workflow is completed and locked.
4. **One major action per screen:** Especially for OOP questionnaire, consent, and fatigue-sensitive modules.
5. **Offline-first:** Drafts, consent, answers, observation notes, and audio can be saved locally and synced later.
6. **Research ethics visible:** Every screen should make it easy to pause, skip, withdraw, or end the session.
7. **City-language ready:** All instruments marked “all cities” must be available in language-adapted versions.

---

## 2. Main App Navigation

```text
Login
  -> Home Dashboard
    -> Instruments
    -> Records
    -> Sync / Export
    -> QA Queue
    -> Settings
    -> Profile
```

Recommended bottom tabs:

```text
Home | Instruments | Records | Sync | QA | Settings | Profile
```

Home cards:

```text
Completed Sessions | Drafts | Pending Sync | Pending QA | Audio Pending Deletion | City Assignment
```

---

## 3. Consent Workflows

### 3.1 Consent selector

```text
Select consent type:
[ ] Written Consent - Oldest Old Participant
[ ] Oral Consent - Low Literacy / Cognitive Impairment
[ ] Institutional NOC - Old Age Home / Community Organisation
[ ] Expert / Key Informant Consent
```

### 3.2 Written consent - OOP

Screens:

1. Study title and investigator introduction.
2. What the study is about.
3. What the participant will be asked to do.
4. Privacy and participant code explanation.
5. Audio recording choice.
6. Minimal risk, right to skip, and right to stop.
7. Voluntary participation and withdrawal rights.
8. Signature / left thumb impression capture.
9. Final consent review and lock.

Required fields:

```text
Participant Code
Age
Gender
City
Preferred Language
Consent to Audio Recording: Yes / No
Consent to Geo-tagging: Yes / No
Signature / Left Thumb Impression
Field Investigator Code
Consent Timestamp
```

### 3.3 Oral consent

Use for low literacy or cognitive impairment situations. The app should show the oral script in the selected language and allow audio recording of verbal assent.

Controls:

```text
Start Assent Recording
Stop Assent Recording
Participant says YES
Participant says NO / unclear / reluctant
Do Not Proceed
Proxy Consent Required: Yes / No
```

Rule:

```text
If the participant refuses, appears confused, or remains unclear after one repeat, the app must block data collection.
```

### 3.4 Institutional NOC

Screens:

```text
Institution details
Permission scope
Conditions
Authorised signatory details
Institutional anonymisation confirmation
NOC document/photo attachment if permitted
```

Institutional NOC does not replace individual participant consent.

### 3.5 Expert / key informant consent

Applies to MoHUA/SPV officials, planners, ICCC operators, municipal officials, OAH administrators, digital literacy trainers, NGO representatives, geriatricians, mental health professionals, and legal scholars.

Default setting:

```text
Identity anonymised unless explicit written naming permission is given.
```

---

## 4. Instrument Launcher

```text
Start New Session
  -> Select City
  -> Select Stakeholder
  -> Select Participant / Institution Code
  -> Complete or verify consent
  -> Select Instrument
  -> Run Instrument
  -> Complete FI log / reflexivity log
  -> Save Draft or Submit for QA
```

Instrument categories:

```text
Consent Forms
OOP Structured Questionnaire
OOP Semi-Structured Interview
Expert / Key Informant Interviews
Old Age Home Administrator Interview
Digital Literacy Trainer Interview
NGO / Geriatrician / Legal Expert Interviews
Caregiver Questionnaire + Interview
Field Observation / Shadowing
Focus Group Discussion
Doctrinal Template
QA Checklists
Annexures
```

---

## 5. SQ-OOP-v1.0 Questionnaire UI

Administration mode:

```text
Read aloud by Field Investigator. Self-administration is not appropriate for OOP participants.
```

### 5.1 Session metadata

```text
Participant ID
Date
City
Start Time
End Time
Field Investigator Code
Location Type: Community-dwelling / OAH-Government / OAH-Private / Other
Neighbourhood / Ward
Break Given at 25 Minutes: Yes / No
```

### 5.2 Sections

| Section | Content | UI requirement |
|---|---|---|
| A | Socio-demographic profile | Single-choice, short text, skip option for fatigue-sensitive questions. |
| B | Digital device ownership and exposure | Show device reference cards; do not assume “smartphone” knowledge. |
| C | Smart City Mission services | City-specific service cards and usage grid. |
| D | Digital Anxiety Scale | 5-point Likert scale with read-aloud response card. |
| E1 | Digital Dependency Index | Frequency and dependency questions. |
| E2 | Dignity Perception Scale | 5-point Likert; reverse scoring only in analysis. |
| F | Social isolation and intergenerational impact | Frequency and perception items. |
| G | Digital literacy programme and welfare schemes | Programme exposure and scheme awareness. |
| H | FI observation log | Completed after session, not in front of participant. |

### 5.3 City-specific Section C service examples

| City | Service cards to configure before fieldwork |
|---|---|
| New Delhi | Delhi Metro app, Delhi One app, EDMC e-services, Mohalla Clinic app, smart parking. |
| Srinagar | Smart bus shelters, Wi-Fi hotspots, ICCC grievance portal, smart waste management. |
| Aizawl | Smart solid waste tracking, sewerage monitoring portal, ICCC civic services. |
| Chennai | Smart road services, e-bus tracking app, digital health centres, smart parks. |
| Gandhinagar | iNagar app, GIFT City infrastructure services, smart health kiosks. |
| Indore | 311 Smart City app, BRTS digital booking, ICCC grievance system, digital waste compliance. |

Grid columns:

```text
Heard of it? | Tried to use it? | Used successfully alone? | Used with help? | Never used / gave up?
```

---

## 6. Break Alerts and Fatigue Detection

### 6.1 Mandatory 25-minute break

Trigger at 25 minutes for SQ-OOP.

Popup:

```text
It has been 25 minutes. Please pause and offer the participant a break.

[Break Accepted]
[Participant Wants to Continue]
[Pause Session]
[End Session]
```

### 6.2 Fatigue flag button

Visible on all OOP tools:

```text
Participant looks tired / distressed
```

Checklist:

```text
[ ] Confusion
[ ] Long silence
[ ] Repeated re-explanation needed
[ ] Physical discomfort
[ ] Emotional distress
[ ] Hearing / visual strain
[ ] Participant requested break
[ ] Participant asked to stop
```

Actions:

```text
Offer Break | Skip Question | End Session | Refer to PI / Supervisor
```

---

## 7. SSI-OOP-v1.0 Interview UI

Rules:

- Minimum 2-day gap from SQ-OOP.
- Audio recording only if separately consented.
- Schedule preferably 9:00 AM-12:00 PM.
- 60-minute alert should appear.

Theme screens:

```text
T1 Life before and after digital transformation
T2 Specific experiences with smart city services
T3 Emotional experience, digital anxiety, mental well-being
T4 Social connections, family dynamics, intergenerational bonds
T5 Rights awareness, dignity, and policy
```

Controls:

```text
Mark Asked
Mark Skipped
Add Note
Add Verbatim Quote
Flag Distress
Pause Recording
End Interview
```

Post-interview log:

```text
Dominant themes
Notable verbatim quotes
Digital barriers raised spontaneously? Yes / No
FI reflexivity note
Data quality assessment
Follow-up or referral needed
```

---

## 8. Expert, Institutional, and Stakeholder Interview UI

Supported tools:

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

Common layout:

```text
Consent check
Profile data
Interview guide by theme
Document request checklist
Audio consent toggle
Closing notes
QA flag
```

---

## 9. Caregiver Tool UI

Tool:

```text
SQ-CG + SSI-CG-v1.0
```

Rules:

- Administer separately from the OOP participant.
- OOP participant must not be present.
- Caregiver must not be present during OOP interview.

Structured part topics:

```text
Relationship to OOP
Frequency of digital help
Types of digital tasks assisted
Weekly time burden
Observed frustration / anxiety
Reduced outings due to digital barriers
Missed services / benefits
Effect on relationship
```

---

## 10. FGD UI

Workflow:

```text
Create FGD code
Select city and group type
Add participant codes
Verify individual consent for each participant
Run phase-wise FGD guide
Capture activity output
Record group findings
Complete moderator note
Submit for QA
```

Rules:

- Use participant codes only during FGD recording.
- Audio is allowed only if all participants consent.
- Do not attribute sensitive statements to named individuals.

---

## 11. Field Observation / Shadowing UI

Tool:

```text
FO-OOP-v1.0
```

Workflow:

```text
Separate shadowing consent
Real destination and real purpose confirmation
Pre-observation checklist
Route and mobility observation
Digital interaction log
Staff interaction log
Accessibility heuristic
Session close
```

Ethics warning shown before start:

```text
Do not intervene, suggest, correct, or guide unless the participant asks to stop or faces physical danger.
```

---

## 12. Doctrinal Template UI

Used by PI/RA.

Fields:

```text
Document type
Jurisdiction
Citation / source
Summary
Digital exclusion relevance
Elder-rights relevance
Smart-city governance relevance
Accessibility obligation
Research objective mapping
Synthesis memo
```

---

## 13. Multilingual Support

Required language adaptation:

| City | Language support |
|---|---|
| New Delhi | Hindi, English, Urdu optional. |
| Srinagar | Kashmiri, Urdu, Hindi, English. |
| Aizawl | Mizo, English, Hindi optional. |
| Chennai | Tamil, English. |
| Gandhinagar | Gujarati, Hindi, English. |
| Indore | Hindi, English. |

Android resource folders:

```text
res/values/strings.xml
res/values-hi/strings.xml
res/values-ur/strings.xml
res/values-ta/strings.xml
res/values-gu/strings.xml
res/values-miz/strings.xml
res/values-ks/strings.xml
```

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

## 14. Accessibility Features

- Large print mode.
- Read-aloud mode.
- Consent audio playback.
- Visual response cards.
- High contrast screen mode.
- Thumb impression capture.
- Proxy consent flag.
- Pause, skip, and withdraw buttons.
- No tiny checkboxes for OOP screens.
- All sensitive questions must allow `S - skipped` with reason.

---

## 15. QA Queue UI

Filters:

```text
City
Field Investigator
Tool Code
Consent Missing
Break Missing
Same-day SQ + SSI Violation
Audio Pending Deletion
Transcript Pending Review
Distress Flag
Low Data Reliability
```

Actions:

```text
Approve
Return for clarification
Escalate to PI
Exclude from analysis
Mark withdrawal processed
```

---

## 16. Frontend Acceptance Checklist

- [ ] Consent cannot be bypassed.
- [ ] Written, oral, institutional NOC, and expert consent workflows exist.
- [ ] OOP questionnaire uses large-font read-aloud design.
- [ ] Sections A-C are fully city-adapted.
- [ ] Sections D-H are supported.
- [ ] Mandatory 25-minute break alert works.
- [ ] Fatigue and distress can be flagged anytime.
- [ ] Audio recording is optional and consent-linked.
- [ ] SQ-OOP and SSI-OOP cannot occur in the same session.
- [ ] Caregiver workflow is separated from OOP workflow.
- [ ] FGD supports individual consent tracking.
- [ ] Observation module shows non-intervention warning.
- [ ] QA queue exists.
- [ ] Multilingual strings are separated by city language.
