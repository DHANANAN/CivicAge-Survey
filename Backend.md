# Backend.md

## CivicAge Survey (CAS) Backend Architecture

**Project:** Smart Cities, Silent Citizens - ICSSR Major Research Project 2025-26  
**App:** CivicAge Survey (CAS)  
**Target group:** Oldest Old Population (75+ years) in New Delhi, Srinagar, Aizawl, Chennai, Gandhinagar, and Indore  
**Backend stack target:** Firebase Authentication + Cloud Firestore + Cloud Storage + Cloud Functions + optional on-device Room cache

This document defines the backend schema required to integrate all consent forms, structured questionnaires, semi-structured interviews, FGDs, observation protocols, QA checklists, and doctrinal templates into the CivicAge Survey app workflow.

---

## 1. Backend Design Principles

1. **Participant-code first:** No analytical table should use participant name as a primary identifier. All instruments must be linked through a participant code.
2. **Consent-led data capture:** No instrument can begin unless the correct consent object is completed and locked.
3. **Instrument separation:** Questionnaire, interview, FGD, observation, caregiver, expert, and doctrinal tools must remain separate records even when linked to the same participant or institution.
4. **Offline-first fieldwork:** Field investigators must be able to collect data offline and sync later.
5. **Minimum identifiable data:** Personally identifying details must be stored only in a restricted linkage collection.
6. **Audio minimisation:** Audio is uploaded only for transcription, then deleted after transcript verification and QA approval.
7. **Auditability:** Every submission, update, export, and deletion must produce an audit log.
8. **ICSSR compliance:** Voluntary participation, withdrawal rights, anonymisation, secure storage, and minimal risk must be enforceable in the backend.

---

## 2. Firebase Authentication and UID Schema

### 2.1 User roles

```json
{
  "roles": {
    "admin": "PI / Co-PI / authorised project administrator",
    "field_supervisor": "City supervisor or QA lead",
    "field_investigator": "Enumerator / interviewer",
    "transcriber": "Transcript-only access, no identifiers",
    "analyst": "Anonymised data access only",
    "viewer": "Read-only dashboard access"
  }
}
```

### 2.2 Firebase user document

Path:

```text
/users/{firebaseUid}
```

Schema:

```json
{
  "firebaseUid": "auth uid",
  "surveyorCode": "FI-ND-001",
  "fullName": "Field Investigator Name",
  "email": "user@example.org",
  "phone": "optional",
  "role": "field_investigator",
  "cityAssigned": "NEW_DELHI",
  "active": true,
  "trainingCompleted": true,
  "ethicsPledgeSigned": true,
  "createdAt": 1730000000000,
  "lastLoginAt": 1730000000000,
  "deviceIds": ["android-device-id-hash"],
  "permissions": {
    "canCollectOOP": true,
    "canCollectCaregiver": true,
    "canCollectExpert": false,
    "canConductFGD": false,
    "canExport": false,
    "canViewIdentifiers": false,
    "canApproveDeletion": false
  }
}
```

---

## 3. Participant, Institution, and Instrument UID Schema

### 3.1 City codes

```text
NEW_DELHI   = ND
SRINAGAR    = SG
AIZAWL      = AZ
CHENNAI     = CH
GANDHINAGAR = GN
INDORE      = IN
```

### 3.2 Stakeholder codes

```text
OOP   = Oldest Old Person
CG    = Caregiver / family member
MOHUA = MoHUA or Smart City SPV official
UP    = Urban planner / interface designer
ICCC  = ICCC operator / technology vendor
MCO   = Municipal ward-level official
OAH   = Old age home / care institution administrator
DLT   = Digital literacy trainer / coordinator
NGO   = NGO / civil society organisation
GERI  = Geriatrician / mental health professional
LEGAL = Legal scholar / elder rights expert
FGD   = Focus group discussion
FO    = Field observation / shadowing
DOC   = Doctrinal document review
QA    = Quality assurance tool
```

### 3.3 Participant code format

```text
CAS-{CITY}-{STAKEHOLDER}-{YYYY}-{SEQUENCE}
```

Examples:

