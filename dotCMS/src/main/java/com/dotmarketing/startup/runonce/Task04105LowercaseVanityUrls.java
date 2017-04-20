package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

/**
 * This upgrade task will perform an update operation on all the records of the
 * {@code virtual_link} table. The values of the {@code url} column will be
 * lower-cased in order to take advantage of the performance improvements
 * provided by database indexes.
 * 
 * @author Jose Castro
 * @version 4.1.0
 * @since Apr 11, 2017
 *
 */
public class Task04105LowercaseVanityUrls extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public String getPostgresScript() {
        return "UPDATE virtual_link SET url = LOWER(url);";
    }

    @Override
    public String getMySQLScript() {
        return "UPDATE virtual_link SET url = LOWER(url);";
    }

    @Override
    public String getOracleScript() {
        return "UPDATE virtual_link SET url = LOWER(url);";
    }

    @Override
    public String getMSSQLScript() {
        return "UPDATE virtual_link SET url = LOWER(url);";
    }

    @Override
    public String getH2Script() {
        return "UPDATE virtual_link SET url = LOWER(url);";
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }

}
