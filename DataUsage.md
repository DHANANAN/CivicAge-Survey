# DataUsage.md

## CivicAge Survey (CAS) Data Usage, Anonymisation, Storage, Sharing, and QA Policy

**Project:** Smart Cities, Silent Citizens - ICSSR Major Research Project 2025-26  
**Purpose:** Define how CAS collects, stores, anonymises, uses, quality-checks, exports, and shares data while protecting participants, institutions, and key informants.

---

## 1. Data Usage Principles

1. **Participant code only:** Research data must use participant or institution codes, not names.
2. **Group findings only:** Reports must present aggregated findings, not identifiable individual stories unless explicit permission exists and ethical review allows it.
3. **Locked linkage file:** Any name, phone number, precise address, proxy details, or institution identity must stay in a restricted linkage file.
4. **Audio minimisation:** Audio exists only for transcription and must be deleted after verified transcription.
5. **Institutional anonymisation by default:** Old age homes, NGOs, city offices, and experts are anonymised unless separate permission is given.
6. **Withdrawal protected:** If a participant withdraws, data must be excluded according to consent and withdrawal protocol.
7. **QA before export:** No record moves into analysis export unless QA status is approved.
8. **Need-to-know access:** Field investigators, transcribers, analysts, supervisors, and admins receive only the data needed for their role.

---

## 2. Data Categories

| Category | Examples | Identifiability | Default handling |
|---|---|---|---|
| Consent data | Signature, thumb impression, assent audio ID, consent choices | Identifiable/sensitive | Restricted access; not in analysis export. |
| Linkage data | Name, phone, address, proxy details | Directly identifiable | Admin-only encrypted linkage file. |
| Questionnaire data | SQ-OOP answers, caregiver questionnaire | Mostly coded responses | Participant code only. |
| Interview data | Transcripts, quotes, field notes | Potentially identifiable | De-identify before analysis. |
| FGD data | Group notes, activity outputs | Group-level but sensitive | No names; group code only. |
| Observation data | Route, service interaction, staff behaviour | Contextually identifiable | Remove exact personal identifiers. |
| Institutional data | OAH profile, programme data, NOC | Institution-identifiable | Anonymise unless permission given. |
| Doctrinal data | Legal/policy documents, synthesis memo | Public-source | Cite source; no participant identifiers. |
| QA data | Review notes, protocol flags | Internal project data | Admin/supervisor access. |
| Audit logs | User actions, timestamps, deletion logs | Internal metadata | Admin-only review. |

---

## 3. Anonymisation Pipeline

```text
Raw field data
  -> Participant code assigned
  -> Direct identifiers separated
  -> Linkage file encrypted and locked
  -> Instrument data cleaned
  -> Transcript de-identified
  -> Institutional names masked
  -> QA review
  -> Anonymised dataset generated
  -> Group-level analysis export
```

### 3.1 Participant code rule

All instruments must use:

```text
CAS-{CITY}-{STAKEHOLDER}-{YYYY}-{SEQUENCE}
```

Examples:

```text
CAS-ND-OOP-2026-0001
CAS-SG-CG-2026-0008
CAS-CH-GERI-2026-0002
```

### 3.2 Direct identifiers to remove from analysis exports

```text
Participant name
Phone number
Full address
Exact house location
Signature / thumb impression
Proxy name
Emergency contact
Aadhaar or other identity number
Raw audio file
Photo/video file
Institution actual name unless permission exists
Named official/expert unless permission exists
```

### 3.3 Transcript de-identification

Before qualitative analysis, replace:

```text
Names -> [PERSON]
Specific address -> [LOCALITY]
Phone number -> [PHONE]
Institution name -> [INSTITUTION]
Official name -> [OFFICIAL]
Exact service counter/staff name -> [STAFF]
Family member names -> [FAMILY_MEMBER]
```

Example:

```text
Raw: My son Rajesh took me to the Indore 311 office near X Colony.
Clean: My [FAMILY_MEMBER] took me to the Indore 311 office near [LOCALITY].
```

---

## 4. Aggregation Rules

### 4.1 Reporting level

