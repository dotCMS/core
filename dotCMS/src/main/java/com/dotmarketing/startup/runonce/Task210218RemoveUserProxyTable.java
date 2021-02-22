package com.dotmarketing.startup.runonce;

import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Task used to migrate data from user_proxy table to user_ and drop user_proxy
 */
public class Task210218RemoveUserProxyTable implements StartupTask {

    private static final ObjectMapper mapper = DotObjectMapperProvider.getInstance()
            .getDefaultObjectMapper();

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {

        DotConnect dotConnect = new DotConnect();
        // retrieves existing additional info from user_proxy table
        final List<Map<String, Object>> additionalInfoMaps = dotConnect.setSQL(
                "select * from user_proxy").loadObjectResults();

        if (null != additionalInfoMaps) {

            try {
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
                            .loadResult();
                }

            } catch (JsonProcessingException e) {
                throw new DotRuntimeException(e);
            }

            //remove user_proxy table
            try {

                dotConnect = new DotConnect();

                if (DbConnectionFactory.isOracle()){
                    dotConnect.setSQL("SELECT COUNT(*) as exist FROM user_tables WHERE table_name='USER_PROXY'");
                    BigDecimal existTable = (BigDecimal) dotConnect.loadObjectResults().get(0).get("exist");
                    if(existTable.longValue() > 0) {
                        new DotConnect().executeStatement("drop table USER_PROXY");
                    }
                } else{
                    new DotConnect().executeStatement("drop table if exists USER_PROXY");
                }
            } catch (SQLException e) {
                throw new DotRuntimeException(e);
            }
        }
    }

    @Override
    public boolean forceRun() {
        try {
            return new DotDatabaseMetaData().tableExists(
                    DbConnectionFactory.getConnection(), DbConnectionFactory.isOracle()? "USER_PROXY": "user_proxy");
        } catch (SQLException e) {

            return Boolean.FALSE;
        }
    }
}
