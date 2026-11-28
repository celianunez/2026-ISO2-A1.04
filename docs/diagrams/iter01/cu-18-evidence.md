# CU-18 acceptance evidence

FR-18 covers the administrator’s user search and role editor. The client uses the shared authenticated API client. Role changes are audited and take effect when the target user next signs in.

| Part | Implementation and review | Evidence |
| --- | --- | --- |
| Client [#27](https://github.com/celianunez/2026-ISO2-A1.04/issues/27) | [#50](https://github.com/celianunez/2026-ISO2-A1.04/pull/50), [#55](https://github.com/celianunez/2026-ISO2-A1.04/pull/55) | Six Node.js cases pass at develop commit [527614d](https://github.com/celianunez/2026-ISO2-A1.04/commit/527614d0286284f03aa4568e0c279f7146a5f9e1). [#56](https://github.com/celianunez/2026-ISO2-A1.04/pull/56) adds the 404 and 403 client regressions. [#55](https://github.com/celianunez/2026-ISO2-A1.04/pull/55) records browser checks of assignment, withdrawal and role visibility with the app and simulated SSO. |
| Server [#28](https://github.com/celianunez/2026-ISO2-A1.04/issues/28) | [#49](https://github.com/celianunez/2026-ISO2-A1.04/pull/49), [#51](https://github.com/celianunez/2026-ISO2-A1.04/pull/51) | `UserRoleControllerTest` checks server authorization and error responses. `AuthorizationServiceContractTest` checks role persistence and audit entries. `LoginFlowIT` in [#52](https://github.com/celianunez/2026-ISO2-A1.04/pull/52) checks the next-session rule with simulated SSO. |
| Design [#26](https://github.com/celianunez/2026-ISO2-A1.04/issues/26) | [#57](https://github.com/celianunez/2026-ISO2-A1.04/pull/57) | [PlantUML source for the sequence and class diagrams](cu-18.puml). Both render with PlantUML 1.2025.4. |

Run the client cases from the repository root with `node --test radio-app/src/test/js/cu-18.test.mjs`. They use a stubbed server. The integration and browser evidence is linked separately above.

## Wiki updates

Replace the FR-18 row in the [requirements traceability matrix](https://github.com/celianunez/2026-ISO2-A1.04/wiki/Requirements-Traceability-Matrix#iteration-1) with the following Markdown. Keep CFG-01 marked as planned until it is tagged.

```markdown
| FR-18 | Sys·2 | CU-18 Manage roles and permissions | 1 | `radio-auth` | [#26](https://github.com/celianunez/2026-ISO2-A1.04/issues/26) | CU-18-C: [#27](https://github.com/celianunez/2026-ISO2-A1.04/issues/27) → [#50](https://github.com/celianunez/2026-ISO2-A1.04/pull/50), [#55](https://github.com/celianunez/2026-ISO2-A1.04/pull/55), [#56](https://github.com/celianunez/2026-ISO2-A1.04/pull/56) | CU-18-S: [#28](https://github.com/celianunez/2026-ISO2-A1.04/issues/28) → [#49](https://github.com/celianunez/2026-ISO2-A1.04/pull/49), [#51](https://github.com/celianunez/2026-ISO2-A1.04/pull/51) | `cu-18/index.html`, `cu-18/cu-18.js`; `cu-18.test.mjs` (6 passed), `StaticClientIT` | `AuthorizationService`, `UserRoleController`; `AuthorizationServiceContractTest`, `UserRoleControllerTest`, `LoginFlowIT` | CFG-01 `0.2.0` (planned) | 🔄 |
```

Add the following under [Iteration 01 → Design](https://github.com/celianunez/2026-ISO2-A1.04/wiki/Iteration-01#design).

```markdown
**CU-18 · Manage roles and permissions (Carlos Ruiz Velasco)**

- [Sequence and design class diagrams (PlantUML)](https://github.com/celianunez/2026-ISO2-A1.04/blob/develop/docs/diagrams/iter01/cu-18.puml), reviewed in [#57](https://github.com/celianunez/2026-ISO2-A1.04/pull/57).
- [Client and server acceptance evidence](https://github.com/celianunez/2026-ISO2-A1.04/blob/develop/docs/diagrams/iter01/cu-18-evidence.md).
```
