SET TRANSACTION ISOLATION LEVEL READ COMMITTED;

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
	priority int
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
	autoLogin bit,
	strangers bit
);

create table Counter (
	name varchar(100) not null primary key,
	currentId int
);

create table Image (
	imageId varchar(200) not null primary key,
	text_ text not null
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
	description varchar(1000) null,
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
	description varchar(1000) null,
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
	defaultPreferences text null,
	narrow bit,
	roles varchar(1000) null,
	active_ bit,
	primary key (portletId, groupId, companyId)
);

create table PortletPreferences (
	portletId varchar(100) not null,
	userId varchar(100) not null,
	layoutId varchar(100) not null,
	preferences text null,
	primary key (portletId, userId, layoutId)
);

create table Release_ (
	releaseId varchar(100) not null primary key,
	createDate datetime null,
	modifiedDate datetime null,
	buildNumber int null,
	buildDate datetime null
);

create table User_ (
	userId varchar(100) not null primary key,
	companyId varchar(100) not null,
	createDate datetime null,
	password_ text null,
	passwordEncrypted bit,
	passwordExpirationDate datetime null,
	passwordReset bit,
	firstName varchar(100) null,
	middleName varchar(100) null,
	lastName varchar(100) null,
	nickName varchar(100) null,
	male bit,
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
	dottedSkins bit,
	roundedSkins bit,
	greeting varchar(100) null,
	resolution varchar(100) null,
	refreshRate varchar(100) null,
	layoutIds varchar(100) null,
	comments varchar(1000) null,
	loginDate datetime null,
	loginIP varchar(100) null,
	lastLoginDate datetime null,
	lastLoginIP varchar(100) null,
	failedLoginAttempts int,
	agreedToTermsOfUse bit,
	active_ bit
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
	path varchar(1000) not null,
	pathDate datetime not null
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
insert into PollsQuestion (questionId, portletId, groupId, companyId, userId, userName, createDate, modifiedDate, title, description) values ('1', '25', '-1', 'liferay.com', 'liferay.com.1', 'John Wayne', GetDate(), GetDate(), 'What is your favorite ice cream flavor?', 'What is your favorite ice cream flavor?');

--
-- Default User
--

insert into User_ (userId, companyId, createDate, password_, passwordEncrypted, passwordReset, firstName, middleName, lastName, male, birthday, emailAddress, skinId, dottedSkins, roundedSkins, greeting, layoutIds, loginDate, failedLoginAttempts, agreedToTermsOfUse, active_) values ('liferay.com.default', 'default', GetDate(), 'password', '0', '0', '', '', '', '1', '19700101', 'default@liferay.com', '01', '0', '0', 'Welcome!', '', GetDate(), 0, '0', '1');

--
-- Test User
--

insert into User_ (userId, companyId, createDate, password_, passwordEncrypted, passwordReset, firstName, middleName, lastName, nickName, male, birthday, emailAddress, skinId, dottedSkins, roundedSkins, greeting, layoutIds, loginDate, failedLoginAttempts, agreedToTermsOfUse, active_) values ('liferay.com.1', 'liferay.com', GetDate(), 'test', '0', '0', 'John', '', 'Wayne', 'Duke', '1', '19700101', 'test@liferay.com', '01', '0', '1', 'Welcome John Wayne!', '1,', GetDate(), 0, '1', '1');

CREATE TABLE [dbo].[QRTZ_CALENDARS] (
  [CALENDAR_NAME] [VARCHAR] (80)  NOT NULL ,
  [CALENDAR] [IMAGE] NOT NULL
)
GO

CREATE TABLE [dbo].[QRTZ_CRON_TRIGGERS] (
  [TRIGGER_NAME] [VARCHAR] (80)  NOT NULL ,
  [TRIGGER_GROUP] [VARCHAR] (80)  NOT NULL ,
  [CRON_EXPRESSION] [VARCHAR] (80)  NOT NULL ,
  [TIME_ZONE_ID] [VARCHAR] (80)
)
GO

CREATE TABLE [dbo].[QRTZ_FIRED_TRIGGERS] (
  [ENTRY_ID] [VARCHAR] (95)  NOT NULL ,
  [TRIGGER_NAME] [VARCHAR] (80)  NOT NULL ,
  [TRIGGER_GROUP] [VARCHAR] (80)  NOT NULL ,
  [IS_VOLATILE] [VARCHAR] (1)  NOT NULL ,
  [INSTANCE_NAME] [VARCHAR] (80)  NOT NULL ,
  [FIRED_TIME] [BIGINT] NOT NULL ,
  [PRIORITY] [INTEGER] NOT NULL ,
  [STATE] [VARCHAR] (16)  NOT NULL,
  [JOB_NAME] [VARCHAR] (80)  NULL ,
  [JOB_GROUP] [VARCHAR] (80)  NULL ,
  [IS_STATEFUL] [VARCHAR] (1)  NULL ,
  [REQUESTS_RECOVERY] [VARCHAR] (1)  NULL
)
GO

CREATE TABLE [dbo].[QRTZ_PAUSED_TRIGGER_GRPS] (
  [TRIGGER_GROUP] [VARCHAR] (80)  NOT NULL
)
GO

CREATE TABLE [dbo].[QRTZ_SCHEDULER_STATE] (
  [INSTANCE_NAME] [VARCHAR] (80)  NOT NULL ,
  [LAST_CHECKIN_TIME] [BIGINT] NOT NULL ,
  [CHECKIN_INTERVAL] [BIGINT] NOT NULL
)
GO

CREATE TABLE [dbo].[QRTZ_LOCKS] (
  [LOCK_NAME] [VARCHAR] (40)  NOT NULL
)
GO

CREATE TABLE [dbo].[QRTZ_JOB_DETAILS] (
  [JOB_NAME] [VARCHAR] (80)  NOT NULL ,
  [JOB_GROUP] [VARCHAR] (80)  NOT NULL ,
  [DESCRIPTION] [VARCHAR] (120) NULL ,
  [JOB_CLASS_NAME] [VARCHAR] (128)  NOT NULL ,
  [IS_DURABLE] [VARCHAR] (1)  NOT NULL ,
  [IS_VOLATILE] [VARCHAR] (1)  NOT NULL ,
  [IS_STATEFUL] [VARCHAR] (1)  NOT NULL ,
  [REQUESTS_RECOVERY] [VARCHAR] (1)  NOT NULL ,
  [JOB_DATA] [IMAGE] NULL
)
GO

CREATE TABLE [dbo].[QRTZ_JOB_LISTENERS] (
  [JOB_NAME] [VARCHAR] (80)  NOT NULL ,
  [JOB_GROUP] [VARCHAR] (80)  NOT NULL ,
  [JOB_LISTENER] [VARCHAR] (80)  NOT NULL
)
GO

CREATE TABLE [dbo].[QRTZ_SIMPLE_TRIGGERS] (
  [TRIGGER_NAME] [VARCHAR] (80)  NOT NULL ,
  [TRIGGER_GROUP] [VARCHAR] (80)  NOT NULL ,
  [REPEAT_COUNT] [BIGINT] NOT NULL ,
  [REPEAT_INTERVAL] [BIGINT] NOT NULL ,
  [TIMES_TRIGGERED] [BIGINT] NOT NULL
)
GO

CREATE TABLE [dbo].[QRTZ_BLOB_TRIGGERS] (
  [TRIGGER_NAME] [VARCHAR] (80)  NOT NULL ,
  [TRIGGER_GROUP] [VARCHAR] (80)  NOT NULL ,
  [BLOB_DATA] [IMAGE] NULL
)
GO

CREATE TABLE [dbo].[QRTZ_TRIGGER_LISTENERS] (
  [TRIGGER_NAME] [VARCHAR] (80)  NOT NULL ,
  [TRIGGER_GROUP] [VARCHAR] (80)  NOT NULL ,
  [TRIGGER_LISTENER] [VARCHAR] (80)  NOT NULL
)
GO

CREATE TABLE [dbo].[QRTZ_TRIGGERS] (
  [TRIGGER_NAME] [VARCHAR] (80)  NOT NULL ,
  [TRIGGER_GROUP] [VARCHAR] (80)  NOT NULL ,
  [JOB_NAME] [VARCHAR] (80)  NOT NULL ,
  [JOB_GROUP] [VARCHAR] (80)  NOT NULL ,
  [IS_VOLATILE] [VARCHAR] (1)  NOT NULL ,
  [DESCRIPTION] [VARCHAR] (120) NULL ,
  [NEXT_FIRE_TIME] [BIGINT] NULL ,
  [PREV_FIRE_TIME] [BIGINT] NULL ,
  [PRIORITY] [INTEGER] NULL ,
  [TRIGGER_STATE] [VARCHAR] (16)  NOT NULL ,
  [TRIGGER_TYPE] [VARCHAR] (8)  NOT NULL ,
  [START_TIME] [BIGINT] NOT NULL ,
  [END_TIME] [BIGINT] NULL ,
  [CALENDAR_NAME] [VARCHAR] (80)  NULL ,
  [MISFIRE_INSTR] [SMALLINT] NULL ,
  [JOB_DATA] [IMAGE] NULL
)
GO

ALTER TABLE [dbo].[QRTZ_CALENDARS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_CALENDARS] PRIMARY KEY  CLUSTERED
  (
    [CALENDAR_NAME]
  ) 
GO

ALTER TABLE [dbo].[QRTZ_CRON_TRIGGERS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_CRON_TRIGGERS] PRIMARY KEY  CLUSTERED
  (
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  ) 
GO

ALTER TABLE [dbo].[QRTZ_FIRED_TRIGGERS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_FIRED_TRIGGERS] PRIMARY KEY  CLUSTERED
  (
    [ENTRY_ID]
  ) 
GO

ALTER TABLE [dbo].[QRTZ_PAUSED_TRIGGER_GRPS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_PAUSED_TRIGGER_GRPS] PRIMARY KEY  CLUSTERED
  (
    [TRIGGER_GROUP]
  ) 
GO

ALTER TABLE [dbo].[QRTZ_SCHEDULER_STATE] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_SCHEDULER_STATE] PRIMARY KEY  CLUSTERED
  (
    [INSTANCE_NAME]
  ) 
GO

ALTER TABLE [dbo].[QRTZ_LOCKS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_LOCKS] PRIMARY KEY  CLUSTERED
  (
    [LOCK_NAME]
  ) 
GO

ALTER TABLE [dbo].[QRTZ_JOB_DETAILS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_JOB_DETAILS] PRIMARY KEY  CLUSTERED
  (
    [JOB_NAME],
    [JOB_GROUP]
  ) 
GO

