package com.dotcms.rendering.velocity.viewtools.navigation;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.util.LinkedList;
import java.util.List;

public class NavToolCacheImpl implements NavToolCache {

    public final String GROUP="navCache";

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

    protected static String key(String hostid, String folderInode, long languageId) {
        return hostid + ":" + folderInode + ":" + languageId;
    }

    @Override
    public void clearCache() {
        cache.flushGroup(GROUP);
    }
    
    @Override
    public NavResult getNav(String hostid, String folderInode, long languageId) {
        try {
            return (NavResult)cache.get(key(hostid,folderInode, languageId), GROUP);
        } catch (DotCacheException e) {
            Logger.warn(this, e.getMessage(), e);
            return null;
        }
    }

    /**
     * we only add folders to the nav cache, as that is how they are called
     */
    @Override
    public void putNav(String hostid, String folderInode, NavResult result, long languageId) {
      if(result!=null  && "folder".equalsIgnoreCase( result.getType())){
        cache.put(key(hostid,folderInode, languageId), result.getUnhydratedNavResult(), GROUP);
      }
    }

    @Override
    public void removeNav(String hostid, String folderInode) {
        List<Language> allLanguages = APILocator.getLanguageAPI().getLanguages();

        if (null != folderInode) {
            for (Language language : allLanguages) {
                removeNav(hostid, folderInode, language.getId());
            }
        }
    }

    @Override
    public void removeNav(String hostid, String folderInode, long languageId) {
        Folder folder;
        try {
            if(!FolderAPI.SYSTEM_FOLDER.equals(folderInode)) {
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
                        NavResult nav=getNav(hostid, fid, languageId);
                        if(nav!=null)
                            ids.addAll(nav.getChildrenFolderIds());
                        cache.remove(key(hostid,fid, languageId), GROUP);
                    }
                    return;
                }
            }
            
            cache.remove(key(hostid,folderInode, languageId), GROUP);
            
        } catch (Exception e) {
            Logger.warn(this, e.getMessage(), e);
        }
        
    }

    @Override
    public void removeNavByPath(String hostid, String path) {
        List<Language> allLanguages = APILocator.getLanguageAPI().getLanguages();

        for(Language language : allLanguages){
            removeNavByPath(hostid, path, language.getId());
        }
    }

    @Override
    public void removeNavByPath(String hostid, String path, long languageId) {
        Folder folder;
        try {
            folder = APILocator.getFolderAPI().findFolderByPath(path, hostid, APILocator.getUserAPI().getSystemUser(), false);
            if(folder != null)
            	removeNav(hostid,folder.getInode(), languageId);
        } catch (Exception e) {
            Logger.warn(this, e.getMessage(), e);
        }
    }
    
}
