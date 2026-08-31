# Block Editor: you can't click an embedded contentlet on older content

**Branch**: `issue-36985-block-editor-embedded-contentlet-cannot-be-selected-when-text-or-other-content-precedes-it-spec`
**Created**: 2026-08-28 · **Revised**: 2026-08-31 · **Status**: Draft — **re-review required** · **Type**: Issue / Bug Resolution
**GitHub**: [#36985](https://github.com/dotCMS/core/issues/36985) (reopened) · prior fix [PR #37104](https://github.com/dotCMS/core/pull/37104) · spec [PR #37285](https://github.com/dotCMS/core/pull/37285) · related [#37145](https://github.com/dotCMS/core/issues/37145), PR #37149

<!--
  dotCMS ISSUE-RESOLUTION spec (/speckit-specify-fix). The mandatory H2 headings below are the
  contract with /speckit-plan and /speckit-tasks — keep them and their order. Readable framing
  lives inside them as leads and H3s.
-->

> **Revision note (2026-08-31).** This spec was approved on an earlier, partly wrong theory. Two
> of its claims have since been disproved in a running browser and are now corrected: nothing
> re-pushes the value, and the host asymmetry is not about which branch of the comparison runs.
> The root cause is now measured end to end and the fix scope has changed as a result. Sections
> rewritten: [Root-Cause Hypothesis](#root-cause-hypothesis),
> [Fix Scope & Non-Goals](#fix-scope--non-goals-mandatory),
> [Acceptance & Verification](#acceptance--verification-mandatory), and all of
> [Reproduction](#reproduction-mandatory). **This needs a second approval before
> `/speckit-tasks`.**

---

## Problem Statement *(mandatory)*

### The short version

In the new Block Editor, clicking an embedded contentlet card does nothing. No selection ring,
and the toolbar's edit / delete / reorder buttons stay greyed out.

The click handler isn't broken — it creates the selection correctly. What happens next is that
clicking the card causes an Angular effect to re-run, that effect decides the content has changed
when it hasn't, and it rebuilds the entire document. The rebuild throws the selection away, a few
milliseconds after it was made.

Three things have to be true at once for you to see it, and that is why it took so long to pin
down:

1. **The stored JSON doesn't round-trip byte-for-byte** through the current schema — so a
   "has the content changed?" check answers *yes* when the honest answer is *no*.
2. **The card you click has an Angular node view.** Selecting it writes an input on that node
   view, which marks every ancestor Angular view dirty and re-runs the effect. Only two nodes in
   the editor are built this way.
3. **The host bound the `value` input.** On screens that don't, the effect re-runs too, but exits
   immediately with nothing to do.

Take away any one of the three and the bug disappears. The full mechanism, with measurements, is
in [Root-Cause Hypothesis](#root-cause-hypothesis).

### Why it looks random

Authors report that some contentlets are selectable and some aren't, on the same site, in the
same content type. That's real, and it's the most confusing part of the bug.

Condition 1 above is a property of each individual contentlet — the shape of the JSON sitting in
the database, which depends on when and how that row was last written. Content saved by the
current editor round-trips cleanly and works. Content saved by an older editor, an importer, or
the REST API often doesn't. Two rows in the same content type can differ, so two cards on the
same site behave differently.

**Severity / Impact**: customer-blocking on Evergreen. Every author editing pre-existing Story
Block content with embedded contentlets is affected. The documented workaround — switching back
to the legacy editor — does not help; see [Non-goals](#non-goals).

---

## Reproduction *(mandatory)*

**Environment**: local dotCMS with the PR #37104 fix present (commit `d85346613c`, first tagged
`v26.08.19-01`), `FEATURE_FLAG_NEW_BLOCK_EDITOR` on (the default). No customer data needed.

> **You must use the legacy JSP contentlet editor.** The content type needs
> `CONTENT_EDITOR2_ENABLED=false` in its metadata. The new Angular Edit Content screen will not
> reproduce this — see [Why only one screen is affected](#why-only-one-screen-is-affected).

Every route below needs the same structural precondition: **at least one block above the card**,
and a contentlet embedded in a Story Block field.

### Option A — the flag round-trip, no hand-written JSON

The simplest route, and the one to give QA. It works because the legacy editor still writes an
attribute the new editor drops.

1. Set `FEATURE_FLAG_NEW_BLOCK_EDITOR=false`.
2. In the legacy editor, author a Block Editor field with this structure — **the list is the part
   that matters**:

   ```
   # Heading 1

   text testing

   - one
   - two

   [insert a dotContent]

   Text testing
   ```
3. Save and Publish.
4. Confirm the `dotContent` card *is* clickable. This is your control.
5. Set `FEATURE_FLAG_NEW_BLOCK_EDITOR=true` (or remove it).
6. Reopen the same content in the JSP editor.
7. The card is no longer selectable on click — though dragging a text range across it still
   selects it.

**Why the list is required.** Without it, content authored on today's legacy editor round-trips
byte-identically and the bug does not appear — measured. The legacy editor configures `TextAlign`
with `['heading','paragraph','listItem','dotImage']`
(`libs/block-editor/src/lib/components/dot-block-editor/dot-block-editor.component.ts:736`); the
new editor uses `['heading','paragraph']`
(`libs/new-block-editor/src/lib/editor/extensions/editor-extensions.ts:151`). So legacy writes
`listItem.attrs.textAlign` and the new editor drops it on parse:

```
legacy: {"type":"listItem","attrs":{"textAlign":null},"content":[…]}
new   : {"type":"listItem","content":[…]}
```

This route exercises a **live** schema divergence rather than a historical one, which makes it
both easier to reproduce and evidence that the bug is not confined to very old content.

### Option B — seeded A/B pair, for precise work

Two contentlets that differ **only** in stored JSON shape, so you can A/B the defect against a
control. This is the route that matches what customers actually hit (pre-2025 content), and it is
what the acceptance criteria are written against.

Create both **through the API, not the editor** — that is what preserves the legacy shape. Fire
each body, stringified into the Block Editor field, at
`/api/v1/workflow/actions/default/fire/PUBLISH`:

```bash
curl -u admin@dotcms.com:admin -X POST \
  'http://localhost:8080/api/v1/workflow/actions/default/fire/PUBLISH' \
  -H 'Content-Type: application/json' \
  -d '{"contentlet": {"contentType":"<CT variable>","title":"Repro 36985 BROKEN","<blockFieldVar>":"<body JSON as a string>"}}'
```

**BROKEN body** — root `chartCount` typo, and no `indent` on the heading or paragraphs:

```jsonc
{
  "type": "doc",
  "attrs": { "chartCount": 118, "wordCount": 20, "readingTime": 1 },
  "content": [
    { "type": "heading",   "attrs": { "textAlign": "left", "level": 2 },
      "content": [{ "type": "text", "text": "Heading above the card" }] },
    { "type": "paragraph", "attrs": { "textAlign": "left" },
      "content": [{ "type": "text", "text": "Some text before the contentlet." }] },
    { "type": "dotContent",
      "attrs": { "data": { "identifier": "<embedded-contentlet-identifier>", "languageId": 1 } } },
    { "type": "paragraph", "attrs": { "textAlign": "left" },
      "content": [{ "type": "text", "text": "Some text after the contentlet." }] }
  ]
}
```

**CONTROL body** — the same document in the shape the current editor emits: root `attrs` uses
`charCount` (no `t`), and every heading and paragraph carries `"indent": 0` **positioned between
`textAlign` and `level`**. The position matters; see
[Four confirmed triggers](#four-confirmed-triggers).

Then open **BROKEN** from the Content portlet and click the card; repeat with **CONTROL** for the
contrast.

> A seeding script is attached to the
> [QA note on the issue](https://github.com/dotCMS/core/issues/36985#issuecomment-5456242587).
> It is deliberately not committed — the steps above are the source of truth.

### Option C — no server at all

Round-trip a legacy-shaped document through the real schema and compare it the way the check
does. This is what the unit tests will assert.

1. Build the schema from the editor's own extension list:
   `getSchema(createEditorExtensions(...))` from
   `libs/new-block-editor/src/lib/editor/extensions/editor-extensions.ts`.
2. Parse and re-serialize a heading whose stored attrs are `{ textAlign, level }` — no `indent` —
   via `Node.fromJSON(schema, stored).toJSON()`.
3. Compare `JSON.stringify(stripDocStats(stored))` against
   `JSON.stringify(stripDocStats(parsed))`.

The strings differ. The editor's side has gained `indent: 0`, sitting between `textAlign` and
`level`.

### What you should and shouldn't see

**Expected Behavior**: the card takes a `NodeSelection` and keeps it. Ring visible, toolbar
actions enabled, card can be edited, deleted or reordered.

**Actual Behavior**: the selection is created and immediately discarded. The document is
rebuilt, the cursor collapses to the end, no ring, "Edit Contentlet" stays disabled.

**Reproducibility**: always, for content matching the data condition — any one trigger is enough.
Never, for content last saved by the new editor.

Two observations that are diagnostic rather than incidental:

- **Dragging a text range across the card still selects it.** That distinguishes this from the
  original #36985 and is explained by the node view, not by the data — see
  [Why clicking is what triggers it](#why-clicking-is-what-triggers-it).
- **Clicking an image, video, table or grid on the same broken document works fine.** Only
  `dotContent` and `codeBlock` are affected.

### Confirming it

Do **not** paste a snippet into the console to verify this. An earlier draft of this spec did,
and the snippet was wrong in a way that silently reported success — it read
`document.querySelector('.ProseMirror').pmViewDesc.view`, and `ViewDesc` sets
`dom.pmViewDesc = this` but never stores the view (prosemirror-view 1.41.8,
`dist/index.js:741-753`), so the comparison was `undefined === undefined`.

Use one of these instead:

- **Automated** — the Jest harness described in [How we'll verify](#how-well-verify). It mounts a
  real `DotCMSEditorComponent`, loads a BROKEN body, performs the same
  `chain().focus().setNodeSelection(pos).run()` the card does, runs change detection, and asserts
  the selection survived. This reproduces the defect headlessly and is the Red-phase test.
- **Manual, with a temporary instrumented build** — how the root cause was actually confirmed.
  Add a log to the effect at `editor.component.ts:619` reporting its run number and the check's
  verdict, build with
  `pnpm nx build dotcms-block-editor --configuration=development` (the container volume-mounts
  `core-web/dist/apps/dotcms-block-editor` — `dotCMS/pom.xml:2385`), and click the card. On
  BROKEN content you will see a second effect run calling `setContent`. On CONTROL you will not.
  Revert and rebuild afterwards.

---

## Scope of Investigation *(mandatory)*

**Affected area**: content editing — the new Block Editor (`core-web/libs/new-block-editor`).
Two things are in scope: the value-synchronisation effect in `editor.component.ts`, and the
comparison function it calls. The hosts that bind it are the legacy JSP contentlet editor (the
iframe behind both the UVE contentlet dialog and the Content portlet), the UVE side panel, and
the Angular Edit Content screen.

**Suspected surface**: frontend only, TypeScript under `libs/new-block-editor`. No Java change
expected — nothing under `com.dotcms.*` or `com.dotmarketing.*`. The legacy JSPs
(`edit_field.jsp`, `edit_contentlet.jsp`) only *host* the web component and shouldn't need
touching.

**Third-party code involved but not modified**: `ngx-tiptap@14.0.1` (the Angular node-view
renderer), `@tiptap/core@3.22.2` (the `setContent` command) and `@angular/core@22.1.0` (the
view-dirtying path). The fix works around their behaviour rather than changing it; specific line
references are in [Root-Cause Hypothesis](#root-cause-hypothesis).

**Related known decisions**: none identified yet; the plan phase formally consults
`dotCMS/platform-adrs`. Worth flagging that the emitted root `charCount` / `wordCount` /
`readingTime` attrs are a deliberate legacy-parity contract that downstream consumers read — any
change must preserve them on the emit path.

---

## Root-Cause Hypothesis

Three independent things combine. It's worth naming them separately, because each one is a place
the bug could be cut, and the fix deliberately cuts two of them.

| | Role | What it is |
|---|---|---|
| **The loaded gun** | makes the check answer wrongly | Stored JSON that doesn't round-trip byte-for-byte through the current schema |
| **The trigger** | re-runs the effect | Clicking a node that has an **Angular node view**, which writes an input and marks every ancestor view dirty |
| **The bullet** | destroys the selection | `setContent` replaces the **entire** document, not the part that changed |

### What's actually happening

Step by step, when you click the card:

1. The card's `mousedown` handler (`contentlet.component.ts:23,121`) runs
   `chain().focus().setNodeSelection(pos).run()`. A `NodeSelection` on the `dotContent` node now
   exists. **This is correct and stays correct.**
2. ProseMirror tells the node view it is selected. Because that node view is an Angular
   component, `ngx-tiptap` calls `selectNode()` → `renderer.updateProps({ selected: true })` →
   `componentRef.setInput(...)`.
3. Angular's `setInput` ends in `markViewDirty`, which walks **every ancestor LView** setting the
   dirty flag — including the editor component's own view.
4. On the next view refresh Angular runs `runEffectsInView`, and the value-synchronisation effect
   at `editor.component.ts:619` runs again.
5. That effect calls `editorContentMatchesParsed()` to decide whether the incoming value differs
   from what the editor holds. On this content it answers `false` — "they're different" — even
   though they aren't.
6. `setContent(..., { emitUpdate: false })` runs at `:626`.
7. `setContent` performs `tr.replaceWith(0, doc.content.size, newDoc)` — it throws away the whole
   document and substitutes a freshly parsed one, even though the content is identical.
8. ProseMirror maps the selection through that replace. A node-boundary anchor cannot survive a
   replace spanning it, so the `NodeSelection` degrades to a `TextSelection` at the end of the
   document.
9. The toolbar is gated on `selection instanceof NodeSelection && selection.node.type.name === 'dotContent'`
   (`editor-toolbar.store.ts:98`), so its contentlet actions go dark.

Measured in the browser on a real affected contentlet:

```
value effect run #1 -> guardMatched=false ... CALLING setContent()      ← load, harmless
node-view mousedown, pos = 69
after setNodeSelection: selection = NodeSelection from = 69             ← correct
value effect run #2 -> guardMatched=false ... CALLING setContent()      ← the clobber
```

and in Jest, the transaction itself:

```
TX steps=["ReplaceStep"] docChanged=true selFrom=30 meta=["preventUpdate"]
        at editor.component.ts:626:25
docSize before=31 after=31        ← identical content, brand-new document
NodeSelection from=23  →  TextSelection from=30
```

Note `docSize before == docSize after`. Nothing changed. The document was replaced anyway.

Nothing here is a rendering problem. The selection genuinely existed and was genuinely destroyed.

### Where the code lives

| What | Where |
|---|---|
| The effect that re-runs | `libs/new-block-editor/src/lib/editor/editor.component.ts:618-632` |
| The comparison it calls | `editor.component.ts:137-155` (`editorContentMatchesParsed`) |
| The destructive call | `editor.component.ts:626` |
| Other callers of the comparison | `writeValue` (`:738`, `setContent` at `:748`), `commitEditor` (`:488`) |
| The `value` input | `editor.component.ts:321` — `input<string>('')` |
| Where the editor signal is set (once) | `editor.component.ts:489` |
| The normalizer | `utils/doc-stats.utils.ts:4,19` (`DOC_STAT_ATTRS`, `stripDocStats`) |
| The emit contract | `editor.component.ts:713` (`withDocStats`) |
| The card's click handler | `extensions/nodes/contentlet/contentlet.component.ts:23,121` |
| The Angular node view | `extensions/nodes/contentlet/contentlet.extension.ts:91` |
| The other Angular node view | `extensions/nodes/code-block/code-block.extension.ts:20` |
| Toolbar gating | `components/toolbar/editor-toolbar.store.ts:98` |
| Selection ring styling | `editor.component.css:272` |

Third-party, for reference:

| What | Where |
|---|---|
| `setContent` → full-document replace | `@tiptap/core@3.22.2/dist/index.js:1148`, replace at `:1157` |
| Node view created with the host's injector | `ngx-tiptap@14.0.1/fesm2022/ngx-tiptap.mjs:240,243` |
| Selection → `setInput` | `ngx-tiptap.mjs:337` (`handleSelectionUpdate`), `:403` (`selectNode`), `:258` (`updateProps`) |
| `setInput` → `markViewDirty` | `@angular/core@22.1.0/fesm2022/_debug_node-chunk.mjs:9242` |
| `markViewDirty` walks all ancestors | `_debug_node-chunk.mjs:6205-6217` |
| `runEffectsInView` | `_debug_node-chunk.mjs:5920-5940` |

### Why the check answers wrongly

Here is the whole of it:

```ts
JSON.stringify(stripDocStats(incoming)) === JSON.stringify(stripDocStats(editor.getJSON()))
```

Two problems with comparing JSON as text:

- **Key order counts.** `{"textAlign":…,"level":…}` and `{"textAlign":…,"indent":…,"level":…}`
  are different strings even when they describe the same document.
- **`stripDocStats` barely normalizes anything.** It touches only the root `attrs`, and only
  three keys inside it. Everything below the root is compared raw.

Meanwhile TipTap normalizes heavily when it parses: it fills in schema defaults, drops attributes
the schema doesn't declare, and reorders keys to schema-declaration order. So the editor's side
of the comparison is always the normalized shape, and the stored side is whatever happened to be
written years ago.

### Four confirmed triggers

All four measured against the real schema built from the editor's own extension list.

**1. The `chartCount` typo.** The legacy editor wrote `SetDocAttrStep('chartCount', …)` — with an
extra `t` — until commit `3cd5549d35` (#26025, **2023-09-11**). `stripDocStats` strips
`charCount`, `wordCount` and `readingTime`. It does not strip `chartCount`, and the current
schema doesn't declare it, so the key survives on the stored side and vanishes on the editor
side.

**2. Missing `indent`.** `IndentExtension` (`extensions/indent.extension.ts:19,81`) declares
`indent` with a default of `0` on `heading`, `paragraph` and `blockquote`. The **legacy** editor
only gained the same declaration in commit `5eb1a74e05` (#32235, **2025-05-27**), so anything
authored before that date has no such key and the new editor adds it on parse.

| Stored heading attrs | Check result |
|---|---|
| `{textAlign, level}` — legacy shape | ❌ `false` |
| `{textAlign, level, indent}` — key added, but last | ❌ `false` |
| `{textAlign, indent, level}` — current shape | ✅ `true` |

That middle row is the one worth staring at. Supplying the missing key isn't enough — it has to be
in the right *position*. This isn't a missing-attribute bug, it's a byte-comparison bug.

**3. Live schema divergence — `listItem.textAlign` and `aiContent.loading`.** Unlike triggers 1
and 2, these are not historical. The two editors declare different schemas *today*, so the new
editor drops attributes the legacy editor is still writing. This is why
[Option A](#option-a--the-flag-round-trip-no-hand-written-json) reproduces with a list. Full
comparison in [What we ruled out](#what-we-ruled-out).

**4. Attribute key order from the API.** The REST API serialises attrs alphabetically
(`indent, level, textAlign`) while the schema order is `textAlign, indent, level`. Fetching a
document through `/api/v1/content/<id>` and feeding it back makes the check answer `false` even
for content that is otherwise perfectly current.

The JSP is not affected today, because it reads the raw stored string and `JSON.parse` preserves
the stored order. But it means **any** future host, serializer, importer or migration that
normalizes key order breaks every document at once. This trigger alone justifies not comparing
strings.

### The common thread

All four are the same defect wearing different clothes: **the check compares bytes where it needs
to compare structure.** Any future schema attribute with a default value, and any attribute
written in a different order by the API, an importer or a migration, will reproduce it.

### Why clicking is what triggers it

The effect does not react to the editor. It reads exactly two things — `this.value()` and
`this.editor()` — and neither changes when you click. ProseMirror's `EditorState` is a plain
immutable object, not a signal, and `ngx-tiptap` uses `markForCheck`, not signals. So there is no
reactive path from a selection to this effect.

It re-runs anyway. **This was measured**: wrapping both reads in `untracked()`, leaving the effect
with zero signal dependencies, it *still* re-ran on every node selection. The re-entry comes from
Angular's view refresh (`setInput` → `markViewDirty` → `runEffectsInView`), not from the
dependency graph.

That has a direct consequence for the fix: **narrowing the effect's dependencies cannot fix this.**
The only options are to remove the standing effect or to make its body cheap and idempotent.

It also explains the two diagnostic observations:

| Action | Reaches `updateProps`? | Effect re-runs | Outcome |
|---|---|---|---|
| Click the contentlet card | yes | yes | **broken** |
| Drag a text range not spanning the card | no | no | works |
| Click an image / video / table / grid | no — plain DOM node view | no | works |
| Click a `codeBlock` | yes — Angular node view | yes | **same latent bug** |

`ngx-tiptap`'s `handleSelectionUpdate` (`ngx-tiptap.mjs:337`) only calls `selectNode()` when the
selection **fully covers the node** (`from <= pos && to >= pos + nodeSize`). A text drag that
doesn't span the card never gets there.

And **only two nodes in the whole editor use an Angular node view** — `dotContent`
(`contentlet.extension.ts:91`) and `codeBlock` (`code-block.extension.ts:20`). Every other node
(image, video, audio, table, grid, `aiContent`, upload placeholder) uses a plain DOM node view and
cannot trigger this.

### Why only one screen is affected

The stored JSON explains *which content* breaks. The node view explains *which click* breaks it.
Neither explains *which screen* — that comes down to whether the host binds the `value` input at
all.

The effect's first line is `const v = this.value(); if (!v) return;`.

**New Angular Edit Content screen** — binds only `[formControlName]`
(`libs/edit-content/src/lib/fields/dot-edit-content-block-editor/dot-edit-content-block-editor.component.html`).
The `value` input is **never set**, so `value()` stays `''`. The effect still re-runs on every
node selection — it just exits on the first line, every time. Content arrives instead through
`writeValue` → `pendingValue` → `commitEditor`, once at bind.

**Legacy JSP contentlet editor** — assigns `blockEditor.value` on the custom element
(`edit_field.jsp:316`), so `value()` is populated and the effect falls through to the comparison.
This is the only host where a re-run can reach `setContent` after load, and therefore the only
host where a failed check destroys a click-made selection.

This host is the iframe used by both the UVE contentlet dialog and the Content portlet. The UVE
side panel also binds `[value]`, so it is exposed in the same way.

| Host | How the value arrives | `value()` populated? |
|---|---|---|
| Legacy JSP contentlet editor | `value` property on the custom element (`edit_field.jsp:278,316`) | **yes** → exposed |
| UVE side panel | `[value]="contentlet.content"` | **yes** → exposed |
| New Angular Edit Content | `[formControlName]` → `writeValue(string)` | no → effect exits immediately |

**Nothing re-pushes the value.** The earlier version of this spec assumed something did; that was
wrong, and it is worth stating plainly because it changes the fix. Verified for all four hosts:
the JSP sets `.value` once and its `valueChange` listener writes only to the hidden form input
(`edit_field.jsp:307-309`); the UVE side panel is one-way and routes `valueChange` into a separate
signal; the Angular screen uses `writeValue` once at bind; the dev demo app sets `value` once.

### Why the legacy editor doesn't have this bug

This is the strongest evidence for the fix, so it's worth being explicit.

The legacy editor uses **the same kind of Angular node view** for `dotContent`
(`libs/block-editor/src/lib/nodes/contentlet-block/contentlet-block.node.ts`, via its own
`lib/NodeViewRenderer.ts`), and its `selectNode()` does **the same**
`renderer.updateProps({ selected: true })` (`NodeViewRenderer.ts:151-159`). Selecting a card marks
Angular views dirty there too.

The difference is entirely in how the host holds `value`:

| | Legacy editor | New editor |
|---|---|---|
| `value` | `@Input() value: Content = ''` (`dot-block-editor.component.ts:129`) — plain input | `input<string>('')` signal (`:321`) |
| Reacts to `value` after load | **no** — `ngOnChanges` handles only `languageId` (`:282-288`) | **yes** — standing `effect()` (`:618`) |
| How content loads | once, imperatively, in `editor.on('create')` (`:400-410`) | via the effect, or `writeValue`/`commitEditor` |
| Bug present | **no** | yes |

Same node view, same dirtying, no bug — because there is nothing reactive listening. **The
regression was introduced by converting a one-shot imperative load into a standing reactive
effect.**

### What we ruled out

Recorded so nobody re-investigates these.

- **A host re-pushing the value** — disproved for all four hosts (above).
- **A custom event swallowing the click** — the full mouse-handler inventory was reviewed; the
  handler fires and the selection is created. Confirmed in the browser.
- **Allowed Blocks / field configuration** — the same document was scored against four
  configurations, including ones disallowing `bulletList` and `h2`. The check returned `true` in
  all four: restricted nodes degrade to `dotUnsupportedBlock` symmetrically on both sides, so the
  verdict doesn't move. **Field config is not a factor.**
- **`customBlocks` / the `commitEditor` slow path** — the fixture's field has no field variables,
  so `loadRemoteExtensions` never takes the slow path and `editor()` is set once (`:489`), yet the
  bug reproduces.
- **The `loader` node being dropped** — it has no equivalent in the new schema, but it is *not*
  lost: unknown nodes round-trip byte-identically via `preserveUnknownBlockNodes` →
  `dotUnsupportedBlock` → `restoreUnknownBlockNodes`. Measured. It also does not trip the check.
- **Stale bundles, permissions, workflow state, content-type restrictions, store corruption** —
  all eliminated earlier.

**Genuine schema divergences that remain** (candidates for a separate compatibility ticket, not
this fix):

| Divergence | Kind | Trips the check | Data lost on save |
|---|---|---|---|
| `listItem.textAlign` | attr dropped | yes | **yes** |
| `aiContent.loading` | attr dropped | yes | **yes** |
| `youtube.height` / `width` default `480/640` → `300/400` | changed default | yes | no |
| `link.rel` `null` → `'noopener noreferrer'`, `link.target` `'_blank'` → `'_self'` | changed default | yes | no |
| `loader` node | unknown node | no | no — preserved |

**One layer not fully explained.** `runEffectsInView` skips effects that aren't marked dirty
(`_debug_node-chunk.mjs:5928`), yet the effect re-ran with zero dependencies. We have not
identified the exact flag that marks the effect node itself dirty. This does not affect the fix —
it is precisely why the fix must not rely on the dependency graph — but it is an honest gap, and
anyone tempted to "just narrow the dependencies" should read
[Why clicking is what triggers it](#why-clicking-is-what-triggers-it) first.

---

## Fix Scope & Non-Goals *(mandatory)*

### What we're changing

Two changes, in that order of importance. The first alone fixes the reported bug; the second
closes the class.

**1. Stop the effect from doing work it wasn't asked to do — latch on raw input identity.**

The effect is re-run without any of its inputs changing. Angular's `setInput` skips when the value
is `Object.is`-equal (`_debug_node-chunk.mjs:9235`), so on a spurious re-run the host is handing
back **the same object reference**. Comparing that reference is O(1) and settles it:

```ts
#loadedValue: unknown = undefined;

effect(() => {
    const v = this.value();
    if (!v) return;
    if (v === this.#loadedValue) return;   // spurious re-run — nothing to do
    const ed = this.editor();
    if (!ed || ed.view.dragging) return;
    this.#loadedValue = v;
    this.loadContent(ed, v);
});
```

This neutralises **all four triggers at once**, because none of them is ever consulted. It also
keeps a genuine host swap working — a new object reference still loads — which matters for the UVE
side panel (see [Assumptions](#assumptions)).

Load the initial value from `commitEditor` (`:488`) rather than `editor.on('create')`. `on('create')`
fires inside `new Editor(...)`, before `this.editor.set(editor)` at `:489`, so the signal isn't
available yet — legacy gets away with it only because it assigns `this.editor` imperatively first.
`commitEditor` already drains `pendingValue`, so both the CVA path and the web-component path
converge in one place, with **no host-specific branching**.

**2. Make the comparison structural, and call it only where a re-push is real.**

Replace the string comparison in `editorContentMatchesParsed` with a ProseMirror document
comparison (`Node.fromJSON(...).content.eq(...)`), so schema defaults, undeclared attrs and key
order stop mattering. Then:

- **Keep it in `writeValue`** (`:738`). This is the `ControlValueAccessor` entry point, and
  Angular forms genuinely can call it more than once — `setValue`, `patchValue`, `reset`. No
  current caller does that to a Story Block control, but that's an observation about today's code,
  not a contract.
- **Drop it from `commitEditor`** (`:488`). That's a one-shot drain; the guard is redundant.
- **Drop it from the effect**, which now latches on identity instead.

Cost is not a concern: measured at 0.7 ms for a 119 KB / ~9,800-node document and 7 ms for an
absurd 1.19 MB / ~100,000-node one, versus 0.4 ms and 4.2 ms for the current string compare.
`Fragment.eq` itself is ~5 µs at 100,000 nodes — effectively free — because it short-circuits on
child identity. All the cost is `Node.fromJSON`, and that parse happens anyway whenever the answer
is "changed".

**Also in scope:**

- Keep today's conservative fallback: if the schema can't deserialize the content, report "not
  equal" so `setContent` still runs.
- Keep the array-shaped-value path from PR #37149 (UVE side panel) working.
- Keep the `withDocStats` emit contract (`:713`) exactly as it is.
- Add regression tests for all four triggers and every shape above.

### Non-goals

- **Not touching the click handler** (`contentlet.component.ts`). It works. The rebuild downstream
  is the problem.
- **Not changing `ngx-tiptap`, TipTap or Angular behaviour.** The node view legitimately writes an
  input on selection; Angular legitimately dirties ancestor views. We stop *depending* on that not
  happening.
- **Not replacing the Angular node view with a plain DOM one.** Doing so also fixes the bug — that
  is how the root cause was confirmed — but it would mean rewriting the contentlet card's UI and
  losing `codeBlock`'s syntax highlighting. Out of proportion.
- **No fix for `codeBlock`'s latent instance beyond what falls out of the above.** It shares the
  effect, so both changes cover it; no `codeBlock`-specific work is planned, and the ACs don't
  cover it. Worth a follow-up test.
- **No data migration** of `chartCount` → `charCount`. Optional cleanup at best.
- **No schema-compatibility work.** Restoring `listItem.textAlign` and `aiContent.loading` and
  re-aligning the four changed defaults is a separate ticket with its own regression surface
  (`link.target` in particular was a deliberate change). Listed in
  [What we ruled out](#what-we-ruled-out).
- **Not porting click-to-select to the legacy editor.** `contentlet-block.node.ts` has no click
  handler at all, so anyone running the #36985 workaround
  (`FEATURE_FLAG_NEW_BLOCK_EDITOR=false`) still sees the original symptom. Separate ticket.
- **No changes to the JSP hosting path** or to how `/dotcms-block-editor/main.js` is built and
  cache-busted.
- **No new toolbar behaviour, styling or selection-ring work.** `editor.component.css:272`
  already styles `[data-type='dot-content'].is-selected`. The missing ring is a symptom.

---

## Regression Risk *(mandatory)*

**Blast radius.** Both changes sit on the content-load path, which every host exercises: the
legacy JSP editor (UVE dialog and Content portlet), the Angular Edit Content screen, and the UVE
side panel. If the load breaks, the field renders empty — the most visible possible failure, and
the failure mode of #37145.

The identity latch is the higher-risk change of the two, because it changes *when* content loads
rather than *how* it is compared. Three cases to get right:

- **First load must still happen.** The latch starts `undefined`, and no real value is ever
  `undefined`, so the first non-empty value always loads. Guard the `!v` check before the latch
  check so `''` never latches.
- **A genuine host swap must still load.** New object reference ⇒ load. This is why the effect is
  latched rather than deleted; see [Assumptions](#assumptions).
- **The CVA path must not double-load.** `commitEditor` drains `pendingValue` *or* `value()`, not
  both, and sets the latch when it loads from `value()`.

Two failure modes for the comparison change:

- **Too permissive** — suppresses a legitimate content push and leaves the editor showing stale
  content.
- **Throws** — blanks the field (#37145). Unknown *marks* raise
  `RangeError: There is no mark type <name> in this schema`; the `catch` must return "not equal".

The unknown-node path is shared with #37149, so the placeholder round-trip
(`preserveUnknownNodesInDocument` / `restoreUnknownBlockNodes`) has to keep working untouched.
This fix changes *when* and *how* the two sides are compared, never what gets stored or emitted.

**Backward compatibility.** The emitted value shape must not change — root `charCount` /
`wordCount` / `readingTime` are read by server-side reporting and headless renderers
(`withDocStats`, `:713`). Stored content isn't rewritten. No REST contract, DB schema or ES
mapping is touched, so this is rollback-safe.

**Data.** Nothing required. Affected rows heal on their own the next time an author saves them,
because the editor re-emits normalized JSON. No migration proposed.

---

## Acceptance & Verification *(mandatory)*

All of these are evaluated **in the legacy JSP contentlet editor** unless stated otherwise —
that's the only host where the effect falls through to `setContent`. AC-001 and AC-002 already
pass on the new Angular screen today, so naming the host matters.

| ID | Criterion |
|---|---|
| **AC-001** | With a body whose stored JSON lacks `indent` on heading and paragraph nodes, and at least one block above the card, clicking the card produces a `dotContent` `NodeSelection` that persists — ring visible, toolbar contentlet actions enabled. |
| **AC-002** | Same, for a body whose root `doc.attrs` contains the legacy `chartCount` key. |
| **AC-003** | Same, for a body containing a bullet list whose `listItem` nodes carry `textAlign` (the live-divergence trigger, and the Option A route). |
| **AC-004** *(the new core assertion)* | Clicking the card dispatches **no** `setContent` transaction — no `ReplaceStep`, and the document object identity is unchanged. This is the assertion the previous ACs were missing: it targets the mechanism, not the symptom. Verified by mounting the component, selecting the node, running change detection and asserting on transactions. |
| **AC-005** *(latch)* | Pushing the **same object reference** into `value` any number of times loads the content exactly once. Pushing a **different** object reference loads again. |
| **AC-006** *(load path)* | On the web-component host, initial content still loads with no `value` effect involvement — i.e. `commitEditor` loads it. On the reactive-forms host, `writeValue` → `pendingValue` → `commitEditor` still loads it. Neither renders an empty field. |
| **AC-007** *(comparator)* | The structural comparison reports "unchanged" for all four trigger shapes — `chartCount`, missing `indent`, `listItem.textAlign`, and attrs re-serialized in alphabetical key order — and "changed" for a genuinely different document. |
| **AC-008** *(regression, #37145)* | A body carrying a **mark** the schema can't deserialize still triggers `setContent` rather than throwing or blanking the field. |
| **AC-009** *(regression, #37149)* | An array-shaped value (UVE side panel) loads correctly and compares equal. Measured `false` today under the string comparison, required `true` — **expect this to fail at Red.** |
| **AC-010** *(regression)* | The emitted value still carries root `charCount`, `wordCount` and `readingTime` when the document is non-empty. |
| **AC-011** *(branch symmetry)* | A document containing a node type unknown to the schema compares equal through **both** entry points — string-valued `writeValue` and object-valued `value` — so `customBlocks` content stops triggering an avoidable `setContent` at load. |

**AC-004 is the criterion that matters most**, and it is new. The previous version of this spec
asserted only on the comparator's return value, which is a pure function — a green unit row there
proves almost nothing about the call site. AC-004 asserts at the call site, and it is reachable in
Jest: mount `DotCMSEditorComponent`, set `field` and `value`, run
`chain().focus().setNodeSelection(pos).run()`, run change detection, and assert no doc-changing
transaction was dispatched and the selection is still a `NodeSelection`. This reproduces the
defect headlessly, so it is a genuine Red-phase test rather than a documentation exercise.

**On AC-008, the node/mark split is deliberate.** `Node.fromJSON` raises `RangeError` for an
unknown mark, so the comparator's `catch` returns "not equal" and the load proceeds as today.
Unknown *nodes* are different — they deserialize fine via the `dotUnsupportedBlock` placeholder and
must compare equal, which is AC-011. Nodes degrade to a placeholder; marks abort deserialization.

### How we'll verify

- **Unit (primary)** — Jest in `libs/new-block-editor`, run with `pnpm nx test new-block-editor`.
  Two kinds:
  - *Component-level* for AC-004, AC-005 and AC-006 — mount the real component and assert on
    dispatched transactions and selection survival. These reproduce the defect and fail today.
  - *Pure round-trip* for AC-007 through AC-011 — build the schema via
    `getSchema(createEditorExtensions(...))` per [Option C](#option-c--no-server-at-all).
- **Manual** — the [Option A](#option-a--the-flag-round-trip-no-hand-written-json) flag round-trip
  in the legacy JSP editor. Confirm the card selects and keeps its ring, and that an image on the
  same document also still selects. **Root cause has been confirmed this way; the post-fix run has
  not happened yet.**
- **e2e** — see the plan's Test Strategy. The previous Principle V exception argued no e2e was
  needed because the manual A/B was cheap; AC-004 now being reachable in Jest strengthens the unit
  coverage, but a Playwright test over the real click remains the only automated check of the
  actual browser path. **The developer must explicitly accept or reject this**, per Principle V
  gate 2.

---

## Assumptions

- The click handler is present and working wherever the fix ships; the defect is the downstream
  rebuild. **Confirmed** in a running browser, twice: by instrumenting the effect, and by
  substituting a plain DOM node view (the selection then survives).
- No Java or REST change is required. To be confirmed in the plan phase.
- The four triggers are representative of the defect class. The fix targets the class, not the
  individual keys, so no exhaustive audit of historical attribute drift is needed.
- **The UVE side panel does not call `open()` while already open.** The identity latch would
  otherwise reload correctly anyway (new object reference), so this is a safety net rather than a
  dependency — but it is the reason the effect is *latched* rather than *deleted*. Today the
  drawer is `[dismissible]="false"` / `[closable]="false"` and `onDrawerHide()` nulls the
  contentlet before the next open, so an in-place swap isn't reachable; `open()` is nonetheless a
  public method.
- `@angular/elements` hands the same object reference back on a re-apply. Follows from
  `setInput`'s `Object.is` skip (`_debug_node-chunk.mjs:9235`), and consistent with the measured
  `valueIdentity=true` on the spurious re-run.
- The API-seeded fixture faithfully represents the affected population: content authored before
  the current schema, or written by an importer or integration.

### Still pending

- **A post-fix manual run.** The root cause is confirmed in a browser; the fix is not implemented,
  so nothing has been verified against it yet.
- **The exact Angular flag that marks the effect node dirty** is unidentified. See
  [What we ruled out](#what-we-ruled-out). It does not gate the fix — the fix is designed not to
  care — but it should be understood before anyone attempts a dependency-narrowing alternative.
- **Developer sign-off on the e2e question**, per Principle V gate 2.
