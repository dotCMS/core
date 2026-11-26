# Stage 7: Structured Output (SHA-Anchored Links)

Print the report with SHA-anchored GitHub links. Your own work writes the same report to a file, with the changes listed in `local-mode.md`.

**Risk Level** goes up one step when `CI_GATE_EVIDENCE` shows a changed workflow that first runs after merge: no check of the PR proves it.

Each finding sits under the severity its reviewer gave it (Critical, Important, Quality); its heading shows the reviewer confidence and the validator score, which grades whether the finding is true, not how much it matters.

```markdown
# PR Review: #<N> - <TITLE>

## Summary
<`PR_BRIEF` from Stage 2, verbatim — the 200-300 word What this PR does brief. Do not re-summarize; the only edit allowed is appending one closing sentence with the quality verdict now that the agents have run.>

**Files Reviewed**: <n> of <non-noise total> — <count per non-empty bucket, e.g. "2 java, 1 java_test, 1 maven, 1 docs"> | **Risk Level**: <Low|Medium|High>
**Coverage**: <"complete" | "PARTIAL — not fully reviewed: <COVERAGE_GAPS from Stage 4b>">
**HEAD commit**: [`<SHORT_SHA>`](https://github.com/dotCMS/core/commit/<HEAD_SHA>)
**Linked issue**: #<ISSUE_NUMBER> (or "none — AC coverage skipped")
**CI exercise** (only when `CI_GATE`): <CI_GATE_EVIDENCE: per changed workflow, the check that ran it or "first runs on <event>"; for a gate, whether it ran and which path it exercised>
**Effective POM** (only when `RUN_MAVEN`): <"diffed, <n> changed lines" | "not computed: <reason>">

---

## AC Coverage (issue #<ISSUE_NUMBER>)

*Only rendered when `RUN_AC_VALIDATOR` was true. If it was false, print a single line: "No linked issue with acceptance criteria — AC coverage not evaluated." and skip the rest of this section.*

| AC | Text (truncated) | Status | Test? | Notes |
|----|------------------|--------|-------|-------|
| AC1 | "..." | COVERED | ✅ | — |
| AC2 | "..." | PARTIAL | ❌ | <gap> |
| AC3 | "..." | MISSING | — | <where you looked> |

**Coverage: <COVERED>/<total> COVERED · <PARTIAL> PARTIAL · <MISSING> MISSING · <UNTESTABLE> UNTESTABLE**

### Out-of-Scope Implementation (not tied to any AC)

| # | Change | Bucket | Recommendation |
|---|--------|--------|----------------|
| 1 | `file:location` | SCOPE_CREEP | <recommendation> |

*(or "No out-of-scope changes detected.")*

### AC Regression Risks
<list, or "None identified">

---

## Test Design (issue #<ISSUE_NUMBER>)

*Only rendered when `RUN_TEST_DESIGNER` was true; otherwise print one line: "Test design not run (no linked ACs, or Spike)."*

**<total> cases from <P> partitions** — COVERED <a> · PARTIAL <b> · NOT COVERED <c> · UNSPECIFIED <d>. Full plan: `<TEST_PLAN_PATH>`

### Cases nobody tests (NOT COVERED / PARTIAL, Critical and High first)

| TC | Case | AC | Risk | Level | Coverage | What's missing |
|----|------|----|------|-------|----------|----------------|
| TC-004 | Users-portlet non-admin calls saveRolePermission | AC4 | Critical | Integration | NOT COVERED | No test sets up this actor |

*(or "Every designed case is covered.")*

### Behavior no AC specifies
<`EP / TC — question for the author`, or "None.">

---

## Critical Issues

### 1. [Issue title] (Confidence: XX / Validator: YY) [Angular|TypeScript|Styling|Test|Java|API|Persistence|Security|JavaTest|Historical|SilentFailure|Comment|Logic|CI|Maven|Docs] [ADDED-BY-THIS-PR|PRE-EXISTING]
**File**: [`path/to/file.ts:LINE`](https://github.com/dotCMS/core/blob/<HEAD_SHA>/path/to/file.ts#L<LINE-1>-L<LINE+1>)
**Issue**: Brief description
**Fix**: Concrete suggestion

[... more critical issues ...]

---

## Important Issues
[Same format]

---

## Quality Issues
[Same format]

---

## All findings (local triage)

*Only rendered when `SELF_TRIAGE` was true.* One row per finding Stage 5 kept, whatever its score, so the author decides what to fix:

| # | Finding | File / symbol | Reviewer (confidence) | Validator score + reason | Added by this PR? |
|---|---------|---------------|-----------------------|--------------------------|-------------------|

---

## Recommendation

**Approve** | **Approve with Comments** | **Request Changes**

[Clear rationale based on severity distribution AND AC coverage]

**AC coverage gates the recommendation** (evaluated only when `RUN_AC_VALIDATOR` was true):
- **≥1 CONFIRMED_MISSING AC → Request Changes** (implementation is functionally incomplete), regardless of finding severity.
- **≥1 PARTIAL AC (no MISSING) → at most Approve with Comments** — cannot be a clean Approve.
- All ACs COVERED / UNTESTABLE → coverage does not block; fall back to severity distribution.
- Out-of-scope changes **never** block the recommendation; they are surfaced for confirmation only.

**Test design gates it too** (only when `RUN_TEST_DESIGNER` was true):
- **≥1 NOT COVERED case with Risk `Critical` → at most Approve with Comments.** On a `Bug` issue, when that case is the reproduction of the reported defect, **Request Changes**: the fix has no regression test.
- UNSPECIFIED cases never block; they are questions for the author.

- Critical: <count> | Important: <count> | Quality: <count>
- AC coverage: <COVERED>/<total> COVERED, <PARTIAL> PARTIAL, <MISSING> MISSING (or "n/a — no linked issue")
- Test design: <NOT COVERED> of <total> cases untested (<Critical/High count> Critical/High), <UNSPECIFIED> unspecified (or "n/a")
- Agent coverage: code ✓ | test ✓ | historical ✓ | silent-failure ✓/✗ | comment ✓/✗ | logic ✓/✗ | ci ✓/✗ | maven ✓/✗ | docs ✓/✗ | ac-validator ✓/✗ | test-designer ✓/✗
```

## SHA-Anchored Link Format

Every `file:line` is a GitHub link on the finding's `**File**:` row (local mode: plain `path:line`), with the **full** `HEAD_SHA` (an abbreviated one may not render a preview) and at least one line of context on each side:

```
https://github.com/dotCMS/core/blob/<HEAD_SHA>/<file-path>#L<line-1>-L<line+1>
```

## Finding origin

Each finding's heading carries the origin Stage 5 recorded (`validation.md`, step 2): the author fixes an ADDED-BY-THIS-PR finding, and a PRE-EXISTING one is context.
