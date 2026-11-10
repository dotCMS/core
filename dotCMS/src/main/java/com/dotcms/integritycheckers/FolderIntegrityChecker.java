package com.dotcms.integritycheckers;

import com.dotcms.repackage.com.csvreader.CsvReader;
import com.dotcms.repackage.com.csvreader.CsvWriter;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.FlushCacheRunnable;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Folder integrity checker implementation
 * 
 * @author Rogelio Blanco
 * @version 1.0
 * @since 06-10-2015
 * 
 */
public class FolderIntegrityChecker extends AbstractIntegrityChecker {

    @Override
    public final IntegrityType getIntegrityType() {
        return IntegrityType.FOLDERS;
    }

    @Override
    public File generateCSVFile(final String outputPath) throws DotDataException, IOException {
        final String outputFile = getOutputFilePath(outputPath);

        File csvFile;
        CsvWriter writer = null;

        try {
            csvFile = new File(outputFile);
            writer = new CsvWriter(new FileWriter(csvFile, true), '|');

            final Connection conn = DbConnectionFactory.getConnection();
            try (final PreparedStatement statement = conn
                    .prepareStatement("select f.inode, f.identifier, i.parent_path, i.asset_name, i.host_inode from folder f join identifier i on f.identifier = i.id ")) {
                try (final ResultSet rs = statement.executeQuery()) {
                    int count = 0;

                    while (rs.next()) {
                        writer.write(rs.getString("inode"));
                        writer.write(rs.getString("identifier"));
                        writer.write(rs.getString("parent_path"));
                        writer.write(rs.getString("asset_name"));
                        writer.write(rs.getString("host_inode"));
                        writer.endRecord();
                        count++;

                        if (count == 1000) {
                            writer.flush();
                            count = 0;
                        }
                    }
                }
            } catch (final SQLException e) {
                throw new DotDataException(String.format("An error occurred when generating the CSV file for Folders " +
                        "to '%s': %s", outputFile, e.getMessage()), e);
            }
        } finally {
            // Close writer
            if (writer != null) {
                writer.close();
            }
        }

        return csvFile;
    }

