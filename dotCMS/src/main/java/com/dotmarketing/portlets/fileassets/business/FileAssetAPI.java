package com.dotmarketing.portlets.fileassets.business;

import com.dotcms.api.tree.Parentable;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.rendering.velocity.viewtools.content.FileAssetMap;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderListener;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;


public interface FileAssetAPI {
	String TITLE_FIELD = "title";
	String FILE_NAME_FIELD = "fileName";
	String UNDERLYING_FILENAME = "underlyingFileName";
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



	public List<FileAsset> findFileAssetsByParentable(final Parentable parent,
			final String sortBy, final boolean working, final boolean archived,
			final User user, final boolean respectFrontendRoles);

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
	 * @param fileAsset
	 * @return
	 * @throws DotStateException
	 */
	FileAssetMap fromFileAsset(final FileAsset fileAsset) throws DotStateException;

	/**
	 *
	 * @param c
	 * @return
	 */
	boolean isFileAsset(Contentlet c);

	/**
	 * This method takes a file and uses tika to parse the metadata from it. It
	 * returns a Map of the metadata and creates a metadata file for the given
	 * Contentlet if does not already exist, if already exist only the metadata is returned and no
	 * file is override.
	 *
	 * @param contentlet Contentlet owner of the file to parse
	 * @param binFile File to parse the metadata from it
	 */
	@Deprecated
	Map<String, String> getMetaDataMap(Contentlet contentlet, File binFile) throws DotDataException;

	/**
	 * Checks if the File Name already exists. Important: This method doesn't check for language of the File Asset and do not double check identifier.
	 *
	 * @param host
	 * @param folder
	 * @param fileName
	 * @return boolean true if exists
	 * @throws DotDataException
	 */
	boolean fileNameExists(Host host, Folder folder, String fileName) throws  DotDataException;


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
    @Deprecated
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
	 * Updates the system asset-Name
	 * We no longer can rename the physical file itself. Only the asset-name we use to refer to the physical chunk of bytes.
	 * @param fileAssetCont The Contentlet with the current FileAsset
	 * @param newName The New Asset Name
	 * @param user Current user executing the rename operation.
	 * @param respectFrontendRoles system flag
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

	/**
	 * Returns the file on the filesystem that backup the fileAsset
	 * @param inode
	 * @param fileName generally speaking this method is expected to be called using the Underlying File Name property
	 * e.g.   getRealAssetPath(inode, fileAsset.getUnderlyingFileName())
	 * @return
	 */
	String getRealAssetPath(String inode, String fileName);

	/**
	 * This method returns the file on the filesystem that backup the fileAsset ignoring the case of the extension
	 *
	 * @param inode
	 * @param fileName
	 * @return the real path of the asset
	 */
	String getRealAssetPathIgnoreExtensionCase(String inode, String fileName);
	
	/**
     * This method returns the relative path for assets
     * 
     * @return the relative folder of where assets are stored
     */
    public String getRelativeAssetsRootPath();

    /**
     * This method returns the root path for assets
     * @deprecated use ConfigUtils.getAbsoluteAssetsRootPath()
     * @return the root folder of where assets are stored
     */
    public String getRealAssetsRootPath();
	
	/**
	 * constructs the file path for content metadata assetpath/inode(0)/inode(1)/inode/metaData/content
	 * @deprecated
	 * This method is Here for compatibility purposes.
	 *    <p> Use {@link Contentlet#getBinaryMetadata(Field)} }
	 *    or {@link FileAsset#getMetaDataMap()} instead.
	 * @param inode content inode
	 * @return
	 */
	@Deprecated
	File getContentMetadataFile(String inode);

	/**
	 * constructs the file path for content metadata assetpath/inode(0)/inode(1)/inode/{fileName}
	 * @deprecated
	 * This method is Here for compatibility purposes.
	 *    <p> Use {@link Contentlet#getBinaryMetadata(Field)} }
	 *    or {@link FileAsset#getMetaDataMap()} instead.
	 * @param inode    {@link String } content inode
	 * @param fileName {@link String}  fileName for the metadata
	 * @return File
	 */
	@Deprecated
	File getContentMetadataFile(String inode, String fileName);
	
	/**
	 * Takes the content metadata file and loads its content in a string.
	 * It handles compression gzip, bzip2 or none using Tika to detect it 
	 * based on the file header.
	 *
	 * @deprecated
	 * This method is Here for compatibility purposes.
	 *    <p> Use {@link Contentlet#getBinaryMetadata(Field)} }
	 *    or {@link FileAsset#getMetaDataMap()} instead.
	 *
	 * @param metadataFile
	 * @return
	 */
	 @Deprecated
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

	/**
	 * Tries to determine the mime type from a since file path, @{@link FileAsset#UNKNOWN_MIME_TYPE} if not found
	 * For a more powerful but also more expensive version see {@link #getMimeType(File)}
	 * @param filename {@link String}
	 * @return String mime type
	 */
	public String getMimeType (String filename);

	/**
	 * Tries to determine the mime type from a binary,  @{@link FileAsset#UNKNOWN_MIME_TYPE} if not found
	 * This is a more poweful and also more expensive version of {@link #getMimeType(String)}
	 * since it uses more methods to figure out/fallbacks the mime type
	 * @param binary {@link File}
	 * @return String
	 */
	public String getMimeType (final File binary);

	/**
	 * @deprecated use ConfigUtils.getAssetTempPath()
	 * @return
	 */
	public String getRealAssetPathTmpBinary();

	/**
	 * this returns a fileAsset for a given inode - if the inode is in cache as a FileAsset, then this method will return it
	 * If the inode is in cache as a contentlet, then it will be converted to a FileAsset, re-added to cache and return it
	 * Otherwise, it will throw a DotStateException that the FileAsset was not found
	 * @param inode
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	FileAsset find(String inode, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;


    
    /**
     * Takes a {@link FileAssetSearcher} searcher object and returns fileAssets based on it. You can build a new
     * searcher using a builder , e.g.
     * FileAssetSearcher searcher = FileAssetSearcher.builder()
     * .folder(parent)
     * .user(user)
     * .respectFrontendRoles(true)
     * .build()
     * 
     * @param searcher
     * @return
     */
    List<FileAsset> findFileAssetsByDB(FileAssetSearcher searcher);

	/**
	 * Use a fileNamePattern to do the filter for the listener
	 * @param fileListener {@link String}
	 * @param fileNamePattern {@link FileListener}
	 */
	void subscribeFileListener (final FileListener fileListener, final String fileNamePattern);

	/**
	 * USe a Predicate to filter the file aset
	 * @param fileListener {@link Predicate} of {@link FileAsset}
	 * @param fileAssetFilter {@link FileListener}
	 */
	void subscribeFileListener (final FileListener fileListener, final Predicate<FileAsset> fileAssetFilter);

	/**
	 * Finds a File Asset by Path
	 * @param uri
	 * @param site
	 * @param languageId
	 * @param live
	 * @return
	 */
	FileAsset getFileByPath(String uri, Host site, long languageId, boolean live);
}
