---
name: skill-doctor
description: Use when a repo skill fails, produces errors, gives wrong instructions, or references stale information. Also use to manage local skill overrides, check skill staleness, or optimize skill token usage and quality.
---

# Skill Doctor

Diagnose issues with repository skills, manage local overrides, and optimize skill quality.

**Scope:** Skills in the current repository's `.claude/skills/` directory only.

## Routing

Parse `$ARGUMENTS`:

| Pattern | Mode |
|---------|------|
| (empty) or `<skill-name>` | **diagnose** |
| `--optimize <skill>` | **optimize** |
| `--manage [skill]` | **manage** (status) |
| `clone/diff/sync/revert <skill>` | **manage** (lifecycle) |
| `report/gist/pr <skill>` | **deliver** |
| `resolve <skill>` | **resolve** |

Unrecognized input: show this table and ask to rephrase. Then **read `./reference.md`** for detailed procedures.

## Manage

Lifecycle: clone -> edit -> diff -> sync/revert -> pr/gist. Local overrides live at `~/.claude/skills/<name>/` with `.skill-origin.json` tracking.

## Diagnose

1. **Identify** skill, validate it exists
2. **Classify**: `invalid_command` | `wrong_assumption` | `stale_info` | `other`
3. **Investigate** -- reproduce, root cause, determine fix, check related issues. Quality gate: confirmed problem + root cause + suggested fix
4. **Duplicate check** in Discussion thread. Match found: upvote, add context, or report anyway
5. **Route**: local override -> fix inline, stop. Repo -> offer clone-and-fix or report-only
6. **Scrub** PII, **review** with developer, **deliver** to GitHub Discussion + Slack

## Optimize

Checks frontmatter, token efficiency (<500 words), structure quality, and staleness (file paths, commands, flags). Scored report with fixes. Local overrides fixed directly; repo versions cloned first.

## Deliver & Resolve

**Deliver**: GitHub Discussion (primary, one thread per skill) + Slack `#log-skill-feedback` (optional notification). Check prerequisites first (gh auth, repo access). See reference.md.

**Resolve**: After fix PR merges, mark Discussion reports resolved, post Slack summary, offer to lock thread.

## Common Mistakes

| Mistake | Fix |
|---------|-----|
| Sending reports without investigating root cause | Quality gate: must have confirmed problem + root cause + suggested fix |
| Sending duplicate reports | Always check Discussion thread for existing reports first |
| Forgetting to check prerequisites before delivery | Run `gh auth status` and repo access check before any delivery action |
| Using `--manage` on detached HEAD | Falls back to HEAD if main branch ref doesn't exist |
| Cloning when manual directory already exists | Checks for `.skill-origin.json` to distinguish tracked vs manual overrides |
| Running `revert` without reviewing changes | Always shows diff and requires explicit name confirmation before `rm -rf` |

## Example: Diagnose Flow

```
Developer: /skill-doctor review

1. Validate: .claude/skills/review/ exists (repo version, no local override)
2. Classify: invalid_command (gh pr diff --name-only doesn't exist)
3. Investigate:
   - Reproduce: gh pr diff --name-only -> "unknown flag: --name-only"
   - Root cause: SKILL.md line 42 uses --name-only, correct flag is --name-status
   - Fix: Change "--name-only" to "--name-status" on line 42
   - Related: line 58 also uses --name-only in a different context
4. Duplicate check: No Discussion thread exists -> first report
5. Route: Repo version -> offer clone-and-fix or report-only
6. Scrub: Replace /Users/stevebolton/ with ~/
7. Developer reviews scrubbed report -> Send
8. Post to GitHub Discussion + Slack notification
```

## Override Warning

When ANY skill is invoked and `~/.claude/skills/<name>/.skill-origin.json` exists, output before skill content:

```
SKILL OVERRIDE ACTIVE: <name>
  Using: ~/.claude/skills/<name>/ (local)
  Instead of: .claude/skills/<name>/ (repo)
  Cloned from: <sha> | Repo is N commits ahead
  Run /skill-doctor --manage <name> for options
```