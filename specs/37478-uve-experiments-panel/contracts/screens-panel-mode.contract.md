# Contract: the three screens in panel mode

One implementation of each screen, two presentations (FR-040, FR-041, D7, SC-015). This file
records, concern by concern, **what differs and what must not**. Anything not listed differs in
neither mode — that is the point of the contract.

Legend: **P** = portlet (`#panel === null`), **N** = panel (`#panel !== null`).

---

## 1. The list — `DotExperimentsListComponent` + `DotExperimentsListStore`

| Concern | P (unchanged) | N | Requirement |
|---|---|---|---|
| Hydrate view state from `route.snapshot.queryParams` | yes | **no** | FR-031, FR-032 |
| `location.subscribe(...)` popstate re-hydration | yes | **no** | FR-035, SC-011 |
| `syncUrlEffect` mirroring view state to the address | yes | **no** | FR-031, SC-010 |
| Fetch | `getAllUnfiltered()` → `GET /api/v1/experiments` | `getAll(pageId)` → `GET /api/v1/experiments?pageId=` | FR-010, D4, SC-005 |
| Bulk page lookup (`DotContentSearchService`, `+working:true +identifier:(…)`) | yes | **no** | FR-010 |
| Site narrowing via `pageInfoByPageId[…].host` | yes | **bypassed** — not merely unfed | FR-029, SC-008 (see note) |
| Analytics health gate before the first fetch | yes | **yes, identical** | FR-027, SC-005 |
| `selectedPageId` | from `?pageId=`, clearable | the page in hand, structural | FR-006, D6 |
| Layout | `p-table`, 7 sortable columns + kebab, `min-width: 81rem` | compact flex rows: name (truncating) + status + goal + schedule on one line, kebab on hover. **Selected by the mode flag alone** — never by measured width, so the panel never renders the table even on a viewport wide enough to clear 81rem | FR-009, FR-041 |
| Page column | shown | **absent** | FR-009 |
| Breadcrumb (`syncBreadcrumbEffect`) | pushed | **not pushed** | FR-033, D8, SC-012 |
| Row → Configure / Results | `router.navigate` | `panel.showConfigure(id)` / `panel.showResults(id)` — **phase 2/3**; until then `router.navigate` as today | FR-008, FR-013, FR-025g |
| New Experiment | `router.navigate(['/experiments','new'], {queryParams})` | `panel.showCreate()` | FR-007 |
| Empty state | site-wide or page-filtered copy | "this page has no experiments" + New Experiment action | FR-006a, FR-029 |
| Loading / error / misconfigured states | as today | **identical, and each distinguishable from empty** | FR-026, FR-027, FR-028, SC-008 |
| Push Publish, Add to Bundle | as today | **as today, unchanged** — no resolver needed | FR-011, D9 |
| Way out to the full portlet | n/a | link opening `/experiments` (unfiltered) in a **new tab** | FR-012, D6, US7 |

> **The site-narrowing trap.** `siteScopedExperiments` drops any experiment whose `pageId` is not
> in `pageInfoByPageId`. With the lookup skipped, that map is empty and the computed returns `[]`
> for a page that has experiments. The empty list then reads as "this page has none" — the exact
> misreport FR-029 forbids. This must be an explicit bypass with a test that fails if the
> narrowing is reintroduced.

---

## 2. Configuration — `DotExperimentsConfigureComponent` + `DotExperimentsConfigureStore` *(phase 2)*

