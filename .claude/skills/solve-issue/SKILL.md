---
name: solve-issue
description: Automates the full development cycle from a GitHub Issue number for the dotCMS/core repository. Orchestrates AC refinement, implementation, review, QA, and PR creation. Use when the user asks to solve, fix, implement, or work on a GitHub issue by number (e.g. "solve issue 34353", "work on #34353", "implement issue 34353"). Do NOT use for non-GitHub tasks, general coding questions, or issues from repositories other than dotCMS/core.
allowed-tools: Bash(gh:*), Bash(git:*), Bash(yarn:*), Bash(bash .claude/skills/solve-issue/scripts/slugify.sh:*), Bash(cd:*), Bash(node:*), Bash(npm pack:*), Read, Glob, Grep, Edit, Write, Agent
---

# Solve Issue

You are the solve-issue orchestrator. Execute these steps in order.

Arguments: `$ARGUMENTS` — extract the issue number and optional flags.

```
Issue → pre-flight → resolve → fetch → existing work → AC refinement → branch → implement → tests → review → PR
```

---

## Step 1 — Pre-flight

```bash
gh --version && gh auth status
gh repo view --json nameWithOwner --jq '.nameWithOwner'
git status --porcelain
```

Stop if: repo is not `dotCMS/core` · working tree is dirty.

---

## Step 2 — Resolve issue number

Parse `$ARGUMENTS` for a number. If `--review` flag is present, jump to Step 9.

If no number found, check current branch:

```bash
git branch --show-current
```

Extract issue number using these patterns in order (stop at first match):

| Priority | Pattern | Example |
|---|---|---|
| 1 | `issue-(\d+)` | `issue-34823-feature` → `34823` |
| 2 | Branch starts with digits | `34823-feature` → `34823` |
| 3 | Digits after `/` or `-` | `feat/34823-something` → `34823` |

If still no number: ask the user — "Which issue are you working on?"

---

## Step 3 — Fetch the issue

```bash
gh issue view <number> --repo dotCMS/core --json number,title,body,labels,assignees,url
```

Save `title`, `body`, `labels` — carried through all steps.

---

## Step 4 — Check for existing work

```bash
gh pr list --repo dotCMS/core --search "issue-<number>" --state open --json number,title,url
git branch -a | grep "issue-<number>"
```

- **Open PR found** → show URL, ask: `resume / create new branch / cancel`
- **Local branch found** → ask: `reuse / create new branch`

---

## Step 5 — AC Refinement

**Do not proceed to Step 6 until `confirmed_acs` is approved.**

Execute the process from `references/issue-refinement.md` directly (do NOT delegate to a sub-agent — the flow uses `AskUserQuestion` which requires the orchestrator).

Input: issue `title`, `body`, `labels` from Step 3.

### Fast path (well-defined issue)

Run the Decompose and Ambiguity Scan phases. If **no CRITICAL or MAJOR ambiguities** are found, write ACs directly, score them, show to user and ask: `"Proceed with these ACs? (yes / edit)"`. No clarification loop needed.

### Slow path (vague issue)

If CRITICAL or MAJOR ambiguities are found, enter the full clarification loop: ask questions via `AskUserQuestion` with concrete options → re-analyze → loop until resolved or max 3 rounds. Then write ACs with clarity score.

**Loop control:** max 3 rounds · minimum clarity score 80/100. If score < 80 on loop 3, flag gaps as `⚠ UNRESOLVED` and proceed.

- `yes` → save as `confirmed_acs`
- `edit` → accept inline edits, save as `confirmed_acs`

---

## Step 6 — Branch + assign

```bash
gh issue edit <number> --repo dotCMS/core --add-assignee @me
git checkout main && git pull origin main
BRANCH=$(bash .claude/skills/solve-issue/scripts/slugify.sh <number> "<title>")
git checkout -b "$BRANCH"
git branch --show-current
```

If already on an `issue-<number>-*` branch (from Step 4 reuse), skip branch creation.

---

## Step 7 — Implement

### 7a. Present implementation plan

Show a plan derived from `confirmed_acs` — which files to create or modify and why, mapped per AC.

Ask: `"Approve plan? (yes / no / edit)"`

### 7b. Load relevant docs

Read `core-web/CLAUDE.md` first (always), then the specific docs for this change. See `references/docs-map.md`.

### 7c. Baseline test check

```bash
cd core-web && yarn nx test <project>
```

If baseline fails, warn: `"⚠️ Pre-existing failure — not caused by your changes."` and continue.

