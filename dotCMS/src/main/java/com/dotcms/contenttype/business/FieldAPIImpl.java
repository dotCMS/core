package com.dotcms.contenttype.business;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.cdi.CDIUtils;
import com.dotcms.content.elasticsearch.business.IndiciesInfo;
import com.dotcms.content.elasticsearch.util.ESMappingUtilHelper;
import com.dotcms.contenttype.business.uniquefields.UniqueFieldValidationStrategyResolver;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.CheckboxField;
import com.dotcms.contenttype.model.field.ColumnField;
import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.CustomField;
import com.dotcms.contenttype.model.field.DateField;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.FileField;
import com.dotcms.contenttype.model.field.HiddenField;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable;
import com.dotcms.contenttype.model.field.JSONField;
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.field.LineDividerField;
import com.dotcms.contenttype.model.field.MultiSelectField;
import com.dotcms.contenttype.model.field.PermissionTabField;
import com.dotcms.contenttype.model.field.RadioField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.RelationshipsTabField;
import com.dotcms.contenttype.model.field.RowField;
import com.dotcms.contenttype.model.field.SelectField;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.contenttype.model.field.TabDividerField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.field.TextAreaField;
import com.dotcms.contenttype.model.field.TimeField;
import com.dotcms.contenttype.model.field.WysiwygField;
import com.dotcms.contenttype.model.field.event.FieldDeletedEvent;
import com.dotcms.contenttype.model.field.event.FieldSavedEvent;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.ContentTypeInternationalization;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.rendering.velocity.services.ContentTypeLoader;
import com.dotcms.rendering.velocity.services.ContentletLoader;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotDataValidationException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.quartz.job.CleanUpFieldReferencesJob;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.dotcms.content.elasticsearch.business.ESContentletAPIImpl.UNIQUE_PER_SITE_FIELD_VARIABLE_NAME;
import static com.dotcms.exception.ExceptionUtil.getErrorMessage;
import static com.dotcms.util.CollectionsUtils.list;
import static com.liferay.util.StringPool.BLANK;


public class FieldAPIImpl implements FieldAPI {

    private final List<Class<? extends Field>> baseFieldTypes = ImmutableList.of(BinaryField.class, StoryBlockField.class,
            CategoryField.class, ConstantField.class, CheckboxField.class, CustomField.class, DateField.class,
            DateTimeField.class, FileField.class, HiddenField.class, HostFolderField.class,
            ImageField.class, KeyValueField.class, LineDividerField.class, MultiSelectField.class,
            PermissionTabField.class, RadioField.class, RelationshipField.class, RelationshipsTabField.class, SelectField.class,
            TabDividerField.class, TagField.class, TextAreaField.class, TimeField.class,
            WysiwygField.class);

    private final PermissionAPI permissionAPI;
    private final ContentletAPI contentletAPI;
    private final UserAPI userAPI;
    private final RelationshipAPI relationshipAPI;
    private final LocalSystemEventsAPI localSystemEventsAPI;
    private final LanguageVariableAPI languageVariableAPI;
    private final FieldFactory fieldFactory;

    public FieldAPIImpl() {
        this(APILocator.getPermissionAPI(),
                APILocator.getContentletAPI(),
                APILocator.getUserAPI(),
                APILocator.getRelationshipAPI(),
                APILocator.getLocalSystemEventsAPI(),
                APILocator.getLanguageVariableAPI(),
                FactoryLocator.getFieldFactory());
    }

    @VisibleForTesting
    public FieldAPIImpl(final PermissionAPI perAPI,
                        final ContentletAPI conAPI,
                        final UserAPI userAPI,
                        final RelationshipAPI relationshipAPI,
                        final LocalSystemEventsAPI localSystemEventsAPI,
                        final LanguageVariableAPI languageVariableAPI,
                        final FieldFactory fieldFactory) {
        this.permissionAPI   = perAPI;
        this.contentletAPI   = conAPI;
        this.userAPI         = userAPI;
        this.relationshipAPI = relationshipAPI;
        this.localSystemEventsAPI = localSystemEventsAPI;
        this.languageVariableAPI = languageVariableAPI;
        this.fieldFactory = fieldFactory;
    }

    @WrapInTransaction
    @Override
    public Field save(final Field field, final User user) throws DotDataException, DotSecurityException {
        return save(field, user, true);
    }

    @WrapInTransaction
    @Override
    public Field save(final Field field, final User user, final boolean reorder)
            throws DotDataException, DotSecurityException {
        if (!UtilMethods.isSet(field.contentTypeId())) {
            Logger.error(this, "ContentTypeId needs to be set to save the Field");
            throw new DotDataValidationException("ContentTypeId needs to be set to save the Field");
        }

        ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);
        ContentType type = contentTypeAPI.find(field.contentTypeId());
        permissionAPI.checkPermission(type, PermissionLevel.EDIT_PERMISSIONS, user);

        // Search for the field in the database to see if it already exists
        var existingFieldOptional = fieldFactory.resolveExistingField(field);

        // Validating if there are actually changes to save
        if (existingFieldOptional.isPresent()) {

            final var existingField = existingFieldOptional.get();

            final Field newFieldCopy = FieldBuilder.builder(field).
                    modDate(existingField.modDate()).build();
            if (newFieldCopy.equals(existingField)) {
                Logger.debug(this, "No changes made to field: " + field.variable() + ", returning");
                return field;
            }
        }

