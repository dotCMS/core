package com.dotcms.rest.api.v1.layout;

import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.portal.PortletAPI;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;


import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.business.LayoutNameAlreadyExistsException;
import com.dotmarketing.exception.DotDataException;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LayoutHelper}: the pure logic behind the {@code /v1/layouts} endpoints
 * (title fallback, view mapping, input validation and position arithmetic) for issue #37353.
 * {@code LayoutAPI} and {@code PortletAPI} are mocked and the title lookup is stubbed, so no
 * message bundle or database is involved.
 *
 * @author hassandotcms
 */
public class LayoutHelperTest {

    private static final String TITLE_PREFIX = "com.dotcms.repackage.javax.portlet.title.";

    private LayoutAPI layoutApi;
    private PortletAPI portletApi;
    private Map<String, String> titles;
    private LayoutHelper helper;
    private final User user = new User();

    @Before
    public void setUp() {
        layoutApi = mock(LayoutAPI.class);
        portletApi = mock(PortletAPI.class);
        titles = new HashMap<>();
        // Resolver behaves like LanguageUtil.get: returns the key itself on a miss.
        final BiFunction<User, String, String> resolver = (u, key) -> titles.getOrDefault(key, key);
        helper = new LayoutHelper(layoutApi, portletApi, resolver);
    }

    /** The 400's displayable text lives in the response entity, not in getMessage(). */
    private static String errorText(final BadRequestException e) {
        return String.valueOf(e.getResponse().getEntity()) + " " + e.getResponse().getHeaderString("error-message");
    }

    // ==================== helpers ====================

    private Portlet registered(final String id, final String name) {
        final Map<String, String> params = new HashMap<>();
        if (name != null) {
            params.put("name", name);
        }
        final Portlet portlet = new Portlet(id, "com.liferay.portlet.JSPPortlet", params);
        when(portletApi.findPortlet(id)).thenReturn(portlet);
        return portlet;
    }

    private static Layout layout(final String id, final String name, final String icon,
                                 final int tabOrder, final String... portletIds) {
        final Layout layout = new Layout();
        layout.setId(id);
        layout.setName(name);
        layout.setDescription(icon);
        layout.setTabOrder(tabOrder);
        layout.setPortletIds(Arrays.asList(portletIds));
        return layout;
    }

    // ==================== US1: titles and views ====================

    /**
     * Method to test: {@link LayoutHelper#title(User, String)}
     * Given: a translation exists for the portlet's title key
     * Expected: the translation is returned
     */
    @Test
    public void title_translationWins() {
        registered("templates", "Some Registered Name");
        titles.put(TITLE_PREFIX + "templates", "Templates");

        assertEquals("Templates", helper.title(user, "templates"));
    }

    /**
     * Method to test: {@link LayoutHelper#title(User, String)}
     * Given: no translation (the resolver returns the key) and a portlet registered with a name
     * Expected: the registered name is returned, never the raw key
     */
    @Test
    public void title_fallsBackToRegisteredName_whenKeyReturned() {
        registered("c_press", "Press Releases");

        assertEquals("Press Releases", helper.title(user, "c_press"));
    }

    /**
     * Method to test: {@link LayoutHelper#title(User, String)}
     * Given: no translation and a portlet with no registered name, or no portlet at all
     * Expected: the id is returned, never the raw key
     */
    @Test
    public void title_fallsBackToId_whenNoName() {
        registered("c_noname", null);

        assertEquals("c_noname", helper.title(user, "c_noname"));
        assertEquals("gone-plugin", helper.title(user, "gone-plugin"));
    }

    /**
     * Method to test: {@link LayoutHelper#toView(Layout, User)}
     * Given: a layout whose description holds the icon and whose portlet ids are in a known order
     * Expected: icon comes from description, and portletTitles is aligned index by index with portletIds
     */
    @Test
    public void toView_mapsDescriptionToIcon_andAlignsTitlesWithPortletIds() {
        registered("templates", null);
        registered("c_press", "Press Releases");
        titles.put(TITLE_PREFIX + "templates", "Templates");
        final Layout layout = layout("id-1", "Site", "language", 3, "templates", "c_press", "unknown-id");

        final SectionView view = helper.toView(layout, user);

        assertEquals("id-1", view.id());
        assertEquals("Site", view.name());
        assertEquals("language", view.icon());
        assertEquals(3, view.tabOrder());
        assertEquals(List.of("templates", "c_press", "unknown-id"), view.portletIds());
        assertEquals(List.of("Templates", "Press Releases", "unknown-id"), view.portletTitles());
    }

