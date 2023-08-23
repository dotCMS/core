package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;

/**
 * Simple adds a new column to the structure table when missing
 */
public class Task230328AddMarkedForDeletionColumn implements StartupTask {

    @Override
    public boolean forceRun() {
        final DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();
        return  Try.of(()->!dotDatabaseMetaData.hasColumn("structure", "marked_for_deletion")).getOrElse(false);
    }

    @Override
    public void executeUpgrade() {
        Try.run(() -> {
            String alterTable = null;
            if(DbConnectionFactory.isPostgres()){
                alterTable = "ALTER TABLE structure ADD COLUMN marked_for_deletion bool not null default false";
            }
            if(DbConnectionFactory.isMsSql()){
                alterTable = "ALTER TABLE structure ADD COLUMN marked_for_deletion tinyint not null default 0";
            }
            if(null == alterTable){
                throw new DotRuntimeException("Unsupported DB!");
            }
            final DotConnect dotConnect = new DotConnect();
            dotConnect.executeStatement(alterTable);
        }).onFailure(e -> {
            final String message = "Error adding marked_for_deletion column to structure table.";
            Logger.error(this, message, e);
            throw new DotRuntimeException(message,e);
        });
    }

}
