package com.dotmarketing.portlets.fileassets.business;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.api.system.event.verifier.ExcludeOwnerVerifierBean;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.rendering.velocity.viewtools.content.FileAssetMap;
import com.dotcms.repackage.org.apache.commons.io.IOUtils;
import com.dotcms.tika.TikaUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.menubuilders.RefreshMenus;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

/**
 * This class is a bridge impl that will support the older
 * com.dotmarketing.portlets.file.model.File as well as the new Contentlet based
 * files
 *
 * @author will
 *
 */
public class FileAssetAPIImpl implements FileAssetAPI {

    private final static String DEFAULT_RELATIVE_ASSET_PATH = "/assets";
	private final SystemEventsAPI systemEventsAPI;
	final ContentletAPI contAPI;
	final PermissionAPI perAPI;
	private final IdentifierAPI identifierAPI;

	public FileAssetAPIImpl() {

		contAPI = APILocator.getContentletAPI();
		perAPI = APILocator.getPermissionAPI();
		systemEventsAPI = APILocator.getSystemEventsAPI();
		identifierAPI   = APILocator.getIdentifierAPI();
	}

	/*
	 * This method will allow you to pass a file where the identifier is not set.  It the file exists on the set host/path
	 * the identifier and all necessary data will be set in order to checkin as a new version of the existing file. The method will
	 * call checkout for you so there is no need to do that work before calling this method
	 * @param fileCon
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException

	public FileAsset checkinFile(Contentlet fileCon, User user,boolean respectFrontendRoles) throws DotSecurityException, DotDataException {
		boolean isExisting = false;
		if(!UtilMethods.isSet(fileCon.getIdentifier())){
			APILocator.getIdentifierAPI().find(fileCon.getHost(),fileCon.getFolder()))
		}

		return fromContentlet(contAPI.checkin(fileCon,user,respectFrontendRoles));
	}
	 */
	@CloseDBIfOpened
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

	@CloseDBIfOpened
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

	@CloseDBIfOpened
	public List<FileAsset> findFileAssetsByHost(final Host parentHost, final User user, final boolean live,
												final boolean working, final boolean archived,
												final boolean respectFrontendRoles)
										throws DotDataException, DotSecurityException {

		List<FileAsset> assets = null;

		try {

			final Folder parentFolder = APILocator.getFolderAPI().find(FolderAPI.SYSTEM_FOLDER, user, true);
			assets = fromContentlets(perAPI.filterCollection
					(contAPI.search("+conHost:" +parentHost.getIdentifier() +" +structureType:" + Structure.STRUCTURE_TYPE_FILEASSET+" +conFolder:" + parentFolder.getInode() + (live?" +live:true":"") + (working? " +working:true":"") + (archived? " +deleted:true":""), -1, 0, null , user, respectFrontendRoles),
					PermissionAPI.PERMISSION_READ, respectFrontendRoles, user));
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}

		return assets;
	} // findFileAssetsByHost.

	@WrapInTransaction
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

	@CloseDBIfOpened
	public FileAsset fromContentlet(final Contentlet con) throws DotStateException {
		if (con == null || con.getInode() == null) {
			throw new DotStateException("Contentlet is null");
		}

		if (!con.isFileAsset()) {
			throw new DotStateException("Contentlet : " + con.getInode() + " is not a FileAsset");
		}

		if(con instanceof FileAsset) {
			return (FileAsset) con;
		}

		final FileAsset fileAsset = new FileAsset();
		fileAsset.setContentTypeId(con.getContentTypeId());
		try {
			contAPI.copyProperties(fileAsset, con.getMap());
		} catch (Exception e) {
			throw new DotStateException("Content -> FileAsset Copy Failed :" + e.getMessage(), e);
		}
		fileAsset.setHost(con.getHost());
		if(UtilMethods.isSet(con.getFolder())){
			try{
				final Identifier ident = APILocator.getIdentifierAPI().find(con);
				final Host host = APILocator.getHostAPI().find(con.getHost(), APILocator.systemUser() , false);
				final Folder folder = APILocator.getFolderAPI().findFolderByPath(ident.getParentPath(), host, APILocator.systemUser(), false);
				fileAsset.setFolder(folder.getInode());
			}catch(Exception e){
				try{
					final Folder folder = APILocator.getFolderAPI().find(con.getFolder(), APILocator.systemUser(), false);
					fileAsset.setFolder(folder.getInode());
				}catch(Exception e1){
					Logger.warn(this, "Unable to convert contentlet to file asset " + con, e1);
				}
			}
		}
		CacheLocator.getContentletCache().add(fileAsset);
		return fileAsset;
	}

