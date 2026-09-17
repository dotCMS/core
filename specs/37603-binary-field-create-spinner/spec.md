# Issue Resolution Specification: Binary field stuck on infinite loading spinner when creating new content

**Feature Branch**: `37603-binary-field-create-spinner` (work is being done on `nicobytes/ticket-binary`)

**Created**: 2026-09-17

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#37603](https://github.com/dotCMS/core/issues/37603)

**Related Support Ticket**: [Freshdesk #39006](https://helpdesk.dotcms.com/helpdesk/tickets/39006)

**Input**: Customer-reported defect on `25.07.10_lts_v16`, reproduced locally against the same image.

## Problem Statement *(mandatory)*

When an editor creates **new** content of any content type that includes a Binary field, the
Binary field area renders a loading spinner that never resolves. Every other field on the form
renders normally. The Binary field never becomes usable, so no file can be attached.

The only way to save is to make the Binary field optional and save while ignoring the spinner.
Reopening the saved content afterwards shows the Binary field working correctly — the defect is
specific to the creation path.

No error message is ever shown. The failure is silent: the error handler that was meant to
replace the spinner writes to a misspelled DOM property, so the spinner simply stays.

**Severity / Impact**: High — major functionality broken. Affects every content type with a
Binary field in the legacy (non-beta) content editor. For the reporting customer this blocks a
pre-cutover upgrade, and several of their content types use Binary fields. Impact is
environment-dependent (see Root-Cause Hypothesis): in some environments the defect is invisible,
which is why it survived undetected across releases.

## Reproduction *(mandatory)*

**Environment**: `dotcms/dotcms:25.07.10_lts_v16` (also present in `main` and `25.07.10_lts_v10`).
Legacy Dojo/JSP content editor — the "Edit Content Beta" editor is unaffected. Browser-independent
(reproduced in Chrome). Requires an environment where `GET /api/v1/content/` returns a
**non-JSON** body — empty, or the site's HTML 404 page.

**Steps to Reproduce**:

1. Run dotCMS in an environment where `GET /api/v1/content/` returns a non-JSON body (empty or
   the site's HTML 404 page). To check, open DevTools → Network and inspect the Response of that
   call: a body of `404` hides the defect; an empty or HTML body exposes it.
2. Create a content type that has a Binary field (e.g. a `FILEASSET` base type).
3. Go to **Content** and create **new** content of that type using the regular (non-beta) editor.

**Expected Behavior**: The Binary field renders and accepts a file upload.

**Actual Behavior**: An infinite loading spinner. The field never becomes usable and no error
message is displayed.

**Reproducibility**: Always, in an environment whose 404 body is not valid JSON. Never, in an
environment whose 404 body is the bare string `404` — there the component is created anyway and
the defect is masked.

## Scope of Investigation *(mandatory)*

- **Affected area**: Content editing — the legacy Dojo/JSP content editor's Binary field
  hydration path (`dotcms-binary-field` web component bootstrapping).
- **Suspected surface**: **Legacy**. The defect is in a JSP under
  `dotCMS/src/main/webapp/html/portlet/ext/contentlet/field/edit_field.jsp`, specifically the
  inline JavaScript in the `autoexecute` IIFE. It is server-rendered legacy UI, not `core-web`
  Angular code and not `com.dotcms.*` / `com.dotmarketing.*` Java.
- **Related known decisions**: None known. The plan should confirm whether any ADR governs the
  legacy editor's contentlet hydration or its planned retirement in favour of the new editor,
  since that affects how much effort to invest here.

## Root-Cause Hypothesis

Confirmed by local reproduction, not merely hypothesised. Three defects compound, all inside the
`autoexecute` IIFE (~lines 800–895 in `main`):

1. **The request is issued with no id.** On create, `inode` is empty (line 66:
   `String inode = inodeObj != null ? inodeObj.toString() : ""`), so the script calls
   `fetch('/api/v1/content/')`. That path never matches the JAX-RS route and returns **404 every
   time**.
2. **The response status is never checked.** The code calls `response.json()` without testing
   `response.ok`.
3. **The error handler has a typo.** The `.catch` assigns `binaryFieldContainer.innerHTMl` —
   lowercase `L` — instead of `innerHTML` (line 890 in `main`, 877 in `v25.07.10_lts_v16`). This
   writes an inert JS property, so the spinner markup is never replaced.

**Why it only breaks in some environments** — visibility depends entirely on the 404 body:

| 404 body | `response.json()` | Result |
|---|---|---|
| `404` (bare status code, from the `SIMPLE_ERROR_PAGES_FOR_BACKEND` branch in `html/error/custom-error-page.jsp`) | Succeeds — `JSON.parse("404")` is valid, and destructuring `{ entity }` off a number yields `undefined` without throwing | Component is created anyway; **works by accident**. What most dev environments see. |
| Empty, or the site's HTML 404 page | Throws `SyntaxError` | `.catch` → typo → **spinner forever** |

The customer's response headers confirm theirs returns HTML
(`Content-Type: text/html;charset=UTF-8`, `X-DOT-VanityUrl: 9cb6dbe2-…`, the same vanity serving
`/home/error-pages/cms404page` in their logs).

**Evidence from local reproduction** on `25.07.10_lts_v16` with a non-parseable 404 body, in
create mode: `spinners: 1, binaryEls: 0` — the `dotcms-binary-field` element is never created.
The typo captured live on the stuck container:

```json
{ "hasTypoProp": true,
  "typoValue": "<div class=\"callOutBox\">Error loading the binary field</div>",
  "realInnerHTML_stillSpinner": true }
```

The error message *is* generated; it just lands on a property that does not exist in the DOM.

> This closes an earlier caveat on the support ticket, which concluded the JSP bug "does not
> appear sufficient on its own" and proposed a stale-cached-JS theory. That theory is not needed:
> the earlier reproduction simply had a 404 body of `404`, which parses. **The JSP bug is
> sufficient on its own.**

[NEEDS CLARIFICATION: why does the customer's error page render the HTML `cms404page` at all?
The `isAPICall` guard in `custom-error-page.jsp` should short-circuit any `/api/` request before
the vanity is resolved, and it does so locally (empty body). Something in their environment
bypasses that guard. This does **not** block the fix — the fix removes the request entirely —
but it is an unexplained behavior worth confirming separately.]

## Fix Scope & Non-Goals *(mandatory)*

**In scope** — three changes, all in `edit_field.jsp`:

- Skip the fetch entirely when `inode` is empty, resolving with an empty contentlet instead.
  This removes the 404 at the source, so the shape of the error body stops mattering.
- Check `response.ok` and throw before calling `response.json()`, so a non-2xx response never
  reaches the JSON parser.
- Correct `innerHTMl` → `innerHTML`, so a genuine failure surfaces an error message instead of
  leaving the spinner running.
- Backport the same change to the `25.07.10_lts` branch.

**Explicitly out of scope / non-goals**:

- **Changing `custom-error-page.jsp` or the 404 body shape.** The open question above is real
  but separate; the fix must not depend on what any error page returns.
- **Rewriting or modernising the legacy editor**, the `autoexecute` IIFE, or migrating this
  field to the new editor. Progressive enhancement only, per Constitution Principle I.
- **Touching the `dotcms-binary-field` web component or any `core-web` code.** The component is
  correct; it is simply never instantiated.
- **The `SIMPLE_ERROR_PAGES_FOR_BACKEND` setting.** Tested on the customer's environment and it
  did not help; it is not part of the fix.
- **Delivery mechanism for the customer** (hotfix release, image layer, or OSGi plugin). Tracked
  separately; this spec covers the code fix in `main` plus the LTS backport.

## Regression Risk *(mandatory)*

- **Blast radius**: `edit_field.jsp` renders every field type in the legacy content editor, but
  the change is confined to the Binary-field branch (`autoexecute` IIFE). The two paths that
  must keep working are **create** (currently broken) and **edit** (currently working — the
  contentlet is still fetched and passed to the component when `inode` is present). The image
  editor hookup (`binaryField-open-image-editor-*` listener) and the `valueUpdated` handler for
  FileAsset base types live inside the same `.then`, so both must still run in create mode.
- **Backward compatibility**: No API, DB, ES mapping, or serialized-state change. Not
  rollback-unsafe. The component receives `{}` instead of `undefined` on create — behaviorally
  equivalent, since the previous "working" path in masked environments also produced `undefined`.
- **Data considerations**: None. No existing data is bad; nothing to migrate or repair.

## Acceptance & Verification *(mandatory)*

- **AC-001**: With the reproduction steps above, in an environment whose 404 body is **not**
  valid JSON, creating new content of a type with a Binary field renders a usable Binary field —
  no spinner. Measured as `spinners: 0, binaryEls: 1` (baseline before the fix:
  `spinners: 1, binaryEls: 0`).
- **AC-002**: No request to `/api/v1/content/` is issued when `inode` is empty — the 404 is
  eliminated at the source rather than handled.
- **AC-003**: Editing existing content with a Binary field continues to work; the component
  still hydrates from the fetched contentlet. Measured as `spinners: 0, binaryEls: 1`.
- **AC-004**: A genuine failure (non-2xx with a valid inode) replaces the spinner with the
  error message rather than leaving it spinning — i.e. the `.catch` has an observable effect.
- **AC-005**: Creating a FileAsset-base-type content still propagates the file name to the
  `title` / `fileName` fields on upload (the `valueUpdated` handler is unaffected).
- **AC-006**: The fix is verified in an environment where the 404 body is non-parseable, **not**
  only in one where it returns `404` — otherwise the verification is vacuous, which is exactly
  how this defect escaped detection.
- **AC-007**: Fix landed in `main` and backported to `25.07.10_lts`.

**Verification method**:

The local reproduction environment already exists and should be reused:
`docker-compose.ticket-binary.yml` (in the `dotcms-notes` working tree) runs
`dotcms/dotcms:25.07.10_lts_v16` on port 8080 and bind-mounts the patched JSP over the
container's copy; commenting out that volume line restores the buggy behavior, giving a
before/after on the same instance.

⚠ **TDD gate needs an explicit developer decision (Constitution Principle V).** This is inline
JavaScript inside a JSP: there is no existing unit-test harness that covers it, and no Jest or
Spectator spec can reach it. The plan MUST record which of these is chosen, and if no automated
test is written, the developer must say so explicitly and explain why — silence is not consent:

1. A Playwright E2E spec covering create-content-with-binary-field, which would need a way to
   force the non-parseable 404 body so the test is meaningful rather than vacuous.
2. A documented manual verification procedure against the repro environment above, capturing
   the `spinners` / `binaryEls` measurements for both create and edit.
3. Extracting the inline script to a testable unit — likely out of proportion for a
   three-line fix on a legacy surface, and in tension with the non-goals above.

## Assumptions

- The legacy Dojo/JSP editor remains supported for the foreseeable future; the existence of the
  Edit Content Beta editor does not make fixing this path unnecessary. The reporting customer
  explicitly declined the beta editor as a workaround.
- Passing `{}` rather than `undefined` as the contentlet on create is safe for the
  `dotcms-binary-field` component. This is supported by the fact that masked environments have
  always passed `undefined` (from destructuring `{ entity }` off the number `404`) and the
  component behaved correctly — but it should be confirmed during planning.
- The fix applies cleanly to `main`: `edit_field.jsp` is byte-identical across
  `25.07.10_lts_v10`, `25.07.10_lts_v16` and `main` (verified by diff), so this is not a
  regression and no version-specific divergence is expected.
