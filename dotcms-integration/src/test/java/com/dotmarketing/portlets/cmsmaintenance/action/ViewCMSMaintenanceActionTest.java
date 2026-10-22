package com.dotmarketing.portlets.cmsmaintenance.action;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ViewCMSMaintenanceActionTest extends IntegrationTestBase {

    private static final List<User> createdUsers = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @AfterClass
    public static void cleanUp() {
        createdUsers.forEach(user -> UserDataGen.remove(user, Boolean.TRUE));
        createdUsers.clear();
    }

    private static User newNonAdmin() throws DotDataException {
        final User user = new UserDataGen()
                .roles(APILocator.getRoleAPI().loadBackEndUserRole())
                .nextPersisted();
        createdUsers.add(user);
        return user;
    }

    private static User newAdmin() throws DotDataException {
        final User user = new UserDataGen()
                .roles(APILocator.getRoleAPI().loadBackEndUserRole(),
                        APILocator.getRoleAPI().loadCMSAdminRole())
                .nextPersisted();
        createdUsers.add(user);
        return user;
    }

    @Test
    public void isAuthorizedAdmin_nonAdminUser_isFalse() throws Exception {
        assertFalse(ViewCMSMaintenanceAction.isAuthorizedAdmin(newNonAdmin()));
    }

    @Test
    public void isAuthorizedAdmin_adminUser_isTrue() throws Exception {
        assertTrue(ViewCMSMaintenanceAction.isAuthorizedAdmin(newAdmin()));
    }

    @Test
    public void isAuthorizedAdmin_null_isFalse() {
        assertFalse(ViewCMSMaintenanceAction.isAuthorizedAdmin(null));
    }
}
