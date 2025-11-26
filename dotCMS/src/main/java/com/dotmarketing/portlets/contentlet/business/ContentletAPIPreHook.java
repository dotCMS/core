package com.dotmarketing.portlets.contentlet.business;

import com.dotcms.content.elasticsearch.business.SearchCriteria;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Jason Tesser
 * @since 1.6.5c
 * This interface should be used as a pre hook for the contentletAPI.  If the hooks 
 * return false then the method will throw an exception up the stack. Stopping the progress.
 * When possible you should always return true and let the methods go about their business.
 */
public interface ContentletAPIPreHook {

	/**
	 * @param offset can be 0 if no offset
	 * @param limit can be 0 of no limit
	 * @return false if the hook should stop the transaction
	 */
	public default boolean findAllContent(int offset, int limit){
      return true;
    }
	
	/**
	 * Finds a Contentlet Object given the inode
	 * @param inode
	 * @return false if the hook should stop the transaction
	 */
	public default boolean find(String inode, User user, boolean respectFrontendRoles){
      return true;
    }
	
	/**
	 * Returns a live Contentlet Object for a given language 
	 * @param languageId
	 * @param contentletId
	 * @return
	 */
	public default boolean findContentletForLanguage(long languageId, Identifier contentletId){
      return true;
    }


	/**
	 * Returns all Contentlets for a specific structure
	 * @param structure
	 * @param user
	 * @param respectFrontendRoles
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public default boolean findByStructure(Structure structure, User user, boolean respectFrontendRoles, int limit, int offset){
      return true;
    }
	
	/**
	 * Returns all Contentlets for a specific structure
	 * @param structureInode
	 * @param user
	 * @param respectFrontendRoles
	 * @param limit
	 * @param offset
	 * @return
	 */
	public default boolean findByStructure(String structureInode, User user, boolean respectFrontendRoles, int limit, int offset){
      return true;
    }
	
	/**
	 * Retrieves a contentlet from the database based on its identifier
	 * @param identifier
	 * @param live Retrieves the live version if false retrieves the working version
	 * @return
	 */
	public default boolean findContentletByIdentifier(String identifier, boolean live, long languageId, User user, boolean respectFrontendRoles){
      return true;
    }

	/**
	 * Retrieves a contentlet from the database based on its identifier
	 * @param identifier
	 * @param languageId
	 * @param variantId
	 * @param user
	 * @param timeMachineDate
	 * @param respectFrontendRoles
	 * @return
	 */
	public default boolean findContentletByIdentifier(String identifier, long languageId, String variantId, User user, Date timeMachineDate, boolean respectFrontendRoles){
		return true;
	}

	/**
	 * Retrieves a contentlet from the database based on its identifier
	 * @param identifier
	 * @return
	 */
	public default boolean findContentletByIdentifierAnyLanguage (String identifier) {
		return true;
	}

	/**
	 * Retrieves a contentlet from the database based on its identifier
	 * @param identifier
	 * @param variant
	 * @return
	 */
	default boolean findContentletByIdentifierAnyLanguage(final String identifier, final String variant,
			final boolean includeDeleted) throws DotDataException{
		return true;
	}

	/**
	 * Retrieves a contentlet from the database based on its identifier
	 * @param identifier
	 * @param variant
	 * @return
	 */
	default boolean findContentletByIdentifierAnyLanguage (String identifier, String variant) {
		return true;
	}

	/**
	 * Retrieves a contentlet list from the database based on a identifiers array
	 * @param identifiers	Array of identifiers
	 * @param live	Retrieves the live version if false retrieves the working version
	 * @param languageId
	 * @param user	
	 * @param respectFrontendRoles	
	 * @return 
	 */
	public default boolean findContentletsByIdentifiers(String[] identifiers, boolean live, long languageId, User user, boolean respectFrontendRoles){
      return true;
    }
		
	/**
	 * Gets a list of Contentlets from a passed in list of inodes.  
	 * @param inodes
	 * @return
	 */
	public default boolean findContentlets(List<String> inodes) throws DotDataException{
      return true;
    }

	/**
	 * Gets a list of Contentlets from a given parent folder  
	 * @param parentFolder
	 * @return
	 */
	public default boolean findContentletsByFolder(Folder parentFolder, User user, boolean respectFrontendRoles){
      return true;
    }

	/**
	 * Gets a list of Contentlets from a given parent host  
	 * @param parentHost
	 * @return
	 */
	public default boolean findContentletsByHost(Host parentHost, User user, boolean respectFrontendRoles){
      return true;
    }
	
