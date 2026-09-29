# Issue Resolution Specification: UVE navigation active state lost on folder-style Page API URLs

**Feature Branch**: `20260925-090926-uve-navigation-active`

**Created**: 2026-09-25

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#37105](https://github.com/dotCMS/core/issues/37105) — "UVE: navigation active state lost after in-editor navigation — isActive() fails on folder-style Page API URLs with no page segment"

**Input**: User description: "https://github.com/dotCMS/core/issues/37105 — We have to be careful with this fix because navigation is used in multiple places, at least viewtool and rest, I think. So let's double check that we don't introduce a regression bug when working on this fix, even if the only affected functionality seems to be on the page editor."

## Problem Statement *(mandatory)*

Velocity navigation menus expose an `active` flag (`$nav.active`) that templates use to mark the
current section and expand its children. That flag is computed per request by
`NavResultHydrated.isActive()` (`dotCMS/src/main/java/com/dotcms/rendering/velocity/viewtools/navigation/NavResultHydrated.java:43-69`),
which derives the "current parent path" by chopping the request URI at its last slash.

That derivation assumes the request URI always ends in a page name. When the Page API renders a
**folder-style URL with no page segment** — which is exactly what the Universal Visual Editor (UVE)
produces when it navigates in-editor using the hrefs `$navtool` hands it — the assumption breaks:

| Request URI | derived `parentPath` | folder `/TravelHub` matches? |
|---|---|---|
| `/api/v1/page/render/TravelHub/index` | `/TravelHub/` | yes |
| `/api/v1/page/render/TravelHub` | `/` (empty, then normalized) | no |
| `/api/v1/page/render/Partners` | `/` | no |

Both branches of the method fail together: the folder branch compares an empty parent path against
`/TravelHub/` and never matches, and the page branch compares the nav href `/TravelHub/index`
against the URI `/TravelHub` and never matches either. Every nav item reports `active == false`.

This does not happen on the published front end because `CMSFilter` normalizes the URL first: it
301-redirects a folder URL that has no trailing slash, then appends the configured index page name
(`CMSFilter.java:127-138`, property `CMS_INDEX_PAGE`, default `index`). Velocity therefore always
sees `/folder/index`. The Page API (`GET /api/v1/page/render/{uri}`, `PageResource.java:489`) is a
JAX-RS resource and applies no such normalization.

**Severity / Impact**: High — major functionality broken for a common template pattern. Any
customer navigation keyed off `$nav.active` renders collapsed in UVE, and depending on the theme's
CSS, completely empty. The first load opened from the Site Browser is correct (it targets the page
asset, so the URI carries `/index`); every in-editor navigation after that silently loses active
state, and it does not come back for the rest of the editing session, because every subsequent
in-editor href is also folder-style. There is no error in the logs, in the browser console, or in
the editor, and `$navtool.getNav(0)` still returns the correct children — so the failure does not
look like a navigation problem and is very hard to diagnose. Reported twice by the same customer
(Freshdesk 32533 on 24.12.27 LTS, Freshdesk 38763 on Evergreen); the first report was closed by
rewriting the customer's template, without the core defect ever being identified.

## Reproduction *(mandatory)*

**Environment**: Current Release (dotEvergreen) on dotCMS Cloud; also reported on 24.12.27 LTS.
Server-side and not browser-specific. The `isActive()` logic is unchanged on `main`, so this
reproduces on latest. Requires the Universal Visual Editor (or any direct call to
`GET /api/v1/page/render/{folder}`).

**Steps to Reproduce**:

1. On any site, create a folder (e.g. `/TravelHub`) containing an `index` page, with child pages
   marked "show on menu".
2. Add a navigation snippet keyed off the active flag to the template:
   ```velocity
   #set($list = $navtool.getNav(0))
   #foreach($n in $list)
     <li class="#if($n.active)active visible#end">$n.title</li>
   #end
   ```
