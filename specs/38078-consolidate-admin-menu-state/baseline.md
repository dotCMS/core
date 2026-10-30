# Baseline & Findings: #38078 menu consolidation

Working notes for the implementation. Written 2026-10-16 on branch
`nicobytes/consolidate-admin-menu-state-into-globalstore` before any production code changed.
Not a design document: [plan.md](./plan.md) and [research.md](./research.md) stay the source of design.

## ⚠️ Finding that changes the premise of User Story 1 (needs a developer decision)

The issue (defect 2) and the spec/plan/research built on it say that `DotMenuService.menu$` **does not
retain the response**, so every `NavigationEnd` re-sends `GET /api/v1/menu`. **That is not what happens.**

- **RxJS experiment** (installed `rxjs@7.8.2`): `http.pipe(shareReplay({ bufferSize: 1, refCount: true }))`
  subscribed three times, one after another, after the source completed → **1 source call**. In RxJS 7 the
  `share` operator only resets on refCount 0 when the source has *not* completed (`!hasCompleted`), so a
  completed HTTP response stays replayed.
- **Real app**, `dotcms/dotcms:latest` on `:8080` driven with Playwright: **1** menu request after login
  and load; **0** across 5 hash navigations and 4 in-app link clicks.
- **Unit evidence**: in the new `iframe-porlet-legacy.component.spec.ts`, a second `getUrlById` after the
  first menu response sent **no** second request.

What this means:

| Item in spec / plan | Consequence |
|---|---|
| Spec Background bullet 1, US1 "Why this priority", research D5 "removes the double fetch" | The premise "every move re-requests the menu" is wrong. US1's request-count assertions (SC-001, E1, E2) **already pass on the old code**. |
| T038 (E1/E2 as US1 **D** tests) and T040 (their Red) | They cannot be Red. E1/E2 are **characterization** tests (they guard against a *regression* of the good behavior). They were moved into the Phase 2a characterization file (`menu-load-and-navigation.spec.ts`) — see "Deviations". |
| Defect 8 / US6 ("a retaining cache would freeze the JSP `r=` timestamp") | It is **already frozen today**: the old cache retains the first response, so the iframe reuses the first `r=`. Fetching the URL at open time (FR-024) is a **behavior change** (an improvement), not a preservation. Its D test is still Red on the old code, for that reason. |
| Defects 1, 3, 4, 5, 6, 7, 9, 10 | Still real and unaffected: public `menu$`, no error path, highlight stored on entities that a replace deletes, 4-character entity id, duplicated label/id checks, `DotCurrentUserService` cache that never refreshes, three registration copies, "Add to menu" on a private instance. |

The refactor is still justified by those defects; only the "extra request" headline is not. The developer should
decide whether to reword the spec/issue (SC-001, US1 rationale) before PR 1 is approved. **Not edited here.**

## Other discrepancies found while reading the code

1. **My Account "starter entry" (spec FR-020, issue AC)**: the "Show Getting Started" checkbox is **always
   rendered**. What depends on the menu is its **initial checked state** (`showStarter` starts `true` only when
   `starter` is in the menu). The C test pins that, and the e2e asserts the checkbox is checked. The spec wording
   "shows the starter entry only when…" is imprecise.
2. **`GET /api/v1/layouts` returns `tabOrder`**; `GET /api/v1/menu` does not. The e2e helper uses it to prove the
   menu order equals the layouts' `tabOrder` order (research D4 unchanged for the client).
3. **CLAUDE.md says `pnpm nx test <p> --testPathPatterns=`**, but this project's Vitest rejects it
   (`Unknown option --testPathPatterns`). Use the positional filter: `pnpm nx test <project> -- <pattern>`.
4. **`libs/global-store/src/lib/store.spec.ts` already exists** (10 tests). The plan said "new"; US3 extends it.
5. The `dotcms-ui-e2e:e2e` target depends on `dotcms-ui:serve:development`, which fails to start here; Playwright
   still runs against `E2E_BASE_URL=http://localhost:8080 CURRENT_ENV=ci`. The e2e runs below used the Angular app
   **bundled in the running `dotcms/dotcms:latest` image**, i.e. the pre-change frontend, not a build of this branch.

