package com.dotmarketing.startup.runonce;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class Task00790DataModelChangesForWebAssets implements StartupTask {
	private void deleteMappingsFromTree() throws SQLException{
		DotConnect dc = new DotConnect();
		String delete_host_containers = "Delete from tree where parent in(select distinct host_inode from identifier) and " 
				                      + "child in(select inode from inode where type ='containers')";
		
		String delete_host_template = "Delete from tree where parent in(select distinct host_inode from identifier) and " 
        						    + "child in(select inode from inode where type ='template')";
		
		String delete_host_folders =  "Delete from tree where parent in(select distinct host_inode from identifier) and " 
			                       + "child in(select inode from inode where type ='folder')";
		
		String delete_folder_folder = "Delete from tree where parent in(select inode from inode where type='folder') and " 
									+ "child in(select inode from inode where type='folder')";
		
		String delete_structure_containers = "Delete from tree where child in(select inode from inode where type='containers') and " 
										   + "parent in(select inode from structure)";
		
		String delete_template_htmlpage = "Delete from tree where child in(select inode from inode where type='htmlpage') and " 
										+ "parent in(select identifier from template)";
		
		dc.executeStatement(delete_host_containers);
		dc.executeStatement(delete_host_template);
		dc.executeStatement(delete_host_folders);
		dc.executeStatement(delete_folder_folder);
		dc.executeStatement(delete_structure_containers);
		dc.executeStatement(delete_template_htmlpage);
	}
		
	private void containerTableChanges() throws DotDataException, SQLException {
		DotConnect dc = new DotConnect();
		String addStructure = "ALTER TABLE containers add structure_inode varchar(36)";
		String addFK = "ALTER TABLE containers add constraint structure_fk foreign key (structure_inode) references structure(inode)";
		String containerQuery = "Select * from tree where child in(Select inode from inode where type='containers') and " 
					 + "parent in(select inode from structure)";
		dc.executeStatement(addStructure);
		dc.executeStatement(addFK);
		
		dc.setSQL(containerQuery);
		List<Map<String, String>> treeResults = dc.loadResults();
		for(Map<String,String> tree : treeResults){
			String stInode = tree.get("parent");
			String containerInode = tree.get("child");
			dc.setSQL("UPDATE containers set structure_inode = ? where inode = ?");
			dc.addParam(stInode);
			dc.addParam(containerInode);
			dc.loadResult();
		}
	}
	
	private void htmlpageTableChanges() throws SQLException, DotDataException {
		DotConnect dc = new DotConnect();
		String addtemplate = "ALTER TABLE htmlpage add template_id varchar(36)";
		String addFK = "ALTER TABLE htmlpage add constraint template_id_fk foreign key (template_id) references identifier(id)";
		String htmlQuery = "Select * from tree where child in(Select inode from inode where type='htmlpage') and " 
					 + "parent in(select identifier from template)";
		
		dc.executeStatement(addtemplate);
		dc.executeStatement(addFK);
		
		addTriggerToHTMLPage();
		dc.setSQL(htmlQuery);
		List<Map<String, String>> treeResults = dc.loadResults();
		for(Map<String,String> tree : treeResults){
			String templateId = tree.get("parent");
			String htmlpageInode = tree.get("child");
			dc.setSQL("UPDATE htmlpage set template_id = ? where inode = ?");
			dc.addParam(templateId);
			dc.addParam(htmlpageInode);
			dc.loadResult();
		}
	}
	
	private void triggerChanges() throws SQLException {
		DotConnect dc = new DotConnect();
		String trigger = "";
		if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
		   trigger =   "CREATE OR REPLACE FUNCTION structure_host_folder_check() RETURNS trigger AS '\n" +
								"DECLARE\n" +
								   "folderInode varchar(36);\n" +
								   "hostInode varchar(36);\n" +
						   	    "BEGIN\n" +
								   "IF ((tg_op = ''INSERT'' OR tg_op = ''UPDATE'') AND (NEW.host IS NOT NULL AND NEW.host <> '''' AND NEW.host <> ''SYSTEM_HOST''\n" + 
								         "AND NEW.folder IS NOT NULL AND NEW.folder <> ''SYSTEM_FOLDER'' AND NEW.folder <> '''')) THEN\n" +
								    	     "select host_inode,folder.inode INTO hostInode,folderInode from folder,identifier where folder.identifier = identifier.id and folder.inode=NEW.folder;\n" +
									  	   "IF (FOUND AND NEW.host = hostInode) THEN\n" +
									  	      "RETURN NEW;\n" +
									  	   "ELSE\n" +
									  	      "RAISE EXCEPTION ''Cannot assign host/folder to structure, folder does not belong to given host'';\n" +
									  	      "RETURN NULL;\n" +
									  	   "END IF;\n" +
									     "ELSE\n" +
									  		"IF((tg_op = ''INSERT'' OR tg_op = ''UPDATE'') AND (NEW.host IS NULL OR NEW.host = '''' OR NEW.host= ''SYSTEM_HOST''\n" + 
									  		    "OR NEW.folder IS NULL OR NEW.folder = '''' OR NEW.folder = ''SYSTEM_FOLDER'')) THEN\n" +
					    			  		   "IF(NEW.host = ''SYSTEM_HOST'' OR NEW.host IS NULL OR NEW.host = '''') THEN\n" +
					    			  		   		"NEW.host = ''SYSTEM_HOST'';\n" +
					    			  		   		"NEW.folder = ''SYSTEM_FOLDER'';\n" +
					    			  		   	"END IF;\n" +
					    			  		   "IF(NEW.folder = ''SYSTEM_FOLDER'' OR NEW.folder IS NULL OR NEW.folder = '''') THEN\n" +
					    			  		   		"NEW.folder = ''SYSTEM_FOLDER'';\n" +
					    			  		   "END IF;\n" +
					    			  		 "RETURN NEW;\n" +
					    			  		 "END IF;\n" +
					    			 "END IF;\n" +
					    		   "RETURN NULL;\n" +
					    		"END\n" +
					    	    "' LANGUAGE plpgsql;";
		}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)){
			trigger = "CREATE OR REPLACE TRIGGER structure_host_folder_trigger\n" +
							   "BEFORE INSERT OR UPDATE ON structure\n" +
							   "FOR EACH ROW\n" +
							   "DECLARE\n" +
							   		"folderInode varchar(36);\n" +
							   		"hostInode varchar(36);\n" +
							   	"BEGIN\n" +
							   		"IF (:NEW.host <> 'SYSTEM_HOST' AND :NEW.folder <> 'SYSTEM_FOLDER') THEN\n" +
							   			"select host_inode, folder.inode INTO hostInode, folderInode from folder,identifier where folder.identifier = identifier.id and folder.inode = :NEW.folder;\n" +
							   		"IF (:NEW.host <> hostInode) THEN\n" +
							   			"RAISE_APPLICATION_ERROR(-20000, 'Cannot assign host/folder to structure, folder does not belong to given host');\n" +
							   		"END IF;\n" +
							   		"ELSE\n" +
							   			"IF(:NEW.host IS NULL OR :NEW.host = '' OR :NEW.host = 'SYSTEM_HOST' OR :NEW.folder IS NULL OR :NEW.folder = '' OR :NEW.folder = 'SYSTEM_FOLDER') THEN\n" +
							   				"IF(:NEW.host = 'SYSTEM_HOST' OR :NEW.host IS NULL OR :NEW.host = '') THEN\n" +
							   					":NEW.host := 'SYSTEM_HOST';\n" +
							   					":NEW.folder := 'SYSTEM_FOLDER';\n" +
							   				"END IF;\n" +
							   				"IF(:NEW.folder = 'SYSTEM_FOLDER' OR :NEW.folder IS NULL OR :NEW.folder = '') THEN\n" +
							   					":NEW.folder := 'SYSTEM_FOLDER';\n" +
							   					"END IF;\n" +
							   				"END IF;\n" +
							   			"END IF;\n" +
							   	"END;\n" +
							   	"/";
		}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
			trigger = "ALTER TRIGGER structure_host_folder_trigger\n" +
							   "ON structure\n" +
							   "FOR INSERT, UPDATE AS\n" +
							   "DECLARE @newFolder varchar(100)\n" + 
							   "DECLARE @newHost varchar(100)\n" +
							   "DECLARE @folderInode varchar(36)\n" + 
							   "DECLARE @hostInode varchar(36)\n" +
							   "DECLARE cur_Inserted3 cursor LOCAL FAST_FORWARD for\n" +
							   "Select folder, host\n" +
							   "from inserted\n" + 
							   "for Read Only\n" +
							   "open cur_Inserted3\n" +
							   "fetch next from cur_Inserted3 into @newFolder,@newHost\n" +
							   "while @@FETCH_STATUS <> -1\n" +
							   "BEGIN\n" +
							   	 "IF (@newHost <> 'SYSTEM_HOST' AND @newFolder <> 'SYSTEM_FOLDER')\n" + 
							   	   "BEGIN\n" +
							   		  "SELECT @hostInode = identifier.host_inode, @folderInode = folder.inode from folder,identifier where folder.identifier = identifier.id and folder.inode = @newFolder\n" +
							   		   "IF (@folderInode IS NULL OR @folderInode = '' OR @newHost <> @hostInode)\n" + 
							   			"BEGIN\n" +
							   			    "RAISERROR (N'Cannot assign host/folder to structure, folder does not belong to given host', 10, 1)\n" +
							   				"ROLLBACK WORK\n" +
							   			"END\n" +
							   	   "END\n" +
							   "fetch next from cur_Inserted3 into @newFolder,@newHost\n" +
							   "END;";
		}
		List<String> triggers = SQLUtil.tokenize(trigger);
		for(String t:triggers){
			dc.executeStatement(t);
		}
	}
	
	private void addTriggerToHTMLPage() throws SQLException {
		DotConnect dc = new DotConnect();
		String trigger="";
		if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
			trigger = "CREATE OR REPLACE FUNCTION check_template_id()RETURNS trigger AS '\n" +
			  		  "DECLARE\n" +        
			  		  	  "templateId varchar(36);\n" +   
			  		  "BEGIN\n" +    
			  		  	  "IF (tg_op = ''INSERT'' OR tg_op = ''UPDATE'') THEN\n" +  
			  		  	  	"select id into templateId from identifier where asset_type=''template'' and id = NEW.template_id;\n" +     
			  		  	  	"IF FOUND THEN\n" +          
			  		  	  		"RETURN NEW;\n" +       
			  		  	  	"ELSE\n" +          
			  		  	  		"RAISE EXCEPTION ''Template Id should be the identifier of a template'';\n" +        
			  		  	  		"RETURN NULL;\n" +       
			  		  	  	"END IF;\n" +   
			  		  	  "END IF;\n" + 
			  		  "RETURN NULL;\n" +  
			  		  "END\n" +
			  		  "' LANGUAGE plpgsql;\n" +
			  		  "CREATE TRIGGER check_template_identifier\n" +
			  		  "BEFORE INSERT OR UPDATE\n" +
			  		  "ON htmlpage\n" +
			  		  "FOR EACH ROW\n" +
			  		  "EXECUTE PROCEDURE check_template_id();";
		} else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
             trigger = "CREATE Trigger check_template_identifier\n" +
             		   "ON htmlpage\n" +
             		   "FOR INSERT,UPDATE AS\n" +  
             		   "DECLARE @templateId varchar(36)\n" +
             		   "DECLARE @tempIdentifier varchar(36)\n" +
             		   "DECLARE htmlpage_cur_Inserted cursor LOCAL FAST_FORWARD for\n" +
             		   "Select template_id\n" +
             		   "from inserted\n" + 
             		   "for Read Only\n" +
             		   "open htmlpage_cur_Inserted\n" +   
             		   "fetch next from htmlpage_cur_Inserted into @templateId\n" +
             		   "while @@FETCH_STATUS <> -1\n" + 
             		   "BEGIN\n" +
             		   "select @tempIdentifier = id from identifier where asset_type='template' and id = @templateId\n" +      
             		   "IF (@tempIdentifier IS NULL)\n" +        
             		   "BEGIN\n" +           
             		   "RAISERROR (N'Template Id should be the identifier of a template', 10, 1)\n" +          
             		   "ROLLBACK WORK\n" +        
             		   "END\n"+ 
             		   "fetch next from htmlpage_cur_Inserted into @templateId\n" +
             		   "END;"; 			
		} else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)){
			trigger = "CREATE OR REPLACE TRIGGER  check_template_identifier \n" + 
			          "BEFORE INSERT OR UPDATE ON htmlpage\n" + 
				      "FOR EACH ROW\n" +
				      "DECLARE\n" +        
				    	"rowcount varchar2(100);\n" +   
				      "BEGIN\n" +
				    	 "select count(*) into rowcount from identifier where id= :NEW.template_id and asset_type='template';\n" +     
				    	   "IF (rowcount = 0) THEN\n" +    
				    	   	 "RAISE_APPLICATION_ERROR(-20000, 'Template Id should be the identifier of a template');\n" +      
				    	   "END IF;\n" +   
				      "END;";

		}else{
			String mysqlTrigger = "DROP TRIGGER IF EXISTS check_templateId_when_insert;\n" +
		  	   					   "CREATE TRIGGER check_templateId_when_insert BEFORE INSERT\n" +
		  	   					   "on htmlpage\n" +
		  	   					   "FOR EACH ROW\n" +
		  	   					   "BEGIN\n" +
		  	   					   "DECLARE identCount INT;\n" +
		  	   					   "select count(id) into identCount from identifier where id = NEW.template_id and asset_type='template';\n" +
		  	   					   "IF(identCount = 0) THEN\n" +
		  	   					   "UPDATE htmlpage set template_id = NEW.template_id where id = NEW.inode;\n" + 
		  	   					   "END IF;\n" +
		  	   					   "END\n" +
		  	   					   "#" +
								   "DROP TRIGGER IF EXISTS check_templateId_when_update;\n" +
			   					   "CREATE TRIGGER check_templateId_when_update  BEFORE UPDATE\n" +
			   					   "on htmlpage\n" +
			   					   "FOR EACH ROW\n" +
			   					   "BEGIN\n" +
			   					   "DECLARE identCount INT;\n" +
			   					   "select count(id)into identCount from identifier where id = NEW.template_id and asset_type='template';\n" +
			   					   "IF(identCount = 0) THEN\n" +
			   					   "UPDATE htmlpage set template_id=NEW.template_id where id = NEW.inode;\n" + 
			   					   "END IF;\n" +
			   					   "END\n" +
			   					   "#";
			List<String> triggers = SQLUtil.tokenize(mysqlTrigger);
			for(String t:triggers){
				dc.executeStatement(t);
			}
		}
		if(UtilMethods.isSet(trigger)){
			dc.executeStatement(trigger);
		}
	}
	
	private void addTriggerToFolder() throws SQLException {
		DotConnect dc = new DotConnect();
		String trigger = "";
		if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
			trigger = "CREATE OR REPLACE FUNCTION folder_identifier_check() RETURNS trigger AS '\n" +
			  		  "DECLARE\n" +
			  		  		"versionsCount integer;\n" +
			  		  "BEGIN\n" +
			  		  		"IF (tg_op = ''DELETE'') THEN\n" +
			  		  			"select count(*) into versionsCount from folder where identifier = OLD.identifier;\n"  +
			  		  			"IF (versionsCount = 0)THEN\n" +  
			  		  				"DELETE from identifier where id = OLD.identifier;\n" +
			  		  			"ELSE\n" +
			  		  				"RETURN OLD;\n" +
			  		  			"END IF;\n" +
			  		  		"END IF;\n" +
			  		  	"RETURN NULL;\n" +
			  		  	"END\n" +
			  		  	"' LANGUAGE plpgsql;\n" +
			  		  	"CREATE TRIGGER folder_identifier_check_trigger AFTER DELETE\n" + 
			  		  	"ON folder FOR EACH ROW\n" + 
			  		  	"EXECUTE PROCEDURE folder_identifier_check();\n";
		}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
			trigger = "CREATE Trigger folder_identifier_check\n" +
			  		  "ON folder\n" +
			  		  "FOR DELETE AS\n" +
			  		  "DECLARE @totalCount int\n" +
			  		  "DECLARE @identifier varchar(36)\n" +   
			  		  "DECLARE folder_cur_Deleted cursor LOCAL FAST_FORWARD for\n" +
			  	  		"Select identifier\n" +
			  	  		"from deleted\n" + 
			  	  		"for Read Only\n" +
			  	  		"open folder_cur_Deleted\n" +   
			  	  		"fetch next from folder_cur_Deleted into @identifier\n" +
			  	  		"while @@FETCH_STATUS <> -1\n" + 
			  	  		"BEGIN\n" +
			  	  			"select @totalCount = count(*) from folder where identifier = @identifier\n" +
			  	  			"IF (@totalCount = 0)\n" +      
			  	  			"BEGIN\n" +       
			  	  				"DELETE from identifier where id = @identifier\n" +           
			  	  			"END\n" + 
			  	  			"fetch next from folder_cur_Deleted into @identifier\n" +
			  	  		"END;\n";
		}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)){
			String oracleTrigger = "CREATE OR REPLACE PACKAGE folder_pkg as\n" +
			     				   "type array is table of folder%rowtype index by binary_integer;\n" +
			     				   "oldvals array;\n" +
			     				   "empty array;\n" +
			     				   "END;\n" +
			     				   "/\n" +
			     				   "CREATE OR REPLACE trigger folder_identifier_bd\n" +
			     				   "BEFORE DELETE ON folder\n" +
			     				   "BEGIN\n" +
			     				   	"folder_pkg.oldvals := folder_pkg.empty;\n" +
			     				   	"END;\n" +
			     				   	"/\n" +
			     				   	"CREATE OR REPLACE TRIGGER folder_identifier_bdfer\n" +
			     				   	"BEFORE DELETE ON folder\n" +
			     				   	"FOR EACH ROW\n" +
			     				   	"BEGIN\n" +
			     				   		"folder_pkg.oldvals(folder_pkg.oldvals.count+1).identifier := :old.identifier;\n" +  
			     				   	"END;\n" +
			     				   	"/\n" +
			     				   	"CREATE OR REPLACE TRIGGER  folder_identifier_trigger\n" + 
			     				   	"AFTER DELETE ON folder\n" +
			     				   	"DECLARE\n" +
			     				   		"versionsCount integer;\n" +
			     				   	"BEGIN\n" +
			     				   		"for i in 1 .. folder_pkg.oldvals.count LOOP\n" + 
			     				   			"select count(*) into versionsCount from folder where identifier = folder_pkg.oldvals(i).identifier;\n" +
			     				   			"IF (versionsCount = 0)THEN\n" +  
			     				   				"DELETE from identifier where id = folder_pkg.oldvals(i).identifier;\n" +      
			     				   			"END IF;\n" +
			     				   		"END LOOP;\n" + 
			     				   		"END;\n" +
			     				   "/\n";
			List<String> triggers = SQLUtil.tokenize(oracleTrigger);
			for(String t:triggers){
				dc.executeStatement(t);
			}

		}else{	
			String checkVersions = "DROP PROCEDURE IF EXISTS checkVersions;\n" +
			   					   "CREATE PROCEDURE checkVersions(IN ident varchar(36),IN tableName VARCHAR(20),OUT versionsCount INT)\n" +
			   					   "BEGIN\n" +
			   					   "SET versionsCount := 0;\n" +
			   					   	  "IF(tableName = 'htmlpage') THEN\n" +
			   					   		"select count(inode) into versionsCount from htmlpage where identifier = ident;\n" +
			   					   	  "END IF;\n" +
			   					   	  "IF(tableName = 'file_asset') THEN\n" +
			   					   		"select count(inode) into versionsCount from file_asset where identifier = ident;\n" + 
			   					   	  "END IF;\n" +
			   					   	  "IF(tableName = 'links') THEN\n" +
			   					   	  	"select count(inode) into versionsCount from links where identifier = ident;\n" +
			   					   	  "END IF;\n" +
			   					   	  "IF(tableName = 'containers') THEN\n" +
			   					   	  	"select count(inode) into versionsCount from containers where identifier = ident;\n" +
			   					   	  "END IF;\n" +
			   					   	  "IF(tableName = 'template') THEN\n" +
			   					   	  	"select count(inode) into versionsCount from template where identifier = ident;\n" +
			   					   	  "END IF;\n" +
			   					   	  "IF(tableName = 'contentlet') THEN\n" +
			   					   	  	"select count(inode) into versionsCount from contentlet where identifier = ident;\n" +
			   					   	  "END IF;\n" +
			   					   	  "IF(tableName = 'folder') THEN\n" +
			   					   	  	"select count(inode) into versionsCount from folder where identifier = ident;\n" +
			   					   	  "END IF;\n" +
			   					   "END\n" +
			   					   "#" ;
			String folderTrigger = "DROP TRIGGER IF EXISTS folder_identifier_check;\n" +
								   "CREATE TRIGGER folder_identifier_check BEFORE DELETE\n" +
								   "on folder\n" +
								   "FOR EACH ROW\n" +
								   "BEGIN\n" +
								   "DECLARE tableName VARCHAR(20);\n" +
								   "DECLARE count INT;\n" +
								   "SET tableName = 'folder';\n" +
								   "CALL checkVersions(OLD.identifier,tableName,count);\n" +
								   "IF(count = 0)THEN\n" + 
								   "delete from identifier where id = OLD.identifier;\n" +
								   "END IF;\n" +
								   "END\n" +
								   "#";
			
			List<String> triggers = SQLUtil.tokenize(checkVersions + folderTrigger);
			for(String t:triggers){
				dc.executeStatement(t);
			}
		}
		if(UtilMethods.isSet(trigger)){
			dc.executeStatement(trigger);
		}
	}

	public void executeUpgrade() throws DotHibernateException {
		DotConnect dc = new DotConnect();
		HibernateUtil.startTransaction();
		try {
			if (DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL))
				  dc.executeStatement("SET TRANSACTION ISOLATION LEVEL READ COMMITTED");
			containerTableChanges();
			htmlpageTableChanges();
			triggerChanges();
			addTriggerToFolder();
			deleteMappingsFromTree();
		} catch (Exception e) {
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