Final outputs must normally report at:

```text
All-city aggregate
City-level aggregate
Stakeholder-group aggregate
Age-band aggregate
Gender aggregate where non-identifying
Location-type aggregate: community-dwelling / old age home
```

### 4.2 Minimum cell size

To avoid re-identification:

```text
Do not publish a subgroup cell if n < 5.
```

If `n < 5`, merge categories or suppress the cell.

### 4.3 Quote usage

Quotes may be used only when:

```text
The transcript is de-identified.
The quote does not reveal the participant, institution, family, locality, or unique event.
The quote is necessary to explain a theme.
The participant has not withdrawn.
```

Quote label format:

```text
OOP participant, New Delhi, community-dwelling, age band 75-79.
```

Do not use:

```text
Full name, exact age, exact ward, institution name, phone, address, or unique identifying incident.
```

---

## 5. Secure Storage Structure

### 5.1 Restricted linkage storage

```text
/projects/{projectId}/linkageFiles/{participantCode}
```

Access:

```text
Admin only
```

### 5.2 Research instrument storage

```text
/projects/{projectId}/instruments/{instrumentUid}
/projects/{projectId}/json/{instrumentType}/{cityCode}/{instrumentUid}.json
```

Access:

```text
Field investigator: own records
Supervisor: assigned city
Analyst: anonymised data only
Admin: full project access
```

### 5.3 Transcript storage

```text
/projects/{projectId}/transcripts/{cityCode}/{toolCode}/{instrumentUid}.txt
```

Transcript states:

```text
raw_transcript
reviewed_transcript
anonymised_transcript
analysis_ready
```

### 5.4 Consent signatures and assent audio

```text
/projects/{projectId}/consentSignatures/{participantCode}.png
/projects/{projectId}/oralAssent/{participantCode}.m4a
```

These must not appear in analysis exports.

---

## 6. Audio Handling and Deletion

### 6.1 Audio lifecycle

```text
Local encrypted audio
  -> Temporary upload
  -> Transcription
  -> Transcript review
  -> Audio deletion
  -> Deletion audit log
```

### 6.2 Required audio deletion proof

```json
{
  "audioJobUid": "AUDIO-CAS-ND-OOP-2026-0001",
  "instrumentUid": "CAS-ND-OOP-2026-0001-SSI-OOP-v1.0-S02",
  "audioDeleted": true,
  "audioDeletedAt": 1730000000000,
  "deletedBy": "cloud_function",
  "deletionAuditLogUid": "AUDIT-2026-000001"
}
```

### 6.3 Export rule

```text
If audioDeleted != true, record cannot be exported.
```

---

## 7. Auto-delete After Study

### 7.1 Retention states

```text
active_collection
analysis_phase
report_writing_phase
study_completed
identifier_deletion_due
identifier_deleted
archived_anonymised_dataset
```

### 7.2 Deletion targets after study completion

Delete or destroy:

```text
Linkage files
Consent signature images, where retention is no longer required
Oral assent audio
Raw interview audio
Temporary transcription audio
Exact geolocation coordinates
Device identifiers
Any accidental direct identifiers in notes
```

Retain only if permitted:

```text
Anonymised questionnaire dataset
Anonymised transcript excerpts
Aggregated city-level tables
Doctrinal research notes
QA summary without identifiers
```

---

## 8. Institutional Anonymisation

### 8.1 Default rule

```text
Institution names are anonymised unless explicit written permission for naming is given.
```

### 8.2 Institution label format

```text
OAH-ND-01
OAH-CH-02
NGO-AZ-01
DLT-IN-01
```

### 8.3 Reports may say

```text
A private old age home in Chennai...
A government old age home in New Delhi...
A city-level NGO in Aizawl...
```

Reports must not say:

```text
The exact institution name, unless naming permission exists.
```

---

## 9. QA Checklist Integration

### 9.1 QA checkpoints

