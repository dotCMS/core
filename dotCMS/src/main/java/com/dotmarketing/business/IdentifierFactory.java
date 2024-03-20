package com.dotmarketing.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;

import java.util.List;

/**
 * Provides data source level access to information related to Identifiers in
 * dotCMS. Every piece of content you create in the application will be
 * associated to a unique ID that never changes.
 * 
 * @author root
 * @version 1.x
 * @since Mar 22, 2012
 * 
 */
public abstract class IdentifierFactory {

	public static final String ID = "id";
	public static final String PARENT_PATH = "parent_path";
	public static final String ASSET_NAME = "asset_name";
	public static final String HOST_INODE = "host_inode";
	public static final String ASSET_TYPE = "asset_type";
	public static final String SYS_PUBLISH_DATE = "syspublish_date";
	public static final String SYS_EXPIRE_DATE = "sysexpire_date";
	public static final String OWNER = "owner";
	public static final String CREATE_DATE = "create_date";
	public static final String ASSET_SUBTYPE = "asset_subtype";

	/**
	 * Retrieves all identifiers matching a URI pattern.
	 *
	 * @param assetType
	 * @param uri
	 *            - Can contain a * at the beginning or end.
	 * @param include
	 *            - Should find all that match pattern if true or all that do
	 *            not match pattern if false.
	 * @param site
	 * @return
	 * @throws DotDataException
	 */
	abstract protected List<Identifier> findByURIPattern(final String assetType, String uri,
			boolean include, Host site) throws DotDataException;

	/**
	 * 
	 * @param webasset
	 * @param folder
	 * @throws DotDataException
	 */
	abstract protected void updateIdentifierURI(Versionable webasset, Folder folder) throws DotDataException;

	/**
	 * Retrieves the identifier matching the URI by looking in cache first, then
	 * in database. It will load the cache for future use.
	 * 
	 * @param site
	 * @param uri
	 */
	abstract protected Identifier findByURI(final Host site, String uri) throws DotDataException;

	/**
	 * Retrieves the identifier matching the URI by looking in cache first, then
	 * in database. It will load the cache for future use.
	 * 
	 * @param siteId
	 * @param uri
	 */
	abstract protected Identifier findByURI(final String siteId, String uri) throws DotDataException;

	/**
	 * Retrieves the identifier matching the URI by looking in cache. Returns
	 * null if not found.
	 * 
	 * @param site
	 * @param uri
	 */
	abstract protected Identifier loadByURIFromCache(Host site, String uri);

	/**
	 * Retrieves the identifier matching the URI by looking in the database.
	 * Returns null if not found.
	 * 
	 * @param identifier
	 */
	abstract protected Identifier loadFromDb(String identifier) throws DotDataException, DotStateException;

	/**
	 * Retrieves the identifier matching the URI by looking in the database.
	 * Returns null if not found.
	 * 
	 * @param versionable
	 */
	abstract protected Identifier loadFromDb(Versionable versionable) throws DotDataException;

	/**
	 * Retrieves the identifier matching the URI by looking in the cache.
	 * Returns null if not found.
	 * 
	 * @param identifier
	 */
	abstract protected Identifier loadFromCache(String identifier);

	/**
	 * Retrieves the identifier matching the URI by looking in the cache.
	 * Returns null if not found.
	 * 
	 * @param versionable
	 */
	abstract protected Identifier loadFromCache(Versionable versionable);

	/**
	 * Retrieves the identifier matching the Inode by looking in the cache.
	 * Returns null if not found.
	 * 
	 * @param inode
	 */
	abstract protected Identifier loadFromCacheFromInode(String inode);

	/**
	 * Retrieves the identifier matching the URI by looking in the cache.
	 * Returns null if not found.
	 * 
	 * @param site
	 * @param uri
	 */
	abstract protected Identifier loadFromCache(Host site, String uri);

	/**
	 * Retrieves the identifier matching the URI by looking in cache first, then
	 * in database. It will load the cache for future use.
	 * 
	 * @param versionable
	 */
	abstract protected Identifier find(Versionable versionable) throws DotDataException;

	/**
	 * Retrieves the identifier matching the URI by looking in cache first, then
	 * in database. It will load the cache for future use.
	 * 
	 * @param identifier
	 */
	abstract protected Identifier find(final String identifier) throws DotStateException, DotDataException;

