# Software Configuration Management Plan – SESCAM Radiodiagnostics

| | |
|---|---|
| **Group** | ISO II – 3A1.04 (team of 6) |
| **Owner** | David Villanueva (configuration manager) |
| **Status** | Draft v0.1 – to be validated at the team meeting |
| **Date** | 06/11/2026 |
| **Structure** | IEEE 828 (Software Configuration Management Plans), as presented in the theory lectures |
| **Related** | [`development-workflow.md`](../workflow/development-workflow.md) (reasons behind the workflow), [`CONTRIBUTING.md`](../../CONTRIBUTING.md) (day-to-day rules), [`project-plan.md`](project-plan.md), [`sqa-plan.md`](sqa-plan.md), `DECISIONS.md` |

## History

| Version | Date | Author | Change |
|---|---|---|---|
| v0.1 | 06/11/2026 | D. Villanueva | First version. Organises the workflow of `development-workflow.md` v0.2 into the SCM plan sections, defines the configuration items and baselines, and resolves the open points R-02 and R-12. |

---

## 1. Introduction

### 1.1 Purpose

This plan defines how the team identifies, controls, records and audits every artefact of the project, so that at any moment we can answer: *what is the current version of each part, who changed it, why, and can we rebuild any delivered version?*

### 1.2 Scope

It applies to everything the team produces for the project: source code, build files, interfaces, tests, models, documents, the wiki and the CI configuration, from Inception to the delivery `v1.0.0` on 15/12/2026. It applies equally to work produced by people and by the AI agent.

### 1.3 Definitions

| Term | Meaning in this project |
|---|---|
| **Configuration item (CI)** | An artefact placed under configuration control, with an identifier and a version (§3.1). |
| **Baseline** | A reviewed and approved set of CIs, tagged, that can only be changed through the change control procedure (§3.2). |
| **Change request (CR)** | A GitHub Issue that asks for a change to one or more CIs. |
| **CCB** | Change Control Board: the weekly team meeting (§2.2). |
| **Release** | A tagged version on `main` (`vX.Y.Z`). |

### 1.4 References

IEEE 828 · Semantic Versioning 2.0.0 · Conventional Commits 1.0.0 · Git Flow (V. Driessen) · `development-workflow.md` · `CONTRIBUTING.md` · `DECISIONS.md`.

## 2. SCM management

### 2.1 Organisation and responsibilities

| Role | Who | Responsibilities |
|---|---|---|
| **Configuration manager** | David | Owns this plan; defines CIs and naming; prepares releases and baselines; runs the configuration audits; keeps the version matrix. |
| **Repository administrator** | Repository owner + a second administrator (to be named, risk R-04 of the project plan) | Branch protection, merge settings, labels, milestones, collaborators. |
| **CCB chair** | Coordinator (Carlos) | Puts change requests on the meeting agenda and records the decision in the minutes. |
| **Author** | Any member | Works on a feature branch, opens the PR, links the issue. |
| **Reviewer** | Any member except the author | Reviews the PR against the checklist; merges it. |
| **Quality lead** | George | Defines the quality gates the CI enforces ([`sqa-plan.md`](sqa-plan.md)). |

### 2.2 Change Control Board

The CCB is the **weekly team meeting**. A dedicated board would be overhead for six people. It decides:

- changes to a **baseline** (§3.1.3);
- changes to an **interface** (`api` package) or to the **module list or dependencies**;
- new **dependencies or tools**;
- changes to this plan.

Small changes inside the current iteration (a fix, a test, an internal refactor) do **not** go to the CCB: the PR review is enough. Decisions are written in the minutes and, if architectural, in `DECISIONS.md`.

### 2.3 Policies

1. Everything is versioned in the GitHub repository `celianunez/2026-ISO2-A1.04` or its wiki. Nothing is shared only by chat or email.
2. Nobody commits directly to `main` or `develop`; every change goes through a reviewed PR.
3. Every change is linked to an issue; every decision to a meeting.
4. Only fictitious data; no secrets, passwords or tokens in the repository.
5. Documentation is in English.

