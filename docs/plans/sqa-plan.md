# Software Quality Assurance Plan – SESCAM Radiodiagnostics

| | |
|---|---|
| **Group** | ISO II – 3A1.04 (team of 6) |
| **Owner** | George (quality and test lead), with the whole team |
| **Status** | Final v1.0 – approved by the team |
| **Date** | 13/11/2026 |
| **Structure** | IEEE 730 (Software Quality Assurance), product quality model ISO/IEC 25010 |
| **Related** | [`quality-testing-strategy.md`](../../quality-testing-strategy.md) (test strategy), [`project-plan.md`](project-plan.md), [`scm-plan.md`](scm-plan.md), [`CONTRIBUTING.md`](../../CONTRIBUTING.md), `DECISIONS.md` |

## History

| Version | Date | Author | Change |
|---|---|---|---|
| v0.1 | 06/11/2026 | D. Villanueva | First version. Builds on `quality-testing-strategy.md`: justifies the characteristics for the healthcare domain, turns them into measurable targets and adds the inspection, control and improvement procedures. |
| v0.2 | 10/11/2026 | D. Villanueva | Adds the impact of the quality characteristics on each component (§3.3) and on each user story (§3.4), quality gates that grow with each release (§5.2.1, D-17) and the automatic architecture checks (ArchUnit and enforcer, D-14). |
| v1.0 | 13/11/2026 | Whole team | Final version, approved by the team and submitted as Theoretical Assignment 1 (D-22). |

---

## 1. Purpose, scope and references

### 1.1 Purpose

This plan states **which product qualities matter most** for a radiodiagnostics management system, **why**, **how they are measured**, and the **procedures** the team follows to inspect, control and improve them during the whole lifecycle. It turns "quality" from an intention into checks that are run on every change.

### 1.2 Scope

All deliverables of the project: code of the 17 modules and `radio-app`, interfaces, tests, simulators, documents and the wiki. It applies to all members and to code produced with the AI agent.

### 1.3 References

ISO/IEC 25010 (product quality model) · IEEE 730 · Regulation (EU) 2016/679 (GDPR), art. 9 (health data) · Spanish Organic Law 3/2018 (LOPDGDD) · Law 41/2002 (patient autonomy and clinical documentation) · HL7 FHIR · DICOM · `quality-testing-strategy.md`.

## 2. Management

### 2.1 Organisation

| Role | Who | Responsibilities |
|---|---|---|
| **Quality lead** | George | Owns this plan and the metrics; chairs the quality review at the end of each iteration; proposes corrective actions. |
| **Reviewer** | Any member except the author | Inspects every PR with the checklist (§5.1.1). |
| **Iteration owner** | Assigned per iteration | Ensures the Definition of Done of the iteration is met. |
| **Configuration manager** | David | Runs the configuration audits at each release ([`scm-plan.md` §3.4](scm-plan.md)). |
| **Architect** | Alejandro | Leads the design and contract inspections (§5.1.2). |
| **Security reviewer** | Rotating, one per iteration | Runs the security review of the iteration (§5.1.4). |

Quality assurance is independent of the author: **nobody approves or merges their own work**.

### 2.2 Tasks per phase

| Phase | QA tasks |
|---|---|
| Inception | Agree this plan, the checklist and the Definition of Done; CI with test and coverage reports. |
| Elaboration | Design and contract inspections; security design review; coverage baseline and confirmation of thresholds. |
| Construction | PR inspections; quality gates; defect tracking; quality review per iteration. |
| Transition | Full regression; final quality audit against §4; lessons learned for the self-assessment. |

## 3. Critical quality characteristics for the healthcare domain

### 3.1 Why the domain changes the priorities

The system handles **health data** of patients (GDPR art. 9 special category), supports **clinical decisions** (prescriptions, priorities, signed reports) and depends on **hospital systems** that it does not control (SSO, HCE, RIS/PACS). A defect here is not only an inconvenience: it can expose a patient's data, delay an urgent test or make a report unreliable. This is what drives the selection below.

### 3.2 Selected characteristics

