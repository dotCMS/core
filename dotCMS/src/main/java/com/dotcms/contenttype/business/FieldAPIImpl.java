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
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.quartz.job.DeleteFieldJobHelper;
import com.dotmarketing.services.ContentletMapServices;
import com.dotmarketing.services.ContentletServices;
import com.dotmarketing.services.StructureServices;
import com.dotmarketing.util.Logger;
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
		ContentTypeAPI tapi = APILocator.getContentTypeAPI(user);
		ContentType type = tapi.find(field.contentTypeId()) ;
		perAPI.checkPermission(type, PermissionLevel.PUBLISH, user);

	    Field oldField = null;
	    if (UtilMethods.isSet(field.id())) {
	    	try {
	    		oldField = fac.byId(field.id());
	    	} catch(NotFoundInDbException e) {
	    		//Do nothing as Starter comes with id but field is unexisting yet
	    	}
	    }

		Field result = fac.save(field);


		Structure structure = new StructureTransformer(type).asStructure();

        CacheLocator.getContentTypeCache().remove(structure);
        StructureServices.removeStructureFile(structure);

        //Refreshing permissions
        if(oldField != null && field instanceof HostFolderField){
        	perAPI.resetChildrenPermissionReferences(structure);
        }

        //http://jira.dotmarketing.net/browse/DOTCMS-5178
        if(oldField != null && ((!oldField.indexed() && field.indexed()) || (oldField.indexed() && !field.indexed()))){
          // rebuild contentlets indexes
          conAPI.reindex(structure);
        }

        if (field instanceof ConstantField) {
            ContentletServices.removeContentletFile(structure);
            ContentletMapServices.removeContentletMapFile(structure);
            conAPI.refresh(structure);
        }

        return result;
	}
  
  @Override
  public FieldVariable save(FieldVariable var, User user) throws DotDataException, DotSecurityException {
      ContentTypeAPI tapi = APILocator.getContentTypeAPI(user);
      Field field = fac.byId(var.fieldId());

      ContentType type = tapi.find(field.contentTypeId()) ;
      APILocator.getPermissionAPI().checkPermission(type, PermissionLevel.PUBLISH, user);
      

      return fac.save(var);
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
	  boolean local = false;
	  try{
		  try {

			  local = HibernateUtil.startLocalTransactionIfNeeded();

		  } catch (DotDataException e1) {
			  Logger.error(FieldAPIImpl.class, e1.getMessage(), e1);

			  throw new DotHibernateException("Unable to start a local transaction " + e1.getMessage(), e1);
		  }


		  ContentTypeAPI tapi = APILocator.getContentTypeAPI(user);
		  ContentType type = tapi.find(field.contentTypeId());

		  perAPI.checkPermission(type, PermissionLevel.PUBLISH, user);


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

	      fac.delete(field);


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
	      conAPI.reindex(structure);

	      // remove the file from the cache
	      ContentletServices.removeContentletFile(structure);
	      ContentletMapServices.removeContentletMapFile(structure);

	  } catch(DotHibernateException e){
		  if(local){
			  HibernateUtil.rollbackTransaction();
		  }
		  throw new DotDataException(e);
	  } finally {
		  if(local){
			  HibernateUtil.commitTransaction();
		  }
	  }
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
    return fac.byContentTypeIdFieldVar(id, fieldVar);
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
    
}

@Override
public List<FieldVariable> loadVariables(Field field) throws DotDataException {
    return fac.loadVariables(field);
}

@Override
public FieldVariable loadVariable(String id) throws DotDataException {
    return fac.loadVariable(id);
}
  
  
  
  
  
  
}