| Concern | P (unchanged) | N | Requirement |
|---|---|---|---|
| `experimentId` source | `route.paramMap`, followed | `panel.experimentId()`, followed | FR-008 |
| Creation prefill (`pageId`, `url`, `language_id`) | `route.snapshot.queryParamMap` | the page in hand + live language | FR-015, FR-022a |
| Post-create URL swap (`new` → `:id/configuration`, `replaceUrl`) | yes | **no navigation at all** — `panel.showConfigure(newId)` | FR-002, FR-016 |
| Page card | summary + Select/Change button + `DotExperimentsChangePageDialogComponent` | summary only, **no button, no dialog**, plus a hint saying why it is fixed | FR-014, FR-014a, D12 |
| Traffic allocation (shares the Page card's row) | present | **present** — must survive the button's removal | FR-017 |
| Fields, validation, autosave, state transitions | as today | **identical** | FR-017, SC-014 |
| Read-only / locked banner | as today | **identical, and stated** | FR-020, FR-030 |
| Unsaved-work protection | `experimentsUnsavedChangesGuard` (`canDeactivate`) | the same confirm, called imperatively before close / back | FR-019, research R7 |
| Back | `router.navigate([EXPERIMENTS_URL], {queryParams})` | `panel.backToList()` | FR-018 |
| Breadcrumb | pushed | **not pushed** | FR-033 |
| Variant Preview / Edit Content | `buildVariantEditorLink` + `router.navigate`, marker `experimentReturn=portlet` | `buildVariantEditorLink` for the page params, `panel.suspendForVariant()`, **no `experimentReturn`** | FR-021, FR-023, FR-031 |
| Open Results | `router.navigate([id,'results'])` | `panel.showResults(id)` | FR-025b |

---

## 3. Results — `DotExperimentsResultsComponent` + `DotExperimentsResultsStore` *(phase 3)*

| Concern | P (unchanged) | N | Requirement |
|---|---|---|---|
| `experimentId` source | `route.paramMap` | `panel.experimentId()` | FR-025a |
| `healthStatus` source | `dotAnalyticsHealthCheckResolver` on the route | supplied by the panel — already known from the list's gate | FR-027, SC-005 |
| chart.js / `primeng/chart` load | with the route chunk | **only when results actually open**, via a nested `@defer` in the panel. `@defer` *is* available here, unlike at the shell→panel edge: results and the chart component live in this same lib, so the static import a `@defer` block needs is not a boundary crossing and `@nx/enforce-module-boundaries` never sees it | FR-025f, FR-037 |
| Measurements shown | all | **all** — never fewer to fit the width | FR-025e |
| Back | `router.navigate([EXPERIMENTS_URL], …)` | `panel.showConfigure(id)` when it came from configuration, else `panel.backToList()` | FR-025b |
| Breadcrumb | pushed | **not pushed** | FR-033 |

---

## 4. The editor side

| Piece | Today | With the flag on | Requirement |
|---|---|---|---|
| Nav item (`dot-ema-shell.component.ts`) | `href: '/experiments'` + `queryParams` | **no `href`** → emits `action('experiments')` → `handleItemAction` opens the panel | FR-001, FR-002 |
| Nav item visibility / `isDisabled` | `!page?.canEdit` | **identical on both sides of the flag** | FR-004 |
| Flag read | `readExperimentsPortletSwitch` | **the same reader, unchanged** — never the batched `uveStore.flags()` | FR-005, D5 |
| Toolbar badge (`DotEmaRunningExperimentComponent`) | `routerLink` to `/edit-page/experiments/{pageId}/{id}/reports` | emits; shell calls `panel.openResults(id)` | FR-025c, FR-025d, D14 |
| Variant return (`handleInfoDisplayAction('variant')`) | `experimentReturn` → portlet, else flag → portlet/legacy | **new first branch**: suspended panel → `resumeFromVariant()`; the two existing branches untouched | FR-023, FR-046 |
| Panel mount | — | the shell `import()`s the component on first open and `createComponent`s it into `#experimentsPanelHost`; it is a `p-drawer` with `appendTo="body"` over the canvas, **not** a grid track | FR-037, SC-006, SC-007 |

---

## 5. Flag off — the invariants

With the flag off, absent, or unreadable, **every row in every table above resolves to the P
column, and none of the N behaviour is reachable or observable** (FR-043, FR-044, SC-009).

Concretely, and each of these is a test:

1. The nav item has its legacy `href` and navigates. — FR-043
2. The toolbar badge keeps its `routerLink`. — FR-025d
3. `DotExperimentsPanelStore` is never opened; the panel component's chunk is never fetched.
   — FR-044, FR-037
4. The full portlet at `/experiments` is reachable and unfiltered from the main navigation,
   **regardless of the flag**. — FR-045
5. #37005's variant round trip (address-based) still works, **regardless of the flag**. — FR-046
6. Both the legacy per-page screens and the portlet screens remain present and functional.
   — FR-047
