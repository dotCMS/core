# Troubleshooting

## Common Issues and Solutions

### 🚨 **Quick Issue Identifier**

**Use this flowchart to identify your issue:**
```
❓ What's happening?
├── 🔇 Workflow not running at all
│   ├── Check: Syntax errors → Run yamllint
│   ├── Check: File location → Verify .github/workflows/
│   └── Check: Repository settings → Workflow disabled?
├── ⏭️ Jobs being skipped
│   ├── Check: Environment variables → CICD_SKIP_TESTS=true?
│   ├── Check: Change detection → Files match filters.yaml?
│   └── Check: Conditional logic → Job conditions false?
├── 💥 Jobs failing
│   ├── Check: Dependencies → Previous jobs failed?
│   ├── Check: Permissions → Secrets accessible?
│   └── Check: Resources → Runner out of space?
├── 🔍 Artifacts missing
│   ├── Check: Artifact run ID → Correct workflow run?
│   ├── Check: Timing → Generated before consumed?
│   └── Check: Naming → Exact name match?
└── 🐌 Workflows too slow
    ├── Check: Change detection → Running unnecessary jobs?
    ├── Check: Caching → Artifacts being reused?
    └── Check: Parallelization → Jobs running in series?
```

### 🔧 **Detailed Solutions**

#### 1. "My workflow isn't running"

**🎯 Goal**: Get workflow to execute when expected

**🔍 Diagnosis Checklist**:
- [ ] Workflow file exists in `.github/workflows/`
- [ ] YAML syntax is valid
- [ ] Trigger conditions are met
- [ ] Repository settings allow workflow

**🛠️ Step-by-Step Fix**:
```bash
# 1. Check for syntax errors
yamllint .github/workflows/your-workflow.yml

# 2. Verify file location
ls -la .github/workflows/

# 3. Test trigger conditions
# For PR workflows: Create a test PR
# For push workflows: Push to monitored branch

# 4. Check repository settings
# Go to: Settings → Actions → General → Workflow permissions
```

**✅ Success Criteria**: Workflow appears in GitHub Actions UI when triggered

#### 2. "Tests are skipped unexpectedly"

**Symptoms**: Test jobs show as "skipped" when they should run
**Common Causes**:
- Change detection filters exclude your changes
- Conditional logic evaluates to false
- Missing required inputs

**Solution Steps**:
```yaml
# Check the initialization phase outputs
- name: Debug Change Detection
  run: |
    echo "Backend changes: ${{ needs.initialize.outputs.backend }}"
    echo "Frontend changes: ${{ needs.initialize.outputs.frontend }}"
    echo "Test changes: ${{ needs.initialize.outputs.test }}"
```

**Fix**: Update `.github/filters.yaml` to include your file paths:
```yaml
backend: &backend
  - 'src/main/java/**'
  - 'your-new-path/**'  # Add this line
```

#### 3. "Build fails with Java version mismatch"

**Symptoms**: Build fails with "Java version X required, but Y found"
**Common Causes**:
- `.sdkmanrc` file not being read
- Action not using `setup-java` action
- Cache corruption

**Solution Steps**:
```bash
# Check the .sdkmanrc file
cat .sdkmanrc

# Verify the action is using setup-java
grep -r "setup-java" .github/workflows/
```

**Fix**: Use the standard pattern:
```yaml
steps:
  - uses: ./.github/actions/core-cicd/prepare-runner
    # This automatically uses .sdkmanrc version
```

#### 3a. "PR fails with '.mvn/maven.config should not be modified'"

**Symptoms**: PR build fails with error about maven.config modification
**Common Causes**:
- Developer accidentally committed `.mvn/maven.config` file
- Trying to change version in PR instead of using release process
- Local build artifacts committed to PR

**Solution Steps**:
```bash
# Check if the file exists in your PR
git diff --name-only origin/main...HEAD | grep maven.config

# Remove the file if it exists
rm -f .mvn/maven.config
git add .mvn/maven.config
git commit -m "Remove maven.config - not allowed in PRs"
```

**Fix**: Never modify versions in PRs:
```yaml
# ❌ WRONG - Don't create this file in PRs
.mvn/maven.config

# ✅ CORRECT - PRs always use snapshot version
# Version changes happen in release workflows only
```