	public List<FileAsset> fromContentlets(final List<Contentlet> contentlets) {
		final List<FileAsset> fileAssets = new ArrayList<>();
		for (Contentlet con : contentlets) {
			fileAssets.add(fromContentlet(con));
		}
		return fileAssets;

	}

	public List<IFileAsset> fromContentletsI(final List<Contentlet> contentlets) {
		final List<IFileAsset> fileAssets = new ArrayList<IFileAsset>();
		for (Contentlet con : contentlets) {
			fileAssets.add(fromContentlet(con));
		}
		return fileAssets;

	}

	@CloseDBIfOpened
	public FileAssetMap fromFileAsset(final FileAsset fileAsset) throws DotStateException {
		if (!fileAsset.isLoaded()) {
		    //Force to pre-load
			fileAsset.load();
		}
		try {
			final FileAssetMap fileAssetMap = new FileAssetMap(fileAsset);
			CacheLocator.getContentletCache().add(fileAsset);
			// We cache the original contentlet that was forced to pre-load its values. That's the state we want to maintain.
			return fileAssetMap;
		} catch (Exception e) {
			throw new DotStateException(e);
		}
	}

	public boolean isFileAsset(Contentlet con)  {
		return (con != null && con.getStructure() != null && con.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_FILEASSET) ;
	}

	public Map<String, String> getMetaDataMap(Contentlet contentlet, final File binFile)
			throws DotDataException {
		return new TikaUtils().getMetaDataMap(contentlet.getInode(), binFile);
	}

	@CloseDBIfOpened
	public boolean fileNameExists(final Host host, final Folder folder, final String fileName, final String identifier)
			throws DotDataException {
		if (!UtilMethods.isSet(fileName)) {
			return true;
		}

		if (folder == null || host == null) {
			return false;
		}

		final Identifier folderId = APILocator.getIdentifierAPI().find(folder);
		final String path =
				folder.getInode().equals(FolderAPI.SYSTEM_FOLDER) ? StringPool.FORWARD_SLASH
						+ fileName : folderId.getPath() + fileName;
		final Identifier fileAssetIdentifier = APILocator.getIdentifierAPI().find(host, path);
		if (null == fileAssetIdentifier || !InodeUtils.isSet(fileAssetIdentifier.getId())
				|| "folder".equals(fileAssetIdentifier.getAssetType())) {
			// if we're looking at a folder or the fileAssetIdentifier wasn't found. It doesn't exist for sure.
			return false;
		}
		//Beyond this point we know something matches the path for that host.
		if (!UtilMethods.isSet(identifier)) {
			//it's a brand new contentlet we're dealing with
			//At this point we know it DOES exist, and since we're dealing with a fresh contentlet that hasn't even been inserted yet (We don't need to worry about lang).
			return true;
		} else {
			// Now we have an identifier and a lang.
			// if the file-asset identifier is different from the contentlet identifier we're looking at. Then it does exist already.
			return !identifier.equals(fileAssetIdentifier.getId());
		}
	}

	@CloseDBIfOpened
	@Override
	public boolean fileNameExists(final Host host, final Folder folder, final String fileName) throws  DotDataException {

		if(!UtilMethods.isSet(fileName) || folder == null || host == null ) {
			return false;
		}


		final Identifier folderId  = this.identifierAPI.find(folder);
		final String path          = folder.getInode().equals(FolderAPI.SYSTEM_FOLDER)?
				new StringBuilder(StringPool.FORWARD_SLASH).append(fileName).toString():
				new StringBuilder(folderId.getPath()).append(fileName).toString();
		final Identifier fileAsset = this.identifierAPI.find(host, path);

		return (fileAsset!=null && InodeUtils.isSet(fileAsset.getId())  && !fileAsset.getAssetType().equals(Contentlet.FOLDER_KEY));
	} // fileNameExists.

