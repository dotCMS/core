# Issue Resolution Specification: Block Editor converts typed characters into `emoji` nodes, splitting marked text

**Feature Branch**: `37340-emoji-text-node`

**Created**: 2026-09-03 · **Revised**: 2026-09-07 (v2 — scope reduced to the editor, see [Revision History](#revision-history))

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#37340](https://github.com/dotCMS/core/issues/37340) — *AC amendment required, see Assumptions*

**Input**: Developer direction, v2: "The implementation was too big and we weren't attacking the real
issue. Emoji should be inserted as regular text — both from `:smiley:` and from the picker. Keep the
node registered but never use it, because of its limitations. The extension is only needed for the
inline `:smiley:` shortcode — add a comment explaining that." Clarified 2026-09-04: "Keep emoji nodes
that already exist, but don't let the user add any other emoji node — just emojis in text."

<!--
  dotCMS ISSUE-RESOLUTION spec (/speckit-specify-fix). Framed around a defect: what is wrong,
  how to reproduce it, how we know it is fixed. Technology-light — code-level design belongs in
  the plan. This is v2: v1 grew a renderer-side programme (VTL + Java + four SDK renderers + a
  generated shortcode map) around a defect whose cause is four lines of editor configuration.
-->

## Problem Statement *(mandatory)*

When an author types or pastes any character that Unicode classifies as an emoji into Block
Editor text, the editor silently replaces that character with a standalone `emoji` node. The
replacement is created **bare** — it carries none of the formatting that surrounded it.

Two consequences, both persisted into the stored Story Block JSON:

1. **Marked text is torn in two.** A single linked phrase becomes `text(link)` +
   `emoji(no marks)` + `text(link)`. The rendered output is two `<a>` elements where the author
   created one.
2. **The character is lost downstream.** The node stores only a TipTap shortcode
   (`{"type":"emoji","attrs":{"name":"copyright"}}`) — no literal character. Consumers have no
   `emoji` branch: the VTL renderer emits nothing, and the JS SDKs fall through to their
   unknown-block component. The same blindness reaches the server: `StoryBlockUtil` counts only
   `"text"` nodes, so an emoji-only Story Block field is treated as **empty** by required-field
   validation and contributes **zero** to character counts.

The customer reported `©`, `®`, and `™`. Those are not a special case — they are the visible
symptom of a defect spanning a large class of characters.

**Severity / Impact**: Medium.

- **Who**: any author using the new Block Editor (`FEATURE_FLAG_NEW_BLOCK_EDITOR`), plus every
  downstream consumer of that content.
- **Accessibility** — the reported symptom. One logical link yields two keyboard tab stops and
  two NVDA Elements List / VoiceOver rotor entries, announced as fragments ("link dotCMS
  Copyright", then "link All rights reserved"). WCAG 2.2 Level A failures: 1.3.1, 2.4.4, 4.1.2;
  closest technique is H2.
- **Content loss** — legal symbols vanish from VTL-rendered pages.
- **How often**: every time an affected character is typed. Links still resolve, which is why
  this is Medium.

## Clarifications

### Session 2026-09-07 (v2 — supersedes the v1 session below)

- Q: What is the actual fix boundary? → A: **The editor only.** Stop creating `emoji` nodes, and
  heal the ones already stored into text when a document is loaded. No renderer, SDK, or Java
  change. Rationale in [Fix Scope & Non-Goals](#fix-scope--non-goals-mandatory).
- Q: Does the emoji picker keep working? → A: **Yes, unchanged in the UI.** It already calls
  `insertContent(emoji.native)` with a literal character; only the conversion that turned that
  character back into a node is removed.
- Q: Does `:smiley:` shortcode entry keep working? → A: **Yes — it is the only reason the
  extension is still needed at authoring time.** It resolves the shortcode against the
  extension's `emojis` table and inserts the **character**, not a node. This must be recorded in
  a code comment (AC-010).
- Q: What happens to `emoji` nodes already saved by the affected customer? → A: **They are healed
  on the editor's parse path.** Loading a document containing an `emoji` node turns that node into a
  `text` node holding the resolved character. Editor-side only: no Java, no migration, no heal
  script. The shortcode table needed to resolve the character already ships inside the editor's own
  dependency, so nothing crosses a build boundary.
- Q: Should the heal also inherit the neighbouring `link` mark, so the reported payload renders as
  one anchor again? → A: **Not in general — but yes for one narrow signature.** The default rule is
  `emoji(marks) → text(same marks, character)`: marks preserved, never invented. Inheriting from a
  neighbour as a general rule would infer **one** author's intent from **one** payload shape, and in
  another document that shape is deliberate.
- Q: Then what about the specific shape a **bare** `emoji` node between two `text` nodes carrying
  **identical** `link` marks? → A: **Inherit, because that shape is a defect fingerprint rather than
  an editorial choice.** Raised in review on PR #37434. The editor cannot produce it deliberately
  going forward: applying a link over an existing `emoji` node marks the node too (verified), and
  after this fix a typed symbol never becomes a node at all. The only construction that reaches it —
  linking two runs separately *around* an already-converted symbol — requires having hit this defect
  first, so it lives in the same legacy population the heal is repairing. **This closes #37340's
  "single link without a re-save" criterion instead of amending it.** Bounded by AC-015 and, more
  importantly, by AC-016's negative cases.
- Q: What if that inference is wrong for someone? → A: **Accepted, and it is a different class of
  harm than a general mark rewrite.** A wrong merge is **visible** — the underline extends over the
  symbol — and locally reversible by selecting and removing the link. The general rule was rejected
  partly on "silent and irreversible"; neither adjective applies to this signature.
- Q: Removing the four creation paths still leaves the node's inherited `parseHTML`, which matches
  `<span data-type="emoji">` — so copying a stored emoji between fields re-creates the node. Keep
  that? → A: **No — neutralize `parseHTML` so pasted emoji HTML lands as text.** Stored JSON is
  unaffected: `setContent` with a JSON value goes through `Node.fromJSON`, which never consults
  `parseHTML`. The editor's HTML-string fall-through (`normalizeEditorContent`, for hosts that pass
  HTML rather than ProseMirror JSON) then degrades an emoji span to its character text rather than
  to nothing, because the rendered span already carries the character — an acceptable, and arguably
  better, outcome.
- Q: What should `emoji` in a field's Allowed Blocks control, now that emoji are plain characters
  an author can always type? → A: **Nothing — remove the gate.** Investigation during this session
  found `emoji` is **not selectable** in Allowed Blocks at all: the option list comes from
  `getEditorBlockOptions()`, which offers block nodes only, and `link`/`emoji`/`youtube` were
  deliberately excluded (#37175). So `has('emoji')` is true only when a field carries **no**
  restriction, and on **every** restricted field the toolbar picker button is hidden and the `:)`
  rule stops firing — a latent defect nobody configured. Both gates go.
- Q: How does support find the content that needs attention? → A: **No longer the blocking problem
  it was.** This question was answered with a read-only query while stored nodes were being left
  alone; the heal supersedes it, since content repairs itself as fields are opened. A locating query
  remains useful and may be shared ad hoc, but it is not a deliverable and there is no report
  feature.
- Q: Constitution Principle V requires an explicit statement when a test type is skipped. Which
  are skipped here? → A: **Jest unit tests only — no integration, Postman, Karate or e2e test is
  written, and that is a deliberate, recorded decision.** Rationale: this change touches no Java,
  no database, no REST endpoint, no renderer and no build artifact, so those layers have nothing to
  assert. The one claim that reaches beyond the editor — that a `text` node carrying a `link` mark
  renders as a single `<a>` — is pre-existing behavior in every renderer, unchanged by this work.
  Accessibility (AC-022, AC-023) is not automatable and is routed to the post-merge QA plan.
  Everything else, AC-013 through AC-021 included, is a Jest assertion.
- Q: Do we still need renderer link-run coalescing (v1 "Gap B")? → A: **No.** Content authored
  after the fix is a single `text` node carrying the `link` mark, so nothing needs coalescing.
  Healed content deliberately keeps its two anchors until an author reapplies the link — coalescing
  it in the renderers would be the same inference the heal refuses to make, just moved downstream
  and duplicated five times.

### Session 2026-09-03 (v1 — retained for the record, mostly superseded)

- **Still binding**: `enableEmoticons` (`:)`) is preserved but now inserts the literal character;
  AC-002's character sample is representative, not exhaustive; existing stored nodes are not
  migrated in Java.
- **Superseded by the v2 session**: the generated `name` → character map, `StoryBlockRenderHelper`
  pre-grouping, the `:name:` unresolved-name fallback across five renderers, the coordinated SDK
  release, and the `emoji`-node absorption rule for link runs. All were consequences of fixing
  the symptom in the renderers; v2 fixes the cause in the editor instead.

## Reproduction *(mandatory)*

**Environment**: `main` (verified against commit `69073e17f1`), `@tiptap/extension-emoji@3.22.2`,
`emoji-regex@10.6.0`, `FEATURE_FLAG_NEW_BLOCK_EDITOR` enabled. Not browser-specific — the defect
is in the stored document.

**Steps to Reproduce**:

1. Enable `FEATURE_FLAG_NEW_BLOCK_EDITOR`.
2. Edit a contentlet with a Story Block field.
3. Type `dotCMS Copyright All rights reserved` in a paragraph.
4. Select the whole line and apply a link to `https://dotcms.com`.
5. Place the cursor between `Copyright ` and `All`, then type or paste `©`.
6. Observe the underline break — one link is now two. Save the contentlet.
7. Inspect the stored field JSON.
8. Render the content through VTL and through a JS SDK, and tab through the link.

**Expected Behavior**:

- Step 6: the link stays visually and structurally intact.
- Step 7: one `text` node carrying one `link` mark, with `©` inside its text.
- Step 8: `<p><a href="https://dotcms.com">dotCMS Copyright © All rights reserved</a></p>`, one
  focus stop, one rotor entry.

**Actual Behavior**:

- Step 7 — three nodes, the middle one unmarked:

  ```json
  [
    { "type": "text", "marks": [{ "type": "link", "attrs": { "href": "https://dotcms.com" } }],
      "text": "dotCMS Copyright " },
    { "type": "emoji", "attrs": { "name": "copyright" } },
    { "type": "text", "marks": [{ "type": "link", "attrs": { "href": "https://dotcms.com" } }],
      "text": "All rights reserved" }
  ]
  ```

- Step 8 — two anchors, two tab stops; under VTL the `©` is absent entirely.

**Reproducibility**: Always, for any character in the affected class.

**Measured scope of the affected class** (each entry in the extension's `emojis` list tested
against `emoji-regex@10.6.0` plus the same `emojiToShortcode` lookup the conversion performs):

| Measure | Count |
| --- | --- |
| Entries in the extension's `emojis` list | 1949 |
| Entries that convert to an `emoji` node when typed | **1907** |
| …of those, **text-presentation** (render as typography, not as pictures) | **219** |
| …of those, pictographic | 1688 |

The 219 text-presentation characters are the ones authors type as ordinary punctuation. Beyond
the three reported:

```
‼ ⁉ ✔ ✖ ✂ ✏ ✒ ⚠ ℹ ♻ ▪ ▫ ◼ ◻ ↔ ↕ ↖ ↗ ↘ ↙ ↩ ↪ ⤴ ⤵ ⬅ ⬆ ⬇ ➡
♀ ♂ ⚧ ✝ ✡ ☪ ☯ ☮ ⚖ ⚙ ☎ ✉ 〰 〽 ✳ ✴ ❇ ⚛ ♾ ⚕ ⚜ ㊗ ㊙ Ⓜ ☑ ▶ ◀ ⏸ ⏹
```

A `✔` inside a linked list item splits that link exactly as `©` does.

**Verified findings that shape the fix** (each reproduced against a real TipTap editor in jsdom):

- **The picker already inserts a literal character.** `emoji-picker.component.ts:48` calls
  `insertContent(emoji.native)`. It only produces a node because the conversion converts the
  character straight back. Removing the conversion fixes the picker with no picker change.
- **Applying a link over an existing `emoji` node already works** — one `<a>`, and the mark
  survives a round-trip. **No schema change is needed.** The split is an *ordering* defect: the
  conversion runs after the mark is applied and rebuilds the node without it.
- **Filtering the extension's `emojis` option is not a viable fix.** That option also feeds
  rendering and shortcode resolution: `full list → "dotCMS © 2026"`, `filtered list → "dotCMS
  :copyright: 2026"`.
- **The image fallback is broader than #37340 records.** 1908 of 1949 entries carry a
  `fallbackImage` on `cdn.jsdelivr.net`, taken whenever `isEmojiSupported()` is false even with
  `forceFallbackImages: false` — producing `<img alt="copyright emoji">` and a third-party
  request. Not creating nodes removes this from newly authored content.

## Scope of Investigation *(mandatory)*

- **Affected area**: content editing only — the new Block Editor. No REST contract, DB schema, or
  Elasticsearch mapping. No renderer.
- **Suspected surface**: modern frontend, a single library.
  - `core-web/libs/new-block-editor/src/lib/editor/extensions/` — where the emoji extension is
    registered (`editor-extensions.ts:171`). Where the conversion is suppressed.
  - `core-web/libs/new-block-editor/src/lib/editor/utils/` — the document parse path, where
    `preserveUnknownNodesInDocument` already normalizes incoming JSON. Where stored `emoji` nodes
    are healed into text.
  - `core-web/libs/new-block-editor/src/lib/editor/components/toolbar/` — the picker button's
    `isAllowed('emoji')` gate. Removed, per the Allowed Blocks finding above.
  - `core-web/libs/new-block-editor/CLAUDE.md` — carries the legacy-compat rationale for
    `AIContent`; the same rationale for `emoji` belongs beside it.
- **Related known decisions**:
  - **#37175** — `link`, `emoji`, and `youtube` are registered **regardless** of a field's Allowed
    Blocks, so stored content authored with them still parses. Binding here: registration stays.
  - **#37145** — dropping a registration TipTap needs in order to parse stored content is a
    data-loss path; removing the `Highlight` mark made TipTap abort the whole document.
  - `new-block-editor/CLAUDE.md` documents the identical pattern for `AIContent`: the
    registration exists solely so old content parses, and removing it "would silently drop those
    blocks on load".
  - The plan consults `dotCMS/platform-adrs`. **ADR-0019** (date-lockstep SDK versioning) is why
    v1's SDK version-bump criterion was withdrawn; v2 touches no SDK, so it does not apply.

**Observation for planning** (not a defect): the extension's `addStorage` runs a canvas-based
`isEmojiSupported()` probe per distinct emoji version at editor construction. That cost stays as
long as the node is registered. Worth measuring, not in scope to fix.

## Root-Cause Hypothesis

`@tiptap/extension-emoji` creates `emoji` nodes from five independent paths. **All five are
authoring-time; none is needed to parse or render stored content.**

| # | Path | Trigger | Marks on the new node |
| --- | --- | --- | --- |
| 1 | `addProseMirrorPlugins` → `appendTransaction` | **any** emoji character appearing anywhere in changed text | none — `tr.replaceRangeWith` |
| 2 | `addInputRules` → `inputRegex` | typing `:copyright:` | none |
| 3 | `addInputRules` → emoticon rule (`enableEmoticons`) | typing `:) ` | none |
| 4 | `addPasteRules` → `pasteRegex` | pasting `:copyright:` | none |
| 5 | `parseHTML` → `span[data-type="emoji"]` | pasting HTML copied from another Block Editor field | whatever the paste carries |

Path 1 is the reported defect and the reason typed, pasted and picker-inserted characters all
become nodes. Paths 2–4 create the same bare node from an explicit author action. Path 5 is not a
creation *rule* but has the same effect, and a fix that closes only 1–4 lets authors keep spreading
the broken shape by copy-paste.

Paths 1–4 each call `tr.setStoredMarks(...)` after inserting. **Stored marks only affect what is
typed next** — they are never applied to the node just created. So a mark the replaced text
carried is lost at that position, splitting the surrounding run.

The two consequences follow directly:

- **Split marked text** — a bare inline node inside a marked run ends the run.
- **Dropped character downstream** — the node carries a shortcode, not a character, and **no
  consumer outside the editor can resolve one**, because the lookup table ships inside an npm
  dependency of the editor. v1 treated this as two renderer gaps to fill in five places; v2 treats
  it as the reason the node is the wrong shape for stored content — so it stops creating them and
  converts the ones that exist.

## Fix Scope & Non-Goals *(mandatory)*

**Chosen approach**: *the Block Editor never creates an `emoji` node again, and converts the ones
it finds into text — preserving their marks exactly, and inferring nothing.*

The `emoji` node type is structurally wrong for this content. It stores a shortcode instead of a
character, it cannot carry the marks around it, and nothing outside the editor understands it.
A literal character in a `text` node has none of those problems: it carries marks natively, every
renderer already handles text, and the backend already stores it. Choosing it fixes the whole
class in one change with no list to maintain across extension upgrades.

**In scope**:

- **Suppress all five node-creation paths.** Typed and pasted characters, `:shortcode:` entry,
  `:)` emoticons and HTML paste all yield a literal character in the surrounding `text` node.
- **Preserve both author shortcuts, with different output.** `:copyright:` and `:) ` still fire;
  they resolve against the extension's `emojis` table and insert the character. Losing them would
  be a functional regression; keeping the node would not fix the bug.
- **Neutralize the node's `parseHTML` so HTML paste cannot mint an `emoji` node.** Pasted emoji
  HTML lands as its character in the text node. **Stored JSON parsing is untouched** — a JSON value
  is deserialized through `Node.fromJSON`, which never consults `parseHTML`, which is why this does
  not conflict with the registration below.
- **Keep the `emoji` node registered, with `emojis` unfiltered.** Two reasons, both required:
  stored `emoji` nodes must still parse (#37145 / #37175 / `AIContent`), and the `emojis` table is
  the shortcode → character source the `:shortcode:` and `:)` input rules read. **A code
  comment must state that this is the extension's only remaining purpose** (AC-010).
- **Heal stored `emoji` nodes into text on the parse path.** Loading a document that contains one
  yields a `text` node holding the resolved character in its place. The transform is
  `emoji(marks) → text(same marks, character)` and nothing more:
  - **Marks are preserved, never inherited.** A bare node yields bare text; a node carrying a
    `link` mark (because a link was applied over it) yields text carrying that same mark.
  - **One exception, narrowly drawn: the link sandwich.** A **bare** node between two `text` nodes
    whose `link` marks match on every attribute inherits that mark, so the run rejoins into a single
    `<a>`. That shape is a fingerprint of this defect, not an editorial choice — see Clarifications.
    Every other neighbour arrangement inherits nothing (AC-016).
  - **Recursive.** `emoji` nodes occur inside headings, list items, blockquotes and table cells,
    not only paragraphs.
  - **Non-destructive on failure.** An unresolvable `name` leaves the node untouched — never blank
    output, never a literal `:name:`.
  - **Identity when absent.** A document containing no `emoji` node is returned unchanged. This is
    the overwhelmingly common case and the one to assert explicitly.
  - The healed shape reaches storage on the author's next save, like any other edit.
- **Remove the `has('emoji')` / `isAllowed('emoji')` gates** from `enableEmoticons` and from the
  toolbar picker button. `emoji` cannot be selected in Allowed Blocks, so the gates never express
  an admin's choice — they only disable emoji authoring on every field that restricts anything
  else. Removing them is a prerequisite for AC-008, not a separate improvement.
- Regression coverage at the editor layer, per Constitution Principle V.

**Explicitly out of scope / non-goals** — with the consequence each one accepts:

- **Every renderer change.** No VTL, no Java, no React/Vue/Angular SDK, no `libs/sdk/types` enum
  member, no generated shortcode map, no changelog or version bump.
  *Consequence*: an `emoji` node in a field nobody ever opens in the editor still renders as nothing
  in VTL and as an unknown block in the SDKs. Accepted — the heal reaches everything an author
  touches, no shipped renderer ever supported the node, and nothing creates new ones after this fix.
  **Track separately if a second report arrives.**
- **Inferring marks during the heal, beyond the single link-sandwich signature.** The heal does not
  look at neighbours in any other arrangement, does not merge across differing mark attributes, and
  does not re-mark a node that carries marks of its own.
  *Consequence*: a symbol that sat between two links to **different** URLs, or next to a
  `hardBreak`, or at a block boundary, heals to unmarked text and stays outside both anchors. That
  is correct — those shapes carry no defect fingerprint, and guessing at them would change content
  for users who never had this bug.
- **Any Java transform, data migration, or heal script.** The heal is a pure JSON transform on the
  editor's parse path. Keeps this change out of the DB and out of
  [rollback-unsafe territory](../../docs/core/ROLLBACK_UNSAFE_CATEGORIES.md) entirely.
- **A report *feature* for contentlets carrying the split** — no endpoint, no UI, no scheduled job.
  #37340 asks for one; the heal makes it largely unnecessary, since content repairs itself as fields
  are opened. Productizing a report is a separate issue if it is ever needed.
- **Removing the `emoji` node registration.** Explicitly preserved for backward compatibility.
- **The legacy Block Editor** (`core-web/libs/block-editor`). It never registered the extension,
  so it cannot create this split.
- **Shortcode authoring as a feature.** The `:` *suggestion* trigger stays inert by design; this
  change neither adds nor restores a `:rocket:` autocomplete menu. Only the input rule fires.
- **Reworking the image-fallback path.** The `cdn.jsdelivr.net` dependency and the
  `<img alt="… emoji">` accessible name are recorded as findings; new content stops hitting them.
- **The `isEmojiSupported()` startup cost.** Measured, not fixed here.
- **Emoji-only-link accessible naming.** A link whose entire text is one emoji may fail WCAG
  2.4.4. The link popover already supports `aria-label`. Authoring guidance, not a code defect.

## Regression Risk *(mandatory)*

- **Blast radius**: one Angular library, behind an opt-in feature flag. Two hot paths inside it:
  - **The extension's plugin/rule set**, on every keystroke in every Block Editor field. The
    change *removes* work from that path rather than adding it.
  - **The document parse path**, on every field load. The heal must be a cheap recursive walk that
    returns the input unchanged when no `emoji` node is present — the overwhelmingly common case,
    and the one AC-019 asserts. It runs once per load, not per keystroke.
  - Compared with v1, `VM_global_library.vm` — which renders every Story Block field on every
    VTL page for every customer, flag or not — is no longer touched at all. That was v1's single
    highest-risk edit.
- **Backward compatibility**:
  - Stored `emoji` nodes MUST keep parsing. The node stays registered and `emojis` stays
    unfiltered; the verified failure mode of filtering is rendering `:copyright:` as literal text.
  - **The HTML-string load path changes shape, not content.** `normalizeEditorContent` falls
    through to HTML for any non-JSON string value, which is not how dotCMS stores Story Block
    fields but is reachable for hosts that pass HTML. With `parseHTML` neutralized, an emoji span
    in such a value resolves to its character as text instead of to an `emoji` node — the span
    already contains the character. The plan should confirm this empirically, including the
    `fallbackImage` variant where the span wraps an `<img>` rather than a character.
  - **The heal is lossy in exactly one direction, by design**: a node's `name` is replaced by its
    character and the shortcode leaves storage. That is the intent — the character is the content,
    and no consumer outside the editor can resolve a shortcode anyway.
  - **The heal must not damage content that never had this bug.** This is the change's sharpest
    risk, and it is why marks are preserved rather than inherited everywhere except one signature.
    Three criteria bound it: AC-014 (marks preserved exactly), **AC-016 (every negative case of the
    sandwich rule, asserted individually)** and AC-019 (a document with no `emoji` node is returned
    unchanged). AC-016 is the one that keeps the permitted inference from widening over time, and it
    is the test most worth reviewing carefully.
  - **The sandwich inference is bounded and visible.** It fires only on a bare node between two text
    nodes with attribute-identical `link` marks. It cannot arise from content authored after this
    fix, because no typed symbol becomes a node. If it were ever wrong, the result is a visibly
    longer underline, reversible by selecting and removing the link — not a silent rewrite.
  - **Cross-editor exposure** (pre-existing, improved): content carrying `emoji` nodes opened in
    the **legacy** editor is a loss path — no registration there, so by the `AIContent` mechanism
    the node is dropped on load. This change reduces future exposure by not creating nodes, and
    *removes* it for any field healed and re-saved through the new editor. **The plan should
    confirm the legacy-drop behavior empirically.**
  - **The heal merges the runs it creates; ProseMirror does not.** An earlier draft assumed
    normalization joined adjacent `text` nodes carrying identical marks. **Measured, and false on
    every load path** — `Fragment.fromJSON` constructs the fragment directly and never calls
    `Fragment.fromArray`, which is where joining lives (research.md R10). The editor *renders* one
    `<a>` because the DOM serializer emits a mark run as one element, which is why the assumption
    survived review; the stored document keeps three nodes, and VTL and the SDKs emit one `<a>` per
    text node. Leaving them unmerged would produce **three** anchors downstream where the defect
    currently produces two — worse than the bug.
  - **The merge is scoped to inline arrays the heal actually touched.** It never runs over a
    document that contained no `emoji` node, and never merges across differing marks. Both are
    asserted (AC-016, AC-017). A document-wide merge would silently normalize adjacent identical-mark
    runs that have nothing to do with this defect — the same class of harm the mark rules exist to
    prevent, arriving through the back door.
  - **Merging is not inference.** The marks are already identical; concatenating the text decides
    nothing and is exactly what `Fragment.fromArray` does. The sandwich rule (AC-015) remains the
    single place this change infers anything.
  - No REST contract, DB schema, or ES mapping change. Not rollback-unsafe in any documented
    category.
- **Data considerations**: an `emoji` node persists only until a field is opened and saved through
  the new editor. Because the heal turns it into a `text` node, three server-side blind spots
  resolve themselves with **no Java change**: `StoryBlockUtil.isTextContentEmpty` (which counts only
  `"text"` nodes and feeds required-field validation at `ESContentletAPIImpl.java:8180`, so a
  required Story Block field whose only content is one emoji currently fails to save as "empty"),
  `StoryBlockUtil.getCharCount` (emoji nodes count as zero characters), and Elasticsearch text
  extraction. Those three are live defects today, independent of links, and none is reported.

## Acceptance & Verification *(mandatory)*

**Editor — root cause**

- **AC-001**: The reproduction steps no longer produce the actual behavior. Step 7 yields exactly
  one `text` node carrying one `link` mark whose text contains `©`.
- **AC-002**: Typing or pasting any character in the affected class into paragraph text stores a
  plain `text` node and creates **no** `emoji` node. Verified against a **representative sample**:
  all three reported symbols, ~20 further text-presentation characters, and a handful of
  pictographic and multi-codepoint cases including a ZWJ sequence and a flag.
- **AC-003**: AC-002 holds at the **start** and at the **end** of linked text, not only mid-run.
- **AC-004**: AC-002 holds when the character arrives by **paste** — plain-text paste, HTML paste,
  and `&copy;` HTML-entity paste.
- **AC-005**: Inserting an emoji from the toolbar picker inserts the literal character into the
  surrounding text node, and inside a link yields a **single** `<a>` in the editor DOM.
- **AC-006**: Typing `:copyright:` inserts `©` into the surrounding text node and creates **no**
  `emoji` node. Pasting the same shortcode does the same. An unknown shortcode is left as typed.
- **AC-007**: Typing a `:)`-style emoticon inserts the literal character into the surrounding text
  node, creates **no** `emoji` node, and preserves the author's surrounding whitespace.
- **AC-008**: Emoji authoring is available on **every** Block Editor field: the toolbar picker
  button is rendered and the `:)` / `:shortcode:` rules fire on a field with a restricted
  `allowedBlocks` list exactly as on an unrestricted one. Regression-guarded against the current
  behavior, where restricting any block silently removes both.
- **AC-009**: A field whose stored `allowedBlocks` value happens to contain `emoji` — writable
  through the field-variable API even though the settings UI never offers it — behaves identically
  to one that does not. The value is inert, not an error.
- **AC-010**: `editor-extensions.ts` (or the extension module it delegates to) carries a comment
  stating that the `emoji` extension is registered **only** so stored `emoji` nodes parse and so
  `:shortcode:` / emoticon entry can resolve a character, that no code path creates `emoji` nodes,
  and that removing the registration would silently drop stored nodes on load — mirroring the
  `AIContent` comment. `new-block-editor/CLAUDE.md` reflects the same.
- **AC-011**: The character renders as text; no `<img alt="… emoji">` is emitted for newly
  authored content.
- **AC-012**: Pasting HTML that contains `<span data-type="emoji">` — the shape produced by copying
  content out of another Block Editor field — creates **no** `emoji` node; the character lands in
  the surrounding text node. Asserted together with AC-013, which proves the same change leaves
  stored-JSON parsing intact — `parseHTML` governs HTML entry points only.

**Editor — healing stored nodes**

- **AC-013**: Loading a document that contains an `emoji` node yields a document with **no** `emoji`
  node: each has become a `text` node holding the character resolved from its `attrs.name`. Within
  each inline array the heal touched, adjacent `text` nodes whose marks are **deep-equal** are then
  merged into one — the normalization the JSON load path does not perform for us (research.md R10).
- **AC-014**: The resulting `text` node carries **exactly** the marks the `emoji` node carried —
  none for a node created by the defect, and the `link` mark for a node a link was applied over.
  **No mark is inherited from a neighbouring node**, with the single narrow exception in AC-015.
  This is the criterion that keeps the transform from altering content unrelated to this defect.
- **AC-015**: **The link sandwich.** Where a **bare** `emoji` node sits between two **`text`** nodes
  whose `link` marks are equal on **every** attribute (`href`, `target`, `rel`, `title`,
  `aria-label`), the healed text node carries that same `link` mark, **and the heal merges the
  resulting identical-mark run into a single `text` node** (AC-013). The reported payload therefore
  renders as a **single** `<a>` with the character inside it — **with no re-save and no author
  action** — in the stored JSON, not merely in the editor's DOM.
- **AC-016**: **The sandwich rule does not fire otherwise.** No mark is inherited when any of these
  holds, each asserted separately:
  - the two `link` marks differ in **any** attribute;
  - either immediate sibling is not a `text` node — `hardBreak`, another `emoji`, an image, or the
    block boundary itself when the node is first or last;
  - either sibling carries no `link` mark;
  - the `emoji` node is **not** bare — a node carrying its own marks keeps them under AC-014 and is
    never re-marked from a neighbour.

  The merge carries the same boundary, asserted alongside:
  - `text` nodes whose marks **differ in any attribute** are never merged, however adjacent;
  - an inline array the heal did **not** touch is never merged, even when it already holds adjacent
    identical-mark `text` nodes. The heal normalizes what it creates, not what it finds.
- **AC-017**: The heal is recursive — `emoji` nodes inside headings, list items, blockquotes and
  table cells are healed identically to those in paragraphs.
- **AC-018**: An `emoji` node whose `name` does not resolve against the `emojis` table is left
  **untouched** — never replaced with empty text and never with a literal `:name:`.
- **AC-019**: A document containing no `emoji` node is returned **unchanged** by the heal (identity
  path). Asserted explicitly, because it is the overwhelmingly common case.
- **AC-020**: Removing the `emoji` extension registration is proven to lose that content — the
  regression guard for AC-013. Asserted by parsing the fixture document with the extension absent
  and observing the node is dropped (the `AIContent` / #37145 mechanism).
- **AC-021**: **The reported payload heals completely.** Loading
  `text(link) + emoji(no marks) + text(link)` with identical `link` marks yields exactly **one**
  `text` node carrying that mark, whose text contains `©` — the shape that renders as a single `<a>`
  in VTL and in every SDK, with no renderer change and no re-save. This is AC-015 applied end to end
  to the customer's actual data.

**Accessibility verification**

- **AC-022**: Tabbing through the rendered link produces exactly **one** focus stop — both for
  content authored after the fix and for the healed reported payload (AC-021).
- **AC-023**: The NVDA Elements List / VoiceOver rotor shows **one** entry carrying the complete
  accessible name, including the symbol.

**Verification method**:

- **Unit (Jest/Spectator)** in `core-web/libs/new-block-editor`, driving a real TipTap editor and
  asserting document shape — AC-001 through AC-012. Parameterize over a character sample so the
  class, not three literals, is covered. Use real `NgZone` in service tests; a mocked one breaks
  the change-detection scheduler.
  `pnpm nx test new-block-editor`
- **Unit (Jest)** for the heal as a pure JSON transform — AC-013 through AC-019 and AC-021. Fast,
  no editor instance needed. Three of these bound the risk and must each be asserted on their own,
  never folded into a broader test: **AC-014** (marks preserved, none inherited), **AC-016** (every
  negative case of the sandwich rule) and **AC-019** (identity path). AC-016 in particular is what
  keeps the one inference this spec permits from widening.
- **Unit (Jest/Spectator)** for AC-020 and for the AC-012 ↔ AC-013 pair — the same fixture must
  survive a JSON load with `parseHTML` neutralized, proving parse rules govern HTML entry points
  only.
- **Regression fixture** — the exact three-node payload from Actual Behavior, shared across the
  editor suites so every assertion runs against identical input.
- **Manual accessibility pass** — AC-022 and AC-023, with NVDA and with VoiceOver, on content
  authored after the fix. Not automatable; record in the post-merge QA plan. Run it on new content
  and on a field healed from the reported payload, which AC-015 rejoins into one anchor.
- **Merge coverage** — the heal's own normalization is asserted in the same pure-transform suite:
  identical marks merge, differing marks never do, and an inline array the heal did not touch is
  returned as found. This replaces what was an empirical check on ProseMirror; the library does not
  perform the merge (research.md R10).
- **Manual cross-editor check** — open a field carrying `emoji` nodes in the legacy editor before
  and after this change, to confirm the pre-existing drop path is unchanged (Regression Risk).

**Test types deliberately not used** (Constitution Principle V requires this be stated, not
implied):

| Test type | Used | Why |
| --- | --- | --- |
| Unit (Jest/Spectator) | **Yes** | Every automatable acceptance criterion lives in the editor. |
| Integration (JUnit) | No | No Java, no database, no API is touched. v1 needed these because it changed `StoryBlockRenderHelper`; v2 does not. |
| Postman | No | No REST endpoint changes. The VTL render path is unmodified. |
| Karate | No | Same — no API surface. |
| e2e (Playwright) | No | The storage round-trip it would prove is plain ProseMirror JSON serialization, already covered at the unit layer. Adds a running-instance dependency for no new assertion. |
| Manual | **Yes** | **AC-022 and AC-023 only** — screen-reader output, not automatable, recorded in the post-merge QA plan. Every other criterion, AC-013 through AC-021 included, is a Jest test. AC-014, AC-016 and AC-019 in particular bound the heal's risk and must not be routed here. |

Per Constitution Principle V, the tests that **are** written are developer-approved and confirmed
failing (Red) before any implementation.

## Assumptions

- **This spec supersedes an acceptance criterion on issue #37340**, which requires "genuine
  pictographic emoji inserted via the emoji picker still work and still produce `emoji` nodes".
  This approach deliberately stops producing `emoji` nodes for **all** newly authored content,
  pictographic included. **Action: amend that AC on #37340 before this spec is approved**, so the
  issue and the spec do not disagree in the record.
- **#37340 also frames two renderer gaps ("no `emoji` branch", "no link coalescing") as in-scope.**
  v2 declares both non-goals and explains why in Fix Scope. **Action: record that on the issue too.**
- **#37340's migration criterion is now met in full, and needs no amendment**: "already-split
  content renders as a single link **without requiring a re-save**." The heal restores the character
  on load, and the link-sandwich rule (AC-015) rejoins the run into one `<a>`. This criterion was
  going to be amended until PR #37434 review raised the narrow signature; it is met instead.
  **Action: only two ACs on #37340 need amending, not three.**
- Emoji as a literal character in a text node is acceptable product behavior. It is already what
  the picker produces and what the legacy Block Editor has always produced.
- Losing shortcode round-tripping (`:rocket:` in storage) for new content is acceptable; the
  character is the content.
- The customer on helpdesk ticket 39197 authored the affected content in the **new** Block Editor.
  Only the new editor registers the extension. **Unconfirmed** — the triage precondition recorded
  on #37340. If they were on the legacy editor, this diagnosis does not explain their data.
- All measurements are against `@tiptap/extension-emoji@3.22.2` and `emoji-regex@10.6.0`. The
  counts are version-specific; the defect class is not.

## Resolved Decisions

| # | Question | Decision | Rationale / consequence |
| --- | --- | --- | --- |
| 1 | Fix boundary — editor, renderers, or both? | **Editor only** | The cause is authoring-side, and the shortcode table only exists there. Fixing it in the editor removes the need for five renderer implementations, a generated map, and an SDK release. |
| 2 | Existing stored `emoji` nodes | **Healed on the editor's parse path** | `emoji(marks) → text(same marks, character)`. The shortcode table already ships inside the editor's dependency, so nothing crosses a build boundary. No Java, no migration, no heal script. |
| 3 | Should the heal inherit neighbouring marks and rejoin the split link? | **No in general; yes for the link sandwich** | A general "inherit from a neighbour" rule infers one author's intent from one payload shape. But a **bare** node between two `text` nodes with attribute-identical `link` marks is a fingerprint this defect alone produces — the editor marks the node when a link is applied over it, and after this fix no typed symbol becomes a node. Raised in PR #37434 review; adopted because it closes #37340's "single link without a re-save" criterion rather than amending it, and because a wrong merge is visible and reversible rather than silent. AC-014, AC-015, AC-016. |
| 4 | `:shortcode:` and `:)` shortcuts | **Kept, output changed to a literal character** | Removing them would be a functional regression; keeping the node would not fix the bug. AC-006, AC-007. |
| 5 | Emoji node registration | **Preserved, `emojis` unfiltered** | #37145 / #37175 / `AIContent`: dropping a registration TipTap needs to parse stored content loses the content. Filtering `emojis` verifiably renders `:copyright:` as text. |
| 6 | Renderer link-run coalescing (v1 "Gap B") | **Dropped** | Retyped content is one marked `text` node, so nothing remains to coalesce for this defect. A genuinely latent coalescing gap can be filed on its own merits. |
| 7 | AC-002 character sample | **Representative, not exhaustive** | Three reported symbols, ~20 further text-presentation characters, plus pictographic and multi-codepoint cases. Stable across extension upgrades. |
| 8 | HTML paste re-creating `emoji` nodes | **Neutralize `parseHTML`** | Closing paths 1–4 alone still lets copy-paste between fields mint new nodes, defeating the fix's one guarantee. Stored JSON parsing is unaffected (`Node.fromJSON` ignores `parseHTML`). AC-012, AC-013. |
| 9 | `emoji` in Allowed Blocks | **Gates removed** | `emoji` is not selectable in Allowed Blocks (`getEditorBlockOptions()` offers block nodes only, #37175), so `has('emoji')` only ever fires on fields that restrict something else — silently removing the picker and `:)` from them. AC-008, AC-009. |
| 10 | Finding content that needs re-entry | **No longer needed** | Superseded by Decision 2 — content repairs itself as fields are opened, so a locating query stopped being the remedy's backbone. A report feature stays out of scope. |
| 11 | Test types skipped | **Jest only, justified in writing** | No Java, DB, REST, renderer or build artifact is touched, so integration/Postman/Karate/e2e have nothing to assert. Accessibility is manual. Recorded rather than left silent, per Principle V. |
| 12 | Keep the node and let it carry marks instead? | **Rejected** | Fixing mark inheritance makes rendering *worse* — `text(link) + emoji(link) + text(link)` is three anchors, not one; today it is two. The node still stores a shortcode, so a ~1949-entry table is needed in Java plus four JS renderers. It is also the only option where the affected set **grows**. |

**Still to confirm outside this spec** — actions, not clarifications:

- Amend the pictographic-emoji AC on [#37340](https://github.com/dotCMS/core/issues/37340), and
  record that the two renderer gaps are now non-goals with the reasoning above.
- Confirm from helpdesk ticket 39197 that the customer authored in the **new** Block Editor.

## Revision History

| Version | Date | Change |
| --- | --- | --- |
| v1 | 2026-09-03 | Original spec. Editor fix **plus** renderer Gap A (`emoji` branch in VTL + four JS renderers), Gap B (link coalescing in five renderers + `StoryBlockRenderHelper`), a build-generated shortcode map crossing the JS → Java boundary, and a coordinated SDK release. Implemented on `…-37340-…-impl`: 33 files, ~2.4k lines, two ACs withdrawn mid-implementation (v1 AC‑012 map, v1 AC‑022 version bump vs ADR-0019). |
| v2.1 | 2026-09-07 | **The link-sandwich exception**, added after review on PR #37434. The heal still preserves marks rather than inheriting them, with one narrow signature carved out: a bare `emoji` node between two `text` nodes carrying attribute-identical `link` marks inherits that mark, so the reported payload rejoins into a single `<a>` with no re-save. Bounded by AC-016's negative cases. Corrects a conflation in `research.md` R8, which had rejected this on the grounds that ProseMirror already handled it — it handles the *already-marked* node, not the bare one. |
| v2 | 2026-09-07 | **Scope reduced to the editor.** Every renderer, SDK, Java and release criterion becomes a non-goal with its consequence stated. Clarify added the `parseHTML` paste path, removed the Allowed Blocks gates, and recorded the skipped test types. An options review then settled the treatment of stored nodes: they are **healed into text on load**, by a transform that preserves marks exactly and infers nothing — restoring the character everywhere without guessing at the link. |
