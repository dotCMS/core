# Advanced Log Analysis Techniques

Efficient strategies for analyzing large CI/CD log files.

## The Log Size Problem

**Typical sizes**:
- PR workflow logs: 50-200 MB compressed
- Merge queue logs: 200-500 MB compressed
- Full extracted logs: 1-2 GB uncompressed

**Context window limits**: Cannot load entire logs into memory or AI context

**Solution**: Stream, filter, and target specific sections

## Smart Log Download Strategy

### 1. Check What's Available First

```bash
# List artifacts WITHOUT downloading
gh api "/repos/dotCMS/core/actions/runs/$RUN_ID/artifacts" \
  --jq '.artifacts[] | {name, size_mb: (.size_in_bytes / 1024 / 1024 | floor), expired}'
```

**Artifact types**:
- `logs_*` - Full workflow logs (large)
- `test-results-*` - Surefire/Failsafe XML (structured, smaller)
- `Test Report` - HTML test reports (visual)
- `*-coverage` - Code coverage (not relevant for failures)

**Priority order**:
1. `test-results-*` - Small, structured, precise failure info
2. Failed job logs only (via API)
3. Full logs archive (last resort)

### 2. Download Only Failed Job Logs

```bash
# Get specific job ID that failed
FAILED_JOB_ID=$(gh api "/repos/dotCMS/core/actions/runs/$RUN_ID/jobs" \
  --jq '.jobs[] | select(.conclusion == "failure") | .id' | head -1)

# Download ONLY that job's logs
gh api "/repos/dotCMS/core/actions/jobs/$FAILED_JOB_ID/logs" > failed-job.log

# Much smaller than full archive!
```

### 3. Progressive Log Extraction

```bash
# Download full archive
gh run download $RUN_ID --dir ./logs

# List contents first (don't extract)
unzip -l logs.zip | head -50

# Identify structure
# Typical structure:
# - 1_Job Name/
#   - 2_Step Name.txt
#   - 3_Another Step.txt

# Extract ONLY failed job directory
unzip logs.zip "*/Failed Job Name/*" -d extracted/

# Or stream search without extracting
unzip -p logs.zip "**/[0-9]*_*.txt" | grep "pattern" | head -100
```

## Pattern Matching Strategies

### Maven Build Failures

**Primary indicators** (check these first):
```bash
# Maven errors (most reliable)
unzip -p logs.zip "**/[0-9]*_*.txt" | grep -A 10 -B 3 "\[ERROR\]" | head -100

# Build failure summary
unzip -p logs.zip "**/[0-9]*_*.txt" | grep -A 20 "BUILD FAILURE" | head -100

# Compilation errors
unzip -p logs.zip "**/[0-9]*_*.txt" | grep -A 15 "COMPILATION ERROR" | head -50
```

**What to look for**:
- `[ERROR] Failed to execute goal` - Maven plugin failures
- `[ERROR] COMPILATION ERROR` - Java compilation issues
- `[ERROR] There are test failures` - Test failures
- `[ERROR] Could not resolve dependencies` - Dependency issues

### Test Failures

**Test failure markers** (surefire/failsafe):
```bash
# Test failure summary
unzip -p logs.zip "**/[0-9]*_*.txt" | grep -E "Tests run:.*Failures: [1-9]" | head -20

# Individual test failures
unzip -p logs.zip "**/[0-9]*_*.txt" | grep -A 25 "<<< FAILURE!" | head -200

# Test errors (crashes)
unzip -p logs.zip "**/[0-9]*_*.txt" | grep -A 25 "<<< ERROR!" | head -200
```

**Test failure structure**:
```
[ERROR] Tests run: 150, Failures: 2, Errors: 0, Skipped: 5
...
[ERROR] testMethodName(com.dotcms.TestClass) Time elapsed: 1.234 s <<< FAILURE!
java.lang.AssertionError: Expected X but was Y
    at org.junit.Assert.fail(Assert.java:88)
    at com.dotcms.TestClass.testMethodName(TestClass.java:123)
```

**Extract failure details**:
```bash
# Get test class and method
grep "<<< FAILURE!" logs.txt | sed 's/.*\(test[A-Za-z]*\)(\([^)]*\).*/\2.\1/'

# Get exception type and message
grep -A 5 "<<< FAILURE!" logs.txt | grep -E "^[a-zA-Z.]*Exception|^java.lang.AssertionError"
```

### Stack Trace Analysis

