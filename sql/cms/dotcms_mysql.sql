create table calendar_reminder (
   user_id varchar(255) not null,
   event_id varchar(36) not null,
   send_date datetime not null,
   primary key (user_id, event_id, send_date)
);
create table analytic_summary_pages (
   id bigint not null auto_increment,
   summary_id bigint not null,
   inode varchar(255),
   hits bigint,
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
   cdate datetime,
   comment_user_id varchar(100),
   type varchar(255),
   method varchar(255),
   subject varchar(255),
   ucomment longtext,
   communication_id varchar(36),
   primary key (inode)
);
create table permission_reference (
   id bigint not null auto_increment,
   asset_id varchar(36),
   reference_id varchar(36),
   permission_type varchar(100),
   primary key (id),
   unique (asset_id)
);
create table contentlet_version_info (
   identifier varchar(36) not null,
   lang bigint not null,
   working_inode varchar(36) not null,
   live_inode varchar(36),
   deleted bit not null,
   locked_by varchar(100),
   locked_on datetime,
   primary key (identifier, lang)
);
create table fixes_audit (
   id varchar(36) not null,
   table_name varchar(255),
   action varchar(255),
   records_altered integer,
   datetime date,
   primary key (id)
);
create table container_version_info (
   identifier varchar(36) not null,
   working_inode varchar(36) not null,
   live_inode varchar(36),
   deleted bit not null,
   locked_by varchar(100),
   locked_on date,
   primary key (identifier)
);
create table trackback (
   id bigint not null auto_increment,
   asset_identifier varchar(36),
   title varchar(255),
   excerpt varchar(255),
   url varchar(255),
   blog_name varchar(255),
   track_date datetime not null,
   primary key (id)
);
create table mailing_list (
   inode varchar(36) not null,
   title varchar(255),
   public_list varchar(1),
   user_id varchar(255),
   primary key (inode)
);
create table recipient (
   inode varchar(36) not null,
   name varchar(255),
   lastname varchar(255),
   email varchar(255),
   sent datetime,
   opened datetime,
   last_result integer,
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
   submit_date datetime,
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
   custom_fields longtext,
   user_inode varchar(36),
   categories varchar(255),
   primary key (web_form_id)
);
create table virtual_link (
   inode varchar(36) not null,
   title varchar(255),
   url varchar(255),
   uri varchar(255),
   active varchar(1),
   primary key (inode)
);
create table analytic_summary_period (
   id bigint not null auto_increment,
   full_date datetime,
   day integer,
   week integer,
   month integer,
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
   tree_order integer,
   primary key (child, parent, relation_type)
);
create table analytic_summary (
   id bigint not null auto_increment,
   summary_period_id bigint not null,
   host_id varchar(36) not null,
   visits bigint,
   page_views bigint,
   unique_visits bigint,
   new_visits bigint,
   direct_traffic bigint,
   referring_sites bigint,
   search_engines bigint,
   bounce_rate integer,
   avg_time_on_site datetime,
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
   show_on_menu varchar(1),
   title varchar(255),
   mod_date datetime,
   mod_user varchar(100),
   sort_order integer,
   friendly_name varchar(255),
   body longtext,
   header longtext,
   footer longtext,
   image varchar(36),
   identifier varchar(36),
   primary key (inode)
);
create table analytic_summary_content (
   id bigint not null auto_increment,
   summary_id bigint not null,
   inode varchar(255),
   hits bigint,
   uri varchar(255),
   title varchar(255),
   primary key (id)
);
create table structure (
   inode varchar(36) not null,
   name varchar(255),
   description varchar(255),
   default_structure varchar(1),
   review_interval varchar(255),
   reviewer_role varchar(255),
   page_detail varchar(36),
   structuretype integer,
   system varchar(1),
   fixed bit not null,
   velocity_var_name varchar(255),
   url_map_pattern text,
   host varchar(36) not null,
   folder varchar(36) not null,
   primary key (inode)
);
create table cms_role (
   id varchar(36) not null,
   role_name varchar(255) not null,
   description longtext,
   role_key varchar(255),
   db_fqn text not null,
   parent varchar(36) not null,
   edit_permissions varchar(1),
   edit_users varchar(1),
   edit_layouts varchar(1),
   locked varchar(1),
   system varchar(1),
   primary key (id)
);
create table permission (
   id bigint not null auto_increment,
   permission_type varchar(100),
   inode_id varchar(36),
   roleid varchar(36),
   permission integer,
   primary key (id),
   unique (permission_type, inode_id, roleid)
);
create table contentlet (
   inode varchar(36) not null,
   show_on_menu varchar(1),
   title varchar(255),
   mod_date datetime,
   mod_user varchar(100),
   sort_order integer,
   friendly_name varchar(255),
   structure_inode varchar(36),
   last_review datetime,
   next_review datetime,
   review_interval varchar(255),
   disabled_wysiwyg varchar(255),
   identifier varchar(36),
   language_id bigint,
   date1 datetime,
   date2 datetime,
   date3 datetime,
   date4 datetime,
   date5 datetime,
   date6 datetime,
   date7 datetime,
   date8 datetime,
   date9 datetime,
   date10 datetime,
   date11 datetime,
   date12 datetime,
   date13 datetime,
   date14 datetime,
   date15 datetime,
   date16 datetime,
   date17 datetime,
   date18 datetime,
   date19 datetime,
   date20 datetime,
   date21 datetime,
   date22 datetime,
   date23 datetime,
   date24 datetime,
   date25 datetime,
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
   text_area1 longtext,
   text_area2 longtext,
   text_area3 longtext,
   text_area4 longtext,
   text_area5 longtext,
   text_area6 longtext,
   text_area7 longtext,
   text_area8 longtext,
   text_area9 longtext,
   text_area10 longtext,
   text_area11 longtext,
   text_area12 longtext,
   text_area13 longtext,
   text_area14 longtext,
   text_area15 longtext,
   text_area16 longtext,
   text_area17 longtext,
   text_area18 longtext,
   text_area19 longtext,
   text_area20 longtext,
   text_area21 longtext,
   text_area22 longtext,
   text_area23 longtext,
   text_area24 longtext,
   text_area25 longtext,
   integer1 bigint,
   integer2 bigint,
   integer3 bigint,
   integer4 bigint,
   integer5 bigint,
   integer6 bigint,
   integer7 bigint,
   integer8 bigint,
   integer9 bigint,
   integer10 bigint,
   integer11 bigint,
   integer12 bigint,
   integer13 bigint,
   integer14 bigint,
   integer15 bigint,
   integer16 bigint,
   integer17 bigint,
   integer18 bigint,
   integer19 bigint,
   integer20 bigint,
   integer21 bigint,
   integer22 bigint,
   integer23 bigint,
   integer24 bigint,
   integer25 bigint,
   `float1` float,
   `float2` float,
   `float3` float,
   `float4` float,
   `float5` float,
   `float6` float,
   `float7` float,
   `float8` float,
   `float9` float,
   `float10` float,
   `float11` float,
   `float12` float,
   `float13` float,
   `float14` float,
   `float15` float,
   `float16` float,
   `float17` float,
   `float18` float,
   `float19` float,
   `float20` float,
   `float21` float,
   `float22` float,
   `float23` float,
   `float24` float,
   `float25` float,
   bool1 varchar(1),
   bool2 varchar(1),
   bool3 varchar(1),
   bool4 varchar(1),
   bool5 varchar(1),
   bool6 varchar(1),
   bool7 varchar(1),
   bool8 varchar(1),
   bool9 varchar(1),
   bool10 varchar(1),
   bool11 varchar(1),
   bool12 varchar(1),
   bool13 varchar(1),
   bool14 varchar(1),
   bool15 varchar(1),
   bool16 varchar(1),
   bool17 varchar(1),
   bool18 varchar(1),
   bool19 varchar(1),
   bool20 varchar(1),
   bool21 varchar(1),
   bool22 varchar(1),
   bool23 varchar(1),
   bool24 varchar(1),
   bool25 varchar(1),
   primary key (inode)
);
create table analytic_summary_404 (
   id bigint not null auto_increment,
   summary_period_id bigint not null,
   host_id varchar(36),
   uri varchar(255),
   referer_uri varchar(255),
   primary key (id)
);
create table cms_layouts_portlets (
   id varchar(36) not null,
   layout_id varchar(36) not null,
   portlet_id varchar(100) not null,
   portlet_order integer,
   primary key (id)
);
create table workflow_comment (
   id varchar(36) not null,
   creation_date datetime,
   posted_by varchar(255),
   wf_comment longtext,
   workflowtask_id varchar(36),
   primary key (id)
);
create table report_asset (
   inode varchar(36) not null,
   report_name varchar(255) not null,
   report_description text not null,
   requires_input varchar(1),
   ds varchar(100) not null,
   web_form_report varchar(1),
   primary key (inode)
);
create table category (
   inode varchar(36) not null,
   category_name varchar(255),
   category_key varchar(255),
   sort_order integer,
   active varchar(1),
   keywords longtext,
   category_velocity_var_name varchar(255),
   primary key (inode)
);
create table htmlpage (
   inode varchar(36) not null,
   show_on_menu varchar(1),
   title varchar(255),
   mod_date datetime,
   mod_user varchar(100),
   sort_order integer,
   friendly_name varchar(255),
   metadata longtext,
   start_date datetime,
   end_date datetime,
   page_url varchar(255),
   https_required varchar(1),
   redirect varchar(255),
   identifier varchar(36),
   seo_description longtext,
   seo_keywords longtext,
   cache_ttl bigint,
   template_id varchar(36),
   primary key (inode)
);
create table chain_link_code (
   id bigint not null auto_increment,
   class_name varchar(255) unique,
   code longtext not null,
   last_mod_date date not null,
   language varchar(255) not null,
   primary key (id)
);
create table analytic_summary_visits (
   id bigint not null auto_increment,
   summary_period_id bigint not null,
   host_id varchar(36),
   visit_time datetime,
   visits bigint,
   primary key (id)
);
create table template_version_info (
   identifier varchar(36) not null,
   working_inode varchar(36) not null,
   live_inode varchar(36),
   deleted bit not null,
   locked_by varchar(100),
   locked_on date,
   primary key (identifier)
);
create table user_preferences (
   id bigint not null auto_increment,
   user_id varchar(100) not null,
   preference varchar(255),
   pref_value longtext,
   primary key (id)
);
create table language (
   id bigint not null auto_increment,
   language_code varchar(5),
   country_code varchar(255),
   language varchar(255),
   country varchar(255),
   primary key (id)
);
create table users_to_delete (
   id bigint not null auto_increment,
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
   clickstream_id bigint not null auto_increment,
   cookie_id varchar(255),
   user_id varchar(255),
   start_date datetime,
   end_date datetime,
   referer varchar(255),
   remote_address varchar(255),
   remote_hostname varchar(255),
   user_agent varchar(255),
   bot varchar(1),
   number_of_requests integer,
   host_id varchar(36),
   last_page_id varchar(50),
   first_page_id varchar(50),
   operating_system varchar(50),
   browser_name varchar(50),
   browser_version varchar(50),
   mobile_device varchar(1),
   primary key (clickstream_id)
);
create table multi_tree (
   child varchar(36) not null,
   parent1 varchar(36) not null,
   parent2 varchar(36) not null,
   relation_type varchar(64),
   tree_order integer,
   primary key (child, parent1, parent2)
);
create table workflow_task (
   id varchar(36) not null,
   creation_date datetime,
   mod_date datetime,
   due_date datetime,
   created_by varchar(255),
   assigned_to varchar(255),
   belongs_to varchar(255),
   title varchar(255),
   description longtext,
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
   click_count integer,
   primary key (inode)
);
create table challenge_question (
   cquestionid bigint not null,
   cqtext varchar(255),
   primary key (cquestionid)
);
create table file_asset (
   inode varchar(36) not null,
   file_name varchar(255),
   file_size integer,
   width integer,
   height integer,
   mime_type varchar(255),
   author varchar(255),
   publish_date datetime,
   show_on_menu varchar(1),
   title varchar(255),
   friendly_name varchar(255),
   mod_date datetime,
   mod_user varchar(100),
   sort_order integer,
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
   clickstream_request_id bigint not null auto_increment,
   clickstream_id bigint,
   server_name varchar(255),
   protocol varchar(255),
   server_port integer,
   request_uri varchar(255),
   request_order integer,
   query_string longtext,
   language_id bigint,
   timestampper datetime,
   host_id varchar(36),
   associated_identifier varchar(36),
   primary key (clickstream_request_id)
);
create table content_rating (
   id bigint not null auto_increment,
   rating float,
   user_id varchar(255),
   session_id varchar(255),
   identifier varchar(36),
   rating_date datetime,
   user_ip varchar(255),
   long_live_cookie_id varchar(255),
   primary key (id)
);
create table chain_state (
   id bigint not null auto_increment,
   chain_id bigint not null,
   link_code_id bigint not null,
   state_order bigint not null,
   primary key (id)
);
create table analytic_summary_workstream (
   id bigint not null auto_increment,
   inode varchar(255),
   asset_type varchar(255),
   mod_user_id varchar(255),
   host_id varchar(36),
   mod_date datetime,
   action varchar(255),
   name varchar(255),
   primary key (id)
);
create table dashboard_user_preferences (
   id bigint not null auto_increment,
   summary_404_id bigint,
   user_id varchar(255),
   ignored varchar(1),
   mod_date datetime,
   primary key (id)
);
create table campaign (
   inode varchar(36) not null,
   title varchar(255),
   from_email varchar(255),
   from_name varchar(255),
   subject varchar(255),
   message longtext,
   user_id varchar(255),
   start_date datetime,
   completed_date datetime,
   active varchar(1),
   locked varchar(1),
   sends_per_hour varchar(15),
   sendemail varchar(1),
   communicationinode varchar(36),
   userfilterinode varchar(36),
   sendto varchar(15),
   isrecurrent varchar(1),
   wassent varchar(1),
   expiration_date datetime,
   parent_campaign varchar(36),
   primary key (inode)
);
create table htmlpage_version_info (
   identifier varchar(36) not null,
   working_inode varchar(36) not null,
   live_inode varchar(36),
   deleted bit not null,
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
   id bigint not null auto_increment,
   summary_id bigint not null,
   hits bigint,
   uri varchar(255),
   primary key (id)
);
create table containers (
   inode varchar(36) not null,
   code longtext,
   pre_loop longtext,
   post_loop longtext,
   show_on_menu varchar(1),
   title varchar(255),
   mod_date datetime,
   mod_user varchar(100),
   sort_order integer,
   friendly_name varchar(255),
   max_contentlets integer,
   use_div varchar(1),
   staticify varchar(1),
   sort_contentlets_by varchar(255),
   lucene_query longtext,
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
   text_message longtext,
   mod_date datetime,
   modified_by varchar(255),
   ext_comm_id varchar(255),
   primary key (inode)
);
create table fileasset_version_info (
   identifier varchar(36) not null,
   working_inode varchar(36) not null,
   live_inode varchar(36),
   deleted bit not null,
   locked_by varchar(100),
   locked_on date not null,
   primary key (identifier)
);
create table workflow_history (
   id varchar(36) not null,
   creation_date datetime,
   made_by varchar(255),
   change_desc longtext,
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
   show_on_menu varchar(1),
   title varchar(255),
   mod_date datetime,
   mod_user varchar(100),
   sort_order integer,
   friendly_name varchar(255),
   protocal varchar(100),
   url varchar(255),
   target varchar(100),
   internal_link_identifier varchar(36),
   link_type varchar(255),
   link_code longtext,
   identifier varchar(36),
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
   graduation_year integer,
   organization varchar(255),
   mail_subscription varchar(1),
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
   last_result integer,
   last_message varchar(255),
   no_click_tracking varchar(1),
   cquestionid varchar(255),
   cqanswer varchar(255),
   chapter_officer varchar(255),
   primary key (inode),
   unique (user_id)
);
create table chain_state_parameter (
   id bigint not null auto_increment,
   chain_state_id bigint not null,
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
   required varchar(1),
   indexed varchar(1),
   listed varchar(1),
   velocity_var_name varchar(255),
   sort_order integer,
   field_values longtext,
   regex_check varchar(255),
   hint varchar(255),
   default_value varchar(255),
   fixed varchar(1),
   read_only varchar(1),
   searchable varchar(1),
   unique_ varchar(1),
   primary key (inode)
);
create table relationship (
   inode varchar(36) not null,
   parent_structure_inode varchar(255),
   child_structure_inode varchar(255),
   parent_relation_name varchar(255),
   child_relation_name varchar(255),
   relation_type_value varchar(255),
   cardinality integer,
   parent_required varchar(1),
   child_required varchar(1),
   fixed varchar(1),
   primary key (inode)
);
create table folder (
   inode varchar(36) not null,
   name varchar(255),
   title varchar(255) not null,
   show_on_menu varchar(1),
   sort_order integer,
   files_masks varchar(255),
   identifier varchar(36),
   default_file_type varchar(36),
   primary key (inode)
);
create table clickstream_404 (
   clickstream_404_id bigint not null auto_increment,
   referer_uri varchar(255),
   query_string longtext,
   request_uri varchar(255),
   user_id varchar(255),
   host_id varchar(36),
   timestampper datetime,
   primary key (clickstream_404_id)
);
create table cms_layout (
   id varchar(36) not null,
   layout_name varchar(255) not null,
   description varchar(255),
   tab_order integer,
   primary key (id)
);
create table field_variable (
   id varchar(36) not null,
   field_id varchar(36),
   variable_name varchar(255),
   variable_key varchar(255),
   variable_value longtext,
   user_id varchar(255),
   last_mod_date date,
   primary key (id)
);
create table report_parameter (
   inode varchar(36) not null,
   report_inode varchar(36),
   parameter_description text,
   parameter_name varchar(255),
   class_type varchar(250),
   default_value text,
   primary key (inode),
   unique (report_inode, parameter_name)
);
create table chain (
   id bigint not null auto_increment,
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
   deleted bit not null,
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
   birthday datetime,
   birthdayfrom datetime,
   birthdayto datetime,
   lastlogintypesearch varchar(100),
   lastloginsince varchar(100),
   loginfrom datetime,
   loginto datetime,
   createdtypesearch varchar(100),
   createdsince varchar(100),
   createdfrom datetime,
   createdto datetime,
   lastvisittypesearch varchar(100),
   lastvisitsince varchar(100),
   lastvisitfrom datetime,
   lastvisitto datetime,
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
   idate datetime,
   type varchar(64),
   primary key (inode)
);
alter table analytic_summary_pages add index fka1ad33b9ed30e054 (summary_id), add constraint fka1ad33b9ed30e054 foreign key (summary_id) references analytic_summary (id);
create index idx_user_comments_1 on user_comments (user_id);
alter table user_comments add index fkdf1b37e85fb51eb (inode), add constraint fkdf1b37e85fb51eb foreign key (inode) references inode (inode);
create index idx_trackback_2 on trackback (url);
create index idx_trackback_1 on trackback (asset_identifier);
create index idx_mailinglist_1 on mailing_list (user_id);
alter table mailing_list add index fk7bc2cd925fb51eb (inode), add constraint fk7bc2cd925fb51eb foreign key (inode) references inode (inode);
create index idx_communication_user_id on recipient (user_id);
create index idx_recipiets_1 on recipient (email);
create index idx_recipiets_2 on recipient (sent);
alter table recipient add index fk30e172195fb51eb (inode), add constraint fk30e172195fb51eb foreign key (inode) references inode (inode);
create index idx_user_webform_1 on web_form (form_type);
create index idx_virtual_link_1 on virtual_link (url);
alter table virtual_link add index fkd844f8ae5fb51eb (inode), add constraint fkd844f8ae5fb51eb foreign key (inode) references inode (inode);
create index idx_analytic_summary_period_4 on analytic_summary_period (month);
create index idx_analytic_summary_period_3 on analytic_summary_period (week);
create index idx_analytic_summary_period_2 on analytic_summary_period (day);
create index idx_analytic_summary_period_5 on analytic_summary_period (year);
create index idx_analytic_summary_1 on analytic_summary (host_id);
create index idx_analytic_summary_2 on analytic_summary (visits);
create index idx_analytic_summary_3 on analytic_summary (page_views);
alter table analytic_summary add index fk9e1a7f4b7b46300 (summary_period_id), add constraint fk9e1a7f4b7b46300 foreign key (summary_period_id) references analytic_summary_period (id);
alter table template add index fkb13acc7a5fb51eb (inode), add constraint fkb13acc7a5fb51eb foreign key (inode) references inode (inode);
alter table analytic_summary_content add index fk53cb4f2eed30e054 (summary_id), add constraint fk53cb4f2eed30e054 foreign key (summary_id) references analytic_summary (id);
alter table structure add index fk89d2d735fb51eb (inode), add constraint fk89d2d735fb51eb foreign key (inode) references inode (inode);
create index idx_permission_2 on permission (permission_type, inode_id);
create index idx_permission_3 on permission (roleid);
alter table contentlet add index fkfc4ef025fb51eb (inode), add constraint fkfc4ef025fb51eb foreign key (inode) references inode (inode);
create index idx_analytic_summary_404_1 on analytic_summary_404 (host_id);
alter table analytic_summary_404 add index fk7050866db7b46300 (summary_period_id), add constraint fk7050866db7b46300 foreign key (summary_period_id) references analytic_summary_period (id);
alter table report_asset add index fk3765ec255fb51eb (inode), add constraint fk3765ec255fb51eb foreign key (inode) references inode (inode);
create index idx_category_1 on category (category_name);
create index idx_category_2 on category (category_key);
alter table category add index fk302bcfe5fb51eb (inode), add constraint fk302bcfe5fb51eb foreign key (inode) references inode (inode);
alter table htmlpage add index fkebf39cba5fb51eb (inode), add constraint fkebf39cba5fb51eb foreign key (inode) references inode (inode);
create index idx_chain_link_code_classname on chain_link_code (class_name);
create index idx_analytic_summary_visits_2 on analytic_summary_visits (visit_time);
create index idx_analytic_summary_visits_1 on analytic_summary_visits (host_id);
alter table analytic_summary_visits add index fk9eac9733b7b46300 (summary_period_id), add constraint fk9eac9733b7b46300 foreign key (summary_period_id) references analytic_summary_period (id);
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
alter table click add index fk5a5c5885fb51eb (inode), add constraint fk5a5c5885fb51eb foreign key (inode) references inode (inode);
alter table file_asset add index fk7ed2366d5fb51eb (inode), add constraint fk7ed2366d5fb51eb foreign key (inode) references inode (inode);
create index idx_user_clickstream_request_2 on clickstream_request (request_uri);
create index idx_user_clickstream_request_1 on clickstream_request (clickstream_id);
create index idx_user_clickstream_request_4 on clickstream_request (timestampper);
create index idx_user_clickstream_request_3 on clickstream_request (associated_identifier);
create index idx_dashboard_workstream_2 on analytic_summary_workstream (host_id);
create index idx_dashboard_workstream_1 on analytic_summary_workstream (mod_user_id);
create index idx_dashboard_workstream_3 on analytic_summary_workstream (mod_date);
create index idx_dashboard_prefs_2 on dashboard_user_preferences (user_id);
alter table dashboard_user_preferences add index fk496242cfd12c0c3b (summary_404_id), add constraint fk496242cfd12c0c3b foreign key (summary_404_id) references analytic_summary_404 (id);
create index idx_campaign_4 on campaign (expiration_date);
create index idx_campaign_3 on campaign (completed_date);
create index idx_campaign_2 on campaign (start_date);
create index idx_campaign_1 on campaign (user_id);
alter table campaign add index fkf7a901105fb51eb (inode), add constraint fkf7a901105fb51eb foreign key (inode) references inode (inode);
alter table analytic_summary_referer add index fk5bc0f3e2ed30e054 (summary_id), add constraint fk5bc0f3e2ed30e054 foreign key (summary_id) references analytic_summary (id);
alter table containers add index fk8a844125fb51eb (inode), add constraint fk8a844125fb51eb foreign key (inode) references inode (inode);
alter table communication add index fkc24acfd65fb51eb (inode), add constraint fkc24acfd65fb51eb foreign key (inode) references inode (inode);
alter table links add index fk6234fb95fb51eb (inode), add constraint fk6234fb95fb51eb foreign key (inode) references inode (inode);
alter table user_proxy add index fk7327d4fa5fb51eb (inode), add constraint fk7327d4fa5fb51eb foreign key (inode) references inode (inode);
create index idx_field_1 on field (structure_inode);
alter table field add index fk5cea0fa5fb51eb (inode), add constraint fk5cea0fa5fb51eb foreign key (inode) references inode (inode);
create index idx_relationship_1 on relationship (parent_structure_inode);
create index idx_relationship_2 on relationship (child_structure_inode);
alter table relationship add index fkf06476385fb51eb (inode), add constraint fkf06476385fb51eb foreign key (inode) references inode (inode);
create index idx_folder_1 on folder (name);
alter table folder add index fkb45d1c6e5fb51eb (inode), add constraint fkb45d1c6e5fb51eb foreign key (inode) references inode (inode);
create index idx_user_clickstream_404_2 on clickstream_404 (user_id);
create index idx_user_clickstream_404_3 on clickstream_404 (host_id);
create index idx_user_clickstream_404_1 on clickstream_404 (request_uri);
alter table report_parameter add index fk22da125e5fb51eb (inode), add constraint fk22da125e5fb51eb foreign key (inode) references inode (inode);
create index idx_chain_key_name on chain (key_name);
alter table user_filter add index fke042126c5fb51eb (inode), add constraint fke042126c5fb51eb foreign key (inode) references inode (inode);
create index idx_index_1 on inode (type);
