package com.dotcms.util.pagination;

import com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.api.v1.user.UserResourceHelper;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
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
    private final Supplier<LayoutAPI> layoutAPI;
    private final UserResourceHelper helper = UserResourceHelper.getInstance();

    /**
     * Keys of the user data map that every caller receives for each listed user: enough to identify the user and
     * display it (pickers, filters, assignment lists). The remaining keys of {@link User#toMap()} -- account state
     * such as last login, failed attempts, flags and dates -- are only included for callers who can administer
     * users (see {@link #canReadFullProfile(User)}).
     */
    public static final Set<String> DIRECTORY_KEYS = Set.of("userId", "id", "firstName", "lastName", "fullName",
            "name", "emailAddress", "emailaddress", "gravitar", "type");

    public static final String QUERY_PARAM = "query";
    public static final String INCLUDE_ANONYMOUS = "includeanonymous";
    public static final String INCLUDE_DEFAULT = "includedefault";
    public static final String ASSET_INODE_PARAM = "assetinode";
    public static final String PERMISSION_PARAM = "permission";
    public static final String ROLES_PARAM = "roles";
    public static final String REMOVE_CURRENT_USER_PARAM = "removeCurrentUser";
    public static final String REQUEST_PASSWORD_PARAM = "requestPassword";

    @VisibleForTesting
    public UserPaginator(final UserAPI userApi, final RoleAPI roleAPI) {
        this(userApi, roleAPI, APILocator::getLayoutAPI);
    }

    /**
     * Creates a paginator with every collaborator supplied by the caller.
     *
     * @param userApi   The {@link UserAPI} that lists and counts users.
     * @param roleAPI   The {@link RoleAPI} that resolves roles.
     * @param layoutAPI Supplies the {@link LayoutAPI} used to tell whether the calling user can administer users;
     *                  resolved lazily so that callers who are CMS Administrators never need it.
     */
    @VisibleForTesting
    public UserPaginator(final UserAPI userApi, final RoleAPI roleAPI, final Supplier<LayoutAPI> layoutAPI) {
        this.userAPI = userApi;
        this.roleAPI = roleAPI;
        this.layoutAPI = layoutAPI;
    }

    public UserPaginator(){
        this(APILocator.getUserAPI(), APILocator.getRoleAPI());
    }

    /**
     * Tells whether the calling user receives the full data map of every listed user, or only the
     * {@link #DIRECTORY_KEYS}. The full profile goes to CMS Administrators and to users with access to the Users
     * portlet, the portlet that displays the account-state fields. CMS Administrators short-circuit before the
     * {@link LayoutAPI} supplier is ever resolved; only non-admin callers trigger a portlet lookup.
     *
     * @param caller The user requesting the list; {@code null} is treated as a caller without privileges.
     *
     * @return {@code true} if every key of {@link User#toMap()} is returned for each listed user.
     *
     * @throws DotDataException An error occurred when resolving the caller's portlet access.
     */
    private boolean canReadFullProfile(final User caller) throws DotDataException {
        if (null == caller) {
            return false;
        }
        if (caller.isAdmin()) {
            return true;
        }
        return this.layoutAPI.get().doesUserHaveAccessToPortlet(PortletID.USERS.toString(), caller);
    }

    /**
     * Return the total of users with name equals to nameFilter.
     * @param nameFilter
     * @return
     *
     */
    private long getTotalRecords(final String nameFilter, final List<Role> roles) {
        try {
            return userAPI.getCountUsersByName(nameFilter, roles);
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    @Override
    public PaginatedArrayList<Map<String, Object>> getItems(final User user, final String filter, final int limit, final int offset,
                                                    final String orderBy, final OrderDirection direction, final Map<String, Object> extraParams) {
        try {
            final List<Role> roles = (List<Role>) extraParams.get(ROLES_PARAM);
            final boolean fullProfile = this.canReadFullProfile(user);
            final List<Map<String, Object>> usersMap;
            if (UtilMethods.isSet(extraParams.get(ASSET_INODE_PARAM)) && UtilMethods.isSet(extraParams.get(PERMISSION_PARAM))) {
                final List<User> userList = helper.getUsersByAssetAndPermissionType(filter, offset, limit,
                        extraParams.get(ASSET_INODE_PARAM).toString(), extraParams.get(PERMISSION_PARAM).toString());
                usersMap = userList.stream().map(userItem -> this.toItem(userItem, null, fullProfile)).collect(Collectors.toList());
            } else {
                final UserAPI.FilteringParams filteringParams = new UserAPI.FilteringParams.Builder().build(extraParams);
                final List<User> users = userAPI.getUsersByName(filter, roles, offset, limit, filteringParams);
                if ((boolean) CollectionsUtils.getMapValue(extraParams, REMOVE_CURRENT_USER_PARAM, false)) {
                    // Removes user making the request from the list
                    users.remove(user);
                }
                final List<String> adminRoleIds = (boolean) extraParams.getOrDefault(REQUEST_PASSWORD_PARAM, false) ?
                                                     collectAdminRolesIfAny() : new ArrayList<>();
                usersMap =
                        users.stream().map(userItem -> this.toItem(userItem, adminRoleIds, fullProfile)).collect(Collectors.toList());
            }
            final PaginatedArrayList<Map<String, Object>> result = new PaginatedArrayList<>();
            result.addAll(usersMap);
            result.setTotalResults(this.getTotalRecords(filter, roles));
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
     * Utility method that transforms a {@link User} object into its list item.
     *
     * @param user        The {@link User} object.
     * @param rolesId     The optional list of Roles IDs, see {@link #addRequestPasswordAttr(User, List)}.
     * @param fullProfile If {@code false}, the item keeps only the {@link #DIRECTORY_KEYS} of the User's data map
     *                    (plus {@link #REQUEST_PASSWORD_PARAM} when computed).
     *
     * @return The {@link Map} containing the User data.
     */
    private Map<String, Object> toItem(final User user, final List<String> rolesId, final boolean fullProfile) {
        final Map<String, Object> userMap = this.addRequestPasswordAttr(user, rolesId);
        if (!fullProfile && !userMap.isEmpty()) {
            userMap.keySet().removeIf(key -> !DIRECTORY_KEYS.contains(key) && !REQUEST_PASSWORD_PARAM.equals(key));
        }
        return userMap;
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