3. Open `/TravelHub/index` in the Universal Visual Editor from the Site Browser. The correct item
   is marked active — the request URI is `/api/v1/page/render/TravelHub/index`.
4. Click any internal navigation link in the rendered page so UVE navigates in-editor.
5. Observe that no item is marked active — the request URI is now `/api/v1/page/render/TravelHub`,
   with no `/index`.
6. Navigate back to the original page. The active state does not return.
7. View the same page on the published site. The active state is correct there, because `CMSFilter`
   normalized the URL to `/TravelHub/index`.

**Expected Behavior**: A folder-style URL resolves the same active nav item as the explicit
index-page URL — in the editor exactly as on the front end.

**Actual Behavior**: Every nav item reports `active == false` for the remainder of the editing
session. No error is raised anywhere.

**Reproducibility**: Always, on any folder-style Page API render URL. The customer confirmed it
with in-template debug output: site, host identifier and the `$navtool.getNav(0)` children were
byte-identical between the working and broken renders, with `requestURI` the only variable that
changed.

## Scope of Investigation *(mandatory)*

- **Affected area**: Page rendering — Velocity navigation viewtool (`$navtool`) as consumed by the
  Page API render endpoint, and therefore by the Universal Visual Editor.
- **Suspected surface**: Modern (`com.dotcms.*`). The defect is in
  `com.dotcms.rendering.velocity.viewtools.navigation.NavResultHydrated`. The behavior it must stay
  consistent with lives in legacy (`com.dotmarketing.filters.CMSFilter` and the `CMS_INDEX_PAGE`
  property read by `com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPIImpl:83-84`),
  but the fix is not expected to modify either.
- **Related known decisions**: Issue #17896 (closed 2020, commit `a6ee51fe71`) added the
  `.replace("/api/v1/page/render", "")` prefix strip to this same method, so that menu selection
  kept working under the Page API. That fix handled the API prefix but not a URI with no page
  segment — this is the same defect class resurfacing under UVE, and its behavior must be
  preserved. The plan phase formally consults `dotCMS/platform-adrs`.

### Who actually consumes the active flag (blast-radius survey)

The concern raised with this request — that navigation is used in more than one place, at least the
viewtool and REST — was checked against the code before scoping. The result narrows the blast radius
considerably:

| Consumer | Calls `isActive()`? | Evidence |
|---|---|---|
| Velocity templates via `$navtool` (`NavTool` → `NavResultHydrated`) | **Yes** — the only caller | `NavTool.java:91,117,256` returns `NavResultHydrated`; `$nav.active` resolves to `isActive()` |
| REST `GET /api/v1/nav/{uri}` (`NavResource.loadJson`) | No | serializes through `NavResource.navToMap`, which emits only `title`, `target`, `code`, `folder`, `host`, `href`, `languageId`, `order`, `type`, `hash`, `children` — `active` is not in the payload |
| GraphQL `DotNavigation` (`NavigationDataFetcher`) | No | uses the same `NavResource.navToMap`; the type declares the same ten fields plus `children` (`NavigationTypeProvider.java:31-46`), no `active` field exists |
| Cached nav tree (`NavToolCache` / `NavToolCacheImpl`) | No | `isActive()` exists only on the per-request `NavResultHydrated` wrapper, which holds a `transient ViewContext`; the cached object is the plain `NavResult`, which has no active/`isActive` member at all. The flag cannot be cached or leak across requests. |

So the REST and GraphQL navigation contracts cannot change as a result of this fix — their response
shape has no `active` field to change. **The real regression surface is Velocity template rendering
on the published front end**, which runs the same method on every page render for every customer
template that reads `$nav.active`. That is where the fix must be proved not to change behavior.

## Root-Cause Hypothesis

