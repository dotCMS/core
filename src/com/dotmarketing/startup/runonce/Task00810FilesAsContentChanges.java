package com.dotmarketing.startup.runonce;


import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

/**
 * This task creates the new default file type column in the folder table
 * also creates the default file asset structure 
 * http://jira.dotmarketing.net/browse/DOTCMS-6435
 * @author Roger 
 *
 */
public class Task00810FilesAsContentChanges implements StartupTask {

	public boolean forceRun() {
		return true;
	}

	public void executeUpgrade() throws DotDataException, DotRuntimeException {
		
		try{
		    DbConnectionFactory.getConnection().setAutoCommit(true);  
			DotConnect dc = new DotConnect();
			String addDefaultFileType = "alter table folder add default_file_type varchar(36)";
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE))
			    addDefaultFileType=addDefaultFileType.replaceAll("varchar\\(", "varchar2\\(");
			
			String addFK = "alter table folder add constraint fk_folder_file_structure_type foreign key(default_file_type) references structure(inode)";
			String updateFolders = "update folder set default_file_type = ?";
			dc.executeStatement(addDefaultFileType);
			//create default file type structure
			Structure defaultFileAssetStructure = createDefaultFileAssetStructure();
			//set all folders to point to default file structure 
			if(DbConnectionFactory.isOracle())
				dc.executeStatement("ALTER TRIGGER RENAME_FOLDER_ASSETS_TRIGGER DISABLE");
			if(DbConnectionFactory.isMySql())
			    dc.executeStatement("SET @DISABLE_TRIGGER=1");
			dc.setSQL(updateFolders);
			dc.addParam(defaultFileAssetStructure.getInode());
			dc.loadResult();
			if(DbConnectionFactory.isOracle())
				dc.executeStatement("ALTER TRIGGER RENAME_FOLDER_ASSETS_TRIGGER ENABLE");
			if(DbConnectionFactory.isMySql())
			    dc.executeStatement("SET @DISABLE_TRIGGERS=NULL");
			//add fk
			dc.executeStatement(addFK);
			CacheLocator.getFolderCache().clearCache();
		}catch(Exception e){
			Logger.error(this, e.getMessage(),e);
		}

	}
	
	private Structure createDefaultFileAssetStructure() throws DotDataException{
		Structure fileAsset = new Structure();
		fileAsset.setFixed(true);
		fileAsset.setHost(Host.SYSTEM_HOST);
		fileAsset.setFolder(FolderAPI.SYSTEM_FOLDER);
		fileAsset.setName(FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_NAME);
		fileAsset.setDescription(FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_DESCRIPTION);
		fileAsset.setOwner(APILocator.getUserAPI().getSystemUser().getUserId());
		fileAsset.setStructureType(Structure.STRUCTURE_TYPE_FILEASSET);
		fileAsset.setVelocityVarName(FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME);
		StructureFactory.saveStructure(fileAsset);
		APILocator.getFileAssetAPI().createBaseFileAssetFields(fileAsset);
		StructureCache.addStructure(fileAsset);
		return fileAsset;
	}

}
