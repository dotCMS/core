# Issue Resolution Specification: Push Publish Results Missing for Multiple Endpoints

**Feature Branch**: `20261029-094709-push-publish-results`

**Created**: 2026-10-29

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [dotCMS/core#34434](https://github.com/dotCMS/core/issues/34434) (support tickets 35120, 37937)

**Input**: User description: "[DEFECT] Push Publish Results Missing for Multiple Endpoints — an environment with both a static/S3 endpoint and a dynamic (HTTP) endpoint shows results for only some endpoints in the push publish audit."

## Problem Statement *(mandatory)*

When a bundle is push published to an environment configured with "Push to All Endpoints" that holds
endpoints of more than one protocol (for example an HTTP/dynamic endpoint and a static or S3 endpoint),
the push publish audit and results dialog do not list every endpoint. Endpoints of one protocol are
missing, and the overall status can read SUCCESS even though one endpoint has no recorded result.

The issue thread went through two framings. The original report was a display gap. A later comment
(support ticket 37937) confirmed by testing that this is a **real delivery failure**: with a mix of
Static and Dynamic endpoints, only the endpoints of the first publisher type in the iteration receive
the bundle, and the second type is skipped with no error and no FAILED status. Which type wins depends
on order, and it reproduces with both standard and "Force Push Everything" filters. This spec
therefore covers two defects: **the bundle is not delivered to every endpoint type**, and **the audit
does not show every endpoint's result**, which also hides the first defect.

**Severity / Impact**: Medium. Operators cannot tell from the audit whether each endpoint received the
bundle. Affects any customer who mixes static/S3 and dynamic endpoints in one environment.

## Reproduction *(mandatory)*

**Environment**: Current main and the evergreen build named in the issue. One publishing environment
with "Push to All Endpoints" enabled, containing one HTTP (dynamic) endpoint and one static or S3
endpoint.

**Steps to Reproduce**:

1. Create an environment with "Push to All Endpoints" and add one dynamic (HTTP) endpoint and one
   static or S3 endpoint.
2. Push publish any content bundle to that environment.
3. Open the bundle's push publish audit / results dialog.
4. Repeat with the endpoints created in the opposite order, and with a "Force Push Everything" filter.

**Expected Behavior**: Every configured endpoint appears in the results with its own status.

**Actual Behavior**: Only endpoints of the first publisher type receive the bundle. The other type is
skipped with no error logged. Only the endpoints of one protocol appear in the audit, and the overall
status shows SUCCESS even though the other endpoints were never reached.

**Reproducibility**: Reproduced on 2026-11-03 on this worktree's build (platform license, full starter,
one environment "Push to All Endpoints" with one `http` endpoint and one `static` endpoint, one file
asset pushed). Reproduced 4 of 4 times, with the push publisher always running first: Default filter
with a failing `http` endpoint, Default filter with a succeeding `http` endpoint, the "Force Push
Everything" filter (`ForcePush.yml`, bundle stored with `force_push=true`), and the Default filter
after a container restart. The publisher order is a `HashSet` of classes and did not change on
restart, so the opposite order (static first) was not observed in those four runs. After a later
restart the order did flip, and static-first was observed (see below).

The S3 publisher was also reproduced (bundle `01M6H8XEN2QKPKDK81MXGV8ABQ`, an environment with an
`http` endpoint and an `awss3` endpoint, Default filter, publisher order push then S3). `AWSS3Publisher`
logged `Retrying bundle: <id>, we don't need to run bundlers again`, then `Bundle push starting`,
`Pushing bundle` and `Bundle is EMPTY`. So the skip is the same for S3 as for static. In this run the
S3 credentials could connect but lacked the `s3:ListAllMyBuckets` permission, which logged "Can't
connect to End Point" and is incidental: the bundlers were skipped before that point.

**Reproduction evidence (log of bundle `01M6GMFCBE0Z219Q7ANCB81VVM`)**:

1. The push publisher ran all bundlers, then tried to send. The `http` endpoint pointed at the same
   instance and failed with "Invalid token", which is incidental to this reproduction.
2. The static publisher then logged `Retrying bundle: <id>, we don't need to run bundlers again`. It
   ran no bundlers, then logged `Bundle is EMPTY` and `Status Update: Bundle sent`. The bundle folder
   on disk (`<id>-static`) was empty.
3. The audit row ended with a single endpoint entry (the `http` one). The static endpoint is absent
   from the endpoint map, and `numTries` is 2 on a first and only attempt.
4. With a succeeding `http` endpoint (a real API token issued by the same instance) the push
   publisher logged "Bundle sent", and the static publisher still logged the "Retrying bundle" line,
   "Bundle is EMPTY" and "Bundle sent". The first publisher failing is therefore not required. The
   final audit in that run is not usable as evidence, because the receiver and the sender are the
   same instance and both write the same audit row.
