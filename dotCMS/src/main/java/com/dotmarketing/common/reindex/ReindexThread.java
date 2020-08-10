package com.dotmarketing.common.reindex;

import com.dotcms.api.system.event.Visibility;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.content.elasticsearch.business.ESReadOnlyMonitor;
import com.dotcms.content.elasticsearch.util.ESReindexationProcessStatus;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.ThreadUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;

/**
 * This thread is in charge of re-indexing the contenlet information placed in the
 * {@code dist_reindex_journal} table. This process is constantly checking the existence of any
 * record in the table and will add its information to the Elastic index.
 * <p>
 * The records added to the table will have a priority level set by the
 * {@link ReindexQueueFactory.Priority} enum. During the process, all
 * the "correct" contents will be processed and re-indexed first. All the "bad" records (contents
 * that could not be re-indexed) will be set a different priority level and be
 * given more opportunities to be re-indexed after all of the correct contents have already been
 * processed.
 * </p>
 * <p>
 * The number of times the bad contents can re-try the re-index process is specified by the
 * {@link ReindexQueueFactory#REINDEX_MAX_FAILURE_ATTEMPTS} property, which can be customized through
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
public class ReindexThread {

    private enum ThreadState {
        STOPPED, PAUSED, RUNNING;
    }

    private final ContentletIndexAPI indexAPI;
    private final ReindexQueueAPI queueApi;
    private final NotificationAPI notificationAPI;
    private final RoleAPI roleAPI;
    private final UserAPI userAPI;

    private static ReindexThread instance ;

    private final int SLEEP = Config.getIntProperty("REINDEX_THREAD_SLEEP", 250);
    private final int SLEEP_ON_ERROR = Config.getIntProperty("REINDEX_THREAD_SLEEP_ON_ERROR", 500);
    private int failedAttemptsCount = 0;
    private long contentletsIndexed = 0;
    // bulk up to this many requests
    public static final int ELASTICSEARCH_BULK_ACTIONS = Config
            .getIntProperty("REINDEX_THREAD_ELASTICSEARCH_BULK_ACTIONS", 250);
    //how many threads will be used per shard
    public static final int ELASTICSEARCH_CONCURRENT_REQUESTS = Config
            .getIntProperty("REINDEX_THREAD_CONCURRENT_REQUESTS", 1);
    //Bulk size in MB. -1 means disabled
    public static final int ELASTICSEARCH_BULK_SIZE = Config
            .getIntProperty("REINDEX_THREAD_ELASTICSEARCH_BULK_SIZE", 10);
    //Time (in seconds) to wait before closing bulk processor in a full reindex
    private static final int BULK_PROCESSOR_AWAIT_TIMEOUT = Config
            .getIntProperty("BULK_PROCESSOR_AWAIT_TIMEOUT", 20);
    private ThreadState STATE = ThreadState.RUNNING;
    private Future<?>  threadRunning;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder().setNameFormat("reindex-thread-%d").build()
    );


    private final static AtomicBoolean rebuildBulkIndexer=new AtomicBoolean(false);

    public static void rebuildBulkIndexer() {
      Logger.warn(ReindexThread.class, "------------------------");
      Logger.warn(ReindexThread.class, "ReindexThread BulkProcessor needs to be Rebuilt");
      Logger.warn(ReindexThread.class, "------------------------");
      ReindexThread.rebuildBulkIndexer.set(true);
    }

    private ReindexThread() {

        this(APILocator.getReindexQueueAPI(), APILocator.getNotificationAPI(), APILocator.getUserAPI(), APILocator.getRoleAPI(),
                APILocator.getContentletIndexAPI());
    }

    @VisibleForTesting
    public ReindexThread(final ReindexQueueAPI queueApi, final NotificationAPI notificationAPI, final UserAPI userAPI,
            final RoleAPI roleAPI, final ContentletIndexAPI indexAPI) {
        this.queueApi = queueApi;
        this.notificationAPI = notificationAPI;
        this.userAPI = userAPI;
        this.roleAPI = roleAPI;
        this.indexAPI = indexAPI;

    }


    private final Runnable ReindexThreadRunnable = () -> {
        Logger.info(this.getClass(), "------------------------");
        Logger.info(this.getClass(), "Reindex Thread is starting, background indexing will begin");
        Logger.info(this.getClass(), "------------------------");

        while (STATE != ThreadState.STOPPED) {
            try {
                runReindexLoop();
            } catch (Exception e) {
                Logger.error(this.getClass(), e.getMessage(), e);
            }
        }
        Logger.warn(this.getClass(), "------------------------");
        Logger.warn(this.getClass(), "Reindex Thread is stopping, background indexing will not take place");
        Logger.warn(this.getClass(), "------------------------");
    };

    @VisibleForTesting
    long totalESPuts() {
        return contentletsIndexed;
    }


    private BulkProcessor closeBulkProcessor(final BulkProcessor bulkProcessor) throws InterruptedException {
      if(bulkProcessor!=null) {
        bulkProcessor.awaitClose(BULK_PROCESSOR_AWAIT_TIMEOUT, TimeUnit.SECONDS);
      }
      rebuildBulkIndexer.set(false);
      return null;
    }



  /**
   * This method is constantly verifying the existence of records in the {@code dist_reindex_journal}
   * table. If a record is found, then it must be added to the Elastic index. If that's not possible,
   * a notification containing the content identifier will be sent to the user via the Notifications
   * API to take care of the problem as soon as possible.
   */
  private void runReindexLoop() {
    BulkProcessor bulkProcessor = null;
    BulkProcessorListener bulkProcessorListener = null;
    while (STATE != ThreadState.STOPPED) {
      try {

        final Map<String, ReindexEntry> workingRecords = queueApi.findContentToReindex();

        if (!workingRecords.isEmpty()) {
          // if this is a reindex record
          if (indexAPI.isInFullReindex()
              || Try.of(()-> workingRecords.values().stream().findFirst().get().getPriority() >= ReindexQueueFactory.Priority.STRUCTURE.dbValue()).getOrElse(false) ) {
              if (bulkProcessor == null || rebuildBulkIndexer.get()) {
                  closeBulkProcessor(bulkProcessor);
                  bulkProcessorListener = new BulkProcessorListener();
                  bulkProcessor = indexAPI.createBulkProcessor(bulkProcessorListener);
              }
              bulkProcessorListener.workingRecords.putAll(workingRecords);
              indexAPI.appendToBulkProcessor(bulkProcessor, workingRecords.values());
              contentletsIndexed += bulkProcessorListener.getContentletsIndexed();
              // otherwise, reindex normally
          } else if (!ESReadOnlyMonitor.getInstance().isIndexOrClusterReadOnly()){
              reindexWithBulkRequest(workingRecords);
          }
        } else {

          bulkProcessor = closeBulkProcessor(bulkProcessor);
          switchOverIfNeeded();

          Thread.sleep(SLEEP);
        }

      } catch (Exception ex) {
        Logger.error(this, "ReindexThread Exception", ex);
        ThreadUtils.sleep(SLEEP_ON_ERROR);
      } finally {
        DbConnectionFactory.closeSilently();
      }
      while (STATE == ThreadState.PAUSED) {
        ThreadUtils.sleep(1000);
        if (System.currentTimeMillis() % 5 == 0) {
          Logger.info(this.getClass(), "Reindex Thread Paused");
        }
      }
    }
  }

    private void reindexWithBulkRequest(Map<String, ReindexEntry> workingRecords)
            throws DotDataException {
        BulkRequest bulk = indexAPI.createBulkRequest();
        bulk = indexAPI.appendBulkRequest(bulk, workingRecords.values());

        contentletsIndexed += bulk.numberOfActions();
        Logger.info(this.getClass(), "-----------");
        Logger.info(this.getClass(), "Total Indexed        : " + contentletsIndexed);
        Logger.info(this.getClass(), "ReindexEntries found : " + workingRecords.size());
        Logger.info(this.getClass(), "BulkRequests created : " + bulk.numberOfActions());
        indexAPI.putToIndex(bulk, new BulkActionListener(workingRecords));
    }

    private boolean switchOverIfNeeded() throws LanguageException, DotDataException, SQLException, InterruptedException {
        if (ESReindexationProcessStatus.inFullReindexation() && queueApi.recordsInQueue() == 0) {
            // The re-indexation process has finished successfully
            indexAPI.reindexSwitchover(false);
            // Generate and send an user notification
            sendNotification("notification.reindexing.success", null, null, false);
            return true;
        }
        return false;
    }

    /**
     * Tells the thread to start processing. Starts the thread
     */
    public static void startThread() {
        getInstance().state(ThreadState.RUNNING);
        if(getInstance().threadRunning ==null || getInstance().threadRunning.isDone()) {
            final Thread thread = new Thread(getInstance().ReindexThreadRunnable, "ReindexThreadRunnable");
            getInstance().threadRunning = getInstance().executor.submit(thread);
        }
    }

    private void state(ThreadState state) {
        getInstance().STATE = state;
    }
    /**
     * Tells the thread to stop processing. Doesn't shut down the thread.
     */
    public static void stopThread() {
        getInstance().state(ThreadState.STOPPED);
        int i=0;
        while(getInstance().threadRunning !=null && ! getInstance().threadRunning.isDone() && ++i<10) {
            getInstance().state(ThreadState.STOPPED);
            ThreadUtils.sleep(500);
        }
        while(getInstance().threadRunning !=null && ! getInstance().threadRunning.isDone()) {
            getInstance().threadRunning.cancel(true);
        }
    }

    /**
     * This instance is intended to already be started. It will try to restart the thread if instance is
     * null.
     */
    public static ReindexThread getInstance() {
        if(instance==null) {
            instance=new ReindexThread();
            startThread();
        }
        return instance;
    }

    public static void pause() {
        getInstance().state(ThreadState.PAUSED);
    }

    public static void unpause() {
        getInstance().state(ThreadState.RUNNING);
    }

    public static boolean isWorking() {
        return getInstance().STATE == ThreadState.RUNNING;
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
}
