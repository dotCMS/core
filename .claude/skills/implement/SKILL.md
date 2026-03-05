---
name: implement
description: Implement code changes for a dotCMS frontend issue. Loads relevant docs, checks baseline tests, then writes code guided by confirmed_acs and the coder_brief. Frontend (Angular/TypeScript/SCSS) only.
allowed-tools: Read, Glob, Grep, Edit, Write, Bash(yarn nx test:*), Bash(cd core-web:*), Bash(cd:*)
---

# Implement

Input: `confirmed_acs`, `coder_brief`, `number` (from `/refine-acs` and `/fetch-issue`).

---

## Step 1 — Present plan

Show what files will be created or modified and why, mapped per AC.

Ask: `"Approve plan? yes / no / edit"` — stop if no.

---

## Step 2 — Load docs

Read these files before writing any code:

```
core-web/CLAUDE.md           ← always
```

Then per change type — see `.claude/skills/solve-issue/references/docs-map.md`.

---

## Step 3 — Baseline test check

```bash
cd core-web && yarn nx test <project>
```

Find `<project>` from the nearest `project.json` in the file tree.

If baseline fails: warn `"⚠️ Pre-existing failure — not caused by your changes."` and continue.

---

## Step 4 — Write code

Implement guided by `confirmed_acs` and `coder_brief`. Verify each AC is addressed.

**Required patterns:**
- Standalone components, `@if`/`@for`, signals, `inject()`, `OnPush`
- `data-testid` attributes on interactive elements
- No hardcoded secrets · no `console.log` in production code
- Tailwind utilities (not PrimeFlex) · PrimeNG components for UI

---

## Output

List changed files. These are passed to `/build-verify` and `/review`.