```text
CAS-ND-OOP-2026-0001
CAS-SG-CG-2026-0018
CAS-CH-GERI-2026-0003
```

### 3.4 Instrument instance UID format

```text
{participantOrInstitutionCode}-{TOOL_CODE}-{SESSION_SEQUENCE}
```

Examples:

```text
CAS-ND-OOP-2026-0001-SQ-OOP-v1.0-S01
CAS-ND-OOP-2026-0001-SSI-OOP-v1.0-S02
CAS-IN-OAH-2026-0004-SSI-OAH-Admin-v1.0-S01
CAS-AZ-FGD-2026-0002-FGD-OOP-CG-v1.0-S01
```

### 3.5 Linkage UID

Personally identifying linkage must be stored separately:

```text
/linkageFiles/{participantCode}
```

Only PI/admin users should access this path.

---

## 4. Firestore Collection Layout

```text
/projects/{projectId}
  /cities/{cityCode}
  /users/{firebaseUid}
  /participants/{participantCode}
  /institutions/{institutionCode}
  /consents/{consentUid}
  /instruments/{instrumentUid}
  /sessions/{sessionUid}
  /audioJobs/{audioJobUid}
  /transcripts/{transcriptUid}
  /qaReviews/{qaReviewUid}
  /exports/{exportUid}
  /auditLogs/{auditLogUid}
  /withdrawalRequests/{requestUid}
```

Recommended project ID:

```text
/projects/SC-SC-ICSSR-2025-26
```

---

## 5. Consent Storage Paths

### 5.1 Consent collection

```text
/projects/SC-SC-ICSSR-2025-26/consents/{consentUid}
```

### 5.2 Consent object schema

```json
{
  "consentUid": "CAS-ND-OOP-2026-0001-CONSENT-OOP-W",
  "participantCode": "CAS-ND-OOP-2026-0001",
  "consentType": "CONSENT-OOP-W",
  "consentMode": "written_thumbprint",
  "stakeholderType": "OOP",
  "cityCode": "ND",
  "language": "Hindi",
  "fieldInvestigatorUid": "firebaseUid",
  "fieldInvestigatorCode": "FI-ND-001",
  "consentGiven": true,
  "audioRecordingConsent": false,
  "geoTagConsent": true,
  "shadowingConsent": false,
  "photoVideoConsent": false,
  "institutionNamingPermission": false,
  "expertNamingPermission": false,
  "proxyConsentRequired": false,
  "proxyNameEncrypted": null,
  "proxyRelation": null,
  "withdrawalExplained": true,
  "skipRightExplained": true,
  "minimalRiskExplained": true,
  "dataUseExplained": true,
  "signedAt": 1730000000000,
  "deviceTimestamp": 1730000000000,
  "serverTimestamp": "serverTimestamp()",
  "signatureStoragePath": "projects/SC-SC-ICSSR-2025-26/consentSignatures/CAS-ND-OOP-2026-0001.png",
  "oralAssentAudioPath": null,
  "locked": true,
  "qaStatus": "pending_review"
}
```

### 5.3 Consent forms to be implemented

| Consent code | Consent form | App workflow |
|---|---|---|
| `CONSENT-OOP-W` | Written informed consent for oldest old persons | Signature or left thumb impression; used before SQ-OOP, SSI-OOP, and optional observation. |
| `CONSENT-OOP-O` | Oral consent for low literacy / cognitive impairment situations | Read-aloud script; verbal assent audio ID; proxy marker where required. |
| `CONSENT-INST-NOC` | Institutional NOC for old age homes / community organisations | Institution-level permission; does not replace individual consent. |
| `CONSENT-KI` | Expert / key informant consent | Used for MoHUA/SPV, planners, ICCC, municipal officials, OAH admins, DLT, NGO, geriatricians, and legal scholars. |

---

## 6. Participant and Institution Records

### 6.1 Participant profile

Path:

```text
/projects/SC-SC-ICSSR-2025-26/participants/{participantCode}
```

Schema:

```json
{
  "participantCode": "CAS-ND-OOP-2026-0001",
  "stakeholderType": "OOP",
  "cityCode": "ND",
  "ageBand": "75-79",
  "gender": "Female",
  "locationType": "community_dwelling",
  "wardOrNeighbourhood": "manual locality only",
  "livingArrangement": "with_adult_children",
  "createdBy": "FI-ND-001",
  "createdAt": 1730000000000,
  "consentStatus": "valid",
  "active": true,
  "withdrawn": false,
  "withdrawnAt": null,
  "instrumentsCompleted": {
    "SQ-OOP-v1.0": false,
    "SSI-OOP-v1.0": false,
    "FO-OOP-v1.0": false
  },
  "riskFlag": "none",
  "qaFlag": "pending"
}
```

### 6.2 Restricted linkage file

Path:

```text
/projects/SC-SC-ICSSR-2025-26/linkageFiles/{participantCode}
```

Schema:

```json
{
  "participantCode": "CAS-ND-OOP-2026-0001",
  "nameEncrypted": "encrypted-string",
  "phoneEncrypted": "encrypted-string",
  "addressEncrypted": "encrypted-string",
  "proxyNameEncrypted": "encrypted-string-or-null",
  "institutionNameEncrypted": "encrypted-string-or-null",
  "emergencyContactEncrypted": "encrypted-string-or-null",
  "createdAt": 1730000000000,
  "deleteAfter": "study_end_plus_required_retention_period",
  "accessRole": "admin_only"
}
```

### 6.3 Institution profile

Path:

```text
/projects/SC-SC-ICSSR-2025-26/institutions/{institutionCode}
```

Schema:

```json
{
  "institutionCode": "CAS-ND-OAH-2026-0001",
  "institutionType": "old_age_home_private",
  "cityCode": "ND",
  "publicDisplayName": "Institution ND-01",
  "actualNameEncrypted": "encrypted-string",
  "nocConsentUid": "CAS-ND-OAH-2026-0001-CONSENT-INST-NOC",
  "namingPermission": false,
  "residentProfileAvailable": true,
  "digitalLiteracyProgrammeAvailable": false,
  "institutionalDataShared": true,
  "createdAt": 1730000000000
}
```

---

## 7. Instrument Storage Paths

All instruments should be stored in a common collection with tool-specific payloads.

```text
/projects/SC-SC-ICSSR-2025-26/instruments/{instrumentUid}
```

Base schema:

```json
{
  "instrumentUid": "CAS-ND-OOP-2026-0001-SQ-OOP-v1.0-S01",
  "toolCode": "SQ-OOP-v1.0",
  "toolType": "structured_questionnaire",
  "participantCode": "CAS-ND-OOP-2026-0001",
  "institutionCode": null,
  "cityCode": "ND",
  "language": "Hindi",
  "sessionUid": "CAS-ND-OOP-2026-0001-S01",
  "createdBy": "FI-ND-001",
  "createdAt": 1730000000000,
  "startedAt": 1730000000000,
  "endedAt": 1730003600000,
  "status": "completed",
  "consentUid": "CAS-ND-OOP-2026-0001-CONSENT-OOP-W",
  "audioConsent": false,
  "geoTagConsent": true,
  "geo": {
    "lat": 28.6139,
    "lng": 77.2090,
    "accuracyM": 25,
    "capturedAt": 1730000000000,
    "manualLocality": "Ward / neighbourhood text"
  },
  "fatigue": {
    "breakAlertShownAtMinute": 25,
    "breakAccepted": true,
    "fatigueObserved": false,
    "distressObserved": false,
    "terminatedEarly": false,
    "terminationReason": null
  },
  "answers": {},
  "skipLog": [],
  "qa": {
    "requiresReview": true,
    "reviewStatus": "pending",
    "dataReliability": "not_reviewed"
  },
  "sync": {
    "localRecordId": "uuid",
    "isSynced": false,
    "firebaseDocumentPath": "",
    "jsonStoragePath": ""
  }
}
```

---

## 8. JSON Storage Paths by Instrument Type

### 8.1 Questionnaires

