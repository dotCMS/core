package com.dotcms.publisher.receiver.handler;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.dotcms.publisher.myTest.bundler.TemplateBundler;
import com.dotcms.publisher.myTest.wrapper.TemplateWrapper;
import com.dotcms.publishing.DotPublishingException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class TemplateHandler implements IHandler {
	private IdentifierAPI iAPI = APILocator.getIdentifierAPI();
	private UserAPI uAPI = APILocator.getUserAPI();
	private TemplateAPI tAPI = APILocator.getTemplateAPI();
	private List<String> infoToRemove = new ArrayList<String>();

	@Override
	public String getName() {
		return this.getClass().getName();
	}
	
	@Override
	public void handle(File bundleFolder) throws Exception {
		Collection<File> templates = FileUtil.listFilesRecursively(bundleFolder, new TemplateBundler().getFileFilter());
		
        handleTemplates(templates);
	}
	
	private void handleTemplates(Collection<File> templates) throws DotPublishingException, DotDataException{
		User systemUser = uAPI.getSystemUser();
		
		try{
	        XStream xstream=new XStream(new DomDriver());
	        //Handle folders
	        for(File templateFile: templates) {
	        	if(templateFile.isDirectory()) continue;
	        	TemplateWrapper templateWrapper = (TemplateWrapper)  xstream.fromXML(new FileInputStream(templateFile));
	        	
	        	Template template = templateWrapper.getTemplate();
	        	Identifier templateId = templateWrapper.getTemplateId();
	        	Host localHost = APILocator.getHostAPI().find(templateId.getHostId(), systemUser, false);
	        	tAPI.saveTemplate(template, localHost, systemUser, false);
	        	VersionInfo info = templateWrapper.getVi();
                infoToRemove.add(info.getIdentifier());
                APILocator.getVersionableAPI().saveVersionInfo(info);
	        }
	        for (File templateFile : templates) {
	        	if(templateFile.isDirectory()) continue;
	        	TemplateWrapper templateWrapper = (TemplateWrapper)  xstream.fromXML(new FileInputStream(templateFile));
	        	VersionInfo info = templateWrapper.getVi();
                infoToRemove.add(info.getIdentifier());
                APILocator.getVersionableAPI().saveVersionInfo(info);
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
