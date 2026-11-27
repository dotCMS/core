# Feature Specification: Backward-compatible 256-bit password hashes

**Feature Branch**: `issue-38322-pbkdf2-256-spec`

**Created**: 2026-10-28

**Status**: Original migration approved in #38323; staged-release scope approved by the developer on 2026-10-30

**Type**: New Feature

**Issue**: https://github.com/dotCMS/core/issues/38322

**Input**: Upgrade newly generated password hashes to 256 bits through Heimdall's existing profile migration mechanism, preserve existing passwords, and upgrade legacy hashes on successful login. Track the work separately from the push-publishing user-protection change.

## Staged-release amendment (2026-10-30)

The developer approved splitting the rollout in response to the rollback-safety review on #38328. **This PR ships the reader-capability release only.** The original migration design below remains the target for a later activation release, not the acceptance criteria for this first release.

### Reader-capability release requirements

- **RC-001**: Register and verify both profile `1` (192 bits) and profile `2` (256 bits), using each record's stored salt and cost. Preserve rejection of incorrect passwords, invalid records, and unknown profiles.
- **RC-002**: Keep profile `1` as the shared generation default for all existing callers. Generated records MUST contain 24 derived bytes, retain the existing iteration configuration and hashed marker, use fresh random salts, and remain readable by an old-only reader.
- **RC-003**: Do not deprecate profile `1` or request/persist a rehash solely because of its profile. Correct current-cost logins MUST leave the stored password unchanged in both email-address and user-ID modes.
- **RC-004**: Preserve existing cost-based upgrade decisions and persistence-failure handling. Low-cost legacy login still upgrades its cost, but MUST write profile `1`, not profile `2`. Current-cost profile-`2` login MUST NOT rewrite the password solely because the write default remains profile `1`.
- **RC-005**: The encrypted-user-ID helper MUST accept correct passwords from both profiles, including outdated-cost records, reject incorrect passwords, and continue returning credentials without persisting a rehash.
- **RC-006**: Document the two-release sequence, phase-1 legacy-write compatibility conditions, and the requirement that every rollback target understand both profiles before phase-2 writes begin. Explain that old-only binaries can accept a stored profile-`2` hash string through their pre-existing legacy fallback, not merely lock users out.
- **RC-007**: Use independent fixed reference vectors and real hashing with mocked account persistence. Confirm the developer-approved tests fail before changing production code, then pass after the change. No dependency, flag, bulk reset, downgrade tooling, or new iteration policy is introduced.

### Acceptance and success criteria for this PR

1. Correct passwords verify against independent fixtures for both profiles; incorrect passwords and invalid/unknown records fail.
2. Shared generation uses profile `1` with 24 derived bytes and distinct salts; old-only readers verify generated records.
3. Repeated current-cost legacy logins preserve the exact stored hash without a rehash save in both login modes.
4. Existing low-cost upgrades persist a verifiable current-cost profile-`1` hash; failed persistence fails authentication without claiming a saved upgrade.
5. Current-cost profile-`2` login and encrypted-user-ID checks succeed without password rewriting.
6. Operator guidance distinguishes this reader-capability release from later activation and documents both the security and availability boundary of old-only readers.

These requirements supersede the immediate activation obligations in original US1/AC1-2, US1/AC5, US2/AC1, US3/AC1-2, FR-001, FR-003-004, FR-008-010, SC-002-003, and the original legacy/deployment assumptions **for this PR**. The remaining original security, verification, salt, and iteration constraints continue to apply. The stronger-profile default and profile-driven migration are deferred until this reader-capable release is the rollback floor.

The original edge-case assertion that conversion is mathematically impossible is corrected: PBKDF2 output is prefix-preserving for identical algorithm, salt, and cost. Neither phase provides a supported automatic downgrade.

## Reusable compatibility contract (developer-approved 2026-10-30)

The developer requested a system that also protects later hash changes, rather than a profile-2-only fix. Reuse Heimdall's versioned records and registry; no new framework, dependency, or runtime activation flag is needed.

- **BC-001**: Reader registration and write-profile selection MUST remain independent. Adding a future profile reader MUST NOT activate its writes or profile-driven migration.
- **BC-002**: Every future write-default change MUST retain all previously supported verifiers and their original identity/derivation semantics. Duplicate profile registration MUST fail rather than silently replace an existing verifier.
- **BC-003**: Unknown-profile, unsupported-format-version, and malformed structured hash records MUST fail authentication, including when the caller submits the stored record itself. They MUST NOT be classified as legacy plaintext/digests, rewritten, or used to bypass verification.
- **BC-004**: Genuine pre-Heimdall plaintext and digest records outside the reserved hexadecimal-version-and-colon prefix MUST retain their existing password-migration path. Values with that prefix MUST be treated as structured records; document the legacy-plaintext ambiguity and recovery/migration caveat rather than reopening the fallback.
- **BC-005**: Regression tests MUST simulate an additional future profile and a writer change, proving all older records still verify and adding readers alone does not request rehash. This test profile is not a new production hashing algorithm.
- **BC-006**: Document the repeatable release rule for every new profile: reserve a new ID and immutable reference vectors, ship its reader without new writes, wait until every allowed rollback target includes that reader, then activate writes in a separately reviewed release. General compatibility does not mean an old binary can verify an algorithm it has never shipped.

