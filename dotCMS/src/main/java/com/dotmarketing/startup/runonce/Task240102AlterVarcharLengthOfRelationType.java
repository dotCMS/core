package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

import java.sql.SQLException;
import java.util.Map;

/**
 * Increased the size of the varchar column {@code relation_type} in the {@code tree} table
 * to allow for larger name fields.
 *
 * @author Andrey Melendez
 * @since Jan 2nd, 2023
 */
public class Task240102AlterVarcharLengthOfRelationType implements StartupTask {

    @Override
    public boolean forceRun() {
        return !(new DotDatabaseMetaData().isColumnLengthExpected("tree",
                "relation_type","255"));
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        final DotConnect dc = new DotConnect();

        try {
            dc.executeStatement("ALTER TABLE tree ALTER COLUMN relation_type type varchar (255)");
        } catch (SQLException e) {
            throw new DotRuntimeException(e);
        }
    }
}
