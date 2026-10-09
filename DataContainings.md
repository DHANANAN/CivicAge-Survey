# DataContainings.md

## CivicAge Survey (CAS) Data Containings, Instrument Crosswalk, Stakeholder Matrix, City Deployment, Pitfalls, and Safeguards

**Project:** Smart Cities, Silent Citizens - ICSSR Major Research Project 2025-26  
**Purpose:** Provide a crosswalk of all instruments, annexures, stakeholders, city deployment requirements, risks, and ethical safeguards for app integration.

---

## 1. Instrument Universe

The CAS app must represent:

```text
17 primary instruments
4 consent forms
4 QA tools
2 annexures
```

Every tool must have:

```text
Tool code
Instrument title
Stakeholder category
Consent requirement
City applicability
Session duration
Administration mode
Storage path
QA requirement
```

---

## 2. Consent Forms Crosswalk

| Code | Form | Stakeholder | App module |
|---|---|---|---|
| `CONSENT-OOP-W` | Written informed consent - oldest old persons | OOP | Signature / left thumb impression workflow. |
| `CONSENT-OOP-O` | Oral consent script - low literacy / cognitive impairment | OOP | Read-aloud script, verbal assent recording, proxy flag. |
| `CONSENT-INST-NOC` | Institutional NOC - old age home / community organisation | Institutions | NOC capture, signatory, anonymisation permission. |
| `CONSENT-KI` | Expert / key informant consent | Officials, experts, professionals | Audio consent and naming permission toggle. |

---

## 3. Primary Instruments Crosswalk

| Tool code | Instrument | Type | Stakeholder | CAS module |
|---|---|---|---|---|
| `SQ-OOP-v1.0` | Structured Questionnaire - Oldest Old Persons | Quantitative | OOP | OOP questionnaire Sections A-H. |
| `SSI-OOP-v1.0` | Semi-Structured Interview - Oldest Old Persons | Qualitative | OOP | 5-theme interview guide. |
| `SSI-MoHUA/SPV-v1.0` | Expert Interview - MoHUA officials and SPV officers | Qualitative | Government | Expert interview module. |
| `SSI-UP-v1.0` | Expert Interview - Urban planners and interface designers | Qualitative | Planners/designers | Design and UX interview module. |
| `SSI-ICCC-v1.0` | Interview - ICCC operators and technology vendors | Qualitative | Technology/service providers | ICCC data and operations module. |
| `FI-MCO-v1.0` | Focused Interview - Municipal corporation ward-level officials | Qualitative | Government/frontline | Short municipal interview module. |
| `SSI-OAH-Admin-v1.0` | Interview - Old age home administrators | Qual + document | OAH administrators | Institution profile and interview module. |
| `SSI-DLT-v1.0` | Interview - Digital literacy programme trainers | Qual + data | Trainers/coordinators | Programme data and interview module. |
| `SSI-NGO-v1.0` | Interview - NGOs and civil society organisations | Qualitative | NGOs | NGO evidence and advocacy module. |
| `SEI-Geri-v1.0` | Expert Interview - Geriatricians and mental health professionals | Expert qualitative | Geriatricians / mental health | Clinical expert module. |
| `EI-Legal-Ac-v1.0` | Expert Interview - Constitutional law and elder rights scholars | Expert qualitative | Legal scholars | Legal expert module. |
| `SQ-CG + SSI-CG-v1.0` | Questionnaire and Interview - Primary caregivers and family members | Mixed | Caregivers | Caregiver structured + interview module. |
| `FO-OOP-v1.0` | Field Observation and Shadowing Protocol | Observational | OOP / SCM environments | Observation and accessibility log. |
| `FGD-OOP/CG-v1.0` | Focus Group Discussion Guide - OOP and caregivers | Qualitative group | OOP + caregivers | FGD group session module. |
| `DOCTRINAL-v1.0` | Legal and Policy Document Analysis Template + Synthesis Memo | Doctrinal | PI/RA | Doctrinal review module. |
| `QA-v1.0` | Data Management and Quality Assurance Checklists | Administrative | All | QA queue and checklist module. |
| `ANNEXURES` | Instrument-objective crosswalk and city deployment matrix | Administrative | Project team | Planning and reporting module. |

