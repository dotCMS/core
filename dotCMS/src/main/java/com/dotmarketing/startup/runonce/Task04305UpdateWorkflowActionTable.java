package com.dotmarketing.startup.runonce;

import com.dotcms.util.ConversionUtils;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.common.db.ForeignKey;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.workflows.model.WorkflowState;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;

/**
 * This task updates the definition of the {@code workflow_action} table, creates the new
 * intermediate {@code workflow_action_step} table, and updates the definition of a table index:
 * <ul>
 * <li>A new column called {@code scheme_id} will be added to the {@code workflow_action} table.
 * This allows actions to be assigned to Workflow Schemes and not to steps directly.</li>
 * <li>The new {@code workflow_action_step} table allows an action to be associated to one or more
 * steps (an N-N relationship).</li>
 * <li>The index created for the {@code workflow_scheme_x_structure} column will be updated to NOT
 * be unique.</li>
 * </ul>
 *
 * @author Jose Castro
 * @version 4.3.0
 * @since Nov 1st, 2017
 */
public class Task04305UpdateWorkflowActionTable implements StartupTask {

    private static final String MYSQL_FIND_INTERMEDIATE_TABLE    = "SELECT * from workflow_action_step";
    private static final String POSTGRES_FIND_INTERMEDIATE_TABLE = "SELECT * from workflow_action_step";
    private static final String MSSQL_FIND_INTERMEDIATE_TABLE    = "SELECT * from workflow_action_step";
    private static final String ORACLE_FIND_INTERMEDIATE_TABLE   = "SELECT * from workflow_action_step";

    private static final String MYSQL_CREATE_INTERMEDIATE_TABLE = "CREATE TABLE workflow_action_step (action_id VARCHAR(36) NOT NULL, step_id VARCHAR(36) NOT NULL, action_order INT default 0)";
    private static final String POSTGRES_CREATE_INTERMEDIATE_TABLE = MYSQL_CREATE_INTERMEDIATE_TABLE;
    private static final String MSSQL_CREATE_INTERMEDIATE_TABLE = "CREATE TABLE workflow_action_step ( action_id NVARCHAR(36) NOT NULL, step_id NVARCHAR(36) NOT NULL, action_order INT default 0, CONSTRAINT pk_workflow_action_step PRIMARY KEY NONCLUSTERED (action_id, step_id) )";
    private static final String ORACLE_CREATE_INTERMEDIATE_TABLE = "CREATE TABLE workflow_action_step ( action_id varchar2(36) NOT NULL, step_id varchar2(36) NOT NULL, action_order number(10,0) default 0, CONSTRAINT pk_workflow_action_step PRIMARY KEY (action_id, step_id) )";

    private static final String MYSQL_CREATE_INTERMEDIATE_TABLE_PK = "ALTER TABLE workflow_action_step ADD CONSTRAINT pk_workflow_action_step PRIMARY KEY (action_id, step_id)";
    private static final String POSTGRES_CREATE_INTERMEDIATE_TABLE_PK = MYSQL_CREATE_INTERMEDIATE_TABLE_PK;

    private static final String MYSQL_FIND_SHOW_ON_OPTION_COLUMN     = "SELECT show_on FROM workflow_action";
    private static final String POSTGRES_FIND_SHOW_ON_OPTION_COLUMN  = "SELECT show_on FROM workflow_action";
    private static final String MSSQL_FIND_SHOW_ON_OPTION_COLUMN     = "SELECT show_on FROM workflow_action";
    private static final String ORACLE_FIND_SHOW_ON_OPTION_COLUMN    = "SELECT show_on FROM workflow_action";

    private static final String MYSQL_FIND_SCHEME_ID_COLUMN    = "SELECT scheme_id FROM workflow_action";
    private static final String POSTGRES_FIND_SCHEME_ID_COLUMN = "SELECT scheme_id FROM workflow_action";
    private static final String MSSQL_FIND_SCHEME_ID_COLUMN    = "SELECT scheme_id FROM workflow_action";
    private static final String ORACLE_FIND_SCHEME_ID_COLUMN   = "SELECT scheme_id FROM workflow_action";

    private static final String MYSQL_ADD_SCHEME_ID_COLUMN    = "ALTER TABLE workflow_action ADD scheme_id VARCHAR(36)";
    private static final String POSTGRES_ADD_SCHEME_ID_COLUMN = MYSQL_ADD_SCHEME_ID_COLUMN;
    private static final String MSSQL_ADD_SCHEME_ID_COLUMN    = "ALTER TABLE workflow_action ADD scheme_id NVARCHAR(36)";
    private static final String ORACLE_ADD_SCHEME_ID_COLUMN   = "ALTER TABLE workflow_action ADD scheme_id varchar2(36)";

