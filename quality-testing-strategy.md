# Quality Characteristics and Testing Strategy

## 1. Purpose and scope

This document proposes which quality characteristics we will prioritise, why, how we plan to test for them, and which criteria we will use for the first quality reviews. It is a strategy to agree on as a team. No tests are implemented at this stage.

## 2. Selected quality characteristics

We use the product quality model of ISO/IEC 25010

| Characteristic                                           | Priority | Justification for this project                                                                                                                                                                                                                                                                                                  |
|----------------------------------------------------------|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Functional suitability (correctness, completeness)       | High     | The system is only useful if it does what the requirements say. Wrong or missing features are the most visible failure.                                                                                                                                                                                                         |
| Security (confidentiality, authenticity, accountability) | High     | We store user accounts and personal data, so access control and input handling must be sound.                                                                                                                                                                                                                                   |
| Maintainability (modularity, testability, modifiability) | High     | We are building the system step-by-step, tackling one user scenario at a time. The front-end and back-end are kept completely separate and only communicate using strict, agreed-upon rules. Because an AI is writing the code based on these rules, keeping the code highly organized and easy to test is absolutely critical. |
| Reliability (fault tolerance, recoverability)            | Medium   | The server depends on external systems (EHR, Admissions, RIS/PACS) and on a timer. If those outside systems crash or take too long to answer, our app shouldn't just break—it needs to handle the failure smoothly and keep running as best as it can.                                                                          |
| Compatibility (interoperability)                         | Medium   | The front-end and back-end talk to each other using strict, agreed-upon rules. Right now, we are using fake versions of outside systems to test things out. These communication rules need to stay exactly the same so we can easily swap out the fake systems for the real ones later without breaking the app.                |

**Not prioritised for now:** Usability; Performance efficiency; Portability

## 3. Proposed testing strategy

**Principles**

- Tests are derived from use cases and their acceptance criteria. This is the Testing discipline of each iteration.

- Humans design the test cases. The AI can write the basic starting code for the tests, but we must check its work just like any other code.

- Tests run automatically with mvn -B verify locally and in GitHub Actions on every pull request.

- No new testing dependency is added unless it is justified in DECISIONS.md.

**How each characteristic is verified**

| Characteristic         | Verification approach                                                                                                                                                                                                                                                                                    |
|------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Functional suitability | At least one acceptance test per use-case flow (main, alternative, exception). Test names reference the use case and requirement.                                                                                                                                                                        |
| Security               | We test permissions to make sure users can only do what their role allows. The fake login system blocks anyone with a missing or invalid login. Finally, we review our code to make sure no real passwords, secrets, or private data ever get saved in the project files.                                |
| Maintainability        | Different parts of the system only talk to each other through shared connection rules, which we double-check during code reviews. We also track our test coverage over time with each new version. Finally, we keep our code updates small and make sure each one connects directly to an assigned task. |
| Reliability            | We write tests where fake outside systems intentionally crash or take too long to respond, checking that our server handles the problem properly instead of breaking. We also use an adjustable clock to test timers quickly instead of waiting around in real time.                                     |
| Compatibility          | We write tests for the connection between the client and server so that if anyone changes how methods look or how data is formatted, a test will instantly fail.                                                                                                                                         |

**Per-iteration flow:** (1) design test cases from the acceptance criteria in the use case issue, (2) implement them with the feature, (3) run mvn -B verify, (4) attach the Surefire and JaCoCo results to the pull request, (5) fix defects inside the feature branch with fix(CUxx): ... (#issue).

## 4. Initial quality review criteria

Proposed gates for every pull request into develop:

1.  CI is green: mvn -B verify compiles and all tests pass. A red build is never merged.

2.  Each use case in the iteration has acceptance tests for its main flow and for each documented alternative flow and exception.

3.  Coverage: report-only in Iteration 0 to get a baseline. Then a proposed minimum of 70% line coverage on domain and service code, to be confirmed by the team.

4.  The reviewer (never the author) checks that the pull request matches its issue, interfaces are unchanged without a recorded decision, role checks exist where needed, no real data or secrets are present, and no unjustified dependency was added.

5.  The pull request description states whether an AI agent was used and for what. AI-generated code gets the same review as human code.

6.  Documentation, CHANGELOG and DECISIONS.md are updated when the change affects them.

This plan is reviewed at the end of each iteration and adjusted if the results show the criteria are too loose or too strict.

## 5. Testing and quality reviews across the project phases

| Phase (milestone)                             | Testing focus                                                                                                                                                                                                                                                                                                                                                         | Quality review focus                                                                                                                                                                                                                           |
|-----------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Inception, It0 (project plan)                 | No executable tests yet. Agree this strategy, set up CI with mvn -B verify, and check that a first trivial test runs green in the pipeline.                                                                                                                                                                                                                           | Review the choice of characteristics, the pull request checklist and the Definition of Done. Record the decisions in DECISIONS.md.                                                                                                             |
| Elaboration (architectural baseline)          | The first few scenarios set up the core data models and connection parts, so tests for our connection rules matter most. This includes testing how the client and server talk, how fake outside systems behave, and how well we keep modules separated.                                                                                                               | We check the overall design of our connection rules and how user security works, including roles and the fake login system. Then, we use our initial test numbers to either keep our required coverage goal or adjust it to be more realistic. |
| Construction (initial operational capability) | In each work cycle, we tackle one remaining scenario from start to finish. We write tests to make sure normal steps, alternate paths, and errors all work properly, while also checking that user permissions and outside system failures are handled correctly. Finally, we run all our tests on every code update to make sure nothing we built before gets broken. | We review every code update using the checklist from section 4. At the end of each work cycle, we check whether our test coverage is improving, look over any remaining bugs, and decide if our goals from section 2 are still on track.       |
| Transition (final delivery)                   | We run full tests on the whole system to make sure everything still works well together, from the client and server all the way to the fake outside systems. During this stage, we do not add any new features.                                                                                                                                                       | Final quality review against the criteria in section 4. Record the results and lessons learned for the self-assessment and the wiki.                                                                                                           |

In every phase, defects found during a feature are fixed inside that feature branch, and any change to this strategy is recorded in DECISIONS.md.
