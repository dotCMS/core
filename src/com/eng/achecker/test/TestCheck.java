package com.eng.achecker.test;

import java.sql.SQLException;

import com.eng.achecker.CheckBean;
import com.eng.achecker.dao.ChecksDAO;


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
