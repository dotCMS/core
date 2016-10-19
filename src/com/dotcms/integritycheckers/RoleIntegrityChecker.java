package com.dotcms.integritycheckers;

import com.dotcms.repackage.com.csvreader.CsvReader;
import com.dotcms.repackage.com.csvreader.CsvWriter;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.repackage.org.apache.commons.lang.mutable.MutableInt;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.FlushCacheRunnable;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Role integrity checker implementation
 * 
 */
public class RoleIntegrityChecker extends AbstractIntegrityChecker {

    private static class CmsRole {
    	private String roleId;
    	private String roleKey;
    	private String roleName;
    	private String qualifiedId;    	
    	private String qualifiedName;

    	public CmsRole(String roleId, String roleKey, String roleName, CmsRole parent) {
    		this.roleId = roleId;
    		this.roleKey = roleKey;
    		this.roleName = roleName;

    		if (parent == null) {
    			this.qualifiedId = roleId;
    			this.qualifiedName = roleName;
    		} else {
    			this.qualifiedId = parent.getQualifiedId() +"-->"+ roleId;
    			this.qualifiedName = parent.getQualifiedName() +"-->"+ roleName;
    		}
    	}

    	public CmsRole(String roleId, String roleKey, String roleName, String qualifiedId, String qualifiedName) {
    		this.roleId = roleId;
    		this.roleKey = roleKey;
    		this.roleName = roleName;

			this.qualifiedId = qualifiedId;
			this.qualifiedName = qualifiedName;
    	}


		public String getRoleId() {
			return roleId;
		}

		public String getRoleKey() {
			return roleKey;
		}

		public String getRoleName() {
			return roleName;
		}

		public String getQualifiedId() {
			return qualifiedId;
		}

		public String getQualifiedName() {
			return qualifiedName;
		}
    }

    @Override
    public final IntegrityType getIntegrityType() {
        return IntegrityType.FOLDERS;
    }

    @Override
    public File generateCSVFile(final String outputPath) throws DotDataException, IOException {
        final String outputFile = outputPath + File.separator
                + getIntegrityType().getDataToCheckCSVName();

        File csvFile = new File(outputFile);
        try {
        	CsvWriter writer = new CsvWriter(new FileWriter(csvFile, true), '|');

        	try {
        		MutableInt count = new MutableInt(0);

        		queryAndConsumeRoles(role -> {
        			try {
	        			writer.write(role.getRoleId());
	        			writer.write(role.getRoleKey());
	        			writer.write(role.getRoleName());
	        			writer.write(role.getQualifiedId());
	        			writer.write(role.getQualifiedName());
	
	                    writer.endRecord();
	
	                    count.increment();
	                    if ((count.intValue() % 500) == 0) {
	                        writer.flush();
	                    }
        			} catch(IOException e) {
        				throw new RuntimeException(e);
        			}
        		});

        		writer.flush();
            } finally {
            	writer.close();
            }
        } catch(Exception e) {
        	throw e;
        }

        return csvFile;
    }