#### 4. "Artifact not found errors"

**Symptoms**: Workflow fails when trying to download artifacts
**Common Causes**:
- Wrong artifact-run-id
- Previous build didn't generate artifacts
- Artifact expired

**Solution Steps**:
```yaml
# Debug artifact settings
- name: Debug Artifacts
  run: |
    echo "Artifact run ID: ${{ inputs.artifact-run-id }}"
    echo "Current run ID: ${{ github.run_id }}"
```

**Fix**: Ensure artifact reuse is set up correctly:
```yaml
uses: ./.github/workflows/cicd_comp_test-phase.yml
with:
  artifact-run-id: ${{ needs.initialize.outputs.artifact-run-id }}
```

#### 5. "Permission denied errors"

**Symptoms**: Actions fail with permission errors
**Common Causes**:
- Wrong permissions in workflow
- Trying to access secrets in PR context
- Missing repository permissions

**Solution Steps**:
```yaml
# Check current permissions
permissions:
  contents: read
  packages: write
  checks: write
```

**Fix**: Never add secrets to PR workflows:
```yaml
# ❌ WRONG - in PR workflow
secrets:
  SLACK_TOKEN: ${{ secrets.SLACK_TOKEN }}

# ✅ CORRECT - use post-workflow reporting
# PRs automatically trigger post-workflow reporting
```

#### 6. "Workflow runs too long"

**Symptoms**: Builds take 30+ minutes, exceed timeout
**Common Causes**:
- Not using change detection
- Running all tests instead of conditional tests
- Missing cache optimization

**Solution Steps**:
```yaml
# Enable change detection
test:
  needs: [initialize]
  if: needs.initialize.outputs.backend == 'true'
  # Only run if backend files changed
```

**Fix**: Use the initialization phase:
```yaml
jobs:
  initialize:
    uses: ./.github/workflows/cicd_comp_initialize-phase.yml
    
  test:
    needs: [initialize]
    if: needs.initialize.outputs.backend == 'true'
    uses: ./.github/workflows/cicd_comp_test-phase.yml
```

## Debugging Workflow Issues

### Step 1: Check Workflow Syntax
```bash
# Use yamllint to validate syntax
yamllint .github/workflows/your-workflow.yml

# Check for common YAML issues
python -c "import yaml; yaml.safe_load(open('.github/workflows/your-workflow.yml'))"
```

### Step 2: Add Debug Outputs
```yaml
- name: Debug Context
  run: |
    echo "Event: ${{ github.event_name }}"
    echo "Ref: ${{ github.ref }}"
    echo "SHA: ${{ github.sha }}"
    echo "Actor: ${{ github.actor }}"
  env:
    GITHUB_CONTEXT: ${{ toJson(github) }}
```

### Step 3: Check Dependencies
```yaml
- name: Check Dependencies
  run: |
    echo "Needs: ${{ toJson(needs) }}"
    echo "Job status: ${{ job.status }}"
```

### Step 4: Verify File Changes
```yaml
- name: Check Changed Files
  run: |
    git diff --name-only ${{ github.event.before }} ${{ github.sha }}
```

## Emergency Procedures

### Workflow Blocking All PRs
1. **Immediate**: Disable the workflow in GitHub UI (Settings → Actions → Workflows)
2. **Quick Fix**: Create emergency PR with minimal fix
3. **Long-term**: Investigate root cause and add safeguards

### Critical Build Failure
1. **Check**: #guild-dev-pipeline Slack for known issues
2. **Diagnose**: Look at recent runs in GitHub Actions UI
3. **Escalate**: If affects production, contact DevOps team immediately

## 🚨 **Critical Warning: Legacy Workflow Impact**

### Understanding Legacy Workflow Risks

**⚠️ Important**: Legacy workflows (`legacy-*.yml`) use their own code and do not rely on common components. When making broader changes to the CI/CD system, these workflows may be affected but won't be tested until release time.