ALTER TABLE [dbo].[QRTZ_JOB_LISTENERS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_JOB_LISTENERS] PRIMARY KEY  CLUSTERED
  (
    [JOB_NAME],
    [JOB_GROUP],
    [JOB_LISTENER]
  ) 
GO

ALTER TABLE [dbo].[QRTZ_SIMPLE_TRIGGERS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_SIMPLE_TRIGGERS] PRIMARY KEY  CLUSTERED
  (
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  ) 
GO

ALTER TABLE [dbo].[QRTZ_TRIGGER_LISTENERS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_TRIGGER_LISTENERS] PRIMARY KEY  CLUSTERED
  (
    [TRIGGER_NAME],
    [TRIGGER_GROUP],
    [TRIGGER_LISTENER]
  ) 
GO

ALTER TABLE [dbo].[QRTZ_TRIGGERS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_TRIGGERS] PRIMARY KEY  CLUSTERED
  (
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  ) 
GO

ALTER TABLE [dbo].[QRTZ_CRON_TRIGGERS] ADD
  CONSTRAINT [FK_QRTZ_CRON_TRIGGERS_QRTZ_TRIGGERS] FOREIGN KEY
  (
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  ) REFERENCES [dbo].[QRTZ_TRIGGERS] (
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  ) ON DELETE CASCADE
GO

ALTER TABLE [dbo].[QRTZ_JOB_LISTENERS] ADD
  CONSTRAINT [FK_QRTZ_JOB_LISTENERS_QRTZ_JOB_DETAILS] FOREIGN KEY
  (
    [JOB_NAME],
    [JOB_GROUP]
  ) REFERENCES [dbo].[QRTZ_JOB_DETAILS] (
    [JOB_NAME],
    [JOB_GROUP]
  ) ON DELETE CASCADE
GO

ALTER TABLE [dbo].[QRTZ_SIMPLE_TRIGGERS] ADD
  CONSTRAINT [FK_QRTZ_SIMPLE_TRIGGERS_QRTZ_TRIGGERS] FOREIGN KEY
  (
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  ) REFERENCES [dbo].[QRTZ_TRIGGERS] (
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  ) ON DELETE CASCADE
GO

ALTER TABLE [dbo].[QRTZ_TRIGGER_LISTENERS] ADD
  CONSTRAINT [FK_QRTZ_TRIGGER_LISTENERS_QRTZ_TRIGGERS] FOREIGN KEY
  (
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  ) REFERENCES [dbo].[QRTZ_TRIGGERS] (
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  ) ON DELETE CASCADE
GO

ALTER TABLE [dbo].[QRTZ_TRIGGERS] ADD
  CONSTRAINT [FK_QRTZ_TRIGGERS_QRTZ_JOB_DETAILS] FOREIGN KEY
  (
    [JOB_NAME],
    [JOB_GROUP]
  ) REFERENCES [dbo].[QRTZ_JOB_DETAILS] (
    [JOB_NAME],
    [JOB_GROUP]
  )
GO

INSERT INTO [dbo].[QRTZ_LOCKS] VALUES('TRIGGER_ACCESS');
INSERT INTO [dbo].[QRTZ_LOCKS] VALUES('JOB_ACCESS');
INSERT INTO [dbo].[QRTZ_LOCKS] VALUES('CALENDAR_ACCESS');
INSERT INTO [dbo].[QRTZ_LOCKS] VALUES('STATE_ACCESS');
INSERT INTO [dbo].[QRTZ_LOCKS] VALUES('MISFIRE_ACCESS');

CREATE TABLE [dbo].[QRTZ_EXCL_CALENDARS] (
  [CALENDAR_NAME] [VARCHAR] (80)  NOT NULL ,
  [CALENDAR] [IMAGE] NOT NULL
)
GO

CREATE TABLE [dbo].[QRTZ_EXCL_CRON_TRIGGERS] (
  [TRIGGER_NAME] [VARCHAR] (80)  NOT NULL ,
  [TRIGGER_GROUP] [VARCHAR] (80)  NOT NULL ,
  [CRON_EXPRESSION] [VARCHAR] (80)  NOT NULL ,
  [TIME_ZONE_ID] [VARCHAR] (80)
)
GO

CREATE TABLE [dbo].[QRTZ_EXCL_FIRED_TRIGGERS] (
  [ENTRY_ID] [VARCHAR] (95)  NOT NULL ,
  [TRIGGER_NAME] [VARCHAR] (80)  NOT NULL ,
  [TRIGGER_GROUP] [VARCHAR] (80)  NOT NULL ,
  [IS_VOLATILE] [VARCHAR] (1)  NOT NULL ,
  [INSTANCE_NAME] [VARCHAR] (80)  NOT NULL ,
  [FIRED_TIME] [BIGINT] NOT NULL ,
  [PRIORITY] [INTEGER] NOT NULL ,
  [STATE] [VARCHAR] (16)  NOT NULL,
  [JOB_NAME] [VARCHAR] (80)  NULL ,
  [JOB_GROUP] [VARCHAR] (80)  NULL ,
  [IS_STATEFUL] [VARCHAR] (1)  NULL ,
  [REQUESTS_RECOVERY] [VARCHAR] (1)  NULL
)
GO

CREATE TABLE [dbo].[QRTZ_EXCL_PAUSED_TRIGGER_GRPS] (
  [TRIGGER_GROUP] [VARCHAR] (80)  NOT NULL
)
GO

CREATE TABLE [dbo].[QRTZ_EXCL_SCHEDULER_STATE] (
  [INSTANCE_NAME] [VARCHAR] (80)  NOT NULL ,
  [LAST_CHECKIN_TIME] [BIGINT] NOT NULL ,
  [CHECKIN_INTERVAL] [BIGINT] NOT NULL
)
GO

CREATE TABLE [dbo].[QRTZ_EXCL_LOCKS] (
  [LOCK_NAME] [VARCHAR] (40)  NOT NULL
)
GO

CREATE TABLE [dbo].[QRTZ_EXCL_JOB_DETAILS] (
  [JOB_NAME] [VARCHAR] (80)  NOT NULL ,
  [JOB_GROUP] [VARCHAR] (80)  NOT NULL ,
  [DESCRIPTION] [VARCHAR] (120) NULL ,
  [JOB_CLASS_NAME] [VARCHAR] (128)  NOT NULL ,
  [IS_DURABLE] [VARCHAR] (1)  NOT NULL ,
  [IS_VOLATILE] [VARCHAR] (1)  NOT NULL ,
  [IS_STATEFUL] [VARCHAR] (1)  NOT NULL ,
  [REQUESTS_RECOVERY] [VARCHAR] (1)  NOT NULL ,
  [JOB_DATA] [IMAGE] NULL
)
GO

CREATE TABLE [dbo].[QRTZ_EXCL_JOB_LISTENERS] (
  [JOB_NAME] [VARCHAR] (80)  NOT NULL ,
  [JOB_GROUP] [VARCHAR] (80)  NOT NULL ,
  [JOB_LISTENER] [VARCHAR] (80)  NOT NULL
)
GO

CREATE TABLE [dbo].[QRTZ_EXCL_SIMPLE_TRIGGERS] (
  [TRIGGER_NAME] [VARCHAR] (80)  NOT NULL ,
  [TRIGGER_GROUP] [VARCHAR] (80)  NOT NULL ,
  [REPEAT_COUNT] [BIGINT] NOT NULL ,
  [REPEAT_INTERVAL] [BIGINT] NOT NULL ,
  [TIMES_TRIGGERED] [BIGINT] NOT NULL
)
GO

CREATE TABLE [dbo].[QRTZ_EXCL_BLOB_TRIGGERS] (
  [TRIGGER_NAME] [VARCHAR] (80)  NOT NULL ,
  [TRIGGER_GROUP] [VARCHAR] (80)  NOT NULL ,
  [BLOB_DATA] [IMAGE] NULL
)
GO

CREATE TABLE [dbo].[QRTZ_EXCL_TRIGGER_LISTENERS] (
  [TRIGGER_NAME] [VARCHAR] (80)  NOT NULL ,
  [TRIGGER_GROUP] [VARCHAR] (80)  NOT NULL ,
  [TRIGGER_LISTENER] [VARCHAR] (80)  NOT NULL
)
GO

CREATE TABLE [dbo].[QRTZ_EXCL_TRIGGERS] (
  [TRIGGER_NAME] [VARCHAR] (80)  NOT NULL ,
  [TRIGGER_GROUP] [VARCHAR] (80)  NOT NULL ,
  [JOB_NAME] [VARCHAR] (80)  NOT NULL ,
  [JOB_GROUP] [VARCHAR] (80)  NOT NULL ,
  [IS_VOLATILE] [VARCHAR] (1)  NOT NULL ,
  [DESCRIPTION] [VARCHAR] (120) NULL ,
  [NEXT_FIRE_TIME] [BIGINT] NULL ,
  [PREV_FIRE_TIME] [BIGINT] NULL ,
  [PRIORITY] [INTEGER] NULL ,
  [TRIGGER_STATE] [VARCHAR] (16)  NOT NULL ,
  [TRIGGER_TYPE] [VARCHAR] (8)  NOT NULL ,
  [START_TIME] [BIGINT] NOT NULL ,
  [END_TIME] [BIGINT] NULL ,
  [CALENDAR_NAME] [VARCHAR] (80)  NULL ,
  [MISFIRE_INSTR] [SMALLINT] NULL ,
  [JOB_DATA] [IMAGE] NULL
)
GO

ALTER TABLE [dbo].[QRTZ_EXCL_CALENDARS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_EXCL_CALENDARS] PRIMARY KEY  CLUSTERED
  (
    [CALENDAR_NAME]
  ) 
GO

ALTER TABLE [dbo].[QRTZ_EXCL_CRON_TRIGGERS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_EXCL_CRON_TRIGGERS] PRIMARY KEY  CLUSTERED
  (
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  ) 
GO

ALTER TABLE [dbo].[QRTZ_EXCL_FIRED_TRIGGERS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_EXCL_FIRED_TRIGGERS] PRIMARY KEY  CLUSTERED
  (
    [ENTRY_ID]
  ) 
GO

ALTER TABLE [dbo].[QRTZ_EXCL_PAUSED_TRIGGER_GRPS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_EXCL_PAUSED_TRIGGER_GRPS] PRIMARY KEY  CLUSTERED
  (
    [TRIGGER_GROUP]
  ) 
GO

ALTER TABLE [dbo].[QRTZ_EXCL_SCHEDULER_STATE] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_EXCL_SCHEDULER_STATE] PRIMARY KEY  CLUSTERED
  (
    [INSTANCE_NAME]
  ) 
GO

ALTER TABLE [dbo].[QRTZ_EXCL_LOCKS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_EXCL_LOCKS] PRIMARY KEY  CLUSTERED
  (
    [LOCK_NAME]
  ) 
