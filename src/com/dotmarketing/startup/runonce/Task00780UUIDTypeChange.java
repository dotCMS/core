package com.dotmarketing.startup.runonce;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;

public class Task00780UUIDTypeChange extends AbstractJDBCStartupTask{

	@Override
	public String getMSSQLScript() {
		return "ALTER TABLE inode ALTER COLUMN inode varchar(36) NOT NULL;" +
		       "ALTER TABLE inode ADD CONSTRAINT pk_inode PRIMARY KEY (inode);" +
		       "ALTER TABLE inode ALTER COLUMN identifier varchar(36);" +
		       "ALTER TABLE identifier ALTER COLUMN inode varchar(36) NOT NULL;" +
		       "ALTER TABLE identifier ADD CONSTRAINT pk_identifier PRIMARY KEY (inode);" +
		       "ALTER TABLE identifier ALTER COLUMN host_inode varchar(36);" +
		       "ALTER TABLE folder ALTER COLUMN inode varchar(36) NOT NULL;" +
		       "ALTER TABLE folder ADD CONSTRAINT pk_folder PRIMARY KEY (inode);" +
		       "ALTER TABLE contentlet ALTER COLUMN inode varchar(36) NOT NULL;" +
		       "ALTER TABLE contentlet ADD CONSTRAINT pk_contentlet PRIMARY KEY (inode);" +
		       "ALTER TABLE contentlet ALTER COLUMN folder varchar(36);" +
		       "ALTER TABLE contentlet ALTER COLUMN structure_inode varchar(36);" +
		       "ALTER TABLE containers ALTER COLUMN inode varchar(36) NOT NULL;" +
		       "ALTER TABLE containers ADD CONSTRAINT pk_containers PRIMARY KEY (inode);" +
		       "ALTER TABLE template ALTER COLUMN inode varchar(36) NOT NULL;" +
		       "ALTER TABLE template ADD CONSTRAINT pk_template PRIMARY KEY (inode);" +
		       "ALTER TABLE template ALTER COLUMN image varchar(36);" +
		       "ALTER TABLE htmlpage ALTER COLUMN inode varchar(36) NOT NULL;" +
		       "ALTER TABLE htmlpage ADD CONSTRAINT pk_htmlpage PRIMARY KEY (inode);" +
		       "ALTER TABLE file_asset ALTER COLUMN inode varchar(36) NOT NULL;" +
		       "ALTER TABLE file_asset ADD CONSTRAINT pk_file_asset PRIMARY KEY (inode);" +
		       "ALTER TABLE links ALTER COLUMN inode varchar(36) NOT NULL;" +
		       "ALTER TABLE links ADD CONSTRAINT pk_links PRIMARY KEY (inode);" +
		       "ALTER TABLE links ALTER COLUMN internal_link_identifier varchar(36);" +
		       "ALTER TABLE structure ALTER COLUMN inode varchar(36) NOT NULL;" +
		       "ALTER TABLE structure ADD CONSTRAINT pk_structure PRIMARY KEY (inode);" +
		       "ALTER TABLE structure ALTER COLUMN host varchar(36);" +
		       "ALTER TABLE structure ALTER COLUMN folder varchar(36);" +
		       "ALTER TABLE structure ALTER COLUMN page_detail varchar(36);" +
		       "ALTER TABLE category ALTER COLUMN inode varchar(36) NOT NULL;" +
		       "ALTER TABLE category ADD CONSTRAINT pk_category PRIMARY KEY (inode);" +
		       "ALTER TABLE workflow_task ALTER COLUMN inode varchar(36) NOT NULL;" +
		       "ALTER TABLE workflow_task ADD CONSTRAINT pk_workflow_task PRIMARY KEY (inode);" +
		       "ALTER TABLE workflow_task ALTER COLUMN webasset varchar(36);" +
		       "ALTER TABLE workflow_comment ALTER COLUMN inode varchar(36) NOT NULL;" +
		       "ALTER TABLE workflow_comment ADD CONSTRAINT pk_workflow_comment PRIMARY KEY (inode);" +
		       "ALTER TABLE workflow_history ALTER COLUMN inode varchar(36) NOT NULL;" +
		       "ALTER TABLE workflow_history ADD CONSTRAINT pk_workflow_history PRIMARY KEY (inode);" +
		       "ALTER TABLE mailing_list ALTER COLUMN inode varchar(36) NOT NULL;" +
		       "ALTER TABLE mailing_list ADD CONSTRAINT pk_mailing_list PRIMARY KEY (inode);" +
		       "ALTER TABLE campaign ALTER COLUMN inode varchar(36) NOT NULL;" +
		       "ALTER TABLE campaign ADD CONSTRAINT pk_campaign PRIMARY KEY (inode);" +
		       "ALTER TABLE campaign ALTER COLUMN communicationinode varchar(36);" +
		       "ALTER TABLE campaign ALTER COLUMN userfilterinode varchar(36);" +
		       "ALTER TABLE campaign ALTER COLUMN parent_campaign varchar(36);" +
		       "ALTER TABLE recipient ALTER COLUMN inode varchar(36) NOT NULL;" +
		       "ALTER TABLE recipient ADD CONSTRAINT pk_recipient PRIMARY KEY (inode);" +
		       "ALTER TABLE click ALTER COLUMN inode varchar(36) NOT NULL;" +
		       "ALTER TABLE click ADD CONSTRAINT pk_click PRIMARY KEY (inode);" +
		       "ALTER TABLE communication ALTER COLUMN inode varchar(36) NOT NULL;" +
		       "ALTER TABLE communication ADD CONSTRAINT pk_communication PRIMARY KEY (inode);" +
		       "ALTER TABLE communication ALTER COLUMN trackback_link_inode varchar(36);" +
		       "ALTER TABLE communication ALTER COLUMN html_page_inode varchar(36);" +
		       "ALTER TABLE user_proxy ALTER COLUMN inode varchar(36) NOT NULL;" +
		       "ALTER TABLE user_proxy ADD CONSTRAINT pk_user_proxy PRIMARY KEY (inode);" +
		       "ALTER TABLE virtual_link ALTER COLUMN inode varchar(36) NOT NULL;" +
		       "ALTER TABLE virtual_link ADD CONSTRAINT pk_virtual_link PRIMARY KEY (inode);" +
		       "ALTER TABLE relationship ALTER COLUMN inode varchar(36) NOT NULL;" +
		       "ALTER TABLE relationship ADD CONSTRAINT pk_relationship PRIMARY KEY (inode);" +
		       "ALTER TABLE relationship ALTER COLUMN parent_structure_inode varchar(36);" +
		       "ALTER TABLE relationship ALTER COLUMN child_structure_inode varchar(36);" +
		       "ALTER TABLE field ALTER COLUMN inode varchar(36) NOT NULL;" +
		       "ALTER TABLE field ADD CONSTRAINT pk_field PRIMARY KEY (inode);" +
		       "ALTER TABLE field ALTER COLUMN structure_inode varchar(36);" +
		       "ALTER TABLE user_comments ALTER COLUMN inode varchar(36) NOT NULL;" +
		       "ALTER TABLE user_comments ADD CONSTRAINT pk_user_comments PRIMARY KEY (inode);" +
		       "ALTER TABLE user_comments ALTER COLUMN communication_id varchar(36);" +
		       "ALTER TABLE user_filter ALTER COLUMN inode varchar(36) NOT NULL;" +
		       "ALTER TABLE user_filter ADD CONSTRAINT pk_user_filter PRIMARY KEY (inode);" +
		       "ALTER TABLE report_asset ALTER COLUMN inode varchar(36) NOT NULL;" +
		       "ALTER TABLE report_asset ADD CONSTRAINT pk_report_asset PRIMARY KEY (inode);" +
		       "ALTER TABLE report_parameter ALTER COLUMN inode varchar(36) NOT NULL;" +
		       "ALTER TABLE report_parameter ADD CONSTRAINT pk_report_parameter PRIMARY KEY (inode);" +
		       "ALTER TABLE report_parameter ALTER COLUMN report_inode varchar(36);" +
		       "ALTER TABLE cms_role ALTER COLUMN id varchar(36) NOT NULL;" +
		       "ALTER TABLE cms_role ALTER COLUMN parent varchar(36);" +
		       "ALTER TABLE users_cms_roles ALTER COLUMN id varchar(36) NOT NULL;" +
		       "ALTER TABLE users_cms_roles ALTER COLUMN role_id varchar(36);" +
		       "ALTER TABLE layouts_cms_roles ALTER COLUMN id varchar(36) NOT NULL;" +
		       "ALTER TABLE layouts_cms_roles ALTER COLUMN layout_id varchar(36);" +
		       "ALTER TABLE layouts_cms_roles ALTER COLUMN role_id varchar(36);" +
		       "ALTER TABLE cms_layout ALTER COLUMN id varchar(36) NOT NULL;" +
		       "ALTER TABLE cms_layouts_portlets ALTER COLUMN id varchar(36) NOT NULL;" +
		       "ALTER TABLE cms_layouts_portlets ALTER COLUMN layout_id varchar(36);" +
		       "ALTER TABLE host_variable ALTER COLUMN id varchar(36) NOT NULL;" +
		       "ALTER TABLE fixes_audit ALTER COLUMN id varchar(36) NOT NULL;" +
		       "ALTER TABLE host_variable ALTER COLUMN host_id varchar(36);" +
		       "ALTER TABLE tree ALTER COLUMN child varchar(36) NOT NULL;" +
		       "ALTER TABLE tree ALTER COLUMN parent varchar(36) NOT NULL;" +
		       "ALTER TABLE multi_tree ALTER COLUMN child varchar(36) NOT NULL;" +
		       "ALTER TABLE multi_tree ALTER COLUMN parent1 varchar(36) NOT NULL;" +
		       "ALTER TABLE multi_tree ALTER COLUMN parent2 varchar(36) NOT NULL;" +
		       "ALTER TABLE permission ALTER COLUMN roleid varchar(36);" +
		       "ALTER TABLE permission ALTER COLUMN inode_id varchar(36);" +
		       "ALTER TABLE permission_reference ALTER COLUMN asset_id varchar(36);" +
		       "ALTER TABLE permission_reference ALTER COLUMN reference_id varchar(36);" +
		       "ALTER TABLE content_rating ALTER COLUMN identifier varchar(36);" +
		       "ALTER TABLE tag_inode ALTER COLUMN inode varchar(36) NOT NULL;" +
		       "ALTER TABLE trackback ALTER COLUMN asset_identifier varchar(36);" +
		       "ALTER TABLE web_form ALTER COLUMN user_inode varchar(36);" +
		       "ALTER TABLE web_form ALTER COLUMN web_form_id varchar(36) NOT NULL;" +
		       "ALTER TABLE calendar_reminder ALTER COLUMN event_id varchar(36) NOT NULL;" +
		       "ALTER TABLE dist_reindex_journal ALTER COLUMN inode_to_index varchar(36) NOT NULL;" +
		       "ALTER TABLE dist_reindex_journal ALTER COLUMN ident_to_index varchar(36) NOT NULL;" +
		       
		       "ALTER TABLE analytic_summary ALTER COLUMN host_id varchar(36) NOT NULL;" +
		       "ALTER TABLE analytic_summary_404 ALTER COLUMN host_id varchar(36);" +
		       "ALTER TABLE analytic_summary_visits ALTER COLUMN host_id varchar(36);" +
		       "ALTER TABLE clickstream ALTER COLUMN host_id varchar(36);" +
		       "ALTER TABLE clickstream_request ALTER COLUMN host_id varchar(36);" +
		       "ALTER TABLE clickstream_404 ALTER COLUMN host_id varchar(36);" +
		       "ALTER TABLE analytic_summary_workstream ALTER COLUMN host_id varchar(36);" +
		       "ALTER TABLE analytic_summary_content ALTER COLUMN inode varchar(36);" +
		       "ALTER TABLE analytic_summary_pages ALTER COLUMN inode varchar(36);" +
		       "ALTER TABLE analytic_summary_workstream ALTER COLUMN inode varchar(36);" +
		       "ALTER TABLE clickstream_request ALTER COLUMN associated_identifier varchar(36);" +
		       
		       "ALTER TABLE cms_role ADD CONSTRAINT pk_cms_role PRIMARY KEY (id);" +
		       "ALTER TABLE users_cms_roles ADD CONSTRAINT pk_users_cms_roles PRIMARY KEY (id);" +
		       "ALTER TABLE layouts_cms_roles ADD CONSTRAINT pk_layouts_cms_roles PRIMARY KEY (id);" +
		       "ALTER TABLE cms_layout ADD CONSTRAINT pk_cms_layout PRIMARY KEY (id);" +
		       "ALTER TABLE cms_layouts_portlets ADD CONSTRAINT pk_cms_layouts_portlets PRIMARY KEY (id);" +
		       "ALTER TABLE host_variable ADD CONSTRAINT pk_host_variable PRIMARY KEY (id);" +
		       "ALTER TABLE fixes_audit ADD CONSTRAINT pk_fixes_audit PRIMARY KEY (id);" +
		       "ALTER TABLE content_rating ADD CONSTRAINT pk_content_rating PRIMARY KEY (id);" +
		       "ALTER TABLE web_form ADD CONSTRAINT pk_web_form_id PRIMARY KEY(web_form_id);" +
		       "ALTER TABLE trackback ADD CONSTRAINT pk_trackback PRIMARY KEY (id);" +
		       "ALTER TABLE dist_reindex_journal ADD CONSTRAINT pk_reindex_journal PRIMARY KEY(id);" +
		       "ALTER TABLE tree ADD CONSTRAINT pk_tree PRIMARY KEY (child, parent, relation_type);" +
		       "ALTER TABLE multi_tree ADD CONSTRAINT pk_multi_tree PRIMARY KEY (child, parent1, parent2);" +
		       "ALTER TABLE permission ADD CONSTRAINT pk_permission PRIMARY KEY (id);" +
		       "ALTER TABLE permission_reference ADD CONSTRAINT pk_permission_reference PRIMARY KEY (id);" +
		       "ALTER TABLE calendar_reminder ADD CONSTRAINT pk_calendar_reminder PRIMARY KEY (user_id, event_id, send_date);" +
		       "ALTER TABLE tag_inode ADD CONSTRAINT pk_tag_inode PRIMARY KEY (tag_id, inode);" +
		       "Create index idx_identifier ON identifier (inode);" + 
			   "Create index tag_inode_inode ON tag_inode (inode);" +
			   "Create index idx_workflow_4 ON workflow_task (webasset);" +
			   "Create index dist_reindex_index1 ON dist_reindex_journal (inode_to_index);" +
			   "Create index dist_reindex_index4 ON dist_reindex_journal (ident_to_index,serverid);" +
			   "Create index idx_permission_reference_2 ON permission_reference (reference_id);" +
			   "Create index idx_permission_reference_3 ON permission_reference (reference_id,permission_type);" +
			   "Create index idx_permission_reference_4 ON permission_reference (asset_id,permission_type);" +
			   "Create index idx_permission_reference_5 ON permission_reference (asset_id,reference_id,permission_type);" +
			   "alter table structure add CONSTRAINT [DF_structure_host] DEFAULT 'SYSTEM_HOST' for host;" +
			   "alter table structure add CONSTRAINT [DF_structure_folder] DEFAULT 'SYSTEM_FOLDER' for folder;" +
			   "alter table structure add CONSTRAINT [CK_structure_host] CHECK(host <> '' AND host IS NOT NULL);" +
			   "Create index idx_trackback_1 ON trackback (asset_identifier);" +
			   "Create index idx_relationship_1 ON relationship (parent_structure_inode);" +
			   "Create index idx_relationship_2 ON relationship (child_structure_inode);" +
               "Create index idx_field_1 ON field (structure_inode);" +
			   "Create unique index IDX_FIELD_VELOCITY_STRUCTURE ON field (velocity_var_name,structure_inode);" +
			   "Create index idx_tree ON tree (child, parent, relation_type);" +
			   "Create index idx_tree_1 ON tree (parent);" +
			   "Create index idx_tree_2 ON tree (child);" +
			   "Create index idx_tree_4 ON tree (parent, child, relation_type);" +
			   "Create index idx_tree_5 ON tree (parent, relation_type);" +
			   "Create index idx_tree_6 ON tree (child, relation_type);" +
			   "Create index idx_index_2 ON inode (identifier);" +
			   "Create index idx_permission_2 ON permission (permission_type, inode_id);" +
			   "Create index idx_permission_3 ON permission (roleid);" +
			   "Create index idx_contentlet_1 ON contentlet (inode, live);" +
			   "Create index idx_contentlet_2 ON contentlet (inode, working);" +
			   "Create index idx_contentlet_3 ON contentlet (inode);" +
			   "Create index idx_contentlet_4 ON contentlet (structure_inode);" +
			   "alter table structure add constraint unique_struct_vel_var_name unique (velocity_var_name);" +
			   "ALTER TABLE identifier add constraint identifier_inode_key unique (uri, host_inode);" +
			   "ALTER TABLE user_proxy ADD CONSTRAINT user_id_key UNIQUE (user_id);" +
			   "ALTER TABLE report_parameter ADD CONSTRAINT report_param_key UNIQUE (report_inode, parameter_name);" +
			   "ALTER TABLE cms_role ADD CONSTRAINT IX_cms_role2 UNIQUE NONCLUSTERED (db_fqn);" +
			   "ALTER TABLE users_cms_roles ADD CONSTRAINT IX_cms_role UNIQUE NONCLUSTERED (role_id, user_id);" +
			   "ALTER TABLE cms_layout ADD CONSTRAINT IX_cms_layout UNIQUE NONCLUSTERED (layout_name);" +
			   "ALTER TABLE layouts_cms_roles ADD CONSTRAINT IX_layouts_cms_roles UNIQUE NONCLUSTERED (role_id, layout_id);" +
			   "ALTER TABLE cms_layouts_portlets ADD CONSTRAINT IX_cms_layouts_portlets UNIQUE NONCLUSTERED (portlet_id, layout_id);" + 
			   "ALTER TABLE permission ADD CONSTRAINT permission_inode_id_key UNIQUE (permission_type,inode_id, roleid);" + 
			   "ALTER TABLE permission_reference ADD CONSTRAINT asset_id_unique_key UNIQUE(asset_id);" +
			   "ALTER TABLE analytic_summary ADD CONSTRAINT pk_analytic_summary PRIMARY KEY (id);" +
			   "ALTER TABLE analytic_summary ADD CONSTRAINT analytic_summary_key UNIQUE (summary_period_id, host_id);" +
		       "create index idx_analytic_summary_1 on analytic_summary (host_id);" +
		       "create index idx_analytic_summary_404_1 on analytic_summary_404 (host_id);" +
		       "create index idx_analytic_summary_visits_1 on analytic_summary_visits (host_id);" +
		       "create index idx_user_clickstream11 on clickstream (host_id);" +
		       "create index idx_dashboard_workstream_2 on analytic_summary_workstream (host_id);" +
		       "create index idx_user_clickstream_404_3 on clickstream_404 (host_id);" +
		       "create index idx_user_clickstream_request_3 on clickstream_request (associated_identifier);" +
		       "alter table analytic_summary_pages add constraint fka1ad33b9ed30e054 foreign key (summary_id) references analytic_summary;" +
		       "alter table analytic_summary_content add constraint fk53cb4f2eed30e054 foreign key (summary_id) references analytic_summary;" +
		       "alter table analytic_summary_referer add constraint fk5bc0f3e2ed30e054 foreign key (summary_id) references analytic_summary;"; 
	}

