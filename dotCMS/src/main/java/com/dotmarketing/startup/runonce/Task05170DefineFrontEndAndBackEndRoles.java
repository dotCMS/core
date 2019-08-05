package com.dotmarketing.startup.runonce;

import static com.dotmarketing.business.Role.DOTCMS_BACK_END_USER;
import static com.dotmarketing.business.Role.DOTCMS_FRONT_END_USER;

import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.Params;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.google.common.primitives.Ints;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This task is meant to create a separation between front-end & back-end users. Basically the task
 * expects to find the two roles with the Keys 'LoggedIn Site User' and 'LoggedIn Site User' so they
 * can be rename to 'DOTCMS_BACK_END_USER' and 'DOTCMS_FRONT_END_USER' respectively. Additionally to
 * that any user with a layout receives the new back-end-role.
 */
public class Task05170DefineFrontEndAndBackEndRoles implements StartupTask {

    private static final String FIND_SYSTEM_ROLE = "SELECT id FROM cms_role WHERE role_key = 'System'";

    private static final String ADD_USER_ROLE = "INSERT INTO users_cms_roles (id, user_id, role_id) VALUES(?, ?, ?)";

    private static final String INSERT_CREATE_ROLE = "INSERT INTO cms_role(id, role_name, description, role_key, db_fqn, parent, edit_permissions, edit_users, edit_layouts, locked, system) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String FIND_OLD_BACKEND_ROLE = "SELECT id FROM cms_role WHERE role_key = 'CMS User'";

    private static final String FIND_OLD_FRONTEND_ROLE = "SELECT id FROM cms_role WHERE role_key = 'LoggedIn Site User'";

    private static final String FRONTEND_ROLE_SANITY_CHECK = "SELECT count(*) as c FROM cms_role r JOIN layouts_cms_roles lr ON r.id = lr.role_id  where r.role_key = 'LoggedIn Site User'";

    private static final String POSTGRES_RENAME_ROLE_KEY_FRONT_END_USER
            = "UPDATE cms_role SET role_key = 'DOTCMS_FRONT_END_USER' , edit_layouts = 'false' , system = 'true' WHERE role_key = 'LoggedIn Site User' ";

    private static final String POSTGRES_RENAME_ROLE_KEY_BACK_END_USER
            = "UPDATE cms_role SET role_key = 'DOTCMS_BACK_END_USER' WHERE role_key = 'CMS User' ";

    private static final String ORACLE_RENAME_ROLE_KEY_FRONT_END_USER
            = "UPDATE cms_role SET role_key = 'DOTCMS_FRONT_END_USER' , edit_layouts = 0, system = 1 WHERE role_key = 'LoggedIn Site User' ";

    private static final String ORACLE_RENAME_ROLE_KEY_BACK_END_USER
            = "UPDATE cms_role SET role_key = 'DOTCMS_BACK_END_USER' WHERE role_key = 'CMS User' ";

    private static final String MSSQL_RENAME_ROLE_KEY_FRONT_END_USER
            = "UPDATE cms_role SET role_key = 'DOTCMS_FRONT_END_USER' , edit_layouts = 0, system = 1 WHERE role_key = 'LoggedIn Site User' ";

    private static final String MSSQL_RENAME_ROLE_KEY_BACK_END_USER
            = "UPDATE cms_role SET role_key = 'DOTCMS_BACK_END_USER' WHERE role_key = 'CMS User' ";

    private static final String MYSQL_RENAME_ROLE_KEY_FRONT_END_USER
            = "UPDATE cms_role SET role_key = 'DOTCMS_FRONT_END_USER' , edit_layouts = 0, system = 1 WHERE role_key = 'LoggedIn Site User' ";

    private static final String MYSQL_RENAME_ROLE_KEY_BACK_END_USER
            = "UPDATE cms_role SET role_key = 'DOTCMS_BACK_END_USER' WHERE role_key = 'CMS User' ";

    //These statements are good to apply after having updated the role keys.
    private static final String FIND_NEW_BACKEND_ROLE = "SELECT id FROM cms_role WHERE role_key = 'DOTCMS_BACK_END_USER'";

