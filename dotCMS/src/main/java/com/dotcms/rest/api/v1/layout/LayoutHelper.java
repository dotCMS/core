package com.dotcms.rest.api.v1.layout;

import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.portal.PortletAPI;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Pure logic behind the {@code /v1/layouts} endpoints: mapping a stored {@link Layout} to the
 * view the Tools portlet consumes, resolving tool titles, and validating the inputs of the
 * writes. Holds no state beyond the APIs it reads from, so it is unit-testable with mocks.
 *
 * @author hassandotcms
 */
public class LayoutHelper {

    /** Prefix of the message key that holds a portlet's localized title. */
    static final String TITLE_KEY_PREFIX = "com.dotcms.repackage.javax.portlet.title.";

    private static final String NAME_INIT_PARAM = "name";

    private final LayoutAPI layoutApi;
    private final PortletAPI portletApi;
    private final BiFunction<User, String, String> titleResolver;

    /**
     * Production wiring: titles come from {@link LanguageUtil#get(User, String)}.
     *
     * @param layoutApi  the layout API
     * @param portletApi the portlet API, used for the registered-name fallback of titles
     */
    public LayoutHelper(final LayoutAPI layoutApi, final PortletAPI portletApi) {
        this(layoutApi, portletApi, (user, key) -> Try.of(() -> LanguageUtil.get(user, key)).getOrElse(key));
    }

    /**
     * Wiring with an injectable title lookup, for tests that run without a message bundle.
     *
     * @param layoutApi     the layout API
     * @param portletApi    the portlet API
     * @param titleResolver resolves a translation key for a user; must return the key itself on a
     *                      miss, as {@code LanguageUtil} does
     */
    public LayoutHelper(final LayoutAPI layoutApi, final PortletAPI portletApi,
                        final BiFunction<User, String, String> titleResolver) {
        this.layoutApi = layoutApi;
        this.portletApi = portletApi;
        this.titleResolver = titleResolver;
    }

    /**
     * Resolves a tool's title for the caller's language: the translation if one exists, else the
     * name the tool was registered with, else its id. The raw translation key is never returned,
     * so the same rule as the tools catalog applies and the two lists never disagree.
     *
     * @param user      the caller, whose language decides the translation
     * @param portletId the tool id
     * @return a displayable title
     */
    public String title(final User user, final String portletId) {
        final String key = TITLE_KEY_PREFIX + portletId;
        final String translated = titleResolver.apply(user, key);
        if (UtilMethods.isSet(translated) && !key.equals(translated)) {
            return translated;
        }
        final Portlet portlet = portletApi.findPortlet(portletId);
        final Map<String, String> initParams = null == portlet ? null : portlet.getInitParams();
        final String registeredName = null == initParams ? null : initParams.get(NAME_INIT_PARAM);
        return UtilMethods.isSet(registeredName) ? registeredName : portletId;
    }

    /**
     * Maps a stored section to its view: the description becomes {@code icon} and every tool id
     * gets a title in the same position.
     *
     * @param layout the stored section
     * @param user   the caller, for title localization
     * @return the view
     */
    public SectionView toView(final Layout layout, final User user) {
        final List<String> portletIds = List.copyOf(layout.getPortletIds());
        final List<String> titles = portletIds.stream()
                .map(id -> title(user, id))
                .collect(Collectors.toList());
        return new SectionView(layout.getId(), layout.getName(),
                null == layout.getDescription() ? "" : layout.getDescription(),
                layout.getTabOrder(), portletIds, titles);
    }

    /**
     * Maps sections to views, preserving the given order.
     *
     * @param layouts the stored sections, already in navigation order
     * @param user    the caller, for title localization
     * @return one view per section, same order
     */
    public List<SectionView> toViews(final List<Layout> layouts, final User user) {
        return layouts.stream().map(layout -> toView(layout, user)).collect(Collectors.toList());
    }
}