    @Override
    public boolean generateIntegrityResults(String endpointId) throws Exception {
        try {
            CsvReader folders = new CsvReader(ConfigUtils.getIntegrityPath() + File.separator
                    + endpointId + File.separator + getIntegrityType().getDataToCheckCSVName(),
                    '|', StandardCharsets.UTF_8);

            boolean tempCreated = false;
            DotConnect dc = new DotConnect();
            final String tempTableName = getTempTableName(endpointId);

            // lets create a temp table and insert all the records coming from
            // the CSV file
            final String tempKeyword = DbConnectionFactory.getTempKeyword();

            String createTempTable = "create "
                    + tempKeyword
                    + " table "
                    + tempTableName
                    + " (inode varchar(36) not null, "
                    + " identifier varchar(36) not null,"
                    + " full_path_lc varchar(510), "
                    + " host_identifier varchar(36) not null, "
                    + " primary key (inode) )"
                    + (DbConnectionFactory.isOracle() ? " ON COMMIT PRESERVE ROWS " : "");

            if (DbConnectionFactory.isOracle()) {
                createTempTable = createTempTable.replaceAll("varchar\\(", "varchar2\\(");
            }

			final String INSERT_TEMP_TABLE = "insert into " + tempTableName + " (inode, identifier, full_path_lc, host_identifier) values(?,?,?,?)";
            while (folders.readRecord()) {

                if (!tempCreated) {
                    dc.executeStatement(createTempTable);
                    tempCreated = true;
                }

				String folderIdentifier = null;
				try {
					folderIdentifier = getStringIfNotBlank("identifier",
							folders.get(1));
					final String folderInode = getStringIfNotBlank("inode",
							folders.get(0));
					final String parentPath = getStringIfNotBlank(
							"parent_path", folders.get(2));
					final String assetName = getStringIfNotBlank("asset_name",
							folders.get(3));
					final String hostIdentifier = getStringIfNotBlank(
							"host_identifier", folders.get(4));
					dc.setSQL(INSERT_TEMP_TABLE);
					dc.addParam(folderInode);
					dc.addParam(folderIdentifier);
                    dc.addParam((parentPath + assetName).toLowerCase());
					dc.addParam(hostIdentifier);
					dc.loadResult();
				} catch (final DotDataException e) {
					folders.close();
					final String assetId = UtilMethods.isSet(folderIdentifier) ? folderIdentifier
							: "";
                    throw new DotDataException(String.format("An error occurred when generating temp table for asset " +
                            "'%s': %s", assetId, e.getMessage()), e);
                }
            }

            folders.close();
            if (!tempCreated) {
            	return false;
            }

            final String hostNameColumnName = dc.setSQL("select f.field_contentlet from field f join structure s on s.inode = f.structure_inode "
                    + "where s.velocity_var_name='Host' and f.velocity_var_name='hostName' ").getString("field_contentlet");

            // compare the data from the CSV to the local db data and see if we
            // have conflicts
            dc.setSQL("select 1 from identifier iden "
                    + "join folder f on iden.id = f.identifier join "
                    + tempTableName
                    + " ft on iden.full_path_lc = ft.full_path_lc "
                    + "join contentlet c on iden.host_inode = c.identifier and ft.host_identifier = iden.host_inode "
                    + "join contentlet_version_info cvi on c.inode = cvi.working_inode "
                    + "where asset_type = 'folder' and f.inode <> ft.inode order by c."+hostNameColumnName+", iden.asset_name");

            final List<Map<String, Object>> results = dc.loadObjectResults();

            if (!results.isEmpty()) {
                // if we have conflicts, lets create a table out of them
                //By default we assume we're on postgres
                String fullFolder = " (COALESCE(c.contentlet_as_json-> 'fields' ->'hostName'->>'value',c."+hostNameColumnName+" ) || iden.parent_path || iden.asset_name) ";

                if (DbConnectionFactory.isMySql()) {
                    //At this point we do not support json on mySQL se we only validate against the dynamic column
                    fullFolder = " concat(c."+hostNameColumnName+",iden.parent_path,iden.asset_name) ";
                } else if (DbConnectionFactory.isMsSql()) {
                    //For msSQL we do support both json and dynamic columns so we need to test against both
                    fullFolder = " (COALESCE(JSON_VALUE(c.contentlet_as_json,'$.fields.hostName.value'),c."+hostNameColumnName+" ) + iden.parent_path + iden.asset_name) ";
                }

                final String INSERT_INTO_RESULTS_TABLE = "insert into "
                        + getIntegrityType().getResultsTableName() 
                        + " (folder, local_inode, remote_inode, local_identifier, remote_identifier, endpoint_id)" 
                        + " select "
                        + fullFolder
                        + " as folder, "
                        + "f.inode as local_inode, ft.inode as remote_inode, f.identifier as local_identifier, ft.identifier as remote_identifier, "
                        + "? from identifier iden "
                        + "join folder f on iden.id = f.identifier join "
                        + tempTableName
                        + " ft on iden.full_path_lc = ft.full_path_lc "
                        + "join contentlet c on iden.host_inode = c.identifier and ft.host_identifier = iden.host_inode "
                        + "join contentlet_version_info cvi on c.inode = cvi.working_inode "
                        + "where asset_type = 'folder' and f.inode <> ft.inode order by c."+hostNameColumnName+", iden.asset_name";

                dc.setSQL(INSERT_INTO_RESULTS_TABLE).addParam(endpointId).loadResult();

            }

            return hasResults(endpointId);
        } catch (final Exception e) {
            throw new Exception(String.format("Error running the Folders Integrity Check for Endpoint '%s': %s",
                    endpointId, e.getMessage()), e);
        }
    }

