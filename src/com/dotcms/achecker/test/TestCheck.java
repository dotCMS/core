package com.dotcms.achecker.test;

import java.sql.SQLException;

import com.dotcms.achecker.CheckBean;
import com.dotcms.achecker.dao.ChecksDAO;


public class TestCheck {

	/**
	 * @param args
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		
		ChecksDAO dao = new ChecksDAO();
		
		CheckBean check = dao.getCheckByID(14);
		
		System.out.println(check.dump());
		
	}

}
