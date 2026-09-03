# Issue Resolution Specification: Block Editor emoji conversion splits marked text and drops characters

**Feature Branch**: `37340-emoji-text-node`

**Created**: 2026-09-03

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#37340](https://github.com/dotCMS/core/issues/37340) — *AC amendment required, see Assumptions*

**Input**: User description: "Giving you the context of the ticket and my two doubts; let's start the specify fix. Keep in mind that we want to preserve the node registration for compatibility, and we need to fix the whole class. I think the best approach is that if an emoji is added inside a paragraph, it should be part of that node instead of a standalone emoji node."

<!--
  This is the dotCMS ISSUE-RESOLUTION spec (used by /speckit-specify-fix). Unlike the
  feature spec, it is framed around a defect: what is wrong, how to reproduce it, and how
  we will know it is fixed. It still flows into /speckit-plan, where the Legacy Impact and
  ADR Alignment gates apply. Keep this technology-light — root-cause and fix details are
  refined in the plan.
-->

## Problem Statement *(mandatory)*

When a content author types or pastes any character that Unicode classifies as an emoji into
Block Editor text, the editor silently replaces that character with a standalone `emoji`
node. The replacement node is created **bare** — it carries none of the formatting that
surrounded it.

Two consequences follow, both persisted into the stored Story Block JSON:

1. **Marked text is torn in two.** A single linked phrase becomes `text(link)` +
   `emoji(no marks)` + `text(link)`. The rendered output is two `<a>` elements where the
   author created one.
2. **The character is lost on published pages.** The stored node holds only a shortcode
   (`{"type":"emoji","attrs":{"name":"copyright"}}`) — no literal character. Consumers with
   no `emoji` branch drop it entirely: the VTL renderer emits nothing, and **all three JS SDKs
   (React, Vue, Angular)** fall through to their unknown-block component.

The customer reported this for `©`, `®`, and `™`. Those three are not a special case — they
are the visible symptom of a defect that spans a large class of characters (measured below).

**Severity / Impact**: Medium.

- **Who**: any author using the new Block Editor (`FEATURE_FLAG_NEW_BLOCK_EDITOR`), plus every
  downstream consumer of the content they authored.
- **Accessibility** — the reported symptom. One logical link yields two keyboard tab stops and
  two entries in the NVDA Elements List / VoiceOver rotor, with the announcement fragmented
  ("link dotCMS Copyright", then "link All rights reserved"). Maps to WCAG 2.2 Level A
  failures: 1.3.1 Info and Relationships, 2.4.4 Link Purpose (In Context), 4.1.2 Name, Role,
  Value. Closest documented technique is H2 (combining adjacent links to the same resource).
- **Content loss** — on VTL-rendered pages the character disappears from published output.
  Legal symbols (`©`, `®`, `™`) vanishing from a footer is a customer-visible defect
  independent of accessibility.
- **How often**: every time an affected character is typed. Links still resolve and navigate,
  which is why this is Medium rather than High.

## Reproduction *(mandatory)*

**Environment**: `main` (verified against commit `69073e17f1`), `@tiptap/extension-emoji@3.22.2`,
`emoji-regex@10.6.0`. Requires `FEATURE_FLAG_NEW_BLOCK_EDITOR` enabled. Not browser-specific —
the defect is in the stored document, so it reproduces in any browser and in any renderer.

**Steps to Reproduce**:

1. Enable `FEATURE_FLAG_NEW_BLOCK_EDITOR`.
2. Edit a contentlet with a Story Block (Block Editor) field.
3. Type `dotCMS Copyright All rights reserved` in a paragraph.
4. Select the whole line and apply a link to `https://dotcms.com`.
5. Place the cursor between `Copyright ` and `All`, then type or paste `©`.
6. Observe the underline break — the single link is now two links. Save the contentlet.
7. Inspect the stored field JSON.
8. Render the content through VTL and through the React/Vue SDK on a page.
9. Tab through the rendered link.

**Expected Behavior**:

