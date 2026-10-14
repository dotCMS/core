# Feature Specification: Migrate core-web from @Injectable to @Service

**Feature Branch**: `38041-injectable-to-service`

**Created**: 2026-10-14

**Status**: Draft

**Related GitHub Issue**: [#38041](https://github.com/dotCMS/core/issues/38041) (Team Falcon, Type: Refactoring, Priority: Low). Follows the earlier `inject()` migration ([#32750](https://github.com/dotCMS/core/issues/32750), [#32752](https://github.com/dotCMS/core/issues/32752), [#32757](https://github.com/dotCMS/core/issues/32757)).

**Input**: User description: "https://github.com/dotCMS/core/issues/38041"

## Background

Angular 22.1 introduces a `@Service` decorator: a shorthand for `@Injectable({providedIn: 'root'})`. An official schematic converts existing services:

| Before | After |
|---|---|
| `@Injectable({providedIn: 'root'})` | `@Service()` |
| `@Injectable()` | `@Service({autoProvided: false})` (same behavior: the class is still not provided automatically) |

The schematic skips a class when it injects through its constructor (itself or the closest ancestor in our own source that defines one), when `@Injectable` receives any option besides `providedIn`, or when `providedIn` is not `'root'`. By default it also covers spec files, and it removes the `Injectable` import only when every class in the file was migrated.

`core-web` is on Angular 22.1.0, so the requirement is met. A static grep (the schematic has not been run) found 291 `@Injectable(` occurrences in 280 files: 215 bare `@Injectable()`, 76 object-form, 33 files in specs/mocks/testing helpers, and 5 files with constructor injection. The largest areas are `libs/data-access` (92), `apps/dotcms-ui` (81), `libs/portlets/edit-ema` (16), `libs/dotcms-js` (15) and `libs/edit-content` (14).

This is a **modernization refactor with no behavior change**.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Know exactly what the migration will touch (Priority: P1)

A frontend maintainer needs the real scope before any file changes: which classes the schematic would convert, which it would skip and why, and how to run it in an Nx workspace that has no `angular.json`.

**Why this priority**: Every later decision (bare `@Injectable()` classes, spec files, batch sizes) depends on real numbers instead of grep estimates. It also costs nothing to undo.

**Independent Test**: Run the dry run, attach the file list to the issue, and have a second maintainer reproduce the run from the documented command alone.

**Acceptance Scenarios**:

1. **Given** a clean checkout with dependencies installed, **When** the maintainer runs the schematic in dry-run mode, **Then** the list of files it would change is produced and attached to #38041.
2. **Given** the workspace has no `angular.json`, **When** a maintainer follows the documented procedure, **Then** the schematic runs (directly through Nx, or with a temporary uncommitted workaround) and the procedure states which.
3. **Given** the dry-run output, **When** it is compared with the grep estimate, **Then** every skipped class is listed with its reason.

---

### User Story 2 - Convert `providedIn: 'root'` services to `@Service()` in reviewable batches (Priority: P1)

A reviewer receives the migration as per-area batches (for example `libs/data-access`, `apps/dotcms-ui`, then smaller libraries), so each pull request can be reviewed without reading hundreds of files at once.

**Why this priority**: This is the core of the migration; the `providedIn: 'root'` conversion is mechanical and preserves behavior exactly.

**Independent Test**: For any single batch, every class that had `providedIn: 'root'` now uses `@Service()`, lint, tests and build pass for the touched projects, and the application behaves as before.

**Acceptance Scenarios**:

1. **Given** a batch area, **When** the schematic is applied with that area's path, **Then** all its `@Injectable({providedIn: 'root'})` classes become `@Service()`.
2. **Given** a migrated file whose classes were all converted, **When** the file is inspected, **Then** no unused `Injectable` import remains.
3. **Given** a migrated file that still contains a class the schematic skipped, **When** the file is inspected, **Then** the `Injectable` import is still present and used.
4. **Given** each touched project, **When** lint, test and build run on affected projects, **Then** all pass.

---

### User Story 3 - Keep the published Angular SDK compatible with Angular 21 (Priority: P1)

An SDK consumer on Angular 21.x installs `@dotcms/angular` and it keeps working, because the SDK is not migrated.

**Why this priority**: `@dotcms/angular` declares `@angular/core >=21.2.0` as a peer, and `@Service` does not exist before 22.1. Migrating it would break released consumers.

**Independent Test**: After all batches land, `libs/sdk/angular` contains no `@Service` and still uses `@Injectable`.

**Acceptance Scenarios**:

1. **Given** the migration is applied across the workspace, **When** `libs/sdk/angular` is searched, **Then** `dotcms.store.ts`, `dotcms-editable-page.service.ts` and every other class there still use `@Injectable`.
2. **Given** any batch run, **When** the run is configured, **Then** the SDK path is excluded explicitly rather than relying on reviewers to revert it.

---

### User Story 4 - Resolve the classes the schematic skips (Priority: P2)

A maintainer wants constructor-injection holdouts either converted or explained, so none is left silently behind.

**Why this priority**: Only five known files are affected, but a skipped class is invisible in the final diff unless it is tracked.

**Independent Test**: Each skipped class is either migrated to `inject()` and then `@Service`, or listed with a reason.

**Acceptance Scenarios**:

1. **Given** the constructor-injection files (`Rule.ts`, `i18n.service.ts`, `notification.service.ts`, `relationship-field.service.ts`), **When** the work completes, **Then** each is converted to `inject()` and migrated, or recorded with a reason.
2. **Given** `dotcms-editable-page.service.ts` in the SDK, **When** the work completes, **Then** it is listed as intentionally left on `@Injectable` (see User Story 3).

---

### User Story 5 - New services use `@Service` by convention (Priority: P2)

A contributor writing a new service reads the frontend standards and uses `@Service`, so the codebase does not drift back to `@Injectable`.

**Why this priority**: Without the documented rule, new code reintroduces the old pattern and the migration decays.

**Independent Test**: Both documents state the rule and the SDK exception.

**Acceptance Scenarios**:

1. **Given** `docs/frontend/ANGULAR_STANDARDS.md` and `core-web/CLAUDE.md`, **When** a contributor looks up how to declare a service, **Then** both say new services use `@Service`.
2. **Given** the same documents, **When** a contributor works on `libs/sdk/angular`, **Then** they are told to keep `@Injectable` until the peer range is raised.

---

### Edge Cases

- A bare `@Injectable()` class provided only through a `providers` array or a component's `providers`, or a test double: migrating gives `@Service({autoProvided: false})`, which is equivalent, but the churn must be an explicit decision (see Assumptions).
- A class with `providedIn` other than `'root'`, or with extra options: the schematic skips it; it is listed, not forced.
- A class that extends a base class whose constructor injects: skipped by the schematic; the base class must be converted first.
- A file mixing migrated and skipped classes: the `Injectable` import must stay.
- Spec files declaring test doubles with `@Injectable()`: migrating them adds churn for little benefit (see Assumptions).
- A batch whose affected projects include the SDK: the SDK must still be excluded.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The migration MUST begin with a dry run, and the resulting list of files MUST be attached to #38041.
- **FR-002**: The documented procedure MUST state how to run the schematic in this Nx workspace (the command and any `angular.json` workaround), and MUST NOT require committing an `angular.json`.
- **FR-003**: `libs/sdk/angular` MUST be excluded from every batch and MUST keep `@Injectable` until its Angular peer range is raised to 22.1 or later.
- **FR-004**: Every class with `@Injectable({providedIn: 'root'})` in a migrated area MUST end up as `@Service()`.
- **FR-005**: A decision on bare `@Injectable()` classes (migrate to `@Service({autoProvided: false})` or leave) MUST be recorded and applied consistently.
- **FR-006**: A decision on spec, mock and testing-helper files (migrate or leave) MUST be recorded and applied consistently.
- **FR-007**: Each class skipped for constructor injection MUST be converted to `inject()` and migrated, or listed with a reason.
- **FR-008**: The migration MUST be delivered in per-area batches small enough for one review each.
- **FR-009**: No migrated file MUST retain an unused `Injectable` import.
- **FR-010**: Application behavior MUST NOT change: same services, same scope, same injection results.
- **FR-011**: Lint, test and build MUST pass for every touched project.
- **FR-012**: `docs/frontend/ANGULAR_STANDARDS.md` and `core-web/CLAUDE.md` MUST state that new services use `@Service`, with the SDK exception.

### Key Entities

- **Service class**: An injectable class in `core-web`; either auto-provided at root or provided explicitly by a consumer.
- **Batch**: One area of the workspace (a project or small group of projects) migrated and reviewed together.
- **Skip list**: The classes the schematic leaves unchanged, each with a reason.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: After the last batch, zero classes outside `libs/sdk/angular` that carry `@Injectable({providedIn: 'root'})` remain, except those on the documented skip list.
- **SC-002**: 100% of skipped classes are either migrated or listed with a reason.
- **SC-003**: Lint, test and build pass for 100% of touched projects in every batch.
- **SC-004**: Zero unused `Injectable` imports remain in migrated files.
- **SC-005**: Zero behavior changes: no test needs a functional update, and no user-visible difference appears in the application.
- **SC-006**: Each batch pull request is reviewable in a single sitting (target: under about 100 changed files).
- **SC-007**: A second maintainer can reproduce a batch from the documented procedure without asking questions.

## Assumptions

- The dry run may produce different counts than the grep estimate; the real list replaces it.
- **Bare `@Injectable()` (215 classes)**: default is to migrate them to `@Service({autoProvided: false})`, which preserves behavior, and to review individually only those that look like test doubles. To be confirmed by the dev before the first batch.
- **Spec/mock/testing files (33)**: default is to leave test doubles on `@Injectable` and migrate only real services that live in those files. To be confirmed before the first batch.
- The schematic is expected to run through Nx's support for Angular devkit collections; if it does not, a temporary uncommitted `angular.json` is acceptable.
- Out of scope: Java/backend code, behavior changes, raising the SDK peer range, and any change to `providers` arrays beyond what the schematic does.
- Depends on: `core-web` staying on `@angular/core` 22.1.0 or later.