| Stage | QA check |
|---|---|
| Consent | Consent type, language, signature/thumb/assent, audio choice, withdrawal explanation. |
| Session | Correct tool, correct participant, timestamps, break rule, no prohibited same-day SQ+SSI. |
| Data quality | Missing fields, excessive skips, inconsistent responses, FI observation log. |
| Interview | Audio consent, transcript review, reflexivity log, distress flag. |
| Observation | Separate consent, non-intervention log, real destination confirmation. |
| FGD | Individual consent for all participants, group-level anonymisation, moderator note. |
| Institutional | NOC, naming permission, anonymised institutional data. |
| Export | De-identification, audio deletion, withdrawal status, QA approval. |

### 9.2 QA decision schema

```json
{
  "qaReviewUid": "QA-ND-2026-0001",
  "reviewedInstrumentUid": "instrument-id",
  "reviewerCode": "SUP-ND-001",
  "checklistType": "data_quality",
  "decision": "approve",
  "remarks": "Consent and break compliance verified.",
  "piEscalationRequired": false,
  "reviewedAt": 1730000000000
}
```

### 9.3 QA blocks

The app must block export when:

```text
Consent incomplete
Audio pending deletion
Transcript not reviewed
QA not approved
Withdrawal pending
Institution named without permission
Break rule missing for SQ-OOP
Same-day SQ/SSI violation
```

---

## 10. Sharing Anonymised Summaries With Institutions

Institutions may receive a brief summary only when:

```text
The institution requested it or the protocol permits it.
The summary is aggregated and anonymised.
No participant-level response is disclosed.
No resident quote is traceable to a person.
The summary does not compare named institutions in a harmful way.
```

Allowed summary content:

```text
Number of sessions conducted at institution category level
General themes on digital access barriers
Suggested accessibility improvements
Aggregated digital literacy needs
Non-identifying policy recommendations
```

Not allowed:

```text
Named resident responses
Caregiver conflict details
Individual distress notes
Exact quotes that identify a resident
Comparative ranking of institutions without explicit permission
```

---

## 11. Data Sharing With Research Team

| Role | Data access |
|---|---|
| Field investigator | Own assigned records, draft and sync status. |
| Field supervisor | City-level records and QA queue. |
| Transcriber | Assigned audio/transcript only; no linkage file. |
| Analyst | Anonymised questionnaire and transcript data only. |
| PI/Admin | Full access, including linkage file, withdrawal, and audit logs. |

---

## 12. Withdrawal Handling

When withdrawal is recorded:

```text
Mark participant as withdrawn
Stop pending sessions
Remove from export-ready dataset
Delete raw audio if any
Delete draft transcript if required
Mark transcript excluded if already anonymised but withdrawal requires exclusion
Update audit log
Notify PI/admin
```

Withdrawal object:

```json
{
  "participantCode": "CAS-ND-OOP-2026-0001",
  "withdrawn": true,
  "withdrawnAt": 1730000000000,
  "requestedBy": "participant",
  "dataUseAfterWithdrawal": "exclude_all",
  "processedBy": "adminUid",
  "processingStatus": "completed"
}
```

---

## 13. Export Dataset Structure

### 13.1 Quantitative export

```text
participantCode
cityCode
ageBand
gender
locationType
SQ section variables
computed scores
skip counts
fatigue flags
QA status
```

### 13.2 Qualitative export

```text
participantCode
cityCode
stakeholderType
toolCode
anonymisedTranscript
themeCodes
non-identifyingQuoteBank
qaStatus
```

### 13.3 Excluded from export

```text
Name
Phone
Address
Signature
Thumb impression
Raw audio
Consent audio
Exact GPS coordinates
Institution actual name without permission
Unreviewed transcript
Withdrawn records
```

---

## 14. Data Usage Acceptance Checklist

- [ ] Analysis data uses participant code only.
- [ ] Linkage file is stored separately and locked.
- [ ] Audio deletion is enforced before export.
- [ ] Transcripts are de-identified.
- [ ] Institutions are anonymised by default.
- [ ] Cell suppression applies for very small groups.
- [ ] Withdrawal requests block export.
- [ ] QA checklist approval is mandatory.
- [ ] Anonymised summaries can be shared with institutions safely.
- [ ] Auto-delete schedule exists after study completion.
