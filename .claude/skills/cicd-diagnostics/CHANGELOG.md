# CI/CD Diagnostics Skill - Changelog

## Version 2.2.2 - 2025-11-10 (Parameter Validation Improvement)

### Problem
The `fetch-logs.py` script's parameter validation was too simplistic, causing false positives when the workspace path ended with a run ID (e.g., `.claude/diagnostics/run-19219835536`). The validation checked if the workspace parameter was all digits, but didn't account for long run IDs appearing in valid paths.

### Solution
Improved the validation logic to distinguish between:
- **Valid workspace paths** that may contain digits (e.g., `/path/to/run-19219835536`)
- **Job IDs** that are purely numeric and typically 11+ digits long

### Changes Made
- Updated `fetch-logs.py` line 39: Changed validation from `workspace_path.isdigit()` to `workspace_path.isdigit() and len(workspace_path) > 10`
- This allows paths containing run IDs to pass validation while still catching parameter order mistakes

### Before
```python
if workspace_path.isdigit():
    # Would incorrectly trigger on paths like "run-19219835536"
```

### After
```python
if workspace_path.isdigit() and len(workspace_path) > 10:
    # Only triggers on pure job IDs (11+ digits), not paths with numbers
```

### Impact
- **Fixed false positives** - Valid workspace paths with run IDs no longer trigger validation errors
- **Maintained error detection** - Still catches actual parameter order mistakes (e.g., swapping workspace and job ID)
- **Better user experience** - Clear error messages when parameters are truly in wrong order
- **No breaking changes** - All correct usage continues to work

### Testing
Validated with:
- ✅ Correct order: `fetch-logs.py 19219835536 /path/to/run-19219835536 54939324205` (works)
- ✅ Wrong order detection: `fetch-logs.py /path/to/workspace 54939324205` (correctly caught)
- ✅ Path with run ID: `.claude/diagnostics/run-19219835536` (no longer false positive)

---

## Version 2.2.1 - 2025-11-10 (Parameter Consistency Documentation Fix)

### Problem
The SKILL.md documentation showed a complex Python code block for calling `fetch-logs.py`, which made it easy to confuse parameter order. The error occurred because:
- Documentation showed nested Python subprocess calls instead of direct Bash
- Parameter order wasn't emphasized clearly
- Inconsistent presentation across different scripts

### Solution
1. **Simplified documentation** - Replaced complex Python examples with straightforward Bash commands
2. **Added parameter order emphasis** - Clearly stated "All scripts follow the same pattern: <RUN_ID> <WORKSPACE> [optional]"
3. **Added error prevention tips** - Documented common error and how to fix it
4. **Consistent examples** - All three scripts now show consistent usage

### Changes Made
- Updated SKILL.md section "3. Download Failed Job Logs" to use simple Bash syntax
- Updated SKILL.md section "2. Fetch Workflow Data" to emphasize consistent parameter order
- Added parameter order documentation and tips

### Before
```python
# Complex Python code calling subprocess
subprocess.run([
    "python3", ".claude/skills/cicd-diagnostics/fetch-logs.py",
    "19131365567",  # RUN_ID
    str(WORKSPACE),  # WORKSPACE path
    str(failed_job_id)  # JOB_ID (optional)
])
```

### After
```bash
# Simple, clear Bash command
python3 .claude/skills/cicd-diagnostics/fetch-logs.py \
    "$RUN_ID" \
    "$WORKSPACE" \
    54939324205  # JOB_ID from fetch-jobs.py output
```

### Impact
- **No code changes required** - The actual Python scripts were already correct
- **Documentation clarity improved** - Easier to understand and use correctly
- **Error prevention** - Clear parameter order reduces mistakes
- **Consistency** - All three scripts now documented the same way

---

## Version 2.2.0 - 2025-11-10 (Flexibility & AI-Driven Investigation)

### Philosophy Change: From Checklist to Investigation

**Problem:** Previous version (2.1.0) had numbered steps (0-10) that felt prescriptive and rigid. Risk of the AI following steps mechanically rather than adapting to findings.

**Solution:** Redesigned as an adaptive, evidence-driven investigation framework.

### Major Changes

#### 1. Investigation Decision Tree (NEW)

Added visual decision tree to guide investigation approach based on failure type:

```
Test Failure → Check code changes + Known issues
Deployment Failure → CHECK EXTERNAL ISSUES FIRST
Infrastructure Failure → Check logs + Patterns
```

