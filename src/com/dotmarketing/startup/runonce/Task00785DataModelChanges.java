package com.dotmarketing.startup.runonce;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.MaintenanceUtil;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;


public class Task00785DataModelChanges implements StartupTask  {

		private List<Map<String, String>> getAssetIdentifiers(String type){
		   DotConnect dc = new DotConnect();
		   dc.setSQL("select * from inode where type = ? where exists (select * from "+type+" a where a.inode=inode.inode)");
		   dc.addParam(type);
		   List<Map<String, String>> results=null;
		   try {
			   results = dc.getResults();
		   } catch (DotDataException e) {
			 Logger.error(this, e.getMessage(), e);
		   }
		   return results;
		}

		private void deleteIdentifiersFromInode(){
		DotConnect dc = new DotConnect();
		String dropFKs = "";
		if (DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){

			dropFKs = "ALTER TABLE tree DROP FOREIGN KEY FK36739EC4AB08AA;" +
			          "ALTER TABLE tree DROP FOREIGN KEY FK36739E5A3F51C;" +
			          "ALTER TABLE permission DROP FOREIGN KEY permission_inode_fk;" +
			          "ALTER TABLE permission_reference DROP FOREIGN KEY permission_asset_id_fk;" +
			          "ALTER TABLE permission_reference DROP FOREIGN KEY permission_reference_id_fk;" +
			          "ALTER TABLE structure DROP FOREIGN KEY fk_structure_host;" +
			          "ALTER TABLE identifier DROP FOREIGN KEY fk9f88aca95fb51eb;";

		}else{
			dropFKs = "Alter table tree drop constraint FK36739EC4AB08AA;" +
            		  "Alter table tree drop constraint FK36739E5A3F51C;" +
            		  "Alter table permission drop constraint permission_inode_fk;" +
            		  "Alter table permission_reference drop constraint permission_asset_id_fk;" +
            		  "Alter table permission_reference drop constraint permission_reference_id_fk;" +
            		  "Alter table structure drop constraint fk_structure_host;"+
            		  "Alter table identifier drop constraint fk9f88aca95fb51eb;";
		}

		String deleteIdentifiers = "DELETE from tree where (parent in(select identifier from inode where type='file_asset') or parent in(select inode from folder)) and child in(select inode from inode where type ='file_asset');" +
								   "DELETE from tree where parent in(select identifier from inode where type='template')and child in(select inode from inode where type ='template');" +
								   "DELETE from tree where parent in(select identifier from inode where type='containers')and child in(select inode from inode where type ='containers');" +
								   "DELETE from tree where parent in(select identifier from inode where type='contentlet')and child in(select inode from inode where type ='contentlet');" +
								   "DELETE from tree where (parent in(select identifier from inode where type='htmlpage')or parent in(select inode from folder)) and child in(select inode from inode where type ='htmlpage');" +
								   "DELETE from tree where (parent in(select identifier from inode where type='links') or parent in(select inode from folder)) and child in(select inode from inode where type ='links');" +
								   "DELETE from inode where type='identifier';";

		/*String addFKs = "alter table tree add constraint FK36739EC4AB08AA foreign key (parent) references inode;" +
						"alter table tree add constraint FK36739E5A3F51C foreign key (child) references inode;" +
						"alter table permission add constraint permission_inode_fk foreign key (inode_id) references inode(inode);" +
						"alter table permission_reference add constraint permission_asset_id_fk foreign key (asset_id) references inode(inode);" +
						"alter table permission_reference add constraint permission_reference_id_fk foreign key (reference_id) references inode(inode);";*/
		try {
			    List<String> queryList = SQLUtil.tokenize(dropFKs + deleteIdentifiers);
				for (String query : queryList) {
					dc.executeStatement(query);
				}
			//dc.executeStatement(addFKs);
		} catch (SQLException e) {
			Logger.error(this, e.getMessage());
			e.printStackTrace();
		}

	}
	private void deleteOrphanedAssets(){
		DotConnect dc = new DotConnect();
		String deleteFileAsset = "DELETE from file_asset where inode in(select inode from inode where type='file_asset' and (identifier not in(select inode from identifier) or identifier is null))";
		String deleteContentlet = "DELETE from contentlet where inode in(select inode from inode where type='contentlet' and (identifier not in(select inode from identifier) or identifier is null))";
		String deleteContainers = "DELETE from containers where inode in(select inode from inode where type='containers' and (identifier not in(select inode from identifier) or identifier is null))";
		String deleteTemplate = "DELETE from template where inode in(select inode from inode where type='template' and (identifier not in(select inode from identifier) or identifier is null))";
		String deleteHTMLPage = "DELETE from htmlpage where inode in(select inode from inode where type='htmlpage' and (identifier not in(select inode from identifier) or identifier is null))";
		String deleteLinks = "DELETE from links where inode in(select inode from inode where type='links' and (identifier not in(select inode from identifier) or identifier is null))";
		
		// http://jira.dotmarketing.net/browse/DOTCMS-7387
		// we need to delete permissions on inodes we will delete
		String deletePermRefInodesToDelete=
		        "delete from permission_reference\n"+
		                "       where exists \n"+
		                "       (select * from inode where (permission_reference.asset_id=inode.inode OR permission_reference.reference_id=inode.inode) AND\n"+
		                "         ((identifier not in(select inode from identifier)) \n"+
		                "         OR (type = 'identifier' and inode not in (SELECT inode FROM identifier))\n"+
		                "         OR (type in('htmlpage','links','contentlet','containers','template','file_asset') and identifier is null))\n"+
		                "       )";
		String deletePermInodesToDelete=
        		"delete from permission\n"+
        		"       where exists \n"+
        		"       (select * from inode where permission.inode_id=inode.inode AND\n"+
        		"         ((identifier not in(select inode from identifier)) \n"+
        		"         OR (type = 'identifier' and inode not in (SELECT inode FROM identifier))\n"+
        		"         OR (type in('htmlpage','links','contentlet','containers','template','file_asset') and identifier is null))\n"+
        		"       )";
		
		String deleteInodes = "DELETE from inode where (identifier not in(select inode from identifier)) OR (type = 'identifier' and inode not in (SELECT inode FROM identifier))" +
		                      "OR (type in('htmlpage','links','contentlet','containers','template','file_asset') and identifier is null)";
		String deleteTree = "DELETE from tree where parent in(select inode from inode where (identifier not in(select inode from identifier)) OR (type = 'identifier' and inode not in (SELECT inode FROM identifier)) " +
				            "OR (type in('htmlpage','links','contentlet','containers','template','file_asset') and identifier is null))";
		String deleteTree1 = "DELETE from tree where child in(select inode from inode where (identifier not in(select inode from identifier)) OR (type = 'identifier' and inode not in (SELECT inode FROM identifier)) " +
        					 "OR (type in('htmlpage','links','contentlet','containers','template','file_asset') and identifier is null))";
	    try{
		    dc.executeStatement(deleteFileAsset);
		    dc.executeStatement(deleteContentlet);
		    dc.executeStatement(deleteContainers);
		    dc.executeStatement(deleteTemplate);
		    dc.executeStatement(deleteHTMLPage);
		    dc.executeStatement(deleteLinks);
		    dc.executeStatement(deletePermRefInodesToDelete);
		    dc.executeStatement(deletePermInodesToDelete);
		    dc.executeStatement(deleteInodes);
		    dc.executeStatement(deleteTree);
		    dc.executeStatement(deleteTree1);
	    }catch (SQLException e) {
	    	Logger.error(this,e.getMessage());
	    	e.printStackTrace();
	    }
	}

