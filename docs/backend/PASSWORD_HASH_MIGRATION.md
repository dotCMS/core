# Password hash migration: reader-capability release

This is the first of two releases. It verifies both 192-bit and 256-bit PBKDF2 records, but the shared password-generation default **continues writing 192-bit profile `1`**. Successful login does not upgrade a password merely because it uses profile `1`. Activating profile-`2` writes and profile-driven rehashing belongs in a later release.

## Profiles and behavior

| Record | Serialized profile ID | Derived output | This reader-capable binary | Old-only binary |
|---|---|---|---|---|
| Legacy PBKDF2 | `1` | 24 bytes / 192 bits | Verifies; rehashes only for outdated cost | Verifies |
| Future PBKDF2 | `2` | 32 bytes / 256 bits | Verifies; rehashes only for outdated cost | Cannot verify normally; legacy fallback can accept the stored hash string as the password |

The record remains `version:profile-id:salt:configuration:derived-hash`. Profile IDs and iteration values retain their existing encoding. Each record keeps its own salt and iteration count; new generation uses a fresh cryptographically random salt.

The default remains **PBKDF2-HMAC-SHA256 with 600,000 iterations**. Existing `PBKDF2_ALGORITHM`, `PBKDF2_DEFAULT_ITERATIONS`, and `PBKDF2_MINIMUM_ITERATIONS` overrides retain their behavior. This release does not change iteration policy or establish FIPS certification or complete NIST compliance.

Account creation, password reset/change, system-generated passwords, and the existing cost-based login upgrade all continue using profile `1` through the shared default. Both email-address and user-ID login modes retain this behavior. Incorrect passwords do not trigger a rehash; persistence failures retain the existing login failure handling. No bulk rewrite or forced password reset is introduced.

The encrypted-user-ID helper accepts correctly verified passwords from either profile, including records with outdated costs. It only returns credentials and does not persist a rehash; that remains the normal password-login flow's responsibility.

## Phase 1: deploy compatible readers, retain legacy writes

1. Confirm this release's shared default generates profile `1`, not `2`. Check custom plugins and other password writers too: explicitly selecting profile `2` is not part of this rollout.
2. Deploy the reader-capable release to all authentication nodes, password-writing processes, and other consumers of the same password database. Rolling deployment alongside old-only binaries is compatible for this password-format change **while all stored records remain profile `1`**.
3. Verify an existing-password login under the configured login mode and confirm its current-cost stored hash remains unchanged. Exercise test-account creation or password reset/change and confirm the result is still profile `1`. Never log passwords or complete stored records.
4. Retain this reader-capable release as an available rollback target. Do not activate profile-`2` writers until every permitted rollback target recognizes both profiles.

**Rollback during phase 1:** This release introduces no profile-`2` records through the shared default, so that default does not move the password-format rollback boundary. Rollback to an old-only binary still requires confirming no profile-`2` records were introduced by custom writers, imports, or an earlier deployment. Other release compatibility constraints continue to apply.

## Phase 2: enable 256-bit writes in a later release

Only after the reader-capable release is the oldest version in the rollback window may a separately reviewed release switch the shared default to profile `2` and deprecate profile `1` for automatic login migration. That later release must retain both verifiers and cover new writes, successful legacy-login upgrades, and compatible-reader rollback.

Do not mix profile-`2` writers with old-only readers. After the first profile-`2` write, rollback must target a binary that understands **both** profiles.

### Security boundary for old-only binaries

The old-only binary's problem is not just lockout. Its `PasswordFactoryProxy.isUnsecurePasswordHash` catches the unknown-profile exception and classifies the record as a legacy password. `LoginFactory.passwordMatch` then permits a plaintext comparison against the stored record. Consequently, someone possessing the complete stored profile-`2` record can submit that record as the password on an old-only node. This is a security reason never to route profile-`2` accounts to old-only nodes or roll them back to such a binary.

This is pre-existing unknown-profile fallback behavior. Fail-closed hardening for unsupported structured records is separate work; registering profile `2` here prevents that particular fallback for profile-`2` records on this release, but does not harden other unknown IDs.

There is **no supported automatic downgrade**. PBKDF2 output is prefix-preserving with identical algorithm, salt, and iteration count, but this release implements no operator conversion procedure. Do not rely on manual truncation/relabeling, database restoration as a routine downgrade, or mass password resets.

## Validation

Focused real-cryptography and mocked-persistence checks:

```bash
./mvnw test -pl :dotcms-core --am \
  -Dtest=PasswordHashMigrationTest,LoginFactoryPasswordMigrationTest,UserManagerPasswordMigrationTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dmaven.build.cache.enabled=false
```

Confirm positive counts and zero failures in `dotCMS/target/surefire-reports/*.txt`. The checks cover independent records from both profiles, legacy-only default generation, old-only reader compatibility for generated records, no profile-driven login rewrite, and preserved cost upgrades in both login modes. They do not validate a live database, SSO configuration, or production deployment rehearsal.

Related: [password-storage security patterns](SECURITY_BACKEND.md), [staged migration specification](../../specs/20261028-154324-pbkdf2-256/spec.md), and [OWASP password-storage guidance](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html#pbkdf2).
