package com.dotcms.system.event.local.business;

import com.dotcms.config.DotInitializer;
import com.dotcms.services.VanityUrlServices;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.business.ApplicationContainerFolderListener;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import java.util.List;

/**
 * Initializer class that allow us to register Local System Events subscribers
 *
 * @author Jonathan Gamba 7/28/17
 */
public class LocalSystemEventSubscribersInitializer implements DotInitializer {

    @Override
    public void init() {

        APILocator.getLocalSystemEventsAPI().subscribe(VanityUrlServices.getInstance());
        this.initApplicationContainerFolderListener();
    }

    public void initApplicationContainerFolderListener() {

        try {

            final User user  = APILocator.systemUser();
            final List<Host> hosts = APILocator.getHostAPI().findAll(user, false);
            for (final Host host : hosts) {

                final Folder appContainerFolder = APILocator.getFolderAPI().findFolderByPath(Constants.CONTAINER_FOLDER_PATH,
                        host, user, false);

                APILocator.getFolderAPI().subscribeFolderListener(appContainerFolder, new ApplicationContainerFolderListener(),
                        childName -> childName.endsWith(Constants.VELOCITY_FILE_EXTENSION));
            }
        } catch (DotDataException | DotSecurityException e) {

            Logger.error(this, "Could not init the: " +
                    ApplicationContainerFolderListener.class.getName() + ", msg: " + e.getMessage(), e);
        }
    }

}