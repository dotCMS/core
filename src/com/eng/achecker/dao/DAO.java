package com.eng.achecker.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface DAO {

	public List<Map<String, Object>> execute(String sql) throws SQLException;

}
