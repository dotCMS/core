# Contract change: `putToIndex` partial bulk failure

`putToIndex` is declared on the public interface `ContentletIndexAPI` (`:159`) and implemented
by the router (`ContentletIndexAPIImpl:2414`) and by each provider
(`ContentletIndexOperationsES:197`, `ContentletIndexOperationsOS:280`). Out-of-tree callers —
plugins, OSGi bundles — can reach the interface method, so this is a contract change and not
an internal refactor.

## Before

```java
void putToIndex(IndexBulkRequest bulkRequest);
```

Throws only when the bulk **call itself** fails (transport error, client illegal state). A
response carrying **per-item** failures — `EsRejectedExecutionException` from a saturated write
queue, an unavailable shard, a version conflict — is logged and the method returns normally.
The caller cannot distinguish a fully applied batch from one where every item was rejected.

## After

Throws when the bulk call fails **or** when the response reports per-item failures. The caller
can no longer mistake a partially or wholly rejected batch for a successful one.

Unchanged:

- An empty batch is a no-op and returns without contacting the index.
- The exception type stays `DotRuntimeException` / `DotIndexException` — no new checked
  exception, no signature change, so this is source- and binary-compatible.

## Behavior per migration phase

The router already isolates the shadow provider, so the new failure surfaces exactly where
ADR-0009 requires and nowhere else. **The escalation is implemented in the providers, not in
the router** — putting it in the router would break the phase 1–2 guarantee below.

| Phase | Providers | Partial failure in ES | Partial failure in OS |
|-------|-----------|-----------------------|-----------------------|
| 0 | ES only | propagates | n/a |
| 1, 2 | ES primary, OS shadow | propagates | logged, swallowed by the router (`ContentletIndexAPIImpl:2429-2435`) — **ADR-0009** |
| 3 | OS only | n/a | propagates |

## Impact on callers

In-tree callers of the router method, all in `ContentletIndexAPIImpl`:

| Site | Method | Effect of the change |
|------|--------|----------------------|
| `:2364` | `indexContentListNow` (`FORCE`) | A rejected add now raises instead of being silently dropped. |
| `:2372` | `indexContentListWaitFor` (`WAIT_FOR`) | Same. |
| `:2379` | `indexContentListDefer` (`DEFER`) | Same. Reached from the journal drain, so the entry is marked failed and retried — the desired outcome. |
| `:3167` | `removeContentAndProcessDependencies` | The delete loss point (L3). With the journal entry in place the removal is retried. |

Out-of-tree callers cannot be enumerated from this repository. A caller that today relies on
`putToIndex` returning normally after a partial failure will begin seeing an exception.

## Migration note for the release

This is a behavior change that can look like a regression and is not one. An environment that
has been silently losing index writes will start surfacing errors at the moment of the write
rather than as unexplained index drift weeks later. The errors were always happening; only
their visibility changed.

Operators seeing new `putToIndex` failures after upgrading should treat them as a pre-existing
condition now made visible — typically index write-queue saturation — and not as a fault
introduced by this release.