    private static final String MYSQL_NOT_NULL_CONSTRAINT_SHOW_ON_COLUMN     = "ALTER TABLE workflow_action MODIFY       scheme_id VARCHAR(36)  NOT NULL;";
    private static final String POSTGRES_NOT_NULL_CONSTRAINT_SHOW_ON_COLUMN  = "ALTER TABLE workflow_action ALTER COLUMN scheme_id SET          NOT NULL;";
    private static final String MSSQL_NOT_NULL_CONSTRAINT_SHOW_ON_COLUMN     = "ALTER TABLE workflow_action ALTER COLUMN scheme_id NVARCHAR(36) NOT NULL";
    private static final String ORACLE_NOT_NULL_CONSTRAINT_SHOW_ON_COLUMN    = "ALTER TABLE workflow_action MODIFY       scheme_id varchar2(36) NOT NULL";

    private static final String MYSQL_ADD_SHOW_ON_OPTION_COLUMN    = "ALTER TABLE workflow_action ADD show_on varchar(255)  default 'LOCKED,UNLOCKED'";
    private static final String POSTGRES_ADD_SHOW_ON_OPTION_COLUMN = "ALTER TABLE workflow_action ADD show_on varchar(255)  default 'LOCKED,UNLOCKED'";
    private static final String MSSQL_ADD_SHOW_ON_OPTION_COLUMN    = "ALTER TABLE workflow_action ADD show_on NVARCHAR(255) default 'LOCKED,UNLOCKED'";
    private static final String ORACLE_ADD_SHOW_ON_OPTION_COLUMN   = "ALTER TABLE workflow_action ADD show_on varchar2(255) default 'LOCKED,UNLOCKED'";


    private static final String MYSQL_SELECT_ACTIONS_AND_STEPS = "SELECT wa.id action_id, ws.id step_id, wa.my_order FROM workflow_step ws INNER JOIN workflow_action wa ON ws.id = wa.step_id";
    private static final String POSTGRES_SELECT_ACTIONS_AND_STEPS = MYSQL_SELECT_ACTIONS_AND_STEPS;
    private static final String MSSQL_SELECT_ACTIONS_AND_STEPS = MYSQL_SELECT_ACTIONS_AND_STEPS;
    private static final String ORACLE_SELECT_ACTIONS_AND_STEPS = MYSQL_SELECT_ACTIONS_AND_STEPS;

    private static final String MYSQL_INSERT_INTO_INTERMEDIATE_TABLE    = "INSERT INTO workflow_action_step (action_id, step_id, action_order) VALUES (?, ?, ?)";
    private static final String POSTGRES_INSERT_INTO_INTERMEDIATE_TABLE = MYSQL_INSERT_INTO_INTERMEDIATE_TABLE;
    private static final String MSSQL_INSERT_INTO_INTERMEDIATE_TABLE    = MYSQL_INSERT_INTO_INTERMEDIATE_TABLE;
    private static final String ORACLE_INSERT_INTO_INTERMEDIATE_TABLE   = MYSQL_INSERT_INTO_INTERMEDIATE_TABLE;

    private static final String MYSQL_SELECT_SCHEME_IDS_FOR_ACTIONS = "SELECT wa.id action_id, ws.scheme_id, wa.requires_checkout FROM workflow_action wa INNER JOIN workflow_step ws ON ws.id = wa.step_id";
    private static final String POSTGRES_SELECT_SCHEME_IDS_FOR_ACTIONS = MYSQL_SELECT_SCHEME_IDS_FOR_ACTIONS;
    private static final String MSSQL_SELECT_SCHEME_IDS_FOR_ACTIONS = MYSQL_SELECT_SCHEME_IDS_FOR_ACTIONS;
    private static final String ORACLE_SELECT_SCHEME_IDS_FOR_ACTIONS = MYSQL_SELECT_SCHEME_IDS_FOR_ACTIONS;


    private static final String MYSQL_UPDATE_SCHEME_IDS_FOR_ACTIONS = "UPDATE workflow_action SET scheme_id = ?,show_on = ?  WHERE id = ?";
    private static final String POSTGRES_UPDATE_SCHEME_IDS_FOR_ACTIONS = MYSQL_UPDATE_SCHEME_IDS_FOR_ACTIONS;
    private static final String MSSQL_UPDATE_SCHEME_IDS_FOR_ACTIONS = MYSQL_UPDATE_SCHEME_IDS_FOR_ACTIONS;
    private static final String ORACLE_UPDATE_SCHEME_IDS_FOR_ACTIONS = MYSQL_UPDATE_SCHEME_IDS_FOR_ACTIONS;

    private static final String MYSQL_SELECT_WORKFLOW_SCHEME_X_STRUCTURE_INDEX = "SELECT index_name FROM information_schema.statistics WHERE table_name = 'workflow_scheme_x_structure' AND column_name = 'structure_id' AND index_name = 'workflow_idx_scheme_structure_2'";
    private static final String POSTGRES_SELECT_WORKFLOW_SCHEME_X_STRUCTURE_INDEX = "SELECT indexname FROM pg_indexes WHERE tablename = 'workflow_scheme_x_structure' AND indexname = 'workflow_idx_scheme_structure_2'";
    private static final String MSSQL_SELECT_WORKFLOW_SCHEME_X_STRUCTURE_INDEX = "SELECT name FROM sys.indexes WHERE object_id = (SELECT object_id FROM sys.objects WHERE name = 'workflow_scheme_x_structure') AND name = 'workflow_idx_scheme_structure_2'";
    private static final String ORACLE_SELECT_WORKFLOW_SCHEME_X_STRUCTURE_INDEX = "SELECT index_name, table_owner, table_name, uniqueness FROM user_indexes WHERE table_name = 'WORKFLOW_SCHEME_X_STRUCTURE' AND index_name = 'WK_IDX_SCHEME_STR_2'";

