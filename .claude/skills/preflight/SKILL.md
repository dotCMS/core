---
name: preflight
description: Pre-flight safety checks before any dotCMS/core development workflow. Verifies gh auth, clean working tree, and correct repository. Run this first.
allowed-tools: Bash(gh --version:*), Bash(gh auth status:*), Bash(gh repo view:*), Bash(git status:*)
---

# Preflight

Run these checks in order. Stop at the first failure.

```bash
gh --version && gh auth status
gh repo view --json nameWithOwner --jq '.nameWithOwner'
git status --porcelain
```

## Pass conditions

| Check | Pass | Fail |
|---|---|---|
| `gh auth status` | Logged in | Stop: `"Run gh auth login first."` |
| Repo | `dotCMS/core` | Stop: `"Wrong repo — expected dotCMS/core."` |
| Working tree | Empty output | Stop: `"Commit or stash changes before starting."` |

## Output on success

```
✅ Preflight passed — gh authenticated, repo dotCMS/core, working tree clean.
```