**Find relevant stack traces**:
```bash
# Find DotCMS code in stack traces (ignore framework)
unzip -p logs.zip "**/[0-9]*_*.txt" | \
  grep -A 50 "Exception:" | \
  grep -E "at com\.(dotcms|dotmarketing)\." | \
  head -100
```

**Stack trace structure**:
```
java.lang.NullPointerException: Cannot invoke method on null object
    at com.dotcms.MyClass.myMethod(MyClass.java:456)        ← Target this
    at com.dotcms.OtherClass.caller(OtherClass.java:123)    ← And this
    at org.junit.internal.runners...                        ← Ignore framework
    at sun.reflect...                                       ← Ignore JVM
```

**Priority**: Lines starting with `at com.dotcms` or `at com.dotmarketing`

### Infrastructure Issues

**Patterns to search**:
```bash
# Timeout issues
grep -i "timeout\|timed out\|deadline exceeded" logs.txt | head -20

# Connection issues
grep -i "connection refused\|connection reset\|unable to connect" logs.txt | head -20

# Rate limiting
grep -i "rate limit\|too many requests\|429" logs.txt | head -20

# Resource exhaustion
grep -i "out of memory\|cannot allocate\|disk.*full" logs.txt | head -20

# Docker issues
grep -i "docker.*error\|failed to pull\|image not found" logs.txt | head -20
```

### Dependency Issues

**Patterns**:
```bash
# Dependency resolution failures
grep -i "could not resolve\|failed to resolve\|artifact not found" logs.txt | head -30

# Version conflicts
grep -i "version conflict\|duplicate\|incompatible" logs.txt | head -20

# Download issues
grep -i "failed to download\|connection to.*refused" logs.txt | head-20
```

## Test Report XML Analysis

**Structure** (surefire/failsafe XML):
```xml
<testsuite name="com.dotcms.ContentTypeAPIImplTest" tests="15" failures="1" errors="0">
  <testcase name="testCreateContentType" classname="com.dotcms.ContentTypeAPIImplTest" time="1.234">
    <failure message="Expected X but was Y" type="java.lang.AssertionError">
      <![CDATA[
      java.lang.AssertionError: Expected X but was Y
          at com.dotcms.ContentTypeAPIImplTest.testCreateContentType(ContentTypeAPIImplTest.java:123)
      ]]>
    </failure>
  </testcase>
</testsuite>
```

**Parse with Read tool or xmllint**:
```bash
# Extract test results only
unzip logs.zip "**/*surefire-reports/*.xml" -d test-results/

# Count failures per test suite
find test-results -name "*.xml" -exec grep -H "failures=" {} \; | grep -v 'failures="0"'

# Extract failure messages
xmllint --xpath "//failure/@message" test-results/*.xml
```

## Efficient Search Workflow

### Step-by-Step Process

**1. Quick Status Check (30 seconds)**:
```bash
gh run view $RUN_ID --json conclusion,jobs \
  --jq '{conclusion, failed_jobs: [.jobs[] | select(.conclusion == "failure") | .name]}'
```

**2. Failed Job Details (1 minute)**:
```bash
gh api "/repos/dotCMS/core/actions/runs/$RUN_ID/jobs" \
  --jq '.jobs[] | select(.conclusion == "failure") |
        {name, failed_steps: [.steps[] | select(.conclusion == "failure") | .name]}'
```

**3. Check Test Artifacts (1 minute)**:
```bash
# List test result artifacts
gh api "/repos/dotCMS/core/actions/runs/$RUN_ID/artifacts" \
  --jq '.artifacts[] | select(.name | contains("test-results")) | {name, id, size_in_bytes}'

# Download if small (< 10 MB)
# Skip if large or expired
```

**4. Job-Specific Logs (2-3 minutes)**:
```bash
# Download only failed job logs
FAILED_JOB_ID=<id>
gh api "/repos/dotCMS/core/actions/jobs/$FAILED_JOB_ID/logs" > failed-job.log

# Search for Maven errors
grep -A 10 "\[ERROR\]" failed-job.log | head -100

# Search for test failures
grep -A 25 "<<< FAILURE!" failed-job.log | head -200
```

**5. Full Archive Analysis (5+ minutes, only if needed)**:
```bash
# Download full logs
gh run download $RUN_ID --name logs --dir ./logs

# List contents
unzip -l logs/*.zip | grep -E "\.txt$" | head -50

# Stream search (no extraction)
unzip -p logs/*.zip "**/[0-9]*_*.txt" | grep -E "\[ERROR\]|<<< FAILURE!" | head -300
```

## Pattern Recognition Guide

