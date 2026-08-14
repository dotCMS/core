package com.dotcms.security.apps;

import static com.dotcms.security.apps.SecretsKeyStoreHelper.SECRETS_KEYSTORE_FILE_PATH_KEY;
import static com.dotcms.security.apps.SecretsKeyStoreHelper.SECRETS_STORE_AUTO_RECREATE;
import static com.dotcms.security.apps.SecretsKeyStoreHelper.SECRETS_STORE_LOAD_TRIES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDGenerator;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Supplier;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Deterministic regression tests for issue #36724 — a multi-node race that silently wiped the App
 * secrets store.
 *
 * These deliberately contain no concurrency and no timing assumptions. The failure the issue
 * describes is a race, but the *damage* was done by a single-threaded code path: any load failure
 * caused {@code getSecretsStore()} to back up and delete the store, and its retry loop then
 * recreated an empty one, loaded it cleanly and logged "KeyStore loaded successfully". Each test
 * below simulates one way of provoking that load failure and asserts the store survives.
 *
 * The concurrent stress test that originally reproduced the race lives alongside this class in
 * {@code SecretsStoreConcurrentWriteRaceTest}; this one is the deterministic guard required by the
 * issue's acceptance criteria.
 */
public class SecretsStoreWipeRegressionTest {

    private static final String CANARY_KEY = "issue36724-canary";
    private static final String CANARY_VALUE = "do-not-lose-me";

    private static final String PASSWORD_A = "correct-horse-battery-staple-A";
    private static final String PASSWORD_B = "a-completely-different-password-B";

    private Path storeDir;
    private Path storePath;
    private String previousPath;
    private String previousAutoRecreate;
    private String previousLoadTries;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Before
    public void setUp() throws IOException {
        storeDir = Files.createTempDirectory("issue36724-secrets-");
        storePath = storeDir.resolve("dotSecretsStore.p12");

        previousPath = Config.getStringProperty(SECRETS_KEYSTORE_FILE_PATH_KEY, null);
        previousAutoRecreate = Config.getStringProperty(SECRETS_STORE_AUTO_RECREATE, null);
        previousLoadTries = Config.getStringProperty(SECRETS_STORE_LOAD_TRIES, null);

        Config.setProperty(SECRETS_KEYSTORE_FILE_PATH_KEY, storePath.toString());
        // Keep the retry budget small so a deliberately unreadable store fails fast.
        Config.setProperty(SECRETS_STORE_LOAD_TRIES, "2");

        // The notification latch is static and never resets in production, so within one Surefire
        // fork it stays set once any test trips it. Clearing it here makes the transition assertion
        // below independent of whichever sibling test ran first.
        SecretsKeyStoreHelper.resetNotifiedLoadFailureLatch();
    }

    @After
    public void tearDown() throws IOException {
        // Each of these is null when the property was unset before the test, and
        // Config.setProperty(key, null) does not reliably clear it -- it can leave the test's own
        // value in place. Restore an explicit safe value instead, matching the guarded pattern in
        // SecretsStoreKeyStoreImplTest. Both classes run in MainSuite3a, so leaking this temp path
        // would point a sibling secrets test at a directory this method then deletes.
        Config.setProperty(SECRETS_KEYSTORE_FILE_PATH_KEY, previousPath == null ? "" : previousPath);
        Config.setProperty(SECRETS_STORE_AUTO_RECREATE,
                previousAutoRecreate == null ? "false" : previousAutoRecreate);
        Config.setProperty(SECRETS_STORE_LOAD_TRIES,
                previousLoadTries == null ? "3" : previousLoadTries);

        if (null != storeDir && Files.exists(storeDir)) {
            try (java.util.stream.Stream<Path> paths = Files.walk(storeDir)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(
                        path -> path.toFile().delete());
            }
        }
    }

    /**
     * Reads a secret as plaintext. {@link SecretsKeyStoreHelper#getValue(String)} returns the value
     * still encrypted -- decryption is normally done by the caching wrapper -- so a test comparing
     * its result directly against the plaintext it stored would never match.
     */
    private String readSecret(final SecretsKeyStoreHelper helper, final String key) {
        final char[] stored = helper.getValue(key);
        assertFalse("the secret must be present in the store",
                Arrays.equals(AppsCache.CACHE_404, stored));
        return new String(helper.decrypt(stored));
    }

