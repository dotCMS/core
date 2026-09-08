# Worked examples

Two illustrative plans — one backend, one frontend. Issue and PR numbers are **fictional**; they
show shape, level of detail, and tone, not real dotCMS history.

Both show the full-plan body only — the part inside `<details>`, which is exactly two sections,
**Summary** then **Test Cases**. Wrap it in the comment skeleton from
[`comment-format.md`](comment-format.md) before posting.

Note how each example carries its assumptions and its scope decisions *inside* the Summary and the
individual cases. There is no assumptions table, no per-issue index and no out-of-scope list to fall
back on, so anything a reviewer must know has to be visible in a case or in those two or three
sentences.

---

## Example A — Backend, two issues, one PR

PR #12345 changed a REST resource and the permission check behind it, closing two issues.

````markdown
## Summary

PR #12345 fixed two issues in the content-listing REST resource: #31904 (archived content leaked
into the default listing) and #31905 (a user without READ on a folder still saw its children in the
response). The change touches `ContentResource` and the permission filter it calls, so the plan
covers permissions, content version state, and the permission cache behind the listing. #31904's
acceptance criteria do not say whether an explicit `?archived=true` filter should still return
archived content; TC-002 assumes it should, and states that assumption in its expected result.
**None of these cases have been executed** — they are the manual pass for the post-merge build.

## Test Cases

```
- Test ID: TC-001
- Issues: #31904
- Test Name: Verify #31904 — archived content no longer appears in the default listing
- Risk: High
- Scenario: Happy Path
- Steps To Reproduce:
  1. Log in to the build under test as a back-end user with publish rights on the default site.
  2. Create a content type `QA Article` with a single text field `title`.
  3. Create two contentlets of that type, `Live One` and `To Archive`, and publish both.
  4. Archive `To Archive` from the content listing.
  5. Call `GET /api/content/_search` with the default parameters for that content type.
- Expected Result: The response contains `Live One` and does **not** contain `To Archive`.
  `resultsSize` counts only the unarchived contentlet.
```

```
- Test ID: TC-002
- Issues: #31904
- Test Name: Explicit archived filter still returns archived content
- Risk: Medium
- Scenario: Edge
- Steps To Reproduce:
  1. Using the same data as TC-001, call the same endpoint with `?archived=true`.
- Expected Result: The response contains `To Archive`. Per assumption A1, the explicit filter is
  unaffected by the fix — if it now returns empty, that is a regression, not the intended behavior.
```

```
- Test ID: TC-004
- Issues: #31905
- Test Name: Verify #31905 — folder children hidden from a user without READ
- Risk: Critical
- Scenario: Negative
- Steps To Reproduce:
  1. Create a folder `/qa-restricted/` on the default site and place two contentlets in it.
  2. Create a role `QA No Read` with no permissions on that folder, and a back-end user in it.
  3. Log in as that user on the build under test.
  4. Call `GET /api/content/_search` scoped to the default site.
- Expected Result: Neither contentlet under `/qa-restricted/` appears. The response is `200` with
  those items absent — not `403`, and not an empty result set for the whole site.
```

```
- Test ID: TC-005
- Issues: #31905
- Test Name: Unauthenticated request to the listing endpoint is rejected
- Risk: High
- Scenario: Negative
- Steps To Reproduce:
  1. From a shell with no session cookie or token, run:
     `curl -i "https://<build under test>/api/content/_search?query=%2BcontentType:QAArticle"`
- Expected Result: `HTTP/1.1 401` with an authentication-required body. No content is returned.
```

```
- Test ID: TC-006
- Issues: #31905
- Test Name: Permission cache reflects a revoked role within the same session
- Risk: High
- Scenario: Edge
- Steps To Reproduce:
  1. Grant `QA No Read` READ on `/qa-restricted/` and confirm as that user that the listing now
     returns both contentlets.
  2. Without logging that user out, revoke the READ permission as an admin.
  3. Re-run the listing call as the original user.
- Expected Result: The two contentlets disappear from the response on the first call after
  revocation — the permission cache does not serve the stale allow.
````

---

## Example B — Frontend, one issue

PR #12400 fixed spacing and keyboard behavior on a boolean field in the content form, closing #31910.
```

````markdown
## Summary

PR #12400 fixed #31910: the "Show on list" boolean field in the content form rendered with no space
between its checkbox and label, and the label was not clickable. The change is confined to one
component's template and stylesheet, so the plan is a UI pass — rendering, keyboard, focus,
responsive and i18n — plus a check that the field still saves correctly.
**None of these cases have been executed.**

## Test Cases

```
- Test ID: TC-001
- Issues: #31910
- Test Name: Verify #31910 — checkbox and label are visually separated and the label is clickable
- Risk: High
- Scenario: Happy Path
- Steps To Reproduce:
  1. Log in to the build under test as a back-end user.
  2. Open any content type that has a boolean field, or add one named `Show on list`.
  3. Create a new contentlet of that type to open the content form.
  4. Locate the `Show on list` field and click **the label text**, not the checkbox.
- Expected Result: There is visible space between checkbox and label, and clicking the label toggles
  the checkbox. The checked state changes on each label click.
```

```
- Test ID: TC-002
- Issues: #31910
- Test Name: Field is reachable and operable by keyboard
- Risk: Medium
- Scenario: Happy Path
- Steps To Reproduce:
  1. With the content form open, put focus in the field above `Show on list`.
  2. Press `Tab` until focus reaches the boolean field.
  3. Press `Space`.
- Expected Result: The field receives a visible focus ring in tab order, and `Space` toggles it.
  Focus does not skip the field or land on a non-interactive wrapper.
```

```
- Test ID: TC-003
- Issues: #31910
- Test Name: Value persists through save and reopen
- Risk: High
- Scenario: Happy Path
- Steps To Reproduce:
  1. Set `Show on list` to checked and save the contentlet.
  2. Navigate away from the form, then reopen the same contentlet.
- Expected Result: The field is still checked. Repeat with unchecked — it stays unchecked.
```

```
- Test ID: TC-004
- Issues: #31910
- Test Name: Layout holds at mobile and tablet widths
- Risk: Low
- Scenario: Boundary
- Steps To Reproduce:
  1. With the content form open, narrow the browser to a phone width, then a tablet width.
- Expected Result: Checkbox and label stay on one line where there is room, wrap cleanly where there
  is not, and never overlap. The field stays clickable at every width.
```

```
- Test ID: TC-005
- Issues: #31910
- Test Name: Label renders in a non-default language
- Risk: Low
- Scenario: Edge
- Steps To Reproduce:
  1. Switch the back-end user's language to a non-default language with a longer translation.
  2. Reopen the content form.
- Expected Result: The label resolves through the message bundle — no raw key such as
  `contenttypes.field.show.on.list` — and the longer string does not break the spacing fixed in
  TC-001.
````
```