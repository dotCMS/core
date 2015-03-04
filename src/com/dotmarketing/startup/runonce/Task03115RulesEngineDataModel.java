package com.dotmarketing.startup.runonce;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

import java.util.List;

public class Task03115RulesEngineDataModel extends AbstractJDBCStartupTask {


    private final StringBuilder MYSQL = new StringBuilder()
            // create RULE table
            .append("create table dot_rule(")
            .append("id varchar(36) primary key,")
            .append("name varchar(255) not null,")
            .append("fire_policy tinyint(1),")
            .append("short_circuit boolean,")
            .append("host varchar(36) not null,")
            .append("folder varchar(36) not null,")
            .append("fire_order int default 0,")
            .append("enabled boolean default false")
            .append(");")

                    // unique constraint rule table
            .append("alter table rule add constraint rule_name_host unique (name, host);")

                    // create RULE_CONDITION table
            .append("create table rule_condition(")
            .append("id varchar(36) primary key,")
            .append("name varchar(255) not null,")
            .append("rule_id varchar(36) references dot_rule(id),")
            .append("conditionlet text not null,")
            .append("rel_operator varchar(20) not null,")
            .append("value text")
            .append(");")

                    // create RULE_ACTION_CLASS table
            .append("create table rule_action_class(")
            .append("id varchar(36) primary key,")
            .append("name varchar(255) not null,")
            .append("rule_id varchar(36) references dot_rule(id),")
            .append("fire_order int default 0,")
            .append("actionlet text not null")
            .append(");");


    private final StringBuilder POSTGRES = new StringBuilder()
            // create RULE table
            .append("create table dot_rule(")
            .append("id varchar(36) primary key,")
            .append("name varchar(255) not null,")
            .append("fire_policy numeric(1,0),")
            .append("short_circuit boolean default false,")
            .append("host varchar(36) not null,")
            .append("folder varchar(36) not null,")
            .append("fire_order int default 0,")
            .append("enabled boolean default false,")
            .append("unique (name, host)")
            .append(");")

                    // create RULE_CONDITION table
            .append("create table rule_condition(")
            .append("id varchar(36) primary key,")
            .append("name varchar(255) not null,")
            .append("rule_id varchar(36) references dot_rule(id),")
            .append("conditionlet text not null,")
            .append("rel_operator varchar(20) not null,")
            .append("value text")
            .append(");")

                    // create RULE_ACTION_CLASS table
            .append("create table rule_action_class(")
            .append("id varchar(36) primary key,")
            .append("name varchar(255) not null,")
            .append("rule_id varchar(36) references dot_rule(id),")
            .append("fire_order int default 0,")
            .append("actionlet text not null")
            .append(");");

    private final StringBuilder ORACLE = new StringBuilder()
            // create RULE table
            .append("create table dot_rule(")
            .append("id varchar2(36),")
            .append("name varchar2(255) not null,")
            .append("fire_policy number(1,0),")
            .append("short_circuit  number(1,0) default 0,")
            .append("host varchar2(36) not null,")
            .append("folder varchar2(36) not null,")
            .append("fire_order number(10,0) default 0,")
            .append("enabled  number(1,0) default 0,")
            .append("primary key (id),")
            .append("unique (name, host)")
            .append(");")

                    // create RULE_CONDITION table
            .append("create table rule_condition(")
            .append("id varchar2(36) primary key,")
            .append("name varchar2(255) not null,")
            .append("rule_id varchar2(36) references dot_rule(id),")
            .append("conditionlet nclob not null,")
            .append("rel_operator varchar2(20) not null,")
            .append("value nclob")
            .append(");")

                    // create RULE_ACTION_CLASS table
            .append("create table rule_action_class(")
            .append("id varchar2(36) primary key,")
            .append("name varchar2(255) not null,")
            .append("rule_id varchar2(36) references dot_rule(id),")
            .append("fire_order number(10,0) default 0,")
            .append("actionlet nclob not null")
            .append(");");

    private final StringBuilder MSSQL = new StringBuilder()
            // create RULE table
            .append("create table dot_rule(")
            .append("id varchar(36) primary key,")
            .append("name varchar(255) not null,")
            .append("fire_policy tinyint,")
            .append("short_circuit tinyint default 0,")
            .append("host varchar(36) not null,")
            .append("folder varchar(36) not null,")
            .append("fire_order int default 0,")
            .append("enabled tinyint default 0,")
            .append("unique (name, host)")
            .append(");")

                    // create RULE_CONDITION table
            .append("create table rule_condition(")
            .append("id varchar(36) primary key,")
            .append("name varchar(255) not null,")
            .append("rule_id varchar(36) references dot_rule(id),")
            .append("conditionlet text not null,")
            .append("rel_operator varchar(20) not null,")
            .append("value text")

            .append(");")

                    // create RULE_ACTION_CLASS table
            .append("create table rule_action_class(")
            .append("id varchar(36) primary key,")
            .append("name varchar(255) not null,")
            .append("rule_id varchar(36) references dot_rule(id),")
            .append("fire_order int default 0,")
            .append("actionlet text not null")
            .append(");");

    private final StringBuilder H2 = new StringBuilder()
            // create RULE table
            .append("create table dot_rule(")
            .append("id varchar(36) primary key,")
            .append("name varchar(255) not null,")
            .append("fire_policy numeric(1,0),")
            .append("short_circuit boolean default false,")
            .append("host varchar(36) not null,")
            .append("folder varchar(36) not null,")
            .append("fire_order int default 0,")
            .append("enabled boolean default false,")
            .append("unique (name, host)")
            .append(");")

                    // create RULE_CONDITION table
            .append("create table rule_condition(")
            .append("id varchar(36) primary key,")
            .append("name varchar(255) not null,")
            .append("rule_id varchar(36) references dot_rule(id),")
            .append("conditionlet text not null,")
            .append("rel_operator varchar(20) not null,")
            .append("value text")
            .append(");")

                    // create RULE_ACTION_CLASS table
            .append("create table rule_action_class(")
            .append("id varchar(36) primary key,")
            .append("name varchar(255) not null,")
            .append("rule_id varchar(36) references dot_rule(id),")
            .append("fire_order int default 0,")
            .append("actionlet text not null")
            .append(");");


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
        System.out.println("-----MYSQL--------");
        System.out.println(MYSQL);
        System.out.println("-----POSTGRES--------");
        System.out.println(POSTGRES);
        System.out.println("-----ORACLE--------");
        System.out.println(ORACLE);
        System.out.println("-----MSSQL--------");
        System.out.println(MSSQL);
        System.out.println("-----H2--------");
        System.out.println(H2);
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
