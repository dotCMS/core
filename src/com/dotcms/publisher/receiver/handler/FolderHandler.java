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
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class FolderHandler implements IHandler {
	private FolderAPI fAPI = APILocator.getFolderAPI();
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
        
		handleFolders(folders);
	}
	
	private void handleFolders(Collection<File> folders) throws DotPublishingException, DotDataException{
		User systemUser = uAPI.getSystemUser();
		
		try{
	        XStream xstream=new XStream(new DomDriver());
	        //Handle folders
	        for(File folderFile: folders) {
	        	if(folderFile.isDirectory()) continue;
	        	FolderWrapper folderWrapper = (FolderWrapper)  xstream.fromXML(new FileInputStream(folderFile));
	        	
	        	Folder folder = folderWrapper.getFolder();
	        	Identifier folderId = folderWrapper.getFolderId();
	        	Host host = folderWrapper.getHost();
	        	Identifier hostId = folderWrapper.getHostId();
	        	
	        	
	        	
	        	//Check Host if exists otherwise create
	        	Host localHost = APILocator.getHostAPI().findByName(host.getHostname(), systemUser, false);
        		
        		if(localHost == null) {
        			host.setProperty("_dont_validate_me", true);
        			
        			Identifier idNew = iAPI.createNew(host, APILocator.getHostAPI().findSystemHost(), hostId.getId());
        			host.setIdentifier(idNew.getId());
        			localHost = APILocator.getHostAPI().save(host, systemUser, false);
        		}
	        	
	        	//Loop over the folder
        		if(!UtilMethods.isSet(fAPI.findFolderByPath(folderId.getPath(), localHost, systemUser, false).getInode())) {
        			Identifier id = iAPI.find(folder.getIdentifier());
        			if(id ==null || !UtilMethods.isSet(id.getId())){
        				Identifier folderIdNew = null;
        				if(folderId.getParentPath().equals("/")) {
	            			folderIdNew = iAPI.createNew(folder, 
	            					localHost, 
	            					folderId.getId());
        				} else {
        					folderIdNew = iAPI.createNew(folder, 
                					fAPI.findFolderByPath(folderId.getParentPath(), localHost, systemUser, false), 
                					folderId.getId());
        				}
            			folder.setIdentifier(folderIdNew.getId());
            		}
        			fAPI.save(folder, folder.getInode(), systemUser, false);
        		}
        			
	        }
        	
    	}
    	catch(Exception e){
    		throw new DotPublishingException(e.getMessage(),e);
    	}
	}

}
