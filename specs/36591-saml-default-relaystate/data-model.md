# Data Model: SAML default RelayState + same-host redirect validation (#36591)

This fix adds no persisted data. There are no database tables, no ES/OpenSearch mappings, no content types and no new config keys. The "entities" below are the in-memory values that move through the login round-trip, plus the rules that govern them.

## Redirect candidate

A destination string proposed for the post-login redirect.

| Source | Where it is read | Trusted? | Validated by |
|---|---|---|---|
| Default RelayState: `referrer` parameter (plus the query string saved where the 401 was raised (`SecurityUtils.rememberLoginQueryString`) when the session's `REDIRECT_AFTER_LOGIN` matches it), else intercepted URI + query; none for login or logout pages | `SamlWebInterceptor.doAuthentication` | No: it comes from the visitor | `RedirectUtil.toSameHostRedirect`, before it is sent |
| RelayState echoed by the IdP | `DotSamlResource.processLogin`, `request.getParameter("RelayState")` | No: it can come from a template, a plugin strategy, IdP configuration, or the default | `toSameHostRedirect`, on the callback |
| Session `REDIRECT_AFTER_LOGIN` | `processLogin` | No: it can hold the visitor's `referrer` | `toSameHostRedirect`, on the callback |
| SAML app config `redirect.after.login` | `processLogin` | Yes: set by an admin | Not validated (out of scope in the spec, FR-004). The back-end check runs on its normalized path. |
| `/dotAdmin/` built-in default | `processLogin` | Yes: a constant | Not validated; marks the login intent as unknown |

## Safe redirect target (validator output)

The value `toSameHostRedirect(candidate, requestHost)` returns when it accepts a candidate. Full algorithm: research R2.

- **Shape**: relative for the site's own hosts. For a host explicitly listed in `auth.relaystate.allowed.hosts` (Phase 15), it's an absolute URL (`scheme://host[:port]` lower-cased, plus the normalized path), so the browser goes to that host. A single leading `/`, then the normalized path, then the optional `?query` and `#fragment` exactly as received.
- **Invariants**:
  - It starts with `/` and not with `//`.
  - The raw value contains no control or whitespace characters, backslash, `"`, `'`, `<`, `>` or backtick, and neither does the path after decoding it once, except that an encoded space (`%20`) is allowed (spec clarification of 2026-10-07).
  - The path contains no `.` or `..` segments, in plain or percent-encoded form.
  - Path segments other than dot segments keep their original encoding.
  - If the candidate was absolute, its host was one of the allowed hosts (case-insensitive): the request host, or, on the SAML paths, the hostname or an alias of the site being logged in to (addendum of 2026-10-09). It had no user-info, and its scheme was `http` or `https`.
- **Rejection**: the method returns empty, never throws, and never logs the candidate value. Callers decide what to log.

## Validation outcomes (examples; the full list is AC-003 / AC-004 in `spec.md`)

| Candidate (request host `site.example.com`) | Result |
|---|---|
| `/members/report?id=42` | `/members/report?id=42` |
| `https://site.example.com/members/report?id=42` | `/members/report?id=42` |
| `HTTPS://SITE.EXAMPLE.COM:8443/a` | `/a` |
| `/dotAdmin/../members/report` | `/members/report` |
| `/members/../dotAdmin/` | `/dotAdmin/` |
| `/a/./b/` | `/a/b/` |
| `/page1/../../other-path`, `/%2e%2e/%2e%2e/other-path` | rejected: climbs above the root |
| `//evil.com`, `/\evil.com`, `/%5Cevil.com`, `/%2F%2Fevil.com`, `/\t//evil.com` | rejected |
| `https://evil.com/x`, `https://site.example.com.evil.com/`, `http://evil.com@site.example.com/` | rejected: wrong host or has user-info |
| `javascript:alert(1)`, `companyCode=abc`, empty, `null` | rejected: not a single-slash path or an http(s) URL |
| `/x'`, `/x"><script>` | rejected: forbidden characters |
| `/bad%zz` | rejected: malformed percent-encoding |

## RelayState selection, sending side (`SamlWebUtils.getRelayState`)

```
registered RelayStateStrategy for site? ── yes ──▶ strategy value (unchanged from today)
        │ no
non-blank auth.relaystate template? ───── yes ──▶ evaluated template (unchanged from today)
        │ no
auth.relaystate.default=false? ────────── yes ──▶ null (Phase 12 off switch; pre-upgrade behavior)
        │ no
default (validated) present? ──────────── yes ──▶ default
        │ no
        ▼
      null  (today's behavior: no RelayState)
```

## Destination resolution, callback side (`DotSamlResource.resolveLoginPath`, which also makes the role decision)

```
RelayState param ──validate──▶ accepted? ── yes ──▶ destination
        │ rejected/absent (rejection → security log: reason + length)
session REDIRECT_AFTER_LOGIN ──validate──▶ accepted? ── yes ──▶ destination  (attribute always removed)
        │ rejected/absent
config redirect.after.login set? ── yes ──▶ destination (trusted, as is)
        │ no
        ▼
/dotAdmin/  + login intent unknown   → (existing) reroute to "/" for front-end-only users when front-end SSO is on
```

After this, the existing steps run unchanged:
1. `FORWARD_QUERY_STRING` is appended only when the destination has no query string yet.
2. The back-end role check runs on the destination's normalized path.
3. The HTML redirect page is written, with the URL attribute-encoded.

## Addendum (2026-10-09): default RelayState exclusions and site aliases

| Situation | Default RelayState sent |
|---|---|
| 401 route: `/dotCMS/login?referrer=/members/report`, session `REDIRECT_AFTER_LOGIN=/members/report`, saved query `id=42` (session keys `REDIRECT_AFTER_LOGIN_QUERY_STRING` + `REDIRECT_AFTER_LOGIN_QUERY_PATH`, written where the 401 is raised: `VelocityServlet` for pages, `SecurityUtils.sendPermissionDenied` for files; Phases 9–10) | `/members/report?id=42` |
| Same, but the session `REDIRECT_AFTER_LOGIN` is another path | `/members/report` (no stale query) |
| Request `/dotCMS/logout`, `/c/portal/logout`, `/dotAdmin/logout`, or a configured `logout.path.values` entry | none |
| Request `/dotCMS/login` (no referrer), `/html/portal/login`, `/c/public/login`, `/c/portal_public/login`, `/application/login/...`, `/dotsaml/login` | none |
| `referrer=/dotCMS/logout` | none |
| Request `/dotAdmin/` | `/dotAdmin/` (never excluded) |

| Absolute RelayState on the callback (site `customer.com` with aliases `www.customer.com`, `cms.customer.com`; callback on `cms.customer.com`) | Result |
|---|---|
| `https://www.customer.com/members/` | `/members/` |
| `https://other-site.com/x` (another site on the instance) | rejected, `OTHER_HOST` |
| `https://www.customer.com.evil.com/` | rejected, `OTHER_HOST` |
