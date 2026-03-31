package com.dotmarketing.quartz.job;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.init.DotInitScheduler;
import com.dotmarketing.quartz.QuartzUtils;
import io.vavr.control.Try;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.SchedulerException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Integration tests for {@link PopulateIdentifierBaseTypeJob}.
 * Verifies that the job can be scheduled, is idempotent on re-scheduling, and can be removed.
 */
public class PopulateIdentifierBaseTypeJobTest extends IntegrationTestBase {

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        DotInitScheduler.start();
        removeJob();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        removeJob();
    }

    /**
     * Method to test: {@link PopulateIdentifierBaseTypeJob#fireJob()}
     * When: Job is not yet scheduled
     * Should: Schedule successfully and be retrievable from the scheduler
     */
    @Test
    public void test_fireJob_schedulesJob() {
        final var jobName = PopulateIdentifierBaseTypeJob.getJobName();
        final var groupName = PopulateIdentifierBaseTypeJob.getJobGroupName();

        try {
            PopulateIdentifierBaseTypeJob.fireJob();
        } catch (final Exception e) {
            fail("fireJob() should not throw: " + e.getMessage());
        }

        final var scheduler = QuartzUtils.getScheduler();
        final var jobDetail = Try.of(() -> scheduler.getJobDetail(jobName, groupName))
                .onFailure(e -> fail("Error retrieving job detail: " + e.getMessage()))
                .getOrNull();

        assertNotNull("Job should be present in scheduler after fireJob()", jobDetail);
    }

    /**
     * Method to test: {@link PopulateIdentifierBaseTypeJob#fireJob()}
     * When: Job is already scheduled
     * Should: Not throw and not create a duplicate
     */
    @Test
    public void test_fireJob_isIdempotent() {
        try {
            PopulateIdentifierBaseTypeJob.fireJob();
            PopulateIdentifierBaseTypeJob.fireJob(); // second call must be a no-op
        } catch (final Exception e) {
            fail("fireJob() called twice should not throw: " + e.getMessage());
        }
    }

    /**
     * Method to test: {@link PopulateIdentifierBaseTypeJob#removeJob()}
     * When: Job is scheduled
     * Should: Remove it from the scheduler
     */
    @Test
    public void test_removeJob_removesFromScheduler() throws SchedulerException {
        PopulateIdentifierBaseTypeJob.fireJob();

        removeJob();

        final var jobName = PopulateIdentifierBaseTypeJob.getJobName();
        final var groupName = PopulateIdentifierBaseTypeJob.getJobGroupName();
        final var scheduler = QuartzUtils.getScheduler();

        final var jobDetail = Try.of(() -> scheduler.getJobDetail(jobName, groupName))
                .getOrNull();
        assertNull("Job should not be present in scheduler after removeJob()", jobDetail);
    }

    /**
     * Method to test: {@link PopulateIdentifierBaseTypeJob#fireJob()}
     * When: All identifier rows already have base_type set (migration complete)
     * Should: Skip scheduling — job should NOT appear in the scheduler
     */
    @Test
    public void test_fireJob_skipsSchedulingWhenNoWorkPending() throws Exception {
        removeJob();

        // Capture only the IDs that are currently NULL so we restore exactly those rows
        final List<String> nullIds = new DotConnect()
                .setSQL("SELECT id FROM identifier WHERE base_type IS NULL AND asset_subtype IS NOT NULL")
                .loadObjectResults()
                .stream()
                .map(row -> (String) row.get("id"))
                .collect(Collectors.toList());

        if (!nullIds.isEmpty()) {
            final String idList = nullIds.stream()
                    .map(id -> "'" + id + "'")
                    .collect(Collectors.joining(","));
            new DotConnect().executeStatement(
                    "UPDATE identifier SET base_type = 1 WHERE id IN (" + idList + ")");
        }

        try {
            assertFalse("hasPendingRows() should return false when all rows are populated",
                    PopulateIdentifierBaseTypeJob.hasPendingRows());

            PopulateIdentifierBaseTypeJob.fireJob();

            final var jobName   = PopulateIdentifierBaseTypeJob.getJobName();
            final var groupName = PopulateIdentifierBaseTypeJob.getJobGroupName();
            final var scheduler = QuartzUtils.getScheduler();

            final var jobDetail = Try.of(() -> scheduler.getJobDetail(jobName, groupName)).getOrNull();
            assertNull("Job should NOT be scheduled when migration is already complete", jobDetail);
        } finally {
            // Restore only the rows we modified
            if (!nullIds.isEmpty()) {
                final String idList = nullIds.stream()
                        .map(id -> "'" + id + "'")
                        .collect(Collectors.joining(","));
                new DotConnect().executeStatement(
                        "UPDATE identifier SET base_type = NULL WHERE id IN (" + idList + ")");
            }
        }
    }

    /**
     * Method to test: {@link PopulateIdentifierBaseTypeJob#hasPendingRows()}
     * When: At least one identifier row has a null base_type and non-null asset_subtype
     * Should: Return true
     */
    @Test
    public void test_hasPendingRows_returnsTrueWhenWorkExists() throws Exception {
        // Force at least one NULL row
        new DotConnect().executeStatement(
                "UPDATE identifier SET base_type = NULL " +
                "WHERE asset_subtype IS NOT NULL AND id = (" +
                "  SELECT id FROM identifier WHERE asset_subtype IS NOT NULL LIMIT 1)");

        assertTrue("hasPendingRows() should return true when NULL rows exist",
                PopulateIdentifierBaseTypeJob.hasPendingRows());
    }

    /**
     * Method to test: {@link PopulateIdentifierBaseTypeJob#hasPendingRows()}
     * When: Identifier rows have null base_type but their asset_subtype has no matching structure
     * Should: Return false — orphaned rows cannot be populated and must not keep the job alive
     */
    @Test
    public void test_hasPendingRows_returnsFalseForOrphanedRows() throws Exception {
        final String orphanSubtype = "zz_nonexistent_type_" + System.currentTimeMillis();

        // Insert an identifier row with a bogus asset_subtype that has no structure entry
        new DotConnect().executeStatement(
                "INSERT INTO identifier (id, parent_path, asset_name, host_inode, asset_type, asset_subtype, syspublish_date, sysexpire_date) " +
                "VALUES ('" + orphanSubtype + "', '/', 'orphan_test', 'SYSTEM_HOST', 'contentlet', '" + orphanSubtype + "', NULL, NULL)");

        try {
            assertFalse("hasPendingRows() should return false when only orphaned rows remain",
                    PopulateIdentifierBaseTypeJob.hasPendingRows());
        } finally {
            new DotConnect().executeStatement(
                    "DELETE FROM identifier WHERE id = '" + orphanSubtype + "'");
        }
    }

    // -----------------------------------------------------------------------

    private static void removeJob() throws SchedulerException {
        PopulateIdentifierBaseTypeJob.removeJob();
    }

}
