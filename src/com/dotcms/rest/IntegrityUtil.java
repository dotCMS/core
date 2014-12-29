package com.dotcms.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageCache;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.business.WorkflowCache;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

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
     * Creates CSV file with HTML Pages information from End Point server. 
     * 
     * @param outputFile
     * @return
     * @throws DotDataException
     * @throws IOException
     */
    private File generateHtmlPagesToCheckCSV(String outputFile) throws DotDataException, IOException {
        Connection conn = DbConnectionFactory.getConnection();
        ResultSet rs = null;
        PreparedStatement statement = null;
        CsvWriter writer = null;
        File csvFile = null;

        try {
            csvFile = new File(outputFile);
            writer = new CsvWriter(new FileWriter(csvFile, true), '|');
            statement = conn.prepareStatement("select h.inode, h.identifier, i.parent_path, i.asset_name, i.host_inode from htmlpage h join identifier i on h.identifier = i.id");
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
                statement = conn.prepareStatement("select html_page, remote_inode, local_inode, remote_identifier, local_identifier from " + resultsTable + " where endpoint_id = ?");
            } else if(type == IntegrityType.FOLDERS) {
				statement = conn.prepareStatement("select remote_inode, local_inode, remote_identifier, local_identifier from " + resultsTable + " where endpoint_id = ?");
			} else {
				statement = conn.prepareStatement("select remote_inode, local_inode from " + resultsTable + " where endpoint_id = ?");
			}

            statement.setString(1, endpointId);
            rs = statement.executeQuery();
            int count = 0;

            while (rs.next()) {
                writer.write(rs.getString("remote_inode"));
                writer.write(rs.getString("local_inode"));

                if(type == IntegrityType.FOLDERS || type == IntegrityType.HTMLPAGES) {
                	writer.write(rs.getString("remote_identifier"));
                	writer.write(rs.getString("local_identifier"));
                }

                if(type == IntegrityType.HTMLPAGES) {
                    writer.write(rs.getString("html_page"));
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
            htmlPagesToCheckCsvFile = integrityUtil.generateHtmlPagesToCheckCSV(outputPath + File.separator + IntegrityType.HTMLPAGES.getDataToCheckCSVName());

            addToZipFile(foldersToCheckCsvFile.getAbsolutePath(), zos, IntegrityType.FOLDERS.getDataToCheckCSVName());
            addToZipFile(structuresToCheckCsvFile.getAbsolutePath(), zos, IntegrityType.STRUCTURES.getDataToCheckCSVName());
            addToZipFile(schemesToCheckCsvFile.getAbsolutePath(), zos, IntegrityType.SCHEMES.getDataToCheckCSVName());
            addToZipFile(htmlPagesToCheckCsvFile.getAbsolutePath(), zos, IntegrityType.HTMLPAGES.getDataToCheckCSVName());

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

            CsvReader folders = new CsvReader(ConfigUtils.getIntegrityPath() + File.separator + endpointId + File.separator + IntegrityType.FOLDERS.getDataToCheckCSVName(), '|');
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

            CsvReader structures = new CsvReader(ConfigUtils.getIntegrityPath() + File.separator + endpointId + File.separator + IntegrityType.STRUCTURES.getDataToCheckCSVName(), '|');
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

            CsvReader schemes = new CsvReader(ConfigUtils.getIntegrityPath() + File.separator + endpointId + File.separator + IntegrityType.SCHEMES.getDataToCheckCSVName(), '|');
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

            CsvReader htmlpages = new CsvReader(ConfigUtils.getIntegrityPath() + File.separator + endpointId + File.separator + IntegrityType.HTMLPAGES.getDataToCheckCSVName(), '|');
            
            boolean tempCreated = false;
            
            DotConnect dc = new DotConnect();
            String tempTableName = getTempTableName(endpointId, IntegrityType.HTMLPAGES);
            String tempKeyword = getTempKeyword();

            //Create a temporary table and insert all the records coming from the CSV file.
            String createTempTable = "create " + tempKeyword + " table " + tempTableName 
            		+ " (inode varchar(36) not null, "
            		+ "identifier varchar(36) not null, "
            		+ "parent_path varchar(255), "
                    + "asset_name varchar(255), "
                    + "host_identifier varchar(36) not null, "
                    + "primary key (inode) )" 
                    + (DbConnectionFactory.isOracle()?" ON COMMIT PRESERVE ROWS ":"");

            if(DbConnectionFactory.isOracle()) {
                createTempTable=createTempTable.replaceAll("varchar\\(", "varchar2\\(");
            }

            final String INSERT_TEMP_TABLE = "insert into " + tempTableName + " values(?,?,?,?,?)";

            while (htmlpages.readRecord()) {
                if(!tempCreated) {
                    dc.executeStatement(createTempTable);
                    tempCreated = true;
                }

                String htmlPageInode = htmlpages.get(0);
				String htmlPageIdentifier = htmlpages.get(1);
				String htmlPageParentPath = htmlpages.get(2);
				String htmlPageAssetName = htmlpages.get(3);
				String htmlPageHostIdentifier = htmlpages.get(4);

				dc.setSQL(INSERT_TEMP_TABLE);
				dc.addParam(htmlPageInode);
				dc.addParam(htmlPageIdentifier);
				dc.addParam(htmlPageParentPath);
				dc.addParam(htmlPageAssetName);
				dc.addParam(htmlPageHostIdentifier);
				dc.loadResult();
            }
            htmlpages.close();

            String resultsTableName = getResultsTableName(IntegrityType.HTMLPAGES);

            //Compare the data from the CSV to the local database data and see if we have conflicts.
            dc.setSQL("select lh.page_url as html_page, "
            		+ "lh.inode as local_inode, "
            		+ "ri.inode as remote_inode, "
            		+ "li.id as local_identifier, "
            		+ "ri.identifier as remote_identifier "
                    + "from identifier as li "
                    + "join htmlpage as lh "
                    + "on lh.identifier = li.id "
                    + "and li.asset_type = 'htmlpage' "
                    + "join " + tempTableName + " as ri "
                    + "on li.asset_name = ri.asset_name "
                    + "and li.parent_path = ri.parent_path "
                    + "and li.host_inode = ri.host_identifier "
                    + "and li.id <> ri.identifier");

            List<Map<String,Object>> results = dc.loadObjectResults();

            //If we have conflicts, lets create a table out of them.
            if(!results.isEmpty()) {
                String fullHtmlPage = " li.parent_path || li.asset_name ";

                if(DbConnectionFactory.isMySql()) {
                	fullHtmlPage = " concat(li.parent_path,li.asset_name) ";
                } else if(DbConnectionFactory.isMsSql()) {
                	fullHtmlPage = " li.parent_path + li.asset_name ";
                }

                final String INSERT_INTO_RESULTS_TABLE = "insert into " + resultsTableName 
                		+ " select " + fullHtmlPage + " as html_page, "
                		+ "lh.inode as local_inode, "
                		+ "ri.inode as remote_inode, "
                		+ "li.id as local_identifier, "
                		+ "ri.identifier as remote_identifier, "
                		+ "'" + endpointId + "' "
                        + "from identifier as li "
                        + "join htmlpage as lh "
                        + "on lh.identifier = li.id "
                        + "and li.asset_type = 'htmlpage' "
                        + "join " + tempTableName + " as ri "
                        + "on li.asset_name = ri.asset_name "
                        + "and li.parent_path = ri.parent_path "
                        + "and li.host_inode = ri.host_identifier "
                        + "and li.id <> ri.identifier";

                dc.executeStatement(INSERT_INTO_RESULTS_TABLE);
            }

            dc.setSQL("select * from "+getResultsTableName(IntegrityType.HTMLPAGES));
            results = dc.loadObjectResults();

            return !results.isEmpty();
            
        } catch(Exception e) {
            throw new Exception("Error running the HTML Pages Integrity Check", e);
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

            CsvReader csvFile = new CsvReader(ConfigUtils.getIntegrityPath() + File.separator + endpointId + File.separator + type.getDataToFixCSVName(), '|');
            DotConnect dc = new DotConnect();
            String resultsTable = getResultsTableName(type);

            String INSERT_TEMP_TABLE = "insert into " + resultsTable + " (local_inode, remote_inode, endpoint_id) values(?,?,?)";

			if(type==IntegrityType.FOLDERS) {
				INSERT_TEMP_TABLE = "insert into " + resultsTable + " (local_inode, remote_inode, local_identifier, remote_identifier, endpoint_id) values(?,?,?,?,?)";
			} else if(type==IntegrityType.HTMLPAGES) {
                INSERT_TEMP_TABLE = "insert into " + resultsTable + " (local_inode, remote_inode, local_identifier, remote_identifier, html_page, endpoint_id) values(?,?,?,?,?,?)";
            }

			while (csvFile.readRecord()) {

				String localInode = csvFile.get(0);
				String remoteInode = csvFile.get(1);
				dc.setSQL(INSERT_TEMP_TABLE);
				dc.addParam(localInode);
				dc.addParam(remoteInode);

				if(type == IntegrityType.FOLDERS || type == IntegrityType.HTMLPAGES) {
					String localIdentifier = csvFile.get(2);
					String remoteIdentifier = csvFile.get(3);
					dc.addParam(localIdentifier);
					dc.addParam(remoteIdentifier);
				}

                if(type == IntegrityType.HTMLPAGES) {
                    String htmlPage = csvFile.get(4);
                    dc.addParam(htmlPage);
                }

				dc.addParam(endpointId);
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
     * Replace Identifier with same Identifier from the other server. 
     * 
     * @param serverId
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public void fixHtmlPages ( String serverId ) throws DotDataException, DotSecurityException {
    	
    	DotConnect dc = new DotConnect();
        String tableName = getResultsTableName( IntegrityType.HTMLPAGES );
        HTMLPageCache htmlPageCache = CacheLocator.getHTMLPageCache();

        //Get the information of the IR.
		dc.setSQL( "SELECT html_page, local_identifier, remote_identifier, local_inode, remote_inode FROM " + tableName + " WHERE endpoint_id = ?" );
		dc.addParam( serverId );
		List<Map<String, Object>> results = dc.loadObjectResults();
		
		for ( Map<String, Object> result : results ) {

			//String oldHtmlPageInode = (String) result.get( "local_inode" );
		    //String newHtmlPageInode = (String) result.get( "remote_inode" );
		    String oldHtmlPageIdentifier = (String) result.get( "local_identifier" );
		    String newHtmlPageIdentifier = (String) result.get( "remote_identifier" );
		    String assetName = (String) result.get( "html_page" );
		    String localInode = (String) result.get( "local_inode" );

            //We need only the last part of the url, not the whole path.
            String[] assetNamebits = assetName.split("/");
            assetName = assetNamebits[assetNamebits.length-1];

		    htmlPageCache.remove(oldHtmlPageIdentifier);
		    CacheLocator.getIdentifierCache().removeFromCacheByInode(localInode);

		 	//Fixing by SQL queries
		    dc.setSQL("INSERT into identifier(id, parent_path, asset_name, host_inode, asset_type, syspublish_date, sysexpire_date) "
		    		+ "SELECT ? , parent_path, 'TEMP_ASSET_NAME', host_inode, asset_type, syspublish_date, sysexpire_date "
		    		+ "FROM identifier WHERE id = ?");
		    dc.addParam(newHtmlPageIdentifier);
		    dc.addParam(oldHtmlPageIdentifier);
		    dc.loadResult();
		    
		    dc.setSQL("UPDATE htmlpage "
		    		+ "SET identifier = ? "
		    		+ "WHERE identifier = ?");
		    dc.addParam(newHtmlPageIdentifier);
		    dc.addParam(oldHtmlPageIdentifier);
		    dc.loadResult();
		    
		    dc.setSQL("UPDATE htmlpage_version_info "
		    		+ "SET identifier = ? "
		    		+ "WHERE identifier = ?");
		    dc.addParam(newHtmlPageIdentifier);
		    dc.addParam(oldHtmlPageIdentifier);
		    dc.loadResult();
		    
		    dc.setSQL("DELETE FROM identifier "
		    		+ "WHERE id = ?");
		    dc.addParam(oldHtmlPageIdentifier);
		    dc.loadResult();
		    
		    dc.setSQL("UPDATE identifier "
		    		+ "SET asset_name = ? "
		    		+ "WHERE id = ?");
		    dc.addParam(assetName);
		    dc.addParam(newHtmlPageIdentifier);
		    dc.loadResult();
		}

		discardConflicts(serverId, IntegrityType.HTMLPAGES);
    }
    
}