---

## 4. SQ-OOP Internal Data Containing

| Section | Data captured | Notes |
|---|---|---|
| A | Socio-demographic profile | Age, gender, language, education, living arrangement, income source, residence, health, impairment, caregiving. |
| B | Digital device ownership and exposure | Phone access, smartphone/basic phone, who operates device, computer access, digital confidence, internet access. |
| C | Awareness and usage of Smart City Mission services | City-specific service awareness, attempts, independent success, assisted use, abandonment, middleman payment, missed services. |
| D | Digital Anxiety Scale | 10-item 5-point Likert scale with low/moderate/high band. |
| E1 | Digital Dependency Frequency Index | Frequency of help, who helps, missed medical/government benefits, increased dependence. |
| E2 | Dignity Perception Scale | 6-item 5-point Likert scale; reverse scoring during analysis. |
| F | Social isolation and intergenerational impact | Public outings, loneliness, technology gap with family, shared community difficulty. |
| G | Digital literacy programme and welfare scheme awareness | Training exposure, post-training ability, awareness of senior schemes, online welfare access. |
| H | FI observation log | Reading ability, visual/motor difficulty, comprehension, privacy, distress, completion status, data reliability. |

---

## 5. Stakeholder Matrix

| Stakeholder | Reason for inclusion | Main instrument(s) | Consent |
|---|---|---|---|
| Oldest Old Persons, 75+ | Primary affected group; direct experience of digital-by-default governance. | `SQ-OOP`, `SSI-OOP`, `FO-OOP`, `FGD-OOP/CG` | OOP written/oral consent. |
| Primary caregivers / family | Observe dependency, support burden, intergenerational effects. | `SQ-CG + SSI-CG`, FGD | Caregiver consent. |
| Old age home administrators | Institutional care setting and resident digital access. | `SSI-OAH-Admin` | Expert consent + institutional NOC. |
| NGOs / civil society | Case evidence, advocacy, field support for elderly. | `SSI-NGO` | Expert/key informant consent. |
| MoHUA officials | National Smart Cities Mission design and policy oversight. | `SSI-MoHUA/SPV` | Expert/key informant consent. |
| Smart City SPV officers | City-level implementation and project governance. | `SSI-MoHUA/SPV` | Expert/key informant consent. |
| Urban planners / interface designers | Design assumptions, accessibility, universal design. | `SSI-UP` | Expert/key informant consent. |
| ICCC operators / tech vendors | Digital service backend, data, failure pathways, offline backup. | `SSI-ICCC` | Expert/key informant consent. |
| Municipal ward officials | Last-mile delivery and offline alternatives. | `FI-MCO` | Expert/key informant consent. |
| Digital literacy trainers | Training design, usability outcomes, gaps. | `SSI-DLT` | Expert/key informant consent. |
| Geriatricians / mental health professionals | Clinical impact of digital anxiety and cognitive load. | `SEI-Geri` | Expert/key informant consent. |
| Constitutional / elder rights legal scholars | Legal framework, Article 21/41 analysis, reform proposals. | `EI-Legal-Ac` | Expert/key informant consent. |
| PI / Research Associate | Doctrinal analysis, QA, synthesis. | `DOCTRINAL`, `QA` | Internal project authorisation. |

---

## 6. City-wise Deployment Matrix

