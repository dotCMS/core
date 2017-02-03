package com.dotmarketing.portlets.files.util;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;

import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

/**
 *@deprecated 
 * @author will
 */

public class FileUtils {




    @SuppressWarnings("unchecked")
	public static boolean existsFileName(Inode parent, String fileName) throws DotStateException, DotDataException, DotSecurityException {

        Logger.debug(FileUtils.class, "UtilMethods.sqlify(fileName)" + UtilMethods.sqlify(fileName));
        List<File> files = APILocator.getFolderAPI().getWorkingFiles((Folder)parent, APILocator.getUserAPI().getSystemUser(),false);
		for(File f:files){
			if(fileName.equalsIgnoreCase(f.getFileName())){
				return (InodeUtils.isSet(f.getInode()));
			}
		}
        return false;
    }



    
    public static File getArchivedFileByIdentifier(Identifier i) throws DotStateException, DotDataException, DotSecurityException {
            return (File)APILocator.getVersionableAPI().findDeletedVersion(i, APILocator.getUserAPI().getSystemUser(),false);
    }

    public static String getVirtualFileURI(File file) throws DotDataException {

        Identifier identifier = (Identifier) APILocator.getIdentifierAPI().find(file.getIdentifier());
        if (InodeUtils.isSet(identifier.getInode())) {
            return (Config.getStringProperty("VIRTUAL_FILE_PREFIX") + identifier.getURI()).intern();
        }
        return null;
    }

    public static String getVersionFileURI(File file) {

        return Config.getStringProperty("VERSION_FILE_PREFIX") + file.getInode() + "."
                + UtilMethods.getFileExtension(file.getFileName()).intern();

    }

    public static String getRelativeAssetPath(Inode inode) {
        String _inode = inode.getInode();
        return getRelativeAssetPath(_inode, UtilMethods.getFileExtension(((com.dotmarketing.portlets.files.model.File) inode).getFileName())
                            .intern());
    }

    public static String getRelativeAssetPath(String inode, String ext) {
        String _inode = inode;
        String path = "";

       	path = java.io.File.separator + _inode.charAt(0)
       		+ java.io.File.separator + _inode.charAt(1) + java.io.File.separator + _inode + "." + ext;

        return path;
    }

    public static String getRealAssetPath(Inode inode) {
        String _inode = inode.getInode();
        return getRealAssetPath (_inode, UtilMethods.getFileExtension(((com.dotmarketing.portlets.files.model.File) inode).getFileName())
                .intern());
    }

    public static String getRealAssetPath(String inode, String ext) {
        String _inode = inode;
        String path = "";

        String realPath = Config.getStringProperty("ASSET_REAL_PATH");
        if (UtilMethods.isSet(realPath) && !realPath.endsWith(java.io.File.separator))
            realPath = realPath + java.io.File.separator;

        String assetPath = Config.getStringProperty("ASSET_PATH");
        if (UtilMethods.isSet(assetPath) && !assetPath.endsWith(java.io.File.separator))
            assetPath = assetPath + java.io.File.separator;
        
        path = ((!UtilMethods.isSet(realPath)) ? assetPath : realPath)
                + _inode.charAt(0) + java.io.File.separator + _inode.charAt(1)
                + java.io.File.separator + _inode + "." + ext;

        if (!UtilMethods.isSet(realPath))
            return FileUtil.getRealPath(path);
        else
            return path;
    	
    }
    
    /**
     * This method returns the path for the file assets directory
     * @return
     */
    public static String getRealAssetPath(){
    	
    	String realPath = null;
		String assetPath = null;
		try {
            realPath = Config.getStringProperty("ASSET_REAL_PATH");
        } catch (Exception e) { }
        try {
            assetPath = Config.getStringProperty("ASSET_PATH");
        } catch (Exception e) { }
        
        if(!UtilMethods.isSet(realPath)){
        	return FileUtil.getRealPath(assetPath);
        }else{
        	return realPath;
        }
    }

    public static String getRelativeAssetsRootPath() {
        String path = "";

        path = Config.getStringProperty("ASSET_PATH");

        return path;
    }

