package com.dotmarketing.portal.struts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.enterprise.LDAPImpl;
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
 * Integration tests for the password {@link DotCustomLoginPostAction#run} stores for directory
 * users after they sign in.
 */
public class DotCustomLoginPostActionTest {

    private static final String SYNC_PASSWORD_KEY = "auth.impl.ldap.syncPassword";
    private static final String PASSWORD = "Str0ng-Test-Passw0rd!";

    private static String originalSyncPassword;
    private static Role createdDirectoryRole;
    private static User user;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        originalSyncPassword = Config.getStringProperty(SYNC_PASSWORD_KEY, null);

        Role role = APILocator.getRoleAPI().loadRoleByKey(LDAPImpl.LDAP_USER_ROLE);
        if (role == null) {
            role = createdDirectoryRole = new RoleDataGen()
                    .key(LDAPImpl.LDAP_USER_ROLE)
                    .name(LDAPImpl.LDAP_USER_ROLE)
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
     * Given: a directory user, with directory passwords not copied into dotCMS.
     * When:  the post-login action runs for that user.
     * Then:  the stored password is a secure hash that the user's previous password no longer
     *        matches.
     */
    @Test
    public void test_run_directoryUser_storesSecureRandomHash() throws Exception {
        Config.setProperty(SYNC_PASSWORD_KEY, "false");
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(WebKeys.USER)).thenReturn(user);

        new DotCustomLoginPostAction().run(request, mock(HttpServletResponse.class));

        final String stored = new DotConnect()
                .setSQL("select password_ from user_ where userId = ?")
                .addParam(user.getUserId())
                .getString("password_");
        assertFalse(PasswordFactoryProxy.isUnsecurePasswordHash(stored));
        assertEquals(AuthenticationStatus.NOT_AUTHENTICATED,
                PasswordFactoryProxy.authPassword(PASSWORD, stored));
    }
}
