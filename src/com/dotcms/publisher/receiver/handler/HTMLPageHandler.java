package com.dotcms.publisher.receiver.handler;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;

import com.dotcms.publisher.myTest.bundler.HTMLPageBundler;
import com.dotcms.publisher.myTest.wrapper.HTMLPageWrapper;
import com.dotcms.publishing.DotPublishingException;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageAPI;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class HTMLPageHandler implements IHandler {
	private IdentifierAPI iAPI = APILocator.getIdentifierAPI();
	private UserAPI uAPI = APILocator.getUserAPI();
	private FolderAPI fAPI = APILocator.getFolderAPI();
	private HTMLPageAPI htmlAPI = APILocator.getHTMLPageAPI();

	@Override
	public String getName() {
		return this.getClass().getName();
	}
	
	@Override
	public void handle(File bundleFolder) throws Exception {
		Collection<File> pages = FileUtil.listFilesRecursively(bundleFolder, new HTMLPageBundler().getFileFilter());
		
        handlePages(pages);
	}
	
	private void handlePages(Collection<File> pages) throws DotPublishingException, DotDataException{
		User systemUser = uAPI.getSystemUser();
		
		try{
	        XStream xstream=new XStream(new DomDriver());
	        //Handle folders
	        for(File pageFile: pages) {
	        	if(pageFile.isDirectory()) continue;
	        	HTMLPageWrapper pageWrapper = (HTMLPageWrapper)  xstream.fromXML(new FileInputStream(pageFile));
	        	
	        	HTMLPage htmlPage = pageWrapper.getPage();
	        	Identifier htmlPageId = pageWrapper.getPageId();
	        	
	        	if(!UtilMethods.isSet(iAPI.find(htmlPage))) {
	        		Identifier id = iAPI.find(htmlPage.getIdentifier());
	        		Folder parentFolder = fAPI.find(htmlPage.getParent(), systemUser, false);
        			if(id ==null || !UtilMethods.isSet(id.getId())){
        				Identifier pageIdNew = null;
        				
        				pageIdNew = iAPI.createNew(htmlPage, 
        						parentFolder, 
            					htmlPageId.getId());
	            			
        				
            			htmlPage.setIdentifier(pageIdNew.getId());
            		}
        			
        			htmlAPI.saveHTMLPage(htmlPage, 
        					APILocator.getTemplateAPI().findLiveTemplate(htmlPage.getTemplateId(), systemUser, false), 
        					parentFolder, 
        					systemUser, 
        					false);
	        	}        			
	        }
        	
    	}
    	catch(Exception e){
    		throw new DotPublishingException(e.getMessage(),e);
    	}    	
    }
}
