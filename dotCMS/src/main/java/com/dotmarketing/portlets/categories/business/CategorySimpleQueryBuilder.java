package com.dotmarketing.portlets.categories.business;


import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import java.util.Map;

/**
 * Build a query to search {@link Category} in the category table using also the tree table.
 *
 * Query Syntax:
 *
 * <code>
 *     SELECT *, [count children]
 *     FROM category as c LEFT JOIN tree ON c.inode = tree.child [root filter] [ Name, Key, Variable Filter]
 *     ORDER BY :orderBy :direction"
 * </code>
 *
 * Where:
 *
 * 1. Count Children: This is a subquery that counts the number of children each Category has.
 * It is included only if CategorySearchCriteria.isCountChildren() is true.
 *
 * 2. Root Filter: This is a WHERE clause that depends on CategorySearchCriteria:
 *
 * - If CategorySearchCriteria.rootInode is set and CategorySearchCriteria.searchAllLevels is false,
 * use: 'tree.parent = ?' to filter categories that are children of the specified root Category.
 * - If CategorySearchCriteria.rootInode is not set and CategorySearchCriteria.searchAllLevels is false,
 * use: 'tree.child IS NULL' to filter only the top-level categories.
 *
 * 3. Filter Categories: Name, Key, Variable Filter: This is applied if {@link CategorySearchCriteria#filter} is not null. The filters can include:
 *
 * LOWER(category_name) LIKE ?
 * LOWER(category_key) LIKE ?
 * LOWER(category_velocity_var_name) LIKE ?
 */
public class CategorySimpleQueryBuilder extends CategoryQueryBuilder{

    private static final String QUERY_TEMPLATE = "SELECT c.* :countChildren  FROM category as c :treeJoin " +
            ":rootFilter :filterCategories ORDER BY :orderBy :direction";

    public CategorySimpleQueryBuilder(final CategorySearchCriteria searchCriteria) {
        super(searchCriteria);
    }


    public String build() throws DotDataException, DotSecurityException {

        final String rootFilter = getRootFilter();
        final String treeJoin = level != Level.ALL_LEVELS ? "LEFT JOIN tree ON c.inode = tree.child" : StringPool.BLANK;

        return StringUtils.format(QUERY_TEMPLATE, Map.of(
                "countChildren", getChildrenCount(),
                "rootFilter", rootFilter,
                "filterCategories", getFilterCategories(this.searchCriteria),
                "orderBy", searchCriteria.orderBy,
                "direction", searchCriteria.direction.toString(),
                "treeJoin", treeJoin
                )
        );
    }

    protected String getFilterCategories(final CategorySearchCriteria searchCriteria) {
        if (!UtilMethods.isSet(searchCriteria.filter)) {
            return StringPool.BLANK;
        }

        final String filterCategories = "(LOWER(category_name) LIKE ?  OR " +
                "LOWER(category_key) LIKE ?  OR " +
                "LOWER(category_velocity_var_name) LIKE ?)";

        if (this.level == Level.ALL_LEVELS) {
            return "WHERE " + filterCategories;
        }

        return " AND " + filterCategories;
    }

    private String getRootFilter() {
        if (this.level == Level.CHILDREN) {
            return "WHERE tree.parent = ?";
        } else if (this.level == Level.TOP) {
            return "WHERE tree.child IS NULL";
        }

        return StringPool.BLANK;
    }

}
