package com.dotcms.enterprise;

import static com.dotcms.enterprise.PasswordFactoryProxy.AuthenticationStatus.AUTHENTICATED;
import static com.dotcms.enterprise.PasswordFactoryProxy.AuthenticationStatus.NEEDS_REHASH;
import static com.dotcms.enterprise.PasswordFactoryProxy.AuthenticationStatus.NOT_AUTHENTICATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.dotcms.enterprise.de.qaware.heimdall.Password;
import com.dotcms.enterprise.de.qaware.heimdall.PasswordException;
import com.dotcms.enterprise.de.qaware.heimdall.PasswordImpl;
import com.dotcms.enterprise.de.qaware.heimdall.algorithm.HashAlgorithm;
import com.dotcms.enterprise.de.qaware.heimdall.algorithm.HashAlgorithmRegistryImpl;
import com.dotcms.enterprise.de.qaware.heimdall.algorithm.PBKDF2;
import com.dotcms.enterprise.de.qaware.heimdall.algorithm.PBKDF2_256;
import com.dotcms.enterprise.de.qaware.heimdall.config.ConfigCoderImpl;
import com.dotcms.enterprise.de.qaware.heimdall.salt.SecureSaltProvider;
import com.dotmarketing.util.Config;
import java.io.InputStream;
import java.util.Base64;
import java.util.Properties;
import org.junit.BeforeClass;
import org.junit.Test;

/** Compatibility checks using independently derived, immutable password records. */
public class PasswordHashMigrationTest {

    private static final Properties FIXTURES = new Properties();

    /** Loads test-only reference vectors without generating them through production code. */
    @BeforeClass
    public static void prepare() throws Exception {
        Config.initializeConfig();
        try (InputStream input = PasswordHashMigrationTest.class
                .getResourceAsStream("/password-hash-migration.properties")) {
            assertNotNull("Reference password records must be available", input);
            FIXTURES.load(input);
        }
    }

    /** Warming the reader must not request a profile upgrade for existing passwords. */
    @Test
    public void legacyCurrentCostVerifiesWithoutRehash() throws Exception {
        final String hash = FIXTURES.getProperty("legacy-current-cost");

        assertEquals(AUTHENTICATED, PasswordFactoryProxy.authPassword(password(), hash));
        assertEquals(NOT_AUTHENTICATED, PasswordFactoryProxy.authPassword("incorrect", hash));
    }

    /** Older iteration counts remain verifiable instead of using the current default. */
    @Test
    public void legacyLowCostVerifiesAndRequestsRehash() throws Exception {
        final String hash = FIXTURES.getProperty("legacy-low-cost");

        assertEquals(NEEDS_REHASH, PasswordFactoryProxy.authPassword(password(), hash));
        assertEquals(NOT_AUTHENTICATED, PasswordFactoryProxy.authPassword("incorrect", hash));
    }

    /** The new profile must recognize independent 256-bit records, including old salt lengths. */
    @Test
    public void currentReferenceRecordVerifiesWithoutProfileRehash() throws Exception {
        final String hash = FIXTURES.getProperty("current-current-cost");

        assertEquals(AUTHENTICATED, PasswordFactoryProxy.authPassword(password(), hash));
        assertEquals(NOT_AUTHENTICATED, PasswordFactoryProxy.authPassword("incorrect", hash));
    }

    /** The new profile must keep the existing cost-based upgrade behavior. */
    @Test
    public void currentLowCostStillRequestsRehash() throws Exception {
        final String hash = FIXTURES.getProperty("current-low-cost");

        assertEquals(NEEDS_REHASH, PasswordFactoryProxy.authPassword(password(), hash));
        assertEquals(NOT_AUTHENTICATED, PasswordFactoryProxy.authPassword("incorrect", hash));
    }

    /** Shared generation must keep legacy writes until a later release activates profile 2. */
    @Test
    public void generatedHashKeeps192BitsAndLegacyProfile() throws Exception {
        final String hash = PasswordFactoryProxy.generateHash(password());
        final String[] parts = hash.split(":");

        assertEquals(5, parts.length);
        assertEquals("1", parts[0]);
        assertEquals("1", parts[1]);
        assertEquals(24, Base64.getDecoder().decode(parts[4]).length);
        assertEquals(AUTHENTICATED, PasswordFactoryProxy.authPassword(password(), hash));
        assertFalse(PasswordFactoryProxy.passwordFactoryCreate().needsRehash(hash));
    }

    /** Iteration settings must continue to come from the existing configuration. */
    @Test
    public void generatedHashPreservesConfiguredIterationCount() throws Exception {
        final String hash = PasswordFactoryProxy.generateHash(password());
        final int iterations = Integer.parseInt(
                new ConfigCoderImpl().decode(hash.split(":")[3]).get("i"), 16);

        assertEquals(Config.getIntProperty("PBKDF2_DEFAULT_ITERATIONS", 600000), iterations);
    }

