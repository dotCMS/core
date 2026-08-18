package com.dotcms.rest.api.v1.content.search.strategies;

import com.dotcms.rest.api.v1.content.search.handlers.FieldContext;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link BooleanFieldStrategy} — the exact-term clause a True/False (BOOL data type)
 * field is filtered with.
 *
 * <p>A BOOL field is mapped in Elasticsearch as a {@code boolean}, so it cannot be matched with the
 * contains-style wildcard the text strategy produces: the wildcard is rejected, and because these
 * queries are not lenient the rejection fails the whole query and surfaces as an empty result set
 * with no error. Hence the exact term.</p>
 *
 * <p>The coercion has to accept the db-style option values these fields are actually authored with.
 * dotCMS's own Radio help text gives {@code True|1 False|0} as the example and the product ships
 * {@code Host.runDashboard} as {@code Yes|1 / No|0}, so {@code 1} and {@code 0} are first-class
 * inputs here.</p>
 *
 * <p>commons-lang3 handles them: verified against the resolved 3.18.0, {@code toBoolean("1")} is
 * {@code true} and {@code toBoolean("0")} is {@code false} — its single-character branch matches
 * {@code '1'}/{@code '0'} alongside {@code y/t/n/f}. That is a property of the library version, not
 * of this class, which is precisely why it is pinned by a test: a downgrade to a release without
 * that branch would otherwise invert every BOOL filter silently, and returning the OPPOSITE rows is
 * worse than erroring.</p>
 *
 * <p>{@link com.dotcms.contenttype.model.field.SelectableValuesField#check()} normalises
 * {@code 1}/{@code 0} before calling the same util. That is belt-and-braces predating the library
 * support, not evidence the support is missing — a reading that has twice been mistaken for a bug
 * here.</p>
 */
public class BooleanFieldStrategyTest {

    private final BooleanFieldStrategy strategy = new BooleanFieldStrategy();

    private String query(final Object value) {
        return strategy.generateQuery(
                new FieldContext.Builder().withFieldName("SSS.flag").withFieldValue(value).build());
    }

    @Test
    public void literalTrueAndFalse() {
        assertEquals("+SSS.flag:true", query("true"));
        assertEquals("+SSS.flag:false", query("false"));
    }

    /**
     * The database representation, which is what a field authored {@code Yes|1 / No|0} actually
     * stores. These two assertions are the ones that would break under a commons-lang3 downgrade, and
     * breaking them means the filter returns the opposite rows rather than failing.
     */
    @Test
    public void dbStyleOneIsTrueAndZeroIsFalse() {
        assertEquals("+SSS.flag:true", query("1"));
        assertEquals("+SSS.flag:false", query("0"));
    }

    /** The tokens commons-lang already handles must keep working. */
    @Test
    public void wordyTruthyValues() {
        assertEquals("+SSS.flag:true", query("yes"));
        assertEquals("+SSS.flag:true", query("on"));
        assertEquals("+SSS.flag:true", query("y"));
        assertEquals("+SSS.flag:true", query("t"));
        assertEquals("+SSS.flag:false", query("no"));
        assertEquals("+SSS.flag:false", query("off"));
        assertEquals("+SSS.flag:false", query("n"));
    }

    /** Case and surrounding whitespace are not meaningful. */
    @Test
    public void caseAndWhitespaceAreIgnored() {
        assertEquals("+SSS.flag:true", query("  TRUE "));
        assertEquals("+SSS.flag:true", query(" Yes"));
        assertEquals("+SSS.flag:true", query(" 1 "));
    }

    /** A real boolean arrives from callers that already parsed the value. */
    @Test
    public void actualBooleanValues() {
        assertEquals("+SSS.flag:true", query(Boolean.TRUE));
        assertEquals("+SSS.flag:false", query(Boolean.FALSE));
    }

    /** Anything unrecognised is not truthy, so it must not be reported as a match for true. */
    @Test
    public void unrecognisedValueIsFalse() {
        assertEquals("+SSS.flag:false", query("maybe"));
    }
}