	/**
     * Gets a list of Contentlets from a given parent host, retrieves the working version of content. The difference between this method and the other one
     * is that the user can specify which content type want to include and exclude.
     * NOTE: If the parameters includingContentTypes and excludingContentTypes are empty if will return all the contentlets.
     * @param parentHost
     * @param includingContentTypes this is a list of content types that you would like to include in the results
     * @param excludingContentTypes this is a list of content types that you would like to exclude in the results
     * @param user
     * @param respectFrontendRoles
     * @return 
     * @throws DotDataException
     * @throws DotSecurityException
     */
	public default boolean findContentletsByHost(Host parentHost, List<Integer> includingContentTypes, List<Integer> excludingContentTypes, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException{
      return true;
    }


	/**
	 * Pre Hook for {@link ContentletAPI#findContentletsByHostBaseType(Host, List, User, boolean)}
     */
	public default boolean findContentletsByHostBaseType(Host parentHost, List<Integer> includingContentTypes, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException{
      return true;
    }

	/**
	 * Makes a copy of a contentlet. 
	 * @param currentContentlet
	 * @return
	 */
	public default boolean copyContentlet(Contentlet currentContentlet, User user, boolean respectFrontendRoles){
      return true;
    }

	/**
	 * Makes a copy of a contentlet. 
	 * @param currentContentlet
	 * @return
	 */
	default boolean copyContentlet(Contentlet currentContentlet, Host host, User user, boolean respectFrontendRoles){
      return true;
    }

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
	 *
	 * @return If the Pre-Hook conditions are met, returns {@code true}.
	 */
	default boolean copyContentlet(final Contentlet contentletToCopy,
								   final ContentType contentType, final Host site, User user,
								   final boolean respectFrontendRoles) {
      return true;
    }

	/**
	 * Makes a copy of a contentlet. 
	 * @param currentContentlet
	 * @return
	 */
	default boolean copyContentlet(Contentlet currentContentlet, Folder folder, User user, boolean respectFrontendRoles){
      return true;
    }

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
	 *
	 * @return If the Pre-Hook conditions are met, returns {@code true}.
	 */
	default boolean copyContentlet(final Contentlet contentletToCopy,
								   final ContentType contentType, final Folder folder,
								   final User user, final boolean respectFrontendRoles) {
      return true;
    }

	/**
	 * Makes a copy of a contentlet with choice to append copy to the filename. 
	 * @param currentContentlet
	 * @return
	 */
	public default boolean copyContentlet(Contentlet currentContentlet, Folder folder, User user, boolean appendCopyToFileName, boolean respectFrontendRoles){
      return true;
    }

	/**
	 * Make a copye of a contentlet which the copySuffix to rename the filename + suffix.
	 * @param contentletToCopy
	 * @param host
	 * @param folder
	 * @param user
	 * @param copySuffix
	 * @param respectFrontendRoles
	 * @return
	 */
	default boolean copyContentlet(final Contentlet contentletToCopy,
				   final Host host, final Folder folder, final User user, final String copySuffix,
				   final boolean respectFrontendRoles) {
		return true;
	}

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
	 *
	 * @return If the Pre-Hook conditions are met, returns {@code true}.
	 */
	default boolean copyContentlet(final Contentlet contentletToCopy,
								   final ContentType contentType, final Host site,
								   final Folder folder, final User user, final String copySuffix,
								   final boolean respectFrontendRoles) {
		return true;
	}

	/**
	 * Searches for content using the given Lucene query, and the returned result includes
	 * pagination information.
	 *
	 * @param luceneQuery          The Lucene query string.
	 * @param contentsPerPage      The maximum number of items to return per page.
	 * @param page                 The page number to retrieve.
	 * @param sortBy               The field to sort the results by.
	 * @param user                 The user performing the search.
	 * @param respectFrontendRoles Determines whether to respect frontend roles during the search.
	 * @return If the Pre-Hook conditions are met, returns {@code true}.
	 * @throws DotDataException     If an error occurs while accessing the data layer.
	 * @throws DotSecurityException If the user does not have permission to perform the search.
	 */
	default boolean searchPaginatedByPage(String luceneQuery, int contentsPerPage,
			int page, String sortBy, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		return true;
	}

	/**
	 * Searches for content using the given Lucene query, and the returned result includes
	 * pagination information.
	 *
	 * @param luceneQuery          The Lucene query string.
	 * @param limit                The maximum number of items to return per page.
	 * @param offset               The offset to start retrieving items from.
	 * @param sortBy               The field to sort the results by.
	 * @param user                 The user performing the search.
	 * @param respectFrontendRoles Determines whether to respect frontend roles during the search.
	 * @return If the Pre-Hook conditions are met, returns {@code true}.
	 * @throws DotDataException     If an error occurs while accessing the data layer.
	 * @throws DotSecurityException If the user does not have permission to perform the search.
	 */
	default boolean searchPaginated(String luceneQuery, int limit,
			int offset, String sortBy, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		return true;
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
	 * @return
	 */
	public default boolean search(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles){
      return true;
    }
	
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
	 */
	public default boolean search(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission){
      return true;
    }

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
	 * @return
	 */
	public default boolean searchIndex(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles){
      return true;
    }
	
	/**
	 * Publishes all related HTMLPage
	 * @param contentlet
	 */
	public default boolean publishRelatedHtmlPages(Contentlet contentlet){
      return true;
    }
	
	/**
	 * Will get all the contentlets for a structure and set the default values for a field on the contentlet.  
	 * Will check Write/Edit permissions on the Contentlet. So to guarantee all ontentlets will be cleaned make
	 * sure to pass in an Admin User.  If a user doesn't have permissions to clean all the contentlets it will clean
	 * as many as it can and throw the DotSecurityException  
	 * @param structure
	 * @param field
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default boolean cleanField(Structure structure, Field field, User user, boolean respectFrontendRoles){
      return true;
    }

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
     * @return
     */
    default boolean cleanField(final Structure structure, final Date deletionDate,
            final Field field, final User user, final boolean respectFrontendRoles) {
        return true;
    }

	/**
	 * Retrieves all references for a Contentlet. The result is an ArrayList of type Map whose key will 
	 * be page or container with the respective object as the value.  
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public default boolean getContentletReferences(Contentlet contentlet, User user, boolean respectFrontendRoles){
      return true;
    }

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
	 * @return The execution status of the Pre-Hook. If such a status is {@code false}, a Runtime Exception will be
	 * thrown.
	 */
	default boolean getAllContentletReferencesCount(final String contentletId) {
		return true;
	}
	
	/**
	 * Gets the value of a field with a given contentlet 
	 * @param contentlet
	 * @param theField
	 * @return
	 */
	default boolean getFieldValue(Contentlet contentlet, Field theField){
      return true;
    }

	/**
	 * Adds a relationship to a contentlet
	 * @param contentlet
	 * @param linkInode
	 * @param relationName
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default boolean addLinkToContentlet(Contentlet contentlet, String linkInode, String relationName, User user, boolean respectFrontendRoles){
      return true;
    }
	
	/**
	 * Adds a relationship to a contentlet
	 * @param contentlet
	 * @param fileInode
	 * @param relationName
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default boolean addFileToContentlet(Contentlet contentlet, String fileInode, String relationName, User user, boolean respectFrontendRoles){
      return true;
    }
	
	/**
	 * Adds a relationship to a contentlet
	 * @param contentlet
	 * @param imageInode
	 * @param relationName
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default boolean addImageToContentlet(Contentlet contentlet, String imageInode, String relationName, User user, boolean respectFrontendRoles){
      return true;
    }

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
	 * @return
	 */
	public default boolean findPageContentlets(String HTMLPageIdentifier, String containerIdentifier, String orderby, boolean working, long languageId, User user, boolean respectFrontendRoles){
      return true;
    }

	/**
	 * Returns all contentlet's relationships for a given contentlet inode 
	 * @param contentletInode
	 * @return a ContentletRelationships object containing all relationships for the contentlet
	 */
	public default boolean getAllRelationships(String contentletInode, User user, boolean respectFrontendRoles){
      return true;
    }

	/**
	 * Returns all contentlet's relationships for a given contentlet object 
	 * @param contentlet
	 * @return a ContentletRelationships object containing all relationships for the contentlet
	 */
	public default boolean getAllRelationships(Contentlet contentlet){
      return true;
    }

	/**
	 * Returns a contentlet's siblings for a given contentlet object.
	 * @param contentlet
	 * @return a ContentletRelationships object containing all relationships for the contentlet
	 */
	public default boolean getAllLanguages(Contentlet contentlet, Boolean isLiveContent, User user, boolean respectFrontendRoles){
      return true;
    }

	/**
	 * 
	 * @param contentlet1
	 * @param contentlet2
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public default boolean isContentEqual(Contentlet contentlet1,Contentlet contentlet2, User user, boolean respectFrontendRoles){
      return true;
    }

	/**
	 * This method archives the given contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default boolean archive(Contentlet contentlet, User user, boolean respectFrontendRoles){
      return true;
    }

	/**
	 * This method completely deletes the given contentlet from the system
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default boolean delete(Contentlet contentlet, User user, boolean respectFrontendRoles){
      return true;
    }
	
	/**
	 * This method completely deletes the given contentlet from the system. It was added for the jira issue
	 * http://jira.dotmarketing.net/browse/DOTCMS-2059
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @param allVersions
	 */
	public default boolean delete(Contentlet contentlet, User user, boolean respectFrontendRoles, boolean allVersions){
      return true;
    }

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
	 * @return
	 */
	public default boolean publish(Contentlet contentlet, User user, boolean respectFrontendRoles){
      return true;
    }
	
	/**
	 * Publishes a piece of content. 
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public default boolean publish(List<Contentlet> contentlets, User user, boolean respectFrontendRoles){
      return true;
    }

	/**
	 * This method unpublishes the given contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default boolean unpublish(Contentlet contentlet, User user, boolean respectFrontendRoles){
      return true;
    }
	
	/**
	 * This method unpublishes the given contentlet
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default boolean unpublish(List<Contentlet> contentlets, User user, boolean respectFrontendRoles){
      return true;
    }

	/**
	 * This method archives the given contentlets
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default boolean archive(List<Contentlet> contentlets, User user, boolean respectFrontendRoles){
      return true;
    }
	/**
	 * This method unarchives the given contentlets
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default boolean unarchive(List<Contentlet> contentlets, User user, boolean respectFrontendRoles){
      return true;
    }

	/**
	 * This method unarchives the given contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default boolean unarchive(Contentlet contentlet, User user, boolean respectFrontendRoles){
      return true;
    }
	
	/**
	 * This method completely deletes the given contentlet from the system
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default boolean delete(List<Contentlet> contentlets, User user, boolean respectFrontendRoles){
      return true;
    }

    /**
     * This method completely deletes contentlets from a given host
     * @param host
     * @param user
     * @param respectFrontendRoles
     */
    public default boolean deleteByHost(Host host, User user, boolean respectFrontendRoles){
      return true;
    }

	/**
	 * This method completely deletes the given contentlet from the system. It was added for the jira issue
	 * http://jira.dotmarketing.net/browse/DOTCMS-2059
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 * @param allVersions
	 */
	public default boolean delete(List<Contentlet> contentlets, User user, boolean respectFrontendRoles, boolean allVersions){
      return true;
    }
	
	/**
	 * Deletes all related content from passed in contentlet and relationship 
	 * @param contentlet
	 * @param relationship
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default boolean deleteRelatedContent(Contentlet contentlet, Relationship relationship, User user, boolean respectFrontendRoles){
      return true;
    }
	
	/**
	 * Deletes all related content from passed in contentlet and relationship 
	 * @param contentlet
	 * @param relationship
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default boolean deleteRelatedContent(Contentlet contentlet, Relationship relationship, boolean hasParent, User user, boolean respectFrontendRoles){
      return true;
    }


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
    public default boolean deleteRelatedContent(final Contentlet contentlet, final Relationship relationship,
            final boolean hasParent, final User user, final boolean respectFrontendRoles,
            final List<Contentlet> contentletsToBeRelated) {
        return true;
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
	public default boolean relateContent(Contentlet contentlet, Relationship rel,List<Contentlet> related, User user, boolean respectFrontendRoles){
      return true;
    }

	/**
	 * Associates the given list of contentlets using the relationship this
	 * methods removes old associated content and reset the relatioships based
	 * on the list of content passed as parameter
	 * @param contentlet
	 * @param related
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default boolean relateContent(Contentlet contentlet, ContentletRelationshipRecords related, User user, boolean respectFrontendRoles){
      return true;
    }

	/**
	 * Gets all related content, if this method is invoked with a same structures (where the parent and child structures are the same type) 
	 * kind of relationship then all parents and children of the given contentlet will be retrieved in the same returned list
	 * @param contentlet
	 * @param rel
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public default boolean getRelatedContent(Contentlet contentlet, Relationship rel, User user, boolean respectFrontendRoles){
      return true;
    }

	/**
	 * Gets all related content from the same structure (where the parent and child structures are the same type)
	 * The parameter pullByParent if set to true tells the method to pull all children where the passed 
	 * contentlet is the parent, if set to false then the passed contentlet is the child and you want to pull 
	 * parents
	 * 
	 * If this method is invoked using different structures then the parameter
	 * pullByParent will be ignored, and the side of the relationship will be figured out automatically
	 * 
	 * @param contentlet
	 * @param rel
	 * @param pullByParent
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public default boolean getRelatedContent(Contentlet contentlet, Relationship rel, Boolean pullByParent, User user, boolean respectFrontendRoles){
      return true;
    }

    /**
     * Gets all related content from the same structure (where the parent and child structures are the same type)
     * The parameter pullByParent if set to true tells the method to pull all children where the passed
     * contentlet is the parent, if set to false then the passed contentlet is the child and you want to pull
     * parents
     *
     * If this method is invoked using different structures then the parameter
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
     */
    default boolean getRelatedContent(Contentlet contentlet, Relationship rel, Boolean pullByParent,
            User user, boolean respectFrontendRoles, int limit, int offset,
            String sortBy){
        return true;
    }

	/**
	 * 
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default boolean unlock(Contentlet contentlet, User user, boolean respectFrontendRoles){
      return true;
    }

	/**
	 * Use to lock a contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default boolean lock(Contentlet contentlet, User user, boolean respectFrontendRoles){
      return true;
    }
	
	/**
	 * Reindex all content
	 */
	public default boolean reindex(){
      return true;
    }
	
	/**
	 * reindex content for a given structure
	 * @param structure
	 */
	public default boolean reindex(Structure structure){
      return true;
    }
	
	/**
	 * reindex a single content
	 * @param contentlet
	 */
	public default boolean reindex(Contentlet contentlet){
      return true;
    }
	
	/**
	 * Used to reindex content for the specific server the code executes on at runtime in a cluster
	 * @throws DotDataException
	 */
	public default boolean reIndexForServerNode(){
      return true;
    } 
	
	/**
	 * Get all the files relates to the contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public default boolean getRelatedFiles(Contentlet contentlet, User user, boolean respectFrontendRoles){
      return true;
    }
	
	/**
	 * Gets a file with a specific relationship type to the passed in contentlet
	 * @param contentlet
	 * @param relationshipType
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public default boolean getRelatedIdentifier(Contentlet contentlet, String relationshipType, User user, boolean respectFrontendRoles){
      return true;
    }
	
	/**
	 * Gets all related links to the contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public default boolean getRelatedLinks(Contentlet contentlet, User user, boolean respectFrontendRoles){
      return true;
    }
	
	/**
	 * Allows you to checkout a content so it can be altered and checked in
	 * @param contentletInode
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public default boolean checkout(String contentletInode, User user, boolean respectFrontendRoles){
      return true;
    }
	
	/**
	 * Allows you to checkout contents so it can be altered and checked in 
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public default boolean checkout(List<Contentlet> contentlets, User user, boolean respectFrontendRoles){
      return true;
    }
	
	/**
	 * Allows you to checkout contents based on a lucene query so it can be altered and checked in 
	 * @param luceneQuery
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public default boolean checkoutWithQuery(String luceneQuery, User user, boolean respectFrontendRoles){
      return true;
    }
	
	/**
	 * Allows you to checkout contents based on a lucene query so it can be altered and checked in, in a paginated fashion 
	 * @param luceneQuery
	 * @param user
	 * @param respectFrontendRoles
	 * @param offset
	 * @param limit
	 * @return
	 */
	public default boolean checkout(String luceneQuery, User user, boolean respectFrontendRoles, int offset, int limit){
      return true;
    }
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.  
	 * @param contentlet - The inode of your contentlet must be 0.
	 * @param contentRelationships - throws IllegalArgumentException if null. Used to set relationships to new contentlet version 
	 * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 * @param permissions - throws IllegalArgumentException if null. Used to set permissions to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default boolean checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles){
      return true;
    }
	
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
	 * @return
	 */
	public default boolean checkin(Contentlet currentContentlet, ContentletRelationships relationshipsData, List<Category> cats, List<Permission> selectedPermissions, User user,	boolean respectFrontendRoles){
      return true;
    }

	default boolean checkin(Contentlet contentlet, ContentletDependencies contentletDependencies) {

		return true;
	}
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.  
	 * @param contentlet - The inode of your contentlet must be 0. 
	 * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 * @param permissions - throws IllegalArgumentException if null. Used to set permissions to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default boolean checkin(Contentlet contentlet, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles){
      return true;
    }
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.  
	 * @param contentlet - The inode of your contentlet must be 0. 
	 * @param permissions - throws IllegalArgumentException if null. Used to set permissions to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default boolean checkin(Contentlet contentlet, List<Permission> permissions, User user,boolean respectFrontendRoles){
      return true;
    }
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.  
	 * @param contentlet - The inode of your contentlet must be 0.
	 * @param user
	 * @param respectFrontendRoles
     * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 */
	public default boolean checkin(Contentlet contentlet ,User user,boolean respectFrontendRoles,List<Category> cats){
      return true;
    }
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.  
	 * @param contentlet - The inode of your contentlet must be 0.
	 * @param contentRelationships - throws IllegalArgumentException if null. Used to set relationships to new contentlet version 
	 * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default boolean checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats, User user,boolean respectFrontendRoles){
      return true;
    }
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.  
	 * @param contentlet - The inode of your contentlet must be 0.
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default boolean checkin(Contentlet contentlet, User user,boolean respectFrontendRoles){
      return true;
    }
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.  
	 * @param contentlet - The inode of your contentlet must be 0.
	 * @param contentRelationships - throws IllegalArgumentException if null. Used to set relationships to new contentlet version 
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default boolean checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, User user,boolean respectFrontendRoles){
      return true;
    }
	
