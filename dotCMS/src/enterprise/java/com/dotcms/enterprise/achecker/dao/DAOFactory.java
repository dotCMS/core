/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.achecker.dao;


import org.apache.commons.logging.Log;

import com.dotcms.enterprise.achecker.dao.DAO;
import com.dotcms.enterprise.achecker.dao.DAOImpl;


public class DAOFactory {
	
	private static DAO daoImpl = null;
	
	private static final Log LOG = org.apache.commons.logging.LogFactory.getLog(DAOFactory.class );

	public static DAO getDAO() throws Exception {
		daoImpl = new DAOImpl();
		return daoImpl;
//		String clsName = PropertyLoader.getValue(DBConstants.DAO_FACTORY_IMPL);
//		try{
//			Class cls = Class.forName(clsName);
//			daoImpl = (DAO) cls.newInstance();
//		}catch (Exception e) {
//			LOG.error(e.getMessage(), e );
//			throw new Exception();
//		}
//		return daoImpl;
	}

}
