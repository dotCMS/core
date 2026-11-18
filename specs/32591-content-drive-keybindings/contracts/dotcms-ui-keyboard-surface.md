# Contract: `@dotcms/ui` keyboard surface

**Status**: proposed with this feature (issue #32591)
**Scope**: the public surface added to the shared `@dotcms/ui` library. Committed because `libs/ui` is
consumed across teams, so these additions are a cross-team contract rather than an internal detail.

**Compatibility statement**: **additive only.** Nothing is removed or renamed. A consumer that ignores
everything below behaves exactly as it does today, with one intended exception called out under
*Breaking-adjacent change* at the end.

---

## 1. Keyboard shortcut registry (new)

A single injectable that arbitrates key combinations between surfaces that can be on screen at the
same time.

### Guarantees

```ts
register(shortcuts: DotKeyboardShortcut | DotKeyboardShortcut[]): DotKeyboardShortcutUnregister
```

| Guarantee | Detail |
|-----------|--------|
| One listener | Exactly one document-level key listener exists for the whole application, however many shortcuts are registered. It is attached lazily on the first claim and detached when the last is withdrawn, so an application registering nothing pays nothing. |
| Per-combination last-in-wins | Each combination keeps its own claim stack. The **most recent claim on that combination** receives the key. A surface that claims one combination never swallows another it did not claim. |
| Restoration | Withdrawing a claim returns the combination to the one beneath it. |
| One call, one withdrawal | A batch registers many combinations and returns a **single** withdrawal covering all of them, so a caller never has to track a handle per shortcut. Idempotent, and an empty batch is harmless. |
| Lifetime | A registration never outlives the surface that made it. |
| Bubble phase | The listener runs in the bubble phase and ignores an event already marked handled. A component that handles a key on its own element is closer to the event target, runs first, and never reaches arbitration. Listening in the capture phase would break this silently and must not be introduced. |
| Scoped prevention | The browser default is prevented **only** for a combination an active claim actually holds. The registry never swallows a key it does not own. |
| Declining | A handler returning `false` declines, and the key falls through to the next claimant and then to the browser. For runtime decisions only; the nesting case is already handled by the bubble phase above. |
| Platform-neutral modifier | `mod` matches either Command or Control, so one registration serves macOS, Windows and Linux. |
| Labels are mandatory | Every registration carries an i18n label, and `activeShortcuts()` exposes the live set. A shortcut cannot ship undocumented. |

### Consumer obligations

- Register on surface open and call the returned withdrawal on destroy. Pass every combination the
  surface wants in one call so there is exactly one thing to remember.
- Decline rather than pre-filter when the surface conditionally does not want a key. A component that
  handles the key on its own element needs no cooperation at all: the bubble phase already resolves it.

### Non-goals

- Not a command palette, and not a place to store application state. It arbitrates live DOM events
  and holds nothing across a surface's lifetime.
- No in-application help surface. Labels exist for author-facing documentation (FR-023).

---

## 2. `dot-search-input` — public `focus()` (new)

Moves focus to the underlying text field.

| Guarantee | Detail |
|-----------|--------|
| Preserves the term | Focusing never clears or alters the current value. |
| Idempotent | Calling it while the field already has focus is a no-op and disturbs nothing. |
| Presentational only | The component gains no knowledge of shortcuts. The host registers the shortcut and calls this; the component stays reusable without the registry. |

The existing `value` input, `search` output, `placeholder`, `debounceTime` and `testId` are unchanged.

---

## 3. `dot-folder-list-view` — keyboard behaviour

No new inputs or outputs are required for the keyboard behaviour itself; it is a behavioural
contract on the existing component. The existing `selection` input and `selectionChange` output
remain the only selection channel.

| Guarantee | Detail |
|-----------|--------|
| Single tab stop | A **multiple-selection** listing presents exactly one row as a tab stop, not one per row. Single-selection listings keep their existing behaviour, unchanged by this feature. |
| Tab stop survives state changes | Sort, page change, page-size change, filter change and search all leave the listing keyboard-reachable with no pointer input needed to recover it. |
| Arrows move focus only | Arrow up/down change which row is focused and never change the selection. |
| Page-bounded | Focus stops at the first and last row of the page. No keyboard action triggers pagination. |
| Non-actionable rows skipped | Rows that cannot be acted on are stepped over, never landed on. |
| Anchor-and-extend | Shift+Arrow and Shift+click on a checkbox extend a contiguous range from the anchor. A plain arrow or click re-anchors. |
| Selection reported once | However the selection changes, it is reported through `selectionChange`, and the rows reported always match the rows rendered as selected. |
| Read-only is inert | With `readOnly` set, no row takes focus and no keyboard or shift-click action changes a selection. |
| Single-selection unaffected | With `selectionMode="single"`, range extension does not apply. |

### Breaking-adjacent change (read this one)

Row `tabindex` behaviour changes in **two of the three** current consumers. Measured, not assumed:

| Consumer | Before | After |
|---|---|---|
| Content Drive (`selectionMode="multiple"`) | every row a tab stop; **none reachable at all** after a column sort | exactly one tab stop, and it survives sorting |
| Action preview (`readOnly`) | every row a tab stop, despite nothing being selectable | no tab stop, correctly inert |
| Asset selection dialog (`selectionMode="single"`) | every row a tab stop | **unchanged** |

The single-selection case is unchanged because the table short-circuits to "every row is tabbable"
whenever the selection is empty, which is the permanent resting state of a single-selection listing.
It is therefore **not a regression** — that consumer behaves exactly as it does on trunk — but it is
also not yet the improvement FR-001 describes. Tracked as an open scope decision, not a defect.

This is a fix, not a redesign, but any consumer or test asserting the current `tabindex` values will
need updating. No such assertion exists today: the repository contains no keyboard tests for this
component, which is precisely why its existing suites could not be cited as no-regression evidence.
