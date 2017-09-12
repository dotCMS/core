package com.dotcms.contenttype.business;

import java.util.List;

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
import com.dotcms.contenttype.model.field.RelationshipsTabField;
import com.dotcms.contenttype.model.field.SelectField;
import com.dotcms.contenttype.model.field.TabDividerField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.field.TextAreaField;
import com.dotcms.contenttype.model.field.TimeField;
import com.dotcms.contenttype.model.field.WysiwygField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.quartz.job.DeleteFieldJobHelper;
import com.dotmarketing.services.ContentletMapServices;
import com.dotmarketing.services.ContentletServices;
import com.dotmarketing.services.StructureServices;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;


public class FieldAPIImpl implements FieldAPI {

  private List<Class> baseFieldTypes = ImmutableList.of(BinaryField.class, CategoryField.class,
      ConstantField.class, CheckboxField.class, CustomField.class, DateField.class,
      DateTimeField.class, FileField.class, HiddenField.class, HostFolderField.class,
      ImageField.class, KeyValueField.class, LineDividerField.class, MultiSelectField.class,
      PermissionTabField.class, RadioField.class, RelationshipsTabField.class, SelectField.class,
      TabDividerField.class, TagField.class, TextAreaField.class, TimeField.class,
      WysiwygField.class);

  private final PermissionAPI perAPI;
  private final ContentletAPI conAPI;
  private final UserAPI userAPI;

  private FieldFactory fac = new FieldFactoryImpl();

  public FieldAPIImpl() {
      this(APILocator.getPermissionAPI(),
          APILocator.getContentletAPI(),
          APILocator.getUserAPI(),
          DeleteFieldJobHelper.INSTANCE);
  }

  @VisibleForTesting
  public FieldAPIImpl(final PermissionAPI perAPI,
                        final ContentletAPI conAPI,
                        final UserAPI userAPI,
                        final DeleteFieldJobHelper deleteFieldJobHelper) {
      this.perAPI = perAPI;
      this.conAPI = conAPI;
      this.userAPI = userAPI;
  }

  @Override
	public Field save(Field field, User user) throws DotDataException, DotSecurityException {
		ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);
		ContentType type = contentTypeAPI.find(field.contentTypeId()) ;
		perAPI.checkPermission(type, PermissionLevel.PUBLISH, user);

	    Field oldField = null;
	    if (UtilMethods.isSet(field.id())) {
	    	try {
	    		oldField = fac.byId(field.id());

	    		if (oldField.sortOrder() != field.sortOrder()){
	    		    if (oldField.sortOrder() > field.sortOrder()) {
                        fac.moveSortOrderForward(field.sortOrder(), oldField.sortOrder());
                    } else {
                        fac.moveSortOrderBackward(oldField.sortOrder(), field.sortOrder());
                    }
                }
	    	} catch(NotFoundInDbException e) {
	    		//Do nothing as Starter comes with id but field is unexisting yet
	    	}
	    }else {
            fac.moveSortOrderForward(field.sortOrder());
        }

		Field result = fac.save(field);
		//update Content Type mod_date to detect the changes done on the field
		contentTypeAPI.updateModDate(type);
		
		Structure structure = new StructureTransformer(type).asStructure();

        CacheLocator.getContentTypeCache().remove(structure);
        StructureServices.removeStructureFile(structure);

        
        if(oldField!=null){
	        if(oldField.indexed() != field.indexed()){
	          conAPI.refresh(structure);
	        } else if (field instanceof ConstantField) {
	        	if(!StringUtils.equals(oldField.values(), field.values()) ){
		            ContentletServices.removeContentletFile(structure);
		            ContentletMapServices.removeContentletMapFile(structure);
		            conAPI.refresh(structure);
	        	}
	        }
        }

