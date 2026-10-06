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
| `youtube` | `asset-by-url-popover.component.ts:83`, `toolbar.component.ts:225` | ❌ | ⚠️ **Known violation, out of scope.** Separate issue. |
| `aiContent`, `aiImage` | `slash-menu-catalog.ts:468,476` | ❌ | ⚠️ **Known violation, out of scope.** The catalogue spells these `aiContentPrompt` / `aiImagePrompt`. Separate issue. |

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
