# Code Review Decisions — #37340 Emoji Text Node

**Date**: 2026-09-08
**Branch**: `issue-37340-block-editor-emoji-node-impl`
**Range reviewed**: `4add4e086c` (merge-base with `origin/main`) → `2a5d4c3efe`
**Baseline at review time**: 19/19 suites, 277/277 tests passing, lint clean, Node v22.22.3

This records what the pre-PR-2 review found, what was accepted, what was rejected, and who
does what. It is the decision record for PR 2 scope — not a task list. Implementation tasks
live in `tasks.md` (gitignored, local).

---

## Process

- Review run by an independent session over the committed range; findings then validated with
  the session that owns the branch and holds the design intent.
- Three findings were verified directly against the source before being reported; four were
  reported as unverified and confirmed by the branch owner.
- **Code is written by the branch-owner session** (single writer — same files, concurrent edits
  would conflict). The reviewing session re-verifies against the push.
- `/speckit-converge` (T048) runs **after** these fixes, not before. It is the developer's gate
  to fire.

---

## Accepted — in scope for PR 2

### 1. Heal has no error boundary; malformed stored JSON blanks the field — Critical

- **Where**: `utils/emoji-heal.utils.ts:68`, `utils/emoji-heal.utils.ts:278`,
  `editor.component.ts:508`
- **Defect**: `attrsEqual(a = {}, b = {})` — default params fire only on `undefined`, so a stored
  mark with `"attrs": null` reaches `Object.keys(null)` and throws. Separately,
  `(previous.marks ?? []).some(...)` lets a stored `"marks": {}` through the `??` guard and dies
  on `.some is not a function`.
- **Impact**: the throw propagates out of `writeValue` / the `value` effect, `setContent` never
  runs, and the field renders empty over intact JSON — the #37145 mechanism this spec cites twice.
- **In threat model**: yes. `data-model.md` records that hand-crafted JSON is reachable through
  the Contentlet REST API because no server-side Story Block validation exists.
- **Not deliberate** — an oversight. There was no "fail loud" intent.
- **Fix**: `try/catch` inside `healEmojiNodes`, returning the input **untouched** on any throw, so
  an unparseable document degrades to exactly the pre-fix behavior. Deliberately _not_ a catch in
  `loadContent` — the heal fails closed at its own boundary rather than making every caller
  defensive. Precedent: `content-match.utils.ts:72`.
- **Also**: harden `attrsEqual` / `marksEqual` against non-array `marks` and non-object `attrs`.

### 2. `healEmojiHtml` fast-path guard is narrower than its own selector — Important

- **Where**: `utils/emoji-heal.utils.ts:140`
- **Defect**: `if (!html.includes('data-type="emoji"')) return html;` is a literal double-quote
  string match. The `querySelectorAll('span[data-type="emoji"]')` on line 148 runs post-parse and
  is quote- and case-agnostic, so `data-type='emoji'`, uppercase, or whitespace-padded markup
  skips the heal entirely.
- **Impact**: the `fallbackImage` span's inner `<img src="cdn.jsdelivr.net/…">` stays exposed and
  `DotImage` claims it — the T051 / R11 defect reopening.
- **Fix**: replace the string match with `/data-type\s*=\s*['"]?emoji/i`.
- **Rejected alternative**: deleting the guard outright (line 148's `!spans.length` already
  early-returns). Rejected because it puts a `DOMParser` on every field load and every paste. The
  regex keeps the fast path and closes the correctness gap.

### 3. Heal runs before the unknown-node pass, rewriting inert custom-block payloads — Important

- **Where**: `editor.component.ts:500-506`
- **Defect**: the heal runs first, so it rewrites `emoji` nodes nested inside a customer's unknown
  node _before_ `preserveUnknownNodesInDocument` stashes that node into `attrs.originalNode`.
  `restoreUnknownBlockNodes` then writes the altered payload back on save.
- **Why it matters**: `dotcms-models/src/lib/unknown-block.util.ts:338-343` documents that payload
  as "inert data… left byte-for-byte as stored", and the mark pass skips `dotUnsupportedBlock` to
  honour it. The heal did not. Silently rewriting a customer's custom-block content is the class
  of harm this whole spec argues against.
- **The comment was wrong in the opposite direction from how it was written**: it claimed
  heal-after "would walk placeholder payloads". In fact heal-after can never reach a payload,
  because `healNode` only recurses into `node.content` and the payload lives in `attrs`.
- **Fix**: reorder to heal **after** `preserveUnknownNodesInDocument`, which makes the payload
  structurally unreachable — and makes the comment true. One line.
