---
name: triage-pr-review
description: Fetch ALL code-review feedback for a dotCMS PR (inline review threads, top-level bot/human comments, review summaries), surface Critical 🔴 and High 🟠 findings, and evaluate each one for relevance and necessity against what the PR actually changed.
argument-hint: <pr-number|pr-url>
allowed-tools: Bash(gh api:*), Bash(gh pr view:*)
---

**Input:** $ARGUMENTS

You are triaging code-review findings for a dotCMS pull request. Your job is to collect every piece of review feedback from ALL GitHub sources, deduplicate, filter to Critical and High severity, check whether each issue is already addressed in the branch, **evaluate whether each finding is actually relevant to what this PR changed**, and present a concise action list so the developer only spends time on things that genuinely matter.

---

## Step 0 — Parse PR number

Accept any of these forms:
- Full URL: `https://github.com/dotCMS/core/pull/35808`
- URL with anchor: `https://github.com/dotCMS/core/pull/35808#issuecomment-123`
- Bare number: `35808`

Extract the numeric PR number. All subsequent calls use `dotCMS/core` as the repo.

---

## Step 1 — Fetch branch metadata and recent commits (API-only, no local checkout)

```bash
gh pr view <PR_NUM> --repo dotCMS/core \
  --json headRefName,headRefOid,commits \
  --jq '{branch: .headRefName, sha: .headRefOid, commits: [.commits[-10:][].messageHeadline]}'
```

Store:
- **BRANCH**: head branch name
- **HEAD_SHA**: head commit SHA — used for all file reads in Steps 4–5
- **COMMITS**: 10 most recent commit messages

> The local working tree is **never touched**. All file content is fetched via the GitHub Contents API in Steps 4–5. This is safe for fork PRs and cannot strand the user on a different branch.

---

## Step 1.5 — Read PR context (diff + description)

Fetch the PR description and the list of changed files. This is the reference you'll use in Step 6 to judge whether each finding is relevant to what this PR actually touched:

```bash
gh pr view <PR_NUM> --repo dotCMS/core \
  --json title,body,files \
  --jq '{title:.title, body:.body, files:[.files[].path]}'
```

Also fetch the full diff so you can see exactly which lines were added or changed:

```bash
gh api repos/dotCMS/core/pulls/<PR_NUM> \
  -H "Accept: application/vnd.github.v3.diff"
```

Store:
- **PR_DESCRIPTION**: the `body` field (explains the goal and scope of the change)
- **CHANGED_FILES**: the list of file paths
- **PR_DIFF**: the unified diff (lines prefixed with `+` are new/changed; `-` are removed)

---

## Step 2 — Fetch ALL review sources in parallel

Run all three commands; each covers a different GitHub source:

**Source A — Inline code-review thread comments** (reviewer clicks a line in the diff):
```bash
gh api repos/dotCMS/core/pulls/<PR_NUM>/comments \
  --paginate \
  --jq '.[] | {source:"inline", user:.user.login, path:.path, line:.line, original_line:.original_line, body:.body}'
```

`.line` is the current line number in the head commit; `.original_line` is the line number on the diff version the reviewer saw. After a force-push or rebase, `.line` may be null (GitHub marks the comment as outdated). **Use `.line` for code reads when non-null; fall back to `.original_line` and label the finding as `[outdated diff position]` when `.line` is null.**

**Source B — Review-level summaries** (the overall APPROVE / REQUEST_CHANGES body):
```bash
gh api repos/dotCMS/core/pulls/<PR_NUM>/reviews \
  --paginate \
  --jq '.[] | {source:"review", user:.user.login, state:.state, body:.body}'
```

**Source C — Top-level PR / issue comments** (where review bots like the dotCMS backend-review bot post structured findings):
```bash
gh api repos/dotCMS/core/issues/<PR_NUM>/comments \
  --paginate \
  --jq '.[] | {source:"issue_comment", user:.user.login, comment_id:.id, body:.body}'
```

---

## Step 3 — Extract and classify findings

For each piece of content collected in Step 2, extract individual findings and assign severity:

### Severity detection rules

**Explicit markers** (structured bot comments use these — always trust them):
- `🔴` or `[🔴 Critical]` or `**[🔴 Critical]**` → **CRITICAL**
- `🟠` or `[🟠 High]` or `**[🟠 High]**` → **HIGH**
- `🟡` or `[🟡 Medium]` → MEDIUM — **skip, do not include in output**
- `🟢` or `[🟢 Low]` → LOW — **skip**

**Implicit severity inference** (for plain human inline comments without explicit markers):

Apply keyword matching carefully — false positives are common. Rules:
- Only match the keyword when it describes the **problem being reported**, not when it appears inside a quoted block, a negation ("this does NOT cause an NPE"), or an unrelated context ("bypass the cache layer").
- When in doubt, do **not** infer HIGH — leave the finding as MEDIUM and skip it.
- All inferred-severity findings must be flagged with `[severity: inferred]` in the output so the developer knows it was guessed, not declared by the reviewer.

