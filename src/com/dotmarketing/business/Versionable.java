package com.dotmarketing.business;

import java.util.Date;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

public interface Versionable {
	/**
	 * The VersionId is the identifier of the versionable web asset
	 * every version of the particular webasset will share the same
	 * versionId/Identifier
	 * @return
	 */
	public String getVersionId();
	/**
	 * sets the versionId (identifier) of the asset
	 * @param versionId
	 */
	public void setVersionId(String versionId);
	
	/**
	 * this returns the string "type" (the db table) of the versionable web asset. 
	 *  Contentlet will return 'Contentlet'
	 *  Folder will return folder
	 *  HTMLPage will return htmlpage
	 *  Template will return template
	 *  File will return file_asset
	 *  Link will return links
	 *  Container will return containers
	 * @return
	 */
	public String getVersionType();
	
	/**
	 * gets the inode (specific version id) of the versionable
	 * @return
	 */
	public String getInode();
	/**
	 * returns if the asset is archived or not
	 * @return
	 */
	public boolean isArchived() throws DotStateException, DotDataException, DotSecurityException;
	/**
	 * returns if the asset is working or not
	 * @return
	 */
	public boolean isWorking() throws DotStateException, DotDataException, DotSecurityException;
	/**
	 * returns if the asset is live or not
	 * @return
	 */
	public boolean isLive() throws DotStateException, DotDataException, DotSecurityException;
	/**
	 * returns the title of the specific asset
	 * @return
	 */
	public String getTitle() throws DotStateException, DotDataException, DotSecurityException;
	/**
	 * returns user who last modified the version
	 * @return
	 */
	public String getModUser();
	/**
	 * returns the timestamp of when the last modification was made
	 * @return
	 */
	public Date getModDate();
	/**
	 * returns if the asset is locked or not
	 * @return
	 */
	public boolean isLocked() throws DotStateException, DotDataException, DotSecurityException;
}
