---
name: cicd-diagnostics
description: Diagnoses DotCMS CI/CD build failures in GitHub Actions workflows (PR, merge queue, trunk, nightly). Identifies failed tests, determines root causes (new vs flaky), compares workflow runs, checks known issues, and creates GitHub issues. Use when investigating build failures, checking workflow status, analyzing test flakiness, or debugging CI/CD pipeline issues.
---

# CI/CD Build Diagnostics

Diagnoses DotCMS CI/CD build failures efficiently using **AI-guided analysis** with utility-assisted data extraction.

## Design Philosophy

This skill follows an **AI-guided, utility-assisted** approach:

- **Utilities** handle data access, caching, and simple extraction (bash scripts)
- **AI** handles pattern recognition, classification, and reasoning (LLM capabilities)

**Why?**
- AI is better at recognizing new patterns and explaining reasoning
- Utilities are better at fast, cached data access
- This avoids brittle hardcoded classification logic

See [DESIGN_PHILOSOPHY.md](DESIGN_PHILOSOPHY.md) for complete details.

## Core Workflow Types

**cicd_1-pr.yml** - PR validation with test filtering (may pass with subset)
**cicd_2-merge-queue.yml** - Full test suite before merge (catches filtered tests)
**cicd_3-trunk.yml** - Post-merge deployment (uses artifacts, no test re-run)
**cicd_4-nightly.yml** - Scheduled full test run (detects flaky tests)

**Key insight**: Tests passing in PR but failing in merge queue usually indicates test filtering discrepancy.

## When to Use This Skill

### Primary Triggers (ALWAYS use skill):

**Run-Specific Analysis:**
- "Analyze [GitHub Actions URL]"
- "Diagnose https://github.com/dotCMS/core/actions/runs/[ID]"
- "What failed in run [ID]"
- "Debug run [ID]"
- "Fully analyze [URL]"
- "Check build [ID]"
- "Investigate run [ID]"
- "What went wrong in run [ID]"
- "Show me the failure for run [ID]"

**PR-Specific Investigation:**
- "What is the CI/CD failure for PR [number]"
- "What is the cicd failure for this PR [URL]"
- "What failed in PR [number]"
- "Check PR [number] CI status"
- "Analyze PR [number] failures"
- "Debug PR [URL]"
- "What's wrong with PR [number]"
- "Why did PR [number] fail"
- "Show PR [number] test failures"
- "What's blocking PR [number]"
- "PR [number] status"
- "Check pull request [number]"
- "Investigate PR [number] build"

**Workflow/Build Investigation:**
- "Why did the build fail?"
- "Why did the PR build fail?"
- "What's wrong with the CI?"
- "Check CI/CD status"
- "Debug [workflow-name] failure"
- "Investigate build failure"
- "What broke the build?"
- "Show me the build failure"
- "What's failing in CI?"
- "CI is broken"
- "Tests are failing"
- "Build is red"

**Comparative Analysis:**
- "Why did PR pass but merge queue fail?"
- "Compare PR and merge queue results"
- "What's blocking the merge queue?"
- "Why did this pass locally but fail in CI?"
- "Compare workflow results"
- "Why did [commit] fail in CI but pass locally"
- "Difference between PR and merge queue"

**Flaky Test Investigation:**
- "Is [test] flaky?"
- "Check test [test-name] reliability"
- "How often does [test] fail?"
- "Analyze flaky test [name]"
- "Why does [test] fail intermittently"
- "Test [name] keeps failing"
- "Flakiness of [test]"
- "Test stability for [name]"

**Nightly/Scheduled Build Analysis:**
- "Check nightly build status"
- "Why did nightly fail?"
- "Analyze nightly build"
- "What's failing in scheduled runs?"
- "Nightly build failure"
- "Show recent nightly results"
- "Is nightly passing?"
- "Scheduled build status"

**Merge Queue Investigation:**
- "Check merge queue health"
- "What's blocking the merge queue?"
- "Why is merge queue failing?"
- "Merge queue status"
- "What's wrong with the merge queue"
- "Show merge queue failures"
- "Is merge queue working?"

**Workflow Health Checks:**
- "Are CI builds passing?"
- "What's the CI status?"
- "Show recent build failures"
- "CI health check"
- "Build stability"
- "Recent CI failures"
- "Overall CI status"

