package com.dotcms.rest.api.v1.content;

import com.dotcms.repackage.javax.portlet.PortletURL;
import com.dotcms.repackage.javax.portlet.WindowState;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.portlet.PortletURLImpl;
import com.liferay.util.LocaleUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.map;

/**
 * Contentlet helper.
 * @author jsanca
 */
public class ContentTypeHelper implements Serializable {

    public static final ContentTypeHelper INSTANCE = new ContentTypeHelper();

    /**
     * Get Str Type Names
     * @param locale {@link Locale}
     * @return Map (type Id -> i18n value)
     * @throws LanguageException
     */
    public final Map<Integer, String> getStrTypeNames(final Locale locale) throws LanguageException {

        return map(
                1, LanguageUtil.get(locale, "Content"),
                2, LanguageUtil.get(locale, "Widget"),
                3, LanguageUtil.get(locale, "Form"),
                4, LanguageUtil.get(locale, "File"),
                5, LanguageUtil.get(locale, "HTMLPage"),
                6, LanguageUtil.get(locale, "Persona")
        );
    } // getStrTypeNames.

    /**
     * Get the action url for the structure
     * @param request
     * @param layoutAPI
     * @param structure
     * @param user
     * @param languageAPI
     * @return String
     */
    public String getActionUrl(final HttpServletRequest request,
                                      final LayoutAPI layoutAPI,
                                      final LanguageAPI languageAPI,
                                      final Structure structure,
                                      final User user
                                      ) {

        final List<Layout> layouts;
        String actionUrl = StringUtils.EMPTY;


        try {
            layouts = layoutAPI.loadLayoutsForUser(user);

            if (0 != layouts.size()) {

                final Layout layout = layouts.get(0);
                final List<String> portletIds = layout.getPortletIds();
                final String portletName = portletIds.get(0);
                final PortletURL portletURL = new PortletURLImpl(request, portletName, layout.getId(), true);

                portletURL.setWindowState(WindowState.MAXIMIZED);

                portletURL.setParameters(map(
                        "struts_action", new String[]{"/ext/contentlet/edit_contentlet"},
                        "cmd", new String[]{"new"},
                        "inode", new String[]{""}
                ));

                actionUrl = portletURL.toString() + "&selectedStructure=" + structure.getInode() +
                        "&lang=" + this.getLanguageId(user.getLanguageId(), languageAPI);
            }
        } catch (Exception e) {

            Logger.error(this, e.getMessage(), e);
        }

        return actionUrl;
    }


    private Long getLanguageId (final String userLocaleString, final LanguageAPI languageAPI) {


        Long languageId = null;
        final Locale locale = LocaleUtil.fromLanguageId(userLocaleString);

        if (null != locale) {

            try {

                languageId =
                        languageAPI.getLanguage(locale.getLanguage(),
                                locale.getCountry()).getId();
            } catch (Exception e) {

                languageId = null;
            }
        }

        if (null == languageId) {

            languageId = languageAPI.getDefaultLanguage().getId();
        }

        return languageId;
    }

} // E:O:F:ContentTypeHelper.
