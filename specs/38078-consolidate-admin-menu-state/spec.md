# Feature Specification: Consolidate Admin Menu State into GlobalStore

**Feature Branch**: `nicobytes/consolidate-admin-menu-state-into-globalstore`

**Created**: 2026-10-16

**Status**: Draft

**Type**: Refactoring (visible navigation behavior must not change; internal defects get closed)

**Related GitHub Issue**: [#38078](https://github.com/dotCMS/core/issues/38078) — *Consolidate admin menu
state into GlobalStore* (labels: Technical Debt, Refactoring, Team Falcon; priority Medium).

**Input**: User description: "https://github.com/dotCMS/core/issues/38078 — Read the issue in depth (body,
comments, linked issues/PRs) and understand it thoroughly before writing the spec. The goal is to make this
work correctly. The spec must call for very good unit tests and e2e tests (Playwright) as first-class
requirements, with a clear test strategy."

## Background *(mandatory for this refactor)*

The admin shell asks the backend for the signed-in user's menu (`GET /api/v1/menu`) and today keeps **more
than one in-memory copy** of the answer. The copy that draws the navigation bar is not the copy that answers
"is this portlet in the menu?". Three caches coexist (a public one on the menu service, a private one on the
current-user service, and the navigation store), and each consumer reads a different one. Because of that:

- **Every move between portlets re-requests the menu and throws the answer away.** The cache that is supposed
  to prevent this drops its stored response as soon as the request completes, so the next read goes to the
  network again.
- **A failed menu request leaves no trace** — the bar stays empty and nothing records why.
- **The highlighted item is stored on the very items that get deleted** whenever the menu is replaced, so the
  highlight survives only if some code remembers to repaint it. The websocket-update path does; the login path
  does not.
- **The item identity is a 4-character abbreviation of the layout id**, so two layouts that share a prefix and
  a portlet silently collapse into one item.
- **Three different rules** decide which item is "the current one" (highlight, breadcrumbs, document title),
  and they can disagree.
- **Dynamic (plugin) route registration is copy-pasted three times.**
- **"Add to menu" fetches into a private service instance**, so its layout dropdown and the bar can disagree
  until a websocket event arrives.

This work makes the navigation store's menu feature the **only** in-memory menu snapshot, turns the HTTP
service into a thin client with no cache, and gives the highlight, breadcrumbs and document title one shared
way to resolve "which item is the user on". Nothing about how the bar looks or behaves for the user changes,
except that the defects above stop happening.

## User Scenarios & Testing *(mandatory)*

<!--
  Stories are ordered by user-visible risk. Because this is a behavior-preserving refactor, most stories are
  "this still works exactly as before"; they are the regression net. Each one MUST be covered by BOTH a
  unit-level test and, where a browser journey exists, a Playwright e2e test (see "Test Strategy").
-->

### User Story 1 - One menu request per session, navigation that stays fast (Priority: P1)

After a backend user signs in, the admin navigation shows that user's tool groups in `tabOrder`. From then on,
moving between portlets never asks the backend for the menu again, and the same loaded menu is what every part
of the shell uses to decide whether a portlet exists.

**Why this priority**: This is the core defect. Today each in-app navigation sends a redundant
`GET /api/v1/menu` and discards the body; the "cache" does not cache. It is also the foundation every other
story builds on.

**Independent Test**: Sign in, record network traffic, open three different portlets one after another from the
bar, and count menu requests. Delivers a measurable reduction in backend traffic with no visible change.

**Acceptance Scenarios**:

1. **Given** a backend user signs in, **When** the admin loads, **Then** the navigation lists that user's
   groups ordered by `tabOrder`, and exactly one menu request was made to fill it.
2. **Given** the navigation is showing, **When** the user moves from one portlet to another inside the admin,
   **Then** no further menu request is sent.
3. **Given** the menu was loaded once, **When** any guard, dialog, selector or screen asks whether a portlet is
   in the menu, **Then** the answer comes from that same loaded menu and no additional request is made.

---

### User Story 2 - Clicking the navigation behaves exactly as before (Priority: P1)

Clicking items, groups and the collapse control opens the same places, with the same query parameter, as today.

