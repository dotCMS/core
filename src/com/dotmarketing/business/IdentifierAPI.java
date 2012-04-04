package com.dotmarketing.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.folders.model.Folder;

public interface IdentifierAPI {

	/**
	 * Will take a String from an inode id and return its identifier from cache or db. If cache miss
	 * this will always hit the db
	 * @param inode
	 * @return
	 * @throws DotDataException
	 * @throws DotStateException if no identifier can be found from passed in inode
	 */
	public Identifier findFromInode(String inode) throws DotDataException;
	
	
	/**
	 * Will take a String from an identifiers id and return its identifier from cache or db. If cache miss
	 * this will always hit the db
	 * @param id
	 * @return Identifier
	 * @throws DotDataException
	 * @throws DotStateException if no identifier can be found from passed in id
	 */
	public Identifier find(String id) throws DotDataException;
	
	/**
	 * Will take a Versionable and return its identifier from cache or db. If cache miss
	 * this will always hit the db
	 * @param versionable
	 * @return Identifier
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	public Identifier find(Versionable versionable) throws DotDataException, DotStateException;
	
	/**
	 * Will take a host and uri and return its identifier from cache or db. If cache miss
	 * this will always hit the db
	 * @param versionable
	 * @return Identifier
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	public Identifier find(Host host, String uri) throws DotDataException, DotStateException;
	
	
	/**
	 * Will take a host and uri and return its identifier from cache or null if not found
	 * This will never hit the db
	 * @param host
	 * @param uri
	 * @return Identifier
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	public Identifier loadFromCache(Host host, String uri) throws DotDataException, DotStateException;
	
	/**
	 * Will take a versionable and return its identifier from cache or null if not found
	 * This will never hit the db
	 * @param versionable
	 * @return Identifier
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	public Identifier loadFromCache(Versionable asset) throws DotDataException, DotStateException;
	
	/**
	 * Will take a string and return its identifier from cache or null if not found.
	 * This will never hit the db
	 * @param versionable
	 * @return Identifier
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	public Identifier loadFromCache(String id) throws DotDataException;
	
	/**
	 * This method WILL HIT the DB. 
	 * @return boolean
	 * @throws DotDataException
	 */
	public boolean isIdentifier(String identifierInode) throws DotDataException;
	
	/**
	 * This method WILL Save to the DB. 
	 * @return boolean
	 * @throws DotDataException
	 */
	public Identifier save(Identifier identifier) throws DotDataException;
	
	/**
	 * This method WILL delete the identifier and all related versonable assets from the DB. 
	 * @return boolean
	 * @throws DotDataException
	 */
	public void delete(Identifier identifier) throws DotDataException;
	
	/**
	 * @param asset
	 * @param parent
	 * @return
	 * @throws DotDataException
	 */
	public Identifier createNew(Versionable asset, Treeable parent) throws DotDataException;
	
	/**
	 * 
	 * @param webasset
	 * @param folder
	 * @throws DotDataException
	 */
	public void updateIdentifierURI(Versionable webasset, Folder folder) throws DotDataException;
	
	

	
	
}
