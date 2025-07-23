# GitHub Issue Management - dotCMS

This document provides guidance for developers managing GitHub Issues, PRs, Epics, and Subtasks following dotCMS patterns and conventions.

**For Claude/AI automation patterns, see [GitHub Automation](../claude/GITHUB_AUTOMATION.md)**

## 🎯 Core Principles

### Issue Management Standards
- **Branch naming**: Always use `issue-{number}-{description}` format
- **Epic tracking**: Use GitHub sub-issues API for proper parent-child relationships
- **PR linking**: Always link to both Epic and specific Issue  
- **Project integration**: All issues auto-added to "dotCMS - Product Planning V2"

### Quality Requirements
- **Issue creation**: Include clear acceptance criteria and proper labels
- **PR standards**: Comprehensive descriptions with testing notes
- **Epic breakdown**: Manageable subtasks with realistic estimates

## 📋 Quick Reference Commands

### Issue Operations (Traditional - Use dotCMS utilities when available)
```bash
# PREFER: git issue-create --title "Task description" --team Platform --type Task --dry-run
# FALLBACK: Manual gh issue create
gh issue create --title "Task description" --label "Team : Platform" --label "Type : Task"

# PREFER: git smart-switch issue-{issue_number}-{descriptive-name}
# FALLBACK: Manual branch creation
git checkout -b issue-{issue_number}-{descriptive-name}

# Close issue with reference
gh issue close 12345 --comment "✅ Completed via PR #12346"
```

### Epic Management
```bash
# Link subtask to epic (requires internal IDs)
gh api -X POST /repos/:owner/:repo/issues/{epic_id}/sub_issues --field sub_issue_id={task_id}

# Check epic progress
gh api /repos/:owner/:repo/issues/{epic_id} --jq '.sub_issues_summary'

# Get internal issue ID
gh api /repos/:owner/:repo/issues/{issue_number} --jq '.id'
```

### PR Operations (Traditional - Use dotCMS utilities when available)
```bash
# PREFER: git issue-pr --type fix --scope core --yes
# FALLBACK: Manual gh pr create
gh pr create --title "Brief description" --body "Detailed description with sections"

# Create draft PR for work in progress
gh pr create --draft --title "WIP: Feature description"
```

## 🚀 Development Utilities

💡 **IMPORTANT**: dotCMS utilities are the preferred method for issue creation and management. They handle project assignment, tags, and complexity automatically.

### dotCMS Utilities
The dotCMS utilities from https://github.com/dotCMS/dotcms-utilities provide enhanced GitHub workflow management.

#### Installation/Updates
```bash
# Install or update dotCMS utilities
bash <(curl -fsSL https://raw.githubusercontent.com/dotcms/dotcms-utilities/main/install-dev-scripts.sh)
```

#### git-issue-create
Create issues with automatic project integration and proper labeling.

```bash
# Interactive issue creation (recommended for developers)
git issue-create

# Create with specific options
git issue-create "Issue title" --team Platform --type Task

# Create and switch to branch automatically
git issue-create "Issue title" --team Platform --type Task --branch

# Create epic
git issue-create "Epic title" --team Platform --epic
```

**Key Options:**
- **--team**: Platform, CloudEng, QA, Frontend, Backend
- **--type**: Enhancement, Defect, Task
- **--priority**: "1 Critical", "2 High", "3 Medium", "4 Low"
- **--branch**: Create and switch to branch after issue creation
- **--epic**: Create as Epic type
- **--assignee**: Assign to specific user

#### git-smart-switch
Enhanced branch switching with WIP management and intelligent commit movement.

```bash
# Switch to existing branch or create new one (creates automatically if doesn't exist)
git smart-switch feature-branch

# Create branch from current state (preserves working state on source)
git smart-switch new-branch -k

# Smart move commits with strategy selection
git smart-switch correct-branch -m

# Move only working changes to target (source becomes clean)
git smart-switch target-branch -w
```

**Smart Move Features:**
- **Three rebase strategies**: Clean slate, preserve source, stack on target
- **Conflict pre-detection**: Guided resolution with rollback option
- **Automatic backups**: Safety with rollback capabilities
- **Working state preservation**: Maintains changes during moves

#### git-issue-branch
Interactive tool for selecting from your assigned issues and creating/switching branches.

```bash
# Interactive issue and branch selection
git issue-branch

# List issues without interactive selection
git issue-branch --list
```

