package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import io.vavr.control.Try;

/**
 * Creates the system table
 * @author jsanca
 */
public class Task230707CreateSystemTable implements StartupTask {

    @Override
    public boolean forceRun() {
        //
        return Try.of(()->!new DotDatabaseMetaData().tableExists(
                DbConnectionFactory.getConnection(), "system_table")).getOrElse(true);
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {

        Try.of(()->new DotConnect().executeStatement("CREATE TABLE if not exists system_table ("
                + "key varchar(511) primary key,"
                + "value text not null"
                + ")")).getOrElseThrow(e-> new DotDataException(e.getMessage(), e));
    }

}
