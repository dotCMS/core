package com.dotmarketing.portlets.contentlet.business;

import com.dotcms.content.business.DotMappingException;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.Field;
import com.liferay.portal.model.User;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.search.SearchHits;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Provides utility methods to interact with {@link Contentlet} objects in
 * dotCMS. This class works closely with the Elastic index in order to minimize 
 * databse calls and maximize the use of the index to search for data.
 * 
 * @author root
 * @version 1.0
 * @since Mar 22, 2012
 *
 */
public abstract class ContentletFactory {

	/**
	 * Use to get all contentlets live/working contentlets
	 * @return
	 * @throws DotDataException
	 */
	protected abstract List<Contentlet> findAllCurrent() throws DotDataException;
	
	/**
	 * Use to get all contentlets live/working contentlets
	 * @param offset
	 * @param limit
	 * @return
	 * @throws DotDataException
	 */
	protected abstract List<Contentlet> findAllCurrent(int offset, int limit) throws DotDataException;
	
	/**
	 * This method gets a Contentlet object given the inode
	 * @param inode
	 * @return 
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	protected abstract Contentlet find(String inode) throws DotDataException, DotSecurityException;

    /**
     * Retrieves a contentlet from the database by its identifier and the working version.
     * It includes archive content if includeDeleted is true
     * @param identifier
     * @param includeDeleted
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    protected abstract Contentlet findContentletByIdentifierAnyLanguage(String identifier,
            boolean includeDeleted) throws DotDataException, DotSecurityException;

    /**
	 * Returns a live Contentlet Object for a given language
	 * @param languageId
	 * @param contentletId
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	protected abstract Contentlet findContentletForLanguage(long languageId, Identifier contentletId) throws DotDataException, DotSecurityException;

    /**
	 * Retrieves a contentlet from the database based on its identifier
	 * @param identifier 
	 * @param live Retrieves the live version if false retrieves the working version
	 * @return
	 * @throws DotDataException 
	 * @throws DotSecurityException 
	 */
	protected abstract Contentlet findContentletByIdentifier(String identifier, Boolean live, Long languageId) throws DotDataException, DotSecurityException;

	/**
	 * Retrieves a contentlet from the database based on its identifier and the working version
	 * @param identifier
	 * @return Contentlet
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	protected abstract Contentlet findContentletByIdentifierAnyLanguage(String identifier) throws DotDataException, DotSecurityException;
	
	/**
	 * Retrieves a contentlet from the database based on its identifier
	 * @param identifier 
	 * @param live Retrieves the live version if false retrieves the working version
	 * @return
	 * @throws DotDataException 
	 * @throws DotSecurityException 
	 */
	protected abstract List<Contentlet> findContentletsByIdentifier(String identifier, Boolean live, Long languageId) throws DotDataException, DotSecurityException;
	
	/**
	 * Gets a list of Contentlets from a passed in list of inodes.  
	 * @param inodes
	 * @return
	 * @throws DotSecurityException 
	 */
	protected abstract List<Contentlet> findContentlets(List<String> inodes) throws DotDataException, DotSecurityException;

	/**
	 * Returns all Contentlets for a specific structure using pagination
	 * @param structureInode
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	protected abstract List<Contentlet> findByStructure(String structureInode, int limit, int offset) throws DotDataException, DotSecurityException;

    /**
     * Returns all Contentlets for a specific structure (whose modDate is less than or equals to maxDate) using pagination
     * @param structureInode
     * @param maxDate
     * @param limit
     * @param offset
     * @return
     * @throws DotDataException
     * @throws DotStateException
     * @throws DotSecurityException
     */
    protected abstract List<Contentlet> findByStructure(String structureInode, Date maxDate,
            int limit, int offset) throws DotDataException, DotStateException, DotSecurityException;
	
	/**
	 * Saves a Contentlet
	 * @param contentlet
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	public abstract Contentlet save(Contentlet contentlet) throws DotDataException, DotSecurityException;

	/**
	 * Saves a Contentlet
	 * Takes a second param that states if the contentlet already exists, if set updates a specific version
	 * @param contentlet
	 * @param existingInode
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	protected abstract Contentlet save(Contentlet contentlet, String existingInode) throws DotDataException, DotSecurityException;
	
	/**
	 * The search here takes a lucene query and pulls Contentlets for you.  You can pass sortBy as null if you do not 
	 * have a field to sort by.  limit should be 0 if no limit and the offset should be -1 is you are not paginating.
	 * @param luceneQuery
	 * @param limit
	 * @param offset
	 * @param sortBy
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	protected abstract List<Contentlet> search(String luceneQuery, int limit, int offset, String sortBy) throws DotDataException, DotSecurityException;

	/**
	 * The search here takes a lucene query and pulls LuceneHits for you.  You can pass sortBy as null if you do not 
	 * have a field to sort by.  limit should be 0 if no limit and the offset should be -1 is you are not paginating.
	 * The returned list will be filtered with only the contentlets that the user can read(use).  you can of course also
	 * pass permissions to further limit in the lucene query itself
	 * @param luceneQuery
	 * @param limit
	 * @param offset
	 * @param sortBy
	 * @return
	 */
	protected abstract SearchHits indexSearch(String luceneQuery, int limit, int offset, String sortBy);
	
