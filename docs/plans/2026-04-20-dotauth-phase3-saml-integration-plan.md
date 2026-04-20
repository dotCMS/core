# dotAuth Phase 3 — SAML Integration Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use `superpowers-extended-cc:executing-plans` to implement this plan task-by-task.

**Goal:** Absorb SAML configuration into the `dotAuth` portlet so admins edit OAuth and SAML through a single UI. OAuth and SAML secrets stay in their existing AppSecrets keys (`dotAuth` and `dotsaml-config`). `dotsaml-config.yml` is removed in the final commit so the portlet is the sole editor, mirroring the phase-2 OAuth pattern.

**Architecture:**
- Backend: Strategy pattern. `ProtocolHandler` interface with `OAuthProtocolHandler` + `SamlProtocolHandler` impls. `DotAuthResource` stays a thin dispatcher that picks a handler per request. Mutual exclusion on save = portlet deletes the other-protocol key, then writes the chosen protocol's secrets. DELETE clears both keys.
- Frontend: discriminated union on `protocol` for `DotAuthConfigView` / `DotAuthConfigPayload`. Two FormGroups in the edit dialog — one OAuth, one SAML — swapped by a `p-selectButton` at the top. New Protocol column in the list; status tag severity encodes protocol (OAuth → `success`, SAML → `info`).

**Tech Stack:** Java 11-syntax / Maven / JAX-RS (backend); Angular 19+ standalone, NgRx SignalStore, PrimeNG (`p-table`, `p-selectButton`, `p-fieldset`, `p-tag`, `pInputTextarea`, `p-password`, `p-select`), Tailwind utilities (frontend).

**Reference:**
- `docs/plans/2026-04-20-dotauth-phase3-saml-integration-design.md` — approved design.
- `dotAuth-phase-3.md` — original phase-3 brief.
- `dotCMS/src/main/resources/apps/dotsaml-config.yml` — SAML field schema (source of truth for field names, labels, option values).
- `dotCMS/src/main/java/com/dotcms/saml/DotSamlProxyFactory.java:36` — `SAML_APP_CONFIG_KEY = "dotsaml-config"` (reference, do not redefine).
- `core-web/libs/portlets/dot-auth/**` — phase-2 portlet scaffolding (template for phase-3 additions).

---

## Task 0: Commit the plan

**Files:** working tree only — design doc is already committed at `047174dde9`.

**Step 1:** Confirm branch and tree state.

Run: `git status`
Expected: on branch `feat/dotauth-saml-integration`. `docs/plans/2026-04-20-dotauth-phase3-saml-integration-plan.md` and `docs/plans/2026-04-20-dotauth-phase3-saml-integration-plan.md.tasks.json` untracked; `.mvn/maven-build-cache-config.xml` modified (pre-existing, leave it alone).

**Step 2:** Commit the plan + tasks file.

```bash
git add docs/plans/2026-04-20-dotauth-phase3-saml-integration-plan.md \
        docs/plans/2026-04-20-dotauth-phase3-saml-integration-plan.md.tasks.json
git commit -m "docs(auth): add dotAuth phase-3 implementation plan"
```

**Note:** No intermediate builds. Final verification happens at Task 14 per user preference.

---

## Task 1: Add `DotAuthProtocol` enum

**Files:**
- Create: `dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/DotAuthProtocol.java`

**Step 1:** Create the enum.

```java
package com.dotcms.auth.dotAuth.rest;

/**
 * Authentication protocol handled by the dotAuth portlet. Used as the
 * discriminator on {@link DotAuthConfigView} and {@link DotAuthConfigForm}.
 */
public enum DotAuthProtocol {

    /** OAuth 2.0 / OIDC — secrets stored under the {@code dotAuth} app key. */
    OAUTH,

    /** SAML 2.0 — secrets stored under the {@code dotsaml-config} app key. */
    SAML
}
```

**Step 2: Commit.**

```bash
git add dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/DotAuthProtocol.java
git commit -m "feat(auth): add DotAuthProtocol enum"
```

---

## Task 2: Add `ProtocolHandler` interface

**Files:**
- Create: `dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/handler/ProtocolHandler.java`

**Step 1:** Create the interface.

```java
package com.dotcms.auth.dotAuth.rest.handler;

import com.dotcms.auth.dotAuth.rest.DotAuthProtocol;
import com.dotcms.security.apps.AppSecrets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Per-protocol strategy for reading and writing secrets through {@link
 * com.dotcms.security.apps.AppsAPI}. Implementations own their secret-key
 * set, their hidden-key set (values masked on GET, preserved on PUT when the
 * client posts back the mask), and their boolean-key set (stored as
 * primitives rather than strings).
 */
public interface ProtocolHandler {

    /** The protocol this handler serves. */
    DotAuthProtocol protocol();

    /** AppSecrets key under which this protocol's secrets are stored. */
    String appKey();

    /** Ordered list of secret keys the portlet is allowed to read/write. */
    List<String> secretKeys();

    /** Subset of {@link #secretKeys()} whose values are masked on GET. */
    Set<String> hiddenKeys();

    /** Subset of {@link #secretKeys()} whose values are stored as booleans. */
    Set<String> booleanKeys();

    /**
     * Render an AppSecrets object for the client. Hidden-key values are
     * replaced with the mask sentinel; boolean keys are unboxed.
     */
    Map<String, Object> maskedValues(AppSecrets secrets);

    /**
     * Build an AppSecrets object from a form submission. If {@code existing}
     * is present and the incoming value for a hidden key is the mask
     * sentinel, the stored value is preserved.
     */
    AppSecrets buildSecrets(Map<String, Object> incoming, Optional<AppSecrets> existing);
}
```

**Step 2: Commit.**

```bash
git add dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/handler/ProtocolHandler.java
git commit -m "feat(auth): add ProtocolHandler strategy interface"
```

---

## Task 3: Extract `OAuthProtocolHandler` from `DotAuthResource`

Pure refactor — lift constants and helpers out of `DotAuthResource` into a new handler class. **Behaviour must not change.**

**Files:**
- Create: `dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/handler/OAuthProtocolHandler.java`
- Modify: `dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/DotAuthResource.java`

**Step 1: Write the failing test.** Create `dotCMS/src/test/java/com/dotcms/auth/dotAuth/rest/handler/OAuthProtocolHandlerTest.java`:

```java
package com.dotcms.auth.dotAuth.rest.handler;

import com.dotcms.auth.dotAuth.DotAuthConstants;
import com.dotcms.auth.dotAuth.rest.DotAuthProtocol;
import com.dotcms.auth.providers.oauth.OAuthAppConfig;
import com.dotcms.security.apps.AppSecrets;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import static org.junit.Assert.*;

public class OAuthProtocolHandlerTest {

    private final OAuthProtocolHandler handler = new OAuthProtocolHandler();

    @Test
    public void protocol_is_OAUTH() {
        assertEquals(DotAuthProtocol.OAUTH, handler.protocol());
    }

    @Test
    public void appKey_is_dotAuth() {
        assertEquals(DotAuthConstants.APP_KEY, handler.appKey());
    }

    @Test
    public void secretKeys_include_all_OAuthAppConfig_keys() {
        assertTrue(handler.secretKeys().contains(OAuthAppConfig.KEY_CLIENT_SECRET));
        assertTrue(handler.secretKeys().contains(OAuthAppConfig.KEY_ISSUER_URL));
        assertTrue(handler.secretKeys().contains(OAuthAppConfig.KEY_CALLBACK_URL));
    }

    @Test
    public void clientSecret_is_hidden() {
        assertTrue(handler.hiddenKeys().contains(OAuthAppConfig.KEY_CLIENT_SECRET));
    }

    @Test
    public void enabled_is_boolean() {
        assertTrue(handler.booleanKeys().contains(OAuthAppConfig.KEY_ENABLED));
    }

    @Test
    public void maskedValues_replaces_clientSecret_with_mask() {
        final AppSecrets secrets = AppSecrets.builder()
                .withKey(DotAuthConstants.APP_KEY)
                .withHiddenSecret(OAuthAppConfig.KEY_CLIENT_SECRET, "real-secret")
                .withSecret(OAuthAppConfig.KEY_CLIENT_ID, "my-id")
                .build();

        final Map<String, Object> out = handler.maskedValues(secrets);

        assertEquals("****", out.get(OAuthAppConfig.KEY_CLIENT_SECRET));
        assertEquals("my-id", out.get(OAuthAppConfig.KEY_CLIENT_ID));
    }

    @Test
    public void buildSecrets_preserves_stored_clientSecret_when_mask_posted_back() {
        final AppSecrets existing = AppSecrets.builder()
                .withKey(DotAuthConstants.APP_KEY)
                .withHiddenSecret(OAuthAppConfig.KEY_CLIENT_SECRET, "stored-secret")
                .build();

        final AppSecrets result = handler.buildSecrets(
                Map.of(OAuthAppConfig.KEY_CLIENT_SECRET, "****"),
                Optional.of(existing));

        assertEquals("stored-secret",
                result.getSecrets().get(OAuthAppConfig.KEY_CLIENT_SECRET).getString());
    }
}
```

**Step 2: Run the test, verify it FAILs.**
Run: `./mvnw test -pl :dotcms-core -Dtest=OAuthProtocolHandlerTest`
Expected: FAIL with "cannot find symbol class OAuthProtocolHandler".

**Step 3: Create the handler** by moving constants and helpers out of `DotAuthResource` verbatim.

```java
package com.dotcms.auth.dotAuth.rest.handler;

import com.dotcms.auth.dotAuth.DotAuthConstants;
import com.dotcms.auth.dotAuth.rest.DotAuthProtocol;
import com.dotcms.auth.providers.oauth.OAuthAppConfig;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.Secret;
import io.vavr.control.Try;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class OAuthProtocolHandler implements ProtocolHandler {

    public static final String HIDDEN_SECRET_MASK = "****";

    private static final List<String> SECRET_KEYS = List.of(
            OAuthAppConfig.KEY_ENABLED,
            OAuthAppConfig.KEY_ENABLE_BACKEND,
            OAuthAppConfig.KEY_ENABLE_FRONTEND,
            OAuthAppConfig.KEY_PROVIDER_TYPE,
            OAuthAppConfig.KEY_ISSUER_URL,
            OAuthAppConfig.KEY_CLIENT_ID,
            OAuthAppConfig.KEY_CLIENT_SECRET,
            OAuthAppConfig.KEY_SCOPES,
            OAuthAppConfig.KEY_AUTHORIZATION_URL,
            OAuthAppConfig.KEY_TOKEN_URL,
            OAuthAppConfig.KEY_USERINFO_URL,
            OAuthAppConfig.KEY_REVOCATION_URL,
            OAuthAppConfig.KEY_LOGOUT_URL,
            OAuthAppConfig.KEY_GROUPS_CLAIM,
            OAuthAppConfig.KEY_GROUPS_URL,
            OAuthAppConfig.KEY_EXTRA_ROLES,
            OAuthAppConfig.KEY_CALLBACK_URL);

    private static final Set<String> HIDDEN_KEYS = Set.of(OAuthAppConfig.KEY_CLIENT_SECRET);

    private static final Set<String> BOOLEAN_KEYS = Set.of(
            OAuthAppConfig.KEY_ENABLED,
            OAuthAppConfig.KEY_ENABLE_BACKEND,
            OAuthAppConfig.KEY_ENABLE_FRONTEND);

    @Override public DotAuthProtocol protocol() { return DotAuthProtocol.OAUTH; }
    @Override public String appKey()            { return DotAuthConstants.APP_KEY; }
    @Override public List<String> secretKeys()  { return SECRET_KEYS; }
    @Override public Set<String> hiddenKeys()   { return HIDDEN_KEYS; }
    @Override public Set<String> booleanKeys()  { return BOOLEAN_KEYS; }

    @Override
    public Map<String, Object> maskedValues(final AppSecrets secrets) {
        final Map<String, Secret> raw = secrets.getSecrets();
        final Map<String, Object> out = new HashMap<>();
        for (final String key : SECRET_KEYS) {
            final Secret secret = raw.get(key);
            if (secret == null) continue;
            if (HIDDEN_KEYS.contains(key))  { out.put(key, HIDDEN_SECRET_MASK); continue; }
            if (BOOLEAN_KEYS.contains(key)) { out.put(key, Try.of(secret::getBoolean).getOrElse(false)); continue; }
            out.put(key, Try.of(secret::getString).getOrElse(""));
        }
        return out;
    }

    @Override
    public AppSecrets buildSecrets(final Map<String, Object> incoming,
                                   final Optional<AppSecrets> existing) {
        final AppSecrets.Builder builder = AppSecrets.builder().withKey(DotAuthConstants.APP_KEY);
        for (final String key : SECRET_KEYS) {
            final Object raw = incoming.get(key);
            if (HIDDEN_KEYS.contains(key)) {
                final String str = raw == null ? null : String.valueOf(raw);
                if (HIDDEN_SECRET_MASK.equals(str)) {
                    existing.map(AppSecrets::getSecrets)
                            .map(m -> m.get(key))
                            .ifPresent(secret -> builder.withSecret(key, secret));
                    continue;
                }
                if (str == null || str.isEmpty()) continue;
                builder.withHiddenSecret(key, str);
                continue;
            }
            if (raw == null) continue;
            if (BOOLEAN_KEYS.contains(key)) {
                builder.withSecret(key, Boolean.parseBoolean(String.valueOf(raw)));
            } else {
                builder.withSecret(key, String.valueOf(raw));
            }
        }
        return builder.build();
    }
}
```

**Step 4:** In `DotAuthResource.java`, remove the private `SECRET_KEYS`, `HIDDEN_KEYS`, `BOOLEAN_KEYS` constants and the `maskedValues()` private method. Leave the public constants `SYSTEM_HOST_SENTINEL` and `HIDDEN_SECRET_MASK` alone for now — Task 5 reshapes the dispatch. Add an instance field:

```java
private final OAuthProtocolHandler oauthHandler = new OAuthProtocolHandler();
```

Replace inline references to the removed constants/method with `oauthHandler.secretKeys()` etc. and `oauthHandler.maskedValues(...)` / `oauthHandler.buildSecrets(...)`. Existing endpoints should compile and behave identically.

**Step 5: Run tests — verify they PASS.**