	@Override
	public String getMySQLScript() {
		return "ALTER TABLE inode MODIFY inode varchar(36);" +
			   "ALTER TABLE inode MODIFY identifier varchar(36);" +
		       "ALTER TABLE identifier MODIFY inode varchar(36);" +
	           "ALTER TABLE identifier MODIFY host_inode varchar(36);" +
		       "ALTER TABLE folder MODIFY inode varchar(36);" +
		       "ALTER TABLE contentlet MODIFY inode varchar(36);" +
		       "ALTER TABLE contentlet MODIFY folder varchar(36);" +
		       "ALTER TABLE contentlet MODIFY structure_inode varchar(36);" +
		       "ALTER TABLE containers MODIFY inode varchar(36);" +
		       "ALTER TABLE template MODIFY inode varchar(36);" +
		       "ALTER TABLE template MODIFY image varchar(36);" +
		       "ALTER TABLE htmlpage MODIFY inode varchar(36);" +
		       "ALTER TABLE file_asset MODIFY inode varchar(36);" +
		       "ALTER TABLE links MODIFY inode varchar(36);" +
		       "ALTER TABLE links MODIFY internal_link_identifier varchar(36);" +
		       "ALTER TABLE structure MODIFY inode varchar(36);" +
		       "ALTER TABLE structure MODIFY host varchar(36);" +
		       "ALTER TABLE structure MODIFY folder varchar(36);" +
		       "ALTER TABLE structure MODIFY page_detail varchar(36);" +
		       "ALTER TABLE category MODIFY inode varchar(36);" +
		       "ALTER TABLE workflow_task MODIFY inode varchar(36);" +
		       "ALTER TABLE workflow_task MODIFY webasset varchar(36);" +
		       "ALTER TABLE workflow_comment MODIFY inode varchar(36);" +
		       "ALTER TABLE workflow_history MODIFY inode varchar(36);" +
		       "ALTER TABLE mailing_list MODIFY inode varchar(36);" +
		       "ALTER TABLE campaign MODIFY inode varchar(36);" +
		       "ALTER TABLE campaign MODIFY communicationinode varchar(36);" +
		       "ALTER TABLE campaign MODIFY userfilterinode varchar(36);" +
		       "ALTER TABLE campaign MODIFY parent_campaign varchar(36);" +
		       "ALTER TABLE recipient MODIFY inode varchar(36);" +
		       "ALTER TABLE click MODIFY inode varchar(36);" +
		       "ALTER TABLE communication MODIFY inode varchar(36);" +
		       "ALTER TABLE communication MODIFY trackback_link_inode varchar(36);" +
		       "ALTER TABLE communication MODIFY html_page_inode varchar(36);" +
		       "ALTER TABLE user_proxy MODIFY inode varchar(36);" +
		       "ALTER TABLE virtual_link MODIFY inode varchar(36);" +
		       "ALTER TABLE relationship MODIFY inode varchar(36);" +
		       "ALTER TABLE relationship MODIFY parent_structure_inode varchar(36);" +
		       "ALTER TABLE relationship MODIFY child_structure_inode varchar(36);" +
		       "ALTER TABLE field MODIFY inode varchar(36);" +
		       "ALTER TABLE field MODIFY structure_inode varchar(36);" +
		       "ALTER TABLE user_comments MODIFY inode varchar(36);" +
		       "ALTER TABLE user_comments MODIFY communication_id varchar(36);" +
		       "ALTER TABLE user_filter MODIFY inode varchar(36);" +
		       "ALTER TABLE report_asset MODIFY inode varchar(36);" +
		       "ALTER TABLE report_parameter MODIFY inode varchar(36);" +
		       "ALTER TABLE report_parameter MODIFY report_inode varchar(36);" +     
		       "ALTER TABLE cms_role MODIFY id varchar(36);" +
		       "ALTER TABLE users_cms_roles MODIFY id varchar(36);" +
		       "ALTER TABLE layouts_cms_roles MODIFY id varchar(36);" +
		       "ALTER TABLE cms_layout MODIFY id varchar(36);" +
		       "ALTER TABLE cms_layouts_portlets MODIFY id varchar(36);" +
		       "ALTER TABLE host_variable MODIFY id varchar(36);" +
		       "ALTER TABLE fixes_audit MODIFY id varchar(36);" +
		       "ALTER TABLE cms_role MODIFY parent varchar(36);" +
		       "ALTER TABLE users_cms_roles MODIFY role_id varchar(36);" +
		       "ALTER TABLE layouts_cms_roles MODIFY layout_id varchar(36);" +
		       "ALTER TABLE layouts_cms_roles MODIFY role_id varchar(36);" +
		       "ALTER TABLE cms_layouts_portlets MODIFY layout_id varchar(36);" +
		       "ALTER TABLE host_variable MODIFY host_id varchar(36);" +
		       "ALTER TABLE tree MODIFY child varchar(36);" +
		       "ALTER TABLE tree MODIFY parent varchar(36);" +
		       "ALTER TABLE multi_tree MODIFY parent1 varchar(36);" +
		       "ALTER TABLE multi_tree MODIFY parent2 varchar(36);" +
		       "ALTER TABLE multi_tree MODIFY child varchar(36);" +
		       "ALTER TABLE permission MODIFY roleid varchar(36);" +
		       "ALTER TABLE permission MODIFY inode_id varchar(36);" +
		       "ALTER TABLE permission_reference MODIFY asset_id varchar(36);" +
		       "ALTER TABLE permission_reference MODIFY reference_id varchar(36);" +
		       "ALTER TABLE content_rating MODIFY identifier varchar(36);" +
		       "ALTER TABLE tag_inode MODIFY inode varchar(36);" +
		       "ALTER TABLE trackback MODIFY asset_identifier varchar(36);" +
		       "ALTER TABLE web_form MODIFY user_inode varchar(36);" +
		       "ALTER TABLE web_form MODIFY web_form_id varchar(36);" +
		       "ALTER TABLE calendar_reminder MODIFY event_id varchar(36);" +
		       "ALTER TABLE dist_reindex_journal MODIFY inode_to_index varchar(36);"+
		       "ALTER TABLE dist_reindex_journal MODIFY ident_to_index varchar(36);" +
		       "ALTER TABLE analytic_summary MODIFY host_id varchar(36);" +
		       "ALTER TABLE analytic_summary_404 MODIFY host_id varchar(36);" +
		       "ALTER TABLE analytic_summary_visits MODIFY host_id varchar(36);" +
		       "ALTER TABLE clickstream MODIFY host_id varchar(36);" +
		       "ALTER TABLE clickstream_request MODIFY host_id varchar(36);" +
		       "ALTER TABLE clickstream_404 MODIFY host_id varchar(36);" +
		       "ALTER TABLE analytic_summary_workstream MODIFY host_id varchar(36);" +
		       "ALTER TABLE analytic_summary_content MODIFY inode varchar(36);" +
		       "ALTER TABLE analytic_summary_pages MODIFY inode varchar(36);" +
		       "ALTER TABLE analytic_summary_workstream MODIFY inode varchar(36);" +
		       "ALTER TABLE clickstream_request MODIFY associated_identifier varchar(36);";
	}

