# Keyboard Shortcuts

How keyboard shortcuts are arbitrated in the admin UI, what ships today, and how to add one.

Introduced with [#32591](https://github.com/dotCMS/core/issues/32591). Before it there was no shortcut
infrastructure at all: the only mechanism was a per-component `(document:keydown.x)` host binding,
which cannot arbitrate between two surfaces that want the same key.

## What ships today

| Combination | Does | Where it is claimed |
|---|---|---|
| `/` | Focus the search field | `dot-content-drive-search-input`, `dot-asset-picker-toolbar` |
| `Mod + K` | Focus the search field (alias for `/`) | as above |
| `Mod + B` | Show or hide the folder tree | `dot-content-drive-shell` |
| `Escape` | Clear the selection | `dot-content-drive-shell` |
| `Escape` | Close the content side panel (wins while open) | `dot-edit-content-side-panel` |
| `↑` `↓` | Move focus between rows | the shared listing |
| `Shift + ↑` `Shift + ↓` | Extend the selection | the shared listing |
| `Shift + click` | Extend the selection to the clicked row | the shared listing |

`Mod` is the platform's primary modifier: Command on macOS, Control on Windows and Linux. One
registration covers both.

### Why these keys

`Mod + F` is the obvious "search" key and is deliberately **not** claimed: it would take away the
browser's find-in-page, which people rely on and which is an accessibility affordance in its own
right. `/` is the web convention for focusing a search box and collides with nothing.

`Mod + K` is kept as an alias rather than the primary because it conventionally opens a *command
palette*. If this application ever wants one, that key is still free.

`Escape` clears the selection and stops there. An earlier revision also cleared every active filter
once nothing was selected; that put a destructive, hard-to-undo action behind the most-reached-for
key on the keyboard. Clearing filters stays on the visible **Clear all** control.

### Bare printable keys and typing

`/` is a single character, so the registry ignores it while focus is in something that takes text —
an `input`, `textarea`, `select`, or anything inside a `contenteditable`. Without that rule, typing a
slash into the very search box the shortcut focuses would re-fire the shortcut instead of entering a
character.

The rule is narrow on purpose: it applies only to a one-character `key` pressed with no modifier. So
`Escape` and the arrows still reach their claimants while typing, and `Mod + K` still works from
inside the search box it focuses — otherwise pressing it twice would be a dead key.

This is the answer to the "editable targets" gap noted in review. A combination that carries a
modifier needs no such policy.

### Standing down while an overlay is above

**Every Content Drive claim declines when something is stacked over the listing** — `Escape` and
`Mod + B` in `dot-content-drive-shell`, and both search keys in
`dot-content-drive-search-input`. Each asks `hasOverlayAbove()` and returns `false`.

Two reasons, and the second is the one that is easy to miss:

1. A dialog covers the listing, so acting on it changes something the user cannot see. They find the
   selection gone, the tree rearranged, or focus sitting in a search box behind the modal when they
   close it.
2. PrimeNG's own `closeOnEscape` binds a **separate** document listener that never consults
   `defaultPrevented`, so it cannot be arbitrated with. See
   [A surface with its own document listener cannot be arbitrated](#a-surface-with-its-own-document-listener-cannot-be-arbitrated).

The search keys were the last to get this and the gap was caught in review rather than by a test.
`Mod + K` was the worse half: it carries a modifier, so the typing rule above never short-circuits
it and it fired from anywhere inside an open dialog, not only from a non-editable target.

**The AssetPicker's toolbar deliberately does not do this**, and that is not an oversight. It *is*
the top overlay, so asking the base-layer question would be true for its whole lifetime and the
picker would decline its own shortcut permanently. Its unconditional claim is correct, and
per-combination last-in-wins is what makes it safe: the picker takes both search keys for exactly as
long as it is open, then hands them back.

So before copying the guard into a new surface, decide which kind you are — see
[How to ask "is an overlay above me?"](#how-to-ask-is-an-overlay-above-me).

The labels shown to authors come from each registration's `label`, which is required. A shortcut
therefore cannot ship undocumented: `activeShortcuts()` is the live list.

## Adding a shortcut

```ts
readonly #shortcuts = inject(DotKeyboardShortcutService);

constructor() {
    const unregister = this.#shortcuts.register([
        {
            combination: 'mod+k',
            label: 'my-portlet.shortcut.search',
            handler: () => this.$searchInput()?.focus()
        },
        { combination: 'escape', label: 'my-portlet.shortcut.dismiss', handler: () => this.close() }
    ]);

    inject(DestroyRef).onDestroy(unregister);
}
```

Pass everything the surface wants in **one** call: a batch returns a single withdrawal, so there is
one thing to remember rather than one per shortcut.

Add the `label` key to `Language.properties`. It is what the author-facing documentation is generated
from, and **labels must be unique within a single `register()` call** — a duplicate throws, because
two shortcuts sharing a label collapse into one indistinguishable documentation entry and there is no
rule to resolve them.

Note the asymmetry: claiming the same **combination** twice is legal and resolved by the claim stack
(most recent wins). Claiming the same **label** twice is not, because nothing resolves it.

## The rules

**Per-combination, last-in-wins.** Each combination keeps its own stack of claims and the newest
receives the key. Withdrawing it restores the one beneath. This is what lets a dialog opening over a
portlet take the search shortcut and hand it back when it closes.

Deliberately *not* "the topmost surface wins outright". A dialog that claims one combination must not
swallow another it never claimed.

**One listener, bubble phase.** There is exactly one document listener for the whole application,
attached lazily on the first claim. It runs in the **bubble** phase and ignores any event already
marked handled.

That last part is what makes the registry compose with components that handle keys themselves. A rich
text editor handling `Mod + B` for bold is closer to the event target, runs first, and marks the event
handled, so the key never reaches arbitration. The registry needs no knowledge of rich text.

> Do not switch this to the capture phase. It is tempting for a "global" system, but it would see
> events before nested handlers, `defaultPrevented` would always be false, and the registry would
> start stealing keys from editors. Nothing would fail loudly.

**Prevention is scoped.** The browser default is suppressed only for a combination something actually
claims, so the registry can never eat a browser or assistive-technology key it does not own.

**Declining.** A handler returning `false` passes the key to the next claimant and then to the
browser. This is for runtime decisions only. The nesting case above is already handled by the bubble
phase and needs no cooperation.

## A surface with its own document listener cannot be arbitrated

`preventDefault` does not stop other listeners on the same node. Two document-level listeners for the
same key both fire, and the registry has no say. Order between them is **registration order**, not
focus and not DOM depth, so the registry usually wins simply because it attaches on its first claim.

Two ways this bites, and they need different answers.

### 1. Our own components

The content side panel used to bind `(document:keydown.escape)` directly, so once Content Drive
claimed Escape both would have run: the panel would close *and* the listing behind it would clear its
selection.

**Fix: move it to the registry.** Handling a key on the component's *own element* is fine and needs no
change, because bubbling already resolves it. Only document-level bindings need migrating.

### 2. PrimeNG overlays, which we cannot migrate

Every `<p-dialog closeOnEscape="true">` binds its **own** document keydown listener when it opens
(`primeng/fesm2022/primeng-dialog.mjs`, `bindDocumentEscapeListener`). It compares z-indexes and
**never consults `defaultPrevented`**, so it closes regardless of what the registry did. It is
third-party code, so it cannot be brought into the registry.

**Fix: a surface underneath must decline while an overlay is above it**, or Escape will both dismiss
the dialog and act on the listing behind it.

This is not only about Escape. Content Drive's `Mod + B` declines for the same reason: a dialog covers
the tree, so toggling it rearranges a layout the user cannot see and they meet it changed once the
dialog closes — and declining leaves the combination to whatever is on top, which may want it (a rich
text surface inside a dialog reads `Mod + B` as bold).

### How to ask "is an overlay above me?"

Use the shared helper, `hasOverlayAbove()` from `@dotcms/ui`:

```ts
#onEscape(): boolean {
    if (hasOverlayAbove()) {
        return false;
    }
    // ...
}
```

Every PrimeNG overlay registers itself in the shared `ZIndexUtils` stack when it opens, so comparing
the top of that stack against your own element's z-index answers the question without matching on
overlay class names, which differ per component and change across versions. It covers dialogs,
confirm popups, select panels and anything added later, rather than a list of visibility flags
somebody has to remember to extend.

Pass **your own overlay element** if you are one (the content side panel does). Omit the argument
from a base-layer surface such as a portlet shell: it has no element in the stack, `ZIndexUtils.get(undefined)`
is `0`, and the question becomes "is any overlay open at all" — which is the right question there.

> ⚠️ **In tests, mock `ZIndexUtils.getCurrent`.** The stack is a module-level singleton, and an
> overlay torn down without closing leaves its entry behind. That does not happen in the application,
> where overlays revert on close, but it does across tests in one file: the Content Drive shell suite
> reads **1102** with nothing visible once an earlier test has opened a dialog. Pin it in a
> `beforeEach` so every test states its own overlay state.

## Selection semantics in the shared listing

The listing follows the desktop file-manager model, not the ARIA APG listbox model. The difference
matters and was a deliberate choice.

- **Anchor-and-extend.** A range is always `anchor … focus` inclusive. `Shift + Arrow` moves focus and
  leaves the anchor put; a plain arrow or click moves both.
- **The anchor is always selected.** A range can shrink to one row but never to none. The anchor is
  the pivot the range turns through.
- **Ranges are additive.** Rows selected before the gesture began survive it. The range recomputes
  each step as `base ∪ range` rather than accumulating, which is what lets it shrink without
  stranding rows it passed over.
- **Page-bounded.** A range never spans pages, and no keyboard action paginates.

APG's listbox model instead toggles each row you land on, which can leave holes in the middle of what
looked like a contiguous drag. File managers avoid that, and this is a file manager.

### Known gap: the listing's ARIA is partial

Rows carry `aria-selected`, but the container keeps PrimeNG's `role="table"`. `aria-selected` is only
fully meaningful on a row inside `role="grid"`, so some assistive technology ignores it. That is a
recorded decision, not an oversight: `role="grid"` brings the grid keyboard contract with it, a screen
reader enters focus mode on a grid row, and Left/Right — which read cell by cell in browse mode today
— would then do nothing, because cell-level navigation is not implemented. Half a grid reads worse
than a table.

So: `aria-selected` stays (ignored by some, misleading to none, better than silence),
`aria-multiselectable` is deliberately absent (`role="table"` does not support it, so it would be
decoration), and the full wiring — cell navigation, `aria-rowcount`/`aria-colindex`, verification
against a real screen reader — is tracked in
[#37439](https://github.com/dotCMS/core/issues/37439). Do not add half of it.

## Testing shortcuts: synthesise the whole sequence

Three bugs shipped past a green suite during this feature because synthetic events were unlike real
ones. If you test keyboard or pointer behaviour, reproduce the browser's full event sequence:

- **Send the modifier's own keydown.** Holding Shift fires a `keydown` for `Shift` *before* the arrow.
  Omitting it hid a bug where shrinking a range could not deselect anything.
- **Set `cancelable: true`.** A constructed `MouseEvent` defaults to `false`, which silently makes
  `preventDefault()` a no-op. A checkbox then toggles on top of your range.
- **Send `mousedown` before `click`.** Components read modifiers from the mousedown, because it is the
  last thing before focus moves.

Also note that the table's own row handlers run *before* a template binding on the same row. Anything
that must observe a key before focus moves has to be in the capture phase.

Every one of those three was found by hand in a browser, not by the suite, which is why the shortcuts
also have a Playwright suite driving real key and pointer events:
`core-web/apps/dotcms-ui-e2e/src/tests/content-drive/content-drive-keyboard.spec.ts`. If you change
keyboard behaviour in the listing, add the case there as well as in Jest — a unit test can only prove
the handler does what you told it to, not that the browser calls it.

## Related

- [Angular Standards](./ANGULAR_STANDARDS.md)
- [Testing Frontend](./TESTING_FRONTEND.md)
- Service and models: `core-web/libs/ui/src/lib/services/dot-keyboard-shortcut/`
