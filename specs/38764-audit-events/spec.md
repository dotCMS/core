# Feature Specification: Structured Security Audit Events

**Feature Branch**: `issue-38764-audit-events`

**Created**: 2026-11-27

**Status**: Draft

**Type**: New Feature

**Input**: dotCMS reports its own security-relevant actions (sign-ins, credential changes, role
grants, bulk data export, plugin upload) as typed, documented, machine-readable events that
operators can forward to a monitoring system and alert on.

## Background

dotCMS records security-relevant actions today as free-text lines mixed into the application
log. The wording differs from one call to the next, many lines name the target of an action
without naming who performed it, some actions are not recorded at all, and nothing is
guaranteed to stay the same between releases. A monitoring system (a SIEM, security
information and event management system, such as Splunk, Elastic or Microsoft Sentinel) cannot
alert reliably on text like that, so an operator who wants to know "who granted this role" or
"which accounts signed in from this address" has to read logs by hand.

This feature adds a dedicated stream of audit events. Each event is one self-contained record
with a fixed set of fields and a stable name, written separately from the application log. The
event names and field names are a published contract: an alert rule written against them keeps
working across releases.

The roles used below are:

- **Security operator**: the person or team that watches a dotCMS installation for unusual
  account activity, for a self-hosted customer or for a hosted environment.
- **Platform operator**: the person who deploys and configures dotCMS and its log collection.
- **dotCMS user**: anyone who signs in or calls the API. Their actions are what gets recorded.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See who signed in, how, and from where (Priority: P1)

As a security operator, I receive one structured event for every sign-in, failed sign-in,
sign-out, lockout and "log in as another user" action, so that I can alert on unusual sign-in
activity without parsing free text.

**Why this priority**: Sign-in activity is the first thing any monitoring system asks an
application for, and it is where every later review of account activity starts. This story
also delivers the event stream itself, which every other story builds on.

**Independent Test**: Sign in successfully through each sign-in method dotCMS offers (the
admin sign-in page, the REST API, single sign-on, a remembered session), fail a password
sign-in, trigger a lockout, sign out, and start and end a "log in as" session. Each of these
actions produces exactly one event in the audit output with the fields described under
Functional Requirements.

**Acceptance Scenarios**:

1. **Given** a user with valid credentials, **When** they sign in with their password,
   **Then** one `auth.login` event is recorded with outcome `success`, the user as the actor,
   the method `password`, the client address and a request identifier.
2. **Given** a wrong password or an unknown user name, **When** a sign-in is attempted,
   **Then** one `auth.login` event is recorded with outcome `failure`, the same reason in both
   cases, and the user name that was attempted, and the event contains no password.
3. **Given** a user who signs in through a single sign-on provider, **When** the sign-in
   completes, **Then** the `auth.login` event names the method as single sign-on and identifies
   the provider.
4. **Given** repeated failed sign-ins that reach a lockout threshold, **When** the lock takes
   effect, **Then** one `auth.lockout` event is recorded naming the account, whether the lock
   applies to one client address or to the whole account, and the address concerned.
5. **Given** an administrator who uses "log in as" for another user, **When** they start and
   later end that session, **Then** one `auth.impersonation.start` and one
   `auth.impersonation.end` event are recorded, and every event produced in between names both
   the administrator and the user they are acting as.
6. **Given** a request that carries its own credentials (an API token, or a user name and
   password sent with the request) and those credentials are invalid, expired or revoked,
   **When** the request is refused, **Then** one `auth.request` event is recorded with outcome
   `failure` and, where the token could be identified, its identifier.
7. **Given** a client that makes many requests with a valid API token, **When** those requests
   succeed, **Then** no audit event is recorded for authenticating them.

---

### User Story 2 - Trust that the stream is complete, or know exactly when it is not (Priority: P1)

As a security operator, I can tell whether I have received every event, and as a dotCMS user I
never wait on, or fail because of, the recording of an event.

**Why this priority**: An audit stream that silently loses events is worse than none, because
people rely on it. A stream that can slow or break sign-in will be switched off. Both
properties have to be true from the first release.