        return result;
	}
  
  @Override
  public FieldVariable save(FieldVariable var, User user) throws DotDataException, DotSecurityException {
      ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);
      Field field = fac.byId(var.fieldId());

      ContentType type = contentTypeAPI.find(field.contentTypeId()) ;
      APILocator.getPermissionAPI().checkPermission(type, PermissionLevel.PUBLISH, user);

      FieldVariable newFieldVariable = fac.save(ImmutableFieldVariable.builder().from(var).userId(user.getUserId()).build());
      
      //update Content Type mod_date to detect the changes done on the field variables
      contentTypeAPI.updateModDate(type);
      
      return newFieldVariable;
  }

  @Override
  public void delete(Field field) throws DotDataException {
	  try {
		  this.delete(field, this.userAPI.getSystemUser());
	  } catch (DotSecurityException e){
		  throw new DotDataException(e);
	  }
  }

  @Override
  public void delete(Field field, User user) throws DotDataException, DotSecurityException {
	  LocalTransaction.wrapReturn(() -> {

		  ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);
		  ContentType type = contentTypeAPI.find(field.contentTypeId());

		  perAPI.checkPermission(type, PermissionLevel.PUBLISH, user);

		  Field oldField = fac.byId(field.id());
		  if(oldField.fixed() || oldField.readOnly()){
		    throw new DotDataException("You cannot delete a fixed or read only fiedl");
		  }

		  Structure structure = new StructureTransformer(type).asStructure();
		  com.dotmarketing.portlets.structure.model.Field legacyField = new LegacyFieldTransformer(field).asOldField();


	      if (!(field instanceof CategoryField) &&
	              !(field instanceof ConstantField) &&
	              !(field instanceof HiddenField) &&
	        	  !(field instanceof LineDividerField) &&
	        	  !(field instanceof TabDividerField) &&
	        	  !(field instanceof RelationshipsTabField) &&
	        	  !(field instanceof PermissionTabField) &&
	        	  !(field instanceof HostFolderField) &&
	        	  structure != null
	      ) {
	    	  this.conAPI.cleanField(structure, legacyField, this.userAPI.getSystemUser(), false);    	  
	      }

	      fac.moveSortOrderBackward(oldField.sortOrder());
	      fac.delete(field);
	      //update Content Type mod_date to detect the changes done on the field
	      contentTypeAPI.updateModDate(type);

	      CacheLocator.getContentTypeCache().remove(structure);
	      StructureServices.removeStructureFile(structure);

	      //Refreshing permissions
	      if (field instanceof HostFolderField) {
	    	  try {
	    		  this.conAPI.cleanHostField(structure, this.userAPI.getSystemUser(), false);
	    	  } catch(DotMappingException e) {}

	    	  this.perAPI.resetChildrenPermissionReferences(structure);
	      }

	      // rebuild contentlets indexes
	      if(field.indexed()){
	        conAPI.reindex(structure);
	      }
	      // remove the file from the cache
	      ContentletServices.removeContentletFile(structure);
	      ContentletMapServices.removeContentletMapFile(structure);

	      return null;
	  });
  }


  @Override
  public List<Field> byContentTypeId(String typeId) throws DotDataException {
    return fac.byContentTypeId(typeId);
  }
  @Override
  public String nextAvailableColumn(Field field) throws DotDataException{
      return fac.nextAvailableColumn(field);
  }
  @Override
  public Field find(String id) throws DotDataException {
    return fac.byId(id);
  }

  @Override
  public Field byContentTypeAndVar(ContentType type, String fieldVar) throws DotDataException {
    return fac.byContentTypeFieldVar(type, fieldVar);
  }

  @Override
  public Field byContentTypeIdAndVar(String id, String fieldVar) throws DotDataException {
    try {
        return byContentTypeAndVar(APILocator.getContentTypeAPI(APILocator.systemUser()).find(id), fieldVar);
    } catch (DotSecurityException e) {
        throw new DotDataException(e);
    }
  }
  
  @Override
  public void deleteFieldsByContentType(ContentType type) throws DotDataException {
    fac.deleteByContentType(type);
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

@Override
public void delete(FieldVariable fieldVar) throws DotDataException {
    fac.delete(fieldVar);
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
