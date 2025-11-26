package com.dotcms.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDuplicateDataException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;

/**
 * Test for the {@link SystemTableFactory}
 * @author jsanca
 */
public class SystemTableFactoryTest extends IntegrationTestBase  {

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

        final SystemTableFactory systemTableFactory = FactoryLocator.getSystemTableFactory();

        if (null != systemTableFactory) {

            systemTableFactory.clearCache();
            // SAVE + FIND
            LocalTransaction.wrap(()->systemTableFactory.saveOrUpdate(key1, value1));
            final Optional<Object> value1FromDB =  wrapOnReadOnlyConn(()->systemTableFactory.find(key1));
            Assert.assertTrue("Should return something",  value1FromDB.isPresent());
            Assert.assertEquals(
                    "The value previous added should be the same of the value recovery from the db with the key: " + key1,
                    value1, value1FromDB.get());

            // UPDATE + FIND
            LocalTransaction.wrap(()->systemTableFactory.saveOrUpdate(key1, value2));
            final Optional<Object> value2FromDB =  wrapOnReadOnlyConn(()->systemTableFactory.find(key1));
            Assert.assertEquals(
                    "The value previous added should be the same of the value recovery from the db with the key: " + key1,
                    value2, value2FromDB.get());

            // DELETE + FIND
            LocalTransaction.wrap(()->systemTableFactory.delete(key1));
            final Optional<Object> value3FromDB =  wrapOnReadOnlyConn(()->systemTableFactory.find(key1));
            Assert.assertFalse("Should not return something",  value3FromDB.isPresent());
        }
    }


    /**
     * Method to test: test double save constraint {@link SystemTableFactory#save(String, Object)}
     * Given Scenario: Creates a key/value twice
     * ExpectedResult: Should throw an exception b/c the key already exist
     * @throws Throwable
     */
    @Test()
    public void test_double_insert () throws Throwable {

        final String key1 = "key13";
        final String value1 = "value1";

        final SystemTableFactory systemTableFactory = FactoryLocator.getSystemTableFactory();

        if (null != systemTableFactory) {

            systemTableFactory.clearCache();
            // SAVE + FIND
            LocalTransaction.wrap(()->systemTableFactory.saveOrUpdate(key1, value1));
            final Optional<Object> value1FromDB =  wrapOnReadOnlyConn(()->systemTableFactory.find(key1));
            Assert.assertTrue("Should return something",  value1FromDB.isPresent());
            Assert.assertEquals(
                    "The value previous added should be the same of the value recovery from the db with the key: " + key1,
                    value1, value1FromDB.get());

            // this should be an update
            LocalTransaction.wrap(()->systemTableFactory.saveOrUpdate(key1, value1));
            final Optional<Object> value2FromDB =  wrapOnReadOnlyConn(()->systemTableFactory.find(key1));
            Assert.assertTrue("Should return something",  value2FromDB.isPresent());
            Assert.assertEquals(
                    "The value previous added should be the same of the value recovery from the db with the key: " + key1,
                    value1, value2FromDB.get());

        }
    }


    /**
     * Method to test: test find all {@link SystemTableFactory#findAll()}
     * Given Scenario: Creates a couple key/value
     * ExpectedResult: Should retrieve both keys
     * @throws Throwable
     */
    @Test()
    public void test_find_all () throws Throwable {

        final String key1 = "key11";
        final String value1 = "value11";
        final String key2 = "key22";
        final String value2 = "value22";

        final SystemTableFactory systemTableFactory = FactoryLocator.getSystemTableFactory();

        if (null != systemTableFactory) {

            try {
                systemTableFactory.clearCache();
                // SAVE + FIND
                LocalTransaction.wrap(() -> systemTableFactory.saveOrUpdate(key1, value1));
                LocalTransaction.wrap(() -> systemTableFactory.saveOrUpdate(key2, value2));
                final Map<String, Object> value1FromDB = wrapOnReadOnlyConn(() -> systemTableFactory.findAll());
                Assert.assertTrue("Should has key1", value1FromDB.containsKey(key1));
                Assert.assertTrue("Should has key2", value1FromDB.containsKey(key2));
                Assert.assertEquals(
                        "The value previous added should be the same of the value recovery from the db with the key: " + key1,
                        value1, value1FromDB.get(key1));
                Assert.assertEquals(
                        "The value previous added should be the same of the value recovery from the db with the key: " + key2,
                        value2, value1FromDB.get(key2));
            } finally {
                try {
                    LocalTransaction.wrap(() -> systemTableFactory.delete(key1));
                    LocalTransaction.wrap(() -> systemTableFactory.delete(key2));
                } catch (Throwable e) {
                    Logger.debug(this, e.getMessage());
                }
            }
        }
    }
}
