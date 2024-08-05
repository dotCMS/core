package com.dotcms.rest.api.v1.contenttype;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.layout.FieldLayout;
import com.dotcms.contenttype.model.field.layout.FieldUtil;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.transform.contenttype.ContentTypeInternationalization;
import com.dotcms.contenttype.transform.contenttype.DetailPageTransformerImpl;
import com.dotcms.contenttype.transform.contenttype.JsonContentTypeTransformer;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.workflows.business.DotWorkflowException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.Logger;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.LocaleUtil;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;

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

    /**
     * Evaluates the content type request and returns a modified content type if necessary.
     *
     * @param contentType The content type to evaluate.
     * @param user        The user making the request.
     * @param isNew       If the content type is new or it is an update.
     * @return The resulting content type after evaluation.
     * @throws DotDataException     If an error occurs while accessing data.
     * @throws DotSecurityException If there are security restrictions preventing the evaluation.
     * @throws URISyntaxException   If the URI syntax is invalid.
     */
    public ContentType evaluateContentTypeRequest(final ContentType contentType, final User user,
            final boolean isNew) throws DotDataException, DotSecurityException, URISyntaxException {
        return evaluateContentTypeRequest(null, contentType, user, isNew);
    }

    /**
     * Evaluates the request content type data and returns a modified content type if necessary.
     *
     * @param idOrVarParameter The id or variable parameter to evaluate.
     * @param contentType      The content type to evaluate.
     * @param user             The user making the request.
     * @param isNew            If the content type is new or it is an update.
     * @return The resulting content type after evaluation.
     * @throws DotDataException     If an error occurs while accessing data.
     * @throws DotSecurityException If there are security restrictions preventing the evaluation.
     * @throws URISyntaxException   If the URI syntax is invalid.
     */
    public ContentType evaluateContentTypeRequest(final String idOrVarParameter,
            final ContentType contentType, final User user, final boolean isNew)
            throws DotDataException, DotSecurityException, URISyntaxException {

        ContentTypeBuilder updatedContentTypeBuilder = null;

        // Evaluate the id of the content type
        var foundId = resolveContentTypeId(idOrVarParameter, contentType, user, isNew);
        if (foundId != null && !foundId.equals(contentType.id())) {
            updatedContentTypeBuilder = setContentTypeId(
                    contentType,
                    updatedContentTypeBuilder,
                    foundId
            );
        }

        // Evaluate if the content type has a detail page and if it is a URI, if so, get the
        // detail page identifier and set it back into the content type detail page but as an
        // identifier.
        // If not conversion is made, the content type is returned as it is.
        var pageDetailIdentifierOptional = new DetailPageTransformerImpl(
                contentType, user).uriToId();
        if (pageDetailIdentifierOptional.isPresent() &&
                !pageDetailIdentifierOptional.get().equals(contentType.detailPage())) {
            updatedContentTypeBuilder = setContentTypeDetailPage(
                    contentType,
                    updatedContentTypeBuilder,
                    pageDetailIdentifierOptional.get()
            );
        }

        // Sort and fix the content type fields
        return sortAndFixContentTypeFields(contentType, updatedContentTypeBuilder);
    }

    /**
     * Saves the associated schemes for the provided content type.
     *
     * @param contentType The content type to save the schemes for.
     * @param workflows   The list of workflow form entries to save.
     */
    @WrapInTransaction
    public void saveSchemesByContentType(final ContentType contentType,
            final List<WorkflowFormEntry> workflows) {

        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();

        final var workflowIds = workflows.stream().
                map(workflowFormEntry -> {
                    try {
                        return resolveWorkflowId(contentType.variable(), workflowFormEntry);
                    } catch (DotDataException | DotSecurityException e) {
                        throw new DotWorkflowException(e.getMessage(), e);
                    }
                }).
                collect(Collectors.toSet());

        try {

            Logger.debug(this, () -> String.format(
                    "Saving the schemes [%s] by content type [%s]",
                    String.join(",", workflowIds), contentType.variable())
            );

            workflowAPI.saveSchemeIdsForContentType(contentType, workflowIds);
        } catch (DotDataException e) {

            Logger.error(this, e.getMessage());
            Logger.debug(this, e.getMessage(), e);
            throw new DotWorkflowException(e.getMessage(), e);
        }

    }

    /**
     * Resolves the workflow id based on the provided workflow form entry.
     *
     * @param contentTypeVariable The content type variable name.
     * @param workflowFormEntry   The workflow form entry to resolve the id for.
     * @return The resolved workflow id.
     * @throws DotDataException     If an error occurs while accessing data.
     * @throws DotSecurityException If there are security restrictions preventing the resolution.
     */
    private String resolveWorkflowId(final String contentTypeVariable,
            final WorkflowFormEntry workflowFormEntry)
            throws DotDataException, DotSecurityException {

        // Trying to find the workflow by id
        var existingWorkflowScheme = getWorkflow(workflowFormEntry.id());

        // Now trying to find by variable if not found by id
        if (null == existingWorkflowScheme) {
            existingWorkflowScheme = getWorkflow(workflowFormEntry.variableName());
        }

        if (null == existingWorkflowScheme) {

            final var key = StringUtils.isNotEmpty(workflowFormEntry.id()) ?
                    workflowFormEntry.id() : workflowFormEntry.variableName();

            final var message = String.format(
                    "Workflow Scheme [%s] in Content Type [%s] not found.",
                    key, contentTypeVariable
            );
            throw new NotFoundInDbException(message);
        }

        return existingWorkflowScheme.getId();
    }

    /**
     * Resolves the content type id based on the provided id or variable parameter, content type,
     * user, and if it's new or an update. First, it tries to resolve the content type using the
     * provided id or variable as a path parameter, if nothing is found, it will use the content
     * type found in the request form.
     *
     * @param idOrVarParameter The id or variable parameter to resolve the id for.
     * @param contentType      The content type to resolve the id for.
     * @param user             The user making the request.
     * @param isNew            Flag indicating if the content type is new or an update.
     * @return The resolved content type id.
     * @throws DotSecurityException If there are security restrictions preventing the resolution.
     * @throws DotDataException     If an error occurs while accessing data.
     */
    private String resolveContentTypeId(final String idOrVarParameter, ContentType contentType,
            final User user, boolean isNew) throws DotSecurityException, DotDataException {

        if (!isNew) {

            // First, we need to try to resolve the content type using the provided id or variable
            // as path parameter, if nothing is found, we will use the content type found in the
            // request form.
            var existingContentType = findContentType(idOrVarParameter, user);
            if (null == existingContentType) {

                // Trying to find the content type by id
                existingContentType = findContentType(contentType.id(), user);

                // Now trying to find by variable if not found by id
                if (null == existingContentType) {
                    existingContentType = findContentType(contentType.variable(), user);
                }
            }

            if (null == existingContentType) {

                final var message = String.format(
                        "Content Type [%s] not found.",
                        idOrVarParameter
                );
                throw new NotFoundInDbException(message);
            }

            if (!existingContentType.id().equals(contentType.id()) && (contentType.id() != null)) {

                // We found a content type with the same variable but different id
                final var message = String.format(
                        "Content Type to update with id [%s] not found, using existing "
                                + "Content Type with variable [%s] and id [%s] instead.",
                        contentType.id(), existingContentType.variable(),
                        existingContentType.id()
                );
                Logger.warn(ContentTypeHelper.class, message);
            }

            return existingContentType.id();
        }

        return contentType.id();
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
     * Sets the content type identifier for a given content type to the provided builder.
     *
     * @param contentType               The content type to set the identifier for.
     * @param updatedContentTypeBuilder The updated content type builder, or null if not available.
     * @param identifier                The identifier to set for the content type.
     * @return The content type builder with the identifier set.
     */
    private ContentTypeBuilder setContentTypeId(final ContentType contentType,
            final ContentTypeBuilder updatedContentTypeBuilder, final String identifier) {

        if (null != updatedContentTypeBuilder) {
            return updatedContentTypeBuilder.id(identifier);
        } else {
            return ContentTypeBuilder.builder(contentType).id(identifier);
        }
    }

    /**
     * Sets the detail page for a given content type to the provided builder.
     *
     * @param contentType               The content type to set the detail page for.
     * @param updatedContentTypeBuilder The updated content type builder, or null if not available.
     * @param pageDetail                The detail page to set for the content type.
     * @return The content type builder with the detail page set.
     * @throws NullPointerException if contentType is null
     */
    private ContentTypeBuilder setContentTypeDetailPage(final ContentType contentType,
            final ContentTypeBuilder updatedContentTypeBuilder, final String pageDetail) {

        if (null != updatedContentTypeBuilder) {
            return updatedContentTypeBuilder.detailPage(pageDetail);
        } else {
            return ContentTypeBuilder.builder(contentType).detailPage(pageDetail);
        }
    }

    /**
     * Prepares the content type fields by ordering them based on the sort order. If
     * updatedContentTypeBuilder is provided, it constructs a new ContentType with the ordered
     * fields and returns the new instance. If updatedContentTypeBuilder is null, it constructs a
     * new ContentTypeBuilder with the original contentType and constructs a new ContentType with
     * the ordered fields. If there are no fields, it returns the original contentType.
     *
     * @param contentType               The content type to prepare.
     * @param updatedContentTypeBuilder The updated content type builder, or null.
     * @return The prepared content type.
     */
    private ContentType sortAndFixContentTypeFields(final ContentType contentType,
            final ContentTypeBuilder updatedContentTypeBuilder) {

        if (null != contentType.fields()) {

            // Ordering the fields
            final var orderedFields = contentType.fields().stream().
                    sorted(Comparator.comparing(Field::sortOrder)).
                    collect(Collectors.toList());
            final var sortOrderFix = FieldUtil.fixSortOrder(orderedFields);
            final var fixedFields = sortOrderFix.getNewFields();

            if (!contentType.fields().equals(fixedFields)) {
                if (null != updatedContentTypeBuilder) {
                    final var updatedContentType = updatedContentTypeBuilder.build();
                    updatedContentType.constructWithFields(fixedFields);

                    return updatedContentType;
                } else {
                    final var contentTypeBuilder = ContentTypeBuilder.builder(contentType);
                    final var updatedContentType = contentTypeBuilder.build();
                    updatedContentType.constructWithFields(fixedFields);

                    return contentType;
                }
            }
        }

        if (null != updatedContentTypeBuilder) {
            return updatedContentTypeBuilder.build();
        } else {
            return contentType;
        }
    }

    /**
     * Fixes the layout of a content type if necessary.
     *
     * @param contentTypeId The ID of the content type to fix the layout for.
     * @param user          The user performing the operation.
     * @return The fixed content type, or the original content type if no fixes were necessary.
     * @throws DotDataException     If an error occurs in the data layer.
     * @throws DotSecurityException If the user doesn't have permission to perform the operation.
     */
    public ContentType fixLayoutIfNecessary(final String contentTypeId, final User user)
            throws DotDataException, DotSecurityException {

        // Looking for the most recent version of the content type and fields
        final var contentTypeAPI = APILocator.getContentTypeAPI(user, true);
        final var contentType = contentTypeAPI.find(contentTypeId);

        // Verifying if the layout is valid to fix it if necessary
        final FieldLayout fieldLayout = new FieldLayout(contentType);
        if (!fieldLayout.isValidate()) {

            // Fixing the layout
            APILocator.getContentTypeFieldLayoutAPI().fixLayout(fieldLayout, user);

            // Return an updated content type with the fixed layout fields
            final var fixedContentType = contentTypeAPI.find(contentTypeId);
            return setFields(contentType, fixedContentType.fields());
        }

        return contentType;
    }

    /**
     * Sets the fields of the given content type and returns the updated content type.
     *
     * @param contentType The content type to update.
     * @param fields      The list of fields to set for the content type.
     * @return The updated content type with the new fields.
     */
    private ContentType setFields(final ContentType contentType, final List<Field> fields) {

        final var contentTypeBuilder = ContentTypeBuilder.builder(contentType);
        final var updatedContentType = contentTypeBuilder.build();
        updatedContentType.constructWithFields(fields);

        return updatedContentType;
    }

    /**
     * Retrieves the existing content type based on the id or variable of the given content type.
     *
     * @param idOrVar The id or variable of the content type to evaluate.
     * @param user    The user making the request.
     * @return The existing content type if found, otherwise null.
     * @throws DotSecurityException If there are security restrictions preventing the evaluation.
     * @throws DotDataException     If an error occurs while accessing data.
     */
    private ContentType findContentType(final String idOrVar, final User user)
            throws DotSecurityException, DotDataException {

        if (StringUtils.isEmpty(idOrVar)) {
            return null;
        }

        final var contentTypeAPI = APILocator.getContentTypeAPI(user, true);

        ContentType existingContentType = null;

        try {
            existingContentType = contentTypeAPI.find(idOrVar);
        } catch (NotFoundInDbException e) {
            final var message = String.format(
                    "Content Type [%s] not found.", idOrVar
            );
            Logger.warn(ContentTypeHelper.class, message);
        }

        return existingContentType;
    }

    /**
     * Retrieves an existing workflow
     *
     * @param workflowIdOrVar The workflow id or variable to search for.
     * @return The existing workflow if found, otherwise null.
     * @throws DotSecurityException If there are security restrictions preventing the evaluation.
     * @throws DotDataException     If an error occurs while accessing data.
     */
    private WorkflowScheme getWorkflow(final String workflowIdOrVar)
            throws DotSecurityException, DotDataException {

        WorkflowScheme existingWorkflowScheme = null;

        try {
            if (StringUtils.isNotEmpty(workflowIdOrVar)) {
                existingWorkflowScheme = APILocator.getWorkflowAPI().findScheme(workflowIdOrVar);
            }
        } catch (DoesNotExistException e) {
            final var message = String.format(
                    "Workflow Scheme [%s] not found.", workflowIdOrVar
            );
            Logger.debug(ContentTypeHelper.class, message);
        }

        return existingWorkflowScheme;
    }

    /**
     * Return  a {@link List} of BaseContentTypesView
     *
     * @param request
     * @return
     * @throws DotDataException
     * @throws LanguageException
     */
    public List<BaseContentTypesView> getTypes(HttpServletRequest request)
            throws LanguageException {
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

    /**
     * Generates a key to be use when calling the {@link ContentType#fieldMap(Function)} method.
     * <p>
     * The regular {@link ContentType#fieldMap()} throws a NPE if the key (variable name) is null as
     * it is used as a key for the map.
     * <p>
     * When the {@link ContentType#fieldMap()} is called from a resource we could have cases where
     * fields are sent without a variable name.
     *
     * @param field {@link Field} to generate the key for.
     * @return The generated key.
     */
    public String generateFieldKey(final Field field) {

        final String key;
        if (StringUtils.isNotEmpty(field.id()) && StringUtils.isNotEmpty(field.variable())) {
            key = String.format("%s-%s", field.id(), field.variable());
        } else if (StringUtils.isNotEmpty(field.variable())) {
            key = field.variable();
        } else if (StringUtils.isNotEmpty(field.id())) {
            key = field.id();
        } else {
            key = field.name();
        }

        return key;
    }

} // E:O:F:ContentTypeHelper.