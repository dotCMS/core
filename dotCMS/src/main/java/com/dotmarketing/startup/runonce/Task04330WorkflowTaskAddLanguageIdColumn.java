

package com.dotmarketing.startup.runonce;

import static com.dotcms.util.CollectionsUtils.*;

import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.ConversionUtils;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This upgrade task will add the column language_id to the workflow_task, set the default language id to all of current records
 * and add an index for the language_id.
 *
 * @author jsanca
 * @version 5.0
 *
 */
public class Task04330WorkflowTaskAddLanguageIdColumn extends AbstractJDBCStartupTask {

    private static final Map<DbType, String> selectLanguageIdColumnSQLMap   = map(
            DbType.POSTGRESQL,   "SELECT language_id FROM workflow_task",
            DbType.MYSQL,        "SELECT language_id FROM workflow_task",
            DbType.ORACLE,       "SELECT language_id FROM workflow_task",
            DbType.MSSQL,        "SELECT language_id FROM workflow_task"
            );

    private static final Map<DbType, String> addLanguageIdColumnSQLMap      = map(
            DbType.POSTGRESQL,   "ALTER TABLE workflow_task ADD language_id INT8",
            DbType.MYSQL,        "ALTER TABLE workflow_task ADD language_id bigint",
            DbType.ORACLE,       "ALTER TABLE workflow_task ADD language_id number(19,0)",
            DbType.MSSQL,        "ALTER TABLE workflow_task ADD language_id NUMERIC(19,0) null"
    );

    private static final Map<DbType, String> updateLanguageIdColumnSQLMap   = map(
            DbType.POSTGRESQL,   "UPDATE workflow_task SET language_id = ?",
            DbType.MYSQL,        "UPDATE workflow_task SET language_id = ?",
            DbType.ORACLE,       "UPDATE workflow_task SET language_id = ?",
            DbType.MSSQL,        "UPDATE workflow_task SET language_id = ?"
    );

    private static final Map<DbType, String> addLanguageIdIndexSQLMap       = map(
            DbType.POSTGRESQL,   "create index idx_workflow_6 on workflow_task (language_id)",
            DbType.MYSQL,        "create index idx_workflow_6 on workflow_task (language_id)",
            DbType.ORACLE,       "create index idx_workflow_6 on workflow_task (language_id)",
            DbType.MSSQL,        "create index idx_workflow_6 on workflow_task (language_id)"
    );

    private static final Map<DbType, String> addLanguageIdForeignKeySQLMap       = map(
            DbType.POSTGRESQL,   "alter table workflow_task add constraint FK_workflow_task_language foreign key (language_id) references language(id)",
            DbType.MYSQL,        "ALTER TABLE workflow_task ADD CONSTRAINT FK_workflow_task_language FOREIGN KEY (language_id) REFERENCES language(id)",
            DbType.ORACLE,       "alter table workflow_task add constraint FK_workflow_task_language foreign key (language_id) references language(id)",
            DbType.MSSQL,        "ALTER TABLE workflow_task ADD CONSTRAINT FK_workflow_task_language FOREIGN KEY (language_id) REFERENCES language(id)"
    );

    private static final long INVALID_LANGUAGE_ID = -1;
    @Override
    public boolean forceRun() {
        return Boolean.TRUE;
    }

    @Override
    public void executeUpgrade() throws DotDataException {


        final boolean created = this.addLanguageIdColumn();

        if (created) {

            if (DbConnectionFactory.isMsSql() && DbConnectionFactory.getAutoCommit()) {
                DbConnectionFactory.setAutoCommit(false); // set a transactional for data
            }

            this.updateAllWorkflowTaskToDefaultLanguage();

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


    private void updateAllWorkflowTaskToDefaultLanguage() throws DotDataException {

        final long id = this.getDefaultLanguageId();

        if (id > 0) {

            Logger.info(this, "Updating all workflow_task to the default language: "
                    + id + ", rows affected: " +
                    new DotConnect().executeUpdate(this.getUpdateLanguageIdColumnSQL(), id));
        } else {

            Logger.warn(this, "The default language id: " + id +
                            " is not valid, the current workflow task were not updated");
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

    private long getDefaultLanguageId() throws DotDataException {

        final String languageCode = Config.getStringProperty("DEFAULT_LANGUAGE_CODE");
        final String countryCode  = Config.getStringProperty("DEFAULT_LANGUAGE_COUNTRY_CODE");
        List<Map<String, Object>> results         = null;

        Logger.info(this, "Checking the default language, code: "
                + languageCode + ", country: " + countryCode);

        results = (UtilMethods.isSet(countryCode))?
                new DotConnect().setSQL("select id from Language where language_code=? and country_code=?")
                    .addParam(languageCode.toLowerCase()).addParam(countryCode.toUpperCase()).loadResults():
                new DotConnect().setSQL("select id from Language where language_code=? and and (country_code = '' OR country_code IS NULL)")
                    .addParam(languageCode.toLowerCase()).loadObjectResults();

        return null != results && results.size() > 0?
                ConversionUtils.toLong(results.get(0).getOrDefault("id", INVALID_LANGUAGE_ID).toString()) :INVALID_LANGUAGE_ID;
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
    public String getH2Script() { return null; }

    @Override
    protected List<String> getTablesToDropConstraints() { return Collections.emptyList(); }

}
