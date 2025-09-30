package com.dotmarketing.quartz.job;

import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import io.vavr.Lazy;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

/**
 * This Quartz Job is in charge of deleting old versions of Contentlets in dotCMS, as well as
 * keeping a limited number of Contentlet versions per available language, without including their
 * live or working versions. This is meant to lower the number of hardlinks that are created in
 * Cloud environments, specifically AWS' managed NFS service "EFS" and help during version
 * upgrades.
 * <p>Syncing large assets directories can take many hours, sometimes days, to complete. During a
 * dotCMS major version upgrade, a "content freeze" period is required during the sync process -
 * meaning users cannot modify any dotCMS content for significant timeframes. In many cases, a
 * significant amount of disk space is consumed by old File Asset versions that users will never
 * need. Deleting old Contentlet version and keeping a fixed number of them can help with this
 * situation.</p>
 * <p>Here's the list of configuration properties that can be set for this Quartz Job:</p>
 * <ul>
 *     <li>{@code content.drop-versions.enabled}: Enables/disables this Job.</li>
 *     <li>{@code content.drop-versions.cron}: How often this Job will run. Defaults to
 *     {@link #CRON_EXPRESSION_DEFAULT}</li>
 *     <li>{@code content.drop-versions.older-days}: How old a piece of Contentlet can be before
 *     being selected for deletion. For instance, if set to {@code 365}, Contentlets with a
 *     modification date older than 365 days will be selected for deletion.</li>
 *     <li>{@code content.drop-versions.greater-than}: The number of Contentlet versions that are
 *     kept in dotCMS per language. For instance, if set to {@code 100}, and a given Contentlet in
 *     English has 123 versions, then 23 versions will be deleted.</li>
 *     <li>{@code content.drop-versions.older-days.batch-size}: The size of the batch that
 *     include a limited list of Contentlets with more than X number of versions, set via the
 *     {@link #GREATER_THAN_PROP} property. This helps improve the stability of the Job and keep
 *     potential database problems/slowdowns as low as possible.</li>
 * </ul>
 *
 * @author Jose Castro
 * @since Sep 20th, 2023
 */
public class DropOldContentVersionsJob implements StatefulJob {

    public static final String ENABLED_PROP = "content.drop-versions.enabled";
    public static final String CRON_EXPR_PROP = "content.drop-versions.cron";
    public static final String OLDER_THAN_DAYS_PROP = "content.drop-versions.older-days";
    public static final String GREATER_THAN_PROP = "content.drop-versions.greater-than";
    public static final String PULL_BATCH_PROP = "content.drop-versions.older-days.batch-size";
    public static final String JOB_NAME = "DropOldContentVersionsJob";
    public static final String JOB_GROUP = "DropOldContentVersionsJobGroup";

    /**
     * Fire every Monday at 1:15AM
     */
    private static final String CRON_EXPRESSION_DEFAULT = "0 15 1 ? * WED *;

    public static final Lazy<Boolean> ENABLED =
            Lazy.of(() -> Config.getBooleanProperty(ENABLED_PROP, true));
    public static final Lazy<String> CRON_EXPRESSION =
            Lazy.of(() -> Config.getStringProperty(CRON_EXPR_PROP, CRON_EXPRESSION_DEFAULT));
    private static final Lazy<Integer> PULL_BATCH_SIZE =
            Lazy.of(() -> Config.getIntProperty(PULL_BATCH_PROP, 500));
    private static final Lazy<Integer> GREATER_THAN =
            Lazy.of(() -> Config.getIntProperty(GREATER_THAN_PROP, 100));
    private static final Lazy<Integer> OLDER_THAN =
            Lazy.of(() -> Config.getIntProperty(OLDER_THAN_DAYS_PROP, 365));

    private final DropOldContentVersionsJobHelper helper = new DropOldContentVersionsJobHelper();

