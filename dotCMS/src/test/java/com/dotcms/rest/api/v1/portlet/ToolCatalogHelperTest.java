package com.dotcms.rest.api.v1.portlet;

import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.business.portal.PortletAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PortletID;
import com.fasterxml.jackson.databind.JsonNode;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ToolCatalogHelper}: the pure logic behind the tools catalog (inclusion
 * rules, title fallback, ordering, custom flag) and the custom-tool read model (issue #37574).
 * The {@code PortletAPI} is mocked and the title lookup is stubbed, so no message bundle or
 * database is involved.
 *
 * @author hassandotcms
 */
public class ToolCatalogHelperTest {

    private static final String TITLE_PREFIX = "com.dotcms.repackage.javax.portlet.title.";

    private PortletAPI portletApi;
    private Map<String, String> titles;
    private ToolCatalogHelper helper;
    private final User user = new User();

    @BeforeClass
    public static void initConfig() {
        Config.initializeConfig();
    }

    @Before
    public void setUp() {
        portletApi = mock(PortletAPI.class);
        titles = new HashMap<>();
        // Resolver behaves like LanguageUtil.get: returns the key itself on a miss.
        final BiFunction<User, String, String> resolver =
                (u, key) -> titles.getOrDefault(key, key);
        helper = new ToolCatalogHelper(portletApi, resolver);
        when(portletApi.canAddPortletToLayout(any(Portlet.class))).thenReturn(true);
    }

    private static Portlet portlet(final String id, final Map<String, String> initParams) {
        return new Portlet(id, "com.liferay.portlet.StrutsPortlet", new HashMap<>(initParams));
    }

    private static Portlet portlet(final String id) {
        return portlet(id, Map.of());
    }

    private void allPortlets(final Portlet... portlets) throws Exception {
        when(portletApi.findAllPortlets()).thenReturn(Arrays.asList(portlets));
    }

    private List<String> ids(final List<ToolCatalogEntryView> rows) {
        return rows.stream().map(ToolCatalogEntryView::id).collect(Collectors.toList());
    }

    /**
     * Given Scenario: the flag that hides the old Languages tool is on (its default).
     * Expected Result: the {@code languages} portlet is not in the catalog.
     */
    @Test
    public void catalog_excludesLanguagesWhenFlagOn() throws Exception {
        allPortlets(portlet(PortletID.LANGUAGES.toString()), portlet("roles"));
        Config.setProperty(FeatureFlagName.FEATURE_FLAG_LOCALES_HIDE_OLD_LANGUAGES_PORTLET, true);
        try {
            assertEquals(List.of("roles"), ids(helper.catalog(user)));
        } finally {
            Config.setProperty(FeatureFlagName.FEATURE_FLAG_LOCALES_HIDE_OLD_LANGUAGES_PORTLET, true);
        }
    }

    /**
     * Given Scenario: the flag that hides the old Languages tool is turned off at runtime.
     * Expected Result: the {@code languages} portlet is in the catalog; the flag is read on every
     * call, not cached at first use.
     */
    @Test
    public void catalog_includesLanguagesWhenFlagOff() throws Exception {
        allPortlets(portlet(PortletID.LANGUAGES.toString()), portlet("roles"));
        Config.setProperty(FeatureFlagName.FEATURE_FLAG_LOCALES_HIDE_OLD_LANGUAGES_PORTLET, true);
        try {
            assertFalse(ids(helper.catalog(user)).contains("languages"));
            Config.setProperty(FeatureFlagName.FEATURE_FLAG_LOCALES_HIDE_OLD_LANGUAGES_PORTLET, false);
            assertTrue(ids(helper.catalog(user)).contains("languages"));
        } finally {
            Config.setProperty(FeatureFlagName.FEATURE_FLAG_LOCALES_HIDE_OLD_LANGUAGES_PORTLET, true);
        }
    }

    /**
     * Given Scenario: a portlet the product marks as not placeable in a section.
     * Expected Result: it is absent, exactly as the legacy picker omits it.
     */
    @Test
    public void catalog_excludesNotPlaceable() throws Exception {
        final Portlet myAccount = portlet("my-account");
        allPortlets(myAccount, portlet("roles"));
        when(portletApi.canAddPortletToLayout(myAccount)).thenReturn(false);

        assertEquals(List.of("roles"), ids(helper.catalog(user)));
    }

    /**
     * Given Scenario: titles that differ only in case, plus two tools with the same title.
     * Expected Result: sorted by title ignoring letter case, equal titles ordered by id.
     */
    @Test
    public void catalog_sortsByTitleIgnoringCase_thenById() throws Exception {
        allPortlets(portlet("b"), portlet("a"), portlet("z2"), portlet("z1"));
        titles.put(TITLE_PREFIX + "b", "banana");
        titles.put(TITLE_PREFIX + "a", "Apple");
        titles.put(TITLE_PREFIX + "z2", "Zebra");
        titles.put(TITLE_PREFIX + "z1", "zebra");

        assertEquals(List.of("a", "b", "z1", "z2"), ids(helper.catalog(user)));
    }

    /**
     * Given Scenario: a tool with a translation, a tool without one but with a registered name,
     * and a tool with neither.
     * Expected Result: translation, then registered name, then id; never the raw key.
     */
    @Test
    public void catalog_titleFallsBackToNameThenId() throws Exception {
        allPortlets(portlet("translated"),
                portlet("c_named", Map.of("name", "Press Releases")),
                portlet("bare"));
        titles.put(TITLE_PREFIX + "translated", "Translated Title");

        final Map<String, String> byId = helper.catalog(user).stream()
                .collect(Collectors.toMap(ToolCatalogEntryView::id, ToolCatalogEntryView::title));
        assertEquals("Translated Title", byId.get("translated"));
        assertEquals("Press Releases", byId.get("c_named"));
        assertEquals("bare", byId.get("bare"));
        assertFalse(byId.values().stream().anyMatch(t -> t.startsWith(TITLE_PREFIX)));
    }

    /**
     * Given Scenario: the API classifies one portlet as custom and another as shipped.
     * Expected Result: the flag on each row is exactly what the API answered.
     */
    @Test
    public void catalog_isCustomComesFromPredicate() throws Exception {
        final Portlet custom = portlet("c_custom");
        final Portlet shipped = portlet("roles");
        allPortlets(custom, shipped);
        when(portletApi.isCustomContentPortlet(custom)).thenReturn(true);
        when(portletApi.isCustomContentPortlet(shipped)).thenReturn(false);

        final Map<String, Boolean> byId = helper.catalog(user).stream()
                .collect(Collectors.toMap(ToolCatalogEntryView::id, ToolCatalogEntryView::isCustom));
        assertTrue(byId.get("c_custom"));
        assertFalse(byId.get("roles"));
    }

    /**
     * Given Scenario: a catalog row is serialized with the REST object mapper.
     * Expected Result: the JSON keys are exactly {@code id}, {@code title}, {@code isCustom}, the
     * names the frontend model expects.
     */
    @Test
    public void catalogEntry_serializesIsCustomKey() throws Exception {
        final JsonNode json = DotObjectMapperProvider.getInstance().getDefaultObjectMapper()
                .valueToTree(new ToolCatalogEntryView("c_x", "X", true));

        assertEquals("c_x", json.get("id").asText());
        assertEquals("X", json.get("title").asText());
        assertTrue(json.get("isCustom").asBoolean());
        assertEquals(3, json.size());
    }

    // ==================== US3: toCustomToolView ====================

    private static Portlet customTool(final String baseTypes, final String contentTypes, final String mode) {
        final Map<String, String> params = new HashMap<>();
        params.put("name", "Press Releases");
        params.put("portletSource", "db");
        if (null != baseTypes) {
            params.put("baseTypes", baseTypes);
        }
        if (null != contentTypes) {
            params.put("contentTypes", contentTypes);
        }
        params.put("dataViewMode", mode);
        return portlet("c_press", params);
    }

    /**
     * Given Scenario: base and content types stored as comma-separated text with stray spaces
     * and an empty item.
     * Expected Result: trimmed lists with no blanks.
     */
    @Test
    public void customView_splitsCommaListsAndTrims() {
        final CustomToolView view = helper.toCustomToolView(customTool("CONTENT, PERSONA", "news,, blog ", "list"));

        assertEquals(List.of("CONTENT", "PERSONA"), view.baseTypes());
        assertEquals(List.of("news", "blog"), view.contentTypes());
    }

    /**
     * Given Scenario: a tool with an empty base type list and no content type param at all.
     * Expected Result: both lists are empty, never null.
     */
    @Test
    public void customView_blankListsBecomeEmpty() {
        final CustomToolView view = helper.toCustomToolView(customTool("", null, "list"));

        assertEquals(List.of(), view.baseTypes());
        assertEquals(List.of(), view.contentTypes());
    }

    /**
     * Given Scenario: a tool stored with the card view mode.
     * Expected Result: {@code card}, exactly as stored, and the stored id and name.
     */
    @Test
    public void customView_keepsDataViewModeAsStored_andUsesStoredIdAndName() {
        final CustomToolView view = helper.toCustomToolView(customTool("CONTENT", "", "card"));

        assertEquals("card", view.dataViewMode());
        assertEquals("c_press", view.portletId());
        assertEquals("Press Releases", view.portletName());
    }
}
