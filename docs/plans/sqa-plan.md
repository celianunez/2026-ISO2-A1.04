# Software Quality Assurance Plan – SESCAM Radiodiagnostics

| | |
|---|---|
| **Group** | ISO II – 3A1.04 (team of 6) |
| **Owner** | George (quality and test lead), with the whole team |
| **Status** | Draft v0.1 – to be validated at the team meeting |
| **Date** | 06/11/2026 |
| **Structure** | IEEE 730 (Software Quality Assurance), product quality model ISO/IEC 25010 |
| **Related** | [`quality-testing-strategy.md`](../../quality-testing-strategy.md) (test strategy), [`project-plan.md`](project-plan.md), [`scm-plan.md`](scm-plan.md), [`CONTRIBUTING.md`](../../CONTRIBUTING.md), `DECISIONS.md` |

## History

| Version | Date | Author | Change |
|---|---|---|---|
| v0.1 | 06/11/2026 | D. Villanueva | First version. Builds on `quality-testing-strategy.md`: justifies the characteristics for the healthcare domain, turns them into measurable targets and adds the inspection, control and improvement procedures. |

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
| Maintainability | Line coverage on `domain` and `application` code | **≥ 70 %** (report-only in It 0; confirmed after It 1) | JaCoCo report in CI | Every build |
| Maintainability | Dependency cycles / imports of another module's internals | **0** | Build rule (ArchUnit or Maven enforcer, D-05) + review | Every build |
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

| Gate | Where | Rule |
|---|---|---|
| G1 – Build and tests | CI on every PR | `mvn -B verify` must pass; a red build cannot be merged (required check) |
| G2 – Review | Branch protection | 1 approval by someone other than the author |
| G3 – Coverage | CI report | Below the threshold → the reviewer requests tests (blocking from It 2 once the threshold is confirmed) |
| G4 – Definition of Done | End of iteration | All items of the DoD (`CONTRIBUTING.md` §4) met for every issue of the iteration |
| G5 – Release audit | Release PR | FCA/PCA of [`scm-plan.md` §3.4](scm-plan.md) passed; 0 open critical/major defects |

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
| W46 | 2 | **Security design review**; contract inspection of the HCE adapter; coverage threshold confirmed |
| W47 | 3 | Design inspection of the booking core; **Elaboration exit review** + audit of `v0.4.0` |
| W48 | 4 | Security review of reports and images (signed-report integrity) |
| W49 | 5 | Quality review; regression of blocks 1–3 |
| W50 | 6 | **Construction exit review** + audit of `v0.7.0` |
| W51 | 7 | Full regression; **final quality audit**; lessons learned |

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
| ArchUnit or Maven enforcer (to confirm, D-05) | Module boundaries and cycles | Build result |
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
