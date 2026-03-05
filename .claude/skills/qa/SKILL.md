---
name: qa
description: QA verification for a dotCMS frontend change against a real environment. Derives test scenarios from accepted ACs, asks for test data, and executes browser or SDK tests. Type "skip" at the data prompt to go straight to PR.
allowed-tools: Write, Bash(cd:*), Bash(yarn nx build:*), Bash(yarn nx run dotcms-ui:serve:*), Bash(node:*), Bash(npm pack:*)
---

# QA

Input: `confirmed_acs`, `coder_brief`, changed files (from `/implement`).

---

## Step 1 — Derive scenarios

Based on the ACs and coder brief:

```markdown
## QA Scenarios for #<number>

### ✅ Positive (must work)
1. <primary happy path>: <expected result>
2. <one per AC>: <expected result>

### 🚫 Negative (must NOT happen)
1. <regression check>: <expected result>

### ⚠️ Edge Cases
1. <boundary / empty / concurrent>: <expected result>
```

---

## Step 2 — Request test data

```
To verify these scenarios I need:
- A running dotCMS instance (host URL)
- API credentials / token
- Any specific content types, site IDs, or pages to visit

Provide what you have, or type "skip" to proceed to PR creation.
```

Wait for response. If `skip` → go to output.

---

## Step 3 — Execute

Choose method based on what changed:

| Changed area | Method |
|---|---|
| Angular UI portlet/component | Serve dev server → navigate → verify behavior |
| `libs/sdk/` | Build → `npm pack` → Node.js test script (see below) |
| Service / data-access only | Jest tests + manual curl against API |
| SCSS/styles only | Serve → screenshot comparison |

**SDK test script pattern:**

```bash
cd core-web && yarn nx build <sdk-project>
cd dist/libs/sdk/<lib-name> && npm pack
```

Write `qa-test.mjs` — initialize SDK with user credentials, run each scenario as `async function`, print pass/fail table, `process.exit(1)` on failure.

```bash
node qa-test.mjs
```

---

## Step 4 — Report

All pass:
```
✅ QA passed — all <N> scenarios verified against <host>
```

Any fail:
```
❌ QA failed — <N> of <M> scenarios failed:
- <scenario>: <detail>
```

On failure: fix implementation, re-run from Step 3. Do NOT proceed with failing QA.

---

## Output

QA results summary — passed to `/create-pr` for the PR body.
