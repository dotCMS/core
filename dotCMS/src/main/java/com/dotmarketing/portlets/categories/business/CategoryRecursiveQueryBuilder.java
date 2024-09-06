package com.dotmarketing.portlets.categories.business;

import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.categories.model.ShortCategory;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Build a query using the 'WITH RECURSIVE' SQL clause.
 * The WITH RECURSIVE clause in SQL is used to define Common Table Expressions (CTEs) that can reference themselves.
 * This feature is particularly useful for working with hierarchical or recursive data structures,
 * such as organizational charts or tree structures, where the depth of the hierarchy is not known in advance,
 * like a categories tree.
 *
 * Here is a basic structure of a query using the WITH RECURSIVE clause that is build using this class:
 *
 * <code>
 *  WITH RECURSIVE CategoryHierarchy AS (
 *       -- Anchor Member
 *      SELECT c.*, [list parent field] FROM Category c [tree table join] [root filter]
 *  UNION ALL
 *      -- Recursive Member
 *      SELECT c.*, ch.level + 1 AS level, [list parent field]
 *      FROM Category c JOIN tree t ON c.inode = t.child JOIN CategoryHierarchy ch ON t.parent = ch.inode
 * )
 * -- Result Member
 * SELECT *, [list parent field], [count children field] FROM CategoryHierarchy ch WHERE level = (SELECT MAX(level) FROM CategoryHierarchy WHERE inode = ch.inode)
 * [level filter] [name, key, variable filter]
 * ORDER BY [order by field] [direction]
 * </code>
 *
 * <h1>Explanation of the WITH RECURSIVE Clause</h1>
 * The WITH RECURSIVE clause consists of several parts:
 * 1. Anchor Member: This is the initial query that forms the base of the recursion. It sets the starting point.
 * 2. Recursive Member: This part references the CTE itself and defines how to move from one level of recursion to the next.
 * 3. Result Member: This sets the fields and records returned by the query
 *
 * <h1>Dynamic Parts of the Query</h1>
 * 1. List Parent Field: This field calculates and returns the parent list and is included if CategorySearchCriteria.isParentList() is true.
 * how the root SQL is changed depends on the member on the RECURSIVE clause:
 * - Anchor Member: A path field is added to the query. How this field is calculated depends on
 * whether CategorySearchCriteria.rootInode is set:
 * If rootInode is set: The json_build_object SQL function is used to create a JSON object for each Category returned by
 * the Anchor Member query. This JSON object includes the key, name, and variable values of the Category.
 * If rootInode is not set: The path field is an array of JSON objects representing the categories from the direct parent
 * up through the tree to the top-level category.
 *
 * - Recursive member: A field to concat the previuos value with the current value on the register proceeded.
 *
 * - Result member: include the listParent calculate field on the result returned.
 *
 * 2. Tree Table Join: This is included if CategorySearchCriteria.rootInode is not set and CategorySearchCriteria.searchAllLevels is false.
 * It means searching only the TOP LEVEL categories, requiring a join to the tree table and a WHERE clause to filter these categories.
 *
 * 3. Root Filter: This defines the starting point if CategorySearchCriteria.rootInode is set, with the WHERE clause 'inode = ?'.
 *
 * 4. Count Children Field: This is a subquery that counts the number of children, included if CategorySearchCriteria.isCountChildren() is true.
 *
 * 5. Level Filter: The 'Anchor Member' and 'Recursive Member' include a 'level' field calculated through recursion.
 * If the Category is the starting point (i.e., CategorySearchCriteria.rootInode is set), the level is 1.
 * If not set, the level for each TOP LEVEL Category is 1. This field is used to filter categories according to the CategorySearchCriteria values:
 *
 * If rootInode is not set and allLevels is false, we look only at TOP LEVEL categories, filtering by level = 1.
 * If rootInode is set and allLevels is false, we filter by level = 2, as we want the children of the root Category.
 *
 * 6. Name, Key, Variable Filter: This is added if {@link CategorySearchCriteria#filter} is not null. The filter could be:
 *
 * LOWER(category_name) LIKE ?
 * LOWER(category_key) LIKE ?
 * LOWER(category_velocity_var_name) LIKE ?
 *
 * This structure allows for dynamic and flexible querying of hierarchical data using SQL's recursive capabilities.
 *
 * Finally this part 'level = (SELECT MAX(level) FROM CategoryHierarchy WHERE inode = ch.inode  by inode)' allow remove duplicated
 */
public class CategoryRecursiveQueryBuilder extends CategoryQueryBuilder{

    private boolean parentList;
    private final static String QUERY_TEMPLATE = "WITH RECURSIVE CategoryHierarchy AS ( " +
            "SELECT c.*, 1 AS level :parentList_1 " +
            "FROM Category c :levelFilter_1 :rootFilter " +
            "UNION ALL " +
            "SELECT c.*,ch.level + 1 AS level :parentList_2 " +
            "FROM Category c JOIN tree t ON c.inode = t.child JOIN CategoryHierarchy ch ON t.parent = ch.inode " +
            ") " +
            "SELECT distinct * :parentList_3 :countChildren FROM CategoryHierarchy c " +
            "WHERE level = (SELECT MAX(level) FROM CategoryHierarchy WHERE inode = c.inode group by inode) :levelFilter_2 :filterCategories " +
            "ORDER BY :orderBy :direction";

    public CategoryRecursiveQueryBuilder(final CategorySearchCriteria searchCriteria) {
        super(searchCriteria);
        this.parentList = searchCriteria.isParentList();
    }


    public String build() throws DotDataException, DotSecurityException {
        final String levelFilter_1 = this.level == Level.TOP ?
                "LEFT JOIN tree ON c.inode = tree.child WHERE tree.child IS NULL" : StringPool.BLANK;
        final String levelFilter_2 = getLevelFilter2();

        final String rootFilter = getRootFilter();

        final String parentList_1 = getListParentRootValue(this.searchCriteria);

        final String parentList_2 =	this.parentList ?
                ", CONCAT(ch.path, ',', json_build_object('inode', c.inode, 'name', c.category_name, 'key', c.category_key)::varchar) AS path" :
                StringPool.BLANK;

        final String parentList_3 = this.parentList ? ", c.path" : StringPool.BLANK;

        return StringUtils.format(QUERY_TEMPLATE, Map.of(
            "rootFilter", rootFilter,
            "levelFilter_1", levelFilter_1,
            "levelFilter_2", levelFilter_2,
            "countChildren", getChildrenCount(),
            "parentList_1", parentList_1,
            "parentList_2", parentList_2,
            "parentList_3", parentList_3,
            "filterCategories", getFilterCategories(),
            "orderBy", searchCriteria.orderBy,
            "direction", searchCriteria.direction.toString()
        ));
    }

    protected String getFilterCategories() {
        return "AND (LOWER(category_name) LIKE ?  OR " +
                    "LOWER(category_key) LIKE ?  OR " +
                    "LOWER(category_velocity_var_name) LIKE ?)";
    }

    private String getListParentRootValue(final CategorySearchCriteria searchCriteria) throws DotDataException,
            DotSecurityException {

        if (searchCriteria.isParentList()) {
            if (UtilMethods.isSet(searchCriteria.rootInode)) {

                final Category category = APILocator.getCategoryAPI().find(searchCriteria.getRootInode(),
                        APILocator.systemUser(), false);

                final List<ShortCategory> rootParentList = FactoryLocator.getCategoryFactory().findHierarchy(CollectionsUtils.list(category.getKey()))
                        .get(0).getParentList();
                final List<ShortCategory> rootParentListWithRootInode = new ArrayList<>(rootParentList);

                rootParentListWithRootInode.add(new ShortCategory.Builder()
                        .setInode(category.getInode())
                        .setName(category.getCategoryName())
                        .setKey(category.getKey())
                        .build());

                final String json = JsonUtil.getJsonStringFromObject(rootParentListWithRootInode);

                return ",'" + json.substring(1, json.length() - 1) + "' AS path";
            }

            return  ", json_build_object('inode', inode, 'name', category_name, 'key', category_key)::varchar AS path";
        }

        return StringPool.BLANK;
    }

    private String getRootFilter() {
        if (UtilMethods.isSet(this.rootInode)) {
            return this.level == Level.TOP ? "AND c.inode = ?" : "WHERE c.inode = ?";
        } else {
            return StringPool.BLANK;
        }
    }

    private String getLevelFilter2() {
        if (this.level == Level.TOP) {
            return "AND level = 1";
        } else if (this.level == Level.CHILDREN) {
            return "AND level = 2";
        } else {
            return StringPool.BLANK;
        }
    }
}
