package com.dotcms.content.index;

import com.dotcms.content.index.domain.SearchHit;
import com.dotcms.content.index.domain.SearchHits;
import com.dotcms.content.index.domain.TotalHits;
import com.dotcms.featureflag.FeatureFlagName;
import com.dotmarketing.util.Config;

public interface IndexSourceHelper {

    static boolean isOpenSearchReadEnabled() {
        return Config.getBooleanProperty(FeatureFlagName.FEATURE_FLAG_OPEN_SEARCH_READ, false);
    }

    static boolean isOpenSearchWriteEnabled() {
        return Config.getBooleanProperty(FeatureFlagName.FEATURE_FLAG_OPEN_SEARCH_WRITE, false);
    }

    static boolean isOpenSearchEnabled() {
        return isOpenSearchReadEnabled() || isOpenSearchWriteEnabled();
    }

}
