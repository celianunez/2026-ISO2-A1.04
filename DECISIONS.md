# Decisions log – SESCAM Radiodiagnostics (ISO II – 3A1.04)

This file records the key decisions of the project.

**How to use it:**

- **Adding a decision:** give it the next ID, the date, the decision, the reason and its status.
- **Status values:**
  - *Proposed*: not agreed yet.
  - *Accepted*: agreed by the team, with the meeting date.
  - *Replaced by D-XX*: superseded by a later decision.
- **Changing a decision:** never delete an accepted one; add a new decision that replaces it.

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
| P-08 | Should the waiting list (HU-15) be a separate module, to make `radio-scheduling` smaller? | Not for now (17 modules). Revisit if scheduling grows too much. | HU-15 |
