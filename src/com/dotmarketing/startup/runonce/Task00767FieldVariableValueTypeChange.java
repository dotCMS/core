package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

public class Task00767FieldVariableValueTypeChange extends AbstractJDBCStartupTask {

	public boolean forceRun() {
		return true;
	}

	@Override
	public String getPostgresScript() {
		return "ALTER TABLE field_variable ALTER COLUMN variable_value TYPE text;";
	}

	@Override
	public String getMySQLScript() {
		return "ALTER TABLE field_variable MODIFY variable_value longtext;";
	}

	@Override
	public String getOracleScript() {
		return "alter table field_variable modify variable_value long;" +
		       "alter table field_variable modify variable_value nclob;";
	}

	@Override
	public String getMSSQLScript() {
		return "ALTER TABLE field_variable ALTER COLUMN variable_value text;";
	}

	@Override
	protected List<String> getTablesToDropConstraints() {		
		return null;
	}

}
