---
name: cicd-diagnostics
description: Diagnoses DotCMS CI/CD build failures in GitHub Actions workflows (PR, merge queue, trunk, nightly). Identifies failed tests, determines root causes (new vs flaky), compares workflow runs, checks known issues, and creates GitHub issues. Use when investigating build failures, checking workflow status, analyzing test flakiness, or debugging CI/CD pipeline issues.
---

# CI/CD Build Diagnostics

Diagnoses DotCMS CI/CD build failures efficiently using GitHub CLI and API.

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

## Diagnostic Approach

### 0. Setup and Load Utilities

**IMPORTANT**: Source utility functions at the start of diagnostic:

```bash
# Load utility functions
source .claude/skills/cicd-diagnostics/utils/github-api.sh
source .claude/skills/cicd-diagnostics/utils/workspace.sh
source .claude/skills/cicd-diagnostics/utils/log-analysis.sh
source .claude/skills/cicd-diagnostics/utils/analysis.sh

# Create or reuse diagnostic workspace (with automatic caching)
DIAGNOSTIC_DIR=$(get_diagnostic_workspace "$RUN_ID")

# Ensure .gitignore is configured
ensure_gitignore_diagnostics

# All subsequent operations use this directory
# Advantages:
# - Easy manual review of logs later
# - Persists across sessions
# - No conflicts with other runs
# - Automatic caching of previous diagnostics
# - Already gitignored (.claude/diagnostics/)
```

**Available Utilities:**
- **github-api.sh** - GitHub API wrappers, run/PR/issue fetching
- **workspace.sh** - Diagnostic workspace management with caching
- **log-analysis.sh** - Error pattern extraction from logs
- **analysis.sh** - Failure classification and recommendations

See [utils/README.md](utils/README.md) for complete function reference.

### 1. Identify Target

**Specific run URL provided:**
```bash
# Extract run ID: https://github.com/dotCMS/core/actions/runs/19131365567
RUN_ID=19131365567

# Check for existing diagnostic directory from previous session
EXISTING_DIR=$(find .claude/diagnostics -maxdepth 1 -type d -name "run-${RUN_ID}-*" 2>/dev/null | head -1)

if [ -n "$EXISTING_DIR" ]; then
  DIAGNOSTIC_DIR="$EXISTING_DIR"
  echo "✓ Found existing diagnostic session: $DIAGNOSTIC_DIR"
  echo "  (Logs and metadata will be reused from previous analysis)"
else
  # Create new workspace
  DIAGNOSTIC_DIR=".claude/diagnostics/run-${RUN_ID}-$(date +%Y%m%d-%H%M%S)"
  mkdir -p "$DIAGNOSTIC_DIR"
  echo "✓ Created new diagnostic workspace: $DIAGNOSTIC_DIR"
fi

# Get run metadata (only if not already cached)
if [ ! -f "$DIAGNOSTIC_DIR/run-metadata.json" ]; then
  gh run view $RUN_ID --json conclusion,status,event,headBranch,headSha,workflowName,url,createdAt,updatedAt,displayTitle > "$DIAGNOSTIC_DIR/run-metadata.json"
fi
cat "$DIAGNOSTIC_DIR/run-metadata.json" | jq '.'
```

**PR URL or number provided:**
```bash
# Extract PR number: https://github.com/dotCMS/core/pull/33711 or just "33711"
PR_NUM=33711

# Get PR details including latest commit
gh pr view $PR_NUM --json number,headRefOid,headRefName,title,author,statusCheckRollup > pr-info.json

# Find the most recent failed workflow run for this PR
FAILED_RUN=$(jq -r '.statusCheckRollup[] | select(.conclusion == "FAILURE" and .workflowName == "-1 PR Check") | .detailsUrl' pr-info.json | head -1)

if [ -n "$FAILED_RUN" ]; then
  # Extract run ID from URL: https://github.com/dotCMS/core/actions/runs/19118302390/...
  RUN_ID=$(echo "$FAILED_RUN" | grep -oP 'runs/\K[0-9]+')
  echo "Found failed run: $RUN_ID"

  # Continue with run analysis using existing logic...
else
  echo "No failed runs found for PR $PR_NUM"
  # Show all check runs for debugging
  jq -r '.statusCheckRollup[] | "\(.name): \(.conclusion // .state)"' pr-info.json
fi
```