```text
Firestore:
/projects/{projectId}/instruments/{instrumentUid}

Cloud Storage JSON backup:
/projects/{projectId}/json/questionnaires/{cityCode}/{toolCode}/{instrumentUid}.json
```

Questionnaire payload:

```json
{
  "toolCode": "SQ-OOP-v1.0",
  "sections": {
    "A_socio_demographic": {},
    "B_device_ownership": {},
    "C_smart_city_services": {},
    "D_digital_anxiety_scale": {},
    "E_dependency_dignity": {},
    "F_social_isolation": {},
    "G_digital_literacy_welfare": {},
    "H_fi_observation_log": {}
  },
  "computedScores": {
    "digitalAnxietyTotal": null,
    "digitalAnxietyBand": null,
    "digitalDependencyIndex": null,
    "dignityRawScore": null,
    "dignityReversedScore": null
  }
}
```

### 8.2 Semi-structured interviews

```text
Firestore:
/projects/{projectId}/instruments/{instrumentUid}

Cloud Storage transcript:
/projects/{projectId}/transcripts/{cityCode}/{toolCode}/{instrumentUid}.txt

Cloud Storage JSON:
/projects/{projectId}/json/interviews/{cityCode}/{toolCode}/{instrumentUid}.json
```

Interview payload:

```json
{
  "toolCode": "SSI-OOP-v1.0",
  "themes": {
    "T1_life_before_after_digital_transformation": [],
    "T2_smart_city_service_incidents": [],
    "T3_emotional_digital_anxiety_wellbeing": [],
    "T4_social_connections_family_dynamics": [],
    "T5_rights_dignity_policy": []
  },
  "audio": {
    "recordingConsent": true,
    "audioJobUid": "audio-job-id",
    "originalAudioStoragePath": "temporary/audio/path.m4a",
    "transcriptionStatus": "completed",
    "audioDeletedAfterTranscription": true
  },
  "postInterviewLog": {
    "dominantThemes": [],
    "notableQuotes": [],
    "digitalBarriersRaisedSpontaneously": null,
    "fiReflexivityNote": "",
    "dataQualityAssessment": "moderate",
    "followUpOrReferralNeeded": false
  }
}
```

### 8.3 Focus group discussions

```text
Firestore:
/projects/{projectId}/instruments/{fgdUid}

Cloud Storage JSON:
/projects/{projectId}/json/fgds/{cityCode}/{fgdUid}.json

Cloud Storage transcript:
/projects/{projectId}/transcripts/fgds/{cityCode}/{fgdUid}.txt
```

FGD payload:

```json
{
  "toolCode": "FGD-OOP-CG-v1.0",
  "fgdCode": "CAS-ND-FGD-2026-0001",
  "participantCodes": ["CAS-ND-OOP-2026-0001", "CAS-ND-CG-2026-0011"],
  "groupType": "OOP_and_Caregiver",
  "moderatorCode": "FI-ND-002",
  "noteTakerCode": "FI-ND-003",
  "phaseNotes": {
    "phase1_warmup": "",
    "phase2_service_mapping": "",
    "phase3_problem_ranking": "",
    "phase4_recommendations": ""
  },
  "activityOutputs": [],
  "audioConsentAllParticipants": false,
  "sensitiveIncidentFlag": false,
  "dominantGroupFindings": []
}
```

### 8.4 Field observation and shadowing

```text
/projects/{projectId}/json/observations/{cityCode}/{instrumentUid}.json
```

Observation payload:

```json
{
  "toolCode": "FO-OOP-v1.0",
  "participantCode": "CAS-ND-OOP-2026-0001",
  "scmFacilityName": "city-specific facility",
  "participantPurpose": "real destination / real purpose",
  "routeObservation": [],
  "digitalInteractionLog": [],
  "staffInteractionLog": [],
  "accessibilityHeuristic": {
    "physicalAccess": null,
    "digitalAccess": null,
    "offlineAlternativeAvailable": null,
    "staffSupportAvailable": null
  },
  "fiInterventionMade": false,
  "interventionReason": null
}
```

### 8.5 Doctrinal and policy template

```text
/projects/{projectId}/json/doctrinal/{docReviewUid}.json
```

