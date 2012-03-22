package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

public class Task00805AddRenameFolderProcedure extends AbstractJDBCStartupTask {

	@Override
	public String getMSSQLScript() {
		return  "CREATE PROCEDURE renameFolderChildren @oldPath varchar(100),@newPath varchar(100),@hostInode varchar(100) AS\n" +
			    "DECLARE @newFolderPath varchar(100)\n" +
			    "DECLARE @oldFolderPath varchar(100)\n" +
			    "DECLARE @assetName varchar(100)\n" +
			       "UPDATE identifier SET  parent_path  = @newPath where parent_path = @oldPath and host_inode = @hostInode\n" +
				"DECLARE folder_data_cursor CURSOR LOCAL FAST_FORWARD for\n" + 
				 "select asset_name from identifier where asset_type='folder' and parent_path = @newPath and host_inode = @hostInode\n" +
				"OPEN folder_data_cursor\n" +
				"FETCH NEXT FROM folder_data_cursor INTO @assetName\n" +
				"while @@FETCH_STATUS <> -1\n" +
				"BEGIN\n" +
				  "SET @newFolderPath = @newPath + @assetName + '/'\n" +
				  "SET @oldFolderPath = @oldPath + @assetName + '/'\n" +
				  "EXEC renameFolderChildren @oldFolderPath,@newFolderPath,@hostInode\n" +
				"fetch next from folder_data_cursor into @assetName\n" +
				"END;\n" +
				"CREATE Trigger rename_folder_assets_trigger\n" +
				"on Folder\n" +
				"FOR UPDATE AS\n" +
				"DECLARE @oldPath varchar(100)\n" +
				"DECLARE @newPath varchar(100)\n" + 
				"DECLARE @newName varchar(100)\n" +
				"DECLARE @hostInode varchar(100)\n" +
				"DECLARE @ident varchar(100)\n" +
				"DECLARE folder_cur_Updated cursor LOCAL FAST_FORWARD for\n" +
				 "Select identifier,name\n" +
				 "from inserted\n" +
				 "for Read Only\n" +
				 "open folder_cur_Updated\n" +
				 "fetch next from folder_cur_Updated into @ident,@newName\n" +
				 "while @@FETCH_STATUS <> -1\n" +
				 "BEGIN\n" +
				     "SELECT @oldPath = parent_path+asset_name+'/',@newPath = parent_path +@newName+'/',@hostInode = host_inode from identifier where id = @ident\n" +
				     "UPDATE identifier SET asset_name = @newName where id = @ident\n" +
				     "EXEC renameFolderChildren @oldPath,@newPath,@hostInode\n" +
				  "fetch next from folder_cur_Updated into @ident,@newName\n" +
				"END;\n";
	}

	@Override
	public String getMySQLScript() {
		return  "DROP PROCEDURE IF EXISTS renameFolderChildren;\n" +
			    "CREATE PROCEDURE renameFolderChildren(IN old_path varchar(100),IN new_path varchar(100),IN hostInode varchar(100))\n" +
				"BEGIN\n" +
		  			"DECLARE new_folder_path varchar(100);\n"+
		  			"DECLARE old_folder_path varchar(100);\n" +
		  			"DECLARE assetName varchar(100);\n" +
		  			"DECLARE no_more_rows boolean;\n" +
		  			"DECLARE cur1 CURSOR FOR select asset_name from identifier where asset_type='folder' and parent_path = new_path and host_inode = hostInode;\n" +
		  			"DECLARE CONTINUE HANDLER FOR NOT FOUND\n" +
		  			"SET no_more_rows := TRUE;\n" + 
		  			"SET max_sp_recursion_depth=255;\n" +
		  			"SET @disable_trigger = 1;\n" +
		  			"UPDATE identifier SET  parent_path  = new_path where parent_path = old_path and host_inode = hostInode;\n" +
		  			"SET @disable_trigger = NULL;\n" +
		  			"OPEN cur1;\n" +
		  			"cur1_loop:LOOP\n" +
		  			"FETCH cur1 INTO assetName;\n" +
		    		"IF no_more_rows THEN\n" +                
		       		  "LEAVE cur1_loop;\n" +                    
		     		"END IF;\n" +
		     		"select CONCAT(new_path,assetName,'/')INTO new_folder_path;\n" +
		     		"select CONCAT(old_path,assetName,'/')INTO old_folder_path;\n" +
		     		"CALL renameFolderChildren(old_folder_path,new_folder_path,hostInode);\n" +
		  			"END LOOP;\n" +
		  			"CLOSE cur1;\n" +
				"END\n" +
				"#\n" +
				"DROP TRIGGER IF EXISTS rename_folder_assets_trigger;\n" +
		        "CREATE TRIGGER rename_folder_assets_trigger AFTER UPDATE\n" +
				"on Folder\n" +
				"FOR EACH ROW\n" +
				"BEGIN\n" +
				  "DECLARE old_parent_path varchar(100);\n" +
				  "DECLARE old_path varchar(100);\n" +
				  "DECLARE new_path varchar(100);\n" +    
				  "DECLARE old_name varchar(100);\n" +
				  "DECLARE hostInode varchar(100);\n" +
				  "IF @disable_trigger <> 1 THEN" +
				  "  select asset_name,parent_path,host_inode INTO old_name,old_parent_path,hostInode from identifier where id = NEW.identifier;\n" +
				  "  SELECT CONCAT(old_parent_path,old_name,'/')INTO old_path;\n" +
				  "  SELECT CONCAT(old_parent_path,NEW.name,'/')INTO new_path;\n" +
				  "  SET @disable_trigger = 1;\n" +
				  "  UPDATE identifier SET asset_name = NEW.name where id = NEW.identifier;\n" +
				  "  SET @disable_trigger = NULL;\n" +
				  "  CALL renameFolderChildren(old_path,new_path,hostInode);\n" +
				  "END IF;" +
				"END\n" +
				"#\n";		
	}

