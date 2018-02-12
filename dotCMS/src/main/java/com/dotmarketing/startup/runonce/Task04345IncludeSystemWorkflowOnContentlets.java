

package com.dotmarketing.startup.runonce;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.ConversionUtils;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.Params;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.google.common.primitives.Ints;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.dotcms.util.CollectionsUtils.list;

/**
 * This upgrade task associated the system workflow to all contentlet that are not already running a workflow.
 * Depending of the state of last version, will set to the step published, unpublish, archive.
 *
 * @author jsanca
 * @version 5.0
 *
 */
public class Task04345IncludeSystemWorkflowOnContentlets implements StartupTask {


    protected static final String       SYSTEM_USER_ID                       = UserAPI.SYSTEM_USER_ID;
    protected static final int          SELECT_MAX_ROWS                      = Config.getIntProperty("task.4340.selectmaxrows", 25);
    protected static final int          MAX_BATCH_SIZE                       = Config.getIntProperty("task.4340.maxbatchsize", 100);
    protected static final long         DEFAULT_LANGUAGE_ID                  = 1L;
    protected static final String       SYSTEM_WORKFLOW_UNPUBLISH_STEP       = "ee24a4cb-2d15-4c98-b1bd-6327126451f3";
    protected static final String       SYSTEM_WORKFLOW_ARCHIVE_STEP         = "d6b095b6-b65f-4bdb-bbfd-701d663dfee2";
    protected static final String       SYSTEM_WORKFLOW_PUBLISH_STEP         = "dc3c9cd0-8467-404b-bf95-cb7df3fbc293";
    protected static final String       SELECT_IDENTIFIER_SQL                = "select id, asset_name from identifier";
    protected static final String       FIND_WORKFLOW_TASK_BY_IDENTIFIER_SQL = "select assigned_to, title, status, webasset, language_id from workflow_task where webasset = ?";
    protected static final String       SELECT_CONTENTLET_VERSION_INFO_SQL   = "select lang, working_inode, live_inode, deleted from contentlet_version_info where identifier=?";
    protected static final String       INSERT_WORKFLOW_TASK_SQL             = "insert into (id, create_date, mode_date, created_by, assigned_to, status, webasset, language_id) values (?,?,?,?,?,?,?,?)";

    protected final DotSubmitter submitter                       = DotConcurrentFactory.getInstance().getSubmitter(DotConcurrentFactory.DOT_SYSTEM_THREAD_POOL);
    protected final SystemWorkflowUpgradeLoggerError loggerError = new SystemWorkflowUpgradeLoggerError();

    @Override
    public boolean forceRun() {
        return Boolean.TRUE;
    }

    @Override
    @CloseDBIfOpened
    public void executeUpgrade() throws DotDataException {

        int selectStartRow = 0;
        final List<WorkflowTask> workflowTasks = new ArrayList<>();
        final List<Future<WorkflowTaskResult>> futures =
                new ArrayList<>();

        Logger.info(this, "Running the upgrade for the system workflow/contentlets");

        List<Map<String, Object>> contentResults =
                (List<Map<String, Object>>)new DotConnect()
                     .setSQL(SELECT_IDENTIFIER_SQL)
                    .setStartRow(selectStartRow)
                    .setMaxRows (SELECT_MAX_ROWS)
                    .loadResults();

        while (contentResults != null && !contentResults.isEmpty()) {

            Logger.info(this, "Reading contentlets from " + selectStartRow +
                        ", to " + (selectStartRow + SELECT_MAX_ROWS));

            if (Config.getBooleanProperty("task.4340.runparallel", true)) {

                Logger.info(this, "Processing the identifier parallel");
                contentResults.stream().parallel().
                        forEach((Map<String, Object> resultIdentifier) -> this.runTask(futures, resultIdentifier));
            } else {
                Logger.info(this, "Processing the identifier normally");contentResults.stream().
                        forEach((Map<String, Object> resultIdentifier) -> this.runTask(futures, resultIdentifier));
            }

            this.processFutures(futures, workflowTasks);
            this.doBatch       (workflowTasks);

            selectStartRow += SELECT_MAX_ROWS;
            contentResults  =
                    (List<Map<String, Object>>)new DotConnect()
                            .setSQL(SELECT_IDENTIFIER_SQL)
                            .setStartRow(selectStartRow)
                            .setMaxRows (SELECT_MAX_ROWS)
                            .loadResults();
            futures.clear();
        }

        this.doBatch (workflowTasks, true);
    } // executeUpgrade.

