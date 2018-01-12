package com.dotcms.util.pagination;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.list;

/**
 * Paginator util for User
 */
public class UserPaginator implements Paginator<Map<String, Object>> {
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
    private long getTotalRecords(final String nameFilter) {
        try {
            return userAPI.getCountUsersByName(nameFilter);
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
            final List<String> rolesId = list( roleAPI.loadRoleByKey(Role.ADMINISTRATOR).getId(), roleAPI.loadCMSAdminRole().getId() );
            final List<User> users = userAPI.getUsersByName(filter, offset, limit, user, false);
            final List<Map<String, Object>> usersMap = users.stream()
                    .map(userItem -> getUserObjectMap(rolesId, userItem))
                    .collect(Collectors.toList());

            final PaginatedArrayList<Map<String, Object>> result = new PaginatedArrayList();
            result.addAll(usersMap);
            result.setTotalResults(this.getTotalRecords(filter));
            return result;
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
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