**Why this priority**: The nav is used on every admin session; any regression here is immediately visible and
affects every user.

**Independent Test**: Click an item, a group (expanded and collapsed), the already-open item, Ctrl/Cmd-click,
and the collapse control; verify destination URL, `mId`, reload behavior and the `dot-side-nav-toggle` event.

**Acceptance Scenarios**:

1. **Given** the navigation is showing, **When** the user clicks an item, **Then** the item's `menuLink` opens
   with query param `mId` equal to the first 4 characters of the layout id.
2. **Given** an item is shown, **When** the user Ctrl-clicks or Cmd-clicks it, **Then** nothing navigates.
3. **Given** a portlet is already open, **When** the user clicks its own item, **Then** that portlet reloads.
4. **Given** the bar is expanded and a group is closed, **When** the user clicks the group, **Then** the group
   opens and the app navigates to the group's first item.
5. **Given** the bar is expanded and the group is already open, **When** the user clicks it, **Then** the group
   toggles without navigating.
6. **Given** the bar is collapsed, **When** the user clicks a group, **Then** the app navigates to that
   group's first item.
7. **Given** any state of the bar, **When** the user clicks the collapse control, **Then** the bar toggles and
   `dot-side-nav-toggle` is emitted. (The control still does not close the open group — preserved as-is.)

---

### User Story 3 - A browser reload restores the bar and highlights the right portlet (Priority: P1)

The three kinds of "reload" are treated separately: a browser reload (F5) restores state from storage and the
URL; an in-memory menu replacement keeps the user's place; in-app navigation only moves the highlight.

**Why this priority**: This is where today's behavior is subtly wrong (highlight stored on deleted items) and
where the refactor is most likely to regress.

**Independent Test**: Collapse the bar, open a portlet, press F5; confirm the bar is still collapsed and the
portlet from the URL is highlighted only after the menu loads.

**Acceptance Scenarios**:

1. **Given** the user collapsed the bar, **When** the browser is reloaded, **Then** the bar comes back
   collapsed (and expanded likewise) from the `dotcms.menu.status` value in `localStorage`.
2. **Given** a browser reload, **When** the menu finishes loading, **Then** the highlighted portlet is the one
   in the current URL; nothing is highlighted before the menu has loaded and nothing is read from leftover
   in-memory state.
3. **Given** a deep link or bookmark with no `mId`, **When** it opens, **Then** the matching portlet is
   selected.
4. **Given** a URL whose `mId` matches the first 4 characters of a layout id and the same portlet id exists in
   more than one layout, **When** it opens, **Then** the item in *that* layout is selected.

---

### User Story 4 - Live menu changes keep the user's place (Priority: P2)

When an administrator changes tool groups, or someone logs in as another user, the navigation updates without
losing where the user is.

**Why this priority**: These are the "in-memory replace" events. Today the highlight is repainted by one path
(websocket) and forgotten by another (login), and "Add to menu" can disagree with the bar.

**Independent Test**: Trigger `UPDATE_PORTLET_LAYOUTS` (add a tool to a group) and a login-as; check the bar,
the highlight, and the number of menu requests.

**Acceptance Scenarios**:

1. **Given** a portlet is highlighted, **When** the backend sends `UPDATE_PORTLET_LAYOUTS`, **Then** the groups
   are replaced with the new menu (one reload) and the same portlet stays highlighted if it still exists.
2. **Given** a portlet is highlighted, **When** an update removes that portlet, **Then** no item stays
   highlighted.
3. **Given** a user opens "Add to menu", **When** the dialog lists layouts, **Then** it lists the layouts of
   the current snapshot — the same ones the bar shows — and after a tool is added the bar updates from
   `UPDATE_PORTLET_LAYOUTS` without a second menu cache.
4. **Given** a user logs in as another user, **When** the login completes, **Then** the menu is reloaded for the
   impersonated user and the app lands on that user's first portlet.

---

### User Story 5 - Everything that asks "is this portlet in the menu?" agrees (Priority: P2)

Route guards, My Account, the locales selector, the workflow task dialog and the legacy iframe all read the one
snapshot.

