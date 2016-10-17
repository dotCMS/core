package com.dotmarketing.viewtools;


import java.util.ArrayList;
import java.util.List;

import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.business.FolderFactory;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;

public class WebsiteWebAPI implements ViewTool {
	
    public void init(Object obj) {
    }
    
    @Deprecated
    public Folder getFolder (String parentFolder, long hostId) {
    	
    	try {
			return getFolder(parentFolder,String.valueOf(hostId));
		} catch (Exception e) {
			Logger.error(this, "Website getFolder Method : Unable to parse to String " ,e);
	    }
		return null;
    }
    
    public Folder getFolder (String parentFolder, String hostId) {
        Folder folder = new Folder();
		try {
			folder = APILocator.getFolderAPI().findFolderByPath(parentFolder, hostId,APILocator.getUserAPI().getSystemUser(),false);
		} catch (Exception e) {
			Logger.error(this,e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(),e);
		} 
        return folder;
    }
    
    @Deprecated
    public List<Folder> getSubFolders (String parentFolder, long hostId) {
        
        try {
			List<Folder> subFolders = getSubFolders (parentFolder, String.valueOf(hostId));
			return subFolders;
		} catch (Exception e) {
			Logger.error(this, "Website getSubFolders Method : Unable to parse to String " ,e);
		}
		return new ArrayList<Folder>();
    }
    
    public List<Folder> getSubFolders (String parentFolder, String hostId) {
        List<Folder> subFolders = new ArrayList<Folder>();
		try {
			Folder folder = APILocator.getFolderAPI().findFolderByPath(parentFolder, hostId,APILocator.getUserAPI().getSystemUser(),false);
			subFolders = APILocator.getFolderAPI().findSubFoldersTitleSort(folder, APILocator.getUserAPI().getSystemUser(),false);
		} catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		} 
        return subFolders;
    }

    public List<Folder> getSubFolders (Folder parentFolder) {
        List<Folder> subFolders = new ArrayList<Folder>();
		try {
			subFolders = APILocator.getFolderAPI().findSubFoldersTitleSort(parentFolder,APILocator.getUserAPI().getSystemUser(),false);
		} catch (Exception e) {
			Logger.error(this, e.getMessage(),e);
		}
        return subFolders;
    }
}