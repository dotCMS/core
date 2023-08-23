package com.dotcms.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDuplicateDataException;
import com.dotmarketing.util.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;

/**
 * Test for the {@link SystemAPI}
 * @author jsanca
 */
public class SystemAPITest extends IntegrationTestBase  {

    @BeforeClass
    public static void prepare () throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: test CRUD operations of {@link SystemTable}
     * Given Scenario: Creates a key/value, query it, update it and delete it
     * ExpectedResult: All operations should be successful
     * @throws Throwable
     */
    @Test
    public void test_crud_success () throws Throwable {

        final String key1 = "akey1";
        final String value1 = "value1";
        final String value2 = "value2";

        final SystemTable systemTable = APILocator.getSystemAPI().getSystemTable();

        if (null != systemTable) {

            // SAVE + FIND
            LocalTransaction.wrap(()->systemTable.set(key1, value1));
            final Optional<String> value1FromDB =  wrapOnReadOnlyConn(()->systemTable.get(key1));
            Assert.assertTrue("Should return something",  value1FromDB.isPresent());
            Assert.assertEquals(
                    "The value previous added should be the same of the value recovery from the db with the key: " + key1,
                    value1, value1FromDB.get());

            // UPDATE + FIND
            LocalTransaction.wrap(()->systemTable.set(key1, value2));
            final Optional<String> value2FromDB =  wrapOnReadOnlyConn(()->systemTable.get(key1));
            Assert.assertEquals(
                    "The value previous added should be the same of the value recovery from the db with the key: " + key1,
                    value2, value2FromDB.get());

            // DELETE + FIND
            LocalTransaction.wrap(()->systemTable.delete(key1));
            final Optional<String> value3FromDB =  wrapOnReadOnlyConn(()->systemTable.get(key1));
            Assert.assertFalse("Should not return something",  value3FromDB.isPresent());
        }
    }


    /**
     * Method to test: test double save constraint {@link SystemTable#save(String, String)}
     * Given Scenario: Creates a key/value twice
     * ExpectedResult: Should throw an exception b/c the key already exist
     * @throws Throwable
     */
    @Test()
    public void test_double_insert () throws Throwable {

        final String key1 = "akey13";
        final String value1 = "value1";

        final SystemTable systemTable = APILocator.getSystemAPI().getSystemTable();

        if (null != systemTable) {

            // SAVE + FIND
            LocalTransaction.wrap(()->systemTable.set(key1, value1));
            final Optional<String> value1FromDB =  wrapOnReadOnlyConn(()->systemTable.get(key1));
            Assert.assertTrue("Should return something",  value1FromDB.isPresent());
            Assert.assertEquals(
                    "The value previous added should be the same of the value recovery from the db with the key: " + key1,
                    value1, value1FromDB.get());

            // this should throw an exception since the key1 already exist.
            LocalTransaction.wrap(() -> systemTable.set(key1, value1));
            final Optional<String> value2FromDB =  wrapOnReadOnlyConn(()->systemTable.get(key1));
            Assert.assertTrue("Should return something",  value2FromDB.isPresent());
            Assert.assertEquals(
                    "The value previous added should be the same of the value recovery from the db with the key: " + key1,
                    value1, value2FromDB.get());
        }
    }


    /**
     * Method to test: test find all {@link SystemTable#all()}
     * Given Scenario: Creates a couple key/value
     * ExpectedResult: Should retrieve both keys
     * @throws Throwable
     */
    @Test()
    public void test_find_all () throws Throwable {

        final String key1 = "akey11";
        final String value1 = "value11";
        final String key2 = "akey22";
        final String value2 = "value22";

        final SystemTable systemTable = APILocator.getSystemAPI().getSystemTable();

        if (null != systemTable) {

            try {
                // SAVE + FIND
                LocalTransaction.wrap(() -> systemTable.set(key1, value1));
                LocalTransaction.wrap(() -> systemTable.set(key2, value2));
                final Map<String, String> value1FromDB = wrapOnReadOnlyConn(() -> systemTable.all());
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
                    LocalTransaction.wrap(() -> systemTable.delete(key1));
                    LocalTransaction.wrap(() -> systemTable.delete(key2));
                } catch (Throwable e) {
                    Logger.debug(this, e.getMessage());
                }
            }
        }
    }
}