**Features:**
- Lists assigned issues and recently created issues
- Shows PR status indicators for existing pull requests
- Most recently updated issues appear first
- Create new branches or switch to existing linked branches

#### git-issue-pr
Create pull requests linked to GitHub issues with conventional commit support.

```bash
# Interactive PR creation
git issue-pr

# Create with specific commit type and scope
git issue-pr --type fix --scope core

# Create as draft
git issue-pr --type feat --scope uve --draft
```

**Features:**
- Auto-detects issue from branch name (issue-{number}-{description})
- Fetches issue title from GitHub automatically
- Supports conventional commit format
- Automatically links PR to issue

## 📊 Issue Types & Labels

### Standard Issue Types
- **Task**: Feature development, technical work
- **Defect**: Bug fixes, regression issues
- **Epic**: Large initiatives with multiple subtasks
- **Enhancement**: Improvements to existing features

### Required Labels
- **Team**: `Team : Platform`, `Team : CloudEng`, etc.
- **Type**: `Type : Task`, `Type : Defect`, `Type : Epic`
- **Priority**: `Priority : High`, `Priority : Medium`, `Priority : Low` (when applicable)

## 🔄 Workflow Patterns

### Standard Flow: Epic → Issue → PR → Branch
```
Epic #12345 "Feature Initiative"
├── Issue #12346 "Implement core functionality"
│   ├── Branch: issue-12346-implement-core-functionality
│   └── PR #12347 "Implement core functionality"
└── Issue #12348 "Add integration tests"
    ├── Branch: issue-12348-add-integration-tests
    └── PR #12349 "Add integration tests"
```

### Issue Lifecycle States
1. **New** → **In Progress** → **Review** → **Done**
2. Automatic project board updates
3. Epic progress tracking via subtask completion

## 🏗️ Epic Management

### Epic Structure Requirements
- **Clear scope**: Business value and technical objectives
- **Manageable subtasks**: Break down into 2-5 day tasks
- **Proper linking**: Use GitHub sub-issues API
- **Progress tracking**: Automatic via subtask completion

### Epic Template Structure
**Required Epic Description Format:**
```markdown
### Epic Description
**Title**: Clear epic name
**Description**: Detailed explanation of the epic scope

### Business Value
- Security improvements
- Performance benefits
- Developer experience improvements

### Scope
- High-level objectives
- What's included/excluded
- Dependencies and requirements

### Success Criteria
- Measurable outcomes
- Acceptance criteria for epic completion

### Epic Timeline
With Claude assistance: X weeks (vs Y weeks without)

### Sub-Tasks & Estimates
#### Phase 1: Foundation (X weeks)
- Task 1.1: Description (X days)
- Task 1.2: Description (X days)

#### Phase 2: Implementation (X weeks)
- Task 2.1: Description (X days)
- Task 2.2: Description (X days)

#### Phase 3: Validation (X weeks)
- Task 3.1: Description (X days)
- Task 3.2: Description (X days)

### Dependencies
- Technical dependencies
- Resource requirements
- External dependencies

### Acceptance Criteria
- [ ] Specific deliverable 1
- [ ] Specific deliverable 2
- [ ] All integration tests pass
- [ ] Documentation updated
```

### Epic Timeline Estimation
- **With Claude assistance**: Include time savings in estimates
- **Phase-based approach**: Foundation → Low-Risk → Medium-Risk → High-Risk → Validation
- **Realistic estimates**: Account for testing, documentation, review

### Subtask Creation Pattern
```bash
# 1. Create epic first
gh issue create --title "Epic: Description" --label "Epic" --body "$(cat epic_template.md)"

# 2. Create subtasks in phases
gh issue create --title "Task 1.1: Foundation task" --body "Details..." \
  --label "Team : Platform" --label "Type : Task"
gh issue create --title "Task 1.2: Foundation task" --body "Details..." \
  --label "Team : Platform" --label "Type : Task"

# 3. Link all subtasks using internal IDs (see API section below)
```

## 🎯 Best Practices

### For Issue Creation
- ✅ Check for existing duplicates
- ✅ Include clear acceptance criteria
- ✅ Add proper team and type labels
- ✅ Reference related issues/epics

### For PR Creation
- ✅ Use descriptive titles (50-70 chars)
- ✅ Include comprehensive description sections
- ✅ Link to parent epic and specific issue
- ✅ Add testing notes and breaking changes

