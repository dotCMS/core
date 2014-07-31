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
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.dotcms.repackage.javacsv.com.csvreader.CsvReader;
import com.dotcms.repackage.javacsv.com.csvreader.CsvWriter;
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

            if(type==IntegrityType.FOLDERS) {
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

                if(type==IntegrityType.FOLDERS) {
                	writer.write(rs.getString("remote_identifier"));
                	writer.write(rs.getString("local_identifier"));
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

        System.out.println("Writing '" + fileName + "' to zip file");

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

    public void generateDataToCheckZip(String endpointId) throws Exception {
        File foldersToCheckCsvFile = null;
        File structuresToCheckCsvFile = null;
        File schemesToCheckCsvFile = null;
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

            addToZipFile(foldersToCheckCsvFile.getAbsolutePath(), zos, IntegrityType.FOLDERS.getDataToCheckCSVName());
            addToZipFile(structuresToCheckCsvFile.getAbsolutePath(), zos, IntegrityType.STRUCTURES.getDataToCheckCSVName());
            addToZipFile(schemesToCheckCsvFile.getAbsolutePath(), zos, IntegrityType.SCHEMES.getDataToCheckCSVName());

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

        discardConflicts(endpointId, type);


    }

    public void generateDataToFixTable(String endpointId, IntegrityType type) throws Exception {

        try {

            CsvReader csvFile = new CsvReader(ConfigUtils.getIntegrityPath() + File.separator + endpointId + File.separator + type.getDataToFixCSVName(), '|');
            DotConnect dc = new DotConnect();
            String resultsTable = getResultsTableName(type);

            String INSERT_TEMP_TABLE = "insert into " + resultsTable + " (local_inode, remote_inode, endpoint_id) values(?,?,?)";

			if(type==IntegrityType.FOLDERS) {
				INSERT_TEMP_TABLE = "insert into " + resultsTable + " (local_inode, remote_inode, local_identifier, remote_identifier, endpoint_id) values(?,?,?,?,?)";
			}

			while (csvFile.readRecord()) {

				//select f.inode, i.parent_path, i.asset_name, i.host_inode
				String localInode = csvFile.get(0);
				String remoteInode = csvFile.get(1);
				dc.setSQL(INSERT_TEMP_TABLE);
				dc.addParam(localInode);
				dc.addParam(remoteInode);

				if(type==IntegrityType.FOLDERS) {
					String localIdentifier = csvFile.get(2);
					String remoteIdentifier = csvFile.get(3);
					dc.addParam(localIdentifier);
					dc.addParam(remoteIdentifier);
				}

				dc.addParam(endpointId);
				dc.loadResult();
			}

        } catch(Exception e) {
            throw new Exception("Error generating data to fix", e);
        }
    }

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

//		final String H2="SELECT COUNT(table_name) as exist FROM information_schema.tables WHERE Table_Name = '"+tableName.toUpperCase()+"' + ";

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
        dc.setSQL("delete from " + resultsTableName+ " where endpoint_id = ?");
        dc.addParam(endpointId);
        dc.loadResult();
    }

    public void fixConflicts(String endpointId, IntegrityType type) throws DotDataException, DotSecurityException {
        if(type == IntegrityType.FOLDERS) {
            fixFolders(endpointId);
        } else if(type == IntegrityType.STRUCTURES) {
            fixStructures(endpointId);
        } else if(type == IntegrityType.SCHEMES) {
            fixSchemes(endpointId);
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
        boolean firstContraintDeleted = false;
        boolean secondConstraintDeleted = false;

        try {

        	 // lets remove from the index all the content under each conflicted folder

        	dc.setSQL("select local_inode from " + tableName + " where endpoint_id = ?");
            dc.addParam(serverId);
            List<Map<String,Object>> results = dc.loadObjectResults();

            for (Map<String, Object> result : results) {
            	String oldFolderInode = (String) result.get("local_inode");

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
            	} catch (DotDataException e) {
            		Logger.info(getClass(), "Folder not found. inode: " + oldFolderInode);
            	}
			}

            if(DbConnectionFactory.isMsSql()) {
    			dc.executeStatement("SET TRANSACTION ISOLATION LEVEL READ COMMITTED;");
        	}

            //First delete the constrain
            if ( DbConnectionFactory.isMySql() ) {
                dc.executeStatement( "alter table folder drop FOREIGN KEY fkb45d1c6e5fb51eb" );
                firstContraintDeleted = true;
                // identifier fk
                dc.executeStatement( "alter table folder drop FOREIGN KEY folder_identifier_fk" );
                secondConstraintDeleted = true;
            } else {
                dc.executeStatement( "alter table folder drop constraint fkb45d1c6e5fb51eb" );
                firstContraintDeleted = true;
                // identifier fk
                dc.executeStatement("alter table folder drop constraint folder_identifier_fk");
                secondConstraintDeleted = true;

            }

            //Update the folder's inode
            updateFrom( serverId, dc, tableName, "folder", "inode" );
            //Update the inode record
            updateFrom( serverId, dc, tableName, "inode", "inode" );

            //Update the folder's identifier
            updateIdentifierFrom( serverId, dc, tableName, "folder", "identifier" );
            //Update the identifier record
            updateIdentifierFrom( serverId, dc, tableName, "identifier", "id" );


            //Update the structure
            if ( DbConnectionFactory.isMySql() ) {
                dc.executeStatement( "UPDATE structure JOIN " + tableName + " ir on structure.folder = ir.local_inode SET structure.folder = ir.remote_inode" );
            } else if ( DbConnectionFactory.isOracle() || DbConnectionFactory.isH2() ) {
                updateFrom( serverId, dc, tableName, "structure", "folder" );
            } else {
                dc.executeStatement( "UPDATE structure SET folder = ir.remote_inode FROM " + tableName + " ir WHERE structure.folder = ir.local_inode" );
            }

            //permission
            updateFrom( serverId, dc, tableName, "permission", "inode_id" );
            //permission_reference
            updateFrom( serverId, dc, tableName, "permission_reference", "asset_id" );

            // lets reindex all the content under each fixed folder

        	dc.setSQL("select remote_inode from " + tableName + " where endpoint_id = ?");
            dc.addParam(serverId);
            results = dc.loadObjectResults();

            for (Map<String, Object> result : results) {
            	String newFolderInode = (String) result.get("remote_inode");
            	try {
            		final Folder folder = APILocator.getFolderAPI().find(newFolderInode, APILocator.getUserAPI().getSystemUser(), false);

            		HibernateUtil.addCommitListener(new Runnable() {
            			public void run() {
            				try {
            					APILocator.getContentletAPI().refreshContentUnderFolder(folder);
            				} catch (DotStateException e) {
            					Logger.error(this,"Error while reindexing content under folder with inode: " + folder.getInode(),e);
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
        } finally {
            try {
                //Add back the constrain
            	if(firstContraintDeleted)
            		dc.executeStatement( "alter table folder add constraint fkb45d1c6e5fb51eb foreign key (inode) references inode (inode)" );

            	if(secondConstraintDeleted)
            		dc.executeStatement( "alter table folder add constraint folder_identifier_fk foreign key (identifier) references identifier (id)" );

            } catch ( SQLException e ) {
                throw new DotDataException( e.getMessage(), e );
            }
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

        	if(DbConnectionFactory.isMsSql()) {
    			dc.executeStatement("SET TRANSACTION ISOLATION LEVEL READ COMMITTED;");
        	}

            //First delete the constrains
            if ( DbConnectionFactory.isMySql() ) {
                dc.executeStatement( "alter table structure drop FOREIGN KEY fk89d2d735fb51eb" );
                dc.executeStatement( "alter table contentlet drop FOREIGN KEY fk_structure_inode" );
                //dc.executeStatement( "alter table containers drop FOREIGN KEY structure_fk" );
            } else {
                dc.executeStatement( "alter table structure drop constraint fk89d2d735fb51eb" );
                dc.executeStatement( "alter table contentlet drop constraint fk_structure_inode" );
                //dc.executeStatement( "alter table containers drop constraint structure_fk" );
            }
            if ( DbConnectionFactory.isMsSql() ) {
                dc.executeStatement( "alter table workflow_scheme_x_structure nocheck constraint all" );
            } else if ( DbConnectionFactory.isOracle() ) {
                enableDisableOracleConstrains( dc, "workflow_scheme_x_structure", false );
            } else if ( DbConnectionFactory.isH2() ) {
                dc.executeStatement( "alter table workflow_scheme_x_structure SET REFERENTIAL_INTEGRITY FALSE" );
            } else if ( !DbConnectionFactory.isMySql() ) {
                dc.executeStatement( "ALTER TABLE workflow_scheme_x_structure DROP CONSTRAINT workflow_scheme_x_structure_scheme_id_fkey" );
                dc.executeStatement( "ALTER TABLE workflow_scheme_x_structure DROP CONSTRAINT workflow_scheme_x_structure_structure_id_fkey ");
            }

            dc.setSQL("select local_inode from " + tableName + " where endpoint_id = ?");
            dc.addParam(serverId);
            List<Map<String,Object>> results = dc.loadObjectResults();

            for (Map<String, Object> result : results) {
				String structureInode = (String) result.get("local_inode");
				Structure st = StructureCache.getStructureByInode(structureInode);

				List<Contentlet> contents = APILocator.getContentletAPI().findByStructure(st, APILocator.getUserAPI().getSystemUser(), false, 0, 0);
				for (Contentlet contentlet : contents) {
					CacheLocator.getContentletCache().remove(contentlet.getInode());
				}

				StructureCache.removeStructure(st);
			}

            //structure
            updateFrom( serverId, dc, tableName, "structure", "inode" );
            //inode
            updateFrom( serverId, dc, tableName, "inode", "inode" );
            //container_structures
            updateFrom( serverId, dc, tableName, "container_structures", "structure_id" );
            //contentlet
            updateFrom( serverId, dc, tableName, "contentlet", "structure_inode" );
            //field
            updateFrom( serverId, dc, tableName, "field", "structure_inode" );
            //containers
            //relationship
            updateFrom( serverId, dc, tableName, "relationship", "parent_structure_inode" );
            updateFrom( serverId, dc, tableName, "relationship", "child_structure_inode" );
            //workflow_scheme_x_structure
            updateFrom( serverId, dc, tableName, "workflow_scheme_x_structure", "structure_id" );
            //permission
            updateFrom( serverId, dc, tableName, "permission", "inode_id" );
            //permission_reference
            updateFrom( serverId, dc, tableName, "permission_reference", "asset_id" );

            discardConflicts(serverId, IntegrityType.STRUCTURES);

        } catch ( SQLException e ) {
            throw new DotDataException( e.getMessage(), e );
        } finally {
            try {
                //Add back the constrains
                dc.executeStatement( "ALTER TABLE structure add constraint fk89d2d735fb51eb foreign key (inode) references inode (inode)" );
                dc.executeStatement( "ALTER TABLE contentlet add constraint fk_structure_inode foreign key (structure_inode) references structure (inode)" );
                if ( DbConnectionFactory.isMsSql() ) {
                    dc.executeStatement( "alter table workflow_scheme_x_structure with check check constraint all" );
                } else if ( DbConnectionFactory.isOracle() ) {
                    enableDisableOracleConstrains( dc, "workflow_scheme_x_structure", true );
                } else if ( DbConnectionFactory.isH2() ) {
                    dc.executeStatement( "alter table workflow_scheme_x_structure SET REFERENTIAL_INTEGRITY TRUE" );
                } else if ( !DbConnectionFactory.isMySql() ) {
                    dc.executeStatement( "ALTER TABLE workflow_scheme_x_structure ADD CONSTRAINT workflow_scheme_x_structure_scheme_id_fkey FOREIGN KEY (scheme_id) REFERENCES workflow_scheme (id)" );
                    dc.executeStatement( "ALTER TABLE workflow_scheme_x_structure ADD CONSTRAINT workflow_scheme_x_structure_structure_id_fkey FOREIGN KEY (structure_id) REFERENCES structure (inode)" );
                }

            } catch ( SQLException e ) {
                throw new DotDataException( e.getMessage(), e );
            }
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

        	if(DbConnectionFactory.isMsSql()) {
    			dc.executeStatement("SET TRANSACTION ISOLATION LEVEL READ COMMITTED;");
        	}

            //First delete the constrains
            if ( DbConnectionFactory.isMsSql() ) {
                dc.executeStatement( "alter table workflow_scheme nocheck constraint all" );
                dc.executeStatement( "alter table workflow_step nocheck constraint all" );
                dc.executeStatement( "alter table workflow_scheme_x_structure nocheck constraint all" );
            } else if ( DbConnectionFactory.isOracle() ) {
                enableDisableOracleConstrains( dc, "workflow_step", false );
                enableDisableOracleConstrains( dc, "workflow_scheme", false );
                enableDisableOracleConstrains( dc, "workflow_scheme_x_structure", false );
            } else if ( DbConnectionFactory.isH2() ) {
                dc.executeStatement( "alter table workflow_scheme SET REFERENTIAL_INTEGRITY FALSE" );
                dc.executeStatement( "alter table workflow_step SET REFERENTIAL_INTEGRITY FALSE" );
                dc.executeStatement( "alter table workflow_scheme_x_structure SET REFERENTIAL_INTEGRITY FALSE" );
            } else if ( !DbConnectionFactory.isMySql() ) {
                dc.executeStatement( "ALTER TABLE workflow_step DROP CONSTRAINT workflow_step_scheme_id_fkey" );
                dc.executeStatement( "ALTER TABLE workflow_scheme_x_structure DROP CONSTRAINT workflow_scheme_x_structure_scheme_id_fkey" );
            }

            //Delete the schemes cache
            dc.setSQL( "SELECT local_inode FROM " + tableName + " WHERE endpoint_id = ?" );
            dc.addParam( serverId );
            List<Map<String, Object>> results = dc.loadObjectResults();
            for ( Map<String, Object> result : results ) {

                String workflowInode = (String) result.get( "local_inode" );

                WorkflowCache workflowCache = CacheLocator.getWorkFlowCache();
                //Verify if the workflow is the default one
                WorkflowScheme defaultScheme = workflowCache.getDefaultScheme();
                if ( defaultScheme != null && defaultScheme.getId().equals( workflowInode ) ) {
                    CacheLocator.getCacheAdministrator().remove( workflowCache.defaultKey, workflowCache.getPrimaryGroup() );
                } else {
                    //Clear the cache
                    WorkflowScheme scheme = workflowCache.getScheme( workflowInode );
                    workflowCache.remove( scheme );
                }
            }

            //workflow_scheme
            updateFrom( serverId, dc, tableName, "workflow_scheme", "id" );
            //workflow_step
            updateFrom( serverId, dc, tableName, "workflow_step", "scheme_id" );
            //workflow_scheme_x_structure
            updateFrom( serverId, dc, tableName, "workflow_scheme_x_structure", "scheme_id" );

            discardConflicts(serverId, IntegrityType.SCHEMES);

        } catch ( SQLException e ) {
            throw new DotDataException( e.getMessage(), e );
        } finally {
            try {
                //Add back the constrains
                if ( DbConnectionFactory.isMsSql() ) {
                    dc.executeStatement( "alter table workflow_scheme with check check constraint all" );
                    dc.executeStatement( "alter table workflow_step with check check constraint all" );
                    dc.executeStatement( "alter table workflow_scheme_x_structure with check check constraint all" );
                } else if ( DbConnectionFactory.isOracle() ) {
                    enableDisableOracleConstrains( dc, "workflow_step", true );
                    enableDisableOracleConstrains( dc, "workflow_scheme", true );
                    enableDisableOracleConstrains( dc, "workflow_scheme_x_structure", true );
                } else if ( DbConnectionFactory.isH2() ) {
                    dc.executeStatement( "alter table workflow_scheme SET REFERENTIAL_INTEGRITY TRUE" );
                    dc.executeStatement( "alter table workflow_step SET REFERENTIAL_INTEGRITY TRUE" );
                    dc.executeStatement( "alter table workflow_scheme_x_structure SET REFERENTIAL_INTEGRITY TRUE" );
                } else if ( !DbConnectionFactory.isMySql() ) {
                    dc.executeStatement( "ALTER TABLE workflow_step ADD CONSTRAINT workflow_step_scheme_id_fkey FOREIGN KEY (scheme_id) REFERENCES workflow_scheme (id)" );
                    dc.executeStatement( "ALTER TABLE workflow_scheme_x_structure ADD CONSTRAINT workflow_scheme_x_structure_scheme_id_fkey FOREIGN KEY (scheme_id) REFERENCES workflow_scheme (id)" );
                }

            } catch ( SQLException e ) {
                throw new DotDataException( e.getMessage(), e );
            }
        }
    }

    /**
     * Generates and executes update from queries using the given tables and columns parameters
     *
     * @param dotConnect
     * @param resultsTable
     * @param updateTable
     * @param updateColumn
     * @throws SQLException
     */
    private static void updateFrom ( String endpointId, DotConnect dotConnect, String resultsTable, String updateTable, String updateColumn ) throws SQLException {

        if ( DbConnectionFactory.isMySql() ) {
            dotConnect.executeStatement( "UPDATE " + updateTable + " JOIN " + resultsTable + " ir on " + updateColumn + " = ir.local_inode and '"+endpointId+"' = ir.endpoint_id  SET " + updateColumn + " = ir.remote_inode" );
        } else if ( DbConnectionFactory.isOracle() ) {
            dotConnect.executeStatement( "UPDATE " + updateTable +
                    " SET " + updateColumn + " = (SELECT ir.remote_inode FROM " + resultsTable + " ir WHERE " + updateColumn + " = ir.local_inode and '"+endpointId+"' = ir.endpoint_id)" +
                    " WHERE exists (SELECT ir.remote_inode FROM " + resultsTable + " ir WHERE " + updateColumn + " = ir.local_inode and '"+endpointId+"' = ir.endpoint_id)" );
        } else if ( DbConnectionFactory.isH2() ) {
            dotConnect.executeStatement( "UPDATE " + updateTable + " ut " +
                    " SET " + updateColumn + " = (select ir.remote_inode FROM " + resultsTable + " ir WHERE ut." + updateColumn + " = ir.local_inode and '"+endpointId+"' = ir.endpoint_id)"
                    + " WHERE exists (SELECT * from " + resultsTable + " ir where ut." + updateColumn + " = ir.local_inode and '"+endpointId+"' = ir.endpoint_id)" );
        } else {
            dotConnect.executeStatement( "UPDATE " + updateTable + " SET " + updateColumn + " = ir.remote_inode FROM " + resultsTable + " ir WHERE " + updateColumn + " = ir.local_inode and '"+endpointId+"' = ir.endpoint_id" );
        }
    }

    private static void updateIdentifierFrom ( String endpointId, DotConnect dotConnect, String resultsTable, String updateTable, String updateColumn ) throws SQLException {

        if ( DbConnectionFactory.isMySql() ) {
            dotConnect.executeStatement( "UPDATE " + updateTable + " JOIN " + resultsTable + " ir on " + updateColumn + " = ir.local_identifier and '"+endpointId+"' = ir.endpoint_id  SET " + updateColumn + " = ir.remote_identifier" );
        } else if ( DbConnectionFactory.isOracle() || DbConnectionFactory.isH2() ) {
            dotConnect.executeStatement( "UPDATE " + updateTable +
                    " SET " + updateColumn + " = (SELECT ir.remote_identifier FROM " + resultsTable + " ir WHERE " + updateColumn + " = ir.local_identifier and '"+endpointId+"' = ir.endpoint_id)" +
                    " WHERE exists (SELECT ir.remote_identifier FROM " + resultsTable + " ir WHERE " + updateColumn + " = ir.local_identifier and '"+endpointId+"' = ir.endpoint_id)" );
        } else {
            dotConnect.executeStatement( "UPDATE " + updateTable + " SET " + updateColumn + " = ir.remote_identifier FROM " + resultsTable + " ir WHERE " + updateColumn + " = ir.local_identifier and '"+endpointId+"' = ir.endpoint_id" );
        }
    }

    /**
     * Method that will enable/disable the constrains for a given table on oracle
     *
     * @param dotConnect
     * @param table
     * @param enable
     * @throws DotDataException
     */
    private static void enableDisableOracleConstrains ( DotConnect dotConnect, String table, boolean enable ) throws DotDataException {

        Connection conn = DbConnectionFactory.getConnection();
        ResultSet rs = null;
        PreparedStatement statement = null;

        try {

            //Create the enable/disable constraint query for each constraint in the given table
            String operation = enable ? "ENABLE" : "DISABLE";
            statement = conn.prepareStatement( "select 'alter table '||a.owner||'.'||a.table_name|| ' " + operation + " constraint '||a.constraint_name||''" +
                    " FROM user_constraints a, user_constraints b" +
                    " WHERE a.constraint_type = 'R'" +
                    " AND a.r_constraint_name = b.constraint_name" +
                    " AND a.r_owner  = b.owner" +
                    " AND b.table_name = '" + table.toUpperCase() + "'" );
            rs = statement.executeQuery();

            while ( rs.next() ) {
                //Enable/disable query
                String constraintQuery = rs.getString( 1 );
                //Execute the constraint query
                dotConnect.executeStatement( constraintQuery );
            }

        } catch ( SQLException e ) {
            throw new DotDataException( e.getMessage(), e );
        } finally {
            try {
                if ( rs != null ) rs.close();
            } catch ( Exception e ) {
            }
            try {
                if ( statement != null ) statement.close();
            } catch ( Exception e ) {
            }
        }
    }

}
