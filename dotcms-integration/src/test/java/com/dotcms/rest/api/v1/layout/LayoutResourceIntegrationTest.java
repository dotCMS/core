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
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import com.liferay.util.Base64;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.Property;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
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

    private static Layout toolsBetaLayout;
    private static Layout contentOnlyLayout;
    private static Role toolsBetaRole;
    private static Role contentOnlyRole;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        resource = new LayoutResource();
        layoutAPI = APILocator.getLayoutAPI();
        roleAPI = APILocator.getRoleAPI();
        testHost = new SiteDataGen().nextPersisted();

        toolsBetaLayout = new LayoutDataGen()
                .name("Tools Beta " + uniq())
                .portletIds(TOOLS_BETA_PORTLET_ID)
                .tabOrder(800000)
                .nextPersisted();
        toolsBetaRole = new RoleDataGen().layout(toolsBetaLayout).nextPersisted();
        toolsBetaUser = backendUser(toolsBetaRole);

        contentOnlyLayout = new LayoutDataGen()
                .name("Content Only " + uniq())
                .portletIds("content")
                .tabOrder(800001)
                .nextPersisted();
        contentOnlyRole = new RoleDataGen().layout(contentOnlyLayout).nextPersisted();
        noPortletUser = backendUser(contentOnlyRole);
    }

    @AfterClass
    public static void cleanup() throws Exception {
        for (final Layout layout : List.of(toolsBetaLayout, contentOnlyLayout)) {
            removeQuietly(layout.getId());
        }
        for (final User user : List.of(toolsBetaUser, noPortletUser)) {
            UserDataGen.remove(user, true);
        }
        for (final Role role : List.of(toolsBetaRole, contentOnlyRole)) {
            RoleDataGen.remove(role, true);
        }
    }

    // ==================== Fixtures ====================

    /** Deletes a section by id through a freshly loaded instance; ignores an id that is already gone. */
    private static void removeQuietly(final String layoutId) throws Exception {
        final Layout layout = layoutAPI.findLayout(layoutId);
        if (layout != null && UtilMethods.isSet(layout.getId())) {
            layoutAPI.removeLayout(layout);
        }
    }

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
        final Layout a = new LayoutDataGen().name("Order A " + uniq()).tabOrder(900001).nextPersisted();
        final Layout b = new LayoutDataGen().name("Order B " + uniq()).tabOrder(900002).nextPersisted();
        try {
            final List<String> expected = layoutAPI.findAllLayouts().stream()
                    .map(Layout::getId).collect(Collectors.toList());
            final List<String> actual = listAs(requestFor(toolsBetaUser)).stream()
                    .map(SectionView::id).collect(Collectors.toList());

            // Same membership; and the two sections with distinct positions come back in position order.
            // (Sections sharing a position may legitimately swap between two reads.)
            assertEquals(new java.util.HashSet<>(expected), new java.util.HashSet<>(actual));
            assertEquals(expected.size(), actual.size());
            assertTrue(actual.indexOf(a.getId()) < actual.indexOf(b.getId()));
        } finally {
            removeQuietly(a.getId());
            removeQuietly(b.getId());
        }
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
        try {
            final SectionView view = find(listAs(requestFor(toolsBetaUser)), layout.getId());

            assertEquals(List.of("templates", "containers", "site-browser"), view.portletIds());
        } finally {
            removeQuietly(layout.getId());
        }
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
        Layout layout = null;
        try {
            layout = new LayoutDataGen().name("Titles " + uniq())
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
            if (layout != null) {
                removeQuietly(layout.getId());
            }
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
        try {
            final SectionView view = find(listAs(requestFor(toolsBetaUser)), layout.getId());

            assertEquals(List.of("languages"), view.portletIds());
        } finally {
            removeQuietly(layout.getId());
        }
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

    /** The 400's displayable text lives in the response entity, not in getMessage(). */
    private static String errorText(final BadRequestException e) {
        return String.valueOf(e.getResponse().getEntity()) + " " + e.getResponse().getHeaderString("error-message");
    }

    // ==================== US2 helpers ====================

    private static SectionForm form(final String name, final String icon) {
        return new SectionForm.Builder().name(name).icon(icon).build();
    }

    private static SectionView entityOf(final Response response) {
        assertEquals(200, response.getStatus());
        return ((ResponseEntitySectionView) response.getEntity()).getEntity();
    }

    private static int sectionCount() throws Exception {
        return layoutAPI.findAllLayouts().size();
    }

    // ==================== US2: POST / PUT /{id} / DELETE /{id} ====================

    /**
     * Method to test: {@link LayoutResource#create}
     * Given: a fresh name and icon
     * Expected: the saved section is returned with a new id, the given name and icon, a position
     *           after every existing section, and no tools
     */
    @Test
    public void create_appendsAfterLastSection_withNoTools_andReturnsSavedSection() throws Exception {
        final int maxBefore = layoutAPI.findAllLayouts().stream().mapToInt(Layout::getTabOrder).max().orElse(0);
        final String name = "Created " + uniq();

        final SectionView created = entityOf(resource.create(adminRequest(), response(), form(name, "bolt")));
        try {
            assertNotNull(created.id());
            assertEquals(name, created.name());
            assertEquals("bolt", created.icon());
            assertTrue(created.tabOrder() > maxBefore);
            assertTrue(created.portletIds().isEmpty());
            assertEquals(name, layoutAPI.findLayout(created.id()).getName());
        } finally {
            layoutAPI.removeLayout(layoutAPI.findLayout(created.id()));
        }
    }

    /**
     * Method to test: {@link LayoutResource#create}
     * Given: a name another section already has
     * Expected: rejected as a duplicate (400) and no section written
     */
    @Test
    public void create_duplicateName_throwsDotStateException_andWritesNothing() throws Exception {
        final Layout existing = new LayoutDataGen().name("Dup " + uniq()).tabOrder(900010).nextPersisted();
        final int before = sectionCount();
        try {
            resource.create(adminRequest(), response(), form(existing.getName(), "bolt"));
            fail("duplicate name accepted");
        } catch (final LayoutNameAlreadyExistsException expected) {
            assertEquals(before, sectionCount());
        } finally {
            // Delete through a freshly loaded instance: findAllLayouts left a Hibernate-session copy
            // of this row, and deleting the data-gen instance would collide with it.
            layoutAPI.removeLayout(layoutAPI.findLayout(existing.getId()));
        }
    }

    /**
     * Method to test: {@link LayoutResource#update}
     * Given: an existing section with tools and a position
     * Expected: name and icon change; position and tools are untouched
     */
    @Test
    public void update_changesNameAndIconOnly() throws Exception {
        final Layout layout = new LayoutDataGen().name("Upd " + uniq()).description("old-icon")
                .portletIds("templates", "containers").tabOrder(900011).nextPersisted();
        try {
            final String newName = "Upd2 " + uniq();
            final SectionView updated = entityOf(resource.update(adminRequest(), response(), layout.getId(), form(newName, "new-icon")));

            assertEquals(newName, updated.name());
            assertEquals("new-icon", updated.icon());
            assertEquals(900011, updated.tabOrder());
            assertEquals(List.of("templates", "containers"), updated.portletIds());
            assertEquals(List.of("templates", "containers"), layoutAPI.loadLayout(layout.getId()).getPortletIds());
        } finally {
            layoutAPI.removeLayout(layoutAPI.findLayout(layout.getId()));
        }
    }

    /**
     * Method to test: {@link LayoutResource#update}
     * Given: a valid new name together with an over-long icon
     * Expected: rejected as invalid and the stored name is unchanged (nothing written)
     */
    @Test
    public void update_invalidIcon_leavesNameUnchanged() throws Exception {
        final Layout layout = new LayoutDataGen().name("Icon " + uniq()).description("ok").tabOrder(900014).nextPersisted();
        try {
            resource.update(adminRequest(), response(), layout.getId(), form("Renamed " + uniq(), "i".repeat(256)));
            fail("over-long icon accepted");
        } catch (final BadRequestException expected) {
            final Layout stored = layoutAPI.findLayout(layout.getId());
            assertEquals(layout.getName(), stored.getName());
            assertEquals("ok", stored.getDescription());
        } finally {
            removeQuietly(layout.getId());
        }
    }

    /**
     * Method to test: {@link LayoutResource#update}, {@link LayoutResource#setTools}, {@link LayoutResource#delete}
     * Given: Getting Started is renamed, and an admin creates another section named "Getting Started"
     * Expected: that section is an ordinary section: it can be emptied, renamed and deleted, because
     *           Getting Started is identified by its fixed id only
     */
    @Test
    public void sectionNamedGettingStarted_underAnotherId_isOrdinary() throws Exception {
        layoutAPI.findGettingStartedLayout();
        final Layout fixed = layoutAPI.findLayout(LayoutAPI.GETTING_STARTED_LAYOUT_ID);
        final String originalName = fixed.getName();
        final String originalIcon = fixed.getDescription();
        resource.update(adminRequest(), response(), LayoutAPI.GETTING_STARTED_LAYOUT_ID,
                form("Onboarding " + uniq(), originalIcon));
        Layout named = null;
        try {
            named = new LayoutDataGen().name(LayoutAPI.GETTING_STARTED_LAYOUT_NAME)
                    .description("admin-icon").portletIds("starter").tabOrder(-6).nextPersisted();

            resource.setTools(adminRequest(), response(), named.getId(), tools());
            assertTrue(layoutAPI.loadLayout(named.getId()).getPortletIds().isEmpty());

            final SectionView renamed = entityOf(resource.update(adminRequest(), response(), named.getId(),
                    form("Renamed " + uniq(), "admin-icon")));
            assertNotEquals(LayoutAPI.GETTING_STARTED_LAYOUT_NAME, renamed.name());

            resource.delete(adminRequest(), response(), named.getId());
            final Layout gone = layoutAPI.findLayout(named.getId());
            assertTrue(gone == null || !UtilMethods.isSet(gone.getId()));
        } finally {
            if (named != null) {
                removeQuietly(named.getId());
            }
            resource.update(adminRequest(), response(), LayoutAPI.GETTING_STARTED_LAYOUT_ID,
                    form(originalName, originalIcon));
        }
    }

    /**
     * Method to test: {@link LayoutResource#update}
     * Given: an id no section has
     * Expected: not found (404)
     */
    @Test
    public void update_unknownId_throwsDoesNotExist() throws Exception {
        try {
            resource.update(adminRequest(), response(), "no-such-" + uniq(), form("X " + uniq(), ""));
            fail("unknown id accepted");
        } catch (final DoesNotExistException expected) {
            // ok
        }
    }

    /**
     * Method to test: {@link LayoutResource#update} together with {@link LayoutAPI#findGettingStartedLayout()}
     * Given: the Getting Started section is renamed and re-iconed through the resource
     * Expected: the product's own Getting Started lookup (run by the user toggle) returns the
     *           section with the new name and icon, and does not rewrite it
     */
    @Test
    public void update_gettingStarted_renameAndIconSurviveToggleLookup() throws Exception {
        layoutAPI.findGettingStartedLayout();
        final Layout original = layoutAPI.findLayout(LayoutAPI.GETTING_STARTED_LAYOUT_ID);
        final String newName = "Onboarding " + uniq();
        try {
            resource.update(adminRequest(), response(), LayoutAPI.GETTING_STARTED_LAYOUT_ID, form(newName, "rocket_launch"));

            final Layout resolved = layoutAPI.findGettingStartedLayout();
            assertEquals(LayoutAPI.GETTING_STARTED_LAYOUT_ID, resolved.getId());
            assertEquals(newName, resolved.getName());
            assertEquals("rocket_launch", resolved.getDescription());
            assertEquals(newName, layoutAPI.findLayout(LayoutAPI.GETTING_STARTED_LAYOUT_ID).getName());
        } finally {
            resource.update(adminRequest(), response(), LayoutAPI.GETTING_STARTED_LAYOUT_ID,
                    form(original.getName(), original.getDescription()));
        }
    }

    /**
     * Method to test: {@link LayoutResource#delete}
     * Given: a section granted to two roles and to one user
     * Expected: the section, its tools and every grant are gone
     */
    @Test
    public void delete_removesSection_toolRows_andRoleGrants() throws Exception {
        final Layout layout = new LayoutDataGen().name("Del " + uniq()).portletIds("templates").tabOrder(900012).nextPersisted();
        final Role roleA = new RoleDataGen().nextPersisted();
        final Role roleB = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().nextPersisted();
        final Role userRole = roleAPI.getUserRole(user);
        roleAPI.addLayoutToRole(layout, roleA);
        roleAPI.addLayoutToRole(layout, roleB);
        roleAPI.addLayoutToRole(layout, userRole);
        assertTrue(roleAPI.loadLayoutIdsForRole(roleA).contains(layout.getId()));

        try {
            final Response response = resource.delete(adminRequest(), response(), layout.getId());

            assertEquals(200, response.getStatus());
            final List<SectionView> remaining = ((ResponseEntitySectionListView) response.getEntity()).getEntity();
            assertTrue(remaining.stream().noneMatch(s -> layout.getId().equals(s.id())));
            final Layout gone = layoutAPI.findLayout(layout.getId());
            assertTrue(gone == null || !UtilMethods.isSet(gone.getId()));
            for (final Role role : List.of(roleA, roleB, userRole)) {
                assertFalse("grant left for role " + role.getName(), roleAPI.loadLayoutIdsForRole(role).contains(layout.getId()));
            }
        } finally {
            removeQuietly(layout.getId());
            UserDataGen.remove(user, true);
            RoleDataGen.remove(roleA, true);
            RoleDataGen.remove(roleB, true);
        }
    }

    /**
     * Method to test: {@link LayoutResource#delete}
     * Given: the Getting Started section
     * Expected: rejected as invalid (400) and the section is intact
     */
    @Test
    public void delete_gettingStarted_throwsBadRequest_andLeavesItIntact() throws Exception {
        layoutAPI.findGettingStartedLayout();
        try {
            resource.delete(adminRequest(), response(), LayoutAPI.GETTING_STARTED_LAYOUT_ID);
            fail("Getting Started deleted");
        } catch (final BadRequestException expected) {
            assertTrue(UtilMethods.isSet(layoutAPI.findLayout(LayoutAPI.GETTING_STARTED_LAYOUT_ID).getId()));
        }
    }

    /**
     * Method to test: {@link LayoutResource#delete}
     * Given: an id no section has
     * Expected: not found (404)
     */
    @Test
    public void delete_unknownId_throwsDoesNotExist() throws Exception {
        try {
            resource.delete(adminRequest(), response(), "no-such-" + uniq());
            fail("unknown id accepted");
        } catch (final DoesNotExistException expected) {
            // ok
        }
    }

    /**
     * Method to test: {@link LayoutResource#create}, {@link LayoutResource#update}, {@link LayoutResource#delete}
     * Given: a non-admin back-end user who holds the Tools (Beta) portlet
     * Expected: every write is refused as forbidden and nothing changes
     */
    @Test
    public void create_update_delete_toolsBetaNonAdmin_throwDotSecurityException_andWriteNothing() throws Exception {
        final Layout layout = new LayoutDataGen().name("Sec " + uniq()).tabOrder(900013).nextPersisted();
        final int before = sectionCount();
        try {
            try {
                resource.create(requestFor(toolsBetaUser), response(), form("Nope " + uniq(), ""));
                fail("non-admin create accepted");
            } catch (final DotSecurityException expected) { /* ok */ }
            try {
                resource.update(requestFor(toolsBetaUser), response(), layout.getId(), form("Nope " + uniq(), ""));
                fail("non-admin update accepted");
            } catch (final DotSecurityException expected) { /* ok */ }
            try {
                resource.delete(requestFor(toolsBetaUser), response(), layout.getId());
                fail("non-admin delete accepted");
            } catch (final DotSecurityException expected) { /* ok */ }

            assertEquals(before, sectionCount());
            assertEquals(layout.getName(), layoutAPI.findLayout(layout.getId()).getName());
        } finally {
            layoutAPI.removeLayout(layoutAPI.findLayout(layout.getId()));
        }
    }

    // ==================== US3 / US4 / US5 helpers ====================

    private static SectionToolsForm tools(final String... ids) {
        return new SectionToolsForm.Builder().portletIds(List.of(ids)).build();
    }

    private static SectionOrderForm order(final List<String> ids) {
        return new SectionOrderForm.Builder().layoutIds(ids).build();
    }

    private static List<SectionView> listOf(final Response response) {
        assertEquals(200, response.getStatus());
        return ((ResponseEntitySectionListView) response.getEntity()).getEntity();
    }

    private static List<String> currentIds() throws Exception {
        return layoutAPI.findAllLayouts().stream().map(Layout::getId).collect(Collectors.toList());
    }

    /** Snapshot of every section's position, to be restored after a reorder test. */
    private static Map<String, Integer> snapshotPositions() throws Exception {
        final Map<String, Integer> positions = new LinkedHashMap<>();
        for (final Layout layout : layoutAPI.findAllLayouts()) {
            positions.put(layout.getId(), layout.getTabOrder());
        }
        return positions;
    }

    /** Restores positions through the plain save, which exists at Red time too. */
    private static void restorePositions(final Map<String, Integer> positions) throws Exception {
        for (final Map.Entry<String, Integer> e : positions.entrySet()) {
            final Layout layout = layoutAPI.findLayout(e.getKey());
            if (layout != null && UtilMethods.isSet(layout.getId()) && layout.getTabOrder() != e.getValue()) {
                layout.setTabOrder(e.getValue());
                layoutAPI.saveLayout(layout);
            }
        }
    }

    @FunctionalInterface
    private interface Call {
        void run() throws Exception;
    }

    /** Runs the call and returns every line the security log received meanwhile. */
    private static List<String> captureSecurityLog(final Call call) throws Exception {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final String loggerName = SecurityLogger.class.getName();
        final org.apache.logging.log4j.core.Logger logger = ctx.getLogger(loggerName);
        final boolean hadOwnConfig = loggerName.equals(ctx.getConfiguration().getLoggerConfig(loggerName).getName());
        final Level previous = logger.getLevel();
        final List<String> lines = new CopyOnWriteArrayList<>();
        final AbstractAppender appender = new AbstractAppender("layouts-security-capture", null, null, true, Property.EMPTY_ARRAY) {
            @Override
            public void append(final LogEvent event) {
                lines.add(event.getMessage().getFormattedMessage());
            }
        };
        appender.start();
        Configurator.setLevel(SecurityLogger.class.getName(), Level.INFO);
        logger.addAppender(appender);
        try {
            call.run();
        } finally {
            logger.removeAppender(appender);
            appender.stop();
            if (hadOwnConfig) {
                Configurator.setLevel(loggerName, previous);
            } else {
                // Configurator.setLevel created a LoggerConfig for this name; drop it again so the
                // suite JVM ends with the configuration it started with.
                ctx.getConfiguration().removeLogger(loggerName);
                ctx.updateLoggers();
            }
        }
        return lines;
    }

    // ==================== US3: PUT /{id}/portlets ====================

    /**
     * Method to test: {@link LayoutResource#setTools}
     * Given: a section and two successive full lists
     * Expected: the section's tools become exactly each list in its order; the full section list is returned
     */
    @Test
    public void setTools_replacesListInOrder_andReturnsFullList() throws Exception {
        final Layout layout = new LayoutDataGen().name("Tools " + uniq()).portletIds("site-browser").tabOrder(900020).nextPersisted();
        try {
            List<SectionView> all = listOf(resource.setTools(adminRequest(), response(), layout.getId(),
                    tools("templates", "containers", "site-browser")));
            assertEquals(List.of("templates", "containers", "site-browser"), find(all, layout.getId()).portletIds());
            assertEquals(List.of("templates", "containers", "site-browser"), layoutAPI.loadLayout(layout.getId()).getPortletIds());

            all = listOf(resource.setTools(adminRequest(), response(), layout.getId(), tools("site-browser", "templates")));
            assertEquals(List.of("site-browser", "templates"), find(all, layout.getId()).portletIds());
            assertEquals(currentIds().size(), all.size());
        } finally {
            layoutAPI.removeLayout(layoutAPI.findLayout(layout.getId()));
        }
    }

    /**
     * Method to test: {@link LayoutResource#setTools}
     * Given: a list holding an id no portlet is registered under
     * Expected: rejected as invalid, message names the id, section unchanged
     */
    @Test
    public void setTools_unknownId_throwsBadRequest_andLeavesSectionUnchanged() throws Exception {
        final Layout layout = new LayoutDataGen().name("Tools " + uniq()).portletIds("templates").tabOrder(900021).nextPersisted();
        try {
            resource.setTools(adminRequest(), response(), layout.getId(), tools("templates", "no-such-portlet"));
            fail("unknown portlet accepted");
        } catch (final BadRequestException expected) {
            assertTrue(errorText(expected).contains("no-such-portlet"));
            assertEquals(List.of("templates"), layoutAPI.loadLayout(layout.getId()).getPortletIds());
        } finally {
            layoutAPI.removeLayout(layoutAPI.findLayout(layout.getId()));
        }
    }

    /**
     * Method to test: {@link LayoutResource#setTools}
     * Given: a registered portlet the product excludes from sections (my-account)
     * Expected: rejected as invalid
     */
    @Test
    public void setTools_notPlaceableId_throwsBadRequest() throws Exception {
        final Layout layout = new LayoutDataGen().name("Tools " + uniq()).tabOrder(900022).nextPersisted();
        try {
            resource.setTools(adminRequest(), response(), layout.getId(), tools("my-account"));
            fail("non-placeable portlet accepted");
        } catch (final BadRequestException expected) {
            assertTrue(errorText(expected).contains("my-account"));
        } finally {
            layoutAPI.removeLayout(layoutAPI.findLayout(layout.getId()));
        }
    }

    /**
     * Method to test: {@link LayoutResource#setTools}
     * Given: the same id twice
     * Expected: rejected as invalid
     */
    @Test
    public void setTools_duplicateId_throwsBadRequest() throws Exception {
        final Layout layout = new LayoutDataGen().name("Tools " + uniq()).tabOrder(900023).nextPersisted();
        try {
            resource.setTools(adminRequest(), response(), layout.getId(), tools("templates", "templates"));
            fail("duplicate portlet accepted");
        } catch (final BadRequestException expected) {
            assertTrue(errorText(expected).contains("templates"));
        } finally {
            layoutAPI.removeLayout(layoutAPI.findLayout(layout.getId()));
        }
    }

    /**
     * Method to test: {@link LayoutResource#setTools}
     * Given: an empty list on an ordinary section
     * Expected: accepted; the section keeps no tools
     */
    @Test
    public void setTools_emptyList_accepted() throws Exception {
        final Layout layout = new LayoutDataGen().name("Tools " + uniq()).portletIds("templates").tabOrder(900024).nextPersisted();
        try {
            listOf(resource.setTools(adminRequest(), response(), layout.getId(), tools()));
            assertTrue(layoutAPI.loadLayout(layout.getId()).getPortletIds().isEmpty());
        } finally {
            layoutAPI.removeLayout(layoutAPI.findLayout(layout.getId()));
        }
    }

    /**
     * Method to test: {@link LayoutResource#setTools}
     * Given: an empty list on the Getting Started section
     * Expected: rejected as invalid; the section keeps its tools
     */
    @Test
    public void setTools_emptyList_onGettingStarted_throwsBadRequest_andKeepsTools() throws Exception {
        final Layout gettingStarted = layoutAPI.findGettingStartedLayout();
        final List<String> before = gettingStarted.getPortletIds();
        assertFalse(before.isEmpty());
        try {
            resource.setTools(adminRequest(), response(), LayoutAPI.GETTING_STARTED_LAYOUT_ID, tools());
            fail("Getting Started emptied");
        } catch (final BadRequestException expected) {
            assertEquals(before, layoutAPI.loadLayout(LayoutAPI.GETTING_STARTED_LAYOUT_ID).getPortletIds());
        }
    }

    /**
     * Method to test: {@link LayoutResource#setTools}
     * Given: the old Languages tool, which the catalog hides but which is registered and placeable
     * Expected: accepted
     */
    @Test
    public void setTools_hiddenLanguages_accepted() throws Exception {
        final Layout layout = new LayoutDataGen().name("Tools " + uniq()).tabOrder(900025).nextPersisted();
        try {
            listOf(resource.setTools(adminRequest(), response(), layout.getId(), tools("languages")));
            assertEquals(List.of("languages"), layoutAPI.loadLayout(layout.getId()).getPortletIds());
        } finally {
            layoutAPI.removeLayout(layoutAPI.findLayout(layout.getId()));
        }
    }

    /**
     * Method to test: {@link LayoutResource#setTools}
     * Given: an id no section has
     * Expected: not found
     */
    @Test
    public void setTools_unknownSection_throwsDoesNotExist() throws Exception {
        try {
            resource.setTools(adminRequest(), response(), "no-such-" + uniq(), tools("templates"));
            fail("unknown section accepted");
        } catch (final DoesNotExistException expected) {
            // ok
        }
    }

    /**
     * Method to test: {@link LayoutResource#setTools}
     * Given: a non-admin Tools (Beta) holder
     * Expected: refused as forbidden; section unchanged
     */
    @Test
    public void setTools_toolsBetaNonAdmin_throwsDotSecurityException() throws Exception {
        final Layout layout = new LayoutDataGen().name("Tools " + uniq()).portletIds("templates").tabOrder(900026).nextPersisted();
        try {
            resource.setTools(requestFor(toolsBetaUser), response(), layout.getId(), tools("containers"));
            fail("non-admin set-tools accepted");
        } catch (final DotSecurityException expected) {
            assertEquals(List.of("templates"), layoutAPI.loadLayout(layout.getId()).getPortletIds());
        } finally {
            layoutAPI.removeLayout(layoutAPI.findLayout(layout.getId()));
        }
    }

    // ==================== US4: PUT /_reorder ====================

    /**
     * Method to test: {@link LayoutResource#reorder}
     * Given: every section id in reverse order
     * Expected: the sections come back in that order with strictly increasing positions
     */
    @Test
    public void reorder_rewritesPositionsStrictlyIncreasing_inSentOrder() throws Exception {
        final Map<String, Integer> snapshot = snapshotPositions();
        try {
            final List<String> reversed = new ArrayList<>(snapshot.keySet());
            Collections.reverse(reversed);

            final List<SectionView> returned = listOf(resource.reorder(adminRequest(), response(), order(reversed)));

            assertEquals(reversed, returned.stream().map(SectionView::id).collect(Collectors.toList()));
            assertEquals(reversed, currentIds());
            final List<Layout> stored = layoutAPI.findAllLayouts();
            for (int i = 1; i < stored.size(); i++) {
                assertTrue("positions not strictly increasing", stored.get(i - 1).getTabOrder() < stored.get(i).getTabOrder());
            }
        } finally {
            restorePositions(snapshot);
        }
    }

    /**
     * Method to test: {@link LayoutResource#reorder}
     * Given: a list that omits one existing section
     * Expected: rejected as invalid and no position changes
     */
    @Test
    public void reorder_missingId_throwsBadRequest_andChangesNoPosition() throws Exception {
        final Map<String, Integer> snapshot = snapshotPositions();
        final List<String> incomplete = new ArrayList<>(snapshot.keySet());
        final String dropped = incomplete.remove(incomplete.size() - 1);
        try {
            resource.reorder(adminRequest(), response(), order(incomplete));
            fail("incomplete list accepted");
        } catch (final BadRequestException expected) {
            assertTrue(errorText(expected).contains(dropped));
            assertEquals(snapshot, snapshotPositions());
        }
    }

    /**
     * Method to test: {@link LayoutResource#reorder}
     * Given: a list with an id no section has
     * Expected: rejected as invalid
     */
    @Test
    public void reorder_unknownId_throwsBadRequest() throws Exception {
        final List<String> withUnknown = new ArrayList<>(currentIds());
        withUnknown.add("no-such-" + uniq());
        try {
            resource.reorder(adminRequest(), response(), order(withUnknown));
            fail("unknown id accepted");
        } catch (final BadRequestException expected) {
            // ok
        }
    }

    /**
     * Method to test: {@link LayoutResource#reorder}
     * Given: a list that repeats an id
     * Expected: rejected as invalid
     */
    @Test
    public void reorder_duplicateId_throwsBadRequest() throws Exception {
        final List<String> withDuplicate = new ArrayList<>(currentIds());
        withDuplicate.add(withDuplicate.get(0));
        try {
            resource.reorder(adminRequest(), response(), order(withDuplicate));
            fail("duplicate id accepted");
        } catch (final BadRequestException expected) {
            // ok
        }
    }

    /**
     * Method to test: {@link LayoutResource#reorder} together with {@link LayoutAPI#findGettingStartedLayout()}
     * Given: Getting Started placed last
     * Expected: it is last, and the product's own lookup keeps it there
     */
    @Test
    public void reorder_gettingStartedMovable() throws Exception {
        layoutAPI.findGettingStartedLayout();
        final Map<String, Integer> snapshot = snapshotPositions();
        try {
            final List<String> ids = new ArrayList<>(snapshot.keySet());
            ids.remove(LayoutAPI.GETTING_STARTED_LAYOUT_ID);
            ids.add(LayoutAPI.GETTING_STARTED_LAYOUT_ID);

            listOf(resource.reorder(adminRequest(), response(), order(ids)));

            final List<String> after = currentIds();
            assertEquals(LayoutAPI.GETTING_STARTED_LAYOUT_ID, after.get(after.size() - 1));
            final int stored = layoutAPI.findLayout(LayoutAPI.GETTING_STARTED_LAYOUT_ID).getTabOrder();
            assertEquals(stored, layoutAPI.findGettingStartedLayout().getTabOrder());
        } finally {
            restorePositions(snapshot);
        }
    }

    /**
     * Method to test: {@link LayoutResource#reorder}
     * Given: a non-admin Tools (Beta) holder
     * Expected: refused as forbidden; no position changes
     */
    @Test
    public void reorder_toolsBetaNonAdmin_throwsDotSecurityException() throws Exception {
        final Map<String, Integer> snapshot = snapshotPositions();
        final List<String> reversed = new ArrayList<>(snapshot.keySet());
        Collections.reverse(reversed);
        try {
            resource.reorder(requestFor(toolsBetaUser), response(), order(reversed));
            fail("non-admin reorder accepted");
        } catch (final DotSecurityException expected) {
            assertEquals(snapshot, snapshotPositions());
        }
    }

    // ==================== US5: gate matrix and audit ====================

    private List<Call> writesAs(final HttpServletRequest request, final String layoutId) throws Exception {
        return List.of(
                () -> resource.create(request, response(), form("Nope " + uniq(), "")),
                () -> resource.update(request, response(), layoutId, form("Nope " + uniq(), "")),
                () -> resource.delete(request, response(), layoutId),
                () -> resource.reorder(request, response(), order(currentIds())),
                () -> resource.setTools(request, response(), layoutId, tools("templates")));
    }

    /**
     * Method to test: every write of {@link LayoutResource}
     * Given: a back-end user with neither tools nor tools-beta
     * Expected: the portlet gate rejects each call with the REST SecurityException (401)
     */
    @Test
    public void everyWrite_noPortletUser_throwsRestSecurityException() throws Exception {
        final Layout layout = new LayoutDataGen().name("Gate " + uniq()).tabOrder(900030).nextPersisted();
        try {
            for (final Call write : writesAs(requestFor(noPortletUser), layout.getId())) {
                try {
                    write.run();
                    fail("write accepted without a portlet grant");
                } catch (final com.dotcms.rest.exception.SecurityException expected) {
                    assertEquals(401, expected.getResponse().getStatus());
                }
            }
        } finally {
            layoutAPI.removeLayout(layoutAPI.findLayout(layout.getId()));
        }
    }

    /**
     * Method to test: every write of {@link LayoutResource}
     * Given: no credentials
     * Expected: rejected (REST SecurityException, 401)
     */
    @Test
    public void everyWrite_anonymous_throwsRestSecurityException() throws Exception {
        final Layout layout = new LayoutDataGen().name("Gate " + uniq()).tabOrder(900031).nextPersisted();
        try {
            for (final Call write : writesAs(anonymousRequest(), layout.getId())) {
                try {
                    write.run();
                    fail("anonymous write accepted");
                } catch (final com.dotcms.rest.exception.SecurityException expected) {
                    assertEquals(401, expected.getResponse().getStatus());
                }
            }
        } finally {
            layoutAPI.removeLayout(layoutAPI.findLayout(layout.getId()));
        }
    }

    /**
     * Method to test: every endpoint of {@link LayoutResource}
     * Given: a CMS Administrator with no explicit Tools grant
     * Expected: every call succeeds (administrator fallback)
     */
    @Test
    public void everyEndpoint_adminWithoutToolsGrant_passes() throws Exception {
        final Map<String, Integer> snapshot = snapshotPositions();
        final SectionView created = entityOf(resource.create(adminRequest(), response(), form("Admin " + uniq(), "bolt")));
        try {
            entityOf(resource.update(adminRequest(), response(), created.id(), form("Admin2 " + uniq(), "bolt")));
            listOf(resource.setTools(adminRequest(), response(), created.id(), tools("templates")));
            listOf(resource.reorder(adminRequest(), response(), order(currentIds())));
            listAs(adminRequest());
        } finally {
            resource.delete(adminRequest(), response(), created.id());
            restorePositions(snapshot);
        }
    }

    /**
     * Method to test: {@link LayoutResource#create} and {@link LayoutResource#delete} audit
     * Given: a successful create and delete by an administrator
     * Expected: the security log holds a line for each with the operation and the acting user
     */
    @Test
    public void everyWrite_success_writesSecurityLogLine() throws Exception {
        final String[] createdId = new String[1];
        final List<String> lines = captureSecurityLog(() -> {
            createdId[0] = entityOf(resource.create(adminRequest(), response(), form("Audit " + uniq(), ""))).id();
            resource.delete(adminRequest(), response(), createdId[0]);
        });
        assertTrue("no create line: " + lines, lines.stream().anyMatch(l -> l.contains("navigation section create " + createdId[0]) && l.contains("by user")));
        assertTrue("no delete line: " + lines, lines.stream().anyMatch(l -> l.contains("navigation section delete " + createdId[0]) && l.contains("by user")));
    }

    /**
     * Method to test: {@link LayoutResource#create} audit on refusal
     * Given: a non-admin Tools (Beta) holder attempts a write
     * Expected: the security log holds a line naming the user and the attempt
     */
    @Test
    public void everyWrite_nonAdminRefusal_writesSecurityLogLine() throws Exception {
        final List<String> lines = captureSecurityLog(() -> {
            try {
                resource.create(requestFor(toolsBetaUser), response(), form("Audit " + uniq(), ""));
                fail("non-admin create accepted");
            } catch (final DotSecurityException expected) {
                // ok
            }
        });
        assertTrue("no refusal line: " + lines, lines.stream().anyMatch(l ->
                l.contains("unauthorized attempt to create navigation section by user " + toolsBetaUser.getUserId())));
    }
}
