# dotCMS SAML Plugin

The SAML 2.0 authentication OSGi bundle (`com.dotcms.samlbundle:com.dotcms.samlbundle`)
built from the source tree in this module. The bundle keeps its existing Maven
coordinates, OSGi symbolic name (`com.dotcms.samlbundle`), activator
(`com.dotcms.saml.osgi.Activator`), configuration contract and authentication code.

## Source provenance

Imported unchanged from the reviewed upstream repository
[`dotCMS/com.dotcms.dotsaml`](https://github.com/dotCMS/com.dotcms.dotsaml) at commit
`2006c0fa021f21dd78db9c9d467bac6cb43e7aff` (main, after upstream PR #25).

The initial import preserved all 89 files under `src/` (62 main Java, 20 test Java,
resources and incidental images) byte-for-byte from that commit. The sorted `path  sha256` manifest of the
imported tree hashes to
`c342abfa5c5aef8b16065943bc3fa08c397a55272a178a5d479f77a40cc7012f`.
All resources and original tests are unchanged. Two source files were adapted after
import to comply with core logging rules: the activator reports the actual OSGi version,
and legacy Base64 diagnostics use `Logger` without printing input bytes or filenames.
Two decoder compatibility tests cover valid round-trips and invalid-input rejection.

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

Runs the 92 imported regressions plus two decoder compatibility tests. The Maven build cache must stay disabled so a
cached module cannot report success without executing them.

Two active integration tests in `GenericBundleActivatorIntegrationTest` also exercise the
packaged bundle: startup/service registration at the Tika release version, and a signed
HTTP-POST response accepted once then rejected by the database-backed replay check.
They use local signed fixtures, not an external IdP, and run in `MainSuite2b`.

```bash
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false \
  -Dit.test=GenericBundleActivatorIntegrationTest -Dmaven.build.cache.enabled=false
```

## Authentication

This integration does not rewrite the reviewed upstream authentication code. It brings
that implementation's response validation, replay protection and signature checks into
the normal core release build, including fixes absent from older published bundles.
Existing supported configuration contracts are retained.
