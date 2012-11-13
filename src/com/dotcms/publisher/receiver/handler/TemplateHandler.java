package com.dotcms.publisher.receiver.handler;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;

import com.dotcms.publisher.myTest.bundler.TemplateBundler;
import com.dotcms.publisher.myTest.wrapper.TemplateWrapper;
import com.dotcms.publishing.DotPublishingException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class TemplateHandler implements IHandler {
	private IdentifierAPI iAPI = APILocator.getIdentifierAPI();
	private UserAPI uAPI = APILocator.getUserAPI();
	private TemplateAPI tAPI = APILocator.getTemplateAPI();

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
	        	
	        	if(!UtilMethods.isSet(iAPI.find(template).getId())) {
	        		Identifier id = iAPI.find(template.getIdentifier());
	        		Host localHost = APILocator.getHostAPI().find(templateId.getHostId(), systemUser, false);
        			if(id ==null || !UtilMethods.isSet(id.getId())){
        				Identifier templateIdNew = null;
        				
        				templateIdNew = iAPI.createNew(template, 
        						localHost, 
        						templateId.getId());
	            			
        				
        				template.setIdentifier(templateIdNew.getId());
            		}
        			
        			tAPI.saveTemplate(template, localHost, systemUser, false);
	        	}        			
	        }
        	
    	}
    	catch(Exception e){
    		throw new DotPublishingException(e.getMessage(),e);
    	}    	
    }
}
