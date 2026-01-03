package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import java.sql.SQLException;

public class Task251212AddVersionColumnIndicesTable implements StartupTask {

    @Override
    public boolean forceRun() {
        try {
            return !new DotDatabaseMetaData().hasColumn("indicies", "index_version");
        } catch (SQLException e) {
            Logger.error(this, e.getMessage(),e);
            return false;
        }
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
        final DotConnect dropIndexStatement = new DotConnect().setSQL("ALTER TABLE indicies DROP CONSTRAINT IF EXISTS indicies_index_type_key");
        dropIndexStatement.loadResult();

        final DotConnect addColumnStatement = new DotConnect().setSQL("ALTER TABLE indicies ADD COLUMN IF NOT EXISTS index_version varchar(16) NULL");
        addColumnStatement.loadResult();

        //Only one type (working,live,site-search) of index per version can exist
        final DotConnect addIndexStatement = new DotConnect().setSQL("ALTER TABLE indicies ADD CONSTRAINT uq_index_type_version UNIQUE (index_type, index_version)");
        addIndexStatement.loadResult();

    }


}
