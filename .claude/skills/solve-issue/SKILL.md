---
name: solve-issue
description: Automates the full development cycle from a GitHub Issue number for the dotCMS/core repository. Orchestrates preflight → fetch-issue → refine-acs → branch → implement → build-verify → review → qa → create-pr. Use when the user asks to solve, fix, implement, or work on a GitHub issue by number (e.g. "solve issue 34353", "work on #34353", "implement issue 34353"). Do NOT use for non-GitHub tasks, general coding questions, or issues from repositories other than dotCMS/core.
allowed-tools: Read, Bash(gh:*), Bash(git:*), Bash(yarn:*), Bash(bash .claude/skills/solve-issue/scripts/slugify.sh:*), Bash(cd:*), Bash(node:*), Bash(npm pack:*), Glob, Grep, Edit, Write, Agent
---

# Solve Issue

Orchestrator for the full issue-to-PR pipeline. Each step is a focused skill — read and execute it in order.

Arguments: `$ARGUMENTS` — extract issue number and flags.

**Flags:**
- `--review` → skip to Step 7 (review existing implementation)
- `--qa` → skip to Step 8 (run QA on existing implementation)

---

## Pipeline

```
preflight → fetch-issue → refine-acs → branch → implement → build-verify → review → qa → create-pr
```

---

## Step 1 — Preflight

Read and execute `.claude/skills/preflight/SKILL.md`.

Stop if any check fails.

---

## Step 2 — Fetch issue

Read and execute `.claude/skills/fetch-issue/SKILL.md`.

Pass `$ARGUMENTS` as the issue number source.

Carry forward: `number`, `title`, `body`, `labels`, `url`, `existing_branch`.

---

## Step 3 — Refine ACs

Read and execute `.claude/skills/refine-acs/SKILL.md`.

Pass: `number`, `title`, `body`, `labels`.

Carry forward: `confirmed_acs`, `coder_brief`.

---

## Step 4 — Branch

Read and execute `.claude/skills/branch/SKILL.md`.

Pass: `number`, `title`, `existing_branch`.

Carry forward: `branch_name`.

---

## Step 5 — Implement

Read and execute `.claude/skills/implement/SKILL.md`.

Pass: `confirmed_acs`, `coder_brief`, `number`.

Carry forward: changed file list.

---

## Step 6 — Build & Verify

Read and execute `.claude/skills/build-verify/SKILL.md`.

Pass: changed file list, `confirmed_acs`.

Stop on unfixable lint errors.

---

## Step 7 — Review  ← entry point for `--review`

Run `/review` on the changed files.

If any 🔴 Critical or 🟡 Important findings:
1. Apply fixes
2. Re-run Step 6
3. Reply and resolve review comments (see review skill for format)

Proceed only when all critical/important findings are resolved.

---

## Step 8 — QA  ← entry point for `--qa`

Read and execute `.claude/skills/qa/SKILL.md`.

Pass: `confirmed_acs`, `coder_brief`, changed file list.

Carry forward: `qa_summary`.

---

## Step 9 — Create PR

Read and execute `.claude/skills/create-pr/SKILL.md`.

Pass: `number`, `branch_name`, `confirmed_acs`, `coder_brief`, review summary, `qa_summary`.

---

## Error handling

| Situation | Action |
|---|---|
| Preflight fails | Stop with message from preflight skill |
| Issue not found | Stop: `"Issue #<number> not found in dotCMS/core."` |
| AC refinement loop ≥ 3 rounds | Flag gaps as `⚠ UNRESOLVED`, proceed |
| Baseline tests fail | Warn (pre-existing), continue |
| Lint errors not auto-fixable | Stop, show errors |
| QA fails | Fix implementation, re-run QA before proceeding |
| PR creation fails | Show error, suggest manual `gh pr create` |
| Issue not on project board | Skip board move, note to user |
| `read:project` scope missing | `gh auth refresh -s project` then retry |
