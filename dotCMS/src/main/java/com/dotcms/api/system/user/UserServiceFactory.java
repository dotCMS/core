package com.dotcms.api.system.user;

import com.dotcms.rest.api.v1.authentication.ResetPasswordTokenUtil;
import com.dotcms.rest.api.v1.authentication.url.UrlStrategy;
import com.dotcms.util.MessageAPI;
import com.dotcms.util.MessageAPIFactory;
import com.dotcms.util.UrlStrategyUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.user.ajax.UserAjax;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.SystemException;
import com.liferay.portal.UserEmailAddressException;
import com.liferay.portal.ejb.CompanyUtil;
import com.liferay.portal.ejb.UserUtil;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.util.Validator;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.getMapValue;
import static com.dotcms.util.ConversionUtils.toBoolean;
import static com.dotcms.util.ConversionUtils.toInt;

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

		private final UserAPI userAPI;
		private final PermissionAPI permissionAPI;
		private final MessageAPI messageService;

		/**
		 * Private class constructor.
		 */
		private UserServiceImpl() {

			this.userAPI 			= APILocator.getUserAPI();
			this.permissionAPI 		= APILocator.getPermissionAPI();
			this.messageService     = MessageAPIFactory.getInstance().getMessageService();
		}


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
	    protected String roleId;
			protected int start;
			protected int limit;
			protected boolean includeAnonymous;
			protected boolean includeDefault;
			
			final ArrayList<Map<String, String>> EMPTY_MAP_LIST = new ArrayList<>();

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
				this(inode, permissionType, filter, start, limit, false, true, "all");
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
					boolean includeAnonymous, boolean includeDefault, String roleId) {
				this.inode = inode;
				this.permissionType = permissionType;
				this.filter = filter;
				this.start = start;
				if (limit > 0) {
					this.limit = limit;
				} else {
					this.limit = 1;
				}
				this.roleId=roleId;
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
				Map<String, Object> results = new HashMap<>(2);
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
						list = new ArrayList<>(pageSize);
						for (User aUser : users) {
							final Map<String, String> aRecord = Map.of(
									"id", aUser.getUserId(), 
									"type", USER_TYPE_VALUE, 
									"name", UtilMethods.isSet(aUser.getFullName()) ? aUser.getFullName() : " ",
									"emailaddress", UtilMethods.isSet(aUser.getEmailAddress()) ? aUser.getEmailAddress() : " ");
							list.add(aRecord);
						}
					} else {
						list = EMPTY_MAP_LIST;
					}
				} catch (Exception ex) {
					Logger.warn(UserAjax.class, "::processUsersList -> Could not process list of users.");
					list = new ArrayList<>(0);
				}
				results.put("data", list);
				results.put("total", totalItemCount);
				return results;
			}
		}

		@Override
		public Map<String, Object> getUsersList(String assetInode, String permission, Map<String, String> params)
				throws Exception {
			final int start = toInt("start", params, 0);
			final int limit = toInt("limit", params, 100);
			final String query = getMapValue(params, "query", StringUtils.EMPTY);
			final String showUserType = params.getOrDefault("showUsers", "all");
			
	    String requiredRoleId=null;
	    if("backEnd".equalsIgnoreCase(showUserType)) {
	      requiredRoleId=APILocator.getRoleAPI().loadBackEndUserRole().getId();
	    }else if("frontEnd".equalsIgnoreCase(showUserType)) {
	      requiredRoleId=APILocator.getRoleAPI().loadFrontEndUserRole().getId();
	    }
	    
			
			
			
			final boolean includeAnonymous = toBoolean("includeAnonymous", params, false);
			// Defaults to "true" for backwards compatibility
			final boolean includeDefault = toBoolean("includeDefault", params, true);
			Map<String, Object> results;
			if ((InodeUtils.isSet(assetInode) && !"0".equals(assetInode)) && (UtilMethods.isSet(permission) && !"0".equals(permission))) {
				results = processUserListWithPermissionOnInode(assetInode, permission, query, start, limit);
			} else {
				results = processUserList(query, start, limit, includeAnonymous, includeDefault, requiredRoleId);
			}
			return results;
		}

		@Override
		public User findUserByCompanyAndEmail(final String companyId,
											  final  String emailAddress) {

			User user = null;

			if (!UtilMethods.isSet(companyId) || !UtilMethods.isSet(emailAddress)) {

				throw new UserException("User can not be null");
			}

			try {

				if (Logger.isDebugEnabled(this.getClass())) {

					Logger.debug(this, "Finding an user by companyId: " + companyId +
							", emailAddress: " + emailAddress);
				}

				user = UserUtil.findByC_EA(companyId, emailAddress);
			} catch (NoSuchUserException | SystemException e) {

				Logger.error(this, e.getMessage(), e);
				throw new UserException(e);
			}

			return user;
		} // findUserByCompanyAndEmail.

		@Override
		public User update(final User user) {

			User userUpdated = null;

			if (!UtilMethods.isSet(user)) {

				throw new UserException("User can not be null");
			}

			try {

				if (Logger.isDebugEnabled(this.getClass())) {

					Logger.debug(this, "Updating the user: " + user.getUserId());
				}

				userUpdated = UserUtil.update(user);
			} catch (SystemException e) {

				Logger.error(this, e.getMessage(), e);
				throw new UserException(e);
			}

			return userUpdated;
		} // update.

		@Override
		public void sendResetPassword(final String companyId,
									  final String emailAddress,
									  final Locale locale) throws UserEmailAddressException, NoSuchUserException {

			this.sendResetPassword(companyId, emailAddress, locale, ANGULAR_RESET_PASSWORD_URL_STRATEGY);
		} // sendResetPassword.

		@Override
		public void sendResetPassword(final String companyId,
									  final String emailAddressParam,
									  final Locale locale,
									  final UrlStrategy resetPasswordUrlStrategy) throws UserEmailAddressException, NoSuchUserException {

			final User user;
			final String emailAddress;
			final String token;
			final Company company;
			final String url;
			final String body;
			final String subject;

			if (!UtilMethods.isSet(emailAddressParam)) {

				throw new UserEmailAddressException("Email is not set");
			}

			emailAddress = emailAddressParam.trim().toLowerCase();

			if (!Validator.isEmailAddress(emailAddress)) {

				throw new UserEmailAddressException("Invalid email format");
			}

			try {

				user = this.findUserByCompanyAndEmail(companyId, emailAddress);

				// we use the ICQ field to store the token:timestamp of the
				// password reset request we put in the email
				// the timestamp is used to set an expiration on the token
				if (Logger.isDebugEnabled(UserServiceFactory.class)) {

					Logger.debug(UserServiceFactory.class, "Generating the token for reset password");
				}

				token = ResetPasswordTokenUtil.createToken();
				user.setIcqId(token);

				this.update(user);

				// Send new password
				company = CompanyUtil.findByPrimaryKey(companyId);

				url = UrlStrategyUtil.getURL(company,
						Map.of(UrlStrategy.USER, user, UrlStrategy.TOKEN, token, UrlStrategy.LOCALE, locale),
						resetPasswordUrlStrategy);
				body    = LanguageUtil.format(locale, "reset-password-email-body", url, false);
				subject = LanguageUtil.get(locale, "reset-password-email-subject");
				this.messageService.sendMail(user, company, subject, body);
			} catch(UserException e){
				throw new NoSuchUserException(e);
			}catch (Exception ioe) {

				Logger.error(this, ioe.getMessage(), ioe);
				throw new UserEmailAddressException(ioe);
			}
		} // sendResetPassword.

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
		private Map<String, Object> processUserList(String query, int start, int limit,boolean includeAnonymous,
				boolean includeDefault, String roleId) {
			Map<String, Object> results = new UsersListTemplate("", 0, query, start, limit, includeAnonymous, includeDefault,roleId) {

			  
				@Override
				public int getUserCount() {
					try {
						return toInt(userAPI.getCountUsersByNameOrEmailOrUserID(this.filter, this.includeAnonymous,
								this.includeDefault,roleId), 0);
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
								this.includeDefault,roleId);
					} catch (DotDataException e) {
						Logger.error(this, e.getMessage(), e);
						return new ArrayList<>();
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
		@SuppressWarnings("unchecked")
		private Map<String, Object> processUserListWithPermissionOnInode(String assetInode, String permission, String query,
				int start, int limit) {
			Map<String, Object> results;
			try {
				final int permissionType = toInt(permission, 0);
				results = new UsersListTemplate(assetInode, permissionType, query, start, limit) {

					@Override
					public int getUserCount() {
						return permissionAPI.getUserCount(inode, permissionType, filter);
					}

					@Override
					public List<User> getUsers() {
						return permissionAPI.getUsers(inode, permissionType, filter, start, limit);
					}

				}.perform();
			} catch (Exception e) {
				Logger.warn(UserServiceImpl.class, String.format(
						"::getUsersList -> Invalid parameters inode(%s) permission(%s).", assetInode, permission));
				results = Collections.EMPTY_MAP;
			}
			return results;
		}
	}

}
