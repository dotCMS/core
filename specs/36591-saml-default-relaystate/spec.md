# Issue Resolution Specification: SAML front-end users return to the page they requested after login (default RelayState + same-host redirect validation)

**Feature Branch**: `issue-36591-saml-default-relaystate-spec` (spec dir `specs/36591-saml-default-relaystate`)

**Created**: 2026-10-02

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#36591](https://github.com/dotCMS/core/issues/36591), item 1 only. Item 2, the logout redirect fall-through, already shipped in PR #36590 (commit `8e2a308ebf`). Refs #32365, #36541.

**Input**: User description: "https://github.com/dotCMS/core/issues/36591 — The original GitHub issue included 2 items, but item 2 was already included in PR 36590, so we can go ahead with item 1. For Frontend Users we want a default RelayState that redirects users to the original request URI in case no RelayState is set, so the return URL survives the round-trip via the SAML RelayState param. Let's make sure we don't introduce regression bugs in our SAML authentication/interceptor when doing this change, and also let's double check this doesn't introduce any security vulnerability via redirect. We should include redirects relative to the same host, not to a different host."

## Problem Statement *(mandatory)*

A front-end visitor on a SAML-protected site opens a protected page, for example `/members/report?id=42`. dotCMS sends them to the IdP (Entra ID, Okta, …), they sign in, and the IdP posts them back to dotCMS. At that point dotCMS has forgotten which page they asked for. Instead of returning them to `/members/report?id=42`, it sends them to a default destination:

- dotCMS only sends a RelayState to the IdP when the SAML app has an `auth.relaystate` template configured. Most sites do not have one, so the RelayState is empty and the IdP has nothing to echo back.
- dotCMS also stores the requested page in the HTTP session (`REDIRECT_AFTER_LOGIN` / `original_request`). That copy is usually gone by the time the callback arrives, because the IdP's cross-site POST is sent without the session cookie (SameSite rules) and a new session is created.
- With no `redirect.after.login` configured, the callback falls back to `/dotAdmin/`. PR #36590 limited the damage: front-end-only users now land on `/` (when front-end SSO is enabled) or get a 403 "Access Denied" page instead of a redirect loop. They still do not reach the page they asked for.

**Severity / Impact**: Medium. This affects every front-end SAML user on a site without an `auth.relaystate` template or `redirect.after.login` setting, which is the default configuration, on every sign-in that starts from a deep link. Users must navigate back by hand, and links shared by email or bookmarked are effectively broken for signed-out users. Nothing is lost and nothing is exposed; the cost is usability and support tickets. A workaround exists, but it can only send everyone to one fixed URL: set `redirect.after.login` or an `auth.relaystate` template.

## Reproduction *(mandatory)*

