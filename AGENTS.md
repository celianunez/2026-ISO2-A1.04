# AGENTS.md — rules for AI agents

Every AI agent (Copilot, Claude, ChatGPT…) used in this repository follows these rules (decision D-28).
The project facts are in [`CONTEXT.md`](CONTEXT.md). The human who runs the agent is accountable for every line
it produces (assignment: *man in the loop*).

## 1. Before starting

- Read [`CONTEXT.md`](CONTEXT.md), this file, the issue of the task and its parent issue, and the `api` interfaces involved.
- Confirm the scope with the human: **one use case part** (`CU-nn-C` or `CU-nn-S`) or one foundation task, and the module(s) it may change.

## 2. Scope: what an agent may change

- Only the module(s) of the task, in the packages of `CONTEXT.md` §3.
- Client parts only in `radio-app/src/main/resources/static/cu-nn/`, using the shared `js/api.js`.
- Tests of the same module(s); `CHANGELOG.md` (section *Unreleased*); the wiki page of the iteration when asked.

## 3. What an agent must never do

- Change another member's module, the parent `pom.xml`, `ArchitectureTest`, or the `api` package of another module.
- Change a merged `api` contract without a decision in `DECISIONS.md` approved by the architect (D-26).
- Add a library, a module dependency or a module.
- Implement other use cases, other iterations, or "everything at once": modelling and code follow the iteration plan.
- Weaken or skip a test, a quality gate, an ArchUnit rule or a coverage threshold to make the build pass.
- Put real personal or clinical data, credentials or tokens anywhere (code, tests, fixtures, prompts, commits).
- Push to `main` or `develop`, merge a pull request, or add itself as co-author of a commit.

## 4. How to work

1. Propose the list of classes and method signatures first; wait for the human's OK.
2. The **human designs the test cases** (inputs and expected results from the use case flows); the agent may write their code.
3. Implement in small steps: domain → application → infrastructure → web (`@PreAuthorize`) → audit calls → client.
4. Run `mvn -B verify -pl <module> -am` while working and `mvn -B verify` before the pull request; show the result.
5. Explain every non-obvious decision so the human can defend it at the review.

## 5. Commits and pull requests

- Branch `feature/<issue>-<short-name>` from `develop`; Conventional Commits in English, `Refs #<issue>` in the footer.
- The pull request follows the template, closes its issue (`Closes #<issue>`) and states honestly in *Use of AI* which tool was used and for what.
- The human records in the issue the real hours with AI next to the estimate without AI.
- The author never merges their own pull request; the owner of the module reviews it.
