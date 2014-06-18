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

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;
import com.dotcms.rest.IntegrityResource.IntegrityType;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
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
			statement = conn.prepareStatement("select f.inode, i.parent_path, i.asset_name, i.host_inode from folder f join identifier i on f.identifier = i.id ");
			rs = statement.executeQuery();
			int count = 0;

			while (rs.next()) {
				writer.write(rs.getString("inode"));
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
				writer.write(rs.getString("workflow_scheme"));
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

			String resultsTable = getResultsTableName(endpointId, type);

			statement = conn.prepareStatement("select remote_inode, local_inode from " + resultsTable);
			rs = statement.executeQuery();
			int count = 0;

			while (rs.next()) {
				writer.write(rs.getString("remote_inode"));
				writer.write(rs.getString("local_inode"));
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

	public void generateDataToCheckZip(String endpointId) {
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
			if(zipFile!=null && zipFile.exists())
				zipFile.delete();
		} finally {
        	if(dataToFixCsvFile!=null && dataToFixCsvFile.exists())
        		dataToFixCsvFile.delete();
        }
	}

	public void checkFoldersIntegrity(String endpointId) throws Exception {

		try {

			CsvReader folders = new CsvReader(ConfigUtils.getIntegrityPath() + File.separator + endpointId + File.separator + IntegrityType.FOLDERS.getDataToCheckCSVName(), '|');
			boolean tempCreated = false;
			DotConnect dc = new DotConnect();
			String tempTableName = getTempTableName(endpointId, IntegrityType.FOLDERS);

			// lets create a temp table and insert all the records coming from the CSV file

			String createTempTable = "create table " + tempTableName + " (inode varchar(36) not null, parent_path varchar(255), "
					+ "asset_name varchar(255), host_identifier varchar(36) not null, primary key (inode) )";

			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)) {
				createTempTable=createTempTable.replaceAll("varchar\\(", "varchar2\\(");
			}

			final String INSERT_TEMP_TABLE = "insert into " + tempTableName + " values(?,?,?,?)";

			while (folders.readRecord()) {

				if(!tempCreated) {
					dc.executeStatement(createTempTable);
					tempCreated = true;
				}

				//select f.inode, i.parent_path, i.asset_name, i.host_inode
				String folderInode = folders.get(0);
				String parentPath = folders.get(1);
				String assetName = folders.get(2);
				String hostIdentifier = folders.get(3);

				dc.setSQL(INSERT_TEMP_TABLE);
				dc.addParam(folderInode);
				dc.addParam(parentPath);
				dc.addParam(assetName);
				dc.addParam(hostIdentifier);
				dc.loadResult();
			}

			folders.close();

			String resultsTableName = getResultsTableName(endpointId, IntegrityType.FOLDERS);

			// compare the data from the CSV to the local db data and see if we have conflicts
			dc.setSQL("select iden.parent_path || iden.asset_name as folder, "
						+ "c.title as host_name, f.inode as local_inode, ft.inode as remote_inode from identifier iden "
						+ "join folder f on iden.id = f.identifier join " + tempTableName + " ft on iden.parent_path = ft.parent_path "
						+ "join contentlet c on iden.host_inode = c.identifier and iden.asset_name = ft.asset_name and ft.host_identifier = iden.host_inode "
						+ "where asset_type = 'folder' and f.inode <> ft.inode order by c.title, iden.asset_name");

			List<Map<String,Object>> results = dc.loadObjectResults();

			if(!results.isEmpty()) {
				// if we have conflicts, lets create a table out of them
				final String CREATE_RESULTS_TABLE = "create table " + resultsTableName + " as select iden.parent_path || iden.asset_name as folder, "
						+ "c.title as host_name, f.inode as local_inode, ft.inode as remote_inode from identifier iden "
						+ "join folder f on iden.id = f.identifier join " + tempTableName + " ft on iden.parent_path = ft.parent_path "
						+ "join contentlet c on iden.host_inode = c.identifier and iden.asset_name = ft.asset_name and ft.host_identifier = iden.host_inode "
						+ "where asset_type = 'folder' and f.inode <> ft.inode order by c.title, iden.asset_name";

				dc.executeStatement(CREATE_RESULTS_TABLE);
				dc.executeStatement("alter table " + resultsTableName + " add primary key (local_inode)");
			}

			// lets drop the temp table
			dc.executeStatement("drop table " + tempTableName );

		} catch(Exception e) {
			throw new Exception("Error running the Folders Integrity Check", e);
		}
	}

	public void checkStructuresIntegrity(String endpointId) throws Exception {

		try {

			CsvReader structures = new CsvReader(ConfigUtils.getIntegrityPath() + File.separator + endpointId + File.separator + IntegrityType.STRUCTURES.getDataToCheckCSVName(), '|');
			boolean tempCreated = false;
			DotConnect dc = new DotConnect();
			String tempTableName = getTempTableName(endpointId, IntegrityType.STRUCTURES);

			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)) {
				tempTableName = tempTableName.substring(0, 29);
			}

			String createTempTable = "create table " + tempTableName + " (inode varchar(36) not null, velocity_var_name varchar(255), "
					+ " primary key (inode) )";

			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)) {
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

			String resultsTableName = getResultsTableName(endpointId, IntegrityType.STRUCTURES);

			// compare the data from the CSV to the local db data and see if we have conflicts
			dc.setSQL("select s.velocity_var_name as velocity_name, "
						+ "s.inode as local_inode, st.inode as remote_inode from structure s "
						+ "join " + tempTableName + " st on s.velocity_var_name = st.velocity_var_name and s.inode <> st.inode");

			List<Map<String,Object>> results = dc.loadObjectResults();

			if(!results.isEmpty()) {
				// if we have conflicts, lets create a table out of them
				String CREATE_RESULTS_TABLE = "create table " + resultsTableName + " as select s.velocity_var_name as velocity_name, "
						+ "s.inode as local_inode, st.inode as remote_inode from structure s "
						+ "join " + tempTableName + " st on s.velocity_var_name = st.velocity_var_name and s.inode <> st.inode";

				dc.executeStatement(CREATE_RESULTS_TABLE);
				dc.executeStatement("alter table " + resultsTableName + " add primary key (local_inode)");
			}

			// lets drop the temp table
			dc.executeStatement("drop table " + tempTableName );


		} catch(Exception e) {
			throw new Exception("Error running the Structures Integrity Check", e);
		}
	}

	public void checkWorkflowSchemesIntegrity(String endpointId) throws Exception {

		try {

			CsvReader schemes = new CsvReader(ConfigUtils.getIntegrityPath() + File.separator + endpointId + File.separator + IntegrityType.SCHEMES.getDataToCheckCSVName(), '|');
			boolean tempCreated = false;
			DotConnect dc = new DotConnect();
			String tempTableName = getTempTableName(endpointId, IntegrityType.SCHEMES);

			String createTempTable = "create table " + tempTableName + " (inode varchar(36) not null, name varchar(255), "
					+ " primary key (inode) )";

			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)) {
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

			String resultsTableName = getResultsTableName(endpointId, IntegrityType.SCHEMES);

			// compare the data from the CSV to the local db data and see if we have conflicts
			dc.setSQL("select s.name, s.id as local_inode, wt.inode as remote_inode from workflow_scheme s "
					+ "join " + tempTableName + " wt on s.name = wt.name and s.id <> wt.inode");

			List<Map<String,Object>> results = dc.loadObjectResults();


			if(!results.isEmpty()) {
				// if we have conflicts, lets create a table out of them
				String CREATE_RESULTS_TABLE = "create table " + resultsTableName + " as select s.name, s.id as local_inode, wt.inode as remote_inode from workflow_scheme s "
						+ "join " + tempTableName + " wt on s.name = wt.name and s.id <> wt.inode";

				dc.executeStatement(CREATE_RESULTS_TABLE);
				dc.executeStatement("alter table " + resultsTableName + " add primary key (local_inode)");

			}

			// lets drop the temp table
			dc.executeStatement("drop table " + tempTableName );


		} catch(Exception e) {
			throw new Exception("Error running the Workflow Schemes Integrity Check", e);
		}
	}


	public List<Map<String, Object>> getIntegrityConflicts(String endpointId, IntegrityType type) throws Exception {
		try {
			DotConnect dc = new DotConnect();

			String resultsTableName = getResultsTableName(endpointId, type);

			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)) {
				resultsTableName = resultsTableName.substring(0, 29);
			}

			if(!doesTableExist(resultsTableName)) {
				return new ArrayList<Map<String, Object>>();
			}

			dc.setSQL("select * from " + resultsTableName);

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


	}

	public void generateDataToFixTable(String endpointId, IntegrityType type) throws Exception {

		try {

			CsvReader csvFile = new CsvReader(ConfigUtils.getIntegrityPath() + File.separator + endpointId + File.separator + type.getDataToFixCSVName(), '|');
			boolean resultsCreated = false;
			DotConnect dc = new DotConnect();
			String resultsTable = getResultsTableName(endpointId, type);

			// lets create a temp table and insert all the records coming from the CSV file

			String createResultsTable = "create table " + resultsTable + " (local_inode varchar(36) not null, remote_inode varchar(36) not null, "
					+ "primary key (local_inode) )";

			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)) {
				createResultsTable=createResultsTable.replaceAll("varchar\\(", "varchar2\\(");
			}

			final String INSERT_TEMP_TABLE = "insert into " + resultsTable + " values(?,?)";

			while (csvFile.readRecord()) {

				if(!resultsCreated) {
					dc.executeStatement(createResultsTable);
					resultsCreated = true;
				}

				//select f.inode, i.parent_path, i.asset_name, i.host_inode
				String localInode = csvFile.get(0);
				String remoteInode = csvFile.get(1);

				dc.setSQL(INSERT_TEMP_TABLE);
				dc.addParam(localInode);
				dc.addParam(remoteInode);
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

		if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)) {
			resultsTableName = resultsTableName.substring(0, 29);
		}

		return resultsTableName;
	}

	private String getResultsTableName(String endpointId, IntegrityType type) {

		if(!UtilMethods.isSet(endpointId)) return null;

		String endpointIdforDB = endpointId.replace("-", "");
		String resultsTableName = type.name().toLowerCase() + "_ir_" + endpointIdforDB;

		if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)) {
			resultsTableName = resultsTableName.substring(0, 29);
		}

		return resultsTableName;
	}

	public Boolean doesIntegrityConflictsDataExist(String endpointId) throws Exception {

		String folderTable = getResultsTableName(endpointId, IntegrityType.FOLDERS);
		String structuresTable = getResultsTableName(endpointId, IntegrityType.STRUCTURES);
		String schemesTable = getResultsTableName(endpointId, IntegrityType.SCHEMES);

//		final String H2="SELECT COUNT(table_name) as exist FROM information_schema.tables WHERE Table_Name = '"+tableName.toUpperCase()+"' + ";

		return doesTableExist(folderTable) || doesTableExist(structuresTable) || doesTableExist(schemesTable);

	}

	private boolean doesTableExist(String tableName) throws DotDataException {
		DotConnect dc = new DotConnect();

		if(DbConnectionFactory.isOracle()) {
			dc.setSQL("SELECT COUNT(*) as exist FROM user_tables WHERE table_name='"+tableName+"'");
			BigDecimal existTable = (BigDecimal)dc.loadObjectResults().get(0).get("exist");
			return existTable.longValue() > 0;
		} else if(DbConnectionFactory.isPostgres()) {
			dc.setSQL("SELECT COUNT(table_name) as exist FROM information_schema.tables WHERE Table_Name = '"+tableName+"' ");
			long existTable = (Long)dc.loadObjectResults().get(0).get("exist");
			return existTable > 0;
		} else if(DbConnectionFactory.isMsSql()) {
			dc.setSQL("SELECT COUNT(*) as exist FROM sysobjects WHERE name = '"+tableName+"'");
			int existTable = (Integer)dc.loadObjectResults().get(0).get("exist");
			return existTable > 0;
		} else if(DbConnectionFactory.isMySql()) {
			dc.setSQL("SHOW TABLES LIKE '"+tableName+"'");
			return !dc.loadObjectResults().isEmpty();
		}

		return false;
	}

