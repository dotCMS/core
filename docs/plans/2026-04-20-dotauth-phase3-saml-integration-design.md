# dotAuth Phase 3 — SAML Integration Design

**Branch:** `feat/dotauth-saml-integration` (forked from `feat/oauth-core-migration`)
**Date:** 2026-04-20
**Status:** Design approved, ready for implementation planning

---

## Goal

Absorb SAML configuration into the `dotAuth` portlet so admins edit OAuth and SAML through a single UI. OAuth and SAML secrets stay in their existing AppSecrets keys (`dotAuth` and `dotsaml-config`) — no data migration, no runtime changes. The Apps-UI `dotsaml-config` editor is removed at the end of the PR so the portlet is the sole editor for both protocols, mirroring the pattern phase 2 established for OAuth.

## Non-breaking constraints honored

- **Customer data** — existing `dotsaml-config` secrets read and save correctly after merge. No migration.
- **SAML runtime** — `SamlWebInterceptor`, `DotSamlProxyFactory`, `/api/v1/dotsaml/metadata/*` untouched.
- **Phase-2 REST surface** — `DotAuthSiteStatus` enum unchanged; `protocol` added as a separate orthogonal field. Missing `protocol` in a PUT body defaults to `OAUTH`.

Deferred / out of scope:
- Feature flag for the whole portlet (may revisit if heavy QA surfaces a need; current plan is a final-commit checkpoint instead).
- Auto-generated SP keypair (currently admin pastes public cert + private key; matches Apps UI today).
- File upload for IDP metadata (textarea only, matches Apps UI).
- Error-message improvement for `"User cannot be extracted from Assertion!"` (orthogonal; separate follow-up).

## Decisions

### Protocol + status modelling

`DotAuthSiteStatus` stays at 3 values (`SITE_OVERRIDE | INHERITED | NOT_CONFIGURED`). A separate `DotAuthProtocol` enum (`OAUTH | SAML`) carries the protocol axis. The table shows Protocol as its own column alongside Status; the pill colors encode protocol (OAuth → `success` green, SAML → `info` blue), the labels encode scope. Phase-2 clients that don't know about `protocol` keep working without code changes.

### Editor consolidation

`dotsaml-config.yml` is deleted in the final commit of the PR, after Okta E2E passes. This removes the Apps UI's SAML editor; the portlet becomes the sole editor for both protocols. Customer SAML data persists (YAML is the Apps-UI schema descriptor, not the storage — `SamlWebInterceptor` reads secrets by key, no descriptor dependency). No `CONFLICT` state is modelled in code because it's unreachable once the portlet is the sole editor and enforces mutual exclusion on save.

### Mutual exclusion on save

Portlet PUT deletes the other-protocol key for the host, then writes the chosen protocol's secrets. Not atomic across the two `AppsAPI` calls; worst-case interim state is "nothing configured" which is strictly safer than a conflict. Confirmation dialog warns the admin when switching protocols on a site with existing data.

### DELETE semantics

`DELETE /v1/dotauth/sites/{hostId}` clears both `dotAuth` and `dotsaml-config` for the host. Matches the admin's mental model of "remove all auth for this site."

### Checkpoint before YAML delete

Single PR, commits kept atomic. The `chore: remove dotsaml-config.yml` commit is the last one and can be dropped independently if the pre-merge QA finds a problem. Until it lands, Apps UI keeps showing the legacy editor, giving admins a fallback during the merge window.

## Architecture

### Backend — Strategy pattern on `DotAuthResource`

```
DotAuthResource (thin dispatcher)
    │
    ├──► ProtocolHandler (interface)
    │       • protocol()
    │       • appKey()
    │       • secretKeys() / hiddenKeys() / booleanKeys()
    │       • maskedValues(AppSecrets)
    │       • buildSecrets(Map, Optional<AppSecrets>)
    │
    ├──► OAuthProtocolHandler   (existing constants + helpers, extracted unchanged)
    │       appKey = "dotAuth"
    │
    └──► SamlProtocolHandler    (new)
            appKey = DotSamlProxyFactory.SAML_APP_CONFIG_KEY  // "dotsaml-config"
            SAML_SECRET_KEYS = [enable, idpName, sPIssuerURL, sPEndpointHostname,
                                signatureValidationType, idPMetadataFile,
                                publicCert, privateKey, buttonParam]
            HIDDEN = {privateKey}
            BOOLEAN = {enable}
```

`DotAuthResource` keeps a `Map<DotAuthProtocol, ProtocolHandler>` and delegates endpoint logic to the handler selected by the incoming `protocol` field (or by detection on GET).

### Backend — DTOs (additive only)

