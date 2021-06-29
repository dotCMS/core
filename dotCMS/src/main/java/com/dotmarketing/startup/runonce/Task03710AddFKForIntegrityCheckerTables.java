package com.dotmarketing.startup.runonce;

import java.sql.SQLException;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

public class Task03710AddFKForIntegrityCheckerTables implements StartupTask {

    private void createMissingFKForIRTables(DotConnect dc) throws SQLException, DotDataException {
        if(DbConnectionFactory.isMsSql()) {
            //truncate tables before applying FKs
            dc.executeStatement("truncate table folders_ir;");
            dc.executeStatement("truncate table structures_ir;");
            dc.executeStatement("truncate table schemes_ir;");
            dc.executeStatement("truncate table htmlpages_ir;");
            dc.executeStatement("truncate table fileassets_ir;");
            //drop tables and recreate them (We need this because language_id is a PK so you can not change the type since a constraint its created by default)
            dc.executeStatement("drop table htmlpages_ir;");
            dc.executeStatement("drop table fileassets_ir;");
            dc.executeStatement("create table htmlpages_ir(html_page nvarchar(255), local_working_inode nvarchar(36), local_live_inode nvarchar(36), remote_working_inode nvarchar(36), remote_live_inode nvarchar(36),local_identifier nvarchar(36), "
            		+ "remote_identifier nvarchar(36), endpoint_id nvarchar(36), language_id bigint, PRIMARY KEY (local_working_inode, language_id, endpoint_id));");
            dc.executeStatement("create table fileassets_ir(file_name nvarchar(255), local_working_inode nvarchar(36), local_live_inode nvarchar(36), remote_working_inode nvarchar(36), remote_live_inode nvarchar(36),local_identifier nvarchar(36), "
            		+ "remote_identifier nvarchar(36), endpoint_id nvarchar(36), language_id bigint, PRIMARY KEY (local_working_inode, language_id, endpoint_id));");

            //add FKS
            dc.executeStatement("alter table folders_ir add constraint FK_folder_ir_ep foreign key (endpoint_id) references publishing_end_point(id);");
            dc.executeStatement("alter table structures_ir add constraint FK_structure_ir_ep foreign key (endpoint_id) references publishing_end_point(id);");
            dc.executeStatement("alter table schemes_ir add constraint FK_scheme_ir_ep foreign key (endpoint_id) references publishing_end_point(id);");
            dc.executeStatement("alter table htmlpages_ir add constraint FK_page_ir_ep foreign key (endpoint_id) references publishing_end_point(id);");
            dc.executeStatement("alter table fileassets_ir add constraint FK_file_ir_ep foreign key (endpoint_id) references publishing_end_point(id);");
        }else if(DbConnectionFactory.isOracle()) {
          //truncate tables before applying FKs
            dc.executeStatement("truncate table folders_ir");
            dc.executeStatement("truncate table structures_ir");
            dc.executeStatement("truncate table schemes_ir");
            dc.executeStatement("truncate table htmlpages_ir");
            dc.executeStatement("truncate table fileassets_ir");
            //add FKS
            dc.executeStatement("alter table folders_ir add constraint FK_folder_ir_ep foreign key (endpoint_id) references publishing_end_point(id)");
            dc.executeStatement("alter table structures_ir add constraint FK_structure_ir_ep foreign key (endpoint_id) references publishing_end_point(id)");
            dc.executeStatement("alter table schemes_ir add constraint FK_scheme_ir_ep foreign key (endpoint_id) references publishing_end_point(id)");
            dc.executeStatement("alter table htmlpages_ir add constraint FK_page_ir_ep foreign key (endpoint_id) references publishing_end_point(id)");
            dc.executeStatement("alter table fileassets_ir add constraint FK_file_ir_ep foreign key (endpoint_id) references publishing_end_point(id)");
        }else if(DbConnectionFactory.isMySql()) {
            //truncate tables before applying FKs
            dc.executeStatement("truncate table folders_ir;");
            dc.executeStatement("truncate table structures_ir;");
            dc.executeStatement("truncate table schemes_ir;");
            dc.executeStatement("truncate table htmlpages_ir;");
            dc.executeStatement("truncate table fileassets_ir;");
            //add FKS
            dc.executeStatement("alter table folders_ir add constraint FK_folder_ir_ep foreign key (endpoint_id) references publishing_end_point(id);");
            dc.executeStatement("alter table structures_ir add constraint FK_structure_ir_ep foreign key (endpoint_id) references publishing_end_point(id);");
            dc.executeStatement("alter table schemes_ir add constraint FK_scheme_ir_ep foreign key (endpoint_id) references publishing_end_point(id);");
            dc.executeStatement("alter table htmlpages_ir add constraint FK_page_ir_ep foreign key (endpoint_id) references publishing_end_point(id);");
            dc.executeStatement("alter table fileassets_ir add constraint FK_file_ir_ep foreign key (endpoint_id) references publishing_end_point(id);");
        }else if(DbConnectionFactory.isPostgres()) {
            //truncate tables before applying FKs
            dc.executeStatement("truncate table folders_ir;");
            dc.executeStatement("truncate table structures_ir;");
            dc.executeStatement("truncate table schemes_ir;");
            dc.executeStatement("truncate table htmlpages_ir;");
            dc.executeStatement("truncate table fileassets_ir;");
            //add FKS
            dc.executeStatement("alter table folders_ir add constraint FK_folder_ir_ep foreign key (endpoint_id) references publishing_end_point(id);");
            dc.executeStatement("alter table structures_ir add constraint FK_structure_ir_ep foreign key (endpoint_id) references publishing_end_point(id);");
            dc.executeStatement("alter table schemes_ir add constraint FK_scheme_ir_ep foreign key (endpoint_id) references publishing_end_point(id);");
            dc.executeStatement("alter table htmlpages_ir add constraint FK_page_ir_ep foreign key (endpoint_id) references publishing_end_point(id);");
            dc.executeStatement("alter table fileassets_ir add constraint FK_file_ir_ep foreign key (endpoint_id) references publishing_end_point(id);");
        }
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
        try {
            DotConnect dc=new DotConnect();
            createMissingFKForIRTables(dc);
        } catch (SQLException e) {
            throw new DotRuntimeException(e.getMessage(),e);
        }

    }

    @Override
    public boolean forceRun() {
        return true;
    }

}
