package com.dotcms.rest.api.v1.contenttype;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.util.ContentTypeUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.structure.business.StructureAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.LocaleUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.dotcms.util.CollectionsUtils.imap;
import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;

/**
 * Contentlet helper.
 * @author jsanca
 */
public class ContentTypeHelper implements Serializable {

    private static final String N_ENTRIES_FIELD_NAME = "nEntries";

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

        List<ContentType> types = APILocator.getContentTypeAPI(user, true).findAll();
        List<BaseContentTypesView> result = list();


            Locale locale = LocaleUtil.getLocale(request);
            Map<String, String> baseContentTypeNames = this.getBaseContentTypeNames(locale);
            BaseContentTypesViewCollection baseContentTypesViewCollection = new BaseContentTypesViewCollection();

            types.stream()
                    .forEach(type -> {

                        baseContentTypesViewCollection.add(new ContentTypeView(
                                type.baseType().toString(), 
                                type.name(), 
                                type.id(),
                                contentTypeUtil.getActionUrl(request, type, user)
                            ));

                                
                    });

            result = baseContentTypesViewCollection.getStructureTypeView(baseContentTypeNames);

            addRecents(request, user, BaseContentType.CONTENT, result);
            addRecents(request, user, BaseContentType.WIDGET, result);
        

        return result;
    }

    private void addRecents(final HttpServletRequest request, final User user, BaseContentType baseType,
                                            List<BaseContentTypesView>  baseContentTypesView)
            throws DotDataException, LanguageException {

        Locale locale = LocaleUtil.getLocale(request);
        
        List<ContentTypeView> recentsContent = new ArrayList<>();
        List<ContentType> types = APILocator.getContentTypeAPI(user, true).recentlyUsed(baseType, -1);
        for(ContentType type : types){
            recentsContent.add(  new ContentTypeView(type,contentTypeUtil.getActionUrl(request, type.id(), user)));
        }

        if (!recentsContent.isEmpty()){
            String name = String.format("RECENT_%s" ,baseType.toString());
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
                    BaseContentType.CONTENT.name(), LanguageUtil.get(locale, "Content"),
                    BaseContentType.WIDGET.name(), LanguageUtil.get(locale, "Widget"),
                    BaseContentType.FORM.name(), LanguageUtil.get(locale, "Form"),
                    BaseContentType.FILEASSET.name(), LanguageUtil.get(locale, "File"),
                    BaseContentType.HTMLPAGE.name(), LanguageUtil.get(locale, "HTMLPage"),
                    BaseContentType.PERSONA.name(), LanguageUtil.get(locale, "Persona")
            );

            BASE_CONTENT_TYPE_LABELS.put(locale, map);
        }

        return map;

    } // getBaseContentTypeNames.

    public List<Map<String, Object>> getContentTypes(User user, String query, int offset, int limit, String orderby, String direction)
            throws DotDataException {
        List<Structure> structures = this.structureAPI.find(user, false, false, query, orderby,
                limit, offset, direction);
        Map<String, Long> entriesByContentTypes = getEntriesByContentTypes(user);

        List<Map<String, Object>> result = structures.stream()
                .map(contentType -> {
                    Map<String, Object> map = contentType.getMap();

                    if (entriesByContentTypes != null) {
                        String key = contentType.getVelocityVarName().toLowerCase();
                        Long contentTypeEntriesNumber = entriesByContentTypes.get(key) == null ? 0l :
                                entriesByContentTypes.get(key);
                        map.put(N_ENTRIES_FIELD_NAME, contentTypeEntriesNumber);
                    }

                    return map;
                })
                .collect(Collectors.toList());

        if (N_ENTRIES_FIELD_NAME.equals(orderby)){
            result.sort( (contentType1, contentType2) -> {
                long l1 = (long) contentType1.get(N_ENTRIES_FIELD_NAME);
                long l2 = (long) contentType2.get(N_ENTRIES_FIELD_NAME);
                return "asc".equals(direction) ? (int) (l1 - l2) : (int) (l2 - l1);
            });
        }

        return result;
    }

    private Map<String, Long> getEntriesByContentTypes(User user) {
        try {
            return APILocator.getContentTypeAPI(user, true).getEntriesByContentTypes();
        } catch (Exception e) {
            return null;
        }
    }

    public long getContentTypesCount() throws DotDataException {
        return this.structureAPI.countStructures("");
    }
} // E:O:F:ContentTypeHelper.
