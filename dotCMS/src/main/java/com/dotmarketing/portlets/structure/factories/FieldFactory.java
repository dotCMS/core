package com.dotmarketing.portlets.structure.factories;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.FieldVariable;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;



public class FieldFactory {

	//### READ ###
	public static Field getFieldByInode(String inode)
	{
		return (Field) InodeFactory.getInode(inode,Field.class);
	}

	@SuppressWarnings("unchecked")
	public static List<Field> getFieldsByStructure(String structureInode)
	{
		String condition = "structure_inode = '" + structureInode + "'";
		String order = "sort_order asc, field_name asc";
		return InodeFactory.getInodesOfClassByConditionAndOrderBy(Field.class,condition,order);
	}

	@SuppressWarnings("unchecked")
	public static List<Field> getFieldsByStructureSortedBySortOrder(String structureInode)
	{
		String condition = "structure_inode = '" + structureInode + "'";
		String order = "sort_order asc";
		return InodeFactory.getInodesOfClassByConditionAndOrderBy(Field.class,condition,order);
	}

	public static boolean isTagField(String fieldLuceneName, Structure st)
	{

		List<Field> targetFields = FieldsCache.getFieldsByStructureInode(st.getInode());
		boolean istag=false;


		   for( Field f : targetFields ) {
	        	if (f.getFieldContentlet().equals(fieldLuceneName)&& f.getFieldType().equals(Field.FieldType.TAG.toString()))
	        {
	        		istag=true;
	        }
		   }
	        	return istag;

		}


	public static List getFieldsByContentletField(String dataType, String fieldInode, String structureInode) {
		StringBuilder condition = new StringBuilder("structure_inode = '" + structureInode + "' and field_contentlet like '" + dataType + "%'");

		if (UtilMethods.isSet(fieldInode)) {
			condition.append(" and inode <> '" + fieldInode + "'");
		}

		if (dataType.equals("text")) {
			condition.append(" and field_contentlet not like '" + dataType + "_area%'");
		}

		String order = "field_contentlet";
		return InodeFactory.getInodesOfClassByConditionAndOrderBy(Field.class, condition.toString(), order);
	}

	public static Field getFieldByVariableName(String structureInode, String velocityVarName)
	{
		String condition = "structure_inode = '" + structureInode + "' and velocity_var_name = '" + velocityVarName + "'";
		return (Field)InodeFactory.getInodeOfClassByCondition(Field.class,condition);
	}

    public static Field getFieldByStructure(String structureInode, String fieldName)
    {
        String condition = "structure_inode = '" + structureInode + "' and field_name = '" + fieldName + "'";
        return (Field) InodeFactory.getInodeOfClassByCondition(Field.class,condition);
    }

    public static Field getFieldByName(String structureType, String fieldName)
    {
        Structure st = StructureFactory.getStructureByType(structureType);
        String condition = "structure_inode = '" + st.getInode() + "' and field_name = '" + fieldName + "'";
        return (Field) InodeFactory.getInodeOfClassByCondition(Field.class,condition);
    }

	//### CREATE AND UPDATE ###
	public static void saveField(Field field) throws DotHibernateException
	{
		field.setModDate(new Date());
		HibernateUtil.saveOrUpdate(field);
		FieldsCache.removeFieldVariables(field);
	}

	public static void saveField(Field field, String existingId) throws DotHibernateException
	{
		field.setModDate(new Date());
		HibernateUtil.saveWithPrimaryKey(field, existingId);
	}

	//### DELETE ###
	public static void deleteField(String inode) throws DotHibernateException
	{
		Field field = getFieldByInode(inode);
		deleteField(field);
	}

	public static void deleteField(Field field) throws DotHibernateException
	{
		InodeFactory.deleteInode(field);
		FieldsCache.removeField(field);

		List<FieldVariable> fieldVars = getFieldVariablesForField(field);
		for (FieldVariable var : fieldVars) {
			deleteFieldVariable(var);
		}

	}