### For Epic Management
- ✅ Break down into logical phases
- ✅ Use systematic subtask linking
- ✅ Track progress through completion
- ✅ Update descriptions as work progresses

## 🔧 Fallback Methods - Traditional GitHub API

💡 **IMPORTANT**: Use these methods only when dotCMS utilities are unavailable or don't support the required functionality (e.g., sub-issues, GitHub V2 Project modifications).

### When to Use Traditional Methods
- **dotCMS utilities not available**: Commands not installed or need updates
- **Sub-issue management**: git-issue-create doesn't currently support sub-issues
- **GitHub V2 Project modifications**: Advanced project board operations
- **API limitations**: When utilities report errors or limitations

### GitHub Sub-issues API Requirements
⚠️ **CRITICAL**: Claude must understand these patterns to properly manage epics and subtasks when utilities don't support sub-issues.

#### Internal ID vs Issue Number
```bash
# ❌ WRONG - Using issue numbers (will fail with HTTP 404)
gh api -X POST /repos/dotCMS/core/issues/32675/sub_issues --field sub_issue_id=32679

# ✅ CORRECT - Using internal IDs
EPIC_ID=$(gh api /repos/dotCMS/core/issues/32675 --jq '.id')      # Get: 3236234567
SUBTASK_ID=$(gh api /repos/dotCMS/core/issues/32679 --jq '.id')   # Get: 3236412276
gh api -X POST /repos/dotCMS/core/issues/32675/sub_issues --field sub_issue_id=$SUBTASK_ID
```

#### Bulk Subtask Creation Workflow
**Critical Pattern for Large Epics (18+ subtasks):**
```bash
# 1. Create all subtasks first (collect issue numbers)
for i in {1..18}; do
    gh issue create --title "Task $i: Description" --body "Details..." \
      --label "Team : Platform" --label "Type : Task"
done

# 2. Collect internal IDs systematically
declare -A SUBTASK_IDS
for issue_num in 32679 32680 32681 32682 32683 32684; do
    SUBTASK_IDS[$issue_num]=$(gh api /repos/dotCMS/core/issues/$issue_num --jq '.id')
done

# 3. Link all subtasks with proper error handling
for issue_num in "${!SUBTASK_IDS[@]}"; do
    sub_issue_id=${SUBTASK_IDS[$issue_num]}
    echo -n "Linking issue #$issue_num (ID: $sub_issue_id) - "
    result=$(gh api -X POST /repos/dotCMS/core/issues/32675/sub_issues \
      --field sub_issue_id=$sub_issue_id 2>&1)
    
    if echo "$result" | grep -q '"sub_issues_summary"'; then
        echo "✅ Success"
    else
        echo "❌ Failed: $(echo "$result" | grep -o '"message":"[^"]*"' | cut -d'"' -f4)"
    fi
done
```

### API Error Types and Recovery
**HTTP 404 "Not Found"**
- **Cause**: Using issue numbers instead of internal IDs
- **Solution**: Always use `gh api /repos/:owner/:repo/issues/{number} --jq '.id'`

**HTTP 422 "Issue may not contain duplicate sub-issues"**
- **Cause**: Attempting to link already linked issues
- **Solution**: Check existing links first:
```bash
gh api /repos/dotCMS/core/issues/32675/sub_issues --jq '.[] | {number: .number, id: .id}'
```

**Rate Limiting**
- **Cause**: Too many API calls in rapid succession
- **Solution**: Add delays between batch operations or use error handling with retry logic

### Epic Progress Tracking
```bash
# Check current epic progress
gh api /repos/dotCMS/core/issues/32675 --jq '.sub_issues_summary'
# Returns: {"total": 18, "completed": 2, "percent_completed": 11}

# List all sub-issues for verification
gh api /repos/dotCMS/core/issues/32675/sub_issues --jq '.[] | {number: .number, title: .title, state: .state}'
```

## 📄 Issue and PR Templates

💡 **IMPORTANT**: These exact templates are required for consistent issue/PR formatting that matches dotCMS standards.

### Issue Description Template
**Required Issue Components:**
```markdown
## Task Description
Brief description of the specific task

### Status: ⏳ IN PROGRESS | ✅ COMPLETED

### Implementation
- **PR**: #XXXXX - PR title
- **Branch**: branch-name-matching-issue
- **Changes**: Bullet points of changes

### Acceptance Criteria
- [ ] Specific requirement 1
- [ ] Specific requirement 2
- [ ] All tests pass

### Related
- Epic: #XXXXX - Epic name
- Implementation PR: #XXXXX
```

