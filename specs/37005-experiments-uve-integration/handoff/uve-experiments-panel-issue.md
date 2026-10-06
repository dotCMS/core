## Description

With `FEATURE_FLAG_EXPERIMENTS_PORTLET` on, the Universal Visual Editor's Experiments entry point currently **ejects the editor**: the `science` nav item navigates to `/experiments?pageId=…&language_id=…` (`dot-ema-shell.component.ts:222-247`), the iframe is torn down, and the editor gets back to the page through the breadcrumb or the back arrow. That is what #37005 shipped, and it is already better than the legacy per-page screens it replaces — but it still costs the page.

This issue keeps the editor on the page: the experiments for the page in hand render **in a panel beside the canvas**, behind the same flag. No new switch, no second rollout to coordinate.

It also settles what #37005 left half-done. FR-021c asks for the page filter to be "visible and clearable". The filter is applied and honoured, but there is no in-UI affordance to clear it, and clearing it would be a dead end anyway: the portlet is opt-in (declared in `portlet.xml`, no `UpgradeTask` adds it to a layout), so on a stock instance `GET /api/v1/menu` carries no `experiments` entry — verified, 43 items, `hasExperiments: false`. An editor arriving from UVE has no path to a site-wide list. In a page-scoped panel that requirement changes shape: the panel **is** the page's scope by construction, and "clearable" becomes an explicit link out to the full portlet.

## What exists today (prior art in UVE)

Four patterns, all live in this template, with their real costs:

| Pattern | Where | Mount cost | Chunk cost |
| --- | --- | --- | --- |
| CSS-collapsed, always mounted | left palette `edit-ema-editor.component.html:21`; right edit panel `:222` (widths `clamp(280px,15vw,320px)` and `clamp(360px,20vw,500px)`, `.scss:25,95`) | mounted on **every** UVE EDIT load, open or not | static import, sits in the editor chunk |
| `@if`-gated overlay drawer | `dot-block-editor-sidebar` (`p-drawer`), `:307-312` | mounts/destroys per open | static import |
| `@defer (when …)` + inner `@if` | Edit Content side panel, `:321-333` | mounts/destroys per open | **separate chunk, fetched on first open only** |
| Routed child of the shell | today's legacy experiments, `lib.routes.ts:93-113` | full route activation; `router-outlet` swap replaces the editor | lazy `loadChildren` |

The right edit panel also shows the tab variant: the wrapper is always mounted, but each tab's contents are `@if ($editorPanelActiveTab() === n)`-gated, so contents mount only while their tab is selected.

## Recommendation

**`@defer (when $experimentsPanel())` + inner `@if`, in a drawer** — the Edit Content side panel pattern, for four reasons:

1. The experiments code never enters the UVE editor chunk. The editor chunk is on the hot path for every page edit; experiments concern a minority of pages.
2. Zero cost when the panel is never opened, which is the common case.
3. Mount/destroy per open makes the panel's open/close its store's `onInit`/`onDestroy` — no stale view state, no live effects or `location` subscriptions while closed.
4. It is the newest pattern in this exact template and already carries its rationale in a comment there.

The nav item becomes an **action**, not a destination: `href` drops, and it routes through `handleItemAction` (`dot-ema-shell.component.ts:458`) like `page-tools` and `properties` already do. That also removes the `$activeHref()` highlight for it and makes the `MenuGuardService` exemption on `/experiments` (`app.routes.ts:180-198`) unnecessary for this path — the panel needs no route at all.

**Two of the three screens, in two phases.** The screens are very different shapes:

