package com.dotmarketing.startup.runonce;

import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import java.sql.SQLException;

public class Task05170DefineFrontEndAndBackEndRoles implements StartupTask {

    private static final String FRONTEND_ROLE_SANITY_CHECK = "SELECT count(*) as c FROM cms_role r JOIN layouts_cms_roles lr ON r.id = lr.role_id  where r.role_key = 'LoggedIn Site User'";

    private static final String POSTGRES_RENAME_ROLE_KEY_FRONT_END_USER
     = "UPDATE cms_role SET role_key = 'DOTCMS_FRONT_END_USER' , edit_layouts = 'false' WHERE role_key = 'LoggedIn Site User' ";

    private static final String POSTGRES_RENAME_ROLE_KEY_BACK_END_USER
     = "UPDATE cms_role SET role_key = 'DOTCMS_BACK_END_USER' WHERE role_key = 'CMS User' ";


    private static final String ORACLE_RENAME_ROLE_KEY_FRONT_END_USER
            = "UPDATE cms_role SET role_key = 'DOTCMS_FRONT_END_USER' , edit_layouts = 0 WHERE role_key = 'LoggedIn Site User' ";

    private static final String ORACLE_RENAME_ROLE_KEY_BACK_END_USER
            = "UPDATE cms_role SET role_key = 'DOTCMS_BACK_END_USER' WHERE role_key = 'CMS User' ";


    private static final String MSSQL_RENAME_ROLE_KEY_FRONT_END_USER
            = "UPDATE cms_role SET role_key = 'DOTCMS_FRONT_END_USER' , edit_layouts = 0 WHERE role_key = 'LoggedIn Site User' ";

    private static final String MSSQL_RENAME_ROLE_KEY_BACK_END_USER
            = "UPDATE cms_role SET role_key = 'DOTCMS_BACK_END_USER' WHERE role_key = 'CMS User' ";


    private static final String MYSQL_RENAME_ROLE_KEY_FRONT_END_USER
            = "UPDATE cms_role SET role_key = 'DOTCMS_FRONT_END_USER' , edit_layouts = 0 WHERE role_key = 'LoggedIn Site User' ";

    private static final String MYSQL_RENAME_ROLE_KEY_BACK_END_USER
            = "UPDATE cms_role SET role_key = 'DOTCMS_BACK_END_USER' WHERE role_key = 'CMS User' ";

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    @CloseDBIfOpened
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        Logger.debug(this, "Upgrading Front-end and Back-End roles definition.");
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }

        final DotConnect dotConnect = new DotConnect();

        dotConnect.setSQL(FRONTEND_ROLE_SANITY_CHECK);
        final int count = dotConnect.getInt("c");
        if(count > 0){
           throw new DotDataException(
             String.format("The front-end Role defined as `LoggedIn Site User` has %d layouts associated. This must be corrected manually before this upgrade task can run.", count)
           );
        }

        if (DbConnectionFactory.isPostgres()) {
            dotConnect.setSQL(POSTGRES_RENAME_ROLE_KEY_FRONT_END_USER);
            dotConnect.loadResult();

            dotConnect.setSQL(POSTGRES_RENAME_ROLE_KEY_BACK_END_USER);
            dotConnect.loadResult();
        }

        if (DbConnectionFactory.isOracle()) {
            dotConnect.setSQL(ORACLE_RENAME_ROLE_KEY_FRONT_END_USER);
            dotConnect.loadResult();

            dotConnect.setSQL(ORACLE_RENAME_ROLE_KEY_BACK_END_USER);
            dotConnect.loadResult();
        }

        if (DbConnectionFactory.isMsSql()) {
            dotConnect.setSQL(MSSQL_RENAME_ROLE_KEY_FRONT_END_USER);
            dotConnect.loadResult();

            dotConnect.setSQL(MSSQL_RENAME_ROLE_KEY_BACK_END_USER);
            dotConnect.loadResult();
        }

        if (DbConnectionFactory.isMySql()) {
            dotConnect.setSQL(MYSQL_RENAME_ROLE_KEY_FRONT_END_USER);
            dotConnect.loadResult();

            dotConnect.setSQL(MYSQL_RENAME_ROLE_KEY_BACK_END_USER);
            dotConnect.loadResult();
        }


    }


}