Doctrinal payload:

```json
{
  "toolCode": "DOCTRINAL-v1.0",
  "documentType": "statute_policy_case_guideline_report",
  "jurisdiction": "India / State / City / International",
  "citation": "full legal citation or source reference",
  "summary": "",
  "relevanceToDigitalExclusion": "",
  "elderRightsDimension": "",
  "smartCityGovernanceDimension": "",
  "accessibilityObligation": "",
  "researchObjectiveMapping": [],
  "synthesisMemo": ""
}
```

### 8.6 QA records

```text
/projects/{projectId}/qaReviews/{qaReviewUid}
/projects/{projectId}/json/qa/{cityCode}/{qaReviewUid}.json
```

QA payload:

```json
{
  "qaReviewUid": "QA-ND-2026-0001",
  "reviewedInstrumentUid": "CAS-ND-OOP-2026-0001-SQ-OOP-v1.0-S01",
  "reviewerUid": "firebaseUid",
  "reviewerRole": "field_supervisor",
  "reviewDate": 1730000000000,
  "checklistType": "consent_completeness / data_quality / transcript_quality / fieldwork_protocol",
  "items": [],
  "decision": "approve / revise / exclude / escalate",
  "remarks": "",
  "piEscalationRequired": false
}
```

---

## 9. Audio Transcription Pipeline

### 9.1 Pipeline overview

```text
Record locally -> Encrypt locally -> Upload temporary audio -> Create audio job -> Transcribe -> Save transcript -> QA review -> Delete audio -> Mark deletion proof
```

### 9.2 Cloud Storage temporary audio path

```text
/projects/{projectId}/audio-temp/{cityCode}/{toolCode}/{instrumentUid}/{audioFileName}.m4a
```

### 9.3 Audio job document

```text
/projects/{projectId}/audioJobs/{audioJobUid}
```

```json
{
  "audioJobUid": "AUDIO-CAS-ND-OOP-2026-0001-SSI-OOP-v1.0-S02",
  "instrumentUid": "CAS-ND-OOP-2026-0001-SSI-OOP-v1.0-S02",
  "participantCode": "CAS-ND-OOP-2026-0001",
  "toolCode": "SSI-OOP-v1.0",
  "audioConsent": true,
  "uploadedBy": "FI-ND-001",
  "audioStoragePath": "projects/.../audio-temp/...m4a",
  "language": "Hindi",
  "durationSeconds": 4200,
  "status": "uploaded / transcribing / transcribed / reviewed / audio_deleted / failed",
  "transcriptPath": "projects/.../transcripts/...txt",
  "createdAt": 1730000000000,
  "transcribedAt": null,
  "reviewedAt": null,
  "audioDeletedAt": null,
  "deleteProof": {
    "deletedByFunction": true,
    "deletedBy": "cloudFunctionName",
    "deletionLogUid": "AUDIT-..."
  }
}
```

### 9.4 Deletion rules

1. Audio is never retained as research data after transcription and transcript QA.
2. If transcription fails, the field supervisor may approve one reprocess attempt.
3. Audio must be deleted after transcript verification.
4. Audio deletion must be logged in `auditLogs`.
5. If the participant withdraws before transcript finalisation, audio and draft transcript must be deleted and marked as withdrawn.

---

## 10. Geo-tagging Rules

### 10.1 Allowed geo data

Use geotagging only when consent is explicitly captured.

```json
{
  "geoTagConsent": true,
  "latitude": 28.6139,
  "longitude": 77.2090,
  "accuracyM": 25,
  "capturedAt": 1730000000000,
  "manualLocality": "Ward / neighbourhood",
  "locationType": "community_dwelling / OAH / public_facility / ward_office / ICCC / other"
}
```

### 10.2 Privacy rules

- Do not store exact home addresses in instrument data.
- Use manual locality or ward for analysis.
- Exact geolocation should be visible only to admin and field supervisor.
- Public reports must use aggregated city / ward / facility category, not individual coordinates.

---

## 11. Admin Dashboard Backend Requirements

### 11.1 Dashboard collections

