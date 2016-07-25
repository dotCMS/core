create table Address (
	addressId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate datetime null,
	modifiedDate datetime null,
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
	config longtext null
);

create table Company (
	companyId varchar(100) not null primary key,
	key_ longtext null,
	portalURL varchar(100) not null,
	homeURL varchar(100) not null,
	mx varchar(100) not null,
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
	autoLogin tinyint,
	strangers tinyint
);

create table Counter (
	name varchar(100) not null primary key,
	currentId integer
);

create table Image (
	imageId varchar(200) not null primary key,
	text_ longtext not null
);

create table PasswordTracker (
	passwordTrackerId varchar(100) not null primary key,
	userId varchar(100) not null,
	createDate datetime not null,
	password_ varchar(100) not null
);

create table PollsChoice (
	choiceId varchar(100) not null,
	questionId varchar(100) not null,
	description longtext null,
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
	createDate datetime null,
	modifiedDate datetime null,
	title varchar(100) null,
	description longtext null,
	expirationDate datetime null,
	lastVoteDate datetime null
);

create table PollsVote (
	questionId varchar(100) not null,
	userId varchar(100) not null,
	choiceId varchar(100) not null,
	voteDate datetime null,
	primary key (questionId, userId)
);

create table Portlet (
	portletId varchar(100) not null,
	groupId varchar(100) not null,
	companyId varchar(100) not null,
	defaultPreferences longtext null,
	narrow tinyint,
	roles longtext null,
	active_ tinyint,
	primary key (portletId, groupId, companyId)
);

create table PortletPreferences (
	portletId varchar(100) not null,
	userId varchar(100) not null,
	layoutId varchar(100) not null,
	preferences longtext null,
	primary key (portletId, userId, layoutId)
);

create table Release_ (
	releaseId varchar(100) not null primary key,
	createDate datetime null,
	modifiedDate datetime null,
	buildNumber integer null,
	buildDate datetime null
);

create table User_ (
	userId varchar(100) not null primary key,
	companyId varchar(100) not null,
	createDate datetime null,
	password_ longtext null,
	passwordEncrypted tinyint,
	passwordExpirationDate datetime null,
	passwordReset tinyint,
	firstName varchar(100) null,
	middleName varchar(100) null,
	lastName varchar(100) null,
	nickName varchar(100) null,
	male tinyint,
	birthday datetime null,
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
	dottedSkins tinyint,
	roundedSkins tinyint,
	greeting varchar(100) null,
	resolution varchar(100) null,
	refreshRate varchar(100) null,
	layoutIds varchar(100) null,
	comments longtext null,
	loginDate datetime null,
	loginIP varchar(100) null,
	lastLoginDate datetime null,
	lastLoginIP varchar(100) null,
	failedLoginAttempts integer,
	agreedToTermsOfUse tinyint,
	active_ tinyint
);

create table UserTracker (
	userTrackerId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	modifiedDate datetime null,
	remoteAddr varchar(100) null,
	remoteHost varchar(100) null,
	userAgent varchar(100) null
);

create table UserTrackerPath (
	userTrackerPathId varchar(100) not null primary key,
	userTrackerId varchar(100) not null,
	path longtext not null,
	pathDate datetime not null
);

##
## Global
##

insert into Counter values ('com.liferay.portal.model.Address', 10);
insert into Counter values ('com.liferay.portal.model.Role', 100);
insert into Counter values ('com.liferay.portal.model.User.liferay.com', 10);
insert into Counter values ('com.liferay.portlet.polls.model.PollsQuestion', 10);
##
## Liferay, LLC
##

insert into Company (companyId, portalURL, homeURL, mx, name, shortName, type_, size_, emailAddress, authType, autoLogin, strangers) values ('liferay.com', 'localhost', 'localhost', 'liferay.com', 'Liferay, LLC', 'Liferay', 'biz', '', 'test@liferay.com', 'emailAddress', '1', '1');
update Company set street = '1220 Brea Canyon Rd.', city = 'Diamond Bar', state = 'CA', zip = '91789' where companyId = 'liferay.com';

insert into PollsDisplay (layoutId, userId, portletId, questionId) values ('1.1', 'group.1', '59', '1');
insert into PollsChoice (choiceId, questionId, description) values ('a', '1', 'Chocolate');
insert into PollsChoice (choiceId, questionId, description) values ('b', '1', 'Strawberry');
insert into PollsChoice (choiceId, questionId, description) values ('c', '1', 'Vanilla');
insert into PollsQuestion (questionId, portletId, groupId, companyId, userId, userName, createDate, modifiedDate, title, description) values ('1', '25', '-1', 'liferay.com', 'liferay.com.1', 'John Wayne', now(), now(), 'What is your favorite ice cream flavor?', 'What is your favorite ice cream flavor?');

##
## Default User
##

insert into User_ (userId, companyId, createDate, password_, passwordEncrypted, passwordReset, firstName, middleName, lastName, male, birthday, emailAddress, skinId, dottedSkins, roundedSkins, greeting, layoutIds, loginDate, failedLoginAttempts, agreedToTermsOfUse, active_) values ('liferay.com.default', 'default', now(), 'password', '0', '0', '', '', '', '1', '1970-01-01', 'default@liferay.com', '01', '0', '0', 'Welcome!', '', now(), 0, '0', '1');

##
## Test User
##

insert into User_ (userId, companyId, createDate, password_, passwordEncrypted, passwordReset, firstName, middleName, lastName, nickName, male, birthday, emailAddress, skinId, dottedSkins, roundedSkins, greeting, layoutIds, loginDate, failedLoginAttempts, agreedToTermsOfUse, active_) values ('liferay.com.1', 'liferay.com', now(), 'test', '0', '0', 'John', '', 'Wayne', 'Duke', '1', '1970-01-01', 'test@liferay.com', '01', '0', '1', 'Welcome John Wayne!', '1,', now(), 0, '1', '1');
CREATE TABLE QRTZ_JOB_DETAILS
  (
    JOB_NAME  VARCHAR(80) NOT NULL,
    JOB_GROUP VARCHAR(80) NOT NULL,
    DESCRIPTION VARCHAR(120) NULL,
    JOB_CLASS_NAME   VARCHAR(128) NOT NULL,
    IS_DURABLE tinyint(1) NOT NULL,
    IS_VOLATILE tinyint(1) NOT NULL,
    IS_STATEFUL tinyint(1) NOT NULL,
    REQUESTS_RECOVERY tinyint(1) NOT NULL,
    JOB_DATA BLOB NULL,
    PRIMARY KEY (JOB_NAME,JOB_GROUP)
);

CREATE TABLE QRTZ_JOB_LISTENERS
  (
    JOB_NAME  VARCHAR(80) NOT NULL,
    JOB_GROUP VARCHAR(80) NOT NULL,
    JOB_LISTENER VARCHAR(80) NOT NULL,
    PRIMARY KEY (JOB_NAME,JOB_GROUP,JOB_LISTENER),
    FOREIGN KEY (JOB_NAME,JOB_GROUP)
        REFERENCES QRTZ_JOB_DETAILS(JOB_NAME,JOB_GROUP)
);

CREATE TABLE QRTZ_TRIGGERS
  (
    TRIGGER_NAME VARCHAR(80) NOT NULL,
    TRIGGER_GROUP VARCHAR(80) NOT NULL,
    JOB_NAME  VARCHAR(80) NOT NULL,
    JOB_GROUP VARCHAR(80) NOT NULL,
    IS_VOLATILE tinyint(1) NOT NULL,
    DESCRIPTION VARCHAR(120) NULL,
    NEXT_FIRE_TIME BIGINT(13) NULL,
    PREV_FIRE_TIME BIGINT(13) NULL,
    PRIORITY INTEGER NULL,
    TRIGGER_STATE VARCHAR(16) NOT NULL,
    TRIGGER_TYPE VARCHAR(8) NOT NULL,
    START_TIME BIGINT(13) NOT NULL,
    END_TIME BIGINT(13) NULL,
    CALENDAR_NAME VARCHAR(80) NULL,
    MISFIRE_INSTR SMALLINT(2) NULL,
    JOB_DATA BLOB NULL,
    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (JOB_NAME,JOB_GROUP)
        REFERENCES QRTZ_JOB_DETAILS(JOB_NAME,JOB_GROUP)
);

