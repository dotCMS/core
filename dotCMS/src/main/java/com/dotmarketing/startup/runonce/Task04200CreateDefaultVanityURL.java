package com.dotmarketing.startup.runonce;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.AbstractJDBCStartupTask;

/**
 * This upgrade task will insert into the tables Inode and Structure the default Vanity URL Content Type.
 * This is needed to be able to migrate the old Vanity URL's (on the virtual_link table) to the new Vanity URL that are content.
 * 
 * @author erickgonzalez
 * @version 4.2.0
 * @since Jun 6, 2017
 *
 */
public class Task04200CreateDefaultVanityURL extends AbstractJDBCStartupTask {

	// TODO: If the Content Type is changed we need to check the values here are the same that the ones on the Starter
	private final String DEFAULT_VANITY_URL_STRUCTURE_INODE = "8e850645-bb92-4fda-a765-e67063a59be0";
	private final String DEFAULT_VANITY_URL_STRUCTURE_VARNAME = "Vanityurlasset";
	private final String DEFAULT_VANITY_URL_STRUCTURE_NAME = "Vanity URL Asset";
	private final String DEFAULT_VANITY_URL_STRUCTURE_DESCRIPTION = "Vanity URL as Content";
	private final String[] DEFAULT_VANITY_URL_FIELDS_INODES = { 
			"4e64a309-8c5e-48cf-b4a9-e724a5e09575", // Order
			"3b7f25bb-c5be-48bb-b00b-5b1b754e550c", // Action
			"de4fea7f-4d8f-48eb-8a63-20772dced99a", // Foward To
			"7e438b93-b631-4812-9c9c-331b03e6b1cd", // Uri
			"49f3803a-b2b0-4e03-bb2e-d1bb2a1c135e", // Site
			"9ae3e58a-75c9-4e4a-90f3-6f0d5f32a6f0"  // Title
	};

	private final String INSERT_INODE_QUERY = "insert into inode (inode, owner, idate, type) values (?, ?, ?, ?)";

	private final String INSERT_FIELD_QUERY = "insert into field "
			+ "(inode,structure_inode,field_name,field_type,field_relation_type,field_contentlet,"
			+ "required,indexed,listed,velocity_var_name,sort_order,field_values,regex_check,"
			+ "hint,default_value,fixed,read_only,searchable,unique_,mod_date) "
			+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

	private final String INSERT_STRUCTURE_QUERY = "insert into structure (inode, name, description, structuretype, system, fixed, velocity_var_name, host, "
			+ "folder, mod_date, default_structure) " + "values ( ?, ?, ?, ?, ?, ?, ?, ?, ? ,?,?)";

	@Override
	public void executeUpgrade() throws DotDataException, DotRuntimeException {
		DotConnect dc = new DotConnect();

		
		//Inserts into Inode Table the reference of the Vanity URL Content Type 
		dc.setSQL(INSERT_INODE_QUERY);
		dc.addParam(DEFAULT_VANITY_URL_STRUCTURE_INODE);
		dc.addParam("system");
		dc.addParam(new Date());
		dc.addParam("structure");
		dc.loadResult();

		//Inserts into Structure Table the Vanity URL Content Type
		dc = new DotConnect();
		dc.setSQL(INSERT_STRUCTURE_QUERY);
		dc.addParam(DEFAULT_VANITY_URL_STRUCTURE_INODE);
		dc.addParam(DEFAULT_VANITY_URL_STRUCTURE_NAME);
		dc.addParam(DEFAULT_VANITY_URL_STRUCTURE_DESCRIPTION);
		dc.addParam(7);
		dc.addParam(false);
		dc.addParam(false);
		dc.addParam(DEFAULT_VANITY_URL_STRUCTURE_VARNAME);
		dc.addParam("SYSTEM_HOST");
		dc.addParam("SYSTEM_FOLDER");
		dc.addParam(new Date());
		dc.addParam(false);
		dc.loadResult();
		
		//Gets the list of required fields for the VanityURL Content Type
		List<Map<String,Object>> requiredFields = setBaseVanityURLFields();
		
		int i = 0;
		for(Map<String,Object> field : requiredFields){
			
			//Insert into Inode Table the reference of the field
			dc.setSQL(INSERT_INODE_QUERY);
			dc.addParam(DEFAULT_VANITY_URL_FIELDS_INODES[i]);
			dc.addParam("");
			dc.addParam(new Date());
			dc.addParam("field");
			dc.loadResult();
			
			//Insert into Field Table
	        dc.setSQL(INSERT_FIELD_QUERY);
	        dc.addParam(DEFAULT_VANITY_URL_FIELDS_INODES[i]);
	        dc.addParam(field.get("structureInode"));
	        dc.addParam(field.get("fieldName"));
	        dc.addParam(field.get("fieldType"));
	        dc.addParam(field.get("fieldRelationType"));
	        dc.addParam(field.get("fieldContentlet"));
	        dc.addParam(field.get("isRequired"));
	        dc.addParam(field.get("isIndexed"));
	        dc.addParam(field.get("isListed"));
	        dc.addParam(field.get("velocityVarName"));
	        dc.addParam(field.get("sortOrder"));
	        dc.addParam(field.get("values"));
	        dc.addParam(field.get("regexCheck"));
	        dc.addParam(field.get("hint"));
	        dc.addParam(field.get("defaultValue"));
	        dc.addParam(field.get("isFixed"));
	        dc.addParam(field.get("isReadOnly"));
	        dc.addParam(field.get("isSearchable"));
	        dc.addParam(field.get("isUnique"));
	        dc.addParam(new Date());
	        dc.loadResult();
	        
	        i++;
		}
		
		
	}

