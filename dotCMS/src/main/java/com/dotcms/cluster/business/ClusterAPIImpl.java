package com.dotcms.cluster.business;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;

public class ClusterAPIImpl implements ClusterAPI {

    private final ServerAPI serverAPI;

    public ClusterAPIImpl() {
        this(APILocator.getServerAPI());
    }

    public ClusterAPIImpl(final ServerAPI serverAPI) {
        this.serverAPI = serverAPI;
    }

    @Override
    public boolean isTransportAutoWire() {
        return Config.getBooleanProperty("AUTOWIRE_CLUSTER_TRANSPORT", true)
            && LicenseUtil.getLevel()> LicenseLevel.STANDARD.level;
    }
}
