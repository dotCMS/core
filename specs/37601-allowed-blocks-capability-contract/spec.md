# Issue Resolution Specification: Allowed Blocks capability-key contract — enforce it, and fix the two keys that violate it

**Feature Branch**: `37601-allowed-blocks-capability-contract`

**Created**: 2026-09-17

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#37601](https://github.com/dotCMS/core/issues/37601)

**Input**: Slice 1 of #37601 — the capability-key contract (invariants I1 and I2) plus the two
defects that violate it today (B: `youtube`; C: `aiContent` / `aiImage`). Defect A (legacy editor
node deletion) and defects D/D2/E (delivery layer) are deliberately **not** in this spec; see
*Fix Scope & Non-Goals*.

## Problem Statement *(mandatory)*

A Block Editor field can be restricted with **Allowed Blocks** in its Settings tab. When an
administrator restricts a field to *anything at all*, the editor silently removes capabilities the
administrator never chose to remove, and that they cannot restore short of clearing the entire
restriction list.

This happens because parts of the editor ask *"is this capability allowed?"* using an identifier
that the Settings tab **cannot produce**. The answer is therefore always "no" on any restricted
field. Two such identifiers are live today:

- **`youtube`** — restricting any block removes the YouTube tab from *Add asset by URL*.
- **`aiContent` / `aiImage`** — the slash menu asks for these, but Settings emits
  `aiContentPrompt` / `aiImagePrompt`. The result is perverse: **ticking "AI Content" in Settings
  is exactly what hides AI Content**, because ticking anything restricts the field and the
  mismatched key then evaluates false. Not ticking it hides it too. No configuration shows it.

The rule these violate was written down by PR #37539 in
`specs/36351-block-editor-link-setting/contracts/allowed-blocks-capability-keys.md`:

> A capability may be gated on Allowed Blocks only if the Settings tab can produce its identifier.
> Gating such a capability is always a defect, never a configuration.