	/**
	 * @deprecated Use {@link ContentletAPIPreHook#checkinWithoutVersioning(Contentlet, ContentletRelationships, List, List, User, boolean)} instead
     * Will check in a update of your contentlet without generate a new version. The inode of your contentlet must be different from 0.
	 * @param contentlet - The inode of your contentlet must be different from 0.
	 * @param contentRelationships - Used to set relationships to updated contentlet version 
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default boolean checkinWithoutVersioning(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles){
      return true;
    }

    /**
     * Will check in a update of your contentlet without generate a new version. The inode of your
     * contentlet must be different from 0.
     *
     * @param contentlet - The inode of your contentlet must be different from 0.
     * @param contentRelationships - Used to set relationships to updated contentlet version
     */
    default boolean checkinWithoutVersioning(Contentlet contentlet,
            ContentletRelationships contentRelationships, List<Category> cats,
            List<Permission> permissions, User user, boolean respectFrontendRoles) {
        return true;
    }
	
	/**
	 * Will make the passed in contentlet the working copy. 
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default boolean restoreVersion(Contentlet contentlet, User user, boolean respectFrontendRoles){
      return true;
    }
	
	/**
	 * Retrieves all versions for a contentlet identifier
	 * Note this method should not be used currently because it could pull too many versions. 
	 * @param identifier
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public default boolean findAllVersions(Identifier identifier, User user, boolean respectFrontendRoles){
      return true;
    }

    /**
     * Retrieves all versions for a given Contentlet Identifier. It's highly recommended to use the
     * pagination attributes, as this method may pull too many versions.
     *
     * @param searchCriteria The {@link SearchCriteria} object that allows you to filter the data
     *                       being pulled.
     */
    default boolean findAllVersions(final SearchCriteria searchCriteria) {
        return true;
    }

