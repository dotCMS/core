package com.dotcms;

import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotcms.repackage.net.sf.hibernate.HibernateException;
import com.dotcms.repackage.org.junit.After;

import java.sql.SQLException;

/**
 * Created by Jonathan Gamba.
 * Date: 3/6/12
 * Time: 4:36 PM
 * <p/>
 * Annotations that can be use: {@link com.dotcms.repackage.junit.org.junit.BeforeClass @BeforeClass}, {@link com.dotcms.repackage.junit.org.junit.Before @Before},
 * {@link com.dotcms.repackage.junit.org.junit.Test @Test}, {@link com.dotcms.repackage.junit.org.junit.AfterClass @AfterClass},
 * {@link com.dotcms.repackage.junit.org.junit.After @After}, {@link com.dotcms.repackage.junit.org.junit.Ignore @Ignore}
 * <br>For managing the assertions use the static class {@link com.dotcms.repackage.junit.org.junit.Assert Assert}
 */
public abstract class TestBase {

    @After
    public void after () throws SQLException, DotHibernateException, HibernateException {

        //Closing the session
        HibernateUtil.getSession().connection().close();
        HibernateUtil.getSession().close();
    }

}