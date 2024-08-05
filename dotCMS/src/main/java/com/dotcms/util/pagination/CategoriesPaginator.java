package com.dotcms.util.pagination;

import com.dotmarketing.portlets.categories.business.CategoryFactory;
import com.dotmarketing.portlets.categories.business.CategorySearchCriteria;
import com.dotmarketing.util.UtilMethods;
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
     * Returns a {@link Category} by pagination. You can set three parameters to customize the search:
     * - searchInAllLevels: A Boolean value.
     * If TRUE, the search will include categories at any level, ignoring the childrenCategories parameter.
     * - childrenCategories: A Boolean value.
     * If FALSE, the search is limited to the top-level category.
     * If TRUE, the search is confined to the children of the category specified by the inode parameter.
     * -inode: If it is set and childrenCategories is TRUE, the search is limited to the children of the category with this inode
     * If it is set searchInAllLevels is TRUE, the search starts at this category and goes through its children recursively .
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

        boolean childrenCategories = extraParams.containsKey("childrenCategories") && (Boolean)extraParams.get("childrenCategories");
        boolean searchInAllLevels = extraParams.containsKey("searchInAllLevels") && (Boolean)extraParams.get("searchInAllLevels");
        String inode = extraParams.containsKey("inode") ? String.valueOf(extraParams.get("inode")) : StringPool.BLANK;
        boolean showChildrenCount = extraParams.containsKey("showChildrenCount") && (Boolean) extraParams.get("showChildrenCount");
        boolean parentList = extraParams.containsKey("parentList") && (Boolean) extraParams.get("parentList");

        try {

            final CategorySearchCriteria searchingCriteria = new CategorySearchCriteria.Builder()
                    .filter(filter)
                    .limit(limit)
                    .offset(offset)
                    .orderBy(UtilMethods.isSet(orderby) ? orderby : "category_name")
                    .direction(direction != null ? direction : OrderDirection.ASC )
                    .rootInode(inode)
                    .setCountChildren(showChildrenCount)
                    .parentList(parentList)
                    .searchAllLevels(searchInAllLevels || childrenCategories)
                    .build();

            final PaginatedCategories categories = categoryAPI.findAll(searchingCriteria, user, false);

            final PaginatedArrayList<Category> result = new PaginatedArrayList<>();
            result.setTotalResults(categories.getTotalCount());

            if (categories.getCategories()!= null) {
                result.addAll(categories.getCategories());
            }

            return result;
        } catch (DotDataException | DotSecurityException e) {
            throw new DotRuntimeException(e);
        }
    }
}