GO

ALTER TABLE [dbo].[QRTZ_EXCL_JOB_DETAILS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_EXCL_JOB_DETAILS] PRIMARY KEY  CLUSTERED
  (
    [JOB_NAME],
    [JOB_GROUP]
  ) 
GO

ALTER TABLE [dbo].[QRTZ_EXCL_JOB_LISTENERS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_EXCL_JOB_LISTENERS] PRIMARY KEY  CLUSTERED
  (
    [JOB_NAME],
    [JOB_GROUP],
    [JOB_LISTENER]
  ) 
GO

ALTER TABLE [dbo].[QRTZ_EXCL_SIMPLE_TRIGGERS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_EXCL_SIMPLE_TRIGGERS] PRIMARY KEY  CLUSTERED
  (
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  ) 
GO

ALTER TABLE [dbo].[QRTZ_EXCL_TRIGGER_LISTENERS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_EXCL_TRIGGER_LISTENERS] PRIMARY KEY  CLUSTERED
  (
    [TRIGGER_NAME],
    [TRIGGER_GROUP],
    [TRIGGER_LISTENER]
  ) 
GO

ALTER TABLE [dbo].[QRTZ_EXCL_TRIGGERS] WITH NOCHECK ADD
  CONSTRAINT [PK_QRTZ_EXCL_TRIGGERS] PRIMARY KEY  CLUSTERED
  (
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  ) 
GO

ALTER TABLE [dbo].[QRTZ_EXCL_CRON_TRIGGERS] ADD
  CONSTRAINT [FK_QRTZ_EXCL_CRON_TRIGGERS_QRTZ_EXCL_TRIGGERS] FOREIGN KEY
  (
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  ) REFERENCES [dbo].[QRTZ_EXCL_TRIGGERS] (
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  ) ON DELETE CASCADE
GO

ALTER TABLE [dbo].[QRTZ_EXCL_JOB_LISTENERS] ADD
  CONSTRAINT [FK_QRTZ_EXCL_JOB_LISTENERS_QRTZ_EXCL_JOB_DETAILS] FOREIGN KEY
  (
    [JOB_NAME],
    [JOB_GROUP]
  ) REFERENCES [dbo].[QRTZ_EXCL_JOB_DETAILS] (
    [JOB_NAME],
    [JOB_GROUP]
  ) ON DELETE CASCADE
GO

ALTER TABLE [dbo].[QRTZ_EXCL_SIMPLE_TRIGGERS] ADD
  CONSTRAINT [FK_QRTZ_EXCL_SIMPLE_TRIGGERS_QRTZ_EXCL_TRIGGERS] FOREIGN KEY
  (
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  ) REFERENCES [dbo].[QRTZ_EXCL_TRIGGERS] (
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  ) ON DELETE CASCADE
GO

ALTER TABLE [dbo].[QRTZ_EXCL_TRIGGER_LISTENERS] ADD
  CONSTRAINT [FK_QRTZ_EXCL_TRIGGER_LISTENERS_QRTZ_EXCL_TRIGGERS] FOREIGN KEY
  (
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  ) REFERENCES [dbo].[QRTZ_EXCL_TRIGGERS] (
    [TRIGGER_NAME],
    [TRIGGER_GROUP]
  ) ON DELETE CASCADE
GO

ALTER TABLE [dbo].[QRTZ_EXCL_TRIGGERS] ADD
  CONSTRAINT [FK_QRTZ_EXCL_TRIGGERS_QRTZ_EXCL_JOB_DETAILS] FOREIGN KEY
  (
    [JOB_NAME],
    [JOB_GROUP]
  ) REFERENCES [dbo].[QRTZ_EXCL_JOB_DETAILS] (
    [JOB_NAME],
    [JOB_GROUP]
  )
GO

INSERT INTO [dbo].[QRTZ_EXCL_LOCKS] VALUES('TRIGGER_ACCESS');
INSERT INTO [dbo].[QRTZ_EXCL_LOCKS] VALUES('JOB_ACCESS');
INSERT INTO [dbo].[QRTZ_EXCL_LOCKS] VALUES('CALENDAR_ACCESS');
INSERT INTO [dbo].[QRTZ_EXCL_LOCKS] VALUES('STATE_ACCESS');
INSERT INTO [dbo].[QRTZ_EXCL_LOCKS] VALUES('MISFIRE_ACCESS');
create table calendar_reminder (
   user_id varchar(255) not null,
   event_id varchar(36) not null,
   send_date datetime not null,
   primary key (user_id, event_id, send_date)
);
create table analytic_summary_pages (
   id numeric(19,0) identity not null,
   summary_id numeric(19,0) not null,
   inode varchar(255) null,
   hits numeric(19,0) null,
   uri varchar(255) null,
   primary key (id)
);
create table tag (
   tag_id varchar(100) not null,
   tagname nvarchar(255) not null,
   host_id varchar(255) null,
   user_id varchar(255) null,
   persona tinyint default 0,
   mod_date datetime null,
   primary key (tag_id)
);
create table user_comments (
   inode varchar(36) not null,
   user_id varchar(255) null,
   cdate datetime null,
   comment_user_id varchar(100) null,
   type varchar(255) null,
   method varchar(255) null,
   subject varchar(255) null,
   ucomment text null,
   communication_id varchar(36) null,
   primary key (inode)
);
create table permission_reference (
   id numeric(19,0) identity not null,
   asset_id varchar(36) null,
   reference_id varchar(36) null,
   permission_type varchar(100) null,
   primary key (id),
   unique (asset_id)
);
create table contentlet_version_info (
   identifier varchar(36) not null,
   lang numeric(19,0) not null,
   working_inode varchar(36) not null,
   live_inode varchar(36) null,
   deleted tinyint not null,
   locked_by varchar(100) null,
   locked_on datetime null,
   version_ts datetime not null,
   primary key (identifier, lang)
);
create table fixes_audit (
   id varchar(36) not null,
   table_name varchar(255) null,
   action varchar(255) null,
   records_altered int null,
   datetime datetime null,
   primary key (id)
);
create table container_version_info (
   identifier varchar(36) not null,
   working_inode varchar(36) not null,
   live_inode varchar(36) null,
   deleted tinyint not null,
   locked_by varchar(100) null,
   locked_on datetime null,
   version_ts datetime not null,
   primary key (identifier)
);
create table trackback (
   id numeric(19,0) identity not null,
   asset_identifier varchar(36) null,
   title varchar(255) null,
   excerpt varchar(255) null,
   url varchar(255) null,
   blog_name varchar(255) null,
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
   title varchar(255) null,
   public_list tinyint null,
   user_id varchar(255) null,
   primary key (inode)
);
create table recipient (
   inode varchar(36) not null,
   name varchar(255) null,
   lastname varchar(255) null,
   email varchar(255) null,
   sent datetime null,
   opened datetime null,
   last_result int null,
   last_message varchar(255) null,
   user_id varchar(100) null,
   primary key (inode)
);
create table web_form (
   web_form_id varchar(36) not null,
   form_type varchar(255) null,
   submit_date datetime null,
   prefix varchar(255) null,
   first_name varchar(255) null,
   middle_initial varchar(255) null,
   middle_name varchar(255) null,
   full_name varchar(255) null,
   organization varchar(255) null,
   title varchar(255) null,
   last_name varchar(255) null,
   address varchar(255) null,
   address1 varchar(255) null,
   address2 varchar(255) null,
   city varchar(255) null,
   state varchar(255) null,
   zip varchar(255) null,
   country varchar(255) null,
   phone varchar(255) null,
   email varchar(255) null,
   custom_fields text null,
   user_inode varchar(36) null,
   categories varchar(255) null,
   primary key (web_form_id)
);
create table virtual_link (
   inode varchar(36) not null,
   title varchar(255) null,
   url varchar(255) null,
   uri varchar(255) null,
   active tinyint null,
   primary key (inode)
);
create table analytic_summary_period (
   id numeric(19,0) identity not null,
   full_date datetime null,
   day int null,
   week int null,
   month int null,
   year varchar(255) null,
   dayname varchar(50) not null,
   monthname varchar(50) not null,
   primary key (id),
   unique (full_date)
);
create table tree (
   child varchar(36) not null,
   parent varchar(36) not null,
   relation_type varchar(64) not null,
   tree_order int null,
   primary key (child, parent, relation_type)
);
create table analytic_summary (
   id numeric(19,0) identity not null,
   summary_period_id numeric(19,0) not null,
   host_id varchar(36) not null,
   visits numeric(19,0) null,
   page_views numeric(19,0) null,
   unique_visits numeric(19,0) null,
   new_visits numeric(19,0) null,
   direct_traffic numeric(19,0) null,
   referring_sites numeric(19,0) null,
   search_engines numeric(19,0) null,
   bounce_rate int null,
   avg_time_on_site datetime null,
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
   show_on_menu tinyint null,
   title varchar(255) null,
   mod_date datetime null,
   mod_user varchar(100) null,
   sort_order int null,
   friendly_name varchar(255) null,
   body text null,
   header text null,
   footer text null,
   image varchar(36) null,
   identifier varchar(36) null,
   drawed tinyint null,
   drawed_body text null,
   add_container_links int null,
   containers_added int null,
   head_code text null,
   theme varchar(255) null,
   primary key (inode)
);
create table analytic_summary_content (
   id numeric(19,0) identity not null,
   summary_id numeric(19,0) not null,
   inode varchar(255) null,
   hits numeric(19,0) null,
   uri varchar(255) null,
   title varchar(255) null,
   primary key (id)
);
create table structure (
   inode varchar(36) not null,
   name varchar(255) null,
   description varchar(255) null,
   default_structure tinyint null,
   review_interval varchar(255) null,
   reviewer_role varchar(255) null,
   page_detail varchar(36) null,
   structuretype int null,
   system tinyint null,
   fixed tinyint not null,
   velocity_var_name varchar(255) null,
   url_map_pattern varchar(512) null,
   host varchar(36) not null,
   folder varchar(36) not null,
   expire_date_var varchar(255) null,
   publish_date_var varchar(255) null,
   mod_date datetime null,
   primary key (inode)
);
create table cms_role (
   id varchar(36) not null,
   role_name varchar(255) not null,
   description text null,
   role_key varchar(255) null,
   db_fqn varchar(1000) not null,
   parent varchar(36) not null,
   edit_permissions tinyint null,
   edit_users tinyint null,
   edit_layouts tinyint null,
   locked tinyint null,
   system tinyint null,
   primary key (id)
);
create table container_structures (
   id varchar(36) not null,
   container_id varchar(36) not null,
   container_inode varchar(36) not null,
   structure_id varchar(36) not null,
   code text null,
   primary key (id)
);
create table permission (
   id numeric(19,0) identity not null,
   permission_type varchar(100) null,
   inode_id varchar(36) null,
   roleid varchar(36) null,
   permission int null,
   primary key (id),
   unique (permission_type, inode_id, roleid)
);
	create table contentlet (inode varchar(36) not null,
	show_on_menu tinyint null,
	title varchar(255) null,
	mod_date datetime null,
	mod_user varchar(100) null,
	sort_order int null,
	friendly_name varchar(255) null,
	structure_inode varchar(36) null,
	last_review datetime null,
	next_review datetime null,
	review_interval varchar(255) null,
	disabled_wysiwyg varchar(255) null,
	identifier varchar(36) null,
	language_id numeric(19,0) null,
	date1 datetime null,
	date2 datetime null,
	date3 datetime null,
	date4 datetime null,
	date5 datetime null,
	date6 datetime null,
	date7 datetime null,
	date8 datetime null,
	date9 datetime null,
	date10 datetime null,
	date11 datetime null,
	date12 datetime null,
	date13 datetime null,
	date14 datetime null,
	date15 datetime null,
	date16 datetime null,
	date17 datetime null,
	date18 datetime null,
	date19 datetime null,
	date20 datetime null,
	date21 datetime null,
	date22 datetime null,
	date23 datetime null,
	date24 datetime null,
	date25 datetime null,
	text1 varchar(255) null,
	text2 varchar(255) null,
	text3 varchar(255) null,
	text4 varchar(255) null,
	text5 varchar(255) null,
	text6 varchar(255) null,
	text7 varchar(255) null,
	text8 varchar(255) null,
	text9 varchar(255) null,
	text10 varchar(255) null,
	text11 varchar(255) null,
	text12 varchar(255) null,
	text13 varchar(255) null,
	text14 varchar(255) null,
	text15 varchar(255) null,
	text16 varchar(255) null,
	text17 varchar(255) null,
	text18 varchar(255) null,
	text19 varchar(255) null,
	text20 varchar(255) null,
	text21 varchar(255) null,
	text22 varchar(255) null,
	text23 varchar(255) null,
	text24 varchar(255) null,
	text25 varchar(255) null,
	text_area1 text null,
	text_area2 text null,
	text_area3 text null,
	text_area4 text null,
	text_area5 text null,
	text_area6 text null,
	text_area7 text null,
	text_area8 text null,
	text_area9 text null,
	text_area10 text null,
	text_area11 text null,
	text_area12 text null,
	text_area13 text null,
	text_area14 text null,
	text_area15 text null,
	text_area16 text null,
	text_area17 text null,
	text_area18 text null,
	text_area19 text null,
	text_area20 text null,
	text_area21 text null,
	text_area22 text null,
	text_area23 text null,
	text_area24 text null,
	text_area25 text null,
	integer1 numeric(19,0) null,
	integer2 numeric(19,0) null,
	integer3 numeric(19,0) null,
	integer4 numeric(19,0) null,
	integer5 numeric(19,0) null,
	integer6 numeric(19,0) null,
	integer7 numeric(19,0) null,
	integer8 numeric(19,0) null,
	integer9 numeric(19,0) null,
	integer10 numeric(19,0) null,
	integer11 numeric(19,0) null,
	integer12 numeric(19,0) null,
	integer13 numeric(19,0) null,
	integer14 numeric(19,0) null,
	integer15 numeric(19,0) null,
	integer16 numeric(19,0) null,
	integer17 numeric(19,0) null,
	integer18 numeric(19,0) null,
	integer19 numeric(19,0) null,
	integer20 numeric(19,0) null,
	integer21 numeric(19,0) null,
	integer22 numeric(19,0) null,
	integer23 numeric(19,0) null,
	integer24 numeric(19,0) null,
	integer25 numeric(19,0) null,
	"float1" float null,
	"float2" float null,
	"float3" float null,
	"float4" float null,
	"float5" float null,
	"float6" float null,
	"float7" float null,
	"float8" float null,
	"float9" float null,
	"float10" float null,
	"float11" float null,
	"float12" float null,
	"float13" float null,
	"float14" float null,
	"float15" float null,
	"float16" float null,
	"float17" float null,
	"float18" float null,
	"float19" float null,
	"float20" float null,
	"float21" float null,
	"float22" float null,
	"float23" float null,
	"float24" float null,
	"float25" float null,
	bool1 tinyint null,
	bool2 tinyint null,
	bool3 tinyint null,
	bool4 tinyint null,
	bool5 tinyint null,
	bool6 tinyint null,
	bool7 tinyint null,
	bool8 tinyint null,
	bool9 tinyint null,
	bool10 tinyint null,
	bool11 tinyint null,
	bool12 tinyint null,
	bool13 tinyint null,
	bool14 tinyint null,
	bool15 tinyint null,
	bool16 tinyint null,
	bool17 tinyint null,
	bool18 tinyint null,
	bool19 tinyint null,
	bool20 tinyint null,
	bool21 tinyint null,
	bool22 tinyint null,
	bool23 tinyint null,
	bool24 tinyint null,
	bool25 tinyint null,
	primary key (inode));