- **Rejected alternative**: teaching `healNode` about `knownNodeNames`. Unnecessary once the order
  is right, and it would couple the util to the schema.

### 4. Two Suggestion plugins share one single-slot `SlashMenuService`, with no arbitration — Important

- **Where**: `extensions/dot-emoji.extension.ts:331-392` vs
  `extensions/slash-command.extension.ts:33-37`
- **Defect**: slash uses `startOfLine: true, allowSpaces: true`, so a `/` session survives a
  space. The emoji plugin sets no `allowedPrefixes`, taking TipTap's `[" "]` default, so `/ :sm`
  satisfies both — two live sessions driving one menu, and each `onExit` checks only its own
  plugin key. One session can close the shared menu while the other still captures keys, so a row
  click can invoke the wrong `command` / `deleteRange`.
- **Fix**: emoji `onStart` / `onUpdate` bail when a slash session already holds the menu.
- **Note**: this was identified while drafting the extension and then not done.

### 5. Healed emit drops the document stats the normal path stamps — Important

- **Where**: `editor.component.ts:485`, `editor.component.ts:803`
- **Defect**: `withDocStats` bails when `charCount() <= 0`. `syncCharacterStatsFromEditor` runs
  only in `onCreate` and `onUpdate`, and `loadContent` uses `emitUpdate: false`, so at the
  deferred emit the count is still the empty-doc value.
- **Impact**: the heal's emitted value carries no `attrs.charCount / wordCount / readingTime`,
  unlike every author-edit emit. An author who opens an affected field and hits Publish persists
  a document that lost those attrs.
- **Fix**: sync stats before `emitValue` on the heal path. Assert `attrs` in the spec.

### 6. HTML load path heals but never emits — Important

- **Where**: `editor.component.ts:511-520`
- **Defect**: the path returns early after `setContent(healEmojiHtml(...))`, skipping the
  `healed !== parsed` emit below. The spec's second guarantee — "the healed shape is emitted to
  the host as soon as the heal rewrites something" — therefore holds on the JSON path only.
- **Not deliberate**. dotCMS not storing HTML is why the asymmetry is low-impact, not why it is
  right.
- **Fix**: emit when `healEmojiHtml` returned a different string.

### 7. Bare `:` opens the emoji menu; the test that should cover it is misnamed — Important

- **Where**: `extensions/dot-emoji.extension.ts:262-264`, `slash-menu.service.ts:190-202`,
  `extensions/dot-emoji.extension.spec.ts:414`
- **Defect**: `filterEmojis` returns five arbitrary rows for an empty query and
  `SlashMenuService.open` sets `isOpen` unconditionally, so a bare colon pops a menu of
  unrelated emoji.
- **Repro is narrower than first reported** — see _Corrections_ below. The trigger must follow a
  space, line start, or `\0`, so the case is `hi :`, not `Note:` or `10:30`.
- **Test defect**: `dot-emoji.extension.spec.ts:414` is named _"does not open a session for a bare
  `:` with no query"_ but its body types `:smi` then a space and asserts the session deactivates.
  The bare-`:` claim is never asserted.
- **Fix**: `onStart` / `onUpdate` close rather than open when `props.query` is empty; add a test
  that actually types a bare colon; rename the existing test to match what it asserts.

---

## Accepted — coverage work in scope for PR 2

Branch coverage at review time: **69.56%** on `dot-emoji.extension.ts`, **75.00%** on
`emoji-heal.utils.ts`.

These are must-have because they bound the only inference this change makes:

- **Two-mark sandwich fixture** — no existing fixture has a second mark alongside `link`, so the
  documented full-mark-set behavior is untested.
- **AC-016: emoji carrying its own marks as the run boundary.**
- **AC-016: emoji whose `name` does not resolve as the run boundary.**
- **AC-016: links differing in `rel` and in `title`** — T030 names five link attrs; only `href`,
  `target` and `ariaLabel` are covered.

---

## Accepted — not in scope, deferred

### `handleDoubleClickOn` at 0% coverage

- **Where**: `extensions/dot-emoji.extension.ts:107-116`
- **Position**: deferred. It is a one-line upstream gesture, and research R4's argument was about
  not _deleting_ the plugin, not about covering it.
- **Counter-argument on the record**: a deliberate decision with no test is one line from being
  removed by the next reader. Revisit if that plugin is ever touched again.

### ADR proposal (T049)

- Editor-side normalization of deprecated Story Block nodes, to be proposed in
  `dotCMS/platform-adrs`. Explicitly not a blocker for PR 2.

### Minor items noted, not actioned

Carried for a future pass; none blocks PR 2.

