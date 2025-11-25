

package com.dotmarketing.startup.runonce;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.util.CloseUtils;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.marshal.MarshalFactory;
import com.dotcms.util.marshal.MarshalUtils;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.Params;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.lang3.mutable.MutableInt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.stream.Stream;

/**
 * This upgrade task will add the column language_id to the workflow_task, set the default language id to all of current records
 * and add an index for the language_id.
 *
 * @author jsanca
 * @version 5.0
 *
 */
public class Task04330WorkflowTaskAddLanguageIdColumn extends AbstractJDBCStartupTask {

    protected static final String       SYSTEM_USER_ROLE_KEY                 = UserAPI.SYSTEM_USER_ID;
    protected static final int          SELECT_MAX_ROWS                      = Config.getIntProperty("task.4330.selectmaxrows", 25);
    protected static final int          MAX_BATCH_SIZE                       = Config.getIntProperty("task.4330.maxbatchsize", 100);
    protected static final String       SELECT_CONTENTLET_LANGS_INFO_SQL     = "select lang from contentlet_version_info where identifier=?";
    protected static final String       INSERT_WORKFLOW_TASK_SQL             = "insert into workflow_task(id, title, creation_date, mod_date, created_by, assigned_to, status, webasset, language_id) values (?,?,?,?,?,?,?,?,?)";

    protected static final long         DEFAULT_LANGUAGE_ID                  = Config.getLongProperty("task.4330.defaultlangid", 1L) ;
    protected static final String       SELECT_WORKFLOW_TASK_SQL             = "select id, assigned_to, title, status, webasset from workflow_task where language_id is null order by creation_date";
    protected static final String       SYSTEM_USER_ID_DEFAULT               = Config.getStringProperty("task.4330.systemuserid","2af7cde3-459a-47e1-8041-22a18aa5ed3c");
    protected final DotSubmitter        submitter                            = DotConcurrentFactory.getInstance().getSubmitter(DotConcurrentFactory.DOT_SYSTEM_THREAD_POOL);
    protected final List<Params>        paramsInsert                         = new ArrayList<>();
    protected final List<Params>        paramsUpdate                         = new ArrayList<>();


    private static final Map<DbType, String> selectLanguageIdColumnSQLMap   = Map.of(
            DbType.POSTGRESQL,   "SELECT language_id FROM workflow_task",
            DbType.MYSQL,        "SELECT language_id FROM workflow_task",
            DbType.ORACLE,       "SELECT language_id FROM workflow_task",
            DbType.MSSQL,        "SELECT language_id FROM workflow_task"
            );

    private static final Map<DbType, String> addLanguageIdColumnSQLMap      = Map.of(
            DbType.POSTGRESQL,   "ALTER TABLE workflow_task ADD language_id INT8",
            DbType.MYSQL,        "ALTER TABLE workflow_task ADD language_id bigint",
            DbType.ORACLE,       "ALTER TABLE workflow_task ADD language_id number(19,0)",
            DbType.MSSQL,        "ALTER TABLE workflow_task ADD language_id NUMERIC(19,0) null"
    );

    private static final Map<DbType, String> updateLanguageIdColumnSQLMap   = Map.of(
            DbType.POSTGRESQL,   "UPDATE workflow_task SET language_id = ? where id = ?",
            DbType.MYSQL,        "UPDATE workflow_task SET language_id = ? where id = ?",
            DbType.ORACLE,       "UPDATE workflow_task SET language_id = ? where id = ?",
            DbType.MSSQL,        "UPDATE workflow_task SET language_id = ? where id = ?"
    );

    private static final Map<DbType, String> addLanguageIdIndexSQLMap       = Map.of(
            DbType.POSTGRESQL,   "create index idx_workflow_6 on workflow_task (language_id)",
            DbType.MYSQL,        "create index idx_workflow_6 on workflow_task (language_id)",
            DbType.ORACLE,       "create index idx_workflow_6 on workflow_task (language_id)",
            DbType.MSSQL,        "create index idx_workflow_6 on workflow_task (language_id)"
    );

    private static final Map<DbType, String> addLanguageIdForeignKeySQLMap       = Map.of(
            DbType.POSTGRESQL,   "alter table workflow_task add constraint FK_workflow_task_language foreign key (language_id) references language(id)",
            DbType.MYSQL,        "ALTER TABLE workflow_task ADD CONSTRAINT FK_workflow_task_language FOREIGN KEY (language_id) REFERENCES language(id)",
            DbType.ORACLE,       "alter table workflow_task add constraint FK_workflow_task_language foreign key (language_id) references language(id)",
            DbType.MSSQL,        "ALTER TABLE workflow_task ADD CONSTRAINT FK_workflow_task_language FOREIGN KEY (language_id) REFERENCES language(id)"
    );

