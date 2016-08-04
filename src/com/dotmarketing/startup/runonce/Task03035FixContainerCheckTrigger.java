package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.beans.Inode;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;

public class Task03035FixContainerCheckTrigger extends AbstractJDBCStartupTask {

	String createTrigger = "drop trigger check_container_versions;\n" +
			"CREATE Trigger check_container_versions\n" +
			"ON containers\n" +
			"FOR DELETE AS\n" +
			"DECLARE @totalCount int\n" +
			"DECLARE @identifier varchar(36)\n" +
			"DECLARE container_cur_Deleted cursor LOCAL FAST_FORWARD for\n" +
			"Select identifier\n" +
			"from deleted\n" +
			"for Read Only\n" +
			"open container_cur_Deleted\n" +
			"fetch next from container_cur_Deleted into @identifier\n" +
			"while @@FETCH_STATUS <> -1\n" +
			"BEGIN\n" +
			"select @totalCount = count(*) from containers where identifier = @identifier\n" +
			"IF (@totalCount = 0)\n" +
			"BEGIN\n" +
			"DELETE from identifier where id = @identifier\n" +
			"END\n" +
			"fetch next from container_cur_Deleted into @identifier\n" +
			"END;\n";

    @Override
    public boolean forceRun() {
    	return DbConnectionFactory.isMsSql();
    }

    @Override
    public String getPostgresScript() {
        return "";
    }

    @Override
    public String getMySQLScript() {
    	return "";
    }

    @Override
    public String getOracleScript() {
    	return "";
    }

    @Override
    public String getMSSQLScript() {
    	return createTrigger;
    }

    @Override
    public String getH2Script() {
    	return "";
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }

}