    /** Repeated generation must use different random salts and still verify correctly. */
    @Test
    public void repeatedGenerationUsesDistinctSalts() throws Exception {
        final String first = PasswordFactoryProxy.generateHash(password());
        final String second = PasswordFactoryProxy.generateHash(password());

        assertNotEquals(first.split(":")[2], second.split(":")[2]);
        assertTrue(PasswordFactoryProxy.passwordFactoryCreate().verify(password(), first));
        assertTrue(PasswordFactoryProxy.passwordFactoryCreate().verify(password(), second));
    }

    /** Unknown profile IDs must fail instead of selecting the current default. */
    @Test
    public void unknownProfileIsRejected() {
        final String unknown = FIXTURES.getProperty("legacy-current-cost")
                .replace("1:1:", "1:7f:");

        assertThrows(PasswordException.class,
                () -> PasswordFactoryProxy.authPassword(password(), unknown));
    }

    /** Incomplete serialized records must not authenticate. */
    @Test
    public void malformedRecordIsRejected() {
        assertThrows(PasswordException.class,
                () -> PasswordFactoryProxy.authPassword(password(), "1:1:incomplete"));
    }

    /** This release must keep generated records readable by old-only binaries. */
    @Test
    public void oldOnlyRegistryCanReadGeneratedHash() throws Exception {
        final PBKDF2 legacy = new PBKDF2();
        final Password oldReader = new PasswordImpl(new SecureSaltProvider(),
                new ConfigCoderImpl(), new HashAlgorithmRegistryImpl(legacy), legacy);
        final String hash = PasswordFactoryProxy.generateHash(password());

        assertTrue(oldReader.verify(password(), hash));
        assertFalse(oldReader.verify("incorrect", hash));
    }

    /** Future profile-2 writers require this reader-capable release as their rollback floor. */
    @Test
    public void oldOnlyRegistryCannotReadNewProfile() throws Exception {
        final PBKDF2 legacy = new PBKDF2();
        final Password oldReader = new PasswordImpl(new SecureSaltProvider(),
                new ConfigCoderImpl(), new HashAlgorithmRegistryImpl(legacy), legacy);
        final String hash = FIXTURES.getProperty("current-current-cost");

        assertThrows(PasswordException.class, () -> oldReader.verify(password(), hash));
    }

    /** Adding a future reader and later changing the writer must retain every older verifier. */
    @Test
    public void futureProfileRegistrationAndWriterSelectionPreserveOlderRecords() throws Exception {
        final HashAlgorithm[] profiles = {new PBKDF2(), new PBKDF2_256(), new FutureProfile()};
        final String[] records = {
                FIXTURES.getProperty("legacy-current-cost"),
                FIXTURES.getProperty("current-current-cost"),
                FIXTURES.getProperty("current-current-cost").replace("1:2:", "1:3:")
        };

        for (final HashAlgorithm writer : profiles) {
            final Password passwords = new PasswordImpl(new SecureSaltProvider(),
                    new ConfigCoderImpl(), new HashAlgorithmRegistryImpl(profiles), writer);

            assertEquals(Integer.toHexString(writer.getId()), passwords.hash(password()).split(":")[1]);
            for (final String record : records) {
                assertTrue(passwords.verify(password(), record));
                assertFalse(passwords.verify("incorrect", record));
                assertFalse(passwords.needsRehash(record));
            }
        }
    }

    /** Duplicate identities must not silently replace an older verifier. */
    @Test
    public void duplicateProfileIdsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new HashAlgorithmRegistryImpl(new PBKDF2(), new PBKDF2()));
    }

    /** Unsupported structured records must never enter the legacy plaintext fallback. */
    @Test
    public void unknownAndMalformedStructuredRecordsAreNotLegacyPasswords() {
        final String record = FIXTURES.getProperty("legacy-current-cost");

        assertFalse(PasswordFactoryProxy.isUnsecurePasswordHash(
                record.replace("1:1:", "1:7f:")));
        assertFalse(PasswordFactoryProxy.isUnsecurePasswordHash(
                record.replace("1:1:", "7f:1:")));
        assertFalse(PasswordFactoryProxy.isUnsecurePasswordHash("1:1:incomplete"));
    }

    /** Actual pre-Heimdall plaintext and digest records retain their migration path. */
    @Test
    public void legacyPlaintextAndDigestRemainEligibleForMigration() {
        assertTrue(PasswordFactoryProxy.isUnsecurePasswordHash("legacy-test-password"));
        assertTrue(PasswordFactoryProxy.isUnsecurePasswordHash("5f4dcc3b5aa765d61d8327deb882cf99"));
    }

    /** Test-only third identity proves the registry contract is not hardcoded to two profiles. */
    private static final class FutureProfile extends PBKDF2_256 {

        /** Uses the next identity without introducing a real production algorithm. */
        @Override
        public int getId() {
            return 3;
        }
    }

    /** Returns the known test password associated with the frozen reference records. */
    private static String password() {
        return FIXTURES.getProperty("password");
    }
}
