package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.Base64;
import com.liferay.util.Encryptor;
import com.liferay.util.EncryptorException;
import io.vavr.control.Try;
import java.sql.SQLException;

public class Task05350AddDotSaltClusterColumn implements StartupTask {

    private static final String SELECT = "select cluster_id from dot_cluster";
    private static final String UPDATE = "update dot_cluster set cluster_salt = ? WHERE cluster_id = ? ";

    private static final String POSTGRES_SQL_ADD_DOT_CLUSTER_SALT_COLUMN = "alter table dot_cluster add column cluster_salt varchar(256);";
    private static final String MSSQL_ADD_DOT_CLUSTER_SALT_COLUMN = "alter table dot_cluster add cluster_salt varchar(256);";
    private static final String MYSQL_ADD_DOT_CLUSTER_SALT_COLUMN = "alter table dot_cluster add cluster_salt varchar(256);";
    private static final String ORACLE_ADD_DOT_CLUSTER_SALT_COLUMN = "alter table dot_cluster add cluster_salt varchar(256);";

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        Logger.debug(this, "Adding column `cluster_salt` to dot_cluster Table definition.");

        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }

        final DotConnect dotConnect = new DotConnect();
        if (DbConnectionFactory.isPostgres()) {
           addColumnPostgres(dotConnect);
        }

        if (DbConnectionFactory.isMsSql()) {
            addColumnMsSQL(dotConnect);
        }

        if (DbConnectionFactory.isMySql()) {
            addColumnMySQL(dotConnect);
        }

        if (DbConnectionFactory.isOracle()) {
            addColumnOracle(dotConnect);
        }

        Try.run(()-> insertDefaultSaltIfNecessary(dotConnect)).onFailure(DotRuntimeException::new);
    }

    private void addColumnPostgres(final DotConnect dotConnect) throws DotDataException {
        dotConnect.setSQL(POSTGRES_SQL_ADD_DOT_CLUSTER_SALT_COLUMN);
        dotConnect.loadResult();
    }

    private void addColumnMsSQL(final DotConnect dotConnect) throws DotDataException {
        dotConnect.setSQL(MSSQL_ADD_DOT_CLUSTER_SALT_COLUMN);
        dotConnect.loadResult();
    }

    private void addColumnMySQL(final DotConnect dotConnect) throws DotDataException {
        dotConnect.setSQL(MYSQL_ADD_DOT_CLUSTER_SALT_COLUMN);
        dotConnect.loadResult();
    }

    private void addColumnOracle(final DotConnect dotConnect) throws DotDataException {
        dotConnect.setSQL(ORACLE_ADD_DOT_CLUSTER_SALT_COLUMN);
        dotConnect.loadResult();
    }

    private void insertDefaultSaltIfNecessary(final DotConnect dotConnect)
            throws DotDataException, EncryptorException {
        dotConnect.setSQL(SELECT);
        final String clusterId = dotConnect.getString("cluster_id");
        if(UtilMethods.isSet(clusterId)){
            dotConnect.setSQL(UPDATE);
            dotConnect.addParam(Base64.objectToString(Encryptor.generateKey()));
            dotConnect.addParam(clusterId);
            dotConnect.loadResult();
        }
    }

}
