package com.dotmarketing.startup.runonce;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.TaskLocatorUtil;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/** Verifies concurrent index DDL never uses or commits an ambient startup transaction. */
class Task261109AddMultiTreeParentIndexesTest {

    private final Connection connection = mock(Connection.class);
    private final Statement statement = mock(Statement.class);
    private final PreparedStatement validation = mock(PreparedStatement.class);
    private final ResultSet result = mock(ResultSet.class);
    private MockedStatic<DbConnectionFactory> database;

    /** Supplies a separate autocommit connection and successful catalog validation. */
    @BeforeEach
    void setUp() throws Exception {
        database = mockStatic(DbConnectionFactory.class);
        final DataSource source = mock(DataSource.class);
        database.when(DbConnectionFactory::isPostgres).thenReturn(true);
        database.when(DbConnectionFactory::getDataSource).thenReturn(source);
        when(source.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        when(connection.createStatement()).thenReturn(statement);
        when(connection.prepareStatement(anyString())).thenReturn(validation);
        when(validation.executeQuery()).thenReturn(result);
        when(result.next()).thenReturn(true);
        when(result.getBoolean(1)).thenReturn(true);
    }

    /** Removes thread-local static mocks after each test. */
    @AfterEach
    void tearDown() {
        database.close();
    }

    /** The framework must run this upgrade once, outside its transaction wrapper. */
    @Test
    void registeredAsNonTransactionalSchemaUpgrade() {
        assertTrue(new Task261109AddMultiTreeParentIndexes().forceRun());
        assertTrue(TaskLocatorUtil.getStartupRunOnceTaskClasses()
                .contains(Task261109AddMultiTreeParentIndexes.class));
        assertTrue(TaskLocatorUtil.getTaskClassesNoTransaction()
                .contains(Task261109AddMultiTreeParentIndexes.class));
    }

    /** New databases need these indexes even though upgrade execution is skipped on first startup. */
    @Test
    void freshSchemaIncludesBothParentIndexes() throws Exception {
        try (var resource = getClass().getClassLoader().getResourceAsStream("postgres.sql")) {
            assertTrue(resource != null, "Bundled PostgreSQL schema must exist");
            final String schema = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(schema.contains("create index idx_multitree_parent1_variant "
                    + "on multi_tree (parent1, variant_id);"));
            assertTrue(schema.contains("create index idx_multitree_parent2_variant "
                    + "on multi_tree (parent2, variant_id);"));
        }
    }

    /** Both missing indexes are created sequentially and each is checked before proceeding. */
    @Test
    void createsAndValidatesBothIndexesSequentially() throws Exception {
        new Task261109AddMultiTreeParentIndexes().executeUpgrade();

        final var order = inOrder(statement, validation, result);
        order.verify(statement).execute("CREATE INDEX CONCURRENTLY IF NOT EXISTS "
                + "idx_multitree_parent1_variant ON multi_tree (parent1, variant_id)");
        order.verify(validation).setString(1, "parent1");
        order.verify(validation).setString(2, "idx_multitree_parent1_variant");
        order.verify(validation).executeQuery();
        order.verify(result).getBoolean(1);
        order.verify(statement).execute("CREATE INDEX CONCURRENTLY IF NOT EXISTS "
                + "idx_multitree_parent2_variant ON multi_tree (parent2, variant_id)");
        order.verify(validation).setString(1, "parent2");
        order.verify(validation).setString(2, "idx_multitree_parent2_variant");
        order.verify(validation).executeQuery();
        order.verify(result).getBoolean(1);
        verify(connection, never()).setAutoCommit(anyBoolean());
        verify(connection, never()).commit();
        verify(connection).close();
        verify(statement).close();
        verify(validation).close();
    }

    /** Never implicitly commit a transaction just to permit concurrent index creation. */
    @Test
    void refusesNonAutocommitConnection() throws Exception {
        when(connection.getAutoCommit()).thenReturn(false);
        assertThrows(DotDataException.class,
                () -> new Task261109AddMultiTreeParentIndexes().executeUpgrade());
        verify(statement, never()).execute(anyString());
        verify(connection, never()).setAutoCommit(anyBoolean());
        verify(connection).close();
    }

    /** IF NOT EXISTS must not hide an invalid or differently defined existing index. */
    @Test
    void refusesInvalidOrConflictingIndex() throws Exception {
        when(result.getBoolean(1)).thenReturn(false);
        final var error = assertThrows(DotDataException.class,
                () -> new Task261109AddMultiTreeParentIndexes().executeUpgrade());
        assertTrue(error.getMessage().contains("idx_multitree_parent1_variant"));
        verify(statement, never()).execute("CREATE INDEX CONCURRENTLY IF NOT EXISTS "
                + "idx_multitree_parent2_variant ON multi_tree (parent2, variant_id)");
        verify(connection).close();
    }

    /** A failed second build remains an error so the version cannot be marked completed. */
    @Test
    void propagatesPartialBuildFailureAndClosesConnection() throws Exception {
        final SQLException failure = new SQLException("second build failed");
        when(statement.execute("CREATE INDEX CONCURRENTLY IF NOT EXISTS "
                + "idx_multitree_parent2_variant ON multi_tree (parent2, variant_id)"))
                .thenThrow(failure);
        final var error = assertThrows(DotDataException.class,
                () -> new Task261109AddMultiTreeParentIndexes().executeUpgrade());
        assertSame(failure, error.getCause());
        verify(connection).close();
        verify(statement).close();
        verify(validation).close();
    }

    /** Other database vendors must never receive PostgreSQL-specific DDL. */
    @Test
    void skipsNonPostgres() throws Exception {
        database.when(DbConnectionFactory::isPostgres).thenReturn(false);
        new Task261109AddMultiTreeParentIndexes().executeUpgrade();
        database.verify(DbConnectionFactory::getDataSource, never());
    }
}
