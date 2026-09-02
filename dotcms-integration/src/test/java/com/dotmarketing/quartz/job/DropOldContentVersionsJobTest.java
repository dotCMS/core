package com.dotmarketing.quartz.job;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.liferay.portal.model.User;
import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.dotcms.content.business.json.ContentletJsonAPI.SAVE_CONTENTLET_AS_JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that the {@link DropOldContentVersionsJob} Quartz Job is working as expected.
 *
 * @author Jose Castro
 * @since Oct 2nd, 2023
 */
public class DropOldContentVersionsJobTest {

    private static User SYSTEM_USER = null;
    private static ContentletDataGen contentletDataGen = null;
    private final VersionableAPI versionableAPI = APILocator.getVersionableAPI();
    private final ContentletAPI contentletAPI = APILocator.getContentletAPI();

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting up the web app environment
        IntegrationTestInitService.getInstance().init();
        final ContentType webPageContentContentType =
                APILocator.getContentTypeAPI(APILocator.systemUser()).find("webPageContent");
        contentletDataGen = new ContentletDataGen(webPageContentContentType.id());
        SYSTEM_USER = APILocator.getUserAPI().getSystemUser();
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b>
     *     {@link DropOldContentVersionsJob#execute(JobExecutionContext)}</li>
     *     <li><b>Given Scenario:</b> Create 4 versions of a 2-year-old piece of Content, and
     *     create 105 versions of another one-year-old Content so that the
     *     Drop Old Content Versions Job can analyze them. The default rules are: Dropping
     *     versions OLDER than 365 days, and keeping NO MORE than 100 versions per language</li>
     *     <li><b>Expected Result:</b> For the two-year-old Contentlet, there will only be 1
     *     version as it is the Published version. For the one-year-old Contentlet, 4 of the
     *     105 versions get deleted, leaving 101: the 100 most-recent old versions are kept
     *     by the {@code OFFSET 100} clause, and the working/live inode is correctly excluded
     *     from the deletion candidate set. (Before the NULL-handling SQL fix, the previous
     *     {@code NOT IN ... UNION ALL} query silently failed to exclude the working/live
     *     inode in this scenario, so 5 versions were deleted leaving 100.)</li>
     * </ul>
     */
    @Test
    public void deleteOldContentlets() throws DotDataException, DotSecurityException,
            JobExecutionException {
        final int olderThan =
                Config.getIntProperty(DropOldContentVersionsJob.OLDER_THAN_DAYS_PROP, 365);
        final int defaultBatchSize = Config.getIntProperty("OLD_CONTENT_BATCH_SIZE", 8192);
        Config.setProperty("OLD_CONTENT_BATCH_SIZE", 4);
        try {
            final Date oneYearAgo = localDateToDate(olderThan);
            final Date twoYearsAgo = localDateToDate(olderThan * 2);

            final List<String> oldContentIds = new ArrayList<>();
            for (int i = 0; i < 25; i++) {
                final String contentTitle = "VERY Old Contentlet " + (i + 1);
                Contentlet veryOldContentlet = TestDataUtils.getGenericContentContent(false, 1);
                // Create first version in English from two years ago
                veryOldContentlet = createContentlet(veryOldContentlet, contentTitle, twoYearsAgo);
                // Create three more versions
                veryOldContentlet = createContentletVersions(veryOldContentlet, contentTitle, twoYearsAgo, 2, 4);
                contentletAPI.unlock(veryOldContentlet, SYSTEM_USER, false);
                ContentletDataGen.publish(veryOldContentlet);
                oldContentIds.add(veryOldContentlet.getIdentifier());
            }

            Contentlet contentWithManyVersions = TestDataUtils.getGenericContentContent(false, 1);
            // Create first version in English form one year ago
            contentWithManyVersions = createContentlet(contentWithManyVersions, "Old Contentlet Version", oneYearAgo);
            // Create 100 more versions
            contentWithManyVersions = createContentletVersions(contentWithManyVersions, "Old Contentlet Version", oneYearAgo, 2, 101);
            // Create three more versions with the current date
            contentWithManyVersions = createContentletVersions(contentWithManyVersions, "Old Contentlet Version", new Date(), 102, 105);
            contentletAPI.unlock(contentWithManyVersions, SYSTEM_USER, false);
            ContentletDataGen.publish(contentWithManyVersions);

            final DropOldContentVersionsJob oldContentVersionsJob = new DropOldContentVersionsJob();
            oldContentVersionsJob.execute(null);

            // The two-year-old Contentlet should have all of its versions removed, except for the
            // published one
            for (final String oldContentId : oldContentIds) {
                final List<Versionable> allVersions =
                        versionableAPI.findAllVersions(oldContentId);
                assertEquals("There should only be 1 version of the two-year-old Contentlet!" + allVersions.size(), 1, allVersions.size());
            }

            // The one-year-old Contentlet has 105 versions; the SQL excludes the working/live
            // inode (1 row), then ORDER BY mod_date DESC OFFSET 100 returns 4 deletion
            // candidates → 105 - 4 = 101 versions remain.
            final List<Versionable> contentletVersions =
                    versionableAPI.findAllVersions(contentWithManyVersions.getIdentifier());
            assertEquals("Expected 101 versions to remain after the job (105 created - 4 deleted): "
                    + contentletVersions.size(), 101, contentletVersions.size());
        } finally {
            Config.setProperty("OLD_CONTENT_BATCH_SIZE", defaultBatchSize);
        }
    }

    /**
     * Regression test for the {@code NOT IN} vs {@code NOT EXISTS} bug — when a
     * Contentlet has only a working version (never published),
     * {@code contentlet_version_info.live_inode} is NULL. The original SQL used
     * {@code c.inode NOT IN (... live_inode ...)}, which returns UNKNOWN against any
     * NULL and therefore zero rows — silently skipping cleanup of unpublished assets
     * (the dominant case for the EFS bloat this job is meant to address) and
     * spinning the outer loop indefinitely.
     *
     * <p>This test creates a draft Contentlet with multiple versions, asserts
     * {@code live_inode} really is NULL, then calls the helper directly and
     * verifies: (1) the working inode is never in the deletion candidate list,
     * (2) the remaining old versions ARE returned (the original buggy query would
     * have returned an empty list).</p>
     */
    @Test
    public void findContentVersionsGreaterThan_protectsWorkingInodeWhenLiveInodeIsNull()
            throws DotDataException, DotSecurityException {
        final long languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        final Date now = new Date();

        Contentlet draft = TestDataUtils.getGenericContentContent(false, languageId);
        draft = createContentlet(draft, "Draft Never Published 1", now);
        // Add 5 more working versions — never publish, so live_inode stays NULL.
        draft = createContentletVersions(draft, "Draft Never Published", now, 2, 6);
        contentletAPI.unlock(draft, SYSTEM_USER, false);

        final String workingInode = draft.getInode();
        final String identifier = draft.getIdentifier();

        // Sanity: confirm the contentlet really has NULL live_inode — otherwise the
        // regression would not be exercised.
        final List<Map<String, Object>> cviRows = new DotConnect()
                .setSQL("SELECT working_inode, live_inode FROM contentlet_version_info " +
                        "WHERE identifier = ? AND lang = ?")
                .addParam(identifier).addParam(languageId)
                .loadObjectResults();
        assertEquals("Expected exactly 1 contentlet_version_info row", 1, cviRows.size());
        assertNotNull("working_inode must be set", cviRows.get(0).get("working_inode"));
        assertNull("live_inode must be NULL for a never-published contentlet — test " +
                "precondition not met", cviRows.get(0).get("live_inode"));
        assertEquals(workingInode, cviRows.get(0).get("working_inode"));

        // Pass 0 as the OFFSET so the helper returns every old version it considers
        // a deletion candidate.
        final DropOldContentVersionsJobHelper helper = new DropOldContentVersionsJobHelper();
        final List<String> deletionCandidates =
                helper.findContentVersionsGreaterThan(identifier, languageId, 0);

        assertFalse("Working inode must NEVER appear as a deletion candidate, even " +
                        "when live_inode is NULL",
                deletionCandidates.contains(workingInode));
        // 6 versions persisted, 1 is the working — the other 5 are deletion candidates.
        // The original NOT IN query would have returned 0 here.
        assertEquals("Old versions must be returned for a draft with NULL live_inode " +
                        "(NOT IN bug regression check)",
                5, deletionCandidates.size());
        assertTrue("Returned candidates must all be inodes other than the working one",
                deletionCandidates.stream().noneMatch(workingInode::equals));
    }

    /**
     * Initial creation of a test Contentlet.
     */
    private Contentlet createContentlet(final Contentlet contentlet, final String title,
                                        final Date modDate) {
        contentlet.setProperty("title", title);
        contentlet.setProperty("_use_mod_date", modDate);
        contentlet.setModDate(modDate);
        return contentletDataGen.persist(contentlet);
    }

    /**
     * Creates several versions of the specified Contentlet. If {@code startIndex} equals 1 and
     * {@code endIndex} equals 3, then 3 versions will be created with each index value appended to
     * the title.
     */
    @NotNull
    private Contentlet createContentletVersions(Contentlet contentlet, String title, final Date modDate,
                                                final int startIndex, final int endIndex) throws DotDataException, DotSecurityException {
        if (null == title) {
            title = contentlet.getTitle();
        }
        for (int i = startIndex; i <= endIndex; i++) {
            contentlet = contentletAPI.checkout(contentlet.getInode(), SYSTEM_USER, false);
            contentlet.setProperty("title", title + " " + i);
            contentlet.setProperty("_use_mod_date", modDate);
            contentlet.setModDate(modDate);
            contentlet = contentletDataGen.persist(contentlet);
        }
        return contentlet;
    }

    /**
     * Utility method used to get the current date minus a specific number of days.
     */
    private Date localDateToDate(final int olderThan) {
        final LocalDate currentDate = LocalDate.now(ZoneId.of(DateUtil.UTC));
        final LocalDate localDate = currentDate.minusDays(olderThan);
        // Convert LocalDate to LocalDateTime by adding midnight time (00:00:00)
        final LocalDateTime localDateTime = localDate.atStartOfDay();
        final Instant instant = localDateTime.atZone(ZoneId.of(DateUtil.UTC)).toInstant();
        return Date.from(instant);
    }

}
