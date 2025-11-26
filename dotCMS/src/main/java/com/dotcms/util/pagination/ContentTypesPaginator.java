package com.dotcms.util.pagination;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.JsonContentTypeTransformer;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.SystemActionWorkflowActionMapping;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import io.vavr.control.Try;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.liferay.util.StringPool.BLANK;

/**
 * This implementation of the {@link PaginatorOrdered} class handles the retrieval of paginated
 * results for {@link ContentType} objects in a high number of features in dotCMS.
 *
 * @author Freddy Rodriguez
 * @since Jun 27th, 2017
 */
public class ContentTypesPaginator implements PaginatorOrdered<Map<String, Object>>{

    public static final String N_ENTRIES_FIELD_NAME = "nEntries";
    public  static final String TYPE_PARAMETER_NAME  = "type";
    public static final String TYPES_PARAMETER_NAME  = "types";
    public static final String HOST_PARAMETER_ID = "host";
    public static final String SITES_PARAMETER_NAME = "sites";
    public static final String COMPARATOR = "comparator";
    public static final String ENTRIES_BY_CONTENT_TYPES = "entriesByContentTypes";
    public static final String VARIABLE = "variable";
    public static final String WORKFLOWS = "workflows";
    public static final String SYSTEM_ACTION_MAPPINGS = "systemActionMappings";
    public static final String ENSURE = "ensure";


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
     * Returns the total amount of the specified Base Content Types living in a list of Site.
     *
     * @param condition Condition that the base Content Type needs to meet.
     * @param type      The Base Content Type to search for.
     * @param siteIds   One or more IDs of the Sites where the total amount of Content Types will be
     *                  determined.
     *
     * @return The total amount of Base Types living in the specified list of Site.
     */
    private long getTotalRecords(final String condition, final BaseContentType type, final List<String> siteIds) {
        return Sneaky.sneak(() ->this.contentTypeAPI.countForSites(condition, type, siteIds));
    }

