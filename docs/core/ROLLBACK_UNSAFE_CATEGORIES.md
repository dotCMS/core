# Rollback-Unsafe Categories — Developer Reference

>**Purpose:** Help developers identify whether a change they are introducing makes a release rollback-unsafe, before the change is merged.
>
>**Rule of thumb:** A change is rollback-unsafe if the previous application version (N-1) cannot start, read data correctly, or serve requests correctly after the change has been deployed by N — **without manual database interaction or Elasticsearch reindexing.**

---

## Risk Levels

| Level | Meaning |
| :---- | :---- |
| 🔴 CRITICAL | N-1 cannot start, or data is permanently lost or unreadable. Recovery requires database backup restoration and/or full Elasticsearch reindex. |
| 🟠 HIGH | N-1 starts but core features are broken for affected content or users. Targeted recovery is required. |
| 🟡 MEDIUM | N-1 degrades in specific scenarios. No backup needed, but ops action is required to restore normal behavior. |
| 🟢 LOW | Narrow or temporary impact. Typically self-recovers or requires minimal intervention. |

---

## Quick Reference — Decision Card

Use this during code review or PR checklist:

```
Is my change a...

Structural storage model change?                      → 🔴 CRITICAL (C-1)
ES mapping change in code or task?                    → 🔴 CRITICAL (C-2)
contentlet_as_json model version bump?                → 🔴 CRITICAL (C-3)
DROP TABLE or DROP COLUMN?                            → 🔴 CRITICAL (C-4)
One-way data transformation or backfill?              → 🟠 HIGH     (H-1)
RENAME TABLE or RENAME COLUMN?                        → 🟠 HIGH     (H-2)
Primary key restructuring?                            → 🟠 HIGH     (H-3)
New ContentType field type?                           → 🟠 HIGH     (H-4)
Storage provider configuration change?                → 🟠 HIGH     (H-5)
DROP PROCEDURE or DROP FUNCTION?                      → 🟠 HIGH     (H-6)
NOT NULL column without default?                      → 🟠 HIGH     (H-7)
Non-broadening column type change?                    → 🟡 MEDIUM   (M-1)
Push publishing bundle XML schema change?             → 🟡 MEDIUM   (M-2)
REST / GraphQL API contract change?                   → 🟡 MEDIUM   (M-3)
OSGi public interface / service change?               → 🟡 MEDIUM   (M-4)


None of the above (ADD COLUMN nullable, CREATE TABLE,
CREATE INDEX, ADD CONSTRAINT non-conflicting)?        → ✅ SAFE
```

---

# 🔴 CRITICAL

---

## C-1 — Structural Data Model Change

**Risk level:** 🔴 CRITICAL

### Context

A structural data model change modifies *how* core entities are stored or referenced — not just adding a new column, but changing the fundamental storage representation that N-1 was built around.

This is distinct from a schema extension (adding a nullable column). Structural changes alter the storage contract between the application and the database.

### Why it is unsafe

N-1 was compiled with assumptions about where data lives. If N moves that data to a different column, table, or format, N-1 will look in the wrong place — reading empty or wrong values, or failing entirely.

These changes typically combine multiple operations (add column \+ migrate data \+ drop old column) and are one-way by design.

### Examples from dotCMS history

- **Content field storage migration:** Content field values migrated from typed columns (`text1…text25`, `bool1…bool25`) to a single `contentlet_as_json` JSONB column. N-1 reads from the typed columns, which are now empty or stale. All content reads silently return no field values.
- **Folder identifier unification:** `folder.inode` was set to equal `folder.identifier`, and path references were updated. N-1 uses `inode` semantics for folder resolution — after migration, all folder path lookups return wrong results.
- **Variant PK insertion into `contentlet_version_info`:** Adding `variant_id` to the primary key of `contentlet_version_info` (Task221007) means N-1 queries that JOIN on the old 2-column PK either fail or return duplicate rows.

### Signals to watch for in code review

