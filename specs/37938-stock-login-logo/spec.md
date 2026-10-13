# Issue Resolution Specification: Configuration API returns the stock dotCMS login logo

**Feature Branch**: `37938-stock-login-logo`

**Created**: 2026-10-12

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#37938](https://github.com/dotCMS/core/issues/37938). It blocks the
**Use dotCMS Logo** button of the Angular Configuration portlet
([#37793](https://github.com/dotCMS/core/issues/37793), epic
[#34738](https://github.com/dotCMS/core/issues/34738)).

**Input**: User description: "Go with the simple path: return the stock logo when the instance
has it, otherwise say it is not available. Never create it."

## Problem Statement *(mandatory)*

The Login Screen Logo dialog of the Angular Configuration portlet has a **Use dotCMS Logo**
button. It puts the stock dotCMS logo back in the logo field; the logo changes when the admin
saves through `PUT /api/v1/configuration/branding`.

That endpoint only accepts a login logo that is a dotAsset path (`/dA/...`), and an empty logo
leaves the login page with no logo at all. So the button needs the `/dA/...` path of the stock
logo, and no API returns it.

The stock logo is not a file shipped with dotCMS. It is a dotAsset in each instance's data. The
starter data creates it with a fixed identifier, `a083e942-5fb0-47ef-9619-6f8f40593474`, and
sets it as the login logo. An admin who uploads their own logo keeps that dotAsset; only the
stored path changes. Instances that were not created from this starter, or where the dotAsset
was deleted, do not have it.

**Severity / Impact**: Medium. The **Use dotCMS Logo** button can't work. Nothing that works
today is broken: admins can still upload any logo, including the dotCMS one.

## Reproduction *(mandatory)*

**Environment**: dotCMS `main` created from the current starter, logged in as a CMS
Administrator.

**Steps to Reproduce**:

1. Upload a custom login logo in the Configuration screen and save.
2. Look for an API that returns the path of the stock dotCMS logo.
3. `PUT /api/v1/configuration/branding` with `loginScreenLogo` empty.
4. Open the login page.

**Expected Behavior**:

- Step 2: an endpoint returns `/dA/a083e942-5fb0-47ef-9619-6f8f40593474/asset/company_logo.png`.
- Saving that path brings the dotCMS logo back on the login page.

**Actual Behavior**:

- Step 2: no endpoint exists.
- Step 4: the login page shows no logo.

**Reproducibility**: Always.

## Scope of Investigation *(mandatory)*

- **Affected area**: company configuration REST API, `/api/v1/configuration`.
- **Suspected surface**: modern code only: `CompanyResource` and `CompanyConfigHelper` in
  `com.dotcms.rest.api.v1.company`. The lookup reads the dotAsset through the existing contentlet
  and permission APIs.
- **Related known decisions**: the company configuration endpoints require a backend user with
  the CMS Administrator role and the Configuration portlet (#37872). This endpoint uses the same
  check.

## Root-Cause Hypothesis

The v1 configuration API was built to read and save the stored values. It has no notion of a
default login logo, because the stock logo exists only as instance data and not in the product.
The Dojo screen never offered a way back to it, so no API was needed until the Angular design
added the button.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- **New endpoint** `GET /api/v1/configuration/dotcmsLogo`. It is read-only.
- **Found:** when the stock logo dotAsset exists and passes every check below, return 200 with
  `{ "entity": "/dA/<identifier>/asset/<file name>" }`.
- **Checks**, all required:
  - the identifier is the starter's stock logo identifier;
  - the dotAsset is published (live) in any language;
  - the anonymous user can read it, so the login page can load it;
  - its file is the stock dotCMS logo, matched by the file's sha256.
- **Path:** built from the dotAsset's actual file name.
- **Not found:** when any check fails, return 404 with a message saying the dotCMS logo is not
  available on this instance.
- **Lookup:** by identifier, not through the search index.
- **Access:** the same check as the other company configuration endpoints.
- **OpenAPI:** `@Operation` / `@Schema` for the 200, 401 and 404 responses; regenerate
  `openapi.yaml`.

**Explicitly out of scope / non-goals**:

- **Creating, recreating or republishing** the stock logo dotAsset. When it is missing, the
  endpoint returns 404.
- **Finding a stock logo stored under another identifier.**
- **Changing the company record.** The login logo changes only when the client saves through
  `PUT /branding`.
- **Changes to `PUT` / `GET /branding`**, `/image/company_logo`, the login page, or the Dojo
  Configuration screen.
- **The navbar logo** and **Restore Defaults**.
- **The frontend button**, which belongs to #37793.

## Regression Risk *(mandatory)*

- **Blast radius**: none outside the new endpoint. No existing class behaviour changes.
- **Backward compatibility**: a new, read-only endpoint. No existing endpoint, response or
  stored value changes. No schema, index or data change, so the change is rollback-safe.
- **Data considerations**: none. The endpoint only reads.

## Acceptance & Verification *(mandatory)*

- **AC-001**: On an instance with the starter's stock logo, `GET /api/v1/configuration/dotcmsLogo`
  returns 200 with
  `{ "entity": "/dA/a083e942-5fb0-47ef-9619-6f8f40593474/asset/company_logo.png" }`.
- **AC-002**: Saving the returned path as `loginScreenLogo` through `PUT /branding` succeeds,
  and `GET /branding` returns it.
- **AC-003**: Two calls return the same path.
- **AC-004**: A call leaves the stored login logo unchanged.
- **AC-005**: It returns 404 when the stock logo dotAsset does not exist.
- **AC-006**: It returns 404 when the dotAsset exists but is not published.
- **AC-007**: It returns 404 when the anonymous user can't read the dotAsset.
- **AC-008**: It returns 404 when the dotAsset's file is not the stock dotCMS logo.
- **AC-009**: The returned path uses the dotAsset's actual file name.
- **AC-010**: A CMS Administrator without the Configuration portlet in their layout gets 200.
  A non-admin backend user and an anonymous request get 401.
- **AC-011**: The OpenAPI annotations describe the 200, 401 and 404 responses and match the real
  response type, and `openapi.yaml` is regenerated and committed.

**Verification method**:

- **Integration tests** in `CompanyResourceIntegrationTest`, already registered in `MainSuite1b`:
  AC-001 to AC-004 and AC-010. The test database is loaded from the same starter, so the stock
  logo is present.
- **Unit tests** in `CompanyConfigHelperTest`, with the lookups mocked: AC-005 to AC-009.
  Archiving, unpublishing or editing the shared starter dotAsset in an integration test would
  affect every later test that reads the login logo.
- **CI OpenAPI check** for AC-011.

## Assumptions

- The starter keeps the stock logo under identifier `a083e942-5fb0-47ef-9619-6f8f40593474`, with
  file `company_logo.png` and sha256
  `6f59e8d34e7b67b14948f228110f14ef13bf2f19d949dc19ea3da08cd7ef74d0`. Both values are read from
  `starter-empty_20260331`, the version pinned in `parent/pom.xml`.
- The frontend (#37793) calls the endpoint when the dialog opens and hides the button on any
  error, so a 404 or a failure never blocks the dialog or Save.