### Error Type Identification

**Compilation Error**:
```
[ERROR] COMPILATION ERROR
[ERROR] /path/to/File.java:[123,45] cannot find symbol
```
→ Code syntax error, missing import, type mismatch

**Test Failure (Assertion)**:
```
<<< FAILURE!
java.lang.AssertionError: expected:<foo> but was:<bar>
```
→ Test expectation not met, code behavior changed

**Test Error (Exception)**:
```
<<< ERROR!
java.lang.NullPointerException
    at com.dotcms.MyClass.method(MyClass.java:123)
```
→ Unexpected exception, code defect

**Timeout**:
```
org.junit.runners.model.TestTimedOutException: test timed out after 30000 milliseconds
```
→ Test hung, infinite loop, or infrastructure slow

**Connection/Infrastructure**:
```
java.net.ConnectException: Connection refused
Could not resolve host: repository.example.com
```
→ Network issue, external service down, infrastructure problem

**Dependency Issue**:
```
[ERROR] Failed to collect dependencies
Could not resolve dependencies for project com.dotcms:dotcms-core
```
→ Maven repository issue, version conflict, missing artifact

## Context Window Optimization

**Problem**: Cannot load 500 MB of logs into context

**Solutions**:

1. **Targeted extraction**: Get only relevant sections
```bash
# Extract just the error summary from a 500 MB log
unzip -p logs.zip "**/5_Test.txt" | \
  grep -A 50 "\[ERROR\] Tests run:" | \
  head -200
# Result: ~10 KB instead of 500 MB
```

2. **Layered analysis**:
   - First: Maven ERROR lines (usually < 100 lines)
   - Second: Specific test failure (usually < 50 lines)
   - Third: Stack trace for that test (usually < 30 lines)
   - Total: ~200 lines instead of millions

3. **Use structured data when possible**:
   - XML test reports: Parse for failures only
   - JSON from gh CLI: Filter with jq
   - Grep with line limits: Never more than needed

## Common Pitfalls

❌ **Don't do this**:
```bash
# Downloads and extracts EVERYTHING (5-10 min, huge context)
gh run download $RUN_ID
unzip -q logs.zip
cat **/*.txt > all-logs.txt  # 1 GB+ file
```

✅ **Do this instead**:
```bash
# Targeted search (30 sec, minimal context)
gh run download $RUN_ID --name logs
unzip -p logs/*.zip "**/[0-9]*_*.txt" | grep -A 10 "\[ERROR\]" | head -100
```

❌ **Don't do this**:
```bash
# Read entire log file
Read: /path/to/5-Test-step.txt  # 200 MB file
```

✅ **Do this instead**:
```bash
# Use Bash grep to extract relevant lines first
grep -A 20 "<<< FAILURE!" /path/to/5-Test-step.txt | head -200 > failures-only.txt
# Then read the small extracted file
Read: failures-only.txt  # 10 KB file
```

## Quick Reference Commands

### Fastest Diagnosis Commands
```bash
# 1. Which job failed? (10 sec)
gh run view $RUN_ID --json jobs --jq '.jobs[] | select(.conclusion == "failure") | .name'

# 2. What step failed? (10 sec)
gh api "/repos/dotCMS/core/actions/runs/$RUN_ID/jobs" --jq '.jobs[] | select(.conclusion == "failure") | .steps[] | select(.conclusion == "failure") | .name'

# 3. Get that job's logs (30 sec)
FAILED_JOB_ID=$(gh api "/repos/dotCMS/core/actions/runs/$RUN_ID/jobs" --jq '.jobs[] | select(.conclusion == "failure") | .id' | head -1)
gh api "/repos/dotCMS/core/actions/jobs/$FAILED_JOB_ID/logs" > job.log

# 4. Find Maven errors (5 sec)
grep -A 10 "\[ERROR\]" job.log | head -100

# 5. Find test failures (5 sec)
grep -A 25 "<<< FAILURE!" job.log | head -200
```

**Total time**: ~60 seconds to identify most failures

## Log Analysis Checklist

When analyzing logs:
- [ ] Start with job-level logs via API (fastest)
- [ ] Look for Maven `[ERROR]` markers first
- [ ] Search for test failure markers: `<<< FAILURE!`, `<<< ERROR!`
- [ ] Extract stack traces with DotCMS code only
- [ ] Check for infrastructure patterns if no code errors
- [ ] Use grep line limits (`head`, `tail`) religiously
- [ ] Only download full archive if absolutely necessary
- [ ] Never try to read entire log files without filtering