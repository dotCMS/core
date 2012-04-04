package com.dotmarketing.portlets.fileassets.business;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class FileAsset extends Contentlet implements IFileAsset {

	public FileAsset() {
		super();

	}

	public void setMenuOrder(int sortOrder) {
		setLongProperty(FileAssetAPI.SORT_ORDER, new Long(sortOrder));

	}

	public int getMenuOrder() {
		return new Integer(String.valueOf(getLongProperty(FileAssetAPI.SORT_ORDER)));
	}

	private static final long serialVersionUID = 1L;

	private String path;

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
		if ("SYSTEM_FOLDER".equals(getFolder())) {
			return getHost();
		} else {
			return getFolder();
		}

	}

	public long getFileSize() {
		return getFileAsset().length();
	}

	public void setFileName(String name) {
	    File ff=getFileAsset();
	    ff.renameTo(new File(ff.getParent(),name));
	}

	public String getFileName() {
		return (getFileAsset()!=null)?getFileAsset().getName(): null;
	}

	public String getMimeType() {
		String mimeType = APILocator.getFileAPI().getMimeType(getFileName());
		if (mimeType == null)
			mimeType = "application/octet-stream";
		return mimeType;
	}

	public void setMimeType(String mimeType) {

	}

	public InputStream getFileInputStream() throws FileNotFoundException {
		return new BufferedInputStream(new FileInputStream(getFileAsset()));
	}

	public File getFileAsset() {
		try {
			return getBinary(FileAssetAPI.BINARY_FIELD);
		} catch (IOException e) {
			throw new DotStateException("Unable to find the fileAsset for :" + this.getInode());
		}
	}

	public boolean isDeleted() throws DotStateException, DotDataException, DotSecurityException {
		return super.isArchived();
	}

	public String getURI(Folder folder) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isShowOnMenu() {
		String isShowOnMenu = super.getStringProperty("showOnMenu");//DOTCMS-6968
		if(isShowOnMenu.contains("true")){
			return true;
		}else{
			return false;
		}
	}

	public void setShowOnMenu(boolean showOnMenu) {
		super.setBoolProperty("showOnMenu", showOnMenu);

	}

	public void setSortOrder(int sortOrder) {
		super.setSortOrder(sortOrder);

	}

	public void setTitle(String title) {
		super.setStringProperty(title, title);

	}

	public String getFriendlyName() {
		return super.getStringProperty("title");
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
       return APILocator.getVersionableAPI().isLocked(getIdentifier());
   }


	public String getType(){
		return "file_asset";
	}

	 public String getExtension(){
		 return UtilMethods.getFileExtension(getFileName());

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
		User modUser = null;
		try {
			modUser = APILocator.getUserAPI().loadUserById(this.getModUser(),APILocator.getUserAPI().getSystemUser(),false);
		} catch (NoSuchUserException e) {
			Logger.debug(this, e.getMessage());
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
		}
		if (UtilMethods.isSet(modUser) && UtilMethods.isSet(modUser.getUserId()) && !modUser.isNew())
			map.put("modUserName", modUser.getFullName());
		else
			map.put("modUserName", "unknown");

		return map;
	 }

	public String getURI() throws DotDataException {
		return UtilMethods.isSet(getIdentifier()) ?
		        APILocator.getIdentifierAPI().find(getIdentifier()).getURI()
		       : "";

	}

	public Date getIDate() {
		// TODO Auto-generated method stub
		return getModDate();
	}

}
