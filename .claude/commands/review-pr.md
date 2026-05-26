---
name: review-pr
description: Fetch ALL code-review feedback for a dotCMS PR (inline review threads, top-level bot/human comments, review summaries) and surface only Critical 🔴 and High 🟠 findings with file:line pointers ready to act on.
argument-hint: <pr-number|pr-url>
context: fork
agent: Explore
allowed-tools: Bash(gh api:*), Bash(gh pr view:*), Bash(git fetch:*), Bash(git checkout:*), Bash(git stash:*), Bash(git log:*), Bash(git show:*), Read, Grep, Glob
---

**Input:** $ARGUMENTS

You are triaging code-review findings for a dotCMS pull request. Your job is to collect every piece of review feedback from ALL GitHub sources, deduplicate, filter to Critical and High severity, check whether each issue is already addressed in the branch, and present a concise action list.

---

## Step 0 — Parse PR number

Accept any of these forms:
- Full URL: `https://github.com/dotCMS/core/pull/35808`
- URL with anchor: `https://github.com/dotCMS/core/pull/35808#issuecomment-123`
- Bare number: `35808`

Extract the numeric PR number. All subsequent calls use `dotCMS/core` as the repo.

---

## Step 1 — Fetch the branch name, switch to it, and capture recent commits

```bash
gh pr view <PR_NUM> --repo dotCMS/core --json headRefName,commits \
  --jq '{branch: .headRefName, commits: [.commits[-10:][].messageHeadline]}'
```

Store the branch name. Then switch the local repo to that branch so that all subsequent file reads (Steps 4–5) reflect the actual PR code, not whatever was checked out before:

```bash
# Stash any uncommitted local changes so checkout cannot fail
git stash

# Fetch the branch if it is not already local
git fetch origin <BRANCH_NAME>

# Switch to it
git checkout <BRANCH_NAME>
```

If `git checkout` fails (e.g. the branch name contains slashes), try:
```bash
git checkout -b <BRANCH_NAME> origin/<BRANCH_NAME>
```

The commit messages tell you what fixes have already been applied — use them in Step 4 to avoid flagging already-fixed issues.

---

## Step 2 — Fetch ALL review sources in parallel

Run all three commands; each covers a different GitHub source:

**Source A — Inline code-review thread comments** (reviewer clicks a line in the diff):
```bash
gh api repos/dotCMS/core/pulls/<PR_NUM>/comments \
  --paginate \
  --jq '.[] | {source:"inline", user:.user.login, path:.path, line:.original_line, body:.body}'
```

**Source B — Review-level summaries** (the overall APPROVE / REQUEST_CHANGES body):
```bash
gh api repos/dotCMS/core/pulls/<PR_NUM>/reviews \
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

**Implicit severity inference** (for plain human inline comments without markers):
Assign HIGH if the comment body contains any of:
- security keywords: `injection`, `traversal`, `XSS`, `SQL`, `NPE`, `NullPointer`, `CVE`, `exploit`, `bypass`, `exfiltrat`
- data-loss keywords: `data loss`, `corruption`, `race condition`, `deadlock`, `connection pool`, `starvation`
- correctness keywords: `never called`, `dead code` (only if the method IS unused — verify with Grep), `wrong user`, `wrong tenant`

Assign CRITICAL if the comment body contains: `arbitrary file`, `arbitrary object`, `remote code`, `path traversal`, `SQL injection`, `Lucene injection`, `query injection`

Everything else from plain human inline comments is MEDIUM or lower — **skip**.

### Deduplication
If the same file+line or the same description appears in multiple sources (e.g. a bot comment and an inline thread), merge them into one finding.

---

## Step 4 — Check whether each Critical/High finding is already fixed

For each finding:

1. Scan the commit messages from Step 1 for keywords matching the finding (e.g. "fix npe", "interrupt", "Italian", "dead code", "lazy").
2. If a relevant commit exists, read the file at the affected path and line to confirm the fix is in place.
3. Mark the finding as **✅ Already fixed** or **❌ Needs action**.

---

## Step 5 — For each unfixed finding, read the code

For every finding marked **❌ Needs action**:

1. Read ±15 lines around the reported file:line.
2. Confirm the issue is still present in the current branch.
3. If the issue is gone (reviewer was looking at old diff), mark it **✅ Already fixed (stale comment)**.

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

#### 2. ...

---

### ✅ Already Fixed

| Finding | Fixed by |
|---|---|
| <short title> | commit `<sha>` — "<message>" |
| ... | |

---

### Skipped (Medium / Low)
<count> Medium/Low findings not shown. Re-run with `--all` to see them.
```

**Rules:**
- Never invent findings. Only report what is explicitly stated in the review sources.
- If a finding references a specific line that no longer exists, mark it stale.
- Keep issue descriptions factual — quote the reviewer's own words where helpful.
- If all Critical/High findings are already fixed, say so clearly and list them in the Already Fixed table.
- Do NOT apply any fixes. Present findings for human review; let the user decide what to act on.