**Key Risks:**
- **Independent Code Paths**: Legacy workflows have their own implementations and don't use modern reusable components
- **Release-Only Testing**: Legacy workflows are primarily exercised during release cycles, not during regular development
- **Hidden Dependencies**: Changes to common patterns, actions, or infrastructure may break legacy workflows
- **Late Discovery**: Failures may not be discovered until critical release moments
- **Different Failure Modes**: Legacy code paths may fail in ways not seen in modern workflows

### Impact Assessment for Broader Changes

**When making changes that could affect legacy workflows:**

#### 🔍 **Changes That May Impact Legacy Workflows**
- **GitHub Actions versions**: Updates to actions used across workflows
- **Runner environments**: Changes to runner images or configurations
- **Shared scripts**: Modifications to scripts that might be used by legacy workflows
- **Infrastructure changes**: Updates to secrets, variables, or external dependencies
- **Build tools**: Changes to Maven, Node.js, or other build tool configurations
- **Authentication**: Updates to token permissions or authentication methods

#### 📋 **Required Actions for Broader Changes**

**1. Impact Assessment**
```bash
# Search for patterns in legacy workflows
grep -r "pattern-to-check" .github/workflows/legacy-*.yml

# Look for shared dependencies
grep -r "actions/checkout" .github/workflows/legacy-*.yml
grep -r "setup-java" .github/workflows/legacy-*.yml
```

**2. Legacy Workflow Testing**
```bash
# Use core-workflow-test repository for validation
# 1. Push changes to core-workflow-test repository
# 2. Manually trigger legacy workflows
# 3. Validate full execution path
# 4. Check for any pattern-dependent failures
```

**3. Documentation**
- Document any shared dependencies between modern and legacy workflows
- Note potential impact areas in PR descriptions
- Update team on any legacy workflow testing performed

### Testing Legacy Workflows

**Using core-workflow-test Repository:**

**⚠️ Important**: Use the core-workflow-test repository to validate legacy workflow execution when making broader changes.

**Testing Process:**
1. **Push Changes**: Apply your changes to the core-workflow-test repository
2. **Manual Trigger**: Manually trigger legacy workflows to test execution
3. **Full Validation**: Run complete legacy workflow execution paths
4. **Pattern Testing**: Verify that common patterns still work correctly
5. **Failure Analysis**: Check for any pattern-dependent failures not seen in modern workflows

**Testing Checklist:**
- [ ] Legacy workflows can be triggered successfully
- [ ] All legacy workflow steps execute without errors
- [ ] Shared actions and patterns work correctly
- [ ] Authentication and permissions function properly
- [ ] External dependencies are accessible
- [ ] Artifacts are generated and consumed correctly

### Long-term Migration Strategy

**Current State:**
- Legacy workflows exist alongside modern component-based workflows
- Release workflows may still use legacy patterns
- Some code paths are only exercised at release time

**Migration Goals:**
- **Reduce Release-Only Code**: Minimize code that is only exercised during release cycles
- **Modernize Legacy Workflows**: Convert legacy workflows to use modern reusable components
- **Increase Test Coverage**: Ensure all code paths are tested during regular development cycles
- **Eliminate Dual Maintenance**: Reduce the need to maintain both legacy and modern implementations

**Benefits After Migration:**
- Less code that is only exercised at release time
- Reduced risk of release-time failures
- Easier maintenance and updates
- More consistent testing across all workflows
- Better reliability and predictability

### Emergency Procedures for Legacy Workflow Failures

**If Legacy Workflows Fail During Release:**

**Immediate Actions:**
1. **Assess Impact**: Determine if failure blocks critical release functionality
2. **Check Dependencies**: Verify external services and infrastructure status
3. **Review Recent Changes**: Identify any recent changes that might affect legacy workflows
4. **Escalate**: Notify release team and DevOps immediately

**Recovery Options:**
1. **Revert Changes**: If recent changes caused the failure, consider reverting
2. **Manual Override**: Use manual processes if automated workflows fail
3. **Emergency Fix**: Apply minimal fixes to restore functionality
4. **Fallback Procedures**: Use backup release procedures if available

**Post-Incident Actions:**
1. **Root Cause Analysis**: Understand why the failure wasn't caught earlier
2. **Improve Testing**: Enhance testing procedures for legacy workflows
3. **Accelerate Migration**: Prioritize migration of problematic legacy workflows
4. **Document Lessons**: Update procedures based on lessons learned