    /**
     * Fixes folders inconsistencies for a given server id Fixing a folder means
     * updating it's inode and identifier with the ones received from the other
     * end
     *
     * @param key
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Override
    public void executeFix(final String key) throws DotDataException, DotSecurityException {

        final DotConnect dc = new DotConnect();

        try {
            // lets remove from the index all the content under each conflicted
            // folder
            dc.setSQL("select folder, local_inode, remote_inode, local_identifier, remote_identifier from "
                    + getIntegrityType().getResultsTableName() + " where endpoint_id = ?");
            dc.addParam(key);
            final List<Map<String, Object>> results = dc.loadObjectResults();

            for (final Map<String, Object> result : results) {

                final String folder = (String) result.get("folder");
                final String oldFolderInode = (String) result.get("local_inode");
                final String newFolderInode = (String) result.get("remote_inode");
                final String oldFolderIdentifier = (String) result.get("local_identifier");
                final String newFolderIdentifier = (String) result.get("remote_identifier");

                //First we need to verify if the new folder identifier already exist
                final Identifier identifierFound = APILocator.getIdentifierAPI().find(newFolderIdentifier);
                if ( identifierFound != null && UtilMethods.isSet(identifierFound.getId()) ) {

                    final Host site = APILocator.getHostAPI().find(identifierFound.getHostId(),APILocator.systemUser(),false);
                    String fullFolderPath = (site.getName() + "/" + identifierFound.getURI()).replaceAll("//","/");

                    //We need to change the ids of the existing folder
                    String existingFolderNewIdentifier = UUIDGenerator.generateUuid();
                    String existingFolderNewInode = UUIDGenerator.generateUuid();
                    //Since we're now using deterministic or contextual ids which are generated in a predictable way
                    //Finding two folders with identical identifiers at the exact same time and at the exact same location is now a case we need to consider.
                    //Therefore now we need to take into account we're not looking at the same folder when attempting to change an identifier. Prior to applying the fix.
                    if(!fullFolderPath.equals(folder)){
                      //If the identifier already exist on another location we need to change it first before applying the real fix
                      applyFixTo(dc, newFolderInode, existingFolderNewInode, newFolderIdentifier, existingFolderNewIdentifier);
                    }
                }

                applyFixTo(dc, oldFolderInode, newFolderInode, oldFolderIdentifier, newFolderIdentifier);
            }

        } catch (final SQLException e) {
            throw new DotDataException(String.format("An error occurred when executing the Folder Integrity fix for " +
                    "key '%s': %s", key, e.getMessage()), e);
        }
    }

    /**
     * Moves a local folder to the inode and identifier the remote end point uses for it. References
     * are first pointed at a placeholder folder, the old rows are replaced with rows under the new
     * ids, and the references are then pointed at the new inode. Every id is bound as a parameter.
     *
     * @param dc the connection helper to run the statements with
     * @param oldFolderInode the local folder inode
     * @param newFolderInode the inode the folder takes
     * @param oldFolderIdentifier the local folder identifier
     * @param newFolderIdentifier the identifier the folder takes
     * @throws DotSecurityException when the folder cannot be read
     * @throws DotDataException when a statement fails
     * @throws SQLException when a statement fails
     */
    private void applyFixTo ( DotConnect dc, final String oldFolderInode, final String newFolderInode, final String oldFolderIdentifier, final String newFolderIdentifier ) throws DotSecurityException, DotDataException, SQLException {

        final Folder folder;
        try {
            folder = APILocator.getFolderAPI().find(oldFolderInode, APILocator.getUserAPI().getSystemUser(), false);
        } catch ( DotDataException e ) {
            Logger.info(getClass(), "Folder not found. inode: " + oldFolderInode);
            return;
        }

        // THIS IS THE NEW CODE

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

        // 1.1) Insert dummy temp row on IDENTIFIER table

        dc.setSQL("insert into identifier (id, parent_path, asset_name, host_inode, asset_type, syspublish_date, sysexpire_date, create_date, owner) values ('TEMP_IDENTIFIER', '/', 'DUMMY_ASSET_NAME', ?, 'folder', NULL, NULL, NULL, NULL) ")
                .addParam(hostForDummyFolder)
                .loadResult();

        // 1.2) Insert dummy temp row on FOLDER table

        if ( DbConnectionFactory.isOracle() ) {
            // Only the database's boolean keywords are concatenated; values are bound.
            dc.setSQL("insert into folder (inode, name, title, show_on_menu, sort_order, files_masks, identifier, default_file_type, mod_date, owner, idate) values ('TEMP_INODE', 'DUMMY_NAME', 'DUMMY_TITLE', '" // nosemgrep
                    + DbConnectionFactory.getDBFalse()
                    + "', '0', '', 'TEMP_IDENTIFIER', ?, to_date('1900-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'DUMMY_OWNER', to_date('1900-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'))");
        } else if ( DbConnectionFactory.isPostgres() ) {
            // Only the database's boolean keywords are concatenated; values are bound.
            dc.setSQL("insert into folder (inode, name, title, show_on_menu, sort_order, files_masks, identifier, default_file_type, mod_date, owner, idate) values ('TEMP_INODE', 'DUMMY_NAME', 'DUMMY_TITLE', " // nosemgrep
                    + DbConnectionFactory.getDBFalse()
                    + ", '0', '', 'TEMP_IDENTIFIER', ?, '1900-01-01 00:00:00.00', 'DUMMY_OWNER', '1900-01-01 00:00:00.00')");
        } else {
            // Only the database's boolean keywords are concatenated; values are bound.
            dc.setSQL("insert into folder (inode, name, title, show_on_menu, sort_order, files_masks, identifier, default_file_type, mod_date, owner, idate) values ('TEMP_INODE', 'DUMMY_NAME', 'DUMMY_TITLE', '" // nosemgrep
                    + DbConnectionFactory.getDBFalse()
                    + "', '0', '', 'TEMP_IDENTIFIER', ?, '1900-01-01 00:00:00.00', 'DUMMY_OWNER', '1900-01-01 00:00:00.00')");
        }
        dc.addParam(fileAssetSt.getInode()).loadResult();

        // 2) Update references to the new dummies temps

        // update foreign tables references to TEMP
        repointFolderReferences(dc, oldFolderInode, "TEMP_INODE");

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
        String owner = (String) oldFolderRow.get("owner");
        Date idate = (Date) oldFolderRow.get("idate");

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
        final Date createDate = (Date) oldIdentifierRow.get("create_date");
        final String oldOwner = (String) oldIdentifierRow.get("owner");

        // now we can safely delete the old folder row. It will also
        // delete the old Identifier
        // lets alter the asset_name to avoid errors in validation
        // when deleting the folder
        dc.setSQL("update identifier set asset_name = '_TO_BE_DELETED_' where id = ?")
                .addParam(oldFolderIdentifier).loadResult();

        dc.setSQL("delete from folder where inode = ?").addParam(oldFolderInode).loadResult();

        //Deletes the old inode in case it exists
        dc.setSQL("delete from inode where inode = ?").addParam(oldFolderInode).loadResult();

        // 4.1) insert real new IDENTIFIER row
        dc.setSQL("insert into identifier (id, parent_path, asset_name, host_inode, asset_type, syspublish_date, sysexpire_date, create_date, owner) values (?, ?, ?, ?, ?, ?, ?, ?, ?) ");
        dc.addParam(newFolderIdentifier);
        dc.addParam(parentPath);
        dc.addParam(assetName);
        dc.addParam(hostId);
        dc.addParam(assetType);
        dc.addParam(syspublishDate);
        dc.addParam(sysexpireDate);
        dc.addParam(createDate);
        dc.addParam(oldOwner);
        dc.loadResult();

        // 4.2) insert real new FOLDER row
        dc.setSQL("insert into folder (inode, name, title, show_on_menu, sort_order, files_masks, identifier, default_file_type, mod_date, owner, idate) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ");
        dc.addParam(newFolderInode);
        dc.addParam(name);
        dc.addParam(title);
        dc.addParam(showOnMenu);
        dc.addParam(sortOrder);
        dc.addParam(filesMasks);
        dc.addParam(newFolderIdentifier);
        dc.addParam(defaultFileType);
        dc.addParam(modDate);
        dc.addParam(owner);
        dc.addParam(idate);
        dc.loadResult();

        // 5) update foreign tables references to the new real row
        repointFolderReferences(dc, "TEMP_INODE", newFolderInode);

        // 6) delete dummy temp
        dc.executeStatement("delete from folder where inode = 'TEMP_INODE'");

        /*
         Lets reindex all the content under the fixed folder
         */
        HibernateUtil.addCommitListener(new FlushCacheRunnable() {
            public void run () {

                String folderPath = null;
                try {

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

    }

    /**
     * Points the content types, permissions and permission references of one folder inode at
     * another inode.
     *
     * @param dc the connection helper to run the statements with
     * @param fromInode the inode the references hold now
     * @param toInode the inode the references should hold
     * @throws DotDataException when a statement fails
     */
    private static void repointFolderReferences(final DotConnect dc, final String fromInode, final String toInode)
            throws DotDataException {
        dc.setSQL("update structure set folder = ? where folder = ?")
                .addParam(toInode).addParam(fromInode).loadResult();
        dc.setSQL("update permission set inode_id = ? where inode_id = ?")
                .addParam(toInode).addParam(fromInode).loadResult();
        dc.setSQL("update permission_reference set asset_id = ? where asset_id = ?")
                .addParam(toInode).addParam(fromInode).loadResult();
    }

}