    private static final String MYSQL_DROP_WORKFLOW_SCHEME_X_STRUCTURE_INDEX = "DROP INDEX workflow_idx_scheme_structure_2 ON workflow_scheme_x_structure";
    private static final String POSTGRES_DROP_WORKFLOW_SCHEME_X_STRUCTURE_INDEX = "DROP INDEX IF EXISTS workflow_idx_scheme_structure_2 CASCADE";
    private static final String MSSQL_DROP_WORKFLOW_SCHEME_X_STRUCTURE_INDEX = MYSQL_DROP_WORKFLOW_SCHEME_X_STRUCTURE_INDEX;
    private static final String ORACLE_DROP_WORKFLOW_SCHEME_X_STRUCTURE_INDEX = "DROP INDEX WK_IDX_SCHEME_STR_2";

    private static final String MYSQL_DROP_WORKFLOW_ACTION_STEP_INDEX = "DROP INDEX workflow_idx_action_step ON workflow_action";
    private static final String POSTGRES_DROP_WORKFLOW_ACTION_STEP_INDEX = "DROP INDEX IF EXISTS workflow_idx_action_step CASCADE";
    private static final String MSSQL_DROP_WORKFLOW_ACTION_STEP_INDEX = MYSQL_DROP_WORKFLOW_ACTION_STEP_INDEX;
    private static final String ORACLE_DROP_WORKFLOW_ACTION_STEP_INDEX = "DROP INDEX wk_idx_act_step";

    private static final String MYSQL_DROP_WORKFLOW_ACTION_STEP_NOT_NULL = "ALTER TABLE workflow_action MODIFY step_id varchar(36) NULL";
    private static final String POSTGRES_DROP_WORKFLOW_ACTION_STEP_NOT_NULL = "ALTER TABLE workflow_action ALTER COLUMN step_id DROP NOT NULL";
    private static final String MSSQL_DROP_WORKFLOW_ACTION_STEP_NOT_NULL = "ALTER TABLE workflow_action ALTER COLUMN step_id NVARCHAR(36) NULL";
    private static final String ORACLE_DROP_WORKFLOW_ACTION_STEP_NOT_NULL = "ALTER TABLE workflow_action MODIFY (step_id NULL)";

    private static final String POSTGRES_CREATE_WORKFLOW_SCHEME_X_STRUCTURE_INDEX = "CREATE INDEX workflow_idx_scheme_structure_2 ON workflow_scheme_x_structure(structure_id)";
    private static final String ORACLE_CREATE_WORKFLOW_SCHEME_X_STRUCTURE_INDEX = "CREATE INDEX wk_idx_scheme_str_2 ON workflow_scheme_x_structure(structure_id)";

    // FOREIGH KEYS
    private static final String MYSQL_CREATE_INTERMEDIATE_TABLE_FOREIGN_KEY_ACTION_ID    = "alter table workflow_action_step add constraint fk_w_action_step_action_id foreign key (action_id) references workflow_action(id)";
    private static final String MYSQL_CREATE_INTERMEDIATE_TABLE_FOREIGN_KEYS_STEP_ID     = "alter table workflow_action_step add constraint fk_w_action_step_step_id   foreign key (step_id)   references workflow_step  (id)";

    private static final String POSTGRES_CREATE_INTERMEDIATE_TABLE_FOREIGN_KEY_ACTION_ID = "alter table workflow_action_step add constraint fk_w_action_step_action_id foreign key (action_id) references workflow_action(id)";
    private static final String POSTGRES_CREATE_INTERMEDIATE_TABLE_FOREIGN_KEYS_STEP_ID  = "alter table workflow_action_step add constraint fk_w_action_step_step_id   foreign key (step_id)   references workflow_step  (id)";

    private static final String MSSQL_CREATE_INTERMEDIATE_TABLE_FOREIGN_KEY_ACTION_ID    = "alter table workflow_action_step add constraint fk_w_action_step_action_id foreign key (action_id) references workflow_action(id)";
    private static final String MSSQL_CREATE_INTERMEDIATE_TABLE_FOREIGN_KEYS_STEP_ID     = "alter table workflow_action_step add constraint fk_w_action_step_step_id   foreign key (step_id)   references workflow_step  (id)";

    private static final String ORACLE_CREATE_INTERMEDIATE_TABLE_FOREIGN_KEY_ACTION_ID   = "alter table workflow_action_step add constraint fk_w_action_step_action_id foreign key (action_id) references workflow_action(id)";
    private static final String ORACLE_CREATE_INTERMEDIATE_TABLE_FOREIGN_KEYS_STEP_ID    = "alter table workflow_action_step add constraint fk_w_action_step_step_id   foreign key (step_id)   references workflow_step  (id)";

