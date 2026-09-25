# Data Model: UVE headless REST-provenance fix

This is frontend-only, in-memory NgRx Signal Store state — no database, no ES/OpenSearch
mapping, no REST contract. The only "entity" touched is the store's page-asset state.

## `PageLoadingConfigState.pageAssetResponse`

**Current shape** (`store/features/page/withPage.ts:29-39`):

```typescript
export interface PageLoadingConfigState {
    isClientReady: boolean;
    requestMetadata: { query: string; variables: Record<string, string> } | null;
    pageAssetResponse: {
        pageAsset: DotCMSPageAsset;
        content?: Record<string, unknown>;
    } | null;
}
```

**Proposed shape** — add one field:

```typescript
export type PageAssetSource = 'rest' | 'graphql';

export interface PageLoadingConfigState {
    isClientReady: boolean;
    requestMetadata: { query: string; variables: Record<string, string> } | null;
    pageAssetResponse: {
        pageAsset: DotCMSPageAsset;
        content?: Record<string, unknown>;
        source?: PageAssetSource;
    } | null;
}
```

### Field: `source`

- **Type**: `'rest' | 'graphql'`
- **Optional**, like `content` — a payload can omit it. Unlike `content`, the *read side*
  (the push gate) treats "not `'graphql'`" — including `undefined` — as "don't push," so an
  unset `source` is never mistaken for a safe-to-push value.
- **Set by**: `setPageAsset` (`withPage.ts`), the single writer of `pageAssetResponse`.
- **Merge rule** (mirrors the existing `content` merge in `setPageAsset`): if the caller's
  payload includes `source`, use it; otherwise inherit the current value. This lets
  mutate-in-place callers (optimistic edits, layout updates) patch the asset without having to
  know or restate its provenance. If there is no current value to inherit (no prior
  `pageAssetResponse`), `source` stays `undefined` — the code does not invent a value it
  doesn't actually know, and `undefined` is exactly as "don't push" as `'rest'` would be.
- **Never inferred from `requestMetadata`, `isClientReady`, or `'content' in payload`** at the
  read side — those are exactly the signals the current bug conflates with provenance. `source`
  is written explicitly by whichever fetch resolved, once, at the point of resolution.

### Where `source` is set explicitly (fetch call sites — 7 total)

| Call site | Branch | `source` |
|---|---|---|
| `withPageApi.ts` `pageLoad` (:293) | `graphQLContent !== undefined` | `'graphql'` |
| `withPageApi.ts` `pageLoad` (:293) | else | `'rest'` |
| `withPageApi.ts` `pageReload` (:348, :357) | `!deps.requestMetadata()` taken → REST branch | `'rest'` |
| `withPageApi.ts` `pageReload` (:348, :357) | GraphQL branch | `'graphql'` |
| `withPageApi.ts` `editorSave` (:404) | REST branch | `'rest'` |
| `withPageApi.ts` `editorSave` (:411) | GraphQL branch | `'graphql'` |
| `withPageApi.ts` `updateRows` (:505) | REST branch | `'rest'` |
| `withPageApi.ts` `updateRows` (:512) | GraphQL branch | `'graphql'` |
| `withWorkflow.ts` `reloadPageAfterLockChange` (:224, :234) | `!requestMetadata \|\| !requestWithParams` → REST | `'rest'` |
| `withWorkflow.ts` `reloadPageAfterLockChange` (:224, :234) | GraphQL branch | `'graphql'` |

### Where `source` is omitted (inherited — mutate-in-place call sites)

| Call site | Why no explicit source |
|---|---|
| `uve-optimistic-save.service.ts` `updateIframeOptimistically` (:49) | Clones and patches the *existing* asset's properties; not a new fetch, so it keeps whatever source the asset already had |
| `withLayout.ts` `updateLayout` (:67) | Same pattern — patches `layout` on the existing asset in place |

### Derived read: the headless push gate

No new state needed for the gate itself — it reads `pageAssetResponse.source` (via the
`pageAsset()` computed, which already spreads `pageAssetResponse` — see `withPage.ts:239-251`)
instead of `requestMetadata`. Conceptually:

```typescript
const canPushToHeadlessClient = (): boolean =>
    pageType() === PageType.TRADITIONAL || pageAsset()?.source === 'graphql';
```

This single predicate replaces `hasClientQuery` at all 4 call sites that decide whether to push
(`$handleReloadContentEffect`, `saveStyleEditor`'s rollback, `saveQuickEditFields`'s rollback,
`updateIframeOptimistically`) — see `plan.md` Findings #4-5 for exact locations.

## State transitions (maps to spec.md's state matrix)

| Transition | `pageAssetResponse.source` after |
|---|---|
| Initial page load, no headless client registered | `'rest'` |
| `CLIENT_READY` registers a query; `pageReload()` GraphQL fetch not yet resolved | still `'rest'` (unchanged until the fetch's `tap`/`catchError` runs) |
| GraphQL fetch resolves | `'graphql'` |
| GraphQL fetch aborts/fails (`catchError`) | **unchanged** — stays whatever it was before the failed attempt (today: stays `'rest'` if this was the first fetch since registration; this is exactly what makes rows 2 and 3 of the state matrix indistinguishable under the old `requestMetadata`-based gate, and exactly what `source` now distinguishes correctly since it was never optimistically flipped) |
| Optimistic edit (style/quick-edit) or layout change | inherited from current `source` (no transition) |
| Rollback after failed save | inherited from the restored history snapshot's `source` (history already deep-clones the full `pageAssetResponse` object per `withHistory`'s `deepClone: true`, so `source` survives undo/redo automatically — no change needed in `withHistory.ts`) |
