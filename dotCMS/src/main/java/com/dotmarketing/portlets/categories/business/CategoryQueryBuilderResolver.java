package com.dotmarketing.portlets.categories.business;

import com.dotmarketing.util.UtilMethods;

/**
 * Allow the selection of which {@link CategoryQueryBuilder} to use based on the {@link CategorySearchCriteria}.
 */
public abstract class CategoryQueryBuilderResolver {

    private CategoryQueryBuilderResolver(){}

    public static CategoryQueryBuilder getQueryBuilder(final CategorySearchCriteria searchCriteria) {
        return mustUseRecursiveTemplate(searchCriteria) ? new CategoryRecursiveQueryBuilder(searchCriteria) :
                new CategorySimpleQueryBuilder(searchCriteria);
    }

    public static boolean mustUseRecursiveTemplate(final CategorySearchCriteria searchCriteria) {
        return (searchCriteria.searchAllLevels && UtilMethods.isSet(searchCriteria.rootInode)) || searchCriteria.isParentList();
    }
}
