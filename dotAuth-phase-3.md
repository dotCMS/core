# Phase 3 — Bring SAML into `dotAuth`, deprecate the Apps SAML editor

## Context

Phase 2 shipped the dotAuth portlet as the dedicated editor for OAuth 2.0 / OIDC, stored under the AppSecrets key `dotAuth`. SAML remained editable **only** via the generic Apps UI under the key `dotsaml-config`, which forces admins into two different mental models and two different editing surfaces for what is — conceptually — "site SSO."

Phase 3 unifies the editing experience. The dotAuth portlet becomes the single UI for configuring SSO. OAuth and SAML remain **stored separately** (under their existing AppSecrets keys) because:

- `dotsaml-config.yml` has real customers today. Changing the storage key or merging schemas would break upgrades. Existing secrets must be readable unchanged after phase 3 deploys.
- The SAML runtime (`DotSamlProxyFactory`, `DotIdentityProviderConfigurationImpl`, `SamlWebInterceptor`, `DotSamlResource`) keys off `SAML_APP_CONFIG_KEY = "dotsaml-config"`. Leaving that alone keeps the runtime untouched — same low-risk posture as phase 2 had with OAuth.
- The two schemas are genuinely different (IDP metadata + keystore vs. issuer URL + client credentials). Forcing them into one record would mean a translation layer for zero gain.

**Per-site constraint (product rule):** a site can have **OAuth** or **SAML**, not both. The portlet enforces this on save — configuring one protocol clears the other for the same host. SYSTEM_HOST follows the same rule and can itself be OAuth or SAML.

**Apps UI deprecation:** the `dotsaml-config` card in the Apps list gets a deprecation banner + "Edit in dotAuth" button. We **do not delete `dotsaml-config.yml`** — removing it would erase all customers' existing SAML configs from the UI and make them un-editable through any path other than the dotAuth portlet immediately. Keeping the YAML also means `AppsAPI` keeps validating structure on read and provides a safety net during the deprecation window.

**Verified (carried forward from phase 2):**
- `AppsAPI.saveSecrets/getSecrets/deleteSecrets/appKeysByHost` are descriptor-agnostic and work on any key.
- `dotAuth` portlet's list+edit pattern (`libs/portlets/dot-auth`) is the template for the unified UX.
- Per-host fallback to SYSTEM_HOST is built into `getSecrets(key, fallbackOnSystemHost=true, …)`.

**Phase-2 surface that stays untouched:** the OAuth portlet code paths. We extend, not rewrite.

## Design

```
                          ┌──────────────────────────────────────────┐
                          │ dotAuth portlet (phase 3)                │
                          │                                          │
                          │ ┌──────────────────────────────────────┐ │
                          │ │ All Sites (system default)           │ │   pinned row
                          │ │ Protocol: SAML      [Configured]     │ │   — SAML example
                          │ │                                      │ │
                          │ │ default                              │ │
                          │ │ Protocol: OAuth     [Site override]  │ │
                          │ │                                      │ │
                          │ │ marketing.example.com                │ │
                          │ │                     [Inherits SAML]  │ │
                          │ │                                      │ │
                          │ │ legacy.example.com  [Not configured] │ │
                          │ └──────────────────────────────────────┘ │
                          │ Edit dialog:                             │
                          │   Protocol: [● OAuth  ○ SAML]            │
                          │   ─── OAuth or SAML fieldsets ───        │
                          └──────────────┬───────────────────────────┘
                                         │
                  ┌──────────────────────┴──────────────────────┐
                  ▼                                             ▼
      AppSecrets key = "dotAuth"                 AppSecrets key = "dotsaml-config"
      (OAuth/OIDC, per-host + SYSTEM_HOST)       (SAML, per-host + SYSTEM_HOST)

Runtime (unchanged):
  OAuthWebInterceptor  ─► reads "dotAuth"        via OAuthAppConfig
  SamlWebInterceptor   ─► reads "dotsaml-config" via DotSamlProxyFactory
```

### Authoritative status per site

Each site row resolves to **one** of these states, computed from what's present in both AppSecrets keys:

- `SAML_OVERRIDE`     — the site has its own `dotsaml-config` row.
- `OAUTH_OVERRIDE`    — the site has its own `dotAuth` row.
- `INHERITED_SAML`    — site has neither; SYSTEM_HOST has `dotsaml-config`.
- `INHERITED_OAUTH`   — site has neither; SYSTEM_HOST has `dotAuth`.
- `NOT_CONFIGURED`    — neither host nor SYSTEM_HOST has either key.
- `CONFLICT`          — the site (or SYSTEM_HOST) has **both** keys populated. Shouldn't happen in normal operation; if it does, portlet surfaces a warning and asks the admin to pick one.