**Why this priority**: These are the consumers that today read three different copies and can contradict each
other, notably the locales check, which never refreshes after a layout change.

**Independent Test**: With the snapshot loaded, check each consumer's answer; change the menu through a layout
update and check that the locales selector answer changes too.

**Acceptance Scenarios**:

1. **Given** a portlet is not in the menu, **When** a user navigates to it, **Then** `MenuGuardService`
   rejects it; Content Search and Site Browser still redirect to Content Drive under the current rules, and
   every other rejected route goes to the first portlet.
2. **Given** the `starter` portlet is / is not in the snapshot, **When** My Account opens, **Then** the starter
   entry is shown only when it is present.
3. **Given** the `locales` portlet is added or removed by a layout update, **When** the locales selector asks,
   **Then** it reports the answer from the updated snapshot.
4. **Given** the `workflow` portlet is in a layout, **When** the workflow task dialog builds its legacy URL,
   **Then** `p_l_id` equals the id of the layout that contains `workflow`.
5. **Given** the page is refreshed on a deep link, **When** the wildcard guard decides, **Then** it has waited
   for the first successful menu load so dynamic routes are already registered (no race).

---

### User Story 6 - Legacy JSP portlets open with a fresh URL (Priority: P2)

A legacy (non-Angular) portlet opens in the iframe using a URL read at the moment it opens, not the URL captured
when the navigation first loaded.

**Why this priority**: JSP URLs embed a timestamp (`r=<currentTimeMillis>`). A correct, retaining cache would
freeze that value; if the iframe reused it, browsers could serve stale JSP content. This must keep working
**without** reintroducing a refetch on every navigation and **without** changing Java.

**Independent Test**: Open a JSP portlet twice; confirm the iframe `src` carries a timestamp obtained at each
open, and that opening an Angular portlet makes no such request and does not use the iframe URL.

**Acceptance Scenarios**:

1. **Given** a legacy JSP portlet, **When** it opens, **Then** the iframe `src` comes from a portlet URL
   requested at open time.
2. **Given** that open-time request completes, **When** it returns, **Then** it does not clear the highlight
   and does not replace the navigation snapshot as a side effect.
3. **Given** an Angular portlet, **When** it opens, **Then** it navigates with `menuLink` and makes no iframe
   URL request.

---

### User Story 7 - Failures never blank a working navigation (Priority: P2)

**Why this priority**: Today a failed request has no error path at all. With one authoritative snapshot, a
transient failure must degrade gracefully.

**Independent Test**: Make the menu request fail (a) on first load and (b) on a later reload; verify screen
state and that nothing throws.

**Acceptance Scenarios**:

1. **Given** a menu is already on screen, **When** a reload request fails, **Then** the menu stays on screen
   and the next successful load replaces it.
2. **Given** the first load of a session fails, **When** the shell renders, **Then** the navigation is empty,
   no error is thrown in the navigation component, and the failure is recorded as an error state.

---

### User Story 8 - Plugin routes, document title and tidy ownership (Priority: P3)

**Why this priority**: Supporting behavior that must survive consolidation: dynamic routes, titles, and a
single instance of each service.

**Independent Test**: Load a menu containing an `angular-module` init param and check route registration after
load, update and login-as; navigate and check the document title equals the highlighted item's label.

**Acceptance Scenarios**:

1. **Given** a menu item declares init param `angular-module` (including the module-federation form
   `remote:<url>|<name>|<exposedModule>`), **When** the first menu load succeeds, **Then** its route is
   registered — and again after `UPDATE_PORTLET_LAYOUTS` and after login-as. Registration is idempotent.
2. **Given** navigation completes, **When** the title is set, **Then** it is the label of the item chosen by
   the same URL resolution used for the highlight (a shorter link can no longer claim a longer path).
3. **Given** the application is running, **Then** `DotMenuService` and `DotNavigationService` each exist
   exactly once on the root injector, with no component- or module-level copies.

---

### Edge Cases

- Two layouts whose ids share the first 4 characters and both contain the same portlet id: both items are kept;
  the `mId` lookup prefers the layout that is already the open group, otherwise the first by `tabOrder`.
