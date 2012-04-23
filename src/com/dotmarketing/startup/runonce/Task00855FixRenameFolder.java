package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;

public class Task00855FixRenameFolder extends AbstractJDBCStartupTask {
    
    @Override
    public boolean forceRun() {
        return true;
    }
    
    @Override
    public String getPostgresScript() {
        return "";
    }
    
    @Override
    public String getMySQLScript() {
        return "DROP TRIGGER IF EXISTS rename_folder_assets_trigger;\n"+
                "CREATE TRIGGER rename_folder_assets_trigger AFTER UPDATE\n"+
                "on Folder\n"+
                "FOR EACH ROW\n"+
                "BEGIN\n"+
                "DECLARE old_parent_path varchar(100);\n"+
                "DECLARE old_path varchar(100);\n"+
                "DECLARE new_path varchar(100);\n"+
                "DECLARE old_name varchar(100);\n"+
                "DECLARE hostInode varchar(100);\n"+
                "IF @disable_trigger IS NULL AND NEW.name<>OLD.name THEN\n"+
                "   select asset_name,parent_path,host_inode INTO old_name,old_parent_path,hostInode from identifier where id = NEW.identifier;\n"+
                "   SELECT CONCAT(old_parent_path,old_name,'/')INTO old_path;\n"+
                "   SELECT CONCAT(old_parent_path,NEW.name,'/')INTO new_path;\n"+
                "   SET @disable_trigger = 1;\n"+
                "   UPDATE identifier SET asset_name = NEW.name where id = NEW.identifier;\n"+
                "   SET @disable_trigger = NULL;\n"+
                "   CALL renameFolderChildren(old_path,new_path,hostInode);\n"+
                "END IF;\n"+
                "END\n"+
                "#\n";
    }
    
    @Override
    public String getOracleScript() {
        return 
                "CREATE OR REPLACE PROCEDURE renameFolderChildren(oldPath IN varchar2,newPath IN varchar2,hostInode IN varchar2) IS\n"+
                "  newFolderPath varchar2(100);\n"+
                "  oldFolderPath varchar2(100);\n"+
                "  assetName varchar2(100);\n"+
                "BEGIN\n"+
                " UPDATE identifier SET  parent_path  = newPath where parent_path = oldPath and host_inode = hostInode;\n"+
                " FOR i in (select * from identifier where asset_type='folder' and parent_path = newPath and host_inode = hostInode)\n"+
                "  LOOP\n"+
                "   newFolderPath := newPath || i.asset_name || '/';\n"+
                "   oldFolderPath := oldPath || i.asset_name || '/';\n"+
                "   renameFolderChildren(oldFolderPath,newFolderPath,hostInode);\n"+
                "  END LOOP;\n"+
                "END;\n"+
                "/\n"+
                "CREATE OR REPLACE TRIGGER rename_folder_assets_trigger\n"+
                "AFTER UPDATE ON Folder\n"+
                "FOR EACH ROW\n"+
                "DECLARE\n"+
                " oldPath varchar2(100);\n"+
                " newPath varchar2(100);\n"+
                " hostInode varchar2(100);\n"+
                "BEGIN\n"+
                "   IF :NEW.name <> :OLD.name THEN\n"+
                "      SELECT parent_path||asset_name||'/',parent_path ||:NEW.name||'/',host_inode INTO oldPath,newPath,hostInode from identifier where id = :NEW.identifier;\n"+
                "      UPDATE identifier SET asset_name = :NEW.name where id = :NEW.identifier;\n"+
                "      renameFolderChildren(oldPath,newPath,hostInode);\n"+
                "    END IF;\n"+
                "END;\n"+
                "/\n";
    }
    
    @Override
    public String getMSSQLScript() {
        return "";
    }
    
    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }
}
