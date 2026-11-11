package com.dotcms.rest.api.v1.portlet;

import com.dotcms.featureflag.FeatureFlagName;
import com.dotmarketing.business.portal.PortletAPI;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Builds the read models the Tools portlet consumes: the tools catalog (every tool that can be
 * placed in a navigation section) and the configuration of one custom content tool. Holds the
 * inclusion, title and ordering rules the legacy Roles &amp; Tools picker applies, so both
 * surfaces show the same list.
 *
 * @author hassandotcms
 */
public class ToolCatalogHelper {

    /** Prefix of the message key that holds a portlet's localized title. */
    static final String TITLE_KEY_PREFIX = "com.dotcms.repackage.javax.portlet.title.";

    private static final String NAME_INIT_PARAM = "name";
    private static final String BASE_TYPES_INIT_PARAM = "baseTypes";
    private static final String CONTENT_TYPES_INIT_PARAM = "contentTypes";

    private final PortletAPI portletApi;
    private final BiFunction<User, String, String> titleResolver;

    /**
     * Production wiring: titles come from {@link LanguageUtil#get(User, String, Object...)}.
     *
     * @param portletApi the portlet API
     */
    public ToolCatalogHelper(final PortletAPI portletApi) {
        this(portletApi, (user, key) -> Try.of(() -> LanguageUtil.get(user, key)).getOrElse(key));
    }

    /**
     * Wiring with an injectable title lookup, for tests that run without a message bundle.
     *
     * @param portletApi    the portlet API
     * @param titleResolver resolves a translation key for a user; must return the key itself on a
     *                      miss, as {@code LanguageUtil} does
     */
    public ToolCatalogHelper(final PortletAPI portletApi,
                             final BiFunction<User, String, String> titleResolver) {
        this.portletApi = portletApi;
        this.titleResolver = titleResolver;
    }

    /**
     * Lists every tool that can be placed in a navigation section, applying the same rules as
     * the legacy Roles &amp; Tools picker: the old Languages tool is dropped while the
     * configuration that hides it is on, tools the product marks as not placeable are dropped,
     * and the result is sorted by title ignoring letter case (ties by id).
     *
     * @param user the caller, whose language decides the titles
     * @return the catalog rows
     * @throws DotRuntimeException when the portlets cannot be loaded; the caller answers an error
     *                             rather than an empty catalog
     */
    public List<ToolCatalogEntryView> catalog(final User user) {
        final Collection<Portlet> portlets;
        try {
            portlets = portletApi.findAllPortlets();
        } catch (final SystemException e) {
            // Surface the failure instead of answering an empty catalog that looks like "no tools".
            throw new DotRuntimeException("Unable to load the portlets for the tools catalog", e);
        }
        final boolean hideOldLanguages = Config.getBooleanProperty(
                FeatureFlagName.FEATURE_FLAG_LOCALES_HIDE_OLD_LANGUAGES_PORTLET, true);
        final String languagesId = PortletID.LANGUAGES.toString();

        final List<ToolCatalogEntryView> rows = portlets.stream()
                .filter(portlet -> !(hideOldLanguages && languagesId.equalsIgnoreCase(portlet.getPortletId())))
                .filter(portletApi::canAddPortletToLayout)
                .map(portlet -> new ToolCatalogEntryView(
                        portlet.getPortletId(),
                        title(user, portlet),
                        portletApi.isCustomContentPortlet(portlet)))
                .sorted(Comparator.comparing((ToolCatalogEntryView row) -> row.title().toLowerCase())
                        .thenComparing(ToolCatalogEntryView::id))
                .collect(Collectors.toList());
        Logger.debug(this, () -> "Tools catalog built with " + rows.size() + " rows");
        return rows;
    }

    /**
     * Resolves a tool's management label, prefixed by its translated navigation parent when set.
     * Missing portlets retain their translated title or id so stored section entries stay readable.
     *
     * @param user the {@link User} whose language decides the label
     * @param portletId the tool id
     * @return the qualified label, for example {@code Identity Access › Users}
     */
    public String title(final User user, final String portletId) {
        return title(user, portletId, portletApi.findPortlet(portletId));
    }

    /**
     * Resolves the same label from an already loaded portlet, without another registry lookup.
     *
     * @param user the {@link User} whose language decides the label
     * @param portlet the registered {@link Portlet}
     * @return the qualified management label
     */
    public String title(final User user, final Portlet portlet) {
        return title(user, portlet.getPortletId(), portlet);
    }

    /** Resolves the title and optional parent while retaining the registered-name and id fallbacks. */
    private String title(final User user, final String portletId, final Portlet portlet) {
        final String key = TITLE_KEY_PREFIX + portletId;
        final String translated = titleResolver.apply(user, key);
        final String registeredName = initParam(portlet, NAME_INIT_PARAM);
        final String toolTitle = UtilMethods.isSet(translated) && !key.equals(translated)
                ? translated : UtilMethods.isSet(registeredName) ? registeredName : portletId;
        final String parent = initParam(portlet, "parent");
        if (parent == null || parent.trim().isEmpty()) {
            return toolTitle;
        }
        final String parentKey = parent.trim();
        final String parentTitle = titleResolver.apply(user, parentKey);
        return (UtilMethods.isSet(parentTitle) ? parentTitle : parentKey) + " › " + toolTitle;
    }

    /**
     * Maps a custom content tool to its editable configuration. The comma-separated base and
     * content types the tool is stored with become lists; the data view mode and parent label
     * are returned as stored. A missing parent is returned as an empty string.
     *
     * @param portlet a portlet for which {@link PortletAPI#isCustomContentPortlet(Portlet)} is true
     * @return the configuration view
     */
    public CustomToolView toCustomToolView(final Portlet portlet) {
        return new CustomToolView(
                portlet.getPortletId(),
                initParam(portlet, NAME_INIT_PARAM),
                splitList(initParam(portlet, BASE_TYPES_INIT_PARAM)),
                splitList(initParam(portlet, CONTENT_TYPES_INIT_PARAM)),
                initParam(portlet, Portlet.DATA_VIEW_MODE_KEY),
                Objects.requireNonNullElse(initParam(portlet, "parent"), ""));
    }

    /** Reads an init-param, returning null for an absent portlet or parameter map. */
    private static String initParam(final Portlet portlet, final String name) {
        final Map<String, String> initParams = null == portlet ? null : portlet.getInitParams();
        return null == initParams ? null : initParams.get(name);
    }

    /** Splits comma-separated text into trimmed, non-blank items; blank or null input gives an empty list. */
    private static List<String> splitList(final String commaSeparated) {
        if (!UtilMethods.isSet(commaSeparated)) {
            return Collections.emptyList();
        }
        return Arrays.stream(commaSeparated.split(StringPool.COMMA))
                .map(String::trim)
                .filter(UtilMethods::isSet)
                .collect(Collectors.toList());
    }
}
