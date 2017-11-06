package com.dotmarketing.startup.runonce;

import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

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
public class Task04300UpdateWorkflowActionTable implements StartupTask {

    private static final String MYSQL_FIND_INTERMEDIATE_TABLE = "SELECT table_name FROM information_schema.tables WHERE table_name = 'workflow_action_step' LIMIT 1";
    private static final String POSTGRES_FIND_INTERMEDIATE_TABLE = MYSQL_FIND_INTERMEDIATE_TABLE;
    private static final String MSSQL_FIND_INTERMEDIATE_TABLE = "SELECT TOP 1 table_name FROM information_schema.tables WHERE table_name = 'workflow_action_step'";
    private static final String ORACLE_FIND_INTERMEDIATE_TABLE = "SELECT table_name FROM user_tables WHERE table_name = 'WORKFLOW_ACTION_STEP'";

    private static final String MYSQL_CREATE_INTERMEDIATE_TABLE = "CREATE TABLE workflow_action_step (action_id VARCHAR(36) NOT NULL, step_id VARCHAR(36) NOT NULL )";
    private static final String POSTGRES_CREATE_INTERMEDIATE_TABLE = MYSQL_CREATE_INTERMEDIATE_TABLE;
    private static final String MSSQL_CREATE_INTERMEDIATE_TABLE = "CREATE TABLE workflow_action_step ( action_id NVARCHAR(36) NOT NULL, step_id NVARCHAR(36) NOT NULL CONSTRAINT pk_workflow_action_step PRIMARY KEY NONCLUSTERED (action_id, step_id) )";
    private static final String ORACLE_CREATE_INTERMEDIATE_TABLE = "CREATE TABLE workflow_action_step ( action_id VARCHAR(36) NOT NULL, step_id VARCHAR(36) NOT NULL, CONSTRAINT pk_workflow_action_step PRIMARY KEY (action_id, step_id) )";

    private static final String MYSQL_CREATE_INTERMEDIATE_TABLE_PK = "ALTER TABLE workflow_action_step ADD CONSTRAINT pk_workflow_action_step PRIMARY KEY (action_id, step_id)";
    private static final String POSTGRES_CREATE_INTERMEDIATE_TABLE_PK = MYSQL_CREATE_INTERMEDIATE_TABLE_PK;

    private static final String MYSQL_FIND_SCHEME_ID_COLUMN = "SELECT column_name FROM information_schema.columns WHERE table_name = 'workflow_action' AND column_name = 'scheme_id'";
    private static final String POSTGRES_FIND_SCHEME_ID_COLUMN = MYSQL_FIND_SCHEME_ID_COLUMN;
    private static final String MSSQL_FIND_SCHEME_ID_COLUMN = MYSQL_FIND_SCHEME_ID_COLUMN;
    private static final String ORACLE_FIND_SCHEME_ID_COLUMN = "SELECT column_name FROM user_tab_columns WHERE table_name = 'WORKFLOW_ACTION' AND column_name = 'SCHEME_ID'";

    private static final String MYSQL_ADD_SCHEME_ID_COLUMN = "ALTER TABLE workflow_action ADD scheme_id VARCHAR(36) NOT NULL";
    private static final String POSTGRES_ADD_SCHEME_ID_COLUMN = MYSQL_ADD_SCHEME_ID_COLUMN;
    private static final String MSSQL_ADD_SCHEME_ID_COLUMN = "ALTER TABLE workflow_action ADD scheme_id NVARCHAR(36) NOT NULL";
    private static final String ORACLE_ADD_SCHEME_ID_COLUMN = MYSQL_ADD_SCHEME_ID_COLUMN;

    private static final String MYSQL_SELECT_ACTIONS_AND_STEPS = "SELECT wa.id action_id, ws.id step_id FROM workflow_step ws INNER JOIN workflow_action wa ON ws.id = wa.step_id";
    private static final String POSTGRES_SELECT_ACTIONS_AND_STEPS = MYSQL_SELECT_ACTIONS_AND_STEPS;
    private static final String MSSQL_SELECT_ACTIONS_AND_STEPS = MYSQL_SELECT_ACTIONS_AND_STEPS;
    private static final String ORACLE_SELECT_ACTIONS_AND_STEPS = MYSQL_SELECT_ACTIONS_AND_STEPS;