**Independent Test**: Make the audit destination unwritable, or produce events faster than they
can be written, while performing ordinary actions. Every action still succeeds, the number of
events lost is reported, and a consumer reading the output can identify where the gap is.

**Acceptance Scenarios**:

1. **Given** the audit destination cannot be written to, **When** users sign in and work,
   **Then** their actions succeed as normal and the count of events that could not be written
   is visible through the product's monitoring and health reporting.
2. **Given** a burst of events larger than the stream can absorb, **When** the burst passes,
   **Then** the number of events written plus the number reported as lost equals the number of
   events produced.
3. **Given** a consumer reading the events of one node, **When** an event is missing from the
   middle of the stream, whether the product lost it or the log collector did, **Then** the
   consumer can detect the gap from the per-node sequence number.
4. **Given** events that are still waiting to be written, **When** dotCMS is shut down in an
   orderly way, **Then** those events are written before the process exits, followed by an
   `audit.stop` event.
5. **Given** a platform operator who has replaced the application's logging configuration with
   their own, or switched a logger off while the system is running, **When** audited actions
   occur, **Then** audit events are still recorded in full.

---

### User Story 3 - See every credential change (Priority: P2)

As a security operator, I receive an event whenever a password is changed or reset and whenever
an API token is created, revoked or deleted, so that I can see when the means of access to an
account changed and who changed it.

**Why this priority**: A credential change is how access to an account is kept or passed on.
These events are low in volume and high in value, and several of them are not recorded at all
today.

**Independent Test**: Change a password, request and complete a password reset, create an API
token, revoke it and delete it. Each action produces one event that names the actor and the
affected account, and no event contains a password, a reset link or a token value.

**Acceptance Scenarios**:

1. **Given** a signed-in user, **When** they change their own password, **Then** one
   `credential.password.change` event is recorded with that user as both actor and target.
2. **Given** an administrator, **When** they change another user's password, **Then** the event
   names the administrator as the actor and the other user as the target.
3. **Given** a password reset request, **When** it is submitted and later completed, **Then**
   one `credential.password.reset_request` and one `credential.password.reset` event are
   recorded, and neither contains the reset link or its token.
4. **Given** a user or administrator, **When** an API token is created, revoked or deleted,
   **Then** a `credential.api_token.create`, `credential.api_token.revoke` or
   `credential.api_token.delete` event records the token's identifier, the account it belongs
   to and who performed the action, and never the token value.
5. **Given** an administrator who revokes all the tokens of one user, or of every user, in one
   action, **When** the action completes, **Then** one `credential.api_token.revoke` event
   records the scope and the number of tokens revoked.
6. **Given** a short-lived token that the product creates for its own internal use while
   carrying out a user's request, **When** it is created or revoked, **Then** the event names
   that user as the actor and is marked as an internal-use token, so that a consumer can
   separate it from tokens people create.

---

### User Story 4 - See the actions that change privileges, move data out or add code (Priority: P2)

As a security operator, I receive an event when a role is granted or revoked, when the database
or a bulk copy of the site's data is downloaded, and when a plugin is uploaded, so that I can
alert on the actions with the largest consequences.

**Why this priority**: These few actions have the largest consequences of anything an
administrator can do, and they are rare in normal use, so an alert on them is cheap and
meaningful. Today several of them are recorded without the acting user or not at all.

**Independent Test**: Grant and revoke a role, download each kind of bulk export, and upload a
plugin. Each produces one event that names the acting user, the client address and the thing
acted on.

**Acceptance Scenarios**:

1. **Given** an administrator, **When** they grant one or more roles to a user, **Then** one
   `role.grant` event names the administrator as actor, the user as target and the roles
   granted.
2. **Given** a role grant that is refused or does not take effect, **When** the attempt ends,
   **Then** the event records `denied` or `failure` and never `success`.
3. **Given** a user who signs in through single sign-on and whose roles are synchronised from
   the provider, **When** the synchronised set of roles is unchanged, **Then** no role event is
   recorded; **When** it changed, **Then** at most one `role.grant` event lists the roles added
   and at most one `role.revoke` event lists the roles removed.
