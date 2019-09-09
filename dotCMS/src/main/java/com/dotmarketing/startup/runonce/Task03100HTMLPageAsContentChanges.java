package com.dotmarketing.startup.runonce;

import java.util.List;
import java.util.Map;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

public class Task03100HTMLPageAsContentChanges implements StartupTask {

	private final String STRUCTURE_INODE = "c541abb1-69b3-4bc5-8430-5e09e5239cc8";
	
	// MYSQL
	private final String MYSQL_STRUCTURE_EXISTS = "SELECT inode FROM structure WHERE inode='c541abb1-69b3-4bc5-8430-5e09e5239cc8';";
	
	private final String MYSQL_INSERT_STRUCTURE_INODE = "INSERT INTO inode" +
										  		  		"(`inode`, `owner`, `idate`, `type`) VALUES " +
										  		  		"(?, 'system', NOW(), 'structure');";
	private final String MYSQL_INSERT_STRUCTURE = "INSERT INTO structure"+
												  "(`inode`,`name`,`description`,`default_structure`,`structuretype`,`system`," +
												  "`fixed`,`velocity_var_name`,`host`,`folder`,`mod_date`)VALUES" +
												  "(?,'Page Asset','Default Structure for Pages',0,5,0,1,'htmlpageasset','SYSTEM_HOST'," +
												  "'SYSTEM_FOLDER',NOW());";
	private final String MYSQL_INSERT_FIELD_INODE = "INSERT INTO inode(`inode`, `owner`, `idate`, `type`) VALUES(?,'', NOW(), 'field');";
	private final String MYSQL_INSERT_FIELD = "INSERT INTO field " +
											  "(`inode`,`structure_inode`,`field_name`,`field_type`,`field_relation_type`,`field_contentlet`," +
											  "`required`,`indexed`,`listed`,`velocity_var_name`,`sort_order`,`field_values`,`regex_check`," +
											  "`hint`,`default_value`,`fixed`,`read_only`,`searchable`,`unique_`,`mod_date`) " +
											  "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW());";
	// POSTGRESQL
	private final String POSTGRESQL_STRUCTURE_EXISTS = "SELECT inode FROM structure WHERE inode='c541abb1-69b3-4bc5-8430-5e09e5239cc8';";
	private final String POSTGRESQL_INSERT_STRUCTURE_INODE = "INSERT INTO inode" +
		  													"(inode, owner, idate, type) VALUES " +
		  													"(?, 'system', NOW(), 'structure');";
	private final String POSTGRESQL_INSERT_STRUCTURE = "INSERT INTO structure"+
													 "(inode,name,description,default_structure,structuretype,system," +
													 "fixed,velocity_var_name,host,folder,mod_date)VALUES" +
													 "(?,'Page Asset','Default Structure for Pages',false,5,false,true,'htmlpageasset','SYSTEM_HOST'," +
													 "'SYSTEM_FOLDER',NOW());";
	private final String POSTGRESQL_INSERT_FIELD_INODE = "INSERT INTO inode(inode, owner, idate, type) VALUES(?,'', NOW(), 'field');";
	private final String POSTGRESQL_INSERT_FIELD = "INSERT INTO field " +
												 "(inode,structure_inode,field_name,field_type,field_relation_type,field_contentlet," +
												 "required,indexed,listed,velocity_var_name,sort_order,field_values,regex_check," +
												 "hint,default_value,fixed,read_only,searchable,unique_,mod_date) " +
												 "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW());";
	
	// H2
	private final String H2_STRUCTURE_EXISTS = "SELECT inode FROM structure WHERE inode='c541abb1-69b3-4bc5-8430-5e09e5239cc8';";
	private final String H2_INSERT_STRUCTURE_INODE = "INSERT INTO inode" +
		  											 "(inode, owner, idate, type) VALUES " +
		  											 "(?, 'system', CURRENT_TIME(), 'structure');";
	private final String H2_INSERT_STRUCTURE = "INSERT INTO structure"+
											   "(inode,name,description,default_structure,structuretype,system," +
											   "fixed,velocity_var_name,host,folder,mod_date)VALUES" +
											   "(?,'Page Asset','Default Structure for Pages',false,5,false,true,'htmlpageasset','SYSTEM_HOST'," +
											   "'SYSTEM_FOLDER',CURRENT_TIME());";
	private final String H2_INSERT_FIELD_INODE = "INSERT INTO inode(inode, owner, idate, type) VALUES(?,'', CURRENT_TIME(), 'field');";
	private final String H2_INSERT_FIELD = "INSERT INTO field " +
											"(inode,structure_inode,field_name,field_type,field_relation_type,field_contentlet," +
											"required,indexed,listed,velocity_var_name,sort_order,field_values,regex_check," +
											"hint,default_value,fixed,read_only,searchable,unique_,mod_date) " +
											"VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,CURRENT_TIME());";
	