| Priority | ISO 25010 characteristic (sub-characteristics) | Justification in this domain | Stories most affected |
|---|---|---|---|
| **1 – Critical** | **Security** (confidentiality, integrity, accountability, authenticity) | Health data: unauthorised access is a legal breach (GDPR, LOPDGDD) and a breach of patient trust. Every access must be attributable to an identified user (accountability), and signed reports must not be altered (integrity, Law 41/2002). | HU-18 roles, HU-19 audit, HU-20 SSO, HU-09 sign reports, HU-03 history |
| **1 – Critical** | **Functional suitability** (correctness, completeness) | Wrong behaviour has clinical consequences: an urgent request queued as routine, two patients booked in the same slot, a report notified before it is signed. Correctness of priorities, bookings and report states is a patient-safety matter. | HU-06 prescribe, HU-13 book, HU-15 waiting list, HU-09 reports, HU-02 notifications |
| **2 – High** | **Reliability** (fault tolerance, recoverability, availability) | The system depends on external systems that can fail or be slow. If the PACS is down, the radiologist must still be able to write the report; if HCE does not answer, admissions must still work. Failures must be recorded and never block clinical work (D-08, CU-42). | HU-21, HU-24, HU-25, HU-02, HU-07 (timer) |
| **2 – High** | **Compatibility** (interoperability) | Healthcare systems interoperate through standards (FHIR/HL7 for patient data, DICOM for images). Our simulators must respect those contracts so that a real system can replace them without changes. | HU-21, HU-24, HU-25 |
| **2 – High** | **Maintainability** (modularity, testability, modifiability) | 17 modules by 6 people with code generated by an AI agent: without strict module boundaries and tests the system becomes impossible to change in the last weeks. | All |
| 3 – Medium | Usability (operability, user error protection) | Relevant for clinical staff, but the product is a prototype evaluated on its process; covered by basic error protection (confirmations, validation). | HU-01, HU-13 |
| 3 – Low | Performance efficiency | Simulated load of a prototype; no response-time requirement in the stories. | – |
| 3 – Low | Portability | One deployable on a known environment (Java 17, Spring Boot). | – |

Not prioritising usability, performance and portability is a **conscious decision**, recorded in `DECISIONS.md`, not an omission: with a fixed delivery date, effort goes to the characteristics whose failure would harm a patient or break the law.

### 3.3 Impact on each component

Each component (Maven module, D-13) is where its quality requirements are guaranteed: inside the
module and at its `api` boundary. Abbreviations: **FS** functional suitability · **SEC** security ·
**REL** reliability · **COM** compatibility · **MAI** maintainability · **USA** usability.
Two rules apply to **every** module and are not repeated in the table: it is used by other modules
only through its `api` package and has no cycles (MAI, checked by `ArchitectureTest`, D-14), and its
REST endpoints check permissions on the server (SEC, HU-18).

