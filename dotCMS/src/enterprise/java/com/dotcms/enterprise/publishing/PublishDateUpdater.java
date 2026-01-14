/*

* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below
*
* Copyright (c) 2023 dotCMS Inc.
*
* With regard to the dotCMS Software and this code:
*
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
*
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
*
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.

*/

package com.dotcms.enterprise.publishing;

import com.dotcms.content.elasticsearch.constants.ESMappingConstants;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import graphql.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;
import org.quartz.CronExpression;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.dotcms.content.elasticsearch.business.ESMappingAPIImpl.publishExpireESDateTimeFormat;


public class PublishDateUpdater {

    private final static String PUBLISH_LUCENE_QUERY = "+contentType:(%4$s) +live:false +deleted:false +%1$s:[1969-12-31t18:00:00-0000 TO %3$s] +%2$s:[%3$s TO 3000-12-31t18:00:00-0000]";
    private final static String UNPUBLISH_LUCENE_QUERY = "+live:true +deleted:false +%s:[1969-12-31t18:00:00-0000 TO %s]";

    private final static String GET_CONTENT_TYPE_WITH_PUBLISH_FIELD = "SELECT velocity_var_name from structure where publish_date_var is not null AND publish_date_var != ''";

    @VisibleForTesting
    public static List<String> getContentTypeVariableWithPublishField() throws DotDataException {
        return new DotConnect()
                .setSQL(GET_CONTENT_TYPE_WITH_PUBLISH_FIELD)
                .loadObjectResults()
                .stream()
                .map(contentTypeMap -> contentTypeMap.get("velocity_var_name"))
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    /**
     * Calculates the previous job run time based on the cron expression configuration.
     * This is used as a fallback when JobExecutionContext.getPreviousFireTime() is not available.
     * 
     * This method is primarily used in:
     * - First run scenarios (when Quartz returns null for previousFireTime)
     * - Test scenarios (tests don't have JobExecutionContext)
     * - Manual invocations (direct API calls)
     *
     * @param currentFireTime The current job execution time
     * @return The previous fire time, or null if it cannot be calculated
     * 
     * NOTE: For a valid cron expression (e.g., "0 0/1 * * * ?"), this should rarely return null.
     * If it does return null in normal scenarios (not first run), it indicates a calculation bug
     * that should be investigated. However, when null is returned, shouldPublishContent() will
     * default to publishing content, which is a safe fallback behavior.
     */
    @VisibleForTesting
    public static Date getPreviousJobRunTime(final Date currentFireTime) {
        try {
            final String cronExpressionStr = Config.getStringProperty(
                    "PUBLISHER_QUEUE_THREAD_CRON_EXPRESSION", "0 0/1 * * * ?");

            final CronExpression cronExpression = new CronExpression(cronExpressionStr);

            final Date nextAfterCurrent = cronExpression.getNextValidTimeAfter(currentFireTime);

            if (nextAfterCurrent == null) {
                Logger.warn(PublishDateUpdater.class,
                        "Cannot calculate next execution time from cron expression");
                return null;
            }

            // Calculate the interval
            final long intervalMillis = nextAfterCurrent.getTime() - currentFireTime.getTime();

            // Go back slightly more than the interval to find the previous execution
            // Use a minimum of 1 minute to ensure we go back far enough
            final long minSearchBackMillis = 60 * 1000; // 1 minute
            final long searchBackMillis = Math.max((long) (intervalMillis * 1.1), minSearchBackMillis);
            final Date searchTime = new Date(currentFireTime.getTime() - searchBackMillis);

            // Try to find the previous execution time
            // getNextValidTimeAfter returns the NEXT time AFTER the given time, so we need to
            // ensure searchTime is before the previous execution to find it correctly
            Date previousTime = cronExpression.getNextValidTimeAfter(searchTime);

            // If we found a valid previous time, verify it's before current time
            if (previousTime != null && previousTime.before(currentFireTime)) {
                // Ensure the previous time is not too far back (more than 2 intervals)
                // This helps avoid edge cases where the calculation goes too far back
                final long maxIntervalMillis = intervalMillis * 2;
                final long timeDiff = currentFireTime.getTime() - previousTime.getTime();
                if (timeDiff > 0 && timeDiff <= maxIntervalMillis) {
                    return previousTime;
                }
            }

            // Fallback: try going back by exactly one interval, but ensure we're before the previous execution
            // We need to go back far enough that getNextValidTimeAfter will find the previous execution
            final Date fallbackSearchTime = new Date(currentFireTime.getTime() - intervalMillis - 1000); // Subtract 1 extra second
            previousTime = cronExpression.getNextValidTimeAfter(fallbackSearchTime);
            
            if (previousTime != null && previousTime.before(currentFireTime)) {
                final long timeDiff = currentFireTime.getTime() - previousTime.getTime();
                // Verify it's within reasonable bounds (not more than 2 intervals)
                if (timeDiff > 0 && timeDiff <= intervalMillis * 2) {
                    return previousTime;
                }
            }

            Logger.warn(PublishDateUpdater.class,
                    "Could not determine previous job run time for currentFireTime: " + currentFireTime);
            return null;

        } catch (ParseException e) {
            Logger.warn(PublishDateUpdater.class,
                    "Failed to parse cron expression: " + e.getMessage());
            return null;
        }
    }

    /**
     * Determines if content should be published based on whether it should have been
     * processed in the previous job run.
     * This prevents automatic republishing of content that was manually unpublished after its
     * scheduled publish date.
     *
     * Logic:
     * - Calculate when the job last ran based on the cron expression
     * - If the content's publishDate is BEFORE the last job run time, it means the job
     *   should have already processed it
     * - If it's currently unpublished (live:false) but should have been published,
     *   it was likely manually unpublished → DON'T republish
     *
     * @param contentlet The contentlet to check
     * @param previousJobRunTime The previous job run time
     * @return true if the content should be published, false if it should be skipped
     */
    @VisibleForTesting
    public static boolean shouldPublishContent(final Contentlet contentlet, final Date previousJobRunTime) {
        try {
            final ContentType contentType = contentlet.getContentType();
            final String publishDateVar = contentType.publishDateVar();

            final Date contentPublishDate = (Date) contentlet.get(publishDateVar);

            if (!UtilMethods.isSet(previousJobRunTime)) {
                Logger.debug(PublishDateUpdater.class,
                        "Cannot determine previous job run time - will publish contentlet " +
                        contentlet.getIdentifier());
                return true;
            }

            // If the content's publish date is BEFORE or EQUAL to the previous job run time,
            // it means the job should have already processed it in a previous run.
            // If it's unpublished now, it was manually unpublished → DON'T republish
            if (!contentPublishDate.after(previousJobRunTime)) {
                Logger.debug(PublishDateUpdater.class,
                        String.format("Contentlet %s publishDate (%s) is before previous job run (%s) - skipping republish",
                                contentlet.getIdentifier(), contentPublishDate, previousJobRunTime));
                return false;
            }

            Logger.debug(PublishDateUpdater.class,
                    String.format("Contentlet %s publishDate (%s) is after previous job run (%s) - will publish",
                            contentlet.getIdentifier(), contentPublishDate, previousJobRunTime));
            return true;

        } catch (Exception e) {
            Logger.warn(PublishDateUpdater.class,
                    "Error checking if content should be published for " + contentlet.getIdentifier() +
                    " - defaulting to publish: " + e.getMessage(), e);
            return true;
        }
    }

    public static PublishDateUpdaterResult updatePublishExpireDates(final Date fireTime) throws DotDataException, DotSecurityException {
        return updatePublishExpireDates(fireTime, null);
    }

    public static PublishDateUpdaterResult updatePublishExpireDates(final Date fireTime, final Date previousFireTime) throws DotDataException, DotSecurityException {

	    if(LicenseUtil.getLevel()< LicenseLevel.PROFESSIONAL.level){
	        return new PublishDateUpdaterResult(0, 0, 0, fireTime);
	    }

        final long startTime = System.currentTimeMillis();
        int totalPublishedCount = 0;
        int totalUnpublishedCount = 0;

        try {
            final List<String> contentTypeVariableWithPublishField = getContentTypeVariableWithPublishField();

            // Extract configuration properties - All related to PUBLISH_JOB_QUEUE functionality
            final int searchBatchSize = Config.getIntProperty("PUBLISH_JOB_QUEUE_SEARCH_BATCH_SIZE", 500);
            final int transactionBatchSize = Config.getIntProperty("PUBLISH_JOB_QUEUE_TRANSACTION_BATCH_SIZE", 100);

        if (!contentTypeVariableWithPublishField.isEmpty()) {
            final String luceneQueryToPublish = getPublishLuceneQuery(fireTime, contentTypeVariableWithPublishField);

            // Use provided previousFireTime if available (from JobExecutionContext),
            // otherwise calculate it from cron expression
            //
            // FALLBACK SCENARIOS (when previousFireTime is null):
            // 1. First run: JobExecutionContext.getPreviousFireTime() returns null on first execution (legitimate)
            // 2. Tests: Tests don't have JobExecutionContext, so they use the calculation fallback (legitimate)
            // 3. Manual invocation: Direct calls to updatePublishExpireDates() without JobExecutionContext (legitimate)
            // 4. Invalid previousFireTime: When validation fails (shouldn't happen, but handled gracefully)
            //
            // NOTE: The calculation fallback should be reliable. If getPreviousJobRunTime() returns null
            // in normal scenarios (not first run), that indicates a bug. However, when null is returned,
            // shouldPublishContent() defaults to publishing content, which is a safe fallback behavior.

            //The Following flag allows to byPass The shouldPublishContent check and force publication of everything
            final boolean includeAllPastContent = Config.getBooleanProperty("PUBLISH_JOB_QUEUE_INCLUDE_ALL_PAST_CONTENT",
                    false);

            Date previousJobRunTime = previousFireTime;

            // Validate previousFireTime if provided (should be before fireTime)
            if (previousJobRunTime != null && !previousJobRunTime.before(fireTime)) {
                Logger.warn(PublishDateUpdater.class,
                        "Invalid previousFireTime (" + previousJobRunTime +
                        ") is not before fireTime (" + fireTime +
                        "). Falling back to calculation.");
                previousJobRunTime = null;
            }

            // If not provided or invalid, calculate it from cron expression
            if (previousJobRunTime == null) {
                previousJobRunTime = getPreviousJobRunTime(fireTime);
            }

            //This method fires both operations publish + unpublish using batches
            totalPublishedCount = processPublishContentInBatch(luceneQueryToPublish, includeAllPastContent, previousJobRunTime,
                    searchBatchSize, transactionBatchSize);
        }

        final String luceneQueryToUnPublish = getExpireLuceneQuery(fireTime);

            totalUnpublishedCount = processUnpublishContentInBatch(luceneQueryToUnPublish, searchBatchSize, transactionBatchSize);
        } catch (DotDataException e) {
            Logger.error(PublishDateUpdater.class,
                    "Error processing publish/unpublish dates: " + e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            Logger.error(PublishDateUpdater.class,
                    "Unexpected error processing publish/unpublish dates: " + e.getMessage(), e);
            throw new DotDataException("Failed to process publish/unpublish dates", e);
        }

        final long totalProcessingTime = System.currentTimeMillis() - startTime;
        final PublishDateUpdaterResult result = new PublishDateUpdaterResult(
                totalPublishedCount, totalUnpublishedCount, totalProcessingTime, fireTime);

        Logger.info(PublishDateUpdater.class,
                String.format("Publish/unpublish operation completed: %s", result));

        return result;
    }

    public static String getPublishLuceneQuery(final Date date,
            final List<String> contentTypeVariableWithPublishField) {

        final String time = publishExpireESDateTimeFormat.get().format(date);
        return getLuceneQuery(PUBLISH_LUCENE_QUERY,
                ESMappingConstants.PUBLISH_DATE,
                ESMappingConstants.EXPIRE_DATE,
                time,
                StringUtils.join(contentTypeVariableWithPublishField, " OR "));
    }

    public static String getExpireLuceneQuery(final Date date) {
        return getLuceneQuery(UNPUBLISH_LUCENE_QUERY, ESMappingConstants.EXPIRE_DATE, publishExpireESDateTimeFormat.get().format(date));
    }

    private static String getLuceneQuery(final String luceneQueryTemplate, final Object ... parameters) {
        return String.format(luceneQueryTemplate, parameters );
    }

    /**
     * Result object containing summary information about the publish/unpublish operation.
     */
    public static class PublishDateUpdaterResult {
        private final int publishedCount;
        private final int unpublishedCount;
        private final long totalProcessingTimeMs;
        private final Date executionDate;

        public PublishDateUpdaterResult(int publishedCount, int unpublishedCount, long totalProcessingTimeMs, Date executionDate) {
            this.publishedCount = publishedCount;
            this.unpublishedCount = unpublishedCount;
            this.totalProcessingTimeMs = totalProcessingTimeMs;
            this.executionDate = executionDate;
        }

        /**
         * Get the number of contentlets that were published.
         * @return Number of published contentlets
         */
        public int getPublishedCount() {
            return publishedCount;
        }

        /**
         * Get the number of contentlets that were unpublished.
         * @return Number of unpublished contentlets
         */
        public int getUnpublishedCount() {
            return unpublishedCount;
        }

        /**
         * Get the total number of contentlets processed (published + unpublished).
         * @return Total number of processed contentlets
         */
        public int getTotalProcessedCount() {
            return publishedCount + unpublishedCount;
        }

        /**
         * Get the total processing time in milliseconds.
         * @return Processing time in milliseconds
         */
        public long getTotalProcessingTimeMs() {
            return totalProcessingTimeMs;
        }

        /**
         * Get the execution date when the operation was performed.
         * @return Execution date
         */
        public Date getExecutionDate() {
            return new Date(executionDate.getTime()); // Return defensive copy
        }

        @Override
        public String toString() {
            return String.format("PublishDateUpdaterResult{published=%d, unpublished=%d, total=%d, timeMs=%d, date=%s}",
                    publishedCount, unpublishedCount, getTotalProcessedCount(), totalProcessingTimeMs, executionDate);
        }
    }

    /**
     * Functional interface for content processing operations (publish/unpublish).
     * Allows for flexible delegation of specific content operations while maintaining
     * common pagination and transaction management logic.
     */
    private interface ContentProcessor {
        /**
         * Process a single contentlet.
         *
         * @param contentlet The contentlet to process
         * @param contentletAPI The ContentletAPI instance
         * @param systemUser The system user for the operation
         * @return true if the contentlet was processed, false if it was skipped
         * @throws Exception if processing fails
         */
        void process(Contentlet contentlet, ContentletAPI contentletAPI, User systemUser) throws Exception;

        /**
         * Get the name of this operation for logging purposes.
         *
         * @return The operation name (e.g., "publish", "unpublish")
         */
        String getOperationName();
    }

    /**
     * Generic method to process content using pagination to avoid loading large collections into memory.
     * Uses indexCount to determine total records and processes content in search batches.
     * Commits transactions every N records as configured by transactionBatchSize parameter.
     *
     * @param luceneQuery The lucene query to search for content
     * @param searchBatchSize The batch size for search operations
     * @param transactionBatchSize The batch size for transaction commits
     * @param processor The content processor that provides operation logic and name
     * @return The number of contentlets actually processed (not skipped)
     */
    private static int processContentInBatch(final String luceneQuery,
                                                   final int searchBatchSize,
                                                   final int transactionBatchSize,
                                                   final ContentProcessor processor) throws DotDataException {

        final User systemUser = APILocator.getUserAPI().getSystemUser();
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        final String operationName = processor.getOperationName();

        try {
            // Phase 1: Get all identifiers first to avoid pagination issues when content state changes
            Logger.debug(PublishDateUpdater.class,
                    String.format("Phase 1: Collecting all identifiers for %s operation using query: %s", operationName, luceneQuery));

            final List<String> allIdentifiers = new ArrayList<>();
            int collectOffset = 0;

            while (true) {
                // Get contentlets to extract their identifiers
                final List<Contentlet> contentletBatch = contentletAPI.search(
                        luceneQuery, searchBatchSize, collectOffset, null, systemUser, false);

                if (contentletBatch.isEmpty()) {
                    break; // No more records to collect
                }

                // Extract identifiers from this batch
                for (final Contentlet contentlet : contentletBatch) {
                    allIdentifiers.add(contentlet.getIdentifier());
                }

                collectOffset += contentletBatch.size();

                Logger.debug(PublishDateUpdater.class,
                        String.format("Collected %d identifiers so far for %s operation", allIdentifiers.size(), operationName));
            }

            Logger.info(PublishDateUpdater.class,
                    String.format("Phase 1 complete: Found %d contentlet identifiers to %s", allIdentifiers.size(), operationName));

            if (allIdentifiers.isEmpty()) {
                Logger.debug(PublishDateUpdater.class, String.format("No contentlets found to %s", operationName));
                return 0;
            }

            // Phase 2: Process contentlets by identifier in batches
            Logger.debug(PublishDateUpdater.class,
                    String.format("Phase 2: Processing %d contentlets in batches of %d with transaction commits every %d records for %s",
                            allIdentifiers.size(), searchBatchSize, transactionBatchSize, operationName));

            int totalProcessed = 0;
            int transactionProcessed = 0;
            boolean transactionStarted;
            int currentIndex = 0;

            HibernateUtil.startTransaction();
            transactionStarted = true;

            while (currentIndex < allIdentifiers.size()) {
                // Calculate batch end index
                final int batchEnd = Math.min(currentIndex + searchBatchSize, allIdentifiers.size());
                final List<String> batchIdentifiers = allIdentifiers.subList(currentIndex, batchEnd);

                Logger.debug(PublishDateUpdater.class,
                        String.format("Processing identifier batch %d-%d of %d total identifiers for %s",
                                currentIndex, batchEnd, allIdentifiers.size(), operationName));

                // Process each identifier in the current batch
                for (final String identifier : batchIdentifiers) {
                    try {
                        // Fetch fresh contentlet by identifier
                        final Contentlet contentlet = contentletAPI.findContentletByIdentifierAnyLanguage(
                                identifier, false);

                        if (contentlet == null) {
                            Logger.warn(PublishDateUpdater.class,
                                    String.format("Contentlet with identifier %s not found, skipping %s operation", identifier, operationName));
                            continue;
                        }

                        // Delegate to the processor function
                        processor.process(contentlet, contentletAPI, systemUser);
                        totalProcessed++;
                        transactionProcessed++;

                        // Commit every transactionBatchSize processed records
                        if (transactionProcessed >= transactionBatchSize) {
                            Logger.debug(PublishDateUpdater.class,
                                    String.format("Committing transaction after processing %d contentlets (total: %d) for %s",
                                            transactionProcessed, totalProcessed, operationName));
                            HibernateUtil.closeAndCommitTransaction();
                            transactionStarted = false;
                            transactionProcessed = 0;

                            // Start new transaction if there are more records to process
                            if (currentIndex + searchBatchSize < allIdentifiers.size()) {
                                HibernateUtil.startTransaction();
                                transactionStarted = true;
                            }
                        }
                    } catch (Exception e) {
                        Logger.error(PublishDateUpdater.class,
                                String.format("Content failed to %s: %s - %s", operationName, identifier, e.getMessage()), e);
                    }
                }

                currentIndex = batchEnd;
            }

            // Commit any remaining records
            if (transactionStarted && transactionProcessed > 0) {
                Logger.debug(PublishDateUpdater.class,
                        String.format("Committing final transaction with %d remaining contentlets for %s", transactionProcessed, operationName));
                HibernateUtil.closeAndCommitTransaction();
            }

            Logger.info(PublishDateUpdater.class,
                    String.format("Successfully processed %d of %d contentlets for %s", totalProcessed, allIdentifiers.size(), operationName));

            return totalProcessed;

        } catch (DotHibernateException e) {
            Logger.error(PublishDateUpdater.class,
                    String.format("Transaction error while processing %s content with pagination: %s", operationName, e.getMessage()), e);
            throw new DotDataException(String.format("Transaction error during %s processing", operationName), e);
        } catch (Exception e) {
            Logger.error(PublishDateUpdater.class,
                    String.format("Unexpected error while processing %s content with pagination: %s", operationName, e.getMessage()), e);
            throw new DotDataException(String.format("Unexpected error during %s processing", operationName), e);
        }
    }

    /**
     * Content processor implementation for publish operations.
     */
    private static class PublishContentProcessor implements ContentProcessor {
        private final boolean includeAllPastContent;
        private final Date previousJobRunTime;

        public PublishContentProcessor(final boolean includeAllPastContent, final Date previousJobRunTime) {
            this.includeAllPastContent = includeAllPastContent;
            this.previousJobRunTime = previousJobRunTime;
        }

        @Override
        public void process(final Contentlet contentlet, final ContentletAPI contentletAPI, final User systemUser) throws Exception {
            if (includeAllPastContent || shouldPublishContent(contentlet, previousJobRunTime)) {
                contentletAPI.publish(contentlet, systemUser, false);
            } else {
                Logger.debug(PublishDateUpdater.class,
                        "Skipping publish for contentlet " + contentlet.getIdentifier() +
                        " - content was already auto-published and manually unpublished");
            }
        }

        @Override
        public String getOperationName() {
            return "publish";
        }
    }

    /**
     * Content processor implementation for unpublish operations.
     */
    private static class UnpublishContentProcessor implements ContentProcessor {
        @Override
        public void process(final Contentlet contentlet, final ContentletAPI contentletAPI, final User systemUser) throws Exception {
            contentletAPI.unpublish(contentlet, systemUser, false);
        }

        @Override
        public String getOperationName() {
            return "unpublish";
        }
    }

    /**
     * Processes publish content using pagination via the generic processContentWithPagination method.
     *
     * @param luceneQuery The lucene query to search for content to publish
     * @param includeAllPastContent Whether to include all past content
     * @param previousJobRunTime Previous job run time for content validation
     * @param searchBatchSize The batch size for search operations
     * @param transactionBatchSize The batch size for transaction commits
     * @return The number of contentlets actually published
     */
    private static int processPublishContentInBatch(final String luceneQuery,
                                                           final boolean includeAllPastContent,
                                                           final Date previousJobRunTime,
                                                           final int searchBatchSize,
                                                           final int transactionBatchSize) throws DotDataException {
        //Build and pass a delegate to deal with the Publish Operation
        final ContentProcessor publishProcessor = new PublishContentProcessor(includeAllPastContent, previousJobRunTime);
        return processContentInBatch(luceneQuery, searchBatchSize, transactionBatchSize, publishProcessor);
    }

    /**
     * Processes unpublish content using pagination via the generic processContentWithPagination method.
     *
     * @param luceneQuery The lucene query to search for content to unpublish
     * @param searchBatchSize The batch size for search operations
     * @param transactionBatchSize The batch size for transaction commits
     * @return The number of contentlets actually unpublished
     */
    private static int processUnpublishContentInBatch(final String luceneQuery,
                                                            final int searchBatchSize,
                                                            final int transactionBatchSize) throws DotDataException {
        //Pass a delegate to deal with Unpublish Operation
        final ContentProcessor unpublishProcessor = new UnpublishContentProcessor();
        return processContentInBatch(luceneQuery, searchBatchSize, transactionBatchSize, unpublishProcessor);
    }

}
