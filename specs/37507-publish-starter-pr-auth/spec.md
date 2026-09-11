# Issue Resolution Specification: Publish Starter PR creation fails — duplicate `Authorization` header between `actions/checkout` and `create-pull-request`

**Feature Branch**: `37507-publish-starter-pr-auth`

**Created**: 2026-09-11

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#37507](https://github.com/dotCMS/core/issues/37507)

**Input**: User description: "necesito crear la especificación del fix reportado en el issue https://github.com/dotCMS/core/issues/37507"

<!--
  This is the dotCMS ISSUE-RESOLUTION spec (used by /speckit-specify-fix). Unlike the
  feature spec, it is framed around a defect: what is wrong, how to reproduce it, and how
  we will know it is fixed. It still flows into /speckit-plan, where the Legacy Impact and
  ADR Alignment gates apply. Keep this technology-light — root-cause and fix details are
  refined in the plan.
-->

## Problem Statement *(mandatory)*

The **Publish Starter** workflow cannot complete the Empty Starter publication flow. After the
starter artifact is downloaded, deployed to Artifactory, and `parent/pom.xml` is updated with the
new `starter.deploy.version`, the **Create Pull Request** step fails and the workflow ends in
failure. No pull request is opened, so the version bump produced by the run is discarded and the
publication is never merged into the codebase.

The step fails because the two GitHub Actions involved in that job disagree on how git credentials
are supplied. `actions/checkout` leaves one set of credentials configured on the repository, the
pull-request action adds a second set, and git ends up transmitting **two** `Authorization` headers
on the same request. GitHub rejects the request outright.

> **Important correction to the original issue report.** Issue #37507 attributes the failure to the
> Node.js 20 deprecation warning emitted by the pull-request action. Evidence from the failed run
> (see Reproduction) shows this attribution is incorrect: the deprecation notice is a non-fatal
> **warning**, and a second action in the very same job (`jfrog/setup-jfrog-cli@v4`) emitted the
> identical warning and completed successfully. The Node.js notice and the failure are concurrent
> but unrelated symptoms. Upgrading the pull-request action is still the correct remedy — but
> because it resolves the credential conflict, not because it changes the Node.js runtime. This
> distinction matters: a fix justified on the wrong grounds would be validated against the wrong
> signal (absence of a warning) instead of the actual outcome (a pull request being created).

**Severity / Impact**: Medium–High for release operations, zero for product runtime. Only the
Empty Starter publication path is blocked; artifact deployment to Artifactory itself still
succeeds, and no customer-facing dotCMS functionality is affected. However, the blocked step is
the one that records the published starter version in the codebase, so every Empty Starter
publication now requires a manual pull request to carry the `pom.xml` change. Affected users are
the release/platform operators who run this workflow. Occurrence is deterministic — every run that
reaches the step fails.

## Reproduction *(mandatory)*

**Environment**: `dotCMS/core` `main` at commit `44a4026`; GitHub-hosted runner
`ubuntu-24.04`; workflow `.github/workflows/cicd_manual_publish-starter.yml`, job
`deploy-artifacts`. Requires workflow inputs `type: empty` and `dry-run: false` — the failing step
is gated on both and is skipped otherwise.

**Steps to Reproduce**:

1. Trigger the **Publish Starter** workflow manually with `type: empty`, `dry-run: false`, and a
   valid `issue-number`.
2. Let `get-starter` complete and the `deploy-artifacts` job proceed through JFrog CLI setup,
   artifact download, artifact deployment, and the `pom.xml` update — all succeed.
3. Observe the **Create Pull Request** step.

**Expected Behavior**: The action pushes the auxiliary branch
`<issue-number>-update-starter-version-<version>-<run_id>` and opens (or updates) a pull request
against the base branch carrying the `starter.deploy.version` bump, with the configured title,
body, and `empty-starter` / `automated pr` labels. The workflow completes successfully and the
Slack notification announces the new starter together with the pull request link.

