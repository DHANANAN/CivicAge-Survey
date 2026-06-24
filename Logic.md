# Logic.md

## CivicAge Survey (CAS) Fieldwork Logic and Session Rules

**Project:** Smart Cities, Silent Citizens - ICSSR Major Research Project 2025-26  
**Purpose:** Define the application logic that controls consent, session sequencing, skip logic, fatigue safeguards, audio recording, timestamps, interviews, FGDs, observation protocols, doctrinal templates, and QA.

---

## 1. Core Logic Principles

1. **Consent gates every instrument.** No data collection tool opens without the appropriate consent record.
2. **SQ and SSI are separate.** The structured questionnaire for oldest old persons and the semi-structured interview must never be administered in the same session.
3. **Two-day gap required.** `SSI-OOP-v1.0` must be scheduled at least two calendar days after `SQ-OOP-v1.0`.
4. **Participant welfare overrides data completion.** Fatigue, confusion, distress, or refusal must pause or stop the session.
5. **Skip is valid data.** Any skipped answer must be recorded as `S` with a reason.
6. **Audio is optional.** Audio recording is never mandatory and must be linked to express consent.
7. **Every action is timestamped.** Start, pause, break, resume, skip, save, submit, audio upload, transcription, deletion, and QA review must be timestamped.
8. **Group and institutional data are anonymised by default.** Naming permission must be explicit.

---

## 2. Global Session State Machine

```text
NOT_STARTED
  -> CONSENT_REQUIRED
  -> CONSENT_IN_PROGRESS
  -> CONSENT_LOCKED
  -> SESSION_READY
  -> SESSION_IN_PROGRESS
  -> PAUSED
  -> COMPLETED_PENDING_FI_LOG
  -> SUBMITTED_PENDING_SYNC
  -> SYNCED_PENDING_QA
  -> QA_APPROVED
  -> EXPORT_READY
```

Exception states:

```text
WITHDRAWN
TERMINATED_EARLY
DO_NOT_PROCEED
QA_EXCLUDED
AUDIO_PENDING_DELETION
```

---

## 3. Consent Logic

### 3.1 Consent-to-tool mapping

| Tool group | Required consent |
|---|---|
| SQ-OOP-v1.0 | `CONSENT-OOP-W` or `CONSENT-OOP-O` |
| SSI-OOP-v1.0 | `CONSENT-OOP-W` or `CONSENT-OOP-O` plus audio consent if recording |
| FO-OOP-v1.0 | Separate observation/shadowing consent |
| FGD-OOP/CG-v1.0 | Individual consent for every participant |
| SQ-CG + SSI-CG-v1.0 | `CONSENT-KI` or caregiver-specific consent screen |
| Expert interviews | `CONSENT-KI` |
| Institutional OAH data | `CONSENT-INST-NOC` plus individual consent for residents |
| Doctrinal template | No human-subject consent; source citation required |
| QA tools | Internal project authorization |

### 3.2 Consent blocking logic

```kotlin
if (!consent.exists || !consent.locked || consent.consentGiven != true) {
    blockInstrumentStart("Consent is incomplete or not locked.")
}
```

### 3.3 Oral consent rule

```text
If verbal assent is NO, unclear, confused, or reluctant -> set session state DO_NOT_PROCEED.
```

### 3.4 Proxy consent rule

```text
Proxy consent may be recorded only when required by protocol. It must never override the participant's clear refusal.
```

---

## 4. OOP Session Separation Rules

### 4.1 SQ-OOP and SSI-OOP separation

```kotlin
val sqDate = getCompletedInstrumentDate(participantCode, "SQ-OOP-v1.0")
val today = currentDate()

if (toolCode == "SSI-OOP-v1.0") {
    require(sqDate != null) { "SQ-OOP must be completed before SSI-OOP." }
    require(daysBetween(sqDate, today) >= 2) { "Minimum 2-day gap from SQ-OOP is required." }
}
```

### 4.2 Same-day block

```text
If SQ-OOP completed today, SSI-OOP button must show: "Available after two days".
```

### 4.3 Session duration caps

