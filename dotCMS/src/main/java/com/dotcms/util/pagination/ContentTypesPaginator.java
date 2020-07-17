package com.dotcms.util.pagination;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.SystemActionWorkflowActionMapping;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.Logger;
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
    public  static final String TYPE_PARAMETER_NAME  = "type";

    private final ContentTypeAPI contentTypeAPI;
    private final WorkflowAPI    workflowAPI;

    @VisibleForTesting
    public ContentTypesPaginator (final ContentTypeAPI contentTypeAPI) {
        this.contentTypeAPI = contentTypeAPI;
        this.workflowAPI    = APILocator.getWorkflowAPI();

    }

    public ContentTypesPaginator () {
        this(APILocator.getContentTypeAPI(APILocator.systemUser()));
    }
    /**
     * Return the total
     * @param condition
     * @return
     */
    private long getTotalRecords(final String condition, final BaseContentType type) {
        return Sneaky.sneak(() ->this.contentTypeAPI.count(condition, type));
    }

    @Override
    public PaginatedArrayList<Map<String, Object>> getItems(final User user, final String filter, final int limit, final int offset, final String orderBy,
            final OrderDirection direction, final Map<String, Object> extraParams) {

        final String typeName =  UtilMethods.isSet(extraParams) && extraParams.containsKey(TYPE_PARAMETER_NAME) ?
                extraParams.get(TYPE_PARAMETER_NAME).toString().replaceAll("\\[","").replaceAll("\\]", "") : "ANY";
         
        String orderByString = UtilMethods.isSet(orderBy) ? orderBy : "mod_date desc";
        
        orderByString =  orderByString.trim().toLowerCase().endsWith(" asc") ||
                orderByString.trim().toLowerCase().endsWith(" desc")
                ? orderByString
                : orderByString + " " + (UtilMethods.isSet(direction) ? direction.toString().toLowerCase(): OrderDirection.ASC.name());
        try {

            final BaseContentType type = BaseContentType.getBaseContentType(typeName);

            final List<ContentType> contentTypes = type != null?
                    this.contentTypeAPI.search(filter,type,  orderByString , limit, offset):
                    this.contentTypeAPI.search(filter,  orderByString , limit, offset);

            final List<Map<String, Object>> contentTypesTransform = transformContentTypesToMap(contentTypes);

            setEntriesAttribute(user, contentTypesTransform,
                    this.workflowAPI.findSchemesMapForContentType(contentTypes),
                    this.workflowAPI.findSystemActionsMapByContentType (contentTypes, user));

            final PaginatedArrayList<Map<String, Object>> result = new PaginatedArrayList<>();
            result.addAll(contentTypesTransform);
            result.setTotalResults(this.getTotalRecords(filter, type));

            return result;
        } catch (DotDataException | DotSecurityException e) {

            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    private void setEntriesAttribute(final User user, final List<Map<String, Object>> contentTypesTransform,
                                     final Map<String, List<WorkflowScheme>> workflowSchemes,
                                     final Map<String, List<SystemActionWorkflowActionMapping>> systemActionMappings)  {

        Map<String, Long> entriesByContentTypes = null;

        try {
            entriesByContentTypes = APILocator.getContentTypeAPI
                    (user, true).getEntriesByContentTypes();
        } catch (DotStateException | DotDataException e) {
            Logger.error(ContentTypesPaginator.class, e);
        }

        for (final Map<String, Object> contentTypeEntry : contentTypesTransform) {

            final String variable = (String) contentTypeEntry.get("variable");
            if (entriesByContentTypes != null) {

                final String key = variable.toLowerCase();
                final Long contentTypeEntriesNumber = entriesByContentTypes.get(key) == null ? 0l :
                        entriesByContentTypes.get(key);
                contentTypeEntry.put(N_ENTRIES_FIELD_NAME, contentTypeEntriesNumber);
            } else {
                contentTypeEntry.put(N_ENTRIES_FIELD_NAME, "N/A");
            }

            if (workflowSchemes.containsKey(variable)) {

                contentTypeEntry.put("workflows", workflowSchemes.get(variable));
            }

            if (systemActionMappings.containsKey(variable)) {

                contentTypeEntry.put("systemActionMappings", workflowSchemes.get(variable));
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