	@Override
	public String getOracleScript() {
		return "ALTER table FOLDER add (new_inode varchar2(36));" +
			   "UPDATE FOLDER set new_inode = cast(inode as varchar2(36));" +
			   "ALTER table FOLDER drop column inode;" +
			   "ALTER table FOLDER rename column new_inode to inode;" +
			   "ALTER TABLE folder MODIFY (inode NOT NULL);" +
			   "ALTER TABLE folder ADD CONSTRAINT pk_folder PRIMARY KEY (inode);" +
			   
			   "ALTER table STRUCTURE add (new_inode varchar2(36));" +
			   "UPDATE STRUCTURE set new_inode = cast(inode as varchar2(36));" +
			   "ALTER table STRUCTURE drop column inode;" +
			   "ALTER table STRUCTURE rename column new_inode to inode;" +
			   "ALTER TABLE  STRUCTURE MODIFY (inode NOT NULL);" +
			   "ALTER TABLE STRUCTURE ADD CONSTRAINT pk_structure PRIMARY KEY (inode);" +
			   
			   "ALTER table STRUCTURE add (new_host varchar2(36));" +
			   "UPDATE STRUCTURE set new_host = cast(host as varchar2(36));" +
			   "ALTER table STRUCTURE drop column host;" +
			   "ALTER table STRUCTURE rename column new_host to host;" +
			   "ALTER TABLE  STRUCTURE MODIFY (host NOT NULL);" +
			   
			   "ALTER table STRUCTURE add (new_folder varchar2(36));" +
			   "UPDATE STRUCTURE set new_folder = cast(folder as varchar2(36));" +
			   "ALTER table STRUCTURE drop column folder;" +
			   "ALTER table STRUCTURE rename column new_folder to folder;" +
			   "ALTER TABLE  STRUCTURE MODIFY (folder NOT NULL);" +
			   
			   "ALTER table STRUCTURE add (new_page_detail varchar2(36));" +
			   "UPDATE STRUCTURE set new_page_detail = cast(page_detail as varchar2(36));" +
			   "ALTER table STRUCTURE drop column page_detail;" +
			   "ALTER table STRUCTURE rename column new_page_detail to page_detail;" +
			   
			   "ALTER table CONTENTLET add (new_inode varchar2(36));" +
			   "UPDATE CONTENTLET set new_inode = cast(inode as varchar2(36));" +
			   "ALTER table CONTENTLET drop column inode;" +
			   "ALTER table CONTENTLET rename column new_inode to inode;" +
			   "ALTER TABLE CONTENTLET MODIFY (inode NOT NULL);" +
			   "ALTER TABLE CONTENTLET ADD CONSTRAINT pk_contentlet PRIMARY KEY (inode);" +
			   
			   "ALTER table CONTENTLET add (new_structure_inode varchar2(36));" +
			   "UPDATE CONTENTLET set new_structure_inode = cast(structure_inode as varchar2(36));" +
			   "ALTER table CONTENTLET drop column structure_inode;" +
			   "ALTER table CONTENTLET rename column new_structure_inode to structure_inode;" +
			   
			   "ALTER table CONTENTLET add (new_folder varchar2(36));" +
			   "UPDATE CONTENTLET set new_folder = cast(folder as varchar2(36));" +
			   "ALTER table CONTENTLET drop column folder;" +
			   "ALTER table CONTENTLET rename column new_folder to folder;" +
			   
			   "ALTER table CONTAINERS add (new_inode varchar2(36));" +
			   "UPDATE CONTAINERS set new_inode = cast(inode as varchar2(36));" +
			   "ALTER table CONTAINERS drop column inode;" +
			   "ALTER table CONTAINERS rename column new_inode to inode;" +
			   "ALTER TABLE CONTAINERS MODIFY (inode NOT NULL);" +
			   "ALTER TABLE CONTAINERS ADD CONSTRAINT pk_containers PRIMARY KEY (inode);" +
			   
			   "ALTER table TEMPLATE add (new_inode varchar2(36));" +
			   "UPDATE TEMPLATE set new_inode = cast(inode as varchar2(36));" +
			   "ALTER table TEMPLATE drop column inode;" +
			   "ALTER table TEMPLATE rename column new_inode to inode;" +
			   "ALTER TABLE TEMPLATE MODIFY (inode NOT NULL);" +
			   "ALTER TABLE TEMPLATE ADD CONSTRAINT pk_template PRIMARY KEY (inode);" +
			   
			   "ALTER table TEMPLATE add (new_image varchar2(36));" +
			   "UPDATE TEMPLATE set new_image = cast(image as varchar2(36));" +
			   "ALTER table TEMPLATE drop column image;" + 
			   "ALTER table TEMPLATE rename column new_image to image;" +
			   
			   "ALTER table HTMLPAGE add (new_inode varchar2(36));" +
			   "UPDATE HTMLPAGE set new_inode = cast(inode as varchar2(36));" +
			   "ALTER table HTMLPAGE drop column inode;" +
			   "ALTER table HTMLPAGE rename column new_inode to inode;" +
			   "ALTER TABLE HTMLPAGE MODIFY (inode NOT NULL);" +
			   "ALTER TABLE HTMLPAGE ADD CONSTRAINT pk_htmlpage PRIMARY KEY (inode);" +
			   
			   "ALTER table LINKS add (new_inode varchar2(36));" +
			   "UPDATE LINKS set new_inode = cast(inode as varchar2(36));" +
			   "ALTER table LINKS drop column inode;" +
			   "ALTER table LINKS rename column new_inode to inode;" +
			   "ALTER TABLE LINKS MODIFY (inode NOT NULL);" +
			   "ALTER TABLE LINKS ADD CONSTRAINT pk_links PRIMARY KEY (inode);" +
			   
			   "ALTER table LINKS add (new_internal_link_identifier varchar2(36));" +
			   "UPDATE LINKS set new_internal_link_identifier = cast(internal_link_identifier as varchar2(36));" +
			   "ALTER table LINKS drop column internal_link_identifier;" + 
			   "ALTER table LINKS rename column new_internal_link_identifier to internal_link_identifier;" +
			   
			   "ALTER table FILE_ASSET add (new_inode varchar2(36));" +
			   "UPDATE FILE_ASSET set new_inode = cast(inode as varchar2(36));" +
			   "ALTER table FILE_ASSET drop column inode;" +
			   "ALTER table FILE_ASSET rename column new_inode to inode;" +
			   "ALTER TABLE FILE_ASSET MODIFY (inode NOT NULL);" +
			   "ALTER TABLE FILE_ASSET ADD CONSTRAINT pk_file_asset PRIMARY KEY (inode);" +
			   
			   "ALTER TABLE identifier add (new_inode varchar2(36));" +
			   "UPDATE identifier set new_inode = cast(inode as varchar2(36));" +
			   "ALTER table identifier drop column inode;" +
			   "ALTER table identifier rename column new_inode to inode;" +
			   "ALTER TABLE identifier ADD CONSTRAINT pk_identifier PRIMARY KEY (inode);" +
			   
			   "ALTER TABLE identifier add (new_host_inode varchar2(36));" +
			   "UPDATE identifier set new_host_inode = cast(host_inode as varchar2(36));" +
			   "ALTER table identifier drop column host_inode;" +
			   "ALTER table identifier rename column new_host_inode to host_inode;" +
			   
			   "ALTER TABLE category add (new_inode varchar2(36));" +
			   "UPDATE category set new_inode = cast(inode as varchar2(36));" +
			   "ALTER table category drop column inode;" +
			   "ALTER table category rename column new_inode to inode;" +
			   "ALTER TABLE category MODIFY (inode NOT NULL);" +
			   "ALTER TABLE category ADD CONSTRAINT pk_category PRIMARY KEY (inode);" +
			   
			   "ALTER TABLE workflow_task add (new_inode varchar2(36));" +
			   "UPDATE workflow_task set new_inode = cast(inode as varchar2(36));" +
			   "ALTER table workflow_task drop column inode;" +
			   "ALTER table workflow_task rename column new_inode to inode;" +
			   "ALTER TABLE workflow_task MODIFY (inode NOT NULL);" +
			   "ALTER TABLE workflow_task ADD CONSTRAINT pk_workflow_task PRIMARY KEY (inode);" +
			   
			   "ALTER TABLE workflow_task add (new_webasset varchar2(36));" +
			   "UPDATE workflow_task set new_webasset = cast(webasset as varchar2(36));" +
			   "ALTER table workflow_task drop column webasset;" +
			   "ALTER table workflow_task rename column new_webasset to webasset;" +
			   
			   "ALTER TABLE workflow_comment add (new_inode varchar2(36));" +
			   "UPDATE workflow_comment set new_inode = cast(inode as varchar2(36));" +
			   "ALTER table workflow_comment drop column inode;" +
			   "ALTER table workflow_comment rename column new_inode to inode;" +
			   "ALTER TABLE workflow_comment MODIFY (inode NOT NULL);" +
			   "ALTER TABLE workflow_comment ADD CONSTRAINT pk_workflow_comment PRIMARY KEY (inode);" +
			   
			   "ALTER TABLE workflow_history add (new_inode varchar2(36));" +
			   "UPDATE workflow_history set new_inode = cast(inode as varchar2(36));" +
			   "ALTER table workflow_history drop column inode;" +
			   "ALTER table workflow_history rename column new_inode to inode;" +
			   "ALTER TABLE workflow_history MODIFY (inode NOT NULL);" +
			   "ALTER TABLE workflow_history ADD CONSTRAINT pk_workflow_history PRIMARY KEY (inode);" +
			   
			   "ALTER TABLE mailing_list add (new_inode varchar2(36));" +
			   "UPDATE mailing_list set new_inode = cast(inode as varchar2(36));" +
			   "ALTER table mailing_list drop column inode;" +
			   "ALTER table mailing_list rename column new_inode to inode;" +
			   "ALTER TABLE mailing_list MODIFY (inode NOT NULL);" +
			   "ALTER TABLE mailing_list ADD CONSTRAINT pk_mailing_list PRIMARY KEY (inode);" +
			   
			   "ALTER TABLE campaign add (new_inode varchar2(36));" +
			   "UPDATE campaign set new_inode = cast(inode as varchar2(36));" +
			   "ALTER table campaign drop column inode;" +
			   "ALTER table campaign rename column new_inode to inode;" +
			   "ALTER TABLE campaign MODIFY (inode NOT NULL);" +
			   "ALTER TABLE campaign ADD CONSTRAINT pk_campaign PRIMARY KEY (inode);" +
			   
			   "ALTER TABLE campaign add (new_communicationinode varchar2(36));" +
			   "UPDATE campaign set new_communicationinode = cast(communicationinode as varchar2(36));" +
			   "ALTER table campaign drop column communicationinode;" +
			   "ALTER table campaign rename column new_communicationinode to communicationinode;" +
			   
			   "ALTER TABLE campaign add (new_userfilterinode varchar2(36));" +
			   "UPDATE campaign set new_userfilterinode = cast(userfilterinode as varchar2(36));" +
			   "ALTER table campaign drop column userfilterinode;" +
			   "ALTER table campaign rename column new_userfilterinode to userfilterinode;" +
			   
			   "ALTER TABLE campaign add (new_parent_campaign varchar2(36));" +
			   "UPDATE campaign set new_parent_campaign = cast(parent_campaign as varchar2(36));" +
			   "ALTER table campaign drop column parent_campaign;" +
			   "ALTER table campaign rename column new_parent_campaign to parent_campaign;" +
			   
			   "ALTER TABLE recipient add (new_inode varchar2(36));" +
			   "UPDATE recipient set new_inode = cast(inode as varchar2(36));" +
			   "ALTER table recipient drop column inode;" +
			   "ALTER table recipient rename column new_inode to inode;" +
			   "ALTER TABLE recipient MODIFY (inode NOT NULL);" +
			   "ALTER TABLE recipient ADD CONSTRAINT pk_recipient PRIMARY KEY (inode);" +
			   
			   "ALTER TABLE click add (new_inode varchar2(36));" +
			   "UPDATE click set new_inode = cast(inode as varchar2(36));" +
			   "ALTER table click drop column inode;" +
			   "ALTER table click rename column new_inode to inode;" +
			   "ALTER TABLE click MODIFY (inode NOT NULL);" +
			   "ALTER TABLE click ADD CONSTRAINT pk_click PRIMARY KEY (inode);" +
			   
			   "ALTER TABLE communication add (new_inode varchar2(36));" +
			   "UPDATE communication set new_inode = cast(inode as varchar2(36));" +
			   "ALTER table communication drop column inode;" +
			   "ALTER table communication rename column new_inode to inode;" +
			   "ALTER TABLE communication MODIFY (inode NOT NULL);" +
			   "ALTER TABLE communication ADD CONSTRAINT pk_communication PRIMARY KEY (inode);" +
			   
			   "ALTER TABLE communication add (new_trackback_link_inode varchar2(36));" +
			   "UPDATE communication set new_trackback_link_inode = cast(trackback_link_inode as varchar2(36));" +
			   "ALTER table communication drop column trackback_link_inode;" +
			   "ALTER table communication rename column new_trackback_link_inode to trackback_link_inode;" +
			   
			   "ALTER TABLE communication add (new_html_page_inode varchar2(36));" +
			   "UPDATE communication set new_html_page_inode = cast(html_page_inode as varchar2(36));" +
			   "ALTER table communication drop column html_page_inode;" +
			   "ALTER table communication rename column new_html_page_inode to html_page_inode;" +
			   
			   "ALTER TABLE user_proxy add (new_inode varchar2(36));" +
			   "UPDATE user_proxy set new_inode = cast(inode as varchar2(36));" +
			   "ALTER table user_proxy drop column inode;" +
			   "ALTER table user_proxy rename column new_inode to inode;" +
			   "ALTER TABLE user_proxy MODIFY (inode NOT NULL);" +
			   "ALTER TABLE user_proxy ADD CONSTRAINT pk_user_proxy PRIMARY KEY (inode);" +
			   
			   "ALTER TABLE virtual_link add (new_inode varchar2(36));" +
			   "UPDATE virtual_link set new_inode = cast(inode as varchar2(36));" +
			   "ALTER table virtual_link drop column inode;" +
			   "ALTER table virtual_link rename column new_inode to inode;" +
			   "ALTER TABLE virtual_link MODIFY (inode NOT NULL);" +
			   "ALTER TABLE virtual_link ADD CONSTRAINT pk_virtual_link PRIMARY KEY (inode);" +
			   
			   "ALTER TABLE relationship add (new_inode varchar2(36));" +
			   "UPDATE relationship set new_inode = cast(inode as varchar2(36));" +
			   "ALTER table relationship drop column inode;" +
			   "ALTER table relationship rename column new_inode to inode;" +
			   "ALTER TABLE relationship MODIFY (inode NOT NULL);" +
			   "ALTER TABLE relationship ADD CONSTRAINT pk_relationship PRIMARY KEY (inode);" +
			   
			   "ALTER TABLE relationship add (new_parent_structure_inode varchar2(36));" +
			   "UPDATE relationship set new_parent_structure_inode = cast(parent_structure_inode as varchar2(36));" +
			   "ALTER table relationship drop column parent_structure_inode;" +
			   "ALTER table relationship rename column new_parent_structure_inode to parent_structure_inode;" +
			   
			   "ALTER TABLE relationship add (new_child_structure_inode varchar2(36));" +
			   "UPDATE relationship set new_child_structure_inode = cast(child_structure_inode as varchar2(36));" +
			   "ALTER table relationship drop column child_structure_inode;" +
			   "ALTER table relationship rename column new_child_structure_inode to child_structure_inode;" +
			   
			   "ALTER TABLE field add (new_inode varchar2(36));" +
			   "UPDATE field set new_inode = cast(inode as varchar2(36));" +
			   "ALTER table field drop column inode;" +
			   "ALTER table field rename column new_inode to inode;" +
			   "ALTER TABLE field MODIFY (inode NOT NULL);" +
			   "ALTER TABLE field ADD CONSTRAINT pk_field PRIMARY KEY (inode);" +
			   
			   "ALTER TABLE field add (new_structure_inode varchar2(36));" +
			   "UPDATE field set new_structure_inode = cast(structure_inode as varchar2(36));" +
			   "ALTER table field drop column structure_inode;" +
			   "ALTER table field rename column new_structure_inode to structure_inode;" +
			   
			   "ALTER TABLE user_comments add (new_inode varchar2(36));" +
			   "UPDATE user_comments set new_inode = cast(inode as varchar2(36));" +
			   "ALTER table user_comments drop column inode;" +
			   "ALTER table user_comments rename column new_inode to inode;" +
			   "ALTER TABLE user_comments MODIFY (inode NOT NULL);" +
			   "ALTER TABLE user_comments ADD CONSTRAINT pk_user_comments PRIMARY KEY (inode);" +
			   
			   "ALTER TABLE user_comments add (new_communication_id varchar2(36));" +
			   "UPDATE user_comments set new_communication_id = cast(communication_id as varchar2(36));" +
			   "ALTER table user_comments drop column communication_id;" +
			   "ALTER table user_comments rename column new_communication_id to communication_id;" +
			   
			   "ALTER TABLE user_filter add (new_inode varchar2(36));" +
			   "UPDATE user_filter set new_inode = cast(inode as varchar2(36));" +
			   "ALTER table user_filter drop column inode;" +
			   "ALTER table user_filter rename column new_inode to inode;" +
			   "ALTER TABLE user_filter MODIFY (inode NOT NULL);" +
			   "ALTER TABLE user_filter ADD CONSTRAINT pk_user_filter PRIMARY KEY (inode);" +
			   
			   "ALTER TABLE report_asset add (new_inode varchar2(36));" +
			   "UPDATE report_asset set new_inode = cast(inode as varchar2(36));" +
			   "ALTER table report_asset drop column inode;" +
			   "ALTER table report_asset rename column new_inode to inode;" +
			   "ALTER TABLE report_asset MODIFY (inode NOT NULL);" +
			   "ALTER TABLE report_asset ADD CONSTRAINT pk_report_asset PRIMARY KEY (inode);" +
			   
			   "ALTER TABLE report_parameter add (new_inode varchar2(36));" +
			   "UPDATE report_parameter set new_inode = cast(inode as varchar2(36));" +
			   "ALTER table report_parameter drop column inode;" +
			   "ALTER table report_parameter rename column new_inode to inode;" +
			   "ALTER TABLE report_parameter MODIFY (inode NOT NULL);" +
			   "ALTER TABLE report_parameter ADD CONSTRAINT pk_report_parameter PRIMARY KEY (inode);" +
			   
			   "ALTER TABLE report_parameter add (new_report_inode varchar2(36));" +
			   "UPDATE report_parameter set new_report_inode = cast(report_inode as varchar2(36));" +
			   "ALTER table report_parameter drop column report_inode;" +
			   "ALTER table report_parameter rename column new_report_inode to report_inode;" +
			   
			   "ALTER TABLE inode add (new_inode varchar2(36));" +
			   "UPDATE inode set new_inode = cast(inode as varchar2(36));" +
			   "ALTER table inode drop column inode;" +
			   "ALTER table inode rename column new_inode to inode;" +
			   "ALTER TABLE inode MODIFY (inode NOT NULL);" +
			   "ALTER TABLE inode ADD CONSTRAINT pk_inode PRIMARY KEY (inode);" +
			   
			   "ALTER TABLE inode add (new_identifier varchar2(36));" +
			   "UPDATE inode set new_identifier = cast(identifier as varchar2(36));" +
			   "ALTER table inode drop column identifier;" +
			   "ALTER table inode rename column new_identifier to identifier;" +
			   
			   "ALTER TABLE cms_role add (new_id varchar2(36));" +
			   "UPDATE cms_role set new_id = cast(id as varchar2(36));" +
			   "ALTER table cms_role drop column id;" +
			   "ALTER table cms_role rename column new_id to id;" +
			   "ALTER TABLE cms_role MODIFY (id NOT NULL);" +
			   "ALTER TABLE cms_role ADD CONSTRAINT pk_cms_role PRIMARY KEY (id);" +
			   
			   "ALTER TABLE cms_role add (new_parent varchar2(36));" +
			   "UPDATE cms_role set new_parent = cast(parent as varchar2(36));" +
			   "ALTER table cms_role drop column parent;" +
			   "ALTER table cms_role rename column new_parent to parent;" +
			   "ALTER TABLE cms_role MODIFY (parent NOT NULL);" +
			   
			   "ALTER TABLE users_cms_roles add (new_id varchar2(36));" +
			   "UPDATE users_cms_roles set new_id = cast(id as varchar2(36));" +
			   "ALTER table users_cms_roles drop column id;" +
			   "ALTER table users_cms_roles rename column new_id to id;" +
			   "ALTER TABLE users_cms_roles MODIFY (id NOT NULL);" +
			   "ALTER TABLE users_cms_roles ADD CONSTRAINT pk_users_cms_roles PRIMARY KEY (id);" +
			   
			   "ALTER TABLE users_cms_roles add (new_role_id varchar2(36));" +
			   "UPDATE users_cms_roles set new_role_id = cast(role_id as varchar2(36));" +
			   "ALTER table users_cms_roles drop column role_id;" +
			   "ALTER table users_cms_roles rename column new_role_id to role_id;" +
			   "ALTER TABLE users_cms_roles MODIFY (role_id NOT NULL);" +
			   
			   "ALTER TABLE layouts_cms_roles add (new_id varchar2(36));" +
			   "UPDATE layouts_cms_roles set new_id = cast(id as varchar2(36));" +
			   "ALTER table layouts_cms_roles drop column id;" +
			   "ALTER table layouts_cms_roles rename column new_id to id;" +
			   "ALTER TABLE layouts_cms_roles MODIFY (id NOT NULL);" +
			   "ALTER TABLE layouts_cms_roles ADD CONSTRAINT pk_layouts_cms_roles PRIMARY KEY (id);" +
			   
			   "ALTER TABLE layouts_cms_roles add (new_layout_id varchar2(36));" +
			   "UPDATE layouts_cms_roles set new_layout_id = cast(layout_id as varchar2(36));" +
			   "ALTER table layouts_cms_roles drop column layout_id;" +
			   "ALTER table layouts_cms_roles rename column new_layout_id to layout_id;" +
			   "ALTER TABLE layouts_cms_roles MODIFY (layout_id NOT NULL);" +
			   
			   "ALTER TABLE layouts_cms_roles add (new_role_id varchar2(36));" +
			   "UPDATE layouts_cms_roles set new_role_id = cast(role_id as varchar2(36));" +
			   "ALTER table layouts_cms_roles drop column role_id;" +
			   "ALTER table layouts_cms_roles rename column new_role_id to role_id;" +
			   "ALTER TABLE layouts_cms_roles MODIFY (role_id NOT NULL);" +
			   
			   "ALTER TABLE cms_layout add (new_id varchar2(36));" +
			   "UPDATE cms_layout set new_id = cast(id as varchar2(36));" +
			   "ALTER table cms_layout drop column id;" +
			   "ALTER table cms_layout rename column new_id to id;" +
			   "ALTER TABLE cms_layout MODIFY (id NOT NULL);" +
			   "ALTER TABLE cms_layout ADD CONSTRAINT pk_cms_layout PRIMARY KEY (id);" +
			   
			   "ALTER TABLE cms_layouts_portlets add (new_id varchar2(36));" +
			   "UPDATE cms_layouts_portlets set new_id = cast(id as varchar2(36));" +
			   "ALTER table cms_layouts_portlets drop column id;" +
			   "ALTER table cms_layouts_portlets rename column new_id to id;" +
			   "ALTER TABLE cms_layouts_portlets MODIFY (id NOT NULL);" +
			   "ALTER TABLE cms_layouts_portlets ADD CONSTRAINT pk_cms_layouts_portlets PRIMARY KEY (id);" +
			   
			   "ALTER TABLE  cms_layouts_portlets add (new_layout_id varchar2(36));" +
			   "UPDATE  cms_layouts_portlets set new_layout_id = cast(layout_id as varchar2(36));" +
			   "ALTER table cms_layouts_portlets drop column layout_id;" +
			   "ALTER table cms_layouts_portlets rename column new_layout_id to layout_id;" +
			   "ALTER TABLE cms_layouts_portlets MODIFY (layout_id NOT NULL);" +
			   
			   "ALTER TABLE host_variable add (new_id varchar2(36));" +
			   "UPDATE host_variable set new_id = cast(id as varchar2(36));" +
			   "ALTER table host_variable drop column id;" +
			   "ALTER table host_variable rename column new_id to id;" +
			   "ALTER TABLE host_variable MODIFY (id NOT NULL);" +
			   "ALTER TABLE host_variable ADD CONSTRAINT pk_host_variable PRIMARY KEY (id);" +
			   
			   "ALTER TABLE host_variable add (new_host_id varchar2(36));" +
			   "UPDATE host_variable set new_host_id = cast(host_id as varchar2(36));" +
			   "ALTER table host_variable drop column host_id;" +
			   "ALTER table host_variable rename column new_host_id to host_id;" +
			   
			   "ALTER TABLE fixes_audit add (new_id varchar2(36));" +
			   "UPDATE fixes_audit set new_id = cast(id as varchar2(36));" +
			   "ALTER table fixes_audit drop column id;" +
			   "ALTER table fixes_audit rename column new_id to id;" +
			   "ALTER TABLE fixes_audit MODIFY (id NOT NULL);" +
			   "ALTER TABLE fixes_audit ADD CONSTRAINT pk_fixes_audit PRIMARY KEY (id);" +
			   
			   "ALTER TABLE tree add (new_child varchar2(36));" +
			   "UPDATE tree set new_child = cast(child as varchar2(36));" +
			   "ALTER table tree drop column child;" +
			   "ALTER table tree rename column new_child to child;" +
			   "ALTER TABLE tree MODIFY (child NOT NULL);" +
			   
			   "ALTER TABLE tree add (new_parent varchar2(36));" +
			   "UPDATE tree set new_parent = cast(parent as varchar2(36));" +
			   "ALTER table tree drop column parent;" +
			   "ALTER table tree rename column new_parent to parent;" +
			   "ALTER TABLE tree MODIFY (parent NOT NULL);" +
			   
			   "ALTER TABLE multi_tree add (new_child varchar2(36));" +
			   "UPDATE multi_tree set new_child = cast(child as varchar2(36));" +
			   "ALTER table multi_tree drop column child;" +
			   "ALTER table multi_tree rename column new_child to child;" +
			   "ALTER TABLE multi_tree MODIFY (child NOT NULL);" +
			   
			   "ALTER TABLE multi_tree add (new_parent1 varchar2(36));" +
			   "UPDATE multi_tree set new_parent1 = cast(parent1 as varchar2(36));" +
			   "ALTER table multi_tree drop column parent1;" +
			   "ALTER table multi_tree rename column new_parent1 to parent1;" +
			   "ALTER TABLE multi_tree MODIFY (parent1 NOT NULL);" +
			   
			   "ALTER TABLE multi_tree add (new_parent2 varchar2(36));" +
			   "UPDATE multi_tree set new_parent2 = cast(parent2 as varchar2(36));" +
			   "ALTER table multi_tree drop column parent2;" +
			   "ALTER table multi_tree rename column new_parent2 to parent2;" +
			   "ALTER TABLE multi_tree MODIFY (parent2 NOT NULL);" +
			   
			   "ALTER TABLE permission add (new_roleid varchar2(36));" +
			   "UPDATE permission set new_roleid = cast(roleid as varchar2(36));" +
			   "ALTER table permission drop column roleid;" +
			   "ALTER table permission rename column new_roleid to roleid;" +
			   
			   "ALTER TABLE permission add (new_inode_id varchar2(36));" +
			   "UPDATE permission set new_inode_id = cast(inode_id as varchar2(36));" +
			   "ALTER table permission drop column inode_id;" +
			   "ALTER table permission rename column new_inode_id to inode_id;" +
			   
			   "ALTER TABLE permission_reference add (new_asset_id varchar2(36));" +
			   "UPDATE permission_reference set new_asset_id = cast(asset_id as varchar2(36));" +
			   "ALTER table permission_reference drop column asset_id;" +
			   "ALTER table permission_reference rename column new_asset_id to asset_id;" +
			   
			   "ALTER TABLE permission_reference add (new_reference_id varchar2(36));" +
			   "UPDATE permission_reference set new_reference_id = cast(reference_id as varchar2(36));" +
			   "ALTER table permission_reference drop column reference_id;" +
			   "ALTER table permission_reference rename column new_reference_id to reference_id;" +
			   
			   "ALTER TABLE content_rating add (new_identifier varchar2(36));" +
			   "UPDATE content_rating set new_identifier = cast(identifier as varchar2(36));" +
			   "ALTER table content_rating drop column identifier;" +
			   "ALTER table content_rating rename column new_identifier to identifier;" +
			   "ALTER TABLE content_rating ADD CONSTRAINT pk_content_rating PRIMARY KEY (id);" +
			   
			   "ALTER TABLE tag_inode add (new_inode varchar2(36));" +
			   "UPDATE tag_inode set new_inode = cast(inode as varchar2(36));" +
			   "ALTER table tag_inode drop column inode;" +
			   "ALTER table tag_inode rename column new_inode to inode;" +
			   "ALTER TABLE tag_inode MODIFY (inode NOT NULL);" +
			   
			   "ALTER TABLE web_form add (new_web_form_id varchar2(36));" +
			   "UPDATE web_form set new_web_form_id = cast(web_form_id as varchar2(36));" +
			   "ALTER table web_form drop column web_form_id;" +
			   "ALTER table web_form rename column new_web_form_id to web_form_id;" +
			   "ALTER TABLE web_form MODIFY (web_form_id NOT NULL);" +
			   "ALTER TABLE web_form ADD CONSTRAINT pk_web_form_id PRIMARY KEY(web_form_id);" +
			   
			   "ALTER TABLE web_form add (new_user_inode varchar2(36));" +
			   "UPDATE web_form set new_user_inode = cast(user_inode as varchar2(36));" +
			   "ALTER table web_form drop column user_inode;" +
			   "ALTER table web_form rename column new_user_inode to user_inode;" +
			   
			   "ALTER TABLE trackback add (new_asset_identifier varchar2(36));" +
			   "UPDATE trackback set new_asset_identifier = cast(asset_identifier as varchar2(36));" +
			   "ALTER table trackback drop column asset_identifier;" +
			   "ALTER table trackback rename column new_asset_identifier to asset_identifier;" +
			   "ALTER TABLE trackback ADD CONSTRAINT pk_trackback PRIMARY KEY (id);" +
			   
			   "ALTER TABLE dist_reindex_journal add (new_inode_to_index varchar2(36));" +
			   "UPDATE dist_reindex_journal set new_inode_to_index = cast(inode_to_index as varchar2(36));" +
			   "ALTER table dist_reindex_journal drop column inode_to_index;" +
			   "ALTER table dist_reindex_journal rename column new_inode_to_index to inode_to_index;" +
			   
			   "ALTER TABLE dist_reindex_journal add (new_ident_to_index varchar2(36));" +
			   "UPDATE dist_reindex_journal set new_ident_to_index = cast(ident_to_index as varchar2(36));" +
			   "ALTER table dist_reindex_journal drop column ident_to_index;" +
			   "ALTER table dist_reindex_journal rename column new_ident_to_index to ident_to_index;" +
			   "ALTER TABLE dist_reindex_journal ADD CONSTRAINT pk_reindex_journal PRIMARY KEY(id);" +
			   
			   "ALTER TABLE calendar_reminder add (new_event_id varchar2(36));" +
			   "UPDATE calendar_reminder set new_event_id = cast(event_id as varchar2(36));" +
			   "ALTER table calendar_reminder drop column event_id;" +
			   "ALTER table calendar_reminder rename column new_event_id to event_id;" +
			   "ALTER TABLE calendar_reminder MODIFY (event_id NOT NULL);" +
			   
			   "ALTER TABLE analytic_summary_404 add (new_host_id varchar2(36));" +   
			   "UPDATE analytic_summary_404 set new_host_id = cast(host_id as varchar2(36));" +   
			   "ALTER table analytic_summary_404 drop column host_id;" +   
			   "ALTER table analytic_summary_404 rename column new_host_id to host_id;" +
			   
			   "ALTER TABLE analytic_summary add (new_host_id varchar2(36));" +   
			   "UPDATE analytic_summary set new_host_id = cast(host_id as varchar2(36));" +   
			   "ALTER table analytic_summary drop column host_id;" +   
			   "ALTER table analytic_summary rename column new_host_id to host_id;" +
			   "ALTER TABLE analytic_summary MODIFY (host_id NOT NULL);" +
			   
			   "ALTER TABLE analytic_summary_visits add (new_host_id varchar2(36));" +   
			   "UPDATE analytic_summary_visits set new_host_id = cast(host_id as varchar2(36));" +   
			   "ALTER table analytic_summary_visits drop column host_id;" +   
			   "ALTER table analytic_summary_visits rename column new_host_id to host_id;" +
			   
			   "ALTER TABLE analytic_summary_workstream add (new_host_id varchar2(36));" +   
			   "UPDATE analytic_summary_workstream set new_host_id = cast(host_id as varchar2(36));" +   
			   "ALTER table analytic_summary_workstream drop column host_id;" +   
			   "ALTER table analytic_summary_workstream rename column new_host_id to host_id;" +
			   
			   "ALTER TABLE analytic_summary_workstream add (new_inode varchar2(36));" +   
			   "UPDATE analytic_summary_workstream set new_inode = cast(inode as varchar2(36));" +   
			   "ALTER table analytic_summary_workstream drop column inode;" +   
			   "ALTER table analytic_summary_workstream rename column new_inode to inode;" +
			   
			   "ALTER TABLE analytic_summary_pages add (new_inode varchar2(36));" +   
			   "UPDATE analytic_summary_pages set new_inode = cast(inode as varchar2(36));" +   
			   "ALTER table analytic_summary_pages drop column inode;" +   
			   "ALTER table analytic_summary_pages rename column new_inode to inode;" +
			   
			   "ALTER TABLE analytic_summary_content add (new_inode varchar2(36));" +   
			   "UPDATE analytic_summary_content set new_inode = cast(inode as varchar2(36));" +   
			   "ALTER table analytic_summary_content drop column inode;" +   
			   "ALTER table analytic_summary_content rename column new_inode to inode;" +
			   
			   "ALTER TABLE clickstream add (new_host_id varchar2(36));" +   
			   "UPDATE clickstream set new_host_id = cast(host_id as varchar2(36));" +   
			   "ALTER table clickstream drop column host_id;" +   
			   "ALTER table clickstream rename column new_host_id to host_id;" +
			   
			   "ALTER TABLE clickstream_request add (new_host_id varchar2(36));" +   
			   "UPDATE clickstream_request set new_host_id = cast(host_id as varchar2(36));" +   
			   "ALTER table clickstream_request drop column host_id;" +   
			   "ALTER table clickstream_request rename column new_host_id to host_id;" +
			   
			   "ALTER TABLE clickstream_request add (new_associated_identifier varchar2(36));" +   
			   "UPDATE clickstream_request set new_associated_identifier = cast(associated_identifier as varchar2(36));" +   
			   "ALTER table clickstream_request drop column associated_identifier;" +   
			   "ALTER table clickstream_request rename column new_associated_identifier to associated_identifier;" +
			   
			   "ALTER TABLE clickstream_404 add (new_host_id varchar2(36));" +   
			   "UPDATE clickstream_404 set new_host_id = cast(host_id as varchar2(36));" +   
			   "ALTER table clickstream_404 drop column host_id;" +   
			   "ALTER table clickstream_404 rename column new_host_id to host_id;" +
			   
			   "ALTER TABLE tree ADD CONSTRAINT pk_tree PRIMARY KEY (child, parent, relation_type);" +
			   "ALTER TABLE multi_tree ADD CONSTRAINT pk_multi_tree PRIMARY KEY (child, parent1, parent2);" +
			   "ALTER TABLE permission ADD CONSTRAINT pk_permission PRIMARY KEY (id);" +
			   "ALTER TABLE permission_reference ADD CONSTRAINT pk_permission_reference PRIMARY KEY (id);" +
			   "ALTER TABLE permission_reference ADD CONSTRAINT asset_id_unique_key UNIQUE(asset_id);" +
			   "ALTER TABLE calendar_reminder ADD CONSTRAINT pk_calendar_reminder PRIMARY KEY (user_id, event_id, send_date);" +
			   "ALTER TABLE tag_inode ADD CONSTRAINT pk_tag_inode PRIMARY KEY (tag_id, inode);" +
			   "ALTER TABLE IDENTIFIER ADD CONSTRAINT identifier_inode_key UNIQUE (uri, host_inode);" +
			   "CREATE UNIQUE INDEX idx_field_velocity_structure ON field (velocity_var_name,structure_inode);" +
			   "CREATE INDEX idx_tree_1 ON tree (parent);" +
			   "CREATE INDEX idx_tree_2 ON tree (child);" +
			   "CREATE INDEX idx_tree_4 ON tree (parent, child, relation_type);" +
			   "CREATE INDEX idx_tree_5 ON tree (parent, relation_type);" +
			   "CREATE INDEX idx_tree_6 ON tree (child, relation_type);" +
			   "ALTER TABLE permission ADD CONSTRAINT permission_inode_id_key UNIQUE (permission_type,inode_id, roleid);" +
			   "create index idx_permission_2 on permission (permission_type, inode_id);" +
			   "create index idx_permission_3 on permission (roleid);" +
			   "CREATE INDEX idx_permission_reference_2 ON permission_reference (reference_id);" +
			   "CREATE INDEX idx_permission_reference_3 ON permission_reference (reference_id,permission_type);" +
			   "CREATE INDEX idx_permission_reference_4 ON permission_reference (asset_id,permission_type);" +
			   "CREATE INDEX idx_permission_reference_5 ON permission_reference (asset_id,reference_id,permission_type);" +
			   "CREATE INDEX idx_contentlet_1 ON contentlet (inode, live);" +
			   "CREATE INDEX idx_contentlet_2 ON contentlet (inode, working);" +
			   "CREATE INDEX idx_contentlet_4 ON contentlet (structure_inode);" +
			   "create index idx_field_1 on field (structure_inode);" +
			   "alter table structure add constraint unique_struct_vel_var_name unique (velocity_var_name);" +
		       "create index idx_relationship_1 on relationship (parent_structure_inode);" +
		       "create index idx_relationship_2 on relationship (child_structure_inode);" +
		       "create index idx_trackback_1 on trackback (asset_identifier);" +
		       "CREATE INDEX dist_reindex_index1 on dist_reindex_journal (inode_to_index);" +
		       "CREATE INDEX dist_reindex_index4 on dist_reindex_journal (ident_to_index,serverid);" +
		       "create index tag_inode_inode on tag_inode(inode);" +
		       "ALTER TABLE cms_role ADD CONSTRAINT cms_role2_unique UNIQUE (role_key);" +
		       "ALTER TABLE cms_role ADD CONSTRAINT cms_role3_unique UNIQUE (db_fqn);" +
		       "ALTER TABLE users_cms_roles ADD CONSTRAINT users_cms_roles1_unique UNIQUE (role_id, user_id);" +
		       "ALTER TABLE layouts_cms_roles ADD CONSTRAINT layouts_cms_roles1_unique UNIQUE (role_id, layout_id);" +
		       "ALTER TABLE cms_layout ADD CONSTRAINT cms_layout_unique_1 UNIQUE (layout_name);" +
		       "ALTER TABLE cms_layouts_portlets ADD CONSTRAINT cms_layouts_portlets_unq_1 UNIQUE (portlet_id, layout_id);" +
		       "ALTER TABLE report_parameter ADD CONSTRAINT report_param_key UNIQUE (report_inode, parameter_name);" +
		       "create index idx_workflow_4 on workflow_task (webasset);" +
		       "ALTER TABLE user_proxy ADD CONSTRAINT user_id_key UNIQUE (user_id);" +
		       "ALTER TABLE analytic_summary ADD CONSTRAINT pk_analytic_summary PRIMARY KEY(id);" +
		       "ALTER TABLE analytic_summary ADD CONSTRAINT analytic_summary_key UNIQUE (summary_period_id, host_id);" +
		       "create index idx_analytic_summary_1 on analytic_summary (host_id);" +
		       "create index idx_analytic_summary_404_1 on analytic_summary_404 (host_id);" +
		       "create index idx_analytic_summary_visits_1 on analytic_summary_visits (host_id);" +
		       "create index idx_user_clickstream11 on clickstream (host_id);" +
		       "create index idx_dashboard_workstream_2 on analytic_summary_workstream (host_id);" +
		       "create index idx_user_clickstream_404_3 on clickstream_404 (host_id);" +
		       "create index idx_user_clickstream_request_3 on clickstream_request (associated_identifier);" +
		       "alter table analytic_summary_pages add constraint fka1ad33b9ed30e054 foreign key (summary_id) references analytic_summary;" +
		       "alter table analytic_summary_content add constraint fk53cb4f2eed30e054 foreign key (summary_id) references analytic_summary;" +
		       "alter table analytic_summary_referer add constraint fk5bc0f3e2ed30e054 foreign key (summary_id) references analytic_summary;";
	}

