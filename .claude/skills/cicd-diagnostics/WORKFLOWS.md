# DotCMS CI/CD Workflows Reference

Complete documentation of workflow behaviors and failure patterns.

## cicd_1-pr.yml - Pull Request Validation

**Purpose**: Fast feedback on PR changes with optimized test selection

**Triggers**:
- Pull request opened/synchronized
- Re-run requested

**Test Strategy**:
- **Filtered tests**: Runs subset based on changed files
- **Optimization goal**: Fast feedback (5-15 min typical)
- **Trade-off**: May miss integration issues caught in full suite

**Common Failure Patterns**:

1. **Code Compilation Errors**
   - Pattern: `[ERROR] COMPILATION ERROR`
   - Cause: Syntax errors, missing imports, type errors
   - Log location: Maven build output, early in job
   - Action: Fix compilation errors in PR

2. **Unit Test Failures**
   - Pattern: `Tests run:.*Failures: [1-9]`
   - Cause: Breaking changes in code
   - Log location: Surefire reports
   - Action: Fix failing tests or revert breaking change

3. **Lint/Format Violations**
   - Pattern: `Checkstyle violations`, `PMD violations`
   - Cause: Code style issues
   - Log location: Static analysis step
   - Action: Run `mvn spotless:apply` locally

4. **Filtered Test Passes (False Positive)**
   - Pattern: PR passes, merge queue fails
   - Cause: Integration test not run in PR due to filtering
   - Detection: Compare PR vs merge queue results for same commit
   - Action: Run full test suite locally or wait for merge queue

**Typical Duration**: 5-20 minutes

**Workflow URL**: https://github.com/dotCMS/core/actions/workflows/cicd_1-pr.yml

## cicd_2-merge-queue.yml - Pre-Merge Full Validation

**Purpose**: Comprehensive validation before merging to main branch

**Triggers**:
- PR added to merge queue (manual or automated)
- Required status checks passed

**Test Strategy**:
- **Full test suite**: ALL tests run (integration, unit, E2E)
- **No filtering**: Catches issues missed in PR workflow
- **Duration**: 30-60 minutes typical

**Common Failure Patterns**:

1. **Test Filtering Discrepancy**
   - Pattern: PR passed ✓, merge queue failed ✗
   - Cause: Test filtered in PR, failed in full suite
   - Detection: Same commit, different outcomes
   - Action: Fix the test that was filtered out
   - Prevention: Run full suite locally before merge

2. **Multiple PR Conflicts**
   - Pattern: PR A passes, PR B passes, merge queue with both fails
   - Cause: Conflicting changes between PRs
   - Detection: Multiple PRs in queue, all passing individually
   - Log pattern: Integration test failures, database state issues
   - Action: Rebase one PR on the other, re-test

3. **Previous PR Failure Contamination**
   - Pattern: PR fails immediately after another PR failure
   - Cause: Shared state or resources from previous run
   - Detection: Check previous run in queue
   - Action: Re-run the workflow (no code changes needed)

4. **Branch Not Synchronized**
   - Pattern: Tests fail that pass on main
   - Cause: PR branch behind main, missing recent fixes
   - Detection: `gh pr view $PR --json mergeable` shows `BEHIND`
   - Action: Merge main into PR branch, re-test

5. **Flaky Tests**
   - Pattern: Intermittent failures, passes on re-run
   - Cause: Test has race conditions, timing dependencies
   - Detection: Same test fails/passes across runs
   - Action: Investigate test, add to flaky test tracking
   - Labels: `flaky-test`

6. **Infrastructure Timeouts**
   - Pattern: `timeout`, `connection refused`, `rate limit exceeded`
   - Cause: GitHub Actions infrastructure, external services
   - Detection: No code changes, external error messages
   - Action: Re-run workflow, check GitHub status

**Typical Duration**: 30-90 minutes

**Critical Checks Before Merge**:
```bash
# Verify PR is up to date
gh pr view $PR_NUMBER --json mergeStateStatus

# Check for other PRs in queue
gh pr list --search "is:open base:main label:merge-queue"

# Review recent merge queue runs
gh run list --workflow=cicd_2-merge-queue.yml --limit 10
```

