create table Address (
	addressId varchar2(100) not null primary key,
	companyId varchar2(100) not null,
	userId varchar2(100) not null,
	userName varchar2(100) null,
	createDate date null,
	modifiedDate date null,
	className varchar2(100) null,
	classPK varchar2(100) null,
	description varchar2(100) null,
	street1 varchar2(100) null,
	street2 varchar2(100) null,
	city varchar2(100) null,
	state varchar2(100) null,
	zip varchar2(100) null,
	country varchar2(100) null,
	phone varchar2(100) null,
	fax varchar2(100) null,
	cell varchar2(100) null,
	priority number(30,0)
);

create table AdminConfig (
	configId varchar2(100) not null primary key,
	companyId varchar2(100) not null,
	type_ varchar2(100) null,
	name varchar2(100) null,
	config long varchar null
);

create table Company (
	companyId varchar2(100) not null primary key,
	key_ long varchar null,
	portalURL varchar2(100) not null,
	homeURL varchar2(100) not null,
	mx varchar2(100) not null,
	name varchar2(100) not null,
	shortName varchar2(100) not null,
	type_ varchar2(100) null,
	size_ varchar2(100) null,
	street varchar2(100) null,
	city varchar2(100) null,
	state varchar2(100) null,
	zip varchar2(100) null,
	phone varchar2(100) null,
	fax varchar2(100) null,
	emailAddress varchar2(100) null,
	authType varchar2(100) null,
	autoLogin number(1, 0),
	strangers number(1, 0)
);

create table Counter (
	name varchar2(100) not null primary key,
	currentId number(30,0)
);

create table Image (
	imageId varchar2(200) not null primary key,
	text_ long varchar not null
);

create table PasswordTracker (
	passwordTrackerId varchar2(100) not null primary key,
	userId varchar2(100) not null,
	createDate date not null,
	password_ varchar2(100) not null
);

create table PollsChoice (
	choiceId varchar2(100) not null,
	questionId varchar2(100) not null,
	description varchar2(4000) null,
	primary key (choiceId, questionId)
);

create table PollsDisplay (
	layoutId varchar2(100) not null,
	userId varchar2(100) not null,
	portletId varchar2(100) not null,
	questionId varchar2(100) not null,
	primary key (layoutId, userId, portletId)
);

create table PollsQuestion (
	questionId varchar2(100) not null primary key,
	portletId varchar2(100) not null,
	groupId varchar2(100) not null,
	companyId varchar2(100) not null,
	userId varchar2(100) not null,
	userName varchar2(100) null,
	createDate date null,
	modifiedDate date null,
	title varchar2(100) null,
	description varchar2(4000) null,
	expirationDate date null,
	lastVoteDate date null
);

create table PollsVote (
	questionId varchar2(100) not null,
	userId varchar2(100) not null,
	choiceId varchar2(100) not null,
	voteDate date null,
	primary key (questionId, userId)
);

create table Portlet (
	portletId varchar2(100) not null,
	groupId varchar2(100) not null,
	companyId varchar2(100) not null,
	defaultPreferences nclob null,
	narrow number(1, 0),
	roles varchar2(4000) null,
	active_ number(1, 0),
	primary key (portletId, groupId, companyId)
);

create table PortletPreferences (
	portletId varchar2(100) not null,
	userId varchar2(100) not null,
	layoutId varchar2(100) not null,
	preferences long varchar null,
	primary key (portletId, userId, layoutId)
);

create table Release_ (
	releaseId varchar2(100) not null primary key,
	createDate date null,
	modifiedDate date null,
	buildNumber number(30,0) null,
	buildDate date null
);

create table User_ (
	userId varchar2(100) not null primary key,
	companyId varchar2(100) not null,
	createDate date null,
	password_ nclob null,
	passwordEncrypted number(1, 0),
	passwordExpirationDate date null,
	passwordReset number(1, 0),
	firstName varchar2(100) null,
	middleName varchar2(100) null,
	lastName varchar2(100) null,
	nickName varchar2(100) null,
	male number(1, 0),
	birthday date null,
	emailAddress varchar2(100) null,
	smsId varchar2(100) null,
	aimId varchar2(100) null,
	icqId varchar2(100) null,
	msnId varchar2(100) null,
	ymId varchar2(100) null,
	favoriteActivity varchar2(100) null,
	favoriteBibleVerse varchar2(100) null,
	favoriteFood varchar2(100) null,
	favoriteMovie varchar2(100) null,
	favoriteMusic varchar2(100) null,
	languageId varchar2(100) null,
	timeZoneId varchar2(100) null,
	skinId varchar2(100) null,
	dottedSkins number(1, 0),
	roundedSkins number(1, 0),
	greeting varchar2(100) null,
	resolution varchar2(100) null,
	refreshRate varchar2(100) null,
	layoutIds varchar2(100) null,
	comments varchar2(4000) null,
	loginDate date null,
	loginIP varchar2(100) null,
	lastLoginDate date null,
	lastLoginIP varchar2(100) null,
	failedLoginAttempts number(30,0),
	agreedToTermsOfUse number(1, 0),
	active_ number(1, 0)
);

create table UserTracker (
	userTrackerId varchar2(100) not null primary key,
	companyId varchar2(100) not null,
	userId varchar2(100) not null,
	modifiedDate date null,
	remoteAddr varchar2(100) null,
	remoteHost varchar2(100) null,
	userAgent varchar2(100) null
);

create table UserTrackerPath (
	userTrackerPathId varchar2(100) not null primary key,
	userTrackerId varchar2(100) not null,
	path varchar2(4000) not null,
	pathDate date not null
);

--
-- Global
--

insert into Counter values ('com.liferay.portal.model.Address', 10);
insert into Counter values ('com.liferay.portal.model.Role', 100);
insert into Counter values ('com.liferay.portal.model.User.liferay.com', 10);
insert into Counter values ('com.liferay.portlet.polls.model.PollsQuestion', 10);

--
-- Liferay, LLC
--

insert into Company (companyId, portalURL, homeURL, mx, name, shortName, type_, size_, emailAddress, authType, autoLogin, strangers) values ('liferay.com', 'localhost', 'localhost', 'liferay.com', 'Liferay, LLC', 'Liferay', 'biz', '', 'test@liferay.com', 'emailAddress', '1', '1');
update Company set street = '1220 Brea Canyon Rd.', city = 'Diamond Bar', state = 'CA', zip = '91789' where companyId = 'liferay.com';

insert into PollsDisplay (layoutId, userId, portletId, questionId) values ('1.1', 'group.1', '59', '1');
insert into PollsChoice (choiceId, questionId, description) values ('a', '1', 'Chocolate');
insert into PollsChoice (choiceId, questionId, description) values ('b', '1', 'Strawberry');
insert into PollsChoice (choiceId, questionId, description) values ('c', '1', 'Vanilla');
insert into PollsQuestion (questionId, portletId, groupId, companyId, userId, userName, createDate, modifiedDate, title, description) values ('1', '25', '-1', 'liferay.com', 'liferay.com.1', 'John Wayne', sysdate, sysdate, 'What is your favorite ice cream flavor?', 'What is your favorite ice cream flavor?');

--
-- Default User
--

insert into User_ (userId, companyId, createDate, password_, passwordEncrypted, passwordReset, firstName, middleName, lastName, male, birthday, emailAddress, skinId, dottedSkins, roundedSkins, greeting, layoutIds, loginDate, failedLoginAttempts, agreedToTermsOfUse, active_) values ('liferay.com.default', 'default', sysdate, 'password', '0', '0', '', '', '', '1', to_date('1970-01-01 00:00:00','YYYY-MM-DD HH24:MI:SS'), 'default@liferay.com', '01', '0', '0', 'Welcome!', '', sysdate, 0, '0', '1');

--
-- Test User
--

