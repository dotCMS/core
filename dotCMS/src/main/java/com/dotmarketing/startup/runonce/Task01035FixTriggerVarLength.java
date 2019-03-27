package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;

public class Task01035FixTriggerVarLength extends AbstractJDBCStartupTask {
    
    @Override
    public boolean forceRun() {
        return DbConnectionFactory.isMsSql();
    }
    
    @Override
    public String getPostgresScript() {
        return "";
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
        String parentPathCheckTrigger = "ALTER Trigger check_identifier_parent_path\n" +
                "ON identifier\n" +
                "FOR INSERT,UPDATE AS\n" +
                "DECLARE @folderId varchar(36)\n" +
                "DECLARE @id varchar(36)\n" +
                "DECLARE @assetType varchar(100)\n" +
                "DECLARE @parentPath varchar(255)\n" +
                "DECLARE @hostInode varchar(36)\n" +
                "DECLARE cur_Inserted2 cursor LOCAL FAST_FORWARD for\n" +
                "Select id,asset_type,parent_path,host_inode\n" +
                "from inserted\n" +
                "for Read Only\n" +
                "open cur_Inserted2\n" +
                "fetch next from cur_Inserted2 into @id,@assetType,@parentPath,@hostInode\n" +
                "while @@FETCH_STATUS <> -1\n" +
                "BEGIN\n" +
                    "IF(@parentPath <>'/' AND @parentPath <>'/System folder')\n" +
                    "BEGIN\n" +
                        "select @folderId = id from identifier where asset_type='folder' and host_inode = @hostInode and parent_path+asset_name+'/' = @parentPath and id <> @id\n" +
                        "IF (@folderId IS NULL)\n" +
                        "BEGIN\n" +
                            "RAISERROR (N'Cannot insert/update for this path does not exist for the given host', 10, 1)\n" +
                            "ROLLBACK WORK\n" +
                        "END\n" +
                    "END\n" +
                "fetch next from cur_Inserted2 into @id,@assetType,@parentPath,@hostInode\n"+
                "END;\n";

        String checkChildAssetsTrigger = "ALTER Trigger check_child_assets\n" +
                 "on identifier\n" +
                 "FOR DELETE AS\n" +
                 "DECLARE @pathCount int\n" +
                 "DECLARE @identifier varchar(36)\n" +
                 "DECLARE @assetType varchar(100)\n" +
                 "DECLARE @assetName varchar(255)\n" +
                 "DECLARE @parentPath varchar(255)\n" +
                 "DECLARE @hostInode varchar(36)\n" +
                 "DECLARE cur_Deleted cursor LOCAL FAST_FORWARD for\n" +
                  "Select id,asset_type,parent_path,asset_name,host_inode\n" +
                   "from deleted\n" +
                   "for Read Only\n" +
                  "open cur_Deleted\n" +
                  "fetch next from cur_Deleted into @identifier,@assetType,@parentPath,@assetName,@hostInode\n" +
                  "while @@FETCH_STATUS <> -1\n" +
                   "BEGIN\n" +
                      "IF(@assetType='folder')\n" +
                      "BEGIN\n" +
                        "select @pathCount = count(*) from identifier where parent_path = @parentPath+@assetName+'/' and host_inode = @hostInode\n" +
                      "END\n" +
                      "IF(@assetType='contentlet')\n" +
                      "BEGIN\n" +
                        "select @pathCount = count(*) from identifier where host_inode = @identifier\n" +
                      "END\n" +
                      "IF (@pathCount > 0)\n" +
                      "BEGIN\n" +
                        "RAISERROR (N'Cannot delete as this path has children', 10, 1)\n" +
                        "ROLLBACK WORK\n" +
                      "END\n" +
                      "fetch next from cur_Deleted into @identifier,@assetType,@parentPath,@assetName,@hostInode\n" +
                   "END;\n";
        return parentPathCheckTrigger + checkChildAssetsTrigger;
    }
    
    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }

    @Override
    public String getH2Script() {
        return null;
    }
    
}
