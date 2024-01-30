package com.dotmarketing.startup.runonce;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import org.apache.commons.lang.math.NumberUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static com.dotmarketing.db.DbConnectionFactory.isDBFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that the {@link Task230110MakeSomeSystemFieldsRemovableByBaseType} UT runs as expected.
 *
 * @author Jose Castro
 * @since Jan 10th, 2023
 */
public class Task230110MakeSomeSystemFieldsRemovableByBaseTypeTest {

    private static final String FIND_CONTENT_TYPE_QUERY = "SELECT inode, name FROM structure WHERE %s";

    private static final Map<Object, List<String>> BASE_TYPES_AND_FIELDS = Map.of(
            BaseContentType.HTMLPAGE.getType(), List.of("friendlyName", "showOnMenu", "sortOrder", "cachettl",
                    "redirecturl", "httpsreq", "seodescription", "seokeywords", "pagemetadata"),
            BaseContentType.FILEASSET.getType(), List.of("title", "showOnMenu", "sortOrder", "description"),
            BaseContentType.PERSONA.getType(), List.of("description"),
            "Host", List.of("hostThumbnail"));

    private static final String GET_FIXED_COLUMN_QUERY = "SELECT fixed FROM field WHERE velocity_var_name = ? AND structure_inode = ?";

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting up the web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b>{@link Task230110MakeSomeSystemFieldsRemovableByBaseType#executeUpgrade()}</li>
     *     <li><b>Given Scenario:</b>Let the UT run so that the specified system Content Types can be updated
     *     according to the task requirements.</li>
     *     <li><b>Expected Result:</b>All of the fields specified in the Upgrade Task must have their {@code 'fixed'}
     *     column set to false.</li>
     * </ul>
     */
    @Test
    public void testExecuteUpgrade() {
        final Task230110MakeSomeSystemFieldsRemovableByBaseType upgradeTask =
                new Task230110MakeSomeSystemFieldsRemovableByBaseType();
        assertTrue("The Upgrade Task can only run with Postgres or SQL Server databases.", upgradeTask.forceRun());
        try {
            upgradeTask.executeUpgrade();
            for (final Map.Entry<Object, List<String>> entry : BASE_TYPES_AND_FIELDS.entrySet()) {
                final Object type = entry.getKey();
                final List<Map<String, Object>> contentTypeList = this.getContentTypes(type);
                for (final Map<String, Object> contentTypeData : contentTypeList) {
                    final String typeId = contentTypeData.get("inode").toString();
                    final String name = contentTypeData.get("name").toString();
                    final List<String> fieldVarNameList = entry.getValue();
                    for (final String fieldVarName : fieldVarNameList) {
                        final List<Map<String, Object>> fieldData =
                                new DotConnect().setSQL(GET_FIXED_COLUMN_QUERY).addParam(fieldVarName).addParam(typeId).loadObjectResults();
                        if (UtilMethods.isSet(fieldData)) {
                            // Assertion
                            assertTrue(String.format("The 'fixed' column in field '%s' for CT '%s' must be 'false' after " + "the UT has run.", fieldVarName, name), isDBFalse(fieldData.get(0).get("fixed").toString()));
                        }
                    }
                }

            }
        } catch (final Exception e) {
            Assert.fail(String.format("An error occurred when running the " +
                                              "'Task230110MakeSomeSystemFieldsRemovableByBaseType' UT: %s",
                    e.getMessage()));
        }
    }

    /**
     * Returns the list of Content Types that match either a specific Base Type, or a Velocity Variable Name.
     *
     * @param type The Base Type -- see {@link BaseContentType} -- or a specific Velocity Variable Name.
     *
     * @return The list with one or more Content Types that match the specified search criterion.
     */
    private List<Map<String, Object>> getContentTypes(final Object type) throws DotDataException {
        Object param;
        String sqlQuery;
        if (NumberUtils.isNumber(type.toString())) {
            sqlQuery = String.format(FIND_CONTENT_TYPE_QUERY, "structuretype = ?");
            param = Integer.valueOf(type.toString());
        } else {
            sqlQuery = String.format(FIND_CONTENT_TYPE_QUERY, "velocity_var_name = ?");
            param = type;
        }
        return new DotConnect().setSQL(sqlQuery).addParam(param).loadObjectResults();
    }

}
