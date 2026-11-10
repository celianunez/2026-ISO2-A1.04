# Decisions log – SESCAM Radiodiagnostics (ISO II – 3A1.04)

This file records the key decisions of the project.

**How to use it:**

- **Adding a decision:** give it the next ID, the date, the decision, the reason and its status.
- **Status values:**
  - *Proposed*: not agreed yet.
  - *Accepted*: agreed by the team, with the meeting date.
  - *Replaced by D-XX*: superseded by a later decision.
- **Changing a decision:** never delete an accepted one; add a new decision that replaces it.

**History of this file**

| Version | Date | Author | Change |
|---|---|---|---|
| v0.1 | 04/11/2026 | A. Aceña | First version: D-01 to D-12 and P-01 to P-08, from the architecture proposal v0.3 |
| v0.2 | 05/11/2026 | Pablo | D-01 to D-12 kept as they were. Added D-13 (module names) and P-09 to P-13 (open points from the review of the Maven structure) |
| v0.3 | 10/11/2026 | D. Villanueva | D-01 to D-13 and P-01 to P-13 kept as they were. Added D-14 to D-21 (architecture checks, versioning and configurations, release labels, quality gates per release, environments and rollback, `radio-app` from Iteration 1, package layers, 8 iterations), proposed after comparing our plans with the reference solution of the course. |

## Decisions