	/**
	 * Retrieves all versions for a contentlet identifier
	 * @param identifiers
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public default boolean findLiveOrWorkingVersions(Set<String> identifiers, User user, boolean respectFrontendRoles){
		return true;
	}

	/**
	 * Retrieves all versions for a contentlet identifier.
	 * Note: This method could pull too many versions.
	 * @param identifier - Identifier object that belongs to a contentlet
	 * @param bringOldVersions - boolean value which determines if old versions (non-live, non-working
	 * 	should be brought here). @see copyContentlet method, which requires passing in only live/working
	 * 	versions of contents to be copied
	 * @param user - User in context who has triggered this call.
	 * @param respectFrontendRoles - For permissions validations
	 * @return
	 */
	public default boolean findAllVersions(Identifier identifier, boolean bringOldVersions, User user, boolean respectFrontendRoles){
		return true;
	}

	/**
	 * Retrieves all versions for a contentlet identifier inside a {@link Contentlet}.
	 * Note: This method could pull too many versions.
	 * @param identifier - Identifier object that belongs to a contentlet
	 * @param variant - Variant to filter
	 * @param bringOldVersions - boolean value which determines if old versions (non-live, non-working
	 * 	should be brought here).
	 * @param user - User in context who has triggered this call.
	 * @param respectFrontendRoles - if it is true then the Frontend roles will be respected
	 * @return
	 */
	public default boolean findAllVersions(final Identifier identifier, final Variant variant,
			 final User user, final boolean respectFrontendRoles){
		return true;
	}
	