Run: `./mvnw test -pl :dotcms-core -Dtest='OAuthProtocolHandlerTest,DotAuthResourceTest'`
Expected: all green. The existing `DotAuthResourceTest` must still pass without edits — this is a behaviour-preserving refactor.

**Step 6: Commit.**

```bash
git add dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/handler/OAuthProtocolHandler.java \
        dotCMS/src/test/java/com/dotcms/auth/dotAuth/rest/handler/OAuthProtocolHandlerTest.java \
        dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/DotAuthResource.java
git commit -m "refactor(auth): extract OAuthProtocolHandler from DotAuthResource"
```

---

## Task 4: Add `SamlProtocolHandler`

**Files:**
- Create: `dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/handler/SamlProtocolHandler.java`
- Create: `dotCMS/src/test/java/com/dotcms/auth/dotAuth/rest/handler/SamlProtocolHandlerTest.java`

Field list must match `dotCMS/src/main/resources/apps/dotsaml-config.yml` exactly (checked at Task 14 E2E).

**Step 1: Write the failing test.** Mirror `OAuthProtocolHandlerTest` shape:

```java
package com.dotcms.auth.dotAuth.rest.handler;

import com.dotcms.auth.dotAuth.rest.DotAuthProtocol;
import com.dotcms.saml.DotSamlProxyFactory;
import com.dotcms.security.apps.AppSecrets;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import static org.junit.Assert.*;

public class SamlProtocolHandlerTest {

    private final SamlProtocolHandler handler = new SamlProtocolHandler();

    @Test
    public void protocol_is_SAML() {
        assertEquals(DotAuthProtocol.SAML, handler.protocol());
    }

    @Test
    public void appKey_matches_DotSamlProxyFactory_constant() {
        assertEquals(DotSamlProxyFactory.SAML_APP_CONFIG_KEY, handler.appKey());
    }

    @Test
    public void secretKeys_match_dotsaml_config_yml_params() {
        assertEquals(
                java.util.List.of("enable", "idpName", "sPIssuerURL", "sPEndpointHostname",
                        "signatureValidationType", "idPMetadataFile", "publicCert",
                        "privateKey", "buttonParam"),
                handler.secretKeys());
    }

    @Test
    public void privateKey_is_hidden() {
        assertTrue(handler.hiddenKeys().contains("privateKey"));
    }

    @Test
    public void enable_is_boolean() {
        assertTrue(handler.booleanKeys().contains("enable"));
    }

    @Test
    public void maskedValues_replaces_privateKey_with_mask_and_unboxes_enable() {
        final AppSecrets secrets = AppSecrets.builder()
                .withKey(DotSamlProxyFactory.SAML_APP_CONFIG_KEY)
                .withHiddenSecret("privateKey", "PEM-content")
                .withSecret("enable", true)
                .withSecret("idpName", "Okta")
                .build();

        final Map<String, Object> out = handler.maskedValues(secrets);

        assertEquals("****", out.get("privateKey"));
        assertEquals(Boolean.TRUE, out.get("enable"));
        assertEquals("Okta", out.get("idpName"));
    }

    @Test
    public void buildSecrets_preserves_stored_privateKey_when_mask_posted_back() {
        final AppSecrets existing = AppSecrets.builder()
                .withKey(DotSamlProxyFactory.SAML_APP_CONFIG_KEY)
                .withHiddenSecret("privateKey", "stored-PEM")
                .build();

        final AppSecrets result = handler.buildSecrets(
                Map.of("privateKey", "****", "idpName", "Okta"),
                Optional.of(existing));

        assertEquals("stored-PEM", result.getSecrets().get("privateKey").getString());
        assertEquals("Okta", result.getSecrets().get("idpName").getString());
    }
}
```

**Step 2:** Run the test. Expected FAIL — class does not exist.
Run: `./mvnw test -pl :dotcms-core -Dtest=SamlProtocolHandlerTest`

**Step 3: Create the handler.**

```java
package com.dotcms.auth.dotAuth.rest.handler;

import com.dotcms.auth.dotAuth.rest.DotAuthProtocol;
import com.dotcms.saml.DotSamlProxyFactory;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.Secret;
import io.vavr.control.Try;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class SamlProtocolHandler implements ProtocolHandler {

    public static final String HIDDEN_SECRET_MASK = "****";

    /** Ordered to match the schema in dotsaml-config.yml. */
    private static final List<String> SAML_SECRET_KEYS = List.of(
            "enable",
            "idpName",
            "sPIssuerURL",
            "sPEndpointHostname",
            "signatureValidationType",
            "idPMetadataFile",
            "publicCert",
            "privateKey",
            "buttonParam");

    private static final Set<String> HIDDEN_KEYS = Set.of("privateKey");

    private static final Set<String> BOOLEAN_KEYS = Set.of("enable");

    @Override public DotAuthProtocol protocol() { return DotAuthProtocol.SAML; }
    @Override public String appKey()            { return DotSamlProxyFactory.SAML_APP_CONFIG_KEY; }
    @Override public List<String> secretKeys()  { return SAML_SECRET_KEYS; }
    @Override public Set<String> hiddenKeys()   { return HIDDEN_KEYS; }
    @Override public Set<String> booleanKeys()  { return BOOLEAN_KEYS; }

    @Override
    public Map<String, Object> maskedValues(final AppSecrets secrets) {
        final Map<String, Secret> raw = secrets.getSecrets();
        final Map<String, Object> out = new HashMap<>();
        for (final String key : SAML_SECRET_KEYS) {
            final Secret secret = raw.get(key);
            if (secret == null) continue;
            if (HIDDEN_KEYS.contains(key))  { out.put(key, HIDDEN_SECRET_MASK); continue; }
            if (BOOLEAN_KEYS.contains(key)) { out.put(key, Try.of(secret::getBoolean).getOrElse(false)); continue; }
            out.put(key, Try.of(secret::getString).getOrElse(""));
        }
        return out;
    }

    @Override
    public AppSecrets buildSecrets(final Map<String, Object> incoming,
                                   final Optional<AppSecrets> existing) {
        final AppSecrets.Builder builder = AppSecrets.builder().withKey(appKey());
        for (final String key : SAML_SECRET_KEYS) {
            final Object raw = incoming.get(key);
            if (HIDDEN_KEYS.contains(key)) {
                final String str = raw == null ? null : String.valueOf(raw);
                if (HIDDEN_SECRET_MASK.equals(str)) {
                    existing.map(AppSecrets::getSecrets)
                            .map(m -> m.get(key))
                            .ifPresent(secret -> builder.withSecret(key, secret));
                    continue;
                }
                if (str == null || str.isEmpty()) continue;
                builder.withHiddenSecret(key, str);
                continue;
            }
            if (raw == null) continue;
            if (BOOLEAN_KEYS.contains(key)) {
                builder.withSecret(key, Boolean.parseBoolean(String.valueOf(raw)));
            } else {
                builder.withSecret(key, String.valueOf(raw));
            }
        }
        return builder.build();
    }
}
```

**Step 4:** Rerun the test. Expected PASS.

**Step 5: Commit.**

```bash
git add dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/handler/SamlProtocolHandler.java \
        dotCMS/src/test/java/com/dotcms/auth/dotAuth/rest/handler/SamlProtocolHandlerTest.java
git commit -m "feat(auth): add SamlProtocolHandler with SAML_SECRET_KEYS"
```

