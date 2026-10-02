# Issue Resolution Specification: Configuration API gaps for the Angular Configuration portlet

**Feature Branch**: `37872-configuration-api-gaps`

**Created**: 2026-10-02

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#37872](https://github.com/dotCMS/core/issues/37872), [#37873](https://github.com/dotCMS/core/issues/37873), [#37874](https://github.com/dotCMS/core/issues/37874) (all part of epic [#34738](https://github.com/dotCMS/core/issues/34738); they block the Angular Configuration portlet [#37793](https://github.com/dotCMS/core/issues/37793))

**Input**: User description: "https://github.com/dotCMS/core/issues/37872 https://github.com/dotCMS/core/issues/37873 https://github.com/dotCMS/core/issues/37874, in one PR, because they are linked and small. Existing live clients must not break."

## Problem Statement *(mandatory)*

The Angular Configuration portlet (#37793) replaces the Dojo Configuration
screen and talks only to the v1 REST API under `/api/v1/configuration`. Three gaps in that API
stop it from reaching parity with the current screen:

1. **Wrong portlet gate (#37872).** The company configuration endpoints check access to the
   **Maintenance** portlet instead of the **Configuration** portlet. Because they also require
   the CMS Administrator role, and a CMS Administrator passes any portlet check, nobody is
   rejected today. The gate still states the wrong intent, and it would start rejecting the
   wrong users if the role requirement ever changed. Its OpenAPI documentation says a rejected request gets **403**
   when it actually gets **401**. The email-validation endpoint is documented only by the
   generator's default entry, and returns 500 when called with no body.
2. **Stored login backgrounds are hidden, and a save wipes them (#37873).** The login background
   can be one of 11 images shipped with dotCMS, stored as `/html/images/backgrounds/bg-N.jpg`;
   the starter ships with `bg-11.jpg`. Older installs may also hold a custom URL typed into the
   Dojo screen's free-text box. The v1 branding API only accepts `/dA/...` asset paths. It
   rejects a preset on save and returns `null` for any other stored value on read. Because a
   save replaces every branding field, a client that reads the branding, edits anything else
   and saves **silently clears** the background that was already set.
3. **No license text endpoint (#37874).** The Configuration screen shows the dotCMS Business
   Source License text. Only a JSP renders it today; no REST endpoint returns it. The text
   changes with each release (copyright year, Change Date), so the frontend must read it from
   the running backend rather than ship its own copy.

**Severity / Impact**:

- #37873 has the highest impact. Any save from the Angular screen wipes the login background on
  every instance that uses a bundled preset or a legacy custom URL, with no error shown. The
  background picker and Restore Defaults cannot work at all.
- #37874 blocks the License dialog. The dialog can be built against placeholder text until this
  lands.
- #37872 changes no one's access in this release. It makes the gate express the right permission
  and makes the documented error codes match what clients receive.

## Reproduction *(mandatory)*

**Environment**: dotCMS `main` (1.0.0-SNAPSHOT), any license level, any browser or HTTP client,
authenticated as a CMS Administrator unless noted.

**Steps to Reproduce**:

*Background presets (#37873):*

1. In the current Configuration screen, pick bundled background `bg-5` and save (or run the
   starter, which ships with `bg-11`).
2. `GET /api/v1/configuration/branding`.
3. `PUT /api/v1/configuration/branding` with the body from step 2, changing only `primaryColor`.
4. Separately, `PUT /api/v1/configuration/branding` with
   `"backgroundImage": "/html/images/backgrounds/bg-5.jpg"`.

*License text (#37874):*

5. Look for any REST endpoint that returns the license text shown on the Configuration screen's
   Licensing tab.

*Portlet gate (#37872):*

6. Read the access checks on the endpoints listed under Scope of Investigation.
7. `POST /api/v1/configuration/_validateCompanyEmail` with no body.

**Expected Behavior**:

- Step 2 returns `backgroundImage: "/html/images/backgrounds/bg-5.jpg"`.
- Step 3 saves the new color and keeps the background.
- Step 4 succeeds and stores the preset.
- Step 5 finds a read-only endpoint returning the license of the running build.
- Step 6: the endpoints require the CMS Administrator role and access to the Configuration
  portlet, and the documented rejection status matches the one returned.
- Step 7 returns 400 with a message saying the body is required.

**Actual Behavior**:

- Step 2 returns `backgroundImage: null`.
- Step 3 succeeds and **clears** the stored background.
- Step 4 returns 400 `backgroundImage must be a dotAsset path starting with /dA`.
- Step 5: no such endpoint exists.
- Step 6: the endpoints require the Maintenance portlet. The rejection is 401 with a `text/plain`
  body, while OpenAPI documents 403.
- Step 7 returns 500.

**Reproducibility**: Always.

## Scope of Investigation *(mandatory)*

- **Affected area**: REST API for company configuration (the Configuration portlet's backend).
- **Suspected surface**: modern `com.dotcms.*` code only:
  - `com.dotcms.rest.api.v1.company.CompanyResource`, covering `GET` and `PUT /branding`,
    `PUT /authentication`, `PUT /locale` and `POST /_regenerateKey`.
  - `CompanyBasicInfoForm`, which validates the write.
  - `CompanyConfigHelper` and `AbstractCompanyConfigView`, which map and document the read.
  - `com.dotcms.rest.api.v1.system.ConfigurationResource#validateEmail`, which serves
    `POST /_validateCompanyEmail`.
  - `com.dotcms.enterprise.LicenseUtil#getLicenseText`, the existing license reader.
- **Live clients that must keep working unchanged**:
  - The Dojo Configuration screen's Send Test Mail calls `POST /_validateCompanyEmail` with a
    JSON body (`company.jsp:93`). It relies on 200 for a valid address and on a 400 `message`
    for an invalid one.
  - The Postman `ConfigurationResource` collection calls `_validateCompanyEmail` with
    `application/json` and asserts the same 400 message and 200 responses.
  - The login page reads the background from `POST /api/v1/loginform` (`backgroundPicture`),
    which this change does not touch.
  - Any integration already calling the `/branding`, `/authentication`, `/locale` and
    `/_regenerateKey` endpoints, which have been released since #34672.
- **Related known decisions**: Configuration settings stay **CMS Administrator only**, as #37872
  decides.

## Root-Cause Hypothesis

- **Gate:** the v1 endpoints were written with `requiredPortlet("maintenance")`, copied from a
  sibling system endpoint. The admin shortcut in the portlet check hides the mistake.
  `WebResource` reports every failed role or portlet check as 401 `text/plain`, but the
  annotations were written assuming 403. `validateEmail` reads the form without a null check.
- **Backgrounds:** the `/dA` rule was added so that only uploaded assets can be used, which keeps
  arbitrary URLs off the anonymous login page. The rule did not account for the 11 bundled
  images that the legacy screen offers, or for values stored before the rule existed. The read
  path applies the same filter, which turns a full-replace save into a silent delete.
- **License:** the text was only ever needed by a server-rendered JSP, so no endpoint was built
  for it.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- Switch the endpoints listed above from the Maintenance portlet to `PortletID.CONFIGURATION`,
  keeping the CMS Administrator requirement.
- Reject a missing body on `validateEmail` with 400, as the sibling endpoints do.
- Correct the OpenAPI responses of all these endpoints to the status code actually returned.
  Give `_validateCompanyEmail` full OpenAPI documentation and a JSON `@Consumes`.
- Accept exactly the 11 bundled presets as `backgroundImage` on write. Return any stored
  background unchanged on read. On write, accept a `backgroundImage` equal to the value already
  stored, so that a read-then-save round trip never fails and never clears it.
- Add a read-only `GET /api/v1/configuration/license` that returns the running build's license
  text and the header values the License dialog shows.
- Regenerate and commit `openapi.yaml`.

**Explicitly out of scope / non-goals**:

- The legacy `/api/config/*` endpoints and the Dojo Configuration screen. They stay as they are
  while that screen is in use.
- The `configuration-beta` portlet id that #37793 registers. While these endpoints require the
  CMS Administrator role, naming it in the gate changes nothing, because a CMS Administrator
  passes every portlet check. It is added only if the role requirement is ever relaxed.
- `PUT /api/v1/configuration`, which sets runtime config properties, keeps its current gate.
- Changing the 401 rejection to 403. That behaviour belongs to `WebResource` and is shared by
  every REST endpoint.
- Changing the `/dA` rule for `loginScreenLogo` and `navBarLogo`.
- Adding the background image to the anonymous `GET /api/v1/configuration`. That endpoint has
  never exposed it, and the login page already gets it from `/api/v1/loginform`.
- Changing the full-replace semantics of `PUT /branding`.

## Regression Risk *(mandatory)*

- **Blast radius**:
  - The five `CompanyResource` endpoints and `_validateCompanyEmail`, including the Dojo Send
    Test Mail and the Postman collection that call the latter.
  - The login page reads its background through `/api/v1/loginform`, which this change does not
    touch.
- **Backward compatibility**: every change keeps existing live clients working:
  - **Gate:** no one gains or loses access. CMS Administrators pass with or without either
    portlet in their layout, and everyone else is rejected with 401, as today.
  - **Email validation:** valid and invalid addresses get the same 200 and 400 responses and
    the same messages. Every known caller already sends `application/json`, so declaring JSON
    rejects none of them. Only a missing body changes, from 500 to 400.
  - **Backgrounds:** the write accepts more values than before and rejects nothing that it
    accepts today. `/dA/...`, empty and absent values behave exactly as before. On read, a
    stored value that used to come back as `null` now comes back as stored. A blank value, and
    the `localhost` value that fresh databases store, still come back as `null`.
  - **License:** a new, purely additive endpoint.
  - No DB schema, ES mapping or stored-data change. The change is rollback-safe.
- **Data considerations**: none. Stored backgrounds start being returned instead of hidden, and
  saves stop clearing them. No repair is needed.
- **Release note**: when the Angular Configuration portlet replaces the Dojo screen, non-admin
  users who can edit these settings through the Configuration portlet today lose that access,
  because the v1 endpoints are CMS Administrator only. Nothing changes for them in this release.

## Acceptance & Verification *(mandatory)*

*Portlet gate (#37872):*

- **AC-001**: The endpoints `GET /branding`, `PUT /branding`, `PUT /authentication`,
  `PUT /locale`, `POST /_regenerateKey` and `POST /_validateCompanyEmail` require the CMS
  Administrator role and access to `PortletID.CONFIGURATION`. None of them references the
  `maintenance` portlet.
- **AC-002**: A CMS Administrator who is a backend user, with neither the Configuration nor the
  Maintenance portlet in their layout, receives 200 from each endpoint.
- **AC-003**: A backend user without the CMS Administrator role receives 401 from each endpoint,
  even with the Configuration portlet in their layout.
- **AC-004**: An anonymous request receives 401 from each endpoint.
- **AC-005**: Each endpoint's `@ApiResponse` annotations document 401 as the rejection status and
  no longer document 403. `_validateCompanyEmail` has `@Operation`, a JSON `@Consumes`, request
  and response schema documentation. The documented response matches today's body
  (`{"entity":"Ok"}`), which does not change.
- **AC-006**: `POST /_validateCompanyEmail` with no body returns 400 with a message saying the
  body is required. A valid address still returns 200, and an invalid one still returns 400 with
  the message `input does not match a valid e-mail pattern.`

*Login background (#37873):*

- **AC-007**: `PUT /branding` accepts each of `/html/images/backgrounds/bg-1.jpg` through
  `bg-11.jpg` as `backgroundImage` and persists it to `company.homeURL`.
- **AC-008**: `GET /branding`, and the response of every `PUT` that returns the company view,
  return the stored `backgroundImage` unchanged, whether it is a preset, a `/dA/...` path or a
  legacy custom URL. A blank or `localhost` stored value is returned as `null`.
- **AC-009**: A new `backgroundImage` that is not a preset, not a `/dA/...` path, not empty and
  not equal to the stored value is rejected with 400. The message names `backgroundImage` and
  both accepted forms. Rejected examples:
  - `bg-0.jpg`, `bg-12.jpg`, `bg-1-sm.jpg`, `bg-no-sm.jpg` under `/html/images/backgrounds/`;
  - a preset with surrounding whitespace, different letter case, a query string, a `#` fragment
    or percent-encoded characters;
  - `../bg-1.jpg`, an absolute `https://…/html/images/backgrounds/bg-1.jpg` and a
    protocol-relative `//…` URL.
- **AC-010**: A `PUT /branding` whose `backgroundImage` equals the value already stored is
  accepted and keeps it, whatever that value is. A GET → PUT round trip that changes only another
  field therefore keeps the background intact. The one exception is a stored `localhost`, which
  is read as `null` and saved back as blank. The login page treats the two the same, falling
  back to the default background (`top_inc.jsp:207`).
- **AC-011**: `/dA/...` values, and empty or absent values, behave exactly as before.
- **AC-012**: The `backgroundImage` `@Schema` descriptions on both the request form and the
  response view list the accepted values.

*License text (#37874):*

- **AC-013**: A new `GET /api/v1/configuration/license` returns a typed `ResponseEntityView`
  whose entity has:
  - `title`: "dotCMS Business Source License" followed by the version stated in the file, for
    example "dotCMS Business Source License 1.1";
  - `licensor`, `changeDate` and `changeLicense`: the values from the license file's header;
  - `text`: the full license text, as `LicenseUtil.getLicenseText()` returns it.
- **AC-014**: The endpoint returns 200 for any user who is a backend user, with no role, portlet
  or license-level requirement. A front-end-only user and an anonymous request receive 401. This
  is deliberately wider than the legacy Licensing tab, which requires the Configuration portlet,
  because the text is public.
- **AC-015**: When the license file cannot be read, for any reason, the endpoint still returns
  200: `text` holds the existing fallback text from `getLicenseText()`, `title` holds
  "dotCMS Business Source License", and the header fields are `null`.
- **AC-016**: The endpoint's `@Operation`, `@ApiResponse` and `@Schema` annotations match its
  actual return type.

*Shared:*

- **AC-017**: `openapi.yaml` is regenerated from the annotations and committed.
- **AC-018**: The existing Postman `ConfigurationResource` collection passes unchanged.

**Verification method**:

- **Code review** for AC-001. No request can show which portlet id is required, because a CMS
  Administrator passes every portlet check and everyone else fails the role check first. The
  behaviour the gate produces is covered by AC-002 to AC-004.
- **Unit test** (`:dotcms-core`, Mockito) for AC-015, where the license file is unreadable or
  missing. An integration test would have to replace the shared servlet context, which leaks
  into later tests in the suite.
- **Integration tests** in `CompanyResourceIntegrationTest` (already registered in
  `MainSuite1b`), covering:
  - AC-002 to AC-004;
  - AC-006;
  - AC-007 to AC-011, with a representative sample of the rejected values in AC-009;
  - AC-013 and AC-014.
  - Tests that call `_regenerateKey` or send test mail clean up after themselves and leave no
    state that other tests in the suite depend on.
- If the license endpoint gets its own test class, that class is registered in a `MainSuite*`
  suite.
- Run with
  `./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=CompanyResourceIntegrationTest -Dmaven.build.cache.enabled=false`,
  and confirm `Tests run: N` in the failsafe report.
- Run the Postman `ConfigurationResource` collection for AC-018.
- The OpenAPI check in CI confirms AC-017.

## Assumptions

- The 11 bundled background files and their paths stay as they are in
  `dotCMS/src/main/webapp/html/images/backgrounds/`.
- The license file keeps its `Licensor:`, `Change Date:`, `Change License:` and
  `Business Source License <version>` header lines. If a line is missing, its field is `null`;
  the endpoint does not fail.
- The license text is the same at every license level, so the endpoint needs no license check.
- No live client depends on `GET /branding` returning `null` for a stored background that is not
  a `/dA` path.
