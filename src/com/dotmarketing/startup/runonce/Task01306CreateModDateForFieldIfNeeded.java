package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;

public class Task01306CreateModDateForFieldIfNeeded extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun() {
        try {
            DotConnect dc=new DotConnect();
            dc.setSQL("select mod_date from field");
            dc.loadResult();
        }
        catch(Exception ex) {
            return true;
        }
        return false;
    }

    public String getGenericScript() {
        return "alter table field add mod_date "+DbConnectionFactory.getDBDateTimeType()+";\n"+
               "update field set mod_date = "+DbConnectionFactory.getDBDateTimeFunction()+";\n";
    }
    
    @Override
    public String getPostgresScript() {
        return getGenericScript();
    }

    @Override
    public String getMySQLScript() {
        return getGenericScript();
    }

    @Override
    public String getOracleScript() {
        return getGenericScript();
    }

    @Override
    public String getMSSQLScript() {
        return getGenericScript();
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }

    @Override
    public String getH2Script() {
        return null;
    }

}
