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
| `list` | **list** (all repo skills) |
| `info <skill>` | **list** (detail for one skill) |
| `feedback [skill] "<message>"` | **feedback** (quick note) |
| (empty) or `<skill-name>` | **diagnose** |
| `--optimize <skill>` | **optimize** |
| `--manage [skill]` | **manage** (status) |
| `clone/diff/sync/revert <skill>` | **manage** (lifecycle) |
| `report/gist/pr <skill>` | **deliver** |
| `resolve <skill>` | **resolve** |

Unrecognized input: show this table and ask to rephrase. Then **read `./reference.md`** for detailed procedures.

## List

Dynamically discovers repo skills by reading `.claude/skills/*/SKILL.md`. Shows name, description, invocation, and override status. `info <skill>` reads the full SKILL.md to provide detailed usage guidance. All information comes from the skill files themselves — nothing is hardcoded. See reference.md for details.

## Feedback

Lightweight path for quick notes. No investigation or quality gate — just scrub, preview, confirm, post. If skill name is omitted, ask which skill (or use the most recently invoked skill if known from conversation context). See reference.md for details.

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

See reference.md for the full table. Key rules:
- Never send reports without root cause + suggested fix
- Always check Discussion thread for duplicates before reporting
- Always run `gh auth status` before any delivery action
- `revert` always shows diff and requires name confirmation

## Override Warning

When ANY skill is invoked and `~/.claude/skills/<name>/.skill-origin.json` exists, output before skill content:

```
SKILL OVERRIDE ACTIVE: <name>
  Using: ~/.claude/skills/<name>/ (local)
  Instead of: .claude/skills/<name>/ (repo)
  Cloned from: <sha> | Repo is N commits ahead
  Run /skill-doctor --manage <name> for options
```