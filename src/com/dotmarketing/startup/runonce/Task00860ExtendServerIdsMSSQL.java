package com.dotmarketing.startup.runonce;

import java.sql.SQLException;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

/**
 *
 *
 * @author Daniel Silva
 */
public class Task00860ExtendServerIdsMSSQL implements StartupTask {

    public boolean forceRun() {
        return true;
    }

    void alterProcedure() throws SQLException {
        if (DbConnectionFactory.isMsSql()) {
        	DotConnect dc = new DotConnect();
        	String alterSql = "ALTER PROCEDURE load_records_to_index(@server_id VARCHAR(100), @records_to_fetch INT)\r\n" +
        	"AS\r\n" +
        	"BEGIN\r\n" +
        	"WITH cte AS (\r\n" +
        	"  SELECT TOP(@records_to_fetch) *\r\n" +
        	"  FROM dist_reindex_journal WITH (ROWLOCK, READPAST, UPDLOCK)\r\n" +
        	"  WHERE serverid IS NULL\r\n" +
        	"  ORDER BY priority ASC)\r\n" +
        	"UPDATE cte\r\n" +
        	"  SET serverid=@server_id\r\n" +
        	"OUTPUT\r\n" +
        	"  INSERTED.*\r\n" +
        	"END;";
        	dc.executeStatement(alterSql);
        }
    }


    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
        try {
        	alterProcedure();
        } catch (Exception ex) {
            throw new DotRuntimeException(ex.getMessage(), ex);
        }
    }


}
