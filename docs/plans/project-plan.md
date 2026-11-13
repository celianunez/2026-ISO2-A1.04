# Project Plan (Unified Process) – SESCAM Radiodiagnostics

| | |
|---|---|
| **Group** | ISO II – 3A1.04 (team of 6) |
| **Owner** | Coordinator (Carlos), with the whole team |
| **Status** | Final v1.0 – approved by the team |
| **Date** | 13/11/2026 |
| **Related** | [`scm-plan.md`](scm-plan.md), [`sqa-plan.md`](sqa-plan.md), [`development-workflow.md`](../workflow/development-workflow.md), [`architecture-proposal.md`](../architecture/architecture-proposal.md), [`DECISIONS.md`](../../DECISIONS.md), `user_stories.xlsx` (repository root), wiki pages 03, 04 and 07 |

## History

| Version | Date | Author | Change |
|---|---|---|---|
| v0.1 | 06/11/2026 | D. Villanueva | First draft. Consolidates the estimates of the user stories spreadsheet, the 8 block-based iterations of the wiki and the architecture decisions D-01…D-13, and adds the justification of every trade-off. |
| v0.2 | 10/11/2026 | D. Villanueva | Milestones linked to the configurations and release labels of the SCM plan (alpha 1, alpha 2, beta, release candidate, final; D-15, D-16) and to the quality gates per release of the SQA plan (D-17). The 8 iterations are recorded as decision D-21. |
| v1.0 | 13/11/2026 | Whole team | Final version, approved by the team and submitted as Theoretical Assignment 1 (D-22). |

---

## 1. Purpose and scope

This plan explains **how** the team will build the comprehensive radiodiagnostics management system for SESCAM, **in what order**, **with what effort and cost**, and **why** each of those choices was made. It covers the whole life of the course project, from Inception (October 2026) to the practical delivery on **15 December 2026**.

The numbers in this document are not the goal by themselves: every table is followed by the reasoning that produced it, and by what would happen if the assumption behind it were wrong.

**In scope:** 26 user stories (HU) in 4 epics, 45 use cases (CU), 17 components, simulated SSO, HCE/admission and RIS/PACS, fictitious data only.
**Out of scope:** real connections to hospital systems, real patient data, image storage, native mobile apps.

## 2. Process model

### 2.1 Why the Unified Process

| UP principle | How it shows in this project | Why it fits |
|---|---|---|
| **Use-case driven** | The 26 HU are mapped to 45 CU; each iteration is defined by the CU it delivers, and the tests come from the CU flows. | The client (SESCAM) describes its needs as user goals: "prescribe a test", "book an appointment". Use cases keep those goals as the unit of planning, design and testing. |
| **Architecture-centric** | Elaboration exists to stabilise the architecture: modules, `api` contracts, adapters with simulators (D-01…D-13). | 17 modules built by 6 people only integrate if the contracts are fixed early. A late architecture change would touch every module. |
| **Iterative and incremental** | 8 iterations of one week; each one ends with tested, merged code. | The domain is new for the team. Short iterations give real data (hours, defects) to correct the estimate after the first two. |
| **Risk-driven** | The riskiest and most depended-upon stories go first (security, audit, external integrations). | If something is going to break the plan, it should break in November, not in December. |

A purely sequential (waterfall) plan was discarded because the requirements of the integrations (FHIR/HL7, DICOM) are only partially known, and an agile process without phases was discarded because the course asks for UP milestones and evidence per phase.

### 2.2 Disciplines inside each iteration (RADIT)

Every iteration goes through **R**equirements, **A**nalysis, **D**esign, **I**mplementation and **T**esting for the use cases it delivers. The weight of each discipline changes with the phase: Inception is mostly requirements and planning, Elaboration is mostly analysis and design, Construction is mostly implementation and testing.

### 2.3 Humans and the AI agent

Engineers own requirements, analysis, interface design, test design and reviews. The AI agent may generate code against fixed interfaces and scaffold tests, and every line it produces goes through the same pull request review as human code. The detailed split is in [`development-workflow.md` §3.6–3.7](../workflow/development-workflow.md). This choice has a direct effect on the estimate (§7.3).

## 3. Organisation and roles

| Member | Main role | Owned document / area |
|---|---|---|
| Carlos | Coordinator (requirements manager) | Project plan, agenda, meetings |
| David | Secretary, configuration manager | Minutes, workflow, SCM plan |
| Celia | Analyst | User stories, use cases, wiki |
| Alejandro | Architect / designer | Architecture proposal, components, interfaces |
| Pablo | Designer / programmer | Maven structure, coding conventions |
| George | Quality and test lead | Quality strategy, SQA plan |

