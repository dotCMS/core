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
import com.dotmarketing.util.ImageUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.awt.Dimension;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Date;
import java.util.Map;

public class FileAsset extends Contentlet implements IFileAsset {

	private String metaData;

    private File file;

	public static final String UNKNOWN_MIME_TYPE = "unknown";

	public FileAsset() {
		super();

	}

	public String getMetaData(){
		if(metaData ==null){
			metaData=(String) super.get(FileAssetAPI.META_DATA_FIELD);
		}
		return metaData;

	}
	
	public long getLanguageId(){
		return super.getLanguageId();
	}



	public void setMetaData(String metaData) {
		this.metaData = metaData;
	}

	public void setMenuOrder(int sortOrder) {
		setLongProperty(FileAssetAPI.SORT_ORDER, new Long(sortOrder));

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

	private long fileSizeInternal = 0;

	public long getFileSize() {
		if(this.fileSizeInternal == 0) {
			final File fileAsset = getFileAsset();
			if(null != fileAsset){
			   this.fileSizeInternal = fileAsset.length();
			}
		}
		return this.fileSizeInternal > 0 ? this.fileSizeInternal : 0;
	}

	private Dimension fileDimension = new Dimension();

	public int getHeight() {
        try {
            if (fileDimension.height == 0) {
                // File dimension is not loaded and we need to load it
                fileDimension = ImageUtil.getInstance().getDimension(getFileAsset());
            }
        } catch (Exception e) {
            Logger.debug(this, "Error getting height for file asset, id: " + getIdentifier(), e);
        }

        return fileDimension.height;
    }

    public int getWidth() {
        try {
            if (fileDimension.width == 0) {
                // File dimension is not loaded and we need to load it
                fileDimension = ImageUtil.getInstance().getDimension(getFileAsset());
            }
        } catch (Exception e) {
            Logger.debug(this, "Error getting width for file asset, id: " + getIdentifier(), e);
        }

        return fileDimension.width;
    }

	/**
	 * This access the physical file on disk
	 * @param name
	 */
	public void setUnderlyingFileName(final String name) {

	    final File fileAsset = getFileAsset();
	    //For the sake of performance we do not rename a file to be the same as it was.
	    //This gets expensive when copyProperties are used on this bean.
		if(!fileAsset.getName().equals(name)){
	       fileAsset.renameTo(new File(fileAsset.getParent(),name));
	    }
	    this.underlyingFileName = null;
	}

  /**
   * This access the physical file on disk
   * 
   * @return
   */
  private String underlyingFileName = null;

	public String getUnderlyingFileName() {
		if (underlyingFileName != null) {
			return underlyingFileName;
		}
		final File fileAsset = getFileAsset();
		this.underlyingFileName = fileAsset != null ? fileAsset.getName() : null;
		return this.underlyingFileName;
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
		String mimeType = APILocator.getFileAssetAPI().getMimeType(getUnderlyingFileName());


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

	public File getFileAsset() {
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
