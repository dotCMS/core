# Issue Resolution Specification: Push Publishing: unreachable endpoint stalls the queue; bundle pushed now shows as Scheduled with old date

**Feature Branch**: `37449-publishing-queue-dead-endpoint`

**Created**: 2026-09-05

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [dotCMS/core#37449](https://github.com/dotCMS/core/issues/37449)

**Input**: User description: "only the 2 fixes. with limited scope. spec/requirements must be only for these 2 problems that we see in reported video. nothiung extra."

<!--
  This is the dotCMS ISSUE-RESOLUTION spec (used by /speckit-specify-fix). Unlike the
  feature spec, it is framed around a defect: what is wrong, how to reproduce it, and how
  we will know it is fixed. It still flows into /speckit-plan, where the Legacy Impact and
  ADR Alignment gates apply. Keep this technology-light — root-cause and fix details are
  refined in the plan.
-->

## Problem Statement *(mandatory)*

Two independent defects in sender-side Push Publishing. Both are visible in the Publishing
Queue portlet, in the Beta version and the legacy one alike.

**Problem 1 — One unreachable endpoint stalls every other bundle.**
When a bundle is pushed to an endpoint that does not answer connection attempts, the publisher
waits about two minutes per attempt before giving up. Bundles are processed one at a time, so
every bundle pushed afterwards waits behind it. Bundles pushed for "now" have been picked up 10
to 19 minutes late, and the publisher ran 6 times in an hour instead of once a minute. An
unexpected error in one bundle also stops the remaining bundles from being processed in that
run.

**Problem 2 — A bundle pushed for "now" is shown as Scheduled with an old date.**
While a pushed bundle waits to be picked up, the Publishing Queue shows it as **Scheduled**,
with **Date Entered** set to the day its assets were added to the draft bundle, and the detail
dialog says **Scheduled for &lt;push time&gt;**. The user did not schedule anything; they asked
for an immediate push. In a healthy system this state lasts about two seconds, so it goes
unnoticed; with Problem 1 it lasts minutes.

**Severity / Impact**: High. Every editor who push-publishes on an instance where any enabled
endpoint is unreachable. All pushes are delayed by minutes per attempt, three attempts per
bundle, with no error shown until the wait ends. Content publish and expire dates run in the
same job and are delayed too. Problem 2 turns the delay into a misleading row: the bundle looks
scheduled although nobody scheduled it, and nothing tells the user it is waiting for a stuck
publisher.

## Reproduction *(mandatory)*

**Environment**: `main` (also seen on a `1.0.0-SNAPSHOT` build of 2026-08-31). One
push-publishing environment `TEST` with a single enabled endpoint whose connection attempts get
no answer (a firewalled or non-routable address).

**Steps to Reproduce**:

1. Create draft bundle A with any content. Create draft bundle B whose assets were added
   several days earlier.
2. Publishing Queue → Bundles → Select Bundle → check A → Configure → Push, default publish
   date, environment `TEST` → Send.
3. Within a few seconds repeat step 2 for bundle B, to any environment.
4. Refresh the Publishing Queue for several minutes. Open View details on B.

**Expected Behavior**:

- A fails within seconds with a connection error. B is picked up within seconds of its publish
  time.
- While B waits it is shown as requested, its Date Entered is the push time, and no
  "Scheduled for" date is shown.

**Actual Behavior**:

- A shows **Sending** for about two minutes per attempt, three attempts, and every status check
  of A costs the same wait. Log: `java.net.ConnectException: Connection timed out`.
- B shows **Scheduled**, Date Entered equal to the day its assets were added, and "Scheduled
  for &lt;push time&gt;", until the publisher reaches it minutes later.

**Reproducibility**: Always, given an enabled endpoint whose connection attempts get no answer.
Problem 2 alone reproduces with any bundle whose assets were added earlier, by reading
`GET /api/v1/publishing` in the short window before the publisher picks it up.

## Scope of Investigation *(mandatory)*

- **Affected area**: Push Publishing, sender side: the publisher queue job that sends bundles
  and checks their status, and the v1 Publishing REST API that the Publishing Queue portlet
  reads.
- **Suspected surface**: Modern `com.dotcms.*`: the publisher packages
  (`com.dotcms.publisher.*`) and the v1 publishing REST package
  (`com.dotcms.rest.api.v1.publishing`). No `com.dotmarketing.*` code is believed to be
  involved. The plan confirms.
- **Related known decisions**: `SCHEDULED` is a read-time status synthesized by the v1 API for
  bundles that have not yet been picked up (introduced with issue #36267); it is never stored.
  This fix narrows when it is reported. The plan consults `dotCMS/platform-adrs`.

## Root-Cause Hypothesis

- **Problem 1**: the HTTP client the publisher uses to upload bundles and to poll their status
  has no connect timeout, so an unanswered connection attempt lasts until the operating system
  gives up. The publisher job runs as a single execution at a time and handles bundles
  sequentially, so that wait blocks everything queued behind it. Per bundle, only the
  publishing-specific exception is handled; any other error ends the run.
- **Problem 2**: the v1 API treats every queued, not-yet-picked-up bundle as `SCHEDULED`
  without checking whether its publish date is in the future or already due, and reports the
  date the assets entered the queue table as the bundle's `createDate`. A push updates the
  publish date but not that entered date.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- **F1 — Bounded connection attempts.** The publisher's HTTP client, for both the bundle upload
  and the status poll, gets a connect timeout, configurable, default about 10 seconds. Only
  the wait for the connection to be established is bounded. A timed-out attempt is treated
  exactly like a refused connection is today.
- **F2 — One failing bundle does not stop the run.** Any error while processing one bundle
  marks that bundle failed with the error message and removes it from the queue, in one
  transaction, and the publisher continues with the next bundle.
- **F3 — Requested, not Scheduled.** A queued bundle whose publish date is already due is
  reported by the v1 API as `BUNDLE_REQUESTED`, with `createDate` equal to its publish date and
  no `scheduledPublishDate`. A queued bundle whose publish date is in the future keeps today's
  `SCHEDULED` with `scheduledPublishDate`. The list, the detail and the `status` filter follow
  the same rule. Nothing is written to the database.

**Explicitly out of scope / non-goals**:

- No frontend change; the portlet already renders `BUNDLE_REQUESTED`.
- No change to the legacy push path (DWR action, Dojo handler, JSPs) or to the receiver side.
- No read timeout, endpoint health tracking or retry-policy change.
- No change to how the publisher job is scheduled or to its one-at-a-time execution.

## Regression Risk *(mandatory)*

- **Blast radius**: F1 and F2 apply to every push, including pushes made from the legacy
  portlet, because both portlets feed the same publisher job. A healthy receiver sees no
  difference; bundle size is not affected since only the connection wait is bounded. F3 changes
  the v1 API response for bundles that are due but not yet picked up, normally a two-second
  window; consumers are the Publishing Queue portlet and any script reading the v1 API.
- **Backward compatibility**: No database schema, index mapping or stored-status change.
  `SCHEDULED` stays a valid API value with a narrower meaning. The new configuration key has a
  default. Rollback-safe.
- **Data considerations**: None. Bundles stuck at deploy time are resolved by their next
  attempt.

## Acceptance & Verification *(mandatory)*

<!-- Measurable, so the fix is provably done. -->

- **AC-001**: With an enabled endpoint whose connection attempts get no answer, a pushed
  bundle reaches `FAILED_TO_SEND_TO_ALL_GROUPS` within the configured connect timeout plus 15
  seconds, and the endpoint message names the timeout.
- **AC-002**: In the AC-001 setup, a bundle pushed 7 seconds later to a healthy or refused
  endpoint has its audit row within 30 seconds of its publish date, and the publisher keeps
  running every minute.
- **AC-003**: An unexpected runtime error while processing one bundle leaves that bundle in a
  failed status with the message recorded and its queue rows removed; the remaining bundles in
  the same run are still processed.
- **AC-004**: A bundle pushed with `publishDate` equal to now, whose assets were added earlier,
  is returned by `GET /api/v1/publishing` and `GET /api/v1/publishing/{id}` as
  `BUNDLE_REQUESTED`, with `createDate` within 5 seconds of the push time and
  `scheduledPublishDate` null, until it is picked up.
- **AC-005**: A bundle pushed with `publishDate` one hour ahead is returned as `SCHEDULED` with
  `scheduledPublishDate` set, as today. `?status=SCHEDULED` returns only future-dated bundles;
  `?status=BUNDLE_REQUESTED` includes due, not-yet-picked-up bundles.
- **AC-006**: Existing push-publishing integration tests stay green, and a push to a healthy
  receiver behaves as before.
- **Verification method**: Integration tests in `dotcms-integration`, in classes already
  registered in a `MainSuite`: one asserting the publisher's HTTP client carries the configured
  connect timeout and its default (AC-001); one running the publisher job over two bundles where
  the first fails unexpectedly and asserting the second is still processed (AC-002, AC-003);
  one for AC-004 and AC-005 through the v1 API with a bundle whose queue entries carry a
  backdated entered date. The effect of the timeout on a real endpoint that drops connection
  attempts is verified manually, since that behavior belongs to the network stack, not to this
  code. Run with `./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=<Class>`.

## Assumptions

- `BUNDLE_REQUESTED` is the right status for a due, not-yet-picked-up bundle: it is the first
  status the publisher writes when it picks a bundle up, so the API reports the same state a
  few seconds earlier.
- Reporting the publish date as `createDate` for a due bundle is acceptable, since for an
  immediate push it is the push time.
- The connect timeout default is decided in the plan; the spec only requires it to be finite
  and configurable.
