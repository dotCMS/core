# GitHub Automation for Claude

This document provides Claude-specific automation patterns and requirements for GitHub issue management using dotCMS utilities.

## 🤖 Core Automation Principles

⚠️ **CRITICAL**: dotCMS utilities are the PREFERRED method for issue creation and management. They handle project assignment, tags, and automation complexity automatically.

### Installation/Updates
```bash
# Install or update dotCMS utilities
bash <(curl -fsSL https://raw.githubusercontent.com/dotcms/dotcms-utilities/main/install-dev-scripts.sh)
```

## 🔧 Claude Automation Patterns

### git-issue-create (PRIMARY METHOD)
🚨 **ESSENTIAL**: Use this for all issue creation - handles project assignment and tags automatically.

#### Claude Automation Workflow
```bash
# 1. ALWAYS use --dry-run first with explicit repo to avoid interactive prompts
git issue-create "Issue title" --team Platform --type Task --repo dotCMS/core --dry-run

# 2. Confirm options with user, then run without --dry-run
git issue-create "Issue title" --team Platform --type Task --repo dotCMS/core --yes

# 3. Create with branch for immediate work
git issue-create "Issue title" --team Platform --type Task --repo dotCMS/core --branch --yes
```

#### Critical for Non-Interactive Usage
⚠️ **ESSENTIAL**: Always specify `--repo` to avoid interactive repository selection prompts, even with `--dry-run`:

```bash
# ❌ WRONG - Will prompt for repository selection
git issue-create "Title" --team Platform --type Task --dry-run

# ✅ CORRECT - Non-interactive with explicit repo
git issue-create "Title" --team Platform --type Task --repo dotCMS/core --dry-run
```

#### Discovery Commands for Claude
```bash
# Discover available teams for a repository
git issue-create --list-teams --repo dotCMS/core --json

# Discover all options for automation
git issue-create --list-all --repo dotCMS/core --json

# Discover available issue types
git issue-create --list-types --json
```

#### Claude Usage Requirements
- **ALWAYS specify --repo**: Use `--repo dotCMS/core` or appropriate repo to avoid interactive prompts
- **ALWAYS use --dry-run first**: Preview before creating, combined with --repo
- **Use --json for parsing**: When needing to process output programmatically
- **Use --yes for automation**: Skip confirmation prompts after --dry-run validation
- **Select appropriate team**: Platform, CloudEng, QA, Frontend, Backend
- **Choose correct type**: Enhancement, Defect, Task, or --epic flag

#### Non-Interactive Command Pattern
```bash
# Standard Claude pattern for issue creation
git issue-create "Title" --team TeamName --type TaskType --repo RepoName --dry-run
# [Confirm with user]
git issue-create "Title" --team TeamName --type TaskType --repo RepoName --yes
```

### git-smart-switch (BRANCH MANAGEMENT)
🚨 **ESSENTIAL**: Enhanced branch switching with WIP management.

#### Claude Automation Workflow
```bash
# 1. Switch to or create new branch (creates automatically if doesn't exist)
git smart-switch issue-12345-feature-name

# 2. Preview operations first for complex moves
git smart-switch target-branch --dry-run

# 3. Use automation flags
git smart-switch target-branch --yes --json

# 4. Smart move commits with preview
git smart-switch correct-branch -m --dry-run
```

#### Claude Usage Requirements
- **Use --dry-run for preview**: Always preview complex operations
- **Use --json for parsing**: When needing to process output
- **Use --yes for automation**: Skip interactive confirmations
- **Understand state preservation**: -k preserves source, -w moves working changes

### git-issue-pr (PR CREATION)
💡 **IMPORTANT**: Create pull requests linked to GitHub issues with automation support.

#### Claude Automation Workflow
```bash
# 1. Automated PR creation with specific options
git issue-pr --type fix --scope core --draft --yes

# 2. Custom PR with full automation
git issue-pr --title "Custom PR Title" --body "Custom description" --yes

# 3. JSON output for processing
git issue-pr --json
```

#### Claude Usage Requirements
- **Use --yes for automation**: Skip confirmation prompts
- **Use --json for parsing**: When needing to process output
- **Specify --type appropriately**: fix, feat, chore, refactor, docs, test, ci, build, perf, style, revert
- **Use --draft for WIP**: Create as draft when work is incomplete

### git-issue-branch (ISSUE SELECTION)
💡 **IMPORTANT**: Interactive issue selection with automation support.

#### Claude Automation Usage
```bash
# List issues in JSON format for processing
git issue-branch --list --json
```

#### Claude Usage Requirements
- **Use --json for parsing**: When needing to process issue lists
- **Process PR indicators**: Handle [has PR] status in automation logic

## 🚨 Fallback to Traditional Methods