## T001 — Baseline test run (`NX_NO_CLOUD=true pnpm nx run-many -t test -p global-store,data-access,dotcms-ui,edit-content`)

| Project | Files | Tests | Result |
|---|---|---|---|
| data-access | 98 | 1038 | ✓ |
| global-store | 6 | 203 | ✓ |
| dotcms-ui | 231 | 2358 | ✓ |
| edit-content | 121 | 2490 | **1 failed** — pre-existing, unrelated: `dot-edit-content-relationship-field.component.spec.ts › Create New Content Dialog › should open the new content dialog when the feature flag is enabled` (5 s timeout). Not caused by this work. |

After Phase 2a: global-store 211 tests ✓; the dotcms-ui specs touched (navigation component, iframe legacy,
workflow detail, My Account, dynamic routes, guards): 109 tests ✓. Lint on `global-store`, `dotcms-ui`,
`dotcms-ui-e2e`: clean. Type-check of the e2e project reports many pre-existing errors in untouched fixtures and
`playwright.config.ts`; none in the new files.

## T002 — `edit-content` may import `@dotcms/store`

Non-spec code already does it (`dot-edit-content-field.component.ts`, `dot-edit-content-form.component.ts`,
`relationship-field.store.ts`). Nx module boundaries allow any dependency (`depConstraints: *`). **No fallback
needed**: the locales selector can inject `GlobalStore` (US5).

## T003 — `getPortletId`

`DotRouterService.getPortletId(url)` (`dot-router.service.ts:372`) is **pure**: it decodes the URL, drops the
query string, splits on `/`, filters `''`, `#`, `c`, applies `PORTLET_ID_RESOLVERS` (a module constant, handles
multi-segment ids such as `analytics/dashboard` → `analytics-dashboard`) and the `add` form. Its spec covers
those. **Decision for US3**: extract the body to an exported pure function `getPortletIdFromUrl(url)` in
`libs/data-access/src/lib/dot-router/` and have the method delegate to it; the resolver uses the function.

## T004 — Login-as dialog (for E7)

Reached through the toolbar avatar (`data-testid="avatar"`) → menu item "Login As"
(`dot-toolbar-user.store.ts:130`). Dialog test ids: `dot-login-as-dialog`, `dot-login-as-user-dropdown`,
`dot-login-as-user-item`, `dot-login-as-password-input` (shown only when a password is needed),
`dot-login-as-submit-button`. Target user `limited1` (`chris@dotcms.com`, password `chris`, in
`src/utils/credentials.ts`). Still to confirm by running it (first e2e task of US4): whether the dialog asks for
the **admin's** password or the target's.

## T005 — E11 fixture

On the running starter instance **no menu item has `initParams['angular-module']`** (checked via
`GET /api/v1/menu`: only `view-jsp`, `view-action`, `baseTypes`, `contentTypes`, `portletSource`, `dataViewMode`,
`name`, `view-path`). Creating one needs a plugin portlet. **E11 is unit-only unless the developer wants a
plugin fixture**; to be signed at the US8 gate (T107). The new `registerRoutesFromMenuItems` unit tests (T019)
already cover module-federation registration, idempotence and malformed descriptors.

## T006 — Who injects `DotNavigationService` / `DotMenuService`

Root consumers: `MenuGuardService`, `ContentletGuardService`, `DotLoginAsComponent`, `DotToolbarUserStore`,
`DotNavigationService` itself. `DotContentTypesPortletComponent` and `DotContentTypesEditModule` re-provide
**both** services; only `DotAddToMenuComponent` injects the scoped `DotMenuService`. Removing those providers
makes their children resolve the root instance, which is the intended single instance.

## Phase 2a — characterization set (all pass on the unchanged production code)

