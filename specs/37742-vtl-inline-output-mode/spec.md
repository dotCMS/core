# Feature Specification: Inline single-file output mode for the VTL migration skill

**Feature Branch**: `nicobytes/vtl-migration-skill-add-inline-single-file-outpu`

**Created**: 2026-09-25

**Status**: Draft

**Type**: New Feature (developer tooling — behavior change to an AI skill, no product code)

**Issue**: [#37742](https://github.com/dotCMS/core/issues/37742) · Parent epic: [#35757](https://github.com/dotCMS/core/issues/35757)

**Input**: User description: "https://github.com/dotCMS/core/issues/37742 — add an inline output mode to the `dot-ui-vtl-migration` skill so a custom-field migration can produce one self-contained file (migrated code in the `#if( $structures.isNewEditModeEnabled() )` branch, original legacy code in the `#else`, no `#parse`, no sibling files). Inline becomes the default; a keyword selects today's three-file router; the skill converts between the two shapes in both directions; and it checks for the cases where putting both branches in one file is not equivalent to the router."

---

## Context

The `dot-ui-vtl-migration` skill (`.claude/skills/dot-ui-vtl-migration/`) migrates Custom Field VTL templates from the legacy Dojo/Dijit API to `DotCustomFieldApi`. Epic #35757 requires every migration to keep the legacy code reachable behind an edit-mode switch, so an instance can go back to the old edit mode. Today the skill satisfies that in exactly one shape: three files — `<name>_old.vtl` (original, verbatim), `<name>_new.vtl` (migrated) and `<name>.vtl`, a router that `#parse`s one or the other by full server path. `SKILL.md` makes that mandatory (its "File Output Pattern" section and the "Three-file output" gate at the top of "Before Outputting") and tells the skill to ask for the server path when it only receives file contents.

That shape assumes the author can write files to the server. A customer migrating a Custom Field on a running instance usually cannot: the field's VTL lives in the content type editor, not on disk, so there is nowhere to put `_new.vtl` and `_old.vtl` and nothing for `#parse` to load. For them the migration must be a single block they can paste.

State verified on this worktree at `f5a4d4a92f` on 2026-09-25:

| Fact | Value |
|---|---|
| `SKILL.md` / `references/migration-guide.md` | 288 / 1419 lines; neither mentions an inline shape |
| Migrated custom fields in the repo (under `dotCMS/src/main/webapp/WEB-INF/velocity/static/`) | 10, all in the three-file shape |
| Router files | all 5 lines, same structure; 9 indent the `#parse` lines with a tab, `htmlpage_assets/url-title.vtl` with 4 spaces; `tag/tag_storage_field_creation.vtl` uses CRLF; none ends with a newline |
| Pairs that define a `#macro` in either branch | 0 |
| Pairs that `#set` the same variable in both branches | **1** — `tag/tag_storage_field_creation` sets `$beginning`, `$isCopyingHost`, `$tagStorageFromURL` and `$tempURL` in both `_old` and `_new` |
| Largest pair | `htmlpage_assets/template_custom_field` — 230 legacy + 843 migrated lines |

One correction to the issue text, confirmed above: the issue says the sampled pairs "define no `#macro` or `#set`". That holds for `#macro` across all ten, but not for `#set` — `tag_storage_field_creation` is a real fixture for the same-variable warning (FR-011).

### Why the two shapes are not equivalent

Velocity parses the **whole** template before rendering it, while `#parse` loads its target only when the render reaches it. So:

- **Router**: the branch not taken is never parsed. A syntax problem in the legacy file cannot break the new edit mode.
- **Inline**: both branches are parsed even though only one renders. A syntax error or an unbalanced directive in the legacy half breaks the field in **both** modes; a `#macro` in either half is registered when the file is parsed, so two definitions of the same macro collide regardless of which branch runs.

`#set` is different: it executes at render time, so the untaken branch's `#set` never runs. A variable set in both branches is therefore not a collision inside the field itself; it is worth reporting only because it signals shared state a reader might misjudge. This is why the spec treats duplicate `#macro` and a legacy branch that fails to parse as **blocking** for inline, and duplicate `#set` as a **warning** (FR-010, FR-011).

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Migrate a pasted custom field into one pasteable block (Priority: P1)

A developer or customer pastes the VTL of a Custom Field — often with no file path, because it came from the content type editor — and asks the skill to migrate it. They get back one block: the migrated code under the new-edit-mode branch and their original code, untouched, under the legacy branch. They paste it back into the field and both edit modes work.

**Why this priority**: this is the whole reason for the issue. Customers without server file access cannot use today's output at all.

**Independent Test**: give the skill the contents of `personas/keytag_custom_field_old.vtl` with no path and no mode keyword; confirm it asks nothing about paths, returns a single file, the `#else` branch is byte-identical to the input, and the field renders correctly with new edit mode on and off.

**Acceptance Scenarios**:

1. **Given** only the contents of a legacy custom field and no mode keyword, **When** the user asks to migrate it, **Then** the skill returns one file whose structure is the edit-mode `#if` with the migrated code, `#else` with the original code, and `#end` — containing no `#parse` — and does not ask for a filename or server path.
2. **Given** that output, **When** the legacy branch is compared with the input, **Then** it is byte-for-byte identical (no re-indentation, no whitespace or line-ending changes).
3. **Given** a file path is provided as well, **When** the skill migrates in inline mode, **Then** the single output keeps the original file's name and replaces it; no `_new` or `_old` file is produced.
4. **Given** any migration, **When** the skill presents its output, **Then** it states which shape it produced ("inline, single file" or "three files with router").

---

### User Story 2 - Refuse to inline when it would break the new edit mode (Priority: P1)

The developer asks for an inline migration of a field whose legacy code defines a macro the migrated code also defines, or whose legacy code does not parse on its own. Instead of silently producing a file that would break both edit modes, the skill explains the eager-parse consequence and offers the three-file shape.

**Why this priority**: inline is becoming the default, so the default must never be the shape that breaks a field. Without this, P1 ships a regression risk.

**Independent Test**: run the skill in inline mode on synthetic inputs — one with the same `#macro` in both branches, one with an unbalanced `#if` in the legacy code — and on `tag/tag_storage_field_creation`; confirm the first two are not emitted inline without the user's explicit choice and the third is emitted with a warning naming the four shared variables.

**Acceptance Scenarios**:

1. **Given** a legacy branch and a migrated branch that both define a `#macro` of the same name, **When** inline output is requested, **Then** the skill names the macro, explains that inline parses both branches so the definitions collide in both edit modes, and offers the three-file shape; it does not emit the inline file unless the user then explicitly chooses it.
2. **Given** legacy code that would not parse on its own (for example an unbalanced `#if`/`#foreach`/`#end`), **When** inline output is requested, **Then** the skill reports the problem and where it is, explains that inlining it would break the new edit mode too, and offers the three-file shape; it never emits that inline file silently.
3. **Given** both branches `#set` the same variable name, **When** inline output is produced, **Then** the output lists each shared variable name, notes that only one branch runs so this is not a collision inside the field, and mentions the three-file shape as an alternative.
4. **Given** none of the above applies, **When** the skill emits inline, **Then** it still states in one line that both branches are parsed together and that the checks found nothing blocking.

---

### User Story 3 - Keep today's three-file output on request (Priority: P2)

A developer migrating a field that ships in this repo (where files live on disk under `velocity/static/`) asks for "three files", "separate files", "router", "#parse", or the Spanish equivalents ("tres archivos", "separados", "enrutador"/"router", "#parse"). They get exactly today's output.

**Why this priority**: the repo's own migrations use this shape and must keep doing so; but it is already the current behavior, so it only needs to be preserved, not built.

**Independent Test**: re-run a three-file request on `personas/keytag_custom_field_old.vtl` with its path; the output set is the same three files, with the same router structure and full-path `#parse` directives, as the skill produces today.

**Acceptance Scenarios**:

1. **Given** a request containing any listed keyword and a file path, **When** the skill migrates, **Then** it produces `_old.vtl`, `_new.vtl` and the router exactly as the current "File Output Pattern" describes.
2. **Given** a three-file request with contents but no path, **When** the skill migrates, **Then** it asks for the filename and server path, as it does today (the path is needed for `#parse`).

---

### User Story 4 - Convert an existing migration between shapes (Priority: P2)

A developer has one of the ten three-file migrations and wants it as a single inline file to hand to a customer; or has an inline file and wants it split for committing to the repo. The skill re-shapes it without re-migrating anything.

**Why this priority**: the ten existing pairs are the obvious first users of the inline shape, and conversion is what lets both shapes coexist without redoing work.

**Independent Test**: collapse `personas/keytag_custom_field` (router + `_new` + `_old`) to inline, then split the result back; `_new` and `_old` come back byte-identical and the router comes back in the canonical structure.

**Acceptance Scenarios**:

1. **Given** a router plus its `_new` and `_old` files, **When** the user asks to convert to inline, **Then** the skill produces one inline file whose `#if` branch is the `_new` content and whose `#else` branch is the `_old` content, each byte-for-byte, after running the same blocking checks as User Story 2.
2. **Given** an inline file and a server path, **When** the user asks to split it, **Then** the skill produces `_new.vtl` (the `#if` branch content), `_old.vtl` (the `#else` branch content) and a router with full-path `#parse` directives.
3. **Given** a conversion in either direction, **When** it runs, **Then** no migration rule is applied to either branch — conversion only moves content.
4. **Given** an input that is not a recognizable router/pair or inline file (for example the router's paths do not match the provided files, or the inline file's outer `#if` is not the edit-mode check), **When** conversion is requested, **Then** the skill says what does not match and does not guess.

---

### Edge Cases

- **Legacy code already broken today**: in router mode it only breaks the legacy edit mode; inline would spread the breakage to the new edit mode. The skill must refuse to inline it silently (FR-010) — the fix belongs to the legacy code, not to the migration.
- **Legacy code that itself uses `#parse` or `$structures.isNewEditModeEnabled()`**: it is copied verbatim into `#else`; nested edit-mode checks are legal and are not rewritten.
- **Very large pairs** (`template_custom_field`, 1 073 lines combined): inline is still one file; size is not a reason to refuse, but the skill must not truncate either branch.
- **Mixed keywords** ("inline, but also give me the router"): the skill asks which shape the user wants rather than producing both.
- **Line endings and trailing newline**: the branch contents keep whatever the source had; the wrapper lines (`#if`, `#else`, `#end`) are added on their own lines without altering the branch bytes.
- **Conversion of a router with space indentation or CRLF** (`url-title.vtl`, `tag_storage_field_creation.vtl`): accepted as a valid router; the round trip compares structure, not the router's whitespace.

## Requirements *(mandatory)*

### Functional Requirements

**Inline generation**

- **FR-001**: In inline mode the skill MUST output exactly one file whose structure is `#if( $structures.isNewEditModeEnabled() )`, the migrated code, `#else`, the original code, `#end` — with no `#parse` directive added and no sibling file.
- **FR-002**: The `#else` branch MUST be the original input byte-for-byte; the skill MUST NOT re-indent it to sit inside the `#if` block. The same no-re-indent rule applies to the migrated branch so both branches can be extracted unchanged.
- **FR-003**: Inline MUST be the mode used when the request names no mode.
- **FR-004**: In inline mode the skill MUST NOT ask for a filename or server path. If a path is given, the single output takes the original file's name.

**Mode selection**

- **FR-005**: The skill MUST switch to three-file mode when the request contains "three files", "separate", "router" or "#parse", or the Spanish equivalents "tres archivos", "separado(s)", "enrutador" or "router"; three-file output MUST be identical in shape to today's "File Output Pattern", including asking for the server path when it is missing.
- **FR-006**: Every migration or conversion output MUST state which shape was produced.
- **FR-007**: When a request contains signals for both shapes, the skill MUST ask which one before producing output.

**Conversion**

- **FR-008**: The skill MUST convert a router plus `_new`/`_old` pair into one inline file, and an inline file (plus a server path) into router, `_new` and `_old`.
- **FR-009**: Conversion MUST move content only: both branches' bytes are preserved exactly, no migration rule is applied, and an input that does not match the expected shape is reported, not guessed at.

**Safety checks before emitting inline** (apply to generation and to three-file → inline conversion)

- **FR-010**: Before emitting inline, the skill MUST check for (a) a `#macro` name defined in both branches and (b) a legacy branch that would not parse on its own (unbalanced block directives or other syntax the skill can recognize as invalid). Either finding is blocking: the skill MUST report it, explain that inline parses both branches so the problem reaches both edit modes, offer the three-file shape, and MUST NOT emit the inline file unless the user explicitly chooses it after that explanation.
- **FR-011**: The skill MUST report every variable that is `#set` in both branches, as a non-blocking warning that names each variable, explains that only one branch executes, and mentions the three-file shape as the alternative.
- **FR-012**: When no finding applies, the inline output MUST still carry a one-line note that both branches are parsed together and the checks passed.

**Documentation**

- **FR-013**: `SKILL.md` MUST document both output modes, the default, the keywords, and conversion; its "Before Outputting" checklist MUST include an inline gate (FR-001, FR-002, FR-010–FR-012) alongside the existing three-file gate.
- **FR-014**: `references/migration-guide.md` MUST document the inline shape with an example, the eager-parse caveat, and when to prefer each mode (inline: no server file access; three-file: files that live in the repo or on disk, or when FR-010 blocks inline).
- **FR-015**: The skill's `description` frontmatter MUST mention inline/single-file output so requests asking for it trigger the skill.

### Key Entities

- **Legacy branch**: the original custom-field VTL, preserved verbatim; lives in `#else` (inline) or `_old.vtl` (three-file).
- **Migrated branch**: the `DotCustomFieldApi` version; lives in the `#if` (inline) or `_new.vtl` (three-file).
- **Router**: the five-line file that `#parse`s one branch by full server path; exists only in three-file mode.
- **Inline file**: one file holding both branches inside the edit-mode `#if`/`#else`/`#end`.
- **Blocking finding / warning**: the outcome of the pre-inline checks — duplicate `#macro` or unparseable legacy (blocking), shared `#set` variable (warning).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An inline migration of `personas/keytag_custom_field` (61 legacy lines) produced by the skill from contents alone renders the field correctly with new edit mode on and with it off, on a running instance, with zero path questions asked.
- **SC-002**: For all 10 existing migrated pairs, three-file → inline → three-file returns `_new` and `_old` byte-identical to the originals and a router with the same structure and `#parse` targets (20 of 20 branch files identical).
- **SC-003**: Of the 10 pairs converted to inline, the skill reports a shared-`#set` warning for exactly one (`tag/tag_storage_field_creation`, naming its 4 shared variables) and a blocking finding for none.
- **SC-004**: On synthetic inputs with a duplicate `#macro` and with an unbalanced legacy directive, the skill emits an inline file 0 times without the user's explicit choice.
- **SC-005**: A request containing any one of the listed mode keywords (English or Spanish) produces the three-file output in every case tried; a request with no keyword produces inline in every case tried.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: the output contract of the `dot-ui-vtl-migration` skill, which serves the Custom Field migration under epic #35757 — the bridge between the legacy (Dojo/Dijit) edit contentlet and the new edit mode. No product code, no Velocity engine behavior and none of the ten migrated VTL files change.
- **Backward-compatibility expectations**: the ten three-file migrations on disk are untouched. The three-file output stays available and unchanged behind a keyword. The one intentional break: anyone relying on the skill defaulting to three files — including the repo's own future migrations of files under `velocity/static/` — must now ask for it by keyword. The skill's documentation must make that visible.
- **Known related decisions**: epic #35757 mandates the if/else revert safety; this feature keeps it in both shapes. The eager-parse versus render-time `#parse` distinction is standard Velocity behavior and is the reason the three-file shape is kept rather than replaced. The plan will formally consult `dotCMS/platform-adrs`.

## Assumptions

- The skill is a set of instructions to an AI agent; "checks" are structural reviews the agent performs on the VTL text, not a Velocity parser run. A legacy branch that fails only in ways not recognizable by reading (for example a runtime-only error) is out of scope for FR-010.
- `$structures.isNewEditModeEnabled()` remains the edit-mode switch for both shapes, unchanged.
- Verification in a running instance (SC-001) uses the existing local dev stack; no new test infrastructure is added to CI for this skill.
- Custom fields that live on disk in this repo will keep being committed in the three-file shape; this feature does not convert any of them.
- Out of scope: migrating additional custom fields, changing the migration rules themselves (API swap, DaisyUI), and any automation that applies the skill in bulk.
