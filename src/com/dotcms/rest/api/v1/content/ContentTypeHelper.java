package com.dotcms.rest.api.v1.content;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.portlet.PortletURL;
import com.dotcms.repackage.javax.portlet.WindowState;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.structure.business.StructureAPI;
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

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;
import java.util.Map;

/**
 * Contentlet helper.
 * @author jsanca
 */
public class ContentTypeHelper implements Serializable {

    private static class SingletonHolder {
        private static final ContentTypeHelper INSTANCE = new ContentTypeHelper();
    }

    public static ContentTypeHelper getInstance() {

        return ContentTypeHelper.SingletonHolder.INSTANCE;
    }

    private final WebResource webResource;
    private final StructureAPI structureAPI;
    private final LayoutAPI layoutAPI;
    private final LanguageAPI languageAPI;

    public ContentTypeHelper() {
        this(new WebResource(),
                APILocator.getStructureAPI(),
                APILocator.getLayoutAPI(),
                APILocator.getLanguageAPI());
    }

    @VisibleForTesting
    protected ContentTypeHelper(WebResource webResource,
                                StructureAPI structureAPI,
                                LayoutAPI layoutAPI,
                                LanguageAPI languageAPI) {
        this.webResource = webResource;
        this.structureAPI = structureAPI;
        this.layoutAPI = layoutAPI;
        this.languageAPI = languageAPI;
    }

    /**
     * Get the action url for the structure
     * @param request
     * @param structure
     * @param user
     * @return String
     */
    public String getActionUrl(final HttpServletRequest request,
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

    /**
     * Return  a {@link List} of StructureTypeView
     *
     * @param request
     * @return
     * @throws DotDataException
     * @throws LanguageException
     */
    public List<StructureTypeView> getTypes(HttpServletRequest request ) throws DotDataException, LanguageException {
        final InitDataObject initData = this.webResource.init(null, true, request, true, null); // should logged in
        final User user = initData.getUser();

        List<Structure> structures = this.structureAPI.find(user, false, true);
        List<StructureTypeView> result = list();

        if (null != structures) {
            Locale locale = LocaleUtil.getLocale(request);
            Map<String, String> strTypeNames = this.getStrTypeNames(locale);
            StructureTypeViewCollection structureTypeViewCollection = new StructureTypeViewCollection();

            structures.stream()
                    .forEach(structure -> {
                        structureTypeViewCollection.add(structure, new ContentTypeView(
                                Structure.Type.getType(structure.getStructureType()).name(),
                                structure.getName(), structure.getInode(),
                                this.getActionUrl(request, structure, user)));
                    });

            result = structureTypeViewCollection.getStructureTypeView(strTypeNames);
        }

        return result;
    }

    /**
     * Get Str Type Names
     * @param locale {@link Locale}
     * @return Map (type Id -> i18n value)
     * @throws LanguageException
     */
    public final Map<String, String> getStrTypeNames(final Locale locale) throws LanguageException {

        return map(
                Structure.Type.getType(Structure.Type.CONTENT.getType()).name(), LanguageUtil.get(locale, "Content"),
                Structure.Type.getType(Structure.Type.WIDGET.getType()).name(), LanguageUtil.get(locale, "Widget"),
                Structure.Type.getType(Structure.Type.FORM.getType()).name(), LanguageUtil.get(locale, "Form"),
                Structure.Type.getType(Structure.Type.FILEASSET.getType()).name(), LanguageUtil.get(locale, "File"),
                Structure.Type.getType(Structure.Type.HTMLPAGE.getType()).name(), LanguageUtil.get(locale, "HTMLPage"),
                Structure.Type.getType(Structure.Type.PERSONA.getType()).name(), LanguageUtil.get(locale, "Persona")
        );
    } // getStrTypeNames.

} // E:O:F:ContentTypeHelper.
