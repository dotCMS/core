# Evergreen Tracks — CX / Support Briefing Outline

Agenda for the live session with **Dean Gonzalez / CX**. Goal: CX can take a track-change request
end to end using [SUPPORT-RUNBOOK.md](SUPPORT-RUNBOOK.md) without asking engineering what the words
mean. 30 minutes plus Q&A.

**Pre-read (send with the invite)** — send the Google Doc links, not the GitHub ones:

- [Evergreen Tracks — Support Runbook (interim)](https://docs.google.com/document/d/1tLEzbZOC5D5cXDq0S_bHtsKqd0-13hSUAcXfPeLlblI/edit) (source: [SUPPORT-RUNBOOK.md](SUPPORT-RUNBOOK.md))
- [Evergreen Tracks — CX / Support Briefing Outline](https://docs.google.com/document/d/1DyjRKAB_aiwnRShLuQLYx-PbnyiH5Ft5XeCgPNDc1YY/edit) — this agenda
- The published customer doc [dev.dotcms.com/docs/evergreen-tracks](https://dev.dotcms.com/docs/evergreen-tracks)

Both Docs live in the **Engineering** shared drive → `dotEvergreen`, editable by `all@dotcms.com`.

---

## 1. What tracks are, in customer language (5 min)

- A track controls **how fresh** a release an environment gets. Same artifacts on every track —
  only the timing differs.
- Three tracks: `latest` (every GA release), `standard` (~14 days old, the default),
  `trailing` (~28 days old).
- **Per environment, not per customer.** Dev on `latest`, prod on `trailing` is normal.
- Where the fleet is today: ~180 `standard`, 9 `trailing`, 1 `latest`. Standard is the default and
  most customers will never ask for anything.
- The one sentence that prevents most escalations: **tracks are forward-only — moving slower never
  rolls a running version backward.**

## 2. What CX owns vs what platform owns (3 min)

- CX: intake, validation, expectation-setting, customer comms, closing the ticket.
- Platform / Enablement: the manifest PR, the merge, verification. CODEOWNER approval is required,
  so this cannot be self-served by support — and that is deliberate.
- Interim reality: every track change is a hand-shipped PR. Self-service is roadmap (Workstream B).

## 3. The request flow, walked live (10 min)

Walk the runbook's five steps against a real example (bcbs `dev-2310`, the one env on `latest`):

1. **Intake** — the ticket template; why an informal Slack ask still becomes a ticket.
2. **Validate** — tenant/env slug (not the friendly name), requested track is one of three,
   requester authority, eligibility gates, and *is this env maintenance-paused*.
3. **Hand off** — what platform will do: label edit; faster move also bumps the pinned image
   digest, slower move is label-only with no restart.
4. **Verify** — Argo Synced/Healthy, `imageID` is the truth, env serving.
5. **Reply** — which env, new track, whether the version changed now, when the next update lands.

## 4. The three answers CX will need most (7 min)

Read them out loud from §6 of the runbook so the wording is shared:

- "Does moving slower roll us back?" → No; it stops advancing until it catches up.
- "When will our next update apply?" → `latest`: next GA. `standard`/`trailing`: next maintenance
  window, newest GA older than ~14/~28 days. **No specific dates promised.**
- "Put us back on last month's version" → that's a rollback, not a track change → escalate.

Also flag: custom cadences / skipping a release are **not** supported; a customer already running a
flagged release is an **incident**, escalate immediately.

## 5. Escalation and the sheet (3 min)

- Escalation path: CX → platform/Enablement, `#team-cloud-engineering`,
  `@dotcms/platform-engineers` / `@dotcms/CloudEng-Support`, ticket always linked.
- The **"dotCMS Maintenance Pause List & Evergreen Tracks"** sheet is CX's registry, kept in sync by
  hand. Git is the source of truth; when they disagree, the sheet gets fixed. Whoever changes a
  track or a pause updates the sheet in the same sitting.
- Maintenance pauses (Duncan Aviation, Jostens, firstmac today) are **never** cleared as a
  side-effect of a track request.

## 6. Q&A capture (rest of session)

Log every question **in the issue thread on dotCMS/core#36522** — one comment, question + answer,
or question + owner if unanswered. Anything that needed an engineering answer during the session is
a gap in the runbook: update the runbook, don't just answer it once.

Known open items to raise if CX doesn't:
- What SLA does a track-change request get? (Not defined yet — CX to propose.)
- The Google Doc above is the published copy today. Does CX also want it mirrored into their own KB
  (e.g. Freshdesk solutions), and if so who owns keeping that copy in sync with the repo file?
- Do we proactively tell customers which track they're on, or answer on request only?

---

## Exit criteria for the session

- [ ] CX can name the three tracks and their timing without notes.
- [ ] CX can state the forward-only rule in customer words.
- [ ] Ticket template in use for the next real request.
- [ ] Q&A captured on #36522; runbook updated for anything that was unclear.
- [ ] Owner named for publishing the runbook to the internal KB.
