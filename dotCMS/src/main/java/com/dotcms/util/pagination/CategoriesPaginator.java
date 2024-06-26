package com.dotcms.util.pagination;

import com.liferay.util.StringPool;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.business.PaginatedCategories;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;

/**
 * Category paginator
 */
public class CategoriesPaginator implements PaginatorOrdered<Category> {

    private final CategoryAPI categoryAPI;

    @VisibleForTesting
    public CategoriesPaginator(final CategoryAPI categoryAPI){
        this.categoryAPI = categoryAPI;
    }

    public CategoriesPaginator(){
        this(APILocator.getCategoryAPI());
    }

    /**
     *
     * Return {@link Category} by pagiunation, you can et 3 parameters to set the way the search is done:
     *
     * - searchInAllLevels: A Boolean value. If TRUE, the search will look for the category at any level,
     * and the childrenCategories parameter is ignored.
     * - childrenCategories: A Boolean value. If FALSE, the search is limited to the top-level category. If TRUE,
     * the search is confined to the children of the category specified by the inode parameter.
     * - inode: If set and childrenCategories is TRUE, the search is limited to the children of the category with this inode.
     *
     * @param user        The {@link User} that is using this method.
     * @param filter      Allows you to add more conditions to the query via SQL code. It's very
     *                    important that you always sanitize the code in order to avoid SQL
     *                    injection attacks. The complexity of the allows SQL code will depend on
     *                    how the Paginator class is implemented.
     * @param limit       The maximum number of returned items in the result set, for pagination
     *                    purposes.
     * @param offset      The requested page number of the result set, for pagination purposes.
     * @param orderby     The order-by clause, which must always be sanitized as well.
     * @param direction   The direction of the order-by clause for the results.
     * @param extraParams A {@link Map} including any extra parameters that may be required by the
     *                    implementation class that are not part of this method's signature.
     *
     * @return
     */
    @Override
    public PaginatedArrayList<Category> getItems(final User user, final String filter, final int limit, final int offset,
                                                 final String orderby, final OrderDirection direction, final Map<String, Object> extraParams) {

        boolean childrenCategories = extraParams.containsKey("childrenCategories") ? (Boolean)extraParams.get("childrenCategories") : false;
        boolean searchInAllLevels = extraParams.containsKey("searchInAllLevels") ? (Boolean)extraParams.get("searchInAllLevels") : false;
        String inode = extraParams.containsKey("inode") ? String.valueOf(extraParams.get("inode")) : StringPool.BLANK;

        try {
            String categoriesSort = null;

            if (orderby != null) {
                categoriesSort = direction == OrderDirection.DESC ? "-" + orderby : orderby;
            }

            final PaginatedArrayList<Category> result = new PaginatedArrayList<>();
            final PaginatedCategories categories = searchInAllLevels ? searchAllLevels(user, filter, limit, offset) :
                searchInOneLevel(user, filter, inode, limit, offset, childrenCategories, categoriesSort);

            result.setTotalResults(categories.getTotalCount());

            if (categories.getCategories()!= null) {
                result.addAll(categories.getCategories());
            }

            return result;
        } catch (DotDataException | DotSecurityException e) {
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Search for categories at every level.
     *
     * @param user to check Permission
     * @param filter Search Filter to apply in key, name or variable's name
     * @param limit The maximum number of returned items in the result set, for pagination purposes.
     * @param offset The requested page number of the result set, for pagination purposes.
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private PaginatedCategories searchAllLevels(User user, String filter, int limit, int offset)
            throws DotDataException, DotSecurityException {
        return categoryAPI.findAll(filter, user, false, limit, offset);
    }

    /**
     * Search for a category at a single level. This could be either the top level or within the immediate children of any category.
     *
     * @param user to Check permission
     * @param filter Search Filter to apply in key, name or variable's name
     * @param inode If the parent inode is set, the search will be conducted only among the children of this category.
     *              Otherwise, the search will include only the top-level categories.
     * @param limit The maximum number of returned items in the result set, for pagination purposes.
     * @param offset The requested page number of the result set, for pagination purposes.
     * @param childrenCategories If it is false then Search only on the Top Level
     * @param orderby Field to order by
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private PaginatedCategories searchInOneLevel(final User user, final String filter, final String inode, final int limit,
                                                 final int offset, final boolean childrenCategories,
                                                 final String orderby) throws DotDataException, DotSecurityException {
        return childrenCategories == false ?
                categoryAPI.findTopLevelCategories(user, false, offset, limit, filter, orderby) :
                categoryAPI.findChildren(user, inode, false, offset, limit, filter, orderby);
    }
}
