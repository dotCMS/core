package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.liferay.portal.model.User;
import io.vavr.Tuple2;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static com.dotmarketing.db.DbConnectionFactory.isDBFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that the {@link Task220825MakeSomeSystemFieldsRemovable} UT runs as expected.
 *
 * @author Jose Castro
 * @since Aug 30th, 2022
 */
public class Task220825MakeSomeSystemFieldsRemovableTest {

    private static final List<String> pageAssetFields = List.of("friendlyName", "showOnMenu", "sortOrder", "cachettl"
            , "redirecturl", "httpsreq", "seodescription", "seokeywords", "pagemetadata");
    private static final List<String> fileAssetFields = List.of("title", "showOnMenu", "sortOrder", "description");
    private static final List<String> personaField = List.of("description");
    private static final List<String> hostField = List.of("hostThumbnail");

    private static final Map<String, Tuple2<String, List<String>>> contentTypes = Map.of(
            "Page Asset", new Tuple2<>("c541abb1-69b3-4bc5-8430-5e09e5239cc8", pageAssetFields),
            "File Asset", new Tuple2<>("33888b6f-7a8e-4069-b1b6-5c1aa9d0a48d", fileAssetFields),
            "Persona", new Tuple2<>("c938b15f-bcb6-49ef-8651-14d455a97045", personaField),
            "Host", new Tuple2<>("855a2d72-f2f3-4169-8b04-ac5157c4380c", hostField));

    private static final String GET_FIXED_COLUMN_QUERY = "SELECT fixed FROM field WHERE velocity_var_name = ? AND structure_inode = ?";

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting up the web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b>{@link Task220825MakeSomeSystemFieldsRemovable#executeUpgrade()}</li>
     *     <li><b>Given Scenario:</b>Let the UT run so that the specified system Content Types can be updated
     *     according to the task requirements.</li>
     *     <li><b>Expected Result:</b>All of the fields specified in the Upgrade Task must have their {@code 'fixed'}
     *     column set to false.</li>
     * </ul>
     */
    @Test
    public void testExecuteUpgrade() {
        // This UT updates system Content Types, so no test data is required
        final Task220825MakeSomeSystemFieldsRemovable upgradeTask = new Task220825MakeSomeSystemFieldsRemovable();
        assertTrue("One or more of the Content Types whose fields need to be updated don't match the expected ID. " +
                           "This Upgrade Task will not run.", upgradeTask.forceRun());
        try {
            upgradeTask.executeUpgrade();
            for (final String contentType : contentTypes.keySet()) {
                final String contentTypeId = contentTypes.get(contentType)._1();
                final List<String> fieldList = contentTypes.get(contentType)._2();
                for (final String fieldVarName : fieldList) {
                    final List<Map<String, Object>> fieldData =
                            new DotConnect().setSQL(GET_FIXED_COLUMN_QUERY).addParam(fieldVarName).addParam(contentTypeId).loadObjectResults();
                    // Assertions
                    assertTrue(String.format("The 'fixed' column in field '%s' for CT '%s' must be false after the UT" +
                                                     " has run.", fieldVarName, contentType),
                            isDBFalse(fieldData.get(0).get("fixed").toString()));
                }
            }
        } catch (final Exception e) {
            Assert.fail(String.format("An error occurred when running the 'Task220825MakeSomeSystemFieldsRemovable' " + "UT: %s", e.getMessage()));
        }
    }

}