### Context Indicators (Use when mentioned):

- User provides GitHub Actions run URL
- User mentions "CI", "build", "workflow", "pipeline", "tests failing in CI"
- User asks about specific workflow names (PR Check, merge queue, nightly, trunk)
- User mentions test failures in automated environments
- User asks about comparing different build results

### Don't Use Skill When:

- User asks about local test execution only
- User wants to run tests locally (use direct commands)
- User is debugging code logic (not CI failures)
- User asks about git operations unrelated to CI

## Diagnostic Workflow

### 0. Setup and Load Utilities

**CRITICAL**: All commands must run from repository root. Never use `cd` to change directories.

**CRITICAL**: Always use `bash` explicitly - user's default shell may be zsh which has incompatible syntax.

**CRITICAL**: Always use `bash -c` wrapper for any command using jq with `select()`, `contains()`, or sourcing utilities.

Initialize the diagnostic workspace:

```bash
# Use the init script to set up workspace
RUN_ID=19131365567
bash .claude/skills/cicd-diagnostics/init-diagnostic.sh "$RUN_ID"
# Outputs: WORKSPACE=/path/to/.claude/diagnostics/run-{RUN_ID}
```

**Available utilities** (sourced via bash -c when needed):
- **workspace.sh** - Diagnostic workspace with automatic caching
- **github-api.sh** - GitHub API wrappers for runs/jobs/logs
- **evidence.sh** - Evidence presentation for AI analysis (primary tool)
- **utilities.sh** - Helper functions (extraction, comparison, frequency)
- **log-analysis.sh** - Simple log pattern extraction

See [utils/README.md](utils/README.md) for function reference.

### 1. Identify Target and Create Workspace

**Extract run ID from URL or PR:**

```bash
# From URL: https://github.com/dotCMS/core/actions/runs/19131365567
RUN_ID=19131365567

# OR from PR number (extract RUN_ID from failed check URL)
PR_NUM=33711
gh pr view $PR_NUM --json statusCheckRollup \
    --jq '.statusCheckRollup[] | select(.conclusion == "FAILURE") | .detailsUrl' | head -1
# Extract RUN_ID from the URL output

# Workspace already created by init script in step 0
WORKSPACE="/Users/stevebolton/git/core2/.claude/diagnostics/run-${RUN_ID}"
```

### 2. Fetch Workflow Data (with caching)

Use helper scripts for guaranteed compatibility:

```bash
# Fetch metadata (uses caching)
bash .claude/skills/cicd-diagnostics/fetch-metadata.sh "$RUN_ID" "$WORKSPACE"

# Fetch jobs (uses caching)
bash .claude/skills/cicd-diagnostics/fetch-jobs.sh "$RUN_ID" "$WORKSPACE"

# Set file paths
METADATA="$WORKSPACE/run-metadata.json"
JOBS="$WORKSPACE/jobs-detailed.json"
```

### 3. Download Failed Job Logs

Use bash -c wrapper for jq commands with `select()` (zsh incompatible):

```bash
bash -c '
WORKSPACE="/Users/stevebolton/git/core2/.claude/diagnostics/run-19131365567"
JOBS="$WORKSPACE/jobs-detailed.json"

# Get first failed job ID and name
FAILED_JOB_ID=$(jq -r ".jobs[] | select(.conclusion == \"failure\") | .id" "$JOBS" | head -1)
FAILED_JOB_NAME=$(jq -r ".jobs[] | select(.conclusion == \"failure\") | .name" "$JOBS" | head -1)

echo "Analyzing failed job: $FAILED_JOB_NAME (ID: $FAILED_JOB_ID)"

# Download logs using helper script
bash .claude/skills/cicd-diagnostics/fetch-logs.sh "$WORKSPACE" "$FAILED_JOB_ID"
'
```

If analyzing a specific job by name (e.g., "CLI Tests"):

```bash
bash -c '
WORKSPACE="/Users/stevebolton/git/core2/.claude/diagnostics/run-19131365567"
JOBS="$WORKSPACE/jobs-detailed.json"

# Filter for specific job name
FAILED_JOB_ID=$(jq -r ".jobs[] | select(.conclusion == \"failure\") | select(.name | contains(\"CLI Tests\")) | .id" "$JOBS" | head -1)

echo "CLI Tests Job ID: $FAILED_JOB_ID"
bash .claude/skills/cicd-diagnostics/fetch-logs.sh "$WORKSPACE" "$FAILED_JOB_ID"
'
```