insert into User_ (userId, companyId, createDate, password_, passwordEncrypted, passwordReset, firstName, middleName, lastName, nickName, male, birthday, emailAddress, skinId, dottedSkins, roundedSkins, greeting, layoutIds, loginDate, failedLoginAttempts, agreedToTermsOfUse, active_) values ('liferay.com.1', 'liferay.com', sysdate, 'test', '0', '0', 'John', '', 'Wayne', 'Duke', '1', to_date('1970-01-01 00:00:00','YYYY-MM-DD HH24:MI:SS'), 'test@liferay.com', '01', '0', '1', 'Welcome John Wayne!', '1,', sysdate, 0, '1', '1');
CREATE TABLE qrtz_job_details
  (
    JOB_NAME  VARCHAR2(80) NOT NULL,
    JOB_GROUP VARCHAR2(80) NOT NULL,
    DESCRIPTION VARCHAR2(120) NULL,
    JOB_CLASS_NAME   VARCHAR2(128) NOT NULL,
    IS_DURABLE VARCHAR2(1) NOT NULL,
    IS_VOLATILE VARCHAR2(1) NOT NULL,
    IS_STATEFUL VARCHAR2(1) NOT NULL,
    REQUESTS_RECOVERY VARCHAR2(1) NOT NULL,
    JOB_DATA BLOB NULL,
    PRIMARY KEY (JOB_NAME,JOB_GROUP)
);
CREATE TABLE qrtz_job_listeners
  (
    JOB_NAME  VARCHAR2(80) NOT NULL,
    JOB_GROUP VARCHAR2(80) NOT NULL,
    JOB_LISTENER VARCHAR2(80) NOT NULL,
    PRIMARY KEY (JOB_NAME,JOB_GROUP,JOB_LISTENER),
    FOREIGN KEY (JOB_NAME,JOB_GROUP)
	REFERENCES QRTZ_JOB_DETAILS(JOB_NAME,JOB_GROUP)
);
CREATE TABLE qrtz_triggers
  (
    TRIGGER_NAME VARCHAR2(80) NOT NULL,
    TRIGGER_GROUP VARCHAR2(80) NOT NULL,
    JOB_NAME  VARCHAR2(80) NOT NULL,
    JOB_GROUP VARCHAR2(80) NOT NULL,
    IS_VOLATILE VARCHAR2(1) NOT NULL,
    DESCRIPTION VARCHAR2(120) NULL,
    NEXT_FIRE_TIME NUMBER(13) NULL,
    PREV_FIRE_TIME NUMBER(13) NULL,
    PRIORITY NUMBER(13) NULL,
    TRIGGER_STATE VARCHAR2(16) NOT NULL,
    TRIGGER_TYPE VARCHAR2(8) NOT NULL,
    START_TIME NUMBER(13) NOT NULL,
    END_TIME NUMBER(13) NULL,
    CALENDAR_NAME VARCHAR2(80) NULL,
    MISFIRE_INSTR NUMBER(2) NULL,
    JOB_DATA BLOB NULL,
    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (JOB_NAME,JOB_GROUP)
	REFERENCES QRTZ_JOB_DETAILS(JOB_NAME,JOB_GROUP)
);
CREATE TABLE qrtz_simple_triggers
  (
    TRIGGER_NAME VARCHAR2(80) NOT NULL,
    TRIGGER_GROUP VARCHAR2(80) NOT NULL,
    REPEAT_COUNT NUMBER(7) NOT NULL,
    REPEAT_INTERVAL NUMBER(12) NOT NULL,
    TIMES_TRIGGERED NUMBER(7) NOT NULL,
    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (TRIGGER_NAME,TRIGGER_GROUP)
	REFERENCES QRTZ_TRIGGERS(TRIGGER_NAME,TRIGGER_GROUP)
);
CREATE TABLE qrtz_cron_triggers
  (
    TRIGGER_NAME VARCHAR2(80) NOT NULL,
    TRIGGER_GROUP VARCHAR2(80) NOT NULL,
    CRON_EXPRESSION VARCHAR2(80) NOT NULL,
    TIME_ZONE_ID VARCHAR2(80),
    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (TRIGGER_NAME,TRIGGER_GROUP)
	REFERENCES QRTZ_TRIGGERS(TRIGGER_NAME,TRIGGER_GROUP)
);
CREATE TABLE qrtz_blob_triggers
  (
    TRIGGER_NAME VARCHAR2(80) NOT NULL,
    TRIGGER_GROUP VARCHAR2(80) NOT NULL,
    BLOB_DATA BLOB NULL,
    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (TRIGGER_NAME,TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS(TRIGGER_NAME,TRIGGER_GROUP)
);
CREATE TABLE qrtz_trigger_listeners
  (
    TRIGGER_NAME  VARCHAR2(80) NOT NULL,
    TRIGGER_GROUP VARCHAR2(80) NOT NULL,
    TRIGGER_LISTENER VARCHAR2(80) NOT NULL,
    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP,TRIGGER_LISTENER),
    FOREIGN KEY (TRIGGER_NAME,TRIGGER_GROUP)
	REFERENCES QRTZ_TRIGGERS(TRIGGER_NAME,TRIGGER_GROUP)
);
CREATE TABLE qrtz_calendars
  (
    CALENDAR_NAME  VARCHAR2(80) NOT NULL,
    CALENDAR BLOB NOT NULL,
    PRIMARY KEY (CALENDAR_NAME)
);
CREATE TABLE qrtz_paused_trigger_grps
  (
    TRIGGER_GROUP  VARCHAR2(80) NOT NULL,
    PRIMARY KEY (TRIGGER_GROUP)
);
CREATE TABLE qrtz_fired_triggers
  (
    ENTRY_ID VARCHAR2(95) NOT NULL,
    TRIGGER_NAME VARCHAR2(80) NOT NULL,
    TRIGGER_GROUP VARCHAR2(80) NOT NULL,
    IS_VOLATILE VARCHAR2(1) NOT NULL,
    INSTANCE_NAME VARCHAR2(80) NOT NULL,
    FIRED_TIME NUMBER(13) NOT NULL,
    PRIORITY NUMBER(13) NOT NULL,
    STATE VARCHAR2(16) NOT NULL,
    JOB_NAME VARCHAR2(80) NULL,
    JOB_GROUP VARCHAR2(80) NULL,
    IS_STATEFUL VARCHAR2(1) NULL,
    REQUESTS_RECOVERY VARCHAR2(1) NULL,
    PRIMARY KEY (ENTRY_ID)
);
CREATE TABLE qrtz_scheduler_state
  (
    INSTANCE_NAME VARCHAR2(80) NOT NULL,
    LAST_CHECKIN_TIME NUMBER(13) NOT NULL,
    CHECKIN_INTERVAL NUMBER(13) NOT NULL,
    PRIMARY KEY (INSTANCE_NAME)
);
CREATE TABLE qrtz_locks
  (
    LOCK_NAME  VARCHAR2(40) NOT NULL,
    PRIMARY KEY (LOCK_NAME)
);
INSERT INTO qrtz_locks values('TRIGGER_ACCESS');
INSERT INTO qrtz_locks values('JOB_ACCESS');
INSERT INTO qrtz_locks values('CALENDAR_ACCESS');
INSERT INTO qrtz_locks values('STATE_ACCESS');
INSERT INTO qrtz_locks values('MISFIRE_ACCESS');
create index idx_qrtz_j_req_recovery on qrtz_job_details(REQUESTS_RECOVERY);
create index idx_qrtz_t_next_fire_time on qrtz_triggers(NEXT_FIRE_TIME);
create index idx_qrtz_t_state on qrtz_triggers(TRIGGER_STATE);
create index idx_qrtz_t_nft_st on qrtz_triggers(NEXT_FIRE_TIME,TRIGGER_STATE);
create index idx_qrtz_t_volatile on qrtz_triggers(IS_VOLATILE);
create index idx_qrtz_ft_trig_name on qrtz_fired_triggers(TRIGGER_NAME);
create index idx_qrtz_ft_trig_group on qrtz_fired_triggers(TRIGGER_GROUP);
create index idx_qrtz_ft_trig_nm_gp on qrtz_fired_triggers(TRIGGER_NAME,TRIGGER_GROUP);
create index idx_qrtz_ft_trig_volatile on qrtz_fired_triggers(IS_VOLATILE);
create index idx_qrtz_ft_trig_inst_name on qrtz_fired_triggers(INSTANCE_NAME);
create index idx_qrtz_ft_job_name on qrtz_fired_triggers(JOB_NAME);
create index idx_qrtz_ft_job_group on qrtz_fired_triggers(JOB_GROUP);
create index idx_qrtz_ft_job_stateful on qrtz_fired_triggers(IS_STATEFUL);
create index idx_qrtz_ft_job_req_recovery on qrtz_fired_triggers(REQUESTS_RECOVERY);


