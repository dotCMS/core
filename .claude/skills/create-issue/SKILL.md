---
name: create-issue
description: Create GitHub issues using repository templates. Use when the user asks to create an issue, bug report, feature request, task, spike, epic, or UX requirement. Also use when the user describes a problem, bug, enhancement, or work item that should be tracked. Supports both English and Spanish input.
---

# Create GitHub Issue

Acts as an experienced Product Owner to create well-structured GitHub issues using the repository's predefined templates.

## Core Workflow

Follow these steps in order:

### 1. Discover Available Templates

```bash
ls .github/ISSUE_TEMPLATE/*.yaml .github/ISSUE_TEMPLATE/*.yml
```

### 2. Read Templates Dynamically

**CRITICAL**: ALWAYS read the actual template files before creating an issue. Never use cached or assumed template structures.

```bash
# Read each template to understand:
# - Template name and description
# - Required and optional fields
# - Default labels assigned by template
# - Field validation requirements
```

Use the Read tool to load templates from `.github/ISSUE_TEMPLATE/`.

### 3. Analyze User Description

Based on the user's description (in English or Spanish), classify the issue:

- **Defect** (defect.yaml): Bugs, errors, things not working as expected
- **Feature** (feature.yaml): New functionality, enhancements within an EPIC
- **Task** (task.yaml): Technical tasks, maintenance, refactoring, library updates
- **Spike** (spike.yaml): Timeboxed research, investigation, unknowns to explore
- **Epic** (epic.yml): Large initiatives, wide areas of functionality
- **UX** (ux.yaml): User experience improvements, interface issues, usability problems

**Note**: Epic and Feature templates are marked "Product team use only", while Defect, Task, and Spike are "Engineering team use". Consider this when selecting templates.

### 4. Generate English Title

Create a concise, descriptive title in English. If the user provided Spanish input, translate the title.

### 5. Select Feature Label

Analyze the description content and select the most appropriate `dotCMS : [Feature]` label. See [references/feature-labels.md](references/feature-labels.md) for complete label list and selection logic.

**Important Distinctions**:
- **Edit Content/Contentlet Editor** issues → Use `dotCMS : New Edit Contentlet` (specific)
- **General content operations** (creation, types, management) → Use `dotCMS : Content Management` (general)

If no clear match, default to `dotCMS : Content Management`.

### 6. Determine Technology

Based on the description, set Technology field:

- **Front-end**: UI/UX work, Angular components, admin interface, user-facing features
- **Java**: Backend services, APIs, core functionality, server-side logic
- **Platform**: Infrastructure, DevOps, system-wide changes
- **Go-To-Market**: Marketing, sales, business features

Most common for engineering: **Front-end** or **Java**.

### 6.5. Ask for Team Assignment

**CRITICAL**: Always ask the user which team should be assigned to this issue using the AskUserQuestion tool.

Available teams:
- **Falcon** 
- **Maintenance**
- **Scout**
- **Platform**


Use the AskUserQuestion tool with these exact options:
```json
{
  "questions": [{
    "question": "Which team should be assigned to this issue?",
    "header": "Team",
    "multiSelect": false,
    "options": [
      {"label": "Falcon (Recommended)"},
      {"label": "Maintenance"},
      {"label": "Scout"},
      {"label": "Platform"}
    ]
  }]
}

Extract the team name from the user's response and use it in step 8.

### 7. Build Issue Body

Based on the template structure read in step 2, construct the issue body in markdown format. Each template has specific fields - build the body to match that structure.

**Common Template Structures**:

**Task Template** (task.yaml):
```markdown
## Description
[Clear explanation of what needs to be done]

## Acceptance Criteria
- [ ] Criterion 1
- [ ] Criterion 2
- [ ] Criterion 3

## Priority
[High/Medium/Low]

## Additional Context
[Any supporting information]
```

**Defect Template** (defect.yaml):
```markdown
## Problem Description
[What's broken or not working]

## Steps to Reproduce
1. Step 1
2. Step 2
3. Step 3

## Expected Behavior
[What should happen]

## Actual Behavior
[What actually happens]

## Severity
[Critical/High/Medium/Low]

## Additional Context
[Screenshots, logs, environment details]
```

**Spike Template** (spike.yaml):
```markdown
## Research Goal
[What needs to be investigated]

## Questions to Answer
- [ ] Question 1
- [ ] Question 2

## Time Box
[Duration, e.g., "2-3 days"]

## Success Criteria
[How we know the spike is complete]
```

### 8. Create Issue with GitHub CLI

**CRITICAL**: Do NOT use `--template` flag. It cannot be combined with `--title` and `--body` in non-interactive mode.

```bash
gh issue create \
  --repo "dotCMS/core" \
  --title "[Generated English Title]" \
  --body "[Markdown body constructed in step 7]" \
  --label "[Template Labels],[Additional Labels]"
```

**Template Labels**: Use labels from the template file you read

**Additional Labels to Apply**:
- **Team Assignment**: Add `Team : [Selected Team]` using the team selected in step 6.5
- **Feature Label**: Add selected `dotCMS : [Feature]` label

**Label Format Examples** (replace `[Team]` with selected team: Falcon, Maintenance, or Scout):
- Defect: `--label "Triage,OKR : Customer Support,Team : [Team],dotCMS : [Feature]"`
- Feature: `--label "Team : [Team],dotCMS : [Feature]"`
- Task: `--label "Triage,Team : [Team],dotCMS : [Feature]"`
- Spike: `--label "Team : [Team],dotCMS : [Feature]"`
- Epic: `--label "Team : [Team],dotCMS : [Feature]"`
- UX: `--label "Team : UX,Team : [Team],dotCMS : [Feature]"`

### 9. Fill Content Fields

**General Guidelines**:
- All content should be in English (translate from Spanish if needed)
- Be concise and useful
- Focus on the most important aspects
- Use concrete acceptance criteria with checkboxes (format: `- [ ] Item`)
- Include relevant links when available

**Field Population Patterns**:
- **Description/Problem**: Clear, specific explanation (2-4 sentences)
- **Acceptance Criteria**: Checkbox list with measurable, testable criteria
- **Steps to Reproduce**: Numbered list for bugs
- **Priority/Severity**: Choose appropriate level based on impact
- **Additional Context**: Include only relevant supporting information
- **Links**: Add Slack threads, Freshdesk tickets, Figma designs when available

### 10. Confirm Creation

After issue creation:
1. Provide the issue URL to the user
2. Confirm the following were set:
   - Type label (based on selected template)
   - Team label applied (Falcon, Maintenance, or Scout)
   - Feature label applied
   - Technology context (Front-end/Java/Platform) mentioned in description if relevant

## Important Notes

- **Always read templates first**: Template structures may change over time - never assume field names or structure
- **No --template flag**: Do NOT use `--template` with `gh issue create` - it's incompatible with `--title` and `--body` in non-interactive mode
- **Build body manually**: Construct the markdown body based on the template structure you read
- **Translation**: Accept Spanish input but create all issues in English
- **Label automation**: Apply all labels automatically without asking for user confirmation
- **Feature label selection**: Use semantic analysis to select the most appropriate feature label
- **Project assignment**: Issues are automatically added to project `dotCMS/7` via template configuration

## Authorization

If project assignment fails, may need to refresh GitHub CLI auth:

```bash
gh auth refresh -s read:project -s project --hostname github.com
```
