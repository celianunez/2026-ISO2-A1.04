# CONTEXT.md — SESCAM Radiodiagnostics

Permanent context for every person and every AI agent working in this repository (decision D-28).
How an agent must behave is in [`AGENTS.md`](AGENTS.md). Decisions: [`DECISIONS.md`](DECISIONS.md).
Plan and dates: [`docs/plans/project-plan.md`](docs/plans/project-plan.md) §5. Rules for branches,
commits and PRs: [`CONTRIBUTING.md`](CONTRIBUTING.md).

## 1. Product

Management of diagnostic imaging tests for SESCAM: prescription → appointment → exam → signed report →
patient follow-up. 26 user stories = 26 functional requirements = 26 use cases (HU-nn = FR-nn = CU-nn, D-23).
Each use case has a client part `CU-nn-C` and a server part `CU-nn-S` (D-24), each with its own issue.

Roles (D-27, P-07): `PATIENT`, `PHYSICIAN`, `ADMISSIONS`, `ADMINISTRATOR`.

## 2. Technology

Java 17 · Maven multi-module · Spring Boot 3.5.x · Spring Security · Spring Data JPA · H2 (in memory for
`dev`/`test`, file for `demo`) · JUnit 5 + Mockito + ArchUnit · JaCoCo · static HTML/JS client served by
`radio-app` (D-04, D-27). No new library without a line in `DECISIONS.md`.

## 3. Architecture

Modular monolith (D-01): one Spring Boot application (`radio-app`) made of one Maven module per component (D-02).

| Module | Responsibility | Owner (D-29) |
|---|---|---|
| `radio-common` | `Role`, `UserRef`, `DomainException` and subclasses, `CurrentUser` | Pablo |
| `radio-app` | `@SpringBootApplication`, profiles, error handler, `ArchitectureTest`; **no business logic** | Pablo (Java), Orhan (`static/`) |
| `radio-audit` | Append-only audit of clinical data access | George |
| `radio-sso-adapter` | `ISsoGateway` + SSO simulator (OpenID Connect simulated) | Alejandro |
| `radio-auth` | Login, Spring Security configuration; users and roles | Alejandro (security), Carlos (roles) |
| `radio-configuration` | System parameters | Celia |
| `radio-catalogue` | Test types, clinical guides | Pablo |
| `radio-hce-adapter` | `IHceGateway` + HCE/admission simulator | Pablo |
| `radio-patient` | Patients | Carlos |
| `radio-request` | Diagnostic requests; the only module that changes a request's status | Alejandro |
| `radio-scheduling` | Agenda, booking, worklists, waiting list | Celia |
| `radio-notification` | Notifications, reminders, patient inbox, email stub | David |
| `radio-jobs` | Timer: due reminders, follow-up generation | David |
| `radio-execution` | Exam execution and incidents | George |
| `radio-report` | Reports (signed = new version), patient history | Carlos |
| `radio-image` | DICOM viewer access | Alejandro |
| `radio-pacs-adapter` | `IPacsGateway` + PACS simulator | Pablo |
| `radio-feedback` | Patient incidents, satisfaction surveys | George |
| `radio-metrics` | One metrics engine (CU-11, CU-16, CU-22), read-only queries | David |

Allowed dependencies between modules (architecture proposal §5 + P-12). Every module may also use `radio-common`:

| Module | Depends on |
|---|---|
| audit, metrics, sso-adapter, hce-adapter, pacs-adapter | – |
| catalogue, configuration | audit |
| auth | sso-adapter, audit |
| patient | hce-adapter, audit |
| image | pacs-adapter, audit |
| request | patient, catalogue, audit |
| notification | configuration, catalogue |
| scheduling | request, patient, catalogue, configuration, notification |
| jobs | notification, request |
| execution | scheduling, request |
| feedback | scheduling |
| report | request, execution, image, notification, audit |
| radio-app | all |

Packages inside each module (D-20, P-10), `<m>` = module name without `radio-`:

```
es.uclm.sescam.<m>.api             interfaces I...Service / I...Gateway, DTO records, api exceptions — the only package other modules may import
es.uclm.sescam.<m>.web             REST controllers under /api/...
es.uclm.sescam.<m>.application     implementation of the api interfaces
es.uclm.sescam.<m>.domain          entities, value objects, business rules
es.uclm.sescam.<m>.infrastructure  JPA repositories, simulators, external clients
```

## 4. Rules

Checked by the build (`mvn -B verify`, CI on every PR, D-14):
- A module uses another module **only through its `api` package** (ArchUnit).
- **No cycles** between modules (ArchUnit).
- Java ≥ 17, Maven ≥ 3.8 (enforcer). Coverage of `domain` + `application` above the gate of the current release (JaCoCo, D-17).

Checked by review:
- Each module owns its tables; never touch another module's tables or repositories (D-06; metrics reads only, P-13).
- Only `radio-request` changes a request's status (D-07).
- External systems only through an adapter with a simulator; a failing simulator is recorded and never blocks the rest (D-08).
- Permissions on the server with `@PreAuthorize`; `/api/**` requires login by default (D-09, HU-18).
- Every read or change of clinical data calls `IAuditService.record` (D-10); audit records are never edited or deleted.
- Direct Java calls between modules through their `api` (D-12). Contracts are written first, by the architect (D-26).
- Fictitious data only.

## 5. Definition of Done of a use case part

- An acceptance test for every flow of the specification (main, alternatives, exceptions).
- A forbidden-role test (403) for every endpoint.
- A contract test for every public `api` interface provided; adapters also tested with the simulator failing.
- Clinical data access audited (verified in a test).
- Javadoc on every public type in `api`.
- `mvn -B verify` green locally and in CI; PR reviewed by someone else; issue closed; *Requirements Traceability Matrix* updated; real hours recorded.
- Sequence and design class diagrams of the use case (PlantUML) linked from the wiki page of the iteration.

## 6. Conventions

English everywhere · 4 spaces, 120 columns · immutable `record` DTOs · `Optional` instead of `null` · unchecked
exceptions extending `DomainException` (`NotFoundException` → 404, `ForbiddenOperationException` → 403, others → 400) ·
SLF4J logger, no `printStackTrace()` · no static mutable state · no business logic in controllers, DTOs or JavaScript ·
`java.time.Clock` injected for anything time-based · tests named after the behaviour, Arrange–Act–Assert.

## 7. Files to read before working on a use case

1. This file and [`AGENTS.md`](AGENTS.md).
2. The issue of the use case part and its parent issue (specification, contract, acceptance criteria).
3. The `api` interfaces you implement or call.
4. [`docs/architecture/architecture-proposal.md`](docs/architecture/architecture-proposal.md) §3–§6 when in doubt.
