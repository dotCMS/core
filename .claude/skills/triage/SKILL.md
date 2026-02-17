---
name: triage
description: Triage GitHub issues using the AI triage pipeline. Fetches the next issue from the "Needs Triage" project column (or a specific issue), runs sub-agents to validate completeness, detect duplicates, and research the codebase, then presents a triage proposal for human approval before posting anything to GitHub.
---

# AI Issue Triage

Runs the dotCMS AI triage pipeline for GitHub issues.

## Usage

```
/triage                        # Process next issue from "Needs Triage" project column
/triage --issue 1234           # Triage a specific issue by number
```

## What it does

1. Fetches the issue from GitHub
2. Runs three sub-agents **in parallel**:
   - **Issue Validator** — checks if the issue has enough information to act on
   - **Duplicate Detector** — searches for existing issues, PRs, and commits
   - **Code Researcher** — finds the relevant code, traces the call chain, identifies the fix location
3. Synthesizes findings into a **Triage Report + Technical Briefing**
4. **Presents the proposal for your approval** before writing anything to GitHub
5. On approval: posts the comment, sets labels, sets issue type, updates the project board

## Requirements

- `gh` CLI authenticated with a user that has access to the dotCMS org and the triage project
- Repository variable `TRIAGE_PROJECT_NUMBER` set to the GitHub Project number
- The triage project must exist in the dotCMS org with these columns:
  - Needs Triage, Needs Info, Triaged, AI Ready, AI In Progress, In Review, Done
- Project custom fields: `Complexity` (Small/Medium/Large), `AI Confidence` (High/Medium/Low/Skip)

## Invoke the triage lead

Use the `triage-lead` agent with the arguments provided:

```
Task: triage-lead
Args: $ARGUMENTS
```
