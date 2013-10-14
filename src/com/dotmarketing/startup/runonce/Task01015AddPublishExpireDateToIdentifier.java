package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;

public class Task01015AddPublishExpireDateToIdentifier extends AbstractJDBCStartupTask {
    
    @Override
    public boolean forceRun() {
        try {
            DotConnect dc=new DotConnect();
            dc.setSQL("select publish_date,expire_date from identifier where publish_date>"
                      +DbConnectionFactory.getDBDateTimeFunction());
            dc.loadResult();
        }
        catch(Exception ex) {
            return true;
        }
        return false;
    }
    
    @Override
    public String getPostgresScript() {        
        return "alter table identifier add syspublish_date timestamp;" +
        		"alter table identifier add sysexpire_date timestamp;" +
                "create index idx_identifier_pub on identifier (syspublish_date);"+
                "create index idx_identifier_exp on identifier (sysexpire_date);";
    }
    
    @Override
    public String getMySQLScript() {
        return "alter table identifier add syspublish_date datetime;" +
                "alter table identifier add sysexpire_date datetime;"+
                "create index idx_identifier_pub on identifier (syspublish_date);"+
                "create index idx_identifier_exp on identifier (sysexpire_date);";
        
    }
    
    @Override
    public String getOracleScript() {
        return "alter table identifier add syspublish_date timestamp;" +
                "alter table identifier add sysexpire_date timestamp;"+
                "create index idx_identifier_pub on identifier (syspublish_date);"+
                "create index idx_identifier_exp on identifier (sysexpire_date);";
    }
    
    @Override
    public String getMSSQLScript() {
        return "alter table identifier add syspublish_date datetime;" +
                "alter table identifier add sysexpire_date datetime;"+
                "create index idx_identifier_pub on identifier (syspublish_date);"+
                "create index idx_identifier_exp on identifier (sysexpire_date);";
    }
    
    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }
    
}
