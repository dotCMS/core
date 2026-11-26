# Feature Specification: Velocity search-call audit and assisted rewrite for the OpenSearch migration

**Feature Branch**: `issue-37520-vtl-audit-endpoint`

**Created**: 2026-11-26

**Status**: Draft

**Type**: New Feature

**Related GitHub Issue**: [dotCMS/core#37520](https://github.com/dotCMS/core/issues/37520)

**Input**: Issue #37520 plus the decisions agreed with the developer on 2026-11-26: a bulk publish
step guarded against later edits, revert that also covers already-published runs, a way to list
past runs so a lost run id is never fatal, a persistent run record, and opt-in rewriting of the
stores that have no versions.

## Context

Customers moving from Elasticsearch to OpenSearch 3 must find and rewrite every piece of Velocity
code that calls the two deprecated search methods, `$estool.esSearch(...)` and
`$estool.esRaw(...)`. Today that is entirely manual and it is the hardest part of the migration to
do alone, for two reasons:

- The code is spread across four stores, and only one of them is a set of files a person could
  search: templates and containers kept in the database, template and container files kept as
  file assets, Velocity inside content type field definitions, and Velocity script steps inside
  workflow actions.
- The failure is silent. These templates keep working while reads still come from Elasticsearch
  and break once reads move to OpenSearch — and they break by rendering *less*, not by raising an
  error.

This feature gives support and administrators one restricted tool that finds that code, explains
what has to change, rewrites the mechanical part on request without touching the live site, and
can undo what it did.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Find every place that will break (Priority: P1)

A migration operator (a CMS administrator who also holds the migration support role) asks for an
audit before moving reads to OpenSearch. They get one report listing every occurrence of the
deprecated calls and their dependent usages, across all four stores, each with enough information
to find it in the admin UI without database access.

**Why this priority**: Knowing what breaks is the value on its own — it turns a silent production
regression into a worklist. Everything else builds on it.

**Independent Test**: Load a fixture containing one occurrence per rule in each store, request the
audit, and check that every occurrence is reported with its location and rule and that nothing in
the installation changed.

**Acceptance Scenarios**:

1. **Given** a template stored in the database whose header calls `$estool.esRaw(`, **When** the
   operator requests the audit, **Then** the report lists it with store "database template", the
   template id and title, the part of the template it is in (header), the line, the rule, and
   "auto-rewritable".
2. **Given** a file container under `/application/containers/` and a scripting endpoint under
   `/application/apivtl/` that use the deprecated calls, **When** the audit runs, **Then** each is
   reported by its site and CMS path, never by its location on disk.
3. **Given** a content type widget whose code is only `#dotParse('/application/vtl/x.vtl')`,
   **When** the audit runs, **Then** findings inside `x.vtl` are reported and also linked to the
   widget that includes it.
4. **Given** a workflow action with a Velocity script step that calls `$estool.esSearch(`,
   **When** the audit runs, **Then** it is reported with the workflow scheme, the action id and the
   action name.
5. **Given** an installation already converted, **When** the audit runs, **Then** it reports zero
   findings.
6. **Given** any audit request, **When** it completes, **Then** no object in any store has a new
   version and no stored code differs from before.

---

### User Story 2 - Preview and apply the mechanical rewrite without touching the live site (Priority: P1)

The operator first asks for a dry run and reads the exact changes the tool would make. They then
run the rewrite for real. Every versioned object gets a new draft (working) version containing the
rewritten code; nothing is published, so visitors keep seeing the current site. The operator
receives a run id and a manifest of everything that was changed.

**Why this priority**: The rewrite removes most of the manual work, and doing it as drafts is what
makes it safe to run on a production installation.

**Independent Test**: Run the dry run on the fixture and confirm nothing changed; run the real
apply and confirm every rewritable versioned object has a new draft with the expected code, its
published version is unchanged, and the pages render byte-for-byte as before.

**Acceptance Scenarios**:

1. **Given** the fixture, **When** the operator requests a dry run, **Then** the response contains,
   per object, the exact before/after text of each change, and no object in any store changes.
2. **Given** the fixture, **When** the operator applies the rewrite, **Then** each versioned object
   with an auto-rewritable finding has a new draft version, its published version is unchanged, and
   the response carries a run id and the manifest.
3. **Given** a template whose `esSearch` result variable is passed into a `#parse`, `#dotParse` or
   macro, **When** the rewrite is applied, **Then** that template is reported and left untouched.
4. **Given** findings for `toString()` used as JSON or for `maxScore`, **When** the rewrite is
   applied, **Then** those lines are never modified.
5. **Given** an installation on which the rewrite was already applied, **When** it is applied
   again, **Then** nothing is written and no run with changes is recorded.

---

### User Story 3 - Publish the run's drafts in one step (Priority: P2)

After reviewing the drafts, the operator publishes them all at once by run id instead of opening
every object. Only drafts that are still exactly what the run wrote are published; anything someone
edited in the meantime is listed back for manual handling, so a bulk publish never pushes an
unreviewed change live.

**Why this priority**: Without it, an installation with dozens of affected templates needs dozens
of manual publishes. It is a convenience on top of Story 2, not a precondition.

**Independent Test**: Apply on the fixture, edit one of the drafts by hand, publish by run id, and
confirm every other object is published while the edited one is reported and stays unpublished.

**Acceptance Scenarios**:

1. **Given** an applied run whose drafts were not edited since, **When** the operator publishes the
   run, **Then** every draft the run wrote becomes the published version.
2. **Given** an applied run where one object's draft was edited after the apply, **When** the
   operator publishes the run, **Then** that object is not published and is returned as "modified
   after apply — publish manually", and the rest are published.
3. **Given** a run that was reverted, **When** the operator tries to publish it, **Then** the
   request is refused and nothing is published.

---

### User Story 4 - Undo a run, even after it was published (Priority: P1)

If something renders wrong after the rewrite, the operator reverts the run by its id and every
object returns to its state before the apply. For objects that have no versions of their own
(content type fields and workflow actions), the code is restored from the copy kept in the run
record. If the run had been published, the published version is restored too.

**Why this priority**: Being able to undo is the condition for being allowed to run the rewrite at
all on a customer installation.

**Independent Test**: Apply on the fixture (including opted-in content type and workflow targets),
publish, revert, and confirm every object's draft and published code equals what it was before the
apply.

**Acceptance Scenarios**:

1. **Given** an applied but unpublished run, **When** the operator reverts it, **Then** every
   versioned object's draft code equals its pre-apply code and its published version is unchanged.
2. **Given** an applied and published run, **When** the operator reverts it, **Then** both the
   draft and the published code of every object equal their pre-apply code.
3. **Given** a run that rewrote a content type field and a workflow action, **When** it is
   reverted, **Then** both hold exactly their pre-apply code, taken from the run record.
4. **Given** an object edited by someone after the apply, **When** the run is reverted, **Then**
   that object is not overwritten and is reported as "modified after apply — revert manually"
   (see clarification in FR-021).
5. **Given** a run already reverted, **When** revert is requested again, **Then** nothing changes
   and the response says the run is already reverted.

---

### User Story 5 - Recover a lost run id (Priority: P2)

The operator lost the run id returned by the apply. They list past runs, newest first, see when
each ran, who ran it, how many objects it touched and whether it is applied, published or
reverted, and open the full record of any run.

**Why this priority**: The run id is the handle for publish and revert; for unversioned objects the
run record is the only backup. Losing access to it must never mean losing the ability to undo.

**Independent Test**: Apply twice with different scopes, restart the server, list runs and open
each one, and confirm both are present with correct state and full manifests.

**Acceptance Scenarios**:

1. **Given** two past runs, **When** the operator lists runs, **Then** both appear newest first
   with run id, date, user, number of objects and state.
2. **Given** a run id, **When** the operator opens it, **Then** the full manifest is returned.
3. **Given** a server restart or a request served by another node of the cluster, **When** runs
   are listed, **Then** the same runs are returned.

---

### User Story 6 - Restricted access (Priority: P1)

The tool reads and rewrites customer code, so only users who are CMS administrators **and**
members of the migration support role — the same gate as the existing migration readiness report —
may use any part of it.

**Why this priority**: Security requirement; it must hold from the first release.

**Independent Test**: Call every operation as an anonymous user, a non-admin with the role, an
admin without the role, and an admin with the role.

**Acceptance Scenarios**:

1. **Given** a user who is not both CMS administrator and member of the migration role, **When**
   they call any operation of the tool, **Then** they are refused with "forbidden" and nothing is
   read or written.
2. **Given** an admin holding the migration role, **When** they call any operation, **Then** it is
   served.

### Edge Cases

- A template has both an auto-rewritable finding and a report-only finding: the rewritable lines
  are rewritten, the report-only lines are left as they are and stay in the report.
- An `esSearch` result variable is reassigned or reused under the same name for another value in
  the same file: the rewrite of `.map.` uses cannot be attributed safely, so the finding is
  report-only.
- An object is locked by another user when the rewrite is applied: it is skipped and reported as
  not written, with who holds the lock and since when, and the run continues with the others. It
  is only unlocked and rewritten when the request names it in the explicit unlock list (FR-029).
- An object becomes locked between the dry run and the apply: it is skipped exactly as above.
- The apply fails half-way (e.g. one save throws): objects already written stay in the manifest so
  they can be reverted; the run is marked as partially applied.
- A file asset `.vtl` exists in several languages: each language version is audited and rewritten
  independently.
- The same widget file is included from many content types: it is rewritten once and every
  including widget points to the same finding.
- The deprecated calls appear only inside Velocity comments (`##` or `#* *#`): reported as
  informational, never rewritten.
- An object was deleted between the apply and a revert: revert reports it as missing and continues.
- A content type field or workflow action is listed as an explicit target but has no finding:
  nothing is written for it.

## Requirements *(mandatory)*

### Functional Requirements

**Scope of the audit**

- **FR-001**: The audit MUST read Velocity from all four stores: (a) templates in the database —
  body, header, footer and head code; (b) containers in the database — code, pre-loop, post-loop,
  and the per-content-type code of each container; (c) `.vtl` file assets, including file
  templates (`/application/templates/`), file containers (`/application/containers/`), theme
  shells and scripting endpoints (`/application/apivtl/`); (d) content type field definitions that
  hold Velocity (widget code and custom fields); (e) the code of Velocity script steps in workflow
  actions.
- **FR-002**: The audit MUST NOT treat the template's visual layout definition or a container's
  content query as code.
- **FR-003**: When a widget or custom field is only an include of a `.vtl` file, the audit MUST
  follow that one include and report the findings inside the included file, linked to the field
  that includes it.
- **FR-004**: Each finding MUST carry: the store, the object type, the object id, a human-readable
  name or CMS path (site + path for files — never a disk path), the part of the object (column,
  field or parameter), the line number, the rule, and whether it is auto-rewritable or manual.
  For templates, containers and file assets it MUST also carry whether the object is locked, by
  whom and since when (content type fields and workflow steps have no lock).
- **FR-004a**: The audit and the dry run MUST allow filtering the report to locked objects only, so
  the operator knows before applying which objects will be skipped and who holds them.
- **FR-005**: The audit MUST be strictly read-only: it creates no version, writes no code, and
  records no run.

**Rules**

- **FR-006**: The following MUST be auto-rewritable: `$estool.esRaw(` → `$estool.raw(`;
  `$r.took` → `$r.tookInMillis` when `$r` holds a raw response; `$r.hits.getAt(n)` →
  `$r.hits.hits.get(n)`; `$estool.esSearch(` → `$estool.search(` together with every
  `$item.map.field` → `$item.field` on the loop variable iterating its results.
- **FR-007**: The following MUST be reported and NEVER rewritten: `toString()` on a search
  response used as JSON (flagged as the highest-risk finding because it fails silently);
  `hits.maxScore`; any `esSearch` whose result variable, or a loop variable over it, is passed to
  `#parse`, `#dotParse` or a macro, or otherwise used outside the file.
- **FR-008**: A rewrite MUST only be applied when every affected usage is provably local to the
  same piece of code. Any doubt makes the finding manual.
- **FR-009**: The rules MUST come from a single rule source that other migration audits (for
  example the plugin audit from #37466) can reuse, not from a list private to this tool.

**Dry run and apply**

- **FR-010**: A dry run MUST return, per object, the exact before/after text of every change it
  would make, and MUST write nothing and record no run.
- **FR-011**: Apply MUST write templates, containers and file assets only as a new draft (working)
  version and MUST NOT publish anything.
- **FR-012**: Apply MUST write only through the product's normal save paths so caches and
  generated Velocity files are invalidated exactly as for a manual edit; it MUST NOT write files
  directly to disk.
- **FR-013**: Content type fields and workflow action steps have no draft/published split — saving
  them changes the live site immediately. Apply MUST NOT write them unless the request names them
  explicitly in a target list; without that list they are report-only.
- **FR-014**: Apply MUST be idempotent: applying on an installation with no remaining
  auto-rewritable findings writes nothing and records no run with changes.
- **FR-015**: Apply MUST return a run id and the run's manifest.

**Run record (manifest)**

- **FR-016**: For every object touched, the manifest MUST record: the object identity and name,
  the rules applied, the version that was current before the apply, and the version the apply
  wrote. For content type fields and workflow action steps it MUST also store the full previous
  code, since it is the only copy that will exist.
- **FR-017**: Run records MUST be persisted durably: they survive restarts and are visible from
  every node of a cluster.
- **FR-017a**: Run records MUST live in storage dedicated to this tool and readable only through
  it. They MUST NOT be stored in the generic system key-value table, because (a) every key there
  becomes a configuration property of the installation, (b) any CMS administrator can read, edit
  and delete its entries through the system-table endpoint and the maintenance JVM-info view,
  bypassing the migration-role gate and allowing the only backup of unversioned code to be lost,
  and (c) the whole table is loaded into memory at startup.
- **FR-018**: Each run MUST record its state (applied, partially applied, published, reverted),
  its date and the user who ran it.

**Publish**

- **FR-019**: Publishing a run MUST publish, for each versioned object in its manifest, the draft
  the run wrote — and only if the object's current draft is still exactly that version. Objects
  edited after the apply MUST NOT be published and MUST be returned as "modified after apply —
  publish manually".
- **FR-020**: Publishing a reverted run MUST be refused without changes.

**Revert**

- **FR-021**: Reverting a run MUST restore each object to its pre-apply state: the draft for
  unpublished runs, and both draft and published version for published runs; content type fields
  and workflow steps from the manifest copy. An object whose current state is not the one the run
  left MUST NOT be overwritten and MUST be reported as "modified after apply — revert manually".
  There is deliberately no option to force the revert over later edits: revert undoes what the
  tool did, never what a person did afterwards, and for content type fields and workflow steps a
  forced overwrite would destroy the only copy of that later work.
- **FR-022**: Reverting an already-reverted run MUST change nothing and say so.

**Runs listing**

- **FR-023**: The operator MUST be able to list all runs, newest first, with run id, date, user,
  number of objects touched and state.
- **FR-024**: The operator MUST be able to retrieve the full manifest of any run by its id.

**Access**

- **FR-025**: Every operation (audit, dry run, apply, publish, revert, list runs, read run) MUST be
  restricted to users who are CMS administrators **and** members of the migration support role,
  the same gate as the migration readiness report; any other caller MUST be refused as forbidden
  before anything is read or written.
- **FR-026**: Every apply, publish and revert MUST be written to the security audit log with the
  user, the run id and the number of objects affected.

**Locks**

- **FR-027**: Apply MUST NOT write an object that another user holds locked; it skips it and
  returns it with the lock owner and lock date.
- **FR-028**: A later apply MUST pick up objects skipped for a lock once the lock is released,
  without touching objects already converted (follows from FR-014).
- **FR-029**: Apply MUST accept an explicit list of object ids whose lock it may remove in order to
  rewrite them. Only those objects are unlocked. For each, the manifest and the security audit log
  MUST record that the lock was removed and which user held it.

### Key Entities

- **Finding**: one occurrence of a rule in one place — store, object, part, line, rule,
  auto-rewritable or manual, and for includes the field that includes the file.
- **Rule**: a named pattern with its classification (auto-rewritable with its replacement, or
  report-only with the reason a person is needed).
- **Run**: one apply — run id, date, user, state, and its manifest.
- **Manifest entry**: one object touched by a run — identity, rules applied, version before,
  version written, whether a lock was removed and from whom, and for unversioned stores the full
  previous code.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: On the reference fixture (one case per rule in each of the four stores), the audit
  reports 100% of the planted occurrences with the correct rule and location, and zero findings on
  the already-migrated fixture.
- **SC-002**: After a dry run and after an audit, zero objects in any store differ from before.
- **SC-003**: Immediately after an apply, every page of the fixture site renders exactly as before
  the apply (versioned stores only; opted-in unversioned targets change by definition).
- **SC-004**: Publish then revert of a run leaves 100% of touched objects with the same draft and
  published code they had before the apply.
- **SC-005**: A second apply on a converted installation changes zero objects.
- **SC-006**: None of the report-only cases (JSON `toString()`, `maxScore`, results escaping the
  file) is ever modified by apply.
- **SC-007**: 100% of calls by users lacking either the admin role or the migration role are
  refused, across every operation.
- **SC-008**: An operator who lost the run id can find and revert the run without database access.
- **SC-009**: Before applying, the operator can see 100% of the objects that will be skipped for a
  lock, with the lock owner; no object locked by another user is written unless it was named in
  the unlock list.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: Templates, containers, file assets, content type field
  definitions and workflow actions — all long-standing product areas, some of them legacy. The
  tool only uses their existing save, version and publish paths; it adds no new behavior to them.
- **Backward-compatibility expectations**: Nothing changes for any installation that does not call
  the tool. The tool adds dedicated storage for its run records, which is a database schema change
  — a rollback-unsafe category (`docs/core/ROLLBACK_UNSAFE_CATEGORIES.md`). The practical impact is
  bounded: rolling back to a version without it leaves storage nobody reads, and run records
  created after the upgrade cannot be used to revert from the older version. The generic system
  key-value table was considered to avoid the schema change and rejected (FR-017a). The two deprecated methods are scheduled for removal; this tool is the
  customer's path off them.
- **Known related decisions**: Same access gate as the migration readiness report
  (`/api/v1/index/migration/readiness`). Rules come from the customer-facing migration guide's
  template section and the deprecation inventory; they must be shared with the plugin audit
  (#37466) as one rule source. The plan phase will consult `dotCMS/platform-adrs`.

## Assumptions

- The tool is a support/operator tool exposed only through the restricted endpoint; there is no
  admin UI for it in this feature.
- On Elasticsearch, the neutral `search` delegates to the same underlying query as `esSearch`, so
  the rewrite changes only the type of the result, not which content is found.
- Run records are kept indefinitely; deleting old runs is out of scope.
- "Provably local" is judged per piece of code (one template part, one file, one field, one
  workflow step); cross-object data flow is never inferred.
- The audit runs synchronously; installations large enough to need background execution are out of
  scope for this feature and would be handled in a follow-up.
- Velocity outside the four stores (for example inside plugins or in content field values of
  regular contentlets) is out of scope.