The portlet computes state client-side from two envelope calls under the hood (or one merged call — see REST below).

### Mutual exclusion on save

When an admin saves OAuth config for site X, the resource deletes `dotsaml-config` secrets for site X in the same unit of work. And vice versa. The edit dialog makes this explicit via a one-time confirmation dialog: *"Saving will replace the existing SAML configuration for this site. Continue?"* — this protects admins from accidentally nuking a working config by flipping the protocol toggle.

### Apps UI deprecation

- `dotsaml-config` card stays visible in `/c/apps` but shows an **info banner** above the form: *"This configuration is now edited in the **dotAuth** portlet. Changes here will be overwritten if the same site is edited in dotAuth."*
- The banner links directly to `/dotAuth` (the portlet).
- We **do not** remove the Apps card or its YAML. Rationale: customers have runbooks and scripts that target the Apps UI; keeping it as a fallback avoids last-mile breakage. The banner is the hint; the dotAuth portlet is the upgrade path.

## Execution Steps

### 1. Backend: extend REST surface

**Files modified / added:**
- Modify: `dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/DotAuthResource.java`
- Modify: `dotCMS/src/main/java/com/dotcms/auth/dotAuth/DotAuthConstants.java` (add SAML key reference)
- Add: `dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/DotAuthProtocol.java` — enum `{ OAUTH, SAML, NONE }`
- Modify: `DotAuthSitesView.java` — add `protocol` (enum) + richer `status` (see authoritative list above). Make the row carry protocol separately from status so UI can render `"OAuth — Site override"` etc.
- Modify: `DotAuthConfigView.java` — add `protocol` field; `values` shape depends on protocol (OAuth fields for OAUTH, SAML fields for SAML).
- Modify: `DotAuthConfigForm.java` — add `protocol: OAUTH | SAML` at the top; switch the `values` schema on it.

**New endpoint behavior:**
- `GET /v1/dotauth/sites` — enumerate hosts, call `appsAPI.appKeysByHost()` once, detect which of `dotauth` / `dotsaml-config` each site has, emit the new status list.
- `GET /v1/dotauth/sites/{hostId}` — resource decides based on which key has a row: return the corresponding protocol's values. For `INHERITED_*` rows, return system defaults with `inherited: true`. Secrets mask on SAML: `privateKey` becomes `****`; `publicCert` stays visible (it's a cert, not a secret).
- `PUT /v1/dotauth/sites/{hostId}` — body carries `protocol` + `values`:
  - `OAUTH`: delete `dotsaml-config` for this host (if any) + save `dotAuth` secrets (existing phase-2 behavior).
  - `SAML`: delete `dotAuth` for this host (if any) + save `dotsaml-config` secrets (new).
  - Handling `****` mask for `dotAuth.clientSecret` (existing) and `dotsaml-config.privateKey` (new) — preserve stored values when mask is posted back.
- `DELETE /v1/dotauth/sites/{hostId}` — **both** keys deleted. This clears the site's override entirely regardless of which protocol was active.

**SAML field set** (mirrors `dotsaml-config.yml`):
```
enable, idpName, sPIssuerURL, sPEndpointHostname, signatureValidationType,
idPMetadataFile, publicCert, privateKey, buttonParam
```
Map these to a constant `SAML_SECRET_KEYS` in `DotAuthResource` the same way `SECRET_KEYS` is used for OAuth. Hidden keys: `{privateKey}`. Boolean keys: `{enable}`.

### 2. Backend: conflict detection

If `GET /sites/{hostId}` finds **both** `dotauth` and `dotsaml-config` populated for the host, return a response with `protocol: CONFLICT`, `conflictSnapshot: { oauth: {...masked values}, saml: {...masked values} }`. The portlet renders a disambiguation UI (radio: "Keep OAuth / Keep SAML / Clear both").

### 3. Frontend: models + service

**Files:**
- Modify: `core-web/libs/dotcms-models/src/lib/dot-auth.model.ts`
  - Add `DotAuthProtocol = 'OAUTH' | 'SAML' | 'NONE' | 'CONFLICT'`.
  - Expand `DotAuthStatus` union (or replace with tuple `{ protocol, scope }` where scope is `OVERRIDE | INHERITED | NOT_CONFIGURED | CONFLICT`).
  - Add `DotAuthSamlConfigValues` interface mirroring the SAML schema.
  - `DotAuthConfigView`/`DotAuthConfigPayload` become discriminated unions on `protocol`.
- Modify: `core-web/libs/data-access/src/lib/dot-auth/dot-auth.service.ts` — no signature change, but widen types.

### 4. Frontend: list UX updates

