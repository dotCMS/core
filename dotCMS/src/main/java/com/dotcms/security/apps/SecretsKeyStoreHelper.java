package com.dotcms.security.apps;

import static com.dotcms.security.apps.AppsCache.CACHE_404;
import static com.dotcms.security.apps.AppsUtil.digest;

import com.dotcms.api.system.event.Visibility;
import com.dotcms.auth.providers.jwt.factories.SigningKeyFactory;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import io.vavr.control.Try;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.SecretKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.apache.commons.lang.time.FastDateFormat;


/**
 * This is basically a safe repository implemented using java.security.KeyStore
 * Which according to the official Java documentation Represents a storage facility for cryptographic keys and certificates.
 * More info Below:
 * @see <a href=https://stackoverflow.com/questions/6243446/how-to-store-a-simple-key-string-inside-java-keystore>https://stackoverflow.com/questions/6243446/how-to-store-a-simple-key-string-inside-java-keystore</a>
 * @see <a href=https://medium.com/@danojadias/aes-256bit-encryption-decryption-and-storing-in-the-database-using-java-2ada3f2a0b14>https://medium.com/@danojadias/aes-256bit-encryption-decryption-and-storing-in-the-database-using-java-2ada3f2a0b14</a>
 * @see <a href=https://neilmadden.blog/2017/11/17/java-keystores-the-gory-details/>https://neilmadden.blog/2017/11/17/java-keystores-the-gory-details</a>
 * This class does not offer any caching for that purpose you need to consume this class through {@link SecretCachedKeyStoreImpl}
 */
public class SecretsKeyStoreHelper {

    static final String SECRETS_KEYSTORE_PASSWORD_KEY = "SECRETS_KEYSTORE_PASSWORD_KEY";
    private static final String SECRETS_STORE_FILE = "dotSecretsStore.p12";
    private static final String SECRETS_STORE_KEYSTORE_TYPE = "pkcs12";
    private static final String SECRETS_STORE_SECRET_KEY_FACTORY_TYPE = "PBE";
    static final String SECRETS_STORE_LOAD_TRIES = "SECRETS_STORE_LOAD_TRIES";

    /**
     * Whether an App secrets store that cannot be loaded may be backed up and replaced with an
     * empty one. Defaults to {@code false}: the store on disk is the only copy of every App
     * credential, and a backup taken at that point is encrypted with the password that just
     * failed, so replacing it is not a recovery -- it is the data loss reported in issue #36724.
     */
    static final String SECRETS_STORE_AUTO_RECREATE = "SECRETS_STORE_AUTO_RECREATE";

    /**
     * Base delay between attempts to re-read the store. Multiplied by the attempt number, so the
     * default rides out roughly 300ms of a concurrent write on another node.
     */
    private static final String SECRETS_STORE_LOAD_RETRY_BACKOFF_MILLIS =
            "SECRETS_STORE_LOAD_RETRY_BACKOFF_MILLIS";

    /**
     * How often a persistently unreadable store may be reported.
     *
     * Every consumer of a secret wraps its read defensively -- typically
     * {@code Try.of(() -> getSecrets(...)).getOrElse(Optional.empty())} -- so a store this node
     * cannot open degrades each App to "not configured" rather than raising into the request. The
     * upside is that a mismatched store never takes an instance down; the downside is that the
     * ERROR logged here is the only signal, and it would otherwise be emitted on every read: once
     * per page render that touches a secret, per login attempt, per content save. An actionable
     * message repeated thousands of times is not actionable, so it is reported once per interval.
     */
    private static final String SECRETS_STORE_LOAD_FAILURE_REPORT_INTERVAL_MILLIS =
            "SECRETS_STORE_LOAD_FAILURE_REPORT_INTERVAL_MILLIS";

    static final String SECRETS_KEYSTORE_FILE_PATH_KEY = "SECRETS_KEYSTORE_FILE_PATH_KEY";
    private static final String APPS_KEY_PROVIDER_CLASS = "APPS_KEY_PROVIDER_CLASS";
    private final String secretsKeyStorePath;
    private final List<StoreCreatedListener> storeCreatedListeners;
    private final Supplier<char[]> passwordSupplier;

    /**
     * When this instance last reported an unreadable store. Per-instance rather than static so that
     * two helpers pointed at different stores cannot silence each other.
     */
    private final AtomicLong lastLoadFailureReportAt = new AtomicLong(0);

