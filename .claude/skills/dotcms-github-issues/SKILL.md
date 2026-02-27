---
name: dotcms-github-issues
description: Create GitHub issues using repository templates. Use when the user asks to create an issue, bug report, feature request, task, spike, epic, or UX requirement. Also use when the user describes a problem, bug, enhancement, or work item that should be tracked. Also use when the user asks to update, query, or view an existing GitHub issue. Also use when the user wants to find, search, list, or discover issues — assigned to them, open for their team, recently active, or matching a keyword. Supports both English and Spanish input.
---

# Create / Update / Query GitHub Issues

AI-agent-native skill for managing GitHub issues in `dotCMS/core`. Infers where possible — asks only what's needed.

**Contents:** [Mode Detection](#step-0--mode-detection) · [CREATE](#create-mode) · [UPDATE](#update-mode) · [QUERY](#query-mode) · [FIND](#find-mode) · [Authorization](#authorization)

**References:** [feature-labels.md](references/feature-labels.md) · [project-fields.md](references/project-fields.md) · [github-apis.md](references/github-apis.md)

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

Concise, imperative, in English. Translate from Spanish if needed.

### Step 4 — Select feature label (optional)

Feature labels are optional. Use [references/feature-labels.md](references/feature-labels.md) to match against the issue description.

- **Single clear match** → apply silently, no confirmation needed. "Clear" means the description contains an explicit keyword from the Keyword Matching Guide. Inferred context does not qualify.
- **2–3 plausible matches** → ask using `AskUserQuestion`. Always include **"None — skip feature label"** as the first option.
- **No clear match** → ask using `AskUserQuestion`. Always include **"None — skip feature label"** as the first option.

Note: `dotCMS : Content Management` is a specific label for issues about core content management features (content types, contentlet operations, content creation workflows). It is **not** a generic fallback.

### Step 4b — Determine native GitHub Issue Type (REQUIRED)

Set via REST PATCH immediately after creation. This is GitHub's first-class type system.

| Template | Native type value |
|---|---|
| Defect | `Bug` |
| Task | `Task` |
| Spike | `Spike` |
| Feature | `Feature` |
| Epic | `Epic` |
| UX | `Task` |
| Pillar | `Pillar` |

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
3. **No cache** → use a two-stage cascading selection. Team is determined by which team the developer belongs to — it has no relation to issue content.

   **Stage 1** — present the 4 highest-usage teams (by historical frequency):
   - `Team : Falcon`
   - `Team : Maintenance`
   - `Team : Scout`
   - `Team : Platform`

   If the user picks "Other", proceed to Stage 2.

   **Stage 2** — present the next tier:
   - `Team : Modernization`
   - `Team : Enablement`
   - `Team : Security`
   - `Team : UX`

   If the user picks "Other" again, they type the team name freely (covers `Team : Architecture`, `Team : Lunik`, `Team : Cloud Eng`, `Team : 3rd Party`, or any new team).

   After selection at any stage, ask: "Set as default team for future issues?" If yes:
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

### Step 8 — Build issue body

Match the template structure read in Step 2. Populate all fields substantively from the user's description.

Best-practice patterns:
- Tables for comparisons
- Code blocks for examples
- Checkboxes for acceptance criteria
- Numbered lists for reproduction steps

All content in English (translate Spanish if needed).

### Step 9 — Create the issue

**CRITICAL**: Do NOT use `--template` flag — incompatible with `--title`/`--body` in non-interactive mode.

```bash
gh issue create \
  --repo "dotCMS/core" \
  --title "TITLE" \
  --body "BODY" \
  --label "TYPE_LABEL,TEAM_LABEL,FEATURE_LABEL[,TEMPLATE_LABELS]"
```

Where:
- `TYPE_LABEL` = the `Type :` label from Step 4c (include when matched)
- `TEAM_LABEL` = `Team : [Name]` from Step 6
- `FEATURE_LABEL` = `dotCMS : [Feature]` from Step 4
- `TEMPLATE_LABELS` = labels defined in the template file (e.g., `Triage`, `OKR : Customer Support`)

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

```bash
# Main issue data
gh issue view ISSUE_NUM --repo dotCMS/core \
  --json number,title,state,labels,assignees,body,projectItems,url

# Sub-issues
gh api repos/dotCMS/core/issues/ISSUE_NUM/sub_issues

# Native type
gh api repos/dotCMS/core/issues/ISSUE_NUM --jq '.type.name'
```

**Output includes:**
- Title, state, labels, assignees, URL
- Native GitHub Issue Type
- Project #7 Status, Technology, Priority fields
- Parent issue (if sub-issue)
- Sub-issues list with completion status
- Blocked-by / blocking relationships

See Section H of [references/github-apis.md](references/github-apis.md) for full query patterns.

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

**Step 1 — Read cached team and resolve project view URL (if available):**
```bash
CACHED_TEAM=$(cat ~/.config/dotcms/create-issue/default-team 2>/dev/null)
```

If `$CACHED_TEAM` is set, look up the team's project view number from [references/project-fields.md](references/project-fields.md). If the view number is known, construct the direct board URL:
```
https://github.com/orgs/dotCMS/projects/7/views/N
```

If the view number is not yet recorded, discover it:
```bash
gh api graphql -f query='
  {
    organization(login: "dotCMS") {
      projectV2(number: 7) {
        views(first: 30) {
          nodes { number name }
        }
      }
    }
  }' | jq -r --arg team "TEAM_NAME" \
    '.data.organization.projectV2.views.nodes[] | select(.name | test($team; "i")) | "\(.number) \(.name)"'
```

Store the resolved view URL (if found) as `$TEAM_VIEW_URL`.

**Step 1b — Resolve sprint filter (if a sprint intent was detected):**

If the user's request includes a sprint filter, resolve it before querying issues.

**No cached team + sprint filter:** If `$CACHED_TEAM` is empty and the user asked for a sprint-filtered view, ask which team before proceeding — sprint fields are team-specific and cannot be resolved without a team. Use `AskUserQuestion` with the same two-stage team list from Step 6 of CREATE mode.

Derive the sprint field name: strip `Team : ` prefix, append ` Sprint` (e.g. `Team : Scout` → `Scout Sprint`). Known field IDs and the iteration discovery query are in [references/project-fields.md](references/project-fields.md) — "Sprint / Iteration Fields" section. Use known IDs to skip the discovery call for Falcon, Maintenance, Scout, and Platform.

From the returned configuration, identify the target sprint title using today's date:

| Sprint intent | Which iteration |
|---|---|
| Current | `iterations[]` where `startDate` ≤ today AND `startDate + duration` > today |
| Next | `iterations[]` where `startDate` > today, earliest by `startDate` |
| Previous / Last | `completedIterations[]`, most recent by `startDate` |
| Named (e.g. "Sprint 42") | Match `title` in `iterations[]` or `completedIterations[]` |

Store the resolved sprint title as `$SPRINT_TITLE` for use in Step 2 jq filtering.

**No sprint field found (teams without iteration fields):** Teams known to use kanban / status-only workflows (Enablement, Modernization, UX, Architecture, and others not in the sprint field table in project-fields.md) do not have iteration fields. When the field query returns no match:

1. Tell the user: *"[Team] doesn't use sprint iterations — showing issues by status instead."*
2. Map the sprint intent to Status values:

| Sprint intent | Status values to show |
|---|---|
| Current / this sprint | `In Progress`, `In Review`, `Current Sprint Backlog` |
| Next sprint | `Next Sprint`, `Next 2-4 Sprints` |
| Previous / last sprint | Cannot reliably reconstruct — offer to show `Done` issues updated in the last 2 weeks instead |

3. Use the flat `gh issue list` + jq status filter (see [references/project-fields.md](references/project-fields.md)) rather than the GraphQL sprint query.

**Step 2 — Run the appropriate queries:**

> **jq shell-quoting rules (apply to all queries below):**
> - Never use `!=` in a jq expression passed via bash — the `!` is shell-escaped as `\!` causing a compile error. Use chained `select()` calls instead: `select($x) | select(...)` rather than `select($x != null)`.
> - Never reuse a `--arg` name as an internal `as $var` binding in the same expression. Use distinct names (e.g. `--arg sprint "..."` + internal `as $iterTitle`).

**Assigned to current user (open):**
```bash
gh issue list --repo dotCMS/core \
  --assignee @me \
  --state open \
  --json number,title,url,labels,assignees,updatedAt,projectItems \
  --limit 50
```

**Open issues for cached team (if `$CACHED_TEAM` is set, no sprint filter):**
```bash
gh issue list --repo dotCMS/core \
  --label "$CACHED_TEAM" \
  --state open \
  --json number,title,url,labels,assignees,updatedAt,projectItems \
  --limit 50
```

> **Sprint filter — use GraphQL instead of `gh issue list`:** Teams have 1000+ issues; `gh issue list` returns up to 100 oldest-first and misses recent Done/closed items. Use the GraphQL sprint query from [references/project-fields.md](references/project-fields.md) — "Filter issues by sprint iteration" section. It uses `states: [OPEN, CLOSED]` and `orderBy: UPDATED_AT DESC`, filtering by `$iterTitle == $sprint`.

**Keyword search:**
```bash
gh issue list --repo dotCMS/core \
  --search "KEYWORD in:title,body" \
  --state open \
  --json number,title,url,labels,updatedAt \
  --limit 10
```

**Recently updated (not filtered by assignee or team):**
```bash
gh issue list --repo dotCMS/core \
  --state open \
  --json number,title,url,labels,updatedAt,assignees \
  --limit 10 \
  --sort updated
```

Run only the queries relevant to the user's intent. For **Default**, run Assigned and Team in parallel and deduplicate by issue number.

---

**Step 3 — Extract project fields from results:**

`gh issue list --json projectItems` returns a flat structure per project — Status is directly accessible but sprint/iteration values are not. See [references/project-fields.md](references/project-fields.md) — "Filter issues by status (fast — flat structure)" for the exact extraction pattern. Show "—" when status is absent.

---

**Step 4 — Present results:**

> **Pagination note:** Sprint queries use `first: 100` with `orderBy: UPDATED_AT DESC`. This covers almost all cases since sprint work is recent. On very active teams where 100+ issues were updated more recently than the sprint's oldest items, some sprint items could be missed. If results look incomplete, paginate by adding `after: "CURSOR"` using the `pageInfo.endCursor` from a prior response — but this is rarely needed in practice.

Format as a numbered list grouped by query type. Include the sprint column when a sprint filter was applied or when sprint data is present.

**When results are from a sprint query, prepend a velocity summary line:**

```
5 Done · 2 In Progress · 1 In Review · 1 Current Sprint Backlog
```

Derive counts from the result set:
```bash
jq 'group_by(.status) | map({status: .[0].status, count: length}) | sort_by(.count) | reverse'
```

Render as `N StatusName · N StatusName · ...` ordered by count descending. Omit statuses with 0 count.

For each issue show:
- Number and title
- Clickable URL
- Status from Project #7 (if available)
- Sprint assignment (if available — "—" if not set)
- Assignee(s) — always show; helps spot misassignment or abandoned work
- Last updated (relative: "today", "2 days ago", "3 weeks ago")
- Exception flags (see below) — shown inline as `⚠`

**Exception flag — applies when status is "In Progress" or "In Review":**

| Condition | Flag |
|---|---|
| Last updated > 30 days ago | `⚠ stale (N months)` |

Sprint assignment is shown as "—" when not set — no flag, just the data. The stale flag is the only active signal. It doesn't filter out the issue — it appears inline so the user can decide whether to investigate.

Example output (no sprint filter):
```
**Team: Scout (In Progress)**
Board: https://github.com/orgs/dotCMS/projects/7/views/5

1. #34354 — [DEFECT] Content Drive Search leaks info with limited user
   https://github.com/dotCMS/core/issues/34354
   Status: In Progress | Sprint: — | Assignee: jsmith | Updated: yesterday

2. #33829 — [TASK] Create Developer Onboarding Guide for Next.js with dotCMS UVE
   https://github.com/dotCMS/core/issues/33829
   Status: In Progress | Sprint: — | Assignee: fmontes | Updated: Nov 2025
   ⚠ stale (3 months)
```

Example output (with sprint filter "last sprint"):
```
**Team: Scout — Sprint 4: Feb 24, 2026 (previous)**
Board: https://github.com/orgs/dotCMS/projects/7/views/5
5 Done · 0 In Progress

1. #34723 — CollectionBuilder.draft() does not return draft content when deployed to Vercel
   https://github.com/dotCMS/core/issues/34723
   Status: Done | Assignee: — | Updated: Feb 24

2. #34708 — Expose `registerStyleEditorSchema` in the `dotUVE` Global Object
   https://github.com/dotCMS/core/issues/34708
   Status: Done | Assignee: — | Updated: Feb 24
```

When a sprint filter is active, include the resolved sprint title and intent ("current", "next", "previous") in the group header so the user can confirm it matched the right sprint.

Include the **Board** link at the top of every team-scoped group when `$TEAM_VIEW_URL` is known. Omit silently if the view number is unknown — do not show a broken URL.

After listing, offer:
> "Enter a number to query details, update, or use as a parent for a new sub-issue."

---

**Step 5 — Handle follow-up selection:**

If the user selects a number from the list:
- Map the selection to the issue number from the results
- Treat it as if the user had said "query issue #N", "update issue #N", or "sub-issue of #N" — proceed in the appropriate mode
- No need to re-ask for the issue number

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