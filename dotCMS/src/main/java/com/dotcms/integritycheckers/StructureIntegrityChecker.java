package com.dotcms.integritycheckers;

import com.dotcms.repackage.com.csvreader.CsvReader;
import com.dotcms.repackage.com.csvreader.CsvWriter;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Structure integrity checker implementation.
 * 
 * @author Rogelio Blanco
 * @version 1.0
 * @since 06-10-2015
 * 
 */
public class StructureIntegrityChecker extends AbstractIntegrityChecker {

    @Override
    public final IntegrityType getIntegrityType() {
        return IntegrityType.STRUCTURES;
    }

    @Override
    public File generateCSVFile(final String outputPath) throws DotDataException, IOException {
        final String outputFile = outputPath + File.separator
                + getIntegrityType().getDataToCheckCSVName();

        File csvFile = null;
        CsvWriter writer = null;

        try {
            csvFile = new File(outputFile);
            writer = new CsvWriter(new FileWriter(csvFile, true), '|');

            Connection conn = DbConnectionFactory.getConnection();
            try (PreparedStatement statement = conn
                    .prepareStatement("select inode, velocity_var_name from structure ")) {
                try (ResultSet rs = statement.executeQuery()) {
                    int count = 0;

                    while (rs.next()) {
                        writer.write(rs.getString("inode"));
                        writer.write(rs.getString("velocity_var_name"));
                        writer.endRecord();
                        count++;

                        if (count == 1000) {
                            writer.flush();
                            count = 0;
                        }
                    }
                }
            } catch (SQLException e) {
                throw new DotDataException(e.getMessage(), e);
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }

        return csvFile;
    }

    @Override
    public boolean generateIntegrityResults(final String endpointId) throws Exception {
        try {
            CsvReader structures = new CsvReader(ConfigUtils.getIntegrityPath() + File.separator
                    + endpointId + File.separator + getIntegrityType().getDataToCheckCSVName(),
                    '|', Charset.forName("UTF-8"));
            boolean tempCreated = false;
            DotConnect dc = new DotConnect();
            String tempTableName = getTempTableName(endpointId);

            String tempKeyword = DbConnectionFactory.getTempKeyword();

            String createTempTable = "create " + tempKeyword + " table " + tempTableName
                    + " (inode varchar(36) not null, velocity_var_name varchar(255) not null, "
                    + " primary key (inode) )";

            if (DbConnectionFactory.isOracle()) {
                createTempTable = createTempTable.replaceAll("varchar\\(", "varchar2\\(");
            }

            final String INSERT_TEMP_TABLE = "insert into " + tempTableName + " (inode, velocity_var_name) values(?,?)";
            while (structures.readRecord()) {

                if (!tempCreated) {
                    dc.executeStatement(createTempTable);
                    tempCreated = true;
                }

                String structureInode = null;
				try {
					structureInode = getStringIfNotBlank("inode",
							structures.get(0));
					final String verVarName = getStringIfNotBlank(
							"velocity_var_name", structures.get(1));
					dc.setSQL(INSERT_TEMP_TABLE);
					dc.addParam(structureInode);
					dc.addParam(verVarName);
					dc.loadResult();
				} catch (DotDataException e) {
					structures.close();
					final String assetId = UtilMethods.isSet(structureInode) ? structureInode
							: "";
					throw new DotDataException(
							"An error occured when generating temp table for asset: "
									+ assetId, e);
				}
            }

            structures.close();
            if (!tempCreated) {
            	return false;
            }

            // compare the data from the CSV to the local db data and see if we
            // have conflicts
            dc.setSQL("select s.velocity_var_name as velocity_name, "
                    + "s.inode as local_inode, st.inode as remote_inode from structure s "
                    + "join " + tempTableName
                    + " st on s.velocity_var_name = st.velocity_var_name and s.inode <> st.inode");

            List<Map<String, Object>> results = dc.loadObjectResults();

            if (!results.isEmpty()) {
                // if we have conflicts, lets create a table out of them
                String INSERT_INTO_RESULTS_TABLE = "insert into "
                        + getIntegrityType().getResultsTableName() 
                        + " (velocity_name, local_inode, remote_inode, endpoint_id)" 
                        + " select s.velocity_var_name as velocity_name, "
                        + "s.inode as local_inode, st.inode as remote_inode, '"
                        + endpointId
                        + "' from structure s "
                        + "join "
                        + tempTableName
                        + " st on s.velocity_var_name = st.velocity_var_name and s.inode <> st.inode";

                dc.executeStatement(INSERT_INTO_RESULTS_TABLE);
            }

            return !results.isEmpty();
        } catch (Exception e) {
            throw new Exception("Error running the Structures Integrity Check", e);
        }
    }

    /**
     * Fixes structures inconsistencies for a given server id
     *
     * @param remoteIP
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Override
    public void executeFix(String remoteIP) throws DotDataException, DotSecurityException {
        DotConnect dc = new DotConnect();

        try {
            dc.setSQL("select local_inode, remote_inode from "
                    + getIntegrityType().getResultsTableName() + " where endpoint_id = ?");
            dc.addParam(remoteIP);
            List<Map<String, Object>> results = dc.loadObjectResults();
            User systemUser = APILocator.getUserAPI().getSystemUser();
            
            for (Map<String, Object> result : results) {
                final String oldStructureInode = (String) result.get("local_inode");
                final String newStructureInode = (String) result.get("remote_inode");

                Structure st = CacheLocator.getContentTypeCache().getStructureByInode(oldStructureInode);

                // Inconsistency fix process
                final String TEMP_INODE = "TEMP_INODE_" + System.currentTimeMillis();
                
                // 1.1) Insert dummy temp row on INODE table
                if (DbConnectionFactory.isOracle()) {
                    dc.executeStatement("insert into inode (inode, owner, idate, type) values ('" + TEMP_INODE + "', 'DUMMY_OWNER', to_date('1900-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'DUMMY_TYPE') " );
                } else {
                    dc.executeStatement("insert into inode (inode, owner, idate, type) values ('" + TEMP_INODE + "', 'DUMMY_OWNER', '1900-01-01 00:00:00.00', 'DUMMY_TYPE') " );
                }

                // 1.2) Insert dummy temp row on STRUCTURE table

                if (DbConnectionFactory.isOracle()) {
                    dc.executeStatement("insert into structure (inode, name, description, default_structure, review_interval, reviewer_role, page_detail, structuretype, system, fixed, velocity_var_name, url_map_pattern, host, folder, expire_date_var, publish_date_var, mod_date) values ('" + TEMP_INODE +"', 'DUMMY_NAME', 'DUMMY_DESC', '"
                            + DbConnectionFactory.getDBFalse()
                            + "', '', '', '', 1, '"
                            + DbConnectionFactory.getDBTrue()
                            + "', '"
                            + DbConnectionFactory.getDBFalse()
                            + "', 'DUMMY_VAR_NAME'"
                            + ", 'DUMMY_PATERN', '"
                            + st.getHost()
                            + "', '"
                            + st.getFolder()
                            + "', 'EXPIRE_DUMMY', 'PUBLISH_DUMMY', to_date('1900-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'))");
                } else if (DbConnectionFactory.isPostgres()) {
                    dc.executeStatement("insert into structure (inode, name, description, default_structure, review_interval, reviewer_role, page_detail, structuretype, system, fixed, velocity_var_name, url_map_pattern, host, folder, expire_date_var, publish_date_var, mod_date) values ('" + TEMP_INODE + "', 'DUMMY_NAME', 'DUMMY_DESC', "
                            + DbConnectionFactory.getDBFalse()
                            + ", '', '', '', 1, "
                            + DbConnectionFactory.getDBTrue()
                            + ", "
                            + DbConnectionFactory.getDBFalse()
                            + ", 'DUMMY_VAR_NAME'"
                            + ", 'DUMMY_PATERN', '"
                            + st.getHost()
                            + "', '"
                            + st.getFolder()
                            + "', 'EXPIRE_DUMMY', 'PUBLISH_DUMMY', '1900-01-01 00:00:00.00')");
                } else {
                    dc.executeStatement("insert into structure (inode, name, description, default_structure, review_interval, reviewer_role, page_detail, structuretype, system, fixed, velocity_var_name, url_map_pattern, host, folder, expire_date_var, publish_date_var, mod_date) values ('" + TEMP_INODE + "', 'DUMMY_NAME', 'DUMMY_DESC', '"
                            + DbConnectionFactory.getDBFalse()
                            + "', '', '', '', 1, '"
                            + DbConnectionFactory.getDBTrue()
                            + "', '"
                            + DbConnectionFactory.getDBFalse()
                            + "', 'DUMMY_VAR_NAME'"
                            + ", 'DUMMY_PATERN', '"
                            + st.getHost()
                            + "', '"
                            + st.getFolder()
                            + "', 'EXPIRE_DUMMY', 'PUBLISH_DUMMY', '1900-01-01 00:00:00.00')");
                }

                // 2) Update references to the new dummies temps

                // update foreign tables references to TEMP
                dc.executeStatement("update container_structures set structure_id = '" + TEMP_INODE + "' where structure_id = '"
                        + oldStructureInode + "'" );
                dc.executeStatement("update contentlet set structure_inode = '" + TEMP_INODE + "' where structure_inode = '"
                        + oldStructureInode + "'" );
                dc.executeStatement("update field set structure_inode = '" + TEMP_INODE + "' where structure_inode = '"
                        + oldStructureInode + "'" );
                dc.executeStatement("update relationship set parent_structure_inode = '" + TEMP_INODE + "' where parent_structure_inode = '"
                        + oldStructureInode + "'" );
                dc.executeStatement("update relationship set child_structure_inode = '" + TEMP_INODE + "' where child_structure_inode = '"
                        + oldStructureInode + "'" );
                dc.executeStatement("update workflow_scheme_x_structure set structure_id = '" + TEMP_INODE + "' where structure_id = '"
                        + oldStructureInode + "'" );
                dc.executeStatement("update permission set inode_id = '" + TEMP_INODE + "' where inode_id = '"
                        + oldStructureInode + "'" );
                dc.executeStatement("update permission_reference set asset_id = '" + TEMP_INODE + "' where asset_id = '"
                        + oldStructureInode + "'" );
				// Update references in Folder regarding default file type and
				// remove folders from cache
				dc.setSQL("SELECT inode FROM folder WHERE default_file_type = '"
						+ oldStructureInode + "'");
				List<Map<String, Object>> referencedFolders = dc
						.loadObjectResults();
				if (!referencedFolders.isEmpty()) {

					dc.executeStatement("UPDATE folder SET default_file_type = '"
							+ TEMP_INODE + "' WHERE default_file_type = '"
							+ oldStructureInode + "'");
				}

                // 3.1) delete old STRUCTURE row
                // lets save old structure columns values first
                dc.setSQL("select * from structure where inode = ?");
                dc.addParam(oldStructureInode);
                Map<String, Object> oldFolderRow = dc.loadObjectResults().get(0);
                String name = (String) oldFolderRow.get("name");
                String description = (String) oldFolderRow.get("description");
                Boolean defaultStructure = DbConnectionFactory.isDBTrue(oldFolderRow.get(
                        "default_structure").toString());
                String reviewInterval = (String) oldFolderRow.get("review_interval");
                String reviewerRole = (String) oldFolderRow.get("reviewer_role");
                String detailPage = (String) oldFolderRow.get("page_detail");

                Integer structureType = null;
                if (oldFolderRow.get("structuretype") != null) {
                    structureType = Integer.valueOf(oldFolderRow.get("structuretype").toString());
                }

                Boolean system = DbConnectionFactory
                        .isDBTrue(oldFolderRow.get("system").toString());
                Boolean fixed = DbConnectionFactory.isDBTrue(oldFolderRow.get("fixed").toString());
                String velocityVarName = (String) oldFolderRow.get("velocity_var_name");
                String urlMapPattern = (String) oldFolderRow.get("url_map_pattern");
                String host = (String) oldFolderRow.get("host");
                String folder = (String) oldFolderRow.get("folder");
                String expireDateVar = (String) oldFolderRow.get("expire_date_var");
                String publishDateVar = (String) oldFolderRow.get("publish_date_var");
                Date modDate = (Date) oldFolderRow.get("mod_date");

                dc.executeStatement("delete from structure where inode = '" + oldStructureInode
                        + "'");

                // 3.2) delete old INODE row

                dc.setSQL("select * from inode where inode = ?");
                dc.addParam(oldStructureInode);
                Map<String, Object> oldInodeRow = dc.loadObjectResults().get(0);
                String owner = (String) oldInodeRow.get("owner");
                Date idate = (Date) oldInodeRow.get("idate");
                String type = (String) oldInodeRow.get("type");

                dc.executeStatement("delete from inode where inode = '" + oldStructureInode + "'");

                // 4.1) insert real new INODE row
                dc.setSQL("insert into inode (inode, owner, idate, type) values (?, ?, ?, ?) ");
                dc.addParam(newStructureInode);
                dc.addParam(owner);
                dc.addParam(idate);
                dc.addParam(type);
                dc.loadResult();

                // 4.2) insert real new STRUCTURE row
                dc.setSQL("insert into structure (inode, name, description, default_structure, review_interval, reviewer_role, page_detail, structuretype, system, fixed, velocity_var_name, url_map_pattern, host, folder, expire_date_var, publish_date_var, mod_date) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ");
                dc.addParam(newStructureInode);
                dc.addParam(name);
                dc.addParam(description);
                dc.addParam(defaultStructure);
                dc.addParam(reviewInterval);
                dc.addParam(reviewerRole);
                dc.addParam(detailPage);
                dc.addParam(structureType);
                dc.addParam(system);
                dc.addParam(fixed);
                dc.addParam(velocityVarName);
                dc.addParam(urlMapPattern);
                dc.addParam(host);
                dc.addParam(folder);
                dc.addParam(expireDateVar);
                dc.addParam(publishDateVar);
                dc.addParam(modDate);
                dc.loadResult();

                // 5) update foreign tables references to the new real row
                dc.executeStatement("update container_structures set structure_id = '"
                        + newStructureInode + "' where structure_id = '" + TEMP_INODE + "'" );
                dc.executeStatement("update contentlet set structure_inode = '" + newStructureInode
                        + "' where structure_inode = '" + TEMP_INODE + "'" );
                dc.executeStatement("update field set structure_inode = '" + newStructureInode
                        + "' where structure_inode = '" + TEMP_INODE + "'" );
                dc.executeStatement("update relationship set parent_structure_inode = '"
                        + newStructureInode + "' where parent_structure_inode = '" + TEMP_INODE + "'" );
                dc.executeStatement("update relationship set child_structure_inode = '"
                        + newStructureInode + "' where child_structure_inode = '" + TEMP_INODE + "'" );
                dc.executeStatement("update workflow_scheme_x_structure set structure_id = '"
                        + newStructureInode + "' where structure_id = '" + TEMP_INODE + "'" );
                dc.executeStatement("update permission set inode_id = '" + newStructureInode
                        + "' where inode_id = '" + TEMP_INODE + "'" );
                dc.executeStatement("update permission_reference set asset_id = '"
                        + newStructureInode + "' where asset_id = '" + TEMP_INODE + "'" );
                if (!referencedFolders.isEmpty()) {
					dc.executeStatement("UPDATE folder SET default_file_type = '"
							+ newStructureInode + "' WHERE default_file_type = '"
							+ TEMP_INODE + "'");
                }

                // 6) delete dummy temp
                dc.executeStatement("delete from structure where inode = '" + TEMP_INODE + "'" );
                dc.executeStatement("delete from inode where inode = '" + TEMP_INODE + "'" );
            }
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

}