    /**
     * When this instance last exhausted its attempts and gave up, or 0 if the last read succeeded.
     *
     * Used to stop paying the retry backoff over and over for a failure already known to be
     * persistent. The wrong-password case never retries -- an integrity failure is deterministic and
     * breaks immediately -- but a truncated or zero-byte store raises an IOException with no
     * UnrecoverableKeyException cause, which is indistinguishable from a torn read mid-write and so
     * is retried. Verified against the JDK: a half-truncated store gives EOFException and a
     * zero-byte one a bare IOException, neither with that cause. Now that a store is preserved
     * rather than wiped and rebuilt, that state persists, and every read -- page render, login,
     * content save -- would burn the full backoff (300ms at defaults) before degrading.
     */
    private final AtomicLong lastTerminalFailureAt = new AtomicLong(0);

    /**
     * Whether the admin notification for an unreadable store has already been raised in this JVM.
     *
     * Separate from {@link #lastLoadFailureReportAt}, and deliberately a one-shot latch rather than
     * an interval: a log line is a stream, but a notification is a persisted row and a permanent
     * item in an administrator's tray. Repeating it says nothing new. Measured on a container with
     * the report interval shortened to 10s, a store left unreadable for five minutes produced 20
     * notification rows; at the 60s default that is still thousands a day for a store nobody has
     * got round to fixing. The recurring signal belongs in the log; the notification only has to
     * say "go and look" once.
     *
     * Static, so it is one notification per JVM rather than per helper instance -- an operator does
     * not need the same instruction twice because two components each hold a helper. A store that
     * breaks again after being fixed re-notifies on the next restart, which in practice is how a
     * salt or password change arrives anyway.
     */
    private static final AtomicBoolean LOAD_FAILURE_NOTIFIED = new AtomicBoolean(false);

    @VisibleForTesting
    public static String getSecretStorePath() {
        final Supplier<String> supplier = () -> APILocator.getFileAssetAPI().getRealAssetsRootPath()
                + File.separator + "server" + File.separator + "secrets" + File.separator + SECRETS_STORE_FILE;
        final String dirPath = Config.getStringProperty(SECRETS_KEYSTORE_FILE_PATH_KEY, supplier.get());
        return Paths.get(dirPath).normalize().toString();
    }

    private SecretsKeyStoreHelper(final String secretsKeyStorePath,
            final Supplier<char[]> passwordSupplier,
            final List<StoreCreatedListener> storeCreatedListeners) {
        this.secretsKeyStorePath = secretsKeyStorePath;
        this.passwordSupplier = passwordSupplier;
        this.storeCreatedListeners = storeCreatedListeners;
    }


    SecretsKeyStoreHelper( final Supplier<char[]> passwordSupplier, final List<StoreCreatedListener> storeCreatedListeners) {
        this(getSecretStorePath(), passwordSupplier, storeCreatedListeners);
    }

   public SecretsKeyStoreHelper() {
       this(getSecretStorePath(),() -> Config
               .getStringProperty(SECRETS_KEYSTORE_PASSWORD_KEY,
                       digest(ClusterFactory.getClusterSalt())).toCharArray(), ImmutableList.of());
    }

    /**
     * This will create a Keystore file for reading/writing if there is not one already there,
     * otherwise, it will return the one there
     * 
     * @return
     */
    private File createStoreIfNeeded() {
        final File secretStoreFile = new File(secretsKeyStorePath);
        if (!secretStoreFile.exists()) {
            try {
                final KeyStore keyStore = KeyStore.getInstance(SECRETS_STORE_KEYSTORE_TYPE);
                keyStore.load(null, passwordSupplier.get());
                saveSecretsStore(keyStore);
                //broadcast
                for(final StoreCreatedListener listener: this.storeCreatedListeners){
                    listener.onStoreCreated();
                }
            } catch (Exception e) {
                Logger.error(this.getClass(), "unable to create secrets store " + SECRETS_STORE_FILE + ": " + e);
                throw new DotRuntimeException(e);
            }
        }
        return secretStoreFile;
    }

   /**
    * loads up the Keystore from disk
    */
    @VisibleForTesting
    KeyStore getSecretsStore() {
        return loadSecretsStore(true);
    }