    /**
     * Method to test: {@link LayoutHelper#toViews(List, User)}
     * Given: layouts in a given order
     * Expected: views come back in the same order, one per layout
     */
    @Test
    public void toViews_preservesInputOrder() {
        final List<Layout> layouts = List.of(
                layout("b", "B", "", 2),
                layout("a", "A", "", 1),
                layout("c", "C", "", 3));

        final List<SectionView> views = helper.toViews(layouts, user);

        assertEquals(List.of("b", "a", "c"), views.stream().map(SectionView::id).toList());
    }

    // ==================== US2: name, icon, position, duplicate mapping ====================

    /**
     * Method to test: {@link LayoutHelper#validateName(String)}
     * Given: a name with surrounding whitespace
     * Expected: the trimmed name
     */
    @Test
    public void validateName_trims() {
        assertEquals("Marketing", helper.validateName("  Marketing  "));
    }

    /**
     * Method to test: {@link LayoutHelper#validateName(String)}
     * Given: null, empty or whitespace-only names
     * Expected: rejected as invalid
     */
    @Test
    public void validateName_rejectsBlank() {
        for (final String blank : new String[]{null, "", "   "}) {
            try {
                helper.validateName(blank);
                fail("blank name accepted: [" + blank + "]");
            } catch (final BadRequestException expected) {
                // ok
            }
        }
    }

    /**
     * Method to test: {@link LayoutHelper#validateName(String)}
     * Given: a 256-character name (255 is the column limit)
     * Expected: rejected as invalid; 255 characters accepted
     */
    @Test
    public void validateName_rejects256Chars() {
        assertEquals(255, helper.validateName("n".repeat(255)).length());
        try {
            helper.validateName("n".repeat(256));
            fail("256-character name accepted");
        } catch (final BadRequestException expected) {
            // ok
        }
    }

    /**
     * Method to test: {@link LayoutHelper#validateIcon(String)}
     * Given: a null or empty icon
     * Expected: the empty string, accepted
     */
    @Test
    public void validateIcon_allowsEmpty() {
        assertEquals("", helper.validateIcon(null));
        assertEquals("", helper.validateIcon(""));
        assertEquals("campaign", helper.validateIcon("campaign"));
    }

    /**
     * Method to test: {@link LayoutHelper#validateIcon(String)}
     * Given: a 256-character icon
     * Expected: rejected as invalid
     */
    @Test
    public void validateIcon_rejects256Chars() {
        try {
            helper.validateIcon("i".repeat(256));
            fail("256-character icon accepted");
        } catch (final BadRequestException expected) {
            // ok
        }
    }

    /**
     * Method to test: {@link LayoutHelper#nextTabOrder(List)}
     * Given: existing sections with positions -320000, 5 and 12
     * Expected: 13
     */
    @Test
    public void nextTabOrder_isMaxPlusOne() {
        final List<Layout> existing = List.of(layout("gs", "GS", "", -320000), layout("a", "A", "", 5),
                layout("b", "B", "", 12));
        assertEquals(13, helper.nextTabOrder(existing));
    }

    /**
     * Method to test: {@link LayoutHelper#nextTabOrder(List)}
     * Given: no sections
     * Expected: 1
     */
    @Test
    public void nextTabOrder_isOne_whenNoSections() {
        assertEquals(1, helper.nextTabOrder(List.of()));
    }

    /**
     * Method to test: {@link LayoutHelper#saveOrDuplicate(Layout)}
     * Given: the layout API fails with a data exception caused by a SQL unique violation (state 23505)
     * Expected: reported as the duplicate-name error, so the caller answers 400 not 500
     */
    @Test
    public void duplicateFromDatabase_isReportedAsLayoutNameAlreadyExists() throws Exception {
        final DotDataException dbFailure = new DotDataException("insert failed",
                new SQLException("duplicate key value violates unique constraint \"cms_layout_name_parent\"", "23505"));
        doThrow(dbFailure).when(layoutApi).saveLayout(any(Layout.class));

        try {
            helper.saveOrDuplicate(layout("x", "Marketing", "", 1));
            fail("unique violation not mapped");
        } catch (final LayoutNameAlreadyExistsException expected) {
            assertTrue(expected.getMessage().contains("Marketing"));
        }
    }

