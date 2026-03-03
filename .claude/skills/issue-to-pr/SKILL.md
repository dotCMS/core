---
name: issue-to-pr
description: Frontend Issue-to-PR pipeline. Takes a GitHub issue, does deep product analysis (pseudocode + user Q&A), researches relevant frontend files, guides implementation, auto-reviews changed code, and creates a draft PR. Frontend (Angular/TypeScript/SCSS) only.
allowed-tools: Bash(git branch --show-current*), Bash(git checkout -b*), Bash(git diff --name-only*), Bash(gh issue view*), Bash(gh pr create*), Bash(gh pr list*)
---

# Issue → PR: Frontend Development Pipeline

End-to-end agent orchestra for shipping a GitHub issue as a frontend PR.

```
Issue → [validate + product analysis] → user Q&A → branch → implement → review → QA → draft PR
```

---

## Usage

```bash
/issue-to-pr <issue-number>
/issue-to-pr             # detects issue from current branch name
/issue-to-pr --review    # skip to review + PR creation (already implemented)
```

---

## Stage 1: Resolve Issue Number

Parse `$ARGUMENTS` for a number. If `--review` flag is present, jump to Stage 6.

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

## Stage 2: Fetch Issue + Run Parallel Agents

Fetch the issue:

```bash
gh issue view <number> --repo dotCMS/core --json number,title,body,labels,assignees,url
```

Then launch **two agents in a single message** (parallel):

**Agent 1** — `subagent_type: dotcms-issue-validator`

Pass the full issue JSON. Ask it to validate completeness and return status (SUFFICIENT or NEEDS_INFO) with missing fields.

**Agent 2** — `subagent_type: dotcms-product-analyst`

Pass:
- Issue number, title, body
- Issue type (inferred from labels or body if not explicit)
- The `core-web/` path as the working area

Ask it to research the issue from a **product perspective**, produce pseudocode, identify edge cases, list open questions, and generate a Coder Brief.

Wait for both agents to complete before proceeding.

---

## Stage 3: Interactive Product Q&A

This is the most important stage. Do NOT skip it.

### 3a — Validation check

If the validator returned `NEEDS_INFO`:
```
⚠️  Issue #<number> may be incomplete.
Missing: <list from validator>

This could make implementation ambiguous. You can still proceed, or update the issue first.
```

Ask: **"Proceed anyway? yes / no"** — if no, stop.

### 3b — Present the product analysis

Show the full output from `dotcms-product-analyst`:

```markdown
## Product Analysis: #<number> — <title>

### What's being built
<Product Understanding section from agent>

### Acceptance Criteria
<AC list from agent>

### Pseudocode
<Pseudocode section from agent>

### Files to Modify
<File list from agent>

### Edge Cases Identified
<Edge cases from agent>
```

### 3c — Ask the open questions

If the product analyst returned any Open Questions, present them NOW to the user one by one (or as a grouped list):

```
Before we proceed, I need your input on a few things:

Q1: <question from product analyst>
Q2: <question from product analyst>
[...]

Please answer these — your answers will be included in the implementation brief.
```

Wait for the user's answers. If there are no open questions, say:
> "The product analysis is complete. No open questions — the requirements are clear enough to proceed."

### 3d — Ask for any additional edge cases or challenges

After the user answers the open questions (or if there were none), ask:

```
Anything else I should know before we start?
- Are there any edge cases you're concerned about?
- Any constraints, performance requirements, or design decisions I should be aware of?
- Any related work in progress that could conflict?

(Press Enter to skip)
```

Wait for the user's response. Empty / skip is fine.

### 3e — Confirm the coder brief

Show the Coder Brief from the product analyst, updated with:
- User's answers to open questions
- Any additional context from 3d

```markdown
## Coder Brief

<Updated Coder Brief — incorporating all user answers>
```

Ask: **"Does this brief look correct? yes / edit / no"**

- `yes` → proceed to Stage 4
- `edit` → ask what to change, update the brief, confirm again
- `no` → stop and ask the user what's wrong

---

## Stage 4: Create Feature Branch (if needed)

Check current branch:

```bash
git branch --show-current
```

If NOT already on an `issue-<number>-*` branch:

```bash
git checkout -b issue-<number>-<slug>
```

Slug rules: lowercase issue title, spaces → hyphens, keep only `[a-z0-9-]`, max 40 chars.

Example: "Add virtual scroll to content type filter" → `issue-34823-add-virtual-scroll-content-type`

---

## Stage 5: Implementation

Present the Coder Brief and files to the developer:

```markdown
## Ready to implement

**Branch**: `issue-<number>-<slug>`

**Start here**: [path/to/entry.ts](path/to/entry.ts)

**Implementation order**:
1. <step 1> — [file](path)
2. <step 2> — [file](path)
3. <step 3> — [file](path)

**Pseudocode reference**: [available above in the product analysis]
```