    @Override
    public void execute(final JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Logger.info(this.getClass(), "--------------------------------------");
        Logger.info(this.getClass(), "DropOldContentVersionsJob has started");
        Logger.info(this, String.format("-> Deleting Contentlets older than %d days...",
                OLDER_THAN.get()));
        this.deleteOldVersionsFromContentlet();
        final List<Language> languageIds = APILocator.getLanguageAPI().getLanguages();
        Logger.info(this, String.format("Looking for Contentlets with more than %d versions per " +
                "language...", GREATER_THAN.get()));
        for (final Language language : languageIds) {
            Logger.info(this, String.format("Searching for Contentlets in language '%s' [ %s ] ...",
                    language, language.getId()));
            while (true) {
                final List<String> oldContentlets =
                        helper.findContentsWithTotalVersionsGreaterThan(GREATER_THAN.get(),
                                language.getId(), PULL_BATCH_SIZE.get());
                if (oldContentlets.isEmpty()) {
                    Logger.info(this, String.format("No more Contentlets in language '%s' [ %s ] " +
                            "have been found", language, language.getId()));
                    break;
                }
                oldContentlets.forEach(oldContentlet -> this.deleteOldVersionsFromContentlet(oldContentlet, language));
            }
        }
        Logger.info(this.getClass(), "DropOldContentVersionsJob has finished!");
    }

    /**
     * Deletes all Contentlet versions that are older than the number of days specified via the
     * {@link #OLDER_THAN_DAYS_PROP} configuration property. Working and/or live versions of such
     * Contentlets will NOT be deleted.
     */
    private void deleteOldVersionsFromContentlet() {
        try {
            final int deleteContents =
                    APILocator.getContentletAPI().deleteOldContent(this.toDate(OLDER_THAN.get()));
            if (deleteContents > 0) {
                Logger.info(this, String.format("-> %d Contentlets older than %d days were " +
                        "deleted", deleteContents, OLDER_THAN.get()));
            } else {
                Logger.info(this, "-> No Contentlets were found");
            }
        } catch (final DotDataException e) {
            Logger.warnAndDebug(this.getClass(), String.format("An error occurred when deleting " +
                            "Contentlets older than %d days: %s", OLDER_THAN.get(),
                    ExceptionUtil.getErrorMessage(e)), e);
        }
    }

    /**
     * This method keeps <b>NO MORE</b> than X amount of versions of the specified Contentlet in a
     * given language. The number of versions to keep is specified via the
     * {@link #GREATER_THAN_PROP} configuration property. The Contentlet's working and/or live
     * versions will NOT be deleted.
     *
     * @param contentletId The ID of the Contentlet whose additional versions may be deleted.
     * @param language     The {@link Language} of the Contentlet versions that may be deleted.
     */
    private void deleteOldVersionsFromContentlet(final String contentletId,
                                                 final Language language) {
        final List<String> contentVersions = helper.findContentVersionsGreaterThan(contentletId,
                language.getId(), GREATER_THAN.get());
        Logger.info(this, String.format("-> Found %d old versions of Contentlet with ID '%s' in " +
                "language '%s' [ %s ]. Deleting them...", contentVersions.size(), contentletId,
                language, language.getId()));
        contentVersions.forEach(inode -> {

            try {
                final boolean deleted = helper.deleteContentVersion(inode);
                Logger.debug(this, String.format("---> Contentlet Inode '%s' in language '%s' %s " +
                        "deleted", inode, language, deleted ? "was" : "was NOT"));
            } catch (final Exception e) {
                Logger.warnAndDebug(this.getClass(), String.format("An error occurred when " +
                                "deleting Contentlet with Inode '%s' in language %s: %s", inode,
                        language.getId(), ExceptionUtil.getErrorMessage(e)), e);
            }

        });
    }

    /**
     * Returns a Date object that represents the current date minus the number of specified days.
     * This is very useful for generating dates from X number of days ago.
     *
     * @param olderThanDays The number of days that will be subtracted form the current date.
     *
     * @return The current {@link Date} minus the specified days.
     */
    private Date toDate(final int olderThanDays) {
        final LocalDate currentDate = LocalDate.now(ZoneId.of(DateUtil.UTC));
        final LocalDate olderThanDate = currentDate.minusDays(olderThanDays);
        // Convert LocalDate to LocalDateTime by adding midnight time (00:00:00)
        final LocalDateTime localDateTime = olderThanDate.atStartOfDay();
        final Instant instant = localDateTime.atZone(ZoneId.of(DateUtil.UTC)).toInstant();
        return Date.from(instant);
    }

}