    private SecretsKeyStoreHelper helperWithPassword(final String password) {
        final Supplier<char[]> supplier = password::toCharArray;
        return new SecretsKeyStoreHelper(supplier, ImmutableList.of());
    }

    /**
     * Any file in the store's directory whose name ends in the store's name is a backup created by
     * {@code backupAndRemoveKeyStore()} (it prefixes a yyyyMMddHHmmss stamp).
     */
    private long countBackups() throws IOException {
        try (java.util.stream.Stream<Path> paths = Files.list(storeDir)) {
            return paths.filter(path -> path.getFileName().toString().endsWith("-dotSecretsStore.p12"))
                    .count();
        }
    }

    private long countTempFiles() throws IOException {
        try (java.util.stream.Stream<Path> paths = Files.list(storeDir)) {
            return paths.filter(path -> path.getFileName().toString().endsWith(".tmp")).count();
        }
    }

    /**
     * Method to test: {@link SecretsKeyStoreHelper#getValue(String)} against a torn store.
     * Given Scenario: a canary secret is stored, then the file is truncated to half its length —
     * exactly what a concurrent reader observed while another node streamed the store back over the
     * destination (defect B1).
     * Expected Result: the failure is raised, and the store file is still on disk, untouched. Before
     * this fix the truncated read triggered backup + delete + regenerate-empty, and the canary was
     * gone for good with only a WARN and an INFO claiming success.
     */
    @Test
    public void test_tornStore_isNotWipedAndFailsLoudly() throws Exception {
        final SecretsKeyStoreHelper helper = helperWithPassword(PASSWORD_A);
        helper.saveValue(CANARY_KEY, CANARY_VALUE.toCharArray());

        assertTrue("the store must exist after a save", Files.exists(storePath));
        final byte[] intact = Files.readAllBytes(storePath);
        assertTrue("a store holding a secret cannot be empty", intact.length > 0);

        // Simulate the torn read: a partial store, as a reader on another node would have seen.
        Files.write(storePath, Arrays.copyOf(intact, intact.length / 2));
        final long tornLength = Files.size(storePath);

        try {
            helper.getValue(CANARY_KEY);
            fail("an unreadable store must raise rather than silently return an empty one");
        } catch (DotRuntimeException expected) {
            // the store could not be read, which is the honest outcome
        }

        assertTrue("the store must NOT be deleted when it cannot be read", Files.exists(storePath));
        assertEquals("the store must be left exactly as it was found",
                tornLength, Files.size(storePath));
        assertEquals("nothing may be backed up and removed by default", 0, countBackups());
    }

    /**
     * Method to test: {@link SecretsKeyStoreHelper#getValue(String)} with a mismatched password.
     * Given Scenario: the store is written by one node and read by another that derives a different
     * password — the cluster-salt rotation / per-node SECRETS_KEYSTORE_PASSWORD_KEY drift described
     * in the issue. This needs no concurrency at all.
     * Expected Result: the store survives intact and the node that owns the correct password can
     * still read the canary. Before this fix the mismatch wiped the store, and the backup it left
     * behind was encrypted with the very password that had just failed — which is why customers
     * reported that restoring the backup did not restore access.
     */
    @Test
    public void test_wrongPassword_preservesStoreAndOtherNodeStillReads() throws Exception {
        final SecretsKeyStoreHelper nodeA = helperWithPassword(PASSWORD_A);
        nodeA.saveValue(CANARY_KEY, CANARY_VALUE.toCharArray());
        final byte[] intact = Files.readAllBytes(storePath);

        final SecretsKeyStoreHelper nodeB = helperWithPassword(PASSWORD_B);
        try {
            nodeB.getValue(CANARY_KEY);
            fail("a store that cannot be decrypted must raise, not be silently replaced");
        } catch (DotRuntimeException expected) {
            // correct: this node cannot decrypt the shared store
        }

        assertTrue("the store must survive a password mismatch", Files.exists(storePath));
        assertTrue("the store bytes must be untouched",
                Arrays.equals(intact, Files.readAllBytes(storePath)));
        assertEquals("no backup may be taken by default", 0, countBackups());

        // The decisive assertion: the data was never at risk.
        assertEquals("the node with the correct password must still read the canary",
                CANARY_VALUE, readSecret(nodeA, CANARY_KEY));
    }

