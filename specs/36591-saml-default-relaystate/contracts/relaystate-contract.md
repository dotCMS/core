# Contracts: SAML RelayState and the post-login redirect (#36591)

This fix changes no REST endpoint signature, no `@Schema`, and no `openapi.yaml` entry. It does change two things: what dotCMS puts on the SAML wire, and two public Java methods that plugins can call. Both are written down here so reviewers can check them.

## C1. SAML wire contract

### AuthnRequest (SP → IdP), started by `SamlWebInterceptor`

| Site configuration | `RelayState` sent: before | `RelayState` sent: after |
|---|---|---|
| `RelayStateStrategy` registered for the site | strategy value | strategy value (unchanged) |
| `auth.relaystate` template set | evaluated template | evaluated template (unchanged) |
| Neither, and the destination passes validation | none | the validated destination: relative path, query and fragment, e.g. `/members/report?id=42` |
| Neither, and the destination is rejected | none | none (unchanged) |

- **Destination** = the `referrer` request parameter when present, otherwise the intercepted request URI plus query string. On the usual front-end route (401 → `/dotCMS/login?referrer=<path>`), the query string saved where the 401 was raised (session key `REDIRECT_AFTER_LOGIN_QUERY_STRING`, written by `SecurityUtils.rememberLoginQueryString` from `VelocityServlet` for pages and from `SecurityUtils.sendPermissionDenied` for files; Phases 9–10) is reattached when the path saved next to it (`REDIRECT_AFTER_LOGIN_QUERY_PATH`, Phase 11; else the session's `REDIRECT_AFTER_LOGIN`) equals the referrer.
- **No default** when the destination is a logout path (the configured `logout.path.values`, e.g. `/dotCMS/logout`, `/c/portal/logout`, `/dotAdmin/logout`) or a login page (`/dotCMS/login`, `/application/login`, `/html/portal/login`, `/c/public/login`, `/c/portal_public/login`, `/dotsaml/login`). `/dotAdmin` is never excluded.
- **Length**: sent in full even when longer than 80 bytes. A debug log line records the length only.
- **Allowed external hosts (Phase 15):** a RelayState, saved session destination or default on a host listed in `auth.relaystate.allowed.hosts` is followed as an absolute URL. `redirect.after.login` is unaffected (trusted, unchecked).
- **Off switch (Phase 12):** with the SAML app property `auth.relaystate.default=false` (exactly `false`, case-insensitive), no default is sent, exactly as before the fix.
- **IdP-initiated sign-ins:** dotCMS sends nothing, so the IdP's own Relay State / Default RelayState is what arrives, subject to the callback's same-host check.
- **`GET /api/v1/dotsaml/login/{idpConfigId}`** (`DotSamlResource.doLogin`): unchanged. It never adds a default RelayState.

### Assertion callback (IdP → SP): `POST /api/v1/dotsaml/login/{idpConfigId}` (`DotSamlResource.processLogin`)

- **Request**: unchanged. It reads `SAMLResponse` and the optional `RelayState` form parameters.
- **Response**: unchanged in kind: the HTML meta-refresh redirect page (200, `text/html`), or the existing 403 no-access page. Two things differ:
  - *Which* destination is used. A `RelayState` or session value is followed only after it passes the same-host check, otherwise the fallback chain runs. Order: RelayState → session `REDIRECT_AFTER_LOGIN` → `redirect.after.login` → `/dotAdmin/`.
  - The redirect URL is written attribute-encoded into the HTML page.
- **Status codes**: unchanged.

## C2. Java API (public, callable from plugins)

### `com.dotcms.util.RedirectUtil`

```java
/** New. Returns the same-host, normalized, relative redirect target, or empty if the candidate is unsafe. Never throws. */
public static Optional<String> toSameHostRedirect(String candidate, String requestHost);

/** New. Why toSameHostRedirect rejects the candidate, or empty when it is accepted. Never throws. Lets callers log the reason without logging the value. */
public static Optional<RedirectUtil.RedirectRejection> rejectionReason(String candidate, String requestHost);

/** New (Phase 8). Same checks, but an absolute URL is accepted when its host is any of allowedHosts (case-insensitive).
 *  The SAML paths pass the request host plus the hostname and aliases of the site being logged in to. The (String, String)
 *  methods above delegate with a single host and keep their behavior. */
public static Optional<String> toSameHostRedirect(String candidate, Collection<String> allowedHosts);
public static Optional<RedirectUtil.RedirectRejection> rejectionReason(String candidate, Collection<String> allowedHosts);

/** New (Phase 15). Site hosts -> normalized relative path; explicitly allowed external hosts -> normalized absolute
 *  URL (scheme://host[:port]/path?query#fragment). Same rules otherwise. The overloads above pass no external hosts. */
public static Optional<String> toAllowedRedirect(String candidate, Collection<String> siteHosts, Collection<String> externalHosts);
public static Optional<RedirectUtil.RedirectRejection> rejectionReason(String candidate, Collection<String> siteHosts, Collection<String> externalHosts);

/** New. Why a destination was rejected. */
public enum RedirectRejection {
    BLANK,               // null, empty or only whitespace
    OTHER_HOST,          // absolute URL for another host, or with user-info
    NOT_A_PATH,          // neither a path starting with "/" nor an http(s) URL
    PROTOCOL_RELATIVE,   // starts, or would start after decoding or resolving dot segments, with "//"
    FORBIDDEN_CHARACTER, // control/whitespace char, backslash, quote, angle bracket or backtick (an encoded space is allowed), an encoded slash (%2F) in the path, or a double-encoded slash, backslash or dot (%252F, %255C, %252E) in the path
    MALFORMED_ENCODING,  // malformed percent-encoding or URL syntax
    ABOVE_ROOT           // ".." segments climb above the root
}

/** Existing signature, unchanged. The URL is now HTML-attribute-encoded before it is written into the page. */
public static void sendRedirectHTML(HttpServletResponse response, String redirectUrl);
```

### `com.dotcms.util.SecurityUtils` (Phases 9–11)

```java
/** New. Session key: query string of the protected page a signed-out visitor requested. */
public static final String REDIRECT_AFTER_LOGIN_QUERY_STRING = "REDIRECT_AFTER_LOGIN_QUERY_STRING";
/** New. Session key: that page's original path, saved at the same moment (persists with Redis sessions). */
public static final String REDIRECT_AFTER_LOGIN_QUERY_PATH = "REDIRECT_AFTER_LOGIN_QUERY_PATH";

/** New. Saves both keys from the request being denied (forward attributes, else the request's own URI and query),
 *  or removes them when there is no query string. Called where the 401 is raised. */
public static void rememberLoginQueryString(HttpServletRequest request);

/** Existing signature, unchanged status codes. Signed out: also calls rememberLoginQueryString.
 *  Signed in (403): also clears both keys. REDIRECT_AFTER_LOGIN handling is unchanged. */
public static void sendPermissionDenied(User user, String originalUri, HttpServletRequest request, HttpServletResponse response);
```

`com.dotcms.rendering.velocity.servlet.VelocityServlet` gets a package-private `handleSecurityException(User, HttpServletRequest, HttpServletResponse)` (not public API). It keeps the existing 401/403 answers for pages, and saves or clears the keys above.

### `com.dotcms.filters.interceptor.saml.SamlWebUtils`

```java
/** Existing signatures, unchanged in behavior (equivalent to passing a null default). */
public String getRelayState(HttpServletRequest, HttpServletResponse, IdentityProviderConfiguration, String siteIdentifier);
public String getRelayState(HttpServletRequest, HttpServletResponse, IdentityProviderConfiguration, String relayStateTemplate, String siteIdentifier);

/** New (Phase 12). SAML app property that turns the default RelayState off; off only when exactly "false". */
public static final String AUTH_RELAYSTATE_DEFAULT_KEY = "auth.relaystate.default";
/** New (Phase 12). False only when AUTH_RELAYSTATE_DEFAULT_KEY is set to "false" (case-insensitive, trimmed). */
public static boolean isDefaultRelayStateEnabled(IdentityProviderConfiguration identityProviderConfiguration);

/** New (Phase 15). SAML app property: extra hosts (exact, comma-separated) a post-login redirect may point to. */
public static final String AUTH_RELAYSTATE_ALLOWED_HOSTS_KEY = "auth.relaystate.allowed.hosts";
/** New (Phase 15). The configured hosts, trimmed; entries with *, /, :, @ or whitespace are ignored. */
public static List<String> configuredAllowedHosts(IdentityProviderConfiguration identityProviderConfiguration);

/** New. Order: registered strategy > non-blank template > defaultRelayState > null. */
public String getRelayStateWithDefault(HttpServletRequest, HttpServletResponse, IdentityProviderConfiguration,
                                       String siteIdentifier, String defaultRelayState);
```

- **Why a new name and not a `getRelayState` overload:** the existing 5-argument overload already takes `(…, String relayStateTemplate, String siteIdentifier)`. An overload taking `(…, String siteIdentifier, String defaultRelayState)` would have the same erased signature and wouldn't compile.
- **`RelayStateStrategy` / `addRelayStateStrategy`**: unchanged. A registered strategy still fully owns the RelayState the site sends. Its value **is** now validated on the callback (C1), so a strategy that produces a URL on another host, or a value that isn't a URL, no longer drives the post-login redirect.

## C3. Logging contract

| Event | Logger | Level | Content |
|---|---|---|---|
| Callback RelayState or session value rejected | `SecurityLogger` | info | Which source was rejected (`RelayState` or `session REDIRECT_AFTER_LOGIN`), the `RedirectRejection` name and the value's length, e.g. `… from RelayState (reason OTHER_HOST, length 28) …`. **Never the value itself.** |
| Default RelayState rejected, so none is sent | `Logger` | debug | The `RedirectRejection` name, e.g. `(reason ABOVE_ROOT)`. Never the value. |
| Default RelayState longer than 80 bytes | `Logger` | debug | The length only |
