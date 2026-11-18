# Contract: bundle-scoped queue-element delete

**Surface**: Java API on `com.dotcms.publisher.business.PublisherAPI` (abstract class) and its
`PublisherAPIImpl`. Not a REST endpoint — no `@Schema`, no `openapi.yaml` regeneration.

This is a **plugin-facing** contract: `PublisherAPI` is reachable through `PublisherAPI.getInstance()`
and third-party plugins compile against it. That is why the change is additive.

## New method

```java
/**
 * Deletes the queue elements for the given asset within a single bundle, leaving the same
 * asset queued in other bundles untouched. Deletes the bundle's publish-audit status only
 * if this removal empties the bundle's queue.
 */
void deleteElementFromPublishQueueTableAndAuditStatus(String identifier, String bundleId)
        throws DotPublisherException;
```

### Behavior

| Input | Expected |
|---|---|
| Asset queued in bundle A and bundle B; called with (asset, A) | Only A's row(s) for that asset are removed. B still lists and will publish the asset. **(AC-004)** |
| The delete empties bundle A's queue | A's `publishing_queue_audit` row is deleted. **(AC-005)** |
| Bundle A still has other queue elements | A's audit status is left in place. **(AC-005)** |
| `bundleId` is null or blank | **Throw / no-op — never widen to a table-wide delete.** `bundle_id` is nullable with no FK, so a null must not silently degrade into `WHERE asset = ?`. |
| Asset not present in that bundle | No rows affected, no exception (matches the existing method's tolerance). |

### SQL

```sql
DELETE FROM publishing_queue WHERE asset = ? AND bundle_id = ?
```

Sequential scan — `publishing_queue` has no index on either column today (see `data-model.md`).
Unchanged from the existing delete's cost profile.

### Transactionality

Follow the existing sibling: `deleteElementFromPublishQueueTable` is annotated `@WrapInTransaction`.
The new delete plus its conditional audit-status removal must be atomic — a delete that succeeds
while the audit cleanup fails would leave an orphaned audit row for an empty bundle.

## Superseded method (retained)

```java
@Deprecated(since = "Sep 9th, 26", forRemoval = true)
void deleteElementFromPublishQueueTableAndAuditStatus(String identifier)
        throws DotPublisherException;
```

- **Semantics unchanged**: still `DELETE FROM publishing_queue WHERE asset = ?` across all bundles.
- **Retained** so plugins compiled against the current API keep working. No removal date, per the
  ADR-0020 precedent.
- **In-repo callers to migrate**: `view_publish_queue_list.jsp:84` (this fix migrates it) and
  `PublisherAPIImplTest.java:111` (keep, as coverage of the deprecated path).

## Untouched

`deleteElementsFromPublishQueueTableAndAuditStatus(String bundleId)` — the whole-bundle delete
behind `&deleteBundle=`. No signature or behavior change.

## Client contract (JSP → server)

The Pending tab's asset checkbox `value` becomes `<assetId>$<operation>$<bundleId>`.

- `deleteQueue()` must keep deriving the asset id as `value.split("$")[0]` — unchanged.
- The request must identify **both** asset and bundle. A request naming an asset without its
  bundle fails **AC-002** and must not be served by the deprecated bundle-agnostic path.
