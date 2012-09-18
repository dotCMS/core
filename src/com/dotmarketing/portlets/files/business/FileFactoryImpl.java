package com.dotmarketing.portlets.files.business;

import java.awt.Container;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.velocity.runtime.resource.ResourceManager;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.cache.WorkingCache;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.menubuilders.RefreshMenus;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.files.model.FileAssetVersionInfo;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.velocity.DotResourceCache;
import com.liferay.portal.model.User;

public class FileFactoryImpl implements com.dotmarketing.portlets.files.business.FileFactory {


	private final String workingFileByName =
		"select {file_asset.*} from file_asset, inode file_asset_1_, " +
		"identifier file_asset_identifier, fileasset_version_info vi where " +
		"file_asset_identifier.parent_path = ? and file_asset_identifier.host_inode = ? and file_asset_identifier.id = file_asset.identifier and " +
		"vi.identifier=file_asset_identifier.id and file_asset.file_name = ? " +
		"and file_asset.inode = file_asset_1_.inode and " +
		"file_asset.inode = vi.working_inode ";

	private final String liveFileByName =
		"select {file_asset.*} from file_asset, inode file_asset_1_, " +
		"identifier file_asset_identifier, fileasset_version_info vi where " +
		"file_asset_identifier.parent_path = ? and file_asset_identifier.id = file_asset.identifier and " +
		"vi.identifier=file_asset_identifier.id and file_asset.file_name = ? " +
		"and file_asset.inode = file_asset_1_.inode and " +
		"file_asset.inode = vi.live_inode ";


	protected FileCache fileCache;
	public FileFactoryImpl() {
		fileCache = CacheLocator.getFileCache();
	}

	/**
	 * This method will save the newFile as the new working version for the
	 * given identifier and data if the given file is new it will copy the
	 * permissions from the folder and if the file given is set to live = true
	 * it will publish it
	 *
	 * @param newFile
	 *            New File to save
	 * @param data
	 *            New data to store
	 * @param folder
	 *            Parent folder to be assigned to the new file
	 * @param identifier
	 *            Identifier of the asset
	 * @param user
	 *            User how is making the modification if null no user id will be
	 *            set as the last modified user
	 * @return
	 * @throws Exception
	 */
	public File getWorkingFileByFileName(String fileName, Folder folder) throws DotDataException{

		Identifier id = APILocator.getIdentifierAPI().find(folder);

    	HibernateUtil hu = new HibernateUtil(File.class);
    	hu.setSQLQuery(workingFileByName);
    	hu.setParam(id.getPath());
    	hu.setParam(id.getHostId());
    	hu.setParam(fileName);

    	return (File) hu.load();


	}

	public File getLiveFileByFileName(String fileName, Folder folder) throws DotDataException{

		Identifier id = APILocator.getIdentifierAPI().find(folder);

    	HibernateUtil hu = new HibernateUtil(File.class);
    	hu.setSQLQuery(liveFileByName);
    	hu.setParam(id.getPath());
    	hu.setParam(fileName);

    	return (File) hu.load();


	}

	@SuppressWarnings("unchecked")
	public File saveFile(File newFile, java.io.File dataFile, Folder parentFolder, Identifier identifier) throws DotDataException {

		boolean localTransation = false;

		try {
			localTransation =  DbConnectionFactory.getConnection().getAutoCommit();
			if (localTransation) {
				HibernateUtil.startTransaction();
			}
			// old working file
			File oldFile = null;
			// if new identifier
			if (identifier == null || !InodeUtils.isSet(identifier.getInode())) {
				identifier = APILocator.getIdentifierAPI().createNew(newFile, parentFolder);
				newFile.setIdentifier(identifier.getInode());
				HibernateUtil.save(newFile);
				APILocator.getVersionableAPI().setWorking(newFile);
				saveFileData(newFile, null, dataFile);
			}else{
				APILocator.getVersionableAPI().removeLive(identifier.getId());
			}
			if (UtilMethods.isSet(dataFile)) {
				HibernateUtil.save(newFile);
				saveFileData(newFile, null, dataFile);
			}
			if (oldFile != null && InodeUtils.isSet(oldFile.getInode())) {
				APILocator.getFileAPI().invalidateCache(oldFile);
				fileCache.remove(oldFile);
				WorkingCache.removeAssetFromCache(oldFile);
			}
			LiveCache.removeAssetFromCache(newFile);
			if (newFile.isLive()) {
				LiveCache.addToLiveAssetToCache(newFile);
			}
			WorkingCache.addToWorkingAssetToCache(newFile);

		} catch (Exception e) {
			if (localTransation) {
				HibernateUtil.rollbackTransaction();
			}
			throw new DotDataException(e.getMessage(),e);

		} finally {

			if (localTransation) {
				HibernateUtil.commitTransaction();
			}
		}
		return newFile;
	}

