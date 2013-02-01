package com.dotcms.publisher.receiver.handler;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import com.dotcms.publisher.pusher.bundler.FolderBundler;
import com.dotcms.publisher.pusher.wrapper.FolderWrapper;
import com.dotcms.publishing.DotPublishingException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import edu.emory.mathcs.backport.java.util.Collections;

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
        List<File> folders = new ArrayList<File>();
        if(new File(bundleFolder + File.separator + "ROOT").exists()){
        	folders = FileUtil.listFilesRecursively(new File(bundleFolder + File.separator + "ROOT"), new FolderBundler().getFileFilter());
        	Collections.sort(folders, new Comparator<File>() {
				@Override
				public int compare(File o1, File o2) {
					if(o1.getAbsolutePath().length() > o2.getAbsolutePath().length())
						return 1;
					else
						return -1;
				}
			});
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
	        	
	        	//Check Host if exists otherwise create
	        	Host localHost = APILocator.getHostAPI().find(host.getIdentifier(), systemUser, false);
        		
	        	
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
        			

        			//Set defaul type
        			Structure defaultStr = StructureCache.getStructureByVelocityVarName(FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME);
        			
        			folder.setDefaultFileType(defaultStr.getInode());
        			
        			Folder localFolder = fAPI.findFolderByPath(folderId.getPath(), localHost, systemUser, false);
        			if((localFolder == null || !UtilMethods.isSet(localFolder.getInode())) && !fAPI.exists(folder.getInode()))
        				fAPI.save(folder, folder.getInode(), systemUser, false);
        			else
        				fAPI.save(folder, systemUser, false);
        		}
        			
	        }
        	
    	}
    	catch(Exception e){
    		throw new DotPublishingException(e.getMessage(),e);
    	}
	}

}