- Two items share the same label (breadcrumb-label fallback): the duplicated-id check no longer hides the
  ambiguity; resolution follows the single URL rule (portlet id + `mId`, then label/portlet-id bookmark
  fallback, then exact `menuLink` path equality).
- A URL with no `mId` on a layout update: today it silently loses the highlight; it must resolve via the
  bookmark fallback.
- A portlet that disappears while the user is on it: highlight cleared, no stale highlight, no error.
- Menu replaced while a group is open: the open group and collapsed/expanded state are UI state and survive the
  replacement.
- A layout with no portlet ids: omitted by the backend; the UI must not render an empty group.
- The `r=` timestamp on JSP URLs changes on every request: it is never treated as stable cache content.
- A bookmark with a stale or invalid `mId`: falls back to the bookmark rule instead of selecting nothing.
- Request failure racing with a successful one (reload during in-flight load): the latest success wins and a
  failure never overwrites a successful snapshot.
- `localStorage` unavailable or holding a non-boolean value: the bar defaults to collapsed without throwing.
- Group order: determined by an explicit `tabOrder`, not by insertion order, so reshuffling the underlying map
  cannot reorder the bar.

## Requirements *(mandatory)*

### Functional Requirements

**Single snapshot & fetching**

- **FR-001**: The navigation store's menu feature MUST be the only in-memory copy of the menu response. No other
  service may hold its own menu response.
- **FR-002**: The menu HTTP service MUST be a thin client that performs the request and holds no cache and no
  public menu observable.
- **FR-003**: The menu MUST be requested once after sign-in; in-app navigation between portlets MUST NOT send
  `GET /api/v1/menu`.
- **FR-004**: An "ensure loaded" read MUST NOT touch the network when the snapshot is ready; an explicit "reload"
  MUST always touch the network. Reload triggers are: app start after auth, `UPDATE_PORTLET_LAYOUTS`, login and
  login-as.
- **FR-005**: The store MUST expose a load status (`idle`, `loading`, `ready`, `error`).
- **FR-006**: On a successful load the store MUST replace the whole snapshot, keep the active item if it still
  exists, clear it if it is gone, and then register dynamic routes.
- **FR-007**: On a failed load the store MUST set the error status; if a snapshot is already showing it MUST be
  left on screen, and the next successful load replaces it. A failed first load MUST leave the navigation empty
  and MUST NOT throw in the navigation component.

**State model**

- **FR-008**: Menu state MUST be split into a *server snapshot* (replaced as a whole) and *UI state* (not deleted
  when the snapshot is replaced): collapsed/expanded, open group (by full layout id), and the active item
  (by item key, not a flag written on every item).
- **FR-009**: Item identity MUST be portlet id plus the **full** layout UUID. The 4-character short layout id MUST
  be stored separately for `mId` lookups; two items that share a short id MUST both be kept.
- **FR-010**: Each group MUST carry an explicit `tabOrder`, and each item its index within the group; the bar
  MUST sort by those, not by object-key order.
- **FR-011**: Each item MUST keep the fields the shell still needs: `url`, `angular`, `ajax`, `initParams`,
  `menuLink`, parent id, parent label and parent icon. Angular items keep `item.url` as `menuLink`; all others
  use `/c/${item.id}`, and the raw `url` stays available for the iframe.
- **FR-012**: The legacy `active`, `isOpen`, `label`, `tabDescription` fields on `DotMenu` MUST NOT be used as
  state.

**Active item resolution (one resolver)**

- **FR-013**: A single URL resolver MUST be used by the highlight, the breadcrumbs and the document title, with
  this order: (1) portlet id + `mId` against the short layout id; (2) the existing bookmark fallback when `mId`
  is absent — breadcrumb label, then portlet id; (3) exact path equality on `menuLink` (not substring match).
- **FR-014**: When two layouts share a short id, the resolver MUST prefer the layout whose full id is already the
  open group, otherwise the first by `tabOrder`.
- **FR-015**: After a browser reload, no item is active until the menu loads and the current URL is resolved; the
  highlight MUST NOT be read from any leftover value.
- **FR-016**: `DotNavigationService` MUST stop fetching and MUST stop calling the highlight logic from three
  places; the store owns the reaction to navigation end. The service keeps `goToFirstPortlet` and the iframe
  reload for a navigation that does not change the route.