	// ORACLE
	private final String ORACLE_STRUCTURE_EXISTS = "SELECT inode FROM structure WHERE inode='c541abb1-69b3-4bc5-8430-5e09e5239cc8'";
	private final String ORACLE_INSERT_STRUCTURE_INODE = "INSERT INTO inode" +
		  											     "(inode, owner, idate, type) VALUES " +
		  											     "(?, 'system', CURRENT_DATE, 'structure')";
	private final String ORACLE_INSERT_STRUCTURE = "INSERT INTO structure"+
											       "(inode,name,description,default_structure,structuretype,system," +
											       "fixed,velocity_var_name,host,folder,mod_date)VALUES" +
											       "(?,'Page Asset','Default Structure for Pages',0,5,0,1,'htmlpageasset','SYSTEM_HOST'," +
											       "'SYSTEM_FOLDER',CURRENT_DATE)";
	private final String ORACLE_INSERT_FIELD_INODE = "INSERT INTO inode(inode, owner, idate, type) VALUES(?,'', CURRENT_DATE, 'field')";
	private final String ORACLE_INSERT_FIELD = "INSERT INTO field " +
											   "(inode,structure_inode,field_name,field_type,field_relation_type,field_contentlet," +
											   "required,indexed,listed,velocity_var_name,sort_order,field_values,regex_check," +
											   "hint,default_value,fixed,read_only,searchable,unique_,mod_date) " +
											   "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,CURRENT_DATE)";
	
	// MSSQL
	private final String MSSQL_STRUCTURE_EXISTS = "SELECT inode FROM structure WHERE inode='c541abb1-69b3-4bc5-8430-5e09e5239cc8'";
	private final String MSSQL_INSERT_STRUCTURE_INODE = "INSERT INTO inode" +
		  											    "(inode, owner, idate, type) VALUES " +
		  											    "(?, 'system', GETDATE(), 'structure')";
	private final String MSSQL_INSERT_STRUCTURE = "INSERT INTO structure"+
											      "(inode,name,description,default_structure,structuretype,system," +
											      "fixed,velocity_var_name,host,folder,mod_date)VALUES" +
											      "(?,'Page Asset','Default Structure for Pages',0,5,0,1,'htmlpageasset','SYSTEM_HOST'," +
											      "'SYSTEM_FOLDER',GETDATE())";
	private final String MSSQL_INSERT_FIELD_INODE = "INSERT INTO inode(inode, owner, idate, type) VALUES(?,'', GETDATE(), 'field')";
	private final String MSSQL_INSERT_FIELD = "INSERT INTO field " +
											  "(inode,structure_inode,field_name,field_type,field_relation_type,field_contentlet," +
											  "required,indexed,listed,velocity_var_name,sort_order,field_values,regex_check," +
											  "hint,default_value,fixed,read_only,searchable,unique_,mod_date) " +
											  "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,GETDATE())";
	
