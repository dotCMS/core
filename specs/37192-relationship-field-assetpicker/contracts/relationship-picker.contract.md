# Contract: `DotRelationshipPicker` — two consumers, one dialog

**Date**: 2026-09-10 · **Plan**: [../plan.md](../plan.md) · **Research**: [R2](../research.md)

This contract exists because #37192's spec treats the relationship dialog as the Relationship
field's own. **It is not.** It has a second consumer, in a different library, reached through an
injection token — and the refactor is only complete when that consumer works on the new picker.

---

## The published shape (must not change)

Defined in `libs/ui/src/lib/components/dot-filter-bar/chips/dot-field-filter/relationship-picker.token.ts`:

```ts
export interface DotRelationshipPicker {
    open(
        field: DotCMSContentTypeField,
        selectedInodes: string[]
    ): Observable<DotCMSContentlet[]>;
}

export const DOT_RELATIONSHIP_PICKER = new InjectionToken<DotRelationshipPicker>(/* … */);
```

`@dotcms/ui`'s field-filter chip injects it `{ optional: true }`. A surface that supplies nothing
still gets a working field filter for every other field type; only the Relationship type degrades,
and the control says so rather than failing to render.

**Why a token and not an import** — the reason is structural and applies to this whole feature:
the dialog lives in `@dotcms/edit-content`, which already depends on `@dotcms/ui` in ~98 files. A
shared chip importing it would make the dependency circular *and* drag `edit-content` into the
legacy custom-element bundle `@dotcms/ui` compiles into. The capability is therefore inverted: the
chip declares what it needs; whichever surface *can* satisfy it does.

---

## The two consumers

| | **Edit-time** — the Relationship field | **Filter-time** — Content Drive's field-filter chip |
|---|---|---|
| Entry point | `DotRelationshipFieldComponent.showExistingContentDialog()` | `createContentDriveRelationshipPicker()` |
| Lives in | `@dotcms/edit-content` | `@dotcms/portlets/dot-content-drive` |
| Question asked | "Which content is related to this one?" | "Which values should this filter match?" |
| Selection mode | from the field's cardinality | always `'single'` today |
| Cardinality / parent context | supplied | **absent by design** |
| "Already related to another parent" | enforced (FR-008) | not applicable — meaningless when choosing values to match |
| Confirm at zero selection | enabled **after FR-013** | **already enabled** — "clearing is a valid filter state" |
| Pre-selection | the field's related contentlets | `selectedInodes` from the chip |
| Cancel | `undefined`, field unchanged | translated to `[]` by the provider |

The token's own doc records why the edit-time context is absent filter-side:

> Cardinality and parent context are absent on purpose: those drive the edit-time "already related
> elsewhere" constraint that disables rows, which is meaningless when you are choosing values to
> match against.

---

## Obligations

### C1 — The shape is frozen
`open(field, selectedInodes) → Observable<DotCMSContentlet[]>` keeps its signature. Changing it means
changing `@dotcms/ui`, which is out of scope (plan Structure Decision).

### C2 — It always completes
A cancel completes with `[]`. A dialog that fails to open completes with `[]`. Callers get no
separate cancellation path, so a non-completing observable hangs the chip forever.

> Note the asymmetry with the edit-time path, which distinguishes `undefined` (cancel) from `[]`
> (confirmed empty). The **provider** absorbs that translation — `Array.isArray(items) ? items : []`
> — so the token's promise stays simple. The new dialog must keep emitting `undefined` on cancel for
> the field's sake, and the provider must keep translating it for the chip's.

### C3 — Filter-time tolerates missing edit-time context
`cardinality`, `parentContentTypeId`, `fieldVariable`, `isParentField` and
`currentContentIdentifier` are all absent for this consumer. The picker must then skip the
constrained-identifiers lookup entirely rather than compute it from partial inputs — the current
store already guards this with `shouldCheckConstraints`, and that guard must survive.

### C4 — Filter-time confirm stays enabled at zero
Already true, via `DotContentDriveRelationshipFooterComponent`. FR-013 brings the edit-time footer to
the same behavior. After that the two footers may differ only by label — collapsing them is a
progressive enhancement, not a requirement (research R3).

### C5 — Content Drive's behavior is unchanged
The chip is a shipped feature. Its dialog may look different after this work, but what it *returns*,
when it completes, and what the chip does with the result must not change.

---

## Migration steps

1. Build the new dialog behind the same `open()` shape.
2. Re-point `createContentDriveRelationshipPicker()` at it — `content-drive-relationship-picker.ts`
   is the only file in the portlet that needs to change.
3. Run the Content Drive relationship-picker spec **unchanged**. It passing against the new dialog is
   the acceptance signal for C1–C5.
4. Only then delete `dot-select-existing-content/` (FR-016).

**If step 3 needs the spec edited to pass, the contract has been broken** — stop and reconsider,
rather than adjusting the test to fit.

---

## Test obligations

| # | Assertion | Where |
|---|---|---|
| T1 | `open()` returns the selected contentlets | `content-drive-relationship-picker.spec.ts` |
| T2 | Cancel completes with `[]`, never hangs | same |
| T3 | A dialog that cannot open completes with `[]` | same |
| T4 | No constrained lookup runs without edit-time context (C3) | picker store spec |
| T5 | Filter-time confirm is enabled at zero selection (C4) | footer spec |
| T6 | Edit-time cancel emits `undefined`, distinct from a confirmed `[]` (C2) | picker component spec |

T1–T3 exist today and **must pass unmodified**. T4–T6 are new.
