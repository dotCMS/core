package com.dotmarketing.business.cache.provider.h22;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.dotmarketing.business.cache.provider.CacheStats;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** H22 stats must discover and count every region with one grouped query per shard table. */
class H22CacheStatsTest {

    private final List<String> urls = new ArrayList<>();
    private final List<Connection> anchors = new ArrayList<>();
    private final List<Connection> collections = new ArrayList<>();
    private H22Cache cache;
    private int tables;
    private int databases;
    private boolean failQueries;
    private boolean unavailable;

    /** Uses isolated in-memory H2 shards, never the application cache or its async writers. */
    @BeforeEach
    void setUp() throws Exception {
        cache = new H22Cache("unused-stats-test") {
            /** Supplies observable JDBC connections without opening an on-disk cache. */
            @Override
            Optional<Connection> createConnection(final boolean autoCommit, final int db) {
                if (unavailable) {
                    return Optional.empty();
                }
                try {
                    final Connection connection = spy(DriverManager.getConnection(urls.get(db)));
                    collections.add(connection);
                    if (failQueries) {
                        doThrow(new SQLException("expected test failure"))
                                .when(connection).prepareStatement(anyString());
                        doThrow(new SQLException("expected test failure"))
                                .when(connection).createStatement();
                    }
                    return Optional.of(connection);
                } catch (SQLException exception) {
                    throw new IllegalStateException(exception);
                }
            }
        };
        databases = shardSetting("numberOfDbs");
        tables = shardSetting("numberOfTablesPerDb");
        for (int db = 0; db < databases; db++) {
            final String url = "jdbc:h2:mem:metrics-stats-" + UUID.randomUUID();
            urls.add(url);
            final Connection anchor = DriverManager.getConnection(url);
            anchors.add(anchor);
            for (int table = 0; table < tables; table++) {
                try (Statement statement = anchor.createStatement()) {
                    statement.execute("create table cache_table_" + table + " (cache_group varchar(255))");
                }
                try (PreparedStatement statement = anchor.prepareStatement(
                        "insert into cache_table_" + table + " (cache_group) values (?)")) {
                    statement.setString(1, "shared");
                    statement.executeUpdate();
                    statement.executeUpdate();
                    statement.setString(1, "db" + db);
                    statement.executeUpdate();
                }
            }
        }
    }

    /** Closing the last connection destroys each test's in-memory database. */
    @AfterEach
    void tearDown() throws Exception {
        for (final Connection connection : collections) {
            if (!connection.isClosed()) {
                connection.close();
            }
        }
        for (final Connection anchor : anchors) {
            anchor.close();
        }
    }

    /** Real grouped SQL sums shared regions across shards without querying again for each group. */
    @Test
    void stats_useOneGroupedQueryPerTable() throws Exception {
        final Map<String, String> sizes = cache.getStats().getStats().stream().collect(Collectors.toMap(
                region -> region.getStatValue(CacheStats.REGION),
                region -> region.getStatValue(CacheStats.REGION_SIZE)));

        assertEquals(Integer.toString(2 * databases * tables), sizes.get("shared"));
        assertEquals(databases + 1, sizes.size());
        for (int db = 0; db < databases; db++) {
            assertEquals(Integer.toString(tables), sizes.get("db" + db));
        }
        assertEquals(databases, collections.size());
        for (final Connection connection : collections) {
            final ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
            verify(connection, times(tables)).prepareStatement(sql.capture());
            assertTrue(sql.getAllValues().stream().allMatch(query -> query.contains("group by cache_group")));
            assertTrue(connection.isClosed());
        }
    }

    /** A failed query must not fabricate a complete set of zero-valued regions. */
    @Test
    void stats_queryFailureReturnsNoInventedCounts() {
        failQueries = true;

        assertTrue(cache.getStats().getStats().isEmpty());
    }

    /** Missing shard connections stay non-fatal without initializing a cache pool. */
    @Test
    void stats_unavailableShardsReturnNoRegions() {
        unavailable = true;

        assertTrue(cache.getStats().getStats().isEmpty());
        assertTrue(collections.isEmpty());
    }

    /** Mirrors the configured shard layout without exposing production fields for tests. */
    private int shardSetting(final String name) throws Exception {
        final Field field = H22Cache.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(cache);
    }
}
