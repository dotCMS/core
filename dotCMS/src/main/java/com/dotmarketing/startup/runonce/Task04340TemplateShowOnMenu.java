package com.dotmarketing.startup.runonce;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import java.util.Collections;
import java.util.List;

/**
 * Both Templates and Layouts are stored in the Template table
 * Templates will have show_on_menu = true
 * Layouts will have show_on_menu = false
 * Currently layouts are distinguished because the title starts with anonymous_layout_ prefix ,
 * and show_on_menu is currently false for all
 * @author andrecurione
 */
public class Task04340TemplateShowOnMenu extends AbstractJDBCStartupTask {

    private static final String UPDATE_TEMPLATE = "UPDATE template SET show_on_menu = " + DbConnectionFactory.getDBTrue()
            + " WHERE TITLE NOT LIKE 'anonymous_layout_%'";

    @Override
    public boolean forceRun() {
        return Boolean.TRUE;
    }

    @Override
    public String getPostgresScript() {
        return UPDATE_TEMPLATE;
    }

    @Override
    public String getMySQLScript() {
        return UPDATE_TEMPLATE;
    }

    @Override
    public String getMSSQLScript() {
        return UPDATE_TEMPLATE;
    }

    @Override
    public String getOracleScript() {
        return UPDATE_TEMPLATE;
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return Collections.emptyList();
    }
}
