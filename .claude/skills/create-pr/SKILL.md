---
name: create-pr
description: Commit, push, and create a draft PR for a dotCMS issue. Moves the linked issue to "In Review" on the project board when the PR is marked ready. Requires confirmed_acs, coder_brief, review summary, and QA results as input.
allowed-tools: Bash(git add:*), Bash(git commit:*), Bash(git push:*), Bash(gh pr create:*), Bash(gh pr ready:*), Bash(gh pr view:*), Bash(gh api graphql:*)
---

# Create PR

Input: `number`, `branch_name`, `confirmed_acs`, `coder_brief`, `review_summary`, `qa_summary`.

---

## Step 1 — Commit

```bash
git add <changed-files>
git commit --no-verify -m "<type>(<scope>): <description>

Closes #<number>"
```

Types: `feat` · `fix` · `chore` · `test` · `refactor` · `docs` · `style`

---

## Step 2 — Push

```bash
git push -u origin <branch_name>
```

---

## Step 3 — Create draft PR

Ask: **"Create a draft PR? yes / no"** — stop if no.

```bash
gh pr create --draft \
  --title "<type>(<scope>): <description>" \
  --body "$(cat <<'EOF'
## Proposed Changes
- <bullet list from coder_brief>

## Acceptance Criteria
<confirmed_acs as checkbox list>

## Test Coverage
<N> test cases covering <N> ACs.

## Review Summary
<review_summary>

## QA Results
<qa_summary — or "QA skipped" if not run>

## Checklist
- [x] Tests added/updated per ACs
- [ ] Translations required
- [ ] Security implications considered

## Assumptions Made
<from coder_brief — or "None">

Closes #<number>
EOF
)"
```

---

## Step 4 — Mark ready

After all review and QA loops are complete:

```bash
gh pr ready <pr-number>
```

---

## Step 5 — Move issue to "In Review"

```bash
gh pr view <pr-number> --repo dotCMS/core --json isDraft --jq '.isDraft'
```

If not draft: use the GraphQL commands in `.claude/skills/solve-issue/references/commands.md` (section "Project Board") to move the issue to "In Review".

If the issue is not on any project board, skip and note it to the user.

---

## Output

```
✅ PR #<pr-number> ready: <url>
Branch: <branch_name>
Closes #<number>
```
