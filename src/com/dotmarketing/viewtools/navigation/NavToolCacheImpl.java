package com.dotmarketing.viewtools.navigation;

import java.util.LinkedList;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class NavToolCacheImpl implements NavToolCache {
    
    private DotCacheAdministrator cache;
    
    public NavToolCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
    }

    @Override
    public String getPrimaryGroup() {
        return GROUP;
    }

    @Override
    public String[] getGroups() {
        return new String[] {GROUP};
    }

    @Override
    public void clearCache() {
        cache.flushGroup(GROUP);
    }

    protected static String key(String hostid, String folderInode) {
        return hostid+":"+folderInode;
    }
    
    @Override
    public NavResult getNav(String hostid, String folderInode) {
        try {
            return (NavResult)cache.get(key(hostid,folderInode), GROUP);
        } catch (DotCacheException e) {
            Logger.warn(this, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void putNav(String hostid, String folderInode, NavResult result) {
        cache.put(key(hostid,folderInode), result, GROUP);
    }

    @Override
    public void removeNav(String folderInode) {
        Folder folder;
        try {
            folder = APILocator.getFolderAPI().find(folderInode, APILocator.getUserAPI().getSystemUser(), true);
            Identifier ident=APILocator.getIdentifierAPI().find(folder);
            removeNav(ident.getHostId(),folder.getInode());
        } catch (Exception e) {
            Logger.warn(this, e.getMessage(),e);
        }
    }
    
    @Override
    public void removeNav(String hostid, String folderInode) {
        Folder folder;
        try {
            if(!folderInode.equals(FolderAPI.SYSTEM_FOLDER)) {
                try {
                    folder = APILocator.getFolderAPI().find(folderInode, APILocator.getUserAPI().getSystemUser(), false);
                }
                catch(Exception ex) {
                    // here we catch the when it have been deleted
                    folder = null;
                }
                if(folder==null || !UtilMethods.isSet(folder.getIdentifier()) || !folder.isShowOnMenu()) {
                    // if the folder have been deleted or should not be shown on menu lets 
                    // remove cache recursively
                    LinkedList<String> ids=new LinkedList<String>();
                    ids.add(folderInode);
                    while(!ids.isEmpty()) {
                        String fid=ids.pop();
                        NavResult nav=getNav(hostid, fid);
                        if(nav!=null)
                            ids.addAll(nav.getChildrenFolderIds());
                        cache.remove(key(hostid,fid), GROUP);
                    }
                    return;
                }
            }
            
            cache.remove(key(hostid,folderInode), GROUP);
            
        } catch (Exception e) {
            Logger.warn(this, e.getMessage(), e);
        }
        
    }

    @Override
    public void removeNavByPath(String hostid, String path) {
        Folder folder;
        try {
            folder = APILocator.getFolderAPI().findFolderByPath(path, hostid, APILocator.getUserAPI().getSystemUser(), false);
            if(folder != null)
            	removeNav(hostid,folder.getInode());
        } catch (Exception e) {
            Logger.warn(this, e.getMessage(), e);
        }
    }
    
}
