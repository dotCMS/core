package com.dotcms.enterprise.publishing.remote.handler;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.remote.bundler.ContainerBundler;
import com.dotcms.enterprise.publishing.remote.handler.HandlerUtil.HandlerType;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.ContainerWrapper;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.rendering.velocity.services.ContainerLoader;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.PushPublishLogger.PushPublishAction;
import com.dotmarketing.util.PushPublishLogger.PushPublishHandler;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ContainerHandler implements IHandler {

	private final UserAPI 			userAPI = APILocator.getUserAPI();
	private final ContainerAPI 		containerAPI = APILocator.getContainerAPI();
	private final List<String> 		infoToRemove = new ArrayList<String>();
	private final PublisherConfig 	config;

	public ContainerHandler(final PublisherConfig config) {
		this.config = config;
	}

	@Override
	public String getName() {
		return this.getClass().getName();
	}

	private void deleteContainer(final Container container) throws DotPublishingException, DotDataException{
		try {
			final Container workingContainer =
					APILocator.getContainerAPI().getWorkingContainerById
							(container.getIdentifier(), APILocator.getUserAPI().getSystemUser(), false);

			if (workingContainer!=null && InodeUtils.isSet(workingContainer.getInode())) {

				APILocator.getContainerAPI().delete(workingContainer, APILocator.getUserAPI().getSystemUser(), false);
			}
		} catch (Exception e) {
			Logger.error(ContainerHandler.class,e.getMessage(),e);
		}
	}


	@Override
	public void handle(final File bundleFolder) throws Exception {

	    if (LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {

			throw new RuntimeException("need an enterprise pro license to run this");
		}

		final Collection<File> containers = FileUtil.listFilesRecursively(bundleFolder, new ContainerBundler().getFileFilter());
        handleContainers(containers);
	}

	private void handleContainers(final Collection<File> containers) throws DotPublishingException, DotDataException {

	    if (LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {

			throw new RuntimeException("need an enterprise pro license to run this");
		}

	    boolean unpublish = false;
	    User systemUser   = userAPI.getSystemUser();
		try {
	        XStream xstream=new XStream(new DomDriver());
	        //Handle folders
	        for(final File containerFile: containers) {
	        	if(containerFile.isDirectory()) continue;

				ContainerWrapper containerWrapper;
	        	try(final InputStream input = Files.newInputStream(containerFile.toPath())){
					containerWrapper = (ContainerWrapper)  xstream.fromXML(input);
				}

	        	Container container = containerWrapper.getContainer();
	        	String modUser = container.getModUser();
	        	Identifier containerId = containerWrapper.getContainerId();

	        	unpublish = containerWrapper.getOperation().equals(Operation.UNPUBLISH);

        		Host localHost = APILocator.getHostAPI().find(containerId.getHostId(), systemUser, false);

    			if(containerWrapper.getOperation().equals(PushPublisherConfig.Operation.UNPUBLISH)) {
    				String containerIden = container.getIdentifier();
    				deleteContainer(container);

					PushPublishLogger.log(getClass(), PushPublishHandler.CONTAINER, PushPublishAction.UNPUBLISH,
							containerIden, container.getInode(), container.getName(), config.getId());
    			}
    			else {
    			    // save if it doesn't exists
    			    final Container existing = existsContainer(container);
					if (existing==null || !InodeUtils.isSet(existing.getIdentifier())) {
        			    containerAPI.save(container,
        					containerWrapper.getCsList(),
        					localHost, systemUser, false);
						PushPublishLogger.log(getClass(), PushPublishHandler.CONTAINER, PushPublishAction.PUBLISH_CREATE,
								container.getIdentifier(), container.getInode(), container.getName(), config.getId());
    			    } else {
    			    	containerAPI.save(existing,
            					containerWrapper.getCsList(),
            					localHost, systemUser, false);
						PushPublishLogger.log(getClass(), PushPublishHandler.CONTAINER, PushPublishAction.PUBLISH_UPDATE,
								container.getIdentifier(), container.getInode(), container.getName(), config.getId());
    			    }
    			}

    			HandlerUtil.setModUser(container.getInode(), modUser, HandlerType.CONTAINERS);
    			CacheLocator.getContainerCache().remove(container);
    	            new ContainerLoader().invalidate(container);


	        }

	        if(!unpublish){
		        for(final File containerFile: containers) {
		        	if(containerFile.isDirectory()) continue;

                    ContainerWrapper containerWrapper;
		        	try(final InputStream input = Files.newInputStream(containerFile.toPath())){
                        containerWrapper = (ContainerWrapper) xstream.fromXML(input);
                    }


		        	final VersionInfo info = containerWrapper.getCvi();
		        	if (info.isLocked() && info.getLockedBy() != null) {
		        		info.setLockedBy(systemUser.getUserId());
		        	}

	                infoToRemove.add(info.getIdentifier());
	                APILocator.getVersionableAPI().saveVersionInfo(info);
		        }
	        }

	        try{
	            for (final String ident : infoToRemove) {
	                APILocator.getVersionableAPI().removeVersionInfoFromCache(ident);
	            }
	        }catch (Exception e) {
	            throw new DotPublishingException("Unable to remove from cache version info", e);
	        }
    	}
    	catch(Exception e){
    		throw new DotPublishingException(e.getMessage(),e);
    	}
    }


	@CloseDBIfOpened
	private Container existsContainer(Container container) {
		Container existing;
		try {
            existing=(Container) HibernateUtil.load(Container.class, container.getInode());
        }
        catch(Exception ex) {
            existing=null;
        }
		return existing;
	}

}