    /**
     * Safely applies pagination slice to a list, handling all edge cases without throwing range exceptions.
     * 
     * @param list   The source list to slice
     * @param offset The starting position for pagination (0-based index)
     * @param limit  The maximum number of items to return (-1 means no limit)
     * @return A sublist with the requested slice, or the original list if limit is -1 or parameters exceed bounds
     */
    private <T> List<T> applySafeSlice(final List<T> list, final int offset, final int limit) {
        // If the limit is -1, return all elements without slicing
        if (limit == -1) {
            return list;
        }
        
        // If a list is empty, return an empty list
        if (list.isEmpty()) {
            return new ArrayList<>();
        }
        
        // If the offset is beyond list size, return all elements in the list
        if (offset >= list.size()) {
            return list;
        }
        
        // Calculate a safe start index (never negative, never beyond list size)
        final int safeStartIndex = Math.max(0, Math.min(offset, list.size()));
        
        // Calculate a safe end index (start + limit, but never beyond list size)
        final int safeEndIndex = Math.min(safeStartIndex + limit, list.size());
        
        // Apply subList only if we have a valid range
        if (safeStartIndex < safeEndIndex) {
            return list.subList(safeStartIndex, safeEndIndex);
        }
        
        // Return empty list if no valid range
        return new ArrayList<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public PaginatedArrayList<Map<String, Object>> getItems(final User user, final String filter, final int limit, final int offset, final String orderBy,
            final OrderDirection direction, final Map<String, Object> extraParams) {
        final List<String> varNamesList = Try.of(() -> (List<String>) extraParams.get(TYPES_PARAMETER_NAME)).getOrNull();
        final List<String> siteList = Try.of(() -> (List<String>) extraParams.get(SITES_PARAMETER_NAME)).getOrNull();
        final List<String> requestedContentTypes = Try.of(() -> (List<String>) extraParams.get(ENSURE)).getOrNull();
        final String orderByParam = SQLUtil.getOrderByAndDirectionSql(orderBy, direction);
        final String siteId = Try.of(() -> extraParams.get(HOST_PARAMETER_ID).toString()).getOrElse(BLANK);
        final List<String> baseTypeNames = getBaseTypeNames(extraParams);
        final Set<BaseContentType> types = BaseContentType.fromNames(baseTypeNames); //This will get me BaseType Any if none is passed
        try {
            long totalRecords = 0L;
            final PaginatedArrayList<Map<String, Object>> result = new PaginatedArrayList<>();
            final List<ContentType> contentTypes;

            // When querying multiple base types, use efficient database-level UNION query
            // For single type or variable name queries, use existing optimized paths
            if (types.size() > 1 && !UtilMethods.isSet(varNamesList) && !UtilMethods.isSet(siteList)) {
                // MULTI-TYPE EFFICIENT PATH: Use database UNION query for optimal performance
                contentTypes = this.contentTypeAPI.searchMultipleTypes(filter, types, orderByParam,
                        limit, offset, siteId, requestedContentTypes);

                // Calculate total records across all types
                for (final BaseContentType type : types) {
                    totalRecords += getTotalRecords(filter, type, List.of(siteId));
                }
            } else {
                // SINGLE-TYPE OR SPECIAL CASES PATH: Use existing logic
                final Set<ContentType> collectedContentTypes = new LinkedHashSet<>();

                for (final BaseContentType type : types) {
                    if (UtilMethods.isSet(varNamesList)) {
                        List<ContentType> optionalTypeList = this.contentTypeAPI.find(varNamesList, filter, offset, limit, orderByParam);
                        if(!type.equals(BaseContentType.ANY)) { //If this is Any type, we don't need to filter'
                            optionalTypeList = optionalTypeList.stream()
                                    .filter(contentType -> contentType.baseType().equals(type))
                                    .collect(Collectors.toList());
                        }
                        if(!optionalTypeList.isEmpty()){
                            collectedContentTypes.addAll(optionalTypeList);
                            //in this case our universe is the total number of varNames.
                            //in case you're wondering, It's not accumulative
                            totalRecords = varNamesList.size();
                        }
                    } else if (UtilMethods.isSet(siteList)) {
                        collectedContentTypes.addAll(this.contentTypeAPI.search(siteList, filter, type, orderByParam, limit, offset));
                        totalRecords += this.getTotalRecords(BLANK, type, siteList);
                    } else {
                        collectedContentTypes.addAll(this.contentTypeAPI.search(filter, type, orderByParam, limit, offset, siteId, requestedContentTypes));
                        totalRecords += getTotalRecords(filter, type, List.of(siteId));
                    }
                }

                // For collected results from individual queries, apply pagination slice
                contentTypes = applySafeSlice(new ArrayList<>(collectedContentTypes), offset, limit);
            }
            final List<Map<String, Object>> contentTypesTransform = transformContentTypesToMap(contentTypes);
            setEntriesAttribute(user, contentTypesTransform,
                    this.workflowAPI.findSchemesMapForContentType(contentTypes),
                    this.workflowAPI.findSystemActionsMapByContentType(contentTypes, user),
                    extraParams);

            result.addAll(Objects.nonNull(extraParams) && extraParams.containsKey(COMPARATOR) ?
                    contentTypesTransform.stream()
                            .sorted((Comparator<Map<String, Object>>) extraParams.get(
                                    COMPARATOR)).collect(Collectors.toList())
                    : contentTypesTransform);

            result.setTotalResults(totalRecords);
            return result;
        } catch (final DotDataException | DotSecurityException e) {
            final String errorMsg = String.format("An error occurred when retrieving paginated Content Types: " +
                    "%s", ExceptionUtil.getErrorMessage(e));
            Logger.error(this, errorMsg, e);
            throw new DotRuntimeException(errorMsg, e);
        }
    }

    /**
     * Backwards compatibility method that guarantees we can get from the extraParams map a string or list as aList
     * @param extraParams
     * @return
     */
    private static List<String> getBaseTypeNames(final Map<String, Object> extraParams) {
        return Try.of(() -> {
            final Object o = extraParams.get(TYPE_PARAMETER_NAME);
            if (o instanceof String) {
                return List.of(o.toString());
            }
            if (o instanceof Collection) {
                return new ArrayList<>((Collection<String>) o);
            }
            return List.of(BaseContentType.ANY.name());
        }).getOrElse(() -> List.of(BaseContentType.ANY.name()));
    }

    /**
     * Adds additional information related to the Content Types being returned by this Paginator. This new data is not
     * returned by the Content Type API, so other APIs must be accessed in order to provide it. Then, such data will be
     * added to the appropriate Content Type by adding it to its property Map.
     *
     * @param user                  The {@link User} performing this action.
     * @param contentTypesTransform The list of {@link ContentType} objects in the form of a list {@link Map} objects.
     * @param workflowSchemes       The Map containing the list of {@link WorkflowScheme} objects for every Content
     *                              Type.
     * @param systemActionMappings  The Map containing the list of {@link SystemActionWorkflowActionMapping} objects for
     *                              every Content Type.
     */
    private void setEntriesAttribute(final User user, final List<Map<String, Object>> contentTypesTransform,
                                     final Map<String, List<WorkflowScheme>> workflowSchemes,
                                     final Map<String, List<SystemActionWorkflowActionMapping>> systemActionMappings,
                                     final Map<String, Object> extraParams)  {

        Map<String, Long> entriesByContentTypes = null;

        try {
            entriesByContentTypes = Objects.nonNull(extraParams) && extraParams.containsKey(ENTRIES_BY_CONTENT_TYPES)?
                    (Map<String, Long>)extraParams.get(ENTRIES_BY_CONTENT_TYPES):
                    APILocator.getContentTypeAPI(user, true).getEntriesByContentTypes();
        } catch (final DotStateException e) {
            final String errorMsg = String.format("Error trying to retrieve total entries by Content Type: %s", e.getMessage());
            Logger.error(ContentTypesPaginator.class, errorMsg, e);
            Logger.debug(ContentTypesPaginator.class, e, () -> errorMsg);
        }

        for (final Map<String, Object> contentTypeEntry : contentTypesTransform) {

            final String variable = (String) contentTypeEntry.get(VARIABLE);
            if (entriesByContentTypes != null) {

                final String key = variable.toLowerCase();
                final Long contentTypeEntriesNumber = entriesByContentTypes.get(key) == null ? 0l :
                        entriesByContentTypes.get(key);
                contentTypeEntry.put(N_ENTRIES_FIELD_NAME, contentTypeEntriesNumber);
            } else {
                contentTypeEntry.put(N_ENTRIES_FIELD_NAME, StringPool.NA);
            }

            if (workflowSchemes.containsKey(variable)) {

                contentTypeEntry.put(WORKFLOWS, workflowSchemes.get(variable));
            }

            if (systemActionMappings.containsKey(variable)) {

                contentTypeEntry.put(SYSTEM_ACTION_MAPPINGS, systemActionMappings.get(variable));
            }
        }
    }

    /**
     * Transforms every Content Type in the list into a Map representation, which allows:
     * <ol>
     *     <li>An easier transformation of a Content Type into JSON data.</li>
     *     <li>Setting/retrieving properties more easily.</li>
     * </ol>
     * Additionally, this method removes unnecessary references to the fields in every Content Type, as they don't need
     * to be part of the response for this Paginator.
     *
     * @param contentTypes The list of {@link ContentType} objects that will be transformed.
     *
     * @return The list of Content Types as Maps.
     */
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

}
