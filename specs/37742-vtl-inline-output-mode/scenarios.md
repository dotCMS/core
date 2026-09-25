# Skill scenarios: inline output mode

**Feature**: [spec.md](spec.md) · **Contract**: [contracts/skill-output.md](contracts/skill-output.md)

These are the tests for the skill (research R-006). Each scenario is run by a **fresh sub-agent** with this preamble:

> You are a Claude Code session with the `dot-ui-vtl-migration` skill installed. Read `.claude/skills/dot-ui-vtl-migration/SKILL.md` and whatever it tells you to read under that directory, and nothing else in the repository. Then answer the user message below exactly as the skill instructs. If the skill says to write files, write them under `<OUT>` (a fresh scratch directory) and list them.

`V` = `dotCMS/src/main/webapp/WEB-INF/velocity/static`. "Pasted" means the file's content is put in the user message; the sub-agent is not told where it came from.

**Extracting the branches of an inline answer for grading**: save the file, then take line 2 up to the line before the top-level `#else`, and the line after it up to the line before the final `#end`. Use `sed -n` ranges plus `perl -pe 'chomp if eof'`, the same recipe the skill uses, then compare with `cmp`.

## A. Inline by default (US1)

| # | User message | Pass when |
|---|---|---|
| A1 | "Migrate this custom field:" + pasted `V/personas/keytag_custom_field_old.vtl` | First line is the inline mode line. Exactly one file. Header is exactly `#if( $structures.isNewEditModeEnabled() )`. No `#parse`. No question about a filename or server path. The extracted `#else` branch is `cmp`-identical to `keytag_custom_field_old.vtl`. The `#if` branch contains no `DotCustomFieldApi.get(`, `.set(`, `dojo.` or `dijit`. |
| A2 | "Migrate `V/personas/keytag_custom_field.vtl`" (a path, no keyword; the sub-agent may read that file and its `_old`) | Inline mode line. One file named `keytag_custom_field.vtl` in `<OUT>`. No `_new`/`_old` written. `#else` branch `cmp`-identical to `keytag_custom_field_old.vtl`. |
| A3 | "Migra este campo en un solo archivo:" + pasted `V/htmlpage_assets/cachettl_custom_field_old.vtl` | Inline mode line. No path question. `#else` branch `cmp`-identical. |

## B. Refusing an unsafe inline (US2)

Synthetic inputs, pasted as given.

**B-macro-legacy**:
```vtl
<script>dojo.require('dijit.form.TextBox');</script>
#macro(renderTag $t)
    <span class="dijitTag">$t</span>
#end
<div id="tags-${fieldId}">#renderTag($tag)</div>
```

**B-unbalanced**:
```vtl
#if( $maxChar )
    <input dojoType="dijit.form.TextBox" maxlength="$maxChar" />
    <script>dojo.ready(function () {});</script>
```

**B-jquery-end** (a stray `#end` hidden in a selector):
```vtl
<input dojoType="dijit.form.DateTextBox" id="end-date" />
<script>dojo.ready(function () { dojo.query('#end-date').style('width', '8em'); });</script>
```

| # | User message | Pass when |
|---|---|---|
| B1 | "Migrate this:" + B-macro-legacy | The migration naturally keeps `#macro(renderTag …)` in the new branch, so the same name is in both. **No file is emitted.** The answer names `renderTag`, says both definitions exist in both edit modes because the whole file is parsed, and offers three files. |
| B2 | B1's conversation + "inline anyway" | Inline emitted. The finding is restated in the mode line. `#else` branch identical to B-macro-legacy. |
| B3 | "Migrate this:" + B-unbalanced | No file. Names line 1 (`#if` never closed), says inline would break the new edit mode too, offers three files. |
| B4 | "Migrate this:" + B-jquery-end | No file. Explains that `#end-date` is read by Velocity as `#end`, at line 2. Offers three files. |
| B5 | "Convert to inline:" + router, `_new` and `_old` of `V/tag/tag_storage_field_creation` (the sub-agent may read them) | Inline emitted with a **warning** naming `$beginning`, `$isCopyingHost`, `$tagStorageFromURL`, `$tempURL` and saying only one branch runs. The file is written in `<OUT>`. Both extracted branches are `cmp`-identical to the repo files; `_old` has CRLF and no trailing newline. |

## C. Three files on request (US3)

| # | User message | Pass when |
|---|---|---|
| C1 | "Migrate `V/personas/keytag_custom_field.vtl` — three files" | Three-file mode line. `<OUT>/keytag_custom_field_old.vtl` `cmp`-identical to the repo `_old`. `<OUT>/keytag_custom_field.vtl` `cmp`-identical to the repo router. A `_new.vtl` is present. |
| C2 | "Migra esto en tres archivos:" + pasted keytag `_old`, no path | Asks for the filename and server path; emits no files. |
| C3–C6 | C1 with "separate files" / "use a router" / "with #parse" / "separados, con enrutador" | Three-file mode line and three files. |
| C7 | "Migrate this inline but also give me the router:" + pasted keytag `_old` | Asks which shape; emits nothing yet. |

## D. Conversion (US4)

| # | User message | Pass when |
|---|---|---|
| D1 | "Convert `V/personas/keytag_custom_field.vtl` to a single inline file" | Inline mode line, checks found nothing blocking. Branches `cmp`-identical to repo `_new` / `_old`. No migration rule applied (the `#if` branch equals `_new` exactly). |
| D2 | "Split this into router + _new + _old, server path `/static/personas/keytag_custom_field.vtl`:" + D1's output file | Three files in `<OUT>`. `_new` and `_old` are `cmp`-identical to the repo; the router is `cmp`-identical to the repo router. |
| D3 | "Split this inline file:" + D1's output, no path | Asks for the router's server path. |
| D4 | "Round-trip all ten migrated custom fields under `V/` (collapse to inline, then split back) into `<OUT>`" | For each of the 10: `_new` and `_old` `cmp`-identical (20/20). Routers: identical for the 8 canonical ones; `url-title` and `tag_storage_field_creation` differ only in whitespace/line endings, with the same two `#parse` targets. |
| D5 | "Convert to inline:" + a router whose `#else` branch parses `/static/x/other_old.vtl` while `_new` is `/static/personas/keytag_custom_field_new.vtl` | Reports the mismatch between the two paths; does not guess. |

## E. Live render (SC-001, by hand)

Put A1's output in a Custom Field on a local instance (`just dev-run`). Open a contentlet with the new edit mode **on**, then **off**. In both, the key/tag field loads, accepts input and saves.

## Results log

Record per run: date, skill commit, scenario id, PASS/FAIL, and one line on the failure. Red runs go first (against the unmodified skill), then Green.

| Date | Skill state | Scenario | Result | Note |
|---|---|---|---|---|
