package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

/**
 * Updates the existing stored procedures and/or functions that are in charge of
 * updating the folder names, which are not allowing to change folder names when
 * the absolute path to a folder is greater than 100 characters. When an folder
 * name changes, 2 verifications must be performed:
 * <ol>
 * <li>The folder name <b>cannot be longer than 255 characters</b>.</li>
 * <li>The parent path of any asset or folder <b>cannot be longer than 255
 * characters</b>.</li>
 * </ol>
 * These limitations are given by the specified size in the {@code Identifier}
 * table: {@code parent_path VARCHAR(255)}. This Upgrade Task increments the
 * size of all folder path and asset name variables in the involved stored
 * procedures and functions from "100" to "255".
 * 
 * @author Jose Castro
 * @version 3.5.1
 * @since 04-15-2016
 */
public class Task03545FixVarcharSizeInFolderOperations extends AbstractJDBCStartupTask {

	private final String POSTGRES_SCRIPT = new StringBuilder()
									.append("CREATE OR REPLACE FUNCTION rename_folder_and_assets()\n")
									.append("RETURNS trigger AS '\n")
									.append("DECLARE\n")
										.append("old_parent_path VARCHAR(255);\n")
										.append("old_path VARCHAR(255);\n")
										.append("new_path VARCHAR(255);\n")
										.append("old_name VARCHAR(255);\n")
										.append("hostInode VARCHAR(100);\n")
									.append("BEGIN\n")
										.append("IF (tg_op = ''UPDATE'' AND NEW.name <> OLD.name) THEN\n")
											.append("SELECT asset_name, parent_path, host_inode INTO old_name, old_parent_path, hostInode FROM identifier WHERE id = NEW.identifier;\n")
											.append("old_path := old_parent_path || old_name || ''/'';\n")
											.append("new_path := old_parent_path || NEW.name || ''/'';\n")
											.append("UPDATE identifier SET asset_name = NEW.name WHERE id = NEW.identifier;\n")
											.append("PERFORM renameFolderChildren(old_path, new_path, hostInode);\n")
											.append("RETURN NEW;\n")
										.append("END IF;\n")
										.append("RETURN NULL;\n")
									.append("END\n")
									.append("'LANGUAGE plpgsql;\n")
									.append("CREATE OR REPLACE FUNCTION renameFolderChildren(old_path VARCHAR(255), new_path VARCHAR(255), hostInode VARCHAR(100))\n")
									.append("RETURNS VOID AS '\n")
									.append("DECLARE\n")
										.append("fi identifier;\n")
										.append("new_folder_path VARCHAR(255);\n")
										.append("old_folder_path VARCHAR(255);\n")
									.append("BEGIN\n")
										.append("UPDATE identifier SET parent_path = new_path WHERE parent_path = old_path AND host_inode = hostInode;\n")
										.append("FOR fi IN SELECT * FROM identifier WHERE asset_type = ''folder'' AND parent_path = new_path AND host_inode = hostInode LOOP\n")
											.append("new_folder_path := new_path || fi.asset_name || ''/'';\n")
											.append("old_folder_path := old_path || fi.asset_name || ''/'';\n")
											.append("PERFORM renameFolderChildren(old_folder_path, new_folder_path, hostInode);\n")
										.append("END LOOP;\n")
									.append("END\n")
									.append("'LANGUAGE plpgsql;").toString();
	
