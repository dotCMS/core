package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This upgrade task will insert into the tables Inode and Structure the default Vanity URL Content
 * Type. This is needed to be able to migrate the old Vanity URL's (on the virtual_link table) to
 * the new Vanity URL that are content.
 *
 * @author erickgonzalez
 * @version 4.2.0
 * @since Jun 6, 2017
 */
public class Task04200CreateDefaultVanityURL extends AbstractJDBCStartupTask {

	/* If the Content Type is changed we need to check the values here are the same that the ones on the Starter */
	private static final String DEFAULT_VANITY_URL_STRUCTURE_INODE = "8e850645-bb92-4fda-a765-e67063a59be0";
	private static final String DEFAULT_VANITY_URL_STRUCTURE_VARNAME = "Vanityurl";
	private static final String DEFAULT_VANITY_URL_STRUCTURE_NAME = "Vanity URL";
	private static final String DEFAULT_VANITY_URL_STRUCTURE_DESCRIPTION = "Default Content Type for Vanity URLs";
	private static final String[] DEFAULT_VANITY_URL_FIELDS_INODES = {
			"4e64a309-8c5e-48cf-b4a9-e724a5e09575", // Order
			"3b7f25bb-c5be-48bb-b00b-5b1b754e550c", // Action
			"de4fea7f-4d8f-48eb-8a63-20772dced99a", // Foward To
			"7e438b93-b631-4812-9c9c-331b03e6b1cd", // Uri
			"49f3803a-b2b0-4e03-bb2e-d1bb2a1c135e", // Site
			"9ae3e58a-75c9-4e4a-90f3-6f0d5f32a6f0"  // Title
	};

	private static final String INSERT_INODE_QUERY = "insert into inode (inode, owner, idate, type) values (?, ?, ?, ?)";

	private static final String INSERT_FIELD_QUERY = "insert into field "
			+ "(inode,structure_inode,field_name,field_type,field_relation_type,field_contentlet,"
			+ "required,indexed,listed,velocity_var_name,sort_order,field_values,regex_check,"
			+ "hint,default_value,fixed,read_only,searchable,unique_,mod_date) "
			+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

	private static final String INSERT_STRUCTURE_QUERY =
			"insert into structure (inode, name, description, structuretype, system, fixed, velocity_var_name, host, "
					+ "folder, mod_date, default_structure) "
					+ "values ( ?, ?, ?, ?, ?, ?, ?, ?, ? ,?,?)";

	private static final int INT_7 = 7;
	private static final int INT_6 = 6;
	private static final int INT_5 = 5;
	private static final int INT_4 = 4;
	private static final int INT_3 = 3;
	private static final int INT_2 = 2;
	private static final int INT_1 = 1;
	private static final String TEXT_FIELD = "com.dotcms.contenttype.model.field.TextField";
	private static final String CUSTOM_FIELD = "com.dotcms.contenttype.model.field.CustomField";
	private static final String SELECT_FIELD = "com.dotcms.contenttype.model.field.SelectField";

	@Override
	public void executeUpgrade() throws DotDataException {
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
		dc.addParam(INT_7);
		dc.addParam(false);
		dc.addParam(true);
		dc.addParam(DEFAULT_VANITY_URL_STRUCTURE_VARNAME);
		dc.addParam("SYSTEM_HOST");
		dc.addParam("SYSTEM_FOLDER");
		dc.addParam(new Date());
		dc.addParam(false);
		dc.loadResult();

		//Gets the list of required fields for the VanityURL Content Type
		List<Map<String, Object>> requiredFields = setBaseVanityURLFields();

		int i = 0;
		for (Map<String, Object> field : requiredFields) {

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
	 * This method creates each field that is required for the Vanity URL Content Type and return a
	 * list with them.
	 *
	 * @return list of fields
	 */
	private List<Map<String, Object>> setBaseVanityURLFields() {
		List fields = new ArrayList();

		fields.add(setFieldElements("Title", TEXT_FIELD, "",
				"text1", true, true, true, "title", INT_1, "", "", "", "", true, false, true));

		fields.add(setFieldElements("Site", CUSTOM_FIELD, "",
				"text2", true, false, true, "site", INT_2,
				"$velutil.mergeTemplate('/static/content/site_selector_field_render.vtl')", "", "",
				"", true, false, false));

		fields.add(
				setFieldElements("Uri", TEXT_FIELD, "", "text3",
						true, true, true, "uri", INT_3, "", "", "", "", true, false, true));

		fields.add(setFieldElements("Action", SELECT_FIELD, "",
				"integer1", true, true, true, "action", INT_4,
				"200 - Forward|200\r\n301 - Permanent Redirect|301\r\n302 - Temporary Redirect|302",
				"", "", "", true, false, true));

		fields.add(
				setFieldElements("Forward To", CUSTOM_FIELD, "",
						"text4", true, true, true, "forwardTo", INT_5,
						"$velutil.mergeTemplate('/static/content/file_browser_field_render.vtl')",
						"", "", "", true, false, false));

		fields.add(setFieldElements("Order", TEXT_FIELD, "",
				"integer2", true, false, true, "order", INT_6, "", "", "", "0", true, false, true));

		return fields;
	}

	/**
	 * This method sets the value of each element that is needed when creating a Field.
	 */
	private Map<String, Object> setFieldElements(String fieldName, String fieldType,
												 String fieldRelationType,
												 String fieldContentlet, boolean required, boolean listed, boolean indexed,
												 String velocityVarName,
												 int sortOrder, String values, String checkRegex, String hint, String defaultValue,
												 boolean fixed, boolean readOnly,
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
		return null;
	}

	@Override
	public String getMySQLScript() {
		return null;
	}

	@Override
	public String getOracleScript() {
		return null;
	}

	@Override
	public String getMSSQLScript() {
		return null;
	}

	@Override
	protected List<String> getTablesToDropConstraints() {
		return new ArrayList<>();
	}

}