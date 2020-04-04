package com.dotmarketing.startup.runonce;

import com.dotmarketing.startup.AbstractJDBCStartupTask;
import java.util.List;

public class Task05205UpdateIndexNameLength extends AbstractJDBCStartupTask {

	@Override
	public String getMSSQLScript() {
		return "DECLARE @SQL VARCHAR(4000)\n"
                + "SET @SQL = 'ALTER TABLE dbo.Indicies DROP CONSTRAINT |ConstraintName| '\n"
                + "\n"
                + "SET @SQL = REPLACE(@SQL, '|ConstraintName|', ( SELECT   name\n"
                + "                                               FROM     sysobjects\n"
                + "                                               WHERE    xtype = 'PK'\n"
                + "                                                        AND parent_obj =        OBJECT_ID('Indicies')))\n"
                + "\n"
                + "EXEC (@SQL)\n"
                + "alter table indicies alter column index_name nvarchar(100) not null;\n"
		        + "alter table Indicies add primary key (index_name);";
	}

	@Override
	public String getMySQLScript() {
		return "alter table indicies modify index_name varchar(100);";
	}

	@Override
	public String getOracleScript() {
		return "alter table indicies modify index_name varchar2(100);";
	}

	@Override
	public String getPostgresScript() {
		return "alter table indicies alter column index_name type varchar(100);";
	}

	@Override
	protected List<String> getTablesToDropConstraints() {
		return null;
	}

	public boolean forceRun() {
		return true;
	}

    @Override
    public String getH2Script() {
        return "alter table indicies alter column index_name type varchar(100);";
    }

}