### PR Description Template
**Required PR Components:**
```markdown
## Summary
Brief description of what this PR accomplishes.

**Related to:**
- **Epic #XXXXX**: Epic name (if applicable)
- **Issue #XXXXX**: Issue name (specific task)

## Changes Made
- Bullet point of changes
- Technical details
- Files modified

## Testing
- How the changes were tested
- Test scenarios covered
- Performance considerations

## Breaking Changes
- List any breaking changes
- Migration notes if needed

🤖 Generated with [Claude Code](https://claude.ai/code)

Co-Authored-By: Claude <noreply@anthropic.com>
```

## 📊 Project Management Integration

### GitHub Projects Structure
- **Project**: "dotCMS - Product Planning V2"
- **Default Status**: "New"
- **Workflow**: New → In Progress → Review → Done

### Project Fields
- **Status**: Current workflow state
- **Team**: Owning team (Platform, CloudEng, etc.)
- **Priority**: 1-4 scale
- **Story Points**: Estimation
- **Sprint**: Current sprint assignment

### Project API Usage
```bash
# Query project items
gh api graphql -f query='
  query {
    organization(login: "dotCMS") {
      projectV2(number: 1) {
        items(first: 20) {
          nodes {
            id
            content {
              ... on Issue {
                number
                title
                url
              }
            }
          }
        }
      }
    }
  }
'
```

## 🔄 Lessons Learned from Real Epic Management

🚨 **ESSENTIAL**: This section contains proven patterns from real-world Epic #32675 implementation. These insights are critical for understanding what actually works vs. theoretical approaches.

### Real-World Epic Experience
Based on implementing **Epic #32675: Remove Repackaged Dependencies** with **18 subtasks**:

#### What Worked Well
1. **Systematic Phase-Based Approach**: Breaking epic into logical phases (Foundation, Low-Risk, Medium-Risk, High-Risk, Validation)
2. **Detailed Subtask Templates**: Each subtask included clear acceptance criteria, risk assessment, and dependencies
3. **Proper Label Management**: Consistent use of "Team : Platform" and "Type : Task" labels
4. **GitHub Sub-issues API**: Successfully linked all 18 subtasks to the parent epic
5. **Epic Progress Tracking**: Real-time progress visibility through sub_issues_summary API

#### Challenges Encountered
1. **API Internal ID Requirement**: GitHub sub-issues API requires internal IDs, not issue numbers
2. **Bulk Operations Complexity**: Linking many subtasks requires careful error handling
3. **Duplicate Link Prevention**: API prevents duplicate linking with helpful error messages
4. **Rate Limiting Considerations**: Batch operations need proper pacing

#### Key Insights
1. **Create First, Link Second**: Always create all subtasks before attempting to link them
2. **Internal ID Management**: Collect and store internal IDs systematically for batch operations
3. **Error Handling is Critical**: Always implement proper error checking for API calls
4. **Epic Progress is Automatic**: GitHub automatically updates epic progress when subtasks are completed

### Recommended Workflow for Large Epics
```bash
# 1. Create Epic
gh issue create --title "Epic: Description" --label "Epic" --body "$(cat epic_template.md)"

# 2. Create all subtasks in phases
# Phase 1
gh issue create --title "Task 1.1: Foundation task" --body "Details..." --label "Team : Platform" --label "Type : Task"
gh issue create --title "Task 1.2: Foundation task" --body "Details..." --label "Team : Platform" --label "Type : Task"

# Phase 2
gh issue create --title "Task 2.1: Low-risk task" --body "Details..." --label "Team : Platform" --label "Type : Task"
# ... continue for all phases

# 3. Collect internal IDs
EPIC_ID=$(gh api /repos/dotCMS/core/issues/32675 --jq '.id')
SUBTASK_IDS=()
for issue_num in 32679 32680 32681 32682; do
    SUBTASK_IDS+=($(gh api /repos/dotCMS/core/issues/$issue_num --jq '.id'))
done

# 4. Link all subtasks with error handling
for sub_issue_id in "${SUBTASK_IDS[@]}"; do
    echo "Linking $sub_issue_id..."
    gh api -X POST /repos/dotCMS/core/issues/32675/sub_issues --field sub_issue_id=$sub_issue_id || echo "Failed to link $sub_issue_id"
done

# 5. Verify final epic status
gh api /repos/dotCMS/core/issues/32675 --jq '.sub_issues_summary'
```

