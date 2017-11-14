package com.dotmarketing.startup.runonce;

import com.dotmarketing.startup.AbstractJDBCStartupTask;
import java.util.List;

/**
 * @author Jonathan Gamba 11/14/17
 */
public class Task04230FixVanityURLInconsistencies extends AbstractJDBCStartupTask {

    private static final String UPDATE_QUERY = "SELECT contentlet.text2, identifier.host_inode"
            + " FROM contentlet, identifier"
            + " WHERE contentlet.identifier = identifier.id"
            + " AND contentlet.structure_inode IN (select inode from structure where structuretype=7)"
            + " AND contentlet.text2 != identifier.host_inode;";

    @Override
    public String getMSSQLScript() {
        return UPDATE_QUERY;
    }

    @Override
    public String getMySQLScript() {
        return UPDATE_QUERY;
    }

    @Override
    public String getOracleScript() {
        return UPDATE_QUERY;
    }

    @Override
    public String getPostgresScript() {
        return UPDATE_QUERY;
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }

    public boolean forceRun() {
        return true;
    }

    @Override
    public String getH2Script() {
        return null;
    }

}