---

## Task 5: Refactor `DotAuthResource` — dispatch on protocol, dual-key read/write/delete, MX

> **Depends on Task 6 (DTO shapes).** Execute Task 6 first even though it's numbered after — the `DotAuthConfigView` / `SiteRowView` / `DotAuthConfigForm` constructors referenced below don't exist until Task 6 adds them. The task list in `.tasks.json` encodes this dependency via `blockedBy`.

**Files:**
- Modify: `dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/DotAuthResource.java`
- Modify: `dotCMS/src/test/java/com/dotcms/auth/dotAuth/rest/DotAuthResourceTest.java` (if it exists; otherwise create)

**Step 1: Write the failing tests.** Add these to `DotAuthResourceTest`:

```java
// GET /sites — detects protocol per site
@Test
public void listSites_marks_site_with_dotAuth_row_as_OAUTH() { /* mock appKeysByHost returns host -> [dotauth] */ }

@Test
public void listSites_marks_site_with_dotsaml_config_row_as_SAML() { /* mock appKeysByHost returns host -> [dotsaml-config] */ }

@Test
public void listSites_marks_host_inherited_from_SAML_system_default() { /* only SYSTEM has dotsaml-config */ }

// GET /sites/{id} — returns matching protocol's values
@Test
public void getConfig_returns_SAML_values_for_SAML_configured_host() { /* ... */ }

// PUT /sites/{id} — mutual exclusion
@Test
public void saveConfig_with_OAUTH_deletes_existing_dotsaml_config_for_host() { /* verify appsAPI.deleteSecrets("dotsaml-config", host, user) called */ }

@Test
public void saveConfig_with_SAML_deletes_existing_dotAuth_for_host() { /* verify appsAPI.deleteSecrets("dotAuth", host, user) called */ }

// PUT missing protocol defaults to OAUTH (backward compat)
@Test
public void saveConfig_with_missing_protocol_defaults_to_OAUTH() { /* ... */ }

// DELETE clears both keys
@Test
public void clearConfig_deletes_both_dotAuth_and_dotsaml_config() { /* verify two deleteSecrets calls */ }
```

**Step 2: Run the tests, verify they FAIL.**
Run: `./mvnw test -pl :dotcms-core -Dtest=DotAuthResourceTest`
Expected: new tests FAIL; existing phase-2 tests still PASS.

**Step 3: Reshape `DotAuthResource`.** Key changes:

Add the handler map as a field:
```java
private final Map<DotAuthProtocol, ProtocolHandler> handlers;

public DotAuthResource() {
    this(new WebResource(), APILocator.getAppsAPI(),
         Map.of(DotAuthProtocol.OAUTH, new OAuthProtocolHandler(),
                DotAuthProtocol.SAML,  new SamlProtocolHandler()));
}

@VisibleForTesting
public DotAuthResource(final WebResource webResource,
                       final AppsAPI appsAPI,
                       final Map<DotAuthProtocol, ProtocolHandler> handlers) {
    this.webResource = webResource;
    this.appsAPI = appsAPI;
    this.handlers = handlers;
}
```

Rewrite `listSites` to inspect both keys per host:
```java
final Set<String> configuredAtSystem = appsByHost
        .getOrDefault(systemHost.getIdentifier().toLowerCase(), Set.of());
final DotAuthProtocol systemProtocol = detectProtocol(configuredAtSystem);
// emit SystemView with (configured, systemProtocol)

for (Host host : allHosts) {
    Set<String> hostKeys = appsByHost.getOrDefault(host.getIdentifier().toLowerCase(), Set.of());
    DotAuthProtocol hostProtocol = detectProtocol(hostKeys);
    if (hostProtocol != null) {
        rows.add(new SiteRowView(host.getIdentifier(), host.getHostname(),
                SITE_OVERRIDE, hostProtocol));
    } else if (systemProtocol != null) {
        rows.add(new SiteRowView(host.getIdentifier(), host.getHostname(),
                INHERITED, systemProtocol));
    } else {
        rows.add(new SiteRowView(host.getIdentifier(), host.getHostname(),
                NOT_CONFIGURED, null));
    }
}

private DotAuthProtocol detectProtocol(Set<String> keys) {
    for (ProtocolHandler h : handlers.values()) {
        if (keys.contains(h.appKey().toLowerCase())) return h.protocol();
    }
    return null;
}
```

Rewrite `getConfig` to try each handler:
```java
for (DotAuthProtocol p : DotAuthProtocol.values()) {
    ProtocolHandler h = handlers.get(p);
    Optional<AppSecrets> own = appsAPI.getSecrets(h.appKey(), false, host, user);
    if (own.isPresent()) {
        return Response.ok(new ResponseEntityDotAuthConfigView(
                new DotAuthConfigView(hostId, p, true, false, h.maskedValues(own.get())))).build();
    }
}
// Fall through to SYSTEM_HOST inheritance, then NOT_CONFIGURED default to OAUTH
if (!host.isSystemHost()) {
    for (DotAuthProtocol p : DotAuthProtocol.values()) {
        ProtocolHandler h = handlers.get(p);
        Optional<AppSecrets> sys = appsAPI.getSecrets(h.appKey(), false, APILocator.systemHost(), user);
        if (sys.isPresent()) {
            return Response.ok(new ResponseEntityDotAuthConfigView(
                    new DotAuthConfigView(hostId, p, false, true, h.maskedValues(sys.get())))).build();
        }
    }
}
return Response.ok(new ResponseEntityDotAuthConfigView(
        new DotAuthConfigView(hostId, DotAuthProtocol.OAUTH, false, false, Map.of()))).build();
```

Rewrite `saveConfig` with MX:
```java
final DotAuthProtocol chosen = Optional.ofNullable(form.getProtocol()).orElse(DotAuthProtocol.OAUTH);
final ProtocolHandler active = handlers.get(chosen);
final ProtocolHandler other  = handlers.get(chosen == DotAuthProtocol.OAUTH
        ? DotAuthProtocol.SAML : DotAuthProtocol.OAUTH);

// Delete other-protocol key (no-op if absent)
appsAPI.deleteSecrets(other.appKey(), host, user);

// Build + save chosen
final Optional<AppSecrets> existing = appsAPI.getSecrets(active.appKey(), false, host, user);
appsAPI.saveSecrets(active.buildSecrets(
        form.getValues() == null ? Map.of() : form.getValues(), existing), host, user);
```

Rewrite `clearConfig` to delete both keys:
```java
for (ProtocolHandler h : handlers.values()) {
    appsAPI.deleteSecrets(h.appKey(), host, user);
}
```

**Step 4: Run the tests, verify PASS.**
Run: `./mvnw test -pl :dotcms-core -Dtest=DotAuthResourceTest`
Expected: all green (existing + new).

**Step 5: Commit.**

```bash
git add dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/DotAuthResource.java \
        dotCMS/src/test/java/com/dotcms/auth/dotAuth/rest/DotAuthResourceTest.java
git commit -m "feat(auth): DotAuthResource dispatches on protocol; dual-key read/write/delete + MX"
```

---

## Task 6: Add `protocol` field to REST DTOs