- Step 6: the link stays visually and structurally intact.
- Step 7: one `text` node carrying one `link` mark, with `©` inside its text.
- Step 8: `<p><a href="https://dotcms.com">dotCMS Copyright © All rights reserved</a></p>`.
- Step 9: exactly one focus stop; one screen-reader entry with the complete accessible name.

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

- Step 8 — two anchors, and under VTL the `©` is absent entirely:

  ```html
  <p>
    <a href="https://dotcms.com">dotCMS Copyright </a>
    <span data-name="copyright" data-type="emoji">©</span>
    <a href="https://dotcms.com">All rights reserved</a>
  </p>
  ```

- Step 9 — two tab stops, two rotor entries, fragmented announcement.

**Reproducibility**: Always, for any character in the affected class.

**Measured scope of the affected class** (script run against the shipped extension: each entry
in the extension's `emojis` list tested against `emoji-regex@10.6.0` followed by the same
`emojiToShortcode` lookup the conversion performs):

| Measure | Count |
| --- | --- |
| Entries in the extension's `emojis` list | 1949 |
| Entries that convert to an `emoji` node when typed | **1907** |
| …of those, **text-presentation** (no `Emoji_Presentation` property — render as typography, not as pictures) | **219** |
| …of those, pictographic | 1688 |

The 219 text-presentation characters are the ones authors type as ordinary punctuation and
have no reason to expect to become emoji. They include, beyond the three reported:

```
‼ ⁉ ✔ ✖ ✂ ✏ ✒ ⚠ ℹ ♻ ▪ ▫ ◼ ◻ ↔ ↕ ↖ ↗ ↘ ↙ ↩ ↪ ⤴ ⤵ ⬅ ⬆ ⬇ ➡
♀ ♂ ⚧ ✝ ✡ ☪ ☯ ☮ ⚖ ⚙ ☎ ✉ 〰 〽 ✳ ✴ ❇ ⚛ ♾ ⚕ ⚜ ㊗ ㊙ Ⓜ ☑ ▶ ◀ ⏸ ⏹
```

A `✔` inside a linked list item splits that link exactly as `©` does.

**Additional verified findings that shape the fix** (each reproduced against a real TipTap
editor in jsdom):

- **Applying a link over an *existing* `emoji` node already works.** The link mark is applied,
  one `<a>` is produced, and the mark survives a save/reload round-trip. `allowsMarkType(link)`
  reports `false` for the node, but ProseMirror does not enforce that for atoms in these code
  paths. **No schema change is needed for mark inheritance.**
- **The split is therefore an ordering defect, not a schema limitation.** The conversion runs
  after the mark is applied and rebuilds the node without marks, destroying the mark that was
  already there.
- **Narrowing the extension's `emojis` option is not a viable fix.** That option also feeds
  rendering, so removing an entry makes already-stored nodes of that name render as a literal
  shortcode. Verified:

  ```text
  full list      → "dotCMS © 2026"
  filtered list  → "dotCMS :copyright: 2026"
  ```

- **The image fallback is broader than the issue records.** 1908 of 1949 entries carry a
  `fallbackImage` pointing at `cdn.jsdelivr.net`. The render path takes it whenever
  `isEmojiSupported()` returns false, even with `forceFallbackImages: false`, producing
  `<img alt="copyright emoji">` and a third-party request from the editor. The 26
  `regional_indicator_*` entries have no `version`, so their support check is
  unconditionally false and they always render as an image.

## Scope of Investigation *(mandatory)*