5. With the "Force Push Everything" filter the static publisher was skipped in the same way, so the
   filter does not matter.

## Scope of Investigation *(mandatory)*

- **Affected area**: Push publishing — the publisher queue job, the per-protocol publishers (push,
  AWS S3, static), and the per-endpoint audit history shown in the publishing queue UI.
- **Suspected surface**: Modern packages (`com.dotcms.publisher.business.PublisherQueueJob`,
  `com.dotcms.publishing.*`, and the `PushPublisher`, `AWSS3Publisher` and `StaticPublisher`
  classes). Confirmed during planning.
- **Related known decisions**: None known. The plan consults `dotCMS/platform-adrs` for anything on
  publishing audit state.

## Root-Cause Hypothesis

There are two defects. The delivery defect (1) was reproduced and its cause confirmed. The two audit
causes under defect 2 were found by code reading; the missing static endpoint in the audit was seen in
the reproduction, but which of the two causes produced it was not separated.

**Defect 1 — the second publisher treats the first attempt as a retry and bundles nothing.**
Confirmed by reproduction and code reading. The cause the issue comment proposed (bundle output
closed after the first publisher) is not what happens.

- In `PublisherAPIImpl.publish()` (line 134), a publisher only builds its bundle when
  `!output.bundleFileExists() || !publishAuditAPI.isPublishRetry(config.getId())`.
- `DirectoryBundleOutput`'s constructor calls `BundlerUtil.getBundleRoot(config)`, which creates the
  bundle folder at once. So `bundleFileExists()` is already true when `createBundleOutput()` returns,
  and the condition reduces to `!isPublishRetry(...)`.
- `isPublishRetry` is true when the audit's `numTries` is above zero. The first publisher's
  `process()` raises `numTries` (the push publisher does so before sending, the static publisher in
  `startStatusHistory`).
- The second publisher in the loop therefore sees `numTries > 0` on the first attempt, skips every
  bundler, and works on an empty folder. The static publisher then reports "Bundle sent" and
  records no endpoint. Because the order is arbitrary, which type is skipped varies, as reported.
- This explains why "Force Push Everything" does not help: the skip is not about which assets are
  eligible, it is about the retry check.

**Third symptom, observed with S3 (a consequence of defect 2).** One minute later the queue job
picked the bundle up again (status 4) and `PushPublisher.process` threw a `NullPointerException` at
line 207: the audit listed the environment but not the S3 endpoint, so `endpointsDetail.get(ep.getId())`
returned null and `epDetail.getStatus()` failed (the FAILED_ENDPOINTS delivery-strategy branch). The
bundle ended with status 6 (FAILED_TO_PUBLISH) and a single audit entry keyed by an empty endpoint id
carrying the NPE message, so the operator sees a failure that names no endpoint. Whether a given
publish ends in this failure or in a silent SUCCESS depends on which audit state the endpoint was
left in. The fix must make the retry path tolerate a missing endpoint entry (treat it as not yet
delivered) as well as recording every endpoint.

**Static first (observed after a container restart flipped the order).** With the publishers in the
order static, push (two bundles: a plain content item, then a live page), the push publisher ran all
its bundlers and sent the bundle, and no "Retrying bundle" line appeared. The static publisher ran its
own bundlers first. The code-read prediction that the static publisher's `startStatusHistory()` would
make the push publisher skip was wrong. So the delivery skip is **asymmetric**: it happens when the
push publisher (or S3) runs first, because the push publisher raises and persists `numTries` before
the next publisher's turn, and not when the static publisher runs first, because the static publisher
does not leave `numTries` above zero at that point. The plan must still test both orders, since the
loop's guard is order-dependent. The audit rows in those two runs were written by the receiver, which
was the same instance as the sender, so they could not show the sender's endpoint list. That was
closed with a second instance (below).

**Two instances, static first (sender on port 8080, receiver on port 8081; bundle
`01M6HHY3WYSA72FT3DXEZZSMGP`, live page, Default filter, environment "Push to All Endpoints" with one
`static` and one `http` endpoint, the `http` one pointing at the receiver with its API token).**

- Delivery worked in this order: the receiver recorded the bundle as published (status 9), so the
  `http` endpoint received it.
- The sender's audit row ended as `BUNDLE_SAVED_SUCCESSFULLY` (14) with **one** endpoint entry, the
  static one. The `http` endpoint, which did receive the bundle, has no entry at all. The log reports
  "Re-publish attempts: 2 out of 3" and the row holds `numTries` 3 on a first and only attempt.
- This is defect 2 observed on the sender, in the order where delivery is not skipped: the audit
  lists only some endpoints and the overall status reads as success. So the audit gap is independent
  of the delivery skip. The S3 publisher (`AWSS3Publisher`) was run with push
