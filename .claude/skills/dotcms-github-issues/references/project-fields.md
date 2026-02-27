# GitHub Project #7 — Field Reference

**Contents:** [Status field](#field-status) · [Technology field](#field-technology) · [Priority field](#field-priority) · [Sprint / Iteration fields](#sprint--iteration-fields) · [Filter by status (fast)](#filter-issues-by-status-fast--flat-structure) · [Filter by sprint (GraphQL)](#filter-issues-by-sprint-iteration-graphql--full-fieldvalues) · [Team Project Views](#team-project-views) · [Team Labels](#known-team-labels) · [Type Labels](#type-label-taxonomy) · [Native Issue Types](#native-github-issue-types)

```
Project: dotCMS - Product Planning
Project Number: 7
Project Node ID: PVT_kwDOAA9Wz84AKDq_
```

## Field: Status

Field ID: `PVTSSF_lADOAA9Wz84AKDq_zgGQTm4`

| Option | Option ID |
|---|---|
| New | `f75ad846` |
| Future | `47fc9ee4` |
| Next 2-4 Sprints | `25ecbb2a` |
| Next Sprint | `69d37ccf` |
| Current Sprint Backlog | `98236657` |
| On Hold | `085afabe` |
| In Progress | `2d61e9f1` |
| In Review | `d773edc0` |
| QA | `48373cc3` |
| Done | `0a6e784e` |

> Default on creation: **New** (set automatically — do not set via mutation)

## Field: Technology

Field ID: `PVTSSF_lADOAA9Wz84AKDq_zgH5iBk`

| Option | Option ID |
|---|---|
| Platform | `dd7e7bda` |
| Front-end | `0db2723c` |
| Java | `82b4e691` |
| Go-To-Market | `4ab07e09` |
| FE and BE | `227314e6` |

## Field: Priority

Field ID: `PVTSSF_lADOAA9Wz84AKDq_zg8SfMQ`

| Option | Option ID |
|---|---|
| Low | `2fd28bd8` |
| Medium | `2d9f80db` |
| High | `6baeb606` |
| Critical | `25ea92ea` |

---

## Sprint / Iteration Fields (team-specific)

Each team has a dedicated iteration field named `{Team Name} Sprint` (e.g., `Enablement Sprint`, `Falcon Sprint`, `Scout Sprint`). These are GitHub Project V2 **iteration fields** — distinct from the Status single-select field — and hold the actual sprint schedule.

### Known sprint field IDs (stable — one per team, does not change per sprint)

| Team | Field name | Field ID |
|---|---|---|
| Falcon | Falcon Sprint | `PVTIF_lADOAA9Wz84AKDq_zgcwTUQ` |
| Maintenance | Maintenance Sprint | `PVTIF_lADOAA9Wz84AKDq_zgIGutk` |
| Scout | Scout Sprint | `PVTIF_lADOAA9Wz84AKDq_zgdo3T0` |
| Platform | Platform Sprint | `PVTIF_lADOAA9Wz84AKDq_zgj83DM` |

> Teams not listed (Enablement, Modernization, Security, UX, Architecture, etc.) do not have sprint fields in Project #7 — they use status-only or kanban workflows.

### Discover all sprint fields and their iterations

```bash
gh api graphql -f query='
  {
    node(id: "PVT_kwDOAA9Wz84AKDq_") {
      ... on ProjectV2 {
        fields(first: 50) {
          nodes {
            ... on ProjectV2IterationField {
              id
              name
              configuration {
                iterations { id title startDate duration }
                completedIterations { id title startDate duration }
              }
            }
          }
        }
      }
    }
  }'
```

Each iteration object has: `id`, `title` (e.g. "Sprint 42"), `startDate` (YYYY-MM-DD), `duration` (days).

### Derive the team's sprint field name

Strip the label prefix: `Team : Enablement` → field name is `Enablement Sprint`.

### Identify current / next / previous sprint

From the field's `configuration`:

| User intent | Which iteration |
|---|---|
| "current sprint" / "this sprint" | `iterations[]` where `startDate` ≤ today AND `startDate + duration days` > today |
| "next sprint" | `iterations[]` where `startDate` > today, earliest one |
| "last sprint" / "previous sprint" | `completedIterations[]`, most recent by `startDate` |
| Specific name (e.g. "Sprint 42") | Match `title` exactly in `iterations[]` or `completedIterations[]` |

### Filter issues by status (fast — flat structure)

`gh issue list --json projectItems` returns a flat object per project with `status.name` directly:

```json
{ "title": "dotCMS - Product Planning", "status": { "optionId": "2d61e9f1", "name": "In Progress" } }
```

Filter by status value:
```bash
gh issue list --repo dotCMS/core \
  --label "Team : Enablement" \
  --state open \
  --json number,title,url,updatedAt,projectItems \
  --limit 100 | jq --arg status "In Progress" '
  [.[] |
    . as $issue |
    ((.projectItems // []) | map(select(.title == "dotCMS - Product Planning")) | first // {}) as $proj |
    select($proj.status.name == $status) |
    {number: $issue.number, title: $issue.title, url: $issue.url, status: $proj.status.name, updatedAt: $issue.updatedAt}
  ] | sort_by(.updatedAt) | reverse'
```

### Filter issues by sprint iteration (GraphQL — full fieldValues)

Sprint/iteration field values are **not** included in `gh issue list --json projectItems`. Use GraphQL with `orderBy: UPDATED_AT DESC` and `states: [OPEN, CLOSED]` — Done issues are closed, and teams may have 1000+ issues so ordering by recency is required to stay within the 100-item limit.

> **Critical:** Do NOT use `gh issue list` for sprint queries. It returns the oldest 100 issues by default and misses recent Done/closed items entirely.

```bash
# Replace TEAM_LABEL, SPRINT_TITLE, SPRINT_FIELD with actual values
# e.g. "Team : Scout", "Sprint 4: Feb 24, 2026", "Scout Sprint"
gh api graphql -f query='
  {
    repository(owner: "dotCMS", name: "core") {
      issues(first: 100, labels: ["TEAM_LABEL"], states: [OPEN, CLOSED],
             orderBy: {field: UPDATED_AT, direction: DESC}) {
        nodes {
          number title url state
          assignees(first: 5) { nodes { login } }
          updatedAt
          projectItems(first: 5) {
            nodes {
              project { number }
              fieldValues(first: 20) {
                nodes {
                  ... on ProjectV2ItemFieldSingleSelectValue {
                    name field { ... on ProjectV2SingleSelectField { name } }
                  }
                  ... on ProjectV2ItemFieldIterationValue {
                    title field { ... on ProjectV2IterationField { name } }
                  }
                }
              }
            }
          }
        }
      }
    }
  }' | jq --arg sprint "SPRINT_TITLE" --arg fieldName "SPRINT_FIELD" '
  [.data.repository.issues.nodes[] |
    . as $issue |
    ((.projectItems.nodes // []) | map(select(.project.number == 7)) | first) as $proj |
    select($proj) |
    (($proj.fieldValues.nodes // []) | map(select(.field.name == $fieldName)) | first | .title // null) as $iterTitle |
    (($proj.fieldValues.nodes // []) | map(select(.field.name == "Status")) | first | .name // null) as $status |
    select($iterTitle == $sprint) |
    {
      number: $issue.number,
      title: $issue.title,
      url: $issue.url,
      state: $issue.state,
      status: ($status // "—"),
      assignees: [$issue.assignees.nodes[].login],
      updatedAt: $issue.updatedAt
    }
  ] | sort_by(.status, .updatedAt) | reverse'
```

> **jq shell-quoting hazard:** The `!=` operator is escaped as `\!=` in bash heredocs, causing a compile error. Use chained `select()` calls instead — e.g. `select($x) | select($x == $y)` rather than `select($x != null)`. Also avoid using the same name for a jq `--arg` variable and an internal variable (e.g. don't use `$sprint` as both `--arg sprint` and as an internal binding — use `$iterTitle` for the internal one).

---

## Team Project Views

Each team has a dedicated view in Project #7. View URLs follow this pattern:

```
https://github.com/orgs/dotCMS/projects/7/views/N
```

### Known team view numbers

| Team label | View # | URL |
|---|---|---|
| Team : Falcon | 4 | https://github.com/orgs/dotCMS/projects/7/views/4 |
| Team : Scout | 5 | https://github.com/orgs/dotCMS/projects/7/views/5 |
| Team : UX | 49 | https://github.com/orgs/dotCMS/projects/7/views/49 |
| Team : Modernization | 65 | https://github.com/orgs/dotCMS/projects/7/views/65 |
| Team : Enablement | 72 | https://github.com/orgs/dotCMS/projects/7/views/72 |

> Teams not listed above (Maintenance, Platform, Architecture, Lunik, Security, Cloud Eng, 3rd Party) do not have a dedicated team view in Project #7 as of 2026-02-27.
>
> Notable non-team views: All Issues (1), Triage - New (12), Roadmap (21), Show Stoppers (30), Sprint Report Card (56), Technical Debt (71).

### Discover all view numbers

```bash
gh api graphql -f query='
  {
    organization(login: "dotCMS") {
      projectV2(number: 7) {
        views(first: 30) {
          nodes {
            number
            name
          }
        }
      }
    }
  }' | jq '.data.organization.projectV2.views.nodes[] | {number, name}'
```

### Derive a team's view URL

When a team view number is known, the URL can be constructed directly. In FIND mode, include this URL at the top of team-scoped results so the user can open the full board view alongside the filtered list.

If the view number for a team is not yet in this reference, run the discovery query above once and record it here.

---

## Known Team Labels

```
Team : Enablement
Team : Falcon
Team : Maintenance
Team : Scout
Team : Platform
Team : Architecture
Team : Lunik
Team : Cloud Eng
Team : Modernization
Team : Security
Team : UX
Team : 3rd Party
```

If a team is not in this list, verify it exists before applying:
```bash
gh label list --repo dotCMS/core --limit 1000 | grep "^Team :"
```

---

## Type Label Taxonomy

Apply one `Type :` label per issue. Select based on template + description keywords.

```
Type : Defect
Type : Task
Type : Spike
Type : New Functionality
Type : Refactoring
Type : CI/CD
Type : Documentation
Type : Test Automation
Type : Technical Design
Type : Visual Design
Type : Research
Type : Question
Type : Manual Testing
Type : Copy
```

### Selection mapping

| Template | Default | Refine on keywords |
|---|---|---|
| Defect | `Type : Defect` | — |
| Spike | `Type : Spike` | — |
| Task | `Type : Task` | "refactor" → `Type : Refactoring`; "CI/CD / pipeline / workflow" → `Type : CI/CD`; "docs / documentation" → `Type : Documentation`; "test automation / e2e" → `Type : Test Automation`; "technical design / ADR" → `Type : Technical Design` |
| Feature | `Type : New Functionality` | — |
| Epic | `Type : New Functionality` | — |
| UX | `Type : Visual Design` | — |

---

## Native GitHub Issue Types

Set via REST PATCH immediately after `gh issue create`. This is GitHub's first-class type system — separate from labels.

| Template | Native type name | Type ID (reference only) |
|---|---|---|
| Defect | `Bug` | 156992 |
| Task | `Task` | 156989 |
| Spike | `Spike` | 26128527 |
| Feature | `Feature` | 156996 |
| Epic | `Epic` | 26685397 |
| UX | `Task` | 156989 |
| Pillar | `Pillar` | 28738322 |

The name string is all that's needed for the PATCH call — ID is for reference only.