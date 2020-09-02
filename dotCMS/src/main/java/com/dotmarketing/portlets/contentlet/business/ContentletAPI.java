package com.dotmarketing.portlets.contentlet.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.content.business.DotMappingException;
import com.dotcms.content.elasticsearch.business.ESSearchResults;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Provides access to a wide range of routines aimed to interact with
 * information related to contents ({@link Contentlet} objects) in dotCMS. You
 * can perform CRUD operations, re-index operations, locking and unlocking,
 * among others.
 *
 * @author Jason Tesser
 * @version 1.0
 * @since Mar 22, 2012
 *
 */
public interface ContentletAPI {

	/**
	 * Use to retrieve all version of all content in the database.  This is not a common method to use. 
	 * Only use if you need to do maintenance tasks like search and replace something in every piece 
	 * of content.  Doesn't respect permissions.
	 * @param offset can be 0 if no offset
	 * @param limit can be 0 of no limit
	 * @return List<Contentlet> list of contentlets
	 * @throws DotDataException
	 */
	public List<Contentlet> findAllContent(int offset, int limit) throws DotDataException;
	
	/**
	 * Finds a Contentlet Object given the inode
	 * @param inode
	 * @return Contentlet object found on ES Index
	 * @throws DotDataException
	 */
	public Contentlet find(String inode, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Returns a working Contentlet Object for a given language
	 * @param languageId Language Id for a version-specific content.
	 * @param contentletId Identifier object which belongs to an existing contentlet
	 * @return Working version of contentlet is found, given id and language
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	public Contentlet findContentletForLanguage(long languageId, Identifier contentletId) throws DotDataException, DotSecurityException;


	/**
	 * Returns all Contentlets for a specific structure
	 * @param structure
	 * @param user
	 * @param respectFrontendRoles
	 * @param limit
	 * @param offset
	 * @return List<Contentlet> list of contentlets
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<Contentlet> findByStructure(Structure structure, User user, boolean respectFrontendRoles, int limit, int offset) throws DotDataException, DotSecurityException;
	
	/**
	 * Returns all Contentlets for a specific structure
	 * @param structureInode
	 * @param user
	 * @param respectFrontendRoles
	 * @param limit
	 * @param offset
	 * @return List<Contentlet> list of contentlets
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<Contentlet> findByStructure(String structureInode, User user, boolean respectFrontendRoles, int limit, int offset) throws DotDataException, DotSecurityException;
	
	/**
	 * Retrieves a contentlet from the Lucene index + cache first, then falls back
	 * to the database if not found based on its identifier
	 * @param identifier 
	 * @param live Retrieves the live version. If false retrieves the working version
	 * @param languageId languageId The LanguageId of the content version we'd like to retrieve
	 * @param user
	 * @param respectFrontendRoles
	 * @return Contentlet Object
	 * @throws DotSecurityException
	 * @throws DotContentletStateException
	 * @throws DotDataException 
	 */
	public Contentlet findContentletByIdentifier(String identifier, boolean live, long languageId, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException;

    /**
     * Retrieves a contentlet from the database by its identifier and the working version.
     * It includes archive content if includeDeleted is true
     * @param identifier
     * @param includeDeleted
     * @return Contentlet object
     * @throws DotSecurityException
     * @throws DotDataException
     */
    Contentlet findContentletByIdentifierAnyLanguage(String identifier, boolean includeDeleted) throws DotDataException;

    /**
	 * Retrieves a contentlet from the database by its identifier and the working version. Ignores archived content
	 * @param identifier
	 * @return Contentlet object
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public Contentlet findContentletByIdentifierAnyLanguage(String identifier) throws DotDataException;

	/**
	 * Retrieves a contentlet list from the database based on a identifiers array
	 * @param identifiers	Array of identifiers
	 * @param live	Retrieves the live version if false retrieves the working version
	 * @param languageId
	 * @param user	
	 * @param respectFrontendRoles	
	 * @return List<Contentlet> list of contentlets
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException
	 */
	public List<Contentlet> findContentletsByIdentifiers(String[] identifiers, boolean live, long languageId, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException;
		
	/**
	 * Gets a list of Contentlets from a passed in list of inodes.  
	 * @param inodes
	 * @return List<Contentlet>
	 * @throws DotSecurityException 
	 */
	public List<Contentlet> findContentlets(List<String> inodes) throws DotDataException, DotSecurityException;
	
	/**
	 * Gets a list of Contentlets from a given parent folder  
	 * @param parentFolder
	 * @return List<Contentlet>
	 * @throws DotSecurityException 
	 */
	public List<Contentlet> findContentletsByFolder(Folder parentFolder, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Gets a list of Contentlets from a given parent host, retrieves the working version of content
	 * @param parentHost
	 * @return List<Contentlet>
	 * @throws DotSecurityException 
	 */
	public List<Contentlet> findContentletsByHost(Host parentHost, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Gets a list of Contentlets from a given parent host, retrieves the working version of content. The difference between this method and the other one
	 * is that the user can specify which content type want to include and exclude.
	 * NOTE: If the parameters includingContentTypes and excludingContentTypes are empty if will return all the contentlets.
	 * @param parentHost
	 * @param includingContentTypes this is a list of content types that you would like to include in the results
	 * @param excludingContentTypes this is a list of content types that you would like to exclude in the results
	 * @param user
	 * @param respectFrontendRoles
	 * @return List<Contentlet> list of contentlets
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<Contentlet> findContentletsByHost(Host parentHost, List<Integer> includingContentTypes, List<Integer> excludingContentTypes, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Returns a list of {@link Contentlet} whose parent host matches the given host and whose base-type
	 * (See {@link Structure.Type}) matches any of the given base types. .
	 *
	 * @param parentHost the host to match content's parent host against
	 * @param includingBaseTypes if not null or empty, content is filtered by these baseTypes
	 * @param user the user requesting the operation
	 * @param respectFrontendRoles a flag to indicate whether or not front-end roles are respected
	 * @return a list of content whose parent matches the given host and whose base-type matches any of the given base
	 * types
	 * @throws DotDataException
     * @throws DotSecurityException
     */
	List<Contentlet> findContentletsByHostBaseType(Host parentHost, List<Integer> includingBaseTypes, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;


	/**
	 * Copies a contentlet, including all its fields including binary files, image and file fields are pointers and the are preserved as the are
	 * so if source contentlet points to image A and resulting new contentlet will point to same image A as well, also copies source permissions.
	 *
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @return Contentlet
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException
	 *             throws this exception if the new contentlet requires a
	 *             destination host or folder mandated by its structure
	 */
	public Contentlet copyContentlet(Contentlet contentlet, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException, DotContentletStateException;
	
	/**
	 * Copies a contentlet, including all its fields including binary files, image and file fields are pointers and the are preserved as the are
	 * so if source contentlet points to image A and resulting new contentlet will point to same image A as well, also copies source permissions.
	 * And moves the the new piece of content to the given host
	 * 
	 * @param contentlet
	 * @param host
	 * @param user
	 * @param respectFrontendRoles
	 * @return Contentlet
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException
	 */
	public Contentlet copyContentlet(Contentlet contentlet, Host host, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException, DotContentletStateException;
	
	/**
	 * Copies a contentlet, including all its fields including binary files, image and file fields are pointers and the are preserved as the are
	 * so if source contentlet points to image A and resulting new contentlet will point to same image A as well, also copies source permissions.
	 * And moves the the new piece of content to the given folder
	 * 
	 * @param contentlet
	 * @param folder
	 * @param user
	 * @param respectFrontendRoles
	 * @return Contentlet
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException
	 */
	public Contentlet copyContentlet(Contentlet contentlet, Folder folder, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException;

	/**
	 * Copies a contentlet, including all its fields including binary files, image and file fields are pointers and the are preserved as the are
	 * so if source contentlet points to image A and resulting new contentlet will point to same image A as well, also copies source permissions.
	 * And moves the the new piece of content to the given folder. appendCopyToFileName will allow to chose to append "COPY" to the file name or not.
	 * 
	 * @param contentlet
	 * @param folder
	 * @param user
	 * @param appendCopyToFileName
	 * @param respectFrontendRoles
	 * @return Contentlet
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException
	 */
	public Contentlet copyContentlet(Contentlet contentlet, Folder folder, User user, boolean appendCopyToFileName, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException;

	/**
	 * Copies a contentlet, including all its fields including binary files, image and file fields are pointers and the are preserved as the are
	 * so if source contentlet points to image A and resulting new contentlet will point to same image A as well, also copies source permissions.
	 * And moves the the new piece of content to the given folder. CopySuffix will be to append suffix to the file name.
	 *
	 * @param contentletToCopy
	 * @param host
	 * @param folder
	 * @param user
	 * @param copySuffix
	 * @param respectFrontendRoles
	 * @return Contentlet
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException
	 */
	Contentlet copyContentlet(Contentlet contentletToCopy, Host host, Folder folder, User user, final String copySuffix, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException;

	/**
	 * The search here takes a lucene query and pulls Contentlets for you.  You can pass sortBy as null if you do not 
	 * have a field to sort by.  limit should be 0 if no limit and the offset should be -1 is you are not paginating.
	 * The returned list will be filtered with only the contentlets that the user can read(use).  you can of course also
	 * pass permissions to further limit in the lucene query itself
	 * @param luceneQuery
	 * @param limit
	 * @param offset
	 * @param sortBy
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	public List<Contentlet> search(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * The search here takes a lucene query and pulls Contentlets for you.  You can pass sortBy as null if you do not 
	 * have a field to sort by.  limit should be 0 if no limit and the offset should be -1 is you are not paginating.
	 * The returned list will be filtered with only the contentlets that match the required permission.  You can of course also
	 * pass permissions to further limit in the lucene query itself
	 * @param luceneQuery
	 * @param limit
	 * @param offset
	 * @param sortBy indexName(previously known as dbColumnName) to order by. Can be null or empty string
	 * @param user
	 * @param respectFrontendRoles
	 * @param requiredPermission
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<Contentlet> search(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission) throws DotDataException, DotSecurityException;

	/**
	 * Adds the permissions query fragment to the given query based on the given user and roles
	 *
	 * @param buffy
	 * @param user
	 * @param roles
	 * @param respectFrontendRoles
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public void addPermissionsToQuery ( StringBuffer buffy, User user, List<Role> roles, boolean respectFrontendRoles ) throws DotSecurityException, DotDataException;

	/**
	 * The search here takes a lucene query and pulls LuceneHits for you.  You can pass sortBy as null if you do not 
	 * have a field to sort by.  limit should be 0 if no limit and the offset should be -1 is you are not paginating.
	 * The returned list will be filtered with only the contentlets that the user can read(use).  you can of course also
	 * pass permissions to further limit in the lucene query itself
	 * @param luceneQuery
	 * @param limit
	 * @param offset
	 * @param sortBy indexName(previously known as dbColumnName) to order by. Can be null or empty string
	 * @param user
	 * @param respectFrontendRoles
	 * @return List<ContentletSearch> list of objects with each content's identifier, inode and score in ES Index
	 * @throws DotSecurityException if user is null and respectFrontendRoles is false
	 * @throws DotDataException 
	 */
	public List<ContentletSearch> searchIndex(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException;
	
	/**
	 * Publishes all related HTMLPage
	 * @param contentlet
	 * @throws DotDataException 
	 * @throws DotStateException 
	 */
	public void publishRelatedHtmlPages(Contentlet contentlet) throws DotStateException, DotDataException;

    /**
	 * Will get all the contentlets for a structure and set the default values for a field on the contentlet.  
	 * Will check Write/Edit permissions on the Contentlet. So to guarantee all Contentlets will be cleaned make
	 * sure to pass in an Admin User.  If a user doesn't have permissions to clean all the contentlets it will clean
	 * as many as it can and throw the DotSecurityException  
	 * @param structure
	 * @param field
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public void cleanField(Structure structure, Field field, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException;

    /**
     * Will get all the contentlets for a structure (whose modDate is lower than or equals to the deletion date)
     * and set the default values for a field on the contentlet.
     * Will check Write/Edit permissions on the Contentlet. So to guarantee all Contentlets will be cleaned make
     * sure to pass in an Admin User.  If a user doesn't have permissions to clean all teh contentlets it will clean
     * as many as it can and throw the DotSecurityException
     * @param structure
     * @param deletionDate
     * @param field
     * @param user
     * @param respectFrontendRoles
     * @throws DotSecurityException
     * @throws DotDataException
     */
    void cleanField(final Structure structure, final Date deletionDate, final Field field, final User user,
            final boolean respectFrontendRoles)
            throws DotSecurityException, DotDataException;

	/**
	 * Will get all the contentlets for a structure and set the system host and system folder for the host values
	 * Will check Write/Edit permissions on the Contentlet. So to guarantee all Contentlets will be cleaned make
	 * sure to pass in an Admin User.  If a user doesn't have permissions to clean all teh contentlets it will clean 
	 * as many as it can and throw the DotSecurityException  
	 * @param structure
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotMappingException 
	 */
	public void cleanHostField(Structure structure, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException, DotMappingException;

	/**
	 * Finds the next date that a contentlet must be reviewed
	 * @param content 
	 * @param user
	 * @param respectFrontendRoles
	 * @return Date
	 * @throws DotSecurityException
	 */
	public Date getNextReview(Contentlet content, User user, boolean respectFrontendRoles) throws DotSecurityException;

	/**
	 * Retrieves all references for a Contentlet. The result is an ArrayList of type Map whose key will 
	 * be page or container with the respective object as the value.  
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException 
	 * @throws DotSecurityException
	 * @throws DotContentletStateException - if the contentlet is null or has an invalid inode
	 */
	public List<Map<String, Object>> getContentletReferences(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException, DotContentletStateException;

    /**
     * Gets the value of a field with a given contentlet
     * @param contentlet
     * @param theField
     * @param user
     * @return Object from DB with field's value
     */
    Object getFieldValue(Contentlet contentlet, com.dotcms.contenttype.model.field.Field theField, User user, final boolean respectFrontEndRoles);

    /**
	 * Gets the value of a field with a given contentlet
	 * @param contentlet
	 * @param theField a legacy field from the contentlet's parent Content Type
	 * @return Object from DB with field's value
	 * @see ContentletAPI#getFieldValue(Contentlet, com.dotcms.contenttype.model.field.Field)
	 * @deprecated use {@link ContentletAPI#getFieldValue(Contentlet, com.dotcms.contenttype.model.field.Field)} instead
	 */
    @Deprecated
    Object getFieldValue(Contentlet contentlet, Field theField);

	/**
	 * Gets the value of a field with a given contentlet
	 * @param contentlet
	 * @param theField
	 * @return Object from DB with field's value
	 */
	Object getFieldValue(Contentlet contentlet, com.dotcms.contenttype.model.field.Field theField);


	/**
	 * Adds a relationship to a contentlet
	 * @param contentlet
	 * @param linkInode
	 * @param relationName
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotSecurityException
	 * @throws DotDataException 
	 */
	public void addLinkToContentlet(Contentlet contentlet, String linkInode, String relationName, User user, boolean respectFrontendRoles)throws DotSecurityException, DotDataException;


	/**
	 * Returns the contentlets on a given page.  Will only return contentlets the user has permission to read/use
	 * You can pass -1 for languageId if you don't want to query to pull based
	 * on languages or 0 if you want to get the default language
	 * @param HTMLPageIdentifier
	 * @param containerIdentifier
	 * @param orderBy
	 * @param working
	 * @param languageId
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public List<Contentlet> findPageContentlets(String HTMLPageIdentifier, String containerIdentifier, String orderBy, boolean working, long languageId, User user, boolean respectFrontendRoles)throws DotSecurityException, DotDataException;

	/**
	 * Returns all contentlet's relationships for a given contentlet inode 
	 * @param contentletInode
	 * @return a ContentletRelationships object containing all relationships for the contentlet
	 * @throws DotDataException
	 */
	public ContentletRelationships getAllRelationships(String contentletInode, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Returns all contentlet's relationships for a given contentlet object 
	 * @param contentlet
	 * @return a ContentletRelationships object containing all relationships for the contentlet
	 * @throws DotDataException
	 */
	public ContentletRelationships getAllRelationships(Contentlet contentlet) throws DotDataException;

	/**
	 * Returns a contentlet's siblings for a given contentlet object.
	 * @param contentlet
	 * @return a ContentletRelationships object containing all relationships for the contentlet
	 * @throws DotDataException
	 */
	public List<Contentlet> getAllLanguages(Contentlet contentlet, Boolean isLiveContent, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * 
	 * @param contentlet1
	 * @param contentlet2
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException 
	 */
	public boolean isContentEqual(Contentlet contentlet1,Contentlet contentlet2, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException;

	/**
	 * This method archives the given contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public void archive(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotDataException,DotSecurityException, DotContentletStateException;

	/**
	 * Destroys the specified {@link Contentlet}. This method will automatically
	 * un-publish, archive, and delete ALL the information related to this
	 * contentlet in all of its languages.
	 *
	 * @param contentlet
	 *            - The contentlet that will be completely destroyed.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @param respectFrontendRoles
	 *            -
	 * @return If the contentlet was successfully destroyed, returns
	 *         {@code true}. Otherwise, returns {@code false}.
	 * @throws DotDataException
	 *             An error occurred when deleting the information from the
	 *             database.
	 * @throws DotSecurityException
	 *             The specified user does not have the required permissions to
	 *             perform this action.
	 */
	public boolean destroy(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException;

	/**
	 * Destroys the specified list of {@link Contentlet} objects . This method
	 * will automatically un-publish, archive, and delete ALL the information
	 * related to these contentlets in all of their languages.
	 *
	 * @param contentlets
	 *            - The list of contentlets that will be completely destroyed.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @param respectFrontendRoles
	 *            -
	 * @return If the contentlets were successfully destroyed, returns
	 *         {@code true}. Otherwise, returns {@code false}.
	 * @throws DotDataException
	 *             An error occurred when deleting the information from the
	 *             database.
	 * @throws DotSecurityException
	 *             The specified user does not have the required permissions to
	 *             perform this action.
	 */
	public boolean destroy(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException;

	/**
	 * Publishes a piece of content. 
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotContentletStateException
	 * @throws DotStateException 
	 */
	public void publish(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException,DotContentletStateException, DotContentletStateException, DotStateException;
	
	/**
	 * Publishes a piece of content. 
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotContentletStateException
	 * @throws DotStateException
	 */
	public void publish(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException,DotContentletStateException, DotStateException;

	/**
	 * This method unpublishes the given contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException if the contentlet cannot be unlocked by the user
	 */
	public void unpublish(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotDataException,DotSecurityException, DotContentletStateException;
	
	/**
	 * This method unpublishes the given contentlet
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException If one or more contentlets are locked and the user is not the one who locked it. It will unpublish all that are possible though
	 */
	public void unpublish(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) throws DotDataException,DotSecurityException, DotContentletStateException;

	/**
	 * This method archives the given contentlets
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public void archive(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) throws DotDataException,DotSecurityException, DotContentletStateException;

	/**
	 * This method unarchives the given contentlets
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException If one or more of the contentlets are not archived.  It will unarchive all that it can though
	 */
	public void unarchive(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException;

	/**
	 * This method unarchives the given contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException if the contentlet is not archived 
	 */
	public void unarchive(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException;
	
	/**
	 * This method completely deletes the given contentlet from the system and make a xml file backup
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public void deleteAllVersionsandBackup(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) throws DotDataException,DotSecurityException, DotContentletStateException;

	/**
	 * This method completely deletes the given contentlet from the system
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @return true when no errors occurs otherwise false
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public boolean delete(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotDataException,DotSecurityException, DotContentletStateException;

	/**
	 * This method completely deletes the given contentlet from the system. It was added for the jira issue
	 * http://jira.dotmarketing.net/browse/DOTCMS-2059
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @param allVersions
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public boolean delete(Contentlet contentlet, User user, boolean respectFrontendRoles, boolean allVersions) throws DotDataException,DotSecurityException, DotContentletStateException;

	/**
	 * Deletes the specified list of {@link Contentlet} objects ONLY in the
	 * specified language. If any of the specified contentlets is not archived,
	 * an exception will be thrown. If there's only one language for a given
	 * contentlet, the object will be destroyed.
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 * @returns true when no errors occurs otherwise false
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public boolean delete(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) throws DotDataException,DotSecurityException, DotContentletStateException;

	/**
	 * This method completely deletes the given contentlet from the system. It was added for the jira issue
	 * http://jira.dotmarketing.net/browse/DOTCMS-2059
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 * @param allVersions
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public void delete(List<Contentlet> contentlets, User user, boolean respectFrontendRoles, boolean allVersions) throws DotDataException,DotSecurityException, DotContentletStateException;

	/**
     * This method completely deletes all contentlets from the system for a the
     * given host.
     * <p>
     * It gathers all the contentlets from the host and then proceed with the
     * delete of each one
     * </p>
     * 
     * @param host
     * @param user
     * @param respectFrontendRoles
     * @return true when no errors occurs otherwise false
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public boolean deleteByHost(Host host, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;


	/**
	 * Deletes all related content from passed in contentlet and relationship 
	 * @param contentlet
	 * @param relationship
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException if contentlet doesn't have passed in relationship
	 */
	public void deleteRelatedContent(Contentlet contentlet, Relationship relationship, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException;
	
	/**
	 * Deletes all related content from passed in contentlet and relationship 
	 * @param contentlet
	 * @param relationship
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException if contentlet doesn't have passed in relationship
	 */
	public void deleteRelatedContent(Contentlet contentlet, Relationship relationship, boolean hasParent, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException;

    /**
     * Deletes all related content from passed in contentlet and relationship
     * @param contentlet
     * @param relationship
     * @param hasParent
     * @param user
     * @param respectFrontendRoles
     * @param contentletsToBeRelated if the delete operation is being used to update related content later,
     * the list of related content should be sent to perform an optimal reindex
     */
    void deleteRelatedContent(Contentlet contentlet, Relationship relationship,
            boolean hasParent, User user, boolean respectFrontendRoles,
            List<Contentlet> contentletsToBeRelated)
            throws DotDataException, DotSecurityException, DotContentletStateException;

    /**
     *
     * @param contentlet
     * @param relationship
     * @param hasParent
     */
    void invalidateRelatedContentCache(Contentlet contentlet, Relationship relationship,
            boolean hasParent);

    /**
     * Returns a list of all contentlets related to this instance given a RelationshipField variable
     * using pagination
     * @param variableName
     * @param user
     * @param respectFrontendRoles
     * @param pullByParents
     * @param limit
     * @param offset
     * @param sortBy
     * @return
     */
    List<Contentlet> getRelatedContent(Contentlet contentlet, String variableName, User user,
            boolean respectFrontendRoles, Boolean pullByParents, int limit, int offset,
            String sortBy);


    /**
     * Returns a list of all contentlets related to this instance given a RelationshipField variable
     * using pagination
     * @param contentlet
     * @param variableName
     * @param user
     * @param respectFrontendRoles
     * @param pullByParents
     * @param limit
     * @param offset
     * @param sortBy
     * @param language
     * @param live
     * @return
     */
    List<Contentlet> getRelatedContent(Contentlet contentlet, String variableName,
            User user,
            boolean respectFrontendRoles, Boolean pullByParents, int limit,
            int offset, String sortBy, long language, Boolean live);

    /**
	 * Associates the given list of contentlets using the relationship this
	 * methods removes old associated content and reset the relationships based
	 * on the list of content passed as parameter
	 * @param contentlet
	 * @param rel
	 * @param related
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException if one of the contentlets doesn't have passed in relationship. The method will still try to handle the other relationships
	 */
	public void relateContent(Contentlet contentlet, Relationship rel,List<Contentlet> related, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException;

	/**
	 * Associates the given list of contentlets using the relationship this
	 * methods removes old associated content and reset the relationships based
	 * on the list of content passed as parameter
	 * @param contentlet
	 * @param related
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException if one of the contentlets doesn't have passed in relationship. The method will still try to handle the other relationships
	 */
	public void relateContent(Contentlet contentlet, ContentletRelationshipRecords related, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException;


    /**
     * @deprecated This method should not be exposed. Use ContentletAPI.getRelated variations instead     * @param contentlet
     * @param rel
     * @param user
     * @param respectFrontendRoles
     * @param pullByParent
     * @param limit
     * @param offset
     * @param sortBy
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Deprecated
    List<Contentlet> filterRelatedContent(Contentlet contentlet, Relationship rel,
            User user, boolean respectFrontendRoles, Boolean pullByParent, int limit, int offset,
            String sortBy)
            throws DotDataException, DotSecurityException;

    /**
	 * Gets all related content, if this method is invoked with the same structures (where the parent and child structures are the same type)
	 * kind of relationship then all parents and children of the given contentlet will be retrieved in the same returned list
	 * @param contentlet
	 * @param rel
	 * @param user
	 * @param respectFrontendRoles
	 * @return List<Contentlet> list of contentlets
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<Contentlet> getRelatedContent(Contentlet contentlet, Relationship rel, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    /**
     * Gets all related content from the same structure (where the parent and child structures are the same type)
     * the parameter pullByParent if set to true tells the method to pull all children where the passed
     * contentlet is the parent, if set to false then the passed contentlet is the child and you want to pull
     * parents
     *
     * If this method is invoked using different structures kind of relationships then the parameter
     * pullByParent will be ignored, and the side of the relationship will be figured out automatically
     *
     * This method uses pagination if necessary (limit, offset, sortBy)
     * @param contentlet
     * @param rel
     * @param pullByParent
     * @param user
     * @param respectFrontendRoles
     * @param limit
     * @param offset
     * @param sortBy
     * @return
     * @throws DotDataException
     */
    List<Contentlet> getRelatedContent(Contentlet contentlet, Relationship rel,
            Boolean pullByParent, User user, boolean respectFrontendRoles, int limit, int offset,
            String sortBy)
            throws DotDataException;

    /**
     * Gets all related content from the same structure (where the parent and child structures are the same type)
     * the parameter pullByParent if set to true tells the method to pull all children where the passed
     * contentlet is the parent, if set to false then the passed contentlet is the child and you want to pull
     * parents
     *
     * If this method is invoked using different structures kind of relationships then the parameter
     * pullByParent will be ignored, and the side of the relationship will be figured out automatically
     *
     * This method uses pagination if necessary (limit, offset, sortBy)
     * @param contentlet
     * @param rel
     * @param pullByParent
     * @param user
     * @param respectFrontendRoles
     * @param limit
     * @param offset
     * @param sortBy
     * @param language
     * @param live
     * @return
     * @throws DotDataException
     */
    List<Contentlet> getRelatedContent(Contentlet contentlet, Relationship rel,
            Boolean pullByParent, User user, boolean respectFrontendRoles, int limit, int offset,
            String sortBy, long language, Boolean live)
            throws DotDataException;

    /**
     * Gets all related content from the same structure (where the parent and child structures are the same type)
     * the parameter pullByParent if set to true tells the method to pull all children where the passed
     * contentlet is the parent, if set to false then the passed contentlet is the child and you want to pull
     * parents
     *
     * If this method is invoked using different structures kind of relationships then the parameter
     * pullByParent will be ignored, and the side of the relationship will be figured out automatically
     *
     * This method uses pagination if necessary (limit, offset, sortBy)
     * @param contentlet
     * @param rel
     * @param pullByParent
     * @param user
     * @param respectFrontendRoles
     * @param language
     * @param live
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    List<Contentlet> getRelatedContent(Contentlet contentlet, Relationship rel,
            Boolean pullByParent, User user, boolean respectFrontendRoles, long language,
            Boolean live)
            throws DotDataException, DotSecurityException;

    /**
	 * Gets all related content from the same structure (where the parent and child structures are the same type)
	 * The parameter pullByParent if set to true tells the method to pull all children where the passed 
	 * contentlet is the parent, if set to false then the passed contentlet is the child and you want to pull 
	 * parents
	 * 
	 * If this method is invoked using different structures kind of relationships then the parameter
	 * pullByParent will be ignored, and the side of the relationship will be figured out automatically
	 * 
	 * @param contentlet
	 * @param rel
	 * @param pullByParent
	 * @param user
	 * @param respectFrontendRoles
	 * @return List<Contentlet> list of contentlets
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<Contentlet> getRelatedContent(Contentlet contentlet, Relationship rel, Boolean pullByParent, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * 
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException
	 */
	public void unlock(Contentlet contentlet, User user, boolean respectFrontendRoles)throws DotDataException, DotSecurityException;

	/**
	 * Use to lock a contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException - if the contentlet is null
	 */
	public void lock(Contentlet contentlet, User user, boolean respectFrontendRoles)throws DotDataException, DotSecurityException, DotContentletStateException;
	
	/**
	 * Reindex all content
	 * @throws DotReindexStateException
	 * @deprecated @see {@link ContentletAPI#refreshAllContent()}
	 */
	public void reindex()throws DotReindexStateException;
	
	/**
	 * reindex content for a given structure
	 * @param structure
	 * @throws DotReindexStateException
	 * @deprecated @see {@link ContentletAPI#refresh(Contentlet)}
	 */
	public void reindex(Structure structure)throws DotReindexStateException;
	
	/**
	 * reindex a single content
	 * @param contentlet
	 * @throws DotReindexStateException
	 * @throws DotDataException 
	 * @deprecated @see {@link ContentletAPI#refresh(Contentlet)}
	 */
	public void reindex(Contentlet contentlet)throws DotReindexStateException, DotDataException;

	/**
	 * Reindexes all content + clear the content caches
	 * @throws DotReindexStateException
	 */
	public void refreshAllContent()throws DotReindexStateException;
	
	/**
	 * Reindexes content for a given structure + refreshes the content from cache
	 * @param structure
	 * @throws DotReindexStateException
	 */
	public void refresh(Structure structure)throws DotReindexStateException;
	
	/**
	 * Reindexes a single content + refreshes it from cache
	 * @param contentlet
	 * @throws DotReindexStateException
	 * @throws DotDataException 
	 */
	public void refresh(Contentlet contentlet)throws DotReindexStateException, DotDataException;

	/**
	 * Reindexes content under a given host + refreshes the content from cache
	 * @param host
	 * @throws DotReindexStateException
	 */
	public void refreshContentUnderHost(Host host)throws DotReindexStateException;
	
	/**
	 * Reindexes content under a given folder + refreshes the content from cache
	 * @param folder
	 * @throws DotReindexStateException
	 */
	public void refreshContentUnderFolder(Folder folder)throws DotReindexStateException;

	/**
	 * Reindexes content under a given folder path
	 *
	 * @param hostId
	 * @param folderPath
	 * @throws DotReindexStateException
	 */
	public void refreshContentUnderFolderPath ( String hostId, String folderPath ) throws DotReindexStateException;

	/**
	 * Gets a file with a specific relationship type to the passed in contentlet
	 * @param contentlet
	 * @param relationshipType
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public Identifier getRelatedIdentifier(Contentlet contentlet, String relationshipType, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * Gets all related links to the contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<Link> getRelatedLinks(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * Allows you to checkout a content so it can be altered and checked in
	 * Note that this method is only intended for use with Checkin methods.
	 * Methods like publish, archive, unpublish,.. will fail when passing
	 * a contentlet returned by this method.
	 * @param contentletInode
	 * @param user
	 * @param respectFrontendRoles
	 * @return Contentlet object
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException if contentlet is not already persisted
	 */
	public Contentlet checkout(String contentletInode, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException;

    @CloseDBIfOpened
    Contentlet checkinWithoutVersioning(Contentlet contentlet,
            ContentletRelationships contentRelationships, List<Category> cats,
            List<Permission> permissions, User user,
            boolean respectFrontendRoles) throws DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException;

    /**
	 * Allows you to checkout contents so it can be altered and checked in.
	 * Note that this method is only intended for use with Checkin methods.
	 * Methods like publish, archive, unpublish,.. will fail when passing
	 * a contentlet returned by this method. 
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 * @return List<Contentlet> List of Contentlets
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException
	 */
	public List<Contentlet> checkout(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException;
	
	/**
	 * Allows you to checkout contents based on a lucene query so it can be altered and checked in.
	 * Note that this method is only intended for use with Checkin methods.
	 * Methods like publish, archive, unpublish,.. will fail when passing
	 * a contentlet returned by this method.
	 * @param luceneQuery
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException
	 */
	public List<Contentlet> checkoutWithQuery(String luceneQuery, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException;
	
	/**
	 * Allows you to checkout contents based on a lucene query so it can be altered and checked in, in a paginated fashion 
	 * Note that this method is only intended for use with Checkin methods.
	 * Methods like publish, archive, unpublish,.. will fail when passing
	 * a contentlet returned by this method.
	 * @param luceneQuery
	 * @param user
	 * @param respectFrontendRoles
	 * @param offset
	 * @param limit
	 * @return List<Contentlet> list of contentlets
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException
	 */
	public List<Contentlet> checkout(String luceneQuery, User user, boolean respectFrontendRoles, int offset, int limit) throws DotDataException, DotSecurityException, DotContentletStateException;
	
	/**
     * @deprecated This method should not be used because it does not consider self related content.
     * Use {@link ContentletAPI#checkin(Contentlet, ContentletRelationships, List, List, User, boolean)} instead
     * Will check in a new version of you contentlet. The inode of your contentlet must be null or empty.
	 * Note that the contentlet argument must be obtained using checkout methods.
     *
     * Important note to be considered: Related content can also be set using any of these methods:
     * 1. {@link Contentlet#setProperty(String, Object)} where the Object is a list of contentlets
     * 2. {@link Contentlet#setRelated(com.dotcms.contenttype.model.field.Field, List)}
     * 3. {@link Contentlet#setRelated(String, List)}
     * 4. {@link Contentlet#setRelatedById(com.dotcms.contenttype.model.field.Field, List, User, boolean)}
     * 5. {@link Contentlet#setRelatedById(String, List, User, boolean)}
     * 6. {@link Contentlet#setRelatedByQuery(com.dotcms.contenttype.model.field.Field, String, String, User, boolean)}
     * 7. {@link Contentlet#setRelatedByQuery(String, String, String, User, boolean)}
     *
     * When related content is sent as a parameter of a checkin call, any related content set
     * through any of the setters above will be ignored.
     * So, the ContentletRelationships in the check in call must be sent as null if the related
     * content is going to be saved through the setter methods
     *
     * When a relationship field value is set to null, related content for this relationship won't be modified
     *
     * When a relationship field value is set to empty list, related content for this relationship will be wiped out
     *
	 * @param contentlet - The inode of your contentlet must be null or empty.
	 * @param contentRelationships - throws IllegalArgumentException if null. Used to set relationships to new contentlet version 
	 * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 * @param permissions - throws IllegalArgumentException if null. Used to set permissions to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 * @return Contentlet object that was saved
	 * @throws IllegalArgumentException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException If inode not = to 0
	 * @throws DotContentletValidationException If content is not valid
	 */
	public Contentlet checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) throws IllegalArgumentException,DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException;
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be null or empty.
	 * This version of checkin contains a more complex structure to pass the relationship in order
	 * to handle a same structures (where the parent and child structures are the same) kind of relationships
	 * in that case you have to specify if the role of the content is the parent of the child of the relationship.
	 * Note that the contentlet argument must be obtained using checkout methods.
     *
     * Important note to be considered: Related content can also be set using any of these methods:
     * 1. {@link Contentlet#setProperty(String, Object)} where the Object is a list of contentlets
     * 2. {@link Contentlet#setRelated(com.dotcms.contenttype.model.field.Field, List)}
     * 3. {@link Contentlet#setRelated(String, List)}
     * 4. {@link Contentlet#setRelatedById(com.dotcms.contenttype.model.field.Field, List, User, boolean)}
     * 5. {@link Contentlet#setRelatedById(String, List, User, boolean)}
     * 6. {@link Contentlet#setRelatedByQuery(com.dotcms.contenttype.model.field.Field, String, String, User, boolean)}
     * 7. {@link Contentlet#setRelatedByQuery(String, String, String, User, boolean)}
     *
     * When related content is sent as a parameter of a checkin call, any related content set
     * through any of the setters above will be ignored.
     * So, the ContentletRelationships in the check in call must be sent as null if the related
     * content is going to be saved through the setter methods
     *
     * When a relationship field value is set to null, related content for this relationship won't be modified
     *
     * When a relationship field value is set to empty list, related content for this relationship will be wiped out
     *
	 * @param currentContentlet - The inode of your contentlet must be null or empty.
	 * @param relationshipsData - 
	 * @param cats
	 * @param selectedPermissions
	 * @param user
	 * @param respectFrontendRoles
	 * @return Contentlet object that was saved
	 */
	public Contentlet checkin(Contentlet currentContentlet, ContentletRelationships relationshipsData, List<Category> cats, List<Permission> selectedPermissions, User user,	boolean respectFrontendRoles)  throws IllegalArgumentException,DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException;

	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be null or empty.
	 * This version of checkin contains a more complex structure to pass the relationship in order
	 * to handle a same structures (where the parent and child structures are the same) kind of relationships
	 * in that case you have to specify if the role of the content is the parent of the child of the relationship.
	 * Note that the contentlet argument must be obtained using checkout methods.
     *
     * Important note to be considered: Related content can also be set using any of these methods:
     * 1. {@link Contentlet#setProperty(String, Object)} where the Object is a list of contentlets
     * 2. {@link Contentlet#setRelated(com.dotcms.contenttype.model.field.Field, List)}
     * 3. {@link Contentlet#setRelated(String, List)}
     * 4. {@link Contentlet#setRelatedById(com.dotcms.contenttype.model.field.Field, List, User, boolean)}
     * 5. {@link Contentlet#setRelatedById(String, List, User, boolean)}
     * 6. {@link Contentlet#setRelatedByQuery(com.dotcms.contenttype.model.field.Field, String, String, User, boolean)}
     * 7. {@link Contentlet#setRelatedByQuery(String, String, String, User, boolean)}
     *
     * When related content is sent as a parameter of a checkin call, any related content set
     * through any of the setters above will be ignored.
     * So, the ContentletRelationships in the check in call must be sent as null if the related
     * content is going to be saved through the setter methods
     *
     * When a relationship field value is set to null, related content for this relationship won't be modified
     *
     * When a relationship field value is set to empty list, related content for this relationship will be wiped out
	 *
	 * @param currentContentlet    - The inode of your contentlet must be null or empty.
	 * @param relationshipsData    -
	 * @param cats
	 * @param selectedPermissions
	 * @param user
	 * @param respectFrontendRoles
	 * @param generateSystemEvent  true in order to generate a system event for this checking operation
	 * @return Contentlet object that was saved
	 */
	public Contentlet checkin(Contentlet currentContentlet, ContentletRelationships relationshipsData,
							  List<Category> cats, List<Permission> selectedPermissions, User user,
							  boolean respectFrontendRoles, boolean generateSystemEvent) throws IllegalArgumentException, DotDataException, DotSecurityException, DotContentletStateException, DotContentletValidationException;

	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be null or empty.
	 * This version of checkin contains a might include dependencies such as the relationship in order
	 * to handle a same structures (where the parent and child structures are the same) kind of relationships
	 * in that case you have to specify if the role of the content is the parent of the child of the relationship.
	 * Note that the contentlet argument must be obtained using checkout methods.
     *
     * Important note to be considered: Related content can also be set using any of these methods:
     * 1. {@link Contentlet#setProperty(String, Object)} where the Object is a list of contentlets
     * 2. {@link Contentlet#setRelated(com.dotcms.contenttype.model.field.Field, List)}
     * 3. {@link Contentlet#setRelated(String, List)}
     * 4. {@link Contentlet#setRelatedById(com.dotcms.contenttype.model.field.Field, List, User, boolean)}
     * 5. {@link Contentlet#setRelatedById(String, List, User, boolean)}
     * 6. {@link Contentlet#setRelatedByQuery(com.dotcms.contenttype.model.field.Field, String, String, User, boolean)}
     * 7. {@link Contentlet#setRelatedByQuery(String, String, String, User, boolean)}
     *
     * When related content is sent as a parameter of a checkin call, any related content set
     * through any of the setters above will be ignored.
     * So, the ContentletRelationships in the check in call must be sent as null if the related
     * content is going to be saved through the setter methods
     *
     * When a relationship field value is set to null, related content for this relationship won't be modified
     *
     * When a relationship field value is set to empty list, related content for this relationship will be wiped out
	 *
	 * @param contentlet    - The inode of your contentlet must be null or empty.
	 * @param contentletDependencies {@link ContentletDependencies}
	 *            include the categories, relationships, modUser, respectAnonymousPermissions and generates system event (in order to generate a system event for this checking operation)
	 * @return Contentlet object that was saved
	 */
	Contentlet checkin(final Contentlet contentlet, ContentletDependencies contentletDependencies) throws DotSecurityException, DotDataException;

	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be null or empty.
	 * Note that the contentlet argument must be obtained using checkout methods.
	 * @param contentlet - The inode of your contentlet must be null or empty.
	 * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 * @param permissions - throws IllegalArgumentException if null. Used to set permissions to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 * @return Contentlet object that was saved
	 * @throws IllegalArgumentException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException If inode not = to 0
	 * @throws DotContentletValidationException If content is not valid
	 */
	public Contentlet checkin(Contentlet contentlet, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) throws IllegalArgumentException,DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException;
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be null or empty.
	 * Note that the contentlet argument must be obtained using checkout methods.  
	 * @param contentlet - The inode of your contentlet must be null or empty.
	 * @param permissions - throws IllegalArgumentException if null. Used to set permissions to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 * @return Contentlet object that was saved
	 * @throws IllegalArgumentException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException If inode not = to 0
	 * @throws DotContentletValidationException If content is not valid
	 */
	public Contentlet checkin(Contentlet contentlet, List<Permission> permissions, User user,boolean respectFrontendRoles) throws IllegalArgumentException,DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException;
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be null or empty.
	 * Note that the contentlet argument must be obtained using checkout methods.  
	 * @param contentlet - The inode of your contentlet must be null or empty.
	 * @param user
	 * @param respectFrontendRoles
	 * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 * @return Contentlet object that was saved
	 * @throws IllegalArgumentException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException If inode not = to 0
	 * @throws DotContentletValidationException If content is not valid
	 */
	public Contentlet checkin(Contentlet contentlet, User user, boolean respectFrontendRoles, List<Category> cats) throws IllegalArgumentException,DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException;
	
	/**
	 * @deprecated This method should not be used because it does not consider self related content.
     * Use {@link ContentletAPI#checkin(Contentlet, ContentletRelationships, List, List, User, boolean)} instead
     * Will check in a new version of you contentlet. The inode of your contentlet must be null or empty.
	 * Note that the contentlet argument must be obtained using checkout methods.
     *
     * Important note to be considered: Related content can also be set using any of these methods:
     * 1. {@link Contentlet#setProperty(String, Object)} where the Object is a list of contentlets
     * 2. {@link Contentlet#setRelated(com.dotcms.contenttype.model.field.Field, List)}
     * 3. {@link Contentlet#setRelated(String, List)}
     * 4. {@link Contentlet#setRelatedById(com.dotcms.contenttype.model.field.Field, List, User, boolean)}
     * 5. {@link Contentlet#setRelatedById(String, List, User, boolean)}
     * 6. {@link Contentlet#setRelatedByQuery(com.dotcms.contenttype.model.field.Field, String, String, User, boolean)}
     * 7. {@link Contentlet#setRelatedByQuery(String, String, String, User, boolean)}
     *
     * When related content is sent as a parameter of a checkin call, any related content set
     * through any of the setters above will be ignored.
     * So, the ContentletRelationships in the check in call must be sent as null if the related
     * content is going to be saved through the setter methods
     *
     * When a relationship field value is set to null, related content for this relationship won't be modified
     *
     * When a relationship field value is set to empty list, related content for this relationship will be wiped out
     *
	 * @param contentlet - The inode of your contentlet must be null or empty.
	 * @param contentRelationships - throws IllegalArgumentException if null. Used to set relationships to new contentlet version 
	 * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 * @return Contentlet object that was saved
	 * @throws IllegalArgumentException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException If inode not = to 0
	 * @throws DotContentletValidationException If content is not valid
	 */
	public Contentlet checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats, User user,boolean respectFrontendRoles) throws IllegalArgumentException,DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException;
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be null or empty.
	 * Note that the contentlet argument must be obtained using checkout methods.  
	 * @param contentlet - The inode of your contentlet must be null or empty.
	 * @param user
	 * @param respectFrontendRoles
	 * @return Contentlet object that was saved
	 * @throws IllegalArgumentException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException If inode not = to 0
	 * @throws DotContentletValidationException If content is not valid
	 */
	public Contentlet checkin(Contentlet contentlet, User user,boolean respectFrontendRoles) throws IllegalArgumentException,DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException;

	/**
	 * @deprecated This method should not be used because it does not consider self related content.
     * Use {@link ContentletAPI#checkin(Contentlet, ContentletRelationships, List, List, User, boolean)} instead
     * Will check in a new version of you contentlet. The inode of your contentlet must be null or empty.
	 * Note that the contentlet argument must be obtained using checkout methods.
     *
     * Important note to be considered: Related content can also be set using any of these methods:
     * 1. {@link Contentlet#setProperty(String, Object)} where the Object is a list of contentlets
     * 2. {@link Contentlet#setRelated(com.dotcms.contenttype.model.field.Field, List)}
     * 3. {@link Contentlet#setRelated(String, List)}
     * 4. {@link Contentlet#setRelatedById(com.dotcms.contenttype.model.field.Field, List, User, boolean)}
     * 5. {@link Contentlet#setRelatedById(String, List, User, boolean)}
     * 6. {@link Contentlet#setRelatedByQuery(com.dotcms.contenttype.model.field.Field, String, String, User, boolean)}
     * 7. {@link Contentlet#setRelatedByQuery(String, String, String, User, boolean)}
     *
     * When related content is sent as a parameter of a checkin call, any related content set
     * through any of the setters above will be ignored.
     * So, the ContentletRelationships in the check in call must be sent as null if the related
     * content is going to be saved through the setter methods
     *
     * When a relationship field value is set to null, related content for this relationship won't be modified
     *
     * When a relationship field value is set to empty list, related content for this relationship will be wiped out
     *
	 * @param contentlet - The inode of your contentlet must be null or empty.
	 * @param contentRelationships - throws IllegalArgumentException if null. Used to set relationships to new contentlet version 
	 * @param user
	 * @param respectFrontendRoles
	 * @return Contentlet object that was saved
	 * @throws IllegalArgumentException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException If inode not = to 0
	 * @throws DotContentletValidationException If content is not valid
	 */
	public Contentlet checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, User user,boolean respectFrontendRoles) throws IllegalArgumentException,DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException;
	
	/**
	 * @deprecated This method should not be used because it does not consider self related content.
     * Use {@link ContentletAPI#checkinWithoutVersioning(Contentlet, ContentletRelationships, List, List, User, boolean)} instead
     * Will check in a update of your contentlet without generating a new version. The inode of your contentlet must be different from null/empty.
	 * Note this method will also attempt to publish the contentlet and related assets (when checking in) without altering the mod date or mod user.
	 * Note that the contentlet argument must be obtained using checkout methods.
     *
     * Important note to be considered: Related content can also be set using any of these methods:
     * 1. {@link Contentlet#setProperty(String, Object)} where the Object is a list of contentlets
     * 2. {@link Contentlet#setRelated(com.dotcms.contenttype.model.field.Field, List)}
     * 3. {@link Contentlet#setRelated(String, List)}
     * 4. {@link Contentlet#setRelatedById(com.dotcms.contenttype.model.field.Field, List, User, boolean)}
     * 5. {@link Contentlet#setRelatedById(String, List, User, boolean)}
     * 6. {@link Contentlet#setRelatedByQuery(com.dotcms.contenttype.model.field.Field, String, String, User, boolean)}
     * 7. {@link Contentlet#setRelatedByQuery(String, String, String, User, boolean)}
     *
     * When related content is sent as a parameter of a checkin call, any related content set
     * through any of the setters above will be ignored.
     * So, the ContentletRelationships in the check in call must be sent as null if the related
     * content is going to be saved through the setter methods
     *
     * When a relationship field value is set to null, related content for this relationship won't be modified
     *
     * When a relationship field value is set to empty list, related content for this relationship will be wiped out
     *
	 * @param contentlet - The inode of your contentlet must be different from null/empty.
	 * @param contentRelationships - Used to set relationships to updated contentlet version 
	 * @param user
	 * @param respectFrontendRoles
	 * @return Contentlet object that was saved
	 * @throws IllegalArgumentException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException If exist another contentlet working or live
	 * @throws DotContentletValidationException If content is not valid
	 */
	public Contentlet checkinWithoutVersioning(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) throws DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException;
	
	/**
	 * Will make the passed in contentlet the working copy. 
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotDataException 
	 * @throws DotContentletStateException 
	 * @throws DotSecurityException 
	 */
	public void restoreVersion(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotSecurityException, DotContentletStateException, DotDataException;

	/**
	 * Retrieves all versions for a contentlet identifier
	 * Note: This method could pull too many versions.
	 * @param identifier
	 * @param user
	 * @param respectFrontendRoles
	 * @return List<Contentlet> List of Contents with all of its versions
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotStateException if the identifier is for contentlet
	 */

	public List<Contentlet> findAllVersions(Identifier identifier, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException, DotStateException;

	/**
	 * Retrieves all versions for a contentlet identifier.
	 * Note: This method could pull too many versions.
	 * @param identifier - Identifier object that belongs to a contentlet
	 * @param bringOldVersions - boolean value which determines if old versions (non-live, non-working
	 * 	should be brought here). @see {@link ContentletAPI#copyContentlet(Contentlet, Host, Folder, User, String, boolean)} method,
	 * 	which requires passing in only live/working
	 * 	versions of contents to be copied
	 * @param user - User in context who has triggered this call.
	 * @param respectFrontendRoles - For permissions validations
	 * List<Contentlet> List of Contents with all of its versions
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotStateException if the identifier is for contentlet
	 */

	public List<Contentlet> findAllVersions(Identifier identifier, boolean bringOldVersions, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException, DotStateException;

	/**
	 * Retrieves all versions for a contentlet identifier checked in by a real user meaning not the system user
	 * @param identifier
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotStateException if the identifier is for contentlet
	 */

	public List<Contentlet> findAllUserVersions(Identifier identifier, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException, DotStateException;
	
	/**
	 * Meant to get the title or name of a contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @return String with title/name of a contentlet
	 * @throws DotSecurityException
	 * @throws DotContentletStateException
	 * @throws DotDataException
	 */
	public String getName(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotSecurityException, DotContentletStateException, DotDataException;
	
	/**
	 * Copies properties from the map to the contentlet
	 * @param contentlet contentlet to copy to
	 * @param properties
	 * @throws DotContentletStateException  if the map passed in has properties that don't match the contentlet
	 * @throws DotSecurityException
	 */
	public void copyProperties(Contentlet contentlet, Map<String, Object> properties) throws DotContentletStateException, DotSecurityException;
	
	/**
	 * Use to check if the inode id is a contentlet
	 * @param inode id to check
	 * @return true if inode belongs to a contentlet
	 * @throws DotDataException
	 * @throws DotRuntimeException
	 */
	public boolean isContentlet(String inode)throws DotDataException, DotRuntimeException;
	
	/**
	 * Will return all content assigned to a specified Category
	 * @param category Category to look for
	 * @param languageId language to pull content for. If 0 will return all languages
	 * @param live should return live or working content
	 * @param orderBy indexName(previously known as dbColumnName) to order by. Can be null or empty string
	 * @param user
	 * @param respectFrontendRoles
	 * @return List<Contentlet> list of contentlets
	 * @throws DotDataException
	 * @throws DotContentletStateException
	 * @throws DotSecurityException
	 */
	public List<Contentlet> find(Category category, long languageId, boolean live, String orderBy, User user, boolean respectFrontendRoles) throws DotDataException, DotContentletStateException, DotSecurityException;

	/**
	 * Will return all content assigned to a specified Categories
	 * @param categories - List of categories to look for
	 * @param languageId language to pull content for. If 0 will return all languages
	 * @param live should return live or working content
	 * @param orderBy indexName(previously known as dbColumnName) to order by. Can be null or empty string
	 * @param user
	 * @param respectFrontendRoles
	 * @return List<Contentlet> list of contentlets
	 * @throws DotDataException
	 * @throws DotContentletStateException
	 * @throws DotSecurityException
	 */
	public List<Contentlet> find(List<Category> categories, long languageId, boolean live, String orderBy, User user, boolean respectFrontendRoles) throws DotDataException, DotContentletStateException, DotSecurityException;

	/**
	 * Use to set contentlet properties.  The value should be String, the proper type of the property
	 * @param contentlet
	 * @param field
	 * @param value
	 * @throws DotContentletStateException if the object isn't the proper type or cannot be converted to the proper type
	 */
	public void setContentletProperty(Contentlet contentlet, Field field, Object value) throws DotContentletStateException;
	
	/**
	 * Use to validate your contentlet.
	 * @param contentlet
	 * @param cats
	 * @throws DotContentletValidationException will be thrown if the contentlet is not valid.  
	 * Use the notValidFields property of the exception to get which fields where not valid  
	 */
	public void validateContentlet(Contentlet contentlet,List<Category> cats)throws DotContentletValidationException; 
	
	/**
	 * Use to validate your contentlet.
	 * @param contentlet
	 * @param contentRelationships
	 * @param cats
	 * @throws DotContentletValidationException will be thrown if the contentlet is not valid.  
	 * Use the notValidFields property of the exception to get which fields where not valid
	 */
	public void validateContentlet(Contentlet contentlet,Map<Relationship, List<Contentlet>> contentRelationships,List<Category> cats) throws DotContentletValidationException;
	
	/**
	 * Use to validate your contentlet.
	 * @param contentlet
	 * @param contentRelationships
	 * @param cats
	 * @throws DotContentletValidationException will be thrown if the contentlet is not valid.  
	 * Use the notValidFields property of the exception to get which fields where not valid
	 */
	public void validateContentlet(Contentlet contentlet, ContentletRelationships contentRelationships, List<Category> cats) throws DotContentletValidationException;
	
	/**
	 * Use to determine if if the field value is a String value withing the contentlet object
	 * @param field
	 * @return true if Field Type is string
	 */
	public boolean isFieldTypeString(Field field);
	
	/**
	 * Use to determine if if the field value is a Date value withing the contentlet object
	 * @param field
	 * @return true if Field Type is date
	 */
	public boolean isFieldTypeDate(Field field);
	
	/**
	 * Use to determine if if the field value is a Long value withing the contentlet object
	 * @param field
	 * @return true if Field Type is long
	 */
	public boolean isFieldTypeLong(Field field);
	
	/**
	 * Use to determine if if the field value is a Boolean value withing the contentlet object
	 * @param field
	 * @return true if Field Type is boolean
	 */
	public boolean isFieldTypeBoolean(Field field);
	
	/**
	 * Use to determine if if the field value is a Float value withing the contentlet object
	 * @param field
	 * @return true if Field Type is float
	 */
	public boolean isFieldTypeFloat(Field field);

	/**
	 * Converts a "fat" (legacy) contentlet into a new contentlet.
	 * @param fatty Fat contentlet to be converted.
	 * @return A "light" contentlet.
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	public Contentlet convertFatContentletToContentlet (com.dotmarketing.portlets.contentlet.business.Contentlet fatty) throws DotDataException, DotSecurityException;
	
	/**
	 * Converts a "light" contentlet into a "fat" (legacy) contentlet.
	 * @param cont A "light" contentlet to be converted.
	 * @return fatty Fat contentlet.
	 * @throws DotDataException
	 */
	public com.dotmarketing.portlets.contentlet.business.Contentlet convertContentletToFatContentlet (Contentlet cont, com.dotmarketing.portlets.contentlet.business.Contentlet fatty) throws DotDataException;
    
   /**
    * Delete old versions contents that are older than a given date
	* Used by the Drop Old Assets Version Tool. For regular deletion
	* of contents, see {@link ContentletAPI#delete(Contentlet, User, boolean)}
    * @param deleteFrom
    * @return Integer value with amount of contents that were deleted
    * @throws DotDataException
	* @see com.dotmarketing.portlets.cmsmaintenance.factories.CMSMaintenanceFactory#deleteOldAssetVersions(Date)
    */
	public int deleteOldContent(Date deleteFrom) throws DotDataException;
	
	/**
	 * Find all contents from a Content type (max value is 500)
	 * where a given field's value is not null/empty.
	 * @param structureInode
	 * @param field
	 * @param user
	 * @return List<String> List of values parsed as strings. Note: this list does not indicate
	 * which contents hold the values pulled here.
	 * @throws DotDataException
	 */
	public List<String> findFieldValues(String structureInode, Field field, User user, boolean respectFrontEndRoles) throws DotDataException;

	/**
	 * Fetches the File Name stored under the contentlet and field
	 * @param contentletInode
	 * @param velocityVariableName
	 * @return fileName
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public java.io.File getBinaryFile(String contentletInode,String velocityVariableName,User user) throws DotDataException,DotSecurityException;

	/**
	 * gets the number of contentlets in the system. This number includes all versions not distinct identifiers
	 * @return  long value indicating the number of contentlets in the system.
	 * @throws DotDataException
	 */
	public long contentletCount() throws DotDataException;
	
	/**
	 * gets the number of contentlet identifiers in the system. This number includes all versions not distinct identifiers
	 * @return long value indicating the number of contentlet identifiers in the system
	 * @throws DotDataException
	 */
	public long contentletIdentifierCount() throws DotDataException;

	/**
	 * Gets a list of contentlets given a Content Identifier.
	 * This one goes to the DB rather than the ES Index
	 * @param identifier
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<Contentlet> getSiblings(String identifier)throws DotDataException, DotSecurityException ;
	
	/**
	 * Will search the DB. Uses the QueryUtil to build
	 * your query object
	 * @param query
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws ValidationException
	 * @throws DotDataException
	 * @see com.dotmarketing.business.query.QueryUtil#DBSearch(Query, Map, String, User, boolean, boolean)
	 */
	public List<Map<String, Serializable>> DBSearch(Query query, User user,boolean respectFrontendRoles) throws ValidationException, DotDataException;
	
	/**
	 * Method will time out after 30 seconds returning false
	 * @deprecated Use the Contentlet.setWaitForIndex instead
	 * @param inode
	 * @return
	 */
	@Deprecated
	public boolean isInodeIndexed(String inode);
	
	/**
	 *  this version optionally adds a +live:true to the search
	 *  if parameter live is true.
	 *  @deprecated Use the Contentlet.setWaitForIndex instead
	 * @param inode
	 * @param live
	 * @return
	 */
	@Deprecated
	public boolean isInodeIndexed(String inode, boolean live);

	/**
	 * This version optionally adds a +live:true to the search if parameter live is true.
	 * @param inode inode to check if it is indexed
	 * @param live boolean true if wants to check the live version
	 * @param secondsToWait int how long to wait before timing out (it milliseconds, so for instance 3 seconds will be 3000)
	 * @return boolean true if it is indexed in the time interval
	 */
	public boolean isInodeIndexed(String inode, boolean live, int secondsToWait);

	/**
	 * Waits until the contentlet with the given inode is indexed with the given status conditions.
	 * <p><strong>Example of the resulting query when using this method:</strong>
	 * +inode:﻿38a3f133-85e1-4b07-b55e-179f38303b90 +live:false +working:true </p> <p>This method
	 * will time out after 30 seconds returning false</p>
	 * @deprecated Use the Contentlet.setWaitForIndex instead
	 */
	@Deprecated
	public boolean isInodeIndexed(String inode, boolean live, boolean working);

	/**
	 * Method will time out after 30 seconds returning false
	 * @param inode
	 * @param secondsToWait - how long to wait before timing out (it milliseconds, so for instance 3 seconds will be 3000)
	 * @return boolean true if it is indexed in the time interval
	 */
	public boolean isInodeIndexed(String inode, int secondsToWait);

    /**
     * Method checks if a content inode belongs to an archived and indexed content
     * @param inode
     * @return Boolean value
     */
    public boolean isInodeIndexedArchived(String inode);

	/**
	 * Method checks if a content inode belongs to an archived and indexed content
	 * @param inode
	 * @param secondsToWait - how long to wait before timing out
	 * @return Boolean value
	 */
	public boolean isInodeIndexedArchived(String inode, int secondsToWait);

	/**
	 * Method will update hostInode of content to SYSTEM_HOST
	 * @param hostIdentifier
	 * @throws DotSecurityException 
	 */	
	public void UpdateContentWithSystemHost(String hostIdentifier) throws DotDataException, DotSecurityException;
	
	/**
	 * Method will remove User References of the given userId in Contentlet  
	 * @param userId
	 * @throws DotSecurityException 
	 */	
	public void removeUserReferences(String userId)throws DotDataException, DotSecurityException;

	/**
	 * Method will remove User References of the given userId in Contentlets
	 * with the specified user id  
	 * @param userToReplace the user to replace
	 * @param replacementUserId Replacement User Id
	 * @param user the user requesting the operation
	 * @exception DotDataException There is a data inconsistency
	 * @throws DotSecurityException 
	 */	
	public void updateUserReferences(User userToReplace, String replacementUserId, User user)throws DotDataException, DotSecurityException;

	/**
	 * Return the URL Map for the specified content if the structure associated to the content has the URL Map Pattern set.
	 * 
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @return String with the URL Map. Null if the structure of the content doesn't have the URL Map Pattern set.
	 */
	public String getUrlMapForContentlet(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException;
	
	/**
	 * Deletes the given version of the contentlet from the system
	 * 
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 */
	public void deleteVersion(Contentlet contentlet, User user,	boolean respectFrontendRoles) throws DotDataException,DotSecurityException;

	/**
	 * Checks if the version you are saving is live=false.  If it is, this method will save 
	 * WITHOUT creating a new version.  Otherwise, it will create a new working (Draft) version and return it to you
	 * @param contentlet - The inode of your contentlet must not be null.
	 * @param contentRelationships - throws IllegalArgumentException if null. Used to set relationships to new contentlet version 
	 * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 * @param permissions - throws IllegalArgumentException if null. Used to set permissions to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 * @throws IllegalArgumentException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException If inode null
	 * @throws DotContentletValidationException If content is not valid
	 */
	public Contentlet saveDraft(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) throws IllegalArgumentException,DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException;

	/**
	 * Checks if the version you are saving is live=false.  If it is, this method will save
	 * WITHOUT creating a new version.  Otherwise, it will create a new working (Draft) version and return it to you
	 * @param contentlet - The inode of your contentlet must not be null.
	 * @param contentletRelationships - throws IllegalArgumentException if null. Used to set relationships to new contentlet version
	 * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 * @param permissions - throws IllegalArgumentException if null. Used to set permissions to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 * @throws IllegalArgumentException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException If inode null
	 * @throws DotContentletValidationException If content is not valid
	 */
	public Contentlet saveDraft(Contentlet contentlet, ContentletRelationships contentletRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) throws IllegalArgumentException,DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException;

	/**
	 * The search here takes a lucene query and pulls Contentlets for you, using the identifier of the contentlet.You can pass sortBy as null if you do not 
	 * have a field to sort by.  limit should be 0 if no limit and the offset should be -1 is you are not paginating.
	 * The returned list will be filtered with only the contentlets that the user can read(use).  you can of course also
	 * pass permissions to further limit in the lucene query itself
	 * @param luceneQuery
	 * @param limit
	 * @param offset
	 * @param sortBy
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<Contentlet> searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * The search here takes a lucene query and pulls Contentlets for you, using the identifier of the contentlet.You can pass sortBy as null if you do not 
	 * have a field to sort by.  limit should be 0 if no limit and the offset should be -1 is you are not paginating.
	 * The returned list will be filtered with only the contentlets that match the required permission.  You can of course also
	 * pass permissions to further limit in the lucene query itself
	 * @param luceneQuery
	 * @param limit
	 * @param offset
	 * @param sortBy indexName(previously known as dbColumnName) to order by. Can be null or empty string
	 * @param user
	 * @param respectFrontendRoles
	 * @param requiredPermission
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<Contentlet> searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission) throws DotDataException, DotSecurityException;

	/**
	 * The search here takes a lucene query and pulls Contentlets for you, using the identifier of the contentlet.You can pass sortBy as null if you do not 
	 * have a field to sort by.  limit should be 0 if no limit and the offset should be -1 is you are not paginating.
	 * The returned list will be filtered with only the contentlets that match the required permission.  You can of course also
	 * pass permissions to further limit in the lucene query itself
	 * Searches default language if anyLanguage is false, and searches all languages if anyLanguage is true.
	 * @param luceneQuery
	 * @param limit
	 * @param offset
	 * @param sortBy indexName(previously known as dbColumnName) to order by. Can be null or empty string
	 * @param user
	 * @param respectFrontendRoles
	 * @param requiredPermission
	 * @param anyLanguage
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<Contentlet> searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission, boolean anyLanguage) throws DotDataException,DotSecurityException;

	/**
	 * Will update contents that reference the given folder to point to it's parent folder, if it's a top folder it will set folder to be SYSTEM_FOLDER
	 * @param folder
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	public void removeFolderReferences(Folder folder)throws DotDataException, DotSecurityException; 

	/**
	 * Tests whether a user can potentially lock a piece of content (needed to test before publish, etc).  This method will return false if content is already locked
	 * by another user.
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotLockException
	 */
	public boolean canLock(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotLockException;
	
	/**
	 * Tests whether a user can potentially lock a piece of content (needed to test before publish, etc).  This method will return false if content is already locked
	 * by another user.
	 * @param contentlet
	 * @param user
	 * @return
	 * @throws DotLockException
	 */
	public boolean canLock(Contentlet contentlet, User user) throws DotLockException;

	/**
	 * Returns the ContentRelationships Map for the specified content.
	 * 
	 * @param contentlet
	 * @param user
	 * @return Map with the ContentRelationships. Empty Map if the content doesn't have associated relationships.
	 */
	
	public Map<Relationship, List<Contentlet>> findContentRelationships(Contentlet contentlet, User user) throws DotDataException, DotSecurityException;

	/**
	 * Reads the field from storage. It is useful to implement lazy loading. 
	 * 
	 */
    public Object loadField(String inode, Field f) throws DotDataException;
    
    /**
     * Allows to count how many records match the specified lucene query and user
     * 
     * @param luceneQuery
     * @param user
     * @param respectFrontendRoles
     * @return
     */
    public long indexCount(String luceneQuery, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    /**
     * Gets the top viewed contents identifiers and numberOfViews  for a particular structure for a specified date interval
     * 
     * @param structureVariableName
     * @param startDate
     * @param endDate
     * @param user
     * @return
     */
	public List<Map<String, String>> getMostViewedContent(String structureVariableName,String startDate, String endDate, User user);
	
	/**
	 * Tasks should be done after a content publish. It is intended to clean up properly the system state after
	 * doing a content publish directly on db or versionableAPI and not by calling the proper method conAPI.publish
	 * 
	 * @param contentlet
	 * @param isNew Boolean that indicates if passed-in content is a new one. when true ContentService and ContentMapService is invalidated
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotContentletStateException
	 * @throws DotStateException
	 */
	public void publishAssociated(Contentlet contentlet, boolean isNew) throws DotSecurityException, DotDataException, DotContentletStateException, DotStateException;
	
	/**
	 * Tasks should be done after a content publish. It is intended to clean up properly the system state after
     * doing a content publish directly on db or versionableAPI and not by calling the proper method conAPI.publish
     * 
	 * @param contentlet
	 * @param isNew Boolean that indicates if it is a new content. when true ContentService and ContentMapService is invalidated
	 * @param isNewVersion boolean that indicates if mod_date and mod_user properties should be updated
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotContentletStateException
	 * @throws DotStateException
	 */
	void publishAssociated(Contentlet contentlet, boolean isNew, boolean isNewVersion) throws DotSecurityException, DotDataException, DotStateException;

	/**
	 * This will only return the list of inodes as hits, and does not load the contentlets from cache.
	 * <br><strong>NOTE: </strong> dotCMS Enterprise only feature.
	 *
	 * @param esQuery
	 * @param live
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public org.elasticsearch.action.search.SearchResponse esSearchRaw ( String esQuery, boolean live, User user, boolean respectFrontendRoles ) throws DotSecurityException, DotDataException;

	/**
	 * Executes a given Elastic Search query.
	 * <br><strong>NOTE: </strong> dotCMS Enterprise only feature.
	 *
	 * @param esQuery
	 * @param live
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public ESSearchResults esSearch ( String esQuery, boolean live, User user, boolean respectFrontendRoles ) throws DotSecurityException, DotDataException;

	/**
	 *
	 * @param inodes
	 * @param user
	 * @return
	 * @throws DotDataException
	 */
	int updateModDate(final Set<String> inodes, final User user) throws DotDataException;

	/**
	 * This will find the live/working version of a piece of content for the language passed in.  If the content is not found in the language passed in
	 * then the method will try to "fallback" and return the content in the default language based on the properties set in the dotmarketing-config.properties
	 * @param identifier
	 * @param live
	 * @param incomingLangId
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotSecurityException
	 */
    Optional<Contentlet> findContentletByIdentifierOrFallback(String identifier, boolean live, long incomingLangId, User user,
            boolean respectFrontendRoles);

    /**
     * System function for finding a contentlet by inode via the database
     * @param inode
     * @return
     */
    Optional<Contentlet> findInDb(String inode);

    /**
     * refresh index by content type
     * @param type
     * @throws DotReindexStateException
     */
    void refresh(ContentType type) throws DotReindexStateException;

}