| Module (CFG where it appears) | Critical characteristics | Verifiable quality requirement | How it is verified |
|---|---|---|---|
| `radio-auth` (CFG-01) | SEC confidentiality, authenticity · FS correctness | Deny by default: an action not in the role matrix is rejected; the role comes from the corporate identity; the 4 roles of HU-18 and nothing else | Parameterised test of the role × action matrix; one forbidden-role test per endpoint; test with missing/expired session |
| `radio-sso-adapter` (CFG-01) | COM interoperability · REL fault tolerance | Same `ISsoGateway` for simulator and real adapter; SSO down ⇒ clear error, no partial session | Contract test of `ISsoGateway`; simulator failure test (timeout, invalid token) |
| `radio-audit` (CFG-01) | SEC accountability, integrity | Append-only: no update or delete operation exists; every record has user, date, action and resource | Test that the `api` has no update/delete; test that a record cannot be modified; query by user/resource/period |
| `radio-configuration` (CFG-01) | FS correctness · SEC accountability | A parameter change applies only to future appointments; every change is audited | Test with an existing appointment after a change; test that the change creates an audit record |
| `radio-catalogue` (CFG-01, ~06) | FS completeness · SEC integrity | Test types have all mandatory attributes; a type in use is deactivated, never deleted; changes audited (P-12) | Validation test per attribute; test "deactivate type in use"; audit test |
| `radio-app` (CFG-01 → 07) | REL availability · MAI modularity · USA error protection | Starts with each profile (simulators/real); no business logic; UI validates input and asks for confirmation on destructive actions | Smoke test of start-up in every configuration; ArchUnit (no domain code in the app); UI walkthrough per role (beta, final) |
| `radio-hce-adapter` (CFG-02) | COM interoperability (FHIR/HL7) · REL fault tolerance | Patient data mapped from the FHIR structure; HCE down ⇒ the failure is recorded and admissions continues manually (CU-42) | Round-trip mapping test; simulator failure test (timeout, error, unavailable) |
| `radio-patient` (CFG-02) | FS correctness · SEC confidentiality, accountability | No duplicate patients; deactivation is logical; every read of patient data is audited | Duplicate test; logical deactivation test; audit-on-read test |
| `radio-request` (CFG-02, ~05, ~06) | FS correctness · SEC integrity · MAI testability | **Only this module changes the request status** (D-07); illegal transitions rejected; priority kept; follow-up series generated once (idempotent) | State × action table test; test of each illegal transition; follow-up generation run twice with a fixed clock |
| `radio-scheduling` (CFG-03, ~05) | FS correctness · REL fault tolerance · USA error protection | **Never two appointments in the same slot**; reschedule follows the same rules; waiting list ordered by priority, deterministically | Concurrency test (two bookings, same slot ⇒ one wins); overlap/compatibility tests; waiting-list ordering table |
| `radio-notification` (CFG-03) | REL fault tolerance · SEC confidentiality | A failing channel never blocks the business action; notices contain no clinical data; no duplicate reminders | Channel-down test (action succeeds, failure recorded); content test without clinical data; duplicate-reminder test |
| `radio-jobs` (CFG-03, ~05) | REL recoverability · MAI testability | Running a job twice produces no duplicates; jobs use an adjustable clock | Job run twice with a fixed clock; test after an interrupted run |
| `radio-execution` (CFG-04) | FS completeness · SEC accountability | Execution data complete; marks the request as performed only through `IRequestService` | Validation test; test that the status changes through `radio-request` |
| `radio-report` (CFG-04, ~05) | SEC integrity (Law 41/2002), accountability · FS correctness | A **signed report is never overwritten**: a change creates a new version; every access to a report is audited | Test "update of a signed report ⇒ new version"; version history test; audit test |
| `radio-image` (CFG-04) | REL fault tolerance · SEC confidentiality | PACS down ⇒ clear message and the report can still be written (HU-25); image access policy applied | PACS-down test; forbidden-role test on images |
| `radio-pacs-adapter` (CFG-04) | COM interoperability (DICOM) · REL fault tolerance | Images are never copied (HU-24); same `IPacsGateway` for simulator and real adapter | Contract test; test that no image is stored; simulator failure test |
| `radio-feedback` (CFG-06) | FS correctness · SEC confidentiality | One survey per completed test; a patient sees only their own incidents | Second-survey test ⇒ rejected; cross-patient access test |
| `radio-metrics` (CFG-06) | FS correctness · SEC confidentiality | Figures match the seed data; read-only (D-06); no identifiable patient data in reports | Tests with a known dataset (oracle); test that it has no write repositories; content test without personal data |

### 3.4 Impact on each user story

The column *quality requirement* goes into the issue of the story as acceptance criteria. Every
story also inherits the two cross-cutting rules of §3.3 (permissions on the server and audit of
clinical data), which are not repeated.

