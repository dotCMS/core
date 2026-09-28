# Feature Specification: Instance-Wide Custom Field Migration Script

**Feature Branch**: `[20260928-120007-custom-field-migration]`

**Created**: 2026-09-28

**Status**: Draft

**Type**: New Feature

**Input**: User description: "Bundle a self-contained Python script in the `dot-ui-vtl-migration` skill (`.claude/skills/dot-ui-vtl-migration/scripts/`) so a customer can migrate every legacy (Dojo/Dijit) custom field in their dotCMS instance in one guided session, instead of copy-pasting VTL into the agent one field at a time. (GitHub issue dotCMS/core#37764)"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Discover everything that needs migrating (Priority: P1)

A customer with a live dotCMS instance asks their AI agent (running the `dot-ui-vtl-migration` skill) to migrate all legacy custom fields at once. Today they can only hand the agent one VTL file at a time, so on an instance with dozens of content types they'd have to know in advance which fields are legacy, find each one's code by hand, and repeat the copy-paste-migrate cycle for every one. Instead, the agent should be able to scan the whole instance and come back with a single, complete inventory: which custom fields need migrating, which don't (and why), and what it downloaded for the agent to work on next.

**Why this priority**: Without a reliable, complete discovery step nothing else in the flow can be trusted — the customer would still have to manually verify no field was missed, which defeats the purpose of the feature. This is the smallest slice that already removes the "one field at a time" pain.

**Independent Test**: Point the tool at a running instance with a mix of legacy custom fields (some referencing a shared downloadable file, some with VTL written directly in the field, some already migrated, some out of scope) and confirm it produces one inventory covering every custom field on every content type, with each one correctly placed in exactly one category, and nothing written back to the instance yet.

**Acceptance Scenarios**:

1. **Given** an instance with several content types that each have a custom field pointing at the *same* downloadable legacy code file, **When** discovery runs, **Then** the file is fetched once, the inventory records every content-type field that uses it, and the customer sees one entry, not a duplicate per field.
2. **Given** an instance with custom fields whose code lives directly in the field configuration (not in a separate file) and still uses old-style patterns, **When** discovery runs, **Then** each such field's code is saved for migration and clearly attributed to its content type and field name.
3. **Given** an instance where some custom fields already run the modern code path, load a file that ships with dotCMS itself, or reference code by a path discovery doesn't handle, **When** discovery runs, **Then** those fields are listed as "nothing to do here" (with a reason) and no file is downloaded or queued for migration for them.
4. **Given** a discovery pass that already completed successfully for a file or field, **When** discovery is run again before anything is migrated, **Then** that same file or field is reported as already covered, not re-downloaded as if new.

---

### User Story 2 - Review the proposed changes before anything goes live (Priority: P2)

After the agent migrates each discovered file, the customer wants to see a clear, per-file summary of what would change on their live instance and to explicitly say "go ahead" before anything is actually published — with the option to first see exactly what *would* happen with zero risk.

**Why this priority**: Publishing to a live, in-use dotCMS instance is not reversible in the same session, and mistakes touch customer-facing pages. A trustworthy preview step is what makes the customer comfortable delegating an instance-wide change to an agent at all.

**Independent Test**: With a set of already-migrated files ready to publish, request a preview run and confirm nothing on the instance changes as a result (no content, code, or configuration is modified), while the customer still sees exactly what each real run would do.

**Acceptance Scenarios**:

1. **Given** migrated files are ready, **When** the customer runs a preview, **Then** every prospective change is described but the live instance is left completely untouched.
2. **Given** the customer wants to try the feature on a handful of items before trusting it with everything, **When** they ask to limit a run to specific items, **Then** only those items are considered and everything else discovered earlier is left alone.

---

### User Story 3 - Publish safely, without clobbering other people's concurrent changes (Priority: P3)

Once the customer confirms, the agent publishes each migrated file to the live instance. Because a real dotCMS instance keeps being used by other people while this migration is in progress, the tool must refuse to publish over content that someone else changed in the meantime, and must refuse to publish a "migration" that doesn't actually look safe (for example, one that silently drops the original behavior instead of preserving it as a fallback).

**Why this priority**: This is the step that actually delivers the business value (the legacy code is gone) but is also the step with real consequences if it goes wrong — a bad publish can break a live page for site visitors, and an unnoticed conflict can silently discard someone else's concurrent edit.