## Performance Optimization Tips

### 1. Use Change Detection
```yaml
# Instead of running all tests
test:
  steps:
    - name: Run All Tests
      run: ./mvnw test

# Run tests conditionally
test:
  if: needs.initialize.outputs.backend == 'true'
  steps:
    - name: Run Backend Tests
      run: ./mvnw test
```

### 2. Enable Caching
```yaml
# The maven-job action handles caching automatically
- uses: ./.github/actions/core-cicd/maven-job
  with:
    generate-artifacts: true  # Enables all caching
```

### 3. Parallel Execution
```yaml
strategy:
  matrix:
    test-group: [unit, integration, e2e]
  fail-fast: false  # Don't cancel other tests if one fails
```

### 4. Future: Build Once, Deploy Anywhere
```yaml
# Long-term goal: Reduce rebuilds by reusing artifacts
# Current: Multiple rebuilds for same code
# Future: Build once, tag and deploy to multiple environments
# See "Long-Term Vision: Build Once, Deploy Anywhere" section for details
```

## Build Failure Analysis: When Tests Pass PR but Fail Later

### Understanding the Pipeline Progression

Tests can pass in PR validation but fail later in the pipeline for several reasons. Understanding these scenarios helps diagnose and resolve issues efficiently.

#### 🔄 **Merge Queue Failures**

**Symptoms**: PR passes all tests, but fails when added to merge queue
**Common Causes**:

1. **Test Filtering Differences**
   - Tests filtered out of PR build but included in merge queue
   - Different test categories run in different phases
   - Configuration differences between PR and merge queue

2. **Queue Dependency Failures**
   - Another PR ahead in the queue failed
   - Causes all subsequent PRs to be rebuilt with updated context
   - Fresh rebuild may expose issues not present in original PR

3. **Flaky Tests**
   - Non-deterministic test behavior
   - Timing issues, race conditions, or environment-dependent failures
   - May pass on initial run but fail on subsequent runs

4. **PR Combination Issues**
   - Individual PRs pass independently
   - Combination of multiple PRs in merge queue exposes conflicts
   - Integration issues only visible when changes are combined


**🔧 Solutions**:

1. **For Flaky Tests**:
   - Re-run the failed workflow steps
   - Use GitHub UI: "Re-run failed jobs" button
   - Or use GitHub CLI: `gh run rerun <run-id> --failed`
   - **GitHub Documentation**: [Re-running workflows and jobs](https://docs.github.com/en/actions/managing-workflow-runs/re-running-workflows-and-jobs)

2. **For Queue Dependency Issues**:
   - Remove from merge queue and re-add after queue clears
   - Wait for failing PRs ahead in queue to be resolved
   - Check #guild-dev-pipeline Slack for queue status

3. **For PR Combination Issues**:
   - Rebase/merge your PR branch with latest main
   - Test locally with combined changes
   - Look for integration conflicts or dependency issues

**📋 Prevention Best Practices**:
- Always rebase/merge with main before adding to merge queue
- Even if GitHub shows "no conflicts", integration issues can still occur
- Regular rebasing reduces risk of combination failures

#### 🚢 **Trunk Build Failures**

**Symptoms**: Merge queue succeeds, but trunk build fails after merge to main
**Common Causes**:

1. **Deployment-Specific Steps**
   - Trunk builds include packaging and deployment steps not in merge queue
   - Additional Docker buildx processes for multi-platform images
   - Platform-specific build steps that only run post-merge

2. **External Resource Issues**
   - Package manager outages (npm, Maven Central, etc.)
   - Docker Hub rate limiting or service issues
   - External service dependencies unavailable

3. **Docker Build Process Issues**
   - Multi-platform Docker builds (buildx) not tested in PR/merge queue
   - Platform-specific build failures (ARM64, AMD64)
   - Build context or layer caching issues

4. **Deployment Environment Issues**
   - Staging environment unavailable
   - Network connectivity problems
   - Resource constraints in deployment targets


**🔧 Solutions**:

1. **For External Resource Issues**:
   - Re-run failed steps: `gh run rerun <run-id> --failed`
   - Check external service status pages
   - Wait for service recovery if temporary outage

2. **For Docker Build Issues**:
   - Re-run the entire build to clear cache issues
   - Check Docker Hub status for rate limiting
   - Verify multi-platform build configurations

3. **For Queue Conflicts**:
   - If another build is in merge queue, wait for natural trunk trigger
   - Avoid running concurrent trunk builds
   - Monitor queue status in #guild-dev-pipeline

**⚠️ Important Notes**:
- Trunk builds don't rebuild and don't run all tests (repackaging only)
- Additional steps may fail that don't run in earlier phases
- Generally safer to wait for natural trunk trigger than force re-run

#### 🌙 **Nightly Build Failures**

**Symptoms**: All previous builds passed, but nightly build fails
**Common Causes**:

1. **Comprehensive Test Coverage**
   - Nightly runs all tests, including those excluded from PR/merge queue
   - Environment-specific tests that only run nightly
   - Extended test suites with longer execution times

2. **Environmental Issues**
   - Different runner environments or configurations
   - Resource constraints during nightly execution window
   - Network or service availability issues

3. **Flaky Test Accumulation**
   - Tests that pass individually but fail in full suite
   - Memory leaks or resource exhaustion over long runs
   - Time-dependent tests that fail at specific hours

4. **Dependency Version Updates**
   - Nightly builds may use latest dependencies
   - Transitive dependency updates causing incompatibilities
   - Security updates affecting functionality

**🔧 Solutions**:

1. **For Flaky Tests**:
   - Re-run failed steps or entire workflow
   - Use GitHub UI re-run functionality
   - Track repeated failures for investigation

2. **For Environmental Issues**:
   - Check runner resource availability
   - Verify external service status
   - Consider splitting large test suites

3. **For Unknown Causes**:
   - Default to re-running failed steps
   - Monitor for patterns in failure
   - Escalate persistent issues to #guild-dev-pipeline

**📈 Pattern Recognition**:
- Single failures usually indicate environmental issues
- Repeated failures suggest code or configuration problems
- Time-based failures may indicate resource or scheduling issues

### Quick Reference: Re-running Failed Workflows

#### Using GitHub UI
1. Navigate to the failed workflow run
2. Click "Re-run failed jobs" button (top right)
3. Optionally select "Re-run all jobs" for full retry
4. Monitor progress in real-time

#### Using GitHub CLI
```bash
# Re-run only failed jobs
gh run rerun <run-id> --failed

# Re-run all jobs
gh run rerun <run-id>

# List recent runs to get run-id
gh run list --limit 10
```

#### **GitHub Documentation Links**:
- [Re-running workflows and jobs](https://docs.github.com/en/actions/managing-workflow-runs/re-running-workflows-and-jobs)
- [Viewing workflow run history](https://docs.github.com/en/actions/monitoring-and-troubleshooting-workflows/viewing-workflow-run-history)

### 🎯 Decision Matrix: When to Re-run vs Investigate

| Scenario | First Action | If Persists |
|----------|-------------|-------------|
| **Flaky test in merge queue** | Re-run failed jobs | Investigate test stability |
| **External service timeout** | Re-run failed jobs | Check service status |
| **Docker build failure** | Re-run entire workflow | Review build configuration |
| **PR combination conflict** | Rebase and re-add to queue | Investigate integration issues |
| **Nightly comprehensive failure** | Re-run failed jobs | Full investigation needed |

### 🔄 Workflow State Recovery

**When workflows fail partway through**:
1. **Assess Impact**: Which phase failed and why?
2. **Check Dependencies**: Are other workflows blocked?
3. **Recovery Strategy**: Re-run vs rebuild vs investigate
4. **Monitor Progress**: Watch for recurring patterns

**Emergency Escalation**:
- **Blocking Production**: Immediate escalation to DevOps
- **Blocking All PRs**: Disable workflow, emergency fix
- **Recurring Failures**: Investigation task in #guild-dev-pipeline

## Analyzing GitHub Workflow Logs for Root Cause Analysis

### Understanding Log Structure and Failure Context

When a Maven job fails, the failure message at the end of the log typically shows only the immediate command that failed, not the underlying root cause. Effective debugging requires examining the logs comprehensively to identify the actual source of the problem.

#### 🔍 **Common Log Analysis Patterns**

**❌ What you see (misleading):**
```
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.0.0-M9:test (default-test) on project dotcms-core: There are test failures.
[ERROR] 
[ERROR] Please refer to /home/runner/work/core-baseline/core-baseline/dotCMS/target/surefire-reports for the individual test results.
```

**✅ What you need to find (root cause):**
- Scroll up to find the actual failing test output
- Look for `java.lang.AssertionError` or exception stack traces
- Check for resource allocation failures, connection timeouts, or environment issues
- Identify the specific test class and method that failed

#### 📋 **Step-by-Step Log Analysis Process**

1. **Start at the Failure Point**
   - Note the failed Maven goal or command
   - Identify the timestamp of the failure

2. **Search Backwards for Context**
   - Look for `[ERROR]` or `[FAIL]` messages above the final failure
   - Find the actual exception or assertion that triggered the failure
   - Check for resource warnings or preliminary error messages

3. **Identify the Root Cause**
   - Locate the specific test or process that failed
   - Look for environmental factors (memory, disk space, network)
   - Check for timing-related issues or race conditions

4. **Gather Supporting Information**
   - Note the runner environment (OS, Java version)
   - Check parallel execution logs for conflicts
   - Review any setup or initialization steps that may have failed

#### 📥 **Downloading Logs for Offline Analysis**

**Using GitHub CLI:**
```bash
# Download logs for a specific run
gh run view <run-id> --log > workflow-logs.txt

# Download logs for the latest run
gh run list --limit 1 --json databaseId --jq '.[0].databaseId' | xargs gh run view --log > latest-logs.txt

# Download logs for a specific job within a run
gh run view <run-id> --log --job <job-name> > job-logs.txt
```

**Using GitHub UI:**
1. Navigate to the failed workflow run
2. Click on the failed job name
3. Click the gear icon (⚙️) in the top right of the job output
4. Select "Download log archive"
5. Extract the ZIP file to access individual step logs

**Using REST API:**
```bash
# Get logs using curl (requires authentication)
curl -L \
  -H "Accept: application/vnd.github+json" \
  -H "Authorization: Bearer <your-token>" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/repos/owner/repo/actions/runs/<run-id>/logs
```

#### 🔧 **Advanced Log Analysis Techniques**

**Filtering Large Log Files:**
```bash
# Extract only error-related lines
grep -E "\[ERROR\]|\[FAIL\]|Exception|Error|Failed" workflow-logs.txt > errors-only.txt

# Find specific test failures
grep -A 10 -B 5 "FAILED" workflow-logs.txt > test-failures.txt

# Search for specific patterns
grep -E "OutOfMemoryError|Connection.*timeout|Permission denied" workflow-logs.txt
```

**Timeline Analysis:**
```bash
# Extract timestamps to understand timing
grep -E "^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}" workflow-logs.txt | head -20

# Find duration of specific steps
grep -E "##\[section\]|##\[debug\]" workflow-logs.txt
```

**Resource Usage Analysis:**
```bash
# Look for memory or disk space issues
grep -E "OutOfMemory|No space left|disk.*full" workflow-logs.txt

# Check for network or service connectivity issues
grep -E "Connection.*refused|timeout|unreachable" workflow-logs.txt
```

#### 🎯 **Common Root Cause Patterns**

**1. Test Flakiness**
```
Look for: Inconsistent test results, timing-dependent failures
Pattern: Tests that pass locally but fail in CI
Solution: Re-run failed jobs, investigate test stability
```

**2. Resource Constraints**
```
Look for: "OutOfMemoryError", "No space left on device"
Pattern: Builds that fail after running for extended periods
Solution: Increase runner resources or optimize build process
```

**3. Network/Service Issues**
```
Look for: Connection timeouts, DNS resolution failures
Pattern: External dependency failures during build
Solution: Retry failed steps, check service status
```

**4. Environment Configuration**
```
Look for: Missing environment variables, incorrect paths
Pattern: Commands that work locally but fail in CI
Solution: Verify environment setup and configuration
```

**5. Parallel Execution Conflicts**
```
Look for: Database locks, file access conflicts
Pattern: Tests that pass individually but fail in parallel
Solution: Review test isolation and resource sharing
```

#### 🚨 **When to Escalate vs Re-run**

**Re-run First (Likely Temporary Issues):**
- External service timeouts
- Network connectivity problems
- Resource allocation failures
- Flaky test patterns

**Investigate First (Likely Code Issues):**
- Consistent test failures across runs
- New failures after code changes
- Permission or configuration errors
- Build compilation failures

**Escalate Immediately:**
- Infrastructure-wide issues
- Security-related failures
- Persistent failures blocking all PRs
- Unknown error patterns

### 🔄 **Handling Flaky Tests - Issue Reporting Protocol**

When you encounter a flaky test that is resolved by re-running the workflow, it's crucial to document the issue to track patterns and prevent future occurrences.

#### 📋 **Flaky Test Response Process**

**1. Re-run the Workflow**
- Use GitHub UI "Re-run failed jobs" or GitHub CLI
- Confirm the test passes on retry
- Document the retry was successful

**2. Search for Existing Issues**
```bash
# Search GitHub issues for the test name
gh issue list --search "TestClassName" --label "Flakey Test"

# Search for similar error patterns
gh issue list --search "NullPointerException" --label "Flakey Test"
```

**3. Gather Required Information**
- **Job Details**: Job name, workflow run ID, timestamp of failure
- **Direct Log Link**: Click on the line number in GitHub UI to get direct link
- **Specific Log File**: Download and extract the relevant log file
- **Error Context**: Full exception stack trace and surrounding context

**4. Create or Update Issue**

#### 🆕 **Creating a New Flaky Test Issue**

**If no existing issue found:**

```markdown
**Title**: [Flaky Test] TestClassName.methodName fails intermittently

**Labels**: Flakey Test, test-stability

**Description Template:**
## Test Information
- **Test Class**: `com.dotcms.example.TestClassName`
- **Test Method**: `methodName`
- **Failure Pattern**: Intermittent failure, passes on retry

## Failure Details
- **Workflow Run**: [Link to failed run](https://github.com/owner/repo/actions/runs/12345)
- **Job Name**: `unit-tests`
- **Timestamp**: 2024-01-15T10:30:45Z
- **Direct Log Link**: [Line 1234](https://github.com/owner/repo/actions/runs/12345/job/67890#step:5:1234)

## Error Details
```
[ERROR] TestClassName.methodName:42 
java.lang.NullPointerException: Cannot invoke "Object.toString()" because "result" is null
    at com.dotcms.example.TestClassName.methodName(TestClassName.java:42)
    at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
    ...
```

## Log File Context
[Attach relevant log file section or full log file]

## Resolution
- ✅ Re-run successful: [Link to successful run](https://github.com/owner/repo/actions/runs/12346)
- 📊 Frequency: First occurrence / Seen X times in past month
- 🔍 Potential Causes: [List any suspected causes]

## Next Steps
- [ ] Monitor for additional occurrences
- [ ] Investigate test isolation issues
- [ ] Review test setup/teardown procedures
```

#### 📝 **Updating an Existing Issue**

**If existing issue found:**

```markdown
**Comment Template:**
## New Occurrence - [Date]

**Failure Details:**
- **Workflow Run**: [Link to failed run](https://github.com/owner/repo/actions/runs/12345)
- **Job Name**: `integration-tests`
- **Timestamp**: 2024-01-15T14:20:30Z
- **Direct Log Link**: [Line 2456](https://github.com/owner/repo/actions/runs/12345/job/67890#step:8:2456)

**Error Pattern:**
```
[Same error pattern as previous occurrences]
```

**Resolution:**
- ✅ Re-run successful: [Link to successful run](https://github.com/owner/repo/actions/runs/12346)

**Frequency Update:**
- Previous occurrences: [Reference previous comments]
- This occurrence: [Current details]
- **Total count**: X occurrences in past [timeframe]

[Attach relevant log file section if provides new context]
```

#### 🔗 **Getting Direct Log Links**

**In GitHub UI:**
1. Navigate to the failed workflow run
2. Click on the failed job
3. Locate the specific error line in the log output
4. Click on the line number (e.g., "1234")
5. Copy the URL from the browser address bar
6. The URL will include the specific line anchor: `#step:5:1234`

**Link Format:**
```
https://github.com/owner/repo/actions/runs/RUN_ID/job/JOB_ID#step:STEP_NUMBER:LINE_NUMBER
```

#### 📊 **Frequency Tracking and Escalation**

**Escalation Thresholds:**
- **2-3 occurrences in 1 week**: Raise with team lead
- **5+ occurrences in 1 month**: High priority investigation needed
- **Daily occurrences**: Immediate escalation - consider disabling test

**Escalation Process:**
1. **Update Issue Priority**: Change label from `Flakey Test` to `Flakey Test-frequent`
2. **Notify Team Lead**: Add comment mentioning team lead with escalation reason
3. **Provide Analysis**: Include frequency data and impact assessment
4. **Suggest Actions**: Recommend investigation, test isolation, or temporary disable

**Escalation Comment Template:**
```markdown
## ⚠️ ESCALATION - Frequent Flaky Test

**Frequency Analysis:**
- Total occurrences: X times in past [timeframe]
- Recent trend: [Increasing/Stable/Decreasing]
- Impact: [Number of workflow re-runs required]

**Recommendation:**
- [ ] Immediate investigation required
- [ ] Consider temporary test disable
- [ ] Review test environment setup
- [ ] Investigate timing/race conditions

@team-lead This test is failing frequently and requires priority attention.
```

#### 🏷️ **Issue Labeling System**

**Required Labels:**
- `Flakey Test`: All flaky test issues

#### 📈 **Tracking and Monitoring**

**Team Monitoring:**
- Regular review of `Flakey Test` labeled issues
- Monthly analysis of flaky test patterns
- Quarterly test stability improvements

**Individual Responsibility:**
- Always create/update issues for flaky tests
- Include all required information in reports
- Monitor issues you've reported for patterns
- Escalate when frequency thresholds are met

#### ✅ **Success Criteria**

**Issue is properly documented when:**
- [ ] Existing issues searched before creating new one
- [ ] All required information included (job details, links, logs)
- [ ] Appropriate labels applied
- [ ] Direct log links provided for easy reference
- [ ] Relevant log files attached when they provide context
- [ ] Escalation process followed for frequent occurrences

#### 📊 **Log Analysis Checklist**

**Before Starting Analysis:**
- [ ] Download complete log files for offline analysis
- [ ] Identify the exact failure timestamp
- [ ] Note the runner environment details
- [ ] Check if failure is reproducible across runs

**During Analysis:**
- [ ] Search backwards from failure point for root cause
- [ ] Check for environmental factors (memory, disk, network)
- [ ] Look for test-specific error messages and stack traces
- [ ] Identify patterns in parallel execution logs

**After Analysis:**
- [ ] Document findings for future reference
- [ ] Determine if issue is code-related or infrastructure-related
- [ ] Plan appropriate response (fix, retry, escalate)
- [ ] Share findings with team if pattern may affect others

## Getting Help

### For Internal Developers
- **Primary Channel**: #guild-dev-pipeline Slack channel
- **Best For**: Questions, troubleshooting, implementation guidance
- **Response Time**: Real-time during business hours
- **Expertise**: Direct access to CI/CD team and community knowledge

### For External Contributors
- **GitHub Issues**: Bug reports and technical issues
- **GitHub Discussions**: Feature requests and architectural questions
- **Documentation**: This README and inline workflow documentation

## Common Debug Commands

### Check Workflow Status
```bash
# View workflow runs
gh run list

# View specific run details
gh run view <run-id>

# View run logs
gh run view <run-id> --log
```

### Analyze File Changes
```bash
# Check what files changed in PR
git diff --name-only origin/main...HEAD

# Check if changes match filters
# Compare with .github/filters.yaml patterns
```

### Test Workflow Locally
```bash
# Use act to test workflows locally (limited functionality)
act pull_request

# Validate YAML syntax
yamllint .github/workflows/*.yml
```

### Check Repository Settings
```bash
# View repository variables
gh variable list

# View repository secrets (names only)
gh secret list
```