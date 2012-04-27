package com.dotcms.achecker.test;


import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.dotcms.achecker.dao.DAOImpl;

public class TestDAO {

	/**
	 * @param args
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		
		DAOImpl dao = new DAOImpl();
		
		List<Map<String, Object>> results = dao.execute("select * from AC_guidelines");

		for ( Map<String, Object> record : results ) {

			System.out.println(record);

		}

	}

}
