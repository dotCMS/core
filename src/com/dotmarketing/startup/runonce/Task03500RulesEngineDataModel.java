package com.dotmarketing.startup.runonce;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

import java.util.List;

/**
 * This task creates and updates a number of tables involved in the Rules Engine
 * implementation, namely, the core structure of the Rules Engine functionality,
 * Conditionlet-specific modifications, among others.
 * 
 * @author Daniel Silva
 * @version 1.0
 * @since 03-04-2015
 *
 */
public class Task03500RulesEngineDataModel extends AbstractJDBCStartupTask {


    private final StringBuilder MYSQL = new StringBuilder()
            // create RULE table
            .append("create table dot_rule(")
            .append("id varchar(36) primary key,")
            .append("name varchar(255) not null,")
            .append("fire_on varchar(20),")
            .append("short_circuit boolean,")
            .append("host varchar(36) not null,")
            .append("folder varchar(36) not null,")
            .append("priority int default 0,")
            .append("enabled boolean default false,")
            .append("mod_date datetime")
            .append(");")

            .append("create index idx_rules_fire_on on dot_rule (fire_on);")

                    // unique constraint rule table
            .append("alter table dot_rule add constraint rule_name_host unique (name, host);")

                    // create RULE_CONDITION_GROUP table
            .append("create table rule_condition_group(")
            .append("id varchar(36) primary key,")
            .append("rule_id varchar(36) references dot_rule(id),")
            .append("operator varchar(10) not null,")
            .append("priority int default 0,")
            .append("mod_date datetime")
            .append(");")


                    // create RULE_CONDITION table
            .append("create table rule_condition(")
            .append("id varchar(36) primary key,")
            .append("name varchar(255) not null,")
            .append("conditionlet text not null,")
            .append("condition_group varchar(36) references rule_condition_group(id),")
            .append("comparison varchar(36) not null,")
            .append("operator varchar(10) not null,")
            .append("priority int default 0,")
            .append("mod_date datetime")
            .append(");")

                    // create RULE_CONDITION_VALUE table
            .append("create table rule_condition_value(")
            .append("id varchar(36) primary key,")
            .append("condition_id varchar(36) references rule_condition(id),")
            .append("paramkey varchar(255) not null,")
            .append("value text,")
            .append("priority int default 0")
            .append(");")

            // create RULE_ACTION  table
            .append("create table rule_action (")
            .append("id varchar(36) primary key,")
            .append("name varchar(255) not null,")
            .append("rule_id varchar(36) references dot_rule(id),")
            .append("priority int default 0,")
            .append("actionlet text not null,")
            .append("mod_date datetime")
            .append(");")

            // create RULE_CONDITION_VALUE table
            .append("create table rule_action_pars(")
            .append("id varchar(36) primary key,")
            .append("rule_action_id varchar(36) references rule_action(id),")
            .append("paramkey varchar(255) not null,")
            .append("value text")
            .append(");")
    		
			 // Create the ANALYTIC_SUMMARY_USER_VISITS table
			.append("CREATE TABLE analytic_summary_user_visits (")
			.append("user_id VARCHAR(255) NOT NULL,")
			.append("host_id VARCHAR(36) NOT NULL,")
			.append("visits BIGINT NOT NULL,")
			.append("last_start_date DATETIME(3) NOT NULL,")
			.append("PRIMARY KEY (user_id, host_id)")
			.append(");")
    		
    		.append("CREATE INDEX idx_analytic_summary_user_visits_1 ON analytic_summary_user_visits (user_id);")
    		.append("CREATE INDEX idx_analytic_summary_user_visits_2 ON analytic_summary_user_visits (host_id);")
    		.append("CREATE INDEX idx_analytic_summary_user_visits_3 ON analytic_summary_user_visits (last_start_date);")
    		
    		.append("ALTER TABLE clickstream MODIFY start_date DATETIME(3);")
    		.append("ALTER TABLE clickstream MODIFY end_date DATETIME(3);");
    
    private final StringBuilder POSTGRES = new StringBuilder()
            // create RULE table
            .append("create table dot_rule(")
            .append("id varchar(36) primary key,")
            .append("name varchar(255) not null,")
            .append("fire_on varchar(20),")
            .append("short_circuit boolean default false,")
            .append("host varchar(36) not null,")
            .append("folder varchar(36) not null,")
            .append("priority int default 0,")
            .append("enabled boolean default false,")
            .append("mod_date timestamp,")
            .append("unique (name, host)")
            .append(");")