**Environment**: Current `main` (after PR #36590). A site with the SAML app enabled, front-end SSO enabled, **no** `auth.relaystate` and **no** `redirect.after.login` in the app config, a real IdP (Entra ID or Okta), and a modern browser with default SameSite cookie behavior.

**Steps to Reproduce**:

1. In a private window (signed out), open a front-end page that requires login, with a query string, e.g. `https://site.example.com/members/report?id=42`.
2. The browser is redirected to the IdP. The outgoing AuthnRequest carries no `RelayState`.
3. Sign in at the IdP as a front-end-only user (no back-end role).
4. The IdP posts the assertion to `/api/v1/dotsaml/login/{siteId}` (`DotSamlResource.processLogin`).

**Expected Behavior**: After the callback, the user is signed in and lands on `https://site.example.com/members/report?id=42`, the exact page and query string they requested.

**Actual Behavior**: The callback receives no `RelayState`, finds no `REDIRECT_AFTER_LOGIN` in its new session, and falls back to `/dotAdmin/`. Because the login intent is unknown, a front-end-only user is then rerouted to `/` (front-end SSO enabled) or shown the 403 no-access page. A user with a back-end role lands in `/dotAdmin/`. Nobody is returned to `/members/report?id=42`.

**Reproducibility**: Always, in the default configuration with a browser that drops the session cookie on the cross-site IdP POST. When the session does survive (same-site IdP, lax cookie settings), the existing `REDIRECT_AFTER_LOGIN` fallback works, which is why the bug can look intermittent across environments.

## Scope of Investigation *(mandatory)*

- **Affected area**: SAML authentication (dotAuth / SAML app). There are two steps: (a) starting a login, where the request is intercepted and sent to the IdP with a RelayState, and (b) the IdP callback, which signs the user in and redirects them.
- **Suspected surface**: Modern code (`com.dotcms.*`):
  - `com.dotcms.filters.interceptor.saml.SamlWebInterceptor#doAuthentication` computes the original request and the RelayState. The PR #36590 loop guard in `intercept` is in the same class.
  - `com.dotcms.filters.interceptor.saml.SamlWebUtils#getRelayState` / `evalRelayState` evaluates the `auth.relaystate` Velocity template and the per-site `RelayStateStrategy` registry (`addRelayStateStrategy`), which plugins use.
  - `com.dotcms.auth.providers.saml.v1.DotSamlResource#processLogin` resolves the destination: RelayState → session `REDIRECT_AFTER_LOGIN` → `redirect.after.login` → `/dotAdmin/`. It also contains the `loginIntentUnknown` reroute and the back-end role gate.
  - `com.dotcms.util.RedirectUtil#sendRedirectHTML` writes the destination into an HTML meta-refresh page without escaping it.
  - There is no legacy `com.dotmarketing.*` change expected. `CMSUrlUtil` and `SecurityUtils.sendPermissionDenied` (from PR #36590) sit on the same flow but are not expected to change.
- **Related known decisions**:
  - PR #36590 split authentication (401, start login) from authorization (403, never re-authenticate) and added the SAML loop guard. This fix must not reopen the loop.
  - OAuth already has a same-origin redirect sanitizer, `OAuthWebInterceptor.sanitizeRedirect`. It accepts only paths starting with a single `/` and rejects `//`, backslashes, schemes/authorities, and control characters. That is the existing precedent for "relative, same-host only".
  - The plan formally consults `dotCMS/platform-adrs`.

## Root-Cause Hypothesis

The requested page only survives the IdP round-trip in the HTTP session. That storage is unreliable across a cross-site POST. The one channel the SAML protocol guarantees to echo back, RelayState, is only populated when an admin configures a template. When no template is configured, `SamlWebUtils.evalRelayState` returns an empty value, so the IdP returns nothing and `processLogin` has nothing to work with.

There is also a latent security gap: `processLogin` passes whatever RelayState it receives directly to `RedirectUtil.sendRedirectHTML`, with no host check and no HTML escaping. Today that value comes only from admin configuration or an IdP-initiated login. Once dotCMS starts filling RelayState from the visitor's own request URL, the value can be steered by anyone who can get a victim to open a crafted link. The validation must therefore be in place before the default is turned on.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- **FR-001: Default RelayState.** When a login is started by `SamlWebInterceptor` and the site has **no** `auth.relaystate` template **and no** custom `RelayStateStrategy` registered, dotCMS sends the user's requested destination as the RelayState. The destination includes the path and query string. It is sanitized as described in FR-003 before it is sent; if sanitizing rejects it, no default RelayState is sent (today's behavior).
  The requested destination is the same value `doAuthentication` already stores as `REDIRECT_AFTER_LOGIN`: the visitor's `referrer` request parameter when present, otherwise the intercepted URI plus query string. Because `referrer` is visitor-supplied, the FR-003 validation applies before it is sent (resolved: Q1).
- **FR-002: Configured behavior wins.** An `auth.relaystate` template or a registered `RelayStateStrategy` keeps producing exactly the RelayState it produces today. The default is never applied on top of it.
- **FR-003: Same-host redirect validation in `processLogin`.** Before redirecting, the RelayState received on the callback must be validated, whether it came from the new default, a template, a custom strategy, or an IdP-initiated login. Only these values are accepted:
  - a relative path that starts with a single `/`, or
  - an absolute `http(s)` URL whose host equals the host of the callback request.

  Everything else is rejected: protocol-relative (`//evil.com`), other hosts, non-http(s) schemes (`javascript:`, `data:`), backslash tricks (`/\evil.com`), control or whitespace characters (`/\t//evil.com`), encoded variants that decode to any of these, and values that are not URLs at all (opaque tokens such as `companyCode=abc`). A rejected value is treated as if no RelayState was sent: the existing fallback chain runs (session `REDIRECT_AFTER_LOGIN` → `redirect.after.login` → `/dotAdmin/` with the "intent unknown" reroute), and the rejection is written to the security log without echoing the full value.

  **Normalization comes first.** The destination is percent-decoded once and its dot segments (`.` / `..`) are normalized before any of the checks above run. A value is rejected if it fails to normalize, or if its `..` segments would climb above the root (for example `/page1/../../other-path`). Browsers would quietly clamp such a path to `/`, but a real destination never looks like that, so it is treated as tampering. The normalized value is what gets validated, what the back-end role gate (`isBackEndLogin`) checks, and what the user is redirected to, so the path dotCMS checks is the path the user lands on. This matters because the gate uses `startsWith` checks on an unnormalized path: without normalization, `/members/../dotAdmin/` would slip past it, and `/dotAdmin/../members/report` would be wrongly denied.
- **FR-004: Same validation on the session fallback.** The session `REDIRECT_AFTER_LOGIN` value can come from the visitor-supplied `referrer` parameter in `doAuthentication`. It goes through the same validation in `processLogin`. Admin-configured `redirect.after.login` is trusted configuration and is left as-is.
- **FR-005: Safe HTML redirect.** The destination written into the `RedirectUtil.sendRedirectHTML` meta-refresh page must not be able to break out of its attribute. Either validation rejects quote and angle-bracket characters, or the value is HTML-attribute-encoded when it is written. The plan picks one.
- **FR-006: Existing guards keep working.** The back-end role gate (`isBackEndLogin` + `AuthAccessDeniedUtil`), the loop guard, and the 401/403 split from PR #36590 behave exactly as today for the redirect target they are given.
- **FR-007: Size limit.** The default RelayState is sent in full even when it is longer than the 80-byte limit in the SAML bindings spec, because Entra ID and Okta accept longer values. When it is over 80 bytes, a debug-level log line records the length, not the value. No truncation is applied and no new config key is added (resolved: Q2).

**Explicitly out of scope / non-goals**:

- Logout redirect fall-through (issue item 2), already fixed in PR #36590.
- `DotSamlResource.doLogin` (`GET /api/v1/dotsaml/login/{idpConfigId}`). Its request URI is the API endpoint itself, not a user destination, so no default RelayState is derived there. Template and strategy behavior there is unchanged.
- Making the HTTP session survive the cross-site IdP POST (SameSite cookie changes).
- Changing what `redirect.after.login`, `auth.relaystate`, or `RelayStateStrategy` mean, or adding new SAML app config keys.
- Cross-host redirects, including to another site on the same dotCMS instance. The user explicitly scoped this to the same host.
- OAuth/OIDC flows. `OAuthWebInterceptor.sanitizeRedirect` may be reused or moved to a shared place, but OAuth behavior must not change.
- The `/dotAdmin/` fragment-based routes (`#/c/...`). Fragments never reach the server, so back-end deep links are not improved by this fix.
- Rewriting the legacy RelayState or login-path code beyond what FR-001 to FR-007 need.

## Regression Risk *(mandatory)*

- **Blast radius**:
  - **Back-end SAML logins.** A signed-out user opening `/dotAdmin/...` will now carry `/dotAdmin/...` as the RelayState. Admins still land in `/dotAdmin/` as today. A front-end-only user who opens a back-end URL used to be rerouted to `/` through the "intent unknown" path; they will now get the 403 no-access page, because their intent (`/dotAdmin`) is now known. This is arguably correct, but it is a visible change and needs an explicit acceptance criterion (AC-005).
  - **Loop guard.** After the callback, the user is redirected to the front-end page and goes back through `SamlWebInterceptor.intercept`. A front-end page the user has no READ on must still end in a single 403, never another IdP round-trip.
  - **Query-string duplication.** `processLogin` appends the session's `FORWARD_QUERY_STRING` to any non-default destination. Once the RelayState already carries the query string, a surviving session would append it twice (`?id=42?id=42` or `?id=42&id=42`). The plan must make sure the query string is added only once.
  - **Plugins and custom setups.** Sites with a registered `RelayStateStrategy` (OSGi plugins) or an `auth.relaystate` template keep their own value (FR-002). But FR-003 now validates that value on the callback. Any template or strategy that produces an absolute URL to **another host**, or an opaque non-URL token that the post-login redirect used to follow, will no longer be redirected to. The flow falls back instead. This is the main back-compat risk and must be called out in release notes.
  - **IdP-initiated logins.** These carry whatever RelayState the IdP was configured with. A cross-host value there now falls back as well.
  - **OAuth.** OAuth is only affected if `sanitizeRedirect` is extracted and shared. Its existing tests must keep passing unchanged.
- **Backward compatibility**: There are no DB, ES-mapping, content, or REST contract changes, and no new config keys. The change is rollback-safe: rolling back restores today's empty RelayState and unvalidated redirect. The visible differences are the ones listed above: front-end users return to their page, cross-host or non-URL RelayState values are no longer followed, and front-end-only users who open back-end URLs get a 403.
- **Data considerations**: None. There is no stored data to migrate or repair.

## Acceptance & Verification *(mandatory)*

- **AC-001 (the fix).** Repeat the reproduction: signed out, default config, open `/members/report?id=42`. The outgoing AuthnRequest carries a RelayState equal to that destination. After the IdP callback, the user is signed in and redirected to `/members/report?id=42` on the same host, even though the callback has a fresh session.
- **AC-002 (configured wins).** With an `auth.relaystate` template, or a `RelayStateStrategy` registered for the site, the sent RelayState is exactly what it is today, byte for byte.
- **AC-003 (open-redirect rejection).** For each of the following RelayState values on the callback, dotCMS does **not** redirect to it. It uses the existing fallback chain and writes a security-log entry:
  - `https://evil.com/x`
  - `//evil.com`
  - `/\evil.com`
  - `/%5Cevil.com`
  - `/\t//evil.com`
  - `javascript:alert(1)`
  - `http://evil.com@site.example.com/` (userinfo trick, accepted only if the actual host matches)
  - `https://site.example.com.evil.com/`
  - `companyCode=abc`
  - a value containing `'` or `"><script>`
  - `/page1/../../other-path` (dot segments climb above the root)
  - `/%2e%2e/%2e%2e/other-path` (the same, percent-encoded)
- **AC-004 (same-host accepted).** `/members/report?id=42` and `https://<callback host>/members/report?id=42` are both followed.
- **AC-005 (back-end unchanged for admins, explicit for front-end-only users).**
  - An admin opening `/dotAdmin/` signed out lands in `/dotAdmin/`.
  - A front-end-only user opening `/dotAdmin/` gets the 403 no-access page, not a loop.
  - `/members/../dotAdmin/` is treated as a back-end destination: a front-end-only user gets the 403 no-access page, and an admin lands in `/dotAdmin/`.
  - `/dotAdmin/../members/report` is treated as a front-end destination: a front-end-only user is redirected to `/members/report`, not denied.
  - An IdP-initiated login with no RelayState keeps today's behavior: `/` for front-end-only users when front-end SSO is enabled, otherwise `/dotAdmin/`.
- **AC-006 (no loop).** A front-end-only user returned to a front-end page they lack READ on receives a single 403 (PR #36590 loop guard). No second AuthnRequest is sent.
- **AC-007 (no duplicate query).** When the session does survive the round-trip, the final URL carries the original query string exactly once.
- **AC-008 (no XSS).** The HTML redirect page never renders the destination in a way that closes the `content`/`href` attribute.
- **AC-009 (referrer honored).** Starting from `/dotCMS/login?referrer=/members/report`, the user lands on `/members/report` after the callback. With `referrer=https://evil.com`, no default RelayState is sent, and the session value is not followed either.
- **Verification method**:
  - **Unit (`:dotcms-core`, Surefire).** New tests for the same-host validator (the AC-003 and AC-004 matrix, including the dot-segment and percent-encoded cases). Tests for the default RelayState derivation in `SamlWebUtils` / `SamlWebInterceptor`: no template → destination; template or strategy present → unchanged; rejected destination → no default. `processLogin` destination-resolution tests covering AC-003, AC-005, and AC-007, written in the mocked style of `SamlWebInterceptorLoopGuardTest`. Re-run `SamlWebInterceptorLoopGuardTest`, `SAMLHelperEnableBackendDefaultTest`, `SecurityUtilsTest`, and the OAuth `sanitizeRedirect` tests.
  - **Integration.** Any new integration test class must be registered in a `MainSuite*` / `Junit5Suite*`, or CI silently never runs it. Run with `-Dmaven.build.cache.enabled=false` and confirm `Tests run: N` in `target/failsafe-reports`.
  - **Manual, against real IdPs (Entra ID and Okta),** as the issue asks:
    - AC-001 with a deep link and query string.
    - AC-005 as both an admin and a front-end-only user, including the `/members/../dotAdmin/` and `/dotAdmin/../members/report` dot-segment cases.
    - An IdP-initiated login.
    - Confirm each IdP echoes the RelayState unchanged, including a destination longer than 80 bytes (FR-007).

## Assumptions

- "Same host" means the host of the callback request (`processLogin`'s `request.getServerName()`), which is the site the user is signing in to. Scheme and port differences on that host (for example http→https behind a proxy) are accepted.
- The SAML library (`SamlAuthenticationService.authentication`) passes a non-empty RelayState to the IdP unchanged. This is already true for template-produced values.
- The IdPs in scope (Entra ID, Okta) echo RelayState back on SP-initiated logins. The issue states this, and manual testing re-confirms it.
- Front-end SSO being enabled (`SAMLHelper.isFrontEndEnabled`) is a precondition for front-end pages to trigger SAML at all. This fix does not change when SAML is triggered.

## Clarifications

### Session 2026-10-02

- Q: Which value should the default RelayState carry? → A: The `referrer` parameter if present, otherwise the original request URI and query string. This is the same value as `REDIRECT_AFTER_LOGIN` and is validated by FR-003.
- Q: What should happen when the destination is longer than the 80-byte SAML RelayState limit? → A: Send the full value. Entra ID and Okta accept it. Log the length at debug level and verify manually with long URLs on both IdPs.
