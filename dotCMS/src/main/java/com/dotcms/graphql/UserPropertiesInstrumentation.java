package com.dotcms.graphql;

import com.dotcms.content.filter.ContentFieldFilter;
import com.dotcms.content.filter.PropertyTreeFilter;
import com.dotcms.graphql.business.PageAPIGraphQLTypesProvider;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherResult;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Keeps user properties out of the GraphQL results returned to callers who are neither CMS administrators
 * nor back-end users: in the {@code _map} field, Story Block fields and, while the user properties setting is
 * on or a content property list names it, the lock owner of a {@code DotPage}, a value that is set is sent as
 * {@link ContentFieldFilter#USER_PROPERTY_PLACEHOLDER}, and searches whose
 * query or sort names one return a field error.
 * Results for back-end callers, and fields of customer content types, are left unchanged.
 *
 * @author hassandotcms
 */
public class UserPropertiesInstrumentation extends SimpleInstrumentation {

    private static final String MAP_FIELD = "_map";
    private static final String MAP_KEY_ARGUMENT = "key";
    private static final Set<String> PAGE_LOCK_FIELDS = Set.of("lockedBy", "lockedByName");
    private static final String QUERY_ARGUMENT = "query";
    /** Sort arguments: {@code sortBy} on collections, {@code sort} as read by relationship fields. */
    private static final List<String> SORT_ARGUMENTS = List.of("sortBy", "sort");

    /**
     * Wraps the fetchers of the fields that can return or search user properties; every other fetcher is
     * returned as is.
     *
     * @param dataFetcher the field's fetcher
     * @param parameters the field being fetched
     * @return the wrapped fetcher, or the original one when the field is not concerned
     */
    @Override
    public DataFetcher<?> instrumentDataFetcher(final DataFetcher<?> dataFetcher,
                                                final InstrumentationFieldFetchParameters parameters) {
        final DataFetchingEnvironment fieldEnvironment = parameters.getEnvironment();
        final String fieldName = fieldEnvironment.getFieldDefinition().getName();
        final boolean contentMap = MAP_FIELD.equals(fieldName);
        final boolean storyBlock = CustomFieldType.STORY_BLOCK.getTypeName().equals(
                typeName(fieldEnvironment.getFieldDefinition().getType()));
        final boolean pageLock = PAGE_LOCK_FIELDS.contains(fieldName)
                && PageAPIGraphQLTypesProvider.DOT_PAGE.equals(parentTypeName(fieldEnvironment));
        final boolean search = fieldEnvironment.containsArgument(QUERY_ARGUMENT)
                || SORT_ARGUMENTS.stream().anyMatch(fieldEnvironment::containsArgument);
        if (!contentMap && !storyBlock && !pageLock && !search) {
            return dataFetcher;
        }
        return environment -> {
            final ContentFieldFilter filter = ContentFieldFilter.forUser(caller(environment));
            if (!filter.isActive()) {
                return dataFetcher.get(environment);
            }
            if (search && !filter.allowsSearch(environment.getArgument(QUERY_ARGUMENT), sortOf(environment))) {
                return DataFetcherResult.newResult()
                        .error(GraphqlErrorBuilder.newError(environment)
                                .message(filter.searchRejectedMessage()).build())
                        .build();
            }
            final Object result = dataFetcher.get(environment);
            if (pageLock) {
                return filter.restrictedProperties().contains(fieldName.toLowerCase(Locale.ROOT))
                        ? ContentFieldFilter.userPropertyValue(result)
                        : result;
            }
            if (storyBlock) {
                return PropertyTreeFilter.filter(result, filter, PropertyTreeFilter.storedContentTypes());
            }
            return contentMap ? filterContentMap(result, environment, filter) : result;
        };
    }

    /**
     * Filters the result of a {@code _map} field: the whole map, or the single value asked for with the
     * {@code key} argument.
     *
     * @param result what the original fetcher returned
     * @param environment the field being fetched
     * @param filter the caller's filter
     * @return the filtered result; {@code null} when the key asked for is removed for the caller
     */
    private Object filterContentMap(final Object result, final DataFetchingEnvironment environment,
                                    final ContentFieldFilter filter) {
        if (result instanceof DataFetcherResult) {
            final DataFetcherResult<?> fetched = (DataFetcherResult<?>) result;
            return DataFetcherResult.newResult()
                    .data(filterContentMap(fetched.getData(), environment, filter))
                    .errors(fetched.getErrors())
                    .localContext(fetched.getLocalContext())
                    .build();
        }
        final String key = environment.getArgument(MAP_KEY_ARGUMENT);
        if (null == key) {
            return PropertyTreeFilter.filter(result, filter, PropertyTreeFilter.storedContentTypes());
        }
        // Filtered with the contentlet's type, so a Story Block value asked for on its own is read like in the whole map
        final Object source = environment.getSource();
        final String contentType = source instanceof Contentlet ? ((Contentlet) source).getContentTypeId() : null;
        final Map<String, Object> single = new HashMap<>();
        single.put(key, result);
        return PropertyTreeFilter.filterContentletProperties(contentType, single, filter,
                PropertyTreeFilter.storedContentTypes()).get(key);
    }

    /**
     * Returns the user the GraphQL request runs for.
     *
     * @param environment the field being fetched
     * @return the {@link User} from the {@link DotGraphQLContext}, or {@code null} when there is none
     */
    private static User caller(final DataFetchingEnvironment environment) {
        final Object context = environment.getContext();
        return context instanceof DotGraphQLContext ? ((DotGraphQLContext) context).getUser() : null;
    }

    /**
     * Returns the sort clause of a search field, from whichever sort argument it has.
     *
     * @param environment the field being fetched
     * @return the sort clause, or {@code null}
     */
    private static String sortOf(final DataFetchingEnvironment environment) {
        return SORT_ARGUMENTS.stream()
                .map(name -> environment.<Object>getArgument(name))
                .filter(value -> value instanceof String)
                .map(String.class::cast)
                .findFirst().orElse(null);
    }

    /**
     * Returns the name of a field's type, without list or non-null wrappers.
     *
     * @param type the field's type
     * @return the type name, or {@code null} when it has none
     */
    private static String typeName(final GraphQLType type) {
        final GraphQLType unwrapped = GraphQLTypeUtil.unwrapAll(type);
        return unwrapped instanceof GraphQLNamedType ? ((GraphQLNamedType) unwrapped).getName() : null;
    }

    /**
     * Returns the name of the type that holds the field being fetched.
     *
     * @param environment the field being fetched
     * @return the parent type name, or {@code null} when it has none
     */
    private static String parentTypeName(final DataFetchingEnvironment environment) {
        return environment.getParentType() instanceof GraphQLNamedType
                ? ((GraphQLNamedType) environment.getParentType()).getName()
                : null;
    }

}