	/**
	 * Returns the contentlets on a given page.  You can pass -1 for languageId if you don't want to query to pull based
	 * on languages or 0 if you want to get the default language
	 * @param HTMLPageIdentifier
	 * @param containerIdentifier
	 * @param orderby
	 * @param working
	 * @param languageId
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	protected abstract List<Contentlet> findPageContentlets(String HTMLPageIdentifier,String containerIdentifier, String orderby, boolean working, long languageId)	throws  DotDataException, DotSecurityException;

	/**
	 * Retrieves all contentlets from the database based on its identifier (including multilingual versions)
	 * @param identifier 
	 * @param live Retrieves the live version if false retrieves the working version
	 * @return
	 * @throws DotDataException 
	 * @throws DotSecurityException 
	 */
	protected abstract List<Contentlet> getContentletsByIdentifier(String identifier, Boolean live) throws DotDataException, DotSecurityException;

	/**
	 * Gets a file with a specific relationship type to the passed in contentlet
	 * @param contentlet
	 * @param relationshipType
	 * @return
	 * @throws DotDataExceptionW
	 */
	protected abstract Identifier getRelatedIdentifier(Contentlet contentlet, String relationshipType) throws DotDataException;
	
	/**
	 * Gets all related links
	 * @param contentlet
	 * @return
	 * @throws DotDataException
	 */
	protected abstract List<Link> getRelatedLinks(Contentlet contentlet) throws DotDataException;

	/**
	 * deletes all passed in contentlets.  This is not an archive.  it is permanent 
	 * @param contentlets
	 * @throws DotDataException
	 */
	protected abstract void delete(List<Contentlet> contentlets)throws DotDataException;
	
	/**
	 * Deletes all the specified in contentlets. This method allows users to
	 * delete only a version of a contentlet, i.e., the Identifier remains.
	 * 
	 * @param contentlets
	 *            - The contentlet that will be deleted.
	 * @param deleteIdentifier
	 *            - If the contentlet identifier must be deleted, set to
	 *            {@code true}. Otherwise, set to {@code false}. -
	 * @return If the contentlet was successfully destroyed, returns
	 *         {@code true}. Otherwise, returns {@code false}.
	 * @throws DotDataException
	 *             An error occurred when deleting the information from the
	 *             database.
	 */
	protected abstract void delete(List<Contentlet> contentlets, boolean deleteIdentifier) throws DotDataException;

	/**
	 * Retrieves all contentlets from the database based on its identifier (including multilingual versions)
	 * @param identifier 
	 * @return
	 * @throws DotDataException 
	 * @throws DotSecurityException 
	 */
	protected abstract List<Contentlet> getContentletsByIdentifier(String identifier) throws DotDataException, DotSecurityException;

	/**
	 * Retrieves all versions for a contentlet identifier checked in by a real user meaning not the system user
	 * @param identifier
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	protected abstract List<Contentlet> findAllUserVersions(Identifier identifier) throws DotDataException, DotSecurityException;
	
	/**
	 * Retrieves all versions for a contentlet identifier
	 * @param identifier
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	protected abstract List<Contentlet> findAllVersions(Identifier identifier) throws DotDataException, DotSecurityException;

	/**
	 * Retrieves all versions for a contentlet identifier.
	 * @param identifier
	 * @param bringOldVersions Include old versions of contents, so it will return only live/working
	 * versions of contents, regardless of their languages
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	protected abstract List<Contentlet> findAllVersions(Identifier identifier, boolean bringOldVersions) throws DotDataException, DotSecurityException;

	/**
	 * Converts a "fat" (legacy) contentlet into a new contentlet.
	 * @param Fat contentlet to be converted.
	 * @return A "light" contentlet.
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	public abstract Contentlet convertFatContentletToContentlet (com.dotmarketing.portlets.contentlet.business.Contentlet fatty) throws DotDataException, DotSecurityException;
	
	/**
	 * Converts a "light" contentlet into a "fat" (legacy) contentlet.
	 * @param A "light" contentlet to be converted.
	 * @return Fat contentlet.
	 * @throws DotDataException
	 */
	public abstract com.dotmarketing.portlets.contentlet.business.Contentlet convertContentletToFatContentlet (Contentlet cont, com.dotmarketing.portlets.contentlet.business.Contentlet fatty) throws DotDataException;

	/**
	 * 
	 * @param structureInode
	 * @param field
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 * @throws DotStateException 
	 */
	protected abstract void cleanField(String structureInode, Field field) throws DotDataException, DotStateException, DotSecurityException;
	
