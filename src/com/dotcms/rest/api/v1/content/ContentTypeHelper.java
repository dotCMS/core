package com.dotcms.rest.api.v1.content;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.portlet.PortletURL;
import com.dotcms.repackage.javax.portlet.WindowState;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.util.ContentTypeUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.structure.business.StructureAPI;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.portlet.PortletURLImpl;
import com.liferay.util.LocaleUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static com.dotcms.util.CollectionsUtils.imap;
import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;

/**
 * Contentlet helper.
 * @author jsanca
 */
public class ContentTypeHelper implements Serializable {

    private static final Map<Locale, Map<String, String>> BASE_CONTENT_TYPE_LABELS = new HashMap<>();

    private static class SingletonHolder {
        private static final ContentTypeHelper INSTANCE = new ContentTypeHelper();
    }

    public static ContentTypeHelper getInstance() {

        return ContentTypeHelper.SingletonHolder.INSTANCE;
    }

    private final WebResource webResource;
    private final StructureAPI structureAPI;
    private final ContentTypeUtil contentTypeUtil;

    public ContentTypeHelper() {
        this(new WebResource(),
                APILocator.getStructureAPI(),
                ContentTypeUtil.getInstance());
    }

    @VisibleForTesting
    protected ContentTypeHelper(WebResource webResource,
                                StructureAPI structureAPI,
                                ContentTypeUtil contentTypeUtil) {
        this.webResource = webResource;
        this.structureAPI = structureAPI;
        this.contentTypeUtil = contentTypeUtil;
    }

    /**
     * Return  a {@link List} of BaseContentTypesView
     *
     * @param request
     * @return
     * @throws DotDataException
     * @throws LanguageException
     */
    public List<BaseContentTypesView> getTypes(HttpServletRequest request ) throws DotDataException, LanguageException {
        final InitDataObject initData = this.webResource.init(null, true, request, true, null); // should logged in
        final User user = initData.getUser();

        List<Structure> structures = this.structureAPI.find(user, false, true);
        List<BaseContentTypesView> result = list();

        if (null != structures) {
            Locale locale = LocaleUtil.getLocale(request);
            Map<String, String> baseContentTypeNames = this.getBaseContentTypeNames(locale);
            BaseContentTypesViewCollection baseContentTypesViewCollection = new BaseContentTypesViewCollection();

            structures.stream()
                    .forEach(structure -> {
                        baseContentTypesViewCollection.add(new ContentTypeView(
                                Structure.Type.getType(structure.getStructureType()).name(),
                                structure.getName(), structure.getInode(),
                                contentTypeUtil.getActionUrl(request, structure, user)));
                    });

            result = baseContentTypesViewCollection.getStructureTypeView(baseContentTypeNames);

            addRecents(request, user, Structure.Type.CONTENT, result);
            addRecents(request, user, Structure.Type.WIDGET, result);
        }

        return result;
    }

    private void addRecents(final HttpServletRequest request, final User user, Structure.Type type,
                                            List<BaseContentTypesView>  baseContentTypesView)
            throws DotDataException, LanguageException {

        Locale locale = LocaleUtil.getLocale(request);

        List<ContentTypeView> recentsContent = structureAPI.getRecentContentType(type, user, -1)
                .stream()
                .map(map -> new ContentTypeView(map.get("type").toString(), map.get("name").toString(), map.get("inode").toString(),
                        contentTypeUtil.getActionUrl(request, map.get("inode").toString(), user)))
                .collect(Collectors.toList());

        if (!recentsContent.isEmpty()){
            String name = String.format("RECENT_%s" ,type.toString());
            String label = LanguageUtil.get(locale, name.toLowerCase());
            baseContentTypesView.add(new BaseContentTypesView(name, label, recentsContent));
        }
    }

    /**
     * Get Str Type Names
     * @param locale {@link Locale}
     * @return Map (type Id -> i18n value)
     * @throws LanguageException
     */
    public synchronized static Map<String, String> getBaseContentTypeNames(final Locale locale) throws LanguageException {

        Map<String, String> map = BASE_CONTENT_TYPE_LABELS.get(locale);

        if (map == null) {
            map = imap(
                    Structure.Type.CONTENT.name(), LanguageUtil.get(locale, "Content"),
                    Structure.Type.WIDGET.name(), LanguageUtil.get(locale, "Widget"),
                    Structure.Type.FORM.name(), LanguageUtil.get(locale, "Form"),
                    Structure.Type.FILEASSET.name(), LanguageUtil.get(locale, "File"),
                    Structure.Type.HTMLPAGE.name(), LanguageUtil.get(locale, "HTMLPage"),
                    Structure.Type.PERSONA.name(), LanguageUtil.get(locale, "Persona")
            );

            BASE_CONTENT_TYPE_LABELS.put(locale, map);
        }

        return map;
    } // getBaseContentTypeNames.

} // E:O:F:ContentTypeHelper.
