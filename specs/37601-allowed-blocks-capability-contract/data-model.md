# Phase 1 — Data Model: the capability-key registry

No persisted entity, no database, no API payload. The "data model" here is the **set of
identifiers** that flows from the Settings tab to the editor's gates, and the relations that must
hold over it. Stating it as data is what makes it testable.

## Entities

### `CapabilityKey`

A string identifier naming a Block Editor capability.

| Field | Type | Notes |
|---|---|---|
| value | `string` | e.g. `bulletList`, `youtube`, `aiContentPrompt` |

### `ProducibleOption` — what Settings can write

Emitted by `getEditorBlockOptions()`
(`core-web/libs/block-editor/src/lib/shared/utils/suggestion.utils.ts:115`), exported publicly via
`core-web/libs/block-editor/src/public-api.ts:1`.

| Field | Type | Notes |
|---|---|---|
| **`code`** | `CapabilityKey` | the value persisted into the field's `allowedBlocks` |
| label | `string` | UI label, irrelevant to the invariants |

**Corrected during implementation**: the emitted field is **`code`**, not `id`. The source list
uses `id` internally, but `getEditorBlockOptions()` maps it —
`.map(({ label, id }) => ({ label, code: id }))` (`suggestion.utils.ts:115-123`). Reading `.id`
off the result yields `undefined` for every entry, which would make I1 pass vacuously. This is
exactly the kind of silent-green the invariant exists to prevent, so T030 verifies the failure
message is real.

`paragraph` is **defined** in that module but **filtered out** of the emitted list at
`suggestion.utils.ts:119` (per #29772), so it is *not* a `ProducibleOption`.

### `ConsumedKey` — what the editor asks about

A `CapabilityKey` passed to `store.isAllowed(...)` or carried as `BlockItem.blockName`.

| Origin | Location | Extraction |
|---|---|---|
| Slash-menu catalog | `slash-menu-catalog.ts` factories returning `BlockItem[]` | **structural** — call the factory, read `blockName` |
| Toolbar template | `toolbar.component.html` (12 sites) | **textual** — regex over the file |
| Toolbar component | `toolbar.component.ts` (14 sites) | **textual** |
| Asset-by-URL popover | `asset-by-url-popover.component.ts` (3 sites) | **textual** |

### `Exemption`

A `ConsumedKey` that is deliberately not required to be producible.

| Key | Why | Enforced at |
|---|---|---|
| `paragraph` | always permitted; removed from the Settings list by #29772 so a field can never exclude it | hardcoded at `slash-menu.service.ts:112` — `item.blockName === 'paragraph' \|\|` |

The exemption list in the tests must mirror this and stay minimal. **An exemption is a documented
decision, never a way to silence a failure.**

## Evaluation rule (existing, unchanged)

`editor.store.ts:54-57,104-107`:

```ts
allowedBlocksSet = blocks.length > 0 ? new Set(blocks) : null;   // empty => unrestricted
isAllowed(block) => !set || set.has(block);
```

Consequence, and the whole defect: while the list is empty every gate passes and a bad key is
invisible. The moment it is non-empty, a non-producible key is **false and unfixable by
configuration**.

## Relations that must hold

- **I1 — `ConsumedKey ⊆ ProducibleOption ∪ Exemption`.**
  Violated today by `youtube`, `aiContent`, `aiImage`.
- **I2 — every `ProducibleOption` resolves to a destination** (a registered extension, a gate, or
  a documented always-on capability).
  Violated today by `aiContentPrompt`, `aiImagePrompt`.

## Current state — verified at `667fc831ee`

Consumed keys *(list below is **incomplete** — `heading1`–`heading6` are also consumed; the
original grep's character class excluded digits. Corrected in red-gate-evidence.md)*:

```
aiContent aiImage audio blockquote bulletList codeBlock dotContent emoji gridBlock
horizontalRule image link orderedList paragraph table video youtube
```

| Key | Producible? | Gated? | Verdict |
|---|---|---|---|
| `youtube` | ✗ | ✓ | **I1 violation** — `toolbar.component.ts:225`, `asset-by-url-popover.component.ts:83` |
| `aiContent` | ✗ (Settings emits `aiContentPrompt`) | ✓ | **I1 violation** — `slash-menu-catalog.ts:468` |
| `aiImage` | ✗ (Settings emits `aiImagePrompt`) | ✓ | **I1 violation** — `slash-menu-catalog.ts:476` |
| `paragraph` | ✗ | exempt | OK — mirrors `slash-menu.service.ts:112` |
| `aiContentPrompt` | ✓ | — | **I2 violation** — nothing consumes it |
| `aiImagePrompt` | ✓ | — | **I2 violation** — nothing consumes it |
| `link`, `emoji` | ✗ | **not gated** | OK — ungated by #37539 / #37442. **Must not be reported.** |
| all others | ✓ | ✓ | OK |