    @Override
    public boolean forceRun() {
        return Boolean.TRUE;
    }

    @Override
    @WrapInTransaction
    public void executeUpgrade() throws DotDataException {

        final boolean created = this.addLanguageIdColumn();

        if (created) {

            if (DbConnectionFactory.isMsSql() && DbConnectionFactory.getAutoCommit()) {
                DbConnectionFactory.setAutoCommit(false); // set a transactional for data
            }

            final StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            try {

                this.updateAllWorkflowTaskToDefaultLanguage();
            } catch (IOException e) {

                Logger.error(this, e.getMessage(), e);
                throw new DotDataException(e);
            }

            stopWatch.stop();
            Logger.info(this, "Migration of the Workflow task DONE, duration:" +
                    DateUtil.millisToSeconds(stopWatch.getTime()) + " seconds");

            // if mssql is in a transaction for the data, commit, close and start a new one.
            if (DbConnectionFactory.isMsSql() && !DbConnectionFactory.getAutoCommit()) {
                this.closeCommitAndStartTransaction();
                DbConnectionFactory.setAutoCommit(true);
            }

            this.addWorkflowTaskLanguageIdIndex();
            this.addWorkflowTaskLanguageIdFK();
        }
    }

    private boolean addLanguageIdColumn() throws DotDataException {

        boolean needToCreate = false;
        Logger.info(this, "Adding new 'language_id' column to 'workflow_task' table.");

        try {

            if (DbConnectionFactory.isMsSql() && !DbConnectionFactory.getAutoCommit()) {
                DbConnectionFactory.setAutoCommit(true);
            }

            new DotConnect().setSQL(this.getSelectLanguageIdColumnSQL()).loadObjectResults();
        } catch (Throwable e) {

            Logger.info(this, "Column 'workflow_task.language_id' does not exists, creating it");
            needToCreate = true;
            // in some databases if an error is throw the transaction is not longer valid
            this.closeAndStartTransaction();
        }

        if (needToCreate) {
            try {

                if (DbConnectionFactory.isMsSql() && !DbConnectionFactory.getAutoCommit()) {
                    DbConnectionFactory.setAutoCommit(true);
                }

                new DotConnect().executeStatement(getAddLanguageIdColumnSQL());
            } catch (SQLException e) {
                throw new DotRuntimeException("The 'language_id' column could not be created.", e);
            } finally {
                this.closeCommitAndStartTransaction();
            }
        }

        return needToCreate;
    }

    private void addWorkflowTaskLanguageIdIndex() {

        Logger.info(this, "Adding new 'language_id' column to index to 'workflow_task' table.");

        try {

            if (DbConnectionFactory.isMsSql() && !DbConnectionFactory.getAutoCommit()) {
                DbConnectionFactory.setAutoCommit(true);
            }

            new DotConnect().executeStatement(getAddLanguageIdIndex());
        } catch (SQLException e) {
            throw new DotRuntimeException("The could not add the index to the column 'language_id' on 'workflow_task' table.", e);
        }
    }

    private void addWorkflowTaskLanguageIdFK() {

        Logger.info(this, "Adding new 'FK_workflow_task_language' FK to 'workflow_task' table.");

        try {

            if (DbConnectionFactory.isMsSql() && !DbConnectionFactory.getAutoCommit()) {
                DbConnectionFactory.setAutoCommit(true);
            }

            new DotConnect().executeStatement(getAddLanguageIdForeignKey());
        } catch (SQLException e) {
            throw new DotRuntimeException("The could not add the FK to the column 'language_id' on 'workflow_task' table.", e);
        }
    }


