# Issue Resolution Specification: A TextField stored in a numeric column makes the whole contentlet unindexable

**Feature Branch**: `37272-textfield-numeric-column`

**Created**: 2026-09-03

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: #37272

**Input**: User description: "37272"

## Problem Statement *(mandatory)*

A contentlet silently disappears from the search index. The save succeeds, the UI reports
nothing, and the reindex shows only an aggregate failure count with no field or content-type
attribution. The only trace is a WARN in the log.

The cause is a field declared as **Text** whose stored value is a Java `String`, but whose backing
storage column is numeric (`integer1`, `float3`, …). At index time the serialization branch is
chosen by the **storage column name** rather than by the value, so the String is handed to a number
formatter, which throws. The exception is then rethrown, and **the entire contentlet is discarded**
— not just the offending field.

**Severity / Impact**: Medium. It affects only contentlets whose value for such a field is a
String — but for those, the document is absent from the index after any reindex, permanently and
invisibly. Observed during ES→OpenSearch migration testing: of 370 contentlets refreshed, 2 were
lost this way (content types `RptTopHits` and `Trending`, field `viewWeekTotal`).

**Not a regression.** Both defects are present verbatim in commit `e8ef584ec9` — *"initial trunk
import"*, 2012-03-22, the oldest commit in the repository. The ES→OpenSearch work neither
introduced nor worsened it; the bulk refresh simply pushed enough documents through the same path
to make it visible.

## Reproduction *(mandatory)*

**Environment**: `dotcms/dotcms:trunk`, PostgreSQL 16, ~1.55 M contentlets, OpenSearch 1.3.20 +
3.4.0 in `PHASE_1_DUAL_WRITE_ES_READS`. Not phase- or engine-specific — the defect is in the
contentlet→document mapper shared by both engines, so it reproduces on Elasticsearch-only installs.

**The field modelling is legitimate and supported.** `TextField.acceptedDataTypes()` includes
`INTEGER` and `FLOAT`, so "Text field, Value type: Whole Number" is a valid UI choice. dotCMS ships
content types modelled this way: `htmlpageasset.sortOrder` and `Vanity URL.order` are both
`ImmutableTextField` + `DataTypes.INTEGER` + `indexed(true)`. The modelling is not the defect.

**What actually decides the failure is the Java class of the value at index time**:

| stored `contentlet_as_json` | value in the contentlet map | outcome |
|---|---|---|
| `{"type":"Long","value":54}` | `Long` | indexed correctly (padded `_dotraw`) |
| `{"type":"Text","value":"54"}` | `String` | 💥 whole contentlet lost |

`DecimalFormat.format(Object)` accepts only `Number`; with a String it throws **always**, even when
the content is numeric. This is why `htmlpageasset.sortOrder` never fails (the core sets it via
`setLongProperty`) and `viewWeekTotal` fails for 100% of its contentlets.

**Steps to Reproduce (A — the defect itself, deterministic; this is the Red test)**:

1. Create a content type with a **Text** field, Value type **Whole Number**, indexed.
2. Build the `Contentlet` and set the value raw, bypassing `setContentletProperty`:
   `contentlet.setStringProperty("<fieldVar>", "54");`
3. Call `APILocator.getContentletMappingAPI().toMap(contentlet)`.

**Steps to Reproduce (B — the production data state, for end-to-end QA)**:

1. Write the String value into the stored JSON directly:
   ```sql
   UPDATE contentlet
   SET contentlet_as_json = jsonb_set(contentlet_as_json,
         '{fields,<fieldVar>}', '{"type": "Text", "value": "54"}'::jsonb)
   WHERE inode = '<inode>';
   ```
2. Flush caches, then reindex that contentlet.

**Expected Behavior**: The contentlet is indexed. The field is serialized in a way that suits its
value, and every other field of the document is indexed regardless.

**Actual Behavior**: The contentlet is absent from the index. The log shows, at WARN level:

```
WARN  business.ESMappingAPIImpl - Error indexing field: viewWeekTotal of contentlet: <inode>
java.lang.IllegalArgumentException: Cannot format given Object as a Number
    at java.base/java.text.DecimalFormat.format(DecimalFormat.java:584)
    at com.dotcms.content.elasticsearch.business.ESMappingAPIImpl.loadFields(...)
    at com.dotcms.content.elasticsearch.business.ESMappingAPIImpl.toMap(...)
```

**Reproducibility**: Always, for every contentlet holding a String value in such a field.

## Scope of Investigation *(mandatory)*

