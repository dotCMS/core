# Claude Code Skills (DotCMS Core Repository)

This directory contains **repository-specific skills** for Claude Code that enhance development workflows for the entire DotCMS core project (both backend and frontend).

## 📁 Directory Structure

```
.claude/
├── agents/                           # Reusable agents (independent workers)
│   ├── README.md                     # Agent documentation
│   ├── typescript-reviewer.md        # TypeScript type safety specialist
│   ├── angular-reviewer.md           # Angular patterns specialist
│   └── test-reviewer.md              # Test quality specialist
│
└── skills/
    └── review/                       # Autonomous PR Review System
        ├── SKILL.md                  # Main skill orchestrator (invokes agents)
        ├── README.md                 # Skill documentation
        ├── USAGE_EXAMPLES.md         # Usage examples
        ├── PERMISSIONS.md            # Permission configuration
        ├── IMPROVEMENTS.md           # Improvement roadmap
        └── references/               # Points to docs/frontend/ (source of truth)
            └── README.md
```

**Key Architectural Point**:
- **Skills** (`.claude/skills/`) = Workflows/processes that orchestrate work
- **Agents** (`.claude/agents/`) = Independent workers that execute specialized tasks
- Agents can be reused by multiple skills or invoked directly

## 🎯 Available Skills

### `/review` - Autonomous PR Review System

Intelligent, multi-agent PR reviewer that automatically:
1. **Classifies files** by domain (frontend, backend, config, docs)
2. **Launches specialized agents** in parallel for comprehensive review
3. **Consolidates findings** by severity with confidence scores
4. **Provides actionable feedback** with file:line references and fixes

**Usage**:
```bash
/review <PR_NUMBER>
/review <PR_URL>

# Examples
/review 34553
/review https://github.com/dotCMS/core/pull/34553
```

**Specialized Agents** (run in parallel for frontend PRs):
- 🔷 **TypeScript Reviewer**: Type safety, generics, null handling (confidence ≥ 75)
- 🟣 **Angular Reviewer**: Modern syntax, component architecture, lifecycle (confidence ≥ 75)
- 🟢 **Test Reviewer**: Spectator patterns, coverage, test quality (confidence ≥ 75)

**Review Domains**:
- ✅ **Frontend** (79%+ frontend files): Launches 3 specialized agents
- ✅ **Backend** (80%+ Java files): Java standards, REST API, security
- ✅ **Multi-Domain**: Separate sections for each domain
- ✅ **Config/Docs**: Syntax validation, security checks

See [`review/USAGE_EXAMPLES.md`](review/USAGE_EXAMPLES.md) for detailed examples.

## 🔄 Local vs Global Skills

**Repository-Local Skills** (`.claude/skills/`):
- ✅ Versioned with the project
- ✅ Shared with the entire team
- ✅ Project-specific workflows and patterns
- ✅ Updated via git commits/PRs
- ✅ Works for both backend (Java) and frontend (Angular/TypeScript)

**Global Skills** (`~/.claude/skills/`):
- Personal skills not specific to this project
- User-specific preferences and workflows
- Not shared with team

## 🚀 How Skills Work

When you invoke a skill (e.g., `/review 34553`), Claude:

1. **Loads the skill** from `.claude/skills/review/SKILL.md`
2. **Follows the instructions** in the skill file
3. **Launches sub-agents** if needed (for parallel reviews)
4. **Returns consolidated results** to you

Skills are essentially **structured prompts with logic** that guide Claude through complex, multi-step workflows.

## 📊 Review Output Format

```markdown
# PR Review: #34553 - Title

## Summary
[Overview with risk level]

## Risk Assessment
- Security: Low/Medium/High
- Breaking Changes: None/Potential/Confirmed
- Performance Impact: Low/Medium/High
- Test Coverage: Good/Partial/Missing

## Frontend Findings (if applicable)
### TypeScript Type Safety
#### Critical Issues 🔴 (95-100)
#### Important Issues 🟡 (85-94)
#### Quality Issues 🔵 (75-84)

### Angular Patterns
[Same structure]

### Test Quality
[Same structure]

## Backend Findings (if applicable)
### Critical Issues 🔴
### Improvements 🟡
### Nitpicks 🔵

## Approval Recommendation
✅ Approve | ⚠️ Approve with Comments | ❌ Request Changes
```

