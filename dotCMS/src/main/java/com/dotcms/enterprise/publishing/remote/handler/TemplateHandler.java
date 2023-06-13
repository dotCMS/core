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
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.InodeUtils;
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

public class TemplateHandler implements IHandler {
	private UserAPI uAPI = APILocator.getUserAPI();
	private TemplateAPI tAPI = APILocator.getTemplateAPI();
	private List<String> infoToRemove = new ArrayList<String>();
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
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level)
	        throw new RuntimeException("need an enterprise pro license to run this");
		Collection<File> templates = FileUtil.listFilesRecursively(bundleFolder, new TemplateBundler().getFileFilter());

        handleTemplates(templates);
	}

	private void handleTemplates(Collection<File> templates) throws DotPublishingException, DotDataException {
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level)
	        throw new RuntimeException("need an enterprise pro license to run this");
		boolean unpublish = false;
	    User systemUser = uAPI.getSystemUser();
		try{
	        XStream xstream=new XStream(new DomDriver());
	        //Handle folders
	        for(File templateFile: templates) {
	        	if(templateFile.isDirectory()) continue;
	        	TemplateWrapper templateWrapper;
				try(final InputStream input = Files.newInputStream(templateFile.toPath())){
					templateWrapper = (TemplateWrapper) xstream.fromXML(input);
				}

	        	Template template = templateWrapper.getTemplate();

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
    	        	Host localHost = APILocator.getHostAPI().find(templateId.getHostId(), systemUser, false);
    	        	tAPI.saveTemplate(template, localHost, systemUser, false);

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
	        try{
	            for (String ident : infoToRemove) {
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
}
