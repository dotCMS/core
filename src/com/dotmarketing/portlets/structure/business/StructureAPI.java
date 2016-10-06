package com.dotmarketing.portlets.structure.business;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;

/**
 * This class provides the dotCMS developer with access to the information related 
 * to Content Type objects (previously known as "Structures").
 * 
 * @author Jorge Urdaneta
 * @version 1.1
 * @since Feb 11, 2013
 *
 */
public interface StructureAPI {

	/**
	 * Removes the specified {@link Structure} (Content Type) from the site by
	 * the specified user. All the information associated to this Structure will
	 * be deleted, including:
	 * <ul>
	 * <li>Contentlets.</li>
	 * <li>Data submitted by Forms (if applicable).</li>
	 * <li>Content relationships.</li>
	 * <li>Folder references (if applicable).</li>
	 * </ul>
	 * However, the Structure <b>will not be deleted</b> if one or both of the
	 * following scenarios is present:
	 * <ol>
	 * <li>There is a Container using this Structure.</li>
	 * <li>The user is trying to delete the Default Structure.</li>
	 * </ol>
	 * 
	 * @param st
	 *            - The Structure that will be deleted.
	 * @param user
	 *            - The {@link User} that is deleting the specified Structure.
	 * @throws DotSecurityException
	 *             The specified user does not have permissions to perform this
	 *             action.
	 * @throws DotDataException
	 *             An error occurred when deleting the information from the
	 *             database.
	 * @throws DotStateException
	 *             A system error occurred.
	 */
    void delete(Structure st, User user) throws DotSecurityException, DotDataException, DotStateException;

	/**
	 * Finds the {@link Structure} object (Content Type) associated to the
	 * specified Inode.
	 * 
	 * @param inode
	 *            - The Inode representing the Structure to find.
	 * @param user
	 *            - The {@link User} that is looking for the specified
	 *            Structure.
	 * @return The {@link Structure} object.
	 * @throws DotSecurityException
	 *             The specified user does not have permissions to perform this
	 *             action.
	 * @throws DotDataException
	 *             An error occurred when deleting the information from the
	 *             database.
	 * @throws DotStateException
	 *             A system error occurred.
	 */
    Structure find(String inode, User user) throws DotSecurityException, DotDataException, DotStateException;

	/**
	 * Retrieves the list of {@link Structure} objects (Content Types) that the
	 * specified user has access to. By default, the result set will be ordered
	 * in ascendent order, and grouped by Content Type and name.
	 * <p>
	 * The Content Types that the user doesn't have permissions on, or that are
	 * not available for the current system license will not be included as part
	 * of the results.
	 * 
	 * @param user
	 *            - The {@link User} retrieving the list of Content Types.
	 * @param respectFrontendRoles
	 *            - If set to <code>true</code>, the permission handling will be
	 *            based on the currently logged-in user or the Anonymous role.
	 *            Otherwise, set to <code>false</code>.
	 * @param allowedStructsOnly
	 *            - If set to <code>true</code>, returns only the Content Types
	 *            the specified user has read permission on. Otherwise, set to
	 *            <code>false</code>.
	 * @return The list of permissioned {@link Structure} objects.
	 * @throws DotDataException
	 *             An error occurred when deleting the information from the
	 *             database.
	 */
	public List<Structure> find(User user, boolean respectFrontendRoles, boolean allowedStructsOnly) throws DotDataException;

	/**
	 * Retrieves the list of {@link Structure} objects (Content Types) that the
	 * specified user has access to. This method will allow you to specify more
	 * filtering options for the result set, such as the ordering, grouping,
	 * record limit, etc.
	 * <p>
	 * The Content Types that the user doesn't have permissions on, or that are
	 * not available for the current system license will not be included as part
	 * of the results.
	 * 
	 * @param user
	 *            - The {@link User} retrieving the list of Content Types.
	 * @param respectFrontendRoles
	 *            - If set to <code>true</code>, the permission handling will be
	 *            based on the currently logged-in user or the Anonymous role.
	 *            Otherwise, set to <code>false</code>.
	 * @param allowedStructsOnly
	 *            - If set to <code>true</code>, returns only the Content Types
	 *            the specified user has read permission on. Otherwise, set to
	 *            <code>false</code>.
	 * @param condition
	 *            - Any specific condition or filtering criteria for the
	 *            resulting Content Types. This value is sanitized before being
	 *            added to the query.
	 * @param orderBy
	 *            - The column(s) to order the results by.
	 * @param limit
	 *            - The maximum number of records to return.
	 * @param offset
	 *            - The record offset for pagination purposes.
	 * @param direction
	 *            - The ordering of the results: <code>asc</code>, or
	 *            <code>desc</code>.
	 * @return A list of {@link Structure} objects based on the current user's
	 *         permissions and the system license.
	 * @throws DotDataException
	 *             An error occurred when retrieving information from the
	 *             database.
	 */
	public List<Structure> find(User user, boolean respectFrontendRoles, boolean allowedStructsOnly, String condition,
			String orderBy, int limit, int offset, String direction) throws DotDataException;

	/**
	 * Finds the {@link Structure} object (Content Type) associated to the
	 * specified Velocity variable name.
	 * 
	 * @param varName
	 *            - Variable name of the Content Type in Velocity.
	 * @param user
	 *            - The {@link User} retrieving the Content Type.
	 * @return The {@link Structure} object.
	 * @throws DotSecurityException
	 *             The specified user does not have permissions to perform this
	 *             action.
	 * @throws DotDataException
	 *             An error occurred when deleting the information from the
	 *             database.
	 */
	Structure findByVarName(String varName, User user) throws DotSecurityException, DotDataException;

	
    /**
     * Counts the amount of structures in DB filtering by the given condition
     * 
     * @param condition to be used
     * @return Amount of structures found
     */
    int countStructures(String condition);


	/**
	 * Return the structures order from who that has a the most recent created
	 * {@link com.dotmarketing.portlets.contentlet.business.Contentlet} to who that has the less recent created
	 * {@link com.dotmarketing.portlets.contentlet.business.Contentlet}.
	 *
	 * @param type Structure.Type
	 * @param user filter {@link com.dotmarketing.portlets.contentlet.business.Contentlet} by user
	 * @param nRecents number of resents structure to return
	 *
	 * @return A List of Map, each Map represents a Structure and has the follows keys: name, inode, type and date
	 *         (the date of the last created contentlet)
	 * @throws DotDataException
     */
	public Collection<Map<String, Object>> getRecentContentType(Structure.Type type, User user, int nRecents) throws DotDataException;

}