- A `runonce` task that combines `ADD COLUMN` \+ `UPDATE … SELECT` \+ `DROP COLUMN` on core tables
- Any task touching `contentlet`, `identifier`, `inode`, `tree`, `multi_tree`, `contentlet_version_info`
- A migration that removes the old storage path after populating the new one

### Safer alternative

Use the **two-phase migration** pattern:

| Release N | Release N+1 |
| :---- | :---- |
| Add new column/structure (SAFE) | Remove old column (after N-1 is fully retired) |
| Write to both old AND new | Write only to new |
| N-1 reads old; N reads new | N-1 no longer in rotation |

---

## C-2 — Elasticsearch Mapping Change

**Risk level:** 🔴 CRITICAL

### Context

Elasticsearch enforces strict schema on indexed documents. Any change to a field's type, analyzer, or nesting structure is applied to the live index at server startup via `putMapping()`. There is no version marker — the mapping is overwritten immediately.

Changes can come from two sources:

- A `runonce` task that triggers reindexing
- A pure code change in `ESMappingAPIImpl`, `ESMappingUtilHelper`, or a field type's mapping handler

### Why it is unsafe

Once N's mapping is applied to the ES index, N-1 queries are sent against a structure they were not built for:

- A `text` field changed to `keyword` breaks N-1 full-text queries
- A new `nested` object breaks N-1 flat-field queries
- A changed analyzer alters tokenization — N-1 phrase searches return wrong results
- A removed field means N-1 `exists` filter queries return incorrect results

There is **no automatic rollback of ES mappings**. Even reverting the application binary does not revert the live index mapping.

### Examples from dotCMS history

- Changing how content fields are mapped based on the new `contentlet_as_json` format
- Adding `variant`\-aware field prefixes to the index
- Any change to `ESMappingConstants` field name constants used in ES queries

### Signals to watch for in code review

- Changes to `ESMappingAPIImpl`, `ESMappingUtilHelper`, or `ESMappingConstants`
- A new field type whose `getESMappingType()` method differs from an existing one
- Any code that calls `client.admin().indices().putMapping()`
- A `runonce` task that calls `ReindexThread` or `ContentletIndexAPI.fullReindexStart()`

### Safer alternative

- Never change the ES type of an existing mapped field — add a new field instead
- If a mapping change is unavoidable, pair it with an explicit reindex task and document it as rollback-unsafe in the release notes
- Maintain a mapping version file that CI can diff between branches to detect breaking changes automatically

---

## C-3 — Content JSON Serialization Model Version Bump

**Risk level:** 🔴 CRITICAL

### Context

Content stored in `contentlet_as_json` uses a versioned JSON model (`@JsonVersionedModel`, `CURRENT_MODEL_VERSION`). The version is stored in every row as `"modelVersion": "N"`. A `ToCurrentVersionConverter` handles upgrades from older versions.

**There is no downgrade converter.** The converter only runs in one direction.

### Why is it unsafe

If N bumps `CURRENT_MODEL_VERSION` (e.g., from `"2"` to `"3"`) and writes content to the database, N-1 will read those rows and find `modelVersion = 3`. There is no converter registered for `3 → 2`, so N-1 will attempt to deserialize the raw JSON as a v2 object.

Jackson's `@JsonIgnoreProperties(ignoreUnknown = true)` will silently ignore fields that exist in v3 but not v2. Any field added to v3 will be invisible to N-1 — content fields return empty or default values with **no error thrown**.

An additional risk exists in **dual persistence mode**: if N writes exclusively to `contentlet_as_json` (not to typed columns), N-1 configured to read typed columns finds only stale or empty column values.

### Example from dotCMS history

- `Task230523CreateVariantFieldInContentlet` added `variant_id` to the contentlet JSON structure and removed it from the top-level dynamic columns. N-1, reading the typed column path, finds no `variantId` value.

### Signals to watch for in code review

- Any change to `CURRENT_MODEL_VERSION` in `ImmutableContentlet`
- New fields added to `ImmutableContentlet` that are required for correct behavior
- Changes to `ToCurrentVersionConverter` that do not add a matching downgrade path
- Changes to `ContentletJsonAPIImpl` serialization logic