        // Validating the field data and applying reordering if necessary
        validateAndReorder(type.id(), reorder, field, existingFieldOptional.orElse(null));

        // ---
        // Saving the field
        Field result = fieldFactory.save(field, existingFieldOptional.orElse(null));

        // And its relationships
        result = processRelationships(result, user, contentTypeAPI, type);

        // Update Content Type mod_date to detect the changes done on the field
        contentTypeAPI.updateModDate(type);

        // ---
        // Handling cache invalidations and indexing
        Structure structure = new StructureTransformer(type).asStructure();

        CacheLocator.getContentTypeCache().remove(structure);
        new ContentTypeLoader().invalidate(structure);

        if (existingFieldOptional.isPresent()) {

            final var oldField = existingFieldOptional.get();

            if (oldField.indexed() != field.indexed()) {
                contentletAPI.refresh(structure);
            } else if (field instanceof ConstantField
                    && (!StringUtils.equals(oldField.values(), field.values()))) {
                new ContentletLoader().invalidate(structure);
                contentletAPI.refresh(structure);
            }

            ActivityLogger.logInfo(ActivityLogger.class, "Update Field Action",
                    String.format(
                            "User %s/%s modified field '%s' to Content Type '%s'",
                            user.getUserId(), user.getFirstName(),
                            field.name(), structure.getVelocityVarName()
                    )
            );
        } else {

            //If saving a new indexed field, it should try to set an ES mapping for the field
            if (result.indexed()) {
                addESMappingForField(structure, result);
            }
            ActivityLogger.logInfo(ActivityLogger.class, "Save Field Action",
                    String.format(
                            "User %s/%s added field '%s' to Content Type '%s'",
                            user.getUserId(), user.getFirstName(), field.name(),
                            structure.getVelocityVarName()
                    )
            );
        }

        Field finalResult = result;
        HibernateUtil.addCommitListener(() ->
                localSystemEventsAPI.notify(new FieldSavedEvent(finalResult))
        );