> **Execute before Task 5** — Task 5's refactor depends on the constructor shapes this task introduces.

**Files:**
- Modify: `dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/DotAuthSitesView.java`
- Modify: `dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/DotAuthConfigView.java`
- Modify: `dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/DotAuthConfigForm.java`

**Step 1: `DotAuthSitesView.SystemView`** — add `protocol`:

```java
public static class SystemView {
    private final boolean configured;
    private final DotAuthProtocol protocol; // nullable

    public SystemView(final boolean configured, final DotAuthProtocol protocol) {
        this.configured = configured;
        this.protocol = protocol;
    }
    public boolean isConfigured() { return configured; }
    public DotAuthProtocol getProtocol() { return protocol; }
}
```

**Step 2: `DotAuthSitesView.SiteRowView`** — add `protocol`:

```java
public static class SiteRowView {
    private final String hostId;
    private final String hostName;
    private final DotAuthSiteStatus status;
    private final DotAuthProtocol protocol; // nullable when status == NOT_CONFIGURED

    public SiteRowView(final String hostId, final String hostName,
                       final DotAuthSiteStatus status, final DotAuthProtocol protocol) {
        this.hostId = hostId;
        this.hostName = hostName;
        this.status = status;
        this.protocol = protocol;
    }
    /* getters ... */
    public DotAuthProtocol getProtocol() { return protocol; }
}
```

**Step 3: `DotAuthConfigView`** — add `protocol`:

```java
public class DotAuthConfigView {
    private final String hostId;
    private final DotAuthProtocol protocol;
    private final boolean configured;
    private final boolean inherited;
    private final Map<String, Object> values;

    public DotAuthConfigView(final String hostId,
                             final DotAuthProtocol protocol,
                             final boolean configured,
                             final boolean inherited,
                             final Map<String, Object> values) {
        this.hostId = hostId;
        this.protocol = protocol;
        this.configured = configured;
        this.inherited = inherited;
        this.values = values;
    }
    /* getters ... */
    public DotAuthProtocol getProtocol() { return protocol; }
}
```

**Step 4: `DotAuthConfigForm`** — add `protocol` (optional, defaults to OAUTH):

```java
public class DotAuthConfigForm extends Validated {

    private final DotAuthProtocol protocol;

    @NotNull
    private final Map<String, Object> values;

    @JsonCreator
    public DotAuthConfigForm(@JsonProperty("protocol") final DotAuthProtocol protocol,
                             @JsonProperty("values")   final Map<String, Object> values) {
        this.protocol = protocol == null ? DotAuthProtocol.OAUTH : protocol;
        this.values = values;
    }

    public DotAuthProtocol getProtocol() { return protocol; }
    public Map<String, Object> getValues() { return values; }
}
```

**Step 5:** Update `DotAuthResource` call sites constructing these DTOs to pass the new `protocol` argument (Task 5 already assumes these shapes — this task closes the gap).

**Step 6: Run backend tests.**
Run: `./mvnw test -pl :dotcms-core -Dtest='DotAuthResourceTest,OAuthProtocolHandlerTest,SamlProtocolHandlerTest'`
Expected: PASS.

**Step 7: Commit.**

```bash
git add dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/DotAuthSitesView.java \
        dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/DotAuthConfigView.java \
        dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/DotAuthConfigForm.java \
        dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/DotAuthResource.java
git commit -m "feat(auth): add protocol field to DotAuth REST DTOs"
```

---

## Task 7: Widen frontend models (discriminated union + SAML values)

**Files:**
- Modify: `core-web/libs/dotcms-models/src/lib/dot-auth.model.ts`

**Step 1:** Replace the file's current contents with:

```typescript
/**
 * dotAuth portlet models — mirror the REST surface at /api/v1/dotauth.
 */

/** Sentinel host id representing the SYSTEM_HOST / global default row. */
export const DOT_AUTH_SYSTEM_HOST = 'SYSTEM_HOST';

/** Value returned for hidden secrets. Posting it back preserves the stored secret. */
export const DOT_AUTH_HIDDEN_SECRET_MASK = '****';

export type DotAuthStatus = 'SITE_OVERRIDE' | 'INHERITED' | 'NOT_CONFIGURED';

export type DotAuthProtocol = 'OAUTH' | 'SAML';

export interface DotAuthSystemView {
    configured: boolean;
    protocol: DotAuthProtocol | null;
}

export interface DotAuthSiteRow {
    hostId: string;
    hostName: string;
    status: DotAuthStatus;
    protocol: DotAuthProtocol | null;
}

export interface DotAuthSitesView {
    system: DotAuthSystemView;
    sites: DotAuthSiteRow[];
}

/** OAuth / OIDC field values — mirrors OAuthAppConfig.KEY_*. */
export interface DotAuthConfigValues {
    enabled?: boolean;
    enableBackend?: boolean;
    enableFrontend?: boolean;
    providerType?: 'OIDC' | 'OAuth2';
    issuerUrl?: string;
    clientId?: string;
    clientSecret?: string;
    scopes?: string;
    authorizationUrl?: string;
    tokenUrl?: string;
    userinfoUrl?: string;
    revocationUrl?: string;
    logoutUrl?: string;
    groupsClaim?: string;
    groupsUrl?: string;
    extraRoles?: string;
    callbackUrl?: string;
}

export type DotAuthSignatureValidation = 'none' | 'response' | 'assertion' | 'responseandassertion';

/** SAML field values — mirrors SamlProtocolHandler.SAML_SECRET_KEYS / dotsaml-config.yml. */
export interface DotAuthSamlConfigValues {
    enable?: boolean;
    idpName?: string;
    sPIssuerURL?: string;
    sPEndpointHostname?: string;
    signatureValidationType?: DotAuthSignatureValidation;
    idPMetadataFile?: string;
    publicCert?: string;
    privateKey?: string;
    buttonParam?: string;
}

export type DotAuthConfigView =
    | { hostId: string; protocol: 'OAUTH'; configured: boolean; inherited: boolean; values: DotAuthConfigValues }
    | { hostId: string; protocol: 'SAML';  configured: boolean; inherited: boolean; values: DotAuthSamlConfigValues };

export type DotAuthConfigPayload =
    | { protocol: 'OAUTH'; values: DotAuthConfigValues }
    | { protocol: 'SAML';  values: DotAuthSamlConfigValues };
```

**Step 2: Run typecheck against dependents.**
Run: `yarn nx build dotcms-models && yarn nx lint dotcms-models`
Expected: both pass.

**Step 3: Commit.**

```bash
git add core-web/libs/dotcms-models/src/lib/dot-auth.model.ts
git commit -m "feat(dot-auth): widen models to discriminated union on protocol"
```

---

## Task 8: Update data-access service types

**Files:**
- Modify: `core-web/libs/data-access/src/lib/dot-auth/dot-auth.service.ts`

**Step 1:** Service method signatures take the new union types. No URL changes.

```typescript
getConfig(hostId: string): Observable<DotAuthConfigView> {
    return this.http
        .get<ResponseView<DotAuthConfigView>>(`${this.endpoint}/sites/${hostId}`)
        .pipe(pluck('entity'));
}

saveConfig(hostId: string, payload: DotAuthConfigPayload): Observable<string> {
    return this.http
        .put<ResponseView<string>>(`${this.endpoint}/sites/${hostId}`, payload)
        .pipe(pluck('entity'));
}
```

