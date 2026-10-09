package com.dotcms.storage;

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
    public static final String FLAG = "FEATURE_FLAG_S3_ASSET_STORAGE";

    /** Job processors that belong to the S3 lifecycle and must not register while it is disabled. */
    private static final Set<Class<?>> JOB_PROCESSORS = Set.of(BinaryAssetCleanupProcessor.class,
            BinaryFieldCleanupProcessor.class);

    private static volatile Boolean enabled;

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
     * Tells the job queue whether a discovered processor may register. S3 lifecycle processors are
     * skipped while the feature is disabled, so their queues do not exist, as on a build without them.
     *
     * @param processor the discovered job processor class
     * @return {@code false} only for an S3 lifecycle processor while the feature is disabled
     */
    public static boolean allowsJobProcessor(final Class<?> processor) {
        return isEnabled() || !JOB_PROCESSORS.contains(processor);
    }

    /**
     * Forgets the value read at first use, so the next {@link #isEnabled()} reads the configuration
     * again. Called by {@link Config#setProperty(String, Object)} for in-memory overrides of the flag,
     * which tests use to switch modes.
     */
    public static void reset() {
        enabled = null;
    }
}
