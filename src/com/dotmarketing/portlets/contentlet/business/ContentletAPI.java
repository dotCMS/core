/**
 * 
 */
package com.dotmarketing.portlets.contentlet.business;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.lucene.queryParser.ParseException;

import com.dotcms.content.business.DotMappingException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
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
 *
 */

public interface ContentletAPI {

	/**
	 * Use to retrieve all version of all content in the database.  This is not a common method to use. 
	 * Only use if you need to do maintenance tasks like search and replace something in every piece 
	 * of content.  Doesn't respect permissions.
	 * @param offset can be 0 if no offset
	 * @param limit can be 0 of no limit
	 * @return
	 * @throws DotDataException
	 */
	public List<Contentlet> findAllContent(int offset, int limit) throws DotDataException;
	
	/**
	 * Finds a Contentlet Object given the inode
	 * @param inode
	 * @return
	 * @throws DotDataException
	 */
	public Contentlet find(String inode, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * Finds a Contentlet Object given the inode
	 * @param inode
	 * @return
	 * @throws DotDataException
	 */
	//http://jira.dotmarketing.net/browse/DOTCMS-2143
	//public Contentlet find(long inode, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	
	/**
	 * Returns a live Contentlet Object for a given language 
	 * @param languageId
	 * @param inode
	 * @return
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
	 * @return
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
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<Contentlet> findByStructure(String structureInode, User user, boolean respectFrontendRoles, int limit, int offset) throws DotDataException, DotSecurityException;
	
	/**
	 * Retrieves a contentlet from the Lucene index + cache first, then falls back
	 * to the database if not found based on its identifier
	 * @param identifier 
	 * @param live Retrieves the live version if false retrieves the working version
	 * @return
	 * @throws DotSecurityException
	 * @throws DotContentletStateException
	 * @throws DotDataException 
	 */
	public Contentlet findContentletByIdentifier(String identifier, boolean live, long languageId, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException;
	
	/**
	 * Retrieves a contentlet list from the database based on a identifiers array
	 * @param identifiers	Array of identifiers
	 * @param live	Retrieves the live version if false retrieves the working version
	 * @param languageId
	 * @param user	
	 * @param respectFrontendRoles	
	 * @return List<Contentlet>
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException
	 */
	public List<Contentlet> findContentletsByIdentifiers(String[] identifiers, boolean live, long languageId, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException;
		
	/**
	 * Gets a list of Contentlets from a passed in list of inodes.  
	 * @param inodes
	 * @return
	 * @throws DotSecurityException 
	 */
	public List<Contentlet> findContentlets(List<String> inodes) throws DotDataException, DotSecurityException;
	
	/**
	 * Gets a list of Contentlets from a given parent folder  
	 * @param parentFolder
	 * @return
	 * @throws DotSecurityException 
	 */
	public List<Contentlet> findContentletsByFolder(Folder parentFolder, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Gets a list of Contentlets from a given parent host, retrieves the working version of content
	 * @param parentHost
	 * @return
	 * @throws DotSecurityException 
	 */
	public List<Contentlet> findContentletsByHost(Host parentHost, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

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
	 * @throws ParseException
	 */
	public List<Contentlet> search(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission) throws DotDataException, DotSecurityException, ParseException;

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
	 * @throws ParseException
	 * @throws DotSecurityException if user is null and respectFrontendRoles is false
	 * @throws DotDataException 
	 */

	public List<ContentletSearch> searchIndex(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles) throws ParseException, DotSecurityException, DotDataException;
	
	/**
	 * Publishes all related HTMLPage
	 * @param contentlet
	 * @throws DotDataException 
	 * @throws DotStateException 
	 */
	public void publishRelatedHtmlPages(Contentlet contentlet) throws DotStateException, DotDataException;
	
	/**
	 * Will get all the contentlets for a structure and set the default values for a field on the contentlet.  
	 * Will check Write/Edit permissions on the Contentlet. So to guarantee all COntentlets will be cleaned make 
	 * sure to pass in an Admin User.  If a user doesn't have permissions to clean all teh contentlets it will clean 
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
	 * Will get all the contentlets for a structure and set the system host and system folder for the host values
	 * Will check Write/Edit permissions on the Contentlet. So to guarantee all COntentlets will be cleaned make 
	 * sure to pass in an Admin User.  If a user doesn't have permissions to clean all teh contentlets it will clean 
	 * as many as it can and throw the DotSecurityException  
	 * @param structure
	 * @param field
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
	 * @return
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
	 * @throws DotContentletStateException - if teh contentlet is null or has an inode of 0 
	 */
	public List<Map<String, Object>> getContentletReferences(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException, DotContentletStateException;
	
	/**
	 * Gets the value of a field with a given contentlet 
	 * @param contentlet
	 * @param theField
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public Object getFieldValue(Contentlet contentlet, Field theField);

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
	 * Adds a relationship to a contentlet
	 * @param contentlet
	 * @param fileInode
	 * @param relationName
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotDataException 
	 * @throws DotSecurityExceptionlanguageId
	 */
	public void addFileToContentlet(Contentlet contentlet, String fileInode, String relationName, User user, boolean respectFrontendRoles)throws DotSecurityException, DotDataException;
	
	/**
	 * Adds a relationship to a contentlet
	 * @param contentlet
	 * @param imageInode
	 * @param relationName
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotSecurityException
	 * @throws DotDataException 
	 */
	public void addImageToContentlet(Contentlet contentlet, String imageInode, String relationName, User user, boolean respectFrontendRoles)throws DotSecurityException, DotDataException;

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
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public List<Contentlet> findPageContentlets(String HTMLPageIdentifier, String containerIdentifier, String orderby, boolean working, long languageId, User user, boolean respectFrontendRoles)throws DotSecurityException, DotDataException;

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
	 * This method completely deletes the given contentlet from the system
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public void delete(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotDataException,DotSecurityException, DotContentletStateException;
	
	
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
	public void delete(Contentlet contentlet, User user, boolean respectFrontendRoles, boolean allVersions) throws DotDataException,DotSecurityException, DotContentletStateException;
	/**
	 * Publishes a piece of content. 
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @return
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
	 * @return
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
	 * @throws DotContentletStateException if the contentent cannot be unlocked by the user
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
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public void delete(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) throws DotDataException,DotSecurityException, DotContentletStateException;
	
	
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
	 * Associates the given list of contentlets using the relationship this
	 * methods removes old associated content and reset the relatioships based
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
	 * methods removes old associated content and reset the relatioships based
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
	public void relateContent(Contentlet contentlet, ContentletRelationshipRecords related, User user, boolean respectFrontendRoles)throws DotDataException, DotSecurityException, DotContentletStateException;

	/**
	 * Gets all related content, if this method is invoked with a same structures (where the parent and child structures are the same type) 
	 * kind of relationship then all parents and children of the given contentlet will be retrieved in the same returned list
	 * @param contentlet
	 * @param rel
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<Contentlet> getRelatedContent(Contentlet contentlet, Relationship rel, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

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
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<Contentlet> getRelatedContent(Contentlet contentlet, Relationship rel, boolean pullByParent, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

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
	 * @deprecated @see refreshAllContent
	 */
	public void reindex()throws DotReindexStateException;
	
	/**
	 * reindex content for a given structure
	 * @param structure
	 * @throws DotReindexStateException
	 * @deprecated @see refresh
	 */
	public void reindex(Structure structure)throws DotReindexStateException;
	
	/**
	 * reindex a single content
	 * @param contentlet
	 * @throws DotReindexStateException
	 * @throws DotDataException 
	 * @deprecated @see refresh
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
	 * @param host
	 * @throws DotReindexStateException
	 */
	public void refreshContentUnderFolder(Folder folder)throws DotReindexStateException;
	
	/**
	 * Get all the files relates to the contentlet
	 * @param contentlet
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<File> getRelatedFiles(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
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
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException if contentlet is not already persisted
	 */
	public Contentlet checkout(String contentletInode, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException;
	
	/**
	 * Allows you to checkout contents so it can be altered and checked in.
	 * Note that this method is only intended for use with Checkin methods.
	 * Methods like publish, archive, unpublish,.. will fail when passing
	 * a contentlet returned by this method. 
	 * @param contentlets
	 * @param user
	 * @param respectFrontendRoles
	 * @return
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
	 * @throws ParseException 
	 */
	public List<Contentlet> checkoutWithQuery(String luceneQuery, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException, ParseException;
	
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
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException
	 * @throws ParseException 
	 */
	public List<Contentlet> checkout(String luceneQuery, User user, boolean respectFrontendRoles, int offset, int limit) throws DotDataException, DotSecurityException, DotContentletStateException, ParseException;
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.
	 * Note that the contentlet argument must be obtained using checkout methods.  
	 * @param contentlet - The inode of your contentlet must be 0.
	 * @param contentRelationships - throws IllegalArgumentException if null. Used to set relationships to new contentlet version 
	 * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 * @param permissions - throws IllegalArgumentException if null. Used to set permissions to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 * @throws IllegalArgumentException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException If inode not = to 0
	 * @throws DotContentletValidationException If content is not valid
	 */
	public Contentlet checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) throws IllegalArgumentException,DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException;
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.  
	 * This version of checkin contains a more complex structure to pass the relationship in order
	 * to handle a same structures (where the parent and child structures are the same) kind of relationships
	 * in that case you have to specify if the role of the content is the parent of the child of the relatioship.
	 * Note that the contentlet argument must be obtained using checkout methods.
	 * @param currentContentlet - The inode of your contentlet must be 0.
	 * @param relationshipsData - 
	 * @param cats
	 * @param selectedPermissions
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public Contentlet checkin(Contentlet currentContentlet, ContentletRelationships relationshipsData, List<Category> cats, List<Permission> selectedPermissions, User user,	boolean respectFrontendRoles)  throws IllegalArgumentException,DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException;
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.  
	 * Note that the contentlet argument must be obtained using checkout methods.
	 * @param contentlet - The inode of your contentlet must be 0. 
	 * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 * @param permissions - throws IllegalArgumentException if null. Used to set permissions to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 * @throws IllegalArgumentException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException If inode not = to 0
	 * @throws DotContentletValidationException If content is not valid
	 */
	public Contentlet checkin(Contentlet contentlet, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) throws IllegalArgumentException,DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException;
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.
	 * Note that the contentlet argument must be obtained using checkout methods.  
	 * @param contentlet - The inode of your contentlet must be 0. 
	 * @param permissions - throws IllegalArgumentException if null. Used to set permissions to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 * @throws IllegalArgumentException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException If inode not = to 0
	 * @throws DotContentletValidationException If content is not valid
	 */
	public Contentlet checkin(Contentlet contentlet, List<Permission> permissions, User user,boolean respectFrontendRoles) throws IllegalArgumentException,DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException;
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.
	 * Note that the contentlet argument must be obtained using checkout methods.  
	 * @param contentlet - The inode of your contentlet must be 0.
	 * @param contentRelationships - throws IllegalArgumentException if null. Used to set relationships to new contentlet version 
	 * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 * @param permissions - throws IllegalArgumentException if null. Used to set permissions to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 * @throws IllegalArgumentException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException If inode not = to 0
	 * @throws DotContentletValidationException If content is not valid
	 */
	public Contentlet checkin(Contentlet contentlet ,User user,boolean respectFrontendRoles,List<Category> cats) throws IllegalArgumentException,DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException;
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.
	 * Note that the contentlet argument must be obtained using checkout methods.  
	 * @param contentlet - The inode of your contentlet must be 0.
	 * @param contentRelationships - throws IllegalArgumentException if null. Used to set relationships to new contentlet version 
	 * @param cats - throws IllegalArgumentException if null. Used to set categories to new contentlet version
	 * @param user
	 * @param respectFrontendRoles
	 * @throws IllegalArgumentException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException If inode not = to 0
	 * @throws DotContentletValidationException If content is not valid
	 */
	public Contentlet checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats, User user,boolean respectFrontendRoles) throws IllegalArgumentException,DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException;
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.
	 * Note that the contentlet argument must be obtained using checkout methods.  
	 * @param contentlet - The inode of your contentlet must be 0.
	 * @param user
	 * @param respectFrontendRoles
	 * @throws IllegalArgumentException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException If inode not = to 0
	 * @throws DotContentletValidationException If content is not valid
	 */
	public Contentlet checkin(Contentlet contentlet, User user,boolean respectFrontendRoles) throws IllegalArgumentException,DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException;
	
	/**
	 * Will check in a new version of you contentlet. The inode of your contentlet must be 0.
	 * Note that the contentlet argument must be obtained using checkout methods.  
	 * @param contentlet - The inode of your contentlet must be 0.
	 * @param contentRelationships - throws IllegalArgumentException if null. Used to set relationships to new contentlet version 
	 * @param user
	 * @param respectFrontendRoles
	 * @throws IllegalArgumentException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException If inode not = to 0
	 * @throws DotContentletValidationException If content is not valid
	 */
	public Contentlet checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, User user,boolean respectFrontendRoles) throws IllegalArgumentException,DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException;
	
	/**
	 * Will check in a update of your contentlet without generating a new version. The inode of your contentlet must be different from 0. 
	 * Note this method will also atempt to publish the contentlet and related assets (when checking in) without altering the mod date or mod user.
	 * Note that the contentlet argument must be obtained using checkout methods.   
	 * @param contentlet - The inode of your contentlet must be different from 0.
	 * @param contentRelationships - Used to set relationships to updated contentlet version 
	 * @param user
	 * @param respectFrontendRoles
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
	 * Note this method should not be used currently because it could pull too many versions. 
	 * @param identifier
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotStateException if the identifier is for contentlet
	 */
	public List<Contentlet> findAllVersions(Identifier identifier, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException, DotStateException;
	
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
	 * @return
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
	 * @return
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
	 * @return
	 * @throws DotDataException
	 * @throws DotContentletStateException
	 * @throws DotSecurityException
	 */
	public List<Contentlet> find(Category category, long languageId, boolean live, String orderBy, User user, boolean respectFrontendRoles) throws DotDataException, DotContentletStateException, DotSecurityException;

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
	 * @throws DotDataException
	 * @throws DotContentletStateException
	 * @throws DotSecurityException
	 */
	public List<Contentlet> find(List<Category> categories,long languageId, boolean live, String orderBy, User user, boolean respectFrontendRoles) throws DotDataException, DotContentletStateException, DotSecurityException;

	/**
	 * Use to set contentlet properties.  The value should be String, the proper type of the property
	 * @param contentlet
	 * @param field
	 * @param value
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotContentletStateException if the object isn't the proper type or cannot be converted to the proper type
	 */
	public void setContentletProperty(Contentlet contentlet, Field field, Object value) throws DotContentletStateException;
	
	/**
	 * Use to validate your contentlet.
	 * @param contentlet
	 * @param categories
	 * @throws DotContentletValidationException will be thrown if the contentlet is not valid.  
	 * Use the notValidFields property of the exception to get which fields where not valid  
	 */
	public void validateContentlet(Contentlet contentlet,List<Category> cats)throws DotContentletValidationException; 
	
	/**
	 * Use to validate your contentlet.
	 * @param contentlet
	 * @param contentRelationships
	 * @param categories
	 * @throws DotContentletValidationException will be thrown if the contentlet is not valid.  
	 * Use the notValidFields property of the exception to get which fields where not valid
	 */
	public void validateContentlet(Contentlet contentlet,Map<Relationship, List<Contentlet>> contentRelationships,List<Category> cats)throws DotContentletValidationException; 
	
	/**
	 * Use to validate your contentlet.
	 * @param contentlet
	 * @param contentRelationships
	 * @param categories
	 * @throws DotContentletValidationException will be thrown if the contentlet is not valid.  
	 * Use the notValidFields property of the exception to get which fields where not valid
	 */
	public void validateContentlet(Contentlet contentlet, ContentletRelationships contentRelationships, List<Category> cats)throws DotContentletValidationException; 
	
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
	 * @return A "light" contentlet.
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	public Contentlet convertFatContentletToContentlet (com.dotmarketing.portlets.contentlet.business.Contentlet fatty) throws DotDataException, DotSecurityException;
	
	/**
	 * Converts a "light" contentlet into a "fat" (legacy) contentlet.
	 * @param A "light" contentlet to be converted.
	 * @return Fat contentlet.
	 * @throws DotDataException
	 */
	public com.dotmarketing.portlets.contentlet.business.Contentlet convertContentletToFatContentlet (Contentlet cont, com.dotmarketing.portlets.contentlet.business.Contentlet fatty) throws DotDataException;
    
   /**
    * 
    * @param deleteFrom
    * @return
    * @throws DotDataException
    */
	public int deleteOldContent(Date deleteFrom) throws DotDataException;
	
	/**

	 * 
	 * @param deleteFrom
	 * @param offset
	 * @return
	 * @throws DotDataException
	 */
	public List<String> findFieldValues(String structureInode, Field field, User user, boolean respectFrontEndRoles) throws DotDataException;
	
	
	//http://jira.dotmarketing.net/browse/DOTCMS-2178
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
	 * @return
	 * @throws DotDataException
	 */
	public long contentletCount() throws DotDataException;
	
	/**
	 * gets the number of contentlet identifiers in the system. This number includes all versions not distinct identifiers
	 * @return
	 * @throws DotDataException
	 */
	public long contentletIdentifierCount() throws DotDataException;
	

	public List<Contentlet> getSiblings(String identifier)throws DotDataException, DotSecurityException ;
	
	/**
	 * Will search the DB.  Use the SQLQueryBuilderFactory or GenericQueryBuiilderFactory to build
	 * your query object
	 * @param query
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws ValidationException
	 * @throws DotDataException
	 */
	public List<Map<String, Serializable>> DBSearch(Query query, User user,boolean respectFrontendRoles) throws ValidationException, DotDataException;
	
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
	public List<Contentlet> searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, ParseException;
	
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
	public List<Contentlet> searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission) throws DotDataException, DotSecurityException, ParseException;

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
	public List<Contentlet> searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission, boolean anyLanguage) throws DotDataException,DotSecurityException, ParseException;

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
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	
	public boolean canLock(Contentlet contentlet, User user) throws   DotLockException ;

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
}
