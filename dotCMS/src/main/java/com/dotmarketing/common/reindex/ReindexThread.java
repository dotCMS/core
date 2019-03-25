package com.dotmarketing.common.reindex;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.elasticsearch.action.bulk.BulkRequestBuilder;

import com.dotcms.api.system.event.Visibility;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.content.elasticsearch.util.ESReindexationProcessStatus;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.model.User;

/**
 * This thread is in charge of re-indexing the contenlet information placed in the
 * {@code dist_reindex_journal} table. This process is constantly checking the existence of any
 * record in the table and will add its information to the Elastic index.
 * <p>
 * The records added to the table will have a priority level set by the
 * {@link ReindexQueueFactory#REINDEX_JOURNAL_PRIORITY_NEWINDEX} constant. During the process, all
 * the "correct" contents will be processed and re-indexed first. All the "bad" records (contents
 * that could not be re-indexed) will be set a different priority level specified by the
 * {@link ReindexQueueFactory#REINDEX_JOURNAL_PRIORITY_FAILED_FIRST_ATTEMPT} constant and will be
 * given more opportunities to be re-indexed after all of the correct contents have already been
 * processed.
 * </p>
 * <p>
 * The number of times the bad contents can re-try the re-index process is specified by the
 * {@link ReindexQueueFactory#RETRY_FAILED_INDEX_TIMES} property, which can be customized through
 * the {@code dotmarketing-config.properties} file. If a content cannot be re-indexed after all the
 * specified attempts, a notification will be sent to the Notification Bar indicating the Identifier
 * of the bad contentlet. This way users can keep track of the failed records and check the logs to
 * get more information about the failure.
 * </p>
 * <p>
 * The reasons why a content cannot be re-indexed can be, for example:
 * <ul>
 * <li>Incorrect data format in the contentlet's data, such as malformed JSON data.</li>
 * <li>Association to orphaned data, such as being associated to an Inode that does not exist in the
 * system.</li>
 * <li>A Content Page, in which one or more of its parent folders do not exist in the system
 * anymore.</li>
 * </ul>
 * </p>
 * 
 * @author root
 * @version 3.3
 * @since Mar 22, 2012
 *
 */
public class ReindexThread extends Thread {

    private static final ContentletIndexAPI indexAPI = APILocator.getContentletIndexAPI();
    private final ReindexQueueAPI jAPI;
    private final NotificationAPI notificationAPI;
    private final RoleAPI roleAPI;
    private final UserAPI userAPI;

    private static ReindexThread instance;

    private boolean work = false;
    private final int SLEEP = Config.getIntProperty("REINDEX_THREAD_SLEEP", 250);
    private final int INIT_DELAY = Config.getIntProperty("REINDEX_THREAD_INIT_DELAY", 7500);
    private final int SLEEP_ON_ERROR = Config.getIntProperty("REINDEX_THREAD_SLEEP_ON_ERROR", 500);
    private int failedAttemptsCount = 0;
    private long contentletsIndexed = 0;
    // bulk up to this many requests
    private static final int ELASTICSEARCH_BULK_SIZE = Config.getIntProperty("REINDEX_THREAD_ELASTICSEARCH_BULK_SIZE", 500);

    private ReindexThread() {

        this(APILocator.getReindexQueueAPI(), APILocator.getNotificationAPI(), APILocator.getUserAPI(), APILocator.getRoleAPI());
    }

    @VisibleForTesting
    public ReindexThread(final ReindexQueueAPI jAPI, final NotificationAPI notificationAPI, final UserAPI userAPI, final RoleAPI roleAPI) {
        super("ReindexThread");
        this.jAPI = jAPI;
        this.notificationAPI = notificationAPI;
        this.userAPI = userAPI;
        this.roleAPI = roleAPI;
    }

    private void finish() {
        work = false;
        die = true;

    }

    /**
     * Counts the failed attempts when indexing and handles error notifications
     */
    private void addIndexingFailedAttempt() {

        failedAttemptsCount += 1;

        if (failedAttemptsCount == 10) {// We just want to create one notification error

            try {
                String languageKey = "notification.reindexing.error";
                String defaultMsg = "An error has occurred during the indexing process, please check your logs and retry later";

                // Generate and send an user notification
                sendNotification(languageKey, null, defaultMsg, true);
            } catch (DotDataException | LanguageException e) {
                Logger.error(this, "Error creating a system notification informing about problems in the indexing process.", e);
            }
        }
    }