## 3. SCM activities

### 3.1 Configuration identification

#### 3.1.1 Configuration items

| CI | Type | Location | Identifier / version |
|---|---|---|---|
| Parent POM | Build | `pom.xml` | `es.uclm.sescam:sescam-parent:<version>` |
| Component modules (17) | Code | `radio-*/` (D-13) | Maven artefact `radio-<name>:<version>` |
| Application module | Code | `radio-app/` | `radio-app:<version>` |
| Module interfaces | Contract | `radio-*/src/main/java/.../api/` | Version of the module (§3.5) |
| Tests | Code | `radio-*/src/test/` | Version of the module |
| CI pipeline | Build | `.github/workflows/ci.yml` | Git history |
| Issue and PR templates | Process | `.github/` | Git history |
| Requirements | Document | `historias_de_usuario_SESCAM_v2.xlsx`, wiki 01–02 | `v2`, wiki revision |
| Architecture and decisions | Document | `architecture-proposal.md`, `DECISIONS.md` | Header version (v0.x) |
| Plans | Document | `docs/plans/*.md`, `docs/workflow/*.md` | Header version |
| UML models | Model | Visual Paradigm `.vpp` | File version in Git |
| Agent rules | Process | `AGENTS.md`, `CONTEXT.md`, `PLAN.md` | Git history |
| Wiki | Document | `2026-ISO2-A1.04.wiki` | Wiki revision (commit) |

**Not configuration items:** `target/`, IDE files, local settings (all in `.gitignore`), generated reports (published as CI artefacts instead).

#### 3.1.2 Naming

| Element | Rule | Example |
|---|---|---|
| Module | `radio-<component>` in singular (D-13) | `radio-scheduling` |
| Java package | `es.uclm.sescam.<component>.{api,domain,application,infrastructure}` | `es.uclm.sescam.scheduling.api` |
| Branch | See `CONTRIBUTING.md` §1 | `feature/42-assign-appointment` |
| Commit | Conventional Commits, English, issue in the footer | `feat(scheduling): assign appointment (#42)` |
| Tag | `v<MAJOR>.<MINOR>.<PATCH>` | `v0.4.0` |
| Document | Header with version, date, author and history table | `v0.2 · 05/11/2026` |

#### 3.1.3 Baselines

| Baseline | When | Contents | Tag |
|---|---|---|---|
| **Functional baseline** | End of Inception (It 0) | Requirements v2, use cases, project plan, this plan, SQA plan, skeleton and CI | `v0.1.0` |
| **Architecture baseline** | End of Elaboration (It 3) | Frozen `api` contracts of all modules, decisions D-01…D-13 accepted, block 1–2 code and tests | `v0.4.0` |
| **Beta baseline** | End of Construction (It 6) | All 26 stories implemented and tested | `v0.7.0` |
| **Product baseline** | Transition (It 7) | Integrated build, documentation and wiki complete | `v1.0.0` |

After a baseline is tagged, its CIs change only through change control (§3.2).

### 3.2 Configuration control

#### 3.2.1 Change request lifecycle

```text
Issue (CR) ──> Evaluation ──> Decision ──> Branch ──> PR ──> Review + CI ──> Merge ──> Closed
             (impact, effort)  (CCB if §2.2,                     │
                                else owner)                       └─> rejected: changes requested
```

1. **Request:** an issue with the `Tarea` template: description, affected CIs, acceptance criteria.
2. **Evaluation:** the owner estimates impact (modules, interfaces, tests, documents) and effort.
3. **Decision:** the CCB approves, rejects or postpones it if it falls under §2.2; otherwise the iteration owner decides. The decision is written in the issue.
4. **Implementation:** `feature/<issue>-<name>` from `develop`.
5. **Verification:** PR with `Closes #<issue>`; CI green; review by another member using the checklist.
6. **Merge and record:** squash merge into `develop`; the issue closes; the board moves to Done; `CHANGELOG.md` is updated.

