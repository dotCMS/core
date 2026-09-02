# Contract: Navigation destinations

**Spec**: [../spec.md](../spec.md) | **Research**: [../research.md](../research.md) R2, R4, R6

The user-visible contract of this work is a set of **URLs**. Every requirement in Sections C and D
of the spec is a statement about one of the rows below, so this file is the table the regression
tests assert against.

`{persona}` is `com.dotmarketing.persona.id`, always the default persona identifier UVE itself uses
(`DEFAULT_PERSONA.identifier`).

---

## 1. UVE Experiments navigation item

**Gesture**: the `science` item in the UVE navigation bar.
**Built at**: `dot-ema-shell.component.ts` `$menuItems`.
**Routed at**: `EditEmaNavigationBarComponent.navigate()`.

| Switch | Destination | Query params | Requirement |
|---|---|---|---|
| **off** | `/edit-page/experiments/{pageId}` | current UVE params, **merged** (`url`, `language_id`, `{persona}`, …) | FR-016 |
| **on** | `/experiments` | `pageAsset={pageId}` only — **not merged** | FR-021, FR-021a, FR-022 |

**Off is byte-identical to today.** The item's `href` stays `experiments/{page.identifier}`, the
`edit-page` prefix and `queryParamsHandling: 'merge'` stay, so the address an editor sees does not
change (FR-016). Verified by a router-spy assertion, not by inspection.

**On carries `pageAsset`, nothing else.** Merging UVE's params would put `url`, `language_id` and
`{persona}` into the list's URL, where its `parseViewState` does not recognise them — stale keys
that survive every later filter change. See [research.md R3](../research.md) for why the key is
`pageAsset` and not `page` (which is pagination).

**Visibility and permissions are unchanged on both branches** (FR-023): the item keeps
`isDisabled: !page?.canEdit`. The switch changes the destination, never who can reach it.

**Mechanism** — `navigate()` gains one branch: an `href` starting with `/` is absolute, anything
else keeps the `edit-page` prefix. All four existing items (`content`, `layout`, `rules/{id}`,
`experiments/{id}`) are relative, so their behavior is unchanged by construction.

---

## 2. Variant open-in-editor (the outbound leg)

**Gesture**: Edit Content / Preview on a row of the portlet's Variants card.
**Built at**: `DotExperimentsConfigureVariantsComponent`, from store data only.
**Not gated by the switch** (FR-027) — the portlet is reachable from the main navigation either way.

```
/edit-page/content
  ?url={selectedPage.path}
  &language_id={selectedPage.languageId}
  &com.dotmarketing.persona.id={persona}
  &variantName={variant.id}
  &experimentId={experiment.id}
  &mode={EDIT_MODE | PREVIEW_MODE}
  &experimentReturn=portlet
```

Navigated with `Router.navigate(['/edit-page/content'], { queryParams })` and **no**
`queryParamsHandling` — the portlet's URL carries `filter`/`orderby`/`pageAsset`, none of which
UVE wants.

`experimentReturn=portlet` is the origin marker §3 reads to decide where the return lands. The
legacy card does **not** set it, which is what keeps FR-018's "as before" true without touching the
legacy path. Its exact name is an implementation detail of this contract, but it must be a query
param — never storage — for the reasons in §3.

### `mode` selection

`UVE_MODE.PREVIEW` when **any** of the following holds; `UVE_MODE.EDIT` otherwise:

| Condition | Source | Requirement |
|---|---|---|
| the variant is the control | `isControlVariant(variant)` | FR-008 |
| the experiment is not a draft | `$isLocked()` (`status !== DRAFT`) | FR-009 |
| the page is locked by another user | `$lockedByAnotherUser()` | FR-010 |

An **OR across all three** — never `!!$disabledTooltipKey()`, which reports only the strongest
reason and does not cover the control at all. When the page-lock branch is what triggered read-only,
the reason is stated to the user (FR-010) from the existing
`EXP_CONFIG_ERROR_LABEL_PAGE_BLOCKED` key.

`mode` sends **`UVE_MODE`** — the type UVE declares for the parameter
(`DotPageApiParams.mode?: UVE_MODE`) and compares against (`mode === UVE_MODE.EDIT`). On the wire
that is the literal `'EDIT_MODE'` / `'PREVIEW_MODE'`, and the test pins those strings.

Note for readers of the legacy card: it sends `DotPageMode` instead, and works only because
`DotPageMode.EDIT`/`PREVIEW` carry the same two strings. The enums diverge at LIVE (`'ADMIN_MODE'`
vs `'LIVE'`), so the equivalence is a coincidence rather than a contract.

### Refusal (FR-004, SC-006)

The action refuses — a message naming the reason, and **no navigation** — when any of:

| Missing | Why it cannot be defaulted |
|---|---|
| `selectedPage()` is null | no page to open; already the state after an unresolvable `pageId` |
| `selectedPage().path` empty | `editEmaGuard` would substitute `url=/`, opening the site root |
| `selectedPage().languageId` absent | `editEmaGuard` would substitute `language_id=1`, opening the wrong language |

Defaulting is what makes these dangerous rather than loud: the guard *completes* missing params
instead of rejecting, so a partially-formed link opens a plausible-looking wrong page. FR-004 exists
because of that, and the refusal is what satisfies it.

---

## 3. Variant return (the inbound leg)

**Gesture**: the variant chip in UVE's info-display.
**Routed at**: `DotUveToolbarComponent.handleInfoDisplayAction('variant')`.

**Resolved by the origin, not by the switch.** The destination answers "which configuration screen
opened this variant?", and the switch is consulted only when there is no answer. A switch-only
branch would make FR-018 and FR-005/FR-027 mutually exclusive on a supported path — switch off,
portlet reached from the main navigation (FR-026), variant opened (FR-027), return — which would
land the editor on the legacy screen they never came from. See
[research.md R6](../research.md) for the full derivation.

| Origin | Destination | Query params | Requirement |
|---|---|---|---|
| the **portlet's** Configure screen (marker set on the outbound leg) | `/experiments/{experimentId}/configuration` | `mode`, `variantName`, `experimentId`, marker → `null`; **not merged** | FR-005, FR-006, FR-027 |
| the **legacy** per-page card (sets no marker) | `/edit-page/experiments/{pageId}/{experimentId}/configuration` | `mode`, `variantName`, `experimentId` → `null`; rest **merged** | FR-018 |
| **absent** — a pasted or bookmarked variant URL | whichever of the two the switch currently selects | as per the branch taken | the spec's deep-link edge case |

The legacy row is the **existing code path, untouched** — which is what makes FR-018's "as before"
true by construction rather than by re-derivation: the legacy outbound leg sets no marker, and its
`queryParamsHandling: 'merge'` still supplies the rest of the address.

**Resolved by the experiment on every branch.** The existing code already sources
`currentExperiment.id` from `store.pageExperiment()`, so US1 scenario 4 (a page hosting two
experiments) and the spec's "two experiments on one page" edge case are already handled — and the
portlet branch keeps that, because the new portlet's route (`experimentsConfigureMatcher`) is keyed
on `experimentId` alone and takes no `pageId` segment.

**FR-006** covers the three nulls on every branch, plus the origin marker on the portlet branch —
otherwise the marker outlives the experiment context it describes.

**The marker is a query param**, not session or local storage: it has to survive a reload and a
pasted link, and it has to be *absent* rather than stale when someone deep-links straight into a
variant. Storage would fail both.

**FR-010a**: the return destination does not read `mode`, so it is the same whether the variant was
opened read-only or editable.

---

## 4. Running-experiment tag — unchanged by this work

**Gesture**: the green "running until …" tag in the UVE toolbar.
**Declared at**: `dot-ema-running-experiment.component.html` — a template `routerLink`.

| Switch | Destination | Requirement |
|---|---|---|
| off | `/edit-page/experiments/{pageId}/{id}/reports` | FR-017 |
| on | `/edit-page/experiments/{pageId}/{id}/reports` — **the same** | — |

The new portlet has no `:id/results` route: `lib.routes.ts` omits it deliberately, pending #37004,
"so the router surfaces an honest 404 instead of falling back to the legacy UVE screens". Switching
this tag would route an opted-in operator into a 404.

FR-017 constrains only the switch-**off** case and is satisfied by leaving the tag alone. Section D
governs the navigation *item*, not the tag, so no requirement goes unmet. Switching the tag belongs
with #37004. Flagged for the reviewer in [plan.md](../plan.md#open-items-for-the-reviewer).

---

## 5. Return to the page from the portlet (FR-024)

With the switch on, the page-filter chip on the list carries a link back to the editor for that
page:

```
/edit-page/content?url={path}&language_id={languageId}&com.dotmarketing.persona.id={persona}
```

The same builder as §2 with the variant, experiment and mode params omitted — one function, two
call sites, rather than a second URL assembler.

---

## Coexistence invariants (FR-025, FR-026)

Assertable against the route configs, with no flag involved:

| Invariant | Where |
|---|---|
| `dotExperimentsRoutes` (legacy) stays mounted under `edit-page/experiments` | `libs/portlets/edit-ema/portlet/src/lib/lib.routes.ts:93-117` |
| `dotExperimentsPortletRoutes` (new) stays mounted at `/experiments` | `apps/dotcms-ui/src/app/app.routes.ts:179` |
| Both are present in the same build; neither is removed | FR-025 |
| The portlet's main-navigation entry is untouched | FR-026 |
