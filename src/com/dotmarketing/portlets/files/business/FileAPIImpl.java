package com.dotmarketing.portlets.files.business;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.velocity.runtime.resource.ResourceManager;

import com.dotcms.enterprise.cmis.QueryResult;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.BaseWebAssetAPI;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.business.query.QueryUtil;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.menubuilders.RefreshMenus;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.business.ChildrenCondition;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.velocity.DotResourceCache;
import com.liferay.portal.model.User;

public class FileAPIImpl extends BaseWebAssetAPI implements FileAPI {

	private PermissionAPI permissionAPI;

	private IdentifierAPI identifierAPI;
	private FolderAPI folderAPI;
	private VersionableAPI vapi;
	private FileFactory ffac;
	private FileCache fcache;
	
	public FileAPIImpl() {
		permissionAPI = APILocator.getPermissionAPI();
		ffac = FactoryLocator.getFileFactory();
		identifierAPI = APILocator.getIdentifierAPI();
		folderAPI = APILocator.getFolderAPI();
		vapi = APILocator.getVersionableAPI();
		fcache = CacheLocator.getFileCache();
	}

	public File copy(File source, Folder destination, boolean forceOverwrite, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		
		if(!isLegacyFilesSupported()){
			throw new DotStateException("File Assets have been disabled.");
		}

		if (!permissionAPI.doesUserHavePermission(source, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to read the source file.");
		}

		if (!permissionAPI.doesUserHavePermission(destination, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to write in the destination folder.");
		}

		// gets filename before extension
		String fileName = UtilMethods.getFileName(source.getFileName());
		// gets file extension
		String fileExtension = UtilMethods.getFileExtension(source.getFileName());

		boolean isNew = false;
		File newFile;
		if (forceOverwrite) {
			newFile = getWorkingFileByFileName(source.getFileName(), destination, user, respectFrontendRoles);
			if (newFile == null) {
				isNew = true;
			}
		} else {
			isNew = true;
		}

		try {
			newFile = new File();
			newFile.copy(source);

			// Setting file name
			if (!forceOverwrite) {
				newFile.setFileName(getCopyFileName(fileName, fileExtension, destination));

				if (!UtilMethods.getFileName(newFile.getFileName()).equals(fileName))
					newFile.setFriendlyName(source.getFriendlyName() + " (COPY) ");
			}

			if (isNew) {
				// persists the webasset
				java.io.File sourceFile = getAssetIOFile(source);
				java.io.File uploadedFile = java.io.File.createTempFile(fileName, "." + fileExtension);
				FileUtils.copyFile(sourceFile, uploadedFile);

				newFile = saveFile(newFile, uploadedFile, destination, user, respectFrontendRoles);
				if(source.isLive())
				    APILocator.getVersionableAPI().setLive(newFile);

				//saveFileData(source, newFile, null);

				// Adding to the parent folder
				// TreeFactory.saveTree(new Tree(destination.getInode(),
				// newFile.getInode()));

				// creates new identifier for this webasset and persists it
				//Identifier newIdentifier = APILocator.getIdentifierAPI().createNew(newFile, destination);

				//Logger.debug(FileFactory.class, "identifier=" + newIdentifier.getURI());
			} else {
				java.io.File sourceFile = getAssetIOFile(source);
				java.io.File uploadedFile = java.io.File.createTempFile(fileName, "." + fileExtension);
				FileUtils.copyFile(sourceFile, uploadedFile);

				newFile = saveFile(newFile, uploadedFile, destination, user, respectFrontendRoles);
			}
			// Copy permissions
			permissionAPI.copyPermissions(source, newFile);

			//save(newFile);
		} catch (Exception e) {
			throw new DotRuntimeException("An error ocurred trying to copy the file.", e);
		}

		return newFile;
	}


	public File getWorkingFileByFileName(String fileName, Folder folder, User user, boolean respectFrontendRoles) throws DotStateException, DotDataException, DotSecurityException {
		File f = ffac.getWorkingFileByFileName(fileName, folder);
		if(!InodeUtils.isSet(f.getInode())){
			return null;
		}
		if (!permissionAPI.doesUserHavePermission(f, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to read the  file.");
		}
		return f;
	}


	private String getCopyFileName(String fileName, String fileExtension, Folder folder) throws DotStateException, DotDataException,
			DotSecurityException {
		String result = new String(fileName);

		List<File> files = APILocator.getFolderAPI().getFiles(folder, APILocator.getUserAPI().getSystemUser(), false);
		boolean isValidFileName = false;
		String temp;

		while (!isValidFileName) {
			isValidFileName = true;
			temp = result + "." + fileExtension;

			for (File file : files) {
				if (file.getFileName().equals(temp)) {
					isValidFileName = false;

					break;
				}
			}

			if (!isValidFileName)
				result += "_copy";
			else
				result = temp;
		}

		return result;
	}



	protected void save(WebAsset file) throws DotDataException {
		throw new DotStateException("This method is not applicable for Files, use saveFile");
	}

	public String getRelativeAssetsRootPath() {
		String path = "";
		path = Config.getStringProperty("ASSET_PATH");
		return path;
	}
	/**
	 * returns the root folder of where assets are stored
	 */
	public String getRealAssetsRootPath() {
		String realPath = Config.getStringProperty("ASSET_REAL_PATH");
		if (UtilMethods.isSet(realPath) && !realPath.endsWith(java.io.File.separator))
			realPath = realPath + java.io.File.separator;
		if (!UtilMethods.isSet(realPath))
			return Config.CONTEXT.getRealPath(getRelativeAssetsRootPath());
		else
			return realPath;
	}

	public java.io.File getAssetIOFile(File file) throws IOException {


		String suffix = UtilMethods.getFileExtension(file.getFileName());


		String assetsPath = getRealAssetsRootPath();
		String fileInode = file.getInode();

		// creates the path where to save the working file based on the inode
		String fileFolderPath = String.valueOf(fileInode);
		if (fileFolderPath.length() == 1) {
			fileFolderPath = fileFolderPath + "0";
		}

		fileFolderPath = assetsPath + java.io.File.separator + fileFolderPath.substring(0, 1) + java.io.File.separator
				+ fileFolderPath.substring(1, 2);

		new java.io.File(fileFolderPath).mkdirs();

		String filePath = fileFolderPath + java.io.File.separator + fileInode + "." + suffix;

		// creates the new file as
		// inode{1}/inode{2}/inode.file_extension
		java.io.File assetFile = new java.io.File(filePath);
		if (!assetFile.exists())
			assetFile.createNewFile();

		return assetFile;
	}

	protected void saveFileData(File file, File destination, java.io.File newDataFile) throws DotDataException, IOException {
		
		if(!isLegacyFilesSupported()){
			throw new DotStateException("File Assets have been disabled.");
		}

		String fileName = file.getFileName();

		String assetsPath = getRealAssetsRootPath();
		new java.io.File(assetsPath).mkdir();

		// creates the new file as
		// inode{1}/inode{2}/inode.file_extension
		java.io.File workingFile = getAssetIOFile(file);

		// To clear velocity cache
		DotResourceCache vc = CacheLocator.getVeloctyResourceCache();
		vc.remove(ResourceManager.RESOURCE_TEMPLATE + workingFile.getPath());

		// If a new version was created, we move the current data to the new
		// version
		if (destination != null && InodeUtils.isSet(destination.getInode())) {
			java.io.File newVersionFile = getAssetIOFile(destination);
			FileUtils.copyFile(workingFile, newVersionFile);
		}

		if (newDataFile != null) {
			// Saving the new working data
			FileUtils.copyFile(newDataFile, workingFile);

			// checks if it's an image
			if (UtilMethods.isImage(fileName)) {

				// gets image height
				BufferedImage img = javax.imageio.ImageIO.read(workingFile);
				int height = img.getHeight();
				file.setHeight(height);

				// gets image width
				int width = img.getWidth();
				file.setWidth(width);

			}

			// Wiping out the thumbnails and resized versions
			String folderPath = workingFile.getParentFile().getAbsolutePath();
			Identifier identifier = identifierAPI.findFromInode(file.getIdentifier());

			java.io.File directory = new java.io.File(folderPath);
			java.io.File[] files = directory.listFiles(new ThumbnailsFileNamesFilter(identifier));

			for (java.io.File iofile : files) {
				try {
					iofile.delete();
				} catch (SecurityException e) {
					Logger.error(FileAPIImpl.class, "FileAPIImpl.saveFileData(): " + iofile.getName()
							+ " cannot be erased. Please check the file permissions.");
				} catch (Exception e) {
					Logger.error(FileAPIImpl.class, "FileAPIImpl.saveFileData(): " + e.getMessage());
				}
			}
		}
	}

	public String getMimeType(String filename) {
		if (filename != null) {
			filename = filename.toLowerCase();
		}

		String mimeType = null;
		
		try {
		    mimeType = Config.CONTEXT.getMimeType(filename);
		    if(!UtilMethods.isSet(mimeType))
		        mimeType = com.dotmarketing.portlets.files.model.File.UNKNOWN_MIME_TYPE;
		}
		catch(Exception ex) {
		    mimeType = com.dotmarketing.portlets.files.model.File.UNKNOWN_MIME_TYPE;
		    Logger.warn(this,"Error looking for mimetype on file: "+filename,ex);
		}

		return mimeType;
	}

	public File saveFile(File file, java.io.File uploadedFile, Folder folder, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		
		if(!isLegacyFilesSupported()){
			//throw new DotStateException("File Assets have been disabled.");
		}

		String fileName = UtilMethods.getFileName(file.getFileName());
		File currentFile = getWorkingFileByFileName(file.getFileName(), folder, user, respectFrontendRoles);

		boolean fileExists = (currentFile != null) && InodeUtils.isSet(currentFile.getInode());

		if (fileExists) {
			if (!permissionAPI.doesUserHavePermission(currentFile, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
				throw new DotSecurityException("You don't have permission to read the source file.");
			}
		}

		if (!permissionAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to write in the destination folder.");
		}

		File workingFile = null;

		try {
			long uploadFileMaxSize = Long.parseLong(Config.getStringProperty("UPLOAD_FILE_MAX_SIZE"));

			// Checking the max file size
			if ((uploadedFile != null) && ((uploadFileMaxSize > 0) && (uploadedFile.length() > uploadFileMaxSize))) {
				if (currentFile != null)
					unLockAsset(currentFile);
				throw new DotDataException("Uploaded file is bigger than the file max size allowed.");
			}

			// CHECK THE FOLDER PATTERN
			if (UtilMethods.isSet(file.getFileName()) && !APILocator.getFolderAPI().matchFilter(folder, file.getFileName())) {
				// when there is an error saving should unlock working asset
				if (currentFile != null)
					unLockAsset(currentFile);
				throw new DotDataException("message.file_asset.error.filename.filters");
			}

			// Setting some flags
			boolean editFile = false;
			boolean newUploadedFile = true;

			// checks if the file is new or it's being edited
			if (fileExists) {
				editFile = true;
				// if it's being edited it keeps the same file name as the
				// current one
				fileName = currentFile.getFileName();
			}

			// checks if another identifier with the same name exists in the
			// same
			// folder
			if (!editFile && (getWorkingFileByFileName(fileName, folder, user, respectFrontendRoles) != null)) {
				throw new DotDataException("message.file_asset.error.filename.exists");
			}

			// to check if a file is being uploaded
			if (fileName.length() == 0) {
				newUploadedFile = false;
			}

			// getting mime type
			if (editFile && newUploadedFile && (file.getMimeType() != null) && (currentFile != null)
					&& (!file.getMimeType().equals(currentFile.getMimeType()))) {
				// when there is an error saving should unlock working asset
				unLockAsset(currentFile);
				throw new DotDataException("message.file_asset.error.mimetype");
			}

			//save(file);
			// ffac.deleteFromCache(file);
			// get the file Identifier
			Identifier ident = null;
			if (fileExists) {
				ident = APILocator.getIdentifierAPI().find(currentFile);
				ffac.deleteFromCache(currentFile);
			} else {
				ident = new Identifier();
			}
			// Saving the file, this creates the new version and save the new
			// data
			if (newUploadedFile && uploadedFile.length() > 0) {

				workingFile = saveFile(file, uploadedFile, folder, ident, user, respectFrontendRoles);

			} else {
				workingFile = saveFile(file, null, folder, ident, user, respectFrontendRoles);
			}
			uploadedFile.delete();
			ffac.deleteFromCache(workingFile);
			ident = APILocator.getIdentifierAPI().find(workingFile);

			// Refreshing the menues
			if (file.isShowOnMenu()) {
				// existing folder with different show on menu ... need to
				// regenerate menu
				RefreshMenus.deleteMenu(file);
			}

		} catch (IOException e) {
			Logger.error(this, "\n\n\nEXCEPTION IN FILE SAVING!!! " + e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage());
		}

		return workingFile;
	}

	protected File saveFile(File newFile, java.io.File dataFile, Folder folder, Identifier identifier, User user,
			boolean respectFrontendRoles) throws DotDataException, DotSecurityException, IOException {
		
		if(!isLegacyFilesSupported()){
			throw new DotStateException("File Assets have been disabled.");
		}

		if (!permissionAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to write in the destination folder.");
		}

		return FactoryLocator.getFileFactory().saveFile(newFile, dataFile, folder, identifier);
	}
	

    
	public boolean delete(File file, User user, boolean respectFrontendRoles) throws DotSecurityException, Exception {
		
		if (permissionAPI.doesUserHavePermission(file, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles)) {
			return deleteAsset(file);
		} else {
			throw new DotSecurityException(WebKeys.USER_PERMISSIONS_EXCEPTION);
		}
	}

	public List<File> getAllHostFiles(Host parentHost, boolean live, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {

		List<File> files = ffac.getAllHostFiles(parentHost, live);
		return permissionAPI.filterCollection(files, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);

	}

	public List<File> getFolderFiles(Folder parentFolder, boolean live, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {

		if (!permissionAPI.doesUserHavePermission(parentFolder, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to read the destination folder.");
		}
		
		ChildrenCondition cond = new ChildrenCondition();
		if(live)
			cond.live=true;
		else
			cond.working=true;
		List<File> files = APILocator.getFolderAPI().getFiles(parentFolder, user, respectFrontendRoles,cond);
		return permissionAPI.filterCollection(files, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);

	}

	public List<Map<String, Serializable>> DBSearch(Query query, User user, boolean respectFrontendRoles) throws ValidationException,
			DotDataException {
		Map<String, String> dbColToObjectAttribute = new HashMap<String, String>();

		if (UtilMethods.isSet(query.getSelectAttributes())) {

			if (!query.getSelectAttributes().contains("title")) {
				query.getSelectAttributes().add("title" + " as " + QueryResult.CMIS_TITLE);
			}
		} else {
			List<String> atts = new ArrayList<String>();
			atts.add("*");
			atts.add("title" + " as " + QueryResult.CMIS_TITLE);
			query.setSelectAttributes(atts);
		}

		return QueryUtil.DBSearch(query, dbColToObjectAttribute, null, user, true, respectFrontendRoles);
	}

	public File getWorkingFileById(String fileId, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		File file = (File) vapi.findWorkingVersion(fileId, user, respectFrontendRoles);
		if (file == null)
			return file;
		if (!permissionAPI.doesUserHavePermission(file, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles))
			throw new DotSecurityException("User " + user.getUserId() + "has no permissions to read file id " + fileId);

		return file;
	}

	public File get(String inode, User user, boolean respectFrontendRoles) throws DotHibernateException, DotSecurityException,
			DotDataException {
		File file = ffac.get(inode);

		if (!permissionAPI.doesUserHavePermission(file, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
			throw new DotSecurityException(WebKeys.USER_PERMISSIONS_EXCEPTION);
		}

		return file;
	}

	public Folder getFileFolder(File file, Host host, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {
		if(file == null || !InodeUtils.isSet(file.getIdentifier())){
			return null;
		}
		Identifier id = APILocator.getIdentifierAPI().find(file.getIdentifier());
		return folderAPI.findFolderByPath(id.getParentPath(), host, user, respectFrontendRoles);

	}

	public List<File> findFiles(User user, boolean includeArchived, Map<String, Object> params, String hostId, String inode,
			String identifier, String parent, int offset, int limit, String orderBy) throws DotSecurityException, DotDataException {
		return ffac.findFiles(user, includeArchived, params, hostId, inode, identifier, parent, offset, limit, orderBy);
	}

	public File copyFile(File file, Folder parent, User user, boolean respectFrontendRoles) throws IOException, DotSecurityException,
			DotDataException {

		if(!isLegacyFilesSupported()){
			throw new DotStateException("File Assets have been disabled.");
		}
		
		if (!permissionAPI.doesUserHavePermission(file, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
			throw new DotSecurityException(WebKeys.USER_PERMISSIONS_EXCEPTION);
		}
		if (!permissionAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, respectFrontendRoles)) {
			throw new DotSecurityException(WebKeys.USER_PERMISSIONS_EXCEPTION);
		}
		return ffac.copyFile(file, parent);

	}


	public boolean renameFile(File file, String newName, User user, boolean respectFrontendRoles) throws DotStateException,
			DotDataException, DotSecurityException {
		if(!isLegacyFilesSupported()){
			throw new DotStateException("File Assets have been disabled.");
		}
		
		if (!permissionAPI.doesUserHavePermission(file, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles)) {
			throw new DotSecurityException(WebKeys.USER_PERMISSIONS_EXCEPTION);
		}

		return ffac.renameFile(file, newName);
	}

	public boolean moveFile(File file, Folder parent, User user, boolean respectFrontendRoles) throws DotStateException, DotDataException,
			DotSecurityException {
		if(!isLegacyFilesSupported()){
			throw new DotStateException("File Assets have been disabled.");
		}
		
		if (!permissionAPI.doesUserHavePermission(file, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
			throw new DotSecurityException(WebKeys.USER_PERMISSIONS_EXCEPTION);
		}
		if (!permissionAPI.doesUserHavePermission(parent, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, respectFrontendRoles)) {
			throw new DotSecurityException(WebKeys.USER_PERMISSIONS_EXCEPTION);
		}
		return ffac.moveFile(file, parent);

	}

	public void publishFile(File file, User user, boolean respectFrontendRoles) throws WebAssetException, DotSecurityException,
			DotDataException {
		
		if(!isLegacyFilesSupported()){
			throw new DotStateException("File Assets have been disabled.");
		}
		
		if (!permissionAPI.doesUserHavePermission(file, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
			throw new DotSecurityException(WebKeys.USER_PERMISSIONS_EXCEPTION);
		}
		ffac.publishFile(file);

	}

	public File getFileByURI(String uri, Host host, boolean live, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {
		File file = ffac.getFileByURI(uri, host, live);
		if (!permissionAPI.doesUserHavePermission(file, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
			throw new DotSecurityException(WebKeys.USER_PERMISSIONS_EXCEPTION);
		}
		return file;
	}

	public File getFileByURI(String uri, String hostId, boolean live, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {
		File file = ffac.getFileByURI(uri, hostId, live);
		if (!permissionAPI.doesUserHavePermission(file, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
			throw new DotSecurityException(WebKeys.USER_PERMISSIONS_EXCEPTION);
		}
		return file;
	}
	

	public File find(String inode, User user, boolean respectFrontendRoles) throws DotStateException, DotDataException, DotSecurityException{

		File file = fcache.get(inode);
		
		if(file ==null){
			file = ffac.get(inode);
			if(file != null && UtilMethods.isSet(file.getInode())){
				fcache.add(file);
			}
			else{
				file = new File();
				file.setInode(inode);
				file.setIdentifier(Constants.FOUR_OH_FOUR_RESPONSE);
				fcache.add(file);
			}
		}
		if(Constants.FOUR_OH_FOUR_RESPONSE.equals(file.getIdentifier())){
			return null;
		}
		if (!permissionAPI.doesUserHavePermission(file, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
			throw new DotSecurityException(WebKeys.USER_PERMISSIONS_EXCEPTION);
		}
		return file;
		
	}
	
	
    public  String getRelativeAssetPath(Inode inode) {
        String _inode = inode.getInode();
        return getRelativeAssetPath(_inode, UtilMethods.getFileExtension(((com.dotmarketing.portlets.files.model.File) inode).getFileName())
                            .intern());
    }

    private  String getRelativeAssetPath(String inode, String ext) {
        String _inode = inode;
        String path = "";

       	path = java.io.File.separator + _inode.charAt(0)
       		+ java.io.File.separator + _inode.charAt(1) + java.io.File.separator + _inode + "." + ext;

        return path;
    }
    
    public String getRealAssetPath(Inode inode) {
        String _inode = inode.getInode();
        return getRealAssetPath (_inode, UtilMethods.getFileExtension(((com.dotmarketing.portlets.files.model.File) inode).getFileName())
                .intern());
    }

    public String getRealAssetPath(String inode, String ext) {
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
            return Config.CONTEXT.getRealPath(path);
        else
            return path;
    	
    }
	
    public  void invalidateCache(File file) throws DotDataException, DotSecurityException {
    	fcache.remove(file);
    	
    }
	
	 public boolean fileNameExists(Folder folder, String fileName) throws DotStateException, DotDataException, DotSecurityException{
		return ffac.fileNameExists(folder, fileName) ;
	 }

    public  String getRealAssetPath(){
    	
    	String realPath = null;
		String assetPath = null;
		try {
            realPath = Config.getStringProperty("ASSET_REAL_PATH");
        } catch (Exception e) { }
        try {
            assetPath = Config.getStringProperty("ASSET_PATH");
        } catch (Exception e) { }
        
        if(!UtilMethods.isSet(realPath)){
        	return Config.CONTEXT.getRealPath(assetPath);
        }else{
        	return realPath;
        }
    }
    
    public String getRealAssetPathTmpBinary() {
        String assetpath=getRealAssetPath();
        java.io.File adir=new java.io.File(assetpath);
        if(!adir.isDirectory())
            adir.mkdir();
        String path=assetpath+java.io.File.separator+"tmp_upload";
        java.io.File dir=new java.io.File(path);
        if(!dir.isDirectory())
            dir.mkdir();
        return path;
    }
    
    public boolean isLegacyFilesSupported(){//DOTCMS-6905
    	boolean isLegacyFilesSupported = false;
		try{
			isLegacyFilesSupported = Config.getBooleanProperty("ENABLE_LEGACY_FILE_SUPPORT");
		}catch(Exception ne){
			Logger.debug(this, ne.getMessage());
			try{
				if(!Config.getBooleanProperty("DISABLE_OLD_FILE_ASSET_IMPL"))
					isLegacyFilesSupported = true;
			}catch(Exception nse){
				Logger.debug(this, ne.getMessage());
			}
		}
		return isLegacyFilesSupported;
    }

    @Override
    public int deleteOldVersions(Date assetsOlderThan) throws DotDataException, DotHibernateException {
        String condition = " mod_date < ? and not exists (select * from fileasset_version_info "+
                " where working_inode=file_asset.inode or live_inode=file_asset.inode)";
        
        String inodesToDelete = "select inode from file_asset where "+condition;
        DotConnect dc = new DotConnect();
        dc.setSQL(inodesToDelete);
        dc.addParam(assetsOlderThan);
        for(Map<String,Object> inodeMap : dc.loadObjectResults()) {
            String inode=inodeMap.get("inode").toString();
            java.io.File fileFolderPath = new java.io.File(
                    APILocator.getFileAPI().getRealAssetPath() + 
                    java.io.File.separator + inode.substring(0, 1) +
                    java.io.File.separator + inode.substring(1, 2));
            if(fileFolderPath.exists() && fileFolderPath.isDirectory()) {
                for(java.io.File ff : fileFolderPath.listFiles())
                    if(ff.getName().startsWith(inode) && UtilMethods.isImage(ff.getName()))
                        if(FileUtils.deleteQuietly(ff))
                            Logger.info(this, "deleting old file "+ff.getAbsolutePath());
                        else
                            Logger.info(this, "can't delete old file "+ff.getAbsolutePath());
            }
        }
        return deleteOldVersions(assetsOlderThan,"file_asset");
    }
}