#### 3.2.2 Classification of changes

| Class | Examples | Approval | Version effect |
|---|---|---|---|
| **Major** | Breaking change in an `api` interface; module added or removed; new dependency | CCB + `DECISIONS.md` | `MAJOR` (or `MINOR` while `0.y.z`) |
| **Minor** | New use case or functionality, backwards compatible | Iteration owner | `MINOR` |
| **Patch** | Bug fix, test, internal refactor, documentation | Reviewer | `PATCH` |

#### 3.2.3 Branches and integration

Git Flow, as in [`development-workflow.md` §3.1](../workflow/development-workflow.md):

| Branch | From | Into | Merge |
|---|---|---|---|
| `main` | – | – | Only release and hotfix |
| `develop` | `main` | `release/*` | – |
| `feature/<issue>-<name>` | `develop` | `develop` | Squash |
| `release/<x.y.z>` | `develop` | `main` + `develop` | Merge commit |
| `hotfix/<x.y.z>` | `main` | `main` + `develop` | Merge commit |

`main` and `develop` are protected: PR required, 1 approval, CI `build` check required, no direct pushes, branches deleted after merge.

**Resolution of R-02 (history before this plan):** the PRs `develop → main` (#6) and `main → develop` (#7) that skipped a release branch are accepted as Iteration 0 history. From the functional baseline onwards, only `release/*` and `hotfix/*` enter `main`.

### 3.3 Configuration status accounting

| Record | Where | Updated by | When |
|---|---|---|---|
| State of each change | Kanban board (Backlog → Ready → In progress → In review → Done) | Author | Every change |
| What changed in each version | `CHANGELOG.md` (Keep a Changelog format) | Author of the PR | Every PR |
| Version of each module and baseline | Version matrix in wiki page 09 | Configuration manager | Every release |
| Planned versions | GitHub milestones (`v0.2.0` … `v1.0.0`) | Configuration manager | Planning meeting |
| Release notes | GitHub Releases | Configuration manager | Every release |
| Decisions | `DECISIONS.md` | Decision owner | Every accepted decision |
| Classification of issues | Labels (`requirement`, `use-case`, `bug`, `docs`, `ci`, `iteration-N`) | Author | Issue creation |

With these records, the status report of each weekly meeting is produced from GitHub (board, milestones, Insights) without extra work.

### 3.4 Configuration audits and reviews

At every release (§4), before merging `release/*` into `main`, the configuration manager and one more member run:

**Functional configuration audit (FCA)** – does the release do what it should?
- ☐ Every use case planned for the iteration has its issue closed and its acceptance tests passing.
- ☐ CI is green on the release branch.
- ☐ Open defects are listed in the release notes.

**Physical configuration audit (PCA)** – is the release what we say it is?
- ☐ The version in the POMs matches the tag and the milestone.
- ☐ `CHANGELOG.md` and the version matrix list the changes and versions of this release.
- ☐ A clean clone of the tag builds with `mvn -B verify`.
- ☐ No secrets, real data or generated files in the tag.

The result is recorded in the release PR. A failed check blocks the release until it is fixed.

### 3.5 Interface control

The `api` packages are the contracts between modules (D-03). Rules:

1. An interface is designed and reviewed **before** it is implemented.
2. After the architecture baseline (`v0.4.0`), a breaking change to an `api` is a *major* change (§3.2.2): it needs the CCB, an entry in `DECISIONS.md`, and updating every consumer in the same PR.
3. Contract tests in the consumer modules fail if an interface changes unexpectedly ([`sqa-plan.md`](sqa-plan.md)).
4. External interfaces (SSO, HCE/admission, PACS) are controlled the same way through their adapter modules (D-08).

### 3.6 Supplier and tool control

| Element | Control |
|---|---|
| Maven dependencies and plugins | Versions pinned in the parent POM (`dependencyManagement`, `pluginManagement`). A new dependency is a *major* change (CCB + `DECISIONS.md`). |
| JDK | Temurin 17 in CI; Java 17 as compilation target (D-04). |
| AI agent output | Treated as code from any author: on a feature branch, through a PR, reviewed by a human. The PR states whether an agent was used and for what. Agent rules in `AGENTS.md`, versioned. |
| Visual Paradigm models | Committed to Git; one owner per `.vpp` per iteration to avoid binary merge conflicts. |

## 4. Versioning and releases

### 4.1 Versioning scheme

- **SemVer 2.0.0**: `MAJOR.MINOR.PATCH`. While the product is in `0.y.z`, the API is not considered stable; `1.0.0` is the delivered product.
- **Resolution of R-12: one version for the whole repository until `1.0.0`.** The parent POM and all modules share the same version, set once in the parent.
  - *Why:* with 18 modules and a team of six, one version and one changelog per module would multiply the release work by 18, while all modules are released together anyway (a modular monolith is one deployable, D-01). A shared version also makes the version matrix trivial to audit (§3.4).
  - *What we keep from per-module versioning:* the changelog groups entries by module, and the version matrix records in which version each module was introduced or changed. After `1.0.0` the team may move to independent versions if modules start to evolve at different speeds.

### 4.2 Planned versions

| Version | Iteration | Phase | Baseline |
|---|---|---|---|
| `0.1.0` | 0 | Inception | Functional |
| `0.2.0` | 1 | Elaboration | – |
| `0.3.0` | 2 | Elaboration | – |
| `0.4.0` | 3 | Elaboration | **Architecture** |
| `0.5.0` | 4 | Construction | – |
| `0.6.0` | 5 | Construction | – |
| `0.7.0` | 6 | Construction | **Beta** |
| `1.0.0` | 7 | Transition | **Product** |

Each version is a GitHub milestone. A fix to a released version produces a `PATCH` (`0.4.1`) through a hotfix branch.

### 4.3 Release procedure

1. Create `release/x.y.z` from `develop`; features are frozen.
2. Set the version in the parent POM; update `CHANGELOG.md` and the version matrix.
3. Run the configuration audits (§3.4).
4. PR into `main` (merge commit); tag `vX.Y.Z` on `main`; PR back into `develop`.
5. Publish the GitHub Release with the release notes; close the milestone.

### 4.4 Hotfix procedure

1. `hotfix/x.y.z` from the tag on `main`.
2. Fix with a test that reproduces the defect.
3. PRs into `main` (tag the new patch) and into `develop`.

## 5. SCM schedule

| Week | Iteration | SCM activity |
|---|---|---|
| W44 | 0 | Repository setup, protection, CI, configuration test, release rehearsal → **functional baseline `v0.1.0`** |
| W45 | 1 | Labels and milestones in use; first module releases `v0.2.0` |
| W46 | 2 | Release `v0.3.0` |
| W47 | 3 | FCA/PCA → **architecture baseline `v0.4.0`**; interfaces frozen |
| W48 | 4 | Release `v0.5.0` |
| W49 | 5 | Release `v0.6.0` |
| W50 | 6 | FCA/PCA → **beta baseline `v0.7.0`** |
| W51 | 7 | Final audit → **product baseline `v1.0.0`** (15/12/2026) |

## 6. SCM resources

| Resource | Use |
|---|---|
| Git + GitHub repository | Version control, branches, tags, PRs, protection rules |
| GitHub Issues + Projects | Change requests and status (Kanban board) |
| GitHub Actions | CI: `mvn -B verify`, test and coverage reports |
| GitHub Wiki | Project documentation, minutes, version matrix |
| Maven (parent POM) | Build, version and dependency management |
| JaCoCo, Surefire | Coverage and test reports attached to each build |

Effort: about 1–2 h per week for the configuration manager (releases, audits, matrix), included in the enabling work of the project plan.

## 7. Plan maintenance

This plan is reviewed by the configuration manager at the end of each phase, and whenever the CCB changes a rule. Changes follow the same procedure as any CI: issue, branch, PR and review, with a new row in the history table. The version number of this document follows the document rule in §3.1.2.
