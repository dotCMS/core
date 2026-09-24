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
}
