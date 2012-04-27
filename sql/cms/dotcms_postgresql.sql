create table calendar_reminder (
   user_id varchar(255) not null,
   event_id varchar(36) not null,
   send_date timestamp not null,
   primary key (user_id, event_id, send_date)
);
create table analytic_summary_pages (
   id int8 not null,
   summary_id int8 not null,
   inode varchar(255),
   hits int8,
   uri varchar(255),
   primary key (id)
);
create table tag (
   tag_id varchar(100) not null,
   tagname varchar(255),
   host_id varchar(255),
   user_id varchar(255),
   primary key (tag_id)
);
create table user_comments (
   inode varchar(36) not null,
   user_id varchar(255),
   cdate timestamp,
   comment_user_id varchar(100),
   type varchar(255),
   method varchar(255),
   subject varchar(255),
   ucomment text,
   communication_id varchar(36),
   primary key (inode)
);
create table permission_reference (
   id int8 not null,
   asset_id varchar(36),
   reference_id varchar(36),
   permission_type varchar(100),
   primary key (id),
   unique (asset_id)
);
create table contentlet_version_info (
   identifier varchar(36) not null,
   lang int8 not null,
   working_inode varchar(36) not null,
   live_inode varchar(36),
   deleted bool not null,
   locked_by varchar(100),
   locked_on timestamp,
   primary key (identifier, lang)
);
create table fixes_audit (
   id varchar(36) not null,
   table_name varchar(255),
   action varchar(255),
   records_altered int4,
   datetime date,
   primary key (id)
);
create table container_version_info (
   identifier varchar(36) not null,
   working_inode varchar(36) not null,
   live_inode varchar(36),
   deleted bool not null,
   locked_by varchar(100),
   locked_on date,
   primary key (identifier)
);
create table trackback (
   id int8 not null,
   asset_identifier varchar(36),
   title varchar(255),
   excerpt varchar(255),
   url varchar(255),
   blog_name varchar(255),
   track_date timestamp not null,
   primary key (id)
);
create table mailing_list (
   inode varchar(36) not null,
   title varchar(255),
   public_list bool,
   user_id varchar(255),
   primary key (inode)
);
create table recipient (
   inode varchar(36) not null,
   name varchar(255),
   lastname varchar(255),
   email varchar(255),
   sent timestamp,
   opened timestamp,
   last_result int4,
   last_message varchar(255),
   user_id varchar(100),
   primary key (inode)
);
create table plugin (
   id varchar(255) not null,
   plugin_name varchar(255) not null,
   plugin_version varchar(255) not null,
   author varchar(255) not null,
   first_deployed_date date not null,
   last_deployed_date date not null,
   primary key (id)
);
create table web_form (
   web_form_id varchar(36) not null,
   form_type varchar(255),
   submit_date timestamp,
   prefix varchar(255),
   first_name varchar(255),
   middle_initial varchar(255),
   middle_name varchar(255),
   full_name varchar(255),
   organization varchar(255),
   title varchar(255),
   last_name varchar(255),
   address varchar(255),
   address1 varchar(255),
   address2 varchar(255),
   city varchar(255),
   state varchar(255),
   zip varchar(255),
   country varchar(255),
   phone varchar(255),
   email varchar(255),
   custom_fields text,
   user_inode varchar(100),
   categories varchar(255),
   primary key (web_form_id)
);
create table virtual_link (
   inode varchar(36) not null,
   title varchar(255),
   url varchar(255),
   uri varchar(255),
   active bool,
   primary key (inode)
);
create table analytic_summary_period (
   id int8 not null,
   full_date timestamp,
   day int4,
   week int4,
   month int4,
   year varchar(255),
   dayname varchar(50) not null,
   monthname varchar(50) not null,
   primary key (id),
   unique (full_date)
);
create table tree (
   child varchar(36) not null,
   parent varchar(36) not null,
   relation_type varchar(64) not null,
   tree_order int4,
   primary key (child, parent, relation_type)
);
create table analytic_summary (
   id int8 not null,
   summary_period_id int8 not null,
   host_id varchar(36) not null,
   visits int8,
   page_views int8,
   unique_visits int8,
   new_visits int8,
   direct_traffic int8,
   referring_sites int8,
   search_engines int8,
   bounce_rate int4,
   avg_time_on_site timestamp,
   primary key (id),
   unique (summary_period_id, host_id)
);
create table users_cms_roles (
   id varchar(36) not null,
   user_id varchar(100) not null,
   role_id varchar(36) not null,
   primary key (id)
);
create table template (
   inode varchar(36) not null,
   show_on_menu bool,
   title varchar(255),
   mod_date timestamp,
   mod_user varchar(100),
   sort_order int4,
   friendly_name varchar(255),
   body text,
   header text,
   footer text,
   image varchar(36),
   identifier varchar(36),
   primary key (inode)
);
create table analytic_summary_content (
   id int8 not null,
   summary_id int8 not null,
   inode varchar(255),
   hits int8,
   uri varchar(255),
   title varchar(255),
   primary key (id)
);
create table structure (
   inode varchar(36) not null,
   name varchar(255),
   description varchar(255),
   default_structure bool,
   review_interval varchar(255),
   reviewer_role varchar(255),
   page_detail varchar(36),
   structuretype int4,
   system bool,
   fixed bool not null,
   velocity_var_name varchar(255),
   url_map_pattern varchar(512),
   host varchar(36) not null,
   folder varchar(36) not null,
   primary key (inode)
);
create table cms_role (
   id varchar(36) not null,
   role_name varchar(255) not null,
   description text,
   role_key varchar(255),
   db_fqn varchar(1000) not null,
   parent varchar(36) not null,
   edit_permissions bool,
   edit_users bool,
   edit_layouts bool,
   locked bool,
   system bool,
   primary key (id)
);
create table permission (
   id int8 not null,
   permission_type varchar(500),
   inode_id varchar(36),
   roleid varchar(36),
   permission int4,
   primary key (id),
   unique (permission_type, inode_id, roleid)
);
	create table contentlet (inode varchar(36) not null,
	show_on_menu bool,
	title varchar(255),
	mod_date timestamp,
	mod_user varchar(100),
	sort_order int4,
	friendly_name varchar(255),
	structure_inode varchar(36),
	last_review timestamp,
	next_review timestamp,
	review_interval varchar(255),
	disabled_wysiwyg varchar(255),
	identifier varchar(36),
	language_id int8,
	date1 timestamp,
	date2 timestamp,
	date3 timestamp,
	date4 timestamp,
	date5 timestamp,
	date6 timestamp,
	date7 timestamp,
	date8 timestamp,
	date9 timestamp,
	date10 timestamp,
	date11 timestamp,
	date12 timestamp,
	date13 timestamp,
	date14 timestamp,
	date15 timestamp,
	date16 timestamp,
	date17 timestamp,
	date18 timestamp,
	date19 timestamp,
	date20 timestamp,
	date21 timestamp,
	date22 timestamp,
	date23 timestamp,
	date24 timestamp,
	date25 timestamp,
	text1 varchar(255),
	text2 varchar(255),
	text3 varchar(255),
	text4 varchar(255),
	text5 varchar(255),
	text6 varchar(255),
	text7 varchar(255),
	text8 varchar(255),
	text9 varchar(255),
	text10 varchar(255),
	text11 varchar(255),
	text12 varchar(255),
	text13 varchar(255),
	text14 varchar(255),
	text15 varchar(255),
	text16 varchar(255),
	text17 varchar(255),
	text18 varchar(255),
	text19 varchar(255),
	text20 varchar(255),
	text21 varchar(255),
	text22 varchar(255),
	text23 varchar(255),
	text24 varchar(255),
	text25 varchar(255),
	text_area1 text,
	text_area2 text,
	text_area3 text,
	text_area4 text,
	text_area5 text,
	text_area6 text,
	text_area7 text,
	text_area8 text,
	text_area9 text,
	text_area10 text,
	text_area11 text,
	text_area12 text,
	text_area13 text,
	text_area14 text,
	text_area15 text,
	text_area16 text,
	text_area17 text,
	text_area18 text,
	text_area19 text,
	text_area20 text,
	text_area21 text,
	text_area22 text,
	text_area23 text,
	text_area24 text,
	text_area25 text,
	integer1 int8,
	integer2 int8,
	integer3 int8,
	integer4 int8,
	integer5 int8,
	integer6 int8,
	integer7 int8,
	integer8 int8,
	integer9 int8,
	integer10 int8,
	integer11 int8,
	integer12 int8,
	integer13 int8,
	integer14 int8,
	integer15 int8,
	integer16 int8,
	integer17 int8,
	integer18 int8,
	integer19 int8,
	integer20 int8,
	integer21 int8,
	integer22 int8,
	integer23 int8,
	integer24 int8,
	integer25 int8,
	"float1" float4,
	"float2" float4,
	"float3" float4,
	"float4" float4,
	"float5" float4,
	"float6" float4,
	"float7" float4,
	"float8" float4,
	"float9" float4,
	"float10" float4,
	"float11" float4,
	"float12" float4,
	"float13" float4,
	"float14" float4,
	"float15" float4,
	"float16" float4,
	"float17" float4,
	"float18" float4,
	"float19" float4,
	"float20" float4,
	"float21" float4,
	"float22" float4,
	"float23" float4,
	"float24" float4,
	"float25" float4,
	bool1 bool,
	bool2 bool,
	bool3 bool,
	bool4 bool,
	bool5 bool,
	bool6 bool,
	bool7 bool,
	bool8 bool,
	bool9 bool,
	bool10 bool,
	bool11 bool,
	bool12 bool,
	bool13 bool,
	bool14 bool,
	bool15 bool,
	bool16 bool,
	bool17 bool,
	bool18 bool,
	bool19 bool,
	bool20 bool,
	bool21 bool,
	bool22 bool,
	bool23 bool,
	bool24 bool,
	bool25 bool,
	primary key (inode));