## 📝 Creating New Skills

To add a new skill for this repository:

1. Create a directory: `.claude/skills/your-skill-name/`
2. Add `SKILL.md` with the skill logic
3. Add `README.md` documenting the skill
4. Test the skill: `/your-skill-name <args>`
5. Commit to the repo for team use

**Tip**: Use the `skill-creator` skill to help design new skills:
```bash
/skill-creator
```

## 🔧 Maintaining Skills

**Updating Skills**:
- Edit the `.md` files in `.claude/skills/`
- Test your changes with real PRs
- Commit via git
- Team gets updates on next pull

**Best Practices**:
- Keep skills focused on a single workflow
- Document usage clearly with examples
- Include error handling and edge cases
- Test with real PRs/data before committing
- Use sub-agents for parallelizable work
- Set appropriate confidence thresholds

## 📚 Documentation

Each skill has its own documentation:
- **`SKILL.md`**: The skill logic (what Claude executes)
- **`README.md`**: User-facing documentation
- **`USAGE_EXAMPLES.md`**: Practical examples and use cases

## 🎨 Skill Design Patterns

### Multi-Agent Pattern (Review Skill)
```
Main Skill → Detects domain → Launches specialized agents in parallel
           → Consolidates results → Presents unified review
```

### Confidence Scoring
```
95-100 🔴 Critical: Must fix before merge
85-94  🟡 Important: Should address
75-84  🔵 Quality: Nice to have
< 75   Not reported (too minor/uncertain)
```

### Self-Validation
Every agent validates:
- File existence in PR diff
- Line number accuracy
- Domain scope adherence
- No duplicate findings
- Evidence-based conclusions

## 🤝 Contributing

To improve existing skills:

1. **Edit locally**: Modify skill files in `.claude/skills/`
2. **Test thoroughly**: Use with real PRs covering edge cases
3. **Document changes**: Update README and examples
4. **Create PR**: Submit changes for team review
5. **Iterate**: Address feedback and refine

## 💡 Tips for Using Skills

- **Re-run reviews**: After fixing issues, run `/review <PR>` again to verify
- **Focus reviews**: "Review PR 34553, focus on security"
- **Staged reviews**: For large PRs, review by domain: "Review only frontend files"
- **Check confidence**: Issues with confidence ≥ 75 are actionable
- **Trust agents**: Agents are trained on project patterns (CLAUDE.md)

## 🐛 Troubleshooting

**"Skill not found"**:
- Ensure you're in the repository root directory
- Check that `.claude/skills/review/SKILL.md` exists

**"Wrong review lens selected"**:
- Check file classification in Stage 2 output
- Report if file types are misclassified

**"Missing issues I expected"**:
- Check confidence threshold (75)
- Issues may be pre-existing (not in diff)
- Code might actually meet standards!

**"Too many issues"**:
- Focus on Critical 🔴 first
- Important 🟡 should be addressed
- Quality 🔵 can wait for follow-up

## 📖 Resources

- [Claude Code Documentation](https://docs.anthropic.com/claude/docs)
- [Review Skill Documentation](review/README.md)
- [Review Agent Documentation](review/agents/README.md)
- [Usage Examples](review/USAGE_EXAMPLES.md)
- [DotCMS CLAUDE.md](../CLAUDE.md) - Project guidelines
- [Frontend Guidelines](../core-web/CLAUDE.md) - Angular/TypeScript standards

## 📈 Metrics

**Review Skill Performance** (PR #34553 example):
- **Files analyzed**: 47 files (37 frontend, 6 config, 2 docs, 2 other)
- **Agents launched**: 3 (TypeScript, Angular, Test) in parallel
- **Execution time**: ~3-4 minutes (parallel) vs ~10-15 minutes (sequential)
- **Issues found**: 4 critical, 9 important, 13 quality
- **Test coverage analyzed**: 1,112 lines across 5 test files
- **TypeScript analyzed**: 632 lines of production code

**Quality Improvements**:
- Catches issues before human review
- Consistent application of standards
- Actionable feedback with fixes
- Reduces review iteration cycles

---

**Repository**: [dotCMS/core](https://github.com/dotCMS/core)
**Last Updated**: 2026-02-10
**Maintained By**: DotCMS Core Team
**Skill Version**: 1.0.0
