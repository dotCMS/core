package com.dotcms.util.pagination;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.api.v1.user.UserResourceHelper;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
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
 * Paginator util for User
 */
public class UserPaginator implements PaginatorOrdered<Map<String, Object>> {

    private final UserAPI userAPI;
    private final RoleAPI roleAPI;
    private final UserResourceHelper helper = UserResourceHelper.getInstance();

    public static final String START_PARAM = "start";
    public static final String LIMIT_PARAM = "limit";
    public static final String QUERY_PARAM = "query";
    public static final String ASSET_INODE_PARAM = "assetinode";
    public static final String PERMISSION_PARAM = "permission";
    public static final String ROLES_PARAM = "roles";
    public static final String REMOVE_CURRENT_USER_PARAM = "removeCurrentUser";
    public static final String REQUEST_PASSWORD_PARAM = "requestPassword";

    @VisibleForTesting
    public UserPaginator(UserAPI userApi, RoleAPI roleAPI){
        this.userAPI = userApi;
        this.roleAPI = roleAPI;
    }

    public UserPaginator(){
        this(APILocator.getUserAPI(), APILocator.getRoleAPI());
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
    public PaginatedArrayList<Map<String, Object>> getItems(final User user, final String filter, int limit, int offset,
                                                    final String orderBy, final OrderDirection direction, final Map<String, Object> extraParams) {
        try {
            final List<Role> roles = (List<Role>) extraParams.get(ROLES_PARAM);
            final List<Map<String, Object>> usersMap;
            if (UtilMethods.isSet(extraParams.get(ASSET_INODE_PARAM)) && UtilMethods.isSet(extraParams.get(PERMISSION_PARAM))) {
                final List<User> userList = helper.getUsersByAssetAndPermissionType(filter, offset, limit,
                        extraParams.get(ASSET_INODE_PARAM).toString(), extraParams.get(PERMISSION_PARAM).toString());
                usersMap = userList.stream().map(this::userToMap).collect(Collectors.toList());
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
                        users.stream().map(userItem -> this.addRequestPasswordAttr(userItem, adminRoleIds)).collect(Collectors.toList());
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
     * Utility method that transforms a {@link User} object into its data map.
     *
     * @param user The {@link User} object.
     *
     * @return The {@link Map} containing the User data.
     */
    private Map<String, Object> userToMap(final User user) {
        return addRequestPasswordAttr(user, null);
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