| Tool | Preferred duration | Mandatory alert |
|---|---:|---:|
| SQ-OOP-v1.0 | 45-55 minutes | Break at 25 minutes; hard warning before 60 minutes |
| SSI-OOP-v1.0 | 60-75 minutes | Alert at 60 minutes |
| Expert interviews | 45-90 minutes depending tool | Alert at configured limit |
| Caregiver questionnaire | 15-20 minutes | Alert at 20 minutes |
| Caregiver interview | 30-40 minutes | Alert at 40 minutes |
| Observation | 60-120 minutes | Safety check every 30 minutes |
| FGD | As per field protocol | Moderator-controlled break |

---

## 5. Skip Logic

### 5.1 Skip value

All skipped items must be stored as:

```json
{
  "questionId": "A7",
  "value": "S",
  "skipReason": "fatigue_sensitive_income_question",
  "skippedAt": 1730000000000,
  "skippedBy": "FI-ND-001"
}
```

### 5.2 Standard skip reasons

```text
participant_refused
participant_fatigued
question_not_applicable
participant_did_not_know
participant_preferred_not_to_say
sensitive_question
language_comprehension_issue
hearing_or_visual_difficulty
time_constraint
fi_protocol_decision
```

### 5.3 Required vs skippable distinction

- Consent fields are not skippable.
- Participant code, city, tool code, FI code, date, start time, and end time are not skippable.
- Sensitive fields such as income, health, disability indicators, distress, and household income must allow skip.
- The app must distinguish `S - skipped` from `N/A - not applicable` and `DK - do not know`.

---

## 6. Timestamping Rules

Each session must store:

```json
{
  "createdAt": 1730000000000,
  "startedAt": 1730000000000,
  "breakPromptedAt": 1730001500000,
  "breakStartedAt": 1730001520000,
  "breakEndedAt": 1730001820000,
  "pausedAt": [],
  "resumedAt": [],
  "endedAt": 1730003600000,
  "submittedAt": 1730003700000,
  "syncedAt": null,
  "qaReviewedAt": null
}
```

Timestamp rules:

1. Device timestamp must be stored for offline use.
2. Server timestamp must be added at sync.
3. Any mismatch greater than a configured threshold should be flagged for QA.
4. Audio upload, transcript generation, audio deletion, export, and withdrawal must have separate timestamps.

---

## 7. Break and Fatigue Logic

### 7.1 Mandatory break at 25 minutes

```kotlin
if (toolCode == "SQ-OOP-v1.0" && elapsedMinutes >= 25 && !breakPromptShown) {
    showBreakPrompt()
    logEvent("break_prompt_shown")
}
```

Break outcomes:

```text
break_accepted
participant_continued
session_paused
session_ended
```

### 7.2 Fatigue detection

The FI may flag fatigue at any point.

Fatigue indicators:

```text
confusion
visible tiredness
repeated re-explanation
long silence
emotional distress
hearing difficulty
visual strain
participant asks to pause
participant asks to stop
```

Logic:

```text
Mild fatigue -> offer break.
Moderate fatigue -> pause or skip non-essential items.
Significant distress -> end sensitive section or terminate session.
Continued distress -> stop session and inform supervisor/PI.
```

### 7.3 Distress escalation

```json
{
  "distressObserved": true,
  "distressLevel": "moderate",
  "actionTaken": "session_paused",
  "referralNeeded": false,
  "piInformed": true
}
```

---

## 8. Audio Recording Logic

### 8.1 Consent-gated audio

```kotlin
if (audioToggle == true && consent.audioRecordingConsent != true) {
    blockAudioRecording("Audio recording consent has not been given.")
}
```

### 8.2 Audio allowed tools

| Tool | Audio default |
|---|---|
| SQ-OOP-v1.0 | No audio, except oral consent assent if needed |
| SSI-OOP-v1.0 | Optional, consent required |
| Expert interviews | Optional, consent required |
| Caregiver interview | Optional, consent required |
| FGD | Optional only if all participants consent |
| Observation | Ambient only if separately consented; no photography without separate written consent |
| Doctrinal template | No audio |

### 8.3 Audio pipeline state

```text
not_started -> recording_local -> uploaded_temp -> transcribing -> transcribed -> transcript_reviewed -> audio_deleted
```

If participant withdraws before transcript finalisation:

```text
delete audio -> delete draft transcript -> mark withdrawn -> audit log
```

---

## 9. Structured Questionnaire Flow