- **FR-017**: The document title on navigation MUST equal the label of the item chosen by the same resolver as
  the highlight.

**Consumers read the snapshot**

- **FR-018**: `MenuGuardService` MUST wait for the snapshot to be ready and reject a portlet not in it, keep the
  Content Search / Site Browser → Content Drive redirect rules as they are today (only re-pointed at the
  snapshot), and send every other rejected route to the first portlet.
- **FR-019**: `DefaultGuardService` MUST wait for the snapshot to be ready instead of subscribing to a private
  observable; the first successful load MUST register dynamic routes before the guard decides.
- **FR-020**: My Account MUST show the starter entry only when `starter` is in the current snapshot.
- **FR-021**: The locales selector MUST report whether `locales` is in the same snapshot, including after a later
  layout update. The current-user service MUST NOT keep its own menu response (it may delegate to the store).
- **FR-022**: The workflow task dialog MUST build its legacy URL with `p_l_id` equal to the id of the layout that
  contains the `workflow` portlet.
- **FR-023**: "Add to menu" MUST list the layouts of the current snapshot; after a tool is added, the navigation
  MUST update from `UPDATE_PORTLET_LAYOUTS` without a second menu cache.

**Legacy iframe**

- **FR-024**: Opening a legacy (non-Angular) portlet MUST set the iframe `src` from a portlet URL requested at
  open time, and MUST NOT reuse the `url` captured when the navigation first loaded. That request MUST NOT clear
  the active item or replace the snapshot. Angular portlets MUST navigate with `menuLink` and MUST NOT use it.

**Dynamic routes**

- **FR-025**: Routes declared by init param `angular-module` MUST be registered after the first successful menu
  load, after `UPDATE_PORTLET_LAYOUTS`, and after login-as, from one load path. Registration MUST stay idempotent
  and the three duplicated registration copies MUST collapse into that one path.

**Events & actions**

- **FR-026**: On `UPDATE_PORTLET_LAYOUTS`, the navigation MUST replace its groups with the new menu and keep the
  current portlet highlighted when it still exists; when it was removed, nothing stays highlighted.
- **FR-027**: Login-as MUST reload the menu for the impersonated user and still land on that user's first
  portlet. A plain login MUST also reload.
- **FR-028**: Nav click, group click (expanded/collapsed), the collapse control, Ctrl/Cmd-click and click on the
  already-open item MUST behave exactly as in User Story 2. `mId` stays the first 4 characters of the layout
  UUID so existing bookmarks keep working.
- **FR-029**: The collapsed/expanded state MUST persist in `localStorage` key `dotcms.menu.status` and be restored
  on browser reload.

**Ownership & scope guards**

- **FR-030**: `DotMenuService` and `DotNavigationService` MUST be provided once, on the root injector. The
  duplicate registrations (shared module `forRoot`, content-types portlet component, content-types edit module)
  MUST be removed.
- **FR-031**: No file under `core-web/libs/dotcms-js` may change. `MenuResource`, `MenuHelper` and `LayoutAPI`
  (Java) MUST NOT change.

### Key Entities

- **Menu snapshot**: the user's menu as last received from the backend — a set of *groups*, each with
  `tabOrder`, and *items* inside them. Replaced as a whole. Has a load status (`idle`, `loading`, `ready`,
  `error`).
- **Menu group**: a layout/tool group — layout UUID, name, tab name, tab icon, URL of first portlet, `tabOrder`.
- **Menu item**: a portlet inside a group — portlet id, label, `url`, `angular`, `ajax`, `initParams`,
  computed `menuLink`, short layout id, index in group. Identity = portlet id + full layout UUID.
- **UI state**: collapsed/expanded (persisted), open group id, active item key. Survives snapshot replacement.
- **URL resolution result**: the single item (or none) the current URL maps to; consumed by highlight,
  breadcrumbs and title.

## Test Strategy *(mandatory — first-class requirement)*

