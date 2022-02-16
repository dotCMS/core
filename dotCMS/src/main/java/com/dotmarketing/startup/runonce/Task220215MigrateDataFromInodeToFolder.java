package com.dotmarketing.startup.runonce;

import com.dotmarketing.startup.AbstractJDBCStartupTask;
import java.util.List;

/**
 * Task used to populate fields: owner and idate in folder table
 */
public class Task220215MigrateDataFromInodeToFolder extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun() {
        return true;
    }

    /**
     * Creates a string builder with the script to be executed in Postgres/MSSQL
     * @return Script compatible with Postgres and MSSQL databases
     */
    private String getScript(){
        final StringBuilder query = new StringBuilder();

        query.append("UPDATE folder SET owner =_owner, idate = _idate from ")
                .append("(SELECT DISTINCT inode.inode _inode, inode.owner _owner, ")
                .append("inode.idate _idate FROM inode WHERE inode.type='folder') my_query");

        return query.toString();
    }

    @Override
    public String getPostgresScript() {
        return getScript();
    }

    @Override
    public String getMySQLScript() {
        return getScript();
    }

    @Override
    public String getOracleScript() {

        return getScript();
    }

    @Override
    public String getMSSQLScript() {
        return getScript();
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }

}
