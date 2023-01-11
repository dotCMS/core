package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static com.dotmarketing.db.DbConnectionFactory.isDBFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that the {@link Task230110MakeSomeSystemFieldsRemovableByVarName} UT runs as expected.
 *
 * @author Jose Castro
 * @since Jan 10th, 2023
 */
public class Task230110MakeSomeSystemFieldsRemovableByVarNameTest {

    private static final String PAGE_ASSET_BASE_TYPE_ID = "c541abb1-69b3-4bc5-8430-5e09e5239cc8";
    private static final String FILE_ASSET_BASE_TYPE_ID = "33888b6f-7a8e-4069-b1b6-5c1aa9d0a48d";
    private static final String PERSONA_BASE_TYPE_ID = "c938b15f-bcb6-49ef-8651-14d455a97045";
    private static final String HOST_TYPE_ID = "855a2d72-f2f3-4169-8b04-ac5157c4380c";

    private static final Map<String, List<String>> CONTENT_TYPES_AND_FIELDS = Map.of(
            PAGE_ASSET_BASE_TYPE_ID, List.of("friendlyName", "showOnMenu", "sortOrder", "cachettl", "redirecturl", "httpsreq", "seodescription", "seokeywords", "pagemetadata"),
            FILE_ASSET_BASE_TYPE_ID, List.of("title", "showOnMenu", "sortOrder", "description"),
            PERSONA_BASE_TYPE_ID, List.of("description"),
            HOST_TYPE_ID, List.of("hostThumbnail"));

    private static final String GET_FIXED_COLUMN_QUERY = "SELECT fixed FROM field WHERE velocity_var_name = ? AND structure_inode = ?";

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting up the web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b>{@link Task230110MakeSomeSystemFieldsRemovableByVarName#executeUpgrade()}</li>
     *     <li><b>Given Scenario:</b>Let the UT run so that the specified system Content Types can be updated
     *     according to the task requirements.</li>
     *     <li><b>Expected Result:</b>All of the fields specified in the Upgrade Task must have their {@code 'fixed'}
     *     column set to false.</li>
     * </ul>
     */
    @Test
    public void testExecuteUpgrade() {
        final Task230110MakeSomeSystemFieldsRemovableByVarName upgradeTask =
                new Task230110MakeSomeSystemFieldsRemovableByVarName();
        assertTrue("The Upgrade Task can only run with Postgres or SQL Server databases.", upgradeTask.forceRun());
        try {
            upgradeTask.executeUpgrade();
            for (final String typeId : CONTENT_TYPES_AND_FIELDS.keySet()) {
                final List<String> fieldList = CONTENT_TYPES_AND_FIELDS.get(typeId);
                for (final String fieldVarName : fieldList) {
                    final List<Map<String, Object>> fieldData =
                            new DotConnect().setSQL(GET_FIXED_COLUMN_QUERY).addParam(fieldVarName).addParam(typeId).loadObjectResults();
                    // Assertion
                    assertTrue(String.format("The 'fixed' column in field '%s' for CT '%s' must be 'false' after the " +
                                                     "UT has run.", fieldVarName, typeId),
                            isDBFalse(fieldData.get(0).get("fixed").toString()));
                }
            }
        } catch (final Exception e) {
            Assert.fail(String.format("An error occurred when running the " +
                                              "'Task230110MakeSomeSystemFieldsRemovableByVarName' UT: %s",
                    e.getMessage()));
        }
    }

}
