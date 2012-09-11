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
public class Task00940AlterTemplateTable implements StartupTask {

    public boolean forceRun() {
        return true;
    }

    void alterProcedure() throws SQLException {
    	DotConnect dc=new DotConnect();

        if(DbConnectionFactory.isMsSql()) {
            dc.executeStatement("ALTER TABLE TEMPLATE ADD DRAWED tinyint");
            dc.executeStatement("ALTER TABLE TEMPLATE ADD DRAWED_BODY text");
            dc.executeStatement("ALTER TABLE TEMPLATE ADD ADD_CONTAINER_LINKS int");
            dc.executeStatement("ALTER TABLE TEMPLATE ADD CONTAINERS_ADDED int");
            dc.executeStatement("ALTER TABLE TEMPLATE ADD HEAD_CODE text");
            dc.executeStatement("ALTER TABLE CONTAINERS ADD FOR_METADATA tinyint");
        }
        else if(DbConnectionFactory.isOracle()) {
        	dc.executeStatement("ALTER TABLE TEMPLATE ADD DRAWED  number(1,0)");
            dc.executeStatement("ALTER TABLE TEMPLATE ADD DRAWED_BODY nclob");
            dc.executeStatement("ALTER TABLE TEMPLATE ADD ADD_CONTAINER_LINKS number(10,0)");
            dc.executeStatement("ALTER TABLE TEMPLATE ADD CONTAINERS_ADDED number(10,0)");
            dc.executeStatement("ALTER TABLE TEMPLATE ADD HEAD_CODE nclob");
            dc.executeStatement("ALTER TABLE CONTAINERS ADD FOR_METADATA number(1,0)");
        }
        else if(DbConnectionFactory.isMySql()) {
        	dc.executeStatement("ALTER TABLE TEMPLATE ADD DRAWED  tinyint(1)");
            dc.executeStatement("ALTER TABLE TEMPLATE ADD DRAWED_BODY longtext");
            dc.executeStatement("ALTER TABLE TEMPLATE ADD ADD_CONTAINER_LINKS integer");
            dc.executeStatement("ALTER TABLE TEMPLATE ADD CONTAINERS_ADDED integer");
            dc.executeStatement("ALTER TABLE TEMPLATE ADD HEAD_CODE longtext");
            dc.executeStatement("ALTER TABLE CONTAINERS ADD FOR_METADATA tinyint(1)");
        }
        else if(DbConnectionFactory.isPostgres()) {
        	dc.executeStatement("ALTER TABLE TEMPLATE ADD DRAWED  bool");
            dc.executeStatement("ALTER TABLE TEMPLATE ADD DRAWED_BODY text");
            dc.executeStatement("ALTER TABLE TEMPLATE ADD ADD_CONTAINER_LINKS int4");
            dc.executeStatement("ALTER TABLE TEMPLATE ADD CONTAINERS_ADDED int4");
            dc.executeStatement("ALTER TABLE TEMPLATE ADD HEAD_CODE text");
            dc.executeStatement("ALTER TABLE CONTAINERS ADD FOR_METADATA bool");
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