**Actual Behavior**: The step fails roughly one second after starting, during the action's own
"Checking the base repository state" phase — before any branch is created or pushed. Verbatim from
the job log of run #77:

```text
remote: Duplicate header: "Authorization"
fatal: unable to access 'https://github.com/dotCMS/core/': The requested URL returned error: 400
##[error]The process '/usr/bin/git' failed with exit code 128
```

The failing git invocation is `git remote prune origin`, the first network operation the action
performs. The job then ends in failure; no branch and no pull request are created.

A separate, non-fatal warning is emitted in the same job and names **two** actions, only one of
which is the subject of this issue:

```text
##[warning]Node.js 20 is deprecated. The following actions target Node.js 20 but are being
forced to run on Node.js 24: jfrog/setup-jfrog-cli@v4, peter-evans/create-pull-request@v6
```

`jfrog/setup-jfrog-cli@v4` carries the same deprecation and completed with conclusion `success` in
this very run — direct evidence that the Node.js notice does not, by itself, fail a step.

**Reproducibility**: Deterministic. The failure is a function of the action versions present in
the workflow, not of timing, network conditions, or credential validity.

**On the "it might be transient" hypothesis** — explicitly checked and ruled out:

- Run **#76** completed successfully **five minutes before** the failure, on the **identical
  commit**. It is not counter-evidence: its `Checkout repository`, `Update pom.xml`, and
  `Create Pull Request` steps all show conclusion **`skipped`**. That run did not exercise the
  failing path at all, because it was not an `empty` + non-`dry-run` publication.
- The same is true of runs **#72 through #75** (2026-07-30 → 2026-07-31): every one of them
  skipped both `Update pom.xml` and `Create Pull Request`. There is **no run in recent history
  that successfully executed the pull-request step**. The verified consequence is that this path
  carries no regression coverage — it is exercised only on real Empty Starter publications, which
  are infrequent, so the defect sat latent from the moment it was introduced until the first run
  that happened to reach the step.
