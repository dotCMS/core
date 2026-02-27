---
allowed-tools: Bash(gh pr view:*), Bash(gh pr diff:*), Bash(gh pr list:*), Bash(gh issue list:*)
---

# Autonomous PR Review System

Intelligent, self-validating pull request reviewer that automatically selects the appropriate review lens based on changed file types.

## Usage

```bash
/review <PR_NUMBER>
/review <PR_URL>
```

## How It Works

This skill performs an **autonomous, multi-stage frontend review** with intelligent PR classification:

1. **Fetch & Analyze**: Gets PR diff and classifies all changed files by domain
2. **Frontend Detection**: Determines if PR is frontend-focused (>50% frontend files)
3. **Multi-Agent Review**: Launches specialized agents (TypeScript, Angular, Test) in parallel
4. **Self-Validation**: Verifies all file references, line numbers, and findings before output
5. **Structured Output**: Delivers consistent, actionable review format

## Review Process

### Stage 1-3: File Classification with Dedicated Agent

Launch the **File Classifier** agent (subagent type: `dotcms-file-classifier`) to handle PR data collection, file classification, and review decision:

```
Task(
    subagent_type="dotcms-file-classifier",
    prompt="Classify PR #<NUMBER> files by domain (Angular, TypeScript, tests, styles) and determine if frontend-focused review is needed.",
    description="Classify PR files"
)
```

The `dotcms-file-classifier` agent will:
1. **Fetch** PR metadata and diff (`gh pr view`, `gh pr diff`)
2. **Classify** every changed file into reviewer buckets (angular, typescript, test, out-of-scope)
3. **Calculate** frontend vs non-frontend ratio
4. **Return** a structured file map with the review decision (REVIEW or SKIP)

**If decision is SKIP**: Report to the user that the PR is not frontend-focused and stop.

**If decision is REVIEW**: Proceed to Stage 4 with the file map.

### Stage 4: Domain-Specific Review with Specialized Agents

**Using the file map from the dotcms-file-classifier agent**, launch **parallel specialized agents** only for buckets that have files:

1. **TypeScript Type Reviewer** (subagent type: `dotcms-typescript-reviewer`)
   - Receives the `typescript-reviewer` file list from the file map
   - Focus: Type safety, generics, null handling, type quality
   - Confidence threshold: ≥ 75
   - **Skip if**: No files in the typescript bucket

2. **Angular Pattern Reviewer** (subagent type: `dotcms-angular-reviewer`)
   - Receives the `angular-reviewer` file list from the file map
   - Focus: Modern syntax, component architecture, lifecycle, subscriptions
   - Confidence threshold: ≥ 75
   - **Skip if**: No files in the angular bucket

3. **Test Quality Reviewer** (subagent type: `dotcms-test-reviewer`)
   - Receives the `test-reviewer` file list from the file map
   - Focus: Spectator patterns, coverage, test quality
   - Confidence threshold: ≥ 75
   - **Skip if**: No files in the test bucket

4. **SCSS/HTML Style Reviewer** (subagent type: `dotcms-scss-html-style-reviewer`)
   - Receives the `styles` file list from the file map (`.scss`, `.css`, `.html` files)
   - Focus: BEM compliance, CSS custom properties, unused classes, SCSS standards, Angular encapsulation, PrimeNG theming
   - Confidence threshold: ≥ 75
   - **Skip if**: No `.scss`, `.css`, or `.html` files in the styles bucket

**Launch agents in parallel** using the Task tool (only for non-empty buckets):
```
Task(subagent_type="dotcms-typescript-reviewer", prompt="Review TypeScript type safety for PR #<NUMBER>. Files: <file-list from dotcms-file-classifier>", description="TypeScript review")
Task(subagent_type="dotcms-angular-reviewer", prompt="Review Angular patterns for PR #<NUMBER>. Files: <file-list from dotcms-file-classifier>", description="Angular review")
Task(subagent_type="dotcms-test-reviewer", prompt="Review test quality for PR #<NUMBER>. Files: <file-list from dotcms-file-classifier>", description="Test review")
Task(subagent_type="dotcms-scss-html-style-reviewer", prompt="Review SCSS/HTML styling standards for PR #<NUMBER>. Files: <styles file-list from dotcms-file-classifier>", description="Style review")
```

**For Backend/Config/Docs changes**: This skill focuses on frontend code review only. Backend reviews are handled separately.

### Stage 5: Consolidate Agent Results

**When multiple specialized agents were invoked:**

1. **Collect** all agent outputs
2. **Merge** findings by severity:
   - Critical Issues 🔴 (95-100): Must fix before merge
   - Important Issues 🟡 (85-94): Should address
   - Quality Issues 🔵 (75-84): Nice to have
3. **Remove duplicates**: If multiple agents flag the same issue, keep the highest confidence score
4. **Organize** by domain section (TypeScript Types, Angular Patterns, Tests)
5. **Calculate** overall statistics and recommendation

