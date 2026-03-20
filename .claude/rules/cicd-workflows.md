---
paths:
  - ".github/workflows/*.yml"
  - ".github/actions/**/*"
---

# CI/CD Workflow Patterns

## Structure

Workflows follow a naming convention with two tiers:

- **Orchestrators** (`cicd_1-pr.yml` through `cicd_8-*.yml`) -- top-level workflows triggered by events (PR, merge queue, nightly, release). Each calls reusable component workflows.
- **Components** (`cicd_comp_*.yml`) -- reusable workflows (`workflow_call`) for individual phases: build, test, deploy, finalize. Called by orchestrators, never triggered directly.
- **Other** (`ai_*.yml`) -- non-CI workflows (e.g., AI agent orchestration).

## Security constraint (CRITICAL)

PR check workflows (`cicd_1-pr.yml`) run on **unmerged code** and must NOT use secrets. Any notification or action requiring secrets belongs in a separate post-workflow (triggered after the PR workflow completes, runs on the base branch).

## When modifying workflows

- Check if the change belongs in an orchestrator or a component. Component changes affect all orchestrators that call them.
- Test with `act` or by pushing to a branch and checking the Actions tab.
- The cicd-diagnostics skill can help debug workflow failures.

## On-demand
- See `docs/infrastructure/` for CI/CD architecture details.