**Workflow URL**: https://github.com/dotCMS/core/actions/workflows/cicd_2-merge-queue.yml

## cicd_3-trunk.yml - Post-Merge Deployment

**Purpose**: Deploy merged changes, publish artifacts, build Docker images

**Triggers**:
- Successful merge to main branch
- Uses artifacts from merge queue (no test re-run)

**Key Operations**:
1. Retrieve build artifacts from merge queue
2. Deploy to staging environment
3. Build and push Docker images
4. Run CLI smoke tests
5. Update documentation sites

**Common Failure Patterns**:

1. **Artifact Retrieval Failure**
   - Pattern: `artifact not found`, `download failed`
   - Cause: Merge queue artifacts expired or missing
   - Detection: Early failure in artifact download step
   - Action: Re-run merge queue to regenerate artifacts

2. **Docker Build Failure**
   - Pattern: `failed to build`, `COPY failed`, `image too large`
   - Cause: Dockerfile changes, dependency updates, resource limits
   - Log location: Docker build step
   - Action: Review Dockerfile changes, check layer sizes

3. **Docker Push Failure**
   - Pattern: `denied: access forbidden`, `rate limit`, `timeout`
   - Cause: Registry authentication, network, rate limits
   - Detection: Build succeeds, push fails
   - Action: Check registry credentials, retry after rate limit

4. **CLI Tool Failures**
   - Pattern: CLI command errors, integration failures
   - Cause: API changes breaking CLI, environment config
   - Log location: CLI test/validation steps
   - Action: Review CLI compatibility with API changes

5. **Deployment Configuration Issues**
   - Pattern: Configuration errors, environment variable issues
   - Cause: Missing secrets, config changes
   - Detection: Deployment step failures
   - Action: Verify environment configuration in GitHub secrets

**Important Notes**:
- Tests are NOT re-run (assumes merge queue validation)
- Test failures here indicate artifact corruption or environment issues
- Deployment failures don't necessarily mean code issues

**Typical Duration**: 15-30 minutes

**Workflow URL**: https://github.com/dotCMS/core/actions/workflows/cicd_3-trunk.yml

## cicd_4-nightly.yml - Scheduled Full Validation

**Purpose**: Detect flaky tests, infrastructure issues, external dependency changes

**Triggers**:
- Scheduled (nightly, e.g., 2 AM UTC)
- Manual trigger via workflow dispatch

**Test Strategy**:
- Full test suite against main branch
- Latest dependencies (detects upstream breaking changes)
- Longer timeout thresholds
- Multiple test runs for flaky detection (optional)

**Common Failure Patterns**:

1. **Flaky Test Detection**
   - Pattern: Test fails occasionally, not consistently
   - Cause: Race conditions, timing dependencies, resource contention
   - Detection: Failure rate < 100% over multiple nights
   - Analysis: Track test across 20-30 nightly runs
   - Action: Mark as flaky, investigate root cause
   - Threshold: >5% failure rate = needs attention

2. **External Dependency Changes**
   - Pattern: Tests fail after dependency update
   - Cause: Upstream library using `latest` or mutable version
   - Detection: No code changes in repo, failure starts suddenly
   - Log pattern: `NoSuchMethodError`, API compatibility errors
   - Action: Pin dependency versions, update code for compatibility

3. **GitHub Actions Version Changes**
   - Pattern: Workflow steps fail, GitHub Actions behavior changed
   - Cause: GitHub Actions runner or action version updated
   - Detection: Workflow YAML unchanged, runner behavior different
   - Log pattern: Action warnings, deprecation notices
   - Action: Update action versions explicitly in workflow

4. **Infrastructure Degradation**
   - Pattern: Timeouts, slow tests, resource exhaustion
   - Cause: GitHub Actions infrastructure issues
   - Detection: Tests pass but take much longer, timeouts
   - Action: Check GitHub Actions status, wait for resolution

5. **Database/Elasticsearch State Issues**
   - Pattern: Tests fail with data inconsistencies
   - Cause: Cleanup issues, state leakage between tests
   - Detection: Tests pass individually, fail in suite
   - Action: Improve test isolation, add cleanup