```text
Consent locked
  -> Participant metadata
  -> Section A
  -> Section B
  -> Section C
  -> Break alert at 25 minutes
  -> Section D
  -> Section E
  -> Section F
  -> Section G
  -> FI completes Section H privately
  -> Review skipped items
  -> Submit / Save draft
```

Validation before submit:

```text
Consent exists
Participant ID exists
City exists
FI code exists
Start and end time exist
Break prompt logged
Skipped items have reasons
FI observation log completed
```

---

## 10. Semi-Structured Interview Flow

```text
Verify SQ completed and 2-day gap
  -> Verify consent and audio choice
  -> Pre-interview checklist
  -> Start interview
  -> Theme screens T1-T5
  -> 60-minute alert
  -> Closing protocol
  -> Post-interview reflexivity log within 30 minutes
  -> Transcript workflow if audio exists
  -> Submit for QA
```

Pre-interview checklist:

```text
[ ] Audio recorder tested, if recording
[ ] Consent confirmed
[ ] Private venue
[ ] SQ responses reviewed to avoid repetition
[ ] City-specific service card available
[ ] Participant comfortable
[ ] Break/stop rights reminded
[ ] Start time noted
[ ] Reflexivity log ready
```

---

## 11. FGD Logic

```text
Create FGD session
  -> Add participant codes
  -> Verify individual consent for each participant
  -> Confirm venue safety and privacy
  -> Confirm audio only if all consent
  -> Run phase guide
  -> Capture group activity output
  -> Record dominant findings
  -> Moderator and note-taker log
  -> Submit for QA
```

Rules:

1. Do not use participant names in FGD notes.
2. Audio is blocked if even one participant refuses.
3. Sensitive statements must be marked as group-level, not attributable.
4. Any distress or conflict must be logged.

---

## 12. Field Observation Logic

```text
Separate observation consent
  -> Confirm real destination and real purpose
  -> Pre-observation checklist
  -> Start timer
  -> Route and mobility log
  -> Digital interaction log
  -> Staff interaction log
  -> Accessibility heuristic
  -> End observation
  -> FI reflection
```

Non-intervention rule:

```text
FI does not intervene, correct, suggest, or guide unless participant asks to stop or faces physical danger.
```

If intervention occurs:

```json
{
  "interventionMade": true,
  "reason": "physical_safety",
  "time": 1730000000000,
  "effectOnData": "observation_partially_interrupted"
}
```

---

## 13. Doctrinal Review Logic

```text
Create document review
  -> Select source type
  -> Add citation
  -> Extract relevant legal/policy content
  -> Map to project objective
  -> Add digital exclusion analysis
  -> Add elder-rights analysis
  -> Add smart-city governance analysis
  -> Write synthesis memo
  -> Submit for PI review
```

Mandatory fields:

```text
Document title
Source/citation
Jurisdiction
Date/version
Relevance to OOP digital exclusion
Research objective mapping
Reviewer code
```

---

## 14. QA Logic

QA triggers:

```text
missing consent
same-day SQ and SSI attempt
2-day gap violation
break not recorded
session duration too long
high skipped-item count
distress flag
audio recorded without consent
audio not deleted after transcription
missing FI observation log
missing reflexivity log
institution named without permission
```

QA decisions:

```text
approve
return_for_clarification
escalate_to_pi
exclude_from_analysis
withdrawal_processed
```

---

## 15. Export Readiness Logic

A record is export-ready only when:

```text
Consent locked
Instrument completed
Required FI logs completed
Audio deleted, if audio was used
Transcript reviewed, if interview was recorded
QA approved
Participant not withdrawn
Anonymisation complete
```

Export block message:

```text
This record cannot be exported because required ethics / QA checks are incomplete.
```

---

## 16. Logic Acceptance Checklist

- [ ] Consent gates all instruments.
- [ ] Oral refusal blocks data collection.
- [ ] SQ-OOP and SSI-OOP are separated.
- [ ] Two-day gap rule is enforced.
- [ ] Break alert triggers at 25 minutes.
- [ ] Fatigue and distress can pause/end sessions.
- [ ] Skip value `S` always includes reason.
- [ ] Audio recording is consent-linked.
- [ ] FGD blocks audio unless all consent.
- [ ] Observation includes non-intervention rules.
- [ ] Doctrinal review requires citation/source.
- [ ] QA blocks export until approved.
