package com.dotcms.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.Theme;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;

/**
 * This Facade encapsulates most of the dot system apis
 * @author jsanca
 */
public interface SystemAPI {

    /**
     * Returns the system user
     * @return User
     */
    default User getSystemUser() {
        return APILocator.systemUser();
    }

    /**
     * Returns the system host
     * @return Host
     */
    default Host getSystemSite() {
        return APILocator.systemHost();
    }

    /**
     * Returns the system theme
     * @return Theme
     */
    default Theme getSystemTheme() {
        return APILocator.getThemeAPI().systemTheme();
    }

    /**
     * Returns the system template
     * @return
     */
    default Template getSystemTemplate() {
        return APILocator.getTemplateAPI().systemTemplate();
    }

    /**
     * Returns the system container
     * @return
     */
    default Container getSystemContainer() {
        return APILocator.getContainerAPI().systemContainer();
    }

    /**
     * Returns the system cache
     * @return
     */
    default SystemCache getSystemCache() {
        return CacheLocator.getSystemCache();
    }

    /**
     * Returns the system table
     * @return SystemTable
     */
    SystemTable getSystemTable();
}