| Story · iteration · module | Critical characteristics | Verifiable quality requirement | Verification |
|---|---|---|---|
| **HU-18** roles and permissions · It1 · auth | SEC confidentiality · FS correctness | Deny by default; role matrix as data, not scattered `if`s | Role × action parameterised test; forbidden-role test per endpoint |
| **HU-20** SSO login · It1 · auth + sso-adapter | SEC authenticity · REL fault tolerance | No access without a valid session; SSO down ⇒ clear error | Missing/expired session tests; SSO simulator failure |
| **HU-19** audit · It1 · audit | SEC accountability, integrity | Every clinical access recorded; records immutable; query only for administrators | Audit-on-access tests; no update/delete test; forbidden-role test on the query |
| **HU-17** system parameters · It1 · configuration | FS correctness · SEC accountability | Changes apply to future appointments only; audited | Test with an existing appointment; audit test |
| **HU-23** test catalogue · It1 · catalogue | FS completeness · SEC integrity | All attributes validated; a type in use is only deactivated | Validation tests; deactivate-in-use test |
| **HU-21** HCE / admission · It2 · hce-adapter + patient | COM interoperability · REL fault tolerance | FHIR mapping correct; HCE down never blocks admissions (CU-42) | Round-trip mapping; simulator timeout/error tests |
| **HU-12** patient data · It2 · patient | FS correctness · SEC confidentiality | No duplicates; logical deactivation; reads audited | Duplicate test; deactivation test; audit test |
| **HU-06** prescribe a test · It2 · request | FS correctness, completeness · SEC accountability | Mandatory fields and priority validated; requestable test types only; author recorded | Validation test per field; non-requestable type test; author test |
| **HU-13** assign appointments · It3 · scheduling | FS correctness · REL fault tolerance | Never two appointments in the same slot; only compatible slots | Concurrency test; compatibility test |
| **HU-01** manage my appointments · It3 · scheduling | SEC confidentiality · USA error protection | A patient acts only on their own appointments and in valid states; cancel asks for confirmation | Other patient's appointment ⇒ forbidden; state × action table |
| **HU-26** reschedule / cancel / change requests · It3 · scheduling | FS correctness · SEC accountability | Same rules as HU-13; cancellation frees the slot; every change attributed | Overlap test on reschedule; freed-slot test; audit test |
| **HU-02** notifications · It3 · notification + jobs | REL fault tolerance · SEC confidentiality | Channel failure never blocks the action; no clinical data in the notice; no duplicates | Channel-down test; content test; duplicate test |
| **HU-10** exam execution · It4 · execution | FS completeness · SEC accountability | Execution data complete; request marked as performed through `radio-request` | Validation test; status-change test |
| **HU-09** reports · It4 · report | SEC integrity, accountability · FS correctness | Signed report never overwritten; signing notifies the patient | New-version test; notification test |
| **HU-24** RIS/PACS · It4 · pacs-adapter | COM interoperability · REL fault tolerance | DICOM contract respected; images never copied; PACS down handled | Contract test; no-copy test; simulator failure |
| **HU-25** DICOM images · It4 · image | REL fault tolerance · SEC confidentiality | Report available with the PACS down; access policy applied; access audited | PACS-down test; forbidden-role test; audit test |
| **HU-07** follow-up tests · It5 · request + jobs | FS correctness · REL recoverability | Follow-up series generated on the right dates and only once | Fixed-clock test; run-twice test; suspended-series test |
| **HU-08** request status and agendas · It5 · request + scheduling | FS completeness · SEC confidentiality | Real lifecycle states; a physician sees only their own requests | Lifecycle test; cross-physician access test |
| **HU-14** daily worklists · It5 · scheduling | FS correctness · USA operability | Worklist matches the booked appointments of the day/room | Test with seed data |
| **HU-15** waiting lists and priorities · It5 · scheduling + request | FS correctness · SEC accountability | Deterministic order by priority and date; priority changes justified and audited | Ordering table test; justification + audit test |
| **HU-03** test history · It5 · report | FS completeness · SEC confidentiality | Complete history, read-only; only the patient (and authorised staff) sees it; access audited | Seed-data test; cross-patient test; audit test |
| **HU-04** incidents and surveys · It6 · feedback | FS correctness · SEC confidentiality | One survey per completed test; own incidents only | Second-survey test; cross-patient test |
| **HU-05** clinical guides and follow-ups · It6 · catalogue + request | FS correctness | The follow-up calendar matches the series of HU-07 | Shared fixed-clock tests with HU-07 |
| **HU-11** physician metrics · It6 · metrics | FS correctness · SEC confidentiality | Figures match the data; only the physician's own requests | Oracle dataset test; cross-physician test |
| **HU-16** operational reports · It6 · metrics | FS correctness · USA operability | Documented formulas (occupancy, waiting, cancellations); export | Oracle dataset test; export test |
| **HU-22** global statistics · It6 · metrics | FS correctness · SEC confidentiality | Totals match; no identifiable personal data | Oracle dataset test; content test |

## 4. Measurable quality requirements

Each characteristic is turned into metrics with a target. A metric without a target or without a way of measuring it is not used.