    @Override
    public boolean generateIntegrityResults(String endpointId) throws Exception {
        try {
            final DotConnect dc = new DotConnect();

            // Create a temp table and insert all roles coming from local db
            final String NAME_TEMP_TABLE_LOCAL = getTempTableName(endpointId, "local");
			final String INSERT_TEMP_TABLE_LOCAL = getTempTableInsertStatement(NAME_TEMP_TABLE_LOCAL);

			dc.executeStatement(getTempTableCreateStatement(NAME_TEMP_TABLE_LOCAL));

            queryAndConsumeRoles(role -> {
				try {
					dc.setSQL(INSERT_TEMP_TABLE_LOCAL);

					dc.addParam(role.getRoleId());
					dc.addParam(role.getRoleKey());
					dc.addParam(role.getRoleName());
					dc.addParam(role.getQualifiedId());
					dc.addParam(role.getQualifiedName());

					dc.loadResult();
				} catch (DotDataException e) {
					throw new RuntimeException("An error occured when generating local temp table for role: "+ role.getRoleId(), e);
				}
            });


            // Create a temp table and insert all roles coming from remote CSV file
            final String NAME_TEMP_TABLE_REMOTE = getTempTableName(endpointId, "remote");
			final String INSERT_TEMP_TABLE_REMOTE = getTempTableInsertStatement(NAME_TEMP_TABLE_REMOTE);

			dc.executeStatement(getTempTableCreateStatement(NAME_TEMP_TABLE_REMOTE));

            CsvReader roles = new CsvReader(ConfigUtils.getIntegrityPath() + File.separator
                    + endpointId + File.separator + getIntegrityType().getDataToCheckCSVName(),
                    '|', Charset.forName("UTF-8"));
            while (roles.readRecord()) {
				try {
					CmsRole role = new CmsRole(roles.get(0), roles.get(1), roles.get(2), roles.get(3), roles.get(4));

					dc.setSQL(INSERT_TEMP_TABLE_REMOTE);
					dc.addParam(role.getRoleId());
					dc.addParam(role.getRoleKey());
					dc.addParam(role.getRoleName());
					dc.addParam(role.getQualifiedId());
					dc.addParam(role.getQualifiedName());

					dc.loadResult();
				} catch (DotDataException e) {
					throw new DotDataException("An error occured when generating remote temp table for role: "+ roles.get(0), e);
				}
            }


            // Compare roles from local and remote temp table and see if there are any conflicts
            dc.setSQL("select 1 from "+NAME_TEMP_TABLE_LOCAL +" tr1 join "+NAME_TEMP_TABLE_REMOTE +" tr2 on tr1.role_key = tr2.role_key "
                    + "where tr1.qualified_name = tr2.qualified_name and tr1.qualified_id <> tr2.qualified_id");

            List<Map<String, Object>> results = dc.loadObjectResults();

            if (!results.isEmpty()) {
                // If there are conflicts, create a table with the results

                final String INSERT_INTO_RESULTS_TABLE = "insert into "
                	+ getIntegrityType().getResultsTableName() 
                	+ " (name, role_key, local_role_id, remote_role_id, local_role_fqn, remote_role_fqn, endpoint_id)"
                	+ " select tr1.qualified_name as name, tr1.role_key as role_key, tr1.id as local_role_id, tr2.id as remote_role_id,"
                	+	" tr1.local_role_fqn as local_role_fqn, tr2.remote_role_fqn as remote_role_fqn,"
                	+ " '"+ endpointId+ "'"
                	+ " from "+NAME_TEMP_TABLE_LOCAL +" tr1 join "+NAME_TEMP_TABLE_REMOTE +" tr2 on tr1.role_key = tr2.role_key"
                    + " where tr1.qualified_name = tr2.qualified_name and tr1.qualified_id <> tr2.qualified_id order by name asc";

                dc.executeStatement(INSERT_INTO_RESULTS_TABLE);

            }

            return (Long) dc.getRecordCount(getIntegrityType().getResultsTableName()) > 0;
        } catch (Exception e) {
            throw new Exception("Error running the Folders Integrity Check", e);
        }
    }

	private String getTempTableCreateStatement(String tempTableName) {
		String createTempTableSql = "create "
		        + DbConnectionFactory.getTempKeyword()
		        + " table "
		        + tempTableName
		        + " (id varchar(36) not null, role_name varchar(255) not null, role_key varchar(255) not null, "
		        + "qualified_id varchar(1000) not null, qualified_name varchar(1000) not null, primary key (id) )"
		        + (DbConnectionFactory.isOracle() ? " ON COMMIT PRESERVE ROWS " : "");

		if (DbConnectionFactory.isOracle()) {
			createTempTableSql = createTempTableSql.replaceAll("varchar\\(", "varchar2\\(");
		}
		return createTempTableSql;
	}

	private String getTempTableInsertStatement(String tempTableName) {
		return "insert into " + tempTableName + " (id, role_name, role_key, qualified_id, qualified_name) values(?,?,?,?,?)";
	}