4. **Given** an administrator, **When** they download a database export, a starter export
   (with or without assets) or the complete set of assets, **Then** one `export.bulk` event
   records who downloaded it and which kind of export it was.
5. **Given** an administrator, **When** they upload a plugin and the product accepts and stores
   the file, **Then** one `plugin.upload` event with outcome `success` records who uploaded it
   and the file name.

---

### User Story 5 - Know exactly what will be recorded (Priority: P3)

As a platform operator, I have a published reference of every event and field, and one switch
for the whole stream, so that I can build alert rules before deploying and decide whether to
collect the events.

**Why this priority**: The stream is useful with its defaults. The reference makes it
adoptable by teams that must document what they collect.

**Independent Test**: Read the published reference and find every event type the product can
produce, with its fields. Turn the stream off in the deployment configuration, restart,
perform audited actions, and confirm that no audit output is produced.

**Acceptance Scenarios**:

1. **Given** the published event reference, **When** it is compared with the events the
   product can produce, **Then** every event name and field matches.
2. **Given** dotCMS starting with audit events on, **When** the stream begins, **Then** an
   `audit.start` event is the first record of that start.
3. **Given** the stream turned off in the deployment configuration, **When** audited actions
   are performed, **Then** no audit events are recorded and nothing else changes.

---

### Edge Cases

- **An action that is attempted and then undone.** If the change is rolled back, the event
  records `failure`. A `success` event is never written for a change that did not take effect.
- **The same action through different interfaces.** A role granted through the REST API, an
  older admin screen, a user creation form or single sign-on provisioning produces the same
  event type with the same fields.
- **Actions with no request.** Work done by a scheduled job or at start-up has no client
  address and no signed-in user. The event carries the actor type `system` and leaves the
  source fields empty.
- **Work that continues after the request ends.** A password reset email is sent in the
  background; the event still names the actor and client address of the request that started
  it.
- **Credentials sent with every request.** Where a client sends a token or a password with
  each request and no session is created, only refused requests are recorded, as `auth.request`
  and not as `auth.login`. A session-based sign-in is recorded once, when the session starts.
- **A client that keeps retrying a refused credential.** The first refusal is recorded at once.
  Further refusals of the same credential from the same client address during the following
  minute are recorded as one summary event at the end of that minute, stating how many
  refusals it stands for.
- **Sign-in failures that dotCMS does not decide.** A sign-in that fails at the single sign-on
  provider never reaches dotCMS and produces no event. A single sign-on response that dotCMS
  itself rejects is recorded as an `auth.login` failure. An invalid or expired "remember me"
  cookie is not recorded in the first version.
- **Remembered sessions.** A sign-in through a "remember me" cookie is recorded as `auth.login`
  with its own method, once per new session, so that it can be told apart from a password
  sign-in. The cookies that keep a browser signed in are not API tokens and produce no
  credential events.
- **A session that ends without a sign-out.** A session that expires produces no `auth.logout`
  event, and a "log in as" session that ends by expiry produces no
  `auth.impersonation.end` event. The reference states this.
- **A lock that ends.** The first version records when a lock takes effect, not when it
  expires or is cleared.
- **A password reset requested for an address that matches no account.** The event is still
  recorded, with the address submitted, truncated, and no target account. Unlike a failed
  sign-in, this event deliberately shows whether an account matched, because an operator needs
  to know whether a reset was actually issued.
- **Malformed or oversized input.** A user name containing line breaks, control characters or
  several thousand characters cannot split, forge or bloat a record: values are encoded and
  truncated to a documented length.
- **Abrupt termination.** If the process is killed without an orderly shutdown, events not yet
  written can be lost. The reference states this limit; the next start is identifiable by a
  new per-start identifier.
- **More than one node.** Each node records its own events with its own node identifier and
  sequence. Ordering across nodes relies on timestamps.

## Requirements *(mandatory)*

### Functional Requirements

**What an event is**

- **FR-001**: The system MUST record each audited action as one self-contained record in a
  documented, machine-readable format, one record per line, separate from the application log.
