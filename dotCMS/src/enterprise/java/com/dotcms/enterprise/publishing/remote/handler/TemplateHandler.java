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
import com.dotcms.enterprise.publishing.remote.bundler.TemplateBundler;
import com.dotcms.enterprise.publishing.remote.handler.HandlerUtil.HandlerType;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.TemplateWrapper;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.rendering.velocity.services.TemplateLoader;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotcms.util.EnterpriseFeature;
import com.dotcms.util.xstream.XStreamHandler;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.business.TemplateSaveParameters;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.FileAssetTemplate;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.PushPublishLogger.PushPublishAction;
import com.dotmarketing.util.PushPublishLogger.PushPublishHandler;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.liferay.util.StringPool;
import com.thoughtworks.xstream.XStream;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.dotmarketing.util.Constants.DONT_RESPECT_FRONT_END_ROLES;

/**
 * This handler class is part of the Push Publishing mechanism that deals with Template-related information inside a
 * bundle and saves it in the receiving instance. This class will read and process only the {@link Template} data
 * files.
 * <p>
 * Templates are layouts available to users when building new HTML, xHTML or XML pages. Each Template includes one or
 * more Containers, which act as server-side includes. The Containers placed in the Template define the areas on a Page
 * that permissioned users will be able to contribute content to, and how that content will be displayed.
 *
 * @author root
 * @version Mar 7, 2013
 */
public class TemplateHandler implements IHandler {

	private final TemplateAPI tAPI = APILocator.getTemplateAPI();
	private final List<String> infoToRemove = new ArrayList<>();
	private final PublisherConfig config;

	public TemplateHandler(PublisherConfig config) {
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
		final Collection<File> templates = FileUtil.listFilesRecursively(bundleFolder, new TemplateBundler().getFileFilter());
        handleTemplates(templates);
	}

