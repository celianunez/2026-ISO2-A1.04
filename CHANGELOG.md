# Changelog

All notable changes to this project are recorded here. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versioning: [SemVer 2.0.0](https://semver.org/), one version for the whole repository (D-15).
Entries are grouped by module. Each version corresponds to one configuration `CFG-0N`
(map in [`docs/plans/scm-plan.md`](docs/plans/scm-plan.md) §3.1.4).

## [Unreleased]

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
