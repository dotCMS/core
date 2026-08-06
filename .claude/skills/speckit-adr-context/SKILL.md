---
name: "speckit-adr-context"
description: "Consult dotCMS/platform-adrs for Architecture Decision Records relevant to the current feature/fix, so planning treats existing decisions as binding input. Runs as a before_plan hook."
argument-hint: "Optional keywords (subsystem, tech, data store). Defaults to inferring from the spec."
compatibility: "Requires an authenticated gh CLI with access to dotCMS/platform-adrs"
metadata:
  author: "dotcms"
  source: "dotcms customization (see .specify/CUSTOMIZATIONS.md)"
user-invocable: true
disable-model-invocation: false
---

## User Input

```text
$ARGUMENTS
```

## Purpose

ADRs are binding architectural context for dotCMS and live **only** in the private repo
`dotCMS/platform-adrs`. This skill surfaces the ADRs relevant to the work being planned so
they are used as **input** to the plan. It is invoked automatically as a `before_plan` hook
(see `.specify/extensions.yml`) and can also be run manually.

**This skill is read-only. It NEVER creates, edits, or commits an ADR.** ADRs are authored
only in `dotCMS/platform-adrs` via its own `new-adr.sh` process. Spec-Kit may only *propose*
an ADR (recorded in the plan's "Proposed ADRs" section).

## Steps

1. **Derive keywords.** Use `$ARGUMENTS` if provided. Otherwise read the current feature spec
   (from `.specify/feature.json` → `feature_directory` → `spec.md`) and extract keywords:
   affected subsystem, technologies, data stores (e.g. `search`, `elasticsearch`, `workflow`,
   `content-drive`, `rest`, `permissions`), and whether it touches legacy `com.dotmarketing.*`.

2. **Run the lookup** (read-only; always exits 0):

   ```bash
   .specify/scripts/bash/adr-context.sh <keyword> <keyword> ...
   ```

   If it reports a network/permission problem, tell the user to check `gh auth status` and
   review `dotCMS/platform-adrs/INDEX.md` manually — then continue (do not block planning).

3. **For each promising match**, optionally read the ADR body for detail:

   ```bash
   gh api repos/dotCMS/platform-adrs/contents/decisions/<file>.md -q .content | base64 -d
   ```

   Pay attention to **status**: treat `accepted` ADRs as binding; note `proposed` ones as
   directional.

4. **Summarize for the plan.** Emit a short list the `/speckit-plan` step will fold into the
   plan's **ADR Alignment (Gate)** section:
   - Relevant ADRs (id, title, status, link, one-line relevance).
   - Any likely conflict with an accepted ADR (must be resolved in the plan).
   - Candidate *proposals* if the work implies a new decision — as proposals only.

5. **Reminder to the planner**: fill the plan's ADR Alignment section from this output.
   Do **not** create any ADR. New decisions are proposed, then authored separately in
   `dotCMS/platform-adrs`.