- **FR-002**: Every event MUST carry: a schema version; a unique event identifier; a timestamp
  in UTC with millisecond precision taken when the action happened; the event name; the
  outcome; the actor; the target of the action where there is one; the client address as
  resolved by the product's existing trusted-proxy rules; the user agent; a request identifier
  shared by all events from one request; the node identifier; a per-start identifier; and a
  per-node sequence number that increases by one with each event. The number is assigned when
  the event is produced, before it is written, so that every event the product drops or fails
  to write leaves a gap a consumer can see.
- **FR-003**: The actor MUST always be present and MUST state its type: a user, the system
  itself, or an unauthenticated caller. For a user the event MUST also state how they
  authenticated and, when an API token was used, the token's identifier. The type `system` is
  used only when no user's request caused the action.
- **FR-004**: When an administrator is acting as another user, every event MUST name both the
  administrator and the user being acted as.
- **FR-005**: The outcome MUST be one of `success`, `failure` or `denied`, carried as a field
  and not as part of the event name. A `failure` or `denied` event MUST carry a reason taken
  from a documented, fixed list.
- **FR-006**: Events MUST NOT contain passwords, password hashes, API token values, session
  identifiers, password-reset links, request bodies or raw error text. Tokens are referred to
  by identifier only.
- **FR-007**: Every text value that originates from a user or a request MUST be encoded so it
  cannot break the record format and MUST be truncated to a documented maximum length.
- **FR-008**: An event type MUST only carry the additional attributes documented for that type.
  There is no free-form field.
- **FR-009**: A failed sign-in MUST record the user name that was attempted, truncated as
  FR-007 requires. Its reason MUST be the same whether the user name is unknown or the password
  is wrong, so that failed sign-ins are not a list of which user names exist. A successful
  sign-in MUST carry the account's sign-in name in the same field, so that failures and a
  later success for one account can be matched.

**Which actions are recorded**

- **FR-010**: The first version MUST record the following events.

  | Event name | Recorded when |
  |---|---|
  | `auth.login` | A sign-in that starts a session succeeds or fails, by any method (password, single sign-on, remembered session). |
  | `auth.logout` | A user signs out. |
  | `auth.lockout` | A lock takes effect after repeated failed sign-ins or refused WebDAV requests. States whether it applies to one client address, to WebDAV from one client address, or to the whole account. |
  | `auth.impersonation.start`, `auth.impersonation.end` | An administrator starts or ends acting as another user. |
  | `auth.request` | A request that carries its own credentials (an API token, or a user name and password) is refused. Successful requests are not recorded in this version. |
  | `credential.password.change` | A password is changed, by its owner or by an administrator. |
  | `credential.password.reset_request` | A password reset is requested. |
  | `credential.password.reset` | A password reset is completed. |
  | `credential.api_token.create` | The product issues an API token, by any path. |
  | `credential.api_token.revoke` | One API token, all of one user's tokens, or every user's tokens are revoked. A bulk revocation is one event. |
  | `credential.api_token.delete` | An API token is deleted. |
  | `role.grant`, `role.revoke` | One or more roles are granted to or removed from a user, by any path, including roles assigned while creating a user. The event lists the roles. |
  | `export.bulk` | A database export, a starter export with or without assets, or the complete set of assets is downloaded. States which. |
  | `plugin.upload` | A plugin file is uploaded through the product. `success` means the file was accepted and stored. |
  | `audit.start`, `audit.stop` | The audit stream starts or stops. |
  | `audit.events.lost` | The stream resumes after losing events; states how many. |

- **FR-011**: An audited action MUST produce the same event through every interface by which
  the product allows that action. The interfaces known at the time of writing are: for
  sign-in, the admin sign-in page, the REST API, single sign-on and remembered sessions; for
  requests that carry their own credentials, the REST API and WebDAV; for passwords and API
  tokens, the REST API, the older user administration screens and
  the password reset pages; for role changes, the REST API, the older user and role
  administration screens, user creation and single sign-on provisioning; for exports and
  plugin upload, the REST API and the maintenance and plugin administration screens. If
  planning finds an interface outside this list, the list is amended and re-approved.
- **FR-012**: For every action in FR-010 that can be refused or can fail, unsuccessful attempts
  MUST be recorded as well as successful ones, unless the table says otherwise.