    /**
     * @param allowRecreate whether a terminal failure may fall back to
     *        {@link #handleUnrecoverableLoad(IOException, boolean)}'s destructive path. False on the
     *        single retry after a recreate, so a store that still cannot be loaded raises instead of
     *        recursing forever.
     */
    private KeyStore loadSecretsStore(final boolean allowRecreate) {

        // Created at most once per call, deliberately OUTSIDE the retry loop below. Calling
        // createStoreIfNeeded() on every attempt is what allowed a transient failure to be
        // "recovered" into a brand new empty store: the first attempt deleted the file, the second
        // recreated it empty, loaded it cleanly and reported success (issue #36724).
        final File secretStoreFile = createStoreIfNeeded();

        // Floored at one attempt. A configured 0 or a negative value would skip the loop entirely,
        // leaving no failure to report, and the terminal handler would then declare a perfectly
        // intact store permanently unreadable on this node -- with a diagnosis pointing at a
        // password or corruption problem that does not exist. One attempt is the minimum that can
        // answer the question the method was asked.
        final int configuredTries = Math.max(1,
                Config.getIntProperty(SECRETS_STORE_LOAD_TRIES, 3));
        // Retrying only helps a genuinely transient failure. If this instance already exhausted its
        // attempts recently, the condition is persistent, so try once and degrade instead of
        // sleeping through the backoff on every read. The window is the report interval: once it
        // lapses the full retry budget returns, so a store that has since been fixed is picked up.
        final int maxLoadTries = recentlyFailedTerminally() ? 1 : configuredTries;
        // Floored at zero: a negative backoff would make Thread.sleep throw
        // IllegalArgumentException, which is not an IOException and would escape the retry loop.
        final long backoffMillis = Math.max(0L,
                Config.getLongProperty(SECRETS_STORE_LOAD_RETRY_BACKOFF_MILLIS, 100));

        IOException lastFailure = null;
        boolean interrupted = false;

        for (int tryCount = 1; tryCount <= maxLoadTries; tryCount++) {
            try (InputStream inputStream = Files.newInputStream(secretStoreFile.toPath())) {

                final KeyStore keyStore = KeyStore.getInstance(SECRETS_STORE_KEYSTORE_TYPE);
                keyStore.load(inputStream, passwordSupplier.get());

                lastTerminalFailureAt.set(0);

                if (tryCount > 1) {
                    Logger.info(SecretsKeyStoreHelper.class, String.format(
                            "App secrets store recovered on attempt %d of %d.", tryCount,
                            maxLoadTries));
                }
                return keyStore;

            } catch (IOException e) {
                lastFailure = e;

                // An integrity failure is deterministic -- either the password is wrong or the
                // bytes are genuinely corrupt. Re-reading cannot change the outcome, so stop
                // rather than spend the remaining attempts on it.
                if (isIntegrityFailure(e) || tryCount == maxLoadTries) {
                    break;
                }

                Logger.warn(SecretsKeyStoreHelper.class, String.format(
                        "Could not read the App secrets store on attempt %d of %d (%s); retrying."
                                + " Another node may be writing it.",
                        tryCount, maxLoadTries, e.getMessage()));

                try {
                    Thread.sleep(backoffMillis * tryCount);
                } catch (final InterruptedException ie) {
                    // Thread.sleep clears the interrupt flag when it throws, so restore it: the
                    // caller -- a shutdown hook, a request timeout -- is entitled to see that this
                    // thread was interrupted. Stop retrying too, rather than sleeping again on a
                    // thread that has been asked to stop; the loop falls through to the terminal
                    // handler, which preserves the store and raises.
                    Thread.currentThread().interrupt();
                    interrupted = true;
                    break;
                }

            } catch (GeneralSecurityException e) {
                Logger.error(this.getClass(),
                        "Unable to load secrets store " + SECRETS_STORE_FILE + ": " + e.getMessage(), e);
                throw new DotRuntimeException(e);
            }
        }

        // An interrupt says nothing about the store, so it must not suppress the retry budget for
        // the next caller.
        if (!interrupted) {
            lastTerminalFailureAt.set(System.currentTimeMillis());
        }

        return handleUnrecoverableLoad(lastFailure, allowRecreate, interrupted);
    }

    /**
     * Distinguishes "cannot decrypt" from "cannot read".
     *
     * A PKCS12 integrity failure -- a wrong password, or corrupt content -- arrives as an
     * {@link IOException} wrapping an {@link UnrecoverableKeyException}. A torn, truncated or
     * missing file does not, and is worth retrying. Before issue #36724 both were handled
     * identically, which is why a password mismatch destroyed the store just as readily as
     * corruption did.
     */
    private static boolean isIntegrityFailure(final IOException e) {
        return null != e && e.getCause() instanceof UnrecoverableKeyException;
    }