**Independent Test**: Simulate one item changing on the server between discovery and publish, and one migrated file that fails the "looks like a real migration" check, and confirm both are skipped and clearly reported — with the rest of the batch publishing normally — while a completely clean run publishes everything for real.

**Acceptance Scenarios**:

1. **Given** an item that was modified on the live instance after it was discovered, **When** publish runs, **Then** that item is skipped and reported (not force-overwritten), and the customer is told to re-run discovery.
2. **Given** a migrated file that doesn't preserve the original legacy behavior as a fallback, or that never actually branches between the old and new experience, **When** publish runs, **Then** that item is skipped and reported instead of published.
3. **Given** a batch containing both publishable items and problem items, **When** publish runs, **Then** the publishable items go live and the problem items are individually called out, so one bad item never blocks the rest of the batch.
4. **Given** a content item of a migrated type, **When** it is subsequently opened in the new editor and separately in the legacy editor, **Then** the new editor shows the new behavior and the legacy editor still shows the original behavior.

---

### Edge Cases

- What happens when the customer's login doesn't have permission to view all content types, download the referenced files, or publish changes? The affected items must be reported as failures with a reason, not silently dropped, and the overall run must fail loudly rather than report false success.
- What happens when the customer's credentials are missing or wrong? The tool must fail immediately, explain which credentials are involved and that the instance may not accept this login method at all, and must not write any file or make any partial progress.
- What happens when the same downloadable file is referenced by fields on several different content types, and those content types are not all migrated to the modern editor at the same time? Each content type must independently show old vs. new behavior for its own contentlets, since the shared file itself supports both.
- How does the tool behave if it's interrupted (e.g., network drop) mid-way through publishing a batch? Items already published stay published; the run must report exactly which items succeeded, which were skipped, and which never got attempted, so the customer knows precisely what to re-run.
- What happens when a discovered item was already unpublished/in a draft state before migration even started? The tool must still be able to act on it, but must call out that publishing it also promotes that pending draft state live, so the customer isn't surprised by unrelated changes going live alongside the migration.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The tool MUST be able to scan every content type on a customer's dotCMS instance and identify every custom field on every one of them.
- **FR-002**: For each custom field found, the tool MUST classify it into exactly one category: (a) points at a separate downloadable legacy code file, (b) has legacy code written directly inside the field itself, (c) loads a file that ships with dotCMS core and is out of scope, (d) references code by a path this tool doesn't resolve and is out of scope, or (e) has nothing that needs migrating (no legacy patterns, or already migrated).
- **FR-003**: The tool MUST download, once per distinct file, every downloadable file referenced by category (a) fields, and MUST record every field across every content type that references that same file.
- **FR-004**: The tool MUST save the in-field code for every category (b) field so it can be migrated the same way as a standalone file.
- **FR-005**: The tool MUST produce one inventory covering every custom field on the instance, grouping entries by category, and identifying category (c), (d), and (e) fields by their content type and field name without downloading or queuing them for migration.
- **FR-006**: The tool MUST let the customer preview a publish run that describes every change it would make without making any of those changes on the live instance.
- **FR-007**: The tool MUST let the customer limit a publish run to a specific subset of previously discovered items, leaving everything else untouched.
- **FR-008**: The tool MUST NOT publish any change to the live instance except as part of an explicit publish step the customer has approved; discovery and preview MUST never modify the instance.
- **FR-009**: Before publishing an item, the tool MUST verify that item has not changed on the live instance since it was discovered, and MUST skip (and report, not silently drop) any item that has.
- **FR-010**: Before publishing an item, the tool MUST verify the migrated version genuinely preserves the original legacy behavior as a fallback alongside new behavior, and MUST skip (and report) any item that doesn't look like a real migration (e.g., the original behavior is missing, or there's no visible switch between old and new).
- **FR-011**: A publish run MUST process every eligible item in the batch independently, so that one item failing its safety checks or a server error does not prevent the rest of the batch from being attempted.
- **FR-012**: The tool MUST report, for every attempted item, the outcome (published, skipped, or failed) with enough detail that the customer knows exactly what to do next (e.g., re-run discovery, or fix a specific file).
- **FR-013**: Re-running discovery after a successful publish MUST recognize every published item as already migrated and MUST NOT re-download or re-queue it.
- **FR-014**: The tool MUST require the customer's own credentials for every instance interaction and MUST fail immediately, with a clear explanation, if credentials are missing or rejected — without writing any partial output.
- **FR-015**: The tool's discovery and publish outcomes MUST be consumable by an AI agent without any human sitting at a terminal — i.e., no interactive prompts, and a final structured result an agent can parse to decide what to tell the customer next.
- **FR-016**: The migration capability MUST be packaged as part of the existing `dot-ui-vtl-migration` skill, reachable by an agent working through that skill, and documented well enough that a new session can discover and use it for an instance-wide request without prior knowledge of the tool.

