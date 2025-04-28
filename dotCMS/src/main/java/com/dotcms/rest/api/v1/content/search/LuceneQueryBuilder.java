package com.dotcms.rest.api.v1.content.search;

import com.dotcms.content.elasticsearch.constants.ESMappingConstants;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rest.api.v1.content.ContentSearchForm;
import com.dotcms.rest.api.v1.content.search.handlers.FieldContext;
import com.dotcms.rest.api.v1.content.search.handlers.FieldHandlerRegistry;
import com.dotcms.rest.api.v1.content.search.strategies.FieldHandlerId;
import com.dotcms.rest.api.v1.content.search.strategies.FieldStrategy;
import com.dotcms.rest.api.v1.content.search.strategies.FieldStrategyFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.dotcms.rest.api.v1.content.search.strategies.FieldHandlerId.ARCHIVED_CONTENT;
import static com.dotcms.rest.api.v1.content.search.strategies.FieldHandlerId.FOLDER_ID;
import static com.dotcms.rest.api.v1.content.search.strategies.FieldHandlerId.GLOBAL_SEARCH;
import static com.dotcms.rest.api.v1.content.search.strategies.FieldHandlerId.LANGUAGE;
import static com.dotcms.rest.api.v1.content.search.strategies.FieldHandlerId.LIVE_CONTENT;
import static com.dotcms.rest.api.v1.content.search.strategies.FieldHandlerId.LOCKED_CONTENT;
import static com.dotcms.rest.api.v1.content.search.strategies.FieldHandlerId.SITE_ID;
import static com.dotcms.rest.api.v1.content.search.strategies.FieldHandlerId.VARIANT;
import static com.dotcms.rest.api.v1.content.search.strategies.FieldHandlerId.WORKFLOW_SCHEME;
import static com.dotcms.rest.api.v1.content.search.strategies.FieldHandlerId.WORKFLOW_STEP;
import static com.liferay.util.StringPool.BLANK;
import static com.liferay.util.StringPool.COLON;
import static com.liferay.util.StringPool.COMMA;
import static com.liferay.util.StringPool.SPACE;

 /**
 * This class provides a way to build a Lucene query based on the user's search criteria. The main
 * goal is to abstract the complexity and all the nuances of building the Lucene query from the
 * developer so dotCMS can determine the best way to retrieve the expected data. Different types of
 * user-defined searchable fields must adhere to specific rules to be included in a query. Keep in
 * mind that only fields that can be marked as {@code User Searchable} can be queried.
 * <p>The {@link ContentSearchForm} object contains all the information necessary to build the
 * query, and is able to take as much or as little data as the user wants to provide. The generated
 * Lucene query is meant to provide the same results as the ones returned in both {@code Search}
 * portlet, the dynamic search dialog in a {@code Relationships} field, and any other part of the
 * system that requires this feature.</p>
 *
 * @author Jose Castro
 * @since Jan 29th, 2025
 */
public class LuceneQueryBuilder {

    private final User user;
    private final ContentSearchForm contentSearchForm;

    private static final String DEFAULT_SORT = "score,modDate desc";

     /**
      * Creates a new instance of the {@link LuceneQueryBuilder} class.
      *
      * @param contentSearchForm The {@link ContentSearchForm} object that contains the user's
      *                          search criteria.
      * @param user              The {@link User} object that represents the user performing the
      *                          search.
      */
    public LuceneQueryBuilder(final ContentSearchForm contentSearchForm, final User user) {
        this.user = user;
        this.contentSearchForm = contentSearchForm;
    }

