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
import com.dotcms.enterprise.publishing.remote.bundler.TemplateBundler;
import com.dotcms.enterprise.publishing.remote.handler.HandlerUtil.HandlerType;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.TemplateWrapper;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.rendering.velocity.services.TemplateLoader;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotcms.util.xstream.XStreamHandler;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.UserAPI;
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
	private UserAPI uAPI = APILocator.getUserAPI();
	private TemplateAPI tAPI = APILocator.getTemplateAPI();
	private List<String> infoToRemove = new ArrayList<>();
	private PublisherConfig config;

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
		Collection<File> templates = FileUtil.listFilesRecursively(bundleFolder, new TemplateBundler().getFileFilter());

        handleTemplates(templates);
	}

	private void handleTemplates(Collection<File> templates) throws DotPublishingException, DotDataException {
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level)
	        throw new RuntimeException("need an enterprise pro license to run this");
		boolean unpublish = false;
	    User systemUser = uAPI.getSystemUser();
	    File workingOn = null;
        Template template = null;
        try{
	        XStream xstream = XStreamHandler.newXStreamInstance();
	        //Handle folders
	        for(File templateFile: templates) {
	            workingOn = templateFile;
	        	if(templateFile.isDirectory()) continue;
	        	TemplateWrapper templateWrapper;
				try(final InputStream input = Files.newInputStream(templateFile.toPath())){
					templateWrapper = (TemplateWrapper) xstream.fromXML(input);
				}

	        	template = templateWrapper.getTemplate();
				if(template instanceof FileAssetTemplate){
					continue;
				}

	        	String modUser = template.getModUser();

    			if(templateWrapper.getOperation().equals(PushPublisherConfig.Operation.UNPUBLISH)) {
    				unpublish = true;
    				Template t = tAPI.find(template.getInode(), APILocator.getUserAPI().getSystemUser(), false);
    				if(t!=null && InodeUtils.isSet(t.getInode())){
    					String templateIden = t.getIdentifier();
    					tAPI.delete(t, APILocator.getUserAPI().getSystemUser(), false);

						PushPublishLogger.log(getClass(), PushPublishHandler.TEMPLATE, PushPublishAction.UNPUBLISH,
								templateIden, t.getInode(), t.getName(), config.getId());

    				}
    				continue;
    			}

    			Template existing = tAPI.find(template.getInode(), systemUser, false);
    			if(existing==null || !InodeUtils.isSet(existing.getIdentifier())) {
    	        	Identifier templateId = templateWrapper.getTemplateId();
					saveTemplate(templateId, template);

					PushPublishLogger.log(getClass(), PushPublishHandler.TEMPLATE, PushPublishAction.PUBLISH,
							template.getIdentifier(), template.getInode(), template.getName(), config.getId());

    	        	HandlerUtil.setModUser(template.getInode(), modUser, HandlerType.TEMPLATE);
    	        	CacheLocator.getTemplateCache().remove(template.getInode());

    	        	new TemplateLoader().invalidate(template);

    			}
	        }
	        if(!unpublish){
		        for (File templateFile : templates) {
		        	if(templateFile.isDirectory()) continue;
		        	TemplateWrapper templateWrapper;
					try(final InputStream input = Files.newInputStream(templateFile.toPath())){
						templateWrapper = (TemplateWrapper) xstream.fromXML(input);
					}
		        	VersionInfo info = templateWrapper.getVi();
		        	if(info.isLocked()){
		        		info.setLockedBy(systemUser.getUserId());
		        	}
	                infoToRemove.add(info.getIdentifier());
	                APILocator.getVersionableAPI().saveVersionInfo(info);
				}
	        }
	        String identifierToDelete = StringPool.BLANK;
	        try{
	            for (String ident : infoToRemove) {
                    identifierToDelete = ident;
	                APILocator.getVersionableAPI().removeVersionInfoFromCache(ident);
	            }
	        } catch (final Exception e) {
                throw new DotPublishingException(String.format("Unable to remove Template Version Info with ID '%s' " +
                        "from cache: %s", identifierToDelete, e.getMessage()), e);
            }
    	} catch (final Exception e) {
            final String errorMsg = String.format("An error occurred when processing Template in '%s' with ID '%s': %s",
                    workingOn, (null == template ? "(empty)" : template.getTitle()), (null == template ? "(empty)" :
                            template.getIdentifier()), e.getMessage());
            Logger.error(this.getClass(), errorMsg, e);
            throw new DotPublishingException(errorMsg, e);
    	}
    }

	private void saveTemplate(Identifier templateId, Template template) throws DotDataException, DotSecurityException {

		final User systemUser = APILocator.systemUser();
		final Host localHost = APILocator.getHostAPI().find(templateId.getHostId(), systemUser, false);

		final Template templateFromId
				= APILocator.getTemplateAPI().findWorkingTemplate(templateId.getId(), systemUser, false);

		if(templateFromId != null && InodeUtils.isSet(templateFromId.getIdentifier())) {
			final TemplateLayout newTemplateLayout = DotTemplateTool.getTemplateLayout(template.getDrawedBody());

			final TemplateSaveParameters templateSaveParameters = new TemplateSaveParameters
					.Builder()
					.setSite(localHost)
					.setNewTemplate(template)
					.setNewLayout(newTemplateLayout)
					.setUseHistory(true)
					.build();

			tAPI.saveAndUpdateLayout(templateSaveParameters, systemUser, false);
		} else {
			tAPI.saveTemplate(template, localHost, systemUser, false);
		}
	}
}
