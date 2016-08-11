package com.dotcms.api.system.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.user.ajax.UserAjax;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * This factory creates a singleton instance of the {@link UserService} class.
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Aug 8, 2016
 *
 */
@SuppressWarnings("serial")
public class UserServiceFactory implements Serializable {

	private final UserService userService = new UserServiceImpl();

	/**
	 * Private constructor for singleton creation.
	 */
	private UserServiceFactory() {

	}

	/**
	 * Singleton holder using initialization on demand
	 */
	private static class SingletonHolder {
		private static final UserServiceFactory INSTANCE = new UserServiceFactory();
	}

	/**
	 * Returns a single instance of this factory.
	 * 
	 * @return A unique {@link UserServiceFactory} instance.
	 */
	public static UserServiceFactory getInstance() {
		return UserServiceFactory.SingletonHolder.INSTANCE;
	}

	/**
	 * Returns a singleton instance of the {@link UserService} class.
	 * 
	 * @return The {@link UserService} instance.
	 */
	public UserService getUserService() {
		return this.userService;
	}

	/**
	 * The concrete implementation of the {@link UserService} class.
	 * 
	 * @author Jose Castro
	 * @version 3.7
	 * @since Aug 8, 2016
	 *
	 */
	private final class UserServiceImpl implements UserService {

		private static final String USER_TYPE_VALUE = "user";

		/**
		 * This inner class is used to process the information related to
		 * internal queries that ultimately generate a final useful result for
		 * the services using this functionality.
		 * 
		 * @author root
		 * @version 1.0
		 * @since Mar 22, 2012
		 *
		 */
		private abstract class UsersListTemplate {

			protected String inode;
			protected int permissionType;
			protected String filter;
			protected int start;
			protected int limit;
			protected boolean includeAnonymous;
			protected boolean includeDefault;

			/**
			 * Returns the official count of {@link User} objects that make up
			 * the user list.
			 * 
			 * @return The number of users in the list.
			 */
			public abstract int getUserCount();

			/**
			 * Returns the list of {@link User} objects that will be returned
			 * according the the specified filtering criteria.
			 * 
			 * @return The user list.
			 */
			public abstract List<User> getUsers();

			/**
			 * Creates an instance of this class.
			 * 
			 * @param inode
			 *            - The Inode of a given asset in order to get the list
			 *            of users that have access to it.
			 * @param permissionType
			 *            - The permission type that users with access to the
			 *            asset Inode must have.
			 * @param filter
			 *            - The string or characters that are part of the first
			 *            name, last name, or e-mail of the user.
			 * @param start
			 *            - For pagination purposes. Lower range of the set to
			 *            include in the query result.
			 * @param limit
			 *            - For pagination purposes. Upper range of the set to
			 *            include in the query result.
			 */
			public UsersListTemplate(String inode, int permissionType, String filter, int start, int limit) {
				this(inode, permissionType, filter, start, limit, false, true);
			}

			/**
			 * Creates an instance of this class.
			 * 
			 * @param inode
			 *            - The Inode of a given asset in order to get the list
			 *            of users that have access to it.
			 * @param permissionType
			 *            - The permission type that users with access to the
			 *            asset Inode must have.
			 * @param filter
			 *            - The string or characters that are part of the first
			 *            name, last name, or e-mail of the user.
			 * @param start
			 *            - For pagination purposes. Lower range of the set to
			 *            include in the query result.
			 * @param limit
			 *            - For pagination purposes. Upper range of the set to
			 *            include in the query result.
			 * @param includeAnonymous
			 *            - If set to {@code true}, the "Anonymous" user will be
			 *            included in the result. Otherwise, set to
			 *            {@code false}.
			 * @param includeDefault
			 *            - If set to {@code true}, the "Default" user will be
			 *            included in the result. Otherwise, set to
			 *            {@code false}.
			 */
			public UsersListTemplate(String inode, int permissionType, String filter, int start, int limit,
					boolean includeAnonymous, boolean includeDefault) {
				this.inode = inode;
				this.permissionType = permissionType;
				this.filter = filter;
				this.start = start;
				this.limit = limit;
				this.includeAnonymous = includeAnonymous;
				this.includeDefault = includeDefault;
			}

			/**
			 * Executes this filtering template based on the specified search
			 * criteria.
			 * 
			 * @return A {@link Map} with the result of the filtering query.
			 */
			public Map<String, Object> perform() {
				ArrayList<Map<String, String>> list = null;
				Map<String, Object> results = new HashMap<String, Object>(2);
				int totalItemCount = 0;
				List<User> users = null;
				int realUserCount = 0;
				try {
					// Step 1. Retrieve users, beginning from "start" parameter,
					// up to a number of "limit" items, filtered by "filter"
					// parameter.
					totalItemCount = getUserCount();
					if (start < totalItemCount) {
						users = getUsers();
						realUserCount = users.size();
					}
					// Step 2. Assemble all of this information into an
					// appropriate container to the view
					if (users != null) {
						int pageSize = realUserCount;
						list = new ArrayList<Map<String, String>>(pageSize);
						for (User aUser : users) {
							Map<String, String> aRecord = new HashMap<String, String>();
							String fullName = aUser.getFullName();
							fullName = (UtilMethods.isSet(fullName) ? fullName : " ");
							String emailAddress = aUser.getEmailAddress();
							emailAddress = (UtilMethods.isSet(emailAddress) ? emailAddress : " ");
							aRecord.put("id", aUser.getUserId());
							aRecord.put("type", USER_TYPE_VALUE);
							aRecord.put("name", fullName);
							aRecord.put("emailaddress", emailAddress);
							list.add(aRecord);
						}
					} else {
						list = new ArrayList<Map<String, String>>(0);
					}
				} catch (Exception ex) {
					Logger.warn(UserAjax.class, "::processUsersList -> Could not process list of users.");
					list = new ArrayList<Map<String, String>>(0);
				}
				results.put("data", list);
				results.put("total", totalItemCount);
				return results;
			}
		}

