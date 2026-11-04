package com.dotmarketing.portlets.categories.business;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.util.pagination.OrderDirection;
import org.junit.jupiter.api.Test;

/**
 * Checks the ORDER BY clause that {@link CategorySimpleQueryBuilder} writes: the requested order
 * comes first and category name breaks ties.
 */
class CategorySimpleQueryBuilderTest {

    /**
     * Method to test: {@link CategorySimpleQueryBuilder#build()}
     * Given: the children of a category ordered by sort order, ascending
     * Expected: the query orders by sort order and then by category name
     */
    @Test
    void build_childrenBySortOrder_ordersTiesByName() throws Exception {
        final String query = new CategorySimpleQueryBuilder(new CategorySearchCriteria.Builder()
                .rootInode("parent-inode")
                .orderBy("sort_order")
                .direction(OrderDirection.ASC)
                .build()).build();

        assertTrue(query.endsWith("ORDER BY sort_order ASC, c.category_name"), query);
    }

    /**
     * Method to test: {@link CategorySimpleQueryBuilder#build()}
     * Given: the top-level categories ordered by key, descending
     * Expected: the query orders by key descending and then by category name
     */
    @Test
    void build_topLevelByKeyDescending_ordersTiesByName() throws Exception {
        final String query = new CategorySimpleQueryBuilder(new CategorySearchCriteria.Builder()
                .orderBy("category_key")
                .direction(OrderDirection.DESC)
                .build()).build();

        assertTrue(query.endsWith("ORDER BY category_key DESC, c.category_name"), query);
    }
}