### Key Entities

- **Content Type**: A dotCMS structure definition that owns zero or more fields, including custom fields.
- **Custom Field**: A field on a content type that runs legacy Dojo/Dijit-based code today; the unit this feature discovers, classifies, and (where in scope) migrates.
- **Downloadable Code File**: A binary asset referenced by one or more custom fields' configuration by a shared identifier; migrated once and republished, decoupled from any single field that uses it.
- **In-Field Code**: Legacy code written directly inside a custom field's own configuration rather than in a separate file; migrated and written back to that field's configuration.
- **Migration Item**: One discovered, classifiable unit of work (a downloadable file or an in-field code block) tracked from discovery through publish, with its own status (pending, published, skipped, failed) and the reason behind that status.
- **Migration Run Record**: The record produced by discovery that lists every migration item found on the instance, used as the source of truth for what a later publish step is allowed to act on.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A customer can get a complete, accurate inventory of every legacy custom field across their entire instance in a single request, with zero fields missed and zero fields double-counted, regardless of instance size.
- **SC-002**: When several content types share the same legacy code file, the customer sees that file listed exactly once, with every content type that uses it named alongside it.
- **SC-003**: No content on the customer's live instance changes as a result of discovery or of a preview run — verified by the instance state being identical before and after those steps.
- **SC-004**: The customer always sees a clear, itemized description of proposed changes before anything is published, and can choose to publish only a subset.
- **SC-005**: When an item changed on the instance after it was discovered, or a migrated item doesn't safely preserve the original behavior, publishing it is refused and reported, in 100% of such cases, without blocking the rest of the batch.
- **SC-006**: After a successful publish, opening an affected piece of content shows the new experience in the modern editor and the original experience in the legacy editor, with no visible regression to either.
- **SC-007**: Running discovery again immediately after a successful publish shows every published item as already done, with no duplicate work offered.
- **SC-008**: An agent-driven session (no person manually reading terminal output) can reliably determine, from the tool's output alone, whether the overall run succeeded, partially succeeded, or failed, and why.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: This feature only migrates legacy (Dojo/Dijit) **custom fields**, one of dotCMS's oldest and still actively used field types, from the legacy Content Editor code path to the modern editor's `DotCustomFieldApi`. It does not touch any other legacy field type, content type feature, or unrelated legacy subsystem.
- **Backward-compatibility expectations**: Every migrated field must keep working exactly as before for content opened in the legacy editor; the migration adds a modern-editor code path alongside the original one rather than replacing it, and nothing may be published without preserving that original behavior as a fallback. Existing content authored against these fields must continue to render and edit correctly after migration.
- **Known related decisions**: This feature builds directly on the single-file inline migration output established for one-VTL-file-at-a-time migration (dotCMS/core#37742, #37757) — this feature adds the instance-wide discovery/publish I/O around that existing capability; it does not change how an individual file is migrated.

## Assumptions

- The customer (or the agent acting on their behalf) already has a dotCMS user with sufficient permission to read all content types, download the files their custom fields reference, and publish content — no separate permission-elevation flow is in scope.
- "The live instance" means a real, reachable dotCMS environment (e.g., a customer's dev, staging, or production instance) that the agent connects to directly for this session; there is no offline or simulated mode in scope.
- Login is by username/password against the instance; no other authentication method is in scope for this feature.
- Migrating an unpublished/draft custom field or file is in scope, but publishing it as part of this migration is expected to also promote any other pending changes on that same content live — this is called out to the customer, not specially isolated.
- A downloadable code file and an in-field code block are the only two shapes of legacy custom-field code this feature acts on; every other shape it encounters is reported, not migrated, by design (out of scope per the source issue).
- This feature packages and exposes the capability; the actual line-by-line VTL migration logic (old code → old+new branching code) is the existing, separate capability this feature reuses, not something this feature redefines.