### Safer alternative

- Treat `CURRENT_MODEL_VERSION` as a breaking change gate — require explicit rollback-unsafe annotation on any PR that bumps it
- Add new fields as optional with defaults so N-1 silently ignores them without losing functionality
- Implement a matching downgrade converter for every upgrade converter added

---

## C-4 — DROP TABLE or DROP Essential Column

**Risk level:** 🔴 CRITICAL

### Context

Dropping a table or column that N-1 actively queries causes immediate startup failures or runtime exceptions in N-1.

### Why it is unsafe

N-1's SQL queries (in `DotConnect`, factory classes, and Hibernate mappings) reference the dropped object by name. On the first query attempt, N-1 throws a `SQLException` (table/column not found). Depending on the code path, this may:

- Prevent the application from starting (if queried during startup)
- Cause 500 errors for any request that touches the dropped object
- Silently return empty results if exceptions are swallowed

### Examples from dotCMS history

- `Task00775DropUnusedTables` — dropped 14 tables and bulk-deleted from `inode`/`tree`
- `Task03745DropLegacyHTMLPageAndFileTables` — dropped `htmlpage_version_info`, `htmlpage`, `fileasset_version_info`, `file_asset`
- `Task210527DropReviewFieldsFromContentletTable` — dropped `last_review`, `next_review`, `review_interval` from `contentlet`
- `Task210805DropUserProxyTable` — dropped `user_proxy` entirely

### Signals to watch for in code review

- Any `runonce` task containing `DROP TABLE` or `ALTER TABLE … DROP COLUMN`
- Tasks named `DropLegacy*`, `Remove*Table`, `Drop*Fields`

### Safer alternative

- Never drop a table or column in the same release that removes its last code reference
- Follow the **two-phase removal pattern**:
  - Release N: Remove all code references (column becomes unused)
  - Release N+1: Drop the column (now safe to drop — N-1 no longer queries it, and N-2 is outside rollback window)

---

---

# 🟠 HIGH

---

## H-1 — One-Way Data Migration or Destructive Backfill

**Risk level:** 🟠 HIGH

### Context

Data migrations transform the *content* of rows — not just the schema. They are a separate risk from schema changes: even if the schema is backward-compatible, transformed data may be semantically unreadable by N-1.

### Why it is unsafe

Once data has been transformed (e.g., all URLs lowercased, all identifiers remapped, field values rewritten), there is no automatic undo. N-1 was written to interpret data in the old format. After the transformation, N-1 may:

- Misinterpret transformed values (e.g., case-sensitive URL matching breaks)
- Fail to resolve references (e.g., identifier remap breaks content lookup)
- Return empty or wrong data (e.g., a semantic rewrite makes old field values meaningless)

### Examples from dotCMS history

- `Task04105LowercaseVanityUrls` — applied `LOWER()` to all vanity URL data; original casing is permanently lost
- `Task04115LowercaseIdentifierUrls` — lowercased all identifier paths; case-sensitive routing in N-1 breaks
- `Task03525LowerTagsTagname` — lowercased all tagnames and merged duplicates; N-1 tag resolution differs
- `Task220330ChangeVanityURLSiteFieldType` — changed field type from `CustomField` to `HostFolderField` and rewrote identifier references
- `Task210901UpdateDateTimezones` — changed all timestamp columns from `timestamp` to `timestamptz` with `USING` clause; time values shifted

### Signals to watch for in code review

- A `runonce` task that issues `UPDATE table SET col = transform(col)` across all rows
- `DELETE` of business records (not orphan cleanup)
- `TRUNCATE` \+ rebuild pattern
- Any migration that changes the semantic meaning of stored values (not just format)

### Safer alternative

- Prefer **additive migrations**: add a new column with the transformed value, keep the original column, and let N-1 continue reading the original
- If transformation is unavoidable, store the original value in a backup column before transforming
- Only drop the original column in a later release (two-phase removal)