If the user asks Claude to implement directly: proceed using the Coder Brief and pseudocode as the implementation spec.

When implementation is done, continue to Stage 6.

---

## Stage 6: Review Changed Files

Get the list of changed frontend files:

```bash
git diff --name-only HEAD
```

If empty (nothing staged), try:
```bash
git diff --name-only HEAD~1
```

**Frontend filter**: keep only files under `core-web/` with extensions `.ts`, `.html`, `.scss`, `.css`, `.spec.ts`.

If no frontend files found → stop: "No frontend changes detected. This pipeline is for frontend code only."

### 6a — Classify

Launch **`dotcms-file-classifier`**:

```
Task(
    subagent_type="dotcms-file-classifier",
    prompt="Classify these changed frontend files by domain (Angular components, TypeScript services, test specs, styles). Files: <file-list>. Return reviewer buckets.",
    description="Classify changed files"
)
```

### 6b — Parallel reviewers (non-empty buckets only)

Launch in a **single message**:

| Bucket has files? | Agent |
|---|---|
| TypeScript | `dotcms-typescript-reviewer` |
| Angular | `dotcms-angular-reviewer` |
| Tests | `dotcms-test-reviewer` |
| SCSS/HTML | `dotcms-scss-html-style-reviewer` |

Pass each agent: the relevant file list + issue number for context.

### 6c — Consolidate

Merge findings by severity:
- 🔴 Critical (95-100): Must fix before merge
- 🟡 Important (85-94): Should address
- 🔵 Quality (75-84): Nice to have

Show consolidated review output.

### 6d — Apply fixes and re-verify

If the review found any 🔴 Critical or 🟡 Important issues:

1. **Apply the fixes** — implement the changes required to address the findings.
2. **Re-run tests** — `yarn nx test <project>` to confirm nothing is broken.
3. **Re-run QA** — if QA was already run in Stage 7, run it again after the fixes. Skip back to Stage 7c (execute QA) and confirm all scenarios still pass.
4. **Reply and resolve review comments** — for each finding that was addressed:
   - Post a reply explaining what was done (or why a change was declined). Keep it concise — "Fixed." is fine for simple changes; add detail for non-obvious decisions.
   - Always end the reply with the signature `— 🤖 Claude` so the human reviewer knows who responded and can continue the conversation.
   - After replying, resolve the thread via the GitHub API.

Only proceed to Stage 8 (PR creation) once all critical/important findings are resolved AND QA is green.

> If this is a post-PR review (comments on an existing PR rather than pre-PR review), the same rule applies: fix → re-run QA → resolve comment → then confirm to the user.

---

## Stage 7: QA

QA validates the implementation against a real environment before the PR is created. This stage is interactive — scenarios are derived from the product analysis, data is requested from the user, and tests are executed and reported.

### 7a — Identify test scenarios

Based on the product analysis and Coder Brief, derive test scenarios in three categories:

**Positive scenarios** — things that MUST work:
- The primary happy path for the feature
- Each acceptance criterion mapped to a concrete observable behavior

**Negative scenarios** — things that must NOT happen:
- Regressions on existing behavior
- Feature absent when disabled (e.g., feature flag off, option `false`, no config set)

**Edge cases** — boundary conditions from the product analysis:
- Null/undefined/empty inputs
- Idempotency (repeated calls)
- Interaction with related methods or conditions

Present the scenarios to the user:

```markdown
## QA Scenarios for #<number>

### ✅ Positive (must work)
1. <scenario>: <expected result>

### 🚫 Negative (must NOT happen)
1. <scenario>: <expected result>

### ⚠️ Edge Cases
1. <scenario>: <expected result>
```

### 7b — Ask for test data

Based on the scenarios, identify what real-world data or environment access is needed:

```
To verify these scenarios against a real environment I need:

<list what is needed — e.g.:>
- A running dotCMS instance (host URL)
- API key / credentials
- A site ID (if site-scoped)
- Content types / content that exist in the environment
- Any specific URLs, routes, or pages to visit

Please provide what you have, or type "skip" to go straight to PR creation.
```

Wait for the user's response. If the user skips, proceed to Stage 8.

### 7c — Execute QA

Choose the execution method based on what changed:

| Changed area | Execution method |
|---|---|
| `libs/sdk/` (SDK library) | Build → npm pack → test with Node.js (see **SDK QA** below) |
| Angular UI portlet/component | Serve dev server, navigate to the affected page, verify behavior |
| Angular service / data-access | Jest tests + manual curl/Postman against the API |
| SCSS/styles only | Screenshot comparison — serve and inspect visually |

**SDK QA — Node.js pack approach:**

```bash
# Build the library
yarn nx build <sdk-project>

# Pack it (creates a .tgz)
cd dist/libs/sdk/<lib-name> && npm pack
```