	public void delete(File file) throws DotDataException, DotStateException, DotSecurityException {
		HibernateUtil.delete(file);

		fileCache.remove(file);
		WorkingCache.removeAssetFromCache(file);
		if (file.isLive()) {
			LiveCache.removeAssetFromCache(file);
		}
		CacheLocator.getIdentifierCache().removeFromCacheByVersionable(file);
	}

	public void deleteFromCache(File file) throws DotDataException, DotStateException, DotSecurityException {
		fileCache.remove(file);
		WorkingCache.removeAssetFromCache(file);
		if (file.isLive()) {
			LiveCache.removeAssetFromCache(file);
		}
		CacheLocator.getIdentifierCache().removeFromCacheByVersionable(file);
	}


	@SuppressWarnings("unchecked")
	public List<File> getAllHostFiles(Host host, boolean live) throws DotDataException {
		HibernateUtil hu = new HibernateUtil(File.class);
		StringBuilder queryBuilder = new StringBuilder("select {file_asset.*} from file_asset, inode file_asset_1_,identifier ident, fileasset_version_info vi "
				+ "where file_asset.inode = file_asset_1_.inode and " + "file_asset.identifier = ident.id and asset_type='file_asset' and ident.id=vi.identifier "
				+ " and ident.host_inode = ? ");
		if (live)
			queryBuilder.append(" and vi.live_inode=file_asset.inode ");
		else
			queryBuilder.append(" and vi.working_inode=file_asset.inode ");

		hu.setSQLQuery(queryBuilder.toString());
		hu.setParam(host.getIdentifier());

		return hu.list();
	}

	@SuppressWarnings("unchecked")
	public File getWorkingFileById(String identifier) throws DotDataException {
		HibernateUtil hu = new HibernateUtil(File.class);
		hu.setSQLQuery("select {file_asset.*} from file_asset, inode file_asset_1_, fileasset_version_info vi " +
				" where file_asset.identifier = ? and vi.identifier=file_asset.identifier and vi.working_inode=file_asset.inode "
				+ "and file_asset_1_.inode = file_asset.inode");
		hu.setParam(identifier);
		List<File> files = hu.list();
		if (files.size() == 0)
			return null;
		return files.get(0);
	}

	public File get(String inode) throws DotStateException, DotDataException, DotSecurityException {
		File file = fileCache.get(inode);

		if ((file == null) || !InodeUtils.isSet(file.getInode())) {
			file = (File) HibernateUtil.load(File.class, inode);

			fileCache.add(file);
			WorkingCache.removeAssetFromCache(file);
			WorkingCache.addToWorkingAssetToCache(file);
			LiveCache.removeAssetFromCache(file);
			if (file.isLive()) {
				LiveCache.addToLiveAssetToCache(file);
			}
		}

		return file;
	}

	public Folder getFileFolder(File file, String hostId) throws DotDataException {
		HibernateUtil hu = new HibernateUtil(Folder.class);
		hu.setSQLQuery("select {folder.*} from folder,identifier,inode folder_1_ where folder.identifier = identifier.id and "
				+ "folder_1_.inode = folder.inode and host_inode = ? and path =(select parent_path from identifier where id=?)");

		hu.setParam(hostId);
		hu.setParam(file.getIdentifier());
		return (Folder) hu.load();
	}