	/**
	 * Creates a new Identifier for a given versionable asset under a given
	 * folder. The ID value will be randomly generated.
	 *
	 * @param webasset
	 *            - The asset that will be created.
	 * @param folder
	 *            - The folder that the asset will be created in.
	 * @return The {@link Identifier} of the new asset.
	 * @throws DotDataException
	 *             An error occurred when interacting with the data source.
	 */
	abstract protected Identifier createNewIdentifier(Versionable webasset, Folder folder) throws DotDataException;

    protected abstract Identifier createNewIdentifier(Folder folder, Folder parent,
            String existingId) throws DotDataException;

    /**
	 * Creates a new Identifier for a given versionable asset under a given
	 * folder. In this method, the ID value will <b>NOT</b> be randomly
	 * generated as it is specified as a parameter.
	 * 
	 * @param webasset
	 *            - The asset that will be created.
	 * @param folder
	 *            - The folder that the asset will be created in.
	 * @param existingId
	 *            - The ID of the new Identifier.
	 * @return The {@link Identifier} of the new asset.
	 * @throws DotDataException
	 *             An error occurred when interacting with the data source.
	 */
	abstract protected Identifier createNewIdentifier(Versionable webasset, Folder folder, String existingId)
			throws DotDataException;

	/**
	 * Creates a new Identifier for a given versionable asset under a given
	 * site. The ID value will be randomly generated.
	 *
	 * @param versionable
	 *            - The asset that will be created.
	 * @param site
	 *            - The site that the asset will be created in.
	 * @return The {@link Identifier} of the new asset.
	 * @throws DotDataException
	 *             An error occurred when interacting with the data source.
	 */
	abstract protected Identifier createNewIdentifier(Versionable versionable, Host site) throws DotDataException;

	protected abstract Identifier createNewIdentifier (Folder folder, Host site, String existingId) throws DotDataException;

	/**
	 * Creates a new Identifier for a given versionable asset under a given
	 * site. In this method, the ID value will <b>NOT</b> be randomly generated
	 * as it is specified as a parameter.
	 * 
	 * @param versionable
	 *            - The asset that will be created.
	 * @param site
	 *            - The site that the asset will be created in.
	 * @param existingId
	 *            - The ID of the new Identifier.
	 * @return The {@link Identifier} of the new asset.
	 * @throws DotDataException
	 *             An error occurred when interacting with the data source.
	 */
	abstract protected Identifier createNewIdentifier(Versionable versionable, Host site, String existingId)
			throws DotDataException;

	/**
	 * 
	 * @return
	 * @throws DotDataException
	 */
	abstract protected List<Identifier> loadAllIdentifiers() throws DotDataException;

	/**
	 * 
	 * @param identifierInode
	 * @return
	 */
	abstract protected boolean isIdentifier(String identifierInode);

	/**
	 * 
	 * @param identifier
	 * @return
	 * @throws DotDataException
	 */
	abstract protected Identifier saveIdentifier(final Identifier identifier) throws DotDataException;

	/**
	 * Deletes all relationships with this identifier. Accordingly, the object will be removed from
	 * the identifier table through a DB trigger
	 *
	 * @param ident
	 */
	abstract protected void deleteIdentifier(Identifier ident) throws DotDataException;

	/**
	 * 
	 * @param siteId
	 * @param parent_path
	 * @return
	 * @throws DotDataException
	 */
	abstract protected List<Identifier> findByParentPath(final String siteId, String parent_path)
			throws DotDataException;

	/**
	 * This method hits the DB, table identifier to get the Asset Type.
	 *
	 * @param identifier
	 *            - The type of Identifier.
	 * @return Type of the Identifier that matches parameter. This method hits
	 *         the DB.
	 * @throws DotDataException
	 */
	abstract protected String getAssetTypeFromDB(String identifier) throws DotDataException;

	/**
	 * Method will change user references of the given userId in Identifier
	 * with the replacement user Id
	 * @param userId User Identifier
	 * @param replacementUserId The user id of the replacement user
	 * @throws DotDataException There is a data inconsistency
	 * @throws DotStateException There is a data inconsistency
	 * @throws DotSecurityException
	 */
	abstract protected void updateUserReferences(final String userId, final String replacementUserId)throws DotDataException, DotSecurityException;

}