	private final String MYSQL_SCRIPT = new StringBuilder()
									.append("DROP TRIGGER IF EXISTS rename_folder_assets_trigger;\n")
									.append("CREATE TRIGGER rename_folder_assets_trigger AFTER UPDATE\n")
									.append("ON folder\n")
									.append("FOR EACH ROW\n")
									.append("BEGIN\n")
										.append("DECLARE old_parent_path VARCHAR(255);\n")
										.append("DECLARE old_path VARCHAR(255);\n")
										.append("DECLARE new_path VARCHAR(255);\n")
										.append("DECLARE old_name VARCHAR(255);\n")
										.append("DECLARE hostInode VARCHAR(100);\n")
										.append("IF @disable_trigger IS NULL AND NEW.name <> OLD.name THEN\n")
											.append("SELECT asset_name, parent_path, host_inode INTO old_name, old_parent_path, hostInode FROM identifier WHERE id = NEW.identifier;\n")
											.append("SELECT CONCAT(old_parent_path, old_name, '/') INTO old_path;\n")
											.append("SELECT CONCAT(old_parent_path, NEW.name, '/') INTO new_path;\n")
											.append("SET @disable_trigger = 1;\n")
											.append("UPDATE identifier SET asset_name = NEW.name WHERE id = NEW.identifier;\n")
											.append("SET @disable_trigger = NULL;\n")
											.append("CALL renameFolderChildren(old_path, new_path, hostInode);\n")
										.append("END IF;\n")
									.append("END\n")
									.append("#\n")
									.append("DROP PROCEDURE IF EXISTS renameFolderChildren;\n")
									.append("CREATE PROCEDURE renameFolderChildren(IN old_path VARCHAR(255), IN new_path VARCHAR(255), IN hostInode VARCHAR(100))\n")
									.append("BEGIN\n")
										.append("DECLARE new_folder_path VARCHAR(255);\n")
										.append("DECLARE old_folder_path VARCHAR(255);\n")
										.append("DECLARE assetName VARCHAR(255);\n")
										.append("DECLARE no_more_rows BOOLEAN;\n")
										.append("DECLARE cur1 CURSOR FOR SELECT asset_name FROM identifier WHERE asset_type = 'folder' AND parent_path = new_path AND host_inode = hostInode;\n")
										.append("DECLARE CONTINUE HANDLER FOR NOT FOUND\n")
										.append("SET no_more_rows := TRUE;\n")
										.append("SET max_sp_recursion_depth = 255;\n")
										.append("SET @disable_trigger = 1;\n")
										.append("UPDATE identifier SET parent_path = new_path WHERE parent_path = old_path AND host_inode = hostInode;\n")
										.append("SET @disable_trigger = NULL;\n")
										.append("OPEN cur1;\n")
										.append("cur1_loop:LOOP\n")
											.append("FETCH cur1 INTO assetName;\n")
											.append("IF no_more_rows THEN\n")
												.append("LEAVE cur1_loop;\n")
											.append("END IF;\n")
											.append("SELECT CONCAT(new_path, assetName, '/') INTO new_folder_path;\n")
											.append("SELECT CONCAT(old_path, assetName, '/') INTO old_folder_path;\n")
											.append("CALL renameFolderChildren(old_folder_path, new_folder_path, hostInode);\n")
										.append("END LOOP;\n")
										.append("CLOSE cur1;\n")
									.append("END\n")
									.append("#").toString();
	
