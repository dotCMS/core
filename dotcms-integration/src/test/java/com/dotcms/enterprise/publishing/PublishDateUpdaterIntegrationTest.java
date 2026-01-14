package com.dotcms.enterprise.publishing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Integration test for PublishDateUpdater functionality.
 * Tests the batch processing, commit behavior, and persistence to database.
 */
public class PublishDateUpdaterIntegrationTest {

    private static ContentType contentType;
    private static User systemUser;
    private static ContentletAPI contentletAPI;

    // Test configuration - smaller batches for testing
    private static final int TEST_SEARCH_BATCH_SIZE = 3;
    private static final int TEST_TRANSACTION_BATCH_SIZE = 2;

    // Store original config values for restoration
    private static String originalSearchBatchSize;
    private static String originalTransactionBatchSize;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        systemUser = APILocator.getUserAPI().getSystemUser();
        contentletAPI = APILocator.getContentletAPI();

        // Store original configuration
        originalSearchBatchSize = Config.getStringProperty("PUBLISH_JOB_QUEUE_SEARCH_BATCH_SIZE");
        originalTransactionBatchSize = Config.getStringProperty("PUBLISH_JOB_QUEUE_TRANSACTION_BATCH_SIZE");

        // Set test-specific batch sizes for verification
        Config.setProperty("PUBLISH_JOB_QUEUE_SEARCH_BATCH_SIZE", String.valueOf(TEST_SEARCH_BATCH_SIZE));
        Config.setProperty("PUBLISH_JOB_QUEUE_TRANSACTION_BATCH_SIZE", String.valueOf(TEST_TRANSACTION_BATCH_SIZE));

        // Create content type with publish and expire date fields
        createTestContentType();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        // Restore original configuration
        if (originalSearchBatchSize != null) {
            Config.setProperty("PUBLISH_JOB_QUEUE_SEARCH_BATCH_SIZE", originalSearchBatchSize);
        }
        if (originalTransactionBatchSize != null) {
            Config.setProperty("PUBLISH_JOB_QUEUE_TRANSACTION_BATCH_SIZE", originalTransactionBatchSize);
        }