Because the team is small, everybody also programs and tests: each member owns the iterations or the use cases assigned at the weekly planning meeting. Roles are recorded in the minutes and in the wiki home page.

## 4. Phases and milestones

| Phase | Iterations | Weeks | Exit criterion (milestone) | Date |
|---|---|---|---|---|
| **Inception** | 0 | W44 | Requirements v2 and use cases agreed; plan, workflow and CI in place; configuration test passed | 01/11/2026 |
| **Elaboration** (alpha) | 1–3 | W45–W47 | **Alpha 1** at the end of It 2 (`v0.3.0-alpha.1`, 15/11/2026: login → patient → prescription end to end). **Architecture baseline = Alpha 2** (`v0.4.0-alpha.2`): contracts frozen, security and audit working, the booking core works end to end (prescribe → book → notify) | 22/11/2026 |
| **Construction** (beta) | 4–6 | W48–W50 | **Initial operational capability = Beta** (`v0.7.0-beta.1`): every story delivered and tested; full path from prescription to signed report | 13/12/2026 |
| **Transition** | 7 | W51 | **Release candidate** `v1.0.0-rc.1` (14/12) → **product release `v1.0.0`**: integrated tagged build, wiki complete, self-assessment | 15/12/2026 |

External fixed dates: **theory report on 13/11/2026** (W46) and **practical delivery on 15/12/2026** (W51).

A milestone is reached when the exit criteria of its label are met ([`scm-plan.md` §4.2](scm-plan.md)) and its
quality gate is passed ([`sqa-plan.md` §5.2.1](sqa-plan.md)), not when the date arrives. If it is not reached,
the label moves to the next configuration and the re-plan follows §10.

## 5. Iteration plan

### 5.1 The plan

| It. | Phase | Week | Block | User stories | SP | h (no AI) |
|---|---|---|---|---|---:|---:|
| 0 | Inception | W44 | — | Skeleton, CI, configuration test | – | 40 |
| 1 | Elaboration | W45 | 1 · Cross-cutting base | HU-18, HU-20, HU-19, HU-17, HU-23 | 25 | 70 |
| 2 | Elaboration | W46 | 2a · Patient data and prescription | HU-21, HU-12, HU-06 | 18 | 52 |
| 3 | Elaboration | W47 | 2b · Appointments | HU-13, HU-01, HU-26, HU-02 | 18 | 50 |
| 4 | Construction | W48 | 3a · Execution, reports, images | HU-10, HU-09, HU-24, HU-25 | 16 | 44 |
| 5 | Construction | W49 | 3b · Follow-up and lists | HU-07, HU-08, HU-14, HU-15, HU-03 | 19 | 52 |
| 6 | Construction | W50 | 4 · Exploitation | HU-04, HU-05, HU-11, HU-16, HU-22 | 20 | 54 |
| 7 | Transition | W51 | — | Integration, packaging, delivery | – | 20 |
| | | | | **26 stories** | **116** | **382** |

### 5.2 Why the stories are grouped this way

**1. Dependencies decide the order.** Each user story in the spreadsheet lists the stories it depends on. The plan is a topological order of that graph: no story is planned before a story it depends on. For example HU-13 (assign appointments) depends on HU-06, HU-12, HU-17 and HU-23, so it cannot start before Iteration 3.

**2. The critical path.** The longest chain of dependencies is:

```text
HU-21 HCE (It 2) + HU-18 roles (It 1)
  └─> HU-12 patients (It 2) ─> HU-06 prescribe (It 2)
        └─> HU-13 book (It 3) ─> HU-10 execute (It 4) ─> HU-09 report (It 4)
              └─> HU-25 images (It 4) / HU-03 history (It 5)
```

HU-23 (catalogue) and HU-17 (configuration) join the chain at HU-06 and HU-13, both from Iteration 1.

It crosses five iterations (1 → 5). Any delay on this chain delays the delivery; a delay on a story outside it (for example HU-04 surveys) does not. That is why the people with more experience are assigned to the chain first.

**3. Risk first.** Block 1 (roles, SSO, audit, configuration, catalogue) is the least visible block for the client, but every other story depends on it, and it contains the two quality characteristics with the highest priority in a healthcare system: security and accountability ([`sqa-plan.md` §3](sqa-plan.md)). The HCE integration (HU-21, the largest story, 8 SP) goes in Iteration 2 for the same reason: an integration problem discovered in December would be fatal.

