package com.dotcms.datagen;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.portal.model.User;

public abstract class AbstractDataGen<T> implements DataGen<T> {
    protected static User user;
    protected Host host;
    protected Folder folder;
    protected Language language;

    static{
        try {
            user = APILocator.getUserAPI().getSystemUser();
        } catch (DotDataException e) {
            throw new RuntimeException("Unable to get System User and/or Default Host", e);
        }
    }

    public AbstractDataGen() {
        try {

            host = APILocator.getHostAPI().findDefaultHost(user, false);
            folder = APILocator.getFolderAPI().findSystemFolder();
            language = APILocator.getLanguageAPI().getDefaultLanguage();
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException("Unable to get System User and/or Default Host", e);
        }
    }
}
