package com.dotmarketing.portlets.widget.business;

import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.business.FieldAPI;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.InodeUtils;
import com.liferay.portal.model.User;

public class WidgetAPIImpl implements WidgetAPI {
	
	public PermissionAPI perAPI = APILocator.getPermissionAPI();

	public void createBaseWidgetFields(Structure structure)	throws DotDataException,DotStateException {
		if(!InodeUtils.isSet(structure.getInode())){
			throw new DotStateException("Cannot create base widget feilds on a structure that doesn't exist");
		}
		Field preExecute = new Field(WIDGET_PRE_EXECUTE_FIELD_NAME,Field.FieldType.HIDDEN,Field.DataType.TEXT,structure,false,false,false,4,"", "", "", true, true, true);
		preExecute.setFieldContentlet(FieldAPI.ELEMENT_CONSTANT);
		Field codeField = new Field(WIDGET_CODE_FIELD_NAME,Field.FieldType.HIDDEN,Field.DataType.TEXT,structure,false,false,false,3,"", "", "", true, true, true);
		codeField.setFieldContentlet(FieldAPI.ELEMENT_CONSTANT);
		Field usageField = new Field(WIDGET_USAGE_FIELD_NAME,Field.FieldType.TEXT_AREA,Field.DataType.TEXT,structure,false,false,false,2,"", "", "", true, true, true);
		usageField.setFieldContentlet(FieldAPI.ELEMENT_CONSTANT);
		Field titleField = new Field(WIDGET_TITLE_FIELD_NAME,Field.FieldType.TEXT,Field.DataType.TEXT,structure,true,true,true,1,"", "", "", true, false, true);
		FieldFactory.saveField(preExecute);
		FieldFactory.saveField(codeField);
		FieldFactory.saveField(usageField);
		FieldFactory.saveField(titleField);
		FieldsCache.clearCache();
	}

	public List<Structure> findAll(User user, boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {
		List<Structure> sts = StructureFactory.getStructures();
		List<Structure> wids = new ArrayList<Structure>();
		for (Structure structure : sts) {
			if(structure.getStructureType() == Structure.STRUCTURE_TYPE_WIDGET){
				wids.add(structure);
			}
		}
		wids = perAPI.filterCollection(wids, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
		return wids;
	}
}
