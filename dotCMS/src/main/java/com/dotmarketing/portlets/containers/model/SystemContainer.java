package com.dotmarketing.portlets.containers.model;

import com.dotmarketing.business.APILocator;

import java.util.Date;

/**
 *
 * @author Jose Castro
 * @version 1.0
 * @since Mar 30th, 2022
 */
public class SystemContainer extends Container {

    private static final String SYSTEM_CONTAINER_NAME = "System Container";
    private static final int DEFAULT_MAX_CONTENTS = 25;

    public SystemContainer() {
        final String userId = APILocator.systemUser().getUserId();
        super.setIdentifier(Container.SYSTEM_CONTAINER);
        super.setInode(Container.SYSTEM_CONTAINER);
        super.setOwner(userId);
        super.setModUser(userId);
        super.setModDate(new Date());
        super.setTitle(SYSTEM_CONTAINER_NAME);
        super.setFriendlyName(SYSTEM_CONTAINER_NAME);
        super.setMaxContentlets(DEFAULT_MAX_CONTENTS);
    }

}