    private void processFutures(final List<Future<WorkflowTaskResult>> futures,
                                final List<WorkflowTask> workflowTasks) {

        WorkflowTaskResult workflowTaskResult = null;
        for (Future<WorkflowTaskResult> future : futures) {

            try {

                workflowTaskResult = future.get();
                if (UtilMethods.isSet(workflowTaskResult.getTasks())) {

                    workflowTasks.addAll(workflowTaskResult.getTasks());
                }
            } catch (InterruptedException | ExecutionException e) {

                Logger.error(this, e.getMessage(), e);
            }
        }
    }

    private void doBatch(final List<WorkflowTask> workflowTasks) {

        this.doBatch(workflowTasks, false);
    }

    private void doBatch(final List<WorkflowTask> workflowTasks, final boolean force) {

        if (force || workflowTasks.size() > MAX_BATCH_SIZE) {

            Logger.info(this, "Doing Workflow update batch");
            final List<Params> params   = list();
            for (WorkflowTask workflowTask : workflowTasks) {

                 params.add(new Params(UUIDGenerator.generateUuid(),
                         DbConnectionFactory.now(),
                         DbConnectionFactory.now(),
                         SYSTEM_USER_ID,
                         workflowTask.getAssignedTo(), // ???
                         workflowTask.getStatus(),
                         workflowTask.getWebasset(),
                         workflowTask.getLanguageId()
                         ));
            }

            try {

               final List<Integer> batchResult =
                       Ints.asList(new DotConnect().executeBatch(
                               INSERT_WORKFLOW_TASK_SQL,
                                params));

               final int rowsAffected          = batchResult.stream().reduce(0, Integer::sum);
               if (rowsAffected != workflowTasks.size()) {

                   Logger.warn(this, "Tried to update: " + workflowTasks.size() + " rows, but just updated: " + rowsAffected +
                        " rows. List of objects: " + workflowTasks);
               }

               Logger.info(this, "Batch DONE, rows affected: " + rowsAffected);
            } catch (DotDataException e) {
                Logger.error(this, e.getMessage(), e);
            }

            workflowTasks.clear();
        }
    }

    private void runTask(final List<Future<WorkflowTaskResult>> futures,
                         final Map<String, Object> resultIdentifier) {

        futures.add(submitter.submit(()-> this.runWorkflowTask (resultIdentifier)));
    }

    private WorkflowTaskResult runWorkflowTask(final Map<String, Object> resultIdentifier) {

        final String identifier  = (String)resultIdentifier.get("id");
        final String title       = (String)resultIdentifier.get("asset_name");
        List<WorkflowTask> tasks = Collections.emptyList();

        if (UtilMethods.isSet(identifier)) {

            try {
                final List<Map<String, Object>> workflowTaskByIdentifier =
                        this.findWorkflowTasksByIdentifier(identifier);
                final List<Map<String, Object>> contentletVersions =
                        this.getContentletVersions(identifier);

                tasks = (UtilMethods.isSet(workflowTaskByIdentifier)) ?
                        // add task to the missing languages
                        contentletVersions.stream()
                                .filter(contentletVersion -> this.notContainsLanguage(ConversionUtils.toLong(contentletVersion.get("lang"), DEFAULT_LANGUAGE_ID), workflowTaskByIdentifier))
                                .map(contentletVersion -> this.toWorkflowTask(contentletVersion, identifier, title, workflowTaskByIdentifier.get(0)))
                                .collect(CollectionsUtils.toImmutableList()) :

                        // not workflow task, add a task for all languages.
                        contentletVersions.stream().map(language -> this.toWorkflowTask(language, identifier, title))
                                .collect(CollectionsUtils.toImmutableList());
            } catch (DotDataException e) {

                Logger.error(this, e.getMessage(), e);
                this.logError (identifier);
            }
        } else {

            this.logError (identifier);
        }

        return new WorkflowTaskResult(tasks);
    }

