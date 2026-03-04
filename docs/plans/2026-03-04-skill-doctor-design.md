# skill-doctor: Skill Feedback, Override Management & Optimization

**Date:** 2026-03-04
**Author:** Steve Bolton
**Status:** Design approved, pending implementation

## Problem

Skills checked into the dotcms/core repository (`.claude/skills/`) can have issues:
- Invalid shell commands that don't work on all environments
- Incorrect assumptions about the codebase or tooling
- Stale information that no longer matches the current source
- Token inefficiency or poor structure

There is no structured way to:
1. Detect and report these issues during normal usage
2. Safely iterate on fixes locally before committing
3. Track when local overrides diverge from the repo version
4. Deliver feedback to maintainers without context-switching

## Scope

**In scope:** Skills in the current repository's `.claude/skills/` directory only.
**Out of scope (for now):** Marketplace plugin skills (superpowers, etc.).

## Design

### Three Modes

| Mode | Trigger | Purpose |
|------|---------|---------|
| **Diagnose** (default) | `/skill-doctor` or auto-detect via hook | Identify issues, report or fix locally |
| **Optimize** | `/skill-doctor --optimize <skill>` | Analyze quality, suggest improvements, reduce tokens |
| **Manage** | `/skill-doctor --manage [skill]` | Clone, diff, sync, revert local overrides |

### Skill Resolution Order

Claude Code loads skills from multiple locations. When a skill exists in both:

1. `~/.claude/skills/<name>/` — **local override, takes precedence**
2. `<repo>/.claude/skills/<name>/` — **canonical repo version**

The local override applies across all branches and worktrees since it lives outside the repo.

### Local Override Lifecycle

#### Cloning a Skill for Local Editing

```
/skill-doctor clone triage
```

This copies `.claude/skills/triage/` to `~/.claude/skills/triage/` and creates a `.skill-origin.json` tracking file:

```json
{
  "source_repo": "dotcms/core",
  "source_path": ".claude/skills/triage",
  "cloned_from_sha": "35c71f1fa8f",
  "cloned_at": "2026-03-04T14:30:00Z",
  "cloned_by": "stevebolton"
}
```

#### Edit Mode Warning

When a local override is active and the skill is invoked, the developer sees:

```
SKILL OVERRIDE ACTIVE: triage
  Using: ~/.claude/skills/triage/ (local)
  Instead of: .claude/skills/triage/ (repo)
  Cloned from: 35c71f1 | Repo is 3 commits ahead
  Run /skill-doctor --manage triage for options
```

This makes it clear that:
- The developer is using a local version
- Others will not see these changes until committed
- The repo version may have been updated since the clone

#### Completing an Override

When done editing, the developer chooses:

| Action | Command | Result |
|--------|---------|--------|
| **Report only** | `/skill-doctor report triage` | Feedback posted to GitHub Discussion + Slack (no code) |
| **Create gist** | `/skill-doctor gist triage` | Diff of changes shared as GitHub Gist |
| **Create PR** | `/skill-doctor pr triage` | Branch + PR with changes applied to repo skill |
| **Revert** | `/skill-doctor revert triage` | Delete local override, revert to repo version |

### Staleness Detection

On skill invocation or `/skill-doctor --manage`:

1. **Repo vs main:** `git diff main -- .claude/skills/<name>/` detects if the current branch has skill changes not yet on main
2. **Local vs repo:** Compare `.skill-origin.json` commit SHA against current repo HEAD for that path. Warn if repo has newer commits affecting the skill.

### Manage Mode Status

```
/skill-doctor --manage
```

Output:

```
Repo Skills (.claude/skills/):

  cicd-diagnostics  LOCAL OVERRIDE (diverged)
                    Local: ~/.claude/skills/cicd-diagnostics/
                    Cloned from: 35c71f1 (2026-02-25)
                    Repo HEAD:   52ceaed (2026-03-03) - 6 commits ahead
                    Local edits: SKILL.md (625 lines removed)

  triage            Up to date with main

  create-issue      Up to date with main

  sdk-analytics     Up to date with main

  review            Branch has uncommitted skill changes
```

### Diagnose Mode (Default)

#### Auto-Detection (PostToolUse Hook)

Watches for signals that a skill caused a problem:
- Bash command exits with non-zero code after a skill was invoked
- Same command retried 3+ times
- Tool call errors or rejections

When detected:

