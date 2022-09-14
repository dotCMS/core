package com.dotmarketing.startup.runonce;


import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import java.sql.SQLException;

public class Task220822CreateVariantTable implements StartupTask {

    private final String POSTGRES_QUERY = "CREATE TABLE IF NOT EXISTS variant ("
            + "  id varchar(255) primary key,"
            + "  name varchar(255) not null UNIQUE,"
            + "  archived boolean NOT NULL default false"
            + ")";

    private static final String MSSQL_VALIDATE	=	"SELECT COUNT(*) as exist " +
            "FROM sysobjects " +
            "WHERE name = 'variant'";

    private final String MSSQL_QUERY = "CREATE TABLE variant ("
            + "  id NVARCHAR(255) primary key,"
            + "  name NVARCHAR(255) not null UNIQUE,"
            + "  archived tinyint not null"
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
        final DotConnect dotConnect = new DotConnect();

        if (DbConnectionFactory.isMsSql()) {
            dotConnect.setSQL(MSSQL_VALIDATE);
            int existTable = (Integer) dotConnect.loadObjectResults().get(0).get("exist");

            if(existTable == 0){
                dotConnect.setSQL(MSSQL_QUERY).loadResult();
            }
        } else {
            dotConnect.setSQL(POSTGRES_QUERY).loadResult();
        }
    }
}
