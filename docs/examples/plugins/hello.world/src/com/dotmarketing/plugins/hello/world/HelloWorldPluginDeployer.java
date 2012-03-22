package com.dotmarketing.plugins.hello.world;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.common.db.DotConnect; // DOTCMS - 3800
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.plugin.PluginDeployer;
import com.dotmarketing.util.Logger;

public class HelloWorldPluginDeployer implements PluginDeployer {

	public boolean deploy() {
		final String PGCREATESQL = "CREATE TABLE hello_world_bean (id bigserial NOT NULL, JOB_NAME character varying(255) NOT NULL, CONSTRAINT hello_world_bean_pkey PRIMARY KEY (id));";
		final String MYCREATESQL = "CREATE TABLE `hello_world_bean` (`id` BIGINT  NOT NULL AUTO_INCREMENT,`name` VARCHAR(255) , PRIMARY KEY (`id`));";
		final String MSCREATESQL = "CREATE TABLE hello_world_bean ( id bigint NOT NULL IDENTITY (1, 1), name varchar(255) NOT NULL, primary key (id));";
		final String OCLCREATESQL = "CREATE TABLE hello_world_bean ( ID INTEGER NOT NULL , name VARCHAR2(255), PRIMARY KEY (ID) VALIDATE );";
		final String ORACLESEQSQL = "CREATE SEQUENCE hello_world_bean_id_seq START WITH 1 INCREMENT BY 1;";
		final String ORACLETRIGGER = "create trigger hello_world_bean_trg\n " +
				"before insert on hello_world_bean \n"+
				"for each row \n"+
				"when (new.id is null) \n"+
				"begin \n"+
				"select hello_world_bean_id_seq.nextval into :new.id from dual; \n"+
				"end;\n"+
				"/\n;";
		try{
			DotConnect dc = new DotConnect();
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
				dc.setSQL(PGCREATESQL);
				try {
					dc.loadResult();
				} catch (DotDataException e) {
					Logger.error(this, e.getMessage(), e);
				}
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				dc.setSQL(MYCREATESQL);
				try {
					dc.loadResult();
				} catch (DotDataException e) {
					Logger.error(this, e.getMessage(), e);
				}
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				dc.setSQL(MSCREATESQL);
				try {
					dc.loadResult();
				} catch (DotDataException e) {
					Logger.error(this, e.getMessage(), e);
				}
			}else{
				dc.setSQL(OCLCREATESQL);
				try {
					dc.loadResult();
					dc.setSQL(ORACLESEQSQL);
					dc.loadResult();
					dc.setSQL(ORACLETRIGGER);
					dc.loadResult();
				} catch (DotDataException e) {
					Logger.error(this, e.getMessage(), e);
				}			
			}
		}finally{
			DbConnectionFactory.closeConnection();
		}
		
		return true;
	}

	public boolean redeploy(String version) {
		// TODO Auto-generated method stub
		return true;
	}

}