### 7d. Implement

Write code guided by `confirmed_acs`. Verify each AC is addressed.

- Standalone components, `@if`/`@for`, signals, `inject()`, OnPush
- No hardcoded secrets · no `console.log` left in production code

---

## Step 8 — Tests + Quality

### 8a. Write tests

Derive test cases directly from `confirmed_acs` — one test per AC at minimum. Use `references/test-plan-prompt.md` as a format guide for Given/When/Then structure.

```bash
cd core-web && yarn nx test <project>
```

### 8b. Format and lint

```bash
cd core-web && yarn nx lint <project> --fix && yarn nx format:write
```

Stop on unfixable errors and show them to the user.

---

## Step 9 — Review

Run `/review` on the changed files. The review skill uses the same specialized agents (`dotcms-file-classifier` → `dotcms-typescript-reviewer`, `dotcms-angular-reviewer`, `dotcms-test-reviewer`, `dotcms-scss-html-style-reviewer`).

If the review found any 🔴 Critical or 🟡 Important issues:

1. **Apply the fixes** — implement the changes required to address the findings.
2. **Re-run tests** — `yarn nx test <project>` to confirm nothing is broken.
3. **Reply and resolve review comments** — for each finding that was addressed:
   - Post a reply explaining what was done (or why a change was declined). Keep it concise.
   - Always end the reply with the signature `— 🤖 Claude` so the human reviewer knows who responded.
   - After replying, resolve the thread via the GitHub API.

Only proceed once all critical/important findings are resolved.

> If this is a `--review` entry point (Step 2 jump), start here with the current branch's changes and proceed to PR.

---

## Step 10 — PR + Quality loop

### 10a. Commit

```bash
git add <changed-files>
git commit --no-verify -m "<type>(<scope>): <description>

Closes #<number>"
```

Types: `feat` · `fix` · `chore` · `test` · `refactor` · `docs` · `style`

### 10b. Push

```bash
git push -u origin "$BRANCH"
```

### 10c. Create draft PR

```bash
gh pr create --draft \
  --title "<type>(<scope>): <description>" \
  --body "$(cat <<'EOF'
## Proposed Changes
- <bullet list>

## Acceptance Criteria
- [ ] AC1: <description>
- [ ] AC2: <description>

## Test Coverage
<N> test cases covering <N> ACs.

## Review Summary
<summary from Step 9>

## Checklist
- [x] Tests added/updated per ACs
- [ ] Translations required
- [ ] Security implications considered

## Assumptions Made
<from AC refinement — or "None">

Closes #<number>
EOF
)"
```

### 10d. Quality loop

```
REPEAT:
  1. Run /review <pr-number>
  2. Fix Critical 🔴 or Important 🟡 issues → commit → push → repeat
  3. Exit when only Quality 🔵 or none remain
```

### 10e. Mark ready

```bash
gh pr ready <pr-number>
```

### 10f. Move issue to "In Review"

After PR is marked ready, move the linked issue on the project board to **"In Review"** using the GraphQL commands in `references/commands.md` (section "Project Board").

If the issue is not on any project board, skip this step and note it to the user.

Report: `"✅ PR #<pr-number> ready: <url> · Branch: $BRANCH · Closes #<number>"`

---

## Error handling

| Situation | Action |
|---|---|
| `gh auth status` fails | Stop: `"Run gh auth login first."` |
| Dirty working tree | Stop: `"Commit or stash changes before starting."` |
| Issue not found | `"Issue #<number> not found in dotCMS/core. Verify the number and try again."` |
| AC refinement loop ≥ 3 rounds | Write ACs with best info, flag gaps as `⚠ UNRESOLVED`, proceed |
| Baseline tests fail | Warn, continue (pre-existing failure) |
| No frontend files changed | `"No frontend changes detected."` |
| Lint errors not auto-fixable | Stop, show errors |
| PR creation fails | Show error, suggest manual `gh pr create` |
| Issue not on a project board | Skip "In Review" move, note it to the user |
| `read:project` scope missing | Prompt user: `gh auth refresh -s project` then retry |

---

## Skill Metadata

- **Scope**: Frontend (`core-web/` — Angular, TypeScript, SCSS)
- **Steps**: 10 — Pre-flight → Resolve → Fetch → Existing work → AC Refinement → Branch → Implement → Tests → Review → PR
- **Agents used**: `/review` skill
- **Related skills**: `/review` (PR-only review)