- **Phase 1 - the list.** A compact panel list (see item 2 below); rows lead into Configure.
- **Phase 2 - Configure, in the panel.** The largest screen in the lib (492K of source vs the list's 120K) and a multi-card form: Details, Goal, Variants, Scheduling, Traffic. It needs a width decision of its own before it is built - the cards are laid out for a full-width column and the Variants card is a table. Two candidate answers: a wider panel for this screen only (the Edit Content side panel already varies its own width, 80% to 100%, `dot-edit-content-side-panel.component.html`), or a single-column reflow of the cards. **Neither is chosen here.** One issue with two phases rather than two issues, because the panel's plumbing - the store's URL independence, the flag read, the resolvers - is shared, and phase 2 changes none of it.
- **Not Results.** It pulls `chart.js` + `primeng/chart` (`dot-experiments-reports-chart.component.ts:1,5`), the heaviest dependency in the portlet, for charts that need width. It stays full-screen in the portlet, opened from Configure as it is today.

Phase 1 has to ship usable on its own: a panel that can only list, with Configure still opening full-screen, is a smaller improvement than the whole thing but not a broken one.

## Why not the alternatives

- **CSS-collapsed always-mounted (palette/edit-panel pattern).** Puts the experiments chunk in the editor bundle *and* mounts the store on every UVE load — which, with the store's current `onInit`, means a health check, a full experiments fetch and a bulk page lookup fired behind a closed panel on every page edit. Non-starter.
- **A third tab in the existing right edit panel.** Mount cost is fine (tab contents are `@if`-gated), but the chunk cost is not unless the tab body is itself `@defer`ed, and the panel is 360-500px wide. It is also selection-scoped — it answers "the contentlet I clicked" — while an experiments list is page-scoped. Wrong drawer.
- **A routed child of the shell (what the legacy screens do).** `router-outlet` swaps the editor **out**, which is the exact cost this issue exists to remove. Route resolvers would run and UVE's URL would have to change, tearing down the iframe.

## What has to change (the real work)

This is not a template change. Five of these are load-bearing.

**1. The list store owns the URL; inside UVE it cannot.** `dot-experiments-list.store.ts` hydrates from `route.snapshot.queryParams` (`:598`), mirrors **every** view-state change back with `Location.go(createUrlTree(…, {queryParamsHandling:'merge'}))` (`:583,624`), and re-hydrates on popstate (`:610`). UVE writes its own params through `#updateLocation` (`dot-ema-shell.component.ts:663`) with `createUrlTree([], {queryParams})` — **no merge**, so it replaces the whole query string. Two `Location.go` writers on one address: the panel's `filter=`/`statuses=`/`page=` land in the UVE URL, and UVE's next view change silently drops them. The panel needs a mode where the store does not own the address — view state in memory, seeded from inputs (`pageId`, `languageId`) instead of query params. This is the largest piece of work here and it is a store change, not a component one.

**2. The list table does not fit a sidebar.** `LIST_TABLE_STYLE` is `min-width: 81rem` (1296px, `constants.ts:110`) across seven sortable columns. It cannot render at 360-500px, and an overlay wide enough for it would cover the page — a dialog with a different animation, not a sidebar. The panel needs a **compact row rendering**: name plus status, the `page` column dropped (the panel *is* the page), goal/schedule/modDate collapsed into a subline or dropped. The store is reused; the table is not.

**3. Fetch the page's experiments, not all of them.** The list calls `getAllUnfiltered()` — every experiment on every site — then resolves each distinct `pageId` through a bulk `htmlpageasset` lookup and narrows client-side (`dot-experiments-list.store.ts:96,112,144`). A page-scoped panel should call the `getAll(pageId)` that already exists (`dot-experiments.service.ts:66`) and skip the page lookup entirely, because UVE already holds `pageAsset()`. Three requests plus a full-set payload become one scoped request. Coordinate with #37007 (server-side list contract), which owns the same swap point.

**4. Reading the flag must not fail open.** The natural move — adding `FEATURE_FLAG_EXPERIMENTS_PORTLET` to `UVE_FEATURE_FLAGS` (`shared/consts.ts:114`) so it rides the batch `getFeatureFlags` request UVE already makes on init — **inverts the default**: `withFlags` maps `FEATURE_FLAG_NOT_FOUND` to `true` (`with-flags.feature.ts:42`). #37005 refused exactly that for this flag, which sits next to the visitor-facing kill switch. Either keep the fail-closed `readExperimentsPortletSwitch` (`experiments-portlet-switch.util.ts`) — the shell already makes that request for the nav item, so the panel adds none — or give `withFlags` a fail-closed variant. Do not join the batch silently.

**5. Resolvers the panel will not have.** The list route provides and resolves `DotPushPublishEnvironmentsResolver` (`dot-experiments/portlet/src/lib/lib.routes.ts`), and the row menu's Push Publish reads it off the route. Mounted outside the router the panel gets none of it and must provide it explicitly, or that action breaks silently. Same for the analytics health gate: the list defers its first fetch until `/api/v1/experiments/health` reports OK (`checkHealth()`, `:603`) — one extra request per open unless it is cached at UVE init.

**6. Breadcrumbs belong to UVE.** The three screens push crumbs onto `GlobalStore` through `putCrumbOnTrail` (`dot-experiments-breadcrumb.util.ts`). In a panel the trail is UVE's page's; the list must push nothing while it renders as a panel.

**7. `#37008` gets simpler, not harder.** With the panel as the entry point, the migration also deletes the `experiments` routed child (`edit-ema/lib.routes.ts:93-113`) and the legacy branch of the nav item, and no full-page eject survives anywhere.

## Performance budget

Measurable, and the reason for the pattern choice:

- The UVE editor chunk must not grow (build stats before/after).
- A UVE page load where the panel is never opened must fire **zero** experiments requests — no list, no health check, no page lookup.
- Opening the panel must fire **one** experiments request for the page (plus the health check, if it is not cached at UVE init).
- Closing the panel must leave no live effect or `location` subscription behind.

## Must not break

- Flag **off**: the legacy per-page screens and the `experiments/{pageId}` nav destination, untouched (`dot-ema-shell.component.ts:245`).
- The variant Edit Content round-trip from #37005: `Edit Content` on a variant opens UVE for that variant and returns to `/experiments/:id/configuration` for the originating experiment (`dot-uve-toolbar.component.ts:370-418`).
- The full portlet at `/experiments`, reachable and unfiltered from the main navigation.
- UVE's own URL: the panel writes nothing to it.

## Acceptance Criteria

- [ ] With the flag on, the UVE Experiments nav item opens a panel beside the canvas; the page stays rendered and the iframe is not reloaded.
- [ ] With the flag off, behavior is unchanged from today (specs + E2E regression).
- [ ] The panel lists only the current page's experiments, correct for zero (empty state with a way to create one), one, and many.
- [ ] The panel offers an explicit way into the full portlet (FR-021c's "clearable", honestly resolved).
- [ ] A row leads to Configure: in the panel once phase 2 lands, full-screen in the portlet before that.
- [ ] Configure in the panel edits, autosaves and transitions exactly as the full-screen screen does - same store, same form, same rules; only the layout differs.
- [ ] Results opens full-screen from Configure, and returns to the page the editor came from.
- [ ] Nothing the panel does writes to UVE's URL, and nothing UVE does resets the panel's view state.
- [ ] The panel pushes no breadcrumb of its own.
- [ ] Push Publish and Add to Bundle work from a panel row, or are deliberately absent with a stated reason.
- [ ] A UVE load that never opens the panel fires no experiments request; the editor chunk does not grow.
- [ ] The flag still fails closed on a missing key and on a failed read.

## Priority

Medium. It improves an entry point that already works; nothing is broken without it.

## Additional Context

- Depends on: #37005 (the flag, the filtered destination and the round-trip all land there).
- Related: #37007 (server-side list contract — same swap point as item 3), #37008 (migration; item 7).
- Spec to seed: `specs/37005-experiments-uve-integration/spec.md` D2 records why the destination is the page-filtered list, which is the premise this panel inherits.
- Out of scope: Results inside the panel; any change to how UVE edits, saves, locks or renders content; any change to how experiments are served to visitors; removing the flag or the legacy screens (#37008).
