package com.dotcms.integritycheckers;

import com.dotcms.http.DotExecutionException;
import com.dotcms.repackage.com.csvreader.CsvReader;
import com.dotcms.repackage.com.csvreader.CsvWriter;
import com.dotcms.rest.IntegrityResource;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.FlushCacheRunnable;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.MaintenanceUtil;
import com.dotmarketing.util.UtilMethods;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * During the push publish process, user structures such as Folders, Content
 * Types (f.k.a Structures), Schemes, Legacy Pages, or Content Pages, might
 * already exist in a given receiver environment. This will cause the publishing
 * process to fail given that the structures seem to be the same, BUT the
 * identifier is different compared to each other.
 * <p>
 * This utility class provides a mechanism to check and resolve situations where
 * one or more of those user structures have conflicts. The solution is to
 * generate a list of conflicts for the users the check it out, and make a
 * decision. If they want to fix the conflicts, the system will make sure the
 * user structures have the same Identifier (and sometimes the Inode) in both
 * sender and receiver servers.
 * </p>
 * <p>
 * The {@link IntegrityResource} class exposes the main methods of this class as
 * REST services.
 * </p>
 *
 * @author Daniel Silva
 * @version 1.5
 * @since 06-23-2014
 *
 */
public class IntegrityUtil {

    public static final String INTEGRITY_DATA_STATUS = "status";
    public static final String INTEGRITY_DATA_ERROR_MESSAGE = "error_message";
    public static final String INTEGRITY_DATA_TO_CHECK_ZIP_FILENAME = "DataToCheck.zip";
    public static final String INTEGRITY_DATA_TO_FIX_ZIP_FILENAME = "DataToFix.zip";
    public static final String INTEGRITY_DATA_STATUS_FILENAME = "DataStatus.properties";
    public static final String REQUESTER_ENDPOINT = "requesterEndpoint";
    public static final String INTEGRITY_DATA_REQUEST_ID = "integrityDataRequestId";

    private File generateDataToFixCSV(String outputPath, String endpointId, IntegrityType type)
            throws DotDataException, IOException {
        File csvFile = null;
        CsvWriter writer = null;

        try {
            final String outputFile = outputPath + File.separator + type.getDataToFixCSVName();
            csvFile = new File(outputFile);
            writer = new CsvWriter(new FileWriter(csvFile, true), '|');

            String resultsTable = type.getResultsTableName();
            if (!type.hasResultsTable()) {
                throw new DotDataException("Integrity type =[" + type
                        + "] does not support this method, because results table is not available.");
            }

            StringBuilder sbSelectTempTable = new StringBuilder();
            switch(type) {
            	case HTMLPAGES:
            	case FILEASSETS:
            		sbSelectTempTable.append("select ").append(type.getFirstDisplayColumnLabel()).append(
            			", remote_working_inode, local_working_inode, remote_live_inode, local_live_inode, remote_identifier, local_identifier, language_id from "
            		);
                    break;
            	case FOLDERS:
            		sbSelectTempTable.append(
            			"select remote_inode, local_inode, remote_identifier, local_identifier from "
            		);
            		break;
            	case CMS_ROLES:
            		sbSelectTempTable.append(
            			"select name, role_key, remote_role_id, local_role_id, local_role_fqn, remote_role_fqn from "
                	);
            		break;
            	default:
            		sbSelectTempTable.append("select remote_inode, local_inode from ");
                    break;
            }
            sbSelectTempTable.append(resultsTable).append(" where endpoint_id = ?");
            
            Connection conn = DbConnectionFactory.getConnection();
            PreparedStatement statement = conn.prepareStatement(sbSelectTempTable.toString());
            statement.setString(1, endpointId);
            try (ResultSet rs = statement.executeQuery()) {
                int count = 0;

                while (rs.next()) {
                	if (type == IntegrityType.CMS_ROLES) {
                        writer.write(rs.getString("name"));
                        writer.write(rs.getString("role_key"));

                        writer.write(rs.getString("remote_role_id"));
                        writer.write(rs.getString("local_role_id"));
                        writer.write(rs.getString("remote_role_fqn"));
                        writer.write(rs.getString("local_role_fqn"));

                	} else if (type == IntegrityType.HTMLPAGES || type == IntegrityType.FILEASSETS) {
                        writer.write(rs.getString("remote_working_inode"));
                        writer.write(rs.getString("local_working_inode"));
                        writer.write(rs.getString("remote_live_inode"));
                        writer.write(rs.getString("local_live_inode"));
                        
                        writer.write(rs.getString("remote_identifier"));
                        writer.write(rs.getString("local_identifier"));
                        
                        writer.write(rs.getString(type.getFirstDisplayColumnLabel()));
                        writer.write(rs.getString("language_id"));
                    } else {
                        writer.write(rs.getString("remote_inode"));
                        writer.write(rs.getString("local_inode"));
                    }

                    if (type == IntegrityType.FOLDERS) {
                        writer.write(rs.getString("remote_identifier"));
                        writer.write(rs.getString("local_identifier"));
                    }

                    writer.endRecord();
                    count++;

                    if (count == 1000) {
                        writer.flush();
                        count = 0;
                    }
                }
            } finally {
                try {
                    statement.close();
                } catch (Exception e) {}
            }
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        } finally {
            if (writer != null)
                writer.close();
        }

        return csvFile;
    }

