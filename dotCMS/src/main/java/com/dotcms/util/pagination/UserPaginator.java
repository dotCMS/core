package com.dotcms.util.pagination;

import com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.api.v1.system.role.SmallRoleView;
import com.dotcms.rest.api.v1.user.UserResourceHelper;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class allows you to retrieve a paginated list of {@link User} objects in your current dotCMS instance.
 *
 * @author Freddy Rodriguez
 * @since Jul 26th, 2017
 */
public class UserPaginator implements PaginatorOrdered<Map<String, Object>> {

    private final UserAPI userAPI;
    private final RoleAPI roleAPI;
    private final UserResourceHelper helper = UserResourceHelper.getInstance();

    public static final String QUERY_PARAM = "query";
    public static final String INCLUDE_ANONYMOUS = "includeanonymous";
    public static final String INCLUDE_DEFAULT = "includedefault";
    public static final String ASSET_INODE_PARAM = "assetinode";
    public static final String PERMISSION_PARAM = "permission";
    public static final String ROLES_PARAM = "roles";
    public static final String ROLE_KEY_PARAM = "roleKey";
    public static final String REMOVE_CURRENT_USER_PARAM = "removeCurrentUser";
    public static final String REQUEST_PASSWORD_PARAM = "requestPassword";
    /** Opt-in flag: when {@code true}, every item carries a {@link #ROLES_PARAM} list of its direct roles. */
    public static final String INCLUDE_ROLES_PARAM = "includeRoles";

    @VisibleForTesting
    public UserPaginator(UserAPI userApi, RoleAPI roleAPI){
        this.userAPI = userApi;
        this.roleAPI = roleAPI;
    }

    public UserPaginator(){
        this(APILocator.getUserAPI(), APILocator.getRoleAPI());
    }

