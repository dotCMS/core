package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

import java.sql.SQLException;

/**
 * This task removes the full_path_lc column from the identifier table and creates the full_path_lc function
 * if it doesn't exist. It also creates an index on the identifier table.
 */
public class Task241013RemoveFullPathLcColumnFromIdentifier implements StartupTask {

    private static final String DROP_FULL_PATH_LC_COLUMN = "ALTER TABLE identifier DROP COLUMN full_path_lc;";

    private static final String CREATE_FULL_PATH_LC_FUNCTION =
            "CREATE OR REPLACE FUNCTION full_path_lc(identifier) RETURNS text\n"
                    + "    AS ' SELECT CASE WHEN $1.parent_path = ''/System folder'' then ''/'' else LOWER($1.parent_path || $1.asset_name) end; '\n"
                    + "LANGUAGE SQL;\n";

    protected static final String DROP_INDEX = "DROP INDEX IF EXISTS idx_ident_uniq_asset_name CASCADE";

    private static final String CREATE_INDEX = "CREATE UNIQUE INDEX idx_ident_uniq_asset_name on identifier (full_path_lc(identifier),host_inode)";

    /**
     * Checks if the full_path_lc column exists in the identifier table
     * @return true if the column does exist, false otherwise
     */
    @Override
    public boolean forceRun() {
        try {
            return new DotDatabaseMetaData().hasColumn("identifier", "full_path_lc") ;
        } catch (SQLException e) {
            Logger.error(this, e.getMessage(),e);
            return false;
        }
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        final DotConnect dc = new DotConnect();
        try {
            //Removes full_path_lc column from identifier table
            dc.executeStatement(DROP_FULL_PATH_LC_COLUMN);

            //Creates full_path_lc function if it doesn't exist
            dc.executeStatement(CREATE_FULL_PATH_LC_FUNCTION);

            //Creates index on identifier table
            dc.executeStatement(DROP_INDEX);
            dc.executeStatement(CREATE_INDEX);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }
}