---

## H-2 — RENAME TABLE or RENAME COLUMN

**Risk level:** 🟠 HIGH

### Context

Renaming a table or column is one of the most immediately breaking changes possible. Unlike dropping, the data still exists — but all N-1 queries use the old name and will fail.

### Why it is unsafe

Every SQL statement in N-1 that references the old name produces a `SQLException: table/column not found` on its first execution. In dotCMS, factories and cache loaders run at startup, so a rename on a core table will typically prevent N-1 from starting at all.

### Examples from dotCMS history

- `Task00780UUIDTypeChange` — renamed `inode` → `identifier` across multiple core tables; broke every N-1 query referencing those tables
- `Task00815WorkFlowTablesChanges` — renamed `inode` → `id` in workflow tables
- `Task03550RenameContainersTable` — renamed `containers` → `dot_containers`; all N-1 container queries fail

### Signals to watch for in code review

- `ALTER TABLE … RENAME TO …`
- `ALTER TABLE … RENAME COLUMN old TO new`
- Tasks named `Rename*`

### Safer alternative

The standard zero-downtime rename pattern:

1. Add the new name as an alias or view (Release N — SAFE)
2. Migrate all code to use the new name (Release N)
3. Remove the old name (Release N+1 — only N-2 is outside rollback window by then)

---

## H-3 — Primary Key or Unique Constraint Restructuring

**Risk level:** 🟠 HIGH

### Context

Dropping and recreating a primary key with a different column set changes the fundamental identity model of a table. N-1 performs JOINs and lookups using the old PK structure — those queries either fail or return incorrect rows.

### Why it is unsafe

N-1 executes queries like `WHERE id = ?` or `JOIN ON a.id = b.id` using the old PK shape. After the restructuring:

- Queries using only the old PK columns may match multiple rows (if the new PK added a discriminator column)
- Queries using the full new PK fail with unknown column errors
- INSERTs from N-1 may violate the new PK if the new column has no default

### Examples from dotCMS history

- `Task04315UpdateMultiTreePK` — dropped and recreated `multi_tree` PK with different columns; N-1 page layout queries return wrong results
- `Task05160MultiTreeAddPersonalizationColumnAndChangingPK` — added personalization to `multi_tree` PK
- `Task221007AddVariantIntoPrimaryKey` — added `variant_id` to `contentlet_version_info` PK; N-1 JOIN logic fails
- `Task221018CreateVariantFieldInMultiTree` — added `variant_id` to `multi_tree` PK

### Signals to watch for in code review

- `ALTER TABLE … DROP CONSTRAINT … PRIMARY KEY`
- `ALTER TABLE … ADD CONSTRAINT … PRIMARY KEY (new, columns)`
- Tasks that combine DROP PK \+ CREATE PK with a different column set

### Safer alternative

- Add the new discriminator column as nullable first (SAFE)
- Let N-1 and N coexist with the old PK
- Only restructure the PK in a later release when N-2 is outside the rollback window

---

## H-4 — New Content Type Field Type (Unrecognized by N-1)

**Risk level:** 🟠 HIGH

### Context

dotCMS field types are registered in `FieldTypeAPI` as named entries that map a string identifier to a Java implementation class. Field type identifiers are stored in the `field` DB table as strings. If N introduces a new field type and content is created using it, N-1 does not have the corresponding implementation class.

### Why it is unsafe

When N-1 reads a field row with an unknown `field_type` string:

- `FieldType.getClazz()` returns null or throws — field transformation fails
- `ESMappingUtilHelper.getMappingForField()` finds no mapper — ES indexing throws an exception at publish time
- REST API field serialization has no handler — responses return 500 errors for any content with that field type

There is **no unknown-field-type fallback**. The failure is hard and immediate for all content using the new type.

### Example from dotCMS

Any release that ships a new field type (e.g., a new widget, block editor field, or custom integration field) without a backward-compatible fallback strategy.

### Signals to watch for in code review

