# Review Agents for DotCMS Core

This directory contains **reusable specialized agents** for code review. These agents can be invoked by the `/review` skill or used independently for targeted reviews.

**Location**: `.claude/agents/` (project-level, shared across all skills)

**Key Point**: These are **independent workers**, not part of the review skill. Any skill or workflow can use them.

## Available Agents

### 🟠 File Classifier
**File**: `file-classifier.md`
**Model**: Sonnet
**Focus**: PR file triage and classification

**Responsibilities**:
- Fetches PR metadata and diff
- Classifies every changed file by domain
- Maps files to the appropriate reviewer agents
- Calculates frontend vs non-frontend ratio
- Returns a structured file map for the orchestrator

**Used as**: First step in the review pipeline, before launching specialized reviewers.

### 🔷 TypeScript Type Reviewer
**File**: `typescript-reviewer.md`
**Model**: Sonnet
**Focus**: TypeScript type system, generics, null safety

**Reviews**:
- Type safety violations (no `any`, proper generics)
- Null handling (optional chaining, nullish coalescing)
- Interface design and type quality
- Type guards and runtime safety
- Function signatures and return types

**Confidence threshold**: ≥ 75
**Excludes**: `.spec.ts` files (handled by test-reviewer)

### 🟣 Angular Pattern Reviewer
**File**: `angular-reviewer.md`
**Model**: Sonnet
**Focus**: Angular framework patterns, modern syntax, architecture

**Reviews**:
- Modern Angular syntax (`@if`, `@for`, `input()`, `output()`)
- Component architecture (standalone, prefix, change detection)
- Template patterns (safe navigation, trackBy, data-testid)
- Lifecycle management (subscription cleanup, signals)
- Service patterns (providedIn, signal stores)
- Import patterns and circular dependencies
- SCSS standards (variables, BEM)

**Confidence threshold**: ≥ 75
**Excludes**: `.spec.ts` files (handled by test-reviewer)

### 🟢 Test Quality Reviewer
**File**: `test-reviewer.md`
**Model**: Sonnet
**Focus**: Test patterns, Spectator usage, coverage

**Reviews**:
- Spectator patterns (`setInput()`, `detectChanges()`, `data-testid`)
- Test structure (AAA pattern, describe blocks, naming)
- Mock quality (proper mocking, reusable factories)
- Test coverage (critical paths, edge cases, errors)
- Async handling (fakeAsync, async/await)
- Common mistakes (test independence, assertions)

**Confidence threshold**: ≥ 75
**Only reviews**: `*.spec.ts` files

## How They Work Together

### Pipeline Execution

The review skill follows a two-phase pipeline:

```typescript
// Phase 1: File classification (single agent)
const fileMap = await Task(
    subagent_type="file-classifier",
    prompt="Classify PR #34553 files by domain",
    description="Classify PR files"
);

// Phase 2: Specialized review (parallel agents, only if REVIEW decision)
if (fileMap.decision === "REVIEW") {
    const [typeResults, angularResults, testResults] = await Promise.all([
        Task(
            subagent_type="typescript-reviewer",
            prompt="Review TypeScript types for PR #34553. Files: <list>",
            description="TypeScript review"
        ),
        Task(
            subagent_type="angular-reviewer",
            prompt="Review Angular patterns for PR #34553. Files: <list>",
            description="Angular review"
        ),
        Task(
            subagent_type="test-reviewer",
            prompt="Review test quality for PR #34553. Files: <list>",
            description="Test review"
        )
    ]);

    // Consolidate results
    mergeAndDeduplicateFindings(typeResults, angularResults, testResults);
}
```

### Non-Overlapping Domains

Each agent has a **clear, exclusive focus**:

| Concern | TypeScript Reviewer | Angular Reviewer | Test Reviewer |
|---------|-------------------|------------------|---------------|
| Type safety (`any`, generics) | ✅ | ❌ | ❌ |
| Angular syntax (`@if`, `input()`) | ❌ | ✅ | ❌ |
| Spectator patterns | ❌ | ❌ | ✅ |
| Null safety | ✅ | ❌ | ❌ |
| Component structure | ❌ | ✅ | ❌ |
| Test coverage | ❌ | ❌ | ✅ |
| Subscriptions | ❌ | ✅ | ❌ |
| Mock quality | ❌ | ❌ | ✅ |

This prevents duplicate findings and ensures expert-level review in each domain.

## Issue Severity Levels

All agents use the same confidence scoring system:

- **95-100** 🔴 **Critical**: Must fix before merge (security, breaks functionality, wrong patterns)
- **85-94** 🟡 **Important**: Should address (performance, memory leaks, poor patterns)
- **75-84** 🔵 **Quality**: Nice to have (improvements, optimizations, clarity)
- **< 75**: Not reported (too minor or uncertain)

**Only issues with confidence ≥ 75 are reported.**

## Agent Permissions & Tools

