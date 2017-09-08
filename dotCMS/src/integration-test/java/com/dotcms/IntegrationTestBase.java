package com.dotcms;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotcms.repackage.net.sf.hibernate.HibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.BaseMessageResources;

import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import org.junit.After;
import org.junit.Assert;

import java.sql.SQLException;
import java.util.List;

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
public abstract class IntegrationTestBase extends BaseMessageResources {

    @After
    public void after () throws SQLException, DotHibernateException, HibernateException {

        //Closing the session
        HibernateUtil.getSession().connection().close();
        HibernateUtil.getSession().close();
    }

    /**
     * Util method to delete all the categories.
     */
    public void cleanCategories(List<Category> categoriesToDelete) {
        final CategoryAPI categoryAPI = APILocator.getCategoryAPI();
        final User systemUser = APILocator.systemUser();

        for (Category category : categoriesToDelete) {
            if (category != null && UtilMethods.isSet(category.getInode())) {
                try {
                    categoryAPI.delete(category, systemUser, false);
                } catch (DotDataException | DotSecurityException e) {
                    Assert.fail("Can't delete Category: " + category.getCategoryName() + ", with id:"
                            + category.getInode() + ", Exception: " + e.getMessage());
                }
            }
        }
    }

}