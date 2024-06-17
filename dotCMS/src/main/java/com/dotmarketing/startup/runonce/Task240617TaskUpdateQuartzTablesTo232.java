package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

import java.sql.SQLException;

public class Task240617TaskUpdateQuartzTablesTo232 implements StartupTask {

	private static final String ADD_SCHED_NAME_COLUMN_TEMPLATE =
			"ALTER TABLE %s ADD COLUMN SCHED_NAME VARCHAR(120) NOT NULL DEFAULT 'DEFAULT'";
	private static final String UPDATE_SCHED_NAME_COLUMN_TEMPLATE =
			"UPDATE %s SET SCHED_NAME = 'DEFAULT'";
	private static final String[] TABLES = {
			"qrtz_job_details",
			"qrtz_triggers",
			"qrtz_simple_triggers",
			"qrtz_cron_triggers",
			"qrtz_blob_triggers",
			"qrtz_calendars",
			"qrtz_paused_trigger_grps",
			"qrtz_fired_triggers",
			"qrtz_scheduler_state",
			"qrtz_locks"
	};

	private static final String MODIFY_COLUMN_SIZE_TEMPLATE =
			"ALTER TABLE %s ALTER COLUMN %s TYPE VARCHAR(%d)";
	private static final String[] COLUMNS_TO_MODIFY = {
			"JOB_NAME", "TRIGGER_NAME", "JOB_GROUP", "TRIGGER_GROUP", "INSTANCE_NAME"
	};
	private static final int NEW_SIZE = 200;

	@Override
	public boolean forceRun() {
		return !hasSchedNameColumn("qrtz_job_details");
	}

	@Override
	public void executeUpgrade() {
		if (forceRun()) {
			final DotConnect dotConnect = new DotConnect();

			try {
				for (String table : TABLES) {
					Logger.info(this, "Adding the 'SCHED_NAME' column to the '" + table + "' table");
					dotConnect.executeStatement(String.format(ADD_SCHED_NAME_COLUMN_TEMPLATE, table));
					Logger.info(this, "Updating the 'SCHED_NAME' column in the '" + table + "' table to 'DEFAULT'");
					dotConnect.executeStatement(String.format(UPDATE_SCHED_NAME_COLUMN_TEMPLATE, table));
				}

				for (String column : COLUMNS_TO_MODIFY) {
					Logger.info(this, "Modifying column size for '" + column + "' in relevant tables");
					dotConnect.executeStatement(String.format(MODIFY_COLUMN_SIZE_TEMPLATE, "qrtz_job_details", column, NEW_SIZE));
					dotConnect.executeStatement(String.format(MODIFY_COLUMN_SIZE_TEMPLATE, "qrtz_triggers", column, NEW_SIZE));
					dotConnect.executeStatement(String.format(MODIFY_COLUMN_SIZE_TEMPLATE, "qrtz_simple_triggers", column, NEW_SIZE));
					dotConnect.executeStatement(String.format(MODIFY_COLUMN_SIZE_TEMPLATE, "qrtz_cron_triggers", column, NEW_SIZE));
					dotConnect.executeStatement(String.format(MODIFY_COLUMN_SIZE_TEMPLATE, "qrtz_blob_triggers", column, NEW_SIZE));
					dotConnect.executeStatement(String.format(MODIFY_COLUMN_SIZE_TEMPLATE, "qrtz_fired_triggers", column, NEW_SIZE));
					dotConnect.executeStatement(String.format(MODIFY_COLUMN_SIZE_TEMPLATE, "qrtz_calendars", column, NEW_SIZE));
					dotConnect.executeStatement(String.format(MODIFY_COLUMN_SIZE_TEMPLATE, "qrtz_paused_trigger_grps", column, NEW_SIZE));
					dotConnect.executeStatement(String.format(MODIFY_COLUMN_SIZE_TEMPLATE, "qrtz_scheduler_state", column, NEW_SIZE));
				}

				// Drop and re-create primary keys and foreign keys to include SCHED_NAME
				updateKeys(dotConnect);

			} catch (SQLException e) {
				throw new DotRuntimeException(e);
			}
		}
	}

	private void updateKeys(DotConnect dotConnect) throws SQLException {
		Logger.info(this, "Updating primary keys and foreign keys to include 'SCHED_NAME'");

		// Drop existing primary keys and foreign keys
		dropKeys(dotConnect);

		// Re-create primary keys including SCHED_NAME
		createPrimaryKeys(dotConnect);

		// Re-create foreign keys including SCHED_NAME
		createForeignKeys(dotConnect);
	}