	@Override
	public String getOracleScript() {
		return "CREATE OR REPLACE PROCEDURE renameFolderChildren(oldPath IN varchar2,newPath IN varchar2,hostInode IN varchar2) IS\n" +
		       "newFolderPath varchar2(100);\n" +
		       "oldFolderPath varchar2(100);\n" +
		       "assetName varchar2(100);\n" +
		       "BEGIN\n" + 
		          "UPDATE identifier SET  parent_path  = newPath where parent_path = oldPath and host_inode = hostInode;\n" +
		          "DECLARE CURSOR folder_data_cursor IS\n" +
		             "select * from identifier where asset_type='folder' and parent_path = newPath and host_inode = hostInode;\n" +
		          "BEGIN\n" +
		             "FOR i in folder_data_cursor\n" +
		             "LOOP\n" +
		               "EXIT WHEN folder_data_cursor%NOTFOUND;\n" +
		               "newFolderPath := newPath || i.asset_name || '/';\n" +
		               "oldFolderPath := oldPath || i.asset_name || '/';\n" +
		               "renameFolderChildren(oldFolderPath,newFolderPath,hostInode);\n" +
		             "END LOOP;\n" +
		          "END;\n" +
		       "END;\n" +
		       "/\n" +
		       "CREATE OR REPLACE TRIGGER rename_folder_assets_trigger\n" + 
		       "AFTER UPDATE ON Folder\n" +
		       "FOR EACH ROW\n" +
		       "DECLARE\n" +
		       	  "oldPath varchar2(100);\n" +
		          "newPath varchar2(100);\n" + 
		       	  "hostInode varchar2(100);\n" +
		       "BEGIN\n" +
		       		"SELECT parent_path||asset_name||'/',parent_path ||:NEW.name||'/',host_inode INTO oldPath,newPath,hostInode from identifier where id = :NEW.identifier;\n" +
		       	    "UPDATE identifier SET asset_name = :NEW.name where id = :NEW.identifier;\n" + 
		            "renameFolderChildren(oldPath,newPath,hostInode);\n" +
		       "END;\n" +
		       "/";
	}

	@Override
	public String getPostgresScript() {
		return "CREATE OR REPLACE FUNCTION renameFolderChildren(old_path varchar(100),new_path varchar(100),hostInode varchar(100))\n" +
			   "RETURNS void AS '\n" +
			   "DECLARE\n" +
			     "fi identifier;\n" +
			     "new_folder_path varchar(100);\n" +
			     "old_folder_path varchar(100);\n" + 
		       "BEGIN\n" +
				  "UPDATE identifier SET  parent_path  = new_path where parent_path = old_path and host_inode = hostInode;\n" +    
			      "FOR fi IN select * from identifier where asset_type=''folder'' and parent_path = new_path and host_inode = hostInode LOOP\n" +
					"new_folder_path := new_path ||fi.asset_name||''/'';\n" +
					"old_folder_path := old_path ||fi.asset_name||''/'';\n" +
					"PERFORM renameFolderChildren(old_folder_path,new_folder_path,hostInode);\n" +
				  "END LOOP;\n" +
			   "END\n" +
			   "'LANGUAGE plpgsql;" +
			   "CREATE OR REPLACE FUNCTION rename_folder_and_assets()\n" +    
		       "RETURNS trigger AS '\n" +   
		       "DECLARE\n" +    
		          "old_parent_path varchar(100);\n" +    
		          "old_path varchar(100);\n" +    
		          "new_path varchar(100);\n" +    
		          "old_name varchar(100);\n" +
		          "hostInode varchar(100);\n" +  
		       "BEGIN\n" +
		          "IF (tg_op = ''UPDATE'') THEN\n" +   
		            "select asset_name,parent_path,host_inode INTO old_name,old_parent_path,hostInode from identifier where id = NEW.identifier;\n" +   
		              "old_path := old_parent_path || old_name || ''/'';\n" +   
		              "new_path := old_parent_path || NEW.name || ''/'';\n"+     
		              "UPDATE identifier SET asset_name = NEW.name where id = NEW.identifier;\n"+
		              "PERFORM renameFolderChildren(old_path,new_path,hostInode);\n"+
		            "RETURN NEW;\n"+  
		          "END IF;\n"+
		      "RETURN NULL;\n"+
		    "END\n"+
		    "'LANGUAGE plpgsql;\n" +
		    "CREATE TRIGGER rename_folder_assets_trigger AFTER UPDATE\n" +
		    "ON Folder FOR EACH ROW\n" +     
		    "EXECUTE PROCEDURE rename_folder_and_assets();";

	}

	@Override
	protected List<String> getTablesToDropConstraints() {
		return null;
	}

	public boolean forceRun() {
		return true;
	}

}
