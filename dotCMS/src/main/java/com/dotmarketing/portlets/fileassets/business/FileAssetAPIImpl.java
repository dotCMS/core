package com.dotmarketing.portlets.fileassets.business;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotcms.repackage.org.apache.commons.io.IOUtils;
import com.dotcms.repackage.org.apache.tika.Tika;
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
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
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
import com.liferay.util.FileUtil;

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

	/**
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
	
	public List<FileAsset> findFileAssetsByHost(Host parentHost, User user, boolean live, boolean working, boolean archived, boolean respectFrontendRoles) throws DotDataException,
	DotSecurityException {
		List<FileAsset> assets = null;
		try{
			Folder parentFolder = APILocator.getFolderAPI().find(FolderAPI.SYSTEM_FOLDER, user, false);
			assets = fromContentlets(perAPI.filterCollection(contAPI.search("+conHost:" +parentHost.getIdentifier() +" +structureType:" + Structure.STRUCTURE_TYPE_FILEASSET+" +conFolder:" + parentFolder.getInode() + (live?" +live:true":"") + (working? " +working:true":"") + (archived? " +deleted:true":""), -1, 0, null , user, respectFrontendRoles),
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
		if (con == null) {
			throw new DotStateException("Contentlet : is null");
		}
		if (con.getStructure().getStructureType() != Structure.STRUCTURE_TYPE_FILEASSET) {
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
				try{
					User systemUser = APILocator.getUserAPI().getSystemUser();
					Host host = APILocator.getHostAPI().find(con.getHost(), systemUser , false);
					Folder folder = APILocator.getFolderAPI().find(con.getFolder(), systemUser, false);
					fa.setFolder(folder.getInode());
				}catch(Exception e1){
					Logger.warn(this, "Unable to convert contentlet to file asset " + con, e1);
				}
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

	public List<IFileAsset> fromContentletsI(List<Contentlet> cons) {
		List<IFileAsset> fas = new ArrayList<IFileAsset>();
		for (Contentlet con : cons) {
			fas.add(fromContentlet(con));
		}
		return fas;

	}

	public boolean isFileAsset(Contentlet con)  {
		return (con != null && con.getStructure() != null && con.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_FILEASSET) ;
	}

	public Map<String, String> getMetaDataMap(Contentlet con, File binFile)  {

		return new TikaUtils().getMetaDataMap(con.getInode(),binFile,false);

	}

	public boolean fileNameExists(Host host, Folder folder, String fileName, String identifier) throws  DotDataException{
		return this.fileNameExists(host, folder, fileName, identifier, -1);
	}

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
			FileAsset fa = fromContentlet(fileAssetCont);
			String ext = fa.getExtension();
			if(!fileNameExists(host, folder, newName+ "." +ext, id.getId())){			    
			    if(fa.isLive()) {
					isfileAssetContLive = true;
			    }
			    File oldFile = fileAssetCont.getBinary(BINARY_FIELD);
				File newFile = new File(oldFile.getPath().substring(0,oldFile.getPath().indexOf(oldFile.getName()))+newName+"."+ext);
				try {
					FileUtils.copyFile(oldFile, newFile);
					fileAssetCont.setInode(null);
					fileAssetCont.setFolder(folder.getInode());
					fileAssetCont.setBinary(BINARY_FIELD, newFile);
					final String newFileName=newName+"."+ext;
					fileAssetCont.setStringProperty(FileAssetAPI.TITLE_FIELD, newFileName);
					fileAssetCont.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, newFileName);
					fileAssetCont= APILocator.getContentletAPI().checkin(fileAssetCont, user, respectFrontendRoles);
					if(isfileAssetContLive) {
						 APILocator.getVersionableAPI().setLive(fileAssetCont);
					}
					LiveCache.removeAssetFromCache(fileAssetCont);
					LiveCache.addToLiveAssetToCache(fileAssetCont);
					WorkingCache.removeAssetFromCache(fileAssetCont);
					WorkingCache.addToWorkingAssetToCache(fileAssetCont);
					RefreshMenus.deleteMenu(folder);
					CacheLocator.getNavToolCache().removeNav(folder.getHostId(), folder.getInode());
					CacheLocator.getIdentifierCache().removeFromCacheByVersionable(fileAssetCont);
				} catch (Exception e) {
					Logger.error(this, "Unable to rename file asset to "
							+ newName + " for asset " + id.getId(), e);
					throw e;
				} finally {
					if (newFile != null) {
						FileUtils.deleteQuietly(newFile);
					}
				}
				return true;
			}
		}
		return false;
	}


    public boolean moveFile ( Contentlet fileAssetCont, Host host, User user, boolean respectFrontendRoles ) throws DotStateException, DotDataException, DotSecurityException {
        return moveFile( fileAssetCont, null, host, user, respectFrontendRoles );
    }

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

                LiveCache.removeAssetFromCache( fileAssetCont );
                LiveCache.addToLiveAssetToCache( fileAssetCont );
                WorkingCache.removeAssetFromCache( fileAssetCont );
                WorkingCache.addToWorkingAssetToCache( fileAssetCont );
                if ( parent != null ) {
                    RefreshMenus.deleteMenu( oldParent, parent );
                    CacheLocator.getNavToolCache().removeNav(parent.getHostId(), parent.getInode());
                } else {
                    RefreshMenus.deleteMenu( oldParent );
                }
                CacheLocator.getNavToolCache().removeNav(oldParent.getHostId(), oldParent.getInode());
                
                CacheLocator.getIdentifierCache().removeFromCacheByVersionable( fileAssetCont );

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
            return FileUtil.getRealPath(path);
        else
            return path;

    }
	
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
        path = Config.getStringProperty("ASSET_PATH");
        return path;
    }

    /**
     * This method returns the root path for assets
     * 
     * @return the root folder of where assets are stored
     */
    public String getRealAssetsRootPath() {
        String realPath = Config.getStringProperty("ASSET_REAL_PATH");
        if (UtilMethods.isSet(realPath) && !realPath.endsWith(java.io.File.separator))
            realPath = realPath + java.io.File.separator;
        if (!UtilMethods.isSet(realPath))
            return FileUtil.getRealPath(getRelativeAssetsRootPath());
        else
            return realPath;
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
            return FileUtil.getRealPath(path);
        else
            return path;

    }

    @Override
    public File getContentMetadataFile(String inode) {
        return new File(APILocator.getFileAPI().getRealAssetsRootPath()+File.separator+
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

        String type=new Tika().detect(metadataFile);

        InputStream input=new FileInputStream(metadataFile);

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
                input.close();
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
            final String realAssetPath = APILocator.getFileAPI().getRealAssetPath();
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
}