	/**
	 * This method creates each field that is required for the Vanity URL Content Type and return a list with them.
	 * @return list of fields
	 */
	private List<Map<String, Object>> setBaseVanityURLFields() {
		List fields = new ArrayList();
		
		fields.add(setFieldElements("Order", "com.dotcms.contenttype.model.field.TextField","", "integer2", true, true, false, "order", 5, "", "", "", "", true, false, true));
		
		fields.add(setFieldElements("Action", "com.dotcms.contenttype.model.field.SelectField","", "integer1", true, true, false, "action", 4, 
				"Forward|200\r\nPermanent Redirect|301\r\nTemporary Redirect|307\r\nAuth Required|401\r\nAuth Failed|403\r\nMissing|404\r\nError|500", "", "", "", true, false, true));
		
		fields.add(setFieldElements("Forward To", "com.dotcms.contenttype.model.field.CustomField","", "text4", true, true, true, "forwardTo", 3, "$velutil.mergeTemplate('/static/content/file_browser_field_render.vtl')", "", "", "", true, false, false));
		
		fields.add(setFieldElements("Uri", "com.dotcms.contenttype.model.field.TextField","", "text3", true, true, true, "uri", 2, "", "", "", "", true, false, true));
		
		fields.add(setFieldElements("Site","com.dotcms.contenttype.model.field.CustomField","","text2", true, true, true,"site",1,"$velutil.mergeTemplate('/static/content/site_selector_field_render.vtl')","","","",true,false,false));
		
		fields.add(setFieldElements("Title", "com.dotcms.contenttype.model.field.TextField","", "text1", true, true, true, "title", 1, "", "", "", "", true, false, true));

		return fields;
	}
	
	/**
	 * This method sets the value of each element that is needed when creating a Field.
	 * 
	 * @param fieldName
	 * @param fieldType
	 * @param fieldRelationType
	 * @param fieldContentlet
	 * @param required
	 * @param listed
	 * @param indexed
	 * @param velocityVarName
	 * @param sortOrder
	 * @param values
	 * @param defaultValue
	 * @param checkRegex
	 * @param fixed
	 * @param readOnly
	 * @param searchable
	 * @return
	 */
	private Map<String, Object> setFieldElements(String fieldName, String fieldType, String fieldRelationType,
			String fieldContentlet, boolean required, boolean listed, boolean indexed, String velocityVarName,
			int sortOrder, String values, String checkRegex, String hint, String defaultValue,boolean fixed, boolean readOnly,
			boolean searchable) {

		Map<String, Object> elements = new HashMap<>();
		elements.put("structureInode", DEFAULT_VANITY_URL_STRUCTURE_INODE);
		elements.put("fieldName", fieldName);
		elements.put("fieldType", fieldType);
		elements.put("fieldRelationType", fieldRelationType);
		elements.put("fieldContentlet", fieldContentlet);
		elements.put("isRequired", required);
		elements.put("isIndexed", indexed);
		elements.put("isListed", listed);
		elements.put("velocityVarName", velocityVarName);
		elements.put("sortOrder", sortOrder);
		elements.put("values", values);
		elements.put("regexCheck", checkRegex);
		elements.put("hint", hint);
		elements.put("defaultValue", defaultValue);
		elements.put("isFixed", fixed);
		elements.put("isReadOnly", readOnly);
		elements.put("isSearchable", searchable);
		elements.put("isUnique", false);
		
		return elements;
	}

	@Override
	public boolean forceRun() {
		return true;
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
