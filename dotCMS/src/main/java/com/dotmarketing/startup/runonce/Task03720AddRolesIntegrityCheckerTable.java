package com.dotmarketing.startup.runonce;

import java.sql.SQLException;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

public class Task03720AddRolesIntegrityCheckerTable implements StartupTask {

    private void createRolesIntegrityCheckerTable(DotConnect dc) throws SQLException, DotDataException {
        if(DbConnectionFactory.isMsSql()) {
            //Add Table
            dc.executeStatement("create table cms_roles_ir(name nvarchar(1000), role_key nvarchar(255), local_role_id nvarchar(36), remote_role_id nvarchar(36), local_role_fqn nvarchar(1000), remote_role_fqn nvarchar(1000), endpoint_id nvarchar(36), PRIMARY KEY (local_role_id, endpoint_id));");

            //Add Foreign Key
            dc.executeStatement("alter table cms_roles_ir add constraint FK_cms_roles_ir_ep foreign key (endpoint_id) references publishing_end_point(id);");
        } else {
            if(DbConnectionFactory.isOracle()) {
                //Add Table
                dc.executeStatement("create table cms_roles_ir(name varchar2(1000), role_key varchar2(255), local_role_id varchar2(36), remote_role_id varchar2(36), local_role_fqn varchar2(1000), remote_role_fqn varchar2(1000), endpoint_id varchar2(36), PRIMARY KEY (local_role_id, endpoint_id))");

                //Add Foreign Key
                dc.executeStatement("alter table cms_roles_ir add constraint FK_cms_roles_ir_ep foreign key (endpoint_id) references publishing_end_point(id)");
            }else {
        	    //Add Table
                dc.executeStatement("create table cms_roles_ir(name varchar(1000), role_key varchar(255), local_role_id varchar(36), remote_role_id varchar(36), local_role_fqn varchar(1000), remote_role_fqn varchar(1000), endpoint_id varchar(36), PRIMARY KEY (local_role_id, endpoint_id));");

                //Add Foreign Key
                dc.executeStatement("alter table cms_roles_ir add constraint FK_cms_roles_ir_ep foreign key (endpoint_id) references publishing_end_point(id);");
            }
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
            createRolesIntegrityCheckerTable(dc);
        } catch (SQLException e) {
            throw new DotRuntimeException(e.getMessage(),e);
        }

    }

    @Override
    public boolean forceRun() {
        return true;
    }

}
