package com.dotmarketing.portlets.fileassets.business;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.storage.model.Metadata;
import com.dotcms.util.MimeTypeUtils;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONIgnore;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.Date;
import java.util.Map;

/**
 * Represents a File in dotCMS.
 * <p>File Assets are files with a specific location within the <b>Site Browser</b> tree, and which
 * can be accessed using a URL which references the location of the file within the tree. File
 * Assets:</p>
 * <ul>
 *     <li>May be accessed via a URL based on either the location of the file within the Site
 *     Browser tree, or the Identifier of the file.</li>
 *     <li>Can be managed via <b>WebDAV</b>, and can be used for File-based Containers and
 *     Scripted Custom Endpoints.</li>
 *     <li>Can be displayed directly in menus (by setting the {@code Show on Menu} property).</li>
 * </ul>
 *
 * @author root
 * @since Mar 22nd, 2012
 */
public class FileAsset extends Contentlet implements IFileAsset {

    private File file;

	public static final String UNKNOWN_MIME_TYPE = "unknown";

	public FileAsset() {
		super();

	}

	/**
	 * Use to build a fileAsset and take the state of another fileAsset
	 * @param contentlet
	 * @throws Exception
	 */
	protected FileAsset(final FileAsset contentlet) throws Exception {
		super(contentlet);
		final File file = contentlet.getFileAsset();
		setBinary(FileAssetAPI.BINARY_FIELD, file);
	}

	/**
	 * Metadata as map getter. Use to get a compiled view including MD for all binaries
	 * @return
	 */
    public Map<String, Serializable> getMetaDataMap() {
        return Try.of(() -> getMetadata().getFieldsMeta())
                .getOrElse(ImmutableMap.of());
    }

	public Metadata getMetadata() throws DotDataException {
		return super.getBinaryMetadata(FileAssetAPI.BINARY_FIELD);
	}

	public long getLanguageId(){
		return super.getLanguageId();
	}

	public void setMenuOrder(int sortOrder) {
		setLongProperty(FileAssetAPI.SORT_ORDER, (long) sortOrder);

	}

	public int getMenuOrder() {
		return Integer.valueOf(String.valueOf(getLongProperty(FileAssetAPI.SORT_ORDER)));
	}

	private static final long serialVersionUID = 1L;


	public String getPath() {
		try {
			final Identifier id = APILocator.getIdentifierAPI().find(this.getIdentifier());
			return id.getParentPath();
		} catch (final Exception e) {
			Logger.error(this, String.format("Failed to get path from File Asset with ID " +
					"'%s': %s", this.getIdentifier(), ExceptionUtil.getErrorMessage(e)), e);
		}
		return null;
	}

	public String getParent() {
		if (FolderAPI.SYSTEM_FOLDER.equals(getFolder())) {
			return getHost();
		} else {
			return getFolder();
		}
	}

	public long getFileSize() {
		return 	Try.of(() -> getMetadata().getSize()).getOrElse(0);
	}

	public String getSha256() {
		return Try.of(() -> getMetadata().getSha256()).getOrNull();
	}

	public int getHeight() {
        return  Try.of(()-> getMetadata().getHeight()).getOrElse(0);
	}

	public int getWidth() {
		return  Try.of(()-> getMetadata().getWidth()).getOrElse(0);
	}

	public String getFileTitle() {
		return  Try.of(()-> getMetadata().getTitle()).getOrElse("unknown");
	}

	public boolean isImage() {
		return Try.of(()-> getMetadata().isImage()).getOrElse(UtilMethods.isImage(getUnderlyingFileName()));
	}

  /**
   * This gives you access to the physical file on disk.
   * @return
   */
  public String getUnderlyingFileName() {
	  return Try.of(() -> getMetadata().getName())
			  .getOrNull();
  }

	/***
	 * This access the logical file name stored on the table Identifier
	 * @return
	 */
	public String getFileName() {
		try {
			return UtilMethods.isSet(getIdentifier()) ?
			  APILocator.getIdentifierAPI().find(getIdentifier()).getAssetName()
			  : StringPool.BLANK;
		} catch (DotDataException e) {
		   throw new DotRuntimeException(e);
		}
	}

	public String getMimeType() {

		String mimeType = Try.of(() -> getMetadata().getContentType()).getOrNull();
		if(null != mimeType){
		   return mimeType;
		}

		mimeType = APILocator.getFileAssetAPI().getMimeType(getUnderlyingFileName());

		if (mimeType == null || UNKNOWN_MIME_TYPE.equals(mimeType)){
			mimeType = MimeTypeUtils.MIME_TYPE_APP_OCTET_STREAM;
		}

		return mimeType;
	}

	/**
	 *
	 * @deprecated
	 * This method is Here for compatibility purposes.
	 *    <p> Use {@link Contentlet#getBinaryMetadata(Field)} }
	 *    or {@link FileAsset#getMetaDataMap()} instead.
	 * @param mimeType
	 */
	@Deprecated
	public void setMimeType(String mimeType) {

	}

	@JsonIgnore
	public InputStream getInputStream() throws IOException {
		return new BufferedInputStream(Files.newInputStream(getFileAsset().toPath()));
	}

    @Override
	public void setBinary(final String velocityVarName, final File newFile)throws IOException{
		file = null;
		super.setBinary(velocityVarName, newFile);
	}

	@Override
	public void setBinary(final com.dotcms.contenttype.model.field.Field field, final File newFile)throws IOException{
		file = null;
		super.setBinary(field, newFile);
	}