**Current branch PR (no URL):**
```bash
CURRENT_BRANCH=$(git branch --show-current)
gh pr list --head "$CURRENT_BRANCH" --json number,url,headRefOid,title,author > pr-info.json

# Get PR workflow runs for commit
HEAD_SHA=$(jq -r '.[0].headRefOid' pr-info.json)
gh run list --workflow=cicd_1-pr.yml --commit=$HEAD_SHA --limit 5 --json databaseId,conclusion,status,displayTitle
```

**Overall workflow health check:**
```bash
# Check recent workflow health
gh run list --workflow=cicd_2-merge-queue.yml --limit 10 --json databaseId,conclusion,displayTitle,createdAt > merge-queue-status.json
gh run list --workflow=cicd_4-nightly.yml --limit 5 --json databaseId,conclusion,displayTitle,createdAt > nightly-status.json
```

### 2. Analyze Failed Jobs

```bash
# Get ALL jobs with detailed step information
gh api "/repos/dotCMS/core/actions/runs/$RUN_ID/jobs" \
  --jq '.jobs[] | {name, id, conclusion, status, started_at, completed_at, steps: [.steps[] | select(.conclusion == "failure") | {name, number, conclusion}]}' \
  > "$DIAGNOSTIC_DIR/jobs-detailed.json"

# Extract just failed jobs for quick view
jq '.jobs[] | select(.conclusion == "failure")' "$DIAGNOSTIC_DIR/jobs-detailed.json"

# List cancelled jobs (may indicate cascading failures)
jq '.jobs[] | select(.conclusion == "cancelled")' "$DIAGNOSTIC_DIR/jobs-detailed.json"

# Check artifacts (test reports, logs)
gh api "/repos/dotCMS/core/actions/runs/$RUN_ID/artifacts" \
  --jq '.artifacts[] | {name, id, size_in_bytes, expired}' \
  > "$DIAGNOSTIC_DIR/artifacts.json"

# Find test-related artifacts
jq 'select(.name | contains("test-results") or contains("build-reports-test"))' "$DIAGNOSTIC_DIR/artifacts.json"
```

### 3. Efficient Log Analysis

**CRITICAL**: Logs can be 100MB+. Use progressive approach with caching:

```bash
# Step 1: Get logs for ONLY failed jobs (not all jobs)
FAILED_JOB_ID=$(jq -r '.jobs[] | select(.conclusion == "failure") | .id' "$DIAGNOSTIC_DIR/jobs-detailed.json" | head -1)

# Check if logs already exist from previous session
LOG_FILE="$DIAGNOSTIC_DIR/failed-job-${FAILED_JOB_ID}.txt"
if [ -f "$LOG_FILE" ] && [ -s "$LOG_FILE" ]; then
  echo "✓ Using cached logs from previous session: $LOG_FILE"
else
  echo "Downloading logs for failed job $FAILED_JOB_ID..."
  gh api "/repos/dotCMS/core/actions/jobs/$FAILED_JOB_ID/logs" > "$LOG_FILE"
  echo "✓ Logs saved to: $LOG_FILE ($(wc -c < "$LOG_FILE" | numfmt --to=iec-i)B)"
fi

# Step 2: Search for error patterns (priority order)
echo "=== BUILD FAILURES ===" > "$DIAGNOSTIC_DIR/error-summary.txt"
grep -E "\[ERROR\]|BUILD FAILURE" "$DIAGNOSTIC_DIR/failed-job-${FAILED_JOB_ID}.txt" | head -100 >> "$DIAGNOSTIC_DIR/error-summary.txt"

echo -e "\n=== TEST FAILURES ===" >> "$DIAGNOSTIC_DIR/error-summary.txt"
grep -E "(<<< FAILURE!|Tests run:.*Failures: [1-9]|::error file=)" "$DIAGNOSTIC_DIR/failed-job-${FAILED_JOB_ID}.txt" | head -100 >> "$DIAGNOSTIC_DIR/error-summary.txt"

echo -e "\n=== TIMEOUT/INFRASTRUCTURE ===" >> "$DIAGNOSTIC_DIR/error-summary.txt"
grep -iE "(timeout|connection refused|rate limit|Process exited with an error)" "$DIAGNOSTIC_DIR/failed-job-${FAILED_JOB_ID}.txt" | head -50 >> "$DIAGNOSTIC_DIR/error-summary.txt"

# Step 3: Display summary
cat "$DIAGNOSTIC_DIR/error-summary.txt"
```