    private void updateAllWorkflowTaskToDefaultLanguage() throws DotDataException, IOException {

        Logger.info(this, "Running upgrade Task04330WorkflowTaskAddLanguageIdColumn");

        int selectStartRow                     = 0;
        int workflowTaskCount                  = 0;
        final String systemUserId              = this.getSystemUserId ();
        final File   workflowTaskDataFile      = File.createTempFile("rows-task-4330", "tmp");
        final MarshalUtils marshalUtils        = MarshalFactory.getInstance().getMarshalUtils();
        final ExecutorCompletionService<WorkflowTaskResult> completionService =
                new ExecutorCompletionService<>(this.submitter);
        BufferedWriter fileWriter                  = null;
        List<Map<String, Object>> workflowTaskList = new DotConnect()
                .setSQL(SELECT_WORKFLOW_TASK_SQL)
                .setStartRow(selectStartRow)
                .setMaxRows(SELECT_MAX_ROWS)
                .loadResults();

        try {

            Logger.info(this, "Running the upgrade for the workflow task languages");

            fileWriter = new BufferedWriter(new FileWriter(workflowTaskDataFile));

            while (workflowTaskList != null && !workflowTaskList.isEmpty()) {

                workflowTaskCount = workflowTaskList.size();

                Logger.info(this, "Reading workflow task from " + selectStartRow +
                        ", to " + (selectStartRow + SELECT_MAX_ROWS) + ", tasks count: " + workflowTaskCount);

                if (Config.getBooleanProperty("task.4330.runparallel", true)) {

                    Logger.info(this, "Processing the workflow task parallel");
                    workflowTaskList.stream().parallel().
                            forEach((Map<String, Object> workflowTaskMap) -> this.runTask(completionService, workflowTaskMap));
                } else {
                    Logger.info(this, "Processing the workflow task normally");
                    workflowTaskList.stream().
                            forEach((Map<String, Object> workflowTaskMap) -> this.runTask(completionService, workflowTaskMap));
                }

                this.processFutures(workflowTaskCount, completionService, fileWriter, marshalUtils);

                selectStartRow   += SELECT_MAX_ROWS;
                workflowTaskList  =
                        (List<Map<String, Object>>) new DotConnect()
                                .setSQL(SELECT_WORKFLOW_TASK_SQL)
                                .setStartRow(selectStartRow)
                                .setMaxRows(SELECT_MAX_ROWS)
                                .loadResults();
            }
        } finally {

            CloseUtils.closeQuietly(fileWriter);
        }

        this.doBatch (workflowTaskDataFile, marshalUtils, systemUserId);
    }

    private String getSystemUserId() throws DotDataException {
        return (String)new DotConnect()
                .setSQL("select id,role_name,role_key from cms_role where role_key = ?")
                .addParam(SYSTEM_USER_ROLE_KEY)
                .loadObjectResults()
                .stream().findFirst().orElse(Map.of("id", SYSTEM_USER_ID_DEFAULT))
                .get("id");
    }

    private void processLine(final String line,
                             final MarshalUtils marshalUtils,
                             final String systemUserId,
                             final MutableInt totalInsertAffected,
                             final MutableInt totalUpdateAffected) {
        final WorkflowTask workflowTask =
                marshalUtils.unmarshal(line, WorkflowTask.class);

        if (null != workflowTask) {

            if (workflowTask.isUpdate()) {

                Logger.info(this, "Updating the workflow: " + workflowTask.getId()
                        + ", with lang: " + workflowTask.getLanguageId());
                this.paramsUpdate.add(new Params(workflowTask.getLanguageId(),
                        workflowTask.getId()
                ));
            } else {

                Logger.info(this, "Inserting the workflow: " + workflowTask.getId()
                        + ", status:" + workflowTask.getStatus()
                        + ", webasset:" + workflowTask.getWebasset()
                        + ", with lang: " + workflowTask.getLanguageId());

                this.paramsInsert.add(new Params(workflowTask.getId(),
                        workflowTask.getTitle(),
                        DbConnectionFactory.now(),
                        DbConnectionFactory.now(),
                        systemUserId,
                        workflowTask.getAssignedTo(),
                        workflowTask.getStatus(),
                        workflowTask.getWebasset(),
                        workflowTask.getLanguageId()
                ));
            }

            if (this.paramsUpdate.size() >= MAX_BATCH_SIZE) {

                this.doUpdateBatch(totalUpdateAffected);
            }

            if (this.paramsInsert.size() >= MAX_BATCH_SIZE) {

                this.doInsertBatch(totalInsertAffected);
            }
        } else {

            Logger.warn(this, "The workflow task is null for line: " + line);
        }
    }

    private void doUpdateBatch (final MutableInt totalUpdateAffected) {

        try {
            final List<Integer> batchResult =
                    Ints.asList(new DotConnect().executeBatch(
                            this.getUpdateLanguageIdColumnSQL(),
                            this.paramsUpdate));

            final int rowsAffected = batchResult.stream().reduce(0, Integer::sum);
            Logger.info(this, "Batch rows workflow task with language, update: " + rowsAffected);
            totalUpdateAffected.add(rowsAffected);
        } catch (DotDataException e) {

            Logger.error(this, "Couldn't update these rows: " + this.paramsUpdate);
            Logger.error(this, e.getMessage(), e);
        } finally {
            this.paramsUpdate.clear();
        }
    }