    private boolean die = false;

    public void run() {

        try {
            Logger.info(this, "Reindex Thread start delayed for " + INIT_DELAY + " millis.");
            Thread.sleep(INIT_DELAY);
        } catch (InterruptedException e) {

        }

        try {
            // if the db has dangling server records, wipe them out so we can reindex
            jAPI.resetServersRecords();
        } catch (DotDataException e) {
            Logger.error(this.getClass(), e.getMessage(), e);
        }
        while(!die) {
            try {
                runReindexLoop();
            } catch (Exception e) {
                Logger.error(this.getClass(), e.getMessage(), e);
            }
        }

    }
    
    @VisibleForTesting
    long contentsIndexed() {
        return contentletsIndexed;
    }

    /**
     * This method is constantly verifying the existence of records in the {@code dist_reindex_journal}
     * table. If a record is found, then it must be added to the Elastic index. If that's not possible,
     * a notification containing the content identifier will be sent to the user via the Notifications
     * API to take care of the problem as soon as possible.
     */
    public void runReindexLoop() {

        unpause();
        BulkRequestBuilder bulk = indexAPI.createBulkRequest();
        while (isWorking() && !die) {
            final Map<String, ReindexEntry> workingRecords = new HashMap<>();
            int recordCount = 0;
            try {
                while (recordCount < ELASTICSEARCH_BULK_SIZE) {
                    workingRecords.putAll(jAPI.findContentToReindex());
                    if (workingRecords.isEmpty() || recordCount == workingRecords.size()) {
                        break;
                    }
                    recordCount = workingRecords.size();
                }

                if (!workingRecords.isEmpty()) {

                    bulk = indexAPI.appendBulkRequest(bulk, workingRecords.values());

                    contentletsIndexed += bulk.numberOfActions();
                    Logger.info(this.getClass(), "-----------");
                    Logger.info(this.getClass(), "Total Indexed :" + contentletsIndexed);
                    Logger.info(this.getClass(), "ReindexEntries found : " + workingRecords.size());
                    Logger.info(this.getClass(), "BulkRequests created : " + bulk.numberOfActions());
                    Optional<String> duration = indexAPI.reindexTimeElapsed();
                    if (duration.isPresent()) {
                        Logger.info(this, "Full Reindex Elapsed : " + duration.get() + "");
                    }
                    Logger.info(this.getClass(), "-----------");
                    indexAPI.putToIndex(bulk, new BulkActionListener(workingRecords));
                    bulk = indexAPI.createBulkRequest();
                } else {
                    switchOverIfNeeded();
                    Thread.sleep(SLEEP);
                }
            } catch (Exception ex) {
                Logger.error(this, "ReindexThread Exception", ex);
                try {
                    Thread.sleep(SLEEP_ON_ERROR);
                } catch (InterruptedException e) {
                    Logger.warn(this, "ReindexThread Sleep InterruptedException: "+e);
                }
            } finally {
                DbConnectionFactory.closeSilently();
            }
        }
    }

    boolean switchOverIfNeeded() throws LanguageException, DotDataException, SQLException, InterruptedException {
        if (ESReindexationProcessStatus.inFullReindexation() && jAPI.recordsInQueue() == 0) {
            // The re-indexation process has finished successfully
            reindexSwitchover(false);

            // Generate and send an user notification
            sendNotification("notification.reindexing.success", null, null, false);
            return true;
        }
        return false;
    }

    /**
     * Tells the thread to start processing. Starts the thread
     */
    public synchronized static void startThread() {

        Logger.info(ReindexThread.class, "ReindexThread ordered to start processing");

        // Creates and starts a thread
        createThread();

    }

    /**
     * Tells the thread to stop processing. Doesn't shut down the thread.
     */
    public synchronized static void stopThread() {
        if (instance != null && instance.isAlive()) {
            Logger.info(ReindexThread.class, "ReindexThread ordered to stop processing");
            instance.finish();
        } else {
            Logger.error(ReindexThread.class, "No ReindexThread available");
        }
    }

    /**
     * Creates and starts a thread that doesn't process anything yet
     */
    private static void createThread() {
        if (instance == null) {
            instance = new ReindexThread();
            instance.setName("ReindexThread");
            instance.start();
            int i = Config.getIntProperty("REINDEX_SLEEP_DURING_INDEX", 0);

        }
    }