    private static final String FIND_NEW_FRONTEND_ROLE = "SELECT id FROM cms_role WHERE role_key = 'DOTCMS_FRONT_END_USER'";


     static final String USERS_WITH_ASSIGNED_LAYOUT_BUT_NOT_BACKEND_ROLE =
            "select distinct(ur.user_id) from users_cms_roles ur \n"
                    + "join layouts_cms_roles l on ur.role_id = l.role_id  \n"
                    + "join cms_role r on r.id = l.role_id \n"
                    + "where ur.user_id not in (\n"
                    + "  select ur2.user_id from users_cms_roles ur2 join cms_role r2 on ur2.role_id = r2.id and r2.role_key = 'DOTCMS_BACK_END_USER'\n"
                    + ")";

    private final static String CMS_USER_LABEL = "CMS User";

    private final static String LOGGED_IN_SITE_USER_LABEL = "LoggedIn Site User";

    private final static String DB_FQN_MASK = "%s --> %s";

    @Override
    public boolean forceRun() {
        return true;
    }

    private void createBaseRolesIfAbsent(final DotConnect dotConnect) throws DotDataException {

        final String systemRoleId = findSystemRoleId(dotConnect);
        if (!UtilMethods.isSet(systemRoleId)) {
            throw new DotStateException("Dude I can't find the `System` Role.");
        }
        dotConnect.setSQL(FIND_OLD_BACKEND_ROLE);
        final boolean backendRoleNotFound = dotConnect.loadResults().isEmpty();
        dotConnect.setSQL(FIND_OLD_FRONTEND_ROLE);
        final boolean frontendRoleNotFound = dotConnect.loadResults().isEmpty();

        if (backendRoleNotFound) {
            insertCreateRole(dotConnect, CMS_USER_LABEL, systemRoleId);
            Logger.warn(this, "No back-end role under the key `"+CMS_USER_LABEL+"` was found. The Role will be inserted.");
        }

        if (frontendRoleNotFound) {
            insertCreateRole(dotConnect, LOGGED_IN_SITE_USER_LABEL, systemRoleId);
            Logger.warn(this, "No front-end role under the key `"+LOGGED_IN_SITE_USER_LABEL+"` was found. The Role will be inserted.");
        }

    }

    private void insertCreateRole(final DotConnect dotConnect, final String roleLabel,
            final String parentRoleId) throws DotDataException {
        final String uuid = UUIDUtil.uuid();
        dotConnect.setSQL(INSERT_CREATE_ROLE);
        dotConnect.addObject(UUIDUtil.uuid()); //id
        dotConnect.addObject(roleLabel);  //role_name
        dotConnect.addObject(roleLabel);  //description
        dotConnect.addObject(roleLabel);  //role_key
        dotConnect.addObject(String.format(DB_FQN_MASK, parentRoleId, uuid));  //db_fqn
        dotConnect.addObject(parentRoleId);  //parent
        dotConnect.addObject(true); //edit_permissions
        dotConnect.addObject(true); //edit_users
        dotConnect.addObject(true); //edit_layouts
        dotConnect.addObject(false); //locked
        dotConnect.addObject(true); //system
        dotConnect.loadResult();
    }

    private void performFrontendRoleSanityCheck(final DotConnect dotConnect) throws DotDataException {
        dotConnect
                .setSQL(FRONTEND_ROLE_SANITY_CHECK); // verifies the 'front-end-role' or its equivalent isn't already assigned layouts. if so.. task must die.
        final int count = dotConnect.getInt("c");
        if (count > 0) {
            throw new DotDataException(
                    String.format(
                            "The front-end Role defined as `LoggedIn Site User` has %d layouts associated. This must be corrected manually before this upgrade task can run.",
                            count)
            );
        }
    }

