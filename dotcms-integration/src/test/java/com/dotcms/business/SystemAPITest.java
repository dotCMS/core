package com.dotcms.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.marshal.MarshalFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDuplicateDataException;
import com.dotmarketing.startup.runalways.Task00002LoadClusterLicenses;
import com.dotmarketing.startup.runonce.Task230707CreateSystemTable;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Test for the {@link SystemAPI}
 * @author jsanca
 */
public class SystemAPITest extends IntegrationTestBase  {

    /** The cluster wide event is published from a commit listener, which may run on a separate thread. */
    private static final int  EVENT_POLL_ATTEMPTS        = 20;
    private static final long EVENT_POLL_INTERVAL_MILLIS = 500L;

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

    /**
     * Method to test: {@link SystemAPI#getSystemTable()}
     * Given Scenario:
     *  1) deletes the system table if exist
     *  2) call the init method to redo
     * ExpectedResult: Since the API creates the table if not exist, the call should be successful
     * @throws Throwable
     */
    @Test()
    public void test_drop_and_re_init () throws Throwable {

        LocalTransaction.wrap(()->Try.run(()->new DotConnect().executeStatement("DROP TABLE IF EXISTS system_table")));
        final SystemTable systemTable = APILocator.getSystemAPI().getSystemTable();
        Assert.assertNotNull(systemTable);
        boolean shouldRun = LocalTransaction.wrapReturn(()->new Task230707CreateSystemTable().forceRun());
        Assert.assertTrue("The table should not exist here", shouldRun);
        SystemTableImpl.class.cast(systemTable).initIfNeeded(); // re-create the table
        shouldRun = LocalTransaction.wrapReturn(()->new Task230707CreateSystemTable().forceRun());
        Assert.assertFalse(shouldRun); // this second time should exist since the table was recreated
        // the idea would be on runtime if the system table is being instance and the table does not exist, it should be created
        // since the IT initialization flow works, the system table is being created in advance so the init was already called, so need to emulate the drop
    }

    /**
     * Method to test: {@link SystemTable#set(String, String)}
     * Given Scenario: A key is set in the system table inside a transaction
     * ExpectedResult: A CLUSTER_WIDE_EVENT carrying a {@link SystemTableUpdatedKeyEvent} for that key
     * is published, and its payload reads back as a typed event -- which is what a remote node relies
     * on to notify its own subscribers and re-resolve the key.
     * @throws Throwable
     */
    @Test
    public void test_set_publishes_cluster_wide_event() throws Throwable {

        final String key = "clusterWideSetKey" + System.currentTimeMillis();
        final SystemTable systemTable = APILocator.getSystemAPI().getSystemTable();
        final long since = System.currentTimeMillis();

        try {
            LocalTransaction.wrap(() -> systemTable.set(key, "value1"));
            assertClusterWideEventPublished(key, since);
        } finally {
            Try.run(() -> LocalTransaction.wrap(() -> systemTable.delete(key)))
                    .onFailure(e -> Logger.debug(this, e.getMessage()));
        }
    }

    /**
     * Method to test: {@link SystemTable#delete(String)}
     * Given Scenario: An existing key is deleted from the system table
     * ExpectedResult: The pre-existing cluster wide propagation on delete is unchanged -- a
     * CLUSTER_WIDE_EVENT is published and its payload reads back as a typed event.
     * @throws Throwable
     */
    @Test
    public void test_delete_publishes_cluster_wide_event() throws Throwable {

        final String key = "clusterWideDeleteKey" + System.currentTimeMillis();
        final SystemTable systemTable = APILocator.getSystemAPI().getSystemTable();
        LocalTransaction.wrap(() -> systemTable.set(key, "value1"));

        final long since = System.currentTimeMillis();
        LocalTransaction.wrap(() -> systemTable.delete(key));
        assertClusterWideEventPublished(key, since);
    }

    /**
     * Asserts that a cluster wide event for the given key was published and that its payload can be
     * rebuilt into a typed {@link SystemTableUpdatedKeyEvent}. The second half matters as much as the
     * first: the receiving node unmarshals the payload before notifying its subscribers, so a payload
     * that cannot be rebuilt propagates nothing.
     *
     * @param key   The system table key that was changed.
     * @param since Timestamp taken right before the change, used to scope the lookup.
     */
    private void assertClusterWideEventPublished(final String key, final long since) throws Exception {

        final String payloadJson = pollForClusterWideEventPayload(key, since);
        Assert.assertNotNull("No CLUSTER_WIDE_EVENT was published for the system table key: " + key
                + ". Other nodes in the cluster would never re-resolve it.", payloadJson);

        final Payload payload = MarshalFactory.getInstance().getMarshalUtils()
                .unmarshal(payloadJson, Payload.class);
        Assert.assertTrue("The cluster wide payload for the key: " + key + " must be readable back as a "
                        + SystemTableUpdatedKeyEvent.class.getSimpleName()
                        + ", otherwise the receiving node cannot notify its subscribers. Got: " + payloadJson,
                payload.getData() instanceof SystemTableUpdatedKeyEvent);
        Assert.assertEquals("The rebuilt event must carry the key that was changed",
                key, SystemTableUpdatedKeyEvent.class.cast(payload.getData()).getKey());
    }

    /**
     * Polls the system_event table for a cluster wide event mentioning the given key. The raw payload
     * is read straight from the database so that an unrelated event elsewhere in the window cannot
     * make this assertion flake.
     *
     * @param key   The system table key that was changed.
     * @param since Timestamp taken right before the change, used to scope the lookup.
     * @return The raw payload of the matching event, or {@code null} if none was published in time.
     */
    private String pollForClusterWideEventPayload(final String key, final long since) throws Exception {

        for (int i = 0; i < EVENT_POLL_ATTEMPTS; i++) {

            final DotConnect dotConnect = new DotConnect();
            dotConnect.setSQL("SELECT payload FROM system_event WHERE event_type = ? AND created >= ?");
            dotConnect.addParam(SystemEventType.CLUSTER_WIDE_EVENT.name());
            dotConnect.addParam(since);
            final List<Map<String, Object>> rows = dotConnect.loadObjectResults();

            for (final Map<String, Object> row : rows) {

                final String payloadJson = (String) row.get("payload");
                if (UtilMethods.isSet(payloadJson) && payloadJson.contains(key)) {
                    return payloadJson;
                }
            }

            DateUtil.sleep(EVENT_POLL_INTERVAL_MILLIS);
        }

        return null;
    }
}
