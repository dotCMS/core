package com.dotmarketing.business;

import com.dotmarketing.exception.DotSecurityException;
import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.folders.model.Folder;

public interface IdentifierAPI {
    
    public static final String IDENT404 = "$$__404__CACHE_MISS__$$";

	/**
	 * Will look for all identifiers matting a URI pattern  
	 * @param uri Can contain a * at the beginning or end
	 * @param include Should find all that match pattern if true or all that do not match pattern if false
	 * @param assetType
	 * @param host
	 * @return
	 * @throws DotDataException
	 */
	public List<Identifier> findByURIPattern(String assetType,String uri, boolean include, Host host) throws DotDataException;

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
	 * Will take a String from an identifiers id and return its identifier from cache or db.
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
	 * @param host
	 * @return Identifier (note: it could return a not null identifier, however it does not means the identifier exists, must check UtiMethods.isSet(identifier.getId()) to make sure it is a valid Identifier)
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
	 * Will take a string and return its identifier from db or null if not found.
	 * This method WILL HIT the DB.
	 * @param id
	 * @return Identifier
	 * @throws DotDataException
	 * @throws DotStateException
	 */

	public Identifier loadFromDb(String id) throws DotDataException, DotStateException;

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
	public Identifier createNew(Versionable asset, Treeable parent, String existingId) throws DotDataException;

	/**
	 *
	 * @param webasset
	 * @param folder
	 * @throws DotDataException
	 */
	public void updateIdentifierURI(Versionable webasset, Folder folder) throws DotDataException;


	/**
	 * Finds identifiers with the specified parent path and host id
	 * 
	 * @param hostId
	 * @param parent_path
	 * @return
	 * @throws DotHibernateException
	 */
	public List<Identifier> findByParentPath(String hostId, String parent_path) throws DotDataException;

	/**
	 * This method hits the DB, table identifier to get the Type of the Asset.
	 *
	 * @param identifier that we want to find its type.
	 * @return Type of the Identifier that matches parameter. This method hist the DB.
	 * @throws DotDataException
	 */
	public String getAssetTypeFromDB(String identifier) throws DotDataException;

	/**
	 * Method will change user references of the given userId in Identifier
	 * with the replacement user Id
	 * @param userId User Identifier
	 * @param replacementUserId The user id of the replacement user
	 * @throws DotDataException There is a data inconsistency
	 * @throws DotStateException There is a data inconsistency
	 * @throws DotSecurityException
	 */
	public void updateUserReferences(final String userId, final String replacementUserId)throws DotDataException, DotSecurityException;

}