    /**
     * Whether this instance gave up on a read within the report interval, meaning the failure is
     * already known to be persistent and retrying would only add latency.
     */
    private boolean recentlyFailedTerminally() {
        final long last = lastTerminalFailureAt.get();
        if (0 == last) {
            return false;
        }
        final long interval = Config
                .getLongProperty(SECRETS_STORE_LOAD_FAILURE_REPORT_INTERVAL_MILLIS, 60000);
        return System.currentTimeMillis() - last < interval;
    }

    /**
     * Raises the admin notification for an unreadable store at most once per JVM.
     * See {@link #LOAD_FAILURE_NOTIFIED}.
     */
    private void notifyLoadFailureOnce() {
        if (LOAD_FAILURE_NOTIFIED.compareAndSet(false, true)) {
            Try.run(this::sendFailureNotification);
        }
    }

    /**
     * Whether the admin notification has yet been raised in this JVM. Only for tests to assert the
     * one-shot behaviour without generating notification rows.
     */
    @VisibleForTesting
    static boolean hasNotifiedLoadFailure() {
        return LOAD_FAILURE_NOTIFIED.get();
    }

    /**
     * Clears the notification latch. Tests only.
     *
     * The latch is static and never resets in production, deliberately -- see
     * {@link #LOAD_FAILURE_NOTIFIED}. Within one Surefire fork that means it stays set once any test
     * trips it, so without this a test could only ever assert {@code true}, and asserting the
     * false-to-true transition or "not notified yet" would silently depend on which siblings ran
     * first. Call it in setup rather than teardown, so a test is unaffected by whatever ran before.
     */
    @VisibleForTesting
    static void resetNotifiedLoadFailureLatch() {
        LOAD_FAILURE_NOTIFIED.set(false);
    }

    /**
     * Whether the ERROR for an unreadable store should be logged now, rate-limited to one per
     * {@link #SECRETS_STORE_LOAD_FAILURE_REPORT_INTERVAL_MILLIS}. The first failure always logs.
     *
     * Covers the log line only. The admin notification is a separate one-shot latch --
     * {@link #notifyLoadFailureOnce()} -- because a persisted notification row should not repeat on
     * an interval the way a log line can.
     */
    @VisibleForTesting
    boolean shouldReportLoadFailure() {
        final long interval = Config
                .getLongProperty(SECRETS_STORE_LOAD_FAILURE_REPORT_INTERVAL_MILLIS, 60000);
        final long now = System.currentTimeMillis();
        final long last = lastLoadFailureReportAt.get();

        return now - last > interval && lastLoadFailureReportAt.compareAndSet(last, now);
    }