    @Override
    public boolean forceRun() {
        return Boolean.TRUE;
    }

    @Override
    public void executeUpgrade() throws DotDataException {
        final DotConnect dc = new DotConnect();

        // SCHEMA CHANGES
        this.createWorkflowActionStepTable                      (dc);
        final boolean schemeIdColumnCreated =
                this.addSchemeIdColumn                          (dc);
        this.addShowOnColumn                                    (dc);
        this.removeWorkflowActionStepIdWorkflowStepFK             ();
        this.executeDropWorkflowActionStepIdIndex               (dc);
        this.executeDropWorkflowActionStepIdNotNullConstrain    (dc);

        // DATA CHANGES
        if (DbConnectionFactory.isMsSql() && DbConnectionFactory.getAutoCommit()) {
            DbConnectionFactory.setAutoCommit(false); // set a transactional for data
        }
        this.addWorkflowActionStepData                          (dc);
        this.updateWorkflowActionData                           (dc);

        // if mssql is in a transaction for the data, commit, close and start a new one.
        if (DbConnectionFactory.isMsSql() && !DbConnectionFactory.getAutoCommit()) {
            this.closeCommitAndStartTransaction();
            DbConnectionFactory.setAutoCommit(true);
        }

        this.updateWorkflowSchemeXStructureData                 (dc);

        if (schemeIdColumnCreated) {
            this.addNotNullConstraintShowOnColumn               (dc);
        }
    } // executeUpgrade.

    private void removeWorkflowActionStepIdWorkflowStepFK() throws DotDataException {

        if (DbConnectionFactory.isMsSql() && !DbConnectionFactory.getAutoCommit()) {
            DbConnectionFactory.setAutoCommit(true);
        }

        final DotDatabaseMetaData metaData = new DotDatabaseMetaData();
        final ForeignKey foreignKey        = metaData.findForeignKeys
                ("workflow_action", "workflow_step",
                        Arrays.asList("step_id"), Arrays.asList("id"));

        if (null != foreignKey) {

            try {
                Logger.info(this, "Droping the FK: " +foreignKey);
                metaData.dropForeignKey(foreignKey);
            } catch (SQLException e) {
                throw new DotDataException(e.getMessage(), e);
            } finally {
                this.closeCommitAndStartTransaction();
            }
        }
    }

    private void executeDropWorkflowActionStepIdIndex(final DotConnect dc) {
        Logger.info(this, "Removing 'workflow_idx_action_step' index in 'workflow_action' table.");
        try {

            if (DbConnectionFactory.isMsSql() && !DbConnectionFactory.getAutoCommit()) {
                DbConnectionFactory.setAutoCommit(true);
            }

            dc.executeStatement(dropWorkflowActionStepIdIndexQuery());
        } catch (SQLException e) {
            throw new DotRuntimeException(
                    "An error occurred when removing 'workflow_idx_action_step' index in 'workflow_action' table.",
                    e);
        }
    }

    private void executeDropWorkflowActionStepIdNotNullConstrain(final DotConnect dc) {
        Logger.info(this, "Removing the 'step_id' NOT NULL constrain from the 'workflow_action' table.");
        try {

            if (DbConnectionFactory.isMsSql() && !DbConnectionFactory.getAutoCommit()) {
                DbConnectionFactory.setAutoCommit(true);
            }

            dc.executeStatement(dropWorkflowActionStepIdNotNullConstrainQuery());
        } catch (SQLException e) {
            throw new DotRuntimeException(
                    "An error occurred when removing the 'step_id' NOT NULL constrain from the 'workflow_action' table.",
                    e);
        }
    }

    private void addNotNullConstraintShowOnColumn(final DotConnect dc) {

        Logger.info(this, "Adding new 'scheme_id' constraints");

        try {
            dc.executeStatement(getNotNullConstraintShowOnColumn());
        } catch (SQLException e) {
            throw new DotRuntimeException("The 'scheme_id' column could not add the not null constraints.", e);
        }
    } // addNotNullConstraintShowOnColumn.

    private String getNotNullConstraintShowOnColumn() {

        String sql = null;
        if (DbConnectionFactory.isMySql()) {
            sql = MYSQL_NOT_NULL_CONSTRAINT_SHOW_ON_COLUMN;
        } else if (DbConnectionFactory.isPostgres()) {
            sql = POSTGRES_NOT_NULL_CONSTRAINT_SHOW_ON_COLUMN;
        } else if (DbConnectionFactory.isMsSql()) {
            sql = MSSQL_NOT_NULL_CONSTRAINT_SHOW_ON_COLUMN;
        } else if (DbConnectionFactory.isOracle()) {
            sql = ORACLE_NOT_NULL_CONSTRAINT_SHOW_ON_COLUMN;
        }

        return sql;
    } // getNotNullConstraintShowOnColumn.