	@CloseDBIfOpened
	public boolean fileNameExists(Host host, Folder folder, String fileName, String identifier, long languageId) throws  DotDataException{
		if( !UtilMethods.isSet(fileName) ){
			return true;
		}

		if( folder == null || host == null ) {
			return false;
		}

		boolean exist = false;

		Identifier folderId = APILocator.getIdentifierAPI().find(folder);
		String path = folder.getInode().equals(FolderAPI.SYSTEM_FOLDER)?"/"+fileName:folderId.getPath()+fileName;
		Identifier fileAsset = APILocator.getIdentifierAPI().find(host, path);

		if(fileAsset!=null && InodeUtils.isSet(fileAsset.getId()) && !identifier.equals(fileAsset.getId()) && !fileAsset.getAssetType().equals("folder")){
			// Let's not break old logic. ie calling fileNameExists method without languageId parameter.
			if (languageId == -1){
				exist = true;
			} else { // New logic.
				//We need to make sure that the contentlets for this identifier have the same language.
				try {
					contAPI.findContentletByIdentifier(fileAsset.getId(), false, languageId,
						APILocator.getUserAPI().getSystemUser(), false);
					exist = true;
				} catch (DotSecurityException dse) {
					// Something could failed, lets log and assume true to not break anything.
					Logger.error(FileAssetAPIImpl.class,
						"Error trying to find contentlet from identifier:" + fileAsset.getId(), dse);
					exist = true;
				} catch (DotContentletStateException dcse){
					// DotContentletStateException is thrown when content is not found.
					exist = false;
				}
			}
		}
		return exist;
    }

	public String getRelativeAssetPath(FileAsset fa) {
		String _inode = fa.getInode();
		return getRelativeAssetPath(_inode, fa.getUnderlyingFileName());
	}

	private  String getRelativeAssetPath(String inode, String fileName) {
		String _inode = inode;
		String path = "";

		path = java.io.File.separator + _inode.charAt(0)
				+ java.io.File.separator + _inode.charAt(1) + java.io.File.separator + _inode + java.io.File.separator + "fileAsset" + java.io.File.separator+ fileName;

		return path;

	}

