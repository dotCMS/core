# Login Lockout

Repeated failed logins lock the failing source out of the account for a fixed time. The code is
`com.dotcms.auth.LoginLockout`; state lives in the `login_lockout` table, so every node of a cluster sees
the same counts and locks. Times are taken from the database clock, so nodes whose own clocks differ still
agree on when a lock ends.

## How it works

- **Per account and client address.** Each address gets `auth.max.failures.limit` password attempts
  (default 5) within the counting window. When they are used up without a success, that address is locked
  out of that account. Other addresses, including the account owner's, are not affected.
  - IPv6 addresses are grouped by their /64 network, since one host can usually use any address in it.
  - WebDAV is counted on its own (`webdav:<address>`), so a mounted drive retrying a stale password locks
    only WebDAV from that machine, not backend logins.
- **Per account, all addresses together (opt-in, off by default).** When `AUTH_LOCKOUT_ACCOUNT_MAX_FAILURES`
  is set above 0 and failures from every address add up to it within the window, the account is locked for
  every address. It caps guessing spread over many addresses, but anyone who knows a login name can trigger
  it and keep that account, administrators included, locked for everyone. Enable it only if that trade-off
  suits the site.
- **Fixed length.** Locks last `AUTH_LOCKOUT_DURATION_SECONDS` and do not grow. A count that ages out of
  the window, or whose lock has run out, starts over.
- **Counted before the check.** Each attempt is counted before its password is checked, so parallel
  requests get at most the limit between them. A client that sends more than that at once from one address
  (a WebDAV mount opening several connections, a script using REST basic auth) gets a refusal for the extra
  requests and can retry. A refused request whose password was right hands its count back, so only wrong
  passwords ever lead to a lock.
- **While locked,** the password is still checked (same timing) and the login fails exactly like a wrong
  password. Nothing is counted.
- **Clearing.** A successful login clears the count for its own address. A password reset, or a new
  password set by an administrator, clears everything for the account. Deleting a user deletes their rows,
  and rows that no longer affect any login are purged periodically.

Every password check goes through it: the backend and front-end login, REST basic auth (and so the
authentication endpoints and the CLI), WebDAV, the legacy `LoginFactory.doLogin` endpoints, Login As, and
the current-password check when users change their own password. Failures rejected by LDAP or a WebDAV
pre-authenticator are counted too. API tokens, SAML, OAuth and remember-me cookies check no password, so
they are neither counted nor blocked.

## Configuration

| Key | Default | Meaning |
|---|---|---|
| `FEATURE_FLAG_AUTH_LOGIN_LOCKOUT` | `true` | Turns lockout on or off. Env var: `DOT_FEATURE_FLAG_AUTH_LOGIN_LOCKOUT`. |
| `auth.max.failures.limit` | `5` | Attempts per address and account in a window (portal property). `0` also turns lockout off. |
| `AUTH_LOCKOUT_ACCOUNT_MAX_FAILURES` | `0` (off) | Failures per account from all addresses in a window before the account is locked for everyone. Set above 0 to enable. |
| `AUTH_LOCKOUT_WINDOW_SECONDS` | `3600` | How long attempts are counted together. |
| `AUTH_LOCKOUT_DURATION_SECONDS` | `900` | Lock length. |

## Behind a proxy or load balancer

The client address is `HttpServletRequest.getRemoteAddr()`. Tomcat's `RemoteIpValve` replaces it with the
`X-Forwarded-For` client only when the request comes from a trusted proxy. The bundled `server.xml` trusts
private address ranges by default; set `CMS_REMOTEIP_INTERNALPROXIES` for anything else.

If the proxy isn't trusted, every user behind it shares one address. Five wrong passwords from anyone then
lock everyone behind that proxy out of the account. When one address is locked out of five or more
accounts at once, dotCMS logs a warning that names it. That warning means either one client is trying many
accounts, or the address is an untrusted proxy.

## Support runbook: a user is locked out

1. **Try another network, or wait.** Locks are per address and last 15 minutes by default, so the user can
   log in from another network or wait. Only the account-wide limit, if a site has enabled it, blocks every
   address.
2. **Unlock now:** reset the password through the "Forgot password" email, or have an administrator set a
   new password in Users. Either clears all lockout state for the account.
3. **Unlock without changing the password:** with database access, clear the state directly.
   - Everything for the user: `DELETE FROM login_lockout WHERE user_id = '<user id>';`
   - One address only: `DELETE FROM login_lockout WHERE user_id = '<user id>' AND source = '<address>';`
4. **Diagnose:** `SELECT source, failures, to_timestamp(locked_until / 1000) FROM login_lockout WHERE
   user_id = '<user id>';` shows each source's count and lock. The security log records every lock with
   its source.
5. **Every lock shows the same proxy or load-balancer address:** Tomcat isn't seeing real client
   addresses. Set `CMS_REMOTEIP_INTERNALPROXIES` (see above).
6. **Turn lockout off:** set `DOT_FEATURE_FLAG_AUTH_LOGIN_LOCKOUT=false`.
