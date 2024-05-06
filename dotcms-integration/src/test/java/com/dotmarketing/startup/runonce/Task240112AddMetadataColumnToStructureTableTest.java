package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotDataException;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Verifies that the {@link Task240112AddMetadataColumnToStructureTable} Upgrade Task works as
 * expected.
 *
 * @author Jose Castro
 * @since Jan 11th, 2024
 */
public class Task240112AddMetadataColumnToStructureTableTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void TestUpgradeTask() throws DotDataException {
        final Task240112AddMetadataColumnToStructureTable task = new Task240112AddMetadataColumnToStructureTable();
        task.executeUpgrade();
        assertTrue(task.forceRun());
    }

}
