package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that the {@link Task230701AddHashIndicesToWorkflowTables} Upgrade Task is working as expected.
 *
 * @author Jose Castro
 * @since Jun 22nd, 2023
 */
public class Task230701AddHashIndicesToWorkflowTablesTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link Task230701AddHashIndicesToWorkflowTables#executeUpgrade()}</li>
     *     <li><b>Given Scenario: </b>Verifies that a new Hash Index is added to the specified Workflow tables.</li>
     *     <li><b>Expected Result: </b>Each Hash Index must be present.</li>
     * </ul>
     */
    @Test
    public void executeTaskUpgrade() throws DotDataException {
        final List<String> tables = List.of("workflow_comment", "workflow_history", "workflowtask_files");
        final Task230701AddHashIndicesToWorkflowTables task = new Task230701AddHashIndicesToWorkflowTables();
        task.executeUpgrade();
        final DotDatabaseMetaData dbMetadata = new DotDatabaseMetaData();
        tables.forEach(table -> {
            try {
                final ResultSet indicesInfo = dbMetadata.getIndices(DbConnectionFactory.getConnection(), null, table, false);
                while (indicesInfo.next()) {
                    final String indexName = indicesInfo.getString("INDEX_NAME");
                    final String columnName = indicesInfo.getString("COLUMN_NAME");
                    final boolean nonUnique = indicesInfo.getBoolean("NON_UNIQUE");
                    if (indexName.equals(table + "_hash_idx")) {
                        assertTrue("Hash Indices are always non-unique", nonUnique);
                        assertEquals("Column name '" + columnName + "' is not 'workflowtask_id'", "workflowtask_id", columnName);
                    }
                }
            } catch (final SQLException e) {
                Logger.error(this, e);
            }
        });
    }

}
