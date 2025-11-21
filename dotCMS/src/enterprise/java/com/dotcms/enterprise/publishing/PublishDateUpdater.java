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

import com.dotcms.business.WrapInTransaction;
import com.dotcms.content.elasticsearch.constants.ESMappingConstants;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import graphql.VisibleForTesting;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.quartz.CronExpression;


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
                .map(velocityVarBameObject -> velocityVarBameObject.toString())
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

    @WrapInTransaction
    public static void updatePublishExpireDates(final Date fireTime) throws DotDataException, DotSecurityException {
        updatePublishExpireDates(fireTime, null);
    }

    @WrapInTransaction
    public static void updatePublishExpireDates(final Date fireTime, final Date previousFireTime) throws DotDataException, DotSecurityException {

	    if(LicenseUtil.getLevel()< LicenseLevel.PROFESSIONAL.level){
	        return;
	    }

        final User systemUser = APILocator.getUserAPI().getSystemUser();

        final List<String> contentTypeVariableWithPublishField = getContentTypeVariableWithPublishField();

        if (!contentTypeVariableWithPublishField.isEmpty()) {
            final String luceneQueryToPublish = getPublishLuceneQuery(fireTime, contentTypeVariableWithPublishField);

            final List<Contentlet> contentletToPublish = APILocator.getContentletAPI()
                    .search(luceneQueryToPublish, 0, 0,
                            null, systemUser, false);

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

            for (final Contentlet contentlet : contentletToPublish) {
                try {
                    if (shouldPublishContent(contentlet,  previousJobRunTime)) {
                        APILocator.getContentletAPI().publish(contentlet, systemUser, false);
                    } else {
                        Logger.debug(PublishDateUpdater.class,
                                "Skipping publish for contentlet " + contentlet.getIdentifier() +
                                " - content was already auto-published and manually unpublished");
                    }
                } catch (Exception e) {
                    Logger.debug(PublishDateUpdater.class,
                            "content failed to publish: " + e.getMessage());
                }
            }
        }

        final String luceneQueryToUnPublish = getExpireLuceneQuery(fireTime);

        final List<Contentlet> contentletToUnPublish = APILocator.getContentletAPI().search(luceneQueryToUnPublish,
                0, 0, null, systemUser, false);


        for(final Contentlet contentlet : contentletToUnPublish) {
            try {
                APILocator.getContentletAPI().unpublish(contentlet, systemUser, false);
            }
            catch(Exception e){
                Logger.debug(PublishDateUpdater.class, "content failed to unpublish: " +  e.getMessage());
            }
        }
    }

    public static String getPublishLuceneQuery(final Date date,
            final List<String> contentTypeVariableWithPublishField) {

        final String time = publishExpireESDateTimeFormat.get().format(date.toInstant());
        return getLuceneQuery(PUBLISH_LUCENE_QUERY,
                ESMappingConstants.PUBLISH_DATE,
                ESMappingConstants.EXPIRE_DATE,
                time,
                StringUtils.join(contentTypeVariableWithPublishField, " OR "));
    }

    public static String getExpireLuceneQuery(final Date date) {
        return getLuceneQuery(UNPUBLISH_LUCENE_QUERY, ESMappingConstants.EXPIRE_DATE,
                publishExpireESDateTimeFormat.get().format(date.toInstant()));
    }

    private static String getLuceneQuery(final String luceneQueryTemplate, final Object ... parameters) {
        return String.format(luceneQueryTemplate, parameters );
    }



}
