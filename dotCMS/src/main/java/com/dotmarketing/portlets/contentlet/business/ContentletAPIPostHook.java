package com.dotmarketing.portlets.contentlet.business;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
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
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Jason Tesser
 * @since 1.6.5c
 * This interface should be used as a post hook for the contentletAPI. The parameters are the same as the contentletAPI 
 * methods except now they also take the return type as the first parameter.
 */
public interface ContentletAPIPostHook {

	/**
	 * @param offset can be 0 if no offset
	 * @param limit can be 0 of no limit
	 * @param returnValue - value returned by primary API Method
	 */
	public default void findAllContent(int offset, int limit, List<Contentlet> returnValue){}
	
	/**
	 * Finds a Contentlet Object given the inode
	 * @param inode
	 * @param returnValue - value returned by primary API Method
	 */
	public default void find(String inode, User user, boolean respectFrontendRoles,Contentlet returnValue){}

	/**
	 * Returns a live Contentlet Object for a given language 
	 * @param languageId
	 * @param contentletId
	 * @param returnValue - value returned by primary API Method
	 */
	public default void findContentletForLanguage(long languageId, Identifier contentletId,Contentlet returnValue){}

	/**
	 * Returns all Contentlets for a specific structure
	 * @param structure
	 * @param user
	 * @param respectFrontendRoles
	 * @param limit
	 * @param offset
	 * @param returnValue - value returned by primary API Method
	 */
	public default void findByStructure(Structure structure, User user, boolean respectFrontendRoles, int limit, int offset,List<Contentlet> returnValue){}
	
	/**
	 * Returns all Contentlets for a specific structure
	 * @param structureInode
	 * @param user
	 * @param respectFrontendRoles
	 * @param limit
	 * @param offset
	 * @param returnValue - value returned by primary API Method
	 */
	public default void findByStructure(String structureInode, User user, boolean respectFrontendRoles, int limit, int offset,List<Contentlet> returnValue){}
	
	/**
	 * Retrieves a contentlet from the database based on its identifier
	 * @param identifier 
	 * @param live Retrieves the live version if false retrieves the working version
	 * @param returnValue - value returned by primary API Method
	 */
	public default void findContentletByIdentifier(String identifier, boolean live, long languageId, User user, boolean respectFrontendRoles,Contentlet returnValue){}

	/**
	 * Retrieves a contentlet from the database based on its identifier
	 * @param identifier
	 */
	public default void findContentletByIdentifierAnyLanguage (String identifier) { }

	/**
	 * Retrieves a contentlet from the database based on its identifier
	 * @param identifier
	 * @param variant
	 */
	default void findContentletByIdentifierAnyLanguage (String identifier, String variant) { }

	/**
	 * Retrieves a contentlet list from the database based on a identifiers array
	 * @param identifiers	Array of identifiers
	 * @param live	Retrieves the live version if false retrieves the working version
	 * @param languageId
	 * @param user	
	 * @param respectFrontendRoles	
	 * @param returnValue - value returned by primary API Method
	 */
	public default void findContentletsByIdentifiers(String[] identifiers, boolean live, long languageId, User user, boolean respectFrontendRoles,List<Contentlet> returnValue){}
		
	/**
	 * Gets a list of Contentlets from a passed in list of inodes.  
	 * @param inodes
	 * @param returnValue - value returned by primary API Method
	 */
	public default void findContentlets(List<String> inodes,List<Contentlet> returnValue){}
	
	/**
	 * Gets a list of Contentlets from a given parent folder  
	 * @param parentFolder
	 * @return
	 */
	public default void findContentletsByFolder(Folder parentFolder, User user, boolean respectFrontendRoles) throws DotDataException{}

	/**
	 * Gets a list of Contentlets from a given parent host  
	 * @param parentHost
	 * @return
	 */
	public default void findContentletsByHost(Host parentHost, User user, boolean respectFrontendRoles) throws DotDataException{}

