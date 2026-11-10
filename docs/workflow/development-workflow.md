# Development Workflow and Configuration Plan – SESCAM Radiodiagnostics

| | |
|---|---|
| **Group** | ISO II – 3A1.04 (team of 6) |
| **Owner** | David Villanueva |
| **Status** | Draft v0.3 – reviewed, to be confirmed at the team meeting |
| **Date** | 10/11/2026 |
| **Source** | `Development Workflow and Configuration Plan – SESCAM Radiodiagnostics.pdf` (v0.1) |
| **Related** | [`CONTRIBUTING.md`](../../CONTRIBUTING.md) (operational rules), [`quality-testing-strategy.md`](../../quality-testing-strategy.md), [`DECISIONS.md`](../../DECISIONS.md), [`scm-plan.md`](../plans/scm-plan.md), [`sqa-plan.md`](../plans/sqa-plan.md) |

## History

| Version | Date | Author | Change |
|---|---|---|---|
| v0.1 | 27/10/2026 | D. Villanueva | First draft (PDF). |
| v0.2 | 05/11/2026 | D. Villanueva | Converted to Markdown. Review against the current repository state, the architecture proposal v0.3 (PR #11) and the quality strategy. Branch, commit and PR rules moved to `CONTRIBUTING.md`. Configuration test skeleton implemented (§5). |
| v0.3 | 10/11/2026 | D. Villanueva | Architecture checks added to the skeleton (enforcer, JaCoCo `check`, `ArchitectureTest`, D-14). R-06 and R-12 resolved by D-15 and D-20; open items updated. |

## 1. Purpose and scope

This document defines the development workflow of the team, the technical setup steps, and a small configuration test to validate both before implementation starts. It covers technical preparation and project planning (Block 1). It does not define the architecture or the module list: those are in the architecture proposal and in `DECISIONS.md`.

The day-to-day rules (branches, commits, pull requests, Definition of Done) are in [`CONTRIBUTING.md`](../../CONTRIBUTING.md). This document explains the reasons for them.

## 2. Guiding principles

- **Unified Process:** use-case driven, iterative and incremental, architecture-centric.
- **Human in the loop:** engineers design interfaces and architecture; the AI agent implements against those contracts and runs tests. Each team member stays accountable for their work.
- **Traceability:** every meeting decision becomes a GitHub Issue, is tracked on the Kanban board and is delivered through a Pull Request.
- **Git Flow:** no direct commits to `main` or `develop`. All integration goes through peer-reviewed Pull Requests.
- **Semantic Versioning 2.0.0**, reflected in release branches and tags.
- **GitHub as primary evidence:** wiki, commits, issues, board and Insights are what gets evaluated. No tools outside GitHub unless justified in `DECISIONS.md`.
- **No overengineering:** every tool or technology added is justified in writing.

## 3. Development workflow

### 3.1 Branching model (Git Flow)

| Branch | Purpose | Created from | Merged into | Merge method |
|---|---|---|---|---|
| `main` | Released, tagged versions only | – | – | – |
| `develop` | Integration of finished work | `main` | `release/*` | – |
| `feature/<issue-id>-<short-name>` | One task or use case | `develop` | `develop` (PR) | Squash merge |
| `release/<x.y.z>` | Stabilisation of a release | `develop` | `main` and `develop` (PRs) | Merge commit |
| `hotfix/<x.y.z>` | Urgent fix on a release | `main` | `main` and `develop` (PRs) | Merge commit |

`main` and `develop` are protected. Detailed naming rules are in `CONTRIBUTING.md` §1.

**Why two merge methods.** A squash merge turns each feature into one commit on `develop`, whose message is the PR title in Conventional Commits format, so the history reads like a changelog. Release and hotfix branches use a merge commit, so the history of `main` keeps its link with `develop` and the tags stay reachable from both branches.

### 3.2 From meeting to delivery (traceability chain)

1. The team meets and writes the minutes in the wiki, listing decisions and deliverables.
2. Each decision or deliverable becomes a GitHub Issue (template `Tarea`) with an owner, labels, an iteration and a milestone.
3. The Issue goes on the Kanban board: **Backlog → Ready → In progress → In review → Done**.
4. The owner creates `feature/<issue-id>-<short-name>` from `develop`.
5. The owner opens a PR into `develop` with `Closes #<issue-id>`.
6. A teammate reviews. CI must be green. Someone other than the author merges with a squash merge.
7. The Issue closes automatically and the board updates.

### 3.3 Pull requests and 3.4 commits

These are in `CONTRIBUTING.md` (§2 commits, §3 pull requests). The PR checklist is the one in section 4 of `quality-testing-strategy.md`, so there is only one list of review criteria.

### 3.5 Versioning and releases

- SemVer `MAJOR.MINOR.PATCH`. `0.y.z` while in Inception/Elaboration; `1.0.0` is the final delivery (Transition).
- Planned versions are GitHub milestones.
- Releases are cut from `release/<x.y.z>` and tagged on `main` as `v<x.y.z>`.
- `CHANGELOG.md` lists the changes of each version. **Open point:** one version and changelog for the whole repository, or one per Maven module (the v0.1 proposal said one per module). See §6.

### 3.6 Responsibilities: humans and agents

| Activity | Human engineers | AI agent |
|---|---|---|
| Requirements elicitation, use case prioritisation | Yes | No |
| Use case analysis and design, architecture decisions | Yes | Can suggest alternatives only |
| Interface design (Java interfaces, module boundaries) | Yes | Can audit, not decide |
| Unit and integration test design | Yes | Can scaffold from the design |
| Boilerplate and contract-compliant implementation | Reviews | Yes |
| Test execution | Reviews results | Yes |
| Code review and quality audit | Yes | Can assist |
| Planning, estimates, schedule | Yes | Can produce a forecast to compare against |

### 3.7 AI agent rules

Shared files in the repository root, versioned like code and changed only through PRs:

- `AGENTS.md`: persistent rules for the agent.
- `CONTEXT.md`: mapping of requirements to use cases and modules, domain glossary.
- `PLAN.md`: current iteration scope and task breakdown.
- `DECISIONS.md`: every assumption and decision with its reason and date.

Minimum rules for `AGENTS.md`:

1. Do not add dependencies, frameworks or tools that are not listed in `DECISIONS.md`.
2. Implement only against the interfaces provided; do not change interfaces without a human decision.
3. Respect module boundaries: modules communicate only through their `api` packages (D-03).
4. Work only inside the current iteration scope described in `PLAN.md`.
5. Never commit to `main` or `develop`; work on a feature branch and open a PR.
6. Use only fictitious data, never real patient data or credentials.

### 3.8 Definition of Done

See `CONTRIBUTING.md` §4.

## 4. Technical setup steps

### 4.1 Repository and GitHub (repository administrator)

| # | Step | Current state (05/11/2026) |
|---|---|---|
| 1 | Repository under the team account | Done: `celianunez/2026-ISO2-A1.04`, public |
| 2 | 6 members + lab instructor as collaborators | 6 members added; **instructor to be checked** |
| 3 | Enable Issues, Projects and Wiki | Done |
| 4 | Create `develop` and make it the default branch | `develop` exists; **default branch is still `main`** |
| 5 | Branch protection on `main` and `develop`: PR required, 1 approval, status checks, no direct pushes | Both branches are protected; **rules to be confirmed by the admin**; status check pending until CI exists |
| 6 | Merge settings: squash + merge commit only, delete branch after merge | **Pending**: all three methods are enabled and branches are not deleted |
| 7 | Labels (`requirement`, `use-case`, `bug`, `docs`, `ci`, `iteration-0`…) | **Pending**: only GitHub default labels |
| 8 | Kanban board with five columns | To be checked |
| 9 | Milestones for planned releases | **Pending** |
| 10 | `.gitignore` (Java, Maven, Eclipse, VS Code) and README with wiki link | `.gitignore` now covers Maven `target/` and IDE files; README links the docs (**wiki link pending**) |
| 11 | Wiki home page with index; final section "Self-Assessment and Experience" | Issue #1 open |

### 4.2 Local environment (every member)

1. Git installed, `user.name` and `user.email` configured, SSH key or credential manager for GitHub.
2. JDK 17 or later (the build targets Java 17, see D-04). Maven 3.8 or later (`mvn -v`).
3. Eclipse with Maven support, and Visual Paradigm.
4. The agreed AI agent, with the shared `AGENTS.md`.
5. Clone the repository and check out `develop`.

### 4.3 Repository skeleton

```text
repo/
  AGENTS.md
  CONTEXT.md
  PLAN.md
  DECISIONS.md
  CONTRIBUTING.md
  README.md
  quality-testing-strategy.md
  pom.xml                     (parent, packaging pom)
  radio-*/                    (one module per component, D-02 / D-13)
  docs/
  .github/
    ISSUE_TEMPLATE/tarea.md
    pull_request_template.md
    workflows/ci.yml
```

### 4.4 Continuous integration

A GitHub Actions workflow builds and tests every PR into `develop` and `main` with `mvn -B verify`, and publishes the Surefire and JaCoCo reports (`quality-testing-strategy.md` §3). It becomes a required status check once it has run once. Implemented in `.github/workflows/ci.yml` (§5.1). `mvn -B verify` also runs the enforcer, the architecture rules and the coverage check, whose thresholds grow per release (`sqa-plan.md` §5.2.1, D-17); *the required check is enabled by the admin.*

## 5. Small configuration test

The test in v0.1 §5 stays valid: a parent POM, `module-a` exposing an interface in an `api` package, `module-b` using only that interface, one JUnit 5 test and the CI workflow. Then the full chain Issue → branch → PR → CI → review → merge → closed Issue, a negative test (a failing PR is blocked, a direct push is rejected) and a release rehearsal (`release/0.1.0` → tag `v0.1.0`).

### 5.1 Implemented skeleton (v0.2)

| Item | File | Content |
|---|---|---|
| Parent POM | `pom.xml` | `es.uclm.sescam:sescam-parent:0.1.0-SNAPSHOT`, packaging `pom`, Java 17 (`maven.compiler.release`), JUnit BOM 5.10.2, ArchUnit 1.3.0, pinned compiler/surefire/JaCoCo/enforcer plugin versions. In `verify`: enforcer (Java ≥ 17, Maven ≥ 3.8), JaCoCo report and JaCoCo `check` with the properties `coverage.line.minimum` / `coverage.branch.minimum` (0.00 = report only in Iteration 0, D-17). |
| Module A | `module-a/` | Interface `es.uclm.sescam.modulea.api.Greeter` and its implementation in `...modulea.internal.DefaultGreeter`. Tests: normal flow and exception flow (null/blank name). |
| Module B | `module-b/` | `es.uclm.sescam.moduleb.Welcome` receives a `Greeter` and imports only `...modulea.api`. Its test uses a stub of the interface, not the real implementation. `es.uclm.sescam.architecture.ArchitectureTest` (ArchUnit) fails if a module uses another module outside its `api` package or if there is a cycle between modules (D-14); it moves to `radio-app` in Iteration 1. |
| CI | `.github/workflows/ci.yml` | Job `build`: Temurin 17, Maven cache, `mvn -B verify`, uploads Surefire and JaCoCo reports as the `test-reports` artifact (also when the build fails). |
| Ignore rules | `.gitignore` | `target/` and Eclipse, IntelliJ and VS Code files. |

Module names are generic on purpose. They will be replaced by the `radio-*` modules when D-13 is accepted (§8).

### 5.2 Results

| Check | Result (05/11/2026, D. Villanueva, local) |
|---|---|
| `mvn -B verify` | BUILD SUCCESS, 6 tests (4 in module A, 2 in module B), 0 failures |
| JaCoCo report | Generated in `module-a/target/site/jacoco/` and `module-b/target/site/jacoco/` |
| Module boundary | `module-b` only imports `es.uclm.sescam.modulea.api.Greeter`; checked by `ArchitectureTest` |
| Architecture checks (10/11/2026) | BUILD SUCCESS with enforcer, `ArchitectureTest` (2 rules) and JaCoCo `check`. Negative tests: making `Welcome` use `modulea.internal.DefaultGreeter` fails the build with an *Architecture Violation*; `-Dcoverage.line.minimum=1.01` fails the build with *Coverage checks have not been met* |
| Negative test (local) | A broken assertion makes `mvn -B verify` fail (exit code 1) |
| CI on GitHub | *Pending: runs on the first PR into `develop`* |

### 5.3 Remaining steps (team)

These need the admin and other members (v0.1 §5.7, steps 2 to 10):

1. Admin marks the `build` check as required in the protection of `develop` and `main`.
2. A PR with a deliberately failing test is blocked; a direct push to `develop` is rejected.
3. Another member reviews and merges; the Issue closes and the board moves it to Done.
4. Release rehearsal: `release/0.1.0` → `main`, tag `v0.1.0`, merge back into `develop`.
5. Every member clones the repository and runs `mvn -B verify` successfully.

## 6. Review notes (v0.1 → v0.2)

| # | Finding | Impact | Proposed action |
|---|---|---|---|
| R-01 | The default branch is `main`, but the workflow uses `develop` as the integration branch. | New PRs target `main` by default, which skips Git Flow. | Admin changes the default branch to `develop`. |
| R-02 | Past PRs went `develop → main` (#6) and `main → develop` (#7) without a release branch. | History does not follow Git Flow; `main` has unreleased work. | Accept as Iteration 0 history. From now on, only `release/*` and `hotfix/*` go into `main`. Align with the SCM plan. |
| R-03 | Many commits are "Add files via upload" (web uploads). | No meaningful history; no link with issues. | Commit rules in `CONTRIBUTING.md` §2. When uploading through the web, edit the commit message. |
| R-04 | Existing branches (`development-workflow`, `Project-Structure`, `alejandroacena7546-patch-1`, `feature/HDU-file`) do not follow `feature/<issue-id>-<short-name>`. | Branches cannot be traced to issues. | Keep them until merged; new branches follow the convention. |
| R-05 | v0.1 lists the Java version as open, while D-04 (PR #11) proposes Java 17, Spring Boot 3.5.16 and JUnit 5. | Two documents could disagree. | Reference D-04; do not decide the stack here. |
| R-06 | v0.1 uses `es.uclm.sescam` and `module-a/b`, while D-13 uses `radio-*` names. | Naming mismatch. | **Resolved:** `groupId` `es.uclm.sescam` and packages `es.uclm.sescam.<component>.{api,domain,application,infrastructure}` (D-13, D-20); `module-a/b` stay only as the configuration test until the first `radio-*` modules (Iteration 1). |
| R-07 | v0.1 has its own PR checklist, different from section 4 of `quality-testing-strategy.md`. | Two lists of review criteria. | One checklist: the quality strategy one, in `.github/pull_request_template.md`. |
| R-08 | v0.1 does not mention JaCoCo, which the quality strategy requires in CI. | CI would not produce the coverage baseline. | Add it to the configuration test (§5). |
| R-09 | All merge methods are enabled and branches are not deleted after merge. | Inconsistent history and stale branches. | Squash for features, merge commit for release/hotfix; enable "Automatically delete head branches". |
| R-10 | The issue template mentions C# and has a placeholder `about:` text. | Confusing for a Java project. | Fixed in this version. |
| R-11 | The suggested labels do not exist. | Issues cannot be classified by type or iteration. | Admin creates the labels in `CONTRIBUTING.md` §5. |
| R-12 | v0.1 says one SemVer version and changelog per Maven module. With 18 modules this is a lot of overhead. | Release work for a team of 6. | **Resolved by D-15** (`scm-plan.md` §4.1): one version for the whole repository until `1.0.0`, plus one identified configuration per iteration (`scm-plan.md` §3.1.4). |
| R-13 | The AI agent is fixed as OpenCode in v0.1. | Members may use other tools; the issue template lists Copilot, ChatGPT, Claude. | Open point: agree on the tool, or on the rules only (`AGENTS.md`), whatever tool is used. |

## 7. Proposed decisions for `DECISIONS.md`

To be added after PR #11 is merged (to avoid conflicts), with the next free IDs:

| Decision | Reason | Status |
|---|---|---|
| **Git Flow** with `main`, `develop`, `feature/*`, `release/*`, `hotfix/*`; `develop` is the default branch. | Required by the course; separates released work from integration. | Proposed |
| **Squash merge** for `feature/*` → `develop`; **merge commit** for `release/*` and `hotfix/*`. | Readable history on `develop`; tags reachable from both branches. | Proposed |
| **Conventional Commits** in English, with the issue number in the footer. | Traceability and readable history. | Proposed |
| **SemVer 2.0.0**, tags `vX.Y.Z` on `main`, milestones per planned version. | Required by the course. | Proposed |
| **GitHub only** (Issues, Projects, Wiki, Actions); no Azure DevOps. | GitHub is the evidence that gets evaluated; Azure DevOps would duplicate it. | Proposed |
| **Documentation in English.** | Agreed in v0.1. | Proposed |

## 8. Open items

| Item | Status | Note |
|---|---|---|
| Java version / framework | See D-04 | Java 17, Spring Boot 3.5.16, JUnit 5 (to confirm) |
| `groupId` and module names | Resolved | D-13, D-20 |
| Versioning per repository or per module | Resolved | D-15: one version per repository; CFG map in `scm-plan.md` §3.1.4 |
| Architecture checks | Resolved | D-14: ArchUnit + enforcer in `mvn -B verify` |
| AI agent tool | Open | R-13 |
| Azure (cloud hosting) | Open | Optional for Transition; fictitious data only |
| Client type (web, desktop) | See P-05 | – |

## 9. Next steps

1. Team reviews this v0.2 in a meeting; minutes go to the wiki.
2. Align with the SCM plan (R-02, R-12) and with the quality strategy (R-07, R-08).
3. Admin applies §4.1 (R-01, R-09, R-11).
4. Run the configuration test (§5) and record the result in the wiki.
5. Add the decisions of §7 to `DECISIONS.md`.
