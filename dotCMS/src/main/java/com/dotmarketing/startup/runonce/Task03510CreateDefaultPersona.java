package com.dotmarketing.startup.runonce;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.personas.business.PersonaAPI;
import com.dotmarketing.portlets.personas.business.PersonaFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.startup.AbstractJDBCStartupTask;

/**
 * Task to add the default persona content type
 * 
 * @author 
 * @version 1.0
 * @since 12-04-2015
 */
public class Task03510CreateDefaultPersona extends AbstractJDBCStartupTask {


	private final String INSERT_FIELD = "INSERT INTO field " +
		"(inode,structure_inode,field_name,field_type,field_relation_type,field_contentlet," +
		"required,indexed,listed,velocity_var_name,sort_order,field_values,regex_check," +
		"hint,default_value,fixed,read_only,searchable,unique_,mod_date) " +
		"VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";


    @Override
    public boolean forceRun() {
        return true;
    }


    /**
     * ALL of this logic and SQL is duped in the PersonaFactoryImpl
     * and could be called by:
     * APILocator.getPersonaAPI().createDefaultPersonaStructure();
     * But I did not want to "CALL an API in a startup task"
     */
    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {

		DotConnect dc = new DotConnect();

		dc.setSQL(PersonaFactory.SELECT_DEFAULT_STRUC_QUERY);
		dc.addParam(PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_INODE);
		if (dc.loadResults().size() > 0) {
			return;
		}

		dc.setSQL(PersonaFactory.INSERT_DEFAULT_STRUC_INODE_QUERY);
		dc.addParam(PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_INODE);
		dc.addParam(UserAPI.SYSTEM_USER_ID);
		dc.addParam(new Date());
		dc.addParam(new Structure().getType());
		dc.loadResult();

		/**
		 * inode name description structuretype system fixed velocity_var_name
		 * host folder mod_date
		 */
		dc = new DotConnect();
		dc.setSQL(PersonaFactory.INSERT_DEFAULT_STRUC_QUERY);
		dc.addParam(PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_INODE);
		dc.addParam(PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_NAME);
		dc.addParam(PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_DESCRIPTION);
		dc.addParam(Structure.STRUCTURE_TYPE_PERSONA);
		dc.addParam(false);
		dc.addParam(false);
		dc.addParam(PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_VARNAME);
		dc.addParam(Host.SYSTEM_HOST);
		dc.addParam(FolderAPI.SYSTEM_FOLDER);
		dc.addParam(new Date());
		dc.addParam(false);
		dc.loadResult();

		Structure proxy = new Structure();
		proxy.setInode(PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_INODE);
		List<Field> fields = getBasePersonaFields(proxy);
		int i = 0;
		String currentInode;
		for (Field f : fields) {
			currentInode = PersonaFactory.DEFAUTL_PERSONA_FIELD_INODES[i++];
			//Insert inode
			dc.setSQL(PersonaFactory.INSERT_DEFAULT_STRUC_INODE_QUERY);
			dc.addParam(currentInode);
			dc.addParam("");
			dc.addParam(new Date());
			dc.addParam(f.getType());
			dc.loadResult();


			//Insert field
			f.setInode(currentInode);
			dc.setSQL(INSERT_FIELD);
			dc.addParam(f.getInode());
			dc.addParam(f.getStructureInode());
			dc.addParam(f.getFieldName());
			dc.addParam(f.getFieldType());
			dc.addParam(f.getFieldRelationType());
			dc.addParam(f.getFieldContentlet());
			dc.addParam(f.isRequired());
			dc.addParam(f.isIndexed());
			dc.addParam(f.isListed());
			dc.addParam(f.getVelocityVarName());
			dc.addParam(f.getSortOrder());
			dc.addParam(f.getValues());
			dc.addParam(f.getRegexCheck());
			dc.addParam(f.getHint());
			dc.addParam(f.getDefaultValue());
			dc.addParam(f.isFixed());
			dc.addParam(f.isReadOnly());
			dc.addParam(f.isSearchable());
			dc.addParam(f.isUnique());
			dc.addParam(f.getModDate());
			dc.loadResult();
		}
    }

	private List<Field> getBasePersonaFields(Structure structure) {
		ArrayList fields = new ArrayList();
		Field field = null;
		byte i = 1;
		int var5 = i + 1;
		field = new Field("Site/Folder", Field.FieldType.HOST_OR_FOLDER, Field.DataType.TEXT, structure, true, false, true, i, "", "", "", true, false, true);
		field.setVelocityVarName("hostFolder");
		field.setFieldContentlet("system_field1");
		fields.add(field);
		field = new Field("Name", Field.FieldType.TEXT, Field.DataType.TEXT, structure, true, true, true, var5++, "", "", "", true, false, true);
		field.setVelocityVarName("name");
		field.setFieldContentlet("text1");
		fields.add(field);
		field = new Field("Key Tag", Field.FieldType.CUSTOM_FIELD, Field.DataType.TEXT, structure, true, true, true, var5++, "$velutil.mergeTemplate(\'/static/personas/keytag_custom_field.vtl\')", "", "[a-zA-Z0-9]+", true, false, true);
		field.setVelocityVarName("keyTag");
		field.setFieldContentlet("text2");
		fields.add(field);
		field = new Field("Photo", Field.FieldType.BINARY, Field.DataType.BINARY, structure, false, false, false, var5++, "", "", "", true, false, false);
		field.setVelocityVarName("photo");
		field.setFieldContentlet("binary1");
		fields.add(field);
		field = new Field("Other Tags", Field.FieldType.TAG, Field.DataType.LONG_TEXT, structure, false, true, true, var5++, "", "", "", true, false, true);
		field.setVelocityVarName("tags");
		field.setFieldContentlet("text_area1");
		field.setListed(false);
		fields.add(field);
		field = new Field("Description", Field.FieldType.TEXT_AREA, Field.DataType.LONG_TEXT, structure, false, false, false, var5++, "", "", "", true, false, true);
		field.setVelocityVarName("description");
		field.setFieldContentlet("text_area2");
		fields.add(field);
		return fields;
	}




	@Override
	public String getPostgresScript() {
		// TODO Auto-generated method stub
		return null;
	}




	@Override
	public String getMySQLScript() {
		// TODO Auto-generated method stub
		return null;
	}




	@Override
	public String getOracleScript() {
		// TODO Auto-generated method stub
		return null;
	}




	@Override
	public String getMSSQLScript() {
		// TODO Auto-generated method stub
		return null;
	}




	@Override
	public String getH2Script() {
		// TODO Auto-generated method stub
		return null;
	}




	@Override
	protected List<String> getTablesToDropConstraints() {
		// TODO Auto-generated method stub
		return null;
	}
    	
    	
    

}
