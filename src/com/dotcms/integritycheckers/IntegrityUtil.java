package com.dotcms.integritycheckers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.dotcms.repackage.com.csvreader.CsvReader;
import com.dotcms.repackage.com.csvreader.CsvWriter;
import com.dotcms.rest.IntegrityResource;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

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

    private File generateDataToFixCSV(String outputPath, String endpointId, IntegrityType type)
            throws DotDataException, IOException {
        File csvFile = null;
        CsvWriter writer = null;
        PreparedStatement statement = null;

        try {
            final String outputFile = outputPath + File.separator + type.getDataToFixCSVName();
            csvFile = new File(outputFile);
            writer = new CsvWriter(new FileWriter(csvFile, true), '|');

            String resultsTable = type.getResultsTableName();
            if (!type.hasResultsTable()) {
                throw new DotDataException("Integrity type =[" + type
                        + "] does not support this method, because results table is not available.");
            }

            Connection conn = DbConnectionFactory.getConnection();
            if (type == IntegrityType.HTMLPAGES || type == IntegrityType.FILEASSETS) {
                statement = conn
                        .prepareStatement(new StringBuilder("select ")
                                .append(type.getFirstDisplayColumnLabel())
                                .append(", remote_working_inode, local_working_inode, remote_live_inode, local_live_inode, remote_identifier, local_identifier, language_id from ")
                                .append(resultsTable).append(" where endpoint_id = ?").toString());
            } else if (type == IntegrityType.FOLDERS) {
                statement = conn
                        .prepareStatement("select remote_inode, local_inode, remote_identifier, local_identifier from "
                                + resultsTable + " where endpoint_id = ?");
            } else {
                statement = conn.prepareStatement("select remote_inode, local_inode from "
                        + resultsTable + " where endpoint_id = ?");
            }

            statement.setString(1, endpointId);
            try (ResultSet rs = statement.executeQuery()) {
                int count = 0;

                while (rs.next()) {
                    if (type == IntegrityType.HTMLPAGES || type == IntegrityType.FILEASSETS) {
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
            }
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        } finally {
            try {
                if (statement != null)
                    statement.close();
            } catch (Exception e) {
            }
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
            try (FileInputStream fis = new FileInputStream(file)) {
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

        // if file doesnt exists, then create it
        if (!dir.exists()) {
            dir.mkdir();
        }

        try (ZipInputStream zin = new ZipInputStream(zipFile)) {
            ZipEntry ze = null;
            while ((ze = zin.getNextEntry()) != null) {
                Logger.info(IntegrityUtil.class, "Unzipping " + ze.getName());

                try (FileOutputStream fout = new FileOutputStream(outputDir + File.separator
                        + ze.getName())) {
                    for (int c = zin.read(); c != -1; c = zin.read()) {
                        fout.write(c);
                    }
                    zin.closeEntry();
                }
            }
        } catch (IOException e) {
            Logger.error(IntegrityUtil.class, "Error while unzipping Integrity Data", e);
            throw new Exception("Error while unzipping Integrity Data", e);
        }
    }

    /**
     * Creates all the CSV from End Point database table and store them inside
     * zip file.
     *
     * @param endpointId
     * @throws Exception
     */
    public void generateDataToCheckZip(String endpointId) throws Exception {
        File zipFile = null;

        try {
            if (!UtilMethods.isSet(endpointId))
                return;

            final String outputPath = ConfigUtils.getIntegrityPath() + File.separator + endpointId;

            File dir = new File(outputPath);

            // if file doesn't exist, create it
            if (!dir.exists()) {
                dir.mkdir();
            }

            zipFile = new File(outputPath + File.separator
                    + IntegrityResource.INTEGRITY_DATA_TO_CHECK_ZIP_FILE_NAME);

            try (FileOutputStream fos = new FileOutputStream(zipFile);
                    ZipOutputStream zos = new ZipOutputStream(fos)) {
                IntegrityType[] types = IntegrityType.values();
                for (IntegrityType integrityType : types) {
                    File fileToCheckCsvFile = null;

                    try {
                        fileToCheckCsvFile = integrityType.createIntegrityCheckerInstance()
                                .generateCSVFile(outputPath);

                        addToZipFile(fileToCheckCsvFile.getAbsolutePath(), zos,
                                integrityType.getDataToCheckCSVName());
                    } finally {
                        if (fileToCheckCsvFile != null && fileToCheckCsvFile.exists())
                            fileToCheckCsvFile.delete();
                    }
                }
            }
        } catch (Exception e) {
            if (zipFile != null && zipFile.exists())
                zipFile.delete();

            throw new Exception(e);
        }
    }

    public void generateDataToFixZip(String endpointId, IntegrityType type) {
        File dataToFixCsvFile = null;
        File zipFile = null;

        try {
            if (!UtilMethods.isSet(endpointId))
                return;

            final String outputPath = ConfigUtils.getIntegrityPath() + File.separator + endpointId;

            File dir = new File(outputPath);

            // if file doesn't exist, create it
            if (!dir.exists()) {
                dir.mkdir();
            }

            zipFile = new File(outputPath + File.separator
                    + IntegrityResource.INTEGRITY_DATA_TO_FIX_ZIP_FILE_NAME);
            try (FileOutputStream fos = new FileOutputStream(zipFile);
                    ZipOutputStream zos = new ZipOutputStream(fos)) {
                // create Folders CSV
                dataToFixCsvFile = generateDataToFixCSV(outputPath, endpointId, type);

                addToZipFile(dataToFixCsvFile.getAbsolutePath(), zos, type.getDataToFixCSVName());
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
                final String tempTableName = getTempTableName(endpointId, integrityType);

                if (doesTableExist(tempTableName)) {
                    dc.executeStatement("truncate table " + tempTableName);
                    dc.executeStatement("drop table " + tempTableName);
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
            DotConnect dc = new DotConnect();
            final String resultsTable = type.getResultsTableName();
            if (!type.hasResultsTable()) {
                throw new DotDataException("Integrity type =[" + type
                        + "] does not support this method, because results table is not available.");
            }

            // Create insert query for temporary table
            StringBuilder sbInsertTempTable = new StringBuilder("insert into ")
                    .append(resultsTable);
            if (type == IntegrityType.FOLDERS) {
                sbInsertTempTable
                        .append(" (local_inode, remote_inode, local_identifier, remote_identifier, endpoint_id) values(?,?,?,?,?)");
            } else if (type == IntegrityType.HTMLPAGES || type == IntegrityType.FILEASSETS) {
                sbInsertTempTable
                        .append(" (local_working_inode, remote_working_inode, local_live_inode, remote_live_inode, local_identifier, remote_identifier, ")
                        .append(type.getFirstDisplayColumnLabel())
                        .append(", endpoint_id, language_id) values(?,?,?,?,?,?,?,?,?)");
            } else {
                sbInsertTempTable.append(" (local_inode, remote_inode, endpoint_id) values(?,?,?)");
            }
            final String INSERT_TEMP_TABLE = sbInsertTempTable.toString();

            while (csvFile.readRecord()) { // TODO: FIX THE INDEXES FOR
                                           // HTMLPAGES
                dc.setSQL(INSERT_TEMP_TABLE);

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

    /**
     * Creates temporary TABLE for integrity checking purposes.
     *
     * @param endpointId
     * @param type
     * @return
     */
    private String getTempTableName(String endpointId, IntegrityType type) {

        if (!UtilMethods.isSet(endpointId))
            return null;

        String endpointIdforDB = endpointId.replace("-", "");
        String resultsTableName = type.name().toLowerCase() + "_temp_" + endpointIdforDB;

        if (DbConnectionFactory.isOracle()) {
            resultsTableName = resultsTableName.substring(0, 29);
        } else if (DbConnectionFactory.isMsSql()) {
            resultsTableName = "#" + resultsTableName;
        }

        return resultsTableName;
    }

    public boolean doesIntegrityConflictsDataExist(String endpointId) throws Exception {
        boolean conflictsDataExist = false;

        IntegrityType[] types = IntegrityType.values();
        for (IntegrityType integrityType : types) {
            if (integrityType.createIntegrityCheckerInstance().doesIntegrityConflictsDataExist(
                    endpointId)) {
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
        type.createIntegrityCheckerInstance().executeFix(endpointId);
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
        type.createIntegrityCheckerInstance().discardConflicts(endpointId);
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
            integrityType.createIntegrityCheckerInstance().discardConflicts(endpointId);
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
            existConflicts |= integrityType.createIntegrityCheckerInstance()
                    .generateIntegrityResults(endpointId);
        }

        return existConflicts;
    }
}