- `LINK_SANDWICH = 'link'` names a rule but holds a mark type — rename to `LINK_MARK_TYPE`.
- `emoji-heal.utils.ts:330` — `return (healed === content ? content : healed) as T;` is a no-op
  ternary.
- `dot-emoji.extension.ts:327` — `DotEmojiBase.config.addProseMirrorPlugins?.call(this)` bypasses
  the extend chain; the TipTap idiom `this.parent?.() ?? []` survives an intermediate `.extend()`.
- Two sources of truth for the emoji table: `editor.component.ts:27` imports the module-level
  `emojis` while the extension reads `this.options.emojis`.
- Type-safety debt: `emojis as EmojiItem[]` ×4, `(node.attrs?.['name'] ?? '') as string` ×2,
  `item.emoji as string`.
- `this.options.suggestion.allow` (`dot-emoji.extension.ts:338`) lacks optional chaining.
- The merge is array-scoped, not creation-scoped: once `changed` is true, `mergeAdjacentText` also
  joins pre-existing identical-mark adjacencies.
- Toolbar template comment drift at `toolbar.component.html:366-367` and `:468-473`.
- `toolbar.component.spec.ts:89-96` selects by tag + CSS class, against
  `TESTING_REVIEW_RULES.md:27`; `:38-41` uses `useValue: {}` instead of `mockProvider`.
- Duplicated fixtures: `editor.component.spec.ts:386-401` re-declares the reported payload inline
  rather than importing `REPORTED_PAYLOAD`; `linkedParagraph` is duplicated in
  `dot-emoji.extension.spec.ts:60-74` and `:344-352`.
- Clean Code refactors proposed and not taken now: split the `:` suggestion concern out of the
  396-line node extension; reduce `healInline` from four jobs to two; collapse `healContent`'s
  three passes into one with a cheap non-allocating pre-scan; extract the duplicated
  `attrs.name` read; de-duplicate the three test-side node walkers.

---

## Rejected

### Link-sandwich inherits the full mark set — not a defect

- **Claim**: `emoji-heal.utils.ts:282` passes `previous.marks`, so a healed node between two
  `text(link + bold)` runs inherits **bold** as well — wider than AC-015, which says it carries
  "that same `link` mark".
- **Rejected, both sessions agreeing**: the gate requires full mark sets to **match**, which fires
  strictly _less_ often than an `href`-only comparison; the inheritance then carries the set that
  already matched. Narrower gate, wider payload, both deliberate — and the comment at
  `emoji-heal.utils.ts:59-64` already says so.
- **Residual action**: none beyond the two-mark fixture already in scope, which makes the behavior
  explicit rather than comment-asserted.

---

## Corrections to the review itself

Recorded so the next reader does not inherit the errors.

- **`Note:` and `10:30` do not open the emoji menu.** The reviewing session offered these as
  bare-`:` repros. `@tiptap/suggestion` defaults `allowedPrefixes = [" "]`
  (`node_modules/@tiptap/suggestion/dist/index.js:70`) and tests the preceding character against
  `^[ \0]?$` (`:26`). In `Note:` the colon follows `e`; in `10:30` it follows `0`. Neither
  matches. The real repro is a colon after a space — `hi :`. Finding 7 stands; its scope is
  narrower.
- **`plan.md`, `tasks.md`, `research.md` and `quickstart.md` are gitignored by design, not
  accidentally uncommitted.** `.gitignore:229-232` lists them explicitly; `.specify/CUSTOMIZATIONS.md`
  explains why Spec-Kit working artifacts stay local. `spec.md`, `data-model.md`, `qa-plan.md` and
  `contracts/` stay tracked. There is nothing to land, and the review's suggestion to commit them
  was wrong.

---

## Process note, no code change

The plan's Implementation Step 1 required a tests-only commit confirmed Red. In practice the specs
landed inside the `fix(...)` commits (`a6b0b408eb`, `5832b14b02`), T013's Red evidence exists only
as prose in `tasks.md`, and `emoji.fixtures.ts` was created two commits _after_ the specs meant to
use it — which is how the duplicate fixtures noted above arose. Worth tightening on the next
feature; not a defect in this one.

Three misleading test names have now been caught in this feature; two of the earlier ones were
hiding real ranking bugs. Test names asserting more than their bodies do is a recurring pattern
here and deserves attention in review, not just at authoring time.

---

## Independent re-verification of the fixes

Fixes landed in `f1db0899a4`. Re-verified by the reviewing session against that commit — behaviorally,
with fixtures written independently of the branch's own tests, not by reading the diff.

**Gate at `f1db0899a4`**: 19/19 suites, **284/284 tests passing**, lint clean, Prettier clean.

