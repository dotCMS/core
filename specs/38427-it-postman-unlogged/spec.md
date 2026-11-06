# Experiment Specification: UNLOGGED integration and Postman test databases

**Feature Branch**: `issue-38427-it-unlogged-spec`
**Created**: 2026-11-04
**Status**: Draft — approval required before implementation
**Type**: Test-performance experiment
**Issue**: https://github.com/dotCMS/core/issues/38427
**Input**: Run conversion once at the beginning of each integration-test suite, not per test or test class, and before the Postman tests; use a draft PR to determine whether tests become faster.

## Goal and Scope

Determine whether PostgreSQL UNLOGGED application tables reduce integration-suite and Postman-batch elapsed time enough to outweigh conversion overhead. A measured lack of improvement is a valid experimental result, not a reason to omit overhead or failing runs.

This affects disposable test databases only. It does not change production schema defaults, application APIs, or production startup behavior.

## User Scenarios & Testing

### User Story 1 — Prepare each integration suite once (P1)

As a developer, I can run an integration suite with its application tables converted to UNLOGGED before its first test, without repeating preparation for every class or method.

**Independent test**: Execute a small suite containing multiple classes and methods; verify preparation completes once before the first method. Execute a second suite in the same JVM and verify it has its own preparation event.

**Acceptance scenarios**:

1. **Given** an explicitly enabled experiment and an initialized disposable database, **when** a suite starts, **then** all existing supported application tables are UNLOGGED before its first test executes.
2. **Given** multiple test classes and methods, **when** they run, **then** conversion is not invoked again for those classes or methods.
3. **Given** two suites sharing a JVM, **when** each starts, **then** each prepares once; an already converted database remains safe to prepare again.
4. **Given** the experiment is disabled, **when** the suite starts, **then** existing persistence settings remain unchanged.
5. **Given** the existing JUnit 4 and JUnit 5 suite families, **when** either runs in experimental mode, **then** it receives the same suite-scoped preparation behavior.

### User Story 2 — Prepare a Postman batch once (P1)

As a developer, I can run a selected batch of Postman collections against UNLOGGED tables without converting before every collection or request.

**Independent test**: Start a disposable Postman environment and run at least two collections; verify preparation occurs after application/schema readiness and before any selected collection runs, exactly once for the batch.

**Acceptance scenarios**:

1. **Given** a newly started Postman environment, **when** the application and schema are ready, **then** conversion completes before the selected collections begin.
2. **Given** multiple selected collections, **when** they execute, **then** preparation is not repeated per collection or request.
3. **Given** conversion fails, **when** the Postman runner would start, **then** the batch fails instead of testing against a partly converted database.

### User Story 3 — Compare actual elapsed time (P2)

As a developer, I can compare logged and unlogged runs without mistaking skipped tests or faster early failures for an improvement.

**Independent test**: Compare matched logged/unlogged runs for an integration suite and a Postman batch, checking actual executed test counts and outcomes.

**Acceptance scenarios**:

1. **Given** identical code, selected tests, initial data, search configuration, and runner class, **when** both modes execute, **then** the results report conversion duration, test execution duration, total duration, test counts, failures, and retries.
2. **Given** a cached or zero-test run, **when** results are compared, **then** it is excluded with the reason stated.
3. **Given** three completed runs per mode per measured suite/batch, **when** results are reported, **then** raw run links, individual durations, medians, and percentage differences are included. Insufficient comparable runs are reported as inconclusive.

### Edge Cases

- Foreign keys require dependency-aware conversion; self-references must remain valid. Unsupported cross-table cycles fail clearly rather than dropping constraints.
- Already UNLOGGED tables are left intact; all fixture rows, indexes, and constraints remain usable.
- Permission failures, lock timeouts, or unsupported table kinds fail before test execution, with persistence changes rolled back.
- System schemas, extension-owned objects, other databases, and temporary tables must not be modified.
- Empty or incompletely initialized application schemas must not silently count as successful preparation.
- Tables created during a test retain their normal behavior; continuous conversion is outside this experiment.
- Unrelated running development/Postman environments must not be reused, stopped, or altered by the experiment.

## Requirements

- **FR-001**: Preparation MUST be suite-scoped for integration tests and batch-scoped for Postman, after schema readiness and before the first test or collection.
- **FR-002**: The experiment MUST be explicitly enabled and disabled for matched comparisons. Normal runs remain unchanged by default; experimental CI runs explicitly enable it.
- **FR-003**: Conversion MUST target only the explicitly identified disposable test database and its application tables. It MUST refuse unapproved database targets.
- **FR-004**: Conversion MUST preserve existing rows, foreign keys, indexes, and transaction semantics; it MUST NOT remove constraints to force success.
- **FR-005**: Conversion MUST be atomic, dependency-aware, safely repeatable, and bounded against indefinite lock waits. Failure prevents tests from starting.
- **FR-006**: Logs MUST identify the suite/batch, database, converted table count, and elapsed conversion time without exposing connection credentials or test data.
- **FR-007**: Comparison runs MUST disable test-execution build-cache shortcuts and verify nonzero executed counts from reports, not just successful job exit codes.
- **FR-008**: Results MUST distinguish test execution time from job setup and include conversion overhead in the end-to-end comparison. Unequal counts or new failures invalidate a speedup claim.
- **FR-009**: No automatic conversion back to LOGGED is required: each measured run uses a fresh disposable environment, removed afterward.

## Success Criteria

- **SC-001**: Preparation occurs exactly once per integration suite and once per Postman batch, never per class, method, collection, or request.
- **SC-002**: Before test execution, every supported existing application table in the targeted schema is confirmed UNLOGGED, with data and constraints preserved; no out-of-scope object is changed.
- **SC-003**: Disabled mode, dependency ordering, self-references, repeated execution, failure rollback, target isolation, and suite/batch ordering have approved tests demonstrated failing before implementation and passing afterward.
- **SC-004**: At least one integration suite and one Postman batch have the matched measurements described above, or the report explicitly states why results remain inconclusive. Broader CI results are reported per suite/batch rather than pooled into a misleading average.
- **SC-005**: The draft reports whether median total time improved, regressed, or remained inconclusive. No minimum speedup is assumed in advance.

## Legacy Considerations

- **Existing behavior touched**: Integration-suite orchestration and Postman environment preparation span modern and legacy dotCMS behavior, but production behavior remains unchanged.
- **Backward compatibility**: Existing suite membership, test ordering, selected Postman collections, APIs, and normal logged-mode runs remain unchanged.
- **Known related decisions**: This experiment does not broaden the production use of UNLOGGED tables. Relevant architectural decisions must be consulted during implementation planning after spec approval.

## Assumptions and Safety

- The benchmark uses PostgreSQL 18, matching the current test database image.
- UNLOGGED tables are truncated after PostgreSQL crash recovery and are unsuitable for persistent or replicated application environments. A crash invalidates the run; discard the database and repeat from a clean environment.
- Conversion rewrites tables and takes strong locks. Its cost is part of the experiment, not excluded as incidental setup.
- The checked-in base schema was loaded successfully in an isolated PostgreSQL 18.6 container: 168 ordinary public tables, with no cross-table foreign-key cycles found. Child-first conversion preserved rows in a small foreign-key fixture. This is feasibility evidence only, not a full initialized-database check or performance result.
- Spec approval precedes implementation planning. Written tests require developer approval and demonstrated assertion failures before implementation. This draft contains no experimental implementation and cannot itself demonstrate a speedup.
