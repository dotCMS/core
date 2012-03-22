package com.dotmarketing.startup.runonce;

import java.util.List;
import java.util.Map;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;

public class Task00800CreateTemplateContainers implements StartupTask{

	public void executeUpgrade() throws DotDataException, DotRuntimeException {
		
		DotConnect dc = new DotConnect();
		
		String createTable = "Create table template_containers" +
		 					 "(id varchar(36) NOT NULL  primary key," +
		 					 "template_id varchar(36) NOT NULL," +
		 					 "container_id varchar(36) NOT NULL)";
		if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE))
		    createTable=createTable.replaceAll("varchar\\(", "varchar2\\(");
		
		String createIndex = "create index idx_template_id on template_containers(template_id)";
		
		String addTemplateFK = "alter table template_containers add constraint FK_template_id foreign key (template_id) references identifier(id)";
		String addContainerFK = "alter table template_containers add constraint FK_container_id foreign key (container_id) references identifier(id)";
		
		String template_container_relations = "Select child,template.identifier from tree,inode,template " +
											  "where parent = template.inode and template.inode = inode.inode and " +
											  "parent in(select inode from inode where type='template') and " +
											  "child in(select id from identifier where asset_type='containers')";
		
		String delete_template_containers = "Delete from tree where child in(select id from identifier where asset_type='containers') " +
											"and parent in(select inode from inode where type='template')";
		

		HibernateUtil.startTransaction();
		try {
			if (DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL))
				  dc.executeStatement("SET TRANSACTION ISOLATION LEVEL READ COMMITTED");
			
			dc.executeStatement(createTable);
			dc.executeStatement(addTemplateFK);
			dc.executeStatement(addContainerFK);
			dc.executeStatement(createIndex);
			
			dc.setSQL(template_container_relations);
			List<Map<String, String>> relations = dc.loadResults();
			
			for(Map<String,String> relation : relations){
			 	String containerId = relation.get("child");
			 	String templateId = relation.get("identifier");
			 	String uuid = UUIDGenerator.generateUuid();
				
				dc.setSQL("insert into template_containers(id,template_id,container_id) values(?,?,?)");
				dc.addParam(uuid);
				dc.addParam(templateId);
				dc.addParam(containerId);
				dc.loadResult();
			}
			dc.executeStatement(delete_template_containers);
		} catch (Exception e) {
		 HibernateUtil.rollbackTransaction();
		 Logger.error(this, e.getMessage(),e);
		}
		
		HibernateUtil.commitTransaction();
	}

	public boolean forceRun() {
		return true;
	}
	

}
