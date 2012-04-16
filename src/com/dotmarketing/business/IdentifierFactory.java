package com.dotmarketing.business;

import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.folders.model.Folder;

/**
 * 
 * @author will
 * 
 */
public abstract class IdentifierFactory {

	abstract protected void updateIdentifierURI(Versionable webasset, Folder folder) throws DotDataException;

	
	
	/**
	 * looks in cache first, then in db.  It will load the cache for future use
	 * @param host
	 * @param uri
	 */
	abstract protected Identifier findByURI(Host host, String uri) throws DotHibernateException;

	
	/**
	 * looks in cache first, then in db.  It will load the cache for future use
	 * @param host
	 * @param uri
	 */
	abstract protected Identifier findByURI(String hostId, String uri) throws DotHibernateException;

	
	/**
	 * looks in cache only, returns null if not found
	 * @param host
	 * @param uri
	 */
	abstract protected Identifier loadByURIFromCache(Host host, String uri);
	/**
	 * looks in db only, returns null if not found
	 * @param identifier
	 */
	abstract protected Identifier loadFromDb(String identifier) throws DotDataException, DotStateException;
	/**
	 * looks in db only, returns null if not found
	 * @param versionable
	 */
	abstract protected Identifier loadFromDb(Versionable versionable) throws DotDataException;
	/**
	 * looks in cache only, returns null if not found
	 * @param identifier
	 */
	abstract protected Identifier loadFromCache(String identifier);
	/**
	 * loads from cache only, returns null if not found
	 * @param versionable
	 */
	abstract protected Identifier loadFromCache(Versionable versionable);

	
	/**
	 * looks in cache only, returns null if not found
	 * @param host
	 * @param uri
	 */
	abstract protected Identifier loadFromCache(Host host, String uri);

	
	/**
	 * looks in cache first, then in db.  It will load the cache for future use
	 * @param versionable
	 */
	abstract protected Identifier find(Versionable versionable) throws DotDataException;
	/**
	 * looks in cache first, then in db.  It will load the cache for future use
	 * @param versionable
	 */
	abstract protected Identifier find(String x) throws DotStateException, DotDataException;

	abstract protected Identifier createNewIdentifier(Versionable webasset, Folder folder) throws DotDataException ;

	abstract protected Identifier createNewIdentifier(Versionable versionable, Host host) throws DotDataException;

	abstract protected List<Identifier> loadAllIdentifiers() throws DotHibernateException;

	abstract protected boolean isIdentifier(String identifierInode);

	// http://jira.dotmarketing.net/browse/DOTCMS-4970

	abstract protected Identifier saveIdentifier(Identifier identifier)throws DotDataException;
	
	abstract protected void deleteIdentifier(Identifier ident) throws DotDataException;
	
	abstract protected List<Identifier> findByParentPath(String hostId, String parent_path) throws DotHibernateException;

}
