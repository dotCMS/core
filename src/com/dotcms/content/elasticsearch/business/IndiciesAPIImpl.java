package com.dotcms.content.elasticsearch.business;

import java.sql.Connection;
import java.sql.SQLException;

import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;

public class IndiciesAPIImpl implements IndiciesAPI {
    
    protected final IndiciesFactory ifac=FactoryLocator.getIndiciesFactory();
    
    public IndiciesInfo loadIndicies() throws DotDataException {
        return loadIndicies(DbConnectionFactory.getConnection());
    }
    
    public IndiciesInfo loadIndicies(Connection conn) throws DotDataException {
        return ifac.loadIndicies(conn);
    }
    
    public void point(IndiciesInfo info) throws DotDataException {
        point(DbConnectionFactory.getConnection(),info);
    }
    
    public void point(Connection conn, IndiciesInfo info) throws DotDataException {
        boolean autocommit=false;
        try {
            autocommit=conn.getAutoCommit();
            if(autocommit){
                conn.setAutoCommit(false);
            }   
            ifac.point(conn,info);
        }
        catch(Exception ex) {
            if(autocommit){
                try {
                    conn.rollback();
                } catch (SQLException e) {
                    Logger.warn(this, e.getMessage(),e);
                }
            }
            throw new DotDataException("exception updating index data",ex);
        }finally{
        	if(autocommit){
        		try{
        			conn.commit();
        		}catch (Exception e) {
					Logger.error(this, e.getMessage(), e);
				}
        	}
        }
    }
    
}
