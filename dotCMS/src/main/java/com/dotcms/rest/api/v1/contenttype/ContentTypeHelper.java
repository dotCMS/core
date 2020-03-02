package com.dotcms.rest.api.v1.contenttype;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.WebResource;
import com.dotcms.util.ContentTypeUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.structure.business.StructureAPI;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.util.LocaleUtil;
import java.io.Serializable;
import java.util.*;
import javax.servlet.http.HttpServletRequest;

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
        List<BaseContentTypesView> result = list();

        Locale locale = LocaleUtil.getLocale(request);
        Map<String, String> baseContentTypeNames = this.getBaseContentTypeNames(locale);

        for(Map.Entry<String,String> baseType : baseContentTypeNames.entrySet()){
            result.add(new BaseContentTypesView(baseType.getKey(),baseType.getValue(),null));
        }

        return result;
    }

    /**
     * Get Str Type Names
     * @param locale {@link Locale}
     * @return Map (type Id -> i18n value)
     * @throws LanguageException
     */
    public synchronized Map<String, String> getBaseContentTypeNames(final Locale locale) throws LanguageException {

        Map<String, String> contentTypesLabelsMap = new LinkedHashMap<>();
        contentTypesLabelsMap.put(BaseContentType.CONTENT.name(), LanguageUtil.get(locale, "Content"));
        contentTypesLabelsMap.put(BaseContentType.WIDGET.name(), LanguageUtil.get(locale, "Widget"));
        contentTypesLabelsMap.put(BaseContentType.FILEASSET.name(), LanguageUtil.get(locale, "File"));
        contentTypesLabelsMap.put(BaseContentType.HTMLPAGE.name(), LanguageUtil.get(locale, "HTMLPage"));
        contentTypesLabelsMap.put(BaseContentType.KEY_VALUE.name(), LanguageUtil.get(locale, "KeyValue"));
        contentTypesLabelsMap.put(BaseContentType.VANITY_URL.name(), LanguageUtil.get(locale, "VanityURL"));
        contentTypesLabelsMap.put(BaseContentType.DOTASSET.name(), LanguageUtil.get(locale, "DotAsset"));

        if(isStandardOrEnterprise()) {
            contentTypesLabelsMap.put(BaseContentType.FORM.name(), LanguageUtil.get(locale, "Form"));
            contentTypesLabelsMap.put(BaseContentType.PERSONA.name(), LanguageUtil.get(locale, "Persona"));
        }

        return contentTypesLabelsMap;

    } // getBaseContentTypeNames.

    @VisibleForTesting
    boolean isStandardOrEnterprise() {
        return LicenseUtil.getLevel() > LicenseLevel.COMMUNITY.level;
    }

    public long getContentTypesCount(String condition) throws DotDataException {
        return this.structureAPI.countStructures(condition);
    }
} // E:O:F:ContentTypeHelper.