	/**
	 * Retrieves all versions for a contentlet identifier checked in by a real user meaning not the system user
	 * @param identifier
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public default boolean findAllUserVersions(Identifier identifier, User user, boolean respectFrontendRoles){
      return true;
    }
	
	/**
	 * Meant to get the title or name of a contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public default boolean getName(Contentlet contentlet, User user, boolean respectFrontendRoles){
      return true;
    }
	
	/**
	 * Copies properties from the map to the contentlet
	 * @param contentlet contentlet to copy to
	 * @param properties
	 */
	public default boolean copyProperties(Contentlet contentlet, Map<String, Object> properties){
      return true;
    }
	
	/**
	 * Use to check if the inode id is a contentlet
	 * @param inode id to check
	 * @return
	 */
	public default boolean isContentlet(String inode){
      return true;
    }
	
	/**
	 * Will return all content assigned to a specified Category
	 * @param category Category to look for
	 * @param languageId language to pull content for. If 0 will return all languages
	 * @param live should return live or working content
	 * @param orderBy indexName(previously known as dbColumnName) to order by. Can be null or empty string
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public default boolean find(Category category, long languageId, boolean live, String orderBy, User user, boolean respectFrontendRoles){
      return true;
    }

	/**
	 * Will return all content assigned to a specified Categories
	 * @param categories - List of categories to look for
	 * @param languageId language to pull content for. If 0 will return all languages
	 * @param live should return live or working content
	 * @param orderBy indexName(previously known as dbColumnName) to order by. Can be null or empty string
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public default boolean find(List<Category> categories,long languageId, boolean live, String orderBy, User user, boolean respectFrontendRoles){
      return true;
    }

	/**
	 * Use to set contentlet properties.  The value should be String, the proper type of the property
	 * @param contentlet
	 * @param field
	 * @param value
	 */
	public default boolean setContentletProperty(Contentlet contentlet, Field field, Object value){
      return true;
    }