### 4. Present Evidence to AI (KEY STEP!)

**This is where AI-guided analysis begins.** Use `evidence.sh` to present raw data:

```bash
# Get complete diagnostic evidence package
source .claude/skills/cicd-diagnostics/utils/evidence.sh

# Check log size first
get_log_stats "$LOG_FILE"

# For large logs (>10MB), extract error sections only
if [ "$(wc -c < "$LOG_FILE")" -gt 10485760 ]; then
    echo "Large log detected - extracting error sections..."
    ERROR_FILE="$WORKSPACE/error-sections.txt"
    extract_error_sections_only "$LOG_FILE" "$ERROR_FILE"
    LOG_TO_ANALYZE="$ERROR_FILE"
else
    LOG_TO_ANALYZE="$LOG_FILE"
fi

# Present complete evidence package
present_complete_diagnostic "$LOG_TO_ANALYZE" > "$WORKSPACE/evidence.txt"

# Display evidence for AI analysis
cat "$WORKSPACE/evidence.txt"
```

**What this shows:**
- Failed tests (JUnit, E2E, Postman)
- Error messages with context
- Assertion failures (expected vs actual)
- Stack traces
- Timing indicators (timeouts, race conditions)
- Infrastructure indicators (Docker, DB, ES)
- First error context (for cascade detection)
- Failure timeline
- Known issues matching test name

### 5. AI Analysis (Natural Reasoning)

**Now AI analyzes the evidence and provides:**

1. **Root Cause Classification**
   - New failure vs flaky test vs infrastructure
   - Reasoning based on evidence patterns
   - Confidence level with explanation

2. **Test Fingerprint** (natural language)
   - Test name and location
   - Failure pattern (assertion type, timing, etc.)
   - Key identifiers for matching similar failures

3. **Known Issue Matching**
   - Compare with open GitHub issues
   - Match patterns from known flaky tests
   - Check if already tracked

4. **Impact Assessment**
   - Blocking vs non-blocking
   - PR vs merge queue differences
   - Frequency in recent runs

**Example AI Analysis:**

```markdown
## Failure Analysis

**Test**: ContentTypeCommandIT.Test_Command_Content_Filter_Order_By_modDate_Ascending
**Pattern**: Boolean flip assertion on modDate ordering
**Match**: Issue #33746 - modDate precision timing

**Classification**: Flaky Test (High Confidence)

**Reasoning**:
1. Test compares modDate ordering (second-level precision)
2. Assertion shows intermittent true/false flip
3. Exact match with documented issue #33746
4. Not a functional bug (would fail consistently)

**Fingerprint**:
- test: ContentTypeCommandIT.Test_Command_Content_Filter_Order_By_modDate_Ascending
- pattern: modDate-ordering
- assertion: boolean-flip
- line: 477
- known-issue: #33746

**Recommendation**: Known flaky test tracked in #33746. Fixes in progress.
```

### 6. Get Additional Context (if needed)

**For comparative analysis or frequency checks:**

```bash
# Get recent run history for workflow
WORKFLOW_NAME=$(jq -r '.workflowName' "$METADATA")
present_recent_runs "$WORKFLOW_NAME" 20 > "$WORKSPACE/recent-runs.txt"

# For PR vs Merge Queue comparison
if [[ "$WORKFLOW_NAME" == *"merge-queue"* ]]; then
    # Check if PR passed
    CURRENT_SHA=$(jq -r '.headSha' "$METADATA")
    PR_RESULT=$(gh run list --workflow=cicd_1-pr.yml --commit=$CURRENT_SHA --limit 1 --json conclusion -q '.[0].conclusion')

    if [ "$PR_RESULT" = "success" ]; then
        echo "⚠️ Test Filtering Issue: PR passed but merge queue failed"
        echo "This suggests test was filtered in PR but ran in merge queue"
    fi
fi

# For flaky test frequency
if [[ "$WORKFLOW_NAME" == *"nightly"* ]]; then
    echo "=== Nightly Build History ==="
    gh run list --workflow=cicd_4-nightly.yml --limit 30 \
        --json databaseId,conclusion,createdAt | \
        jq -r '.[] | "\(.databaseId) | \(.conclusion) | \(.createdAt)"' | \
        column -t -s '|'
fi
```