	private final String MSSQL_SCRIPT = new StringBuilder()
									.append("IF EXISTS (SELECT name FROM sys.objects WHERE type = 'TR' AND name = 'rename_folder_assets_trigger')\n")
										.append("DROP TRIGGER rename_folder_assets_trigger;\n")
									.append("GO\n")
									.append("CREATE Trigger rename_folder_assets_trigger\n")
									.append("ON Folder\n")
									.append("FOR UPDATE\n")
									.append("AS\n")
										.append("DECLARE @oldPath VARCHAR(255)\n")
										.append("DECLARE @newPath VARCHAR(255)\n")
										.append("DECLARE @newName VARCHAR(255)\n")
										.append("DECLARE @hostInode VARCHAR(100)\n")
										.append("DECLARE @ident VARCHAR(100)\n")
										.append("DECLARE @folderPathLength INT\n")
										.append("DECLARE @errorMsg VARCHAR(1000)\n")
										.append("DECLARE folder_cur_Updated cursor LOCAL FAST_FORWARD for\n")
											.append("SELECT inserted.identifier, inserted.name\n")
											.append("FROM inserted JOIN deleted ON (inserted.inode = deleted.inode)\n")
											.append("WHERE inserted.name <> deleted.name\n")
											.append("FOR READ ONLY\n")
										.append("OPEN folder_cur_Updated\n")
										.append("FETCH NEXT FROM folder_cur_Updated INTO @ident, @newName\n")
										.append("WHILE @@FETCH_STATUS <> -1\n")
											.append("BEGIN\n")
												.append("SET @folderPathLength = 0\n")
												.append("SELECT @oldPath = parent_path + asset_name + '/', @newPath = parent_path + @newName + '/', @hostInode = host_inode FROM identifier WHERE id = @ident\n")
												.append("SET @folderPathLength = LEN(@newPath)\n")
												.append("IF (@folderPathLength > 255)\n")
													.append("BEGIN\n")
														.append("SET @errorMsg = 'Folder path ' + @newPath + ' is longer than 255 characters'\n")
														.append("RAISERROR (@errorMsg, 16, 1)\n")
														.append("ROLLBACK WORK\n")
														.append("RETURN\n")
													.append("END\n")
												.append("UPDATE identifier SET asset_name = @newName WHERE id = @ident\n")
												.append("EXEC renameFolderChildren @oldPath, @newPath, @hostInode\n")
												.append("FETCH NEXT FROM folder_cur_Updated INTO @ident, @newName\n")
											.append("END;\n")
									.append("GO\n")
									.append("IF EXISTS (SELECT name FROM sys.objects WHERE type = 'P' AND name = 'renameFolderChildren')\n")
										.append("DROP PROCEDURE renameFolderChildren;\n")
									.append("GO\n")
									.append("CREATE PROCEDURE renameFolderChildren @oldPath VARCHAR(255), @newPath VARCHAR(255), @hostInode VARCHAR(100) AS\n")
										.append("DECLARE @newFolderPath VARCHAR(255)\n")
										.append("DECLARE @oldFolderPath VARCHAR(255)\n")
										.append("DECLARE @assetName VARCHAR(255)\n")
										.append("DECLARE @folderPathLength INT\n")
										.append("DECLARE @errorMsg VARCHAR(1000)\n")
										.append("UPDATE identifier SET parent_path = @newPath WHERE parent_path = @oldPath AND host_inode = @hostInode\n")
										.append("DECLARE folder_data_cursor CURSOR LOCAL FAST_FORWARD FOR\n")
											.append("SELECT asset_name FROM identifier WHERE asset_type = 'folder' AND parent_path = @newPath AND host_inode = @hostInode\n")
										.append("OPEN folder_data_cursor\n")
										.append("FETCH NEXT FROM folder_data_cursor INTO @assetName\n")
										.append("WHILE @@FETCH_STATUS <> -1\n")
											.append("BEGIN\n")
												.append("SET @folderPathLength = 0\n")
												.append("SET @newFolderPath = @newPath + @assetName + '/'\n")
												.append("SET @folderPathLength = LEN(@newPath) + LEN(@assetName) + 1\n")
												.append("IF (@folderPathLength > 255)\n")
													.append("BEGIN\n")
														.append("SET @errorMsg = 'Folder path ' + @newPath + @assetName + '/' + ' is longer than 255 characters'\n")
														.append("RAISERROR (@errorMsg, 16, 1)\n")
														.append("ROLLBACK WORK\n")
														.append("RETURN\n")
													.append("END\n")
												.append("SET @oldFolderPath = @oldPath + @assetName + '/'\n")
												.append("EXEC renameFolderChildren @oldFolderPath, @newFolderPath, @hostInode\n")
												.append("FETCH NEXT FROM folder_data_cursor INTO @assetName\n")
										.append("END;").toString();
	
	
	private final String ORACLE_SCRIPT = new StringBuilder()
									.append("CREATE OR REPLACE TRIGGER rename_folder_assets_trigger\n")
										.append("AFTER UPDATE ON Folder\n")
										.append("FOR EACH ROW\n")
									.append("DECLARE\n")
										.append("oldPath VARCHAR2(255);\n")
										.append("newPath VARCHAR2(255);\n")
										.append("hostInode VARCHAR2(100);\n")
									.append("BEGIN\n")
										.append("IF :NEW.name <> :OLD.name THEN\n")
											.append("SELECT parent_path || asset_name || '/', parent_path || :NEW.name || '/', host_inode INTO oldPath, newPath, hostInode FROM identifier WHERE id = :NEW.identifier;\n")
											.append("UPDATE identifier SET asset_name = :NEW.name WHERE id = :NEW.identifier;\n")
											.append("renameFolderChildren(oldPath, newPath, hostInode);\n")
										.append("END IF;\n")
									.append("END;\n")
									.append("/\n")
									.append("CREATE OR REPLACE PROCEDURE renameFolderChildren(oldPath IN VARCHAR2, newPath IN VARCHAR2, hostInode IN VARCHAR2)\n")
									.append("IS\n")
										.append("newFolderPath VARCHAR2(255);\n")
										.append("oldFolderPath VARCHAR2(255);\n")
									.append("BEGIN\n")
										.append("UPDATE identifier SET parent_path = newPath WHERE parent_path = oldPath AND host_inode = hostInode;\n")
										.append("FOR i IN (SELECT * FROM identifier WHERE asset_type = 'folder' AND parent_path = newPath AND host_inode = hostInode) LOOP\n")
											.append("newFolderPath := newPath || i.asset_name || '/';\n")
											.append("oldFolderPath := oldPath || i.asset_name || '/';\n")
											.append("renameFolderChildren(oldFolderPath, newFolderPath, hostInode);\n")
										.append("END LOOP;\n")
									.append("END;\n")
									.append("/").toString();

	@Override
	public boolean forceRun() {
		return true;
	}

	/**
	 * The SQL for Postgres
	 *
	 * @return
	 */
	@Override
	public String getPostgresScript() {
		return POSTGRES_SCRIPT;
	}

	/**
	 * The SQL for MySQL
	 *
	 * @return
	 */
	@Override
	public String getMySQLScript() {
		return MYSQL_SCRIPT;
	}

	/**
	 * The SQL for Oracle
	 *
	 * @return
	 */
	@Override
	public String getOracleScript() {
		return ORACLE_SCRIPT;
	}

	/**
	 * The SQL for MSSQL
	 *
	 * @return
	 */
	@Override
	public String getMSSQLScript() {
		return MSSQL_SCRIPT;
	}

	/**
	 * The SQL for H2. The stored procedure to update is simulated by a Java
	 * class, so there's no SQL for it in H2.
	 *
	 * @return
	 */
	@Override
	public String getH2Script() {
		return "";
	}

	@Override
	protected List<String> getTablesToDropConstraints() {
		return null;
	}

}
