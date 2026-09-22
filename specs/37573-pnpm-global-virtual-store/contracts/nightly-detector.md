# Contract: The nightly global-store detector

**Location**: `.github/workflows/cicd_scheduled_pnpm-global-store.yml` (new)
**Satisfies**: FR-016, FR-019, SC-010
**Consumers**: whoever triages a red scheduled workflow; any developer who opted in

---

## Why it exists

FR-006 keeps the global virtual store out of the repository, so it is the one configuration **no
pull request ever exercises**. Without a dedicated job, the first adopter to pull a dependency bump
discovers the breakage by hand, on their own machine, with no signal telling them it is not their
fault. This workflow is the only thing standing between that developer and a lost afternoon.

---

## Triggers

| Trigger | Behaviour |
|---|---|
| `schedule` (nightly cron) | Full run |
| `workflow_dispatch` | Full run, for anyone verifying a fix |
| Pull request | **Never.** FR-019 is explicit: it is a detector, not a gate |
| Merge queue | **Never.** Same reason |

Standalone rather than a job inside `cicd_4-nightly.yml`: a developer-environment signal must not be
able to redden the product nightly, and it must be triageable on its own. It also stays clear of the
merge-queue path filtering that [ADR-0013](https://github.com/dotCMS/platform-adrs/blob/main/decisions/0013-skip-integration-and-postman-tests-for-frontend-only-changes-in-merge-queue-to-increase-flow.md)
governs, because it is not in that machinery at all.

---

## What it does

1. Check out the trunk and set up pnpm and Node from the repository's pinned versions.
2. Opt the runner in **the same way a developer does** — `pnpm config set --global virtualStoreType global`
   (see [research R-001](../research.md#r-001--where-a-per-developer-opt-in-can-actually-live)).
   Using the real mechanism rather than an approximation is the point: a job that exercised some
   other path would not be evidence about what developers run.
3. `pnpm install --frozen-lockfile`.
4. Build `dotcms-ui` for production **with the build cache skipped**. A cached run replays recorded
   output and reports success without executing anything, so a cached detector detects nothing.
5. Run the asset file-count assertion (`core-web/tools/assert-dist-assets.mjs`).
6. Report the pnpm store cache outcome, for FR-016.

---

## Guarantees

- **Detection within 24 hours** (SC-010) of any change that reintroduces an undeclared import or
  moves a corrected package past its validated version range.
- **Actionable output**: the failure names the package and the unresolved specifier, so a reader can
  act without first reproducing the global store locally.
- **Never blocks a merge.** The cost of this choice is stated and accepted in the spec: whoever broke
  it has already merged, so the finding needs triage rather than a revert-in-flight.

---

## Explicitly not guaranteed

- **That the pipeline's pnpm store cache covers the global virtual store.** The pipeline uses
  `pnpm/setup` with `cache: true`, which caches the package store keyed on the lockfile and never
  caches the installed directory. The global virtual store materialises under `store/v11/links/…`,
  inside the store path — but whether the action's cache actually covers `links/` is **unverified**
  and is a task, not an assumption. Getting it wrong costs this one job a full download each night
  and cannot affect any other job. The workflow must report the cache outcome so the answer is
  observable rather than inferred.
- **Coverage of the dev server or the test suite.** The detector builds. FR-009 and FR-010 are
  verified during implementation, not nightly — a nightly dev-server check would need a browser and
  buys little, since a resolution regression fails the build first.
- **Any statement about developers who did not opt in.** Their configuration is the default local
  store, which the ordinary pipeline already covers on every pull request.