		@Override
		public Map<String, Object> getUsersList(String assetInode, String permission, Map<String, String> params)
				throws Exception {
			int start = 0;
			if (params.containsKey("start")) {
				start = Integer.parseInt((String) params.get("start"));
			}
			int limit = -1;
			if (params.containsKey("limit")) {
				limit = Integer.parseInt((String) params.get("limit"));
			}
			String query = "";
			if (params.containsKey("query")) {
				query = (String) params.get("query");
			}
			boolean includeAnonymous = false;
			if (params.containsKey("includeAnonymous")) {
				includeAnonymous = Boolean.valueOf((String) params.get("includeAnonymous"));
			}
			// Initially set to "true" for backwards compatibility
			boolean includeDefault = true;
			if (params.containsKey("includeDefault")) {
				includeDefault = Boolean.valueOf((String) params.get("includeDefault"));
			}
			Map<String, Object> results;
			if ((InodeUtils.isSet(assetInode) && !assetInode.equals("0"))
					&& (UtilMethods.isSet(permission) && !permission.equals("0"))) {
				results = processUserListWithPermissionOnInode(assetInode, permission, query, start, limit);
			} else {
				results = processUserList(query, start, limit, includeAnonymous, includeDefault);
			}
			return results;
		}

		/**
		 * Returns a {@link Map} containing a list of dotCMS {@link User}
		 * objects based on the specified search criteria.
		 * 
		 * @param query
		 *            - The string or characters that are part of the first
		 *            name, last name, or e-mail of the user.
		 * @param start
		 *            - For pagination purposes. Lower range of the set to
		 *            include in the query result.
		 * @param limit
		 *            - For pagination purposes. Upper range of the set to
		 *            include in the query result.
		 * @param includeAnonymous
		 *            - If set to {@code true}, the "Anonymous" user will be
		 *            included in the result. Otherwise, set to {@code false}.
		 * @param includeDefault
		 *            - If set to {@code true}, the "Default" user will be
		 *            included in the result. Otherwise, set to {@code false}.
		 * @return A Map containing the user list and additional query
		 *         information.
		 */
		private Map<String, Object> processUserList(String query, int start, int limit, boolean includeAnonymous,
				boolean includeDefault) {
			Map<String, Object> results = new UsersListTemplate("", 0, query, start, limit, includeAnonymous, includeDefault) {
				UserAPI userAPI = APILocator.getUserAPI();

				@Override
				public int getUserCount() {
					try {
						return new Long(userAPI.getCountUsersByNameOrEmailOrUserID(this.filter, this.includeAnonymous,
								this.includeDefault)).intValue();
					} catch (DotDataException e) {
						Logger.error(this, e.getMessage(), e);
						return 0;
					}
				}

				@Override
				public List<User> getUsers() {
					try {
						int page = (start / limit) + 1;
						int pageSize = limit;
						return userAPI.getUsersByNameOrEmailOrUserID(filter, page, pageSize, this.includeAnonymous,
								this.includeDefault);
					} catch (DotDataException e) {
						Logger.error(this, e.getMessage(), e);
						return new ArrayList<User>();
					}
				}

			}.perform();
			return results;
		}

		/**
		 * Returns a {@link Map} containing a list of dotCMS {@link User}
		 * objects that have a specific permission type on a specific Inode of
		 * an asset.
		 * 
		 * @param assetInode
		 *            - The Inode of a given asset in order to get the list of
		 *            users that have access to it.
		 * @param permission
		 *            - The permission type that users with access to the asset
		 *            Inode must have.
		 * @param query
		 *            - The string or characters that are part of the first
		 *            name, last name, or e-mail of the user.
		 * @param start
		 *            - For pagination purposes. Lower range of the set to
		 *            include in the query result.
		 * @param limit
		 *            - For pagination purposes. Upper range of the set to
		 *            include in the query result.
		 * @return A Map containing the user list and additional query
		 *         information.
		 */
		private Map<String, Object> processUserListWithPermissionOnInode(String assetInode, String permission, String query,
				int start, int limit) {
			Map<String, Object> results;
			try {
				int permissionType = Integer.parseInt(permission);
				String inode = assetInode;
				results = new UsersListTemplate(inode, permissionType, query, start, limit) {

					PermissionAPI perAPI = APILocator.getPermissionAPI();

					@Override
					public int getUserCount() {
						return perAPI.getUserCount(inode, permissionType, filter);
					}

					@Override
					public List<User> getUsers() {
						return perAPI.getUsers(inode, permissionType, filter, start, limit);
					}

				}.perform();
			} catch (NumberFormatException nfe) {
				Logger.warn(UserServiceImpl.class, String.format(
						"::getUsersList -> Invalid parameters inode(%s) permission(%s).", assetInode, permission));
				results = new HashMap<String, Object>(0);
			}
			return results;
		}
	}

}
