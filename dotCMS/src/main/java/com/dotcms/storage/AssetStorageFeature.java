package com.dotcms.storage;

import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.storage.binary.BinaryAssetCleanupProcessor;
import com.dotcms.storage.binary.BinaryFieldCleanupProcessor;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

import java.util.Set;

/**
 * Startup configuration for the opt-in S3 asset lifecycle. Disabled preserves filesystem/NFS behavior.
 *
 * <p>The flag is read once per process and then kept. Several storage objects capture the mode when
 * they are built, so changing it on a running node (system table, properties reload) would leave that
 * node half-enabled. Set it through the environment or properties file and restart.</p>
 */
public final class AssetStorageFeature {
    public static final String FLAG = FeatureFlagName.FEATURE_FLAG_S3_ASSET_STORAGE;

    /**
     * Job queues that belong to the S3 lifecycle and must not register while it is disabled. The
     * queue names are compile-time constants, so reading the flag does not load the job processors.
     */
    private static final Set<String> JOB_QUEUES = Set.of(BinaryAssetCleanupProcessor.QUEUE,
            BinaryFieldCleanupProcessor.QUEUE);

    private static volatile Boolean enabled;

    static {
        // Registered before the first isEnabled() can latch, since that call loads this class.
        Config.onOverride(FLAG, AssetStorageFeature::reset);
    }

    private AssetStorageFeature() { }

    /**
     * Returns whether the S3 asset lifecycle is enabled for this process. The first call reads the
     * configuration; later calls return that value. The first read logs at INFO only when the flag
     * is on, so a node with the flag off writes no new INFO line compared with releases without
     * this feature.
     *
     * @return {@code true} when the feature flag was on at first read
     */
    public static boolean isEnabled() {
        Boolean value = enabled;
        if (value == null) {
            synchronized (AssetStorageFeature.class) {
                if (enabled == null) {
                    enabled = Config.getBooleanProperty(FLAG, false);
                    if (enabled) {
                        Logger.info(AssetStorageFeature.class, "S3 asset storage enabled");
                    } else {
                        Logger.debug(AssetStorageFeature.class, "S3 asset storage disabled");
                    }
                }
                value = enabled;
            }
        }
        return value;
    }

    /**
     * Tells the job queue whether a discovered processor may register for a queue. S3 lifecycle queues
     * are skipped while the feature is disabled, so they do not exist, as on a build without them.
     *
     * @param queueName the queue the discovered processor would register for
     * @return {@code false} only for an S3 lifecycle queue while the feature is disabled
     */
    public static boolean allowsJobQueue(final String queueName) {
        return isEnabled() || !JOB_QUEUES.contains(queueName);
    }

    /**
     * Forgets the value read at first use, so the next {@link #isEnabled()} reads the configuration
     * again. Registered with {@link Config#onOverride(String, Runnable)}, so in-memory overrides of the
     * flag made with {@link Config#setProperty(String, Object)}, which tests use to switch modes, are
     * re-read. Property reloads and the system table do not trigger it.
     */
    public static void reset() {
        enabled = null;
    }
}