    private void updateWorkflowSchemeXStructureData(DotConnect dc) throws DotDataException {
        Logger.info(this, "Updating index in 'workflow_scheme_x_structure' table.");
        try {
            dc.setSQL(selectTableIndex());
            final List<Map<String, Object>> tableIndex = dc.loadObjectResults();
            if (!tableIndex.isEmpty()) {
                dc.executeStatement(dropTableIndex());
            }
            // In MySQL and SQL Server re-creating the index is NOT necessary as the regular index
            // is created separately
            if (DbConnectionFactory.isPostgres() || DbConnectionFactory.isOracle()) {
                dc.executeStatement(createTableIndex());
            }
        } catch (SQLException e) {
            throw new DotRuntimeException(
                    "An error occurred when updating the index in 'workflow_scheme_x_structure' table.",
                    e);
        }
    }

    private void updateWorkflowActionData(final DotConnect dc) throws DotDataException {

        Logger.info(this, "Associating Workflow Actions to Workflow Schemes.");

        final List<Map<String, Object>> schemeIds =
                dc.setSQL(selectSchemeIdsForActions()).loadObjectResults();
        schemeIds.stream().forEach(row -> {

            dc.setSQL(updateSchemeIdsForActions());
            dc.addParam(row.get("scheme_id").toString());
            dc.addParam(this.isLocked(row.get("requires_checkout"))?
                    WorkflowState.LOCKED.name(): WorkflowState.UNLOCKED.name());
            dc.addParam(row.get("action_id").toString());

            try {
                dc.loadResult();
            } catch (DotDataException e) {
                throw new DotRuntimeException(
                        "An error occurred when associating Workflow Actions to Workflow Schemes.",
                        e);
            }
        });
    } // updateWorkflowActionData.

    private void addWorkflowActionStepData(final DotConnect dc) throws DotDataException {

        Logger.info(this, "Adding data to 'workflow_action_step' table.");
        final List<Map<String, Object>> results =
                dc.setSQL(selectActionsAndSteps()).loadObjectResults();

        results.stream().forEach(row -> {

            dc.setSQL(insertActionsAndSteps());
            dc.addParam(row.get("action_id"));
            dc.addParam(row.get("step_id"));
            dc.addParam(row.get("my_order"));

            Logger.debug(this, "Adding to workflow_action_step the row: " + row);

            try {
                dc.loadResult();
            } catch (DotDataException e) {

                Logger.error(this, "ERROR on Adding to workflow_action_step the row: "
                        + row + ", err:" + e.getMessage(), e);
                throw new DotRuntimeException(
                        "An error occurred when adding data to the 'workflow_action_step' table.",
                        e);
            }
        });
    } // addWorkflowActionStepData.

    private void addShowOnColumn(final DotConnect dc) throws DotDataException {

        boolean needToCreate = false;
        Logger.info(this, "Adding new 'show_on' column to 'workflow_action' table.");

        try {

            if (DbConnectionFactory.isMsSql() && !DbConnectionFactory.getAutoCommit()) {
                DbConnectionFactory.setAutoCommit(true);
            }

            dc.setSQL(findShowOnColumn()).loadObjectResults();
        } catch (Throwable e) {

            Logger.info(this, "Column 'workflow_action.show_on' does not exists, creating it");
            needToCreate = true;
            // in some databases if an error is throw the transaction is not longer valid
            this.closeAndStartTransaction();
        }

        if (needToCreate) {
            try {

                if (DbConnectionFactory.isMsSql() && !DbConnectionFactory.getAutoCommit()) {
                    DbConnectionFactory.setAutoCommit(true);
                }

                dc.executeStatement(addShowOnColumn());
            } catch (SQLException e) {
                throw new DotRuntimeException("The 'show_on' column could not be created.", e);
            } finally {
                this.closeCommitAndStartTransaction();
            }
        }
    } // addShowOnColumn.

    private boolean addSchemeIdColumn(final DotConnect dc) throws DotDataException {

        boolean needToCreate = false;
        Logger.info(this, "Adding new 'scheme_id' column to 'workflow_action' table.");

        try {

            if (DbConnectionFactory.isMsSql() && !DbConnectionFactory.getAutoCommit()) {
                DbConnectionFactory.setAutoCommit(true);
            }

            dc.setSQL(findSchemeIdColumn()).loadObjectResults();
        } catch (Throwable e) {

            Logger.info(this, "Column 'workflow_action.scheme_id' does not exists, creating it");
            needToCreate = true;
            // in some databases if an error is throw the transaction is not longer valid
            this.closeAndStartTransaction();
        }

        if (needToCreate) {
            try {

                if (DbConnectionFactory.isMsSql() && !DbConnectionFactory.getAutoCommit()) {
                    DbConnectionFactory.setAutoCommit(true);
                }

                dc.executeStatement(addSchemeIdColumn());
            } catch (SQLException e) {
                throw new DotRuntimeException("The 'scheme_id' column could not be created.", e);
            } finally {
                this.closeCommitAndStartTransaction();
            }
        }

        return needToCreate;
    } // addSchemeIdColumn.

