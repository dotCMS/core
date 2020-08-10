package com.dotcms.system.event.local.business;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.publishing.listener.PushPublishKeyResetEventListener;
import com.dotcms.saml.DotSamlProxyFactory;
import com.dotcms.security.apps.AppSecretSavedEvent;
import com.dotcms.security.apps.AppsKeyResetEventListener;
import com.dotcms.system.event.local.type.security.CompanyKeyResetEvent;
import java.util.List;

import com.dotcms.config.DotInitializer;
import com.dotcms.content.elasticsearch.business.event.ContentletCheckinEvent;
import com.dotcms.graphql.listener.ContentTypeAndFieldsModsListeners;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.business.ApplicationContainerFolderListener;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.workflows.business.UnassignedWorkflowContentletCheckinListener;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

/**
 * Initializer class that allow us to register Local System Events subscribers
 *
 * @author Jonathan Gamba 7/28/17
 */
public class LocalSystemEventSubscribersInitializer implements DotInitializer {

    @Override
    public void init() {



        APILocator.getLocalSystemEventsAPI().subscribe(new ContentTypeAndFieldsModsListeners());

        this.initApplicationContainerFolderListener();

        APILocator.getLocalSystemEventsAPI().subscribe(ContentletCheckinEvent.class, UnassignedWorkflowContentletCheckinListener.getInstance());

        APILocator.getLocalSystemEventsAPI().subscribe(CompanyKeyResetEvent.class, PushPublishKeyResetEventListener.INSTANCE.get());

        APILocator.getLocalSystemEventsAPI().subscribe(CompanyKeyResetEvent.class, AppsKeyResetEventListener.INSTANCE.get());

        APILocator.getLocalSystemEventsAPI().subscribe(AppSecretSavedEvent.class,  DotSamlProxyFactory.getInstance());

    }

    public void initApplicationContainerFolderListener() {

        try {

            final User user  = APILocator.systemUser();
            final List<Host> hosts = APILocator.getHostAPI().findAllFromDB(user, false);
            final ApplicationContainerFolderListener listener = new ApplicationContainerFolderListener();
            for (final Host host : hosts) {

                final Folder appContainerFolder = APILocator.getFolderAPI().findFolderByPath(Constants.CONTAINER_FOLDER_PATH,
                        host, user, false);

                APILocator.getFolderAPI().subscribeFolderListener(appContainerFolder, listener,
                        childName -> null != childName && childName.endsWith(Constants.VELOCITY_FILE_EXTENSION));
            }
        } catch (DotDataException | DotSecurityException e) {

            Logger.error(this, "Could not init the: " +
                    ApplicationContainerFolderListener.class.getName() + ", msg: " + e.getMessage(), e);
        }
    }

}