### Stage 6: Self-Validation Checklist

**Before outputting the review, verify:**

1. **File Existence**: Every file mentioned in findings exists in the PR diff
2. **Line Number Accuracy**: All line references are within the actual changed line ranges
3. **Domain Matching**: Review lens matches the actual file types changed
4. **Agent Scope**: Each agent only reported issues in their domain
5. **Completeness**: All significant changes are addressed (no major files skipped)
6. **Consistency**: Recommendations don't contradict each other (across agents)
7. **Evidence**: Every finding cites specific files and line numbers
8. **No duplicates**: Same issue not reported by multiple agents

**If validation fails**, re-analyze before presenting to the user.

### Stage 7: Structured Output

```markdown
# PR Review: #<NUMBER> - <TITLE>

## Summary
[2-3 sentence overview of what changed and overall quality assessment]

**Files Changed**: <count> frontend files
**Review Decision**: REVIEW (frontend-focused PR)
**Risk Level**: <Low|Medium|High>

## Risk Assessment

**Security**: <None|Low|Medium|High> - [explanation if not None]
**Breaking Changes**: <None|Potential|Confirmed> - [explanation if not None]
**Performance Impact**: <None|Low|Medium|High> - [explanation if not None]
**Test Coverage**: <Good|Partial|Missing> - [explanation]

---

## Frontend Findings
[Only if frontend files changed - consolidate from specialized agents]

### TypeScript Type Safety
[From dotcms-typescript-reviewer agent]

#### Critical Issues 🔴 (95-100)
[Type safety violations, raw generics, unsafe casts]

#### Important Issues 🟡 (85-94)
[Missing type guards, weak types, null safety]

#### Quality Issues 🔵 (75-84)
[Type improvements, better generics]

### Angular Patterns
[From dotcms-angular-reviewer agent]

#### Critical Issues 🔴 (95-100)
[Legacy syntax, missing standalone, memory leaks]

#### Important Issues 🟡 (85-94)
[OnPush, subscriptions, component structure]

#### Quality Issues 🔵 (75-84)
[Pattern improvements, optimizations]

### Test Quality
[From dotcms-test-reviewer agent]

#### Critical Issues 🔴 (95-100)
[Wrong Spectator usage, missing detectChanges]

#### Important Issues 🟡 (85-94)
[Coverage gaps, poor mocking, async issues]

#### Quality Issues 🔵 (75-84)
[Test organization, clarity]

### Styling Standards
[From dotcms-scss-html-style-reviewer agent — only if .scss/.css/.html files changed]

#### Critical Issues 🔴 (95-100)
[BEM violations, hardcoded colors/spacing, ::ng-deep misuse]

#### Important Issues 🟡 (85-94)
[Unused classes, missing CSS variables, nesting depth exceeded]

#### Quality Issues 🔵 (75-84)
[Selector improvements, mixin usage, PrimeNG theming patterns]

---

## Approval Recommendation

**✅ Approve** | **⚠️ Approve with Comments** | **❌ Request Changes**

[Clear rationale based on findings above]

**Statistics**:
- Total Critical Issues: <count>
- Total Important Issues: <count>
- Total Quality Issues: <count>

**Next Steps**:
- [Actionable items if changes needed]
- [Or confirmation message if approved]
```

## Error Handling

If PR fetch fails:
- Verify PR number is valid: `gh pr list --limit 100`
- Check if PR is from a fork (may need different permissions)
- Suggest: "Unable to fetch PR #<number>. Does it exist in this repo?"

If no files changed:
- This shouldn't happen, but if it does: "PR #<number> appears to have no changed files. This may be a merge commit or empty PR."

If unable to classify domain:
- Default to **Multi-Domain Review** and analyze all files
- Flag unusual file types for user attention

## Examples

**Example 1: Frontend-Only PR**
```
Files changed: 3 TypeScript components, 2 SCSS files, 1 spec file
Decision: REVIEW (100% frontend files)
Output: Focuses on Angular patterns, component structure, testing
```

**Example 2: Mixed PR (Skipped)**
```
Files changed: 8 Java files, 3 TypeScript files, 1 docker-compose.yml
Decision: SKIP (25% frontend files - below 50% threshold)
Output: "PR is not frontend-focused. Skipping review."
```


Use this as your **single entry point** for all PR reviews.

## Tips for Best Results

- Run after PR is updated: `/review <NUMBER>` again to see if issues were addressed
- For large PRs (50+ files), Claude may need to focus on specific areas - you can guide with: "Focus the review on security concerns" or "Check test coverage especially"
- If the PR is draft or WIP, mention it so review adjusts expectations
- For urgent reviews, add: "This is blocking deployment, prioritize critical issues only"

## Skill Metadata

- **Author**: Generated from usage insights analysis
- **Last Updated**: 2026-02-24
- **Replaces**: dotcms-code-reviewer-frontend
- **Dependencies**: `gh` CLI, access to repository
