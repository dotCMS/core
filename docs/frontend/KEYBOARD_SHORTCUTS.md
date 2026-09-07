# Keyboard Shortcuts

How keyboard shortcuts are arbitrated in the admin UI, what ships today, and how to add one.

Introduced with [#32591](https://github.com/dotCMS/core/issues/32591). Before it there was no shortcut
infrastructure at all: the only mechanism was a per-component `(document:keydown.x)` host binding,
which cannot arbitrate between two surfaces that want the same key.

## What ships today

| Combination | Does | Where it is claimed |
|---|---|---|
| `Mod + K` | Focus the search field | `dot-content-drive-search-input` |
| `Mod + B` | Show or hide the folder tree | `dot-content-drive-shell` |
| `Escape` | Clear the selection, then clear all filters | `dot-content-drive-shell` |
| `Escape` | Close the content side panel (wins while open) | `dot-edit-content-side-panel` |
| `↑` `↓` | Move focus between rows | the shared listing |
| `Shift + ↑` `Shift + ↓` | Extend the selection | the shared listing |
| `Shift + click` | Extend the selection to the clicked row | the shared listing |

`Mod` is the platform's primary modifier: Command on macOS, Control on Windows and Linux. One
registration covers both.

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
from.

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
same key both fire, and the registry has no say.

This is not hypothetical. The content side panel used to bind `(document:keydown.escape)` directly, so
once Content Drive claimed Escape both would have run: the panel would close *and* the listing behind
it would clear its selection. The panel was migrated to the registry for exactly this reason.

**If a component handles a key at document level, move it to the registry.** Handling a key on the
component's *own element* is fine and needs no change, because bubbling already resolves it.

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

## Related

- [Angular Standards](./ANGULAR_STANDARDS.md)
- [Testing Frontend](./TESTING_FRONTEND.md)
- Service and models: `core-web/libs/ui/src/lib/services/dot-keyboard-shortcut/`