    /**
     * Gets a list of Contentlets from a given parent host, retrieves the
     * working version of content. The difference between this method and the
     * other one is that the user can specify which content type want to include
     * and exclude. NOTE: If the parameters includingContentTypes and
     * excludingContentTypes are empty if will return all the contentlets.
     * 
     * @param parentHost
     * @param includingContentTypes
     *            this is a list of content types that you would like to include
     *            in the results
     * @param excludingContentTypes
     *            this is a list of content types that you would like to exclude
     *            in the results
     * @param user
     * @param respectFrontendRoles
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public default void findContentletsByHost(Host parentHost, List<Integer> includingContentTypes, List<Integer> excludingContentTypes, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException{}

	/**
	 * Post Hook for {@link ContentletAPI#findContentletsByHostBaseType(Host, List, User, boolean)}
     */
    public default void findContentletsByHostBaseType(Host parentHost, List<Integer> includingContentTypes, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException{}

	/**
	 * Makes a copy of a contentlet. 
	 * @param currentContentlet
	 * @param returnValue - value returned by primary API Method
	 */
	public default void copyContentlet(Contentlet currentContentlet, User user, boolean respectFrontendRoles,Contentlet returnValue){}

	/**
	 * Makes a copy of a contentlet. 
	 * @param currentContentlet
	 * @param returnValue - value returned by primary API Method
	 */
	default void copyContentlet(Contentlet currentContentlet, Host host, User user, boolean respectFrontendRoles,Contentlet returnValue){}

	/**
	 * Copies a contentlet including all its fields. Binary files, Image and File fields are
	 * pointers, and they are preserved as they are. So, if source contentlet points to image A,
	 * the resulting new contentlet will point to the same image A as well. Additionally, this
	 * method copies source permissions and moves the new piece of content to the given folder.
	 *
	 * @param contentletToCopy     The {@link Contentlet} that will be copied.
	 * @param contentType          Optional. The {@link ContentType} that will be used to save the
	 *                             copied Contentlet. This is useful when copying Sites and you
	 *                             choose to copy both Content Types and Contentets.
	 * @param site                 The {@link Host} where the copied Contentlet will be saved.
	 * @param user                 The {@link User} that is performing the action.
	 * @param respectFrontendRoles If the User executing this action has the front-end role, or if
	 *                             front-end roles must be validated against this user, set to
	 *                             {@code true}.
	 */
	default void copyContentlet(final Contentlet contentletToCopy, final ContentType contentType,
								final Host site, final User user,
								final boolean respectFrontendRoles, final Contentlet returnValue) {
	}

	/**
	 * Makes a copy of a contentlet. 
	 * @param currentContentlet
	 * @param returnValue - value returned by primary API Method
	 */
	default void copyContentlet(Contentlet currentContentlet, Folder folder, User user, boolean respectFrontendRoles,Contentlet returnValue){}

	/**
	 * Copies a contentlet including all its fields. Binary files, Image and File fields are
	 * pointers, and they are preserved as they are. So, if source contentlet points to image A,
	 * the resulting new contentlet will point to the same image A as well. Additionally, this
	 * method copies source permissions and moves the new piece of content to the given folder.
	 *
	 * @param contentletToCopy     The {@link Contentlet} that will be copied.
	 * @param contentType          Optional. The {@link ContentType} that will be used to save the
	 *                             copied Contentlet. This is useful when copying Sites and you
	 *                             choose to copy both Content Types and Contentets.
	 * @param folder               The {@link Folder} where the copied Contentlet will be saved.
	 * @param user                 The {@link User} that is performing the action.
	 * @param respectFrontendRoles If the User executing this action has the front-end role, or if
	 *                             front-end roles must be validated against this user, set to
	 *                             {@code true}.
	 */
	default void copyContentlet(final Contentlet contentletToCopy, final ContentType contentType,
								final Folder folder, final User user,
								final boolean respectFrontendRoles, final Contentlet returnValue) {
	}

	/**
	 * Makes a copy of a contentlet with choice to append copy to the filename. 
	 * @param currentContentlet
	 * @param returnValue - value returned by primary API Method
	 */
	default void copyContentlet(Contentlet currentContentlet, Folder folder, User user, boolean appendCopyToFileName, boolean respectFrontendRoles,Contentlet returnValue){}

	default void copyContentlet(final Contentlet currentContentlet, final ContentType contentType,
								final Folder folder, final User user,
								final boolean appendCopyToFileName,
								final boolean respectFrontendRoles, final Contentlet returnValue) {
	}

	/**
	 * Makes a copy of a content.
	 * @param contentletToCopy
	 * @param host
	 * @param folder
	 * @param user
	 * @param copySuffix
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method
	 */
	default void copyContentlet(final Contentlet contentletToCopy,
						final Host site, final Folder folder, final User user, final String copySuffix,
						final boolean respectFrontendRoles, Contentlet returnValue) {}

	/**
	 * Copies a contentlet including all its fields. Binary files, Image and File fields are
	 * pointers, and they are preserved as they are. So, if source contentlet points to image A,
	 * the resulting new contentlet will point to the same image A as well. Additionally, this
	 * method copies source permissions and moves the new piece of content to the given folder. When
	 * copying a File Asset, the value of the {@code opySuffix} parameter will be appended to the
	 * file name.
	 *
	 * @param contentletToCopy     The {@link Contentlet} that will be copied.
	 * @param contentType          Optional. The {@link ContentType} that will be used to save the
	 *                             copied Contentlet. This is useful when copying Sites and you
	 *                             choose to copy both Content Types and Contentets.
	 * @param site                 The {@link Host} where the copied Contentlet will be saved.
	 * @param folder               The {@link Folder} where the copied Contentlet will be saved.
	 * @param user                 The {@link User} that is performing the action.
	 * @param copySuffix           The suffix that will be appended to the file name, if
	 *                             applicable.
	 * @param respectFrontendRoles If the User executing this action has the front-end role, or if
	 *                             front-end roles must be validated against this user, set to
	 *                             {@code true}.
	 */
	default void copyContentlet(final Contentlet contentletToCopy, final ContentType contentType,
								final Host site, final Folder folder, final User user,
								final String copySuffix, final boolean respectFrontendRoles,
								Contentlet returnValue) {
	}

	/**
	 * Searches for content using the given Lucene query.
	 *
	 * @param luceneQuery          The Lucene query string.
	 * @param contentsPerPage      The maximum number of items to return per page.
	 * @param page                 The page number to retrieve.
	 * @param sortBy               The field to sort the results by.
	 * @param user                 The user performing the search.
	 * @param respectFrontendRoles Determines whether to respect frontend roles during the search.
	 * @throws DotDataException     If an error occurs while accessing the data layer.
	 * @throws DotSecurityException If the user does not have permission to perform the search.
	 */
	default void searchPaginatedByPage(String luceneQuery, int contentsPerPage,
			int page, String sortBy, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
	}

	/**
	 * Searches for content using the given Lucene query.
	 *
	 * @param luceneQuery          The Lucene query string.
	 * @param limit                The maximum number of items to return per page.
	 * @param offset               The offset to start retrieving items from.
	 * @param sortBy               The field to sort the results by.
	 * @param user                 The user performing the search.
	 * @param respectFrontendRoles Determines whether to respect frontend roles during the search.
	 * @throws DotDataException     If an error occurs while accessing the data layer.
	 * @throws DotSecurityException If the user does not have permission to perform the search.
	 */
	default void searchPaginated(String luceneQuery, int limit,
			int offset, String sortBy, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
	}

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
	 * @param returnValue - value returned by primary API Method
	 */
	public default void search(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles,List<Contentlet> returnValue){}
	
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
	 * @param returnValue - value returned by primary API Method
	*/
	public default void search(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission,List<Contentlet> returnValue){}

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
	 * @param returnValue - value returned by primary API Method
	 */

	public default void searchIndex(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles,List<ContentletSearch> returnValue){}
	
	/**
	 * Publishes all related HTMLPage
	 * @param contentlet
	 */
	public default void publishRelatedHtmlPages(Contentlet contentlet){}
	
	/**
	 * Will get all the contentlets for a structure and set the default values for a field on the contentlet.  
	 * Will check Write/Edit permissions on the Contentlet. So to guarantee all Contentlets will be cleaned make
	 * sure to pass in an Admin User.  If a user doesn't have permissions to clean all the contentlets it will clean
	 * as many as it can and throw the DotSecurityException  
	 * @param structure
	 * @param field
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default void cleanField(Structure structure, Field field, User user, boolean respectFrontendRoles){}

    /**
     * Will get all the contentlets for a structure (whose modDate is lower than or equals to the deletion date)
     * and set the default values for a field on the contentlet.
     * Will check Write/Edit permissions on the Contentlet. So to guarantee all Contentlets will be cleaned make
     * sure to pass in an Admin User.  If a user doesn't have permissions to clean all the contentlets it will clean
     * as many as it can and throw the DotSecurityException
     * @param structure
     * @param deletionDate
     * @param field
     * @param user
     * @param respectFrontendRoles
     */
    default void cleanField(final Structure structure, final Date deletionDate, final Field field,
            final User user, final boolean respectFrontendRoles) {
    }

	/**
	 * Retrieves all references for a Contentlet. The result is an ArrayList of type Map whose key will 
	 * be page or container with the respective object as the value.  
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void getContentletReferences(Contentlet contentlet, User user, boolean respectFrontendRoles,List<Map<String, Object>> returnValue){}

	/**
	 * This is a simplified version of the more complex {@link #getContentletReferences(Contentlet, User, boolean)}
	 * method. This one will only be focused on querying the database to return the number of Containers that include
	 * the specified Contentlet ID.
	 * <p>The result provided by this method can be used to customize or determine specific behaviors. For example,
	 * this
	 * piece of information is used by the dotCMS UI to ask the User whether they want to edit a Contentlet referenced
	 * everywhere, or if dotCMS should create a copy of such a Contentlet so they can edit that one version.</p>
	 *
	 * @param contentletId The Contentlet ID whose references will be retrieved.
	 *
	 * @return The number of times the specified Contentlet is added to a Container in any HTML Page.
	 */
	default void getContentletReferenceCount(final String contentletId) {

	}

	/**
	 * Gets the value of a field with a given contentlet 
	 * @param contentlet
	 * @param theField
	 * @param returnValue - value returned by primary API Method 
	 */
	default void getFieldValue(Contentlet contentlet, Field theField,Object returnValue){}
	
    /**
     * Gets the value of a field with a given contentlet 
     * @param contentlet
     * @param theField
     * @param returnValue - value returned by primary API Method 
     */
    default void getFieldValue(Contentlet contentlet, com.dotcms.contenttype.model.field.Field theField,Object returnValue){}


	/**
	 * Gets the value of a field with a given contentlet
	 * @param contentlet
	 * @param theField
	 * @param returnValue - value returned by primary API Method
	 * @param user
	 */
	default void getFieldValue(Contentlet contentlet, com.dotcms.contenttype.model.field.Field theField,Object returnValue, User user){}

	/**
	 * Adds a relationship to a contentlet
	 * @param contentlet
	 * @param linkInode
	 * @param relationName
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default void addLinkToContentlet(Contentlet contentlet, String linkInode, String relationName, User user, boolean respectFrontendRoles){}
	
	/**
	 * Adds a relationship to a contentlet
	 * @param contentlet
	 * @param fileInode
	 * @param relationName
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default void addFileToContentlet(Contentlet contentlet, String fileInode, String relationName, User user, boolean respectFrontendRoles){}
	
	/**
	 * Adds a relationship to a contentlet
	 * @param contentlet
	 * @param imageInode
	 * @param relationName
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default void addImageToContentlet(Contentlet contentlet, String imageInode, String relationName, User user, boolean respectFrontendRoles){}

	/**
	 * Returns the contentlets on a given page.  Will only return contentlets the user has permission to read/use
	 * You can pass -1 for languageId if you don't want to query to pull based
	 * on languages or 0 if you want to get the default language
	 * @param HTMLPageIdentifier
	 * @param containerIdentifier
	 * @param orderby
	 * @param working
	 * @param languageId
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void findPageContentlets(String HTMLPageIdentifier, String containerIdentifier, String orderby, boolean working, long languageId, User user, boolean respectFrontendRoles,List<Contentlet> returnValue){}

	/**
	 * Returns all contentlet's relationships for a given contentlet inode 
	 * @param contentletInode a ContentletRelationships object containing all relationships for the contentlet
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void getAllRelationships(String contentletInode, User user, boolean respectFrontendRoles,ContentletRelationships returnValue){}

	/**
	 * Returns all contentlet's relationships for a given contentlet object 
	 * @param contentlet a ContentletRelationships object containing all relationships for the contentlet
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void getAllRelationships(Contentlet contentlet,ContentletRelationships returnValue){}

	/**
	 * Returns a contentlet's siblings for a given contentlet object.
	 * @param contentlet
	 * a ContentletRelationships object containing all relationships for the contentlet
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void getAllLanguages(Contentlet contentlet, Boolean isLiveContent, User user, boolean respectFrontendRoles,List<Contentlet> returnValue){}

	/**
	 * 
	 * @param contentlet1
	 * @param contentlet2
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void isContentEqual(Contentlet contentlet1,Contentlet contentlet2, User user, boolean respectFrontendRoles,boolean returnValue){}

	/**
	 * This method archives the given contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 */ 
	public default void archive(Contentlet contentlet, User user, boolean respectFrontendRoles){}

	/**
	 * This method completely deletes the given contentlet from the system
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default void delete(Contentlet contentlet, User user, boolean respectFrontendRoles){}
	
	/**
	 * This method completely deletes the given contentlet from the system. It was added for the jira issue
	 * http://jira.dotmarketing.net/browse/DOTCMS-2059
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @param allVersions
	 */
	public default void delete(Contentlet contentlet, User user, boolean respectFrontendRoles, boolean allVersions){}

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
	public default boolean destroy(Contentlet contentlet, User user, boolean respectFrontendRoles){
      return true;
    }
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
	public default boolean destroy(List<Contentlet> contentlets, User user, boolean respectFrontendRoles){
	  return true;
	}
	
	/**
	 * Publishes a piece of content. 
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default void publish(Contentlet contentlet, User user, boolean respectFrontendRoles){}
	
	/**
	 * Publishes a piece of content. 
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default void publish(List<Contentlet> contentlets, User user, boolean respectFrontendRoles){}

	/**
	 * This method unpublishes the given contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default void unpublish(Contentlet contentlet, User user, boolean respectFrontendRoles){}
	
	/**
	 * This method unpublishes the given contentlet
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default void unpublish(List<Contentlet> contentlets, User user, boolean respectFrontendRoles){}

	/**
	 * This method archives the given contentlets
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default void archive(List<Contentlet> contentlets, User user, boolean respectFrontendRoles){}
	/**
	 * This method unarchives the given contentlets
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default void unarchive(List<Contentlet> contentlets, User user, boolean respectFrontendRoles){}

	/**
	 * This method unarchives the given contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default void unarchive(Contentlet contentlet, User user, boolean respectFrontendRoles){}
	
	/**
	 * This method completely deletes the given contentlet from the system
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default void delete(List<Contentlet> contentlets, User user, boolean respectFrontendRoles){}

    /**
     * This method completely deletes contentlets from a given host
     * @param host
     * @param user
     * @param respectFrontendRoles
     */
    public default void deleteByHost(Host host, User user, boolean respectFrontendRoles){}

	/**
	 * This method completely deletes the given contentlet from the system. It was added for the jira issue
	 * http://jira.dotmarketing.net/browse/DOTCMS-2059
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 * @param allVersions
	 */
	public default void delete(List<Contentlet> contentlets, User user, boolean respectFrontendRoles, boolean allVersions){}
	
	/**
	 * Deletes all related content from passed in contentlet and relationship 
	 * @param contentlet
	 * @param relationship
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default void deleteRelatedContent(Contentlet contentlet, Relationship relationship, User user, boolean respectFrontendRoles){}
	
	/**
	 * Deletes all related content from passed in contentlet and relationship 
	 * @param contentlet
	 * @param relationship
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default void deleteRelatedContent(Contentlet contentlet, Relationship relationship, boolean hasParent, User user, boolean respectFrontendRoles){}

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
    public default void deleteRelatedContent(final Contentlet contentlet, final Relationship relationship,
            final boolean hasParent, final User user, final boolean respectFrontendRoles,
            final List<Contentlet> contentletsToBeRelated) {
    }
	
	/**
	 * Associates the given list of contentlets using the relationship this
	 * methods removes old associated content and reset the relatioships based
	 * on the list of content passed as parameter
	 * @param contentlet
	 * @param rel
	 * @param related
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default void relateContent(Contentlet contentlet, Relationship rel,List<Contentlet> related, User user, boolean respectFrontendRoles){}

	/**
	 * Associates the given list of contentlets using the relationship this
	 * methods removes old associated content and reset the relatioships based
	 * on the list of content passed as parameter
	 * @param contentlet
	 * @param related
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default void relateContent(Contentlet contentlet, ContentletRelationshipRecords related, User user, boolean respectFrontendRoles){}

	/**
	 * Gets all related content, if this method is invoked with a same structures (where the parent and child structures are the same type) 
	 * kind of relationship then all parents and children of the given contentlet will be retrieved in the same returned list
	 * @param contentlet
	 * @param rel
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method */
	public default void getRelatedContent(Contentlet contentlet, Relationship rel, User user, boolean respectFrontendRoles,List<Contentlet> returnValue){}

	/**
     * Gets all related content from the same structure (where the parent and child structures are the same type)
     * The parameter pullByParent if set to true tells the method to pull all children where the passed
     * contentlet is the parent, if set to false then the passed contentlet is the child and you want to pull
     * parents
     *
     * If this method is invoked using different structures, then the parameter
     * pullByParent will be ignored, and the side of the relationship will be figured out automatically
     *
     * @param contentlet
     * @param rel
     * @param pullByParent
     * @param user
     * @param respectFrontendRoles
     * @param returnValue - value returned by primary API Method */
    public default void getRelatedContent(Contentlet contentlet, Relationship rel, Boolean pullByParent, User user, boolean respectFrontendRoles,List<Contentlet> returnValue){}

    /**
     * Gets all related content from the same structure (where the parent and child structures are the same type)
     * The parameter pullByParent if set to true tells the method to pull all children where the passed
     * contentlet is the parent, if set to false then the passed contentlet is the child and you want to pull
     * parents
     *
     * If this method is invoked using different structures, then the parameter
     * pullByParent will be ignored, and the side of the relationship will be figured out automatically
     *
     * This method uses pagination if necessary (limit, offset, sortBy)
     *
     * @param contentlet
     * @param rel
     * @param pullByParent
     * @param user
     * @param respectFrontendRoles
     * @param returnValue
     * @param limit
     * @param offset
     * @param sortBy
     */
    default void getRelatedContent(Contentlet contentlet, Relationship rel, Boolean pullByParent, User user, boolean respectFrontendRoles, List<Contentlet> returnValue, int limit, int offset,
            String sortBy){}


	/**
	 * 
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default void unlock(Contentlet contentlet, User user, boolean respectFrontendRoles){}

	/**
	 * Use to lock a contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default void lock(Contentlet contentlet, User user, boolean respectFrontendRoles){}
	
	/**
	 * Reindex all content
	 */
	public default void reindex(){}
	
	/**
	 * reindex content for a given structure
	 * @param structure
	 */
	public default void reindex(Structure structure){}
	
	/**
	 * reindex a single content
	 * @param contentlet
	 */
	public default void reindex(Contentlet contentlet){}
	
	/**
	 * Used to reindex content for the specific server the code executes on at runtime in a cluster
	 */
	public default void reIndexForServerNode(){} 

	/**
	 * Gets a file with a specific relationship type to the passed in contentlet
	 * @param contentlet
	 * @param relationshipType
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void getRelatedIdentifier(Contentlet contentlet, String relationshipType, User user, boolean respectFrontendRoles,Identifier returnValue){}
	
	/**
	 * Gets all related links to the contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void getRelatedLinks(Contentlet contentlet, User user, boolean respectFrontendRoles,List<Link> returnValue){}
	
	/**
	 * Allows you to checkout a content so it can be altered and checked in
	 * @param contentletInode
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void checkout(String contentletInode, User user, boolean respectFrontendRoles,Contentlet returnValue){}
	
	/**
	 * Allows you to checkout contents so it can be altered and checked in 
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void checkout(List<Contentlet> contentlets, User user, boolean respectFrontendRoles,List<Contentlet> returnValue){}
	
	/**
	 * Allows you to checkout contents based on a lucene query so it can be altered and checked in 
	 * @param luceneQuery
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void checkout(String luceneQuery, User user, boolean respectFrontendRoles, List<Contentlet> returnValue){}
	
	/**
	 * Allows you to checkout contents based on a lucene query so it can be altered and checked in, in a paginated fashion 
	 * @param luceneQuery
	 * @param user
	 * @param respectFrontendRoles
	 * @param offset
	 * @param limit
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void checkout(String luceneQuery, User user, boolean respectFrontendRoles, int offset, int limit,List<Contentlet> returnValue){}
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.  
	 * @param contentlet - The inode of your contentlet must be 0.
	 * @param contentRelationships - throws IllegalArgumentException if null. Used to set relationships to new contentlet version 
	 * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 * @param permissions - throws IllegalArgumentException if null. Used to set permissions to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles,Contentlet returnValue){}
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.  
	 * This version of checkin contains a more complex structure to pass the relationship in order
	 * to handle a same structures (where the parent and child structures are the same) kind of relationships
	 * in that case you have to specify if the role of the content is the parent of the child of the relatioship
	 * @param currentContentlet - The inode of your contentlet must be 0.
	 * @param relationshipsData - 
	 * @param cats
	 * @param selectedPermissions
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void checkin(Contentlet currentContentlet, ContentletRelationships relationshipsData, List<Category> cats, List<Permission> selectedPermissions, User user,	boolean respectFrontendRoles,Contentlet returnValue){}

	public default void checkin(Contentlet contentlet, ContentletDependencies contentletDependencies, Contentlet c) {}

	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.  
	 * @param contentlet - The inode of your contentlet must be 0. 
	 * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 * @param permissions - throws IllegalArgumentException if null. Used to set permissions to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void checkin(Contentlet contentlet, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles,Contentlet returnValue){}
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.  
	 * @param contentlet - The inode of your contentlet must be 0. 
	 * @param permissions - throws IllegalArgumentException if null. Used to set permissions to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void checkin(Contentlet contentlet, List<Permission> permissions, User user,boolean respectFrontendRoles,Contentlet returnValue){}
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.  
	 * @param contentlet - The inode of your contentlet must be 0.
	 * @param user
	 * @param respectFrontendRoles
     * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void checkin(Contentlet contentlet ,User user,boolean respectFrontendRoles,List<Category> cats,Contentlet returnValue){}
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.  
	 * @param contentlet - The inode of your contentlet must be 0.
	 * @param contentRelationships - throws IllegalArgumentException if null. Used to set relationships to new contentlet version 
	 * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats, User user,boolean respectFrontendRoles,Contentlet returnValue){}
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.  
	 * @param contentlet - The inode of your contentlet must be 0.
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void checkin(Contentlet contentlet, User user,boolean respectFrontendRoles, Contentlet returnValue){}
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.  
	 * @param contentlet - The inode of your contentlet must be 0.
	 * @param contentRelationships - throws IllegalArgumentException if null. Used to set relationships to new contentlet version 
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, User user,boolean respectFrontendRoles,Contentlet returnValue){}
	
	/**
	 * @deprecated {@link ContentletAPIPostHook#checkinWithoutVersioning(Contentlet, ContentletRelationships, List, List, User, boolean, Contentlet)} instead
     * Will check in a update of your contentlet without generate a new version. The inode of your contentlet must be different from 0.
	 * @param contentlet - The inode of your contentlet must be different from 0.
	 * @param contentRelationships - Used to set relationships to updated contentlet version 
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void checkinWithoutVersioning(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles,Contentlet returnValue){}

    /**
     * Will check in a update of your contentlet without generate a new version. The inode of your
     * contentlet must be different from 0.
     *
     * @param contentlet - The inode of your contentlet must be different from 0.
     * @param contentRelationships - Used to set relationships to updated contentlet version
     * @param returnValue - value returned by primary API Method
     */
    default void checkinWithoutVersioning(Contentlet contentlet,
            ContentletRelationships contentRelationships, List<Category> cats,
            List<Permission> permissions, User user, boolean respectFrontendRoles,
            Contentlet returnValue) {
    }


    /**
	 * Will make the passed in contentlet the working copy. 
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles 
	 */
	public default void restoreVersion(Contentlet contentlet, User user, boolean respectFrontendRoles){}
	
	/**
	 * Retrieves all versions for a contentlet identifier
	 * Note this method should not be used currently because it could pull too many versions. 
	 * @param identifier
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void findAllVersions(Identifier identifier, User user, boolean respectFrontendRoles,List<Contentlet> returnValue){}

	/**
	 * Retrieves all versions for a contentlet identifier
	 * Note this method should not be used currently because it could pull too many versions.
	 * @param identifier - Identifier object that belongs to a contentlet
	 * @param bringOldVersions - boolean value which determines if old versions (non-live, non-working
	 * 	should be brought here). @see copyContentlet method, which requires passing in only live/working
	 * 	versions of contents to be copied
	 * @param user - User in context who has triggered this call.
	 * @param respectFrontendRoles - For permissions validations
	 * @param returnValue - value returned by primary API Method
	 */
	public default void findAllVersions(Identifier identifier, boolean bringOldVersions, User user, boolean respectFrontendRoles,List<Contentlet> returnValue){}

	public default void findAllVersions(final Identifier identifier, final Variant variant,
			final User user, boolean respectFrontendRoles){

	}

	/**
	 * Retrieves all versions for a contentlet identifier
	 * @param identifiers
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default void findLiveOrWorkingVersions(Set<String> identifiers, User user, boolean respectFrontendRoles){

	}

	/**
	 * Retrieves all versions for a contentlet identifier checked in by a real user meaning not the system user
	 * @param identifier
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void findAllUserVersions(Identifier identifier, User user, boolean respectFrontendRoles,List<Contentlet> returnValue){}
	
	/**
	 * Meant to get the title or name of a contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void getName(Contentlet contentlet, User user, boolean respectFrontendRoles,String returnValue){}
	
	/**
	 * Copies properties from the map to the contentlet
	 * @param contentlet contentlet to copy to
	 * @param properties
	 */
	public default void copyProperties(Contentlet contentlet, Map<String, Object> properties){}
	
	/**
	 * Use to check if the inode id is a contentlet
	 * @param inode id to check
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void isContentlet(String inode,boolean returnValue){}
	
	/**
	 * Will return all content assigned to a specified Category
	 * @param category Category to look for
	 * @param languageId language to pull content for. If 0 will return all languages
	 * @param live should return live or working content
	 * @param orderBy indexName(previously known as dbColumnName) to order by. Can be null or empty string
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void find(Category category, long languageId, boolean live, String orderBy, User user, boolean respectFrontendRoles,List<Contentlet> returnValue){}

	/**
	 * Will return all content assigned to a specified Categories
	 * @param categories - List of categories to look for
	 * @param languageId language to pull content for. If 0 will return all languages
	 * @param live should return live or working content
	 * @param orderBy indexName(previously known as dbColumnName) to order by. Can be null or empty string
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void find(List<Category> categories,long languageId, boolean live, String orderBy, User user, boolean respectFrontendRoles,List<Contentlet> returnValue){}

	/**
	 * Use to set contentlet properties.  The value should be String, the proper type of the property
	 * @param contentlet
	 * @param field
	 * @param value
	 */
	public default void setContentletProperty(Contentlet contentlet, Field field, Object value){}
	
	/**
	 * Use to validate your contentlet.
	 * @param contentlet
	 * @param cats - categories
	 */
	public default void validateContentlet(Contentlet contentlet,List<Category> cats){} 
	
	/**
	 * Use to validate your contentlet.
	 * @param contentlet
	 * @param contentRelationships
	 * @param cats - categories
	 * Use the notValidFields property of the exception to get which fields where not valid
	 */
	public default void validateContentlet(Contentlet contentlet,Map<Relationship, List<Contentlet>> contentRelationships,List<Category> cats){} 
	
	/**
	 * Use to validate your contentlet.
	 * @param contentlet
	 * @param contentRelationships
	 * @param cats - categories
	 */
	public default void validateContentlet(Contentlet contentlet, ContentletRelationships contentRelationships, List<Category> cats){}

	/**
	 * Use to validate your contentlet.
	 * @param contentlet
	 * @param cats - categories
	 */
	default void validateContentletNoRels(Contentlet contentlet, List<Category> cats){}


	/**
	 * Use to determine if if the field value is a String value withing the contentlet object
	 * @param field
	 */
	public default void isFieldTypeString(Field field,boolean returnValue){}
	
	/**
	 * Use to determine if if the field value is a Date value withing the contentlet object
	 * @param field
	 */
	public default void isFieldTypeDate(Field field,boolean returnValue){}
	
	/**
	 * Use to determine if if the field value is a Long value withing the contentlet object
	 * @param field
	 */
	public default void isFieldTypeLong(Field field,boolean returnValue){}
	
	/**
	 * Use to determine if if the field value is a Boolean value withing the contentlet object
	 * @param field
	 */
	public default void isFieldTypeBoolean(Field field,boolean returnValue){}
	
	/**
	 * Use to determine if if the field value is a Float value withing the contentlet object
	 * @param field
	 */
	public default void isFieldTypeFloat(Field field,boolean returnValue){}

	/**
	 * Applies permission to the child contentlets of the structure
	 * @param structure
	 * @param user
	 * @param permissions
	 * @param respectFrontendRoles
	 */
	public default void applyStructurePermissionsToChildren(Structure structure, User user, List<Permission> permissions, boolean respectFrontendRoles){}
	
   /**
    * 
    * @param deleteFrom
    * @return
    * @param returnValue - value returned by primary API Method */
	public default void deleteOldContent(Date deleteFrom,int returnValue){}

    /**
     *
     * @param structureInode
     * @param field
     * @param user
     * @param respectFrontEndRoles
     * @param returnValue  - value returned by primary API Method
     */
	public default void findFieldValues(String structureInode, Field field, User user, boolean respectFrontEndRoles,List<String> returnValue){}
	
	/**
	 * Fetches the File Name stored under the contentlet and field
	 * @param contentletInode
	 * @param velocityVariableName
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void getBinaryFile(String contentletInode,String velocityVariableName,User user,java.io.File returnValue){}
	
	/**
	 * gets the number of contentlets in the system. This number includes all versions not distinct identifiers
	 * @param returnValue
	 * @return
	 * @throws DotDataException
	 */
	public default long contentletCount(long returnValue) throws DotDataException{
	  return 0;
	}

	/**
	 * gets the number of contentlet identifiers in the system. This number includes all versions not distinct identifiers
	 * @param returnValue
	 * @return
	 * @throws DotDataException
	 */
	public default long contentletIdentifierCount(long returnValue) throws DotDataException{
      return 0;
    }

	/**
	 * 
	 * @param contentletInodeOrIdentifier
	 * @return
	 * @throws DotDataException
	 */
	public default boolean removeContentletFromIndex(String contentletInodeOrIdentifier) throws DotDataException{
	  return true;
	}

	/**
	 * 
	 * @param structure
	 */
	public default void refresh(Structure structure){}

    /**
     * 
     * @param type
     */
    public default void refresh(ContentType type){}

	
	/**
	 * 
	 * @param contentlet
	 */
	public default void refresh(Contentlet contentlet){}

	/**
	 * 
	 */
	public default void refreshAllContent(){}

	/**
	 * 
	 * @param identifier
	 * @return
	 * @throws DotDataException
	 */
	public default List<Contentlet> getSiblings(String identifier)throws DotDataException {
	  return ImmutableList.of();
	}

	/**
	 * 
	 * @param contentlet
	 * @param contentRelationships
	 * @param cats
	 * @param permissions
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue
	 */
	public default void checkinWithNoIndex(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles,Contentlet returnValue){}
	
	/**
	 * Will check in a new version of you contentlet without indexing. The inode of your contentlet must be not set.  
	 * This version of checkinWithNoIndex contains a more complex structure to pass the relationship in order
	 * to handle a same structures (where the parent and child structures are the same) kind of relationships
	 * in that case you have to specify if the role of the content is the parent of the child of the relatioship
	 * @param currentContentlet - The inode of your contentlet must be not set.
	 * @param relationshipsData - 
	 * @param cats
	 * @param selectedPermissions
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void checkinWithNoIndex(Contentlet currentContentlet, ContentletRelationships relationshipsData, List<Category> cats, List<Permission> selectedPermissions, User user,	boolean respectFrontendRoles,Contentlet returnValue){}
	
	/**
	 * Will check in a new version of you contentlet without indexing. The inode of your contentlet must be not set.   
	 * @param contentlet - The inode of your contentlet must be not set. 
	 * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 * @param permissions - throws IllegalArgumentException if null. Used to set permissions to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void checkinWithNoIndex(Contentlet contentlet, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles,Contentlet returnValue){}
	
	/**
	 * Will check in a new version of you contentlet without indexing. The inode of your contentlet must be not set.  
	 * @param contentlet - The inode of your contentlet must be not set. 
	 * @param permissions - throws IllegalArgumentException if null. Used to set permissions to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void checkinWithNoIndex(Contentlet contentlet, List<Permission> permissions, User user,boolean respectFrontendRoles,Contentlet returnValue){}
	
	/**
	 * Will check in a new version of you contentlet without indexing. The inode of your contentlet must be not set.  
	 * @param contentlet - The inode of your contentlet must be not set.
	 * @param user
	 * @param respectFrontendRoles
     * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
     * @param returnValue - value returned by primary API Method
	 */
	public default void checkinWithNoIndex(Contentlet contentlet ,User user,boolean respectFrontendRoles,List<Category> cats,Contentlet returnValue){}
	
	/**
	 * Will check in a new version of you contentlet without indexing. The inode of your contentlet must be not set.  
	 * @param contentlet - The inode of your contentlet must be not set.
	 * @param contentRelationships - throws IllegalArgumentException if null. Used to set relationships to new contentlet version 
	 * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void checkinWithNoIndex(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats, User user,boolean respectFrontendRoles,Contentlet returnValue){}
	
	/**
	 * Will check in a new version of you contentlet without indexing. The inode of your contentlet must be not set.  
	 * @param contentlet - The inode of your contentlet must be not set.
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void checkinWithNoIndex(Contentlet contentlet, User user,boolean respectFrontendRoles, Contentlet returnValue){}
	
	/**
	 * Will check in a new version of you contentlet without indexing. The inode of your contentlet must be not set.   
	 * @param contentlet - The inode of your contentlet must be not set.
	 * @param contentRelationships - throws IllegalArgumentException if null. Used to set relationships to new contentlet version 
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public default void checkinWithNoIndex(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, User user,boolean respectFrontendRoles,Contentlet returnValue){}
	
	/**
	 * 
	 * @param query
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws ValidationException
	 * @throws DotDataException
	 */
	public default void DBSearch(Query query, User user,boolean respectFrontendRoles, List<Map<String, Serializable>> returnValue) throws ValidationException,DotDataException{}
	
	/**
	 * Method will time out after 30 seconds returning false
	 * @param inode
	 * @return
	 */
	public default void isInodeIndexed(String inode, boolean returnValue){}


	/**
	 * Method will time out after 30 seconds returning false
	 * @param inode
	 * @return
	 */
	public default boolean isInodeIndexedArchived(String inode) {
		return true;
	};

	default boolean isInodeIndexedArchived(String inode, int secondsToWait) {
		return true;
	}

	/**
	 * 
	 * @param inode
	 * @param live
	 * @param returnValue
	 */
	public default void isInodeIndexed(String inode, boolean live, boolean returnValue){}

	public default void isInodeIndexed(String inode, boolean live, int secondsToWait, boolean returnValue) {}

	public default void isInodeIndexed(String inode, boolean live, boolean working, boolean returnValue) {}

	/**
	 * Method will time out after 30 seconds returning false
	 * @param inode
	 * @param secondsToWait - how long to wait before timing out
	 * @return
	 */
	public default void isInodeIndexed(String inode, int secondsToWait, boolean returnValue){}
	
	/**
	 * Method will update hostInode of content to systemhost
	 * @param hostIdentifier
	 */	
	public default void UpdateContentWithSystemHost(String hostIdentifier)throws DotDataException{}
	
	/**
	 * Method will remove User References of the given userId in Contentlet  
	 * @param userId
	 */	
	public default void removeUserReferences(String userId)throws DotDataException{}
	
	/**
	 * Method will replace user references of the given userId in Contentlet s
	 * and replaced with the specified replacement userId 
	 * @param userToReplace the user to replace
	 * @param replacementUserId Replacement User Id
	 * @param user the user requesting the operation
	 */
	public default void updateUserReferences(User userToReplace,String replacementUserId, User user)throws DotDataException{}

	/**
	 * Return the URL Map for the specified content if the structure associated to the content has the URL Map Pattern set.
	 * 
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public default void getUrlMapForContentlet(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException{}
	
	/**
	 * Deletes the given version of the contentlet from the system
	 * 
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default void deleteVersion(Contentlet contentlet, User user,	boolean respectFrontendRoles) throws DotDataException,DotSecurityException{}
	
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
	public default void  saveDraft(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) throws IllegalArgumentException,DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException{}

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
	public default void  saveDraft(Contentlet contentlet, ContentletRelationships contentletRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) throws IllegalArgumentException,DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException{}


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
	public default void searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException{}
	
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
	public default void searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission) throws DotDataException, DotSecurityException{}

	/**
	 * The search here takes a lucene query and pulls Contentlets for you, using the identifier of the contentlet.You can pass sortBy as null if you do not 
	 * have a field to sort by.  limit should be 0 if no limit and the offset should be -1 is you are not paginating.
	 * The returned list will be filtered with only the contentlets that match the required permission.  You can of course also
	 * pass permissions to further limit in the lucene query itself
	 * Searches default langugae if anyLanguage is false, and searches all languages if anyLanguage is true.
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
	public default void searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission, boolean anyLanguage) throws DotDataException, DotSecurityException{}
	
	/**
	 * Reindexes content under a given host + refreshes the content from cache
	 * @param host
	 * @throws DotReindexStateException
	 */
	public default void refreshContentUnderHost(Host host)throws DotReindexStateException{}
	
	/**
	 * Reindexes content under a given folder + refreshes the content from cache
	 * @param folder
	 * @throws DotReindexStateException
	 */
	public default void refreshContentUnderFolder(Folder folder) throws DotReindexStateException{}

	/**
	 * Reindexes content under a given folder path
	 *
	 * @param hostId
	 * @param folderPath
	 * @throws DotReindexStateException
	 */
	public default void refreshContentUnderFolderPath ( String hostId, String folderPath ) throws DotReindexStateException{}
	
	/**
	 * Will update contents that reference the given folder to point to it's parent folder, if it's a top folder it will set folder to be SYSTEM_FOLDER
	 * @param folder
	 * @throws DotDataException
	 */
	public default void removeFolderReferences(Folder folder) throws DotDataException{}
	
	/**
	 * Tests whether a user can potentially lock a piece of content (needed to test before publish, etc).  This method will return false if content is already locked
	 * by another user.
	 * @param contentlet
	 * @param user
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 *
	 */
	public default boolean canLock(Contentlet contentlet, User user) throws   DotLockException{
	  return true;
	}

	/**
	 * 
	 * @param luceneQuery
	 * @param user
	 * @param respectFrontendRoles
	 * @param c
	 */
    public default void searchIndexCount(String luceneQuery, User user, boolean respectFrontendRoles, long c){}
    
	/**
	 * Returns the ContentRelationships Map for the specified content.
	 * 
	 * @param contentlet
	 * @param user
	 * @return Map with the ContentRelationships. Empty Map if the content doesn't have associated relationships.
	 */
	public default void findContentRelationships(Contentlet contentlet, User user) throws DotDataException, DotSecurityException{}

	/**
	 * 
	 * @param inode
	 * @param field
	 * @param value
	 */
    public default void loadField(String inode, Field field, Object value){}


	/**
	 * loadField Post Hook
	 * @param inode
	 * @param field
	 * @return
	 * @throws DotDataException
	 */
	public default void loadField(String inode, com.dotcms.contenttype.model.field.Field field, Object value) throws DotDataException{}

    /**
     * 
     * @param luceneQuery
     * @param user
     * @param respectFrontendRoles
     * @param value
     */
    public default void indexCount(String luceneQuery, User user,
            boolean respectFrontendRoles, long value){}

    /**
     * Gets the top viewed content for a particular structure for a specified date interval
     * 
     * @param structureVariableName
     * @param startDate
     * @param endDate
     * @param user
     * @return
     */
	public default boolean getMostViewedContent(String structureVariableName, String startDate, String endDate, User user){
	  return true;
	}

	/**
	 * 
	 * @param contentlet
	 * @param isNew
	 * @param isNewVersion
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotContentletStateException
	 * @throws DotStateException
	 */
    public default void publishAssociated(Contentlet contentlet, boolean isNew, boolean isNewVersion) throws DotSecurityException, DotDataException, DotContentletStateException, DotStateException{}

    /**
     * 
     * @param contentlet
     * @param isNew
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws DotContentletStateException
     * @throws DotStateException
     */
    public default void publishAssociated(Contentlet contentlet, boolean isNew) throws DotSecurityException, DotDataException, DotContentletStateException, DotStateException{}

    /**
     * 
     * @param esQuery
     * @param live
     * @param user
     * @param respectFrontendRoles
     * @throws DotSecurityException
     * @throws DotDataException
     */
    public default void esSearchRaw(String esQuery, boolean live, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException{}

    /**
     * 
     * @param esQuery
     * @param live
     * @param user
     * @param respectFrontendRoles
     * @throws DotSecurityException
     * @throws DotDataException
     */
	public default void esSearch(String esQuery, boolean live, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException{}

	/**
	 * 
	 * @param buffy
	 * @param user
	 * @param roles
	 * @param respectFrontendRoles
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public default void addPermissionsToQuery ( StringBuffer buffy, User user, List<Role> roles, boolean respectFrontendRoles ) throws DotSecurityException, DotDataException{}

	/**
	 *
	 * @param inodes
	 * @throws DotDataException
	 */
	public default void updateModDate(final Set<String> inodes, final User user) throws DotDataException {}

    public default void findContentletByIdentifierOrFallback(String identifier, boolean live, long incomingLangId, User user, boolean respectFrontendRoles) {}

    public default void findInDb(String inode) {};

    /**
     * @deprecated This method should not be exposed. Use ContentletAPI.getRelated variations instead
     * @param contentlet
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
    default boolean  filterRelatedContent(Contentlet contentlet, Relationship rel,
            User user, boolean respectFrontendRoles, Boolean pullByParent, int limit, int offset,
            String sortBy)
            throws DotDataException, DotSecurityException{
        return true;
    }

    default void getRelatedContent(Contentlet contentlet, String variableName, User user,
            boolean respectFrontendRoles, Boolean pullByParents, int limit, int offset,
            String sortBy){

    }

    default void getRelatedContent(Contentlet contentlet, String variableName, User user, boolean respectFrontendRoles, Boolean pullByParents, int limit, int offset, String sortBy,
            long language, Boolean live){

    }

    default void getRelatedContent(Contentlet contentlet, Relationship rel, Boolean pullByParent, User user, boolean respectFrontendRoles, int limit, int offset, String sortBy,
            long language, Boolean live){

    }

    default void getRelatedContent(Contentlet contentlet, Relationship rel, Boolean pullByParent, User user, boolean respectFrontendRoles, long language, Boolean live){

    }

    default void invalidateRelatedContentCache(Contentlet contentlet, Relationship relationship, boolean hasParent){

    }

    default void findContentletByIdentifierAnyLanguage(String identifier, boolean includeDeleted){

    }

	/**
	 * Post Hook Move
	 * @param contentlet
	 * @param user
	 * @param hostAndFolderPath
	 * @param respectFrontendRoles
	 */
    default void move(Contentlet contentlet, User user, String hostAndFolderPath, boolean respectFrontendRoles) {}

	/**
	 * Post Hook Move
	 * @param contentlet
	 * @param user
	 * @param host
	 * @param folderFolderPath
	 * @param respectFrontendRoles
	 */
	default void move(Contentlet contentlet, User user, Host host, String folderFolderPath, boolean respectFrontendRoles) {}

	/**
	 * Post Hook Move
	 * @param contentlet
	 * @param user
	 * @param host
	 * @param folder
	 * @param respectFrontendRoles
	 */
	default void move(final Contentlet contentlet, User user, Host host, Folder folder, boolean respectFrontendRoles) {}

	default void getAllContentByVariants(User user, boolean respectFrontendRoles, String[] variantNames) {

	}

    default void saveContentOnVariant(Contentlet contentlet, String variantName, User user){

	}
}