        return result;
    }

    /**
     * Processes relationships for the given field. If the field is a {@link RelationshipField}, it
     * ensures that the relationship record is added or updated.
     *
     * @param field          the field for which relationships are processed
     * @param user           the user performing the operation
     * @param contentTypeAPI the ContentTypeAPI instance for accessing content type operations
     * @param type           the content type to which the field belongs
     * @return the field with updated relationship information
     * @throws DotDataException     if a data access error occurs
     * @throws DotSecurityException if a security violation occurs
     */
    private Field processRelationships(final Field field, final User user,
                                       final ContentTypeAPI contentTypeAPI, final ContentType type)
            throws DotDataException, DotSecurityException {

        //if RelationshipField, Relationship record must be added/updated
        if (field instanceof RelationshipField
                && !(((RelationshipField) field).skipRelationshipCreation())) {

            Optional<Relationship> relationship = getRelationshipForField(
                    field, contentTypeAPI, type, user
            );

            if (relationship.isPresent()) {
                relationshipAPI.save(relationship.get());
                Logger.info(
                        this,
                        "The relationship has been saved successfully for field " + field.name()
                );
            }

            //update field reference (in case it might have been modified)
            return fieldFactory.byId(field.id());
        }

        return field;
    }

    /**
     * Validates and reorders the field if necessary. If the field is new, it validates the variable
     * name. For existing fields, it ensures the content type and relationship types are
     * consistent.
     *
     * @param contentTypeId the ID of the content type to which the field belongs
     * @param reorder       flag indicating if the fields should be reordered
     * @param field         the field to be validated and potentially reordered
     * @param oldField      the existing field from the database, if it exists
     * @throws DotDataException if a data access error occurs
     */
    private void validateAndReorder(final String contentTypeId, final boolean reorder,
                                    final Field field, final Field oldField) throws DotDataException {

        if (null != oldField) {

            validateExistingField(field, oldField);

            if (reorder) {
                reorderFields(contentTypeId, field, oldField);
            }

        } else {

            //This validation should only be for new fields, since the field velocity var name(variable) can not be modified
            validateVariableName(field);

            if (reorder) {
                fieldFactory.moveSortOrderForward(contentTypeId, field.sortOrder());
            }
        }

        if (field instanceof RelationshipField
                && !(((RelationshipField) field).skipRelationshipCreation())) {
            validateRelationshipField(field);
        }
    }

    /**
     * Validates that an existing field's content type and relationship type cannot be modified.
     *
     * @param field    the field to be validated
     * @param oldField the existing field from the database
     * @throws DotDataException           if a data access error occurs
     * @throws DotDataValidationException if the validation fails
     */
    private void validateExistingField(final Field field, final Field oldField)
            throws DotDataException {

        if (!oldField.contentTypeId().equals(field.contentTypeId())) {
            throw new DotDataValidationException(
                    String.format("Content type property in fields can not be "
                            + "modified, use [%s] instead", oldField.contentTypeId())
            );
        }

        if (oldField instanceof RelationshipField && null != oldField.relationType()
                && null != field.relationType()
                && !oldField.relationType().equals(field.relationType())) {

            final var errorMessage = "Related content type cannot be modified. A new "
                    + "relationship field should be created instead";
            Logger.error(this, errorMessage);
            throw new DotDataException(errorMessage);
        }
    }

    /**
     * Validates the variable name of a new field. The variable name must follow specific naming
     * conventions.
     *
     * @param field the field to be validated
     * @throws DotDataValidationException if the variable name is invalid
     */
    private void validateVariableName(final Field field) throws DotDataValidationException {

        if (UtilMethods.isSet(field.variable()) && !field.variable()
                .matches("^[A-Za-z][0-9A-Za-z]*")) {

            final String errorMessage = "Field velocity var name " + field.variable()
                    + " contains characters not allowed, here is a suggestion of the variable: "
                    + com.dotmarketing.util.StringUtils.camelCaseLower(field.variable());
            Logger.error(this, errorMessage);
            throw new DotDataValidationException(errorMessage);
        }
    }

    /**
     * Reorders the fields of a content type if the sort order of the field has changed.
     *
     * @param contentTypeId the ID of the content type to which the field belongs
     * @param field         the field to be reordered
     * @param oldField      the existing field from the database
     * @throws DotDataException if a data access error occurs
     */
    private void reorderFields(final String contentTypeId, final Field field, final Field oldField)
            throws DotDataException {

        if (oldField.sortOrder() != field.sortOrder()) {
            if (oldField.sortOrder() > field.sortOrder()) {
                fieldFactory.moveSortOrderForward(
                        contentTypeId,
                        field.sortOrder(),
                        oldField.sortOrder()
                );
            } else {
                fieldFactory.moveSortOrderBackward(
                        contentTypeId,
                        oldField.sortOrder(),
                        field.sortOrder()
                );
            }
        }
    }

    /**
     * This method tries to set an ES mapping for the field.
     * In case of failure, we just log a warning and continue with the transaction
     * @param field
     */
    private void addESMappingForField(final Structure structure, final Field field) {
        try {
            final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();
            if (indiciesInfo != null){
                if (UtilMethods.isSet(indiciesInfo.getLive())) {
                    ESMappingUtilHelper.getInstance().addCustomMapping(field, indiciesInfo.getLive());
                    Logger.debug(this.getClass(), () -> String.format(
                            "Elasticsearch mapping set for Field: %s. Content type: %s on Index: %s",
                            field.name(), structure.getName(), APILocator.getESIndexAPI()
                                    .removeClusterIdFromName(indiciesInfo.getLive())));
                }

                if (UtilMethods.isSet(indiciesInfo.getWorking())) {
                    ESMappingUtilHelper.getInstance().addCustomMapping(field, indiciesInfo.getWorking());
                    Logger.debug(this.getClass(), () -> String.format(
                            "Elasticsearch mapping set for Field: %s. Content type: %s on Index: %s",
                            field.name(), structure.getName(), APILocator.getESIndexAPI()
                                    .removeClusterIdFromName(indiciesInfo.getWorking())));
                }
            }

        } catch (Exception e) {
            Logger.warnAndDebug(this.getClass(), String.format(
                    "Error trying to set Elasticsearch mapping for Field: %s. Content type: %s",
                    field.name(), structure.getName()), e);
        }
    }

    /**
     * Validates that properties n a relationship field are set correctly
     * @param field
     * @throws DotDataException
     */
    private void validateRelationshipField(Field field) throws DotDataValidationException {

        String errorMessage = null;
        if(!UtilMethods.isSet(field.relationType())){
            errorMessage = "Relation Type needs to be set";
        }
        if (!field.indexed()){
            errorMessage = "Relationship Field " + field.name() + " must always be indexed";
        }

        if (field.listed()){
            errorMessage = "Relationship Field " + field.name() + " cannot be listed";
        }
        if(!UtilMethods.isSet(field.values())){
            errorMessage = "Relationship Cardinality needs to be set";
        }else {

            final int cardinality = Integer.parseInt(field.values());

            //check if cardinality is valid
            if (cardinality < 0 || cardinality >= RELATIONSHIP_CARDINALITY.values().length) {
                errorMessage = "Cardinality value is incorrect";
            }
        }

        if (errorMessage != null){
            Logger.error(this, errorMessage);
            throw new DotDataValidationException(errorMessage);
        }
    }

    /**
     * Verify if a relationship already exists for this field. Otherwise, a new relationship object
     * is created
     * @param field
     * @param contentTypeAPI
     * @param type
     * @return
     * @throws DotDataException
     */
    @VisibleForTesting
    Optional<Relationship> getRelationshipForField(final Field field, final ContentTypeAPI contentTypeAPI,
                                                   final ContentType type, final User user) throws DotDataException, DotSecurityException {
        Relationship relationship;
        ContentType relatedContentType;
        try {
            final int cardinality = Integer.parseInt(field.values());

            final String[] relationType = field.relationType().split("\\.");

            //we need to find the id of the related structure using the velocityVarName set in the relationType
            try {
                relatedContentType = contentTypeAPI.find(relationType[0]);
            } catch (NotFoundInDbException e) {
                final String errorMessage = "Unable to save relationships for field " + field.name()
                        + " because the related content type " + relationType[0]
                        + " was not found in DB."
                        + " A new attempt will be made when the related content type is saved.";
                Logger.info(this, errorMessage);
                Logger.debug(this, errorMessage, e);

                return Optional.empty();
            }

            //getting the existing relationship
            relationship = relationshipAPI.getRelationshipFromField(field, user);

            //verify if the relationship already exists
            if (UtilMethods.isSet(relationship) && UtilMethods.isSet(relationship.getInode())) {

                updateRelationshipObject(field, type, relatedContentType, relationship, cardinality, user);

            } else {
                //otherwise, a new relationship will be created
                relationship = new Relationship(type, relatedContentType, field);
            }

        } catch (DotSecurityException e) {
            Logger.error(this, "Error saving relationship for field: " + field.variable(), e);
            throw e;
        } catch (Exception e) {
            //we need to capture any error found during relationship creation
            //(ie.: NumberFormatException, NullPointerException, ArrayOutOfBoundException)
            Logger.error(this, "Error saving relationship for field: " + field.variable(), e);
            throw new DotDataException(e);
        }

        return Optional.of(relationship);
    }

    /**
     * Update properties in a existing relationship
     * In case the `required` property is set to true, the other side of the relationship (if exists) will be switched to not required
     * @param field
     * @param type
     * @param relatedContentType
     * @param relationship
     * @param cardinality
     * @throws DotDataException
     */
    private void updateRelationshipObject(final Field field, final ContentType type, final ContentType relatedContentType,
                                          final Relationship relationship, final int cardinality, final User user)
            throws DotDataException {

        final boolean isChildField;
        final String relationName = field.variable();
        FieldBuilder builder;

        if (relationshipAPI.sameParentAndChild(relationship)){
            isChildField = relationship.getParentRelationName() != null && relationship
                    .getParentRelationName().equals(field.variable()) || (
                    relationship.getParentRelationName() == null && !relationship
                            .getChildRelationName().equals(field.variable()));

        } else{
            isChildField = relationship.getChildStructureInode().equals(type.id());
        }

        //check which side of the relationship is being updated (parent or child)
        if (isChildField) {
            //parent is updated
            relationship.setParentRelationName(relationName);
            relationship.setParentRequired(field.required());


            //only one side of the relationship can be required
            if (relationship.getChildRelationName() != null && field.required() && relationship.isChildRequired()) {
                //setting as not required this side of the relationship
                relationship.setParentRequired(false);

                sendRelationshipErrorMessage(relationship.getParentRelationName(), user);

                builder = FieldBuilder.builder(field);
                builder.required(false);
                fieldFactory.save(builder.build());
            }

            if (relationship.getChildRelationName() != null) {
                //verify if the cardinality was changed to update it on the other side of the relationship
                final Field otherSideField = byContentTypeAndVar(relatedContentType,
                        relationship.getChildRelationName());
                if (!otherSideField.values().equals(field.values())) {
                    //if cardinality changes, the other side field will be updated with the new cardinality
                    builder = FieldBuilder.builder(otherSideField);
                    fieldFactory.save(builder.values(field.values()).build());
                }
            }
        } else {
            //child is updated
            relationship.setChildRelationName(relationName);
            relationship.setChildRequired(field.required());

            //only one side of the relationship can be required
            if (field.required() && relationship.getParentRelationName() != null && relationship.isParentRequired()) {
                //setting this side of the relationship as not required
                relationship.setChildRequired(false);
                sendRelationshipErrorMessage(relationship.getChildRelationName(), user);

                builder = FieldBuilder.builder(field);
                builder.required(false);
                fieldFactory.save(builder.values(field.values()).build());
            }

            //verify if the cardinality was changed to update it on the other side of the relationship
            if (relationship.getParentRelationName() != null) {
                final Field otherSideField = Try.of(()->byContentTypeAndVar(relatedContentType,
                        relationship.getParentRelationName())).getOrNull();

                if (otherSideField!=null && !otherSideField.values().equals(field.values())) {
                    //if cardinality changes, the other side field will be updated with the new cardinality
                    builder = FieldBuilder.builder(otherSideField);
                    fieldFactory.save(builder.values(field.values()).build());
                }
            }

        }
        relationship.setCardinality(cardinality);
    }

    private void sendRelationshipErrorMessage(
            final String relationName,
            final User user
    ) {

        final SystemMessageEventUtil systemMessageEventUtil = SystemMessageEventUtil.getInstance();

        try {
            systemMessageEventUtil.pushMessage(
                    new SystemMessageBuilder()
                            .setMessage(LanguageUtil.format(
                                    user.getLocale(),
                                    "contenttypes.field.properties.relationship.required.error",
                                    relationName)
                            )
                            .setSeverity(MessageSeverity.INFO)
                            .setType(MessageType.SIMPLE_MESSAGE)
                            .setLife(6000)
                            .create(),
                    list(user.getUserId())
            );
        } catch (LanguageException e) {
            throw new DotRuntimeException(e);
        }
    }

    @Override
    public void save(final List<FieldVariable> fieldVariables, final Field field) {
        if (null == field) {
            Logger.error(getClass(), "Failed to save Field Variables as Field is null");
            return;
        }
        if (UtilMethods.isNotSet(field.id())) {
            Logger.error(getClass(), String.format("Failed to save Field Variables as Field ID in " +
                    "'%s' is missing", field.name()));
            return;
        }
        try {
            final List<FieldVariable> existingVariables = loadVariables(field);
            // Delete variables that either:
            // 1. Don't have a complete match (key, id, value) in incoming list, OR
            // 2. Their key doesn't exist at all in incoming list
            existingVariables.stream()
                    .filter(existingVar -> {
                        // Check if key exists in incoming list
                        boolean keyExistsInIncoming = fieldVariables.stream()
                                .anyMatch(incomingVar -> hasSameKey(existingVar, incomingVar));

                        if (!keyExistsInIncoming) {
                            // Key doesn't exist in incoming list - delete it
                            return true;
                        }

                        // Key exists - check if there's a complete match
                        boolean hasCompleteMatch = fieldVariables.stream()
                                .anyMatch(incomingVar -> matchesFieldVariable(existingVar, incomingVar));

                        // Delete if no complete match found
                        return !hasCompleteMatch;
                    })
                    .forEach(varToDelete -> {
                        try {
                            delete(varToDelete);
                        } catch (final DotDataException | UniqueFieldValueDuplicatedException e) {
                            throw new DotStateException(e);
                        }
                    });

            // Save only variables that don't already exist (by key, id, and value)
            fieldVariables.stream()
                    .filter(incomingVar -> existingVariables.stream()
                            .noneMatch(existingVar -> matchesFieldVariable(existingVar, incomingVar))
                    )
                    .forEach(fieldVariable -> {
                        try {
                            save(ImmutableFieldVariable.builder()
                                            .from(fieldVariable)
                                            .fieldId(field.id())
                                            .id(null)
                                            .build(),
                                    APILocator.systemUser());
                        } catch (final DotDataException | DotSecurityException e) {
                            throw new DotStateException(String.format("Failed to save Field Variable " +
                                    "'%s': %s", fieldVariable.key(), getErrorMessage(e)), e);
                        }
                    });
        } catch (final DotDataException e) {
            throw new DotStateException(String.format("Failed to save Field Variables in Field " +
                    "'%s' [ %s ]: %s", field.name(), field.id(), getErrorMessage(e)), e);
        }
    }

    /**
     * Checks if two field variables have the same key.
     */
    private boolean hasSameKey(final FieldVariable var1, final FieldVariable var2) {
        return var1.key() != null && var1.key().equals(var2.key());
    }

    /**
     * Checks if two field variables match by comparing their key, id, and value.
     */
    private boolean matchesFieldVariable(final FieldVariable var1, final FieldVariable var2) {
        return var1.key() != null && var1.key().equals(var2.key()) &&
                var1.id() != null && var1.id().equals(var2.id()) &&
                var1.value() != null && var1.value().equals(var2.value());
    }

    @WrapInTransaction
    @Override
    public FieldVariable save(final FieldVariable var, final User user) throws DotDataException, DotSecurityException {
        ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);
        Field field = fieldFactory.byId(var.fieldId());

        ContentType type = contentTypeAPI.find(field.contentTypeId()) ;
        APILocator.getPermissionAPI().checkPermission(type, PermissionLevel.EDIT_PERMISSIONS, user);

        FieldVariable newFieldVariable = fieldFactory.save(ImmutableFieldVariable.builder().from(var).userId(user.getUserId()).build());

        //update Content Type mod_date to detect the changes done on the field variables
        contentTypeAPI.updateModDate(type);

        //Validates custom mapping format
        if (var.key().equals(FieldVariable.ES_CUSTOM_MAPPING_KEY)){
            try {
                new JSONObject(var.value());
            } catch (JSONException e) {
                handleInvalidCustomMappingError(var, user, field, type, e);
            }

            //Verifies the field is marked as System Indexed. In case it isn't, the field will be updated with this flag on
            if (!field.indexed()) {
                save(FieldBuilder.builder(field).indexed(true).build(), user);
                Logger.info(this, "Field " + type.variable() + "." + field.variable()
                        + " has been marked as System Indexed as it has defined a field variable with key "
                        + FieldVariable.ES_CUSTOM_MAPPING_KEY);
            }
        } else if (var.key().equals(UNIQUE_PER_SITE_FIELD_VARIABLE_NAME)) {
            final Optional<String> previousValueOpt = field.fieldVariableValue(UNIQUE_PER_SITE_FIELD_VARIABLE_NAME);
            if (previousValueOpt.isPresent() && previousValueOpt.get().equalsIgnoreCase(newFieldVariable.value())) {
                // 'uniquePerSite' value was not changed, do not recalculate
                return newFieldVariable;
            }
            final UniqueFieldValidationStrategyResolver resolver =
                    CDIUtils.getBeanThrows(UniqueFieldValidationStrategyResolver.class);
            try {
                this.sendStartRecalculationNotification(user, field);
                resolver.get().recalculate(field, Boolean.parseBoolean(newFieldVariable.value()));
                this.sendEndRecalculationNotification(user, field);
            } catch (final UniqueFieldValueDuplicatedException e) {
                this.sendFailedRecalculationNotification(user, field);
                Logger.error(this, getErrorMessage(e), e);
                throw new DotDataException(e);
            }
        }
        return newFieldVariable;
    }

    /**
     * Sends a system message (warning) when a custom mapping is invalid
     * @param fieldVariable
     * @param user
     * @param field
     * @param type
     * @param exception
     */
    private void handleInvalidCustomMappingError(final FieldVariable fieldVariable, final User user,
                                                 final Field field, final ContentType type, final JSONException exception) {
        final String message;
        try {
            message = "Invalid format on field variable value. Value should be a JSON object. Field variable: "
                    + fieldVariable.key() + ". Field: " + field.name() + ". Content Type: " + type
                    .name();

            final SystemMessageEventUtil systemMessageEventUtil = SystemMessageEventUtil.getInstance();

            systemMessageEventUtil.pushMessage(
                    new SystemMessageBuilder()
                            .setMessage(LanguageUtil.get(
                                    user.getLocale(),
                                    "message.fieldvariables.invalid.custom.mapping"))
                            .setSeverity(MessageSeverity.WARNING)
                            .setType(MessageType.SIMPLE_MESSAGE)
                            .setLife(6000)
                            .create(),
                    list(user.getUserId())
            );
            Logger.warnAndDebug(FieldAPIImpl.class, message, exception);
        } catch (LanguageException ex) {
            throw new DotRuntimeException(ex);
        }
    }

    @Override
    public void delete(final Field field) throws DotDataException {
        try {
            this.delete(field, this.userAPI.getSystemUser());
        } catch (DotSecurityException e){
            throw new DotDataException(e);
        }
    }

    @WrapInTransaction
    @Override
    public void delete(final Field field, final User user) throws DotDataException, DotSecurityException {

        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);
        final ContentType type = contentTypeAPI.find(field.contentTypeId());

        permissionAPI.checkPermission(type, PermissionLevel.EDIT_PERMISSIONS, user);

        Field oldField = fieldFactory.byId(field.id());
        if(oldField.fixed() || oldField.readOnly()){
            throw new DotDataException("You cannot delete a fixed or read only field");
        }

        final Structure structure = new StructureTransformer(type).asStructure();

        fieldFactory.moveSortOrderBackward(type.id(), oldField.sortOrder());

        fieldFactory.delete(field);

        ActivityLogger.logInfo(ActivityLogger.class, "Delete Field Action",
                String.format("User %s/%s deleted field %s from %s Content Type.", user.getUserId(), user.getFirstName(),
                        field.name(), structure.getName()));

        //update Content Type mod_date to detect the changes done on the field
        contentTypeAPI.updateModDate(type);

        CacheLocator.getContentTypeCache().remove(structure);

        //if RelationshipField, Relationship record must be updated/deleted
        if (field instanceof RelationshipField) {
            removeRelationshipLink(field, type, contentTypeAPI);
        }

        // rebuild contentlets indexes
        if(field.indexed()){
            contentletAPI.reindex(structure);
        }

        CleanUpFieldReferencesJob.triggerCleanUpJob(field, user);
        sendToast(user, "Starting job", "Starting cleanUpFieldReferencesJob for field '" + field.name() + "'");
        final SystemMessageEventUtil messageEventUtil = SystemMessageEventUtil.getInstance();
        sendLegacyNotification(user,messageEventUtil, "Starting cleanUpFieldReferencesJob for field '" + field.name() + "'");
        localSystemEventsAPI.notify(new FieldDeletedEvent(field));

    }

    /**
     * Given a field load and return its variables.
     *
     * @param field field variables belong to
     * @return list of variables
     * @throws DotDataException when SQL error happens
     */
    @Override
    @CloseDBIfOpened
    public List<FieldVariable> loadVariables(final Field field) throws DotDataException {
        return UtilMethods.isSet(field) ? fieldFactory.loadVariables(field) : Collections.emptyList();
    }

    /**
     * Remove one-sided relationship when the field is deleted
     * @param field
     * @param type
     * @param contentTypeAPI
     * @throws DotDataException
     */
    private void removeRelationshipLink(final Field field, final ContentType type,
                                        final ContentTypeAPI contentTypeAPI)
            throws DotDataException, DotSecurityException {

        final Optional<Relationship> result = relationshipAPI
                .byParentChildRelationName(type, field.variable());

        if (result.isPresent()) {
            Relationship relationship = result.get();
            //it's a one-sided relationship and must be deleted
            if (!UtilMethods.isSet(relationship.getParentRelationName()) || !UtilMethods
                    .isSet(relationship.getChildRelationName())) {
                relationshipAPI.delete(relationship);
            } else {
                //the relationship must be updated, removing one side of the relationship
                if (UtilMethods.isSet(relationship.getParentRelationName()) && relationship
                        .getParentRelationName().equals(field.variable())) {
                    unlinkParent(relationship);
                } else {
                    unlinkChild(relationship);
                }

                relationshipAPI.save(relationship);
            }

            //If it is not a self-relationship, the other content type must be reindexed
            //The current content type is reindexed in the delete method
            if (!relationshipAPI.sameParentAndChild(relationship)) {
                Structure otherSideStructure;
                if (relationship.getChildStructureInode().equals(field.contentTypeId())) {
                    otherSideStructure = new StructureTransformer(
                            contentTypeAPI.find(relationship.getParentStructureInode()))
                            .asStructure();
                } else {
                    otherSideStructure = new StructureTransformer(
                            contentTypeAPI.find(relationship.getChildStructureInode()))
                            .asStructure();
                }

                contentletAPI.reindex(otherSideStructure);
            }
        }
    }

    /**
     *
     * @param relationship
     */
    private void unlinkParent(final Relationship relationship) {
        relationship.setParentRequired(false);
        relationship.setParentRelationName(null);
    }

    /**
     *
     * @param relationship
     */
    private void unlinkChild(final Relationship relationship) {
        relationship.setChildRequired(false);
        relationship.setChildRelationName(null);
    }


    @CloseDBIfOpened
    @Override
    public List<Field> byContentTypeId(final String typeId) throws DotDataException {
        return fieldFactory.byContentTypeId(typeId);
    }

    @CloseDBIfOpened
    @Override
    public String nextAvailableColumn(final Field field) throws DotDataException{
        return fieldFactory.nextAvailableColumn(field);
    }

    @CloseDBIfOpened
    @Override
    public Field find(final String id) throws DotDataException {
        return fieldFactory.byId(id);
    }

    @CloseDBIfOpened
    @Override
    public Field byContentTypeAndVar(final ContentType type, final String fieldVar) throws DotDataException {
        return fieldFactory.byContentTypeFieldVar(type, fieldVar);
    }

    @CloseDBIfOpened
    @Override
    public Optional<Field> byContentTypeAndFieldRelationType(final String id,
                                                             final String fieldRelationType) throws DotDataException {
        return fieldFactory.byContentTypeIdFieldRelationTypeInDb(id, fieldRelationType);
    }

    @CloseDBIfOpened
    @Override
    public Field byContentTypeIdAndVar(final String id, final String fieldVar) throws DotDataException {
        try {
            return byContentTypeAndVar(APILocator.getContentTypeAPI(APILocator.systemUser()).find(id), fieldVar);
        } catch (DotSecurityException e) {
            throw new DotDataException(e);
        }
    }

    @WrapInTransaction
    @Override
    public void deleteFieldsByContentType(final ContentType type) throws DotDataException {
        final List<Field> fields = byContentTypeId(type.id());
        for (Field field : fields) {
            delete(field);
        }
    }

  @Override
  public List<Class<? extends Field>> fieldTypes() {
    return baseFieldTypes;
  }

  @Override
  public void registerFieldType(Field type) {
    throw new DotStateException("Not implemented");
  }

  @Override
  public void deRegisterFieldType(Field type) {
    throw new DotStateException("Not implemented");
  }

  @WrapInTransaction
  @Override
  public void delete(final FieldVariable fieldVar) throws DotDataException, UniqueFieldValueDuplicatedException {
    fieldFactory.delete(fieldVar);
    final Field field = this.find(fieldVar.fieldId());
    final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(this.userAPI.getSystemUser());
	ContentType type;
	try {
		type = contentTypeAPI.find(field.contentTypeId());
		 //update Content Type mod_date to detect the changes done on the field variable
		contentTypeAPI.updateModDate(type);
    } catch (final DotSecurityException e) {
        final String errorMsg = String.format("Error updating Content Type mode_date containing Field Variable " +
                "'%s': %s", fieldVar.key(), getErrorMessage(e));
        throw new DotDataException(errorMsg);
    }
    if (fieldVar.key().equals(UNIQUE_PER_SITE_FIELD_VARIABLE_NAME)) {
        final UniqueFieldValidationStrategyResolver resolver =
              CDIUtils.getBeanThrows(UniqueFieldValidationStrategyResolver.class);
        // During bundle publishing or background jobs, HTTP request context is not available
        // because bundle processing runs in background threads (Quartz jobs or thread pools).
        // The HTTP request lifecycle completes before the background thread executes, and
        // Tomcat recycles request objects after the HTTP response completes.
        //
        // SECURITY NOTE: This method is called during ContentType save operations. The actual
        // permission checks are performed earlier in the call chain by ContentTypeAPI using
        // the user from PublisherConfig (bundle owner). The user here is only used for sending
        // notifications, not for permission checks. Using systemUser as fallback is acceptable
        // for notifications in background thread contexts.
        User user = Try.of(() -> {
            final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
            if (request != null) {
                return WebAPILocator.getUserWebAPI().getLoggedInUser(request);
            }
            return (User) null;
        }).getOrElse((User) null);
        
        // If no user from request (bundle publishing, background jobs), use system user
        // This is safe because permission checks happen earlier in ContentTypeAPI.save()
        if (user == null) {
            user = APILocator.systemUser();
        }
        
        try {
            this.sendStartRecalculationNotification(user, field);
            resolver.get().recalculate(field, false);
            this.sendEndRecalculationNotification(user, field);
        } catch (final UniqueFieldValueDuplicatedException e) {
            this.sendFailedRecalculationNotification(user, field);
            throw e;
        }
	}
  }

    /**
     * Delete a bunch of fields, if a Exception is throw deleting any field then no field is delete
     *
     * @param fieldsID fields's id to delete
     * @param user user who delete the fields
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
  @WrapInTransaction
  public Collection<String> deleteFields(final List<String> fieldsID, final User user) throws DotDataException, DotSecurityException {

    final List<String> deleteIds = new ArrayList<>();

    for (final String fieldId : fieldsID) {
        try {
            final Field field = find(fieldId);
            delete(field, user);
            deleteIds.add(field.id());
        } catch (NotFoundInDbException e) {
            continue;
        }
    }

    return deleteIds;
  }

  @WrapInTransaction
  @Override
  public void saveFields(final List<Field> fields, final User user) throws DotSecurityException, DotDataException {
    for (final Field field : fields) {
        save(field, user, false);
    }
  }

    @Override
    public Map<String, Object> getFieldInternationalization(
            final ContentType contentType,
            final ContentTypeInternationalization contentTypeInternationalization,
            final Map<String, Object> fieldMap
    )  {
        return getFieldInternationalization(contentType, contentTypeInternationalization, fieldMap, APILocator.systemUser());
    }

    @Override
    public Map<String, Object> getFieldInternationalization(
            final ContentType contentType,
            final ContentTypeInternationalization contentTypeInternationalization,
            final Map<String, Object> fieldMap,
            final User user
    )  {

        final long languageId = contentTypeInternationalization.getLanguageId();
        final boolean live = contentTypeInternationalization.isLive();

        try {
            final ImmutableMap.Builder<String, Object> builder = new ImmutableMap.Builder<>();

            for (final String propertyName : fieldMap.keySet()) {
                final String key = String.format("%s.%s.%s", contentType.variable(), fieldMap.get("variable"), propertyName);
                final String i18nValue = this.languageVariableAPI.getLanguageVariable(
                        key, languageId, user, live, user == null);

                if (!i18nValue.equals(key) && !i18nValue.equals(fieldMap.get(propertyName).toString())) {
                    builder.put(propertyName, i18nValue);
                } else {
                    builder.put(propertyName, fieldMap.get(propertyName));
                }
            }

            return builder.build();
        } catch (DotRuntimeException e) {
            if (ExceptionUtil.causedBy(e, ConnectException.class)) {
               return  new ImmutableMap.Builder<String, Object>().putAll(fieldMap).build();
            } else {
                throw e;
            }
        }


    }

    @Override
    public boolean isFullScreenField(final com.dotcms.contenttype.model.field.Field field) {

        if(!Config.getBooleanProperty(FieldAPI.FULLSCREEN_FIELD_FEATURE_FLAG, true)){
            return false;
        }

        try {
            final ContentType type = APILocator.getContentTypeAPI(APILocator.systemUser()).find(field.contentTypeId());

            if (!(field instanceof WysiwygField ||
                    field instanceof StoryBlockField ||
                    field instanceof TextAreaField ||
                    field instanceof CustomField ||
                    field instanceof JSONField
            )) {
                return false;
            }

            boolean showFullScreen = Try.of(() -> Boolean.parseBoolean(field.fieldVariablesMap().get("showFullScreen").value())).getOrElse(true);
            if (!showFullScreen) {
                return false;
            }

            final List<com.dotcms.contenttype.model.field.Field> fields = type.fields();
            com.dotcms.contenttype.model.field.Field pppField = Try.of(() -> fields.get(fields.indexOf(field) - 3)).getOrNull();
            com.dotcms.contenttype.model.field.Field ppField = Try.of(() -> fields.get(fields.indexOf(field) - 2)).getOrNull();
            com.dotcms.contenttype.model.field.Field pField = Try.of(() -> fields.get(fields.indexOf(field) - 1)).getOrNull();
            com.dotcms.contenttype.model.field.Field nextField = Try.of(() -> fields.get(fields.indexOf(field) + 1)).getOrNull();

            // we are either in a tab or the first field
            if (pppField != null && !(pppField instanceof TabDividerField)) {
                return false;
            }
            // we are the last field in the type or there is a column next
            if (nextField != null && !(nextField instanceof TabDividerField)) {
                return false;
            }
            // there is a row > column before us
            if (!(ppField instanceof RowField) || !(pField instanceof ColumnField)) {
                return false;
            }

            return true;

        } catch (final Exception e) {
            Logger.warnAndDebug(this.getClass(), "isFullScreenField failed: " + getErrorMessage(e) + ", field: " + field, e);
        }

        return false;
    }

    /**
     * Sends a notification to the user when the recalculation of unique field values starts.
     *
     * @param user  The {@link User} that will receive the notification.
     * @param field The unique {@link Field} that the notification is associated with.
     */
    private void sendStartRecalculationNotification(final User user, final Field field) {
        this.sendNotification("message.contentlet.unique.start.recalculation", field, user);
    }

    /**
     * Sends a notification to the user when the recalculation of unique field values ends.
     *
     * @param user  The {@link User} that will receive the notification.
     * @param field The unique {@link Field} that the notification is associated with.
     */
    private void sendEndRecalculationNotification(final User user, final Field field) {
        this.sendNotification("message.contentlet.unique.finish.recalculation", field, user);
    }

    /**
     * Sends a notification to the user when the recalculation of unique field values fails.
     *
     * @param user  The {@link User} that will receive the notification.
     * @param field The unique {@link Field} that the notification is associated with.
     */
    private void sendFailedRecalculationNotification(final User user, final Field field) {
        this.sendNotification("message.contentlet.unique.failed.recalculation", field, user);
    }

    /**
     * Sends a notification to both the System Events API and the Notification API.
     *
     * @param messageKey The message to be sent to the logged-in user.
     * @param field      The unique {@link Field} that the notification is associated with.
     * @param user       The {@link User} that will receive the notification.
     */
    private void sendNotification(final String messageKey, Field field, final User user) {
        final SystemMessageEventUtil messageEventUtil = SystemMessageEventUtil.getInstance();
        final String notificationTitle = Try.of(() -> LanguageUtil.get(user,
                "message.contentlet.unique.notification.title")).getOrElse(BLANK);
        String message = Try.of(() -> LanguageUtil.get(user, messageKey)).getOrElse(BLANK);
        message = message.replace("{0}", field.name());

        sendLegacyNotification(user, messageEventUtil, message);
        sendToast(user, notificationTitle, message);
    }

    private static void sendToast(final User user, final String notificationTitle,
                                  final String message) {
        try {
            APILocator.getNotificationAPI().generateNotification(
                    new I18NMessage(notificationTitle),
                    new I18NMessage(message),
                    null,
                    NotificationLevel.INFO,
                    NotificationType.GENERIC,
                    user.getUserId(),
                    user.getLocale()
            );
        } catch (final DotDataException e) {
            // Notification could not be sent. Just move on
        }
    }

    private static void sendLegacyNotification(final User user,
                                        final SystemMessageEventUtil messageEventUtil,
                                        final String message) {
        try {
            messageEventUtil.pushSimpleTextEvent(message, user.getUserId());
        } catch (final Exception e) {
            // Notification could not be sent. Just move on
        }
    }

}
