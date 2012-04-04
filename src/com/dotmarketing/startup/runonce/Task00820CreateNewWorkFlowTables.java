package com.dotmarketing.startup.runonce;

import java.sql.SQLException;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;


public class Task00820CreateNewWorkFlowTables implements StartupTask {

    protected void createNewTablesOracle() throws DotDataException, SQLException {
    	DotConnect dc = new DotConnect();

    		dc.executeStatement("create table workflow_scheme( \n" +
    							" id varchar2(36) primary key, \n" +
    							" name varchar2(255) not null, \n" +
    							" description varchar2(255), \n" +
    							" archived number(1,0) default 0, \n" +
    							" mandatory number(1,0) default 0, \n" +
    							" default_scheme number(1,0) default 0, \n" +
    							" entry_action_id varchar2(36))");

    		dc.executeStatement("create table workflow_step( \n" +
    							" id varchar2(36) primary key, \n" +
    							" name varchar2(255) not null, \n" +
    							" scheme_id varchar2(36) not null references workflow_scheme(id), \n" +
    							" my_order number(10,0) default 0,\n" +
    							" resolved number(1,0) default 0)");

    		dc.executeStatement("create index wk_idx_step_scheme on workflow_step(scheme_id)");

    		dc.executeStatement("create table workflow_action(\n" +
    					        "id varchar2(36) primary key,\n" +
    					        "step_id varchar2(36) not null  references workflow_step(id),\n" +
    					        "name varchar2(255) not null,\n" +
    					        "condition_to_progress nclob,\n" +
    					        "next_step_id varchar2(36) not null references workflow_step(id),\n" +
    					        "next_assign varchar2(36) not null references cms_role(id),\n" +
    					        "my_order number(10,0) default 0,\n" +
    					        "assignable number(1,0) default 0,\n" +
    					        "commentable number(1,0) default 0,\n" +
    					        "requires_checkout number(1,0) default 0,\n" +
    					        "icon varchar2(255) default 'defaultWfIcon',\n" +
    					        "use_role_hierarchy_assign number(1,0) default 0)");

    		dc.executeStatement("create index wk_idx_act_step on workflow_action(step_id)");

            dc.executeStatement("create table workflow_action_class(\n" +
            					"id varchar2(36) primary key,\n" +
            					"action_id varchar2(36) not null references workflow_action(id),\n" +
            					"name varchar2(255) not null,\n" +
            					"my_order number(10,0) default 0,\n" +
            					"clazz nclob)");
    		dc.executeStatement("create index wk_idx_act_class_act on workflow_action_class(action_id)");

    		dc.executeStatement("create table workflow_action_class_pars(\n" +
    							"id varchar2(36) primary key,\n"+
    							"workflow_action_class_id varchar2(36) not null references workflow_action_class(id),\n" +
    							"key varchar2(255) not null,\n" +
    							"value nclob)");

    		dc.executeStatement("create index wk_idx_actclassparamact on\n" +
    			                 "workflow_action_class_pars(workflow_action_class_id)");


    		dc.executeStatement("create table workflow_scheme_x_structure(\n" +
    							"id varchar2(36) primary key,\n" +
    							"scheme_id varchar2(36) not null references workflow_scheme(id),\n" +
    							"structure_id varchar2(36) not null references structure(inode))");

    		dc.executeStatement("create unique index wk_idx_scheme_str_2 on\n" +
    							"workflow_scheme_x_structure(structure_id)");

    		dc.executeStatement("delete from workflow_history");
            dc.executeStatement("delete from workflow_comment");
            dc.executeStatement("delete from workflowtask_files");
            dc.executeStatement("delete from workflow_task");
            dc.executeStatement("alter table workflow_task add constraint FK_workflow_task_asset foreign key (webasset) references identifier(id)");
            dc.executeStatement("alter table workflow_task add constraint FK_workflow_assign foreign key (assigned_to) references cms_role(id)");
            dc.executeStatement("alter table workflow_task add constraint FK_workflow_step foreign key (status) references workflow_step(id)");
            dc.executeStatement("alter table workflow_scheme add constraint FK_wf_scheme_action foreign key (entry_action_id) references workflow_action(id)");

    		dc.executeStatement("ALTER TABLE workflow_history add  workflow_action_id varchar(36)");
    		dc.executeStatement("create index wf_histroy_action_idx on workflow_history(workflow_action_id)");
    		dc.executeStatement("ALTER TABLE workflow_history add  workflow_step_id varchar(36)");
    		dc.executeStatement("create index wf_histroy_step_idx on workflow_history(workflow_step_id)");


    }
    protected void createNewTablesSQLServer() throws DotDataException, SQLException {
    		DotConnect dc = new DotConnect();
    		dc.executeStatement("create table workflow_scheme(\n" +
    		    			"id varchar(36) primary key,\n" +
    		    			"name varchar(255) not null,\n" +
    		    			"description varchar(255),\n" +
    		    			"archived tinyint default 0,\n" +
    		    			"mandatory tinyint default 0,\n" +
    		    			"default_scheme tinyint default 0,\n" +
    		    			"entry_action_id varchar(36)\n" +
    		    			")");

    		dc.executeStatement("create table workflow_step(\n" +
    							"id varchar(36) primary key,\n" +
    							"name varchar(255) not null,\n" +
    							"scheme_id varchar(36) references workflow_scheme(id),\n" +
    							"my_order int default 0,\n" +
    		       		        "resolved tinyint default 0\n" +
    		       		        ")");
    		dc.executeStatement("create index workflow_idx_step_scheme on workflow_step(scheme_id)");

            dc.executeStatement("create table workflow_action(\n" +
            					"id varchar(36) primary key,\n" +
            					"step_id varchar(36) not null  references workflow_step(id),\n" +
            					"name varchar(255) not null,\n" +
            					"condition_to_progress text,\n" +
            					"next_step_id varchar(36) not null references workflow_step(id),\n" +
            					"next_assign varchar(36) not null references cms_role(id),\n" +
            					"my_order int default 0,\n" +
            					"assignable tinyint default 0,\n" +
            					"commentable tinyint default 0,\n" +
            					"requires_checkout tinyint default 0,\n" +
            					"icon varchar(255) default 'defaultWfIcon',\n" +
            					"use_role_hierarchy_assign tinyint default 0\n" +
    							")");
    		dc.executeStatement("create index workflow_idx_action_step on workflow_action(step_id)");

    		dc.executeStatement("create table workflow_action_class(\n" +
    							"id varchar(36) primary key,\n" +
    							"action_id varchar(36) references workflow_action(id),\n" +
    							"name varchar(255) not null,\n" +
    							"my_order int default 0,\n" +
    							"clazz text\n" +
    							")");
    		dc.executeStatement("create index workflow_idx_action_class_action on workflow_action_class(action_id)");


    		dc.executeStatement("create table workflow_action_class_pars(" +
            					"id varchar(36) primary key,\n" +
            					"workflow_action_class_id varchar(36) not null references workflow_action_class(id),\n" +
            					"\"key\" varchar(255) not null,\n" +
            					"value text)");

    		dc.executeStatement("create index workflow_idx_action_class_param_action on \n" +
    							" workflow_action_class_pars(workflow_action_class_id)");


    		dc.executeStatement("create table workflow_scheme_x_structure(\n" +
    							"id varchar(36) primary key,\n" +
    							"scheme_id varchar(36) references workflow_scheme(id),\n" +
    							"structure_id varchar(36) references structure(inode))");

    		dc.executeStatement("create index workflow_idx_scheme_structure_1 on \n" +
    							" workflow_scheme_x_structure(structure_id)");

    		dc.executeStatement("create unique index workflow_idx_scheme_structure_2 on \n" +
    							" workflow_scheme_x_structure(structure_id)");

    		dc.executeStatement("delete from workflow_history");
            dc.executeStatement("delete from workflow_comment");
            dc.executeStatement("delete from workflowtask_files");
            dc.executeStatement("delete from workflow_task");
            dc.executeStatement("alter table workflow_task add constraint FK_workflow_task_asset foreign key (webasset) references identifier(id)");
            dc.executeStatement("ALTER TABLE workflow_task ALTER COLUMN status varchar(36)");
            dc.executeStatement("ALTER TABLE workflow_task ALTER COLUMN assigned_to varchar(36)");
            dc.executeStatement("alter table workflow_task add constraint FK_workflow_assign foreign key (assigned_to) references cms_role(id)");
            dc.executeStatement("alter table workflow_task add constraint FK_workflow_step foreign key (status) references workflow_step(id)");
            dc.executeStatement("alter table workflow_scheme add constraint FK_wf_scheme_action foreign key (entry_action_id) references workflow_action(id)");

    		dc.executeStatement("ALTER TABLE workflow_history add  workflow_action_id varchar(36)");
    		dc.executeStatement("create index wf_histroy_action_idx on workflow_history(workflow_action_id)");
    		dc.executeStatement("ALTER TABLE workflow_history add  workflow_step_id varchar(36)");
    		dc.executeStatement("create index wf_histroy_step_idx on workflow_history(workflow_step_id)");

    }
    protected void createNewTablesPostgres() throws DotDataException, SQLException {
        DotConnect dc = new DotConnect();
        String createTableSufix=";";

        dc.executeStatement("create table workflow_scheme(" +
        		"id varchar(36) primary key," +
        		"name varchar(255) not null," +
        		"description varchar(255)," +
        		"archived boolean default false," +
        		"mandatory boolean default false," +
        		"default_scheme boolean default false," +
        		"entry_action_id varchar(36))"+createTableSufix);


        dc.executeStatement("create table workflow_step(" +
        		"id varchar(36) primary key," +
        		"name varchar(255) not null," +
        		"scheme_id varchar(36) not null references workflow_scheme(id)," +
        		"my_order int default 0, " +
        		"resolved boolean default false)"+createTableSufix
        	);

        dc.executeStatement("create index wf_idx_step_scheme on workflow_step(scheme_id)");

        dc.executeStatement("create table workflow_action(" +
        		"id varchar(36) primary key," +
        		"step_id varchar(36) not null  references workflow_step(id)," +
        		"name varchar(255) not null," +
        		"condition_to_progress text," +
        		"next_step_id varchar(36) not null references workflow_step(id)," +
        		"next_assign varchar(36) not null references cms_role(id)," +
        		"my_order int default 0," +
        		"assignable boolean default false," +
        		"commentable boolean default false,"+
        		"requires_checkout boolean default false,"+
        		"icon varchar(255) default 'defaultWfIcon',"+
    			"use_role_hierarchy_assign bool default false)"+createTableSufix
        );

        dc.executeStatement("create index wf_idx_act_step on workflow_action(step_id);");

        dc.executeStatement(
				"create table workflow_action_class(" +
				"id varchar(36) primary key," +
				"action_id varchar(36) not null  references workflow_action(id)," +
				"name varchar(255) not null," +
				"my_order int default 0," +
				"clazz text" +
				")"+createTableSufix);

        dc.executeStatement("create index wf_idx_act_class_act on workflow_action_class(action_id);");

        if(DbConnectionFactory.isMySql()){
        	dc.executeStatement("SET sql_mode='ANSI_QUOTES';");
        	dc.executeStatement("create table workflow_action_class_pars(" +
            		"id varchar(36) primary key," +
    				"workflow_action_class_id varchar(36) not null  references workflow_action_class(id)," +
            		"\"key\" varchar(255) not null," +
            		"value text)"+createTableSufix);
        }else{
        	dc.executeStatement("create table workflow_action_class_pars(" +
            		"id varchar(36) primary key," +
            		"key varchar(255) not null," +
    				"workflow_action_class_id varchar(36) not null  references workflow_action_class(id)," +
            		"value text)"+createTableSufix);
        }
        dc.executeStatement("create index wf_idx_action_class_param_action on workflow_action_class_pars(id);");

        dc.executeStatement("create table workflow_scheme_x_structure(" +
		        "id varchar(36) primary key," +
		        "scheme_id varchar(36)  not null references workflow_scheme(id)," +
		        "structure_id varchar(36) not null references structure(inode))"+createTableSufix);

        dc.executeStatement("create index wf_idx_scheme_structure_1 on " +
        		"workflow_scheme_x_structure(structure_id);");

        dc.executeStatement("create unique index workflow_idx_scheme_structure_2 on " +
        		"workflow_scheme_x_structure(structure_id);");

        dc.executeStatement("delete from workflow_history;  ");
        dc.executeStatement("delete from workflow_comment;  ");
        dc.executeStatement("delete from workflowtask_files;  ");
        dc.executeStatement("delete from workflow_task; ");
        dc.executeStatement("alter table workflow_task add constraint FK_workflow_task_asset foreign key (webasset) references identifier(id)");
        dc.executeStatement("ALTER TABLE workflow_task ALTER COLUMN status TYPE varchar(36)");
        dc.executeStatement("ALTER TABLE workflow_task ALTER COLUMN assigned_to TYPE varchar(36)");
        dc.executeStatement("alter table workflow_task add constraint FK_workflow_assign foreign key (assigned_to) references cms_role(id)");
        dc.executeStatement("alter table workflow_task add constraint FK_workflow_step foreign key (status) references workflow_step(id)");
        dc.executeStatement("alter table workflow_scheme add constraint FK_wf_scheme_action foreign key (entry_action_id) references workflow_action(id)");

		dc.executeStatement("ALTER TABLE workflow_history add  workflow_action_id varchar(36)");
		dc.executeStatement("create index wf_histroy_action_idx on workflow_history(workflow_action_id)");
		dc.executeStatement("ALTER TABLE workflow_history add  workflow_step_id varchar(36)");
		dc.executeStatement("create index wf_histroy_step_idx on workflow_history(workflow_step_id)");

    }