    public static String getRealAssetsRootPath() {
        String realPath = Config.getStringProperty("ASSET_REAL_PATH");
        if (UtilMethods.isSet(realPath) && !realPath.endsWith(java.io.File.separator))
            realPath = realPath + java.io.File.separator;
        if (!UtilMethods.isSet(realPath))
            return FileUtil.getRealPath(getRelativeAssetsRootPath());
        else
            return realPath;
    }

    // remove the uri from a single cache
    /*
     * Never used methods
     * 
     * private synchronized static void removeURI(Object key, java.util.Map map) {
     * map.remove(key); }
     * 
     * private static long howBigIsCache(java.util.Map map) {
     * 
     * long size = 0;
     * 
     * for (Iterator it = map.entrySet().iterator(); it.hasNext();) { Map.Entry
     * me = (Map.Entry) it.next(); size += me.getKey().toString().length(); size +=
     * me.getValue().toString().length(); } return size; }
     */

    @SuppressWarnings("unchecked")
	public static java.util.List<File> getFilesByCondition(String condition) {

        HibernateUtil dh = new HibernateUtil(File.class);
        List list=null ;
        try {
			dh.setQuery("from inode in class class com.dotmarketing.portlets.files.model.File where type='file_asset' and " + condition
			        + " order by file_name, sort_order");
			list = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(FileUtils.class, e.getMessage(), e);
		}
        return list;
    }
    
	public static java.util.List<File> getFilesPerRoleParentAndCondition(Role[] roles,
            com.dotmarketing.portlets.folders.model.Folder folderParent, String condition, User user) 
    {
		String order = "";
		return getFilesPerRoleParentAndCondition(roles, folderParent, condition, user, order);
	}

    @SuppressWarnings("unchecked")
	public static java.util.List<File> getFilesPerRoleParentAndCondition(Role[] roles,
            com.dotmarketing.portlets.folders.model.Folder folderParent, String condition, User user, String order) {
    	PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        java.util.List<File> entries = new java.util.ArrayList<File>();
               
        java.util.List permissions = new ArrayList();
		try {
			permissions = permissionAPI.getPermissionIdsFromRoles(folderParent, roles, user);
		} catch (DotDataException e) {
			Logger.error(FileUtils.class, "Could not load permissions : ",e);
		}
        
        if (permissions.contains(String.valueOf(PERMISSION_READ))) {
            // read permission
            try {
				entries = getFileChildrenByConditionAndOrder(folderParent,condition,order);
			} catch (Exception e) {
				Logger.error(FileUtils.class, e.getMessage(), e);
			} 
        }
        return entries;
    }
    
    @SuppressWarnings("unchecked")
	public static java.util.List<File> getFilesByParentFolderPerRole(
            Folder folderParent, String order, User user) throws DotDataException, DotSecurityException {

    	

        List<File> entries =APILocator.getFolderAPI().getLiveFiles(folderParent, user, true);

		
		return entries;
    }

 
    

	
  
	
	public class ThumbnailsFileNamesFilter implements FilenameFilter {

		List<Versionable> versions;
		
		@SuppressWarnings("unchecked")
		public ThumbnailsFileNamesFilter (Identifier fileIden) throws DotStateException, DotDataException, DotSecurityException {
			versions = APILocator.getVersionableAPI().findAllVersions(fileIden);
			
		}
		
		public boolean accept(java.io.File dir, String name) {
			
			for (Versionable version : versions) {
				File file = (File)version;
				if (name.startsWith(String.valueOf(file.getInode()) + "_thumb") ||
						name.startsWith(String.valueOf(file.getInode()) + "_resized"))
					return true;
			}
			return false;
		}
		
	}
    @SuppressWarnings("unchecked")
	public static java.util.List<File> getFileChildrenByConditionAndOrder(Inode i, String condition,String order) {

        return InodeFactory.getChildrenClassByConditionAndOrderBy(i, File.class, condition, order);

    }


    @SuppressWarnings("unchecked")
	public static java.util.List<File> getChildrenFilesByOrder(Inode i) {

        return InodeFactory.getChildrenClassByOrder(i, File.class, "sort_order");

    }

}
