package com.dotcms.contenttype.business;

import java.util.List;

import com.dotcms.contenttype.model.field.*;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import com.liferay.util.StringUtil;


public class FieldApiImpl implements FieldApi {

	private List<Class> baseFieldTypes = ImmutableList.of(BinaryField.class,
			CategoryField.class, ConstantField.class, CheckboxField.class, CustomField.class, DateField.class, DateTimeField.class,
			FileField.class, HiddenField.class, HostFolderField.class, ImageField.class, KeyValueField.class, LineDividerField.class,
			MultiSelectField.class, PermissionTabField.class, RadioField.class, RelationshipsTabField.class, SelectField.class,
			TabDividerField.class, TagField.class, TextAreaField.class, TimeField.class, WysiwygField.class);

	FieldFactory fac = new FieldFactoryImpl();

	@Override
	public Field save(Field field, User user) throws DotDataException, DotSecurityException {
		ContentTypeApi tapi = APILocator.getContentTypeAPI2();
		ContentType type = tapi.find(field.contentTypeId(), user) ;
		APILocator.getPermissionAPI().checkPermission(type, PermissionLevel.PUBLISH, user);
		
		
		

		
		
		return fac.save(field);
	}
	
	@Override
	public void delete(Field field) throws DotDataException {
		 fac.delete(field);
	}


	@Override
	public List<Field> byContentTypeId(String typeId) throws DotDataException {
		 return fac.byContentTypeId(typeId);
	}
	
	
	@Override
	public Field find(String id) throws DotDataException {
		 return fac.byId(id);
	}
	@Override
	public Field byContentTypeAndVar(ContentType type,String fieldVar) throws DotDataException {
		 return fac.byContentTypeFieldVar(type,fieldVar);
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
}