| Characteristic | Metric | Target | How it is measured | When |
|---|---|---|---|---|
| Functional suitability | Use case flows with an acceptance test (main, alternative, exception) | **100 %** of the flows of the iteration | Traceability: test names reference `CUxx`; checked in the PR | Every PR |
| Functional suitability | Open defects of severity *critical* or *major* at release | **0** | Issues with label `bug` + severity | Every release |
| Security | Endpoints with a server-side permission check and a test with an unauthorised role | **100 %** | Security tests per endpoint; review checklist | Every PR |
| Security | Accesses to clinical data without an audit record | **0** | Tests asserting that CU-38 is recorded for each clinical read/write | Every PR touching clinical data |
| Security | Secrets or real data in the repository | **0** | Review checklist + GitHub secret scanning | Every PR |
| Security | Signed reports that can be modified | **0** | Test: an update of a signed report is rejected and creates a new version | It 4 and regression |
| Reliability | External-system failure scenarios handled without blocking (timeout, error, unavailable) | **100 %** of the adapters | Simulator failure tests per adapter | Every PR on an adapter |
| Compatibility | Contract tests for each `api` and each adapter | **≥ 1 per public interface** | Contract tests in the consumer module | Every PR changing an `api` |
| Maintainability | Line / branch coverage on `domain` and `application` code | **Grows per release**: report-only in It 0 → line ≥ 70 % / branch ≥ 55 % at the beta → ≥ 75 % / 60 % at the final (§5.2.1) | JaCoCo `check` in CI with the thresholds of the parent POM (D-17) | Every build |
| Maintainability | Dependency cycles / imports of another module's internals | **0** | `ArchitectureTest` (ArchUnit) in `mvn -B verify` (D-05, D-14) | Every build |
| Maintainability | PR size | ≤ ~400 changed lines, one issue | Review | Every PR |
| Process | PRs merged with red CI or self-approved | **0** | Branch protection + Insights | Weekly |

## 5. Procedures

### 5.1 Inspect

#### 5.1.1 Pull request review (every change)

Every PR into `develop` is inspected by a member who is not the author, with this checklist (the same one in `.github/pull_request_template.md`):

- ☐ CI is green (`mvn -B verify`).
- ☐ The PR matches its issue (`Closes #…`) and only that issue.
- ☐ Every flow of every use case touched has an acceptance test.
- ☐ Permissions are checked on the server; there is a test with a forbidden role.
- ☐ Clinical data accesses are audited (CU-38).
- ☐ No interface changed without a decision in `DECISIONS.md`.
- ☐ No new dependency without a decision.
- ☐ No real data, secrets or credentials.
- ☐ The PR states whether an AI agent was used and for what.
- ☐ `CHANGELOG.md` / documentation updated if affected.

The reviewer either approves, or requests changes with a comment per finding. Findings are classified (§8).

#### 5.1.2 Design and contract inspection (start of each iteration)

Before implementation, the architect and the iteration owner inspect the analysis and design of the iteration's use cases: class and communication diagrams, `api` interfaces, DTOs and dependencies. Checks: the design covers every flow of the use case; dependencies respect D-05 (no cycles); each interface belongs to the module that provides it (D-03); external systems are reached only through adapters (D-08). Result recorded in the iteration page of the wiki.

#### 5.1.3 Requirements inspection (Inception and when a story changes)

Each user story is checked for: a clear actor and goal, verifiable acceptance criteria, mapping to use cases, and dependencies stated. A story that fails is corrected before it enters an iteration.

#### 5.1.4 Security review (each iteration that touches clinical data or roles)

A member other than the author reviews: role checks on every new endpoint, audit of clinical data, handling of identifiers in logs (no personal data in logs), and the signed-report integrity rule. Elaboration includes a specific **security design review** of the SSO simulation and the role model (HU-18, HU-20) before Iteration 2.

### 5.2 Control

#### 5.2.1 Quality gates

Gates on **every change**:

| Gate | Where | Rule |
|---|---|---|
| G1 – Build and tests | CI on every PR | `mvn -B verify` must pass (tests, enforcer, `ArchitectureTest`, JaCoCo `check`); a red build cannot be merged (required check) |
| G2 – Review | Branch protection | 1 approval by someone other than the author |
| G3 – Coverage | CI (`jacoco:check`) | Below the threshold of the current release gate (table below) the build fails |
| G4 – Definition of Done | End of iteration | All items of the DoD (`CONTRIBUTING.md` §4) met for every issue of the iteration |
| G5 – Release audit | Release PR | FCA/PCA of [`scm-plan.md` §3.4](scm-plan.md) passed and the release gate below met |

Gates **per release** (D-17). The thresholds grow with the product: early configurations protect the
architecture, later ones the completeness and the recoverability. The coverage values are the
properties `coverage.line.minimum` / `coverage.branch.minimum` of the parent POM, raised on each
`release/*` branch ([`scm-plan.md` §4.3](scm-plan.md)); internal configurations keep the value of the last release.

| Gate | CFG-00 `v0.1.0` | CFG-02 Alpha 1 | CFG-03 Alpha 2 | CFG-06 Beta | CFG-07 RC | CFG-07 Final |
|---|---|---|---|---|---|---|
| Line coverage, `domain` + `application` | report only | ≥ 50 % | ≥ 60 % | ≥ 70 % | ≥ 75 % | ≥ 75 % |
| Branch coverage, `domain` + `application` | report only | ≥ 40 % | ≥ 45 % | ≥ 55 % | ≥ 60 % | ≥ 60 % |
| Failing tests · ArchUnit violations · enforcer failures | 0 | 0 | 0 | 0 | 0 | 0 |
| Use-case flows of the delivered stories with an acceptance test | – | 100 % | 100 % | 100 % | 100 % | 100 % |
| Traceability HU → CU → test (stories) | 0 / 26 | 8 / 26 | 12 / 26 | 26 / 26 | 26 / 26 | 26 / 26 |
| Endpoints with a forbidden-role test · clinical accesses audited | – | 100 % | 100 % | 100 % | 100 % | 100 % |
| Contract tests (≥ 1 per public `api` and adapter) | – | 100 % | 100 % | 100 % | 100 % | 100 % |
| Adapter failure tests (simulator failing) | – | SSO, HCE | SSO, HCE | all (SSO, HCE, PACS) | all | all |
| Open defects | – | 0 critical | 0 critical | 0 critical, 0 major | 0 critical, 0 major | 0 critical, 0 major |
| Database restore rehearsal | – | – | – | 1 rehearsal | 1 rehearsal | documented |
| Walkthrough with the roles (usability, error protection) | – | 1 role | 2 roles | 4 roles | 4 roles | 4 roles + user guide |

If a configuration does not pass its gate, the label is not given (D-16): the gate is never lowered
to fit the date; the scope is cut instead ([`project-plan.md` §8](project-plan.md)).

#### 5.2.2 Phase entry and exit criteria

| Phase exit | Quality criteria |
|---|---|
| Inception → Elaboration | This plan and the checklist agreed; CI running; configuration test passed |
| Elaboration → Construction | Contract tests for every `api`; security design reviewed; coverage baseline taken and threshold confirmed; 0 open critical defects |
| Construction → Transition | 100 % of use case flows tested; all metrics of §4 on target or with a recorded justification |
| Transition → Delivery | Full regression green from a clean clone; final quality audit recorded |

#### 5.2.3 Defect tracking

Defects are issues with label `bug` and a severity:

| Severity | Definition | Response |
|---|---|---|
| **Critical** | Data exposure, wrong clinical behaviour (priority, booking, report state), or system down | Fixed before anything else; blocks the release |
| **Major** | A use case flow does not work; no workaround | Fixed in the same iteration; blocks the release |
| **Minor** | Wrong behaviour with a workaround | Planned in the next iteration |
| **Trivial** | Cosmetic | Backlog |

Each defect fix includes a test that reproduces it (regression).

### 5.3 Improve

1. **Quality review per iteration** (at the Friday review meeting, 15 min): the quality lead presents the metrics of §4 for the iteration, the defects found (where and by whom: review, test or user), and the trend of coverage.
2. **Root cause of every critical or major defect**: why did it pass the gates? The answer becomes a corrective action: a new checklist item, a new test type, a stricter gate.
3. **Preventive actions** from trends: for example, if most defects come from AI-generated code in one area, the design inspection of that area is reinforced before the next iteration.
4. **Adjusting the targets**: if a target is unrealistic (too strict or too loose), the change is decided at the meeting and recorded in `DECISIONS.md` and in the history of this plan.
5. **Lessons learned**: recorded in the minutes ("errors we have learnt from") and summarised in the self-assessment at the end of the project.