            .append("create index idx_rules_fire_on on dot_rule (fire_on);")


                            // create RULE_CONDITION_GROUP table
            .append("create table rule_condition_group(")
            .append("id varchar(36) primary key,")
            .append("rule_id varchar(36) references dot_rule(id),")
            .append("operator varchar(10) not null,")
            .append("priority int default 0,")
            .append("mod_date timestamp")
            .append(");")

                    // create RULE_CONDITION table
            .append("create table rule_condition(")
            .append("id varchar(36) primary key,")
            .append("name varchar(255) not null,")
            .append("conditionlet text not null,")
            .append("condition_group varchar(36) references rule_condition_group(id),")
            .append("comparison varchar(36) not null,")
            .append("operator varchar(10) not null,")
            .append("priority int default 0,")
            .append("mod_date timestamp")
            .append(");")

                    // create RULE_CONDITION_VALUE table
            .append("create table rule_condition_value(")
            .append("id varchar(36) primary key,")
            .append("condition_id varchar(36) references rule_condition(id),")
            .append("paramkey varchar(255) not null,")
            .append("value text,")
            .append("priority int default 0")
            .append(");")

                    // create RULE_ACTION  table
            .append("create table rule_action (")
            .append("id varchar(36) primary key,")
            .append("name varchar(255) not null,")
            .append("rule_id varchar(36) references dot_rule(id),")
            .append("priority int default 0,")
            .append("actionlet text not null,")
            .append("mod_date timestamp")
            .append(");")

            // create RULE_CONDITION_VALUE table
            .append("create table rule_action_pars(")
            .append("id varchar(36) primary key,")
            .append("rule_action_id varchar(36) references rule_action(id),")
            .append("paramkey varchar(255) not null,")
            .append("value text")
            .append(");")
            
            // Create the ANALYTIC_SUMMARY_USER_VISITS table
			.append("CREATE TABLE analytic_summary_user_visits (")
			.append("user_id VARCHAR(255) NOT NULL,")
			.append("host_id VARCHAR(36) NOT NULL,")
			.append("visits INT8 NOT NULL,")
			.append("last_start_date TIMESTAMP NOT NULL,")
			.append("PRIMARY KEY (user_id, host_id)")
			.append(");")
    		
    		.append("CREATE INDEX idx_analytic_summary_user_visits_1 ON analytic_summary_user_visits (user_id);")
    		.append("CREATE INDEX idx_analytic_summary_user_visits_2 ON analytic_summary_user_visits (host_id);")
    		.append("CREATE INDEX idx_analytic_summary_user_visits_3 ON analytic_summary_user_visits (last_start_date);");

    private final StringBuilder ORACLE = new StringBuilder()
            // create RULE table
            .append("create table dot_rule(")
            .append("id varchar2(36),")
            .append("name varchar2(255) not null,")
            .append("fire_on varchar2(20),")
            .append("short_circuit  number(1,0) default 0,")
            .append("host varchar2(36) not null,")
            .append("folder varchar2(36) not null,")
            .append("priority number(10,0) default 0,")
            .append("enabled  number(1,0) default 0,")
            .append("mod_date timestamp,")
            .append("primary key (id),")
            .append("unique (name, host)")
            .append(");")

            .append("create index idx_rules_fire_on on dot_rule (fire_on);")

                            // create RULE_CONDITION_GROUP table
            .append("create table rule_condition_group(")
            .append("id varchar(36) primary key,")
            .append("rule_id varchar(36) references dot_rule(id),")
            .append("operator varchar(10) not null,")
            .append("priority number(10,0) default 0,")
            .append("mod_date timestamp")
            .append(");")


                    // create RULE_CONDITION table
            .append("create table rule_condition(")
            .append("id varchar2(36) primary key,")
            .append("name varchar2(255) not null,")
            .append("conditionlet nclob not null,")
            .append("condition_group varchar(36) references rule_condition_group(id),")
            .append("comparison varchar2(36) not null,")
            .append("operator varchar2(10) not null,")
            .append("priority number(10,0) default 0,")
            .append("mod_date timestamp")
            .append(");")

                    // create RULE_CONDITION_VALUE table
            .append("create table rule_condition_value(")
            .append("id varchar(36) primary key,")
            .append("condition_id varchar(36) references rule_condition(id),")
            .append("paramkey varchar2(255) not null,")
            .append("value nclob,")
            .append("priority number(10,0) default 0")
            .append(");")

