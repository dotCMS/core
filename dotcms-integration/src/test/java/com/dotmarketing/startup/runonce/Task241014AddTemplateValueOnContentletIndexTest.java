package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.exception.DotDataException;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Verifies that the {@link Task241014AddTemplateValueOnContentletIndex} Upgrade Task is working as
 * expected.
 *
 * @since Oct 24, 2014
 */
public class Task241014AddTemplateValueOnContentletIndexTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link Task241014AddTemplateValueOnContentletIndex#executeUpgrade()}</li>
     *     <li><b>Given Scenario: </b>Verifies that new index is included in the contentlet table.</li>
     *     <li><b>Expected Result: </b>Index must be present.</li>
     * </ul>
     */
    @Test
    public void executeTaskUpgrade() throws DotDataException {
        final Task241014AddTemplateValueOnContentletIndex task = new Task241014AddTemplateValueOnContentletIndex();
        task.executeUpgrade();

        final DotDatabaseMetaData dmd = new DotDatabaseMetaData();

        assertTrue("idx_contentlet_template_value should exist in the contentlet table",
                dmd.checkIndex("contentlet", "idx_contentlet_template_value"));
    }
}