**Decision points at key stages:**
- After evidence: External issue or internal?
- After known issues: Duplicate or new?
- After analysis: Confidence HIGH/MEDIUM/LOW?

#### 2. Removed Rigid Step Numbers

**Before:**
```
### 0. Setup and Load Utilities
### 1. Identify Target
### 2. Fetch Workflow Data
...
### 10. Create Issue
```

**After:**
```
## Investigation Toolkit

Use these techniques flexibly:

### Setup and Load Utilities (Always Start Here)
### Identify Target and Create Workspace
### Fetch Workflow Data
...
### Create Issue (if needed)
```

**Impact:** AI can now skip irrelevant steps, reorder techniques, and adapt depth based on findings.

#### 3. Conditional Guidance Added

Every major technique now has "When to use" guidance:

**Example - Check Known Issues:**
```
Check External Issues when evidence suggests:
- 🔴 HIGH Priority - Authentication errors + service names
- 🟡 MEDIUM Priority - Infrastructure errors + timing
- ⚪ LOW Priority - Test failures with clear assertions

Skip external checks if:
- Test assertion failure with obvious code bug
- Known flaky test already documented
```

#### 4. Enhanced Key Principles

**New Principle: Tool Selection Based on Failure Type**

| Failure Type | Primary Tools | Skip |
|--------------|---------------|------|
| Deployment/Auth | external_issues.py, WebSearch | Deep log analysis |
| Test assertion | Code changes, test history | External checks |
| Flaky test | Run history, timing patterns | External checks |

**Updated Principle: Adaptive Investigation Depth**

```
Quick Win (30 sec - 2 min) → Known issue? Clear error?
Standard Investigation (2-10 min) → Gather, hypothesize, test
Deep Dive (10+ min) → Unclear patterns, multiple theories
```

**Don't always do everything - Stop when confident.**

#### 5. Natural Reporting Guidelines

**Before:** Fixed template with 8 required sections

**After:** Write naturally with relevant sections:
- Core sections (always): Summary, Root Cause, Evidence, Recommendations
- Optional sections: Known Issues, Timeline, Test Fingerprint (when relevant)

**Guideline:** "A deployment authentication error doesn't need a 'Test Fingerprint' section."

### Success Criteria Updated

**Changed focus from checklist completion to investigation quality:**

