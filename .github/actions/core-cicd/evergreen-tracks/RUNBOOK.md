# Evergreen Tracks — Operator Runbook

Procedures for the three exception operations: **taint a release**, **hold a track**, and
**hold a single environment**. Routine promotion needs no operator — `standard`/`trailing`
advance on a daily 06:00 ET cron and `latest` moves on every GA cut; see
[README.md](README.md) for that and for the manual break-glass dispatch.

Because promotion is unattended, these exception controls are how you intervene: **taint**
before a bad release can be picked up, **hold** to freeze or reverse a track, and the IaC
labels to park one environment.

Two systems are involved. Know which one you're touching:

| System | Owns | Repo |
|---|---|---|
| **Registry tags** — `latest` / `standard` / `trailing` and the `_tainted` / `_hold` markers | Which release each *track* means, fleet-wide | `dotCMS/core` → `.github/actions/core-cicd/evergreen-tracks/` |
| **Environment manifests** — the `dotcms.cloud/evergreen-track` label and the pinned `image:` digest | Which release each *environment* runs | `dotCMS/infrastructure-as-code` → `kubernetes/customers/<customer>/<env>/statefulset.yaml` |

> **Key fact:** no customer manifest ever references a floating tag. Every manifest pins an
> immutable `<version>@sha256:<digest>`. Moving a registry tag therefore changes **nothing**
> in any environment until the evergreen-tracks-reconciler (or a human PR) rewrites that pin.
> Registry-side operations 1 and 2 shape *future* rolls; they do not restart pods.

---

## 1. Taint a release

**Use when** a GA release is known-bad and no track should land on it.

Tainting adds a `<version>_tainted` marker tag. The promote planner excludes tainted versions
from eligibility, so no track will advance onto it.

### Steps

1. Run the **`evergreen-tracks-admin`** GitHub Action (Actions → evergreen-tracks-admin → Run workflow):
   | Input | Value |
   |---|---|
   | `repo` | `dotcms/dotcms` |
   | `action` | `taint` |
   | `version` | the bad GA version, e.g. `26.07.06-3` |
   | `apply` | `false` ← **dry-run first** |
2. Read the output, confirm it targets the version you intended.
3. Re-run with `apply` = `true`.

### Verify

```bash
uv run evergreen-tracks promote --repo dotcms/dotcms --tracks standard,trailing
```
The tainted version must not appear as any track's target.

### ⚠️ Taint does not roll anything back

Taint only blocks **future** promotion. If a track *already* points at the version you just
tainted, it stays there — the planner is forward-only, so it will not walk a track backward
onto an older good release. To move a track off a bad release you must **hold it onto a
known-good version** (procedure 2), which repoints the track tag explicitly.

### Undo

Same workflow, `action` = `untaint`, same `version`. (Untaint deletes the marker via the
Docker Hub API; the workflow supplies the credentials.)

---

## 2. Hold a track

**Use when** you need to freeze `standard` / `trailing` / `latest` on a specific version —
during an incident, or to pull a track off a bad release.

Hold does two things: it writes a `<track>_hold` marker **and immediately points the track tag
at that version**. Unlike promotion, hold is **not** forward-only — this is the escape hatch
that can move a track *backward* onto a known-good release. While the marker exists, promote
skips the track and self-heals the floating tag back to the held digest if it drifts.

### Steps

1. Run **`evergreen-tracks-admin`**:
   | Input | Value |
   |---|---|
   | `repo` | `dotcms/dotcms` |
   | `action` | `hold` |
   | `track` | `standard`, `trailing`, or `latest` |
   | `version` | the version to freeze on, e.g. `26.06.22-03` |
   | `apply` | `false` → then `true` |
2. Holding onto a **tainted** version is refused unless you also set `force` = `true`. Don't,
   unless you're deliberately choosing the least-bad option and have said so in the ticket.

### Verify

```bash
uv run evergreen-tracks promote --repo dotcms/dotcms --tracks standard,trailing
```
Expect `<track>: held at <track>_hold, skipping promotion` and no move for that track.

### Undo

Same workflow, `action` = `release-hold`, same `track`. This deletes only the marker — the
track tag stays where it is, and the next promote run advances it normally.

---

## 3. Hold a single environment

**Use when** one customer environment must stop taking updates while the rest of its track
keeps moving. This is an **infrastructure-as-code** change, not a registry change.

Edit that environment's `kubernetes/customers/<customer>/<env>/statefulset.yaml`.