	public List<File> findFiles(User user, boolean includeArchived, Map<String, Object> params, String hostId, String inode,
			String identifier, String parent, int offset, int limit, String orderBy) throws DotSecurityException, DotDataException {

		PaginatedArrayList<File> assets = new PaginatedArrayList<File>();
		List<Permissionable> toReturn = new ArrayList<Permissionable>();
		int internalLimit = 500;
		int internalOffset = 0;
		boolean done = false;

		StringBuilder conditionBuffer = new StringBuilder().append(" asset.inode=versioninfo.workingInode ");
		String condition = !includeArchived ? " and versioninfo.deleted = "
				+ DbConnectionFactory.getDBFalse() : " ";
		conditionBuffer.append(condition);

		List<Object> paramValues = null;
		if (params != null && params.size() > 0) {
			conditionBuffer.append(" and (");
			paramValues = new ArrayList<Object>();
			int counter = 0;
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				if (counter == 0) {
					if (entry.getValue() instanceof String) {
						if (entry.getKey().equalsIgnoreCase("inode")) {
							conditionBuffer.append(" asset.").append(entry.getKey()).append(" = '").append(entry.getValue()).append("'");
						} else {
							conditionBuffer.append(" lower(asset.").append(entry.getKey()).append(") like ? ");
							paramValues.add("%" + ((String) entry.getValue()).toLowerCase() + "%");
						}
					} else {
						conditionBuffer.append(" asset.").append(entry.getKey()).append(" = ").append(entry.getValue());
					}
				} else {
					if (entry.getValue() instanceof String) {
						if (entry.getKey().equalsIgnoreCase("inode")) {
							conditionBuffer.append(" OR asset.").append(entry.getKey()).append(" = '").append(entry.getValue()).append("'");
						} else {
							conditionBuffer.append(" OR lower(asset.").append(entry.getKey()).append(") like ? ");
							paramValues.add("%" + ((String) entry.getValue()).toLowerCase() + "%");
						}
					} else {
						conditionBuffer.append(" OR asset.").append(entry.getKey()).append(" = ").append(entry.getValue());
					}
				}

				counter += 1;
			}
			conditionBuffer.append(" ) ");
		}

		StringBuilder query = new StringBuilder();
		query.append("select asset from asset in class ").append(File.class.getName()).append(", ").append("inode in class ").append(Inode.class.getName())
				.append(", identifier in class ").append(Identifier.class.getName()).append(", versioninfo in class ").append(FileAssetVersionInfo.class.getName());
		if (UtilMethods.isSet(parent)) {
			query.append(" ,tree in class ").append(Tree.class.getName()).append(" where asset.inode=inode.inode ")
					.append("and asset.identifier = identifier.id and tree.parent = '").append(parent).append("' and tree.child=asset.inode");

		} else {
			query.append(" where asset.inode=inode.inode and asset.identifier = identifier.id");
		}
		query.append(" and versioninfo.identifier=identifier.id ");
		if (UtilMethods.isSet(hostId)) {
			query.append(" and identifier.hostId = '").append(hostId).append("'");
		}
		if (UtilMethods.isSet(inode)) {
			query.append(" and asset.inode = '").append(inode).append("'");
		}
		if (UtilMethods.isSet(identifier)) {
			query.append(" and asset.identifier = '").append(identifier).append("'");
		}
		if (!UtilMethods.isSet(orderBy)) {
			orderBy = "modDate desc";
		}

