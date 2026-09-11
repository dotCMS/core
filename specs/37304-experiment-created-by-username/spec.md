# Feature Specification: Creator Name on the Experiments API

**Feature Branch**: `issue-37304-experiment-created-by-username`

**Created**: 2026-09-10

**Status**: Draft

**Type**: Task (additive, non-breaking API contract change)

**Epic**: [#36763 — Experiments: A/B Testing v2](https://github.com/dotCMS/core/issues/36763)

**Work item**: [dotCMS/core#37304 — Expose the experiment creator's username in the Experiments API](https://github.com/dotCMS/core/issues/37304)

**Input**: User description: "Expose the experiment creator's username in the Experiments API" — taken from issue #37304.

---

## Scope Note *(read this first)*

Every Experiment already records who created it, but only as an opaque user ID. The payload says
`"createdBy": "dotcms.org.1"` and nothing else. That ID is not a name: it is the same value the
permission layer uses as the experiment's owner, and it means nothing to the person reading a
listing.

The new Experiments portlet wants a **Created By** column. With today's contract the portlet has two
bad options: render the raw ID, or issue a second round of requests to translate every ID it sees
into a name. Both push work onto the client that the server can do once, cheaply, from data it has
already loaded.

This work adds one field beside the existing one: `createdByUserName`, carrying the creator's full
name. It is **additive**. `createdBy` keeps its key, keeps its value and keeps its meaning, and the
owner/permission behaviour that reads it is untouched. Nothing that consumes the current payload has
to change.

Because the field belongs to the Experiment itself and not to one endpoint's response shape, it
arrives on *every* response that carries an Experiment — the listing, the single fetch, and each of
the lifecycle and variant operations that return the updated experiment.

Two things this work deliberately does **not** do. It does not touch `lastModifiedBy`, which has the
exact same ID-not-name problem and is left for a follow-up. And it does not store the name: the
value is resolved when the experiment is read, so an experiment created by someone who later changes
their name does not keep serving the old one.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See who created each experiment in the listing (Priority: P1)

A marketer opens the Experiments portlet and scans the list of experiments for the site. Each row
tells them who created that experiment by name — "Admin User", not "dotcms.org.1" — so they can find
their own work, or find the colleague to ask about someone else's.

**Why this priority**: This is the whole point of the change and the reason the consumer issue
(#37307, listing Created By column) is blocked. The listing is where an ID is least useful, because
the reader is comparing many rows at once.

**Independent Test**: Create experiments owned by two different known users, request the experiment
list, and confirm each entry reports the corresponding user's full name.

**Acceptance Scenarios**:

1. **Given** an experiment created by a user whose full name is "Admin User", **When** the experiment
   list is requested, **Then** that experiment's entry reports `createdBy` unchanged as the user ID
   **and** reports the creator's name as "Admin User".
2. **Given** a list containing experiments created by several different users, **When** the list is
   requested, **Then** each entry reports the name of its own creator.
3. **Given** a list containing many experiments created by the *same* user, **When** the list is
   requested, **Then** every entry reports that user's name and the response is served without a
   perceptible slowdown compared with the same list before this change.

---

### User Story 2 - The name travels with the experiment everywhere (Priority: P2)

A client that has just created, started, ended, archived or otherwise modified an experiment gets the
updated experiment back and can render the creator's name from that response alone, without a
follow-up fetch and without special-casing which operation it just performed.

**Why this priority**: Without it, a portlet that renders a row from a create/update response and a
row from the listing has to handle two different shapes for the same object. It is also what makes
the change cheap: one place to resolve the name, not fourteen.

**Independent Test**: Exercise each endpoint whose response carries an Experiment and confirm the
creator's name is present in each one.

**Acceptance Scenarios**:

1. **Given** an existing experiment, **When** it is fetched by ID, **Then** the response carries the
   creator's name.
2. **Given** an existing draft experiment, **When** it is started, ended, cancelled, archived,
   updated, or has a variant added, removed, renamed or promoted, **Then** each of those responses
   carries the creator's name for that experiment.
3. **Given** an experiment created through the create endpoint, **When** the create response is read,
   **Then** it already carries the creator's name — no second request is needed.

---

### User Story 3 - The column is never blank (Priority: P3)

An administrator looks at a listing that includes experiments created by a user who has since been
deleted, and by the system itself. Every row still shows something identifying in the Created By
column, and no row is missing or empty. The listing loads normally.

**Why this priority**: A rare data condition must not produce a broken-looking column or, worse, a
failed request that hides every other experiment in the list.

**Independent Test**: Point an experiment's `createdBy` at a user ID that no longer resolves, request
both the list and the single fetch, and confirm the response succeeds and reports the raw ID.

**Acceptance Scenarios**:

1. **Given** an experiment whose creator ID does not resolve to any user, **When** the experiment is
   requested, **Then** the request succeeds and the creator name field carries the raw creator ID.
2. **Given** an experiment created by the system user, **When** the experiment is requested, **Then**
   the request succeeds and the creator name field carries a non-empty value.
3. **Given** a list where one experiment has an unresolvable creator and the others do not, **When**
   the list is requested, **Then** the request succeeds, the resolvable entries report real names and
   only the affected entry falls back to the raw ID.

---

### Edge Cases

- **Creator no longer exists** (deleted user, orphaned reference): the field falls back to the raw
  creator ID. It is never null, never absent and never an empty string.
- **Creator resolves but has no usable name** (first, middle and last name all blank): treated the
  same as unresolvable — fall back to the raw creator ID, so the column still identifies something.
- **Creator is the system user**: resolves normally; if the system user has no usable name, the
  fallback rule applies. The request never fails because of it.
- **User lookup fails for an infrastructure reason** (database error, cache error): the experiment is
  still returned successfully with the fallback value. A failure to decorate must never turn a
  successful experiment read into a failed request.
- **Same creator repeated across a long list**: resolving the name must not cost one uncached lookup
  per row.
- **Creator renames themselves**: subsequent reads report the new name. The value is resolved from
  current user data at read time, not captured when the experiment was created.
- **An experiment payload is read back in** (a client echoing a response, a future import path): the
  extra field must not make that payload unreadable.
- **Experiment with no variants / archived / ended**: status has no bearing on the field; it is
  present in every state.

---

## Requirements *(mandatory)*

### Functional Requirements

**The field**

- **FR-001**: Every API response that carries an Experiment MUST include a creator-name field,
  `createdByUserName`, alongside the existing `createdBy`.
- **FR-002**: `createdByUserName` MUST carry the full name of the user identified by that
  experiment's `createdBy` value.
- **FR-003**: `createdByUserName` MUST always be present and non-empty. It is never null, never
  omitted, and never an empty string.
- **FR-004**: The field MUST be a single display-ready string. The API is not required to expose
  first name and last name separately.

**Nothing existing changes**

- **FR-005**: `createdBy` MUST keep its current JSON key and its current value (the user ID). No
  consumer of the present contract may break.
- **FR-006**: Owner and permission behaviour MUST be unchanged: the experiment's owner continues to
  resolve from `createdBy`, never from the new field.
- **FR-007**: No other existing Experiment field may be removed, renamed, retyped or reordered by
  this work. The change is strictly additive.
- **FR-008**: `lastModifiedBy` MUST be left exactly as it is today — same key, same value, no name
  companion. It is out of scope (see Out of Scope).

**Fallback and failure**

- **FR-009**: When the creator ID does not resolve to a user, `createdByUserName` MUST fall back to
  the raw creator ID.
- **FR-010**: When the creator resolves to a user whose name is blank, `createdByUserName` MUST fall
  back to the raw creator ID under the same rule as FR-009.
- **FR-011**: A failure to resolve the creator MUST NOT fail the experiment request. The endpoint
  still returns its normal success response with the experiment payload and the fallback value.
- **FR-012**: A failure to resolve one experiment's creator inside a list MUST NOT affect the other
  entries in that list.
- **FR-013**: Resolution failures MUST be observable to an operator (logged), without the log line
  being emitted once per row of a large listing.

**Cost and freshness**

- **FR-014**: Serving a list of N experiments MUST NOT perform N uncached user lookups. Resolution is
  per distinct creator, and repeated creators cost no additional database work.
- **FR-015**: The change MUST NOT add user lookups to code paths that do not serialize an Experiment
  — in particular the running-experiment selection performed during page rendering, and the
  push-publish dependency walk. Reading an experiment for those purposes must cost what it costs
  today.
- **FR-016**: `createdByUserName` MUST reflect the creator's current name. The name MUST NOT be
  captured at experiment-creation time and stored beside the experiment.
- **FR-017**: This work MUST NOT introduce a database schema change. No new column, no migration, no
  upgrade task.

**Contract, docs and tests**

- **FR-018**: `createdByUserName` MUST be documented in the API schema for the Experiment, with a
  description that states it is the creator's display name and that it falls back to the creator ID.
  The documented type MUST match what is actually returned.
- **FR-019**: The committed `openapi.yaml` MUST be regenerated from the annotations and committed
  together with the code change, so the CI contract check passes.
- **FR-020**: Integration tests MUST cover: the happy path from the list endpoint, the happy path
  from the single-fetch endpoint, and the unresolvable-creator fallback returning success plus the
  raw ID.
- **FR-021**: Every new integration test class MUST be registered in a `MainSuite*` / `Junit5Suite*`
  `@SuiteClasses` list, otherwise it never runs in CI.
- **FR-022**: Existing experiment tests MUST continue to pass unchanged, in particular any asserting
  on the shape or value of `createdBy`.
- **FR-023**: An Experiment payload that contains `createdByUserName` MUST still be readable wherever
  Experiment JSON is parsed back into an Experiment. The added field must not make a round-tripped
  payload fail to parse.

### Endpoints in scope

Every endpoint below returns a payload carrying one or more Experiments and is therefore covered by
FR-001. This list is taken from the resource as it exists today, not from the issue body (see
Assumptions, A7):

| # | Endpoint |
|---|---|
| 1 | `POST /v1/experiments` (create) |
| 2 | `PATCH /v1/experiments/{experimentId}` (partial update) |
| 3 | `PUT /v1/experiments/{experimentId}/_archive` |
| 4 | `GET /v1/experiments/{id}` |
| 5 | `GET /v1/experiments` (list) |
| 6 | `DELETE /v1/experiments/{experimentId}/goals/primary` |
| 7 | `POST /v1/experiments/{experimentId}/_start` |
| 8 | `POST /v1/experiments/{experimentId}/_end` |
| 9 | `POST /v1/experiments/scheduled/{experimentId}/_cancel` |
| 10 | `POST /v1/experiments/{experimentId}/variants` |
| 11 | `DELETE /v1/experiments/{experimentId}/variants/{name}` |
| 12 | `PUT /v1/experiments/{experimentId}/variants/{name}` |
| 13 | `PUT /v1/experiments/{experimentId}/variants/{name}/_promote` |
| 14 | `DELETE /v1/experiments/{experimentId}/targetingConditions/{id}` |

Endpoints under `/v1/experiments` that do **not** carry an Experiment, and are therefore untouched:
`DELETE /v1/experiments/{experimentId}` (returns a confirmation message), `POST /v1/experiments/isUserIncluded`,
`GET /v1/experiments/{id}/results`, `GET /v1/experiments/health`.

### Key Entities

- **Experiment**: the object being serialized. Already carries `createdBy` (the creator's user ID,
  also used as the permission owner) and `lastModifiedBy`. Gains one read-time field,
  `createdByUserName`, that is derived from `createdBy` and not persisted.
- **User**: the creator behind `createdBy`. Supplies the display name. May not exist any more, and
  may have no usable name; both cases resolve to the fallback rule.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A reader of an experiment listing can identify the creator of every row by name,
  without the client making any request beyond the one that returned the list.
- **SC-002**: 100% of the 14 endpoints listed under "Endpoints in scope" return the creator name
  field; 0% of the endpoints outside that list change shape.
- **SC-003**: 0 existing Experiment fields change key, value, type or meaning — verified by the
  existing experiment test suite passing without modification.
- **SC-004**: For a listing of 50 experiments created by a single user, the response time is
  indistinguishable from the same listing before this change (within normal run-to-run variation),
  and the number of user records read from the database is at most 1.
- **SC-005**: 100% of requests for an experiment whose creator cannot be resolved still succeed and
  report a non-empty creator name value.
- **SC-006**: The published API documentation describes the new field, and the repository's generated
  contract file matches what the build produces — the CI contract check passes on the first run.
- **SC-007**: The consumer work (#37307, listing Created By column) can be built against the
  documented contract with no further backend change.

---

## Assumptions

- **A1 — User lookups are already cache-backed.** Loading a user by ID consults the user cache before
  hitting the database (`UserFactoryLiferayImpl.loadUserById` reads `UserCache` first and populates
  it on a miss). A listing whose experiments share a creator therefore costs one database read for
  the first row and none afterwards, which is what FR-014 relies on. FR-014 still stands as a
  requirement rather than a freebie, because the cache is not infinite and a listing spanning many
  distinct creators must not degenerate into one lookup per row.

- **A2 — The mechanism for a derived JSON field is a plan-phase decision, and it is constrained.**
  The Experiment model is an Immutables value type serialized by Jackson. The two obvious immutables
  mechanisms behave differently and neither is free:
  - an eagerly-derived attribute is computed at build time, so it would fire on *every* Experiment
    construction, including the ones that never reach a response (the database transformer, the
    push-publish dependency walk, running-experiment selection during page rendering) — which
    FR-015 forbids;
  - a lazily-computed attribute is memoized inside the instance, and experiment instances are
    themselves cached, so a memoized name would outlive a rename — which FR-016 forbids.

  Neither mechanism is settable from JSON, which is where FR-023 comes from. The spec therefore
  states the constraints (FR-014, FR-015, FR-016, FR-023) and leaves the mechanism to `/speckit-plan`,
  which must verify its choice against all four before committing to it.

- **A3 — Documenting the field needs no new endpoint annotations.** `ExperimentsResource` today
  carries only a `@Tag`; it has no `@Operation`/`@ApiResponse` annotations at all, and the Experiment
  schema in `openapi.yaml` is derived from the model. A schema annotation placed on the model's
  accessor does reach the generated contract — verified against `AbstractTimestampsView`, whose
  per-accessor descriptions and examples appear verbatim under the `TimestampsView` schema. So
  FR-018 is satisfied by annotating the model, and adding a full Swagger annotation pass to
  `ExperimentsResource` is **not** part of this work.

- **A4 — "Full name" means the platform's existing notion of a full name**: first, middle and last
  name joined as `User.getFullName()` already does, rather than a new formatting rule invented here.
  That method returns an empty string when every part is blank, which is exactly the case FR-010
  covers.

- **A5 — There is no persistence round-trip to break.** Experiments are rebuilt from database columns
  by a transformer, not by parsing stored Experiment JSON, and the push-publish wrapper for
  experiments has no bundler or handler wired to it today. FR-023 is therefore a guard against future
  and client-side round-trips, not a description of a path that runs on every read.

- **A6 — The consumer needs one display string.** #37307 renders a single Created By column, so a
  single pre-joined name is sufficient; separate first/last fields are not required.

- **A7 — The issue's endpoint list is close but not exact**, and the table above supersedes it. The
  issue lists `delete` among the endpoints that carry an Experiment; `DELETE /v1/experiments/{id}`
  actually returns a confirmation message and no experiment. The issue also omits two endpoints that
  *do* return an Experiment: `DELETE /v1/experiments/{experimentId}/goals/primary` and
  `DELETE /v1/experiments/{experimentId}/targetingConditions/{id}`. Both differences are corrections
  to the issue text only — they do not change the approach, because the field is added once at the
  Experiment level and every experiment-carrying response inherits it.

- **A8 — Backend only.** No frontend work belongs to this issue; the portlet column is #37307.

---

## Out of Scope

- **`lastModifiedBy`.** It has the same ID-not-name problem and is deliberately excluded. If the
  portlet later surfaces a "last modified by" column, it can be added with the same mechanism as a
  follow-up issue.
- **The portlet's Created By column itself** (#37307) — this work only supplies the data.
- **Storing the creator's name.** No new column, no migration, no denormalized copy (FR-016, FR-017).
- **Paging, sorting, counting or permission filtering on the experiment list.** Known gaps in the
  list endpoint, tracked separately; this change must not attempt them and must not make them harder.
- **Names for any other user reference** in the experiment payload or elsewhere in the API.
- **A Swagger annotation pass over `ExperimentsResource`.** Only the new field is documented (A3).

---

## Dependencies

- **Consumer**: [#37307](https://github.com/dotCMS/core/issues/37307) — the Experiments portlet
  listing Created By column — is blocked on this field and must not merge before it.
- **Epic**: [#36763](https://github.com/dotCMS/core/issues/36763) — Experiments: A/B Testing v2.