             // create RULE_ACTION  table
            .append("create table rule_action (")
            .append("id varchar2(36) primary key,")
            .append("name varchar2(255) not null,")
            .append("rule_id varchar2(36) references dot_rule(id),")
            .append("priority number(10,0) default 0,")
            .append("actionlet nclob not null,")
            .append("mod_date timestamp")
            .append(");")

            // create RULE_CONDITION_VALUE table
            .append("create table rule_action_pars(")
            .append("id varchar2(36) primary key,")
            .append("rule_action_id varchar2(36) references rule_action(id),")
            .append("paramkey varchar2(255) not null,")
            .append("value nclob")
            .append(");")
    
    		// Create the ANALYTIC_SUMMARY_USER_VISITS table
 			.append("CREATE TABLE analytic_summary_user_visits (")
 			.append("user_id VARCHAR2(255) NOT NULL,")
 			.append("host_id VARCHAR2(36) NOT NULL,")
 			.append("visits NUMBER(19,0) NOT NULL,")
 			.append("last_start_date TIMESTAMP NOT NULL,")
 			.append("PRIMARY KEY (user_id, host_id)")
 			.append(");")
     		
     		.append("CREATE INDEX idx_analytic_user_visits_1 ON analytic_summary_user_visits (user_id);")
     		.append("CREATE INDEX idx_analytic_user_visits_2 ON analytic_summary_user_visits (host_id);")
     		.append("CREATE INDEX idx_analytic_user_visits_3 ON analytic_summary_user_visits (last_start_date);");

    private final StringBuilder MSSQL = new StringBuilder()
            // create RULE table
            .append("create table dot_rule(")
            .append("id varchar(36) primary key,")
            .append("name varchar(255) not null,")
            .append("fire_on varchar(20),")
            .append("short_circuit tinyint default 0,")
            .append("host varchar(36) not null,")
            .append("folder varchar(36) not null,")
            .append("priority int default 0,")
            .append("enabled tinyint default 0,")
            .append("mod_date datetime,")
            .append("unique (name, host)")
            .append(");")

            .append("create index idx_rules_fire_on on dot_rule (fire_on);")

                            // create RULE_CONDITION_GROUP table
            .append("create table rule_condition_group(")
            .append("id varchar(36) primary key,")
            .append("rule_id varchar(36) references dot_rule(id),")
            .append("operator varchar(10) not null,")
            .append("priority int default 0,")
            .append("mod_date datetime")
            .append(");")

                    // create RULE_CONDITION table
            .append("create table rule_condition(")
            .append("id varchar(36) primary key,")
            .append("name varchar(255) not null,")
            .append("conditionlet text not null,")
            .append("condition_group varchar(36) references rule_condition_group(id),")
            .append("comparison varchar(36) not null,")
            .append("operator varchar(10) not null,")
            .append("priority int default 0,")
            .append("mod_date datetime")
            .append(");")

                    // create RULE_CONDITION_VALUE table
            .append("create table rule_condition_value(")
            .append("id varchar(36) primary key,")
            .append("condition_id varchar(36) references rule_condition(id),")
            .append("paramkey varchar(255) not null,")
            .append("value text,")
            .append("priority int default 0")
            .append(");")

             // create RULE_ACTION  table
            .append("create table rule_action (")
            .append("id varchar(36) primary key,")
            .append("name varchar(255) not null,")
            .append("rule_id varchar(36) references dot_rule(id),")
            .append("priority int default 0,")
            .append("actionlet text not null,")
            .append("mod_date datetime")
            .append(");")

            // create RULE_CONDITION_VALUE table
            .append("create table rule_action_pars(")
            .append("id varchar(36) primary key,")
            .append("rule_action_id varchar(36) references rule_action(id),")
            .append("paramkey varchar(255) not null,")
            .append("value text")
            .append(");")
    
	        // Create the ANALYTIC_SUMMARY_USER_VISITS table
			.append("CREATE TABLE analytic_summary_user_visits (")
			.append("user_id VARCHAR(255) NOT NULL,")
			.append("host_id VARCHAR(36) NOT NULL,")
			.append("visits NUMERIC(19,0) NOT NULL,")
			.append("last_start_date DATETIME NOT NULL,")
			.append("PRIMARY KEY (user_id, host_id)")
			.append(");")
			