    /**
     * Builds the appropriate Lucene query based on the searchable parameters specified via the
     * {@link ContentSearchForm} object. Several searchable fields in dotCMS share the same value
     * formatting and/or query configuration, but others need a very specific format for
     * Elasticsearch to interpret it and return the expected results.
     *
     * @return The Lucene query that can be used to retrieve the expected data from the
     * Elasticsearch.
     *
     * @throws DotDataException     An error occurred when interacting with the database.
     * @throws DotSecurityException The specified User doesn't have the required permissions to
     *                              access specific data.
     */
    public String build() throws DotDataException, DotSecurityException {
        final Set<String> relatedContentIDs = new HashSet<>();
        final List<String> queryTerms = new ArrayList<>();
        final List<ContentType> contentTypeList = this.getContentTypes();
        queryTerms.add(this.createQuery(ESMappingConstants.TITLE, contentSearchForm.globalSearch(), GLOBAL_SEARCH));
        queryTerms.add(this.getContentTypeQuery(contentTypeList));
        queryTerms.add(String.join(SPACE, this.getSystemSearchableQueryTerms()));
        queryTerms.add(String.join(SPACE, this.getContentStatusQueryTerms()));
        final String orderByClause = this.getOrderByClause();
        final FieldContext.Builder fieldContextBuilder = new FieldContext.Builder()
                .withUser(user)
                .withPage(this.contentSearchForm.page())
                .withOffset(this.contentSearchForm.offset())
                .withSortBy(orderByClause);
        contentTypeList
                .forEach(contentType -> this.resolveFieldList(contentType)
                        .forEach(fieldVarName -> {
                            final Field field = contentType.fieldMap().get(fieldVarName);
                            if (field != null) {
                                final String luceneFieldName = contentType.variable() + "." + fieldVarName;
                                final Object fieldValue = this.getFieldValue(contentType, fieldVarName);
                                fieldContextBuilder.withContentType(contentType);
                                fieldContextBuilder.withFieldName(luceneFieldName);
                                fieldContextBuilder.withFieldValue(fieldValue);
                                Logger.debug(this, String.format("Processing Field Name '%s' with Field Value '%s'", luceneFieldName, fieldValue));
                                final Function<FieldContext, String> handler = FieldHandlerRegistry.getHandler(field.type());
                                if (null != handler) {
                                    final String query = handler.apply(fieldContextBuilder.build());
                                    if (field instanceof RelationshipField && UtilMethods.isSet(query) && !query.contains(COLON)) {
                                        // For Relationships fields, we need to track the related IDs from
                                        // all of them first and add them to the Lucene query in the end
                                        relatedContentIDs.addAll(Arrays.asList(query.split(COMMA)));
                                    } else {
                                        queryTerms.add(query);
                                    }
                                    Logger.debug(this, String.format("Generated query term for field '%s': '%s'", fieldVarName, query));
                                } else {
                                    Logger.debug(this, String.format("No Field Handler found for field '%s'. Using the default strategy", luceneFieldName));
                                    final FieldStrategy defaultStrategy = FieldStrategyFactory.getStrategy(FieldHandlerId.DEFAULT);
                                    queryTerms.add(defaultStrategy.generateQuery(fieldContextBuilder.build()));
                                }
                            }
                        }));

        if (!relatedContentIDs.isEmpty()) {
            queryTerms.add("+identifier:(" + String.join(" OR ", relatedContentIDs) + ")");
        }
        final String generatedQuery = queryTerms.stream().filter(term -> UtilMethods.isSet(term) && !term.isEmpty())
                .reduce((a, b) -> a + SPACE + b).orElse(BLANK);
        Logger.debug(this, String.format("Generated Lucene Query: %s", generatedQuery));
        return generatedQuery;
    }

    /**
     * Retrieves the value of a specific field from the {@link ContentSearchForm} object. If the
     * field is not found, it returns an empty string.
     *
     * @param contentType  The {@link ContentType} that contains the field.
     * @param fieldVarName The Velocity Variable Name of the field.
     *
     * @return The value of the field if found, otherwise an empty string.
     */
    private Object getFieldValue(final ContentType contentType, final String fieldVarName) {
        final Optional<Object> fieldValueOpt = this.contentSearchForm
                .searchableFieldsByContentTypeAndField(contentType.id(), fieldVarName);
        return fieldValueOpt.orElseGet(() ->
                this.contentSearchForm.searchableFieldsByContentTypeAndField(contentType.variable(), fieldVarName)
                        .orElse(BLANK));
    }

    /**
     * Resolves the list of fields to be used in the Lucene query based on the {@link ContentType}
     * that they belong to.
     *
     * @param contentType The {@link ContentType} that the fields belong to, if any.
     *
     * @return The list of fields to be used in the Lucene query.
     */
    private List<String> resolveFieldList(final ContentType contentType) {
        if (null == this.contentSearchForm.searchableFields() || this.contentSearchForm.searchableFields().isEmpty()) {
            return List.of();
        }
        final List<String> searchableFields = this.contentSearchForm.searchableFields(contentType.id());
        final List<String> resolvedFields = UtilMethods.isSet(searchableFields)
                ? searchableFields
                : this.contentSearchForm.searchableFields(contentType.variable());
        Logger.debug(this, String.format("Resolved Fields: %s", resolvedFields));
        return resolvedFields;
    }

