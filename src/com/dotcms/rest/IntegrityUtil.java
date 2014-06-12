package com.dotcms.rest;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.csvreader.CsvWriter;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;

public class IntegrityUtil {

	public static List<Map<String, String>> writeFoldersCSV(CsvWriter writer) throws DotDataException, IOException {
		List<Map<String, String>> folders = new ArrayList<Map<String, String>>();
		DotConnect dc = new DotConnect();
		dc.setSQL("select f.inode, i.parent_path, i.asset_name, i.host_inode from folder f join identifier i on f.identifier = i.id ");

		List<Map<String, Object>> results = dc.loadObjectResults();



		Connection conn = DbConnectionFactory.getConnection();
		ResultSet rs = null;
		PreparedStatement statement = null;

		try {

			statement = conn.prepareStatement("select f.inode, i.parent_path, i.asset_name, i.host_inode from folder f join identifier i on f.identifier = i.id ");
			rs = statement.executeQuery();

			while (rs.next()) {
				writer.write(rs.getString("inode"));
				writer.write(rs.getString("parent_path"));
				writer.write(rs.getString("asset_name"));
				writer.write(rs.getString("host_inode"));
			}

		} catch (SQLException e) {
			throw new DotDataException(e.getMessage(),e);
		}finally {
        	try { if (rs != null) rs.close(); } catch (Exception e) { }
        	try { if ( statement!= null ) statement.close(); } catch (Exception e) { }
        }


		for (Map<String, Object> row : results) {
			Map<String, String> folder = new HashMap<String, String>();
			folder.put("inode", (String)row.get("inode"));
			folder.put("parent_path", (String)row.get("parent_path"));
			folder.put("asset_name", (String)row.get("asset_name"));
			folder.put("host_inode", (String)row.get("host_inode"));
            folders.add(folder);
		}

		return folders;
	}
}