**4. Architecture-centric Elaboration.** Iterations 1–3 together touch almost every module type: cross-cutting (auth, audit, configuration), an external adapter (HCE), core domain (patients, requests, scheduling) and notifications. When Elaboration ends, every kind of contract has been written and tested at least once, which is what an architecture baseline means.

**5. Why blocks 2 and 3 are split.** Block 2 has 36 SP / 102 h and block 3 has 35 SP / 96 h. One week cannot absorb ~100 h (see the capacity analysis in §7.4), so each is split in two halves along a dependency boundary: 2a (data and prescription) is the input of 2b (appointments); 3a (execution and reports) is the input of 3b (follow-ups, lists and history).

**6. Why not one iteration per story.** An earlier draft had 28 iterations (one story each, up to five per week). It was rejected because: five iterations a week means five planning/review cycles a week, which is pure overhead for a team of six; each iteration would be too small to be integrated and demonstrated; and it hid the real constraint, which is weekly capacity, not the number of stories. The rule *one use case is realised in one and only one iteration* still holds: each CU appears in exactly one iteration.

**7. The theory report week.** W46 contains the theory report (13/11). Iteration 2 has fewer stories (3) than Iteration 1 (5) to leave room for it.

### 5.3 What each iteration produces

Each iteration ends with: the CU documented (analysis and design diagrams in Visual Paradigm), the code merged into `develop` through reviewed pull requests, acceptance tests for every flow of every CU, the real hours recorded, and an identified **minimum configuration** `CFG-0N` (tag `cfg-iter0N`) with its version (`0.2.0` … `1.0.0`) and, at the milestones, its label (alpha, beta, release candidate, final): see the map in [`scm-plan.md` §3.1.4](scm-plan.md). The iteration page in the wiki records who did each part.

## 6. Architectural trade-offs

The architecture is described in [`architecture-proposal.md`](../architecture/architecture-proposal.md) and the decisions in [`DECISIONS.md`](../../DECISIONS.md). This section explains the cost of each choice for the plan.

| # | Decision | Alternative discarded | Why | Cost / accepted risk |
|---|---|---|---|---|
| D-01 | **Modular monolith**, one Spring Boot application | Microservices | Six students, seven weeks: microservices add deployment, network failures, distributed transactions and one pipeline per service. None of that adds value to the course goals. | If one module is slow or fails, the whole application is affected. Accepted: the system is a prototype with simulated load. |
| D-02 / D-03 | **One Maven module per component (17)**, each with its own `api` package | One central `radio-api` module with all interfaces | The assignment asks for one submodule per component and SemVer per component. A central API module would make every interface change a change in a shared module that everyone depends on. | More POMs to maintain (17 + app). Accepted: the parent POM centralises versions and plugins. |
| D-05 | **No cycles between modules**, dependencies only towards lower levels | Free dependencies | Gives a build order and an implementation order (lower levels first: Block 1). | Some features need a call "upwards"; they must be solved with an interface in the lower module. |
| D-07 | **Only `radio-request` changes the state of a request** | Each module updates the state it needs | One source of truth for the lifecycle of a request (HU-08). | `radio-request` is on the critical path; it is built in Iteration 2. |
| D-08 | **Adapter + simulator for each external system** | Connecting to real systems, or no integration at all | Real systems are out of scope (confirmed by the professor), but the integration must be designed. The simulator lets us test failures (timeouts, errors) that a real system would rarely give us. | The simulator may not behave like a real HCE/PACS. Accepted: the contract is the deliverable; replacing the simulator does not change the rest of the system. |
| D-11 | **One metrics engine** for HU-11, HU-16 and HU-22 | Three separate implementations | The three stories need the same queries. | HU-11, HU-16 and HU-22 are planned together (Iteration 6) so the engine is designed once. |
| D-12 | **Direct calls between modules**, no event bus | Asynchronous events | Simpler to debug and test; matches the «include» relations of the use cases. | Notifications are synchronous: if the notification channel fails, the failure is recorded and does not block the action (D-08). |
| D-10 | **Append-only audit** | Normal table that can be updated | Health data (GDPR art. 9) require proof of who accessed what. | Audit records grow forever; acceptable at prototype scale. |

Planning consequence: Elaboration is longer (3 iterations, 172 h) than Construction per story, because the architectural work is front-loaded on purpose.

## 7. Estimation

### 7.1 Method

1. **Story points** (Fibonacci scale 1, 2, 3, 5, 8, 13) were assigned to each story by the team, comparing stories with each other, not by guessing hours. Points measure size, complexity and uncertainty together.
2. **Conversion to hours** with the table of the spreadsheet (sheet *Parámetros*):