| Task | What | Where |
|---|---|---|
| T010 | item click `mId` (full UUID → 4 chars), Cmd/Ctrl-click, open-item reload, same portlet id in another layout navigates, group click ×4, collapse control + persistence-free collapse | `dot-navigation.component.spec.ts` (+13 cases) |
| T011 | **already covered**: `menu-guard.service.spec.ts` has 31 cases for allow/reject/first portlet/Content Drive rules | no change |
| T012 | **already covered**: `default-guard.service.spec.ts` pins registration, re-navigate and `goToMain()` | no change |
| T013 | `p_l_id` = layout that contains `workflow`, asserted at the HTTP boundary | `dot-workflow-task-detail.service.spec.ts` (+1) |
| T014 | My Account "Show Getting Started" checkbox starts checked iff `starter` is in the menu | `dot-my-account.component.spec.ts` (+2) |
| T015 | legacy iframe `src` from the JSP url, Angular item never used, `?url=` query, `add` route, no-access; **new file** | `iframe-porlet-legacy.component.spec.ts` (5) |
| T016 | **already covered**: locales selector manage control shown/hidden (`Manage locales button`) | no change |
| T017 | persistence of `dotcms.menu.status`: absent/true/false/invalid, write-back, `storage` event, other keys, empty state after reload | `with-menu.feature.spec.ts` (+8) |
| T018 | **already covered**: `breadcrumb.feature.spec.ts` "Menu Item Matching with mId Query Parameter" | no change |
| T019 | `registerRoutesFromMenuItems`: remote module, idempotent, non-Angular ignored, malformed descriptor | `dynamic-route.service.spec.ts` (+4) |
| T020–T023 | E2E (17 tests incl. auth setup, run twice: 17/17): load + tabOrder + one request, three portlets zero requests, Ctrl/Cmd, open Angular/legacy item, group click expanded/collapsed, collapse control, F5 expanded/collapsed, deep link with/without `mId`, absent portlet → first portlet, `/c/content` and `/c/site-browser` → Content Drive, My Account checkbox | `src/tests/menu/*.spec.ts` |

### Deviations from tasks.md

- **E1/E2 moved from US1 (D, T038) to Phase 2a (C)** because of the finding above. T038 should be dropped or
  reduced to "stays green".
- **Workflow task dialog e2e (E12 part, T023) not written**: it needs a workflow task seeded through REST. The
  `p_l_id` rule is covered at unit level (T013). To confirm at the gate.
- **My Account e2e asserts the checkbox state**, not the presence of an "entry" (discrepancy 1).
- The iframe "reload uses a fresh URL" case was **removed from the C set**: on the old code the reload returns
  the cached first URL, so pinning it would pin the bug. It belongs to US6 as a D test.
- `store.spec.ts` exists already (extend in US3).

## Gate status

- T024 **Developer approval of the C set** — given ("go ahead") after the finding above was reported.
- T025 **Baseline + mutation check** — done. All C tests pass on the unchanged code (above) and every one of the
  20 deliberate breaks below was caught (production files restored after each; `git status` clean of production
  changes).

### Mutation table (unit level)

| Broken behavior (production line) | Result |
|---|---|
| item click `mId` = first 4 characters | killed (2 failed) |
| Cmd-click must not navigate | killed (1) |
| open item = same portlet **and** same layout | killed (2) |
| collapsed group click navigates | killed (1) |
| collapse control emits `dot-side-nav-toggle` | killed (2) |
| expanded group click toggles | killed (5) |
| guard sends a rejected route to the first portlet | killed (5) |
| guard rejects a portlet missing from the menu | killed (25) |
| default guard registers dynamic routes | killed (2) |
| default guard `goToMain()` | killed (1) |
| `p_l_id` = layout that contains `workflow` | killed (1) |
| My Account starter initial state | killed (2) |
| iframe ignores Angular items | killed (1) |
| iframe `?url=` query param wins | killed (1) |
| iframe `add` route uses the contentlet service | killed (1) |
| stored `false` restores the expanded bar | killed (1) |
| bar state is written back to `localStorage` | killed (1) |
| `storage` event follows another tab | killed (1) |
| dynamic route registration is idempotent | killed (2) |
| malformed remote descriptor is rejected | killed (1) |

**Limit**: the Playwright journeys (T020–T023) cannot be mutation-checked here because they run against the frontend
bundled in the running `dotcms/dotcms:latest` image; breaking this branch's code does not change that bundle. They
were checked for stability instead (17/17, run twice). They get a Red/Green meaning only when run against a build of
this branch (CI, or `nx serve` once the dev server works).

## Phase 2b — Foundation tests (waiting for approval T028)

- **T026** `libs/data-access/src/lib/dot-menu/dot-menu.service.spec.ts` — 5 cases.
- **T027** `libs/global-store/src/lib/features/menu/with-menu.feature.spec.ts` — 21 new cases (`withMenu loading`):
  `ensureLoaded` ×6, `reload` ×6, first-load failure, routes registrar ×4, `loadFreshPortletUrl` ×4.