    private List<Map<String, Object>> findWorkflowTasksByIdentifier(final String identifier) throws DotDataException {

        return new DotConnect()
                .setSQL(FIND_WORKFLOW_TASK_BY_IDENTIFIER_SQL)
                .addParam(identifier)
                .loadResults();
    }

    private List<Map<String, Object>> getContentletVersions(final String identifier) throws DotDataException  {

        return new DotConnect()
                .setSQL(SELECT_CONTENTLET_VERSION_INFO_SQL)
                .addParam(identifier)
                .loadResults();
    }

    private boolean notContainsLanguage(final Long language,
                                     final List<Map<String, Object>> workflowTaskByIdentifier) {

        return null == workflowTaskByIdentifier.stream()
                .filter(workflowTask -> language.equals(workflowTask.get("language_id")))
                .findFirst().orElse(null);
    }

    private WorkflowTask toWorkflowTask(final Map<String, Object> contentletVersion, final String identifier, final String title) {

        final long language       = ConversionUtils.toLong(contentletVersion.get("lang"), DEFAULT_LANGUAGE_ID);
        final String liveInode    = (String)contentletVersion.get("live_inode");
        final boolean archive     = ConversionUtils.toBooleanFromDb(contentletVersion.get("deleted"));

        String status = SYSTEM_WORKFLOW_UNPUBLISH_STEP; // WORKING/UNPUBLISH
        if (archive) {
            status    = SYSTEM_WORKFLOW_ARCHIVE_STEP; // ARCHIVE
        } else if (UtilMethods.isSet(liveInode)) {
            status    = SYSTEM_WORKFLOW_PUBLISH_STEP; // PUBLISH
        }

        return toWorkflowTask(language, identifier, title, status);
    }

    private WorkflowTask toWorkflowTask(final Map<String, Object> contentletVersion, final String identifier, final String title,
                                        final Map<String, Object> workflowTask) {

        final long language       = ConversionUtils.toLong(contentletVersion.get("lang"), DEFAULT_LANGUAGE_ID);
        final String status       = (String)workflowTask.get("status");
        return toWorkflowTask(language, identifier, title, status);
    }

    // hace un toWorkflowTask con el lang, id, title, step
    private WorkflowTask toWorkflowTask(final long language, final String identifier, final String title,
                                        final String status) {

        return new WorkflowTask("__SYSTEM_USER_ROLE__", title, status, identifier, language);
    }

    private void logError(String identifier) {

        this.loggerError.logError(
                "Could not process the workflow steps for the contentlet/identifier: " +
                identifier);
    }

    public static class WorkflowTaskResult {

        private final List<WorkflowTask>  tasks;

        public WorkflowTaskResult(List<WorkflowTask> tasks) {
            this.tasks = tasks;
        }

        public List<WorkflowTask> getTasks() {
            return tasks;
        }
    }

    public static class WorkflowTask {

        private final String  assignedTo;
        private final String  title;
        private final String  status;
        private final String  webasset;
        private final long    languageId;

        public WorkflowTask(String assignedTo, String title, String status, String webasset, long languageId) {
            this.assignedTo = assignedTo;
            this.title = title;
            this.status = status;
            this.webasset = webasset;
            this.languageId = languageId;
        }

        public String getAssignedTo() {
            return assignedTo;
        }

        public String getTitle() {
            return title;
        }

        public String getStatus() {
            return status;
        }

        public String getWebasset() {
            return webasset;
        }

        public long getLanguageId() {
            return languageId;
        }
    }
}
