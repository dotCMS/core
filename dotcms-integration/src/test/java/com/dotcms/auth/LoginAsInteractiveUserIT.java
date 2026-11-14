package com.dotcms.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.rest.api.v1.user.UserResourceHelper;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.UserAPI;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Login As between two regular back-end users keeps working, and the built-in account is refused both as
 * the user who starts it and as the user to switch to.
 */
public class LoginAsInteractiveUserIT extends IntegrationTestBase {

    private static final String PASSWORD = "Interactive-User-1!";
    private static final List<User> toClean = new ArrayList<>();
    private static User principal;
    private static User target;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        final Role loginAs = APILocator.getRoleAPI().findRoleByFQN(Role.SYSTEM + " --> " + Role.LOGIN_AS);
        final Role backEnd = APILocator.getRoleAPI().loadBackEndUserRole();
        principal = new UserDataGen().password(PASSWORD).roles(loginAs, backEnd).nextPersisted();
        target = new UserDataGen().password(PASSWORD).roles(backEnd).nextPersisted();
        APILocator.getRoleAPI().addLayoutToRole(APILocator.getLayoutAPI().findAllLayouts().get(0),
                APILocator.getRoleAPI().getUserRole(target));
        toClean.add(principal);
        toClean.add(target);
    }

    @AfterClass
    public static void cleanup() {
        toClean.forEach(UserDataGen::remove);
    }

    /** Keep-green: Login As from one back-end user to another works as today. */
    @Test
    public void login_as_between_regular_users_still_works() throws Exception {
        final Map<String, Object> data = UserResourceHelper.getInstance()
                .doLoginAs(principal, target.getUserId(), null, null);
        assertEquals(target.getUserId(), data.get(WebKeys.USER_ID));
        assertEquals(principal.getUserId(), data.get(WebKeys.PRINCIPAL_USER_ID));
    }

    /** The built-in account is not a valid user to switch to, even with the caller's own password. */
    @Test
    public void built_in_account_is_refused_as_the_target() {
        assertThrows(Exception.class, () -> UserResourceHelper.getInstance()
                .doLoginAs(principal, UserAPI.SYSTEM_USER_ID, PASSWORD, null));
    }

    /** The built-in account cannot start a Login As either. */
    @Test
    public void built_in_account_is_refused_as_the_principal() throws Exception {
        final User system = APILocator.getUserAPI().getSystemUser();
        assertThrows(Exception.class, () -> UserResourceHelper.getInstance()
                .doLoginAs(system, target.getUserId(), null, null));
    }
}
