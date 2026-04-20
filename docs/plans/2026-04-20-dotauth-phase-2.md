# dotAuth Phase 2 — Native Portlet Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use `superpowers-extended-cc:executing-plans` to implement this plan task-by-task.

**Goal:** Replace the generic-Apps YAML editor for OAuth with a purpose-built `dotAuth` portlet, rename the AppSecrets key `dotOAuth → dotAuth`, and surface the SYSTEM_HOST fallback as a first-class inheritance model in the UI.

**Architecture:**
- Backend: drop `dotOAuth.yml`; new REST resource `/api/v1/dotauth` delegates straight to `AppsAPI`. `SYSTEM_HOST` is the sentinel that routes writes to `APILocator.systemHost()`. Runtime OAuth stack is untouched.
- Frontend: new Nx library `libs/portlets/dot-auth` following the `dot-tags` pattern (Shell → List + SignalStore → Dialog). A pinned "All Sites (system default)" row represents SYSTEM_HOST; per-site rows carry a status tag `Site override | Inherits from System | Not configured`. Inheritance shown as a PrimeNG `p-tag` + banner in the edit dialog.

**Tech Stack:** Java 11-syntax / Maven / JAX-RS (backend); Angular 19+ standalone, NgRx SignalStore, PrimeNG (`p-table`, `p-tag`, `p-password`, `p-select`, `p-inputSwitch`, `p-fieldset`, `p-message`, `DialogService`, `p-confirmDialog`), Tailwind utilities (frontend); Nx generator tooling.

**Reference:**
- `dotAuth-phase-2.md` — design doc (root of the repo).
- `core-web/libs/portlets/dot-tags/**` — canonical CRUD portlet.
- `core-web/libs/portlets/CLAUDE.md` — post-gen checklist.

---

## Frontend component strategy (existing Angular/PrimeNG pieces)

