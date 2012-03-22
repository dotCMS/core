package com.dotcms;

import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import net.sf.hibernate.HibernateException;
import org.junit.After;

import java.sql.SQLException;

/**
 * Created by Jonathan Gamba.
 * Date: 3/6/12
 * Time: 4:36 PM
 * <p/>
 * Annotations that can be use: {@link org.junit.BeforeClass @BeforeClass}, {@link org.junit.Before @Before},
 * {@link org.junit.Test @Test}, {@link org.junit.AfterClass @AfterClass},
 * {@link org.junit.After @After}, {@link org.junit.Ignore @Ignore}
 * <br>For managing the assertions use the static class {@link org.junit.Assert Assert}
 */
public abstract class TestBase {

    @After
    public void after () throws SQLException, DotHibernateException, HibernateException {

        //Closing the session
        HibernateUtil.getSession().connection().close();
        HibernateUtil.getSession().close();
    }

}