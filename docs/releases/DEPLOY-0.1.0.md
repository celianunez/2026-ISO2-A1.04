# DEPLOY-0.1.0 — CFG-00 · functional baseline

| | |
|---|---|
| **Version** | `0.1.0` |
| **Configuration** | CFG-00 (`scm-plan.md` §3.1.4) |
| **Tags** | `v0.1.0` and `cfg-iter00` on the merge commit of `release/0.1.0` into `main` |
| **Label** | Internal · functional baseline (not a pre-release) |
| **Quality gate** | R0: build green, 0 failing tests, 0 ArchUnit violations, enforcer passes, coverage report only (`coverage.line.minimum` = `coverage.branch.minimum` = `0.00`) — `sqa-plan.md` §5.2.1 |
| **Release manager** | David Villanueva (configuration manager) |

## Contents of the configuration

| Item | Version | Notes |
|---|---|---|
| `sescam-parent` (parent POM) | 0.1.0 | Java 17, JUnit 5.10.2, JaCoCo 0.8.12, enforcer 3.5.0, ArchUnit 1.3.0 |
| `module-a` | 0.1.0 | Configuration test: `api.Greeter` + `internal.DefaultGreeter` |
| `module-b` | 0.1.0 | Configuration test: `Welcome` uses only `module-a` `api`; `ArchitectureTest` |
| CI | `.github/workflows/ci.yml` | `mvn -B verify` on every PR, Surefire + JaCoCo reports |
| Plans and decisions | `docs/plans/*`, `docs/workflow/*`, `DECISIONS.md` v0.3 | Functional baseline documents |

There is no `radio-app` in this configuration (it is created in CFG-01, D-19), so there is no
application to start, no Spring profile and no database copy.

## Dependencies (`mvn dependency:list`)

`module-a`:

```
org.junit.jupiter:junit-jupiter:jar:5.10.2:test
org.junit.jupiter:junit-jupiter-api:jar:5.10.2:test
org.junit.jupiter:junit-jupiter-engine:jar:5.10.2:test
org.junit.jupiter:junit-jupiter-params:jar:5.10.2:test
org.junit.platform:junit-platform-commons:jar:1.10.2:test
org.junit.platform:junit-platform-engine:jar:1.10.2:test
org.opentest4j:opentest4j:jar:1.3.0:test
org.apiguardian:apiguardian-api:jar:1.1.2:test
```

`module-b`:

```
es.uclm.sescam:module-a:jar:0.1.0:compile
com.tngtech.archunit:archunit:jar:1.3.0:test
com.tngtech.archunit:archunit-junit5:jar:1.3.0:test
com.tngtech.archunit:archunit-junit5-api:jar:1.3.0:test
com.tngtech.archunit:archunit-junit5-engine:jar:1.3.0:test
com.tngtech.archunit:archunit-junit5-engine-api:jar:1.3.0:test
org.slf4j:slf4j-api:jar:2.0.12:test
org.junit.jupiter:junit-jupiter:jar:5.10.2:test
org.junit.jupiter:junit-jupiter-api:jar:5.10.2:test
org.junit.jupiter:junit-jupiter-engine:jar:5.10.2:test
org.junit.jupiter:junit-jupiter-params:jar:5.10.2:test
org.junit.platform:junit-platform-commons:jar:1.10.2:test
org.junit.platform:junit-platform-engine:jar:1.10.2:test
org.opentest4j:opentest4j:jar:1.3.0:test
org.apiguardian:apiguardian-api:jar:1.1.2:test
```

## How to rebuild this configuration

```bash
git clone https://github.com/celianunez/2026-ISO2-A1.04.git && cd 2026-ISO2-A1.04
git checkout v0.1.0
mvn -B clean verify
```

Verified on 10/11/2026 with Apache Maven 3.8.7 and OpenJDK 21 (target Java 17): BUILD SUCCESS,
6 unit tests + 2 architecture rules, 0 failures, all coverage checks met.

## Rollback

This is the first configuration: there is nothing to roll back to. From CFG-01 on, rolling back
to this configuration means `git checkout cfg-iter00` + `mvn -B clean verify` (`scm-plan.md` §4.5).
