package com.dotmarketing.db;

import javax.sql.DataSource;

public class DockerSecretDatasourceStrategy implements DotDatasourceStrategy {

    @Override
    public DataSource getDatasource() {
        return null;
    }
}
