# Data Model: navigation active state on folder-style Page API URLs

**Feature**: `specs/20260925-090926-uve-navigation-active/` | **Issue**: [#37105](https://github.com/dotCMS/core/issues/37105)

## There are no persisted entities

This fix creates, reads, updates and deletes nothing. No table, column, index, Elasticsearch or
OpenSearch mapping, cache region or serialized structure is added or altered. `isActive()` is a
pure function of one request URI and one nav item, computed per request and thrown away.

That is worth stating rather than leaving as an empty section, because it is what makes the change
rollback-safe (Constitution Principle IV / [Rollback-Unsafe Change
Categories](../../docs/core/ROLLBACK_UNSAFE_CATEGORIES.md)) and why the plan needs no
migration.

What follows is the model that *does* matter here: the shapes a request URI can take, and the
existing objects the computation reads.

---

## 1. Request URI — the input, and its states

A single string with a lifecycle across two very different paths.

| State | Produced by | Example | Has page segment? |
|---|---|---|---|
| **Raw front-end URI** | Browser | `/TravelHub` | no |
| **Normalized front-end URI** | `CMSFilter` — 301 to add the trailing slash, then append `CMS_INDEX_PAGE` (`CMSFilter.java:127-138`) | `/TravelHub/index` | **yes, always** |
| **Raw Page API URI** | JAX-RS, no normalization | `/api/v1/page/render/TravelHub` | no |
| **Prefix-stripped Page API URI** | `isActive()` itself | `/TravelHub` | **no — the defect** |
| **Index candidate** | `isActive()`, new | `/TravelHub/index` | yes, synthesized |

**Derivation rules** (full algorithm in [contracts/nav-active-resolution.md §1](./contracts/nav-active-resolution.md)):

- `reqURI` — the request URI with a leading `/api/v1/page/render/` or `/api/v1/page/renderHTML/`
  prefix removed, keeping the leading slash of the page path.
- `indexCandidate` — `reqURI` plus `/` plus the configured index page name, **defined only when
  `reqURI` does not already end in that suffix.** Undefined for every front-end index-page URI,
  which is what keeps the front end a no-op.
- `parentPath` — `reqURI` (or `indexCandidate`) truncated at its last `/`, then forced to end in
  `/`. Never empty: guarded when no `/` exists.

**Validation rules**

| Rule | Source |
|---|---|
| `parentPath` is never the empty string | AC-003 |
| The method throws for no routable URI, including the site root | AC-003 |
| The index page segment is read from `CMS_INDEX_PAGE`, never hardcoded | AC-007, Constitution Principle II |
| Stripping recognizes a prefix only when followed by `/` | [research.md §R3](./research.md) |

---

## 2. `NavResult` — the item being tested (read-only, unchanged)

`com.dotcms.rendering.velocity.viewtools.navigation.NavResult`. Cached; **has no active state of
its own** and is not modified by this fix.

| Field used by `isActive()` | Meaning | Role in the comparison |
|---|---|---|
| `href` | The item's URL path | Compared against `reqURI` / `indexCandidate`, or used as a `parentPath` prefix |
| `type` (`isFolder()`) | `folder`, `htmlpage`, `link`, `file` | Selects the folder branch or the page branch |
| `isCodeLink()` | Item is a Velocity code link | Page branch only: a code link is never active |

The remaining fields (`title`, `target`, `order`, `hostId`, `languageId`, `folderId`, children) are
untouched by this computation and are the ones the REST and GraphQL surfaces serialize.

---

## 3. `NavResultHydrated` — where the transient state lives

`NavResultHydrated extends NavResult`, constructed per request (`NavTool.java:117,256`) and again
for every child (`NavResultHydrated.java:122-123`). It holds:

| Field | Lifetime |
|---|---|
| `navResult` | The cached, unhydrated `NavResult` it delegates to |
| `context` — `transient ViewContext` | **Request-scoped.** The source of the `HttpServletRequest`, and the reason `isActive()` exists only on this subclass |

**Why the active flag cannot leak**: the cache stores `NavResult`, which has no `isActive` method
at all; the wrapper that computes it is created fresh per request and its context is `transient`.
There is no cached state to invalidate and no cross-request contamination to reason about
([research.md §R6](./research.md)).

---

## 4. Configuration read

| Key | Default | Where it is read | Declared |
|---|---|---|---|
| `CMS_INDEX_PAGE` | `index` | `HTMLPageAssetAPIImpl.CMS_INDEX_PAGE` — an existing `Lazy<String>` over `Config.getStringProperty` (`HTMLPageAssetAPIImpl.java:83-84`) | `dotmarketing-config.properties:728` |

Reused rather than re-read, so the value the fix appends is by construction the same value
`CMSFilter` appends — which is the whole definition of "matches the front end".

Reached through a package-private `Supplier<String>` seam on `NavResultHydrated`
(`indexPageName`, defaulting to `HTMLPageAssetAPIImpl.CMS_INDEX_PAGE::get`). The seam exists
because the `Lazy` is `static final` and memoizes on first `get()`, so a test cannot vary the value
via `Config.setProperty` at either layer — see [research.md §R4](./research.md).

---

## 5. The uniqueness constraint the design depends on

Not introduced here, but load-bearing for the no-regression argument, so it belongs in the model.

On `identifier`:

- `unique (parent_path, asset_name, host_inode)` — `dotCMS/src/main/resources/postgres.sql:1033`
- `CREATE UNIQUE INDEX idx_ident_uniq_asset_name on identifier (full_path_lc(identifier), host_inode)` — `postgres.sql:1822`
- `full_path_lc(identifier)` = `LOWER(parent_path || asset_name)` — `postgres.sql:1818`

**Consequence**: a folder and a page cannot occupy the same path on the same host. This is what
makes the added folder-branch match incapable of producing a front-end false positive
([contracts/nav-active-resolution.md §4](./contracts/nav-active-resolution.md)). The guarantee is
scoped per host, and the cross-host residual is covered by test cases XH-1 and XH-2 rather than by
assertion.
