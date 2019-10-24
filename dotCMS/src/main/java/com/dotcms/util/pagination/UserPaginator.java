package com.dotcms.util.pagination;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;

/**
 * Paginator util for User
 */
public class UserPaginator implements PaginatorOrdered<Map<String, Object>> {
    private final UserAPI userAPI;
    private final RoleAPI roleAPI;

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
                                                    final String orderby, final OrderDirection direction, final Map<String, Object> extraParams) {

        return getItems(user, filter, limit, offset);
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
        try {
            final List<Role> roles =
            Collections.singletonList(roleAPI.loadBackEndUserRole());
            final List<String> rolesId = collectAdminRolesIfAny();
            final List<User> users = userAPI.getUsersByName(filter, roles ,offset, limit);
            final List<Map<String, Object>> usersMap = users.stream()
                    .map(userItem -> getUserObjectMap(rolesId, userItem))
                    .collect(Collectors.toList());

            final PaginatedArrayList<Map<String, Object>> result = new PaginatedArrayList<>();
            result.addAll(usersMap);
            result.setTotalResults(this.getTotalRecords(filter, roles));
            return result;
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    private List<String> collectAdminRolesIfAny() throws DotDataException {
        final List <Role> availableRoles = Arrays.asList(
                roleAPI.loadRoleByKey(Role.ADMINISTRATOR),
                roleAPI.loadCMSAdminRole()
        );
        return availableRoles.stream().filter(Objects::nonNull).map(Role::getId).collect(CollectionsUtils.toImmutableList());
    }

    @Nullable
    private Map<String, Object> getUserObjectMap(final List<String> rolesId, final User userItem) {
        try{
            final Map<String, Object> userMap = userItem.toMap();
            final String id = userItem.getUserId();
            userMap.put("requestPassword", roleAPI.doesUserHaveRoles(id, rolesId));

            return userMap;
        }catch(Exception e){
            return null;
        }
    }
}
