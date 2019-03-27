/**
 * 
 */
package com.dotmarketing.business.skeleton;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;


import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.DotValidationException;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;

/**
 * @author Jason Tesser
 * 
 */

public interface DotCMSAPIPreHook {

	/**
	 * Use to retrieve all version of all content in the database. This is not a
	 * common method to use. Only use if you need to do maintenance tasks like
	 * search and replace something in every piece of content. Doesn't respect
	 * permissions.
	 * 
	 * @param offset
	 *            can be 0 if no offset
	 * @param limit
	 *            can be 0 of no limit
	 * @return
	 * @throws DotDataException
	 */
	public List<Inode> findAll(int offset, int limit) throws DotDataException;

	/**
	 * Finds a Inode Object given the inode
	 * 
	 * @param inode
	 * @return
	 * @throws DotDataException
	 */
	public Inode findByInode(String inode, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Retrieves a node from the cache first, then falls
	 * back to the database if not found based on its identifier
	 * 
	 * @param identifier
	 * @param live
	 *            Retrieves the live version if false retrieves the working
	 *            version
	 * @return
	 * @throws DotSecurityException
	 * @throws DotStateException
	 * @throws DotDataException
	 */
	public Inode findByIdentifier(String identifier, boolean live, long languageId, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException, DotStateException;

	/**
	 * Retrieves a node list from the database based on a identifiers
	 * array
	 * 
	 * @param identifiers
	 *            Array of identifiers
	 * @param live
	 *            Retrieves the live version if false retrieves the working
	 *            version
	 * @param languageId
	 * @param user
	 * @param respectFrontendRoles
	 * @return List<Inode>
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotStateException
	 */
	public List<Inode> findByIdentifiers(String[] identifiers, boolean live, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException, DotStateException;


	/**
	 * Gets a list of Inodes from a given parent folder
	 * 
	 * @param parentFolder
	 * @return
	 * @throws DotSecurityException
	 */
	public List<Inode> findByFolder(Folder parentFolder, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException;

	/**
	 * Gets a list of Inodes from a given parent host, retrieves the
	 * working version of content
	 * 
	 * @param parentHost
	 * @return
	 * @throws DotSecurityException
	 */
	public List<Inode> findByHost(Host parentHost, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException;

	/**
	 * Copies a node, including all its fields including binary files,
	 * image and file fields are pointers and the are preserved as the are so if
	 * source node points to image A and resulting new node will
	 * point to same image A as well, also copies source permissions.
	 * 
	 * @param node
	 * @param user
	 * @param respectFrontendRoles
	 * @return Inode
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotStateException
	 *             throws this exception if the new node requires a
	 *             destination host or folder mandated by its structure
	 */
	public Inode copy(Inode node, Inode folderOrHost, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException,
			DotStateException;


	/**
	 * The search here takes a lucene query and pulls Inodes for you. You
	 * can pass sortBy as null if you do not have a field to sort by. limit
	 * should be 0 if no limit and the offset should be -1 is you are not
	 * paginating. The returned list will be filtered with only the nodes
	 * that the user can read(use). you can of course also pass permissions to
	 * further limit in the lucene query itself
	 * 
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
	public List<Inode> search(String condition, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException;

	/**
	 * The search here takes a lucene query and pulls Inodes for you. You
	 * can pass sortBy as null if you do not have a field to sort by. limit
	 * should be 0 if no limit and the offset should be -1 is you are not
	 * paginating. The returned list will be filtered with only the nodes
	 * that match the required permission. You can of course also pass
	 * permissions to further limit in the lucene query itself
	 * 
	 * @param luceneQuery
	 * @param limit
	 * @param offset
	 * @param sortBy
	 *            indexName(previously known as dbColumnName) to order by. Can
	 *            be null or empty string
	 * @param user
	 * @param respectFrontendRoles
	 * @param requiredPermission
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws ParseException
	 */
	public List<Inode> search(String condition, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles,
			int requiredPermission) throws DotDataException, DotSecurityException;


	/**
	 * Retrieves all references for a Inode. The result is an ArrayList of
	 * type Map whose key will be page or container with the respective object
	 * as the value.
	 * 
	 * @param node
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotStateException
	 *             - if teh node is null or has an inode of 0
	 */
	public List<Map<String, Object>> getReferences(Inode node, User user, boolean respectFrontendRoles)
			throws DotSecurityException, DotDataException, DotStateException;

	/**
	 * 
	 * @param node1
	 * @param node2
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public boolean isEqual(Inode node1, Inode node2, User user, boolean respectFrontendRoles)
			throws DotSecurityException, DotDataException;

	/**
	 * This method archives the given node
	 * 
	 * @param node
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public void archive(Inode node, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException,
			DotStateException;

	/**
	 * This method completely deletes the given node from the system
	 * 
	 * @param node
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public void delete(Inode node, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException,
			DotStateException;

	/**
	 * This method completely deletes the given node from the system. It
	 * was added for the jira issue
	 * http://jira.dotmarketing.net/browse/DOTCMS-2059
	 * 
	 * @param node
	 * @param user
	 * @param respectFrontendRoles
	 * @param allVersions
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */

	public void delete(Inode node, User user, boolean respectFrontendRoles, boolean allVersions) throws DotDataException,
			DotSecurityException, DotStateException;

	/**
	 * Publishes a piece of content.
	 * 
	 * @param node
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotStateException
	 */
	public void publish(Inode node, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException,
			DotStateException, DotStateException, DotStateException;

	/**
	 * Publishes a piece of content.
	 * 
	 * @param nodes
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotStateException
	 */
	public void publish(List<Inode> nodes, User user, boolean respectFrontendRoles) throws DotSecurityException,
			DotDataException, DotStateException, DotStateException;

	/**
	 * This method unpublishes the given node
	 * 
	 * @param node
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotStateException
	 *             if the contentent cannot be unlocked by the user
	 */
	public void unpublish(Inode node, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException,
			DotStateException;

	/**
	 * This method unpublishes the given node
	 * 
	 * @param nodes
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotStateException
	 *             If one or more nodes are locked and the user is not the
	 *             one who locked it. It will unpublish all that are possible
	 *             though
	 */
	public void unpublish(List<Inode> nodes, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException, DotStateException;

	/**
	 * This method archives the given nodes
	 * 
	 * @param nodes
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public void archive(List<Inode> nodes, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException, DotStateException;

	/**
	 * This method unarchives the given nodes
	 * 
	 * @param nodes
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotStateException
	 *             If one or more of the nodes are not archived. It will
	 *             unarchive all that it can though
	 */
	public void unarchive(List<Inode> nodes, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException, DotStateException;

	/**
	 * This method unarchives the given node
	 * 
	 * @param node
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotStateException
	 *             if the node is not archived
	 */
	public void unarchive(Inode node, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException,
			DotStateException;

	/**
	 * This method completely deletes the given node from the system and
	 * make a xml file backup
	 * 
	 * @param nodes
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public void deleteAllVersionsandBackup(List<Inode> nodes, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException, DotStateException;

	/**
	 * This method completely deletes the given node from the system
	 * 
	 * @param nodes
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public void delete(List<Inode> nodes, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException, DotStateException;

	/**
	 * This method completely deletes the given node from the system. It
	 * was added for the jira issue
	 * http://jira.dotmarketing.net/browse/DOTCMS-2059
	 * 
	 * @param nodes
	 * @param user
	 * @param respectFrontendRoles
	 * @param allVersions
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public void delete(List<Inode> nodes, User user, boolean respectFrontendRoles, boolean allVersions) throws DotDataException,
			DotSecurityException, DotStateException;

	/**
	 * 
	 * @param node
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotStateException
	 */
	public void unlock(Inode node, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Use to lock a node
	 * 
	 * @param node
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotStateException
	 *             - if the node is null
	 */
	public void lock(Inode node, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException,
			DotStateException;

	/**
	 * Allows you to checkout a content so it can be altered and checked in
	 * 
	 * @param nodeInode
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotStateException
	 *             if node is not already persisted
	 */
	public Inode checkout(String nodeInode, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException, DotStateException;

	/**
	 * Allows you to checkout contents so it can be altered and checked in
	 * 
	 * @param nodes
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotStateException
	 */
	public List<Inode> checkout(List<Inode> nodes, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException, DotStateException;

	/**
	 * Allows you to checkout contents based on a lucene query so it can be
	 * altered and checked in
	 * 
	 * @param luceneQuery
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotStateException
	 * @throws ParseException
	 */
	public List<Inode> checkoutByCondition(String condition, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException, DotStateException;

	/**
	 * Allows you to checkout contents based on a lucene query so it can be
	 * altered and checked in, in a paginated fashion
	 * 
	 * @param luceneQuery
	 * @param user
	 * @param respectFrontendRoles
	 * @param offset
	 * @param limit
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotStateException
	 * @throws ParseException
	 */
	public List<Inode> checkoutByCondition(String condition, User user, boolean respectFrontendRoles, int offset, int limit)
			throws DotDataException, DotSecurityException, DotStateException;

	/**
	 * Will check in a new version of you node. The inode of your
	 * object to checkin must not be set.
	 * 
	 * @param node
	 *            - The inode of your node must be 0.
	 * @param contentRelationships
	 *            - throws IllegalArgumentException if null. Used to set
	 *            relationships to new node version
	 * @param cats
	 *            - throws IllegalArgumentException if null. Used to set
	 *            categories to new node version
	 * @param permissions
	 *            - throws IllegalArgumentException if null. Used to set
	 *            permissions to new node version
	 * @param user
	 * @param respectFrontendRoles
	 * @throws IllegalArgumentException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotStateException
	 *             If inode not = to 0
	 * @throws DotValidationException
	 *             If content is not valid
	 */
	public Inode checkin(Inode node, List<Permission> permissions, User user, boolean respectFrontendRoles)
			throws IllegalArgumentException, DotDataException, DotSecurityException, DotStateException, DotValidationException;


	/**
	 * Will check in a new version of you node. The inode of your
	 * object to checkin must not be set.
	 * 
	 * @param node
	 *            - The inode of your node must be 0.
	 * @param user
	 * @param respectFrontendRoles
	 * @throws IllegalArgumentException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotStateException
	 *             If inode not = to 0
	 * @throws DotValidationException
	 *             If content is not valid
	 */
	public Inode checkin(Inode node, User user, boolean respectFrontendRoles) throws IllegalArgumentException,
			DotDataException, DotSecurityException, DotStateException, DotValidationException;

	/**
	 * Will check in a update of your node without generating a new
	 * version. The inode of your node must be different from 0. Note this
	 * method will also atempt to publish the node and related assets
	 * (when checking in) without altering the mod date or mod user.
	 * 
	 * @param node
	 * Will check in without versioning your node. 
	 * @param user
	 * @param respectFrontendRoles
	 * @throws IllegalArgumentException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotStateException
	 *             If exist another node working or live
	 * @throws DotValidationException
	 *             If content is not valid
	 */
	public Inode checkinWithoutVersioning(Inode node, List<Permission> permissions, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException, DotStateException, DotValidationException;

	/**
	 * Will make the passed in node the working copy.
	 * 
	 * @param node
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	public void restoreVersion(Inode node, User user, boolean respectFrontendRoles) throws DotSecurityException,
			DotStateException, DotDataException;

	/**
	 * Retrieves all versions for a node identifier Note this method
	 * should not be used currently because it could pull too many versions.
	 * 
	 * @param identifier
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotStateException
	 *             if the identifier is for node
	 */
	public List<Inode> findAllVersions(Identifier identifier, User user, boolean respectFrontendRoles) throws DotSecurityException,
			DotDataException, DotStateException;

	/**
	 * Retrieves all versions for a node identifier checked in by a real
	 * user meaning not the system user
	 * 
	 * @param identifier
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotStateException
	 *             if the identifier is for node
	 */
	public List<Inode> findAllUserVersions(Identifier identifier, User user, boolean respectFrontendRoles)
			throws DotSecurityException, DotDataException, DotStateException;

	/**
	 * Meant to get the title or name of a node
	 * 
	 * @param node
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotSecurityException
	 * @throws DotStateException
	 * @throws DotDataException
	 */
	public String getName(Inode node, User user, boolean respectFrontendRoles) throws DotSecurityException, DotStateException,
			DotDataException;

	/**
	 * Copies properties from the map to the node
	 * 
	 * @param node
	 *            node to copy to
	 * @param properties
	 * @throws DotStateException
	 *             if the map passed in has properties that don't match the
	 *             node
	 * @throws DotSecurityException
	 */
	public Inode copyFromMap(Inode node, Map<String, Object> properties) throws DotStateException, DotSecurityException;

	/**
	 * Use to validate your node.
	 * 
	 * @param node
	 * @param categories
	 * @throws DotValidationException
	 *             will be thrown if the node is not valid. Use the
	 *             notValidFields property of the exception to get which fields
	 *             where not valid
	 */
	public void validate(Inode node) throws DotValidationException;

	/**
	 * 
	 * @param deleteFrom
	 * @param offset
	 * @return
	 * @throws DotDataException
	 */
	public int deleteOld(Date deleteFrom, int offset) throws DotDataException;

	/**
	 * gets the number of nodes in the system. This number includes all
	 * versions not distinct identifiers
	 * 
	 * @return
	 * @throws DotDataException
	 */
	public long count() throws DotDataException;

	/**
	 * gets the number of node identifiers in the system. This number
	 * includes all versions not distinct identifiers
	 * 
	 * @return
	 * @throws DotDataException
	 */
	public long identifierCount() throws DotDataException;

	public List<Map<String, Serializable>> DBSearch(Query query, User user, boolean respectFrontendRoles) throws ValidationException,
			DotDataException;

	/**
	 * Method will update hostInode of content to systemhost
	 * 
	 * @param hostIdentifier
	 */
	public void UpdateWithSystemHost(String hostIdentifier) throws DotDataException;

	/**
	 * Method will remove User References of the given userId in Inode
	 * 
	 * @param userId
	 */
	public void removeUserReferences(String userId) throws DotDataException;

	/**
	 * Deletes the given version of the node from the system
	 * 
	 * @param node
	 * @param user
	 * @param respectFrontendRoles
	 */
	public void deleteVersion(Inode node, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Checks if the version you are saving is live=false. If it is, this method
	 * will save WITHOUT creating a new version. Otherwise, it will create a new
	 * working (Draft) version and return it to you
	 * 
	 * @param node
	 *            - The inode of your node must not be null.
	 * @param contentRelationships
	 *            - throws IllegalArgumentException if null. Used to set
	 *            relationships to new node version
	 * @param cats
	 *            - throws IllegalArgumentException if null. Used to set
	 *            categories to new node version
	 * @param permissions
	 *            - throws IllegalArgumentException if null. Used to set
	 *            permissions to new node version
	 * @param user
	 * @param respectFrontendRoles
	 * @throws IllegalArgumentException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotStateException
	 *             If inode null
	 * @throws DotValidationException
	 *             If content is not valid
	 */
	public Inode saveDraft(Inode node, List<Permission> permissions, User user, boolean respectFrontendRoles)
			throws IllegalArgumentException, DotDataException, DotSecurityException, DotStateException, DotValidationException;

	/**
	 * The search here takes a lucene query and pulls Inodes for you, using
	 * the identifier of the node.You can pass sortBy as null if you do
	 * not have a field to sort by. limit should be 0 if no limit and the offset
	 * should be -1 is you are not paginating. The returned list will be
	 * filtered with only the nodes that the user can read(use). you can
	 * of course also pass permissions to further limit in the lucene query
	 * itself
	 * 
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
	public List<Inode> searchByIdentifier(String identifier, int limit, int offset, String sortBy, User user,
			boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * The search here takes a lucene query and pulls Inodes for you, using the
	 * identifier of the node.You can pass sortBy as null if you do not have a
	 * field to sort by. limit should be 0 if no limit and the offset should be
	 * -1 is you are not paginating. The returned list will be filtered with
	 * only the nodes that match the required permission. You can of course also
	 * pass permissions to further limit in the lucene query itself
	 * 
	 * @param luceneQuery
	 * @param limit
	 * @param offset
	 * @param sortBy
	 *            indexName(previously known as dbColumnName) to order by. Can
	 *            be null or empty string
	 * @param user
	 * @param respectFrontendRoles
	 * @param requiredPermission
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws ParseException
	 */
	public List<Inode> searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user,
			boolean respectFrontendRoles, int requiredPermission) throws DotDataException, DotSecurityException;

}