    /**
     * Method to test: the {@code SECRETS_STORE_AUTO_RECREATE} opt-in.
     * Given Scenario: an operator explicitly accepts losing the secrets to get a working store back,
     * which is the pre-fix behaviour.
     * Expected Result: the store is backed up and replaced with an empty one, and the read returns
     * empty instead of raising. This pins the escape hatch so the destructive path stays available
     * to anyone who genuinely wants it.
     */
    @Test
    public void test_autoRecreateOptIn_restoresLegacyBehaviour() throws Exception {
        final SecretsKeyStoreHelper nodeA = helperWithPassword(PASSWORD_A);
        nodeA.saveValue(CANARY_KEY, CANARY_VALUE.toCharArray());

        Config.setProperty(SECRETS_STORE_AUTO_RECREATE, "true");

        final SecretsKeyStoreHelper nodeB = helperWithPassword(PASSWORD_B);

        assertTrue("the recreated store is empty, so the canary is gone",
                Arrays.equals(AppsCache.CACHE_404, nodeB.getValue(CANARY_KEY)));
        assertEquals("the old store must have been backed up before being replaced",
                1, countBackups());
        assertTrue("a fresh store must have been created", Files.exists(storePath));
    }

    /**
     * Method to test: {@link SecretsKeyStoreHelper#saveValue(String, char[])} publishing.
     * Given Scenario: repeated saves, the operation that previously truncated the destination and
     * streamed the bytes back in.
     * Expected Result: the store is never observed at zero length between saves, every save is
     * immediately readable, and no .tmp file is left behind. The old code only deleted its temp file
     * on the happy path, leaving a copy of every secret on any failure.
     */
    @Test
    public void test_savesPublishAtomicallyAndLeaveNoTempFiles() throws Exception {
        final SecretsKeyStoreHelper helper = helperWithPassword(PASSWORD_A);

        for (int i = 0; i < 25; i++) {
            final String key = "key-" + i;
            final String value = UUIDGenerator.generateUuid();
            helper.saveValue(key, value.toCharArray());

            assertTrue("the store must never be truncated to nothing", Files.size(storePath) > 0);
            assertEquals("every published value must be immediately readable",
                    value, readSecret(helper, key));
        }

        assertEquals("no temp file may survive a save", 0, countTempFiles());
        assertEquals("saving must never back anything up", 0, countBackups());
    }

    /**
     * Method to test: {@link SecretsKeyStoreHelper#shouldReportLoadFailure()}
     * Given Scenario: a store this node cannot open. Every consumer wraps its read defensively, so
     * the state persists and every subsequent read reaches the same failure handler.
     * Expected Result: the failure is reported once, then suppressed until the interval elapses —
     * while every read still raises. Without this the actionable ERROR (and the admin notification,
     * which writes a row) would be emitted on every page render, login and content save.
     */
    @Test
    public void test_repeatedLoadFailures_areReportedOnceButAlwaysRaise() throws Exception {
        assertFalse("setUp cleared the latch, so this test can assert the transition",
                SecretsKeyStoreHelper.hasNotifiedLoadFailure());

        final SecretsKeyStoreHelper nodeA = helperWithPassword(PASSWORD_A);
        nodeA.saveValue(CANARY_KEY, CANARY_VALUE.toCharArray());

        final SecretsKeyStoreHelper nodeB = helperWithPassword(PASSWORD_B);

        assertTrue("the first failure must always be reported", nodeB.shouldReportLoadFailure());
        assertFalse("an immediately following failure must be suppressed",
                nodeB.shouldReportLoadFailure());
        assertFalse(nodeB.shouldReportLoadFailure());

        // Suppressing the report must not suppress the failure itself.
        for (int i = 0; i < 3; i++) {
            try {
                nodeB.getValue(CANARY_KEY);
                fail("every read of an unreadable store must raise, reported or not");
            } catch (DotRuntimeException expected) {
                // correct
            }
        }

        assertTrue("the store must survive every one of those attempts", Files.exists(storePath));
        assertEquals("and must never be backed up or replaced", 0, countBackups());
        assertEquals("the owning node must still read its secret", CANARY_VALUE,
                readSecret(nodeA, CANARY_KEY));

        // The admin notification is a persisted row, so unlike the log line it must not repeat on
        // an interval. A container run with the interval shortened to 10s produced 20 notification
        // rows in five minutes before this was separated out.
        assertTrue("the notification latch must be set after the first failure",
                SecretsKeyStoreHelper.hasNotifiedLoadFailure());

        // A second helper on the same broken store must not raise another notification: the latch
        // is per JVM, not per instance, because an operator does not need the same instruction
        // twice just because two components each hold a helper.
        final SecretsKeyStoreHelper nodeC = helperWithPassword("yet-another-wrong-password");
        try {
            nodeC.getValue(CANARY_KEY);
            fail("still unreadable for this password");
        } catch (DotRuntimeException expected) {
            // correct
        }
        assertTrue("latch stays set; no second notification",
                SecretsKeyStoreHelper.hasNotifiedLoadFailure());
    }

