package com.dotmarketing.startup.runonce;


import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import java.sql.SQLException;

public class Task222208CreateVariantTable implements StartupTask {

    private final String POSTGRES_QUERY = "CREATE TABLE IF NOT EXISTS variant ("
            + "  id varchar(36) primary key,"
            + "  name varchar(255) not null,"
            + "  deleted boolean NOT NULL default false"
            + ")";

    private final String MSSQL_QUERY = "CREATE TABLE IF NOT EXISTS variant ("
            + "  id NVARCHAR(36) primary key,"
            + "  name NVARCHAR(255) not null,"
            + "  deleted tinyint not null"
            + ")";

    @Override
    public boolean forceRun() {
        try {
            return !new DotDatabaseMetaData().tableExists(
                    DbConnectionFactory.getConnection(), "variant");
        } catch (SQLException e) {

            return Boolean.FALSE;
        }
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        final String query = DbConnectionFactory.isMsSql() ? MSSQL_QUERY : POSTGRES_QUERY;
        new DotConnect().setSQL(query).loadResult();
    }
}