Assign HIGH (inferred) if the comment clearly describes one of:
- A security problem: SQL/Lucene/query injection, path traversal, XSS, CVE, data exfiltration
- A null-pointer / NPE that would crash in production
- A data-loss scenario: corruption, race condition, deadlock, connection pool exhaustion
- A correctness bug: wrong user/tenant context

Assign CRITICAL (inferred) only for explicit attack-surface phrases: `arbitrary file`, `arbitrary object`, `remote code execution`, `path traversal`, `SQL injection`, `Lucene injection`.

For `dead code` / `never called`: only infer HIGH if you can **confirm with Grep** that no other file references the method — Java reflection, Spring/CDI wiring, JSP/Velocity templates, and test classes are common false-negative sources. If uncertain, skip.

Everything else from plain human inline comments is MEDIUM or lower — **skip**.

### Deduplication
If the same file+line or the same description appears in multiple sources (e.g. a bot comment and an inline thread), merge them into one finding.

---

## Step 4 — Check whether each Critical/High finding is already fixed

For each finding:

1. Scan **COMMITS** from Step 1 for keywords matching the finding (e.g. "fix npe", "interrupt", "dead code").
2. If a relevant commit exists, fetch the file at HEAD_SHA to confirm the fix is in place (see Step 5 for how to fetch file content).
3. Mark the finding as **✅ Already fixed** or **❌ Needs action**.

---

## Step 5 — For each unfixed finding, read the code via GitHub API

For every finding marked **❌ Needs action**, fetch the file content from the PR head — **no local checkout needed**:

```bash
gh api repos/dotCMS/core/contents/<URL-encoded-path>?ref=<HEAD_SHA> \
  -H "Accept: application/vnd.github.v3.raw"
```

Use the line number from Step 2 (prefer `.line`; fall back to `.original_line` if `.line` is null, and note `[outdated diff position]` in the finding). Read ±15 lines around that number in the response.

1. Confirm the issue is still present at HEAD_SHA.
2. If the issue is gone (reviewer was looking at an old diff), mark it **✅ Already fixed (stale comment)**.

---

## Step 6 — Evaluate relevance and necessity

For every finding that survived Step 5 (still present in the code), answer these three questions before assigning a recommendation:

### Q1 — Is the flagged code touched by this PR?
- Check whether the file appears in **CHANGED_FILES**.
- If yes, check whether the flagged line is in the **PR_DIFF** (a `+` line or within a hunk that was modified). If the line was not touched by this PR, the reviewer was commenting on pre-existing code.

### Q2 — Is the fix aligned with this PR's goal?
- Re-read **PR_DESCRIPTION**. If fixing the finding requires a refactor, architecture change, or work clearly out of scope of what the PR set out to do, note that.

### Q3 — Is this cosmetic or trivial?
- Javadoc / comment wording, whitespace, variable naming, code style → cosmetic.
- A real logic error, security hole, or data integrity risk → not cosmetic.

### Recommendation labels

| Label | When to use |
|---|---|
| **Must fix** | Finding is in code this PR changed AND it's a real correctness / security / data-integrity problem. Developer should address before merge. |
| **Nice to have** | Finding is in code this PR touched but the problem is minor (style, Javadoc, small refactor opportunity). Worth fixing but not a blocker. |
| **Out of scope — skip** | Finding is in pre-existing code the PR did not modify, OR it requires work clearly outside this PR's stated goal. Log it as a follow-up issue instead of blocking this PR. |

---

## Output

Produce exactly this structure. Omit any section that has no findings.

```
## PR #<number> — <PR title>
Branch: <branch-name>

---

### ❌ Needs Action — Critical & High findings

#### 1. [🔴 CRITICAL | 🟠 HIGH] <Short title>
**Source:** <inline thread | review summary | bot comment> — @<username>
**Location:** `path/to/File.java:line`
**Issue:** One-paragraph description of the problem and why it matters.
**Suggested fix:** Concrete code-level change.
**Recommendation:** Must fix | Nice to have | Out of scope — skip
**Relevance note:** One sentence explaining why — e.g. "Line 142 is in a hunk added by this PR" or "This file was not changed by this PR; the issue predates it."

#### 2. ...

---

### ✅ Already Fixed

| Finding | Fixed by |
|---|---|
| <short title> | commit `<sha>` — "<message>" |
| ... | |

---

### Skipped (Medium / Low)
<count> Medium/Low findings not shown.
```

**Rules:**
- Never invent findings. Only report what is explicitly stated in the review sources.
- If a finding references a specific line that no longer exists, mark it stale.
- Keep issue descriptions factual — quote the reviewer's own words where helpful.
- If all Critical/High findings are already fixed, say so clearly and list them in the Already Fixed table.
- Do NOT apply any fixes. Present findings for human review; let the user decide what to act on.
- The relevance evaluation (Step 6) is advisory — always show the finding; the **Recommendation** label tells the developer how urgently to act on it.