    private static void addToZipFile(String fileName, ZipOutputStream zos, String zipEntryName)
            throws Exception {
        try {
            Logger.info(IntegrityUtil.class, "Writing '" + fileName + "' to zip file");

            File file = new File(fileName);
            try (InputStream fis = Files.newInputStream(file.toPath())) {
                ZipEntry zipEntry = new ZipEntry(zipEntryName);
                zos.putNextEntry(zipEntry);

                byte[] bytes = new byte[1024];
                int length;
                while ((length = fis.read(bytes)) >= 0) {
                    zos.write(bytes, 0, length);
                }

                zos.closeEntry();
            } catch (FileNotFoundException f) {
                Logger.error(IntegrityUtil.class, "Could not find file " + fileName, f);
                throw new Exception("Could not find file " + fileName, f);
            } catch (IOException e) {
                Logger.error(IntegrityUtil.class, "Error writing file to zip: " + fileName, e);
                throw new Exception("Error writing file to zip: " + fileName, e);
            }
        } catch (NullPointerException npe) {
            Logger.error(IntegrityUtil.class, "File name cannot be NULL", npe);
            throw new Exception("File name cannot be NULL", npe);
        }
    }

    public static void unzipFile(InputStream zipFile, String outputDir) throws Exception {
        File dir = new File(outputDir);

        // if file doesn't exists, then create it
        if (!dir.exists()) {
            dir.mkdir();
        }
        
        ZipInputStream zin = null;
        OutputStream os = null;
        
        try {
            
            ZipEntry ze = null;
            zin = new ZipInputStream(zipFile);
            while ((ze = zin.getNextEntry()) != null) {
                
             // for each entry to be extracted
                int bytesRead;
                byte[] buf = new byte[1024];
                
                Logger.info(IntegrityUtil.class, "Unzipping " + ze.getName());

                os = Files.newOutputStream(Paths.get(outputDir + File.separator + ze.getName()));

                while ( (bytesRead = zin.read( buf, 0, 1024 )) > -1 )
                    os.write( buf, 0, bytesRead );
                try {
                    if ( null != os ) {
                        os.close();
                    }
                } catch ( Exception e ) {
                    Logger.warn( IntegrityUtil.class, "Error Closing Stream.", e );
                }
            }
        } catch (IOException e) {
            Logger.error(IntegrityUtil.class, "Error while unzipping Integrity Data", e);
            throw new Exception("Error while unzipping Integrity Data", e);
        } finally { // close your streams
            if ( zin != null ) {
                try {
                    zin.close();
                } catch ( IOException e ) {
                    Logger.warn( IntegrityUtil.class, "Error Closing Stream.", e );
                }
            }
            if ( os != null ) {
                try {
                    os.close();
                } catch ( IOException e ) {
                    Logger.warn( IntegrityUtil.class, "Error Closing Stream.", e );
                }
            }
        }
    }

