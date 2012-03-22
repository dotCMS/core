package com.dotmarketing.portlets.fileassets.business;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;

public interface IFileAsset {

	public String getVersionId();

	public boolean isDeleted() throws DotStateException, DotDataException, DotSecurityException;

	public boolean isArchived() throws DotStateException, DotDataException, DotSecurityException;

	public String getInode();

	public void setInode(String inode);

	public boolean isLive() throws DotStateException, DotDataException, DotSecurityException;

	public boolean isLocked() throws DotStateException, DotDataException, DotSecurityException;

	public java.util.Date getModDate();

	public String getModUser();

	public boolean isWorking() throws DotStateException, DotDataException, DotSecurityException;
	
	public void setModDate(java.util.Date modDate);

	public void setModUser(String modUser);

	public abstract String getURI(Folder folder);

	public boolean isShowOnMenu();

	public int getMenuOrder();

	public String getTitle();

	public void setShowOnMenu(boolean showOnMenu);

	public void setMenuOrder(int sortOrder);

	public void setTitle(String title);

	public String getFriendlyName();

	public void setFriendlyName(String friendlyName);

	public String getOwner();

	public String getPath();

	public String getParent();

	public Permissionable getParentPermissionable() throws DotDataException;

	public long getFileSize();

	public void setFileName(String name);

	public String getFileName();

	public String getMimeType();

	public void setMimeType(String mimeType);

	public Map<String, Object> getMap() throws DotRuntimeException, DotDataException, DotSecurityException;

	public String getPermissionId();
	
	public Date getIDate();

	public String getVersionType();

	public InputStream getFileInputStream() throws FileNotFoundException;

	public File getFileAsset();
	
	public String getType();
	
	public String getURI() throws DotDataException;
	
	public String getExtension();

}
