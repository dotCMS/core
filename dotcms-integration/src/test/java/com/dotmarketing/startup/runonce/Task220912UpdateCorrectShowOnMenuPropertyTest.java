package com.dotmarketing.startup.runonce;

import com.dotcms.content.business.json.ContentletJsonAPI;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static com.dotmarketing.db.DbConnectionFactory.getDBFalse;
import static com.dotmarketing.db.DbConnectionFactory.isMsSql;
import static com.dotmarketing.db.DbConnectionFactory.isPostgres;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Integration Test for the {@link Task220912UpdateCorrectShowOnMenuProperty} Upgrade Task.
 *
 * @author Jose Castro
 * @since Sep 12th, 2022
 */
public class Task220912UpdateCorrectShowOnMenuPropertyTest {

    private static final String VELOCITY_VAR_NAME = "showOnMenu";
    private static final String FIND_CONTENT_TYPES_WITH_FIELD = "SELECT structure_inode, field_contentlet FROM field " +
                                                                        "WHERE velocity_var_name = ?";
    private static final String FIND_CONTENTS_WITH_INCORRECT_SHOW_ON_MENU_VALUE = "SELECT COUNT(*) as count FROM contentlet " +
                                                                                          "WHERE %s = 'true' AND " +
                                                                                          "show_on_menu = " + getDBFalse() + " AND " +
                                                                                          "structure_inode = ?";

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b>{@link Task220912UpdateCorrectShowOnMenuProperty#executeUpgrade()}</li>
     *     <li><b>Given Scenario:</b>The {@code show_on_menu} column in the {@code contentlet} table must reflect the
     *     actual value of the 'Show On Menu' field. There situations during customer upgrades in which these two values
     *     didn't match, which caused several problems for the Support Team.</li>
     *     <li><b>Expected Result:</b>Both the {@code show_on_menu} column and the {@code showOnMenu} attribute in the
     *     {@code contentlet_as_json} column must be the same.</li>
     * </ul>
     */
    @Test
    public void testExecuteUpgrade() {
        final Task220912UpdateCorrectShowOnMenuProperty upgradeTask = new Task220912UpdateCorrectShowOnMenuProperty();
        assertTrue("There must be at least one Content Type with a 'Show On Menu' field, like the 'Page' type.",
                upgradeTask.forceRun());
        try {
            upgradeTask.executeUpgrade();
            final List<Map<String, Object>> typesWithShowOnMenuField =
                    new DotConnect().setSQL(FIND_CONTENT_TYPES_WITH_FIELD).addParam(VELOCITY_VAR_NAME).loadObjectResults();
            for (final Map<String, Object> contentTypeInfo : typesWithShowOnMenuField) {
                final String sqlQuery = String.format(FIND_CONTENTS_WITH_INCORRECT_SHOW_ON_MENU_VALUE,
                        getExpectedColumnReference(contentTypeInfo.get("field_contentlet").toString()));
                final String contentTypeId = contentTypeInfo.get("structure_inode").toString();
                final List<Map<String, Object>> contentletList =
                        new DotConnect().setSQL(sqlQuery).addParam(contentTypeId).loadObjectResults();
                assertTrue("There must be NO contentlets with an inconsistent 'Show On Menu' field after the upgrade" +
                                   ".", "0".equals(contentletList.get(0).get("count").toString()));
            }
        } catch (final Exception e) {
            throw new AssertionError(String.format("An error occurred when running the 'Task220912UpdateCorrectShowOnMenuProperty' " +
                                       "UT: %s", e.getMessage()), e);
        }
    }

    /**
     * Returns the expected column based on the current JSON-supported database. If not, the legacy {@code text }column
     * name will be used.
     *
     * @param columnName The legacy text column.
     *
     * @return The correct database column.
     */
    private String getExpectedColumnReference(String columnName) {
        if (APILocator.getContentletJsonAPI().isJsonSupportedDatabase()) {
            if (isPostgres()) {
                columnName = ContentletJsonAPI.CONTENTLET_AS_JSON + "->'fields'->'" + VELOCITY_VAR_NAME + "'->>'value'";
            } else if (isMsSql()) {
                columnName =
                        "JSON_VALUE(" + ContentletJsonAPI.CONTENTLET_AS_JSON + ", '$.fields." + VELOCITY_VAR_NAME + ".value')";
            }
        }
        return columnName;
    }

}
