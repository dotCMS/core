# Contract: the Allowed Blocks capability-key registry

Extends — does not replace —
`specs/36351-block-editor-link-setting/contracts/allowed-blocks-capability-keys.md`, which states
the rule in prose. **This document defines the enforceable form.**

## The rule

> A capability may be gated on Allowed Blocks **only if** the Settings tab can produce its
> identifier. A capability that is consulted but not producible is permanently forbidden the
> instant an administrator restricts the field to anything at all — an outcome nobody chose and
> nobody can undo. **Gating such a capability is always a defect, never a configuration.**

## The single producer

`getEditorBlockOptions()` — `core-web/libs/block-editor/src/lib/shared/utils/suggestion.utils.ts`,
exported from `@dotcms/block-editor` (`src/public-api.ts`).

**There is exactly one producer, and tests import it. Never copy the list into a fixture:** a copy
cannot detect drift in the thing it was copied from, which is the entire point.

## I1 — no gate on a non-producible key

Every key consulted via `store.isAllowed(...)` or carried as `BlockItem.blockName` **MUST** be in
`getEditorBlockOptions()`, or on the exemption list below.

**Scanned surfaces** (extend this list when a gate is added elsewhere — the textual half sees only
the files it is pointed at):

- `components/slash-menu/slash-menu-catalog.ts` — structural (factories → `blockName`)
- `components/toolbar/toolbar.component.html` — textual
- `components/toolbar/toolbar.component.ts` — textual
- `components/asset-by-url-popover/asset-by-url-popover.component.ts` — textual

**Exemptions** — a documented decision, never a way to silence a failure:

| Key | Reason | Mirrors |
|---|---|---|
| `paragraph` | Always permitted; removed from the Settings list by #29772 | `slash-menu.service.ts:112` |

**Must be red on `main` @ `667fc831ee` for exactly**: `youtube`, `aiContent`, `aiImage`.
**Must NOT report**: `link`, `emoji` — genuinely ungated by #37539 / #37442. Anchor the matcher to
`isAllowed(` / `has(` so popover ids (`popovers.isOpen('link')`) and mark lookups
(`m.type.name === 'link'`) are not mistaken for gates.

## I2 — no producible option without a destination

Every option `getEditorBlockOptions()` can emit **MUST** resolve to something real: a registered
extension, a gate, or a documented always-on capability.

**Must be red on `main` today for**: `aiContentPrompt`, `aiImagePrompt`.

## Resolution pattern for a violation

Established by `link` (#36351 → PR #37539) and `emoji` (#37340 → PR #37442):
**ungate the capability.** Do not add a Settings toggle — that is a product decision, not a defect
fix. If a capability genuinely should be restrictable, making it producible is a feature with its
own spec.

## Previously "known violations, out of scope" — now closed

| Key | Was | Now |
|---|---|---|
| `youtube` | known violation | **ungated** (defect B) |
| `aiContent` / `aiImage` | known violation | **key corrected** to `aiContentPrompt` / `aiImagePrompt` (defect C) |

## Why this exists

Six rounds in sixteen months, every one triaged as an isolated incident: #22164, #23764, #24370
(2022-08→2023-05) · #27101 (2024-01) · #29772 (2025-05) · #34110 (2025-12) · #35032 (2026-03,
[FD #35973](https://dotcms.freshdesk.com/a/tickets/35973)) · #36351, #37145, #37175, #37340
(2026-06→09).

The same workaround — *"uncheck everything under Allowed Blocks and save"* — went to four
customers, and a fifth ([FD #38349](https://dotcms.freshdesk.com/a/tickets/38349)) was **closed as
user error** while #36351 sat open and unfixed for two more months.

A prose contract did not stop any of it. A failing build will.