    private void renameOldFrontendRoles(final DotConnect dotConnect) throws DotDataException {
        if (DbConnectionFactory.isPostgres()) {
            dotConnect.setSQL(POSTGRES_RENAME_ROLE_KEY_FRONT_END_USER);
            dotConnect.loadResult();
        }

        if (DbConnectionFactory.isOracle()) {
            dotConnect.setSQL(ORACLE_RENAME_ROLE_KEY_FRONT_END_USER);
            dotConnect.loadResult();
        }

        if (DbConnectionFactory.isMsSql()) {
            dotConnect.setSQL(MSSQL_RENAME_ROLE_KEY_FRONT_END_USER);
            dotConnect.loadResult();
        }

        if (DbConnectionFactory.isMySql()) {
            dotConnect.setSQL(MYSQL_RENAME_ROLE_KEY_FRONT_END_USER);
            dotConnect.loadResult();
        }
    }

    private void renameOldBackendRoles(final DotConnect dotConnect) throws DotDataException {
        if (DbConnectionFactory.isPostgres()) {
            dotConnect.setSQL(POSTGRES_RENAME_ROLE_KEY_BACK_END_USER);
            dotConnect.loadResult();
        }

        if (DbConnectionFactory.isOracle()) {
            dotConnect.setSQL(ORACLE_RENAME_ROLE_KEY_BACK_END_USER);
            dotConnect.loadResult();
        }

        if (DbConnectionFactory.isMsSql()) {
            dotConnect.setSQL(MSSQL_RENAME_ROLE_KEY_BACK_END_USER);
            dotConnect.loadResult();
        }

        if (DbConnectionFactory.isMySql()) {
            dotConnect.setSQL(MYSQL_RENAME_ROLE_KEY_BACK_END_USER);
            dotConnect.loadResult();
        }
    }

    private List<Map> loadUsersWithLayoutButNotBackendRole(final DotConnect dotConnect)
            throws DotDataException {
        dotConnect.setSQL(USERS_WITH_ASSIGNED_LAYOUT_BUT_NOT_BACKEND_ROLE);
        return dotConnect.loadResults();
    }

    private String findSystemRoleId(final DotConnect dotConnect) {
        dotConnect.setSQL(FIND_SYSTEM_ROLE);
        return dotConnect.getString("id");
    }

    private String findNewBackendRoleId(final DotConnect dotConnect) {
        dotConnect.setSQL(FIND_NEW_BACKEND_ROLE);
        return dotConnect.getString("id");
    }

    private String findNewFrontendRoleId(final DotConnect dotConnect) {
        dotConnect.setSQL(FIND_NEW_FRONTEND_ROLE);
        return dotConnect.getString("id");
    }

    private boolean newBackendRoleExists(final DotConnect dotConnect){
        boolean found = false;
        try{
            found = UtilMethods.isSet(findNewBackendRoleId(dotConnect));
        }catch (Exception e){
           // Empty
        }
        return found;
    }

    private boolean newFrontendRoleExists(final DotConnect dotConnect){
        boolean found = false;
        try{
            found = UtilMethods.isSet(findNewFrontendRoleId(dotConnect));
        }catch (Exception e){
            // Empty
        }
        return found;
    }

    private void assignBackendRole(final List<Map> users, final DotConnect dotConnect)
            throws DotDataException, DotRuntimeException {

        final String backendRoleId = findNewBackendRoleId(dotConnect);
        final List<Params> paramsList = new ArrayList<>(users.size());
        for (final Map userData : users) {
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

        //This checks the front-end-role's already assigned to anybody with a layout.. which is an invalid state and required closer attention.
        performFrontendRoleSanityCheck(dotConnect);

        //This is an extra precaution in case a new starter without the expected roles is used.
        createBaseRolesIfAbsent(dotConnect);

        if (newBackendRoleExists(dotConnect)) {
            Logger.warn(this, "The New Back-end role "+DOTCMS_BACK_END_USER + " already exists on this database.");
        } else {
            renameOldBackendRoles(dotConnect);
        }

        if (newFrontendRoleExists(dotConnect)) {
            Logger.warn(this, "The New Front-end role "+DOTCMS_FRONT_END_USER + " already exists on this database.");
        } else {
            renameOldFrontendRoles(dotConnect);
        }

        // These must run once we have ensured the new Roles are now in place
        final List<Map> users = loadUsersWithLayoutButNotBackendRole(dotConnect);

        assignBackendRole(users, dotConnect);
    }


}
