package com.dotcms.util.pagination;


import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.JsonContentTypeTransformer;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.org.apache.poi.openxml4j.opc.ContentTypes;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.structure.business.StructureAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;

/**
 * Handle {@link ContentType} pagination
 */
public class ContentTypesPaginator implements Paginator<Map<String, Object>>{
    private static final String N_ENTRIES_FIELD_NAME = "nEntries";

    private final StructureAPI structureAPI;

    @VisibleForTesting
    public ContentTypesPaginator (StructureAPI structureAPI) {
        this.structureAPI = structureAPI;
    }

    public ContentTypesPaginator () {
        this(APILocator.getStructureAPI());
    }

    /**
     * Return the total
     * @param condition
     * @return
     */
    private long getTotalRecords(String condition) {
        return this.structureAPI.countStructures(condition);
    }

    @Override
    public PaginatedArrayList<Map<String, Object>> getItems(User user, String filter, int limit, int offset, String orderby,
                                                            OrderDirection direction, Map<String, Object> extraParams) {

        String queryCondition = this.getQueryCondition(filter);

        try {
            List<Structure> structures = this.structureAPI.find(user, false, false, queryCondition,
                    orderby, limit, offset, direction.toString().toLowerCase());

            List<ContentType> contentTypes = new StructureTransformer(structures).asList();
            List<Map<String, Object>> contentTypesTransform = transformContentTypesToMap(contentTypes);

            setEntriesAttribute(user, contentTypesTransform);


            if (N_ENTRIES_FIELD_NAME.equals(orderby)) {
                orderByEntries(direction, contentTypesTransform);
            }

            final PaginatedArrayList<Map<String, Object>> result = new PaginatedArrayList();
            result.addAll(contentTypesTransform);
            result.setTotalResults(this.getTotalRecords(queryCondition));

            return result;
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    private void orderByEntries(final OrderDirection direction, final List<Map<String, Object>> contentTypesTransform) {
        contentTypesTransform.sort((contentType1, contentType2) -> {
            long l1 = (long) contentType1.get(N_ENTRIES_FIELD_NAME);
            long l2 = (long) contentType2.get(N_ENTRIES_FIELD_NAME);
            return OrderDirection.ASC == direction ? (int) (l1 - l2) : (int) (l2 - l1);
        });
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

    private String getQueryCondition(String filter){
        return UtilMethods.isSet(filter) ? String.format("(upper(name) like '%%%s%%')", filter.toUpperCase()) : StringUtils.EMPTY;
    }
}