    /**
     * Method to test: {@link LayoutHelper#saveOrDuplicate(Layout)}
     * Given: the layout API fails for an unrelated reason
     * Expected: the failure propagates unchanged
     */
    @Test
    public void otherDatabaseFailure_propagates() throws Exception {
        final DotDataException dbFailure = new DotDataException("connection lost", new SQLException("boom", "08006"));
        doThrow(dbFailure).when(layoutApi).saveLayout(any(Layout.class));

        try {
            helper.saveOrDuplicate(layout("x", "Marketing", "", 1));
            fail("unrelated failure swallowed");
        } catch (final LayoutNameAlreadyExistsException wrong) {
            fail("unrelated failure mapped to duplicate name");
        } catch (final RuntimeException | DotDataException expected) {
            // ok: propagated (wrapped or not), never as a duplicate
        }
    }

    // ==================== US3: tool list validation ====================

    private void placeable(final String... ids) {
        for (final String id : ids) {
            registered(id, null);
            when(portletApi.canAddPortletToLayout(id)).thenReturn(true);
        }
    }

    /**
     * Method to test: {@link LayoutHelper#validateToolIds(List)}
     * Given: ids of registered, placeable portlets
     * Expected: accepted, order preserved
     */
    @Test
    public void validateToolIds_acceptsRegisteredPlaceable() {
        placeable("templates", "containers");
        assertEquals(List.of("templates", "containers"), helper.validateToolIds(List.of("templates", "containers")));
    }

    /**
     * Method to test: {@link LayoutHelper#validateToolIds(List)}
     * Given: an id no portlet is registered under
     * Expected: rejected, message names the id
     */
    @Test
    public void validateToolIds_rejectsUnknown_namingId() {
        placeable("templates");
        try {
            helper.validateToolIds(List.of("templates", "gone-plugin"));
            fail("unknown id accepted");
        } catch (final BadRequestException expected) {
            assertTrue(errorText(expected).contains("gone-plugin"));
        }
    }

    /**
     * Method to test: {@link LayoutHelper#validateToolIds(List)}
     * Given: a registered portlet the product marks as not placeable in a section
     * Expected: rejected, message names the id
     */
    @Test
    public void validateToolIds_rejectsNotPlaceable_namingId() {
        registered("my-account", null);
        when(portletApi.canAddPortletToLayout("my-account")).thenReturn(false);
        try {
            helper.validateToolIds(List.of("my-account"));
            fail("non-placeable id accepted");
        } catch (final BadRequestException expected) {
            assertTrue(errorText(expected).contains("my-account"));
        }
    }

    /**
     * Method to test: {@link LayoutHelper#validateToolIds(List)}
     * Given: the same id twice
     * Expected: rejected, message names the id
     */
    @Test
    public void validateToolIds_rejectsDuplicate_namingId() {
        placeable("templates");
        try {
            helper.validateToolIds(List.of("templates", "templates"));
            fail("duplicate id accepted");
        } catch (final BadRequestException expected) {
            assertTrue(errorText(expected).contains("templates"));
        }
    }

    /**
     * Method to test: {@link LayoutHelper#validateToolIds(List)}
     * Given: an empty list
     * Expected: accepted
     */
    @Test
    public void validateToolIds_acceptsEmpty() {
        assertEquals(List.of(), helper.validateToolIds(List.of()));
    }

    /**
     * Method to test: {@link LayoutHelper#validateToolIds(List)}
     * Given: the old Languages tool, registered and placeable though the catalog hides it
     * Expected: accepted, because validation is about registration and placeability, not catalog membership
     */
    @Test
    public void validateToolIds_acceptsLanguages_whenRegisteredAndPlaceable() {
        placeable("languages");
        assertEquals(List.of("languages"), helper.validateToolIds(List.of("languages")));
    }

    // ==================== US4: reorder validation and positions ====================