- A new class that extends `Field` and is registered in `FieldTypeAPI`
- Content types that use the new field type in default/seed data

### Safer alternative

- On N-1, register the new field type as an **unknown/passthrough** type so it is ignored gracefully rather than failing hard
- Introduce the field type in a prior release without assigning it to any content type (warming the registry before use)

---

## H-5 — Binary Storage Provider Change

**Risk level:** 🟠 HIGH

### Context

`FileStorageAPIImpl` abstracts binary file storage behind a pluggable `StoragePersistenceProvider` (File System, S3, DB, Redis). Before this abstraction existed, files were stored at fixed filesystem paths under `/dotserver/dotCMS/assets/{inode}/`.

### Why it is unsafe

If N switches the active storage provider (e.g., File System → S3) and binary assets are written to the new location, N-1 after rollback reverts to the File System provider and looks for files at the old filesystem path. **Those files do not exist there.** All binary asset requests (file downloads, image rendering, metadata reads) return 404 or throw exceptions.

Additionally, if N changes the metadata format stored in `storage_data` (governed by `getBinaryMetadataVersion()`), N-1's metadata reader may fail to parse or silently return empty metadata for all binary assets.

### Signals to watch for in code review

- Changes to `StoragePersistenceProvider` default configuration
- Changes to `FileMetadataAPI.getBinaryMetadataVersion()`
- Any `runonce` task that migrates files between storage locations

### Safer alternative

- Never change the default storage provider as part of a regular release — treat it as infrastructure configuration
- When adding a new storage provider, support reading from both old and new locations (fallback chain) before removing the old path

---

## H-6 — DROP PROCEDURE or DROP FUNCTION

**Risk level:** 🟠 HIGH

### Context

dotCMS uses stored procedures and database functions for operations like `dotFolderPath()`, `load_records_to_index`, and folder rename triggers. If N drops a procedure that N-1 calls, N-1 will throw a `SQLException` on any code path that invokes it.

### Why it is unsafe

Unlike table/column drops (which are called from Java code and can be inspected), stored procedure calls may be embedded in SQL strings that are harder to grep. Failures surface at runtime, not at startup.

### Examples from dotCMS history

- `Task05225RemoveLoadRecordsToIndex` — dropped `load_records_to_index` stored procedure
- `Task00922FixdotfolderpathMSSQL` — altered `dotFolderPath` function logic; N-1 relies on the previous behavior

### Signals to watch for in code review

- `DROP PROCEDURE`, `DROP FUNCTION`, `DROP TRIGGER`
- Tasks that ALTER a procedure's logic (N-1 calls the new logic — behavior differs)

---

## H-7 — Adding a NOT NULL Column Without a Default (on Large Tables)

**Risk level:** 🟠 HIGH

### Context

Adding a NOT NULL column to a large table requires a table rewrite in some databases (especially MySQL/MariaDB), which can lock the table for minutes. This is an operational risk during deployment, not a rollback risk per se.

However, if N adds a NOT NULL column and then N-1 tries to INSERT without providing that column, N-1 writes fail.

### Why it is low risk for rollback

N-1 does not know about the new column and will not attempt to INSERT it. The default value handles the constraint for any N-1 INSERT. As long as a sensible default is provided, N-1 continues to write correctly.

The risk escalates to MEDIUM/HIGH if:

- No default is provided (N-1 INSERTs fail)
- The column is added and then populated with a NOT NULL constraint in separate steps, but N-1 starts writing between those steps

---

---

# 🟡 MEDIUM

---

## M-1 — Non-Broadening Column Type Change

**Risk level:** 🟡 MEDIUM

### Context

Changing a column's data type is safe only if the new type is a strict superset of the old (e.g., `VARCHAR(100)` → `VARCHAR(500)`, `DATE` → `TIMESTAMP`). Any change that truncates data, alters semantics, or requires a `USING` conversion clause is unsafe.

### Why it is unsafe