| Class | Change |
|---|---|
| `DotAuthProtocol` (new) | `enum { OAUTH, SAML }` |
| `DotAuthSitesView.SiteRowView` | + `protocol: DotAuthProtocol?` (null when `status == NOT_CONFIGURED`) |
| `DotAuthSitesView.SystemView` | + `protocol: DotAuthProtocol?` |
| `DotAuthConfigView` | + `protocol: DotAuthProtocol` |
| `DotAuthConfigForm` | + `protocol: DotAuthProtocol?` (defaults to `OAUTH` when missing) |
| `DotAuthSiteStatus` | unchanged — 3 values |

### Backend — endpoint behaviour

- `GET /v1/dotauth/sites` — enumerate hosts once via `appsAPI.appKeysByHost()`; for each host ask both handlers which key it has; emit `protocol` alongside `status`.
- `GET /v1/dotauth/sites/{hostId}` — detect which key is present on host, else fall through to SYSTEM_HOST for inheritance; return the matching handler's masked values.
- `PUT /v1/dotauth/sites/{hostId}` — dispatch on `form.protocol`; delete other-protocol key; save chosen handler's secrets. Hidden-key mask (`****`) preserved for `clientSecret` (OAuth) and `privateKey` (SAML).
- `DELETE /v1/dotauth/sites/{hostId}` — delete both keys.
- `/v1/dotauth/debug` — temp phase-2 diagnostic, removed in cleanup commit.

### Frontend — models

`libs/dotcms-models/src/lib/dot-auth.model.ts`:

```typescript
export type DotAuthProtocol = 'OAUTH' | 'SAML';

export interface DotAuthSamlConfigValues {
    enable?: boolean;
    idpName?: string;
    sPIssuerURL?: string;
    sPEndpointHostname?: string;
    signatureValidationType?: 'none' | 'response' | 'assertion' | 'responseandassertion';
    idPMetadataFile?: string;
    publicCert?: string;
    privateKey?: string;    // masked as '****' on GET
    buttonParam?: string;
}

export interface DotAuthSiteRow {
    hostId: string;
    hostName: string;
    status: DotAuthStatus;
    protocol: DotAuthProtocol | null;
}

export interface DotAuthSystemView {
    configured: boolean;
    protocol: DotAuthProtocol | null;
}

export type DotAuthConfigView =
    | { hostId: string; protocol: 'OAUTH'; configured: boolean; inherited: boolean;
        values: DotAuthConfigValues }
    | { hostId: string; protocol: 'SAML'; configured: boolean; inherited: boolean;
        values: DotAuthSamlConfigValues };

export type DotAuthConfigPayload =
    | { protocol: 'OAUTH'; values: DotAuthConfigValues }
    | { protocol: 'SAML';  values: DotAuthSamlConfigValues };
```

Existing `DotAuthConfigValues` interface stays named as-is (OAuth-only) — skip the rename to keep the diff focused.

### Frontend — list component

New Protocol column between Site and Status. Composed status tag:

```typescript
statusTag(status: DotAuthStatus, protocol: DotAuthProtocol | null): StatusTag {
    if (status === 'NOT_CONFIGURED') {
        return { labelKey: 'dotauth.status.not-configured', severity: 'secondary' };
    }
    const severity = protocol === 'OAUTH' ? 'success' : 'info';
    const labelKey = status === 'SITE_OVERRIDE'
        ? 'dotauth.status.site-override'
        : 'dotauth.status.inherited';
    return { labelKey, severity };
}
```

Store unchanged in structure — new `protocol` field rides through existing state.

### Frontend — edit dialog

Two FormGroups, swap which one is active based on a protocol toggle (`p-selectButton`). Cleaner than validator juggling on a single FormGroup, and the discriminated union narrows naturally on the save payload.

```typescript
readonly oauthForm = this.fb.group({ /* phase-2 fields unchanged */ });
readonly samlForm  = this.fb.group({ /* SAML_SECRET_KEYS */ });
readonly activeForm = computed(() =>
    this.selectedProtocol() === 'OAUTH' ? this.oauthForm : this.samlForm);
```

SAML fieldsets grouped three ways:
- **IDP Details** — `idpName`, `idPMetadataFile` (textarea), `signatureValidationType` (p-select)
- **SP Details** — `sPIssuerURL`, `sPEndpointHostname`, `buttonParam`
- **Credentials** — `publicCert` (textarea), `privateKey` (`p-password`)

Protocol-switch confirmation fires only when the current form is dirty; inheritance banner pattern unchanged from phase 2; `privateKey` joins `clientSecret` in the mask-on-GET / preserve-on-PUT-when-`****` handling.

## i18n

New keys in `dotCMS/src/main/webapp/WEB-INF/messages/Language.properties`:
- `dotauth.protocol.oauth`, `dotauth.protocol.saml`
- `dotauth.confirm.switch-protocol.header`, `.message`
- `dotauth.fieldset.saml.idp`, `.sp`, `.credentials`
- `dotauth.field.{enable, idpName, sPIssuerURL, sPEndpointHostname, signatureValidationType, idPMetadataFile, publicCert, privateKey, buttonParam}`
- `dotauth.field.sigvalidation.{none, response, assertion, responseandassertion}`

