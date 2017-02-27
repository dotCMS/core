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
	String TITLE_FIELD = "title";
	String FILE_NAME_FIELD = "fileName";
	String DESCRIPTION = "description";
	String SIZE_FIELD = "fileSize";
	String BINARY_FIELD = "fileAsset";
	String MIMETYPE_FIELD = "mimeType";
	String HOST_FOLDER_FIELD = "hostFolder";
	String SORT_ORDER = "sortOrder";
	String SHOW_ON_MENU = "showOnMenu";
	String META_DATA_FIELD = "metaData";
	String CONTENT_FIELD = "content";
	String TITLE_FIELD_NAME = "Title";
	String FILE_NAME_FIELD_NAME = "File Name";
	String DESCRIPTION_NAME = "Description";
	String BINARY_FIELD_NAME = "File Asset";
	String HOST_FOLDER_FIELD_NAME = "Host Or Folder";
	String SORT_ORDER_NAME = "Sort Order";
	String SHOW_ON_MENU_NAME = "Show On Menu";
	String META_DATA_FIELD_NAME = "Metadata";
	String META_DATA_TAB_NAME = "Metadata";
	String DEFAULT_FILE_ASSET_STRUCTURE_NAME = "File Asset";
	String DEFAULT_FILE_ASSET_STRUCTURE_DESCRIPTION = "Default structure for all uploaded files";
	String DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME = "FileAsset";
	String DEFAULT_FILE_ASSET_STRUCTURE_INODE = "33888b6f-7a8e-4069-b1b6-5c1aa9d0a48d";

	void createBaseFileAssetFields(Structure structure) throws DotDataException,DotStateException;

    /**
     *
     * @param con
     * @return
     * @throws DotStateException
     */
	FileAsset fromContentlet(Contentlet con) throws DotStateException;

	/**
	 *
	 * @param cons
	 * @return
	 * @throws DotStateException
	 */
	List<FileAsset> fromContentlets(List<Contentlet> cons) throws DotStateException;

	/**
	 *
	 * @param cons
	 * @return
	 * @throws DotStateException
	 */
	List<IFileAsset> fromContentletsI(List<Contentlet> cons) throws DotStateException;

	/**
	 *
	 * @param c
	 * @return
	 */
	boolean isFileAsset(Contentlet c);

	/**
	 * Returns a map with the given binary file's meta data
	 * @param binFile
	 * @return
	 */
	Map<String, String> getMetaDataMap(Contentlet con, File binFile);

	/**
	 * Checks if the File Name already exists. Important: This method doesn't check for language of the File Asset.
	 *
	 * @param host
	 * @param folder
	 * @param fileName
	 * @param identifier
	 * @return
	 * @throws DotDataException
	 */
	boolean fileNameExists(Host host, Folder folder, String fileName, String identifier) throws  DotDataException;

	/**
	 * Checks if the File Name already exists. I verifies that the FileAsset found has the same language.
	 *
	 * @param host
	 * @param folder
	 * @param fileName
	 * @param identifier
	 * @param languageId
	 * @return
     * @throws DotDataException
     */
    boolean fileNameExists(Host host, Folder folder, String fileName, String identifier, long languageId) throws  DotDataException;

	/**
	 *
	 * @param fa
	 * @return
	 */
	String getRelativeAssetPath(FileAsset fa);

	/**
	 *
	 * @param parentFolder
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	List<FileAsset> findFileAssetsByFolder(Folder parentFolder, User user, boolean respectFrontendRoles) throws DotDataException,DotSecurityException;


	/**
	 *
	 * @param parentHost
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	List<FileAsset> findFileAssetsByHost(Host parentHost, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 *
	 * @param parentHost
	 * @param user
	 * @param respectFrontendRoles
	 * @param live
	 * @param working
	 * @param archived
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	List<FileAsset> findFileAssetsByHost(Host parentHost, User user, boolean live, boolean working, boolean archived, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;


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
     * Moves a given contentlet to a given folder
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
    public boolean moveFile ( Contentlet fileAssetCont, Folder parent, User user, boolean respectFrontendRoles ) throws DotStateException, DotDataException, DotSecurityException;

    /**
     * Moves a given contentlet to a given host
     *
     * @param fileAssetCont
     * @param host
     * @param user
     * @param respectFrontendRoles
     * @return
     * @throws DotStateException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public boolean moveFile ( Contentlet fileAssetCont, Host host, User user, boolean respectFrontendRoles ) throws DotStateException, DotDataException, DotSecurityException;

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

	String getRealAssetPath(String inode, String fileName); 
	
	/**
     * This method returns the relative path for assets
     * 
     * @return the relative folder of where assets are stored
     */
    public String getRelativeAssetsRootPath();

    /**
     * This method returns the root path for assets
     * 
     * @return the root folder of where assets are stored
     */
    public String getRealAssetsRootPath();
	
	/**
	 * constructs the file path for content metadata assetpath/inode(0)/inode(1)/inode/metaData/content
	 * 
	 * @param inode content inode
	 * @return
	 */
	File getContentMetadataFile(String inode);
	
	/**
	 * Takes the content metadata file and loads its content in a string.
	 * It handles compression gzip, bzip2 or none using Tika to detect it 
	 * based on the file header.
	 * 
	 * @param inode
	 * @return
	 */
	String getContentMetadataAsString(File metadataFile) throws Exception;

    /**
     * Cleans up thumbnails folder from a contentlet file asset, it uses the
     * identifier to remove the generated folder.
     * 
     * <p>
     * Note: the thumbnails are generated once, so when the image is updated
     * then we need to clean the old thumbnails; that way it will generate a new
     * one.
     * </p>
     * 
     * @param contentlet
     */
    public void cleanThumbnailsFromContentlet(Contentlet contentlet);

    /**
     * This method cleans thumbnails folder.
     * 
     * <p>
     * Note: the thumbnails are generated once, so when the image is updated
     * then we need to clean the old thumbnail; that way it will generate a new
     * one.
     * </p>
     * 
     * @param fileAsset
     */
    public void cleanThumbnailsFromFileAsset(IFileAsset fileAsset);

	public String getMimeType (String filename);

	public String getRealAssetPathTmpBinary();
}
