---
allowed-tools: Bash(gh pr view:*), Bash(gh pr diff:*), Bash(gh pr list:*)
---

# Autonomous PR Review System

Multi-agent frontend code reviewer that classifies PR files, partitions work, and launches specialized agents in parallel.

## Usage

```bash
/dotcms-frontend-review <PR_NUMBER>
/dotcms-frontend-review <PR_URL>
```

## Review Process

### Stage 1: Fetch PR Data (Orchestrator — Inline)

Fetch the PR diff and file list. **Do NOT spawn an agent for this.**

```bash
# Get PR metadata
gh pr view <PR_NUMBER> --json title,body,headRefName,baseRefName,additions,deletions,changedFiles

# Get full diff
gh pr diff <PR_NUMBER>

# Get file list with stats
gh pr diff <PR_NUMBER> --name-only
```

### Stage 2: Classify Files (Orchestrator — Inline)

Classify every changed file by extension. **Do NOT spawn an agent for this.**

#### Classification Rules

| Pattern | Bucket | Reviewer |
|---------|--------|----------|
| `*.component.ts`, `*.component.html`, `*.component.scss`, `*.directive.ts`, `*.pipe.ts`, `*.service.ts` | **code** | `dotcms-code-reviewer` |
| `*.ts` (excluding `*.spec.ts` and Angular-specific above) | **code** | `dotcms-code-reviewer` |
| `*.scss`, `*.css` (standalone, not `.component.scss`) | **code** | `dotcms-code-reviewer` |
| `*.html` (standalone, not `.component.html`) | **code** | `dotcms-code-reviewer` |
| `*.spec.ts` | **test** | `dotcms-test-reviewer` |
| Everything else (`*.java`, `*.xml`, `*.json`, `*.md`, `*.yml`, etc.) | **out-of-scope** | None |

#### Frontend Detection

```
frontend_files = code_bucket.length + test_bucket.length
total_files = all changed files

If frontend_files > 50% of total_files → REVIEW
If frontend_files ≤ 50%                → SKIP (report to user and stop)
```

### Stage 3: Partition & Launch Agents

#### Code Reviewer Partitioning Formula

Based on the number of files in the **code** bucket:

```
1-15 files   → 1 code-reviewer instance
16-30 files  → 2 code-reviewer instances
31-45 files  → 3 code-reviewer instances
46+ files    → 4 code-reviewer instances (cap)
```

When partitioning files across multiple instances:
- Distribute files evenly across instances
- Keep related files together (e.g., `.component.ts` + `.component.html` + `.component.scss` from the same component go to the same instance)

#### Test Reviewer Partitioning Formula

Based on the number of files in the **test** bucket:

```
1-4 files    → 1 test-reviewer instance
5-8 files    → 2 test-reviewer instances
9+ files     → 3 test-reviewer instances (cap)
```

When partitioning files across multiple instances:
- Distribute files evenly across instances
- No grouping rules — each `.spec.ts` is independent

#### Agent Prompts

For each **code-reviewer** instance, include in the prompt:
1. The PR number and title
2. The **full diff chunks** for the files assigned to that instance
3. The file list for that instance

```
Agent(
    subagent_type="dotcms-code-reviewer",
    prompt="Review frontend code for PR #<NUMBER> '<TITLE>'.

Your assigned files:
<file-list>

PR diff for your files:
<diff-chunks-for-assigned-files>

Review Angular patterns, TypeScript type safety, and SCSS/HTML styling. Read the standards docs first. Use Read(core-web/**) for full file context when the diff is insufficient.",
    description="Code review (batch N)"
)
```

For each **test-reviewer** instance:

```
Agent(
    subagent_type="dotcms-test-reviewer",
    prompt="Review test quality for PR #<NUMBER> '<TITLE>'.

Test files:
<test-file-list>

PR diff for test files:
<diff-chunks-for-test-files>

Review Spectator patterns, coverage, and test quality. Read TESTING_REVIEW_RULES.md first. Use Read(core-web/**) for full file context when needed.",
    description="Test review (batch N)"
)
```

**Launch ALL agent instances in parallel** (code-reviewer instances + test-reviewer instances).

**Skip code-reviewer** if no code files in the PR.
**Skip test-reviewer** if no `.spec.ts` files in the PR.

### Stage 4: Validate & Consolidate

When all agents complete:

1. **Collect** all agent outputs
2. **Validate** findings:
   - Every file mentioned exists in the PR diff
   - Line numbers are within changed line ranges
   - No duplicate findings (if multiple code-reviewer instances flagged the same issue, keep the highest confidence)
   - No contradictions between agents
   - Each finding has a concrete file:line reference
3. **Merge** findings into a single flat list sorted by severity

### Stage 5: Structured Output

```markdown
# PR Review: #<NUMBER> - <TITLE>

## Summary
[2-3 sentence overview of changes and quality assessment]

**Files Reviewed**: <count> frontend files | **Risk Level**: <Low|Medium|High>

---

## Critical Issues 🔴 (95-100)

### 1. [Issue title] (Confidence: XX) [Angular|TypeScript|Styling|Test]
**File**: `path/to/file.ts:LINE`
**Issue**: Brief description
**Fix**: Concrete suggestion

[... more critical issues ...]

---

## Important Issues 🟡 (85-94)

### N. [Issue title] (Confidence: XX) [Domain]
**File**: `path/to/file.ts:LINE`
**Issue**: Brief description
**Fix**: Concrete suggestion

[... more important issues ...]

---

## Quality Issues 🔵 (75-84)

### N. [Issue title] (Confidence: XX) [Domain]
**File**: `path/to/file.ts:LINE`
**Issue**: Brief description
**Fix**: Concrete suggestion

[... more quality issues ...]

---

## Recommendation

**✅ Approve** | **⚠️ Approve with Comments** | **❌ Request Changes**

[Clear rationale]

- Critical: <count> | Important: <count> | Quality: <count>
```

## Error Handling

If PR fetch fails:
- Verify PR number: `gh pr list --limit 100`
- Check fork permissions
- Report: "Unable to fetch PR #<number>. Does it exist in this repo?"

If no frontend files changed:
- Report: "PR #<number> has no frontend files. Skipping review."

If an agent returns empty results:
- That's fine — it means no issues found in its domain

## Tips

- Run again after PR is updated to see if issues were addressed
- For urgent reviews: "This is blocking deployment, prioritize critical issues only"
- For large PRs (50+ files), the system automatically partitions work across up to 4 agents
