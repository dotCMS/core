/**
 * 
 @param returnValue - value returned by primary API Method */
package com.dotmarketing.portlets.contentlet.business;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.lucene.queryParser.ParseException;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;

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
	public void findAllContent(int offset, int limit, List<Contentlet> returnValue);
	
	/**
	 * Finds a Contentlet Object given the inode
	 * @param inode
	 * @param returnValue - value returned by primary API Method
	 */
	public void find(String inode, User user, boolean respectFrontendRoles,Contentlet returnValue);
	
	/**
	 * Finds a Contentlet Object given the inode
	 * @param inode
	 * @param returnValue - value returned by primary API Method
	 */
	//public void find(long inode, User user, boolean respectFrontendRoles,Contentlet returnValue);
	
	/**
	 * Returns a live Contentlet Object for a given language 
	 * @param languageId
	 * @param inode
	 * @param returnValue - value returned by primary API Method
	 */
	public void findContentletForLanguage(long languageId, Identifier contentletId,Contentlet returnValue);
	
	/**
	 * Returns all Contentlets for a specific structure
	 * @param structure
	 * @param user
	 * @param respectFrontendRoles
	 * @param limit
	 * @param offset
	 * @param returnValue - value returned by primary API Method
	 */
	public void findByStructure(Structure structure, User user, boolean respectFrontendRoles, int limit, int offset,List<Contentlet> returnValue);
	
	/**
	 * Returns all Contentlets for a specific structure
	 * @param structureInode
	 * @param user
	 * @param respectFrontendRoles
	 * @param limit
	 * @param offset
	 * @param returnValue - value returned by primary API Method
	 */
	public void findByStructure(String structureInode, User user, boolean respectFrontendRoles, int limit, int offset,List<Contentlet> returnValue);
	
	/**
	 * Retrieves a contentlet from the database based on its identifier
	 * @param identifier 
	 * @param live Retrieves the live version if false retrieves the working version
	 * @param returnValue - value returned by primary API Method
	 */
	public void findContentletByIdentifier(String identifier, boolean live, long languageId, User user, boolean respectFrontendRoles,Contentlet returnValue);

	/**
	 * Retrieves a contentlet list from the database based on a identifiers array
	 * @param identifiers	Array of identifiers
	 * @param live	Retrieves the live version if false retrieves the working version
	 * @param languageId
	 * @param user	
	 * @param respectFrontendRoles	
	 * @param returnValue - value returned by primary API Method
	 */
	public void findContentletsByIdentifiers(String[] identifiers, boolean live, long languageId, User user, boolean respectFrontendRoles,List<Contentlet> returnValue);
		
	/**
	 * Gets a list of Contentlets from a passed in list of inodes.  
	 * @param inodes
	 * @param returnValue - value returned by primary API Method
	 */
	public void findContentlets(List<String> inodes,List<Contentlet> returnValue);
	
	/**
	 * Gets a list of Contentlets from a given parent folder  
	 * @param parentFolder
	 * @return
	 */
	public void findContentletsByFolder(Folder parentFolder, User user, boolean respectFrontendRoles) throws DotDataException;

	/**
	 * Gets a list of Contentlets from a given parent host  
	 * @param parentHost
	 * @return
	 */
	public void findContentletsByHost(Host parentHost, User user, boolean respectFrontendRoles) throws DotDataException;

	/**
	 * Makes a copy of a contentlet. 
	 * @param currentContentlet
	 * @param returnValue - value returned by primary API Method
	 */
	public void copyContentlet(Contentlet currentContentlet, User user, boolean respectFrontendRoles,Contentlet returnValue);

	/**
	 * Makes a copy of a contentlet. 
	 * @param currentContentlet
	 * @param returnValue - value returned by primary API Method
	 */
	public void copyContentlet(Contentlet currentContentlet, Host host, User user, boolean respectFrontendRoles,Contentlet returnValue);

	/**
	 * Makes a copy of a contentlet. 
	 * @param currentContentlet
	 * @param returnValue - value returned by primary API Method
	 */
	public void copyContentlet(Contentlet currentContentlet, Folder folder, User user, boolean respectFrontendRoles,Contentlet returnValue);

	/**
	 * Makes a copy of a contentlet with choice to append copy to the filename. 
	 * @param currentContentlet
	 * @param returnValue - value returned by primary API Method
	 */
	public void copyContentlet(Contentlet currentContentlet, Folder folder, User user, boolean appendCopyToFileName, boolean respectFrontendRoles,Contentlet returnValue);
	
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
	public void search(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles,List<Contentlet> returnValue);
	
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
	public void search(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission,List<Contentlet> returnValue);

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

	public void searchIndex(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles,List<ContentletSearch> returnValue);
	
	/**
	 * Publishes all related HTMLPage
	 * @param contentlet
	 */
	public void publishRelatedHtmlPages(Contentlet contentlet);
	
	/**
	 * Will get all the contentlets for a structure and set the default values for a field on the contentlet.  
	 * Will check Write/Edit permissions on the Contentlet. So to guarantee all COntentlets will be cleaned make 
	 * sure to pass in an Admin User.  If a user doesn't have permissions to clean all teh contentlets it will clean 
	 * as many as it can and throw the DotSecurityException  
	 * @param structure
	 * @param field
	 * @param user
	 * @param respectFrontendRoles
	 */
	public void cleanField(Structure structure, Field field, User user, boolean respectFrontendRoles);

	/**
	 * Will get all the contentlets for a structure and set the default values for a host field.  
	 * Will check Write/Edit permissions on the Contentlet. So to guarantee all Contentlets will be cleaned make 
	 * sure to pass in an Admin User.  If a user doesn't have permissions to clean all teh contentlets it will clean 
	 * as many as it can and throw the DotSecurityException  
	 * @param structure
	 * @param field
	 * @param user
	 * @param respectFrontendRoles
	 */
	public void cleanHostField(Structure structure, User user, boolean respectFrontendRoles);
	
	/**
	 * Finds the next date that a contentlet must be reviewed
	 * @param content 
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public void getNextReview(Contentlet content, User user, boolean respectFrontendRoles,Date returnValue);

	/**
	 * Retrieves all references for a Contentlet. The result is an ArrayList of type Map whose key will 
	 * be page or container with the respective object as the value.  
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public void getContentletReferences(Contentlet contentlet, User user, boolean respectFrontendRoles,List<Map<String, Object>> returnValue);
	
	/**
	 * Gets the value of a field with a given contentlet 
	 * @param contentlet
	 * @param theField
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public void getFieldValue(Contentlet contentlet, Field theField,Object returnValue);

	/**
	 * Adds a relationship to a contentlet
	 * @param contentlet
	 * @param linkInode
	 * @param relationName
	 * @param user
	 * @param respectFrontendRoles
	 */
	public void addLinkToContentlet(Contentlet contentlet, String linkInode, String relationName, User user, boolean respectFrontendRoles);
	
	/**
	 * Adds a relationship to a contentlet
	 * @param contentlet
	 * @param fileInode
	 * @param relationName
	 * @param user
	 * @param respectFrontendRoles
	 */
	public void addFileToContentlet(Contentlet contentlet, String fileInode, String relationName, User user, boolean respectFrontendRoles);
	
	/**
	 * Adds a relationship to a contentlet
	 * @param contentlet
	 * @param imageInode
	 * @param relationName
	 * @param user
	 * @param respectFrontendRoles
	 */
	public void addImageToContentlet(Contentlet contentlet, String imageInode, String relationName, User user, boolean respectFrontendRoles);

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
	public void findPageContentlets(String HTMLPageIdentifier, String containerIdentifier, String orderby, boolean working, long languageId, User user, boolean respectFrontendRoles,List<Contentlet> returnValue);

	/**
	 * Returns all contentlet's relationships for a given contentlet inode 
	 * @param contentletInode a ContentletRelationships object containing all relationships for the contentlet
	 * @param returnValue - value returned by primary API Method 
	 */
	public void getAllRelationships(String contentletInode, User user, boolean respectFrontendRoles,ContentletRelationships returnValue);

	/**
	 * Returns all contentlet's relationships for a given contentlet object 
	 * @param contentlet a ContentletRelationships object containing all relationships for the contentlet
	 * @param returnValue - value returned by primary API Method 
	 */
	public void getAllRelationships(Contentlet contentlet,ContentletRelationships returnValue);


	/**
	 * Returns a contentlet's siblings for a given contentlet object.
	 * @param contentlet
	 * a ContentletRelationships object containing all relationships for the contentlet
	 * @param returnValue - value returned by primary API Method 
	 */
	public void getAllLanguages(Contentlet contentlet, Boolean isLiveContent, User user, boolean respectFrontendRoles,List<Contentlet> returnValue);

	/**
	 * 
	 * @param contentlet1
	 * @param contentlet2
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public void isContentEqual(Contentlet contentlet1,Contentlet contentlet2, User user, boolean respectFrontendRoles,boolean returnValue);

	/**
	 * This method archives the given contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 */ 
	public void archive(Contentlet contentlet, User user, boolean respectFrontendRoles);

	

	/**
	 * This method completely deletes the given contentlet from the system
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 */
	public void delete(Contentlet contentlet, User user, boolean respectFrontendRoles);
	
	
	/**
	 * This method completely deletes the given contentlet from the system. It was added for the jira issue
	 * http://jira.dotmarketing.net/browse/DOTCMS-2059
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @param allVersions
	 */
	public void delete(Contentlet contentlet, User user, boolean respectFrontendRoles, boolean allVersions);
	/**
	 * Publishes a piece of content. 
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 */
	public void publish(Contentlet contentlet, User user, boolean respectFrontendRoles);
	
	/**
	 * Publishes a piece of content. 
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public void publish(List<Contentlet> contentlets, User user, boolean respectFrontendRoles);

	/**
	 * This method unpublishes the given contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public void unpublish(Contentlet contentlet, User user, boolean respectFrontendRoles);
	
	/**
	 * This method unpublishes the given contentlet
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 */
	public void unpublish(List<Contentlet> contentlets, User user, boolean respectFrontendRoles);

	/**
	 * This method archives the given contentlets
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 */
	public void archive(List<Contentlet> contentlets, User user, boolean respectFrontendRoles);
	/**
	 * This method unarchives the given contentlets
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 */
	public void unarchive(List<Contentlet> contentlets, User user, boolean respectFrontendRoles);

	/**
	 * This method unarchives the given contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 */
	public void unarchive(Contentlet contentlet, User user, boolean respectFrontendRoles);
	
	/**
	 * This method completely deletes the given contentlet from the system
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 */
	public void delete(List<Contentlet> contentlets, User user, boolean respectFrontendRoles);
	
	
	/**
	 * This method completely deletes the given contentlet from the system. It was added for the jira issue
	 * http://jira.dotmarketing.net/browse/DOTCMS-2059
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 * @param allVersions
	 */
	public void delete(List<Contentlet> contentlets, User user, boolean respectFrontendRoles, boolean allVersions);
	
	/**
	 * Deletes all related content from passed in contentlet and relationship 
	 * @param contentlet
	 * @param relationship
	 * @param user
	 * @param respectFrontendRoles
	 */
	public void deleteRelatedContent(Contentlet contentlet, Relationship relationship, User user, boolean respectFrontendRoles);
	
	/**
	 * Deletes all related content from passed in contentlet and relationship 
	 * @param contentlet
	 * @param relationship
	 * @param user
	 * @param respectFrontendRoles
	 */
	public void deleteRelatedContent(Contentlet contentlet, Relationship relationship, boolean hasParent, User user, boolean respectFrontendRoles);
	
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
	public void relateContent(Contentlet contentlet, Relationship rel,List<Contentlet> related, User user, boolean respectFrontendRoles);

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
	public void relateContent(Contentlet contentlet, ContentletRelationshipRecords related, User user, boolean respectFrontendRoles);

	/**
	 * Gets all related content, if this method is invoked with a same structures (where the parent and child structures are the same type) 
	 * kind of relationship then all parents and children of the given contentlet will be retrieved in the same returned list
	 * @param contentlet
	 * @param rel
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method */
	public void getRelatedContent(Contentlet contentlet, Relationship rel, User user, boolean respectFrontendRoles,List<Contentlet> returnValue);

	/**
	 * Gets all related content from a same structures (where the parent and child structures are the same type) 
	 * The parameter pullByParent if set to true tells the method to pull all children where the passed 
	 * contentlet is the parent, if set to false then the passed contentlet is the child and you want to pull 
	 * parents
	 * 
	 * If this method is invoked for a no same structures kind of relationships then the parameter
	 * pullByParent will be ignored, and the side of the relationship will be figured out automatically
	 * 
	 * @param contentlet
	 * @param rel
	 * @param pullByParent
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method */
	public void getRelatedContent(Contentlet contentlet, Relationship rel, boolean pullByParent, User user, boolean respectFrontendRoles,List<Contentlet> returnValue);

	/**
	 * Gets all contents referenced by a given file asset
	 * @param file asset.
	 * @param live contentlets or not.
	 * @param user
	 * @param respectFrontendRoles
	 * List of contentlets. Null if no related contentlets found.
	 * @param returnValue - value returned by primary API Method 
	 */
	public void getReferencingContentlet(File file, boolean live, User user, boolean respectFrontendRoles,List<Contentlet> returnValue);
	
	/**
	 * Refreshes (regenerates) all content files referenced by a given file asset
	 * @param file asset
	 * @param live contentlets or not 
	 */
	public void refreshReferencingContentlets(File file, boolean live);

	/**
	 * 
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 */
	public void unlock(Contentlet contentlet, User user, boolean respectFrontendRoles);

	/**
	 * Use to lock a contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 */
	public void lock(Contentlet contentlet, User user, boolean respectFrontendRoles);
	
	/**
	 * Reindex all content
	 */
	public void reindex();
	
	/**
	 * reindex content for a given structure
	 * @param structure
	 */
	public void reindex(Structure structure);
	
	/**
	 * reindex a single content
	 * @param contentlet
	 */
	public void reindex(Contentlet contentlet);
	
	/**
	 * Used to reindex content for the specific server the code executes on at runtime in a cluster
	 */
	public void reIndexForServerNode(); 
	
	/**
	 * Get all the files relates to the contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public void getRelatedFiles(Contentlet contentlet, User user, boolean respectFrontendRoles,List<File> returnValue);
	
	/**
	 * Gets a file with a specific relationship type to the passed in contentlet
	 * @param contentlet
	 * @param relationshipType
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public void getRelatedIdentifier(Contentlet contentlet, String relationshipType, User user, boolean respectFrontendRoles,Identifier returnValue);
	
	/**
	 * Gets all related links to the contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public void getRelatedLinks(Contentlet contentlet, User user, boolean respectFrontendRoles,List<Link> returnValue);
	
	/**
	 * Allows you to checkout a content so it can be altered and checked in
	 * @param contentletInode
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public void checkout(String contentletInode, User user, boolean respectFrontendRoles,Contentlet returnValue);
	
	/**
	 * Allows you to checkout contents so it can be altered and checked in 
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public void checkout(List<Contentlet> contentlets, User user, boolean respectFrontendRoles,List<Contentlet> returnValue);
	
	/**
	 * Allows you to checkout contents based on a lucene query so it can be altered and checked in 
	 * @param luceneQuery
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public void checkout(String luceneQuery, User user, boolean respectFrontendRoles, List<Contentlet> returnValue);
	
	/**
	 * Allows you to checkout contents based on a lucene query so it can be altered and checked in, in a paginated fashion 
	 * @param luceneQuery
	 * @param user
	 * @param respectFrontendRoles
	 * @param offset
	 * @param limit
	 * @param returnValue - value returned by primary API Method 
	 */
	public void checkout(String luceneQuery, User user, boolean respectFrontendRoles, int offset, int limit,List<Contentlet> returnValue);
	
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
	public void checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles,Contentlet returnValue);
	
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
	public void checkin(Contentlet currentContentlet, ContentletRelationships relationshipsData, List<Category> cats, List<Permission> selectedPermissions, User user,	boolean respectFrontendRoles,Contentlet returnValue);
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.  
	 * @param contentlet - The inode of your contentlet must be 0. 
	 * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 * @param permissions - throws IllegalArgumentException if null. Used to set permissions to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public void checkin(Contentlet contentlet, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles,Contentlet returnValue);
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.  
	 * @param contentlet - The inode of your contentlet must be 0. 
	 * @param permissions - throws IllegalArgumentException if null. Used to set permissions to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public void checkin(Contentlet contentlet, List<Permission> permissions, User user,boolean respectFrontendRoles,Contentlet returnValue);
	
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
	public void checkin(Contentlet contentlet ,User user,boolean respectFrontendRoles,List<Category> cats,Contentlet returnValue);
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.  
	 * @param contentlet - The inode of your contentlet must be 0.
	 * @param contentRelationships - throws IllegalArgumentException if null. Used to set relationships to new contentlet version 
	 * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public void checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats, User user,boolean respectFrontendRoles,Contentlet returnValue);
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.  
	 * @param contentlet - The inode of your contentlet must be 0.
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public void checkin(Contentlet contentlet, User user,boolean respectFrontendRoles, Contentlet returnValue);
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.  
	 * @param contentlet - The inode of your contentlet must be 0.
	 * @param contentRelationships - throws IllegalArgumentException if null. Used to set relationships to new contentlet version 
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public void checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, User user,boolean respectFrontendRoles,Contentlet returnValue);
	
	/**
	 * Will check in a update of your contentlet without generate a new version. The inode of your contentlet must be different from 0.  
	 * @param contentlet - The inode of your contentlet must be different from 0.
	 * @param contentRelationships - Used to set relationships to updated contentlet version 
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public void checkinWithoutVersioning(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles,Contentlet returnValue);
	
	/**
	 * Will make the passed in contentlet the working copy. 
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles 
	 */
	public void restoreVersion(Contentlet contentlet, User user, boolean respectFrontendRoles);
	
	/**
	 * Retrieves all versions for a contentlet identifier
	 * Note this method should not be used currently because it could pull too many versions. 
	 * @param identifier
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public void findAllVersions(Identifier identifier, User user, boolean respectFrontendRoles,List<Contentlet> returnValue);
	
	/**
	 * Retrieves all versions for a contentlet identifier checked in by a real user meaning not the system user
	 * @param identifier
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public void findAllUserVersions(Identifier identifier, User user, boolean respectFrontendRoles,List<Contentlet> returnValue);
	
	/**
	 * Meant to get the title or name of a contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public void getName(Contentlet contentlet, User user, boolean respectFrontendRoles,String returnValue);
	
	/**
	 * Copies properties from the map to the contentlet
	 * @param contentlet contentlet to copy to
	 * @param properties
	 */
	public void copyProperties(Contentlet contentlet, Map<String, Object> properties);
	
	/**
	 * Use to check if the inode id is a contentlet
	 * @param inode id to check
	 * @param returnValue - value returned by primary API Method 
	 */
	public void isContentlet(String inode,boolean returnValue);
	
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
	public void find(Category category, long languageId, boolean live, String orderBy, User user, boolean respectFrontendRoles,List<Contentlet> returnValue);

	/**
	 * Will return all content assigned to a specified Categories
	 * @param categories - List of categories to look for
	 * @param languageId language to pull content for. If 0 will return all languages
	 * @param category Category to look for
	 * @param live should return live or working content
	 * @param orderBy indexName(previously known as dbColumnName) to order by. Can be null or empty string
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public void find(List<Category> categories,long languageId, boolean live, String orderBy, User user, boolean respectFrontendRoles,List<Contentlet> returnValue);

	/**
	 * Use to set contentlet properties.  The value should be String, the proper type of the property
	 * @param contentlet
	 * @param field
	 * @param value
	 * @param user
	 * @param respectFrontendRoles
	 */
	public void setContentletProperty(Contentlet contentlet, Field field, Object value);
	
	/**
	 * Use to validate your contentlet.
	 * @param contentlet
	 * @param categories
	 */
	public void validateContentlet(Contentlet contentlet,List<Category> cats); 
	
	/**
	 * Use to validate your contentlet.
	 * @param contentlet
	 * @param contentRelationships
	 * @param categories
	 * Use the notValidFields property of the exception to get which fields where not valid
	 */
	public void validateContentlet(Contentlet contentlet,Map<Relationship, List<Contentlet>> contentRelationships,List<Category> cats); 
	
	/**
	 * Use to validate your contentlet.
	 * @param contentlet
	 * @param contentRelationships
	 * @param categories
	 */
	public void validateContentlet(Contentlet contentlet, ContentletRelationships contentRelationships, List<Category> cats); 
	
	/**
	 * Use to determine if if the field value is a String value withing the contentlet object
	 * @param field
	 */
	public void isFieldTypeString(Field field,boolean returnValue);
	
	/**
	 * Use to determine if if the field value is a Date value withing the contentlet object
	 * @param field
	 */
	public void isFieldTypeDate(Field field,boolean returnValue);
	
	/**
	 * Use to determine if if the field value is a Long value withing the contentlet object
	 * @param field
	 */
	public void isFieldTypeLong(Field field,boolean returnValue);
	
	/**
	 * Use to determine if if the field value is a Boolean value withing the contentlet object
	 * @param field
	 */
	public void isFieldTypeBoolean(Field field,boolean returnValue);
	
	/**
	 * Use to determine if if the field value is a Float value withing the contentlet object
	 * @param field
	 */
	public void isFieldTypeFloat(Field field,boolean returnValue);

	/**
	 * Converts a "fat" (legacy) contentlet into a new contentlet.
	 * @param Fat contentlet to be converted.
	 */
	public void convertFatContentletToContentlet (com.dotmarketing.portlets.contentlet.business.Contentlet fatty,Contentlet returnValue);
	
	/**
	 * Converts a "light" contentlet into a "fat" (legacy) contentlet.
	 * @param A "light" contentlet to be converted.
	 */
	public void convertContentletToFatContentlet (Contentlet cont, com.dotmarketing.portlets.contentlet.business.Contentlet fatty,com.dotmarketing.portlets.contentlet.business.Contentlet returnValue);
    
	/**
	 * Applies permission to the child contentlets of the structure
	 * @param structure
	 * @param user
	 * @param permissions
	 * @param respectFrontendRoles
	 */
	public void applyStructurePermissionsToChildren(Structure structure, User user, List<Permission> permissions, boolean respectFrontendRoles);
	
   /**
    * 
    * @param deleteFrom
    * @param offset
    * @return
    * @param returnValue - value returned by primary API Method */
	public void deleteOldContent(Date deleteFrom, int offset,int returnValue);
	
	/**

	 * 
	 * @param deleteFrom
	 * @param offset
	 * @param returnValue - value returned by primary API Method 
	 */
	public void findFieldValues(String structureInode, Field field, User user, boolean respectFrontEndRoles,List<String> returnValue);
	
	
	/**
	 * Fetches the File Name stored under the contentlet and field
	 * @param contentletInode
	 * @param velocityVariableName
	 * @param returnValue - value returned by primary API Method 
	 */
	public void getBinaryFile(String contentletInode,String velocityVariableName,User user,java.io.File returnValue);
	
	/**
	 * gets the number of contentlets in the system. This number includes all versions not distinct identifiers
	 * @param returnValue
	 * @return
	 * @throws DotDataException
	 */
	public long contentletCount(long returnValue) throws DotDataException;

	/**
	 * gets the number of contentlet identifiers in the system. This number includes all versions not distinct identifiers
	 * @param returnValue
	 * @return
	 * @throws DotDataException
	 */
	public long contentletIdentifierCount(long returnValue) throws DotDataException;
	
	public boolean removeContentletFromIndex(String contentletInodeOrIdentifier) throws DotDataException;

	public void refresh(Structure structure);

	public void refresh(Contentlet contentlet);

	public void refreshAllContent();

	public List<Contentlet> getSiblings(String identifier)throws DotDataException ;
	
	public void checkinWithNoIndex(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles,Contentlet returnValue);
	
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
	public void checkinWithNoIndex(Contentlet currentContentlet, ContentletRelationships relationshipsData, List<Category> cats, List<Permission> selectedPermissions, User user,	boolean respectFrontendRoles,Contentlet returnValue);
	
	/**
	 * Will check in a new version of you contentlet without indexing. The inode of your contentlet must be not set.   
	 * @param contentlet - The inode of your contentlet must be not set. 
	 * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 * @param permissions - throws IllegalArgumentException if null. Used to set permissions to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public void checkinWithNoIndex(Contentlet contentlet, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles,Contentlet returnValue);
	
	/**
	 * Will check in a new version of you contentlet without indexing. The inode of your contentlet must be not set.  
	 * @param contentlet - The inode of your contentlet must be not set. 
	 * @param permissions - throws IllegalArgumentException if null. Used to set permissions to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public void checkinWithNoIndex(Contentlet contentlet, List<Permission> permissions, User user,boolean respectFrontendRoles,Contentlet returnValue);
	
	/**
	 * Will check in a new version of you contentlet without indexing. The inode of your contentlet must be not set.  
	 * @param contentlet - The inode of your contentlet must be not set.
	 * @param contentRelationships - throws IllegalArgumentException if null. Used to set relationships to new contentlet version 
	 * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 * @param permissions - throws IllegalArgumentException if null. Used to set permissions to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public void checkinWithNoIndex(Contentlet contentlet ,User user,boolean respectFrontendRoles,List<Category> cats,Contentlet returnValue);
	
	/**
	 * Will check in a new version of you contentlet without indexing. The inode of your contentlet must be not set.  
	 * @param contentlet - The inode of your contentlet must be not set.
	 * @param contentRelationships - throws IllegalArgumentException if null. Used to set relationships to new contentlet version 
	 * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public void checkinWithNoIndex(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats, User user,boolean respectFrontendRoles,Contentlet returnValue);
	
	/**
	 * Will check in a new version of you contentlet without indexing. The inode of your contentlet must be not set.  
	 * @param contentlet - The inode of your contentlet must be not set.
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public void checkinWithNoIndex(Contentlet contentlet, User user,boolean respectFrontendRoles, Contentlet returnValue);
	
	/**
	 * Will check in a new version of you contentlet without indexing. The inode of your contentlet must be not set.   
	 * @param contentlet - The inode of your contentlet must be not set.
	 * @param contentRelationships - throws IllegalArgumentException if null. Used to set relationships to new contentlet version 
	 * @param user
	 * @param respectFrontendRoles
	 * @param returnValue - value returned by primary API Method 
	 */
	public void checkinWithNoIndex(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, User user,boolean respectFrontendRoles,Contentlet returnValue);
	
	/**
	 * 
	 * @param query
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws ValidationException
	 * @throws DotDataException
	 */
	public void DBSearch(Query query, User user,boolean respectFrontendRoles, List<Map<String, Serializable>> returnValue) throws ValidationException,DotDataException;
	
	
	/**
	 * Method will time out after 30 seconds returning false
	 * @param inode
	 * @return
	 */
	public void isInodeIndexed(String inode, boolean returnValue);

	/**
	 * Method will time out after 30 seconds returning false
	 * @param inode
	 * @param secondsToWait - how long to wait before timing out
	 * @return
	 */
	public void isInodeIndexed(String inode, int secondsToWait, boolean returnValue);
	
	/**
	 * Method will update hostInode of content to systemhost
	 * @param identifier
	 */	
	public void UpdateContentWithSystemHost(String hostIdentifier)throws DotDataException;
	
	/**
	 * Method will remove User References of the given userId in Contentlet  
	 * @param userId
	 */	
	public void removeUserReferences(String userId)throws DotDataException;
	
	/**
	 * Return the URL Map for the specified content if the structure associated to the content has the URL Map Pattern set.
	 * 
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public void getUrlMapForContentlet(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException;
	
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
	public void  saveDraft(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) throws IllegalArgumentException,DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException;
	
	
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
	 * @throws ParseException
	 */
	public void searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, ParseException;
	
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
	 * @throws ParseException
	 */
	public void searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission) throws DotDataException, DotSecurityException, ParseException;

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
	 * @throws ParseException
	 */
	public void searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission, boolean anyLanguage) throws DotDataException, DotSecurityException, ParseException;
	
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
	public void refreshContentUnderFolder(Folder folder) throws DotReindexStateException;
	
	/**
	 * Will update contents that reference the given folder to point to it's parent folder, if it's a top folder it will set folder to be SYSTEM_FOLDER
	 * @param folder
	 * @throws DotDataException
	 */
	public void removeFolderReferences(Folder folder) throws DotDataException;
	
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
	public boolean canLock(Contentlet contentlet, User user) throws   DotLockException;

    public void searchIndexCount(String luceneQuery, User user, boolean respectFrontendRoles, long c);
    
	/**
	 * Returns the ContentRelationships Map for the specified content.
	 * 
	 * @param contentlet
	 * @param user
	 * @return Map with the ContentRelationships. Empty Map if the content doesn't have associated relationships.
	 */
	
	public void findContentRelationships(Contentlet contentlet, User user) throws DotDataException, DotSecurityException;

    public void loadField(String inode, Field field, Object value);

    public void indexCount(String luceneQuery, User user,
            boolean respectFrontendRoles, long value);

}