    private void doInsertBatch (final MutableInt totalInsertAffected) {

        try {
            final List<Integer> batchResult =
                    Ints.asList(new DotConnect().executeBatch(
                            INSERT_WORKFLOW_TASK_SQL,
                            this.paramsInsert,
                            this::setParams));

            final int rowsAffected = batchResult.stream().reduce(0, Integer::sum);
            Logger.info(this, "Batch rows workflow task with language, inserted: " + rowsAffected);
            totalInsertAffected.add(rowsAffected);
        } catch (DotDataException e) {

            Logger.error(this, "Couldn't insert these rows: " + this.paramsInsert);
            Logger.error(this, e.getMessage(), e);
        } finally {
            this.paramsInsert.clear();
        }
    }

    private void doBatch(final File   workflowTaskDataFile,
                         final MarshalUtils marshalUtils,
                         final String systemUserId) throws IOException {

        final MutableInt totalInsertAffected   = new MutableInt(0);
        final MutableInt totalUpdateAffected   = new MutableInt(0);

        Logger.info(this, "Doing Workflow Task update batch");

        try (final Stream<String> streamLines = Files.lines(workflowTaskDataFile.toPath())) {

            streamLines.forEachOrdered( line -> this.processLine (line, marshalUtils, systemUserId,
                    totalInsertAffected, totalUpdateAffected));

            if (!this.paramsUpdate.isEmpty()) {

                this.doUpdateBatch(totalUpdateAffected);
            }

            if (!this.paramsInsert.isEmpty()) {

                this.doInsertBatch(totalInsertAffected);
            }
        } finally {

            Logger.info(this, "-- total inserts: " + totalInsertAffected.intValue());
            Logger.info(this, "-- total updates: " + totalUpdateAffected.intValue());
            Logger.info(this, "-- total rows Affected: " +
                    (totalInsertAffected.intValue() + totalUpdateAffected.intValue()));
        }
    }

    private void setParams(final PreparedStatement preparedStatement,
                           final Params params) throws SQLException {

        int i = 0;
        for (; i < 5; ++i) {

            preparedStatement.setObject(i+1, params.get(i));
        }

        // AssignedTo
        DotConnect.setStringOrNull(preparedStatement, i+1, (String)params.get(i++));
        // Status
        DotConnect.setStringOrNull(preparedStatement, i+1, (String)params.get(i++));
        // webasset
        DotConnect.setStringOrNull(preparedStatement, i+1, (String)params.get(i++));
        // lang id
        preparedStatement.setObject(i+1, params.get(i));

    }

    private void runTask(final ExecutorCompletionService<WorkflowTaskResult> completionService,
                         final Map<String, Object> workflowTaskMap) {

        completionService.submit(()-> this.runWorkflowTask (workflowTaskMap));
    }

    @CloseDBIfOpened
    private WorkflowTaskResult runWorkflowTask(final Map<String, Object> workflowTaskMap) {

        final String id                = (String)workflowTaskMap.get("id");
        final String assignedTo        = (String)workflowTaskMap.get("assigned_to");
        final String title             = (String)workflowTaskMap.get("title");
        final String status            = (String)workflowTaskMap.get("status");
        final String webasset          = (String)workflowTaskMap.get("webasset");
        final ImmutableList.Builder<WorkflowTask> tasks =
                new ImmutableList.Builder<>();

        try {

            final List<Map<String, Object>> contentletLanguages =
                    new DotConnect().setSQL(SELECT_CONTENTLET_LANGS_INFO_SQL)
                            .addParam(webasset).loadObjectResults();

            if (contentletLanguages.size() > 0) {

                tasks.add(new WorkflowTask(true, id, assignedTo, title, status, webasset,
                        ConversionUtils.toLong(contentletLanguages.get(0).get("lang"), DEFAULT_LANGUAGE_ID)));
                for (int i = 1; i < contentletLanguages.size(); ++i) {

                    tasks.add(new WorkflowTask(false, UUIDGenerator.generateUuid(), assignedTo, title, status, webasset,
                            ConversionUtils.toLong(contentletLanguages.get(i).get("lang"), DEFAULT_LANGUAGE_ID)));
                }
            } else {

                Logger.info(this, "The not version info for the workflowTaskMap: " + workflowTaskMap);
            }
        } catch (DotDataException e) {

            Logger.error(this, e.getMessage(), e);
        }

        return new WorkflowTaskResult(tasks.build());
    }

