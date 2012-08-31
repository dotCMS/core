package com.dotmarketing.portlets.folders.business;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.DotIdentifierStateException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;

/**
 * @author margaret
 *
 */
 public interface FolderAPI   {

	public static final String SYSTEM_FOLDER = "SYSTEM_FOLDER";

	/**
	 * Find a folder by a Host and a path
	 * 
	 * @param path
	 * @param hostId
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotStateException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public abstract Folder findFolderByPath(String path, Host host, User user, boolean respectFrontEndPermissions)
			throws DotStateException, DotDataException, DotSecurityException;
	/**
	 * Find a folder by a Host and a path
	 * 
	 * @param path
	 * @param hostId
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotStateException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public abstract Folder findFolderByPath(String path, String hostid, User user, boolean respectFrontEndPermissions)
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
	public abstract boolean renameFolder(Folder folder, String newName, User user, boolean respectFrontEndPermissions)
			throws DotStateException, DotDataException, DotSecurityException;

	/**
	 * returns the parent folder for any given asset
	 * 
	 * @param asset
	 * @return
	 * @throws DotIdentifierStateException
	 * @throws DotDataException
	 */
	public abstract Folder findParentFolder(Treeable asset, User user, boolean respectFrontEndPermissions) throws DotIdentifierStateException,
			DotDataException, DotSecurityException;

	/**
	 * 
	 * @param folder
	 * @return List of sub folders for passed in folder
	 * @throws DotHibernateException
	 */
	public abstract List<Folder> findSubFolders(Folder folder, User user, boolean respectFrontEndPermissions) throws DotStateException,
			DotDataException, DotSecurityException;

	/**
	 * 
	 * @param folder
	 * @return List of sub folders for passed in folder
	 * @throws DotHibernateException
	 */
	public abstract List<Folder> findSubFolders(Host host, User user, boolean respectFrontEndPermissions) throws DotStateException,
			DotDataException, DotSecurityException;

	/**
	 * 
	 * @param folder
	 *            Recursively
	 * @return List of sub folders for passed in folder
	 * @throws DotHibernateException
	 */
	public abstract List<Folder> findSubFoldersRecursively(Folder folder, User user, boolean respectFrontEndPermissions)
			throws DotStateException, DotDataException, DotSecurityException;

	/**
	 * 
	 * @param folder
	 * @return List of sub folders for passed in folder
	 * @throws DotHibernateException
	 */
	public abstract List<Folder> findSubFoldersRecursively(Host host, User user, boolean respectFrontEndPermissions)
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
	public abstract void copy(Folder folderToCopy, Folder newParentFolder, User user, boolean respectFrontEndPermissions)
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
	public abstract void copy(Folder folderToCopy, Host newParentHost, User user, boolean respectFrontEndPermissions)
			throws DotDataException, DotSecurityException, DotStateException, IOException;

	/**
	 * Does a folder already exist?
	 * 
	 * @param path
	 * @param hostId
	 * @return
	 * @throws DotHibernateException
	 * @throws DotDataException 
	 */
	public abstract boolean exists(String folderInode) throws DotDataException;

	/**
	 * Does a folder already exist?
	 * 
	 * @param path
	 * @param hostId
	 * @return
	 * @throws DotHibernateException
	 */
	public abstract List<Inode> findMenuItems(Folder folder, User user, boolean respectFrontEndPermissions) throws DotStateException,
			DotDataException;

	/**
	 * Takes a folder and a user and deletes all underneath assets User needs
	 * edit permssions on folder to delete everything underneath
	 * 
	 * @param folder
	 * @param user
	 * @param respectFrontEndPermissions
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public abstract void delete(Folder folder, User user, boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException;

	/**
	 * Gets a list of 'working' HTMLPages under given folder
	 * 
	 * @param parent
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotStateException
	 * @throws DotDataException
	 */
	public abstract List<HTMLPage> getHTMLPages(Folder parent, User user, boolean respectFrontEndPermissions) throws DotStateException,
			DotDataException, DotSecurityException;

    /**
	 * Gets a list of 'working' HTMLPages under a given host
	 *
	 * @param host
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotStateException
	 * @throws DotDataException
	 */
	public abstract List<HTMLPage> getHTMLPages(Host host, User user, boolean respectFrontEndPermissions) throws DotStateException,
			DotDataException, DotSecurityException;

	public  List<HTMLPage> getHTMLPages(Folder parent, boolean working, boolean deleted, User user, boolean respectFrontEndPermissions) throws DotStateException,
            DotDataException, DotSecurityException;

    /**
     * Gets a list of HTMLPages under a given host
     *
     * @param host
     * @param working
     * @param deleted
     * @param user
     * @param respectFrontEndPermissions
     * @return
     * @throws DotStateException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public List<HTMLPage> getHTMLPages ( Host host, boolean working, boolean deleted, User user, boolean respectFrontEndPermissions ) throws DotStateException,
            DotDataException, DotSecurityException;


    /**
	 * Gets a list of 'working' Links under given folder
	 * 
	 * @param parent
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotStateException
	 * @throws DotDataException
	 */
	public abstract List<Link> getLinks(Folder parent, User user, boolean respectFrontEndPermissions) throws DotStateException,
			DotDataException, DotSecurityException;
	
	public  List<Link> getLinks(Folder parent, boolean working, boolean deleted, User user, boolean respectFrontEndPermissions) throws DotStateException,
    DotDataException, DotSecurityException;


	/**
	 * Gets a list of  'working' File under given folder
	 * 
	 * @param parent
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotStateException
	 * @throws DotDataException
	 */
	public abstract List<File> getFiles(Folder parent, User user, boolean respectFrontEndPermissions) throws DotStateException,
			DotDataException, DotSecurityException;
	
	/**
	 * Gets a list of  'working' File under given folder
	 * 
	 * @param parent
	 * @param user
	 * @param respectFrontEndPermissions
	 * @param condition
	 * @return
	 * @throws DotStateException
	 * @throws DotDataException
	 */
	public abstract List<File> getFiles(Folder parent, User user, boolean respectFrontEndPermissions, ChildrenCondition cond) throws DotStateException,
			DotDataException, DotSecurityException;


	/**
	 * Gets a list of  'working'  Contentlet under given folder
	 * 
	 * @param parent
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotStateException
	 * @throws DotDataException
	 */
	public abstract List<Contentlet> getContent(Folder parent, User user, boolean respectFrontEndPermissions) throws DotStateException,
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
	public abstract List<Structure> getStructures(Folder parent, User user, boolean respectFrontEndPermissions) throws DotStateException,
			DotDataException, DotSecurityException;

	/**
	 * find will hit first cache, and then db
	 * 
	 * @param id
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotHibernateException
	 * @throws DotDataException 
	 */
	public abstract Folder find(String id, User user, boolean respectFrontEndPermissions) throws DotHibernateException,
			DotSecurityException, DotDataException;

	/**
	 * Saves a folder. The folder needs to have been created from the
	 * createFolder method, which will give it a valid identifier identifier
	 * 
	 * @param folder
	 * @param user
	 * @param respectFrontEndPermissions
	 * @throws DotHibernateException
	 * @throws DotSecurityException
	 * @throws DotDataException 
	 */
	public abstract void save(Folder folder, User user, boolean respectFrontEndPermissions) throws DotHibernateException,
			DotSecurityException, DotDataException;

	// http://jira.dotmarketing.net/browse/DOTCMS-3232
	public abstract Folder findSystemFolder() throws DotDataException;

	/**
	 * This method returns a new folder or the folder on the path you have
	 * passed in
	 * 
	 * @param path
	 * @param host
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotHibernateException
	 * @throws DotSecurityException
	 */
	public abstract Folder createFolders(String path, Host host, User user, boolean respectFrontEndPermissions)
			throws DotHibernateException, DotSecurityException, DotDataException;

	/**
	 * Pulls a complete list of all folders on a host
	 * 
	 * @param host
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotHibernateException
	 * @throws DotSecurityException
	 */
	public abstract List<Folder> findFoldersByHost(Host host, User user, boolean respectFrontEndPermissions) throws DotHibernateException,
			DotSecurityException;

	/**
	 * Required by CMIS
	 * 
	 * @param query
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws ValidationException
	 */
	public abstract List<Map<String, Serializable>> DBSearch(Query query, User user, boolean respectFrontendRoles)
			throws ValidationException, DotDataException;
	
	/**Checks if folder1 is child of folder2
	 * 
	 * @param folder1
	 * @param folder2
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 * @throws DotSecurityException
	 */
	public abstract boolean isChildFolder(Folder folder1,Folder folder2) throws DotDataException, DotSecurityException;
	
	/**
	 * 
	 * 
	 * @param folder
	 * @param fileName
	 * @return
	 */
	public abstract boolean matchFilter(Folder folder, String fileName);
	
	/**
	 * Find the sorted Items of a folder 
	 * 
	 * @param folder
	 * @param orderDirection
	 * @return
	 * @throws DotDataException
	 */
	public abstract List<Inode> findMenuItems(Folder folder, int orderDirection) throws DotDataException;
	
	/**
	 * Builds the Navigation Tree with the items 
	 * 
	 * @param items
	 * @param depth
	 * @param user
	 * @return
	 * @throws DotDataException
	 */
	public abstract List<Object> buildNavigationTree(List items, int depth,User user) throws DotDataException; 
	
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
	public abstract List<Inode> findMenuItems(Host host,User user,boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException;
	
	/** 
	 * Find subFolders of a folder sort by Title
	 * 
	 * @param folder
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotDataException
	 */
	public abstract List<Folder> findSubFoldersTitleSort(Folder folder,User user,boolean respectFrontEndPermissions) throws DotDataException;
	
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
	public abstract boolean move(Folder folderToMove, Folder newParentFolder,User user,boolean respectFrontEndPermissions)throws DotDataException, DotSecurityException;
	
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
	public abstract boolean move(Folder folderToMove, Host newParentHost,User user,boolean respectFrontEndPermissions)throws DotDataException, DotSecurityException;
	
	/**
	 * 
	 * @param host
	 * @param showOnMenu
	 * @return
	 * @throws DotHibernateException
	 */
	public abstract List<Folder> findSubFolders(Host host,boolean showOnMenu) throws DotHibernateException;
	
	/**
	 * 
	 * @param folder
	 * @param showOnMenu
	 * @return
	 * @throws DotStateException
	 * @throws DotDataException
	 */
	public abstract List<Folder> findSubFolders(Folder folder,boolean showOnMenu) throws DotStateException, DotDataException;
	
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
	public abstract List<String> getEntriesTree(Folder mainFolder, String openNodes, String view, String content, String structureInode,User user) throws DotStateException, DotDataException,DotSecurityException;
	
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
	public abstract List<String> getFolderTree(String openNodes, String view, String content, String structureInode,User user) throws DotStateException, DotDataException, DotSecurityException;
	
	/**
	 * 
	 * @param parent
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public abstract List<HTMLPage> getWorkingHTMLPages(Folder parent,User user, boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException;
	
	/**
	 * 
	 * @param parent
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public abstract List<Link> getWorkingLinks(Folder parent,User user, boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException;
	
	/**
	 * 
	 * @param parent
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public abstract List<File> getWorkingFiles(Folder parent,User user, boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException;
	
	/**
	 * 
	 * @param parent
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public abstract List<Contentlet> getWorkingContent(Folder parent,User user, boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException;
	
	/**
	 * 
	 * @param parent
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public abstract List<HTMLPage> getLiveHTMLPages(Folder parent,User user, boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException;
	
	/**
	 * 
	 * @param parent
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public abstract List<Link> getLiveLinks(Folder parent,User user, boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException;
	
	/**
	 * 
	 * @param parent
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public abstract List<File> getLiveFiles(Folder parent,User user, boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException;
	/**
	 * returns child files sorted by title
	 * @param parent
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public abstract List<File> getLiveFilesSortTitle(Folder parent,User user, boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException;
	/**
	 * returns child files sorted by title
	 * @param parent
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public abstract List<File> getLiveFilesSortOrder(Folder parent,User user, boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException;
	
	
	/**
	 * 
	 * @param parent
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public abstract List<Contentlet> getLiveContent(Folder parent,User user, boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException;
	
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
	public abstract boolean updateIdentifierUrl(Folder folderToMove, Folder newParentFolder,User user,boolean respectFrontEndPermissions)throws DotDataException, DotSecurityException;
	
}
