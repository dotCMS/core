package com.dotcms.rest.api.v1.folder;

import com.dotcms.repackage.net.sf.hibernate.expression.Example;
import com.dotcms.repackage.org.apache.commons.collections.map.HashedMap;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.util.TreeableNameComparator;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by jasontesser on 9/28/16.
 */
public class FolderHelper {

    private final HostAPI hostAPI;
    private final FolderAPI folderAPI;
    private final UserAPI userAPI;

    private final static TreeableNameComparator TREEABLE_NAME_COMPARATOR =
            new TreeableNameComparator();

    private FolderHelper() {
        this.hostAPI = APILocator.getHostAPI();
        this.folderAPI = APILocator.getFolderAPI();
        this.userAPI = APILocator.getUserAPI();
    }

    private static class SingletonHolder {
        private static final FolderHelper INSTANCE = new FolderHelper();
    }

    protected class FolderResults {

        protected FolderResults(List<Folder> folders, List<Map<String, String>> errors) {
            this.setErrors(errors);
            this.setFolders(folders);
        }

        List<Folder> folders;
        /**
         * Map of any errors found while saving a folder.  The map has 2 attributes. failedPath and errorMessage
         */
        List<Map<String, String>> errors;

        protected List<String> getErrorKeys() {
            List<String> keys = new ArrayList<String>();
            keys.add("failedPath");
            keys.add("errorMessage");
            return keys;
        }

        protected List<Folder> getFolders() {
            return folders
                    .stream()
                    .collect(Collectors.toList());
        }

        protected void setFolders(List<Folder> folders) {
            this.folders = folders;
        }

        protected List<ErrorEntity> getErrorEntities() {
            List<ErrorEntity> ret = new ArrayList<ErrorEntity>();
            for (Map<String,String> error : errors) {
                ErrorEntity ee = new ErrorEntity(error.get("failedPath"),error.get("errorMessage"));
                ret.add(ee);
            }
            return ret;
        }

        protected void setErrors(List<Map<String, String>> errors) {
            this.errors = errors;
        }
    }

    /**
     * Get the instance.
     *
     * @return BrowserTreeHelper
     */
    public static FolderHelper getInstance() {

        return FolderHelper.SingletonHolder.INSTANCE;
    } // getInstance.

    public FolderResults createFolders(List<String> paths, String siteName, User user) throws DotDataException, DotSecurityException {

        List<Folder> savedFolders = new ArrayList<Folder>();
        List<Map<String, String>> errors = new ArrayList<Map<String, String>>();
        Host host = hostAPI.findByName(siteName, user, true);

        for (String path : paths) {
            try {
                savedFolders.add(folderAPI.createFolders(path, host, user, true));
            } catch (Exception e) {
                Logger.error(this, e.getMessage(), e);
                Map<String, String> error = new HashMap<String, String>();
                error.put("failedPath",path);
                error.put("errorMessage",e.getMessage());
                errors.add(error);
            }
        }
        return new FolderResults(savedFolders, errors);
    }

    /**
     * Return the folder for the passes in URI.
     *
     * @param siteName Folder to lookup the URI on
     * @param user
     * @param uri The URI (Path) to get the folder for. If you pass / will return the SystemFolder
     * @return List of assets that the given user has permissions to see under a URI on a Host. (Currently this only returns Folder and File assets)
     * @throws com.dotmarketing.exception.DotDataException if one is thrown when the sites are search
     * @throws com.dotmarketing.exception.DotSecurityException if one is thrown when the sites are search
     */
    public Folder loadFolderByURI(String siteName, User user, String uri) throws DotDataException, DotSecurityException {
        Host host = hostAPI.findByName(siteName,user,true);
        Folder ret = null;
        if(uri.equals("/")){
            ret = folderAPI.findSystemFolder();
        }else{
            ret = folderAPI.findFolderByPath(uri,host,user,true);
        }
        return ret;
    }

}