**E2E/Playwright Test Failures**:
```bash
# E2E tests have detailed error annotations
grep "::error file=" "$DIAGNOSTIC_DIR/failed-job-${FAILED_JOB_ID}.txt" | \
  sed 's/%0A/\n/g' > "$DIAGNOSTIC_DIR/e2e-failures.txt"

# Extract test summary
grep -E "(passed|failed|flaky|Assertion failed)" "$DIAGNOSTIC_DIR/failed-job-${FAILED_JOB_ID}.txt" | \
  tail -50 > "$DIAGNOSTIC_DIR/test-summary.txt"
```

**Pattern priority:**
1. Maven `[ERROR]` and `BUILD FAILURE` (most critical)
2. Test patterns: `::error file=`, `<<< FAILURE!`, `Tests run:.*Failures:`
3. E2E: `locator.waitFor:`, `Test timeout`, `Assertion failed`
4. Infrastructure: `timeout`, `connection refused`, `Process exited with an error`
5. Dependencies: `Could not resolve`, `artifact not found`

### 4. Root Cause Classification

**New Failure** - Compare with recent runs:
```bash
WORKFLOW_NAME=$(jq -r '.workflowName' "$DIAGNOSTIC_DIR/run-metadata.json")

# Get recent run history
gh run list --workflow="$WORKFLOW_NAME" --limit 20 \
  --json databaseId,conclusion,headSha,displayTitle,createdAt \
  > "$DIAGNOSTIC_DIR/recent-runs.json"

# Find last successful run
LAST_SUCCESS_SHA=$(jq -r '.[] | select(.conclusion == "success") | .headSha' "$DIAGNOSTIC_DIR/recent-runs.json" | head -1)
CURRENT_SHA=$(jq -r '.headSha' "$DIAGNOSTIC_DIR/run-metadata.json")

# Compare commits if available
if [ -n "$LAST_SUCCESS_SHA" ] && [ "$LAST_SUCCESS_SHA" != "$CURRENT_SHA" ]; then
  gh api "/repos/dotCMS/core/compare/$LAST_SUCCESS_SHA...$CURRENT_SHA" \
    --jq '.commits[] | {sha: .sha[:7], message: .commit.message, author: .commit.author.name}' \
    > "$DIAGNOSTIC_DIR/commits-since-success.json"
fi
```

**Flaky Test** - Check historical patterns:
```bash
# Check nightly build history (detects flaky tests)
gh run list --workflow=cicd_4-nightly.yml --limit 30 \
  --json databaseId,conclusion,createdAt \
  > "$DIAGNOSTIC_DIR/nightly-history.json"

# Calculate failure rate
TOTAL=$(jq '. | length' "$DIAGNOSTIC_DIR/nightly-history.json")
FAILURES=$(jq '[.[] | select(.conclusion == "failure")] | length' "$DIAGNOSTIC_DIR/nightly-history.json")
echo "Nightly failure rate: $FAILURES/$TOTAL" >> "$DIAGNOSTIC_DIR/error-summary.txt"
```

**Test Filtering Issue** - Compare PR vs merge queue:
```bash
# If PR passed but merge queue failed
PR_RESULT=$(gh run list --workflow=cicd_1-pr.yml --commit=$CURRENT_SHA --limit 1 --json conclusion -q '.[0].conclusion')
MQ_RESULT=$(gh run list --workflow=cicd_2-merge-queue.yml --commit=$CURRENT_SHA --limit 1 --json conclusion -q '.[0].conclusion')

if [ "$PR_RESULT" = "success" ] && [ "$MQ_RESULT" = "failure" ]; then
  echo "⚠️ Test Filtering Issue: PR passed but merge queue failed" >> "$DIAGNOSTIC_DIR/error-summary.txt"
  echo "This usually indicates a test was filtered in PR but ran in merge queue" >> "$DIAGNOSTIC_DIR/error-summary.txt"
fi
```