    /**
     * Generates the Lucene query for the Content Types specified in the {@link ContentSearchForm}
     *
     * @param contentTypeList The list of {@link ContentType} objects to be used in the query.
     *
     * @return The Lucene query for the Content Types specified in the {@link ContentSearchForm}.
     */
    private String getContentTypeQuery(List<ContentType> contentTypeList) {
        return FieldStrategyFactory.getStrategy(FieldHandlerId.CONTENT_TYPE_IDS)
                .generateQuery(new FieldContext.Builder()
                        .withFieldName(ESMappingConstants.CONTENT_TYPE)
                        .withFieldValue(contentTypeList.stream().map(ContentType::variable).collect(Collectors.toList()))
                        .build());
    }

    /**
     * Generates the Lucene query for the system searchable attributes specified in the
     * {@link ContentSearchForm}. These fields represent query parameters that don't belong to a
     * specific Content Type. For instance:
     * <ul>
     *     <li>The global search; i.e., when users type in characters the search field.</li>
     *     <li>The site ID.</li>
     *     <li>The language ID.</li>
     *     <li>The workflow scheme ID.</li>
     *     <li>The workflow step ID.</li>
     *     <li>The variant name.</li>
     *     <li>The System Host content flag.</li>
     * </ul>
     *
     * @return The Lucene query for the system searchable fields specified in the
     * {@link ContentSearchForm}.
     */
    private List<String> getSystemSearchableQueryTerms() {
        final List<String> systemSearchableQueryTerms = Stream.of(
                        this.createSiteOrFolderQuery(),
                        this.createQuery(ESMappingConstants.LANGUAGE_ID, contentSearchForm.languageId(), LANGUAGE),
                        this.createQuery(ESMappingConstants.WORKFLOW_SCHEME, contentSearchForm.workflowSchemeId(), WORKFLOW_SCHEME),
                        this.createQuery(ESMappingConstants.WORKFLOW_STEP, contentSearchForm.workflowStepId(), WORKFLOW_STEP),
                        this.createQuery(ESMappingConstants.VARIANT, contentSearchForm.variantName(), VARIANT))
                .filter(UtilMethods::isSet)
                .collect(Collectors.toList());
        Logger.debug(this, String.format("System Searchable query terms: %s", systemSearchableQueryTerms));
        return systemSearchableQueryTerms;
    }

     /**
      * Determines whether the Lucene query must add a term to search for a Site ID, or a Folder ID.
      * Both terms are exclusive, which means only one of them can be added to the query. The
      * presence of the Site ID attribute will take precedence over the Folder ID.
      *
      * @return The Lucene query for the Site ID or the Folder ID.
      */
    private String createSiteOrFolderQuery() {
        String query = "";
        if (UtilMethods.isSet(contentSearchForm.siteId())) {
            // Passing down the 'systemHostContent' as an extra param to the SiteAttributeStrategy
            // to determine if the query should include System Host content or not
            query = this.createQuery(ESMappingConstants.CONTENTLET_HOST, contentSearchForm.siteId(), SITE_ID,
                    Map.of("systemHostContent", contentSearchForm.systemHostContent())) + SPACE;
        }
        query += this.createQuery(ESMappingConstants.CONTENTLET_FOLDER, contentSearchForm.folderId(), FOLDER_ID);
        return query.trim();
    }

     /**
      * Generates the Lucene query for the content status attributes specified in the
      * {@link ContentSearchForm}. These fields represent query parameters that don't belong to a
      * specific Content Type. For instance:
      * <ul>
      *     <li>The deleted content term.</li>
      *     <li>The locked content term.</li>
      *     <li>The unpublished content term.</li>
      *     <li>The working content term.</li>
      * </ul>
      *
      * @return The Lucene query for the content status attributes specified in the
      * {@link ContentSearchForm}.
      */
     private List<String> getContentStatusQueryTerms() {
         final List<String> contentStatusQueryTerms = Stream.of(
                         this.createQuery(ESMappingConstants.DELETED, contentSearchForm.archivedContent(), ARCHIVED_CONTENT),
                         this.createQuery(ESMappingConstants.LOCKED, contentSearchForm.lockedContent(), LOCKED_CONTENT),
                         this.createQuery(ESMappingConstants.LIVE, contentSearchForm.unpublishedContent(), LIVE_CONTENT),
                         "+working:true")
                 .filter(UtilMethods::isSet)
                 .collect(Collectors.toList());
         Logger.debug(this, String.format("Content Status query terms: %s", contentStatusQueryTerms));
         return contentStatusQueryTerms;
     }

