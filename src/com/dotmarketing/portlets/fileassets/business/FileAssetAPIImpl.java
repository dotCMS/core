package com.dotmarketing.portlets.fileassets.business;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.dotcms.tika.TikaUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.cache.WorkingCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.menubuilders.RefreshMenus;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * This class is a bridge impl that will support the older
 * com.dotmarketing.portlets.file.model.File as well as the new Contentlet based
 * files
 * 
 * @author will
 * 
 */
public class FileAssetAPIImpl implements FileAssetAPI {

	ContentletAPI contAPI;
	PermissionAPI perAPI;

	public FileAssetAPIImpl() {
		contAPI = APILocator.getContentletAPI();
		perAPI = APILocator.getPermissionAPI();
	}

	public List<FileAsset> findFileAssetsByFolder(Folder parentFolder, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {
		List<FileAsset> assets = null;
		try{
			assets = fromContentlets(perAPI.filterCollection(contAPI.search("+structureType:" + Structure.STRUCTURE_TYPE_FILEASSET+" +conFolder:" + parentFolder.getInode(), -1, 0, null , user, respectFrontendRoles), 
					PermissionAPI.PERMISSION_READ, respectFrontendRoles, user));
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
		return assets;

	}
	
	public List<FileAsset> findFileAssetsByHost(Host parentHost, User user, boolean respectFrontendRoles) throws DotDataException,
	DotSecurityException {
		List<FileAsset> assets = null;
		try{
			Folder parentFolder = APILocator.getFolderAPI().find(FolderAPI.SYSTEM_FOLDER, user, false);
			assets = fromContentlets(perAPI.filterCollection(contAPI.search("+conHost:" +parentHost.getIdentifier() +" +structureType:" + Structure.STRUCTURE_TYPE_FILEASSET+" +conFolder:" + parentFolder.getInode(), -1, 0, null , user, respectFrontendRoles), 
					PermissionAPI.PERMISSION_READ, respectFrontendRoles, user));
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
		return assets;

	}

	public void createBaseFileAssetFields(Structure structure) throws DotDataException, DotStateException {
		if (structure == null || !InodeUtils.isSet(structure.getInode())) {
			throw new DotStateException("Cannot create base fileasset fields on a structure that doesn't exist");
		}
		if (structure.getStructureType() != Structure.STRUCTURE_TYPE_FILEASSET) {
			throw new DotStateException("Cannot create base fileasset fields on a structure that is not a file asset");
		}
		Field field = new Field(HOST_FOLDER_FIELD_NAME, Field.FieldType.HOST_OR_FOLDER, Field.DataType.TEXT, structure, true, false, true, 1,
				"", "", "", true, false, true);

		field.setVelocityVarName(HOST_FOLDER_FIELD);
		FieldFactory.saveField(field);

		field = new Field(BINARY_FIELD_NAME, Field.FieldType.BINARY, Field.DataType.BINARY, structure, true, false, false, 2, "", "", "", true,
				false, false);
		field.setVelocityVarName(BINARY_FIELD);
		FieldFactory.saveField(field);
		
		
		field = new Field(TITLE_FIELD_NAME, Field.FieldType.TEXT, Field.DataType.TEXT, structure, true, true, true, 3, "", "", "", true, false,
				true);
		field.setVelocityVarName(TITLE_FIELD);
		field.setListed(false);
		FieldFactory.saveField(field);
		
		
		field = new Field(FILE_NAME_FIELD_NAME, Field.FieldType.TEXT, Field.DataType.TEXT, structure, false, true, true, 4, "", "", "", true, true,
				true);
		field.setVelocityVarName(FILE_NAME_FIELD);
		FieldFactory.saveField(field);
		

		field = new Field(META_DATA_TAB_NAME, Field.FieldType.TAB_DIVIDER, Field.DataType.SECTION_DIVIDER, structure, false, false, false, 5, "", "", "", false,
				false, false);
		field.setVelocityVarName("MetadataTab");
		FieldFactory.saveField(field);
		
		
		field = new Field(META_DATA_FIELD_NAME, Field.FieldType.KEY_VALUE, Field.DataType.LONG_TEXT, structure, false, false, false, 6, "", "", "", true,
				true, true);
		field.setVelocityVarName(META_DATA_FIELD);
		FieldFactory.saveField(field);

		
		field = new Field(SHOW_ON_MENU_NAME, Field.FieldType.CHECKBOX, Field.DataType.TEXT, structure, false, false, true, 7, "|true", "false", "", true, false,
				false);
		field.setVelocityVarName(SHOW_ON_MENU);
		FieldFactory.saveField(field);
		
		
		field = new Field(SORT_ORDER_NAME, Field.FieldType.TEXT, Field.DataType.INTEGER, structure, false, false, true, 8, "", "0", "", true, false,
				false);
		field.setVelocityVarName(SORT_ORDER);
		FieldFactory.saveField(field);
		

		
		field = new Field(DESCRIPTION_NAME, Field.FieldType.TEXT, Field.DataType.TEXT, structure, false, true, true, 9, "", "", "", true, false,
				true);
		field.setVelocityVarName(DESCRIPTION);
		field.setListed(false);
		field.setSearchable(false);
		FieldFactory.saveField(field);
		
		FieldsCache.clearCache();
	}

	public FileAsset fromContentlet(Contentlet con) throws DotStateException {
		if (con == null || con.getStructure().getStructureType() != Structure.STRUCTURE_TYPE_FILEASSET) {
			throw new DotStateException("Contentlet : " + con.getInode() + " is not a FileAsset");
		}

		FileAsset fa = new FileAsset();
		fa.setStructureInode(con.getStructureInode());
		try {
			contAPI.copyProperties((Contentlet) fa, con.getMap());
		} catch (Exception e) {
			throw new DotStateException("File Copy Failed :" + e.getMessage(), e);
		}
		fa.setHost(con.getHost());
		if(UtilMethods.isSet(con.getFolder())){
			try{
				Identifier ident = APILocator.getIdentifierAPI().find(con);
				User systemUser = APILocator.getUserAPI().getSystemUser();			
				Host host = APILocator.getHostAPI().find(con.getHost(), systemUser , false);
				Folder folder = APILocator.getFolderAPI().findFolderByPath(ident.getParentPath(), host, systemUser, false);
				fa.setFolder(folder.getInode());
			}catch(Exception e){
				Logger.warn(this, "Unable to convert contentlet to file asset " + con, e);
			}
		}
		return fa;
	}

	public List<FileAsset> fromContentlets(List<Contentlet> cons) {
		List<FileAsset> fas = new ArrayList<FileAsset>();
		for (Contentlet con : cons) {
			fas.add(fromContentlet(con));
		}
		return fas;

	}
	public boolean isFileAsset(Contentlet con)  {
		return (con != null && con.getStructure() != null && con.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_FILEASSET) ;
	}

	public Map<String, String> getMetaDataMap(Contentlet con, File binFile)  {

		return new TikaUtils().getMetaDataMap(binFile);

	}
	

	
	public boolean fileNameExists(Host host, Folder folder, String fileName, String identifier) throws  DotDataException{
		if(!UtilMethods.isSet(fileName)){
			return true;
		}
		if(folder==null)
			return false;

		boolean ret = false;
		Identifier folderId = APILocator.getIdentifierAPI().find(folder);
		String path = folder.getInode().equals(FolderAPI.SYSTEM_FOLDER)?"/"+fileName:folderId.getPath()+fileName;
		Identifier fileAsset = APILocator.getIdentifierAPI().find(host, path);
		if(fileAsset!=null && InodeUtils.isSet(fileAsset.getId()) && !identifier.equals(fileAsset.getId()) && !fileAsset.getAssetType().equals("folder")){
			ret = true;
		}
		return ret;
    }

	public String getRelativeAssetPath(FileAsset fa) {
		String _inode = fa.getInode();
		return getRelativeAssetPath(_inode, fa.getFileName());
	}

	private  String getRelativeAssetPath(String inode, String fileName) {
		String _inode = inode;
		String path = "";

		path = java.io.File.separator + _inode.charAt(0)
				+ java.io.File.separator + _inode.charAt(1) + java.io.File.separator + _inode + java.io.File.separator + "fileAsset" + java.io.File.separator+ fileName;

		return path;

	}
	
	public  boolean renameFile (Contentlet fileAssetCont, String newName, User user, boolean respectFrontendRoles) throws DotStateException, DotDataException, DotSecurityException, IOException {
		boolean isfileAssetContLive = false;
		Identifier id = APILocator.getIdentifierAPI().find(fileAssetCont);
		if(id!=null && InodeUtils.isSet(id.getId())){
			Host host = APILocator.getHostAPI().find(id.getHostId(), user, respectFrontendRoles);
			Folder folder = APILocator.getFolderAPI().findFolderByPath(id.getParentPath(), host, user, respectFrontendRoles);
			if(!fileNameExists(host, folder, newName, id.getId())){
			    FileAsset fa = fromContentlet(fileAssetCont);
			    if(fa.isLive())
					isfileAssetContLive = true;
				
				String ext = fa.getExtension();
				File oldFile = fileAssetCont.getBinary(BINARY_FIELD);
				File newFile = new File(oldFile.getPath().substring(0,oldFile.getPath().indexOf(oldFile.getName()))+newName+"."+ext);
				FileUtils.moveFile(oldFile, newFile);
				fileAssetCont.setInode(null);
				fileAssetCont.setFolder(folder.getInode());
				fileAssetCont.setBinary(BINARY_FIELD, newFile);
				final String newFileName=newName+"."+ext;
				fileAssetCont.setStringProperty(FileAssetAPI.TITLE_FIELD, newFileName);
				fileAssetCont.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, newFileName);
				fileAssetCont= APILocator.getContentletAPI().checkin(fileAssetCont, user, respectFrontendRoles);
				if(isfileAssetContLive)
					 APILocator.getVersionableAPI().setLive(fileAssetCont);
				
				LiveCache.removeAssetFromCache(fileAssetCont);
		    	LiveCache.addToLiveAssetToCache(fileAssetCont);
		    	WorkingCache.removeAssetFromCache(fileAssetCont);
		   		WorkingCache.addToWorkingAssetToCache(fileAssetCont);
		   		RefreshMenus.deleteMenu(folder);
		   		CacheLocator.getIdentifierCache().removeFromCacheByVersionable(fileAssetCont);
				return true;
			}
		}
		return false;
	}
	
	
	public  boolean moveFile (Contentlet fileAssetCont, Folder parent, User user, boolean respectFrontendRoles) throws DotStateException, DotDataException, DotSecurityException  {
		boolean isfileAssetContLive = false;
		Identifier id = APILocator.getIdentifierAPI().find(fileAssetCont);
		if(id!=null && InodeUtils.isSet(id.getId())){
			FileAsset fa = fromContentlet(fileAssetCont);
			if(fa.isLive())
				isfileAssetContLive = true;
			
			Host host = APILocator.getHostAPI().find(id.getHostId(), user, respectFrontendRoles);
			Folder oldParent = APILocator.getFolderAPI().findFolderByPath(id.getParentPath(), host, user, respectFrontendRoles);
			if(!fileNameExists(host, parent, fa.getFileName(), id.getId())){
				fileAssetCont.setInode(null);
				fileAssetCont.setFolder(parent.getInode());
				fileAssetCont = APILocator.getContentletAPI().checkin(fileAssetCont, user, respectFrontendRoles);
				if(isfileAssetContLive)
					 APILocator.getVersionableAPI().setLive(fileAssetCont);
				
				LiveCache.removeAssetFromCache(fileAssetCont);
				LiveCache.addToLiveAssetToCache(fileAssetCont);
				WorkingCache.removeAssetFromCache(fileAssetCont);
				WorkingCache.addToWorkingAssetToCache(fileAssetCont);
				RefreshMenus.deleteMenu(oldParent,parent);
				CacheLocator.getIdentifierCache().removeFromCacheByVersionable(fileAssetCont);
				return true;
			}
		}
		return false;
	}

	public List<FileAsset> findFileAssetsByFolder(Folder parentFolder,
			String sortBy, boolean live, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		List<FileAsset> assets = null;
		try{
			assets = fromContentlets(perAPI.filterCollection(contAPI.search("+structureType:" + Structure.STRUCTURE_TYPE_FILEASSET+" +conFolder:" + parentFolder.getInode() + (live?" +live:true":""), -1, 0, sortBy , user, respectFrontendRoles), 
					PermissionAPI.PERMISSION_READ, respectFrontendRoles, user));
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
		return assets;
	}
	
	public List<FileAsset> findFileAssetsByFolder(Folder parentFolder,
			String sortBy, boolean live, boolean working, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		List<FileAsset> assets = null;
		try{
			assets = fromContentlets(perAPI.filterCollection(contAPI.search("+structureType:" + Structure.STRUCTURE_TYPE_FILEASSET+" +conFolder:" + parentFolder.getInode() + (live?" +live:true":"") + (working? " +working:true":""), -1, 0, sortBy , user, respectFrontendRoles), 
					PermissionAPI.PERMISSION_READ, respectFrontendRoles, user));
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
		return assets;
	}
	
	public String getRealAssetPath(String inode, String fileName, String ext) {
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
                + java.io.File.separator + _inode+ java.io.File.separator + "fileAsset" + java.io.File.separator + fileName + "." + ext;

        if (!UtilMethods.isSet(realPath))
            return Config.CONTEXT.getRealPath(path);
        else
            return path;
    	
    }
	
	public String getRealAssetPath(String inode) {
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
                + java.io.File.separator + _inode+ java.io.File.separator + "fileAsset" + java.io.File.separator;

        if (!UtilMethods.isSet(realPath))
            return Config.CONTEXT.getRealPath(path);
        else
            return path;
    	
    }
	
	

	
	
	
}
