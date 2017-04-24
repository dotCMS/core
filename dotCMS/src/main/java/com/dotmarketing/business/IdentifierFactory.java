package com.dotmarketing.business;

import java.util.Date;
import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.folders.model.Folder;

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

	/**
	 * Retrieves all identifiers matching a URI pattern.
	 * 
	 * @param uri
	 *            - Can contain a * at the beginning or end.
	 * @param include
	 *            - Should find all that match pattern if true or all that do
	 *            not match pattern if false.
	 * @param assetType
	 * @param site
	 * @param hasLive
	 *            - Pull only if the identifier has a published version.
	 * @param pullDeleted
	 * @param startDate
	 *            - Used to search between dates.
	 * @param endDate
	 * @return
	 * @throws DotDataException
	 */
	abstract protected List<Identifier> findByURIPattern(String assetType, String uri, boolean hasLive,
			boolean pullDeleted, boolean include, Host site, Date startDate, Date endDate) throws DotDataException;

	/**
	 * 
	 * @param webasset
	 * @param folder
	 * @throws DotDataException
	 */
	abstract protected void updateIdentifierURI(Versionable webasset, Folder folder) throws DotDataException;

	/**
	 * Retrieves all identifiers matching a URI pattern.
	 * 
	 * @param uri
	 *            - Can contain a * at the beginning or end.
	 * @param include
	 *            - Should find all that match pattern if true or all that do
	 *            not match pattern if false.
	 * @param assetType
	 * @param hasLive
	 *            - Pull only if the identifier has a published version.
	 * @param pullDeleted
	 * @param site
	 * @return
	 * @throws DotDataException
	 */
	abstract protected List<Identifier> findByURIPattern(String assetType, String uri, boolean hasLive,
			boolean pullDeleted, boolean include, Host site) throws DotDataException;

	/**
	 * Retrieves the identifier matching the URI by looking in cache first, then
	 * in database. It will load the cache for future use.
	 * 
	 * @param site
	 * @param uri
	 */
	abstract protected Identifier findByURI(Host site, String uri) throws DotHibernateException;

	/**
	 * Retrieves the identifier matching the URI by looking in cache first, then
	 * in database. It will load the cache for future use.
	 * 
	 * @param site
	 * @param uri
	 */
	abstract protected Identifier findByURI(String siteId, String uri) throws DotHibernateException;

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
	 * @param versionable
	 */
	abstract protected Identifier find(String x) throws DotStateException, DotDataException;

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
	 * @throws DotHibernateException
	 */
	abstract protected List<Identifier> loadAllIdentifiers() throws DotHibernateException;

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
	abstract protected Identifier saveIdentifier(Identifier identifier) throws DotDataException;

	/**
	 * 
	 * @param ident
	 * @throws DotDataException
	 */
	abstract protected void deleteIdentifier(Identifier ident) throws DotDataException;

	/**
	 * 
	 * @param siteId
	 * @param parent_path
	 * @return
	 * @throws DotHibernateException
	 */
	abstract protected List<Identifier> findByParentPath(String siteId, String parent_path)
			throws DotHibernateException;

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

}
