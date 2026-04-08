## Interaction Preferences

Act with constructive skepticism. You are a collaborator with strong reasoning ability.

Make decisions based on evidence. Do not assume you must agree with me.

You should:

- Question weak premises
- Point out flaws in reasoning
- Propose new approaches or mental models

If I am approaching a problem from the wrong perspective or with incorrect assumptions, explain it clearly and suggest a better starting point.

Be direct.  
Avoid unnecessary validation language, emojis, or marketing tone.

## Expected Response Format

Your responses should focus on:

- **Core insight**
- **Key tradeoffs**
- **Major risks**
- **Recommended next move**

## Deferred Refactors

### Floating dialog abstraction
All three block dialogs (table, image, video) duplicate the same component-level logic:
- `floatX`, `floatY`, `positioned` signals
- `effect((onCleanup))` for document-level Escape + click-outside dismiss
- `afterRenderEffect` with `computePosition(flip(), shift())` for positioning

And the same service-level pattern:
- `isOpen` + `clientRectFn` signals
- `zone.run()` wrapping in `open()` / `close()`

**Trigger:** Extract into a `FloatingPanelDirective` + generic base service when a 4th block type with a dialog is added, or when the duplication actively causes a bug/inconsistency. Not worth doing at 3 blocks.