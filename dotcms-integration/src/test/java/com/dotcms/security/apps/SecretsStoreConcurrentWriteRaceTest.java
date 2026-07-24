package com.dotcms.security.apps;

import static org.junit.Assert.assertFalse;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.Config;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Reproduction harness for issue #35592 — "App configurations silently wiped out
 * (dotSecretsStore.p12 integrity failure -&gt; empty store regenerated)".
 *
 * <p>In a multi-node cluster every node reads and writes the SAME
 * {@code dotSecretsStore.p12} living on shared assets storage, with no lock. When two nodes
 * touch it concurrently:
 * <ul>
 *   <li>{@code SecretsKeyStoreHelper.saveSecretsStore()} writes a tmp file then
 *       {@code FileUtil.copyFile(tmp, dest)}. On shared network storage (NFS/EFS) hard links
 *       are unavailable, so {@code copyFile} truncates the destination to zero and streams the
 *       bytes back in — a reader on another node can observe a torn / empty PKCS12.</li>
 *   <li>A torn read throws {@code Failed PKCS12 integrity checking}, which
 *       {@code getSecretsStore()} "recovers" from by backing up, deleting, and regenerating an
 *       EMPTY store — silently wiping every previously stored App secret.</li>
 * </ul>
 *
 * <p>This test seeds a canary secret and then hammers the shared store with concurrent writes
 * and reads. It asserts the canary can never disappear. That assertion FAILS on {@code master},
 * which is precisely the reproduction of #35592 — it is a bug harness, not a passing regression
 * test; it flips to green only once a fix (locking / atomic write / no wipe-on-transient-error)
 * lands.
 *
 * <p>Everything is isolated to a temp file via {@code SECRETS_KEYSTORE_FILE_PATH_KEY}, so the
 * real store is never touched, and both Config overrides are restored in {@code finally}.
 */
public class SecretsStoreConcurrentWriteRaceTest {

    private static final String SECRETS_KEYSTORE_FILE_PATH_KEY = "SECRETS_KEYSTORE_FILE_PATH_KEY";
    private static final String CONTENT_VERSION_HARD_LINK = "CONTENT_VERSION_HARD_LINK";

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void concurrent_writes_silently_wipe_previously_stored_secret() throws Exception {

        final Path tmpDir = Files.createTempDirectory("secrets-race-35592");
        final String storePath = tmpDir.resolve("dotSecretsStore.p12").toString();
        final String priorPath = Config.getStringProperty(SECRETS_KEYSTORE_FILE_PATH_KEY, null);
        final boolean priorHardLink = Config.getBooleanProperty(CONTENT_VERSION_HARD_LINK, true);

        // Point the store at an isolated temp file, and simulate shared network storage
        // (NFS/EFS) where hard links are unavailable -> FileUtil.copyFile truncates + streams.
        Config.setProperty(SECRETS_KEYSTORE_FILE_PATH_KEY, storePath);
        Config.setProperty(CONTENT_VERSION_HARD_LINK, false);

        final ExecutorService pool = Executors.newFixedThreadPool(3);
        try {
            // A fixed password shared by both "nodes" (deterministic, like a shared cluster_salt).
            final Supplier<char[]> pwd = () -> "shared-cluster-salt-digest".toCharArray();
            final SecretsKeyStoreHelper nodeA = new SecretsKeyStoreHelper(pwd, ImmutableList.of());
            final SecretsKeyStoreHelper nodeB = new SecretsKeyStoreHelper(pwd, ImmutableList.of());

            final String canaryKey = "canary-app-config";
            final char[] canaryVal = "must-survive".toCharArray();
            nodeA.saveValue(canaryKey, canaryVal);

            final int iterations = 800;
            final AtomicReference<String> lostReason = new AtomicReference<>(null);

            // Writer "node": continuously rewrites the whole shared store with new keys.
            final Future<?> writer = pool.submit(() -> {
                for (int i = 0; i < iterations; i++) {
                    nodeA.saveValue("k-" + i, ("v-" + i).toCharArray());
                }
            });

            // Two reader "nodes" checking the canary is still present on every read.
            final Runnable readerTask = () -> {
                for (int i = 0; i < iterations && lostReason.get() == null; i++) {
                    try {
                        final char[] value = nodeB.getValue(canaryKey);
                        if (Arrays.equals(value, AppsCache.CACHE_404)
                                || !Arrays.equals(value, canaryVal)) {
                            lostReason.compareAndSet(null,
                                    "canary secret disappeared (store wiped / regenerated empty)");
                        }
                    } catch (Exception e) {
                        lostReason.compareAndSet(null,
                                "read threw during concurrent write: " + e);
                    }
                }
            };
            final Future<?> reader1 = pool.submit(readerTask);
            final Future<?> reader2 = pool.submit(readerTask);

            writer.get(60, TimeUnit.SECONDS);
            reader1.get(60, TimeUnit.SECONDS);
            reader2.get(60, TimeUnit.SECONDS);

            // On master this FAILS -> reproduces #35592 (a durably-stored secret was silently lost).
            assertFalse("Repro #35592 — App secret was silently wiped by a concurrent multi-node "
                            + "write: " + lostReason.get(),
                    lostReason.get() != null);
        } finally {
            pool.shutdownNow();
            Config.setProperty(CONTENT_VERSION_HARD_LINK, priorHardLink);
            Config.setProperty(SECRETS_KEYSTORE_FILE_PATH_KEY, priorPath == null ? "" : priorPath);
            try (var paths = Files.walk(tmpDir)) {
                paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
    }
}
