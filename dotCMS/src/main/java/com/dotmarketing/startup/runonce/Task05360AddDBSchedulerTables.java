package com.dotmarketing.startup.runonce;

import static com.dotcms.util.CollectionsUtils.map;
import java.sql.SQLException;
import java.util.Map;
import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.DbType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

public class Task05360AddDBSchedulerTables implements StartupTask {

    final String pgsql = "create table scheduled_tasks (\n" + 
                    "  task_name text not null,\n" + 
                    "  task_instance text not null,\n" + 
                    "  task_data bytea,\n" + 
                    "  execution_time timestamp with time zone not null,\n" + 
                    "  picked BOOLEAN not null,\n" + 
                    "  picked_by text,\n" + 
                    "  last_success timestamp with time zone,\n" + 
                    "  last_failure timestamp with time zone,\n" + 
                    "  consecutive_failures INT,\n" + 
                    "  last_heartbeat timestamp with time zone,\n" + 
                    "  version BIGINT not null,\n" + 
                    "  PRIMARY KEY (task_name, task_instance)\n" + 
                    ")";
    
    final String mysql = "create table test.scheduled_tasks (\n" + 
                    "  task_name varchar(40) not null,\n" + 
                    "  task_instance varchar(40) not null,\n" + 
                    "  task_data blob,\n" + 
                    "  execution_time timestamp(6) not null,\n" + 
                    "  picked BOOLEAN not null,\n" + 
                    "  picked_by varchar(50),\n" + 
                    "  last_success timestamp(6) null,\n" + 
                    "  last_failure timestamp(6) null,\n" + 
                    "  consecutive_failures INT,\n" + 
                    "  last_heartbeat timestamp(6) null,\n" + 
                    "  version BIGINT not null,\n" + 
                    "  PRIMARY KEY (task_name, task_instance)\n" + 
                    ")";
    
    final String mssql = "create table scheduled_tasks (\n" + 
                    "  task_name varchar(250) not null,\n" + 
                    "  task_instance varchar(250) not null,\n" + 
                    "  task_data  nvarchar(max),\n" + 
                    "  execution_time datetimeoffset  not null,\n" + 
                    "  picked bit,\n" + 
                    "  picked_by text,\n" + 
                    "  last_success datetimeoffset ,\n" + 
                    "  last_failure datetimeoffset ,\n" + 
                    "  consecutive_failures INT,\n" + 
                    "  last_heartbeat datetimeoffset ,\n" + 
                    "  [version] BIGINT not null,\n" + 
                    "  PRIMARY KEY (task_name, task_instance)\n" + 
                    ")";
    
    final String oracle = "create table scheduled_tasks (\n" + 
                    "    task_name            varchar(100),\n" + 
                    "    task_instance        varchar(100),\n" + 
                    "    task_data            blob,\n" + 
                    "    execution_time       TIMESTAMP(6),\n" + 
                    "    picked               NUMBER(1, 0),\n" + 
                    "    picked_by            varchar(50),\n" + 
                    "    last_success         TIMESTAMP(6),\n" + 
                    "    last_failure         TIMESTAMP(6),\n" + 
                    "    consecutive_failures NUMBER(19, 0),\n" + 
                    "    last_heartbeat       TIMESTAMP(6),\n" + 
                    "    version              NUMBER(19, 0),\n" + 
                    "    PRIMARY KEY (task_name, task_instance)\n" + 
                    ")";
    
    private final Map<DbType, String> createDbScheduler = map(

            DbType.POSTGRESQL,   pgsql,
            DbType.MYSQL,        mysql,
            DbType.ORACLE,       oracle,
            DbType.MSSQL,        mssql
    );


    @Override
    @CloseDBIfOpened
    public boolean forceRun() {

        try {

            return !new DotDatabaseMetaData().tableExists(
                    DbConnectionFactory.getConnection(), "scheduled_tasks");
        } catch (SQLException e) {

            return Boolean.FALSE;
        }
    }

    @Override
    public void executeUpgrade() throws DotDataException {
        if (DbConnectionFactory.isMsSql() && !DbConnectionFactory.getAutoCommit()) {
            DbConnectionFactory.setAutoCommit(true);
        }
        try {
            this.createSchedulerTable();

        } catch (SQLException e) {

            Logger.error(this, e.getMessage(), e);
            throw new DotDataException(e.getMessage(), e);
        }
    } 

    private void createSchedulerTable() throws SQLException {

        Logger.info(this, "Creates the table scheduled_tasks.");
        final DbType dbType =
                        DbType.getDbType(DbConnectionFactory.getDBType());
        try {

            new DotConnect().executeStatement(createDbScheduler.get(dbType));
        } catch (SQLException e) {
            Logger.error(this, "The table 'scheduled_tasks' could not be created.", e);
            throw  e;
        }
    }



}