    /**
     * Creates a String representing the integrity data path based on provided endpointId.
     *
     * @param endpointId endpoint if
     * @return integrity data path
     */
    public static String getIntegrityDataPath(final String endpointId) {
        return ConfigUtils.getIntegrityPath() + File.separator + endpointId;
    }

    /**
     * Creates a String representing the integrity data file path based on provided endpointId and the file name.
     * File names to be used: DataToCheck.zip, DataToFix.zip and DataStatus.properties.
     *
     * @param endpointId endpoint gid
     * @param dataFile data filename
     * @return path to filename
     */
    public static String getIntegrityDataFile(final String endpointId, final String dataFile) {
        return getIntegrityDataPath(endpointId) + File.separator + dataFile;
    }

    /**
     * Tells whether a integrity file exists based on provided endpointId and the file name.
     *
     * @param endpointId endpoint id
     * @param dataFile data filename
     * @return path to filename
     */
    public static boolean doesIntegrityDataFileExist(final String endpointId, final String dataFile) {
        return new File(getIntegrityDataFile(endpointId, dataFile)).exists();
    }

    /**
     * Gets a {@link IntegrityDataExecutionMetadata} instance based on provided endpoint id which contains metadata of the
     * integrity data generation execution.
     *
     * @param endpointId endpoint id
     * @return Optional wrapping the integrity generation  metadata
     */
    public static Optional<IntegrityDataExecutionMetadata> getIntegrityMetadata(final String endpointId) {
        final File statusFile = new File(getIntegrityDataFile(endpointId, INTEGRITY_DATA_STATUS_FILENAME));
        if (!statusFile.exists()) {
            return Optional.empty();
        }

        final Properties statusData = new Properties();
        try {
            statusData.load(new FileInputStream(statusFile));
        } catch (IOException e) {
            Logger.error(
                    IntegrityUtil.class,
                    String.format("Could load status from %s", statusFile.getAbsolutePath()),
                    e);
            return Optional.empty();
        }

        return Optional.of(new IntegrityDataExecutionMetadata(
                statusData.getProperty(REQUESTER_ENDPOINT),
                statusData.getProperty(INTEGRITY_DATA_REQUEST_ID),
                statusData.getProperty(INTEGRITY_DATA_STATUS),
                statusData.getProperty(INTEGRITY_DATA_ERROR_MESSAGE)));
    }

    /**
     * Saves a integrity data generation metadata in a to-be-discovered location so it can be read by concurrent parts
     * that need to know what is the status of the integrity data generation.
     * The data is saved as {@link Properties} file.
     *
     * @param integrityDataExecutionMetadata execution metadata
     */
    private static void saveIntegrityDataStatus(IntegrityDataExecutionMetadata integrityDataExecutionMetadata) {
        final String endpointId = integrityDataExecutionMetadata.getEndpointId();
        final File integrityDir = new File(getIntegrityDataPath(endpointId));
        if (!integrityDir.exists()) {
            integrityDir.mkdir();
        }

        final File statusFile = new File(getIntegrityDataFile(endpointId, INTEGRITY_DATA_STATUS_FILENAME));
        if (statusFile.exists()) {
            statusFile.delete();
        }

        final Properties statusData = new Properties();
        final BiConsumer<String, String> addData = (data, name) -> {
            if (StringUtils.isNotBlank(data)) {
                statusData.setProperty(name, data);
            }
        };

        addData.accept(endpointId, REQUESTER_ENDPOINT);
        addData.accept(integrityDataExecutionMetadata.getRequestId(), INTEGRITY_DATA_REQUEST_ID);
        statusData.setProperty(INTEGRITY_DATA_STATUS, integrityDataExecutionMetadata.getProcessStatus().toString().toUpperCase());
        addData.accept(integrityDataExecutionMetadata.getErrorMessage(), INTEGRITY_DATA_ERROR_MESSAGE);

        try (FileOutputStream output = new FileOutputStream(statusFile)) {
            statusData.store(output, null);
        } catch (IOException e) {
            throw new DotExecutionException(
                    String.format("Could not save status to %s", statusFile.getAbsolutePath()),
                    e);
        }
    }