Per the constitution (Principle V, Test-First), tests are written, dev-approved, and confirmed **failing for
the right reason (Red)** before any implementation. Because this is a refactor of behavior that must not change,
tests come in two flavors: **characterization tests** (pin today's observable behavior *before* touching code,
so they pass against the old code and keep passing) and **defect tests** (fail today, pass after).

**Quality bar for every test**: asserts user-visible or contract-visible behavior (requests sent, URL opened,
state exposed, `src` set), never implementation internals; deterministic (no sleeps, no real time); follows
`docs/frontend/TESTING_FRONTEND.md` (Spectator/Jest or Vitest per project, `byTestId`) and
`docs/frontend/TESTING_REVIEW_RULES.md`; one reason to fail per test; includes the negative case (what must
*not* happen — e.g. no request, no highlight, no throw).

### Unit tests (required, per area)

| Area | Must prove |
|---|---|
| Menu store feature (`with-menu.feature.spec.ts`) | load replaces snapshot; ensure-loaded makes **no request** when `ready`; reload **always** requests; failed reload keeps previous entities and sets `error`; failed first load leaves empty + `error`; active key **survives** replace when item still exists and is **cleared** when gone; full-UUID ids; two layouts sharing a short id keep both items; `mId` lookup incl. tie-break (open group, then `tabOrder`); groups ordered by `tabOrder` regardless of insertion order; `isNavigationCollapsed` / open group untouched by replace; `localStorage` restore incl. invalid values |
| URL resolver | each rule in order (portlet+`mId`; bookmark label then portlet id; exact `menuLink` equality); stale/invalid `mId`; duplicate labels; shorter `menuLink` does **not** claim a longer path; highlight, breadcrumb and title agree for the same URL |
| `DotMenuService` | `getMenus()` performs exactly one request per call and keeps **no** state; the old "second subscribe does not hit HTTP" test is deleted with `menu$` |
| `DotNavigationService` | **no `GET /api/v1/menu` on `NavigationEnd`**; exactly one reload on `UPDATE_PORTLET_LAYOUTS`; login and login-as reload and go to first portlet; no second orchestrator when injected twice; title equals resolver label; iframe reload when navigation doesn't change the route |
| `DotNavigationComponent` | item click (`mId` = first 4 chars; Ctrl/Cmd no-op; already-open reload); group click expanded/collapsed/open; collapse control toggles and emits `dot-side-nav-toggle`; empty snapshot after failed first load renders without throwing |
| Guards (`MenuGuardService`, `DefaultGuardService`) | wait for ready; reject missing portlet; Content Search / Site Browser → Content Drive; others → first portlet; dynamic routes registered before the wildcard decides |
| `DynamicRouteInitializerService` / dynamic routes | registered after first load, after update, after login-as; idempotent; module-federation string form |
| Legacy iframe | JSP portlet requests URL at open time; Angular portlet requests **nothing**; fresh-URL request does not clear active item nor replace snapshot |
| Workflow task detail service | `p_l_id` = layout id containing `workflow` |
| My Account | starter entry iff `starter` in snapshot |
| Locales selector / current-user service | answer tracks the snapshot, including after a later layout update; no private cache remains |
| Add to menu | lists snapshot layouts; no private menu fetch |
| Providers | `DotMenuService` / `DotNavigationService` registered once at root (no component/module providers) |
| Regression guard | repository check that `core-web/libs/dotcms-js` is unchanged by this work |

### End-to-end tests (Playwright, required)

Located in `core-web/apps/dotcms-ui-e2e` following `AGENTS.md`: `getByRole` → `getByTestId` first; data and
layout changes created via REST helpers in `src/requests/`, never via UI setup; `waitForResponse` /
`waitForRequest` registered **before** the triggering action; unique names, cleaned up in `afterEach`; no
`force: true`, no `isVisible()` as a wait. Existing test ids to reuse: `nav-item`, `nav-item-main`,
`nav-item-label`, `nav-item-toggle`, `nav-sub-group-header`, `nav-sub-group-name`,
`dot-nav-header-toggle-button`.