- **Affected area**: Search indexing — the contentlet→index-document mapping, plus the failure
  reporting surfaced to the operator during a reindex or bulk refresh.
- **Suspected surface**: `com.dotcms.content.elasticsearch.business.ESMappingAPIImpl.loadFields`
  (`:990`; branch at `:1113-1116`, throwing line `:1118`, WARN+rethrow at `:1143-1145`). Despite
  the `elasticsearch` package name it is the **single shared mapper for both engines** —
  `com.dotcms.content.index.opensearch.OSBulkHelper:250` calls the same `toMap()`. It reads legacy
  field metadata (`com.dotmarketing.portlets.structure.model.Field`), so the plan must confirm
  legacy impact on the field model.
- **Related known decisions**: The ES→OpenSearch migration — the mapper is shared, so a change to
  what is emitted per field changes both engines' documents at once. The plan formally consults
  `dotCMS/platform-adrs`.

## Root-Cause Hypothesis

Four layers make a decision about such a field, and three of them key off the storage column:

| layer | keys off | behavior for `integer1` |
|---|---|---|
| write (`FieldHandlerStrategyFactory`) | `dbColumn()` | `integerStrategy` coerces the value to `Long` |
| read (`ContentletJsonAPIImpl.getValue`, `:444`) | **nothing** | returns the value exactly as the JSON declares it |
| index (`ESMappingAPIImpl.loadFields`) | `getFieldContentlet()` | numeric branch → `DecimalFormat.format(value)` |
| index mapping (`ESMappingUtilHelper:429`) | `dataType()` | maps the field as `long` |

The read layer validates nothing. The write path guarantees a `Long`, but a value already present
in the JSON enters unchecked. Two independent defects then compound:

1. **Wrong discriminator.** `loadFields` picks the branch by column name. `"integer1"` starts with
   `integer`, so a String reaches `numFormatter.format(valueObj)` and throws.

2. **A single bad field kills the whole document.** The per-field `catch` logs at **WARN** — the
   level that says "recoverable, carry on" — and then rethrows as `DotDataException`, aborting the
   contentlet. The date branches in the same method do the opposite and degrade to `toString()`.

Defect 2 is what turns a mis-typed field into a lost document; it would have contained defect 1 on
its own.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- **Handle the parse failure at index time** for a Text field that carries a numeric value,
  converting best-effort instead of assuming the stored value already matches the column:
  - A shared best-effort conversion helper (alongside the existing `NumberUtil.toInt` /
    `NumberUtil.toLong` / `NumberUtil.pad`, following the same `Supplier<T> defaultOne` idiom).
  - **Index (`loadFields`)**: convert the value; if it converts, the numeric branch behaves exactly
    as today (a stored `"54"` produces a document identical to a correctly-stored `54`, padding
    included). If it does not convert, emit `0` under the numeric key — keeping the document
    consistent with the field's `long` mapping — and the **original text** under `_dotraw`, so the
    real value stays visible and searchable. Log a WARN naming the field and content type.
  Scope is the index path only: a Text field that carries a numeric value must survive the parse,
  whichever way the value happens to be stored.
- Make a per-field serialization failure **non-fatal to the document**: the field degrades (or is
  skipped) and the rest of the contentlet is still indexed, consistent with the date branches in
  the same method.
- Make the failure **actionable**: the report names the field (and content type), and the log level
  matches the outcome — WARN when the document is still indexed, ERROR when it is aborted.

**Explicitly out of scope / non-goals**:

- **Changing the index mapping generator** so it keys off the declared field type instead of
  `dataType()`. That changes the mapping of existing indices, forces a reindex, and is
  rollback-unsafe.
- **Repairing existing mis-typed stored values** (rewriting `contentlet_as_json`). The fix must
  make these contentlets index correctly *as they are*.
- **Validating field-type/storage-column consistency at content-type save time** (the issue's
  "consider" bullet). That validation already exists and already says this modelling is legal:
  `FieldFactoryImpl:478` rejects a field whose `dataType()` is not in `acceptedDataTypes()`, and
  `TextField.acceptedDataTypes()` deliberately includes `INTEGER` and `FLOAT`. Forbidding the
  combination means removing them from that list, which would break dotCMS's own built-in content
  types at bootstrap (`PageContentType:125-127`, `VanityUrlContentType:85-88`,
  `ContentTypeInitializer:123` all build `ImmutableTextField` + `DataTypes.INTEGER`). The
  consistency worth enforcing is **value vs. data type**, not field type vs. data type — and the
  preventive lever for that is the serialization layer, also out of scope (next bullet).