**Infrastructure Issue** - Check external factors:
```bash
# Look for infrastructure patterns
echo -e "\n=== INFRASTRUCTURE INDICATORS ===" >> "$DIAGNOSTIC_DIR/error-summary.txt"
grep -iE "elasticsearch.*exception|database.*error|docker.*failed|container.*exit" "$DIAGNOSTIC_DIR/failed-job-${FAILED_JOB_ID}.txt" | head -20 >> "$DIAGNOSTIC_DIR/error-summary.txt"
```

### 5. Check Known Issues

```bash
# Search for known issues related to test failures
# Extract test name from error summary
TEST_NAME=$(grep -oP '(?<=file=)[\w/.-]+(?=,)' "$DIAGNOSTIC_DIR/error-summary.txt" | head -1 | xargs basename .spec.ts)

if [ -n "$TEST_NAME" ]; then
  # Search for related issues
  gh issue list --search "\"$TEST_NAME\" in:title,body" \
    --json number,title,state,labels,createdAt \
    --limit 10 > "$DIAGNOSTIC_DIR/related-issues.json"

  # Show results
  echo -e "\n=== RELATED ISSUES ===" >> "$DIAGNOSTIC_DIR/error-summary.txt"
  jq -r '.[] | "#\(.number): \(.title) [\(.state)]"' "$DIAGNOSTIC_DIR/related-issues.json" >> "$DIAGNOSTIC_DIR/error-summary.txt"
fi

# Check for flaky test issues
gh issue list --label "Flakey Test" --state open \
  --json number,title,labels \
  --limit 10 > "$DIAGNOSTIC_DIR/flaky-tests.json"

# Check for CI/CD infrastructure issues
gh issue list --label "ci-cd" --state open \
  --json number,title,labels \
  --limit 10 > "$DIAGNOSTIC_DIR/cicd-issues.json"
```

### 6. Get PR Context

```bash
# Get PR details if this is a PR run
HEAD_BRANCH=$(jq -r '.headBranch' "$DIAGNOSTIC_DIR/run-metadata.json")

if [[ "$HEAD_BRANCH" =~ ^issue-([0-9]+) ]]; then
  ISSUE_NUM=${BASH_REMATCH[1]}

  # Get issue context
  gh issue view $ISSUE_NUM \
    --json title,body,labels,author \
    > "$DIAGNOSTIC_DIR/pr-issue-context.json"

  # Get PR if exists
  gh pr list --head "$HEAD_BRANCH" --limit 1 \
    --json number,title,author,commits \
    > "$DIAGNOSTIC_DIR/pr-context.json"
fi
```

### 7. Generate Comprehensive Report

Create structured analysis report:

```bash
cat > "$DIAGNOSTIC_DIR/DIAGNOSIS.md" <<'EOF'
## CI/CD Failure Diagnosis: [workflow] #[run-id]

**Root Cause**: [Category] - [Brief explanation]
**Confidence**: [High/Medium/Low]

---

### Executive Summary

[2-3 sentence overview of what failed and why]

**Key Point**: [Most important finding]

---

### Workflow Details

- **Run ID**: [id]
- **Workflow**: [name]
- **Trigger**: [event]
- **Branch**: [branch]
- **Commit**: [sha]
- **Author**: [author]
- **Duration**: [duration]
- **Timestamp**: [timestamp]

---

### Job Status Summary

**✅ Successful Jobs** ([count]):
- [list]

**❌ Failed Jobs** ([count]):
- [list with details]

**⏸️ Canceled Jobs** ([count]):
- [list]

---

### Failure Analysis

#### Primary Failure: [Job Name]

**Test Results**: [X failed / Y passed (duration)]

**Failed Tests**:
1. **`[test-spec]`**
   - **Failure Pattern**: `[error-type]`
   - **Root Cause**: [explanation]
   - **Specific Issues**:
     - [detail 1]
     - [detail 2]

**Error Patterns**:
```
[key error messages]
```

**Infrastructure Observations**:
- [observation 1]
- [observation 2]

---

### Root Cause Classification

**Type**: **[New Failure | Flaky Test | Infrastructure | Test Filtering]**

**Evidence**:
1. [evidence point 1]
2. [evidence point 2]
3. [evidence point 3]

**Frequency**: [Once | Intermittent | Consistent] ([X/Y recent runs])

---

### Impact Assessment

**Build Impact**: [❌ Blocking | ⚠️ Warning | ✅ Advisory]

**Code Quality Impact**: [assessment]

**Risk Level**: **[High | Medium | Low]** - [explanation]

---

### Recommendations

#### Immediate Actions

1. **[Primary action]**
   ```bash
   [command or link]
   ```

2. **[Alternative action]**

#### Short-term Solutions

3. **[Solution 1]**

4. **[Solution 2]**

#### Long-term Solutions

5. **[Improvement 1]**

6. **[Improvement 2]**

---

### Related Issues & Context

- **Known Issue**: #[number] - [title]
- **PR Issue**: #[number] - [title]
- **Recent Successful Run**: #[id] ([date])
- **Nightly Build Status**: [status]

---

### Workflow URL
[url]

---

### Conclusion

[Summary and recommended action]

---

### Diagnostic Artifacts

All analysis files saved to: `$DIAGNOSTIC_DIR`

- `run-metadata.json` - Run details
- `jobs-detailed.json` - All jobs and steps
- `failed-job-*.txt` - Complete logs
- `error-summary.txt` - Extracted errors
- `related-issues.json` - Known issues
- `DIAGNOSIS.md` - This report
EOF

echo -e "\n✅ Diagnosis complete. Review: $DIAGNOSTIC_DIR/DIAGNOSIS.md"
```

