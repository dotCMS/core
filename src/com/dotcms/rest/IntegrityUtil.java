package com.dotcms.rest;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.ConfigUtils;

public class IntegrityUtil {

	public static void writeFoldersCSV(CsvWriter writer) throws DotDataException, IOException {
		Connection conn = DbConnectionFactory.getConnection();
		ResultSet rs = null;
		PreparedStatement statement = null;

		try {

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
        }

	}

	public static void writeStructuresCSV(CsvWriter writer) throws DotDataException, IOException {
		Connection conn = DbConnectionFactory.getConnection();
		ResultSet rs = null;
		PreparedStatement statement = null;

		try {

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
        }

	}

	public static void writeWorkflowSchemesCSV(CsvWriter writer) throws DotDataException, IOException {
		Connection conn = DbConnectionFactory.getConnection();
		ResultSet rs = null;
		PreparedStatement statement = null;

		try {

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
        }

	}

	public static void checkFoldersIntegrity(String endpointId) throws Exception {

		try {

			CsvReader folders = new CsvReader(ConfigUtils.getIntegrityPath() + File.separator + endpointId + File.separator + "folders.csv", '|');
			boolean tempCreated = false;
			DotConnect dc = new DotConnect();
			String endpointIdforDB = DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)?endpointId.replace("-", "").substring(0, 17):endpointId.replace("-", "");

			String createTempTable = "create table folder_temp_" +endpointIdforDB+ " (inode varchar(36) not null, parent_path varchar(255), "
					+ "asset_name varchar(255), host_identifier varchar(36) not null, primary key (inode) )";

			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)) {
				createTempTable=createTempTable.replaceAll("varchar\\(", "varchar2\\(");
			}

			final String INSERT_TEMP_TABLE = "insert into folder_temp_" + endpointIdforDB + " values(?,?,?,?)";

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

		} catch(Exception e) {
			throw new Exception("Error running the Folders Integrity Check", e);
		}
	}


}
