package com.dotcms.rest.api.v1.layout;

import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.PortletDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.LayoutNameAlreadyExistsException;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import com.liferay.util.Base64;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Integration tests for {@link LayoutResource}, the {@code /v1/layouts} endpoints the Tools
 * portlet uses to manage navigation sections (issue #37353).
 * <p>
 * The resource is invoked directly with mock authenticated requests, following
 * {@code RoleResourceIntegrationTest}. Three callers are exercised: a CMS Administrator through
 * Basic auth, a non-admin back-end user whose only relevant grant is a section holding the
 * {@code tools-beta} portlet, and a non-admin back-end user with neither {@code tools} nor
 * {@code tools-beta}.
 *
 * @author hassandotcms
 */
public class LayoutResourceIntegrationTest {

    private static final String TOOLS_BETA_PORTLET_ID = "tools-beta";

    private static LayoutResource resource;
    private static LayoutAPI layoutAPI;
    private static RoleAPI roleAPI;
    private static Host testHost;

    /** Non-admin back-end user granted a section that contains {@code tools-beta}. */
    private static User toolsBetaUser;

    /** Non-admin back-end user whose sections contain neither {@code tools} nor {@code tools-beta}. */
    private static User noPortletUser;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        resource = new LayoutResource();
        layoutAPI = APILocator.getLayoutAPI();
        roleAPI = APILocator.getRoleAPI();
        testHost = new SiteDataGen().nextPersisted();

        final Layout toolsBetaLayout = new LayoutDataGen()
                .name("Tools Beta " + uniq())
                .portletIds(TOOLS_BETA_PORTLET_ID)
                .tabOrder(800000)
                .nextPersisted();
        final Role toolsBetaRole = new RoleDataGen().layout(toolsBetaLayout).nextPersisted();
        toolsBetaUser = backendUser(toolsBetaRole);

        final Layout contentOnlyLayout = new LayoutDataGen()
                .name("Content Only " + uniq())
                .portletIds("content")
                .tabOrder(800001)
                .nextPersisted();
        final Role contentOnlyRole = new RoleDataGen().layout(contentOnlyLayout).nextPersisted();
        noPortletUser = backendUser(contentOnlyRole);
    }

    // ==================== Fixtures ====================

    /**
     * Creates an active back-end user holding the given role plus the back-end user role, so it
     * passes {@code requiredBackendUser(true)} and reaches the portlet gate.
     */
    private static User backendUser(final Role role) throws Exception {
        final User user = new UserDataGen().roles(role).nextPersisted();
        final Role backendRole = roleAPI.loadBackEndUserRole();
        if (!roleAPI.doesUserHaveRole(user, backendRole)) {
            roleAPI.addRoleToUser(backendRole, user);
        }
        return user;
    }

    private static String uniq() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    // ==================== Request builders ====================

    /** Request authenticated as {@code admin@dotcms.com} through Basic auth. */
    private static HttpServletRequest adminRequest() {
        final MockHeaderRequest request = baseRequest();
        request.setHeader("Authorization",
                "Basic " + new String(Base64.encode("admin@dotcms.com:admin".getBytes())));
        return request;
    }

    /** Request whose session already carries the given user, as a logged-in browser would. */
    private static HttpServletRequest requestFor(final User user) {
        final MockHeaderRequest request = baseRequest();
        request.getSession().setAttribute(com.liferay.portal.util.WebKeys.USER_ID, user.getUserId());
        request.getSession().setAttribute(com.liferay.portal.util.WebKeys.USER, user);
        return request;
    }

    /** Request with no credentials at all. */
    private static HttpServletRequest anonymousRequest() {
        return baseRequest();
    }

    private static MockHeaderRequest baseRequest() {
        return new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(
                                new MockHttpRequestIntegrationTest(testHost.getHostname(), "/").request())
                                .request())
                        .request());
    }

    private static HttpServletResponse response() {
        return new MockHttpResponse();
    }

    // ==================== Read helpers ====================

    @SuppressWarnings("unchecked")
    private static List<SectionView> listAs(final HttpServletRequest request) throws Exception {
        final Response response = resource.list(request, response());
        assertEquals(200, response.getStatus());
        final ResponseEntitySectionListView view = (ResponseEntitySectionListView) response.getEntity();
        assertNotNull(view);
        return view.getEntity();
    }

    private static SectionView find(final List<SectionView> sections, final String id) {
        return sections.stream().filter(s -> id.equals(s.id())).findFirst()
                .orElseThrow(() -> new AssertionError("section " + id + " not in list"));
    }

    // ==================== US1: GET /v1/layouts ====================

    /**
     * Method to test: {@link LayoutResource#list}
     * Given: two sections with explicit positions
     * Expected: the list has exactly the ids of {@code findAllLayouts}, in the same order
     */
    @Test
    public void list_returnsEverySection_inFindAllLayoutsOrder() throws Exception {
        new LayoutDataGen().name("Order A " + uniq()).tabOrder(900001).nextPersisted();
        new LayoutDataGen().name("Order B " + uniq()).tabOrder(900002).nextPersisted();

        final List<String> expected = layoutAPI.findAllLayouts().stream()
                .map(Layout::getId).collect(Collectors.toList());
        final List<String> actual = listAs(requestFor(toolsBetaUser)).stream()
                .map(SectionView::id).collect(Collectors.toList());

        assertEquals(expected, actual);
    }

    /**
     * Method to test: {@link LayoutResource#list}
     * Given: a section saved with tools in a specific order
     * Expected: portletIds come back in exactly that order
     */
    @Test
    public void list_preservesToolOrderWithinSection() throws Exception {
        final Layout layout = new LayoutDataGen().name("Tool Order " + uniq())
                .portletIds("templates", "containers", "site-browser").tabOrder(900003).nextPersisted();

        final SectionView view = find(listAs(requestFor(toolsBetaUser)), layout.getId());

        assertEquals(List.of("templates", "containers", "site-browser"), view.portletIds());
    }

    /**
     * Method to test: {@link LayoutResource#list}
     * Given: a section holding a shipped tool and a custom tool registered with a name but no translation
     * Expected: portletTitles is aligned with portletIds, the shipped tool is translated, the custom
     *           tool shows its registered name, and no title is the raw translation key
     */
    @Test
    public void list_alignsTitlesWithPortletIds_andNeverReturnsRawKey() throws Exception {
        final String customId = "c_pm_" + uniq();
        final Portlet custom = new PortletDataGen().portletId(customId)
                .initParams(Map.of("name", "Press Releases " + customId)).nextPersisted();
        try {
            final Layout layout = new LayoutDataGen().name("Titles " + uniq())
                    .portletIds("templates", custom.getPortletId()).tabOrder(900004).nextPersisted();

            final SectionView view = find(listAs(requestFor(toolsBetaUser)), layout.getId());

            assertEquals(view.portletIds().size(), view.portletTitles().size());
            assertEquals("Templates", view.portletTitles().get(0));
            assertEquals("Press Releases " + customId, view.portletTitles().get(1));
            for (final String title : view.portletTitles()) {
                assertFalse("raw key leaked: " + title,
                        title.startsWith("com.dotcms.repackage.javax.portlet.title."));
            }
        } finally {
            // Leave no custom portlet behind: it would surface in the tools catalog for other suites.
            APILocator.getPortletAPI().deletePortlet(custom.getPortletId());
        }
    }

    /**
     * Method to test: {@link LayoutResource#list}
     * Given: a section holding the old Languages tool, which the tools catalog hides by default
     * Expected: the tool is still listed in the section, because the read reflects what is stored
     */
    @Test
    public void list_includesHiddenLanguagesTool_whenStoredInSection() throws Exception {
        final Layout layout = new LayoutDataGen().name("Languages " + uniq())
                .portletIds("languages").tabOrder(900005).nextPersisted();

        final SectionView view = find(listAs(requestFor(toolsBetaUser)), layout.getId());

        assertEquals(List.of("languages"), view.portletIds());
    }

    /**
     * Method to test: {@link LayoutResource#list}
     * Given: a CMS Administrator with no explicit Tools grant
     * Expected: the list is returned (administrator fallback)
     */
    @Test
    public void list_adminWithoutGrant_ok() throws Exception {
        assertFalse(listAs(adminRequest()).isEmpty());
    }

    /**
     * Method to test: {@link LayoutResource#list}
     * Given: a non-admin back-end user with neither tools nor tools-beta
     * Expected: the portlet gate rejects the call (REST SecurityException, 401)
     */
    @Test
    public void list_noPortletUser_rejected() throws Exception {
        try {
            resource.list(requestFor(noPortletUser), response());
            fail("expected the portlet gate to reject the call");
        } catch (final com.dotcms.rest.exception.SecurityException expected) {
            assertEquals(401, expected.getResponse().getStatus());
        }
    }
}
