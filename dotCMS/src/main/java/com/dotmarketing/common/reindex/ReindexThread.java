package com.dotmarketing.common.reindex;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;

import com.dotcms.api.system.event.Visibility;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.content.elasticsearch.util.ESReindexationProcessStatus;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.business.journal.DistributedJournalAPI;
import com.dotmarketing.common.business.journal.DistributedJournalFactory;
import com.dotmarketing.common.business.journal.IndexJournal;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.model.User;

/**
 * This thread is in charge of re-indexing the contenlet information placed in the
 * {@code dist_reindex_journal} table. This process is constantly checking the existence of any
 * record in the table and will add its information to the Elastic index.
 * <p>
 * The records added to the table will have a priority level set by the
 * {@link DistributedJournalFactory#REINDEX_JOURNAL_PRIORITY_NEWINDEX} constant. During the process,
 * all the "correct" contents will be processed and re-indexed first. All the "bad" records
 * (contents that could not be re-indexed) will be set a different priority level specified by the
 * {@link DistributedJournalFactory#REINDEX_JOURNAL_PRIORITY_FAILED_FIRST_ATTEMPT} constant and will
 * be given more opportunities to be re-indexed after all of the correct contents have already been
 * processed.
 * </p>
 * <p>
 * The number of times the bad contents can re-try the re-index process is specified by the
 * {@link DistributedJournalFactory#RETRY_FAILED_INDEX_TIMES} property, which can be customized
 * through the {@code dotmarketing-config.properties} file. If a content cannot be re-indexed after
 * all the specified attempts, a notification will be sent to the Notification Bar indicating the
 * Identifier of the bad contentlet. This way users can keep track of the failed records and check
 * the logs to get more information about the failure.
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

    public static final int REINDEX_THREAD_SLEEP_DEFAULT_VALUE = 500;
    public static final int REINDEX_THREAD_INIT_DELAY_DEFAULT_VALUE = 5000;

    private static final ContentletIndexAPI indexAPI = APILocator.getContentletIndexAPI();
    private final DistributedJournalAPI jAPI;
    private final NotificationAPI notificationAPI;
    private final RoleAPI roleAPI;
    private final UserAPI userAPI;

    private static ReindexThread instance;

    private boolean work = false;
    private final int sleep = Config.getIntProperty("reindex.thread.sleep", 100);
    private final int delay = Config.getIntProperty("reindex.thread.delay", 7500);
    private final int delayOnError = Config.getIntProperty("reindex.thread.delayonerror", 500);
    private int failedAttemptsCount = 0;

    // if there are old records in the reindexQueue that have been claimed by a server that is no longer
    // running, tee them back up
    private static final int REQUE_REINDEX_RECORDS_OLDER_THAN_SEC = Config.getIntProperty("REQUE_REINDEX_RECORDS_OLDER_THAN_SEC", 120);

    // bulk up to this many requests
    private static final int ELASTICSEARCH_BULK_SIZE = Config.getIntProperty("ELASTICSEARCH_BULK_SIZE", 500);


    private ReindexThread() {

        this(APILocator.getDistributedJournalAPI(), APILocator.getNotificationAPI(), APILocator.getUserAPI(), APILocator.getRoleAPI());
    }

    @VisibleForTesting
    public ReindexThread(final DistributedJournalAPI jAPI, final NotificationAPI notificationAPI, final UserAPI userAPI,
            final RoleAPI roleAPI) {
        super("ReindexThread");
        this.jAPI = jAPI;
        this.notificationAPI = notificationAPI;
        this.userAPI = userAPI;
        this.roleAPI = roleAPI;
    }

    private void finish() {
        work = false;

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
        } else {

            /*
             * Check every TEN failures if we still have records on the dist_reindex_journal as a fall back in
             * case the user wants to finish the re-index process clearing that table or if we have some endless
             * running thread because the server didn't notice the reindex was cancelled.
             */
            if (failedAttemptsCount % 10 == 0) {

                Connection conn = null;

                try {

                    conn = DbConnectionFactory.getDataSource().getConnection();
                    conn.setAutoCommit(false);
                    long foundRecords = jAPI.recordsInQueue(conn);
                    if (foundRecords == 0) {
                        stopFullReindexation();
                        stopThread();
                    }

                } catch (Exception e) {
                    Logger.error(this, "Error verifying pending records for indexing", e);
                    try {
                        stopFullReindexation();
                        stopThread();
                    } catch (DotDataException e1) {
                        Logger.error(this, "Error forcing the index thread to stop", e);
                    }
                } finally {
                    if (conn != null) {
                        try {
                            conn.close();
                        } catch (SQLException e) {
                            Logger.error(this, "Error closing connection", e);
                        }
                    }
                }
            }
        }
    }

    private boolean die = false;

    public void run() {

        try {
            Logger.info(this, "Reindex Thread start delayed for " + delay + " millis.");
            Thread.sleep(delay);
        } catch (InterruptedException e) {

        }

        try {
            // if the db has dangling server records, wipe them out so we can reindex
            jAPI.resetServersRecords();
        } catch (DotDataException e) {
            Logger.error(this.getClass(), e.getMessage(), e);
        }

        try {
            runReindexLoop();
        } catch (Exception e) {
            Logger.fatal(this.getClass(), e.getMessage(), e);
        }

    }

    /**
     * This method is constantly verifying the existence of records in the {@code dist_reindex_journal}
     * table. If a record is found, then it must be added to the Elastic index. If that's not possible,
     * a notification containing the content identifier will be sent to the user via the Notifications
     * API to take care of the problem as soon as possible.
     */
    public void runReindexLoop() {

        long contentletsIndexed = 0;
        unpause();
        BulkRequestBuilder bulk = indexAPI.createBulkRequest();
        while (isWorking() && !die) {
            final Collection<IndexJournal> workingRecords = new HashSet<>();
            try {

                workingRecords.addAll(jAPI.findContentReindexEntriesToReindex(false));
                if (workingRecords.isEmpty()) {
                    switchOverIfNeeded();
                }
                if (workingRecords.isEmpty() && jAPI.recordsInQueue() > 0) {

                    jAPI.requeStaleReindexRecords(REQUE_REINDEX_RECORDS_OLDER_THAN_SEC);

                    workingRecords.addAll(jAPI.findContentReindexEntriesToReindex(true));
                }
                if (!workingRecords.isEmpty()) {

                    bulk = writeRequestsToBulk(bulk, workingRecords);

                    // Delete records from dist_reindex_journal
                    jAPI.deleteReindexEntry(workingRecords);

                    contentletsIndexed += bulk.numberOfActions();
                    Logger.info(this.getClass(), "-----------");
                    Logger.info(this.getClass(), "total:" + contentletsIndexed);
                    Logger.info(this.getClass(), "workingRecords: " + workingRecords.size());

                }

                if ((workingRecords.isEmpty() && bulk.numberOfActions() > 0) || bulk.numberOfActions() >= ELASTICSEARCH_BULK_SIZE) {
                    Logger.info(this.getClass(), "putting bulk:" + bulk.numberOfActions());
                    indexAPI.putToIndex(bulk, new BulkActionListener(workingRecords));
                    bulk = indexAPI.createBulkRequest();
                } else if (workingRecords.isEmpty()) {
                    Thread.sleep(sleep);
                }
            } catch (Exception ex) {
                Logger.error(this, "ReindexThread Exception", ex);
            } finally {
                DbConnectionFactory.closeSilently();
            }
        }
    }

    private boolean switchOverIfNeeded() throws LanguageException, DotDataException, SQLException, InterruptedException {
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
    public synchronized static void startThread(int sleep, int delay) {

        Logger.info(ReindexThread.class, "ContentIndexationThread ordered to start processing");

        // Creates and starts a thread
        createThread();

    }

    /**
     * Tells the thread to stop processing. Doesn't shut down the thread.
     */
    public synchronized static void stopThread() {
        if (instance != null && instance.isAlive()) {
            Logger.info(ReindexThread.class, "ContentIndexationThread ordered to stop processing");
            instance.finish();
        } else {
            Logger.error(ReindexThread.class, "No ContentIndexationThread available");
        }
    }

    /**
     * Creates and starts a thread that doesn't process anything yet
     */
    public synchronized static void createThread() {
        if (instance == null) {
            instance = new ReindexThread();
            instance.setName("ReindexThread");
            instance.start();
            int i = Config.getIntProperty("REINDEX_SLEEP_DURING_INDEX", 0);

        }
    }

    /**
     * Tells the thread to finish what it's down and stop
     */
    public synchronized static void shutdownThread() {
        if (instance != null && instance.isAlive()) {
            Logger.info(ReindexThread.class, "ReindexThread shutdown initiated");
            instance.die = true;
        } else {
            Logger.warn(ReindexThread.class, "ReindexThread not running (or already shutting down)");
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

    @CloseDBIfOpened
    private List<Map<String, String>> getContentletVersionInfoByIdentifier(final String id) throws DotDataException {

        final String sql = "select working_inode,live_inode from contentlet_version_info where identifier=?";
        final DotConnect dc = new DotConnect();
        dc.setSQL(sql);
        dc.addParam(id);
        return dc.loadResults();
    }

    @CloseDBIfOpened
    private com.dotmarketing.portlets.contentlet.business.Contentlet getContentletByINode(final String inode) throws DotHibernateException {

        return (com.dotmarketing.portlets.contentlet.business.Contentlet) HibernateUtil
                .load(com.dotmarketing.portlets.contentlet.business.Contentlet.class, inode);
    }

    @CloseDBIfOpened
    private Contentlet convertFatContentletToContentlet(final com.dotmarketing.portlets.contentlet.business.Contentlet fattyContentlet)
            throws DotDataException, DotSecurityException {

        return FactoryLocator.getContentletFactory().convertFatContentletToContentlet(fattyContentlet);
    }

    private BulkRequestBuilder writeRequestsToBulk(final BulkRequestBuilder bulk, final Collection<IndexJournal> idxs)
            throws DotDataException, DotSecurityException {

        for (IndexJournal idx : idxs) {
            writeRequestsToBulk(bulk, idx);
        }
        return bulk;
    }

    private BulkRequestBuilder writeRequestsToBulk(BulkRequestBuilder bulk, IndexJournal idx)
            throws DotDataException, DotSecurityException {

        Logger.debug(this, "Indexing document " + idx.getIdentToIndex());

        final List<ContentletVersionInfo> versions = APILocator.getVersionableAPI().findContentletVersionInfos(idx.getIdentToIndex());

        final Set<String> inodes = new HashSet<>();

        for (ContentletVersionInfo cvi : versions) {
            String workingInode = cvi.getWorkingInode();
            String liveInode = cvi.getLiveInode();
            inodes.add(workingInode);
            if (UtilMethods.isSet(liveInode) && !workingInode.equals(liveInode)) {
                inodes.add(liveInode);
            }
        }

        for (String inode : inodes) {
            Logger.debug(this, "indexing: id:" + inode + " priority: " + idx.getPriority());
            final com.dotmarketing.portlets.contentlet.business.Contentlet fattyContentlet = this.getContentletByINode(inode);
            final Contentlet contentlet = this.convertFatContentletToContentlet(fattyContentlet);
            contentlet.setIndexPolicy(IndexPolicy.DEFER);
            if (idx.isDelete() && idx.getIdentToIndex().equals(contentlet.getIdentifier())) {
                // we delete contentlets from the identifier pointed on index journal record
                // its dependencies are reindexed in order to update its relationships fields
                indexAPI.removeContentFromIndex(contentlet);
            } else {
                if (idx.isReindex()) {
                    bulk = indexAPI.addToReindexBulkRequest(bulk, ImmutableList.of(contentlet));
                } else {
                    bulk = indexAPI.addToBulkRequest(bulk, ImmutableList.of(contentlet));
                }
            }
        }
        return bulk;
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
            this.jAPI.cleanDistReindexJournal();
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
            this.jAPI.cleanDistReindexJournal();
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
    @WrapInTransaction
    private void reindexSwitchover(boolean forceSwitch) throws SQLException, DotDataException, InterruptedException {

        // We double check again. Only one node will enter this critical
        // region, then others will enter just to see that the switchover is
        // done

        if (forceSwitch || jAPI.recordsInQueue() == 0) {
            Logger.info(this, "Running Reindex Switchover");
            // Wait a bit while all records gets flushed to index
            indexAPI.fullReindexSwitchover();
            failedAttemptsCount = 0;
            // Wait a bit while elasticsearch flushes it state
        }

    }

    class BulkActionListener implements ActionListener<BulkResponse> {

        final Collection<IndexJournal> recordsToDelete;

        BulkActionListener(final Collection<IndexJournal> recordsToDelete) {
            this.recordsToDelete = recordsToDelete;
        }

        void handleRecords(Collection<IndexJournal> failedRecords) {

            try {
                if (failedRecords != null && !failedRecords.isEmpty()) {
                    /*
                     * Reset to null the server id of the failed records in the reindex journal table in order to make
                     * them available again for the reindex process.
                     */
                    jAPI.resetServerForReindexEntry(failedRecords);
                }
            } catch (DotDataException e) {
                Logger.error(this, "Error adding back failed records to reindex queue", e);
            }
        }

        @Override
        public void onResponse(BulkResponse resp) {

            // Handle failures on the re-index process if any
            List<IndexJournal> failedRecords = failureHandler(resp);

            // Handle the processed records
            handleRecords(failedRecords);
        }

        @Override
        public void onFailure(Exception ex) {

            Logger.error(ReindexThread.class, "Indexing process failed", ex);

            // Handle the processed records
            handleRecords(recordsToDelete);

            // Reset the failed attempts count as the onFailure will finish the indexing process
            failedAttemptsCount = 0;
        }

        /**
         * Checks if we had failures when indexing, on failure we will retry the indexing process of the
         * records that failed, the process WON'T continue with failed records.
         *
         * @param resp
         */
        private List<IndexJournal> failureHandler(final BulkResponse resp) {

            // List of records that failed and will be added to the queue for more attempts
            List<IndexJournal> failedRecords = new ArrayList<>();

            // Verify if we have failures to handle
            if (resp.hasFailures() && isWorking()) {

                Logger.error(this, "Error indexing content [" + resp.buildFailureMessage() + "]");

                // Counts the failed attempts when indexing and handles error notifications
                addIndexingFailedAttempt();

                // Search for the failed items
                for (BulkItemResponse itemResponse : resp.getItems()) {

                    // Check if the indexing process failed for this item
                    if (itemResponse.isFailed()) {

                        // Get the data of the failed record
                        String initialId = itemResponse.getId();
                        // Remove the language from the id in order to get just the inode/identifier
                        int languageIndex = initialId.lastIndexOf("_");
                        String failedId = initialId;
                        if (languageIndex != -1) {
                            failedId = initialId.substring(0, languageIndex);
                        }

                        // Search the failed record into the list of records to delete
                        Iterator<IndexJournal> toDeleteIterator = recordsToDelete.iterator();
                        while (toDeleteIterator.hasNext()) {

                            IndexJournal indexToDelete = toDeleteIterator.next();
                            if (indexToDelete.getIdentToIndex().equals(failedId)) {

                                // Add it to the list of records that failed and needs to be added back to the
                                // reindex queue
                                if (!exist(failedRecords, indexToDelete)) {
                                    failedRecords.add(indexToDelete);
                                }

                                /*
                                 * Remove the record from the list of contents to remove from the index journal table as it indexing
                                 * process failed and we want a re-try with those records.
                                 */
                                toDeleteIterator.remove();
                            }
                        }
                    }
                }

                if (!failedRecords.isEmpty()) {

                    Logger.error(this,
                            "Reindex thread will try to re-index [" + String.valueOf(failedRecords.size()) + "] failed records.");

                    try {
                        Thread.sleep(delayOnError);
                    } catch (InterruptedException e) {
                        Logger.error(this, e.getMessage(), e);
                    }
                }
            }

            return failedRecords;
        }

        /**
         * Checks if a given record already exist on a given list
         *
         * @param toRestore
         * @param toCompare
         * @return
         */
        private boolean exist(List<IndexJournal> toRestore, IndexJournal toCompare) {

            boolean exist = false;
            for (IndexJournal current : toRestore) {

                if (current.getId() == toCompare.getId()) {
                    exist = true;
                    break;
                }
            }

            return exist;
        }

    }

}
