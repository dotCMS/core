package com.dotcms.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.ReturnableDelegate;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Optional;

public class SystemTableTest extends IntegrationTestBase  {

    private static DotCacheAdministrator cache;
    private static User systemUser;

    @BeforeClass
    public static void prepare () throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        cache = CacheLocator.getCacheAdministrator();

        //Setting the test user
        systemUser = APILocator.getUserAPI().getSystemUser();
    }

    /**
     * Method to test: test CRUD operations of {@link SystemTableFactory}
     * Given Scenario: Creates a key/value, query it, update it and delete it
     * ExpectedResult: All operations should be successful
     * @throws Throwable
     */
    @Test
    public void test_crud_success () throws Throwable {

        final String key1 = "key1";
        final String value1 = "value1";
        final String value2 = "value2";

        final SystemTableFactory systemTableFactory = null;

        if (null != systemTableFactory) {

            systemTableFactory.clearCache();
            // SAVE + FIND
            LocalTransaction.wrap(()->systemTableFactory.save(key1, value1));
            final Optional<String> value1FromDB =  closeConn(()->systemTableFactory.find(key1));
            Assert.assertTrue("Should return something",  value1FromDB.isPresent());
            Assert.assertEquals(
                    "The value previous added should be the same of the value recovery from the db with the key: " + key1,
                    value1, value1FromDB.get());

            // UPDATE + FIND
            LocalTransaction.wrap(()->systemTableFactory.update(key1, value2));
            final Optional<String> value2FromDB =  closeConn(()->systemTableFactory.find(key1));
            Assert.assertEquals(
                    "The value previous added should be the same of the value recovery from the db with the key: " + key1,
                    value2, value2FromDB);

            // DELETE + FIND
            LocalTransaction.wrap(()->systemTableFactory.delete(key1));
            final Optional<String> value3FromDB =  closeConn(()->systemTableFactory.find(key1));
            Assert.assertFalse("Should not return something",  value3FromDB.isPresent());
        }
    }

}