| City | Core OOP tools | Expert / institutional tools | City-specific frontend configuration |
|---|---|---|---|
| New Delhi | SQ-OOP, SSI-OOP, FO-OOP, FGD | MoHUA/SPV, planners, MCO, OAH, NGO, legal experts, geriatricians | Hindi/English/Urdu; Delhi Metro app, Delhi One app, EDMC e-services, Mohalla Clinic app, smart parking. |
| Srinagar | SQ-OOP, SSI-OOP, FO-OOP, FGD | SPV, MCO, ICCC, OAH, NGO | Kashmiri/Urdu/Hindi/English; smart bus shelters, Wi-Fi hotspots, ICCC grievance portal, smart waste. Include internet disruption probe. |
| Aizawl | SQ-OOP, SSI-OOP, FO-OOP, FGD | SPV, MCO, ICCC, NGO, DLT | Mizo/English/Hindi; solid waste tracking, sewerage monitoring, ICCC civic services. Include church/YMA/community support probe. |
| Chennai | SQ-OOP, SSI-OOP, FO-OOP, FGD | SPV, planners, OAH, geriatricians, DLT, NGO | Tamil/English; smart road services, e-bus tracking, digital health centres, smart parks. Include SHG/community support probe. |
| Gandhinagar | SQ-OOP, SSI-OOP, FO-OOP, FGD | SPV, planners, ICCC, DLT, MCO | Gujarati/Hindi/English; iNagar app, GIFT City infrastructure, smart health kiosks. |
| Indore | SQ-OOP, SSI-OOP, FO-OOP, FGD | SPV, MCO, ICCC, NGO, DLT | Hindi/English; 311 Smart City app, BRTS digital booking, ICCC grievance system, digital waste compliance. |

---

## 7. Instrument-to-Objective Crosswalk Template

Use this table inside the app’s admin/planning module.

| Research objective | Tool(s) | Data type | Expected output |
|---|---|---|---|
| Measure digital access barriers among the oldest old | SQ-OOP Sections B-C, D, E | Quantitative | Scores, frequencies, city comparisons. |
| Understand lived experience and dignity impact | SSI-OOP Themes T1-T5 | Qualitative | Thematic narratives and anonymised quotes. |
| Assess dependency and caregiver burden | SQ-CG + SSI-CG, SQ-OOP Section E | Mixed | Dependency pathways and family support patterns. |
| Assess institutional support | SSI-OAH-Admin, SSI-DLT, SSI-NGO | Qualitative + administrative data | Institutional readiness and training gaps. |
| Assess design and governance accountability | SSI-MoHUA/SPV, SSI-UP, SSI-ICCC, FI-MCO | Expert qualitative | Policy/design gap findings. |
| Observe real-world usability | FO-OOP | Observational | Route, kiosk, staff support, offline alternative evidence. |
| Develop legal and policy recommendations | EI-Legal-Ac, DOCTRINAL | Doctrinal/legal | Constitutional and statutory reform basis. |
| Validate fieldwork quality | QA-v1.0 | Administrative | Protocol compliance and data reliability. |

---

## 8. Pitfall Mitigation Strategies

| Pitfall | Risk | App mitigation |
|---|---|---|
| Consent treated as formality | Ethical invalidity | Consent gate, read-aloud mode, consent lock, audit log. |
| OOP participant fatigues | Harm and low-quality data | 25-minute alert, fatigue flag, skippable sensitive questions, end-session option. |
| Family/caregiver influences OOP answers | Biased data | Privacy checklist; caregiver tool administered separately. |
| SQ and SSI conducted together | Protocol violation | Hard block and 2-day gap logic. |
| Audio recorded without consent | Ethical breach | Audio button disabled unless consent exists. |
| Audio retained unnecessarily | Privacy risk | Transcribe -> verify -> delete pipeline. |
| Institutional identity disclosed | Reputational risk | Anonymise by default; naming permission toggle. |
| City services not adapted | Poor validity | City-specific service card required before Section C. |
| Skipped responses confused with missing data | Analysis error | `S` code with mandatory reason. |
| Translation inconsistency | Measurement error | Language version metadata and pre-test checklist. |
| Low internet access during fieldwork | Data loss | Offline drafts and queued sync. |
| Observation becomes staged | Invalid data | Real destination confirmation and non-intervention warning. |
| FGD confidentiality weakened | Group harm | Individual consent, no names in notes, group-level reporting. |
| Expert interview becomes PR response | Thin data | Evidence-based probes and document request checklist. |
| Small subgroup reported | Re-identification | Minimum cell size and aggregation rule. |