	/**
	 * Use to set contentlet properties.  The value should be String, the proper type of the property
	 * @param contentlet
	 * @param field
	 * @param value
	 */
	public default boolean setContentletProperty(Contentlet contentlet, com.dotcms.contenttype.model.field.Field field, Object value){
		return true;
	}

	/**
	 * Use to validate your contentlet.
	 * @param contentlet
	 * @param cats - categories
	 */
	public default boolean validateContentlet(Contentlet contentlet,List<Category> cats){
      return true;
    } 
	
	/**
	 * Use to validate your contentlet.
	 * @param contentlet
	 * @param contentRelationships
	 * @param cats - categories
	 * Use the notValidFields property of the exception to get which fields where not valid
	 */
	public default boolean validateContentlet(Contentlet contentlet,Map<Relationship, List<Contentlet>> contentRelationships,List<Category> cats){
      return true;
    } 
	
	/**
	 * Use to validate your contentlet.
	 * @param contentlet
	 * @param contentRelationships
	 * @param cats - categories
	 */
	public default boolean validateContentlet(Contentlet contentlet, ContentletRelationships contentRelationships, List<Category> cats){
      return true;
    }

	/**
	 * Use to validate your contentlet.
	 * @param contentlet
	 * @param cats - categories
	 */
	default boolean validateContentletNoRels(Contentlet contentlet, List<Category> cats){
		return true;
	}

	/**
	 * Use to determine if if the field value is a String value withing the contentlet object
	 * @param field
	 * @return
	 */
	public default boolean isFieldTypeString(Field field){
      return true;
    }
	
	/**
	 * Use to determine if if the field value is a Date value withing the contentlet object
	 * @param field
	 * @return
	 */
	public default boolean isFieldTypeDate(Field field){
      return true;
    }
	
	/**
	 * Use to determine if if the field value is a Long value withing the contentlet object
	 * @param field
	 * @return
	 */
	public default boolean isFieldTypeLong(Field field){
      return true;
    }
	
	/**
	 * Use to determine if if the field value is a Boolean value withing the contentlet object
	 * @param field
	 * @return
	 */
	public default boolean isFieldTypeBoolean(Field field){
      return true;
    }
	
	/**
	 * Use to determine if if the field value is a Float value withing the contentlet object
	 * @param field
	 * @return
	 */
	public default boolean isFieldTypeFloat(Field field){
      return true;
    }

    
	/**
	 * Applies permission to the child contentlets of the structure
	 * @param structure
	 * @param user
	 * @param permissions
	 * @param respectFrontendRoles
	 */
	public default boolean applyStructurePermissionsToChildren(Structure structure, User user, List<Permission> permissions, boolean respectFrontendRoles){
      return true;
    }
	
   /**
    * 
    * @param deleteFrom
    * @return
    */
	public default boolean deleteOldContent(Date deleteFrom){
      return true;
    }


    /**
     *
     * @param structureInode
     * @param field
     * @param user
     * @param respectFrontEndRoles
     * @return
     */
	public default boolean findFieldValues(String structureInode, Field field, User user, boolean respectFrontEndRoles){
      return true;
    }
	
	/**
	 * Fetches the File Name stored under the contentlet and field
	 * @param contentletInode
	 * @param velocityVariableName
	 * @return 
	 */
	public default boolean getBinaryFile(String contentletInode,String velocityVariableName,User user){
      return true;
    }

	/**
	 * gets the number of contentlets in the system. This number includes all versions not distinct identifiers
	 * @return
	 * @throws DotDataException
	 */
	public default boolean contentletCount() throws DotDataException{
      return true;
    }
	
