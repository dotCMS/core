# CI/CD Diagnostics Skill

Expert diagnostic tool for analyzing DotCMS CI/CD build failures in GitHub Actions.

## Skill Overview

This skill provides automated diagnosis of CI/CD failures across all DotCMS workflows:
- **cicd_1-pr.yml** - Pull Request validation
- **cicd_2-merge-queue.yml** - Pre-merge full validation
- **cicd_3-trunk.yml** - Post-merge deployment
- **cicd_4-nightly.yml** - Scheduled full test runs

## Capabilities

### 🔍 Intelligent Failure Analysis
- Identifies failed jobs and steps
- Extracts relevant errors from large log files efficiently
- Classifies failures (new, flaky, infrastructure, test filtering)
- Compares workflow results (PR vs merge queue)
- Checks historical patterns across runs

### 📊 Root Cause Determination
- New failures introduced by specific commits
- Flaky tests with failure rate calculation
- Infrastructure issues (timeouts, connectivity)
- Test filtering discrepancies between workflows
- External dependency changes

### 🔗 GitHub Integration
- Searches existing issues for known problems
- Creates detailed GitHub issues with proper labels
- Links failures to related PRs and commits
- Provides actionable recommendations

### ⚡ Efficiency Optimized
- Progressive disclosure of log analysis
- Streaming search without full extraction
- Job-specific log downloads
- Pattern-based error detection
- Context window optimized

## Skill Structure

```
cicd-diagnostics/
├── SKILL.md              # Main skill instructions (concise, <300 lines)
├── WORKFLOWS.md          # Detailed workflow documentation
├── LOG_ANALYSIS.md       # Advanced log analysis techniques
├── ISSUE_TEMPLATE.md     # GitHub issue templates
└── README.md             # This file
```

## Usage

The skill activates automatically when you ask questions like:

- "Why did the build fail?"
- "Check CI/CD status"
- "Analyze run 19131365567"
- "Is ContentTypeAPIImplTest flaky?"
- "Why did my PR pass but merge queue fail?"
- "What's blocking the merge queue?"
- "Debug the nightly build failure"

Or invoke explicitly:
```bash
/cicd-diagnostics
```

## Example Scenarios

### Scenario 1: Analyze Specific Run
```
You: "Analyze https://github.com/dotCMS/core/actions/runs/19131365567"

Skill:
1. Extracts run ID and fetches run details
2. Identifies failed jobs and steps
3. Downloads and analyzes logs efficiently
4. Determines root cause with evidence
5. Checks for known issues
6. Provides actionable recommendations
```

### Scenario 2: Check Current PR
```
You: "Check my PR build status"

Skill:
1. Gets current branch name
2. Finds associated PR
3. Gets latest PR workflow runs
4. Analyzes any failures
5. Reports status and recommendations
```

### Scenario 3: Flaky Test Investigation
```
You: "Is ContentTypeAPIImplTest flaky?"

Skill:
1. Searches nightly build history
2. Counts failures vs successes
3. Calculates failure rate
4. Checks existing flaky test issues
5. Recommends action (fix vs quarantine)
```

### Scenario 4: Workflow Comparison
```
You: "Why did PR pass but merge queue fail?"

Skill:
1. Gets PR workflow results
2. Gets merge queue results for same commit
3. Identifies test filtering differences
4. Explains discrepancy
5. Recommends fixing the filtered tests
```

## Key Principles

### Efficiency First
- Start with high-level status (30 sec)
- Progress to detailed logs only if needed (5+ min)
- Use streaming and filtering for large files
- Target specific patterns based on failure type

### Workflow Context Matters
- **PR failures** → Usually code issues or filtered tests
- **Merge queue failures** → Test filtering, conflicts, or flaky tests
- **Trunk failures** → Deployment/artifact issues
- **Nightly failures** → Flaky tests or infrastructure

### Progressive Investigation
1. Run status → Failed jobs (30 sec)
2. Maven errors → Test failures (2 min)
3. Full log analysis (5+ min, only if needed)
4. Historical comparison (2 min)
5. Issue creation (2 min, if needed)

## Reference Files

### SKILL.md
Main skill instructions with:
- Core workflow types
- 7-step diagnostic approach
- Key principles and efficiency tips
- Success criteria

**Use**: Core instructions loaded when skill activates

### WORKFLOWS.md
Detailed workflow documentation:
- Each workflow's purpose and triggers
- Common failure patterns with detection methods
- Test strategies and typical durations
- Cross-cutting failure causes
- Diagnostic decision tree

**Use**: Reference when you need detailed workflow-specific information

### LOG_ANALYSIS.md
Advanced log analysis techniques:
- Smart download strategies
- Pattern matching for different error types
- Efficient search workflows
- Context window optimization
- Quick reference commands

**Use**: Reference when analyzing logs to find specific patterns efficiently

### ISSUE_TEMPLATE.md
GitHub issue templates:
- Build Failure Report
- Flaky Test Report
- Infrastructure Issue Report
- Failure Update Comment
- Label standards and conventions

**Use**: Reference when creating or updating GitHub issues

## Best Practices

### Do ✅
- Start with job status before downloading logs
- Use streaming (`unzip -p`) for large archives
- Search for Maven `[ERROR]` first
- Check test filtering differences (PR vs merge queue)
- Compare with historical runs
- Search existing issues before creating new ones
- Provide specific, actionable recommendations

### Don't ❌
- Download entire log archives unnecessarily
- Try to read full logs without filtering
- Assume PR passing means all tests pass (filtering!)
- Create duplicate issues without searching
- Provide vague recommendations
- Ignore workflow context

## Integration with GitHub CLI

All commands use `gh` CLI for:
- Workflow run queries
- Job and step details
- Log downloads
- Artifact management
- Issue search and creation
- PR status checks

**Required**: `gh` CLI installed and authenticated

## Output Format

Standard diagnostic report structure:
```markdown
## CI/CD Failure Diagnosis: [workflow] #[run-id]

**Root Cause**: [Category] - [Explanation]
**Confidence**: [High/Medium/Low]

### Failure Details
[Specific job, step, test information]

### Classification
[Type, frequency, related issues]

### Evidence
[Key log excerpts, commits, patterns]

### Recommendations
[Actionable steps with commands/links]
```

## Success Criteria

A successful diagnosis provides:
1. ✅ Specific failure point (job, step, test)
2. ✅ Root cause category with evidence
3. ✅ New vs recurring classification
4. ✅ Known issue status
5. ✅ Actionable recommendations
6. ✅ Issue creation if needed

## Contributing

When updating this skill:
1. Keep SKILL.md concise (<500 lines)
2. Move detailed content to reference files
3. Maintain one level of reference depth
4. Test with real failure scenarios
5. Update examples with actual patterns
6. Keep commands up-to-date with gh CLI

## Version History

- **v1.0** (2025-11-06) - Initial skill creation
  - Four workflow support
  - Progressive disclosure structure
  - Efficient log analysis
  - GitHub issue integration