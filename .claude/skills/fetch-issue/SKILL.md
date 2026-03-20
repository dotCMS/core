---
name: fetch-issue
description: Fetch a GitHub issue from dotCMS/core and check for existing work (branches and PRs). Pass an issue number, or omit to detect from current branch name.
allowed-tools: Bash(gh issue view:*), Bash(gh issue edit:*), Bash(gh pr list:*), Bash(git branch:*)
---

# Fetch Issue

Arguments: `$ARGUMENTS` — extract the issue number.

## Step 1 — Resolve number

If no number in `$ARGUMENTS`, check current branch:

```bash
git branch --show-current
```

Extract using these patterns (stop at first match):

| Priority | Pattern | Example |
|---|---|---|
| 1 | `issue-(\d+)` | `issue-34823-feature` → `34823` |
| 2 | Branch starts with digits | `34823-feature` → `34823` |
| 3 | Digits after `/` or `-` | `feat/34823-something` → `34823` |

If still no number: ask — `"Which issue number are you working on?"`

## Step 2 — Fetch

```bash
gh issue view <number> --repo dotCMS/core --json number,title,body,labels,assignees,url
```

If not found: `"Issue #<number> not found in dotCMS/core. Verify the number and try again."`

## Step 3 — Check for existing work

```bash
gh pr list --repo dotCMS/core --search "issue-<number>" --state open --json number,title,url
git branch -a | grep "issue-<number>"
```

- **Open PR found** → show URL, ask: `resume / create new branch / cancel`
- **Local branch found, no PR** → ask: `reuse / create new branch`
- **Nothing found** → continue

## Output

Return `number`, `title`, `body`, `labels`, `url`, and `existing_branch` (if any) — these are carried into all subsequent steps.