### Option A — pause it (preferred; keeps the hold visible)

Keep the track label, add the pause label:

```yaml
  labels:
    dotcms.cloud/evergreen-track: standard
    dotcms.cloud/maintenance-pause: 'true'
```

The reconciler logs `SKIP <tenant>/<env>: maintenance-paused`, so the hold shows up in every
run report. Use this for temporary holds.

### Option B — unsubscribe it (silent; for permanent opt-out)

Delete the `dotcms.cloud/evergreen-track` line entirely. Tracks are strictly opt-in — an
unlabelled env is skipped with no report line. Use this only for a deliberate permanent
opt-out (e.g. an LTS env), since nothing surfaces it later.

### Choosing the version it sits on

The env stays on whatever its `image:` line pins. To also change that version, edit the one
image line — digest is mandatory:

```yaml
        image: mirror.gcr.io/dotcms/dotcms:26.07.06-3@sha256:962acedc475b589d6bf816230fd52c3072e815939e041fa2e917b65bc1e7ed99
```

Take the digest from **Docker Hub**, not `mirror.gcr.io` (the mirror lags).

> **Editing the image line alone is NOT a hold.** With a track label still present, the next
> reconciler run rewrites that pin forward again. You must do **A or B** to actually freeze it.

### Ship it

1. Open a PR touching only that env's `statefulset.yaml`.
2. `master` requires **1 CODEOWNER approval** (`@dotcms/platform-engineers` /
   `@dotcms/CloudEng-Support`) plus the manifest/label checks. Merge.
3. Argo CD auto-syncs on its **poll** (no webhook — allow a few minutes). No manual sync needed.
4. **Pods:** a label-only change (A/B) edits `metadata.labels`, not the pod template, so it does
   **not** restart pods. Only an `image:` change rolls them.

### Verify

Labels as committed:
```bash
grep -H -e evergreen-track -e maintenance-pause \
  kubernetes/customers/<customer>/<env>/statefulset.yaml
```

What's actually running (`imageID` is the truth — the `ver` / `dotcms.cloud/dotcms-version`
labels in manifests are stale decoration):
```bash
kubectl -n <customer> get pods -l fullname=dotcms-<customer>-<env> \
  -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.status.containerStatuses[*].imageID}{"\n"}{end}'
```
Plus the Argo CD UI (Application = the **customer**, not the env) for Synced / Healthy.

---

## Gotchas

- **Never hand-patch a live env** (`kubectl set image`). Argo runs `automated: {}` **without
  `selfHeal`**, so a manual patch is not reverted until the next git change — it silently
  diverges from git. Always edit the manifest.
- **Argo Applications are per customer** with `recurse: true`, so a sync covers all of that
  customer's envs; only the changed manifest produces a diff.
- **One dotcms image line per manifest.** The reconciler errors if a manifest matches more or
  fewer than exactly one `mirror.gcr.io/dotcms/dotcms:` line.
- **Envs with `replicas < 2` are skipped** by the reconciler's eligibility gates (as are envs
  without Redis session sharing enabled) — they will not roll even when subscribed.
- **Registry and env state are separate.** A track hold (2) does not pause an environment, and
  an env pause (3) does not stop the track from moving for everyone else.
- **Dry-run is the default** on both `evergreen-tracks-promote` and `evergreen-tracks-admin`.
  Use it every time; the plan output is cheap and the mistakes are not.
- **Update the CX "Maintenance Pause List & Evergreen Tracks" sheet** when you pause or
  unsubscribe an env. Sheet → label sync is not automated, so git-only changes drift from the
  source of truth CX reads.

---

## Quick reference

| I need to… | Where | Action |
|---|---|---|
| Advance standard/trailing fleet-wide | `evergreen-tracks-promote` | automatic — daily 06:00 ET cron |
| Advance them off-cycle / review a plan first | `evergreen-tracks-promote` | dispatch → approve gate (break-glass) |
| Stop any track landing on a bad release | `evergreen-tracks-admin` | `taint` |
| Freeze a track / pull it off a bad release | `evergreen-tracks-admin` | `hold` |
| Resume a frozen track | `evergreen-tracks-admin` | `release-hold` |
| Stop one env from updating | IaC manifest PR | add `maintenance-pause: 'true'` |
| Move one env to a different track | IaC manifest PR | change `evergreen-track` value |
| Change one env's version | IaC manifest PR | edit the pinned `image:` digest |