    private void createWorkflowActionStepTable(final DotConnect dc) throws DotDataException {

        boolean needToCreate = false;
        Logger.info(this, "Creating intermediate 'workflow_action_step' table.");

        try {

            if (DbConnectionFactory.isMsSql() && !DbConnectionFactory.getAutoCommit()) {
                DbConnectionFactory.setAutoCommit(true);
            }

            dc.setSQL(findIntermediateTable()).loadObjectResults();
        } catch (Throwable e) {

            Logger.info(this, "Table 'workflow_action_step' does not exists, creating it");
            needToCreate = true;
            // in some databases if an error is throw the transaction is not longer valid
            this.closeAndStartTransaction();
        }

        if (needToCreate) {
            try {

                if (DbConnectionFactory.isMsSql() && !DbConnectionFactory.getAutoCommit()) {
                    DbConnectionFactory.setAutoCommit(true);
                }

                dc.setSQL(createIntermediateTable()).loadResult();
                // The SQL Server and Oracle table definition already include de PK creation
                if (DbConnectionFactory.isMySql() || DbConnectionFactory.isPostgres()) {
                    dc.setSQL(createIntermediateTablePk()).loadResult();
                }

                // adding the FK
                Logger.info(this, "Creating the Workflow action step intermediate FKs.");
                dc.setSQL(this.createIntermediateTableForeignKeyActionId()).loadResult();
                dc.setSQL(this.createIntermediateTableForeignKeyStepId()).loadResult();
            } catch (Exception e) {
                throw new DotRuntimeException(
                        "The 'workflow_action_step' table could not be created.", e);
            } finally {
                this.closeCommitAndStartTransaction();
            }
        }
    } // createWorkflowActionStepTable.

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

    private boolean isLocked(final Object requiresCheckout) {

        return (null != requiresCheckout)?
                ConversionUtils.toBooleanFromDb(requiresCheckout):false;
    }

    private String createIntermediateTableForeignKeyActionId() {

        String sql;
        if (DbConnectionFactory.isMySql()) {
            sql = MYSQL_CREATE_INTERMEDIATE_TABLE_FOREIGN_KEY_ACTION_ID;
        } else if (DbConnectionFactory.isPostgres()) {
            sql = POSTGRES_CREATE_INTERMEDIATE_TABLE_FOREIGN_KEY_ACTION_ID;
        } else if (DbConnectionFactory.isMsSql()) {
            sql = MSSQL_CREATE_INTERMEDIATE_TABLE_FOREIGN_KEY_ACTION_ID;
        } else {
            sql = ORACLE_CREATE_INTERMEDIATE_TABLE_FOREIGN_KEY_ACTION_ID;
        }

        return sql;
    }

    private String createIntermediateTableForeignKeyStepId() {

        String sql;
        if (DbConnectionFactory.isMySql()) {
            sql =  MYSQL_CREATE_INTERMEDIATE_TABLE_FOREIGN_KEYS_STEP_ID;
        } else if (DbConnectionFactory.isPostgres()) {
            sql =  POSTGRES_CREATE_INTERMEDIATE_TABLE_FOREIGN_KEYS_STEP_ID;
        } else if (DbConnectionFactory.isMsSql()) {
            sql =  MSSQL_CREATE_INTERMEDIATE_TABLE_FOREIGN_KEYS_STEP_ID;
        } else {
            sql =  ORACLE_CREATE_INTERMEDIATE_TABLE_FOREIGN_KEYS_STEP_ID;
        }

        return sql;
    }

    /**
     * Verifies if the unique index in the {@code workflow_scheme_x_structure} table exists.
     * @return
     */
    private String selectTableIndex() {

        String sql = null;
        if (DbConnectionFactory.isMySql()) {
            sql =  MYSQL_SELECT_WORKFLOW_SCHEME_X_STRUCTURE_INDEX;
        } else if (DbConnectionFactory.isPostgres()) {
            sql =  POSTGRES_SELECT_WORKFLOW_SCHEME_X_STRUCTURE_INDEX;
        } else if (DbConnectionFactory.isMsSql()) {
            sql =  MSSQL_SELECT_WORKFLOW_SCHEME_X_STRUCTURE_INDEX;
        } else if (DbConnectionFactory.isOracle()) {
            sql =  ORACLE_SELECT_WORKFLOW_SCHEME_X_STRUCTURE_INDEX;
        }

        return sql;
    }

    /**
     * Creates the {@code workflow_scheme_x_structure} without the unique index.
     * @return
     */
    private String createTableIndex() {
        if (DbConnectionFactory.isPostgres()) {
            return POSTGRES_CREATE_WORKFLOW_SCHEME_X_STRUCTURE_INDEX;
        } else if (DbConnectionFactory.isOracle()) {
            return ORACLE_CREATE_WORKFLOW_SCHEME_X_STRUCTURE_INDEX;
        } else {
            return null;
        }
    }

    /**
     * Drops the {@code workflow_scheme_x_structure} unique index.
     * @return
     */
    private String dropTableIndex() {

        String sql = null;

        if (DbConnectionFactory.isMySql()) {
            sql =  MYSQL_DROP_WORKFLOW_SCHEME_X_STRUCTURE_INDEX;
        } else if (DbConnectionFactory.isPostgres()) {
            sql =  POSTGRES_DROP_WORKFLOW_SCHEME_X_STRUCTURE_INDEX;
        } else if (DbConnectionFactory.isMsSql()) {
            sql =  MSSQL_DROP_WORKFLOW_SCHEME_X_STRUCTURE_INDEX;
        } else if (DbConnectionFactory.isOracle()) {
            sql =  ORACLE_DROP_WORKFLOW_SCHEME_X_STRUCTURE_INDEX;
        }

        return sql;
    }