	@JsonIgnore
	public File getFileAsset() {
		// Calling the getBinary method can be relatively expensive since it constantly verifies
		// the existence of the file on disk. Therefore, we'll keep a file reference at hand
		if (null == file) {
			try {
				file = getBinary(FileAssetAPI.BINARY_FIELD);
			} catch (final IOException e) {
				throw new DotStateException(
						String.format("Failed to find binary file for File Asset '%s' [ %s ]: %s",
								getFileName(), getInode(), ExceptionUtil.getErrorMessage(e)
						)
				);
			}
		}
		return file;
	}

	public boolean isDeleted() throws DotStateException, DotDataException, DotSecurityException {
		return super.isArchived();
	}

	public String getURI(Folder folder) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isShowOnMenu() {
		String isShowOnMenu = super.getStringProperty(FileAssetAPI.SHOW_ON_MENU);//DOTCMS-6968
		if(UtilMethods.isSet(isShowOnMenu) && isShowOnMenu.contains("true")){
			return true;
		}else{
			return false;
		}
	}

	public void setShowOnMenu(boolean showOnMenu) {
		super.setStringProperty(FileAssetAPI.SHOW_ON_MENU, Boolean.toString(showOnMenu));

	}

	public void setSortOrder(int sortOrder) {
		super.setSortOrder(sortOrder);

	}

	public void setTitle(final String title) {
		super.setStringProperty(FileAssetAPI.TITLE_FIELD, title);

	}

	public String getFriendlyName() {
		return super.getStringProperty(FileAssetAPI.TITLE_FIELD);
	}

	public void setFriendlyName(String friendlyName) {
		// Not implemented
	}

	public boolean isArchived() throws DotStateException, DotDataException, DotSecurityException {
       return isDeleted();
	}


	/**
	 * Returns the live.
	 * @return boolean
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	public boolean isLive() throws DotStateException, DotDataException, DotSecurityException {
	    return APILocator.getVersionableAPI().isLive(this);
	}

	/**
	 * Returns the locked.
	 * @return boolean
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	public boolean isLocked() throws DotStateException, DotDataException, DotSecurityException {
       return APILocator.getVersionableAPI().isLocked(this);
   }

	public String getType(){
		return "file_asset";
	}

	 public String getExtension(){
		 return UtilMethods.getFileExtension(getUnderlyingFileName());
	 }

	/**
	 * Creates a Map with the different attributes that belong to this File Asset.
	 *
	 * @return A Map containing the File Asset's attributes.
	 */
	@Override
	public Map<String, Object> getMap() {
		final Map<String,Object> map = super.getMap();
		boolean live =  false;
		boolean working =  false;
		boolean deleted = false;
		boolean locked  = false;
		try {
			live =  isLive();
			working = isWorking();
			deleted = isDeleted();
			locked  = isLocked();
		} catch (final Exception e) {
			Logger.error(this, String.format("Failed to retrieve live/working/deleted/locked status for FileAsset " +
					"'%s' [ %s ]: %s", getFileName(), getInode(), ExceptionUtil.getErrorMessage(e)), e);
		}
		map.put("extension", getExtension());
		map.put("live", live);
		map.put("working", working);
		map.put("deleted", deleted);
		map.put("locked", locked);
		map.put("isContent", true);
		map.put("fileAssetType", this.getContentTypeId());
		map.put("friendlyName", getStringProperty(FileAssetAPI.DESCRIPTION));
		map.put("mimeType", getMimeType());
		User modUser = null;
		try {
			modUser = APILocator.getUserAPI().loadUserById(this.getModUser(),APILocator.getUserAPI().getSystemUser(),false);
		} catch (final NoSuchUserException | DotDataException e) {
			Logger.debug(this, String.format("Failed to retrieve 'mod_user' for FileAsset " +
					"'%s' [ %s ]: %s", getFileName(), getInode(), ExceptionUtil.getErrorMessage(e)));
		} catch (final Exception e) {
			Logger.error(this, String.format("An error occurred when retrieving 'mod_user' for FileAsset " +
					"'%s' [ %s ]: %s", getFileName(), getInode(), ExceptionUtil.getErrorMessage(e)), e);
		}
		if (UtilMethods.isSet(modUser) && UtilMethods.isSet(modUser.getUserId()) && !modUser.isNew()) {
			map.put("modUserName", modUser.getFullName());
		} else {
			map.put("modUserName", "unknown");
		}
		map.put("type", this.getType());
		return map;
	 }

	public String getURI() throws DotDataException {
		if( UtilMethods.isSet(getIdentifier()) && UtilMethods.isSet(APILocator.getIdentifierAPI().find(getIdentifier()).getId())) {
			return APILocator.getIdentifierAPI().find(getIdentifier()).getURI();
		}
		Folder folder = Try.of(()->APILocator.getFolderAPI().find(getFolder(),APILocator.systemUser(),false)).getOrNull();


		if(folder == null) {
			return StringPool.BLANK;
		}

		String fileName = UtilMethods.isSet(this.getFileName())
				? this.getFileName()
				: UtilMethods.isSet(this.map.get("fileName"))
						? (String)this.map.get("fileName")
						: UtilMethods.isSet(this.getUnderlyingFileName())
							? this.getUnderlyingFileName()
							: StringPool.BLANK;
		return folder.getPath() + fileName;




	}

	public Date getIDate() {
		return getModDate();
	}

	@Override
	public String toString() {
		return this.getFileName();
	}

}
