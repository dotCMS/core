package com.dotmarketing.viewtools.navigation;

import java.util.List;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;

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
    public List<NavResult> getNav(String hostid, String folderInode) {
        try {
            return (List<NavResult>)cache.get(key(hostid,folderInode), GROUP);
        } catch (DotCacheException e) {
            Logger.warn(this, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void putNav(String hostid, String folderInode, List<NavResult> items) {
        cache.put(key(hostid,folderInode), items, GROUP);
    }

    @Override
    public void removeNav(String hostid, String folderInode) {
        cache.remove(key(hostid,folderInode), GROUP);
    }
    
    @Override
    public void removeNav(String folderInode) {
        Folder folder;
        try {
            folder = APILocator.getFolderAPI().find(folderInode, APILocator.getUserAPI().getSystemUser(), false);
            cache.remove(key(folder.getHostId(),folderInode), GROUP);
        } catch (Exception e) {
            Logger.warn(this, e.getMessage(), e);
        }
        
    }

    @Override
    public void removeNavAndChildren(String hostid, String folderInode) {
        removeNav(hostid,folderInode);
        
        // this is the black hole eating stars
    }

    @Override
    public void removeNavByPath(String hostid, String path) {
        Folder folder;
        try {
            folder = APILocator.getFolderAPI().findFolderByPath(path, hostid, APILocator.getUserAPI().getSystemUser(), false);
            removeNav(hostid,folder.getInode());
        } catch (Exception e) {
            Logger.warn(this, e.getMessage(), e);
        }
    }
    
}
