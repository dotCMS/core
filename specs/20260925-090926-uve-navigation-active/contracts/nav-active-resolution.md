# Contract: `$nav.active` resolution

**Feature**: `specs/20260925-090926-uve-navigation-active/` | **Issue**: [#37105](https://github.com/dotCMS/core/issues/37105)

The interface this fix touches is not a REST endpoint — it is the **template-facing behavioral
contract** of `$nav.active` in Velocity, produced by
`com.dotcms.rendering.velocity.viewtools.navigation.NavResultHydrated.isActive()`.

This document is the **test oracle**. Every row is a case the unit tests must assert, and the
"Today" column is what the current code produces, so a reviewer can see at a glance which rows are
fixes and which rows are the no-regression guarantee.

---

## 1. The algorithm under contract

```
strippedURI    := stripRenderPrefix(request.getRequestURI())   // null when no rendering prefix
reqURI         := strippedURI, or the raw URI when strippedURI is null
indexPage      := indexPageName.get()   // seam; defaults to HTMLPageAssetAPIImpl.CMS_INDEX_PAGE::get
indexSuffix    := "/" + indexPage

active         := matches(reqURI)
                  OR ( strippedURI is not null           // <- Page API requests ONLY
                       AND reqURI does not end with indexSuffix
                       AND matches( reqURI ends with "/" ? reqURI + indexPage
                                                         : reqURI + indexSuffix ) )
```

where `matches(uri)` is **today's logic, unchanged**, plus one guard:

```
lastSlash := uri.lastIndexOf('/')
if lastSlash < 0        -> false                       // guard: never substring(0, -1)
parentPath := uri.substring(0, lastSlash)
if parentPath does not end with "/" -> parentPath += "/"

if isFolder() AND href does not end with "/"  -> parentPath.startsWith(href + "/")
else                                          -> !isCodeLink() AND href.equalsIgnoreCase(uri)
```

and `stripRenderPrefix` replaces the current global substring `replace`:

```
if uri startsWith "/api/v1/page/renderHTML/" -> drop the first 23 chars   // "/api/v1/page/renderHTML"
if uri startsWith "/api/v1/page/render/"     -> drop the first 19 chars   // "/api/v1/page/render"
otherwise                                    -> unchanged
```

`indexPageName` is a package-private `Supplier<String>` on `NavResultHydrated`, initialised to
`HTMLPageAssetAPIImpl.CMS_INDEX_PAGE::get` — the existing `Lazy<String>` over
`Config.getStringProperty("CMS_INDEX_PAGE", "index")` (`HTMLPageAssetAPIImpl.java:83-84`).
Production behavior is unchanged: the same memoized value, read once. **The seam exists because the
`Lazy` is `static final` and `io.vavr.Lazy` memoizes on first `get()`**, so `Config.setProperty` in a
test has no effect once any code in the JVM has read it — which makes rows CFG-2 and CFG-3
untestable at any layer without it. Tests assign the supplier and restore it in `@After`.

**The trailing `/` in both `startsWith` tests is load-bearing**, twice over. It makes the two tests
mutually exclusive, so their order does not matter — without it, `/api/v1/page/renderHTML/x` would
match the shorter `render` prefix and strip to `HTML/x`, which is precisely the bug in the current
`replace`. It also stops a page whose path merely begins with `render` from being mangled.

---

## 2. Invariants

| # | Invariant | Why it holds |
|---|---|---|
| **I1** | An item active today is still active. | `active` is an OR over today's test plus a new one. OR is monotone: it can only turn `false` into `true`. |
| **I2** | **Front-end rendering is bit-for-bit unchanged, for every URI shape.** | The index candidate is computed **only when a Page API rendering prefix was actually stripped**. A front-end request carries no such prefix, so the second operand is never evaluated and the expression reduces to today's code — not "reduces for index pages", but always. |
| **I3** | No false positive can reach the front end. | Follows from I2 rather than from an argument about what folder and page paths can coexist. The database constraint in §4 is no longer load-bearing; it is now corroboration. |
| **I4** | `parentPath` is never the empty string and the method never throws. | The `lastSlash < 0` guard. **Confirmed reachable at the Red gate**: a URI with no slash threw `StringIndexOutOfBoundsException: Range [0, -1)` before the guard existed. |
| **I5** | The REST and GraphQL nav payloads are unaffected. | Neither calls `isActive()`; neither exposes an `active` field. [research.md §R6](../research.md). |

---

## 3. Truth table — the test oracle

`F:` = folder nav item, `P:` = page nav item. "Today" is current `main`; "Contract" is required
after the fix. Rows where the two differ are **the fix**; rows where they agree are **the
regression guarantee**.

### 3.1 Front end (via `CMSFilter`) — every row must be unchanged

| # | Request URI | Nav item | Today | Contract | |
|---|---|---|---|---|---|
| FE-1 | `/TravelHub/index` | `F: /TravelHub` | active | active | unchanged (I2) |
| FE-2 | `/TravelHub/index` | `P: /TravelHub/index` | active | active | unchanged (I2) |
| FE-3 | `/TravelHub/index` | `F: /Travel` | not active | not active | same-prefix sibling stays off |
| FE-4 | `/TravelHub/index` | `F: /Partners` | not active | not active | unchanged |
| FE-5 | `/a/b/c/index` | `F: /a`, `F: /a/b`, `F: /a/b/c` | all active | all active | ancestors-active is existing, intended behavior |
| FE-6 | `/index` (site root) | `P: /index` | active | active | unchanged |
| FE-7 | `/index` (site root) | `F: /TravelHub` | not active | not active | root must not light up a folder |
| FE-8 | `/TravelHub/contact` (non-index page) | `F: /TravelHub` | active | active | unchanged |
| FE-9 | `/TravelHub/contact` | `P: /TravelHub/contact` | active | active | unchanged |
| FE-10 | `/store/product/widget-123` (URL-mapped detail) | `F: /store` | active | active | unchanged |
| FE-11 | any URI | `codeLink` item | not active | not active | `isCodeLink()` guard preserved |

### 3.2 Page API, explicit page URI — the #17896 guarantee (AC-005)

| # | Request URI | Nav item | Today | Contract | |
|---|---|---|---|---|---|
| API-1 | `/api/v1/page/render/TravelHub/index` | `F: /TravelHub` | active | active | prefix strip still works |
| API-2 | `/api/v1/page/render/TravelHub/index` | `P: /TravelHub/index` | active | active | |
| API-3 | `/api/v1/page/render/TravelHub/contact` | `F: /TravelHub` | active | active | |

### 3.3 Page API, folder-style URI — **the defect** (AC-002)

| # | Request URI | Nav item | Today | Contract | |
|---|---|---|---|---|---|
| **FIX-1** | `/api/v1/page/render/TravelHub` | `F: /TravelHub` | **not active** | **active** | the reported bug |
| **FIX-2** | `/api/v1/page/render/TravelHub` | `P: /TravelHub/index` | **not active** | **active** | the page branch, same cause |
| FIX-3 | `/api/v1/page/render/TravelHub/` | `F: /TravelHub` | active | active | **corrected at the Red gate — this already works today.** A trailing slash puts `lastIndexOf("/")` at the end, so `parentPath` comes out `/TravelHub/` and matches. Only the *slash-less* folder URI is broken. Kept as a characterization row. |
| **FIX-4** | `/api/v1/page/render/a/b/c` | `F: /a`, `F: /a/b`, `F: /a/b/c` | none active | **all active** | nested, matches FE-5 |
| **FIX-5** | `/api/v1/page/render/Travel` | `F: /Travel` | not active | **active** | same-prefix pair, positive half |
| **FIX-6** | `/api/v1/page/render/Travel` | `F: /TravelHub` | not active | **not active** | same-prefix pair, negative half — must stay off |
| **FIX-7** | `/api/v1/page/render/store/product/widget-123` (URL-mapped detail) | `F: /store` | active | active | unchanged — mirrors FE-10; the detail URI must not be treated as a folder |
| **FIX-8** | `/api/v1/page/render/<vanity-url>` matching no nav item | any | not active | **not active**, no throw | the spec's non-goal "must not make vanity/URL-mapped resolution worse", made executable (AC-003) |

### 3.4 Site root and degenerate URIs (AC-003)

| # | Request URI | Nav item | Today | Contract | |
|---|---|---|---|---|---|
| ROOT-1 | `/api/v1/page/render/` | `P: /index` | not active | **active** | root resolves its index page, as the front end does (FE-6) |
| ROOT-2 | `/api/v1/page/render/` | `F: /TravelHub` | not active | not active | root must not light up a folder |
| ROOT-3 | `/api/v1/page/render/index` | `P: /index` | active | active | unchanged |
| ROOT-4 | any of the above | — | no throw | no throw | I4 |

### 3.5 The prefix strip (R3)

| # | Request URI | Stripped to | Today | Contract |
|---|---|---|---|---|
| PRE-1 | `/api/v1/page/render/about-us` | `/about-us` | `/about-us` | `/about-us` |
| PRE-2 | `/api/v1/page/renderHTML/about-us` | `/about-us` | **`HTML/about-us`** ❌ | **`/about-us`** ✅ |
| PRE-3 | `/api/v1/page/renderHTML/TravelHub` | `/TravelHub` | `HTML/TravelHub` ❌ | `/TravelHub`, then FIX-1 applies |
| PRE-4 | `/about-us` (front end) | `/about-us` | `/about-us` | `/about-us` |
| PRE-5 | `/api/v1/page/_render-sources/x` | unchanged | unchanged | unchanged — no Velocity render on that endpoint |

### 3.6 Configured index page name (AC-007)

| # | `CMS_INDEX_PAGE` | Request URI | Nav item | Contract |
|---|---|---|---|---|
| CFG-1 | `index` (default) | `/api/v1/page/render/TravelHub` | `P: /TravelHub/index` | active |
| CFG-2 | `default` | `/api/v1/page/render/TravelHub` | `P: /TravelHub/default` | active |
| CFG-3 | `default` | `/api/v1/page/render/TravelHub` | `P: /TravelHub/index` | **not** active |
| CFG-4 | `default` | `/TravelHub/default` (front end) | `F: /TravelHub` | active, and `indexCandidate` is `none` |

Driven by assigning `NavResultHydrated.indexPageName` (see §1), not by `Config.setProperty`, and
restored in `@After`. A test that sets the property instead will silently pass or fail depending on
whether anything else in the JVM has already read the `Lazy`.

---

## 4. The front-end false positive, and how it was closed

An earlier revision of this contract computed the index candidate for *every* request and argued
the resulting front-end false positive was unrepresentable. The Red gate proved the false positive
was real (row XH-2 failed), so the design was tightened instead of the argument.

**What the risk was.** For a front-end non-index page such as `/a/b`, a candidate of `/a/b/index`
gives `parentPath = /a/b/`, which lights up a *folder* nav item whose href is exactly `/a/b` while
the user is on a *page* of that path.

**Why arguing it away was not enough.** On one host it genuinely cannot happen: `identifier` carries
`unique (parent_path, asset_name, host_inode)` (`dotCMS/src/main/resources/postgres.sql:1033`) and a
unique index on `full_path_lc(identifier)` = `LOWER(parent_path || asset_name)` per host
(`postgres.sql:1818-1822`), so folder `/a/b/` and page `/a/b` collide and cannot coexist. But both
keys include `host_inode`, and a nav tree can carry items from another host. That left a residual
the contract could only describe, not exclude.

**How it is closed.** The candidate is computed **only for a Page API request** — one whose URI
actually carried a rendering prefix. `CMSFilter` has already redirected a folder URL and appended
the index page name before Velocity sees a front-end request, so a front-end URI always names a page
and never needs a candidate. The Page API applies none of that, which is the whole defect. The guard
therefore costs nothing real and removes the entire class of front-end change, cross-host included.

| # | Scenario | Contract |
|---|---|---|
| XH-1 | Front-end URI `/a/b`; nav item `F: //hostB/a/b` | not active — host-qualified href matches nothing |
| XH-2 | Front-end URI `/a/b`; nav item `F: /a/b` with an unqualified href from another host | **not active** — no candidate is computed for a front-end URI |

## 5. Contracts explicitly NOT changed

| Interface | Guarantee | Basis |
|---|---|---|
| `GET /api/v1/nav/{uri}` | Response is byte-identical. No `active` key is added. | `NavResource.navToMap` enumerates eleven keys literally and never calls `isActive()`. File untouched. |
| GraphQL `DotNavigation` | Schema and response are byte-identical. No `active` field is added. | `NavigationTypeProvider.createNavigationFields()` declares the same eleven fields. File untouched. |
| `openapi.yaml` | No regeneration needed. | No JAX-RS annotation changes; no endpoint added, removed or re-described. |
| `CMSFilter` front-end URL normalization | Unchanged, including the 301 for folder URLs. | Out of scope per the spec; the fix mirrors its behavior without touching it. |