| # | Journey | Asserts |
|---|---|---|
| E1 | Sign in and load admin | navigation shows tool groups in `tabOrder`; exactly **one** `GET /api/v1/menu` for the load |
| E2 | Move between three portlets from the bar | **zero** additional `GET /api/v1/menu`; each item highlights; URL carries `mId` = first 4 chars of the layout id |
| E3 | Click semantics | Ctrl/Cmd-click does not navigate; clicking the open item reloads it; group click expanded opens + navigates to first item; group click collapsed navigates to first item; collapse control toggles the bar |
| E4 | Browser reload (F5) | bar restores collapsed/expanded from `dotcms.menu.status`; after the menu loads, the portlet in the URL is highlighted |
| E5 | Deep link / bookmark with and without `mId` | the right item is highlighted; with duplicate portlet across layouts, the layout named by `mId` wins |
| E6 | Layout update (`UPDATE_PORTLET_LAYOUTS`): add a tool via REST / "Add to menu" | bar updates with one reload; current portlet stays highlighted; removing the current portlet's tool leaves nothing highlighted; "Add to menu" dropdown and bar agree |
| E7 | Login-as another user | menu reloads for the impersonated user and lands on that user's first portlet |
| E8 | Legacy JSP portlet (e.g. Publishing Queue in `#detailFrame`) | iframe `src` is set from a URL requested at open time (distinct `r=` on each open); an Angular portlet opens with no extra menu request |
| E9 | Failed menu request (intercepted/aborted) | first-load failure → empty nav, shell doesn't crash, no uncaught error; later-reload failure → previous nav remains |
| E10 | Guard behavior | direct URL to a portlet not in the user's menu is rejected and sent to the first portlet; Content Search / Site Browser URLs land on Content Drive |
| E11 | Plugin route | a portlet with `angular-module` init param is reachable after load and after a layout update *(if a fixture plugin can be provided; otherwise covered by unit tests and noted)* |
| E12 | My Account / locales / workflow dialog | starter entry presence follows the snapshot; locales selector reflects a layout update; workflow task dialog opens with the right `p_l_id` |

Each e2e journey is tagged `@critical` where it guards a P1 story (E1–E5). E2 and E1 together are the headline
proof of the main defect (redundant requests).

### Gates

1. Tests written (characterization + defect tests, unit + e2e). 2. Developer approves them. 3. Red confirmed
   (defect tests fail for the right reason; characterization tests pass on the old code). Only then implement.
4. Before PR 2: all unit tests green, e2e journeys green against a running instance, `/speckit-converge` run.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Signing in and moving through ten different portlets sends exactly **1** menu request in total
  (today: 1 + one per navigation).
- **SC-002**: 100 % of the navigation behaviors listed in User Story 2 behave identically before and after
  (verified by characterization tests passing on both).
- **SC-003**: After a browser reload on any portlet URL, the correct portlet is highlighted once the menu has
  loaded, in 100 % of the deep-link cases tested (with `mId`, without `mId`, and duplicate portlet across
  layouts).
- **SC-004**: After a layout update, the bar, the "Add to menu" list and the locales/starter checks all agree
  within one reload (no consumer reports a stale answer).
- **SC-005**: A failed menu request never empties a navigation that was already showing, and never causes an
  uncaught error in the shell.
- **SC-006**: Exactly **one** in-memory copy of the menu exists and exactly **one** instance each of the menu
  and navigation services exists (verified by tests).
- **SC-007**: Every acceptance criterion of #38078 maps to at least one unit test and, where a browser journey
  exists, at least one Playwright test (traceability table produced in the plan).
- **SC-008**: Zero changes under `core-web/libs/dotcms-js` and to the Java menu classes.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: The admin shell navigation, route guards, the legacy (JSP/Dojo) iframe host,
  the workflow task dialog, My Account, the locales selector and "Add to menu". The legacy iframe/JSP surface
  is part of the older product and must keep working with its timestamped URLs.
- **Backward-compatibility expectations**: Visible navigation behavior is unchanged. `mId` stays the first 4
  characters of the layout UUID so existing bookmarks keep working. The backend contract of `GET /api/v1/menu`
  (`@NoCache`, per-user, sorted by `tabOrder`, JSP `r=` timestamp, `UPDATE_PORTLET_LAYOUTS`) is context the
  frontend must keep honoring; no Java change, no new per-portlet URL endpoint, and the `r=` timestamp stays.
  `core-web/libs/dotcms-js` (including `RoutingService` and its unused `GET /api/v1/CORE_WEB/menu`) is
  explicitly out of scope — do not delete, retarget or clean it up.
