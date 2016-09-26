package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.startup.AbstractJDBCStartupTask;

import java.util.List;

/**
 * @author Nollymar Longa
 * @since 07/08/2016
 */
public class Task03550RenameContainersTable extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun() {
        try {
            DotConnect dc=new DotConnect();
            dc.setSQL("select identifier from dot_containers");
            dc.loadResult();
        }
        catch(Exception ex) {
            return true;
        }
        return false;
    }

    @Override
    public String getPostgresScript() {
        return "ALTER TABLE containers RENAME TO dot_containers";
    }

    @Override
    public String getMySQLScript() {
        return "RENAME TABLE containers TO dot_containers";
    }

    @Override
    public String getOracleScript() {
        return "ALTER TABLE containers RENAME TO dot_containers";
    }

    @Override
    public String getMSSQLScript() {
        return "exec sp_rename 'containers','dot_containers';";
    }

    @Override
    public String getH2Script() {
        return "ALTER TABLE containers RENAME TO dot_containers";
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }

}
