package com.dotcms.api.system.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the user listing behind the legacy Users portlet ({@code UserAjax#getUsersList}), in
 * particular the {@code showUsers} role filter.
 */
public class UserServiceFactoryTest {

    private static final List<User> usersToClean = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @AfterClass
    public static void cleanUp() {
        usersToClean.forEach(UserDataGen::remove);
    }

    /** User IDs in the {@code data} rows of a {@code getUsersList} result. */
    @SuppressWarnings("unchecked")
    private static List<String> listedUserIds(final String query, final String showUsers) throws Exception {
        final Map<String, Object> result = UserServiceFactory.getInstance().getUserService()
                .getUsersList(null, null, Map.of("query", query, "limit", "100", "showUsers", showUsers));
        return ((List<Map<String, String>>) result.get("data")).stream()
                .map(row -> row.get("id"))
                .collect(Collectors.toList());
    }

    /**
     * Method to test: {@link UserService#getUsersList(String, String, Map)}
     * Given Scenario: Two back-end users share a unique first name; one of them also holds the
     * CMS Administrator role. The list is requested with {@code showUsers=cmsAdmin}, then with
     * {@code showUsers=all}.
     * ExpectedResult: The CMS Administrator filter returns the administrator only; the default
     * filter returns both.
     */
    @Test
    public void test_getUsersList_showUsersCmsAdmin_returnsOnlyCmsAdministrators() throws Exception {
        final String prefix = "svcAdmin" + System.nanoTime();
        final Role cmsAdmin = APILocator.getRoleAPI().loadCMSAdminRole();
        final User administrator = new UserDataGen().firstName(prefix).lastName("Admin")
                .roles(cmsAdmin, TestUserUtils.getBackendRole()).nextPersisted();
        usersToClean.add(administrator);
        final User plain = new UserDataGen().firstName(prefix).lastName("Plain")
                .roles(TestUserUtils.getBackendRole()).nextPersisted();
        usersToClean.add(plain);

        final List<String> admins = listedUserIds(prefix, "cmsAdmin");
        assertTrue("the CMS Administrator must be listed", admins.contains(administrator.getUserId()));
        assertFalse("a user without the role must not be listed", admins.contains(plain.getUserId()));
        assertEquals(1, admins.size());

        final List<String> all = listedUserIds(prefix, "all");
        assertTrue(all.contains(administrator.getUserId()));
        assertTrue(all.contains(plain.getUserId()));
    }
}
