# Contributing – SESCAM Radiodiagnostics (ISO II – 3A1.04)

Rules for branches, commits and pull requests. The reasons behind them are in [`docs/workflow/development-workflow.md`](docs/workflow/development-workflow.md).

**Summary:** Issue → `feature/<issue-id>-<name>` from `develop` → Conventional Commits → PR into `develop` with `Closes #<issue-id>` → green CI + 1 review → someone else squash-merges.

## 1. Branches (Git Flow)

| Branch | Created from | Merged into | Merge method | Who |
|---|---|---|---|---|
| `main` | – | – | – | Only receives `release/*` and `hotfix/*` |
| `develop` | `main` | `release/*` | – | Integration branch, default branch |
| `feature/<issue-id>-<short-name>` | `develop` | `develop` | Squash merge | Issue owner |
| `release/<x.y.z>` | `develop` | `main`, then `develop` | Merge commit | Release manager |
| `hotfix/<x.y.z>` | `main` | `main` and `develop` | Merge commit | Any member, with an issue |

**Rules**

- Never push directly to `main` or `develop`. Everything goes through a PR.
- Every `feature/*` branch starts from an Issue. Its number goes in the name.
- Names are lowercase, in English, words separated by hyphens. No spaces, accents or uppercase.
- Keep feature branches short-lived (days, not weeks). Update them from `develop` before opening the PR.
- Delete the branch after merge.

**Examples**

```text
feature/12-appointment-booking
feature/15-ci-workflow
release/0.1.0
hotfix/0.1.1
```

```bash
git switch develop
git pull
git switch -c feature/12-appointment-booking
```

## 2. Commits (Conventional Commits)

```text
<type>(<scope>): <summary>

<optional body: what and why>

Refs #<issue-id>
```

- **Language:** English, imperative mood ("add", not "added" or "adds").
- **Summary:** lowercase, no final period, 72 characters maximum.
- **Scope:** the module (`request`, `scheduling`…), the use case (`CU05`) or the area (`workflow`, `ci`, `docs`).
- **Footer:** `Refs #N` for a related issue. `Closes #N` only goes in the PR description.
- One logical change per commit. Do not mix formatting and behaviour changes.
- Do not use "Add files via upload". When uploading files through the GitHub web, write a proper commit message.

| Type | Use for |
|---|---|
| `feat` | A new feature or behaviour |
| `fix` | A bug fix |
| `docs` | Documentation only |
| `test` | Adding or changing tests |
| `refactor` | Code change with no behaviour change |
| `build` | Maven, dependencies, build configuration |
| `ci` | GitHub Actions workflows |
| `chore` | Other maintenance (templates, `.gitignore`…) |

**Examples**

```text
feat(scheduling): add booking interface
fix(CU05): reject overlapping appointments (#23)
docs(workflow): add reviewed development workflow v0.2
test(request): cover priority change exception flow
build: add jacoco report to parent pom
ci: run mvn verify on pull requests
```

A breaking change to an interface adds `!` after the type (`feat(request)!: ...`) and a `BREAKING CHANGE:` footer. It needs a recorded decision in `DECISIONS.md`.

## 3. Pull requests

**Opening**

- One PR covers one Issue. Keep it small enough to review in about 30 minutes.
- Target branch: `develop` (`main` only for `release/*` and `hotfix/*`).
- Title in Conventional Commits format. With a squash merge it becomes the commit on `develop`.
- The description follows the template. It includes `Closes #<issue-id>` and says whether an AI agent was used and for what.
- Run `mvn -B verify` locally before opening the PR (once the Maven build exists).
- Open it as a **draft** while it is not ready for review.

**Reviewing**

- At least **1 approval** from a member who is not the author.
- The reviewer uses the checklist in the template (from section 4 of `quality-testing-strategy.md`). AI-generated code is reviewed with the same rigour as human code.
- All review comments are resolved before merge.

**Merging**

- CI must be green. A red build is never merged.
- **The author never merges their own PR.** The reviewer or another member merges.
- `feature/*` → `develop`: **Squash and merge**. `release/*` and `hotfix/*`: **Create a merge commit**.
- Delete the branch after merge.

## 4. Definition of Done

A task is done when:

- [ ] The code compiles and all tests pass in CI.
- [ ] At least one teammate approved the PR.
- [ ] The Issue is linked and closed, and the board is updated.
- [ ] Documentation, `CHANGELOG.md` and `DECISIONS.md` are updated when relevant.
- [ ] There are no unresolved review comments.

## 5. Issues and labels

- Use the `Tarea` issue template. Every issue has an owner, labels, an iteration and a milestone.
- Proposed labels (to be created by the admin): `requirement`, `use-case`, `task`, `bug`, `docs`, `ci`, `iteration-0`, `iteration-1`…

## 6. Versioning and releases

- [Semantic Versioning 2.0.0](https://semver.org/): `MAJOR.MINOR.PATCH`. `0.y.z` until the final delivery (`1.0.0`).
- Each planned version is a GitHub milestone.
- Release steps:
  1. `release/x.y.z` from `develop`, with only stabilisation fixes.
  2. PR into `main` (merge commit).
  3. Tag `vX.Y.Z` on `main`.
  4. Merge back into `develop`.
- `CHANGELOG.md` is updated in every release.
