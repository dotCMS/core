# Phase 1 Data Model: Pending tab delete no-op (#36861)

**No new entities, no schema change, no migration.** This fix reads and deletes rows in an
existing table. This document records the shape it depends on, because DEC-001 makes the fix
sensitive to one column that the current delete ignores.

## `publishing_queue` (existing — `dotCMS/src/main/resources/postgres.sql:2255`)

| Column | Type | Role in this fix |
|---|---|---|
| `id` | `BIGSERIAL PK` | Not used by the delete paths. |
| `operation` | `INT8` | Part of the checkbox identity (`1` = publish, else unpublish). Renders the add/close icon. |
| `asset` | `VARCHAR(2000) NOT NULL` | The identifier the current bundle-agnostic delete keys on. |
| `language_id` | `INT8 NOT NULL` | Untouched. An existing overload deletes by `asset` + `language_id`; this fix does not use it. |
| `entered_date` | `timestamptz` | Default sort (`entered_date desc`). |
| `publish_date` | `timestamptz` | Shown per bundle; drives the gray/red styling. |
| `type` | `VARCHAR(256)` | Asset type, fed to `PermissionableProxy.setType()` for the permission check. |
| **`bundle_id`** | `VARCHAR(256)` | **The column this fix turns load-bearing.** Already present and populated — no migration. Becomes part of the checkbox `id`, is carried to the server, and joins the new `DELETE` predicate. |

### Constraints and indexes — as-is

- `bundle_id` is **nullable** and carries no FK to `publishing_bundle`. The new delete must
  tolerate a null/blank `bundle_id` rather than assuming one (fall back to a clear failure, not a
  table-wide delete — see the contract's safety rule).
- **There is no index on `asset` or `bundle_id`.** The only index in this family is
  `idx_pub_qa_1` on `publishing_queue_audit(status)`. So `WHERE asset = ? AND bundle_id = ?`
  performs a sequential scan — exactly as today's `WHERE asset = ?` already does. This is **not a
  regression** and adding an index is out of scope; noted so nobody claims a performance win that
  isn't there. The table is a work queue and is expected to be small.

## `publishing_queue_audit` (existing)

Keyed by `bundle_id` (PK). Relevant only to AC-005: the audit row for a bundle is deleted when,
and only when, that bundle's queue becomes empty. The new bundle-scoped delete must apply the
same rule the existing method applies — see `contracts/publisher-api-delete.md`.

## In-memory structures in the JSP (changed by DEC-002)

| Structure | Today | After |
|---|---|---|
| `permissionMap` | `Map<String, Boolean>`, populated per bundle inside a nested loop, one `doesUserHavePermission` call each. Missing entries for zero-element bundles → the AC-008 NPE. | Populated from one batched `filterCollection` result. Every bundle in `iresults` gets an entry (absent ⇒ `false`), which is what removes the NPE. |
| bundle → queue elements | `getQueueElementsByBundleId` called **twice** per bundle (permission pass, then render pass). | Resolved once into `Map<String, List<PublishQueueElement>>` and reused. |

**State transition**: none. Queue entries have no lifecycle state of their own — a row exists or
it does not. The publish-audit status is separate and is only *deleted*, never transitioned, by
this code path.
