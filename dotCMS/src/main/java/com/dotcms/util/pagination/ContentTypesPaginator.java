package com.dotcms.util.pagination;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.JsonContentTypeTransformer;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.rainerhahnekamp.sneakythrow.Sneaky;


/**
 * Handle {@link ContentType} pagination
 */
public class ContentTypesPaginator implements PaginatorOrdered<Map<String, Object>>{

    private static final String N_ENTRIES_FIELD_NAME = "nEntries";
    public static final String TYPE_PARAMETER_NAME = "type";

    private final ContentTypeAPI contentTypeAPI;

    @VisibleForTesting
    public ContentTypesPaginator (ContentTypeAPI contentTypeAPI) {
        this.contentTypeAPI = contentTypeAPI;
    }

    public ContentTypesPaginator () {
        this.contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
    }
    /**
     * Return the total
     * @param condition
     * @return
     */
    private long getTotalRecords(String condition, BaseContentType type) {
        return Sneaky.sneak(() ->this.contentTypeAPI.count(condition, type));
    }

    @Override
    public PaginatedArrayList<Map<String, Object>> getItems(final User user, final String filter, final int limit, final int offset, final String orderBy,
            final OrderDirection direction, final Map<String, Object> extraParams) {

        String typeName =  extraParams.containsKey(TYPE_PARAMETER_NAME) ? extraParams.get(TYPE_PARAMETER_NAME).toString().replaceAll("\\[","").replaceAll("\\]", "") : "ANY";
        final BaseContentType type = BaseContentType.getBaseContentType(typeName);
                
         
        String orderByString = (orderBy==null)? "mod_date desc" : orderBy;
        
        orderByString =  (orderBy.trim().toLowerCase().endsWith(" asc") || orderBy.trim().toLowerCase().endsWith(" desc")) 
                ? orderBy
                : orderBy + " " + (UtilMethods.isSet(direction) ? direction.toString().toLowerCase(): OrderDirection.ASC.name());
        try {
            final List<ContentType> contentTypes = (type != null) ? this.contentTypeAPI.search(filter,type,  orderByString , limit, offset) : this.contentTypeAPI.search(filter,  orderByString , limit, offset);


            final List<Map<String, Object>> contentTypesTransform = transformContentTypesToMap(contentTypes);

            setEntriesAttribute(user, contentTypesTransform);

            final PaginatedArrayList<Map<String, Object>> result = new PaginatedArrayList<>();
            result.addAll(contentTypesTransform);
            result.setTotalResults(this.getTotalRecords(filter, type));

            return result;
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }


    private void setEntriesAttribute(final User user, final List<Map<String, Object>> contentTypesTransform) throws DotDataException {
        Map<String, Long> entriesByContentTypes = APILocator.getContentTypeAPI(user, true).getEntriesByContentTypes();

        for (final Map<String, Object> contentTypeEntry : contentTypesTransform) {
            if (entriesByContentTypes != null) {
                String key = ((String) contentTypeEntry.get("variable")).toLowerCase();
                Long contentTypeEntriesNumber = entriesByContentTypes.get(key) == null ? 0l :
                        entriesByContentTypes.get(key);
                contentTypeEntry.put(N_ENTRIES_FIELD_NAME, contentTypeEntriesNumber);
            }
        }
    }

    private List<Map<String, Object>> transformContentTypesToMap(final List<ContentType> contentTypes) {
        return new JsonContentTypeTransformer(contentTypes)
                        .mapList()
                        .stream()
                        .map(contentTypeMap -> {
                            contentTypeMap.remove("fields");
                            return contentTypeMap;
                        })
                        .collect(Collectors.toList());
    }



    @NotNull
    private BaseContentType getBaseContentType(final String filter) {
        try {
            return BaseContentType.valueOf(filter);
        } catch (IllegalArgumentException e) {
            BaseContentType result = null;

            for (final BaseContentType baseContentType : BaseContentType.values()) {
                if (baseContentType.name().startsWith(filter.toUpperCase())) {
                    return baseContentType;
                }
            }

            throw e;
        }
    }
}