`NavResultHydrated.isActive()` computes the current parent path by string-chopping the request URI
at its last slash, and treats the trailing segment as a page name that can be discarded. When the
URI has no page segment (a folder-style URL such as `/TravelHub`), the last slash is at index 0, the
chopped `parentPath` is the empty string, and the subsequent normalization turns it into `/` — which
matches no folder href. The page branch fails for the mirror-image reason: the nav item's href
carries the index page name (`/TravelHub/index`) that the request URI does not.

The method is effectively relying on `CMSFilter`'s URL normalization having already run. Under the
Page API it has not. The fix is therefore expected to be a normalization of the request URI (append
the configured index page name for a folder-style URI, so it is compared in the same shape the front
end compares) rather than a rewrite of the matching logic.

Two adjacent observations in the same expression, to be confirmed in planning:

- The prefix strip is a global substring `replace`, not a prefix strip. `GET /api/v1/page/renderHTML/{uri}`
  (`PageResource.java:1200`) also renders Velocity, and its URI becomes `HTML/about-us` after the
  replace — no leading slash, so the derived paths are garbage and active state is already wrong
  there today. `GET /api/v1/page/_render-sources/{uri}` (`PageResource.java:261`) is not stripped at all.
- The index page name is configurable (`CMS_INDEX_PAGE`, default `index`). Any normalization must
  read it through `Config` rather than hardcode `index`, or the editor and the front end will
  disagree on sites that configure it.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- `NavResultHydrated.isActive()` resolves the same nav item for a folder-style Page API URI as it
  does for the equivalent explicit index-page URI.
- The derived parent path is never the empty string, and the method never throws, for any URI the
  Page API can route.
- The Page API prefix handling is corrected to strip a genuine prefix, so that the Velocity-rendering
  Page API endpoints (`/render/`, `/renderHTML/`) all resolve active state correctly rather than only
  `/render/`. If planning finds `/renderHTML/` deprecated or otherwise out of reach, it is dropped
  and the reason recorded.
- The index page segment used in normalization comes from the `CMS_INDEX_PAGE` property via
  `Config`, matching what `CMSFilter` appends.
- Unit test coverage for the URI shapes listed under Acceptance.

**Explicitly out of scope / non-goals**:

- Adding an `active` field to the REST `/api/v1/nav` response or to the GraphQL `DotNavigation`
  type. Neither exposes it today; adding one would be a new API contract, not a fix.
- Changing `CMSFilter`, the front-end URL normalization, or the 301 redirect behavior for folder
  URLs.
- Changing what UVE sends — making the editor append `/index` client-side would paper over a
  server-side defect that any Page API caller can hit.
- Refactoring `NavTool`, `NavResult`, `NavToolCache`, or the nav caching strategy.
- Fixing the unrelated UVE defect tracked as #36141 (duplicate `document.write` re-executing inline
  scripts), which came out of the same investigation.
- Any change to how vanity URLs or URL-mapped (detail page) URLs resolve — the fix must not make
  them worse, but resolving nav active state for them is not a goal.

## Regression Risk *(mandatory)*

- **Blast radius**: One method, but on a very hot path. `isActive()` runs for every nav item on
  every Velocity page render, on the published front end as well as in the editor, for every
  customer template that reads `$nav.active`. The survey above establishes that REST
  (`/api/v1/nav`) and GraphQL (`DotNavigation`) do not call it and expose no `active` field, and
  that the nav cache stores the unhydrated `NavResult`, which has no active state — so those three
  surfaces cannot regress from this change. The genuine risk is a normalization that is too
  permissive and marks **extra** items active on the front end (for example, matching an ancestor
  folder as well as the current one, or matching a sibling whose name is a prefix of the current
  one — the exact false positive the existing trailing-slash normalization was added to prevent).
  Every front-end URI shape that works today must produce a byte-identical result after the fix.
- **Backward compatibility**: No API contract, serialized state, DB schema, or index mapping is
  involved — the change is rollback-safe. The behavioral contract that must not break is the one
  from #17896: menu selection continues to work under the Page API. The one intended behavior change
  is that folder-style Page API URIs now resolve active state where they previously resolved none;
  a template that (deliberately or accidentally) relied on nothing being active in UVE would see a
  difference, which is the point of the fix.
