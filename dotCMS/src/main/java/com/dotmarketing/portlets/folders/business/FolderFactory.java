package com.dotmarketing.portlets.folders.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.DotIdentifierStateException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * This Factory class allows you to interact with Folders in dotCMS.
 *
 * @author maria
 * @since Mar 22nd, 2012
 */
public abstract class FolderFactory {


	/**
	 * Deletes a folder.
	 * The folder reference is not explicitly deleted from the identifier table because there is
	 * a db trigger that executes the delete operation once the folder table is cleaned up.
	 * Only the Identifier Cache is flushed.
	 * @param folder
	 * @throws DotDataException
	 */
	abstract void delete(final Folder folder) throws DotDataException;

	abstract Folder find(String folderInode) throws  DotDataException;

	public abstract void save(Folder folderInode) throws DotDataException;
	
	abstract boolean exists(String folderInode) throws DotDataException;

	protected boolean isChildFolder(final Folder child, final Folder parent) throws DotIdentifierStateException, DotDataException, DotSecurityException{
		return false;
	}

	protected boolean matchFilter(Folder folder, String fileName){
		return false;
	}

	protected List<Treeable> getChildrenClass(Folder parent, Class clazz) throws DotStateException, DotDataException{
		return null;
	}

	protected List<Folder> getSubFoldersNameSort(Folder folder) throws DotStateException, DotDataException {
		return null;
	}

	protected List<Folder> getSubFoldersTitleSort(Folder folder) throws DotStateException, DotDataException {
		return null;
	}
	protected Folder findFolderByPath(String path, Host host) throws DotDataException {
		return null;
	}

	/**
	 *
	 * @param folder
	 * @return
	 * @throws DotStateException
	 * @throws DotDataException
	 *
	 * @deprecated use {@link #getSubFoldersTitleSort(Folder)}
	 */
	@Deprecated
	protected List<Folder> getSubFolders(Folder folder) throws DotStateException, DotDataException {
		return null;
	}

	protected Folder findSystemFolder() throws DotDataException {
		return null;
	}

	protected List<Inode> getMenuItems(Folder folder) throws DotStateException, DotDataException{
		return null;
	}

	abstract void copy(Folder folder, Host destination) throws DotDataException, DotSecurityException, DotStateException, IOException;

	abstract void copy(Folder folder, Folder destination) throws DotDataException, DotStateException, DotSecurityException, IOException;

    abstract boolean move(Folder folder, Host destination) throws DotDataException, DotSecurityException;

	abstract boolean move(Folder folder, Folder destination) throws DotDataException, DotSecurityException;

	protected boolean renameFolder(Folder folder, String newName, User ser, boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {
		return false;
	}

	protected List getMenuItems(Folder folder, int orderDirection) throws DotStateException, DotDataException{
		return null;
	}

	protected List getMenuItems(Host host) throws DotStateException, DotDataException, DotSecurityException {
		return null;
	}

	abstract void updateMovedFolderAssets(Folder folder) throws DotDataException, DotStateException, DotSecurityException;

	protected List<Folder> getFoldersByParent(Folder folder, User user, boolean respectFrontendRoles) throws DotDataException{
		return null;
	}
	protected List<Folder> findFoldersByHost(Host host) {
		return null;
	}
	protected List<Folder> findThemesByHost(Host host) {
		return null;
	}
	protected List<Folder> findSubFolders(final Host host, Boolean showOnMenu) {
		return null;
	}
	protected List<Folder> findSubFolders(final Folder folder, Boolean showOnMenu) throws DotStateException, DotDataException{
	    return null;
	}

	protected List<String> getEntriesTree(Folder mainFolder, String openNodes, String view, String content, String structureInode, Locale locale,
			TimeZone timeZone, Role[] roles, boolean isAdminUser, User user) throws DotStateException, DotDataException,DotSecurityException {
		return null;
	}

	protected List<String> getFolderTree(String openNodes, String view, String content, String structureInode, Locale locale, TimeZone timeZone,
			Role[] roles, boolean isAdminUser, User user) throws DotStateException, DotDataException, DotSecurityException {
		return null;
	}

	protected List<Treeable> getChildrenClass(Folder parent, Class clazz, ChildrenCondition cond, String orderby) throws DotStateException,DotDataException{
		return null;
	}

	protected List<Treeable> getChildrenClass(Folder parent, Class clazz, ChildrenCondition cond) throws DotStateException,DotDataException{
		return null;
	}

    protected List<Treeable> getChildrenClass ( Host host, Class clazz, ChildrenCondition cond ) throws DotStateException, DotDataException {
        return null;
    }

    protected List<Treeable> getChildrenClass ( Host host, Class clazz ) throws DotStateException, DotDataException {
        return null;
    }

	/**
	 * Updates folder's owner when a user is replaced by another
	 * @param userId ID of the user to be replace
	 * @param replacementUserId ID of the new folder's owner
	 * @throws DotDataException
	 */
    public abstract void updateUserReferences(String userId, String replacementUserId)
            throws DotDataException;

	/**
	 * Generates a report of the different Content Types living under a Folder, and the number of
	 * content items for each type. This implementation analyzes all child folders and their
	 * respective sub-child folders, not only the specified one.
	 *
	 * @param folderPath     The path of the folder to analyze.
	 * @param siteId         The ID of the Site that the folder lives in.
	 * @param orderBy        The column name used to order the results by.
	 * @param orderDirection The sort direction of the results.
	 * @param limit          The maximum number of results to return, for pagination purposes.
	 * @param offset         The page number of the results to return, for pagination purposes.
	 *
	 * @return A list of maps, each one representing a different Content Type and the total number
	 * of content items for each type.
	 *
	 * @throws DotDataException An error occurred when interacting with the database.
	 */
	abstract List<Map<String, Object>> getContentReport(final String folderPath, final String siteId, final String orderBy, final String orderDirection, final int limit, final int offset) throws DotDataException;

	/**
	 * Returns the total count of Content Types that live under the specified Folder and all of its
	 * sub-folders.
	 *
	 * @param folderPath The path of the folder to analyze.
	 * @param siteId     The ID of the Site that the folder lives in.
	 *
	 * @return The total count of Content Types that live under the specified Folder and all of its
	 * sub-folders
	 *
	 * @throws DotDataException An error occurred when interacting with the database.
	 */
	abstract int getContentTypeCount(final String folderPath, final String siteId) throws DotDataException;

}