//	public void generateRemoteFixData

	public void discardConflicts(String endpointId, IntegrityType type) throws Exception {
		try {
			DotConnect dc = new DotConnect();
			String resultsTableName = getResultsTableName(endpointId, type);
			dc.executeStatement("drop table " + resultsTableName);

		} catch(Exception e) {
			throw new Exception("Error running the Structures Integrity Check", e);
		}
	}

	public void fixConflicts(String endpointId, IntegrityType type) throws DotDataException {
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
     *
     * @param serverId
     * @throws DotDataException
     */
    public void fixFolders ( String serverId ) throws DotDataException {

        DotConnect dc = new DotConnect();

        try {

            String tableName = getResultsTableName( serverId, IntegrityType.FOLDERS );

            //First delete the constrain
            if ( DbConnectionFactory.getDBType().equals( DbConnectionFactory.MYSQL ) ) {
                dc.executeStatement( "alter table folder drop FOREIGN KEY fkb45d1c6e5fb51eb" );
            } else {
                dc.executeStatement( "alter table folder drop constraint fkb45d1c6e5fb51eb" );
            }

            //Update the folder
            updateFrom( dc, tableName, "folder", "inode" );
            //Update the inode
            updateFrom( dc, tableName, "inode", "inode" );
            //Update the structure
            if ( DbConnectionFactory.getDBType().equals( DbConnectionFactory.MYSQL ) ) {
                dc.executeStatement( "UPDATE structure JOIN " + tableName + " ir on structure.folder = ir.local_inode SET structure.folder = ir.remote_inode" );
            } else if ( DbConnectionFactory.getDBType().equals( DbConnectionFactory.ORACLE ) ) {
                updateFrom( dc, tableName, "structure", "folder" );
            } else {
                dc.executeStatement( "UPDATE structure SET folder = ir.remote_inode FROM " + tableName + " ir WHERE structure.folder = ir.local_inode" );
            }

            //permission
            updateFrom( dc, tableName, "permission", "inode_id" );
            //permission_reference
            updateFrom( dc, tableName, "permission_reference", "asset_id" );

            dc.executeStatement("drop table " + tableName);

        } catch ( SQLException e ) {
            throw new DotDataException( e.getMessage(), e );
        } finally {
            try {
                //Add back the constrain
                dc.executeStatement( "alter table folder add constraint fkb45d1c6e5fb51eb foreign key (inode) references inode (inode)" );
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
     */
    public void fixStructures ( String serverId ) throws DotDataException {

        DotConnect dc = new DotConnect();

        try {

            String tableName = getResultsTableName( serverId, IntegrityType.STRUCTURES );

            //First delete the constrains
            if ( DbConnectionFactory.getDBType().equals( DbConnectionFactory.MYSQL ) ) {
                dc.executeStatement( "alter table structure drop FOREIGN KEY fk89d2d735fb51eb" );
                dc.executeStatement( "alter table contentlet drop FOREIGN KEY fk_structure_inode" );
                dc.executeStatement( "alter table containers drop FOREIGN KEY structure_fk" );
            } else {
                dc.executeStatement( "alter table structure drop constraint fk89d2d735fb51eb" );
                dc.executeStatement( "alter table contentlet drop constraint fk_structure_inode" );
                dc.executeStatement( "alter table containers drop constraint structure_fk" );
            }

            //structure
            updateFrom( dc, tableName, "structure", "inode" );
            //inode
            updateFrom( dc, tableName, "inode", "inode" );
            //container_structures
            updateFrom( dc, tableName, "container_structures", "structure_id" );
            //contentlet
            updateFrom( dc, tableName, "contentlet", "structure_inode" );
            //field
            updateFrom( dc, tableName, "field", "structure_inode" );
            //containers
            updateFrom( dc, tableName, "containers", "structure_inode" );
            //relationship
            updateFrom( dc, tableName, "relationship", "parent_structure_inode" );
            updateFrom( dc, tableName, "relationship", "child_structure_inode" );
            //workflow_scheme_x_structure
            updateFrom( dc, tableName, "workflow_scheme_x_structure", "structure_id" );
            //permission
            updateFrom( dc, tableName, "permission", "inode_id" );
            //permission_reference
            updateFrom( dc, tableName, "permission_reference", "asset_id" );

            dc.executeStatement("drop table " + tableName);

        } catch ( SQLException e ) {
            throw new DotDataException( e.getMessage(), e );
        } finally {
            try {
                //Add back the constrains
                dc.executeStatement( "ALTER TABLE structure add constraint fk89d2d735fb51eb foreign key (inode) references inode (inode)" );
                dc.executeStatement( "ALTER TABLE contentlet add constraint fk_structure_inode foreign key (structure_inode) references structure (inode)" );
                dc.executeStatement( "ALTER TABLE containers add constraint structure_fk foreign key (structure_inode) references structure (inode)" );
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

        try {

            String tableName = getResultsTableName( serverId, IntegrityType.SCHEMES );

            //First delete the constrains
            if ( DbConnectionFactory.getDBType().equals( DbConnectionFactory.MSSQL ) ) {
                dc.executeStatement( "alter table workflow_scheme nocheck constraint all" );
                dc.executeStatement( "alter table workflow_step nocheck constraint all" );
            } else if ( DbConnectionFactory.getDBType().equals( DbConnectionFactory.ORACLE ) ) {
                enableDisableOracleConstrains( dc, "workflow_step", false );
                enableDisableOracleConstrains( dc, "workflow_scheme", false );
            } else if ( !DbConnectionFactory.getDBType().equals( DbConnectionFactory.MYSQL ) ) {
                dc.executeStatement( "ALTER TABLE workflow_step DROP CONSTRAINT workflow_step_scheme_id_fkey" );
            }

            //workflow_scheme
            updateFrom( dc, tableName, "workflow_scheme", "id" );
            //workflow_step
            updateFrom( dc, tableName, "workflow_step", "scheme_id" );
            //workflow_scheme_x_structure
            updateFrom( dc, tableName, "workflow_scheme_x_structure", "scheme_id" );

            dc.executeStatement("drop table " + tableName);

        } catch ( SQLException e ) {
            throw new DotDataException( e.getMessage(), e );
        } finally {
            try {
                //Add back the constrains
                if ( DbConnectionFactory.getDBType().equals( DbConnectionFactory.MSSQL ) ) {
                    dc.executeStatement( "alter table workflow_scheme with check check constraint all" );
                    dc.executeStatement( "alter table workflow_step with check check constraint all" );
                } else if ( DbConnectionFactory.getDBType().equals( DbConnectionFactory.ORACLE ) ) {
                    enableDisableOracleConstrains( dc, "workflow_step", true );
                    enableDisableOracleConstrains( dc, "workflow_scheme", true );
                } else if ( !DbConnectionFactory.getDBType().equals( DbConnectionFactory.MYSQL ) ) {
                    dc.executeStatement( "ALTER TABLE workflow_step ADD CONSTRAINT workflow_step_scheme_id_fkey FOREIGN KEY (scheme_id) REFERENCES workflow_scheme (id)" );
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
    private static void updateFrom ( DotConnect dotConnect, String resultsTable, String updateTable, String updateColumn ) throws SQLException {

        if ( DbConnectionFactory.getDBType().equals( DbConnectionFactory.MYSQL ) ) {
            dotConnect.executeStatement( "UPDATE " + updateTable + " JOIN " + resultsTable + " ir on " + updateColumn + " = ir.local_inode SET " + updateColumn + " = ir.remote_inode" );
        } else if ( DbConnectionFactory.getDBType().equals( DbConnectionFactory.ORACLE ) ) {
            dotConnect.executeStatement( "UPDATE " + updateTable +
                    " SET " + updateColumn + " = (SELECT ir.remote_inode FROM " + resultsTable + " ir WHERE " + updateColumn + " = ir.local_inode)" +
                    " WHERE exists (SELECT ir.remote_inode FROM " + resultsTable + " ir WHERE " + updateColumn + " = ir.local_inode)" );
        } else {
            dotConnect.executeStatement( "UPDATE " + updateTable + " SET " + updateColumn + " = ir.remote_inode FROM " + resultsTable + " ir WHERE " + updateColumn + " = ir.local_inode" );
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
