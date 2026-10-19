# Issue Resolution Specification: Configuration API gaps for the Angular Configuration portlet

**Feature Branch**: `37872-configuration-api-gaps`

**Created**: 2026-10-02

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#37872](https://github.com/dotCMS/core/issues/37872), [#37873](https://github.com/dotCMS/core/issues/37873), [#37874](https://github.com/dotCMS/core/issues/37874). All three block the Angular Configuration portlet ([#37793](https://github.com/dotCMS/core/issues/37793), epic [#34738](https://github.com/dotCMS/core/issues/34738)).

**Input**: User description: "Fix #37872, #37873 and #37874 in one PR, because they are linked and small. Existing live clients must not break."

## Problem Statement *(mandatory)*

The Angular Configuration portlet uses only the v1 API under `/api/v1/configuration`. Three gaps
stop it from matching the current Dojo screen:

1. **Wrong portlet gate (#37872).** The company configuration endpoints check for the
   **Maintenance** portlet instead of **Configuration**. Nobody is rejected because of it today:
   the endpoints also require the CMS Administrator role, and an admin passes any portlet check.
   But the gate states the wrong permission. The docs also say a rejected call gets 403, when it
   gets 401. And `_validateCompanyEmail` returns 500 when it receives no body.
2. **Login backgrounds are hidden, and a save wipes them (#37873).** dotCMS ships 11 login
   backgrounds, stored as `/html/images/backgrounds/bg-N.jpg`; the starter uses `bg-11.jpg`.
   The branding API only accepts `/dA/...` paths. It rejects a preset on save and returns
   `null` for one on read. A save replaces every branding field, so reading the branding,
   changing a color and saving **silently clears the background**.
3. **No license endpoint (#37874).** Only a JSP shows the dotCMS Business Source License. The
   text changes each release, so the Angular dialog must read it from the running server.

**Severity / Impact**:

- **#37873 is the real bug.** Every Angular save would wipe the login background without any
  error, and the background picker can't work.
- **#37874** blocks the License dialog.
- **#37872** changes no one's access. It fixes the stated permission and the docs.

## Reproduction *(mandatory)*

**Environment**: dotCMS `main`, logged in as a CMS Administrator.

**Steps to Reproduce**:

1. Set the login background to `bg-5` in the current Configuration screen.
2. `GET /api/v1/configuration/branding`.
3. `PUT /api/v1/configuration/branding` with that body, changing only `primaryColor`.
4. `PUT /api/v1/configuration/branding` with
   `"backgroundImage": "/html/images/backgrounds/bg-5.jpg"`.
5. `POST /api/v1/configuration/_validateCompanyEmail` with no body.

**Expected Behavior**:

- Step 2 returns the `bg-5` path.
- Step 3 keeps the background.
- Step 4 succeeds.
- Step 5 returns 400.
- A REST endpoint returns the license text.

**Actual Behavior**:

- Step 2 returns `backgroundImage: null`.
- Step 3 clears the background.
- Step 4 returns 400 `backgroundImage must be a dotAsset path starting with /dA`.
- Step 5 returns 500.
- No license endpoint exists.

**Reproducibility**: Always.

## Scope of Investigation *(mandatory)*

- **Affected area**: company configuration REST API, all modern `com.dotcms.*` code:
  - `CompanyResource`: `GET`/`PUT /branding`, `PUT /authentication`, `PUT /locale`,
    `POST /_regenerateKey`.
  - `CompanyBasicInfoForm`, `CompanyConfigHelper`, `AbstractCompanyConfigView`: background
    validation and mapping.
  - `ConfigurationResource#validateEmail`: `POST /_validateCompanyEmail`.
  - `LicenseUtil#getLicenseText`: the existing license reader.
- **Live clients that must keep working**:
  - The Dojo screen's Send Test Mail and the Postman `ConfigurationResource` collection both
    call `_validateCompanyEmail` with JSON.
  - Any integration using the branding, authentication, locale or regenerate-key endpoints,
    released in #34672.
  - The login page, which reads the background from `/api/v1/loginform`. This change does not
    touch that endpoint.
- **Related known decisions**: these settings stay **CMS Administrator only** (#37872).

## Root-Cause Hypothesis

- **Gate:** `requiredPortlet("maintenance")` was copied from a sibling system endpoint. The admin
  shortcut hides it. `WebResource` returns 401 for every failed check, but the annotations
  assumed 403.
- **Backgrounds:** the `/dA` rule keeps arbitrary URLs off the anonymous login page, but it
  missed the bundled images and older stored values. The same rule on read turns a full-replace
  save into a silent delete.
- **License:** only a JSP ever needed the text.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- **Portlet gate:** require `PortletID.CONFIGURATION` instead of `maintenance`, keeping the CMS
  Administrator requirement. Document 401 instead of 403.
- **`_validateCompanyEmail`:** add OpenAPI docs and a JSON `@Consumes`, and return 400 for a
  missing body.
- **Login background:**
  - accept exactly the 11 bundled backgrounds;
  - return the stored value on read;
  - accept saving back the value already stored, so a round trip never fails or clears it.
- **License:** add `GET /api/v1/configuration/license`.
- **OpenAPI:** regenerate `openapi.yaml`.

**Explicitly out of scope / non-goals**:

- **Legacy code:** the `/api/config/*` endpoints and the Dojo screen.
- **The `configuration-beta` portlet id.** An admin passes every portlet check, so adding it
  changes nothing while the endpoints are admin-only.
- **`PUT /api/v1/configuration`** (runtime config properties) keeps its gate.
- **401 vs 403:** changing the status code itself; it is shared by every REST endpoint.
- **Logo fields:** the `/dA` rule for `loginScreenLogo` and `navBarLogo`.
- **Branding saves:** the full-replace behaviour of `PUT /branding`.

## Regression Risk *(mandatory)*

- **Blast radius**: the six endpoints above and their callers listed in Scope of Investigation.
- **Backward compatibility**: nothing that works today changes:
  - **Access:** no one gains or loses it.
  - **`_validateCompanyEmail`:** gives the same responses and messages. Its callers already
    send JSON. Only a missing body changes, from 500 to 400.
  - **Background write:** accepts more values and rejects nothing it accepts today.
  - **Background read:** returns stored values that it used to hide. Blank and the
    fresh-install `localhost` value still return `null`.
  - **License:** a new endpoint.
  - **Rollback:** no schema or data change, so the change is rollback-safe.
- **Data considerations**: none. Stored backgrounds are simply no longer hidden or cleared.
- **Release note**: once the Angular screen replaces Dojo, non-admins who can edit these
  settings through the Configuration portlet today lose that access. Nothing changes for them
  in this release.

## Acceptance & Verification *(mandatory)*

*Portlet gate (#37872):*

- **AC-001**: The six endpoints require the CMS Administrator role and `PortletID.CONFIGURATION`.
  None references `maintenance`.
- **AC-002**: A CMS Administrator with neither portlet in their layout gets 200 from each.
- **AC-003**: A non-admin backend user gets 401 from each, even with the Configuration portlet
  in their layout.
- **AC-004**: An anonymous request gets 401 from each.
- **AC-005**: The docs list 401, not 403. `_validateCompanyEmail` has `@Operation`, JSON
  `@Consumes`, and request and response schemas. Its response body stays `{"entity":"Ok"}`.
- **AC-006**: `_validateCompanyEmail` returns 400 with no body. A valid address still gets 200,
  and an invalid one still gets 400 `input does not match a valid e-mail pattern.`

*Login background (#37873):*

- **AC-007**: `PUT /branding` accepts `/html/images/backgrounds/bg-1.jpg` through `bg-11.jpg`
  and saves them.
- **AC-008**: `GET /branding` and the `PUT` responses return the stored `backgroundImage`
  unchanged. Blank and `localhost` return `null`.
- **AC-009**: Any other new value under that folder gets 400, and the message names the accepted
  values. Examples: `bg-12.jpg`, `bg-1-sm.jpg`, `../bg-1.jpg`, a different letter case, a query
  string, an absolute URL.
- **AC-010**: Saving back the stored `backgroundImage` is accepted, so a GET → PUT round trip
  keeps the background.
- **AC-011**: `/dA/...` and empty values behave as before.
- **AC-012**: The `@Schema` of `backgroundImage`, on both the request and the response, lists
  the accepted values.

*License (#37874):*

- **AC-013**: `GET /api/v1/configuration/license` returns `title` ("dotCMS Business Source
  License" plus the version in the file), `licensor`, `changeDate`, `changeLicense`, and the
  full `text`.
- **AC-014**: Any backend user gets 200. Front-end-only and anonymous users get 401.
- **AC-015**: If the file can't be read, the endpoint still returns 200 with the existing
  fallback text and `null` header fields.
- **AC-016**: The OpenAPI annotations match the real response type.

*Shared:*

- **AC-017**: `openapi.yaml` is regenerated and committed.
- **AC-018**: The Postman `ConfigurationResource` collection passes unchanged.

**Verification method**:

- **Integration tests** in `CompanyResourceIntegrationTest`, already in `MainSuite1b`: AC-002 to
  AC-004, AC-006 to AC-011, AC-013 and AC-014.
- **Unit test** for AC-015. An integration test would have to replace the shared servlet
  context, which leaks into later tests.
- **Code review** for AC-001. No request can show the portlet id, because admins pass every
  portlet check.
- **Postman** collection for AC-018, and the CI OpenAPI check for AC-017.

## Assumptions

- The 11 background files keep their current paths.
- The license file keeps its `Licensor:`, `Change Date:`, `Change License:` and version lines.
  A missing line gives a `null` field, not an error.
- No client depends on `GET /branding` returning `null` for a stored background.