    /**
     * This instance is intended to already be started. It will try to restart the thread if instance is
     * null.
     */
    public static ReindexThread getInstance() {
        if (instance == null) {
            createThread();
        }
        return instance;
    }




    public synchronized void pause() {
        work = false;
    }

    public synchronized void unpause() {
        work = true;
    }

    public boolean isWorking() {
        return work;
    }

    /**
     * Stops the full re-indexation process. This means clearing up the content queue and the reindex
     * journal.
     * 
     * @throws DotDataException
     */
    @WrapInTransaction
    public void stopFullReindexation() throws DotDataException {
        try {
            pause();
            this.jAPI.deleteReindexAndFailedRecords();
            indexAPI.fullReindexAbort();
        } finally {
            unpause();
        }
    }

    /**
     * Stops the current re-indexation process and switches the current index to the new one. The main
     * goal of this method is to allow users to switch to the new index even if one or more contents
     * could not be re-indexed.
     * <p>
     * This is very useful because the new index can be created and used immediately. The user can have
     * the new re-indexed content available and then work on the conflicting contents, which can be
     * either fixed or removed from the database.
     * </p>
     * 
     * @throws SQLException An error occurred when interacting with the database.
     * @throws DotDataException The process to switch to the new failed.
     * @throws InterruptedException The established pauses to switch to the new index failed.
     */
    public void stopFullReindexationAndSwitchover() throws SQLException, DotDataException, InterruptedException {
        try {
            pause();
            this.jAPI.deleteReindexAndFailedRecords();
            reindexSwitchover(true);
        } finally {
            unpause();
        }
    }

    /**
     * Generates a new notification displayed at the top left side of the back-end page in dotCMS. This
     * utility method allows you to send reports to the user regarding the operations performed during
     * the re-index, whether they succeeded or failed.
     * 
     * @param key - The message key that should be present in the language properties files.
     * @param msgParams - The parameters, if any, that will replace potential placeholders in the
     *        message. E.g.: "This is {0} test."
     * @param defaultMsg - If set, the default message in case the key does not exist in the properties
     *        file. Otherwise, the message key will be returned.
     * @param error - true if we want to send an error notification
     * @throws DotDataException The notification could not be posted to the system.
     * @throws LanguageException The language properties could not be retrieved.
     */
    protected void sendNotification(final String key, final Object[] msgParams, final String defaultMsg, boolean error)
            throws DotDataException, LanguageException {

        NotificationLevel notificationLevel = error ? NotificationLevel.ERROR : NotificationLevel.INFO;

        // Search for the CMS Admin role and System User
        final Role cmsAdminRole = this.roleAPI.loadCMSAdminRole();
        final User systemUser = this.userAPI.getSystemUser();

        this.notificationAPI.generateNotification(new I18NMessage("notification.reindex.error.title"), // title = Reindex Notification
                new I18NMessage(key, defaultMsg, msgParams), null, // no actions
                notificationLevel, NotificationType.GENERIC, Visibility.ROLE, cmsAdminRole.getId(), systemUser.getUserId(),
                systemUser.getLocale());
    }

    /**
     * Switches the current index structure to the new re-indexed data. This method also allows users to
     * switch to the new re-indexed data even if there are still remaining contents in the
     * {@code dist_reindex_journal} table.
     * 
     * @param forceSwitch - If {@code true}, the new index will be used, even if there are contents that
     *        could not be processed. Otherwise, set to {@code false} and the index switch will only
     *        happen if ALL contents were re-indexed.
     * @throws SQLException An error occurred when interacting with the database.
     * @throws DotDataException The process to switch to the new failed.
     * @throws InterruptedException The established pauses to switch to the new index failed.
     */
    @CloseDBIfOpened
    private void reindexSwitchover(boolean forceSwitch) throws SQLException, DotDataException, InterruptedException {

        // We double check again. Only one node will enter this critical
        // region, then others will enter just to see that the switchover is
        // done

        if (forceSwitch || jAPI.recordsInQueue() == 0) {
            Logger.info(this, "Running Reindex Switchover");
            // Wait a bit while all records gets flushed to index
            indexAPI.fullReindexSwitchover(forceSwitch);
            failedAttemptsCount = 0;
            // Wait a bit while elasticsearch flushes it state
        }

    }

}
