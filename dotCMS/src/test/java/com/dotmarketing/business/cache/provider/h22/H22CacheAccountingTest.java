package com.dotmarketing.business.cache.provider.h22;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotmarketing.business.cache.provider.CacheStats;
import com.dotmarketing.util.UtilMethods;
import java.io.NotSerializableException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class H22CacheAccountingTest {

    private static final String GROUP = "accounting";
    private static final String BYTES_WRITTEN = "cache.stats.region.bytes.written";
    private static final String AVERAGE_SIZE = "cache.stats.region.serialized.size.avg";
    private H22Cache cache;
    private PreparedStatement statement;
    private H22GroupStats groupStats;

    /** Uses JDBC mocks so accounting tests never start or modify an H22 database. */
    @Before
    public void setUp() throws Exception {
        final Connection connection = mock(Connection.class);
        statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(mock(ResultSet.class));
        cache = new H22Cache("unused-accounting-test") {
            /** Supplies a mock connection without opening an H22 database. */
            @Override
            Optional<Connection> createConnection(final boolean autoCommit, final int dbNumber) {
                return Optional.of(connection);
            }

            /** Keeps the test region visible before its first successful write. */
            @Override
            public Set<String> getGroups() {
                return Set.of(GROUP);
            }
        };
        final Field statsField = H22Cache.class.getDeclaredField("stats");
        statsField.setAccessible(true);
        groupStats = ((H2GroupStatsList) statsField.get(cache)).group(GROUP);
    }

    /** A successful SQL update counts once, even when JDBC execute returns false. */
    @Test
    public void testSuccessfulWriteIsCountedOnce() throws Exception {
        upsert("key", "content");

        assertEquals(1, counter("writes"));
    }

    /** Accounts the actual serialized byte array, not its bit count. */
    @Test
    public void testSerializedLengthIsCountedInBytes() throws Exception {
        upsert("key", "content");
        final ArgumentCaptor<byte[]> serialized = ArgumentCaptor.forClass(byte[].class);
        verify(statement).setBytes(eq(3), serialized.capture());

        assertEquals(serialized.getValue().length, counter("totalSize"));
    }

    /** Replacing a key contributes another write to the write-weighted average. */
    @Test
    public void testRepeatedKeyWritesHaveCorrectAverage() throws Exception {
        upsert("key", "first");
        final long firstSize = counter("totalSize");
        upsert("key", "a longer replacement");
        final ArgumentCaptor<byte[]> serialized = ArgumentCaptor.forClass(byte[].class);
        verify(statement, times(2)).setBytes(eq(3), serialized.capture());
        final long expectedBytes = serialized.getAllValues().stream()
                .mapToLong(bytes -> bytes.length).sum();

        assertEquals(2, counter("writes"));
        assertEquals(expectedBytes, counter("totalSize"));
        assertEquals(UtilMethods.prettyByteify(expectedBytes / 2),
                cache.getStats().getStats().get(0).getStatValue(AVERAGE_SIZE));
        assertTrue(counter("totalSize") > firstSize);
    }

    /** Failed SQL execution leaves successful-write accounting unchanged. */
    @Test
    public void testFailedSqlDoesNotCountAsSuccessfulWrite() throws Exception {
        upsert("key", "first");
        final long writesBefore = counter("writes");
        final long bytesBefore = counter("totalSize");
        when(statement.execute()).thenThrow(new SQLException("expected test failure"));
        try {
            upsert("key", "replacement");
            fail("Expected SQL failure");
        } catch (SQLException expected) {
            assertEquals(writesBefore, counter("writes"));
            assertEquals(bytesBefore, counter("totalSize"));
        }
    }

    /** Failed serialization must not contribute bytes or successful writes. */
    @Test
    public void testFailedSerializationDoesNotCountAsSuccessfulWrite() throws Exception {
        try {
            upsert("key", new BrokenSerializable());
            fail("Expected serialization failure");
        } catch (NotSerializableException expected) {
            assertEquals(0, counter("writes"));
            assertEquals(0, counter("totalSize"));
        }
    }

    /** New regions report zero without presenting write totals as cache memory usage. */
    @Test
    public void testEmptyRegionHasZeroSerializedSizeAndExplicitWriteColumns() {
        final CacheStats region = cache.getStats().getStats().get(0);

        assertEquals(UtilMethods.prettyByteify(0), region.getStatValue(BYTES_WRITTEN));
        assertEquals(UtilMethods.prettyByteify(0), region.getStatValue(AVERAGE_SIZE));
        assertEquals("0", region.getStatValue(CacheStats.REGION_SIZE));
        assertNull(region.getStatValue(CacheStats.REGION_MEM_TOTAL_PRETTY));
        assertNull(region.getStatValue(CacheStats.REGION_MEM_PER_OBJECT));
    }

    /** Exercises the real private upsert path without widening production visibility. */
    private void upsert(final String key, final Serializable value) throws Exception {
        final Method upsert = H22Cache.class.getDeclaredMethod("doUpsert", Fqn.class,
                Serializable.class);
        upsert.setAccessible(true);
        try {
            upsert.invoke(cache, new Fqn(GROUP, key), value);
        } catch (InvocationTargetException exception) {
            throw (Exception) exception.getCause();
        }
    }

    /** Reads either the legacy boxed long or its thread-safe Number replacement. */
    private long counter(final String name) throws Exception {
        final Field field = H22GroupStats.class.getDeclaredField(name);
        field.setAccessible(true);
        return ((Number) field.get(groupStats)).longValue();
    }

    private static class BrokenSerializable implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Object notSerializable = new Object();
    }
}