## 🚀 Advanced Troubleshooting

🚨 **ESSENTIAL**: These error handling patterns are required for successful bulk operations. Without them, Epic subtask creation fails.

### Bulk Subtask Creation Error Handling
**Problem**: Creating many subtasks efficiently while handling API limitations
**Solution**: Use systematic approach with proper error handling

```bash
# 1. Create all subtasks first
for i in {1..5}; do
    gh issue create --title "Task 2.$i: Description" --body "Task details" --label "Team : Platform" --label "Type : Task"
done

# 2. Collect internal IDs systematically
for issue_num in 32679 32680 32681 32682; do
    echo "Issue #$issue_num: $(gh api /repos/dotCMS/core/issues/$issue_num --jq '.id')"
done

# 3. Link all subtasks with error handling
for sub_issue_id in 3236412276 3236413134 3236414468; do
    echo -n "Linking issue ID: $sub_issue_id - "
    result=$(gh api -X POST /repos/dotCMS/core/issues/32675/sub_issues --field sub_issue_id=$sub_issue_id 2>&1)
    if echo "$result" | grep -q '"sub_issues_summary"'; then
        echo "✅ Success"
    else
        echo "❌ Failed: $(echo "$result" | grep -o '"message":"[^"]*"' | cut -d'"' -f4)"
    fi
done
```

### GitHub Sub-issues API Reference
```bash
# Get internal ID for any issue
gh api /repos/dotCMS/core/issues/{issue_number} --jq '.id'

# Link subtask to epic (requires internal IDs)
gh api -X POST /repos/dotCMS/core/issues/{epic_internal_id}/sub_issues --field sub_issue_id={subtask_internal_id}

# List all sub-issues for an epic
gh api /repos/dotCMS/core/issues/{epic_number}/sub_issues

# Check epic progress
gh api /repos/dotCMS/core/issues/{epic_number} --jq '.sub_issues_summary'
# Returns: {"total": 18, "completed": 2, "percent_completed": 11}
```

### Project API Error Handling
```bash
# If project updates fail, check:
1. Project exists and is accessible
2. GraphQL query syntax is correct
3. Authentication token has proper scopes
```

## 📈 Claude Automation Benefits

### Proven Time Savings
- **Issue creation**: 5 minutes → 1 minute
- **Epic planning**: 2 hours → 30 minutes
- **PR creation**: 10 minutes → 2 minutes
- **Bulk operations**: Handle 16+ subtasks in 15 minutes

### Key Capabilities
- **Pattern recognition**: Identify similar issues and solutions
- **Bulk operations**: Efficient handling of multiple issues/PRs
- **Code analysis**: Understand implementation impact scope
- **Documentation**: Auto-generate comprehensive descriptions

### Success Metrics from Epic #32675
- **18 subtasks** successfully created and linked
- **Epic progress tracking** functional (showing 18 total, 2 completed)
- **Systematic phase approach** enables clear milestone tracking
- **Claude automation** reduced epic setup time by 80%+

## 🚨 Critical Reminders

### Branch Naming (NEVER violate)
- **MUST** start with `issue-{number}-`
- **MUST** use kebab-case (hyphens, not underscores)
- **MUST** match the issue number exactly
- **MUST** be descriptive but concise

### Project Integration
- All issues automatically added to "dotCMS - Product Planning V2"
- Status updates handled through project board
- Epic progress tracked via sub_issues_summary API

### Security Considerations
- Never expose sensitive information in issue descriptions
- Use private comments for security-related discussions
- Follow responsible disclosure for vulnerability reports

## 📚 Related Documentation

For additional implementation patterns and examples, see:
- [Git Workflows](GIT_WORKFLOWS.md) - Branch management and PR workflows
- [Documentation Maintenance](../claude/DOCUMENTATION_MAINTENANCE.md) - How to update this guide
- [Workflow Patterns](../claude/WORKFLOW_PATTERNS.md) - Claude automation patterns

---

**Note**: This document contains both guidance and critical implementation details necessary for Claude to properly handle dotCMS's GitHub workflow patterns. Essential technical information (like API requirements and error handling) is included here as it's needed for proper system operation.