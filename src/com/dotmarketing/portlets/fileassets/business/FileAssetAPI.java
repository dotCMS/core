package com.dotmarketing.portlets.fileassets.business;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;


public interface FileAssetAPI {
	public static final String TITLE_FIELD = "title";
	public static final String FILE_NAME_FIELD = "fileName";
	public static final String DESCRIPTION = "description";
	public static final String SIZE_FIELD = "fileSize";
	public static final String BINARY_FIELD = "fileAsset";
	public static final String MIMETYPE_FIELD = "mimeType";
	public static final String HOST_FOLDER_FIELD = "hostFolder";
	public static final String SORT_ORDER = "sortOrder";
	public static final String SHOW_ON_MENU = "showOnMenu";
	public static final String META_DATA_FIELD = "metaData";
	public static final String CONTENT_FIELD = "content";
	public static final String URI_FIELD = "fileURI";
	public static final String TITLE_FIELD_NAME = "Title";
	public static final String FILE_NAME_FIELD_NAME = "File Name";
	public static final String DESCRIPTION_NAME = "Description";
	public static final String BINARY_FIELD_NAME = "File Asset";
	public static final String HOST_FOLDER_FIELD_NAME = "Host Or Folder";
	public static final String SORT_ORDER_NAME = "Sort Order";
	public static final String SHOW_ON_MENU_NAME = "Show On Menu";
	public static final String META_DATA_FIELD_NAME = "Metadata";
	public static final String META_DATA_TAB_NAME = "Metadata";
	public static final String DEFAULT_FILE_ASSET_STRUCTURE_NAME = "File Asset";
	public static final String DEFAULT_FILE_ASSET_STRUCTURE_DESCRIPTION = "Default structure for all uploaded files";
	public static final String DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME = "FileAsset";
	
	public void createBaseFileAssetFields(Structure structure) throws DotDataException,DotStateException;
	
    /**
     * 
     * @param con
     * @return
     * @throws DotStateException
     */
	public FileAsset fromContentlet(Contentlet con) throws DotStateException;
	
	/**
	 * 
	 * @param cons
	 * @return
	 * @throws DotStateException
	 */
	public List<FileAsset> fromContentlets(List<Contentlet> cons) throws DotStateException;
	
	/**
	 * 
	 * @param c
	 * @return
	 */
	public boolean isFileAsset(Contentlet c);
	
	/**
	 * Returns a map with the given binary file's meta data
	 * @param binFile
	 * @return
	 */
	public Map<String, String> getMetaDataMap(File binFile);
	
	/**
	 * 
	 * @param host
	 * @param folder
	 * @param fileName
	 * @param identifier
	 * @return
	 * @throws DotDataException
	 */
	
	/**
	 * 
	 * @param host
	 * @param folder
	 * @param fileName
	 * @param identifier
	 * @return
	 * @throws DotDataException
	 */
	public boolean fileNameExists(Host host, Folder folder, String fileName, String identifier) throws  DotDataException;
	
	/**
	 * 
	 * @param fa
	 * @return
	 */
	public String getRelativeAssetPath(FileAsset fa);
	
	/**
	 * 
	 * @param parentFolder
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<FileAsset> findFileAssetsByFolder(Folder parentFolder, User user, boolean respectFrontendRoles) throws DotDataException,DotSecurityException;
	
	
	/**
	 * 
	 * @param parentHost
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<FileAsset> findFileAssetsByHost(Host parentHost, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	
	/**
	 * 
	 * @param parentFolder
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<FileAsset> findFileAssetsByFolder(Folder parentFolder, String sortBy, boolean live, User user, boolean respectFrontendRoles) throws DotDataException,DotSecurityException;
	
	
	
	/**
	 * 
	 * @param parentFolder
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<FileAsset> findFileAssetsByFolder(Folder parentFolder, String sortBy, boolean live, boolean working, User user, boolean respectFrontendRoles) throws DotDataException,DotSecurityException;
	
	/**
	 * 
	 * @param fileAssetCont
	 * @param newName
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotStateException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws IOException
	 */
	public  boolean renameFile (Contentlet fileAssetCont, String newName, User user, boolean respectFrontendRoles) throws DotStateException, DotDataException, DotSecurityException, IOException;
	
	/**
	 * 
	 * @param fileAssetCont
	 * @param parent
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotStateException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public  boolean moveFile (Contentlet fileAssetCont, Folder parent, User user, boolean respectFrontendRoles) throws DotStateException, DotDataException, DotSecurityException;
	
    /**
     * 
     * @param inode
     * @param fileName
     * @param ext
     * @return
     */
	public String getRealAssetPath(String inode, String fileName, String ext);
	
	/**
	 * 
	 * @param inode
	 * @return
	 */
	public String getRealAssetPath(String inode); 
}
