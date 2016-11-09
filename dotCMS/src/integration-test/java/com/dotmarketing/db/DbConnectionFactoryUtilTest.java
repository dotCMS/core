package com.dotmarketing.db;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.util.IntegrationTestInitService;

public class DbConnectionFactoryUtilTest {
    

    
	@BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
	}
    
    @After
    @Before
    public void prep() throws Exception {
        HibernateUtil.closeSession();
    }
    
    /**
     * This test is for the inTransaction method
     * of the DBConnectionFacotry - and returns if we are in a session or not
     */
    @Test
    public void testInTransaction() throws Exception {
    	
    	
    	
    	
        Assert.assertFalse(DbConnectionFactory.inTransaction() );

    	HibernateUtil.startTransaction();
        Assert.assertTrue( DbConnectionFactory.inTransaction());
    	HibernateUtil.commitTransaction();
        Assert.assertFalse(DbConnectionFactory.inTransaction() );

    	DbConnectionFactory.getConnection();

        Assert.assertFalse(DbConnectionFactory.inTransaction() );
        HibernateUtil.closeSession();
    }
}