`listSites()` signature unchanged — `DotAuthSitesView` typedef already widened in Task 7.

**Step 2: Run tests.**
Run: `yarn nx test data-access --testPathPattern=dot-auth.service`
Expected: PASS (add a SAML fixture case if the test doesn't already exercise the union — the existing test should still work with the wider types because it casts/destructures OAuth-only fields).

**Step 3: Commit.**

```bash
git add core-web/libs/data-access/src/lib/dot-auth/dot-auth.service.ts
git commit -m "feat(dot-auth): widen DotAuthService types to discriminated union"
```

---

## Task 9: Add Protocol column + composed status tag to list

**Files:**
- Modify: `core-web/libs/portlets/dot-auth/src/lib/dot-auth-list/dot-auth-list.component.ts`
- Modify: `core-web/libs/portlets/dot-auth/src/lib/dot-auth-list/dot-auth-list.component.html`

**Step 1:** In `dot-auth-list.component.ts`, replace `statusTag(status)` with the protocol-aware version:

```typescript
statusTag(status: DotAuthStatus, protocol: DotAuthProtocol | null): StatusTag {
    if (status === 'NOT_CONFIGURED') {
        return { labelKey: 'dotauth.status.not-configured', severity: 'secondary' };
    }
    const severity: 'success' | 'info' = protocol === 'OAUTH' ? 'success' : 'info';
    const labelKey = status === 'SITE_OVERRIDE'
        ? 'dotauth.status.site-override'
        : 'dotauth.status.inherited';
    return { labelKey, severity };
}
```

Update `systemStatusTag` to take protocol too:

```typescript
readonly systemStatusTag = computed<StatusTag>(() => {
    const sys = this.store.system();
    if (!sys.configured) {
        return { labelKey: 'dotauth.status.not-configured', severity: 'secondary' };
    }
    const severity: 'success' | 'info' = sys.protocol === 'OAUTH' ? 'success' : 'info';
    return { labelKey: 'dotauth.status.configured', severity };
});
```

Add a helper for the protocol cell:

```typescript
protocolLabelKey(protocol: DotAuthProtocol | null): string | null {
    if (!protocol) return null;
    return protocol === 'OAUTH' ? 'dotauth.protocol.oauth' : 'dotauth.protocol.saml';
}
```

**Step 2:** Template updates in `dot-auth-list.component.html`:

- Add a new `<th>` between Site and Status: `<th>{{ 'dotauth.column.protocol' | dm }}</th>` (plus matching column in the system row).
- Add the protocol `<td>` rendering:

```html
<td>
  @if (row.protocol) {
    <span class="font-medium" data-testid="dotauth-protocol-cell">
      {{ protocolLabelKey(row.protocol) | dm }}
    </span>
  } @else {
    <span class="text-color-secondary">—</span>
  }
</td>
```

- Update the status-tag invocation: `statusTag(row.status, row.protocol)`; for the system row use `store.system().protocol`.

**Step 3: Run lint.**
Run: `yarn nx lint portlets-dot-auth-portlet`
Expected: PASS.

**Step 4: Commit.**

```bash
git add core-web/libs/portlets/dot-auth/src/lib/dot-auth-list/dot-auth-list.component.ts \
        core-web/libs/portlets/dot-auth/src/lib/dot-auth-list/dot-auth-list.component.html
git commit -m "feat(dot-auth): protocol column + composed status tag in list"
```

---

## Task 10: Update list + store tests

**Files:**
- Modify: `core-web/libs/portlets/dot-auth/src/lib/dot-auth-list/store/dot-auth-list.store.spec.ts`
- Modify: `core-web/libs/portlets/dot-auth/src/lib/dot-auth-list/dot-auth-list.component.spec.ts`

**Step 1:** Widen list fixtures to include a SAML row and a protocol on the system:

```typescript
const fixture: DotAuthSitesView = {
    system: { configured: true, protocol: 'SAML' },
    sites: [
        { hostId: '1', hostName: 'a.example',  status: 'SITE_OVERRIDE', protocol: 'OAUTH' },
        { hostId: '2', hostName: 'b.example',  status: 'INHERITED',     protocol: 'SAML'  },
        { hostId: '3', hostName: 'c.example',  status: 'NOT_CONFIGURED', protocol: null   }
    ]
};
```

**Step 2:** Add component tests:

```typescript
it('renders Protocol cell for each configured row', () => {
    const cells = spectator.queryAll(byTestId('dotauth-protocol-cell'));
    expect(cells.length).toBe(2); // two configured rows
    expect(cells[0].textContent?.trim()).toContain('OAuth');
    expect(cells[1].textContent?.trim()).toContain('SAML');
});

it('uses `info` severity tag for SAML rows', () => {
    const samlRow = spectator.queryLast(byTestId('dotauth-status-tag'));
    expect(samlRow?.getAttribute('ng-reflect-severity')).toBe('info');
});
```

**Step 3: Run tests.**
Run: `yarn nx test portlets-dot-auth-portlet --testPathPattern=dot-auth-list`
Expected: PASS.

**Step 4: Commit.**

```bash
git add core-web/libs/portlets/dot-auth/src/lib/dot-auth-list/store/dot-auth-list.store.spec.ts \
        core-web/libs/portlets/dot-auth/src/lib/dot-auth-list/dot-auth-list.component.spec.ts
git commit -m "test(dot-auth): SAML coverage for list component + store"
```

---

## Task 11: Protocol toggle + SAML fieldsets in edit dialog

**Files:**
- Modify: `core-web/libs/portlets/dot-auth/src/lib/dot-auth-edit/dot-auth-edit.component.ts`
- Modify: `core-web/libs/portlets/dot-auth/src/lib/dot-auth-edit/dot-auth-edit.component.html`

**Step 1:** Add the `SelectButtonModule` import; add `ConfirmationService` to the providers array of the host list component (or use local `ConfirmDialogModule` inside the dialog).

**Step 2:** In the `.ts` file, introduce the second form and a signal-backed protocol selector:

```typescript
readonly selectedProtocol = signal<DotAuthProtocol>('OAUTH');
private readonly initialProtocol = signal<DotAuthProtocol | null>(null);

readonly oauthForm: FormGroup = this.fb.group({ /* existing 17 controls, unchanged */ });

readonly samlForm: FormGroup = this.fb.group({
    enable: [true],
    idpName: ['', Validators.required],
    sPIssuerURL: ['', Validators.required],
    sPEndpointHostname: ['', Validators.required],
    signatureValidationType: ['none' as DotAuthSignatureValidation],
    idPMetadataFile: ['', Validators.required],
    publicCert: ['', Validators.required],
    privateKey: ['', Validators.required],
    buttonParam: ['/api/v1/dotsaml/metadata/$siteId']
});

readonly activeForm = computed(() =>
    this.selectedProtocol() === 'OAUTH' ? this.oauthForm : this.samlForm);

readonly protocolOptions = [
    { label: 'OAuth 2.0 / OIDC', value: 'OAUTH' as const },
    { label: 'SAML 2.0',          value: 'SAML'  as const }
];

readonly signatureValidationOptions = [
    { labelKey: 'dotauth.field.sigvalidation.none',                  value: 'none'                  as const },
    { labelKey: 'dotauth.field.sigvalidation.response',              value: 'response'              as const },
    { labelKey: 'dotauth.field.sigvalidation.assertion',             value: 'assertion'             as const },
    { labelKey: 'dotauth.field.sigvalidation.responseandassertion',  value: 'responseandassertion'  as const }
];
```

**Step 3:** Rewire `ngOnInit` to switch on the view's protocol:

```typescript
ngOnInit(): void {
    this.service.getConfig(this.hostId).pipe(take(1), takeUntilDestroyed(this.destroyRef))
        .subscribe((view) => {
            this.inherited.set(view.inherited);
            this.selectedProtocol.set(view.protocol);
            this.initialProtocol.set(view.configured ? view.protocol : null);
            if (view.protocol === 'OAUTH') {
                this.oauthForm.patchValue(view.values ?? {});
                this.applyProviderValidators(
                    (view.values?.providerType as 'OIDC' | 'OAuth2') ?? 'OIDC');
            } else {
                this.samlForm.patchValue(view.values ?? {});
            }
            this.loading.set(false);
        });
    // Keep the existing providerType-watcher on oauthForm.
}
```

**Step 4:** Add protocol-switch confirmation:

```typescript
onProtocolChange(event: { value: DotAuthProtocol }): void {
    const currentlyDirty = this.activeForm().dirty
        && this.initialProtocol() !== null
        && this.initialProtocol() === this.selectedProtocol();
    const target = event.value;

    if (!currentlyDirty || target === this.selectedProtocol()) {
        this.selectedProtocol.set(target);
        return;
    }

    this.confirmationService.confirm({
        header:  this.dotMessageService.get('dotauth.confirm.switch-protocol.header'),
        message: this.dotMessageService.get('dotauth.confirm.switch-protocol.message',
                    this.labelFor(target), this.labelFor(this.selectedProtocol())),
        acceptLabel: this.dotMessageService.get('Continue'),
        rejectLabel: this.dotMessageService.get('Cancel'),
        accept:  () => this.selectedProtocol.set(target),
        reject:  () => {/* ngModel two-way binding reverts the selectButton */}
    });
}

private labelFor(p: DotAuthProtocol): string {
    return this.dotMessageService.get(
        p === 'OAUTH' ? 'dotauth.protocol.oauth' : 'dotauth.protocol.saml');
}
```

**Step 5:** Rewrite `save()` to emit the union payload:

```typescript
save(): void {
    const form = this.activeForm();
    if (form.invalid) {
        form.markAllAsTouched();
        return;
    }
    const payload: DotAuthConfigPayload = this.selectedProtocol() === 'OAUTH'
        ? { protocol: 'OAUTH', values: form.getRawValue() as DotAuthConfigValues }
        : { protocol: 'SAML',  values: form.getRawValue() as DotAuthSamlConfigValues };
    this.dialogRef.close(payload);
}
```

**Step 6:** Template — add `p-selectButton` at the top and swap fieldsets:

```html
<p-selectButton
    [options]="protocolOptions"
    [(ngModel)]="selectedProtocolValue"    <!-- model-signal bridge -->
    (onChange)="onProtocolChange($event)"
    optionLabel="label"
    optionValue="value"
    data-testid="dotauth-protocol-toggle" />

@if (inherited()) {
    <p-message severity="info" text="{{ 'dotauth.dialog.inherited-banner' | dm }}" />
}

@if (selectedProtocol() === 'OAUTH') {
    <form [formGroup]="oauthForm" class="form">
        <!-- existing phase-2 fieldsets, untouched -->
    </form>
} @else {
    <form [formGroup]="samlForm" class="form">
        <p-fieldset legend="{{ 'dotauth.fieldset.saml.idp' | dm }}">
            <div class="field">
                <label for="idpName">{{ 'dotauth.field.idpName' | dm }}</label>
                <input pInputText id="idpName" formControlName="idpName"
                       data-testid="saml-idp-name" />
            </div>
            <div class="field">
                <label for="idPMetadataFile">{{ 'dotauth.field.idPMetadataFile' | dm }}</label>
                <textarea pInputTextarea id="idPMetadataFile" formControlName="idPMetadataFile"
                          rows="8" data-testid="saml-idp-metadata"></textarea>
            </div>
            <div class="field">
                <label for="signatureValidationType">{{ 'dotauth.field.signatureValidationType' | dm }}</label>
                <p-select id="signatureValidationType" formControlName="signatureValidationType"
                          [options]="signatureValidationOptions"
                          optionLabel="labelKey" optionValue="value"
                          data-testid="saml-sigvalidation">
                    <ng-template pTemplate="item" let-opt>{{ opt.labelKey | dm }}</ng-template>
                    <ng-template pTemplate="selectedItem" let-opt>{{ opt.labelKey | dm }}</ng-template>
                </p-select>
            </div>
        </p-fieldset>

        <p-fieldset legend="{{ 'dotauth.fieldset.saml.sp' | dm }}">
            <div class="field">
                <label for="sPIssuerURL">{{ 'dotauth.field.sPIssuerURL' | dm }}</label>
                <input pInputText id="sPIssuerURL" formControlName="sPIssuerURL"
                       data-testid="saml-sp-issuer" />
            </div>
            <div class="field">
                <label for="sPEndpointHostname">{{ 'dotauth.field.sPEndpointHostname' | dm }}</label>
                <input pInputText id="sPEndpointHostname" formControlName="sPEndpointHostname"
                       data-testid="saml-sp-endpoint" />
            </div>
            <div class="field">
                <label for="buttonParam">{{ 'dotauth.field.buttonParam' | dm }}</label>
                <input pInputText id="buttonParam" formControlName="buttonParam"
                       data-testid="saml-button-param" />
            </div>
        </p-fieldset>

        <p-fieldset legend="{{ 'dotauth.fieldset.saml.credentials' | dm }}">
            <div class="field">
                <label for="publicCert">{{ 'dotauth.field.publicCert' | dm }}</label>
                <textarea pInputTextarea id="publicCert" formControlName="publicCert"
                          rows="6" data-testid="saml-public-cert"></textarea>
            </div>
            <div class="field">
                <label for="privateKey">{{ 'dotauth.field.privateKey' | dm }}</label>
                <p-password id="privateKey" formControlName="privateKey"
                            [feedback]="false" [toggleMask]="true"
                            data-testid="saml-private-key" />
            </div>
        </p-fieldset>
    </form>
}
```

Add a `selectedProtocolValue` getter/setter or two-way-signal wiring so `ngModel` on `p-selectButton` participates in the signal-based state.

**Step 7: Run lint.**
Run: `yarn nx lint portlets-dot-auth-portlet`
Expected: PASS.

**Step 8: Commit.**

```bash
git add core-web/libs/portlets/dot-auth/src/lib/dot-auth-edit/dot-auth-edit.component.ts \
        core-web/libs/portlets/dot-auth/src/lib/dot-auth-edit/dot-auth-edit.component.html
git commit -m "feat(dot-auth): protocol toggle + SAML fieldsets in edit dialog"
```

---

## Task 12: Update edit component tests

**Files:**
- Modify: `core-web/libs/portlets/dot-auth/src/lib/dot-auth-edit/dot-auth-edit.component.spec.ts`

**Step 1:** Two `describe` blocks.

`describe('when protocol = OAUTH', ...)` — lift phase-2 tests; ensure they pass with the new fixture shape (`view.protocol === 'OAUTH'`).

`describe('when protocol = SAML', ...)` — new tests:

```typescript
it('pre-populates SAML fieldset from SAML values', () => {
    // mock service getConfig returning { protocol: 'SAML', values: { idpName: 'Okta', ... } }
    expect(spectator.query(byTestId('saml-idp-name'))?.value).toBe('Okta');
});

it('emits a SAML payload on save', () => {
    const ref = spectator.inject(DynamicDialogRef);
    spectator.typeInElement('Okta', byTestId('saml-idp-name'));
    // fill remaining required fields
    spectator.click(byTestId('dotauth-save-btn'));
    expect(ref.close).toHaveBeenCalledWith({ protocol: 'SAML', values: expect.objectContaining({ idpName: 'Okta' }) });
});

it('replaces the stored privateKey with **** and posts that back untouched', () => {
    // when view is loaded with privateKey === '****', user doesn't touch it,
    // the saved payload must carry '****' so the backend preserves the stored secret
});

it('confirms before switching from a dirty OAuth form to SAML', () => {
    const confirm = spectator.inject(ConfirmationService);
    spectator.typeInElement('my-id', byTestId('dotauth-client-id'));
    // toggle protocol
    expect(confirm.confirm).toHaveBeenCalled();
});
```

**Step 2: Run tests.**
Run: `yarn nx test portlets-dot-auth-portlet --testPathPattern=dot-auth-edit`
Expected: PASS.

**Step 3: Commit.**

```bash
git add core-web/libs/portlets/dot-auth/src/lib/dot-auth-edit/dot-auth-edit.component.spec.ts
git commit -m "test(dot-auth): SAML + protocol-switch coverage for edit component"
```

---

## Task 13: Add SAML i18n keys

**Files:**
- Modify: `dotCMS/src/main/webapp/WEB-INF/messages/Language.properties`

**Step 1:** Append to the end of the phase-2 dotAuth block:

```properties
# dotAuth phase 3 — SAML
dotauth.column.protocol=Protocol
dotauth.protocol.oauth=OAuth 2.0 / OIDC
dotauth.protocol.saml=SAML 2.0

dotauth.confirm.switch-protocol.header=Switch protocol?
dotauth.confirm.switch-protocol.message=Saving as {0} will delete the existing {1} configuration for this site. Continue?

dotauth.fieldset.saml.idp=IDP Details
dotauth.fieldset.saml.sp=SP Details
dotauth.fieldset.saml.credentials=Credentials

dotauth.field.enable=Enabled
dotauth.field.idpName=IDP Name
dotauth.field.sPIssuerURL=Service Provider Issuer ID
dotauth.field.sPEndpointHostname=Service Provider Endpoint Hostname/Port
dotauth.field.signatureValidationType=Signature Validation
dotauth.field.idPMetadataFile=IDP Metadata XML
dotauth.field.publicCert=Public Certificate
dotauth.field.privateKey=Private Key
dotauth.field.buttonParam=Metadata URL

dotauth.field.sigvalidation.none=None
dotauth.field.sigvalidation.response=Only Response
dotauth.field.sigvalidation.assertion=Only Assertion
dotauth.field.sigvalidation.responseandassertion=Response & Assertion
```

**Step 2: Commit.**

```bash
git add dotCMS/src/main/webapp/WEB-INF/messages/Language.properties
git commit -m "feat(dot-auth): i18n keys for SAML protocol + fieldsets"
```

---

## Task 14: CHECKPOINT — build, lint, Okta E2E, upgrade smoke

No code changes. This is the manual verification gate before the YAML-delete commit merges.

**Step 1: Full backend + frontend build + lint + test pass.**

```bash
./mvnw install -pl :dotcms-core --am -DskipTests
./mvnw test -pl :dotcms-core -Dtest='com.dotcms.auth.dotAuth.**'
yarn nx lint portlets-dot-auth-portlet data-access dotcms-models
yarn nx test portlets-dot-auth-portlet data-access
yarn nx build dotcms-ui
```

Expected: all green.

**Step 2: Okta E2E on `just dev-run-fixed`.** Follow the 5-step E2E checklist in the design doc. The Okta tenant + self-signed cert from the design phase are already set up locally.

1. Portlet → SYSTEM_HOST → SAML → paste Okta IDP metadata → save → row shows "SAML · Configured".
2. `/dotAdmin/` → Okta login → signed in.
3. Portlet → `default` → view shows inherited SAML + banner → save → row flips to "SAML · Site override".
4. Portlet → `default` → toggle OAuth → confirm dialog warns → enter OIDC fields → save → "OAuth · Site override"; SAML row for `default` gone.
5. Portlet → Clear `default` → "Not configured"; verify via REST both keys deleted.

**Step 3: Upgrade smoke.** On a DB with a pre-existing `dotsaml-config` SYSTEM_HOST row (simulate by editing via Apps UI before switching to this branch, or load a snapshot), boot this branch. Open portlet. Expect SYSTEM_HOST row shows "SAML · Configured" with the correct values — no click needed, no data loss.

**If any step fails:** stop. Tasks 15 is gated on this. Fix forward with a new task (or revert the offending prior commit).

**Step 4: No commit** — this is a verification gate only.

---

## Task 15: Remove `dotsaml-config.yml` and temp debug endpoint

**Files:**
- Delete: `dotCMS/src/main/resources/apps/dotsaml-config.yml`
- Modify: `dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/DotAuthResource.java` (remove the `/v1/dotauth/debug` method)

**Step 1: Gate check.** Confirm Task 14 passed cleanly. If it didn't, do not run this task.

**Step 2:** Delete the YAML.

```bash
git rm dotCMS/src/main/resources/apps/dotsaml-config.yml
```

**Step 3:** Remove the `debug()` method from `DotAuthResource` and any unused imports it needed.

**Step 4: Run backend tests once more.**

```bash
./mvnw test -pl :dotcms-core -Dtest='com.dotcms.auth.dotAuth.**'
```

Expected: PASS. The runtime SAML classes don't reference the YAML descriptor — `DotIdentityProviderConfigurationImpl` reads secrets by key only.

**Step 5: Commit as two atomic commits** so either can be cherry-picked if a reviewer wants to keep the debug endpoint for longer.

```bash
git add dotCMS/src/main/resources/apps/dotsaml-config.yml
git commit -m "chore(auth): remove dotsaml-config.yml; dotAuth portlet is sole SAML editor"

git add dotCMS/src/main/java/com/dotcms/auth/dotAuth/rest/DotAuthResource.java
git commit -m "chore(auth): remove temp /v1/dotauth/debug endpoint"
```

---

## Done

- 15 tasks, ~15 commits, one PR on `feat/dotauth-saml-integration`.
- Backend delta: 4 new files, 4 modified files.
- Frontend delta: 1 new type file, 4 modified components / stores.
- Verification: unit tests per handler + resource; component tests per list/edit; Okta E2E + upgrade smoke manual gate before the destructive final commits.