| SP | Hours (no AI) | Hours per point |
|---:|---:|---:|
| 2 | 4 | 2.0 |
| 3 | 8 | 2.7 |
| 5 | 14 | 2.8 |
| 8 | 24 | 3.0 |
| 13 | 40 | 3.1 |

**Why the conversion is not linear:** bigger stories have more alternative flows, more integration points and more uncertainty, so each point costs more. This is the usual diseconomy of scale of software estimation. It also creates an incentive to split big stories: HU-24 (from HU-21), HU-25 (from HU-09) and HU-26 (from HU-13) were split for this reason, keeping the same total effort.

3. **Enabling work** that carries no story points is estimated directly: Iteration 0 (skeleton, CI, configuration test) 40 h and Iteration 7 (integration and delivery) 20 h.

**Result:** 116 SP → 322 h for the stories + 60 h enabling work = **382 h without AI**.

### 7.2 Cost

The rates are course-scale assumptions, recorded in `DECISIONS.md` when accepted:

| Role | €/h | Share of the work (assumed) | Contribution (€/h) |
|---|---:|---:|---:|
| Analyst | 50 | 30 % | 15.00 |
| Designer | 45 | 20 % | 9.00 |
| Programmer | 30 | 35 % | 10.50 |
| Tester | 20 | 15 % | 3.00 |
| **Blended rate** | | 100 % | **37.50** |

Every member plays several roles, so a single blended rate is used instead of tracking the role of every hour. The share comes from the RADIT weight per phase: analysis and design dominate Elaboration, implementation and testing dominate Construction.

| Concept | Hours | Cost (€) |
|---|---:|---:|
| Development work (382 h × 37.50) | 382 | 14,325.00 |
| Weekly meetings (6 people × 1 h × 8 weeks × 37.50) | 48 | 1,800.00 |
| **Subtotal** | 430 | 16,125.00 |
| Contingency (10 %) | – | 1,612.50 |
| **Budget** | | **17,737.50** |

**Why a 10 % contingency:** the team has no historical velocity. The contingency covers the expected error of the first estimate and is released (or consumed) at the review of Iteration 2, when real data exist.

### 7.3 Sensitivity analysis

A single number would hide how fragile the estimate is. The two assumptions with the largest effect are the AI factor and the rate.

**AI factor** (applied to the 322 h of stories; enabling work with AI fixed at 36 h):

| Scenario | Factor | Hours | Cost (€) | Comment |
|---|---:|---:|---:|---|
| Optimistic | 0.5 | 197 | 7,387.50 | Spreadsheet assumption: agent generates most boilerplate |
| Realistic | 0.7 | 261 | 9,802.50 | Review and correction of generated code takes real time |
| No AI (baseline) | 1.0 | 382 | 14,325.00 | Budget baseline |

**Rate** (382 h): an implementation-heavy distribution (20/20/45/15) gives 35.50 €/h → 13,561 €; an analysis-heavy one (40/20/30/10) gives 40.00 €/h → 15,280 €. The cost moves ±6 %, much less than with the AI factor (up to −48 %).

**Conclusion:** the AI factor is the assumption that matters. That is why the **budget baseline is the no-AI figure**, the AI figure is reported only as a scenario, and the factor is replaced by the measured value after two iterations (§10).

### 7.4 Capacity against load

| Week | Planned h (no AI) | h/person (6) | Planned h (AI 0.5) | h/person |
|---|---:|---:|---:|---:|
| W44 (It 0) | 40 | 6.7 | 24 | 4.0 |
| W45 (It 1) | 70 | 11.7 | 35 | 5.8 |
| W46 (It 2) | 52 | 8.7 | 26 | 4.3 |
| W47 (It 3) | 50 | 8.3 | 25 | 4.2 |
| W48 (It 4) | 44 | 7.3 | 22 | 3.7 |
| W49 (It 5) | 52 | 8.7 | 26 | 4.3 |
| W50 (It 6) | 54 | 9.0 | 27 | 4.5 |
| W51 (It 7) | 20 | 3.3 | 12 | 2.0 |

Assuming a realistic availability of **about 8 h per person per week** (48 h for the team) for this subject, the no-AI plan exceeds capacity in W45 and is at the limit in W46–W50, while the AI scenario fits with margin. Two consequences:

1. The plan is only feasible if the AI agent really saves time. This is the main schedule risk (R-01 in §9) and it is checked at the end of Iteration 1.
2. Iteration 1 is the heaviest (70 h) on purpose: it is the riskiest block, and it is better to discover a capacity problem in week 1 of Elaboration than in Construction.

## 8. Schedule trade-offs

| Trade-off | Decision | Reason |
|---|---|---|
| Scope vs. date | **The date is fixed (15/12); scope is the variable.** | The delivery date cannot move. |
| What is cut first | **Block 4 (Iteration 6: HU-04, HU-05, HU-11, HU-16, HU-22).** | No other story depends on block 4 (it is a leaf of the dependency graph), so cutting it does not break anything already built. The client loses statistics and surveys, but keeps the clinical flow. |
| What is never cut | Block 1 and the critical path (§5.2). | Without them there is no working system. |
| Buffer | Iteration 7 is a stabilisation iteration with no new features. | It absorbs defects and integration problems. Note that the delivery is on Tuesday 15/12, so in practice the buffer is the weekend before plus two days. |
| Theory report week | Lighter Iteration 2. | Avoid competing with the theory report. |
| Integration | Continuous (every PR builds all modules) instead of a final big integration. | A late "big bang" integration is the classic failure of plans that read *1 CU = 1 iteration* literally. |

## 9. Risk management

| ID | Risk | Prob. | Impact | Mitigation | Owner |
|---|---|---|---|---|---|
| R-01 | The AI factor of 0.5 is optimistic; the plan exceeds capacity | High | High | Measure real hours from Iteration 1; descope block 4 if velocity < 70 % of plan after Iteration 2 | Coordinator |
| R-02 | The architecture is not agreed in time and modules are built on different assumptions | Medium | High | Decisions D-01…D-13 confirmed at the meeting before Iteration 1; interface changes only through `DECISIONS.md` and a PR | Architect |
| R-03 | It builds on one machine but not in CI (6 machines) | High | Medium | Temurin 17 + Maven in CI; "clone and `mvn -B verify`" as the entry checklist | Configuration manager |
| R-04 | Only one person can merge, protect branches and release | Medium | High | Second administrator; release rehearsal documented | Coordinator |
| R-05 | Integration simulators do not behave like real systems | Medium | Medium | Contract tests on the adapters; failures always recorded, never blocking (D-08) | Architect |
| R-06 | A member leaves or does not contribute | Medium | High | Each iteration has two people who know it; work assigned through issues, so it can be reassigned | Coordinator |
| R-07 | Real data or secrets end up in the repository | Low | High | Fictitious data only; review checklist; secret scanning | Quality lead |
| R-08 | Dates compress around 13/11 and 15/12 | High | Medium | Lighter Iteration 2; Iteration 7 with no features; re-plan recorded in the minutes | Coordinator |

Risks are reviewed at every weekly meeting; a risk that materialises gets an issue and an entry in the project history.

## 10. Monitoring and control

| What | How | When | Action threshold |
|---|---|---|---|
| Real hours per iteration | Logged by each member on the issue; summed in wiki page 06 | End of each iteration | Iteration > 150 % of its estimate → reason in the minutes, re-estimate similar stories |
| Velocity | Story points done per week | From Iteration 2 | Velocity < 70 % of plan → descope according to §8 |
| AI factor | Real hours with AI ÷ estimated hours without AI | After Iterations 1 and 2 | Replace 0.5 in the forecast with the measured value |
| Schedule (simplified earned value) | PV = planned hours of the iterations due; EV = planned hours of the iterations *done*; AC = real hours | Weekly | SPI = EV/PV < 0.8 or CPI = EV/AC < 0.8 → re-plan at the next meeting |
| Quality gates | See [`sqa-plan.md`](sqa-plan.md) | Every PR | A red build is never merged |

Every re-plan is recorded in the minutes, in the wiki agenda (page 04) and in the project history (page 10). A plan that is never changed is not evidence of good planning; a plan whose changes are explained is.

## 11. Assumptions and open points

| # | Assumption / open point | Effect if wrong | Status |
|---|---|---|---|
| A-1 | Availability of ~8 h per person per week | Capacity analysis in §7.4 | To confirm at the meeting |
| A-2 | AI factor 0.5 | Sensitivity in §7.3 | Measured after Iteration 2 |
| A-3 | Rates and blended rate 37.50 €/h | ±6 % of cost | To record in `DECISIONS.md` |
| A-4 | D-01…D-21 accepted as proposed | Re-plan of Elaboration | To confirm at the meeting |
| A-5 | UI technology and database (P-04, P-05) | Needed before Iteration 1 | Open |