### 7. Generate Comprehensive Report

**AI writes report naturally** (not a template):

**CRITICAL**: Generate TWO separate reports:
1. **DIAGNOSIS.md** - User-facing failure diagnosis (no skill evaluation)
2. **ANALYSIS_EVALUATION.md** - Skill effectiveness evaluation (meta-analysis)

```bash
# Generate DIAGNOSIS.md (user-facing report)
cat > "$WORKSPACE/DIAGNOSIS.md" <<'EOF'
# CI/CD Failure Diagnosis - Run {RUN_ID}

**Analysis Date:** {DATE}
**Run URL:** {URL}
**Workflow:** {WORKFLOW_NAME}
**Event:** {EVENT_TYPE}
**Conclusion:** {CONCLUSION}
**Analyzed By:** cicd-diagnostics skill with AI-guided analysis

---

## Executive Summary
[2-3 sentence overview of the failure]

---

## Failure Details
[Specific failure information with line numbers and context]

### Failed Job
- **Name:** {JOB_NAME}
- **Job ID:** {JOB_ID}
- **Duration:** {DURATION}

### Specific Test Failure
- **Test:** {TEST_NAME}
- **Location:** Line {LINE_NUMBER}
- **Error Type:** {ERROR_TYPE}
- **Assertion:** {ASSERTION_MESSAGE}

---

## Root Cause Analysis

### Classification: **{CATEGORY}** ({CONFIDENCE} Confidence)

### Evidence Supporting Diagnosis
[Detailed evidence-based reasoning]

### Why This Is/Isn't a Code Defect
[Clear explanation]

---

## Test Fingerprint

**Natural Language Description:**
[Human-readable description of failure pattern]

**Matching Criteria for Future Failures:**
[How to identify similar failures]

---

## Impact Assessment

### Severity: **{SEVERITY}**

### Business Impact
- **Blocking:** {YES/NO}
- **False Positive:** {YES/NO}
- **Developer Friction:** {LEVEL}
- **CI/CD Reliability:** {IMPACT_DESCRIPTION}

### Frequency Analysis
[Historical failure data]

### Risk Assessment
[Risk levels for different categories]

---

## Recommendations

### Immediate Actions (Unblock)
1. [Specific action with command/link]

### Short-term Solutions (Reduce Issues)
2. [Solution with explanation]

### Long-term Improvements (Prevent Recurrence)
3. [Systemic improvement suggestion]

---

## Related Context

### GitHub Issues
[Related open/closed issues]

### Recent Workflow History
[Pattern analysis from recent runs]

### Related PR/Branch
[Context about what triggered this run]

---

## Diagnostic Artifacts

All diagnostic data saved to: `{WORKSPACE_PATH}`

### Files Generated
- `run-metadata.json` - Workflow run metadata
- `jobs-detailed.json` - All job details
- `failed-job-*.txt` - Complete job logs
- `error-sections.txt` - Extracted error sections
- `evidence.txt` - Structured evidence
- `DIAGNOSIS.md` - This report
- `ANALYSIS_EVALUATION.md` - Skill effectiveness evaluation

---

## Conclusion
[Final summary with action items]

**Action Required:**
1. [Priority action]
2. [Follow-up action]

**Status:** [Ready for retry | Needs code fix | Investigation needed]
EOF

# Generate ANALYSIS_EVALUATION.md (skill meta-analysis)
cat > "$WORKSPACE/ANALYSIS_EVALUATION.md" <<'EOF'
# Skill Effectiveness Evaluation - Run {RUN_ID}

**Purpose:** Meta-analysis of cicd-diagnostics skill performance for continuous improvement.

---

## Analysis Summary

- **Run Analyzed:** {RUN_ID}
- **Time to Diagnosis:** {DURATION}
- **Cached Data Used:** {YES/NO}
- **Evidence Size:** {LOG_SIZE} → {EXTRACTED_SIZE}
- **Classification:** {CATEGORY} ({CONFIDENCE} confidence)

---

## What Worked Well

### 1. {Category} ✅
[Specific success with examples]

### 2. {Category} ✅
[Specific success with examples]

[Additional categories as needed]

---

## AI Adaptive Analysis Strengths

The skill successfully demonstrated AI-guided analysis by:

1. **Natural Pattern Recognition**
   [How AI identified patterns without hardcoded rules]

2. **Contextual Reasoning**
   [How AI connected evidence to root cause]

3. **Cross-Reference Synthesis**
   [How AI linked to related issues/history]

4. **Confidence Assessment**
   [How AI provided reasoning for confidence level]

5. **Comprehensive Recommendations**
   [How AI generated actionable solutions]

**Key Insight:** The AI adapted to evidence rather than following rigid rules, enabling:
- [Specific capability 1]
- [Specific capability 2]
- [Specific capability 3]

---

## What Could Be Improved

### 1. {Area for Improvement}
- **Gap:** [What was missing]
- **Impact:** [Effect on analysis]
- **Suggestion:** [Specific improvement idea]

### 2. {Area for Improvement}
- **Gap:** [What was missing]
- **Impact:** [Effect on analysis]
- **Suggestion:** [Specific improvement idea]

[Additional areas as needed]

---

## Performance Metrics

### Speed
- **Data Fetching:** {TIME}
- **Evidence Extraction:** {TIME}
- **AI Analysis:** {TIME}
- **Total Duration:** {TIME}
- **vs Manual Analysis:** {COMPARISON}

### Accuracy
- **Root Cause Correct:** {YES/NO/PARTIAL}
- **Known Issue Match:** {YES/NO/PARTIAL}
- **Classification Accuracy:** {CONFIDENCE_LEVEL}

### Completeness
- [x] Identified specific failure point
- [x] Determined root cause with reasoning
- [x] Created natural test fingerprint
- [x] Assessed frequency/history
- [x] Checked known issues
- [x] Provided actionable recommendations
- [x] Saved diagnostic artifacts

---

## Design Validation

### AI-Guided Approach ✅/❌
[How well the evidence-driven AI analysis worked]

### Utility Functions ✅/❌
[How well the bash utilities performed]

### Caching Strategy ✅/❌
[How well the workspace caching worked]

---

## Recommendations for Skill Enhancement

### High Priority
1. [Specific improvement with rationale]
2. [Specific improvement with rationale]

### Medium Priority
3. [Specific improvement with rationale]
4. [Specific improvement with rationale]

### Low Priority
5. [Specific improvement with rationale]

---

## Comparison with Previous Approaches

### Before (Hardcoded Logic)
[Issues with rule-based classification]

### After (AI-Guided)
[Benefits of evidence-driven analysis]

### Impact
- **Accuracy:** [Improvement]
- **Flexibility:** [Improvement]
- **Maintainability:** [Improvement]

---

## Conclusion

[Overall assessment of skill effectiveness]

**Key Strengths:**
- [Strength 1]
- [Strength 2]
- [Strength 3]

**Areas for Growth:**
- [Area 1]
- [Area 2]

**Ready for production use:** {YES/NO}
**Recommended next steps:** [Action items]
EOF

echo "✅ Diagnosis complete: $WORKSPACE/DIAGNOSIS.md"
echo "✅ Evaluation saved: $WORKSPACE/ANALYSIS_EVALUATION.md"
```

