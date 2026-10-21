# dotCMS SAML Plugin

The SAML 2.0 authentication OSGi bundle (`com.dotcms.samlbundle:com.dotcms.samlbundle`)
built from the source tree in this module. The bundle keeps its existing Maven
coordinates, OSGi symbolic name (`com.dotcms.samlbundle`), activator
(`com.dotcms.saml.osgi.Activator`) and configuration keys. Upgrades from older
standalone bundles may require the opt-ins and database permissions described below.

## Source provenance

Initially imported unchanged from the reviewed upstream repository
[`dotCMS/com.dotcms.dotsaml`](https://github.com/dotCMS/com.dotcms.dotsaml) at commit
`2006c0fa021f21dd78db9c9d467bac6cb43e7aff` (main, after upstream PR #25).

The initial import preserved all 89 files under `src/` (62 main Java, 20 test Java,
resources and incidental images) byte-for-byte from that commit. The sorted `path  sha256` manifest of the
imported tree hashes to
`c342abfa5c5aef8b16065943bc3fa08c397a55272a178a5d479f77a40cc7012f`.
All resources are unchanged. Two source files were adapted after import to comply
with core logging rules: the activator reports the actual OSGi version, and legacy
Base64 diagnostics use `Logger` without printing input bytes or filenames.
Two decoder compatibility tests cover valid round-trips and invalid-input rejection.
The authentication service and endpoint-host redirect also preserve the computed
RelayState across the alias-to-endpoint hop, covered by four additional login-start tests.

## Versioning

The module declares no version: it inherits `${revision}${sha1}${changelist}` from
`com.dotcms.core.plugins:dotcms-core-plugins-parent`, so every core release produces a
SAML bundle at exactly the core release version.

The `provided` dependency `com.dotcms:dotcms-core:26.03.27-01` is a **compile API
baseline only** for the legacy `com.dotmarketing` types the source imports. It is not
the plugin's release version and does not pin the produced artifact. It is deliberately
fixed rather than `${project.version}`: depending on the reactor core would create a
`core -> system-bundles -> SAML -> core` cycle. Because it is `provided`, core is never
embedded in the bundle.

## Build and publish

Make future SAML fixes in this module. The upstream repository records the import's
provenance; releases do not fetch mutable source from it.

Built by the normal core reactor like the Tika plugins. The existing generic publisher
(`.github/scripts/publish-to-s3`) uploads the matching JAR and POM under the existing
`com.dotcms.samlbundle:com.dotcms.samlbundle` coordinates with the existing Bunny
credentials; no SAML-specific secret or release workflow exists. `osgi-base/system-bundles`
references the same `${project.version}` and packages the identical JAR.

## Tests

```bash
./mvnw -ntp -pl :com.dotcms.samlbundle -am test \
  -Dmaven.build.cache.enabled=false -DskipTests=false
```

Runs the 92 imported regressions, two decoder compatibility tests and four alias-redirect regressions. The Maven build cache must stay disabled so a
cached module cannot report success without executing them.

Two active integration tests in `GenericBundleActivatorIntegrationTest` also exercise the
packaged bundle: startup/service registration at the Tika release version, and a signed
HTTP-POST response accepted once then rejected by the database-backed replay check.
They use local signed fixtures, not an external IdP, and run in `MainSuite2a`.

```bash
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false \
  -Dit.test=GenericBundleActivatorIntegrationTest -Dmaven.build.cache.enabled=false
```

## Authentication

This integration brings the reviewed upstream response validation, replay protection
and signature checks into the normal core release build, including fixes absent from
older published bundles. Login started on a site alias first moves to the Service
Provider Endpoint Hostname so its request-binding cookie is available on callback.
That hop preserves the computed RelayState; the callback still checks the destination
against the site's hosts and `auth.relaystate.allowed.hosts`.

### Upgrade checklist for older standalone bundles

Existing configuration keys and bundle identity are retained, **not the older, more
permissive authentication defaults**. Before upgrading, review each site's SAML app
optional properties and database permissions:

- **IdP-initiated login:** `allow.unsolicited.responses` defaults to `false`. If users
  start login from an IdP portal tile (a response without `InResponseTo`), explicitly
  set `allow.unsolicited.responses=true`. Other assertion checks still apply.
- **Legacy signatures:** `allow.sha1.signatures` defaults to `false`. Configure the IdP
  to use RSA or ECDSA with SHA-256/384/512 and SHA-256/384/512 digests. Only as a
  temporary compatibility measure, set `allow.sha1.signatures=true` for an IdP still
  using RSA-SHA1 or SHA-1 digests; acceptance is logged as a warning.
- **Replay persistence:** on first login the bundle attempts to create
  `dotsaml_assertion_replay` and its expiry index in the dotCMS database. Ensure the
  database user can create those objects and insert/delete replay records. Database
  failures reject login; replay protection is never skipped. Test with the actual
  production database role before rollout.
- **Signature verification cannot be disabled:** `verify.signature.credentials=false`
  and `verify.signature.profile=false` are ignored with a warning.

Smoke-test SP-initiated login (including site aliases) and, where enabled,
IdP-initiated login before deploying to customers.

### Troubleshooting rejected logins

| Log message contains | Action |
|---|---|
| `Unsolicited SAML Responses (IdP-initiated login) are not accepted` | Start login from dotCMS, or explicitly enable `allow.unsolicited.responses` for this IdP. |
| `signature algorithm ... is not allowed` / `digest algorithm ... is not allowed` | Switch the IdP to SHA-256 or stronger; use `allow.sha1.signatures=true` only as a temporary exception. |
| `does not match an authentication request started from this browser` | Restart login from dotCMS; check HTTPS, request cookies and `authn.request.max.age` (default 900 seconds). |
| `Could not check whether the SAML assertion has already been used` | Check database connectivity and permission to create the replay table/index and insert/delete records. |
| `has already been used` | Start a new login instead of reposting the same response. |

The [reviewed upstream README](https://github.com/dotCMS/com.dotcms.dotsaml/blob/2006c0fa021f21dd78db9c9d467bac6cb43e7aff/README.md#troubleshooting-rejected-saml-logins)
contains the full validation checklist and troubleshooting table.