Labels lifted from `dotsaml-config.yml` for muscle-memory parity.

## Testing

**Backend:**
- `DotAuthResourceTest` — add SAML branch per endpoint (list / get / save / delete), both override and inherited paths.
- New `OAuthProtocolHandlerTest`, `SamlProtocolHandlerTest` — `maskedValues`, `buildSecrets`, hidden-key handling.
- One cross-protocol MX integration test — save OAuth on SAML-configured host, verify `dotsaml-config` row deleted.

**Frontend:**
- `DotAuthListStoreTest` — widen fixtures with `protocol: 'SAML'` rows.
- `DotAuthListComponentTest` — protocol cell renders, status-tag severity per protocol.
- `DotAuthEditComponentTest` — two describe blocks (OAUTH / SAML); protocol-switch confirm dialog.

**E2E with Okta (`just dev-run`):**
1. Portlet → edit SYSTEM_HOST → toggle SAML → paste Okta metadata → save → row shows "SAML · Configured".
2. `/dotAdmin/` → Okta sign-in → back in dotCMS signed in.
3. Portlet → edit `default` → inherited SAML view + banner → save → "SAML · Site override".
4. Portlet → edit `default` → toggle OAuth → confirm dialog → enter OIDC fields → save → "OAuth · Site override"; SAML row gone.
5. Portlet → Clear `default` → "Not configured"; both keys deleted.

**Upgrade-smoke checkpoint (gating commit 13):** boot this branch against a DB with pre-existing `dotsaml-config` SYSTEM_HOST row; open portlet; expect "SAML · Configured" with correct values, no click required.

## Commit order

Single PR, atomic commits:

1. `feat(auth): add DotAuthProtocol enum + ProtocolHandler interface`
2. `refactor(auth): extract OAuthProtocolHandler from DotAuthResource`
3. `feat(auth): add SamlProtocolHandler + SAML_SECRET_KEYS`
4. `feat(auth): DotAuthResource dispatches on protocol; dual-key read/write/delete + MX`
5. `feat(auth): add protocol field to DotAuth REST DTOs (additive)`
6. `feat(dot-auth): widen models to discriminated union on protocol`
7. `feat(dot-auth): protocol column + composed status tag in list`
8. `feat(dot-auth): protocol toggle + SAML fieldsets in edit dialog`
9. `feat(dot-auth): SAML i18n keys`
10. `test(auth): SAML coverage for protocol handlers + DotAuthResource`
11. `test(dot-auth): SAML branch coverage for list + edit components`
12. **CHECKPOINT — upgrade smoke + Okta E2E**
13. `chore(auth): remove dotsaml-config.yml; portlet is sole SAML editor`
14. `chore(auth): remove temp /v1/dotauth/debug endpoint`

If the checkpoint fails, commits 1–11 still merge; 13 is reattempted after the fix.

## Files touched

### New
- `dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/DotAuthProtocol.java`
- `dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/handler/ProtocolHandler.java`
- `dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/handler/OAuthProtocolHandler.java`
- `dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/handler/SamlProtocolHandler.java`

### Modified
- `dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/DotAuthResource.java` — dispatcher shape
- `dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/DotAuthSitesView.java` — `protocol` on both views
- `dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/DotAuthConfigView.java` — `protocol`
- `dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/DotAuthConfigForm.java` — `protocol` (default OAUTH)
- `dotCMS/src/main/webapp/WEB-INF/messages/Language.properties` — i18n keys
- `core-web/libs/dotcms-models/src/lib/dot-auth.model.ts` — discriminated union + SAML values
- `core-web/libs/data-access/src/lib/dot-auth/dot-auth.service.ts` — type widening
- `core-web/libs/portlets/dot-auth/src/lib/dot-auth-list/dot-auth-list.component.{ts,html}` — protocol column
- `core-web/libs/portlets/dot-auth/src/lib/dot-auth-list/store/dot-auth-list.store.ts` — protocol passthrough
- `core-web/libs/portlets/dot-auth/src/lib/dot-auth-edit/dot-auth-edit.component.{ts,html}` — protocol toggle + SAML fieldsets

### Removed (final commit)
- `dotCMS/src/main/resources/apps/dotsaml-config.yml`
- `/v1/dotauth/debug` endpoint in `DotAuthResource`

### Untouched
- `DotSamlProxyFactory`, `DotIdentityProviderConfigurationImpl`, `SamlWebInterceptor`, `DotSamlResource` — SAML runtime.
- `OAuthWebInterceptor`, `OAuthAppConfig` — OAuth runtime.
- `com.dotcms.samlbundle` (OSGi bundle) — external.
