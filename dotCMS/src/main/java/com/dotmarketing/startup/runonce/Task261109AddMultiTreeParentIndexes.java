package com.dotmarketing.startup.runonce;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.StartupTask;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/** Adds parent-leading indexes for page/container lookups without blocking normal writes. */
public class Task261109AddMultiTreeParentIndexes implements StartupTask {

    private static final String VALIDATE_INDEX = """
            SELECT i.indisvalid AND i.indisready AND i.indislive
                AND NOT i.indisunique AND i.indnkeyatts = 2 AND i.indnatts = 2
                AND i.indpred IS NULL AND i.indexprs IS NULL AND am.amname = 'btree'
                AND pg_get_indexdef(i.indexrelid, 1, true) = ?
                AND pg_get_indexdef(i.indexrelid, 2, true) = 'variant_id'
            FROM pg_index i
            JOIN pg_class c ON c.oid = i.indexrelid
            JOIN pg_am am ON am.oid = c.relam
            WHERE i.indexrelid = to_regclass(?) AND i.indrelid = 'multi_tree'::regclass
            """;

    /** Executes for existing installations until the startup framework records this version. */
    @Override
    public boolean forceRun() {
        return true;
    }

    /**
     * Creates missing indexes sequentially outside any ambient startup transaction.
     * Existing invalid/conflicting indexes require operator intervention, never an automatic drop.
     *
     * @throws DotDataException if a build fails or an existing index cannot provide this coverage
     */
    @Override
    public void executeUpgrade() throws DotDataException {
        if (!DbConnectionFactory.isPostgres()) {
            return;
        }
        try (Connection connection = DbConnectionFactory.getDataSource().getConnection()) {
            if (!connection.getAutoCommit()) {
                throw new DotDataException("Concurrent multi_tree indexes require an autocommit connection");
            }
            try (Statement ddl = connection.createStatement();
                    PreparedStatement validation = connection.prepareStatement(VALIDATE_INDEX)) {
                for (final String parent : List.of("parent1", "parent2")) {
                    final String index = "idx_multitree_" + parent + "_variant";
                    ddl.execute(parent.equals("parent1")
                            ? "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_multitree_parent1_variant ON multi_tree (parent1, variant_id)"
                            : "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_multitree_parent2_variant ON multi_tree (parent2, variant_id)");
                    validation.setString(1, parent);
                    validation.setString(2, index);
                    try (ResultSet result = validation.executeQuery()) {
                        if (!result.next() || !result.getBoolean(1)) {
                            throw new DotDataException("Index " + index
                                    + " is invalid, not ready, or has a conflicting definition; "
                                    + "inspect it before retrying the upgrade");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new DotDataException("Unable to create concurrent multi_tree parent indexes", e);
        }
    }
}
