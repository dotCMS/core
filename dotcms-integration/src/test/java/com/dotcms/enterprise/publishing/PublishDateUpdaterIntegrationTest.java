
package com.dotcms.enterprise.publishing;

import static org.awaitility.Awaitility.await;
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
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.time.Duration;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

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

    // Unique identifiers for this test run
    private static final String TEST_UNIQUE_ID = "PDUpdTest_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 4);

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

        // Clean up test content type and associated content
        if (contentType != null) {
            try {
                // First, try to delete any remaining contentlets of this type
                final ContentletAPI contentletAPI = APILocator.getContentletAPI();
                final String deleteContentletsQuery = "+contentType:" + contentType.variable();

                try {
                    final long contentletCount = contentletAPI.indexCount(deleteContentletsQuery, systemUser, false);
                    if (contentletCount > 0) {
                        Logger.info(PublishDateUpdaterIntegrationTest.class,
                                "Found " + contentletCount + " contentlets to clean up for content type: " + contentType.variable());

                        final java.util.List<Contentlet> contentletsToDelete = contentletAPI.search(
                                deleteContentletsQuery, 100, 0, null, systemUser, false);

                        for (Contentlet contentlet : contentletsToDelete) {
                            try {
                                // Unpublish if published
                                if (contentlet.isLive()) {
                                    contentletAPI.unpublish(contentlet, systemUser, false);
                                }
                                // Archive the contentlet
                                contentletAPI.archive(contentlet, systemUser, false);
                                // Delete the contentlet
                                contentletAPI.delete(contentlet, systemUser, false);
                            } catch (Exception contentletDeleteException) {
                                Logger.warn(PublishDateUpdaterIntegrationTest.class,
                                        "Could not delete contentlet " + contentlet.getIdentifier() + ": " +
                                                contentletDeleteException.getMessage());
                            }
                        }
                    }
                } catch (Exception contentSearchException) {
                    Logger.warn(PublishDateUpdaterIntegrationTest.class,
                            "Could not search for contentlets to clean up: " + contentSearchException.getMessage());
                }

                // Now delete the content type
                APILocator.getContentTypeAPI(systemUser).delete(contentType);
                Logger.info(PublishDateUpdaterIntegrationTest.class,
                        "Successfully cleaned up test content type: " + contentType.variable());

            } catch (Exception e) {
                Logger.warn(PublishDateUpdaterIntegrationTest.class,
                        "Could not clean up test content type " + contentType.variable() + ": " + e.getMessage());
            }
        }
    }

    /**
     * This Test covers the <b>Unpublish</b> Operation
     * Test Method: {@link PublishDateUpdater#updatePublishExpireDates(Date, Date)}
     * When: Multiple contentlets are scheduled to unpublish (expire)
     * Should: Process content in batches, commit transactions properly, and persist unpublish changes to database
     */
    @Test
    public void testUnpublishOperationShouldExpireContentAndCommitToDatabase()
            throws DotDataException, DotSecurityException {

        final Date now = new Date();
        final Calendar cal = Calendar.getInstance();
        cal.setTime(now);

        // Create publish and expire times for unpublish test
        cal.setTime(now);
        final Date currentPublishDate = cal.getTime();

        cal.setTime(now);
        cal.add(Calendar.SECOND, 10);  // Very short expire date for content that will be unpublished soon
        final Date nearFutureExpireDate = cal.getTime();

        // Create published content that will be unpublished
        final Contentlet[] contentToUnpublish = createPublishedTestContent(3, currentPublishDate, nearFutureExpireDate);

        // Verify initial state - content to unpublish should be published
        for (Contentlet content : contentToUnpublish) {
            assertTrue("Content should initially be published",
                    content.isLive());
        }

        // Wait for the content to expire
        await("Waiting for content to expire").atMost(Duration.ofSeconds(29)).until(() ->
                new Date().after(nearFutureExpireDate));

        // Now create fire time that is after the expire date
        final Date futureFireTime = new Date(); // Current time, which should be after nearFutureExpireDate

        cal.setTime(nearFutureExpireDate);
        cal.add(Calendar.MILLISECOND, -100); // Previous fire time should be before the expire date
        final Date unpublishPreviousFireTime = cal.getTime();

        // Execute the method to test unpublishing
        PublishDateUpdater.PublishDateUpdaterResult unpublishResult =
                PublishDateUpdater.updatePublishExpireDates(futureFireTime, unpublishPreviousFireTime);

        // Verify the result object contains expected unpublished count
        assertTrue("Result should show unpublished count", unpublishResult.getUnpublishedCount() >= contentToUnpublish.length );
        assertTrue("Processing time should be positive", unpublishResult.getTotalProcessingTimeMs() > 0);

        // Verify unpublish operations were committed to database
        int unpublishedCount = 0;
        for (Contentlet content : contentToUnpublish) {
            // Refresh from database to verify persistence
            try {
                Contentlet refreshedContent = contentletAPI.findContentletByIdentifier(
                        content.getIdentifier(), true, content.getLanguageId(), systemUser, false);

                if (refreshedContent == null || !refreshedContent.isLive()) {
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
                String.format("Successfully processed %d unpublish operations in batches - Result: %s",
                        unpublishedCount, unpublishResult));
    }

    /**
     * This Test covers the <b>Publish</b> Operation specifically
     * Test Method: {@link PublishDateUpdater#updatePublishExpireDates(Date, Date)}
     * When: Multiple contentlets are scheduled to publish
     * Should: Process content in batches, commit transactions properly, and persist publish changes to database
     */
    @Test
    public void testPublishOperationShouldPublishContentAndCommitToDatabase()
            throws DotDataException, DotSecurityException {

        final Date now = new Date();
        final Calendar cal = Calendar.getInstance();
        cal.setTime(now);

        // Create publish date for content that should be published
        cal.setTime(now);
        final Date publishDate = cal.getTime();

        // Create expire date very far in the future (optional but safe)
        cal.setTime(now);
        cal.add(Calendar.YEAR, 1);  // Expire date 1 year in the future
        final Date farFutureExpireDate = cal.getTime();

        // Create unpublished content that will be published
        final Contentlet[] contentToPublish = createTestContent(5, publishDate,
                farFutureExpireDate);



        // Verify initial state - content to publish should be unpublished
        for (Contentlet content : contentToPublish) {
            assertFalse("Content should initially be unpublished",
                    content.isLive());
        }

        // Create fire time and previous fire time
        // Current time for execution

        cal.setTime(now);
        cal.add(Calendar.MINUTE, -1); // Previous fire time should be before current time
        final Date previousFireTime = cal.getTime();

        //fire time should be slightly earlier than publishDate
        cal.setTime(publishDate);
        final Date fireTime = cal.getTime();

        // Execute the method to test publishing
        PublishDateUpdater.PublishDateUpdaterResult publishResult =
                PublishDateUpdater.updatePublishExpireDates(fireTime, previousFireTime);

        // Verify the result object contains expected published count
        assertTrue("Result should show published count",
                publishResult.getPublishedCount() >= contentToPublish.length );
        assertEquals("Result should show no unpublished count for publish operation", 0,
                publishResult.getUnpublishedCount());
        assertTrue("Processing time should be positive",
                publishResult.getTotalProcessingTimeMs() > 0);

        // Verify publish operations were committed to database
        int publishedCount = 0;
        for (Contentlet content : contentToPublish) {
            // Refresh from database to verify persistence
            Contentlet refreshedContent = contentletAPI.findContentletByIdentifier(
                    content.getIdentifier(), true, content.getLanguageId(), systemUser, false);

            if (refreshedContent != null && refreshedContent.isLive()) {
                publishedCount++;
            }
        }

        assertEquals("All eligible content should have been published and committed",
                contentToPublish.length, publishedCount);

        Logger.info(this.getClass(),
                String.format(
                        "Successfully processed %d publish operations in batches - Result: %s",
                        publishedCount, publishResult));
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
                .defaultValue(null)
                .next();

        final Field expireDateField = new FieldDataGen()
                .type(DateTimeField.class)
                .name("Expire Date")
                .velocityVarName("expireDate")
                .defaultValue(null)
                .next();

        contentType = new ContentTypeDataGen()
                .name(TEST_UNIQUE_ID)
                .velocityVarName("pdUpdTest" + System.currentTimeMillis())
                .publishDateFieldVarName("publishDate")
                .expireDateFieldVarName("expireDate")
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
            final String uniqueTitle = "Test-Content " + i + "_" + UUID.randomUUID().toString().substring(0, 8);
            contentlets[i] = new ContentletDataGen(contentType.id())
                    .setPolicy(IndexPolicy.FORCE)
                    .setProperty("title", uniqueTitle)
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
            final String uniqueTitle = "Published Test Content " + i + "_" + UUID.randomUUID().toString().substring(0, 8);
            Contentlet unpublishedContent = new ContentletDataGen(contentType.id())
                    .setPolicy(IndexPolicy.FORCE)
                    .setProperty("title", uniqueTitle)
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