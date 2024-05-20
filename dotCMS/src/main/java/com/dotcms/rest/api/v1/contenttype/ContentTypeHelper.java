package com.dotcms.rest.api.v1.contenttype;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.transform.contenttype.ContentTypeInternationalization;
import com.dotcms.contenttype.transform.contenttype.DetailPageTransformerImpl;
import com.dotcms.contenttype.transform.contenttype.JsonContentTypeTransformer;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.WebResource;
import com.dotcms.util.ContentTypeUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.business.StructureAPI;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.LocaleUtil;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/**
 * Contentlet helper.
 * @author jsanca
 */
public class ContentTypeHelper implements Serializable {

    private static final String DETAIL_PAGE = "detailPage";
    private static final String DETAIL_PAGE_PATH = "detailPagePath";

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
     * Evaluates the content type request and returns a modified content type if necessary.
     *
     * @param contentType The content type to evaluate.
     * @param user        The user making the request.
     * @return The resulting content type after evaluation.
     * @throws DotDataException     If an error occurs while accessing data.
     * @throws DotSecurityException If there are security restrictions preventing the evaluation.
     * @throws URISyntaxException   If the URI syntax is invalid.
     */
    public ContentType evaluateContentTypeRequest(final ContentType contentType, final User user)
            throws DotDataException, DotSecurityException, URISyntaxException {

        // Evaluate if the content type has a detail page and if it is a URI, if so, get the
        // detail page identifier and set it back into the content type detail page but as an
        // identifier.
        // If not conversion is made, the content type is returned as it is.
        var pageDetailIdentifierOptional = new DetailPageTransformerImpl(
                contentType, user).uriToId();
        if (pageDetailIdentifierOptional.isPresent()) {
            final var updatedContentType = ContentTypeBuilder.builder(contentType).
                    detailPage(pageDetailIdentifierOptional.get())
                    .build();
            updatedContentType.constructWithFields(contentType.fields());
            return updatedContentType;
        }

        return contentType;
    }

    /**
     * Converts a ContentType object to a Map representation to be used as a response.
     *
     * @param contentType The ContentType object to convert.
     * @param user        The user making the request.
     * @return The converted ContentType object as a Map.
     * @throws DotDataException     If an error occurs while accessing data.
     * @throws DotSecurityException If there are security restrictions preventing the conversion.
     */
    public Map<String, Object> contentTypeToMap(final ContentType contentType, final User user)
            throws DotDataException, DotSecurityException {
        return contentTypeToMap(contentType, null, user);
    }

    /**
     * Converts a ContentType object to a Map representation for use as a response.
     *
     * @param contentType                     The ContentType object to convert.
     * @param contentTypeInternationalization The ContentTypeInternationalization object.
     * @param user                            The user making the request.
     * @return The converted ContentType object as a Map.
     * @throws DotDataException     If an error occurs while accessing data.
     * @throws DotSecurityException If there are security restrictions preventing the conversion.
     */
    public Map<String, Object> contentTypeToMap(final ContentType contentType,
            final ContentTypeInternationalization contentTypeInternationalization, final User user)
            throws DotDataException, DotSecurityException {

        // Transform the content type to a map
        var contentTypeMap = new JsonContentTypeTransformer(
                contentType, contentTypeInternationalization
        ).mapObject();

        try {
            // Add the detail page path to the map
            final var pageDetailURIOptional = new DetailPageTransformerImpl(
                    contentType, user).idToUri();
            pageDetailURIOptional.ifPresent(s -> contentTypeMap.put(DETAIL_PAGE_PATH, s));
        } catch (DoesNotExistException e) {
            // The idToUri method throws a DoesNotExistException and logs a warning if the detail
            // page is not found.
            contentTypeMap.remove(DETAIL_PAGE);
        }

        return contentTypeMap;
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