# GitHub API Reference — dotcms-github-issues skill

Exact `gh` commands for all operations. Replace placeholders in ALL_CAPS.

---

## A. Set native GitHub Issue Type

Call immediately after every `gh issue create`. The `--type` flag does not exist on `gh issue create` — this PATCH is mandatory.

```bash
gh api repos/dotCMS/core/issues/ISSUE_NUM -X PATCH -f type='TYPE_NAME'
# TYPE_NAME: Bug | Task | Spike | Feature | Epic | Pillar
```

Verify:
```bash
gh api repos/dotCMS/core/issues/ISSUE_NUM --jq '.type.name'
```

---

## B. Get issue's project item ID

Required before any field mutation. Returns the item ID for Project #7.

```bash
gh api graphql -f query='
  { repository(owner:"dotCMS", name:"core") {
      issue(number:ISSUE_NUM) {
        projectItems(first:10) {
          nodes { id project { number } }
        }
      }
  } }' \
  | jq -r '.data.repository.issue.projectItems.nodes[]
            | select(.project.number==7) | .id'
```

---

## C. Set a single-select project field (Status / Technology / Priority)

Requires: project item ID (from B), field ID and option ID (from [project-fields.md](project-fields.md)).

> **Important:** Do NOT use GraphQL variables (`$projId:ID!` etc.) — the `!` in `ID!` is
> backslash-escaped by Claude Code's Bash tool, causing a parse error. Inline all values
> directly as string literals instead.

```bash
gh api graphql -f query='mutation { updateProjectV2ItemFieldValue(input:{projectId:"PVT_kwDOAA9Wz84AKDq_" itemId:"ITEM_ID" fieldId:"FIELD_ID" value:{singleSelectOptionId:"OPTION_ID"}}) { projectV2Item { id } } }'
```

**Example — set Technology to Java:**
```bash
# 1. Get item ID
ITEM_ID=$(gh api graphql -f query='
  { repository(owner:"dotCMS", name:"core") {
      issue(number:ISSUE_NUM) {
        projectItems(first:10) {
          nodes { id project { number } }
        }
      }
  } }' \
  | jq -r '.data.repository.issue.projectItems.nodes[]
            | select(.project.number==7) | .id')

# 2. Set Technology = Java (inline all IDs — no variables)
gh api graphql -f query='mutation { updateProjectV2ItemFieldValue(input:{projectId:"PVT_kwDOAA9Wz84AKDq_" itemId:"'"$ITEM_ID"'" fieldId:"PVTSSF_lADOAA9Wz84AKDq_zgH5iBk" value:{singleSelectOptionId:"82b4e691"}}) { projectV2Item { id } } }'
```

---

## D. Get issue database ID

Required for sub-issues endpoints (which use integer DB ID, not display number).

```bash
gh api repos/dotCMS/core/issues/ISSUE_NUM --jq '.id'
```

---

## E. Add sub-issue (child under parent)

```bash
# Step 1: get child's database ID
CHILD_DB_ID=$(gh api repos/dotCMS/core/issues/CHILD_NUM --jq '.id')

# Step 2: attach as sub-issue of parent
gh api repos/dotCMS/core/issues/PARENT_NUM/sub_issues \
  -X POST \
  -H "Accept: application/vnd.github+json" \
  -F sub_issue_id=$CHILD_DB_ID
```

Note: `sub_issue_id` is the integer database ID, not the display number.

---

## F. List sub-issues of an issue

```bash
gh api repos/dotCMS/core/issues/ISSUE_NUM/sub_issues
```

---

## G. Remove a sub-issue

```bash
# Get child's database ID first (Section D)
CHILD_DB_ID=$(gh api repos/dotCMS/core/issues/CHILD_NUM --jq '.id')

gh api repos/dotCMS/core/issues/PARENT_NUM/sub_issues/$CHILD_DB_ID \
  -X DELETE \
  -H "Accept: application/vnd.github+json"
```

---

## H. Full issue state query (QUERY mode)

```bash
# Main issue data including project items
gh issue view ISSUE_NUM --repo dotCMS/core \
  --json number,title,state,labels,assignees,body,projectItems,url

# Native issue type
gh api repos/dotCMS/core/issues/ISSUE_NUM --jq '.type.name'

# Sub-issues
gh api repos/dotCMS/core/issues/ISSUE_NUM/sub_issues

# Dependencies summary (if available)
gh api repos/dotCMS/core/issues/ISSUE_NUM --jq '.issue_dependencies_summary'
```

To extract Project #7 field values from the `projectItems` JSON:
```bash
gh issue view ISSUE_NUM --repo dotCMS/core --json projectItems \
  | jq '.projectItems[] | select(.project.number==7) | .fieldValues'
```