- **Red evidence (run before approval, to check they are well formed)**: 4 of 5 data-access cases fail with
  `spectator.service.getMenus is not a function`; 21 of 21 store cases fail with
  `store.ensureLoaded / store.reload / store.status is not a function`. The one passing data-access case
  ("exposes no menu cache or legacy query") passes because the stub has no members, and guards against regressions.
- Two **behavior-free stubs** exist only so the imports resolve: `menu-routes-registrar.token.ts` (the injection
  token declaration) and `dot-menu.service.ts` (empty class). T031/T033 replace them with the real thing.

## Implementation log

- **Gates**: the developer authorized the whole run ("vamos con todo", 2026-10-16). Gate tasks after that message
  (T028 onward) were passed on that blanket authorization, each with its evidence recorded here.
- **Foundation (T030–T036)**: `MenuStatus`, real `DotMenuService` (`@dotcms/data-access`), `reload`/`ensureLoaded`/
  `whenSettled`/`loadFreshPortletUrl` in `with-menu.feature.ts`, registrar token bound in `providers.ts`. Green:
  global-store 232, data-access 1043, edit-content 2490, dotcms-ui 2382.
- **US1 (T037–T043)**: Red = the rewritten `dot-navigation.service.spec.ts` failed with
  `Path: DotNavigationService -> DotMenuService` (the service still depended on the legacy cache). T038 (E1/E2 as
  D tests) was dropped: they are characterization tests since Phase 2a. `DotNavigationService` now only talks to
  the store; the push-in `loadMenu(menus)` is gone and specs seed the store through the HTTP boundary.
- **US2 (T044–T046)**: no production change; the characterization set stayed green.
- **US3 (T047–T064)**: Red = 44 store failures + 2 service failures (`activeKey`, resolver and selection missing;
  the title `indexOf` bug showed as `'Content Search'` where `''` was expected). Implemented: `getMenuItemKey` and
  new entity fields, full-UUID entity key, `tabOrder`/`itemIndex`, pure `resolveMenuItem` (+ the pure
  `getPortletIdFromUrl` extracted from `DotRouterService`), `activeKey` replacing the entity flag, the single
  `withMenuSelection` reaction composed inside the breadcrumbs `withFeature`, the sub-nav reading `activeKey`.
  `DotNavigationService` no longer highlights. Green: global-store 268, dotcms-ui 2412, data-access 1043.
  - **Deviation**: the spec edge case "a stale `mId` falls back to the bookmark rule" is **not** implemented. The
    existing breadcrumb spec pins the opposite ("should NOT match when mId does not match parentMenuId prefix"), and
    that test is part of the approved characterization set. A stale `mId` resolves to no item, as before.
  - T050 was folded into `with-menu-selection.feature.spec.ts` (highlight, last breadcrumb and title agree), and the
    existing 1389-line breadcrumb spec passes unchanged on the shared resolver.
  - The duplicate-portlet e2e (T052) passes on the old bundle too (the 4-character keys differ there), so it is a
    guard, not a Red test; the 4-character collision is unit-only.
- **US4 (T065–T072)**: Add-to-menu spec went Red (`No provider for DotMenuService`); the component now reads the groups
  from `GlobalStore.menuGroup()` after `ensureLoaded()` and never requests the menu itself (dropdown label is the group
  label = `tabName`, the one the bar shows). E6/E7 e2e written: **E6 "layout update keeps the highlight" is a real Red on
  the old bundle** (the old websocket path omitted `bookmark`, so a URL without `mId` loses the highlight) and E7 passes.
  The "Add to menu" dropdown e2e (E6 part 3) was not written: it needs the content-type actions UI; unit-covered.
- **Parallelism finding**: a layout change is broadcast to every open admin session, so any test that changes a layout
  breaks the request counts of tests running in parallel. Everything that counts menu requests or changes a layout
  lives in `menu-layouts-and-requests.spec.ts` (serial). A first attempt with a separate Playwright project and a
  `dependencies` link re-ran the **whole** e2e suite against the shared dev instance (Playwright runs dependency
  projects completely, ignoring CLI file filters); it was reverted and `playwright.config.ts` is unchanged. That run
  created and removed test data on the dev instance as the suite normally does; layouts were verified unchanged after.