    private void queryAndConsumeRoles(Consumer<CmsRole> consumer) throws DotDataException {
    	String query = "select id, role_key, role_name, parent, db_fqn from cms_role order by db_fqn asc";

        Connection conn = DbConnectionFactory.getConnection();
        try (PreparedStatement statement = conn.prepareStatement(query)) {
            try (ResultSet rs = statement.executeQuery()) {

            	Stack<CmsRole> currentRolePath = new Stack<>();

                while (rs.next()) {
                	// Read role attributes from database
                	String roleId = rs.getString("id");
                	String roleKey = StringUtils.defaultIfEmpty(rs.getString("role_key"), "");
                	String roleName = rs.getString("role_name");
                	String roleParent = rs.getString("parent");

                	// Resolve parent
                	CmsRole parent = null;
                	while(!currentRolePath.isEmpty()) {
                		if (currentRolePath.peek().getRoleId().equals(roleParent)) {
                			parent = currentRolePath.peek();
                			break;
                		}
            			currentRolePath.pop();
                	}

                	// Create, register and consume role
                	CmsRole role = new CmsRole(roleId, roleKey, roleName, parent);
                	currentRolePath.push(role);
                	consumer.accept(role);
                }
            }
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }    	
    }
    

    
    /**
     * Fixes folders inconsistencies for a given server id Fixing a folder means
     * updating it's inode and identifier with the ones received from the other
     * end
     *
     * @param serverId
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Override
    public void executeFix(final String serverId) throws DotDataException, DotSecurityException {
/*
        DotConnect dc = new DotConnect();

        try {
            // lets remove from the index all the content under each conflicted
            // folder
            dc.setSQL("select local_inode, remote_inode, local_identifier, remote_identifier from "
                    + getIntegrityType().getResultsTableName() + " where endpoint_id = ?");
            dc.addParam(serverId);
            List<Map<String, Object>> results = dc.loadObjectResults();

            for (Map<String, Object> result : results) {

                String oldFolderInode = (String) result.get("local_inode");
                String newFolderInode = (String) result.get("remote_inode");
                String oldFolderIdentifier = (String) result.get("local_identifier");
                String newFolderIdentifier = (String) result.get("remote_identifier");

                //First we need to verify if the new folder identifier already exist
                Identifier identifierFound = APILocator.getIdentifierAPI().find(newFolderIdentifier);
                if ( identifierFound != null && UtilMethods.isSet(identifierFound.getId()) ) {

                    //We need to change the ids of the existing folder
                    String existingFolderNewIdentifier = UUIDGenerator.generateUuid();
                    String existingFolderNewInode = UUIDGenerator.generateUuid();

                    //If the identifier already exist on another location we need to change it first before to apply the real fix
                    applyFixTo(dc, newFolderInode, existingFolderNewInode, newFolderIdentifier, existingFolderNewIdentifier);
                }

                applyFixTo(dc, oldFolderInode, newFolderInode, oldFolderIdentifier, newFolderIdentifier);
            }

        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
*/
    }

    private void applyFixTo ( DotConnect dc, final String oldFolderInode, final String newFolderInode, final String oldFolderIdentifier, final String newFolderIdentifier ) throws DotSecurityException, SQLException {
/*
        try {

            final Folder folder = APILocator.getFolderAPI().find(oldFolderInode, APILocator.getUserAPI().getSystemUser(), false);

            // Clean up the caches
            List<Contentlet> contents = APILocator.getContentletAPI().findContentletsByFolder(folder, APILocator.getUserAPI().getSystemUser(), false);
            for ( Contentlet contentlet : contents ) {
                APILocator.getContentletIndexAPI().removeContentFromIndex(contentlet);
                CacheLocator.getContentletCache().remove(contentlet.getInode());
            }

            Identifier folderIdentifier = APILocator.getIdentifierAPI().find(folder.getIdentifier());
            CacheLocator.getFolderCache().removeFolder(folder, folderIdentifier);
            CacheLocator.getIdentifierCache().removeFromCacheByIdentifier(oldFolderIdentifier);
            CacheLocator.getIdentifierCache().removeFromCacheByInode(oldFolderInode);

            // THIS IS THE NEW CODE

            // 1.1) Insert dummy temp row on INODE table
            if ( DbConnectionFactory.isOracle() ) {
                dc.executeStatement("insert into inode (inode, owner, idate, type) values ('TEMP_INODE', 'DUMMY_OWNER', to_date('1900-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'DUMMY_TYPE') ");
            } else {
                dc.executeStatement("insert into inode (inode, owner, idate, type) values ('TEMP_INODE', 'DUMMY_OWNER', '1900-01-01 00:00:00.00', 'DUMMY_TYPE') ");
            }

            Structure fileAssetSt = CacheLocator.getContentTypeCache()
                    .getStructureByVelocityVarName("FileAsset");

            // lets see if we have structures referencing the folder, if
            // so, let's use its host for the dummy identifier
            List<Structure> referencedStructures = APILocator.getFolderAPI().getStructures(
                    folder, APILocator.getUserAPI().getSystemUser(), false);
            String hostForDummyFolder = "SYSTEM_HOST";

            if ( referencedStructures != null && !referencedStructures.isEmpty() ) {
                Structure st = referencedStructures.get(0);
                hostForDummyFolder = st.getHost();
            }

            // 1.2) Insert dummy temp row on IDENTIFIER table

            dc.executeStatement("insert into identifier (id, parent_path, asset_name, host_inode, asset_type, syspublish_date, sysexpire_date) values ('TEMP_IDENTIFIER', '/System folder', 'DUMMY_ASSET_NAME', '"
                    + hostForDummyFolder + "', " + "'folder', NULL, NULL) ");

            // 1.3) Insert dummy temp row on FOLDER table

            if ( DbConnectionFactory.isOracle() ) {
                dc.executeStatement("insert into folder (inode, name, title, show_on_menu, sort_order, files_masks, identifier, default_file_type, mod_date) values ('TEMP_INODE', 'DUMMY_NAME', 'DUMMY_TITLE', '"
                        + DbConnectionFactory.getDBFalse()
                        + "', '0', '', 'TEMP_IDENTIFIER', '"
                        + fileAssetSt.getInode()
                        + "', to_date('1900-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'))");
            } else if ( DbConnectionFactory.isPostgres() ) {
                dc.executeStatement("insert into folder (inode, name, title, show_on_menu, sort_order, files_masks, identifier, default_file_type, mod_date) values ('TEMP_INODE', 'DUMMY_NAME', 'DUMMY_TITLE', "
                        + DbConnectionFactory.getDBFalse()
                        + ", '0', '', 'TEMP_IDENTIFIER', '"
                        + fileAssetSt.getInode()
                        + "', '1900-01-01 00:00:00.00')");
            } else {
                dc.executeStatement("insert into folder (inode, name, title, show_on_menu, sort_order, files_masks, identifier, default_file_type, mod_date) values ('TEMP_INODE', 'DUMMY_NAME', 'DUMMY_TITLE', '"
                        + DbConnectionFactory.getDBFalse()
                        + "', '0', '', 'TEMP_IDENTIFIER', '"
                        + fileAssetSt.getInode()
                        + "', '1900-01-01 00:00:00.00')");
            }

            // 2) Update references to the new dummies temps

            // update foreign tables references to TEMP
            dc.executeStatement("update structure set folder = 'TEMP_INODE' where folder = '"
                    + oldFolderInode + "'");
            dc.executeStatement("update permission set inode_id = 'TEMP_INODE' where inode_id = '"
                    + oldFolderInode + "'");
            dc.executeStatement("update permission_reference set asset_id = 'TEMP_INODE' where asset_id = '"
                    + oldFolderInode + "'");

            // 3.1) delete old FOLDER row
            // lets save old folder columns values first
            dc.setSQL("select * from folder where inode = ?");
            dc.addParam(oldFolderInode);
            Map<String, Object> oldFolderRow = dc.loadObjectResults().get(0);
            String name = (String) oldFolderRow.get("name");
            String title = (String) oldFolderRow.get("title");
            Boolean showOnMenu = DbConnectionFactory.isDBTrue(oldFolderRow.get(
                    "show_on_menu").toString());

            Integer sortOrder = 0;
            if ( oldFolderRow.get("sort_order") != null ) {
                sortOrder = Integer.valueOf(oldFolderRow.get("sort_order").toString());
            }

            String filesMasks = (String) oldFolderRow.get("files_masks");
            String defaultFileType = (String) oldFolderRow.get("default_file_type");
            Date modDate = (Date) oldFolderRow.get("mod_date");

            // lets save old identifier columns values first
            dc.setSQL("select * from identifier where id = ?");
            dc.addParam(oldFolderIdentifier);
            Map<String, Object> oldIdentifierRow = dc.loadObjectResults().get(0);
            final String parentPath = (String) oldIdentifierRow.get("parent_path");
            final String assetName = (String) oldIdentifierRow.get("asset_name");
            final String hostId = (String) oldIdentifierRow.get("host_inode");
            String assetType = (String) oldIdentifierRow.get("asset_type");
            Date syspublishDate = (Date) oldIdentifierRow.get("syspublish_date");
            Date sysexpireDate = (Date) oldIdentifierRow.get("sysexpire_date");

            // now we can safely delete the old folder row. It will also
            // delete the old Identifier
            // lets alter the asset_name to avoid errors in validation
            // when deleting the folder
            dc.executeStatement("update identifier set asset_name = '_TO_BE_DELETED_' where id = '"
                    + oldFolderIdentifier + "'");

            dc.executeStatement("delete from folder where inode = '" + oldFolderInode + "'");

            // 3.2) delete old INODE row

            dc.setSQL("select * from inode where inode = ?");
            dc.addParam(oldFolderInode);
            Map<String, Object> oldInodeRow = dc.loadObjectResults().get(0);
            String owner = (String) oldInodeRow.get("owner");
            Date idate = (Date) oldInodeRow.get("idate");
            String type = (String) oldInodeRow.get("type");

            dc.executeStatement("delete from inode where inode = '" + oldFolderInode + "'");

            // 4.1) insert real new INODE row
            dc.setSQL("insert into inode (inode, owner, idate, type) values (?, ?, ?, ?) ");
            dc.addParam(newFolderInode);
            dc.addParam(owner);
            dc.addParam(idate);
            dc.addParam(type);
            dc.loadResult();

            // 4.2) insert real new IDENTIFIER row
            dc.setSQL("insert into identifier (id, parent_path, asset_name, host_inode, asset_type, syspublish_date, sysexpire_date) values (?, ?, ?, ?, ?, ?, ?) ");
            dc.addParam(newFolderIdentifier);
            dc.addParam(parentPath);
            dc.addParam(assetName);
            dc.addParam(hostId);
            dc.addParam(assetType);
            dc.addParam(syspublishDate);
            dc.addParam(sysexpireDate);
            dc.loadResult();

            // 4.3) insert real new FOLDER row
            dc.setSQL("insert into folder (inode, name, title, show_on_menu, sort_order, files_masks, identifier, default_file_type, mod_date) values (?, ?, ?, ?, ?, ?, ?, ?, ?) ");
            dc.addParam(newFolderInode);
            dc.addParam(name);
            dc.addParam(title);
            dc.addParam(showOnMenu);
            dc.addParam(sortOrder);
            dc.addParam(filesMasks);
            dc.addParam(newFolderIdentifier);
            dc.addParam(defaultFileType);
            dc.addParam(modDate);
            dc.loadResult();

            // 5) update foreign tables references to the new real row
            dc.executeStatement("update structure set folder = '" + newFolderInode
                    + "' where folder = 'TEMP_INODE'");
            dc.executeStatement("update permission set inode_id = '" + newFolderInode
                    + "' where inode_id = 'TEMP_INODE'");
            dc.executeStatement("update permission_reference set asset_id = '"
                    + newFolderInode + "' where asset_id = 'TEMP_INODE'");

            // 6) delete dummy temp
            dc.executeStatement("delete from folder where inode = 'TEMP_INODE'");
            dc.executeStatement("delete from inode where inode = 'TEMP_INODE'");

            // Lets reindex all the content under the fixed folder
            HibernateUtil.addCommitListener(new FlushCacheRunnable() {
                public void run () {

                    String folderPath = null;
                    try {

                        //Cleaning the cache for the new ids
                        CacheLocator.getIdentifierCache().removeFromCacheByIdentifier(newFolderIdentifier);
                        CacheLocator.getIdentifierCache().removeFromCacheByInode(newFolderInode);

                        //In oder to avoid duplicated code: Create a dummy Identifier object in order to use the getPath method logic
                        Identifier dummyIdentifier = new Identifier();
                        dummyIdentifier.setAssetName(assetName);
                        dummyIdentifier.setParentPath(parentPath);

                        folderPath = dummyIdentifier.getPath();
                        APILocator.getContentletAPI().refreshContentUnderFolderPath(hostId, folderPath);
                    } catch ( Exception e ) {
                        if ( folderPath != null ) {
                            Logger.error(this, "Error while reindexing content under folder with path [" + folderPath + "].", e);
                        } else {
                            Logger.error(this, "Error while reindexing content under folder.", e);
                        }
                    }
                }
            });

        } catch ( DotDataException e ) {
            Logger.info(getClass(), "Folder not found. inode: " + oldFolderInode);
        }
*/
    }
}