### When to Use GitHub CLI/API
Use traditional methods only when dotCMS utilities don't support functionality:
- **Sub-issue management**: git-issue-create doesn't currently support sub-issues
- **GitHub V2 Project modifications**: Advanced project board operations
- **Utilities unavailable**: Commands not installed or reporting errors

### GitHub Sub-issues API (Fallback Only)
⚠️ **CRITICAL**: For sub-issue management when utilities don't support it.

```bash
# Get internal IDs (required for sub-issues API)
EPIC_ID=$(gh api /repos/dotCMS/core/issues/32675 --jq '.id')
SUBTASK_ID=$(gh api /repos/dotCMS/core/issues/32679 --jq '.id')

# Link subtask to epic
gh api -X POST /repos/dotCMS/core/issues/32675/sub_issues --field sub_issue_id=$SUBTASK_ID

# Check epic progress
gh api /repos/dotCMS/core/issues/32675 --jq '.sub_issues_summary'
```

## 📋 Claude Decision Tree

### For Issue Creation
1. **First choice**: `git issue-create` with --repo and --dry-run for non-interactive usage
2. **If unavailable**: Traditional `gh issue create`
3. **For sub-issues**: Use GitHub API (git-issue-create doesn't support sub-issues yet)

#### Issue Creation Decision Tree
```bash
# Step 1: Check if git-issue-create is available
if command -v git-issue-create &> /dev/null; then
    # Step 2: Use non-interactive pattern
    git issue-create "Title" --team Platform --type Task --repo dotCMS/core --dry-run
    # Step 3: Confirm with user, then execute
    git issue-create "Title" --team Platform --type Task --repo dotCMS/core --yes
else
    # Step 4: Fall back to traditional method
    gh issue create --title "Title" --label "Team : Platform" --label "Type : Task"
fi
```

### For Branch Management
1. **First choice**: `git smart-switch` with --dry-run for complex operations
2. **If unavailable**: Traditional `git checkout -b`

### For PR Creation
1. **First choice**: `git issue-pr` with appropriate flags
2. **If unavailable**: Traditional `gh pr create`

### For Issue Discovery
1. **First choice**: `git issue-branch --list --json`
2. **If unavailable**: Traditional `gh issue list`

## 🔍 Automation Best Practices

### Proactive Workflow Suggestions

#### When User Mentions Different Work Context
If user asks about working on something different, suggest branch switching:

```bash
# Suggest switching to appropriate branch
"It sounds like you're working on a different feature/issue. Would you like to switch to the appropriate branch first?"

# Then offer to help with branch switching
git smart-switch issue-12345-new-feature-name
```

#### When Discovering Unrelated Issues
If unrelated bugs/issues are found during development, suggest creating issues:

```bash
# Suggest creating issue for discovered problems
"I noticed [describe issue]. Would you like me to create a quick issue to track this?"

# Then offer to create issue
git issue-create "Fix [brief description]" --team Platform --type Defect --repo dotCMS/core --dry-run
```

#### Common Proactive Suggestions
- **Branch switching**: When context changes or user mentions different work
- **Issue creation**: When bugs/improvements are discovered during development
- **Epic tracking**: When discussing large features that need breakdown
- **PR creation**: When work is complete and ready for review

### Error Handling
```bash
# Check if utilities are available
if ! command -v git-issue-create &> /dev/null; then
    echo "dotCMS utilities not available, using traditional methods"
    # Fall back to gh cli
fi

# Handle utility updates
if git-issue-create --help | grep -q "new version available"; then
    echo "Updating dotCMS utilities..."
    bash <(curl -fsSL https://raw.githubusercontent.com/dotcms/dotcms-utilities/main/install-dev-scripts.sh)
fi
```

### JSON Processing
```bash
# Parse team options
TEAMS=$(git issue-create --list-teams --json | jq -r '.teams[]')

# Parse issue list
ISSUES=$(git issue-branch --list --json | jq -r '.issues[] | select(.state == "open")')
```

### Confirmation Patterns
```bash
# Always confirm before final action
git issue-create "New feature" --team Platform --type Task --dry-run
echo "Creating issue with above configuration. Proceed? (y/n)"
read -r response
if [[ "$response" == "y" ]]; then
    git issue-create "New feature" --team Platform --type Task --yes
fi
```

## 📚 Related Claude Documentation

- [Workflow Patterns](WORKFLOW_PATTERNS.md) - General Claude workflow patterns
- [Documentation Maintenance](DOCUMENTATION_MAINTENANCE.md) - How to maintain this documentation

---

**Note**: This document is specifically for Claude automation patterns. For general developer usage, see [GitHub Issue Management](../core/GITHUB_ISSUE_MANAGEMENT.md).