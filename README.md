# 2026-ISO2-A1.04

SESCAM Radiodiagnostics – Software Engineering II (ISO II), group 3A1.04.

## Documentation

- [Contributing: branches, commits and pull requests](CONTRIBUTING.md)
- [Development workflow and configuration plan](docs/workflow/development-workflow.md)
- [Decisions log](DECISIONS.md)

## Build

Requirements: JDK 17 or later and Maven 3.8 or later.

```bash
mvn -B verify
```

Test reports: `*/target/surefire-reports/`. Coverage reports (JaCoCo): `*/target/site/jacoco/index.html`.