**Investigation Quality:**
- ✅ Used adaptive investigation depth (stopped when confident)
- ✅ Let evidence guide technique selection (didn't use every tool blindly)
- ✅ Made appropriate use of external validation (when patterns suggest it)

**Removed rigid requirements:**
- ❌ "Checked known issues" → ✅ "Assessed whether this is a known issue (when relevant)"
- ❌ "Validated external dependencies" → ✅ "Made appropriate use of external validation"

### Examples of Improved Flexibility

**Scenario 1: Clear Test Assertion Failure**
- **Old behavior:** Still checks external issues, runs full diagnostic
- **New behavior:** Quickly identifies code change, checks internal issues, done

**Scenario 2: NPM Authentication Error**
- **Old behavior:** Goes through all 10 steps sequentially
- **New behavior:** Decision tree → Deployment failure → Check external FIRST → Find npm security update → Done

**Scenario 3: Unclear Pattern**
- **Old behavior:** Might stop at step 7 without deep analysis
- **New behavior:** Recognizes low confidence → Gathers more context → Compares runs → Forms conclusion

### Backward Compatibility

✅ All utilities unchanged - still work the same way
✅ Evidence extraction unchanged - same quality
✅ External issue detection - still available when needed
✅ No breaking changes to existing functionality

### Documentation Impact

- **SKILL.md:** Complete restructure (~200 lines changed)
- **Philosophy section:** New 6-point investigation pattern
- **Decision tree:** New visual guide
- **Key Principles:** Rewritten with flexibility focus
- **Success Criteria:** Shifted from compliance to quality

---

## Version 2.1.0 - 2025-11-10

### Major Enhancements

#### 1. External Issue Detection (NEW)

**Problem Solved:** Skill was missing critical external service changes (like npm security updates) that cause CI/CD failures.

**Solution:** Added comprehensive external issue detection system.

**New Capabilities:**
- **Automated pattern detection** for npm, Docker, GitHub Actions errors
- **Likelihood assessment** (LOW/MEDIUM/HIGH) for external causes
- **Targeted web search generation** based on error patterns
- **Service-specific checks** with direct links to status pages
- **Timeline correlation** to detect service change impacts

**New Files:**
- `utils/external_issues.py` - External issue detection utilities
  - `extract_error_indicators()` - Parse logs for external error patterns
  - `generate_search_queries()` - Create targeted web searches
  - `suggest_external_checks()` - Recommend which services to verify
  - `format_external_issue_report()` - Generate markdown report section

**Updated Files:**
- `SKILL.md` - Added Step 5: "Check Known Issues (Internal and External)"
  - Automated detection using new utility
  - Internal GitHub issue searches
  - External web searches for high-likelihood issues
  - Correlation analysis with red flags

**Success Criteria Updated:**
- ✅ **Checked known issues - internal (GitHub) AND external (service changes)**
- ✅ **Validated external dependencies (npm, Docker, GitHub Actions) if relevant**
- ✅ Generated comprehensive natural report **with external context**

#### 2. Improved Error Detection in Logs

**Problem Solved:** NPM OTP errors and other critical deployment failures were buried under transient Docker errors.

**Solution:** Enhanced evidence extraction to prioritize and properly detect critical errors.

**Changes to `utils/evidence.py`:**
- **Enhanced error keyword detection:**
  - Added `npm ERR!`, `::error::`, `##[error]`
  - Added `FAILURE:`, `Failed to`, `Cannot`, `Unable to`

- **Smart filtering:**
  - Skip false positives (`.class` files, `.jar` references)
  - Distinguish between recoverable vs. fatal errors

- **Prioritization:**
  - Scan entire log (not just first 100 lines)
  - Show **last 10 error groups** (final/fatal errors)
  - Provide more context (10 lines vs 6 lines after error)

- **Two-pass strategy:**
  - First pass: Critical deployment/infrastructure errors
  - Second pass: Test errors (if no critical errors found)

**Before:**
```
ERROR MESSAGES ===
[Shows first 100 lines of Docker blob errors, stops]
[NPM OTP error at line 38652 never shown]
```

**After:**
```
ERROR MESSAGES ===
[Shows last 10 critical error groups from entire log]
[NPM OTP error properly captured and displayed]
```

### Bug Fixes

1. **Path handling in Python scripts** - Scripts now work correctly when called from any directory
2. **Step numbering** - Fixed duplicate step 6, renumbered workflow steps (5-10)
3. **Evidence limit** - Increased from 100 to 150 lines to capture more context
4. **Smart file listing filter** - Fixed overly aggressive `.class` file filtering:
   - **Before:** Skipped ANY line containing `.class` (would miss real errors like `ERROR: Failed to load class MyClass`)
   - **After:** Only skip lines that are pure file listings (tar/zip output) without error keywords
   - **Logic:** Skip line ONLY if it contains `.class` AND path pattern (`maven/dotserver`) AND NO error keywords (`ERROR:`, `FAILURE:`, `Failed`, `Exception:`)
   - **Result:** Now captures real Java class loading errors while filtering file listings

### Documentation Updates

**README.md:**
- Added external issue detection to capabilities
- Updated examples to show external validation

**SKILL.md:**
- Restructured diagnostic workflow (0-10 steps)
- Added detailed Step 5 with external issue checking
- Updated success criteria
- Added external_issues.py utility reference

### Examples Added

**NPM Security Update (November 2025):**
- Demonstrates detecting npm classic token revocation
- Shows correlation with failure timeline
- Provides migration path recommendations

**Detection Pattern:**
```
🔴 External Cause Likelihood: HIGH

Indicators:
- NPM authentication errors (EOTP/ENEEDAUTH) often caused by
  npm registry policy changes
- Multiple consecutive failures suggest external change

Recommended Web Searches:
- npm EOTP authentication error November 2025
- npm classic token revoked 2025
```

### Migration Notes

**For existing diagnostics:**
1. Re-run skill on historical failures to check for external causes
2. Update any diagnosis reports to include external validation
3. Use new utility for future diagnostics

**No breaking changes** - All existing functionality preserved.

### Testing

Validated with:
- Run 19219835536 (nightly build failure Nov 10, 2025)
- Successfully identified npm EOTP error
- Detected npm security update as external cause
- Generated accurate timeline correlation
- Provided actionable migration recommendations

### Future Enhancements

Potential additions for future versions:
- Expand external_issues.py to detect more service patterns
- Add caching for web search results
- Create database of known external service changes
- Add Slack/email notifications for external issues
- Integration with service status APIs

---

## Version 2.0.0 - 2025-11-07

Initial Python-based implementation with evidence-driven analysis.

## Version 1.0.0 - 2025-10-15

Initial bash-based implementation.
