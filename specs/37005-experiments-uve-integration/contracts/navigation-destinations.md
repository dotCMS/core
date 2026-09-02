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
  &mode={EDIT | PREVIEW}
```

Navigated with `Router.navigate(['/edit-page/content'], { queryParams })` and **no**
`queryParamsHandling` — the portlet's URL carries `filter`/`orderby`/`pageAsset`, none of which
UVE wants.

### `mode` selection

`PREVIEW` when **any** of the following holds; `EDIT` otherwise:

| Condition | Source | Requirement |
|---|---|---|
| the variant is the control | `isControlVariant(variant)` | FR-008 |
| the experiment is not a draft | `$isLocked()` (`status !== DRAFT`) | FR-009 |
| the page is locked by another user | `$lockedByAnotherUser()` | FR-010 |

An **OR across all three** — never `!!$disabledTooltipKey()`, which reports only the strongest
reason and does not cover the control at all. When the page-lock branch is what triggered read-only,
the reason is stated to the user (FR-010) from the existing
`EXP_CONFIG_ERROR_LABEL_PAGE_BLOCKED` key.

`mode` sends `DotPageMode` values (`EDIT` / `PREVIEW`) — the values the legacy card sends and UVE
reads — **not** `UVE_MODE` (`'EDIT_MODE'` / `'PREVIEW_MODE'`). The two are distinct enums and the
wrong one fails silently into the wrong presentation, so the exact string is asserted.

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

| Switch | Destination | Query params | Requirement |
|---|---|---|---|
| **off** | `/edit-page/experiments/{pageId}/{experimentId}/configuration` | `mode`, `variantName`, `experimentId` → `null`; rest **merged** | FR-018 |
| **on** | `/experiments/{experimentId}/configuration` | `mode`, `variantName`, `experimentId` → `null`; **not merged** | FR-005, FR-006 |

**Resolved by the experiment, both ways.** The existing code already sources
`currentExperiment.id` from `store.pageExperiment()`, so US1 scenario 4 (a page hosting two
experiments) and the spec's "two experiments on one page" edge case are already handled — and the
on branch keeps that, because the new portlet's route
(`experimentsConfigureMatcher`) is keyed on `experimentId` alone and takes no `pageId` segment.

**The three nulls are the whole of FR-006** and are identical on both branches, so no variant,
experiment or mode parameter survives the return.

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
