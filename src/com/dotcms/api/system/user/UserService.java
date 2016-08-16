package com.dotcms.api.system.user;

import java.io.Serializable;
import java.util.Map;

import com.dotmarketing.business.UserAPI;

/**
 * Provides useful methods to interact with {@link User} objects in dotCMS. Most
 * of the functionality exposed by this class is aimed to providing information
 * for the UI layer, which in turn can be exposed via REST Services. The
 * rationale behind this service is to provide other system components or
 * third-party code with more complex operations than the ones exposed via the
 * {@link UserAPI} class.
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Aug 8, 2016
 *
 */
public interface UserService extends Serializable {

	/**
	 * Returns a list of dotCMS users based on the specified search criteria.
	 * Two types of result can be obtained by calling this method:
	 * <ul>
	 * <li>If both the {@code assetInode} and the {@code permission} values
	 * <b>are set</b>, this method will return the list of users that have the
	 * specified permission type on the specified Inode.</li>
	 * <li>If the {@code assetInode} or the {@code permission} value <b>is NOT
	 * set</b>, this method will return a list of users based on the criteria
	 * specified in the {@code params} Map:
	 * <ul>
	 * <li>{@code query}: The String or characters that can match the first
	 * name, last name, or e-mail of a user. This is the same value that would
	 * be passed to the {@code LIKE} keyword in SQL. This value will be
	 * automatically sanitized to strip off malicious code.</li>
	 * <li>{@code start}: For pagination purposes. The bottom range of records
	 * to include in the result.</li>
	 * <li>{@code end}: For pagination purposes. The top range of records to
	 * include in the result.</li>
	 * <li>{@code includeAnonymous}: Set to {@code true} if anonymous users will
	 * be included in the result list. Otherwise, set to {@code false}.</li>
	 * <li>{@code includeDefault}: Set to {@code true} if the default user will
	 * be included in the result list. Otherwise, set to {@code false}.</li>
	 * </ul>
	 * </li>
	 * </ul>
	 * 
	 * @param assetInode
	 *            - (Optional) The Inode of the asset that one or more users
	 *            have permission on.
	 * @param permission
	 *            - (Optional) The type of permission assigned to the specified
	 *            asset.
	 * @param params
	 *            - Additional parameters for more specific queries.
	 * @return A {@code Map} containing the dotCMS users that match the filter
	 *         criteria.
	 * @throws Exception
	 *             An error occurred when retrieving the user list.
	 */
	public Map<String, Object> getUsersList(String assetInode, String permission, Map<String, String> params)
			throws Exception;

}
