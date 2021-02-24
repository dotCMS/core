package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * Task used to remove user_proxy table from database
 */
public class Task210224DropUserProxyTable implements StartupTask {

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {

        DotConnect dotConnect = new DotConnect();
        //remove user_proxy table
        try {
            if (DbConnectionFactory.isOracle()){
                dotConnect.setSQL("SELECT COUNT(*) as exist FROM user_tables WHERE table_name='USER_PROXY'");
                BigDecimal existTable = (BigDecimal) dotConnect.loadObjectResults().get(0).get("exist");
                if(existTable.longValue() > 0) {
                    new DotConnect().executeStatement("drop table USER_PROXY");
                }
            } else{
                new DotConnect().executeStatement("drop table if exists USER_PROXY");
            }
        } catch (SQLException e) {
            throw new DotRuntimeException(e);
        }
    }

    @Override
    public boolean forceRun() {
        try {
            return new DotDatabaseMetaData().tableExists(
                    DbConnectionFactory.getConnection(), DbConnectionFactory.isOracle()? "USER_PROXY": "user_proxy");
        } catch (SQLException e) {

            return Boolean.FALSE;
        }
    }
}