    /**
     * Terminal handler for a store that could not be loaded after every attempt.
     *
     * It does not delete anything unless an operator has explicitly opted in via
     * {@link #SECRETS_STORE_AUTO_RECREATE}. Because {@link #saveValue(String, char[])} loads the
     * store before mutating it, throwing here also prevents a caller from writing a
     * nearly-empty store over a file that is intact but merely unreadable by this node.
     */
    private KeyStore handleUnrecoverableLoad(final IOException cause, final boolean allowRecreate,
            final boolean interrupted) {

        // The interrupted case is reported for what it is. Falling through to "could not be read"
        // would blame the store for a shutdown or a request timeout, sending an operator looking for
        // a corrupt file or a salt mismatch that does not exist.
        final String diagnosis;
        if (interrupted) {
            diagnosis = "the thread was interrupted while waiting to re-read it, so the read was"
                    + " abandoned. The store itself may well be fine; this usually means a shutdown"
                    + " or a request timeout, not a problem with the file";
        } else if (isIntegrityFailure(cause)) {
            diagnosis = "the store could not be decrypted. The password is derived from the cluster"
                    + " salt, so this usually means the salt changed or SECRETS_KEYSTORE_PASSWORD_KEY"
                    + " differs between nodes";
        } else {
            diagnosis = "the store could not be read";
        }

        // An interrupt never justifies the destructive path, even with SECRETS_STORE_AUTO_RECREATE
        // enabled. Nothing has been learned about the store: the read was abandoned before it could
        // fail on its own merits. Backing it up and replacing it with an empty one here would let a
        // shutdown or a request timeout discard every App credential.
        if (interrupted || !allowRecreate
                || !Config.getBooleanProperty(SECRETS_STORE_AUTO_RECREATE, false)) {

            // The store is preserved, so this state persists until an operator fixes it -- and every
            // read lands here again. Report it periodically instead of once per read; see
            // SECRETS_STORE_LOAD_FAILURE_REPORT_INTERVAL_MILLIS.
            if (shouldReportLoadFailure()) {
                Logger.error(SecretsKeyStoreHelper.class, String.format(
                        "Unable to load the App secrets store '%s': %s. The existing store has been"
                                + " LEFT IN PLACE and no secrets were lost. Apps will not work until"
                                + " this is resolved. Restore the correct salt/password, or set"
                                + " %s=true to back it up and start over -- which DISCARDS every"
                                + " stored App credential.",
                        secretsKeyStorePath, diagnosis, SECRETS_STORE_AUTO_RECREATE), cause);
            } else {
                Logger.debug(SecretsKeyStoreHelper.class,
                        () -> "App secrets store is still unreadable: " + diagnosis);
            }

            // Not on the interrupted path. The notification latch is one-shot for the life of the
            // JVM, so spending it here -- on a read that was abandoned, where the diagnosis above
            // says the store may well be fine -- would silently suppress the notification for a
            // genuinely unreadable store failing later in the same JVM. The throttled ERROR still
            // fires either way, so the signal is not lost, only the one-time "go and look".
            if (!interrupted) {
                notifyLoadFailureOnce();
            }

            throw new SecretsStoreUnreadableException(
                    "Unable to load the App secrets store: " + diagnosis, cause);
        }

        // The destructive path runs once and then succeeds, so it is never throttled.
        Try.run(this::sendFailureNotification);
        Logger.error(SecretsKeyStoreHelper.class, String.format(
                "Unable to load the App secrets store '%s': %s. %s is enabled, so it will be backed"
                        + " up and replaced with an EMPTY store. Every App credential must be"
                        + " re-entered; note the backup is encrypted with the same password that"
                        + " just failed.",
                secretsKeyStorePath, diagnosis, SECRETS_STORE_AUTO_RECREATE), cause);

        Sneaky.sneaked(this::backupAndRemoveKeyStore).run();
        // allowRecreate=false: if the freshly created store also fails to load, raise rather than
        // recurse into another backup-and-delete cycle.
        return loadSecretsStore(false);
    }