These extend the approved reader-capability scope. The developer explicitly approved the contract and tests before production hardening. A 29-test Red run exposed duplicate-ID replacement, the plaintext fallback, and unchecked malformed-field errors; production changes followed that run. The earlier decision to defer generalized unknown-profile hardening is superseded. The operator guide records that reserving the structured prefix excludes ambiguous old plaintext values from login fallback.

## Deferred activation-release design

The following original design describes phase 2. It MUST NOT be activated by the reader-capability release.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Existing users retain access (Priority: P1)

As an existing user, I can sign in with my current password after the upgrade without a forced password reset. A correct password upgrades its stored protection; an incorrect password does not change my account.

**Why this priority**: A stronger default must not lock out existing users or change their credentials.

**Independent Test**: Authenticate a known pre-change password record with correct and incorrect passwords, then repeat with the upgraded record. Exercise the supported email-address and user-ID login modes.

**Acceptance Scenarios**:

1. **Given** a pre-change 192-bit password hash, **When** the user provides the correct password, **Then** verification succeeds and identifies the record as needing upgrade.
2. **Given** a legacy password that authenticates through the normal password-login flow, **When** login completes successfully, **Then** the stored password becomes a freshly salted 256-bit hash, remains marked as hashed/encrypted, and verifies the same password.
3. **Given** a legacy or upgraded password hash, **When** the user provides an incorrect password, **Then** authentication fails and the stored password remains unchanged.
4. **Given** a successfully upgraded record whose iteration count meets current policy, **When** the user signs in again, **Then** authentication succeeds without requesting another upgrade solely because of its profile.
5. **Given** a legacy record with fewer iterations than current policy, **When** the correct password is supplied, **Then** verification uses its stored iteration count and successful login upgrades both the legacy output length and the outdated cost.

---

### User Story 2 - New password writes use the stronger profile (Priority: P1)

As an administrator or user, I expect newly created, changed, reset, or system-generated passwords to use the same current protection without choosing an algorithm manually.

**Why this priority**: Updating one caller would leave other password-writing workflows on the old default.

**Independent Test**: Generate password records through the shared password-generation entry point and verify their output length, profile, salts, verification results, and upgrade decisions. Confirm the account workflows using that entry point retain their existing behavior.

**Acceptance Scenarios**:

1. **Given** a newly generated password record, **When** its stored verifier is decoded, **Then** the derived hash contains exactly 32 bytes (256 bits) and its profile is distinct from the legacy profile.
2. **Given** the same password hashed twice, **When** the records are inspected, **Then** each has a different random salt and both verify the password.
3. **Given** a new record generated using the default configuration, **When** its cost is inspected, **Then** it retains the current 600,000-iteration default; this feature does not reduce or otherwise change iteration policy.
4. **Given** a new-profile record below the configured minimum cost, **When** its upgrade status is checked, **Then** it still requests rehash; the output-length migration does not disable existing cost upgrades.

---

### User Story 3 - Operators understand deployment and rollback limits (Priority: P1)

As an operator, I know which versions can read the new password records and can deploy without routing authentication traffic to a node unable to verify them.

**Why this priority**: Existing hashes remain readable after upgrade, but that does not make new hashes readable by old releases.

**Independent Test**: Review the release procedure against a compatibility matrix for legacy records, new records, current readers, and old-only readers. Demonstrate that an old-only reader cannot verify a new-profile record and that the operator procedure does not rely on it doing so.

**Acceptance Scenarios**:

1. **Given** a deployment containing old-only readers, **When** an operator prepares to activate new-profile writes, **Then** the documented procedure requires upgrading all password-reading/writing nodes before authentication traffic resumes, using a coordinated maintenance deployment.
2. **Given** an environment requiring a rolling upgrade, **When** a migration is planned, **Then** the procedure requires a prior reader-capability release or a separately approved staged-activation design; the new default must not be advertised as safe for mixed old-only readers and new writers.
3. **Given** at least one new-profile password has been stored, **When** rollback is considered, **Then** the documented supported target must recognize both profiles. Rolling back to an old-only release is explicitly unsafe.

### Edge Cases

