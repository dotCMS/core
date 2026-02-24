# Autonomous PR Review Skill

**Single-command, intelligent frontend PR reviews** that automatically detect whether a PR is frontend-focused and launches specialized review agents in parallel.

## Problem Solved

Previously, you had to:
- Manually determine if a PR had enough frontend changes to warrant review
- Run review agents individually (TypeScript, Angular, Test)
- Deal with reviewing non-frontend PRs that didn't need frontend analysis
- Manually consolidate findings from multiple review passes

This skill **automates all of that** with a single command.

## Quick Start

```bash
# Review any PR - automatically detects if frontend-focused
/review 34535
/review https://github.com/dotCMS/core/pull/34535

# That's it! The skill will:
# 1. Fetch the PR diff
# 2. Classify files (frontend vs non-frontend)
# 3. Determine if frontend-focused (>50% frontend files)
# 4. Launch specialized agents in parallel (TypeScript, Angular, Test)
# 5. Consolidate findings and self-validate before showing results
```

## What Makes This Special

### 🎯 Automatic Frontend Detection
- Analyzes file extensions and paths to identify frontend files
- Calculates percentage of frontend changes in the PR
- Only proceeds with review if >50% of files are frontend (TypeScript, Angular, tests, SCSS)

### 🤖 Specialized Review Agents (NEW!)
For frontend code, launches **3 parallel expert agents** using registered agent types:
- **TypeScript Reviewer** (`dotcms-typescript-reviewer`): Type safety, generics, null handling
- **Angular Reviewer** (`dotcms-angular-reviewer`): Modern syntax, component patterns, architecture
- **Test Reviewer** (`dotcms-test-reviewer`): Spectator patterns, coverage, test quality

Each agent is a domain expert with:
- Non-overlapping focus areas (no duplicate findings)
- Parallel execution (faster reviews)
- Confidence-based scoring (≥75 threshold)
- Self-validation before reporting
- Pre-approved permissions (no repeated prompts)
- Uses dedicated tools (Glob, Grep, Read) for efficiency

**Key Innovation**: Agents are **registered types** in the system, not generic workers. They have specialized prompts, pre-configured permissions, and domain expertise built-in.

See [`.claude/agents/README.md`](../../agents/README.md) for agent details.

### 🛡️ Self-Validation
Before showing you the review, it verifies:
- All referenced files actually exist in the diff
- Line numbers are accurate
- All findings are within frontend domain scope
- No contradictory recommendations
- No duplicate findings across agents

### 🔍 Comprehensive Frontend Standards
Three specialized agents work in parallel to review:
- **TypeScript Type Safety**: Generics, type quality, null handling, unsafe patterns
- **Angular Patterns**: Modern syntax (@if/@for), standalone components, lifecycle, subscriptions
- **Test Quality**: Spectator patterns, coverage, mocking, async handling

## File Structure

```
.claude/
├── agents/                           # ⭐ Specialized review agents (reusable)
│   ├── README.md                     # Agent documentation
│   ├── dotcms-typescript-reviewer.md  # TypeScript type safety specialist
│   ├── dotcms-angular-reviewer.md    # Angular patterns specialist
│   └── dotcms-test-reviewer.md       # Test quality specialist
└── skills/review/
    ├── SKILL.md                      # Main skill logic (orchestrates agents)
    └── README.md                     # This file
```

## Examples

### Example 1: Frontend-Only PR
```
$ /review 34535

Files changed: 1 SCSS, 12 VTL templates
Review Lens: Frontend-Only
Output: Focuses on SCSS standards, template patterns, no backend checks
```

### Example 2: Angular Component PR
```
$ /review 34553

Files changed: 3 TypeScript components, 2 HTML templates, 3 spec files
Review Lens: Frontend-Only
Agents Launched: dotcms-typescript-reviewer, dotcms-angular-reviewer, dotcms-test-reviewer
Output: Comprehensive frontend review with findings from all 3 agents
```

## Output Format

Every review includes:

```markdown
# PR Review: #<NUMBER> - <TITLE>

## Summary
[Brief overview + risk level]

## Risk Assessment
- Security: [Low/Medium/High]
- Breaking Changes: [None/Potential/Confirmed]
- Performance Impact: [Low/Medium/High]
- Test Coverage: [Good/Partial/Missing]

## [Domain] Findings
### Critical Issues 🔴
[Must fix before merge]

### Improvements 🟡
[Should address]

### Nitpicks 🔵
[Nice to have]

## Approval Recommendation
[✅ Approve | ⚠️ Approve with Comments | ❌ Request Changes]
```

## Architecture

This skill **orchestrates** specialized agents:
- **Agents are workers**: Independent reviewers with focused expertise (`.claude/agents/`)
- **Skill is orchestrator**: Launches agents in parallel, consolidates findings
- **References are truth**: Single source of truth that agents read dynamically

**Replaces**: `dotcms-code-reviewer-frontend` and manual domain detection

Use `/review` as your **single entry point** for frontend PR reviews.

## Tips

- **Re-run after updates**: `/review 34535` again to check if issues were addressed
- **Focus on specific areas**: "Review PR 34535, focus on security" or "Check test coverage"
- **Large PRs**: For 50+ file PRs, you can ask Claude to focus on critical areas first
- **Draft PRs**: Mention "This is a draft PR" so expectations are adjusted

## Integration with Your Workflow

This skill fits into the autonomous PR review pipeline suggested in your insights report:

1. **You**: `/review 34535`
2. **Claude**: Fetches diff, classifies files, selects lens, reviews, validates
3. **You**: Review findings, request changes or approve
4. **Developer**: Makes updates
5. **You**: `/review 34535` (re-run to verify fixes)

## Future Enhancements

As Claude's capabilities improve, this skill could:
- Auto-comment on the PR with findings
- Track which issues were addressed between reviews
- Compare against main branch to predict merge conflicts
- Suggest reviewers based on file ownership

## Troubleshooting

**"Unable to fetch PR"**:
- Verify PR number: `gh pr list --limit 20`
- Check you're in the correct repo directory
- Ensure `gh` CLI is authenticated: `gh auth status`

**"Review seems to miss files"**:
- Large PRs may need focus: "Focus on TypeScript type issues" or "Review only test files"
- Check if PR is from a fork (may have permission issues)

**"Wrong lens selected"**:
- This shouldn't happen with the new classification logic
- If it does, report the file distribution so we can refine

## Created

Based on usage insights analysis from 25 Claude Code sessions (2026-01-08 to 2026-02-06).

Addresses friction pattern: "Wrong review domain wastes full review cycles"
