package com.dotcms.rest;

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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.repackage.com.csvreader.CsvReader;
import com.dotcms.repackage.com.csvreader.CsvWriter;
import com.dotcms.rest.IntegrityResource.IntegrityType;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageCache;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.business.WorkflowCache;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

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

    private File generateFoldersToCheckCSV(String outputFile) throws DotDataException, IOException {
        Connection conn = DbConnectionFactory.getConnection();
        ResultSet rs = null;
        PreparedStatement statement = null;
        CsvWriter writer = null;
        File csvFile = null;

        try {
            csvFile = new File(outputFile);
            writer = new CsvWriter(new FileWriter(csvFile, true), '|');
            statement = conn.prepareStatement("select f.inode, f.identifier, i.parent_path, i.asset_name, i.host_inode from folder f join identifier i on f.identifier = i.id ");
			rs = statement.executeQuery();
            int count = 0;

            while (rs.next()) {
                writer.write(rs.getString("inode"));
                writer.write(rs.getString("identifier"));
                writer.write(rs.getString("parent_path"));
                writer.write(rs.getString("asset_name"));
                writer.write(rs.getString("host_inode"));
                writer.endRecord();
                count++;

                if(count==1000) {
                    writer.flush();
                    count = 0;
                }
            }

        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(),e);
        }finally {
            try { if (rs != null) rs.close(); } catch (Exception e) { }
            try { if ( statement!= null ) statement.close(); } catch (Exception e) { }
            if(writer!=null) writer.close();

        }

        return csvFile;

    }

	/**
	 * Creates CSV file with either the HTML Pages or Content Pages information
	 * from End Point server.
	 *
	 * @param outputFile
	 *            - The file containing the list of pages.
	 * @param type
	 *            - The type of page to retrieve: Legacy Page of Content Page.
	 * @return a {@link File} with the page information.
	 * @throws DotDataException
	 *             An error occurred when querying the database.
	 * @throws IOException
	 *             An error occurred when writing to the file.
	 */
    private File generateHtmlPagesToCheckCSV(String outputFile, IntegrityType type) throws DotDataException, IOException {
        Connection conn = DbConnectionFactory.getConnection();
        ResultSet rs = null;
        PreparedStatement statement = null;
        CsvWriter writer = null;
        File csvFile = null;

        try {
            csvFile = new File(outputFile);
            writer = new CsvWriter(new FileWriter(csvFile, true), '|');
            if (type.equals(IntegrityType.HTMLPAGES)) {
            	// Query the legacy pages
            	statement = conn.prepareStatement("select distinct hvi.working_inode, hvi.live_inode, h.identifier, i.parent_path, i.asset_name, i.host_inode " +
                        "from htmlpage h join identifier i on h.identifier = i.id join htmlpage_version_info hvi on i.id = hvi.identifier");
            } else {
            	// Query the new content pages pages
				String query = "SELECT DISTINCT "
						+ "c.identifier, cvi.working_inode, cvi.live_inode, i.parent_path, i.asset_name, i.host_inode, c.language_id "
						+ "FROM "
						+ "contentlet c "
						+ "INNER JOIN contentlet_version_info cvi ON (c.identifier = cvi.identifier and c.language_id = cvi.lang) "
						+ "INNER JOIN structure s ON (c.structure_inode = s.inode AND s.structuretype = 5) "
						+ "INNER JOIN identifier i ON (i.id = c.identifier)";
            	statement = conn.prepareStatement(query);
            }
			rs = statement.executeQuery();
            int count = 0;

            while (rs.next()) {
                writer.write(rs.getString("working_inode"));
                writer.write(rs.getString("live_inode"));
                writer.write(rs.getString("identifier"));
                writer.write(rs.getString("parent_path"));
                writer.write(rs.getString("asset_name"));
                writer.write(rs.getString("host_inode"));
                if (type.equals(IntegrityType.CONTENTPAGES)) {
                	// Include language ID for new pages
                	writer.write(rs.getString("language_id"));
                } else {
                	// Pass meaningless value for legacy pages
                	writer.write("0");
                }
                writer.endRecord();
                count++;

                if(count==1000) {
                    writer.flush();
                    count = 0;
                }
            }

        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(),e);
        }finally {
            try { if (rs != null) rs.close(); } catch (Exception e) { }
            try { if ( statement!= null ) statement.close(); } catch (Exception e) { }
            if(writer!=null) writer.close();
        }

        return csvFile;
    }

    private File generateStructuresToCheckCSV(String outputFile) throws DotDataException, IOException {
        Connection conn = DbConnectionFactory.getConnection();
        ResultSet rs = null;
        PreparedStatement statement = null;
        CsvWriter writer = null;
        File csvFile = null;

        try {
            csvFile = new File(outputFile);
            writer = new CsvWriter(new FileWriter(csvFile, true), '|');
            statement = conn.prepareStatement("select inode, velocity_var_name from structure ");
            rs = statement.executeQuery();
            int count = 0;

            while (rs.next()) {
                writer.write(rs.getString("inode"));
                writer.write(rs.getString("velocity_var_name"));
                writer.endRecord();
                count++;

                if(count==1000) {
                    writer.flush();
                    count = 0;
                }
            }

        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(),e);
        }finally {
            try { if (rs != null) rs.close(); } catch (Exception e) { }
            try { if ( statement!= null ) statement.close(); } catch (Exception e) { }
            if(writer!=null) writer.close();
        }

        return csvFile;

    }

    private File generateSchemesToCheckCSV(String outputFile) throws DotDataException, IOException {
        Connection conn = DbConnectionFactory.getConnection();
        ResultSet rs = null;
        PreparedStatement statement = null;
        CsvWriter writer = null;
        File csvFile = null;

        try {

            csvFile = new File(outputFile);
            writer = new CsvWriter(new FileWriter(csvFile, true), '|');
            statement = conn.prepareStatement("select id, name from workflow_scheme ");
            rs = statement.executeQuery();
            int count = 0;

            while (rs.next()) {
                writer.write(rs.getString("id"));
                writer.write(rs.getString("name"));
                writer.endRecord();
                count++;

                if(count==1000) {
                    writer.flush();
                    count = 0;
                }
            }

        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(),e);
        }finally {
            try { if (rs != null) rs.close(); } catch (Exception e) { }
            try { if ( statement!= null ) statement.close(); } catch (Exception e) { }
            if(writer!=null) writer.close();
        }

        return csvFile;

    }

    private File generateDataToFixCSV(String outputPath, String endpointId, IntegrityType type) throws DotDataException, IOException {
        Connection conn = DbConnectionFactory.getConnection();
        ResultSet rs = null;
        PreparedStatement statement = null;
        CsvWriter writer = null;
        File csvFile = null;

        try {
            String outputFile = outputPath + File.separator + type.getDataToFixCSVName();
            csvFile = new File(outputFile);
            writer = new CsvWriter(new FileWriter(csvFile, true), '|');

            String resultsTable = getResultsTableName(type);

            if(type == IntegrityType.HTMLPAGES){
                statement = conn.prepareStatement("select html_page, remote_working_inode, local_working_inode, remote_live_inode, local_live_inode, remote_identifier, local_identifier, language_id from " + resultsTable + " where endpoint_id = ?");
            } else if(type == IntegrityType.FOLDERS) {
				statement = conn.prepareStatement("select remote_inode, local_inode, remote_identifier, local_identifier from " + resultsTable + " where endpoint_id = ?");
			} else {
				statement = conn.prepareStatement("select remote_inode, local_inode from " + resultsTable + " where endpoint_id = ?");
			}

            statement.setString(1, endpointId);
            rs = statement.executeQuery();
            int count = 0;

            while (rs.next()) {
                if(type == IntegrityType.HTMLPAGES) {
                    writer.write(rs.getString("remote_working_inode"));
                    writer.write(rs.getString("local_working_inode"));
                    writer.write(rs.getString("remote_live_inode"));
                    writer.write(rs.getString("local_live_inode"));
                } else {
                    writer.write(rs.getString("remote_inode"));
                    writer.write(rs.getString("local_inode"));
                }

                if(type == IntegrityType.FOLDERS || type == IntegrityType.HTMLPAGES) {
                	writer.write(rs.getString("remote_identifier"));
                	writer.write(rs.getString("local_identifier"));
                }

                if(type == IntegrityType.HTMLPAGES) {
                    writer.write(rs.getString("html_page"));
                    writer.write(rs.getString("language_id"));
                }

                writer.endRecord();
                count++;

                if(count==1000) {
                    writer.flush();
                    count = 0;
                }
            }

        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(),e);
        }finally {
            try { if (rs != null) rs.close(); } catch (Exception e) { }
            try { if ( statement!= null ) statement.close(); } catch (Exception e) { }
            if(writer!=null) writer.close();

        }

        return csvFile;

    }

    private static void addToZipFile(String fileName, ZipOutputStream zos, String zipEntryName) throws Exception  {

        Logger.info(IntegrityUtil.class, "Writing '" + fileName + "' to zip file");

        try {

            File file = new File(fileName);
            FileInputStream fis = new FileInputStream(file);
            ZipEntry zipEntry = new ZipEntry(zipEntryName);
            zos.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }

            zos.closeEntry();
            fis.close();

        } catch(FileNotFoundException f){
            Logger.error(IntegrityResource.class, "Could not find file " + fileName, f);
            throw new Exception("Could not find file " + fileName, f);
        } catch (IOException e) {
            Logger.error(IntegrityResource.class, "Error writing file to zip: " + fileName, e);
            throw new Exception("Error writing file to zip: " + fileName, e);
        }
    }

    public static void unzipFile(InputStream zipFile, String outputDir) throws Exception {
        ZipInputStream zin = new ZipInputStream(zipFile);

        ZipEntry ze = null;

        File dir = new File(outputDir);

        // if file doesnt exists, then create it
        if (!dir.exists()) {
            dir.mkdir();
        }

        try {

            while ((ze = zin.getNextEntry()) != null) {
                System.out.println("Unzipping " + ze.getName());

                FileOutputStream fout = new FileOutputStream(outputDir + File.separator +ze.getName());
                for (int c = zin.read(); c != -1; c = zin.read()) {
                    fout.write(c);
                }
                zin.closeEntry();
                fout.close();
            }
            zin.close();

        } catch(IOException e) {
            Logger.error(IntegrityResource.class, "Error while unzipping Integrity Data", e);
            throw new Exception("Error while unzipping Integrity Data", e);
        }

    }

    /**
     * Creates all the CSV from End Point database table and store them inside zip file.
     *
     * @param endpointId
     * @throws Exception
     */
    public void generateDataToCheckZip(String endpointId) throws Exception {
        File foldersToCheckCsvFile = null;
        File structuresToCheckCsvFile = null;
        File schemesToCheckCsvFile = null;
        File htmlPagesToCheckCsvFile = null;
        File contentPagesToCheckCsvFile = null;
        File zipFile = null;

        try {

            if(!UtilMethods.isSet(endpointId))
                return;

            String outputPath = ConfigUtils.getIntegrityPath() + File.separator + endpointId;

            File dir = new File(outputPath);

            // if file doesn't exist, create it
            if (!dir.exists()) {
                dir.mkdir();
            }

            zipFile = new File(outputPath + File.separator + IntegrityResource.INTEGRITY_DATA_TO_CHECK_ZIP_FILE_NAME);
            FileOutputStream fos = new FileOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(fos);

            // create Folders CSV
            IntegrityUtil integrityUtil = new IntegrityUtil();

            foldersToCheckCsvFile = integrityUtil.generateFoldersToCheckCSV(outputPath + File.separator + IntegrityType.FOLDERS.getDataToCheckCSVName());
            structuresToCheckCsvFile = integrityUtil.generateStructuresToCheckCSV(outputPath + File.separator + IntegrityType.STRUCTURES.getDataToCheckCSVName());
            schemesToCheckCsvFile = integrityUtil.generateSchemesToCheckCSV(outputPath + File.separator + IntegrityType.SCHEMES.getDataToCheckCSVName());
            htmlPagesToCheckCsvFile = integrityUtil.generateHtmlPagesToCheckCSV(outputPath + File.separator + IntegrityType.HTMLPAGES.getDataToCheckCSVName(), IntegrityType.HTMLPAGES);
            contentPagesToCheckCsvFile = integrityUtil.generateHtmlPagesToCheckCSV(outputPath + File.separator + IntegrityType.CONTENTPAGES.getDataToCheckCSVName(), IntegrityType.CONTENTPAGES);
            addToZipFile(foldersToCheckCsvFile.getAbsolutePath(), zos, IntegrityType.FOLDERS.getDataToCheckCSVName());
            addToZipFile(structuresToCheckCsvFile.getAbsolutePath(), zos, IntegrityType.STRUCTURES.getDataToCheckCSVName());
            addToZipFile(schemesToCheckCsvFile.getAbsolutePath(), zos, IntegrityType.SCHEMES.getDataToCheckCSVName());
            addToZipFile(htmlPagesToCheckCsvFile.getAbsolutePath(), zos, IntegrityType.HTMLPAGES.getDataToCheckCSVName());
            addToZipFile(contentPagesToCheckCsvFile.getAbsolutePath(), zos, IntegrityType.CONTENTPAGES.getDataToCheckCSVName());
            zos.close();
            fos.close();
        } catch (Exception e) {

            if(zipFile!=null && zipFile.exists())
                zipFile.delete();

            throw new Exception(e);
        } finally {
            if(foldersToCheckCsvFile!=null && foldersToCheckCsvFile.exists())
                foldersToCheckCsvFile.delete();
            if(structuresToCheckCsvFile!=null && structuresToCheckCsvFile.exists())
                structuresToCheckCsvFile.delete();
            if(schemesToCheckCsvFile!=null && schemesToCheckCsvFile.exists())
                schemesToCheckCsvFile.delete();
            if(htmlPagesToCheckCsvFile!=null && htmlPagesToCheckCsvFile.exists())
            	htmlPagesToCheckCsvFile.delete();
			if (contentPagesToCheckCsvFile != null
					&& contentPagesToCheckCsvFile.exists()) {
				contentPagesToCheckCsvFile.delete();
			}
        }
    }

    public void generateDataToFixZip(String endpointId, IntegrityType type) {
        File dataToFixCsvFile = null;
        File zipFile = null;

        try {

            if(!UtilMethods.isSet(endpointId))
                return;

            String outputPath = ConfigUtils.getIntegrityPath() + File.separator + endpointId;

            File dir = new File(outputPath);

            // if file doesn't exist, create it
            if (!dir.exists()) {
                dir.mkdir();
            }

            zipFile = new File(outputPath + File.separator + IntegrityResource.INTEGRITY_DATA_TO_FIX_ZIP_FILE_NAME);
            FileOutputStream fos = new FileOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(fos);

            // create Folders CSV
            IntegrityUtil integrityUtil = new IntegrityUtil();

            dataToFixCsvFile = integrityUtil.generateDataToFixCSV(outputPath, endpointId, type );

            addToZipFile(dataToFixCsvFile.getAbsolutePath(), zos, type.getDataToFixCSVName());

            zos.close();
            fos.close();
        } catch (Exception e) {
        	Logger.error(getClass(), "Error generating fix for remote", e);
            if(zipFile!=null && zipFile.exists())
                zipFile.delete();
        } finally {
            if(dataToFixCsvFile!=null && dataToFixCsvFile.exists())
                dataToFixCsvFile.delete();
        }
    }

    public Boolean checkFoldersIntegrity(String endpointId) throws Exception {

        try {

            CsvReader folders = new CsvReader(ConfigUtils.getIntegrityPath() + File.separator + endpointId + File.separator + IntegrityType.FOLDERS.getDataToCheckCSVName(), '|', Charset.forName("UTF-8"));
            boolean tempCreated = false;
            DotConnect dc = new DotConnect();
            String tempTableName = getTempTableName(endpointId, IntegrityType.FOLDERS);

            // lets create a temp table and insert all the records coming from the CSV file
            String tempKeyword = getTempKeyword();

            String createTempTable = "create " +tempKeyword+ " table " + tempTableName + " (inode varchar(36) not null, identifier varchar(36) not null,parent_path varchar(255), "
                    + "asset_name varchar(255), host_identifier varchar(36) not null, primary key (inode) )" + (DbConnectionFactory.isOracle()?" ON COMMIT PRESERVE ROWS ":"");

            if(DbConnectionFactory.isOracle()) {
                createTempTable=createTempTable.replaceAll("varchar\\(", "varchar2\\(");
            }

            final String INSERT_TEMP_TABLE = "insert into " + tempTableName + " values(?,?,?,?,?)";

            while (folders.readRecord()) {

                if(!tempCreated) {
                    dc.executeStatement(createTempTable);
                    tempCreated = true;
                }

                String folderInode = folders.get(0);
				String folderIdentifier = folders.get(1);
				String parentPath = folders.get(2);
				String assetName = folders.get(3);
				String hostIdentifier = folders.get(4);

				dc.setSQL(INSERT_TEMP_TABLE);
				dc.addParam(folderInode);
				dc.addParam(folderIdentifier);
				dc.addParam(parentPath);
				dc.addParam(assetName);
				dc.addParam(hostIdentifier);
				dc.loadResult();
            }

            folders.close();

            String resultsTableName = getResultsTableName(IntegrityType.FOLDERS);

            // compare the data from the CSV to the local db data and see if we have conflicts
            dc.setSQL("select 1 from identifier iden "
                    + "join folder f on iden.id = f.identifier join " + tempTableName + " ft on iden.parent_path = ft.parent_path "
                    + "join contentlet c on iden.host_inode = c.identifier and iden.asset_name = ft.asset_name and ft.host_identifier = iden.host_inode "
                    + "join contentlet_version_info cvi on c.inode = cvi.working_inode "
                    + "where asset_type = 'folder' and f.inode <> ft.inode order by c.title, iden.asset_name");

            List<Map<String,Object>> results = dc.loadObjectResults();

            if(!results.isEmpty()) {
                // if we have conflicts, lets create a table out of them

                String fullFolder = " c.title || iden.parent_path || iden.asset_name ";

                if(DbConnectionFactory.isMySql()) {
                    fullFolder = " concat(c.title,iden.parent_path,iden.asset_name) ";
                } else if(DbConnectionFactory.isMsSql()) {
                    fullFolder = " c.title + iden.parent_path + iden.asset_name ";
                }

                final String INSERT_INTO_RESULTS_TABLE = "insert into " +resultsTableName+ " select " + fullFolder + " as folder, "
						+ "f.inode as local_inode, ft.inode as remote_inode, f.identifier as local_identifier, ft.identifier as remote_identifier, "
						+ "'" +endpointId+ "' from identifier iden "
						+ "join folder f on iden.id = f.identifier join " + tempTableName + " ft on iden.parent_path = ft.parent_path "
						+ "join contentlet c on iden.host_inode = c.identifier and iden.asset_name = ft.asset_name and ft.host_identifier = iden.host_inode "
						+ "join contentlet_version_info cvi on c.inode = cvi.working_inode "
						+ "where asset_type = 'folder' and f.inode <> ft.inode order by c.title, iden.asset_name";

                dc.executeStatement(INSERT_INTO_RESULTS_TABLE);

            }

            dc.setSQL("select * from folders_ir");
            results = dc.loadObjectResults();

            return !results.isEmpty();
        } catch(Exception e) {
            throw new Exception("Error running the Folders Integrity Check", e);
        }
    }

    public Boolean checkStructuresIntegrity(String endpointId) throws Exception {

        try {

            CsvReader structures = new CsvReader(ConfigUtils.getIntegrityPath() + File.separator + endpointId + File.separator + IntegrityType.STRUCTURES.getDataToCheckCSVName(), '|', Charset.forName("UTF-8"));
            boolean tempCreated = false;
            DotConnect dc = new DotConnect();
            String tempTableName = getTempTableName(endpointId, IntegrityType.STRUCTURES);

            String tempKeyword = getTempKeyword();

            String createTempTable = "create " +tempKeyword+ " table " + tempTableName + " (inode varchar(36) not null, velocity_var_name varchar(255), "
                    + " primary key (inode) )";

            if(DbConnectionFactory.isOracle()) {
                createTempTable=createTempTable.replaceAll("varchar\\(", "varchar2\\(");
            }

            final String INSERT_TEMP_TABLE = "insert into " + tempTableName + " values(?,?)";

            while (structures.readRecord()) {

                if(!tempCreated) {
                    dc.executeStatement(createTempTable);
                    tempCreated = true;
                }

                //select f.inode, i.parent_path, i.asset_name, i.host_inode
                String structureInode = structures.get(0);
                String verVarName = structures.get(1);

                dc.setSQL(INSERT_TEMP_TABLE);
                dc.addParam(structureInode);
                dc.addParam(verVarName);
                dc.loadResult();
            }

            structures.close();

            String resultsTableName = getResultsTableName(IntegrityType.STRUCTURES);

            // compare the data from the CSV to the local db data and see if we have conflicts
            dc.setSQL("select s.velocity_var_name as velocity_name, "
                    + "s.inode as local_inode, st.inode as remote_inode from structure s "
                    + "join " + tempTableName + " st on s.velocity_var_name = st.velocity_var_name and s.inode <> st.inode");

            List<Map<String,Object>> results = dc.loadObjectResults();

            if(!results.isEmpty()) {
                // if we have conflicts, lets create a table out of them
                String INSERT_INTO_RESULTS_TABLE = "insert into " +resultsTableName+ " select s.velocity_var_name as velocity_name, "
                        + "s.inode as local_inode, st.inode as remote_inode, '" + endpointId + "' from structure s "
                        + "join " + tempTableName + " st on s.velocity_var_name = st.velocity_var_name and s.inode <> st.inode";

                dc.executeStatement(INSERT_INTO_RESULTS_TABLE);
            }

            return !results.isEmpty();
        } catch(Exception e) {
            throw new Exception("Error running the Structures Integrity Check", e);
        }
    }

    public Boolean checkWorkflowSchemesIntegrity(String endpointId) throws Exception {

        try {

            CsvReader schemes = new CsvReader(ConfigUtils.getIntegrityPath() + File.separator + endpointId + File.separator + IntegrityType.SCHEMES.getDataToCheckCSVName(), '|', Charset.forName("UTF-8"));
            boolean tempCreated = false;
            DotConnect dc = new DotConnect();
            String tempTableName = getTempTableName(endpointId, IntegrityType.SCHEMES);

            String tempKeyword = getTempKeyword();

            String createTempTable = "create " +tempKeyword+ " table " + tempTableName + " (inode varchar(36) not null, name varchar(255), "
                    + " primary key (inode) )";

            if(DbConnectionFactory.isOracle()) {
                createTempTable=createTempTable.replaceAll("varchar\\(", "varchar2\\(");
            }

            final String INSERT_TEMP_TABLE = "insert into " + tempTableName + " values(?,?)";

            while (schemes.readRecord()) {

                if(!tempCreated) {
                    dc.executeStatement(createTempTable);
                    tempCreated = true;
                }

                //select f.inode, i.parent_path, i.asset_name, i.host_inode
                String schemeInode = schemes.get(0);
                String name = schemes.get(1);

                dc.setSQL(INSERT_TEMP_TABLE);
                dc.addParam(schemeInode);
                dc.addParam(name);
                dc.loadResult();
            }

            schemes.close();

            String resultsTableName = getResultsTableName(IntegrityType.SCHEMES);

            // compare the data from the CSV to the local db data and see if we have conflicts
            dc.setSQL("select s.name, s.id as local_inode, wt.inode as remote_inode from workflow_scheme s "
                    + "join " + tempTableName + " wt on s.name = wt.name and s.id <> wt.inode");

            List<Map<String,Object>> results = dc.loadObjectResults();


            if(!results.isEmpty()) {
                // if we have conflicts, lets create a table out of them
                final String INSERT_INTO_RESULTS_TABLE = "insert into "+resultsTableName+" select s.name, s.id as local_inode, wt.inode as remote_inode , '" + endpointId + "' from workflow_scheme s "
                        + "join " + tempTableName + " wt on s.name = wt.name and s.id <> wt.inode";

                dc.executeStatement(INSERT_INTO_RESULTS_TABLE);

            }

            return !results.isEmpty();
        } catch(Exception e) {
            throw new Exception("Error running the Workflow Schemes Integrity Check", e);
        }
    }

    /**
     * Checks possible conflicts with HTMLPages.
     *
     * @param endpointId Information of the Server you want to examine.
     * @return
     * @throws Exception
     */
    public Boolean checkHtmlPagesIntegrity(String endpointId) throws Exception {
		try {
			DotConnect dc = new DotConnect();
			checkPages(endpointId, IntegrityType.HTMLPAGES);
			checkPages(endpointId, IntegrityType.CONTENTPAGES);
			dc.setSQL("select * from "
					+ getResultsTableName(IntegrityType.HTMLPAGES));
			List<Map<String, Object>> results = dc.loadObjectResults();
			return !results.isEmpty();
		} catch (Exception e) {
			throw new Exception("Error running the HTML Pages Integrity Check",
					e);
		}
    }

	/**
	 * Checks the existence of conflicts with both the legacy HTML pages and
	 * Content Pages.
	 *
	 * @param endpointId
	 *            - The ID of the endpoint where conflicts will be detected.
	 * @param type
	 *            - The type of HTML asset to check: Legacy Page, or Content
	 *            Page.
	 * @throws IOException
	 *             An error occurred when reading the file containing the page
	 *             data.
	 * @throws SQLException
	 *             There's a syntax error in a SQL statement.
	 * @throws DotDataException
	 *             An error occurred when interacting with the database.
	 */
	private void checkPages(String endpointId, IntegrityType type) throws IOException,
			SQLException, DotDataException {
    	CsvReader htmlpages = new CsvReader(ConfigUtils.getIntegrityPath() + File.separator + endpointId + File.separator + type.getDataToCheckCSVName(), '|', Charset.forName("UTF-8"));

        DotConnect dc = new DotConnect();
        String tempTableName = getTempTableName(endpointId, type);
        String tempKeyword = getTempKeyword();
        String integerKeyword = getIntegerKeyword();

        //Create a temporary table and insert all the records coming from the CSV file.
        StringBuilder createTempTable = new StringBuilder();

        createTempTable.append("create ").append(tempKeyword).append(" table ").append(tempTableName)
                .append(" (working_inode varchar(36) not null")
                .append(", live_inode varchar(36) not null ")
                .append(", identifier varchar(36) not null ")
                .append(", parent_path varchar(255)")
                .append(", asset_name varchar(255)")
                .append(", host_identifier varchar(36) not null")
                .append(", language_id ").append(integerKeyword).append(" not null")
                .append(", primary key (working_inode, language_id) )").append((DbConnectionFactory.isOracle() ? " ON COMMIT PRESERVE ROWS " : ""));


        String createTempTableStr = createTempTable.toString();

        if(DbConnectionFactory.isOracle()) {
            createTempTableStr=createTempTableStr.replaceAll("varchar\\(", "varchar2\\(");
        }

        final String INSERT_TEMP_TABLE = "insert into " + tempTableName + " values(?,?,?,?,?,?,?)";

        dc.executeStatement(createTempTableStr);
        while (htmlpages.readRecord()) {

            String workingInode = null;
            String liveInode = null;

            workingInode = htmlpages.get(0);
            liveInode = htmlpages.get(1);

            String htmlPageIdentifier = htmlpages.get(2);
            String htmlPageParentPath = htmlpages.get(3);
            String htmlPageAssetName = htmlpages.get(4);
            String htmlPageHostIdentifier = htmlpages.get(5);
            String htmlPageLanguage = htmlpages.get(6);
            dc.setSQL(INSERT_TEMP_TABLE);

            dc.addParam(workingInode);
            dc.addParam(liveInode);
            dc.addParam(htmlPageIdentifier);
            dc.addParam(htmlPageParentPath);
            dc.addParam(htmlPageAssetName);
            dc.addParam(htmlPageHostIdentifier);
            dc.addParam(new Long(htmlPageLanguage));


			dc.loadResult();
        }
        htmlpages.close();

        String resultsTableName = getResultsTableName(IntegrityType.HTMLPAGES);

        //Compare the data from the CSV to the local database data and see if we have conflicts.
        String selectSQL = null;
        if (type.equals(IntegrityType.HTMLPAGES)) {
        	// Query the legacy pages
			selectSQL = "select lh.page_url as html_page, "
					+ "lh.inode as local_inode, "
					+ "ri.working_inode as remote_inode, "
					+ "li.id as local_identifier, "
					+ "ri.identifier as remote_identifier, ri.language_id "
					+ "from identifier as li " + "join htmlpage as lh "
					+ "on lh.identifier = li.id "
					+ "and li.asset_type = 'htmlpage' " + "join "
					+ tempTableName + " as ri "
					+ "on li.asset_name = ri.asset_name "
					+ "and li.parent_path = ri.parent_path "
					+ "and li.host_inode = ri.host_identifier "
					+ "and li.id <> ri.identifier";
        } else {
        	// Query the new content pages
			selectSQL = "SELECT DISTINCT "
					+ "li.asset_name as html_page, lcvi.working_inode as local_working_inode, lcvi.live_inode as local_live_inode, "
                    + "t.working_inode as remote_working_inode, t.live_inode as remote_live_inode, "
					+ "lc.identifier as local_identifier, t.identifier as remote_identifier, "
					+ "lc.language_id "
					+ "FROM "
					+ "identifier li "
					+ "INNER JOIN contentlet lc ON (lc.identifier = li.id and li.asset_type = 'contentlet') "
					+ "INNER JOIN contentlet_version_info lcvi ON (lc.identifier = lcvi.identifier) "
					+ "INNER JOIN structure ls ON (lc.structure_inode = ls.inode and ls.structuretype = 5) "
					+ "INNER JOIN "
					+ tempTableName
					+ " t ON (li.asset_name = t.asset_name AND li.parent_path = t.parent_path "
					+ "AND li.host_inode = host_identifier AND lc.identifier <> t.identifier "
					+ "AND lc.language_id = t.language_id)";
		}

        if(DbConnectionFactory.isOracle()) {
            selectSQL = selectSQL.replaceAll(" as ", " ");
        }

        dc.setSQL(selectSQL);

        List<Map<String,Object>> results = dc.loadObjectResults();

        //If we have conflicts, lets create a table out of them.
        if(!results.isEmpty()) {
            String fullHtmlPage = " li.parent_path || li.asset_name ";

            if(DbConnectionFactory.isMySql()) {
            	fullHtmlPage = " concat(li.parent_path,li.asset_name) ";
            } else if(DbConnectionFactory.isMsSql()) {
            	fullHtmlPage = " li.parent_path + li.asset_name ";
            }

            String insertSQL = null;
            if (type.equals(IntegrityType.HTMLPAGES)) {
            	// Query the legacy pages
	            insertSQL = "insert into " + resultsTableName + " (html_page, local_working_inode, remote_working_inode, local_identifier, remote_identifier, endpoint_id, language_id) "
	                + " select " + fullHtmlPage + " as html_page, "
	                + "lh.inode as local_inode, "
	                + "ri.inode as remote_inode, "
	                + "li.id as local_identifier, "
	                + "ri.identifier as remote_identifier, "
	                + "'" + endpointId + "', ri.language_id "
	                + "from identifier as li "
	                + "join htmlpage as lh "
	                + "on lh.identifier = li.id "
	                + "and li.asset_type = 'htmlpage' "
	                + "join " + tempTableName + " as ri "
	                + "on li.asset_name = ri.asset_name "
	                + "and li.parent_path = ri.parent_path "
	                + "and li.host_inode = ri.host_identifier "
	                + "and li.id <> ri.identifier";
            } else {
            	// Query the new content pages
            	insertSQL = "insert into " + resultsTableName + " "
            			+ "select DISTINCT " + fullHtmlPage + " as html_page, "
            			+ "lcvi.working_inode as local_working_inode, lcvi.live_inode as local_live_inode, "
                        + "t.working_inode as remote_working_inode, t.live_inode as remote_live_inode, "
            			+ "lc.identifier as local_identifier, t.identifier as remote_identifier, "
            			+ "'" + endpointId + "', " + "t.language_id as language_id "
            			+ "FROM "
    					+ "identifier li "
    					+ "INNER JOIN contentlet lc ON (lc.identifier = li.id and li.asset_type = 'contentlet') "
    					+ "INNER JOIN contentlet_version_info lcvi ON (lc.identifier = lcvi.identifier and lc.language_id = lcvi.lang) "
    					+ "INNER JOIN structure ls ON (lc.structure_inode = ls.inode and ls.structuretype = 5) "
    					+ "INNER JOIN "
    					+ tempTableName + " "
    					+ "t ON (li.asset_name = t.asset_name AND li.parent_path = t.parent_path "
    					+ "AND li.host_inode = host_identifier AND lc.identifier <> t.identifier "
    					+ "AND lc.language_id = t.language_id)";
            }

            if(DbConnectionFactory.isOracle()) {
                insertSQL = insertSQL.replaceAll(" as ", " ");
            }

            dc.executeStatement(insertSQL);
        }
    }

	public void dropTempTables(String endpointId) throws DotDataException {
        DotConnect dc = new DotConnect();
        try {

            if(doesTableExist(getTempTableName(endpointId, IntegrityType.FOLDERS))) {
                dc.executeStatement("truncate table " + getTempTableName(endpointId, IntegrityType.FOLDERS));
                dc.executeStatement("drop table " + getTempTableName(endpointId, IntegrityType.FOLDERS));
            }

            if(doesTableExist(getTempTableName(endpointId, IntegrityType.STRUCTURES))) {
                dc.executeStatement("truncate table " + getTempTableName(endpointId, IntegrityType.STRUCTURES));
                dc.executeStatement("drop table " + getTempTableName(endpointId, IntegrityType.STRUCTURES));
            }

            if(doesTableExist(getTempTableName(endpointId, IntegrityType.SCHEMES))) {
                dc.executeStatement("truncate table " + getTempTableName(endpointId, IntegrityType.SCHEMES));
                dc.executeStatement("drop table " + getTempTableName(endpointId, IntegrityType.SCHEMES));
            }

            if(doesTableExist(getTempTableName(endpointId, IntegrityType.HTMLPAGES))) {
                dc.executeStatement("truncate table " + getTempTableName(endpointId, IntegrityType.HTMLPAGES));
                dc.executeStatement("drop table " + getTempTableName(endpointId, IntegrityType.HTMLPAGES));
            }

            if(doesTableExist(getTempTableName(endpointId, IntegrityType.CONTENTPAGES))) {
                dc.executeStatement("truncate table " + getTempTableName(endpointId, IntegrityType.CONTENTPAGES));
                dc.executeStatement("drop table " + getTempTableName(endpointId, IntegrityType.CONTENTPAGES));
            }

        } catch (SQLException e) {
            Logger.error(getClass(), "Error dropping Temp tables");
            throw new DotDataException("Error dropping Temp tables", e);
        }
    }


    public List<Map<String, Object>> getIntegrityConflicts(String endpointId, IntegrityType type) throws Exception {
        try {
            DotConnect dc = new DotConnect();

            String resultsTableName = getResultsTableName(type);

            if(!doesTableExist(resultsTableName)) {
                return new ArrayList<Map<String, Object>>();
            }

            dc.setSQL("select * from " + resultsTableName + " where endpoint_id = ?");
            dc.addParam(endpointId);

            return dc.loadObjectResults();

        } catch(Exception e) {
            throw new Exception("Error running the "+type.name()+" Integrity Check", e);
        }
    }

    public void fixConflicts(InputStream dataToFix, String endpointId, IntegrityType type) throws Exception {

        String outputDir = ConfigUtils.getIntegrityPath() + File.separator + endpointId;
        // lets first unzip the given file
        unzipFile(dataToFix, outputDir);

        // lets generate the tables with the data to be fixed
        generateDataToFixTable(endpointId, type);

        fixConflicts(endpointId, type);

//        discardConflicts(endpointId, type);


    }

    public void generateDataToFixTable(String endpointId, IntegrityType type) throws Exception {

        try {

            CsvReader csvFile = new CsvReader(ConfigUtils.getIntegrityPath() + File.separator + endpointId + File.separator + type.getDataToFixCSVName(), '|', Charset.forName("UTF-8"));
            DotConnect dc = new DotConnect();
            String resultsTable = getResultsTableName(type);

            String INSERT_TEMP_TABLE = "insert into " + resultsTable + " (local_inode, remote_inode, endpoint_id) values(?,?,?)";

			if(type==IntegrityType.FOLDERS) {
				INSERT_TEMP_TABLE = "insert into " + resultsTable + " (local_inode, remote_inode, local_identifier, remote_identifier, endpoint_id) values(?,?,?,?,?)";
			} else if(type==IntegrityType.HTMLPAGES) {
				INSERT_TEMP_TABLE = "insert into " + resultsTable + " (local_working_inode, remote_working_inode, local_live_inode, remote_live_inode, local_identifier, remote_identifier, html_page, endpoint_id, language_id) values(?,?,?,?,?,?,?,?,?)";
            }

			while (csvFile.readRecord()) { // TODO: FIX THE INDEXES FOR HTMLPAGES

                String localWorkingInode = csvFile.get(0);
                String remoteWorkingInode = csvFile.get(1);

				dc.setSQL(INSERT_TEMP_TABLE);

				dc.addParam(localWorkingInode);
				dc.addParam(remoteWorkingInode);

                if(type == IntegrityType.HTMLPAGES) {
                    String localLiveInode = csvFile.get(2);;
                    String remoteLiveInode = csvFile.get(3);
                    String localIdentifier = csvFile.get(4);
                    String remoteIdentifier = csvFile.get(5);
                    String htmlPage = csvFile.get(6);
                    dc.addParam(localLiveInode);
                    dc.addParam(remoteLiveInode);
                    dc.addParam(localIdentifier);
                    dc.addParam(remoteIdentifier);
                    dc.addParam(htmlPage);
                } else if(type == IntegrityType.FOLDERS) {
					String localIdentifier = csvFile.get(2);
					String remoteIdentifier = csvFile.get(3);
					dc.addParam(localIdentifier);
					dc.addParam(remoteIdentifier);
				}

				dc.addParam(endpointId);

				if (type == IntegrityType.HTMLPAGES) {
                    String languageId = csvFile.get(7);
                    dc.addParam(new Long(languageId));
                }

				dc.loadResult();
			}

        } catch(Exception e) {
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

        if(!UtilMethods.isSet(endpointId)) return null;

        String endpointIdforDB = endpointId.replace("-", "");
        String resultsTableName = type.name().toLowerCase() + "_temp_" + endpointIdforDB;

        if(DbConnectionFactory.isOracle()) {
            resultsTableName = resultsTableName.substring(0, 29);
        } else if(DbConnectionFactory.isMsSql()) {
            resultsTableName = "#" + resultsTableName;
        }

        return resultsTableName;
    }

    private String getResultsTableName(IntegrityType type) {

        return type.name().toLowerCase() + "_ir";
    }

    /**
     * Return Temporary word depending on the Database in the data source.
     *
     * @return
     */
    private String getTempKeyword() {
        String tempKeyword = "temporary";

        if(DbConnectionFactory.isMsSql()) {
            tempKeyword = "";
        } else if(DbConnectionFactory.isOracle()) {
            tempKeyword = "global " + tempKeyword;
        }

        return tempKeyword;
    }

    public Boolean doesIntegrityConflictsDataExist(String endpointId) throws Exception {

        String folderTable = getResultsTableName(IntegrityType.FOLDERS);
        String structuresTable = getResultsTableName(IntegrityType.STRUCTURES);
        String schemesTable = getResultsTableName(IntegrityType.SCHEMES);

        DotConnect dc = new DotConnect();
        dc.setSQL("SELECT 1 FROM " + folderTable+ " where endpoint_id = ?");
        dc.addParam(endpointId);

        boolean isThereFolderData = !dc.loadObjectResults().isEmpty();

        dc.setSQL("SELECT 1 FROM " + structuresTable+ " where endpoint_id = ?");
        dc.addParam(endpointId);

        boolean isThereStructureData = !dc.loadObjectResults().isEmpty();

        dc.setSQL("SELECT 1 FROM " + schemesTable+ " where endpoint_id = ?");
        dc.addParam(endpointId);

        boolean isThereSchemaData = !dc.loadObjectResults().isEmpty();

        return isThereFolderData || isThereStructureData || isThereSchemaData;

    }

    private boolean doesTableExist(String tableName) throws DotDataException {
        DotConnect dc = new DotConnect();

        if(DbConnectionFactory.isOracle()) {
            dc.setSQL("SELECT COUNT(*) as exist FROM user_tables WHERE table_name='"+tableName.toUpperCase()+"'");
            BigDecimal existTable = (BigDecimal)dc.loadObjectResults().get(0).get("exist");
            return existTable.longValue() > 0;
        } else if(DbConnectionFactory.isPostgres() || DbConnectionFactory.isMySql()) {
            dc.setSQL("SELECT COUNT(table_name) as exist FROM information_schema.tables WHERE Table_Name = '"+tableName+"' ");
            long existTable = (Long)dc.loadObjectResults().get(0).get("exist");
            return existTable > 0;
        } else if(DbConnectionFactory.isMsSql()) {
            dc.setSQL("SELECT COUNT(*) as exist FROM sysobjects WHERE name = '"+tableName+"'");
            int existTable = (Integer)dc.loadObjectResults().get(0).get("exist");
            return existTable > 0;
        }  else if(DbConnectionFactory.isH2()) {
            dc.setSQL("SELECT COUNT(1) as exist FROM information_schema.tables WHERE Table_Name = '"+tableName.toUpperCase()+"' ");
            long existTable = (Long)dc.loadObjectResults().get(0).get("exist");
            return existTable > 0;
        }

        return false;
    }

    public void discardConflicts(String endpointId, IntegrityType type) throws DotDataException {
        DotConnect dc = new DotConnect();
        String resultsTableName = getResultsTableName(type);
        try {
			dc.executeStatement("delete from " + resultsTableName+ " where endpoint_id = '" + endpointId + "'");
		} catch (SQLException e) {
			throw new DotDataException(e.getMessage(), e);
		}

    }

    public void fixConflicts(String endpointId, IntegrityType type) throws DotDataException, DotSecurityException {
        if(type == IntegrityType.FOLDERS) {
            fixFolders(endpointId);
        } else if(type == IntegrityType.STRUCTURES) {
            fixStructures(endpointId);
        } else if(type == IntegrityType.SCHEMES) {
            fixSchemes(endpointId);
        } else if(type == IntegrityType.HTMLPAGES) {
            fixHtmlPages(endpointId);
        }
    }
    /**

     * Fixes folders inconsistencies for a given server id
     * Fixing a folder means updating it's inode and identifier with the ones received from the other end
     *
     * @param serverId
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public void fixFolders ( String serverId ) throws DotDataException, DotSecurityException {

        DotConnect dc = new DotConnect();
        String tableName = getResultsTableName( IntegrityType.FOLDERS );

        try {

        	 // lets remove from the index all the content under each conflicted folder

        	dc.setSQL("select local_inode, remote_inode, local_identifier, remote_identifier from " + tableName + " where endpoint_id = ?");
            dc.addParam(serverId);
            List<Map<String,Object>> results = dc.loadObjectResults();

            for (Map<String, Object> result : results) {
            	String oldFolderInode = (String) result.get("local_inode");
            	String newFolderInode = (String) result.get("remote_inode");
            	String oldFolderIdentifier = (String) result.get("local_identifier");
            	String newFolderIdentifier = (String) result.get("remote_identifier");

            	try {
            		Folder folder = APILocator.getFolderAPI().find(oldFolderInode, APILocator.getUserAPI().getSystemUser(), false);

            		List<Contentlet> contents = APILocator.getContentletAPI().findContentletsByFolder(folder, APILocator.getUserAPI().getSystemUser(), false);
            		for (Contentlet contentlet : contents) {
            			APILocator.getContentletIndexAPI().removeContentFromIndex(contentlet);
            			CacheLocator.getContentletCache().remove(contentlet.getInode());
            		}

            		Identifier folderIdentifier = APILocator.getIdentifierAPI().find(folder.getIdentifier());
            		CacheLocator.getFolderCache().removeFolder(folder, folderIdentifier);

            		CacheLocator.getIdentifierCache().removeFromCacheByIdentifier(folderIdentifier.getId());


            		// THIS IS THE NEW CODE

                    // 1.1) Insert dummy temp row on INODE table
                    if ( DbConnectionFactory.isOracle() ) {
                        dc.executeStatement( "insert into inode values ('TEMP_INODE', 'DUMMY_OWNER', to_date('1900-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'DUMMY_TYPE') " );
                    } else {
                        dc.executeStatement( "insert into inode values ('TEMP_INODE', 'DUMMY_OWNER', '1900-01-01 00:00:00.00', 'DUMMY_TYPE') " );
                    }

            		Structure fileAssetSt = StructureCache.getStructureByVelocityVarName("FileAsset");

            		// lets see if we have structures referencing the folder, if so, let's use its host for the dummy identifier
            		List<Structure> referencedStructures = APILocator.getFolderAPI().getStructures(folder, APILocator.getUserAPI().getSystemUser(), false);
            		String hostForDummyFolder = "SYSTEM_HOST";

            		if (referencedStructures!=null && !referencedStructures.isEmpty()) {
						Structure st = referencedStructures.get(0);
						hostForDummyFolder = st.getHost();
					}

            		// 1.2) Insert dummy temp row on IDENTIFIER table

            		dc.executeStatement("insert into identifier values ('TEMP_IDENTIFIER', '/System folder', 'DUMMY_ASSET_NAME', '"+hostForDummyFolder+"', "
            				+ "'folder', NULL, NULL) ");

            		// 1.3) Insert dummy temp row on FOLDER table

                    if ( DbConnectionFactory.isOracle() ) {
                        dc.executeStatement("insert into folder values ('TEMP_INODE', 'DUMMY_NAME', 'DUMMY_TITLE', '"+DbConnectionFactory.getDBFalse()+"', '0', '', 'TEMP_IDENTIFIER', '"+ fileAssetSt.getInode()+ "', to_date('1900-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'))");
                    } else if(DbConnectionFactory.isPostgres()) {
                        dc.executeStatement("insert into folder values ('TEMP_INODE', 'DUMMY_NAME', 'DUMMY_TITLE', "+DbConnectionFactory.getDBFalse()+", '0', '', 'TEMP_IDENTIFIER', '"+ fileAssetSt.getInode()+ "', '1900-01-01 00:00:00.00')");
                    } else {
                        dc.executeStatement("insert into folder values ('TEMP_INODE', 'DUMMY_NAME', 'DUMMY_TITLE', '"+DbConnectionFactory.getDBFalse()+"', '0', '', 'TEMP_IDENTIFIER', '"+ fileAssetSt.getInode()+ "', '1900-01-01 00:00:00.00')");
                    }

                    // 2) Update references to the new dummies temps

            		// update foreign tables references to TEMP
            		dc.executeStatement("update structure set folder = 'TEMP_INODE' where folder = '"+oldFolderInode+"'");
            		dc.executeStatement("update permission set inode_id = 'TEMP_INODE' where inode_id = '"+oldFolderInode+"'");
            		dc.executeStatement("update permission_reference set asset_id = 'TEMP_INODE' where asset_id = '"+oldFolderInode+"'");

            		// 3.1) delete old FOLDER row
            		// lets save old folder columns values first
            		dc.setSQL("select * from folder where inode = ?");
            		dc.addParam(oldFolderInode);
            		Map<String, Object> oldFolderRow = dc.loadObjectResults().get(0);
            		String name = (String) oldFolderRow.get("name");
            		String title = (String) oldFolderRow.get("title");
            		Boolean showOnMenu = DbConnectionFactory.isDBTrue(oldFolderRow.get("show_on_menu").toString());

                    Integer sortOrder = 0;
                    if ( oldFolderRow.get( "sort_order" ) != null ) {
                        sortOrder = Integer.valueOf( oldFolderRow.get( "sort_order" ).toString() );
                    }

                    String filesMasks = (String) oldFolderRow.get("files_masks");
            		String defaultFileType = (String) oldFolderRow.get("default_file_type");
            		Date modDate = (Date) oldFolderRow.get("mod_date");

            		// lets save old identifier columns values first
            		dc.setSQL("select * from identifier where id = ?");
            		dc.addParam(oldFolderIdentifier);
            		Map<String, Object> oldIdentifierRow = dc.loadObjectResults().get(0);
            		String parentPath = (String) oldIdentifierRow.get("parent_path");
            		String assetName = (String) oldIdentifierRow.get("asset_name");
            		String hostId = (String) oldIdentifierRow.get("host_inode");
            		String assetType = (String) oldIdentifierRow.get("asset_type");
            		Date syspublishDate = (Date) oldIdentifierRow.get("syspublish_date");
            		Date sysexpireDate = (Date) oldIdentifierRow.get("sysexpire_date");

            		// now we can safely delete the old folder row. It will also delete the old Identifier
            		// lets alter the asset_name to avoid errors in validation when deleting the folder
            		dc.executeStatement("update identifier set asset_name = '_TO_BE_DELETED_' where id = '" + oldFolderIdentifier + "'");

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
            		dc.setSQL("insert into inode values (?, ?, ?, ?) ");
            		dc.addParam(newFolderInode);
            		dc.addParam(owner);
            		dc.addParam(idate);
            		dc.addParam(type);
            		dc.loadResult();

            		// 4.2) insert real new IDENTIFIER row
            		dc.setSQL("insert into identifier values (?, ?, ?, ?, ?, ?, ?) ");
            		dc.addParam(newFolderIdentifier);
            		dc.addParam(parentPath);
            		dc.addParam(assetName);
            		dc.addParam(hostId);
            		dc.addParam(assetType);
            		dc.addParam(syspublishDate);
            		dc.addParam(sysexpireDate);
            		dc.loadResult();

            		// 4.3) insert real new FOLDER row
            		dc.setSQL("insert into folder values (?, ?, ?, ?, ?, ?, ?, ?, ?) ");
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
            		dc.executeStatement("update structure set folder = '"+newFolderInode+"' where folder = 'TEMP_INODE'");
            		dc.executeStatement("update permission set inode_id = '"+newFolderInode+"' where inode_id = 'TEMP_INODE'");
            		dc.executeStatement("update permission_reference set asset_id = '"+newFolderInode+"' where asset_id = 'TEMP_INODE'");

            		// 6) delete dummy temp
            		dc.executeStatement("delete from folder where inode = 'TEMP_INODE'");
            		dc.executeStatement("delete from inode where inode = 'TEMP_INODE'");


            	} catch (DotDataException e) {
            		Logger.info(getClass(), "Folder not found. inode: " + oldFolderInode);
            	}
			}

            // lets reindex all the content under each fixed folder

        	dc.setSQL("select remote_inode from " + tableName + " where endpoint_id = ?");
            dc.addParam(serverId);
            results = dc.loadObjectResults();

            for (Map<String, Object> result : results) {
            	final String newFolderInode = (String) result.get("remote_inode");
            	try {


            		HibernateUtil.addCommitListener(new Runnable() {
            			public void run() {
            				Folder folder = null;
							try {
								folder = APILocator.getFolderAPI().find(newFolderInode, APILocator.getUserAPI().getSystemUser(), false);
            					APILocator.getContentletAPI().refreshContentUnderFolder(folder);
            				} catch (DotStateException | DotDataException | DotSecurityException e) {
            					Logger.error(this,"Error while reindexing content under folder with inode: " + folder!=null?folder.getInode():"null",e);
            				}
            			}
            		});

            	} catch (DotDataException e) {
            		Logger.info(getClass(), "Folder not found. inode: " + newFolderInode);
            	}

			}

            discardConflicts(serverId, IntegrityType.FOLDERS);

        } catch ( SQLException e ) {
            throw new DotDataException( e.getMessage(), e );
        }
    }


    /**
     * Fixes structures inconsistencies for a given server id
     *
     * @param serverId
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public void fixStructures ( String serverId ) throws DotDataException, DotSecurityException {

        DotConnect dc = new DotConnect();
        String tableName = getResultsTableName( IntegrityType.STRUCTURES );

        try {

            dc.setSQL("select local_inode, remote_inode from " + tableName + " where endpoint_id = ?");
            dc.addParam(serverId);
            List<Map<String,Object>> results = dc.loadObjectResults();

            for (Map<String, Object> result : results) {
            	String oldStructureInode = (String) result.get("local_inode");
            	String newStructureInode = (String) result.get("remote_inode");

				Structure st = StructureCache.getStructureByInode(oldStructureInode);

				List<Contentlet> contents = APILocator.getContentletAPI().findByStructure(st, APILocator.getUserAPI().getSystemUser(), false, 0, 0);
				for (Contentlet contentlet : contents) {
					CacheLocator.getContentletCache().remove(contentlet.getInode());
				}

				StructureCache.removeStructure(st);


				// THIS IS THE NEW CODE

                // 1.1) Insert dummy temp row on INODE table
                if ( DbConnectionFactory.isOracle() ) {
                    dc.executeStatement("insert into inode values ('TEMP_INODE', 'DUMMY_OWNER', to_date('1900-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'DUMMY_TYPE') ");
                } else {
                    dc.executeStatement("insert into inode values ('TEMP_INODE', 'DUMMY_OWNER', '1900-01-01 00:00:00.00', 'DUMMY_TYPE') ");
                }

                // 1.2) Insert dummy temp row on STRUCTURE table

                if ( DbConnectionFactory.isOracle() ) {
                    dc.executeStatement("insert into structure values ('TEMP_INODE', 'DUMMY_NAME', 'DUMMY_DESC', '"+DbConnectionFactory.getDBFalse()+"', '', '', '', 1, '"+DbConnectionFactory.getDBTrue()+"', '"+DbConnectionFactory.getDBFalse()+"', 'DUMMY_VAR_NAME'"
                            + ", 'DUMMY_PATERN', '"+st.getHost()+"', '"+st.getFolder()+"', 'EXPIRE_DUMMY', 'PUBLISH_DUMMY', to_date('1900-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'))");
                } else if ( DbConnectionFactory.isPostgres() ) {
                    dc.executeStatement( "insert into structure values ('TEMP_INODE', 'DUMMY_NAME', 'DUMMY_DESC', " + DbConnectionFactory.getDBFalse() + ", '', '', '', 1, " + DbConnectionFactory.getDBTrue() + ", " + DbConnectionFactory.getDBFalse() + ", 'DUMMY_VAR_NAME'"
                            + ", 'DUMMY_PATERN', '" + st.getHost() + "', '" + st.getFolder() + "', 'EXPIRE_DUMMY', 'PUBLISH_DUMMY', '1900-01-01 00:00:00.00')" );
                } else {
                    dc.executeStatement("insert into structure values ('TEMP_INODE', 'DUMMY_NAME', 'DUMMY_DESC', '"+DbConnectionFactory.getDBFalse()+"', '', '', '', 1, '"+DbConnectionFactory.getDBTrue()+"', '"+DbConnectionFactory.getDBFalse()+"', 'DUMMY_VAR_NAME'"
                            + ", 'DUMMY_PATERN', '"+st.getHost()+"', '"+st.getFolder()+"', 'EXPIRE_DUMMY', 'PUBLISH_DUMMY', '1900-01-01 00:00:00.00')");
                }

                // 2) Update references to the new dummies temps

        		// update foreign tables references to TEMP
        		dc.executeStatement("update container_structures set structure_id = 'TEMP_INODE' where structure_id = '"+oldStructureInode+"'");
        		dc.executeStatement("update contentlet set structure_inode = 'TEMP_INODE' where structure_inode = '"+oldStructureInode+"'");
        		dc.executeStatement("update field set structure_inode = 'TEMP_INODE' where structure_inode = '"+oldStructureInode+"'");
        		dc.executeStatement("update relationship set parent_structure_inode = 'TEMP_INODE' where parent_structure_inode = '"+oldStructureInode+"'");
        		dc.executeStatement("update relationship set child_structure_inode = 'TEMP_INODE' where child_structure_inode = '"+oldStructureInode+"'");
        		dc.executeStatement("update workflow_scheme_x_structure set structure_id = 'TEMP_INODE' where structure_id = '"+oldStructureInode+"'");
        		dc.executeStatement("update permission set inode_id = 'TEMP_INODE' where inode_id = '"+oldStructureInode+"'");
        		dc.executeStatement("update permission_reference set asset_id = 'TEMP_INODE' where asset_id = '"+oldStructureInode+"'");

        		// 3.1) delete old STRUCTURE row
        		// lets save old structure columns values first
        		dc.setSQL("select * from structure where inode = ?");
        		dc.addParam(oldStructureInode);
        		Map<String, Object> oldFolderRow = dc.loadObjectResults().get(0);
        		String name = (String) oldFolderRow.get("name");
        		String description = (String) oldFolderRow.get("description");
        		Boolean defaultStructure = DbConnectionFactory.isDBTrue(oldFolderRow.get("default_structure").toString());
        		String reviewInterval = (String) oldFolderRow.get("review_interval");
        		String reviewerRole = (String) oldFolderRow.get("reviewer_role");
        		String detailPage = (String) oldFolderRow.get("page_detail");

                Integer structureType = null;
                if ( oldFolderRow.get( "structuretype" ) != null ) {
                    structureType = Integer.valueOf( oldFolderRow.get( "structuretype" ).toString() );
                }

        		Boolean system = DbConnectionFactory.isDBTrue(oldFolderRow.get("system").toString());
        		Boolean fixed = DbConnectionFactory.isDBTrue(oldFolderRow.get("fixed").toString());
        		String velocityVarName = (String) oldFolderRow.get("velocity_var_name");
        		String urlMapPattern = (String) oldFolderRow.get("url_map_pattern");
        		String host = (String) oldFolderRow.get("host");
        		String folder = (String) oldFolderRow.get("folder");
        		String expireDateVar = (String) oldFolderRow.get("expire_date_var");
        		String publishDateVar = (String) oldFolderRow.get("publish_date_var");
        		Date modDate = (Date) oldFolderRow.get("mod_date");

        		dc.executeStatement("delete from structure where inode = '" + oldStructureInode + "'");

        		// 3.2) delete old INODE row

        		dc.setSQL("select * from inode where inode = ?");
        		dc.addParam(oldStructureInode);
        		Map<String, Object> oldInodeRow = dc.loadObjectResults().get(0);
        		String owner = (String) oldInodeRow.get("owner");
        		Date idate = (Date) oldInodeRow.get("idate");
        		String type = (String) oldInodeRow.get("type");

        		dc.executeStatement("delete from inode where inode = '" + oldStructureInode + "'");



        		// 4.1) insert real new INODE row
        		dc.setSQL("insert into inode values (?, ?, ?, ?) ");
        		dc.addParam(newStructureInode);
        		dc.addParam(owner);
        		dc.addParam(idate);
        		dc.addParam(type);
        		dc.loadResult();

        		// 4.2) insert real new STRUCTURE row
        		dc.setSQL("insert into structure values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ");
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
        		dc.executeStatement("update container_structures set structure_id = '"+newStructureInode+"' where structure_id = 'TEMP_INODE'");
        		dc.executeStatement("update contentlet set structure_inode = '"+newStructureInode+"' where structure_inode = 'TEMP_INODE'");
        		dc.executeStatement("update field set structure_inode = '"+newStructureInode+"' where structure_inode = 'TEMP_INODE'");
        		dc.executeStatement("update relationship set parent_structure_inode = '"+newStructureInode+"' where parent_structure_inode = 'TEMP_INODE'");
        		dc.executeStatement("update relationship set child_structure_inode = '"+newStructureInode+"' where child_structure_inode = 'TEMP_INODE'");
        		dc.executeStatement("update workflow_scheme_x_structure set structure_id = '"+newStructureInode+"' where structure_id = 'TEMP_INODE'");
        		dc.executeStatement("update permission set inode_id = '"+newStructureInode+"' where inode_id = 'TEMP_INODE'");
        		dc.executeStatement("update permission_reference set asset_id = '"+newStructureInode+"' where asset_id = 'TEMP_INODE'");

        		// 6) delete dummy temp
        		dc.executeStatement("delete from structure where inode = 'TEMP_INODE'");
        		dc.executeStatement("delete from inode where inode = 'TEMP_INODE'");
			}

            discardConflicts(serverId, IntegrityType.STRUCTURES);

        } catch ( SQLException e ) {
            throw new DotDataException( e.getMessage(), e );
        }
    }

    /**
     * Fixes worflow schemes inconsistencies for a given server id
     *
     * @param serverId
     * @throws DotDataException
     */
    public void fixSchemes ( String serverId ) throws DotDataException {

        DotConnect dc = new DotConnect();
        String tableName = getResultsTableName( IntegrityType.SCHEMES );

        try {

            //Delete the schemes cache
            dc.setSQL( "SELECT local_inode, remote_inode FROM " + tableName + " WHERE endpoint_id = ?" );
            dc.addParam( serverId );
            List<Map<String, Object>> results = dc.loadObjectResults();
            for ( Map<String, Object> result : results ) {

            	String oldWorkflowId = (String) result.get( "local_inode" );
                String newWorkflowId = (String) result.get( "remote_inode" );

                WorkflowCache workflowCache = CacheLocator.getWorkFlowCache();
                //Verify if the workflow is the default one
                WorkflowScheme defaultScheme = workflowCache.getDefaultScheme();
                if ( defaultScheme != null && defaultScheme.getId().equals( oldWorkflowId ) ) {
                    CacheLocator.getCacheAdministrator().remove( workflowCache.defaultKey, workflowCache.getPrimaryGroup() );
                } else {
                    //Clear the cache
                    WorkflowScheme oldScheme = APILocator.getWorkflowAPI().findScheme(oldWorkflowId);
                    List<WorkflowStep> steps = APILocator.getWorkflowAPI().findSteps(oldScheme);

                    for (int i = 0; i < steps.size(); i++) {
                        WorkflowStep workflowStep =  steps.get(i);
                        workflowCache.remove(workflowStep);
                    }

                    WorkflowScheme scheme = workflowCache.getScheme( oldWorkflowId );
                    workflowCache.remove( scheme );
                }

             // THIS IS THE NEW CODE

        		// 1) Insert dummy temp row on WORKFLOW_SCHEME table

                if ( DbConnectionFactory.isOracle() ) {
                    dc.executeStatement("insert into workflow_scheme values ('TEMP_INODE', 'DUMMY_NAME', 'DUMMY_DESC', '"+DbConnectionFactory.getDBFalse()+"', '"+DbConnectionFactory.getDBFalse()+"', '"+DbConnectionFactory.getDBFalse()+"', '', to_date('1900-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'))");
                } else if(DbConnectionFactory.isPostgres()) {
                    dc.executeStatement( "insert into workflow_scheme values ('TEMP_INODE', 'DUMMY_NAME', 'DUMMY_DESC', " + DbConnectionFactory.getDBFalse() + ", " + DbConnectionFactory.getDBFalse() + ", " + DbConnectionFactory.getDBFalse() + ", '', '1900-01-01 00:00:00.00')" );
                } else {
                    dc.executeStatement("insert into workflow_scheme values ('TEMP_INODE', 'DUMMY_NAME', 'DUMMY_DESC', '"+DbConnectionFactory.getDBFalse()+"', '"+DbConnectionFactory.getDBFalse()+"', '"+DbConnectionFactory.getDBFalse()+"', '', '1900-01-01 00:00:00.00')");
                }

                // 2) Update references to the new dummies temps

        		// update foreign tables references to TEMP
        		dc.executeStatement("update workflow_step set scheme_id = 'TEMP_INODE' where scheme_id = '"+oldWorkflowId+"'");
        		dc.executeStatement("update workflow_scheme_x_structure set scheme_id = 'TEMP_INODE' where scheme_id = '"+oldWorkflowId+"'");

        		// 3) delete old WORKFLOW_SCHEME row
        		// lets save old scheme columns values first
        		dc.setSQL("select * from workflow_scheme where id = ?");
        		dc.addParam(oldWorkflowId);
        		Map<String, Object> oldFolderRow = dc.loadObjectResults().get(0);
        		String name = (String) oldFolderRow.get("name");
        		String desc = (String) oldFolderRow.get("description");
        		Boolean archived = DbConnectionFactory.isDBTrue(oldFolderRow.get("archived").toString());
        		Boolean mandatory = DbConnectionFactory.isDBTrue(oldFolderRow.get("mandatory").toString());
        		Boolean isDefaultScheme = DbConnectionFactory.isDBTrue(oldFolderRow.get("default_scheme").toString());
        		String entryActionId = (String) oldFolderRow.get("entry_action_id");
        		Date modDate = (Date) oldFolderRow.get("mod_date");

        		dc.executeStatement("delete from workflow_scheme where id = '" + oldWorkflowId + "'");


        		// 4) insert real new WORKFLOW_SCHEME row
        		dc.setSQL("insert into workflow_scheme values (?, ?, ?, ?, ?, ?, ?, ?) ");
        		dc.addParam(newWorkflowId);
        		dc.addParam(name);
        		dc.addParam(desc);
        		dc.addParam(archived);
        		dc.addParam(mandatory);
        		dc.addParam(isDefaultScheme);
        		dc.addParam(entryActionId);
        		dc.addParam(modDate);
        		dc.loadResult();

        		// 5) update foreign tables references to the new real row
        		dc.executeStatement("update workflow_step set scheme_id = '"+newWorkflowId+"' where scheme_id = 'TEMP_INODE'");
        		dc.executeStatement("update workflow_scheme_x_structure set scheme_id = '"+newWorkflowId+"' where scheme_id = 'TEMP_INODE'");

        		// 6) delete dummy temp
        		dc.executeStatement("delete from workflow_scheme where id = 'TEMP_INODE'");
            }

            discardConflicts(serverId, IntegrityType.SCHEMES);

        } catch ( SQLException e ) {
            throw new DotDataException( e.getMessage(), e );
        }
    }

	/**
	 * Replace Identifier with same Identifier from the other server. For the
	 * new Content Pages, it is necessary to take the following 2 situations
	 * into consideration:
	 * <ol>
	 * <li>If the page to fix only has one specified language, fix all existing
	 * versions.</li>
	 * <li>If the page to fix only has more than one language, fix the versions
	 * <b>ONE BY ONE</b>. Otherwise, the second time the next language is found,
	 * the old identifier will not be present anymore since it was already
	 * changed, which will cause an issue.</li>
	 * </ol>
	 *
	 * @param serverId
	 *            - The ID of the endpoint where the data will be fixed.
	 * @throws DotDataException
	 *             An error occurred when interacting with the database.
	 * @throws DotSecurityException
	 *             The current user does not have permission to perform this
	 *             ation.
	 */
    public void fixHtmlPages ( String serverId ) throws DotDataException, DotSecurityException {
    	DotConnect dc = new DotConnect();
        String tableName = getResultsTableName( IntegrityType.HTMLPAGES );
        //Get the information of the IR.
		dc.setSQL( "SELECT html_page, local_identifier, remote_identifier, local_working_inode, remote_working_inode, local_live_inode, remote_live_inode, language_id FROM " + tableName + " WHERE endpoint_id = ?" );
		dc.addParam( serverId );
		List<Map<String, Object>> results = dc.loadObjectResults();
		Map<String, Integer> versionCount = new HashMap<String, Integer>();
		for (Map<String, Object> result : results) {
			String oldIdentifier = (String) result.get("local_identifier");
			if (versionCount.containsKey(oldIdentifier)) {
				int counter = versionCount.get(oldIdentifier);
				versionCount.put(oldIdentifier, counter + 1);
			} else {
				versionCount.put(oldIdentifier, 1);
			}
		}
		for (Map<String, Object> result : results) {
			String oldIdentifier = (String) result.get("local_identifier");
			Identifier identifier = APILocator.getIdentifierAPI().find(
					oldIdentifier);
			if ("htmlpage".equals(identifier.getAssetType())) {
				fixLegacyPageConflicts(result);
			} else {
				int counter = versionCount.get(oldIdentifier);
				boolean fixAllVersions = counter == 1 ? true : false;
				fixContentPageConflicts(result, fixAllVersions);
				if (!fixAllVersions) {
					// Decrease version counter if greater than 1
					versionCount.put(oldIdentifier, counter - 1);
				}
			}
		}
		discardConflicts(serverId, IntegrityType.HTMLPAGES);
    }

	/**
	 * Directly updates the information of a given HTML Page - i.e., the legacy
	 * {@link HTMLPage} - to resolve the conflict (two pages with same path but
	 * different identifier) found in the receiver server before a push publish
	 * is triggered.
	 * <p>
	 * This method is the same for solving both local and remote conflicts. The
	 * only difference is in what server (either the sender or the receiver)
	 * this method is called.
	 * </p>
	 *
	 * @param pageData
	 *            - A {@link Map} with the page information that was generated
	 *            when the conflict was detected.
	 * @throws DotDataException
	 *             An error occurred when interacting with the database.
	 */
	private void fixLegacyPageConflicts(Map<String, Object> pageData)
			throws DotDataException {
		HTMLPageCache htmlPageCache = CacheLocator.getHTMLPageCache();
		DotConnect dc = new DotConnect();
		String oldHtmlPageIdentifier = (String) pageData
				.get("local_identifier");
		String newHtmlPageIdentifier = (String) pageData
				.get("remote_identifier");
		String assetName = (String) pageData.get("html_page");
		String localInode = (String) pageData.get("local_working_inode");
		// We need only the last part of the url, not the whole path.
		String[] assetNamebits = assetName.split("/");
		assetName = assetNamebits[assetNamebits.length - 1];
		htmlPageCache.remove(oldHtmlPageIdentifier);
		CacheLocator.getIdentifierCache().removeFromCacheByInode(localInode);
		// Fixing by SQL queries
		dc.setSQL("INSERT INTO identifier(id, parent_path, asset_name, host_inode, asset_type, syspublish_date, sysexpire_date) "
				+ "SELECT ? , parent_path, 'TEMP_ASSET_NAME', host_inode, asset_type, syspublish_date, sysexpire_date "
				+ "FROM identifier WHERE id = ?");
		dc.addParam(newHtmlPageIdentifier);
		dc.addParam(oldHtmlPageIdentifier);
		dc.loadResult();
		dc.setSQL("UPDATE htmlpage " + "SET identifier = ? "
				+ "WHERE identifier = ?");
		dc.addParam(newHtmlPageIdentifier);
		dc.addParam(oldHtmlPageIdentifier);
		dc.loadResult();
		dc.setSQL("UPDATE htmlpage_version_info " + "SET identifier = ? "
				+ "WHERE identifier = ?");
		dc.addParam(newHtmlPageIdentifier);
		dc.addParam(oldHtmlPageIdentifier);
		dc.loadResult();
		dc.setSQL("DELETE FROM identifier " + "WHERE id = ?");
		dc.addParam(oldHtmlPageIdentifier);
		dc.loadResult();
		dc.setSQL("UPDATE identifier " + "SET asset_name = ? " + "WHERE id = ?");
		dc.addParam(assetName);
		dc.addParam(newHtmlPageIdentifier);
		dc.loadResult();
		dc.setSQL("UPDATE multi_tree " + "SET parent1 = ? "
				+ "WHERE parent1 = ?");
		dc.addParam(newHtmlPageIdentifier);
		dc.addParam(oldHtmlPageIdentifier);
		dc.loadResult();
	}

	/**
	 * Directly updates the information of a given Content Page - i.e., the new
	 * {@link IHTMLPage} - to resolve the conflict (two pages with same path but
	 * different identifier) found in the receiver server before a push publish
	 * is triggered. This new HTML page is a specialized form of the
	 * {@link Contentlet} class.
	 * <p>
	 * This method is the same for solving both local and remote conflicts. The
	 * only difference is in what server (either the sender or the receiver)
	 * this method is called and the distribution of data fields, which is
	 * handled by a previous method. Bearing this in mind, the conflict
	 * resolution process performs the following plain SQL queries (the
	 * execution order is very important to avoid foreign key conflicts):
	 * <ol>
	 * <li>Create the <code>Inode</code> and <code>Identifier</code> records,
	 * which <b>MUST EXIST</b> before a contentlet (content page) can be
	 * created. Initially, the <code>asset_Name</code> of the new Identifier
	 * will have a temporary name.</li>
	 * <li>Create the new <code>Contentlet</code> and
	 * <code>Contentlet_Version_Info</code> records <b>WITHOUT DELETING</b> the
	 * old page records. Otherwise, exceptions will be thrown regarding foreign
	 * key constraints with several other tables.</li>
	 * <li>Delete the old <code>Contentlet_Version_Info</code> and
	 * <code>Contentlet</code> records.</li>
	 * <li>Update all the existing versions of the page in the
	 * <code>Contentlet</code> table so they point to the new identifier.</li>
	 * <li>Delete the old <code>Identifier</code> and <code>Inode</code>
	 * records.</li>
	 * <li>Update the <code>Identifier</code> record with the correct
	 * <code>asset_name</code>.</li>
	 * <li>Update the <code>Multi_Tree</code> records so that the change in the
	 * identifier does not cause the page content to be missing.</li>
	 * <li>Remove the old page entries from <code>Contentlet</code> and
	 * <code>Identifier</code> cache. This also includes removing the entries
	 * for any existing versions of the page so that older versions of the page
	 * can be brought back under the new identifier without any errors.</li>
	 * </ol>
	 * </p>
	 *
	 * @param pageData
	 *            - A {@link Map} with the page information that was captured
	 *            when the conflict was detected.
	 * @param fixAllVersions
	 *            - If <code>true</code>, all existing versions of the page will
	 *            be updated in order to keep data consistency. Otherwise, ONLY
	 *            the specified page and language will be updated.
	 * @throws DotDataException
	 *             An error occurred when interacting with the database.
	 * @throws DotSecurityException
	 *             The current user does not have permission to perform this
	 *             action.
	 */
	private void fixContentPageConflicts(Map<String, Object> pageData,
			boolean fixAllVersions) throws DotDataException,
			DotSecurityException {
		DotConnect dc = new DotConnect();
		String oldHtmlPageIdentifier = (String) pageData
				.get("local_identifier");
		String newHtmlPageIdentifier = (String) pageData
				.get("remote_identifier");
		String assetName = (String) pageData.get("html_page");
		String localWorkingInode = (String) pageData.get("local_working_inode");
		String localLiveInode = (String) pageData.get("local_live_inode");
		String remoteWorkingInode = (String) pageData.get("remote_working_inode");
		String remoteLiveInode = (String) pageData.get("remote_live_inode");

        Long languageId;
        if (DbConnectionFactory.isOracle()) {
            BigDecimal lang = (BigDecimal) pageData.get("language_id");
            languageId = new Long(lang.toPlainString());
        } else {
            languageId = (Long) pageData.get("language_id");
        }

        String[] assetUrl = assetName.split("/");
		assetName = assetUrl[assetUrl.length - 1];
		ContentletAPI contentletAPI = APILocator.getContentletAPI();
		ContentletIndexAPI indexAPI = APILocator.getContentletIndexAPI();
		User systemUser = APILocator.getUserAPI().getSystemUser();
		Contentlet existingWorkingContentPage = contentletAPI.find(localWorkingInode,systemUser, false);
        Contentlet existingLiveContentPage = null;

        try {
            existingLiveContentPage = contentletAPI.find(localLiveInode, systemUser, false);
        } catch(DotHibernateException e) { /*No Live Version*/ }

		dc.setSQL("SELECT id FROM identifier WHERE id = ?");
		dc.addParam(newHtmlPageIdentifier);
		List<Map<String, Object>> results = dc.loadObjectResults();
		// If not existing, add the new Identifier with a temporary asset name
		if (results == null || results.size() == 0) {
			dc.setSQL("INSERT INTO identifier(id, parent_path, asset_name, host_inode, asset_type, syspublish_date, sysexpire_date) "
					+ "SELECT ? , parent_path, 'TEMP_CONTENTPAGE_NAME', host_inode, asset_type, syspublish_date, sysexpire_date "
					+ "FROM identifier WHERE id = ?");
			dc.addParam(newHtmlPageIdentifier);
			dc.addParam(oldHtmlPageIdentifier);
			dc.loadResult();
		}
		// Insert the new Inodes records so it can be used in the contentlet
		dc.setSQL("INSERT INTO inode(inode, owner, idate, type) "
				+ "SELECT ?, owner, idate, type "
				+ "FROM inode i WHERE i.inode = ?");
		dc.addParam(remoteWorkingInode);
		dc.addParam(localWorkingInode);
        dc.loadResult();

        if(!remoteWorkingInode.equals(remoteLiveInode) && UtilMethods.isSet(remoteLiveInode) && UtilMethods.isSet(localLiveInode)) {
            dc.setSQL("INSERT INTO inode(inode, owner, idate, type) "
                    + "SELECT ?, owner, idate, type "
                    + "FROM inode i WHERE i.inode = ?");
            dc.addParam(remoteLiveInode);
            dc.addParam(localLiveInode);
            dc.loadResult();
        }

		// Insert the new working Contentlet record with the new Inode
		String contentletQuery = "INSERT INTO contentlet(inode, show_on_menu, title, mod_date, mod_user, sort_order, friendly_name, structure_inode, last_review, next_review, review_interval, disabled_wysiwyg, identifier, language_id, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12, date13, date14, date15, date16, date17, date18, date19, date20, date21, date22, date23, date24, date25, text1, text2, text3, text4, text5, text6, text7, text8, text9, text10, text11, text12, text13, text14, text15, text16, text17, text18, text19, text20, text21, text22, text23, text24, text25, text_area1, text_area2, text_area3, text_area4, text_area5, text_area6, text_area7, text_area8, text_area9, text_area10, text_area11, text_area12, text_area13, text_area14, text_area15, text_area16, text_area17, text_area18, text_area19, text_area20, text_area21, text_area22, text_area23, text_area24, text_area25, integer1, integer2, integer3, integer4, integer5, integer6, integer7, integer8, integer9, integer10, integer11, integer12, integer13, integer14, integer15, integer16, integer17, integer18, integer19, integer20, integer21, integer22, integer23, integer24, integer25, \"float1\", \"float2\", \"float3\", \"float4\", \"float5\", \"float6\", \"float7\", \"float8\", \"float9\", \"float10\", \"float11\", \"float12\", \"float13\", \"float14\", \"float15\", \"float16\", \"float17\", \"float18\", \"float19\", \"float20\", \"float21\", \"float22\", \"float23\", \"float24\", \"float25\", bool1, bool2, bool3, bool4, bool5, bool6, bool7, bool8, bool9, bool10, bool11, bool12, bool13, bool14, bool15, bool16, bool17, bool18, bool19, bool20, bool21, bool22, bool23, bool24, bool25) "
				+ "SELECT ?, show_on_menu, title, mod_date, mod_user, sort_order, friendly_name, structure_inode, last_review, next_review, review_interval, disabled_wysiwyg, ?, ?, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12, date13, date14, date15, date16, date17, date18, date19, date20, date21, date22, date23, date24, date25, text1, text2, text3, text4, text5, text6, text7, text8, text9, text10, text11, text12, text13, text14, text15, text16, text17, text18, text19, text20, text21, text22, text23, text24, text25, text_area1, text_area2, text_area3, text_area4, text_area5, text_area6, text_area7, text_area8, text_area9, text_area10, text_area11, text_area12, text_area13, text_area14, text_area15, text_area16, text_area17, text_area18, text_area19, text_area20, text_area21, text_area22, text_area23, text_area24, text_area25, integer1, integer2, integer3, integer4, integer5, integer6, integer7, integer8, integer9, integer10, integer11, integer12, integer13, integer14, integer15, integer16, integer17, integer18, integer19, integer20, integer21, integer22, integer23, integer24, integer25, \"float1\", \"float2\", \"float3\", \"float4\", \"float5\", \"float6\", \"float7\", \"float8\", \"float9\", \"float10\", \"float11\", \"float12\", \"float13\", \"float14\", \"float15\", \"float16\", \"float17\", \"float18\", \"float19\", \"float20\", \"float21\", \"float22\", \"float23\", \"float24\", \"float25\", bool1, bool2, bool3, bool4, bool5, bool6, bool7, bool8, bool9, bool10, bool11, bool12, bool13, bool14, bool15, bool16, bool17, bool18, bool19, bool20, bool21, bool22, bool23, bool24, bool25 "
				+ "FROM contentlet c INNER JOIN contentlet_version_info cvi on (c.inode = cvi.working_inode) WHERE c.identifier = ? and c.language_id = ?";
		if (DbConnectionFactory.isMySql()) {
			// Use correct escape char when using reserved words as column names
			contentletQuery = contentletQuery.replaceAll("\"", "`");
		}
		dc.setSQL(contentletQuery);
		dc.addParam(remoteWorkingInode);
		dc.addParam(newHtmlPageIdentifier);
		dc.addParam(languageId);
		dc.addParam(oldHtmlPageIdentifier);
		dc.addParam(languageId);
		dc.loadResult();

        if(!remoteWorkingInode.equals(remoteLiveInode)  && UtilMethods.isSet(remoteLiveInode) && UtilMethods.isSet(localLiveInode)) {
            contentletQuery = "INSERT INTO contentlet(inode, show_on_menu, title, mod_date, mod_user, sort_order, friendly_name, structure_inode, last_review, next_review, review_interval, disabled_wysiwyg, identifier, language_id, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12, date13, date14, date15, date16, date17, date18, date19, date20, date21, date22, date23, date24, date25, text1, text2, text3, text4, text5, text6, text7, text8, text9, text10, text11, text12, text13, text14, text15, text16, text17, text18, text19, text20, text21, text22, text23, text24, text25, text_area1, text_area2, text_area3, text_area4, text_area5, text_area6, text_area7, text_area8, text_area9, text_area10, text_area11, text_area12, text_area13, text_area14, text_area15, text_area16, text_area17, text_area18, text_area19, text_area20, text_area21, text_area22, text_area23, text_area24, text_area25, integer1, integer2, integer3, integer4, integer5, integer6, integer7, integer8, integer9, integer10, integer11, integer12, integer13, integer14, integer15, integer16, integer17, integer18, integer19, integer20, integer21, integer22, integer23, integer24, integer25, \"float1\", \"float2\", \"float3\", \"float4\", \"float5\", \"float6\", \"float7\", \"float8\", \"float9\", \"float10\", \"float11\", \"float12\", \"float13\", \"float14\", \"float15\", \"float16\", \"float17\", \"float18\", \"float19\", \"float20\", \"float21\", \"float22\", \"float23\", \"float24\", \"float25\", bool1, bool2, bool3, bool4, bool5, bool6, bool7, bool8, bool9, bool10, bool11, bool12, bool13, bool14, bool15, bool16, bool17, bool18, bool19, bool20, bool21, bool22, bool23, bool24, bool25) "
                    + "SELECT ?, show_on_menu, title, mod_date, mod_user, sort_order, friendly_name, structure_inode, last_review, next_review, review_interval, disabled_wysiwyg, ?, ?, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12, date13, date14, date15, date16, date17, date18, date19, date20, date21, date22, date23, date24, date25, text1, text2, text3, text4, text5, text6, text7, text8, text9, text10, text11, text12, text13, text14, text15, text16, text17, text18, text19, text20, text21, text22, text23, text24, text25, text_area1, text_area2, text_area3, text_area4, text_area5, text_area6, text_area7, text_area8, text_area9, text_area10, text_area11, text_area12, text_area13, text_area14, text_area15, text_area16, text_area17, text_area18, text_area19, text_area20, text_area21, text_area22, text_area23, text_area24, text_area25, integer1, integer2, integer3, integer4, integer5, integer6, integer7, integer8, integer9, integer10, integer11, integer12, integer13, integer14, integer15, integer16, integer17, integer18, integer19, integer20, integer21, integer22, integer23, integer24, integer25, \"float1\", \"float2\", \"float3\", \"float4\", \"float5\", \"float6\", \"float7\", \"float8\", \"float9\", \"float10\", \"float11\", \"float12\", \"float13\", \"float14\", \"float15\", \"float16\", \"float17\", \"float18\", \"float19\", \"float20\", \"float21\", \"float22\", \"float23\", \"float24\", \"float25\", bool1, bool2, bool3, bool4, bool5, bool6, bool7, bool8, bool9, bool10, bool11, bool12, bool13, bool14, bool15, bool16, bool17, bool18, bool19, bool20, bool21, bool22, bool23, bool24, bool25 "
                    + "FROM contentlet c INNER JOIN contentlet_version_info cvi on (c.inode = cvi.live_inode) WHERE c.identifier = ? and c.language_id = ?";

            dc.setSQL(contentletQuery);
            dc.addParam(remoteLiveInode);
            dc.addParam(newHtmlPageIdentifier);
            dc.addParam(languageId);
            dc.addParam(oldHtmlPageIdentifier);
            dc.addParam(languageId);
            dc.loadResult();
        }

		// Insert the new Contentlet_version_info record with the new Inode

        if(UtilMethods.isSet(remoteLiveInode) && UtilMethods.isSet(localLiveInode)) {
            dc.setSQL("INSERT INTO contentlet_version_info(identifier, lang, working_inode, live_inode, deleted, locked_by, locked_on, version_ts) "
                    + "SELECT ?, ?, ?, ?, deleted, locked_by, locked_on, version_ts "
                    + "FROM contentlet_version_info WHERE identifier = ? AND working_inode = ? AND lang = ?");
            dc.addParam(newHtmlPageIdentifier);
            dc.addParam(languageId);
            dc.addParam(remoteWorkingInode);
            dc.addParam(remoteLiveInode);
            dc.addParam(oldHtmlPageIdentifier);
            dc.addParam(localWorkingInode);
            dc.addParam(languageId);
            dc.loadResult();
        } else  {
            dc.setSQL("INSERT INTO contentlet_version_info(identifier, lang, working_inode, live_inode, deleted, locked_by, locked_on, version_ts) "
                    + "SELECT ?, ?, ?, live_inode, deleted, locked_by, locked_on, version_ts "
                    + "FROM contentlet_version_info WHERE identifier = ? AND working_inode = ? AND lang = ?");
            dc.addParam(newHtmlPageIdentifier);
            dc.addParam(languageId);
            dc.addParam(remoteWorkingInode);
            dc.addParam(oldHtmlPageIdentifier);
            dc.addParam(localWorkingInode);
            dc.addParam(languageId);
            dc.loadResult();
        }

        // Remove the live_inode references from Contentlet_version_info
        dc.setSQL("DELETE FROM contentlet_version_info WHERE identifier = ? AND lang = ? AND working_inode = ?");
        dc.addParam(oldHtmlPageIdentifier);
        dc.addParam(languageId);
        dc.addParam(localWorkingInode);
        dc.loadResult();

		// Remove the conflicting version of the Contentlet record
		dc.setSQL("DELETE FROM contentlet WHERE identifier = ? AND inode = ? AND language_id = ?");
		dc.addParam(oldHtmlPageIdentifier);
		dc.addParam(localWorkingInode);
		dc.addParam(languageId);
		dc.loadResult();

        if(UtilMethods.isSet(localLiveInode) && UtilMethods.isSet(remoteLiveInode) && !localLiveInode.equals(localWorkingInode)) {
            // Remove the conflicting version of the Contentlet record
            dc.setSQL("DELETE FROM contentlet WHERE identifier = ? AND inode = ? AND language_id = ?");
            dc.addParam(oldHtmlPageIdentifier);
            dc.addParam(localLiveInode);
            dc.addParam(languageId);
            dc.loadResult();
        }

		// If fixing all versions, or last version of the same Identifier
		if (fixAllVersions) {
			// Update other Contentlet languages with new Identifier
			dc.setSQL("UPDATE contentlet SET identifier = ? WHERE identifier = ?");
			dc.addParam(newHtmlPageIdentifier);
			dc.addParam(oldHtmlPageIdentifier);
			dc.loadResult();
			// Update previous version of the Contentlet_version_info with new
			// Identifier
			dc.setSQL("UPDATE contentlet_version_info SET identifier = ? WHERE identifier = ?");
			dc.addParam(newHtmlPageIdentifier);
			dc.addParam(oldHtmlPageIdentifier);
			dc.loadResult();
			// Remove the old Identifier record
			dc.setSQL("DELETE FROM identifier WHERE id = ?");
			dc.addParam(oldHtmlPageIdentifier);
			dc.loadResult();
			// Update the Identifier with the correct asset name
			dc.setSQL("UPDATE identifier SET asset_name = ? WHERE id = ?");
			dc.addParam(assetName);
			dc.addParam(newHtmlPageIdentifier);
			dc.loadResult();
			// Update the content references in the page with the new Identifier
			dc.setSQL("UPDATE multi_tree SET parent1 = ? WHERE parent1 = ?");
			dc.addParam(newHtmlPageIdentifier);
			dc.addParam(oldHtmlPageIdentifier);
			dc.loadResult();
		} else {
			// Update other Contentlet languages with new Identifier
			dc.setSQL("UPDATE contentlet SET identifier = ? WHERE identifier = ? AND language_id = ?");
			dc.addParam(newHtmlPageIdentifier);
			dc.addParam(oldHtmlPageIdentifier);
			dc.addParam(languageId);
			dc.loadResult();
			// Update previous version of the Contentlet_version_info with new
			// Identifier
			dc.setSQL("UPDATE contentlet_version_info SET identifier = ? WHERE identifier = ? AND lang = ?");
			dc.addParam(newHtmlPageIdentifier);
			dc.addParam(oldHtmlPageIdentifier);
			dc.addParam(languageId);
		}
		// Remove the old Inode record
		dc.setSQL("DELETE FROM inode WHERE inode = ?");
		dc.addParam(localWorkingInode);
		dc.loadResult();

        if(UtilMethods.isSet(localLiveInode) && UtilMethods.isSet(remoteLiveInode) && !localLiveInode.equals(localWorkingInode)) {
            // Remove the old Inode record
            dc.setSQL("DELETE FROM inode WHERE inode = ?");
            dc.addParam(localLiveInode);
            dc.loadResult();
        }

		// Add a new Lucene index for the updated page
		Contentlet newWorkingContentPage = new Contentlet();
        newWorkingContentPage.setStructureInode(existingWorkingContentPage
				.getStructureInode());
		contentletAPI.copyProperties(newWorkingContentPage,
                existingWorkingContentPage.getMap());
        newWorkingContentPage.setIdentifier(newHtmlPageIdentifier);
        newWorkingContentPage.setInode(remoteWorkingInode);
		indexAPI.addContentToIndex(newWorkingContentPage);

        if(UtilMethods.isSet(localLiveInode) && !localLiveInode.equals(localWorkingInode)) {
            Contentlet newLiveContentPage = new Contentlet();
            newWorkingContentPage.setStructureInode(existingLiveContentPage
                    .getStructureInode());
            contentletAPI.copyProperties(newLiveContentPage,
                    existingLiveContentPage.getMap());
            newLiveContentPage.setIdentifier(newHtmlPageIdentifier);
            newLiveContentPage.setInode(remoteLiveInode);
            indexAPI.addContentToIndex(newLiveContentPage);
        }


		// Remove the Lucene index for the old page
		indexAPI.removeContentFromIndex(existingWorkingContentPage);

        if(UtilMethods.isSet(existingLiveContentPage) && !existingWorkingContentPage.getInode().equals(existingLiveContentPage.getInode())) {
            indexAPI.removeContentFromIndex(existingLiveContentPage);
        }

		// Clear cache entries of ALL versions of the Contentlet too
		CacheLocator.getContentletCache().remove(localWorkingInode);

        if(UtilMethods.isSet(localLiveInode) && !localWorkingInode.equals(localLiveInode)) {
            CacheLocator.getContentletCache().remove(localLiveInode);
        }

        CacheLocator.getIdentifierCache().removeFromCacheByInode(localWorkingInode);

        if(UtilMethods.isSet(localLiveInode) && !localWorkingInode.equals(localLiveInode)) {
            CacheLocator.getIdentifierCache().removeFromCacheByInode(localLiveInode);
        }
		// Refresh all or only language-specific versions of the page
		if (fixAllVersions) {
			dc.setSQL("SELECT inode FROM contentlet WHERE identifier = ?");
			dc.addParam(newHtmlPageIdentifier);
		} else {
			dc.setSQL("SELECT inode FROM contentlet WHERE identifier = ? AND language_id = ?");
			dc.addParam(newHtmlPageIdentifier);
			dc.addParam(languageId);
		}
		List<Map<String, Object>> versions = dc.loadObjectResults();
		for (Map<String, Object> result : versions) {
			String historyInode = (String) result.get("inode");
			CacheLocator.getContentletCache().remove(historyInode);
		}
	}

	/**
	 * Returns the correct numeric data type for the language ID, depending on
	 * the current database.
	 *
	 * @return The database-dependent keyword to represent the language ID.
	 */
	private String getIntegerKeyword() {
		String keyword = null;
		if (DbConnectionFactory.isH2()) {
			keyword = "bigint";
		} else if (DbConnectionFactory.isPostgres()) {
			keyword = "int8";
		} else if (DbConnectionFactory.isMySql()) {
			keyword = "bigint";
		} else if (DbConnectionFactory.isMsSql()) {
			keyword = "numeric(19,0)";
		} else if (DbConnectionFactory.isOracle()) {
			keyword = "number(19,0)";
		}
		return keyword;
	}

}