- **FR-013**: A `success` event MUST be recorded only after the action has taken effect. An
  attempt that was refused, failed or was undone MUST be recorded as `denied` or `failure`.

**Keeping the volume meaningful**

- **FR-014**: A sign-in that starts a session MUST produce one event per sign-in, not one per
  request.
- **FR-015**: Role changes made by synchronising a user's roles from a single sign-on provider
  MUST produce at most one `role.grant` event and one `role.revoke` event per sign-in, and none
  when the roles did not change.
- **FR-016**: The first `auth.request` refusal of a credential from a client address MUST be
  recorded at once. Further refusals of the same credential from the same address during the
  following minute MUST be recorded as one summary event at the end of that minute, stating
  the number of refusals it stands for. A pending summary MUST be written on an orderly
  shutdown.
- **FR-017**: A token that the product creates for its own internal use while carrying out a
  user's request MUST be recorded with that user as the actor and MUST be marked as an
  internal-use token.

**Delivery**

- **FR-018**: Events MUST be written to a dedicated audit file, separate from the application
  log. The file MUST NOT grow without bound: the product limits its size and the number of
  older files kept, and the limits are documented and configurable.
- **FR-019**: The platform operator MUST be able to have the events also written to standard
  output, each carrying a field that lets a collector separate audit events from other output.
  This copy is off by default, so that an upgrade adds nothing to standard output.
- **FR-020**: The user action that produces an event MUST NOT wait for the event to be written
  and MUST NOT fail because it could not be written, including when the audit destination is
  slow, full or unavailable.
- **FR-021**: The system MUST count events written, events dropped (not accepted because the
  stream was at capacity) and events failed (accepted but not written), and expose the three
  counts through the product's existing monitoring and health reporting. These counts are the
  authoritative record of loss. When writing resumes after a loss, the system MUST also record
  an `audit.events.lost` event stating how many events were lost.
- **FR-022**: On an orderly shutdown the system MUST write all pending events before exiting.
- **FR-023**: The audit stream MUST NOT depend on the application's logging configuration.
  Replacing that configuration, or changing log levels while the system is running, MUST NOT
  stop, filter or reformat audit events.

**Control and documentation**

- **FR-024**: Audit events MUST be on by default. The platform operator MUST be able to turn the
  stream off through the deployment configuration. The audit settings MUST NOT be changeable
  from inside the running application, whether through an admin screen or an API.
- **FR-025**: The product MUST publish a reference of every event name, its meaning, its
  fields, its reasons and the release that introduced it. The reference MUST match what the
  product emits, and that match MUST be verified automatically.
- **FR-026**: Event names and field names are a compatibility contract. A published name MUST
  NOT be renamed, removed or reused for a different meaning. New events and new fields MAY be
  added. Any change a consumer could not ignore MUST change the schema version.
- **FR-027**: Field names MUST follow the Elastic Common Schema (a widely used public naming
  convention for log fields) wherever it defines an equivalent field, and a product-specific
  namespace for everything else. This is a naming choice only; it places no requirement on
  where events are stored.

### Out of Scope

- Storing, searching or displaying audit events inside dotCMS.
- Sending events over the network to a monitoring system. The product writes them; the
  operator's log collector ships them.
- Making the recorded events tamper-proof, or setting how long they are kept once collected.
  Both are properties of where the operator ships them.
- Turning individual event types on or off. The first version has one switch for the stream.
- Events for creating, changing and deleting users and roles themselves; for permission
  changes on content; for configuration and secret changes; for activating, stopping or
  removing a plugin, for changes to the packages plugins may load, and for plugins placed
  directly on the file system; for other downloads such as log files; and for refused access
  to content. These are planned as later additions to the same catalogue.
- Recording the end of a session by expiry, and the end of a lock.
- Generating a starter file onto the server's own disk without downloading it, which an
  optional older setting allows.
- Events for ordinary administrative and content activity (publishing, workflow, templates).
- Any change to the existing free-text security, admin and activity log lines.
- Any change to the format or content of the application log.

### Key Entities