| ID | Date | Decision | Reason | Status |
|----|------|----------|--------|--------|
| D-01 | 04/11/2026 | **Modular monolith**: one Spring Boot application (`radio-app`) made of independent modules. | Team size and timeline. Simple deployment. Microservices would add too much overhead. | Agreed by Alejandro and Pablo – to confirm at the meeting |
| D-02 | 04/11/2026 | **One Maven submodule per component (17)** plus `radio-app`, which only starts the application. There is no central `radio-api` module. | The assignment asks for one submodule per component. See the list in `docs/architecture/architecture-proposal.md` (section 3). | Agreed by Alejandro and Pablo – to confirm at the meeting |
| D-03 | 04/11/2026 | **Each module has its own `api` package**, with its public interfaces, DTOs and REST controllers. Every interface belongs to the module of the component that provides it. Other modules only use that `api`. | Low coupling: each module can be developed and tested separately. | Agreed by Alejandro and Pablo – to confirm at the meeting |
| D-04 | 04/11/2026 | **Stack: Java 17, Maven, Spring Boot 3.5.16, JUnit 5.** | Spring Boot 3.5.16 is the stable line with Java 17 and JUnit 5. Spring Boot 4 brings JUnit 6 and renamed starters, which is not worth it with our deadlines. | Agreed by Pablo and Alejandro – to confirm at the meeting |
| D-05 | 04/11/2026 | **The dependencies between modules must have no cycles.** A module only depends on modules of a lower level (table in section 5 of the architecture document). | Avoids compilation and design problems. It also gives a build order. | Proposed |
| D-06 | 04/11/2026 | **Each module owns its own tables and repositories.** No module reads or writes another module's tables. Only exception: `radio-metrics` may run read-only queries for statistics. | Modules stay independent. The metrics need data from several modules (HU-11, HU-16, HU-22). | Proposed |
| D-07 | 04/11/2026 | **Only `radio-request` changes the status of a request.** The other modules call `IRequestService` (`markScheduled`, `markPerformed`, `markReported`…). | A single source of truth for the request lifecycle (HU-08). | Proposed |
| D-08 | 04/11/2026 | **External systems (SSO, HCE/admission, RIS/PACS) go through adapter modules with a simulator.** Integration failures are recorded and never block the rest of the system. | Required by HU-20, HU-21, HU-24 and CU-42. We can develop and test without the real systems. | Proposed |
| D-09 | 04/11/2026 | **Security with Spring Security, configured in `radio-auth`.** Permissions are checked on the server, not only in the UI. The other modules use the roles and the current user from Spring Security. | HU-18. Modules do not need a dependency on `radio-auth`, which avoids cycles. | Proposed |
| D-10 | 04/11/2026 | **Audit in `radio-audit`, append-only.** Every access to clinical data is recorded, and records cannot be edited or deleted. | HU-19, CU-38. GDPR: health data. | Proposed |
| D-11 | 04/11/2026 | **A single metrics module (`radio-metrics`)** for HU-11, HU-16 and HU-22. | Suggested in the user stories Excel. The same queries are not repeated three times. | Proposed |
| D-12 | 04/11/2026 | **Modules communicate through direct calls to the interfaces.** There is no event bus for now. Images are not stored, only retrieved from the PACS. | Simpler, and it matches the «include» relations of the use cases. HU-24 asks for images "without duplicating them". | Proposed |
| D-13 | 05/11/2026 | **Module names follow the architecture proposal v0.3**, in singular: `radio-auth`, `radio-configuration`, `radio-catalogue`, `radio-patient`, `radio-request`, `radio-scheduling`, `radio-execution`, `radio-report`, `radio-image`, `radio-feedback`, `radio-metrics`, `radio-notification`, `radio-audit`, `radio-jobs`, `radio-sso-adapter`, `radio-hce-adapter`, `radio-pacs-adapter`, plus `radio-app`. | The architecture proposal renumbers the components as C1–C17 and splits the integration module into three adapter modules, one per external system, each with its simulator. Having one name per module in the POMs, the diagrams and the documents avoids the mismatch we had in v0.2. | Proposed |
| D-14 | 10/11/2026 | **Module boundaries and cycles are checked by the build**: an ArchUnit test (a class of module X may only use the `api` package of another module Y; no cycles between modules) and `maven-enforcer` (Java ≥ 17, Maven ≥ 3.8). It runs inside `mvn -B verify`, so the CI enforces it on every PR. | D-05 and D-03 were rules only "by convention"; with code generated by an AI agent a rule that is not executed is not a rule. Closes the tool choice left open in `sqa-plan.md` §9. | Proposed – to confirm at the meeting |
| D-15 | 10/11/2026 | **One version for the whole repository until `1.0.0`, and one identified configuration per iteration**: every iteration ends with `CFG-0N`, an annotated tag `cfg-iter0N` on `develop` with a version without `SNAPSHOT`. The configuration is the tag plus the list of modules that exist and that changed (map in `scm-plan.md` §3.1.4). | Resolves R-12 of `development-workflow.md`. All modules are deployed together (D-01), so per-module versions would multiply the release work by 18 without adding information; the CFG map keeps the per-module view. | Proposed – to confirm at the meeting |
| D-16 | 10/11/2026 | **Milestone configurations are labelled with SemVer pre-releases**: CFG-02 `0.3.0-alpha.1` (alpha 1), CFG-03 `0.4.0-alpha.2` (alpha 2 = architecture baseline), CFG-06 `0.7.0-beta.1` (beta), CFG-07 `1.0.0-rc.1` (release candidate) and `1.0.0` (final). A label is given when its **exit criteria are met, not when the date arrives**; otherwise the label is delayed and the reason is written in the minutes. | The assignment asks to anticipate alpha, beta and final versions on top of the minimum configurations. Criteria make the label verifiable. | Proposed – to confirm at the meeting |
| D-17 | 10/11/2026 | **Quality gates grow with each release** (coverage, traceability, adapter failure tests, defects, restore rehearsal), table in `sqa-plan.md` §5.2.1. The JaCoCo `check` thresholds are the properties `coverage.line.minimum` and `coverage.branch.minimum` of the parent POM, raised on each `release/*` branch. | A single 70 % target from day one is either ignored (too strict early) or too weak at the end. | Proposed – to confirm at the meeting |
| D-18 | 10/11/2026 | **Environments `dev` → `ci` → `test` → `alpha` → `beta` → `demo`, and rollback = check out a `cfg-iter0N` tag, rebuild with `mvn -B clean verify`, redeploy that jar and restore the database copy of that configuration.** Every release has a `DEPLOY-<version>.md` with the exact module list. | Every configuration must be deployable and recoverable, not only buildable. Depends on P-04 (database). | Proposed – to confirm at the meeting |
| D-19 | 10/11/2026 | **`radio-app` is created in Iteration 1**, with the first modules, and grows every iteration. | Without the composition root no alpha can be run end to end. Replaces the wiki draft that created it in Transition. | Proposed – to confirm at the meeting |
| D-20 | 10/11/2026 | **Packages inside a module: `api`, `domain`, `application`, `infrastructure`** (architecture proposal v0.3 §2). Only `api` may be used by other modules (D-03, checked by D-14). The `internal` package of the configuration-test modules `module-a`/`module-b` is equivalent to `domain` + `application` + `infrastructure`. | The wiki draft used `api/domain/internal/app`; one layout everywhere. | Proposed – to confirm at the meeting |
| D-21 | 10/11/2026 | **8 iterations of one week, grouped by blocks of user stories** (`project-plan.md` §5). Replaces the draft with 28 iterations (one per story) that remained on some wiki pages. Each use case is still realised in one and only one iteration. | Justified in `project-plan.md` §5.2 (capacity, overhead, integration). | Proposed – to confirm at the meeting |