| #   | Fix                                     | How it was verified                                                                                                                                                                                                                                                                               | Result               |
| --- | --------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------- |
| 1   | Error boundary                          | 6 malformed shapes built independently: `"attrs": null`, `"marks": {}`, `marks` as a string, `attrs` as an array, `content` as a non-array, `null` nested in `content`. Asserted no throw **and** that the return is never empty/undefined.                                                       | Pass (13 assertions) |
| 2   | HTML guard regex                        | Double-quoted, single-quoted, unquoted, whitespace-padded, and uppercase-attribute-**name** markup, each asserting the `jsdelivr` `<img>` is gone.                                                                                                                                                | Pass (6 assertions)  |
| 3   | Heal ordering                           | Round-tripped an `emoji` node nested in a custom block through `preserveUnknownNodesInDocument` → `healEmojiNodes` → `restoreUnknownBlockNodes`, asserting **exact `JSON.stringify` equality** with the original. Second case asserts a sibling top-level emoji still heals in the same document. | Pass (2 assertions)  |
| 4–7 | Suppression, stats, HTML emit, bare `:` | Verified in source at `dot-emoji.extension.ts:340-350`, `:363-378`, `editor.component.ts:579-587`, `:528-535`.                                                                                                                                                                                    | Confirmed            |

Coverage moved as follows:

| File                     | Branch, before | Branch, after |
| ------------------------ | -------------- | ------------- |
| `emoji-heal.utils.ts`    | 75.00%         | **80.24%**    |
| `dot-emoji.extension.ts` | 69.56%         | see note      |
| `editor.component.ts`    | 53.19%         | 55.20%        |

`emoji-heal.utils.ts` statements are 94.73%, functions 100%. Remaining uncovered lines are
`107-111`, `181`, `379` — the `asAttrs` object-comparison branch, the `healEmojiHtml` fallback, and
the `catch`. These are the defensive paths, reachable only through shapes the suite constructs
deliberately; leaving them uncovered is a reasonable call rather than a gap.

### One residual gap found during re-verification — not a regression

`healEmojiHtml`'s guard is now `/data-type\s*=\s*['"]?emoji/i`, but the selector on the next line
is `span[data-type="emoji"]`, and CSS attribute-**value** matching is case-sensitive. So
`data-type="EMOJI"` passes the guard, pays for the `DOMParser`, matches no span, and returns the
HTML untouched with the `<img src="cdn.jsdelivr.net/…">` still exposed. Verified by asserting the
defect shape directly.

- **Not a regression.** The old `includes('data-type="emoji"')` guard failed the same input earlier
  and more cheaply. Behavior for uppercase values is unchanged.
- **Low likelihood.** The extension writes lowercase; this needs hand-authored or transformed HTML.
- **But guard and selector still disagree** — now in the opposite direction. That disagreement is
  the exact shape of finding 2.
- **Fix if taken**: `span[data-type="emoji" i]` on the selector, or drop the `/i` from the guard so
  the two agree on being case-sensitive. Deferred by default; noted so the next reader sees it.

## Assessment

**Ready to merge?** With the seven accepted fixes and the four coverage additions.

**Reasoning**: the architecture is sound, the implementation tracks the approved spec closely, and
the suite drives a real TipTap editor rather than mocks. But findings 1, 2 and 3 are correctness
defects in the heal — the one component whose entire justification is that it must not damage
content it had no reason to touch — and all three are small, localized changes that should land
before this reaches customer content.

### Residual gap — CLOSED, not deferred

The reviewer flagged that `/data-type\s*=\s*['"]?emoji/i` is case-insensitive while
`span[data-type="emoji"]` is not — CSS matches attribute *values* case-sensitively, only *names*
case-insensitively. So `data-type="EMOJI"` passed the guard, paid for the `DOMParser`, matched zero
spans, and returned the HTML with the `<img>` still exposed.

Recorded as deferred-by-default; closed instead. The selector now carries the CSS `i` flag
(`span[data-type="emoji" i]`), which costs nothing and makes the regex's `/i` honest — guard and
selector agree about case, and the disagreement was itself the shape of finding 2.

Verified non-vacuously: with the flag removed the new uppercase-value spec fails, so it discriminates
rather than passing by construction.

Also taken from the review notes: `slash-menu.service.ts`'s `update()` docblock now states that it
does **not** open the menu and that a caller whose first event can arrive while the dropdown is shut
must call `open()`. The API shape gave no hint, and that trap cost this feature an afternoon.

Deferred as agreed: typing the test doubles against the service interface so the compiler catches an
omitted method. Three stubs diverged from the real interface during this work, each presenting as
absence rather than error.

Final gate: 19/19 suites, **288/288 tests**, lint clean, Prettier clean.
