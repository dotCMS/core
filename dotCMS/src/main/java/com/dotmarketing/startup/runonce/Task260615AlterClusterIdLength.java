package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

import java.sql.SQLException;

/**
 * Increases the size of the varchar column {@code cluster_id} in the {@code dot_cluster} table
 * from 36 to 255 chars. Idempotent via {@link #forceRun()}.
 *
 * @since Jun 15th, 2026
 */
public class Task260615AlterClusterIdLength implements StartupTask {

    @Override
    public boolean forceRun() {
        return !(new DotDatabaseMetaData().isColumnLengthExpected("dot_cluster",
                "cluster_id", "255"));
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        final DotConnect dc = new DotConnect();
        try {
            dc.executeStatement("ALTER TABLE dot_cluster ALTER COLUMN cluster_id type varchar (255)");
        } catch (SQLException e) {
            throw new DotRuntimeException(e);
        }
    }
}
