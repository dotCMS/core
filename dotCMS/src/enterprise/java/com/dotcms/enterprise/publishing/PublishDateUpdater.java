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

import static com.dotcms.content.elasticsearch.business.ESMappingAPIImpl.publishExpireESDateTimeFormat;
import com.dotcms.content.elasticsearch.constants.ESMappingConstants;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import graphql.VisibleForTesting;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.quartz.CronExpression;


public class PublishDateUpdater {

    private static final String RESPECT_WORKFLOW_RESOLUTION_PROPERTY =
            "PUBLISH_JOB_QUEUE_RESPECT_WORKFLOW_RESOLUTION";
    private static final String WORKFLOW_RESOLUTION_SCHEMES_PROPERTY =
            "PUBLISH_JOB_QUEUE_RESPECT_WORKFLOW_RESOLUTION_SCHEMES";

    private final static String PUBLISH_LUCENE_QUERY = "+contentType:(%4$s) +live:false +deleted:false +%1$s:[1969-12-31t18:00:00-0000 TO %3$s] +%2$s:[%3$s TO 3000-12-31t18:00:00-0000]";
    private final static String UNPUBLISH_LUCENE_QUERY = "+live:true +deleted:false +%s:[1969-12-31t18:00:00-0000 TO %s]";

    // Impossible wfstep term used when no resolved step exists, so the publish query stays valid
    // and conservative instead of silently becoming an allow-all query
    private static final String MATCH_NO_STEP_TOKEN = "no_resolved_workflow_step";
    private static final Pattern LUCENE_SAFE_ID = Pattern.compile("[a-zA-Z0-9_-]+");

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
            // Read the workflow resolution configuration once per job run so the query filter and
            // every per-contentlet decision in this run share the same stable snapshot
            final WorkflowResolutionConfig workflowResolutionConfig = WorkflowResolutionConfig.fromConfig();
            final String luceneQueryToPublish = getPublishLuceneQuery(fireTime,
                    contentTypeVariableWithPublishField, workflowResolutionConfig,
                    APILocator.getWorkflowAPI());

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
            final boolean forcePublishAllContent = Config.getBooleanProperty("PUBLISH_JOB_QUEUE_FORCE_INCLUDE_PAST_CONTENT",
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
            totalPublishedCount = processPublishContentInBatch(luceneQueryToPublish, forcePublishAllContent, previousJobRunTime,
                    searchBatchSize, transactionBatchSize, workflowResolutionConfig);
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

        Logger.debug(PublishDateUpdater.class,
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

    /**
     * Builds the scheduled publish query adding, when the workflow resolution check is enabled, an
     * index-level pre-filter on the current workflow step. The pre-filter only reduces the
     * candidate set so held content is not re-read on every run;
     * {@link #workflowAllowsScheduledPublish} remains the final authoritative gate.
     *
     * @param date the fire time bounding the publish date range
     * @param contentTypeVariableWithPublishField content types with a publish date field
     * @param workflowResolutionConfig per-run snapshot of the workflow resolution configuration
     * @param workflowAPI workflow API used to resolve the step IDs referenced by the filter
     * @return the publish query, optionally extended with the workflow step pre-filter
     */
    @VisibleForTesting
    static String getPublishLuceneQuery(final Date date,
            final List<String> contentTypeVariableWithPublishField,
            final WorkflowResolutionConfig workflowResolutionConfig,
            final WorkflowAPI workflowAPI) {

        final String baseQuery = getPublishLuceneQuery(date, contentTypeVariableWithPublishField);
        if (!workflowResolutionConfig.respectWorkflowResolution()) {
            return baseQuery;
        }
        return baseQuery + workflowResolutionQueryFilter(workflowResolutionConfig, workflowAPI);
    }

    /**
     * Builds the workflow step clause appended to the publish query. Any failure while reading the
     * workflow definitions falls back to the unfiltered historical query: the filter is only an
     * optimization and {@link #workflowAllowsScheduledPublish} still enforces the policy.
     *
     * @param workflowResolutionConfig per-run snapshot of the workflow resolution configuration
     * @param workflowAPI workflow API used to resolve schemes and steps
     * @return the Lucene clause to append, or an empty string when no filtering is possible
     */
    private static String workflowResolutionQueryFilter(
            final WorkflowResolutionConfig workflowResolutionConfig, final WorkflowAPI workflowAPI) {
        try {
            return workflowResolutionConfig.configuredSchemes().isEmpty()
                    ? globalResolvedStepFilter(workflowAPI)
                    : scopedUnresolvedStepFilter(workflowResolutionConfig.configuredSchemes(),
                            workflowAPI);
        } catch (final Exception e) {
            Logger.warn(PublishDateUpdater.class,
                    "Could not build the workflow resolution publish query filter, falling back to"
                            + " the unfiltered query: " + e.getMessage(), e);
            return StringUtils.EMPTY;
        }
    }

    /**
     * Builds the global-mode pre-filter: only content sitting on a resolved workflow step may be
     * published, so the query is narrowed to the resolved step IDs. Content without a current step
     * is intentionally excluded because the global check holds it as well. When no resolved step
     * exists, a clause matching no content is returned instead of an allow-all query.
     *
     * @param workflowAPI workflow API used to list schemes and steps
     * @return the Lucene clause restricting candidates to resolved steps
     * @throws DotDataException when the workflow definitions cannot be read
     */
    private static String globalResolvedStepFilter(final WorkflowAPI workflowAPI)
            throws DotDataException {
        final List<String> resolvedStepIds = new ArrayList<>();
        for (final WorkflowScheme scheme : workflowAPI.findSchemes(false)) {
            resolvedStepIds.addAll(stepIds(workflowAPI.findSteps(scheme), true));
        }
        if (resolvedStepIds.isEmpty()) {
            return stepFilterClause("+", List.of(MATCH_NO_STEP_TOKEN));
        }
        return stepFilterClause("+", resolvedStepIds);
    }

    /**
     * Builds the scoped-mode pre-filter: only content currently sitting on an unresolved step of a
     * configured scheme is excluded. Content without a step, content on unconfigured workflows and
     * content on resolved configured steps stays a candidate so the Java gate can decide.
     *
     * @param configuredSchemes configured workflow scheme IDs or variable names
     * @param workflowAPI workflow API used to resolve schemes and steps
     * @return the Lucene clause excluding unresolved configured steps, or empty when none exist
     * @throws DotDataException when the workflow definitions cannot be read
     * @throws DotSecurityException when a configured scheme cannot be read
     */
    private static String scopedUnresolvedStepFilter(final Set<String> configuredSchemes,
            final WorkflowAPI workflowAPI) throws DotDataException, DotSecurityException {
        final List<String> unresolvedStepIds = new ArrayList<>();
        for (final String schemeIdOrVariable : configuredSchemes) {
            final Optional<WorkflowScheme> scheme =
                    findConfiguredScheme(workflowAPI, schemeIdOrVariable);
            if (scheme.isPresent()) {
                unresolvedStepIds.addAll(stepIds(workflowAPI.findSteps(scheme.get()), false));
            }
        }
        if (unresolvedStepIds.isEmpty()) {
            return StringUtils.EMPTY;
        }
        return stepFilterClause("-", unresolvedStepIds);
    }

    /**
     * Resolves a configured workflow scheme by ID or variable name. A scheme that no longer exists
     * is skipped so a stale configuration entry does not disable the whole filter.
     *
     * @param workflowAPI workflow API used to resolve the scheme
     * @param schemeIdOrVariable configured scheme ID or variable name
     * @return the resolved scheme, or empty when it does not exist
     * @throws DotDataException when the workflow definitions cannot be read
     * @throws DotSecurityException when the scheme cannot be read
     */
    private static Optional<WorkflowScheme> findConfiguredScheme(final WorkflowAPI workflowAPI,
            final String schemeIdOrVariable) throws DotDataException, DotSecurityException {
        try {
            return Optional.ofNullable(workflowAPI.findScheme(schemeIdOrVariable));
        } catch (final DoesNotExistException e) {
            Logger.debug(PublishDateUpdater.class, String.format(
                    "Configured workflow scheme %s was not found, skipping it in the publish query filter",
                    schemeIdOrVariable));
            return Optional.empty();
        }
    }

    /**
     * Extracts the IDs of the steps whose {@code resolved} flag matches the requested value.
     *
     * @param steps workflow steps of a scheme
     * @param resolved the {@code resolved} value to select
     * @return the IDs of the matching steps
     */
    private static List<String> stepIds(final List<WorkflowStep> steps, final boolean resolved) {
        return steps.stream()
                .filter(step -> step.isResolved() == resolved)
                .map(WorkflowStep::getId)
                .collect(Collectors.toList());
    }

    /**
     * Formats a {@code wfstep} clause following the pattern already used by
     * {@code WorkflowAPIImpl}: space-separated {@code wfstep:} terms inside a single required or
     * prohibited group. If any step ID is not safe for the Lucene syntax, no clause is produced so
     * the query never excludes content it should not; the Java gate still enforces the policy.
     *
     * @param operator {@code "+"} to require the group or {@code "-"} to prohibit it
     * @param stepIds workflow step IDs to include in the clause
     * @return the formatted clause, or an empty string when an unsafe ID is found
     */
    private static String stepFilterClause(final String operator, final List<String> stepIds) {
        final boolean unsafeId = stepIds.stream()
                .anyMatch(id -> id == null || !LUCENE_SAFE_ID.matcher(id).matches());
        if (unsafeId) {
            Logger.warn(PublishDateUpdater.class,
                    "Found a workflow step ID that is not safe for the Lucene syntax, falling back"
                            + " to the unfiltered publish query");
            return StringUtils.EMPTY;
        }
        return String.format(" %s(%s:%s )", operator, ESMappingConstants.WORKFLOW_STEP,
                String.join(" " + ESMappingConstants.WORKFLOW_STEP + ":", stepIds));
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
    interface ContentProcessor {

        /**
         * Process a single contentlet.
         *
         * @param contentlet The contentlet to process
         * @param systemUser The system user for the operation
         * @return true if the contentlet was processed, false if it was skipped
         * @throws Exception if processing fails
         */
        boolean process(Contentlet contentlet, User systemUser) throws Exception;

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
    @VisibleForTesting
    static int processContentInBatch(final String luceneQuery,
                                                   final int searchBatchSize,
                                                   final int transactionBatchSize,
                                                   final ContentProcessor processor) throws DotDataException {

        final User systemUser = APILocator.getUserAPI().getSystemUser();
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        final String operationName = processor.getOperationName();

        try {
            final List<String> allInodes = contentletAPI.searchIndex(luceneQuery, 0, 0, null,
                    systemUser, false).stream().map(ContentletSearch::getInode).collect(Collectors.toList());

            Logger.debug(PublishDateUpdater.class,
                    String.format("Found %d contentlet inodes to %s", allInodes.size(), operationName));

            if (allInodes.isEmpty()) {
                Logger.debug(PublishDateUpdater.class, String.format("No contentlets found to %s", operationName));
                return 0;
            }
            
            Logger.debug(PublishDateUpdater.class,
                    String.format("Processing %d contentlets in batches of %d with transaction commits every %d records for %s",
                            allInodes.size(), searchBatchSize, transactionBatchSize, operationName));

            int totalProcessed = 0;
            int totalVisited = 0;
            int transactionVisited = 0;
            boolean transactionStarted;
            int currentIndex = 0;

            HibernateUtil.startTransaction();
            transactionStarted = true;

            //First we collect all the inodes to be able to collect batches from the index to perform the operation
            //We use inodes to have an initial inventory of everything that will be processed and still be able to use batches
            while (currentIndex < allInodes.size()) {
                // Calculate batch end index
                final int batchEnd = Math.min(currentIndex + searchBatchSize, allInodes.size());
                final List<String> inodes = allInodes.subList(currentIndex, batchEnd);

                Logger.debug(PublishDateUpdater.class,
                        String.format("Processing identifier batch %d-%d of %d total allInodes for %s",
                                currentIndex, batchEnd, allInodes.size(), operationName));

                // Process each identifier in the current batch
                for (final String inode : inodes) {
                    totalVisited++;
                    transactionVisited++;
                    try {
                        // Fetch fresh contentlet by inode
                        final Contentlet contentlet = contentletAPI.find(inode, systemUser, false);

                        if (contentlet == null) {
                            Logger.warn(PublishDateUpdater.class,
                                    String.format("Contentlet with inode %s not found, skipping %s operation", inode, operationName));
                        } else {
                            // Delegate to the processor function
                            final boolean processed = processor.process(contentlet, systemUser);
                            if (processed) {
                                totalProcessed++;
                            }
                        }
                    } catch (Exception e) {
                        Logger.error(PublishDateUpdater.class,
                                String.format("Content failed to %s: %s - %s", operationName, inode, e.getMessage()), e);
                    }

                    // Commit every transactionBatchSize visited records, including failed or missing contentlets
                    if (transactionVisited >= transactionBatchSize) {
                        Logger.debug(PublishDateUpdater.class,
                                String.format("Committing transaction after visiting %d contentlets (processed: %d) for %s",
                                        transactionVisited, totalProcessed, operationName));
                        HibernateUtil.closeAndCommitTransaction();
                        transactionStarted = false;
                        transactionVisited = 0;

                        // Start new transaction if there are more records to visit
                        if (totalVisited < allInodes.size()) {
                            HibernateUtil.startTransaction();
                            transactionStarted = true;
                        }
                    }
                }

                currentIndex = batchEnd;
            }

            // Commit any remaining records
            if (transactionStarted && transactionVisited > 0) {
                Logger.debug(PublishDateUpdater.class,
                        String.format("Committing final transaction after visiting %d remaining contentlets for %s",
                                transactionVisited, operationName));
                HibernateUtil.closeAndCommitTransaction();
            }

            Logger.info(PublishDateUpdater.class,
                    String.format("Successfully processed %d of %d contentlets for %s", totalProcessed, allInodes.size(), operationName));

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
    static class Publisher implements ContentProcessor {
        private final boolean forcePublishing;
        private final Date previousJobRunTime;
        private final ContentletAPI contentletAPI;
        private final WorkflowAPI workflowAPI;
        private final WorkflowResolutionConfig workflowResolutionConfig;

        public Publisher(final boolean forcePublishing, final Date previousJobRunTime,
                final WorkflowResolutionConfig workflowResolutionConfig) {
            this.forcePublishing = forcePublishing;
            this.previousJobRunTime = previousJobRunTime;
            this.contentletAPI = APILocator.getContentletAPI();
            this.workflowAPI = APILocator.getWorkflowAPI();
            this.workflowResolutionConfig = workflowResolutionConfig;
        }

        /**
         * Creates a publisher with explicit dependencies for unit testing. The force-publishing
         * setting ensures tests target the workflow gate instead of the date-window guard.
         *
         * @param contentletAPI content API used to publish content
         * @param workflowAPI workflow API used to inspect the current step
         * @param workflowResolutionConfig explicit workflow resolution configuration snapshot
         */
        @VisibleForTesting
        Publisher(final ContentletAPI contentletAPI, final WorkflowAPI workflowAPI,
                final WorkflowResolutionConfig workflowResolutionConfig) {
            this.forcePublishing = true;
            this.previousJobRunTime = null;
            this.contentletAPI = contentletAPI;
            this.workflowAPI = workflowAPI;
            this.workflowResolutionConfig = workflowResolutionConfig;
        }

        @Override
        public boolean process(final Contentlet contentlet, final User systemUser) throws Exception {
            if (!workflowAllowsScheduledPublish(contentlet, workflowAPI, workflowResolutionConfig)) {
                return false;
            }

            //This Point is where we decide if the content should get published or skipped
            //But We can always force the publish operation though
            if (forcePublishing || shouldPublishContent(contentlet, previousJobRunTime)) {
                contentletAPI.publish(contentlet, systemUser, false);
                return true;
            } else {
                Logger.debug(PublishDateUpdater.class,
                        "Skipping publish for contentlet " + contentlet.getIdentifier() +
                        " - content was already auto-published and manually unpublished");
                return false;
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
    static class Unpublisher implements ContentProcessor {

        private final ContentletAPI contentletAPI;

        public Unpublisher() {
            this.contentletAPI = APILocator.getContentletAPI();
        }

        /**
         * Creates an unpublisher with an explicit content API for unit testing.
         *
         * @param contentletAPI content API used to unpublish content
         */
        @VisibleForTesting
        Unpublisher(final ContentletAPI contentletAPI) {
            this.contentletAPI = contentletAPI;
        }

        @Override
        public boolean process(final Contentlet contentlet, final User systemUser) throws Exception {
            contentletAPI.unpublish(contentlet, systemUser, false);
            return true;
        }

        @Override
        public String getOperationName() {
            return "unpublish";
        }
    }

    /**
     * Determines whether the scheduled local publisher may publish the content. When the feature
     * flag is disabled, workflow state is not queried and historical behavior is preserved. When
     * enabled, publication requires an explicitly resolved current workflow step. An optional list
     * of workflow scheme IDs or variable names limits the check to selected workflows; an empty
     * list applies it globally. Workflow lookup failures are propagated so the batch processor holds
     * the affected content and continues with the remaining queue.
     *
     * @param contentlet content selected by the scheduled publishing job
     * @param workflowAPI workflow API used to inspect the current step
     * @param workflowResolutionConfig per-run snapshot of the workflow resolution configuration
     * @return {@code true} when scheduled publication is allowed
     * @throws DotDataException when the current workflow state cannot be read
     * @throws DotSecurityException when the current workflow scheme cannot be read
     */
    @VisibleForTesting
    static boolean workflowAllowsScheduledPublish(final Contentlet contentlet,
            final WorkflowAPI workflowAPI, final WorkflowResolutionConfig workflowResolutionConfig)
            throws DotDataException, DotSecurityException {
        if (!workflowResolutionConfig.respectWorkflowResolution()) {
            return true;
        }

        final Optional<WorkflowStep> currentStep = workflowAPI.findCurrentStep(contentlet);
        final Set<String> configuredSchemes = workflowResolutionConfig.configuredSchemes();
        if (!configuredSchemes.isEmpty()
                && !matchesConfiguredWorkflow(contentlet, currentStep, configuredSchemes,
                        workflowAPI)) {
            return true;
        }

        final boolean resolved = currentStep.map(WorkflowStep::isResolved).orElse(false);
        if (!resolved) {
            // Held content is a normal state, not an anomaly, so it is logged at debug level
            Logger.debug(PublishDateUpdater.class, String.format(
                    "Skipping scheduled publish for contentlet %s: current workflow step %s is not resolved",
                    contentlet.getIdentifier(),
                    currentStep.map(WorkflowStep::getId).orElse("not found")));
        }
        return resolved;
    }

    /**
     * Immutable per-run snapshot of the workflow resolution configuration. The properties are read
     * once per job execution so the query filter and every per-contentlet decision within the same
     * run use the same stable values, while property changes stay visible to the next run without
     * a restart.
     *
     * @param respectWorkflowResolution whether scheduled publication requires a resolved step
     * @param configuredSchemes workflow scheme IDs or variable names limiting the check; empty
     *        applies the check to every workflow
     */
    record WorkflowResolutionConfig(boolean respectWorkflowResolution,
                                    Set<String> configuredSchemes) {

        WorkflowResolutionConfig {
            configuredSchemes = Set.copyOf(configuredSchemes);
        }

        /**
         * Reads and normalizes the workflow resolution properties, returning a stable snapshot for
         * the current job run. When the flag is disabled the scheme list is not read at all.
         *
         * @return the configuration snapshot for this job execution
         */
        static WorkflowResolutionConfig fromConfig() {
            if (!Config.getBooleanProperty(RESPECT_WORKFLOW_RESOLUTION_PROPERTY, false)) {
                return new WorkflowResolutionConfig(false, Set.of());
            }
            final Set<String> configuredSchemes = Arrays.stream(
                            Config.getStringProperty(WORKFLOW_RESOLUTION_SCHEMES_PROPERTY, "")
                                    .split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet());
            return new WorkflowResolutionConfig(true, configuredSchemes);
        }
    }

    private static boolean matchesConfiguredWorkflow(final Contentlet contentlet,
            final Optional<WorkflowStep> currentStep, final Set<String> configuredSchemes,
            final WorkflowAPI workflowAPI) throws DotDataException, DotSecurityException {
        if (currentStep.isPresent()) {
            final String schemeId = currentStep.get().getSchemeId();
            if (!UtilMethods.isSet(schemeId)) {
                throw new DotDataException("Current workflow step does not reference a workflow scheme");
            }

            if (configuredSchemes.contains(schemeId)) {
                return true;
            }
            return matchesConfiguredScheme(workflowAPI.findScheme(schemeId), configuredSchemes);
        }

        return workflowAPI.findSchemesForContentType(contentlet.getContentType()).stream()
                .anyMatch(scheme -> matchesConfiguredScheme(scheme, configuredSchemes));
    }

    private static boolean matchesConfiguredScheme(final WorkflowScheme scheme,
            final Set<String> configuredSchemes) {
        return scheme != null && (configuredSchemes.contains(scheme.getId())
                || configuredSchemes.contains(scheme.getVariableName()));
    }

    /**
     * Processes publish content using pagination via the generic processContentWithPagination method.
     *
     * @param luceneQuery The lucene query to search for content to publish
     * @param forcePublishAllContent Whether to include all past content
     * @param previousJobRunTime Previous job run time for content validation
     * @param searchBatchSize The batch size for search operations
     * @param transactionBatchSize The batch size for transaction commits
     * @param workflowResolutionConfig Per-run snapshot of the workflow resolution configuration
     * @return The number of contentlets actually published
     */
    private static int processPublishContentInBatch(final String luceneQuery,
                                                           final boolean forcePublishAllContent,
                                                           final Date previousJobRunTime,
                                                           final int searchBatchSize,
                                                           final int transactionBatchSize,
                                                           final WorkflowResolutionConfig workflowResolutionConfig) throws DotDataException {
        //Build and pass a delegate to deal with the Publish Operation
        final ContentProcessor publishProcessor = new Publisher(forcePublishAllContent, previousJobRunTime,
                workflowResolutionConfig);
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
        final ContentProcessor unpublishProcessor = new Unpublisher();
        return processContentInBatch(luceneQuery, searchBatchSize, transactionBatchSize, unpublishProcessor);
    }

}
