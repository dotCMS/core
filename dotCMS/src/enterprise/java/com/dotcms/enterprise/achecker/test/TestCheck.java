/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.achecker.test;

import java.sql.SQLException;

import com.dotcms.enterprise.achecker.CheckBean;
import com.dotcms.enterprise.achecker.dao.ChecksDAO;


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
