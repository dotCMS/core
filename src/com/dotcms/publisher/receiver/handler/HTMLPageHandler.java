package com.dotcms.publisher.receiver.handler;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.dotcms.publisher.myTest.bundler.HTMLPageBundler;
import com.dotcms.publisher.myTest.wrapper.HTMLPageWrapper;
import com.dotcms.publishing.DotPublishingException;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageAPI;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class HTMLPageHandler implements IHandler {
	private UserAPI uAPI = APILocator.getUserAPI();
	private FolderAPI fAPI = APILocator.getFolderAPI();
	private HTMLPageAPI htmlAPI = APILocator.getHTMLPageAPI();
	private List<String> infoToRemove = new ArrayList<String>();

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
	        		
        		Folder parentFolder = fAPI.findFolderByPath(htmlPageId.getParentPath(), 
        		        APILocator.getHostAPI().find(htmlPageId.getHostId(), systemUser, false), systemUser, false);
    			
    			htmlAPI.saveHTMLPage(htmlPage, 
    					APILocator.getTemplateAPI().findWorkingTemplate(htmlPage.getTemplateId(), systemUser, false), 
    					parentFolder, 
    					systemUser, 
    					false);
	        }
	        
	        for(File pageFile: pages) {
	        	if(pageFile.isDirectory()) continue;
	        	HTMLPageWrapper pageWrapper = (HTMLPageWrapper)  xstream.fromXML(new FileInputStream(pageFile));
	        	VersionInfo info = pageWrapper.getVi();
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
