---
name: build-verify
description: Run tests, write tests, lint, and format for a dotCMS frontend project. Detects the Nx project name automatically. Auto-fixes lint errors and stops on unfixable failures.
allowed-tools: Read, Edit, Write, Bash(cd core-web:*), Bash(cd:*), Bash(yarn nx test:*), Bash(yarn nx lint:*), Bash(yarn nx format:*), Bash(git diff:*)
---

# Build & Verify

Input: changed file list and `confirmed_acs` (from `/implement`).

Find `<project>` from the nearest `project.json` walking up from any changed file. Read the `"name"` field.

---

## Step 1 — Write tests

Derive test cases from `confirmed_acs` using the format in `.claude/skills/solve-issue/references/test-plan-prompt.md`. One test per AC at minimum.

Use Jest + Spectator patterns from `core-web/CLAUDE.md`:
- `data-testid` selectors
- `spectator.setInput()` for inputs
- `mockProvider` from `@ngneat/spectator/jest`

```bash
cd core-web && yarn nx test <project>
```

Fix any test failures before proceeding.

---

## Step 2 — Lint

```bash
cd core-web && yarn nx lint <project> --fix
```

If unfixable errors remain: stop and show them to the user.

---

## Step 3 — Format

```bash
cd core-web && yarn nx format:write
```

---

## Output

```
✅ Build verified — <N> tests passing, lint clean, formatted.
```

Or list failures with exact error output for the user to review.
