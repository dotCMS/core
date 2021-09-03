package com.dotcms.publishing.manifest;

import com.dotcms.publishing.manifest.ManifestItem.ManifestInfo;
import java.util.Collection;

public interface ManifestReader {

    Collection<ManifestInfo> getIncludedAssets();
    Collection<ManifestInfo> getExcludedAssets();
    Collection<ManifestInfo> getAssets(final ManifestReason manifestReason);
    Collection<ManifestInfo> getAssets();
}