**IMPORTANT**:
- **DIAGNOSIS.md** = User-facing failure analysis (what failed, why, how to fix)
- **ANALYSIS_EVALUATION.md** = Internal skill evaluation (how well the skill performed)
- DO NOT mix skill effectiveness evaluation into DIAGNOSIS.md
- Users should not see skill meta-analysis in their failure reports

### 8. Create Issue (if needed)

**AI determines if issue creation needed:**

```bash
# AI decides based on:
# - Is this already tracked? (check known issues)
# - Is this a new failure? (check recent history)
# - Is this blocking? (impact assessment)

if [ "$CREATE_ISSUE" = "yes" ]; then
    # AI generates issue content naturally
    gh issue create \
        --title "[CI/CD] Brief failure description" \
        --label "bug,ci-cd,Flakey Test" \
        --body "$(cat <<'ISSUE'
## Summary
[AI-written summary]

## Failure Evidence
[Key excerpts from evidence]

## Root Cause
[AI analysis]

## Reproduction
[Steps or patterns]

## Diagnostic Run
- Run ID: $RUN_ID
- Workspace: $WORKSPACE
- Full report: [link to DIAGNOSIS.md if committed]

## Recommendation
[Fix suggestions]
ISSUE
)"
fi
```

## Key Principles

### 1. Evidence-Driven Analysis

