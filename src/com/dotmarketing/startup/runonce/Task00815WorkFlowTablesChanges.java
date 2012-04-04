package com.dotmarketing.startup.runonce;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;

public class Task00815WorkFlowTablesChanges implements StartupTask{
	
	private void workflowTaskChanges() throws SQLException, DotDataException{
		String dropInode = "";
		DotConnect dc = new DotConnect();
		dropWorkFlowTaskIndexes();
		if (DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
			dropInode = "ALTER TABLE workflow_task DROP FOREIGN KEY fk441116055fb51eb;" +
						"ALTER TABLE workflow_task change inode id varchar(36);";
		}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)){
			dropInode = "ALTER TABLE workflow_task DROP CONSTRAINT fk441116055fb51eb;" +
			            "ALTER TABLE workflow_task add id varchar(36);" +
			            "UPDATE workflow_task set id = cast(inode as varchar(36));" +
			            "ALTER TABLE workflow_task drop column inode;" +
			            "ALTER TABLE workflow_task MODIFY (id NOT NULL);" +
			            "ALTER TABLE workflow_task ADD CONSTRAINT workflow_task_pkey PRIMARY KEY(id);"; 
		}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
			dropInode = "ALTER TABLE workflow_task DROP CONSTRAINT fk441116055fb51eb;" +
			            "ALTER TABLE workflow_task DROP CONSTRAINT pk_workflow_task;" +
				        "ALTER TABLE workflow_task add new_inode varchar(36);" +
			            "UPDATE workflow_task set new_inode = cast(inode as varchar(36));" +
			            "ALTER TABLE workflow_task drop column inode;" +
			            "EXEC SP_RENAME 'dbo.workflow_task.new_inode','id','COLUMN';" + 
			            "ALTER TABLE workflow_task ALTER column id varchar(36) not null;" + 
			            "ALTER TABLE workflow_task ADD CONSTRAINT workflow_task_pkey PRIMARY KEY(id);";
		}else{
			dropInode = "ALTER TABLE workflow_task DROP CONSTRAINT fk441116055fb51eb;" + 
				        "ALTER TABLE workflow_task add id varchar(36);" +
			            "UPDATE workflow_task set id = cast(inode as varchar(36));" +
			            "ALTER TABLE workflow_task drop column inode;" +
			            "ALTER TABLE workflow_task ALTER COLUMN id SET NOT NULL;" +
			            "ALTER TABLE workflow_task ADD CONSTRAINT workflow_task_pkey PRIMARY KEY(id);";
		}
		String createTable= "Create table workflowtask_files" +
		 					"(id varchar(36) NOT NULL  primary key," +
		 					"workflowtask_id varchar(36) NOT NULL," +
		 					"file_inode varchar(36) NOT NULL);";
        
		String addFKs = "alter table workflowtask_files add constraint FK_workflow_id foreign key (workflowtask_id) references workflow_task(id);"
			          + "alter table workflowtask_files add constraint FK_task_file_inode foreign key (file_inode) references file_asset(inode);";
		
		String workflowtask_fileasset_relations = "Select child,parent from tree where parent in(select id from workflow_task) and child in(select inode from file_asset)";
		
		String delete_task_file_relations = "Delete from tree where parent in(select id from workflow_task) and child in(select inode from file_asset)";
		
		List<String> dropInodeQueries = SQLUtil.tokenize(dropInode + createTable+addFKs);
		for(String dropInodeQuery :dropInodeQueries){
			dc.executeStatement(dropInodeQuery);
		}
		addWorkFlowTaskIndexes();
		dc.setSQL(workflowtask_fileasset_relations);
		List<Map<String, String>> relations = dc.loadResults();
    	for(Map<String,String> relation : relations){
    		String fileInode = relation.get("child");
    		String workflowTaskId = relation.get("parent");	
    		String uuid = UUIDGenerator.generateUuid();
			dc.setSQL("insert into workflowtask_files(id,workflowtask_id,file_inode) values(?,?,?)");
			dc.addParam(uuid);
			dc.addParam(workflowTaskId);
			dc.addParam(fileInode);
			dc.loadResult();
    	}
		dc.executeStatement(delete_task_file_relations);
	}
    private void workflowCommentChanges() throws SQLException, DotDataException{
    	DotConnect dc = new DotConnect();
    	String dropInode = "";
    	if (DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
			dropInode = "ALTER TABLE workflow_comment DROP FOREIGN KEY fk94993ddf5fb51eb;" +
						"ALTER TABLE workflow_comment change inode id varchar(36);";
		}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)){
			dropInode = "ALTER TABLE workflow_comment DROP CONSTRAINT fk94993ddf5fb51eb;" +
			            "ALTER TABLE workflow_comment add id varchar(36);" +
			            "UPDATE workflow_comment set id = cast(inode as varchar(36));" +
			            "ALTER TABLE workflow_comment drop column inode;" +
			            "ALTER TABLE workflow_comment MODIFY (id NOT NULL);" +
			            "ALTER TABLE workflow_comment ADD CONSTRAINT workflow_comment_pkey PRIMARY KEY(id);"; 
		}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
			dropInode = "ALTER TABLE workflow_comment DROP CONSTRAINT fk94993ddf5fb51eb;" +
			            "ALTER TABLE workflow_comment DROP CONSTRAINT pk_workflow_comment;" +
				        "ALTER TABLE workflow_comment add new_inode varchar(36);" +
			            "UPDATE workflow_comment set new_inode = cast(inode as varchar(36));" +
			            "ALTER TABLE workflow_comment drop column inode;" +
			            "EXEC SP_RENAME 'dbo.workflow_comment.new_inode','id','COLUMN';" + 
			            "ALTER TABLE workflow_comment ALTER column id varchar(36) not null;" + 
			            "ALTER TABLE workflow_comment ADD CONSTRAINT workflow_comment_pkey PRIMARY KEY(id);";
		}else{
			dropInode = "ALTER TABLE workflow_comment DROP CONSTRAINT fk94993ddf5fb51eb;" + 
				        "ALTER TABLE workflow_comment add id varchar(36);" +
			            "UPDATE workflow_comment set id = cast(inode as varchar(36));" +
			            "ALTER TABLE workflow_comment drop column inode;" +
			            "ALTER TABLE workflow_comment ALTER COLUMN id SET NOT NULL;" +
			            "ALTER TABLE workflow_comment ADD CONSTRAINT workflow_comment_pkey PRIMARY KEY(id);";
		}
		String addWorkFlowCommentFK = "alter table workflow_comment add workflowtask_id varchar(36);" + 
									  "alter table workflow_comment add constraint wf_id_comment_FK foreign key (workflowtask_id) references workflow_task(id);";
		
		String workflowtask_workflowcomment_relations = "Select child,parent from tree where parent in(select id from workflow_task) and child in(select id from workflow_comment)";
		
		String deleteFromTree = "Delete from tree where parent in(select id from workflow_task) and child in(select id from workflow_comment)";
		
		List<String> queries = SQLUtil.tokenize(dropInode+addWorkFlowCommentFK);
		for(String query :queries){
			dc.executeStatement(query);
		}
		dc.setSQL(workflowtask_workflowcomment_relations);
		List<Map<String, String>> relations = dc.loadResults();
    	for(Map<String,String> relation : relations){
    		String workflowCommentId = relation.get("child");
    		String workflowTaskId = relation.get("parent");	
    		dc.setSQL("UPDATE workflow_comment set workflowtask_id = ? where id = ?");
			dc.addParam(workflowTaskId);
			dc.addParam(workflowCommentId);
			dc.loadResult();
    	}
    	dc.executeStatement(deleteFromTree);
	}
    private void workflowHistoryChanges() throws SQLException, DotDataException{
    	String dropInode = "";
    	DotConnect dc = new DotConnect();
    	if (DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
			dropInode = "ALTER TABLE workflow_history DROP FOREIGN KEY fk933334145fb51eb;" +
						"ALTER TABLE workflow_history change inode id varchar(36);";
		}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)){
			dropInode = "ALTER TABLE workflow_history DROP CONSTRAINT fk933334145fb51eb;" +
			            "ALTER TABLE workflow_history add id varchar(36);" +
			            "UPDATE workflow_history set id = cast(inode as varchar(36));" +
			            "ALTER TABLE workflow_history drop column inode;" +
			            "ALTER TABLE workflow_history MODIFY (id NOT NULL);" +
			            "ALTER TABLE workflow_history ADD CONSTRAINT workflow_history_pkey PRIMARY KEY(id);"; 
		}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
			dropInode = "ALTER TABLE workflow_history DROP CONSTRAINT fk933334145fb51eb;" +
			            "ALTER TABLE workflow_history DROP CONSTRAINT pk_workflow_history;" + 
				        "ALTER TABLE workflow_history add new_inode varchar(36);" +
			            "UPDATE workflow_history set new_inode = cast(inode as varchar(36));" +
			            "ALTER TABLE workflow_history drop column inode;" +
			            "EXEC SP_RENAME 'dbo.workflow_history.new_inode','id','COLUMN';" + 
			            "ALTER TABLE workflow_history ALTER column id varchar(36) not null;" + 
			            "ALTER TABLE workflow_history ADD CONSTRAINT workflow_history_pkey PRIMARY KEY(id);";
		}else{
			dropInode = "ALTER TABLE workflow_history DROP CONSTRAINT fk933334145fb51eb;" + 
				        "ALTER TABLE workflow_history add id varchar(36);" +
			            "UPDATE workflow_history set id = cast(inode as varchar(36));" +
			            "ALTER TABLE workflow_history drop column inode;" +
			            "ALTER TABLE workflow_history ALTER COLUMN id SET NOT NULL;" +
			            "ALTER TABLE workflow_history ADD CONSTRAINT workflow_history_pkey PRIMARY KEY(id);";
		}
		String addWorkFlowHistoryFK = "alter table workflow_history add workflowtask_id varchar(36);" + 
        							  "alter table workflow_history add constraint wf_id_history_FK foreign key (workflowtask_id) references workflow_task(id)";
		
		String workflowtask_workflowhistory_relations = "Select child,parent from tree where parent in(select id from workflow_task) and child in(select id from workflow_history)";
		
		String deleteFromTree = "Delete from tree where parent in(select id from workflow_task) and child in(select id from workflow_history)";
		
		List<String> queries = SQLUtil.tokenize(dropInode+addWorkFlowHistoryFK);
		for(String query :queries){
			dc.executeStatement(query);
		}
		
		dc.setSQL(workflowtask_workflowhistory_relations);
		List<Map<String, String>> relations = dc.loadResults();
    	for(Map<String,String> relation : relations){
    		String workflowHistoryInode = relation.get("child");
    		String workflowTaskInode = relation.get("parent");	
    		dc.setSQL("UPDATE workflow_history set workflowtask_id = ? where id = ?");
			dc.addParam(workflowTaskInode);
			dc.addParam(workflowHistoryInode);
			dc.loadResult();
    	}
    	
    	dc.executeStatement(deleteFromTree);
    }
    private void dropWorkFlowTaskIndexes() throws SQLException{
    	String indexes = "";
    	DotConnect dc = new DotConnect();
    	if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)||
    			      DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
    		indexes = "drop index idx_workflow_1;" +
    		          "drop index idx_workflow_2;" +
    		          "drop index idx_workflow_3;" +
    		          "drop index idx_workflow_4;" +
    		          "drop index idx_workflow_5;";
    	}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
    		indexes = "drop index workflow_task.idx_workflow_1;" +
    		          "drop index workflow_task.idx_workflow_2;" +
    		          "drop index workflow_task.idx_workflow_3;" +
    		          "drop index workflow_task.idx_workflow_4;" +
    		          "drop index workflow_task.idx_workflow_5;";
    	}
    	List<String> indexList = SQLUtil.tokenize(indexes);
    	for(String index:indexList){
    		dc.executeStatement(index);
    	}
    }
    private void addWorkFlowTaskIndexes() throws SQLException{
    	DotConnect dc = new DotConnect();
    	if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)||
    			      DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL) ||
    			      DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
    		dc.executeStatement("create index idx_workflow_4 on workflow_task (webasset)");
    		dc.executeStatement("create index idx_workflow_5 on workflow_task (created_by)");
    		dc.executeStatement("create index idx_workflow_2 on workflow_task (belongs_to)");
    		dc.executeStatement("create index idx_workflow_3 on workflow_task (status)");
    		dc.executeStatement("create index idx_workflow_1 on workflow_task (assigned_to)");
    	}
    }

	public void executeUpgrade() throws DotDataException, DotRuntimeException {
		Connection conn = null;
		DotConnect dc = new DotConnect();
		HibernateUtil.startTransaction();
		  try {
			conn = DbConnectionFactory.getDataSource().getConnection();
			  if (DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL))
				 dc.executeStatement("SET storage_engine=INNODB", conn);
			  if (DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL))
			     dc.executeStatement("SET TRANSACTION ISOLATION LEVEL READ COMMITTED");
			  workflowTaskChanges();
			  workflowCommentChanges();
			  workflowHistoryChanges();
		} catch (SQLException e) {
			HibernateUtil.rollbackTransaction();
			Logger.error(this, e.getMessage());
			e.printStackTrace();
		}
		HibernateUtil.commitTransaction();
	}

	public boolean forceRun() {
		return true;
	}

}