	public static String getNextAvaliableFieldNumber (String dataType, String currentFieldInode, String structureInode) {
		List fields = FieldFactory.getFieldsByContentletField(dataType,currentFieldInode,structureInode);
		if (fields.size()>0) {

			//check if there is at least one empty
			if (fields.size()<25) {

				int lastDataTypeIdx = 0;
				for (int i=1;i<=25;i++) {
					boolean found = false;
					Iterator fieldsIter = fields.iterator();
					while (fieldsIter.hasNext() && !found) {
						Field nextField = (Field) fieldsIter.next();
						try {
							lastDataTypeIdx =Integer.parseInt(nextField.getFieldContentlet().replace(dataType,""));
							if (i == lastDataTypeIdx) {
								//found this field, break and try to find the next one
								found = true;
							}
						}
						catch (NumberFormatException e){

						}
					}
					if (!found) {
						lastDataTypeIdx = i;
						break;
					}
				}
				//found an empty field
				//set the contentlet field to be the data type plus the last one used
				return dataType + lastDataTypeIdx;
			}
			else {
				return null;
			}
		}
		else {
			//set the contentlet field to be the data type plus the last one used
			return dataType + "1";
		}
	}

	public static void saveFieldVariable(FieldVariable fieldVar){

		String id = fieldVar.getId();
		Field proxy = new Field();
		proxy.setInode(fieldVar.getFieldId());


		if(InodeUtils.isSet(id)) {
			try {
				HibernateUtil.update(fieldVar);
			} catch (DotHibernateException e) {
				Logger.error(FieldFactory.class, e.getMessage());
			}
		}else{
			try {
				HibernateUtil.save(fieldVar);
			} catch (DotHibernateException e) {
				Logger.error(FieldFactory.class, e.getMessage());
			}
		}
		FieldsCache.removeField(proxy);
		FieldsCache.removeFieldVariables(proxy);

		Field f = getFieldByInode(fieldVar.getFieldId());
		f.setModDate(new Date());
		try {
			saveField(f);
		} catch (DotHibernateException e) {
			Logger.error(FieldFactory.class, e.getMessage());
		}


	}

	public static FieldVariable getFieldVariable(String id){

		FieldVariable fVar = new FieldVariable();
		 try {
			 fVar = (FieldVariable) HibernateUtil.load(FieldVariable.class, id);
		} catch (DotHibernateException e) {
			Logger.error(FieldFactory.class, e.getMessage());
		}
		return fVar;
	}

	public static void deleteFieldVariable(String id){
		FieldVariable fieldVar = getFieldVariable(id);
		deleteFieldVariable(fieldVar);
	}

	public static void deleteFieldVariable(FieldVariable fieldVar){
		try {
			HibernateUtil.delete(fieldVar);
		} catch (DotHibernateException e) {
			Logger.error(FieldFactory.class, e.getMessage());
		}

	}

	public static List <FieldVariable> getAllFieldVariables(){

		List <FieldVariable> result = new ArrayList<FieldVariable>();
		HibernateUtil hu = new HibernateUtil(FieldVariable.class);
		try {
			hu.setQuery("from " + FieldVariable.class.getName());
			result = hu.list();
		} catch (DotHibernateException e) {
			Logger.error(FieldFactory.class, e.getMessage());
		}
		return result;
	}

	public static List<FieldVariable> getFieldVariablesForField (String fieldId ){

		Field proxy = new Field();
		proxy.setInode(fieldId);
		return getFieldVariablesForField(proxy);
	}

	public static List<FieldVariable> getFieldVariablesForField (Field field ){

		if(field == null || field.getInode() ==null){
			return new ArrayList<FieldVariable>();
		}
		List<FieldVariable> result = null;

		result = FieldsCache.getFieldVariables(field);
		if(result ==null || result.isEmpty()){
			HibernateUtil dh = new HibernateUtil(FieldVariable.class);

			try {
				dh.setQuery("from " + FieldVariable.class.getName()
						+ " fvar where fvar.fieldId = ?");

				dh.setParam(field.getInode());
				result = dh.list();
			} catch (DotHibernateException e) {
				Logger.error(FieldFactory.class, e.getMessage());
			}

			FieldsCache.addFieldVariables(field, result);
		}
		return result;
	}

}
