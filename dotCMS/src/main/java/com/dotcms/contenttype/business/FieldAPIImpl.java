package com.dotcms.contenttype.business;

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
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang.StringUtils;


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
            if(UtilMethods.isSet(field.variable()) && !field.variable().matches("^[a-zA-Z0-9]+")) {
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
            Optional<Relationship> relationship = getRelationshipForField(result, contentTypeAPI,
                  type, user);

            if (relationship.isPresent()) {
              relationshipAPI.save(relationship.get());
              Logger.info(this,
                      "The relationship has been saved successfully for field " + field.name());
            }
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

      HibernateUtil.addCommitListener(()-> {
          localSystemEventsAPI.notify(new FieldSavedEvent(result));
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

                updateRelationshipObject(field, type, relatedContentType, relationship, cardinality);

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
            final Relationship relationship, final int cardinality)
            throws DotDataException {
        final String relationName = field.variable();
        //check which side of the relationship is being updated (parent or child)
        if (relationship.getChildStructureInode().equals(type.id())) {
            //parent is updated
            relationship.setParentRelationName(relationName);
            relationship.setParentRequired(field.required());


            //only one side of the relationship can be required
            if (relationship.getChildRelationName() != null) {
                //setting as not required the other side of the relationship
                relationship.setChildRequired(false);
                final FieldBuilder builder = FieldBuilder.builder(byContentTypeAndVar(relatedContentType, relationship.getChildRelationName()));
                if(field.required()){
                    builder.required(false);
                }

                //update cardinality in case it is changed
                fieldFactory.save(builder.values(field.values()).build());
            }
        } else {
            //child is updated
            relationship.setChildRelationName(relationName);
            relationship.setChildRequired(field.required());

            //only one side of the relationship can be required
            if (field.required() && relationship.getParentRelationName() != null) {
                //setting as not required the other side of the relationship
                relationship.setParentRequired(false);

                final FieldBuilder builder = FieldBuilder.builder(byContentTypeAndVar(relatedContentType, relationship.getParentRelationName()));
                if(field.required()){
                    builder.required(false);
                }

                //update cardinality in case it is changed
                fieldFactory.save(builder.values(field.values()).build());
            }
        }
        relationship.setCardinality(cardinality);
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
          removeRelationshipLink(field, type);
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
     * @throws DotDataException
     */
    private void removeRelationshipLink(Field field, ContentType type)
            throws DotDataException {

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