    /**
     * Query to drop the workflow_idx_action_step index.
     */
    private String dropWorkflowActionStepIdIndexQuery() {

        String sql = null;

        if (DbConnectionFactory.isMySql()) {
            sql = MYSQL_DROP_WORKFLOW_ACTION_STEP_INDEX;
        } else if (DbConnectionFactory.isPostgres()) {
            sql = POSTGRES_DROP_WORKFLOW_ACTION_STEP_INDEX;
        } else if (DbConnectionFactory.isMsSql()) {
            sql = MSSQL_DROP_WORKFLOW_ACTION_STEP_INDEX;
        } else if (DbConnectionFactory.isOracle()) {
            sql = ORACLE_DROP_WORKFLOW_ACTION_STEP_INDEX;
        }

        return sql;
    }

    /**
     * Query to remove the step_id NOT NULL constrain from the workflow_action table
     */
    private String dropWorkflowActionStepIdNotNullConstrainQuery() {

        String sql = null;

        if (DbConnectionFactory.isMySql()) {
            sql = MYSQL_DROP_WORKFLOW_ACTION_STEP_NOT_NULL;
        } else if (DbConnectionFactory.isPostgres()) {
            sql = POSTGRES_DROP_WORKFLOW_ACTION_STEP_NOT_NULL;
        } else if (DbConnectionFactory.isMsSql()) {
            sql = MSSQL_DROP_WORKFLOW_ACTION_STEP_NOT_NULL;
        } else if (DbConnectionFactory.isOracle()) {
            sql = ORACLE_DROP_WORKFLOW_ACTION_STEP_NOT_NULL;
        }

        return sql;
    }

    /**
     * Verifies if the {@code scheme_id} column already exists in the {@code workflow_action} table.
     * @return
     */
    private String findSchemeIdColumn() {

        String sql = null;

        if (DbConnectionFactory.isMySql()) {
            sql =  MYSQL_FIND_SCHEME_ID_COLUMN;
        } else if (DbConnectionFactory.isPostgres()) {
            sql =  POSTGRES_FIND_SCHEME_ID_COLUMN;
        } else if (DbConnectionFactory.isMsSql()) {
            sql =  MSSQL_FIND_SCHEME_ID_COLUMN;
        } else if (DbConnectionFactory.isOracle()) {
            sql =  ORACLE_FIND_SCHEME_ID_COLUMN;
        }

        return sql;
    }

    /**
     * Verifies if the {@code show_on} column already exists in the {@code workflow_action} table.
     * @return String
     */
    private String findShowOnColumn() {

        String sql = null;

        if (DbConnectionFactory.isMySql()) {
            sql =  MYSQL_FIND_SHOW_ON_OPTION_COLUMN;
        } else if (DbConnectionFactory.isPostgres()) {
            sql =  POSTGRES_FIND_SHOW_ON_OPTION_COLUMN;
        } else if (DbConnectionFactory.isMsSql()) {
            sql =  MSSQL_FIND_SHOW_ON_OPTION_COLUMN;
        } else if (DbConnectionFactory.isOracle()) {
            sql =  ORACLE_FIND_SHOW_ON_OPTION_COLUMN;
        }

        return sql;
    }

    /**
     * Create the PK constraint for the intermediate table.
     * @return
     */
    private String createIntermediateTablePk() {
        if (DbConnectionFactory.isMySql()) {
            return MYSQL_CREATE_INTERMEDIATE_TABLE_PK;
        } else if (DbConnectionFactory.isPostgres()) {
            return POSTGRES_CREATE_INTERMEDIATE_TABLE_PK;
        } else {
            return null;
        }
    }

    /**
     * Verifies if the intermediate {@code workflow_action_step} table already exists.
     * @return
     */
    private String findIntermediateTable() {

        String sql = null;

        if (DbConnectionFactory.isMySql()) {
            sql =  MYSQL_FIND_INTERMEDIATE_TABLE;
        } else if (DbConnectionFactory.isPostgres()) {
            sql =  POSTGRES_FIND_INTERMEDIATE_TABLE;
        } else if (DbConnectionFactory.isMsSql()) {
            sql =  MSSQL_FIND_INTERMEDIATE_TABLE;
        } else if (DbConnectionFactory.isOracle()) {
            sql =  ORACLE_FIND_INTERMEDIATE_TABLE;
        }

        return sql;
    }

    /**
     * Inserts the relationships between workflow actions and the workflow scheme where they live.
     * @return
     */
    private String updateSchemeIdsForActions() {

        String sql = null;

        if (DbConnectionFactory.isMySql()) {
            sql =  MYSQL_UPDATE_SCHEME_IDS_FOR_ACTIONS;
        } else if (DbConnectionFactory.isPostgres()) {
            sql =  POSTGRES_UPDATE_SCHEME_IDS_FOR_ACTIONS;
        } else if (DbConnectionFactory.isMsSql()) {
            sql =  MSSQL_UPDATE_SCHEME_IDS_FOR_ACTIONS;
        } else if (DbConnectionFactory.isOracle()) {
            sql =  ORACLE_UPDATE_SCHEME_IDS_FOR_ACTIONS;
        }

        return sql;
    }

