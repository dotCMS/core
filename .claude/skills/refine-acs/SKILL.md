---
name: refine-acs
description: Acceptance criteria refinement for a dotCMS issue. Runs dotcms-product-analyst + dotcms-issue-validator in parallel, presents product analysis, runs Q&A loop, and produces confirmed_acs. Requires issue number, title, and body as input.
allowed-tools: Agent
---

# Refine ACs

Input: issue `number`, `title`, `body`, `labels` from `/fetch-issue`.

**Do not proceed until `confirmed_acs` is approved by the user.**

---

## Step 1 — Parallel analysis

Launch **both agents in a single message**:

**Agent 1** — `subagent_type: dotcms-issue-validator`

Pass full issue JSON. Returns: `SUFFICIENT` or `NEEDS_INFO` with missing fields.

**Agent 2** — `subagent_type: dotcms-product-analyst`

Pass: issue number, title, body, labels, and `core-web/` as working area.

Ask it to: research from a product perspective, produce pseudocode, identify edge cases, list open questions, and generate a Coder Brief.

Wait for both before continuing.

---

## Step 2 — Validation gate

If validator returned `NEEDS_INFO`:
```
⚠️  Issue #<number> may be incomplete.
Missing: <list>

This could make implementation ambiguous.
```
Ask: **"Proceed anyway? yes / no"** — stop if no.

---

## Step 3 — Present product analysis

```markdown
## Product Analysis: #<number> — <title>

### What's being built
<Product Understanding from analyst>

### Pseudocode
<Pseudocode from analyst>

### Files to Modify
<File list from analyst>

### Edge Cases
<Edge cases from analyst>
```

---

## Step 4 — Q&A loop

Follow the process in `.claude/skills/solve-issue/references/issue-refinement.md`.

**Fast path:** No CRITICAL or MAJOR ambiguities → write ACs, score them, show to user, ask: `"Proceed? yes / edit"`

**Slow path:** CRITICAL or MAJOR ambiguities → run clarification loop (max 3 rounds, min score 80/100).

After loop 3 with score < 80: flag unresolved items with `⚠ UNRESOLVED` and proceed.

---

## Step 5 — Confirm

Ask: `"Does this coder brief look correct? yes / edit / no"`

- `yes` → save as `confirmed_acs` + `coder_brief`, return both
- `edit` → accept inline edits, confirm again
- `no` → ask what's wrong, re-run analyst with additional context

---

## Output

Return `confirmed_acs` (checkbox list) and `coder_brief` (implementation spec with pseudocode, file list, edge cases, user answers).
