package com.dotcms.storage;

import com.dotmarketing.util.Config;

/** Startup configuration for the opt-in S3 asset lifecycle. Disabled preserves filesystem/NFS behavior. */
public final class AssetStorageFeature {
    public static final String FLAG = "FEATURE_FLAG_S3_ASSET_STORAGE";

    private AssetStorageFeature() { }

    public static boolean isEnabled() {
        return Config.getBooleanProperty(FLAG, false);
    }
}