    protected void createNewTablesMySQL() throws DotDataException, SQLException {
        DotConnect dc = new DotConnect();
        String createTableSufix=";";
        if(DbConnectionFactory.isMySql())
            createTableSufix=" ENGINE=INNODB;";

        dc.executeStatement("create table workflow_scheme(" +
        		"id varchar(36) primary key," +
        		"name varchar(255) not null," +
        		"description varchar(255)," +
        		"archived boolean default false," +
        		"mandatory boolean default false," +
        		"default_scheme boolean default false," +
        		"entry_action_id varchar(36))"+createTableSufix);


        dc.executeStatement("create table workflow_step(" +
        		"id varchar(36) primary key," +
        		"name varchar(255) not null," +
        		"scheme_id varchar(36) not null references workflow_scheme(id)," +
        		"my_order int default 0, " +
        		"resolved boolean default false)"+createTableSufix
        	);

        dc.executeStatement("create index wf_idx_step_scheme on workflow_step(scheme_id)");

        dc.executeStatement("create table workflow_action(" +
        		"id varchar(36) primary key," +
        		"step_id varchar(36) not null  references workflow_step(id)," +
        		"name varchar(255) not null," +
        		"condition_to_progress text," +
        		"next_step_id varchar(36) not null references workflow_step(id)," +
        		"next_assign varchar(36) not null references cms_role(id)," +
        		"my_order int default 0," +
        		"assignable boolean default false," +
        		"commentable boolean default false,"+
        		"requires_checkout boolean default false,"+
        		"icon varchar(255) default 'defaultWfIcon',"+
    			"use_role_hierarchy_assign bool default false)"+createTableSufix
        );

        dc.executeStatement("create index wf_idx_act_step on workflow_action(step_id);");

        dc.executeStatement(
				"create table workflow_action_class(" +
				"id varchar(36) primary key," +
				"action_id varchar(36) not null  references workflow_action(id)," +
				"name varchar(255) not null," +
				"my_order int default 0," +
				"clazz text" +
				")"+createTableSufix);

        dc.executeStatement("create index wf_idx_act_class_act on workflow_action_class(action_id);");

        if(DbConnectionFactory.isMySql()){
        	dc.executeStatement("SET sql_mode='ANSI_QUOTES';");
        	dc.executeStatement("create table workflow_action_class_pars(" +
            		"id varchar(36) primary key," +
    				"workflow_action_class_id varchar(36) not null  references workflow_action_class(id)," +
            		"\"key\" varchar(255) not null," +
            		"value text)"+createTableSufix);
        }else{
        	dc.executeStatement("create table workflow_action_class_pars(" +
            		"id varchar(36) primary key," +
            		"key varchar(255) not null," +
    				"workflow_action_class_id varchar(36) not null  references workflow_action_class(id)," +
            		"value text)"+createTableSufix);
        }
        dc.executeStatement("create index wf_idx_action_class_param_action on workflow_action_class_pars(id);");

        dc.executeStatement("create table workflow_scheme_x_structure(" +
		        "id varchar(36) primary key," +
		        "scheme_id varchar(36)  not null references workflow_scheme(id)," +
		        "structure_id varchar(36) not null references structure(inode))"+createTableSufix);

        dc.executeStatement("create index wf_idx_scheme_structure_1 on " +
        		"workflow_scheme_x_structure(structure_id);");

        dc.executeStatement("create unique index workflow_idx_scheme_structure_2 on " +
        		"workflow_scheme_x_structure(structure_id);");

        dc.executeStatement("delete from workflow_history;  ");
        dc.executeStatement("delete from workflow_comment;  ");
        dc.executeStatement("delete from workflowtask_files;  ");
        dc.executeStatement("delete from workflow_task; ");
        dc.executeStatement("alter table workflow_task add constraint FK_workflow_task_asset foreign key (webasset) references identifier(id)");
        dc.executeStatement("ALTER TABLE workflow_task MODIFY status varchar(36)");
        dc.executeStatement("ALTER TABLE workflow_task MODIFY assigned_to varchar(36)");
        dc.executeStatement("alter table workflow_task add constraint FK_workflow_assign foreign key (assigned_to) references cms_role(id)");
        dc.executeStatement("alter table workflow_task add constraint FK_workflow_step foreign key (status) references workflow_step(id)");
        dc.executeStatement("alter table workflow_scheme add constraint FK_wf_scheme_action foreign key (entry_action_id) references workflow_action(id)");

		dc.executeStatement("ALTER TABLE workflow_history add  workflow_action_id varchar(36)");
		dc.executeStatement("create index wf_histroy_action_idx on workflow_history(workflow_action_id)");
		dc.executeStatement("ALTER TABLE workflow_history add  workflow_step_id varchar(36)");
		dc.executeStatement("create index wf_histroy_step_idx on workflow_history(workflow_step_id)");

    }

    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);

            if(DbConnectionFactory.isOracle()){
            	createNewTablesOracle();
            }else if(DbConnectionFactory.isMsSql()){
            	createNewTablesSQLServer();
            }else if(DbConnectionFactory.isPostgres()){
            	createNewTablesPostgres();
            }
            else{
            	createNewTablesMySQL();
            }

            // I know I should not call an API
            // but this is better than having people get an error
            // when they try to edit content
            APILocator.getWorkflowAPI().createDefaultScheme();

        } catch (Exception e) {
            throw new DotDataException(e.getMessage(),e);
        }
    }

    public boolean forceRun() {
        return true;
    }

}
