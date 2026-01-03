create table Address (
	addressId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate timestamptz null,
	modifiedDate timestamptz null,
	className varchar(100) null,
	classPK varchar(100) null,
	description varchar(100) null,
	street1 varchar(100) null,
	street2 varchar(100) null,
	city varchar(100) null,
	state varchar(100) null,
	zip varchar(100) null,
	country varchar(100) null,
	phone varchar(100) null,
	fax varchar(100) null,
	cell varchar(100) null,
	priority integer
);

create table AdminConfig (
	configId varchar(100) not null primary key,
	companyId varchar(100) not null,
	type_ varchar(100) null,
	name varchar(100) null,
	config text null
);

create table Company (
	companyId varchar(100) not null primary key,
	key_ text null,
	portalURL varchar(100) not null,
	homeURL varchar(100) not null,
	mx varchar(100) default 'dotcms.com',
	name varchar(100) not null,
	shortName varchar(100) not null,
	type_ varchar(100) null,
	size_ varchar(100) null,
	street varchar(100) null,
	city varchar(100) null,
	state varchar(100) null,
	zip varchar(100) null,
	phone varchar(100) null,
	fax varchar(100) null,
	emailAddress varchar(100) null,
	authType varchar(100) null,
	autoLogin bool,
	strangers bool,
    default_language_id int8 null
);

create table Counter (
	name varchar(100) not null primary key,
	currentId integer
);

create table Image (
	imageId varchar(200) not null primary key,
	text_ text not null
);

create table PasswordTracker (
	passwordTrackerId varchar(100) not null primary key,
	userId varchar(100) not null,
	createDate timestamptz not null,
	password_ varchar(100) not null
);

create table PollsChoice (
	choiceId varchar(100) not null,
	questionId varchar(100) not null,
	description text null,
	primary key (choiceId, questionId)
);

create table PollsDisplay (
	layoutId varchar(100) not null,
	userId varchar(100) not null,
	portletId varchar(100) not null,
	questionId varchar(100) not null,
	primary key (layoutId, userId, portletId)
);

create table PollsQuestion (
	questionId varchar(100) not null primary key,
	portletId varchar(100) not null,
	groupId varchar(100) not null,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate timestamptz null,
	modifiedDate timestamptz null,
	title varchar(100) null,
	description text null,
	expirationDate timestamptz null,
	lastVoteDate timestamptz null
);

create table PollsVote (
	questionId varchar(100) not null,
	userId varchar(100) not null,
	choiceId varchar(100) not null,
	voteDate timestamptz null,
	primary key (questionId, userId)
);

create table Portlet (
	portletId varchar(100) not null,
	groupId varchar(100) not null,
	companyId varchar(100) not null,
	defaultPreferences text null,
	narrow bool,
	roles text null,
	active_ bool,
	primary key (portletId, groupId, companyId)
);

create table PortletPreferences (
	portletId varchar(100) not null,
	userId varchar(100) not null,
	layoutId varchar(100) not null,
	preferences text null,
	primary key (portletId, userId, layoutId)
);

create table User_ (
	userId varchar(100) not null primary key,
	companyId varchar(100) not null,
	createDate timestamptz null,
	mod_date timestamptz null,
	password_ text null,
	passwordEncrypted bool,
	passwordExpirationDate timestamptz null,
	passwordReset bool,
	firstName varchar(100) null,
	middleName varchar(100) null,
	lastName varchar(100) null,
	nickName varchar(100) null,
	male bool,
	birthday timestamptz null,
	emailAddress varchar(100) null,
	smsId varchar(100) null,
	aimId varchar(100) null,
	icqId varchar(100) null,
	msnId varchar(100) null,
	ymId varchar(100) null,
	favoriteActivity varchar(100) null,
	favoriteBibleVerse varchar(100) null,
	favoriteFood varchar(100) null,
	favoriteMovie varchar(100) null,
	favoriteMusic varchar(100) null,
	languageId varchar(100) null,
	timeZoneId varchar(100) null,
	skinId varchar(100) null,
	dottedSkins bool,
	roundedSkins bool,
	greeting varchar(100) null,
	resolution varchar(100) null,
	refreshRate varchar(100) null,
	layoutIds varchar(100) null,
	comments text null,
	loginDate timestamptz null,
	loginIP varchar(100) null,
	lastLoginDate timestamptz null,
	lastLoginIP varchar(100) null,
	failedLoginAttempts integer,
	agreedToTermsOfUse bool,
	active_ bool,
    delete_in_progress BOOLEAN DEFAULT FALSE,
    delete_date timestamptz,
    additional_info JSONB NULL
);

create table UserTracker (
	userTrackerId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	modifiedDate timestamptz null,
	remoteAddr varchar(100) null,
	remoteHost varchar(100) null,
	userAgent varchar(100) null
);

create table UserTrackerPath (
	userTrackerPathId varchar(100) not null primary key,
	userTrackerId varchar(100) not null,
	path text not null,
	pathDate timestamptz not null
);

--
-- Global
--

insert into Counter values ('com.liferay.portal.model.Address', 10);
insert into Counter values ('com.liferay.portal.model.Role', 100);
insert into Counter values ('com.liferay.portal.model.User.liferay.com', 10);
insert into Counter values ('com.liferay.portlet.polls.model.PollsQuestion', 10);;

--
-- Liferay, LLC
--

insert into Company (companyId, portalURL, homeURL, mx, name, shortName, type_, size_, emailAddress, authType, autoLogin, strangers) values ('liferay.com', 'localhost', 'localhost', 'liferay.com', 'Liferay, LLC', 'Liferay', 'biz', '', 'test@liferay.com', 'emailAddress', 't', 't');
update Company set street = '1220 Brea Canyon Rd.', city = 'Diamond Bar', state = 'CA', zip = '91789' where companyId = 'liferay.com';

insert into PollsDisplay (layoutId, userId, portletId, questionId) values ('1.1', 'group.1', '59', '1');
insert into PollsChoice (choiceId, questionId, description) values ('a', '1', 'Chocolate');
insert into PollsChoice (choiceId, questionId, description) values ('b', '1', 'Strawberry');
insert into PollsChoice (choiceId, questionId, description) values ('c', '1', 'Vanilla');
insert into PollsQuestion (questionId, portletId, groupId, companyId, userId, userName, createDate, modifiedDate, title, description) values ('1', '25', '-1', 'liferay.com', 'liferay.com.1', 'John Wayne', current_timestamp, current_timestamp, 'What is your favorite ice cream flavor?', 'What is your favorite ice cream flavor?');

--
-- Default User
--

insert into User_ (userId, companyId, createDate, password_, passwordEncrypted, passwordReset, firstName, middleName, lastName, male, birthday, emailAddress, skinId, dottedSkins, roundedSkins, greeting, layoutIds, loginDate, failedLoginAttempts, agreedToTermsOfUse, active_) values ('liferay.com.default', 'default', current_timestamp, 'password', 'f', 'f', '', '', '', 't', '01/01/1970', 'default@liferay.com', '01', 'f', 'f', 'Welcome!', '', current_timestamp, 0, 'f', 't');
--
-- Test User
--

insert into User_ (userId, companyId, createDate, password_, passwordEncrypted, passwordReset, firstName, middleName, lastName, nickName, male, birthday, emailAddress, skinId, dottedSkins, roundedSkins, greeting, layoutIds, loginDate, failedLoginAttempts, agreedToTermsOfUse, active_) values ('liferay.com.1', 'liferay.com', current_timestamp, 'test', 'f', 'f', 'John', '', 'Wayne', 'Duke', 't', '01/01/1970', 'test@liferay.com', '01', 'f', 't', 'Welcome John Wayne!', '1,', current_timestamp, 0, 't', 't');
CREATE TABLE qrtz_job_details
  (
    JOB_NAME  VARCHAR(80) NOT NULL,
    JOB_GROUP VARCHAR(80) NOT NULL,
    DESCRIPTION VARCHAR(120) NULL,
    JOB_CLASS_NAME   VARCHAR(128) NOT NULL,
    IS_DURABLE BOOL NOT NULL,
    IS_VOLATILE BOOL NOT NULL,
    IS_STATEFUL BOOL NOT NULL,
    REQUESTS_RECOVERY BOOL NOT NULL,
    JOB_DATA BYTEA NULL,
    PRIMARY KEY (JOB_NAME,JOB_GROUP)
);

CREATE TABLE qrtz_job_listeners
  (
    JOB_NAME  VARCHAR(80) NOT NULL,
    JOB_GROUP VARCHAR(80) NOT NULL,
    JOB_LISTENER VARCHAR(80) NOT NULL,
    PRIMARY KEY (JOB_NAME,JOB_GROUP,JOB_LISTENER),
    FOREIGN KEY (JOB_NAME,JOB_GROUP)
	REFERENCES QRTZ_JOB_DETAILS(JOB_NAME,JOB_GROUP)
);

CREATE TABLE qrtz_triggers
  (
    TRIGGER_NAME VARCHAR(80) NOT NULL,
    TRIGGER_GROUP VARCHAR(80) NOT NULL,
    JOB_NAME  VARCHAR(80) NOT NULL,
    JOB_GROUP VARCHAR(80) NOT NULL,
    IS_VOLATILE BOOL NOT NULL,
    DESCRIPTION VARCHAR(120) NULL,
    NEXT_FIRE_TIME BIGINT NULL,
    PREV_FIRE_TIME BIGINT NULL,
    PRIORITY INTEGER NULL,
    TRIGGER_STATE VARCHAR(16) NOT NULL,
    TRIGGER_TYPE VARCHAR(8) NOT NULL,
    START_TIME BIGINT NOT NULL,
    END_TIME BIGINT NULL,
    CALENDAR_NAME VARCHAR(80) NULL,
    MISFIRE_INSTR SMALLINT NULL,
    JOB_DATA BYTEA NULL,
    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (JOB_NAME,JOB_GROUP)
	REFERENCES QRTZ_JOB_DETAILS(JOB_NAME,JOB_GROUP)
);

