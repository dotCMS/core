package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Config;

public class Task01320PostgresqlIndiciesFK extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun() {
        return DbConnectionFactory.isPostgres() && Config.getBooleanProperty("ENABLE_Task01320PostgresqlIndiciesFK",true);
    }

    @Override
    public String getPostgresScript() {
        return  "create index idx_fileasset_vi_live on fileasset_version_info(live_inode);\n"+
                "create index idx_fileasset_vi_working on fileasset_version_info(working_inode);\n"+
                "create index idx_link_vi_live on link_version_info(live_inode);\n"+
                "create index idx_link_vi_working on link_version_info(working_inode);\n"+
                "create index idx_container_vi_live on container_version_info(live_inode);\n"+
                "create index idx_container_vi_working on container_version_info(working_inode);\n"+
                "create index idx_template_vi_live on template_version_info(live_inode);\n"+
                "create index idx_template_vi_working on template_version_info(working_inode);\n"+
                "create index idx_contentlet_vi_live on contentlet_version_info(live_inode);\n"+
                "create index idx_contentlet_vi_working on contentlet_version_info(working_inode);\n"+
                "create index idx_htmlpage_vi_live on htmlpage_version_info(live_inode);\n"+
                "create index idx_htmlpage_vi_working on htmlpage_version_info(working_inode);\n"+
                "create index folder_ident on folder (identifier);\n"+
                "create index contentlet_ident on contentlet (identifier);\n"+
                "create index links_ident on links (identifier);\n"+
                "create index htmlpage_ident on htmlpage (identifier);\n"+
                "create index containers_ident on containers (identifier);\n"+
                "create index template_ident on template (identifier);\n"+
                "create index contentlet_moduser on contentlet (mod_user);\n"+
                "create index contentlet_lang on contentlet (language_id);\n";
    }

    @Override
    public String getMySQLScript() {
        return "";
    }

    @Override
    public String getOracleScript() {
        return "";
    }

    @Override
    public String getMSSQLScript() {
        return "";
    }

    @Override
    public String getH2Script() {
        return "";
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }

}
