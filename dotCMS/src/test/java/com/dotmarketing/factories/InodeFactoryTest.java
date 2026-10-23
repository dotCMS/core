package com.dotmarketing.factories;

import static org.junit.Assert.assertEquals;

import com.dotcms.UnitTestBase;
import org.junit.Test;

/**
 * Verifies how {@link InodeFactory#plainOrderBy(String)} normalizes the {@code order by}
 * argument before it is appended to a query: only a single column, optionally qualified and
 * optionally followed by a direction, is kept. Everything else falls back to the empty string so
 * the caller applies its default order.
 */
public class InodeFactoryTest extends UnitTestBase {

    /**
     * Method to test: {@link InodeFactory#plainOrderBy(String)}
     * Given: a bare column, a qualified column, and each of them with a direction
     * Should: keep the expression as given
     */
    @Test
    public void singleColumnIsKept() {
        assertEquals("sort_order", InodeFactory.plainOrderBy("sort_order"));
        assertEquals("sort_order desc", InodeFactory.plainOrderBy("sort_order desc"));
        assertEquals("contentlet.sort_order", InodeFactory.plainOrderBy("contentlet.sort_order"));
        assertEquals("contentlet.sort_order asc", InodeFactory.plainOrderBy("contentlet.sort_order asc"));
        assertEquals("contentlet.mod_date desc", InodeFactory.plainOrderBy("contentlet.mod_date desc"));
    }

    /**
     * Method to test: {@link InodeFactory#plainOrderBy(String)}
     * Given: mixed case, surrounding whitespace and several spaces before the direction
     * Should: return the lower-cased, trimmed, single-spaced expression
     */
    @Test
    public void caseAndWhitespaceAreNormalized() {
        assertEquals("contentlet.sort_order desc", InodeFactory.plainOrderBy("  Contentlet.Sort_Order   DESC  "));
        assertEquals("title", InodeFactory.plainOrderBy("\tTITLE\n"));
    }

    /**
     * Method to test: {@link InodeFactory#plainOrderBy(String)}
     * Given: null, an empty string, or only whitespace
     * Should: return the empty string
     */
    @Test
    public void unsetValueIsEmpty() {
        assertEquals("", InodeFactory.plainOrderBy(null));
        assertEquals("", InodeFactory.plainOrderBy(""));
        assertEquals("", InodeFactory.plainOrderBy("   "));
    }

    /**
     * Method to test: {@link InodeFactory#plainOrderBy(String)}
     * Given: anything beyond one column and a direction (several columns, a function call, an
     * extra statement, a direction other than asc/desc, a column starting with a digit)
     * Should: return the empty string so the default order applies
     */
    @Test
    public void anythingElseIsEmpty() {
        assertEquals("", InodeFactory.plainOrderBy("title, mod_date"));
        assertEquals("", InodeFactory.plainOrderBy("lower(contentlet.title)"));
        assertEquals("", InodeFactory.plainOrderBy("contentlet.title desc; select 1"));
        assertEquals("", InodeFactory.plainOrderBy("contentlet.title random"));
        assertEquals("", InodeFactory.plainOrderBy("contentlet.title desc nulls last"));
        assertEquals("", InodeFactory.plainOrderBy("1title"));
        assertEquals("", InodeFactory.plainOrderBy("schema.contentlet.title"));
        assertEquals("", InodeFactory.plainOrderBy("contentlet.title desc --"));
    }

}
