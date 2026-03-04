# Commands Reference

All commands run from the `core-web/` directory.

---

## Detecting the Nx project name

Find the nearest `project.json` walking up from the modified file's directory. Read the `"name"` field — use it as `<project>` in all nx commands.

---

## Tests

```bash
yarn nx test <project>                                    # Run all tests for project
yarn nx test <project> --testPathPattern=<file-or-regex>  # Run specific test file
yarn nx test <project> --testNamePattern="<test name>"    # Run tests matching name
yarn nx affected:test                                     # Only projects affected by changes
```

## Lint

```bash
yarn nx lint <project>                                    # Check lint errors
yarn nx lint <project> --fix                              # Auto-fix lint errors
```

## Format

```bash
yarn nx format:write                                      # Apply Prettier to all changed files
yarn nx format:check                                      # Verify format (no write)
```

## Build

```bash
yarn nx build <project>                                   # Build project
yarn nx affected:build                                    # Only build affected projects
yarn nx run dotcms-ui:serve                               # Dev server
```

---

## Git

```bash
git checkout main && git pull origin main
BRANCH=$(bash .claude/skills/solve-issue/scripts/slugify.sh <number> "<title>")
git checkout -b "$BRANCH"
git add <specific-files>
git commit --no-verify -m "<type>(<scope>): <message>"
git push -u origin "$BRANCH"
git status --porcelain                                    # Clean check (empty = clean)
git branch -a | grep "issue-<number>"                     # Find existing branches
```

---

## gh CLI

```bash
gh issue view <number> --repo dotCMS/core --json number,title,body,labels,assignees,url
gh issue edit <number> --repo dotCMS/core --add-assignee @me
gh pr create --draft --title "<title>" --body "<body>"
gh pr ready <pr-number>
gh pr list --repo dotCMS/core --search "issue-<number>" --state open --json number,title,url
gh pr view <pr-number> --repo dotCMS/core --json isDraft --jq '.isDraft'
```

---

## Project Board (Step 10f)

Move issue to "In Review" after PR is marked ready:

```bash
# Find project item ID
gh api graphql -f query='{
  repository(owner: "dotCMS", name: "core") {
    issue(number: <number>) {
      projectItems(first: 5) {
        nodes {
          id
          project { id title }
          fieldValues(first: 10) {
            nodes {
              ... on ProjectV2ItemFieldSingleSelectValue {
                name
                field { ... on ProjectV2SingleSelectField { name id } }
              }
            }
          }
        }
      }
    }
  }
}'

# Update status to "In Review"
gh api graphql -f query='mutation {
  updateProjectV2ItemFieldValue(input: {
    projectId: "<project-id>"
    itemId: "<item-id>"
    fieldId: "<status-field-id>"
    value: { singleSelectOptionId: "<in-review-option-id>" }
  }) {
    projectV2Item { id }
  }
}'
```
