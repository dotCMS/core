package com.dotmarketing.startup.runonce;

import com.dotcms.contenttype.transform.contenttype.StructureTransformer;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.VelocityUtil;

import java.util.List;

public class Task00768CreateTagStorageFieldOnHostStructure implements StartupTask {
    
    protected void upgradeStructureFields() throws DotDataException {
        try { 
            DotConnect dc=new DotConnect();
            if(DbConnectionFactory.isOracle()) {
                dc.executeStatement("alter table structure add expire_date_var varchar2(255)");
                dc.executeStatement("alter table structure add publish_date_var varchar2(255)");
            }
            else {
                dc.executeStatement("alter table structure add expire_date_var varchar(255)");
                dc.executeStatement("alter table structure add publish_date_var varchar(255)");
            }
        }
        catch(Exception ex) {
            throw new DotDataException(ex.getMessage(),ex);
        }
    }

	public void executeUpgrade() throws DotDataException, DotRuntimeException {
		
	    upgradeStructureFields();
	    
		Structure structure = StructureFactory.getStructureByVelocityVarName("Host");
		String structureInode = structure.getInode();
		
		Field field = new Field();
		field.setFieldName("Tag Storage");
        field.setStructureInode(structureInode);
        field.setFieldType(Field.FieldType.CUSTOM_FIELD.toString());
        field.setUnique(false);
        field.setFixed(true);
        field.setReadOnly(false);
        field.setIndexed(true);
        field.setReadOnly(false);
		
		
		List<Field> fields = FieldsCache.getFieldsByStructureInode(structure.getInode());
		int sortOrder = 0;
		boolean alreadyExists = false;
		
		for (Field f : fields) {
			
			if (f.getFieldType().equalsIgnoreCase(field.getFieldType())
					&& f.getFieldType().equalsIgnoreCase(Field.FieldType.HOST_OR_FOLDER.toString())) {
				Logger.debug(this, "The field already exist on structure.");
				alreadyExists = true;
				
			}
			if (f.getVelocityVarName().equals("hostThumbnail"))
				sortOrder = f.getSortOrder() - 1;
		}
		if(!alreadyExists){
		    field.setSortOrder(sortOrder);
		    
			String fieldVelocityName = VelocityUtil.convertToVelocityVariable(field.getFieldName(), false);
			int found = 0;
			if (VelocityUtil.isNotAllowedVelocityVariableName(fieldVelocityName)) {
				found++;
			}

			String velvar;
			for (Field f : fields) {
				velvar = f.getVelocityVarName();
				if (velvar != null) {
					if (fieldVelocityName.equals(velvar)) {
						found++;
					} else if (velvar.contains(fieldVelocityName)) {
						String number = velvar.substring(fieldVelocityName.length());
						if (RegEX.contains(number, "^[0-9]+$")) {
							found++;
						}
					}
				}
			}
			if (found > 0) {
				fieldVelocityName = fieldVelocityName + Integer.toString(found);
			}
			
			if(!validateInternalFieldVelocityVarName(fieldVelocityName)){
				fieldVelocityName+="1";
			}
			
			field.setVelocityVarName(fieldVelocityName);
		
			field.setValues("#parse('static/tag/tag_storage_field_creation.vtl')");
			
			String fieldContentlet = FieldFactory.getNextAvaliableFieldNumber(Field.DataType.TEXT.toString(), field.getInode(), field
					.getStructureInode());
			if (fieldContentlet == null) {
				// didn't find any empty ones, so im throwing an error
				// to the user to select a new one
			}
			field.setFieldContentlet(fieldContentlet);
			
			FieldFactory.saveField(field);

			FieldsCache.removeFields(structure);
			CacheLocator.getContentTypeCache().remove(structure);

			StructureFactory.saveStructure(structure);
			
			//Populate host contents with tag storage value = SYSTEM_HOST
			DotConnect dc = new DotConnect();
			dc.setSQL("update contentlet set "+fieldContentlet+" = ? " +
					"where live = ? and working = ? and structure_inode in (select inode from structure where name = 'Host')" 
					+ " and title not like ?") ;
			dc.addParam(Host.SYSTEM_HOST);
			if(DbConnectionFactory.isPostgres()){
				dc.addParam(true);
				dc.addParam(true);	
			}
			else {
				dc.addParam(DbConnectionFactory.getDBTrue());
				dc.addParam(DbConnectionFactory.getDBTrue());
			}

			dc.addParam("System Host");
			dc.loadResult();
			
		}
	
	}
	
	public boolean forceRun() {
		return true;
	}
	
private boolean validateInternalFieldVelocityVarName(String fieldVelVarName){
		
	    if(fieldVelVarName.equals(Contentlet.INODE_KEY)||
	    		fieldVelVarName.equals(Contentlet.LANGUAGEID_KEY)||
	    		fieldVelVarName.equals(Contentlet.STRUCTURE_INODE_KEY)||
	    		fieldVelVarName.equals(Contentlet.LAST_REVIEW_KEY)||
	    		fieldVelVarName.equals(Contentlet.NEXT_REVIEW_KEY)||
	    		fieldVelVarName.equals(Contentlet.REVIEW_INTERNAL_KEY)||
	    		fieldVelVarName.equals(Contentlet.DISABLED_WYSIWYG_KEY)||
	    		fieldVelVarName.equals(Contentlet.LOCKED_KEY)||
	    		fieldVelVarName.equals(Contentlet.ARCHIVED_KEY)||
	    		fieldVelVarName.equals(Contentlet.LIVE_KEY)||
	    		fieldVelVarName.equals(Contentlet.WORKING_KEY)||
	    		fieldVelVarName.equals(Contentlet.MOD_DATE_KEY)||
	    		fieldVelVarName.equals(Contentlet.MOD_USER_KEY)||
	    		fieldVelVarName.equals(Contentlet.OWNER_KEY)||
	    		fieldVelVarName.equals(Contentlet.IDENTIFIER_KEY)||
	    		fieldVelVarName.equals(Contentlet.SORT_ORDER_KEY)||
	    		fieldVelVarName.equals(Contentlet.HOST_KEY)||
	    		fieldVelVarName.equals(Contentlet.FOLDER_KEY)){
	    	return false;
	    }

	    return true;
		
	}



}