	@Override
	public String getPostgresScript() {
		return "ALTER TABLE inode ALTER COLUMN inode TYPE varchar(36);" +
		       "ALTER TABLE inode ALTER COLUMN identifier TYPE varchar(36);" +
		       "ALTER TABLE identifier ALTER COLUMN inode TYPE varchar(36);" +
	           "ALTER TABLE identifier ALTER COLUMN host_inode TYPE varchar(36);" +
		       "ALTER TABLE folder ALTER COLUMN inode TYPE varchar(36);" +
		       "ALTER TABLE contentlet ALTER COLUMN inode TYPE varchar(36);" +
		       "ALTER TABLE contentlet ALTER COLUMN folder TYPE varchar(36);" +
		       "ALTER TABLE contentlet ALTER COLUMN structure_inode TYPE varchar(36);" +
		       "ALTER TABLE containers ALTER COLUMN inode TYPE varchar(36);" +
		       "ALTER TABLE template ALTER COLUMN inode TYPE varchar(36);" +
		       "ALTER TABLE template ALTER COLUMN image TYPE varchar(36);" +
		       "ALTER TABLE htmlpage ALTER COLUMN inode TYPE varchar(36);" +
		       "ALTER TABLE links ALTER COLUMN inode TYPE varchar(36);" +
		       "ALTER TABLE links ALTER COLUMN internal_link_identifier TYPE varchar(36);" +
		       "ALTER TABLE file_asset ALTER COLUMN inode TYPE varchar(36);" +
		       "ALTER TABLE category ALTER COLUMN inode TYPE varchar(36);" +
		       "ALTER TABLE structure ALTER COLUMN inode TYPE varchar(36);" +
		       "ALTER TABLE structure ALTER COLUMN host TYPE varchar(36);" +
		       "ALTER TABLE structure ALTER COLUMN folder TYPE varchar(36);" +
		       "ALTER TABLE structure ALTER COLUMN page_detail TYPE varchar(36);" +
		       "ALTER TABLE workflow_task ALTER COLUMN inode TYPE varchar(36);" +
		       "ALTER TABLE workflow_task ALTER COLUMN webasset TYPE varchar(36);" +
		       "ALTER TABLE workflow_comment ALTER COLUMN inode TYPE varchar(36);" +
		       "ALTER TABLE workflow_history ALTER COLUMN inode TYPE varchar(36);" +
		       "ALTER TABLE mailing_list ALTER COLUMN inode TYPE varchar(36);" +
		       "ALTER TABLE campaign ALTER COLUMN inode TYPE varchar(36);" +
		       "ALTER TABLE campaign ALTER COLUMN communicationinode TYPE varchar(36);" +
		       "ALTER TABLE campaign ALTER COLUMN userfilterinode TYPE varchar(36);" +
		       "ALTER TABLE campaign ALTER COLUMN parent_campaign TYPE varchar(36);" +
		       "ALTER TABLE recipient ALTER COLUMN inode TYPE varchar(36);" +
		       "ALTER TABLE click ALTER COLUMN inode TYPE varchar(36);" +
		       "ALTER TABLE communication ALTER COLUMN inode TYPE varchar(36);" +
		       "ALTER TABLE communication ALTER COLUMN trackback_link_inode TYPE varchar(36);" +
		       "ALTER TABLE communication ALTER COLUMN html_page_inode TYPE varchar(36);" +
		       "ALTER TABLE user_proxy ALTER COLUMN inode TYPE varchar(36);" +
		       "ALTER TABLE virtual_link ALTER COLUMN inode TYPE varchar(36);" +
		       "ALTER TABLE relationship ALTER COLUMN inode TYPE varchar(36);" +
		       "ALTER TABLE relationship ALTER COLUMN parent_structure_inode TYPE varchar(36);" +
		       "ALTER TABLE relationship ALTER COLUMN child_structure_inode TYPE varchar(36);" +
		       "ALTER TABLE field ALTER COLUMN inode TYPE varchar(36);" +
		       "ALTER TABLE field ALTER COLUMN structure_inode TYPE varchar(36);" +
		       "ALTER TABLE user_comments ALTER COLUMN inode TYPE varchar(36);" +
		       "ALTER TABLE user_comments ALTER COLUMN communication_id TYPE varchar(36);" +
		       "ALTER TABLE user_filter ALTER COLUMN inode TYPE varchar(36);" +
		       "ALTER TABLE report_asset ALTER COLUMN inode TYPE varchar(36);" +
		       "ALTER TABLE report_parameter ALTER COLUMN inode TYPE varchar(36);" +
		       "ALTER TABLE report_parameter ALTER COLUMN report_inode TYPE varchar(36);" +
		       "ALTER TABLE cms_role ALTER COLUMN id TYPE varchar(36);" +
		       "ALTER TABLE users_cms_roles ALTER COLUMN id TYPE varchar(36);" +
		       "ALTER TABLE layouts_cms_roles ALTER COLUMN id TYPE varchar(36);" +
		       "ALTER TABLE cms_layout ALTER COLUMN id TYPE varchar(36);" +
		       "ALTER TABLE cms_layouts_portlets ALTER COLUMN id TYPE varchar(36);" +
		       "ALTER TABLE host_variable ALTER COLUMN id TYPE varchar(36);" +
		       "ALTER TABLE fixes_audit ALTER COLUMN id TYPE varchar(36);"+
		       "ALTER TABLE cms_role ALTER COLUMN parent TYPE varchar(36);" +
		       "ALTER TABLE users_cms_roles ALTER COLUMN role_id TYPE varchar(36);" +
		       "ALTER TABLE layouts_cms_roles ALTER COLUMN layout_id TYPE varchar(36);" +
		       "ALTER TABLE layouts_cms_roles ALTER COLUMN role_id TYPE varchar(36);" +
		       "ALTER TABLE cms_layouts_portlets ALTER COLUMN layout_id TYPE varchar(36);" +
		       "ALTER TABLE host_variable ALTER COLUMN host_id TYPE varchar(36);" +
		       "ALTER TABLE tree ALTER COLUMN child TYPE varchar(36);" +
		       "ALTER TABLE tree ALTER COLUMN parent TYPE varchar(36);" +
		       "ALTER TABLE multi_tree ALTER COLUMN child TYPE varchar(36);" +
		       "ALTER TABLE multi_tree ALTER COLUMN parent1 TYPE varchar(36);" +
		       "ALTER TABLE multi_tree ALTER COLUMN parent2 TYPE varchar(36);" +
		       "ALTER TABLE permission ALTER COLUMN roleid TYPE varchar(36);" +
		       "ALTER TABLE permission ALTER COLUMN inode_id TYPE varchar(36);" +
		       "ALTER TABLE permission_reference ALTER COLUMN asset_id TYPE varchar(36);" +
		       "ALTER TABLE permission_reference ALTER COLUMN reference_id TYPE varchar(36);" +
		       "ALTER TABLE content_rating ALTER COLUMN identifier TYPE varchar(36);" +
		       "ALTER TABLE tag_inode ALTER COLUMN inode TYPE varchar(36);" +
		       "ALTER TABLE web_form ALTER COLUMN web_form_id TYPE varchar(36);" +
		       "ALTER TABLE web_form ALTER COLUMN user_inode TYPE varchar(36);" +
		       "ALTER TABLE calendar_reminder ALTER COLUMN event_id TYPE varchar(36);" +
		       "ALTER TABLE trackback ALTER COLUMN asset_identifier TYPE varchar(36);" +
		       "ALTER TABLE dist_reindex_journal ALTER COLUMN inode_to_index TYPE varchar(36);" +
		       "ALTER TABLE dist_reindex_journal ALTER COLUMN ident_to_index TYPE varchar(36);" +
		       "ALTER TABLE analytic_summary ALTER COLUMN host_id TYPE varchar(36);" +
		       "ALTER TABLE analytic_summary_404 ALTER COLUMN host_id TYPE varchar(36);" +
		       "ALTER TABLE analytic_summary_visits ALTER COLUMN host_id TYPE varchar(36);" +
		       "ALTER TABLE clickstream ALTER COLUMN host_id TYPE varchar(36);" +
		       "ALTER TABLE clickstream_request ALTER COLUMN host_id TYPE varchar(36);" +
		       "ALTER TABLE clickstream_404 ALTER COLUMN host_id TYPE varchar(36);" +
		       "ALTER TABLE analytic_summary_workstream ALTER COLUMN host_id TYPE varchar(36);" +
		       "ALTER TABLE analytic_summary_workstream ALTER COLUMN inode TYPE varchar(36);" +
		       "ALTER TABLE analytic_summary_pages ALTER COLUMN inode TYPE varchar(36);" +
		       "ALTER TABLE analytic_summary_content ALTER COLUMN inode TYPE varchar(36);" +
		       "ALTER TABLE clickstream_request ALTER COLUMN associated_identifier TYPE varchar(36);";		    	   
	}

