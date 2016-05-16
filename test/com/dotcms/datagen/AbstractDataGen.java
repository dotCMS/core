package com.dotcms.datagen;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.liferay.portal.model.User;

public abstract class AbstractDataGen<T> implements DataGen<T> {
    protected static final User user;
    protected static final Host defaultHost;
    private static final HostAPI hostAPI = APILocator.getHostAPI();

    static {
        try {
            user = APILocator.getUserAPI().getSystemUser();
            defaultHost = hostAPI.findDefaultHost(user, false);
        } catch (Exception e) {
            throw new RuntimeException("Unable to get System User and/or Default Host", e);
        }
    }
}