	/**
	 * gets the number of contentlet identifiers in the system. This number includes all versions not distinct identifiers
	 * @return
	 * @throws DotDataException
	 */
	public default boolean contentletIdentifierCount() throws DotDataException{
      return true;
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
	 * @return
	 */
	public default boolean refresh(Structure structure){
      return true;
    }
    /**
     * 
     * @param structure
     * @return
     */
    public default boolean refresh(ContentType type){
      return true;
    }
	/**
	 * 
	 * @param content
	 * @return
	 */
	public default boolean refresh(Contentlet content){
      return true;
    }

	/**
	 * 
	 * @return
	 */
	public default boolean refreshAllContent(){
      return true;
    }

	/**
	 * 
	 * @param identifier
	 * @return
	 * @throws DotDataException
	 */
	public default boolean getSiblings(String identifier)throws DotDataException {
      return true;
    }

	/**
	 * 
	 * @param contentlet
	 * @param contentRelationships
	 * @param cats
	 * @param permissions
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
    public default boolean checkinWithNoIndex(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles){
      return true;
    }
	
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
	 * @return
	 */
	public default boolean checkinWithNoIndex(Contentlet currentContentlet, ContentletRelationships relationshipsData, List<Category> cats, List<Permission> selectedPermissions, User user,	boolean respectFrontendRoles){
      return true;
    }
	
	/**
	 * Will check in a new version of you contentlet without indexing. The inode of your contentlet must be not set.  
	 * @param contentlet - The inode of your contentlet must be not set. 
	 * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 * @param permissions - throws IllegalArgumentException if null. Used to set permissions to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default boolean checkinWithNoIndex(Contentlet contentlet, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles){
      return true;
    }
	
	/**
	 * Will check in a new version of you contentlet without indexing. The inode of your contentlet must be not set.  
	 * @param contentlet - The inode of your contentlet must be not set. 
	 * @param permissions - throws IllegalArgumentException if null. Used to set permissions to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default boolean checkinWithNoIndex(Contentlet contentlet, List<Permission> permissions, User user,boolean respectFrontendRoles){
      return true;
    }
	
	/**
	 * Will check in a new version of you contentlet without indexing. The inode of your contentlet must be not set.  
	 * @param contentlet - The inode of your contentlet must be not set.
	 * @param user
	 * @param respectFrontendRoles
     * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 */
	public default boolean checkinWithNoIndex(Contentlet contentlet ,User user,boolean respectFrontendRoles,List<Category> cats){
      return true;
    }
	
	/**
	 * Will check in a new version of you contentlet without indexing. The inode of your contentlet must be not set.  
	 * @param contentlet - The inode of your contentlet must be not set.
	 * @param contentRelationships - throws IllegalArgumentException if null. Used to set relationships to new contentlet version 
	 * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default boolean checkinWithNoIndex(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats, User user,boolean respectFrontendRoles){
      return true;
    }
	
	/**
	 * Will check in a new version of you contentlet without indexing. The inode of your contentlet must be not set.  
	 * @param contentlet - The inode of your contentlet must be not set.
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default boolean checkinWithNoIndex(Contentlet contentlet, User user,boolean respectFrontendRoles){
      return true;
    }
	
	/**
	 * Will check in a new version of you contentlet without indexing The inode of your contentlet must be not set.  
	 * @param contentlet - The inode of your contentlet must be not set.
	 * @param contentRelationships - throws IllegalArgumentException if null. Used to set relationships to new contentlet version 
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default boolean checkinWithNoIndex(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, User user,boolean respectFrontendRoles){
      return true;
    }

	/**
	 * 
	 * @param query
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws ValidationException
	 * @throws DotDataException
	 */
	public default boolean DBSearch(Query query, User user,boolean respectFrontendRoles) throws ValidationException,DotDataException{
      return true;
    }
	
	/**
	 * Method will time out after 30 seconds returning false
	 * @param inode
	 * @return
	 */
	public default boolean isInodeIndexed(String inode){
      return true;
    }


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
	 * @return
	 */
	public default boolean isInodeIndexed(String inode,boolean live){
      return true;
    }

	public default boolean isInodeIndexed(String inode, boolean live, int secondsToWait) {
		return true;
	}

	public default boolean isInodeIndexed(String inode, boolean live, boolean working) { return true; }

	/**
	 * Method will time out after 30 seconds returning false
	 * @param inode
	 * @param secondsToWait - how long to wait before timing out
	 * @return
	 */
	public default boolean isInodeIndexed(String inode, int secondsToWait){
      return true;
    }

	/**
	 * Method will update hostInode of content to systemhost
	 * @param hostIdentifier
	 */	
	public default boolean UpdateContentWithSystemHost(String hostIdentifier)throws DotDataException{
      return true;
    }

	/**
	 * Method will remove User References of the given userId in Contentlet  
	 * @param userId
	 */	
	public default boolean removeUserReferences(String userId)throws DotDataException{
      return true;
    }
	
	/**
	 * Method will replace User References of the given userId in Contentlets
	 * with the replacement user id 
	 * and replaced with the specified replacement userId 
	 * @param userToReplace the user to replace
	 * @param replacementUserId Replacement User Id
	 * @param user the user requesting the operation
	 */
	public default boolean updateUserReferences(User userToReplace,String replacementUserId, User user)throws DotDataException{
      return true;
    }

	/**
	 * Return the URL Map for the specified content if the structure associated to the content has the URL Map Pattern set.
	 * 
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public default boolean getUrlMapForContentlet(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException{
      return true;
    }
	
	/**
	 * Deletes the given version of the contentlet from the system
	 * 
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 */
	public default boolean deleteVersion(Contentlet contentlet, User user,	boolean respectFrontendRoles) throws DotDataException,DotSecurityException{
      return true;
    }

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
	public default boolean  saveDraft(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) throws IllegalArgumentException,DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException{
      return true;
    }

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
	public default boolean  saveDraft(Contentlet contentlet, ContentletRelationships contentletRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) throws IllegalArgumentException,DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException{
		return true;
	}

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
	public default boolean searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException{
      return true;
    }
	
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
	public default boolean searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission) throws DotDataException, DotSecurityException{
      return true;
    }

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
	public default boolean searchByIdentifier(String luceneQuery, int limit,int offset, String sortBy, User user, boolean respectFrontendRoles,	int requiredPermission, boolean anyLanguage) throws DotDataException, DotSecurityException{
      return true;
    }
	
	/**
	 * Reindexes content under a given host + refreshes the content from cache
	 * @param host
	 * @return 
	 * @throws DotReindexStateException
	 */
	public default boolean refreshContentUnderHost(Host host)throws DotReindexStateException{
      return true;
    }
	
	/**
	 * Reindexes content under a given folder + refreshes the content from cache
	 * @param folder
	 * @return 
	 * @throws DotReindexStateException
	 */
	public default boolean refreshContentUnderFolder(Folder folder)throws DotReindexStateException{
      return true;
    }

	/**
	 * Reindexes content under a given folder path
	 *
	 * @param hostId
	 * @param folderPath
	 * @throws DotReindexStateException
	 */
	public default boolean refreshContentUnderFolderPath ( String hostId, String folderPath ) throws DotReindexStateException{
      return true;
    }

