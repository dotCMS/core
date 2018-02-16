package com.dotmarketing.startup.runonce;

import com.dotmarketing.beans.Host;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.MaintenanceUtil;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;

import java.util.*;

public class Task04345ChangeMultiTreePrimaryKey extends AbstractJDBCStartupTask  {


    public boolean forceRun() {
        return true;
    }

    private static final String query = "ALTER TABLE multi_tree DROP CONSTRAINT multi_tree_pkey;" +
            "ALTER TABLE multi_tree ADD CONSTRAINT multi_tree_pkey PRIMARY KEY (child, parent1, parent2, relation_type);";
    private static final  String queryMySQL = "ALTER TABLE multi_tree DROP PRIMARY KEY;" +
            "ALTER TABLE multi_tree ADD PRIMARY KEY (child, parent1, parent2, relation_type);";

    @Override
    public String getPostgresScript() {
        return query;
    }

    @Override
    public String getMySQLScript() {
        return queryMySQL;
    }

    @Override
    public String getOracleScript() {
        return query;
    }

    @Override
    public String getMSSQLScript() {
        return query;
    }

    @Override
    public String getH2Script() {
        return query;
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }
}
