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
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This task is meant to create a separation between front-end & back-end users. Basically the task
 * expects to find the two roles with the Keys 'LoggedIn Site User' and 'LoggedIn Site User' so they
 * can be rename to 'DOTCMS_BACK_END_USER' and 'DOTCMS_FRONT_END_USER' respectively.
 * Additionally to that all users must receive the new 'DOTCMS_FRONT_END_USER'
 * any user with a layout OR API_KEY receives the new DOTCMS_BACK_END_USER role.
 */
public class Task05170DefineFrontEndAndBackEndRoles implements StartupTask {

    private  static final String BACKEND_USER_ROLE_NAME = "Back-end User";

    private  static final String FRONTEND_USER_ROLE_NAME = "Front-end User";

    private static final String FIND_SYSTEM_ROLE = "SELECT id FROM cms_role WHERE role_key = 'System'";

    private static final String ADD_USER_ROLE = "INSERT INTO users_cms_roles (id, user_id, role_id) VALUES(?, ?, ?)";

    private static final String INSERT_CREATE_ROLE = "INSERT INTO cms_role(id, role_name, description, role_key, db_fqn, parent, edit_permissions, edit_users, edit_layouts, locked, system) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String FIND_OLD_BACKEND_ROLE = "SELECT id FROM cms_role WHERE role_key = 'CMS User'";

    private static final String FIND_OLD_FRONTEND_ROLE = "SELECT id FROM cms_role WHERE role_key = 'LoggedIn Site User'";

    private static final String FRONTEND_ROLE_SANITY_CHECK = "SELECT count(*) as c FROM cms_role r JOIN layouts_cms_roles lr ON r.id = lr.role_id  where r.role_key = 'LoggedIn Site User'";

    private static final String POSTGRES_RENAME_ROLE_KEY_FRONT_END_USER
            = "UPDATE cms_role SET role_key = 'DOTCMS_FRONT_END_USER', role_name = 'Front-end User', edit_layouts = 'false', system = 'true',  locked = 'true'  WHERE role_key = 'LoggedIn Site User' ";

    private static final String POSTGRES_RENAME_ROLE_KEY_BACK_END_USER
            = "UPDATE cms_role SET role_key = 'DOTCMS_BACK_END_USER', role_name = 'Back-end User',  system = 'true', locked = 'true'  WHERE role_key = 'CMS User' ";

    private static final String ORACLE_RENAME_ROLE_KEY_FRONT_END_USER
            = "UPDATE cms_role SET role_key = 'DOTCMS_FRONT_END_USER', role_name = 'Front-end User',  edit_layouts = 0, system = 1, locked = 1  WHERE role_key = 'LoggedIn Site User' ";

    private static final String ORACLE_RENAME_ROLE_KEY_BACK_END_USER
            = "UPDATE cms_role SET role_key = 'DOTCMS_BACK_END_USER', role_name = 'Back-end User', system = 1, locked = 1  WHERE role_key = 'CMS User' ";

    private static final String MSSQL_RENAME_ROLE_KEY_FRONT_END_USER
            = "UPDATE cms_role SET role_key = 'DOTCMS_FRONT_END_USER', role_name = 'Front-end User',  edit_layouts = 0, system = 1, locked = 1 WHERE role_key = 'LoggedIn Site User' ";

    private static final String MSSQL_RENAME_ROLE_KEY_BACK_END_USER
            = "UPDATE cms_role SET role_key = 'DOTCMS_BACK_END_USER', role_name = 'Back-end User',  system = 1, locked = 1 WHERE role_key = 'CMS User' ";

    private static final String MYSQL_RENAME_ROLE_KEY_FRONT_END_USER
            = "UPDATE cms_role SET role_key = 'DOTCMS_FRONT_END_USER', role_name = 'Front-end User',  edit_layouts = 0, system = 1, locked = 1 WHERE role_key = 'LoggedIn Site User' ";

    private static final String MYSQL_RENAME_ROLE_KEY_BACK_END_USER
            = "UPDATE cms_role SET role_key = 'DOTCMS_BACK_END_USER', role_name = 'Back-end User',  system = 1, locked = 1 WHERE role_key = 'CMS User' ";

    //These statements are good to apply after having updated the role keys.
    private static final String FIND_NEW_BACKEND_ROLE = "SELECT id FROM cms_role WHERE role_key = 'DOTCMS_BACK_END_USER'";

    private static final String FIND_NEW_FRONTEND_ROLE = "SELECT id FROM cms_role WHERE role_key = 'DOTCMS_FRONT_END_USER'";

