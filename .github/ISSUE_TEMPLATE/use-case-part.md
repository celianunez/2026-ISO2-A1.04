---
name: Use case part (client or server)
about: Sub-issue of a use case, for its client part CU-nn-C or its server part CU-nn-S (D-24)
title: 'CU-nn-C – Name (client)  |  CU-nn-S – Name (server)'
labels: 'use case part'
assignees: ''

---

## Part

| Field | Value |
|---|---|
| Parent use case | #__ (CU-nn – Name) |
| Part | ☐ Client `CU-nn-C` · ☐ Server `CU-nn-S` |
| Module / package | Client: page in `radio-app` · Server: `radio-...` (`web`, `api`, `application`, `domain`) |
| Branch | `feature/<issue-id>-cu-nn-client` or `feature/<issue-id>-cu-nn-server` (CONTRIBUTING.md) |

## Scope

*Steps and flows of the parent specification that this part implements.*

- Main flow steps:
- Alternative flows / exceptions:

## Contract

- **Interface used (client) or implemented (server):** `I...Service`
- **Input / output:**
- **Errors:**

## RADIT disciplines

- [ ] Analysis
- [ ] Design
- [ ] Implementation
- [ ] Testing

## Acceptance criteria

- [ ] **Given** [context], **when** [action], **then** [expected result].
- [ ] Client: UI/controller tests against a server stub · Server: unit tests of the service and integration tests with the simulators.
- [ ] The server repeats every check made by the client (permissions, formats).
- [ ] The pull request updates the row of the use case in the *Requirements Traceability Matrix*.

## Effort estimate

| Without AI | With AI | Real |
|---|---|---|
| `X` h | `Y` h | `–` |