    /**
     * Return the total of users with name equals to nameFilter, applying the same filtering
     * params as the item query so the count stays consistent with the returned page.
     * @param nameFilter
     * @return
     *
     */
    private long getTotalRecords(final String nameFilter, final List<Role> roles,
                                 final UserAPI.FilteringParams filteringParams) {
        try {
            return userAPI.getCountUsersByName(nameFilter, roles, filteringParams);
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    @Override
    public PaginatedArrayList<Map<String, Object>> getItems(final User user, final String filter, final int limit, final int offset,
                                                    final String orderBy, final OrderDirection direction, final Map<String, Object> extraParams) {
        try {
            final List<Role> roles = (List<Role>) extraParams.get(ROLES_PARAM);
            final UserAPI.FilteringParams filteringParams = new UserAPI.FilteringParams.Builder().build(extraParams);
            final boolean includeRoles = (boolean) extraParams.getOrDefault(INCLUDE_ROLES_PARAM, false);
            final String usersRootRoleId = includeRoles ? this.usersRootRoleId() : null;
            final List<Map<String, Object>> usersMap = new ArrayList<>();
            if (UtilMethods.isSet(extraParams.get(ASSET_INODE_PARAM)) && UtilMethods.isSet(extraParams.get(PERMISSION_PARAM))) {
                final List<User> userList = helper.getUsersByAssetAndPermissionType(filter, offset, limit,
                        extraParams.get(ASSET_INODE_PARAM).toString(), extraParams.get(PERMISSION_PARAM).toString());
                for (final User userItem : userList) {
                    usersMap.add(this.toItem(userItem, null, includeRoles, usersRootRoleId));
                }
            } else {
                final List<User> users = userAPI.getUsersByName(filter, roles, offset, limit, filteringParams);
                if ((boolean) CollectionsUtils.getMapValue(extraParams, REMOVE_CURRENT_USER_PARAM, false)) {
                    // Removes user making the request from the list
                    users.remove(user);
                }
                final List<String> adminRoleIds = (boolean) extraParams.getOrDefault(REQUEST_PASSWORD_PARAM, false) ?
                                                     collectAdminRolesIfAny() : new ArrayList<>();
                for (final User userItem : users) {
                    usersMap.add(this.toItem(userItem, adminRoleIds, includeRoles, usersRootRoleId));
                }
            }
            final PaginatedArrayList<Map<String, Object>> result = new PaginatedArrayList<>();
            result.addAll(usersMap);
            result.setTotalResults(this.getTotalRecords(filter, roles, filteringParams));
            return result;
        } catch (final Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Return a list os User Map.
     *
     * @param user user to filter
     * @param filter extra filter parameter
     * @param limit Number of items to return
     * @param offset offset
     * @return
     */
    @Override
    public PaginatedArrayList<Map<String, Object>> getItems(final User user, final String filter, final int limit, final int offset) {
        return getItems(user, filter, limit, offset, null, null, Map.of());
    }

    /**
     * Utility method that creates a list with the IDs of the CMS Administrator Roles in dotCMS.
     *
     * @return The list of CMS Administrator Role IDs.
     *
     * @throws DotDataException An error occurred when accessing the data source.
     */
    private List<String> collectAdminRolesIfAny() throws DotDataException {
        final List <Role> availableRoles = Arrays.asList(
                roleAPI.loadRoleByKey(Role.ADMINISTRATOR),
                roleAPI.loadCMSAdminRole()
        );
        return availableRoles.stream().filter(Objects::nonNull).map(Role::getId).collect(CollectionsUtils.toImmutableList());
    }

    /**
     * Builds the paginated item for a User: its data map, the optional {@link #REQUEST_PASSWORD_PARAM} flag and,
     * when requested, its direct Roles under the {@link #ROLES_PARAM} key. Role loading is deliberately kept out of
     * {@link #addRequestPasswordAttr(User, List)} so that a role-lookup failure surfaces as an error instead of
     * silently turning the item into an empty map.
     *
     * @param user            The {@link User} being transformed.
     * @param adminRoleIds    The optional CMS Administrator Role IDs used to compute {@link #REQUEST_PASSWORD_PARAM}.
     * @param includeRoles    If {@code true}, the item carries the User's direct Roles.
     * @param usersRootRoleId The ID of the {@code cms_users} root Role, used to leave personal Roles out; may be
     *                        {@code null}, in which case no Role is excluded.
     *
     * @return The item {@link Map}.
     *
     * @throws DotDataException An error occurred when loading the User's Roles.
     */
    private Map<String, Object> toItem(final User user, final List<String> adminRoleIds, final boolean includeRoles,
                                       final String usersRootRoleId) throws DotDataException {
        final Map<String, Object> userMap = this.addRequestPasswordAttr(user, adminRoleIds);
        if (includeRoles && !userMap.isEmpty()) {
            userMap.put(ROLES_PARAM, this.directRolesOf(user, usersRootRoleId));
        }
        return userMap;
    }

    /**
     * Returns the Roles the given User holds <b>directly</b> -- i.e., rows in {@code users_cms_roles} -- as minimal
     * {@link SmallRoleView}s. Roles reached through the hierarchy (children of a held Role) are excluded, and so is
     * the User's own personal Role, mirroring the legacy Users portlet Roles tab ({@code UserAjax#getUserRoles}).
     * <p>Personal Roles are recognized the way the legacy code did -- their ID-based {@code DBFQN} hangs under the
     * {@code cms_users} root Role -- rather than through {@link Role#isUser()}, whose name-based {@code FQN} check
     * would also swallow any ordinary root Role whose name happens to start with {@code "User"}.</p>
     *
     * @param user            The {@link User}.
     * @param usersRootRoleId The ID of the {@code cms_users} root Role, or {@code null} to skip the exclusion.
     *
     * @return The list of direct Roles, possibly empty.
     *
     * @throws DotDataException An error occurred when accessing the data source.
     */
    private List<SmallRoleView> directRolesOf(final User user, final String usersRootRoleId) throws DotDataException {
        return this.roleAPI.loadRolesForUser(user.getUserId(), false).stream()
                .filter(role -> !isPersonalRole(role, usersRootRoleId))
                .map(role -> new SmallRoleView(role.getName(), role.getId(), role.getRoleKey(), false))
                .collect(Collectors.toList());
    }

    private static boolean isPersonalRole(final Role role, final String usersRootRoleId) {
        return UtilMethods.isSet(usersRootRoleId) && UtilMethods.isSet(role.getDBFQN())
                && role.getDBFQN().contains(usersRootRoleId);
    }

    /**
     * Resolves the ID of the {@code cms_users} root Role every personal Role hangs from. It is a system Role that is
     * always present; should it be missing, a warning is logged and {@code null} is returned so the roles list is
     * still produced, just without the personal-Role exclusion.
     *
     * @return The root Role ID, or {@code null} if it cannot be resolved.
     *
     * @throws DotDataException An error occurred when accessing the data source.
     */
    private String usersRootRoleId() throws DotDataException {
        final Role usersRoot = this.roleAPI.loadRoleByKey(RoleAPI.USERS_ROOT_ROLE_KEY);
        if (null == usersRoot || !UtilMethods.isSet(usersRoot.getId())) {
            Logger.warn(this, "Users root role '" + RoleAPI.USERS_ROOT_ROLE_KEY
                    + "' not found; personal roles will not be excluded from the users list");
            return null;
        }
        return usersRoot.getId();
    }

    /**
     * Utility method that transforms a {@link User} object into its data map. Additionally, if such a User has any of
     * the specified Role IDs assigned to it, the {@link #REQUEST_PASSWORD_PARAM} attribute will be added to the User
     * data map and set to {@code true}.
     *
     * @param rolesId The optional list of Roles IDs.
     * @param user    The {@link User} being transformed into a Map.
     *
     * @return The {@link Map} containing the User data and the potential {@link #REQUEST_PASSWORD_PARAM} attribute.
     */
    private Map<String, Object> addRequestPasswordAttr(final User user, final List<String> rolesId) {
        try {
            final Map<String, Object> userMap = user.toMap();
            if (UtilMethods.isSet(rolesId)) {
                userMap.put(REQUEST_PASSWORD_PARAM, roleAPI.doesUserHaveRoles(user.getUserId(), rolesId));
            }
            return userMap;
        } catch (final Exception e) {
            return Collections.emptyMap();
        }
    }

}