    /**
     * Saves a integrity data generation metadata in a to-be-discovered location so it can be read by concurrent parts
     * that need to know what is the status of the integrity data generation.
     * Individual parameters are used to create a {@link IntegrityDataExecutionMetadata}.
     *
     * @param endpointId endpoint id
     * @param requestId request id
     * @param processStatus {@link IntegrityResource.ProcessStatus} instance to reflect the current status
     * @param errorMessage error message associated to detected error
     */
    public static void saveIntegrityDataStatus(final String endpointId,
                                               final String requestId,
                                               final IntegrityResource.ProcessStatus processStatus,
                                               final String errorMessage) {
        saveIntegrityDataStatus(new IntegrityDataExecutionMetadata(endpointId, requestId, processStatus, errorMessage));
    }

    /**
     * Saves a integrity data generation metadata in a to-be-discovered location so it can be read by concurrent parts
     * that need to know what is the status of the integrity data generation.
     * Individual parameters are used to create a {@link IntegrityDataExecutionMetadata}.
     *
     * @param endpointId endpoint id
     * @param requestId request id
     * @param processStatus {@link IntegrityResource.ProcessStatus} instance to reflect the current status
     */
    public static void saveIntegrityDataStatus(final String endpointId,
                                               final String requestId,
                                               final IntegrityResource.ProcessStatus processStatus) {
        saveIntegrityDataStatus(endpointId, requestId, processStatus, null);
    }

