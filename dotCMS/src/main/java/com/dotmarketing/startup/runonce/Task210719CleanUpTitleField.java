package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import java.sql.SQLException;

public class Task210719CleanUpTitleField implements StartupTask {
    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {

        try {
             new DotConnect()
                    .executeStatement("update contentlet set title=null");
        } catch (SQLException exception) {
            throw new DotRuntimeException(exception);
        }
    }
}