## 6. Reviews and audits schedule

| Week | Iteration | Reviews and audits |
|---|---|---|
| W44 | 0 | Requirements inspection; this plan agreed; configuration audit of `v0.1.0` |
| W45 | 1 | Design/contract inspection of block 1; first coverage report; security review (roles, SSO, audit) |
| W46 | 2 | **Security design review**; contract inspection of the HCE adapter; **Alpha 1 gate** (CFG-02) |
| W47 | 3 | Design inspection of the booking core; **Elaboration exit review** + **Alpha 2 gate** and audit of `v0.4.0-alpha.2` |
| W48 | 4 | Security review of reports and images (signed-report integrity) |
| W49 | 5 | Quality review; regression of blocks 1–3 |
| W50 | 6 | **Construction exit review** + **Beta gate** and audit of `v0.7.0-beta.1` |
| W51 | 7 | Full regression; **RC gate** (`v1.0.0-rc.1`); **final quality audit** (`v1.0.0`); lessons learned |

PR inspections and the quality gates run continuously in every week.

## 7. Testing

The test strategy is detailed in [`quality-testing-strategy.md`](../../quality-testing-strategy.md). In summary:

| Level | What | Characteristic |
|---|---|---|
| Unit | Domain and application logic of each module | Functional suitability, maintainability |
| Acceptance | One test per use case flow (main, alternative, exception), named after the CU | Functional suitability |
| Security | Forbidden roles, unauthenticated access, audit records, signed-report integrity | Security |
| Contract | Each `api` and each adapter, from the consumer side | Compatibility, maintainability |
| Fault injection | Simulators returning errors, timeouts or unavailability; adjustable clock for the timer | Reliability |
| Regression | Full suite on every build | All |

Tests are designed by people from the acceptance criteria; the AI agent may scaffold them; all are reviewed.

## 8. Problem reporting and corrective action

| Source | Recorded as | Follow-up |
|---|---|---|
| Review finding | PR comment, classified *blocking* / *non-blocking* | Blocking findings must be fixed before approval |
| Failed test / CI | Red build on the PR | Fixed on the same branch |
| Defect found after merge | Issue `bug` with severity (§5.2.3) | Fix + regression test; root cause if critical/major |
| Process problem | Minutes, section "errors we have learnt from" | Corrective action with owner and date |

## 9. Tools, records and retention

| Tool | Use | Record kept |
|---|---|---|
| GitHub Actions | Build, tests, coverage | Surefire and JaCoCo reports attached to every build |
| JUnit 5 | Unit, acceptance, contract and fault-injection tests | Test results in CI |
| JaCoCo | Coverage | Coverage report per build |
| ArchUnit (`ArchitectureTest`) and Maven enforcer (D-14) | Module boundaries, cycles, Java/Maven versions | Build result |
| GitHub secret scanning | Secrets in the repository | Alerts |
| GitHub PRs and Issues | Reviews and defects | Comments, approvals, issue history |
| Wiki | Quality report (page 08), iteration pages, minutes | Wiki history |

All records stay in GitHub until the end of the course: they are the evidence of this plan being applied.

## 10. Quality risks

| Risk | Effect on quality | Mitigation |
|---|---|---|
| AI-generated code accepted without real review | Hidden defects, inconsistent design | Same checklist as human code; PR states AI usage; design inspection before implementation |
| Time pressure near 13/11 and 15/12 | Gates skipped | Gates enforced by branch protection, not by goodwill; descope (project plan §8) instead of lowering quality |
| Simulators too simple | Reliability and compatibility over-estimated | Fault-injection tests; contracts based on FHIR/DICOM structures |
| Coverage target pursued for its own sake | Tests that do not check behaviour | Coverage is a minimum, not the goal; reviewers check that tests assert the acceptance criteria |
| Reviews concentrated on one person | Bottleneck and lower attention | Rotating reviewers; at most two open reviews per person |
