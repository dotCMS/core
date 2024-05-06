/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/

package com.dotcms.enterprise.publishing.remote.handler;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.remote.bundler.LinkBundler;
import com.dotcms.enterprise.publishing.remote.handler.HandlerUtil.HandlerType;
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
	private UserAPI uAPI = APILocator.getUserAPI();
	private List<String> infoToRemove = new ArrayList<>();
	private PublisherConfig config;

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

	private void deleteLink(Link link) throws DotPublishingException, DotDataException{
		try {
			Link l = APILocator.getMenuLinkAPI().find(link.getInode(), APILocator.getUserAPI().getSystemUser(), false);
			if(l!=null && InodeUtils.isSet(l.getInode())){
				APILocator.getMenuLinkAPI().delete(link, APILocator.getUserAPI().getSystemUser(), false);
			}
		} catch (final Exception e) {
            Logger.error(this, String.format("An error occurred when deleting Link '%s' [%s]: %s", (null == link ? "" +
                    "(null)" : link.getTitle()), (null == link ? "(null)" : link.getIdentifier()), e.getMessage()), e);
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
                        "'%s' from cache: %s", identToRemove, e.getMessage()), e);
            }
    	} catch (final Exception e) {
            final String errorMsg = String.format("An error occurred when processing Link in '%s' with ID '%s': %s",
                    workingOn, (null == linkToPublish ? "(empty)" : linkToPublish.getTitle()), (null == linkToPublish
                            ? "(empty)" : linkToPublish.getIdentifier()), e.getMessage());
            Logger.error(this.getClass(), errorMsg, e);
            throw new DotPublishingException(errorMsg, e);
    	}
    }
}