	private void dropKeys(DotConnect dotConnect) throws SQLException {
		Logger.info(this, "Dropping existing primary keys and foreign keys");

		String[] dropForeignKeysStatements = {
				"ALTER TABLE qrtz_triggers DROP CONSTRAINT IF EXISTS qrtz_triggers_job_name_job_group_fkey",
				"ALTER TABLE qrtz_simple_triggers DROP CONSTRAINT IF EXISTS qrtz_simple_triggers_trigger_name_trigger_group_fkey",
				"ALTER TABLE qrtz_cron_triggers DROP CONSTRAINT IF EXISTS qrtz_cron_triggers_trigger_name_trigger_group_fkey",
				"ALTER TABLE qrtz_blob_triggers DROP CONSTRAINT IF EXISTS qrtz_blob_triggers_trigger_name_trigger_group_fkey",
				"ALTER TABLE qrtz_fired_triggers DROP CONSTRAINT IF EXISTS qrtz_fired_triggers_trigger_name_trigger_group_fkey"
		};

		for (String stmt : dropForeignKeysStatements) {
			dotConnect.executeStatement(stmt);
		}

		String[] dropPrimaryKeysStatements = {
				"ALTER TABLE qrtz_job_details DROP CONSTRAINT IF EXISTS qrtz_job_details_pkey",
				"ALTER TABLE qrtz_triggers DROP CONSTRAINT IF EXISTS qrtz_triggers_pkey",
				"ALTER TABLE qrtz_simple_triggers DROP CONSTRAINT IF EXISTS qrtz_simple_triggers_pkey",
				"ALTER TABLE qrtz_cron_triggers DROP CONSTRAINT IF EXISTS qrtz_cron_triggers_pkey",
				"ALTER TABLE qrtz_blob_triggers DROP CONSTRAINT IF EXISTS qrtz_blob_triggers_pkey",
				"ALTER TABLE qrtz_calendars DROP CONSTRAINT IF EXISTS qrtz_calendars_pkey",
				"ALTER TABLE qrtz_paused_trigger_grps DROP CONSTRAINT IF EXISTS qrtz_paused_trigger_grps_pkey",
				"ALTER TABLE qrtz_fired_triggers DROP CONSTRAINT IF EXISTS qrtz_fired_triggers_pkey",
				"ALTER TABLE qrtz_scheduler_state DROP CONSTRAINT IF EXISTS qrtz_scheduler_state_pkey",
				"ALTER TABLE qrtz_locks DROP CONSTRAINT IF EXISTS qrtz_locks_pkey"
		};

		for (String stmt : dropPrimaryKeysStatements) {
			dotConnect.executeStatement(stmt);
		}
	}

	private void createPrimaryKeys(DotConnect dotConnect) throws SQLException {
		Logger.info(this, "Creating new primary keys including 'SCHED_NAME'");

		String[] createPrimaryKeysStatements = {
				"ALTER TABLE qrtz_job_details ADD PRIMARY KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)",
				"ALTER TABLE qrtz_triggers ADD PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)",
				"ALTER TABLE qrtz_simple_triggers ADD PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)",
				"ALTER TABLE qrtz_cron_triggers ADD PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)",
				"ALTER TABLE qrtz_blob_triggers ADD PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)",
				"ALTER TABLE qrtz_calendars ADD PRIMARY KEY (SCHED_NAME, CALENDAR_NAME)",
				"ALTER TABLE qrtz_paused_trigger_grps ADD PRIMARY KEY (SCHED_NAME, TRIGGER_GROUP)",
				"ALTER TABLE qrtz_fired_triggers ADD PRIMARY KEY (SCHED_NAME, ENTRY_ID)",
				"ALTER TABLE qrtz_scheduler_state ADD PRIMARY KEY (SCHED_NAME, INSTANCE_NAME)",
				"ALTER TABLE qrtz_locks ADD PRIMARY KEY (SCHED_NAME, LOCK_NAME)"
		};

		for (String stmt : createPrimaryKeysStatements) {
			dotConnect.executeStatement(stmt);
		}
	}

	private void createForeignKeys(DotConnect dotConnect) throws SQLException {
		Logger.info(this, "Creating new foreign keys including 'SCHED_NAME'");

		String[] createForeignKeysStatements = {
				"ALTER TABLE qrtz_triggers ADD FOREIGN KEY (SCHED_NAME, JOB_NAME, JOB_GROUP) REFERENCES qrtz_job_details(SCHED_NAME, JOB_NAME, JOB_GROUP)",
				"ALTER TABLE qrtz_simple_triggers ADD FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) REFERENCES qrtz_triggers(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)",
				"ALTER TABLE qrtz_cron_triggers ADD FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) REFERENCES qrtz_triggers(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)",
				"ALTER TABLE qrtz_blob_triggers ADD FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) REFERENCES qrtz_triggers(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)",
				"ALTER TABLE qrtz_fired_triggers ADD FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) REFERENCES qrtz_triggers(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)"
		};

		for (String stmt : createForeignKeysStatements) {
			dotConnect.executeStatement(stmt);
		}
	}

	private boolean hasSchedNameColumn(String tableName) {
		final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();

		try {
			return databaseMetaData.hasColumn(tableName, "SCHED_NAME");
		} catch (SQLException e) {
			return false;
		}
	}
}