     /**
      * Creates a Lucene query based on the field name, value, and Field Handler ID.
      *
      * @param fieldName      The search term that will be used to build the Lucene query.
      * @param value          The value of the search term.
      * @param fieldHandlerId The {@link FieldHandlerId} that will be used to build the Lucene
      *                       query.
      *
      * @return The Lucene query based on the field name, value, and Field Handler ID.
      */
    private String createQuery(final String fieldName, final Object value, final FieldHandlerId fieldHandlerId) {
        return this.createQuery(fieldName, value, fieldHandlerId, Map.of());
    }

    /**
     * Creates a Lucene query based on the field name, value, strategy ID, and extra parameters
     * provided.
     *
     * @param fieldName   The search term that will be used to build the Lucene query.
     * @param value       The value of the search term.
     * @param strategyId  The {@link FieldHandlerId} that will be used to build the Lucene query.
     * @param extraParams The extra parameters that may be used to add more data to the Lucene
     *                    query.
     *
     * @return The Lucene query based on the field name, value, strategy ID, and extra parameters
     * provided.
     */
    private String createQuery(final String fieldName, final Object value,
                               final FieldHandlerId strategyId,
                               final Map<String, Object> extraParams) {
        final FieldStrategy strategy = FieldStrategyFactory.getStrategy(strategyId);
        final FieldContext fieldContext = new FieldContext.Builder()
                .withFieldName(fieldName)
                .withFieldValue(value)
                .withExtraParams(extraParams)
                .build();
        return strategy.checkRequiredValues(fieldContext)
                ? strategy.generateQuery(fieldContext)
                : BLANK;
    }

    /**
     * Retrieves the list of {@link ContentType} objects based on the IDs specified in the
     * {@link ContentSearchForm}. Keep in mind that both the Content Type's ID or Velocity Variable
     * Name are supported.
     *
     * @return The list of {@link ContentType} objects based on the IDs specified in the
     * {@link ContentSearchForm}.
     */
    private List<ContentType> getContentTypes() {
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);
        final List<ContentType> contentTypeList = Optional.ofNullable(contentSearchForm.contentTypeIds())
                .stream()
                .flatMap(Collection::stream)
                .map(id -> this.fetchContentType(contentTypeAPI, id))
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
        Logger.debug(this, String.format("Content Types to query: %s", contentTypeList));
        return contentTypeList;
    }

    /**
     * Fetches a {@link ContentType} via the API.
     *
     * @param api           The {@link ContentTypeAPI}  used to fetch the {@link ContentType}.
     * @param contentTypeId The ID of the {@link ContentType} to be fetched.
     *
     * @return An {@link Optional} containing the {@link ContentType} if found, otherwise an empty
     * {@link Optional}.
     */
    private Optional<ContentType> fetchContentType(final ContentTypeAPI api, final String contentTypeId) {
        try {
            return Optional.ofNullable(api.find(contentTypeId));
        } catch (final DotSecurityException e) {
            Logger.error(this, String.format("User '%s' is not authorized for Content Type ID '%s'", user.getUserId(), contentTypeId), e);
        } catch (final DotDataException e) {
            Logger.error(this, String.format("Error retrieving Content Type ID '%s'", contentTypeId), e);
        }
        return Optional.empty();
    }

    /**
     * Retrieves the Lucene query sort based on the {@link ContentSearchForm} object. If no sort is
     * specified, it defaults to the value of {@link #DEFAULT_SORT}.
     *
     * @return The Lucene query sort based on the {@link ContentSearchForm} object.
     */
    public String getOrderByClause() {
        final String orderBy = UtilMethods.isSet(contentSearchForm.orderBy()) ? contentSearchForm.orderBy() : DEFAULT_SORT;
        if (!UtilMethods.isSet(orderBy)) {
            return "modDate desc";
        } else if (orderBy.equalsIgnoreCase("score,modDate desc") && this.contentSearchForm.contentTypeIds().isEmpty()) {
            return "modDate desc";
        } else if (orderBy.endsWith("__wfstep__")) {
            return "wfCurrentStepName";
        } else if (orderBy.endsWith("__wfstep__ desc")) {
            return "wfCurrentStepName desc";
        }
        Logger.debug(this, String.format("Adjusted Order by: %s", orderBy));
        return orderBy;
    }

}
