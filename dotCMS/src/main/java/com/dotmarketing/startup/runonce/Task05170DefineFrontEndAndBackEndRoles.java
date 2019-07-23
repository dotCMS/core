package com.dotmarketing.startup.runonce;

import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.Params;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.google.common.primitives.Ints;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    //These statements are good to apply after having updated the role keys.
    private static final String FIND_BACKEND_ROLE = "SELECT id FROM cms_role WHERE role_key = 'DOTCMS_BACK_END_USER'";

    private static final String ADD_USER_ROLE = "INSERT INTO users_cms_roles (id, user_id, role_id) VALUES(?, ?, ?)";

    private static final String USERS_WITH_ASSIGNED_LAYOUT_BUT_NOT_BACKEND_ROLE = "select distinct(ur.user_id) from users_cms_roles ur \n"
            + "join layouts_cms_roles l on ur.role_id = l.role_id  \n"
            + "join cms_role r on r.id = l.role_id \n"
            + "where ur.user_id not in (\n"
            + "  select ur2.user_id from users_cms_roles ur2 join cms_role r2 on ur2.role_id = r2.id and r2.role_key = 'DOTCMS_BACK_END_USER'\n"
            + ")";

    @Override
    public boolean forceRun() {
        return true;
    }

    private List<Map> loadUsersWithLayoutButNotBackendRole(final DotConnect dotConnect)
            throws DotDataException {
        dotConnect.setSQL(USERS_WITH_ASSIGNED_LAYOUT_BUT_NOT_BACKEND_ROLE);
        return dotConnect.loadResults();
    }

    private String findBackendRoleId(final DotConnect dotConnect){
        dotConnect.setSQL(FIND_BACKEND_ROLE);
        return dotConnect.getString("id");
    }

    @CloseDBIfOpened
    private void addBackendRole(final List<Map> users, final DotConnect dotConnect) throws DotDataException, DotRuntimeException {

        final String backendRoleId = findBackendRoleId(dotConnect);
        final List <Params> paramsList = new ArrayList<>(users.size());
        for (final Map userData:users) {
            final String id = UUIDGenerator.generateUuid();
            final String userId = userData.get("user_id").toString();

            final Params params = new Params(id, userId, backendRoleId);
            Logger.debug(Task05170DefineFrontEndAndBackEndRoles.class, params::toString);
            paramsList.add(params);

        }

        final List<Integer> batchResult =
                Ints.asList(dotConnect.executeBatch(ADD_USER_ROLE, paramsList));

        final int rowsAffected = batchResult.stream().reduce(0, Integer::sum);
        Logger.info(this, "Batch rows user roles, inserted: " + rowsAffected);

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

        final List<Map> users = loadUsersWithLayoutButNotBackendRole(dotConnect);

        addBackendRole(users, dotConnect);

    }


}
