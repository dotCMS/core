package com.dotcms.content.index.opensearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for how the OpenSearch reindex byte limit is resolved (#37905, AC-006, AC-007).
 *
 * <h2>What is at stake</h2>
 * <p>{@code OS_REINDEX_BULK_SIZE_MB} falls back to {@code REINDEX_THREAD_ELASTICSEARCH_BULK_SIZE},
 * where {@code -1} means "disabled". A plain fallback would let an Elasticsearch {@code -1}
 * silently turn the OpenSearch limit off and bring back the HTTP 413 / OutOfMemoryError this fix
 * removes. So only an explicit OpenSearch value {@code <= 0} disables the limit (with a throttled WARN);
 * an unset OpenSearch value takes a positive Elasticsearch value or else 10 MB.</p>
 *
 * <pre>
 *   ./mvnw test -pl :dotcms-core -Dmaven.build.cache.enabled=false -Dtest=OSReindexBulkSizeTest
 * </pre>
 */
public class OSReindexBulkSizeTest {

    private static final String OS_KEY = "OS_REINDEX_BULK_SIZE_MB";
    private static final String ES_KEY = "REINDEX_THREAD_ELASTICSEARCH_BULK_SIZE";
    private static final long MB = 1_048_576L;

    private MockedStatic<Config> config;
    private MockedStatic<Logger> logger;

    @Before
    public void setUp() {
        config = mockStatic(Config.class);
        logger = mockStatic(Logger.class);
    }

    @After
    public void tearDown() {
        logger.close();
        config.close();
    }

    /** The enum entry pairs the OpenSearch key with its Elasticsearch fallback. */
    @Test
    public void test_propertyKeys() {
        assertEquals(OS_KEY, OSIndexProperty.REINDEX_BULK_SIZE_MB.osKey);
        assertEquals(ES_KEY, OSIndexProperty.REINDEX_BULK_SIZE_MB.esFallback);
    }

    /** OS set to a positive value wins, whatever ES says. */
    @Test
    public void test_positiveOsValue_isUsed() {
        assertEquals(5 * MB, resolve(5, 10));
        assertEquals(5 * MB, resolve(5, -1));
        verifyNoWarn();
    }

    /** An explicit OS value of -1 or 0 disables the limit and logs a (throttled) WARN naming the key. */
    @Test
    public void test_explicitOsZeroOrNegative_disablesWithOneWarn() {
        assertTrue(resolve(-1, 10) <= 0);
        logger.verify(() -> Logger.warnEvery(eq(ContentletIndexOperationsOS.class), anyString(),
                contains(OS_KEY), anyInt()), times(1));
    }

    /** OS set to 0 also disables. */
    @Test
    public void test_explicitOsZero_disables() {
        assertTrue(resolve(0, 10) <= 0);
    }

    /** OS unset: a positive ES value is inherited. */
    @Test
    public void test_unsetOs_inheritsPositiveEs() {
        assertEquals(10 * MB, resolve(null, 10));
        verifyNoWarn();
    }

    /** OS unset and ES disabled: the disabled value is NOT inherited; 10 MB applies, no WARN. */
    @Test
    public void test_unsetOs_doesNotInheritDisabledEs() {
        assertEquals(10 * MB, resolve(null, -1));
        assertEquals(10 * MB, resolve(null, 0));
        verifyNoWarn();
    }

    /** Neither set: 10 MB. */
    @Test
    public void test_bothUnset_defaultsToTenMb() {
        assertEquals(10 * MB, resolve(null, null));
        verifyNoWarn();
    }

    /** Resolves with the given property values; {@code null} means "not set". */
    private long resolve(final Integer osValue, final Integer esValue) {
        stub(OS_KEY, osValue);
        stub(ES_KEY, esValue);
        return ContentletIndexOperationsOS.resolveReindexBulkMaxBytes();
    }

    private void stub(final String key, final Integer value) {
        config.when(() -> Config.getStringProperty(eq(key), isNull()))
                .thenReturn(value == null ? null : String.valueOf(value));
        config.when(() -> Config.getIntProperty(eq(key), anyInt()))
                .thenAnswer(invocation -> value == null
                        ? invocation.getArgument(1) : value);
    }

    private void verifyNoWarn() {
        logger.verify(() -> Logger.warn(eq(ContentletIndexOperationsOS.class), anyString()), never());
        logger.verify(() -> Logger.warnEvery(eq(ContentletIndexOperationsOS.class), anyString(),
                anyString(), anyInt()), never());
    }
}