    private static List<Layout> three() {
        return List.of(layout("a", "A", "", 1), layout("b", "B", "", 2), layout("c", "C", "", 3));
    }

    /**
     * Method to test: {@link LayoutHelper#validateOrder(List, List)}
     * Given: exactly the existing ids, in any order
     * Expected: accepted
     */
    @Test
    public void validateOrder_acceptsExactSet() {
        helper.validateOrder(List.of("c", "a", "b"), three());
    }

    /**
     * Method to test: {@link LayoutHelper#validateOrder(List, List)}
     * Given: a list that omits an existing id
     * Expected: rejected, message names the missing id
     */
    @Test
    public void validateOrder_rejectsMissingId_namingIt() {
        try {
            helper.validateOrder(List.of("c", "a"), three());
            fail("missing id accepted");
        } catch (final BadRequestException expected) {
            assertTrue(errorText(expected).contains("b"));
        }
    }

    /**
     * Method to test: {@link LayoutHelper#validateOrder(List, List)}
     * Given: a list with an id no section has
     * Expected: rejected, message names the unknown id
     */
    @Test
    public void validateOrder_rejectsUnknownId_namingIt() {
        try {
            helper.validateOrder(List.of("c", "a", "b", "zz"), three());
            fail("unknown id accepted");
        } catch (final BadRequestException expected) {
            assertTrue(errorText(expected).contains("zz"));
        }
    }

    /**
     * Method to test: {@link LayoutHelper#validateOrder(List, List)}
     * Given: a list that repeats an id
     * Expected: rejected, message names the repeated id
     */
    @Test
    public void validateOrder_rejectsDuplicateId_namingIt() {
        try {
            helper.validateOrder(List.of("a", "a", "b", "c"), three());
            fail("duplicate id accepted");
        } catch (final BadRequestException expected) {
            assertTrue(errorText(expected).contains("a"));
        }
    }

    /**
     * Method to test: {@link LayoutHelper#positions(List)}
     * Given: a validated order
     * Expected: 1-based positions in list order
     */
    @Test
    public void positions_assignOneBasedPositionsInListOrder() {
        final Map<String, Integer> positions = helper.positions(List.of("c", "a", "b"));
        assertEquals(Integer.valueOf(1), positions.get("c"));
        assertEquals(Integer.valueOf(2), positions.get("a"));
        assertEquals(Integer.valueOf(3), positions.get("b"));
        assertEquals(3, positions.size());
    }

    // ==================== Getting Started identity ====================

    /**
     * Method to test: {@link LayoutHelper#isGettingStarted(Layout)} / {@link LayoutHelper#isLegacyGettingStarted(Layout)}
     * Given: the section holding the fixed id
     * Expected: it is Getting Started, and not a legacy one
     */
    @Test
    public void gettingStarted_byFixedId_isNotLegacy() throws Exception {
        final Layout fixed = layout(LayoutAPI.GETTING_STARTED_LAYOUT_ID, "Renamed", "", -320000, "starter");
        assertTrue(helper.isGettingStarted(fixed));
        assertFalse(helper.isLegacyGettingStarted(fixed));
    }

    /**
     * Method to test: {@link LayoutHelper#isLegacyGettingStarted(Layout)}
     * Given: a section named "Getting Started" under another id, and no section holds the fixed id
     * Expected: it is a legacy Getting Started; once a fixed-id section exists it is an ordinary section
     */
    @Test
    public void gettingStarted_byNameOnly_isLegacy_untilFixedIdExists() throws Exception {
        final Layout legacy = layout("legacy-id", LayoutAPI.GETTING_STARTED_LAYOUT_NAME, "", -5, "starter");
        when(layoutApi.findLayout(LayoutAPI.GETTING_STARTED_LAYOUT_ID)).thenReturn(new Layout());
        assertTrue(helper.isLegacyGettingStarted(legacy));

        when(layoutApi.findLayout(LayoutAPI.GETTING_STARTED_LAYOUT_ID))
                .thenReturn(layout(LayoutAPI.GETTING_STARTED_LAYOUT_ID, "Getting Started", "", -320000, "starter"));
        assertFalse(helper.isGettingStarted(legacy));
        assertFalse(helper.isLegacyGettingStarted(legacy));
    }
}
