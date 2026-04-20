# Phase 2 — Rebrand `dotOAuth` → `dotAuth` with a native portlet

## Context

Phase 1 (commit `a95d7f8f14`) shipped the OAuth/OIDC plugin into core under app key `dotOAuth` and a generic-Apps YAML config. Phase 2 narrows the editing surface: the OAuth config gets edited through a **dedicated dotCMS portlet** ("dotAuth"), not the generic Apps UI. SAML is **out of scope** for this phase.

Why a portlet instead of the generic Apps UI:
- Per-site OAuth config benefits from purpose-built UX (provider presets, callback URL preview, validation hints), which the generic Apps form can't deliver.
- A portlet shows up as a first-class navigation entry instead of being buried under "Apps".
- Removing the YAML descriptor means the same key doesn't render twice (Apps card + portlet).

**Verified earlier in this session** (still holds):
- `AppsAPIImpl.saveSecrets / getSecrets / deleteSecrets / filterSitesForAppKey / appKeysByHost` are descriptor-agnostic — they read/write the secret store directly. So `dotAuth` can live in AppSecrets without registering a YAML.
- The dot-tags portlet is the canonical reference for new CRUD portlets (`core-web/libs/portlets/dot-tags/`); generator command and post-fixes are in `core-web/libs/portlets/CLAUDE.md`.
- New portlets register in `dotCMS/src/main/webapp/WEB-INF/portlet.xml` via `com.dotcms.spring.portlet.PortletController`.
- The Apps list click handler is `goToApp(key)` at `core-web/apps/dotcms-ui/src/app/portlets/dot-apps/dot-apps-list/dot-apps-list.component.ts:76-78` — clean place to drop the legacy `dotOAuth` card if any breadcrumbs of it survive.

**Phase-1 surface that stays untouched:** the OAuth runtime — `OAuthAppConfig`, `OAuthWebInterceptor`, the REST callback, viewtools, AutoLoginFilter wiring, OIDC discovery. Only the storage key name and the editing surface change.

**SAML (`dotsaml-config`) is not modified.** No dual-key reads, no startup import, no enablement gate changes.

## Design

```
                                       ┌──────────────────────┐
Admin UI                               │ Apps list            │
                                       │   ├── SSO - SAML     │  ← unchanged
                                       │   └── ... others     │
                                       └──────────────────────┘
                                       ┌──────────────────────────────────┐
Sidebar / portlet picker               │ dotAuth portlet                  │
                                       │   ┌──────────────────────────┐   │
                                       │   │ All Sites (system)       │   │  ← pinned row
                                       │   │   status: Configured     │   │
                                       │   ├──────────────────────────┤   │
                                       │   │ default                  │   │
                                       │   │   status: Inherits       │   │
                                       │   ├──────────────────────────┤   │
                                       │   │ marketing.example.com    │   │
                                       │   │   status: Site override  │   │
                                       │   └──────────────────────────┘   │
                                       │   - edit dialog (OAuth fields)   │
                                       └─────────┬────────────────────────┘
                                                 │ reads/writes via AppsAPI
                                                 ▼
                                AppSecrets key = "dotAuth" (per host, plus SYSTEM_HOST)

Runtime:
  OAuthWebInterceptor ─► OAuthAppConfig
                           getSecrets("dotAuth", true, host, user)
                                                  ^^^^ fallback to SYSTEM_HOST
                           enabled = config.enabled (existing flag)
```

No YAML descriptor is registered for `dotAuth`. The portlet is the sole editor.

### System host as "global default"

dotCMS's AppsAPI already supports site-scoped secrets with a SYSTEM_HOST fallback — `getSecrets(key, fallbackOnSystemHost=true, host, user)`. `OAuthAppConfig.loadSecrets` already uses this flag, so the runtime semantics are:

- Site has its own `dotAuth` row → that wins.
- Site has no row, SYSTEM_HOST does → SYSTEM_HOST config applies to that site.
- Neither → OAuth disabled for that site.