- Legacy hashes already at the current iteration count still need the output-length upgrade.
- Users who never authenticate with a password retain their existing hashes until a password operation occurs; no bulk rewrite or forced reset is introduced.
- Incorrect passwords, malformed records, and unsupported profile identifiers must never authenticate or be rewritten as successful upgrades.
- Existing valid legacy records must remain usable even when their salt length differs from that of newly generated records.
- A current-profile record with outdated iteration settings still needs a cost upgrade.
- Failure to persist a rehash must not be reported as a successfully persisted migration or result in plaintext storage; retain the existing authentication failure handling.
- Switching the login identifier between email address and user ID must not change hash verification or migration behavior.
- A newer password record cannot be converted into a legacy record without the user's password; operational guidance must not suggest such a conversion.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Newly generated password verifiers MUST contain exactly 256 derived bits, using the existing PBKDF2-HMAC-SHA256 default and preserving existing algorithm/iteration configuration behavior. The default iteration count MUST remain 600,000.
- **FR-002**: The existing 192-bit profile identifier and its verification semantics MUST remain available. The current identifier MUST NOT be repurposed to mean a different derived output length.
- **FR-003**: The new profile MUST have a distinct, unused identifier and become the shared password-generation default. Stored records MUST retain the information needed to select their verifier and cost without guessing from current defaults.
- **FR-004**: Legacy-profile hashes MUST request rehash after successful verification, including records whose iterations already meet current policy. The normal password-login flow MUST persist the replacement hash without changing the user's password or identity.
- **FR-005**: Current-profile hashes that meet current iteration policy MUST verify without requesting another profile upgrade. Existing cost-based rehash decisions MUST continue to work.
- **FR-006**: Password verification MUST reject incorrect passwords and invalid/unsupported records. Failed authentication MUST NOT rewrite the stored password.
- **FR-007**: New records MUST use fresh cryptographically random salts. Verification MUST retain comparison behavior that does not reveal matching password-hash prefixes through early exit.
- **FR-008**: All existing consumers of the shared password-generation default MUST receive the new profile without caller-specific algorithm choices. Persisted generated or upgraded passwords MUST contain hashes, not plaintext, and retain the appropriate hashed/encrypted marker.
- **FR-009**: Existing email-address and user-ID password-login modes MUST both preserve successful authentication and legacy migration. Account status, authorization, login identifiers, and password-reset semantics MUST remain unchanged.
- **FR-010**: Release guidance MUST state the coordinated-deployment prerequisite, mixed-version limitations, and first-new-write rollback boundary. No claim of compatibility with old-only readers is permitted.
- **FR-011**: Automated compatibility coverage MUST include immutable, known pre-change 192-bit records, correct/incorrect passwords, lower iteration counts, new output length, distinct salts, and upgrade decisions before and after migration. Legacy fixtures MUST NOT be regenerated using the changed implementation.
- **FR-012**: Implementation MUST follow specification approval, developer-approved tests, and a confirmed failing test run for the new behavior. Implementation completion requires convergence against this specification.

### Key Entities *(include if feature involves data)*

- **Stored password record**: A verifier containing format/profile identity, salt, cost parameters, and derived hash; it is not an encrypted copy of the password.
- **Legacy hash profile**: The existing 192-bit derivation and profile identity, retained for verification and migration.
- **Current hash profile**: The new 256-bit derivation used for new writes, separately identifiable from the legacy profile.
- **Upgrade decision**: Whether a verified record needs replacement because its profile or iteration cost is outdated.
- **Reader compatibility**: Whether an application version understands the profile identifiers already stored in its password database.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All supported legacy-password fixtures authenticate with their known correct passwords and reject incorrect passwords after upgrade, without a forced user reset.
- **SC-002**: Every new password record in the acceptance tests contains exactly 32 derived bytes; repeated generation for the same password produces different salts.
- **SC-003**: One successful legacy-password login produces an upgraded stored record that accepts the same password and does not require another upgrade when it meets current policy.
- **SC-004**: Failed-authentication cases leave the persisted password record unchanged in both supported login modes.
- **SC-005**: The reviewed operator procedure identifies a compatible deployment and rollback target for every supported reader/record combination, with zero claims that old-only readers can verify the new profile.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: Shared password creation and verification, including the older password-login path and its existing automatic rehash behavior. System-generated passwords using the shared default are also affected.
- **Backward-compatibility expectations**: Preserve old records and existing user passwords. Deprecate the old profile for generation, not its ability to verify. The prior release cannot read the new profile; deployment and rollback limits are part of the feature, not an optional follow-up.
- **Known related decisions**: The push-publishing user-protection work creates replacement passwords through the shared factory and preserves existing receiver accounts (PR #38253); this feature must not change that account-preservation policy. Relevant binding architecture decisions will be consulted during planning, after this specification is approved.

## Assumptions

- This is a separate, security-sensitive change. The first PR contains this specification only; implementation follows approval by another developer, not necessarily merge.
- The scope is a 256-bit output profile using the current password-hashing family and configuration. It does not introduce Argon2, pepper infrastructure, dependencies, new iteration policy, or bulk password rewrites.
- Existing serialized hash records can identify their verifier. An implementation must preserve that distinction rather than silently reinterpret old records.
- A coordinated maintenance deployment is the baseline rollout. This feature does not itself promise a rolling activation mechanism or automatic downgrade to old-only binaries.
- The normal password-login path's rehash behavior is reused; this does not add password-based migration to SSO-only sign-ins.
- Meeting this output-length goal is not a claim of complete NIST or FIPS compliance.

## References

- Existing PBKDF2 work-factor guidance: [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html#pbkdf2).
- Salted password storage, work factors, and derived output length: [NIST SP 800-63B-4, password verifiers](https://pages.nist.gov/800-63-4/sp800-63b.html#passwordver).