        // Clean up test content type
        if (contentType != null) {
            try {
                APILocator.getContentTypeAPI(systemUser).delete(contentType);
            } catch (Exception e) {
                Logger.warn(PublishDateUpdaterIntegrationTest.class,
                        "Could not clean up test content type: " + e.getMessage());
            }
        }
    }

    /**
     * Test Method: {@link PublishDateUpdater#updatePublishExpireDates(Date, Date)}
     * When: Multiple contentlets are scheduled to publish and unpublish
     * Should: Process content in batches, commit transactions properly, and persist changes to database
     */
    @Test
    public void test_updatePublishExpireDates_shouldCommitBatchesAndPersistToDatabase()
            throws DotDataException, DotSecurityException {

        final Date now = new Date();
        final Calendar cal = Calendar.getInstance();
        cal.setTime(now);

        // Create publish times: some in the past (should publish), some in future (should not publish)
        cal.add(Calendar.MINUTE, -10);
        final Date pastPublishDate = cal.getTime();

        cal.setTime(now);
        cal.add(Calendar.MINUTE, 10);
        final Date futurePublishDate = cal.getTime();

        // Create expire times: some in the past (should unpublish), some in future (should not unpublish)
        cal.setTime(now);
        cal.add(Calendar.MINUTE, -5);
        final Date pastExpireDate = cal.getTime();

        cal.setTime(now);
        cal.add(Calendar.MINUTE, 15);
        final Date futureExpireDate = cal.getTime();

        // Create test content - enough to test multiple batches
        final Contentlet[] contentToPublish = createTestContent(5, pastPublishDate, futureExpireDate);
        final Contentlet[] contentToUnpublish = createPublishedTestContent(3, pastPublishDate, pastExpireDate);

        // Verify initial state - content to publish should be unpublished
        for (Contentlet content : contentToPublish) {
            assertFalse("Content should initially be unpublished",
                    contentletAPI.isLive(content));
        }

        // Verify initial state - content to unpublish should be published
        for (Contentlet content : contentToUnpublish) {
            assertTrue("Content should initially be published",
                    contentletAPI.isLive(content));
        }

        // Calculate previous fire time (1 minute ago)
        cal.setTime(now);
        cal.add(Calendar.MINUTE, -1);
        final Date previousFireTime = cal.getTime();

        // Execute the method under test
        PublishDateUpdater.updatePublishExpireDates(now, previousFireTime);

        // Verify publish operations were committed to database
        int publishedCount = 0;
        for (Contentlet content : contentToPublish) {
            // Refresh from database to verify persistence
            Contentlet refreshedContent = contentletAPI.findContentletByIdentifier(
                    content.getIdentifier(), true, content.getLanguageId(), systemUser, false);

            if (refreshedContent != null && contentletAPI.isLive(refreshedContent)) {
                publishedCount++;
            }
        }

        assertEquals("All eligible content should have been published and committed",
                contentToPublish.length, publishedCount);

        // Verify unpublish operations were committed to database
        int unpublishedCount = 0;
        for (Contentlet content : contentToUnpublish) {
            // Refresh from database to verify persistence
            try {
                Contentlet refreshedContent = contentletAPI.findContentletByIdentifier(
                        content.getIdentifier(), true, content.getLanguageId(), systemUser, false);

                if (refreshedContent == null || !contentletAPI.isLive(refreshedContent)) {
                    unpublishedCount++;
                }
            } catch (Exception e) {
                // Content was unpublished, so live version might not exist
                unpublishedCount++;
            }
        }

        assertEquals("All eligible content should have been unpublished and committed",
                contentToUnpublish.length, unpublishedCount);

        Logger.info(this.getClass(),
                String.format("Successfully processed %d publish and %d unpublish operations in batches",
                        publishedCount, unpublishedCount));
    }

    /**
     * Test Method: {@link PublishDateUpdater#updatePublishExpireDates(Date, Date)}
     * When: Processing large number of contentlets with small batch sizes
     * Should: Handle multiple transaction commits without data loss
     */
    @Test
    public void test_updatePublishExpireDates_shouldHandleMultipleTransactionCommitsCorrectly()
            throws DotDataException, DotSecurityException {

        final Date now = new Date();
        final Calendar cal = Calendar.getInstance();
        cal.setTime(now);

        // Create content that should all be published (past publish date, future expire)
        cal.add(Calendar.MINUTE, -30);
        final Date pastPublishDate = cal.getTime();

        cal.setTime(now);
        cal.add(Calendar.MINUTE, 60);
        final Date futureExpireDate = cal.getTime();

        // Create more content than our transaction batch size to force multiple commits
        final int contentCount = TEST_TRANSACTION_BATCH_SIZE * 3 + 1; // 7 items with batch size 2
        final Contentlet[] testContent = createTestContent(contentCount, pastPublishDate, futureExpireDate);

        // Verify initial state
        for (Contentlet content : testContent) {
            assertFalse("Content should initially be unpublished",
                    contentletAPI.isLive(content));
        }

        cal.setTime(now);
        cal.add(Calendar.MINUTE, -1);
        final Date previousFireTime = cal.getTime();

        // Execute the method under test
        PublishDateUpdater.updatePublishExpireDates(now, previousFireTime);

        // Verify all content was processed and committed across multiple transactions
        int publishedCount = 0;
        for (Contentlet content : testContent) {
            Contentlet refreshedContent = contentletAPI.findContentletByIdentifier(
                    content.getIdentifier(), true, content.getLanguageId(), systemUser, false);

            if (refreshedContent != null && contentletAPI.isLive(refreshedContent)) {
                publishedCount++;
            }
        }

        assertEquals("All content should be published despite multiple transaction commits",
                contentCount, publishedCount);

        Logger.info(this.getClass(),
                String.format("Successfully processed %d items across multiple transaction batches of size %d",
                        publishedCount, TEST_TRANSACTION_BATCH_SIZE));
    }

    /**
     * Creates a test content type with publish and expire date fields
     */
    private static void createTestContentType() throws DotDataException, DotSecurityException {
        final Field titleField = new FieldDataGen()
                .type(TextField.class)
                .name("title")
                .velocityVarName("title")
                .next();

        final Field publishDateField = new FieldDataGen()
                .type(DateTimeField.class)
                .name("Publish Date")
                .velocityVarName("publishDate")
                .next();

        final Field expireDateField = new FieldDataGen()
                .type(DateTimeField.class)
                .name("Expire Date")
                .velocityVarName("expireDate")
                .next();

        contentType = new ContentTypeDataGen()
                .name("PublishDateUpdaterTest")
                .velocityVarName("publishDateUpdaterTest")
                .publishDateVar("publishDate")
                .expireDateVar("expireDate")
                .field(titleField)
                .field(publishDateField)
                .field(expireDateField)
                .nextPersisted();
    }

    /**
     * Creates test contentlets with specified publish and expire dates
     */
    private Contentlet[] createTestContent(int count, Date publishDate, Date expireDate) throws DotDataException {
        Contentlet[] contentlets = new Contentlet[count];

        for (int i = 0; i < count; i++) {
            contentlets[i] = new ContentletDataGen(contentType.id())
                    .setProperty("title", "Test Content " + i)
                    .setProperty("publishDate", publishDate)
                    .setProperty("expireDate", expireDate)
                    .nextPersisted();
        }

        return contentlets;
    }

    /**
     * Creates test contentlets that are initially published
     */
    private Contentlet[] createPublishedTestContent(int count, Date publishDate, Date expireDate)
            throws DotDataException, DotSecurityException {
        Contentlet[] contentlets = new Contentlet[count];

        for (int i = 0; i < count; i++) {
            Contentlet unpublishedContent = new ContentletDataGen(contentType.id())
                    .setProperty("title", "Published Test Content " + i)
                    .setProperty("publishDate", publishDate)
                    .setProperty("expireDate", expireDate)
                    .nextPersisted();

            // Publish the content
            contentletAPI.publish(unpublishedContent, systemUser, false);

            contentlets[i] = unpublishedContent;
        }

        return contentlets;
    }
}