			.append("CREATE INDEX idx_analytic_summary_user_visits_1 ON analytic_summary_user_visits (user_id);")
			.append("CREATE INDEX idx_analytic_summary_user_visits_2 ON analytic_summary_user_visits (host_id);")
			.append("CREATE INDEX idx_analytic_summary_user_visits_3 ON analytic_summary_user_visits (last_start_date);");

    private final StringBuilder H2 = new StringBuilder()
            // create RULE table
            .append("create table dot_rule(")
            .append("id varchar(36) primary key,")
            .append("name varchar(255) not null,")
            .append("fire_on varchar(20),")
            .append("short_circuit boolean default false,")
            .append("host varchar(36) not null,")
            .append("folder varchar(36) not null,")
            .append("priority int default 0,")
            .append("enabled boolean default false,")
            .append("mod_date timestamp,")
            .append("unique (name, host)")
            .append(");")


            .append("create index idx_rules_fire_on on dot_rule (fire_on);")

                            // create RULE_CONDITION_GROUP table
            .append("create table rule_condition_group(")
            .append("id varchar(36) primary key,")
            .append("rule_id varchar(36) references dot_rule(id),")
            .append("operator varchar(10) not null,")
            .append("priority int default 0,")
            .append("mod_date timestamp")
            .append(");")

                    // create RULE_CONDITION table
            .append("create table rule_condition(")
            .append("id varchar(36) primary key,")
            .append("name varchar(255) not null,")
            .append("conditionlet text not null,")
            .append("condition_group varchar(36) references rule_condition_group(id),")
            .append("comparison varchar(36) not null,")
            .append("operator varchar(10) not null,")
            .append("priority int default 0,")
            .append("mod_date timestamp")
            .append(");")

                    // create RULE_CONDITION_VALUE table
            .append("create table rule_condition_value(")
            .append("id varchar(36) primary key,")
            .append("condition_id varchar(36) references rule_condition(id),")
            .append("paramkey varchar(255) not null,")
            .append("value text,")
            .append("priority int default 0")
            .append(");")

                    // create RULE_ACTION  table
            .append("create table rule_action (")
            .append("id varchar(36) primary key,")
            .append("name varchar(255) not null,")
            .append("rule_id varchar(36) references dot_rule(id),")
            .append("priority int default 0,")
            .append("actionlet text not null,")
            .append("mod_date timestamp")
            .append(");")

            // create RULE_CONDITION_VALUE table
            .append("create table rule_action_pars(")
            .append("id varchar(36) primary key,")
            .append("rule_action_id varchar(36) references rule_action(id),")
            .append("paramkey varchar(255) not null,")
            .append("value text")
            .append(");")
    
            // Create the ANALYTIC_SUMMARY_USER_VISITS table
 			.append("CREATE TABLE analytic_summary_user_visits (")
 			.append("user_id VARCHAR(255) NOT NULL,")
 			.append("host_id VARCHAR(36) NOT NULL,")
 			.append("visits BIGINT NOT NULL,")
 			.append("last_start_date TIMESTAMP NOT NULL,")
 			.append("PRIMARY KEY (user_id, host_id)")
 			.append(");")
 			
 			.append("CREATE INDEX idx_analytic_summary_user_visits_1 ON analytic_summary_user_visits (user_id);")
 			.append("CREATE INDEX idx_analytic_summary_user_visits_2 ON analytic_summary_user_visits (host_id);")
 			.append("CREATE INDEX idx_analytic_summary_user_visits_3 ON analytic_summary_user_visits (last_start_date);");


    @Override
    public boolean forceRun() {
        return true;
    }

    /**
     * The SQL for Postgres
     *
     * @return
     */
    @Override
    public String getPostgresScript() {
        return POSTGRES.toString();
    }

    /**
     * The SQL MySQL
     *
     * @return
     */
    @Override
    public String getMySQLScript() {
        return MYSQL.toString();
    }

    /**
     * The SQL for Oracle
     *
     * @return
     */
    @Override
    public String getOracleScript() {
        return ORACLE.toString();
    }

    /**
     * The SQL for MSSQL
     *
     * @return
     */
    @Override
    public String getMSSQLScript() {
        return MSSQL.toString();
    }

    /**
     * The SQL for H2
     *
     * @return
     */
    @Override
    public String getH2Script() {
        return H2.toString();
    }

    /**
     * This is a list of tables which will get the constraints dropped prior to the task executing and then get recreated afer the execution of the DB Specific SQL
     *
     * @return
     */
    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }

}