	/**
	 * Set the system host in the identifier of all the contents of the specified structure. 
	 * 
	 * @param structureInode
	 * @throws DotDataException
	 * @throws DotMappingException 
	 * @throws DotSecurityException 
	 * @throws DotStateException 
	 */ 
	protected abstract void cleanIdentifierHostField(String structureInode) throws DotDataException, DotMappingException, DotStateException, DotSecurityException;
	
	/**
	 * 
	 * @param deleteFrom
	 * @return
	 * @throws DotDataException
	 */
	protected abstract int deleteOldContent(Date deleteFrom) throws DotDataException;
	
	/**
	 * 
	 * @param structureInode
	 * @param field
	 * @return
	 * @throws DotDataException
	 */
	protected abstract List<Contentlet> findContentletsWithFieldValue(String structureInode, Field field) throws DotDataException;
	
	/**
	 * gets the number of contentlets in the system. This number includes all versions not distinct identifiers
	 * @return
	 * @throws DotDataException
	 */
	protected abstract long contentletCount() throws DotDataException;
	
	/**
	 * gets the number of contentlet identifiers in the system. This number includes all versions not distinct identifiers
	 * @return
	 * @throws DotDataException
	 */
	protected abstract long contentletIdentifierCount() throws DotDataException;

	/**
	 * 
	 * @param query
	 * @param fields
	 * @param structureInode
	 * @return
	 * @throws ValidationException
	 * @throws DotDataException
	 */
	protected abstract List<Map<String, Serializable>> DBSearch(Query query, List<Field> fields, String structureInode) throws ValidationException,DotDataException;
	
	protected abstract void UpdateContentWithSystemHost(String hostIdentifier) throws DotDataException, DotSecurityException;
	/**
	 * Method will remove User References of the given userId in Contentlet 
	 * with the system user id
	 * @param userId User Id to change
	 * @throws DotSecurityException 
	 */	
	protected abstract void removeUserReferences(String userId)throws DotDataException, DotSecurityException;
	
	/**
	 * Method will replace user references of the given userId in Contentlets
	 * with the replacement user id  
	 * @param userToReplace the user to replace
	 * @param replacementUserId Replacement User Id
	 * @param user the user requesting the operation
	 * @exception DotDataException There is a data inconsistency
	 * @throws DotSecurityException 
	 */	
	protected abstract void updateUserReferences(User userToReplace, String replacementUserId, User user) throws DotDataException, DotStateException, ElasticsearchException, DotSecurityException;

	protected abstract void deleteVersion(Contentlet contentlet)throws DotDataException;
	
	/**
	 * Will update contents that reference the given folder to point to it's parent folder, if it's a top folder it will set folder to be SYSTEM_FOLDER
	 * @param folder
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	protected abstract void removeFolderReferences(Folder folder) throws DotDataException, DotSecurityException;

    protected abstract Object loadField(String inode, String fieldContentlet) throws DotDataException;
    
    protected abstract long indexCount(String query);

	/**
	 * This indexCount will use the thirdparty mechanism to async known when the query is returning something.
	 * @param query          {@link String} query to test if get results
	 * @param timeoutMillis  {@link Long}   time in millis to timeout
	 */
	protected abstract long indexCount(final String query,
							  final long timeoutMillis);

	/**
	 * This indexCount will use the thirdparty mechanism to async known when the query is returning something.
	 * this one use an async response, the indexCountSuccess will be called if the count is success, otherwise if the indexCountFailure is not null will be invoked.
	 * @param query
	 * @param timeoutMillis
	 * @param indexCountSuccess
	 * @param indexCountFailure
	 */
	protected abstract void indexCount(final String query,
					final long timeoutMillis,
					final Consumer<Long> indexCountSuccess,
					final Consumer<Exception> indexCountFailure);
    
    /**
     * Gets the top viewed contents identifier and numberOfViews for a particular structure for a specified date interval
     * 
     * @param structureName
     * @param startDate
     * @param endDate
     * @param user
     * @return
     * @throws DotDataException 
     */
	public abstract List<Map<String, String>> getMostViewedContent(String structureInode,Date startDate, Date endDate, User user) throws DotDataException;

    protected List<Contentlet> findPageContentlets(String HTMLPageIdentifier, String containerId, String uniqueId, String orderby,
            boolean working, long languageId) throws DotDataException, DotStateException, DotSecurityException {
        // TODO Auto-generated method stub
        return null;
    }

	/**
	 * Updates all the content associated with the specified inodes
	 * @param inodes
	 * @param user
	 * @return number of rows affected
	 * @throws DotDataException
	 */
	public abstract int updateModDate(final Set<String> inodes, User user) throws DotDataException;

    public abstract Optional<Contentlet> findInDb(String inode) ;

}