**Files:**
- Modify: `core-web/libs/portlets/dot-auth/src/lib/dot-auth-list/dot-auth-list.component.{ts,html}`
  - Show a **Protocol** column between Site and Status.
  - Status tag colors: `OAUTH_*` → success (green), `SAML_*` → info (blue), `INHERITED_*` → info (muted), `NOT_CONFIGURED` → secondary, `CONFLICT` → danger.
  - System row shows protocol + status.
  - Conflict rows expose a "Resolve" button instead of "Edit".

### 5. Frontend: edit dialog — protocol toggle

**Files:**
- Modify: `core-web/libs/portlets/dot-auth/src/lib/dot-auth-edit/dot-auth-edit.component.{ts,html}`
  - Top of form: `p-selectbutton` with OAuth / SAML options (default selected = current protocol if any, else OAuth).
  - Swap fieldset groups based on selection:
    - **OAuth** (existing): Provider / Endpoints / Roles & Scopes.
    - **SAML** (new): IDP Details (`idpName`, `idPMetadataFile`, `signatureValidationType`), SP Details (`sPIssuerURL`, `sPEndpointHostname`, `buttonParam`), Credentials (`publicCert` as textarea, `privateKey` as password). Reuse the existing fieldset pattern — new component under `dot-auth-edit/saml-fieldsets/`.
  - Protocol toggle triggers mutual-exclusion warning dialog when switching from one populated protocol to the other.
  - `idPMetadataFile` is a **file upload** field in the Apps UI; in phase 3 we accept pasted XML as a textarea (simplest path). File upload can be a follow-up.

### 6. Frontend: conflict resolution dialog

**Files:**
- Add: `core-web/libs/portlets/dot-auth/src/lib/dot-auth-conflict/dot-auth-conflict.component.{ts,html}`
  - Shows both OAuth and SAML snapshots side-by-side (read-only).
  - Action buttons: "Keep OAuth (deletes SAML)", "Keep SAML (deletes OAuth)", "Clear both".
  - Calls `saveSite` with the chosen protocol, or `clearSite` if admin wants to start over.

### 7. i18n keys

**Files:**
- Modify: `dotCMS/src/main/webapp/WEB-INF/messages/Language.properties`
  - Add `dotauth.protocol.oauth=OAuth 2.0 / OIDC`
  - Add `dotauth.protocol.saml=SAML 2.0`
  - Add `dotauth.status.site-override.oauth=OAuth override`
  - Add `dotauth.status.site-override.saml=SAML override`
  - Add `dotauth.status.inherited.oauth=Inherits OAuth from System`
  - Add `dotauth.status.inherited.saml=Inherits SAML from System`
  - Add `dotauth.status.conflict=Conflict — both OAuth and SAML configured`
  - Add `dotauth.confirm.switch-protocol.header=Switch protocol?`
  - Add `dotauth.confirm.switch-protocol.message=Saving as {0} will delete the existing {1} configuration for this site. Continue?`
  - Add `dotauth.conflict.header=Resolve conflict`
  - Add `dotauth.conflict.message=This site has both OAuth and SAML configured. Choose which to keep.`
  - SAML field labels: `dotauth.field.idpName`, `dotauth.field.sPIssuerURL`, `dotauth.field.sPEndpointHostname`, `dotauth.field.signatureValidationType`, `dotauth.field.idPMetadataFile`, `dotauth.field.publicCert`, `dotauth.field.privateKey`, `dotauth.field.buttonParam`, `dotauth.field.enable` (note: spelled without the `d` to match the existing SAML secret key).
  - `dotauth.fieldset.saml.idp=IDP Details`
  - `dotauth.fieldset.saml.sp=SP Details`
  - `dotauth.fieldset.saml.credentials=Credentials`

### 8. Apps UI deprecation banner

**Files:**
- Modify: `core-web/apps/dotcms-ui/src/app/portlets/dot-apps/dot-apps-configuration-detail/dot-apps-configuration-detail.component.{ts,html}` — or the closest equivalent where the per-app detail renders.
  - When `appKey === 'dotsaml-config'`, render a `p-message severity="warn"` banner at the top: *"This configuration is now edited in the dotAuth portlet. Changes made here may be overwritten."* with an "Open dotAuth" button that routes to `/dotAuth`.
  - Behind a feature flag `FEATURE_FLAG_DOTSAML_APPS_DEPRECATION=true` so it can be toggled off if it causes customer confusion.

### 9. Verify

**Backend:**
```bash
./mvnw install -pl :dotcms-core -DskipTests
./mvnw test -pl :dotcms-core -Dtest='com.dotcms.auth.**'
```

**Frontend:**
```bash
yarn nx lint portlets-dot-auth-portlet
yarn nx test portlets-dot-auth-portlet
yarn nx build dotcms-ui
```

