# Issue Resolution Specification: Disable Basic authentication by default

**Feature Branch**: `issue-5818-basic-auth-spec` (proposed publication branch; issue directory `5818-disable-basic-auth`)

**Created**: 2026-10-19

**Status**: Design reference — implementation and tests continue together in PR #38090

**Type**: Issue / Bug Resolution — security hardening (Tier 2)

**Related GitHub Issue**: [dotCMS/core#5818 — allow to disable basic auth on WebResource](https://github.com/dotCMS/core/issues/5818)

**Input**: Introduce `ALLOW_BASIC_AUTH={NONE,JWT_TOKEN,ALL}`, default `NONE`; verify APIs, WebDAV, Ajax actions and DWR. Preserve designated password login, session and Bearer access; migrate normal Postman tests without globally enabling `ALL`.

## Problem Statement *(mandatory)*

Programmatic username/password authentication exposes backend password validation across multiple API surfaces rather than the designated login pathway. Operators need a consistent way to disable these password-guessing paths while retaining session/token access and an explicit legacy compatibility option.

**Severity / Impact**: Security hardening affecting backend accounts and password-Basic integrations by reducing exposed programmatic password-validation paths. Restricting Basic does not prevent brute force against retained login endpoints.

## Reproduction *(mandatory)*

**Environment**: Audited current source, standard configuration without `ALLOW_BASIC_AUTH`, disposable local fixture account with permission to read a protected resource, and a client without session cookies or Bearer credentials. Runtime reproduction has not been performed for this spec.

**Steps to Reproduce**:

1. Request a fixture's protected REST resource anonymously; confirm access is denied.
2. Repeat with `Authorization: Basic <base64(username:password)>`, using the fixture account's correct password, then an incorrect password. Do not reuse cookies.
3. Repeat against a protected WebDAV read, such as `PROPFIND /webdav/working/`, using an authorized fixture account.
4. Check representative `/DotAjaxDirector/` actions through their actual filters/servlet and inspect action-specific credential fallbacks. Check DWR separately rather than assuming it accepts Basic.

**Expected Behavior**: At absent/default `NONE`, Basic credentials cannot authenticate or invoke account-password verification, even with the correct password or a valid API JWT in the secret slot. Independently supported session/Bearer access and designated password login still work.

**Actual Behavior**: Source inspection finds REST password-Basic validation, separate WebDAV password/JWT validation, and additional Ajax credential fallbacks behind upstream authentication. No common mode setting was found. DWR has no independent Basic parser identified. These observations do not claim every route was successfully exploited over HTTP.

**Reproducibility**: Expected to be deterministic for reachable credential paths with valid fixture permissions; full-chain Ajax reachability remains to be verified.

## Scope of Investigation *(mandatory)*

- **Affected area**: Modern/legacy APIs and shared HTTP GraphQL, WebDAV's `autopub`, `nonpub`, `live`, `working` trees, Ajax actions including the issue's `*AjaxActions.java` surface, DWR, shared asset authentication, token issuance and dependent clients/tests, including the CLI's deprecated token-login dependency.
- **Suspected surface**: Both modern `com.dotcms.*` and legacy `com.dotmarketing.*`. A REST-only restriction leaves independent WebDAV validation; mocked authenticated users do not prove HTTP enforcement.
- **Related known decisions**: Security-by-default, legacy-aware scope, permission preservation and developer-approved failing tests are binding ([constitution](../../.specify/memory/constitution.md)). Planning must consult relevant ADRs and assess Legacy Impact after spec approval; no ADR alignment is claimed here.
- **Evidence**: Completed read-only API/JWT, WebDAV/Ajax/DWR and Postman audits informed this draft. No builds or runtime authentication probes were executed.

## Root-Cause Hypothesis

Multiple programmatic credential paths lack a common restrictive policy. A Basic-header-only restriction leaves equivalent password transports; globally disabling password verification breaks legitimate login. Existing JWT recognition can accept browser/login tokens as well as API tokens, so JWT-shaped input alone cannot establish token-only authentication or username ownership. Planning will confirm this hypothesis, not assume a particular implementation.

## Fix Scope & Non-Goals *(mandatory)*

**In scope — proposed policy for review**:

- `NONE` is the documented default: Basic passwords and Basic JWTs cannot authenticate. Normalize surrounding whitespace and case for recognized mode names; `FALSE` is a disabling alias for `NONE`. Missing, empty or unrecognized configuration fails closed with the same disabling effect. Supported spellings and configuration activation/restart requirements must be documented.
- `JWT_TOKEN` accepts only a username plus a valid same-owner API JWT. Reject browser/login/remember-me USER_TOKENs. `ALL` explicitly permits legacy password authentication and valid API-JWT authentication on Basic-capable surfaces; preserve existing legacy token compatibility where already supported, without adding Basic or USER_TOKEN support to unrelated endpoints.
- Equivalent programmatic password transports follow the restriction: `DOTAUTH`, currently accepted legacy URI/map/request credentials and Ajax `u/p` or `user/passwd` fallbacks. Do not broaden previously unsupported aliases. Password validation remains available through designated login, including `/api/v1/authentication` and normal form login and the existing deprecated JSON token endpoint used by the CLI. Retaining that endpoint does not permit Basic authentication on it.
- Restrictive modes must not invoke password verification, password-based pre/external authentication, password-login side effects or password-failure counters through these paths. Token-only access must not create a reusable session that outlives or bypasses token revocation, expiry or IP constraints.
- API JWT validation requires trusted signature/allowed algorithm, expiry and configured issuer rules, persisted token existence, nonrevocation, validity dates, active existing owner and applicable request-IP/network restrictions. Bind the submitted username to that owner's canonical account under configured email/user-ID login conventions. Full API-JWT validity and owner binding apply in both `JWT_TOKEN` and `ALL`; only already-supported non-API legacy token compatibility is grandfathered in `ALL`. JWT_TOKEN failures never fall back to password verification. ALL retains its explicitly enabled password fallback when API-token authentication fails.
- Preserve independently valid session precedence, supported Bearer types, frontend/backend sessions, remember-me and existing authorization/SSL requirements. Preserve asset-only anonymous fallback for foreign/replayed Basic; never use it to grant private access or weaken protected APIs. Stronger Bearer-only endpoints remain Bearer-only even in `ALL`.
- Disabled WebDAV credentials cannot read or mutate protected resources in any method/tree. Harmless discovery such as `OPTIONS` may retain existing behavior only if it authenticates nobody, validates no password and exposes no protected data. Check empty/named-property `PROPPATCH` and library shortcuts. Preserve DWR's public engine/util resources and supported authenticated access without adding Basic support. Protected REST requests without independently valid credentials retain HTTP 401 authentication denial; malformed credentials retain existing HTTP 400 behavior and permission failures remain unchanged. Preserve existing Ajax/DWR denial behavior and other status/challenge contracts; general status cleanup is out of scope.
- Preserve the remote-token workflow's existing user-facing success/error contract under secure defaults. Remote issuance must use supported designated login/token issuance rather than a password-Basic exemption; do not drop the workflow, require receiver `ALL`, or merely change its outer test header. This is a bounded compatibility migration, not a remote-auth redesign.

**Smallest complete test migration**:

- Normal Postman bootstrap obtains a real API JWT through supported JSON session login then API-token issuance, or uses an explicitly supplied valid API JWT. Keep any bootstrap cookie isolated from ordinary collection requests; do not replace API JWTs with login JWTs or log/hardcode secrets.
- Convert ordinary collection/folder defaults, request overrides, manual headers, pre-request scripts and nested scripted calls to Bearer. Honor supplied tokens consistently; standalone imports must bootstrap safely or clearly require a pre-issued token, never silently fall back to Basic. Known traps include Nav manual headers, Tags nested calls, Template language lookup and JobQueue/BulkUpload/BulkRefresh token-variable guards.
- Preserve endpoints/assertions and original actor permissions: workflow starter users, scripting and limited users need appropriate tokens, not a universal administrator token. Classify unset auth; preserve public/noauth, denied-permission, invalid/expired token, login/logout/session and token-lifecycle cases. Preserve browser-user-token versus API-token revocation semantics.
- Ordinary local/CI groups remain at absent/default `NONE`, without global `ALL`, shared admin cookies or mass skips. Migrate affected integration/Karate fixtures and importable examples at the same boundary. Retain dedicated password/mode-specific tests rather than converting away their purpose.
- Keep WebDAV REST setup on Bearer, default protected-operation denial at `NONE`, and successful WebDAV CRUD/permissions in isolated `JWT_TOKEN` coverage with username/API-JWT credentials. Resolve conflicting explicit Bearer/inherited Basic headers and verify effective wire authentication. Retain remote issuance success/error coverage at secure defaults. Use a small separate three-mode matrix, not three copies of all functional tests.

**Explicitly out of scope / non-goals**:

- Rewriting authentication/legacy subsystems, changing password storage, rate limiting, MFA, SSO or unrelated token-revocation semantics.
- Adding direct WebDAV Bearer or DWR Basic support; restricting existing Bearer token types or unrelated outbound Basic to other products/services.
- Content/schema/index migrations, new token formats/auth frameworks, ADR creation, implementation/tests or plan/task artifacts in this spec-only change.

## Regression Risk *(mandatory)*

- **Blast radius**: Shared REST/GraphQL, Ajax, assets, browser workflows, separate WebDAV and token issuance; HTTP filters and independent fallbacks need verification.
- **Backward compatibility**: Password-Basic clients intentionally stop working by default; token Basic also stops at `NONE`. Existing scripts/startup clients must migrate to supported login/Bearer/session paths or explicitly opt into compatibility mode. Basic-only WebDAV protected operations require explicit `JWT_TOKEN` configuration and token credentials; direct Bearer support is not promised. `ALL` restores password exposure and is not the CI default or recommended migration. Remote-token workflow compatibility must be preserved without reopening Basic. Older senders still emit password Basic: upgrade senders first or use pre-issued tokens/a supported receiver-compatible flow; a new receiver does not automatically repair an old sender.
- **Test migration**: Static audit found 99 runner-discovered collections, 2,915 request definitions, 1,290 effectively Basic-authenticated requests, 1,261 request-level Basic overrides and 51 Basic-producing script blocks across eleven ordinary CI shards. These are inventory counts, not executed tests; changing roots alone is insufficient.
- **Data considerations**: No persistent content/account conversion is expected. Preserve passwords/tokens without disclosure; isolate and clean up test credentials. Rollback or `ALL` can restore password-validation exposure even without data migration.

## Acceptance & Verification *(mandatory)*

- **AC-001 — Modes and password isolation**: With fresh cookie-free clients, absent/`NONE`/`FALSE` and invalid/empty configuration reject correct/incorrect passwords and valid API JWTs in Basic. Recognized modes behave identically across case/whitespace variants. `JWT_TOKEN` accepts a same-owner API JWT but not a real password; `ALL` retains valid legacy credentials and permissions. Assert zero password-verifier/pre-authenticator/password-login calls and side effects in restrictive modes, including equivalent legacy transports, not merely non-success HTTP status.
- **AC-002 — Strict tokens**: JWT_TOKEN rejects malformed/tampered/unsigned/disallowed-algorithm, expired, revoked/deleted, not-yet-valid, wrong-owner, inactive/deleted-user, wrong-network and USER_TOKEN credentials. Apply full API-JWT validity and owner binding when authenticating a token in `ALL`; that mode may still accept the submitted secret as a legacy account password. USER_TOKEN exclusion is specific to `JWT_TOKEN`. Exercise configured issuer rules, email and ID login conventions, and revocation after a successful request. Subsequent access cannot survive invalidation via an accidentally minted session.
- **AC-003 — Surfaces and malformed input**: Verify modern/legacy APIs, GraphQL, all WebDAV trees/methods and shortcuts, representative Ajax fallbacks through full HTTP chains, and DWR protected/public paths. Invalid Base64, missing colon and empty parts fail safely without authentication, secret disclosure or uncontrolled exceptions. Scheme-case variations cannot bypass policy; colon-containing secrets must not be truncated into different valid credentials. Discovery never validates disabled passwords or leaks protected data; assert documented denial/status/challenge behavior.
- **AC-004 — Unaffected access**: In every mode, designated JSON/form login, supported frontend/backend session/remember-me/Bearer access and permissions retain their behavior, including sessions with replayed Basic. Bearer-only endpoints remain stricter; public asset fallback stays anonymous and private assets remain protected. Migrate protected asset password-Basic positives to Bearer while retaining intentional foreign-Basic public-fallback tests. Assert actual identity/permissions, not merely servlet forwarding or HTTP success. Remote-token issuance retains its success/error and permission coverage at `NONE` without password Basic on the receiving API.
- **AC-005 — Honest migration**: Bootstrap and ordinary Postman groups execute successfully at default `NONE` using appropriate actor API JWTs, without leaked bootstrap sessions. Inventory every retained Basic use as dedicated compatibility/negative coverage. Preserve anonymous/auth-negative/session/token-lifecycle assertions and standalone use. Exercise default WebDAV denial plus isolated JWT_TOKEN CRUD and a small separate mode matrix, without global `ALL`, blanket 401 substitutions or skipped functional assertions.
- **AC-006 — Evidence and guidance**: Reports identify executed classes/collections, nonzero tests/assertions and bootstrap errors/skips/failures; process exit or cached success alone is insufficient. Operator guidance states supported configuration, fail-closed behavior, activation semantics, intentional breaks, compatibility risks and migration paths without secrets.

**Verification method**: After approval, write developer-approved regressions and demonstrate meaningful Red before implementation. Reuse/extend `WebResourceIntegrationTest`, `ApiTokenAPITest`, `JsonWebTokenServiceIntegrationTest`, `WebDavAuthenticationFilterTest`, WebDAV authorization and `AjaxDirectorServletIntegrationTest`; add actual DWR/full-chain coverage where absent. Register new integration classes in a CI suite. Use targeted class runs with skip flags disabled/build-cache bypass and inspect test reports; no full Java integration suite is required. Verify representative mixed-auth/script/actor Postman collections first, then all ordinary groups at `NONE`, with dedicated isolated mode runs. Reconcile actual selection against the collection inventory.

**Delivery workflow**: The developer suspended the separate specification workflow for this security issue and requested implementation in the same PR. This file records the design context, not a separate approval gate. Regression tests were written and reviewed before implementation; meaningful failing tests and focused passing results are recorded in the PR. Live integration/Newman verification must not be inferred from unit or compilation checks.

## Assumptions

- `ALL` preserves existing legacy compatibility; `JWT_TOKEN` is strictly API-token-only. Session precedence and limited public fallback remain unchanged.
- Retain deprecated JSON `POST /api/v1/authentication/api-token` for existing CLI clients. Retirement and deployed-client migration are outside this Basic-authentication change; the endpoint receives no Basic exemption.
