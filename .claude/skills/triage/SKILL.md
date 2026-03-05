---
name: triage
description: Triage GitHub issues using the AI triage pipeline. Fetches the next issue from the "Needs Triage" project column (or a specific issue), creates an agent team to validate completeness, detect duplicates, and research the codebase in parallel, then presents a triage proposal for human approval before posting anything to GitHub.
---

# AI Issue Triage

You are now the triage orchestrator. Follow these steps exactly.

## Step 1: Environment check

Run these checks. Stop with an error message if any fail:
```bash
gh --version
gh auth status
gh repo view --json nameWithOwner --jq '.nameWithOwner'
```
The repo must be `dotCMS/core`. If not, stop and tell the user to run this from the repo root.

## Step 2: Parse arguments and get issue number

Arguments received: `$ARGUMENTS`

**If `--issue <number>` is present**, extract the issue number and skip to Step 3.

**If no arguments**, fetch the next issue from the queue:
```bash
gh issue list --repo dotCMS/core --search "is:issue no:assignee -label:\"Team : Falcon\",\"Team : Platform\",\"Team : Scout\",\"Team : Maintenance\",\"Team : Modernization\",\"Team : Enablement\",\"Flakey Test\" no:parent-issue -linked:pr state:open type:Task,Bug sort:updated-asc" --json title,number,url --limit 1
```

Extract the `number` from the first result. If the array is empty, stop and tell the user: "No issues need triage — queue is empty."

## Step 3: Fetch the issue

```bash
gh issue view <number> --repo dotCMS/core --json number,title,body,labels,assignees,url,projectItems
```

## Step 4: Pre-flight check

Skip and stop if:
- Issue has any assignee
- Issue has any label starting with `Team :`
- Issue has a parent issue

## Step 5: Create agent team and run three teammates IN PARALLEL

### 5a. Create the team

```
TeamCreate(team_name: "triage-<number>", description: "Triage pipeline for issue #<number>")
```

### 5b. Create the task list

Create four tasks using `TaskCreate`. The team-router task must be blocked by the code-researcher task.

- Task A: "Validate issue #<number> completeness" (assign to validator)
- Task B: "Detect duplicates for issue #<number>" (assign to dup-detector)
- Task C: "Research codebase for issue #<number>" (assign to researcher) — save the task ID
- Task D: "Route issue #<number> to team" (assign to router) — set `addBlockedBy: [Task C ID]`

### 5c. Spawn three teammates in a SINGLE message (parallel)

You MUST make THREE Task tool calls in a single response. Do not do any research yourself.

**Teammate 1** — `subagent_type: issue-validator`, `team_name: "triage-<number>"`, `name: "validator"`
Pass the full issue content. Tell it to mark Task A as in_progress, complete it when done, and send results back to you via SendMessage.

**Teammate 2** — `subagent_type: duplicate-detector`, `team_name: "triage-<number>"`, `name: "dup-detector"`
Pass issue number, title, body, and 3-5 keywords. Tell it to mark Task B as in_progress, complete it when done, and send results back to you via SendMessage.

**Teammate 3** — `subagent_type: code-researcher`, `team_name: "triage-<number>"`, `name: "researcher"`
Pass issue number, title, full body, inferred type, and repo path (current working directory). Tell it to mark Task C as in_progress, complete it when done, and send results (especially the `Relevant Files` list) back to you via SendMessage.

Wait for all three teammates to send their results via SendMessage.

### 5d. Spawn team-router after researcher completes

Once you receive the researcher's SendMessage with the `Relevant Files` list, spawn the router teammate:

**Teammate 4** — `subagent_type: team-router`, `team_name: "triage-<number>"`, `name: "router"`
Pass the `Relevant Files` list from the researcher's message directly in the prompt. Do not ask it to search for files. Tell it to mark Task D as in_progress, complete it when done, and send the suggested team back to you via SendMessage.

Wait for the router to send its result via SendMessage.

### 5e. Shut down teammates and delete team

After all four teammates have reported their results:

1. Send `SendMessage(type: "shutdown_request")` to all four teammates.
2. Wait for shutdown confirmations.
3. Call `TeamDelete()` to clean up the team.

## Step 6: Synthesize and present proposal

Combine the four results into a triage proposal. Use the `Suggested Team` from team-router, complexity from code-researcher.

The team-router always returns a team — use it as-is. Do not re-check the triage config or do your own routing.

For priority, map severity to these exact label names:
- Critical / Show Stopper → `Priority : 1 Show Stopper`
- High → `Priority : 2 High`
- Medium / Average → `Priority : 3 Average`
- Low → `Priority : 4 Low`

Show the full proposal to the user and ask: **Approve? yes / no / edit**

## Step 7: Execute on approval

On `yes`, run these steps in order:

### 7a. Post the triage comment
```bash
gh issue comment <number> --repo dotCMS/core --body "..."
```

### 7b. Add labels (use exact label names from Step 6)
```bash
gh issue edit <number> --repo dotCMS/core --add-label "Team : X" --add-label "Priority : X X X"
```

### 7c. Add issue to the AI Triage Pipeline project and set status to Triaged

First, get the project number from the repo variable:
```bash
gh variable get TRIAGE_PROJECT_NUMBER --repo dotCMS/core
```

Then get the item ID and field metadata using that number:
```bash
gh api graphql -f query='
query {
  organization(login: "dotCMS") {
    projectV2(number: <TRIAGE_PROJECT_NUMBER>) {
      id
      fields(first: 20) {
        nodes {
          ... on ProjectV2SingleSelectField {
            id
            name
            options { id name }
          }
        }
      }
      items(first: 5) { nodes { id } }
    }
  }
}'
```

Then add the issue to the project:
```bash
gh api graphql -f query='
mutation {
  addProjectV2ItemById(input: {
    projectId: "<project-id>"
    contentId: "<issue-node-id>"
  }) {
    item { id }
  }
}'
```

Get the issue node ID with:
```bash
gh issue view <number> --repo dotCMS/core --json id --jq '.id'
```

Then set the Status field to "Triaged" using the item ID and field/option IDs discovered above:
```bash
gh api graphql -f query='
mutation {
  updateProjectV2ItemFieldValue(input: {
    projectId: "<project-id>"
    itemId: "<item-id>"
    fieldId: "<status-field-id>"
    value: { singleSelectOptionId: "<triaged-option-id>" }
  }) {
    projectV2Item { id }
  }
}'
```
