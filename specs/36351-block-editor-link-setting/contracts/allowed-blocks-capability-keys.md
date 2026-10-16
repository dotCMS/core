# Contract — Allowed Blocks capability keys

**Feature**: Block Editor 2.0 — hyperlinks stop depending on Allowed Blocks
**Date**: 2026-09-14

The Block Editor has no external API contract to change here. Its one internal contract is the
agreement between the surface that **writes** capability identifiers and the surfaces that
**read** them. That agreement was never written down, which is how the same bug shipped three
times. This document states it and records the state before and after this change.

## The rule

> **A capability may be gated on Allowed Blocks only if the Settings tab can produce its identifier.**

A capability that is consulted but not producible is permanently forbidden the instant an
administrator restricts the field to anything at all — an outcome nobody chose and nobody can undo.
Gating such a capability is always a defect, never a configuration.

Established by **#37175**, applied to emoji by **#37340**, applied to hyperlinks here.

## Producible identifiers

Everything `getEditorBlockOptions()` can emit
(`core-web/libs/block-editor/src/lib/shared/utils/suggestion.utils.ts:115`).
**Unchanged by this feature.**

```
aiContentPrompt   aiImagePrompt   audio        blockquote    bulletList
codeBlock         dotContent      gridBlock    heading1      heading2
heading3          heading4        heading5     heading6      horizontalRule
image             orderedList     table        video
```

`paragraph` is filtered out of the catalogue and treated as always-permitted by consumers.

## Consulted identifiers — after this change

| Identifier | Consulted at | Producible | Status |
|------------|--------------|------------|--------|
| `audio`, `blockquote`, `bulletList`, `codeBlock`, `horizontalRule`, `image`, `orderedList`, `table`, `video` | toolbar template + `editor-extensions.ts` | ✅ | Correct |
| `dotContent`, `gridBlock` | `editor-extensions.ts` | ✅ | Correct |
| `heading1`…`heading6` | toolbar block-type select + `editor-extensions.ts` | ✅ | Correct |
| `paragraph` | slash-menu filter | n/a | Correct — special-cased as always permitted |
| ~~`link`~~ | ~~toolbar template, `editor-extensions.ts`~~ | ❌ | **Removed by this change.** No site consults it. |
| `emoji` | — | ❌ | Already removed by #37340. Only explanatory comments remain. |
| `youtube` | — (ungated) | ❌ | ✅ **Resolved** in #37601. Ungated in both sites, following the `link` (#36351) and `emoji` (#37340) precedent. Enforced by `capability-keys.i1.spec.ts`. |
| `aiContentPrompt`, `aiImagePrompt` | `slash-menu-catalog.ts` | ✅ | ✅ **Resolved** in #37601. The keys were corrected (not ungated — these two ARE producible, so the gate is legitimate). Enforced by `capability-keys.i1.spec.ts` **and** `capability-keys.i2.spec.ts`. |

## What this change alters

Three sites stop consulting `link`. Nothing is added to either column.

| Site | Before | After |
|------|--------|-------|
| `toolbar.component.html:373` | `@if (isAllowed('link'))` wraps the button | No condition — the button always renders |
| `editor-extensions.ts:134-135` | `autolink: has('link')`, `linkOnPaste: has('link')` | `autolink: true`, `linkOnPaste: true` |
| `link.extension.ts:43-47` | `addPasteRules()` returns the parent rules only when `autolink \|\| linkOnPaste` | Override deleted; the parent's rules apply unconditionally |

## Invariants a reviewer can check

1. **`link` appears in no gating expression.** `grep -rn "isAllowed('link')\|has('link')"` over
   `core-web/libs/new-block-editor/src` returns nothing outside comments and specs.
2. **The `link` mark is still registered unconditionally.** Removing it from the schema aborts
   `Node.fromJSON` and blanks the whole document (#37175). This change touches the authoring gate,
   never the registration.
3. **No producible identifier lost its gate.** Every row marked ✅ above still gates. This is spec
   FR-006 and is covered by a test.
4. **The producible set is unchanged.** `suggestion.utils.ts` is not in the diff — which is also
   what keeps the legacy editor's slash menu untouched (spec FR-009).


---

## Enforcement (added by #37601)

This document stated the rule in prose from #37539 onward. It did not stop the class recurring:
the same defect was patched ad hoc four times in ten weeks and six times in sixteen months, the
same workaround went to four customers, and a fifth ticket
([FD #38349](https://dotcms.freshdesk.com/a/tickets/38349)) was closed as *user error* while
#36351 sat open and unfixed for two more months.

Two vitest specs now enforce it, in `core-web/libs/new-block-editor/src/lib/editor/extensions/`:

| Spec | Invariant | Fails when |
|---|---|---|
| `capability-keys.i1.spec.ts` | Every consulted key is producible, or explicitly exempt | A gate names something the Settings tab cannot write |
| `capability-keys.i2.spec.ts` | Every producible option has a consumer | Settings offers a checkbox that resolves to nothing |

Shared extraction lives in `capability-keys.testing.ts`. Two things about it are load-bearing:

- **The producer is imported, never copied.** `getEditorBlockOptions` comes from
  `@dotcms/block-editor` (`public-api.ts`). A fixture copy cannot detect drift in its own original.
  Note the field is `code`, not `id` — reading `.id` yields `undefined` and the invariant passes
  vacuously.
- **Comments are stripped before the textual scan.** These files carry long explanations of why a
  capability is no longer gated, quoting the removed call verbatim. Scanned raw, the prose reads as
  a live gate, and the quickest way to green would be deleting the explanation of why the bug
  happened.

**Known limitation:** the textual half reads only the files in `SCANNED_FILES`. A gate added in a
new file escapes it — extend the list when that happens.

**Resolution pattern.** If a key is not producible, **ungate the capability** (`link` #37539,
`emoji` #37442, `youtube` #37601). Do not add a Settings toggle: making a capability restrictable
is a product decision with its own spec, not a defect fix. If the key is merely *wrong* and the
capability genuinely is producible, fix the key (`aiContentPrompt` / `aiImagePrompt`, #37601).