	@Override
	protected List<String> getTablesToDropConstraints() {
		List<String> tables = new ArrayList<String>();
		if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)
				||DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
			dropIndexes();
			tables.add("template");
			tables.add("htmlpage");
			tables.add("links");
			tables.add("file_asset");
			tables.add("containers");
			tables.add("contentlet");
			tables.add("field");
			tables.add("relationship");
			tables.add("structure");
			tables.add("folder");
			tables.add("identifier");
			tables.add("category");
			tables.add("workflow_task");
			tables.add("workflow_comment");
			tables.add("workflow_history");
			tables.add("mailing_list");
			tables.add("campaign");
			tables.add("click");
			tables.add("recipient");
			tables.add("communication");
			tables.add("user_proxy");
			tables.add("virtual_link");
			tables.add("user_comments");
			tables.add("user_filter");
			tables.add("report_asset");
			tables.add("report_parameter");
			tables.add("cms_role");
			tables.add("users_cms_roles");
			tables.add("cms_layout");
			tables.add("layouts_cms_roles");
			tables.add("cms_layouts_portlets");
			tables.add("host_variable");
			tables.add("fixes_audit");
			tables.add("permission");
			tables.add("permission_reference");
			tables.add("content_rating");
			tables.add("tag_inode");
			tables.add("web_form");
			tables.add("calendar_reminder");
			tables.add("trackback");
			tables.add("dist_reindex_journal");
			tables.add("tree");
			tables.add("multi_tree");
			tables.add("analytic_summary");
			tables.add("inode");
		}
		return tables;
	}
	
	private void dropIndexes(){
		DotConnect dc = new DotConnect();
		
		String sql ="";
		if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)){
			sql = "drop index IDX_FIELD_VELOCITY_STRUCTURE;" +
			      "alter table analytic_summary_pages drop constraint fka1ad33b9ed30e054;" +
			      "alter table analytic_summary_content drop constraint fk53cb4f2eed30e054;" +
			      "alter table analytic_summary_referer drop constraint fk5bc0f3e2ed30e054;";
		}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
			sql = "drop index idx_identifier ON identifier;" + 
			      "drop index tag_inode_inode ON tag_inode;" +
			      "drop index idx_workflow_4 ON workflow_task;" +
			      "drop index dist_reindex_index1 ON dist_reindex_journal;" +
			      "drop index dist_reindex_index4 ON dist_reindex_journal;" +
			      "drop index idx_permission_reference_2 ON	permission_reference;" +
			      "drop index idx_permission_reference_3 ON	permission_reference;" +
			      "drop index idx_permission_reference_4 ON	permission_reference;" +
			      "drop index idx_permission_reference_5 ON	permission_reference;" +
			      "Alter table structure drop constraint DF_structure_host;" +
			      "Alter table structure drop constraint DF_structure_folder;" +
			      "Alter table structure drop constraint CK_structure_host;" +
			      "drop index idx_trackback_1 ON trackback;" +
			      "drop index idx_relationship_1 ON relationship;" +
			      "drop index idx_relationship_2 ON relationship;" +
                  "drop index idx_field_1 ON field;" +
				  "drop index IDX_FIELD_VELOCITY_STRUCTURE ON field;" +
				  "drop index idx_tree ON tree;" +
				  "drop index idx_tree_1 ON tree;" +
				  "drop index idx_tree_2 ON tree;" +
				  "drop index idx_tree_4 ON tree;" +
				  "drop index idx_tree_5 ON tree;" +
				  "drop index idx_tree_6 ON tree;" +
				  "drop index idx_index_2 ON inode;" +
				  "drop index idx_permission_2 ON permission;" +
				  "drop index idx_permission_3 ON permission;" +
				  "drop index idx_contentlet_1 ON contentlet;" +
				  "drop index idx_contentlet_2 ON contentlet;" +
				  "drop index idx_contentlet_3 ON contentlet;" +
				  "drop index idx_contentlet_4 ON contentlet;" +
				  "drop index idx_analytic_summary_1 on analytic_summary;" +
			      "drop index idx_analytic_summary_404_1 on analytic_summary_404;" +
			      "drop index idx_analytic_summary_visits_1 on analytic_summary_visits;" +
			      "drop index idx_user_clickstream11 on clickstream;" +
			      "drop index idx_dashboard_workstream_2 on analytic_summary_workstream;" +
			      "drop index idx_user_clickstream_404_3 on clickstream_404;" +
			      "drop index idx_user_clickstream_request_3 on clickstream_request;" +
			      "alter table analytic_summary_pages drop constraint fka1ad33b9ed30e054;" +
			      "alter table analytic_summary_content drop constraint fk53cb4f2eed30e054;" +
			      "alter table analytic_summary_referer drop constraint fk5bc0f3e2ed30e054;";
				  
		}
		Connection con=null;
		try {
		  con=DbConnectionFactory.getDataSource().getConnection();
		  con.setAutoCommit(true);
		  List<String> queryList = SQLUtil.tokenize(sql);
		  for(String query : queryList){
		      try {
		          dc.executeStatement(query,con);
		      } catch (Exception e) {
		          Logger.error(this,e.getMessage());
		      }
	      }
		} catch (Exception e) {
			Logger.error(this,e.getMessage(), e);
		}
		finally {
		    try {
		        con.close();
		    }catch(Exception ex) { Logger.error(this,ex.getMessage(), ex); }
		}
	}

	public boolean forceRun() {
      return true;
	}
}
