# Changelog

All notable changes to this project are recorded here. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versioning: [SemVer 2.0.0](https://semver.org/), one version for the whole repository (D-15).
Entries are grouped by module. Each version corresponds to one configuration `CFG-0N`
(map in [`docs/plans/scm-plan.md`](docs/plans/scm-plan.md) §3.1.4).

## [Unreleased]
### radio-audit
- `IAuditService` contract (record with result, query; append-only), `AuditEntryDto`, `AuditQuery`, `AuditResult` (F-2, #39).
- Added append-only audit log: `AuditService` implements `IAuditService` with an immutable `AuditEntry`, a repository without update or delete, and `GET /api/audit?user=&from=&to=&resource=` for administrators only (CU-19-S, #31).

### radio-sso-adapter
- `ISsoGateway` contract (authenticate, getCorporateIdentity), `CorporateIdentityDto`, `SsoUnavailableException`, `InvalidSsoTokenException` (F-2, #39).

### radio-auth
- `IAuthenticationService` and `IAuthorizationService` contracts, `AuthenticatedUserDto`, `AuthenticationFailedException` (F-2, #39).

### radio-configuration
- `IConfigurationService` contract with validity dates, `OpeningHoursDto`, `AppointmentPolicyDto`, `PriorityLevelDto`, `InvalidParameterException` (F-2, #39).

### radio-catalogue
- `ICatalogueService` contract, `TestTypeDto`, `TestTypeData`, `Modality`, `ClinicalGuideDto`, `DuplicateTestTypeException`, `InvalidTestTypeException` (F-2, #39).


### Build
- **F-1 foundation**: parent POM `sescam-parent` `0.2.0-SNAPSHOT` with the Spring Boot 3.5.16 BOM (D-04, latest 3.5.x on Maven Central), `spring-boot-maven-plugin`, `parameters=true` in the compiler and the module list of CFG-01 (`docs/plans/scm-plan.md` §3.1.4).
- `radio-common`: `Role` (with `authority()`), `UserRef`, `DomainException`, `NotFoundException`, `ForbiddenOperationException` and `CurrentUser`, which reads the user id and the roles from the `SecurityContextHolder` (D-09). Unit tests for `CurrentUser` and `Role`.
- `radio-app`: `RadioApplication` (composition root, D-19), `ApiExceptionHandler` (404 / 403 / 400 / validation → `{ "error", "message" }`), `application.yml` with the `dev`, `test` and `demo` profiles (H2 in memory, in memory and file, D-18) and `radio.adapters.mode: simulator`; `RadioApplicationIT`; `ApiExceptionHandlerTest`.
- `ArchitectureTest` (D-14) moved from `module-b` to `radio-app`, unchanged: this module depends on every other one, so it sees all the classes.
- Skeletons of `radio-auth`, `radio-sso-adapter`, `radio-audit`, `radio-configuration` and `radio-catalogue`: `pom.xml` and one `package-info.java` per package, no classes (the contracts are F-2, the code belongs to each owner).
- Surefire also runs the `*IT` integration tests, so `mvn -B verify` and the CI cover them.

### Removed
- `module-a` and `module-b`, the configuration test of Iteration 0, replaced by the first real modules (CFG-01).

### Documentation
- Re-plan after the slippage of Elaboration: decisions D-25 to D-29 (*proposed*: compressed calendar, contracts first, resolution of P-01 to P-13, agent descriptors, team of 7 with module owners); `project-plan.md` v1.1 and `scm-plan.md` v1.1 with the new dates.

### Documentation
- `CONTEXT.md` and `AGENTS.md`: descriptors for the AI agents of every member (D-28).

## [0.1.0] - 2026-11-10 — CFG-00 · functional baseline

### Build
- Parent POM `sescam-parent` with Java 17, JUnit 5, JaCoCo report and coverage check (thresholds per release, D-17) and `maven-enforcer` (Java ≥ 17, Maven ≥ 3.8, D-14).
- `ArchitectureTest` (ArchUnit): modules use each other only through `api`, no cycles (D-03, D-05, D-14).
- CI: GitHub Actions running `mvn -B verify` and publishing Surefire and JaCoCo reports.

### module-a / module-b (configuration test)
- `Greeter` interface in `module-a` `api` and its implementation in `internal`; `module-b` uses only the interface.

### Documentation
- Development workflow, `CONTRIBUTING.md`, PR and issue templates.
- Project plan, SCM plan and SQA plan (`docs/plans/`), with the configuration map, release labels, quality gates per release, environments and rollback.
- `DECISIONS.md` (D-01…D-21), architecture proposal v0.3, quality and testing strategy, user stories spreadsheet.

[Unreleased]: https://github.com/celianunez/2026-ISO2-A1.04/compare/v0.1.0...develop
[0.1.0]: https://github.com/celianunez/2026-ISO2-A1.04/releases/tag/v0.1.0