CREATE TABLE qrtz_simple_triggers
  (
    TRIGGER_NAME VARCHAR(80) NOT NULL,
    TRIGGER_GROUP VARCHAR(80) NOT NULL,
    REPEAT_COUNT BIGINT NOT NULL,
    REPEAT_INTERVAL BIGINT NOT NULL,
    TIMES_TRIGGERED BIGINT NOT NULL,
    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (TRIGGER_NAME,TRIGGER_GROUP)
	REFERENCES QRTZ_TRIGGERS(TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE qrtz_cron_triggers
  (
    TRIGGER_NAME VARCHAR(80) NOT NULL,
    TRIGGER_GROUP VARCHAR(80) NOT NULL,
    CRON_EXPRESSION VARCHAR(80) NOT NULL,
    TIME_ZONE_ID VARCHAR(80),
    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (TRIGGER_NAME,TRIGGER_GROUP)
	REFERENCES QRTZ_TRIGGERS(TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE qrtz_blob_triggers
  (
    TRIGGER_NAME VARCHAR(80) NOT NULL,
    TRIGGER_GROUP VARCHAR(80) NOT NULL,
    BLOB_DATA BYTEA NULL,
    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (TRIGGER_NAME,TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS(TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE qrtz_trigger_listeners
  (
    TRIGGER_NAME  VARCHAR(80) NOT NULL,
    TRIGGER_GROUP VARCHAR(80) NOT NULL,
    TRIGGER_LISTENER VARCHAR(80) NOT NULL,
    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP,TRIGGER_LISTENER),
    FOREIGN KEY (TRIGGER_NAME,TRIGGER_GROUP)
	REFERENCES QRTZ_TRIGGERS(TRIGGER_NAME,TRIGGER_GROUP)
);


CREATE TABLE qrtz_calendars
  (
    CALENDAR_NAME  VARCHAR(80) NOT NULL,
    CALENDAR BYTEA NOT NULL,
    PRIMARY KEY (CALENDAR_NAME)
);


CREATE TABLE qrtz_paused_trigger_grps
  (
    TRIGGER_GROUP  VARCHAR(80) NOT NULL,
    PRIMARY KEY (TRIGGER_GROUP)
);

CREATE TABLE qrtz_fired_triggers
  (
    ENTRY_ID VARCHAR(95) NOT NULL,
    TRIGGER_NAME VARCHAR(80) NOT NULL,
    TRIGGER_GROUP VARCHAR(80) NOT NULL,
    IS_VOLATILE BOOL NOT NULL,
    INSTANCE_NAME VARCHAR(80) NOT NULL,
    FIRED_TIME BIGINT NOT NULL,
    PRIORITY INTEGER NOT NULL,
    STATE VARCHAR(16) NOT NULL,
    JOB_NAME VARCHAR(80) NULL,
    JOB_GROUP VARCHAR(80) NULL,
    IS_STATEFUL BOOL NULL,
    REQUESTS_RECOVERY BOOL NULL,
    PRIMARY KEY (ENTRY_ID)
);

CREATE TABLE qrtz_scheduler_state
  (
    INSTANCE_NAME VARCHAR(80) NOT NULL,
    LAST_CHECKIN_TIME BIGINT NOT NULL,
    CHECKIN_INTERVAL BIGINT NOT NULL,
    PRIMARY KEY (INSTANCE_NAME)
);

CREATE TABLE qrtz_locks
  (
    LOCK_NAME  VARCHAR(40) NOT NULL,
    PRIMARY KEY (LOCK_NAME)
);


INSERT INTO qrtz_locks values('TRIGGER_ACCESS');
INSERT INTO qrtz_locks values('JOB_ACCESS');
INSERT INTO qrtz_locks values('CALENDAR_ACCESS');
INSERT INTO qrtz_locks values('STATE_ACCESS');
INSERT INTO qrtz_locks values('MISFIRE_ACCESS');


CREATE TABLE QRTZ_EXCL_job_details
  (
    JOB_NAME  VARCHAR(80) NOT NULL,
    JOB_GROUP VARCHAR(80) NOT NULL,
    DESCRIPTION VARCHAR(120) NULL,
    JOB_CLASS_NAME   VARCHAR(128) NOT NULL,
    IS_DURABLE BOOL NOT NULL,
    IS_VOLATILE BOOL NOT NULL,
    IS_STATEFUL BOOL NOT NULL,
    REQUESTS_RECOVERY BOOL NOT NULL,
    JOB_DATA BYTEA NULL,
    PRIMARY KEY (JOB_NAME,JOB_GROUP)
);

CREATE TABLE QRTZ_EXCL_job_listeners
  (
    JOB_NAME  VARCHAR(80) NOT NULL,
    JOB_GROUP VARCHAR(80) NOT NULL,
    JOB_LISTENER VARCHAR(80) NOT NULL,
    PRIMARY KEY (JOB_NAME,JOB_GROUP,JOB_LISTENER),
    FOREIGN KEY (JOB_NAME,JOB_GROUP)
	REFERENCES QRTZ_EXCL_JOB_DETAILS(JOB_NAME,JOB_GROUP)
);

CREATE TABLE QRTZ_EXCL_triggers
  (
    TRIGGER_NAME VARCHAR(80) NOT NULL,
    TRIGGER_GROUP VARCHAR(80) NOT NULL,
    JOB_NAME  VARCHAR(80) NOT NULL,
    JOB_GROUP VARCHAR(80) NOT NULL,
    IS_VOLATILE BOOL NOT NULL,
    DESCRIPTION VARCHAR(120) NULL,
    NEXT_FIRE_TIME BIGINT NULL,
    PREV_FIRE_TIME BIGINT NULL,
    PRIORITY INTEGER NULL,
    TRIGGER_STATE VARCHAR(16) NOT NULL,
    TRIGGER_TYPE VARCHAR(8) NOT NULL,
    START_TIME BIGINT NOT NULL,
    END_TIME BIGINT NULL,
    CALENDAR_NAME VARCHAR(80) NULL,
    MISFIRE_INSTR SMALLINT NULL,
    JOB_DATA BYTEA NULL,
    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (JOB_NAME,JOB_GROUP)
	REFERENCES QRTZ_EXCL_JOB_DETAILS(JOB_NAME,JOB_GROUP)
);

CREATE TABLE QRTZ_EXCL_simple_triggers
  (
    TRIGGER_NAME VARCHAR(80) NOT NULL,
    TRIGGER_GROUP VARCHAR(80) NOT NULL,
    REPEAT_COUNT BIGINT NOT NULL,
    REPEAT_INTERVAL BIGINT NOT NULL,
    TIMES_TRIGGERED BIGINT NOT NULL,
    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (TRIGGER_NAME,TRIGGER_GROUP)
	REFERENCES QRTZ_EXCL_TRIGGERS(TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE QRTZ_EXCL_cron_triggers
  (
    TRIGGER_NAME VARCHAR(80) NOT NULL,
    TRIGGER_GROUP VARCHAR(80) NOT NULL,
    CRON_EXPRESSION VARCHAR(80) NOT NULL,
    TIME_ZONE_ID VARCHAR(80),
    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (TRIGGER_NAME,TRIGGER_GROUP)
	REFERENCES QRTZ_EXCL_TRIGGERS(TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE QRTZ_EXCL_blob_triggers
  (
    TRIGGER_NAME VARCHAR(80) NOT NULL,
    TRIGGER_GROUP VARCHAR(80) NOT NULL,
    BLOB_DATA BYTEA NULL,
    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (TRIGGER_NAME,TRIGGER_GROUP)
        REFERENCES QRTZ_EXCL_TRIGGERS(TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE QRTZ_EXCL_trigger_listeners
  (
    TRIGGER_NAME  VARCHAR(80) NOT NULL,
    TRIGGER_GROUP VARCHAR(80) NOT NULL,
    TRIGGER_LISTENER VARCHAR(80) NOT NULL,
    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP,TRIGGER_LISTENER),
    FOREIGN KEY (TRIGGER_NAME,TRIGGER_GROUP)
	REFERENCES QRTZ_EXCL_TRIGGERS(TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE QRTZ_EXCL_calendars
  (
    CALENDAR_NAME  VARCHAR(80) NOT NULL,
    CALENDAR BYTEA NOT NULL,
    PRIMARY KEY (CALENDAR_NAME)
);

CREATE TABLE QRTZ_EXCL_paused_trigger_grps
  (
    TRIGGER_GROUP  VARCHAR(80) NOT NULL,
    PRIMARY KEY (TRIGGER_GROUP)
);

CREATE TABLE QRTZ_EXCL_fired_triggers
  (
    ENTRY_ID VARCHAR(95) NOT NULL,
    TRIGGER_NAME VARCHAR(80) NOT NULL,
    TRIGGER_GROUP VARCHAR(80) NOT NULL,
    IS_VOLATILE BOOL NOT NULL,
    INSTANCE_NAME VARCHAR(80) NOT NULL,
    FIRED_TIME BIGINT NOT NULL,
    PRIORITY INTEGER NOT NULL,
    STATE VARCHAR(16) NOT NULL,
    JOB_NAME VARCHAR(80) NULL,
    JOB_GROUP VARCHAR(80) NULL,
    IS_STATEFUL BOOL NULL,
    REQUESTS_RECOVERY BOOL NULL,
    PRIMARY KEY (ENTRY_ID)
);

CREATE TABLE QRTZ_EXCL_scheduler_state
  (
    INSTANCE_NAME VARCHAR(80) NOT NULL,
    LAST_CHECKIN_TIME BIGINT NOT NULL,
    CHECKIN_INTERVAL BIGINT NOT NULL,
    PRIMARY KEY (INSTANCE_NAME)
);

CREATE TABLE QRTZ_EXCL_locks
  (
    LOCK_NAME  VARCHAR(40) NOT NULL,
    PRIMARY KEY (LOCK_NAME)
);

INSERT INTO QRTZ_EXCL_locks values('TRIGGER_ACCESS');
INSERT INTO QRTZ_EXCL_locks values('JOB_ACCESS');
INSERT INTO QRTZ_EXCL_locks values('CALENDAR_ACCESS');
INSERT INTO QRTZ_EXCL_locks values('STATE_ACCESS');
INSERT INTO QRTZ_EXCL_locks values('MISFIRE_ACCESS');

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
   tagname varchar(255) not null,
   host_id varchar(255),
   user_id varchar(255),
   persona boolean default false,
   mod_date timestamptz,
   primary key (tag_id)
);
create table user_comments (
   inode varchar(36) not null,
   user_id varchar(255),
   cdate timestamptz,
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
   locked_on timestamptz,
   version_ts timestamptz not null,
   variant_id varchar(255) default 'DEFAULT' not null,
   publish_date timestamptz,
   primary key (identifier, lang, variant_id)
);
create table fixes_audit (
   id varchar(36) not null,
   table_name varchar(255),
   action varchar(255),
   records_altered int4,
   datetime timestamptz,
   primary key (id)
);
create table container_version_info (
   identifier varchar(36) not null,
   working_inode varchar(36) not null,
   live_inode varchar(36),
   deleted bool not null,
   locked_by varchar(100),
   locked_on timestamptz,
   version_ts timestamptz not null,
   primary key (identifier)
);
create table trackback (
   id int8 not null,
   asset_identifier varchar(36),
   title varchar(255),
   excerpt varchar(255),
   url varchar(255),
   blog_name varchar(255),
   track_date timestamptz not null,
   primary key (id)
);
create table plugin (
   id varchar(255) not null,
   plugin_name varchar(255) not null,
   plugin_version varchar(255) not null,
   author varchar(255) not null,
   first_deployed_date timestamptz not null,
   last_deployed_date timestamptz not null,
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
   sent timestamptz,
   opened timestamptz,
   last_result int4,
   last_message varchar(255),
   user_id varchar(100),
   primary key (inode)
);
create table web_form (
   web_form_id varchar(36) not null,
   form_type varchar(255),
   submit_date timestamptz,
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
create table analytic_summary_period (
   id int8 not null,
   full_date timestamptz,
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
   avg_time_on_site timestamptz,
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
   mod_date timestamptz,
   mod_user varchar(100),
   sort_order int4,
   friendly_name varchar(255),
   body text,
   header text,
   footer text,
   image varchar(36),
   identifier varchar(36),
   drawed bool,
   drawed_body text,
   add_container_links int4,
   containers_added int4,
   head_code text,
   theme varchar(255),
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
   expire_date_var varchar(255),
   publish_date_var varchar(255),
   mod_date timestamptz,
   sort_order int4,
   icon varchar(255),
   marked_for_deletion bool not null default false,
   metadata JSONB NULL,
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
create table container_structures (
   id varchar(36) not null,
   container_id varchar(36) not null,
   container_inode varchar(36) not null,
   structure_id varchar(36) not null,
   code text,
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
	mod_date timestamptz,
	mod_user varchar(100),
	sort_order int4,
	friendly_name varchar(255),
	structure_inode varchar(36),
	disabled_wysiwyg varchar(1000),
	identifier varchar(36),
	language_id int8,
    variant_id varchar(255) default 'DEFAULT',
	contentlet_as_json jsonb,
	date1 timestamptz,
	date2 timestamptz,
	date3 timestamptz,
	date4 timestamptz,
	date5 timestamptz,
	date6 timestamptz,
	date7 timestamptz,
	date8 timestamptz,
	date9 timestamptz,
	date10 timestamptz,
	date11 timestamptz,
	date12 timestamptz,
	date13 timestamptz,
	date14 timestamptz,
	date15 timestamptz,
	date16 timestamptz,
	date17 timestamptz,
	date18 timestamptz,
	date19 timestamptz,
	date20 timestamptz,
	date21 timestamptz,
	date22 timestamptz,
	date23 timestamptz,
	date24 timestamptz,
	date25 timestamptz,
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
create table report_asset (
   inode varchar(36) not null,
   report_name varchar(255) not null,
   report_description varchar(1000) not null,
   requires_input bool,
   ds varchar(100) not null,
   web_form_report bool,
   primary key (inode)
);
create table workflow_comment (
   id varchar(36) not null,
   creation_date timestamptz,
   posted_by varchar(255),
   wf_comment text,
   workflowtask_id varchar(36),
   primary key (id)
);
create table category (
   inode varchar(36) not null,
   category_name varchar(255),
   category_key varchar(255),
   sort_order int4,
   active bool,
   keywords text,
   category_velocity_var_name varchar(255),
   mod_date timestamptz,
   primary key (inode)
);
create table chain_link_code (
   id int8 not null,
   class_name varchar(255) unique,
   code text not null,
   last_mod_date timestamptz not null,
   language varchar(255) not null,
   primary key (id)
);
create table analytic_summary_visits (
   id int8 not null,
   summary_period_id int8 not null,
   host_id varchar(36),
   visit_time timestamptz,
   visits int8,
   primary key (id)
);
create table template_version_info (
   identifier varchar(36) not null,
   working_inode varchar(36) not null,
   live_inode varchar(36),
   deleted bool not null,
   locked_by varchar(100),
   locked_on timestamptz,
   version_ts timestamptz not null,
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
   syspublish_date timestamptz,
   sysexpire_date timestamptz,
   owner varchar(255),
   create_date timestamptz,
   asset_subtype varchar(255),
   primary key (id),
   unique (parent_path, asset_name, host_inode)
);
create table clickstream (
   clickstream_id int8 not null,
   cookie_id varchar(255),
   user_id varchar(255),
   start_date timestamptz,
   end_date timestamptz,
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
   relation_type varchar(64) not null,
   tree_order int4,
   personalization varchar(255) not null default 'dot:default',
   variant_id varchar(255) default 'DEFAULT' not null,
   style_properties JSONB,
   primary key (child, parent1, parent2, relation_type, personalization, variant_id)
);
create table workflow_task (
   id varchar(36) not null,
   creation_date timestamptz,
   mod_date timestamptz,
   due_date timestamptz,
   created_by varchar(255),
   assigned_to varchar(255),
   belongs_to varchar(255),
   title varchar(255),
   description text,
   status varchar(255),
   webasset varchar(255),
   language_id INT8,
   primary key (id)
);

create table workflow_action_mappings (
   id varchar(36) not null,
   action varchar(36) not null,
   workflow_action varchar(255) not null,
   scheme_or_content_type  varchar(255) not null,
   primary key (id)
);

CREATE UNIQUE INDEX idx_workflow_action_mappings ON workflow_action_mappings (action, workflow_action, scheme_or_content_type);

insert into workflow_action_mappings(id, action, workflow_action, scheme_or_content_type)
values ('3d6be719-6b61-4ef8-a594-a9764e461597','NEW'      ,'ceca71a0-deee-4999-bd47-b01baa1bcfc8','d61a59e1-a49c-46f2-a929-db2b4bfa88b2');
insert into workflow_action_mappings(id, action, workflow_action, scheme_or_content_type)
values ('63865890-c863-43a1-ab61-4b495dba5eb5','EDIT'     ,'ceca71a0-deee-4999-bd47-b01baa1bcfc8','d61a59e1-a49c-46f2-a929-db2b4bfa88b2');
insert into workflow_action_mappings(id, action, workflow_action, scheme_or_content_type)
values ('2016a72e-85c7-4ee0-936f-36ce52df355e','PUBLISH'  ,'000ec468-0a63-4283-beb7-fcb36c107b2f','d61a59e1-a49c-46f2-a929-db2b4bfa88b2');
insert into workflow_action_mappings(id, action, workflow_action, scheme_or_content_type)
values ('3ec446c8-a9b6-47fe-830f-1e623493090c','UNPUBLISH','38efc763-d78f-4e4b-b092-59cd8c579b93','d61a59e1-a49c-46f2-a929-db2b4bfa88b2');
insert into workflow_action_mappings(id, action, workflow_action, scheme_or_content_type)
values ('e7b8c8a3-e605-473c-8680-6d95cac15c9b','ARCHIVE'  ,'4da13a42-5d59-480c-ad8f-94a3adf809fe','d61a59e1-a49c-46f2-a929-db2b4bfa88b2');
insert into workflow_action_mappings(id, action, workflow_action, scheme_or_content_type)
values ('99019118-df2c-4297-a5aa-2fe3fe0f52ce','UNARCHIVE','c92f9aa1-9503-4567-ac30-d3242b54d02d','d61a59e1-a49c-46f2-a929-db2b4bfa88b2');
insert into workflow_action_mappings(id, action, workflow_action, scheme_or_content_type)
values ('d073436e-3c10-4e4c-8c97-225e9cddf320','DELETE'   ,'777f1c6b-c877-4a37-ba4b-10627316c2cc','d61a59e1-a49c-46f2-a929-db2b4bfa88b2');
insert into workflow_action_mappings(id, action, workflow_action, scheme_or_content_type)
values ('3d73437e-3f1c-8e5c-ac97-a25e9cddf320','DESTROY'  ,'1e0f1c6b-b67f-4c99-983d-db2b4bfa88b2','d61a59e1-a49c-46f2-a929-db2b4bfa88b2');


create table tag_inode (
   tag_id varchar(100) not null,
   inode varchar(100) not null,
   field_var_name varchar(255),
   mod_date timestamptz,
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
   timestamptzper timestamptz,
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
   rating_date timestamptz,
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
   mod_date timestamptz,
   action varchar(255),
   name varchar(255),
   primary key (id)
);
create table dashboard_user_preferences (
   id int8 not null,
   summary_404_id int8,
   user_id varchar(255),
   ignored bool,
   mod_date timestamptz,
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
   start_date timestamptz,
   completed_date timestamptz,
   active bool,
   locked bool,
   sends_per_hour varchar(15),
   sendemail bool,
   communicationinode varchar(36),
   userfilterinode varchar(36),
   sendto varchar(15),
   isrecurrent bool,
   wassent bool,
   expiration_date timestamptz,
   parent_campaign varchar(36),
   primary key (inode)
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
create table dot_containers (
   inode varchar(36) not null,
   code text,
   pre_loop text,
   post_loop text,
   show_on_menu bool,
   title varchar(255),
   mod_date timestamptz,
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
   mod_date timestamptz,
   modified_by varchar(255),
   ext_comm_id varchar(255),
   primary key (inode)
);
create table workflow_history (
   id varchar(36) not null,
   creation_date timestamptz,
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
   last_mod_date timestamptz,
   primary key (id)
);
create table links (
   inode varchar(36) not null,
   show_on_menu bool,
   title varchar(255),
   mod_date timestamptz,
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
   mod_date timestamptz,
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
   mod_date timestamptz,
   primary key (inode),
   unique (relation_type_value)
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
   mod_date timestamptz,
   owner varchar(255),
   idate timestamptz,
   primary key (inode)
);
create table clickstream_404 (
   clickstream_404_id int8 not null,
   referer_uri varchar(255),
   query_string text,
   request_uri varchar(255),
   user_id varchar(255),
   host_id varchar(36),
   timestamptzper timestamptz,
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
   last_mod_date timestamptz,
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
   locked_on timestamptz,
   version_ts timestamptz not null,
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
   birthday timestamptz,
   birthdayfrom timestamptz,
   birthdayto timestamptz,
   lastlogintypesearch varchar(100),
   lastloginsince varchar(100),
   loginfrom timestamptz,
   loginto timestamptz,
   createdtypesearch varchar(100),
   createdsince varchar(100),
   createdfrom timestamptz,
   createdto timestamptz,
   lastvisittypesearch varchar(100),
   lastvisitsince varchar(100),
   lastvisitfrom timestamptz,
   lastvisitto timestamptz,
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
   idate timestamptz,
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
create index idx_analytic_summary_visits_2 on analytic_summary_visits (visit_time);
create index idx_analytic_summary_visits_1 on analytic_summary_visits (host_id);
alter table analytic_summary_visits add constraint fk9eac9733b7b46300 foreign key (summary_period_id) references analytic_summary_period;
create index idx_preference_1 on user_preferences (preference);
create index idx_identifier_pub on identifier (syspublish_date);
create index idx_identifier_exp on identifier (sysexpire_date);
create index idx_identifier_asset_subtype on identifier (asset_subtype);
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
create index idx_workflow_6 on workflow_task (language_id);
create index idx_workflow_4 on workflow_task (webasset);
create index idx_workflow_5 on workflow_task (created_by);
create index idx_workflow_2 on workflow_task (belongs_to);
create index idx_workflow_3 on workflow_task (status);
create index idx_workflow_1 on workflow_task (assigned_to);
create index idx_click_1 on click (link);
alter table click add constraint fk5a5c5885fb51eb foreign key (inode) references inode;
create index idx_user_clickstream_request_2 on clickstream_request (request_uri);
create index idx_user_clickstream_request_1 on clickstream_request (clickstream_id);
create index idx_user_clickstream_request_4 on clickstream_request (timestamptzper);
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
alter table dot_containers add constraint fk8a844125fb51eb foreign key (inode) references inode;
alter table communication add constraint fkc24acfd65fb51eb foreign key (inode) references inode;
alter table links add constraint fk6234fb95fb51eb foreign key (inode) references inode;
create index idx_field_1 on field (structure_inode);
alter table field add constraint fk5cea0fa5fb51eb foreign key (inode) references inode;
create index idx_relationship_1 on relationship (parent_structure_inode);
create index idx_relationship_2 on relationship (child_structure_inode);
create index idx_folder_1 on folder (name);
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
create sequence permission_reference_seq;
create sequence summary_visits_seq;
create sequence chain_link_code_seq;
create sequence clickstream_seq;
create sequence summary_404_seq;
create sequence content_rating_sequence;
create sequence chain_seq;
create sequence summary_content_seq;
create sequence summary_pages_seq;
create sequence summary_referer_seq;
create sequence summary_period_seq;
create sequence workstream_seq;
create sequence clickstream_request_seq;
create sequence clickstream_404_seq;
create sequence user_to_delete_seq;
create sequence chain_state_parameter_seq;
create sequence permission_seq;
--postgres
CREATE INDEX idx_tree ON tree USING btree (child, parent, relation_type);
CREATE INDEX idx_tree_1 ON tree USING btree (parent);
CREATE INDEX idx_tree_2 ON tree USING btree (child);
CREATE INDEX idx_tree_3 ON tree USING btree (relation_type);
CREATE INDEX idx_tree_4 ON tree USING btree (parent, child, relation_type);
CREATE INDEX idx_tree_5 ON tree USING btree (parent, relation_type);
CREATE INDEX idx_tree_6 ON tree USING btree (child, relation_type);

CREATE INDEX idx_contentlet_3 ON contentlet USING btree (inode);

CREATE INDEX idx_permisision_4 ON permission USING btree (permission_type);

CREATE INDEX idx_permission_reference_2 ON permission_reference USING btree(reference_id);
CREATE INDEX idx_permission_reference_3 ON permission_reference USING btree(reference_id,permission_type);
CREATE INDEX idx_permission_reference_4 ON permission_reference USING btree(asset_id,permission_type);
CREATE INDEX idx_permission_reference_5 ON permission_reference USING btree(asset_id,reference_id,permission_type);
CREATE INDEX idx_permission_reference_6 ON permission_reference USING btree(permission_type);

CREATE UNIQUE INDEX idx_field_velocity_structure ON field (velocity_var_name,structure_inode);


alter table chain_state add constraint fk_state_chain foreign key (chain_id) references chain(id);
alter table chain_state add constraint fk_state_code foreign key (link_code_id) references chain_link_code(id);
alter table chain_state_parameter add constraint fk_parameter_state foreign key (chain_state_id) references chain_state(id);

alter table permission add constraint permission_role_fk foreign key (roleid) references cms_role(id);

alter table contentlet add constraint FK_structure_inode foreign key (structure_inode) references structure(inode);


ALTER TABLE structure ALTER fixed SET NOT NULL;
ALTER TABLE structure ALTER fixed SET DEFAULT false;

ALTER TABLE field ALTER fixed SET NOT NULL;
ALTER TABLE field ALTER fixed SET DEFAULT false;
ALTER TABLE field ALTER read_only SET NOT NULL;
ALTER TABLE field ALTER read_only SET DEFAULT false;

ALTER TABLE campaign ALTER active SET DEFAULT false;

insert into User_ (userId, companyId, createDate, password_, passwordEncrypted, passwordReset, firstName, middleName, lastName, male, birthday, emailAddress, skinId, dottedSkins, roundedSkins, greeting, layoutIds, loginDate, failedLoginAttempts, agreedToTermsOfUse, active_) values ('dotcms.org.default', 'default', current_timestamp, 'password', 'f', 'f', '', '', '', 't', '01/01/1970', 'default@dotcms.org', '01', 'f', 'f', 'Welcome!', '', current_timestamp, 0, 'f', 't');
create index addres_userid_index on address(userid);
create index tag_user_id_index on tag(user_id);
create index tag_is_persona_index on tag(persona);
create index tag_inode_tagid on tag_inode(tag_id);
create index tag_inode_inode on tag_inode(inode);
-- These two indexes are here instead of the hibernate file because Oracle by default creates an index on a unique field.  So creating an index would try to create the same index twice.
create index idx_chain_link_code_classname on chain_link_code (class_name);
create index idx_chain_key_name on chain (key_name);
CREATE TABLE dist_journal
(
  id bigserial NOT NULL,
  object_to_index character varying(1024) NOT NULL,
  serverid character varying(64),
  journal_type integer NOT NULL,
  time_entered timestamptz NOT NULL,
  CONSTRAINT dist_journal_pkey PRIMARY KEY (id),
  CONSTRAINT dist_journal_object_to_index_key UNIQUE (object_to_index, serverid, journal_type)
);

create table plugin_property (
   plugin_id varchar(255) not null,
   propkey varchar(255) not null,
   original_value varchar(255) not null,
   current_value varchar(255) not null,
   primary key (plugin_id, propkey)
);
alter table plugin_property add constraint fk_plugin_plugin_property foreign key (plugin_id) references plugin(id);

CREATE TABLE dist_process ( id bigserial NOT NULL, object_to_index character varying(1024) NOT NULL, serverid character varying(64), journal_type integer NOT NULL, time_entered timestamptz NOT NULL, CONSTRAINT dist_process_pkey PRIMARY KEY (id));
CREATE INDEX dist_process_index on dist_process (object_to_index, serverid,journal_type);

CREATE TABLE dist_reindex_journal
(
  id bigserial NOT NULL,
  inode_to_index character varying(100) NOT NULL,
  ident_to_index character varying(100) NOT NULL,
  serverid character varying(64),
  priority integer NOT NULL,
  time_entered timestamptz NOT NULL DEFAULT CURRENT_DATE,
  index_val varchar(325),
  dist_action integer NOT NULL DEFAULT 1,
  CONSTRAINT dist_reindex_journal_pkey PRIMARY KEY (id)
);

CREATE INDEX dist_reindex_index1 on dist_reindex_journal (inode_to_index);
CREATE INDEX dist_reindex_index2 on dist_reindex_journal (dist_action);
CREATE INDEX dist_reindex_index3 on dist_reindex_journal (serverid);
CREATE INDEX dist_reindex_index4 on dist_reindex_journal (ident_to_index,serverid);
CREATE INDEX dist_reindex_index on dist_reindex_journal (serverid,dist_action);
CREATE INDEX dist_reindex_index5 ON dist_reindex_journal (priority, time_entered);
CREATE INDEX dist_reindex_index6 ON dist_reindex_journal (priority);
CREATE INDEX idx_identifier ON identifier USING btree (id);

CREATE TABLE quartz_log (id bigserial NOT NULL, JOB_NAME character varying(255) NOT NULL, serverid character varying(64), time_started timestamptz NOT NULL, CONSTRAINT quartz_log_pkey PRIMARY KEY (id));

alter table cms_role add CONSTRAINT cms_role_name_role_key UNIQUE (role_key);
alter table cms_role add CONSTRAINT cms_role_name_db_fqn UNIQUE (db_fqn);
alter table cms_role add constraint fkcms_role_parent foreign key (parent) references cms_role;

alter table users_cms_roles add CONSTRAINT users_cms_roles_parent1 UNIQUE (role_id,user_id);
alter table users_cms_roles add constraint fkusers_cms_roles1 foreign key (role_id) references cms_role;
alter table users_cms_roles add constraint fkusers_cms_roles2 foreign key (user_id) references user_;

ALTER TABLE cms_layout add CONSTRAINT cms_layout_name_parent UNIQUE (layout_name);

alter table portlet add CONSTRAINT portlet_role_key UNIQUE (portletid);
alter table cms_layouts_portlets add CONSTRAINT cms_layouts_portlets_parent1 UNIQUE (layout_id,portlet_id);
alter table cms_layouts_portlets add constraint fkcms_layouts_portlets foreign key (layout_id) references cms_layout;

alter table layouts_cms_roles add constraint fklayouts_cms_roles1 foreign key (role_id) references cms_role;
alter table layouts_cms_roles add constraint fklayouts_cms_roles2 foreign key (layout_id) references cms_layout;
alter table layouts_cms_roles add CONSTRAINT layouts_cms_roles_parent1 UNIQUE (role_id,layout_id);

ALTER TABLE dot_containers add constraint containers_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE template add constraint template_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE contentlet add constraint content_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE links add constraint links_identifier_fk foreign key (identifier) references identifier(id);


CREATE OR REPLACE FUNCTION "boolIntResult"("intParam" integer, "boolParam" boolean)
  RETURNS boolean AS
$BODY$select case
		WHEN $2 AND $1 != 0 then true
		WHEN $2 != true AND $1 = 0 then true
		ELSE false
	END$BODY$
  LANGUAGE 'sql' VOLATILE
;
CREATE OR REPLACE FUNCTION "intBoolResult"("boolParam" boolean, "intParam" integer)
  RETURNS boolean AS
$BODY$select case
		WHEN $1 AND $2 != 0 then true
		WHEN $1 != true AND $2 = 0 then true
		ELSE false
	END$BODY$
  LANGUAGE 'sql' VOLATILE
 ;
CREATE OPERATOR =(
  PROCEDURE = "intBoolResult",
  LEFTARG = bool,
  RIGHTARG = int4);

CREATE OPERATOR =(
  PROCEDURE = "boolIntResult",
  LEFTARG = int4,
  RIGHTARG = bool);

CREATE OR REPLACE FUNCTION "boolBigIntResult"("intParam" bigint, "boolParam" boolean)
  RETURNS boolean AS
$BODY$select case
		WHEN $2 AND $1 != 0 then true
		WHEN $2 != true AND $1 = 0 then true
		ELSE false
	END$BODY$
  LANGUAGE 'sql' VOLATILE
;
CREATE OR REPLACE FUNCTION "bigIntBoolResult"("boolParam" boolean, "intParam" bigint)
  RETURNS boolean AS
$BODY$select case
		WHEN $1 AND $2 != 0 then true
		WHEN $1 != true AND $2 = 0 then true
		ELSE false
	END$BODY$
  LANGUAGE 'sql' VOLATILE
;
CREATE OPERATOR =(
   PROCEDURE="bigIntBoolResult",
   LEFTARG=boolean,
   RIGHTARG=bigint);


CREATE OPERATOR =(
  PROCEDURE = "boolBigIntResult",
  LEFTARG = bigint,
  RIGHTARG = bool);

CREATE OR REPLACE FUNCTION identifier_host_inode_check() RETURNS trigger AS '
DECLARE
	inodeType varchar(100);
BEGIN
  IF (tg_op = ''INSERT'' OR tg_op = ''UPDATE'') AND substr(NEW.asset_type, 0, 8) <> ''content'' AND
		(NEW.host_inode IS NULL OR NEW.host_inode = '''') THEN
		RAISE EXCEPTION ''Cannot insert/update a null or empty host inode for this kind of identifier'';
		RETURN NULL;
  ELSE
		RETURN NEW;
  END IF;

  RETURN NULL;
END
' LANGUAGE plpgsql;

CREATE TRIGGER required_identifier_host_inode_trigger BEFORE INSERT OR UPDATE
    ON identifier FOR EACH ROW
    EXECUTE PROCEDURE identifier_host_inode_check ();

create table import_audit (
	id bigint not null,
	start_date timestamptz,
	userid varchar(255),
	filename varchar(512),
	status int,
	last_inode varchar(100),
	records_to_import bigint,
	serverid varchar(255),
	primary key (id)
	);

alter table category alter column category_velocity_var_name set not null;

alter table import_audit add column warnings text,
	add column errors text,
	add column results text,
	add column messages text;

alter table structure alter host set default 'SYSTEM_HOST';
alter table structure alter folder set default 'SYSTEM_FOLDER';
alter table structure alter column velocity_var_name set not null;
alter table structure add constraint unique_struct_vel_var_name unique (velocity_var_name);
create index idx_structure_host on structure (host);
create index idx_structure_folder on structure (folder);

-- calculated identifier column
CREATE OR REPLACE FUNCTION full_path_lc(identifier) RETURNS text
    AS ' SELECT CASE WHEN $1.parent_path = ''/System folder'' then ''/'' else LOWER($1.parent_path || $1.asset_name) end; '
LANGUAGE SQL;

-- Case sensitive unique asset-name,parent_path for a given host
CREATE UNIQUE INDEX idx_ident_uniq_asset_name on identifier (full_path_lc(identifier),host_inode);

CREATE OR REPLACE FUNCTION structure_host_folder_check() RETURNS trigger AS '
DECLARE
    folderInode varchar(100);
    hostInode varchar(100);
BEGIN
    IF ((tg_op = ''INSERT'' OR tg_op = ''UPDATE'') AND (NEW.host IS NOT NULL AND NEW.host <> '''' AND NEW.host <> ''SYSTEM_HOST''
          AND NEW.folder IS NOT NULL AND NEW.folder <> ''SYSTEM_FOLDER'' AND NEW.folder <> '''')) THEN
          select host_inode,folder.inode INTO hostInode,folderInode from folder,identifier where folder.identifier = identifier.id and folder.inode=NEW.folder;
	  IF (FOUND AND NEW.host = hostInode) THEN
		 RETURN NEW;
	  ELSE
		 RAISE EXCEPTION ''Cannot assign host/folder to structure, folder does not belong to given host'';
		 RETURN NULL;
	  END IF;
    ELSE
        IF((tg_op = ''INSERT'' OR tg_op = ''UPDATE'') AND (NEW.host IS NULL OR NEW.host = '''' OR NEW.host= ''SYSTEM_HOST''
           OR NEW.folder IS NULL OR NEW.folder = '''' OR NEW.folder = ''SYSTEM_FOLDER'')) THEN
          IF(NEW.host = ''SYSTEM_HOST'' OR NEW.host IS NULL OR NEW.host = '''') THEN
             NEW.host = ''SYSTEM_HOST'';
             NEW.folder = ''SYSTEM_FOLDER'';
          END IF;
          IF(NEW.folder = ''SYSTEM_FOLDER'' OR NEW.folder IS NULL OR NEW.folder = '''') THEN
             NEW.folder = ''SYSTEM_FOLDER'';
          END IF;
        RETURN NEW;
        END IF;
    END IF;
  RETURN NULL;
END
' LANGUAGE plpgsql;

CREATE TRIGGER structure_host_folder_trigger BEFORE INSERT OR UPDATE
    ON structure FOR EACH ROW
    EXECUTE PROCEDURE structure_host_folder_check();

CREATE OR REPLACE FUNCTION content_versions_check() RETURNS trigger AS '
   DECLARE
       versionsCount integer;
   BEGIN
       IF (tg_op = ''DELETE'') THEN
         select count(*) into versionsCount from contentlet where identifier = OLD.identifier;
         IF (versionsCount = 0)THEN
		DELETE from identifier where id = OLD.identifier;
	   ELSE
	      RETURN OLD;
	   END IF;
	END IF;
   RETURN NULL;
   END
  ' LANGUAGE plpgsql;
 CREATE TRIGGER content_versions_check_trigger AFTER DELETE
 ON contentlet FOR EACH ROW
 EXECUTE PROCEDURE content_versions_check();

CREATE OR REPLACE FUNCTION link_versions_check() RETURNS trigger AS '
  DECLARE
	versionsCount integer;
  BEGIN
  IF (tg_op = ''DELETE'') THEN
    select count(*) into versionsCount from links where identifier = OLD.identifier;
    IF (versionsCount = 0)THEN
	DELETE from identifier where id = OLD.identifier;
    ELSE
	RETURN OLD;
    END IF;
  END IF;
RETURN NULL;
END
' LANGUAGE plpgsql;
CREATE TRIGGER link_versions_check_trigger AFTER DELETE
ON links FOR EACH ROW
EXECUTE PROCEDURE link_versions_check();


CREATE OR REPLACE FUNCTION container_versions_check() RETURNS trigger AS '
  DECLARE
	versionsCount integer;
  BEGIN
  IF (tg_op = ''DELETE'') THEN
    select count(*) into versionsCount from dot_containers where identifier = OLD.identifier;
    IF (versionsCount = 0)THEN
	DELETE from identifier where id = OLD.identifier;
    ELSE
	RETURN OLD;
    END IF;
  END IF;
RETURN NULL;
END
' LANGUAGE plpgsql;

CREATE TRIGGER container_versions_check_trigger AFTER DELETE
ON dot_containers FOR EACH ROW
EXECUTE PROCEDURE container_versions_check();

CREATE OR REPLACE FUNCTION template_versions_check() RETURNS trigger AS '
  DECLARE
	versionsCount integer;
  BEGIN
  IF (tg_op = ''DELETE'') THEN
    select count(*) into versionsCount from template where identifier = OLD.identifier;
    IF (versionsCount = 0)THEN
	DELETE from identifier where id = OLD.identifier;
    ELSE
	RETURN OLD;
    END IF;
  END IF;
RETURN NULL;
END
' LANGUAGE plpgsql;

CREATE TRIGGER template_versions_check_trigger AFTER DELETE
ON template FOR EACH ROW
EXECUTE PROCEDURE template_versions_check();

CREATE OR REPLACE FUNCTION identifier_parent_path_check() RETURNS trigger AS '
 DECLARE
    folderId varchar(100);
  BEGIN
     IF (tg_op = ''INSERT'' OR tg_op = ''UPDATE'') THEN
      IF(NEW.parent_path=''/'') OR (NEW.parent_path=''/System folder'') THEN
        RETURN NEW;
     ELSE
      select id into folderId from identifier where asset_type=''folder'' and host_inode = NEW.host_inode and lower(parent_path||asset_name||''/'') = lower(NEW.parent_path) and id <> NEW.id;
      IF FOUND THEN
        RETURN NEW;
      ELSE
        RAISE EXCEPTION ''Cannot % folder % [%] in path % as one or more parent folders do not exist in Site %'', tg_op, NEW.asset_name, NEW.id, NEW.parent_path, NEW.host_inode;
        RETURN NULL;
      END IF;
     END IF;
    END IF;
RETURN NULL;
END
  ' LANGUAGE plpgsql;
CREATE TRIGGER identifier_parent_path_trigger
  BEFORE INSERT OR UPDATE
  ON identifier FOR EACH ROW
  EXECUTE PROCEDURE identifier_parent_path_check();

CREATE OR REPLACE FUNCTION check_child_assets() RETURNS trigger AS '
DECLARE
   pathCount integer;
BEGIN
   IF (tg_op = ''DELETE'') THEN
      IF(OLD.asset_type =''folder'') THEN
	   select count(*) into pathCount from identifier where parent_path = OLD.parent_path||OLD.asset_name||''/''  and host_inode = OLD.host_inode;
	END IF;
	IF(OLD.asset_type =''contentlet'') THEN
	   select count(*) into pathCount from identifier where host_inode = OLD.id;
	END IF;
	IF (pathCount > 0 )THEN
	  RAISE EXCEPTION ''Cannot delete as this path has children'';
	  RETURN NULL;
	ELSE
	  RETURN OLD;
	END IF;
   END IF;
   RETURN NULL;
END
' LANGUAGE plpgsql;

CREATE TRIGGER check_child_assets_trigger BEFORE DELETE
ON identifier FOR EACH ROW
EXECUTE PROCEDURE check_child_assets();

alter table structure add constraint fk_structure_host foreign key (host) references identifier(id);

create index idx_template3 on template (lower(title));

CREATE INDEX idx_contentlet_4 ON contentlet (structure_inode);
CREATE INDEX idx_contentlet_variant ON contentlet (variant_id);
CREATE INDEX idx_contentlet_identifier ON contentlet (identifier);

alter table contentlet add constraint fk_user_contentlet foreign key (mod_user) references user_(userid);
alter table dot_containers add constraint fk_user_containers foreign key (mod_user) references user_(userid);
alter table template add constraint fk_user_template foreign key (mod_user) references user_(userid);
alter table links add constraint fk_user_links foreign key (mod_user) references user_(userid);

ALTER TABLE Folder add constraint folder_identifier_fk foreign key (identifier) references identifier(id);
--ALTER TABLE dot_containers add constraint structure_fk foreign key (structure_inode) references structure(inode);

CREATE OR REPLACE FUNCTION folder_identifier_check() RETURNS trigger AS '
DECLARE
   versionsCount integer;
BEGIN
   IF (tg_op = ''DELETE'') THEN
      select count(*) into versionsCount from folder where identifier = OLD.identifier;
	IF (versionsCount = 0)THEN
	  DELETE from identifier where id = OLD.identifier;
	ELSE
	  RETURN OLD;
	END IF;
   END IF;
   RETURN NULL;
END
' LANGUAGE plpgsql;
CREATE TRIGGER folder_identifier_check_trigger AFTER DELETE
ON folder FOR EACH ROW
EXECUTE PROCEDURE folder_identifier_check();

create index idx_template_id on template_containers(template_id);
alter table template_containers add constraint FK_template_id foreign key (template_id) references identifier(id);
alter table template_containers add constraint FK_container_id foreign key (container_id) references identifier(id);

CREATE OR REPLACE FUNCTION renameFolderChildren(old_path varchar(255),new_path varchar(255),hostInode varchar(255))
RETURNS void AS '
DECLARE
   fi identifier;
   new_folder_path varchar(255);
   old_folder_path varchar(255);
BEGIN
    UPDATE identifier SET  parent_path  = new_path where parent_path = old_path and host_inode = hostInode;
    FOR fi IN select * from identifier where asset_type=''folder'' and parent_path = new_path and host_inode = hostInode LOOP
	 new_folder_path := new_path ||fi.asset_name||''/'';
	 old_folder_path := old_path ||fi.asset_name||''/'';
	 PERFORM renameFolderChildren(old_folder_path,new_folder_path,hostInode);
    END LOOP;
END
'LANGUAGE plpgsql;
CREATE OR REPLACE FUNCTION rename_folder_and_assets()
RETURNS trigger AS '
DECLARE
   old_parent_path varchar(255);
   old_path varchar(255);
   new_path varchar(255);
   old_name varchar(255);
   hostInode varchar(100);
BEGIN
   IF (tg_op = ''UPDATE'' AND NEW.name<>OLD.name) THEN
      select asset_name,parent_path,host_inode INTO old_name,old_parent_path,hostInode from identifier where id = NEW.identifier;
      old_path := old_parent_path || old_name || ''/'';
      new_path := old_parent_path || NEW.name || ''/'';
      UPDATE identifier SET asset_name = NEW.name where id = NEW.identifier;
      PERFORM renameFolderChildren(old_path,new_path,hostInode);
      RETURN NEW;
   END IF;
RETURN NULL;
END
'LANGUAGE plpgsql;
CREATE TRIGGER rename_folder_assets_trigger AFTER UPDATE
ON Folder FOR EACH ROW
EXECUTE PROCEDURE rename_folder_and_assets();

CREATE OR REPLACE FUNCTION dotFolderPath(parent_path text, asset_name text)
  RETURNS text AS '
BEGIN
  IF(parent_path=''/System folder'') THEN
    RETURN ''/'';
  ELSE
    RETURN parent_path || asset_name || ''/'';
  END IF;
END;'
LANGUAGE plpgsql;

alter table contentlet_version_info add constraint fk_contentlet_version_info_identifier foreign key (identifier) references identifier(id) on delete cascade;
alter table container_version_info  add constraint fk_container_version_info_identifier  foreign key (identifier) references identifier(id);
alter table template_version_info   add constraint fk_template_version_info_identifier   foreign key (identifier) references identifier(id);
alter table link_version_info       add constraint fk_link_version_info_identifier       foreign key (identifier) references identifier(id);

alter table contentlet_version_info add constraint fk_contentlet_version_info_working foreign key (working_inode) references contentlet(inode);
alter table container_version_info  add constraint fk_container_version_info_working  foreign key (working_inode) references dot_containers(inode);
alter table template_version_info   add constraint fk_template_version_info_working   foreign key (working_inode) references template(inode);
alter table link_version_info       add constraint fk_link_version_info_working       foreign key (working_inode) references links(inode);

alter table contentlet_version_info add constraint fk_contentlet_version_info_live foreign key (live_inode) references contentlet(inode);
alter table container_version_info  add constraint fk_container_version_info_live  foreign key (live_inode) references dot_containers(inode);
alter table template_version_info   add constraint fk_template_version_info_live   foreign key (live_inode) references template(inode);
alter table link_version_info       add constraint fk_link_version_info_live       foreign key (live_inode) references links(inode);

alter table contentlet_version_info add constraint fk_contentlet_version_info_lang foreign key (lang) references language(id);

alter table folder add constraint fk_folder_file_structure_type foreign key(default_file_type) references structure(inode);

alter table workflowtask_files add constraint FK_workflow_id foreign key (workflowtask_id) references workflow_task(id);
CREATE INDEX IF NOT EXISTS workflowtask_files_hash_idx ON workflowtask_files USING HASH(workflowtask_id);
alter table workflow_comment add constraint workflowtask_id_comment_FK foreign key (workflowtask_id) references workflow_task(id);
CREATE INDEX IF NOT EXISTS workflow_comment_hash_idx ON workflow_comment USING HASH(workflowtask_id);
alter table workflow_history add constraint workflowtask_id_history_FK foreign key (workflowtask_id) references workflow_task(id);
CREATE INDEX IF NOT EXISTS workflow_history_hash_idx ON workflow_history USING HASH(workflowtask_id);
alter table workflow_task add constraint unique_workflow_task unique (webasset,language_id);

alter table contentlet add constraint fk_contentlet_lang foreign key (language_id) references language(id);

alter table Company add constraint fk_default_lang_id foreign key (default_language_id) references language(id);

create table workflow_scheme(
	id varchar(36) primary key,
	name varchar(255) not null,
    variable_name varchar(255) not null unique,
	description text,
	archived boolean default false,
	mandatory boolean default false,
	default_scheme boolean default false,
	entry_action_id varchar(36),
	mod_date timestamptz
);
CREATE INDEX idx_workflow_lower_variable_name ON workflow_scheme (LOWER(variable_name));

create table workflow_step(
	id varchar(36) primary key,
	name varchar(255) not null,
	scheme_id varchar(36) references workflow_scheme(id),
	my_order int default 0,
	resolved boolean default false,
	escalation_enable boolean default false,
	escalation_action varchar(36),
	escalation_time int default 0
);
create index workflow_idx_step_scheme on workflow_step(scheme_id);

-- Permissionable ---
create table workflow_action(
	id varchar(36) primary key,
	step_id varchar(36),
	name varchar(255) not null,
	condition_to_progress text,
	next_step_id varchar(36),
	next_assign varchar(36) not null references cms_role(id),
	my_order int default 0,
	assignable boolean default false,
	commentable boolean default false,
	requires_checkout boolean default false,
	icon varchar(255) default 'defaultWfIcon',
    show_on varchar(255) default 'LOCKED,UNLOCKED',
	use_role_hierarchy_assign bool default false,
    scheme_id VARCHAR(36) NOT NULL,
    metadata JSONB NULL
);

CREATE TABLE workflow_action_step (action_id VARCHAR(36) NOT NULL, step_id VARCHAR(36) NOT NULL, action_order INT default 0);
ALTER  TABLE workflow_action_step ADD CONSTRAINT pk_workflow_action_step PRIMARY KEY (action_id, step_id);
ALTER  TABLE workflow_action_step ADD CONSTRAINT fk_w_action_step_action_id foreign key (action_id) references workflow_action(id);
ALTER  TABLE workflow_action_step ADD CONSTRAINT fk_w_action_step_step_id   foreign key (step_id)   references workflow_step  (id);

create table workflow_action_class(
	id varchar(36) primary key,
	action_id varchar(36) references workflow_action(id),
	name varchar(255) not null,
	my_order int default 0,
	clazz text
);
create index workflow_idx_action_class_action on workflow_action_class(action_id);

create table workflow_action_class_pars(
	id varchar(36) primary key,
	workflow_action_class_id varchar(36) references workflow_action_class(id),
	key varchar(255) not null,
	value text
);
create index workflow_idx_action_class_param_action on
	workflow_action_class_pars(workflow_action_class_id);


create table workflow_scheme_x_structure(
	id varchar(36) primary key,
	scheme_id varchar(36) references workflow_scheme(id),
	structure_id varchar(36) references structure(inode)
);


delete from workflow_history;
delete from workflow_comment;
delete from workflowtask_files;
delete from workflow_task;
alter table workflow_task add constraint FK_workflow_task_language foreign key (language_id) references language(id);
alter table workflow_task add constraint FK_workflow_assign foreign key (assigned_to) references cms_role(id);
alter table workflow_task add constraint FK_workflow_step foreign key (status) references workflow_step(id);
alter table workflow_step add constraint fk_escalation_action foreign key (escalation_action) references workflow_action(id);


alter table contentlet_version_info add constraint FK_con_ver_lockedby foreign key (locked_by) references user_(userid);
alter table container_version_info  add constraint FK_tainer_ver_info_lockedby  foreign key (locked_by) references user_(userid);
alter table template_version_info   add constraint FK_temp_ver_info_lockedby   foreign key (locked_by) references user_(userid);
alter table link_version_info       add constraint FK_link_ver_info_lockedby       foreign key (locked_by) references user_(userid);


ALTER TABLE tag ALTER COLUMN host_id set default 'SYSTEM_HOST';
alter table tag add constraint tag_tagname_host unique (tagname, host_id);
alter table tag_inode add constraint fk_tag_inode_tagid foreign key (tag_id) references tag (tag_id);

ALTER TABLE tag ALTER COLUMN user_id TYPE text;

-- ****** Indicies Data Storage *******
create table indicies (
  index_name varchar(100) primary key,
  index_type varchar(16) not null,
  index_version varchar(16) null
);
-- We can only have one index type per version
ALTER TABLE indicies
    ADD CONSTRAINT uq_index_type_version
        UNIQUE (index_type, index_version);

-- ****** Log Console Table *******
  CREATE TABLE log_mapper (
    enabled   	 numeric(1,0) NOT null,
    log_name 	 varchar(30) NOT null,
    description  varchar(50) NOT null,
    primary key (log_name)
  );

  insert into log_mapper (ENABLED,LOG_NAME,DESCRIPTION) values ('1','dotcms-userActivity.log','Log Users action on pages, structures, documents.');
  insert into log_mapper (ENABLED,LOG_NAME,DESCRIPTION) values ('1','dotcms-security.log','Log users login activity into dotCMS.');
  insert into log_mapper (ENABLED,LOG_NAME,DESCRIPTION) values ('1','dotcms-adminaudit.log','Log Admin activity on dotCMS.');
  insert into log_mapper (ENABLED,LOG_NAME,DESCRIPTION) values ('1','dotcms-pushpublish.log','Log Push Publishing activity on dotCMS.');
  insert into log_mapper (ENABLED,LOG_NAME,DESCRIPTION) values ('1','visitor-v3.log','Log Visitor Filter activity on dotCMS.');

create index idx_identifier_perm on identifier (asset_type,host_inode);

CREATE TABLE broken_link (
   id VARCHAR(36) PRIMARY KEY,
   inode VARCHAR(36) NOT NULL,
   field VARCHAR(36) NOT NULL,
   link VARCHAR(255) NOT NULL,
   title VARCHAR(255) NOT NULL,
   status_code integer NOT NULL
);

alter table broken_link add CONSTRAINT fk_brokenl_content
    FOREIGN KEY (inode) REFERENCES contentlet(inode) ON DELETE CASCADE;

alter table broken_link add CONSTRAINT fk_brokenl_field
    FOREIGN KEY (field) REFERENCES field(inode) ON DELETE CASCADE;


-- ****** Content Publishing Framework *******
CREATE TABLE publishing_queue (
    id BIGSERIAL PRIMARY KEY NOT NULL,
    operation INT8,
    asset VARCHAR(2000) NOT NULL,
    language_id INT8 NOT NULL,
    entered_date timestamptz,
    publish_date timestamptz,
    type VARCHAR(256),
    bundle_id VARCHAR(256)
);

CREATE TABLE publishing_queue_audit
(bundle_id VARCHAR(256) PRIMARY KEY NOT NULL,
status INTEGER,
status_pojo text,
status_updated timestamptz,
create_date timestamptz);

-- ****** Content Publishing Framework - End Point Management *******
CREATE TABLE publishing_end_point (
	id varchar(36) PRIMARY KEY,
	group_id varchar(700),
	server_name varchar(700) unique,
	address varchar(250),
	port varchar(10),
	protocol varchar(10),
	enabled bool,
	auth_key text,
	sending bool);

create table publishing_environment(
	id varchar(36) NOT NULL  primary key,
	name varchar(255) NOT NULL unique,
	push_to_all bool NOT NULL
);

create table sitesearch_audit (
    job_id varchar(36),
    job_name varchar(255) not null,
    fire_date timestamptz not null,
    incremental bool not null,
    start_date timestamptz,
    end_date timestamptz,
    host_list varchar(500) not null,
    all_hosts bool not null,
    lang_list varchar(500) not null,
    path varchar(500) not null,
    path_include bool not null,
    files_count integer not null,
    pages_count integer not null,
    urlmaps_count integer not null,
    index_name varchar(100) not null,
    primary key(job_id,fire_date)
);

create table publishing_bundle(
  id varchar(36) NOT NULL  primary key,
  name varchar(255) NOT NULL,
  publish_date timestamptz,
  expire_date timestamptz,
  owner varchar(100),
  force_push bool,
  filter_key varchar(100)
);

ALTER TABLE publishing_bundle ADD CONSTRAINT FK_publishing_bundle_owner FOREIGN KEY (owner) REFERENCES user_(userid);

create table publishing_bundle_environment(
	id varchar(36) NOT NULL primary key,
	bundle_id varchar(36) NOT NULL,
	environment_id varchar(36) NOT NULL
);

alter table publishing_bundle_environment add constraint FK_bundle_id foreign key (bundle_id) references publishing_bundle(id);
alter table publishing_bundle_environment add constraint FK_environment_id foreign key (environment_id) references publishing_environment(id);

create table publishing_pushed_assets(
	bundle_id varchar(36) NOT NULL,
	asset_id varchar(255) NOT NULL,
	asset_type varchar(255) NOT NULL,
	push_date timestamptz,
	environment_id varchar(36) NOT NULL,
	endpoint_ids text,
	publisher text
);

CREATE INDEX idx_pushed_assets_1 ON publishing_pushed_assets (bundle_id);
CREATE INDEX idx_pushed_assets_2 ON publishing_pushed_assets (environment_id);
CREATE INDEX idx_pushed_assets_3 ON publishing_pushed_assets (asset_id, environment_id);

CREATE INDEX idx_pub_qa_1 ON publishing_queue_audit (status);

-- Cluster Tables

CREATE TABLE dot_cluster(cluster_id varchar(36), cluster_salt VARCHAR(256), PRIMARY KEY (cluster_id) );
CREATE TABLE cluster_server(server_id varchar(36), cluster_id varchar(36) NOT NULL, name varchar(100), ip_address varchar(39) NOT NULL, host varchar(255), cache_port SMALLINT, es_transport_tcp_port SMALLINT, es_network_port SMALLINT, es_http_port SMALLINT, key_ varchar(100), PRIMARY KEY (server_id));
ALTER TABLE cluster_server add constraint fk_cluster_id foreign key (cluster_id) REFERENCES dot_cluster(cluster_id);
CREATE TABLE cluster_server_uptime(id varchar(36), server_id varchar(36) references cluster_server(server_id), startup timestamptz null, heartbeat timestamptz null, PRIMARY KEY (id));
ALTER TABLE cluster_server_uptime add constraint fk_cluster_server_id foreign key (server_id) REFERENCES cluster_server(server_id);

-- so the foreign keys needs an explicit index (!!) --
create index idx_link_vi_live on link_version_info(live_inode);
create index idx_link_vi_working on link_version_info(working_inode);
create index idx_container_vi_live on container_version_info(live_inode);
create index idx_container_vi_working on container_version_info(working_inode);
create index idx_template_vi_live on template_version_info(live_inode);
create index idx_template_vi_working on template_version_info(working_inode);
create index idx_contentlet_vi_live on contentlet_version_info(live_inode);
create index idx_contentlet_vi_working on contentlet_version_info(working_inode);
create index folder_ident on folder (identifier);
create index contentlet_ident on contentlet (identifier);
create index links_ident on links (identifier);
create index containers_ident on dot_containers (identifier);
create index template_ident on template (identifier);
create index contentlet_moduser on contentlet (mod_user);
create index contentlet_lang on contentlet (language_id);
CREATE INDEX CONCURRENTLY idx_contentlet_template_value ON contentlet((contentlet_as_json->'fields'->'template'->>'value'));
-- end of fk indicies --

-- Notifications Table
CREATE TABLE notification (
    group_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    notification_type VARCHAR(100),
    notification_level VARCHAR(100),
    time_sent timestamptz NOT NULL,
    was_read BOOL
);
ALTER TABLE notification ADD CONSTRAINT PK_notification PRIMARY KEY (group_id, user_id);
ALTER TABLE notification ALTER was_read SET DEFAULT FALSE;
CREATE INDEX idx_not_read ON notification (was_read);

-- indices for version_info tables on version_ts
create index idx_contentlet_vi_version_ts on contentlet_version_info(version_ts);
create index idx_container_vi_version_ts on container_version_info(version_ts);
create index idx_template_vi_version_ts on template_version_info(version_ts);
create index idx_link_vi_version_ts on link_version_info(version_ts);

-- container multiple structures
create index idx_container_id on container_structures(container_id);
alter table container_structures add constraint FK_cs_container_id foreign key (container_id) references identifier(id);
alter table container_structures add constraint FK_cs_inode foreign key (container_inode) references inode(inode);


-- license repo
create table sitelic(id varchar(36) primary key, serverid varchar(100), license text not null, lastping timestamptz not null, startup_time bigint);

-- Integrity Checker
create table folders_ir(folder varchar(255), local_inode varchar(36), remote_inode varchar(36), local_identifier varchar(36), remote_identifier varchar(36), endpoint_id varchar(40), PRIMARY KEY (local_inode, endpoint_id));
create table hosts_ir(local_identifier varchar(36), remote_identifier varchar(36), endpoint_id varchar(40), local_working_inode varchar(36), local_live_inode varchar(36), remote_working_inode varchar(36), remote_live_inode varchar(36), language_id int8, host varchar(255), PRIMARY KEY (local_working_inode, language_id, endpoint_id));
create table structures_ir(velocity_name varchar(255), local_inode varchar(36), remote_inode varchar(36), endpoint_id varchar(40), PRIMARY KEY (local_inode, endpoint_id));
create table schemes_ir(name varchar(255), local_inode varchar(36), remote_inode varchar(36), endpoint_id varchar(40), PRIMARY KEY (local_inode, endpoint_id));
create table htmlpages_ir(html_page varchar(255), local_working_inode varchar(36), local_live_inode varchar(36), remote_working_inode varchar(36), remote_live_inode varchar(36),local_identifier varchar(36), remote_identifier varchar(36), endpoint_id varchar(40), language_id bigint, PRIMARY KEY (local_working_inode, language_id, endpoint_id));
create table fileassets_ir(file_name varchar(255), local_working_inode varchar(36), local_live_inode varchar(36), remote_working_inode varchar(36), remote_live_inode varchar(36),local_identifier varchar(36), remote_identifier varchar(36), endpoint_id varchar(40), language_id bigint, PRIMARY KEY (local_working_inode, language_id, endpoint_id));
create table cms_roles_ir(name varchar(1000), role_key varchar(255), local_role_id varchar(36), remote_role_id varchar(36), local_role_fqn varchar(1000), remote_role_fqn varchar(1000), endpoint_id varchar(40), PRIMARY KEY (local_role_id, endpoint_id));


---Server Action
create table cluster_server_action(
	server_action_id varchar(36) not null, 
	originator_id varchar(36) not null, 
	server_id varchar(36) not null, 
	failed bool, 
	response varchar(2048), 
	action_id varchar(1024) not null,
	completed bool, 
	entered_date timestamptz not null,
	time_out_seconds bigint not null,
	PRIMARY KEY (server_action_id)
);

-- Rules Engine
create table dot_rule(id varchar(36) primary key,name varchar(255) not null,fire_on varchar(20),short_circuit boolean default false,parent_id varchar(36) not null,folder varchar(36) not null,priority int default 0,enabled boolean default false,mod_date timestamptz);
create table rule_condition_group(id varchar(36) primary key,rule_id varchar(36) references dot_rule(id),operator varchar(10) not null,priority int default 0,mod_date timestamptz);
create table rule_condition(id varchar(36) primary key,conditionlet text not null,condition_group varchar(36) references rule_condition_group(id),comparison varchar(36) not null,operator varchar(10) not null,priority int default 0,mod_date timestamptz);
create table rule_condition_value (id varchar(36) primary key,condition_id varchar(36) references rule_condition(id), paramkey varchar(255) not null, value text,priority int default 0);
create table rule_action (id varchar(36) primary key,rule_id varchar(36) references dot_rule(id),priority int default 0,actionlet text not null,mod_date timestamptz);
create table rule_action_pars(id varchar(36) primary key,rule_action_id varchar(36) references rule_action(id), paramkey varchar(255) not null,value text);
create index idx_rules_fire_on on dot_rule (fire_on);

CREATE TABLE system_event (
    identifier VARCHAR(36) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    payload TEXT NOT NULL,
    created BIGINT NOT NULL,
    server_id varchar(36) NOT NULL
);
ALTER TABLE system_event ADD CONSTRAINT PK_system_event PRIMARY KEY (identifier);
CREATE INDEX idx_system_event ON system_event (created);

--Content Types improvement
CREATE INDEX idx_lower_structure_name ON structure (LOWER(velocity_var_name));


CREATE TABLE api_token_issued(
    token_id varchar(255) NOT NULL, 
    token_userid varchar(255) NOT NULL, 
    issue_date timestamptz NOT NULL, 
    expire_date timestamptz NOT NULL, 
    requested_by_userid  varchar(255) NOT NULL, 
    requested_by_ip  varchar(255) NOT NULL, 
    revoke_date timestamptz, 
    allowed_from  varchar(255) , 
    issuer  varchar(255) , 
    claims  text , 
    mod_date  timestamptz NOT NULL, 
    PRIMARY KEY (token_id)
 );

create index idx_api_token_issued_user ON api_token_issued (token_userid);

create table storage_group (
    group_name varchar(255)  not null,
    mod_date   timestamptz NOT NULL DEFAULT CURRENT_DATE,
    PRIMARY KEY (group_name)
);

create table storage (
    path       varchar(255) not null,
    group_name varchar(255) not null,
    hash       varchar(64) not null,
    mod_date   timestamptz NOT NULL DEFAULT CURRENT_DATE,
    hash_ref   varchar(64),
    PRIMARY KEY (path, group_name),
    FOREIGN KEY (group_name) REFERENCES storage_group (group_name)
);

CREATE INDEX idx_storage_hash ON storage (hash);

create table storage_data (
    hash_id  varchar(64) not null,
    data     bytea not null,
    mod_date timestamptz NOT NULL DEFAULT CURRENT_DATE,
    PRIMARY KEY (hash_id)
);

create table storage_x_data (
    storage_hash varchar(64)                 not null,
    data_hash    varchar(64)                 not null,
    data_order   integer                     not null,
    mod_date     timestamptz NOT NULL DEFAULT CURRENT_DATE,
    PRIMARY KEY (storage_hash, data_hash),
    FOREIGN KEY (data_hash) REFERENCES storage_data (hash_id)
);

-- https://github.com/lukas-krecan/ShedLock
CREATE TABLE shedlock(name VARCHAR(64) NOT NULL, lock_until timestamptz NOT NULL,
                      locked_at timestamptz NOT NULL, locked_by VARCHAR(255) NOT NULL, PRIMARY KEY (name));


create table variant (
     name varchar(255) primary key,
     description varchar(255) not null,
     archived boolean NOT NULL default false
);

create table experiment (
     id  varchar(255) primary key,
     page_id varchar(255) not null,
     name varchar(255) not null,
     description varchar(255) not null,
     status varchar(255) not null,
     traffic_proportion jsonb not null,
     traffic_allocation float4 not null,
     mod_date timestamptz not null,
     scheduling jsonb,
     creation_date timestamptz not null,
     created_by varchar(255) not null,
     last_modified_by varchar(255) not null,
     goals jsonb,
     lookback_window integer not null,
     running_ids jsonb
);

CREATE INDEX idx_exp_pageid ON experiment (page_id);

-- system table for general purposes and configuration
create table  if not exists system_table (
     key varchar(511) primary key,
     value text not null
);


-- Set up "like 'param%'" indexes for inode and identifier
CREATE INDEX if not exists inode_inode_leading_idx ON inode(inode  COLLATE "C");
CREATE INDEX if not exists identifier_id_leading_idx ON identifier(id  COLLATE "C");

-- Table for active jobs in the queue
CREATE TABLE job_queue
(
    id         VARCHAR(255) PRIMARY KEY,
    queue_name VARCHAR(255) NOT NULL,
    state      VARCHAR(50)  NOT NULL,
    priority   INTEGER DEFAULT 0,
    created_at timestamptz  NOT NULL
);

-- Table for job details and historical record
CREATE TABLE job
(
    id             VARCHAR(255) PRIMARY KEY,
    queue_name     VARCHAR(255) NOT NULL,
    state          VARCHAR(50)  NOT NULL,
    parameters     JSONB        NOT NULL,
    result         JSONB,
    progress       FLOAT   DEFAULT 0,
    created_at     timestamptz  NOT NULL,
    updated_at     timestamptz  NOT NULL,
    started_at     timestamptz,
    completed_at   timestamptz,
    execution_node VARCHAR(255),
    retry_count    INTEGER DEFAULT 0
);

-- Table for detailed job history
CREATE TABLE job_history
(
    id             VARCHAR(255) PRIMARY KEY,
    job_id         VARCHAR(255) NOT NULL,
    state          VARCHAR(50)  NOT NULL,
    execution_node VARCHAR(255),
    created_at     timestamptz  NOT NULL,
    result         JSONB,
    FOREIGN KEY (job_id) REFERENCES job (id)
);

-- Indexes (add an index for the new parameters field in job_queue)
CREATE INDEX idx_job_queue_status ON job_queue (state);
CREATE INDEX idx_job_queue_priority_created_at ON job_queue (priority DESC, created_at ASC);
CREATE INDEX idx_job_parameters ON job USING GIN (parameters);
CREATE INDEX idx_job_result ON job USING GIN (result);
CREATE INDEX idx_job_status ON job (state);
CREATE INDEX idx_job_created_at ON job (created_at);
CREATE INDEX idx_job_history_job_id ON job_history (job_id);
CREATE INDEX idx_job_history_job_id_state ON job_history (job_id, state);

CREATE TABLE IF NOT EXISTS analytic_custom_attributes (
    event_type  varchar(255) primary key,
    custom_attribute jsonb not null
);