- The trigger is datable: `actions/checkout` in this workflow was bumped to **v7.0.1** on
  **2026-08-28** (commit `31102249cc`, PR #36850, a repo-wide action-bump effort). Every
  successful run predates that bump; run #77 is the first to reach the step after it.

## Scope of Investigation *(mandatory)*

- **Affected area**: CI/CD release automation — specifically the Empty Starter publication flow.
  No dotCMS application code, API, database, or index is involved.
- **Suspected surface**: Repository GitHub Actions configuration only. Neither modern
  (`com.dotcms.*`) nor legacy (`com.dotmarketing.*`) Java surfaces are touched, so the plan's
  Legacy Impact gate is expected to be a no-op for this fix. The single file in scope is
  `.github/workflows/cicd_manual_publish-starter.yml`; `peter-evans/create-pull-request` is used
  **exactly once** in the entire repository (verified across `.github/`), so the blast radius of
  changing its version is confined to this one workflow.
- **Findings from inspecting the workflow** (beyond what the issue reports):
  - The `Create Pull Request` step is correctly gated and was reached only because run #77 was a
    genuine `empty` + non-`dry-run` publication. The gating itself is not defective.
  - **Adjacent defect, independently verified**: the `send-notification` job composes its Slack
    message from `needs.update-pom.outputs.pull-request-url`. There is no job named `update-pom` —
    `update-pom` is a *step id* inside the `deploy-artifacts` job, and `send-notification` declares
    `needs: [deploy-artifacts]` only. That expression therefore resolves to an empty string on
    every run, and the `deploy-artifacts` job outputs do not expose the pull request URL either.
    Consequence: even once the pull request is created successfully, the Slack alert that is
    supposed to prompt operators for approval will show an empty link. This defect is independent
    of the version upgrade and predates it. Scope decision below.
  - The repository's prevailing supply-chain convention is to pin third-party actions to a full
    commit SHA (116 SHA-pinned usages versus 49 tag-pinned across `.github/workflows/`, with a
    dedicated commit `3881aba812`, "pin every bumped action to a commit SHA"). The replacement
    version must follow that convention; the current `@v6` is one of the tag-pinned stragglers.
    There is no `.github/dependabot.yml`, so nothing will keep this action current automatically.
- **Related known decisions**: No ADR is known to govern GitHub Actions version policy. The plan
  phase consults `dotCMS/platform-adrs` per the standard `before_plan` hook; if an ADR covering
  action pinning or supply-chain policy exists, the chosen pinning form must comply with it.

## Root-Cause Hypothesis

Two actions in the same job configure git credentials by different, mutually incompatible
mechanisms, and the second does not detect the first.

1. `actions/checkout@v7.0.1` checks the repository out and, with its default
   `persist-credentials: true`, leaves credentials behind for subsequent steps. Newer checkout
   versions no longer do this solely via `http.<url>.extraheader`; they write a separate git
   credentials config file and wire it in through `includeIf.gitdir` entries. The run's own
   post-job cleanup confirms this shape: it removes `includeif.gitdir:` entries pointing at
   `/home/runner/work/_temp/git-credentials-<uuid>.config`.
2. `create-pull-request@v6` then probes for persisted credentials by looking **only** for a local
   `http.https://github.com/.extraheader` matching `^AUTHORIZATION:`. Because checkout v7 supplies
   them through the `includeIf` file instead, that probe finds nothing, and the action concludes it
   must inject its own — writing `http.https://github.com/.extraheader AUTHORIZATION: basic ***`
   using `secrets.CI_MACHINE_TOKEN`.
3. Both credential sources are now live simultaneously. The next git network call emits two
   `Authorization` headers, and GitHub answers `400 Duplicate header: "Authorization"`, which git
   surfaces as exit code 128.

This is a known upstream incompatibility, not a dotCMS-specific misconfiguration: upstream issues
[#4228](https://github.com/peter-evans/create-pull-request/issues/4228) ("Incompatible with
actions/checkout@v6") and
[#4272](https://github.com/peter-evans/create-pull-request/issues/4272) (the duplicate-header error
verbatim) describe the same failure. The action's maintainer states the minimum compatible release
is **v7.0.9**, and a reporter independently confirms the current **v8** line works. The bump of
`actions/checkout` to v7.0.1 on 2026-08-28 is what moved this workflow onto the incompatible side
of that boundary, which is consistent with the timeline in Reproduction.

Corollaries that follow from this being the cause rather than the Node.js runtime:

- `ACTIONS_ALLOW_USE_UNSECURE_NODE_VERSION=true` would **not** have fixed the failure. Forcing the
  action back onto Node 20 leaves the credential conflict untouched. The issue is right to reject
  that escape hatch, but for a different reason than it states.
- The credentials in `CI_MACHINE_TOKEN` are not implicated. The request never got far enough to be
  evaluated; it was rejected on header shape.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- Replace `peter-evans/create-pull-request@v6` in
  `.github/workflows/cicd_manual_publish-starter.yml` with a release that is compatible with
  `actions/checkout` v6+ — at minimum v7.0.9, with the current v8 line preferred. Selecting the v8
  line additionally clears the Node.js 20 deprecation notice for this action, making it the choice
  that resolves both the failure and the reported symptom.
- Pin the replacement to its full commit SHA with a trailing version comment, matching the
  repository's prevailing convention for third-party actions.
- Verify that the six action inputs this workflow actually uses (`token`, `branch`,
  `commit-message`, `title`, `body`, `labels`) behave unchanged on the selected release, and that
  the step's `if:` gating and the `pr_created` job output continue to work.
- End-to-end validation of a real Empty Starter publication run reaching and passing the
  pull-request step, since no automated coverage exists for this path.
- Repair the broken pull request link in the Slack notification (see Scope of Investigation): the
  `deploy-artifacts` job must expose the created pull request's URL as a job output, and
  `send-notification` must read it from that output instead of the non-existent
  `needs.update-pom.*`. Deliberately included rather than split out: it is a defect in the same
  end-to-end flow, it is the only channel through which an operator learns the pull request exists
  and needs approval, and without it the primary fix cannot be observed from where operators
  actually watch this workflow.

**Explicitly out of scope / non-goals**:

- The Node.js 20 deprecation carried by `jfrog/setup-jfrog-cli@v4`, named in the same warning.
  It is non-fatal, that step completes successfully today, and it belongs to an unrelated action
  with its own upgrade considerations. Tracked separately; not a prerequisite for unblocking
  starter publication.
- A repository-wide audit or upgrade of remaining tag-pinned actions. `create-pull-request` is
  pinned to a SHA here as part of this fix, but the other ~49 tag-pinned usages are a distinct
  supply-chain effort.
- Introducing Dependabot or any other automated action-update mechanism.
- Restructuring the Publish Starter workflow — its job layout, gating conditions, dry-run
  semantics, Artifactory deployment, or Slack notification format.
- Adding automated regression coverage for the publication path. The absence of such coverage is
  documented above as a contributing factor, but building it is materially larger than this fix
  and warrants its own issue.

## Regression Risk *(mandatory)*

- **Blast radius**: Confined to one workflow file.
  `peter-evans/create-pull-request` appears exactly once in the repository, and the Publish Starter
  workflow is `workflow_dispatch`-only — it cannot be triggered by a push, pull request, or
  schedule, so a regression cannot affect ordinary CI, the merge queue, or releases. `get-starter`
  and the Artifactory deployment are untouched. The Slack repair extends the change to the
  `deploy-artifacts` job outputs and the `send-notification` message composition; the risk it adds
  is that a mistake there degrades or breaks the notification, which is cosmetic relative to the
  publication itself and is covered by AC-008 and AC-009.
- **Input compatibility across the major-version gap** — assessed against upstream release notes,
  which narrows the risk the issue flags as an unquantified trade-off:
  - **v7.0.0** introduced three behaviour changes: the `git-token` input was renamed
    `branch-token`; the deprecated `PULL_REQUEST_NUMBER` output environment variable was removed;
    and rate-limited requests are now retried. **None apply** — this workflow uses neither
    `git-token` nor `PULL_REQUEST_NUMBER`.
  - **v8.0.0** is a runtime bump only (Node 24). Its sole compatibility requirement is Actions
    Runner v2.327.1 or later on **self-hosted** runners; this job runs on a GitHub-hosted
    `ubuntu-24.04` runner, so the requirement is satisfied by definition.
  - Conclusion: the six inputs in use are unchanged across v6 → v8. Residual risk is low, but must
    still be confirmed by an actual run rather than assumed, because no run in recent history has
    exercised this step.
- **Backward compatibility**: No content, API, serialized state, database schema, or index mapping
  is touched. Nothing here falls under the rollback-unsafe categories. Rollback is a one-line
  revert of the action reference, which restores exactly today's (broken) behaviour — the revert
  is safe, it simply is not useful.
- **Behaviour to preserve explicitly**: the existing branch naming scheme
  (`<issue-number>-update-starter-version-<version>-<run_id>`, unique per run via `run_id`); the
  commit message, title, body, and both labels; the `if:` gating that keeps the step skipped for
  non-`empty` types and dry runs; and the `pr_created` job output derived from the step's outcome.
- **Data considerations**: None. No existing data requires migration or repair. Any auxiliary
  branches left behind by previously failed runs should be checked for and cleaned up, but the
  failure mode here aborts before branch creation, so none are expected.

## Acceptance & Verification *(mandatory)*

- **AC-001**: A manual **Publish Starter** run with `type: empty` and `dry-run: false` completes
  the `Create Pull Request` step successfully. The reproduction no longer yields
  `Duplicate header: "Authorization"` / `git exit code 128`.
- **AC-002**: That run opens a pull request against the intended base branch carrying the
  `starter.deploy.version` bump in `parent/pom.xml`, with the expected branch name, title, body,
  and both `empty-starter` and `automated pr` labels.
- **AC-003**: Re-running the workflow when the auxiliary branch already exists updates the existing
  pull request instead of failing. (Note: the branch name embeds `github.run_id`, so in practice
  each run targets a fresh branch — this criterion verifies the action's update path does not error
  if the collision does occur.)
- **AC-004**: The `deploy-artifacts` job completes successfully end to end, and the earlier steps —
  JFrog CLI setup, artifact download, Artifactory deployment, and the `pom.xml` update — behave
  exactly as before the change.
- **AC-005**: A run that is gated off (`type` other than `empty`, or `dry-run: true`) still skips
  the `Update pom.xml` and `Create Pull Request` steps and completes successfully, creating no
  branch, no commit, and no pull request.
- **AC-006**: The Node.js 20 deprecation warning no longer names `peter-evans/create-pull-request`.
  The warning may still name `jfrog/setup-jfrog-cli@v4`; that is expected and out of scope.
  **This criterion is corroborating evidence, not the definition of success** — AC-001 is.
- **AC-007**: The action is pinned to a full commit SHA with a trailing version comment, consistent
  with the repository's other SHA-pinned third-party actions.
- **AC-008**: The Slack notification for an Empty Starter publication contains a working link to
  the created pull request instead of an empty value, sourced from a `deploy-artifacts` job output
  rather than a reference to a non-existent job.
- **AC-009**: A gated-off run (per AC-005) produces a Slack notification that degrades cleanly —
  no broken or placeholder pull request link, since no pull request was created.

- **Verification method**: No unit, integration, or Postman test can cover this — the defect lives
  in workflow configuration and only manifests against GitHub's real git endpoint with real
  credentials. Verification is therefore an observed workflow run:
  1. Run the modified workflow from the fix branch with `type: empty`, `dry-run: false` against a
     non-production starter source, and confirm AC-001 through AC-004 and AC-006 from the job log
     and the resulting pull request.
  2. Run once more with `dry-run: true` to confirm AC-005.
  3. Attach both run URLs to issue #37507 and to the implementation pull request, and record in the
     pull request description that the root cause was the duplicate `Authorization` header rather
     than the Node.js runtime, so the correction to the original diagnosis is not lost.

  [NEEDS CLARIFICATION: validating AC-001 requires `dry-run: false`, which performs a real
  Artifactory upload and opens a real pull request. Is there an accepted non-production route for
  this (a disposable Artifactory target, a fork, or a throwaway `issue-number`), or is the accepted
  validation a real Empty Starter publication whose pull request is then closed unmerged?]

## Assumptions

- The Empty Starter publication flow is expected to keep working as designed; the goal is to
  restore it, not to redesign it.
- `secrets.CI_MACHINE_TOKEN` is valid and carries the permissions needed to push a branch and open
  a pull request on `dotCMS/core`. The failed run provides no evidence either way, since the
  request was rejected before the token was evaluated. If the fix removes the duplicate-header
  error and a permissions error surfaces underneath it, that is a **second, previously masked**
  defect and should be handled as such rather than folded into this one silently.
- `actions/checkout` stays at v7.0.1 or newer. Downgrading checkout is an alternative way to clear
  the conflict, but it would contradict the deliberate repo-wide bump in PR #36850 and is therefore
  not treated as a candidate remedy.
- The v8 line is the intended target, on the grounds that it satisfies the compatibility minimum
  and clears the deprecation notice in one move. Should the plan phase uncover a blocking reason to
  stay on the v7 line, v7.0.9 or newer remains acceptable for AC-001 through AC-005, with AC-006
  then unmet for this action.
- The base branch for the generated pull request is the branch the workflow runs from (`main` in
  the failed run), consistent with the action's default behaviour, since the workflow sets no
  explicit `base` input.
