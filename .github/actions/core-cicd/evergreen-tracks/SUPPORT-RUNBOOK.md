# Evergreen Tracks — Support Runbook (interim)

**Audience:** CX / support agents taking customer track-change requests, and the platform
engineer who applies them. **Interim** because track changes are a manual manifest edit until
self-service track selection ships.

> **Published copy for CX:** this file is the canonical source; the copy support actually reads is
> the Google Doc **[Evergreen Tracks — Support Runbook (interim)](https://docs.google.com/document/d/1tLEzbZOC5D5cXDq0S_bHtsKqd0-13hSUAcXfPeLlblI/edit)**
> (Engineering shared drive → `dotEvergreen`). **Land corrections here, then re-publish the Doc** —
> the Doc carries a banner saying so, but nothing enforces it.

**Related docs**

| Doc | Use it for |
|---|---|
| [RUNBOOK.md](RUNBOOK.md) — operator runbook | Exception operations: tainting a bad release, holding a track, holding a single environment. Incident work, not routine requests. |
| [README.md](README.md) | How the track tags themselves advance (the promote action, cadence, approval gate). |
| [dev.dotcms.com/docs/evergreen-tracks](https://dev.dotcms.com/docs/evergreen-tracks) | The published customer-facing explanation. **Everything you tell a customer must match this page.** |
| ["dotCMS Maintenance Pause List & Evergreen Tracks" sheet](https://docs.google.com/spreadsheets/d/1pDSXjBYuUfGLufNcrhK-zVeSLrGb-c38qpZzDM3LUfg/edit) | The CX-facing registry of who is on what track and who is paused. Updated by hand — see step 4.4. |

---

## 0. The model in one minute

A **track** controls *how fresh* a release an environment receives. All tracks install the same
released artifacts; only the timing differs.

| Track | Lands on the customer | Typical use |
|---|---|---|
| `latest` | Every GA release, immediately | Dev / early-validation envs, teams with strong CI |
| `standard` | GA releases ~14 days old — the default | Most environments |
| `trailing` | GA releases ~28 days old | Conservative / production-leaning envs |

Two facts that drive almost every support answer:

1. **Track is per environment**, not per customer. Dev on `latest` and prod on `trailing` is a
   normal, supported setup.
2. **Tracks are forward-only.** Moving an environment to a *slower* track never rolls its running
   version backward. It simply stops advancing until the slower track catches up to the version it
   already runs.

Fleet snapshot (2026-07-28): 190 subscribed environments — 180 `standard`, 9 `trailing`
(greensky, lennox, lennox-dr, suny), 1 `latest` (bcbs `dev-2310`).

---

## 1. Who does what

| Step | Owner |
|---|---|
| Intake, validation, customer comms | **CX / support** |
| The manifest change (PR to `dotCMS/infrastructure-as-code`) | **Platform / Enablement** (Steve's team) — CODEOWNER approval is required, so support cannot self-serve this |
| Verification that it applied | Platform, reported back on the ticket |
| Closing the loop with the customer | **CX / support** |

Support's job is steps 2, 3, 6 and 7. Step 4 and 5 are documented here so you know what you are
asking for, can read the PR, and can answer "is it done yet".

---

## 2. Intake — what the request looks like

Track changes arrive as a **support ticket** (Freshdesk). They also arrive informally in calls and
in Slack; if that happens, **open a ticket anyway** — the ticket is the audit trail for a change to
a customer's production update behavior.

Capture these fields before handing off. Paste this block into the ticket:

```
Track change request
--------------------
Customer / tenant:        e.g. bcbs
Environment(s):           e.g. dev-2310            (one line per env)
Current track:            latest | standard | trailing | unknown
Requested track:          latest | standard | trailing
Requester:                name + email
Requester is authorized:  yes/no — how confirmed
Reason (optional):        e.g. "want dev validating new releases first"
Urgency / window:         normal | before <date> | tied to a release
```

If the customer asks "**what track am I on?**" rather than for a change, that is answerable
without any change — see step 3.1, answer, and close.

---

## 3. Validate before handing off

### 3.1 Which environment, exactly

Environments map 1:1 to a directory in `dotCMS/infrastructure-as-code`:

```
kubernetes/customers/<tenant>/<env>/statefulset.yaml
```

e.g. `kubernetes/customers/bcbs/dev-2310/statefulset.yaml`. Customer-facing environment names
("dev", "our UAT box") are not always the directory name — confirm the tenant slug and env slug,
not the friendly name. Current track for every subscribed env:

```bash
# in a dotCMS/infrastructure-as-code checkout
grep -r "dotcms.cloud/evergreen-track" kubernetes/customers/ | sed 's|kubernetes/customers/||'
```

The CX sheet is the human-facing registry, but **git is the source of truth** — if they disagree,
git wins and the sheet needs fixing.

### 3.2 Which track

Only `latest`, `standard`, `trailing`. There is no "every release but skip X", no per-customer
schedule, and no custom age threshold. If the customer wants something else, that is a product
conversation → escalate (step 7), don't promise it.

### 3.3 Requester authority

The requester must be a **named technical contact / admin on the account**. A track change alters
when production updates land, so treat it like any other production change request: if the
requester is not a known contact, get confirmation from one before handing off.

### 3.4 Set the forward-only expectation *before* the change

Say this explicitly on the ticket, in these words:

> Moving to a faster track takes effect as soon as we make the change. Moving to a slower track
> never rolls a running version backward — your environment stays on its current version and
> simply stops advancing until that version ages into the new track, then updates resume.

If the customer's actual goal is "**go back to the version we ran last month**", that is a
**rollback**, not a track change. Do not action it as a track change → escalate (step 7).

### 3.5 Eligibility

An environment must satisfy all of these to be updated automatically. Platform will re-check, but
flagging it at intake avoids a round trip:

- `spec.replicas >= 2` (updates are rolling, zero-downtime — a single-replica env can't be)
- Redis session sharing enabled (`TOMCAT_REDIS_SESSION_ENABLED = "true"`)
- **not** currently maintenance-paused (`dotcms.cloud/maintenance-pause: "true"`)

If an env fails a gate, the track label can still be set, but the env will be skipped and reported
rather than rolled. Say so on the ticket instead of promising updates that won't happen.

### 3.6 Paused customers

Some environments are deliberately paused (as of 2026-07-28: Duncan Aviation, Jostens, firstmac).
**Do not un-pause an environment as part of a track-change request** — the pause exists for a
reason (e.g. a Java upgrade hold) and clearing it is a separate decision by the CX owner for that
account. Note the pause on the ticket and escalate.

---

## 4. Apply — the manifest change

> Performed by **platform / Enablement**. Support: this is what to expect to see linked on the
> ticket.

### 4.1 Edit the environment's manifest

One file: `kubernetes/customers/<tenant>/<env>/statefulset.yaml`. The track lives in the
StatefulSet's own `metadata.labels` (not the pod template):

```yaml
metadata:
  namespace: bcbs
  name: dotcms-bcbs-dev-2310
  labels:
    ...
    dotcms.cloud/evergreen-track: latest        # <- the track
```

### 4.2 Faster move vs slower move — they are different edits

**No manifest ever references a floating tag.** Every environment pins an immutable
`<version>@sha256:<digest>`:

```yaml
        image: mirror.gcr.io/dotcms/dotcms:26.07.27-01@sha256:d0e1515a70989e2deb1d59364773ef76802af9857b7ea058dffb282b8f34f19d
```

So the label alone changes *future* behavior; it does not move the running version.

| Move | Edit | Effect |
|---|---|---|
| **Faster** (e.g. `standard` → `latest`) | Change the label **and**, in the same PR, bump the pinned `image:` to the requested track's current version + digest | Rolling update now — which is what the public doc promises ("takes effect as soon as our support team makes the change") |
| **Slower** (e.g. `latest` → `trailing`) | Change the label **only**. Never edit the image line downward | No restart, no version change. The env stops advancing until `trailing` reaches its version |

Interim caveat (2026-07-28): the floating-tag reconciler that would advance pins automatically is
deployed but running **dry-run everywhere** (`PUSH_ENABLED=false`), so **no environment's version
advances on its own today** — every version change is a human PR. That is why a faster-track move
must bump the image pin in the same PR rather than waiting for automation.

### 4.3 Resolve the track's current version + digest

Take the digest from **Docker Hub**, not `mirror.gcr.io` (the mirror lags):

```bash
curl -s https://hub.docker.com/v2/repositories/dotcms/dotcms/tags/latest \
  | python3 -c 'import sys,json; print(json.load(sys.stdin)["digest"])'
```

Then find the version tag sharing that digest (the pin keeps the version human-readable):

```bash
docker buildx imagetools inspect dotcms/dotcms:latest        # or match the digest in Hub's tag list
```

Snapshot for orientation — **re-resolve, never copy these** (2026-07-28):

| Track | Version | Digest |
|---|---|---|
| `latest` | 26.07.27-01 | `sha256:52bbae5e0c99bc82a6ee4a04e0bd3c98fd86367b181db0ef17531be7d37d00ce` |
| `standard` | 26.07.06-3 | `sha256:962acedc475b589d6bf816230fd52c3072e815939e041fa2e917b65bc1e7ed99` |
| `trailing` | 26.06.22-03 | `sha256:d28362adad00db09faf941f5c0de39a7b1ba6f6dca5d1903442a1ffb1fe7e2e6` |

Note that the same version can exist as several rebuild digests (e.g. `26.07.27-01` and
`26.07.27-01_66cdc38`). Always pin the digest the *track tag* currently resolves to.

### 4.4 Ship it

1. PR against `master` touching **only** that environment's `statefulset.yaml` (one env per PR
   keeps the audit trail clean and the revert trivial). Reference the support ticket in the body.
2. `master` requires **1 CODEOWNER approval** — `CODEOWNERS` is `* @dotcms/platform-engineers
   @dotcms/CloudEng-Support` — plus the manifest/label checks. Merge.
3. Argo CD picks it up on its **poll** (no webhook) — allow a few minutes.
4. **Update the CX sheet** (*Evergreen Tracks* tab). Sheet ← git sync is not automated, so skipping
   this leaves CX reading stale data.

Never `kubectl set image` a live environment. Argo runs `automated: {}` **without `selfHeal`**, so
a hand patch is not reverted and silently diverges from git.

---

## 5. Verify

1. **Label as committed:**
   ```bash
   grep -H -e evergreen-track -e maintenance-pause \
     kubernetes/customers/<tenant>/<env>/statefulset.yaml
   ```
2. **Argo CD:** the Application is named for the **customer**, not the env (`recurse: true` covers
   all their envs). Expect **Synced / Healthy**.
3. **What is actually running** — `imageID` is the truth; the `ver` and
   `dotcms.cloud/dotcms-version` labels in manifests are stale decoration:
   ```bash
   kubectl -n <tenant> get pods -l fullname=dotcms-<tenant>-<env> \
     -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.status.containerStatuses[*].imageID}{"\n"}{end}'
   ```
4. **Environment healthy:** for a faster move (pods rolled), confirm the env is serving before
   replying — all pods Ready, no crash-looping, site responding.
5. **Reply to the customer** with: which env, new track, whether a version change happened now
   (and to what version), and when their next update will land (step 6.2).

A slower move is verified by the label alone — pods do **not** restart, so there is nothing new
running to check.

---

## 6. Customer answers (copy-paste)

### 6.1 "Does moving to a slower track roll us back?"

> No. Tracks are forward-only. Your environment keeps running its current version; it just stops
> advancing until that version ages into the new track's window, and updates resume from there.

### 6.2 "When will our next update apply?"

| Track | Answer |
|---|---|
| `latest` | On the next GA release. |
| `standard` | At our next scheduled maintenance window, landing on the newest GA release older than ~14 days. |
| `trailing` | At our next scheduled maintenance window, landing on the newest GA release older than ~28 days. |

Do **not** promise a specific date or a fixed day-of-week. `standard` and `trailing` advance only
when an operator dispatches the promote action at a maintenance window — that human step *is* the
cadence gate. If the customer needs a date, ask platform for the next window rather than guessing.

### 6.3 "Can we go back to version X?"

Not via tracks — that is a rollback. Escalate (step 7). Nothing in this runbook moves a customer
backward.

### 6.4 "Can we pause updates for a while?"

Yes — that is a maintenance pause, not a track change. It keeps the track label but stops rolls.
Escalate to platform with the reason and the expected duration, and record it on the *Maintenance
Pause List* tab.

### 6.5 "We heard release X has a problem — will we get it?"

If a release is flagged with a known issue, we taint it and no track advances onto it. If the
customer is *already* on a flagged release, that is an incident, not a track question → escalate
immediately (step 7).

### 6.6 "Can we have a custom schedule / skip a release / pick our own age threshold?"

Not supported. Three tracks, defined by version age. Anything else is a product request →
escalate, don't promise.

---

## 7. Escalation

**Path:** CX / support → **platform / Enablement**. Slack `#team-cloud-engineering`
(`#support` for the customer-facing thread), GitHub teams `@dotcms/platform-engineers` /
`@dotcms/CloudEng-Support`. Always link the support ticket.

| Situation | Where it goes |
|---|---|
| Routine track change (validated per step 3) | Platform — manifest PR (step 4) |
| Env fails an eligibility gate (single replica, no Redis sessions) | Platform — needs an env change first, not just a label |
| Environment is maintenance-paused | Account's CX owner + platform, before anything is changed |
| Customer wants an older version (rollback) | Platform — **incident/change path**, not a track change |
| Customer is on a release with a known issue | Platform, **immediately** — taint / track hold, see [RUNBOOK.md](RUNBOOK.md) |
| Need to freeze a whole track, or pull a track off a bad release | Platform only — [RUNBOOK.md](RUNBOOK.md) procedures 1 and 2 |
| Customer wants a custom cadence / self-service today | Product — self-service is Workstream B, not yet available |
| Suspected regression after an update | Normal bug/incident path, and tell platform which env and which digest it is running |

---

## 8. Known interim limitations

Be honest about these on tickets; they are the reason this runbook says "interim".

- **No self-service.** Every track change is a manifest PR by platform. Self-service selection is
  on the roadmap (Workstream B).
- **No automatic version advance yet.** The reconciler is deployed but dry-run everywhere, so
  `standard`/`trailing` envs move only when a human ships the pin. Never tell a customer their
  environment will update itself on a specific date.
- **Sheet ↔ git drift.** The CX sheet is updated by hand. Verify against git before answering
  "what track am I on".
- **Track ≠ running version.** The label says what an env *should* follow; `imageID` says what it
  runs. They can differ (a paused env, a skipped gate, a pin nobody advanced).

---

## 9. Quick reference

| I need to… | Do this |
|---|---|
| Tell a customer what track they're on | `grep -r evergreen-track kubernetes/customers/<tenant>/` — git, not the sheet |
| Move an env to a faster track | Hand to platform: label + image-pin bump, one PR |
| Move an env to a slower track | Hand to platform: label only, no image change, no restart |
| Answer "when's our next update" | Step 6.2 — no specific dates |
| Handle "put us back on the old version" | Rollback → escalate |
| Handle "pause our updates" | Maintenance pause → escalate; record on the sheet |
| Handle "release X is broken" | Escalate immediately → operator [RUNBOOK.md](RUNBOOK.md) |

Briefing material for the CX walkthrough of this runbook: [CX-BRIEFING.md](CX-BRIEFING.md).