- **US5 (T073–T089)**: Red = 32 guard failures, 3 default-guard, 3 workflow, My Account/locales/current-user failing on the
  legacy dependency. Implemented `hasPortlet`/`layoutIdForPortlet`, guards on `ensureLoaded` + store reads, workflow
  `p_l_id`, My Account, locales selector (a `computed` over `hasPortlet`), and removed `#menuCache$`/`isPortletInMenu`
  from `DotCurrentUserService`. Not written: locales-selector e2e (needs the content edit sidebar).
- **US6 (T090–T095)**: iframe host asks `loadFreshPortletUrl` on open, on re-emitted route params and on reload;
  `?url=` and `add` paths unchanged. The store answers an Angular item with `''` and no request. The E8 e2e
  (open-time request, distinct `r=`) is in the serial file.
- **US7 (T096–T102)**: tests for the bar (empty on first failure, keeps groups on a failed reload, recovers) and the
  service (no throw, no navigation to a stale first portlet, retries on the next navigation) were **already green on
  the Foundation**, as the plan anticipated; no extra production change was needed.
- **US8 (T103–T113)**: Red = `providers.spec.ts` failed 6 of 9 (duplicate `DotNavigationService` in
  `SharedModule.forRoot()`, legacy `DotMenuService` still in the root providers and in the content-types component and
  edit module, the initializer file still present). Done: duplicate providers removed, `DynamicRouteInitializerService`
  and the app `DotMenuService` (+ specs) deleted, ten specs that provided the old service cleaned up. The registrar
  tests (called after load, after reload, optional) were already green from the Foundation. **E11 stays unit-only**
  (no plugin portlet in the starter): `registerRoutesFromMenuItems` (module-federation, idempotent, malformed) and the
  registrar are covered; no `test.fixme` was added so lint stays clean. Needs developer sign-off.
- **Polish**: grep gate clean (remaining hits are a private `#reloadMenu` helper and `DotContentTypeService.getUrlById`,
  a different method); `core-web/libs/dotcms-js` and `dotCMS/src/main/java` have an empty diff against `origin/main`;
  `docs/frontend/BREADCRUMBS.md` and `core-web/libs/global-store/README.md` updated.

## End-to-end evidence against a build of THIS branch

`nx serve dotcms-ui --port=4300` (the dev server of this worktree, proxying `/api` to the dev instance on :8080), then
`E2E_BASE_URL=http://localhost:4300 CURRENT_ENV=ci pnpm nx e2e dotcms-ui-e2e src/tests/menu`: **25/25 passed, twice**
(24 tests + auth setup). Against the old bundle (:8080) the one real Red was "a layout update keeps the highlight":
it fails there (the old websocket path omitted `bookmark`) and passes on this branch.

Two e2e findings worth keeping:
- The suite shares **one server session** (one saved login). A login-as on it turns every parallel test into the
  impersonated user (401s on admin Basic-auth calls, other menus on screen). `menu-login-as.spec.ts` therefore starts
  from an empty `storageState` and signs in again.
- A layout change is broadcast to every open session, so tests that count menu requests or change layouts live in one
  serial file (`menu-layouts-and-requests.spec.ts`), and removal tests use a portlet that lives in a single layout
  (`tags`); removing a portlet that exists in two layouts correctly leaves it highlighted.

## Final verification (T118–T120)

`nx affected -t test,lint --base=origin/main` (40+ projects): everything passes **except** five targets in projects this
branch does not touch (`git diff origin/main` is empty for them): `dotcms-js:lint` (an `any` in `dot-router.service.ts`),
`dotcms-webcomponents:test` (Stencil needs Jest 27) and `:lint` (import order), `block-editor:lint` and
`block-editor:test` (**35 failed | 88 passed, identical on a clean `origin/main` worktree**). Trunk debt, not this work.
Counts on the touched projects: dotcms-ui 2434, global-store 275, data-access 1042, edit-content 2492, dotcms-models 16.
Full production build of `dotcms-ui` was not run; the dev build (esbuild + type check) of this branch compiled and served
the 25/25 e2e run.