6. **Time-Dependent Test Failures**
   - Pattern: Tests fail at specific times (timezone, daylight saving)
   - Cause: Hard-coded dates, timezone assumptions
   - Detection: Failure coincides with date/time changes
   - Action: Use relative dates, mock time in tests

**Flaky Test Analysis Process**:
```bash
# Get last 30 nightly runs
gh run list --workflow=cicd_4-nightly.yml --limit 30 --json databaseId,conclusion,createdAt

# For specific test, count failures
# (requires parsing test report artifacts across runs)

# Calculate flaky percentage
# Flaky if: 5% < failure rate < 95%
# Consistently failing if: failure rate >= 95%
# Stable if: failure rate < 5%
```

**Typical Duration**: 45-90 minutes

**Workflow URL**: https://github.com/dotCMS/core/actions/workflows/cicd_4-nightly.yml

## Cross-Cutting Failure Causes

These affect all workflows:

### Reproducibility Issues

**External Dependencies with Mutable Versions**:
- Maven dependencies using version ranges or `LATEST`
- Docker base images using `latest` tag
- GitHub Actions without pinned versions (@v2 vs @v2.1.0)
- NPM dependencies without lock file or using `^` ranges

**Detection**:
- Failures start suddenly without code changes
- Different results across runs with same code
- Dependency resolution messages in logs

**Prevention**:
- Pin all dependency versions explicitly
- Use lock files (package-lock.json, yarn.lock)
- Pin GitHub Actions to commit SHA: `uses: actions/checkout@a12b3c4`
- Avoid `latest` tags for Docker images

### Infrastructure Issues

**GitHub Actions Platform**:
- Runner outages or degraded performance
- Artifact storage issues
- Registry rate limits
- Network connectivity issues

**Detection**:
```bash
# Check GitHub status
curl -s https://www.githubstatus.com/api/v2/status.json | jq '.status.description'

# Look for infrastructure patterns in logs
grep -i "timeout\|rate limit\|connection refused\|runner.*fail" logs.txt
```

**Action**: Wait for GitHub resolution, retry workflow

**External Services**:
- Maven Central unavailable
- Docker Hub rate limits
- NPM registry issues
- Elasticsearch download failures

**Detection**:
- `Could not resolve`, `connection timeout`, `rate limit`
- Service-specific error messages

**Action**: Wait for service resolution, use mirrors/caches

### Resource Constraints

**Memory/Disk Issues**:
- Pattern: `OutOfMemoryError`, `No space left on device`
- Cause: Large test suite, memory leaks, artifact accumulation
- Action: Optimize test memory, clean up artifacts, split jobs

**Timeout Issues**:
- Pattern: Job cancelled, timeout reached
- Cause: Tests running longer than expected, hung processes
- Action: Investigate slow tests, increase timeout, optimize

## Workflow Comparison Matrix

| Aspect | PR | Merge Queue | Trunk | Nightly |
|--------|-----|-------------|--------|---------|
| **Tests** | Filtered subset | Full suite | None (reuses) | Full suite |
| **Duration** | 5-20 min | 30-90 min | 15-30 min | 45-90 min |
| **Purpose** | Fast feedback | Validation | Deployment | Stability |
| **Failure = Code Issue?** | Usually yes | Usually yes | Maybe no | Maybe no |
| **Retry Safe?** | Yes | Yes (check queue) | Yes | Yes |

## Diagnostic Decision Tree

```
Build failed?
├─ Which workflow?
│  ├─ PR → Check compilation, unit tests, lint
│  ├─ Merge Queue → Compare with PR results
│  │  ├─ PR passed → Test filtering issue
│  │  ├─ PR failed → Same issue, expected
│  │  └─ First failure → Check queue, branch sync
│  ├─ Trunk → Check artifact retrieval, deployment
│  └─ Nightly → Likely flaky or infrastructure
│
├─ Error type?
│  ├─ Compilation → Code issue, fix in PR
│  ├─ Test failure → Check if new or flaky
│  ├─ Timeout → Infrastructure or slow test
│  └─ Dependency → External issue or reproducibility
│
└─ Historical pattern?
   ├─ First time → New issue, recent change
   ├─ Intermittent → Flaky test, track
   └─ Always fails → Consistent issue, needs fix
```