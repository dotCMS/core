package com.dotcms.contenttype.business;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.content.business.DotMappingException;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.CheckboxField;
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
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.field.LineDividerField;
import com.dotcms.contenttype.model.field.MultiSelectField;
import com.dotcms.contenttype.model.field.PermissionTabField;
import com.dotcms.contenttype.model.field.RadioField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.RelationshipsTabField;
import com.dotcms.contenttype.model.field.SelectField;
import com.dotcms.contenttype.model.field.TabDividerField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.field.TextAreaField;
import com.dotcms.contenttype.model.field.TimeField;
import com.dotcms.contenttype.model.field.WysiwygField;
import com.dotcms.contenttype.model.field.event.FieldDeletedEvent;
import com.dotcms.contenttype.model.field.event.FieldSavedEvent;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.rendering.velocity.services.ContentTypeLoader;
import com.dotcms.rendering.velocity.services.ContentletLoader;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotDataValidationException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.StopWatch;


public class FieldAPIImpl implements FieldAPI {

  private final List<Class> baseFieldTypes = ImmutableList.of(BinaryField.class, CategoryField.class,
      ConstantField.class, CheckboxField.class, CustomField.class, DateField.class,
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

  private final FieldFactory fieldFactory = new FieldFactoryImpl();

  public FieldAPIImpl() {
      this(APILocator.getPermissionAPI(),
          APILocator.getContentletAPI(),
          APILocator.getUserAPI(),
          APILocator.getRelationshipAPI(),
          APILocator.getLocalSystemEventsAPI());
  }

  @VisibleForTesting
  public FieldAPIImpl(final PermissionAPI perAPI,
                        final ContentletAPI conAPI,
                        final UserAPI userAPI,
                        final RelationshipAPI relationshipAPI,
                        final LocalSystemEventsAPI localSystemEventsAPI) {
      this.permissionAPI   = perAPI;
      this.contentletAPI   = conAPI;
      this.userAPI         = userAPI;
      this.relationshipAPI = relationshipAPI;
      this.localSystemEventsAPI = localSystemEventsAPI;
  }

  @WrapInTransaction
  @Override
  public Field save(final Field field, final User user) throws DotDataException, DotSecurityException {

      if(!UtilMethods.isSet(field.contentTypeId())){
          Logger.error(this, "ContentTypeId needs to be set to save the Field");
          throw new DotDataValidationException("ContentTypeId needs to be set to save the Field");
      }

		ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);
		ContentType type = contentTypeAPI.find(field.contentTypeId()) ;
		permissionAPI.checkPermission(type, PermissionLevel.EDIT_PERMISSIONS, user);

	    Field oldField = null;
	    if (UtilMethods.isSet(field.id())) {
	    	try {
	    		oldField = fieldFactory.byId(field.id());

                if(!oldField.variable().equals(field.variable())){
                    throw new DotDataValidationException("Field variable can not be modified, please use the following: " + oldField.variable());
                }

                if(!oldField.contentTypeId().equals(field.contentTypeId()) ){
                  throw new DotDataValidationException("Field content type can not be modified, "
                      + "please use the following: " + oldField.contentTypeId());
                }

                if (oldField.sortOrder() != field.sortOrder()){
	    		    if (oldField.sortOrder() > field.sortOrder()) {
                        fieldFactory.moveSortOrderForward(type.id(), field.sortOrder(), oldField.sortOrder());
                    } else {
                        fieldFactory.moveSortOrderBackward(type.id(), oldField.sortOrder(), field.sortOrder());
                    }
                }

                if (oldField instanceof RelationshipField && null != oldField.relationType()
                        && null != field.relationType() && !oldField.relationType()
                        .equals(field.relationType())) {
                    Logger.error(this,
                            "Related content type cannot be modified. A new relationship field should be created instead");
                    throw new DotDataException(
                            "Related content type cannot be modified. A new relationship field should be created instead");
                }

            } catch(NotFoundInDbException e) {
	    		//Do nothing as Starter comes with id but field is unexisting yet
	    	}
	    }else {
	        //This validation should only be for new fields, since the field velocity var name(variable) can not be modified
            if(UtilMethods.isSet(field.variable()) && !field.variable().matches("^[A-Za-z][0-9A-Za-z]*")) {
                final String errorMessage = "Field velocity var name "+ field.variable() +" contains characters not allowed, here is a suggestion of the variable: " + com.dotmarketing.util.StringUtils.camelCaseLower(field.variable());
                Logger.error(this, errorMessage);
                throw new DotDataValidationException(errorMessage);
            }
            fieldFactory.moveSortOrderForward(type.id(), field.sortOrder());
        }

        if (field instanceof RelationshipField) {
	        validateRelationshipField(field);
        }

        Field result = fieldFactory.save(field);

        //if RelationshipField, Relationship record must be added/updated
        if (field instanceof RelationshipField) {
            Logger.info(this, "Field " + field.name() + ", var name: " + field.variable()
                + " in content type: " + type.id() + "  is a relationship field, looking for existing relationship");
            Optional<Relationship> relationship = getRelationshipForField(result, contentTypeAPI,
                  type, user);

            if (relationship.isPresent()) {
              relationshipAPI.save(relationship.get());
              Logger.info(this,
                      "The relationship has been saved successfully for field " + field.name());
            }

            //update field reference (in case it might have been modified)
            result = fieldFactory.byId(result.id());
       }
      //update Content Type mod_date to detect the changes done on the field
		contentTypeAPI.updateModDate(type);
		
		Structure structure = new StructureTransformer(type).asStructure();

        CacheLocator.getContentTypeCache().remove(structure);
        new ContentTypeLoader().invalidate(structure);

      if(oldField!=null){
          if(oldField.indexed() != field.indexed()){
              contentletAPI.refresh(structure);
          } else if (field instanceof ConstantField) {
              if(!StringUtils.equals(oldField.values(), field.values()) ){
                  new ContentletLoader().invalidate(structure);
                  contentletAPI.refresh(structure);
              }
          }

          ActivityLogger.logInfo(ActivityLogger.class, "Update Field Action",
                  String.format("User %s/%s modified field %s to %s Structure.", user.getUserId(), user.getFirstName(),
                          field.name(), structure.getName()));
      } else {
          ActivityLogger.logInfo(ActivityLogger.class, "Save Field Action",
                  String.format("User %s/%s added field %s to %s Structure.", user.getUserId(), user.getFirstName(), field.name(),
                          structure.getName()));
      }

      Field finalResult = result;
      HibernateUtil.addCommitListener(()-> {
          localSystemEventsAPI.notify(new FieldSavedEvent(finalResult));
      });

      return result;
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
            Logger.info(this, "Relation types for field " + field.name() + ", var name: " + field.variable()
                    + ", related content type: " + relationType[0] + ", field content type: "
                    + (relationType.length > 1 ? relationType[1] : "()") + ", cardinality: " + cardinality);
            //we need to find the id of the related structure using the velocityVarName set in the relationType
            try {
                relatedContentType = contentTypeAPI.find(relationType[0]);
                if (relatedContentType != null) {
                    Logger.info(this, "Related content type id: " + relatedContentType.id()
                            + ", name: " + relatedContentType.name() + ", var name: " + relatedContentType.variable());
                }
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
                Logger.info(this, "Relationship exists, updating for field: " + field.name()
                        + ", var name: " + field.variable()
                        + ", related content type: " + (relatedContentType == null ? "" : relatedContentType.id())
                        + ", var name: " + (relatedContentType == null ? "" : relatedContentType.variable())
                        + ", relationship id: " + relationship.getInode()
                        + ", relationship parent name: " + relationship.getParentRelationName()
                        + ", relationship child name: " + relationship.getChildRelationName()
                        + ", relationship parent inode: " + relationship.getParentStructureInode()
                        + ", relationship child inode: " + relationship.getChildStructureInode()
                        + ", relationship type: " + relationship.getRelationTypeValue());
                updateRelationshipObject(field, type, relatedContentType, relationship, cardinality, user);

            } else {
                //otherwise, a new relationship will be created
                Logger.info(this, "Adding new relationship for field: " + field.name()
                        + ", var name: " + field.variable()
                        + ", related content type: " + (relatedContentType == null ? "" : relatedContentType.id())
                        + ", var name: " + (relatedContentType == null ? "" : relatedContentType.variable()));
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

            Logger.info(this, "Relatioship field is child field: "
                    + field.id() + ", var name: " + field.variable() + ", related content type: "
                    + relatedContentType.id() + ", with name: " + relatedContentType.name()
                    + ", and var name: " + relatedContentType.variable()
                    + ", relationship id: " + relationship.getInode());

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
                Logger.info(this, "Looking for other side field for child field: "
                        + relationship.getChildRelationName() + ", in related content type id: "
                        + relatedContentType.id() + ", with name: " + relatedContentType.name()
                        + ", and var name: " + relatedContentType.variable());
                final Field otherSideField = fieldFactory.byContentTypeIdFieldVar(relatedContentType.id(),
                        relationship.getChildRelationName());

                if (!otherSideField.values().equals(field.values())) {
                    //if cardinality changes, the other side field will be updated with the new cardinality
                    builder = FieldBuilder.builder(otherSideField);
                    fieldFactory.save(builder.values(field.values()).build());
                }
            }
        } else {

            Logger.info(this, "Relatioship field is parent field: "
                    + field.id() + ", var name: " + field.variable() + ", related content type: "
                    + relatedContentType.id() + ", with name: " + relatedContentType.name()
                    + ", and var name: " + relatedContentType.variable()
                    + ", relationship id: " + relationship.getInode());

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

                Logger.info(this, "Looking for other side field for parent field: "
                        + relationship.getParentRelationName() + ", in related content type id: "
                        + relatedContentType.id() + ", with name: " + relatedContentType.name()
                        + ", and var name: " + relatedContentType.variable());

                final Field otherSideField = fieldFactory.byContentTypeIdFieldVar(relatedContentType.id(),
                        relationship.getParentRelationName());

                if (!otherSideField.values().equals(field.values())) {
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
      
      return newFieldVariable;
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
      com.dotmarketing.portlets.structure.model.Field legacyField = new LegacyFieldTransformer(field).asOldField();

      Logger.info(this, "Deleting Field: " + field.name() + " with ID: " + field.id()
              + " and var name:" + field.variable() + ", from Content Type: " + structure.getName()
              + " with ID: " + structure.id());
      StopWatch stopWatch = new StopWatch();
      stopWatch.start();

      if (!(field instanceof CategoryField) &&
          !(field instanceof ConstantField) &&
          !(field instanceof HiddenField) &&
          !(field instanceof LineDividerField) &&
          !(field instanceof TabDividerField) &&
          !(field instanceof RelationshipsTabField) &&
          !(field instanceof RelationshipField) &&
          !(field instanceof PermissionTabField) &&
          !(field instanceof HostFolderField) &&
          structure != null
      ) {
          this.contentletAPI.cleanField(structure, legacyField, this.userAPI.getSystemUser(), false);
      }

      fieldFactory.moveSortOrderBackward(type.id(), oldField.sortOrder());
      fieldFactory.delete(field);

      stopWatch.stop();
      Logger.info(this, "Deleted Field: " + field.name() + " with ID: " + field.id()
                + " and var name:" + field.variable() + ", from Content Type: " + structure.getName()
                + " with ID: " + structure.id() + ", elapsed time: " + stopWatch);

      ActivityLogger.logInfo(ActivityLogger.class, "Delete Field Action",
          String.format("User %s/%s deleted field %s from %s Content Type.", user.getUserId(), user.getFirstName(),
              field.name(), structure.getName()));

      //update Content Type mod_date to detect the changes done on the field
      contentTypeAPI.updateModDate(type);

      CacheLocator.getContentTypeCache().remove(structure);


      //Refreshing permissions
      if (field instanceof HostFolderField) {
          try {
              this.contentletAPI.cleanHostField(structure, this.userAPI.getSystemUser(), false);
          } catch(DotMappingException e) {}

          this.permissionAPI.resetChildrenPermissionReferences(structure);
      }

      //if RelationshipField, Relationship record must be updated/deleted
      if (field instanceof RelationshipField) {
          Logger.info(this, "Removing relationship link for: " + field.name()
                  + " with ID: " + field.id() + " and var name:" + field.variable()
                  + ", from Content Type: " + structure.getName() + " with ID: " + structure.id());
          StopWatch relationshipLinkWatch = new StopWatch();
          relationshipLinkWatch.start();
          removeRelationshipLink(field, type, contentTypeAPI);
          relationshipLinkWatch.stop();
          Logger.info(this, "Removed relationship link: " + field.name() + " with ID: " + field.id()
                  + " and var name:" + field.variable() + ", from Content Type: " + structure.getName()
                  + " with ID: " + structure.id() + ", elapsed time: " + stopWatch);
      }

      // rebuild contentlets indexes
      if(field.indexed()){
          contentletAPI.reindex(structure);
      }
      // remove the file from the cache
      new ContentletLoader().invalidate(structure);

      HibernateUtil.addCommitListener(()-> {
          localSystemEventsAPI.notify(new FieldDeletedEvent(field.variable()));
      });
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
  public List<Class> fieldTypes() {
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
  public void delete(final FieldVariable fieldVar) throws DotDataException {

    fieldFactory.delete(fieldVar);
    Field field = this.find(fieldVar.fieldId());
    ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(this.userAPI.getSystemUser());
	ContentType type;
	try {
		type = contentTypeAPI.find(field.contentTypeId());
		 //update Content Type mod_date to detect the changes done on the field variable
		contentTypeAPI.updateModDate(type);
	} catch (DotSecurityException e) {
		throw new DotDataException("Error updating Content Type mode_date for FieldVariable("+fieldVar.id()+"). "+e.getMessage());
	}
  }
  
  
  
  
  
  
}