CREATE TABLE QRTZ_SIMPLE_TRIGGERS
  (
    TRIGGER_NAME VARCHAR(80) NOT NULL,
    TRIGGER_GROUP VARCHAR(80) NOT NULL,
    REPEAT_COUNT BIGINT(7) NOT NULL,
    REPEAT_INTERVAL BIGINT(12) NOT NULL,
    TIMES_TRIGGERED BIGINT(7) NOT NULL,
    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (TRIGGER_NAME,TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS(TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE QRTZ_CRON_TRIGGERS
  (
    TRIGGER_NAME VARCHAR(80) NOT NULL,
    TRIGGER_GROUP VARCHAR(80) NOT NULL,
    CRON_EXPRESSION VARCHAR(80) NOT NULL,
    TIME_ZONE_ID VARCHAR(80),
    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (TRIGGER_NAME,TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS(TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE QRTZ_BLOB_TRIGGERS
  (
    TRIGGER_NAME VARCHAR(80) NOT NULL,
    TRIGGER_GROUP VARCHAR(80) NOT NULL,
    BLOB_DATA BLOB NULL,
    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (TRIGGER_NAME,TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS(TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE QRTZ_TRIGGER_LISTENERS
  (
    TRIGGER_NAME  VARCHAR(80) NOT NULL,
    TRIGGER_GROUP VARCHAR(80) NOT NULL,
    TRIGGER_LISTENER VARCHAR(80) NOT NULL,
    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP,TRIGGER_LISTENER),
    FOREIGN KEY (TRIGGER_NAME,TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS(TRIGGER_NAME,TRIGGER_GROUP)
);


CREATE TABLE QRTZ_CALENDARS
  (
    CALENDAR_NAME  VARCHAR(80) NOT NULL,
    CALENDAR BLOB NOT NULL,
    PRIMARY KEY (CALENDAR_NAME)
);



CREATE TABLE QRTZ_PAUSED_TRIGGER_GRPS
  (
    TRIGGER_GROUP  VARCHAR(80) NOT NULL,
    PRIMARY KEY (TRIGGER_GROUP)
);

CREATE TABLE QRTZ_FIRED_TRIGGERS
  (
    ENTRY_ID VARCHAR(95) NOT NULL,
    TRIGGER_NAME VARCHAR(80) NOT NULL,
    TRIGGER_GROUP VARCHAR(80) NOT NULL,
    IS_VOLATILE tinyint(1) NOT NULL,
    INSTANCE_NAME VARCHAR(80) NOT NULL,
    FIRED_TIME BIGINT(13) NOT NULL,
    PRIORITY INTEGER NOT NULL,
    STATE VARCHAR(16) NOT NULL,
    JOB_NAME VARCHAR(80) NULL,
    JOB_GROUP VARCHAR(80) NULL,
    IS_STATEFUL tinyint(1) NULL,
    REQUESTS_RECOVERY tinyint(1) NULL,
    PRIMARY KEY (ENTRY_ID)
);

CREATE TABLE QRTZ_SCHEDULER_STATE
  (
    INSTANCE_NAME VARCHAR(80) NOT NULL,
    LAST_CHECKIN_TIME BIGINT(13) NOT NULL,
    CHECKIN_INTERVAL BIGINT(13) NOT NULL,
    PRIMARY KEY (INSTANCE_NAME)
);

CREATE TABLE QRTZ_LOCKS
  (
    LOCK_NAME  VARCHAR(40) NOT NULL,
    PRIMARY KEY (LOCK_NAME)
);


INSERT INTO QRTZ_LOCKS values('TRIGGER_ACCESS');
INSERT INTO QRTZ_LOCKS values('JOB_ACCESS');
INSERT INTO QRTZ_LOCKS values('CALENDAR_ACCESS');
INSERT INTO QRTZ_LOCKS values('STATE_ACCESS');
INSERT INTO QRTZ_LOCKS values('MISFIRE_ACCESS');

CREATE TABLE QRTZ_EXCL_JOB_DETAILS
  (
    JOB_NAME  VARCHAR(80) NOT NULL,
    JOB_GROUP VARCHAR(80) NOT NULL,
    DESCRIPTION VARCHAR(120) NULL,
    JOB_CLASS_NAME   VARCHAR(128) NOT NULL,
    IS_DURABLE tinyint(1) NOT NULL,
    IS_VOLATILE tinyint(1) NOT NULL,
    IS_STATEFUL tinyint(1) NOT NULL,
    REQUESTS_RECOVERY tinyint(1) NOT NULL,
    JOB_DATA BLOB NULL,
    PRIMARY KEY (JOB_NAME,JOB_GROUP)
);

CREATE TABLE QRTZ_EXCL_JOB_LISTENERS
  (
    JOB_NAME  VARCHAR(80) NOT NULL,
    JOB_GROUP VARCHAR(80) NOT NULL,
    JOB_LISTENER VARCHAR(80) NOT NULL,
    PRIMARY KEY (JOB_NAME,JOB_GROUP,JOB_LISTENER),
    FOREIGN KEY (JOB_NAME,JOB_GROUP)
        REFERENCES QRTZ_EXCL_JOB_DETAILS(JOB_NAME,JOB_GROUP)
);

CREATE TABLE QRTZ_EXCL_TRIGGERS
  (
    TRIGGER_NAME VARCHAR(80) NOT NULL,
    TRIGGER_GROUP VARCHAR(80) NOT NULL,
    JOB_NAME  VARCHAR(80) NOT NULL,
    JOB_GROUP VARCHAR(80) NOT NULL,
    IS_VOLATILE tinyint(1) NOT NULL,
    DESCRIPTION VARCHAR(120) NULL,
    NEXT_FIRE_TIME BIGINT(13) NULL,
    PREV_FIRE_TIME BIGINT(13) NULL,
    PRIORITY INTEGER NULL,
    TRIGGER_STATE VARCHAR(16) NOT NULL,
    TRIGGER_TYPE VARCHAR(8) NOT NULL,
    START_TIME BIGINT(13) NOT NULL,
    END_TIME BIGINT(13) NULL,
    CALENDAR_NAME VARCHAR(80) NULL,
    MISFIRE_INSTR SMALLINT(2) NULL,
    JOB_DATA BLOB NULL,
    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (JOB_NAME,JOB_GROUP)
        REFERENCES QRTZ_EXCL_JOB_DETAILS(JOB_NAME,JOB_GROUP)
);

CREATE TABLE QRTZ_EXCL_SIMPLE_TRIGGERS
  (
    TRIGGER_NAME VARCHAR(80) NOT NULL,
    TRIGGER_GROUP VARCHAR(80) NOT NULL,
    REPEAT_COUNT BIGINT(7) NOT NULL,
    REPEAT_INTERVAL BIGINT(12) NOT NULL,
    TIMES_TRIGGERED BIGINT(7) NOT NULL,
    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (TRIGGER_NAME,TRIGGER_GROUP)
        REFERENCES QRTZ_EXCL_TRIGGERS(TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE QRTZ_EXCL_CRON_TRIGGERS
  (
    TRIGGER_NAME VARCHAR(80) NOT NULL,
    TRIGGER_GROUP VARCHAR(80) NOT NULL,
    CRON_EXPRESSION VARCHAR(80) NOT NULL,
    TIME_ZONE_ID VARCHAR(80),
    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (TRIGGER_NAME,TRIGGER_GROUP)
        REFERENCES QRTZ_EXCL_TRIGGERS(TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE QRTZ_EXCL_BLOB_TRIGGERS
  (
    TRIGGER_NAME VARCHAR(80) NOT NULL,
    TRIGGER_GROUP VARCHAR(80) NOT NULL,
    BLOB_DATA BLOB NULL,
    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (TRIGGER_NAME,TRIGGER_GROUP)
        REFERENCES QRTZ_EXCL_TRIGGERS(TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE QRTZ_EXCL_TRIGGER_LISTENERS
  (
    TRIGGER_NAME  VARCHAR(80) NOT NULL,
    TRIGGER_GROUP VARCHAR(80) NOT NULL,
    TRIGGER_LISTENER VARCHAR(80) NOT NULL,
    PRIMARY KEY (TRIGGER_NAME,TRIGGER_GROUP,TRIGGER_LISTENER),
    FOREIGN KEY (TRIGGER_NAME,TRIGGER_GROUP)
        REFERENCES QRTZ_EXCL_TRIGGERS(TRIGGER_NAME,TRIGGER_GROUP)
);


CREATE TABLE QRTZ_EXCL_CALENDARS
  (
    CALENDAR_NAME  VARCHAR(80) NOT NULL,
    CALENDAR BLOB NOT NULL,
    PRIMARY KEY (CALENDAR_NAME)
);



CREATE TABLE QRTZ_EXCL_PAUSED_TRIGGER_GRPS
  (
    TRIGGER_GROUP  VARCHAR(80) NOT NULL,
    PRIMARY KEY (TRIGGER_GROUP)
);

CREATE TABLE QRTZ_EXCL_FIRED_TRIGGERS
  (
    ENTRY_ID VARCHAR(95) NOT NULL,
    TRIGGER_NAME VARCHAR(80) NOT NULL,
    TRIGGER_GROUP VARCHAR(80) NOT NULL,
    IS_VOLATILE tinyint(1) NOT NULL,
    INSTANCE_NAME VARCHAR(80) NOT NULL,
    FIRED_TIME BIGINT(13) NOT NULL,
    PRIORITY INTEGER NOT NULL,
    STATE VARCHAR(16) NOT NULL,
    JOB_NAME VARCHAR(80) NULL,
    JOB_GROUP VARCHAR(80) NULL,
    IS_STATEFUL tinyint(1) NULL,
    REQUESTS_RECOVERY tinyint(1) NULL,
    PRIMARY KEY (ENTRY_ID)
);

CREATE TABLE QRTZ_EXCL_SCHEDULER_STATE
  (
    INSTANCE_NAME VARCHAR(80) NOT NULL,
    LAST_CHECKIN_TIME BIGINT(13) NOT NULL,
    CHECKIN_INTERVAL BIGINT(13) NOT NULL,
    PRIMARY KEY (INSTANCE_NAME)
);

CREATE TABLE QRTZ_EXCL_LOCKS
  (
    LOCK_NAME  VARCHAR(40) NOT NULL,
    PRIMARY KEY (LOCK_NAME)
);


INSERT INTO QRTZ_EXCL_LOCKS values('TRIGGER_ACCESS');
INSERT INTO QRTZ_EXCL_LOCKS values('JOB_ACCESS');
INSERT INTO QRTZ_EXCL_LOCKS values('CALENDAR_ACCESS');
INSERT INTO QRTZ_EXCL_LOCKS values('STATE_ACCESS');
INSERT INTO QRTZ_EXCL_LOCKS values('MISFIRE_ACCESS');
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
   tagname varchar(255) not null,
   host_id varchar(255),
   user_id varchar(255),
   persona boolean default false,
   mod_date datetime,
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
   version_ts datetime not null,
   primary key (identifier, lang)
);
create table fixes_audit (
   id varchar(36) not null,
   table_name varchar(255),
   action varchar(255),
   records_altered integer,
   datetime datetime,
   primary key (id)
);
create table container_version_info (
   identifier varchar(36) not null,
   working_inode varchar(36) not null,
   live_inode varchar(36),
   deleted bit not null,
   locked_by varchar(100),
   locked_on datetime,
   version_ts datetime not null,
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
create table plugin (
   id varchar(255) not null,
   plugin_name varchar(255) not null,
   plugin_version varchar(255) not null,
   author varchar(255) not null,
   first_deployed_date datetime not null,
   last_deployed_date datetime not null,
   primary key (id)
);
create table mailing_list (
   inode varchar(36) not null,
   title varchar(255),
   public_list tinyint(1),
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
   active tinyint(1),
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
   show_on_menu tinyint(1),
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
   drawed tinyint(1),
   drawed_body longtext,
   add_container_links integer,
   containers_added integer,
   head_code longtext,
   theme varchar(255),
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
   default_structure tinyint(1),
   review_interval varchar(255),
   reviewer_role varchar(255),
   page_detail varchar(36),
   structuretype integer,
   system tinyint(1),
   fixed bit not null,
   velocity_var_name varchar(255),
   url_map_pattern text,
   host varchar(36) not null,
   folder varchar(36) not null,
   expire_date_var varchar(255),
   publish_date_var varchar(255),
   mod_date datetime,
   primary key (inode)
);
create table cms_role (
   id varchar(36) not null,
   role_name varchar(255) not null,
   description longtext,
   role_key varchar(255),
   db_fqn text not null,
   parent varchar(36) not null,
   edit_permissions tinyint(1),
   edit_users tinyint(1),
   edit_layouts tinyint(1),
   locked tinyint(1),
   system tinyint(1),
   primary key (id)
);
create table container_structures (
   id varchar(36) not null,
   container_id varchar(36) not null,
   container_inode varchar(36) not null,
   structure_id varchar(36) not null,
   code longtext,
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
   show_on_menu tinyint(1),
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
   bool1 tinyint(1),
   bool2 tinyint(1),
   bool3 tinyint(1),
   bool4 tinyint(1),
   bool5 tinyint(1),
   bool6 tinyint(1),
   bool7 tinyint(1),
   bool8 tinyint(1),
   bool9 tinyint(1),
   bool10 tinyint(1),
   bool11 tinyint(1),
   bool12 tinyint(1),
   bool13 tinyint(1),
   bool14 tinyint(1),
   bool15 tinyint(1),
   bool16 tinyint(1),
   bool17 tinyint(1),
   bool18 tinyint(1),
   bool19 tinyint(1),
   bool20 tinyint(1),
   bool21 tinyint(1),
   bool22 tinyint(1),
   bool23 tinyint(1),
   bool24 tinyint(1),
   bool25 tinyint(1),
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
create table report_asset (
   inode varchar(36) not null,
   report_name varchar(255) not null,
   report_description text not null,
   requires_input tinyint(1),
   ds varchar(100) not null,
   web_form_report tinyint(1),
   primary key (inode)
);
create table workflow_comment (
   id varchar(36) not null,
   creation_date datetime,
   posted_by varchar(255),
   wf_comment longtext,
   workflowtask_id varchar(36),
   primary key (id)
);
create table category (
   inode varchar(36) not null,
   category_name varchar(255),
   category_key varchar(255),
   sort_order integer,
   active tinyint(1),
   keywords longtext,
   category_velocity_var_name varchar(255),
   mod_date datetime,
   primary key (inode)
);
create table htmlpage (
   inode varchar(36) not null,
   show_on_menu tinyint(1),
   title varchar(255),
   mod_date datetime,
   mod_user varchar(100),
   sort_order integer,
   friendly_name varchar(255),
   metadata longtext,
   start_date datetime,
   end_date datetime,
   page_url varchar(255),
   https_required tinyint(1),
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
   last_mod_date datetime not null,
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
   locked_on datetime,
   version_ts datetime not null,
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
   syspublish_date datetime,
   sysexpire_date datetime,
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
   bot tinyint(1),
   number_of_requests integer,
   host_id varchar(36),
   last_page_id varchar(50),
   first_page_id varchar(50),
   operating_system varchar(50),
   browser_name varchar(50),
   browser_version varchar(50),
   mobile_device tinyint(1),
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
   field_var_name varchar(255),
   mod_date datetime,
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
   show_on_menu tinyint(1),
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
   ignored tinyint(1),
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
   active tinyint(1),
   locked tinyint(1),
   sends_per_hour varchar(15),
   sendemail tinyint(1),
   communicationinode varchar(36),
   userfilterinode varchar(36),
   sendto varchar(15),
   isrecurrent tinyint(1),
   wassent tinyint(1),
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
   locked_on datetime,
   version_ts datetime not null,
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
create table dot_containers (
   inode varchar(36) not null,
   code longtext,
   pre_loop longtext,
   post_loop longtext,
   show_on_menu tinyint(1),
   title varchar(255),
   mod_date datetime,
   mod_user varchar(100),
   sort_order integer,
   friendly_name varchar(255),
   max_contentlets integer,
   use_div tinyint(1),
   staticify tinyint(1),
   sort_contentlets_by varchar(255),
   lucene_query longtext,
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
   locked_on datetime not null,
   version_ts datetime not null,
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
   last_mod_date datetime,
   primary key (id)
);
create table links (
   inode varchar(36) not null,
   show_on_menu tinyint(1),
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
   mail_subscription tinyint(1),
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
   no_click_tracking tinyint(1),
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
   required tinyint(1),
   indexed tinyint(1),
   listed tinyint(1),
   velocity_var_name varchar(255),
   sort_order integer,
   field_values longtext,
   regex_check varchar(255),
   hint varchar(255),
   default_value varchar(255),
   fixed tinyint(1),
   read_only tinyint(1),
   searchable tinyint(1),
   unique_ tinyint(1),
   mod_date datetime,
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
   parent_required tinyint(1),
   child_required tinyint(1),
   fixed tinyint(1),
   primary key (inode)
);
create table folder (
   inode varchar(36) not null,
   name varchar(255),
   title varchar(255) not null,
   show_on_menu tinyint(1),
   sort_order integer,
   files_masks varchar(255),
   identifier varchar(36),
   default_file_type varchar(36),
   mod_date datetime,
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
   last_mod_date datetime,
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
   locked_on datetime,
   version_ts datetime not null,
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
alter table dot_containers add index fk8a844125fb51eb (inode), add constraint fk8a844125fb51eb foreign key (inode) references inode (inode);
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
-- mysql
CREATE INDEX idx_tree ON tree (child, parent, relation_type);
CREATE INDEX idx_tree_1 ON tree (parent);
CREATE INDEX idx_tree_2 ON tree (child);
CREATE INDEX idx_tree_3 ON tree (relation_type);
CREATE INDEX idx_tree_4 ON tree (parent, child, relation_type);
CREATE INDEX idx_tree_5 ON tree (parent, relation_type);
CREATE INDEX idx_tree_6 ON tree (child, relation_type);

CREATE INDEX idx_contentlet_3 ON contentlet (inode);


CREATE INDEX idx_identifier ON identifier (id);
CREATE INDEX idx_permisision_4 ON permission (permission_type);


CREATE INDEX idx_permission_reference_2 ON permission_reference (reference_id);
CREATE INDEX idx_permission_reference_3 ON permission_reference (reference_id,permission_type);
CREATE INDEX idx_permission_reference_4 ON permission_reference (asset_id,permission_type);
CREATE INDEX idx_permission_reference_5 ON permission_reference (asset_id,reference_id,permission_type);
CREATE INDEX idx_permission_reference_6 ON permission_reference (permission_type);

CREATE UNIQUE INDEX idx_field_velocity_structure ON field (velocity_var_name,structure_inode);

alter table tree add index (parent);
alter table tree add index (child);

alter table chain_state add constraint fk_state_chain foreign key (chain_id) references chain(id);
alter table chain_state add constraint fk_state_code foreign key (link_code_id) references chain_link_code(id);
alter table chain_state_parameter add constraint fk_parameter_state foreign key (chain_state_id) references chain_state(id);

alter table permission add constraint permission_role_fk foreign key (roleid) references cms_role(id);

alter table contentlet add constraint FK_structure_inode foreign key (structure_inode) references structure(inode);

ALTER TABLE structure MODIFY fixed tinyint(1) DEFAULT '0' NOT NULL;

ALTER TABLE field MODIFY fixed tinyint(1) DEFAULT '0' NOT NULL;
ALTER TABLE field MODIFY read_only tinyint(1) DEFAULT '1' NOT NULL;

ALTER TABLE campaign MODIFY active tinyint(1) DEFAULT '0' NOT NULL;

insert into User_ (userId, companyId, createDate, password_, passwordEncrypted, passwordReset, firstName, middleName, lastName, male, birthday, emailAddress, skinId, dottedSkins, roundedSkins, greeting, layoutIds, loginDate, failedLoginAttempts, agreedToTermsOfUse, active_) values ('dotcms.org.default', 'default', now(), 'password', '0', '0', '', '', '', '1', '1970-01-01', 'default@dotcms.org', '01', '0', '0', 'Welcome!', '', now(), 0, '0', '1');

create index addres_userid_index on address(userid);
create index tag_user_id_index on tag(user_id);
create index tag_is_persona_index on tag(persona);
create index tag_inode_tagid on tag_inode(tag_id);
create index tag_inode_inode on tag_inode(inode);
CREATE TABLE `dist_journal` (
  `id` BIGINT  NOT NULL AUTO_INCREMENT,
  `object_to_index` VARCHAR(1024)  NOT NULL,
  `serverid` VARCHAR(64)  NOT NULL,
  `journal_type` INTEGER  NOT NULL,
  `time_entered` DATETIME  NOT NULL,
  PRIMARY KEY (`id`)
);
ALTER TABLE dist_journal ADD UNIQUE (object_to_index(255), serverid,journal_type);

create table plugin_property (
   plugin_id varchar(255) not null,
   propkey varchar(255) not null,
   original_value varchar(255) not null,
   current_value varchar(255) not null
);
alter table plugin_property add constraint fk_plugin_plugin_property foreign key (plugin_id) references plugin(id);

CREATE TABLE `dist_process` (`id` BIGINT  NOT NULL AUTO_INCREMENT,`object_to_index` VARCHAR(1024)  NOT NULL,`serverid` VARCHAR(64)  NOT NULL,`journal_type` INTEGER  NOT NULL,`time_entered` DATETIME  NOT NULL, PRIMARY KEY (`id`));
CREATE INDEX dist_process_index USING BTREE on dist_process (object_to_index (255), serverid,journal_type);

CREATE TABLE `dist_reindex_journal` (`id` BIGINT  NOT NULL AUTO_INCREMENT,`inode_to_index` VARCHAR(100)  NOT NULL,`ident_to_index` VARCHAR(100)  NOT NULL,`serverid` VARCHAR(64),`priority` INTEGER  NOT NULL,`time_entered` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, index_val varchar(325), dist_action integer NOT NULL DEFAULT 1, PRIMARY KEY (`id`));

CREATE INDEX dist_reindex_index1 USING BTREE on dist_reindex_journal (inode_to_index (100));
CREATE INDEX dist_reindex_index2 USING BTREE on dist_reindex_journal (dist_action);
CREATE INDEX dist_reindex_index3 USING BTREE on dist_reindex_journal (serverid);
CREATE INDEX dist_reindex_index4 USING BTREE on dist_reindex_journal (ident_to_index,serverid);
CREATE INDEX dist_reindex_index  USING BTREE on dist_reindex_journal (serverid,dist_action);
CREATE INDEX dist_reindex_index5 USING BTREE ON dist_reindex_journal (priority, time_entered);
CREATE INDEX dist_reindex_index6 USING BTREE ON dist_reindex_journal (priority);

CREATE TABLE `quartz_log` (`id` BIGINT  NOT NULL AUTO_INCREMENT,`JOB_NAME` VARCHAR(255)  NOT NULL,`serverid` VARCHAR(64) ,`time_started` DATETIME  NOT NULL, PRIMARY KEY (`id`));


ALTER TABLE cms_role ADD UNIQUE (role_key);

alter table cms_role add constraint fkcms_role_parent foreign key (parent) references cms_role (id) ON DELETE CASCADE;


ALTER TABLE cms_layout ADD UNIQUE (layout_name);
ALTER TABLE portlet ADD UNIQUE (portletid);
ALTER TABLE cms_layouts_portlets ADD UNIQUE (portlet_id, layout_id);
alter table cms_layouts_portlets add constraint fkcms_layouts_portlets foreign key (layout_id) references cms_layout(id);

ALTER TABLE users_cms_roles ADD UNIQUE (role_id, user_id);
alter table users_cms_roles add constraint fkusers_cms_roles1 foreign key (role_id) references cms_role (id);
alter table users_cms_roles add constraint fkusers_cms_roles2 foreign key (user_id) references user_ (userid);

ALTER TABLE layouts_cms_roles ADD UNIQUE (role_id, layout_id);
alter table layouts_cms_roles add constraint fklayouts_cms_roles1 foreign key (role_id) references cms_role (id);
alter table layouts_cms_roles add constraint fklayouts_cms_roles2 foreign key (layout_id) references cms_layout (id);

ALTER TABLE dot_containers add constraint containers_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE template add constraint template_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE htmlpage add constraint htmlpage_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE file_asset add constraint file_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE contentlet add constraint content_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE links add constraint links_identifier_fk foreign key (identifier) references identifier(id);


create table dist_reindex_lock (dummy int);
create table dist_lock (dummy int);

create table import_audit (
	id bigint not null,
	start_date timestamp,
	userid varchar(255),
	filename varchar(512),
	status int,
	last_inode varchar(100),
	records_to_import bigint,
	serverid varchar(255),
	primary key (id)
	);

alter table category modify column category_velocity_var_name varchar(255) not null;

alter table import_audit add column warnings text,
	add column errors text,
	add column results text,
	add column messages text;

alter table structure modify host varchar(100) default 'SYSTEM_HOST' not null;
alter table structure modify folder varchar(100) default 'SYSTEM_FOLDER' not null;
alter table structure add constraint fk_structure_folder foreign key (folder) references folder(inode);
alter table structure modify column velocity_var_name varchar(255) not null;
alter table structure add constraint unique_struct_vel_var_name unique (velocity_var_name);

DROP PROCEDURE IF EXISTS load_records_to_index;
CREATE PROCEDURE load_records_to_index(IN server_id VARCHAR(100), IN records_to_fetch INT, IN priority_level INT)
BEGIN
DECLARE v_id BIGINT;
DECLARE v_inode_to_index VARCHAR(100);
DECLARE v_ident_to_index VARCHAR(100);
DECLARE v_serverid VARCHAR(64);
DECLARE v_priority INT;
DECLARE v_time_entered TIMESTAMP;
DECLARE v_index_val VARCHAR(325);
DECLARE v_dist_action INT;
DECLARE cursor_end BOOL DEFAULT FALSE;
DECLARE cur1 CURSOR FOR SELECT * FROM dist_reindex_journal WHERE serverid IS NULL or serverid='' AND priority <= priority_level ORDER BY priority ASC LIMIT records_to_fetch FOR UPDATE;
DECLARE CONTINUE HANDLER FOR NOT FOUND SET cursor_end:=TRUE;

DROP TEMPORARY TABLE IF EXISTS tmp_records_reindex;
CREATE TEMPORARY TABLE tmp_records_reindex (
  id BIGINT PRIMARY KEY,
  inode_to_index varchar(36),
  ident_to_index varchar(36),
  dist_action INT,
  priority INT
) ENGINE=MEMORY;

OPEN cur1;
WHILE (NOT cursor_end) DO
  FETCH cur1 INTO v_id,v_inode_to_index,v_ident_to_index,v_serverid,v_priority,v_time_entered,v_index_val,v_dist_action;
  IF (NOT cursor_end) THEN
    UPDATE dist_reindex_journal SET serverid=server_id WHERE id=v_id;
    INSERT INTO tmp_records_reindex VALUES (v_id, v_inode_to_index, v_ident_to_index, v_dist_action, v_priority);
  END IF;
END WHILE;
CLOSE cur1;

SELECT * FROM tmp_records_reindex;

END;
#
DROP TRIGGER IF EXISTS check_parent_path_when_update;
CREATE TRIGGER check_parent_path_when_update  BEFORE UPDATE
on identifier
FOR EACH ROW
BEGIN
DECLARE idCount INT;
DECLARE canUpdate boolean default false;
 IF @disable_trigger IS NULL THEN
   select count(id)into idCount from identifier where asset_type='folder' and CONCAT(parent_path,asset_name,'/')= NEW.parent_path and host_inode = NEW.host_inode and id <> NEW.id;
   IF(idCount > 0 OR NEW.parent_path = '/' OR NEW.parent_path = '/System folder') THEN
     SET canUpdate := TRUE;
   END IF;
   IF(canUpdate = FALSE) THEN
     delete from Cannot_update_for_this_path_does_not_exist_for_the_given_host;
   END IF;
 END IF;
END
#
DROP TRIGGER IF EXISTS check_parent_path_when_insert;
CREATE TRIGGER check_parent_path_when_insert  BEFORE INSERT
on identifier
FOR EACH ROW
BEGIN
DECLARE idCount INT;
DECLARE canInsert boolean default false;
 select count(id)into idCount from identifier where asset_type='folder' and CONCAT(parent_path,asset_name,'/')= NEW.parent_path and host_inode = NEW.host_inode and id <> NEW.id;
 IF(idCount > 0 OR NEW.parent_path = '/' OR NEW.parent_path = '/System folder') THEN
   SET canInsert := TRUE;
 END IF;
 IF(canInsert = FALSE) THEN
  delete from Cannot_insert_for_this_path_does_not_exist_for_the_given_host;
 END IF;
END
#
DROP PROCEDURE IF EXISTS checkVersions;
CREATE PROCEDURE checkVersions(IN ident VARCHAR(100),IN tableName VARCHAR(20), OUT versionsCount INT)
BEGIN
  SET versionsCount := 0;
  IF(tableName = 'htmlpage') THEN
    select count(inode) into versionsCount from htmlpage where identifier = ident;
  END IF;
  IF(tableName = 'file_asset') THEN
    select count(inode) into versionsCount from file_asset where identifier = ident;
  END IF;
  IF(tableName = 'links') THEN
    select count(inode) into versionsCount from links where identifier = ident;
  END IF;
  IF(tableName = 'dot_containers') THEN
    select count(inode) into versionsCount from dot_containers where identifier = ident;
  END IF;
  IF(tableName = 'template') THEN
    select count(inode) into versionsCount from template where identifier = ident;
  END IF;
  IF(tableName = 'contentlet') THEN
    select count(inode) into versionsCount from contentlet where identifier = ident;
  END IF;
  IF(tableName = 'folder') THEN
    select count(inode) into versionsCount from folder where identifier = ident;
  END IF;
END
#

DROP TRIGGER IF EXISTS check_htmlpage_versions;
CREATE TRIGGER check_htmlpage_versions AFTER DELETE
on htmlpage
FOR EACH ROW
BEGIN
DECLARE tableName VARCHAR(20);
DECLARE count INT;
SET tableName = 'htmlpage';
CALL checkVersions(OLD.identifier,tableName,count);
IF(count = 0)THEN
 delete from identifier where id = OLD.identifier;
END IF;
END
#

DROP TRIGGER IF EXISTS check_file_versions;
CREATE TRIGGER check_file_versions AFTER DELETE
on file_asset
FOR EACH ROW
BEGIN
DECLARE tableName VARCHAR(20);
DECLARE count INT;
SET tableName = 'file_asset';
CALL checkVersions(OLD.identifier,tableName,count);
IF(count = 0)THEN
 delete from identifier where id = OLD.identifier;
END IF;
END
#
DROP TRIGGER IF EXISTS check_links_versions;
CREATE TRIGGER check_links_versions AFTER DELETE
on links
FOR EACH ROW
BEGIN
DECLARE tableName VARCHAR(20);
DECLARE count INT;
SET tableName = 'links';
CALL checkVersions(OLD.identifier,tableName,count);
IF(count = 0)THEN
 delete from identifier where id = OLD.identifier;
END IF;
END
#
DROP TRIGGER IF EXISTS check_container_versions;
CREATE TRIGGER check_container_versions AFTER DELETE
on dot_containers
FOR EACH ROW
BEGIN
DECLARE tableName VARCHAR(20);
DECLARE count INT;
SET tableName = 'dot_containers';
CALL checkVersions(OLD.identifier,tableName,count);
IF(count = 0)THEN
 delete from identifier where id = OLD.identifier;
END IF;
END
#
DROP TRIGGER IF EXISTS check_template_versions;
CREATE TRIGGER check_template_versions AFTER DELETE
on template
FOR EACH ROW
BEGIN
DECLARE tableName VARCHAR(20);
DECLARE count INT;
SET tableName = 'template';
CALL checkVersions(OLD.identifier,tableName,count);
IF(count = 0)THEN
 delete from identifier where id = OLD.identifier;
END IF;
END
#
DROP TRIGGER IF EXISTS check_content_versions;
CREATE TRIGGER check_content_versions AFTER DELETE
on contentlet
FOR EACH ROW
BEGIN
DECLARE tableName VARCHAR(20);
DECLARE count INT;
SET tableName = 'contentlet';
CALL checkVersions(OLD.identifier,tableName,count);
IF(count = 0)THEN
 delete from identifier where id = OLD.identifier;
END IF;
END
#
alter table structure add constraint fk_structure_host foreign key (host) references identifier(id);

create index idx_template3 on template (title);

CREATE INDEX idx_contentlet_4 ON contentlet (structure_inode);

CREATE INDEX idx_contentlet_identifier ON contentlet (identifier);

ALTER TABLE Folder add constraint folder_identifier_fk foreign key (identifier) references identifier(id);
--ALTER TABLE dot_containers add constraint structure_fk foreign key (structure_inode) references structure(inode);
ALTER TABLE htmlpage add constraint template_id_fk foreign key (template_id) references identifier(id);

DROP TRIGGER IF EXISTS check_templateId_when_insert;
CREATE TRIGGER check_templateId_when_insert BEFORE INSERT
on htmlpage
FOR EACH ROW
BEGIN
DECLARE identCount INT;
select count(id) into identCount from identifier where id = NEW.template_id and asset_type='template';
IF(identCount = 0) THEN
delete from Template_Id_should_be_the_identifier_of_a_template;
END IF;
END
#
DROP TRIGGER IF EXISTS check_templateId_when_update;
CREATE TRIGGER check_templateId_when_update  BEFORE UPDATE
on htmlpage
FOR EACH ROW
BEGIN
DECLARE identCount INT;
select count(id)into identCount from identifier where id = NEW.template_id and asset_type='template';
IF(identCount = 0) THEN
delete from Template_Id_should_be_the_identifier_of_a_template;
END IF;
END
#
DROP TRIGGER IF EXISTS folder_identifier_check;
CREATE TRIGGER folder_identifier_check AFTER DELETE
on folder
FOR EACH ROW
BEGIN
DECLARE tableName VARCHAR(20);
DECLARE count INT;
SET tableName = 'folder';
CALL checkVersions(OLD.identifier,tableName,count);
IF(count = 0)THEN
delete from identifier where id = OLD.identifier;
END IF;
END
#
alter table contentlet add constraint fk_user_contentlet foreign key (mod_user) references user_(userid);
alter table htmlpage add constraint fk_user_htmlpage foreign key (mod_user) references user_(userid);
alter table dot_containers add constraint fk_user_containers foreign key (mod_user) references user_(userid);
alter table template add constraint fk_user_template foreign key (mod_user) references user_(userid);
alter table file_asset add constraint fk_user_file_asset foreign key (mod_user) references user_(userid);
alter table links add constraint fk_user_links foreign key (mod_user) references user_(userid);

create index idx_template_id on template_containers(template_id);
alter table template_containers add constraint FK_template_id foreign key (template_id) references identifier(id);
alter table template_containers add constraint FK_container_id foreign key (container_id) references identifier(id);

DROP TRIGGER IF EXISTS check_child_assets;
CREATE TRIGGER check_child_assets BEFORE DELETE
ON IDENTIFIER
FOR EACH ROW
BEGIN
  DECLARE pathCount INT;
    IF(OLD.asset_type ='folder') THEN
      select count(*) into pathCount from identifier where parent_path = CONCAT(OLD.parent_path,OLD.asset_name,'/') and host_inode = OLD.host_inode;
    END IF;
    IF(OLD.asset_type ='contentlet') THEN
	 select count(*) into pathCount from identifier where host_inode = OLD.id;
    END IF;
 IF(pathCount > 0) THEN
   delete from Cannot_delete_as_this_path_has_children;
 END IF;
END
#
DROP PROCEDURE IF EXISTS renameFolderChildren;
CREATE PROCEDURE renameFolderChildren(IN old_path varchar(255),IN new_path varchar(255),IN hostInode varchar(100))
BEGIN
 DECLARE new_folder_path varchar(255);
 DECLARE old_folder_path varchar(255);
 DECLARE assetName varchar(255);
 DECLARE no_more_rows boolean;
 DECLARE cur1 CURSOR FOR select asset_name from identifier where asset_type='folder' and parent_path = new_path and host_inode = hostInode;
 DECLARE CONTINUE HANDLER FOR NOT FOUND
 SET no_more_rows := TRUE;
 SET max_sp_recursion_depth=255;
 SET @disable_trigger = 1;
 UPDATE identifier SET  parent_path  = new_path where parent_path = old_path and host_inode = hostInode;
 SET @disable_trigger = NULL;
 OPEN cur1;
 cur1_loop:LOOP
 FETCH cur1 INTO assetName;
 IF no_more_rows THEN
   LEAVE cur1_loop;
 END IF;
 select CONCAT(new_path,assetName,'/')INTO new_folder_path;
 select CONCAT(old_path,assetName,'/')INTO old_folder_path;
 CALL renameFolderChildren(old_folder_path,new_folder_path,hostInode);
END LOOP;
CLOSE cur1;
END
#
DROP TRIGGER IF EXISTS rename_folder_assets_trigger;
CREATE TRIGGER rename_folder_assets_trigger AFTER UPDATE
on Folder
FOR EACH ROW
BEGIN
DECLARE old_parent_path varchar(255);
DECLARE old_path varchar(255);
DECLARE new_path varchar(255);
DECLARE old_name varchar(255);
DECLARE hostInode varchar(100);
IF @disable_trigger IS NULL AND NEW.name<>OLD.name THEN
	select asset_name,parent_path,host_inode INTO old_name,old_parent_path,hostInode from identifier where id = NEW.identifier;
	SELECT CONCAT(old_parent_path,old_name,'/')INTO old_path;
	SELECT CONCAT(old_parent_path,NEW.name,'/')INTO new_path;
	SET @disable_trigger = 1;
	UPDATE identifier SET asset_name = NEW.name where id = NEW.identifier;
	SET @disable_trigger = NULL;
	CALL renameFolderChildren(old_path,new_path,hostInode);
END IF;
END
#

DROP FUNCTION IF EXISTS dotFolderPath;
CREATE FUNCTION dotFolderPath (parent_path char(255), asset_name char(255)) RETURNS char(255)
DETERMINISTIC
BEGIN
IF (parent_path='/System folder') THEN
  RETURN '/';
ELSE
  RETURN CONCAT(parent_path,asset_name,'/');
END IF;
END
#
alter table contentlet_version_info add constraint fk_contentlet_version_info_identifier foreign key (identifier) references identifier(id) on delete cascade;
alter table container_version_info  add constraint fk_container_version_info_identifier  foreign key (identifier) references identifier(id);
alter table template_version_info   add constraint fk_template_version_info_identifier   foreign key (identifier) references identifier(id);
alter table htmlpage_version_info   add constraint fk_htmlpage_version_info_identifier   foreign key (identifier) references identifier(id);
alter table fileasset_version_info  add constraint fk_fileasset_version_info_identifier  foreign key (identifier) references identifier(id);
alter table link_version_info       add constraint fk_link_version_info_identifier       foreign key (identifier) references identifier(id);

alter table contentlet_version_info add constraint fk_contentlet_version_info_working foreign key (working_inode) references contentlet(inode);
alter table container_version_info  add constraint fk_container_version_info_working  foreign key (working_inode) references dot_containers(inode);
alter table template_version_info   add constraint fk_template_version_info_working   foreign key (working_inode) references template(inode);
alter table htmlpage_version_info   add constraint fk_htmlpage_version_info_working   foreign key (working_inode) references htmlpage(inode);
alter table fileasset_version_info  add constraint fk_fileasset_version_info_working  foreign key (working_inode) references file_asset(inode);
alter table link_version_info       add constraint fk_link_version_info_working       foreign key (working_inode) references links(inode);

alter table contentlet_version_info add constraint fk_contentlet_version_info_live foreign key (live_inode) references contentlet(inode);
alter table container_version_info  add constraint fk_container_version_info_live  foreign key (live_inode) references dot_containers(inode);
alter table template_version_info   add constraint fk_template_version_info_live   foreign key (live_inode) references template(inode);
alter table htmlpage_version_info   add constraint fk_htmlpage_version_info_live   foreign key (live_inode) references htmlpage(inode);
alter table fileasset_version_info  add constraint fk_fileasset_version_info_live  foreign key (live_inode) references file_asset(inode);
alter table link_version_info       add constraint fk_link_version_info_live       foreign key (live_inode) references links(inode);

alter table contentlet_version_info add constraint fk_contentlet_version_info_lang foreign key (lang) references language(id);

alter table folder add constraint fk_folder_file_structure_type foreign key(default_file_type) references structure(inode);

alter table workflowtask_files add constraint FK_workflow_id foreign key (workflowtask_id) references workflow_task(id);
-- alter table workflowtask_files add constraint FK_task_file_inode foreign key (file_inode) references file_asset(inode);
alter table workflow_comment add constraint workflowtask_id_comment_FK foreign key (workflowtask_id) references workflow_task(id);
alter table workflow_history add constraint workflowtask_id_history_FK foreign key (workflowtask_id) references workflow_task(id);

alter table contentlet add constraint fk_contentlet_lang foreign key (language_id) references language(id);

create table workflow_scheme(
    id varchar(36) primary key,
    name varchar(255) not null,
    description varchar(255),
    archived boolean default false,
    mandatory boolean default false,
    default_scheme boolean default false,
    entry_action_id varchar(36),
    mod_date datetime
);
alter table workflow_scheme add constraint unique_workflow_scheme_name unique (name);

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
    step_id varchar(36) not null  references workflow_step(id),
    name varchar(255) not null,
    condition_to_progress text,
    next_step_id varchar(36) not null references workflow_step(id),
    next_assign varchar(36) not null references cms_role(id),
    my_order int default 0,
    assignable boolean default false,
    commentable boolean default false,
    requires_checkout boolean default false,
    icon varchar(255) default 'defaultWfIcon',
    use_role_hierarchy_assign bool default false
);
create index workflow_idx_action_step on workflow_action(step_id);


create table workflow_action_class(
    id varchar(36) primary key,
    action_id varchar(36) references workflow_action(id),
    name varchar(255) not null,
    my_order int default 0,
    clazz text
);
create index workflow_idx_action_class_action on workflow_action_class(action_id);
SET sql_mode='ANSI_QUOTES';
create table workflow_action_class_pars(
    id varchar(36) primary key,
    workflow_action_class_id char(36) references workflow_action_class(id),
    "key" varchar(255) not null,
    value text
);
create index workflow_idx_action_class_param_action on
    workflow_action_class_pars(workflow_action_class_id);


create table workflow_scheme_x_structure(
    id varchar(36) primary key,
    scheme_id varchar(36) not null references workflow_scheme(id),
    structure_id varchar(36) not null references structure(inode)
);
create index workflow_idx_scheme_structure_1 on
    workflow_scheme_x_structure(structure_id);

create unique index workflow_idx_scheme_structure_2 on
    workflow_scheme_x_structure(structure_id);

alter table workflow_step add constraint fk_escalation_action foreign key (escalation_action) references workflow_action(id);

alter table contentlet_version_info add constraint FK_con_ver_lockedby foreign key (locked_by) references user_(userid);
alter table container_version_info  add constraint FK_tainer_ver_info_lockedby  foreign key (locked_by) references user_(userid);
alter table template_version_info   add constraint FK_temp_ver_info_lockedby   foreign key (locked_by) references user_(userid);
alter table htmlpage_version_info   add constraint FK_page_ver_info_lockedby   foreign key (locked_by) references user_(userid);
alter table fileasset_version_info  add constraint FK_fil_ver_info_lockedby  foreign key (locked_by) references user_(userid);
alter table link_version_info       add constraint FK_link_ver_info_lockedby       foreign key (locked_by) references user_(userid);

ALTER TABLE tag ALTER COLUMN host_id set default 'SYSTEM_HOST';
alter table tag add constraint tag_tagname_host unique (tagname, host_id);
alter table tag_inode add constraint fk_tag_inode_tagid foreign key (tag_id) references tag (tag_id);

-- ****** Indicies Data Storage *******
create table indicies (
  index_name varchar(30) primary key,
  index_type varchar(16) not null unique
);

-- ****** Log Console Table *******
  CREATE TABLE log_mapper (
    enabled   	 tinyint(1) not null,
    log_name 	 varchar(30) not null,
    description  varchar(50) not null,
    primary key (log_name)
  );

  insert into log_mapper (ENABLED,LOG_NAME,DESCRIPTION) values ('1','dotcms-userActivity.log','Log Users action on pages, structures, documents.');
  insert into log_mapper (ENABLED,LOG_NAME,DESCRIPTION) values ('1','dotcms-security.log','Log users login activity into dotCMS.');
  insert into log_mapper (ENABLED,LOG_NAME,DESCRIPTION) values ('1','dotcms-adminaudit.log','Log Admin activity on dotCMS.');
  insert into log_mapper (ENABLED,LOG_NAME,DESCRIPTION) values ('1','dotcms-pushpublish.log','Log Push Publishing activity on dotCMS.');

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
create table publishing_queue (id BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL, operation bigint, asset VARCHAR(2000) NOT NULL, language_id bigint NOT NULL,
entered_date DATETIME,last_try DATETIME, num_of_tries bigint NOT NULL DEFAULT 0, in_error tinyint(1) DEFAULT '0', last_results LONGTEXT,
publish_date DATETIME, server_id VARCHAR(256),
type VARCHAR(256), bundle_id VARCHAR(256) , target text);

CREATE TABLE IF NOT EXISTS publishing_queue_audit (
	bundle_id VARCHAR(36) PRIMARY KEY NOT NULL,
	status INTEGER,
	status_pojo LONGTEXT,
	status_updated DATETIME,
	create_date DATETIME);

-- ****** Content Publishing Framework - End Point Management *******
CREATE TABLE IF NOT EXISTS publishing_end_point (
	id varchar(36) PRIMARY KEY,
	group_id varchar(700),
	server_name varchar(255) unique,
	address varchar(250),
	port varchar(10),
	protocol varchar(10),
	enabled tinyint,
	auth_key text,
	sending tinyint
);

create table publishing_environment(
	id varchar(36) NOT NULL  primary key,
	name varchar(255) NOT NULL unique,
	push_to_all bool NOT NULL
);

create table sitesearch_audit (
    job_id varchar(36),
    job_name varchar(255) not null,
    fire_date datetime not null,
    incremental tinyint not null,
    start_date datetime,
    end_date datetime,
    host_list varchar(500) not null,
    all_hosts tinyint not null,
    lang_list varchar(500) not null,
    path varchar(500) not null,
    path_include tinyint not null,
    files_count integer not null,
    pages_count integer not null,
    urlmaps_count integer not null,
    index_name varchar(100) not null,
    primary key(job_id,fire_date)
);

drop table publishing_queue;

CREATE TABLE publishing_queue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL,
    operation bigint,
    asset VARCHAR(2000) NOT NULL,
    language_id bigint NOT NULL,
    entered_date DATETIME,
    publish_date DATETIME,
    type VARCHAR(256),
    bundle_id VARCHAR(256)
);

create table publishing_bundle(
	  id varchar(36) NOT NULL  primary key,
	  name varchar(255) NOT NULL,
	  publish_date DATETIME,
	  expire_date DATETIME,
	  owner varchar(100)
);

create table publishing_bundle_environment(id varchar(36) NOT NULL primary key,bundle_id varchar(36) NOT NULL, environment_id varchar(36) NOT NULL);

alter table publishing_bundle_environment add constraint FK_bundle_id foreign key (bundle_id) references publishing_bundle(id);
alter table publishing_bundle_environment add constraint FK_environment_id foreign key (environment_id) references publishing_environment(id);

create table publishing_pushed_assets(
	bundle_id varchar(36) NOT NULL,
	asset_id varchar(36) NOT NULL,
	asset_type varchar(255) NOT NULL,
	push_date DATETIME,
	environment_id varchar(36) NOT NULL
);

CREATE INDEX idx_pushed_assets_1 ON publishing_pushed_assets (bundle_id);
CREATE INDEX idx_pushed_assets_2 ON publishing_pushed_assets (environment_id);
CREATE INDEX idx_pushed_assets_3 ON publishing_pushed_assets (asset_id, environment_id);

CREATE INDEX idx_pub_qa_1 ON publishing_queue_audit (status);


alter table publishing_bundle add force_push tinyint(1) ;

-- Cluster Tables

CREATE TABLE dot_cluster(cluster_id varchar(36), PRIMARY KEY (cluster_id) );
CREATE TABLE cluster_server(server_id varchar(36), cluster_id varchar(36) NOT NULL, name varchar(100), ip_address varchar(39) NOT NULL, host varchar(36), cache_port SMALLINT, es_transport_tcp_port SMALLINT, es_network_port SMALLINT, es_http_port SMALLINT, key_ varchar(100), PRIMARY KEY (server_id) );
ALTER TABLE cluster_server add constraint fk_cluster_id foreign key (cluster_id) REFERENCES dot_cluster(cluster_id);
CREATE TABLE cluster_server_uptime(id varchar(36),server_id varchar(36) NOT NULL, startup datetime, heartbeat datetime, PRIMARY KEY (id)) ;
ALTER TABLE cluster_server_uptime add constraint fk_cluster_server_id foreign key (server_id) REFERENCES cluster_server(server_id);


-- Notifications Table
create table notification(id varchar(36) NOT NULL,message text NOT NULL, notification_type varchar(100), notification_level varchar(100), user_id varchar(255) NOT NULL, time_sent DATETIME NOT NULL, was_read bit default 0, PRIMARY KEY (id));
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
create table sitelic(id varchar(36) primary key, serverid varchar(100), license longtext not null, lastping datetime not null);

-- Integrity Checker
create table folders_ir(folder varchar(255), local_inode varchar(36), remote_inode varchar(36), local_identifier varchar(36), remote_identifier varchar(36), endpoint_id varchar(36), PRIMARY KEY (local_inode, endpoint_id));
create table structures_ir(velocity_name varchar(255), local_inode varchar(36), remote_inode varchar(36), endpoint_id varchar(36), PRIMARY KEY (local_inode, endpoint_id));
create table schemes_ir(name varchar(255), local_inode varchar(36), remote_inode varchar(36), endpoint_id varchar(36), PRIMARY KEY (local_inode, endpoint_id));
create table htmlpages_ir(html_page varchar(255), local_working_inode varchar(36), local_live_inode varchar(36), remote_working_inode varchar(36), remote_live_inode varchar(36),local_identifier varchar(36), remote_identifier varchar(36), endpoint_id varchar(36), language_id bigint, PRIMARY KEY (local_working_inode, language_id, endpoint_id));
create table fileassets_ir(file_name varchar(255), local_working_inode varchar(36), local_live_inode varchar(36), remote_working_inode varchar(36), remote_live_inode varchar(36),local_identifier varchar(36), remote_identifier varchar(36), endpoint_id varchar(36), language_id bigint, PRIMARY KEY (local_working_inode, language_id, endpoint_id));

---Server Action
create table cluster_server_action(
	server_action_id varchar(36) not null, 
	originator_id varchar(36) not null, 
	server_id varchar(36) not null, 
	failed boolean default false, 
	response varchar(2048), 
	action_id varchar(1024) not null,
	completed boolean default false, 
	entered_date datetime not null,
	time_out_seconds bigint not null,
	PRIMARY KEY (server_action_id)
);

-- Rules Engine
create table dot_rule(id varchar(36) primary key,name varchar(255) not null,fire_on varchar(20),short_circuit boolean,parent_id varchar(36) not null,folder varchar(36) not null,priority int default 0,enabled boolean default false,mod_date datetime);
create table rule_condition_group(id varchar(36) primary key,rule_id varchar(36) references dot_rule(id),operator varchar(10) not null,priority int default 0,mod_date datetime);
create table rule_condition(id varchar(36) primary key,conditionlet text not null,condition_group varchar(36) references rule_condition_group(id),comparison varchar(36) not null,operator varchar(10) not null,priority int default 0,mod_date datetime);
create table rule_condition_value (id varchar(36) primary key,
	condition_id varchar(36) references rule_condition(id),paramkey VARCHAR(255) NOT NULL,value text,priority int default 0);
create table rule_action (id varchar(36) primary key,rule_id varchar(36) references dot_rule(id),priority int default 0,actionlet text not null,mod_date datetime);
create table rule_action_pars(id varchar(36) primary key,rule_action_id varchar(36) references rule_action(id), paramkey varchar(255) not null,value text);
create index idx_rules_fire_on on dot_rule (fire_on);

-- Delete User
ALTER TABLE user_ ADD delete_in_progress BOOLEAN DEFAULT FALSE;
ALTER TABLE user_ ADD delete_date DATETIME;

