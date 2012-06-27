/**
 * 
 */
package com.dotmarketing.portlets.contentlet.business;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.lucene.queryParser.ParseException;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;

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
	public boolean findAllContent(int offset, int limit);
	
	/**
	 * Finds a Contentlet Object given the inode
	 * @param inode
	 * @return false if the hook should stop the transaction
	 */
	public boolean find(String inode, User user, boolean respectFrontendRoles);
	
	/**
	 * Finds a Contentlet Object given the inode
	 * @param inode
	 * @return
	 */
	//public boolean find(long inode, User user, boolean respectFrontendRoles);
	
	/**
	 * Returns a live Contentlet Object for a given language 
	 * @param languageId
	 * @param inode
	 * @return
	 */
	public boolean findContentletForLanguage(long languageId, Identifier contentletId);
	
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
	public boolean findByStructure(Structure structure, User user, boolean respectFrontendRoles, int limit, int offset);
	
	/**
	 * Returns all Contentlets for a specific structure
	 * @param structureInode
	 * @param user
	 * @param respectFrontendRoles
	 * @param limit
	 * @param offset
	 * @return
	 */
	public boolean findByStructure(String structureInode, User user, boolean respectFrontendRoles, int limit, int offset);
	
	/**
	 * Retrieves a contentlet from the database based on its identifier
	 * @param identifier 
	 * @param live Retrieves the live version if false retrieves the working version
	 * @return
	 */
	public boolean findContentletByIdentifier(String identifier, boolean live, long languageId, User user, boolean respectFrontendRoles);

	/**
	 * Retrieves a contentlet list from the database based on a identifiers array
	 * @param identifiers	Array of identifiers
	 * @param live	Retrieves the live version if false retrieves the working version
	 * @param languageId
	 * @param user	
	 * @param respectFrontendRoles	
	 * @return 
	 */
	public boolean findContentletsByIdentifiers(String[] identifiers, boolean live, long languageId, User user, boolean respectFrontendRoles);
		
	/**
	 * Gets a list of Contentlets from a passed in list of inodes.  
	 * @param inodes
	 * @return
	 */
	public boolean findContentlets(List<String> inodes) throws DotDataException;

	/**
	 * Gets a list of Contentlets from a given parent folder  
	 * @param parentFolder
	 * @return
	 */
	public boolean findContentletsByFolder(Folder parentFolder, User user, boolean respectFrontendRoles);

	/**
	 * Gets a list of Contentlets from a given parent host  
	 * @param parentHost
	 * @return
	 */
	public boolean findContentletsByHost(Host parentHost, User user, boolean respectFrontendRoles);

	/**
	 * Makes a copy of a contentlet. 
	 * @param currentContentlet
	 * @return
	 */
	public boolean copyContentlet(Contentlet currentContentlet, User user, boolean respectFrontendRoles);

	/**
	 * Makes a copy of a contentlet. 
	 * @param currentContentlet
	 * @return
	 */
	public boolean copyContentlet(Contentlet currentContentlet, Host host, User user, boolean respectFrontendRoles);

	/**
	 * Makes a copy of a contentlet. 
	 * @param currentContentlet
	 * @return
	 */
	public boolean copyContentlet(Contentlet currentContentlet, Folder folder, User user, boolean respectFrontendRoles);

	/**
	 * Makes a copy of a contentlet with choice to append copy to the filename. 
	 * @param currentContentlet
	 * @return
	 */
	public boolean copyContentlet(Contentlet currentContentlet, Folder folder, User user, boolean appendCopyToFileName, boolean respectFrontendRoles);
	
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
	public boolean search(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles);
	
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
	public boolean search(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission);

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

	public boolean searchIndex(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles);
	
	/**
	 * Publishes all related HTMLPage
	 * @param contentlet
	 */
	public boolean publishRelatedHtmlPages(Contentlet contentlet);
	
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
	public boolean cleanField(Structure structure, Field field, User user, boolean respectFrontendRoles);

	/**
	 * Will get all the contentlets for a structure and set the default values for the host fields  
	 * Will check Write/Edit permissions on the Contentlet. So to guarantee all COntentlets will be cleaned make 
	 * sure to pass in an Admin User.  If a user doesn't have permissions to clean all teh contentlets it will clean 
	 * as many as it can and throw the DotSecurityException  
	 * @param structure
	 * @param field
	 * @param user
	 * @param respectFrontendRoles
	 */
	public boolean cleanHostField(Structure structure, User user, boolean respectFrontendRoles);
	
	/**
	 * Finds the next date that a contentlet must be reviewed
	 * @param content 
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public boolean getNextReview(Contentlet content, User user, boolean respectFrontendRoles);

	/**
	 * Retrieves all references for a Contentlet. The result is an ArrayList of type Map whose key will 
	 * be page or container with the respective object as the value.  
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public boolean getContentletReferences(Contentlet contentlet, User user, boolean respectFrontendRoles);
	
	/**
	 * Gets the value of a field with a given contentlet 
	 * @param contentlet
	 * @param theField
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public boolean getFieldValue(Contentlet contentlet, Field theField);

	/**
	 * Adds a relationship to a contentlet
	 * @param contentlet
	 * @param linkInode
	 * @param relationName
	 * @param user
	 * @param respectFrontendRoles
	 */
	public boolean addLinkToContentlet(Contentlet contentlet, String linkInode, String relationName, User user, boolean respectFrontendRoles);
	
	/**
	 * Adds a relationship to a contentlet
	 * @param contentlet
	 * @param fileInode
	 * @param relationName
	 * @param user
	 * @param respectFrontendRoles
	 */
	public boolean addFileToContentlet(Contentlet contentlet, String fileInode, String relationName, User user, boolean respectFrontendRoles);
	
	/**
	 * Adds a relationship to a contentlet
	 * @param contentlet
	 * @param imageInode
	 * @param relationName
	 * @param user
	 * @param respectFrontendRoles
	 */
	public boolean addImageToContentlet(Contentlet contentlet, String imageInode, String relationName, User user, boolean respectFrontendRoles);

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
	public boolean findPageContentlets(String HTMLPageIdentifier, String containerIdentifier, String orderby, boolean working, long languageId, User user, boolean respectFrontendRoles);

	/**
	 * Returns all contentlet's relationships for a given contentlet inode 
	 * @param contentletInode
	 * @return a ContentletRelationships object containing all relationships for the contentlet
	 */
	public boolean getAllRelationships(String contentletInode, User user, boolean respectFrontendRoles);

	/**
	 * Returns all contentlet's relationships for a given contentlet object 
	 * @param contentlet
	 * @return a ContentletRelationships object containing all relationships for the contentlet
	 */
	public boolean getAllRelationships(Contentlet contentlet);


	/**
	 * Returns a contentlet's siblings for a given contentlet object.
	 * @param contentlet
	 * @return a ContentletRelationships object containing all relationships for the contentlet
	 */
	public boolean getAllLanguages(Contentlet contentlet, Boolean isLiveContent, User user, boolean respectFrontendRoles);
	


	/**
	 * 
	 * @param contentlet1
	 * @param contentlet2
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public boolean isContentEqual(Contentlet contentlet1,Contentlet contentlet2, User user, boolean respectFrontendRoles);

	/**
	 * This method archives the given contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 */
	public boolean archive(Contentlet contentlet, User user, boolean respectFrontendRoles);

	

	/**
	 * This method completely deletes the given contentlet from the system
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 */
	public boolean delete(Contentlet contentlet, User user, boolean respectFrontendRoles);
	
	
	/**
	 * This method completely deletes the given contentlet from the system. It was added for the jira issue
	 * http://jira.dotmarketing.net/browse/DOTCMS-2059
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @param allVersions
	 */
	public boolean delete(Contentlet contentlet, User user, boolean respectFrontendRoles, boolean allVersions);
	/**
	 * Publishes a piece of content. 
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public boolean publish(Contentlet contentlet, User user, boolean respectFrontendRoles);
	
	/**
	 * Publishes a piece of content. 
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public boolean publish(List<Contentlet> contentlets, User user, boolean respectFrontendRoles);

	/**
	 * This method unpublishes the given contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 */
	public boolean unpublish(Contentlet contentlet, User user, boolean respectFrontendRoles);
	
	/**
	 * This method unpublishes the given contentlet
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 */
	public boolean unpublish(List<Contentlet> contentlets, User user, boolean respectFrontendRoles);

	/**
	 * This method archives the given contentlets
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 */
	public boolean archive(List<Contentlet> contentlets, User user, boolean respectFrontendRoles);
	/**
	 * This method unarchives the given contentlets
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 */
	public boolean unarchive(List<Contentlet> contentlets, User user, boolean respectFrontendRoles);

	/**
	 * This method unarchives the given contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 */
	public boolean unarchive(Contentlet contentlet, User user, boolean respectFrontendRoles);
	
	/**
	 * This method completely deletes the given contentlet from the system
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 */
	public boolean delete(List<Contentlet> contentlets, User user, boolean respectFrontendRoles);
	
	
	/**
	 * This method completely deletes the given contentlet from the system. It was added for the jira issue
	 * http://jira.dotmarketing.net/browse/DOTCMS-2059
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 * @param allVersions
	 */
	public boolean delete(List<Contentlet> contentlets, User user, boolean respectFrontendRoles, boolean allVersions);
	
	/**
	 * Deletes all related content from passed in contentlet and relationship 
	 * @param contentlet
	 * @param relationship
	 * @param user
	 * @param respectFrontendRoles
	 */
	public boolean deleteRelatedContent(Contentlet contentlet, Relationship relationship, User user, boolean respectFrontendRoles);
	
	/**
	 * Deletes all related content from passed in contentlet and relationship 
	 * @param contentlet
	 * @param relationship
	 * @param user
	 * @param respectFrontendRoles
	 */
	public boolean deleteRelatedContent(Contentlet contentlet, Relationship relationship, boolean hasParent, User user, boolean respectFrontendRoles);
	
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
	public boolean relateContent(Contentlet contentlet, Relationship rel,List<Contentlet> related, User user, boolean respectFrontendRoles);

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
	public boolean relateContent(Contentlet contentlet, ContentletRelationshipRecords related, User user, boolean respectFrontendRoles);

	/**
	 * Gets all related content, if this method is invoked with a same structures (where the parent and child structures are the same type) 
	 * kind of relationship then all parents and children of the given contentlet will be retrieved in the same returned list
	 * @param contentlet
	 * @param rel
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public boolean getRelatedContent(Contentlet contentlet, Relationship rel, User user, boolean respectFrontendRoles);

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
	 * @return
	 */
	public boolean getRelatedContent(Contentlet contentlet, Relationship rel, boolean pullByParent, User user, boolean respectFrontendRoles);

	/**
	 * Gets all contents referenced by a given file asset
	 * @param file asset.
	 * @param live contentlets or not.
	 * @param user
	 * @param respectFrontendRoles
	 * @return List of contentlets. Null if no related contentlets found.
	 */
	public boolean getReferencingContentlet(File file, boolean live, User user, boolean respectFrontendRoles);
	
	/**
	 * Refreshes (regenerates) all content files referenced by a given file asset
	 * @param file asset
	 * @param live contentlets or not
	 * @return 
	 */
	public boolean refreshReferencingContentlets(File file, boolean live);

	/**
	 * 
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 */
	public boolean unlock(Contentlet contentlet, User user, boolean respectFrontendRoles);

	/**
	 * Use to lock a contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 */
	public boolean lock(Contentlet contentlet, User user, boolean respectFrontendRoles);
	
	/**
	 * Reindex all content
	 */
	public boolean reindex();
	
	/**
	 * reindex content for a given structure
	 * @param structure
	 */
	public boolean reindex(Structure structure);
	
	/**
	 * reindex a single content
	 * @param contentlet
	 */
	public boolean reindex(Contentlet contentlet);
	
	/**
	 * Used to reindex content for the specific server the code executes on at runtime in a cluster
	 * @throws DotDataException
	 */
	public boolean reIndexForServerNode(); 
	
	/**
	 * Get all the files relates to the contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public boolean getRelatedFiles(Contentlet contentlet, User user, boolean respectFrontendRoles);
	
	/**
	 * Gets a file with a specific relationship type to the passed in contentlet
	 * @param contentlet
	 * @param relationshipType
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public boolean getRelatedIdentifier(Contentlet contentlet, String relationshipType, User user, boolean respectFrontendRoles);
	
	/**
	 * Gets all related links to the contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public boolean getRelatedLinks(Contentlet contentlet, User user, boolean respectFrontendRoles);
	
	/**
	 * Allows you to checkout a content so it can be altered and checked in
	 * @param contentletInode
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public boolean checkout(String contentletInode, User user, boolean respectFrontendRoles);
	
	/**
	 * Allows you to checkout contents so it can be altered and checked in 
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public boolean checkout(List<Contentlet> contentlets, User user, boolean respectFrontendRoles);
	
	/**
	 * Allows you to checkout contents based on a lucene query so it can be altered and checked in 
	 * @param luceneQuery
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws ParseException 
	 */
	public boolean checkoutWithQuery(String luceneQuery, User user, boolean respectFrontendRoles);
	
	/**
	 * Allows you to checkout contents based on a lucene query so it can be altered and checked in, in a paginated fashion 
	 * @param luceneQuery
	 * @param user
	 * @param respectFrontendRoles
	 * @param offset
	 * @param limit
	 * @return
	 * @throws ParseException 
	 */
	public boolean checkout(String luceneQuery, User user, boolean respectFrontendRoles, int offset, int limit);
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.  
	 * @param contentlet - The inode of your contentlet must be 0.
	 * @param contentRelationships - throws IllegalArgumentException if null. Used to set relationships to new contentlet version 
	 * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 * @param permissions - throws IllegalArgumentException if null. Used to set permissions to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 */
	public boolean checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles);
	
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
	public boolean checkin(Contentlet currentContentlet, ContentletRelationships relationshipsData, List<Category> cats, List<Permission> selectedPermissions, User user,	boolean respectFrontendRoles);
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.  
	 * @param contentlet - The inode of your contentlet must be 0. 
	 * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 * @param permissions - throws IllegalArgumentException if null. Used to set permissions to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 */
	public boolean checkin(Contentlet contentlet, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles);
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.  
	 * @param contentlet - The inode of your contentlet must be 0. 
	 * @param permissions - throws IllegalArgumentException if null. Used to set permissions to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 */
	public boolean checkin(Contentlet contentlet, List<Permission> permissions, User user,boolean respectFrontendRoles);
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.  
	 * @param contentlet - The inode of your contentlet must be 0.
	 * @param contentRelationships - throws IllegalArgumentException if null. Used to set relationships to new contentlet version 
	 * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 * @param permissions - throws IllegalArgumentException if null. Used to set permissions to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 */
	public boolean checkin(Contentlet contentlet ,User user,boolean respectFrontendRoles,List<Category> cats);
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.  
	 * @param contentlet - The inode of your contentlet must be 0.
	 * @param contentRelationships - throws IllegalArgumentException if null. Used to set relationships to new contentlet version 
	 * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 */
	public boolean checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats, User user,boolean respectFrontendRoles);
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.  
	 * @param contentlet - The inode of your contentlet must be 0.
	 * @param user
	 * @param respectFrontendRoles
	 */
	public boolean checkin(Contentlet contentlet, User user,boolean respectFrontendRoles);
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.  
	 * @param contentlet - The inode of your contentlet must be 0.
	 * @param contentRelationships - throws IllegalArgumentException if null. Used to set relationships to new contentlet version 
	 * @param user
	 * @param respectFrontendRoles
	 */
	public boolean checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, User user,boolean respectFrontendRoles);
	
	/**
	 * Will check in a update of your contentlet without generate a new version. The inode of your contentlet must be different from 0.  
	 * @param contentlet - The inode of your contentlet must be different from 0.
	 * @param contentRelationships - Used to set relationships to updated contentlet version 
	 * @param user
	 * @param respectFrontendRoles
	 */
	public boolean checkinWithoutVersioning(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles);
	
	/**
	 * Will make the passed in contentlet the working copy. 
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 */
	public boolean restoreVersion(Contentlet contentlet, User user, boolean respectFrontendRoles);
	
	/**
	 * Retrieves all versions for a contentlet identifier
	 * Note this method should not be used currently because it could pull too many versions. 
	 * @param identifier
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public boolean findAllVersions(Identifier identifier, User user, boolean respectFrontendRoles);
	
	/**
	 * Retrieves all versions for a contentlet identifier checked in by a real user meaning not the system user
	 * @param identifier
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public boolean findAllUserVersions(Identifier identifier, User user, boolean respectFrontendRoles);
	
	/**
	 * Meant to get the title or name of a contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public boolean getName(Contentlet contentlet, User user, boolean respectFrontendRoles);
	
	/**
	 * Copies properties from the map to the contentlet
	 * @param contentlet contentlet to copy to
	 * @param properties
	 */
	public boolean copyProperties(Contentlet contentlet, Map<String, Object> properties);
	
	/**
	 * Use to check if the inode id is a contentlet
	 * @param inode id to check
	 * @return
	 */
	public boolean isContentlet(String inode);
	
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
	public boolean find(Category category, long languageId, boolean live, String orderBy, User user, boolean respectFrontendRoles);

	/**
	 * Will return all content assigned to a specified Categories
	 * @param categories - List of categories to look for
	 * @param languageId language to pull content for. If 0 will return all languages
	 * @param category Category to look for
	 * @param live should return live or working content
	 * @param orderBy indexName(previously known as dbColumnName) to order by. Can be null or empty string
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public boolean find(List<Category> categories,long languageId, boolean live, String orderBy, User user, boolean respectFrontendRoles);

	/**
	 * Use to set contentlet properties.  The value should be String, the proper type of the property
	 * @param contentlet
	 * @param field
	 * @param value
	 * @param user
	 * @param respectFrontendRoles
	 */
	public boolean setContentletProperty(Contentlet contentlet, Field field, Object value);
	
	/**
	 * Use to validate your contentlet.
	 * @param contentlet
	 * @param categories
	 */
	public boolean validateContentlet(Contentlet contentlet,List<Category> cats); 
	
	/**
	 * Use to validate your contentlet.
	 * @param contentlet
	 * @param contentRelationships
	 * @param categories
	 * Use the notValidFields property of the exception to get which fields where not valid
	 */
	public boolean validateContentlet(Contentlet contentlet,Map<Relationship, List<Contentlet>> contentRelationships,List<Category> cats); 
	
	/**
	 * Use to validate your contentlet.
	 * @param contentlet
	 * @param contentRelationships
	 * @param categories
	 */
	public boolean validateContentlet(Contentlet contentlet, ContentletRelationships contentRelationships, List<Category> cats); 
	
	/**
	 * Use to determine if if the field value is a String value withing the contentlet object
	 * @param field
	 * @return
	 */
	public boolean isFieldTypeString(Field field);
	
	/**
	 * Use to determine if if the field value is a Date value withing the contentlet object
	 * @param field
	 * @return
	 */
	public boolean isFieldTypeDate(Field field);
	
	/**
	 * Use to determine if if the field value is a Long value withing the contentlet object
	 * @param field
	 * @return
	 */
	public boolean isFieldTypeLong(Field field);
	
	/**
	 * Use to determine if if the field value is a Boolean value withing the contentlet object
	 * @param field
	 * @return
	 */
	public boolean isFieldTypeBoolean(Field field);
	
	/**
	 * Use to determine if if the field value is a Float value withing the contentlet object
	 * @param field
	 * @return
	 */
	public boolean isFieldTypeFloat(Field field);

	/**
	 * Converts a "fat" (legacy) contentlet into a new contentlet.
	 * @param Fat contentlet to be converted.
	 * @return
	 */
	public boolean convertFatContentletToContentlet (com.dotmarketing.portlets.contentlet.business.Contentlet fatty);
	
	/**
	 * Converts a "light" contentlet into a "fat" (legacy) contentlet.
	 * @param A "light" contentlet to be converted.
	 * @return
	 */
	public boolean convertContentletToFatContentlet (Contentlet cont, com.dotmarketing.portlets.contentlet.business.Contentlet fatty);
    
	/**
	 * Applies permission to the child contentlets of the structure
	 * @param structure
	 * @param user
	 * @param permissions
	 * @param respectFrontendRoles
	 */
	public boolean applyStructurePermissionsToChildren(Structure structure, User user, List<Permission> permissions, boolean respectFrontendRoles);
	
	
   /**
    * 
    * @param deleteFrom
    * @param offset
    * @return
    */
	public boolean deleteOldContent(Date deleteFrom, int offset);
	
	/**

	 * 
	 * @param deleteFrom
	 * @param offset
	 * @return
	 */
	public boolean findFieldValues(String structureInode, Field field, User user, boolean respectFrontEndRoles);
	
	
	/**
	 * Fetches the File Name stored under the contentlet and field
	 * @param contentletInode
	 * @param velocityVariableName
	 * @return 
	 */
	public boolean getBinaryFile(String contentletInode,String velocityVariableName,User user);

	/**
	 * gets the number of contentlets in the system. This number includes all versions not distinct identifiers
	 * @return
	 * @throws DotDataException
	 */
	public boolean contentletCount() throws DotDataException;
	
	/**
	 * gets the number of contentlet identifiers in the system. This number includes all versions not distinct identifiers
	 * @return
	 * @throws DotDataException
	 */
	public boolean contentletIdentifierCount() throws DotDataException;
	
	public boolean removeContentletFromIndex(String contentletInodeOrIdentifier) throws DotDataException;

	public boolean refresh(Structure structure);

	public boolean refresh(Contentlet content);
	
	public boolean refreshAllContent();
	
	public boolean getSiblings(String identifier)throws DotDataException ;
	
    public boolean checkinWithNoIndex(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles);
	
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
	public boolean checkinWithNoIndex(Contentlet currentContentlet, ContentletRelationships relationshipsData, List<Category> cats, List<Permission> selectedPermissions, User user,	boolean respectFrontendRoles);
	
	/**
	 * Will check in a new version of you contentlet without indexing. The inode of your contentlet must be not set.  
	 * @param contentlet - The inode of your contentlet must be not set. 
	 * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 * @param permissions - throws IllegalArgumentException if null. Used to set permissions to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 */
	public boolean checkinWithNoIndex(Contentlet contentlet, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles);
	
	/**
	 * Will check in a new version of you contentlet without indexing. The inode of your contentlet must be not set.  
	 * @param contentlet - The inode of your contentlet must be not set. 
	 * @param permissions - throws IllegalArgumentException if null. Used to set permissions to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 */
	public boolean checkinWithNoIndex(Contentlet contentlet, List<Permission> permissions, User user,boolean respectFrontendRoles);
	
	/**
	 * Will check in a new version of you contentlet without indexing. The inode of your contentlet must be not set.  
	 * @param contentlet - The inode of your contentlet must be not set.
	 * @param contentRelationships - throws IllegalArgumentException if null. Used to set relationships to new contentlet version 
	 * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 * @param permissions - throws IllegalArgumentException if null. Used to set permissions to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 */
	public boolean checkinWithNoIndex(Contentlet contentlet ,User user,boolean respectFrontendRoles,List<Category> cats);
	
	/**
	 * Will check in a new version of you contentlet without indexing. The inode of your contentlet must be not set.  
	 * @param contentlet - The inode of your contentlet must be not set.
	 * @param contentRelationships - throws IllegalArgumentException if null. Used to set relationships to new contentlet version 
	 * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 */
	public boolean checkinWithNoIndex(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats, User user,boolean respectFrontendRoles);
	
	/**
	 * Will check in a new version of you contentlet without indexing. The inode of your contentlet must be not set.  
	 * @param contentlet - The inode of your contentlet must be not set.
	 * @param user
	 * @param respectFrontendRoles
	 */
	public boolean checkinWithNoIndex(Contentlet contentlet, User user,boolean respectFrontendRoles);
	
	/**
	 * Will check in a new version of you contentlet without indexing The inode of your contentlet must be not set.  
	 * @param contentlet - The inode of your contentlet must be not set.
	 * @param contentRelationships - throws IllegalArgumentException if null. Used to set relationships to new contentlet version 
	 * @param user
	 * @param respectFrontendRoles
	 */
	public boolean checkinWithNoIndex(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, User user,boolean respectFrontendRoles);

	/**
	 * 
	 * @param query
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws ValidationException
	 * @throws DotDataException
	 */
	public boolean DBSearch(Query query, User user,boolean respectFrontendRoles) throws ValidationException,DotDataException;

	
	/**
	 * Method will time out after 30 seconds returning false
	 * @param inode
	 * @return
	 */
	public boolean isInodeIndexed(String inode);

	/**
	 * Method will time out after 30 seconds returning false
	 * @param inode
	 * @param secondsToWait - how long to wait before timing out
	 * @return
	 */
	public boolean isInodeIndexed(String inode, int secondsToWait);
	/**
	 * Method will update hostInode of content to systemhost
	 * @param identifier
	 */	
	public boolean UpdateContentWithSystemHost(String hostIdentifier)throws DotDataException;
	/**
	 * Method will remove User References of the given userId in Contentlet  
	 * @param userId
	 */	
	public boolean removeUserReferences(String userId)throws DotDataException;

	/**
	 * Return the URL Map for the specified content if the structure associated to the content has the URL Map Pattern set.
	 * 
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public boolean getUrlMapForContentlet(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException;
	
	/**
	 * Deletes the given version of the contentlet from the system
	 * 
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 */
	public boolean deleteVersion(Contentlet contentlet, User user,	boolean respectFrontendRoles) throws DotDataException,DotSecurityException;


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
	public boolean  saveDraft(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) throws IllegalArgumentException,DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException;

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
	public boolean searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, ParseException;
	
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
	public boolean searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission) throws DotDataException, DotSecurityException, ParseException;

	
	/**
	 * Reindexes content under a given host + refreshes the content from cache
	 * @param host
	 * @return 
	 * @throws DotReindexStateException
	 */
	public boolean refreshContentUnderHost(Host host)throws DotReindexStateException;
	
	/**
	 * Reindexes content under a given folder + refreshes the content from cache
	 * @param folder
	 * @return 
	 * @throws DotReindexStateException
	 */
	public boolean refreshContentUnderFolder(Folder folder)throws DotReindexStateException;
	
	/**
	 * Will update contents that reference the given folder to point to it's parent folder, if it's a top folder it will set folder to be SYSTEM_FOLDER
	 * @param folder
	 * @throws DotDataException
	 */
	public boolean removeFolderReferences(Folder folder) throws DotDataException;


	public boolean canLock(Contentlet contentlet, User user) throws   DotLockException;

    public boolean searchIndexCount(String luceneQuery, User user, boolean respectFrontendRoles);
    
	/**
	 * Returns the ContentRelationships Map for the specified content.
	 * 
	 * @param contentlet
	 * @param user
	 * @return Map with the ContentRelationships. Empty Map if the content doesn't have associated relationships.
	 */
	
	public boolean findContentRelationships(Contentlet contentlet, User user) throws DotDataException, DotSecurityException;
	
	public boolean loadField(String inode, Field field) throws DotDataException;

    public boolean indexCount(String luceneQuery, User user,
            boolean respectFrontendRoles);
}
