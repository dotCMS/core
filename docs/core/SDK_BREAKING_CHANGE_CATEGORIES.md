# SDK Breaking Change Categories — Developer Reference

>**Purpose:** Help developers (and the automated PR checker) decide whether a change breaks
>compatibility with already-published `@dotcms/client`, `@dotcms/react`, `@dotcms/angular`, and
>`@dotcms/uve` SDK versions — i.e. whether `MinSdkVersion.VALUE` needs to move.
>
>**Rule of thumb:** a change is SDK-breaking if an SDK built against the server contract *before*
>this change would misbehave (throw, silently drop data, or misinterpret a message) against the
>server *after* this change — with no fallback path in the SDK for the old shape.

Unlike [`ROLLBACK_UNSAFE_CATEGORIES.md`](ROLLBACK_UNSAFE_CATEGORIES.md), this reference doesn't use a
CRITICAL/HIGH/MEDIUM/LOW risk scale — this doc answers a single binary question for a single
purpose (does `MinSdkVersion.VALUE` need to move), not a graded operational-risk assessment. Each
category below is either **Breaking** or **Conditionally breaking** (breaks only if a specific
additional condition holds, called out explicitly).

---

## Quick Reference — Decision Card

```
Is my change a...

Removed/renamed GraphQL type or field reachable            → Breaking       (G-1)
  via graphql.page / graphql.content?
New REQUIRED argument on an existing GraphQL field/query    → Breaking       (G-2)
  used by the page API's query builder?
Changed GraphQL structured-error extensions.code semantics  → Breaking       (G-3)
  (NOT_FOUND, PERMISSION_DENIED) that page-api.ts branches on?
Renamed/removed JSON field in /api/v1/nav or                → Breaking       (R-1)
  /api/v1/content response shapes the SDK types model?
Removed or renamed an inbound postMessage name the SDK      → Breaking       (U-1)
  listens for (__DOTCMS_UVE_EVENT__)?
Changed the payload shape of an outbound postMessage the    → Breaking       (U-2)
  editor consumes (DotCMSUVEAction) without a compat shape?
Changed X-DotCMS-Version / X-DotCMS-Min-SDK header names,   → Breaking       (H-1)
  casing assumptions, or the version-comparison contract?

New OPTIONAL GraphQL field/query, additive REST field,      → ✅ Not breaking
  new postMessage type old SDKs simply never send/receive,
  internal refactor with no wire-format change, admin-UI
  (dotcms-ui) only change
```

---

# G — GraphQL Page/Content API Surface

The Page API (`core-web/libs/sdk/client/src/lib/client/page/page-api.ts`) builds and sends GraphQL
queries via `buildPageQuery`/`buildQuery`, and the Content API's collection builder does the same for
`XCollection(...)`-style queries. Both are SDK-authored queries sent to the server's GraphQL schema.

## G-1 — Removing or Renaming a Reachable GraphQL Type/Field

**Direction:** Breaking

### Context

