package com.dotmarketing.portlets.fileassets.business;

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
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.awt.Dimension;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.Date;
import java.util.Map;

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
        return Try.of(() -> super.getBinaryMetadata(FileAssetAPI.BINARY_FIELD).getFieldsMeta())
                .getOrElse(ImmutableMap.of());
    }

	public long getLanguageId(){
		return super.getLanguageId();
	}

	public void setMenuOrder(int sortOrder) {
		setLongProperty(FileAssetAPI.SORT_ORDER, (long) sortOrder);

	}

	public int getMenuOrder() {
		return new Integer(String.valueOf(getLongProperty(FileAssetAPI.SORT_ORDER)));
	}

	private static final long serialVersionUID = 1L;


	public String getPath() {
		Identifier id = null;
		try{
			id = APILocator.getIdentifierAPI().find(this.getIdentifier());
			return id.getParentPath();
		}catch(Exception e){
			Logger.error(this, e.getMessage(), e);
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
		return 	Try.of(() -> Integer.parseInt(getMetaDataMap().get("fileSize").toString())).getOrElse(0);
	}

	public int getHeight() {
        return computeDimensions().height;
	}

	public int getWidth() {
		return computeDimensions().width;
	}

	private Dimension computeDimensions() {
	   try {
           final Map<String, Serializable> metaDataMap = getMetaDataMap();
           final int height = Integer.parseInt(metaDataMap.get("height").toString());
           final int width = Integer.parseInt(metaDataMap.get("width").toString());
           return new Dimension(width, height);
       }catch (Exception e){
          return new Dimension(0, 0);
       }
    }

  /**
   * This gives you access to the physical file on disk.
   * @return
   */
  public String getUnderlyingFileName() {
	  return Try.of(() -> getMetaDataMap().get("title").toString())
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

		String mimeType = Try.of(() -> getMetaDataMap().get("contentType").toString()).getOrNull();
		if(null != mimeType){
		   return mimeType;
		}

		mimeType = APILocator.getFileAssetAPI().getMimeType(getUnderlyingFileName());

		if (mimeType == null || UNKNOWN_MIME_TYPE.equals(mimeType)){
			mimeType = "application/octet-stream";
		}

		return mimeType;
	}

	public void setMimeType(String mimeType) {

	}

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

	public File getFileAsset() {
		// calling getBinary can be relatively expensive since it constantly verifies the existence of the file on disk.
		// Therefore we'll keep a file reference at hand.
		if (null == file) {
			try {
				file = getBinary(FileAssetAPI.BINARY_FIELD);
			} catch (IOException e) {
				throw new DotStateException("Unable to find the fileAsset for :" + this.getInode());
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

	 public Map<String, Object> getMap() throws DotRuntimeException {
		Map<String,Object> map = super.getMap();
		boolean live =  false;
		boolean working =  false;
		boolean deleted = false;
		boolean locked  = false;
		try{
			live =  isLive();
			working = isWorking();
			deleted = isDeleted();
			locked  = isLocked();
		}catch(Exception e){
			Logger.error(this, e.getMessage(), e);
		}
		map.put("extension", getExtension());
		map.put("live", live);
		map.put("working", working);
		map.put("deleted", deleted);
		map.put("locked", locked);
		map.put("isContent", true);
		map.put("fileAssetType", this.getStructureInode());
		map.put("friendlyName", getStringProperty(FileAssetAPI.DESCRIPTION));
		map.put("mimeType", getMimeType());
		User modUser = null;
		try {
			modUser = APILocator.getUserAPI().loadUserById(this.getModUser(),APILocator.getUserAPI().getSystemUser(),false);
		} catch (NoSuchUserException | DotDataException e) {
			Logger.debug(this, e.getMessage());
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
		}
		if (UtilMethods.isSet(modUser) && UtilMethods.isSet(modUser.getUserId()) && !modUser.isNew())
			map.put("modUserName", modUser.getFullName());
		else
			map.put("modUserName", "unknown");

		 map.put("type", this.getType());
		return map;
	 }

	public String getURI() throws DotDataException {
		return UtilMethods.isSet(getIdentifier()) ?
		        APILocator.getIdentifierAPI().find(getIdentifier()).getURI()
		       : StringPool.BLANK;

	}

	public Date getIDate() {
		// TODO Auto-generated method stub
		return getModDate();
	}

	@Override
	public String toString() {
		return this.getFileName();
	}

}
