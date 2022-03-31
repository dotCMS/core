package com.dotmarketing.startup.runonce;

import com.dotmarketing.startup.AbstractJDBCStartupTask;
import java.util.List;

public class Task220330ChangeVanityURLSiteFieldType extends AbstractJDBCStartupTask  {

    private String getScript(){
        return "UPDATE field SET field_type = 'com.dotcms.contenttype.model.field.HostFolderField' "
                + "WHERE velocity_var_name = 'site' and structure_inode in (select inode from structure where structuretype = 7)";
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

    @Override
    public boolean forceRun() {
        return true;
    }
}
