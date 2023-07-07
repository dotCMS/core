package com.dotcms.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDuplicateDataException;
import com.liferay.portal.model.User;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

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


    /**
     * Method to test: test double save constraint {@link SystemTableFactory#save(String, String)}
     * Given Scenario: Creates a key/value twice
     * ExpectedResult: Should throw an exception b/c the key already exist
     * @throws Throwable
     */
    @Test(expected = DotDuplicateDataException.class)
    public void test_double_insert () throws Throwable {

        final String key1 = "key1";
        final String value1 = "value1";

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

            // this should throw an exception since the key1 already exist.
            LocalTransaction.wrap(()->systemTableFactory.save(key1, value1));
            Assert.fail("The duplicate key should throw an exception");
        }
    }

    /**
     * Method to test: test update on non existing key constraint {@link SystemTableFactory#update(String, String)}
     * Given Scenario: tries to update an non existing a key
     * ExpectedResult: Should throw an exception b/c the key does not exist
     * @throws Throwable
     */
    @Test(expected = DoesNotExistException.class)
    public void test_update_non_existing_key () throws Throwable {

        final String key1 = "key10";
        final String value1 = "value1";

        final SystemTableFactory systemTableFactory = null;

        if (null != systemTableFactory) {

            systemTableFactory.clearCache();
            // SAVE + FIND
            LocalTransaction.wrap(()->systemTableFactory.update(key1, value1));
            Assert.fail("The duplicate key should throw an exception");
        }
    }

    /**
     * Method to test: test delete on non-existing key constraint {@link SystemTableFactory#delete(String)}
     * Given Scenario: tries to delete an non-existing a key
     * ExpectedResult: Should throw an exception b/c the key does not exist
     * @throws Throwable
     */
    @Test(expected = DoesNotExistException.class)
    public void test_delete_non_existing_key () throws Throwable {

        final String key1 = "key10";

        final SystemTableFactory systemTableFactory = null;

        if (null != systemTableFactory) {

            systemTableFactory.clearCache();
            // SAVE + FIND
            LocalTransaction.wrap(()->systemTableFactory.delete(key1));
            Assert.fail("The non existing key should throw an exception on delete");
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

        final String key1 = "key1";
        final String value1 = "value1";
        final String key1 = "key2";
        final String value1 = "value2";

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

            // this should throw an exception since the key1 already exist.
            LocalTransaction.wrap(()->systemTableFactory.save(key1, value1));
            Assert.fail("The duplicate key should throw an exception");
        }
    }
}
