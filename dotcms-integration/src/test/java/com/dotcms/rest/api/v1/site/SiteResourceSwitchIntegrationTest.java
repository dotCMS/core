package com.dotcms.rest.api.v1.site;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Switching the current site through {@link SiteResource#switchSite} with real permissions: the
 * user needs READ on the site as a back-end user, and front-end roles do not count.
 */
public class SiteResourceSwitchIntegrationTest {

    private static final List<Host> createdSites = new ArrayList<>();
    private static final List<User> createdUsers = new ArrayList<>();

    @BeforeAll
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /** Removes the users and sites the tests created. */
    @AfterAll
    public static void cleanup() {
        createdUsers.forEach(user -> UserDataGen.remove(user, Boolean.TRUE));
        for (final Host site : createdSites) {
            try {
                APILocator.getHostAPI().archive(site, APILocator.systemUser(), false);
                APILocator.getHostAPI().delete(site, APILocator.systemUser(), false);
            } catch (final Exception e) {
                Logger.warn(SiteResourceSwitchIntegrationTest.class,
                        "Unable to delete test site: " + e.getMessage());
            }
        }
    }

    /**
     * Given: a site whose only READ grant is to CMS Anonymous, and a back-end user with no
     *        permissions on it
     * When:  the user switches to the site
     * Then:  the switch is refused and the session's selected site is not the requested one
     */
    @Test
    public void test_switchSite_siteReadableOnlyThroughFrontEndRoles_isRefused() throws Exception {
        final Host site = newSite();
        final Role anonymous = APILocator.getRoleAPI().loadCMSAnonymousRole();
        grantRead(site, anonymous);
        final User user = newBackendUser();
        // the site does resolve when front-end roles count
        assertNotNull(APILocator.getHostAPI().find(site.getIdentifier(), user, true));

        final HttpServletRequest request = request(user);
        assertThrows(ForbiddenException.class, () -> new SiteResource()
                .switchSite(request, new MockHttpResponse(), site.getIdentifier()));

        assertNotEquals(site.getIdentifier(),
                request.getSession().getAttribute(WebKeys.CMS_SELECTED_HOST_ID));
    }

    /**
     * Given: a site on which a back-end user's own role has READ
     * When:  the user switches to the site
     * Then:  the switch succeeds and the session's selected site is the requested one
     */
    @Test
    public void test_switchSite_siteReadableAsBackendUser_switches() throws Exception {
        final Host site = newSite();
        final User user = newBackendUser();
        grantRead(site, APILocator.getRoleAPI().getUserRole(user));

        final HttpServletRequest request = request(user);
        final Response response = new SiteResource()
                .switchSite(request, new MockHttpResponse(), site.getIdentifier());

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(site.getIdentifier(),
                request.getSession().getAttribute(WebKeys.CMS_SELECTED_HOST_ID));
    }

    /** Creates a site that is removed after the class runs. */
    private static Host newSite() {
        final Host site = new SiteDataGen().nextPersisted();
        createdSites.add(site);
        return site;
    }

    /** Creates a back-end user, with no other role, that is removed after the class runs. */
    private static User newBackendUser() throws Exception {
        final User user = new UserDataGen()
                .roles(APILocator.getRoleAPI().loadBackEndUserRole())
                .nextPersisted();
        createdUsers.add(user);
        return user;
    }

    /** Gives the role READ on the site itself. */
    private static void grantRead(final Host site, final Role role) throws Exception {
        APILocator.getPermissionAPI().save(
                new Permission(site.getPermissionId(), role.getId(), PermissionAPI.PERMISSION_READ, true),
                site, APILocator.systemUser(), false);
    }

    /** A request with a session, authenticated as the given user. */
    private static HttpServletRequest request(final User user) {
        final HttpServletRequest request = new MockSessionRequest(
                new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/api/v1/site/switch").request())
                        .request())
                .request();
        request.setAttribute(com.liferay.portal.util.WebKeys.USER, user);
        return request;
    }
}
