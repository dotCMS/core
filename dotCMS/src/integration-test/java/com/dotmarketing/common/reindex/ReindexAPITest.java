package com.dotmarketing.common.reindex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.google.common.collect.ImmutableList;

/**
 * Test for {@link ReindexQueueAPI}
 */
public class ReindexAPITest extends IntegrationTestBase {

    int numberToTest = 20;
    static ReindexQueueAPI reindexQueueAPI = null;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        reindexQueueAPI = APILocator.getReindexQueueAPI();

        //wipe out any failed records
        reindexQueueAPI.deleteReindexAndFailedRecords();
    }

    @AfterClass
    public static void restartReindexThread() throws Exception {
      ReindexThread.unpause();
    }

    @Test
    public void test_highestpriority_reindex_vs_normal_reindex() throws DotDataException {

        //Pausing reindex to avoid race condition when findContentToReindex is called (no content will be indexed)
        ReindexThread.pause();

        final List<Contentlet> contentlets = new ArrayList<>();

        ContentType type = new ContentTypeDataGen()
                .fields(ImmutableList
                        .of(ImmutableTextField.builder().name("Title").variable("title")
                                .searchable(true).listed(true).build()))
                .nextPersisted();

        for (int i = 0; i < numberToTest; i++) {
            contentlets.add(
                    new ContentletDataGen(type.id())
                            .setProperty("title", "contentTest " + System.currentTimeMillis())
                            .nextPersisted());
        }

        final List<Contentlet> contentletsHighPriority = contentlets
                .subList(0, numberToTest / 2);
        final List<Contentlet> contentletsLowPriority = contentlets
                .subList(numberToTest / 2, contentlets.size());

        assertNotNull(contentletsHighPriority);
        assertEquals(numberToTest / 2, contentletsHighPriority.size());

        assertNotNull(contentletsLowPriority);
        assertEquals(numberToTest / 2, contentletsLowPriority.size());

        final Set<String> highIdentifiers =
                contentletsHighPriority.stream().filter(Objects::nonNull)
                        .map(Contentlet::getIdentifier).collect(Collectors.toSet());
        final Set<String> lowIdentifiers =
                contentletsLowPriority.stream().filter(Objects::nonNull)
                        .map(Contentlet::getIdentifier).collect(Collectors.toSet());

        final ReindexQueueAPI reindexQueueAPI = APILocator.getReindexQueueAPI();

        new DotConnect().setSQL("delete from dist_reindex_journal").loadResult();

        reindexQueueAPI.addIdentifierReindex(lowIdentifiers);
        reindexQueueAPI.addReindexHighPriority(highIdentifiers);

        // fetch 10
        Map<String, ReindexEntry> reindexEntries = reindexQueueAPI
                .findContentToReindex(numberToTest / 2);

        assertNotNull(reindexEntries);
        assertEquals(highIdentifiers.size(), reindexEntries.size());
        assertTrue(reindexEntries.keySet().containsAll(highIdentifiers));

        reindexEntries = reindexQueueAPI.findContentToReindex(numberToTest / 2);

        assertNotNull(reindexEntries);
        assertEquals(lowIdentifiers.size(), reindexEntries.size());
        assertTrue(reindexEntries.keySet().containsAll(lowIdentifiers));

        ReindexThread.unpause();
    }

    @Test
    public void test_content_type_reindex() throws Exception {
        ReindexThread.pause();
        ContentType type = new ContentTypeDataGen().nextPersisted();
        new DotConnect().setSQL("delete from dist_reindex_journal").loadResult();
        for (int i = 0; i < numberToTest; i++) {
            new ContentletDataGen(type.id()).setPolicy(IndexPolicy.DEFER).nextPersisted();
        }

        final ReindexQueueAPI reindexQueueAPI = APILocator.getReindexQueueAPI();



        Map<String, ReindexEntry> reindexEntries = reindexQueueAPI.findContentToReindex(numberToTest);

        assertTrue("should have " + numberToTest + " to reindex, only got" +  reindexEntries.size(),  reindexEntries.size()== numberToTest);


        new DotConnect().setSQL("delete from dist_reindex_journal").loadResult();
        reindexEntries = reindexQueueAPI.findContentToReindex();
        assertTrue("now we have none", reindexEntries.isEmpty());


        List<Field> newFields = new ArrayList<>();
        newFields.addAll(type.fields());

        newFields.add(
                ImmutableTextField.builder().name("asdasdasd").variable("asdasdasd").searchable(true).contentTypeId(type.id()).build());

        //Pausing reindex to avoid race condition when findContentToReindex is called (no content will be indexed)


        APILocator.getContentTypeAPI(APILocator.systemUser()).save(type, newFields);

        reindexEntries = reindexQueueAPI.findContentToReindex(numberToTest);
        assertEquals(numberToTest, reindexEntries.size());
        assertEquals(reindexEntries.values().iterator().next().getPriority(), ReindexQueueFactory.Priority.STRUCTURE.dbValue());
        ReindexThread.unpause();
    }

    @Test
    public void test_adding_content_adds_to_queue_after_transaction_commit() throws Exception {

        final ContentType type = new ContentTypeDataGen()
                .fields(ImmutableList
                        .of(ImmutableTextField.builder().name("Title").variable("title").searchable(true).listed(true).build()))
                .nextPersisted();
        LocalTransaction.wrap(()->new DotConnect().setSQL("delete from dist_reindex_journal").loadResult());


         
        LocalTransaction.wrap(()->{

            final Contentlet contentlet = new ContentletDataGen(type.id()).setPolicy(IndexPolicy.DEFER)
                    .setProperty("title", "contentTest " + System.currentTimeMillis()).next();
            APILocator.getContentletAPI().checkin(contentlet, APILocator.systemUser(), false);
            
            assertTrue(reindexQueueAPI.recordsInQueue() > 0);

            try {
              
                Connection conn = DbConnectionFactory.getDataSource().getConnection();
                long records = reindexQueueAPI.recordsInQueue(conn);
                assertEquals("other connections should not see the uncommited reindex records", 0, records);

            }catch(Exception e) {
              throw new DotRuntimeException(e);
            }
            
            assertTrue(reindexQueueAPI.recordsInQueue() > 0);

        });

        try (Connection conn = DbConnectionFactory.getDataSource().getConnection()) {

            assertTrue("other connections should NOW see the uncommited reindex records", reindexQueueAPI.recordsInQueue(conn) > 0);

        }

    }

    /**
     * If a record has been claimed by a server, but was never indexed, e.g. the server was shutdown
     * before the content ever got reindexed, then dotcms should pick it up again after a set period of
     * time: https://github.com/dotCMS/core/issues/7950
     * 
     * @throws Exception
     */
    @Test
    public void test_failed_records() throws Exception {
        ReindexThread.pause();
        final ContentType type = new ContentTypeDataGen()
                .fields(ImmutableList
                        .of(ImmutableTextField.builder().name("Title").variable("title")
                                .searchable(true).listed(true).build()))
                .nextPersisted();

        LocalTransaction.wrap(()-> new DotConnect().setSQL("delete from dist_reindex_journal").loadResult());

        //Pausing reindex to avoid race condition when findContentToReindex is called (no content will be indexed)

        final Contentlet contentlet = new ContentletDataGen(type.id()).setPolicy(IndexPolicy.DEFER)
                .setProperty("title", "contentTest " + System.currentTimeMillis()).nextPersisted();

        Map<String, ReindexEntry> reindexEntries;
        ReindexEntry entry;
        int i = 0;

        // we need to fail 1 time more than the REINDEX_MAX_FAILURE_ATTEMPTS
        while (i <= ReindexQueueFactory.REINDEX_MAX_FAILURE_ATTEMPTS) {
            // it has been marked as failed and is available again for reindex
            ReindexQueueFactory.resetLastIdReindexed();
            reindexEntries = reindexQueueAPI.findContentToReindex();
            assertTrue("The id: " + contentlet.getIdentifier() + " is not on: " + reindexEntries,
                    reindexEntries.containsKey(contentlet.getIdentifier()));
            entry = reindexEntries.values().stream().findFirst().orElse(null);
            assertNotNull(entry);
            assertEquals(i, entry.errorCount());
            reindexQueueAPI.markAsFailed(entry, "failure:" + i);
            i++;
        }

        // entry has errored out and is no longer in queue
        reindexEntries = reindexQueueAPI.findContentToReindex();
        entry = reindexEntries.values().stream().findFirst().orElse(null);

        assertNull(entry);

        ReindexThread.unpause();

    }

    /**
     * If a record has been claimed by a server, but was never indexed, e.g. the server was shutdown
     * before the content ever got reindexed, then dotcms should pick it up again after a set period of
     * time: https://github.com/dotCMS/core/issues/7950
     * 
     * @throws Exception
     */
    @Test
    public void test_deleted_records() throws Exception {

        final List<Contentlet> contentlets = new ArrayList<>();
        final ContentType type = new ContentTypeDataGen()
                .fields(ImmutableList
                        .of(ImmutableTextField.builder().name("Title").variable("title")
                                .searchable(true).listed(true).build()))
                .nextPersisted();

        for (int i = 0; i < numberToTest; i++) {
            contentlets.add(
                    new ContentletDataGen(type.id())
                            .setProperty("title", "contentTest " + System.currentTimeMillis())
                            .nextPersisted());
        }

        final List<String> ids = contentlets.stream().map(c -> c.getIdentifier())
                .collect(Collectors.toList());

        final ReindexQueueAPI reindexQueueAPI = APILocator.getReindexQueueAPI();

        new DotConnect().setSQL("delete from dist_reindex_journal").loadResult();

        //Pausing reindex to avoid race condition when findContentToReindex is called (no content will be indexed)
        ReindexThread.pause();
        reindexQueueAPI.addIdentifierDelete(ids);


        Map<String, ReindexEntry> reindexEntries = reindexQueueAPI.findContentToReindex();
        assertEquals(ids.size(), reindexEntries.size());
        for(ReindexEntry entry : reindexEntries.values()) {
            assertTrue(ids.contains(entry.getIdentToIndex()));
            assertTrue(entry.isDelete());
        }

        ReindexThread.unpause();
    }
}
