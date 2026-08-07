---
name: dot-cicd-diagnose
owner: "@dotcms/platform"
status: active
description: Use when a GitHub Actions workflow fails, PR build breaks, merge queue rejects, nightly reports failures, or user mentions CI/CD test failures in dotCMS/core. Also use for "check build", "diagnose run", "why did CI fail", "flaky test", "merge queue blocked".
---

# CI/CD Build Diagnostics

Diagnose DotCMS GitHub Actions failures as a senior platform engineer. Use diagnostic scripts for structured evidence gathering, triage before deep-diving, and stop when confident.

## Triage First (Critical — Baseline Gap)

Without this skill, agents dive straight into source code analysis — spending 20+ minutes and 100k+ tokens on what might be a known flaky test. **Always triage before investigating.**

```
Identify failure → Check known issues → THEN deep dive (only if needed)

Quick Win (30s-2min): Known issue? → Link and done.
Standard (2-10min):  Gather evidence → Hypothesize → Conclude.
Deep Dive (10+min):  Only for novel, unclear failures.
```

**Failure type determines tools, not a fixed sequence:**

| Failure Type | Start With | Skip |
|---|---|---|
| Test assertion | Known issues search, then code changes | External checks |
| Flaky test | Run history, timing patterns | Code deep-dive |
| Deployment/Auth | external_issues.py, WebSearch | Log analysis |
| Infrastructure | Recent runs, log patterns | Code changes |
| Skipped/missing jobs | Annotations (workflow YAML errors) | Logs |

## Workflow Types

- **cicd_1-pr.yml** — PR validation with test filtering (subset may pass)
- **cicd_2-merge-queue.yml** — Full suite before merge (catches filtered tests)
- **cicd_3-trunk.yml** — Post-merge deployment (artifacts, no re-test)
- **cicd_4-nightly.yml** — Scheduled full run (detects flaky tests)

PR passes + merge queue fails = test filtering discrepancy.

## Prerequisites

**This skill must be run from within a checkout of `dotCMS/core`** (any worktree is fine). Requires Python 3.8+ and GitHub CLI (`gh`) authenticated.

`diagnose.py` runs preflight checks automatically and will fail with actionable errors if anything is missing.

## Investigation Workflow

### 1. Gather Evidence

All operations go through `diagnose.py`. It handles preflight, workspace, caching, and structured output. All cached data is reused automatically on re-runs.

```bash
# Full gather (default) — metadata, jobs, annotations, logs, error summary
python3 .claude/skills/dot-cicd-diagnose/diagnose.py <RUN_ID_OR_URL>

# Progressive subcommands — use when you need specific data or want to save tokens
python3 .claude/skills/dot-cicd-diagnose/diagnose.py <ID> --metadata      # Metadata + jobs + step detail
python3 .claude/skills/dot-cicd-diagnose/diagnose.py <ID> --jobs          # Jobs + step detail only
python3 .claude/skills/dot-cicd-diagnose/diagnose.py <ID> --annotations   # Workflow annotations only
python3 .claude/skills/dot-cicd-diagnose/diagnose.py <ID> --logs          # Download logs + error summary
python3 .claude/skills/dot-cicd-diagnose/diagnose.py <ID> --logs <JOB_ID> # Single job log + errors
python3 .claude/skills/dot-cicd-diagnose/diagnose.py <ID> --evidence      # Full evidence.py analysis
python3 .claude/skills/dot-cicd-diagnose/diagnose.py <ID> --evidence <ID> # evidence.py on single job
```

**Recommended progression:**
1. Start with full gather (no flag) for most cases
2. If the output is enough to diagnose → stop
3. If you need deeper log analysis → `--evidence` or `--evidence <JOB_ID>`
4. If you need to re-check just one thing → use the specific subcommand

**Read the FULL output before proceeding to analysis.** Key signals:
- Steps marked `FAIL <- likely caused job failure` = primary root cause
- Steps marked `continue-on-error` = masked secondary issues (real bugs, report separately)
- `##[error]` lines that don't match any failed step = errors from `continue-on-error` steps

**Do NOT write inline `python3 -c` to parse jobs JSON or extract errors.** `diagnose.py` already provides all of this. Ad-hoc parsing causes bugs and misses `continue-on-error` signals.

**Do NOT run ad-hoc `gh run view`, `gh api`, or `gh run list` commands to re-fetch data that `diagnose.py` already provided.** The job list, step details, and error summaries are all in the output. Running separate `gh` commands wastes tokens, triggers permission prompts, and often misses the structured signals (like `continue-on-error` detection) that `diagnose.py` provides. Only use direct `gh` commands for data that `diagnose.py` does not cover (e.g., comparing against other runs, checking PR info, searching issues).

### 2. Check Known Issues (Before Deep Dive!)

**This is the step the baseline skipped.** Search before re-investigating from scratch:

```bash
# Search by test name or error message
gh issue list --repo dotCMS/core --search "TestClassName" --state all --limit 10 \
  --json number,title,state,labels
gh issue list --repo dotCMS/core --label "Flakey Test" --state all --limit 20
```

