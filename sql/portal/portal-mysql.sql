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
	password_ varchar(100) null,
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
