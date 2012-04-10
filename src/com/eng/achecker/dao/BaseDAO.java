package com.eng.achecker.dao;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BaseDAO {

	public static final String DEFAULT_PREFIX = "AC_";

	protected String tablePrefix;

	protected DAO dao;

	public BaseDAO() {
		try{
			dao = DAOFactory.getDAO();
			this.tablePrefix = DEFAULT_PREFIX;
		}catch (Exception e) {
			// TODO: handle exception
		}
	}

	public List<Map<String, Object>> execute(String sql) throws SQLException {
		return dao.execute(sql);
	}

	public <T> List<T> execute(Class<T> clazz, String sql) throws SQLException{
		try{
		List<Map<String, Object>> list = dao.execute(sql);
		List<T> result = new ArrayList<T>();
		for ( Map<String, Object> map : list) {
			Constructor<T> method = clazz.getConstructor(Map.class);
			result.add(method.newInstance(map));
		}
		return result;
		}catch (Exception e) {
			e.printStackTrace();
			throw new SQLException( e );
		}
	}

	public <T> T executeOne(Class<T> clazz, String sql)  throws SQLException  {
		try {
			Map<String, Object> map = executeOne(sql);
			Constructor<T> method = clazz.getConstructor(Map.class);
			return method.newInstance(map);
		}
		catch (Exception t) {
			t.printStackTrace();
			throw new SQLException( t );			
		}
		 
	}

	public Map<String, Object> executeOne(String sql) throws SQLException {
		List<Map<String, Object>> results = execute(sql);
		if ( results.size() > 0 )
			return results.get(0);
		return null;
	}

	public String getTablePrefix() {
		return tablePrefix;
	}

	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}

}