**End-to-end (`just dev-run-fixed`):**

**Clean-slate paths:**
1. `dotAuth` portlet → edit System → select **SAML**, paste IDP metadata, save → row shows "SAML — Configured".
2. Edit `default` → form pre-populates with inherited SAML values + inheritance banner → save as-is → row shows "SAML override".
3. Hit `/dotAdmin/` → redirects to the SAML IDP (proves SAML runtime reads the secret).
4. Edit `default` → switch protocol to **OAuth** → confirmation dialog warns that SAML will be deleted → confirm → enter Okta fields → save → row shows "OAuth override". Under the hood: `dotsaml-config` row for `default` is gone; `dotAuth` row is present.
5. Hit `/dotAdmin/` → redirects to Okta (proves OAuth runtime takes over on the same site).

**Upgrade path (carried over from a phase-2 customer):**
6. Start from a DB with existing `dotsaml-config` secrets for SYSTEM_HOST (simulate customer state by editing via Apps UI before upgrade). Upgrade to phase-3 build. Open dotAuth portlet → System row shows "SAML — Configured" with the right values. No data loss, no edit required.
7. Open Apps UI → navigate to `dotsaml-config` → deprecation banner visible, "Open dotAuth" button routes to `/dotAuth`.

**Conflict path (synthetic — shouldn't happen organically):**
8. Using AppsAPI directly (or `curl` against the Apps REST), populate **both** `dotAuth` and `dotsaml-config` for the same host. Refresh dotAuth portlet → row shows "Conflict" status. Click Resolve → dialog shows both snapshots → pick "Keep OAuth" → SAML row deleted, OAuth remains. Refresh → status is now "OAuth override".

## Files Touched

### New
- `dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/DotAuthProtocol.java`
- `core-web/libs/portlets/dot-auth/src/lib/dot-auth-conflict/dot-auth-conflict.component.{ts,html}`
- `core-web/libs/portlets/dot-auth/src/lib/dot-auth-edit/saml-fieldsets/*` (SAML field groups, split into sub-components only if the file gets unwieldy)

### Modified
- `dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/DotAuthResource.java` — dual-key writes, conflict detection, SAML paths.
- `dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/DotAuthSitesView.java`, `DotAuthConfigView.java`, `DotAuthConfigForm.java` — add protocol + richer status.
- `dotCMS/src/main/java/com/dotcms/auth/dotAuth/DotAuthConstants.java` — expose the SAML key (reference, not redefine; use `DotSamlProxyFactory.SAML_APP_CONFIG_KEY`).
- `dotCMS/src/main/webapp/WEB-INF/messages/Language.properties` — new keys.
- `core-web/libs/dotcms-models/src/lib/dot-auth.model.ts` — protocol enum + SAML values type + discriminated unions.
- `core-web/libs/data-access/src/lib/dot-auth/dot-auth.service.ts` — type tightening (no URL changes).
- `core-web/libs/portlets/dot-auth/src/lib/dot-auth-list/dot-auth-list.component.{ts,html}` — protocol column + conflict row.
- `core-web/libs/portlets/dot-auth/src/lib/dot-auth-edit/dot-auth-edit.component.{ts,html}` — protocol toggle + fieldset swap + mutual-exclusion confirm.
- `core-web/libs/portlets/dot-auth/src/lib/dot-auth-list/store/dot-auth-list.store.ts` — `resolveConflict(hostId, keepProtocol)` method.
- `core-web/apps/dotcms-ui/src/app/portlets/dot-apps/**` — deprecation banner on `dotsaml-config` detail page.

### Not touched
- `dotsaml-config.yml` — stays.
- `DotSamlProxyFactory`, `DotIdentityProviderConfigurationImpl`, `SamlWebInterceptor`, `DotSamlResource` — SAML runtime stays.
- `OAuthWebInterceptor`, `OAuthAppConfig`, phase-1 OAuth runtime — untouched.

## Out of Scope

- **LDAP / other auth protocols** — same exclusion as phase 2.
- **Multi-IDP per site** — one OAuth config or one SAML config per site, period.
- **Automatic migration** between OAuth and SAML.
- **File-upload for `idPMetadataFile`** — phase 3 accepts pasted XML in a textarea; file upload is a follow-up (needs multipart handling + `FormDataMultiPart` + file validation).
- **Removing the Apps UI entry for `dotsaml-config`** — kept as a fallback during the deprecation window. A future phase can delete it once we have telemetry showing admins have moved to dotAuth.
- **Unifying the storage schemas** under one AppSecrets key — conceptually tempting, operationally risky, and buys us nothing. Revisit only if we ever add a third protocol.
