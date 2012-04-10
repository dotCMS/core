create table calendar_reminder (
   user_id varchar2(255) not null,
   event_id varchar2(36) not null,
   send_date date not null,
   primary key (user_id, event_id, send_date)
);
create table analytic_summary_pages (
   id number(19,0) not null,
   summary_id number(19,0) not null,
   inode varchar2(255),
   hits number(19,0),
   uri varchar2(255),
   primary key (id)
);
create table tag (
   tag_id varchar2(100) not null,
   tagname varchar2(255),
   host_id varchar2(255),
   user_id varchar2(255),
   primary key (tag_id)
);
create table user_comments (
   inode varchar2(36) not null,
   user_id varchar2(255),
   cdate date,
   comment_user_id varchar2(100),
   type varchar2(255),
   method varchar2(255),
   subject varchar2(255),
   ucomment nclob,
   communication_id varchar2(36),
   primary key (inode)
);
create table permission_reference (
   id number(19,0) not null,
   asset_id varchar2(36),
   reference_id varchar2(36),
   permission_type varchar2(100),
   primary key (id),
   unique (asset_id)
);
create table contentlet_version_info (
   identifier varchar2(36) not null,
   lang number(19,0) not null,
   working_inode varchar2(36) not null,
   live_inode varchar2(36),
   deleted number(1,0) not null,
   locked_by varchar2(100),
   locked_on date,
   primary key (identifier, lang)
);
create table fixes_audit (
   id varchar2(36) not null,
   table_name varchar2(255),
   action varchar2(255),
   records_altered number(10,0),
   datetime date,
   primary key (id)
);
create table container_version_info (
   identifier varchar2(36) not null,
   working_inode varchar2(36) not null,
   live_inode varchar2(36),
   deleted number(1,0) not null,
   locked_by varchar2(100),
   locked_on date,
   primary key (identifier)
);
create table trackback (
   id number(19,0) not null,
   asset_identifier varchar2(36),
   title varchar2(255),
   excerpt varchar2(255),
   url varchar2(255),
   blog_name varchar2(255),
   track_date date not null,
   primary key (id)
);
create table mailing_list (
   inode varchar2(36) not null,
   title varchar2(255),
   public_list number(1,0),
   user_id varchar2(255),
   primary key (inode)
);
create table recipient (
   inode varchar2(36) not null,
   name varchar2(255),
   lastname varchar2(255),
   email varchar2(255),
   sent date,
   opened date,
   last_result number(10,0),
   last_message varchar2(255),
   user_id varchar2(100),
   primary key (inode)
);
create table plugin (
   id varchar2(255) not null,
   plugin_name varchar2(255) not null,
   plugin_version varchar2(255) not null,
   author varchar2(255) not null,
   first_deployed_date date not null,
   last_deployed_date date not null,
   primary key (id)
);
create table web_form (
   web_form_id varchar2(36) not null,
   form_type varchar2(255),
   submit_date date,
   prefix varchar2(255),
   first_name varchar2(255),
   middle_initial varchar2(255),
   middle_name varchar2(255),
   full_name varchar2(255),
   organization varchar2(255),
   title varchar2(255),
   last_name varchar2(255),
   address varchar2(255),
   address1 varchar2(255),
   address2 varchar2(255),
   city varchar2(255),
   state varchar2(255),
   zip varchar2(255),
   country varchar2(255),
   phone varchar2(255),
   email varchar2(255),
   custom_fields nclob,
   user_inode varchar2(100),
   categories varchar2(255),
   primary key (web_form_id)
);
create table virtual_link (
   inode varchar2(36) not null,
   title varchar2(255),
   url varchar2(255),
   uri varchar2(255),
   active number(1,0),
   primary key (inode)
);
create table analytic_summary_period (
   id number(19,0) not null,
   full_date date,
   day number(10,0),
   week number(10,0),
   month number(10,0),
   year varchar2(255),
   dayname varchar2(50) not null,
   monthname varchar2(50) not null,
   primary key (id),
   unique (full_date)
);
create table tree (
   child varchar2(36) not null,
   parent varchar2(36) not null,
   relation_type varchar2(64) not null,
   tree_order number(10,0),
   primary key (child, parent, relation_type)
);
create table analytic_summary (
   id number(19,0) not null,
   summary_period_id number(19,0) not null,
   host_id varchar2(36) not null,
   visits number(19,0),
   page_views number(19,0),
   unique_visits number(19,0),
   new_visits number(19,0),
   direct_traffic number(19,0),
   referring_sites number(19,0),
   search_engines number(19,0),
   bounce_rate number(10,0),
   avg_time_on_site date,
   primary key (id),
   unique (summary_period_id, host_id)
);
create table users_cms_roles (
   id varchar2(36) not null,
   user_id varchar2(100) not null,
   role_id varchar2(36) not null,
   primary key (id)
);
create table template (
   inode varchar2(36) not null,
   show_on_menu number(1,0),
   title varchar2(255),
   mod_date date,
   mod_user varchar2(100),
   sort_order number(10,0),
   friendly_name varchar2(255),
   body nclob,
   header nclob,
   footer nclob,
   image varchar2(36),
   identifier varchar2(36),
   primary key (inode)
);
create table analytic_summary_content (
   id number(19,0) not null,
   summary_id number(19,0) not null,
   inode varchar2(255),
   hits number(19,0),
   uri varchar2(255),
   title varchar2(255),
   primary key (id)
);
create table structure (
   inode varchar2(36) not null,
   name varchar2(255),
   description varchar2(255),
   default_structure number(1,0),
   review_interval varchar2(255),
   reviewer_role varchar2(255),
   page_detail varchar2(36),
   structuretype number(10,0),
   system number(1,0),
   fixed number(1,0) not null,
   velocity_var_name varchar2(255),
   url_map_pattern varchar2(512),
   host varchar2(36) not null,
   folder varchar2(36) not null,
   primary key (inode)
);
create table cms_role (
   id varchar2(36) not null,
   role_name varchar2(255) not null,
   description nclob,
   role_key varchar2(255),
   db_fqn varchar2(1000) not null,
   parent varchar2(36) not null,
   edit_permissions number(1,0),
   edit_users number(1,0),
   edit_layouts number(1,0),
   locked number(1,0),
   system number(1,0),
   primary key (id)
);
create table permission (
   id number(19,0) not null,
   permission_type varchar2(500),
   inode_id varchar2(36),
   roleid varchar2(36),
   permission number(10,0),
   primary key (id),
   unique (permission_type, inode_id, roleid)
);
	create table contentlet (inode varchar2(36) not null,
	show_on_menu number(1,0),
	title varchar2(255),
	mod_date date,
	mod_user varchar2(100),
	sort_order number(10,0),
	friendly_name varchar2(255),
	structure_inode varchar2(36),
	last_review date,
	next_review date,
	review_interval varchar2(255),
	disabled_wysiwyg varchar2(255),
	identifier varchar2(36),
	language_id number(19,0),
	date1 date,
	date2 date,
	date3 date,
	date4 date,
	date5 date,
	date6 date,
	date7 date,
	date8 date,
	date9 date,
	date10 date,
	date11 date,
	date12 date,
	date13 date,
	date14 date,
	date15 date,
	date16 date,
	date17 date,
	date18 date,
	date19 date,
	date20 date,
	date21 date,
	date22 date,
	date23 date,
	date24 date,
	date25 date,
	text1 varchar2(255),
	text2 varchar2(255),
	text3 varchar2(255),
	text4 varchar2(255),
	text5 varchar2(255),
	text6 varchar2(255),
	text7 varchar2(255),
	text8 varchar2(255),
	text9 varchar2(255),
	text10 varchar2(255),
	text11 varchar2(255),
	text12 varchar2(255),
	text13 varchar2(255),
	text14 varchar2(255),
	text15 varchar2(255),
	text16 varchar2(255),
	text17 varchar2(255),
	text18 varchar2(255),
	text19 varchar2(255),
	text20 varchar2(255),
	text21 varchar2(255),
	text22 varchar2(255),
	text23 varchar2(255),
	text24 varchar2(255),
	text25 varchar2(255),
	text_area1 nclob,
	text_area2 nclob,
	text_area3 nclob,
	text_area4 nclob,
	text_area5 nclob,
	text_area6 nclob,
	text_area7 nclob,
	text_area8 nclob,
	text_area9 nclob,
	text_area10 nclob,
	text_area11 nclob,
	text_area12 nclob,
	text_area13 nclob,
	text_area14 nclob,
	text_area15 nclob,
	text_area16 nclob,
	text_area17 nclob,
	text_area18 nclob,
	text_area19 nclob,
	text_area20 nclob,
	text_area21 nclob,
	text_area22 nclob,
	text_area23 nclob,
	text_area24 nclob,
	text_area25 nclob,
	integer1 number(19,0),
	integer2 number(19,0),
	integer3 number(19,0),
	integer4 number(19,0),
	integer5 number(19,0),
	integer6 number(19,0),
	integer7 number(19,0),
	integer8 number(19,0),
	integer9 number(19,0),
	integer10 number(19,0),
	integer11 number(19,0),
	integer12 number(19,0),
	integer13 number(19,0),
	integer14 number(19,0),
	integer15 number(19,0),
	integer16 number(19,0),
	integer17 number(19,0),
	integer18 number(19,0),
	integer19 number(19,0),
	integer20 number(19,0),
	integer21 number(19,0),
	integer22 number(19,0),
	integer23 number(19,0),
	integer24 number(19,0),
	integer25 number(19,0),
	"float1" float,
	"float2" float,
	"float3" float,
	"float4" float,
	"float5" float,
	"float6" float,
	"float7" float,
	"float8" float,
	"float9" float,
	"float10" float,
	"float11" float,
	"float12" float,
	"float13" float,
	"float14" float,
	"float15" float,
	"float16" float,
	"float17" float,
	"float18" float,
	"float19" float,
	"float20" float,
	"float21" float,
	"float22" float,
	"float23" float,
	"float24" float,
	"float25" float,
	bool1 number(1,0),
	bool2 number(1,0),
	bool3 number(1,0),
	bool4 number(1,0),
	bool5 number(1,0),
	bool6 number(1,0),
	bool7 number(1,0),
	bool8 number(1,0),
	bool9 number(1,0),
	bool10 number(1,0),
	bool11 number(1,0),
	bool12 number(1,0),
	bool13 number(1,0),
	bool14 number(1,0),
	bool15 number(1,0),
	bool16 number(1,0),
	bool17 number(1,0),
	bool18 number(1,0),
	bool19 number(1,0),
	bool20 number(1,0),
	bool21 number(1,0),
	bool22 number(1,0),
	bool23 number(1,0),
	bool24 number(1,0),
	bool25 number(1,0),
	primary key (inode));
