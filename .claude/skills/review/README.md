# Autonomous PR Review Skill

**Single-command, intelligent PR reviews** that automatically detect whether changes are frontend, backend, config, or docs — and apply the appropriate review standards.

## Problem Solved

Previously, you had to:
- Manually determine if a PR was frontend or backend
- Run different review skills depending on the domain
- Deal with Claude applying the wrong review lens (frontend reviewer on backend code)
- Validate that the review matched the actual changes

This skill **automates all of that** with a single command.

## Quick Start

```bash
# Review any PR - automatically selects correct lens
/review 34535
/review https://github.com/dotCMS/core/pull/34535

# That's it! The skill will:
# 1. Fetch the PR diff
# 2. Classify files (frontend/backend/config/docs)
# 3. Select the right review lens
# 4. Apply domain-specific standards
# 5. Self-validate before showing you results
```

## What Makes This Special

### 🎯 Automatic Domain Detection
- Analyzes file extensions and paths
- Calculates percentage of changes per domain
- Selects primary review lens based on data (not guessing)

### 🤖 Specialized Review Agents (NEW!)
For frontend code, launches **3 parallel expert agents** using registered agent types:
- **TypeScript Reviewer** (`typescript-reviewer`): Type safety, generics, null handling
- **Angular Reviewer** (`angular-reviewer`): Modern syntax, component patterns, architecture
- **Test Reviewer** (`test-reviewer`): Spectator patterns, coverage, test quality

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
- Review lens matches the actual changes
- No contradictory recommendations
- No duplicate findings across agents

### 📊 Multi-Domain Support
For PRs that touch both frontend and backend:
- Provides **separate sections** for each domain
- Applies appropriate standards to each
- Clear risk assessment per domain

### 🔍 Comprehensive Standards
- **Frontend** (3 specialized agents):
  - TypeScript type safety and quality
  - Modern Angular patterns and architecture
  - Spectator test patterns and coverage

## File Structure

```
.claude/
├── agents/                           # ⭐ Specialized review agents (reusable)
│   ├── README.md                     # Agent documentation
│   ├── typescript-reviewer.md        # TypeScript type safety specialist
│   ├── angular-reviewer.md           # Angular patterns specialist
│   └── test-reviewer.md              # Test quality specialist
└── skills/review/
    ├── SKILL.md                      # Main skill logic (orchestrates agents)
    ├── README.md                     # This file
    └── references/
        └── README.md                 # References docs/frontend/ as source of truth
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
Agents Launched: typescript-reviewer, angular-reviewer, test-reviewer
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
- Large PRs may need focus: "Review backend changes only"
- Check if PR is from a fork (may have permission issues)

**"Wrong lens selected"**:
- This shouldn't happen with the new classification logic
- If it does, report the file distribution so we can refine

## Created

Based on usage insights analysis from 25 Claude Code sessions (2026-01-08 to 2026-02-06).

Addresses friction pattern: "Wrong review domain wastes full review cycles"
