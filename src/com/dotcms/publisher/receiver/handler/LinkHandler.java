package com.dotcms.publisher.receiver.handler;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.dotcms.publisher.pusher.bundler.LinkBundler;
import com.dotcms.publisher.pusher.wrapper.LinkWrapper;
import com.dotcms.publishing.DotPublishingException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class LinkHandler implements IHandler {
	private UserAPI uAPI = APILocator.getUserAPI();
	private List<String> infoToRemove = new ArrayList<String>();

	@Override
	public String getName() {
		return this.getClass().getName();
	}
	
	@Override
	public void handle(File bundleFolder) throws Exception {
		Collection<File> templates = FileUtil.listFilesRecursively(bundleFolder, new LinkBundler().getFileFilter());
		
		handleLinks(templates);
	}
	
	private void handleLinks(Collection<File> links) throws DotPublishingException, DotDataException{
		User systemUser = uAPI.getSystemUser();
		
		try{
	        XStream xstream=new XStream(new DomDriver());

	        for(File linkFile: links) {
	        	if(linkFile.isDirectory()) continue;
	        	LinkWrapper linkWrapper = (LinkWrapper)  xstream.fromXML(new FileInputStream(linkFile));
	        	
	        	Link link = linkWrapper.getLink(); 
	        	Host h = APILocator.getHostAPI().find(link.getHostId(), systemUser, false);
	        	Folder destination = APILocator.getFolderAPI().findFolderByPath(link.getParent(), h, systemUser, false);
	        	APILocator.getMenuLinkAPI().save(link, destination, systemUser, false);
	        }
	        for (File linkFile : links) {
	        	if(linkFile.isDirectory()) continue;
	        	LinkWrapper linkWrapper = (LinkWrapper)  xstream.fromXML(new FileInputStream(linkFile));
	        	VersionInfo info = linkWrapper.getVi();
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
