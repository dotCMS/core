package com.dotmarketing.portlets.files.business;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;

public interface FileFactory {
	
	/**
	 * Saves a file, new or a version
	 * @param newFile
	 * @param dataFile
	 * @param folder
	 * @param identifier
	 * @param user
	 * @return
	 * @throws Exception
	 */
	public File saveFile (File newFile, java.io.File dataFile, Folder parentFolder, Identifier identifier)  throws DotDataException ;
	/**
	 * Delete file from persistent repository.
	 * 
	 * @param file
	 * @throws DotDataException
	 */
	void delete(File file)  throws DotDataException, DotStateException, DotSecurityException;
	
	/**
	 * Delete file from cache.
	 * 
	 * @param file
	 * @throws DotDataException
	 */
	void deleteFromCache(File file)  throws DotDataException, DotStateException, DotSecurityException;
	
	/**
	 * Retrieves the list of all files attached to a given host
	 * @param host The parent host
	 * @param live If true it will return the live versions of the files
	 * @return
	 * @throws DotDataException
	 */
	List<File> getAllHostFiles(Host host, boolean live) throws DotDataException;
	
	/**
	 * Gets the file of name under the folder
	 * @param fileName
	 * @param folder
	 * @return
	 * @throws DotDataException
	 */
	public File getWorkingFileByFileName(String fileName, Folder folder) throws DotDataException;
	
	
	/**
	 * Gets the file of name under the folder
	 * @param fileName
	 * @param folder
	 * @return
	 * @throws DotDataException
	 */
	public File getLiveFileByFileName(String fileName, Folder folder) throws DotDataException;
	
	/**
	 * Returns the object File with the specified inode
	 * 
	 * @param inode
	 * @return File
	 * @throws DotHibernateException
	 * @throws DotDataException 
	 * @throws DotStateException 
	 */
	File get(String inode) throws DotStateException, DotDataException, DotSecurityException;
	
	/**
	 * Returns the parent folder of a given file
	 * @param file
	 * @return
	 * @throws DotDataException
	 */
	Folder getFileFolder(File file,String hostId) throws DotDataException;
	
	 boolean fileNameExists(Folder folder, String fileName) throws DotStateException, DotDataException, DotSecurityException;
	/**
	 * Retrieves a paginated list of files the user can use 
	 * @param user
	 * @param includeArchived
	 * @param params
	 * @param hostId
	 * @param inode
	 * @param identifier
	 * @param parent
	 * @param offset
	 * @param limit
	 * @param orderBy
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	List<File> findFiles(User user, boolean includeArchived, Map<String,Object> params, String hostId, String inode, String identifier, String parent, int offset, int limit, String orderBy) throws DotSecurityException, DotDataException;

    /**
     * Copy a file into the given host
     *
     * @param file File to be copied
     * @param host Destination host
     * @return true if copy success, false otherwise
     */
    public File copyFile ( File file, Host host ) throws DotDataException, IOException;

    /**
     * Copy a file into the given directory
     *
     * @param file   File to be copied
     * @param parent Destination Folder
     * @return true if copy success, false otherwise
     */
    public File copyFile ( File file, Folder parent ) throws DotDataException, IOException;

    /**
     * gets the io.File handle for the file
     * @param file
     * @return
     * @throws IOException
     */
    public  java.io.File getAssetIOFile (File file) throws IOException ;
    

	
	public  boolean renameFile (File file, String newName) throws DotStateException, DotDataException, DotSecurityException;

    /**
     * Moves a file into the given directory
     *
     * @param file   File to be copied
     * @param parent Destination Folder
     * @return true if copy success, false otherwise
     */
    public Boolean moveFile(File file, Folder parent) throws DotStateException, DotDataException, DotSecurityException ;

    /**
     * Moves a file into the given host
     *
     * @param file File to be copied
     * @param host Destination host
     * @return true if copy success, false otherwise
     */
    public Boolean moveFile(File file, Host host) throws DotStateException, DotDataException, DotSecurityException ;

    void publishFile(File file) throws WebAssetException, DotSecurityException, DotDataException ;
    
    public File getFileByURI(String uri, Host host, boolean live) throws DotDataException, DotSecurityException ;


    public  File getFileByURI(String uri, String hostId, boolean live) throws DotDataException, DotSecurityException ;

    
}