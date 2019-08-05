package com.dotmarketing.startup.runonce;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.PortletDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.DbType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task05170DefineFrontEndAndBackEndRolesTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void Test_Upgrade_On_Old_Backend_And_Frontend_Roles_Expect_New_Role_Keys()
            throws DotDataException {
        final DbType dbType = DbType.getDbType(DbConnectionFactory.getDBType());

        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
        final DotConnect dotConnect = new DotConnect();

        dotConnect
                .setSQL(" UPDATE cms_role SET role_key = 'LoggedIn Site User' WHERE role_key = 'DOTCMS_BACK_END_USER' ");
        dotConnect.loadResult();

        dotConnect
                .setSQL(" UPDATE cms_role SET role_key = 'CMS User' WHERE role_key = 'DOTCMS_FRONT_END_USER' ");
        dotConnect.loadResult();

        try {
            final Task05170DefineFrontEndAndBackEndRoles task05170DefineFrontEndAndBackEndRoles =
                    new Task05170DefineFrontEndAndBackEndRoles();

            assertTrue(task05170DefineFrontEndAndBackEndRoles.forceRun());
            task05170DefineFrontEndAndBackEndRoles.executeUpgrade();

            dotConnect
                    .setSQL(" SELECT count(*) as x FROM cms_role  WHERE role_key = 'DOTCMS_BACK_END_USER' ");
            assertEquals("Task should have created the new roles", dotConnect.getInt("x"), 1);

            dotConnect
                    .setSQL(" SELECT count(*) as x FROM cms_role  WHERE role_key = 'DOTCMS_FRONT_END_USER' ");
            assertEquals("Task should have created the new roles", dotConnect.getInt("x"), 1);

            dotConnect
                    .setSQL(" SELECT count(*) as x FROM cms_role  WHERE role_key = 'LoggedIn Site User' ");
            assertEquals("Task should have created the new roles", dotConnect.getInt("x"), 0);

            dotConnect.setSQL(" SELECT count(*) as x FROM cms_role  WHERE role_key = 'CMS User' ");
            assertEquals("Task should have created the new roles", dotConnect.getInt("x"), 0);


        } catch (Exception e) {
            final String errMessage =
                    "Could not execute upgrade task 05170 : " + dbType + " Err: " + e.toString();
            Logger.error(getClass(), errMessage, e);
            Assert.fail(errMessage);
        }
    }


    @Test
    public void Test_Upgrade_On_None_Existing_Roles_Expect_New_Role_Keys()
            throws DotDataException {
        final DbType dbType = DbType.getDbType(DbConnectionFactory.getDBType());

        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
        final DotConnect dotConnect = new DotConnect();

        //Get rid of any traces of the roles
        dotConnect
                .setSQL(String
                        .format(" UPDATE cms_role SET role_key = 'AnyRoleKey-%d' WHERE role_key = 'DOTCMS_BACK_END_USER' ",
                                System.currentTimeMillis()));
        dotConnect.loadResult();

        dotConnect
                .setSQL(String
                        .format(" UPDATE cms_role SET role_key = 'AnyRoleKey-%d' WHERE role_key = 'DOTCMS_FRONT_END_USER' ",
                                System.currentTimeMillis()));
        dotConnect.loadResult();

        dotConnect
                .setSQL(String
                        .format(" UPDATE cms_role SET role_key = 'Any-Role-Key-%d' WHERE role_key = 'LoggedIn Site User' ",
                                System.currentTimeMillis()));
        dotConnect.loadResult();

        dotConnect
                .setSQL(String
                        .format(" UPDATE cms_role SET role_key = 'Any-Role-Key-%d' WHERE role_key = 'CMS User' ",
                                System.currentTimeMillis()));
        dotConnect.loadResult();

        try {
            final Task05170DefineFrontEndAndBackEndRoles task05170DefineFrontEndAndBackEndRoles =
                    new Task05170DefineFrontEndAndBackEndRoles();

            task05170DefineFrontEndAndBackEndRoles.executeUpgrade();

            //always make sure the new Roles are always created

            dotConnect
                    .setSQL(" SELECT count(*) as x FROM cms_role  WHERE role_key = 'DOTCMS_BACK_END_USER' ");
            assertEquals("Task should have created the new roles", dotConnect.getInt("x"), 1);

            dotConnect
                    .setSQL(" SELECT count(*) as x FROM cms_role  WHERE role_key = 'DOTCMS_FRONT_END_USER' ");
            assertEquals("Task should have created the new roles", dotConnect.getInt("x"), 1);

            dotConnect
                    .setSQL(" SELECT count(*) as x FROM cms_role  WHERE role_key = 'LoggedIn Site User' ");
            assertEquals("Task should have created the new roles", dotConnect.getInt("x"), 0);

            dotConnect.setSQL(" SELECT count(*) as x FROM cms_role  WHERE role_key = 'CMS User' ");
            assertEquals("Task should have created the new roles", dotConnect.getInt("x"), 0);

        } catch (Exception e) {
            final String errMessage =
                    "Could not execute upgrade task 05170 : " + dbType + " Err: " + e.toString();
            Logger.error(getClass(), errMessage, e);
            Assert.fail(errMessage);
        }
    }


    @Test
    public void Test_Create_User_Then_Assign_Then_Assign_Layouts_Expect_BackendRole_Added()
            throws DotDataException {

        final DotConnect dotConnect = new DotConnect();
        final PortletDataGen portletDataGen = new PortletDataGen();
        final Portlet portlet1 = portletDataGen.nextPersisted();
        final Portlet portlet2 = portletDataGen.nextPersisted();
        final Portlet portlet3 = portletDataGen.nextPersisted();

        final LayoutDataGen layoutDataGen = new LayoutDataGen();
        final Layout layout1 = layoutDataGen
                .portletIds(portlet1.getPortletId(), portlet2.getPortletId(),
                        portlet3.getPortletId()).nextPersisted();

        final RoleDataGen roleDataGen1 = new RoleDataGen();
        final Role role1 = roleDataGen1.editLayouts(true).layout(layout1).nextPersisted();

        final User user1 = new UserDataGen().roles(role1).nextPersisted();
        final User user2 = new UserDataGen().roles(role1).nextPersisted();
        final User user3 = new UserDataGen().roles(role1).nextPersisted();

        dotConnect
                .setSQL(Task05170DefineFrontEndAndBackEndRoles.USERS_WITH_ASSIGNED_LAYOUT_BUT_NOT_BACKEND_ROLE);
        List<Map> usersWithAssignedLayoutButNotABackEndRole = dotConnect.loadResults();
        final Set<String> userIds = usersWithAssignedLayoutButNotABackEndRole.stream()
                .map(map -> map.get("user_id").toString()).collect(Collectors.toSet());
        assertTrue(
                "We expected the recently created users to be listed as users with layouts but not a backend role. ",
                usersWithAssignedLayoutButNotABackEndRole.size() >= 3);
        assertTrue(userIds.contains(user1.getUserId()));
        assertTrue(userIds.contains(user2.getUserId()));
        assertTrue(userIds.contains(user3.getUserId()));

        final Task05170DefineFrontEndAndBackEndRoles task05170DefineFrontEndAndBackEndRoles =
                new Task05170DefineFrontEndAndBackEndRoles();

        task05170DefineFrontEndAndBackEndRoles.executeUpgrade();
        // SQL must be re-set to clean to `reset` the instance
        dotConnect
                .setSQL(Task05170DefineFrontEndAndBackEndRoles.USERS_WITH_ASSIGNED_LAYOUT_BUT_NOT_BACKEND_ROLE);
        usersWithAssignedLayoutButNotABackEndRole = dotConnect.loadResults();
        assertTrue("The new users we just created should be gone by now. ", usersWithAssignedLayoutButNotABackEndRole.isEmpty());
    }

}
