package com.dotmarketing.common.reindex;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
    }

    @Test
    public void test_highestpriority_reindex_vs_normal_reindex() throws DotDataException {

        final List<Contentlet> contentlets = new ArrayList<>();

        ContentType type = new ContentTypeDataGen()
                .fields(ImmutableList
                        .of(ImmutableTextField.builder().name("Title").variable("title").searchable(true).listed(true).build()))
                .nextPersisted();

        for (int i = 0; i < numberToTest; i++) {
            contentlets.add(
                    new ContentletDataGen(type.id()).setProperty("title", "contentTest " + System.currentTimeMillis()).nextPersisted());
        }

        final ReindexQueueAPI reindexQueueAPI = APILocator.getReindexQueueAPI();

        new DotConnect().setSQL("delete from dist_reindex_journal").loadResult();

        final List<Contentlet> contentletsHighPriority = contentlets.subList(0, numberToTest / 2);
        final List<Contentlet> contentletsLowPriority = contentlets.subList(numberToTest / 2, contentlets.size());

        assertNotNull(contentletsHighPriority);
        assertTrue(contentletsHighPriority.size() == numberToTest / 2);

        assertNotNull(contentletsLowPriority);
        assertTrue(contentletsLowPriority.size() == numberToTest / 2);

        final Set<String> highIdentifiers =
                contentletsHighPriority.stream().filter(Objects::nonNull).map(Contentlet::getIdentifier).collect(Collectors.toSet());
        final Set<String> lowIdentifiers =
                contentletsLowPriority.stream().filter(Objects::nonNull).map(Contentlet::getIdentifier).collect(Collectors.toSet());

        reindexQueueAPI.addIdentifierReindex(lowIdentifiers);
        reindexQueueAPI.addReindexHighPriority(highIdentifiers);

        // fetch 50
        Map<String, ReindexEntry> reindexEntries = reindexQueueAPI.findContentToReindex(numberToTest / 2);

        assertNotNull(reindexEntries);
        assertTrue(reindexEntries.size() == numberToTest / 2);
        assertTrue(reindexEntries.keySet().containsAll(highIdentifiers));

        reindexEntries = reindexQueueAPI.findContentToReindex(numberToTest / 2);

        assertNotNull(reindexEntries);
        assertTrue(reindexEntries.size() == numberToTest / 2);
        assertTrue(reindexEntries.keySet().containsAll(lowIdentifiers));

    }

    @Test
    public void test_content_type_reindex() throws Exception {

        ContentType type = new ContentTypeDataGen().nextPersisted();

        for (int i = 0; i < numberToTest; i++) {
            new ContentletDataGen(type.id()).nextPersisted();
        }

        final ReindexQueueAPI reindexQueueAPI = APILocator.getReindexQueueAPI();

        new DotConnect().setSQL("delete from dist_reindex_journal").loadResult();

        Map<String, ReindexEntry> reindexEntries = reindexQueueAPI.findContentToReindex(numberToTest / 2);

        assertTrue(reindexEntries.size() == 0);
        List<Field> origFields = new ArrayList<>();
        List<Field> newFields = new ArrayList<>();
        origFields.addAll(type.fields());
        newFields.addAll(type.fields());

        newFields.add(
                ImmutableTextField.builder().name("asdasdasd").variable("asdasdasd").searchable(true).contentTypeId(type.id()).build());

        APILocator.getContentTypeAPI(APILocator.systemUser()).save(type, newFields);
        APILocator.getContentTypeAPI(APILocator.systemUser()).save(type, origFields);
        reindexEntries = reindexQueueAPI.findContentToReindex(numberToTest);
        assertTrue(reindexEntries.size() == numberToTest);
        assertTrue(reindexEntries.values().iterator().next().getPriority() == ReindexQueueFactory.Priority.STRUCTURE.dbValue());

    }

    @Test
    public void test_adding_content_adds_to_queue_after_transaction_commit() throws Exception {

        ContentType type = new ContentTypeDataGen()
                .fields(ImmutableList
                        .of(ImmutableTextField.builder().name("Title").variable("title").searchable(true).listed(true).build()))
                .nextPersisted();
        new DotConnect().setSQL("delete from dist_reindex_journal").loadResult();

        try {
            HibernateUtil.startTransaction();

            Contentlet con = new ContentletDataGen(type.id()).setProperty("title", "contentTest " + System.currentTimeMillis()).next();
            APILocator.getContentletAPI().checkin(con, APILocator.systemUser(), false);

            assertTrue(reindexQueueAPI.recordsInQueue() > 0);

            try (Connection conn = DbConnectionFactory.getDataSource().getConnection()) {
                long records = reindexQueueAPI.recordsInQueue(conn);
                assertTrue("other connections should not see the uncommited reindex records", records == 0);

            }

        } finally {
            HibernateUtil.closeAndCommitTransaction();
        }

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
    public void test_old_records_get_re_queued() throws Exception {

        ContentType type = new ContentTypeDataGen()
                .fields(ImmutableList
                        .of(ImmutableTextField.builder().name("Title").variable("title").searchable(true).listed(true).build()))
                .nextPersisted();
        new DotConnect().setSQL("delete from dist_reindex_journal").loadResult();

        Contentlet con = new ContentletDataGen(type.id()).setProperty("title", "contentTest " + System.currentTimeMillis()).nextPersisted();

        // get unclaimed records for reindex, Our new content is in there and is marked as claimed (e.g.
        // serverId is set to
        // this server)
        Map<String, ReindexEntry> reindexEntries = reindexQueueAPI.findContentToReindex();
        assertTrue(reindexEntries.containsKey(con.getIdentifier()));

        // no more unclaimed records for reindex
        reindexEntries = reindexQueueAPI.findContentToReindex();
        assertTrue(reindexEntries.isEmpty());

        // make the reindex record 3 minutes old in db
        Date oldRecord = new Date((System.currentTimeMillis() - (3 * 60 * 1000)));

        new DotConnect().setSQL("update dist_reindex_journal set time_entered = ?").addParam(oldRecord).loadResult();

        // record still not available because the server only calls re-queue every two minutes
        reindexEntries = reindexQueueAPI.findContentToReindex();
        assertTrue(reindexEntries.isEmpty());

        // force a requeue
        new ReindexQueueFactory().requeueStaleReindexRecords();

        // record is requeued for reindexing
        reindexEntries = reindexQueueAPI.findContentToReindex();
        assertTrue(reindexEntries.containsKey(con.getIdentifier()));

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

        ContentType type = new ContentTypeDataGen()
                .fields(ImmutableList
                        .of(ImmutableTextField.builder().name("Title").variable("title").searchable(true).listed(true).build()))
                .nextPersisted();

        new DotConnect().setSQL("delete from dist_reindex_journal").loadResult();

        Contentlet con = new ContentletDataGen(type.id()).setProperty("title", "contentTest " + System.currentTimeMillis()).nextPersisted();

        Map<String, ReindexEntry> reindexEntries = null;
        ReindexEntry entry = null;
        int i = 0;

        // we need to fail 1 time more than the REINDEX_MAX_FAILURE_ATTEMPTS
        while (i <= ReindexQueueFactory.REINDEX_MAX_FAILURE_ATTEMPTS) {
            // it has been marked as failed and is available again for reindex
            reindexEntries = reindexQueueAPI.findContentToReindex();
            assertTrue(reindexEntries.containsKey(con.getIdentifier()));
            entry = reindexEntries.values().stream().findFirst().orElse(null);
            assertNotNull(entry);
            assertTrue(entry.errorCount() == i);
            reindexQueueAPI.markAsFailed(entry, "failure:" + i);
            i++;
        }
        // entry has errored out and is no longer in queue
        reindexEntries = reindexQueueAPI.findContentToReindex();
        entry = reindexEntries.values().stream().findFirst().orElse(null);

        assertTrue(entry == null);

    }

}
