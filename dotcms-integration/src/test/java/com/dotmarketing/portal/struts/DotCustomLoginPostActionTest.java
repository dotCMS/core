package com.dotmarketing.portal.struts;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.enterprise.PasswordFactoryProxy;
import com.dotcms.enterprise.PasswordFactoryProxy.AuthenticationStatus;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests ensuring {@link DotCustomLoginPostAction#run} preserves the local password
 * of former directory users after they sign in.
 */
public class DotCustomLoginPostActionTest {

    private static final String SYNC_PASSWORD_KEY = "auth.impl.ldap.syncPassword";
    private static final String DIRECTORY_ROLE_KEY = "LDAP User";
    private static final String PASSWORD = "Str0ng-Test-Passw0rd!";

    private static String originalSyncPassword;
    private static Role createdDirectoryRole;
    private static User user;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        originalSyncPassword = Config.getStringProperty(SYNC_PASSWORD_KEY, null);

        Role role = APILocator.getRoleAPI().loadRoleByKey(DIRECTORY_ROLE_KEY);
        if (role == null) {
            role = createdDirectoryRole = new RoleDataGen()
                    .key(DIRECTORY_ROLE_KEY)
                    .name(DIRECTORY_ROLE_KEY)
                    .nextPersisted();
        }

        user = new UserDataGen()
                .emailAddress("login-post-action-" + UUIDGenerator.generateUuid() + "@dotcms.test")
                .password(PASSWORD)
                .roles(role)
                .nextPersisted();
    }

    @AfterClass
    public static void cleanup() {
        Config.setProperty(SYNC_PASSWORD_KEY, originalSyncPassword);
        UserDataGen.remove(user);
        if (createdDirectoryRole != null) {
            RoleDataGen.remove(createdDirectoryRole);
        }
    }

    /**
     * Given: a former directory user, with the legacy password-sync setting disabled.
     * When:  the post-login action runs for that user.
     * Then:  the stored password is unchanged and the user's local password still matches.
     */
    @Test
    public void test_run_formerDirectoryUser_preservesLocalPassword() throws Exception {
        Config.setProperty(SYNC_PASSWORD_KEY, "false");
        final String originalPassword = new DotConnect()
                .setSQL("select password_ from user_ where userId = ?")
                .addParam(user.getUserId())
                .getString("password_");
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(WebKeys.USER)).thenReturn(user);

        new DotCustomLoginPostAction().run(request, mock(HttpServletResponse.class));

        final String stored = new DotConnect()
                .setSQL("select password_ from user_ where userId = ?")
                .addParam(user.getUserId())
                .getString("password_");
        assertEquals(originalPassword, stored);
        assertEquals(AuthenticationStatus.AUTHENTICATED,
                PasswordFactoryProxy.authPassword(PASSWORD, stored));
    }
}