The portlet must make this visible and editable:

- **Pinned first row: "All Sites (system default)"** — represents the SYSTEM_HOST config. Editing it changes the global default that any site without its own override inherits.
- **Per-site rows** show one of three statuses:
  - `Site override` — has its own row.
  - `Inherits from System` — no row, but SYSTEM_HOST has one.
  - `Not configured` — no row, SYSTEM_HOST has none either.
- **Clear** on a site row removes the site-specific override; the site falls back to inheriting from System (if System is configured).
- **Clear** on the System row removes the global default; sites that were only inheriting become "Not configured".

This is purely a portlet UX layer — no runtime code changes needed; OAuthAppConfig's existing fallback handles everything.

## Execution Steps

### 1. Rename the AppSecrets key (`dotOAuth` → `dotAuth`)

- `dotCMS/src/main/java/com/dotcms/auth/providers/oauth/OAuthConstants.java`: `APP_KEY = "dotAuth"` (was `"dotOAuth"`).
- `dotCMS/src/test/java/com/dotcms/auth/providers/oauth/OAuthConstantsTest.java`: assert `"dotAuth"`.
- No other call-site changes needed — `OAuthAppConfig.loadSecrets` already reads `OAuthConstants.APP_KEY`.

Acceptable migration story for phase 1 dotOAuth rows: there are none in production yet (phase 1 isn't released). Drop the dotOAuth.yml in step 2.

### 2. Delete `dotOAuth.yml`

- `git rm dotCMS/src/main/resources/apps/dotOAuth.yml`.
- After this, `dotOAuth` does not appear in the generic Apps UI, and `dotAuth` never did. The portlet is the only path.

### 3. Backend REST: `/v1/dotauth`

New: `dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/DotAuthResource.java` (+ form DTO).

Endpoints:
- `GET /v1/dotauth/sites` → an envelope with two parts:
  - `system: {configured: bool}` — has the SYSTEM_HOST row been created?
  - `sites: [{hostId, hostName, status}]` for all non-system sites, where `status ∈ {SITE_OVERRIDE, INHERITED, NOT_CONFIGURED}`. `INHERITED` requires the system row to be configured. Implementation: enumerate hosts, call `AppsAPI.appKeysByHost()` once, and join.
- `GET /v1/dotauth/sites/SYSTEM_HOST` → full SYSTEM_HOST config; hidden secrets (`clientSecret`) returned as `****`. Sentinel `SYSTEM_HOST` (matches AppsAPI's internal usage) routes to `APILocator.systemHost()`.
- `GET /v1/dotauth/sites/{hostId}` → for a site override, returns its own values + `inherited: false`. For a site without an override but where SYSTEM_HOST has one, returns the system values + `inherited: true` so the portlet can pre-populate the form when admins want to break inheritance. Empty + system-empty case: `inherited: false`, `configured: false`, `values: {}`.
- `PUT /v1/dotauth/sites/{hostId}` (or `SYSTEM_HOST`) → upsert. Body: `{values: {field: value, ...}}` mirroring the existing OAuth secret keys (`enabled`, `enableBackend`, `enableFrontend`, `providerType`, `issuerUrl`, `clientId`, `clientSecret`, `scopes`, `authorizationUrl`, `tokenUrl`, `userinfoUrl`, `revocationUrl`, `logoutUrl`, `groupsClaim`, `groupsUrl`, `extraRoles`, `callbackUrl`). When the client posts `****` for a hidden field, preserve the existing stored value.
- `DELETE /v1/dotauth/sites/{hostId}` (or `SYSTEM_HOST`) → clear that host's secrets. On a site row this restores inheritance from System; on the System row it removes the global default.

All endpoints delegate directly to `AppsAPI` — no separate persistence. Save/get/delete reach the system host by passing `APILocator.systemHost()` for the hostId param.

Wire the package in `DotRestApplication.java`:
- Add `"com.dotcms.auth.dotAuth.rest"` to the `packages(...)` list.
- Add `"/v1/dotauth"` to `CORE_REST_PATHS`.

Constants file: `dotCMS/src/main/java/com/dotcms/auth/dotAuth/DotAuthConstants.java` — just `APP_KEY = "dotAuth"`. (`OAuthConstants.APP_KEY` can `=` this constant to keep one source of truth.)

### 4. Java portlet registration

- `dotCMS/src/main/webapp/WEB-INF/portlet.xml`: add a `<portlet>` entry for `dotAuth` matching the dot-tags pattern (Spring `PortletController`, `<portlet-url>/dotAuth</portlet-url>`).
- `dotCMS/src/main/webapp/WEB-INF/messages/Language.properties`:
  - `com.dotcms.repackage.javax.portlet.title.dotAuth=dotAuth` (controls the page header — must be exact-case `dotAuth`).
  - All `dotauth.*` UI keys (column labels, mode tags, button labels, dialog field labels). Reuse the generic `Save` / `Cancel` keys from this file rather than adding portlet-scoped ones.
- Layout placement: figure out which sidebar group `dotAuth` belongs in. Likely "Settings" alongside "Apps" — confirmed during execution by checking how `tags` was added to a layout.

### 5. Frontend Nx library: `libs/portlets/dot-auth`

Generator (per `core-web/libs/portlets/CLAUDE.md`):
```bash
yarn nx generate @nx/angular:library --name=portlet \
  --directory=libs/portlets/dot-auth \
  --tags=type:feature,scope:dotcms-ui,portlet:auth \
  --prefix=dot --standalone --no-interactive
```

Post-generator fixes (from `libs/portlets/CLAUDE.md`):
1. tsconfig.base.json alias → `"@dotcms/portlets/dot-auth/portlet": ["libs/portlets/dot-auth/src/index.ts"]`
2. project.json `name` → `portlets-dot-auth-portlet`
3. jest.config.cts `displayName` → `portlets-dot-auth-portlet`, add `isolatedModules: true`
4. Delete generated `README.md` and `src/lib/portlet/` boilerplate

Code structure (mirror `dot-tags`):
- `dot-auth-shell/` — trivial wrapper rendering `<dot-auth-list />`.
- `dot-auth-list/` — PrimeNG `p-table` with **a pinned first row for "All Sites (system default)"** above the per-site rows. Per-site rows show a `Site override` / `Inherits from System` / `Not configured` badge plus Edit/Clear buttons. Clear is hidden on `Not configured` rows; on the System row, Clear's confirmation copy warns "this will remove the global default for all inheriting sites".
- `dot-auth-list/store/dot-auth-list.store.ts` — SignalStore: `withState({system, sites, status, filter})`, `withMethods({loadSites, saveSite(hostId, payload), clearSite(hostId), setFilter})`. `hostId === 'SYSTEM_HOST'` is treated as the system row throughout. `withHooks(onInit -> loadSites)`. Errors via `DotHttpErrorManagerService`.
- `dot-auth-edit/dot-auth-edit.component.ts` + `.html` — modal dialog with the OAuth form. Header shows the site name (or "All Sites — system default" for SYSTEM_HOST). When opening a site that inherits, the form pre-populates from the inherited values and shows a banner: "This site is inheriting from the system default. Saving will create a site-specific override." Reactive form, fields grouped logically (Provider, Endpoints, Roles & Scopes). On submit, `dialogRef.close({values})`. Hidden `clientSecret` rendered with `<p-password>`.
- `lib.routes.ts` — `[{ path: '', component: DotAuthShellComponent }]`, exported as `dotAuthRoutes`.

Data service (new): `core-web/libs/data-access/src/lib/dot-auth/dot-auth.service.ts` wrapping the four REST endpoints. Re-export from `core-web/libs/data-access/src/index.ts`.

Frontend route registration: `core-web/apps/dotcms-ui/src/app/app.routes.ts` — add a `'dotAuth'` route with `loadChildren: () => import('@dotcms/portlets/dot-auth/portlet').then(m => m.dotAuthRoutes)`. `MenuGuardService` wired the same way as `tags` and `plugins`.

### 6. Verify

Backend:
```bash
./mvnw install -pl :dotcms-core -DskipTests -Dmaven.build.cache.enabled=false
./mvnw test -pl :dotcms-core -Dtest='com.dotcms.auth.**'
```

Frontend:
```bash
yarn nx lint portlets-dot-auth-portlet
yarn nx build dotcms-ui
```

End-to-end (in `just dev-run`):
1. Sidebar shows a "dotAuth" entry. Clicking it lands on the portlet (page header reads `dotAuth`, exact case).
2. Pinned "All Sites (system default)" row shows "Not configured". `default` row shows "Not configured".
3. Edit the System row → fill Okta fields (`https://integrator-9422530.okta.com/oauth2/default`, client id, secret) → Save → System row shows "Configured"; `default` row now shows "Inherits from System".
4. Hit `http://localhost:8080/dotAdmin/` → redirects to Okta (proves the System config is applied to the default site via the SYSTEM_HOST fallback). Login completes back into dotCMS.
5. Edit `default` site → form pre-populates with the inherited values + inheritance banner shows. Change a field, Save → `default` flips to "Site override". `/dotAdmin/` continues to redirect to Okta using the override.
6. Edit again → `clientSecret` field shows `****` mask; saving without changing it preserves the stored value.
7. Clear `default` site → confirm → row reverts to "Inherits from System"; `/dotAdmin/` redirect still works (back to System config).
8. Clear System row → confirm (with the warning copy) → both rows show "Not configured"; `/dotAdmin/` shows native login.
9. Apps list (the existing one): `dotOAuth` no longer appears (yml deleted). `dotsaml-config` is unchanged.

## Files Touched

### New
- `dotCMS/src/main/java/com/dotcms/auth/dotAuth/DotAuthConstants.java`
- `dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/DotAuthResource.java` (+ form DTO)
- `core-web/libs/portlets/dot-auth/**` (generator output + custom code)
- `core-web/libs/data-access/src/lib/dot-auth/dot-auth.service.ts`

### Modified
- `dotCMS/src/main/java/com/dotcms/auth/providers/oauth/OAuthConstants.java` (APP_KEY value)
- `dotCMS/src/main/java/com/dotcms/rest/config/DotRestApplication.java` (add package + path)
- `dotCMS/src/main/webapp/WEB-INF/portlet.xml` (dotAuth entry)
- `dotCMS/src/main/webapp/WEB-INF/messages/Language.properties` (portlet title + dotauth.* keys)
- `dotCMS/src/test/java/com/dotcms/auth/providers/oauth/OAuthConstantsTest.java` (APP_KEY assertion)
- `core-web/apps/dotcms-ui/src/app/app.routes.ts` (register dotAuth route)
- `core-web/libs/data-access/src/index.ts` (export DotAuthService)
- `core-web/tsconfig.base.json` (alias)

### Deleted
- `dotCMS/src/main/resources/apps/dotOAuth.yml`

### Possibly touched (depends on layout placement decision)
- A startup task to drop `dotAuth` into a default layout group, modeled on `Task260206AddUsagePortletToMenu` / `Task260320AddPluginsPortletToMenu`. Confirmed during execution.

## Out of Scope
- SAML — `dotsaml-config` reader, `DotIdentityProviderConfigurationImpl`, `DotSamlProxyFactory` are untouched.
- Multi-protocol (`authMode`) field — site is OAuth-or-not; the portlet has no SAML fields.
- LDAP / other auth protocols.
- Migration from any prior `dotOAuth` configurations (none exist in customer deployments — phase 1 unreleased).