create table analytic_summary_404 (
   id int8 not null,
   summary_period_id int8 not null,
   host_id varchar(36),
   uri varchar(255),
   referer_uri varchar(255),
   primary key (id)
);
create table cms_layouts_portlets (
   id varchar(36) not null,
   layout_id varchar(36) not null,
   portlet_id varchar(100) not null,
   portlet_order int4,
   primary key (id)
);
create table workflow_comment (
   id varchar(36) not null,
   creation_date timestamp,
   posted_by varchar(255),
   wf_comment text,
   workflowtask_id varchar(36),
   primary key (id)
);
create table report_asset (
   inode varchar(36) not null,
   report_name varchar(255) not null,
   report_description varchar(1000) not null,
   requires_input bool,
   ds varchar(100) not null,
   web_form_report bool,
   primary key (inode)
);
create table category (
   inode varchar(36) not null,
   category_name varchar(255),
   category_key varchar(255),
   sort_order int4,
   active bool,
   keywords text,
   category_velocity_var_name varchar(255),
   primary key (inode)
);
create table htmlpage (
   inode varchar(36) not null,
   show_on_menu bool,
   title varchar(255),
   mod_date timestamp,
   mod_user varchar(100),
   sort_order int4,
   friendly_name varchar(255),
   metadata text,
   start_date timestamp,
   end_date timestamp,
   page_url varchar(255),
   https_required bool,
   redirect varchar(255),
   identifier varchar(36),
   seo_description text,
   seo_keywords text,
   cache_ttl int8,
   template_id varchar(36),
   primary key (inode)
);
create table chain_link_code (
   id int8 not null,
   class_name varchar(255) unique,
   code text not null,
   last_mod_date date not null,
   language varchar(255) not null,
   primary key (id)
);
create table analytic_summary_visits (
   id int8 not null,
   summary_period_id int8 not null,
   host_id varchar(36),
   visit_time timestamp,
   visits int8,
   primary key (id)
);
create table template_version_info (
   identifier varchar(36) not null,
   working_inode varchar(36) not null,
   live_inode varchar(36),
   deleted bool not null,
   locked_by varchar(100),
   locked_on date,
   primary key (identifier)
);
create table user_preferences (
   id int8 not null,
   user_id varchar(100) not null,
   preference varchar(255),
   pref_value text,
   primary key (id)
);
create table language (
   id int8 not null,
   language_code varchar(5),
   country_code varchar(255),
   language varchar(255),
   country varchar(255),
   primary key (id)
);
create table users_to_delete (
   id int8 not null,
   user_id varchar(255),
   primary key (id)
);
create table identifier (
   id varchar(36) not null,
   parent_path varchar(255),
   asset_name varchar(255),
   host_inode varchar(36),
   asset_type varchar(64),
   primary key (id),
   unique (parent_path, asset_name, host_inode)
);
create table clickstream (
   clickstream_id int8 not null,
   cookie_id varchar(255),
   user_id varchar(255),
   start_date timestamp,
   end_date timestamp,
   referer varchar(255),
   remote_address varchar(255),
   remote_hostname varchar(255),
   user_agent varchar(255),
   bot bool,
   host_id varchar(36),
   last_page_id varchar(50),
   first_page_id varchar(50),
   operating_system varchar(50),
   browser_name varchar(50),
   browser_version varchar(50),
   mobile_device bool,
   number_of_requests int4,
   primary key (clickstream_id)
);
create table multi_tree (
   child varchar(36) not null,
   parent1 varchar(36) not null,
   parent2 varchar(36) not null,
   relation_type varchar(64),
   tree_order int4,
   primary key (child, parent1, parent2)
);
create table workflow_task (
   id varchar(36) not null,
   creation_date timestamp,
   mod_date timestamp,
   due_date timestamp,
   created_by varchar(255),
   assigned_to varchar(255),
   belongs_to varchar(255),
   title varchar(255),
   description text,
   status varchar(255),
   webasset varchar(255),
   primary key (id)
);
create table tag_inode (
   tag_id varchar(100) not null,
   inode varchar(100) not null,
   primary key (tag_id, inode)
);
create table click (
   inode varchar(36) not null,
   link varchar(255),
   click_count int4,
   primary key (inode)
);
create table challenge_question (
   cquestionid int8 not null,
   cqtext varchar(255),
   primary key (cquestionid)
);
create table file_asset (
   inode varchar(36) not null,
   file_name varchar(255),
   file_size int4,
   width int4,
   height int4,
   mime_type varchar(255),
   author varchar(255),
   publish_date timestamp,
   show_on_menu bool,
   title varchar(255),
   friendly_name varchar(255),
   mod_date timestamp,
   mod_user varchar(100),
   sort_order int4,
   identifier varchar(36),
   primary key (inode)
);
create table layouts_cms_roles (
   id varchar(36) not null,
   layout_id varchar(36) not null,
   role_id varchar(36) not null,
   primary key (id)
);
create table clickstream_request (
   clickstream_request_id int8 not null,
   clickstream_id int8,
   server_name varchar(255),
   protocol varchar(255),
   server_port int4,
   request_uri varchar(255),
   request_order int4,
   query_string text,
   language_id int8,
   timestampper timestamp,
   host_id varchar(36),
   associated_identifier varchar(36),
   primary key (clickstream_request_id)
);
create table content_rating (
   id int8 not null,
   rating float4,
   user_id varchar(255),
   session_id varchar(255),
   identifier varchar(36),
   rating_date timestamp,
   user_ip varchar(255),
   long_live_cookie_id varchar(255),
   primary key (id)
);
create table chain_state (
   id int8 not null,
   chain_id int8 not null,
   link_code_id int8 not null,
   state_order int8 not null,
   primary key (id)
);
create table analytic_summary_workstream (
   id int8 not null,
   inode varchar(255),
   asset_type varchar(255),
   mod_user_id varchar(255),
   host_id varchar(36),
   mod_date timestamp,
   action varchar(255),
   name varchar(255),
   primary key (id)
);
create table dashboard_user_preferences (
   id int8 not null,
   summary_404_id int8,
   user_id varchar(255),
   ignored bool,
   mod_date timestamp,
   primary key (id)
);
create table campaign (
   inode varchar(36) not null,
   title varchar(255),
   from_email varchar(255),
   from_name varchar(255),
   subject varchar(255),
   message text,
   user_id varchar(255),
   start_date timestamp,
   completed_date timestamp,
   active bool,
   locked bool,
   sends_per_hour varchar(15),
   sendemail bool,
   communicationinode varchar(36),
   userfilterinode varchar(36),
   sendto varchar(15),
   isrecurrent bool,
   wassent bool,
   expiration_date timestamp,
   parent_campaign varchar(36),
   primary key (inode)
);
create table htmlpage_version_info (
   identifier varchar(36) not null,
   working_inode varchar(36) not null,
   live_inode varchar(36),
   deleted bool not null,
   locked_by varchar(100),
   locked_on date,
   primary key (identifier)
);
create table workflowtask_files (
   id varchar(36) not null,
   workflowtask_id varchar(36) not null,
   file_inode varchar(36) not null,
   primary key (id)
);
create table analytic_summary_referer (
   id int8 not null,
   summary_id int8 not null,
   hits int8,
   uri varchar(255),
   primary key (id)
);
create table containers (
   inode varchar(36) not null,
   code text,
   pre_loop text,
   post_loop text,
   show_on_menu bool,
   title varchar(255),
   mod_date timestamp,
   mod_user varchar(100),
   sort_order int4,
   friendly_name varchar(255),
   max_contentlets int4,
   use_div bool,
   staticify bool,
   sort_contentlets_by varchar(255),
   lucene_query text,
   notes varchar(255),
   identifier varchar(36),
   structure_inode varchar(36),
   primary key (inode)
);
create table communication (
   inode varchar(36) not null,
   title varchar(255),
   trackback_link_inode varchar(36),
   communication_type varchar(255),
   from_name varchar(255),
   from_email varchar(255),
   email_subject varchar(255),
   html_page_inode varchar(36),
   text_message text,
   mod_date timestamp,
   modified_by varchar(255),
   ext_comm_id varchar(255),
   primary key (inode)
);
create table fileasset_version_info (
   identifier varchar(36) not null,
   working_inode varchar(36) not null,
   live_inode varchar(36),
   deleted bool not null,
   locked_by varchar(100),
   locked_on date not null,
   primary key (identifier)
);
create table workflow_history (
   id varchar(36) not null,
   creation_date timestamp,
   made_by varchar(255),
   change_desc text,
   workflowtask_id varchar(36),
   workflow_action_id varchar(36),
   workflow_step_id varchar(36),
   primary key (id)
);
create table host_variable (
   id varchar(36) not null,
   host_id varchar(36),
   variable_name varchar(255),
   variable_key varchar(255),
   variable_value varchar(255),
   user_id varchar(255),
   last_mod_date date,
   primary key (id)
);
create table links (
   inode varchar(36) not null,
   show_on_menu bool,
   title varchar(255),
   mod_date timestamp,
   mod_user varchar(100),
   sort_order int4,
   friendly_name varchar(255),
   identifier varchar(36),
   protocal varchar(100),
   url varchar(255),
   target varchar(100),
   internal_link_identifier varchar(36),
   link_type varchar(255),
   link_code text,
   primary key (inode)
);
create table user_proxy (
   inode varchar(36) not null,
   user_id varchar(255),
   prefix varchar(255),
   suffix varchar(255),
   title varchar(255),
   school varchar(255),
   how_heard varchar(255),
   company varchar(255),
   long_lived_cookie varchar(255),
   website varchar(255),
   graduation_year int4,
   organization varchar(255),
   mail_subscription bool,
   var1 varchar(255),
   var2 varchar(255),
   var3 varchar(255),
   var4 varchar(255),
   var5 varchar(255),
   var6 varchar(255),
   var7 varchar(255),
   var8 varchar(255),
   var9 varchar(255),
   var10 varchar(255),
   var11 varchar(255),
   var12 varchar(255),
   var13 varchar(255),
   var14 varchar(255),
   var15 varchar(255),
   var16 varchar(255),
   var17 varchar(255),
   var18 varchar(255),
   var19 varchar(255),
   var20 varchar(255),
   var21 varchar(255),
   var22 varchar(255),
   var23 varchar(255),
   var24 varchar(255),
   var25 varchar(255),
   last_result int4,
   last_message varchar(255),
   no_click_tracking bool,
   cquestionid varchar(255),
   cqanswer varchar(255),
   chapter_officer varchar(255),
   primary key (inode),
   unique (user_id)
);
create table chain_state_parameter (
   id int8 not null,
   chain_state_id int8 not null,
   name varchar(255) not null,
   value varchar(255) not null,
   primary key (id)
);
create table field (
   inode varchar(36) not null,
   structure_inode varchar(255),
   field_name varchar(255),
   field_type varchar(255),
   field_relation_type varchar(255),
   field_contentlet varchar(255),
   required bool,
   indexed bool,
   listed bool,
   velocity_var_name varchar(255),
   sort_order int4,
   field_values text,
   regex_check varchar(255),
   hint varchar(255),
   default_value varchar(255),
   fixed bool,
   read_only bool,
   searchable bool,
   unique_ bool,
   primary key (inode)
);
create table relationship (
   inode varchar(36) not null,
   parent_structure_inode varchar(255),
   child_structure_inode varchar(255),
   parent_relation_name varchar(255),
   child_relation_name varchar(255),
   relation_type_value varchar(255),
   cardinality int4,
   parent_required bool,
   child_required bool,
   fixed bool,
   primary key (inode)
);
create table folder (
   inode varchar(36) not null,
   name varchar(255),
   title varchar(255) not null,
   show_on_menu bool,
   sort_order int4,
   files_masks varchar(255),
   identifier varchar(36),
   default_file_type varchar(36),
   primary key (inode)
);
create table clickstream_404 (
   clickstream_404_id int8 not null,
   referer_uri varchar(255),
   query_string text,
   request_uri varchar(255),
   user_id varchar(255),
   host_id varchar(36),
   timestampper timestamp,
   primary key (clickstream_404_id)
);
create table cms_layout (
   id varchar(36) not null,
   layout_name varchar(255) not null,
   description varchar(255),
   tab_order int4,
   primary key (id)
);
create table field_variable (
   id varchar(36) not null,
   field_id varchar(36),
   variable_name varchar(255),
   variable_key varchar(255),
   variable_value text,
   user_id varchar(255),
   last_mod_date date,
   primary key (id)
);
create table report_parameter (
   inode varchar(36) not null,
   report_inode varchar(36),
   parameter_description varchar(1000),
   parameter_name varchar(255),
   class_type varchar(250),
   default_value varchar(4000),
   primary key (inode),
   unique (report_inode, parameter_name)
);
create table chain (
   id int8 not null,
   key_name varchar(255) unique,
   name varchar(255) not null,
   success_value varchar(255) not null,
   failure_value varchar(255) not null,
   primary key (id)
);
create table link_version_info (
   identifier varchar(36) not null,
   working_inode varchar(36) not null,
   live_inode varchar(36),
   deleted bool not null,
   locked_by varchar(100),
   locked_on date,
   primary key (identifier)
);
create table template_containers (
   id varchar(36) not null,
   template_id varchar(36) not null,
   container_id varchar(36) not null,
   primary key (id)
);
create table user_filter (
   inode varchar(36) not null,
   title varchar(255),
   firstname varchar(100),
   middlename varchar(100),
   lastname varchar(100),
   emailaddress varchar(100),
   birthdaytypesearch varchar(100),
   birthday timestamp,
   birthdayfrom timestamp,
   birthdayto timestamp,
   lastlogintypesearch varchar(100),
   lastloginsince varchar(100),
   loginfrom timestamp,
   loginto timestamp,
   createdtypesearch varchar(100),
   createdsince varchar(100),
   createdfrom timestamp,
   createdto timestamp,
   lastvisittypesearch varchar(100),
   lastvisitsince varchar(100),
   lastvisitfrom timestamp,
   lastvisitto timestamp,
   city varchar(100),
   state varchar(100),
   country varchar(100),
   zip varchar(100),
   cell varchar(100),
   phone varchar(100),
   fax varchar(100),
   active_ varchar(255),
   tagname varchar(255),
   var1 varchar(255),
   var2 varchar(255),
   var3 varchar(255),
   var4 varchar(255),
   var5 varchar(255),
   var6 varchar(255),
   var7 varchar(255),
   var8 varchar(255),
   var9 varchar(255),
   var10 varchar(255),
   var11 varchar(255),
   var12 varchar(255),
   var13 varchar(255),
   var14 varchar(255),
   var15 varchar(255),
   var16 varchar(255),
   var17 varchar(255),
   var18 varchar(255),
   var19 varchar(255),
   var20 varchar(255),
   var21 varchar(255),
   var22 varchar(255),
   var23 varchar(255),
   var24 varchar(255),
   var25 varchar(255),
   categories varchar(255),
   primary key (inode)
);
create table inode (
   inode varchar(36) not null,
   owner varchar(255),
   idate timestamp,
   type varchar(64),
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
