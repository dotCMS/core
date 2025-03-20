package com.dotmarketing.common.reindex;

import static com.dotmarketing.common.reindex.ReindexQueueFactory.REINDEX_MAX_FAILURE_ATTEMPTS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.reindex.ReindexQueueFactory.Priority;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

public class ReindexQueueAPITest {

    private static ReindexQueueAPI reindexQueueAPI;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        reindexQueueAPI = APILocator.getReindexQueueAPI();
    }

    /**
     * Method to test: {@link ReindexQueueAPI#getFailedReindexRecords()}
     * Given Scenario: A reindex record is marked as failed, then a call to {@link ReindexQueueAPI#getFailedReindexRecords()} should return this record
     * ExpectedResult: A failed record must be returned
     *
     */
    @Test
    public void test_getFailedReindexRecords_shouldReturnFailedRecords() throws DotDataException {
        final String recordId = UUIDGenerator.shorty();

        //cleans up data first
        reindexQueueAPI.deleteReindexAndFailedRecords();

        try {
            //adds a new entry and verifies it is not marked as failed
            reindexQueueAPI.addReindexHighPriority(recordId);
            List<ReindexEntry> failedRecords = reindexQueueAPI.getFailedReindexRecords();
            assertFalse(UtilMethods.isSet(failedRecords));

            //gets ReindexEntry from recordId
            final ReindexEntry currentEntry = getReindexEntry(recordId);

            //marks the entry as failed and verifies it is returned
            currentEntry.setPriority(REINDEX_MAX_FAILURE_ATTEMPTS);
            reindexQueueAPI.markAsFailed(currentEntry, "Testing failed record");

            failedRecords = reindexQueueAPI.getFailedReindexRecords();
            assertTrue(UtilMethods.isSet(failedRecords));
            assertEquals(1, failedRecords.size());
            assertEquals(recordId, failedRecords.get(0).getIdentToIndex());

        }finally{
            reindexQueueAPI.deleteReindexAndFailedRecords();
        }
    }


    /**
     * Method to test: {@link ReindexQueueAPI#getFailedReindexRecords()}
     * Given Scenario: A reindex record with {@link com.dotmarketing.common.reindex.ReindexQueueFactory.Priority#REINDEX} , then a call to {@link ReindexQueueAPI#getFailedReindexRecords()} should not return this record
     * ExpectedResult: None failed records returned
     *
     */
    @Test
    public void test_getFailedReindexRecords_shouldNotReturnAnyRecord() throws DotDataException {
        final String recordId = UUIDGenerator.shorty();

        //cleans up data first
        reindexQueueAPI.deleteReindexAndFailedRecords();

        try {
            //adds a new entry and verifies it is not marked as failed
            reindexQueueAPI.addReindexHighPriority(recordId);
            List<ReindexEntry> failedRecords = reindexQueueAPI.getFailedReindexRecords();
            assertFalse(UtilMethods.isSet(failedRecords));

            //gets ReindexEntry from recordId
            ReindexEntry currentEntry = getReindexEntry(recordId);

            //forces the priority to be set to Priority.REINDEX
            currentEntry.setPriority(Priority.REINDEX.ordinal() - 1);
            reindexQueueAPI.markAsFailed(currentEntry, "Testing failed record");
            failedRecords = reindexQueueAPI.getFailedReindexRecords();
            assertFalse(UtilMethods.isSet(failedRecords));

            currentEntry = getReindexEntry(recordId);
            assertEquals(Priority.REINDEX.ordinal(), currentEntry.getPriority());

        }finally{
            reindexQueueAPI.deleteReindexAndFailedRecords();
        }

    }

    /**
     * Method to Test: {@link ReindexQueueAPIImpl#markAsFailed(ReindexEntry, String)}
     * When: Try to mark a reindex as fail
     * Should: Should call the {@link ReindexQueueFactory#markAsFailed(ReindexEntry, String)}
     *         and
     *
     * @throws DotDataException
     */
    @Test
    public void markAsFailed() throws DotDataException {

        final ReindexQueueFactory reindexQueueFactory  = mock(ReindexQueueFactory.class);

        final ReindexQueueAPIImpl reindexQueueAPI = new ReindexQueueAPIImpl(reindexQueueFactory);

        final ReindexEntry reindexEntry = mock(ReindexEntry.class);
        final String cause = "Test Cause";

        reindexQueueAPI.markAsFailed(reindexEntry, cause);

        verify(reindexQueueFactory).markAsFailed(reindexEntry, cause);
    }

    /**
     * Gets a {@link ReindexEntry} given its id
     * @param recordId
     * @return
     * @throws DotDataException
     */
    private ReindexEntry getReindexEntry(final String recordId) throws DotDataException {
        final Map<String, ReindexEntry> entries = reindexQueueAPI.findContentToReindex();

        assertTrue(UtilMethods.isSet(entries));

        final ReindexEntry currentEntry = entries.get(recordId);

        assertNotNull(currentEntry);
        return currentEntry;
    }
}