```text
/projects/{projectId}/dashboardStats/{cityCode}
/projects/{projectId}/dashboardStats/allCities
```

### 11.2 Required indicators

```json
{
  "cityCode": "ND",
  "totalParticipants": 0,
  "oopParticipants": 0,
  "caregiverParticipants": 0,
  "expertParticipants": 0,
  "completedSQ": 0,
  "completedSSI": 0,
  "completedFGD": 0,
  "completedObservation": 0,
  "pendingSync": 0,
  "pendingQA": 0,
  "withdrawnParticipants": 0,
  "audioPendingDeletion": 0,
  "breakComplianceRate": 0,
  "consentCompletenessRate": 0,
  "lastUpdatedAt": 1730000000000
}
```

### 11.3 Dashboard role access

| Dashboard module | Admin | Field supervisor | Field investigator | Analyst | Transcriber |
|---|---:|---:|---:|---:|---:|
| All-city progress | Yes | Read | No | Read anonymised | No |
| City progress | Yes | Own city | Own assignments only | Read anonymised | No |
| Raw identifiers | Yes | No, unless authorised | No | No | No |
| Audio jobs | Yes | Own city metadata | Own uploads only | No | Assigned job only |
| Transcript text | Yes | Own city | Own records | Anonymised only | Assigned job |
| QA decisions | Yes | Own city | Own records status | Read aggregate | No |
| Export | Yes | City export if authorised | No | Anonymised export only | No |

---