	@CloseDBIfOpened
	public  boolean renameFile (Contentlet fileAssetCont, String newName, User user, boolean respectFrontendRoles) throws DotStateException, DotDataException, DotSecurityException, IOException {
		boolean isfileAssetContLive = false;
		Identifier id = APILocator.getIdentifierAPI().find(fileAssetCont);
		if(id!=null && InodeUtils.isSet(id.getId())){
			Host host = APILocator.getHostAPI().find(id.getHostId(), user, respectFrontendRoles);
			Folder folder = APILocator.getFolderAPI().findFolderByPath(id.getParentPath(), host, user, respectFrontendRoles);
			FileAsset fa = fromContentlet(fileAssetCont);
			String ext = fa.getExtension();
			if(!fileNameExists(host, folder, newName+ "." +ext, id.getId())){
			    if(fa.isLive()) {
					isfileAssetContLive = true;
			    }
				try {
					fileAssetCont.setInode(null);
					fileAssetCont.setFolder(folder.getInode());
					final String newFileName = newName + "." + ext;
					fileAssetCont.setStringProperty(FileAssetAPI.TITLE_FIELD, newFileName);
					fileAssetCont.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, newFileName);
					fileAssetCont= APILocator.getContentletAPI().checkin(fileAssetCont, user, respectFrontendRoles);
					if(isfileAssetContLive) {
						 APILocator.getVersionableAPI().setLive(fileAssetCont);
					}

					CacheLocator.getNavToolCache().removeNav(folder.getHostId(), folder.getInode());
					CacheLocator.getIdentifierCache().removeFromCacheByVersionable(fileAssetCont);
				} catch (Exception e) {
					Logger.error(this, "Unable to rename file asset to "
							+ newName + " for asset " + id.getId(), e);
					throw e;
				}
				return true;
			}
		}
		return false;
	}


	@WrapInTransaction
    public boolean moveFile ( Contentlet fileAssetCont, Host host, User user, boolean respectFrontendRoles ) throws DotStateException, DotDataException, DotSecurityException {
        return moveFile( fileAssetCont, null, host, user, respectFrontendRoles );
    }

	@WrapInTransaction
    public  boolean moveFile (Contentlet fileAssetCont, Folder parent, User user, boolean respectFrontendRoles) throws DotStateException, DotDataException, DotSecurityException  {
        return moveFile( fileAssetCont, parent, null, user, respectFrontendRoles );
    }

    private boolean moveFile ( Contentlet fileAssetCont, Folder parent, Host host, User user, boolean respectFrontendRoles ) throws DotStateException, DotDataException, DotSecurityException {

        boolean isfileAssetContLive = false;

        //Getting the contentlet identifier
        Identifier id = APILocator.getIdentifierAPI().find( fileAssetCont );
        if ( id != null && InodeUtils.isSet( id.getId() ) ) {

            FileAsset fa = fromContentlet( fileAssetCont );
            if ( fa.isLive() )
                isfileAssetContLive = true;

            if ( host == null ) {
                host = APILocator.getHostAPI().find( id.getHostId(), user, respectFrontendRoles );
            }

            //Verify if the file already exist
            Boolean fileNameExists;
            if ( parent != null ) {
                fileNameExists = fileNameExists( host, parent, fa.getFileName(), id.getId() );
            } else {
                fileNameExists = fileNameExists( host, APILocator.getFolderAPI().findSystemFolder(), fa.getFileName(), id.getId() );
            }

            if ( !fileNameExists ) {

                Folder oldParent = APILocator.getFolderAPI().findFolderByPath( id.getParentPath(), host, user, respectFrontendRoles );

                fileAssetCont.setInode( null );
                fileAssetCont.setHost( host != null ? host.getIdentifier() : (parent != null ? parent.getHostId() : fileAssetCont.getHost()) );
                fileAssetCont.setFolder( parent != null ? parent.getInode() : null );
                fileAssetCont = APILocator.getContentletAPI().checkin( fileAssetCont, user, respectFrontendRoles );
                if ( isfileAssetContLive )
                    APILocator.getVersionableAPI().setLive( fileAssetCont );

                if ( parent != null ) {
                    RefreshMenus.deleteMenu( oldParent, parent );
                    CacheLocator.getNavToolCache().removeNav(parent.getHostId(), parent.getInode());
                } else {
                    RefreshMenus.deleteMenu( oldParent );
                }
                CacheLocator.getNavToolCache().removeNav(oldParent.getHostId(), oldParent.getInode());

                CacheLocator.getIdentifierCache().removeFromCacheByVersionable( fileAssetCont );

				this.systemEventsAPI.pushAsync(SystemEventType.MOVE_FILE_ASSET, new Payload(fileAssetCont, Visibility.EXCLUDE_OWNER,
						new ExcludeOwnerVerifierBean(user.getUserId(), PermissionAPI.PERMISSION_READ, Visibility.PERMISSION)));

                return true;
            }
        }

        return false;
    }

    @CloseDBIfOpened
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

	@CloseDBIfOpened
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

  @Override
  @CloseDBIfOpened
  public FileAsset find(final String inode, final User user, final boolean respectFrontendRoles)
      throws DotDataException, DotSecurityException {

    return fromContentlet(contAPI.find(inode, user, respectFrontendRoles));

  }
	
	
	public String getRealAssetPath(String inode, String fileName, String ext) {
        String _inode = inode;
        String path = "";

        String realPath = Config.getStringProperty("ASSET_REAL_PATH");
        if (UtilMethods.isSet(realPath) && !realPath.endsWith(java.io.File.separator))
            realPath = realPath + java.io.File.separator;

        String assetPath = Config.getStringProperty("ASSET_PATH", DEFAULT_RELATIVE_ASSET_PATH);
        if (UtilMethods.isSet(assetPath) && !assetPath.endsWith(java.io.File.separator))
            assetPath = assetPath + java.io.File.separator;

        path = ((!UtilMethods.isSet(realPath)) ? assetPath : realPath)
                + _inode.charAt(0) + java.io.File.separator + _inode.charAt(1)
                + java.io.File.separator + _inode+ java.io.File.separator + "fileAsset" + java.io.File.separator + fileName + "." + ext;

        if (!UtilMethods.isSet(realPath))
            return FileUtil.getRealPath(path);
        else
            return path;

    }

	/**
	 * Returns the file on the filesystem that backup the fileAsset
	 * @param inode
	 * @param fileName generally speaking this method is expected to be called using the Underlying File Name property
	 * e.g.   getRealAssetPath(inode, fileAsset.getUnderlyingFileName())
	 * @return
	 */
	@Override
	public String getRealAssetPath(String inode, String fileName) {

		String extension = UtilMethods.getFileExtension(fileName);
		String fileNameWOExtenstion  =  UtilMethods.getFileName(fileName);


        return getRealAssetPath(inode, fileNameWOExtenstion, extension);

    }

	/**
	 * This method returns the relative path for assets
     *
     * @return the relative folder of where assets are stored
	 */
	public String getRelativeAssetsRootPath() {
        String path = "";
        path = Try.of(() -> Config.getStringProperty("ASSET_PATH", DEFAULT_RELATIVE_ASSET_PATH))
                .getOrElse(DEFAULT_RELATIVE_ASSET_PATH);
        return path;
    }

    /**
     * This method returns the root path for assets
     *
     * @return the root folder of where assets are stored
     */
    public String getRealAssetsRootPath() {
        return ConfigUtils.getAbsoluteAssetsRootPath();
    }

	public String getRealAssetPath(String inode) {
        String _inode = inode;
        String path = "";

        String realPath = Config.getStringProperty("ASSET_REAL_PATH", null);
        if (UtilMethods.isSet(realPath) && !realPath.endsWith(java.io.File.separator))
            realPath = realPath + java.io.File.separator;

        String assetPath = Config.getStringProperty("ASSET_PATH", DEFAULT_RELATIVE_ASSET_PATH);
        if (UtilMethods.isSet(assetPath) && !assetPath.endsWith(java.io.File.separator))
            assetPath = assetPath + java.io.File.separator;

        path = ((!UtilMethods.isSet(realPath)) ? assetPath : realPath)
                + _inode.charAt(0) + java.io.File.separator + _inode.charAt(1)
                + java.io.File.separator + _inode+ java.io.File.separator + "fileAsset" + java.io.File.separator;

        if (!UtilMethods.isSet(realPath))
            return FileUtil.getRealPath(path);
        else
            return path;

    }

    @Override
    public File getContentMetadataFile(String inode) {
        return new File(getRealAssetsRootPath()+File.separator+
                inode.charAt(0)+File.separator+inode.charAt(1)+File.separator+inode+File.separator+
                "metaData"+File.separator+"content");
    }

    @Override
    public String getContentMetadataAsString(File metadataFile) throws Exception {
        Logger.debug(this.getClass(), "DEBUG --> Parsing Metadata from file: " + metadataFile.getPath() );

        //Check if Metadata Max Size is set (in Bytes)
        int metadataLimitInBytes = Config.getIntProperty("META_DATA_MAX_SIZE", 5) * 1024 * 1024;

        //If Max Size limit is greater than what Java allows for Int values
        if(metadataLimitInBytes > Integer.MAX_VALUE){
            metadataLimitInBytes = Integer.MAX_VALUE;
        }

        //Subtracting 1024 Bytes (buffer size)
        metadataLimitInBytes = metadataLimitInBytes - 1024;

		String type = new TikaUtils().detect(metadataFile);

        InputStream input= Files.newInputStream(metadataFile.toPath());

        if(type.equals("application/x-gzip")) {
            // gzip compression was used
            input = new GZIPInputStream(input);
        }
        else if(type.equals("application/x-bzip2")) {
            // bzip2 compression was used
            input = new BZip2CompressorInputStream(input);
        }

        //Depending on the max limit of the metadata file size,
        //we'll get as many bytes as we can so we can parse it
        //and then they'll be added to the ContentMap
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] buf = new byte[1024];
        int bytesRead = 0;
        int copied = 0;

        while (bytesRead < metadataLimitInBytes && (copied = input.read(buf,0,buf.length)) > -1 ) {
            baos.write(buf,0,copied);
            bytesRead = bytesRead + copied;

        }

        InputStream limitedInput = new ByteArrayInputStream(baos.toByteArray());

        //let's close the original input since it's no longer necessary to keep it open
        if (input != null) {
            try {
                input.close(); // todo: the file resource close handling for io should be on try catch
            } catch (IOException e) {
                 Logger.error(this.getClass(), "There was a problem with parsing a file Metadata: " + e.getMessage(), e);
            }
       }

        return IOUtils.toString(limitedInput);
    }

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
    public void cleanThumbnailsFromContentlet(Contentlet contentlet) {
        if (contentlet.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_FILEASSET) {
            this.cleanThumbnailsFromFileAsset(APILocator.getFileAssetAPI().fromContentlet(
                    contentlet));
            return;
        }

        Logger.warn(this, "Contentlet parameter is NOT a fileasset.");
    }

    /**
     * Cleans up thumbnails folder for an specific asset, it uses the identifier
     * to remove the generated folder.
     *
     * <p>
     * Note: the thumbnails are generated once, so when the image is updated
     * then we need to clean the old thumbnails; that way it will generate a new
     * one.
     * </p>
     *
     * @param fileAsset
     */
    public void cleanThumbnailsFromFileAsset(IFileAsset fileAsset) {
        // Wiping out the thumbnails and resized versions
        // http://jira.dotmarketing.net/browse/DOTCMS-5911
        final String inode = fileAsset.getInode();
        if (UtilMethods.isSet(inode)) {
            final String realAssetPath = getRealAssetsRootPath();
            java.io.File tumbnailDir = new java.io.File(realAssetPath + java.io.File.separator
                    + "dotGenerated" + java.io.File.separator + inode.charAt(0)
                    + java.io.File.separator + inode.charAt(1));
            if (tumbnailDir != null) {
                java.io.File[] files = tumbnailDir.listFiles();
                if (files != null) {
                    for (java.io.File iofile : files) {
                        try {
                            if (iofile.getName().startsWith("dotGenerated_")) {
                                iofile.delete();
                            }
                        } catch (SecurityException e) {
                            Logger.error(
                                    this,
                                    "EditFileAction._saveWorkingFileData(): "
                                            + iofile.getName()
                                            + " cannot be erased. Please check the file permissions.");
                        } catch (Exception e) {
                            Logger.error(this,
                                    "EditFileAction._saveWorkingFileData(): " + e.getMessage());
                        }
                    }
                }
            }
        }
    }

	public String getMimeType(String filename) {
		if (filename != null) {
			filename = filename.toLowerCase();
		}

		String mimeType;

		try {
			mimeType = Config.CONTEXT.getMimeType(filename);
			if(!UtilMethods.isSet(mimeType))
				mimeType = FileAsset.UNKNOWN_MIME_TYPE;
		}
		catch(Exception ex) {
			mimeType = FileAsset.UNKNOWN_MIME_TYPE;
			Logger.warn(this,"Error looking for mimetype on file: "+filename,ex);
		}

		return mimeType;
	}

	public String getRealAssetPathTmpBinary() {

		java.io.File adir=new java.io.File(getRealAssetsRootPath() +java.io.File.separator+"tmp_upload");
		if(!adir.isDirectory())
			adir.mkdirs();

		return adir.getPath();
	}

}