---

## 9. Ethical Safeguards

### 9.1 Voluntary participation

The app must display and record:

```text
Participation is voluntary.
Participant may skip any question.
Participant may stop at any time.
Withdrawal has no negative consequence.
```

### 9.2 Withdrawal rights

Every participant record must allow:

```text
Withdraw participant
Mark data excluded
Delete audio/transcript if required
Notify PI/admin
Audit action
```

### 9.3 Minimal risk protocol

Sensitive domains:

```text
Health
Income
Dependency
Family support
Distress
Loneliness
Digital embarrassment
Missed entitlements
Middleman payments
```

Safeguards:

```text
Skip option
Pause option
End session option
Referral/follow-up flag
PI escalation for distress
```

### 9.4 Privacy and confidentiality

```text
No names in research export.
No raw audio in final dataset.
No exact address in instrument data.
Institution anonymised unless permission exists.
Expert identity anonymised unless permission exists.
FGD data reported as group findings.
```

### 9.5 No harm in institutions

For old age home residents:

```text
Participation or withdrawal must not affect care.
Institutional NOC does not replace individual consent.
Institution staff should not be present during OOP sessions.
No photographs/video on premises unless separately permitted.
```

---

## 10. Data Containing Fields by Module

### 10.1 Universal fields

```json
{
  "projectId": "SC-SC-ICSSR-2025-26",
  "cityCode": "ND",
  "toolCode": "SQ-OOP-v1.0",
  "participantCode": "CAS-ND-OOP-2026-0001",
  "fieldInvestigatorCode": "FI-ND-001",
  "language": "Hindi",
  "startedAt": 1730000000000,
  "endedAt": 1730000000000,
  "status": "completed",
  "qaStatus": "pending"
}
```

### 10.2 Consent fields

```text
consentType
consentMode
consentGiven
audioRecordingConsent
geoTagConsent
shadowingConsent
proxyConsentRequired
withdrawalExplained
skipRightExplained
signatureOrThumbPath
oralAssentAudioPath
locked
```

### 10.3 Session welfare fields

```text
breakAlertShown
breakAccepted
fatigueObserved
distressObserved
terminatedEarly
terminationReason
referralNeeded
piEscalationRequired
```

### 10.4 QA fields

```text
reviewerCode
reviewDate
checklistType
decision
remarks
protocolViolationFlag
dataReliability
exportReady
```

---

## 11. Annexures in App

### Annexure A1 - Instrument-to-objective crosswalk

App purpose:

```text
Admin planning screen showing which tool supports which research objective and whether sufficient data has been collected.
```

### Annexure A2 - City-wise deployment matrix

App purpose:

```text
City dashboard showing completed, pending, and QA-blocked tools across New Delhi, Srinagar, Aizawl, Chennai, Gandhinagar, and Indore.
```

Suggested dashboard fields:

```text
City
Tool code
Target count
Completed count
Pending QA
Approved count
Rejected/excluded count
Notes
```

---

## 12. Deployment Readiness Checklist

- [ ] All 17 primary tools are present in the app.
- [ ] All 4 consent forms are present.
- [ ] QA tools are mapped to the QA queue.
- [ ] Annexure A1 and A2 dashboards are represented.
- [ ] Stakeholder-specific consent logic works.
- [ ] City-specific service cards are configured.
- [ ] Six-city languages are configured.
- [ ] Sensitive questions allow skip.
- [ ] Audio deletion pipeline exists.
- [ ] Institutional anonymisation is default.
- [ ] FGD group-level confidentiality is enforced.
- [ ] Observation protocol includes non-intervention warning.
- [ ] Export blocks withdrawn or QA-incomplete records.
