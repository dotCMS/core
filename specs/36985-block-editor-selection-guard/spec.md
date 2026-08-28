# Block Editor: you can't click an embedded contentlet on older content

**Branch**: `issue-36985-block-editor-embedded-contentlet-cannot-be-selected-when-text-or-other-content-precedes-it-spec`
**Created**: 2026-08-28 · **Status**: Draft · **Type**: Issue / Bug Resolution
**GitHub**: [#36985](https://github.com/dotCMS/core/issues/36985) (reopened) · prior fix [PR #37104](https://github.com/dotCMS/core/pull/37104) · related [#37145](https://github.com/dotCMS/core/issues/37145), PR #37149

<!--
  dotCMS ISSUE-RESOLUTION spec (/speckit-specify-fix). The mandatory H2 headings below are the
  contract with /speckit-plan and /speckit-tasks — keep them and their order. Readable framing
  lives inside them as leads and H3s.
-->

---

## Problem Statement *(mandatory)*

### The short version

In the new Block Editor, clicking an embedded contentlet card does nothing. No selection ring,
and the toolbar's edit / delete / reorder buttons stay greyed out.

The click handler isn't broken. It selects the card correctly. Then, a few milliseconds later,
an unrelated piece of code rebuilds the entire document and throws the selection away.

That rebuild is guarded by a check that asks "has the content actually changed?" The check
compares two JSON strings. For content saved by today's editor the strings match, the rebuild
is skipped, and everything works. For older content the strings don't match — even though the
content is identical — so the rebuild fires every time.

**The fix**: make that check compare documents structurally instead of comparing text. The
full mechanism is in [Root-Cause Hypothesis](#root-cause-hypothesis).

### Why it looks random

Authors report that some contentlets are selectable and some aren't, on the same site, in the
same content type. That's real, and it's the most confusing part of the bug.

It depends entirely on the shape of the JSON stored in the database, which depends on when and
how the content was last written. Content saved by the current editor works. Content saved by
an older editor, an importer, or the REST API often doesn't.

**Severity / Impact**: customer-blocking on Evergreen. Every author editing pre-existing Story
Block content with embedded contentlets is affected. The documented workaround — switching back
to the legacy editor — does not help; see [Non-goals](#non-goals).

---

## Reproduction *(mandatory)*

**Environment**: local dotCMS with the PR #37104 fix present (commit `d85346613c`, first tagged
`v26.08.19-01`), `FEATURE_FLAG_NEW_BLOCK_EDITOR` on (the default). No customer data needed.

### Option A — no server, about thirty seconds

Round-trip a legacy-shaped document through the real schema and compare it the way the check
does.

1. Build the schema from the editor's own extension list: `getSchema(createEditorExtensions(...))`
   from `libs/new-block-editor/src/lib/editor/extensions/editor-extensions.ts`.
2. Parse and re-serialize a heading whose stored attrs are `{ textAlign, level }` — no `indent`
   — via `Node.fromJSON(schema, stored).toJSON()`.
3. Compare `JSON.stringify(stripDocStats(stored))` against `JSON.stringify(stripDocStats(parsed))`.

The strings differ. The editor's side has gained `indent: 0`, sitting between `textAlign` and
`level`.

### Option B — running instance

> **You must use the legacy JSP contentlet editor.** The content type needs
> `CONTENT_EDITOR2_ENABLED=false` in its metadata. Following these steps on the new Angular Edit
> Content screen will not reproduce anything — see
> [Why only one screen is affected](#why-only-one-screen-is-affected).

You need two contentlets that differ **only** in the shape of their stored JSON, so you can A/B
the defect against a control:

- **BROKEN** — root `chartCount`, and heading/paragraph nodes with no `indent`.
- **CONTROL** — the same document in the shape the current editor emits.

Both must put text above the card; that's the structural precondition.

1. `just dev-run`
2. Create or pick a content type with a Block Editor (Story Block) field, with
   `CONTENT_EDITOR2_ENABLED=false` in its metadata.
3. Create any contentlet to embed and note its identifier.
4. Create both parents **through the API, not the editor** — that is what preserves the legacy
   shape. Fire each body, stringified into the Block Editor field, at
   `/api/v1/workflow/actions/default/fire/PUBLISH`:

   ```bash
   curl -u admin@dotcms.com:admin -X POST \
     'http://localhost:8080/api/v1/workflow/actions/default/fire/PUBLISH' \
     -H 'Content-Type: application/json' \
     -d '{"contentlet": {"contentType":"<CT variable>","title":"Repro 36985 BROKEN","<blockFieldVar>":"<body JSON as a string>"}}'
   ```

   **BROKEN body** — note the root `chartCount` and the absent `indent`:

   ```jsonc
   {
     "type": "doc",
     "attrs": { "chartCount": 262, "wordCount": 41, "readingTime": 1 },
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

   **CONTROL body** — the same document, current shape: drop the root `attrs` entirely, and give
   every heading and paragraph `"indent": 0` **positioned between `textAlign` and `level`**. The
   position matters; see the measured table in
   [Three confirmed triggers](#three-confirmed-triggers).

5. Open **BROKEN** from the Content portlet and click the embedded card.
6. Repeat with **CONTROL** to see the contrast.

> **A seeding script that does all of this is attached to the
> [QA note on the issue](https://github.com/dotCMS/core/issues/36985#issuecomment-5456242587).** It is deliberately not committed to the repo — the steps above
> are the source of truth, and the script is a convenience.

### What you should and shouldn't see

**Expected Behavior**: the card takes a `NodeSelection` and keeps it. Ring visible, toolbar
actions enabled, card can be edited, deleted or reordered.

**Actual Behavior**: the selection is created and immediately discarded. The document is
rebuilt, the cursor collapses to the end, no ring, "Edit Contentlet" stays disabled.

**Reproducibility**: always, for content matching the data condition — either trigger alone is
enough. Never, for content last saved by the new editor. Note that dragging a text *range*
across the card still works, which is what distinguishes this from the original #36985.

### Confirming it in the browser

The `EditorView` isn't reachable from the DOM, so this check works on DOM identity and mutations
instead. Run this before clicking, with the fixture open and the card visible:

```js
const pm = document.querySelector('.ProseMirror');
window.__snap = { pm, card: pm.querySelector('[data-type="dot-content"]') };
window.__muts = 0;
new MutationObserver((m) => { window.__muts += m.length; }).observe(pm, {
    childList: true,
    subtree: true
});
```

Click the card, then:

```js
({
    editorRebuilt: document.querySelector('.ProseMirror') !== __snap.pm,
    docRebuilt: document.querySelector('[data-type="dot-content"]') !== __snap.card,
    mutations: __muts,
    selectionRing: !!document.querySelector('[data-type="dot-content"].is-selected')
})
```

| Reading | What it means |
|---|---|
| `editorRebuilt: true` | A new `EditorView` was created — `commitEditor` ran again |
| `editorRebuilt: false`, `docRebuilt: true` | Same editor; `setContent` rebuilt the document. **This is what BROKEN should show** |
| `docRebuilt: false`, `mutations: 0` | Nothing rebuilt — the selection was lost some other way |
| `selectionRing: false` | Confirms the `NodeSelection` didn't survive |

CONTROL should show `docRebuilt: false` and `selectionRing: true`.

> ⚠️ **Don't reach for `pmViewDesc.view`.** `ViewDesc` sets `dom.pmViewDesc = this` but never
> stores the view (prosemirror-view 1.41.8, `dist/index.js:741-753`; the only `this.view`
> assignments belong to `ViewTreeUpdater`, `MouseDown` and `DOMObserver`, none reachable from the
> DOM). The accessor returns `undefined`, so any comparison written on it is
> `undefined === undefined` — a silent `true` that looks like a passing check. An earlier draft
> of this spec made exactly that mistake.

---

## Scope of Investigation *(mandatory)*

**Affected area**: content editing — the new Block Editor (`core-web/libs/new-block-editor`),
specifically the value-synchronisation check shared by every host: the legacy JSP contentlet
editor (the iframe behind both the UVE contentlet dialog and the Content portlet), the Angular
Edit Content screen, and the UVE side panel.

**Suspected surface**: frontend only, TypeScript under `libs/new-block-editor`. No Java change
expected — nothing under `com.dotcms.*` or `com.dotmarketing.*`. The legacy JSPs
(`edit_field.jsp`, `edit_contentlet.jsp`) only *host* the web component and shouldn't need
touching.

**Related known decisions**: none identified yet; the plan phase formally consults
`dotCMS/platform-adrs`. Worth flagging that the emitted root `charCount` / `wordCount` /
`readingTime` attrs are a deliberate legacy-parity contract that downstream consumers read — any
change must preserve them on the emit path.

---

## Root-Cause Hypothesis

### What's actually happening

Step by step, when you click the card:

1. The click handler fires and creates a `NodeSelection` on the `dotContent` node. Correct.
2. A value-synchronisation effect runs.
3. That effect calls `editorContentMatchesParsed()` to decide whether the incoming value differs
   from what the editor currently holds.
4. The check returns `false` — "they're different" — even though they aren't.
5. `setContent(..., { emitUpdate: false })` runs and rebuilds the document.
6. The rebuild discards the `NodeSelection`; the cursor collapses to the end of the document.
7. The toolbar is gated on that selection, so its contentlet actions go dark.

Nothing here is a rendering problem. The selection genuinely existed and was genuinely
destroyed.

### Where the code lives

| What | Where |
|---|---|
| The faulty check | `libs/new-block-editor/src/lib/editor/editor.component.ts:137-155` |
| The effect that calls it | `editor.component.ts:619-632` |
| Other callers | `writeValue` (`:747`), `commitEditor` (`:498`) |
| The normalizer it uses | `utils/doc-stats.utils.ts:4` (`stripDocStats`) |
| Toolbar gating | `components/toolbar/editor-toolbar.store.ts:98` |
| Selection ring styling | `editor.component.css:272` |

### Why the check fails

Here is the whole of it:

```ts
JSON.stringify(stripDocStats(incoming)) === JSON.stringify(stripDocStats(editor.getJSON()))
```

Two problems with comparing JSON as text:

- **Key order counts.** `{"textAlign":…,"level":…}` and `{"textAlign":…,"indent":…,"level":…}`
  are different strings even when they describe the same thing.
- **`stripDocStats` barely normalizes anything.** It touches only the root `attrs`, and only
  three keys inside it. Everything below the root is compared raw.

Meanwhile TipTap normalizes heavily when it parses: it fills in schema defaults, drops
attributes the schema doesn't declare, and reorders keys to schema order. So the editor's side
of the comparison is always the normalized shape, and the stored side is whatever happened to be
written years ago.

### Three confirmed triggers

**1. The `chartCount` typo.** The legacy editor wrote `SetDocAttrStep('chartCount', …)` — with
an extra `t` — until commit `3cd5549d35` (#26025, 2023-09-11). `stripDocStats` strips
`charCount`, `wordCount` and `readingTime`. It does not strip `chartCount`, and the current
schema doesn't declare it, so the key survives on the stored side and vanishes on the editor
side.

**2. Missing `indent`.** `IndentExtension` (`extensions/indent.extension.ts:19,84`) declares
`indent` with a default of `0` on `heading`, `paragraph` and `blockquote`. Content written
before that extension existed has no such key, so the editor adds it on parse.

Measured locally against the real schema via `getSchema(createEditorExtensions(...))`:

| Stored heading attrs | Check result |
|---|---|
| `{textAlign, level}` — legacy shape | ❌ `false` |
| `{textAlign, level, indent}` — key added, but last | ❌ `false` |
| `{textAlign, indent, level}` — current shape | ✅ `true` |

That middle row is the one worth staring at. Supplying the missing key isn't enough — it has to
be in the right *position*. This isn't a missing-attribute bug, it's a byte-comparison bug.

**3. Unknown node types** (same function, different cause). The check has two branches, and they
don't do the same work:

- The object branch (`:152`) runs the incoming value through `preserveUnknownNodesInDocument`,
  rewriting any unrecognised node into a `dotUnsupportedBlock` placeholder.
- The string branch (`:143`) skips that step.

The editor's own document always holds the placeholder. So on the string branch, a document with
a custom block is compared placeholder-against-original and fails forever. (The two placeholders
serialize identically — the asymmetry is that one side never gets rewritten.)

This one doesn't widen the click-selection problem, because the string branch is only reached
from `writeValue`, which is a one-shot reactive-forms host. But it does mean the new Edit Content
screen fires an avoidable `setContent` at load for any content using `customBlocks`, and it's a
trap for any future host that passes a string and re-pushes.

### The common thread

All three are the same defect wearing different clothes: **the check compares bytes where it
needs to compare structure.** Any future schema attribute with a default value, and any attribute
written in a different order by the API, an importer or a migration, will reproduce it.

### Why only one screen is affected

The stored JSON explains *which content* breaks. It doesn't explain *which screen*, because for
triggers 1 and 2 both branches of the check agree: BROKEN is `false` either way, CONTROL is
`true` either way.

The difference is how each host hands the value to the editor.

**New Angular Edit Content screen** — binds with `[formControlName]`, so the value arrives once
through `writeValue` at form bind. Angular reactive forms never echo a control-value-accessor's
own `onChange` back to it, and the `value` input is never set, so the effect at `:619`
short-circuits on `if (!v) return` forever. `setContent` does fire once at load on broken
content, but that's before any click and costs nothing. Nothing can call it again afterwards, so
clicks are always safe here.

**Legacy JSP contentlet editor** — assigns `blockEditor.value` on the custom element
(`edit_field.jsp:316`), so the effect at `:619` *is* the loader and stays live on `value()` and
`editor()`. This is the only host where a re-push can reach `setContent` after load, and
therefore the only host where a failed check destroys a click-made selection.

This host is the iframe used by both the UVE contentlet dialog and the Content portlet.

| Host | How the value arrives | Branch used |
|---|---|---|
| Legacy JSP contentlet editor | `value` input, parsed object (`edit_field.jsp:278,316`) | object (`:152`) |
| UVE side panel | `value` input, `BlockEditorData.content` is `JSONContent` | object (`:152`) |
| New Angular Edit Content | `formControlName` → `writeValue(string)` | string (`:143`) |

Two edge cases recorded rather than hand-waved: when the JSP's `JSON.parse` throws it falls back
to showdown-converted HTML (`edit_field.jsp:282`), but that isn't Block Editor JSON and resolves
through the HTML comparison, so trigger 3 can't apply to it. And `commitEditor`'s `pendingValue`
is fed only by `writeValue`, so it belongs to the reactive-forms host alone.

### One thing we still don't know

Something re-pushes the value in the legacy host after load, and we haven't identified what. The
JSP assigns `.value` once, Angular Elements replays initial inputs once then clears them, and
signal inputs dirty-check by reference — so it's either a distinct object with equal content, or
a second `editor()` emission.

The obvious candidate for the second — `commitEditor` re-running after remote `customBlocks`
extensions resolve — is ruled out for the local fixture: its Story Block field has no field
variables, so `loadRemoteExtensions` never takes the slow path and `editor()` is set once, yet
the bug still reproduces.

This doesn't block the fix. Once the comparison is structural, the verdict for these documents
becomes "unchanged", so a re-push is harmless rather than merely suppressed.

---

## Fix Scope & Non-Goals *(mandatory)*

### What we're changing

- Replace the string comparison in `editorContentMatchesParsed` with a structural ProseMirror
  document comparison, so schema defaults, undeclared attrs and key order stop mattering.
- Keep today's conservative fallback: if the schema can't deserialize the content, report "not
  equal" so `setContent` still runs.
- Keep the array-shaped-value path from PR #37149 (UVE side panel) working.
- Keep the `withDocStats` emit contract exactly as it is.
- Add regression tests for all three triggers and all the shapes above.

### Non-goals

- **Not touching the click handler** (`contentlet.component.ts`). It works. The rebuild
  downstream is the problem.
- **No data migration** of `chartCount` → `charCount`. Optional cleanup at best; the structural
  comparison makes it unnecessary here.
- **Not porting click-to-select to the legacy editor.** `contentlet-block.node.ts:23-24` has no
  click handler at all, so anyone running the #36985 workaround
  (`FEATURE_FLAG_NEW_BLOCK_EDITOR=false`) still sees the original symptom. Separate ticket.
- **No changes to the JSP hosting path** or to how `/dotcms-block-editor/main.js` is built and
  cache-busted.
- **No change to the synchronisation effect itself** — only to the check it calls. An apply-once
  memo in the effect would also hide the symptom, but we're deliberately skipping it: the
  structural comparison already makes the verdict "unchanged", and adding a memo now would remove
  the pressure to find out what actually re-pushes the value. Revisit once that's known.
- **No new toolbar behaviour, styling or selection-ring work.** `editor.component.css:272`
  already styles `[data-type='dot-content'].is-selected`. The missing ring is a symptom, not a
  cause.

---

## Regression Risk *(mandatory)*

**Blast radius.** This check is the single gate in front of every `setContent` on the load path.
It runs in three places — the value effect, `writeValue`, and `commitEditor` — so every host
evaluates it: the legacy JSP editor (UVE dialog and Content portlet), the Angular Edit Content
screen, and the UVE side panel.

Only the web-component host is re-entrant after load, so only it can regress at click time. The
others evaluate once at load and would regress there instead.

Two failure modes to watch:

- **Too permissive** — suppresses a legitimate content push and leaves the editor showing stale
  content.
- **Throws** — blanks the field, which is the failure mode of #37145.

The unknown-node path is shared with #37149, so the placeholder round-trip
(`preserveUnknownNodesInDocument` / `restoreUnknownBlockNodes`) has to keep working untouched.
This fix changes *how the two sides are compared*, never what gets stored or emitted.

**Backward compatibility.** The emitted value shape must not change — root `charCount` /
`wordCount` / `readingTime` are read by server-side reporting and headless renderers
(`withDocStats`, `editor.component.ts:713`). Stored content isn't rewritten. No REST contract, DB
schema or ES mapping is touched, so this is rollback-safe.

**Data.** Nothing required. Affected rows heal on their own the next time an author saves them,
because the editor re-emits normalized JSON. No migration proposed.

---

## Acceptance & Verification *(mandatory)*

All of these are evaluated **in the legacy JSP contentlet editor** unless stated otherwise —
that's the host where the effect stays live. AC-001 and AC-002 already pass on the new Angular
screen today, so naming the host matters.

| ID | Criterion |
|---|---|
| **AC-001** | With a body whose stored JSON lacks `indent` on heading and paragraph nodes, and at least one block above the card, clicking the card produces a `dotContent` `NodeSelection` that persists — ring visible, toolbar contentlet actions enabled. |
| **AC-002** | Same, for a body whose root `doc.attrs` contains the legacy `chartCount` key. |
| **AC-003** | Loading either body dispatches no `setContent` transaction. The check reports the document as unchanged. |
| **AC-004** *(regression)* | A genuinely different incoming value still triggers `setContent`. A body carrying a **mark** the schema can't deserialize still triggers `setContent` rather than throwing or blanking the field — #37145 must not regress. |
| **AC-005** *(fix + regression)* | An array-shaped value (UVE side panel) loads and compares equal. This is a behaviour change, not a preserved verdict: a bare array survives `preserveUnknownNodesInDocument` as an array, so today it is compared against a `{type:'doc'}` object and can never match. Measured today `false`, required `true` — expect it to fail at Red. #37149's load path must keep working. |
| **AC-006** *(regression)* | The emitted value still carries root `charCount`, `wordCount` and `readingTime` when the document is non-empty. |
| **AC-007** *(branch symmetry)* | A document containing a node type unknown to the schema compares equal through **both** entry points — string-valued `writeValue` and object-valued `value` input — so `customBlocks` content stops triggering an avoidable `setContent` at load. |
| **AC-008** *(idempotence)* | Pushing a value equal to the editor's current document dispatches no transaction, however many times it's pushed. This has to hold for the legacy shapes above, so the unidentified re-push becomes harmless rather than merely suppressed. |

**On AC-008, the unit row is only half the criterion.** The comparator is a pure function, so
calling it twice with equal content cannot fail — a Jest row for it documents intent but carries
no risk. The real assertion is at the *call site*: that the effect dispatches no `setContent`.
That isn't reachable from a pure-function spec, so it is verified manually via Option B
(`docRebuilt: false` after the click). Worth stating plainly, because frontend-only changes skip
integration and Postman in the merge queue — Jest is the only automated gate on main, and a green
unit row must not be read as covering the call site.

**On AC-004, the node/mark split is deliberate.** `Node.fromJSON` raises
`RangeError: There is no mark type <name> in this schema`, so the comparator's `catch` returns
"not equal" and the load proceeds as it does today. Unknown *nodes* are different — they
deserialize fine via the `dotUnsupportedBlock` placeholder and must compare equal, which is
AC-007. Nodes degrade to a placeholder; marks abort deserialization.

### How we'll verify

- **Unit** — Jest specs in `libs/new-block-editor` covering AC-001 through AC-008 as pure
  round-trip cases built from `getSchema(createEditorExtensions(...))`, i.e. the Option A recipe.
  Run with `pnpm nx test new-block-editor`.
- **Manual** — A/B against the seeded fixtures from Option B, in the legacy JSP contentlet
  editor, using the DOM-identity check above. After clicking the card, BROKEN must report
  `docRebuilt: false` and `selectionRing: true`, matching CONTROL. **Not yet run** — it needs a
  browser, and neither investigating session could drive one.

---

## Assumptions

- The click handler is present and working wherever the fix ships; the defect is the downstream
  rebuild. Confirmed by instrumenting the running component — patching `value()` and reading
  `isActive('dotContent')` and the toolbar's disabled state.
- No Java or REST change is required. To be confirmed in the plan phase.
- The three triggers are representative of the defect class. The fix targets the class, not the
  individual keys, so we don't need an exhaustive audit of historical attribute drift.
- Fixing the check is sufficient — the selection survives once the spurious `setContent` is gone.
  Confirmed by pinning `value()` to `editor.getJSON()` at runtime.
- The API-seeded fixture faithfully represents the affected population: content authored before
  the current schema, or written by an importer or integration.

### Still pending

Two measurements are **not** done, and nothing above should be read as if they were.

- **The local CONTROL click in the legacy JSP editor has not been performed.** Neither
  investigating session could drive a browser. The host half of the root cause currently rests on
  customer-environment evidence plus the routing in `dot-contentlet-editor.service.ts:204`, which
  shows the Content portlet opening the same legacy JSP editor as the UVE dialog. If CONTROL
  turns out to fail locally too, the guard verdict is *not* what drives that host and
  [Why only one screen is affected](#why-only-one-screen-is-affected) needs rewriting before
  anyone implements against it.
- **The re-push has not been identified.** See
  [One thing we still don't know](#one-thing-we-still-dont-know). The DOM-identity check in
  Reproduction splits the two remaining explanations; nobody has run it.
