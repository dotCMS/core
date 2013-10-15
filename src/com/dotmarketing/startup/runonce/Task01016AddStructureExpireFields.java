package com.dotmarketing.startup.runonce;

import java.sql.Connection;
import java.util.List;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;

public class Task01016AddStructureExpireFields extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun() {
        // if getting expire_date_var,publish_date_var from structure fails
        // then add the new fields. use an aditional connection to avoid 
        // interrupting the transaction.
        Connection con=null;
        try {
            con=DbConnectionFactory.getDataSource().getConnection();
            con.setAutoCommit(true);
            
            DotConnect dc=new DotConnect();
            dc.setSQL("select expire_date_var,publish_date_var from structure");
            dc.loadResult(con);
        }
        catch(Exception ex) {
            return true;
        }
        finally {
            if(con!=null) {
                try { con.close(); }
                catch(Exception ex) 
                { Logger.error(this, ex.getMessage(),ex);}
            }
        }
        return false;
    }

    @Override
    public String getPostgresScript() {
        return "alter table structure add expire_date_var varchar(255);\n"+
               "alter table structure add publish_date_var varchar(255);\n";
    }

    @Override
    public String getMySQLScript() {
        return "alter table structure add expire_date_var varchar(255);\n"+
               "alter table structure add publish_date_var varchar(255);\n";
    }

    @Override
    public String getOracleScript() {
        return "alter table structure add expire_date_var varchar2(255);\n"+
               "alter table structure add publish_date_var varchar2(255);\n";
    }

    @Override
    public String getMSSQLScript() {
        return "alter table structure add expire_date_var varchar(255);\n"+
               "alter table structure add publish_date_var varchar(255);\n";
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }

}
