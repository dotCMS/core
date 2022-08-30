package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Verifies that the {@link Task220825MakeSomeSystemFieldsRemovable} UT runs as expected.
 *
 * @author Jose Castro
 * @since Aug 30th, 2022
 */
public class Task220825MakeSomeSystemFieldsRemovableTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting up the web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testExecuteUpgrade() {
        // This UT updates system Content Types, so no test data is required
        final Task220825MakeSomeSystemFieldsRemovable upgradeTask = new Task220825MakeSomeSystemFieldsRemovable();
        assertTrue("One or more of the Content Types whose fields need to be updated don't match the expected ID",
                upgradeTask.forceRun());
        try {
            upgradeTask.executeUpgrade();
        } catch (final Exception e) {
            Assert.fail("An error occurred when running the 'Task220825MakeSomeSystemFieldsRemovable' UT");
        }
    }

}
