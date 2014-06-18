package com.dotcms.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
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

	public File generateFoldersToCheckCSV(String outputFile) throws DotDataException, IOException {
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

	public File generateStructuresToCheckCSV(String outputFile) throws DotDataException, IOException {
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

	public File generateSchemesToCheckCSV(String outputFile) throws DotDataException, IOException {
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

	public File generateFoldersToFixCSV(String outputFile, String endpointId) throws DotDataException, IOException {
		Connection conn = DbConnectionFactory.getConnection();
		ResultSet rs = null;
		PreparedStatement statement = null;
		CsvWriter writer = null;
		File csvFile = null;

		try {
			csvFile = new File(outputFile);
			writer = new CsvWriter(new FileWriter(csvFile, true), '|');

			String resultsTable = getResultsTableName(endpointId, IntegrityType.FOLDERS);

			statement = conn.prepareStatement("select folder, host_name, local_inode, remote_inode from " + resultsTable);
			rs = statement.executeQuery();
			int count = 0;

			while (rs.next()) {
				writer.write(rs.getString("folder"));
				writer.write(rs.getString("host_name"));
				writer.write(rs.getString("local_inode"));
				writer.write(rs.getString("remote_inode"));
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

	public File generateStructuresToFixCSV(String outputFile, String endpointId) throws DotDataException, IOException {
		Connection conn = DbConnectionFactory.getConnection();
		ResultSet rs = null;
		PreparedStatement statement = null;
		CsvWriter writer = null;
		File csvFile = null;

		try {
			csvFile = new File(outputFile);
			writer = new CsvWriter(new FileWriter(csvFile, true), '|');

			String resultsTable = getResultsTableName(endpointId, IntegrityType.STRUCTURES);

			statement = conn.prepareStatement("select velocity_name, local_inode, remote_inode from " + resultsTable);
			rs = statement.executeQuery();
			int count = 0;

			while (rs.next()) {
				writer.write(rs.getString("velocity_name"));
				writer.write(rs.getString("local_inode"));
				writer.write(rs.getString("remote_inode"));
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

	public File generateSchemesToFixCSV(String outputFile, String endpointId) throws DotDataException, IOException {
		Connection conn = DbConnectionFactory.getConnection();
		ResultSet rs = null;
		PreparedStatement statement = null;
		CsvWriter writer = null;
		File csvFile = null;

		try {
			csvFile = new File(outputFile);
			writer = new CsvWriter(new FileWriter(csvFile, true), '|');

			String resultsTable = getResultsTableName(endpointId, IntegrityType.SCHEMES);

			statement = conn.prepareStatement("select name, local_inode, remote_inode from " + resultsTable);
			rs = statement.executeQuery();
			int count = 0;

			while (rs.next()) {
				writer.write(rs.getString("name"));
				writer.write(rs.getString("local_inode"));
				writer.write(rs.getString("remote_inode"));
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
			if(foldersToCheckCsvFile!=null && foldersToCheckCsvFile.exists())
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

	public void generateDataToFixZip(String endpointId) {
		File foldersToFixCsvFile = null;
        File structuresToFixCsvFile = null;
        File schemesToFixCsvFile = null;
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

        	foldersToFixCsvFile = integrityUtil.generateFoldersToCheckCSV(outputPath + File.separator + IntegrityType.FOLDERS.getDataToFixCSVName());
        	structuresToFixCsvFile = integrityUtil.generateStructuresToCheckCSV(outputPath + File.separator + IntegrityType.STRUCTURES.getDataToFixCSVName());
        	schemesToFixCsvFile = integrityUtil.generateSchemesToCheckCSV(outputPath + File.separator + IntegrityType.SCHEMES.getDataToFixCSVName());

        	addToZipFile(foldersToFixCsvFile.getAbsolutePath(), zos, IntegrityType.FOLDERS.getDataToFixCSVName());
        	addToZipFile(structuresToFixCsvFile.getAbsolutePath(), zos, IntegrityType.STRUCTURES.getDataToFixCSVName());
        	addToZipFile(schemesToFixCsvFile.getAbsolutePath(), zos, IntegrityType.SCHEMES.getDataToFixCSVName());

        	zos.close();
        	fos.close();
		} catch (Exception e) {
			if(foldersToFixCsvFile!=null && foldersToFixCsvFile.exists())
				zipFile.delete();
		} finally {
        	if(foldersToFixCsvFile!=null && foldersToFixCsvFile.exists())
        		foldersToFixCsvFile.delete();
        	if(structuresToFixCsvFile!=null && structuresToFixCsvFile.exists())
        		structuresToFixCsvFile.delete();
        	if(schemesToFixCsvFile!=null && schemesToFixCsvFile.exists())
        		schemesToFixCsvFile.delete();

        }
	}

	public void checkFoldersIntegrity(String endpointId) throws Exception {

		try {

			CsvReader folders = new CsvReader(ConfigUtils.getIntegrityPath() + File.separator + endpointId + File.separator + IntegrityType.FOLDERS.name() + ".csv", '|');
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
			}

			// lets drop the temp table
			dc.executeStatement("drop table " + tempTableName );

		} catch(Exception e) {
			throw new Exception("Error running the Folders Integrity Check", e);
		}
	}

	public void checkStructuresIntegrity(String endpointId) throws Exception {

		try {

			CsvReader structures = new CsvReader(ConfigUtils.getIntegrityPath() + File.separator + endpointId + File.separator + IntegrityType.STRUCTURES.name() + ".csv", '|');
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
			}

			// lets drop the temp table
			dc.executeStatement("drop table " + tempTableName );


		} catch(Exception e) {
			throw new Exception("Error running the Structures Integrity Check", e);
		}
	}

	public void checkWorkflowSchemesIntegrity(String endpointId) throws Exception {

		try {

			CsvReader schemes = new CsvReader(ConfigUtils.getIntegrityPath() + File.separator + endpointId + File.separator + IntegrityType.SCHEMES.name() + ".csv", '|');
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

	private String getTempTableName(String endpointId, IntegrityType type) {
		String endpointIdforDB = endpointId.replace("-", "");
		String resultsTableName = type.name().toLowerCase() + "_temp_" + endpointIdforDB;

		if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)) {
			resultsTableName = resultsTableName.substring(0, 29);
		}

		return resultsTableName;
	}

	private String getResultsTableName(String endpointId, IntegrityType type) {
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


}
