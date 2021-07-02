package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotcms.auth.providers.jwt.factories.ApiTokenSQL;
import com.dotmarketing.startup.AbstractJDBCStartupTask;

public class Task05060CreateApiTokensIssuedTable extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun() {
        return true;
    }

    ApiTokenSQL sql = ApiTokenSQL.getInstance();


    @Override
    public String getPostgresScript() {
        return sql.CREATE_TOKEN_TABLE_SCRIPT();

    }

    @Override
    public String getMySQLScript() {
        return sql.CREATE_TOKEN_TABLE_SCRIPT();
    }

    @Override
    public String getOracleScript() {
        return sql.CREATE_TOKEN_TABLE_SCRIPT().replaceAll(" text", " nclob");
    }

    @Override
    public String getMSSQLScript() {
        return sql.CREATE_TOKEN_TABLE_SCRIPT();
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        // TODO Auto-generated method stub
        return null;
    }

}