    private void processFutures(final int workflowTaskCount,
                                final ExecutorCompletionService<WorkflowTaskResult> completionService,
                                final BufferedWriter workflowTaskDataFileWriter,
                                final MarshalUtils marshalUtils) {

        for (int i = 0; i < workflowTaskCount; ++i) {

            try {

                final List<WorkflowTask> tasks =
                        completionService.take().get().getWorkflowTasks();

                if (!tasks.isEmpty()) {

                    for (final WorkflowTask workflowTask : tasks) {

                        try {

                            workflowTaskDataFileWriter.write(marshalUtils.marshal(workflowTask));
                            workflowTaskDataFileWriter.newLine();
                        } catch (IOException e) {
                            Logger.error(this, e.getMessage(), e);
                        }
                    }
                } else {

                    Logger.warn(this, "Zero tasks recovered");
                }
            } catch (InterruptedException | ExecutionException e) {

                Logger.error(this, e.getMessage(), e);
            }
        }

        try {

            workflowTaskDataFileWriter.flush();
        } catch (IOException e) {
            Logger.error(this, e.getMessage(), e);
        }
    }

    private String getAddLanguageIdForeignKey() {

        final DbType dbType = DbType.getDbType(DbConnectionFactory.getDBType());

        return addLanguageIdForeignKeySQLMap.getOrDefault(dbType, null);
    }

    private String getUpdateLanguageIdColumnSQL() {

        final DbType dbType = DbType.getDbType(DbConnectionFactory.getDBType());

        return updateLanguageIdColumnSQLMap.getOrDefault(dbType, null);
    }

    private String getAddLanguageIdIndex() {

        final DbType dbType = DbType.getDbType(DbConnectionFactory.getDBType());

        return addLanguageIdIndexSQLMap.getOrDefault(dbType, null);
    }

    private String getAddLanguageIdColumnSQL() {

        final DbType dbType = DbType.getDbType(DbConnectionFactory.getDBType());

        return addLanguageIdColumnSQLMap.getOrDefault(dbType, null);
    }

    private String getSelectLanguageIdColumnSQL() {

        final DbType dbType = DbType.getDbType(DbConnectionFactory.getDBType());

        return selectLanguageIdColumnSQLMap.getOrDefault(dbType, null);
    }

    private void closeCommitAndStartTransaction() throws DotHibernateException {
        if (DbConnectionFactory.inTransaction()) {
            HibernateUtil.closeAndCommitTransaction();
            HibernateUtil.startTransaction();
        }
    }

    private void closeAndStartTransaction() throws DotHibernateException {

        HibernateUtil.closeSessionSilently();
        HibernateUtil.startTransaction();
    }

    @Override
    public String getPostgresScript() { return null; }

    @Override
    public String getMySQLScript() { return null; }

    @Override
    public String getOracleScript() { return null; }

    @Override
    public String getMSSQLScript() { return null; }

    @Override
    protected List<String> getTablesToDropConstraints() { return Collections.emptyList(); }


    public static class WorkflowTaskResult {

        private final List<WorkflowTask> workflowTasks;

        public WorkflowTaskResult(List<WorkflowTask> workflowTasks) {
            this.workflowTasks = workflowTasks;
        }

        public List<WorkflowTask> getWorkflowTasks() {
            return workflowTasks;
        }
    }
    public static class WorkflowTask {

        private final boolean update;
        private final String  id;
        private final String  assignedTo;
        private final String  title;
        private final String  status;
        private final String  webasset;
        private final long    languageId;

        @JsonCreator
        public WorkflowTask(@JsonProperty("update") final boolean update,
                            @JsonProperty("id") final String  id,
                            @JsonProperty("assignedTo") final String  assignedTo,
                            @JsonProperty("title") final String  title,
                            @JsonProperty("status") final String  status,
                            @JsonProperty("webasset") final String  webasset,
                            @JsonProperty("languageId") final long    languageId) {

            this.update     = update;
            this.id         = id;
            this.assignedTo = assignedTo;
            this.title      = title;
            this.status     = status;
            this.webasset   = webasset;
            this.languageId = languageId;
        }

        public boolean isUpdate() {
            return update;
        }

        public String getId() {
            return id;
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
