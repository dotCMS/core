package com.dotcms.rest;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.csvreader.CsvWriter;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;

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
	
	
}
