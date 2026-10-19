package com.dotcms.content.elasticsearch.business;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import com.dotcms.UnitTestBase;
import com.dotcms.util.SecurityLoggerServiceAPI;
import com.dotcms.util.SecurityLoggerServiceAPIFactory;
import org.junit.Before;
import org.junit.Test;

/**
 * Verifies how {@link ESContentFactoryImpl#resolvePageContentletsOrderBy(String)} turns the
 * "sort contentlets by" setting of a container into the sort expression used when loading the
 * contentlets of a page container.
 */
public class ESContentFactoryImplPageOrderByTest extends UnitTestBase {

    private static final String DEFAULT_ORDER = "multi_tree.tree_order";

    @Before
    public void before() {
        SecurityLoggerServiceAPIFactory
                .setAlternativeSecurityLogger(mock(SecurityLoggerServiceAPI.class));
    }

    /**
     * Method to test: {@link ESContentFactoryImpl#resolvePageContentletsOrderBy(String)}
     * Given: no sort requested
     * Should: fall back to the multi-tree position
     */
    @Test
    public void emptyValueUsesTreeOrder() {
        assertEquals(DEFAULT_ORDER, ESContentFactoryImpl.resolvePageContentletsOrderBy(null));
        assertEquals(DEFAULT_ORDER, ESContentFactoryImpl.resolvePageContentletsOrderBy(""));
        assertEquals(DEFAULT_ORDER, ESContentFactoryImpl.resolvePageContentletsOrderBy("  "));
    }

    /**
     * Method to test: {@link ESContentFactoryImpl#resolvePageContentletsOrderBy(String)}
     * Given: the {@code tree_order} alias
     * Should: resolve to the qualified multi-tree column
     */
    @Test
    public void treeOrderAliasUsesQualifiedColumn() {
        assertEquals(DEFAULT_ORDER,
                ESContentFactoryImpl.resolvePageContentletsOrderBy("tree_order"));
    }

    /**
     * Method to test: {@link ESContentFactoryImpl#resolvePageContentletsOrderBy(String)}
     * Given: a supported sort column, with and without a direction
     * Should: keep it
     */
    @Test
    public void supportedColumnIsKept() {
        assertEquals("title", ESContentFactoryImpl.resolvePageContentletsOrderBy("title"));
        assertEquals("mod_date desc",
                ESContentFactoryImpl.resolvePageContentletsOrderBy("mod_date desc"));
        assertEquals("mod_date",
                ESContentFactoryImpl.resolvePageContentletsOrderBy("modDate"));
    }

    /**
     * Method to test: {@link ESContentFactoryImpl#resolvePageContentletsOrderBy(String)}
     * Given: a value that is not a single supported sort column
     * Should: fall back to the multi-tree position instead of using the value
     */
    @Test
    public void unsupportedValueUsesTreeOrder() {
        assertEquals(DEFAULT_ORDER,
                ESContentFactoryImpl.resolvePageContentletsOrderBy("multi_tree.tree_order"));
        assertEquals(DEFAULT_ORDER,
                ESContentFactoryImpl.resolvePageContentletsOrderBy("tree_order, title"));
        assertEquals(DEFAULT_ORDER,
                ESContentFactoryImpl.resolvePageContentletsOrderBy("tree_order,(select 1)"));
        assertEquals(DEFAULT_ORDER,
                ESContentFactoryImpl.resolvePageContentletsOrderBy("not_a_column"));
    }
}