- **Audit event**: One record of one action. Has a name, an outcome, a time, an actor, an
  optional target, the origin of the request and the identifiers that let a consumer order
  events and detect gaps.
- **Event catalogue**: The fixed list of event names the product can produce, each with its
  meaning, its permitted attributes, its reasons and the release that introduced it. It is the
  source of both the product's behaviour and the published reference.
- **Actor**: Who performed the action: a user (with how they authenticated), the system, or an
  unauthenticated caller. Separate from the impersonator when one administrator acts as
  another user.
- **Target**: What the action was applied to: a user account, an API token, an export or a
  plugin file, identified by type, identifier and name. For role events the target is the
  user, and the roles are listed as an attribute of the event.
- **Audit stream status**: The counts of events written, dropped and failed since start.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Every event in the catalogue is covered by at least one automated test that
  performs the action and checks the resulting event.
- **SC-002**: For each event in FR-010, performing the action through each interface listed in
  FR-011 produces exactly one event with the correct outcome, in 100% of tested cases.
- **SC-003**: Using only the published reference, an operator can express these three alert
  rules, and each matches when replayed against sample output: five failed sign-ins followed by
  a successful one for the same account within ten minutes; any plugin upload; any grant of an
  administrator role.
- **SC-004**: With the audit destination made unwritable, 100% of sign-ins and other audited
  actions still succeed, and the counts reported as dropped and failed add up to the number of
  events not written.
- **SC-005**: Under a burst that exceeds what the stream can absorb, events written plus events
  reported as dropped or failed equals events produced, with no unaccounted difference.
- **SC-006**: A client making 1,000 successful API requests with one valid token produces zero
  audit events. A client making 1,000 requests in one minute with one expired token produces
  at most three `auth.request` events whose counts add up to 1,000. A single sign-on user who
  signs in ten times with unchanged roles produces ten sign-in events and zero role events.
- **SC-007**: Across the events produced by the automated tests, zero contain a password, a
  token value, a session identifier or a reset link.
- **SC-008**: After an orderly shutdown with events pending, zero events are missing from the
  output.
- **SC-009**: With the application's logging configuration replaced by one that defines no
  audit output, the audit events of a test run are still complete.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: Sign-in in all its forms, including the older admin sign-in
  screens, WebDAV and single sign-on; user and role administration, part of which is still
  served by older admin screens; API token management; database, starter and asset export;
  plugin upload. All of these are in, or reachable through, the older product surface, which
  is why FR-011 requires the same event whichever interface is used. The existing free-text
  security, admin and activity log lines are left exactly as they are.
- **Backward-compatibility expectations**: No existing API, admin workflow or log line changes.
  One thing is new for an operator on upgrade, because the feature is on by default: a new
  audit file appears beside the existing logs, bounded in size as FR-018 requires. Nothing is
  added to standard output unless the operator asks for it. There is no database or search
  index change, so the release can be rolled back without data migration.
- **Known related decisions**: The client address in each event follows the existing
  trusted-proxy rules (`docs/backend/CLIENT_ADDRESS_RESOLUTION.md`). An open request asks for
  the older, overlapping loggers to be consolidated
  ([#32412](https://github.com/dotCMS/core/issues/32412)); this feature does not do that, and
  is designed so that it can happen later without changing the event contract. The plan phase
  will consult `dotCMS/platform-adrs`.

## Assumptions

- The first version covers security actions only. The catalogue is built so that other
  categories can be added later without changing existing events.
- Operators already run a log collector and a monitoring system, or can add one. dotCMS writes
  the events; it does not deliver them.
- Availability comes before completeness: when events cannot be recorded, the product keeps
  working and reports the loss. There is no mode that refuses user actions when auditing is
  unavailable.
- In a container the audit file is only as durable as the storage it is written to. Operators
  who collect from standard output turn that copy on; operators who want the file to outlive
  the container mount storage for it. The documentation explains both.
- The exact list of field names, the fixed list of reasons and the maximum lengths are contract
  details delivered with the implementation as part of the published reference. This
  specification fixes what information is present and the rules the names follow.
- The feature targets current releases. It is not designed to be applied to older long-term
  support lines.