	/**
	 * Handles the templates in the given collection. That is, it will read each template file and
	 * checks whether they need to be published, un-published, or deleted.
	 *
	 * @param templates The collection of template files to handle.
	 *
	 * @throws DotPublishingException An error occurred while processing the templates.
	 */
	@EnterpriseFeature(licenseLevel = LicenseLevel.PROFESSIONAL)
	private void handleTemplates(final Collection<File> templates) throws DotPublishingException {
		boolean unpublish = false;
	    final User systemUser = APILocator.systemUser();
	    File workingOn = null;
        Template template = null;
        try{
	        final XStream xstream = XStreamHandler.newXStreamInstance();
	        for (final File templateFile: templates) {
	            workingOn = templateFile;
				if (templateFile.isDirectory()) {
					continue;
				}
	        	TemplateWrapper templateWrapper;
				try(final InputStream input = Files.newInputStream(templateFile.toPath())){
					templateWrapper = (TemplateWrapper) xstream.fromXML(input);
				}

	        	template = templateWrapper.getTemplate();
				if(template instanceof FileAssetTemplate){
					continue;
				}
	        	final String modUser = template.getModUser();
    			if(templateWrapper.getOperation().equals(PushPublisherConfig.Operation.UNPUBLISH)) {
    				unpublish = true;
    				final Template templateToUnpublish = tAPI.find(template.getInode(), APILocator.getUserAPI().getSystemUser(), DONT_RESPECT_FRONT_END_ROLES);
					if (templateToUnpublish != null && InodeUtils.isSet(templateToUnpublish.getInode())) {
    					final String templateId = templateToUnpublish.getIdentifier();
						tAPI.deleteTemplate(templateToUnpublish, APILocator.getUserAPI().getSystemUser(), DONT_RESPECT_FRONT_END_ROLES);
						PushPublishLogger.log(getClass(), PushPublishHandler.TEMPLATE, PushPublishAction.UNPUBLISH,
								templateId, templateToUnpublish.getInode(), templateToUnpublish.getName(), config.getId());
    				}
    				continue;
    			}
				// If the Inode of the pushed Template equals an existing one, no data will be overwritten
    			final Template existing = tAPI.find(template.getInode(), systemUser, DONT_RESPECT_FRONT_END_ROLES);
    			if(existing==null || !InodeUtils.isSet(existing.getIdentifier())) {
    	        	final Identifier templateId = templateWrapper.getTemplateId();
					saveTemplate(templateId, template);

					PushPublishLogger.log(getClass(), PushPublishHandler.TEMPLATE, PushPublishAction.PUBLISH,
							template.getIdentifier(), template.getInode(), template.getName(), config.getId());

    	        	HandlerUtil.setModUser(template.getInode(), modUser, HandlerType.TEMPLATE);
    	        	CacheLocator.getTemplateCache().remove(template.getInode());

    	        	new TemplateLoader().invalidate(template);
    			}
	        }
	        if(!unpublish){
		        for (final File templateFile : templates) {
					if (templateFile.isDirectory()) {
						continue;
					}
		        	TemplateWrapper templateWrapper;
					try(final InputStream input = Files.newInputStream(templateFile.toPath())){
						templateWrapper = (TemplateWrapper) xstream.fromXML(input);
					}
		        	final VersionInfo info = templateWrapper.getVi();
		        	if(info.isLocked()){
		        		info.setLockedBy(systemUser.getUserId());
		        	}
	                infoToRemove.add(info.getIdentifier());
	                APILocator.getVersionableAPI().saveVersionInfo(info);
				}
	        }
	        String identifierToDelete = StringPool.BLANK;
	        try{
	            for (final String ident : infoToRemove) {
                    identifierToDelete = ident;
	                APILocator.getVersionableAPI().removeVersionInfoFromCache(ident);
	            }
	        } catch (final Exception e) {
                throw new DotPublishingException(String.format("Unable to remove Template Version Info with ID '%s' " +
                        "from cache: %s", identifierToDelete, ExceptionUtil.getErrorMessage(e)), e);
            }
    	} catch (final Exception e) {
            final String errorMsg = String.format("An error occurred when processing Template in '%s' with title '%s' [%s]: %s",
                    workingOn, (null == template ? "(empty)" : template.getTitle()), (null == template ? "(empty)" :
                            template.getIdentifier()), ExceptionUtil.getErrorMessage(e));
            Logger.error(this.getClass(), errorMsg, e);
            throw new DotPublishingException(errorMsg, e);
    	}
    }

	/**
	 * Saves the given template to the system. If the template already exists, it updates it.
	 *
	 * @param templateId The {@link Identifier} of the template.
	 * @param template   The {@link Template} to save.
	 *
	 * @throws DotDataException     An error occurred while interacting with the database.
	 * @throws DotSecurityException An error occurred while checking user permissions.
	 */
	private void saveTemplate(final Identifier templateId, final Template template) throws DotDataException, DotSecurityException {
		final User systemUser = APILocator.systemUser();
		final Host templateSite = APILocator.getHostAPI().find(templateId.getHostId(), systemUser, DONT_RESPECT_FRONT_END_ROLES);
		final Template templateFromId = tAPI.findWorkingTemplate(templateId.getId(), systemUser, DONT_RESPECT_FRONT_END_ROLES);

		if (templateFromId != null && InodeUtils.isSet(templateFromId.getIdentifier()) && template.isDrawed()) {
			final TemplateLayout newTemplateLayout = DotTemplateTool.getTemplateLayout(template.getDrawedBody());

			final TemplateSaveParameters templateSaveParameters = new TemplateSaveParameters
					.Builder()
					.setSite(templateSite)
					.setNewTemplate(template)
					.setNewLayout(newTemplateLayout)
					.setUseHistory(true)
					.build();
			tAPI.saveAndUpdateLayout(templateSaveParameters, systemUser, DONT_RESPECT_FRONT_END_ROLES);
		} else {
			tAPI.saveTemplate(template, templateSite, systemUser, DONT_RESPECT_FRONT_END_ROLES);
		}
	}

}