<!--
  Keep to WHAT is affected, not the code-level fix (that is the plan's job). But DO name the
  product area, since dotCMS mixes modern and legacy surfaces — this drives Legacy Impact in
  the plan.
-->

- **Affected area**: content editing (new Block Editor authoring) and page rendering (Story
  Block renderers — VTL and the JS SDKs). No REST contract, DB schema, or Elasticsearch
  mapping is involved unless a data migration is chosen (see Regression Risk).
- **Suspected surface**: predominantly modern and frontend.
  - `core-web/libs/new-block-editor` — Angular, modern. Where the conversion is registered.
  - `core-web/libs/sdk/react`, `core-web/libs/sdk/vue`, `core-web/libs/sdk/angular` — modern.
    **The Angular SDK ships two independent Block Editor renderers** (`dotcms-block-editor-renderer`
    and the semantic/native successor `dotcms-block-editor-renderer-semantic`), each with its own
    node dispatch and its own recursive mark renderer. Neither is mentioned in #37340; both carry
    Gap A and Gap B identically. Verified: `grep` for an `emoji` node type across `libs/sdk/`
    returns **zero** hits in any SDK.
  - `core-web/libs/sdk/types` — the shared `internal.ts` enum of Story Block node types has no
    `emoji` member; all three JS SDKs dispatch from it.
  - `dotCMS/src/main/webapp/WEB-INF/velocity/` (`VM_global_library.vm`, `static/storyblock/render.vtl`)
    — **legacy Velocity macro surface**. Not `com.dotmarketing.*` Java, but legacy in style and
    the highest-risk edit in this change: the macro is shared by all Story Block rendering.
  - No Java package under `com.dotcms.*` or `com.dotmarketing.*` is expected to change.
- **Related known decisions**:
  - #37175 established that `link`, `emoji`, and `youtube` are registered **regardless of a
    field's Allowed Blocks**, so that stored content authored with them still parses. That
    decision is binding here and is why registration is preserved.
  - #37145 established that dropping a registration TipTap needs to parse stored content is a
    data-loss path — removing the `Highlight` mark caused TipTap to abort the whole document.
  - `core-web/libs/new-block-editor/CLAUDE.md` documents the same legacy-compat reasoning for
    the `AIContent` node: the registration exists solely so old content parses, and removing
    it "would silently drop those blocks on load".
  - The plan formally consults `dotCMS/platform-adrs`.

**Observation for planning** (not a defect): the extension's `addStorage` runs a canvas-based
`isEmojiSupported()` probe once per distinct emoji version at editor construction. This cost
remains as long as the node is registered, and is worth measuring but is not in scope to fix.

## Root-Cause Hypothesis

The `emoji` extension's `appendTransaction` hook runs on **every** document change. It scans
changed text nodes with `emoji-regex`, and for each match found in its `emojis` list it
replaces the matched range with a new inline `emoji` node.

Two properties of that replacement produce the defect:

1. **It is indiscriminate.** The gate is "is this character in the Unicode emoji set", which
   is true for 1907 of 1949 catalogued characters — including 219 that render as typography
   and that authors type as ordinary text. There is no notion of author intent.
2. **It discards formatting.** The replacement creates the node with no marks. The hook does
   set stored marks afterwards, but stored marks only affect what the author types *next* —
   they are never applied to the node just created. So any mark the replaced text carried is
   lost at that position, splitting the surrounding run.

The two renderer gaps identified in the issue are **independent pre-existing defects** that
this bug makes visible, and both must be addressed for already-stored content:

- **Gap A** — no renderer has an `emoji` branch, so the character is dropped (VTL) or renders
  as an unknown block (React, Vue, and both Angular renderers).
- **Gap B** — no renderer coalesces adjacent text nodes that share an identical `link` mark, so
  each emits its own `<a>`. All five renderer implementations use the same recursive
  "outermost mark wraps the rest" pattern and none looks at its siblings, which is precisely
  why none of them coalesce. This is latent for any stored JSON with adjacent same-link text
  nodes and predates the new editor.

## Fix Scope & Non-Goals *(mandatory)*

**Chosen approach** (developer decision, per user input): *an emoji typed or pasted into
paragraph text stays part of that text node.* Suppress the automatic
character-to-node conversion entirely, while keeping the `emoji` node **registered** so that
content already saved with `emoji` nodes continues to parse and render.

This approach was selected over a per-character allowlist because it fixes the entire class in
one change, requires no list to maintain across extension upgrades, and eliminates the marks
problem structurally — text nodes carry marks natively, so there is no node left to strip
them. It is also consistent with how insertion already works: the emoji picker calls
`insertContent(emoji.native)` with a raw character, so the picker needs no `emoji` node to
function.

**In scope**:

- Suppress automatic conversion of typed or pasted characters into `emoji` nodes, for the
  whole affected class — all 1907 convertible characters, not only `©`/`®`/`™`.
- Keep the `emoji` node registered, with its `emojis` option **unfiltered**, so already-stored
  `emoji` nodes still parse and still resolve their character.
- Keep emoji insertion working end to end: the toolbar picker inserts the literal character
  into the text node, and it inherits the surrounding marks like any other typed character.
- Redirect the `:)`-style emoticon input rule (`enableEmoticons`, gated on `has('emoji')`) to
  insert the literal character into the text node instead of creating an `emoji` node. The
  shortcut is preserved for authors who use it; only its output changes.
- Preserve behavior parity whether or not `emoji` appears in a field's Allowed Blocks.
- **Gap A** — add an `emoji` branch to the VTL Story Block renderer and to **all four JS SDK
  renderers** (React, Vue, Angular standard, Angular semantic), plus the `emoji` member in the
  shared `libs/sdk/types` node-type enum, so already-stored `emoji` nodes render their character
  as inline text and never as a block-level element inside a `<p>`.
- **Gap B** — coalesce adjacent text nodes sharing an identical `link` mark into a single `<a>`
  in VTL and in all four JS SDK renderers, comparing full mark attributes so links differing in
  `href`, `target`, `rel`, `title`, or `aria-label` stay separate.
- Regression coverage at each layer, per Constitution Principle V.

**Explicitly out of scope / non-goals**:

- **The legacy Block Editor** (`core-web/libs/block-editor`). It never registered the emoji
  extension, so it cannot create this split.
- **Removing the `emoji` node registration.** Explicitly preserved for backward compatibility.
- **Shortcode authoring as a feature.** The `:` suggestion trigger is already inert by design;
  this change does not add or restore `:rocket:` shortcode entry.
- **A report of affected contentlets.** #37340 asks for a query identifying content already
  carrying the split. It is read-only and useful, but explicitly deferred out of this change —
  track it separately.
- **Reworking the extension's image-fallback path** beyond what Gap A requires. The
  `cdn.jsdelivr.net` dependency and the `<img alt="… emoji">` accessible name are recorded
  here as findings and should be tracked separately.
- **The `isEmojiSupported()` startup cost.** Measured and noted, not fixed here.
- **Any wholesale rewrite of the VTL Story Block macro.** Gap A and Gap B are additive edits;
  progressive enhancement only, per Constitution Principle I.
- **Emoji-only-link accessible naming.** A link whose entire text is one emoji gets its
  accessible name from that character and may fail WCAG 2.4.4. The link popover already
  supports `aria-label` for this. Authoring guidance, not a code defect.

## Regression Risk *(mandatory)*

- **Blast radius**:
  - **Highest risk is the VTL macro.** `VM_global_library.vm` renders *every* Story Block field
    on *every* VTL-rendered page, for all customers, flag or no flag. Gap B changes how text
    runs are grouped there. A defect introduced in coalescing affects far more content than
    this bug does.
  - The React, Vue, and both Angular SDK text renderers carry the same coalescing risk for
    headless consumers — five implementations of the same logic, which must stay behaviourally
    identical or the same content renders differently per framework.
  - Within the editor, suppressing conversion touches the shared `appendTransaction` path, so
    every Block Editor keystroke is on that path.
  - **Cross-editor exposure**: content authored in the new editor and then opened in the
    **legacy** editor is a pre-existing loss path — the legacy editor has no `emoji`
    registration, and by the mechanism documented for `AIContent` the node would be dropped on
    load. This change reduces future exposure by not creating new nodes, but does not fix
    existing ones. **The plan should confirm this behavior empirically.**
- **Backward compatibility**:
  - Stored `emoji` nodes MUST keep working. The node stays registered and `emojis` stays
    unfiltered — the verified failure mode of filtering it is rendering `:copyright:` as
    literal text.
  - No REST contract, DB schema, or ES mapping changes. Not rollback-unsafe in the
    [documented categories](../../docs/core/ROLLBACK_UNSAFE_CATEGORIES.md) **unless** a data
    migration is chosen.
  - Renderer coalescing must not merge across differing mark attributes, and must keep other
    marks (e.g. `bold`) nested correctly inside the single `<a>`.
  - This change **contradicts an acceptance criterion in issue #37340**, which requires
    pictographic emoji to keep producing `emoji` nodes. See Assumptions.
- **Data considerations**:
  - Content already saved with the split needs to render as one link **without a re-save**.
    Gap B coalescing achieves this at render time and is the preferred path.
  - Existing `emoji` nodes remain in stored content indefinitely. Gap A is what keeps them
    rendering, which is why it is in scope rather than optional.
  - **Decided: render-only.** Stored `emoji` nodes are NOT normalized — no data migration and
    no heal-on-load. Gap A and Gap B make existing content render correctly without a re-save,
    which satisfies AC-015 on its own and keeps this change out of Java, the DB, and
    rollback-relevant territory. Accepted consequence: old `emoji` nodes persist indefinitely,
    so the legacy-editor drop path and the `<img>` fallback stay live for that content.

## Acceptance & Verification *(mandatory)*

<!-- Measurable, so the fix is provably done. -->

**Editor — root cause**

- **AC-001**: The reproduction steps above no longer produce the actual behavior; they produce
  the expected behavior. Specifically, step 7 yields exactly one `text` node carrying one
  `link` mark whose text contains `©`.
- **AC-002**: Typing or pasting any character in the affected class into paragraph text stores
  a plain `text` node and creates **no** `emoji` node. Verified against a **representative
  sample** (decided — not exhaustive): all three reported symbols, ~20 further
  text-presentation characters, and a handful of pictographic and multi-codepoint cases
  including a ZWJ sequence and a flag.
- **AC-003**: AC-002 holds at the **start** and at the **end** of linked text, not only in the
  middle.
- **AC-004**: AC-002 holds when the character arrives by **paste** — plain-text paste, HTML
  paste, and `&copy;` HTML-entity paste.
- **AC-005**: Inserting an emoji from the toolbar picker inserts the literal character into the
  surrounding text node, and inside a link yields a **single** `<a>` in the editor DOM.
- **AC-006**: Typing a `:)`-style emoticon inserts the literal character into the surrounding
  text node and creates **no** `emoji` node. Inside linked text it leaves the link as a single
  `text` node with one `link` mark.
- **AC-007**: Behavior is identical whether or not `emoji` is in the field's Allowed Blocks.
- **AC-008**: The character renders as text; no `<img alt="… emoji">` is emitted for
  newly authored content.

**Renderers — Gap A (existing `emoji` nodes)**

- **AC-009**: Given stored JSON containing an `emoji` node, the VTL renderer outputs the
  character. It is no longer silently dropped.
- **AC-010**: The React, Vue, and both Angular SDK renderers render the character for the same
  input instead of falling through to their unknown-block component.
- **AC-011**: No renderer emits a block-level element (e.g. `<div>`) inside a `<p>` for an
  `emoji` node, in UVE or on the live site.

**Renderers — Gap B (adjacent-link coalescing)**

- **AC-012**: Given stored JSON with adjacent text nodes sharing an identical `link` mark, VTL
  and all four JS SDK renderers each emit exactly **one** `<a>`.
- **AC-013**: Given adjacent text nodes whose `link` marks differ in any of `href`, `target`,
  `rel`, `title`, or `aria-label`, each renderer emits **two** `<a>` elements.
- **AC-014**: A `bold` (or other) mark inside a coalesced link still nests correctly within the
  single `<a>`.
- **AC-015**: Content already saved with the split renders as a single link **without requiring
  a re-save**.

**Accessibility verification**

- **AC-016**: Tabbing through the rendered link produces exactly **one** focus stop.
- **AC-017**: The NVDA Elements List / VoiceOver rotor shows **one** entry carrying the
  complete accessible name, including the symbol.

**Verification method**:

- **Unit (Jest/Spectator)** in `core-web/libs/new-block-editor` — drives a real TipTap editor
  and asserts document shape for AC-001 through AC-008. Parameterize over a character sample so
  the class, not three literals, is covered. Use real `NgZone` in service tests; a mocked one
  breaks the change-detection scheduler.
  `pnpm nx test new-block-editor`
- **Unit (Jest)** for the React, Vue, and Angular SDK renderers — AC-010 through AC-014. The
  Angular SDK needs both of its renderers covered.
  `pnpm nx test sdk-react` / `pnpm nx test sdk-vue` / `pnpm nx test sdk-angular`
- **VTL renderer** — AC-009, AC-011 through AC-014. Exercised via a Postman collection
  rendering a fixture contentlet whose Story Block field holds both an `emoji` node and
  adjacent same-link text nodes.
  `./mvnw verify -pl :dotcms-postman -Dpostman.test.skip=false -Dpostman.collections=<collection>`
- **Regression fixture** — a stored-JSON fixture reproducing the exact three-node payload from
  the Actual Behavior section, shared across the renderer test suites so all five renderer
  implementations assert against identical input. This fixture is what proves **AC-015**: it is only ever
  rendered, never re-saved.
- **Manual accessibility pass** — AC-016 and AC-017, with NVDA and with VoiceOver. Not
  automatable; must be recorded in the post-merge QA plan.

Per Constitution Principle V, these tests are written, developer-approved, and confirmed
failing (Red) before any implementation.

## Assumptions

- **This spec's approach supersedes an acceptance criterion in issue #37340.** That issue
  requires "Genuine pictographic emoji inserted via the emoji picker still work and still
  produce `emoji` nodes — no regression." The approach chosen here deliberately stops producing
  `emoji` nodes for **all** newly authored content, pictographic included. The user's stated
  direction is explicit and takes precedence. **Action: amend the AC on #37340 before this spec
  is approved**, so the issue and the spec do not disagree in the record.
- Emoji rendered as a literal character in a text node is acceptable product behavior. This is
  already what the picker produces (`insertContent(emoji.native)`), and what the legacy Block
  Editor has always produced.
- Losing shortcode round-tripping (`:rocket:`) for new content is acceptable. The `:`
  suggestion trigger is already inert by design in this editor.
- The customer on helpdesk ticket 39197 authored the affected content in the **new** Block
  Editor. Only the new editor registers the emoji extension, so only it can create this split.
  **This remains unconfirmed** and is the triage precondition recorded on #37340 — if they were
  on the legacy editor, this diagnosis does not explain their data.
- All measurements in this spec were taken against `@tiptap/extension-emoji@3.22.2` and
  `emoji-regex@10.6.0`. The counts are version-specific; the defect class is not.

## Resolved Decisions

All three open questions were decided by the developer on 2026-09-03. None remain open; the
spec carries no `[NEEDS CLARIFICATION]` markers.

| # | Question | Decision | Rationale / consequence |
| --- | --- | --- | --- |
| 1 | Existing stored `emoji` nodes — render-only, heal-on-edit, or migrate? | **Render-only** | Gap A + Gap B satisfy AC-015 without a re-save. No Java, no DB, not rollback-relevant. Accepted trade-off: old nodes persist, so the legacy-editor drop path and the `<img>` fallback stay live for that content. |
| 2 | `enableEmoticons` (`:)`) — keep as text, or drop? | **Insert literal character** | Preserves a shortcut authors already use, while removing the node creation that strips marks. `:)` now yields `🙂` inside the text node. Covered by AC-006. |
| 3 | Does AC-002's character sample need to be exhaustive? | **Representative sample** | All 3 reported symbols, ~20 further text-presentation characters, plus pictographic and multi-codepoint cases (ZWJ sequence, flag). Fast, readable, and stable across extension upgrades. |

**Still to confirm outside this spec** — not a clarification, an action:

- Amend the acceptance criterion on issue [#37340](https://github.com/dotCMS/core/issues/37340)
  that requires pictographic emoji to keep producing `emoji` nodes. This spec deliberately stops
  producing them for all newly authored content. See Assumptions.
- Confirm from helpdesk ticket 39197 that the customer authored the affected content in the
  **new** Block Editor. This is the triage precondition already recorded on #37340.
