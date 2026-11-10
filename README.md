# 2026-ISO2-A1.04

SESCAM Radiodiagnostics – Software Engineering II (ISO II), group 3A1.04.

## Documentation

- [Contributing: branches, commits and pull requests](CONTRIBUTING.md)
- [Development workflow and configuration plan](docs/workflow/development-workflow.md)
- [Decisions log](DECISIONS.md)
- [Changelog](CHANGELOG.md)
- Plans: [project](docs/plans/project-plan.md) · [configuration management](docs/plans/scm-plan.md) (configurations, releases, environments, rollback) · [quality assurance](docs/plans/sqa-plan.md) (quality by component and story, gates per release)
- [Architecture proposal v0.3](docs/architecture/architecture-proposal.md) · [Quality and testing strategy](quality-testing-strategy.md)

## Build

Requirements: JDK 17 or later and Maven 3.8 or later.

```bash
mvn -B verify
```

`verify` also runs the toolchain check (enforcer), the architecture rules (`ArchitectureTest`, ArchUnit)
and the coverage check; the thresholds of the current release gate are the properties
`coverage.line.minimum` and `coverage.branch.minimum` of the parent POM.

Test reports: `*/target/surefire-reports/`. Coverage reports (JaCoCo): `*/target/site/jacoco/index.html`.
