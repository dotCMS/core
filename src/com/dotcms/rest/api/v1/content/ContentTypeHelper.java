package com.dotcms.rest.api.v1.content;

import com.dotcms.contenttype.model.type.BaseContentType;
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
                        baseContentTypesViewCollection.add(structure, new ContentTypeView(
                                BaseContentType.getBaseContentType(structure.getStructureType()).name(),
                                structure.getName(), structure.getInode(),
                                contentTypeUtil.getActionUrl(request, structure, user)));
                    });

            result = baseContentTypesViewCollection.getStructureTypeView(baseContentTypeNames);
        }

        return result;
    }

    /**
     * Get Str Type Names
     * @param locale {@link Locale}
     * @return Map (type Id -> i18n value)
     * @throws LanguageException
     */
    public final Map<String, String> getBaseContentTypeNames(final Locale locale) throws LanguageException {

        return map(
                BaseContentType.CONTENT.name(), LanguageUtil.get(locale, "Content"),
                BaseContentType.WIDGET.name(), LanguageUtil.get(locale, "Widget"),
                BaseContentType.FORM.name(), LanguageUtil.get(locale, "Form"),
                BaseContentType.FILEASSET.name(), LanguageUtil.get(locale, "File"),
                BaseContentType.HTMLPAGE.name(), LanguageUtil.get(locale, "HTMLPage"),
                BaseContentType.PERSONA.name(), LanguageUtil.get(locale, "Persona")
        );
    } // getBaseContentTypeNames.

} // E:O:F:ContentTypeHelper.