    @Override
    public boolean forceRun() {
    	DotConnect dc = new DotConnect();
    	if(DbConnectionFactory.isMySql())
    		dc.setSQL(MYSQL_STRUCTURE_EXISTS);
    	else if(DbConnectionFactory.isPostgres())
    		dc.setSQL(POSTGRESQL_STRUCTURE_EXISTS);
    	else if(DbConnectionFactory.isH2())
    		dc.setSQL(H2_STRUCTURE_EXISTS);
    	else if(DbConnectionFactory.isOracle())
    		dc.setSQL(ORACLE_STRUCTURE_EXISTS);
    	else if(DbConnectionFactory.isMsSql())
    		dc.setSQL(MSSQL_STRUCTURE_EXISTS);
    	List<Map<String, Object>> results = null;
		try {
			results = dc.loadObjectResults();
		} catch (DotDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
        return results==null || results.size()<1;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
    	
    	try{
    		// structure inode
	    	DbConnectionFactory.getConnection().setAutoCommit(true);  
	    	DotConnect dc = new DotConnect();
	    	if(DbConnectionFactory.isMySql())
	    		dc.setSQL(MYSQL_INSERT_STRUCTURE_INODE);
	    	else if(DbConnectionFactory.isPostgres())
	    		dc.setSQL(POSTGRESQL_INSERT_STRUCTURE_INODE);
	    	else if(DbConnectionFactory.isH2())
	    		dc.setSQL(H2_INSERT_STRUCTURE_INODE);
	    	else if(DbConnectionFactory.isOracle())
	    		dc.setSQL(ORACLE_INSERT_STRUCTURE_INODE);
	    	else if(DbConnectionFactory.isMsSql())
	    		dc.setSQL(MSSQL_INSERT_STRUCTURE_INODE);
			dc.addParam(STRUCTURE_INODE);
			dc.loadResult();
			
			// structure
			if(DbConnectionFactory.isMySql())
				dc.setSQL(MYSQL_INSERT_STRUCTURE);
			else if(DbConnectionFactory.isPostgres())
	    		dc.setSQL(POSTGRESQL_INSERT_STRUCTURE);
			else if(DbConnectionFactory.isH2())
	    		dc.setSQL(H2_INSERT_STRUCTURE);
			else if(DbConnectionFactory.isOracle())
	    		dc.setSQL(ORACLE_INSERT_STRUCTURE);
			else if(DbConnectionFactory.isMsSql())
	    		dc.setSQL(MSSQL_INSERT_STRUCTURE);
			dc.addParam(STRUCTURE_INODE);
			dc.loadResult();
	    	
			// --- fields
			// Title
	        if(DbConnectionFactory.isMySql())
				dc.setSQL(MYSQL_INSERT_FIELD_INODE);
	        else if(DbConnectionFactory.isPostgres())
	    		dc.setSQL(POSTGRESQL_INSERT_FIELD_INODE);
	        else if(DbConnectionFactory.isH2())
	    		dc.setSQL(H2_INSERT_FIELD_INODE);
	        else if(DbConnectionFactory.isOracle())
	    		dc.setSQL(ORACLE_INSERT_FIELD_INODE);
	        else if(DbConnectionFactory.isMsSql())
	    		dc.setSQL(MSSQL_INSERT_FIELD_INODE);
			dc.addParam("c623cd2f-6653-47d8-9825-1153061ea088");
			dc.loadResult();
	        
			if(DbConnectionFactory.isMySql())
				dc.setSQL(MYSQL_INSERT_FIELD);
			else if(DbConnectionFactory.isPostgres())
	    		dc.setSQL(POSTGRESQL_INSERT_FIELD);
			else if(DbConnectionFactory.isH2())
	    		dc.setSQL(H2_INSERT_FIELD);
			else if(DbConnectionFactory.isOracle())
	    		dc.setSQL(ORACLE_INSERT_FIELD);
			else if(DbConnectionFactory.isMsSql())
	    		dc.setSQL(MSSQL_INSERT_FIELD);
			dc.addParam("c623cd2f-6653-47d8-9825-1153061ea088"); // inode
			dc.addParam("c541abb1-69b3-4bc5-8430-5e09e5239cc8"); // str inode
			dc.addParam("Title"); // name
			dc.addParam("custom_field"); // type
			dc.addParam("HTMLPage Asset:title"); //relation
			dc.addParam("text1"); // contentlet
			dc.addParam(true); // require
			dc.addParam(true); // indexed
			dc.addParam(true); // listed
			dc.addParam("title"); // velocity name
			dc.addParam(1); // sort order
			dc.addParam("$velutil.mergeTemplate('/static/htmlpage_assets/title_custom_field.vtl')"); // values
			dc.addParam(""); // regex
			dc.addParam(""); // hint
			dc.addParam(""); // default value
			dc.addParam(true); // fixed
			dc.addParam(false); // read only
			dc.addParam(true); // searchable
			dc.addParam(false); // unique
			dc.loadResult();
	        
			// Host or folder
			if(DbConnectionFactory.isMySql())
				dc.setSQL(MYSQL_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isPostgres())
	    		dc.setSQL(POSTGRESQL_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isH2())
	    		dc.setSQL(H2_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isOracle())
	    		dc.setSQL(ORACLE_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isMsSql())
	    		dc.setSQL(MSSQL_INSERT_FIELD_INODE);
			dc.addParam("23b5f1be-935e-442e-be48-1cf2d1c96d71");
			dc.loadResult();
	        
			if(DbConnectionFactory.isMySql())
				dc.setSQL(MYSQL_INSERT_FIELD);
			else if(DbConnectionFactory.isPostgres())
	    		dc.setSQL(POSTGRESQL_INSERT_FIELD);
			else if(DbConnectionFactory.isH2())
	    		dc.setSQL(H2_INSERT_FIELD);
			else if(DbConnectionFactory.isOracle())
	    		dc.setSQL(ORACLE_INSERT_FIELD);
			else if(DbConnectionFactory.isMsSql())
	    		dc.setSQL(MSSQL_INSERT_FIELD);
			dc.addParam("23b5f1be-935e-442e-be48-1cf2d1c96d71"); // inode
			dc.addParam("c541abb1-69b3-4bc5-8430-5e09e5239cc8"); // str inode
			dc.addParam("Host or Folder"); // name
			dc.addParam("host or folder"); // type
			dc.addParam("HTMLPage Asset:hostOrFolder"); //relation
			dc.addParam("text2"); // contentlet
			dc.addParam(true); // require
			dc.addParam(true); // indexed
			dc.addParam(false); // listed
			dc.addParam("hostFolder"); // velocity name
			dc.addParam(2); // sort order
			dc.addParam(""); // values
			dc.addParam(""); // regex
			dc.addParam(""); // hint
			dc.addParam(""); // default value
			dc.addParam(true); // fixed
			dc.addParam(false); // read only
			dc.addParam(true); // searchable
			dc.addParam(false); // unique
			dc.loadResult();
			
			// Url
			if(DbConnectionFactory.isMySql())
				dc.setSQL(MYSQL_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isPostgres())
	    		dc.setSQL(POSTGRESQL_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isH2())
	    		dc.setSQL(H2_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isOracle())
	    		dc.setSQL(ORACLE_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isMsSql())
	    		dc.setSQL(MSSQL_INSERT_FIELD_INODE);
			dc.addParam("a1bfbb4f-b78b-4197-94e7-917f4e812043");
			dc.loadResult();
	        
			if(DbConnectionFactory.isMySql())
				dc.setSQL(MYSQL_INSERT_FIELD);
			else if(DbConnectionFactory.isPostgres())
	    		dc.setSQL(POSTGRESQL_INSERT_FIELD);
			else if(DbConnectionFactory.isH2())
	    		dc.setSQL(H2_INSERT_FIELD);
			else if(DbConnectionFactory.isOracle())
	    		dc.setSQL(ORACLE_INSERT_FIELD);
			else if(DbConnectionFactory.isMsSql())
	    		dc.setSQL(MSSQL_INSERT_FIELD);
			dc.addParam("a1bfbb4f-b78b-4197-94e7-917f4e812043"); // inode
			dc.addParam("c541abb1-69b3-4bc5-8430-5e09e5239cc8"); // str inode
			dc.addParam("Url"); // name
			dc.addParam("text"); // type
			dc.addParam("HTMLPage Asset:url"); //relation
			dc.addParam("text3"); // contentlet
			dc.addParam(true); // require
			dc.addParam(true); // indexed
			dc.addParam(true); // listed
			dc.addParam("url"); // velocity name
			dc.addParam(3); // sort order
			dc.addParam(""); // values
			dc.addParam(""); // regex
			dc.addParam(""); // hint
			dc.addParam(""); // default value
			dc.addParam(true); // fixed
			dc.addParam(false); // read only
			dc.addParam(true); // searchable
			dc.addParam(false); // unique
			dc.loadResult();
			
			// Cache TTL
			if(DbConnectionFactory.isMySql())
				dc.setSQL(MYSQL_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isPostgres())
	    		dc.setSQL(POSTGRESQL_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isH2())
	    		dc.setSQL(H2_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isOracle())
	    		dc.setSQL(ORACLE_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isMsSql())
	    		dc.setSQL(MSSQL_INSERT_FIELD_INODE);
			dc.addParam("e633ab20-0aa1-4ed1-b052-82a711af61df");
			dc.loadResult();
	        
			if(DbConnectionFactory.isMySql())
				dc.setSQL(MYSQL_INSERT_FIELD);
			else if(DbConnectionFactory.isPostgres())
	    		dc.setSQL(POSTGRESQL_INSERT_FIELD);
			else if(DbConnectionFactory.isH2())
	    		dc.setSQL(H2_INSERT_FIELD);
			else if(DbConnectionFactory.isOracle())
	    		dc.setSQL(ORACLE_INSERT_FIELD);
			else if(DbConnectionFactory.isMsSql())
	    		dc.setSQL(MSSQL_INSERT_FIELD);
			dc.addParam("e633ab20-0aa1-4ed1-b052-82a711af61df"); // inode
			dc.addParam("c541abb1-69b3-4bc5-8430-5e09e5239cc8"); // str inode
			dc.addParam("Cache TTL"); // name
			dc.addParam("custom_field"); // type
			dc.addParam("HTMLPage Asset:cacheTtl"); //relation
			dc.addParam("text4"); // contentlet
			dc.addParam(true); // require
			dc.addParam(true); // indexed
			dc.addParam(false); // listed
			dc.addParam("cachettl"); // velocity name
			dc.addParam(4); // sort order
			dc.addParam("$velutil.mergeTemplate('/static/htmlpage_assets/cachettl_custom_field.vtl')"); // values
			dc.addParam("^[0-9]+$"); // regex
			dc.addParam(""); // hint
			dc.addParam(""); // default value
			dc.addParam(true); // fixed
			dc.addParam(false); // read only
			dc.addParam(true); // searchable
			dc.addParam(false); // unique
			dc.loadResult();
			
			// Template
			if(DbConnectionFactory.isMySql())
				dc.setSQL(MYSQL_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isPostgres())
	    		dc.setSQL(POSTGRESQL_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isH2())
	    		dc.setSQL(H2_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isOracle())
	    		dc.setSQL(ORACLE_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isMsSql())
	    		dc.setSQL(MSSQL_INSERT_FIELD_INODE);
			dc.addParam("bf73876b-8517-4123-a0ec-d862ba6e8797");
			dc.loadResult();
	        
			if(DbConnectionFactory.isMySql())
				dc.setSQL(MYSQL_INSERT_FIELD);
			else if(DbConnectionFactory.isPostgres())
	    		dc.setSQL(POSTGRESQL_INSERT_FIELD);
			else if(DbConnectionFactory.isH2())
	    		dc.setSQL(H2_INSERT_FIELD);
			else if(DbConnectionFactory.isOracle())
	    		dc.setSQL(ORACLE_INSERT_FIELD);
			else if(DbConnectionFactory.isMsSql())
	    		dc.setSQL(MSSQL_INSERT_FIELD);
			dc.addParam("bf73876b-8517-4123-a0ec-d862ba6e8797"); // inode
			dc.addParam("c541abb1-69b3-4bc5-8430-5e09e5239cc8"); // str inode
			dc.addParam("Template"); // name
			dc.addParam("custom_field"); // type
			dc.addParam("HTMLPage Asset:template"); //relation
			dc.addParam("text5"); // contentlet
			dc.addParam(true); // require
			dc.addParam(true); // indexed
			dc.addParam(false); // listed
			dc.addParam("template"); // velocity name
			dc.addParam(5); // sort order
			dc.addParam("$velutil.mergeTemplate('/static/htmlpage_assets/template_custom_field.vtl')"); // values
			dc.addParam(""); // regex
			dc.addParam(""); // hint
			dc.addParam(""); // default value
			dc.addParam(true); // fixed
			dc.addParam(false); // read only
			dc.addParam(true); // searchable
			dc.addParam(false); // unique
			dc.loadResult();
			
			// Advance tab
			if(DbConnectionFactory.isMySql())
				dc.setSQL(MYSQL_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isPostgres())
	    		dc.setSQL(POSTGRESQL_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isH2())
	    		dc.setSQL(H2_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isOracle())
	    		dc.setSQL(ORACLE_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isMsSql())
	    		dc.setSQL(MSSQL_INSERT_FIELD_INODE);
			dc.addParam("1aa4bbc6-d30e-4b43-8f13-d6e8f2a58a52");
			dc.loadResult();
	        
			if(DbConnectionFactory.isMySql())
				dc.setSQL(MYSQL_INSERT_FIELD);
			else if(DbConnectionFactory.isPostgres())
	    		dc.setSQL(POSTGRESQL_INSERT_FIELD);
			else if(DbConnectionFactory.isH2())
	    		dc.setSQL(H2_INSERT_FIELD);
			else if(DbConnectionFactory.isOracle())
	    		dc.setSQL(ORACLE_INSERT_FIELD);
			else if(DbConnectionFactory.isMsSql())
	    		dc.setSQL(MSSQL_INSERT_FIELD);
			dc.addParam("1aa4bbc6-d30e-4b43-8f13-d6e8f2a58a52"); // inode
			dc.addParam("c541abb1-69b3-4bc5-8430-5e09e5239cc8"); // str inode
			dc.addParam("Advance Properties"); // name
			dc.addParam("tab_divider"); // type
			dc.addParam("HTMLPage Asset:advanceProperties"); //relation
			dc.addParam("section_divider1"); // contentlet
			dc.addParam(false); // require
			dc.addParam(false); // indexed
			dc.addParam(false); // listed
			dc.addParam("advancetab"); // velocity name
			dc.addParam(6); // sort order
			dc.addParam(""); // values
			dc.addParam(""); // regex
			dc.addParam(""); // hint
			dc.addParam(""); // default value
			dc.addParam(false); // fixed
			dc.addParam(false); // read only
			dc.addParam(false); // searchable
			dc.addParam(false); // unique
			dc.loadResult();
			
			// Show on Menu
			if(DbConnectionFactory.isMySql())
				dc.setSQL(MYSQL_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isPostgres())
	    		dc.setSQL(POSTGRESQL_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isH2())
	    		dc.setSQL(H2_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isOracle())
	    		dc.setSQL(ORACLE_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isMsSql())
	    		dc.setSQL(MSSQL_INSERT_FIELD_INODE);
			dc.addParam("99ac031c-7d72-4b08-bedd-37a71b594950");
			dc.loadResult();
	        
			if(DbConnectionFactory.isMySql())
				dc.setSQL(MYSQL_INSERT_FIELD);
			else if(DbConnectionFactory.isPostgres())
	    		dc.setSQL(POSTGRESQL_INSERT_FIELD);
			else if(DbConnectionFactory.isH2())
	    		dc.setSQL(H2_INSERT_FIELD);
			else if(DbConnectionFactory.isOracle())
	    		dc.setSQL(ORACLE_INSERT_FIELD);
			else if(DbConnectionFactory.isMsSql())
	    		dc.setSQL(MSSQL_INSERT_FIELD);
			dc.addParam("99ac031c-7d72-4b08-bedd-37a71b594950"); // inode
			dc.addParam("c541abb1-69b3-4bc5-8430-5e09e5239cc8"); // str inode
			dc.addParam("Show on Menu"); // name
			dc.addParam("checkbox"); // type
			dc.addParam("HTMLPage Asset:showOnMenu"); //relation
			dc.addParam("text6"); // contentlet
			dc.addParam(false); // require
			dc.addParam(true); // indexed
			dc.addParam(false); // listed
			dc.addParam("showOnMenu"); // velocity name
			dc.addParam(7); // sort order
			dc.addParam("|true"); // values
			dc.addParam(""); // regex
			dc.addParam(""); // hint
			dc.addParam("false"); // default value
			dc.addParam(true); // fixed
			dc.addParam(false); // read only
			dc.addParam(false); // searchable
			dc.addParam(false); // unique
			dc.loadResult();
			
			// Sort order
			if(DbConnectionFactory.isMySql())
				dc.setSQL(MYSQL_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isPostgres())
	    		dc.setSQL(POSTGRESQL_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isH2())
	    		dc.setSQL(H2_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isOracle())
	    		dc.setSQL(ORACLE_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isMsSql())
	    		dc.setSQL(MSSQL_INSERT_FIELD_INODE);
			dc.addParam("1677ca4f-e46f-449f-ae59-4952fb567e5e");
			dc.loadResult();
	        
			if(DbConnectionFactory.isMySql())
				dc.setSQL(MYSQL_INSERT_FIELD);
			else if(DbConnectionFactory.isPostgres())
	    		dc.setSQL(POSTGRESQL_INSERT_FIELD);
			else if(DbConnectionFactory.isH2())
	    		dc.setSQL(H2_INSERT_FIELD);
			else if(DbConnectionFactory.isOracle())
	    		dc.setSQL(ORACLE_INSERT_FIELD);
			else if(DbConnectionFactory.isMsSql())
	    		dc.setSQL(MSSQL_INSERT_FIELD);
			dc.addParam("1677ca4f-e46f-449f-ae59-4952fb567e5e"); // inode
			dc.addParam("c541abb1-69b3-4bc5-8430-5e09e5239cc8"); // str inode
			dc.addParam("Sort Order"); // name
			dc.addParam("text"); // type
			dc.addParam("HTMLPage Asset:sortOrder"); //relation
			dc.addParam("integer1"); // contentlet
			dc.addParam(true); // require
			dc.addParam(true); // indexed
			dc.addParam(false); // listed
			dc.addParam("sortOrder"); // velocity name
			dc.addParam(8); // sort order
			dc.addParam(""); // values
			dc.addParam(""); // regex
			dc.addParam(""); // hint
			dc.addParam(0); // default value
			dc.addParam(true); // fixed
			dc.addParam(false); // read only
			dc.addParam(true); // searchable
			dc.addParam(false); // unique
			dc.loadResult();
			
			// Friendly name
	        if(DbConnectionFactory.isMySql())
				dc.setSQL(MYSQL_INSERT_FIELD_INODE);
	        else if(DbConnectionFactory.isPostgres())
	    		dc.setSQL(POSTGRESQL_INSERT_FIELD_INODE);
	        else if(DbConnectionFactory.isH2())
	    		dc.setSQL(H2_INSERT_FIELD_INODE);
	        else if(DbConnectionFactory.isOracle())
	    		dc.setSQL(ORACLE_INSERT_FIELD_INODE);
	        else if(DbConnectionFactory.isMsSql())
	    		dc.setSQL(MSSQL_INSERT_FIELD_INODE);
			dc.addParam("d8a7431e-140d-4076-bf07-17fdfad6a14e");
			dc.loadResult();
	        
			if(DbConnectionFactory.isMySql())
				dc.setSQL(MYSQL_INSERT_FIELD);
			else if(DbConnectionFactory.isPostgres())
	    		dc.setSQL(POSTGRESQL_INSERT_FIELD);
			else if(DbConnectionFactory.isH2())
	    		dc.setSQL(H2_INSERT_FIELD);
			else if(DbConnectionFactory.isOracle())
	    		dc.setSQL(ORACLE_INSERT_FIELD);
			else if(DbConnectionFactory.isMsSql())
	    		dc.setSQL(MSSQL_INSERT_FIELD);
			dc.addParam("d8a7431e-140d-4076-bf07-17fdfad6a14e"); // inode
			dc.addParam("c541abb1-69b3-4bc5-8430-5e09e5239cc8"); // str inode
			dc.addParam("Friendly Name"); // name
			dc.addParam("text"); // type
			dc.addParam("HTMLPage Asset:friendlyName"); //relation
			dc.addParam("text7"); // contentlet
			dc.addParam(true); // require
			dc.addParam(true); // indexed
			dc.addParam(false); // listed
			dc.addParam("friendlyName"); // velocity name
			dc.addParam(9); // sort order
			dc.addParam(""); // values
			dc.addParam(""); // regex
			dc.addParam(""); // hint
			dc.addParam(""); // default value
			dc.addParam(true); // fixed
			dc.addParam(false); // read only
			dc.addParam(true); // searchable
			dc.addParam(false); // unique
			dc.loadResult();
			
			// Redirect Url
			
			if(DbConnectionFactory.isMySql())
				dc.setSQL(MYSQL_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isPostgres())
	    		dc.setSQL(POSTGRESQL_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isH2())
	    		dc.setSQL(H2_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isOracle())
	    		dc.setSQL(ORACLE_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isMsSql())
	    		dc.setSQL(MSSQL_INSERT_FIELD_INODE);
			dc.addParam("ba99667c-87be-44cd-82b6-4aa7bb157ac7");
			dc.loadResult();
	        
			if(DbConnectionFactory.isMySql())
				dc.setSQL(MYSQL_INSERT_FIELD);
			else if(DbConnectionFactory.isPostgres())
	    		dc.setSQL(POSTGRESQL_INSERT_FIELD);
			else if(DbConnectionFactory.isH2())
	    		dc.setSQL(H2_INSERT_FIELD);
			else if(DbConnectionFactory.isOracle())
	    		dc.setSQL(ORACLE_INSERT_FIELD);
			else if(DbConnectionFactory.isMsSql())
	    		dc.setSQL(MSSQL_INSERT_FIELD);
			dc.addParam("ba99667c-87be-44cd-82b6-4aa7bb157ac7"); // inode
			dc.addParam("c541abb1-69b3-4bc5-8430-5e09e5239cc8"); // str inode
			dc.addParam("Redirect Url"); // name
			dc.addParam("custom_field"); // type
			dc.addParam("HTMLPage Asset:redirectURL"); //relation
			dc.addParam("text8"); // contentlet
			dc.addParam(false); // require
			dc.addParam(true); // indexed
			dc.addParam(false); // listed
			dc.addParam("redirecturl"); // velocity name
			dc.addParam(10); // sort order
			dc.addParam("$velutil.mergeTemplate('/static/htmlpage_assets/redirect_custom_field.vtl')"); // values
			dc.addParam(""); // regex
			dc.addParam(""); // hint
			dc.addParam(""); // default value
			dc.addParam(true); // fixed
			dc.addParam(false); // read only
			dc.addParam(true); // searchable
			dc.addParam(false); // unique
			dc.loadResult();
			
			// Https Required
			
			if(DbConnectionFactory.isMySql())
				dc.setSQL(MYSQL_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isPostgres())
	    		dc.setSQL(POSTGRESQL_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isH2())
	    		dc.setSQL(H2_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isOracle())
	    		dc.setSQL(ORACLE_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isMsSql())
	    		dc.setSQL(MSSQL_INSERT_FIELD_INODE);
			dc.addParam("b0d65eee-b050-4fa2-bcf6-4016dc4e20af");
			dc.loadResult();
	        
			if(DbConnectionFactory.isMySql())
				dc.setSQL(MYSQL_INSERT_FIELD);
			else if(DbConnectionFactory.isPostgres())
	    		dc.setSQL(POSTGRESQL_INSERT_FIELD);
			else if(DbConnectionFactory.isH2())
	    		dc.setSQL(H2_INSERT_FIELD);
			else if(DbConnectionFactory.isOracle())
	    		dc.setSQL(ORACLE_INSERT_FIELD);
			else if(DbConnectionFactory.isMsSql())
	    		dc.setSQL(MSSQL_INSERT_FIELD);
			dc.addParam("b0d65eee-b050-4fa2-bcf6-4016dc4e20af"); // inode
			dc.addParam("c541abb1-69b3-4bc5-8430-5e09e5239cc8"); // str inode
			dc.addParam("HTTPS Required"); // name
			dc.addParam("checkbox"); // type
			dc.addParam("HTMLPage Asset:httpsRequired"); //relation
			dc.addParam("text9"); // contentlet
			dc.addParam(false); // require
			dc.addParam(true); // indexed
			dc.addParam(false); // listed
			dc.addParam("httpsreq"); // velocity name
			dc.addParam(11); // sort order
			dc.addParam("|true"); // values
			dc.addParam(""); // regex
			dc.addParam(""); // hint
			dc.addParam("false"); // default value
			dc.addParam(true); // fixed
			dc.addParam(false); // read only
			dc.addParam(false); // searchable
			dc.addParam(false); // unique
			dc.loadResult();
			
			// SEO Desc
			if(DbConnectionFactory.isMySql())
				dc.setSQL(MYSQL_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isPostgres())
	    		dc.setSQL(POSTGRESQL_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isH2())
	    		dc.setSQL(H2_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isOracle())
	    		dc.setSQL(ORACLE_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isMsSql())
	    		dc.setSQL(MSSQL_INSERT_FIELD_INODE);
			dc.addParam("dfc5f28d-d47e-4007-869a-f2d5cfbc3d39");
			dc.loadResult();
	        
			if(DbConnectionFactory.isMySql())
				dc.setSQL(MYSQL_INSERT_FIELD);
			else if(DbConnectionFactory.isPostgres())
	    		dc.setSQL(POSTGRESQL_INSERT_FIELD);
			else if(DbConnectionFactory.isH2())
	    		dc.setSQL(H2_INSERT_FIELD);
			else if(DbConnectionFactory.isOracle())
	    		dc.setSQL(ORACLE_INSERT_FIELD);
			else if(DbConnectionFactory.isMsSql())
	    		dc.setSQL(MSSQL_INSERT_FIELD);
			dc.addParam("dfc5f28d-d47e-4007-869a-f2d5cfbc3d39"); // inode
			dc.addParam("c541abb1-69b3-4bc5-8430-5e09e5239cc8"); // str inode
			dc.addParam("SEO Description"); // name
			dc.addParam("textarea"); // type
			dc.addParam("HTMLPage Asset:seoDescription"); //relation
			dc.addParam("text_area1"); // contentlet
			dc.addParam(false); // require
			dc.addParam(true); // indexed
			dc.addParam(false); // listed
			dc.addParam("seodescription"); // velocity name
			dc.addParam(12); // sort order
			dc.addParam(""); // values
			dc.addParam(""); // regex
			dc.addParam(""); // hint
			dc.addParam(""); // default value
			dc.addParam(true); // fixed
			dc.addParam(false); // read only
			dc.addParam(true); // searchable
			dc.addParam(false); // unique
			dc.loadResult();
			
			// SEO keywords
			
			if(DbConnectionFactory.isMySql())
				dc.setSQL(MYSQL_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isPostgres())
	    		dc.setSQL(POSTGRESQL_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isH2())
	    		dc.setSQL(H2_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isOracle())
	    		dc.setSQL(ORACLE_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isMsSql())
	    		dc.setSQL(MSSQL_INSERT_FIELD_INODE);
			dc.addParam("f00b3844-820d-4967-9f8e-0cce68d22b13");
			dc.loadResult();
	        
			if(DbConnectionFactory.isMySql())
				dc.setSQL(MYSQL_INSERT_FIELD);
			else if(DbConnectionFactory.isPostgres())
	    		dc.setSQL(POSTGRESQL_INSERT_FIELD);
			else if(DbConnectionFactory.isH2())
	    		dc.setSQL(H2_INSERT_FIELD);
			else if(DbConnectionFactory.isOracle())
	    		dc.setSQL(ORACLE_INSERT_FIELD);
			else if(DbConnectionFactory.isMsSql())
	    		dc.setSQL(MSSQL_INSERT_FIELD);
			dc.addParam("f00b3844-820d-4967-9f8e-0cce68d22b13"); // inode
			dc.addParam("c541abb1-69b3-4bc5-8430-5e09e5239cc8"); // str inode
			dc.addParam("SEO Keywords"); // name
			dc.addParam("textarea"); // type
			dc.addParam("HTMLPage Asset:seoKeywords"); //relation
			dc.addParam("text_area2"); // contentlet
			dc.addParam(false); // require
			dc.addParam(true); // indexed
			dc.addParam(false); // listed
			dc.addParam("seokeywords"); // velocity name
			dc.addParam(13); // sort order
			dc.addParam(""); // values
			dc.addParam(""); // regex
			dc.addParam(""); // hint
			dc.addParam(""); // default value
			dc.addParam(true); // fixed
			dc.addParam(false); // read only
			dc.addParam(true); // searchable
			dc.addParam(false); // unique
			dc.loadResult();
			
			// Page Metadata
			
			if(DbConnectionFactory.isMySql())
				dc.setSQL(MYSQL_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isPostgres())
	    		dc.setSQL(POSTGRESQL_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isH2())
	    		dc.setSQL(H2_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isOracle())
	    		dc.setSQL(ORACLE_INSERT_FIELD_INODE);
			else if(DbConnectionFactory.isMsSql())
	    		dc.setSQL(MSSQL_INSERT_FIELD_INODE);
			dc.addParam("c50906a6-dafb-4348-a185-a9334448813c");
			dc.loadResult();
	        
			if(DbConnectionFactory.isMySql())
				dc.setSQL(MYSQL_INSERT_FIELD);
			else if(DbConnectionFactory.isPostgres())
	    		dc.setSQL(POSTGRESQL_INSERT_FIELD);
			else if(DbConnectionFactory.isH2())
	    		dc.setSQL(H2_INSERT_FIELD);
			else if(DbConnectionFactory.isOracle())
	    		dc.setSQL(ORACLE_INSERT_FIELD);
			else if(DbConnectionFactory.isMsSql())
	    		dc.setSQL(MSSQL_INSERT_FIELD);
			dc.addParam("c50906a6-dafb-4348-a185-a9334448813c"); // inode
			dc.addParam("c541abb1-69b3-4bc5-8430-5e09e5239cc8"); // str inode
			dc.addParam("Page Metadata"); // name
			dc.addParam("textarea"); // type
			dc.addParam("HTMLPage Asset:seoKeywords"); //relation
			dc.addParam("text_area3"); // contentlet
			dc.addParam(false); // require
			dc.addParam(true); // indexed
			dc.addParam(false); // listed
			dc.addParam("pagemetadata"); // velocity name
			dc.addParam(14); // sort order
			dc.addParam(""); // values
			dc.addParam(""); // regex
			dc.addParam(""); // hint
			dc.addParam(""); // default value
			dc.addParam(true); // fixed
			dc.addParam(false); // read only
			dc.addParam(true); // searchable
			dc.addParam(false); // unique
			dc.loadResult();
			
    	}catch(Exception e){
			Logger.error(this, e.getMessage(),e);
		}
    }
}
