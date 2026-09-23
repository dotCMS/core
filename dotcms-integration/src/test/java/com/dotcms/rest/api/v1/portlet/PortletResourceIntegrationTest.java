package com.dotcms.rest.api.v1.portlet;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.portal.PortletAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PortletID;
import com.liferay.portal.ejb.UserTestUtil;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import com.liferay.util.Base64;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Integration tests for the {@link PortletResource} changes delivered by issue #37574: the tools
 * catalog, the single custom-tool read, the widened Roles-or-Tools gate on the custom-tool writes
 * and the custom-only guard on the custom-tool delete.
 * <p>
 * The resource is invoked directly with mock authenticated requests, following the pattern of
 * {@code RoleResourceIntegrationTest}. Non-admin users are granted a layout (navigation section)
 * containing exactly the portlet ids a scenario needs, which is how portlet gates are decided.
 *
 * @author hassandotcms
 */
public class PortletResourceIntegrationTest {

    private static final String TOOLS_BETA = PortletID.TOOLS_BETA.toString();
    private static final String ROLES = PortletID.ROLES.toString();

    private static PortletResource resource;
    private static PortletAPI portletAPI;
    private static RoleAPI roleAPI;
    private static Host testHost;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        resource = new PortletResource();
        portletAPI = APILocator.getPortletAPI();
        roleAPI = APILocator.getRoleAPI();
        testHost = new SiteDataGen().nextPersisted();
    }

    // ==================== Helpers ====================

    private static String uniq() {
        return Long.toString(System.nanoTime());
    }

    private static HttpServletRequest baseRequest() {
        return new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(
                                new MockHttpRequestIntegrationTest(testHost.getHostname(), "/").request())
                                .request())
                        .request());
    }

    /**
     * Request authenticated as the default CMS Administrator through Basic auth, as the Postman
     * collections do.
     */
    private static HttpServletRequest adminRequest() {
        final MockHeaderRequest request = (MockHeaderRequest) baseRequest();
        request.setHeader("Authorization",
                "Basic " + new String(Base64.encode("admin@dotcms.com:admin".getBytes())));
        return request;
    }

    /**
     * Request whose session already carries the given user, as the back-end login filter leaves
     * it.
     */
    private static HttpServletRequest requestFor(final User user) {
        final HttpServletRequest request = baseRequest();
        request.getSession().setAttribute(com.liferay.portal.util.WebKeys.USER_ID, user.getUserId());
        request.getSession().setAttribute(com.liferay.portal.util.WebKeys.USER, user);
        return request;
    }

    /** Request with no session user and no credentials. */
    private static HttpServletRequest anonymousRequest() {
        return baseRequest();
    }

    /**
     * Creates a non-admin back-end user whose only relevant grant is a fresh layout containing
     * exactly the given portlet ids. Ids need not belong to a registered portlet, which is how the
     * {@code tools} / {@code tools-beta} grant is given before that portlet ships.
     */
    private static User backendUserWithLayout(final String... portletIds) throws Exception {
        final Layout layout = new LayoutDataGen().portletIds(portletIds).nextPersisted();
        final Role role = new RoleDataGen().layout(layout).nextPersisted();
        final User user = UserTestUtil.getUser("pr" + uniq(), false, true);
        roleAPI.addRoleToUser(roleAPI.loadBackEndUserRole(), user);
        roleAPI.addRoleToUser(role, user);
        return user;
    }

    /** A non-admin back-end user with no layout at all. */
    private static User backendUserWithoutGrant() throws Exception {
        final User user = UserTestUtil.getUser("prnone" + uniq(), false, true);
        roleAPI.addRoleToUser(roleAPI.loadBackEndUserRole(), user);
        return user;
    }

    private static CustomPortletForm.Builder customToolForm(final String id, final String name) {
        return CustomPortletForm.builder()
                .withPortletId(id)
                .withPortletName(name)
                .withBaseTypes("CONTENT")
                .withContentTypes("")
                .withDataViewMode("list");
    }

    /** Creates a custom content tool as admin and returns its stored ({@code c_}-prefixed) id. */
    @SuppressWarnings("unchecked")
    private static String createCustomToolAsAdmin(final String id, final String name) {
        final Response response = resource.saveNew(adminRequest(), customToolForm(id, name).build());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final Map<String, String> entity =
                ((ResponseEntityView<Map<String, String>>) response.getEntity()).getEntity();
        return entity.get("portlet");
    }

    private static void deleteQuietly(final String portletId) {
        if (null != portletId && null != portletAPI.findPortlet(portletId)) {
            portletAPI.deletePortlet(portletId);
        }
    }

    /**
     * Makes sure the product's Language Variables tool exists, inserting it exactly as
     * {@code Task241016AddCustomLanguageVariablesPortletToLayout} does when the startup task did
     * not run against this database.
     */
    private static void ensureLanguageVariablesPortlet() throws Exception {
        final String id = PortletID.LANGUAGE_VARIABLES.toString();
        if (null != portletAPI.findPortlet(id)) {
            return;
        }
        final Map<String, String> initParams = new HashMap<>();
        initParams.put("name", "Language Variables");
        initParams.put("baseTypes", "");
        initParams.put("dataViewMode", "list");
        initParams.put("view-action", "/ext/contentlet/view_contentlets");
        initParams.put("portletSource", "db");
        initParams.put("contentTypes", "Languagevariable");
        FactoryLocator.getPortletFactory().insertPortlet(
                new Portlet(id, "SHARED_KEY", "com.liferay.portlet.StrutsPortlet", initParams));
        assertNotNull("Language Variables portlet must be loadable after insert", portletAPI.findPortlet(id));
    }

    @SuppressWarnings("unchecked")
    private static List<ToolCatalogEntryView> catalogFor(final HttpServletRequest request) {
        final Response response = resource.getToolsCatalog(request, new MockHttpResponse().response());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        return ((ResponseEntityView<List<ToolCatalogEntryView>>) response.getEntity()).getEntity();
    }

    private static Optional<ToolCatalogEntryView> row(final List<ToolCatalogEntryView> rows, final String id) {
        return rows.stream().filter(r -> id.equals(r.id())).findFirst();
    }

    /**
     * The 404 body is the standard envelope with one {@code custom.content.portlet.not.found}
     * error naming the id, the same shape the resource's other not-found answers have.
     */
    @SuppressWarnings("unchecked")
    private static void assertNotFoundMessageBody(final Response response, final String portletId) {
        final List<com.dotcms.rest.ErrorEntity> errors =
                ((ResponseEntityView<?>) response.getEntity()).getErrors();
        assertEquals(1, errors.size());
        assertEquals("custom.content.portlet.not.found", errors.get(0).getErrorCode());
        assertTrue(errors.get(0).getMessage().contains(portletId));
    }

    private static void assertUnauthorized(final Runnable call) {
        try {
            call.run();
            fail("Expected the portlet gate to reject the call with 401");
        } catch (final SecurityException e) {
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), e.getResponse().getStatus());
        }
    }

    // ==================== US1: GET /v1/portlet/_catalog ====================

    /**
     * Given Scenario: an anonymous caller requests the catalog.
     * Expected Result: 401, no tool data.
     */
    @Test
    public void catalog_anonymous_401() {
        assertUnauthorized(() -> resource.getToolsCatalog(anonymousRequest(), new MockHttpResponse().response()));
    }

    /**
     * Given Scenario: a back-end user with no Tools grant in any section.
     * Expected Result: 401.
     */
    @Test
    public void catalog_backendUserWithoutGrant_401() throws Exception {
        final User user = backendUserWithoutGrant();
        assertUnauthorized(() -> resource.getToolsCatalog(requestFor(user), new MockHttpResponse().response()));
    }

    /**
     * Given Scenario: a back-end user who holds only the Roles portlet.
     * Expected Result: 401; Roles alone does not open the catalog.
     */
    @Test
    public void catalog_rolesOnlyUser_401() throws Exception {
        final User user = backendUserWithLayout(ROLES);
        assertUnauthorized(() -> resource.getToolsCatalog(requestFor(user), new MockHttpResponse().response()));
    }

    /**
     * Given Scenario: a non-admin back-end user whose only grant is a section containing
     * {@code tools-beta}.
     * Expected Result: 200 with a non-empty catalog.
     */
    @Test
    public void catalog_toolsBetaUser_200() throws Exception {
        final User user = backendUserWithLayout(TOOLS_BETA);
        assertFalse(catalogFor(requestFor(user)).isEmpty());
    }

    /**
     * Given Scenario: the CMS Administrator with no explicit grant.
     * Expected Result: 200, administrators pass every portlet gate.
     */
    @Test
    public void catalog_admin_200() {
        assertFalse(catalogFor(adminRequest()).isEmpty());
    }

    /**
     * Given Scenario: an instance with shipped tools, Language Variables and a custom tool created
     * a moment ago.
     * Expected Result: the custom tool is present, flagged custom, titled with the name it was
     * given; shipped tools and Language Variables are present and not custom; no row is a tool
     * the product marks as not placeable; ids are unique; rows are sorted by title ignoring case.
     */
    @Test
    public void catalog_membershipAndFlags() throws Exception {
        final String name = "QA Catalog " + uniq();
        String customId = null;
        try {
            customId = createCustomToolAsAdmin("qa-catalog-" + uniq(), name);
            ensureLanguageVariablesPortlet();

            final List<ToolCatalogEntryView> rows = catalogFor(adminRequest());

            final ToolCatalogEntryView custom = row(rows, customId).orElseThrow();
            assertTrue(custom.isCustom());
            assertEquals(name, custom.title());

            final ToolCatalogEntryView roles = row(rows, ROLES).orElseThrow();
            assertFalse(roles.isCustom());

            final ToolCatalogEntryView langVars = row(rows, PortletID.LANGUAGE_VARIABLES.toString()).orElseThrow();
            assertFalse(langVars.isCustom());

            for (final ToolCatalogEntryView r : rows) {
                assertTrue("not placeable tool leaked into catalog: " + r.id(),
                        portletAPI.canAddPortletToLayout(r.id()));
                assertFalse("raw title key leaked for " + r.id(),
                        r.title().startsWith("com.dotcms.repackage.javax.portlet.title."));
            }

            final List<String> ids = rows.stream().map(ToolCatalogEntryView::id).collect(Collectors.toList());
            assertEquals("each id once", ids.size(), ids.stream().distinct().count());

            final List<ToolCatalogEntryView> sorted = rows.stream()
                    .sorted(Comparator.comparing((ToolCatalogEntryView r) -> r.title().toLowerCase())
                            .thenComparing(ToolCatalogEntryView::id))
                    .collect(Collectors.toList());
            assertEquals(sorted, rows);
        } finally {
            deleteQuietly(customId);
        }
    }

    /**
     * Given Scenario: the configuration that hides the old Languages tool is on, then turned off.
     * Expected Result: the old Languages tool is absent, then present.
     */
    @Test
    public void catalog_languagesFlag() {
        final String languages = PortletID.LANGUAGES.toString();
        assertNotNull("precondition: the languages portlet is registered", portletAPI.findPortlet(languages));
        try {
            Config.setProperty(FeatureFlagName.FEATURE_FLAG_LOCALES_HIDE_OLD_LANGUAGES_PORTLET, true);
            assertFalse(row(catalogFor(adminRequest()), languages).isPresent());

            Config.setProperty(FeatureFlagName.FEATURE_FLAG_LOCALES_HIDE_OLD_LANGUAGES_PORTLET, false);
            assertTrue(row(catalogFor(adminRequest()), languages).isPresent());
        } finally {
            Config.setProperty(FeatureFlagName.FEATURE_FLAG_LOCALES_HIDE_OLD_LANGUAGES_PORTLET, true);
        }
    }

    /**
     * Given Scenario: a custom tool is deleted.
     * Expected Result: it is absent from the very next catalog read.
     */
    @Test
    public void catalog_freshAfterDelete() {
        final String customId = createCustomToolAsAdmin("qa-fresh-" + uniq(), "QA Fresh " + uniq());
        assertTrue(row(catalogFor(adminRequest()), customId).isPresent());

        portletAPI.deletePortlet(customId);

        assertFalse(row(catalogFor(adminRequest()), customId).isPresent());
    }

    // ==================== US2: custom-tool writes, Roles OR Tools gate; delete refusal ====================

    private static void assertCreateUpdateDelete(final User user) throws Exception {
        final String id = "qa-writes-" + uniq();
        String storedId = null;
        try {
            final Response created = resource.saveNew(requestFor(user),
                    customToolForm(id, "QA Writes " + id).build());
            assertEquals(Response.Status.OK.getStatusCode(), created.getStatus());
            storedId = ((ResponseEntityView<Map<String, String>>) created.getEntity()).getEntity().get("portlet");
            assertNotNull(portletAPI.findPortlet(storedId));

            final Response updated = resource.updatePortlet(requestFor(user),
                    customToolForm(storedId, "QA Renamed " + id).build());
            assertEquals(Response.Status.OK.getStatusCode(), updated.getStatus());
            assertEquals("QA Renamed " + id, portletAPI.findPortlet(storedId).getInitParams().get("name"));

            final Response deleted = resource.deleteCustomPortlet(requestFor(user), storedId);
            assertEquals(Response.Status.OK.getStatusCode(), deleted.getStatus());
            assertEquals(storedId + " deleted",
                    ((ResponseEntityView<Map<String, String>>) deleted.getEntity()).getEntity().get("message"));
            assertNull(portletAPI.findPortlet(storedId));
        } finally {
            deleteQuietly(storedId);
        }
    }

    /**
     * Given Scenario: a non-admin back-end user whose only grant is a section containing
     * {@code tools-beta} creates, renames and deletes a custom content tool.
     * Expected Result: every step succeeds; the tool is gone at the end.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void customWrites_toolsBetaUser_createUpdateDelete_200() throws Exception {
        assertCreateUpdateDelete(backendUserWithLayout(TOOLS_BETA));
    }

    /**
     * Given Scenario: a non-admin back-end user whose only grant is a section containing
     * {@code roles} performs the same three operations.
     * Expected Result: every step succeeds exactly as before this change.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void customWrites_rolesOnlyUser_createUpdateDelete_200() throws Exception {
        assertCreateUpdateDelete(backendUserWithLayout(ROLES));
    }

    /**
     * Given Scenario: a back-end user with neither Roles nor Tools in any section attempts each
     * custom-tool write.
     * Expected Result: 401 on create, update and delete; nothing is created.
     */
    @Test
    public void customWrites_backendUserWithoutGrant_401() throws Exception {
        final User user = backendUserWithoutGrant();
        final String id = "qa-denied-" + uniq();
        final CustomPortletForm form = customToolForm(id, "QA Denied").build();

        assertUnauthorized(() -> resource.saveNew(requestFor(user), form));
        assertUnauthorized(() -> resource.updatePortlet(requestFor(user), form));
        assertUnauthorized(() -> resource.deleteCustomPortlet(requestFor(user), "c_" + id));
        assertNull(portletAPI.findPortlet("c_" + id));
    }

    /**
     * Given Scenario: a Tools-only user tries to add a tool to a section through the existing
     * add-to-section operation.
     * Expected Result: 401, exactly as today; that operation keeps its Roles gate.
     */
    @Test
    public void addToLayout_toolsBetaUser_401() throws Exception {
        final Layout layout = new LayoutDataGen().portletIds(TOOLS_BETA).nextPersisted();
        final Role role = new RoleDataGen().layout(layout).nextPersisted();
        final User user = UserTestUtil.getUser("prtools" + uniq(), false, true);
        roleAPI.addRoleToUser(roleAPI.loadBackEndUserRole(), user);
        roleAPI.addRoleToUser(role, user);

        final String customId = createCustomToolAsAdmin("qa-add-" + uniq(), "QA Add");
        try {
            assertUnauthorized(() -> {
                try {
                    resource.addContentPortletToLayout(requestFor(user), customId, layout.getId());
                } catch (final com.dotmarketing.exception.DotDataException e) {
                    throw new RuntimeException(e);
                }
            });
        } finally {
            deleteQuietly(customId);
        }
    }

    /**
     * Given Scenario: a Roles-only user adds an admin-created custom tool to the section they hold.
     * Expected Result: 200 and the section contains the tool (unchanged behaviour).
     */
    @Test
    public void addToLayout_rolesOnlyUser_200() throws Exception {
        final Layout layout = new LayoutDataGen().portletIds(ROLES).nextPersisted();
        final Role role = new RoleDataGen().layout(layout).nextPersisted();
        final User user = UserTestUtil.getUser("prroles" + uniq(), false, true);
        roleAPI.addRoleToUser(roleAPI.loadBackEndUserRole(), user);
        roleAPI.addRoleToUser(role, user);

        final String customId = createCustomToolAsAdmin("qa-addok-" + uniq(), "QA Add Ok");
        try {
            final Response response = resource.addContentPortletToLayout(requestFor(user), customId, layout.getId());
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            assertTrue(APILocator.getLayoutAPI().loadLayout(layout.getId()).getPortletIds().contains(customId));
        } finally {
            deleteQuietly(customId);
        }
    }

    /**
     * Given Scenario: the id of a tool shipped in portlet.xml, placed in a section, is sent to the
     * custom-tool delete by an administrator.
     * Expected Result: 404 and the section still contains the tool. Membership is the observable,
     * because an XML portlet stays loadable even when the old unconditional delete runs.
     */
    @Test
    public void deleteCustom_shippedTool_404_stillInSection() throws Exception {
        final Layout layout = new LayoutDataGen().portletIds(ROLES).nextPersisted();

        final Response response = resource.deleteCustomPortlet(adminRequest(), ROLES);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertNotFoundMessageBody(response, ROLES);
        assertTrue(APILocator.getLayoutAPI().loadLayout(layout.getId()).getPortletIds().contains(ROLES));
        assertNotNull(portletAPI.findPortlet(ROLES));
    }

    /**
     * Given Scenario: Language Variables, a product tool stored as a database row, is placed in a
     * section and sent to the custom-tool delete by an administrator.
     * Expected Result: 404; the tool is still registered and still in the section.
     */
    @Test
    public void deleteCustom_languageVariables_404_stillInSection() throws Exception {
        ensureLanguageVariablesPortlet();
        final String langVars = PortletID.LANGUAGE_VARIABLES.toString();
        final Layout layout = new LayoutDataGen().portletIds(langVars).nextPersisted();

        final Response response = resource.deleteCustomPortlet(adminRequest(), langVars);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertNotNull(portletAPI.findPortlet(langVars));
        assertTrue(APILocator.getLayoutAPI().loadLayout(layout.getId()).getPortletIds().contains(langVars));
    }

    /**
     * Given Scenario: an administrator sends the custom-tool update for Language Variables, a
     * product tool stored as a database row, with a different name, base types and view mode.
     * Expected Result: 404; the stored configuration is exactly what it was before.
     */
    @Test
    public void updateCustom_shippedDbTool_404_configUnchanged() throws Exception {
        ensureLanguageVariablesPortlet();
        final String langVars = PortletID.LANGUAGE_VARIABLES.toString();
        final Map<String, String> before = new HashMap<>(portletAPI.findPortlet(langVars).getInitParams());

        final Response response = resource.updatePortlet(adminRequest(), CustomPortletForm.builder()
                .withPortletId("Language-Variables")
                .withPortletName("Hijacked")
                .withBaseTypes("CONTENT")
                .withContentTypes("")
                .withDataViewMode("card")
                .build());

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertEquals(before, portletAPI.findPortlet(langVars).getInitParams());
    }

    /**
     * Given Scenario: an id no tool has is sent to the custom-tool delete.
     * Expected Result: 404.
     */
    @Test
    public void deleteCustom_unknownId_404() {
        final Response response = resource.deleteCustomPortlet(adminRequest(), "c_nope-" + uniq());
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    // ==================== US3: GET /v1/portlet/custom/{portletId} ====================

    @SuppressWarnings("unchecked")
    private static CustomToolView readCustomTool(final HttpServletRequest request, final String id) {
        final Response response = resource.getCustomTool(request, new MockHttpResponse().response(), id);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        return ((ResponseEntityView<CustomToolView>) response.getEntity()).getEntity();
    }

    /**
     * Given Scenario: a custom tool saved with two base types, two content types and the card
     * view; a Tools-only user reads it by id.
     * Expected Result: the stored id, name, both lists and {@code card}, exactly as saved.
     */
    @Test
    public void customRead_toolsBetaUser_returnsSavedValues() throws Exception {
        final ContentType ct1 = new ContentTypeDataGen().nextPersisted();
        final ContentType ct2 = new ContentTypeDataGen().nextPersisted();
        final String name = "QA Read " + uniq();
        String storedId = null;
        try {
            final Response created = resource.saveNew(adminRequest(), CustomPortletForm.builder()
                    .withPortletId("qa-read-" + uniq())
                    .withPortletName(name)
                    .withBaseTypes("CONTENT,PERSONA")
                    .withContentTypes(ct1.variable() + "," + ct2.variable())
                    .withDataViewMode("card")
                    .build());
            assertEquals(Response.Status.OK.getStatusCode(), created.getStatus());
            storedId = ((ResponseEntityView<Map<String, String>>) created.getEntity()).getEntity().get("portlet");

            final CustomToolView view = readCustomTool(requestFor(backendUserWithLayout(TOOLS_BETA)), storedId);

            assertEquals(storedId, view.portletId());
            assertEquals(name, view.portletName());
            assertEquals(List.of("CONTENT", "PERSONA"), view.baseTypes());
            assertEquals(List.of(ct1.variable(), ct2.variable()), view.contentTypes());
            assertEquals("card", view.dataViewMode());
        } finally {
            deleteQuietly(storedId);
        }
    }

    /**
     * Given Scenario: the id of a tool shipped in portlet.xml is read through the custom-tool read.
     * Expected Result: 404, it has no custom configuration to edit.
     */
    @Test
    public void customRead_shippedTool_404() {
        final Response response = resource.getCustomTool(adminRequest(), new MockHttpResponse().response(), ROLES);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertNotFoundMessageBody(response, ROLES);
    }

    /**
     * Given Scenario: an id no tool has.
     * Expected Result: 404.
     */
    @Test
    public void customRead_unknownId_404() {
        final Response response = resource.getCustomTool(adminRequest(), new MockHttpResponse().response(),
                "c_nope-" + uniq());
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    /**
     * Given Scenario: a Roles-only user reads a custom tool by id.
     * Expected Result: 401; Roles alone does not open the read, the legacy screen is their path.
     */
    @Test
    public void customRead_rolesOnlyUser_401() throws Exception {
        final User user = backendUserWithLayout(ROLES);
        assertUnauthorized(() -> resource.getCustomTool(requestFor(user), new MockHttpResponse().response(), "c_any"));
    }

    /**
     * Given Scenario: an anonymous caller.
     * Expected Result: 401.
     */
    @Test
    public void customRead_anonymous_401() {
        assertUnauthorized(() -> resource.getCustomTool(anonymousRequest(), new MockHttpResponse().response(), "c_any"));
    }

    // ==================== US4: the final `tools` id opens every Tools gate ====================

    /**
     * Given Scenario: a non-admin back-end user whose only grant is a section containing the
     * final {@code tools} id (no portlet of that name registered yet).
     * Expected Result: the catalog, the custom-tool read and a custom-tool write all succeed.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void toolsFinalId_opensCatalogReadAndWrite() throws Exception {
        final User user = backendUserWithLayout(PortletID.TOOLS.toString());
        assertFalse(catalogFor(requestFor(user)).isEmpty());

        final String adminMade = createCustomToolAsAdmin("qa-final-read-" + uniq(), "QA Final Read");
        String userMade = null;
        try {
            assertEquals(adminMade, readCustomTool(requestFor(user), adminMade).portletId());

            final Response created = resource.saveNew(requestFor(user),
                    customToolForm("qa-final-write-" + uniq(), "QA Final Write").build());
            assertEquals(Response.Status.OK.getStatusCode(), created.getStatus());
            userMade = ((ResponseEntityView<Map<String, String>>) created.getEntity()).getEntity().get("portlet");
            assertNotNull(portletAPI.findPortlet(userMade));
        } finally {
            deleteQuietly(adminMade);
            deleteQuietly(userMade);
        }
    }
}
