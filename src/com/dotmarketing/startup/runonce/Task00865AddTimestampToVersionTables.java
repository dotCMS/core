package com.dotmarketing.startup.runonce;

import java.sql.SQLException;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

/**
 *
 *
 * @author Daniel Silva
 */
public class Task00865AddTimestampToVersionTables implements StartupTask {

    public boolean forceRun() {
        return true;
    }

    void alterProcedure() throws SQLException {
    	DotConnect dc=new DotConnect();

        if(DbConnectionFactory.isMsSql()) {
            // MS SQL
            dc.executeStatement("alter table container_version_info add version_ts datetime default getdate() not null");
            dc.executeStatement("alter table contentlet_version_info add version_ts datetime default getdate() not null");
            dc.executeStatement("alter table fileasset_version_info add version_ts datetime default getdate() not null");
            dc.executeStatement("alter table htmlpage_version_info add version_ts datetime default getdate() not null");
            dc.executeStatement("alter table link_version_info add version_ts datetime default getdate() not null");
            dc.executeStatement("alter table template_version_info add version_ts datetime default getdate() not null");
        }
        else if(DbConnectionFactory.isOracle()) {
            // ORACLE
            dc.executeStatement("alter table container_version_info add version_ts timestamp default sysdate not null");
            dc.executeStatement("alter table contentlet_version_info add version_ts timestamp default sysdate not null");
            dc.executeStatement("alter table fileasset_version_info add version_ts timestamp default sysdate not null");
            dc.executeStatement("alter table htmlpage_version_info add version_ts timestamp default sysdate not null");
            dc.executeStatement("alter table link_version_info add version_ts timestamp default sysdate not null");
            dc.executeStatement("alter table template_version_info add version_ts timestamp default sysdate not null");
        }
        else if(DbConnectionFactory.isMySql()) {
            // MySQL
            dc.executeStatement("alter table container_version_info add version_ts datetime");
            dc.executeStatement("update container_version_info set version_ts=now()");
            dc.executeStatement("alter table container_version_info modify version_ts datetime not null");
            
            dc.executeStatement("alter table contentlet_version_info add version_ts datetime");
            dc.executeStatement("update contentlet_version_info set version_ts=now()");
            dc.executeStatement("alter table contentlet_version_info modify version_ts datetime not null");
            
            dc.executeStatement("alter table fileasset_version_info add version_ts datetime");
            dc.executeStatement("update fileasset_version_info set version_ts=now()");
            dc.executeStatement("alter table fileasset_version_info modify version_ts datetime not null");
            
            dc.executeStatement("alter table htmlpage_version_info add version_ts datetime");
            dc.executeStatement("update htmlpage_version_info set version_ts=now()");
            dc.executeStatement("alter table htmlpage_version_info modify version_ts datetime not null");
            
            dc.executeStatement("alter table link_version_info add version_ts datetime");
            dc.executeStatement("update link_version_info set version_ts=now()");
            dc.executeStatement("alter table link_version_info modify version_ts datetime not null");
            
            dc.executeStatement("alter table template_version_info add version_ts datetime");
            dc.executeStatement("update template_version_info set version_ts=now()");
            dc.executeStatement("alter table template_version_info modify version_ts datetime not null");
        }
        else if(DbConnectionFactory.isPostgres()) {
            // PostgreSQL
            dc.executeStatement("alter table container_version_info add version_ts timestamp not null default now()");
            dc.executeStatement("alter table contentlet_version_info add version_ts timestamp not null default now()");
            dc.executeStatement("alter table fileasset_version_info add version_ts timestamp not null default now()");
            dc.executeStatement("alter table htmlpage_version_info add version_ts timestamp not null default now()");
            dc.executeStatement("alter table link_version_info add version_ts timestamp not null default now()");
            dc.executeStatement("alter table template_version_info add version_ts timestamp not null default now()");
        }
    }


    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
        try {
        	alterProcedure();
        } catch (Exception ex) {
            throw new DotRuntimeException(ex.getMessage(), ex);
        }
    }


}
