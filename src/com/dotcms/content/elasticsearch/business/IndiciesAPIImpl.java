package com.dotcms.content.elasticsearch.business;

import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;

public class IndiciesAPIImpl implements IndiciesAPI {
    
    protected final IndiciesFactory ifac=FactoryLocator.getIndiciesFactory();
    
    public IndiciesInfo loadIndicies() throws DotDataException {
        return ifac.loadIndicies();
    }
    
    public void point(IndiciesInfo info) throws DotDataException {
        boolean autocommit=false;
        try {
            autocommit=DbConnectionFactory.getConnection().getAutoCommit();
            if(autocommit){
                HibernateUtil.startTransaction();
            }   
            ifac.point(info);
        }
        catch(Exception ex) {
            if(autocommit){
                HibernateUtil.rollbackTransaction();
            }
            throw new DotDataException("exception updating index data",ex);
        }finally{
        	if(autocommit){
        		try{
        			HibernateUtil.commitTransaction();
        		}catch (Exception e) {
					Logger.error(this, e.getMessage(), e);
				}
        	}
        }
    }
    
}