    private static final String MYSQL_INSERT_INTO_INTERMEDIATE_TABLE = "INSERT INTO workflow_action_step (action_id, step_id) VALUES (?, ?)";
    private static final String POSTGRES_INSERT_INTO_INTERMEDIATE_TABLE = MYSQL_INSERT_INTO_INTERMEDIATE_TABLE;
    private static final String MSSQL_INSERT_INTO_INTERMEDIATE_TABLE = MYSQL_INSERT_INTO_INTERMEDIATE_TABLE;
    private static final String ORACLE_INSERT_INTO_INTERMEDIATE_TABLE = MYSQL_INSERT_INTO_INTERMEDIATE_TABLE;

    private static final String MYSQL_SELECT_SCHEME_IDS_FOR_ACTIONS = "SELECT wa.id action_id, ws.scheme_id FROM workflow_action wa INNER JOIN workflow_step ws ON ws.id = wa.step_id";
    private static final String POSTGRES_SELECT_SCHEME_IDS_FOR_ACTIONS = MYSQL_SELECT_SCHEME_IDS_FOR_ACTIONS;
    private static final String MSSQL_SELECT_SCHEME_IDS_FOR_ACTIONS = MYSQL_SELECT_SCHEME_IDS_FOR_ACTIONS;
    private static final String ORACLE_SELECT_SCHEME_IDS_FOR_ACTIONS = MYSQL_SELECT_SCHEME_IDS_FOR_ACTIONS;

    private static final String MYSQL_UPDATE_SCHEME_IDS_FOR_ACTIONS = "UPDATE workflow_action SET scheme_id = ? WHERE id = ?";
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

    private static final String POSTGRES_CREATE_WORKFLOW_SCHEME_X_STRUCTURE_INDEX = "CREATE INDEX workflow_idx_scheme_structure_2 ON workflow_scheme_x_structure(structure_id)";
    private static final String ORACLE_CREATE_WORKFLOW_SCHEME_X_STRUCTURE_INDEX = "CREATE INDEX wk_idx_scheme_str_2 ON workflow_scheme_x_structure(structure_id)";

    @Override
    public boolean forceRun() {
        return Boolean.TRUE;
    }