- **Changing how contentlets are serialized** — making `Field.fieldValue()` pick the JSON value
  type from the field's `dataType()` instead of from `value instanceof`. That is the preventive
  counterpart to this fix and would also close the `ImportStarterUtil` route (the starter import
  saves through `ESContentFactoryImpl`, which re-serializes every contentlet via
  `contentletJsonAPI.toJson(...)`, `:1869-1872`). It is deliberately **out of scope**: it changes
  every save of every contentlet in the system, which is a different blast radius and deserves its
  own Red/Green cycle and its own review. Recorded under Regression Risk → *Identified risk*.
- **Hardening `ImportStarterUtil` directly.** Same reasoning — named as a known gap, not fixed
  here.
- **Backfilling the already-lost documents.** A normal reindex after the fix recovers them.
- Rewriting `loadFields` or the legacy `Field` model wholesale. Progressive enhancement only.
- Per-document / per-engine failure reporting in the reindex UI beyond naming the field — that is
  the companion issue referenced in #37272.

## Regression Risk *(mandatory)*

- **Blast radius**: `loadFields` runs for **every field of every contentlet on every index write** —
  the hottest path in indexing, shared by both engines. Built-in content types use exactly this
  field shape (`htmlpageasset.sortOrder`, `Vanity URL.order`), and their `_dotraw` is a
  zero-padded string (`0000000000000000054.000000000000000000`) precisely so that lexicographic
  sorting equals numeric sorting. **Routing those through the text branch would break page
  ordering on every installation** — hence the value-based discriminator rather than a
  type-based one.
- **Backward compatibility**: The emitted values feed index mappings and existing queries (range,
  sort, exact match via `_dotraw`, VTL/GraphQL/Elastic search by field). Changing the *type* of an
  emitted value is potentially rollback-unsafe during a rolling deploy. Making a previously-fatal
  failure non-fatal is safe in that direction — it can only add documents that were missing.
- **Data considerations**: Documents lost to this defect reappear on the next reindex. No schema or
  DB migration. Note the index mapping for such a field is `long`: emitting a non-numeric String
  under the numeric key would produce a mapper parsing exception at the engine, i.e. the same lost
  document one layer down — so the text-degradation path must not write the numeric key.

### Identified risk: unvalidated ingestion path (`ImportStarterUtil`)

**No supported user-facing path on trunk can create the bad value.** Every save funnels through
`ContentletAPI.setContentletProperty` → `integerStrategy`, which coerces `"54"` → `54L` and rejects
a non-numeric String with `DotNumericFieldException`. This was verified for:

| path | where | result |
|---|---|---|
| New content editor (REST / workflow fire) | `MapToContentletPopulator:279` | coerced |
| Legacy editor (Struts/dojo) | `ContentletWebAPIImpl`, `ContentletAjax` | coerced |
| Content Import portlet **and** the new import job | `ImportUtil:3460` (both `ImportContentletsAction` and `ImportContentletsProcessor` funnel here) | coerced; non-numeric values reject the line |

`MapToContentletPopulator` has exactly one `setStringProperty` bypass (`:259`) and it applies only
to Category fields.

**The starter / site-export import does not go through the API at all:**

```java
// ImportStarterUtil.java:894
APILocator.getContentletJsonAPI().toMutableContentlet(cont);   // JSON → map, no coercion
// ImportStarterUtil.java:782
FactoryLocator.getContentletFactory().save((Contentlet) obj);  // factory directly — no checkin, no validation
```

`toMutableContentlet` (`ContentletJsonAPIImpl:244`) reuses the same `getValue()` that returns the
value exactly as the JSON declares it. A starter carrying `{"type":"Text","value":"54"}` for a
field now backed by a numeric column is written to the database **verbatim**, with nothing
reconciling the JSON value type against the field's `dataType`.

This is the most plausible provenance of the two observed rows: a value stored as a String by an
older instance, exported, and re-imported into trunk. It also explains why the defect cannot be
reproduced through the UI, and why a dataset copy rebuilt by re-saving content through the API no
longer contains the case.

**Risk to this fix**: none directly — the `loadFields` change makes such data index correctly
regardless of how it arrived. **Risk to the system**: `ImportStarterUtil` remains a route by which
values whose type contradicts their field's storage column enter the database unvalidated. Named
here so the plan can decide whether it warrants separate treatment; **not fixed by this change**.

**To confirm the provenance** on the instance where it was observed:

```sql
SELECT inode, contentlet_as_json -> 'fields' -> 'viewWeekTotal' AS stored, integer1
FROM contentlet WHERE inode = '<inode>';
```