| Concern | Component / util |
|---|---|
| List table | `p-table` (lazy off — the list is short; pagination not needed) |
| System row pinning | `frozenValue` input on `p-table`, or a static header `<tr>` above `pTemplate="body"` (mirrors the dot-tags loading-row approach) |
| Status badge | `p-tag` with three severities: `success` (configured / site override), `info` (inherits from system), `secondary` (not configured) |
| Edit dialog | `DialogService.open(...)` — same wrapper dot-tags uses (`width: '700px'`, `closable: true`, `closeOnEscape: true`) |
| Confirm Clear | `p-confirmDialog` (shared singleton per portlet — dot-tags pattern) |
| Form field grouping | `p-fieldset` (three sections: Provider, Endpoints, Roles & Scopes) |
| Enabled / backend / frontend toggles | `p-inputSwitch` |
| Provider type select | `p-select` with options `OIDC` / `OAuth2` |
| Client secret input | `p-password` with `[feedback]="false"` and mask handling (`****` sentinel ⇒ don't touch) |
| Scopes / URLs / roles | `pInputText` |
| Inheritance banner | `p-message severity="info"` at top of edit dialog body |
| Search over site list | `p-iconField` + `pInputText` (copy the exact wrapper from `dot-tags-list.component.html`) |
| i18n | `DotMessagePipe` (`| dm`) — all user-facing text |
| HTTP / error handling | `DotHttpErrorManagerService` inside the store |

No new shared UI library work is needed — everything above is already in use elsewhere in the admin.

---

## Task 0: Commit design doc + plan

**Files:** working tree only.

**Step 1:** Confirm you are on `feat/oauth-core-migration` and the tree is clean apart from the design doc / plan.

Run: `git status`
Expected: `dotAuth-phase-2.md`, `docs/plans/2026-04-20-dotauth-phase-2.md`, `docs/plans/2026-04-20-dotauth-phase-2.md.tasks.json` untracked; `.mvn/maven-build-cache-config.xml` modified; no other drift.

**Step 2:** Commit the design doc + plan so later diffs stay clean. **Do NOT** include the Maven cache config change in this commit — leave it alone for now.

```bash
git add dotAuth-phase-2.md docs/plans/2026-04-20-dotauth-phase-2.md docs/plans/2026-04-20-dotauth-phase-2.md.tasks.json
git commit -m "docs(auth): add dotAuth phase-2 design doc and implementation plan"
```

**Note:** No intermediate build here. Build verification happens once at Task 16 per user preference.

---

## Task 1: Create `DotAuthConstants` and repoint `OAuthConstants.APP_KEY`

**Files:**
- Create: `dotCMS/src/main/java/com/dotcms/auth/dotAuth/DotAuthConstants.java`
- Modify: `dotCMS/src/main/java/com/dotcms/auth/providers/oauth/OAuthConstants.java:15`
- Modify: `dotCMS/src/test/java/com/dotcms/auth/providers/oauth/OAuthConstantsTest.java`

**Step 1: Write the failing test.** Update `OAuthConstantsTest` so `APP_KEY` is asserted to equal `"dotAuth"`.

**Step 2: Run the test, verify it FAILs.**
Run: `./mvnw test -pl :dotcms-core -Dtest=OAuthConstantsTest`
Expected: FAIL with `expected:<"dotAuth"> but was:<"dotOAuth">`.

**Step 3: Create `DotAuthConstants`:**
```java
package com.dotcms.auth.dotAuth;

/**
 * Constants for the {@code dotAuth} App/portlet. This is the single source of
 * truth for the AppSecrets key used by the OAuth runtime and by the dotAuth
 * portlet's REST layer.
 */
public final class DotAuthConstants {

    /** AppSecrets key under which the dotAuth portlet stores OAuth config. */
    public static final String APP_KEY = "dotAuth";

    private DotAuthConstants() {}
}
```

**Step 4:** In `OAuthConstants.java`, replace the `APP_KEY` literal with a reference to `DotAuthConstants.APP_KEY` and update the adjacent comment so it no longer says "core uses dotOAuth". Keep the constant `public static final String` so existing call-sites (e.g., `OAuthAppConfig.loadSecrets`) continue to compile unchanged.

**Step 5:** Also update the javadoc in `OAuthAppConfig.java:17` and the `// Secret keys (must match dotOAuth.yml params)` comment at `OAuthAppConfig.java:26` to read `dotAuth`.

**Step 6: Rerun the test. Verify it passes.**
Run: `./mvnw test -pl :dotcms-core -Dtest=OAuthConstantsTest`
Expected: PASS.

**Step 7: Commit.**
```bash
git add dotCMS/src/main/java/com/dotcms/auth/dotAuth/DotAuthConstants.java \
        dotCMS/src/main/java/com/dotcms/auth/providers/oauth/OAuthConstants.java \
        dotCMS/src/main/java/com/dotcms/auth/providers/oauth/OAuthAppConfig.java \
        dotCMS/src/test/java/com/dotcms/auth/providers/oauth/OAuthConstantsTest.java
git commit -m "refactor(auth): rename AppSecrets key dotOAuth -> dotAuth"
```

---

## Task 2: Delete `dotOAuth.yml`

**Files:**
- Delete: `dotCMS/src/main/resources/apps/dotOAuth.yml`

**Step 1:**
Run: `git rm dotCMS/src/main/resources/apps/dotOAuth.yml`

**Step 2:** Sanity-grep — no YAML reference to `dotOAuth` should remain:
Run (Grep tool): pattern `dotOAuth`, glob `**/*.yml`
Expected: zero matches.

**Step 3: Commit.**
```bash
git commit -m "chore(auth): remove dotOAuth apps YAML; portlet is the sole editor"
```

---

## Task 3: REST DTOs

**Files:**
- Create: `dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/DotAuthSiteStatus.java`
- Create: `dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/DotAuthSitesView.java`
- Create: `dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/DotAuthConfigView.java`
- Create: `dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/DotAuthConfigForm.java`

**Step 1:** Enum `DotAuthSiteStatus` with values `SITE_OVERRIDE`, `INHERITED`, `NOT_CONFIGURED`.

**Step 2:** `DotAuthSitesView` — the `GET /sites` envelope:
```java
public final class DotAuthSitesView {
    public final SystemView system;
    public final List<SiteRowView> sites;
    // nested SystemView { boolean configured; }
    // nested SiteRowView { String hostId; String hostName; DotAuthSiteStatus status; }
}
```

**Step 3:** `DotAuthConfigView` — `GET /sites/{hostId}` response:
```java
public final class DotAuthConfigView {
    public final String hostId;       // real id, or "SYSTEM_HOST" sentinel
    public final boolean configured;  // true when row exists for THIS hostId
    public final boolean inherited;   // true when hostId has no row but system does
    public final Map<String,Object> values; // field->value, clientSecret masked as "****"
}
```

**Step 4:** `DotAuthConfigForm` — `PUT /sites/{hostId}` body. Use a `Map<String,Object> values` rather than a pile of nullable fields — the runtime already stores untyped `Secret`s. Mark secrets that come in as `"****"` for preservation semantics in the resource.

**Step 5: Commit.**
```bash
git add dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/
git commit -m "feat(auth): add dotAuth REST DTOs"
```

---

## Task 4: `DotAuthResource` — happy-path endpoints

**Files:**
- Create: `dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/DotAuthResource.java`

**Step 1: Scaffolding.**
- `@Path("/v1/dotauth")`, class annotated with `@Produces(APPLICATION_JSON)`.
- Inject `WebResource` + `AppsAPI` via constructor (follow the pattern in `com/dotcms/rest/api/v1/apps/AppsResource.java`).
- User + permission check: require CMS admin (`new WebResource.InitBuilder(webResource).requiredBackendUser(true).requiredPortlet("dotAuth").init(...)`). Verify the exact builder flags against `AppsResource` — copy them verbatim.

**Step 2:** Implement `GET /sites`:
- Pull all sites via `APILocator.getHostAPI().findAll(user, false)`; exclude archived and the system host from the per-site list.
- `appKeysByHost()` returns `Map<String, Set<String>>` (host id → set of configured app keys). Call once; use it to decide `SITE_OVERRIDE` per site.
- `system.configured = appKeysByHost().getOrDefault(systemHost.getIdentifier(), Set.of()).contains(DotAuthConstants.APP_KEY)`.
- Per site: `SITE_OVERRIDE` when the set contains `dotAuth`; else `INHERITED` when `system.configured`; else `NOT_CONFIGURED`.

**Step 3:** Implement `GET /sites/{hostId}`:
- Sentinel: if `"SYSTEM_HOST".equals(hostId)` → use `APILocator.systemHost()`. Otherwise `APILocator.getHostAPI().find(hostId, user, false)`.
- Use `appsAPI.getSecrets(APP_KEY, fallbackOnSystemHost=true, host, user)` to retrieve the map. Determine `configured` (host has its own row) with `appsAPI.getSecrets(APP_KEY, false, host, user).isPresent()`.
- `inherited = !configured && !host.isSystemHost() && systemHasOwnRow`.
- Build a stable `values` map using `OAuthAppConfig`'s `KEY_*` names. For `clientSecret`, emit `"****"` when present, omit otherwise.

**Step 4:** Implement `PUT /sites/{hostId}`:
- Resolve host the same way (including sentinel).
- Load existing secrets-for-this-host-only (`fallbackOnSystemHost=false`). For each incoming value:
  - If the key is `clientSecret` and incoming value equals `"****"` → keep the existing stored value.
  - Otherwise replace.
- Build a new `AppSecrets` via `AppSecrets.Builder().withKey(APP_KEY).withHiddenSecret("clientSecret", ...).withSecret(...)` mirroring `AppsResource`. Call `appsAPI.saveSecrets(appSecrets, host, user)`.

**Step 5:** Implement `DELETE /sites/{hostId}`:
- `appsAPI.deleteSecrets(APP_KEY, host, user)`.
- Return 204.

**Step 6: Commit.**
```bash
git add dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/DotAuthResource.java
git commit -m "feat(auth): add DotAuthResource REST endpoints"
```

---

## Task 5: Register the REST package

**Files:**
- Modify: `dotCMS/src/main/java/com/dotcms/rest/config/DotRestApplication.java`

**Step 1:** In the `packages(...)` list, add `"com.dotcms.auth.dotAuth.rest"`.
**Step 2:** In `CORE_REST_PATHS`, add `"/v1/dotauth"`.

**Step 3: Build and run the existing REST discovery tests to confirm wiring:**
Run: `./mvnw test -pl :dotcms-core -Dtest='com.dotcms.rest.config.**'`
Expected: PASS.

**Step 4: Commit.**
```bash
git add dotCMS/src/main/java/com/dotcms/rest/config/DotRestApplication.java
git commit -m "feat(auth): wire /v1/dotauth into DotRestApplication"
```

---

## Task 6: `portlet.xml` entry

**Files:**
- Modify: `dotCMS/src/main/webapp/WEB-INF/portlet.xml`

**Step 1:** After the `tags` portlet block (`portlet.xml:469-474`), insert:
```xml
<portlet>
    <portlet-name>dotAuth</portlet-name>
    <display-name>dotAuth</display-name>
    <portlet-class>com.dotcms.spring.portlet.PortletController</portlet-class>
    <portlet-url>/dotAuth</portlet-url>
</portlet>
```

**Step 2: Commit.**
```bash
git add dotCMS/src/main/webapp/WEB-INF/portlet.xml
git commit -m "feat(auth): register dotAuth portlet in portlet.xml"
```

---

## Task 7: Language properties

**Files:**
- Modify: `dotCMS/src/main/webapp/WEB-INF/messages/Language.properties`

**Step 1:** Add the portlet title (exact case — this drives the page header):
```
com.dotcms.repackage.javax.portlet.title.dotAuth=dotAuth
```

**Step 2:** Add `dotauth.*` keys. Keep them prefix-consistent so the portlet doesn't leak naming into shared i18n:

```
dotauth.header=dotAuth
dotauth.subheader=OAuth / OIDC configuration per site, with a system-wide default.
dotauth.search.placeholder=Search sites
dotauth.table.header.site=Site
dotauth.table.header.status=Status
dotauth.table.header.actions=Actions
dotauth.row.system.label=All Sites (system default)
dotauth.status.site-override=Site override
dotauth.status.inherited=Inherits from System
dotauth.status.not-configured=Not configured
dotauth.status.configured=Configured
dotauth.action.edit=Edit
dotauth.action.clear=Clear
dotauth.action.configure=Configure
dotauth.confirm.clear.system.header=Clear system default?
dotauth.confirm.clear.system.message=This removes the OAuth configuration used by every site that is inheriting from System. Inheriting sites will show "Not configured".
dotauth.confirm.clear.site.header=Clear site override?
dotauth.confirm.clear.site.message=The site will revert to inheriting the System default configuration.
dotauth.banner.inheriting=This site is inheriting the System default. Saving will create a site-specific override.
dotauth.banner.system=These settings apply to every site that does not have its own override.
dotauth.dialog.header.system=All Sites — system default
dotauth.dialog.header.site=Edit: {0}
dotauth.fieldset.provider=Provider
dotauth.fieldset.endpoints=Endpoints
dotauth.fieldset.roles=Roles & Scopes
dotauth.field.enabled=Enabled
dotauth.field.enableBackend=Protect the admin console
dotauth.field.enableFrontend=Protect front-end logins
dotauth.field.providerType=Provider type
dotauth.field.providerType.oidc=OpenID Connect (auto-discovery)
dotauth.field.providerType.oauth2=OAuth 2.0 (manual URLs)
dotauth.field.issuerUrl=Issuer URL
dotauth.field.clientId=Client ID
dotauth.field.clientSecret=Client Secret
dotauth.field.scopes=Scopes
dotauth.field.authorizationUrl=Authorization URL
dotauth.field.tokenUrl=Token URL
dotauth.field.userinfoUrl=Userinfo URL
dotauth.field.revocationUrl=Revocation URL
dotauth.field.logoutUrl=Logout URL
dotauth.field.groupsClaim=Groups Claim
dotauth.field.groupsUrl=Groups URL
dotauth.field.extraRoles=Extra Roles
dotauth.field.callbackUrl=Callback Host Override
```

Reuse the existing `Save` / `Cancel` / `Delete` keys — do NOT redefine them.

**Step 3: Commit.**
```bash
git add dotCMS/src/main/webapp/WEB-INF/messages/Language.properties
git commit -m "feat(auth): add dotAuth i18n keys"
```

---

## Task 8: Generate the Nx library

**Files:**
- Create: `core-web/libs/portlets/dot-auth/**` (generator output)

**Step 1:** Run the generator (from `core-web/`):
```bash
yarn nx generate @nx/angular:library --name=portlet \
  --directory=libs/portlets/dot-auth \
  --tags=type:feature,scope:dotcms-ui,portlet:auth \
  --prefix=dot --standalone --no-interactive
```

**Step 2: Post-generator fixes** (per `libs/portlets/CLAUDE.md`):
1. `core-web/tsconfig.base.json`: replace the generated alias with `"@dotcms/portlets/dot-auth/portlet": ["libs/portlets/dot-auth/src/index.ts"]`.
2. `libs/portlets/dot-auth/project.json` → `"name": "portlets-dot-auth-portlet"`.
3. `libs/portlets/dot-auth/jest.config.ts` → `displayName: 'portlets-dot-auth-portlet'`; ensure the transform has `isolatedModules: true`.
4. Delete `libs/portlets/dot-auth/README.md` and the generated `src/lib/portlet/` boilerplate.
5. Confirm `libs/portlets/dot-auth/tsconfig.spec.json` only has `module`, `target`, `types`.

**Step 3: Sanity-check the scaffold:**
Run: `yarn nx lint portlets-dot-auth-portlet`
Expected: no errors (scaffold is empty — this mostly catches mis-wired config).

**Step 4: Commit.**
```bash
git add core-web/libs/portlets/dot-auth core-web/tsconfig.base.json
git commit -m "feat(dot-auth): scaffold Nx library"
```

---

## Task 9: Data-access service

**Files:**
- Create: `core-web/libs/data-access/src/lib/dot-auth/dot-auth.service.ts`
- Modify: `core-web/libs/data-access/src/index.ts`
- Create (models): `core-web/libs/dotcms-models/src/lib/dot-auth/*.ts` — `DotAuthSiteRow`, `DotAuthSitesView`, `DotAuthConfigView`, `DotAuthStatus` (union `'SITE_OVERRIDE'|'INHERITED'|'NOT_CONFIGURED'`), `DotAuthConfigPayload`.

**Step 1:** Export models from `libs/dotcms-models/src/index.ts`.

**Step 2:** Service outline:
```ts
@Injectable({ providedIn: 'root' })
export class DotAuthService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/dotauth/sites';

  listSites()                      = () => this.http.get<{entity: DotAuthSitesView}>(this.base).pipe(pluck('entity'));
  getConfig(hostId: string)        = () => this.http.get<{entity: DotAuthConfigView}>(`${this.base}/${hostId}`).pipe(pluck('entity'));
  saveConfig(hostId: string, payload: DotAuthConfigPayload) = ...
  clearConfig(hostId: string)      = this.http.delete<void>(`${this.base}/${hostId}`);
}
```
(Sketch — use real method signatures, not arrow-field pseudocode.)

**Step 3:** Add a small spec file mirroring `dot-tags.service.spec.ts` to verify URL shape; use `HttpTestingController`.
Run: `yarn nx test data-access --testPathPattern=dot-auth`
Expected: PASS.

**Step 4:** Re-export from `libs/data-access/src/index.ts`.

**Step 5: Commit.**
```bash
git add core-web/libs/data-access core-web/libs/dotcms-models
git commit -m "feat(dot-auth): data-access service and models"
```

---

## Task 10: SignalStore

**Files:**
- Create: `core-web/libs/portlets/dot-auth/src/lib/dot-auth-list/store/dot-auth-list.store.ts`

**Step 1:** State:
```ts
interface DotAuthListState {
  system: { configured: boolean };
  sites: DotAuthSiteRow[];
  filter: string;
  status: 'init' | 'loading' | 'loaded' | 'error';
}
```

**Step 2:** Methods:
- `loadSites()` — calls `DotAuthService.listSites()`, `catchError → httpErrorManager.handle → EMPTY`, on error set `status: 'loaded'` (keep UI usable).
- `saveSite(hostId, payload)` — `saveConfig()` then `loadSites()`.
- `clearSite(hostId)` — `clearConfig()` then `loadSites()`.
- `setFilter(filter)`.
- Computed `filteredSites` — client-side filter by hostName (no backend search; list is tiny).

**Step 3:** `withHooks(onInit)` calls `loadSites()`. Wrap in `untracked()` per the portlet CLAUDE.md.

**Step 4:** Store spec following `dot-tags-list.store.spec.ts` — cover happy path + error path for each method.
Run: `yarn nx test portlets-dot-auth-portlet --testPathPattern=store`
Expected: PASS.

**Step 5: Commit.**
```bash
git add core-web/libs/portlets/dot-auth/src/lib/dot-auth-list/store
git commit -m "feat(dot-auth): list SignalStore"
```

---

## Task 11: List component (table + pinned system row)

**Files:**
- Create: `core-web/libs/portlets/dot-auth/src/lib/dot-auth-list/dot-auth-list.component.{ts,html,spec.ts}`

**Step 1:** Component shell — clone the imports/providers list from `dot-tags-list.component.ts` minus `SplitButton`/`TableCheckbox` (not needed). Add `TagModule` for status pills.

**Step 2:** Template — a single `p-table` with `[value]="store.filteredSites()"`, a static pinned row rendered in `pTemplate="caption"` OR — cleaner — use `frozenValue` with a single-element array containing a synthetic `SYSTEM_HOST` row and `frozenRows="1"`. Pick whichever PrimeNG version supports `frozenValue` on non-scrollable tables; if not available, use a `<tr>` in the header template above the body. The system row is keyed off `hostId === 'SYSTEM_HOST'`.

**Step 3:** Status cell — `p-tag`:
- `SITE_OVERRIDE` → `severity="success"`, label `dotauth.status.site-override`.
- `INHERITED` → `severity="info"`, label `dotauth.status.inherited`.
- `NOT_CONFIGURED` → `severity="secondary"`, label `dotauth.status.not-configured`.
- System row → `p-tag severity="success"` "Configured" when `store.system().configured` else `"Not configured"`.

**Step 4:** Action cell — `p-button` Edit (always) + `p-button` Clear (hidden when row is `NOT_CONFIGURED`). On the system row, Clear is shown only when `system.configured`. Use `severity="danger" [text]="true"` for Clear.

**Step 5:** `openEditDialog(row)` — `DialogService.open(DotAuthEditComponent, { width: '700px', data: { hostId, hostName }, closable: true, closeOnEscape: true, draggable: false, position: 'center' })`. On close with a non-null `values`, call `store.saveSite(hostId, { values })`.

**Step 6:** `confirmClear(row)` — `ConfirmationService.confirm(...)` with system-vs-site copy, `[style]="{ width: '500px' }"` on the `<p-confirmDialog>` template.

**Step 7:** Copy the empty-state + loading-skeleton blocks from `dot-tags-list.component.html` and adapt labels.

**Step 8:** Component spec — cover:
- System row renders pinned with "Not configured" / "Configured" tag.
- Site row renders correct tag for each of the three states.
- Clear is hidden on `NOT_CONFIGURED`.
- Clicking Edit opens the dialog with the right `data`.
- Confirming Clear calls `store.clearSite(hostId)`.

Run: `yarn nx test portlets-dot-auth-portlet`
Expected: PASS.

**Step 9: Commit.**
```bash
git add core-web/libs/portlets/dot-auth/src/lib/dot-auth-list
git commit -m "feat(dot-auth): list component with pinned system row"
```

---

## Task 12: Edit dialog (reactive form, fieldsets, secret mask)

**Files:**
- Create: `core-web/libs/portlets/dot-auth/src/lib/dot-auth-edit/dot-auth-edit.component.{ts,html,spec.ts}`

**Step 1:** On init, read `DynamicDialogConfig.data = { hostId, hostName }`. Inject `DotAuthService`; call `getConfig(hostId)` with `take(1)`. Build a `FormGroup` with all KEY_* fields (see `OAuthAppConfig.java`). Pre-populate from response `values` (includes inherited defaults when applicable).

**Step 2:** Render:
- If `hostId === 'SYSTEM_HOST'`: `p-message severity="info"` with `dotauth.banner.system`.
- Else if `response.inherited`: `p-message severity="info"` with `dotauth.banner.inheriting`.

**Step 3:** Wrap fields in three `p-fieldset` blocks per the i18n keys (`Provider`, `Endpoints`, `Roles & Scopes`). Provider fieldset has the three toggles (`enabled`, `enableBackend`, `enableFrontend`), `providerType` select, `issuerUrl`, `clientId`, `clientSecret` (`p-password [feedback]="false" [toggleMask]="true"`). Endpoints fieldset shows authorizationUrl/tokenUrl/userinfoUrl/revocationUrl/logoutUrl/callbackUrl. Roles fieldset: scopes/groupsClaim/groupsUrl/extraRoles.

**Step 4:** Conditional visibility — when `providerType === 'OIDC'`, gray out authorization/token/userinfo URLs (they're auto-discovered). Keep them editable but show a small hint. Do NOT hide them — admins sometimes override.

**Step 5:** `clientSecret` mask semantics: if the field value on open is `"****"`, keep the form control's initial value as `"****"`. On submit, include the field in the payload only if the user typed something different; otherwise emit `"****"` to preserve server-side.

**Step 6:** Validators: `required` on `clientId`, `clientSecret` when submitting an enabled config. `issuerUrl` required when `providerType === 'OIDC'`. `tokenUrl` + `authorizationUrl` + `userinfoUrl` required when `providerType === 'OAuth2'`. Use Angular built-in `Validators.required`; URL format validation is out of scope.

**Step 7:** Footer with Save + Cancel buttons. Save → `dialogRef.close({ values: form.getRawValue() })`. Cancel → `dialogRef.close()`.

**Step 8:** Component spec — verify secret mask preservation, inheritance banner, required-field behavior across provider types.
Run: `yarn nx test portlets-dot-auth-portlet --testPathPattern=edit`
Expected: PASS.

**Step 9: Commit.**
```bash
git add core-web/libs/portlets/dot-auth/src/lib/dot-auth-edit
git commit -m "feat(dot-auth): edit dialog with fieldsets and secret mask"
```

---

## Task 13: Shell + routes + top-level index

**Files:**
- Create: `core-web/libs/portlets/dot-auth/src/lib/dot-auth-shell/dot-auth-shell.component.ts`
- Create: `core-web/libs/portlets/dot-auth/src/lib/lib.routes.ts`
- Modify: `core-web/libs/portlets/dot-auth/src/index.ts`

**Step 1:** Shell — mirror `dot-tags-shell`. Template is `<dot-auth-list />`, host class `flex flex-col h-full min-h-0`.

**Step 2:** Routes:
```ts
export const dotAuthRoutes: Routes = [
  { path: '', component: DotAuthShellComponent }
];
```

**Step 3:** Re-export `dotAuthRoutes` from `src/index.ts`.

**Step 4: Commit.**
```bash
git add core-web/libs/portlets/dot-auth/src
git commit -m "feat(dot-auth): shell component and lib routes"
```

---

## Task 14: Register the route in the admin shell

**Files:**
- Modify: `core-web/apps/dotcms-ui/src/app/app.routes.ts:159-165`

**Step 1:** Add (next to `'tags'`):
```ts
{
    path: 'dotAuth',
    canActivate: [MenuGuardService],
    canActivateChild: [MenuGuardService],
    data: { reuseRoute: false },
    loadChildren: () =>
        import('@dotcms/portlets/dot-auth/portlet').then((m) => m.dotAuthRoutes)
},
```

**Step 2: Build, verify the admin bundle picks up the lazy chunk:**
Run: `yarn nx build dotcms-ui`
Expected: BUILD SUCCESS; new chunk appears in `dist/apps/dotcms-ui/` output summary.

**Step 3: Commit.**
```bash
git add core-web/apps/dotcms-ui/src/app/app.routes.ts
git commit -m "feat(dot-auth): register dotAuth route in app shell"
```

---

## Task 15: Layout placement (startup task)

**Files:**
- Create: `dotCMS/src/main/java/com/dotcms/startup/runonce/TaskNNNNNNAddDotAuthPortletToMenu.java`
- Modify: `dotCMS/src/main/java/com/dotmarketing/startup/StartupTasks.java` (or the current registry file — confirm)

**Step 1:** Model this task on `Task260320AddPluginsPortletToMenu` (or `Task260206AddUsagePortletToMenu`). Resolve the exact next task number by grepping the latest `Task2603*` numbers in `dotCMS/src/main/java/com/dotcms/startup/runonce/` and picking the next free one.

**Step 2:** Target layout: the `settings` layout's "Configuration" group (same group as `apps`). Copy the SQL-fragment structure from the reference task; only the portlet id (`dotAuth`) and layout-row id need to change.

**Step 3:** Register the task in the startup-tasks list.

**Step 4: Bring up `just test-integration-ide` once** and confirm the portlet appears in the Settings sidebar under the expected group.

**Step 5: Commit.**
```bash
git add dotCMS/src/main/java/com/dotcms/startup/runonce/TaskNNNNNNAddDotAuthPortletToMenu.java \
        dotCMS/src/main/java/...StartupTasks.java
git commit -m "feat(dot-auth): add portlet to default Settings layout"
```

---

## Task 16: Backend verification

**Step 1:** Full module build:
Run: `./mvnw install -pl :dotcms-core -DskipTests -Dmaven.build.cache.enabled=false`
Expected: BUILD SUCCESS.

**Step 2:** Targeted test pass:
Run: `./mvnw test -pl :dotcms-core -Dtest='com.dotcms.auth.**'`
Expected: PASS; includes `OAuthConstantsTest` asserting `dotAuth`.

**Step 3:** REST smoke (optional — only if you have a live dev instance):
```bash
curl -s -u admin@dotcms.com:admin -H 'Accept: application/json' \
     http://localhost:8080/api/v1/dotauth/sites | jq .
```
Expected: `{ "entity": { "system": { "configured": false }, "sites": [ ... ] } }`.

---

## Task 17: Frontend verification

**Step 1:** Lint:
Run: `yarn nx lint portlets-dot-auth-portlet`
Expected: no errors.

**Step 2:** Full unit tests:
Run: `yarn nx test portlets-dot-auth-portlet`
Expected: PASS.

**Step 3:** Production build:
Run: `yarn nx build dotcms-ui`
Expected: BUILD SUCCESS.

---

## Task 18: Manual end-to-end walkthrough

(From the design doc, locked in as the acceptance test.)

1. `just dev-run` → log in as admin.
2. Sidebar shows **dotAuth** under Settings; click it. Page header reads **dotAuth** (exact case).
3. Pinned "All Sites (system default)" row shows **Not configured**; `default` site shows **Not configured**.
4. Click Edit on the system row → fill Okta fields (`https://integrator-9422530.okta.com/oauth2/default`, client id, secret) → Save. System row → **Configured**; `default` row → **Inherits from System**.
5. Hit `http://localhost:8080/dotAdmin/` → redirects to Okta; login completes into dotCMS.
6. Click Edit on `default` → form pre-populates with inherited values, inheritance banner visible. Change a field → Save. `default` → **Site override**. `/dotAdmin/` still redirects to Okta using the override.
7. Reopen Edit on `default` → `clientSecret` field shows `****`. Save without touching it → stored secret preserved (login still works).
8. Clear on `default` → confirm → row reverts to **Inherits from System**. `/dotAdmin/` redirect still works.
9. Clear on system row → confirmation shows the "affects inheriting sites" warning → confirm → both rows show **Not configured**. `/dotAdmin/` shows native login.
10. Open the generic Apps list — `dotOAuth` is gone; `dotsaml-config` is present and untouched.

Record result ✅ / notes inline in the PR description.

---

## Task 19: Final cleanup + PR

**Step 1:** `git log --oneline feat/oauth-core-migration..HEAD` — verify the commit sequence reads cleanly (1 per task). Rebase/reword locally if needed (no force-push to shared branches).

**Step 2:** `git push -u origin feat/oauth-core-migration`.

**Step 3:** Open PR against `main` with body that summarises:
- What changed (constants rename, yaml deletion, REST resource, portlet, frontend lib).
- What did NOT change (OAuth runtime, SAML).
- The manual walkthrough results.
- Screenshots of the portlet (empty state, system configured + site inherits, site override, clear confirmation).

---

## Out of Scope (do NOT touch)

- `dotsaml-config` app / `DotIdentityProviderConfigurationImpl` / `DotSamlProxyFactory`.
- `OAuthWebInterceptor`, `OAuthAppConfig.config(...)`, `AutoLoginFilter` wiring, OIDC discovery, REST callback.
- Multi-protocol (`authMode`) field.
- LDAP / other auth protocols.
- Migration of any prior `dotOAuth` configurations (none exist — phase 1 unreleased).