    @Override
    public void executeUpgrade() throws DotDataException {
        final DotConnect dc = new DotConnect();
        if (DbConnectionFactory.isMsSql()) {
            try {
                dc.executeStatement("SET TRANSACTION ISOLATION LEVEL READ COMMITTED");
            } catch (SQLException e) {
                throw new DotRuntimeException(
                        "Transaction isolation level could not be set.", e);
            }
        }

        Logger.info(this, "Creating intermediate 'workflow_action_step' table.");
        dc.setSQL(findIntermediateTable());
        final List<Map<String, Object>> intermediateTable = dc.loadObjectResults();
        if (intermediateTable.isEmpty()) {
            try {
                dc.executeStatement(createIntermediateTable());
                // The SQL Server and Oracle table definition already include de PK creation
                if (DbConnectionFactory.isMySql() || DbConnectionFactory.isPostgres()) {
                    dc.executeStatement(createIntermediateTablePk());
                }
            } catch (SQLException e) {
                throw new DotRuntimeException(
                        "The 'workflow_action_step' table could not be created.", e);
            }
        }

        Logger.info(this, "Adding new 'scheme_id' column to 'workflow_action' table.");
        dc.setSQL(findSchemeIdColumn());
        final List<Map<String, Object>> newColumn = dc.loadObjectResults();
        if (newColumn.isEmpty()) {
            try {
                dc.executeStatement(addSchemeIdColumn());
            } catch (SQLException e) {
                throw new DotRuntimeException("The 'scheme_id' column could not be created.", e);
            }
        }

        Logger.info(this, "Adding data to 'workflow_action_step' table.");
        dc.setSQL(selectActionsAndSteps());
        final List<Map<String, Object>> actionsAndSteps = dc.loadObjectResults();
        actionsAndSteps.stream().forEach(row -> {
            dc.setSQL(insertActionsAndSteps());
            dc.addParam(row.get("action_id"));
            dc.addParam(row.get("step_id"));
            try {
                dc.loadResult();
            } catch (DotDataException e) {
                throw new DotRuntimeException(
                        "An error occurred when adding data to the 'workflow_action_step' table.",
                        e);
            }
        });

        Logger.info(this, "Associating Workflow Actions to Workflow Schemes.");
        dc.setSQL(selectSchemeIdsForActions());
        final List<Map<String, Object>> schemeIds = dc.loadObjectResults();
        schemeIds.stream().forEach(row -> {
            dc.setSQL(updateSchemeIdsForActions());
            dc.addParam(row.get("scheme_id").toString());
            dc.addParam(row.get("action_id").toString());
            try {
                dc.loadResult();
            } catch (DotDataException e) {
                throw new DotRuntimeException(
                        "An error occurred when associating Workflow Actions to Workflow Schemes.",
                        e);
            }
        });

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

    /**
     * Verifies if the unique index in the {@code workflow_scheme_x_structure} table exists.
     * @return
     */
    private String selectTableIndex() {
        if (DbConnectionFactory.isMySql()) {
            return MYSQL_SELECT_WORKFLOW_SCHEME_X_STRUCTURE_INDEX;
        } else if (DbConnectionFactory.isPostgres()) {
            return POSTGRES_SELECT_WORKFLOW_SCHEME_X_STRUCTURE_INDEX;
        } else if (DbConnectionFactory.isMsSql()) {
            return MSSQL_SELECT_WORKFLOW_SCHEME_X_STRUCTURE_INDEX;
        } else if (DbConnectionFactory.isOracle()) {
            return ORACLE_SELECT_WORKFLOW_SCHEME_X_STRUCTURE_INDEX;
        } else {
            return null;
        }
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
        if (DbConnectionFactory.isMySql()) {
            return MYSQL_DROP_WORKFLOW_SCHEME_X_STRUCTURE_INDEX;
        } else if (DbConnectionFactory.isPostgres()) {
            return POSTGRES_DROP_WORKFLOW_SCHEME_X_STRUCTURE_INDEX;
        } else if (DbConnectionFactory.isMsSql()) {
            return MSSQL_DROP_WORKFLOW_SCHEME_X_STRUCTURE_INDEX;
        } else if (DbConnectionFactory.isOracle()) {
            return ORACLE_DROP_WORKFLOW_SCHEME_X_STRUCTURE_INDEX;
        } else {
            return null;
        }
    }

    /**
     * Verifies if the {@code scheme_id} column already exists in the {@code workflow_action} table.
     * @return
     */
    private String findSchemeIdColumn() {
        if (DbConnectionFactory.isMySql()) {
            return MYSQL_FIND_SCHEME_ID_COLUMN;
        } else if (DbConnectionFactory.isPostgres()) {
            return POSTGRES_FIND_SCHEME_ID_COLUMN;
        } else if (DbConnectionFactory.isMsSql()) {
            return MSSQL_FIND_SCHEME_ID_COLUMN;
        } else if (DbConnectionFactory.isOracle()) {
            return ORACLE_FIND_SCHEME_ID_COLUMN;
        } else {
            return null;
        }
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
        if (DbConnectionFactory.isMySql()) {
            return MYSQL_FIND_INTERMEDIATE_TABLE;
        } else if (DbConnectionFactory.isPostgres()) {
            return POSTGRES_FIND_INTERMEDIATE_TABLE;
        } else if (DbConnectionFactory.isMsSql()) {
            return MSSQL_FIND_INTERMEDIATE_TABLE;
        } else if (DbConnectionFactory.isOracle()) {
            return ORACLE_FIND_INTERMEDIATE_TABLE;
        } else {
            return null;
        }
    }

    /**
     * Inserts the relationships between workflow actions and the workflow scheme where they live.
     * @return
     */
    private String updateSchemeIdsForActions() {
        if (DbConnectionFactory.isMySql()) {
            return MYSQL_UPDATE_SCHEME_IDS_FOR_ACTIONS;
        } else if (DbConnectionFactory.isPostgres()) {
            return POSTGRES_UPDATE_SCHEME_IDS_FOR_ACTIONS;
        } else if (DbConnectionFactory.isMsSql()) {
            return MSSQL_UPDATE_SCHEME_IDS_FOR_ACTIONS;
        } else if (DbConnectionFactory.isOracle()) {
            return ORACLE_UPDATE_SCHEME_IDS_FOR_ACTIONS;
        } else {
            return null;
        }
    }

    /**
     * Retrieves the relationships between workflow actions and the workflow scheme where they live.
     * @return
     */
    private String selectSchemeIdsForActions() {
        if (DbConnectionFactory.isMySql()) {
            return MYSQL_SELECT_SCHEME_IDS_FOR_ACTIONS;
        } else if (DbConnectionFactory.isPostgres()) {
            return POSTGRES_SELECT_SCHEME_IDS_FOR_ACTIONS;
        } else if (DbConnectionFactory.isMsSql()) {
            return MSSQL_SELECT_SCHEME_IDS_FOR_ACTIONS;
        } else if (DbConnectionFactory.isOracle()) {
            return ORACLE_SELECT_SCHEME_IDS_FOR_ACTIONS;
        } else {
            return null;
        }
    }

    /**
     * Inserts the relationships between workflow actions and the workflow steps where they can
     * be used in the new intermediate table.
     * @return
     */
    private String insertActionsAndSteps() {
        if (DbConnectionFactory.isMySql()) {
            return MYSQL_INSERT_INTO_INTERMEDIATE_TABLE;
        } else if (DbConnectionFactory.isPostgres()) {
            return POSTGRES_INSERT_INTO_INTERMEDIATE_TABLE;
        } else if (DbConnectionFactory.isMsSql()) {
            return MSSQL_INSERT_INTO_INTERMEDIATE_TABLE;
        } else if (DbConnectionFactory.isOracle()) {
            return ORACLE_INSERT_INTO_INTERMEDIATE_TABLE;
        } else {
            return null;
        }
    }

    /**
     * Retrieves the relationships between workflow actions and the workflow steps where they can
     * be used.
     * @return
     */
    private String selectActionsAndSteps() {
        if (DbConnectionFactory.isMySql()) {
            return MYSQL_SELECT_ACTIONS_AND_STEPS;
        } else if (DbConnectionFactory.isPostgres()) {
            return POSTGRES_SELECT_ACTIONS_AND_STEPS;
        } else if (DbConnectionFactory.isMsSql()) {
            return MSSQL_SELECT_ACTIONS_AND_STEPS;
        } else if (DbConnectionFactory.isOracle()) {
            return ORACLE_SELECT_ACTIONS_AND_STEPS;
        } else {
            return null;
        }
    }

    /**
     * Adds the new {@code scheme_id} column to the {@code workflow_action} table so that it is now
     * related directly with a workflow scheme.
     * @return
     */
    private String addSchemeIdColumn() {
        if (DbConnectionFactory.isMySql()) {
            return MYSQL_ADD_SCHEME_ID_COLUMN;
        } else if (DbConnectionFactory.isPostgres()) {
            return POSTGRES_ADD_SCHEME_ID_COLUMN;
        } else if (DbConnectionFactory.isMsSql()) {
            return MSSQL_ADD_SCHEME_ID_COLUMN;
        } else if (DbConnectionFactory.isOracle()) {
            return ORACLE_ADD_SCHEME_ID_COLUMN;
        } else {
            return null;
        }
    }

    /**
     * Creates the new intermediate table called {@code workflow_action_step}.
     * @return
     */
    private String createIntermediateTable() {
        if (DbConnectionFactory.isMySql()) {
            return MYSQL_CREATE_INTERMEDIATE_TABLE;
        } else if (DbConnectionFactory.isPostgres()) {
            return POSTGRES_CREATE_INTERMEDIATE_TABLE;
        } else if (DbConnectionFactory.isMsSql()) {
            return MSSQL_CREATE_INTERMEDIATE_TABLE;
        } else if (DbConnectionFactory.isOracle()) {
            return ORACLE_CREATE_INTERMEDIATE_TABLE;
        } else {
            return StringUtils.EMPTY;
        }
    }

}
