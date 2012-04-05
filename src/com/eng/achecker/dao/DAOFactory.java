package com.eng.achecker.dao;


import org.apache.commons.logging.Log;

import com.eng.achecker.utility.PropertyLoader;


public class DAOFactory {
	
	private static DAO daoImpl = null;
	
	private static final Log LOG = org.apache.commons.logging.LogFactory.getLog(DAOFactory.class );

	public static DAO getDAO() throws Exception {
		String clsName = PropertyLoader.getValue(DBConstants.DAO_FACTORY_IMPL);
		try{
			Class cls = Class.forName(clsName);
			daoImpl = (DAO) cls.newInstance();
		}catch (Exception e) {
			LOG.error(e.getMessage(), e );
			throw new Exception();
		}
		return daoImpl;
	}

}