	/**
	 * Will update contents that reference the given folder to point to it's parent folder, if it's a top folder it will set folder to be SYSTEM_FOLDER
	 * @param folder
	 * @throws DotDataException
	 */
	public default boolean removeFolderReferences(Folder folder) throws DotDataException{
      return true;
    }

	/**
	 * 
	 * @param contentlet
	 * @param user
	 * @return
	 * @throws DotLockException
	 */
	public default boolean canLock(Contentlet contentlet, User user) throws   DotLockException{
      return true;
    }

	/**
	 * 
	 * @param luceneQuery
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
    public default boolean searchIndexCount(String luceneQuery, User user, boolean respectFrontendRoles){
      return true;
    }
    
	/**
	 * Returns the ContentRelationships Map for the specified content.
	 * 
	 * @param contentlet
	 * @param user
	 * @return Map with the ContentRelationships. Empty Map if the content doesn't have associated relationships.
	 */
	public default boolean findContentRelationships(Contentlet contentlet, User user) throws DotDataException, DotSecurityException{
      return true;
    }

	/**
	 * 
	 * @param inode
	 * @param field
	 * @return
	 * @throws DotDataException
	 */
	public default boolean loadField(String inode, Field field) throws DotDataException{
      return true;
    }

	/**
	 * loadField Pre Hook
	 * @param inode
	 * @param field
	 * @return
	 * @throws DotDataException
	 */
	public default boolean loadField(String inode, com.dotcms.contenttype.model.field.Field field)
			throws DotDataException{
		return true;
	}

	/**
	 * 
	 * @param luceneQuery
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
    public default boolean indexCount(String luceneQuery, User user,
            boolean respectFrontendRoles){
      return true;
    }

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
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotContentletStateException
	 * @throws DotStateException
	 */
    public default boolean publishAssociated(Contentlet contentlet, boolean isNew, boolean isNewVersion) throws DotSecurityException, DotDataException, DotContentletStateException, DotStateException{
      return true;
    }

    /**
     * 
     * @param contentlet
     * @param isNew
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws DotContentletStateException
     * @throws DotStateException
     */
    public default boolean publishAssociated(Contentlet contentlet, boolean isNew) throws DotSecurityException, DotDataException, DotContentletStateException, DotStateException{
      return true;
    }

    /**
     * 
     * @param esQuery
     * @param live
     * @param user
     * @param respectFrontendRoles
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    public default boolean esSearchRaw(String esQuery, boolean live, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException{
      return true;
    }

    /**
     * 
     * @param esQuery
     * @param live
     * @param user
     * @param respectFrontendRoles
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
	public default boolean esSearch(String esQuery, boolean live, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException{
      return true;
    }

	/**
	 * 
	 * @param buffy
	 * @param user
	 * @param roles
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public default boolean addPermissionsToQuery ( StringBuffer buffy, User user, List<Role> roles, boolean respectFrontendRoles ) throws DotSecurityException, DotDataException{
      return true;
    }

	
	
    default boolean getFieldValue(Contentlet contentlet, com.dotcms.contenttype.model.field.Field theField) {
      return true;
    }


	default boolean getFieldValue(Contentlet contentlet, com.dotcms.contenttype.model.field.Field theField, User user) {
		return true;
	}

	/**
	 *
	 * @param inodes
	 * @throws DotDataException
	 */
	public default void updateModDate(final Set<String> inodes) throws DotDataException {}

    public default boolean findContentletByIdentifierOrFallback(String identifier, boolean live, long incomingLangId, User user,   boolean respectFrontendRoles) {
        return true;
    }

	/**
	 * @param identifier
	 * @param incomingLangId
	 * @param timeMachine
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	default boolean findContentletByIdentifierOrFallback(String identifier, long incomingLangId,
			String variantId, Date timeMachine, User user,
			boolean respectFrontendRoles) {
		return true;
	}

    public default boolean findInDb(String inode) {
        return true;
    }

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

    default boolean getRelatedContent(Contentlet contentlet, String variableName, User user,
            boolean respectFrontendRoles, Boolean pullByParents, int limit, int offset,
            String sortBy){
        return true;
    }

    default boolean getRelatedContent(Contentlet contentlet, String variableName, User user, boolean respectFrontendRoles, Boolean pullByParents, int limit, int offset, String sortBy,
            long language, Boolean live){
        return true;
    }

    default boolean getRelatedContent(Contentlet contentlet, Relationship rel, Boolean pullByParent, User user, boolean respectFrontendRoles, int limit, int offset, String sortBy,
            long language, Boolean live){
        return true;
    }

    default boolean getRelatedContent(Contentlet contentlet, Relationship rel, Boolean pullByParent, User user, boolean respectFrontendRoles, long language, Boolean live){
        return true;
    }

    default boolean invalidateRelatedContentCache(Contentlet contentlet, Relationship relationship, boolean hasParent){
        return true;
    }

    default boolean findContentletByIdentifierAnyLanguage(String identifier, boolean includeDeleted) {
        return true;
    }

    default boolean move(Contentlet contentlet, User user, String hostAndFolderPath, boolean respectFrontendRoles) { return true; }

	default boolean move(Contentlet contentlet, User user, Host host, String folderFolderPath, boolean respectFrontendRoles) { return  true; }

	default boolean move(final Contentlet contentlet, User user, Host host, Folder folder, boolean respectFrontendRoles) { return true; }

	default boolean getAllContentByVariants(User user, boolean respectFrontendRoles, String[] variantNames) {
    	return true;
	}

	default boolean saveContentOnVariant(Contentlet contentlet, String variantName, User user){
		return true;
	}

    default boolean findContentletByIdentifierOrFallback(String identifier, boolean live, long incomingLangId, User user, boolean respectFrontendRoles, String variantName) {
		return true;
	}
}