create table analytic_summary_404 (
   id number(19,0) not null,
   summary_period_id number(19,0) not null,
   host_id varchar2(36),
   uri varchar2(255),
   referer_uri varchar2(255),
   primary key (id)
);
create table cms_layouts_portlets (
   id varchar2(36) not null,
   layout_id varchar2(36) not null,
   portlet_id varchar2(100) not null,
   portlet_order number(10,0),
   primary key (id)
);
create table workflow_comment (
   id varchar2(36) not null,
   creation_date date,
   posted_by varchar2(255),
   wf_comment nclob,
   workflowtask_id varchar2(36),
   primary key (id)
);
create table report_asset (
   inode varchar2(36) not null,
   report_name varchar2(255) not null,
   report_description varchar2(1000) not null,
   requires_input number(1,0),
   ds varchar2(100) not null,
   web_form_report number(1,0),
   primary key (inode)
);
create table category (
   inode varchar2(36) not null,
   category_name varchar2(255),
   category_key varchar2(255),
   sort_order number(10,0),
   active number(1,0),
   keywords nclob,
   category_velocity_var_name varchar2(255),
   primary key (inode)
);
create table htmlpage (
   inode varchar2(36) not null,
   show_on_menu number(1,0),
   title varchar2(255),
   mod_date date,
   mod_user varchar2(100),
   sort_order number(10,0),
   friendly_name varchar2(255),
   metadata nclob,
   start_date date,
   end_date date,
   page_url varchar2(255),
   https_required number(1,0),
   redirect varchar2(255),
   identifier varchar2(36),
   seo_description nclob,
   seo_keywords nclob,
   cache_ttl number(19,0),
   template_id varchar2(36),
   primary key (inode)
);
create table chain_link_code (
   id number(19,0) not null,
   class_name varchar2(255) unique,
   code nclob not null,
   last_mod_date date not null,
   language varchar2(255) not null,
   primary key (id)
);
create table analytic_summary_visits (
   id number(19,0) not null,
   summary_period_id number(19,0) not null,
   host_id varchar2(36),
   visit_time date,
   visits number(19,0),
   primary key (id)
);
create table template_version_info (
   identifier varchar2(36) not null,
   working_inode varchar2(36) not null,
   live_inode varchar2(36),
   deleted number(1,0) not null,
   locked_by varchar2(100),
   locked_on date,
   primary key (identifier)
);
create table user_preferences (
   id number(19,0) not null,
   user_id varchar2(100) not null,
   preference varchar2(255),
   pref_value nclob,
   primary key (id)
);
create table language (
   id number(19,0) not null,
   language_code varchar2(5),
   country_code varchar2(255),
   language varchar2(255),
   country varchar2(255),
   primary key (id)
);
create table users_to_delete (
   id number(19,0) not null,
   user_id varchar2(255),
   primary key (id)
);
create table identifier (
   id varchar2(36) not null,
   parent_path varchar2(255),
   asset_name varchar2(255),
   host_inode varchar2(36),
   asset_type varchar2(64),
   primary key (id),
   unique (parent_path, asset_name, host_inode)
);
create table clickstream (
   clickstream_id number(19,0) not null,
   cookie_id varchar2(255),
   user_id varchar2(255),
   start_date date,
   end_date date,
   referer varchar2(255),
   remote_address varchar2(255),
   remote_hostname varchar2(255),
   user_agent varchar2(255),
   bot number(1,0),
   host_id varchar2(36),
   last_page_id varchar2(50),
   first_page_id varchar2(50),
   operating_system varchar2(50),
   browser_name varchar2(50),
   browser_version varchar2(50),
   mobile_device number(1,0),
   number_of_requests number(10,0),
   primary key (clickstream_id)
);
create table multi_tree (
   child varchar2(36) not null,
   parent1 varchar2(36) not null,
   parent2 varchar2(36) not null,
   relation_type varchar2(64),
   tree_order number(10,0),
   primary key (child, parent1, parent2)
);
create table workflow_task (
   id varchar2(36) not null,
   creation_date date,
   mod_date date,
   due_date date,
   created_by varchar2(255),
   assigned_to varchar2(255),
   belongs_to varchar2(255),
   title varchar2(255),
   description nclob,
   status varchar2(255),
   webasset varchar2(255),
   primary key (id)
);
create table tag_inode (
   tag_id varchar2(100) not null,
   inode varchar2(100) not null,
   primary key (tag_id, inode)
);
create table click (
   inode varchar2(36) not null,
   link varchar2(255),
   click_count number(10,0),
   primary key (inode)
);
create table challenge_question (
   cquestionid number(19,0) not null,
   cqtext varchar2(255),
   primary key (cquestionid)
);
create table file_asset (
   inode varchar2(36) not null,
   file_name varchar2(255),
   file_size number(10,0),
   width number(10,0),
   height number(10,0),
   mime_type varchar2(255),
   author varchar2(255),
   publish_date date,
   show_on_menu number(1,0),
   title varchar2(255),
   friendly_name varchar2(255),
   mod_date date,
   mod_user varchar2(100),
   sort_order number(10,0),
   identifier varchar2(36),
   primary key (inode)
);
create table layouts_cms_roles (
   id varchar2(36) not null,
   layout_id varchar2(36) not null,
   role_id varchar2(36) not null,
   primary key (id)
);
create table clickstream_request (
   clickstream_request_id number(19,0) not null,
   clickstream_id number(19,0),
   server_name varchar2(255),
   protocol varchar2(255),
   server_port number(10,0),
   request_uri varchar2(255),
   request_order number(10,0),
   query_string nclob,
   language_id number(19,0),
   timestampper date,
   host_id varchar2(36),
   associated_identifier varchar2(36),
   primary key (clickstream_request_id)
);
create table content_rating (
   id number(19,0) not null,
   rating float,
   user_id varchar2(255),
   session_id varchar2(255),
   identifier varchar2(36),
   rating_date date,
   user_ip varchar2(255),
   long_live_cookie_id varchar2(255),
   primary key (id)
);
create table chain_state (
   id number(19,0) not null,
   chain_id number(19,0) not null,
   link_code_id number(19,0) not null,
   state_order number(19,0) not null,
   primary key (id)
);
create table analytic_summary_workstream (
   id number(19,0) not null,
   inode varchar2(255),
   asset_type varchar2(255),
   mod_user_id varchar2(255),
   host_id varchar2(36),
   mod_date date,
   action varchar2(255),
   name varchar2(255),
   primary key (id)
);
create table dashboard_user_preferences (
   id number(19,0) not null,
   summary_404_id number(19,0),
   user_id varchar2(255),
   ignored number(1,0),
   mod_date date,
   primary key (id)
);
create table campaign (
   inode varchar2(36) not null,
   title varchar2(255),
   from_email varchar2(255),
   from_name varchar2(255),
   subject varchar2(255),
   message nclob,
   user_id varchar2(255),
   start_date date,
   completed_date date,
   active number(1,0),
   locked number(1,0),
   sends_per_hour varchar2(15),
   sendemail number(1,0),
   communicationinode varchar2(36),
   userfilterinode varchar2(36),
   sendto varchar2(15),
   isrecurrent number(1,0),
   wassent number(1,0),
   expiration_date date,
   parent_campaign varchar2(36),
   primary key (inode)
);
create table htmlpage_version_info (
   identifier varchar2(36) not null,
   working_inode varchar2(36) not null,
   live_inode varchar2(36),
   deleted number(1,0) not null,
   locked_by varchar2(100),
   locked_on date,
   primary key (identifier)
);
create table workflowtask_files (
   id varchar2(36) not null,
   workflowtask_id varchar2(36) not null,
   file_inode varchar2(36) not null,
   primary key (id)
);
create table analytic_summary_referer (
   id number(19,0) not null,
   summary_id number(19,0) not null,
   hits number(19,0),
   uri varchar2(255),
   primary key (id)
);
create table containers (
   inode varchar2(36) not null,
   code nclob,
   pre_loop nclob,
   post_loop nclob,
   show_on_menu number(1,0),
   title varchar2(255),
   mod_date date,
   mod_user varchar2(100),
   sort_order number(10,0),
   friendly_name varchar2(255),
   max_contentlets number(10,0),
   use_div number(1,0),
   staticify number(1,0),
   sort_contentlets_by varchar2(255),
   lucene_query nclob,
   notes varchar2(255),
   identifier varchar2(36),
   structure_inode varchar2(36),
   primary key (inode)
);
create table communication (
   inode varchar2(36) not null,
   title varchar2(255),
   trackback_link_inode varchar2(36),
   communication_type varchar2(255),
   from_name varchar2(255),
   from_email varchar2(255),
   email_subject varchar2(255),
   html_page_inode varchar2(36),
   text_message nclob,
   mod_date date,
   modified_by varchar2(255),
   ext_comm_id varchar2(255),
   primary key (inode)
);
create table fileasset_version_info (
   identifier varchar2(36) not null,
   working_inode varchar2(36) not null,
   live_inode varchar2(36),
   deleted number(1,0) not null,
   locked_by varchar2(100),
   locked_on date not null,
   primary key (identifier)
);
create table workflow_history (
   id varchar2(36) not null,
   creation_date date,
   made_by varchar2(255),
   change_desc nclob,
   workflowtask_id varchar2(36),
   workflow_action_id varchar2(36),
   workflow_step_id varchar2(36),
   primary key (id)
);
create table host_variable (
   id varchar2(36) not null,
   host_id varchar2(36),
   variable_name varchar2(255),
   variable_key varchar2(255),
   variable_value varchar2(255),
   user_id varchar2(255),
   last_mod_date date,
   primary key (id)
);
create table links (
   inode varchar2(36) not null,
   show_on_menu number(1,0),
   title varchar2(255),
   mod_date date,
   mod_user varchar2(100),
   sort_order number(10,0),
   friendly_name varchar2(255),
   identifier varchar2(36),
   protocal varchar2(100),
   url varchar2(255),
   target varchar2(100),
   internal_link_identifier varchar2(36),
   link_type varchar2(255),
   link_code nclob,
   primary key (inode)
);
create table user_proxy (
   inode varchar2(36) not null,
   user_id varchar2(255),
   prefix varchar2(255),
   suffix varchar2(255),
   title varchar2(255),
   school varchar2(255),
   how_heard varchar2(255),
   company varchar2(255),
   long_lived_cookie varchar2(255),
   website varchar2(255),
   graduation_year number(10,0),
   organization varchar2(255),
   mail_subscription number(1,0),
   var1 varchar2(255),
   var2 varchar2(255),
   var3 varchar2(255),
   var4 varchar2(255),
   var5 varchar2(255),
   var6 varchar2(255),
   var7 varchar2(255),
   var8 varchar2(255),
   var9 varchar2(255),
   var10 varchar2(255),
   var11 varchar2(255),
   var12 varchar2(255),
   var13 varchar2(255),
   var14 varchar2(255),
   var15 varchar2(255),
   var16 varchar2(255),
   var17 varchar2(255),
   var18 varchar2(255),
   var19 varchar2(255),
   var20 varchar2(255),
   var21 varchar2(255),
   var22 varchar2(255),
   var23 varchar2(255),
   var24 varchar2(255),
   var25 varchar2(255),
   last_result number(10,0),
   last_message varchar2(255),
   no_click_tracking number(1,0),
   cquestionid varchar2(255),
   cqanswer varchar2(255),
   chapter_officer varchar2(255),
   primary key (inode),
   unique (user_id)
);
create table chain_state_parameter (
   id number(19,0) not null,
   chain_state_id number(19,0) not null,
   name varchar2(255) not null,
   value varchar2(255) not null,
   primary key (id)
);
create table field (
   inode varchar2(36) not null,
   structure_inode varchar2(255),
   field_name varchar2(255),
   field_type varchar2(255),
   field_relation_type varchar2(255),
   field_contentlet varchar2(255),
   required number(1,0),
   indexed number(1,0),
   listed number(1,0),
   velocity_var_name varchar2(255),
   sort_order number(10,0),
   field_values nclob,
   regex_check varchar2(255),
   hint varchar2(255),
   default_value varchar2(255),
   fixed number(1,0),
   read_only number(1,0),
   searchable number(1,0),
   unique_ number(1,0),
   primary key (inode)
);
create table relationship (
   inode varchar2(36) not null,
   parent_structure_inode varchar2(255),
   child_structure_inode varchar2(255),
   parent_relation_name varchar2(255),
   child_relation_name varchar2(255),
   relation_type_value varchar2(255),
   cardinality number(10,0),
   parent_required number(1,0),
   child_required number(1,0),
   fixed number(1,0),
   primary key (inode)
);
create table folder (
   inode varchar2(36) not null,
   name varchar2(255),
   title varchar2(255) not null,
   show_on_menu number(1,0),
   sort_order number(10,0),
   files_masks varchar2(255),
   identifier varchar2(36),
   default_file_type varchar2(36),
   primary key (inode)
);
create table clickstream_404 (
   clickstream_404_id number(19,0) not null,
   referer_uri varchar2(255),
   query_string nclob,
   request_uri varchar2(255),
   user_id varchar2(255),
   host_id varchar2(36),
   timestampper date,
   primary key (clickstream_404_id)
);
create table cms_layout (
   id varchar2(36) not null,
   layout_name varchar2(255) not null,
   description varchar2(255),
   tab_order number(10,0),
   primary key (id)
);
create table field_variable (
   id varchar2(36) not null,
   field_id varchar2(36),
   variable_name varchar2(255),
   variable_key varchar2(255),
   variable_value nclob,
   user_id varchar2(255),
   last_mod_date date,
   primary key (id)
);
create table report_parameter (
   inode varchar2(36) not null,
   report_inode varchar2(36),
   parameter_description varchar2(1000),
   parameter_name varchar2(255),
   class_type varchar2(250),
   default_value varchar2(4000),
   primary key (inode),
   unique (report_inode, parameter_name)
);
create table chain (
   id number(19,0) not null,
   key_name varchar2(255) unique,
   name varchar2(255) not null,
   success_value varchar2(255) not null,
   failure_value varchar2(255) not null,
   primary key (id)
);
create table link_version_info (
   identifier varchar2(36) not null,
   working_inode varchar2(36) not null,
   live_inode varchar2(36),
   deleted number(1,0) not null,
   locked_by varchar2(100),
   locked_on date,
   primary key (identifier)
);
create table template_containers (
   id varchar2(36) not null,
   template_id varchar2(36) not null,
   container_id varchar2(36) not null,
   primary key (id)
);
create table user_filter (
   inode varchar2(36) not null,
   title varchar2(255),
   firstname varchar2(100),
   middlename varchar2(100),
   lastname varchar2(100),
   emailaddress varchar2(100),
   birthdaytypesearch varchar2(100),
   birthday date,
   birthdayfrom date,
   birthdayto date,
   lastlogintypesearch varchar2(100),
   lastloginsince varchar2(100),
   loginfrom date,
   loginto date,
   createdtypesearch varchar2(100),
   createdsince varchar2(100),
   createdfrom date,
   createdto date,
   lastvisittypesearch varchar2(100),
   lastvisitsince varchar2(100),
   lastvisitfrom date,
   lastvisitto date,
   city varchar2(100),
   state varchar2(100),
   country varchar2(100),
   zip varchar2(100),
   cell varchar2(100),
   phone varchar2(100),
   fax varchar2(100),
   active_ varchar2(255),
   tagname varchar2(255),
   var1 varchar2(255),
   var2 varchar2(255),
   var3 varchar2(255),
   var4 varchar2(255),
   var5 varchar2(255),
   var6 varchar2(255),
   var7 varchar2(255),
   var8 varchar2(255),
   var9 varchar2(255),
   var10 varchar2(255),
   var11 varchar2(255),
   var12 varchar2(255),
   var13 varchar2(255),
   var14 varchar2(255),
   var15 varchar2(255),
   var16 varchar2(255),
   var17 varchar2(255),
   var18 varchar2(255),
   var19 varchar2(255),
   var20 varchar2(255),
   var21 varchar2(255),
   var22 varchar2(255),
   var23 varchar2(255),
   var24 varchar2(255),
   var25 varchar2(255),
   categories varchar2(255),
   primary key (inode)
);
create table inode (
   inode varchar2(36) not null,
   owner varchar2(255),
   idate date,
   type varchar2(64),
   primary key (inode)
);
alter table analytic_summary_pages add constraint fka1ad33b9ed30e054 foreign key (summary_id) references analytic_summary;
create index idx_user_comments_1 on user_comments (user_id);
alter table user_comments add constraint fkdf1b37e85fb51eb foreign key (inode) references inode;
create index idx_trackback_2 on trackback (url);
create index idx_trackback_1 on trackback (asset_identifier);
create index idx_mailinglist_1 on mailing_list (user_id);
alter table mailing_list add constraint fk7bc2cd925fb51eb foreign key (inode) references inode;
create index idx_communication_user_id on recipient (user_id);
create index idx_recipiets_1 on recipient (email);
create index idx_recipiets_2 on recipient (sent);
alter table recipient add constraint fk30e172195fb51eb foreign key (inode) references inode;
create index idx_user_webform_1 on web_form (form_type);
create index idx_virtual_link_1 on virtual_link (url);
alter table virtual_link add constraint fkd844f8ae5fb51eb foreign key (inode) references inode;
create index idx_analytic_summary_period_4 on analytic_summary_period (month);
create index idx_analytic_summary_period_3 on analytic_summary_period (week);
create index idx_analytic_summary_period_2 on analytic_summary_period (day);
create index idx_analytic_summary_period_5 on analytic_summary_period (year);
create index idx_analytic_summary_1 on analytic_summary (host_id);
create index idx_analytic_summary_2 on analytic_summary (visits);
create index idx_analytic_summary_3 on analytic_summary (page_views);
alter table analytic_summary add constraint fk9e1a7f4b7b46300 foreign key (summary_period_id) references analytic_summary_period;
alter table template add constraint fkb13acc7a5fb51eb foreign key (inode) references inode;
alter table analytic_summary_content add constraint fk53cb4f2eed30e054 foreign key (summary_id) references analytic_summary;
alter table structure add constraint fk89d2d735fb51eb foreign key (inode) references inode;
create index idx_permission_2 on permission (permission_type, inode_id);
create index idx_permission_3 on permission (roleid);
alter table contentlet add constraint fkfc4ef025fb51eb foreign key (inode) references inode;
create index idx_analytic_summary_404_1 on analytic_summary_404 (host_id);
alter table analytic_summary_404 add constraint fk7050866db7b46300 foreign key (summary_period_id) references analytic_summary_period;
alter table report_asset add constraint fk3765ec255fb51eb foreign key (inode) references inode;
create index idx_category_1 on category (category_name);
create index idx_category_2 on category (category_key);
alter table category add constraint fk302bcfe5fb51eb foreign key (inode) references inode;
alter table htmlpage add constraint fkebf39cba5fb51eb foreign key (inode) references inode;
create index idx_analytic_summary_visits_2 on analytic_summary_visits (visit_time);
create index idx_analytic_summary_visits_1 on analytic_summary_visits (host_id);
alter table analytic_summary_visits add constraint fk9eac9733b7b46300 foreign key (summary_period_id) references analytic_summary_period;
create index idx_preference_1 on user_preferences (preference);
create index idx_user_clickstream11 on clickstream (host_id);
create index idx_user_clickstream12 on clickstream (last_page_id);
create index idx_user_clickstream15 on clickstream (browser_name);
create index idx_user_clickstream_2 on clickstream (user_id);
create index idx_user_clickstream16 on clickstream (browser_version);
create index idx_user_clickstream_1 on clickstream (cookie_id);
create index idx_user_clickstream13 on clickstream (first_page_id);
create index idx_user_clickstream14 on clickstream (operating_system);
create index idx_user_clickstream17 on clickstream (remote_address);
create index idx_multitree_1 on multi_tree (relation_type);
create index idx_workflow_4 on workflow_task (webasset);
create index idx_workflow_5 on workflow_task (created_by);
create index idx_workflow_2 on workflow_task (belongs_to);
create index idx_workflow_3 on workflow_task (status);
create index idx_workflow_1 on workflow_task (assigned_to);
create index idx_click_1 on click (link);
alter table click add constraint fk5a5c5885fb51eb foreign key (inode) references inode;
alter table file_asset add constraint fk7ed2366d5fb51eb foreign key (inode) references inode;
create index idx_user_clickstream_request_2 on clickstream_request (request_uri);
create index idx_user_clickstream_request_1 on clickstream_request (clickstream_id);
create index idx_user_clickstream_request_4 on clickstream_request (timestampper);
create index idx_user_clickstream_request_3 on clickstream_request (associated_identifier);
create index idx_dashboard_workstream_2 on analytic_summary_workstream (host_id);
create index idx_dashboard_workstream_1 on analytic_summary_workstream (mod_user_id);
create index idx_dashboard_workstream_3 on analytic_summary_workstream (mod_date);
create index idx_dashboard_prefs_2 on dashboard_user_preferences (user_id);
alter table dashboard_user_preferences add constraint fk496242cfd12c0c3b foreign key (summary_404_id) references analytic_summary_404;
create index idx_campaign_4 on campaign (expiration_date);
create index idx_campaign_3 on campaign (completed_date);
create index idx_campaign_2 on campaign (start_date);
create index idx_campaign_1 on campaign (user_id);
alter table campaign add constraint fkf7a901105fb51eb foreign key (inode) references inode;
alter table analytic_summary_referer add constraint fk5bc0f3e2ed30e054 foreign key (summary_id) references analytic_summary;
alter table containers add constraint fk8a844125fb51eb foreign key (inode) references inode;
alter table communication add constraint fkc24acfd65fb51eb foreign key (inode) references inode;
alter table links add constraint fk6234fb95fb51eb foreign key (inode) references inode;
alter table user_proxy add constraint fk7327d4fa5fb51eb foreign key (inode) references inode;
create index idx_field_1 on field (structure_inode);
alter table field add constraint fk5cea0fa5fb51eb foreign key (inode) references inode;
create index idx_relationship_1 on relationship (parent_structure_inode);
create index idx_relationship_2 on relationship (child_structure_inode);
alter table relationship add constraint fkf06476385fb51eb foreign key (inode) references inode;
create index idx_folder_1 on folder (name);
alter table folder add constraint fkb45d1c6e5fb51eb foreign key (inode) references inode;
create index idx_user_clickstream_404_2 on clickstream_404 (user_id);
create index idx_user_clickstream_404_3 on clickstream_404 (host_id);
create index idx_user_clickstream_404_1 on clickstream_404 (request_uri);
alter table report_parameter add constraint fk22da125e5fb51eb foreign key (inode) references inode;
alter table user_filter add constraint fke042126c5fb51eb foreign key (inode) references inode;
create index idx_index_1 on inode (type);
create sequence summary_seq;
create sequence user_preferences_seq;
create sequence dashboard_usrpref_seq;
create sequence chain_state_seq;
create sequence trackback_sequence;
create sequence language_seq;
create sequence permission_reference_seq;
create sequence summary_visits_seq;
create sequence chain_link_code_seq;
create sequence clickstream_seq;
create sequence summary_404_seq;
create sequence content_rating_sequence;
create sequence summary_content_seq;
create sequence summary_pages_seq;
create sequence chain_seq;
create sequence summary_referer_seq;
create sequence workstream_seq;
create sequence summary_period_seq;
create sequence clickstream_request_seq;
create sequence clickstream_404_seq;
create sequence chain_state_parameter_seq;
create sequence user_to_delete_seq;
create sequence permission_seq;