    /**
     * Creates all the CSV from End Point database table and store them inside
     * zip file.
     *
     * @param endpointId
     * @throws Exception
     */
    public static void generateDataToCheckZip(final String endpointId) {
        if (!UtilMethods.isSet(endpointId)) {
            Logger.error(IntegrityUtil.class, "Endpoint was not provided");
            return;
        }

        Logger.info(
                IntegrityUtil.class,
                String.format("Starting integrity data generation job for endpoint %s", endpointId));

        File zipFile = null;
        try {
            final String outputPath = getIntegrityDataPath(endpointId);
            final File dir = new File(outputPath);
            // if file doesn't exist, create it
            if (!dir.exists()) {
                dir.mkdir();
            }

            zipFile = new File(getIntegrityDataFile(endpointId, INTEGRITY_DATA_TO_CHECK_ZIP_FILENAME));
            try(final OutputStream os = Files.newOutputStream(zipFile.toPath());
                final ZipOutputStream zos = new ZipOutputStream(os)) {
                final IntegrityType[] types = IntegrityType.values();
                for (IntegrityType integrityType : types) {
                    File fileToCheckCsvFile = null;

                    try {
                        fileToCheckCsvFile = integrityType.getIntegrityChecker().generateCSVFile(outputPath);
                        addToZipFile(fileToCheckCsvFile.getAbsolutePath(), zos, integrityType.getDataToCheckCSVName());
                    } finally {
                        if (fileToCheckCsvFile != null && fileToCheckCsvFile.exists()) {
                            fileToCheckCsvFile.delete();
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (zipFile != null && zipFile.exists()) {
                zipFile.delete();
            }

            throw new DotExecutionException(e);
        }
    }

    public void generateDataToFixZip(final String endpointId, final IntegrityType type) {
        if (!UtilMethods.isSet(endpointId)) {
            return;
        }

        File dataToFixCsvFile = null;
        File zipFile = null;
        try {
            final String outputPath = getIntegrityDataPath(endpointId);
            final File dir = new File(outputPath);
            // if file doesn't exist, create it
            if (!dir.exists()) {
                dir.mkdir();
            }

            zipFile = new File(getIntegrityDataFile(endpointId, INTEGRITY_DATA_TO_FIX_ZIP_FILENAME));
            try (OutputStream outputStream = Files.newOutputStream(zipFile.toPath());
                 ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
                // create Folders CSV
                dataToFixCsvFile = generateDataToFixCSV(outputPath, endpointId, type);

                addToZipFile(dataToFixCsvFile.getAbsolutePath(), zipOutputStream, type.getDataToFixCSVName());
            }
        } catch (Exception e) {
            Logger.error(getClass(), "Error generating fix for remote", e);
            if (zipFile != null && zipFile.exists())
                zipFile.delete();
        } finally {
            if (dataToFixCsvFile != null && dataToFixCsvFile.exists())
                dataToFixCsvFile.delete();
        }
    }

    public void dropTempTables(final String endpointId) throws DotDataException {
        DotConnect dc = new DotConnect();
        try {
            IntegrityType[] types = IntegrityType.values();
            for (IntegrityType integrityType : types) {
            	for(String tempTableName : integrityType.getIntegrityChecker().getTempTableNames(endpointId)) {
                    if (doesTableExist(tempTableName)) {
                        dc.executeStatement("truncate table " + tempTableName);
                        dc.executeStatement("drop table " + tempTableName);
                    }else if (DbConnectionFactory.isMySql()){
                        dc.executeStatement("drop table if exists " + tempTableName);
                    } else if (DbConnectionFactory.isMsSql()){
                        dc.executeStatement("IF OBJECT_ID('tempdb.dbo." + tempTableName + "', 'U') IS NOT NULL"
                                + "  DROP TABLE " + tempTableName);
                    }
            	}
            }
        } catch (SQLException e) {
            Logger.error(getClass(), "Error dropping Temp tables");
            throw new DotDataException("Error dropping Temp tables", e);
        }
    }

    public List<Map<String, Object>> getIntegrityConflicts(String endpointId, IntegrityType type)
            throws Exception {
        try {
            DotConnect dc = new DotConnect();

            final String resultsTableName = type.getResultsTableName();
            if (!type.hasResultsTable() || !doesTableExist(resultsTableName)) {
                return new ArrayList<Map<String, Object>>();
            }

            dc.setSQL("select * from " + resultsTableName + " where endpoint_id = ? ORDER BY " + type.getFirstDisplayColumnLabel());
            dc.addParam(endpointId);

            return dc.loadObjectResults();

        } catch (Exception e) {
            throw new Exception("Error running the " + type.name() + " Integrity Check", e);
        }
    }

	/**
	 * Un-zips the file containing the information of the specified type. The
	 * data of the records that will be fixed in the destination end point will
	 * be stored in a results table, which is unique for every type of integrity
	 * fix.
	 * 
	 * @param dataToFix
	 *            - The {@link InputStream} containing the data to fix.
	 * @param endpointId
	 *            - The ID of the end point where the data will be fixed.
	 * @param type
	 *            - The type of object (Content Page, Folder, Content Type,
	 *            etc.) that will be fixed.
	 * @throws Exception
	 *             An error occurred during the integrity fix process. The
	 *             results table must be wiped out.
	 */
    public void fixConflicts(InputStream dataToFix, String endpointId, IntegrityType type)
            throws Exception {
        final String outputDir = ConfigUtils.getIntegrityPath() + File.separator + endpointId;

        // lets first unzip the given file
        unzipFile(dataToFix, outputDir);

        // lets generate the tables with the data to be fixed
        generateDataToFixTable(endpointId, type);
        fixConflicts(endpointId, type);

        HibernateUtil.addCommitListener(new FlushCacheRunnable() {
            @Override
            public void run() {

               IntegrityUtil.this.flushAllCache();
            }
        });

    }

    /**
     * Flush all caches
     * @throws DotDataException
     */
    public void flushAllCache() {

        try {
            MaintenanceUtil.flushCache();
            APILocator.getPermissionAPI().resetAllPermissionReferences();
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
        }
    }

    /**
	 * Takes the information from the .ZIP file and stores it in the results
	 * table so that the process to fix records begins. Every type of object
	 * (Content Page, Folder, Content Type, etc.) has its own results table
	 * which indicates what records <b>MUST</b> be changed in the specified end
	 * point.
	 * 
	 * @param endpointId
	 *            - The ID of the end point where the data will be fixed.
	 * @param type
	 *            - The type of object (Content Page, Folder, Content Type,
	 *            etc.) that will be fixed.
	 * @throws Exception
	 *             An error occurred during the integrity fix process.
	 */
    public void generateDataToFixTable(String endpointId, IntegrityType type) throws Exception {

        try {
            CsvReader csvFile = new CsvReader(ConfigUtils.getIntegrityPath() + File.separator
                    + endpointId + File.separator + type.getDataToFixCSVName(), '|',
                    Charset.forName("UTF-8"));

            final String resultsTable = type.getResultsTableName();
            if (!type.hasResultsTable()) {
                throw new DotDataException("Integrity type =[" + type
                        + "] does not support this method, because results table is not available.");
            }

            // Create insert query for temporary table
            StringBuilder sbInsertTempTable = new StringBuilder("insert into ").append(resultsTable);
            switch(type) {
	        	case HTMLPAGES:
	        	case FILEASSETS:
	                sbInsertTempTable.append(
	                	" (local_working_inode, remote_working_inode, local_live_inode, remote_live_inode, local_identifier, remote_identifier, "
	                ).append(type.getFirstDisplayColumnLabel()).append(", endpoint_id, language_id) values(?,?,?,?,?,?,?,?,?)");
	                break;
            	case FOLDERS:
                    sbInsertTempTable.append(" (local_inode, remote_inode, local_identifier, remote_identifier, endpoint_id) values(?,?,?,?,?)");
                    break;
            	case CMS_ROLES:
            		sbInsertTempTable.append(" (name, role_key, local_role_id, remote_role_id, local_role_fqn, remote_role_fqn, endpoint_id) values(?,?,?,?,?,?,?)");
            		break;
                default:
                    sbInsertTempTable.append(" (local_inode, remote_inode, endpoint_id) values(?,?,?)");
                	break;
            }
            final String INSERT_TEMP_TABLE = sbInsertTempTable.toString();

            DotConnect dc = new DotConnect();
            while (csvFile.readRecord()) { // TODO: FIX THE INDEXES FOR
                                           // HTMLPAGES
                dc.setSQL(INSERT_TEMP_TABLE);

                if (type == IntegrityType.CMS_ROLES) {
	                dc.addParam(csvFile.get(0)); // name
	                dc.addParam(csvFile.get(1)); // role_key
	                dc.addParam(csvFile.get(2)); // local_role_id
	                dc.addParam(csvFile.get(3)); // remote_role_id
	                dc.addParam(csvFile.get(4)); // local_role_fqn
	                dc.addParam(csvFile.get(5)); // remote_role_fqn
                } else {
	                dc.addParam(csvFile.get(0)); // localWorkingInode
	                dc.addParam(csvFile.get(1)); // remoteWorkingInode

	                if (type == IntegrityType.HTMLPAGES || type == IntegrityType.FILEASSETS) {
	                    dc.addParam(csvFile.get(2)); // localLiveInode
	                    dc.addParam(csvFile.get(3)); // remoteLiveInode
	                    dc.addParam(csvFile.get(4)); // localIdentifier
	                    dc.addParam(csvFile.get(5)); // remoteIdentifier
	                    dc.addParam(csvFile.get(6)); // contentletAssetName
	                } else if (type == IntegrityType.FOLDERS) {
	                    dc.addParam(csvFile.get(2)); // localIdentifier
	                    dc.addParam(csvFile.get(3)); // remoteIdentifier
	                }
                }

                dc.addParam(endpointId);

                if (type == IntegrityType.HTMLPAGES || type == IntegrityType.FILEASSETS) {
                    dc.addParam(new Long(csvFile.get(7))); // languageId
                }

                dc.loadResult();
            }

        } catch (Exception e) {
            throw new Exception("Error generating data to fix", e);
        }
    }

    public boolean doesIntegrityConflictsDataExist(String endpointId) throws Exception {
        boolean conflictsDataExist = false;

        IntegrityType[] types = IntegrityType.values();
        for (IntegrityType integrityType : types) {
            if (integrityType.getIntegrityChecker().doesIntegrityConflictsDataExist(endpointId)) {
                conflictsDataExist = true;
                break;
            }
        }

        return conflictsDataExist;
    }

    private boolean doesTableExist(String tableName) throws DotDataException {
        DotConnect dc = new DotConnect();

        if (DbConnectionFactory.isOracle()) {
            dc.setSQL("SELECT COUNT(*) as exist FROM user_tables WHERE table_name='"
                    + tableName.toUpperCase() + "'");
            BigDecimal existTable = (BigDecimal) dc.loadObjectResults().get(0).get("exist");
            return existTable.longValue() > 0;
        } else if (DbConnectionFactory.isPostgres() || DbConnectionFactory.isMySql()) {
            dc.setSQL("SELECT COUNT(table_name) as exist FROM information_schema.tables WHERE Table_Name = '"
                    + tableName + "' ");
            long existTable = (Long) dc.loadObjectResults().get(0).get("exist");
            return existTable > 0;
        } else if (DbConnectionFactory.isMsSql()) {
            dc.setSQL("SELECT COUNT(*) as exist FROM sysobjects WHERE name = '" + tableName + "'");
            int existTable = (Integer) dc.loadObjectResults().get(0).get("exist");
            return existTable > 0;
        } else if (DbConnectionFactory.isH2()) {
            dc.setSQL("SELECT COUNT(1) as exist FROM information_schema.tables WHERE Table_Name = '"
                    + tableName.toUpperCase() + "' ");
            long existTable = (Long) dc.loadObjectResults().get(0).get("exist");
            return existTable > 0;
        }

        return false;
    }

	/**
	 * Executes the integrity fix process according to the specified type.
	 * 
	 * @param endpointId
	 *            - The ID of the end point where the data will be fixed.
	 * @param type
	 *            - The type of object (Content Page, Folder, Content Type,
	 *            etc.) that will be fixed.
	 * @throws DotDataException
	 *             An error occurred when interacting with the database.
	 * @throws DotSecurityException
	 *             The specified user does not have permissions to perform the
	 *             action.
	 */
    public void fixConflicts(final String endpointId, IntegrityType type) throws DotDataException,
            DotSecurityException {
        type.getIntegrityChecker().executeFix(endpointId);
    }

    /**
     * Discard conflicts from a integrity type in a specific endpoint.
     * 
     * @param endpointId
     * @param type
     *            indicates the integrity type
     * @throws DotDataException
     */
    public void discardConflicts(final String endpointId, IntegrityType type)
            throws DotDataException {
        type.getIntegrityChecker().discardConflicts(endpointId);
    }

    /**
     * Discard conflicts from each integrity type in {@link IntegrityType} enum
     * by passing a specific endpoint.
     * 
     * @param endpointId
     * @throws DotDataException
     * @throws Exception
     */
    public void completeDiscardConflicts(final String endpointId) throws DotDataException {
        IntegrityType[] types = IntegrityType.values();
        for (IntegrityType integrityType : types) {
            integrityType.getIntegrityChecker().discardConflicts(endpointId);
        }
    }

    /**
     * Check integrity from each integrity type in {@link IntegrityType} enum by
     * passing a specific endpoint. Also, it generates and fill the results
     * table to view results
     * 
     * @param endpointId
     * @return is there is at least one conflict returns true, otherwise false
     * @throws Exception
     */
    public boolean completeCheckIntegrity(final String endpointId) throws Exception {
        boolean existConflicts = false;

        IntegrityType[] types = IntegrityType.values();
        for (IntegrityType integrityType : types) {
            existConflicts |= integrityType.getIntegrityChecker()
                    .generateIntegrityResults(endpointId);
        }

        return existConflicts;
    }

    /**
     * Integrity data generation metadata bean to be saved.
     */
    public static class IntegrityDataExecutionMetadata implements Serializable {

        private final String endpointId;
        private final String requestId;
        private final IntegrityResource.ProcessStatus processStatus;
        private final String errorMessage;

        public IntegrityDataExecutionMetadata(String endpointId,
                                              String requestId,
                                              IntegrityResource.ProcessStatus processStatus,
                                              String errorMessage) {
            this.endpointId = endpointId;
            this.requestId = requestId;
            this.processStatus = processStatus;
            this.errorMessage = errorMessage;
        }

        public IntegrityDataExecutionMetadata(String endpointId,
                                              String requestId,
                                              String status,
                                              String errorMessage) {
            this(endpointId, requestId, IntegrityResource.ProcessStatus.valueOf(status.toUpperCase()), errorMessage);
        }

        public String getEndpointId() {
            return endpointId;
        }

        public String getRequestId() {
            return requestId;
        }

        public IntegrityResource.ProcessStatus getProcessStatus() {
            return processStatus;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

    }

}