create table analytic_summary_404 (
   id numeric(19,0) identity not null,
   summary_period_id numeric(19,0) not null,
   host_id varchar(36) null,
   uri varchar(255) null,
   referer_uri varchar(255) null,
   primary key (id)
);
create table cms_layouts_portlets (
   id varchar(36) not null,
   layout_id varchar(36) not null,
   portlet_id varchar(100) not null,
   portlet_order int null,
   primary key (id)
);
create table report_asset (
   inode varchar(36) not null,
   report_name varchar(255) not null,
   report_description varchar(1000) not null,
   requires_input tinyint null,
   ds varchar(100) not null,
   web_form_report tinyint null,
   primary key (inode)
);
create table workflow_comment (
   id varchar(36) not null,
   creation_date datetime null,
   posted_by varchar(255) null,
   wf_comment text null,
   workflowtask_id varchar(36) null,
   primary key (id)
);
create table category (
   inode varchar(36) not null,
   category_name varchar(255) null,
   category_key varchar(255) null,
   sort_order int null,
   active tinyint null,
   keywords text null,
   category_velocity_var_name varchar(255) null,
   mod_date datetime null,
   primary key (inode)
);
create table htmlpage (
   inode varchar(36) not null,
   show_on_menu tinyint null,
   title varchar(255) null,
   mod_date datetime null,
   mod_user varchar(100) null,
   sort_order int null,
   friendly_name varchar(255) null,
   metadata text null,
   start_date datetime null,
   end_date datetime null,
   page_url varchar(255) null,
   https_required tinyint null,
   redirect varchar(255) null,
   identifier varchar(36) null,
   seo_description text null,
   seo_keywords text null,
   cache_ttl numeric(19,0) null,
   template_id varchar(36) null,
   primary key (inode)
);
create table chain_link_code (
   id numeric(19,0) identity not null,
   class_name varchar(255) null unique,
   code text not null,
   last_mod_date datetime not null,
   language varchar(255) not null,
   primary key (id)
);
create table analytic_summary_visits (
   id numeric(19,0) identity not null,
   summary_period_id numeric(19,0) not null,
   host_id varchar(36) null,
   visit_time datetime null,
   visits numeric(19,0) null,
   primary key (id)
);
create table template_version_info (
   identifier varchar(36) not null,
   working_inode varchar(36) not null,
   live_inode varchar(36) null,
   deleted tinyint not null,
   locked_by varchar(100) null,
   locked_on datetime null,
   version_ts datetime not null,
   primary key (identifier)
);
create table user_preferences (
   id numeric(19,0) identity not null,
   user_id varchar(100) not null,
   preference varchar(255) null,
   pref_value text null,
   primary key (id)
);
create table language (
   id numeric(19,0) identity not null,
   language_code varchar(5) null,
   country_code varchar(255) null,
   language varchar(255) null,
   country varchar(255) null,
   primary key (id)
);
create table users_to_delete (
   id numeric(19,0) identity not null,
   user_id varchar(255) null,
   primary key (id)
);
create table identifier (
   id varchar(36) not null,
   parent_path varchar(255) null,
   asset_name varchar(255) null,
   host_inode varchar(36) null,
   asset_type varchar(64) null,
   syspublish_date datetime null,
   sysexpire_date datetime null,
   primary key (id),
   unique (parent_path, asset_name, host_inode)
);
create table clickstream (
   clickstream_id numeric(19,0) identity not null,
   cookie_id varchar(255) null,
   user_id varchar(255) null,
   start_date datetime null,
   end_date datetime null,
   referer varchar(255) null,
   remote_address varchar(255) null,
   remote_hostname varchar(255) null,
   user_agent varchar(255) null,
   bot tinyint null,
   number_of_requests int null,
   host_id varchar(36) null,
   last_page_id varchar(50) null,
   first_page_id varchar(50) null,
   operating_system varchar(50) null,
   browser_name varchar(50) null,
   browser_version varchar(50) null,
   mobile_device tinyint null,
   primary key (clickstream_id)
);
create table multi_tree (
   child varchar(36) not null,
   parent1 varchar(36) not null,
   parent2 varchar(36) not null,
   relation_type varchar(64) null,
   tree_order int null,
   primary key (child, parent1, parent2)
);
create table workflow_task (
   id varchar(36) not null,
   creation_date datetime null,
   mod_date datetime null,
   due_date datetime null,
   created_by varchar(255) null,
   assigned_to varchar(255) null,
   belongs_to varchar(255) null,
   title varchar(255) null,
   description text null,
   status varchar(255) null,
   webasset varchar(255) null,
   primary key (id)
);
create table tag_inode (
   tag_id varchar(100) not null,
   inode varchar(100) not null,
	 field_var_name varchar(255),
   mod_date datetime null,
   primary key (tag_id, inode)
);
create table click (
   inode varchar(36) not null,
   link varchar(255) null,
   click_count int null,
   primary key (inode)
);
create table challenge_question (
   cquestionid numeric(19,0) not null,
   cqtext varchar(255) null,
   primary key (cquestionid)
);
create table file_asset (
   inode varchar(36) not null,
   file_name varchar(255) null,
   file_size int null,
   width int null,
   height int null,
   mime_type varchar(255) null,
   author varchar(255) null,
   publish_date datetime null,
   show_on_menu tinyint null,
   title varchar(255) null,
   friendly_name varchar(255) null,
   mod_date datetime null,
   mod_user varchar(100) null,
   sort_order int null,
   identifier varchar(36) null,
   primary key (inode)
);
create table layouts_cms_roles (
   id varchar(36) not null,
   layout_id varchar(36) not null,
   role_id varchar(36) not null,
   primary key (id)
);
create table clickstream_request (
   clickstream_request_id numeric(19,0) identity not null,
   clickstream_id numeric(19,0) null,
   server_name varchar(255) null,
   protocol varchar(255) null,
   server_port int null,
   request_uri varchar(255) null,
   request_order int null,
   query_string text null,
   language_id numeric(19,0) null,
   timestampper datetime null,
   host_id varchar(36) null,
   associated_identifier varchar(36) null,
   primary key (clickstream_request_id)
);
create table content_rating (
   id numeric(19,0) identity not null,
   rating float null,
   user_id varchar(255) null,
   session_id varchar(255) null,
   identifier varchar(36) null,
   rating_date datetime null,
   user_ip varchar(255) null,
   long_live_cookie_id varchar(255) null,
   primary key (id)
);
create table chain_state (
   id numeric(19,0) identity not null,
   chain_id numeric(19,0) not null,
   link_code_id numeric(19,0) not null,
   state_order numeric(19,0) not null,
   primary key (id)
);
create table analytic_summary_workstream (
   id numeric(19,0) identity not null,
   inode varchar(255) null,
   asset_type varchar(255) null,
   mod_user_id varchar(255) null,
   host_id varchar(36) null,
   mod_date datetime null,
   action varchar(255) null,
   name varchar(255) null,
   primary key (id)
);
create table dashboard_user_preferences (
   id numeric(19,0) identity not null,
   summary_404_id numeric(19,0) null,
   user_id varchar(255) null,
   ignored tinyint null,
   mod_date datetime null,
   primary key (id)
);
create table campaign (
   inode varchar(36) not null,
   title varchar(255) null,
   from_email varchar(255) null,
   from_name varchar(255) null,
   subject varchar(255) null,
   message text null,
   user_id varchar(255) null,
   start_date datetime null,
   completed_date datetime null,
   active tinyint null,
   locked tinyint null,
   sends_per_hour varchar(15) null,
   sendemail tinyint null,
   communicationinode varchar(36) null,
   userfilterinode varchar(36) null,
   sendto varchar(15) null,
   isrecurrent tinyint null,
   wassent tinyint null,
   expiration_date datetime null,
   parent_campaign varchar(36) null,
   primary key (inode)
);
create table htmlpage_version_info (
   identifier varchar(36) not null,
   working_inode varchar(36) not null,
   live_inode varchar(36) null,
   deleted tinyint not null,
   locked_by varchar(100) null,
   locked_on datetime null,
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
   id numeric(19,0) identity not null,
   summary_id numeric(19,0) not null,
   hits numeric(19,0) null,
   uri varchar(255) null,
   primary key (id)
);
create table dot_containers (
   inode varchar(36) not null,
   code text null,
   pre_loop text null,
   post_loop text null,
   show_on_menu tinyint null,
   title varchar(255) null,
   mod_date datetime null,
   mod_user varchar(100) null,
   sort_order int null,
   friendly_name varchar(255) null,
   max_contentlets int null,
   use_div tinyint null,
   staticify tinyint null,
   sort_contentlets_by varchar(255) null,
   lucene_query text null,
   notes varchar(255) null,
   identifier varchar(36) null,
   primary key (inode)
);
create table communication (
   inode varchar(36) not null,
   title varchar(255) null,
   trackback_link_inode varchar(36) null,
   communication_type varchar(255) null,
   from_name varchar(255) null,
   from_email varchar(255) null,
   email_subject varchar(255) null,
   html_page_inode varchar(36) null,
   text_message text null,
   mod_date datetime null,
   modified_by varchar(255) null,
   ext_comm_id varchar(255) null,
   primary key (inode)
);
create table fileasset_version_info (
   identifier varchar(36) not null,
   working_inode varchar(36) not null,
   live_inode varchar(36) null,
   deleted tinyint not null,
   locked_by varchar(100) null,
   locked_on datetime not null,
   version_ts datetime not null,
   primary key (identifier)
);
create table workflow_history (
   id varchar(36) not null,
   creation_date datetime null,
   made_by varchar(255) null,
   change_desc text null,
   workflowtask_id varchar(36) null,
   workflow_action_id varchar(36) null,
   workflow_step_id varchar(36) null,
   primary key (id)
);
create table host_variable (
   id varchar(36) not null,
   host_id varchar(36) null,
   variable_name varchar(255) null,
   variable_key varchar(255) null,
   variable_value varchar(255) null,
   user_id varchar(255) null,
   last_mod_date datetime null,
   primary key (id)
);
create table links (
   inode varchar(36) not null,
   show_on_menu tinyint null,
   title varchar(255) null,
   mod_date datetime null,
   mod_user varchar(100) null,
   sort_order int null,
   friendly_name varchar(255) null,
   protocal varchar(100) null,
   url varchar(255) null,
   target varchar(100) null,
   internal_link_identifier varchar(36) null,
   link_type varchar(255) null,
   link_code text null,
   identifier varchar(36) null,
   primary key (inode)
);
create table user_proxy (
   inode varchar(36) not null,
   user_id varchar(255) null,
   prefix varchar(255) null,
   suffix varchar(255) null,
   title varchar(255) null,
   school varchar(255) null,
   how_heard varchar(255) null,
   company varchar(255) null,
   long_lived_cookie varchar(255) null,
   website varchar(255) null,
   graduation_year int null,
   organization varchar(255) null,
   mail_subscription tinyint null,
   var1 varchar(255) null,
   var2 varchar(255) null,
   var3 varchar(255) null,
   var4 varchar(255) null,
   var5 varchar(255) null,
   var6 varchar(255) null,
   var7 varchar(255) null,
   var8 varchar(255) null,
   var9 varchar(255) null,
   var10 varchar(255) null,
   var11 varchar(255) null,
   var12 varchar(255) null,
   var13 varchar(255) null,
   var14 varchar(255) null,
   var15 varchar(255) null,
   var16 varchar(255) null,
   var17 varchar(255) null,
   var18 varchar(255) null,
   var19 varchar(255) null,
   var20 varchar(255) null,
   var21 varchar(255) null,
   var22 varchar(255) null,
   var23 varchar(255) null,
   var24 varchar(255) null,
   var25 varchar(255) null,
   last_result int null,
   last_message varchar(255) null,
   no_click_tracking tinyint null,
   cquestionid varchar(255) null,
   cqanswer varchar(255) null,
   chapter_officer varchar(255) null,
   primary key (inode),
   unique (user_id)
);
create table chain_state_parameter (
   id numeric(19,0) identity not null,
   chain_state_id numeric(19,0) not null,
   name varchar(255) not null,
   value varchar(255) not null,
   primary key (id)
);
create table field (
   inode varchar(36) not null,
   structure_inode varchar(255) null,
   field_name varchar(255) null,
   field_type varchar(255) null,
   field_relation_type varchar(255) null,
   field_contentlet varchar(255) null,
   required tinyint null,
   indexed tinyint null,
   listed tinyint null,
   velocity_var_name varchar(255) null,
   sort_order int null,
   field_values text null,
   regex_check varchar(255) null,
   hint varchar(255) null,
   default_value varchar(255) null,
   fixed tinyint null,
   read_only tinyint null,
   searchable tinyint null,
   unique_ tinyint null,
   mod_date datetime null,
   primary key (inode)
);
create table relationship (
   inode varchar(36) not null,
   parent_structure_inode varchar(255) null,
   child_structure_inode varchar(255) null,
   parent_relation_name varchar(255) null,
   child_relation_name varchar(255) null,
   relation_type_value varchar(255) null,
   cardinality int null,
   parent_required tinyint null,
   child_required tinyint null,
   fixed tinyint null,
   primary key (inode)
);
create table folder (
   inode varchar(36) not null,
   name varchar(255) null,
   title varchar(255) not null,
   show_on_menu tinyint null,
   sort_order int null,
   files_masks varchar(255) null,
   identifier varchar(36) null,
   default_file_type varchar(36) null,
   mod_date datetime null,
   primary key (inode)
);
create table clickstream_404 (
   clickstream_404_id numeric(19,0) identity not null,
   referer_uri varchar(255) null,
   query_string text null,
   request_uri varchar(255) null,
   user_id varchar(255) null,
   host_id varchar(36) null,
   timestampper datetime null,
   primary key (clickstream_404_id)
);
create table cms_layout (
   id varchar(36) not null,
   layout_name varchar(255) not null,
   description varchar(255) null,
   tab_order int null,
   primary key (id)
);
create table field_variable (
   id varchar(36) not null,
   field_id varchar(36) null,
   variable_name varchar(255) null,
   variable_key varchar(255) null,
   variable_value text null,
   user_id varchar(255) null,
   last_mod_date datetime null,
   primary key (id)
);
create table report_parameter (
   inode varchar(36) not null,
   report_inode varchar(36) null,
   parameter_description varchar(1000) null,
   parameter_name varchar(255) null,
   class_type varchar(250) null,
   default_value varchar(4000) null,
   primary key (inode),
   unique (report_inode, parameter_name)
);
create table chain (
   id numeric(19,0) identity not null,
   key_name varchar(255) null unique,
   name varchar(255) not null,
   success_value varchar(255) not null,
   failure_value varchar(255) not null,
   primary key (id)
);
create table link_version_info (
   identifier varchar(36) not null,
   working_inode varchar(36) not null,
   live_inode varchar(36) null,
   deleted tinyint not null,
   locked_by varchar(100) null,
   locked_on datetime null,
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
   title varchar(255) null,
   firstname varchar(100) null,
   middlename varchar(100) null,
   lastname varchar(100) null,
   emailaddress varchar(100) null,
   birthdaytypesearch varchar(100) null,
   birthday datetime null,
   birthdayfrom datetime null,
   birthdayto datetime null,
   lastlogintypesearch varchar(100) null,
   lastloginsince varchar(100) null,
   loginfrom datetime null,
   loginto datetime null,
   createdtypesearch varchar(100) null,
   createdsince varchar(100) null,
   createdfrom datetime null,
   createdto datetime null,
   lastvisittypesearch varchar(100) null,
   lastvisitsince varchar(100) null,
   lastvisitfrom datetime null,
   lastvisitto datetime null,
   city varchar(100) null,
   state varchar(100) null,
   country varchar(100) null,
   zip varchar(100) null,
   cell varchar(100) null,
   phone varchar(100) null,
   fax varchar(100) null,
   active_ varchar(255) null,
   tagname varchar(255) null,
   var1 varchar(255) null,
   var2 varchar(255) null,
   var3 varchar(255) null,
   var4 varchar(255) null,
   var5 varchar(255) null,
   var6 varchar(255) null,
   var7 varchar(255) null,
   var8 varchar(255) null,
   var9 varchar(255) null,
   var10 varchar(255) null,
   var11 varchar(255) null,
   var12 varchar(255) null,
   var13 varchar(255) null,
   var14 varchar(255) null,
   var15 varchar(255) null,
   var16 varchar(255) null,
   var17 varchar(255) null,
   var18 varchar(255) null,
   var19 varchar(255) null,
   var20 varchar(255) null,
   var21 varchar(255) null,
   var22 varchar(255) null,
   var23 varchar(255) null,
   var24 varchar(255) null,
   var25 varchar(255) null,
   categories varchar(255) null,
   primary key (inode)
);
create table inode (
   inode varchar(36) not null,
   owner varchar(255) null,
   idate datetime null,
   type varchar(64) null,
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
create index idx_chain_link_code_classname on chain_link_code (class_name);
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
create index idx_chain_key_name on chain (key_name);
alter table user_filter add constraint fke042126c5fb51eb foreign key (inode) references inode;
create index idx_index_1 on inode (type);
--mssql
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

alter table chain_state add constraint fk_state_chain foreign key (chain_id) references chain(id);
alter table chain_state add constraint fk_state_code foreign key (link_code_id) references chain_link_code(id);
alter table chain_state_parameter add constraint fk_parameter_state foreign key (chain_state_id) references chain_state(id);

alter table permission add constraint permission_role_fk foreign key (roleid) references cms_role(id);

alter table contentlet add constraint FK_structure_inode foreign key (structure_inode) references structure(inode);

ALTER TABLE structure ALTER COLUMN fixed tinyint NOT NULL;
alter table structure add CONSTRAINT [DF_structure_fixed]  DEFAULT ((0)) for fixed;

ALTER TABLE field ALTER COLUMN fixed tinyint NOT NULL;
ALTER TABLE field ALTER COLUMN read_only tinyint  NOT NULL;
ALTER TABLE campaign ALTER COLUMN active tinyint NOT NULL;
alter table field add CONSTRAINT [DF_field_fixed]  DEFAULT ((0)) for fixed;
alter table field add CONSTRAINT [DF_field_read_only]  DEFAULT ((0)) for read_only;

insert into User_ (userId, companyId, createDate, password_, passwordEncrypted, passwordReset, firstName, middleName, lastName, male, birthday, emailAddress, skinId, dottedSkins, roundedSkins, greeting, layoutIds, loginDate, failedLoginAttempts, agreedToTermsOfUse, active_) values ('dotcms.org.default', 'default', GetDate(), 'password', '0', '0', '', '', '', '1', '19700101', 'default@dotcms.org', '01', '0', '0', 'Welcome!', '', GetDate(), 0, '0', '1');

create index addres_userid_index on address(userid);
create index tag_user_id_index on tag(user_id);
create index tag_is_persona_index on tag(persona);
create index tag_inode_tagid on tag_inode(tag_id);
create index tag_inode_inode on tag_inode(inode);
CREATE TABLE dist_journal
	   (
       id bigint NOT NULL IDENTITY (1, 1),
       object_to_index VARCHAR(1024) NOT NULL,
       serverid varchar(64) NOT NULL,
       journal_type int NOT NULL,
       time_entered datetime NOT NULL
       ) ;

ALTER TABLE dist_journal ADD CONSTRAINT
       PK_dist_journal PRIMARY KEY CLUSTERED
       (
       id
       );


ALTER TABLE dist_journal ADD CONSTRAINT
       IX_dist_journal UNIQUE NONCLUSTERED
       (
       object_to_index,
       serverid,
       journal_type
       );
CREATE TABLE dist_process ( id bigint NOT NULL IDENTITY (1, 1), object_to_index varchar(1024) NOT NULL, serverid varchar(64) NOT NULL, journal_type int NOT NULL, time_entered datetime NOT NULL ) ;
ALTER TABLE dist_process ADD CONSTRAINT PK_dist_process PRIMARY KEY CLUSTERED ( id);

create table plugin_property (
   plugin_id varchar(255) not null,
   propkey varchar(255) not null,
   original_value varchar(255) not null,
   current_value varchar(255) not null,
   primary key (plugin_id, propkey)
);

alter table plugin_property add constraint fk_plugin_plugin_property foreign key (plugin_id) references plugin(id);

CREATE TABLE dist_reindex_journal ( id bigint NOT NULL IDENTITY (1, 1), inode_to_index varchar(100) NOT NULL,ident_to_index varchar(100) NOT NULL, serverid varchar(64), priority int NOT NULL, time_entered datetime DEFAULT getDate(), index_val varchar(325),dist_action integer NOT NULL DEFAULT 1);

CREATE INDEX dist_reindex_index1 on dist_reindex_journal (inode_to_index);
CREATE INDEX dist_reindex_index2 on dist_reindex_journal (dist_action);
CREATE INDEX dist_reindex_index3 on dist_reindex_journal (serverid);
CREATE INDEX dist_reindex_index4 on dist_reindex_journal (ident_to_index,serverid);
CREATE INDEX dist_reindex_index on dist_reindex_journal (serverid,dist_action);
CREATE INDEX dist_reindex_index5 ON dist_reindex_journal (priority, time_entered);
CREATE INDEX dist_reindex_index6 ON dist_reindex_journal (priority);

ALTER TABLE dist_reindex_journal ADD CONSTRAINT PK_dist_reindex_journal PRIMARY KEY CLUSTERED ( id);

CREATE TABLE quartz_log ( id bigint NOT NULL IDENTITY (1, 1), JOB_NAME varchar(255) NOT NULL, serverid varchar(64) , time_started datetime NOT NULL, primary key (id));

CREATE TRIGGER check_role_key_uniqueness
ON cms_role
FOR INSERT, UPDATE
AS
DECLARE @c varchar(100)
SELECT @c = count(*)
FROM cms_role e INNER JOIN inserted i ON i.role_key = e.role_key WHERE i.role_key IS NOT NULL AND i.id <> e.id
IF (@c > 0)
BEGIN
   RAISERROR ('Duplicated role key.', 16, 1)
   ROLLBACK TRANSACTION
END;

CREATE TRIGGER check_identifier_host_inode
ON identifier
FOR INSERT, UPDATE AS
DECLARE @assetType varchar(10)
DECLARE @hostInode varchar(50)
DECLARE cur_Inserted cursor
LOCAL FAST_FORWARD for
 Select asset_type, host_inode
 from inserted
 for Read Only
open cur_Inserted
fetch next from cur_Inserted into @assetType,@hostInode
while @@FETCH_STATUS <> -1
BEGIN
 IF(@assetType <> 'content' AND (@hostInode is null OR @hostInode = ''))
 BEGIN
	RAISERROR (N'Cannot insert/update a null or empty host inode for this kind of identifier', 10, 1)
	ROLLBACK WORK
 END
fetch next from cur_Inserted into @assetType,@hostInode
END;

ALTER TABLE cms_role ADD CONSTRAINT IX_cms_role2 UNIQUE NONCLUSTERED (db_fqn);
alter table cms_role add constraint fkcms_role_parent foreign key (parent) references cms_role;

ALTER TABLE users_cms_roles ADD CONSTRAINT IX_cms_role UNIQUE NONCLUSTERED (role_id, user_id);
alter table users_cms_roles add constraint fkusers_cms_roles1 foreign key (role_id) references cms_role;
alter table users_cms_roles add constraint fkusers_cms_roles2 foreign key (user_id) references user_;

ALTER TABLE cms_layout ADD CONSTRAINT IX_cms_layout UNIQUE NONCLUSTERED (layout_name);

ALTER TABLE portlet ADD CONSTRAINT IX_portletid UNIQUE NONCLUSTERED (portletid);

ALTER TABLE cms_layouts_portlets ADD CONSTRAINT IX_cms_layouts_portlets UNIQUE NONCLUSTERED (portlet_id, layout_id);
alter table cms_layouts_portlets add constraint fklcms_layouts_portlets foreign key (layout_id) references cms_layout;

ALTER TABLE layouts_cms_roles ADD CONSTRAINT IX_layouts_cms_roles UNIQUE NONCLUSTERED (role_id, layout_id);
alter table layouts_cms_roles add constraint fklayouts_cms_roles1 foreign key (role_id) references cms_role;
alter table layouts_cms_roles add constraint fklayouts_cms_roles2 foreign key (layout_id) references cms_layout;

ALTER TABLE dot_containers add constraint containers_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE template add constraint template_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE htmlpage add constraint htmlpage_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE file_asset add constraint file_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE contentlet add constraint content_identifier_fk foreign key (identifier) references identifier(id);
ALTER TABLE links add constraint links_identifier_fk foreign key (identifier) references identifier(id);


create table dist_reindex_lock (dummy int not null);
ALTER TABLE dist_reindex_lock ADD CONSTRAINT PK_dist_reindex_lock PRIMARY KEY CLUSTERED (dummy);
create table dist_lock (dummy int not null);
ALTER TABLE dist_lock ADD CONSTRAINT PK_dist_lock PRIMARY KEY CLUSTERED (dummy);
insert into dist_reindex_lock (dummy) values (1);
insert into dist_lock (dummy) values (1);

create table import_audit (
	id bigint not null,
	start_date datetime,
	userid varchar(255),
	filename varchar(512),
	status int,
	last_inode varchar(100),
	records_to_import bigint,
	serverid varchar(255),
	primary key (id)
	);

alter table category alter column category_velocity_var_name varchar(255) not null;

alter table import_audit add warnings text,
	errors text,
	results text,
	messages text;

alter table structure add CONSTRAINT [DF_structure_host] DEFAULT 'SYSTEM_HOST' for host;
alter table structure add CONSTRAINT [DF_structure_folder] DEFAULT 'SYSTEM_FOLDER' for folder;
alter table structure add CONSTRAINT [CK_structure_host] CHECK(host <> '' AND host IS NOT NULL)
alter table structure add constraint fk_structure_folder foreign key (folder) references folder(inode);
alter table structure alter column velocity_var_name varchar(255) not null;
alter table structure add constraint unique_struct_vel_var_name unique (velocity_var_name);

CREATE TRIGGER structure_host_folder_trigger
ON structure
FOR INSERT, UPDATE AS
DECLARE @newFolder varchar(100)
DECLARE @newHost varchar(100)
DECLARE @folderInode varchar(100)
DECLARE @hostInode varchar(100)
DECLARE cur_Inserted cursor
LOCAL FAST_FORWARD for
 Select folder, host
 from inserted
 for Read Only
open cur_Inserted
fetch next from cur_Inserted into @newFolder,@newHost
while @@FETCH_STATUS <> -1
BEGIN
   IF (@newHost <> 'SYSTEM_HOST' AND @newFolder <> 'SYSTEM_FOLDER')
   BEGIN
	  SELECT @hostInode = identifier.host_inode, @folderInode = folder.inode from folder,identifier where folder.identifier = identifier.id and folder.inode = @newFolder
      IF (@folderInode IS NULL OR @folderInode = '' OR @newHost <> @hostInode)
      BEGIN
	    RAISERROR (N'Cannot assign host/folder to structure, folder does not belong to given host', 10, 1)
	    ROLLBACK WORK
	  END
  END
fetch next from cur_Inserted into @newFolder,@newHost
END;

CREATE PROCEDURE load_records_to_index(@server_id VARCHAR(100), @records_to_fetch INT, @priority_level INT)
AS
BEGIN
WITH cte AS (
  SELECT TOP(@records_to_fetch) *
  FROM dist_reindex_journal WITH (ROWLOCK, READPAST, UPDLOCK)
  WHERE serverid IS NULL
  AND priority <= @priority_level
  ORDER BY priority ASC)
UPDATE cte
  SET serverid=@server_id
OUTPUT
  INSERTED.*
END;

CREATE Trigger check_file_versions
ON file_asset
FOR DELETE AS
 DECLARE @totalCount int
 DECLARE @identifier varchar(100)
 DECLARE file_cur_Deleted cursor LOCAL FAST_FORWARD for
 Select identifier
  from deleted
  for Read Only
 open file_cur_Deleted
 fetch next from file_cur_Deleted into @identifier
 while @@FETCH_STATUS <> -1
 BEGIN
 select @totalCount = count(*) from file_asset where identifier = @identifier
 IF (@totalCount = 0)
  BEGIN
    DELETE from identifier where id = @identifier
  END
fetch next from file_cur_Deleted into @identifier
END;
CREATE Trigger check_content_versions
ON contentlet
FOR DELETE AS
 DECLARE @totalCount int
 DECLARE @identifier varchar(100)
 DECLARE content_cur_Deleted cursor LOCAL FAST_FORWARD for
 Select identifier
  from deleted
  for Read Only
 open content_cur_Deleted
 fetch next from content_cur_Deleted into @identifier
 while @@FETCH_STATUS <> -1
 BEGIN
 select @totalCount = count(*) from contentlet where identifier = @identifier
 IF (@totalCount = 0)
  BEGIN
   DELETE from identifier where id = @identifier
  END
fetch next from content_cur_Deleted into @identifier
END;

CREATE Trigger check_link_versions
ON links
FOR DELETE AS
 DECLARE @totalCount int
 DECLARE @identifier varchar(100)
 DECLARE link_cur_Deleted cursor LOCAL FAST_FORWARD for
 Select identifier
  from deleted
  for Read Only
 open link_cur_Deleted
 fetch next from link_cur_Deleted into @identifier
 while @@FETCH_STATUS <> -1
 BEGIN
 select @totalCount = count(*) from links where identifier = @identifier
 IF (@totalCount = 0)
  BEGIN
   DELETE from identifier where id = @identifier
  END
fetch next from link_cur_Deleted into @identifier
END;

CREATE Trigger check_container_versions
ON dot_containers
FOR DELETE AS
 DECLARE @totalCount int
 DECLARE @identifier varchar(100)
 DECLARE container_cur_Deleted cursor LOCAL FAST_FORWARD for
 Select identifier
  from deleted
  for Read Only
 open container_cur_Deleted
 fetch next from container_cur_Deleted into @identifier
 while @@FETCH_STATUS <> -1
 BEGIN
 select @totalCount = count(*) from dot_containers where identifier = @identifier
 IF (@totalCount = 0)
  BEGIN
   DELETE from identifier where id = @identifier
  END
fetch next from container_cur_Deleted into @identifier
END;


CREATE Trigger check_template_versions
ON template
FOR DELETE AS
 DECLARE @totalCount int
 DECLARE @identifier varchar(100)
 DECLARE template_cur_Deleted cursor LOCAL FAST_FORWARD for
 Select identifier
  from deleted
  for Read Only
 open template_cur_Deleted
 fetch next from template_cur_Deleted into @identifier
 while @@FETCH_STATUS <> -1
 BEGIN
 select @totalCount = count(*) from template where identifier = @identifier
 IF (@totalCount = 0)
  BEGIN
   DELETE from identifier where id = @identifier
  END
fetch next from template_cur_Deleted into @identifier
END;

CREATE Trigger check_htmlpage_versions
ON htmlpage
FOR DELETE AS
 DECLARE @totalCount int
 DECLARE @identifier varchar(100)
 DECLARE htmlpage_cur_Deleted cursor LOCAL FAST_FORWARD for
 Select identifier
  from deleted
  for Read Only
 open htmlpage_cur_Deleted
 fetch next from htmlpage_cur_Deleted into @identifier
 while @@FETCH_STATUS <> -1
 BEGIN
 select @totalCount = count(*) from htmlpage where identifier = @identifier
 IF (@totalCount = 0)
  BEGIN
   DELETE from identifier where id = @identifier
  END
fetch next from htmlpage_cur_Deleted into @identifier
END;

CREATE Trigger check_identifier_parent_path
 ON identifier
 FOR INSERT,UPDATE AS
 DECLARE @folderId varchar(100)
 DECLARE @id varchar(100)
 DECLARE @assetType varchar(100)
 DECLARE @parentPath varchar(255)
 DECLARE @hostInode varchar(100)
 DECLARE cur_Inserted cursor LOCAL FAST_FORWARD for
 Select id,asset_type,parent_path,host_inode
  from inserted
  for Read Only
 open cur_Inserted
 fetch next from cur_Inserted into @id,@assetType,@parentPath,@hostInode
 while @@FETCH_STATUS <> -1
 BEGIN
  IF(@parentPath <>'/' AND @parentPath <>'/System folder')
  BEGIN
    select @folderId = id from identifier where asset_type='folder' and host_inode = @hostInode and parent_path+asset_name+'/' = @parentPath and id <>@id
    IF (@folderId IS NULL)
     BEGIN
       RAISERROR (N'Cannot insert/update for this path does not exist for the given host', 10, 1)
       ROLLBACK WORK
     END
  END
 fetch next from cur_Inserted into @id,@assetType,@parentPath,@hostInode
END;

alter table structure add constraint fk_structure_host foreign key (host) references identifier(id);

create index idx_template3 on template (title);

CREATE INDEX idx_contentlet_4 ON contentlet (structure_inode);

CREATE INDEX idx_contentlet_identifier ON contentlet (identifier);

ALTER TABLE Folder add constraint folder_identifier_fk foreign key (identifier) references identifier(id);
--ALTER TABLE dot_containers add constraint structure_fk foreign key (structure_inode) references structure(inode);
ALTER TABLE htmlpage add constraint template_id_fk foreign key (template_id) references identifier(id);

CREATE Trigger check_template_identifier
ON htmlpage
FOR INSERT,UPDATE AS
DECLARE @templateId varchar(100)
DECLARE @tempIdentifier varchar(100)
DECLARE htmlpage_cur_Inserted cursor LOCAL FAST_FORWARD for
 Select template_id
 from inserted
 for Read Only
open htmlpage_cur_Inserted
fetch next from htmlpage_cur_Inserted into @templateId
while @@FETCH_STATUS <> -1
BEGIN
 select @tempIdentifier = id from identifier where asset_type='template' and id = @templateId
 IF (@tempIdentifier IS NULL)
 BEGIN
   RAISERROR (N'Template Id should be the identifier of a template', 10, 1)
   ROLLBACK WORK
 END
fetch next from htmlpage_cur_Inserted into @templateId
END;

CREATE Trigger folder_identifier_check
ON folder
FOR DELETE AS
DECLARE @totalCount int
DECLARE @identifier varchar(100)
DECLARE folder_cur_Deleted cursor LOCAL FAST_FORWARD for
 Select identifier
 from deleted
 for Read Only
 open folder_cur_Deleted
 fetch next from folder_cur_Deleted into @identifier
 while @@FETCH_STATUS <> -1
  BEGIN
   select @totalCount = count(*) from folder where identifier = @identifier
   IF (@totalCount = 0)
   BEGIN
     DELETE from identifier where id = @identifier
   END
   fetch next from folder_cur_Deleted into @identifier
END;

alter table contentlet add constraint fk_user_contentlet foreign key (mod_user) references user_(userid);
alter table htmlpage add constraint fk_user_htmlpage foreign key (mod_user) references user_(userid);
alter table dot_containers add constraint fk_user_containers foreign key (mod_user) references user_(userid);
alter table template add constraint fk_user_template foreign key (mod_user) references user_(userid);
alter table file_asset add constraint fk_user_file_asset foreign key (mod_user) references user_(userid);
alter table links add constraint fk_user_links foreign key (mod_user) references user_(userid);

create index idx_template_id on template_containers(template_id);
alter table template_containers add constraint FK_template_id foreign key (template_id) references identifier(id);
alter table template_containers add constraint FK_container_id foreign key (container_id) references identifier(id);

CREATE Trigger check_child_assets
on identifier
FOR DELETE AS
DECLARE @pathCount int
DECLARE @identifier varchar(100)
DECLARE @assetType varchar(100)
DECLARE @assetName varchar(255)
DECLARE @parentPath varchar(255)
DECLARE @hostInode varchar(100)
DECLARE cur_Deleted cursor LOCAL FAST_FORWARD for
 Select id,asset_type,asset_name,parent_path,host_inode
  from deleted
  for Read Only
 open cur_Deleted
 fetch next from cur_Deleted into @identifier,@assetType,@assetName,@parentPath,@hostInode
 while @@FETCH_STATUS <> -1
 BEGIN
   IF(@assetType='folder')
   BEGIN
     select @pathCount = count(*) from identifier where parent_path= @parentPath+@assetName+'/'  and host_inode = @hostInode
   END
   IF(@assetType='contentlet')
   BEGIN
     select @pathCount = count(*) from identifier where host_inode = @identifier
   END
   IF (@pathCount > 0)
    BEGIN
     RAISERROR (N'Cannot delete as this path has children', 10, 1)
     ROLLBACK WORK
    END
fetch next from cur_Deleted into @identifier,@assetType,@assetName,@parentPath,@hostInode
END;

CREATE PROCEDURE renameFolderChildren @oldPath varchar(255),@newPath varchar(255),@hostInode varchar(100) AS
DECLARE @newFolderPath varchar(255)
DECLARE @oldFolderPath varchar(255)
DECLARE @assetName varchar(255)
DECLARE @folderPathLength INT
DECLARE @errorMsg VARCHAR(1000)
UPDATE identifier SET  parent_path  = @newPath where parent_path = @oldPath and host_inode = @hostInode
DECLARE folder_data_cursor CURSOR LOCAL FAST_FORWARD for
select asset_name from identifier where asset_type='folder' and parent_path = @newPath and host_inode = @hostInode
OPEN folder_data_cursor
FETCH NEXT FROM folder_data_cursor INTO @assetName
while @@FETCH_STATUS <> -1
BEGIN
	 SET @folderPathLength = 0
     SET @newFolderPath = @newPath + @assetName + '/'
     SET @folderPathLength = LEN(@newPath) + LEN(@assetName) + 1
     IF (@folderPathLength > 255)
        BEGIN
	        SET @errorMsg = 'Folder path ' + @newPath + @assetName + '/' + ' is longer than 255 characters'
	        RAISERROR (@errorMsg, 16, 1)
	        ROLLBACK WORK
	        RETURN
        END 
     SET @oldFolderPath = @oldPath + @assetName + '/'
     EXEC renameFolderChildren @oldFolderPath,@newFolderPath,@hostInode
fetch next from folder_data_cursor into @assetName
END;

CREATE Trigger rename_folder_assets_trigger
on Folder
FOR UPDATE AS
DECLARE @oldPath varchar(255)
DECLARE @newPath varchar(255)
DECLARE @newName varchar(255)
DECLARE @hostInode varchar(100)
DECLARE @ident varchar(100)
DECLARE @folderPathLength INT
DECLARE @errorMsg VARCHAR(1000)
DECLARE folder_cur_Updated cursor LOCAL FAST_FORWARD for
 Select inserted.identifier,inserted.name
 from inserted join deleted on (inserted.inode=deleted.inode)
 where inserted.name<>deleted.name
 for Read Only
open folder_cur_Updated
fetch next from folder_cur_Updated into @ident,@newName
while @@FETCH_STATUS <> -1
BEGIN
  SET @folderPathLength = 0
  SELECT @oldPath = parent_path+asset_name+'/',@newPath = parent_path +@newName+'/',@hostInode = host_inode from identifier where id = @ident
  SET @folderPathLength = LEN(@newPath)
  IF (@folderPathLength > 255)
    BEGIN
	    SET @errorMsg = 'Folder path ' + @newPath + ' is longer than 255 characters'
	    RAISERROR (@errorMsg, 16, 1)
	    ROLLBACK WORK
	    RETURN
    END 
  UPDATE identifier SET asset_name = @newName where id = @ident
  EXEC renameFolderChildren @oldPath,@newPath,@hostInode
fetch next from folder_cur_Updated into @ident,@newName
END;

CREATE FUNCTION dbo.dotFolderPath(@parent_path varchar(36), @asset_name varchar(36))
RETURNS varchar(36)
BEGIN
  IF(@parent_path='/System folder')
  BEGIN
    RETURN '/'
  END
  RETURN @parent_path+@asset_name+'/'
END;

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
--alter table workflowtask_files add constraint FK_task_file_inode foreign key (file_inode) references file_asset(inode);
alter table workflow_comment add constraint workflowtask_id_comment_FK foreign key (workflowtask_id) references workflow_task(id);
alter table workflow_history add constraint workflowtask_id_history_FK foreign key (workflowtask_id) references workflow_task(id);

alter table contentlet add constraint fk_contentlet_lang foreign key (language_id) references language(id);

create table workflow_scheme(
    id varchar(36) primary key,
    name varchar(255) not null,
    description varchar(255),
    archived tinyint default 0,
    mandatory tinyint default 0,
    default_scheme tinyint default 0,
    entry_action_id varchar(36),
    mod_date datetime
);
alter table workflow_scheme add constraint unique_workflow_scheme_name unique (name);

create table workflow_step(
    id varchar(36) primary key,
    name varchar(255) not null,
    scheme_id varchar(36) references workflow_scheme(id),
    my_order int default 0,
    resolved tinyint default 0,
    escalation_enable tinyint default 0,
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
    assignable tinyint default 0,
    commentable tinyint default 0,
    requires_checkout tinyint default 0,
    icon varchar(255) default 'defaultWfIcon',
    use_role_hierarchy_assign tinyint default 0
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

create table workflow_action_class_pars(
    id varchar(36) primary key,
    workflow_action_class_id varchar(36) not null references workflow_action_class(id),
    "key" varchar(255) not null,
    value text
);
create index workflow_idx_action_class_param_action on
    workflow_action_class_pars(workflow_action_class_id);


create table workflow_scheme_x_structure(
    id varchar(36) primary key,
    scheme_id varchar(36) references workflow_scheme(id),
    structure_id varchar(36) references structure(inode)
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

ALTER TABLE tag add CONSTRAINT [DF_tag_host] DEFAULT 'SYSTEM_HOST' for host_id;
alter table tag add constraint tag_tagname_host unique (tagname, host_id);
alter table tag_inode add constraint fk_tag_inode_tagid foreign key (tag_id) references tag (tag_id);

drop index tag_user_id_index on tag;
alter table tag alter column user_id text;


-- ****** Indicies Data Storage *******
create table indicies (
  index_name varchar(30) primary key,
  index_type varchar(16) not null unique
);
-- ****** Log Console Table *******
  CREATE TABLE log_mapper (
    enabled   	 numeric(1,0) not null,
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
   status_code bigint NOT NULL
);

alter table broken_link add CONSTRAINT fk_brokenl_content
    FOREIGN KEY (inode) REFERENCES contentlet(inode) ON DELETE CASCADE;

alter table broken_link add CONSTRAINT fk_brokenl_field
    FOREIGN KEY (field) REFERENCES field(inode) ON DELETE CASCADE;

-- ****** Content Publishing Framework *******
CREATE TABLE publishing_queue
(id bigint IDENTITY (1, 1)PRIMARY KEY NOT NULL,
operation numeric(19,0), asset VARCHAR(2000) NOT NULL,
language_id numeric(19,0) NOT NULL, entered_date DATETIME,
last_try DATETIME, num_of_tries numeric(19,0) NOT NULL DEFAULT 0,
in_error tinyint DEFAULT 0, last_results TEXT,
publish_date DATETIME, server_id VARCHAR(256),
type VARCHAR(256), bundle_id VARCHAR(256), target text);

CREATE TABLE publishing_queue_audit
(bundle_id VARCHAR(256) PRIMARY KEY NOT NULL,
status INTEGER,
status_pojo text,
status_updated DATETIME,
create_date DATETIME);

-- ****** Content Publishing Framework - End Point Management *******
CREATE TABLE publishing_end_point (
	id varchar(36) PRIMARY KEY,
	group_id varchar(700),
	server_name varchar(700) unique,
	address varchar(250),
	port varchar(10),
	protocol varchar(10),
	enabled tinyint DEFAULT 0,
	auth_key text,
	sending tinyint DEFAULT 0);

create table publishing_environment(
	id varchar(36) NOT NULL  primary key,
	name varchar(255) NOT NULL unique,
	push_to_all tinyint NOT NULL
);


create table sitesearch_audit (
    job_id varchar(36),
    job_name varchar(255) not null,
    fire_date DATETIME not null,
    incremental tinyint not null,
    start_date DATETIME,
    end_date DATETIME,
    host_list varchar(500) not null,
    all_hosts tinyint not null,
    lang_list varchar(500) not null,
    path varchar(500) not null,
    path_include tinyint not null,
    files_count numeric(19,0) not null,
    pages_count numeric(19,0) not null,
    urlmaps_count numeric(19,0) not null,
    index_name varchar(100) not null,
    primary key(job_id,fire_date)
);

drop table publishing_queue;

CREATE TABLE publishing_queue (
      id bigint IDENTITY (1, 1)PRIMARY KEY NOT NULL,
      operation numeric(19,0),
      asset VARCHAR(2000) NOT NULL,
      language_id numeric(19,0) NOT NULL,
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

create table publishing_bundle_environment(
	id varchar(36) NOT NULL primary key,
	bundle_id varchar(36) NOT NULL,
	environment_id varchar(36) NOT NULL
);

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

alter table publishing_bundle add force_push tinyint ;

CREATE INDEX idx_pub_qa_1 ON publishing_queue_audit (status);

--Cluster Tables

CREATE TABLE dot_cluster(cluster_id varchar(36), PRIMARY KEY (cluster_id) );
CREATE TABLE cluster_server(server_id varchar(36) NOT NULL, cluster_id varchar(36) NOT NULL, name varchar(100), ip_address varchar(39) NOT NULL, host varchar(36), cache_port SMALLINT, es_transport_tcp_port SMALLINT, es_network_port SMALLINT, es_http_port SMALLINT, key_ varchar(100), PRIMARY KEY (server_id) );
ALTER TABLE cluster_server add constraint fk_cluster_id foreign key (cluster_id) REFERENCES dot_cluster(cluster_id);
CREATE TABLE cluster_server_uptime(id varchar(36) NOT NULL, server_id varchar(36) NOT NULL, startup datetime null, heartbeat datetime null, PRIMARY KEY (id) );
ALTER TABLE cluster_server_uptime add constraint fk_cluster_server_id foreign key (server_id) REFERENCES cluster_server(server_id);

-- Notifications Table
create table notification(id varchar(36) NOT NULL,message text NOT NULL, notification_type varchar(100), notification_level varchar(100), user_id varchar(255) NOT NULL, time_sent DATETIME NOT NULL, was_read tinyint default 0, PRIMARY KEY (id));
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
create table sitelic(id varchar(36) primary key, serverid varchar(100), license text not null, lastping datetime not null);

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
	failed bit not null, 
	response varchar(2048), 
	action_id varchar(1024) not null,
	completed bit not null, 
	entered_date datetime not null,
	time_out_seconds bigint not null,
	PRIMARY KEY (server_action_id)
);

-- Rules Engine
create table dot_rule(id varchar(36) primary key,name varchar(255) not null,fire_on varchar(20),short_circuit tinyint default 0,parent_id varchar(36) not null,folder varchar(36) not null,priority int default 0,enabled tinyint default 0,mod_date datetime);
create table rule_condition_group(id varchar(36) primary key,rule_id varchar(36) references dot_rule(id),operator varchar(10) not null,priority int default 0,mod_date datetime);
create table rule_condition(id varchar(36) primary key,conditionlet text not null,condition_group varchar(36) references rule_condition_group(id),comparison varchar(36) not null,operator varchar(10) not null,priority int default 0,mod_date datetime);
create table rule_condition_value (id varchar(36) primary key,condition_id varchar(36) references rule_condition(id), paramkey varchar(255) not null, value text,priority int default 0);
create table rule_action (id varchar(36) primary key,rule_id varchar(36) references dot_rule(id),priority int default 0,actionlet text not null,mod_date datetime);
create table rule_action_pars(id varchar(36) primary key,rule_action_id varchar(36) references rule_action(id), paramkey varchar(255) not null,value text);
create index idx_rules_fire_on on dot_rule (fire_on);


-- Delete User
ALTER TABLE user_ ADD delete_in_progress TINYINT DEFAULT 0;
ALTER TABLE user_ ADD delete_date DATETIME NULL;