CREATE TABLE qrtz_excl_job_details
  (
    JOB_NAME  VARCHAR2(80) NOT NULL,
    JOB_GROUP VARCHAR2(80) NOT NULL,
    DESCRIPTION VARCHAR2(120) NULL,
    JOB_CLASS_NAME   VARCHAR2(128) NOT NULL,
    IS_DURABLE VARCHAR2(1) NOT NULL,
    IS_VOLATILE VARCHAR2(1) NOT NULL,
    IS_STATEFUL VARCHAR2(1) NOT NULL,
    REQUESTS_RECOVERY VARCHAR2(1) NOT NULL,
    JOB_DATA BLOB NULL,
    PRIMARY KEY (JOB_NAME,JOB_GROUP)
);
CREATE TABLE qrtz_excl_job_listeners
  (
    JOB_NAME  VARCHAR2(80) NOT NULL,
    JOB_GROUP VARCHAR2(80) NOT NULL,
    JOB_LISTENER VARCHAR2(80) NOT NULL,
    PRIMARY KEY (JOB_NAME,JOB_GROUP,JOB_LISTENER),
    FOREIGN KEY (JOB_NAME,JOB_GROUP)
	REFERENCES QRTZ_EXCL_JOB_DETAILS(JOB_NAME,JOB_GROUP)
);
CREATE TABLE qrtz_excl_triggers
  (
    TRIGGER_NAME VARCHAR2(80) NOT NULL,
    TRIGGER_GROUP VARCHAR2(80) NOT NULL,
    JOB_NAME  VARCHAR2(80) NOT NULL,
    JOB_GROUP VARCHAR2(80) NOT NULL,
    IS_VOLATILE VARCHAR2(1) NOT NULL,
    DESCRIPTION VARCHAR2(120) NULL,
    NEXT_FIRE_TIME NUMBER(13) NULL,
    PREV_FIRE_TIME NUMBER(13) NULL,
    PRIORITY NUMBER(13) NULL,
    TRIGGER_STATE VARCHAR2(16) NOT NULL,
    TRIGGER_TYPE VARCHAR2(8) NOT NULL,
    START_TIME NUMBER(13) NOT NULL,
    END_TIME NUMBER(13) NULL,
    CALENDAR_NAME VARCHAR2(80) NULL,
    MISFIRE_INSTR NUMBER(2) NULL,
    JOB_DATA BLOB NULL,
    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (JOB_NAME,JOB_GROUP)
	REFERENCES QRTZ_EXCL_JOB_DETAILS(JOB_NAME,JOB_GROUP)
);
CREATE TABLE qrtz_excl_simple_triggers
  (
    TRIGGER_NAME VARCHAR2(80) NOT NULL,
    TRIGGER_GROUP VARCHAR2(80) NOT NULL,
    REPEAT_COUNT NUMBER(7) NOT NULL,
    REPEAT_INTERVAL NUMBER(12) NOT NULL,
    TIMES_TRIGGERED NUMBER(7) NOT NULL,
    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (TRIGGER_NAME,TRIGGER_GROUP)
	REFERENCES QRTZ_EXCL_TRIGGERS(TRIGGER_NAME,TRIGGER_GROUP)
);
CREATE TABLE qrtz_excl_cron_triggers
  (
    TRIGGER_NAME VARCHAR2(80) NOT NULL,
    TRIGGER_GROUP VARCHAR2(80) NOT NULL,
    CRON_EXPRESSION VARCHAR2(80) NOT NULL,
    TIME_ZONE_ID VARCHAR2(80),
    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (TRIGGER_NAME,TRIGGER_GROUP)
	REFERENCES QRTZ_EXCL_TRIGGERS(TRIGGER_NAME,TRIGGER_GROUP)
);
CREATE TABLE qrtz_excl_blob_triggers
  (
    TRIGGER_NAME VARCHAR2(80) NOT NULL,
    TRIGGER_GROUP VARCHAR2(80) NOT NULL,
    BLOB_DATA BLOB NULL,
    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (TRIGGER_NAME,TRIGGER_GROUP)
        REFERENCES QRTZ_EXCL_TRIGGERS(TRIGGER_NAME,TRIGGER_GROUP)
);
CREATE TABLE qrtz_excl_trigger_listeners
  (
    TRIGGER_NAME  VARCHAR2(80) NOT NULL,
    TRIGGER_GROUP VARCHAR2(80) NOT NULL,
    TRIGGER_LISTENER VARCHAR2(80) NOT NULL,
    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP,TRIGGER_LISTENER),
    FOREIGN KEY (TRIGGER_NAME,TRIGGER_GROUP)
	REFERENCES QRTZ_EXCL_TRIGGERS(TRIGGER_NAME,TRIGGER_GROUP)
);
CREATE TABLE qrtz_excl_calendars
  (
    CALENDAR_NAME  VARCHAR2(80) NOT NULL,
    CALENDAR BLOB NOT NULL,
    PRIMARY KEY (CALENDAR_NAME)
);
CREATE TABLE qrtz_excl_paused_trigger_grps
  (
    TRIGGER_GROUP  VARCHAR2(80) NOT NULL,
    PRIMARY KEY (TRIGGER_GROUP)
);
CREATE TABLE qrtz_excl_fired_triggers
  (
    ENTRY_ID VARCHAR2(95) NOT NULL,
    TRIGGER_NAME VARCHAR2(80) NOT NULL,
    TRIGGER_GROUP VARCHAR2(80) NOT NULL,
    IS_VOLATILE VARCHAR2(1) NOT NULL,
    INSTANCE_NAME VARCHAR2(80) NOT NULL,
    FIRED_TIME NUMBER(13) NOT NULL,
    PRIORITY NUMBER(13) NOT NULL,
    STATE VARCHAR2(16) NOT NULL,
    JOB_NAME VARCHAR2(80) NULL,
    JOB_GROUP VARCHAR2(80) NULL,
    IS_STATEFUL VARCHAR2(1) NULL,
    REQUESTS_RECOVERY VARCHAR2(1) NULL,
    PRIMARY KEY (ENTRY_ID)
);
CREATE TABLE qrtz_excl_scheduler_state
  (
    INSTANCE_NAME VARCHAR2(80) NOT NULL,
    LAST_CHECKIN_TIME NUMBER(13) NOT NULL,
    CHECKIN_INTERVAL NUMBER(13) NOT NULL,
    PRIMARY KEY (INSTANCE_NAME)
);
CREATE TABLE qrtz_excl_locks
  (
    LOCK_NAME  VARCHAR2(40) NOT NULL,
    PRIMARY KEY (LOCK_NAME)
);
INSERT INTO qrtz_excl_locks values('TRIGGER_ACCESS');
INSERT INTO qrtz_excl_locks values('JOB_ACCESS');
INSERT INTO qrtz_excl_locks values('CALENDAR_ACCESS');
INSERT INTO qrtz_excl_locks values('STATE_ACCESS');
INSERT INTO qrtz_excl_locks values('MISFIRE_ACCESS');
create index idx_qrtz_excl_j_req_recovery on qrtz_excl_job_details(REQUESTS_RECOVERY);
create index idx_qrtz_excl_t_next_fire_time on qrtz_excl_triggers(NEXT_FIRE_TIME);
create index idx_qrtz_excl_t_state on qrtz_excl_triggers(TRIGGER_STATE);
create index idx_qrtz_excl_t_nft_st on qrtz_excl_triggers(NEXT_FIRE_TIME,TRIGGER_STATE);
create index idx_qrtz_excl_t_volatile on qrtz_excl_triggers(IS_VOLATILE);
create index idx_qrtz_excl_ft_trig_name on qrtz_excl_fired_triggers(TRIGGER_NAME);
create index idx_qrtz_excl_ft_trig_group on qrtz_excl_fired_triggers(TRIGGER_GROUP);
create index idx_qrtz_excl_ft_trig_nm_gp on qrtz_excl_fired_triggers(TRIGGER_NAME,TRIGGER_GROUP);
create index idx_qrtz_excl_ft_trig_volatile on qrtz_excl_fired_triggers(IS_VOLATILE);
create index idx_qrtz_excl_fttriginstname on qrtz_excl_fired_triggers(INSTANCE_NAME);
create index idx_qrtz_excl_ft_jobname on qrtz_excl_fired_triggers(JOB_NAME);
create index idx_qrtz_excl_ft_jobgroup on qrtz_excl_fired_triggers(JOB_GROUP);
create index idx_qrtz_excl_ft_jobstateful on qrtz_excl_fired_triggers(IS_STATEFUL);
create index idx_qrtzexclftjobreqrec on qrtz_excl_fired_triggers(REQUESTS_RECOVERY);
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
   tagname varchar2(255) not null,
   host_id varchar2(255),
   user_id varchar2(255),
   persona number(1,0) default 0,
   mod_date date,
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
   version_ts date not null,
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
   version_ts date not null,
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
create table plugin (
   id varchar2(255) not null,
   plugin_name varchar2(255) not null,
   plugin_version varchar2(255) not null,
   author varchar2(255) not null,
   first_deployed_date date not null,
   last_deployed_date date not null,
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
   drawed number(1,0),
   drawed_body nclob,
   add_container_links number(10,0),
   containers_added number(10,0),
   head_code nclob,
   theme varchar2(255),
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
   expire_date_var varchar2(255),
   publish_date_var varchar2(255),
   mod_date date,
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
create table container_structures (
   id varchar2(36) not null,
   container_id varchar2(36) not null,
   container_inode varchar2(36) not null,
   structure_id varchar2(36) not null,
   code nclob,
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
create table report_asset (
   inode varchar2(36) not null,
   report_name varchar2(255) not null,
   report_description varchar2(1000) not null,
   requires_input number(1,0),
   ds varchar2(100) not null,
   web_form_report number(1,0),
   primary key (inode)
);
create table workflow_comment (
   id varchar2(36) not null,
   creation_date date,
   posted_by varchar2(255),
   wf_comment nclob,
   workflowtask_id varchar2(36),
   primary key (id)
);
create table category (
   inode varchar2(36) not null,
   category_name varchar2(255),
   category_key varchar2(255),
   sort_order number(10,0),
   active number(1,0),
   keywords nclob,
   category_velocity_var_name varchar2(255),
   mod_date date,
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
   version_ts date not null,
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
   syspublish_date date,
   sysexpire_date date,
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
	 field_var_name varchar2(255),
   mod_date date,
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
   version_ts date not null,
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
create table dot_containers (
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
   version_ts date not null,
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
   mod_date date,
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
   mod_date date,
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
   version_ts date not null,
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
create index idx_identifier_pub on identifier (syspublish_date);
create index idx_identifier_exp on identifier (sysexpire_date);
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
alter table dot_containers add constraint fk8a844125fb51eb foreign key (inode) references inode;
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
--oracle
CREATE INDEX idx_tree_1 ON tree (parent);
CREATE INDEX idx_tree_2 ON tree (child);
CREATE INDEX idx_tree_3 ON tree (relation_type);
CREATE INDEX idx_tree_4 ON tree (parent, child, relation_type);
CREATE INDEX idx_tree_5 ON tree (parent, relation_type);
CREATE INDEX idx_tree_6 ON tree (child, relation_type);

CREATE INDEX idx_permisision_4 ON permission (permission_type);

CREATE INDEX idx_permission_reference_2 ON permission_reference (reference_id);
CREATE INDEX idx_permission_reference_3 ON permission_reference (reference_id,permission_type);
CREATE INDEX idx_permission_reference_4 ON permission_reference (asset_id,permission_type);
CREATE INDEX idx_permission_reference_5 ON permission_reference (asset_id,reference_id,permission_type);
CREATE INDEX idx_permission_reference_6 ON permission_reference (permission_type);

CREATE UNIQUE INDEX idx_field_velocity_structure ON field (velocity_var_name,structure_inode);

alter table chain_state add constraint fk_state_chain foreign key (chain_id) references chain(id);
alter table chain_state add constraint fk_state_code foreign key (link_code_id) references chain_link_code(id);
alter table chain_state_parameter add constraint fk_parameter_state foreign key (chain_state_id) references chain_state(id);

alter table permission add constraint permission_role_fk foreign key (roleid) references cms_role(id);

alter table contentlet add constraint FK_structure_inode foreign key (structure_inode) references structure(inode);

ALTER TABLE structure MODIFY fixed DEFAULT 0;

ALTER TABLE field MODIFY fixed DEFAULT 0;
ALTER TABLE field MODIFY read_only DEFAULT 0;

ALTER TABLE campaign MODIFY active DEFAULT 0;

insert into User_ (userId, companyId, createDate, password_, passwordEncrypted, passwordReset, firstName, middleName, lastName, male, birthday, emailAddress, skinId, dottedSkins, roundedSkins, greeting, layoutIds, loginDate, failedLoginAttempts, agreedToTermsOfUse, active_) values ('dotcms.org.default', 'default', sysdate, 'password', '0', '0', '', '', '', '1', to_date('1970-01-01 00:00:00','YYYY-MM-DD HH24:MI:SS'), 'default@dotcms.org', '01', '0', '0', 'Welcome!', '', sysdate, 0, '0', '1');

create index addres_userid_index on address(userid);
create index tag_user_id_index on tag(user_id);
create index tag_is_persona_index on tag(persona);
create index tag_inode_tagid on tag_inode(tag_id);
create index tag_inode_inode on tag_inode(inode);
CREATE TABLE "DIST_JOURNAL" ( "ID" INTEGER NOT NULL ,
"OBJECT_TO_INDEX" VARCHAR2(255), "SERVERID" VARCHAR2(64),
"JOURNAL_TYPE" INTEGER, "TIME_ENTERED" TIMESTAMP, PRIMARY KEY ("ID")
VALIDATE , UNIQUE ("OBJECT_TO_INDEX", "SERVERID", "JOURNAL_TYPE")
VALIDATE );
CREATE SEQUENCE dist_journal_id_seq
START WITH 1
INCREMENT BY 1;
create trigger DIST_JOURNAL_trg
before insert on DIST_JOURNAL
for each row
when (new.id is null)
begin
    select dist_journal_id_seq.nextval into :new.id from dual;
end;
/

CREATE TABLE dist_process ( ID INTEGER NOT NULL , OBJECT_TO_INDEX VARCHAR2(255), SERVERID VARCHAR2(64), JOURNAL_TYPE INTEGER, TIME_ENTERED TIMESTAMP, PRIMARY KEY (ID) VALIDATE );
CREATE SEQUENCE dist_process_id_seq START WITH 1 INCREMENT BY 1;
create trigger dist_process_trg
before insert on dist_process
for each row
when (new.id is null)
begin
select dist_process_id_seq.nextval into :new.id from dual;
end;
/

CREATE INDEX dist_process_index on dist_process (object_to_index, serverid,journal_type);


CREATE TABLE dist_reindex_journal (
  ID INTEGER NOT NULL ,
  INODE_TO_INDEX varchar2(100),
  IDENT_TO_INDEX varchar2(100),
  SERVERID VARCHAR2(64),
  priority INTEGER,
  TIME_ENTERED TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  index_val varchar2(325) ,
  dist_action INTEGER DEFAULT 1 NOT NULL,
  PRIMARY KEY (ID) VALIDATE);

CREATE INDEX dist_reindex_index1 on dist_reindex_journal (inode_to_index);
CREATE INDEX dist_reindex_index2 on dist_reindex_journal (dist_action);
CREATE INDEX dist_reindex_index3 on dist_reindex_journal (serverid);
CREATE INDEX dist_reindex_index4 on dist_reindex_journal (ident_to_index,serverid);
CREATE INDEX dist_reindex_index on dist_reindex_journal (serverid,dist_action);
CREATE INDEX dist_reindex_index5 ON dist_reindex_journal (priority, time_entered);
CREATE INDEX dist_reindex_index6 ON dist_reindex_journal (priority);

CREATE SEQUENCE dist_reindex_id_seq START WITH 1 INCREMENT BY 1;

create trigger dist_reindex_journal_trg
		before insert on dist_reindex_journal
		for each row
		when (new.id is null)
		begin
		select dist_reindex_id_seq.nextval into :new.id from dual;
		end;
/


CREATE TABLE quartz_log ( ID INTEGER NOT NULL , JOB_NAME VARCHAR2(255), SERVERID VARCHAR2(64),  TIME_STARTED TIMESTAMP, PRIMARY KEY (ID) VALIDATE );

create table plugin_property (
   plugin_id varchar2(255) not null,
   propkey varchar2(255) not null,
   original_value varchar2(255) not null,
   current_value varchar2(255) not null,
   primary key (plugin_id, propkey)
);

alter table plugin_property add constraint fk_plugin_plugin_property foreign key (plugin_id) references plugin(id);

CREATE SEQUENCE quartz_log_id_seq START WITH 1 INCREMENT BY 1;
create trigger quartz_log_trg
before insert on quartz_log
for each row
when (new.id is null)
begin
select quartz_log_id_seq.nextval into :new.id from dual;
end;
/

CREATE OR REPLACE TRIGGER check_identifier_host_inode
BEFORE INSERT OR UPDATE ON identifier
FOR EACH ROW
DECLARE
BEGIN
    dbms_output.put_line('asset_type: ' || SUBSTR(:new.asset_type,0,7));
    dbms_output.put_line('host_inode: ' || :new.host_inode);
    IF SUBSTR(:new.asset_type,0,7) <> 'content' AND (:new.host_inode is NULL OR :new.host_inode = '') THEN
    	RAISE_APPLICATION_ERROR(-20000, 'Cannot insert/update a null or empty host inode for this kind of identifier');
    END IF;
END;
/

ALTER TABLE cms_role ADD CONSTRAINT cms_role2_unique UNIQUE (role_key);
ALTER TABLE cms_role ADD CONSTRAINT cms_role3_unique UNIQUE (db_fqn);
alter table cms_role add constraint fkcms_role_parent foreign key (parent) references cms_role;

ALTER TABLE cms_layout ADD CONSTRAINT cms_layout_unique_1 UNIQUE (layout_name);

ALTER TABLE portlet ADD CONSTRAINT portlet_unique_1 UNIQUE (portletid);
ALTER TABLE cms_layouts_portlets ADD CONSTRAINT cms_layouts_portlets_unq_1 UNIQUE (portlet_id, layout_id);
alter table cms_layouts_portlets add constraint fkcms_layouts_portlets foreign key (layout_id) references cms_layout;

ALTER TABLE users_cms_roles ADD CONSTRAINT users_cms_roles1_unique UNIQUE (role_id, user_id);
alter table users_cms_roles add constraint fkusers_cms_roles1 foreign key (role_id) references cms_role;
alter table users_cms_roles add constraint fkusers_cms_roles2 foreign key (user_id) references user_;

ALTER TABLE layouts_cms_roles ADD CONSTRAINT layouts_cms_roles1_unique UNIQUE (role_id, layout_id);
alter table layouts_cms_roles add constraint fklayouts_cms_roles1 foreign key (role_id) references cms_role;
alter table layouts_cms_roles add constraint fklayouts_cms_roles2 foreign key (layout_id) references cms_layout;

ALTER TABLE dot_containers add constraint containers_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE template add constraint template_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE htmlpage add constraint htmlpage_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE file_asset add constraint file_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE contentlet add constraint content_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE links add constraint links_identifier_fk foreign key (identifier) references identifier(id);

create table import_audit (
	id integer not null,
	start_date timestamp,
	userid varchar(255),
	filename varchar(512),
	status integer,
	last_inode varchar(100),
	records_to_import integer,
	serverid varchar(255),
	primary key (id)
	);

alter table category modify (category_velocity_var_name varchar2(255) not null);

alter table import_audit add( warnings nclob,
	errors nclob,
	results nclob,
	messages nclob);

alter table structure modify host default 'SYSTEM_HOST';
alter table structure modify folder default 'SYSTEM_FOLDER';
alter table structure add constraint fk_structure_folder foreign key (folder) references folder(inode);
alter table structure modify (velocity_var_name varchar2(255) not null);
alter table structure add constraint unique_struct_vel_var_name unique (velocity_var_name);


CREATE OR REPLACE TRIGGER structure_host_folder_trigger
BEFORE INSERT OR UPDATE ON structure
FOR EACH ROW
DECLARE
   folderInode varchar2(100);
   hostInode varchar2(100);
BEGIN
    IF (:NEW.host <> 'SYSTEM_HOST' AND :NEW.folder <> 'SYSTEM_FOLDER') THEN
        select host_inode, folder.inode INTO hostInode, folderInode from folder,identifier where folder.identifier = identifier.id and folder.inode = :NEW.folder;
	  IF (:NEW.host <> hostInode) THEN
		RAISE_APPLICATION_ERROR(-20000, 'Cannot assign host/folder to structure, folder does not belong to given host');
	  END IF;
    ELSE
       IF(:NEW.host IS NULL OR :NEW.host = '' OR :NEW.host = 'SYSTEM_HOST' OR :NEW.folder IS NULL OR :NEW.folder = '' OR :NEW.folder = 'SYSTEM_FOLDER') THEN
          IF(:NEW.host = 'SYSTEM_HOST' OR :NEW.host IS NULL OR :NEW.host = '') THEN
               :NEW.host := 'SYSTEM_HOST';
               :NEW.folder := 'SYSTEM_FOLDER';
          END IF;
          IF(:NEW.folder = 'SYSTEM_FOLDER' OR :NEW.folder IS NULL OR :NEW.folder = '') THEN
             :NEW.folder := 'SYSTEM_FOLDER';
          END IF;
       END IF;
    END IF;
END;
/
create or replace
 PACKAGE types
AS
TYPE ref_cursor IS REF CURSOR;
END;
/
CREATE OR REPLACE TYPE reindex_record AS OBJECT (
  ID INTEGER,
  INODE_TO_INDEX varchar2(36),
  IDENT_TO_INDEX varchar2(36),
  priority INTEGER,
  dist_action INTEGER
);
/
CREATE OR REPLACE TYPE reindex_record_list IS TABLE OF reindex_record;
/
CREATE OR REPLACE FUNCTION load_records_to_index(server_id VARCHAR2, records_to_fetch NUMBER, priority_level NUMBER)
   RETURN types.ref_cursor IS
 cursor_ret types.ref_cursor;
 data_ret reindex_record_list;
BEGIN
  data_ret := reindex_record_list();
  FOR dj in (SELECT * FROM dist_reindex_journal
         WHERE serverid IS NULL AND priority <= priority_level AND rownum<=records_to_fetch
         ORDER BY priority ASC
         FOR UPDATE)
  LOOP
    UPDATE dist_reindex_journal SET serverid=server_id WHERE id=dj.id;
    data_ret.extend;
    data_ret(data_ret.Last) := reindex_record(dj.id,dj.inode_to_index,dj.ident_to_index,dj.priority,dj.dist_action);
  END LOOP;
  OPEN cursor_ret FOR
    SELECT * FROM TABLE(CAST(data_ret AS reindex_record_list));
  RETURN cursor_ret;
END;
/
CREATE OR REPLACE PACKAGE check_parent_path_pkg as
    type ridArray is table of rowid index by binary_integer;
    newRows ridArray;
    empty   ridArray;
END;
/
CREATE OR REPLACE TRIGGER check_parent_path_bi
BEFORE INSERT OR UPDATE ON identifier
BEGIN
  check_parent_path_pkg.newRows := check_parent_path_pkg.empty;
END;
/
CREATE OR REPLACE TRIGGER check_parent_path_aifer
AFTER INSERT OR UPDATE ON identifier
FOR EACH ROW
BEGIN
   check_parent_path_pkg.newRows(check_parent_path_pkg.newRows.count+1) := :new.rowid;
END;
/
CREATE OR REPLACE TRIGGER identifier_parent_path_check
AFTER INSERT OR UPDATE ON identifier
DECLARE
  rowcount varchar2(100);
  assetIdentifier varchar2(100);
  parentPath varchar2(255);
  hostInode varchar2(100);
BEGIN
   for i in 1 .. check_parent_path_pkg.newRows.count LOOP
      select id,parent_path,host_inode into assetIdentifier,parentPath,hostInode from identifier where rowid = check_parent_path_pkg.newRows(i);
      IF(parentPath='/' OR parentPath='/System folder') THEN
        return;
      ELSE
        select count(*) into rowcount from identifier where asset_type='folder' and host_inode = hostInode and parent_path||asset_name||'/' = parentPath and id <> assetIdentifier;
        IF (rowcount = 0) THEN
           RAISE_APPLICATION_ERROR(-20000, 'Cannot insert/update for this path does not exist for the given host');
        END IF;
      END IF;
END LOOP;
END;
/
CREATE OR REPLACE PACKAGE htmlpage_pkg as
 type array is table of htmlpage%rowtype index by binary_integer;
 oldvals array;
 empty array;
END;
/
CREATE OR REPLACE TRIGGER htmlpage_versions_bd
BEFORE DELETE ON htmlpage
BEGIN
  htmlpage_pkg.oldvals := htmlpage_pkg.empty;
END;
/
CREATE OR REPLACE TRIGGER htmlpage_versions_bdfer
BEFORE DELETE ON htmlpage
FOR EACH ROW
BEGIN
   htmlpage_pkg.oldvals(htmlpage_pkg.oldvals.count+1).identifier := :old.identifier;
END;
/
CREATE OR REPLACE TRIGGER htmlpage_versions_trigger
AFTER DELETE ON htmlpage
DECLARE
   versionsCount integer;
BEGIN
   for i in 1 .. htmlpage_pkg.oldvals.count LOOP
     select count(*) into versionsCount from htmlpage where identifier = htmlpage_pkg.oldvals(i).identifier;
     IF (versionsCount = 0)THEN
	 DELETE from identifier where id = htmlpage_pkg.oldvals(i).identifier;
     END IF;
   END LOOP;
END;
/
CREATE OR REPLACE PACKAGE file_pkg as
  type array is table of file_asset%rowtype index by binary_integer;
  oldvals array;
  empty array;
END;
/
CREATE OR REPLACE trigger file_versions_bd
BEFORE DELETE ON file_asset
BEGIN
    file_pkg.oldvals := file_pkg.empty;
END;
/
CREATE OR REPLACE TRIGGER file_versions_bdfer
BEFORE DELETE ON file_asset
FOR EACH ROW
BEGIN
    file_pkg.oldvals(file_pkg.oldvals.count+1).identifier := :old.identifier;
END;
/
CREATE OR REPLACE TRIGGER  file_versions_trigger
AFTER DELETE ON file_asset
DECLARE
   versionsCount integer;
BEGIN
   for i in 1 .. file_pkg.oldvals.count LOOP
    select count(*) into versionsCount from file_asset where identifier = file_pkg.oldvals(i).identifier;
     IF (versionsCount = 0)THEN
	 DELETE from identifier where id = file_pkg.oldvals(i).identifier;
     END IF;
   END LOOP;
END;
/
CREATE OR REPLACE PACKAGE content_pkg as
  type array is table of contentlet%rowtype index by binary_integer;
  oldvals array;
  empty array;
END;
/
CREATE OR REPLACE TRIGGER content_versions_bd
BEFORE DELETE ON contentlet
BEGIN
   content_pkg.oldvals := content_pkg.empty;
END;
/
CREATE OR REPLACE TRIGGER  content_versions_bdfer
BEFORE DELETE ON contentlet
FOR EACH ROW
BEGIN
    content_pkg.oldvals(content_pkg.oldvals.count+1).identifier := :old.identifier;
END;
/
CREATE OR REPLACE TRIGGER  content_versions_trigger
AFTER DELETE ON contentlet
DECLARE
   versionsCount integer;
BEGIN
   for i in 1 .. content_pkg.oldvals.count LOOP
     select count(*) into versionsCount from contentlet where identifier = content_pkg.oldvals(i).identifier;
     IF (versionsCount = 0)THEN
	   DELETE from identifier where id = content_pkg.oldvals(i).identifier;
     END IF;
   END LOOP;
END;
/
CREATE OR REPLACE PACKAGE link_pkg as
 type array is table of links%rowtype index by binary_integer;
 oldvals array;
 empty array;
END;
/
CREATE OR REPLACE TRIGGER link_versions_bd
BEFORE DELETE ON links
BEGIN
   link_pkg.oldvals := link_pkg.empty;
END;
/
CREATE OR REPLACE TRIGGER link_versions_bdfer
BEFORE DELETE ON links
FOR EACH ROW
BEGIN
    link_pkg.oldvals(link_pkg.oldvals.count+1).identifier := :old.identifier;
END;
/
CREATE OR REPLACE TRIGGER link_versions_trigger
AFTER DELETE ON links
DECLARE
   versionsCount integer;
BEGIN
   for i in 1 .. link_pkg.oldvals.count LOOP
      select count(*) into versionsCount from links where identifier = link_pkg.oldvals(i).identifier;
      IF (versionsCount = 0)THEN
	  DELETE from identifier where id = link_pkg.oldvals(i).identifier;
      END IF;
END LOOP;
END;
/
CREATE OR REPLACE PACKAGE container_pkg as
 type array is table of dot_containers%rowtype index by binary_integer;
 oldvals array;
 empty array;
END;
/
CREATE OR REPLACE TRIGGER container_versions_bd
BEFORE DELETE ON dot_containers
BEGIN
   container_pkg.oldvals := container_pkg.empty;
END;
/
CREATE OR REPLACE TRIGGER container_versions_bdfer
BEFORE DELETE ON dot_containers
FOR EACH ROW
BEGIN
     container_pkg.oldvals(container_pkg.oldvals.count+1).identifier := :old.identifier;
END;
/
CREATE OR REPLACE TRIGGER container_versions_trigger
AFTER DELETE ON dot_containers
DECLARE
  versionsCount integer;
BEGIN
   for i in 1 .. container_pkg.oldvals.count LOOP
     select count(*) into versionsCount from dot_containers where identifier = container_pkg.oldvals(i).identifier;
     IF (versionsCount = 0)THEN
	  DELETE from identifier where id = container_pkg.oldvals(i).identifier;
     END IF;
   END LOOP;
END;
/
CREATE OR REPLACE PACKAGE template_pkg as
  type array is table of template%rowtype index by binary_integer;
  oldvals    array;
  empty    array;
END;
/
CREATE OR REPLACE TRIGGER template_versions_bd
BEFORE DELETE ON template
BEGIN
   template_pkg.oldvals := template_pkg.empty;
END;
/
CREATE OR REPLACE TRIGGER template_versions_bdfer
BEFORE DELETE ON template
FOR EACH ROW
BEGIN
   template_pkg.oldvals(template_pkg.oldvals.count+1).identifier := :old.identifier;
END;
/
CREATE OR REPLACE TRIGGER template_versions_trigger
AFTER DELETE ON template
DECLARE
  versionsCount integer;
BEGIN
  for i in 1 .. template_pkg.oldvals.count LOOP
    select count(*) into versionsCount from template where identifier = template_pkg.oldvals(i).identifier;
    IF (versionsCount = 0)THEN
	DELETE from identifier where id = template_pkg.oldvals(i).identifier;
    END IF;
  END LOOP;
END;
/


ALTER TABLE clickstream MODIFY start_date TIMESTAMP;
ALTER TABLE clickstream MODIFY end_date TIMESTAMP;
ALTER TABLE clickstream_request MODIFY timestampper TIMESTAMP;
ALTER TABLE clickstream_404 MODIFY timestampper TIMESTAMP;
ALTER TABLE analytic_summary_period MODIFY full_date TIMESTAMP;
ALTER TABLE analytic_summary MODIFY avg_time_on_site TIMESTAMP;
ALTER TABLE analytic_summary_workstream MODIFY mod_date TIMESTAMP;
ALTER TABLE analytic_summary_visits MODIFY visit_time TIMESTAMP;
ALTER TABLE dashboard_user_preferences MODIFY mod_date TIMESTAMP;
alter table structure add constraint fk_structure_host foreign key (host) references identifier(id);

create index idx_template3 on template (lower(title));

CREATE INDEX idx_contentlet_4 ON contentlet (structure_inode);

CREATE INDEX idx_contentlet_identifier ON contentlet (identifier);

ALTER TABLE Folder add constraint folder_identifier_fk foreign key (identifier) references identifier(id);
--ALTER TABLE dot_containers add constraint structure_fk foreign key (structure_inode) references structure(inode);
ALTER TABLE htmlpage add constraint template_id_fk foreign key (template_id) references identifier(id);

CREATE OR REPLACE TRIGGER  check_template_identifier
BEFORE INSERT OR UPDATE ON htmlpage
FOR EACH ROW
DECLARE
  rowcount varchar2(100);
BEGIN
  select count(*) into rowcount from identifier where id= :NEW.template_id and asset_type='template';
  IF (rowcount = 0) THEN
    RAISE_APPLICATION_ERROR(-20000, 'Template Id should be the identifier of a template');
  END IF;
END;
/
CREATE OR REPLACE PACKAGE folder_pkg as
   type array is table of folder%rowtype index by binary_integer;
   oldvals array;
   empty array;
END;
/
CREATE OR REPLACE trigger folder_identifier_bd
BEFORE DELETE ON folder
BEGIN
folder_pkg.oldvals := folder_pkg.empty;
END;
/
CREATE OR REPLACE TRIGGER folder_identifier_bdfer
BEFORE DELETE ON folder
FOR EACH ROW
BEGIN
  folder_pkg.oldvals(folder_pkg.oldvals.count+1).identifier := :old.identifier;
END;
/
CREATE OR REPLACE TRIGGER  folder_identifier_trigger
AFTER DELETE ON folder
DECLARE
  versionsCount integer;
BEGIN
  for i in 1 .. folder_pkg.oldvals.count LOOP
    select count(*) into versionsCount from folder where identifier = folder_pkg.oldvals(i).identifier;
    IF (versionsCount = 0)THEN
	 DELETE from identifier where id = folder_pkg.oldvals(i).identifier;
    END IF;
 END LOOP;
END;
/
alter table contentlet add constraint fk_user_contentlet foreign key (mod_user) references user_(userid);
alter table htmlpage add constraint fk_user_htmlpage foreign key (mod_user) references user_(userid);
alter table dot_containers add constraint fk_user_containers foreign key (mod_user) references user_(userid);
alter table template add constraint fk_user_template foreign key (mod_user) references user_(userid);
alter table file_asset add constraint fk_user_file_asset foreign key (mod_user) references user_(userid);
alter table links add constraint fk_user_links foreign key (mod_user) references user_(userid);

create index idx_template_id on template_containers(template_id);
alter table template_containers add constraint FK_template_id foreign key (template_id) references identifier(id);
alter table template_containers add constraint FK_container_id foreign key (container_id) references identifier(id);

CREATE OR REPLACE PACKAGE child_assets_pkg as
   type array is table of identifier%rowtype index by binary_integer;
   oldvals array;
   empty array;
END;
/
CREATE OR REPLACE trigger check_child_assets_bd
BEFORE DELETE ON identifier
BEGIN
child_assets_pkg.oldvals := child_assets_pkg.empty;
END;
/
CREATE OR REPLACE TRIGGER check_child_assets_bdfer
BEFORE DELETE ON identifier
FOR EACH ROW
Declare
  i    number default child_assets_pkg.oldvals.count+1;
BEGIN
  child_assets_pkg.oldvals(i).id := :old.id;
  child_assets_pkg.oldvals(i).asset_type := :old.asset_type;
  child_assets_pkg.oldvals(i).asset_name:= :old.asset_name;
  child_assets_pkg.oldvals(i).parent_path:= :old.parent_path;
  child_assets_pkg.oldvals(i).host_inode:= :old.host_inode;
END;
/
CREATE OR REPLACE TRIGGER  check_child_assets_trigger
AFTER DELETE ON identifier
DECLARE
  pathCount integer;
BEGIN
  for i in 1 .. child_assets_pkg.oldvals.count LOOP
    IF(child_assets_pkg.oldvals(i).asset_type='folder')THEN
      select count(*) into pathCount from identifier where parent_path = child_assets_pkg.oldvals(i).parent_path||child_assets_pkg.oldvals(i).asset_name||'/' and host_inode = child_assets_pkg.oldvals(i).host_inode;
    END IF;
    IF(child_assets_pkg.oldvals(i).asset_type='contentlet')THEN
      select count(*) into pathCount from identifier where host_inode = child_assets_pkg.oldvals(i).id;
    END IF;
    IF (pathCount > 0 )THEN
          RAISE_APPLICATION_ERROR(-20000, 'Cannot delete as this path has children');
    END IF;
 END LOOP;
END;
/
CREATE OR REPLACE PROCEDURE renameFolderChildren(oldPath IN varchar2,newPath IN varchar2,hostInode IN varchar2) IS
  newFolderPath varchar2(255);
  oldFolderPath varchar2(255);
BEGIN
 UPDATE identifier SET  parent_path  = newPath where parent_path = oldPath and host_inode = hostInode;
 FOR i in (select * from identifier where asset_type='folder' and parent_path = newPath and host_inode = hostInode)
  LOOP
   newFolderPath := newPath || i.asset_name || '/';
   oldFolderPath := oldPath || i.asset_name || '/';
   renameFolderChildren(oldFolderPath,newFolderPath,hostInode);
  END LOOP;
END;
/
CREATE OR REPLACE TRIGGER rename_folder_assets_trigger
AFTER UPDATE ON Folder
FOR EACH ROW
DECLARE
 oldPath varchar2(255);
 newPath varchar2(255);
 hostInode varchar2(100);
BEGIN
	IF :NEW.name <> :OLD.name THEN
      SELECT parent_path||asset_name||'/',parent_path ||:NEW.name||'/',host_inode INTO oldPath,newPath,hostInode from identifier where id = :NEW.identifier;
      UPDATE identifier SET asset_name = :NEW.name where id = :NEW.identifier;
      renameFolderChildren(oldPath,newPath,hostInode);
    END IF;
END;
/
CREATE OR REPLACE FUNCTION dotFolderPath(parent_path IN varchar2, asset_name IN varchar2) RETURN varchar2 IS
BEGIN
  IF parent_path='/System folder' THEN
    RETURN '/';
  ELSE
    RETURN parent_path || asset_name || '/';
  END IF;
END;
/
alter table contentlet_version_info add constraint fk_con_ver_info_ident foreign key (identifier) references identifier(id) on delete cascade;
alter table container_version_info  add constraint fk_container_ver_info_ident  foreign key (identifier) references identifier(id);
alter table template_version_info   add constraint fk_template_ver_info_ident   foreign key (identifier) references identifier(id);
alter table htmlpage_version_info   add constraint fk_htmlpage_ver_info_ident   foreign key (identifier) references identifier(id);
alter table fileasset_version_info  add constraint fk_fileasset_ver_info_ident  foreign key (identifier) references identifier(id);
alter table link_version_info       add constraint fk_link_ver_info_ident      foreign key (identifier) references identifier(id);

alter table contentlet_version_info add constraint fk_con_ver_info_working foreign key (working_inode) references contentlet(inode);
alter table container_version_info  add constraint fk_container_ver_info_working  foreign key (working_inode) references dot_containers(inode);
alter table template_version_info   add constraint fk_template_ver_info_working   foreign key (working_inode) references template(inode);
alter table htmlpage_version_info   add constraint fk_htmlpage_ver_info_working   foreign key (working_inode) references htmlpage(inode);
alter table fileasset_version_info  add constraint fk_fileasset_ver_info_working  foreign key (working_inode) references file_asset(inode);
alter table link_version_info       add constraint fk_link_version_info_working       foreign key (working_inode) references links(inode);

alter table contentlet_version_info add constraint fk_con_ver_info_live foreign key (live_inode) references contentlet(inode);
alter table container_version_info  add constraint fk_container_ver_info_live  foreign key (live_inode) references dot_containers(inode);
alter table template_version_info   add constraint fk_template_ver_info_live   foreign key (live_inode) references template(inode);
alter table htmlpage_version_info   add constraint fk_htmlpage_ver_info_live   foreign key (live_inode) references htmlpage(inode);
alter table fileasset_version_info  add constraint fk_fileasset_ver_info_live  foreign key (live_inode) references file_asset(inode);
alter table link_version_info       add constraint fk_link_version_info_live       foreign key (live_inode) references links(inode);

alter table contentlet_version_info add constraint fk_con_lang_ver_info_lang foreign key (lang) references language(id);

alter table folder add constraint fk_folder_file_structure_type foreign key(default_file_type) references structure(inode);

alter table workflowtask_files add constraint FK_workflow_id foreign key (workflowtask_id) references workflow_task(id);
--alter table workflowtask_files add constraint FK_task_file_inode foreign key (file_inode) references file_asset(inode);
alter table workflow_comment add constraint wf_id_comment_FK foreign key (workflowtask_id) references workflow_task(id);
alter table workflow_history add constraint wf_id_history_FK foreign key (workflowtask_id) references workflow_task(id);

alter table contentlet add constraint fk_contentlet_lang foreign key (language_id) references language(id);

create table workflow_scheme(
	id varchar2(36) primary key,
	name varchar2(255) not null,
	description varchar2(255),
	archived number(1,0) default 0,
	mandatory number(1,0) default 0,
	default_scheme number(1,0) default 0,
	entry_action_id varchar2(36),
	mod_date timestamp
);
alter table workflow_scheme add constraint unique_workflow_scheme_name unique (name);

create table workflow_step(
	id varchar2(36) primary key,
	name varchar2(255) not null,
	scheme_id varchar2(36) not null references workflow_scheme(id),
	my_order number(10,0) default 0,
	resolved number(1,0) default 0,
	escalation_enable number(1,0) default 0,
    escalation_action varchar(36),
    escalation_time number(10,0) default 0
);
create index wk_idx_step_scheme on workflow_step(scheme_id);

-- Permissionable ---
create table workflow_action(
	id varchar2(36) primary key,
	step_id varchar2(36) not null  references workflow_step(id),
	name varchar2(255) not null,
	condition_to_progress nclob,
	next_step_id varchar2(36) not null references workflow_step(id),
	next_assign varchar2(36) not null references cms_role(id),
	my_order number(10,0) default 0,
	assignable number(1,0) default 0,
	commentable number(1,0) default 0,
	requires_checkout number(1,0) default 0,
	icon varchar2(255) default 'defaultWfIcon',
	use_role_hierarchy_assign number(1,0) default 0
);
create index wk_idx_act_step on workflow_action(step_id);


create table workflow_action_class(
	id varchar2(36) primary key,
	action_id varchar2(36) not null references workflow_action(id),
	name varchar2(255) not null,
	my_order number(10,0) default 0,
	clazz nclob
);
create index wk_idx_act_class_act on workflow_action_class(action_id);

create table workflow_action_class_pars(
	id varchar2(36) primary key,
	workflow_action_class_id varchar2(36) not null references workflow_action_class(id),
	key varchar2(255) not null,
	value nclob
);
create index wk_idx_actclassparamact on
	workflow_action_class_pars(workflow_action_class_id);


create table workflow_scheme_x_structure(
	id varchar2(36) primary key,
	scheme_id varchar2(36) not null references workflow_scheme(id),
	structure_id varchar2(36) not null references structure(inode)
);

create unique index wk_idx_scheme_str_2 on
	workflow_scheme_x_structure(structure_id);



delete from workflow_history;
delete from workflow_comment;
delete from workflowtask_files;
delete from workflow_task;
alter table workflow_task add constraint FK_workflow_task_asset foreign key (webasset) references identifier(id);
alter table workflow_task add constraint FK_workflow_assign foreign key (assigned_to) references cms_role(id);
alter table workflow_task add constraint FK_workflow_step foreign key (status) references workflow_step(id);
alter table workflow_step add constraint fk_escalation_action foreign key (escalation_action) references workflow_action(id);

alter table contentlet_version_info add constraint FK_con_ver_lockedby foreign key (locked_by) references user_(userid);
alter table container_version_info  add constraint FK_tainer_ver_info_lockedby  foreign key (locked_by) references user_(userid);
alter table template_version_info   add constraint FK_temp_ver_info_lockedby   foreign key (locked_by) references user_(userid);
alter table htmlpage_version_info   add constraint FK_page_ver_info_lockedby   foreign key (locked_by) references user_(userid);
alter table fileasset_version_info  add constraint FK_fil_ver_info_lockedby  foreign key (locked_by) references user_(userid);
alter table link_version_info       add constraint FK_link_ver_info_lockedby       foreign key (locked_by) references user_(userid);

ALTER table tag MODIFY host_id default 'SYSTEM_HOST';
alter table tag add constraint tag_tagname_host unique (tagname, host_id);
alter table tag_inode add constraint fk_tag_inode_tagid foreign key (tag_id) references tag (tag_id);

drop index tag_user_id_index;
alter table tag modify user_id long;
alter table tag modify user_id CLOB;
create index tag_user_id_index on tag(user_id) indextype is ctxsys.context;


-- ****** Indicies Data Storage *******
create table indicies (
  index_name varchar2(30) primary key,
  index_type varchar2(16) not null unique
);
  -- ****** Log Console Table *******
  create table log_mapper (
    enabled   	 number(1,0) not null,
    log_name 	 varchar2(30) not null,
    description  varchar2(50) not null,
    primary key (log_name)
  );

  insert into log_mapper (ENABLED,LOG_NAME,DESCRIPTION) values ('1','dotcms-userActivity.log','Log Users action on pages, structures, documents.');
  insert into log_mapper (ENABLED,LOG_NAME,DESCRIPTION) values ('1','dotcms-security.log','Log users login activity into dotCMS.');
  insert into log_mapper (ENABLED,LOG_NAME,DESCRIPTION) values ('1','dotcms-adminaudit.log','Log Admin activity on dotCMS.');
  insert into log_mapper (ENABLED,LOG_NAME,DESCRIPTION) values ('1','dotcms-pushpublish.log','Log Push Publishing activity on dotCMS.');

create index idx_identifier_perm on identifier (asset_type,host_inode);


CREATE TABLE broken_link (
   id VARCHAR(36) PRIMARY KEY,
   inode VARCHAR2(36) NOT NULL,
   field VARCHAR2(36) NOT NULL,
   link VARCHAR2(255) NOT NULL,
   title VARCHAR2(255) NOT NULL,
   status_code integer NOT NULL
);

alter table broken_link add CONSTRAINT fk_brokenl_content
    FOREIGN KEY (inode) REFERENCES contentlet(inode) ON DELETE CASCADE;

alter table broken_link add CONSTRAINT fk_brokenl_field
    FOREIGN KEY (field) REFERENCES field(inode) ON DELETE CASCADE;

-- ****** Content Publishing Framework *******
CREATE SEQUENCE PUBLISHING_QUEUE_SEQ START WITH 1 INCREMENT BY 1;

CREATE TABLE publishing_queue (
  id INTEGER PRIMARY KEY NOT NULL,
  operation number(19,0),
  asset VARCHAR2(2000) NOT NULL,
  language_id number(19,0) NOT NULL,
  entered_date TIMESTAMP,
  publish_date TIMESTAMP,
  type VARCHAR2(256),
  bundle_id VARCHAR2(256)
);

CREATE OR REPLACE TRIGGER PUBLISHING_QUEUE_TRIGGER before
insert on publishing_queue for each row
begin select PUBLISHING_QUEUE_SEQ.nextval into :new.id from dual;
end;
/

CREATE TABLE publishing_queue_audit
(bundle_id VARCHAR2(256) PRIMARY KEY NOT NULL,
status INTEGER,
status_pojo nclob,
status_updated TIMESTAMP,
create_date TIMESTAMP);

-- ****** Content Publishing Framework - End Point Management *******
CREATE TABLE publishing_end_point (
	id VARCHAR2(36) PRIMARY KEY,
	group_id VARCHAR2(700),
	server_name VARCHAR2(700) unique,
	address VARCHAR2(250),
	port VARCHAR2(10),
	protocol VARCHAR2(10),
	enabled number(1,0) DEFAULT 0,
	auth_key nclob,
	sending number(1,0) DEFAULT 0);

create table publishing_environment(
	id varchar2(36) NOT NULL  primary key,
	name varchar2(255) NOT NULL unique,
	push_to_all number(1,0) DEFAULT 0 NOT NULL
);

create table sitesearch_audit (
    job_id varchar2(36),
    job_name varchar2(255) not null,
    fire_date timestamp not null,
    incremental number(1,0) not null,
    start_date timestamp,
    end_date timestamp,
    host_list varchar2(500) not null,
    all_hosts number(1,0) not null,
    lang_list varchar2(500) not null,
    path varchar2(500) not null,
    path_include number(1,0) not null,
    files_count number(10,0) not null,
    pages_count number(10,0) not null,
    urlmaps_count number(10,0) not null,
    index_name varchar2(100) not null,
    primary key(job_id,fire_date)
);

create table publishing_bundle(
	  id varchar2(36) NOT NULL  primary key,
	  name varchar2(255) NOT NULL,
	  publish_date TIMESTAMP,
	  expire_date TIMESTAMP,
	  owner varchar2(100)
);

create table publishing_bundle_environment(
	id varchar2(36) NOT NULL primary key,
	bundle_id varchar2(36) NOT NULL,
	environment_id varchar2(36) NOT NULL
);

alter table publishing_bundle_environment add constraint FK_bundle_id foreign key (bundle_id) references publishing_bundle(id);
alter table publishing_bundle_environment add constraint FK_environment_id foreign key (environment_id) references publishing_environment(id);

create table publishing_pushed_assets(
	bundle_id varchar2(36) NOT NULL,
	asset_id varchar2(36) NOT NULL,
	asset_type varchar2(255) NOT NULL,
	push_date TIMESTAMP,
	environment_id varchar2(36) NOT NULL
);

CREATE INDEX idx_pushed_assets_1 ON publishing_pushed_assets (bundle_id);
CREATE INDEX idx_pushed_assets_2 ON publishing_pushed_assets (environment_id);
CREATE INDEX idx_pushed_assets_3 ON publishing_pushed_assets (asset_id, environment_id);

alter table publishing_bundle add force_push number(1,0) ;

CREATE INDEX idx_pub_qa_1 ON publishing_queue_audit (status);

-- Cluster Tables
CREATE TABLE dot_cluster(cluster_id varchar2(36), PRIMARY KEY (cluster_id) );
CREATE TABLE cluster_server(server_id varchar2(36) NOT NULL, cluster_id varchar2(36) NOT NULL, name varchar2(100), ip_address varchar2(39) NOT NULL, host varchar2(36), cache_port SMALLINT, es_transport_tcp_port SMALLINT, es_network_port SMALLINT, es_http_port SMALLINT, key_ varchar2(100), PRIMARY KEY (server_id) );
ALTER TABLE cluster_server add constraint fk_cluster_id foreign key (cluster_id) REFERENCES dot_cluster(cluster_id);
CREATE TABLE cluster_server_uptime(id varchar2(36) NOT NULL,server_id varchar2(36) NOT NULL, startup TIMESTAMP, heartbeat TIMESTAMP, PRIMARY KEY (id));
ALTER TABLE cluster_server_uptime add constraint fk_cluster_server_id foreign key (server_id) REFERENCES cluster_server(server_id);

-- Notifications Table
create table notification(id varchar2(36) NOT NULL,message nclob NOT NULL, notification_type varchar2(100), notification_level varchar2(100), user_id varchar2(255) NOT NULL, time_sent TIMESTAMP NOT NULL, was_read number(1,0) default 0, PRIMARY KEY (id));
create index idx_not_user ON notification (user_id);
create index idx_not_read ON notification (was_read);

-- indices for version_info tables on version_ts
create index idx_contentlet_vi_version_ts on contentlet_version_info(version_ts);
create index idx_container_vi_version_ts on container_version_info(version_ts);
create index idx_template_vi_version_ts on template_version_info(version_ts);
create index idx_htmlpage_vi_version_ts on htmlpage_version_info(version_ts);
create index idx_fileasset_vi_version_ts on fileasset_version_info(version_ts);
create index idx_link_vi_version_ts on link_version_info(version_ts);

-- container multiple structures
create index idx_container_id on container_structures(container_id);
alter table container_structures add constraint FK_cs_container_id foreign key (container_id) references identifier(id);
alter table container_structures add constraint FK_cs_inode foreign key (container_inode) references inode(inode);


-- license repo
create table sitelic(id varchar(36) primary key, serverid varchar(100), license nclob not null, lastping date not null);

create table folders_ir(folder varchar2(255), local_inode varchar2(36), remote_inode varchar2(36), local_identifier varchar2(36), remote_identifier varchar2(36), endpoint_id varchar2(36), PRIMARY KEY (local_inode, endpoint_id));
create table structures_ir(velocity_name varchar2(255), local_inode varchar2(36), remote_inode varchar2(36), endpoint_id varchar2(36), PRIMARY KEY (local_inode, endpoint_id));
create table schemes_ir(name varchar2(255), local_inode varchar2(36), remote_inode varchar2(36), endpoint_id varchar2(36), PRIMARY KEY (local_inode, endpoint_id));
create table htmlpages_ir(html_page varchar2(255), local_working_inode varchar2(36), local_live_inode varchar2(36), remote_working_inode varchar2(36), remote_live_inode varchar2(36),local_identifier varchar2(36), remote_identifier varchar2(36), endpoint_id varchar2(36), language_id number(19,0), PRIMARY KEY (local_working_inode, language_id, endpoint_id));
create table fileassets_ir(file_name varchar2(255), local_working_inode varchar2(36), local_live_inode varchar2(36), remote_working_inode varchar2(36), remote_live_inode varchar2(36),local_identifier varchar2(36), remote_identifier varchar2(36), endpoint_id varchar2(36), language_id number(19,0), PRIMARY KEY (local_working_inode, language_id, endpoint_id));

---Server Action
create table cluster_server_action(
	server_action_id varchar2(36) not null, 
	originator_id varchar2(36) not null, 
	server_id varchar2(36) not null, 
	failed number(1, 0), 
	response varchar2(2048), 
	action_id varchar2(1024) not null,
	completed number(1, 0), 
	entered_date date not null,
	time_out_seconds number(13) not null,
	PRIMARY KEY (server_action_id)
);

-- Rules Engine
create table dot_rule(id varchar2(36),name varchar2(255) not null,fire_on varchar2(20),short_circuit  number(1,0) default 0,parent_id varchar2(36) not null,folder varchar2(36) not null,priority number(10,0) default 0,enabled  number(1,0) default 0,mod_date timestamp,primary key (id));
create table rule_condition_group(id varchar2(36) primary key,rule_id varchar2(36) references dot_rule(id),operator varchar2(10) not null,priority number(10,0) default 0,mod_date timestamp);
create table rule_condition(id varchar2(36) primary key,conditionlet nclob not null,condition_group varchar(36) references rule_condition_group(id),comparison varchar2(36) not null,operator varchar2(10) not null,priority number(10,0) default 0,mod_date timestamp);
create table rule_condition_value (id varchar2(36) primary key,condition_id varchar2(36) references rule_condition(id), paramkey varchar2(255) not null, value nclob,priority number(10,0) default 0);
create table rule_action (id varchar2(36) primary key,rule_id varchar2(36) references dot_rule(id),priority number(10,0) default 0,actionlet nclob not null,mod_date timestamp);
create table rule_action_pars(id varchar2(36) primary key,rule_action_id varchar2(36) references rule_action(id), paramkey varchar2(255) not null,value nclob);
create index idx_rules_fire_on on dot_rule (fire_on);


-- Delete User
ALTER TABLE user_ ADD delete_in_progress number(1,0) default 0;
ALTER TABLE user_ ADD delete_date DATE;