    private static final String UPDATE_ROLE_NAME  = " UPDATE cms_role SET role_name = ? WHERE id = ? ";

     //Anyone with layout OR an API Key gets added to the backend user role
     static final String USERS_WITH_ASSIGNED_LAYOUT_OR_API_KEY_BUT_NOT_BACKEND_ROLE =
             "select u.userid as user_id from user_ u \n"
             + "  join users_cms_roles ur on ur.user_id = u.userid\n"
             + "  join layouts_cms_roles l on ur.role_id = l.role_id  \n"
             + "  join cms_role r on r.id = l.role_id \n"
             + "   where not exists (\n"
             + "    select ur2.user_id from users_cms_roles ur2, cms_role r2 where ur2.role_id = r2.id and r2.role_key = 'DOTCMS_BACK_END_USER' and ur2.user_id = ur.user_id  \n"
             + "  ) union  (  \n"
             + "       select token_userid as user_id from  api_token_issued, user_ where api_token_issued.token_userid= user_.userid and not exists (\n"
             + "          select ur2.user_id from users_cms_roles ur2, cms_role r2 where ur2.role_id = r2.id and r2.role_key = 'DOTCMS_BACK_END_USER' and ur2.user_id = token_userid \n"
             + "       )\n"
             + "  )";

    //Everyone gets the front end user role
    private static final String USERS_WITHOUT_FRONTEND_ROLE =
            "select distinct(user_.userid) as user_id from user_\n"
            + "  where not exists ( \n"
            + "     select ur2.user_id from users_cms_roles ur2, cms_role r2 where ur2.role_id = r2.id \n"
            + "     and r2.role_key = 'DOTCMS_FRONT_END_USER' and ur2.user_id = user_.userid \n"
            + "  )";


    private final static String CMS_USER_LABEL = "CMS User";

    private final static String LOGGED_IN_SITE_USER_LABEL = "LoggedIn Site User";

    private final static String DB_FQN_MASK = "%s --> %s";

    @Override
    public boolean forceRun() {
        return true;
    }

    public static void createOldBackendRolesIfAbsent(final DotConnect dotConnect, final String systemRoleId) throws DotDataException {
        dotConnect.setSQL(FIND_OLD_BACKEND_ROLE);
        final boolean backendRoleNotFound = dotConnect.loadResults().isEmpty();
        if (backendRoleNotFound) {
            insertCreateRole(dotConnect, CMS_USER_LABEL, systemRoleId);
            Logger.warn(Task05170DefineFrontEndAndBackEndRoles.class, "No back-end role under the key `"+CMS_USER_LABEL+"` was found. The Role will be inserted.");
        }
    }

    public static void createOldFrontendRolesIfAbsent(final DotConnect dotConnect, final String systemRoleId) throws DotDataException {
        dotConnect.setSQL(FIND_OLD_FRONTEND_ROLE);
        final boolean frontendRoleNotFound = dotConnect.loadResults().isEmpty();
        if (frontendRoleNotFound) {
            insertCreateRole(dotConnect, LOGGED_IN_SITE_USER_LABEL, systemRoleId);
            Logger.warn(Task05170DefineFrontEndAndBackEndRoles.class, "No front-end role under the key `"+LOGGED_IN_SITE_USER_LABEL+"` was found. The Role will be inserted.");
        }
    }

