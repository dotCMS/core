package com.dotcms.system.event.local.business;

import com.dotcms.ai.listener.AIAppListener;
import com.dotcms.analytics.listener.AnalyticsAppListener;
import com.dotcms.config.DotInitializer;
import com.dotcms.content.elasticsearch.business.event.ContentletCheckinEvent;
import com.dotcms.graphql.listener.ContentTypeAndFieldsModsListeners;
import com.dotcms.publishing.listener.PushPublishKeyResetEventListener;
import com.dotcms.rendering.velocity.services.MacroCacheRefresherJob;
import com.dotcms.rest.api.v1.system.logger.ChangeLoggerLevelEvent;
import com.dotcms.security.apps.AppSecretSavedEvent;
import com.dotcms.security.apps.AppsKeyResetEventListener;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotcms.system.event.local.type.security.CompanyKeyResetEvent;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerStructureFinderStrategyResolver;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.ApplicationContainerFolderListener;
import com.dotmarketing.portlets.folders.business.ApplicationTemplateFolderListener;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.workflows.business.UnassignedWorkflowContentletCheckinListener;
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



        APILocator.getLocalSystemEventsAPI().subscribe(new ContentTypeAndFieldsModsListeners());
        APILocator.getLocalSystemEventsAPI().subscribe(ContainerStructureFinderStrategyResolver.getInstance());

        this.initApplicationContainerFolderListener();
        this.initApplicationTemplateFolderListener();

        APILocator.getLocalSystemEventsAPI().subscribe(ContentletCheckinEvent.class,  UnassignedWorkflowContentletCheckinListener.getInstance());

        APILocator.getLocalSystemEventsAPI().subscribe(CompanyKeyResetEvent.class,    PushPublishKeyResetEventListener.INSTANCE.get());

        APILocator.getLocalSystemEventsAPI().subscribe(CompanyKeyResetEvent.class,    AppsKeyResetEventListener.INSTANCE.get());

        APILocator.getLocalSystemEventsAPI().subscribe(ChangeLoggerLevelEvent.class, new EventSubscriber<ChangeLoggerLevelEvent>() {

            @Override
            public String getId() {
                return Logger.class.getName();
            }

            @Override
            public void notify(final ChangeLoggerLevelEvent event) {

                Logger.onChangeLoggerLevelEventHandler (event);
            }
        });

        APILocator.getLocalSystemEventsAPI().subscribe(APILocator.getTemplateAPI());
        APILocator.getLocalSystemEventsAPI().subscribe(APILocator.getContainerAPI());

        APILocator.getLocalSystemEventsAPI().subscribe(AppSecretSavedEvent.class, AnalyticsAppListener.Instance.get());
        APILocator.getLocalSystemEventsAPI().subscribe(AppSecretSavedEvent.class, AIAppListener.Instance.get());

        this.initDotVelocityMacrosVtlFiles();
    }

    private void initDotVelocityMacrosVtlFiles() {

        APILocator.getFileAssetAPI().subscribeFileListener(new MacroCacheRefresherJob(),
                "dot_velocity_macros.*"); // handles the dot_velocity_macros.vtl
    }

    public void initApplicationContainerFolderListener() {

        try {

            final User user  = APILocator.systemUser();
            final List<Host> hosts = APILocator.getHostAPI().findAllFromDB(user,
                    HostAPI.SearchType.INCLUDE_SYSTEM_HOST);
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

    public void initApplicationTemplateFolderListener() {

        try {

            final User user  = APILocator.systemUser();
            final List<Host> hosts = APILocator.getHostAPI().findAllFromDB(user,
                    HostAPI.SearchType.INCLUDE_SYSTEM_HOST);
            final ApplicationTemplateFolderListener listener = new ApplicationTemplateFolderListener();
            for (final Host host : hosts) {

                final Folder appTemplateFolder = APILocator.getFolderAPI().findFolderByPath(Constants.TEMPLATE_FOLDER_PATH,
                        host, user, false);

                APILocator.getFolderAPI().subscribeFolderListener(appTemplateFolder, listener,
                        childName -> null != childName && (childName.endsWith(Constants.VELOCITY_FILE_EXTENSION) || childName.endsWith(Constants.JSON_FILE_EXTENSION)));
            }
        } catch (DotDataException | DotSecurityException e) {

            Logger.error(this, "Could not init the: " +
                    ApplicationTemplateFolderListener.class.getName() + ", msg: " + e.getMessage(), e);
        }
    }

}
