# FIND Mode — Query Mechanics

Commands, GraphQL, jq patterns, and output formatting for FIND mode. SKILL.md carries the
decision flow; this file carries the mechanics.

**Contents:** [Step 1 — team + board URL](#step-1--read-cached-team-and-resolve-project-view-url) · [Step 1b — resolve sprint](#step-1b--resolve-sprint-filter) · [Step 2 — queries](#step-2--run-the-appropriate-queries) · [Step 3 — extract project fields](#step-3--extract-project-fields-from-results) · [Step 4 — present results](#step-4--present-results)

---

## Step 1 — Read cached team and resolve project view URL
```bash
CACHED_TEAM=$(cat ~/.config/dotcms/create-issue/default-team 2>/dev/null)
```

If `$CACHED_TEAM` is set, look up the team's project view number from [project-fields.md](project-fields.md). If the view number is known, construct the direct board URL:
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

## Step 1b — Resolve sprint filter

If the user's request includes a sprint filter, resolve it before querying issues.

**No cached team + sprint filter:** If `$CACHED_TEAM` is empty and the user asked for a sprint-filtered view, ask which team before proceeding — sprint fields are team-specific and cannot be resolved without a team. Use `AskUserQuestion` with the same two-stage team list from Step 6 of CREATE mode.

Derive the sprint field name: strip `Team : ` prefix, append ` Sprint` (e.g. `Team : Scout` → `Scout Sprint`). Known field IDs and the iteration discovery query are in [project-fields.md](project-fields.md) — "Sprint / Iteration Fields" section. Use known IDs to skip the discovery call for Falcon, Maintenance, Scout, and Platform.

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

3. Use the flat `gh issue list` + jq status filter (see [project-fields.md](project-fields.md)) rather than the GraphQL sprint query.

## Step 2 — Run the appropriate queries

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

> **Sprint filter — use GraphQL instead of `gh issue list`:** Teams have 1000+ issues; `gh issue list` returns up to 100 oldest-first and misses recent Done/closed items. Use the GraphQL sprint query from [project-fields.md](project-fields.md) — "Filter issues by sprint iteration" section. It uses `states: [OPEN, CLOSED]` and `orderBy: UPDATED_AT DESC`, filtering by `$iterTitle == $sprint`.

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

## Step 3 — Extract project fields from results

`gh issue list --json projectItems` returns a flat structure per project — Status is directly accessible but sprint/iteration values are not. See [project-fields.md](project-fields.md) — "Filter issues by status (fast — flat structure)" for the exact extraction pattern. Show "—" when status is absent.

---

## Step 4 — Present results

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

