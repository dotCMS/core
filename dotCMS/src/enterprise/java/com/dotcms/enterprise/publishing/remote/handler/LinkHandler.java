/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.remote.handler;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.remote.bundler.LinkBundler;
import com.dotcms.enterprise.publishing.remote.handler.HandlerUtil.HandlerType;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.LinkWrapper;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.util.xstream.XStreamHandler;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.factories.LinkFactory;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.PushPublishLogger.PushPublishAction;
import com.dotmarketing.util.PushPublishLogger.PushPublishHandler;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This handler class is part of the Push Publishing mechanism that deals with Link-related information inside a bundle
 * and saves it in the receiving instance. This class will read and process only the {@link Link} data files.
 * <p>
 * Menu Links are used to include HTML pages and documentation in navigation menus, even when those pages normally would
 * not be displayed because they exist outside the Navigation parameters being used by the template or HTML page.
 *
 * @author root
 * @since Mar 7, 2013
 */
public class LinkHandler implements IHandler {

	private final UserAPI uAPI = APILocator.getUserAPI();
	private final List<String> infoToRemove = new ArrayList<>();
	private final PublisherConfig config;

	public LinkHandler(PublisherConfig config) {
		this.config = config;
	}

	@Override
	public String getName() {
		return this.getClass().getName();
	}

	@Override
	public void handle(File bundleFolder) throws Exception {
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
            throw new RuntimeException("need an enterprise pro license to run this");
        }
		Collection<File> templates = FileUtil.listFilesRecursively(bundleFolder, new LinkBundler().getFileFilter());

		handleLinks(templates);
	}

	private void deleteLink(Link link) {
		try {
			Link l = APILocator.getMenuLinkAPI().find(link.getInode(), APILocator.getUserAPI().getSystemUser(), false);
			if(l!=null && InodeUtils.isSet(l.getInode())){
				APILocator.getMenuLinkAPI().delete(link, APILocator.getUserAPI().getSystemUser(), false);
			}
		} catch (final Exception e) {
            Logger.error(this, String.format("An error occurred when deleting Link '%s' [%s]: %s",
					(null == link ? "(null)" : link.getTitle()), (null == link ? "(null)" : link.getIdentifier()), ExceptionUtil.getErrorMessage(e)), e);
        }
	}

	private void handleLinks(Collection<File> links) throws DotPublishingException, DotDataException {
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
            throw new RuntimeException("need an enterprise pro license to run this");
        }
		boolean unpublish = false;
	    User systemUser = uAPI.getSystemUser();
	    File workingOn = null;
	    Link linkToPublish = null;
		try{
	        XStream xstream = XStreamHandler.newXStreamInstance();

	        for(File linkFile: links) {
	            workingOn = linkFile;
	        	if(linkFile.isDirectory()) continue;
                LinkWrapper linkWrapper;
	        	try (final InputStream input = Files.newInputStream(linkFile.toPath())){
                     linkWrapper = (LinkWrapper)  xstream.fromXML(input);
                }

	        	for(Link link : linkWrapper.getLinks()){
                    linkToPublish = link;
	    			if(linkWrapper.getOperation().equals(PushPublisherConfig.Operation.UNPUBLISH)) {
	    				unpublish = true;
	    				String linkIden = link.getIdentifier();
	    				deleteLink(link);
						PushPublishLogger.log(getClass(), PushPublishHandler.LINK, PushPublishAction.UNPUBLISH,
								linkIden, link.getInode(), link.getName(), config.getId());
	    				continue;
	    			}

	    			Link existing = null;
	    			Identifier existingId = null;
	    			Identifier linkId = linkWrapper.getLinkId();
	    			Host localHost = APILocator.getHostAPI().find(linkId.getHostId(), systemUser, false);
	    			String modUser = link.getModUser();

	    			try{
	    				existing = APILocator.getMenuLinkAPI().find(link.getInode(), systemUser, false);
	    				existingId = APILocator.getIdentifierAPI().find(existing.getIdentifier());
	    			}catch (DotSecurityException | DotDataException e) {
	    				Logger.debug(getClass(), "Could not find existing Link or Identifier");
					}
	    			if(existing==null || !InodeUtils.isSet(existing.getIdentifier())) {
    		        	Host h = APILocator.getHostAPI().find(link.getHostId(), systemUser, false);
    		        	Folder destination = APILocator.getFolderAPI().findFolderByPath(link.getParent(), h, systemUser, false);
    		        	APILocator.getMenuLinkAPI().save(link, destination, systemUser, false);
						PushPublishLogger.log(getClass(), PushPublishHandler.LINK, PushPublishAction.PUBLISH,
								link.getIdentifier(), link.getInode(), link.getName(), config.getId());
	    			} else if(!linkId.getParentPath().equals(existingId.getParentPath())) {
	                	// if was moved to HOST
	    				if(linkId.getParentPath().equals("/")) {
	    					LinkFactory.moveLink( existing, localHost );
	    				} else { // if was moved to another FOLDER
	    					Folder newParentFolder = APILocator.getFolderAPI().findFolderByPath(linkId.getParentPath(), localHost, systemUser, false);
	    					LinkFactory.moveLink( existing, newParentFolder );
	    				}
	                }

	    			HandlerUtil.setModUser(link.getInode(), modUser, HandlerType.LINKS);
		        }
	        }
	        if(!unpublish){
		        for (File linkFile : links) {
                    workingOn = linkFile;
		        	if(linkFile.isDirectory()) continue;
                    LinkWrapper linkWrapper;
                    try(final InputStream input = Files.newInputStream(linkFile.toPath())){
                        linkWrapper = (LinkWrapper) xstream.fromXML(input);
                    }
                    linkToPublish = UtilMethods.isSet(linkWrapper.getLinks()) ? linkWrapper.getLinks().get(0) : null;
		        	VersionInfo info = linkWrapper.getVi();
		        	if(info.isLocked() && info.getLockedBy() != null){
		        		info.setLockedBy(systemUser.getUserId());
		        	}

	                infoToRemove.add(info.getIdentifier());
	                APILocator.getVersionableAPI().saveVersionInfo(info);
				}
	        }
	        String identToRemove = null;
	        try{
	            for (String ident : infoToRemove) {
	                identToRemove = ident;
	                APILocator.getVersionableAPI().removeVersionInfoFromCache(ident);
	            }
	        } catch (final Exception e) {
                throw new DotPublishingException(String.format("An error occurred when removing Version Info with ID " +
                        "'%s' from cache: %s", identToRemove, ExceptionUtil.getErrorMessage(e)), e);
            }
    	} catch (final Exception e) {
            final String errorMsg = String.format("An error occurred when processing Link in '%s' with title '%s' [%s]: %s",
                    workingOn, (null == linkToPublish ? "(empty)" : linkToPublish.getTitle()), (null == linkToPublish
                            ? "(empty)" : linkToPublish.getIdentifier()), ExceptionUtil.getErrorMessage(e));
            Logger.error(this.getClass(), errorMsg, e);
            throw new DotPublishingException(errorMsg, e);
    	}
    }

}