N-1 writes values assuming the old type contract. If N changes the type in a way that transforms values (e.g., `USING` clause on PostgreSQL, implicit truncation in MySQL), data written by N-1 after rollback may fail type constraints, or data written by N may be misread by N-1.

### Examples from dotCMS history

- `Task00767FieldVariableValueTypeChange` — changed `variable_value` from `VARCHAR` to `LONGTEXT`
- `Task03515AlterPasswordColumnFromUserTable` — changed `password_` column type
- `Task210901UpdateDateTimezones` — changed all timestamps from `timestamp` to `timestamptz` with `USING` clause; time values are semantically shifted

### Signals to watch for in code review

- `ALTER TABLE … ALTER COLUMN … TYPE … USING …`
- `ALTER TABLE … MODIFY … LONGTEXT` (MySQL)
- `ALTER TABLE … ALTER COLUMN … NVARCHAR(MAX)` (MSSQL)

---

## M-2 — Push Publishing Bundle Format Change

**Risk level:** 🟡 MEDIUM

### Context

Push publishing bundles are XML archives sent from a sender to one or more receivers. The bundle XML schema is **not versioned** — there is no `<bundleVersion>` element. The MD5 signature verification that previously detected format mismatches has been disabled.

### Why it is unsafe

In a mixed-version environment (sender on N, any receiver still on N-1):

- If N adds required XML elements to the bundle, N-1 receivers fail parsing or silently drop the new data
- If N removes elements that N-1 expects, N-1 receivers may throw NullPointerException or import incomplete content
- There is no mechanism to detect the version mismatch at the receiver — failures may be silent

Bundles queued in `publishing_queue` at rollback time were generated by N. N-1 will attempt to send them to receivers — if format changed, receivers corrupt silently.

### Signals to watch for in code review

- Changes to any `*Bundler.java` class XML output structure
- New bundle handler (`*Handler.java`) that expects new XML elements
- Changes to `BundleXMLAsc` envelope structure

---

## M-3 — REST / GraphQL / Headless API Contract Change

**Risk level:** 🟡 MEDIUM

### Context

dotCMS exposes content through REST endpoints (`/api/v1/content`, `/api/v1/page/render`) and a GraphQL API. Headless front-ends, mobile apps, and integration partners build against these contracts. The Angular admin frontend also consumes internal REST APIs.

### Why it is unsafe

If N changes an API contract (field renamed, nesting changed, required parameter added, GraphQL type removed) and clients have deployed against N's contract, rolling back to N-1 breaks those clients:

- REST: Renamed fields in JSON responses cause client-side NullPointerExceptions or silent data loss
- GraphQL: Removed types or fields cause query validation errors — clients receive errors for previously valid queries
- Angular admin: If N's frontend (cached by CDN or browser) calls an endpoint that only exists in N, users get 404/500 errors after rollback

This risk exists **entirely outside the DB layer** — no migration task is involved.

### Signals to watch for in code review

- Renaming a JSON field in a REST response (use `@JsonProperty` to maintain backward compatibility)
- Removing a GraphQL type or field without a deprecation period
- Changing the structure of a pagination envelope or error response format
- Any API change where the Angular frontend is updated in the same PR to use the new shape

---

## M-4 — OSGi Plugin API Breakage

**Risk level:** 🟡 MEDIUM

### Context

dotCMS supports third-party OSGi plugins that depend on core Java interfaces. If N changes a public interface, method signature, or service registration contract, plugins compiled against N will not work on N-1 (or vice versa after rollback).

### Why it is unsafe

There is no semantic versioning enforced on internal dotCMS APIs exposed to OSGi plugins. A one-line change to an interface method signature (new parameter, changed return type) will cause:

- `NoSuchMethodError` or `ClassCastException` at plugin activation time
- Plugin activation failure — features provided by the plugin become unavailable
- In some cases, OSGi container instability that can affect core services

### Signals to watch for in code review

- Method signature changes to interfaces that are declared in `osgi/`\-exported packages
- Changes to `@Reference` or `@Provides` annotations on OSGi service components
- Removal of a service registration that plugins may be consuming

---
