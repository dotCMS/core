---
name: dot-issue-manage
owner: "@dotcms/scout"
status: active
description: Create GitHub issues using repository templates. Use when the user asks to create an issue, bug report, feature request, task, spike, epic, or UX requirement. Also use when the user describes a problem, bug, enhancement, or work item that should be tracked. Also use when the user asks to update, query, or view an existing GitHub issue. Also use when the user wants to find, search, list, or discover issues — assigned to them, open for their team, recently active, or matching a keyword. Supports both English and Spanish input.
---

# Create / Update / Query / Find GitHub Issues

Manages GitHub issues in `dotCMS/core`. Infers where possible — asks only what's needed.

**Issues have two audiences.** A human triager reads the first screen and decides; an agent
implementing the work reads everything. Write for the human first and fold the depth beneath —
see [references/issue-writing-style.md](references/issue-writing-style.md).

**Contents:** [Mode Detection](#step-0--mode-detection) · [CREATE](#create-mode) · [UPDATE](#update-mode) · [QUERY](#query-mode) · [FIND](#find-mode) · [Authorization](#authorization)

**References:** [issue-writing-style.md](references/issue-writing-style.md) · [issue-refinement.md](references/issue-refinement.md) · [feature-labels.md](references/feature-labels.md) · [project-fields.md](references/project-fields.md) · [github-apis.md](references/github-apis.md) · [find-queries.md](references/find-queries.md)

## Step 0 — Mode Detection

### 0a — Check current branch for issue context

Before classifying the request, check the current git branch for an embedded issue number:

```bash
git branch --show-current 2>/dev/null
```

Extract the issue number by trying these patterns in order (stop at first match):

| Priority | Pattern | Example branch | Extracts |
|---|---|---|---|
| 1 | `issue-(\d+)` | `issue-34791-nx-workspace` | `34791` |
| 2 | Branch starts with digits | `34792-fix-login` | `34792` |
| 3 | Digits after `/` or `-` separator | `fix/34793-something` | `34793` |

Do **not** match short numeric suffixes that are clearly not issue numbers (e.g., `setup-e028`, `v2`, `node22`). A valid issue number is 4+ consecutive digits standing alone — not embedded inside a word or preceded by a letter.

Store the result as the **branch issue number**.

**Use the branch issue number as the default** for UPDATE and QUERY operations when the user refers to "the issue", "this issue", "it", "the current issue/PR", or any other pronoun without specifying a number explicitly — unless the user is clearly discussing a different issue mid-conversation.

If no issue number is found in the branch name, proceed without a default.

### 0b — Classify mode

Read the user's request and classify:

- **CREATE** — "create an issue for X", "file a bug for Y", "spike on Z", "I need an issue for…"
- **UPDATE** — "update issue #N", "set issue #N to In Progress", "add sub-issue to #N", "change the title of #N"
- **QUERY** — "show issue #N", "what's the status of #N", "list sub-issues of #N", "check #N"
- **FIND** — "find an issue", "what issues are assigned to me", "show my open issues", "which issues am I working on", "open issues for my team", "search for X", "what's on my plate", "show recent issues"

If the mode is UPDATE or QUERY and no issue number is given but a branch issue number was found, use it and proceed — no need to ask.

If genuinely unclear, ask one question to clarify.

---

## CREATE Mode

### Step 1 — Classify issue type

From the user's description — do NOT ask unless genuinely ambiguous:

| User signals | Template |
|---|---|
| Bug / not working / broken / error / regression | **Defect** |
| Research / investigate / POC / spike / unknowns / explore | **Spike** |
| Task / implement / refactor / update library / improve / CI / infra | **Task** |
| New feature / add capability (product context) | **Feature** |
| Large initiative / umbrella work | **Epic** |
| UX / usability / interface problem / design | **UX** |

Note: Feature and Epic templates are "Product team use only"; Defect, Task, and Spike are "Engineering team use".

### Step 2 — Read the correct template

```bash
ls .github/ISSUE_TEMPLATE/
```

Then read the specific template file for the type chosen in Step 1 using the Read tool.
Always read fresh — never assume structure.

### Step 3 — Generate title

The title is what a triager scans in a list of 200. In English (translate from Spanish if needed).

- **≤ 12 words.** Longer means two ideas — pick the one that matters.
- **Name the symptom, not the mechanism.** "Multi-file selection uploads only the first file"
  beats "AssetPicker onSelect iterates files[0]".
- One clause. No colon-plus-restatement, no comma-chained second finding.
- Area prefix only when it disambiguates (`block-editor:`, `Asset Picker:`) — never a
  `[DEFECT]` / `[TASK]` prefix, the labels and native type already carry that.

| Bad | Rewrite |
|---|---|
| `Phase 2 ES read fallback never fires: content read path bypasses PhaseRouter, OpenSearch outage returns empty results` | `Phase 2: OpenSearch outage returns empty content instead of falling back to ES` |
| `[TASK] Improve the developer onboarding experience` | `Add \`dotcms agent setup\` — one command to connect an IDE to dotCMS` |

### Step 4 — Select feature label (optional)

Feature labels are optional. Use [references/feature-labels.md](references/feature-labels.md) to match against the issue description.

- **Single clear match** → apply silently, no confirmation needed. "Clear" means the description contains an explicit keyword from the Keyword Matching Guide. Inferred context does not qualify.
- **2–3 plausible matches** → ask using `AskUserQuestion`. Always include **"None — skip feature label"** as the first option.
- **No clear match** → ask using `AskUserQuestion`. Always include **"None — skip feature label"** as the first option.

Note: `dotCMS : Content Management` is a specific label for issues about core content management features (content types, contentlet operations, content creation workflows). It is **not** a generic fallback.

### Step 4b — Determine native GitHub Issue Type (REQUIRED)

Set via REST PATCH immediately after creation. This is GitHub's first-class type system.

Read the matching template file in `.github/ISSUE_TEMPLATE/` (e.g. `defect.yaml`, `task.yaml`) and use its `type:` field value — that is the exact string for the REST PATCH call. For example, `defect.yaml` has `type: bug` → use `Bug` (capitalize first letter).

> **Exception:** `ux.yaml` has no `type:` field. Use `Task` for UX issues.

### Step 4c — Select `Type :` label (OPTIONAL)

Apply alongside the feature label. Omit only when no reasonable match exists.

| Template | Default `Type :` label | Refine on description keywords |
|---|---|---|
| Defect | `Type : Defect` | — |
| Spike | `Type : Spike` | — |
| Task | `Type : Task` | "refactor" → `Type : Refactoring`; "CI/CD/pipeline/workflow" → `Type : CI/CD`; "docs/documentation" → `Type : Documentation`; "test automation/e2e" → `Type : Test Automation`; "technical design/ADR" → `Type : Technical Design` |
| Feature | `Type : New Functionality` | — |
| Epic | `Type : New Functionality` | — |
| UX | `Type : Visual Design` | — |

### Step 5 — Determine Technology

Derived automatically from user description — no extra input needed:

| Signals | Technology value |
|---|---|
| Angular / frontend / UI / TypeScript / component / admin interface | `Front-end` |
| Java / REST / backend / Spring / Maven / server-side | `Java` |
| Angular + Java / full-stack / both frontend and backend | `FE and BE` |
| Build / CI / Docker / infra / Nx / npm / Maven structure / pipeline | `Platform` |
| Marketing / sales / business | `Go-To-Market` |

See [references/project-fields.md](references/project-fields.md) for field and option IDs.

### Step 6 — Determine team assignment

**Cache file:** `~/.config/dotcms/create-issue/default-team`

This path is user-level and stable regardless of where the skill is installed (user-level `~/.claude/` or project-level plugin).

**Resolution order:**

1. **Explicit in user's message** (e.g., "Enablement team issue", "for the Falcon team") → use it directly; if different from cached default, offer to update the cache
2. **Cache hit** — read the file:
   ```bash
   cat ~/.config/dotcms/create-issue/default-team 2>/dev/null
   ```
   If non-empty, use that team silently — no question asked. Mention it briefly in the confirmation (e.g., "Team: Enablement (default)").
3. **No cache** → ask with `AskUserQuestion`, using the two-stage cascading list in
   [references/project-fields.md](references/project-fields.md) — "Team selection cascade".
   Team follows which team the developer belongs to; it has no relation to issue content.

   After selection, ask: "Set as default team for future issues?" If yes:
   ```bash
   mkdir -p ~/.config/dotcms/create-issue && echo "Team : SELECTED_TEAM" > ~/.config/dotcms/create-issue/default-team
   ```

**Override:** If the user says the wrong team was used, or provides a different team explicitly, apply the new team and ask whether to update the default.

**Reset:** User can clear the cache at any time:
```bash
rm ~/.config/dotcms/create-issue/default-team
```

If the user specifies a team not in the known list, verify it exists before applying:
```bash
gh label list --repo dotCMS/core --limit 1000 | grep "^Team :"
```

### Step 7 — Check for relationships

Scan user's description for:
- "sub-issue of #N" / "child of #N" / "part of epic #N" → capture parent number
- "blocks #N" / "blocked by #N" → capture dependency numbers

Ask only if partially specified (e.g., "it's a sub-issue" but no number given).

### Step 7a — Quick-draft detection (skip Issue Refinement when requested)

**Trigger:** User explicitly asks to create the issue without going through ambiguity resolution. Treat as quick-draft when the request includes any of:

- **English:** "quick draft", "just draft it", "without full details", "skip ambiguities", "skip the questions", "create quickly", "minimal details"
- **Spanish:** "draft rápido", "borrador rápido", "sin detalles completos", "sin resolver ambigüedades", "sin preguntas", "crear rápido", "sin pasar por ambigüedades"

The trigger is about *skipping the clarification questions*, not about the work being small.
"quick fix", "quick win", or "a small task" are descriptions of the issue — they do **not** set
quick-draft.

When **quick-draft** is set:

- **Skip Step 7b** (Issue Refinement loop) entirely — do not run Phase 1–6, do not ask clarification questions.
- In Step 8, for the Acceptance Criteria section use a short placeholder, e.g.:
  - "**Acceptance criteria** — To be refined. (Quick draft; details to be added later.)"
  - Or 1–3 bullet points derived only from the user's raw description, with no ambiguity resolution.
- Proceed directly from Step 7 to Step 8 after relationships are captured.

When quick-draft is **not** set, continue to Step 7b as usual.

### Step 7b — Issue Refinement Loop (REQUIRED before writing body, unless quick-draft)

Read [references/issue-refinement.md](references/issue-refinement.md) and execute the full loop against the user's description.

**Goal:** Produce unambiguous, testable Acceptance Criteria as checkbox items before the issue body is written.

**Execute in strict order** (phase numbering matches the reference file):

1. **Phase 1 — Decompose**: Extract problem, actor, expected behavior, business rules, out-of-scope.
2. **Phase 2 — Ambiguity Scan**: Flag every ambiguity by type and severity (CRITICAL / MAJOR / MINOR).
3. **Phase 3 — Clarification Questions**: For each CRITICAL and MAJOR ambiguity, ask one precise question with concrete options. Present all questions at once, then **stop and wait**. Do NOT guess. Do NOT proceed.
4. **Phase 4 — Re-Analyze**: After the user replies, confirm resolutions and check for new ambiguities. If new CRITICAL/MAJOR found → return to Phase 3.
5. **Phase 5 — Write AC**: When all CRITICAL + MAJOR are resolved (or loop 3 reached), write checkbox-based Acceptance Criteria covering happy path, sad path, and edge cases.
6. **Phase 6 — Compress**: Merge, cut, and group down to 3–7 criteria, one line each, stating observable outcomes. This phase is not optional — the refinement loop produces precision, not volume.

**Loop control:** Max 3 clarification rounds. After round 3 — rewrite with best available info, flag unresolved items with `⚠ UNRESOLVED`.

**Skip condition:** If the user's request is a Defect/Bug and contains reproduction steps with expected vs actual results — ambiguity scan still runs, but the bar for CRITICAL is higher (focus on missing edge cases and error handling rather than unclear outcomes).

**The refinement conversation is not the issue body.** Ambiguity tables, resolution summaries, scores, and rejected options stay in the conversation. Only the compressed Phase 6 checkboxes go into the **Acceptance Criteria section** in Step 8.

### Step 8 — Build issue body

Match the template structure read in Step 2. All content in English (translate Spanish if needed).

**Issues are read by humans first.** A triager, a PM, or a developer picking the work up months
later reads the first screen and decides whether to keep reading. Technical depth stays in the
issue — it goes *below* the summary, not above it.

Read [references/issue-writing-style.md](references/issue-writing-style.md) for body shapes per
template, worked before/after examples, and the pre-publish checklist.

**Non-negotiable rules — apply to every issue, quick-draft included:**

1. **Lead with the consequence.** The first sentence of the body says what is broken (or what is
   missing) and what it costs, in plain language. One sentence, two at most, ≤ 40 words. No class
   names, file paths, line numbers, or stack frames in it. Never open with background or
   "As part of the X migration…".
2. **≤ 150 words visible** before the first collapsed block, lead included. Impact goes in at most
   4 one-line bullets.
3. **Fold the depth.** Stack traces, code blocks over 10 lines, line-number inventories,
   call-site enumerations, design-doc citation tables, and alternatives-considered go inside
   `<details><summary>specific label</summary>`. GitHub collapses it for humans; the API returns
   the full text, so agents lose nothing.
4. **Never fold** Steps to Reproduce, Acceptance Criteria, version, severity, or links — a
   reviewer or QA acts directly on those.
5. **Acceptance criteria: 3–7**, one line each (≤ 25 words), one checkbox per *observable
   outcome* — not per implementation site. Above 7, group under sub-headings. Hard cap 12.
   Implementation specifics belong in a folded note, not in a checkbox.

**When quick-draft was set (Step 7a):** rules 1–4 still apply — brevity is not an excuse for an
unreadable lead. For Acceptance Criteria use a short placeholder ("To be refined") or 1–3 bullets
drawn from the user's description; do not run or reuse the Issue Refinement loop.

### Step 8b — Readability gate (REQUIRED before creating)

Copy this checklist into your response and answer each item against the drafted body. Any `no`
→ fix the body and re-check. Do not proceed to Step 9 with an unchecked box.

```
- [ ] First sentence is ≤ 40 words and names the consequence, not the mechanism
- [ ] First sentence contains no file path, class name, line number, or stack frame
- [ ] Reading only the first 10 lines tells you what it is and why it matters
- [ ] Visible prose before the first fold is ≤ 150 words
- [ ] Every stack trace, long code block, and line-number inventory is inside <details>
- [ ] Every <summary> says what is inside it, specifically
- [ ] Acceptance criteria: ≤ 7, or grouped under sub-headings
- [ ] Every acceptance criterion is one line and states an observable outcome
- [ ] Steps to Reproduce and Acceptance Criteria are not folded
```

### Step 9 — Create the issue

**CRITICAL**: Do NOT use `--template` flag — incompatible with `--title`/`--body` in non-interactive mode.

```bash
gh issue create \
  --repo "dotCMS/core" \
  --title "TITLE" \
  --body "BODY" \
  --label "LABEL1,LABEL2,..."
```

Build the label list by including only those that apply — omit any that were not selected:
- `Type :` label from Step 4c (include when matched)
- `Team : [Name]` from Step 6 (always include)
- `dotCMS : [Feature]` from Step 4 (omit if none was selected — do not leave a trailing comma)
- Template labels from the template file (e.g., `Triage`, `OKR : Customer Support`)

Capture the returned issue number.

### Step 9b — Set native GitHub Issue Type (REQUIRED, always)

Immediately after creation:

```bash
gh api repos/dotCMS/core/issues/ISSUE_NUM -X PATCH -f type='TYPE_NAME'
```

Use the type name from Step 4b: `Bug` | `Task` | `Spike` | `Feature` | `Epic` | `Pillar`.

See [references/github-apis.md](references/github-apis.md) — Section A.

### Step 10 — Set Technology field in Project #7

No extra user input needed — derived from Step 5.

1. Get the project item ID (Section B of [references/github-apis.md](references/github-apis.md))
2. Set the Technology single-select field (Section C) using IDs from [references/project-fields.md](references/project-fields.md)

Status defaults to "New" automatically — do not set it.

### Step 11 — Set relationships (if captured in Step 7)

- **Parent/sub-issue**: POST to sub-issues REST endpoint — Section E of [references/github-apis.md](references/github-apis.md)
  - Requires the child issue's database ID (Section D), not the display number
- **Blocked-by/blocking**: Add a cross-reference comment on the related issue

### Step 12 — Confirm

Report back:
- Issue URL
- Team label applied
- Feature label applied
- `Type :` label applied (if any)
- Native type set
- Technology field set in Project #7
- Any relationships established

**Quick-draft tip (only when Issue Refinement ran):** If the Issue Refinement loop (Step 7b) was executed (i.e., quick-draft was **not** set), append a brief tip after the confirmation:

> *Tip: Say "quick draft" or "rápido" next time to skip the AC clarification step.*

---

## UPDATE Mode

Accept an issue number and user intent. Show current state first, then apply changes.

### Flow

**Step 1 — Fetch current state:**
```bash
gh issue view NUMBER --repo dotCMS/core \
  --json number,title,state,labels,assignees,body,projectItems,url
```

**Step 2 — Validate and show current state:**

For each expected field, explicitly confirm ✓ correct or flag ✗ gap:

- **Feature label** (`dotCMS : *`): Is one present? Is it the right one for this issue type/content?
- **Team label** (`Team : *`): Present and correct?
- **`Type :` label**: Present and matching the issue type?
- **Native GitHub type**: Matches the issue template type?
- **Technology field** (Project #7): Set? Correct for the content?

Show a validation table to the user, e.g.:
```
✓ dotCMS : Build    — feature label, correct
✓ Team : Enablement — team label, correct
✗ Type : Spike      — missing
✗ Native type       — Task (should be Spike)
✗ Technology        — not set (should be Platform)
```

Then propose the changes needed based on the gaps found.

**Step 3 — Apply changes** (as specified by user or ask what to change):

| What to update | Command |
|---|---|
| Labels (add) | `gh issue edit N --repo dotCMS/core --add-label "LABEL"` |
| Labels (remove) | `gh issue edit N --repo dotCMS/core --remove-label "LABEL"` |
| Title | `gh issue edit N --repo dotCMS/core --title "NEW TITLE"` |
| Native type | `gh api repos/dotCMS/core/issues/N -X PATCH -f type='TYPE_NAME'` |
| Project Status / Technology / Priority | GraphQL mutation — Section C of [references/github-apis.md](references/github-apis.md) |
| Add sub-issue | Section E of [references/github-apis.md](references/github-apis.md) |
| Remove sub-issue | Section G of [references/github-apis.md](references/github-apis.md) |

**Step 4 — Confirm** each change applied.

---

## QUERY Mode

Accept an issue number. Return comprehensive state.

### Flow

Run the three queries in Section H of [references/github-apis.md](references/github-apis.md):
main issue data, sub-issues, native type.

**Output includes:**
- Title, state, labels, assignees, URL
- Native GitHub Issue Type
- Project #7 Status, Technology, Priority fields
- Parent issue (if sub-issue)
- Sub-issues list with completion status
- Blocked-by / blocking relationships

---

## FIND Mode

Discover issues relevant to the user without needing a specific issue number. Results are presented as a numbered list with clickable URLs that can be used directly in follow-up UPDATE, QUERY, or CREATE (sub-issue) operations.

### Trigger classification

Infer the search intent from the user's phrasing. Sprint and scope filters compose freely.

**Scope filter** (who/what):

| Phrasing | Scope |
|---|---|
| "assigned to me", "my issues", "what am I working on", "what's on my plate" | **Assigned** |
| "my team's issues", "open for [team]", "what is [team] working on" | **Team** |
| "recent", "recently updated", "what changed lately" | **Recent** |
| "find X", "search for X", "issues about X" | **Keyword** |
| No specific filter | **Default: Assigned + Team combined** |

**Sprint filter** (when — applied on top of scope filter when present):

| Phrasing | Sprint intent |
|---|---|
| "current sprint", "this sprint", "in sprint", "in the sprint" | **Current** |
| "next sprint" | **Next** |
| "last sprint", "previous sprint" | **Previous** |
| "sprint 42" / any specific sprint name | **Named** — match title |
| No sprint filter | No sprint constraint — use Status field as fallback if helpful |

Sprint + scope combinations are valid: "my issues in the current sprint" = Assigned filtered to current; "Enablement issues next sprint" = Team filtered to next sprint.

If the intent is ambiguous between Assigned and Team, run both and merge results.

---

### Flow

All commands, GraphQL queries, jq patterns, and output formatting live in
[references/find-queries.md](references/find-queries.md) — read it before running any query.

**Step 1 — Resolve team and board URL.** Read the cached team
(`~/.config/dotcms/create-issue/default-team`). If set, look up its project view number in
[references/project-fields.md](references/project-fields.md) and build the board URL.

**Step 1b — Resolve the sprint (only if a sprint intent was detected).** Derive the field name
from the team (`Team : Scout` → `Scout Sprint`), then match today's date against the iterations
to pick current / next / previous / named. Teams on kanban have no iteration field — fall back
to Status values and say so. If no team is cached and a sprint was asked for, ask which team
first (`AskUserQuestion`, same two-stage list as CREATE Step 6).

**Step 2 — Run only the queries the intent calls for.** Assigned, Team, Keyword, or Recent; for
**Default**, run Assigned and Team and deduplicate. Sprint-filtered searches must use the
GraphQL query, not `gh issue list` — the CLI returns 100 oldest-first and misses recent items.

**Step 3 — Extract Status from `projectItems`.** Show "—" when absent.

**Step 4 — Present as a numbered list**, grouped, with the board link, a velocity summary line
for sprint queries, and a `⚠ stale (N months)` flag on In Progress / In Review issues untouched
for over 30 days. Then offer:

> "Enter a number to query details, update, or use as a parent for a new sub-issue."

**Step 5 — Handle follow-up selection.** If the user picks a number, map it to the issue number
and continue in the matching mode (QUERY / UPDATE / CREATE sub-issue) — do not re-ask.

---

### Notes

- If neither `--assignee @me` nor a team label returns results, fall back to recently updated issues across the repo (limit 10, sort by updated).
- If a keyword search returns no results, say so clearly and suggest broadening the terms.
- De-duplicate results across query types — if an issue appears in both Assigned and Team lists, show it once under whichever group is more specific (Assigned takes priority).
- Issues from the current branch (Step 0a) are shown at the top of results if they appear, flagged as "current branch".

---

## Authorization

If project field mutations fail, refresh GitHub CLI auth:
```bash
gh auth refresh -s read:project -s project --hostname github.com
```