### 8. Create Issue (if needed)

```bash
# Only create if: new failure not tracked, or need to document flaky test
if [ -z "$(jq -r '.[] | select(.state == "OPEN")' "$DIAGNOSTIC_DIR/related-issues.json")" ]; then
  # No open issue exists, consider creating one
  gh issue create \
    --title "[CI/CD] [Brief description]" \
    --label "bug,ci-cd,[workflow-label]" \
    --body-file "$DIAGNOSTIC_DIR/ISSUE_TEMPLATE.md"
fi
```

## Key Principles

### Efficiency First

**Progressive Investigation Depth**:
1. **30 seconds**: Run status + failed job identification
2. **2 minutes**: Error pattern extraction from failed job logs
3. **5 minutes**: Full analysis with context and comparisons
4. **10+ minutes**: Deep dive only if patterns unclear

**Start Specific, Expand as Needed**:
- Focus on failed jobs only (not all jobs)
- Get logs for failed jobs only (not entire workflow)
- Search for specific error patterns (not reading everything)
- Compare with recent runs only if root cause unclear

### Workflow Context Matters

Different workflows have different failure implications:

- **PR failures** = Code issues OR filtered tests OR flaky tests
- **Merge queue failures** = Test filtering discrepancy OR conflicts OR flaky tests
- **Trunk failures** = Deployment/artifact issues (tests don't re-run)
- **Nightly failures** = Flaky tests OR infrastructure degradation

### Always Consider

- **Test filtering differences**: PR runs subset, merge queue runs all
- **Multiple PRs in queue**: Can cause conflicts/race conditions
- **Infrastructure status**: GitHub Actions, Elasticsearch, Database
- **Historical patterns**: Is this new or recurring?
- **PR code changes**: What areas of code were modified?

### Diagnostic Workspace Benefits

Using `.claude/diagnostics/run-[ID]-[timestamp]/`:
- ✅ Easy manual review after skill completes
- ✅ Persists across Claude sessions
- ✅ No conflicts (timestamped directories)
- ✅ Already gitignored
- ✅ Organized by run ID
- ✅ Can compare multiple runs side-by-side

## Output Format

Always provide:

1. **Executive Summary** (2-3 sentences)
2. **Root Cause** (specific category + confidence)
3. **Evidence** (log excerpts, patterns, comparisons)
4. **Recommendations** (immediate, short-term, long-term)
5. **Related Context** (issues, PRs, recent runs)
6. **Diagnostic Path** (where to find detailed files)

## Success Criteria

✅ Identified specific failure point (job, step, test)
✅ Determined root cause category (New/Flaky/Infrastructure/Filtering)
✅ Assessed if new vs recurring (compared with history)
✅ Checked for known issues (searched GitHub issues)
✅ Provided actionable next steps (with commands/links)
✅ Saved diagnostic artifacts for review
✅ Created comprehensive report in DIAGNOSIS.md
✅ Documented skill improvement suggestions (if any)

## Self-Improvement Protocol

After completing each diagnosis, the skill should reflect on its effectiveness and suggest improvements:

### Track Diagnostic Challenges

Document in `$DIAGNOSTIC_DIR/SKILL_IMPROVEMENTS.md`:

```bash
cat > "$DIAGNOSTIC_DIR/SKILL_IMPROVEMENTS.md" <<'EOF'
# Skill Improvement Suggestions

## Challenges Encountered

[Document any difficulties during this diagnosis]

### Examples:
- Pattern not matched: [specific error pattern that wasn't caught]
- Inefficient process: [step that took too long or was redundant]
- Missing trigger: [phrase/context that should have triggered skill but didn't]
- Data not found: [expected information that was unavailable]
- Better approach: [more efficient method discovered during diagnosis]

## Suggested Improvements

### 1. [Improvement Category]

**Problem**: [What made diagnosis difficult]

**Suggestion**: [Specific change to SKILL.md]

**Location**: [Section to update]

**Code/Pattern**:
```
[New pattern, trigger phrase, or bash command]
```

### 2. [Next improvement]

[Repeat pattern above]

## Apply Improvements?

- [ ] Yes, update skill with these suggestions
- [ ] No, skip for now
- [ ] Partial (specify which ones)

EOF
```

### Present Improvements to User

At the end of each diagnostic report, include:

```markdown
---

## 🔧 Skill Self-Assessment

During this diagnosis, I identified [N] potential improvements to the cicd-diagnostics skill:

1. **[Improvement 1 Title]**
   - Challenge: [what was difficult]
   - Suggestion: [specific improvement]

2. **[Improvement 2 Title]**
   - Challenge: [what was difficult]
   - Suggestion: [specific improvement]

Full details saved to: `$DIAGNOSTIC_DIR/SKILL_IMPROVEMENTS.md`

**Would you like me to apply these improvements to the skill?**
```

### Common Improvement Categories

Track and suggest improvements in these areas:

1. **Trigger Phrase Expansion**
   - User asked for diagnosis but skill wasn't triggered
   - New patterns of user questions discovered

2. **Error Pattern Enhancement**
   - New error formats not currently recognized
   - Missing grep patterns for specific test frameworks
   - Infrastructure errors not in current list

3. **Efficiency Optimizations**
   - Redundant API calls identified
   - More efficient bash command discovered
   - Better jq query pattern found

4. **Context Detection**
   - Workflow types not properly identified
   - Missing classification criteria
   - New failure patterns discovered

5. **Report Template Updates**
   - Missing sections that would be valuable
   - Better ways to present information
   - Additional context that should always be included

### Example Self-Assessment

```markdown
## 🔧 Skill Self-Assessment

During this diagnosis, I identified 2 potential improvements:

1. **E2E Playwright Error Pattern Detection**
   - Challenge: Had to manually search for `sed 's/%0A/\n/g'` pattern for E2E failures
   - Suggestion: Add dedicated E2E error extraction to section 3 of SKILL.md
   - Impact: Would save 1-2 minutes on E2E test failures

2. **Flaky Test Label Search**
   - Challenge: "Flakey Test" label has inconsistent spelling (also "Flaky Test")
   - Suggestion: Update search to use both spellings: `--search "label:\"Flakey Test\" OR label:\"Flaky Test\""`
   - Impact: More comprehensive known issue detection

Would you like me to update the cicd-diagnostics skill with these improvements?
```

### Applying Improvements

If user approves, update SKILL.md:

```bash
# 1. Read current SKILL.md
# 2. Apply suggested changes
# 3. Test changes don't break existing functionality
# 4. Commit with message: "chore(skill): improve cicd-diagnostics based on run [ID] analysis"
```

## Reference Files

For detailed information:
- [WORKFLOWS.md](WORKFLOWS.md) - Complete workflow descriptions and failure patterns
- [LOG_ANALYSIS.md](LOG_ANALYSIS.md) - Advanced log analysis techniques and patterns
- [ISSUE_TEMPLATE.md](ISSUE_TEMPLATE.md) - Issue creation template
- [README.md](README.md) - Quick reference and examples