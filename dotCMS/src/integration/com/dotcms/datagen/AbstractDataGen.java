package com.dotcms.datagen;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;

public abstract class AbstractDataGen<T> implements DataGen<T> {
    protected static User user;
    protected static Host host;
    protected static Folder folder;

    public AbstractDataGen() {
        try {
            user = APILocator.getUserAPI().getSystemUser();
            host = APILocator.getHostAPI().findDefaultHost(user, false);
            folder = APILocator.getFolderAPI().findSystemFolder();
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException("Unable to get System User and/or Default Host", e);
        }
    }
}
