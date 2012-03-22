package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.startup.*;

public class Task00766AddFieldVariableTable extends AbstractJDBCStartupTask {

	@Override
	public String getMSSQLScript() {
		return " create table field_variable ( " 
				+ " id varchar(36) not null, "
				+ " field_id varchar(36) null, "
				+ " variable_name varchar(255) null, "
				+ " variable_key varchar(255) null, "
				+ " variable_value varchar(255) null, "
				+ " user_id varchar(255) null, "
				+ " last_mod_date datetime null, "
				+ " primary key (id)); " ;
	}

	@Override
	public String getMySQLScript() {
		return " create table field_variable ( " 
				+ " id varchar(36) not null, "
				+ " field_id varchar(36), "
				+ " variable_name varchar(255), "
				+ " variable_key varchar(255), "
				+ " variable_value varchar(255), "
				+ " user_id varchar(255), "
				+ " last_mod_date date, "
				+ " primary key (id)); ";
		}

	@Override
	public String getOracleScript() {
		return " create table field_variable ( " 
				+ " id varchar(36) not null, " 
				+ " field_id varchar(36), "
				+ " variable_name varchar2(255), "
				+ " variable_key varchar2(255), "
				+ " variable_value varchar2(255), "
				+ " user_id varchar2(255), "
				+ " last_mod_date date, "
				+ " primary key (id)); ";
		}
	
	@Override
	public String getPostgresScript() {		
		return "CREATE TABLE field_variable " 
				+ " ( id varchar(36) NOT NULL, " 
				+ " field_id varchar(36), " 
				+ " variable_name character varying(255), " 
				+ " variable_key character varying(255), " 
				+ " variable_value character varying(255), " 
				+ " user_id character varying(255), " 
				+ " last_mod_date date, " 
				+ " CONSTRAINT field_variable_pkey PRIMARY KEY (id)) " 
				+ " WITH (OIDS=FALSE); ";
	}
	

	public boolean forceRun() {
		return true;
	}

	@Override
	protected List<String> getTablesToDropConstraints() {
		// TODO Auto-generated method stub
		return null;
	}

}
