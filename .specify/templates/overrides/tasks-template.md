---

description: "dotCMS task list template — TDD-mandatory (see Constitution Principle V)"
---

# Tasks: [FEATURE NAME]

**Input**: Design documents from `/specs/[###-feature-name]/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests (TDD — MANDATORY)**: Per Constitution Principle V, **no implementation code is written
before** (1) tests are written, (2) the developer validates and approves them — or explicitly
states which test type cannot be implemented and why — and (3) the tests are confirmed to
**FAIL (Red)**. Every user-story phase below encodes these three gates as tasks. Do not delete
the gate tasks and do not reorder implementation ahead of them.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- **[GATE]**: A blocking checkpoint that requires developer action; never auto-satisfy it
- Include exact file paths in descriptions

## Path Conventions (dotCMS)

- **Backend**: `dotCMS/src/main/java/com/dotcms/...` (prefer modern) or `com/dotmarketing/...` (legacy)
- **Integration tests**: `dotcms-integration/src/test/java/...`
- **Postman**: `dotcms-postman/src/main/resources/postman/...`
- **Karate**: `dotCMS/src/curl-test/...` (or the module's karate suite)
- **Frontend + e2e**: `core-web/libs/...`, `core-web/apps/...` (Jest/Spectator, Playwright)

## Test types by layer (choose the applicable ones per story)

| Layer / change | Test type | How to run |
|---|---|---|
| Java service/util logic | Unit (JUnit) | module test goal |
| Java + DB/ES, API behavior | Integration | `./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=Class#method` |
| REST endpoint contracts | Postman | `./mvnw verify -pl :dotcms-postman -Dpostman.test.skip=false -Dpostman.collections=<name>` |
| REST/API scenarios | Karate | run the karate suite for the endpoint |
| Angular component/service | Jest/Spectator | `yarn nx test <project>` |
| User-facing flow | e2e | Playwright suite |

<!--
  ============================================================================
  The tasks below are SAMPLE TASKS. /speckit-tasks MUST replace them with real
  tasks derived from spec.md (user stories + priorities), plan.md, data-model.md,
  and contracts/ — while PRESERVING the TDD gate structure (Tests → Approval GATE
  → Red GATE → Implementation) in every user-story phase.
  ============================================================================
-->

## Phase 1: Setup (Shared Infrastructure)

- [ ] T001 Create/verify project structure per implementation plan
- [ ] T002 [P] Ensure test scaffolding exists for the layers this feature will touch
        (integration/postman/karate/e2e harness reachable)

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [ ] T003 Base models/entities and shared infrastructure the stories depend on
- [ ] T004 [P] Error handling / logging (`Logger`, not `System.out`) and config (`Config`) wiring

**Checkpoint**: Foundation ready.

---

## Phase 3: User Story 1 - [Title] (Priority: P1) 🎯 MVP

**Goal**: [What this story delivers]

**Independent Test**: [How to verify this story on its own]

### Tests for User Story 1 (MANDATORY — write FIRST)

> Write these tests before any implementation. Pick the layer-appropriate type(s) from the
> table above (unit / integration / postman / karate / e2e).

- [ ] T010 [P] [US1] [Test type] test for [behavior] in [exact test path]
- [ ] T011 [P] [US1] [Test type] test for [edge/sad path] in [exact test path]

- [ ] T012 [US1] [GATE] **Developer approval** — present the test set to the developer for
        review. Proceed only on explicit approval. If a needed test type genuinely cannot be
        implemented, the developer must state **which** and **why** here; record that rationale.
        Do not continue without an explicit decision.
- [ ] T013 [US1] [GATE] **Red** — run the approved tests and confirm they **FAIL** for the
        intended reason (not compile/setup errors). Paste/record the failing result. Do NOT
        write implementation until Red is confirmed.

### Implementation for User Story 1 *(only after T012 + T013 pass)*

- [ ] T014 [P] [US1] Create [Entity] in dotCMS/src/main/java/com/dotcms/[domain]/...
- [ ] T015 [US1] Implement [Service/endpoint] (make the failing tests pass — Green)
- [ ] T016 [US1] Refactor; add validation, error handling, logging
- [ ] T017 [US1] Confirm the tests from T010–T011 now PASS

**Checkpoint**: User Story 1 is functional and independently testable.

---

## Phase 4: User Story 2 - [Title] (Priority: P2)

**Goal**: [What this story delivers]

### Tests for User Story 2 (MANDATORY — write FIRST)

- [ ] T018 [P] [US2] [Test type] test for [behavior] in [exact test path]
- [ ] T019 [US2] [GATE] **Developer approval** of the US2 test set (or documented reason a
        type is omitted).
- [ ] T020 [US2] [GATE] **Red** — confirm US2 tests FAIL before implementing.

### Implementation for User Story 2 *(only after T019 + T020 pass)*

- [ ] T021 [P] [US2] Create [Entity]/[Service] ...
- [ ] T022 [US2] Implement [endpoint/feature]; make tests Green
- [ ] T023 [US2] Confirm US2 tests now PASS

**Checkpoint**: User Stories 1 AND 2 work independently.

---

[Add more user story phases as needed, following the SAME Tests → Approval GATE → Red GATE →
Implementation pattern. Never omit the gates.]

---

## Phase N: Polish & Cross-Cutting Concerns

- [ ] TXXX [P] Documentation updates in docs/
- [ ] TXXX Additional unit tests for edge cases surfaced during implementation
- [ ] TXXX Run quickstart.md validation; confirm full suite for touched layers is green
- [ ] TXXX Security hardening; progressive-enhancement cleanups in touched files

---

## Dependencies & Execution Order

### Within Each User Story (TDD — enforced)

1. **Tests written** (layer-appropriate) — before any implementation
2. **[GATE] Developer approval** — explicit; omissions justified in writing
3. **[GATE] Red** — tests confirmed failing for the right reason
4. Implementation (Green): models → services → endpoints → integration
5. Confirm tests pass; refactor
6. Story complete before moving to next priority

`/speckit-implement` MUST halt at each `[GATE]` task and not write implementation code until
the approval and Red gates are satisfied.

### Phase Dependencies

- Setup (Phase 1) → Foundational (Phase 2, BLOCKS all stories) → User Stories (Phase 3+) → Polish

### Parallel Opportunities

- `[P]` tasks touch different files with no dependencies and may run together.
- Tests within a story marked `[P]` can be written in parallel — but the Approval and Red
  gates are single blocking checkpoints for the whole story.

---

## Notes

- The `[GATE]` tasks are non-negotiable (Constitution Principle V). Never mark them complete
  on the developer's behalf.
- "No tests" is never a default — it requires an explicit, recorded developer decision with a reason.
- Never run the full integration suite; target specific classes/methods.
- Commit after each task or logical group (Red commit, then Green commit is encouraged).
- Verify tests FAIL before implementing; verify they PASS after.
