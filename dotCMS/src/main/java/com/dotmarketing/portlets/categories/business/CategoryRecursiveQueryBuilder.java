package com.dotmarketing.portlets.categories.business;

import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.categories.model.ShortCategory;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CategoryRecursiveQueryBuilder extends CategoryQueryBuilder{

    private boolean parentList;
    final String queryTemplate = "WITH RECURSIVE CategoryHierarchy AS ( " +
            "SELECT c.*, 1 AS level :parentList_1 " +
            "FROM Category c :levelFilter_1 :rootFilter " +
            "UNION ALL " +
            "SELECT c.*, ch.level + 1 AS level :parentList_2 " +
            "FROM Category c JOIN tree t ON c.inode = t.child JOIN CategoryHierarchy ch ON t.parent = ch.inode " +
            ") " +
            "SELECT * :parentList_3 :countChildren FROM CategoryHierarchy ch :levelFilter_2 :filterCategories " +
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

        final String parentList_3 = this.parentList ? ", ch.path" : StringPool.BLANK;

        return StringUtils.format(queryTemplate, Map.of(
                    "rootFilter", rootFilter,
                    "levelFilter_1", levelFilter_1,
                    "levelFilter_2", levelFilter_2,
                    "countChildren", getChildrenCount(),
                    "parentList_1", parentList_1,
                    "parentList_2", parentList_2,
                    "parentList_3", parentList_3,
                    "filterCategories", getFilterCategories(),
                "orderBy",searchCriteria.orderBy,
                "direction", searchCriteria.direction.toString()
                )
        );
    }

    protected String getFilterCategories() {

        if (getLevelFilter2().isBlank()) {
            return "WHERE LOWER(category_name) LIKE ?  OR " +
                    "LOWER(category_key) LIKE ?  OR " +
                    "LOWER(category_velocity_var_name) LIKE ?";
        } else {
            return "LOWER(category_name) LIKE ?  OR " +
                    "LOWER(category_key) LIKE ?  OR " +
                    "LOWER(category_velocity_var_name) LIKE ?";
        }
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
            return "WHERE level = 1";
        } else if (this.level == Level.CHILDREN) {
            return "WHERE level = 2";
        } else {
            return StringPool.BLANK;
        }
    }
}