**Don't hardcode classification logic**. Present evidence and let AI reason:

❌ **Bad** (hardcoded):
```bash
if grep -q "modDate"; then
    echo "flaky_test"
fi
```

✅ **Good** (evidence-driven):
```bash
present_failure_evidence "$LOG_FILE"
# AI sees "modDate + boolean flip + issue #33746" and concludes "flaky test"
```

### 2. Progressive Investigation

Start specific, expand as needed:

1. **30 seconds**: Run status + failed job identification
2. **2 minutes**: Error evidence presentation
3. **5 minutes**: AI analysis with context
4. **10+ minutes**: Deep dive if patterns unclear

### 3. Workflow Context Matters

Different workflows = different failure implications:

- **PR failures**: Code issues OR filtered tests OR flaky tests
- **Merge queue failures**: Test filtering OR conflicts OR flaky tests
- **Trunk failures**: Deployment/artifact issues (tests don't re-run)
- **Nightly failures**: Flaky tests OR infrastructure degradation

### 4. Leverage Caching

Workspace automatically caches:
- Run metadata
- Job details
- Downloaded logs
- Evidence extraction

**Rerunning the skill uses cached data** (much faster!)

### 5. macOS Compatibility

**Never use `grep -P`** (Perl regex not available on macOS):

❌ **Bad**: `grep -oP '(?<=file=)[\w/.-]+(?=,)'`
✅ **Good**: `sed -E 's/.*file=([^,]+).*/\1/'` or let AI extract

## Output Format

Always provide:

1. **Executive Summary** (2-3 sentences)
2. **Root Cause** (category + confidence + reasoning)
3. **Evidence** (key excerpts from logs)
4. **Test Fingerprint** (natural language description)
5. **Impact Assessment** (blocking/frequency/risk)
6. **Recommendations** (immediate/short-term/long-term)
7. **Related Context** (issues, PRs, history)
8. **Diagnostic Artifacts** (workspace path)

## Success Criteria

✅ Identified specific failure point (job, step, test)
✅ Determined root cause with reasoning (not just category label)
✅ Created natural test fingerprint (not mechanical hash)
✅ Assessed frequency/history (new vs recurring)
✅ Checked known issues (searched GitHub)
✅ Provided actionable recommendations
✅ Saved diagnostic artifacts in workspace
✅ Generated comprehensive natural report

## Comparison with Old Approach

### Before (Hardcoded Logic)
```bash
# 100+ lines of pattern matching
detect_flaky_patterns() {
    if grep -qi "modDate"; then indicators+=("date_ordering"); fi
    if grep -E "expected: <true> but was: <false>"; then indicators+=("boolean_flip"); fi
    # ... 20 more hardcoded rules
}

classify_root_cause() {
    if [ "$has_known_issue" = true ]; then category="flaky_test"; fi
    # ... 50 more lines of brittle logic
}
```

**Problems:**
- Misses new patterns
- Can't explain reasoning
- Hard to maintain
- macOS incompatible

### After (AI-Guided)
```bash
# Present evidence to AI
present_complete_diagnostic "$LOG_FILE"

# AI analyzes and explains:
# "This is ContentTypeCommandIT with modDate ordering (line 477),
#  boolean flip assertion, matching known issue #33746.
#  Classification: Flaky Test (high confidence)"
```

**Benefits:**
- Recognizes new patterns
- Explains reasoning clearly
- Easy to maintain
- Works on all platforms
- More accurate

## Reference Files

For detailed information:
- [DESIGN_PHILOSOPHY.md](DESIGN_PHILOSOPHY.md) - AI-guided design approach
- [DESIGN_REVIEW.md](DESIGN_REVIEW.md) - Comprehensive architecture review
- [WORKFLOWS.md](WORKFLOWS.md) - Workflow descriptions and patterns
- [utils/README.md](utils/README.md) - Utility function reference
- [ISSUE_TEMPLATE.md](ISSUE_TEMPLATE.md) - Issue creation template
- [README.md](README.md) - Quick reference and examples