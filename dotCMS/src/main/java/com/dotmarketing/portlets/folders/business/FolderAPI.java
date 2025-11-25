package com.dotmarketing.portlets.folders.business;

import com.dotcms.api.tree.Parentable;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.DotIdentifierStateException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Encapsulates operations for folders on dotCMS, can copy, find, delete, create, etc.
 * @author margaret
 *
 */
 public interface FolderAPI   {

	String SYSTEM_FOLDER = "SYSTEM_FOLDER";
	String SYSTEM_FOLDER_ID = "bc9a1d37-dd2d-4d49-a29d-0c9be740bfaf";
	String SYSTEM_FOLDER_ASSET_NAME = "system folder";
	String SYSTEM_FOLDER_PARENT_PATH = "/System folder";

	/**
	 * Find a folder by a Host and a path
	 *
	 * @param path {@link String}
	 * @param host {@link Host}
	 * @param user {@link User}
	 * @param respectFrontEndPermissions {@link Boolean}
	 * @return Folder
	 * @throws DotStateException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	Folder findFolderByPath(String path, Host host, User user, boolean respectFrontEndPermissions)
			throws DotStateException, DotDataException, DotSecurityException;

	/**
	 * Find a folder by a Host and a path
	 *
	 * @param path
	 * @param hostid
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotStateException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	Folder findFolderByPath(String path, String hostid, User user, boolean respectFrontEndPermissions)
	throws DotStateException, DotDataException, DotSecurityException;



	/**
	 * rename folder
	 *
	 * @param folder
	 * @param newName
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotStateException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	boolean renameFolder(Folder folder, String newName, User user, boolean respectFrontEndPermissions)
			throws DotStateException, DotDataException, DotSecurityException;

	/**
	 * returns the parent folder for any given asset
	 *
	 * @param asset
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotIdentifierStateException
	 * @throws DotDataException
	 */
	Folder findParentFolder(Treeable asset, User user, boolean respectFrontEndPermissions) throws DotIdentifierStateException,
			DotDataException, DotSecurityException;

	/**
	 *
	 * @param folder
	 * @return List of sub folders for passed in folder
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	List<Folder> findSubFolders(Folder folder, User user, boolean respectFrontEndPermissions) throws DotStateException,
			DotDataException, DotSecurityException;

	/**
	 *
	 * @param host
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return List of sub folders for passed in folder
	 * @throws DotStateException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	List<Folder> findSubFolders(Host host, User user, boolean respectFrontEndPermissions) throws DotStateException,
	DotDataException, DotSecurityException;

	/**
	 *
	 * @param host
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return List of themes for passed in host
	 * @throws DotStateException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	List<Folder> findThemes(Host host, User user, boolean respectFrontEndPermissions) throws DotStateException,
			DotDataException, DotSecurityException;

	/**
	 *
	 * @param folder
	 *            Recursively
	 * @return List of sub folders for passed in folder
	 * @throws DotStateException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	List<Folder> findSubFoldersRecursively(Folder folder, User user, boolean respectFrontEndPermissions)
			throws DotStateException, DotDataException, DotSecurityException;

	/**
	 *
	 * @param host
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return List of sub folders for passed in folder
	 * @throws DotStateException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	List<Folder> findSubFoldersRecursively(Host host, User user, boolean respectFrontEndPermissions)
			throws DotStateException, DotDataException, DotSecurityException;

	/**
	 * Will copy a folder to a new folder with all it contains.
	 *
	 * @param folderToCopy
	 * @param newParentFolder
	 * @param user
	 * @param respectFrontEndPermissions
	 * @throws DotDataException
	 * @throws IOException
	 * @throws DotStateException
	 */
	void copy(Folder folderToCopy, Folder newParentFolder, User user, boolean respectFrontEndPermissions)
			throws DotDataException, DotSecurityException, DotStateException, IOException;

	/**
	 * Copies a folder to the root of another host
	 *
	 * @param folderToCopy
	 * @param newParentHost
	 * @param user
	 * @param respectFrontEndPermissions
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws IOException
	 * @throws DotStateException
	 */
	void copy(Folder folderToCopy, Host newParentHost, User user, boolean respectFrontEndPermissions)
			throws DotDataException, DotSecurityException, DotStateException, IOException;

	/**
	 * Does a folder already exist?
	 *
	 * @param folderInode or folderID
	 * @return boolean
	 * @throws DotDataException
	 */
	boolean exists(String folderInode) throws DotDataException;

	/**
	 * Does a folder already exist?
	 *
	 * @param folder
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return List of Inode
	 * @throws DotStateException
	 * @throws DotDataException
	 */
	List<Inode> findMenuItems(Folder folder, User user, boolean respectFrontEndPermissions) throws DotStateException,
			DotDataException;

	/**
	 * Takes a folder and a user and deletes all underneath assets User needs
	 * edit permissions on folder to delete everything underneath
	 *
	 * @param folder
	 * @param user
	 * @param respectFrontEndPermissions
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	void delete(Folder folder, User user, boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException;


    /**
     * Gets a list of 'working' Links under given folder
     *
     * @param parent
     * @param user
     * @param respectFrontEndPermissions
     * @return List of Link
     * @throws DotStateException
     * @throws DotDataException
     */
    public abstract List<Link> getLinks ( Folder parent, User user, boolean respectFrontEndPermissions ) throws DotStateException,
            DotDataException, DotSecurityException;

    /**
     * Gets a list of 'working' Links under given host
     *
     * @param host
     * @param user
     * @param respectFrontEndPermissions
     * @return List of Link
     * @throws DotStateException
     * @throws DotDataException
     */
    List<Link> getLinks ( Host host, User user, boolean respectFrontEndPermissions ) throws DotStateException,
            DotDataException, DotSecurityException;

    List<Link> getLinks ( Folder parent, boolean working, boolean deleted, User user, boolean respectFrontEndPermissions ) throws DotStateException,
            DotDataException, DotSecurityException;

    List<Link> getLinks ( Host host, boolean working, boolean deleted, User user, boolean respectFrontEndPermissions ) throws DotStateException,
            DotDataException, DotSecurityException;


	/**
	 * Gets a list of  'working'  Contentlet under given folder
	 *
	 * @deprecated use {@link com.dotmarketing.portlets.contentlet.business.ContentletAPI#findContentletsByFolder(Folder,
	 * User, boolean)} instead
	 */
	@Deprecated
	List<Contentlet> getContent(Folder parent, User user, boolean respectFrontEndPermissions) throws DotStateException,
			DotDataException, DotSecurityException;

	/**
	 * Gets a list of Structure under given folder
	 *
	 * @param parent
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotStateException
	 * @throws DotDataException
	 */
	List<Structure> getStructures(Folder parent, User user, boolean respectFrontEndPermissions) throws DotStateException,
	DotDataException, DotSecurityException;


	/**
	 * find will hit first cache, and then db
	 *
	 * @param id
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	Folder find(String id, User user, boolean respectFrontEndPermissions) throws
			DotSecurityException, DotDataException;

	/**
	 * Validates that the folder name is not a reserved word
	 * @param folder folder whose name will be validated
	 * @throws DotDataException
	 */
	void validateFolderName(final Folder folder) throws DotDataException;

	/**
	 * Saves a folder. The folder needs to have been created from the
	 * createFolder method, which will give it a valid identifier identifier
	 *
	 * @param folder
	 * @param user
	 * @param respectFrontEndPermissions
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	void save(Folder folder, User user, boolean respectFrontEndPermissions) throws
			DotSecurityException, DotDataException;

	/**
	 * Saves a folder. The folder needs to have been created from the
	 * createFolder method, which will give it a valid identifier identifier
	 * @param folder
	 * @param existingId
	 * @param user
	 * @param respectFrontEndPermissions
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	void save(Folder folder,String existingId, User user, boolean respectFrontEndPermissions) throws
	DotSecurityException, DotDataException;

	// http://jira.dotmarketing.net/browse/DOTCMS-3232
	Folder findSystemFolder() ;

	/**
	 * This method returns a new folder or the folder on the path you have
	 * passed in
	 *
	 * @param path
	 * @param host
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotSecurityException
	 * 	 * @throws DotDataException
	 */
	Folder createFolders(String path, Host host, User user, boolean respectFrontEndPermissions)
			throws DotSecurityException, DotDataException;

	/**
	 * Pulls a complete list of all folders on a host
	 *
	 * @param host
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotSecurityException
	 */
	List<Folder> findFoldersByHost(Host host, User user, boolean respectFrontEndPermissions) throws
			DotSecurityException;

	/**
	 *
	 * @param query
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws ValidationException
	 * @throws DotDataException
	 */
	List<Map<String, Serializable>> DBSearch(Query query, User user, boolean respectFrontendRoles)
			throws ValidationException, DotDataException;

	/**
     * Checks if childFolder is child of parentFolder
	 *
	 * @param childFolder {@link Folder}
	 * @param parentFolder  {@link Folder}
	 * @return boolean
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotSecurityException
	 */
	boolean isChildFolder(final Folder childFolder, final Folder parentFolder) throws DotDataException, DotSecurityException;

	/**
	 *
	 *
	 * @param folder
	 * @param fileName
	 * @return
	 */
	boolean matchFilter(Folder folder, String fileName);

	/**
	 * Find the sorted Items of a folder
	 *
	 * @param folder
	 * @param orderDirection
	 * @return
	 * @throws DotDataException
	 */
	List<Inode> findMenuItems(Folder folder, int orderDirection) throws DotDataException;

	/**
	 * Find the Items of a host
	 *
	 * @param host
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	List<Inode> findMenuItems(Host host,User user,boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException;

	/**
	 * Find subFolders of a folder sort by Title
	 *
	 * @param folder
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotDataException
	 */
	List<Folder> findSubFoldersTitleSort(Folder folder,User user,boolean respectFrontEndPermissions) throws DotDataException;

	/**
	 * Will move a folder to a new folder with all it contains.
	 *
	 * @param folderToMove
	 * @param newParentFolder
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	boolean move(Folder folderToMove, Folder newParentFolder,User user,boolean respectFrontEndPermissions)throws DotDataException, DotSecurityException;

	/**
	 * Moves a folder to the root of another host
	 *
	 * @param folderToMove
	 * @param newParentHost
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	boolean move(Folder folderToMove, Host newParentHost,User user,boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException;

	/**
	 * Move a folder identify by identifier, to new path
	 * @param folderId       {@link String} current folder id
	 * @param newFolderId    {@link String} new folder id path to move
	 * @param user			 {@link User}   current user
	 * @param respectFrontendRoles {@link Boolean}
	 * @return boolean true if the move is successfully
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	boolean move (final String folderId, final String newFolderId,
							   final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 *
	 * @param host
	 * @param showOnMenu
	 * @return
	 */
	List<Folder> findSubFolders(Host host,boolean showOnMenu);

	/**
	 *
	 * @param folder
	 * @param showOnMenu
	 * @return
	 * @throws DotStateException
	 * @throws DotDataException
	 */
	List<Folder> findSubFolders(Folder folder,boolean showOnMenu) throws DotStateException, DotDataException;

	/**
	 *
	 * @param mainFolder
	 * @param openNodes
	 * @param view
	 * @param content
	 * @param structureInode
	 * @param user
	 * @return
	 * @throws DotStateException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	List<String> getEntriesTree(Folder mainFolder, String openNodes, String view, String content, String structureInode,User user) throws DotStateException, DotDataException,DotSecurityException;

	/**
	 *
	 * @param openNodes
	 * @param view
	 * @param content
	 * @param structureInode
	 * @param user
	 * @return
	 * @throws DotStateException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	List<String> getFolderTree(String openNodes, String view, String content, String structureInode,User user) throws DotStateException, DotDataException, DotSecurityException;

	/**
	 *
	 * @param parent
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	List<Link> getWorkingLinks(Folder parent,User user, boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException;

	/**
	 *
	 * @param parent
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	List<Contentlet> getWorkingContent(Folder parent,User user, boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException;

	/**
	 *
	 * @param parent
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	List<Link> getLiveLinks(Folder parent,User user, boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException;

	/**
	 *
	 * @param parent
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	List<Contentlet> getLiveContent(Folder parent,User user, boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException;

	/**
	 * Subscribe a listener to handle changes over the folder
	 * @param folder {@link Folder}
	 * @param folderListener folderListener
	 */
	void subscribeFolderListener (final Folder folder, final FolderListener folderListener);

	/**
	 * Subscribe a listener to handle changes over the folder, this one filters the child events by name.
	 * @param @param folder {@link Folder}
	 * @param folderListener folderListener
	 * @param childNameFilter {@link Predicate} filter
	 */
	void subscribeFolderListener (final Folder folder, final FolderListener folderListener, final Predicate<String> childNameFilter);

    /**
     *
     * @param parent (host or folder)
     * @return List of sub folders for passed in folder regardless the show on menu boolean
     * @throws DotDataException
     * @throws DotSecurityException
     */
    List<Folder> findSubFoldersByParent(Parentable parent, User user, boolean respectFrontEndPermissions)
            throws DotDataException, DotSecurityException;

	/**
	 * Updates folder's owner when a user is replaced by another
	 * @param userId ID of the user to be replace
	 * @param replacementUserId ID of the new folder's owner
	 * @throws DotDataException
	 */
    void updateUserReferences(String userId, String replacementUserId)
            throws DotDataException;

	/**
	 * Generates a report of the different Content Types living under a Folder, and the number of
	 * content items for each type. This implementation analyzes all child folders and their
	 * respective sub-child folders, not only the specified one.
	 *
	 * @param folder         The {@link Folder} that the report will be generated for.
	 * @param orderBy        The column name to order the report by. Usually set to
	 *                       {@code "LOWER(name)"}.
	 * @param orderDirection The direction to order the report by: {@code "ASC"} or {@code "DESC"}.
	 * @param limit          The maximum number of records to return, for pagination purposes.
	 * @param offset         The page number of the returned results, for pagination purposes.
	 * @param user           The {@link User} that is requesting the report.
	 *
	 * @return A list of maps with the data for generating the Content Report.
	 *
	 * @throws DotDataException An error occurred while retrieving the data from the database.
	 */
	List<Map<String, Object>> getContentReport(final Folder folder, final String orderBy,
											   final String orderDirection, final int limit,
											   final int offset, final User user) throws DotDataException;

	/**
	 * Returns the total count of Content Types that live under the specified Folder and all of its
	 * sub-folders.
	 *
	 * @param folder                     The {@link Folder} that the Content Type count will be
	 *                                   generated for.
	 * @param user                       The {@link User} that is calling this method.
	 * @param respectFrontEndPermissions If front-end Roles for the specified User must be
	 *                                   validated, set this to {@code true}.
	 *
	 * @return The total count of Content Types that live under the specified Folder and all of its
	 * sub-folders.
	 *
	 * @throws DotDataException     An error occurred when interacting with the database.
	 * @throws DotSecurityException The specified User does not have the necessary permissions to
	 *                              access the data.
	 */
	int getContentTypeCount(final Folder folder, final User user,
							final boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException;

}