```
I noticed a potential skill issue. Would you like me to capture feedback?
[Yes] [No]
```

#### Manual Invocation

```
/skill-doctor                    # Diagnose last skill issue
/skill-doctor triage             # Diagnose specific skill
```

#### Context Gathering

Collects:
- Skill name, version (git SHA), source path
- Issue type: `invalid_command` | `wrong_assumption` | `stale_info` | `other`
- Failed command + exit code + stderr (truncated)
- What was attempted vs what happened
- Relevant SKILL.md excerpt
- Environment (OS, shell)

#### Content Scrubbing

Scrubs the **report body content** only (not the reporter identity — reports are posted as the developer using their own credentials):

**Auto-scrub patterns:**
- API keys/tokens (`sk-*`, `ghp_*`, `Bearer *`) -> `[REDACTED_TOKEN]`
- Home directory paths (`/Users/username/`) -> `~/`
- Environment variable values -> `[ENV:VAR_NAME]`
- IP addresses -> `[IP_REDACTED]`
- `.env` file contents -> `[REDACTED]`

**Mandatory human review:** Developer sees the scrubbed report and can edit before sending.

#### Delivery

Reports are posted using the **developer's own credentials**:

1. **GitHub Discussion** (via `gh api`): Full structured report as a comment on the skill's discussion thread in `dotcms/core` under the "Skill Feedback" category. One thread per skill.
2. **Slack** (via Slack MCP): Summary + link to Discussion comment posted to `#skill-feedback`.

#### Report Format

```markdown
## Skill Feedback Report

**Skill:** `triage` (.claude/skills/triage/)
**Type:** `invalid_command`
**Date:** 2026-03-04
**Repo:** dotcms/core @ 35c71f1

### What happened
Skill instructed to run `gh project item-list` but the command syntax
has changed in gh CLI v2.44+. The `--format` flag is no longer supported.

### Failed command
gh project item-list 65 --owner dotcms --format json --jq '.items[]'

Exit code: 1
Stderr: unknown flag: --format

### Expected behavior
Should use `gh project item-list 65 --owner dotcms --json id,title`

### Relevant skill excerpt
> Use `gh project item-list` with `--format json` to fetch project items

### Environment
- OS: darwin (Darwin 25.2.0)
- Shell: zsh
- gh version: 2.47.0
```

### Optimize Mode

```
/skill-doctor --optimize triage
```

Analyzes the skill against writing-skills best practices:

1. **Token efficiency:** Word count, redundant content, sections that could be cross-referenced
2. **Structure:** Description starts with "Use when..."? Under 500 words? Flowcharts only for decisions?
3. **Quality:** One excellent example vs many mediocre? Concrete triggers in description?
4. **Staleness:** References to files/commands that no longer exist in the codebase

Presents suggestions. For local overrides, offers to apply fixes directly.

### Credential Model

- **GitHub:** Uses developer's `gh` CLI authentication (already configured)
- **Slack:** Uses developer's Slack MCP connection (already configured)
- **No service accounts or shared credentials**
- Reports are authored as the developer — this is intentional and expected

### Privacy Model

- Reporter identity is visible (posted with their credentials)
- Report body content is scrubbed of secrets, tokens, PII patterns
- Developer reviews and approves every report before it's sent
- Three consent points: (1) agree to capture, (2) review content, (3) confirm send

## Setup Requirements (One-Time)

1. **GitHub:** Create "Skill Feedback" discussion category on `dotcms/core` (admin, manual)
2. **Slack:** Use `#log-skill-feedback` channel
3. **Skill:** Install `skill-doctor` to `~/.claude/skills/skill-doctor/`
4. **Convention:** `.skill-origin.json` files in local overrides (created automatically by clone command)

## Implementation Components

1. **SKILL.md** — Main skill document with all three modes
2. **PostToolUse hook** — Auto-detection of skill failures
3. **Shell helpers** — `clone.sh`, `diff.sh`, `sync.sh`, `revert.sh` for override management
4. **PII scrubber** — Pattern-based content scrubbing (inline in skill or small script)
5. **Report templates** — Markdown templates for Discussion and Slack formats

## Open Questions

1. Should `/skill-doctor --manage` run automatically on session start to warn about stale overrides?
2. Should the hook watch ALL Bash failures or only those within N seconds of a skill invocation?
3. Should optimize mode also check for security issues (hardcoded paths, unsafe patterns)?