package com.dotmarketing.common.reindex;

import static com.dotmarketing.common.reindex.ReindexQueueFactory.REINDEX_MAX_FAILURE_ATTEMPTS;
import static graphql.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.reindex.ReindexQueueFactory.Priority;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import org.junit.BeforeClass;
import org.junit.Test;

public class ReindexQueueFactoryTest {

    private static final ReindexQueueFactory factory = new ReindexQueueFactory();

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();

    }

    /**
     * Method to test: {@link ReindexQueueFactory#requeueStaleReindexRecords()}
     * Test Case: A piece of content is put under a stale state
     * Expected Results: After calling the {@link ReindexQueueFactory#requeueStaleReindexRecords()} method, the piece of content is added to the queue to be indexed
     * @throws DotDataException
     */
    @Test
    public void testRequeueStaleReindexRecords() throws DotDataException {
        ReindexThread.pause();
        try {
            Config.setProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", true);
            final DotConnect dotConnect = new DotConnect();
            dotConnect.setSQL("delete from dist_reindex_journal").loadResult();
            ContentType type = new ContentTypeDataGen().nextPersisted();
            new ContentletDataGen(type.id()).setPolicy(IndexPolicy.DEFER).nextPersisted();

            dotConnect.setSQL("update dist_reindex_journal set priority = ?")
                    .addParam(Priority.ERROR.dbValue() + REINDEX_MAX_FAILURE_ATTEMPTS + 1).loadResult();

            assertEquals(0, factory.recordsInQueue());
            factory.requeueStaleReindexRecords();
            assertEquals(0, factory.recordsInQueue());

            ReindexThread.pause();
            dotConnect.setSQL("update dist_reindex_journal set priority = ?")
                    .addParam(Priority.REINDEX.dbValue()).loadResult();
            assertEquals(1, factory.recordsInQueue());
            factory.requeueStaleReindexRecords();
            assertEquals(1, factory.findContentToReindex(1).size());
        }finally {
            Config.setProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", false);
            ReindexThread.unpause();
        }
    }

    /**
     * Method to test: {@link ReindexQueueFactory#refreshContentUnderHost(Host)}
     * Test Case: Every piece of content in a host is added to the queue to be indexed
     * Expected Results: The reindex queue must be greater than 1
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void testRefreshContentUnderHost() throws DotDataException, DotSecurityException {
        ReindexThread.pause();

        try {
            Config.setProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", true);
            final DotConnect dotConnect = new DotConnect();
            ContentType type = new ContentTypeDataGen().nextPersisted();
            new ContentletDataGen(type.id()).nextPersisted();

            dotConnect.setSQL("delete from dist_reindex_journal").loadResult();

            final Host host = APILocator.getHostAPI()
                    .findDefaultHost(APILocator.systemUser(), false);

            factory.refreshContentUnderHost(host);

            assertTrue(factory.findContentToReindex(1).size() > 0);

        } finally {
            Config.setProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", false);
            ReindexThread.unpause();
        }
    }

    /**
     * Method to test: {@link ReindexQueueFactory#refreshContentUnderFolderPath(String, String)}
     * Test Case: Every piece of content under a folder is added to the queue to be indexed
     * Expected Results: The reindex queue must be equals to 1
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void testRefreshContentUnderFolderPath() throws DotDataException, DotSecurityException {
        ReindexThread.pause();

        try {
            Config.setProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", true);
            final String folderName = "myTestingFolder" + System.currentTimeMillis();
            final DotConnect dotConnect = new DotConnect();
            final ContentType type = new ContentTypeDataGen().nextPersisted();
            final Host host = APILocator.getHostAPI()
                    .findDefaultHost(APILocator.systemUser(), false);
            final Folder folder = new FolderDataGen().site(host).name(folderName).nextPersisted();
            new ContentletDataGen(type.id()).folder(folder).nextPersisted();

            dotConnect.setSQL("delete from dist_reindex_journal").loadResult();

            factory.refreshContentUnderFolderPath(host.getIdentifier(), folder.getPath());

            assertEquals(1, factory.findContentToReindex(1).size());
        } finally {
            Config.setProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", false);
            ReindexThread.unpause();
        }
    }

    /**
     * Method to test: {@link ReindexQueueFactory#refreshContentUnderFolder(Folder)}
     * Test Case: Every piece of content under a folder is added to the queue to be indexed
     * Expected Results: The reindex queue must be equals to 1
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void testRefreshContentUnderFolder() throws DotDataException, DotSecurityException {
        ReindexThread.pause();

        try {
            Config.setProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", true);
            final String folderName = "myTestingFolder" + System.currentTimeMillis();
            final DotConnect dotConnect = new DotConnect();
            final ContentType type = new ContentTypeDataGen().nextPersisted();
            final Host host = APILocator.getHostAPI()
                    .findDefaultHost(APILocator.systemUser(), false);
            final Folder folder = new FolderDataGen().site(host).name(folderName).nextPersisted();
            new ContentletDataGen(type.id()).folder(folder).nextPersisted();

            dotConnect.setSQL("delete from dist_reindex_journal").loadResult();

            factory.refreshContentUnderFolder(folder);

            assertEquals(1, factory.findContentToReindex(1).size());
        } finally {
            Config.setProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", false);
            ReindexThread.unpause();
        }
    }

    /**
     * Method to test: {@link ReindexQueueFactory#addStructureReindexEntries(String)}
     * Test Case: Every piece of content in a content type is added to the queue to be indexed
     * Expected Results: The reindex queue must be equals to 1
     * @throws DotDataException
     */
    @Test
    public void testAddStructureReindexEntries() throws DotDataException {
        ReindexThread.pause();

        try {
            Config.setProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", true);
            final DotConnect dotConnect = new DotConnect();
            final ContentType type = new ContentTypeDataGen().nextPersisted();
            new ContentletDataGen(type.id()).nextPersisted();

            dotConnect.setSQL("delete from dist_reindex_journal").loadResult();

            factory.addStructureReindexEntries(type.inode());

            assertEquals(1, factory.findContentToReindex(1).size());
        } finally {
            Config.setProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", false);
            ReindexThread.unpause();
        }
    }

    /**
     * Method to test: {@link ReindexQueueFactory#addAllToReindexQueue()}
     * Test Case: Every piece of content is added to the queue to be indexed
     * Expected Results: The reindex queue must be greater than 0
     * @throws DotDataException
     */
    @Test
    public void testAddAllToReindexQueue() throws DotDataException {
        ReindexThread.pause();

        try {
            Config.setProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", true);
            final DotConnect dotConnect = new DotConnect();
            final ContentType type = new ContentTypeDataGen().nextPersisted();
            new ContentletDataGen(type.id()).nextPersisted();

            dotConnect.setSQL("delete from dist_reindex_journal").loadResult();

            assertEquals(0, factory.recordsInQueue());

            factory.addAllToReindexQueue();

            assertTrue(factory.recordsInQueue() > 0);
        } finally {
            Config.setProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", false);
            ReindexThread.unpause();
        }
    }

}