Write a `qa-test.mjs` script that initializes the SDK with the user-provided credentials, runs each scenario as an async function, and prints a pass/fail table:

```js
// qa-test.mjs
import { createClient } from '<path-to-.tgz>';

const client = createClient({ host, siteId, authToken });

const results = [];

async function scenario(name, fn, expect) {
    try {
        const result = await fn();
        const passed = expect(result);
        results.push({ name, passed, detail: passed ? 'ok' : JSON.stringify(result) });
    } catch (e) {
        results.push({ name, passed: false, detail: e.message });
    }
}

// --- Scenarios ---
await scenario(
    '<name>',
    () => client.content.getCollection('<Type>').<method>().limit(5),
    (r) => /* assertion */
);

// --- Report ---
console.table(results);
if (results.some(r => !r.passed)) process.exit(1);
```

```bash
node qa-test.mjs
```

### 7d — Report results

If all pass:
```
✅ QA passed — all <N> scenarios verified against <host>
```

If any fail:
```
❌ QA failed — <N> of <M> scenarios failed:
- <scenario>: <detail>

Fixing before PR creation.
```

On failure: fix the implementation, re-run from 7c. Do NOT proceed to Stage 8 with failing QA.

> **After any review fix (Stage 6d):** always treat QA as mandatory — re-run from 7c regardless of whether it passed before. A fix that addresses a review finding may introduce a regression.

---

## Stage 8: Create Draft PR

Ask: **"Create a draft PR? yes / no"**

On yes, launch `github-workflow-manager`:

```
Task(
    subagent_type="github-workflow-manager",
    prompt="Create a draft PR for issue #<number> in dotCMS/core.
    Branch: <current-branch>. Base: main.
    Title: '<issue-title>'.
    PR body must include:
    - 'Closes #<number>'
    - Summary of what was implemented (from the Coder Brief)
    - Acceptance criteria checklist (from the product analysis)
    - Review findings summary (from Stage 6)
    - QA results summary (from Stage 7, if run)
    Use the standard dotCMS PR format.",
    description="Create draft PR"
)
```

Return the PR URL to the user.

### 8b — Move issue to "In Review"

If the PR is **not a draft** (either created as ready or promoted out of draft), move the linked issue on the project board to the **"In Review"** column:

```bash
# Check if PR is draft
gh pr view <pr-number> --repo dotCMS/core --json isDraft --jq '.isDraft'
```

If `isDraft` is `false`:

1. Find the issue's project item ID via GraphQL:
```graphql
{
  repository(owner: "dotCMS", name: "core") {
    issue(number: <issue-number>) {
      projectItems(first: 5) {
        nodes {
          id
          project { id title }
          fieldValues(first: 10) {
            nodes {
              ... on ProjectV2ItemFieldSingleSelectValue {
                name
                field { ... on ProjectV2SingleSelectField { name } }
              }
            }
          }
        }
      }
    }
  }
}
```

2. Find the "Status" field ID and the "In Review" option ID for the project.

3. Update the item status:
```graphql
mutation {
  updateProjectV2ItemFieldValue(input: {
    projectId: "<project-id>"
    itemId: "<item-id>"
    fieldId: "<status-field-id>"
    value: { singleSelectOptionId: "<in-review-option-id>" }
  }) {
    projectV2Item { id }
  }
}
```

If the issue is not on any project board, skip this step and note it to the user.

---

## Error Handling

| Situation | Action |
|---|---|
| Issue not found | "Issue #<number> not found in dotCMS/core. Verify the number and try again." |
| NEEDS_INFO on validation | Warn but offer to continue |
| Product analyst returns no open questions | Skip Q&A, proceed with brief directly |
| No frontend files changed | "No frontend changes detected. Use `/review <PR>` for reviewing an existing PR." |
| User rejects coder brief | Ask what's wrong, re-run product analyst with additional context if needed |
| User skips QA | Note in PR body that QA was skipped, proceed to Stage 8 |
| QA fails | Fix implementation, re-run QA from Stage 7c before proceeding |
| PR creation fails | Show error and suggest manual `gh pr create` command |
| Issue not on a project board | Skip "In Review" move, note it to the user |
| `read:project` scope missing | Prompt user: `gh auth refresh -s project` then retry |

---

## Skill Metadata

- **Scope**: Frontend only (`core-web/` — Angular, TypeScript, SCSS)
- **Stages**: 8 total — Resolve → Fetch+Analyze → Q&A → Branch → Implement → Review → QA → PR
- **Agents used**: `dotcms-issue-validator`, `dotcms-product-analyst`, `dotcms-file-classifier`, `dotcms-typescript-reviewer`, `dotcms-angular-reviewer`, `dotcms-test-reviewer`, `dotcms-scss-html-style-reviewer`, `github-workflow-manager`
- **Related skills**: `/review` (PR-only review), `/triage` (issue triage)
