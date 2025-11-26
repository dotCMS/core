package com.dotmarketing.portlets.containers.model;

import com.dotcms.publisher.util.PusheableAsset;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.util.Date;

/**
 * This class represents the default <b>System Container</b> in dotCMS.
 * <p>The idea behind this approach is to allow Users and Content Authors to add any sort of content to HTML Pages
 * without having to go through the usual configuration or setup process in both the Container and the Template that
 * references it. A pre-defined code template is used to render any kind of Content Type on the page, which can be
 * located in {@link com.dotmarketing.portlets.containers.business.ContainerAPIImpl#DEFAULT_CONTAINER_FILE_NAME}.</p>
 *
 * @author Jose Castro
 * @since Mar 30th, 2022
 */
public class SystemContainer extends Container {

    private static final String SYSTEM_CONTAINER_NAME = "System Container";
    private static final int DEFAULT_MAX_CONTENTS = Config.getIntProperty("systemcontainer.maxcontents", 25);

    /**
     * Creates an instance of the default <b>System Container</b>.
     */
    public SystemContainer() {
        final String userId = APILocator.systemUser().getUserId();
        super.setIdentifier(Container.SYSTEM_CONTAINER);
        super.setInode(Container.SYSTEM_CONTAINER);
        super.setOwner(userId);
        super.setModUser(userId);
        super.setModDate(new Date());
        super.setTitle(SYSTEM_CONTAINER_NAME);
        super.setFriendlyName(SYSTEM_CONTAINER_NAME);
        super.setNotes(SYSTEM_CONTAINER_NAME);
        super.setMaxContentlets(DEFAULT_MAX_CONTENTS);
    }

    // we override it, in order to do the permissionable behind a container object
    @Override
    public String getPermissionType() {
        return Container.class.getCanonicalName();
    }

    @Override
    public void setIdentifier(String identifier) {
        Logger.debug(this, () -> "System Container ID cannot be overridden.");
    }

    @Override
    public void setOwner(String owner) {
        Logger.debug(this, () -> "System Container owner cannot be overridden.");
    }

    @Override
    public void setModDate(Date modDate) {
        Logger.debug(this, () -> "System Container mod date cannot be overridden.");
    }

    @Override
    public void setModUser(String modUser) {
        Logger.debug(this, () -> "System Container mod user cannot be overridden.");
    }

    @Override
    public void setTitle(String title) {
        Logger.debug(this, () -> "System Container title cannot be overridden.");
    }

    @Override
    public void setFriendlyName(String friendlyName) {
        Logger.debug(this, () -> "System Container friendly name cannot be overridden.");
    }

    @Override
    public void setNotes(String notes) {
        Logger.debug(this, () -> "System Container notes cannot be overridden.");
    }

    @Override
    public void setInode(String inode) {
        Logger.debug(this, () -> "System Container inode cannot be overridden.");
    }

    @Override
    public void setMaxContentlets(int maxContentlets) {
        Logger.debug(this, () -> "System Container max contentlets cannot be overridden.");
    }

    @Override
    public void setCode(final String code) {
        if (!UtilMethods.isSet(super.getCode())) {
            super.setCode(code);
        }
    }

    @Override
    public ManifestInfo getManifestInfo() {
        return new ManifestInfoBuilder()
                .objectType(PusheableAsset.CONTAINER.getType())
                .id(this.getIdentifier())
                .inode(this.inode)
                .title(this.getTitle())
                .siteId(Host.SYSTEM_HOST)
                .build();
    }

    @Override
    public boolean isLive()  {
        return true;
    }

}
