package com.dotmarketing.startup.runonce;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.content.business.json.ContentletJsonAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.util.List;
import java.util.Map;

import static com.dotmarketing.db.DbConnectionFactory.getDBFalse;
import static com.dotmarketing.db.DbConnectionFactory.getDBTrue;
import static com.dotmarketing.db.DbConnectionFactory.isMsSql;
import static com.dotmarketing.db.DbConnectionFactory.isPostgres;

/**
 * This Upgrade Task makes sure that all Contentlets with a {@code showOnMenu} property have the correct information at
 * the database level. There were situations during customer upgrades in which the values of the {@code show_on_menu}
 * and the {@code contentlet_as_json} columns didn't match, which was not the expected output.
 *
 * @author Jose Castro
 * @since Sep 12th, 2022
 */
public class Task220912UpdateCorrectShowOnMenuProperty implements StartupTask {

    private static final String VELOCITY_VAR_NAME = "showOnMenu";
    private static final String FIND_CONTENT_TYPES_WITH_FIELD = "SELECT structure_inode, field_contentlet FROM field WHERE velocity_var_name = ?";
    private static final String UPDATE_SHOW_ON_MENU_COLUMN = "UPDATE contentlet SET show_on_menu = " + getDBTrue() + " WHERE show_on_menu = " + getDBFalse() + " AND %s AND structure_inode = ?";

    @Override
    public boolean forceRun() {
        try {
            // If one or more Content Types have a 'Show On Menu' field, run this task
            final boolean forceRun =
                    UtilMethods.isSet(new DotConnect().setSQL(FIND_CONTENT_TYPES_WITH_FIELD).addParam(VELOCITY_VAR_NAME).loadObjectResults());
            if (!forceRun) {
                Logger.info(this, "There are no Content Types with a 'Show On Menu' field.");
            }
            return forceRun;
        } catch (final DotDataException e) {
            Logger.error(this, "An error occurred when checking what Content Types have a 'Show On Menu' field. " +
                                       "Skipping Upgrade Task...");
            return Boolean.FALSE;
        }
    }

    @WrapInTransaction
    @Override
    public void executeUpgrade() throws DotDataException {
        final List<Map<String, Object>> dbResults =
                new DotConnect().setSQL(FIND_CONTENT_TYPES_WITH_FIELD).addParam(VELOCITY_VAR_NAME).loadObjectResults();
        for (final Map<String, Object> contentTypeInfo : dbResults) {
            final String sqlQuery = String.format(UPDATE_SHOW_ON_MENU_COLUMN, getExpectedColumnReference(contentTypeInfo.get(
                    "field_contentlet").toString()));
            final String contentTypeId = contentTypeInfo.get("structure_inode").toString();
            Logger.info(this, String.format("Updating value of 'Show On Menu' field in all contents of type '%s'",
                    contentTypeId));
            new DotConnect().setSQL(sqlQuery).addParam(contentTypeId).loadObjectResults();
        }
    }

    /**
     * Returns the column or columns that must be inspected in order to get the actual value of the 'Show On Menu'
     * field. For instance, customers may NOT be using a database that supports JSON, in which case the legacy
     * {@code text} column must be inspected, and not the new {@code contentlet_as_json} column. Another scenario is if
     * the {@code contentlet_as_json} column has just been created during the same upgrade and it's still empty. In this
     * case, we still need to read the legacy column.
     *
     * @param columnName Legacy data column from the {@code contentlet} table.
     *
     * @return The SQL code used to read the value of the existing 'Show On Menu' value.
     */
    private String getExpectedColumnReference(String columnName) {
        String columnReference = columnName + " = 'true'";
        if (APILocator.getContentletJsonAPI().isJsonSupportedDatabase()) {
            if (isPostgres()) {
                columnReference += " OR " + ContentletJsonAPI.CONTENTLET_AS_JSON + "->'fields'->'" + VELOCITY_VAR_NAME + "'->>'value' = 'true'";
            } else if (isMsSql()) {
                columnReference += " OR " + "JSON_VALUE(" + ContentletJsonAPI.CONTENTLET_AS_JSON + ", '$.fields." + VELOCITY_VAR_NAME + ".value') = 'true'";
            }
        }
        return "(" + columnReference + ")";
    }

}
