package com.dotmarketing.startup.runonce;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldDataBaseUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that the {@link Task250826AddIndexesToUniqueFieldsTable} works as expected.
 */
public class Task250826AddIndexesToUniqueFieldsTableTest extends IntegrationTestBase {

    /**
     * Initializes the test environment.
     *
     * @throws Exception if an error occurs during initialization.
     */
    @BeforeClass
    public static void setup() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * <ul>
     *     <li><b>Method to test:
     *     </b>{@link Task250826AddIndexesToUniqueFieldsTable#executeUpgrade()}</li>
     *     <li><b>Given Scenario: </b>Run the Upgrade Task and verify that the expected indexes
     *     are created.</li>
     *     <li>
     *         <b>Expected Result: </b>There must be 7 Indexes (the existing Primary Key Index
     *         plus the new ones):
     *         <ol>
     *              <li>unique_fields_pkey</li>
     *              <li>idx_unique_fields_contentlet_ids_gin</li>
     *              <li>idx_unique_fields_language_id</li>
     *              <li>idx_unique_fields_content_type_id</li>
     *              <li>idx_unique_fields_field_variable_name</li>
     *              <li>idx_unique_fields_variant</li>
     *              <li>idx_unique_fields_live</li>
     *         </ol>
     *     </li>
     * </ul>
     */
    @Test
    public void executeUpgradeTask() throws DotDataException {
        // ╔══════════════════╗
        // ║  Initialization  ║
        // ╚══════════════════╝
        final Task250826AddIndexesToUniqueFieldsTable task =
                new Task250826AddIndexesToUniqueFieldsTable();
        if (!task.forceRun()) {
            new UniqueFieldDataBaseUtil().createTableAndPopulate();
        }

        // ╔════════════════════════╗
        // ║  Generating Test data  ║
        // ╚════════════════════════╝
        task.executeUpgrade();
        final DotConnect dc = new DotConnect().setSQL("SELECT indexname, indexdef FROM pg_indexes WHERE tablename = 'unique_fields'");
        final List<Map<String, Object>> tableIndexes = dc.loadObjectResults();

        // ╔══════════════╗
        // ║  Assertions  ║
        // ╚══════════════╝
        assertEquals("There must be 7 indexes for the 'unique_fields' table", 7,
                tableIndexes.size());
        assertTrue("The 'unique_fields_pkey' Index must be present",
                this.isIndexPresent("unique_fields_pkey", tableIndexes));
        assertTrue("The 'idx_unique_fields_contentlet_ids_gin' Index must be present",
                this.isIndexPresent("idx_unique_fields_contentlet_ids_gin", tableIndexes));
        assertTrue("The 'idx_unique_fields_language_id' Index must be present",
                this.isIndexPresent("idx_unique_fields_language_id", tableIndexes));
        assertTrue("The 'idx_unique_fields_content_type_id' Index must be present",
                this.isIndexPresent("idx_unique_fields_content_type_id", tableIndexes));
        assertTrue("The 'idx_unique_fields_field_variable_name' Index must be present",
                this.isIndexPresent("idx_unique_fields_field_variable_name", tableIndexes));
        assertTrue("The 'idx_unique_fields_variant' Index must be present",
                this.isIndexPresent("idx_unique_fields_variant", tableIndexes));
        assertTrue("The 'idx_unique_fields_live' Index must be present",
                this.isIndexPresent("idx_unique_fields_live", tableIndexes));
    }

    /**
     * Finds the specified Index name in the list of indexes that belong to the
     * {@code unique_fields} table.
     *
     * @param indexName    The index name.
     * @param tableIndexes The result set.
     *
     * @return If the Index was found, returns {@code true}.
     */
    private boolean isIndexPresent(final String indexName,
                                   final List<Map<String, Object>> tableIndexes) {
        return tableIndexes.stream().anyMatch(index -> index.get("indexname").equals(indexName));
    }

}