Each agent has **pre-approved permissions** via `allowed-tools` in their frontmatter:

```yaml
# Example: angular-reviewer.md
allowed-tools:
  - Bash(gh pr diff:*)
  - Bash(gh pr view:*)
  - Read(core-web/**)
  - Read(docs/frontend/**)
  - Grep(*.ts)
  - Grep(*.html)
  - Glob(core-web/**)
```

**Critical**: Agents use **dedicated tools** (Glob, Grep, Read) instead of Bash commands with pipes:

```bash
# ✅ CORRECT: Use dedicated tools
Glob('core-web/**/*.component.ts')
Read(core-web/libs/portlets/my-component.ts)
Grep('@if', path='core-web/', glob='*.html')

# ❌ WRONG: Don't use git diff with pipes
# git diff main --name-only | grep -E '\.ts' | grep -v '\.spec\.ts'
```

**Why?** Bash pipes require complex permissions that trigger repeated user prompts. Dedicated tools have granular, pre-approved permissions.

## Using Agents Independently

While the main `review` skill orchestrates these agents automatically, you can also invoke them directly for focused reviews using their **registered agent types**:

### TypeScript-Only Review
```bash
Task(
    subagent_type="typescript-reviewer",
    prompt="Review TypeScript types for PR #34553",
    description="TypeScript type review"
)
```

### Angular-Only Review
```bash
Task(
    subagent_type="angular-reviewer",
    prompt="Review Angular patterns for PR #34553",
    description="Angular pattern review"
)
```

### Test-Only Review
```bash
Task(
    subagent_type="test-reviewer",
    prompt="Review test quality for PR #34553",
    description="Test quality review"
)
```

## Agent Output Format

Each agent returns findings in this structure:

```markdown
# [Agent Name] Review

## Files Analyzed
- path/to/file1.ts (45 lines)
- path/to/file2.ts (23 lines)

## Critical Issues 🔴 (95-100)
[Detailed findings with file paths, line numbers, code examples, fixes]

## Important Issues 🟡 (85-94)
[Detailed findings...]

## Quality Issues 🔵 (75-84)
[Detailed findings...]

## Summary
- Critical: X
- Important: Y
- Quality: Z

Recommendation: [Approve/Approve with Comments/Request Changes]
```

## Consolidation Strategy

The main review skill consolidates agent outputs:

1. **Collect** all agent findings
2. **Merge** by severity level (Critical, Important, Quality)
3. **Deduplicate** - If multiple agents flag the same line, keep highest confidence
4. **Organize** by domain section (TypeScript Types, Angular Patterns, Tests)
5. **Calculate** overall statistics
6. **Recommend** final approval status

## Benefits of Specialized Agents

✅ **Expertise**: Each agent is an expert in its domain
✅ **Efficiency**: Parallel execution reviews faster
✅ **Clarity**: Clear separation of concerns
✅ **No Duplicates**: Non-overlapping domains prevent redundant findings
✅ **Confidence**: Each agent deeply understands its focus area
✅ **Reusability**: Can be invoked independently when needed

## Extending the System

To add a new specialized agent:

1. Create `agents/your-agent-reviewer.md` with:
   - Clear mission and scope
   - Non-overlapping domain
   - Issue confidence scoring
   - Output format matching others
   - Self-validation checklist

2. Update `SKILL.md` Stage 4 to launch the new agent

3. Update this README with the new agent description

4. Test with sample PRs to ensure no overlap with existing agents

## Examples

### Frontend PR with All Domains
```
Files changed:
- 3 .component.ts files
- 2 .html templates
- 4 .spec.ts files

Agents launched:
✅ typescript-reviewer → Reviews .component.ts for types
✅ angular-reviewer → Reviews .component.ts + .html for patterns
✅ test-reviewer → Reviews .spec.ts for test quality

Result: Comprehensive review with 3 specialized sections
```

### Pure TypeScript Utility PR
```
Files changed:
- 2 .util.ts files (no Angular, no tests)

Agents launched:
✅ typescript-reviewer → Reviews type safety only

Result: Focused type safety review, no Angular/test sections
```

### Test-Only PR
```
Files changed:
- 5 .spec.ts files (test updates only)

Agents launched:
✅ test-reviewer → Reviews test quality only

Result: Focused test quality review, no type/pattern sections
```

## Troubleshooting

**Agent reports issues outside its domain**:
- Review agent's "What NOT to Flag" section
- Ensure agent's scope is clear in the prompt
- Update agent's self-validation checklist

**Duplicate findings across agents**:
- Check "Non-Overlapping Domains" table
- One agent may need refined scope
- Consolidation step should catch and merge

**Low confidence scores**:
- Agent may need more reference examples
- Consider expanding pattern library
- Check if issue is too subjective

**Agent too strict/lenient**:
- Adjust confidence scoring rubric
- Review "Red Flags" vs "What NOT to Flag"
- Calibrate threshold (currently 75)
