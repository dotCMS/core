# CI/CD Diagnostics Reference Guide

Detailed technical expertise and diagnostic patterns for DotCMS CI/CD failure analysis.

## Table of Contents

1. [Core Expertise & Approach](#core-expertise--approach)
2. [Specialized Diagnostic Skills](#specialized-diagnostic-skills)
3. [Design Philosophy](#design-philosophy)
4. [Detailed Analysis Patterns](#detailed-analysis-patterns)
5. [Report Templates](#report-templates)
6. [User Collaboration Examples](#user-collaboration-examples)
7. [Comparison with Old Approach](#comparison-with-old-approach)

## Core Expertise & Approach

### Technical Depth

**GitHub Actions:**
- Runner environments, workflow dispatch patterns, matrix builds
- Test filtering strategies, artifact propagation
- Caching strategies and optimization

**DotCMS Architecture:**
- Java/Maven build system
- Docker containers, PostgreSQL/Elasticsearch dependencies
- Integration test infrastructure

**Testing Frameworks:**
- JUnit 5, Postman collections, Karate scenarios, Playwright E2E tests

**Log Analysis:**
- Efficient parsing of multi-GB logs
- Error cascade detection
- Timing correlation
- Infrastructure failure patterns

## Specialized Diagnostic Skills

### Timing & Race Condition Recognition

**Clock precision issues:**
- Second-level timestamps causing non-deterministic ordering (e.g., modDate sorting failures)
- Pattern indicators: Boolean flip assertions, intermittent ordering failures

**Test execution timing:**
- Rapid test execution causing identical timestamps
- sleep() vs Awaitility patterns
- Pattern indicators: Tests that fail faster on faster CI runners

**Database timing:**
- Transaction isolation, commit timing
- Optimistic locking failures

**Async operation timing:**
- Background jobs, scheduled tasks
- Publish/expire date updates

**Cache timing:**
- TTL expiration races
- Cache invalidation timing

### Async Testing Anti-Patterns (CRITICAL)

**Thread.sleep() anti-pattern:**
- Fixed delays causing flaky tests (too short = intermittent failure, too long = slow tests)
- Pattern indicators:
  - `Thread.sleep(1000)` or `Thread.sleep(5000)` in test code
  - Intermittent failures with timing-related assertions
  - Tests that fail faster on faster CI runners
  - "Expected X but was Y" where Y is intermediate state
  - Flakiness that increases under load or on slower machines

**Correct Async Testing Patterns:**

```java
// ❌ WRONG: Fixed sleep (flaky and slow)
publishContent(content);
Thread.sleep(5000);  // Hope it's done by now!
assertTrue(isPublished(content));

// ✅ CORRECT: Awaitility with timeout and polling
publishContent(content);
await()
    .atMost(Duration.ofSeconds(10))
    .pollInterval(Duration.ofMillis(100))
    .untilAsserted(() -> assertTrue(isPublished(content)));

// ✅ CORRECT: With meaningful error message
await()
    .atMost(10, SECONDS)
    .pollDelay(100, MILLISECONDS)
    .untilAsserted(() -> {
        assertThat(getContentStatus(content))
            .describedAs("Content %s should be published", content.getId())
            .isEqualTo(Status.PUBLISHED);
    });

// ✅ CORRECT: Await condition (more efficient than untilAsserted)
await()
    .atMost(Duration.ofSeconds(10))
    .until(() -> isPublished(content));
```

**When to recommend Awaitility:**
- Any test with `Thread.sleep()` followed by assertions
- Any test checking async operation results (publish, index, cache update)
- Any test with timing-dependent behavior
- Any test that fails intermittently with state-related assertions

### Threading & Concurrency Issues

**Thread safety violations:**
- Shared mutable state, non-atomic operations
- Race conditions on counters/maps

**Deadlock patterns:**
- Circular lock dependencies
- Database connection pool exhaustion

**Thread pool problems:**
- Executor queue overflow, thread starvation, improper shutdown

**Quartz job context:**
- Background jobs running in separate thread pools
- Different lifecycle than HTTP requests

**Concurrent modification:**
- ConcurrentModificationException
- Iterator failures during parallel access

**Pattern indicators:**
- NullPointerException in background threads
- "user" is null errors
- Intermittent failures under load

### Request Context Issues (CRITICAL for DotCMS)

**Servlet lifecycle boundaries:**
- HTTP request/response lifecycle vs background thread execution

**ThreadLocal anti-patterns:**
- HttpServletRequestThreadLocal accessed from Quartz jobs
- Scheduled tasks or thread pools accessing request context

**Request object recycling:**
- Tomcat request object reuse after response completion

**User context propagation:**
- Failure to pass User object to background operations
- Bundle publishing, permission jobs

**Session scope leakage:**
- Session-scoped beans accessed from background threads

**Pattern indicators:**
- `Cannot invoke "com.liferay.portal.model.User.getUserId()" because "user" is null`
- `HttpServletRequest` accessed after response completion
- NullPointerException in `PublisherQueueJob`, `IdentifierDateJob`, `CascadePermissionsJob`
- Failures in bundle publishing, content push, or scheduled background tasks

**Common DotCMS Request Context Patterns:**

```java
// ❌ WRONG: Accessing HTTP request in background thread (Quartz job)
User user = HttpServletRequestThreadLocal.INSTANCE.getRequest().getUser(); // NPE!

// ✅ CORRECT: Pass user context explicitly
PublisherConfig config = new PublisherConfig();
config.setUser(systemUser); // Or user from bundle metadata
```

### Analytical Methodology

1. **Progressive Investigation:** Start with high-level patterns (30s), drill down only when needed (up to 10+ min for complex issues)
2. **Evidence-Based Reasoning:** Facts are facts, hypotheses are clearly labeled as such
3. **Multiple Hypothesis Testing:** Consider competing explanations before committing to root cause
4. **Efficient Resource Use:** Extract minimal necessary log context (99%+ size reduction for large files)

### Problem-Solving Philosophy

- **Adaptive Intelligence:** Recognize new failure patterns without pre-programmed rules
- **Skeptical Validation:** Don't accept first obvious answer; validate through evidence
- **User Collaboration:** When multiple paths exist, present options and ask user preference
- **Fact Discipline:** Known facts labeled as facts, theories labeled as theories, confidence levels explicit

## Design Philosophy

This skill follows an **AI-guided, utility-assisted** approach:

- **Utilities** handle data access, caching, and extraction (Python modules)
- **AI** (you, the senior engineer) handles pattern recognition, classification, and reasoning

**Why this works:**
- Senior engineers excel at recognizing new patterns and explaining reasoning
- Utilities excel at fast, cached data access and log extraction
- Avoids brittle hardcoded classification logic
- Adapts to new failure modes without code changes

## Detailed Analysis Patterns

### Example AI Analysis

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

## Report Templates

### DIAGNOSIS.md Template

```markdown
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
```

### ANALYSIS_EVALUATION.md Template

```markdown
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
[How well the Python utilities performed]

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
```

## User Collaboration Examples

### When to Ask for User Input:

**1. Multiple plausible root causes** with similar evidence weight:
```
I've identified two equally plausible explanations:

1. **Test filtering discrepancy** - Test may be filtered in PR but runs in merge queue
2. **Environmental timing issue** - Race condition in test setup

Would you like me to:
A) Deep dive into test filtering configuration (5 min analysis)
B) Analyze test timing patterns across recent runs (5 min analysis)
C) Investigate both in parallel (10 min analysis)
```

**2. Insufficient information** requiring deeper investigation:
```
**FACT**: Test failed with NullPointerException at line 234
**HYPOTHESIS**: Could be either (a) data initialization race or (b) mock configuration issue
**NEED**: Additional log context around test setup (lines 200-240)

Would you like me to extract and analyze the full setup context? This will add ~2 min.
```

**3. Trade-offs between investigation paths:**
```
I can either:
- **Quick path** (2 min): Verify this matches known flaky test pattern → recommend retry
- **Thorough path** (10 min): Analyze why test is flaky → identify potential fix

What's your priority: unblock immediately or understand root cause?
```

**4. Recommendation requires user context:**
```
This appears to be a genuine code defect in the new pagination logic.

Options:
1. Revert PR and investigate offline
2. Push fix commit to existing PR
3. Merge with known issue and create follow-up

What's the team's current priority: stability or feature velocity?
```

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

```python
# Present evidence to AI
evidence = present_complete_diagnostic(log_file)

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

## Additional Context

For more information:
- [WORKFLOWS.md](WORKFLOWS.md) - Detailed workflow descriptions and failure patterns
- [LOG_ANALYSIS.md](LOG_ANALYSIS.md) - Advanced log analysis techniques
- [utils/README.md](utils/README.md) - Utility function reference
- [ISSUE_TEMPLATE.md](ISSUE_TEMPLATE.md) - Issue creation template


