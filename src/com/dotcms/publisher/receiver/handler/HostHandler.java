package com.dotcms.publisher.receiver.handler;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;

import com.dotcms.publisher.myTest.bundler.FolderBundler;
import com.dotcms.publisher.myTest.wrapper.FolderWrapper;
import com.dotcms.publishing.DotPublishingException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class HostHandler implements IHandler {
	private IdentifierAPI iAPI = APILocator.getIdentifierAPI();
	private UserAPI uAPI = APILocator.getUserAPI();
	
	@Override
	public String getName() {
		return this.getClass().getName();
	}
	
	@Override
	public void handle(File bundleFolder) throws Exception {
		//For each content take the wrapper and save it on DB
        Collection<File> folders = new ArrayList<File>();
        if(new File(bundleFolder + File.separator + "ROOT").exists()){
        	folders = FileUtil.listFilesRecursively(new File(bundleFolder + File.separator + "ROOT"), new FolderBundler().getFileFilter());
        }
        
        handleHosts(folders);
	}
	
	private void handleHosts(Collection<File> folders) throws DotPublishingException, DotDataException{
		User systemUser = uAPI.getSystemUser();
		
		try{
	        XStream xstream=new XStream(new DomDriver());
	        //Handle folders
	        for(File folderFile: folders) {
	        	if(folderFile.isDirectory()) continue;
	        	FolderWrapper folderWrapper = (FolderWrapper)  xstream.fromXML(new FileInputStream(folderFile));
	        	
	        	Host host = folderWrapper.getHost();
	        	Identifier hostId = folderWrapper.getHostId();
	        	
	        	
	        	
	        	//Check Host if exists otherwise create
	        	Host localHost = APILocator.getHostAPI().find(host.getIdentifier(), systemUser, false);
        		
        		if(localHost == null) {
        			host.setProperty("_dont_validate_me", true);
        			
        			Identifier idNew = iAPI.createNew(host, APILocator.getHostAPI().findSystemHost(), hostId.getId());
        			host.setIdentifier(idNew.getId());
        			localHost = APILocator.getHostAPI().save(host, systemUser, false);
        		}
	        }
        	
    	}
    	catch(Exception e){
    		throw new DotPublishingException(e.getMessage(),e);
    	}
	}

}