- **Known related decisions**: The state-management standard in `docs/frontend/STATE_MANAGEMENT.md`
  (signal-store feature, `rxMethod` + `tapResponse`, `patchState` only, no menu state in an injectable
  service), `docs/frontend/ANGULAR_STANDARDS.md`, `docs/frontend/TESTING_FRONTEND.md`, and the existing
  GlobalStore breadcrumbs feature. The plan phase will formally consult `dotCMS/platform-adrs`.

## Out of Scope

- Anything under `core-web/libs/dotcms-js`.
- Any Java change; a per-portlet URL endpoint; removing `r=` from `MenuHelper`.
- Content Drive redirect rules in `MenuGuardService`, other than pointing them at the single snapshot.
- The collapse button's current effect (`collapseNavigation` does not close the open group even though its
  comment says it does) — left as is.
- Changing the `mId` query param format.

## Assumptions

- Users are backend (admin) users; the menu endpoint already enforces that and per-user sorting.
- The open-time legacy URL request (FR-024) is a `GET /api/v1/menu` made when the iframe opens; the issue
  accepts this. If that request fails, the iframe falls back to the `url` already in the snapshot rather than
  blocking the portlet. (Chosen default; revisit in plan if undesirable.)
- Only one group is open at a time and only one item is active at a time (current constraints preserved).
- E2E runs against a dotCMS instance started the usual way (`E2E_BASE_URL` + `CURRENT_ENV=ci` against a
  container); layouts for E6/E7 can be created and changed through REST helpers, and a plugin fixture for E11
  may not be available — in that case that journey is covered at unit level and the gap is recorded in the plan.
- Other callers that mention `/api/v1/menu` only in comments (`app.routes.ts`, the dot-ai routes, the
  experiments list) go through the guard and are not separate consumers of the HTTP endpoint; the plan verifies
  this by grep before implementation.

## Input Discoveries to Carry Into Planning *(non-normative)*

The issue states its discoveries and recommended fix are part of the task and must not be dropped. They are
recorded here as planning input, not as requirements beyond the FRs above:

- **Defects to close** (issue numbering): (1) a public `menu$` second source of truth; (2) the response-sharing
  operator on RxJS 7.8 drops its connection when subscribers reach zero, so the "cache" refetches — the existing
  "no second request" unit test hand-assigns `menu$` and never exercises this; (3) no error path/status;
  (4) highlight lives on entities that replace deletes; (5) entity id is the 4-char abbreviation; (6) ambiguous
  label fallback and a duplicated `item.id === portletId` check; (7) the current-user service caches the first
  response forever; (8) JSP `url` embeds a changing `r=` timestamp, so a retaining cache would freeze it;
  (9) dynamic-route registration copied three times; (10) "Add to menu" fetches into a private instance.
- **Recommended direction**: move fetch into the menu feature (`rxMethod` + `tapResponse`); `status`;
  `ensureLoaded` / `reload`; `activeKey` instead of per-entity `active`; full-UUID entity id with `shortLayoutId`;
  explicit `tabOrder`; one resolver shared by highlight, breadcrumbs and title; `hasPortlet(id, { jspOnly })`,
  `layoutIdForPortlet(id)` and `loadFreshPortletUrl(id)` as store reads replacing `isPortletInMenu`,
  `getDotMenuId` and `getUrlById`; `DotMenuService` reduced to `getMenus()`.
- **Suggested order** (one outcome, split only for delivery): (1) status + fetch + ensureLoaded/reload, stop
  refetching on navigation; (2) move the three queries to store reads and delete the current-user cache;
  (3) one resolver, `activeKey`; (4) full-UUID ids, `tabOrder`, single root providers, "Add to menu" from store.
- **Files in play** are listed in the issue body (store + models, navigation component/service, guards, iframe,
  workflow task detail, My Account, add-to-menu, providers/shared module, current-user service, locales
  selector); backend files are read-only.