first and is skipped the same way (see the reproduction above).

**Defect 2 — the audit does not show every endpoint.**

1. **Publishers skip recording successful endpoints once the audit has content.** The push, S3 and
   static publishers each write their endpoints into the audit history only when the history is empty
   or the environment failed. The queue job creates the audit row with an empty history, so whichever
   publisher runs first records its endpoints. The publisher that runs second sees a non-empty history
   and records only failures, so its successful endpoints never appear. The order comes from a
   `HashSet` of publisher classes in `getPublishersForBundle`, so it is not deterministic.
2. **The audit-collection step can overwrite one protocol's endpoints with the other's.** In
   `PublisherQueueJob.collectEndpointInfoFromRemote`, static and S3 endpoints are not polled
   remotely; their local record is copied in. Dynamic endpoints are polled and their history is
   copied in. Both copies use a top-level `putAll` into a map keyed by environment, so the later
   write can replace the other protocol's endpoint map. The status checks compare against the number
   of environments rather than the number of endpoints, which lets the status read SUCCESS.
   `PublisherQueueJob` also does not poll static and S3 endpoints, so an undelivered bundle is not
   noticed there. This matches the reported behavior that the queue shows success.

Unknown: whether the remote history is keyed by the same environment id as the local record. That
decides whether cause 2 occurs in practice. It is confirmed in planning, by reproduction.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- The bundle is delivered to the endpoints of every publisher type in the environment, whichever
  publisher runs first, with the standard and the "Force Push Everything" filters.
- The decision to reuse an existing bundle (a real retry) is no longer triggered by another publisher
  having raised the attempt count during the same first attempt.
- A publisher that cannot deliver sets a FAILED status instead of being skipped silently. A static
  publisher that finds an empty bundle does not report "Bundle sent".
- Every endpoint of every protocol in the environment is recorded in the audit history, whichever
  publisher runs first.
- Merging local and remote endpoint details per endpoint, so one protocol cannot replace the other's.
- Status evaluation that counts endpoints, so a missing or failed endpoint cannot read as SUCCESS.
- A deterministic publisher order, if the plan finds it needed.

**Explicitly out of scope / non-goals**:

- Redesigning the publishing pipeline. The bundle output lifecycle is changed only as far as the
  confirmed delivery cause requires.
- Changing the shape of the stored audit history (`PublishAuditHistory` JSON).
- Changing the push publish UI beyond showing the endpoints the backend now records.
- Repairing audit rows already written by past publishes.
- Retry semantics, except where the existing "record only on failure" guard exists for retries and
  must be preserved.

## Regression Risk *(mandatory)*

- **Blast radius**: All push publishing, including single-protocol environments, retries of failed
  bundles, and the queue status that drives "Publishing" and "Success". The guard being relaxed
  appears to protect retry history from being overwritten, so retry behavior is the main regression
  risk.
- **Backward compatibility**: The stored audit JSON shape is unchanged, so rollback should be safe.
  The plan confirms against the rollback-unsafe categories.
- **Data considerations**: Existing audit rows are not migrated. Only new publishes are corrected.

## Acceptance & Verification *(mandatory)*

- **AC-001**: Publishing to an environment with one dynamic and one static (or S3) endpoint delivers
  the bundle to both, and produces an audit with both endpoints, each with its own status, in either
  publisher order and with either filter.
- **AC-004**: If a publisher cannot deliver, the audit shows FAILED for its endpoints. It is never
  skipped without a status.
- **AC-002**: Single-protocol environments and retries of a failed bundle record the same results as
  before.
- **AC-003**: If one endpoint's result is missing or failed, the overall status is not SUCCESS.
- **Verification method**: A new integration test with one environment holding a static and a dynamic
  endpoint, run in both publisher orders, asserting both endpoints are in the audit. Existing
  `PublisherQueueJobTest`, `PublisherAPIImplTest` and a retry scenario run as the regression check.
  Run with `./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dmaven.build.cache.enabled=false -Dit.test=PublisherQueueJobTest`
  and confirm `Tests run: N` in the failsafe report. Suite placement follows the integration-test
  rule (measured with `just suite-timings`) in the plan.

## Assumptions

- The delivery failure is real: reported in ticket 37937 and reproduced here for the push-first order.
- This is Tier 2: the fix touches the core publishing loop shared by every publisher, and the retry
  check also protects real retries.
- Mixed-protocol environments are a supported configuration.
- The research combined reading code on current main with a local reproduction (static and S3
  endpoints). Both publisher orders were observed, and the audit
endpoint list was observed on a real sender (a second instance as the receiver) in the static-first
order. The push-first order's audit was seen in the first runs.