**It is a markdown file and nothing enforces it.** The same class of defect has been fixed
ad-hoc four times in ten weeks — `link` (#36351), `underline` (#36572), `highlight` (#37145),
`emoji` (#37340) — and six times over sixteen months counting earlier rounds (#35032, #34110,
#29772, #27101, #24370, #23764, #22164). Each was triaged as an isolated incident.

**Severity / Impact**: High for recurrence, Medium per instance.

Per instance: administrators lose a capability on restricted fields with no way to restore it and
no error. Affects every field with Allowed Blocks configured, on every build where
`FEATURE_FLAG_NEW_BLOCK_EDITOR` is available (since #35257, 2026-05-08).

The recurrence is the real cost. Support has prescribed the same workaround — *"uncheck everything
under Allowed Blocks and save"* — to four customers
([FD #37852](https://dotcms.freshdesk.com/a/tickets/37852),
[FD #38997](https://dotcms.freshdesk.com/a/tickets/38997),
[FD #38993](https://dotcms.freshdesk.com/a/tickets/38993),
[FD #38349](https://dotcms.freshdesk.com/a/tickets/38349)), and in a fifth
([FD #38349](https://dotcms.freshdesk.com/a/tickets/38349)) closed the ticket as **user error**
while the real issue (#36351) sat open, unfixed, for two more months.

## Reproduction *(mandatory)*

**Environment**: `main` @ `667fc831ee` (2026-09-17). `FEATURE_FLAG_NEW_BLOCK_EDITOR=true`
(the default). Any site, any content type with a Block Editor (Story Block) field. Scenario C
additionally requires the dotAI plugin installed so the slash-menu entries would otherwise render.

### Scenario B — restricting any block kills the YouTube tab

1. Content Model → a content type with a Block Editor field → open that field's **Settings** tab.
2. Under **Allowed Blocks**, select a single unrelated block — e.g. `Bulleted List`. Save.
3. Create or edit a contentlet of that type.
4. In the editor toolbar, open the **Add asset by URL** popover.

**Expected Behavior**: the **YouTube** option is present and selectable. `youtube` is not something
an administrator can restrict, so it must never be gated.

**Actual Behavior**: the YouTube option is disabled/absent. `image` and `video` in the same popover
behave correctly — they *are* producible keys and remain correctly gated.

### Scenario C — ticking "AI Content" is what hides AI Content

1. Same field's **Settings** tab → **Allowed Blocks** → select **AI Content** and **AI Image**
   (plus at least one other block, so the field is restricted).  Save.
2. Create or edit a contentlet of that type.
3. Type `/` to open the slash menu.

**Expected Behavior**: **AI Content** and **AI Image** appear — they were explicitly allowed.

**Actual Behavior**: both are absent. Deselecting them does not help either; on a restricted field
there is no configuration that makes them appear.

**Reproducibility**: Always. Both are deterministic for any field whose Allowed Blocks list is
non-empty. Neither depends on stored content, browser, or data state.

## Scope of Investigation *(mandatory)*

- **Affected area**: Content editing — the Block Editor (Story Block) field in the dotCMS admin
  UI. Specifically the new TipTap-v3 editor (`core-web/libs/new-block-editor`) and the Settings-tab
  option list it is configured from (`core-web/libs/block-editor`).
- **Suspected surface**: **Neither `com.dotcms.*` nor `com.dotmarketing.*` — this is frontend
  only.** All affected code is TypeScript/Angular under `core-web/**`. No Java is involved, no
  REST contract, no persisted data, no database, no ElasticSearch. Legacy Impact for the plan
  phase is expected to be nil; the legacy Block Editor (`core-web/libs/block-editor`) is read only
  as the source of the Settings option list and is not modified.
- **Related known decisions**:
  - `specs/36351-block-editor-link-setting/contracts/allowed-blocks-capability-keys.md` — the
    written contract this spec makes enforceable. It already records `youtube` and
    `aiContent`/`aiImage` as *"known violation, out of scope"*; this work closes them.
  - PR #37442 (#37340, emoji) applied the same reasoning a fourth time, in its own words:
    *"A gate that could only misfire … Nobody configured that."*
  - No ADR is known to govern Block Editor capability gating. The plan phase consults
    `dotCMS/platform-adrs` formally.

## Root-Cause Hypothesis

There is **no single source of truth** for Allowed Blocks capability identifiers, and no test
relates the producer to its consumers.

- The **producer** is `getEditorBlockOptions()` in
  `core-web/libs/block-editor/src/lib/shared/utils/suggestion.utils.ts` — the only thing that
  decides what the Settings tab can write into a field's `allowedBlocks`.
- The **consumers** are scattered: `editor-extensions.ts`, the toolbar, the slash-menu catalog and
  the asset-by-url popover each call `store.isAllowed(...)` / `has(...)` with a string literal.
- Nothing checks that a consumed literal exists in the producer's list, or that a producible option
  resolves to anything at all.

**Verified inventory (main @ `667fc831ee`).** Seventeen distinct keys are consulted today:

`aiContent aiImage audio blockquote bulletList codeBlock dotContent emoji gridBlock horizontalRule
image link orderedList paragraph table video youtube`

Of these, exactly **three are not producible** by `getEditorBlockOptions()` and are gated anyway —
`youtube`, `aiContent`, `aiImage`. They are the expected red set for I1. `paragraph` is also
non-producible (the option list filters it out at `suggestion.utils.ts:119`, per #29772) but is
already **explicitly exempted** by a hardcoded check at
`core-web/libs/new-block-editor/src/lib/editor/components/slash-menu/slash-menu.service.ts:112`
(`item.blockName === 'paragraph' ||`); the invariant must mirror that exemption rather than
report it. `link` and `emoji` no longer appear as gates — #37539 and #37442 removed them —
confirming the intended resolution pattern.

A gate on a non-producible key is unconditionally false on any restricted field, which is why
these read as "the feature disappeared when I restricted the field". The inverse — a producible
option with no consumer — is the `aiContentPrompt` / `aiImagePrompt` half: Settings offers a
checkbox that maps to nothing.

The fix for each individual key is 1–2 lines. **The fix for the class is the test.** Without an
enforced relation, a fifth key will be added under the same assumption; the history says so.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- **I1 — no gate on a non-producible key.** An automated check that fails if any capability key
  consulted by the new Block Editor is absent from `getEditorBlockOptions()`. `paragraph` is
  exempt (special-cased as always permitted). Must fail on `main` today for `youtube`,
  `aiContent` and `aiImage`.
- **I2 — no producible key without a destination.** The inverse check: every option
  `getEditorBlockOptions()` can emit must resolve to something real — a registered extension, a
  gate, or a documented always-on capability. Must fail on `main` today for `aiContentPrompt` /
  `aiImagePrompt`.
- **Defect B** — remove the `youtube` gate from the toolbar and the asset-by-url popover.
- **Defect C** — correct the slash-menu catalog to consult `aiContentPrompt` / `aiImagePrompt`.
- Update the capability-keys contract document: move the `youtube` and `aiContent`/`aiImage` rows
  from *"known violation, out of scope"* to resolved, pointing at the tests that now enforce them.
- Record the enforced contract in `core-web/libs/new-block-editor/CLAUDE.md`.

**Explicitly out of scope / non-goals**:

- **Defect A of #37601** — the legacy editor deleting `dotAudio` / `aiContent` nodes on load. It
  changes which stored content loads and deliberately reverses long-standing behaviour; it needs
  its own spec, review and QA. Highest-risk item in the family, kept separate on purpose.
- **Defects D, D2 and E of #37601** — the delivery layer (five renderers, two Velocity templates,
  the SDK `isValidBlocks` validator). Different blast radius (published output rather than the
  authoring UI) and different reviewers. Own spec.
- **Adding `youtube`, `aiContent` or `aiImage` to the Settings tab** as newly restrictable options.
  That is a product decision, not a defect fix. This work makes non-producible keys ungated,
  following the precedent set for `link` (#36351/#37539) and `emoji` (#37340/#37442).
- Any change to the legacy Block Editor's behaviour. It is read as the source of the option list
  and otherwise untouched.
- Any change to the dotAI plugin's own availability check (`store.aiInstalled()`), which is a
  separate and correct gate and must keep winning.
- The `table-handle-popover` `mergeCells` TypeError noted in #37601 — unrelated, console-only.

## Regression Risk *(mandatory)*

- **Blast radius**: Confined to the new Block Editor's authoring UI. Removing the `youtube` gate
  can only make a capability *more* available; it cannot hide anything. Correcting the AI keys can
  only make the slash-menu entries appear where an administrator explicitly allowed them. The
  risk worth testing is the inverse: that the **other** gates in the same components still work —
  `image` and `video` share the asset-by-url popover with `youtube`, and the slash-menu catalog
  gates many producible blocks besides the AI ones.
- **Backward compatibility**: No stored JSON, REST contract, GraphQL fetcher, DB or ES mapping is
  touched. No persisted `allowedBlocks` value changes meaning: a field that lists `aiContentPrompt`
  today keeps that exact value and simply starts working. Nothing to migrate, nothing to roll
  forward. This is a clean backport candidate.
- **Data considerations**: None. No existing data is bad; existing configurations begin to be
  honoured rather than repaired.
- **Rollback**: Fully rollback-safe. Reverting restores the previous (defective) gating with no
  data residue.
- **One accepted behaviour change, called out for reviewers**: on a restricted field the YouTube
  tab becomes available where it previously was not. That is the fix, not a side effect, and it
  matches how `link` and `emoji` were resolved.

## Acceptance & Verification *(mandatory)*

- **AC-001**: Scenario B no longer reproduces. On a field restricted to any subset of blocks, the
  **Add asset by URL** popover offers the YouTube option.
- **AC-002**: Scenario C no longer reproduces. With **AI Content** and **AI Image** ticked in
  Allowed Blocks, both appear in the slash menu of a restricted field.
- **AC-003**: With AI Content / AI Image **not** ticked on an otherwise restricted field, both stay
  hidden — the gate still functions, it is merely reading the right key.
- **AC-004**: On an unrestricted field (empty Allowed Blocks) nothing changes: every capability
  remains available, exactly as today.
- **AC-005**: Regression — the gates that *are* correct keep working. `image`, `video`, `table` and
  code blocks remain restricted when excluded from a non-empty Allowed Blocks list.
- **AC-006**: Regression — the dotAI availability check still wins. With the AI plugin absent, AI
  Content / AI Image stay hidden regardless of Allowed Blocks.
- **AC-007 (I1)**: An automated test relates every capability key consulted by the new Block
  Editor to `getEditorBlockOptions()` and fails when one is not producible. Demonstrated **red on
  `main`** for `youtube`, `aiContent` and `aiImage` before any production code changes, and green
  after.
- **AC-008 (I2)**: An automated test relates every option `getEditorBlockOptions()` can emit to a
  real destination and fails when one resolves to nothing. Demonstrated **red on `main`** for
  `aiContentPrompt` / `aiImagePrompt` before any production code changes, and green after.
- **AC-009**: Each invariant test carries a comment naming the issues and tickets it would have
  caught (#37175, #36351, #37340, [FD #38349](https://dotcms.freshdesk.com/a/tickets/38349)), so a
  future red build explains itself.
- **AC-010**: `allowed-blocks-capability-keys.md` no longer lists `youtube` or
  `aiContent`/`aiImage` as known violations and points at the enforcing tests.

**Verification method**:

- **Vitest**, run from `core-web`: `pnpm exec nx test new-block-editor`. New specs alongside
  `libs/new-block-editor/src/lib/editor/extensions/` and the toolbar / slash-menu / popover
  components.
  **Corrected during planning (research.md R1, R3):** an earlier draft named Jest and
  `pnpm nx test block-editor`. Neither is right. `libs/new-block-editor` is a **vitest** project
  (`vite.config.mts`, no `jest.config.ts`), and `libs/block-editor` has **no test target at all**
  — no test config, `"targets": {}`, tag `skip:test`, and CI runs
  `nx affected -t test --exclude=tag:skip:test`. Both invariant specs therefore live in
  `new-block-editor` and import the producer via its public export
  (`@dotcms/block-editor` → `public-api.ts`). #37601's claim that `nx test block-editor` has
  "10 pre-existing failures" is stale for the same reason.
- **The Red gate is explicit**: AC-007 and AC-008 must be observed failing on unmodified `main`
  and the failure output recorded in the PR, before Defect B or C is touched. Per Constitution
  Principle V this is a hard gate, not a preference.
- **Manual pass in the running app for both scenarios.** Every defect in this family was invisible
  to unit tests and obvious in the browser within thirty seconds; #37175 shipped with an unmet AC
  that only QA in the app caught, which is what forced #37313.
- No integration, Postman or Karate run is applicable — the change is `core-web/**` only, which
  ADR-0013 already excludes from those suites in the merge queue.

## Assumptions

- The user input for this invocation was empty; scope was taken from #37601 as **slice 1** of the
  three-way split proposed in that discussion (contract + B + C now; legacy editor and delivery
  layer separately). If a different slice was intended, this spec should be re-scoped before
  planning.
- `paragraph` remains a special case: always permitted and exempt from I1. This matches existing
  behaviour and #29772, which removed it from the Settings option list deliberately.
- The precedent set by #36351/#37539 (`link`) and #37340/#37442 (`emoji`) — *ungate the capability
  rather than add a Settings toggle* — is the intended resolution for `youtube` too. If Product
  wants YouTube to become restrictable, that is a feature and this spec's AC-001 changes.
- The dotAI plugin can be installed in the verification environment; otherwise AC-002 is verified
  by unit test only and AC-006 becomes the manual check.