    /**
     * Method to test: the retry-count floor in {@code loadSecretsStore}.
     * Given Scenario: SECRETS_STORE_LOAD_TRIES misconfigured to 0, with a perfectly readable store.
     * Expected Result: the secret still reads. Without a floor the loop never runs, no failure is
     * recorded, and the terminal handler declares an intact store permanently unreadable on this
     * node -- blaming a password or corruption problem that does not exist.
     */
    @Test
    public void test_zeroConfiguredTries_stillReadsAReadableStore() throws Exception {
        final SecretsKeyStoreHelper helper = helperWithPassword(PASSWORD_A);
        helper.saveValue(CANARY_KEY, CANARY_VALUE.toCharArray());

        Config.setProperty(SECRETS_STORE_LOAD_TRIES, "0");

        assertEquals("a misconfigured retry count must not make a good store unreadable",
                CANARY_VALUE, readSecret(helper, CANARY_KEY));

        Config.setProperty(SECRETS_STORE_LOAD_TRIES, "-5");
        assertEquals("negative is floored too", CANARY_VALUE, readSecret(helper, CANARY_KEY));
    }

    /**
     * Method to test: the retry backoff in {@code loadSecretsStore}.
     * Given Scenario: a torn store, so the read retries with backoff, on a thread that is already
     * interrupted.
     * Expected Result: the failure still raises, and the thread's interrupt flag survives.
     * Thread.sleep clears the flag when it throws, so swallowing InterruptedException would lose a
     * shutdown or timeout signal and leave the loop sleeping on a thread asked to stop.
     */
    @Test
    public void test_interruptDuringRetryBackoff_isNotSwallowed() throws Exception {
        final SecretsKeyStoreHelper helper = helperWithPassword(PASSWORD_A);
        helper.saveValue(CANARY_KEY, CANARY_VALUE.toCharArray());

        // Force the retrying (non-integrity) path: a truncated store, not a bad password.
        final byte[] intact = Files.readAllBytes(storePath);
        Files.write(storePath, Arrays.copyOf(intact, intact.length / 2));
        Config.setProperty(SECRETS_STORE_LOAD_TRIES, "3");

        Thread.currentThread().interrupt();
        try {
            helper.getValue(CANARY_KEY);
            fail("a torn store must still raise");
        } catch (DotRuntimeException expected) {
            // correct
        } finally {
            // Read and clear, so the assertion below is about what the helper left behind and this
            // test cannot leak an interrupt into whatever runs next in the suite.
            final boolean stillInterrupted = Thread.interrupted();
            assertTrue("the interrupt flag must survive the retry backoff", stillInterrupted);
        }

        assertTrue("and the store must still be intact", Files.exists(storePath));
        assertEquals("with nothing backed up or replaced", 0, countBackups());
    }

    /**
     * Method to test: file permissions on the published store.
     * Given Scenario: the store is written into a directory shared across the cluster.
     * Expected Result: it is readable only by its owner. Previously both the temp file and the store
     * were created with whatever the default umask allowed, for a file that holds every App
     * credential in plain reach of anything that can read the assets volume.
     */
    @Test
    public void test_publishedStoreIsNotWorldReadable() throws Exception {
        final SecretsKeyStoreHelper helper = helperWithPassword(PASSWORD_A);
        helper.saveValue(CANARY_KEY, CANARY_VALUE.toCharArray());

        final File storeFile = storePath.toFile();
        if (!storePath.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            return; // permission model does not apply
        }

        assertFalse("the secrets store must not be readable by other users",
                storeFile.canRead() && Files.getPosixFilePermissions(storePath).stream()
                        .anyMatch(perm -> perm.name().startsWith("OTHERS")
                                || perm.name().startsWith("GROUP")));
    }
}