SDK consumers write GraphQL fragments referencing schema types/fields directly in their own code
(see `PageClient.get()`'s `graphql.page` / `graphql.content` / `graphql.fragments` parameters). These
fragments are compiled into the request the SDK sends — the SDK itself doesn't know the schema
shape ahead of time; it trusts the server contract.

### Why it breaks compatibility

If a field or type an already-deployed customer application's fragment references is removed or
renamed, the customer's next request fails GraphQL query validation entirely — `response.data` comes
back `null` (see `page-api.ts`'s "BAD QUERY" branch), and the whole page load throws a `DotErrorPage`.
There's no partial degradation; the entire query fails.

### Signals to watch for in code review

- A field or type removed from a GraphQL schema definition that's part of the public content/page
  surface (not an internal-only type)
- A field renamed without a `@deprecated`-and-kept-working transition period

### Safer alternative

- Add the new field/type alongside the old one; deprecate the old one for at least one release cycle
  before removing it
- If a rename is unavoidable, keep the old field as an alias resolving to the same value

---

## G-2 — New Required Argument on an Existing GraphQL Field/Query

**Direction:** Breaking

### Context

`buildPageQuery`/`buildQuery` in `page-api.ts` construct queries with a fixed, SDK-known set of
arguments (e.g. the page query's `url`, `languageId`, `mode`, `personaId`, etc., built from
`DotCMSPageRequestParams`).

### Why it breaks compatibility

If the server adds a new **required** (non-nullable, no default) argument to a field the SDK already
queries, every request from an SDK built before that change omits it — GraphQL query validation
rejects the request outright.

### Signals to watch for in code review

- A new argument added to an existing GraphQL field/query definition with no default value
- Any change to the page or content query root that isn't purely additive-and-optional

### Safer alternative

- New arguments must be optional with a server-side default that preserves today's behavior

---

## G-3 — Structured GraphQL Error Contract Change

**Direction:** Breaking

### Context

`page-api.ts` branches explicitly on `error.extensions?.code` (`NOT_FOUND` → 404 + specific message,
`PERMISSION_DENIED` → 403 + specific message, anything else → generic 400) to build a typed
`DotErrorPage`. This is a contract the SDK actively parses, not just logs.

### Why it breaks compatibility

If the server stops setting `extensions.code`, renames the code strings, or changes when they're
emitted, the SDK's error classification silently falls through to the generic/wrong branch — a
"page not found" could get misreported as a generic 400, breaking any consumer code that branches on
`DotErrorPage`'s `status`/`code`.

### Signals to watch for in code review

- Changes to how GraphQL resolvers set `extensions.code` on errors
- Renaming or removing the `NOT_FOUND` / `PERMISSION_DENIED` extension codes

### Safer alternative

- Treat `extensions.code` values as a versioned public contract — add new codes freely, never rename
  or remove existing ones

---

# R — REST Response Shape Changes

## R-1 — REST Response Field Removed or Renamed

**Direction:** Breaking

### Context

`NavigationClient.get()` (`navigation-api.ts`) calls `GET {dotcmsUrl}/api/v1/nav{path}` and reads the
response as `{ entity: DotCMSNavigationItem[] }` directly off `response.entity` — no defensive
parsing. The Content API's collection builder similarly expects a fixed response envelope.

### Why it breaks compatibility

A renamed or removed field in the REST response (e.g. `entity` renamed, or a `DotCMSNavigationItem`
field like `href`/`title` renamed) means the SDK either reads `undefined` silently (TypeScript types
lie about runtime shape) or the whole navigation tree fails to render, with no clear error since
there's no schema validation at the boundary.

### Signals to watch for in code review

- Renaming a JSON field in `/api/v1/nav`, `/api/v1/content`, or `/api/v1/page/*` responses
- Changing the response envelope structure (e.g. `entity` wrapper removed or restructured)

### Safer alternative

- Add new fields additively; never rename or remove a field already returned in these endpoints
  without a deprecation window
- Prefer `@JsonProperty` aliasing on the Java side so both old and new field names resolve

---

# U — UVE/Editor `postMessage` Protocol

The Universal Visual Editor communicates with the SDK via `window.postMessage` in both directions.
Inbound (editor → SDK) messages are dispatched by name via `__DOTCMS_UVE_EVENT__` constants and
consumed in `core-web/libs/sdk/uve/src/internal/events.ts` (e.g. `onContentChanges`, `onPageReload`,
`onAutoBounds`, `onIframeScroll`, `onScrollToSection`, `onContentletClicked` — matching message names
`UVE_SET_PAGE_DATA`, `UVE_RELOAD_PAGE`, `UVE_FLUSH_BOUNDS`, `UVE_SCROLL_INSIDE_IFRAME`,
`UVE_SCROLL_TO_SECTION`, `UVE_SELECTION_CLEARED`). Outbound (SDK → editor) messages are named via the
`DotCMSUVEAction` enum in `core-web/libs/sdk/types/src/lib/editor/public.ts` (e.g. `set-url`,
`set-bounds`, `set-contentlet`, `set-selected-contentlet`, `scroll`).

## U-1 — Inbound Message Name or Payload Removed/Renamed

**Direction:** Breaking

### Context

An old SDK's `events.ts` listeners match on an exact `event.data.name` string. There is no version
negotiation in the protocol itself — it's a bare string match.

### Why it breaks compatibility

If the editor stops sending a message name an already-deployed SDK listens for (or renames it, or
changes the payload shape a listener destructures), that SDK's corresponding feature goes silently
dead — no error, the callback just never fires (e.g. `onAutoBounds`'s drag-flush channel, or
`onScrollToSection`'s section-jump handling).

### Signals to watch for in code review

- A message name constant in `__DOTCMS_UVE_EVENT__` removed or renamed
- A payload shape change for an existing message (e.g. `event.data.sectionIndex` renamed or
  restructured in `onScrollToSection`)

### Safer alternative

- Add new message names/payloads alongside old ones; dual-emit both shapes for at least one release
  cycle before removing the old one

---

## U-2 — Outbound Message Payload Shape Change

**Direction:** Breaking

### Context

The editor's own listeners parse `DotCMSUVEAction` payloads sent by the SDK (e.g. `set-bounds`,
`set-selected-contentlet`). An older editor session (cached admin UI, or a customer running an
older dotCMS version against a newer SDK in a mixed-version scenario) expects the payload shape it
was built against.

### Why it breaks compatibility

If the SDK-side payload shape changes (fields renamed/removed on the object sent via
`window.parent.postMessage`) without the editor also updating in lockstep, the editor either
misreads the payload or ignores it — selection overlays, bounds, or hover state stop updating
correctly with no visible error.

### Signals to watch for in code review

- A payload shape change on any `DotCMSUVEAction` message (`set-bounds`, `set-contentlet`,
  `set-selected-contentlet`, `set-url`, `scroll`, etc.)
- Renaming a `DotCMSUVEAction` enum value's string (the wire value, not just the enum key)

### Safer alternative

- Keep the wire-level string values of `DotCMSUVEAction` stable even if the TS enum key changes
- Add new fields to a payload additively; never remove/rename a field an existing editor build reads

---

# H — SDK Compatibility Headers Themselves

## H-1 — Compatibility Handshake Mechanism Change

**Direction:** Breaking (most severe category — breaks the detection mechanism itself)

### Context

`SdkVersionWebInterceptor` sets `X-DotCMS-Version` (from `ReleaseInfo.getVersion()`) and
`X-DotCMS-Min-SDK` (from `MinSdkVersion.VALUE`) on every response. `sdk-compatibility.ts`'s
`checkSdkCompatibility()` reads them case-insensitively via `Headers.get()`, and `compareVersions()`
parses both as numeric, dot/dash-separated segments (`parseVersionSegments`), returning `null` (fail
open, no warning) for anything that doesn't parse as plain integers — including LTS-shaped strings
like `26.7.14_lts_v1`.

### Why it breaks compatibility

This is the mechanism this entire document exists to protect. If a change alters the header names,
their casing-sensitivity assumptions, or the version-string shape/comparison semantics
`compareVersions()` depends on (e.g. switching away from date-lockstep numeric segments to something
`parseVersionSegments` can't parse), the compatibility check silently stops working for every SDK
version at once — not just for one release's floor value.

### Signals to watch for in code review

- Any change to `X-DotCMS-Version` / `X-DotCMS-Min-SDK` header names in `SdkVersionWebInterceptor`
- Any change to `compareVersions()` / `parseVersionSegments()` in `sdk-compatibility.ts`
- Bumping `MinSdkVersion.VALUE` to a non-numeric-segment (e.g. LTS-shaped) string — this doesn't just
  fail to gate correctly, it silently disables the check entirely for that comparison

### Safer alternative

- Treat this mechanism itself as doubly-reviewed: any change here should be treated as breaking by
  default unless proven otherwise, since a bug here is invisible (fails open, no warning, no error)

---

## Non-Breaking Examples (for calibration)

- Admin UI (`dotcms-ui`) only changes — not consumed by any SDK
- Adding a new **optional** GraphQL field/query (existing SDK queries simply don't request it)
- Adding a new REST response field (additive — old SDK code ignores fields it doesn't read)
- Adding a new inbound/outbound `postMessage` type that old SDKs simply never send/receive
- Internal refactors with no change to any wire format (GraphQL schema, REST JSON shape, or
  `postMessage` payload)
- Test-only or documentation-only changes

---

## A Note on This Document's Maturity

Unlike `ROLLBACK_UNSAFE_CATEGORIES.md` (grounded in years of real dotCMS incident history), this
document starts with no real-world track record of an actual SDK-breaking release. Categories above
are derived from reading the current SDK source, not from a postmortem. Treat the automated AI check
built on this document as an aid to human review, not a substitute for it, until it accumulates a
track record — the same posture the rollback-safety check already takes via its `Human: ...`
override labels. Expect this document to gain real "Examples from dotCMS history" entries over time
as actual breaking changes occur and get retroactively categorized here.