	public void dotPathFunction() throws SQLException {

		DotConnect dc = new DotConnect();
		String pgPathFunction = "CREATE OR REPLACE FUNCTION dotFolderPath(parent_path text, asset_name text)\n"+
								  "RETURNS text AS '\n"+
								  "BEGIN\n"+
								  "  IF(parent_path=''/System folder'') THEN\n"+
								  "    RETURN ''/'';\n"+
								  "  ELSE\n"+
								  "    RETURN parent_path || asset_name || ''/'';\n"+
								  "  END IF;\n"+
								  "END;'\n"+
								  "LANGUAGE plpgsql;\n";
		String myPathFunction = "CREATE FUNCTION dotFolderPath (parent_path char(255), asset_name char(255)) RETURNS char(255)\n"+
								"BEGIN\n"+
								"IF (parent_path='/System folder') THEN\n"+
								"  RETURN '/';\n"+
								"ELSE\n"+
								"  RETURN CONCAT(parent_path,asset_name,'/');\n"+
								"END IF;\n"+
								"END\n";
		String oraPathFunction ="CREATE OR REPLACE FUNCTION dotFolderPath(parent_path IN varchar2, asset_name IN varchar2) RETURN varchar2 IS\n"+
								"BEGIN\n"+
								"  IF parent_path='/System folder' THEN\n"+
								"    RETURN '/';\n"+
								"  ELSE\n"+
								"    RETURN parent_path || asset_name || '/';\n"+
								"  END IF;\n"+
								"END;\n";
		String msPathFunction = "CREATE FUNCTION dotFolderPath(@parent_path CHAR(255), @asset_name CHAR(255))\n" +
				                " RETURNS CHAR(255)\n" +
				                "BEGIN\n" +
				                "  IF(@parent_path='/System folder')\n" +
				                "  BEGIN\n" +
				                "	RETURN '/';\n" +
				                "  END\n" +
				                "  RETURN @parent_path+@asset_name+'/';\n" +
				                "END;\n";
		if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL))
			dc.executeStatement(pgPathFunction);
		else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL))
			dc.executeStatement(myPathFunction);
		else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE))
			dc.executeStatement(oraPathFunction);
		else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL))
			dc.executeStatement(msPathFunction);
	}

	public void triggersChanges(){
		DotConnect dc = new DotConnect();
		String fileTrigger = "CREATE OR REPLACE FUNCTION file_asset_live_version_check() RETURNS trigger AS '\n" +
							 "DECLARE\n" +
							 "currentliveinode varchar(36);\n" +
							 "BEGIN\n" +
							 "IF tg_op = ''DELETE'' THEN\n" +
							 "RETURN OLD;\n" +
							 "END IF;\n" +
							 "IF tg_op = ''INSERT'' OR tg_op = ''UPDATE'' THEN\n" +
							 "select inode.inode into currentliveinode from file_asset, inode where live = true and file_asset.inode = inode.inode and\n" +
							 "file_asset.identifier = (select file_asset.identifier from file_asset where file_asset.inode = NEW.inode);\n" +
							 "IF FOUND AND NEW.live = true AND NEW.inode <> currentliveinode THEN\n" +
							 "RAISE EXCEPTION ''Cannot insert/update multiple live versions in the file_asset table,  inode: %'', currentliveinode;\n" +
							 "RETURN NULL;\n" +
							 "ELSE\n" +
							 "RETURN NEW;\n" +
							 "END IF;\n" +
							 "END IF;\n" +
							 "RETURN NULL;\n" +
							 "END\n" +
							 "' LANGUAGE plpgsql;";

		String ContentletTrigger1 = "CREATE OR REPLACE FUNCTION content_live_version_check() RETURNS trigger AS '\n" +
								    "DECLARE\n" +
								    "currentliveinode varchar(36);\n" +
								    "BEGIN\n" +
								    "IF tg_op = ''DELETE'' THEN\n" +
								    "RETURN OLD;\n" +
								    "END IF;\n" +
								    "IF tg_op = ''INSERT'' OR tg_op = ''UPDATE'' THEN\n" +
								    "select inode.inode into currentliveinode from contentlet, inode where live = true and contentlet.inode = inode.inode and\n" +
								    "contentlet.identifier = (select contentlet.identifier from contentlet where contentlet.inode = NEW.inode) and contentlet.language_id = NEW.language_id;\n" +
								    "IF FOUND AND NEW.live = true AND NEW.inode <> currentliveinode THEN\n" +
								    "RAISE EXCEPTION ''Cannot insert/update multiple live versions in the contentlet table,  inode: %'', currentliveinode;\n" +
								    "RETURN NULL;\n" +
								    "ELSE\n" +
								    "RETURN NEW;\n" +
								    "END IF;\n" +
								    "END IF;\n" +
								    "RETURN NULL;\n" +
								    "END\n" +
								    "' LANGUAGE plpgsql;";

		String ContentletTrigger2 = "CREATE OR REPLACE FUNCTION content_work_version_check() RETURNS trigger AS '\n" +
									"DECLARE\n" +
									"currentworkinginode varchar(36);\n" +
									"BEGIN\n" +
									"IF tg_op = ''DELETE'' THEN\n" +
									"RETURN OLD;\n" +
									"END IF;\n" +
									"IF tg_op = ''INSERT'' OR tg_op = ''UPDATE'' THEN\n" +
									"select inode.inode into currentworkinginode from contentlet, inode where working = true and contentlet.inode = inode.inode and\n" +
									"contentlet.identifier = (select contentlet.identifier from contentlet where contentlet.inode = NEW.inode) and contentlet.language_id = NEW.language_id;\n" +
									"IF FOUND AND NEW.working = true AND NEW.inode <> currentworkinginode THEN\n" +
									"RAISE EXCEPTION ''Cannot insert/update multiple working versions in the contentlet table, Working inode: %'', currentworkinginode;\n" +
									"RETURN NULL;\n" +
									"ELSE\n" +
									"RETURN NEW;\n" +
									"END IF;\n" +
									"END IF;\n" +
									"RETURN NULL;\n" +
									"END\n" +
									"' LANGUAGE plpgsql;";

		String identifierTrigger = "CREATE OR REPLACE FUNCTION identifier_host_inode_check() RETURNS trigger AS '\n" +
								   "DECLARE\n" +
								   "inodeType varchar(100);\n" +
								   "BEGIN\n" +
								   "IF (tg_op = ''INSERT'' OR tg_op = ''UPDATE'') AND substr(NEW.asset_type, 0, 8) <> ''content'' AND\n"+
								   "(NEW.host_inode IS NULL OR NEW.host_inode = '''') THEN\n" +
								   "RAISE EXCEPTION ''Cannot insert/update a null or empty host inode for this kind of identifier'';\n" +
								   "RETURN NULL;\n" +
								   "ELSE\n" +
								   "RETURN NEW;\n"+
								   "END IF;\n" +
								   "RETURN NULL;\n"+
								   "END\n" +
								   "' LANGUAGE plpgsql;";

		String triggerInOracle = "CREATE OR REPLACE TRIGGER check_identifier_host_inode\n" +
								 "BEFORE INSERT OR UPDATE ON identifier\n" +
								 "FOR EACH ROW\n" +
								 "DECLARE\n" +
								 "BEGIN\n" +
								     "dbms_output.put_line('asset_type: ' || SUBSTR(:new.asset_type,0,7));\n" +
								 "dbms_output.put_line('host_inode: ' || :new.host_inode);\n" +
								 "IF SUBSTR(:new.asset_type,0,7) <> 'content' AND (:new.host_inode is NULL OR :new.host_inode = '') THEN\n" +
								     "RAISE_APPLICATION_ERROR(-20000, 'Cannot insert/update a null or empty host inode for this kind of identifier');\n" +
								 "END IF;\n" +
								 "END;";

		String triggerInMSSQL = "CREATE TRIGGER check_identifier_host_inode\n" +
								"ON identifier\n" +
								"FOR INSERT, UPDATE AS\n" +
								"DECLARE @assetType varchar(10)\n" +
								"DECLARE @hostInode varchar(50)\n" +
								"DECLARE cur_Inserted1 cursor LOCAL FAST_FORWARD for\n" +
								"Select [asset_type], [host_inode]\n" +
								"from inserted\n" +
								"for Read Only\n" +
								"open cur_Inserted1\n" +
								"fetch next from cur_Inserted1 into @assetType,@hostInode\n" +
								"while @@FETCH_STATUS <> -1\n" +
								"BEGIN\n" +
								"IF(@assetType <> 'content' AND (@hostInode is null OR @hostInode = ''))\n" +
								"BEGIN\n" +
								"RAISERROR (N'Cannot insert/update a null or empty host inode for this kind of identifier', 10, 1)\n" +
								"ROLLBACK WORK\n" +
								"END\n" +
								"fetch next from cur_Inserted1 into @assetType,@hostInode\n" +
								"END;";

			try {
				if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)){
					dc.executeStatement(triggerInOracle);
				}
				if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
					dc.executeStatement(fileTrigger);
					dc.executeStatement(ContentletTrigger1);
					dc.executeStatement(ContentletTrigger2);
					dc.executeStatement(identifierTrigger);
				}
				if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
					dc.executeStatement(triggerInMSSQL);
				}
			} catch (SQLException e) {
				Logger.error(this, e.getMessage());
				e.printStackTrace();
			}
	}
	private List<String> newTriggersForPostgres(){
		List<String> triggers = new ArrayList<String>();
		String fileVersionsCheckTrigger = "CREATE OR REPLACE FUNCTION file_versions_check() RETURNS trigger AS '\n" +
		  								  "DECLARE\n" +
		  								      "versionsCount integer;\n" +
		  								  "BEGIN\n" +
		  								      "IF (tg_op = ''DELETE'') THEN\n" +
		  								         "select count(*) into versionsCount from file_asset where identifier = OLD.identifier;\n"  +
		  								      "IF (versionsCount = 0)THEN\n" +
		  								         "DELETE from identifier where id = OLD.identifier;\n" +
		  								      "ELSE\n" +
		  								        "RETURN OLD;\n" +
		  								      "END IF;\n" +
		  								     "END IF;\n" +
		  								  "RETURN NULL;\n" +
		  								  "END\n" +
		  								  "' LANGUAGE plpgsql;\n" +
		  								  "CREATE TRIGGER file_versions_check_trigger AFTER DELETE\n" +
		  								  "ON file_asset FOR EACH ROW\n" +
		  								  "EXECUTE PROCEDURE file_versions_check();\n";

		String contentVersionsCheckTrigger = "CREATE OR REPLACE FUNCTION content_versions_check() RETURNS trigger AS '\n" +
			 								 "DECLARE\n" +
			 								     "versionsCount integer;\n" +
			 								 "BEGIN\n" +
			 								     "IF (tg_op = ''DELETE'') THEN\n" +
			 								        "select count(*) into versionsCount from contentlet where identifier = OLD.identifier;\n" +
			 								     "IF (versionsCount = 0)THEN\n" +
			 								        "DELETE from identifier where id = OLD.identifier;\n" +
			 								     "ELSE\n" +
			 								        "RETURN OLD;\n" +
			 								        "END IF;\n" +
			 								      "END IF;\n" +
			 								    "RETURN NULL;\n" +
			 								 "END\n" +
			 								 "' LANGUAGE plpgsql;\n" +
			 								 "CREATE TRIGGER content_versions_check_trigger AFTER DELETE\n" +
			 								 "ON contentlet FOR EACH ROW\n" +
			 								 "EXECUTE PROCEDURE content_versions_check();\n";

		String linkVersionsCheckTrigger = "CREATE OR REPLACE FUNCTION link_versions_check() RETURNS trigger AS '\n" +
		  								  "DECLARE\n" +
		  								     "versionsCount integer;\n" +
		  								  "BEGIN\n" +
		  								     "IF (tg_op = ''DELETE'') THEN\n" +
		  								       "select count(*) into versionsCount from links where identifier = OLD.identifier;\n" +
		  								     "IF (versionsCount = 0)THEN\n" +
		  								       "DELETE from identifier where id = OLD.identifier;\n" +
		  								     "ELSE\n" +
		  								       "RETURN OLD;\n" +
		  								     "END IF;\n" +
		  								    "END IF;\n" +
		  								    "RETURN NULL;\n" +
		  								  "END\n" +
		  								  "' LANGUAGE plpgsql;\n" +
		  								  "CREATE TRIGGER link_versions_check_trigger AFTER DELETE\n" +
		  								  "ON links FOR EACH ROW\n" +
		  								  "EXECUTE PROCEDURE link_versions_check();\n";

		String containerVersionsCheckTrigger = "CREATE OR REPLACE FUNCTION container_versions_check() RETURNS trigger AS '\n" +
			   								   "DECLARE\n" +
			   								     "versionsCount integer;\n" +
			   								   "BEGIN\n" +
			   								     "IF (tg_op = ''DELETE'') THEN\n" +
			   								       "select count(*) into versionsCount from containers where identifier = OLD.identifier;\n" +
			   								     "IF (versionsCount = 0)THEN\n" +
			   								       "DELETE from identifier where id = OLD.identifier;\n" +
			   								     "ELSE\n" +
			   								       "RETURN OLD;\n" +
			   								     "END IF;\n" +
			   								    "END IF;\n" +
			   								    "RETURN NULL;\n" +
			   								   "END\n" +
			   								   "' LANGUAGE plpgsql;\n" +
			   								   "CREATE TRIGGER container_versions_check_trigger AFTER DELETE\n" +
			   								   "ON containers FOR EACH ROW\n" +
			   								   "EXECUTE PROCEDURE container_versions_check();\n";

		String templateVersionsCheckTrigger = "CREATE OR REPLACE FUNCTION template_versions_check() RETURNS trigger AS '\n" +
											  "DECLARE\n" +
											    "versionsCount integer;\n" +
											  "BEGIN\n" +
											    "IF (tg_op = ''DELETE'') THEN\n" +
											       "select count(*) into versionsCount from template where identifier = OLD.identifier;\n" +
											    "IF (versionsCount = 0)THEN\n" +
											       "DELETE from identifier where id = OLD.identifier;\n" +
											    "ELSE\n" +
											       "RETURN OLD;\n" +
											    "END IF;\n" +
											   "END IF;\n" +
											  "RETURN NULL;\n" +
											  "END\n" +
											  "' LANGUAGE plpgsql;\n" +
											  "CREATE TRIGGER template_versions_check_trigger AFTER DELETE\n" +
											  "ON template FOR EACH ROW\n" +
											  "EXECUTE PROCEDURE template_versions_check();\n";

		String htmlpageVersionsCheckTrigger = "CREATE OR REPLACE FUNCTION htmlpage_versions_check() RETURNS trigger AS '\n" +
			  								  "DECLARE\n" +
			  								    "versionsCount integer;\n" +
			  								  "BEGIN\n" +
			  								    "IF (tg_op = ''DELETE'') THEN\n" +
			  								      "select count(*) into versionsCount from htmlpage where identifier = OLD.identifier;\n" +
			  								    "IF (versionsCount = 0)THEN\n" +
			  								      "DELETE from identifier where id = OLD.identifier;\n" +
			  								  "ELSE\n" +
			  								      "RETURN OLD;\n" +
			  								   "END IF;\n" +
			  								  "END IF;\n" +
			  								  "RETURN NULL;\n" +
			  								  "END\n" +
			  								  "' LANGUAGE plpgsql;\n" +
			  								  "CREATE TRIGGER htmlpage_versions_check_trigger AFTER DELETE\n" +
			  								  "ON htmlpage FOR EACH ROW\n" +
			  								  "EXECUTE PROCEDURE htmlpage_versions_check();\n";

		String parentPathCheckTrigger = "CREATE OR REPLACE FUNCTION identifier_parent_path_check()  RETURNS trigger AS '\n" +
									    "DECLARE\n" +
									       "folderId varchar(36);\n" +
									    "BEGIN\n" +
									       "IF (tg_op = ''INSERT'' OR tg_op = ''UPDATE'') THEN\n" +
									          "IF (NEW.parent_path=''/'') OR (NEW.parent_path=''/System folder'') THEN\n"+
									             "RETURN NEW;\n"+
									          "ELSE\n" +
									             "select id into folderId from identifier where asset_type=''folder'' and host_inode = NEW.host_inode and parent_path||asset_name||''/'' = NEW.parent_path and id <> NEW.id;\n"+
									          "IF FOUND THEN\n" +
									             "RETURN NEW;\n" +
									          "ELSE\n" +
									               "RAISE EXCEPTION ''Cannot insert/update for this path does not exist for the given host TRUNK!!'';\n" +
									               "RETURN NULL;\n" +
									          "END IF;\n" +
									       "END IF;\n" +
									     "END IF;\n" +
									    "RETURN NULL;\n" +
									    "END\n" +
									    "' LANGUAGE plpgsql;\n" +
									    "CREATE TRIGGER identifier_parent_path_trigger\n" +
									    "BEFORE INSERT OR UPDATE\n" +
									    "ON identifier FOR EACH ROW\n" +
									    "EXECUTE PROCEDURE identifier_parent_path_check();\n";

		String checkChildAssetTrigger = "CREATE OR REPLACE FUNCTION check_child_assets() RETURNS trigger AS '\n" +
		  								   "DECLARE\n" +
		  								   	  "pathCount integer;\n"+
		  								   "BEGIN\n" +
		  								   	  "IF (tg_op = ''DELETE'') THEN\n" +
		  								   	  	"IF(OLD.asset_type =''folder'') THEN\n" +
		  								   	  		"select count(*) into pathCount from identifier where parent_path = OLD.parent_path||OLD.asset_name||''/'' and host_inode = OLD.host_inode;\n" +
		  								   	  	"END IF;\n" +
		  								   	  	"IF(OLD.asset_type =''contentlet'') THEN\n" +
		  								   	  	    "select count(*) into pathCount from identifier where host_inode = OLD.id;\n" +
		  								   	  	"END IF;\n" +
		  								   	  	"IF (pathCount > 0 )THEN\n" +
		  								   	  		"RAISE EXCEPTION ''Cannot delete as this path has children'';\n" +
		  								   	  		"RETURN NULL;\n" +
		  								   	  	"ELSE\n" +
		  								   	  		"RETURN OLD;\n" +
		  								   	  	"END IF;\n" +
		  								   	  "END IF;\n" +
		  								   	"RETURN NULL;\n" +
		  								   	"END\n" +
		  								   	"' LANGUAGE plpgsql;\n" +
		  								   	"CREATE TRIGGER check_child_assets_trigger BEFORE DELETE\n" +
		  								   	"ON identifier FOR EACH ROW\n" +
		  								   	"EXECUTE PROCEDURE check_child_assets();";

			triggers = SQLUtil.tokenize(fileVersionsCheckTrigger +
										contentVersionsCheckTrigger +
										linkVersionsCheckTrigger +
										containerVersionsCheckTrigger +
										templateVersionsCheckTrigger +
										htmlpageVersionsCheckTrigger +
										parentPathCheckTrigger +
										checkChildAssetTrigger);
			return triggers;
	}
	private List<String> newTriggersForOracle(){
		List<String> triggers = new ArrayList<String>();

		String fileVersionsCheckTrigger = "CREATE OR REPLACE PACKAGE file_pkg as\n" +
		  								     "type array is table of file_asset%rowtype index by binary_integer;\n" +
		  								     "oldvals array;\n" +
		  								     "empty array;\n" +
		  								  "END;\n" +
		  								  "/\n" +
		  								  "CREATE OR REPLACE trigger file_versions_bd\n" +
		  								  "BEFORE DELETE ON file_asset\n" +
		  								  "BEGIN\n" +
		  								  		"file_pkg.oldvals := file_pkg.empty;\n" +
										  "END;\n" +
										  "/\n" +
										  "CREATE OR REPLACE TRIGGER file_versions_bdfer\n" +
										  "BEFORE DELETE ON file_asset\n" +
										  "FOR EACH ROW\n" +
										  "BEGIN\n" +
										  		"file_pkg.oldvals(file_pkg.oldvals.count+1).identifier := :old.identifier;\n" +
										  "END;\n" +
										  "/\n" +
										  "CREATE OR REPLACE TRIGGER  file_versions_trigger\n" +
										  "AFTER DELETE ON file_asset\n" +
										  "DECLARE\n" +
										  		"versionsCount integer;\n" +
										  "BEGIN\n" +
										  		"for i in 1 .. file_pkg.oldvals.count LOOP\n" +
										  		  "select count(*) into versionsCount from file_asset where identifier = file_pkg.oldvals(i).identifier;\n" +
										  		  "IF (versionsCount = 0)THEN\n" +
										  			"DELETE from identifier where id = file_pkg.oldvals(i).identifier;\n" +
										  		  "END IF;\n" +
										  		"END LOOP;\n" +
										  "END;\n" +
										  "/\n";

		String contentVersionsCheckTrigger = "CREATE OR REPLACE PACKAGE content_pkg as\n" +
											    "type array is table of contentlet%rowtype index by binary_integer;\n" +
												"oldvals array;\n" +
												"empty array;\n" +
											 "END;\n" +
											 "/\n" +
											 "CREATE OR REPLACE TRIGGER content_versions_bd\n" +
											 "BEFORE DELETE ON contentlet\n" +
											 "BEGIN\n" +
											 	"content_pkg.oldvals := content_pkg.empty;\n" +
											 "END;\n" +
											 "/\n" +
											 "CREATE OR REPLACE TRIGGER  content_versions_bdfer\n" +
											 "BEFORE DELETE ON contentlet\n" +
											 "FOR EACH ROW\n" +
											 "BEGIN\n" +
											 	"content_pkg.oldvals(content_pkg.oldvals.count+1).identifier := :old.identifier;\n" +
											 "END;\n" +
											 "/\n" +
											 "CREATE OR REPLACE TRIGGER  content_versions_trigger\n" +
											 "AFTER DELETE ON contentlet\n" +
											 "DECLARE\n" +
											 	"versionsCount integer;\n" +
											 "BEGIN\n" +
											 	"for i in 1 .. content_pkg.oldvals.count LOOP\n" +
											 	    "select count(*) into versionsCount from contentlet where identifier = content_pkg.oldvals(i).identifier;\n" +
											 	    "IF (versionsCount = 0)THEN\n" +
											 	       "DELETE from identifier where id = content_pkg.oldvals(i).identifier;\n" +
											 	    "END IF;\n" +
											 	"END LOOP;\n" +
											 "END;\n" +
											 "/\n";

		String linkVersionsCheckTrigger = "CREATE OR REPLACE PACKAGE link_pkg as\n" +
		 								    "type array is table of links%rowtype index by binary_integer;\n" +
		 								    "oldvals array;\n" +
		 								    "empty array;\n" +
										  "END;\n" +
										  "/\n" +
										  "CREATE OR REPLACE TRIGGER link_versions_bd\n" +
										  "BEFORE DELETE ON links\n" +
										  "BEGIN\n" +
										  		"link_pkg.oldvals := link_pkg.empty;\n" +
										  "END;\n" +
										  "/\n" +
										  "CREATE OR REPLACE TRIGGER link_versions_bdfer\n" +
										  "BEFORE DELETE ON links\n" +
										  "FOR EACH ROW\n" +
										  "BEGIN\n" +
										  		"link_pkg.oldvals(link_pkg.oldvals.count+1).identifier := :old.identifier;\n" +
										  "END;\n" +
										  "/\n" +
										  "CREATE OR REPLACE TRIGGER link_versions_trigger\n" +
										  "AFTER DELETE ON links\n" +
										  "DECLARE\n" +
										  		"versionsCount integer;\n" +
										  "BEGIN\n" +
										  	  "for i in 1 .. link_pkg.oldvals.count LOOP\n" +
										  		 "select count(*) into versionsCount from links where identifier = link_pkg.oldvals(i).identifier;\n" +
										  		 "IF (versionsCount = 0)THEN\n" +
										  		     "DELETE from identifier where id = link_pkg.oldvals(i).identifier;\n" +
										  		 "END IF;\n" +
										  	  "END LOOP;\n" +
										  "END;\n" +
										  "/\n";

		String containerVersionsCheckTrigger = "CREATE OR REPLACE PACKAGE container_pkg as\n" +
		 									   	   "type array is table of containers%rowtype index by binary_integer;\n" +
		 									   	   "oldvals array;\n" +
		 									   	   "empty array;\n" +
											   "END;\n" +
											   "/\n" +
											   "CREATE OR REPLACE TRIGGER container_versions_bd\n" +
											   "BEFORE DELETE ON containers\n" +
											   "BEGIN\n" +
											   	  "container_pkg.oldvals := container_pkg.empty;\n" +
											   "END;\n" +
											   "/\n" +
											   "CREATE OR REPLACE TRIGGER container_versions_bdfer\n" +
											   "BEFORE DELETE ON containers\n" +
											   "FOR EACH ROW\n" +
											   "BEGIN\n" +
											      "container_pkg.oldvals(container_pkg.oldvals.count+1).identifier := :old.identifier;\n" +
											   "END;\n" +
											   "/\n" +
											   "CREATE OR REPLACE TRIGGER container_versions_trigger\n" +
											   "AFTER DELETE ON containers\n" +
											   "DECLARE\n" +
											       "versionsCount integer;\n" +
											   "BEGIN\n" +
											       "for i in 1 .. container_pkg.oldvals.count LOOP\n" +
											        "select count(*) into versionsCount from containers where identifier = container_pkg.oldvals(i).identifier;\n" +
											         "IF (versionsCount = 0)THEN\n" +
											           "DELETE from identifier where id = container_pkg.oldvals(i).identifier;\n" +
											         "END IF;\n" +
											       "END LOOP;\n" +
											   "END;\n" +
											   "/\n";

		String templateVersionsCheckTrigger = "CREATE OR REPLACE PACKAGE template_pkg as\n" +
											    "type array is table of template%rowtype index by binary_integer;\n" +
											    "oldvals array;\n" +
			                                    "empty array;\n" +
											  "END;\n" +
											  "/\n" +
											  "CREATE OR REPLACE TRIGGER template_versions_bd\n" +
											  "BEFORE DELETE ON template\n" +
											  "BEGIN\n" +
											      "template_pkg.oldvals := template_pkg.empty;\n" +
											  "END;\n" +
											  "/\n" +
											  "CREATE OR REPLACE TRIGGER template_versions_bdfer\n" +
											  "BEFORE DELETE ON template\n" +
											  "FOR EACH ROW\n" +
											  "BEGIN\n" +
											     "template_pkg.oldvals(template_pkg.oldvals.count+1).identifier := :old.identifier;\n" +
											  "END;\n" +
											  "/\n" +
											  "CREATE OR REPLACE TRIGGER template_versions_trigger\n" +
											  "AFTER DELETE ON template\n" +
											  "DECLARE\n" +
											      "versionsCount integer;\n" +
											  "BEGIN\n" +
											      "for i in 1 .. template_pkg.oldvals.count LOOP\n" +
											       "select count(*) into versionsCount from template where identifier = template_pkg.oldvals(i).identifier;\n" +
											       "IF (versionsCount = 0)THEN\n" +
											          "DELETE from identifier where id = template_pkg.oldvals(i).identifier;\n" +
											       "END IF;\n" +
											      "END LOOP;\n" +
											  "END;\n" +
											  "/\n";

		String htmlpageVersionsCheckTrigger = "CREATE OR REPLACE PACKAGE htmlpage_pkg as\n" +
		 									    "type array is table of htmlpage%rowtype index by binary_integer;\n" +
		 									    "oldvals array;\n" +
		 									    "empty array;\n" +
		 									  "END;\n" +
		 									  "/\n" +
		 									  "CREATE OR REPLACE TRIGGER htmlpage_versions_bd\n" +
		 									  "BEFORE DELETE ON htmlpage\n" +
		 									  "BEGIN\n" +
		 									  		"htmlpage_pkg.oldvals := htmlpage_pkg.empty;\n" +
											  "END;\n" +
											  "/\n" +
											  "CREATE OR REPLACE TRIGGER htmlpage_versions_bdfer\n" +
											  "BEFORE DELETE ON htmlpage\n" +
											  "FOR EACH ROW\n" +
											  "BEGIN\n" +
											  		"htmlpage_pkg.oldvals(htmlpage_pkg.oldvals.count+1).identifier := :old.identifier;\n" +
											  "END;\n" +
											  "/\n" +
											  "CREATE OR REPLACE TRIGGER htmlpage_versions_trigger\n" +
											  "AFTER DELETE ON htmlpage\n" +
											  "DECLARE\n" +
											  	  "versionsCount integer;\n" +
											  "BEGIN\n" +
											  	  "for i in 1 .. htmlpage_pkg.oldvals.count LOOP\n" +
											  	  	  "select count(*) into versionsCount from htmlpage where identifier = htmlpage_pkg.oldvals(i).identifier;\n" +
											  	  	  "IF (versionsCount = 0)THEN\n" +
											  	  	  	 "DELETE from identifier where id = htmlpage_pkg.oldvals(i).identifier;\n" +
											  	  	  "END IF;\n" +
											  	  "END LOOP;\n" +
											  "END;\n" +
											  "/\n";

		String parentPathCheckTrigger = " CREATE OR REPLACE PACKAGE check_parent_path_pkg as \n" +
                                        "   type ridArray is table of rowid index by binary_integer; \n"+
                                        "   newRows ridArray; \n" +
                                        "   empty   ridArray; \n" +
                                        " END; \n" +
                                        "/\n"+
				                        "CREATE OR REPLACE TRIGGER identifier_parent_path_check\n " +
										" AFTER INSERT OR UPDATE ON identifier\n " +
										" DECLARE\n " +
										"   rowcount varchar2(100);\n " +
										"   assetIdentifier varchar2(100);\n " +
										"   parentPath varchar2(100);\n " +
										"   hostInode varchar2(100);\n " +
										" BEGIN\n " +
										"    for i in 1 .. check_parent_path_pkg.newRows.count LOOP\n " +
										"       select id,parent_path,host_inode into assetIdentifier,parentPath,hostInode from identifier where rowid = check_parent_path_pkg.newRows(i);\n " +
										"       IF(parentPath='/' OR parentPath='/System folder') THEN\n " +
										"         return;\n " +
										"       ELSE\n " +
										"         select count(*) into rowcount from identifier where asset_type='folder' and host_inode = hostInode and parent_path||asset_name||'/' = parentPath and id <> assetIdentifier;\n " +
										"         IF (rowcount = 0) THEN    \n " +
										"            RAISE_APPLICATION_ERROR(-20000, 'Cannot insert/update for this path does not exist for the given host');   \n " +
										"         END IF;   \n " +
										"       END IF;\n " +
										" END LOOP;\n " +
										" END;\n " +
		 								"/\n";

		String checkChildAssetsTrigger = "CREATE OR REPLACE PACKAGE child_assets_pkg as\n" +
		   								    "type array is table of identifier%rowtype index by binary_integer;\n" +
		   								    "oldvals array;\n" +
		   								    "empty array;\n" +
		   								 "END;\n" +
										 "/\n" +
		   								 "CREATE OR REPLACE trigger check_child_assets_bd\n" +
		   								 "BEFORE DELETE ON identifier\n" +
		   								 "BEGIN\n" +
		   								    "child_assets_pkg.oldvals := child_assets_pkg.empty;\n" +
		   								 "END;\n" +
		   								 "/\n" +
		   								 "CREATE OR REPLACE TRIGGER check_child_assets_bdfer\n" +
		   								 "BEFORE DELETE ON identifier\n" +
		   								 "FOR EACH ROW\n" +
		   								 "Declare\n" +
		   								     "i    number default child_assets_pkg.oldvals.count+1;\n" +
		   								 "BEGIN\n" +
		   								     "child_assets_pkg.oldvals(i).id := :old.id;\n" +
		   								     "child_assets_pkg.oldvals(i).asset_type := :old.asset_type;\n" +
		   								     "child_assets_pkg.oldvals(i).parent_path:= :old.parent_path;\n" +
		   								     "child_assets_pkg.oldvals(i).asset_name:= :old.asset_name;\n" +
		   								     "child_assets_pkg.oldvals(i).host_inode:= :old.host_inode;\n" +
		   								 "END;\n" +
		   								 "/\n" +
		   								 "CREATE OR REPLACE TRIGGER  check_child_assets_trigger\n" +
		   								 "AFTER DELETE ON identifier\n" +
		   								 "DECLARE\n" +
		   								     "pathCount integer;\n" +
										 "BEGIN\n" +
										     "for i in 1 .. child_assets_pkg.oldvals.count LOOP\n" +
										        "IF(child_assets_pkg.oldvals(i).asset_type='folder')THEN\n" +
										 	        "select count(*) into pathCount from identifier where parent_path = child_assets_pkg.oldvals(i).parent_path||child_assets_pkg.oldvals(i).asset_name||'/' and host_inode = child_assets_pkg.oldvals(i).host_inode;\n" +
										        "END IF;\n" +
										        "IF(child_assets_pkg.oldvals(i).asset_type='contentlet')THEN\n" +
										 	        "select count(*) into pathCount from identifier where host_inode = child_assets_pkg.oldvals(i).id;\n" +
										        "END IF;\n" +
										        "IF (pathCount > 0 )THEN\n" +
										 	        "RAISE_APPLICATION_ERROR(-20000, 'Cannot delete as this path has children');\n" +
										        "END IF;\n" +
										     "END LOOP;\n" +
										 "END;\n" +
										 "/";
		triggers = SQLUtil.tokenize(fileVersionsCheckTrigger +
									contentVersionsCheckTrigger +
									linkVersionsCheckTrigger +
									containerVersionsCheckTrigger +
									templateVersionsCheckTrigger +
									htmlpageVersionsCheckTrigger +
									parentPathCheckTrigger +
									checkChildAssetsTrigger);
		return triggers;
	}
	private List<String> newTriggersForMSSQL(){
		List<String> triggers = new ArrayList<String>();
		String fileVersionsCheckTrigger = "CREATE Trigger check_file_versions\n" +
										  "ON file_asset\n" +
										  "FOR DELETE AS\n" +
										  	  "DECLARE @totalCount int\n" +
										  	  "DECLARE @identifier varchar(36)\n" +
										  	  "DECLARE file_cur_Deleted cursor LOCAL FAST_FORWARD for\n" +
										  	  	"Select [identifier]\n" +
										  	  	"from deleted\n" +
										  	  	"for Read Only\n" +
										  	  	"open file_cur_Deleted\n" +
										  	  	"fetch next from file_cur_Deleted into @identifier\n" +
										  	  	"while @@FETCH_STATUS <> -1\n" +
										  	  	"BEGIN\n" +
										  	  		"select @totalCount = count(*) from file_asset where [file_asset].[identifier] = @identifier\n" +
										  	  		"IF (@totalCount = 0)\n" +
										  	  		"BEGIN\n" +
										  	  			"DELETE from identifier where id = @identifier\n" +
										  	  		"END\n" +
										  	  			"fetch next from file_cur_Deleted into @identifier\n" +
										  	  	"END;\n";

		String contentVersionsCheckTrigger = "CREATE Trigger check_content_versions\n" +
											 "ON contentlet\n" +
											 "FOR DELETE AS\n" +
											    "DECLARE @totalCount int\n" +
											    "DECLARE @identifier varchar(36)\n" +
											    "DECLARE content_cur_Deleted cursor LOCAL FAST_FORWARD for\n" +
											    "Select identifier\n" +
											    "from deleted\n" +
											    "for Read Only\n" +
											    "open content_cur_Deleted\n" +
											    "fetch next from content_cur_Deleted into @identifier\n" +
											    "while @@FETCH_STATUS <> -1\n" +
											    "BEGIN\n" +
											    	"select @totalCount = count(*) from contentlet where identifier = @identifier\n" +
											    	"IF (@totalCount = 0)\n" +
											    	"BEGIN\n" +
											    		"DELETE from identifier where id = @identifier\n" +
											    	"END\n" +
											    	"fetch next from content_cur_Deleted into @identifier\n" +
											    "END;\n";

		String linkVersionsCheckTrigger = "CREATE Trigger check_link_versions\n" +
										  "ON links\n" +
										  "FOR DELETE AS\n" +
										     "DECLARE @totalCount int\n" +
										     "DECLARE @identifier varchar(36)\n" +
										     "DECLARE link_cur_Deleted cursor LOCAL FAST_FORWARD for\n" +
										     "Select identifier\n" +
										     "from deleted\n" +
										     "for Read Only\n" +
										     "open link_cur_Deleted\n" +
										     "fetch next from link_cur_Deleted into @identifier\n" +
										     "while @@FETCH_STATUS <> -1\n" +
										     "BEGIN\n" +
										     	"select @totalCount = count(*) from links where identifier = @identifier\n" +
										     	"IF (@totalCount = 0)\n" +
										     	"BEGIN\n" +
										     		"DELETE from identifier where id = @identifier\n" +
										     	"END\n" +
										     	"fetch next from link_cur_Deleted into @identifier\n" +
										     "END;\n";

		String containerVersionsCheckTrigger = "CREATE Trigger check_container_versions\n" +
											   "ON containers\n" +
											   "FOR DELETE AS\n" +
											      "DECLARE @totalCount int\n" +
											      "DECLARE @identifier varchar(36)\n" +
											      "DECLARE container_cur_Deleted cursor LOCAL FAST_FORWARD for\n" +
											      "Select identifier\n" +
											      "from deleted\n" +
											      "for Read Only\n" +
											      "open container_cur_Deleted\n" +
											      "fetch next from content_cur_Deleted into @identifier\n" +
											      "while @@FETCH_STATUS <> -1\n" +
											      "BEGIN\n" +
											      	"select @totalCount = count(*) from containers where identifier = @identifier\n" +
											      	"IF (@totalCount = 0)\n" +
											      	"BEGIN\n" +
											      		"DELETE from identifier where id = @identifier\n" +
											      	"END\n" +
											      		"fetch next from container_cur_Deleted into @identifier\n" +
											      "END;\n";

		String templateVersionsCheckTrigger = "CREATE Trigger check_template_versions\n" +
											  "ON template\n" +
											  "FOR DELETE AS\n" +
											     "DECLARE @totalCount int\n" +
											     "DECLARE @identifier varchar(36)\n" +
											     "DECLARE template_cur_Deleted cursor LOCAL FAST_FORWARD for\n" +
											     "Select identifier\n" +
											     "from deleted\n" +
											     "for Read Only\n" +
											     "open template_cur_Deleted\n" +
											     "fetch next from template_cur_Deleted into @identifier\n" +
											     "while @@FETCH_STATUS <> -1\n" +
											     "BEGIN\n" +
											     	"select @totalCount = count(*) from template where identifier = @identifier\n" +
											     	"IF (@totalCount = 0)\n" +
											     	"BEGIN\n" +
											     		"DELETE from identifier where id = @identifier\n" +
											     	"END\n" +
											     	 "fetch next from template_cur_Deleted into @identifier\n" +
											     "END;\n";

		String htmlpageVersionsCheckTrigger = "CREATE Trigger check_htmlpage_versions\n" +
											  "ON htmlpage\n" +
											  "FOR DELETE AS\n" +
											  	 "DECLARE @totalCount int\n" +
											  	 "DECLARE @identifier varchar(36)\n" +
											  	 "DECLARE htmlpage_cur_Deleted cursor LOCAL FAST_FORWARD for\n" +
											  	 "Select identifier\n" +
											  	 "from deleted\n" +
											  	 "for Read Only\n" +
											  	 "open htmlpage_cur_Deleted\n" +
											  	 "fetch next from htmlpage_cur_Deleted into @identifier\n" +
											  	 "while @@FETCH_STATUS <> -1\n" +
											  	 "BEGIN\n" +
											  	 	"select @totalCount = count(*) from htmlpage where identifier = @identifier\n" +
											  	 	"IF (@totalCount = 0)\n" +
											  	 	"BEGIN\n" +
											  	 		"DELETE from identifier where id = @identifier\n" +
											  	 	"END\n" +
											  	 		"fetch next from htmlpage_cur_Deleted into @identifier\n" +
											  	 "END;\n";

		String parentPathCheckTrigger = "CREATE Trigger check_identifier_parent_path\n" +
									    "ON identifier\n" +
										"FOR INSERT,UPDATE AS\n" +
										"DECLARE @folderId varchar(36)\n" +
										"DECLARE @id varchar(36)\n" +
										"DECLARE @assetType varchar(100)\n" +
										"DECLARE @parentPath varchar(100)\n" +
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

		String checkChildAssetsTrigger = "CREATE Trigger check_child_assets\n" +
										 "on identifier\n" +
										 "FOR DELETE AS\n" +
										 "DECLARE @pathCount int\n" +
										 "DECLARE @identifier varchar(36)\n" +
										 "DECLARE @assetType varchar(100)\n" +
										 "DECLARE @assetName varchar(100)\n" +
										 "DECLARE @parentPath varchar(100)\n" +
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

		triggers = SQLUtil.tokenize(fileVersionsCheckTrigger +
				   contentVersionsCheckTrigger +
				   linkVersionsCheckTrigger +
				   containerVersionsCheckTrigger +
				   templateVersionsCheckTrigger +
				   htmlpageVersionsCheckTrigger +
				   parentPathCheckTrigger +
				   checkChildAssetsTrigger);

		return triggers;
	}
	private List<String> newTriggersForMySql(){
		List<String> triggers = new ArrayList<String>();
		String parentPathCheckWhenUpdate =  "DROP TRIGGER IF EXISTS check_parent_path_when_update;\n"+
                            		        "CREATE TRIGGER check_parent_path_when_update  BEFORE UPDATE\n"+
                            		        "on identifier\n"+
                            		        "FOR EACH ROW\n"+
                            		        "BEGIN\n"+
                            		        "DECLARE idCount INT;\n"+
                            		        "DECLARE canUpdate boolean default false;\n"+
                            		        " IF @disable_trigger IS NULL THEN\n"+
                            		        "   select count(id)into idCount from identifier where asset_type='folder' and CONCAT(parent_path,asset_name,'/')= NEW.parent_path and host_inode = NEW.host_inode and id <> NEW.id;\n"+
                            		        "   IF(idCount > 0 OR NEW.parent_path = '/' OR NEW.parent_path = '/System folder') THEN\n"+
                            		        "     SET canUpdate := TRUE;\n"+
                            		        "   END IF;\n"+
                            		        "   IF(canUpdate = FALSE) THEN\n"+
                            		        "     delete from Cannot_update_for_this_path_does_not_exist_for_the_given_host;\n"+
                            		        "   END IF;\n"+
                            		        " END IF;\n"+
                            		        "END\n"+
                            		        "#\n";

		String parentPathCheckWhenInsert =  "DROP TRIGGER IF EXISTS check_parent_path_when_insert;\n"+
                            		        "CREATE TRIGGER check_parent_path_when_insert  BEFORE INSERT\n"+
                            		        "on identifier\n"+
                            		        "FOR EACH ROW\n"+
                            		        "BEGIN\n"+
                            		        "DECLARE idCount INT;\n"+
                            		        "DECLARE canInsert boolean default false;\n"+
                            		        " select count(id)into idCount from identifier where asset_type='folder' and CONCAT(parent_path,asset_name,'/')= NEW.parent_path and host_inode = NEW.host_inode and id <> NEW.id;\n"+
                            		        " IF(idCount > 0 OR NEW.parent_path = '/' OR NEW.parent_path = '/System folder') THEN\n"+
                            		        "   SET canInsert := TRUE;\n"+
                            		        " END IF;\n"+
                            		        " IF(canInsert = FALSE) THEN\n"+
                            		        "  delete from Cannot_insert_for_this_path_does_not_exist_for_the_given_host;\n"+
                            		        " END IF;\n"+
                            		        "END\n"+
                            		        "#\n";

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
							    "END\n" +
							    "#\n";

		String fileVersionsCheck = "DROP TRIGGER IF EXISTS check_file_versions;\n" +
								   "CREATE TRIGGER check_file_versions BEFORE DELETE\n" +
								   "on file_asset\n" +
								   "FOR EACH ROW\n" +
								   "BEGIN\n" +
								   "DECLARE tableName VARCHAR(20);\n" +
								   "DECLARE count INT;\n" +
								   "SET tableName = 'file_asset';\n" +
								   "CALL checkVersions(OLD.identifier,tableName,count);\n" +
								   "IF(count = 0)THEN\n" +
								   "delete from identifier where id = OLD.identifier;\n" +
								   "END IF;\n" +
								   "END\n" +
								   "#\n";
		String htmlpageVersionsCheck = "DROP TRIGGER IF EXISTS check_htmlpage_versions;\n" +
									   "CREATE TRIGGER check_htmlpage_versions BEFORE DELETE\n" +
									   "on htmlpage\n" +
									   "FOR EACH ROW\n" +
									   "BEGIN\n" +
									   "DECLARE tableName VARCHAR(20);\n" +
									   "DECLARE count INT;\n" +
									   "SET tableName = 'htmlpage';\n" +
									   "CALL checkVersions(OLD.identifier,tableName,count);\n" +
									   "IF(count = 0)THEN\n" +
									   "delete from identifier where id = OLD.identifier;\n" +
									   "END IF;\n" +
									   "END\n"+
									   "#\n";
		String linksVersionsCheck = "DROP TRIGGER IF EXISTS check_links_versions;\n" +
									"CREATE TRIGGER check_links_versions BEFORE DELETE\n" +
									"on links\n" +
									"FOR EACH ROW\n" +
									"BEGIN\n" +
									"DECLARE tableName VARCHAR(20);\n" +
									"DECLARE count INT;\n" +
									"SET tableName = 'links';\n" +
									"CALL checkVersions(OLD.identifier,tableName,count);\n" +
									"IF(count = 0)THEN\n" +
									"delete from identifier where id = OLD.identifier;\n" +
									"END IF;\n" +
									"END\n" +
									"#\n";
		String containerVersionsCheck = "DROP TRIGGER IF EXISTS check_container_versions;\n" +
										"CREATE TRIGGER check_container_versions BEFORE DELETE\n" +
										"on containers\n" +
										"FOR EACH ROW\n" +
										"BEGIN\n" +
										"DECLARE tableName VARCHAR(20);\n" +
										"DECLARE count INT;\n" +
										"SET tableName = 'containers';\n" +
										"CALL checkVersions(OLD.identifier,tableName,count);\n" +
										"IF(count = 0)THEN\n" +
										"delete from identifier where id = OLD.identifier;\n" +
										"END IF;\n" +
										"END\n"+
										"#\n";
		String templateVersionsCheck = "DROP TRIGGER IF EXISTS check_template_versions;\n" +
									   "CREATE TRIGGER check_template_versions BEFORE DELETE\n" +
									   "on template\n" +
									   "FOR EACH ROW\n" +
									   "BEGIN\n" +
									   "DECLARE tableName VARCHAR(20);\n" +
									   "DECLARE count INT;\n" +
									   "SET tableName = 'template';\n" +
									   "CALL checkVersions(OLD.identifier,tableName,count);\n" +
									   "IF(count = 0)THEN\n" +
									   "delete from identifier where id = OLD.identifier;\n" +
									   "END IF;\n" +
									   "END\n"+
									   "#\n";
		String contentVersionsCheck = "DROP TRIGGER IF EXISTS check_content_versions;\n" +
									  "CREATE TRIGGER check_content_versions BEFORE DELETE\n" +
									  "on contentlet\n" +
									  "FOR EACH ROW\n" +
									  "BEGIN\n" +
									  "DECLARE tableName VARCHAR(20);\n" +
									  "DECLARE count INT;\n" +
									  "SET tableName = 'contentlet';\n" +
									  "CALL checkVersions(OLD.identifier,tableName,count);\n" +
									  "IF(count = 0)THEN\n" +
									  "delete from identifier where id = OLD.identifier;\n" +
									  "END IF;\n" +
									  "END\n"+
									  "#\n";
		String checkChildAssets =     "DROP TRIGGER IF EXISTS check_child_assets;\n" +
									  "CREATE TRIGGER check_child_assets BEFORE DELETE\n" +
									  "ON IDENTIFIER\n" +
									  "FOR EACH ROW\n" +
									  "BEGIN\n" +
										   "DECLARE pathCount INT;\n" +
										      "IF(OLD.asset_type ='folder') THEN\n" +
										 		"select count(*) into pathCount from identifier where parent_path = concat(concat(OLD.parent_path,OLD.asset_name),'/') and host_inode = OLD.host_inode;\n" +
										 	  "END IF;\n" +
										 	  "IF(OLD.asset_type ='contentlet') THEN\n" +
										 	  	"select count(*) into pathCount from identifier where host_inode = OLD.id;\n" +
										 	  "END IF;\n" +
										 	  "IF(pathCount > 0) THEN\n" +
										 	  	" delete from Cannot_delete_as_this_path_has_children;\n" +
										 	  "END IF;\n" +
									  "END\n" +
									  "#\n";
	   triggers = SQLUtil.tokenize(parentPathCheckWhenUpdate
			                       + parentPathCheckWhenInsert
			                       + checkVersions
			                       + fileVersionsCheck
			                       + htmlpageVersionsCheck
			                       + linksVersionsCheck
			                       + containerVersionsCheck
			                       + templateVersionsCheck
			                       + contentVersionsCheck
			                       + checkChildAssets);
	   return triggers;

	}
	private void addNewTriggers(){
		DotConnect dc = new DotConnect();
		List<String> newTriggers = new ArrayList<String>();
		if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
			newTriggers = newTriggersForPostgres();
		}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
			newTriggers = newTriggersForMSSQL();
		}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)){
			newTriggers = newTriggersForOracle();
		}else{
			newTriggers = newTriggersForMySql();
		}
		try {
			for(String trigger :newTriggers){
				dc.executeStatement(trigger);
			}
		} catch (SQLException e) {
		  Logger.error(this, e.getMessage());
		  e.printStackTrace();
		}
	}

	private void contentletChanges() throws DotDataException, SQLException{
		DotConnect dc = new DotConnect();
		String query = "select contentlet.identifier,folder.path from contentlet,folder,identifier where contentlet.folder<>'SYSTEM_FOLDER' " +
				       "and contentlet.folder = folder.inode and contentlet.identifier = identifier.id ";
		String dropFolderColumn ="ALTER TABLE contentlet DROP COLUMN folder";
		String dropFK = "";
		dc.setSQL(query);
		List<Map<String, String>> contentFolders = dc.loadResults();
		for(Map<String, String> contentFolder : contentFolders){
			String identifier = contentFolder.get("identifier");
			String parentPath = contentFolder.get("path");
			dc.setSQL("Update identifier set parent_path=? where id=?");
			dc.addParam(parentPath);
			dc.addParam(identifier);
			dc.loadResult();
		}
		if (DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
			dropFK = "ALTER TABLE contentlet DROP FOREIGN KEY fk_folder";
		}else {
			dropFK  = "Alter table contentlet drop constraint fk_folder";
		}
		dc.executeStatement(dropFK);
		dc.executeStatement(dropFolderColumn);
	}

	private void folderTableChanges() throws SQLException, DotDataException{
		DotConnect dc = new DotConnect();
		String addIdentifierToFolder = "ALTER TABLE Folder add identifier varchar(36)";
		if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE))
		    addIdentifierToFolder=addIdentifierToFolder.replaceAll("varchar\\(", "varchar2\\(");
		String addFK = "ALTER TABLE Folder add constraint folder_identifier_fk foreign key (identifier) references identifier(id)";
		String dropHostColumn = "ALTER TABLE Folder drop column host_inode";
		String folderQuery = "Select * from folder where host_inode in(Select distinct host_inode from identifier) order by host_inode, path";
		String deleteFromFolder = "Delete from folder where host_inode not in (select distinct host_inode from identifier) and inode<>'SYSTEM_FOLDER'";
		String deletePathColumn = "ALTER TABLE Folder drop column path";

		dc.executeStatement(addIdentifierToFolder);
		dc.executeStatement(addFK);

		dc.setSQL(folderQuery);
		List<Map<String, String>> folders = dc.loadResults();

		for(Map<String,String> folder : folders){
			String inode = folder.get("inode").trim();
			String name = folder.get("name");
			String path = folder.get("path");
			String type = "folder";
			String finalPath = "";
			String uuid = UUIDGenerator.generateUuid();

			dc.setSQL("insert into identifier(id,parent_path,asset_name,host_inode,asset_type)values (?,?,?,?,?)");
			dc.addParam(uuid);
			if(path !=null){
				finalPath = path.substring(0, path.lastIndexOf("/"));
				finalPath = finalPath.substring(0, finalPath.lastIndexOf("/")+1);
			}
			if(UtilMethods.isSet(finalPath)){
				dc.addParam(finalPath);
			}
			dc.addParam(name);
			dc.addParam(folder.get("host_inode").trim());
			dc.addParam(type);
			dc.loadResult();

			dc.setSQL("Update folder set identifier =? where inode=?");
			dc.addParam(uuid);
			dc.addParam(inode);
			dc.loadResult();
		}

		dc.setSQL("select * from folder where inode='SYSTEM_FOLDER'");
		if(dc.loadResults().size()>0){
			Map<String,String> folder = (Map<String,String>)dc.loadResults().get(0);
			String inode = folder.get("inode").trim();
			String type = "folder";
			String uuid = UUIDGenerator.generateUuid();
			Logger.info(this, "Executing Insert into identifier(id,parent_path,asset_name,host_inode,asset_type)values ("+uuid+","+folder.get("path")+","+folder.get("name")+","+Host.SYSTEM_HOST+","+type+")");
			dc.setSQL("insert into identifier(id,parent_path,asset_name,host_inode,asset_type)values (?,?,?,?,?)");
			dc.addParam(uuid);
			dc.addParam(folder.get("path"));
			dc.addParam(folder.get("name"));
			dc.addParam(Host.SYSTEM_HOST);
			dc.addParam(type);
			dc.loadResult();

			Logger.info(this, "Executing Update folder set identifier = '"+uuid+"' where inode='"+inode+"'");
			dc.setSQL("Update folder set identifier =? where inode=?");
			dc.addParam(uuid);
			dc.addParam(inode);
			dc.loadResult();
		}
		dc.executeStatement(deleteFromFolder);
		dc.executeStatement(dropHostColumn);
		dc.executeStatement(deletePathColumn);
	}

	public boolean forceRun() {
	  return true;
	}

	public void executeUpgrade() throws DotDataException, DotRuntimeException {
		DotConnect dc = new DotConnect();

		//HibernateUtil.startTransaction();
		try {
		    DbConnectionFactory.getConnection().setAutoCommit(true);
		  if (DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL))
			 dc.executeStatement("SET storage_engine=INNODB");
		  if (DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL))
		     dc.executeStatement("SET TRANSACTION ISOLATION LEVEL READ COMMITTED");

		
		deleteIdentifiersFromInode();
		if(Config.getBooleanProperty("upgrade-cleanup-bad-data",true))
		    deleteOrphanedAssets();
		
		String addConstraint = "";
		String addIdentifierColumn = "alter table containers add identifier varchar(36);" +
			             			 "alter table template add identifier varchar(36);" +
			             		     "alter table htmlpage add identifier varchar(36);"+
			             		     "alter table file_asset add identifier varchar(36);" +
			             			 "alter table contentlet add identifier varchar(36);" +
			             			 "alter table links add identifier varchar(36);";		
		
	    if (DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
	        try {
	            dc.executeStatement("alter table identifier drop foreign key host_inode_fk");
	            dc.executeStatement("alter table identifier drop index host_inode_fk");
	        }
	        catch(Exception ex) {
	            Logger.info(this, "no need to drop host_inode_fk");
	        }
	        addConstraint = "alter table structure drop foreign key fk_structure_host;"+
                    "ALTER TABLE identifier change inode id varchar(36);" +
                    "ALTER TABLE structure ADD CONSTRAINT fk_structure_host FOREIGN KEY (host) REFERENCES identifier (id);"+
                    "ALTER TABLE identifier drop index uri;";
	    }else  if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
		    dc.setSQL("SELECT * FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS where table_name='identifier' and constraint_type<>'FOREIGN KEY'");
		    List<Map<String, String>> results = dc.getResults();
		    for(Map<String, String> key :results){
			   String constraint = key.get("constraint_name");
			   dc.executeStatement("ALTER TABLE identifier DROP CONSTRAINT " +constraint);
		    }
	        addConstraint = "DROP INDEX idx_identifier ON identifier;" +
	      				    "DROP INDEX idx_index_2 ON inode;" +
	      				    "ALTER TABLE identifier add new_inode varchar(36);" +
   		  				  	"UPDATE identifier set new_inode = cast(inode as varchar(36));" +
   		  				  	"ALTER TABLE identifier drop column inode;" +
   		  				  	"EXEC SP_RENAME 'dbo.identifier.new_inode','id','COLUMN';" +
   		  				  	"ALTER TABLE identifier ALTER column id varchar(36) not null;" +
   		  				  	"ALTER TABLE identifier ADD CONSTRAINT identifier_pkey PRIMARY KEY(id);" +
   		  				  	"CREATE INDEX idx_identifier ON identifier(id);";
	    }else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)){
		    addConstraint = "ALTER TABLE identifier add id varchar2(36);" +
	   		  				"UPDATE identifier set id = cast(inode as varchar2(36));" +
	   		  				"ALTER TABLE identifier drop column inode;" +
	   		  			    "ALTER TABLE identifier MODIFY (id NOT NULL);" +
	   		  				"ALTER TABLE identifier ADD CONSTRAINT identifier_pkey PRIMARY KEY(id);" +
	   		  				"ALTER TABLE identifier DROP UNIQUE (uri,host_inode);";
		    addIdentifierColumn=addIdentifierColumn.replaceAll("varchar\\(", "varchar2\\(");
		}else{
		    try {
                dc.executeStatement("alter table identifier drop constraint host_inode_fk");
            } catch(Exception ex) {
                Logger.info(this, "no need to drop host_inode_fk");
            }
		   addConstraint = "ALTER TABLE identifier add id varchar(36);" +
				   		   "UPDATE identifier set id = cast(inode as varchar(36));" +
				   		   "ALTER TABLE identifier drop column inode;" +
				   		   "ALTER TABLE identifier ALTER COLUMN id SET NOT NULL;" +
				   		   "ALTER TABLE identifier ADD CONSTRAINT identifier_pkey PRIMARY KEY(id);" +
		                   "CREATE INDEX idx_identifier ON identifier USING btree (id);";
		}
		addConstraint =  addConstraint+
					     "ALTER TABLE Inode DROP COLUMN identifier;" +
					     "ALTER TABLE structure add constraint fk_structure_host foreign key (host) references identifier(id);"+
					     "ALTER TABLE containers add constraint containers_identifier_fk foreign key (identifier) references identifier(id);" +
					     "ALTER TABLE template add constraint template_identifier_fk foreign key (identifier) references identifier(id);" +
					     "ALTER TABLE htmlpage add constraint htmlpage_identifier_fk foreign key (identifier) references identifier(id);" +
					     "ALTER TABLE file_asset add constraint file_identifier_fk foreign key (identifier) references identifier(id);" +
					     "ALTER TABLE contentlet add constraint content_identifier_fk foreign key (identifier) references identifier(id);" +
					     "ALTER TABLE links add constraint links_identifier_fk foreign key (identifier) references identifier(id);" +
					     "ALTER TABLE identifier add parent_path varchar(255);" +
					     "ALTER TABLE identifier add asset_name varchar(255);" +
					     "ALTER TABLE identifier add asset_type varchar(64);";

		if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE))
		    addConstraint=addConstraint.replaceAll("varchar\\(", "varchar2\\(");

		String dropUriColumn = "ALTER TABLE Identifier DROP COLUMN URI";

        String addUniqueKey = "ALTER TABLE identifier ADD CONSTRAINT identifier_unique_key UNIQUE(parent_path,asset_name,host_inode)";

        List<String> queryList = SQLUtil.tokenize(addIdentifierColumn);
		for(String query : queryList){
		   dc.executeStatement(query);
	    }
		String inode = "";
		String identifier = "";
		List<Map<String, String>> containerList = getAssetIdentifiers("containers");
		for(Map<String,String> container:containerList){
			inode = container.get("inode");
			identifier = container.get("identifier");
			dc.setSQL("Update containers set identifier = ? where inode=?");
			dc.addParam(identifier);
			dc.addParam(inode);
			dc.loadResult();
	    }
		List<Map<String, String>> templateList = getAssetIdentifiers("template");
		for(Map<String,String> template:templateList){
		   inode = template.get("inode");
		   identifier = template.get("identifier");
		   dc.setSQL("Update template set identifier = ? where inode=?");
		   dc.addParam(identifier);
		   dc.addParam(inode);
		   dc.loadResult();
	    }
		List<Map<String,String>> htmlpageList = getAssetIdentifiers("htmlpage");
		for(Map<String,String> htmlpage:htmlpageList){
			inode = htmlpage.get("inode");
			identifier = htmlpage.get("identifier");
			dc.setSQL("Update htmlpage set identifier =? where inode=?");
			dc.addParam(identifier);
			dc.addObject(inode);
			dc.loadResult();
		}
		List<Map<String,String>> file_assetList = getAssetIdentifiers("file_asset");
		for(Map<String,String> file_asset:file_assetList){
			inode = file_asset.get("inode");
			identifier = file_asset.get("identifier");
			dc.setSQL("Update file_asset set identifier =? where inode=?");
			dc.addParam(identifier);
			dc.addObject(inode);
			dc.loadResult();
		}
		List<Map<String,String>> contentletList = getAssetIdentifiers("contentlet");
		for(Map<String,String> contentlet:contentletList){
			inode = contentlet.get("inode");
			identifier = contentlet.get("identifier");
			dc.setSQL("Update contentlet set identifier =? where inode=?");
			dc.addParam(identifier);
			dc.addObject(inode);
			dc.loadResult();
		}
		List<Map<String,String>> linksList = getAssetIdentifiers("links");
		for(Map<String,String> links:linksList){
			inode = links.get("inode");
			identifier = links.get("identifier");
			dc.setSQL("Update links set identifier =? where inode=?");
			dc.addParam(identifier);
			dc.addObject(inode);
			dc.loadResult();
		}
		List<String> constraintList = SQLUtil.tokenize(addConstraint);
		for(String constraint : constraintList){
		  	dc.executeStatement(constraint);
		}

		if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)) {
			// the MSSQL version of this trigger creates a cursor without
			// setting its scope. So if the default is GLOBAL then this upgrade task will fail
			dc.setSQL("DROP TRIGGER check_identifier_host_inode");
			dc.loadResult();
		}

		String uri = "";
		String assetType ="";
		String assetName ="";
		String parentPath = "";
		dc.setSQL("SELECT * from identifier order by uri");
		List<Map<String, String>> identifierData = dc.loadResults();
		for(Map<String,String> iden :identifierData){
		   uri = iden.get("uri");
		   String ident = iden.get("id").trim();
		   if(uri.contains("content")&& !uri.contains("/")){
			 assetType = "contentlet";
			 parentPath = "/";
			 assetName = ident+".content";
		   }else if(uri.contains("template")&& !uri.contains("/")){
			 assetType="template";
			 parentPath = "/";
			 assetName = ident+".template";
		   }else if(uri.contains("containers")&& !uri.contains("/")){
			 assetType = "containers";
			 parentPath = "/";
			 assetName = ident+".containers";
		   }else if(UtilMethods.getFileExtension(uri).equals("dot")){
			 assetType = "htmlpage";
			 parentPath = uri.substring(0, uri.lastIndexOf("/")+1);
			 assetName = uri.substring(uri.lastIndexOf("/")+1);
		   }else if(UtilMethods.getFileExtension(uri)!="" && !UtilMethods.getFileExtension(uri).equals("dot")){
			 assetType="file_asset";
			 parentPath = uri.substring(0, uri.lastIndexOf("/")+1);
			 assetName = uri.substring(uri.lastIndexOf("/")+1);
		   }else if(ident.equals("SYSTEM_HOST")){
			   assetType = "contentlet";
			   parentPath = "/";
			   assetName = null;
		   }else{
			 assetType = "links";
			 parentPath = uri.substring(0, uri.lastIndexOf("/")+1);
			 assetName = uri.substring(uri.lastIndexOf("/")+1);
		   }
		   dc.setSQL("UPDATE identifier set parent_path =?, asset_name=?, asset_type=? where id=?");
		   dc.addParam(parentPath);
		   dc.addParam(assetName);
		   dc.addParam(assetType);
		   dc.addParam(iden.get("id"));
		   dc.loadResult();
		}
		contentletChanges();

		if(DbConnectionFactory.isMsSql())
		    dropUniqueURIHOSTI();

		folderTableChanges();
		dc.executeStatement(dropUriColumn);
		dc.executeStatement(addUniqueKey);

		if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)||
				 DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE) ||
				   		DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
			triggersChanges();  //Update existing triggers
		}
		addNewTriggers(); //Add New Triggers
		dotPathFunction(); // Add function dotFolderPath
	  } catch (SQLException e) {
		 //HibernateUtil.rollbackTransaction();
		 Logger.error(this, e.getMessage());
	 }

	 //HibernateUtil.commitTransaction();
	 CacheLocator.getCacheAdministrator().flushAll();
	 MaintenanceUtil.flushCache();
	 MaintenanceUtil.deleteStaticFileStore();
  }

	protected void dropUniqueURIHOSTI() {
	    try {
    	    Connection conn = DbConnectionFactory.getConnection();
    	    DatabaseMetaData dbmd=conn.getMetaData();
    	    ResultSet idxrs = dbmd.getIndexInfo(conn.getCatalog(), null, "IDENTIFIER", false, false);
    	    while(idxrs.next()) {
    	        String cn=idxrs.getString("COLUMN_NAME");
    	        if(cn!=null && cn.equalsIgnoreCase("uri")) {
    	            String constraint=idxrs.getString("INDEX_NAME");
    	            String statement="DROP INDEX identifier."+constraint;
    	            DotConnect dc=new DotConnect();
    	            dc.executeStatement(statement);
    	            break;
    	        }
    	    }
	    }
	    catch(Exception ex) {
	        Logger.warn(this, "can't drop unque index on identifier(uri,host_inode): "+ex.getMessage(),ex);
	    }
	}
}