    /**
     * Persists the keystore, tries to do a 2 phase commit, saving to a tmp file, copying that to the
     * keystore file, then deleting the tmp file
     * 
     * @param keyStore
     * @return
     */
    private KeyStore saveSecretsStore(final KeyStore keyStore) {
        final File secretStoreFile = new File(secretsKeyStorePath);
        secretStoreFile.getParentFile().mkdirs();

        File secretStoreFileTmp = null;
        try {
            // Unique per writer, and in the destination's own directory because an atomic move is
            // only available within a single filesystem.
            //
            // The name used to be derived from currentTimeMillis() alone, so two saves on this node
            // landing in the same millisecond resolved to the same path, both opened it, and
            // interleaved -- and publishAtomically would then rename the interleaved result into
            // place as the live store. That used to be self-correcting, because a store that failed
            // to load was wiped and rebuilt; now that the store is preserved (issue #36724), such a
            // collision would leave it corrupt until an operator intervened.
            //
            // createTempFile also creates the file with owner-only permissions on POSIX, so the tmp
            // never exists world-readable in the window before restrictPermissions runs.
            secretStoreFileTmp = Files.createTempFile(secretStoreFile.getParentFile().toPath(),
                    SECRETS_STORE_FILE + "_", ".tmp").toFile();

            try (FileOutputStream fos = new FileOutputStream(secretStoreFileTmp)) {
                keyStore.store(fos, passwordSupplier.get());
                fos.flush();
                // The rename below must not publish bytes that are still buffered.
                fos.getFD().sync();
            }

            restrictPermissions(secretStoreFileTmp);
            publishAtomically(secretStoreFileTmp, secretStoreFile);

        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "unable to save secrets store " + secretStoreFileTmp + ": " + e, e);
            throw new DotRuntimeException(e);
        } finally {
            // The old code only deleted the tmp file on the happy path, leaking a copy of every
            // secret on any failure.
            final File tmpToClean = secretStoreFileTmp;
            if (null != tmpToClean) {
                Try.run(() -> Files.deleteIfExists(tmpToClean.toPath()));
            }
        }
        return keyStore;
    }

    /**
     * Publishes the store by renaming the tmp file over it, so a concurrent reader -- including one
     * on another cluster node sharing this file -- sees either the whole previous store or the whole
     * new one. Never a partial write, and never a missing file.
     *
     * Deliberately not {@code FileUtil.copyFile}, which this method replaced. That utility exists
     * for content versioning and either hard-links (deleting the destination first) or truncates the
     * destination and streams the bytes back in. Both leave the shared store empty or partial for a
     * window, which is defects B1 and B2 of issue #36724.
     */
    private static void publishAtomically(final File source, final File destination)
            throws IOException {
        try {
            Files.move(source.toPath(), destination.toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Logger.warn(SecretsKeyStoreHelper.class, String.format(
                    "The filesystem holding '%s' does not support atomic moves; falling back to a"
                            + " non-atomic replace. Concurrent readers on other nodes may observe a"
                            + " partial store on this filesystem.", destination), e);
            Files.move(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Restricts the file to owner read/write.
     *
     * Applied to the tmp file *before* it is moved into place, because the published store inherits
     * the tmp file's permissions. Previously both the tmp file and the store were created with
     * whatever the default umask allowed, in a directory shared across the cluster.
     */
    private static void restrictPermissions(final File file) {
        try {
            final Path path = file.toPath();
            if (path.getFileSystem().supportedFileAttributeViews().contains("posix")) {
                Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"));
            }
        } catch (Exception e) {
            Logger.warn(SecretsKeyStoreHelper.class,
                    "Unable to restrict permissions on the App secrets store: " + e.getMessage());
        }
    }

    /**
     * secret values are stored encryped in cache for 30 seconds and are decrypted when requested. If
     * value is not in cache, it will be read from the keystore
     * 
     * @param variableKey
     * @return
     * @throws Exception
     */
    public char[] getValue(final String variableKey) {
        final char[] fromStore = loadValueFromStore(variableKey);
        return fromStore == null ? CACHE_404 : fromStore;
    }

    /**
     * tries to load the value from the store on disk
     * 
     * @param variableKey
     * @return
     */
    private char[] loadValueFromStore(final String variableKey) {
        try {
           final KeyStore keyStore = getSecretsStore();
            if (keyStore.containsAlias(variableKey)) {
                final PasswordProtection keyStorePP = new PasswordProtection(passwordSupplier.get());
                final SecretKeyFactory factory = SecretKeyFactory.getInstance(SECRETS_STORE_SECRET_KEY_FACTORY_TYPE);
                final SecretKeyEntry secretKeyEntry = (SecretKeyEntry) keyStore.getEntry(variableKey, keyStorePP);
                return ((PBEKeySpec) factory.getKeySpec(secretKeyEntry.getSecretKey(), PBEKeySpec.class)).getPassword();
            } else {
                return null;
            }

        } catch (Exception e) {
            Logger.error(SecretsKeyStoreHelper.class,e);
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Use this to destroy the secrets repo
     * @return
     */
    
    public synchronized void destroy(){
        final File secretStoreFile = new File(secretsKeyStorePath);
        secretStoreFile.delete();
    }

    /**
     * Number of secrets stored
     * @return
     */
    public int size(){
       return Sneaky.sneaked(() -> getSecretsStore().size()).get();
    }


    /**
     * This method saves a secret value into the keystore. The value to be stored is encrypted using the
     * company key and is used as a "password" for a keystore entry - using it as a passowrd allows us
     * to store an arbitrary string in a keystore
     * 
     * @param variableKey
     * @param variableValue
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws KeyStoreException
     * @throws Exception
     */

    public char [] saveValue(final String variableKey, final char[] variableValue) {
        try {
            final SecretKeyFactory factory = SecretKeyFactory.getInstance(SECRETS_STORE_SECRET_KEY_FACTORY_TYPE);
            final KeyStore keyStore = getSecretsStore();
            final char [] encryptedVal = encrypt(variableValue);
            final SecretKey generatedSecret = factory.generateSecret(new PBEKeySpec(encryptedVal));
            final PasswordProtection keyStorePP = new PasswordProtection(passwordSupplier.get());
            keyStore.setEntry(variableKey, new KeyStore.SecretKeyEntry(generatedSecret), keyStorePP);
            saveSecretsStore(keyStore);
            return encryptedVal;
        } catch (Exception e) {
            Logger.warn(this.getClass(), "Unable to save secret from " + SECRETS_STORE_FILE + ": " + e);
            throw new DotRuntimeException(e);
        }
    }

    /**
     * deletes a value from the store.
     */

    void deleteValue(final String secretKey) {
        try {
            final KeyStore keyStore = getSecretsStore();
            keyStore.deleteEntry(secretKey);
            saveSecretsStore(keyStore);
        } catch (KeyStoreException  e) {
            Logger.warn(this.getClass(), "Unable to delete secret from  " + SECRETS_STORE_FILE + ": " + e);
            throw new DotRuntimeException(e);
        }
    }

    /**
     * While In memory, values remain encrypted. This is the key used for such purpose
     * @return
     */
    private Key key() {
        final String providerClassName = getCustomKeyProvider();
        if(UtilMethods.isSet(providerClassName)){
            try {
                @SuppressWarnings("unchecked")
                final SigningKeyFactory customKeyProvider = ((Class<SigningKeyFactory>) Class
                        .forName(providerClassName)).getDeclaredConstructor().newInstance();
                return customKeyProvider.getKey();
            } catch (Exception e) {
                Logger.error(this.getClass(), " Fail to get Security Key from Custom Key Provider Will fallback to default key provider. ", e);
            }
        }
        return Sneaky.sneak(() -> AppsKeyDefaultProvider.INSTANCE.get().getKey());
    }

    /**
     * brings the possibility to load a custom class to override the default Key provider thought an implementation of <code>SigningKeyFactory</code>
     * @return
     */
    private String getCustomKeyProvider() {
        return Config
                .getStringProperty(APPS_KEY_PROVIDER_CLASS, null);
    }

    /**
     * encryption function
     * @param val
     * @return
     */
    @VisibleForTesting
    char[] encrypt(final char[] val) {
        return Sneaky.sneak(() -> AppsUtil.encrypt(key(), val));
    }

    /**
     * decryption function
     * @param encryptedString
     * @return
     */
    @VisibleForTesting
    char[] decrypt(final String encryptedString) {
        if (encryptedString == null || encryptedString.length() == 0) {
            return null;
        }
        return Sneaky.sneak(() -> AppsUtil.decrypt(key(), encryptedString));
    }

    /**
     *
     * @param encryptedString
     * @return
     */
    @VisibleForTesting
    char[] decrypt(final char[] encryptedString) {
        if (encryptedString == null || encryptedString.length == 0) {
            return null;
        }
        return decrypt(new String(encryptedString));
    }

    /**
     * {@inheritDoc}
     */
    void backupAndRemoveKeyStore() throws IOException {
        final File secretStoreFile = new File(secretsKeyStorePath);
        if (!secretStoreFile.exists()) {
            Logger.warn(SecretsKeyStoreHelper.class, String.format("KeyStore file `%s` does NOT exist therefore it can not be backed-up. ",secretsKeyStorePath));
            return;
        }
        final FastDateFormat datetimeFormat = FastDateFormat.getInstance("yyyyMMddHHmmss");
        final String name = secretStoreFile.getName();
        final File secretStoreFileBak = new File(secretStoreFile.getParent(), datetimeFormat.format(new Date()) + "-" + name );
        Files.copy(secretStoreFile.toPath(), secretStoreFileBak.toPath());
        secretStoreFile.delete();

        Logger.info(SecretsKeyStoreHelper.class, ()->String.format("KeyStore `%s` has been removed a backup has been created.", secretsKeyStorePath));
    }

    /**
     * broad cast system-wide a notification in case of a keystore load exception
     * @throws DotDataException
     */
    private void sendFailureNotification()
            throws DotDataException {

        final NotificationAPI notificationAPI = APILocator.getNotificationAPI();
        // Search for the CMS Admin role and System User
        final Role cmsAdminRole = APILocator.getRoleAPI().loadCMSAdminRole();
        final User systemUser = APILocator.systemUser();

        notificationAPI.generateNotification(new I18NMessage("apps.fail.recover.secrets.title"),
                new I18NMessage("apps.fail.recover.secrets.notification", null), null, // no actions
                NotificationLevel.WARNING, NotificationType.GENERIC, Visibility.ROLE, cmsAdminRole.getId(), systemUser.getUserId(),
                systemUser.getLocale());
    }

    @FunctionalInterface
    public interface StoreCreatedListener {

        void onStoreCreated();

    }
}