- **Data considerations**: None. No stored data is wrong and nothing needs repair or migration —
  the flag is computed per request.

## Acceptance & Verification *(mandatory)*

- **AC-001**: The reproduction above no longer produces the actual behavior. After an in-editor
  navigation in UVE to a folder-style URL, the nav item for the current section is marked active and
  its children stay expanded, and returning to the originally opened page restores the original
  state.
- **AC-002**: `isActive()` returns the same result for `/api/v1/page/render/<folder>` as for
  `/api/v1/page/render/<folder>/index`, for both the folder branch and the page branch (a nav item
  whose href is `/folder/index` is active when the URI is `/folder`).
- **AC-003**: The derived parent path is never the empty string for any routable URI, and the method
  throws no exception for a site-root URI (`/api/v1/page/render/` and `/api/v1/page/render/index`)
  or for a URL-mapped / vanity URL that matches no nav item.
- **AC-004 (regression, front end)**: For every request URI shape the front end produces today —
  `/folder/index`, `/folder/page`, `/a/b/c/index`, `/index` — the active/inactive result for every
  nav item is unchanged from current behavior. In particular no ancestor folder and no
  same-prefix sibling (`/Travel` vs `/TravelHub`) becomes active that is not active today.
- **AC-005 (regression, #17896)**: The `/api/v1/page/render` prefix strip added for issue #17896
  still works — active state under the Page API for an explicit page URI is unchanged.
- **AC-006 (regression, REST & GraphQL)**: `GET /api/v1/nav/{uri}` and the GraphQL `DotNavigation`
  query return byte-identical payloads before and after the change, confirming the survey's claim
  that neither consumes the active flag.
- **AC-007**: The index page segment used in normalization is read from the `CMS_INDEX_PAGE`
  property, so a site that configures a non-default index page name resolves active state the same
  way in the editor and on the front end.

- **Verification method**:
  - **Unit tests (primary, and the Red gate for TDD)**: a new
    `NavResultHydratedTest` in `dotCMS/src/test/java/com/dotcms/rendering/velocity/viewtools/navigation/`,
    alongside the existing `NavResultTest`, driving `isActive()` with a mocked
    `HttpServletRequest`/`ViewContext` over the matrix: `/folder/index`, `/folder`, `/folder/`,
    nested `/a/b/c` and `/a/b/c/index`, site root `/` and `/index`, a same-prefix sibling pair, a
    URL-mapped page URI, and each of those again with the `/api/v1/page/render` prefix. Run with
    `./mvnw test -pl :dotcms-core -Dtest=NavResultHydratedTest`.
  - **Integration test**: extend
    `dotcms-integration/src/test/java/com/dotcms/rendering/velocity/viewtools/navigation/NavToolTest.java`
    (already registered in `MainSuite1b`) with a case that renders a menu through `$navtool` for both
    URI shapes and asserts the same item is active. Run with
    `./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dmaven.build.cache.enabled=false -Dit.test=NavToolTest`,
    confirming `Tests run: N` in `target/failsafe-reports/` rather than trusting the exit code.
  - **Manual**: the seven reproduction steps above, on a site with a `/folder/index` page and a
    template reading `$nav.active`, checked in UVE and on the published front end.

## Assumptions

- The UVE behavior described in the report — that in-editor navigation uses the folder-style hrefs
  returned by `$navtool`, without appending an index page segment — is current and is not itself
  scheduled to change. The fix is server-side regardless, so it holds either way.
- "Same result as the front end" is defined as: the result `isActive()` produces for the URI that
  `CMSFilter` would have normalized the equivalent front-end request to.
- No customer template is knowingly depending on nav active state being absent under the Page API.
  The change is a behavior change in that narrow case by design.