		List<File> resultList = new ArrayList<File>();
		HibernateUtil dh = new HibernateUtil(File.class);
		String type;
		int countLimit = 100;
		int size = 0;
		try {
			type = ((Inode) File.class.newInstance()).getType();
			query.append(" and asset.type='").append(type).append("' ");
			final String conditions=conditionBuffer.toString().trim();
			if(conditions.length()>0)
			    query.append(" and ").append(conditions);
			query.append(" order by asset.").append(orderBy);
			dh.setQuery(query.toString());

			if (paramValues != null && paramValues.size() > 0) {
				for (Object value : paramValues) {
					dh.setParam((String) value);
				}
			}

			while (!done) {
				dh.setFirstResult(internalOffset);
				dh.setMaxResults(internalLimit);
				resultList = dh.list();
				PermissionAPI permAPI = APILocator.getPermissionAPI();
				toReturn.addAll(permAPI.filterCollection(resultList, PermissionAPI.PERMISSION_READ, false, user));
				if (countLimit > 0 && toReturn.size() >= countLimit + offset)
					done = true;
				else if (resultList.size() < internalLimit)
					done = true;

				internalOffset += internalLimit;
			}

			if (offset > toReturn.size()) {
				size = 0;
			} else if (countLimit > 0) {
				int toIndex = offset + countLimit > toReturn.size() ? toReturn.size() : offset + countLimit;
				size = toReturn.subList(offset, toIndex).size();
			} else if (offset > 0) {
				size = toReturn.subList(offset, toReturn.size()).size();
			}
			assets.setTotalResults(size);

			if(limit!=-1) {
				int from = offset < toReturn.size() ? offset : 0;
				int pageLimit = 0;
				for (int i = from; i < toReturn.size(); i++) {
					if (pageLimit < limit) {
						assets.add((File) toReturn.get(i));
						pageLimit += 1;
					} else {
						break;
					}

				}
			} else {
				for (int i = 0; i < toReturn.size(); i++) {
					assets.add((File) toReturn.get(i));
				}
			}

		} catch (Exception e) {

			Logger.error(FileFactoryImpl.class, "findFiles failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

		return assets;
	}

	/**
	 * This method will copy the file data from file to version if version is
	 * not null and version inode > 0 and will replace current file data if
	 * newData passed is not null
	 *
	 * @param file
	 * @param version
	 * @param newData
	 * @throws IOException
	 * @throws Exception
	 */
	public void saveFileData(File file, File destination, java.io.File newDataFile) throws IOException  {

		String fileName = file.getFileName();

		// This was added for http://jira.dotmarketing.net/browse/DOTCMS-5390
		// but this breaks the original intent of the
		// method. See the doc for the method above. Caused
		// http://jira.dotmarketing.net/browse/DOTCMS-5539 so commented out.
		// if(newDataFile ==null || newDataFile.length() ==0){
		// throw new
		// DotStateException("Null or 0 lenght java.io.file passed in for file:"
		// + file.getInode());
		// }

		String assetsPath = APILocator.getFileAPI().getRealAssetsRootPath();
		new java.io.File(assetsPath).mkdir();

		// creates the new file as
		// inode{1}/inode{2}/inode.file_extension
		java.io.File workingFile = getAssetIOFile(file);

		// http://jira.dotmarketing.net/browse/DOTCMS-1873
		// To clear velocity cache
		DotResourceCache vc = CacheLocator.getVeloctyResourceCache();
		vc.remove(ResourceManager.RESOURCE_TEMPLATE + workingFile.getPath());

		// If a new version was created, we move the current data to the new
		// version
		if (destination != null && InodeUtils.isSet(destination.getInode())) {
			java.io.File newVersionFile = getAssetIOFile(destination);
			// FileUtil.copyFile(workingFile, newVersionFile);
			FileUtils.copyFile(workingFile, newVersionFile);
			// FileInputStream is = new FileInputStream(workingFile);
			// FileChannel channelFrom = is.getChannel();
			// java.io.File newVersionFile = getAssetIOFile(destination);
			// FileChannel channelTo = new
			// FileOutputStream(newVersionFile).getChannel();
			// channelFrom.transferTo(0, channelFrom.size(), channelTo);
			// channelTo.force(false);
			// channelTo.close();
			// channelFrom.close();
		}

		if (newDataFile != null) {
			// Saving the new working data
			FileUtils.copyFile(newDataFile, workingFile);

			file.setSize((int) newDataFile.length());

			// checks if it's an image
			if (UtilMethods.isImage(fileName)) {
				InputStream in = null;
				try {
					// gets image height
					in = new BufferedInputStream(new FileInputStream(workingFile));
					byte[] imageData = new byte[in.available()];
					in.read(imageData);
					Image image = Toolkit.getDefaultToolkit().createImage(imageData);
					MediaTracker mediaTracker = new MediaTracker(new Container());
					mediaTracker.addImage(image, 0);
					mediaTracker.waitForID(0);
					int imageWidth = image.getWidth(null);
					int imageHeight = image.getHeight(null);

					in.close();
					in = null;
					// gets image width
					file.setHeight(imageHeight);
					file.setWidth(imageWidth);
				} catch (Exception e) {
					Logger.error(FileFactory.class, "Unable to read image " + workingFile + " : " + e.getMessage());
					throw new IOException(e);
				} finally {
					if (in != null) {
						try {
							in.close();
						} catch (Exception e) {

							Logger.error(FileFactory.class, "Unable to close image " + e.getMessage());
						}
					}

				}
			}
			// Wiping out the thumbnails and resized versions
			// http://jira.dotmarketing.net/browse/DOTCMS-5911
			String inode = file.getInode();
			if (UtilMethods.isSet(inode)) {
				java.io.File tumbnailDir = new java.io.File(Config.CONTEXT.getRealPath("/assets/dotGenerated/" + inode.charAt(0) + "/"
						+ inode.charAt(1)));
				if (tumbnailDir != null) {
					java.io.File[] files = tumbnailDir.listFiles();
					if (files != null) {
						for (java.io.File iofile : files) {
							try {
								if (iofile.getName().startsWith("dotGenerated_")) {
									iofile.delete();
								}
							} catch (SecurityException e) {
								Logger.error(FileFactory.class, "EditFileAction._saveWorkingFileData(): " + iofile.getName()
										+ " cannot be erased. Please check the file permissions.");
							} catch (Exception e) {
								Logger.error(FileFactory.class, "EditFileAction._saveWorkingFileData(): " + e.getMessage());
							}
						}
					}
				}
			}
		}
	}

	public List<File> getFolderFiles(Folder folder, boolean live) throws DotDataException {
		// TODO Auto-generated method stub
		return null;
	}

    /**
     * Copy a file into the given host
     *
     * @param file File to be copied
     * @param host Destination host
     * @return true if copy success, false otherwise
     */
    public File copyFile ( File file, Host host ) throws DotDataException, IOException {
        return copyFile(file, null, host);
    }

    /**
     * Copy a file into the given directory
     *
     * @param file   File to be copied
     * @param parent Destination Folder
     * @return true if copy success, false otherwise
     */
    public File copyFile ( File file, Folder parent ) throws DotDataException, IOException {
        return copyFile(file, parent, null);
    }

    /**
     * Copy a file into the given directory OR host
     *
     * @param file   File to be copied
     * @param parent Destination Folder
     * @param host Destination host
     * @return true if copy success, false otherwise
     * @throws IOException
     * @throws DotHibernateException
     */
    private File copyFile ( File file, Folder parent, Host host ) throws DotDataException, IOException {

        File newFile = new File();

        newFile.copy( file );

        // gets filename before extension
        String fileName = com.dotmarketing.util.UtilMethods.getFileName( file.getFileName() );
        // gets file extension
        String fileExtension = com.dotmarketing.util.UtilMethods.getFileExtension( file.getFileName() );

        Boolean fileNameExists;
        if (parent != null) {
            fileNameExists = fileNameExists( parent, file.getFileName() );
        } else {
            fileNameExists = fileNameExists( APILocator.getFolderAPI().findSystemFolder(), file.getFileName() );
        }

        // Setting file name
        if ( fileNameExists ) {
            // adds "copy" word to the filename
            newFile.setFileName( fileName + "_copy." + fileExtension );
            newFile.setFriendlyName( file.getFriendlyName() + " (COPY) " );
        } else {
            newFile.setFileName( fileName + "." + fileExtension );
        }

        Identifier identifier;
        if ( parent != null ) {
            identifier = APILocator.getIdentifierAPI().createNew( newFile, parent );
        } else {
            identifier = APILocator.getIdentifierAPI().createNew( newFile, host );
        }
        newFile.setIdentifier( identifier.getInode() );

        // persists the webasset
        HibernateUtil.saveOrUpdate( newFile );

        saveFileData( file, newFile, null );

        Logger.debug( FileFactory.class, "identifier=" + identifier.getURI() );

        WorkingCache.removeAssetFromCache( newFile );
        WorkingCache.addToWorkingAssetToCache( newFile );
        PermissionAPI permissionAPI = APILocator.getPermissionAPI();

        try {
            APILocator.getVersionableAPI().setWorking( newFile );
            if ( file.isLive() )
                APILocator.getVersionableAPI().setLive( newFile );
        } catch ( DotStateException e ) {
            Logger.error( this, e.getMessage() );
        } catch ( DotSecurityException e ) {
            Logger.error( this, e.getMessage() );
        }
        // Copy permissions
        permissionAPI.copyPermissions( file, newFile );

        return newFile;
    }

    public  java.io.File getAssetIOFile (File file) throws IOException {

		String fileName = file.getFileName();
		String suffix = UtilMethods.getFileExtension(fileName);

		String assetsPath =APILocator.getFileAPI().getRealAssetsRootPath();
		String fileInode = file.getInode();

		// creates the path where to save the working file based on the inode
		String fileFolderPath = String.valueOf(fileInode);
		if (fileFolderPath.length() == 1) {
			fileFolderPath = fileFolderPath + "0";
		}

		fileFolderPath = assetsPath + java.io.File.separator +
			fileFolderPath.substring(0, 1) + java.io.File.separator +
			fileFolderPath.substring(1, 2);

		new java.io.File(fileFolderPath).mkdirs();

		String filePath = fileFolderPath + java.io.File.separator +
			fileInode + "." + suffix;

		// creates the new file as
		// inode{1}/inode{2}/inode.file_extension
		java.io.File assetFile = new java.io.File(filePath);
		if (!assetFile.exists())
			assetFile.createNewFile();

		return assetFile;
    }

	public boolean fileNameExists(Folder folder, String fileName) throws  DotDataException{
	    	if(fileName ==null){
	    		return true;
	    	}
	    	try{
		    	List<File> files= APILocator.getFolderAPI().getFiles(folder, APILocator.getUserAPI().getSystemUser(), false);
		    	for(File f : files){
		    		if(f.getTitle().equals(fileName)){
		    			return true;
		    		}

		    	}
	    	}
	    	catch(Exception e){
	    		Logger.error(this.getClass(), e.getMessage(),e);
	    	}
	    	return false;
	    }


	@SuppressWarnings({ "unchecked", "deprecation" })
	public  boolean renameFile (File file, String newName) throws DotStateException, DotDataException, DotSecurityException {

    	//getting old file properties
    	String oldFileName = file.getFileName();
    	String ext = UtilMethods.getFileExtension(oldFileName);
    	Folder folder = APILocator.getFolderAPI().findParentFolder(file, APILocator.getUserAPI().getSystemUser(), false);

    	Identifier ident = APILocator.getIdentifierAPI().find(file);

    	String newFileName = newName;
    	if(UtilMethods.isSet(ext)){
    		newFileName = newFileName + "." + ext;
    	}

    	if(fileNameExists(folder, newFileName) || file.isLocked())
    		return false;

    	List<Versionable> versions = APILocator.getVersionableAPI().findAllVersions(ident);

    	boolean islive = false;


		for (Versionable version : versions) {
			File f = (File)version;


	    	// sets filename for this new file
	    	f.setFileName(newFileName);

	    	HibernateUtil.saveOrUpdate(f);

	    	if (f.isLive())
	    		islive = true;
    	}

		LiveCache.removeAssetFromCache(file);
		WorkingCache.removeAssetFromCache(file);
   		CacheLocator.getIdentifierCache().removeFromCacheByVersionable(file);

		ident.setURI(APILocator.getIdentifierAPI().find(folder).getPath() + newFileName);
    	//HibernateUtil.saveOrUpdate(ident);
		APILocator.getIdentifierAPI().save(ident);

    	if (islive){
    		LiveCache.removeAssetFromCache(file);
    		LiveCache.addToLiveAssetToCache(file);
    	}
    	WorkingCache.removeAssetFromCache(file);
   		WorkingCache.addToWorkingAssetToCache(file);
   		CacheLocator.getIdentifierCache().removeFromCacheByVersionable(file);

    	//RefreshMenus.deleteMenus();
   		RefreshMenus.deleteMenu(file);

    	return true;

    }

    /**
     * Moves a file into the given host
     *
     * @param file File to be copied
     * @param host Destination host
     * @return true if copy success, false otherwise
     */
    public Boolean moveFile ( File file, Host host ) throws DotStateException, DotDataException, DotSecurityException {
        return moveFile( file, null, host );
    }

    /**
     * Moves a file into the given directory
     *
     * @param file   File to be copied
     * @param parent Destination Folder
     * @return true if copy success, false otherwise
     */
    public Boolean moveFile ( File file, Folder parent ) throws DotStateException, DotDataException, DotSecurityException {
        return moveFile( file, parent, null );
    }

    /**
     * Moves a file into the given directory OR host
     *
     * @param file   File to be moved
     * @param parent Destination Folder
     * @param host   Destination Host
     * @return true if move success, false otherwise
     * @throws DotDataException
     * @throws DotStateException
     * @throws DotSecurityException
     */
    private Boolean moveFile ( File file, Folder parent, Host host ) throws DotStateException, DotDataException, DotSecurityException {

        HostAPI hostAPI = APILocator.getHostAPI();

        //Find file identifier
        Identifier identifier = com.dotmarketing.business.APILocator.getIdentifierAPI().find( file );

        // gets working container
        File workingWebAsset = (File) APILocator.getVersionableAPI().findWorkingVersion( identifier, APILocator.getUserAPI().getSystemUser(), false );
        // gets live container
        File liveWebAsset = (File) APILocator.getVersionableAPI().findLiveVersion( identifier, APILocator.getUserAPI().getSystemUser(), false );

        // checks if another identifer with the same name exists in the same
        Boolean fileNameExists;
        if ( parent != null ) {
            fileNameExists = fileNameExists( parent, file.getFileName() );
        } else {
            fileNameExists = fileNameExists( APILocator.getFolderAPI().findSystemFolder(), file.getFileName() );
        }
        if ( fileNameExists ) {
            return false;
        }

        // assets cache
        if ( (liveWebAsset != null) && (InodeUtils.isSet( liveWebAsset.getInode() )) ) {
            LiveCache.removeAssetFromCache( liveWebAsset );
        }
        WorkingCache.removeAssetFromCache( workingWebAsset );

        // gets old parent
        Folder oldParent = APILocator.getFolderAPI().findParentFolder( workingWebAsset, APILocator.getUserAPI().getSystemUser(), false );

        /*oldParent.deleteChild(workingWebAsset);
          if ((liveWebAsset != null) && (InodeUtils.isSet(liveWebAsset.getInode()))) {
              oldParent.deleteChild(liveWebAsset);
          }
          //add new Parent
          parent.addChild(workingWebAsset);
          if ((liveWebAsset != null) && (InodeUtils.isSet(liveWebAsset.getInode()))) {
              parent.addChild(liveWebAsset);
          }*/

        // gets identifier for this webasset and changes the uri and persists it
        User systemUser;
        try {
            systemUser = APILocator.getUserAPI().getSystemUser();
            if ( host == null ) {
                host = hostAPI.findParentHost( parent, systemUser, false );
            }
        } catch ( DotDataException e ) {
            Logger.error( FileFactory.class, e.getMessage(), e );
            throw new DotRuntimeException( e.getMessage(), e );

        } catch ( DotSecurityException e ) {
            Logger.error( FileFactory.class, e.getMessage(), e );
            throw new DotRuntimeException( e.getMessage(), e );
        }
        identifier.setHostId( host.getIdentifier() );
        identifier.setURI( parent != null ? workingWebAsset.getURI( parent ) : workingWebAsset.getURI() );
        //HibernateUtil.saveOrUpdate(identifier);
        APILocator.getIdentifierAPI().save( identifier );

        if ( UtilMethods.isSet( liveWebAsset ) )
            CacheLocator.getIdentifierCache().removeFromCacheByVersionable( liveWebAsset );
//		IdentifierCache.addAssetToIdentifierCache(liveWebAsset);

        // Add to Preview and Live Cache
        if ( (liveWebAsset != null) && (InodeUtils.isSet( liveWebAsset.getInode() )) ) {
            LiveCache.removeAssetFromCache( liveWebAsset );
            LiveCache.addToLiveAssetToCache( liveWebAsset );
        }
        WorkingCache.removeAssetFromCache( workingWebAsset );
        WorkingCache.addToWorkingAssetToCache( workingWebAsset );

        if ( file.isShowOnMenu() ) {
            //existing folder with different show on menu ... need to regenerate menu
            if ( parent != null ) {
                RefreshMenus.deleteMenu( oldParent, parent );
            } else {
                RefreshMenus.deleteMenu( oldParent );
            }
        }

        return true;
    }

	public  void publishFile(File file) throws WebAssetException, DotSecurityException, DotDataException {

		PublishFactory.publishAsset(file, APILocator.getUserAPI().getSystemUser(), false);

	}
    public  File getFileByURI(String uri, Host host, boolean live) throws DotDataException, DotSecurityException {
        return getFileByURI(uri, host.getIdentifier(), live);
    }

    public  File getFileByURI(String uri, String hostId, boolean live) throws DotDataException, DotSecurityException {


        //uri = uri.replaceAll(Config.getStringProperty("VIRTUAL_FILE_PREFIX"), "");
        Logger.debug(FileFactory.class, "getFileByURI=" + uri);
        Host host = APILocator.getHostAPI().find(hostId, APILocator.getUserAPI().getSystemUser(),false);
        Identifier id = APILocator.getIdentifierAPI().find(host, uri);
        return (live) ? (File) APILocator.getVersionableAPI().findLiveVersion(id, APILocator.getUserAPI().getSystemUser(), false)
        		      : (File) APILocator.getVersionableAPI().findWorkingVersion(id, APILocator.getUserAPI().getSystemUser(), false);

    }
}