    private static void insertCreateRole(final DotConnect dotConnect, final String roleLabel,
            final String parentRoleId) throws DotDataException {
        final String uuid = UUIDUtil.uuid();
        dotConnect.setSQL(INSERT_CREATE_ROLE);
        dotConnect.addObject(uuid); //id
        dotConnect.addObject(roleLabel);  //role_name
        dotConnect.addObject(roleLabel);  //description
        dotConnect.addObject(roleLabel);  //role_key
        dotConnect.addObject(String.format(DB_FQN_MASK, parentRoleId, uuid));  //db_fqn
        dotConnect.addObject(parentRoleId);  //parent
        dotConnect.addObject(false); //edit_permissions
        dotConnect.addObject(true); //edit_users
        dotConnect.addObject(false); //edit_layouts
        dotConnect.addObject(true); //locked
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

    private List<Map> loadUsersWithLayoutOrAPIKeyButNotBackendRole(final DotConnect dotConnect)
            throws DotDataException {
        dotConnect.setSQL(USERS_WITH_ASSIGNED_LAYOUT_OR_API_KEY_BUT_NOT_BACKEND_ROLE);
        return dotConnect.loadResults();
    }

    private List<Map> loadAllUsersWithoutFrontEndRole(final DotConnect dotConnect)
          throws DotDataException {
        dotConnect.setSQL(USERS_WITHOUT_FRONTEND_ROLE);
        return dotConnect.loadResults();
    }

    public static String findSystemRoleId(final DotConnect dotConnect) {
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

    private void updateRoleName(final DotConnect dotConnect, final String roleName, final String roleId) throws DotDataException{
        dotConnect.setSQL(UPDATE_ROLE_NAME);
        dotConnect.addParam(roleName);
        dotConnect.addParam(roleId);
        dotConnect.loadResult();
    }

    private boolean newBackendRoleExists(final DotConnect dotConnect){
        boolean found = false;
        try{
            final String backEndRoleId = findNewBackendRoleId(dotConnect);
            found = UtilMethods.isSet(backEndRoleId);
            if(found){
               updateRoleName(dotConnect,BACKEND_USER_ROLE_NAME, backEndRoleId);
            }

        }catch (Exception e){
           // Empty
        }
        return found;
    }

    private boolean newFrontendRoleExists(final DotConnect dotConnect){
        boolean found = false;
        try{
            final String frontEndRoleId = findNewFrontendRoleId(dotConnect);
            found = UtilMethods.isSet(frontEndRoleId);
            if(found){
                updateRoleName(dotConnect,FRONTEND_USER_ROLE_NAME, frontEndRoleId);
            }
        }catch (Exception e){
            // Empty
        }
        return found;
    }

    private int addRole(final List<Map> users, final String roleId ,final DotConnect dotConnect) throws DotDataException, DotRuntimeException{
        final List<Params> paramsList = new ArrayList<>(users.size());
        for (final Map userData : users) {
            final String id = UUIDGenerator.generateUuid();
            final String userId = userData.get("user_id").toString();

            final Params params = new Params(id, userId, roleId);
            Logger.debug(Task05170DefineFrontEndAndBackEndRoles.class, params::toString);
            paramsList.add(params);

        }
        final List<Integer> batchResult =
                Ints.asList(dotConnect.executeBatch(ADD_USER_ROLE, paramsList));

        return batchResult.stream().reduce(0, Integer::sum);
    }

    private void assignBackendRole(final List<Map> users, final DotConnect dotConnect)
            throws DotDataException, DotRuntimeException {

        final String backendRoleId = findNewBackendRoleId(dotConnect);
        final int rowsAffected = addRole(users, backendRoleId, dotConnect);
        Logger.info(this, "Batch rows user roles, inserted: " + rowsAffected);

    }

    private void assignFrontendRole(final List<Map> users, final DotConnect dotConnect)
            throws DotDataException, DotRuntimeException {

        final String frontendRoleId = findNewFrontendRoleId(dotConnect);
        final int rowsAffected = addRole(users, frontendRoleId, dotConnect);
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

        //This checks the front-end-role's already assigned to anybody with a layout.. which is an invalid state and requires closer attention.
        performFrontendRoleSanityCheck(dotConnect);

        final String systemRoleId = findSystemRoleId(dotConnect);
        if (!UtilMethods.isSet(systemRoleId)) {
            throw new DotStateException("Dude I can't find the `System` Role.");
        }

        if (newBackendRoleExists(dotConnect)) {
            //Update name
            Logger.warn(this, "The New Back-end role "+DOTCMS_BACK_END_USER + " already exists on this database.");
        } else {
            createOldBackendRolesIfAbsent(dotConnect, systemRoleId);
            renameOldBackendRoles(dotConnect);
        }

        if (newFrontendRoleExists(dotConnect)) {
            //Update name
            Logger.warn(this, "The New Front-end role "+DOTCMS_FRONT_END_USER + " already exists in this database.");
        } else {
            createOldFrontendRolesIfAbsent(dotConnect, systemRoleId);
            renameOldFrontendRoles(dotConnect);
        }

        // These statements must run once we have ensured the new Roles are now in place.
        final List<List<Map>> userCandidatesForBackendRolePartitionedList = Lists.partition(
           loadUsersWithLayoutOrAPIKeyButNotBackendRole(dotConnect), 200
        );

        for(final List<Map> userCandidatesForBackendRole: userCandidatesForBackendRolePartitionedList) {
            assignBackendRole(userCandidatesForBackendRole, dotConnect);
        }

        final List<List<Map>> userCandidatesForFrontendRolePartitionedList =  Lists.partition(
           loadAllUsersWithoutFrontEndRole(dotConnect),200
        );
        for(final List<Map> userCandidatesForFrontendRole: userCandidatesForFrontendRolePartitionedList){
           assignFrontendRole(userCandidatesForFrontendRole, dotConnect);
        }

    }


}