## Pending decisions (to resolve at the meeting)

| ID | Question | Proposal | Related |
|----|----------|----------|---------|
| P-01 | Which is the source of truth for patient data: our system or HCE/admission? | HCE/admission when the patient exists there. We keep a local copy and the data HCE does not have. | HU-12, HU-21 |
| P-02 | Which SSO protocol: OpenID Connect or SAML? | OpenID Connect: simpler, and easy to simulate. | HU-20 |
| P-03 | Where is the boundary between configuration, catalogue and priorities? | Catalogue = attributes of each test type. Configuration = global parameters (schedules, policies, priority levels). The waiting list uses the priority levels from configuration. | HU-15, HU-17, HU-23 |
| P-04 | Which database? | H2 for development and tests; PostgreSQL or MySQL for the demo. | – |
| P-05 | UI technology? | Static web pages (HTML/JS) inside `radio-app` that call the REST api of each module. Alternative: Thymeleaf. | all |
| P-06 | Notification channels? | In-app inbox and an email stub first; push later. | HU-02 |
| P-07 | Are there only 4 roles (patient, physician, admissions staff, administrator)? | Yes, as in HU-18. The physician also records the exam execution (HU-10). | HU-18 |
| P-08 | Should the waiting list (HU-15) be a separate module, to make `radio-scheduling` smaller? | Not for now (17 components). Revisit if scheduling grows too much. | HU-15 |
| P-09 | Do we keep `radio-common` as a technical module for the shared value objects (`UserRef`, `DomainException`)? | Yes, for now: the repository has 19 POMs (17 components + `radio-app` + `radio-common`). If the team prefers exactly 18, those few types move to the modules that use them. | D-02, wiki *Module Structure* |
| P-10 | Where do the REST controllers live: inside `api` (as D-03 says) or in a separate `web` package? | Separate `web` package, so `api` stays a contract of interfaces and DTOs and other modules do not depend on Spring MVC classes. | D-03 |
| P-11 | Do the interfaces keep the `I` prefix (`IPatientService`, `IRequestService`)? | Yes, as in the architecture proposal and as generated by Visual Paradigm. Renaming 17 interfaces later costs much more than now. | architecture proposal §4 |
| P-12 | Does `radio-catalogue` record the audit entry when a test type is created? | Yes: `radio-catalogue` depends on `radio-audit` (no cycle, both are low level). The dependency table of the architecture proposal leaves the catalogue without dependencies, so this needs to be added there. | HU-19, HU-23 |
| P-13 | How does `radio-metrics` get the data of the other modules: read-only queries on their tables (D-06 exception) or their query APIs? | Read-only queries, as D-06 says. The query APIs of the other modules would have to expose every field the metrics need. | D-06, D-11 |