## 12. Firestore Security Rules - Policy Skeleton

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    function signedIn() {
      return request.auth != null;
    }

    function userDoc() {
      return get(/databases/$(database)/documents/projects/SC-SC-ICSSR-2025-26/users/$(request.auth.uid));
    }

    function role() {
      return userDoc().data.role;
    }

    function isAdmin() {
      return signedIn() && role() in ['admin'];
    }

    function isSupervisor() {
      return signedIn() && role() in ['field_supervisor'];
    }

    function isFieldInvestigator() {
      return signedIn() && role() in ['field_investigator'];
    }

    match /projects/{projectId}/linkageFiles/{participantCode} {
      allow read, write: if isAdmin();
    }

    match /projects/{projectId}/participants/{participantCode} {
      allow create: if isFieldInvestigator() || isSupervisor() || isAdmin();
      allow read: if isAdmin() || isSupervisor() || resource.data.createdBy == userDoc().data.surveyorCode;
      allow update: if isAdmin() || isSupervisor() || resource.data.createdBy == userDoc().data.surveyorCode;
    }

    match /projects/{projectId}/instruments/{instrumentUid} {
      allow create: if isFieldInvestigator() || isSupervisor() || isAdmin();
      allow read: if isAdmin() || isSupervisor() || resource.data.createdBy == userDoc().data.surveyorCode;
      allow update: if isAdmin() || isSupervisor() || resource.data.createdBy == userDoc().data.surveyorCode;
    }

    match /projects/{projectId}/auditLogs/{auditLogUid} {
      allow create: if signedIn();
      allow read: if isAdmin();
      allow update, delete: if false;
    }
  }
}
```

---

## 13. Audit Log Schema

Path:

```text
/projects/SC-SC-ICSSR-2025-26/auditLogs/{auditLogUid}
```

```json
{
  "auditLogUid": "AUDIT-2026-000001",
  "actorUid": "firebaseUid",
  "actorCode": "FI-ND-001",
  "actorRole": "field_investigator",
  "action": "create_instrument / update_instrument / consent_lock / audio_upload / transcript_create / audio_delete / export / withdrawal",
  "targetPath": "/projects/.../instruments/...",
  "participantCode": "CAS-ND-OOP-2026-0001",
  "cityCode": "ND",
  "timestamp": 1730000000000,
  "deviceIdHash": "hash",
  "ipHash": "hash-if-available",
  "details": {}
}
```

---

## 14. Compliance With ICSSR Ethical Protocols

### 14.1 Required backend controls

| Ethical requirement | Backend control |
|---|---|
| Voluntary participation | Consent object must include `consentGiven`, `withdrawalExplained`, and `skipRightExplained`. |
| Right to withdraw | `withdrawalRequests` collection and `withdrawn` flag on participant profile. |
| Skip any question | `skipLog` array with question ID and reason. |
| Minimal risk | `distressObserved`, `terminatedEarly`, and `referralNeeded` flags. |
| Audio optional | Instrument cannot require audio if `audioRecordingConsent` is false. |
| Privacy | Participant code used in instruments; identifiers moved to restricted linkage collection. |
| Group reporting only | Export functions must remove names, phone numbers, exact addresses, and raw coordinates. |
| Institutional anonymisation | Institution profile has `namingPermission`; default false. |
| Audio deletion | Audio job cannot close until `audioDeletedAt` is set. |
| QA | `qaReviews` required before final export. |

### 14.2 Withdrawal request schema

```json
{
  "requestUid": "WITHDRAW-CAS-ND-OOP-2026-0001",
  "participantCode": "CAS-ND-OOP-2026-0001",
  "requestedAt": 1730000000000,
  "requestedBy": "participant / proxy / PI",
  "receivedBy": "FI-ND-001",
  "status": "pending / approved / completed",
  "deleteIdentifiableData": true,
  "excludeResearchData": true,
  "audioDeleted": true,
  "transcriptDeleted": true,
  "completedAt": null,
  "adminApprovalUid": null
}
```

---

## 15. Cloud Functions Required

| Function | Trigger | Purpose |
|---|---|---|
| `onInstrumentCreate` | Firestore create | Validate consent, session rules, and required metadata. |
| `onConsentLock` | Firestore update | Prevent later modification of consent except by admin correction log. |
| `onAudioUploaded` | Storage finalize | Create transcription job if consent exists. |
| `runTranscription` | Queue / callable | Transcribe audio into text. |
| `deleteAudioAfterTranscriptQA` | QA approval | Delete temporary audio and write audit log. |
| `updateDashboardStats` | Firestore write | Refresh city and all-city indicators. |
| `generateAnonymisedExport` | Callable admin/analyst | Produce analysis dataset without identifiers. |
| `processWithdrawal` | Callable admin | Apply withdrawal decision and deletion rules. |
| `flagProtocolViolation` | Firestore write | Flag same-day SQ + SSI, missing break, missing consent, or excessive duration. |

---

## 16. Recommended Indexes

```text
instruments: cityCode ASC, toolCode ASC, status ASC
instruments: participantCode ASC, toolCode ASC, startedAt DESC
instruments: createdBy ASC, status ASC, updatedAt DESC
consents: participantCode ASC, consentType ASC
qaReviews: reviewedInstrumentUid ASC, decision ASC
sessions: participantCode ASC, startedAt ASC
audioJobs: status ASC, cityCode ASC, createdAt ASC
withdrawalRequests: status ASC, requestedAt ASC
```

---

## 17. Migration Notes for Current App

1. Rename user-facing labels from `CivilAge Survey` to `CivicAge Survey` where required by the project title.
2. Preserve current offline Room cache but expand entity fields to support all instrument types.
3. Continue using local draft save and sync status, but sync into instrument-specific Firebase paths.
4. Replace one generic survey form with modular tool launcher: Consent -> Instrument Category -> Tool -> Session -> QA.
5. Add admin-only linkage file handling.
6. Add audio job deletion proof before export.
7. Add city-language metadata at every record level.

---

## 18. Minimum Backend Acceptance Checklist

- [ ] All 17 primary instruments can be represented as separate `toolCode` records.
- [ ] All 4 consent forms are implemented and linked to instruments.
- [ ] QA records exist and can block final export.
- [ ] Audio is optional and deletable after transcription.
- [ ] SQ-OOP and SSI-OOP cannot occur in the same session.
- [ ] A 2-day gap rule can be validated through session timestamps.
- [ ] Participant codes, not names, are used in analysis records.
- [ ] Institutional names are anonymised unless explicit permission exists.
- [ ] City-wise deployment can be tracked.
- [ ] Admin dashboard can show progress, protocol violations, QA status, audio deletion status, and export readiness.