If match found → link to issue, assess if new information, and stop.

**For deployment/auth errors**, check external services:

```python
import sys; from pathlib import Path
sys.path.insert(0, str(Path(".claude/skills/dot-cicd-diagnose/utils")))
from external_issues import extract_error_indicators, format_external_issue_report, generate_search_queries
log_content = Path("$WORKSPACE/failed-job-$JOB_ID.txt").read_text(errors='ignore')
indicators = extract_error_indicators(log_content)
print(format_external_issue_report(indicators, generate_search_queries(indicators, "DATE"), []))
```

### 3. Present Evidence for Analysis

Use `evidence.py` to extract structured failure data from logs:

```python
import sys; from pathlib import Path
sys.path.insert(0, str(Path(".claude/skills/dot-cicd-diagnose/utils")))
from evidence import present_complete_diagnostic, get_log_stats
LOG = Path("$WORKSPACE/failed-job-$JOB_ID.txt")
print(get_log_stats(LOG))
print(present_complete_diagnostic(LOG))
```

This extracts: failed tests, error messages, assertion failures, stack traces, timing indicators, infrastructure events, cascade detection, known issue matches.

### 4. Analyze (Evidence-Based)

Form **competing hypotheses** and evaluate each against evidence:
- **Code defect** — New bug from recent PR changes?
- **Flaky test** — Race condition, timing, shared state?
- **Infrastructure** — Docker/DB/ES environment issue?
- **Test filtering** — PR subset passed, full suite failed?
- **Cascading failure** — One primary error causing secondary failures?

**Before concluding root cause, verify the error actually failed the job.** `diagnose.py` flags steps where `continue-on-error` is detectable from the API (step failed but later non-cleanup steps succeeded). Look at the step marked `FAIL ← likely caused job failure` for the actual root cause.

**WARNING: `continue-on-error` steps may show `conclusion: success` in the API even when they have internal errors.** GitHub masks the failure — the step reports success but error messages are still in the raw logs. For trunk deployment jobs, check [WORKFLOWS.md](WORKFLOWS.md) for which steps have `continue-on-error` (notably CLI Deploy / deploy-jfrog). When you see `##[error]` lines in logs that don't correspond to any failed step in `diagnose.py` output, that's a masked `continue-on-error` error. Report these as **secondary findings** — they are real bugs being hidden, not the cause of the run failure, but worth flagging.

Label each finding: **FACT** (logs show it), **HYPOTHESIS** (theory), **CONFIDENCE** (High/Medium/Low).

See [REFERENCE.md](REFERENCE.md) for detailed diagnostic patterns (timing analysis, thread context, concurrency).

### 5. Compare Runs (If Needed)

```python
from evidence import present_recent_runs
print(present_recent_runs("cicd_1-pr.yml", 20))  # Check if intermittent
```

### 6. Report

Write DIAGNOSIS.md to the diagnostic workspace directory (e.g., `.claude/diagnostics/run-<RUN_ID>/DIAGNOSIS.md`). **Never write to the project root.** Natural language, like a senior engineer writing to a colleague.

**Always include:** Executive Summary, Root Cause (with confidence), Evidence, Recommendations.
**Include when relevant:** Known Issues, Timeline, Test Fingerprint, Impact Assessment, Competing Hypotheses.

Do not force sections that add no value. See [REFERENCE.md](REFERENCE.md) for templates.

### 7. Create Issue (If Warranted)

Only when: not already tracked, new failure pattern, blocking development, actionable.

```bash
gh issue create --repo dotCMS/core --title "[CI/CD] Brief description" \
  --label "bug,ci-cd,Flakey Test" --body "$(cat $WORKSPACE/DIAGNOSIS.md)"
```

## Key Principles

1. **Triage first** — Check known issues before deep investigation. Most failures are known.
2. **Adaptive depth** — Stop when confident. A known flaky test needs 2 min, not 20.
3. **Evidence-driven** — Present evidence to AI reasoning, don't hardcode classification rules.
4. **Context matters** — Same error means different things in different workflows.
5. **Use the scripts** — Workspace caching means re-runs are fast. Ad-hoc `gh` commands waste tokens.
6. **Never skip a failed step** — If `diagnose.py` errors, diagnose why and fix it before continuing. Do not rationalize past it ("I'll just use gh directly", "I don't really need that data"). Proceeding without evidence produces guesswork, not diagnosis.

## Reference Files

- **[REFERENCE.md](REFERENCE.md)** — Diagnostic patterns, report templates, collaboration examples
- **[WORKFLOWS.md](WORKFLOWS.md)** — Workflow descriptions and CI/CD pipeline details
- **[LOG_ANALYSIS.md](LOG_ANALYSIS.md)** — Advanced log analysis techniques
- **[utils/README.md](utils/README.md)** — Utility function API reference
- **[ISSUE_TEMPLATE.md](ISSUE_TEMPLATE.md)** — Issue creation template