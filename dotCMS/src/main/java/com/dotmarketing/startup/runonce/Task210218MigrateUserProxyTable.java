package com.dotmarketing.startup.runonce;

import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Task used to migrate data from user_proxy table to user_ as json objects
 */
public class Task210218MigrateUserProxyTable implements StartupTask {

    private static final ObjectMapper mapper = DotObjectMapperProvider.getInstance()
            .getDefaultObjectMapper();

    private static final String POSTGRES_SCRIPT = "alter table user_ add additional_info JSONB NULL;";

    private static final String MYSQL_SCRIPT = "alter table user_ add additional_info text NULL;";

    private static final String ORACLE_SCRIPT = "alter table user_ add additional_info NCLOB NULL;";

    private static final String MSSQL_SCRIPT = "SET TRANSACTION ISOLATION LEVEL READ COMMITTED;\n"
            + "alter table user_ add additional_info NVARCHAR(MAX) NULL;";

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {

        try{
            migrateDataFromUserProxyToUser();
        } catch(Exception e){
            Logger.error(this, "Unable to execute SQL upgrade", e);
            throw e;
        }
    }

    /**
     * Creates the `additional_info` column in the `user_` table and migrates existing data from `user_proxy`
     * to `user_`. The migrated data will be stored in the new column
     * @throws DotDataException
     */
    private void migrateDataFromUserProxyToUser() throws DotDataException {
        DotConnect dotConnect = new DotConnect();
        // retrieves existing additional info from user_proxy table
        final List<Map<String, Object>> additionalInfoMaps = dotConnect.setSQL(
                "select * from user_proxy").loadObjectResults();

        dotConnect = new DotConnect();

        Connection connection;
        try {
            connection = DbConnectionFactory.getDataSource().getConnection();

            if (DbConnectionFactory.isPostgres()){
                dotConnect.executeStatement(POSTGRES_SCRIPT, connection);
            }else if (DbConnectionFactory.isMySql()){
                dotConnect.executeStatement(MYSQL_SCRIPT, connection);
            }else if (DbConnectionFactory.isOracle()){
                dotConnect.executeStatement(ORACLE_SCRIPT, connection);
            }else{
                dotConnect.executeStatement(MSSQL_SCRIPT, connection);
            }

            if (null != additionalInfoMaps) {

                //migrates info from proxy_user table to user_
                for (final Map<String, Object> additionalInfo : additionalInfoMaps) {
                    dotConnect = new DotConnect();
                    final String userId = (String) additionalInfo.get("user_id");
                    additionalInfo.remove("user_id");
                    additionalInfo.remove("inode");
                    dotConnect.setSQL(
                            "update user_ set additional_info=" + (
                                    DbConnectionFactory.isPostgres() ? "? ::jsonb" : "?")
                                    + " where userid = ? ")
                            .addParam(mapper.writeValueAsString(additionalInfo)).addParam(userId)
                            .loadResult(connection);
                }
            }

        } catch (JsonProcessingException | SQLException exception) {
            throw new DotDataException(exception);
        }

        Logger.info(this, "additional_info column created and user_proxy data migrated");
    }

    @Override
    public boolean forceRun() {
        try {
            final DotDatabaseMetaData dbMetadata = new DotDatabaseMetaData();
            return !dbMetadata.hasColumn("user_", "additional_info") && dbMetadata.tableExists(
                    DbConnectionFactory.getConnection(), DbConnectionFactory.isOracle()? "USER_PROXY": "user_proxy");

        } catch (SQLException e) {

            return Boolean.FALSE;
        }
    }
}