    /**
     * Retrieves the relationships between workflow actions and the workflow scheme where they live.
     * @return
     */
    private String selectSchemeIdsForActions() {

        String sql = null;

        if (DbConnectionFactory.isMySql()) {
            sql =  MYSQL_SELECT_SCHEME_IDS_FOR_ACTIONS;
        } else if (DbConnectionFactory.isPostgres()) {
            sql =  POSTGRES_SELECT_SCHEME_IDS_FOR_ACTIONS;
        } else if (DbConnectionFactory.isMsSql()) {
            sql =  MSSQL_SELECT_SCHEME_IDS_FOR_ACTIONS;
        } else if (DbConnectionFactory.isOracle()) {
            sql =  ORACLE_SELECT_SCHEME_IDS_FOR_ACTIONS;
        }

        return sql;
    }

    /**
     * Inserts the relationships between workflow actions and the workflow steps where they can
     * be used in the new intermediate table.
     * @return
     */
    private String insertActionsAndSteps() {

        String sql = null;

        if (DbConnectionFactory.isMySql()) {
            sql =  MYSQL_INSERT_INTO_INTERMEDIATE_TABLE;
        } else if (DbConnectionFactory.isPostgres()) {
            sql =  POSTGRES_INSERT_INTO_INTERMEDIATE_TABLE;
        } else if (DbConnectionFactory.isMsSql()) {
            sql =  MSSQL_INSERT_INTO_INTERMEDIATE_TABLE;
        } else if (DbConnectionFactory.isOracle()) {
            sql =  ORACLE_INSERT_INTO_INTERMEDIATE_TABLE;
        }

        return sql;
    }

    /**
     * Retrieves the relationships between workflow actions and the workflow steps where they can
     * be used.
     * @return
     */
    private String selectActionsAndSteps() {

        String sql = null;

        if (DbConnectionFactory.isMySql()) {
            sql =  MYSQL_SELECT_ACTIONS_AND_STEPS;
        } else if (DbConnectionFactory.isPostgres()) {
            sql =  POSTGRES_SELECT_ACTIONS_AND_STEPS;
        } else if (DbConnectionFactory.isMsSql()) {
            sql =  MSSQL_SELECT_ACTIONS_AND_STEPS;
        } else if (DbConnectionFactory.isOracle()) {
            sql =  ORACLE_SELECT_ACTIONS_AND_STEPS;
        }

        return sql;
    }

    /**
     * Adds the new {@code scheme_id} column to the {@code workflow_action} table so that it is now
     * related directly with a workflow scheme.
     * @return
     */
    private String addSchemeIdColumn() {

        String sql = null;

        if (DbConnectionFactory.isMySql()) {
            sql =  MYSQL_ADD_SCHEME_ID_COLUMN;
        } else if (DbConnectionFactory.isPostgres()) {
            sql =  POSTGRES_ADD_SCHEME_ID_COLUMN;
        } else if (DbConnectionFactory.isMsSql()) {
            sql =  MSSQL_ADD_SCHEME_ID_COLUMN;
        } else if (DbConnectionFactory.isOracle()) {
            sql =  ORACLE_ADD_SCHEME_ID_COLUMN;
        }

        return sql;
    }

    /**
     * Adds the new {@code show_on} column to the {@code workflow_action} table so that it is now
     * multivalue instead of boolean.
     * @return
     */
    private String addShowOnColumn() {

        String sql = null;

        if (DbConnectionFactory.isMySql()) {
            sql =  MYSQL_ADD_SHOW_ON_OPTION_COLUMN;
        } else if (DbConnectionFactory.isPostgres()) {
            sql =  POSTGRES_ADD_SHOW_ON_OPTION_COLUMN;
        } else if (DbConnectionFactory.isMsSql()) {
            sql =  MSSQL_ADD_SHOW_ON_OPTION_COLUMN;
        } else if (DbConnectionFactory.isOracle()) {
            sql =  ORACLE_ADD_SHOW_ON_OPTION_COLUMN;
        }

        return sql;
    }

    /**
     * Creates the new intermediate table called {@code workflow_action_step}.
     * @return
     */
    private String createIntermediateTable() {

        String sql = StringUtils.EMPTY;

        if (DbConnectionFactory.isMySql()) {
            sql =  MYSQL_CREATE_INTERMEDIATE_TABLE;
        } else if (DbConnectionFactory.isPostgres()) {
            sql =  POSTGRES_CREATE_INTERMEDIATE_TABLE;
        } else if (DbConnectionFactory.isMsSql()) {
            sql =  MSSQL_CREATE_INTERMEDIATE_TABLE;
        } else if (DbConnectionFactory.isOracle()) {
            sql =  ORACLE_CREATE_INTERMEDIATE_TABLE;
        }

        return sql;
    }

}
