# Password hash migration: 192 to 256 bits

New password records use a 256-bit PBKDF2 output. Existing 192-bit records remain verifiable and upgrade after a successful password login. Users keep their passwords; this is not a bulk rewrite or a forced password reset.

## Profiles and behavior

| Record | Serialized profile ID | Derived output | Current binary | Old-only binary |
|---|---|---|---|---|
| Legacy PBKDF2 | `1` | 24 bytes / 192 bits | Verifies; requests rehash | Verifies |
| Current PBKDF2 | `2` | 32 bytes / 256 bits | Verifies; requests rehash only if cost is outdated | Cannot verify |

The record remains `version:profile-id:salt:configuration:derived-hash`. Profile IDs and iteration values retain their existing encoding. Each record keeps its own salt and iteration count; new generation uses a fresh cryptographically random salt. Existing salts are not replaced until a new hash is written.

The default remains **PBKDF2-HMAC-SHA256 with 600,000 iterations**. Existing `PBKDF2_ALGORITHM`, `PBKDF2_DEFAULT_ITERATIONS`, and `PBKDF2_MINIMUM_ITERATIONS` overrides retain their behavior. This upgrade changes output length, not iteration policy; it does not establish FIPS certification or complete NIST compliance.

The shared password factory now generates profile `2` for account creation, password reset/change, and other callers that use its default. A correct legacy password requests an upgrade even when its iteration count is already current. The existing password-login flow saves a freshly salted current-profile hash; a subsequent login does not repeat the profile upgrade. Both email-address and user-ID login modes use this behavior. Incorrect passwords do not trigger a rehash. A failure to persist the upgrade follows the existing login failure handling, not a reported successful migration.

The legacy encrypted-user-ID verification helper also accepts a correctly verified password that needs rehash. It retains its existing credential-return behavior; it does not perform the normal login path's stored upgrade itself.

Accounts that never perform password authentication, including SSO-only accounts, do not automatically migrate through login. They migrate when a password-writing operation occurs. Do not reset those accounts solely to convert their stored hash.

## Coordinated maintenance deployment

**Backward-readable does not mean rollback-safe.** A new binary reads old records, but an old-only binary does not understand profile `2`. A password creation, reset/change, or successful legacy login can introduce the first new-profile record immediately.

1. Confirm every process that reads or writes this password database will run a binary supporting **both profile IDs**. Include authentication nodes, background jobs, and consumers that verify persisted password records.
2. Establish a maintenance window. Stop authentication traffic and password-writing activity across all affected nodes before replacing binaries. Do not leave an old-only verifier serving against a database receiving profile `2` writes.
3. Take and verify the normal coordinated backup of the application's database and associated state. Record a compatible recovery/rollback target. Treat restoration as disaster recovery with possible loss of intervening writes, not a routine downgrade mechanism.
4. Upgrade **all** affected readers and writers before resuming traffic. This change has no staged-activation flag: do not assume you can leave the new nodes running in legacy-write mode.
5. With traffic still controlled, verify an existing-password login under the configured login mode, confirm that a subsequent login works, and exercise a password reset/change or test-account creation. Do not print passwords or complete stored password records into deployment logs.
6. Resume traffic only after every participating binary is compatible. Monitor authentication failures and rehash persistence errors.

**Rolling upgrade:** Mixing old-only readers with new-profile writers is not supported by this baseline. A rolling upgrade requires a prior release that can read both profiles while continuing legacy writes, or a separately approved staged-activation design. Neither capability is implied by this change.

## Rollback boundary

Before any profile `2` write, old-only readers can still verify the existing profile `1` records, subject to the release's other compatibility constraints. **After the first profile `2` write, an old-only release is not a supported rollback target.** Confirm any target binary actually recognizes both IDs; do not infer compatibility from its release name alone.

After that boundary, recover by fixing forward or deploying a compatible binary that retains both verifiers. **This change provides no supported automatic downgrade.** PBKDF2 output is prefix-preserving: with the same algorithm, salt and iteration count, a shorter derivation matches the corresponding prefix of a longer one. Conversion is therefore not mathematically impossible, but this release does not implement or validate an operator downgrade procedure. Do not rely on manual truncation/relabeling, claim automatic downgrade, or mass-reset users as the deployment migration strategy.

## Validation

Focused real-cryptography and mocked-persistence checks:

```bash
./mvnw test -pl :dotcms-core --am \
  -Dtest=PasswordHashMigrationTest,LoginFactoryPasswordMigrationTest,UserManagerPasswordMigrationTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dmaven.build.cache.enabled=false
```

Confirm positive counts and zero failures in `dotCMS/target/surefire-reports/*.txt`. These unit tests validate compatibility and the login persistence-call contract, not a live database, SSO configuration, or production maintenance rehearsal.

Related: [password-storage security patterns](SECURITY_BACKEND.md), [approved migration specification](../../specs/20261028-154324-pbkdf2-256/spec.md), and the [OWASP password-storage guidance](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html#pbkdf2).