Install-wide detector (any numeric column whose stored JSON type is not the matching numeric type):

```sql
SELECT st.velocity_var_name AS ct, f.velocity_var_name AS field,
       f.field_type, f.field_contentlet, kv.value ->> 'type' AS json_type, count(*)
FROM contentlet c
JOIN structure st ON st.inode = c.structure_inode
JOIN field     f  ON f.structure_inode = st.inode
CROSS JOIN LATERAL jsonb_each(c.contentlet_as_json -> 'fields') AS kv(key, value)
WHERE kv.key = f.velocity_var_name
  AND ( (f.field_contentlet LIKE 'integer%' AND kv.value ->> 'type' <> 'Long')
     OR (f.field_contentlet LIKE 'float%'   AND kv.value ->> 'type' <> 'Float') )
GROUP BY 1,2,3,4,5 ORDER BY 6 DESC;
```

(Contentlets with a null `contentlet_as_json` read from the legacy columns and do not suffer this
defect; `jsonb_each(NULL)` drops them from the result silently.)

## Acceptance & Verification *(mandatory)*

- **AC-001**: A contentlet holding a String value in a Text field backed by a numeric column is
  indexed successfully, on both the Elasticsearch and OpenSearch write paths.
- **AC-002**: When a single field cannot be serialized, the remaining fields of that contentlet are
  still indexed; the document is present in the index rather than absent.
- **AC-003**: The failure report/log for an unserializable field names the **field** (and content
  type), not only the contentlet inode.
- **AC-004**: The log level matches the outcome — WARN when the document is still indexed, ERROR
  when it is aborted. No WARN-then-rethrow.
- **AC-005 (regression, critical)**: A field of the same shape whose value **is** a `Number` —
  `htmlpageasset.sortOrder`, `Vanity URL.order` — produces a byte-identical index document,
  including the zero-padded `_dotraw`. Page ordering by `sortOrder` is unchanged.
- **AC-006 (regression)**: Correctly-modelled `Integer`, `Float`/`Decimal`, `Boolean`, `Date`,
  `DateTime`, `Checkbox`/`Multi-Select`, `Key-Value`, `Tag`, `Category` and `Relationship` fields
  produce byte-identical index documents, including the unique-field SHA-256 entry.
- **AC-007**: A value that cannot be converted to a number is indexed as `0` under the numeric key
  (so it cannot trigger a mapper parsing exception against the field's `long` mapping) while
  `_dotraw` carries the original text, and a WARN names the field and content type.
- **AC-008**: A stored String that *is* numeric (`"54"`) produces an index document identical to
  the same contentlet stored as a number — including the zero-padded `_dotraw`.

- **Verification method**:
  - **Unit** — a focused test over `loadFields` across (declared type × storage column × value
    class), asserting the emitted map entries. The `TextField`-on-`integer1`-with-String case must
    fail first (Red).
  - **Integration** — `dotcms-integration`, a `*Test` class registered in the matching
    `@SuiteClasses` suite: content type with a Text field on a numeric column, contentlet carrying
    a String value, index it, assert the document is retrievable and the other fields populated.
    Run with `./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=<TestClass>`.
  - **Regression** — assert `htmlpageasset.sortOrder` still emits the zero-padded `_dotraw`.
  - **Manual** — apply reproduction B on a seeded instance, run
    `POST /api/v1/content/_bulkrefresh`, confirm zero failures for the affected content types.

## Assumptions

- A Text field on a numeric column is a **legitimate existing state** to be tolerated, not an
  invalid state to be rejected. The fix makes it index correctly rather than unrepresentable.
- The value in such a field may be either a `Number` or a `String`, and both must index.
- No customer data appears in this spec: content-type and field names are the internal ones from
  the migration-testing instance named in the issue.

## Resolved Decisions

- **C-1 — how the value is emitted.** Convert best-effort rather than discriminating by column or
  by declared type. A type-based rule would route `htmlpageasset.sortOrder` through the text branch
  and break page ordering on every installation.
- **C-2 — where consistency is enforced.** Not in this fix. Content-type-save-time validation is
  the wrong lever (see non-goals). The right preventive lever is the serialization layer
  (`Field.fieldValue()` respecting `dataType()`), but that touches every save in the system and is
  tracked separately under *Identified risk*. **This fix handles the parse failure where it
  surfaces — at index time — so that a Text field carrying a numeric value indexes correctly no
  matter how the value was stored.**
- **Unconvertible values index as `0`** under the numeric key, with the original text preserved in
  `_dotraw` and a WARN naming the field and content type.
