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

public class CategorySimpleQueryBuilder extends CategoryQueryBuilder{

    final String queryTemplate = "SELECT * :countChildren  FROM category as c :rootFilter :filterCategories ORDER BY :orderBy :direction";

    public CategorySimpleQueryBuilder(final CategorySearchCriteria searchCriteria) {
        super(searchCriteria);
    }


    public String build() throws DotDataException, DotSecurityException {

        final String rootFilter = getRootFilter();


        return StringUtils.format(queryTemplate, Map.of(
                "countChildren", getChildrenCount(),
                "rootFilter", rootFilter,
                "filterCategories", getFilterCategories(this.searchCriteria),
                "orderBy",searchCriteria.orderBy,
                "direction", searchCriteria.direction.toString()
                )
        );
    }

    protected String getFilterCategories(final CategorySearchCriteria searchCriteria) {

        return UtilMethods.isSet(searchCriteria.filter) ?
                "AND (LOWER(category_name) LIKE ?  OR " +
                        "LOWER(category_key) LIKE ?  OR " +
                        "LOWER(category_velocity_var_name) LIKE ?)" : StringPool.BLANK;
    }

    private String getRootFilter() {
        if (this.level == Level.CHILDREN) {
            return "LEFT JOIN tree ON c.inode = tree.child WHERE tree.parent = ?";
        } else if (this.level == Level.TOP) {
            return "LEFT JOIN tree ON c.inode = tree.child WHERE tree.child IS NULL";
        }

        throw new IllegalArgumentException("If the Level is equals to ALL_LEVELS then The Query must be a recursive query");
    }

}
