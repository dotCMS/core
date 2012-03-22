create table ABContact (
	contactId varchar(100) not null primary key,
	userId varchar(100) not null,
	firstName varchar(100),
	middleName varchar(100),
	lastName varchar(100),
	nickName varchar(100),
	emailAddress varchar(100),
	homeStreet varchar(100),
	homeCity varchar(100),
	homeState varchar(100),
	homeZip varchar(100),
	homeCountry varchar(100),
	homePhone varchar(100),
	homeFax varchar(100),
	homeCell varchar(100),
	homePager varchar(100),
	homeTollFree varchar(100),
	homeEmailAddress varchar(100),
	businessCompany varchar(100),
	businessStreet varchar(100),
	businessCity varchar(100),
	businessState varchar(100),
	businessZip varchar(100),
	businessCountry varchar(100),
	businessPhone varchar(100),
	businessFax varchar(100),
	businessCell varchar(100),
	businessPager varchar(100),
	businessTollFree varchar(100),
	businessEmailAddress varchar(100),
	employeeNumber varchar(100),
	jobTitle varchar(100),
	jobClass varchar(100),
	hoursOfOperation long varchar,
	birthday timestamp,
	timeZoneId varchar(100),
	instantMessenger varchar(100),
	website varchar(100),
	comments long varchar
);

create table ABContacts_ABLists (
	contactId varchar(100) not null,
	listId varchar(100) not null
);

create table ABList (
	listId varchar(100) not null primary key,
	userId varchar(100) not null,
	name varchar(100)
);

create table Address (
	addressId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100),
	createDate timestamp,
	modifiedDate timestamp,
	className varchar(100),
	classPK varchar(100),
	description varchar(100),
	street1 varchar(100),
	street2 varchar(100),
	city varchar(100),
	state varchar(100),
	zip varchar(100),
	country varchar(100),
	phone varchar(100),
	fax varchar(100),
	cell varchar(100),
	priority integer
);

create table AdminConfig (
	configId varchar(100) not null primary key,
	companyId varchar(100) not null,
	type_ varchar(100),
	name varchar(100),
	config long varchar
);

create table BJEntries_BJTopics (
	entryId varchar(100) not null,
	topicId varchar(100) not null
);

create table BJEntries_BJVerses (
	entryId varchar(100) not null,
	verseId varchar(100) not null
);

create table BJEntry (
	entryId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	createDate timestamp,
	modifiedDate timestamp,
	name varchar(100),
	content long varchar,
	versesInput long varchar
);

create table BJTopic (
	topicId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	createDate timestamp,
	modifiedDate timestamp,
	name varchar(100),
	description long varchar
);

create table BJVerse (
	verseId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	name varchar(100)
);

create table BlogsCategory (
	categoryId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	createDate timestamp,
	modifiedDate timestamp,
	name varchar(100)
);

create table BlogsComments (
	commentsId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100),
	createDate timestamp,
	modifiedDate timestamp,
	entryId varchar(100),
	content long varchar
);

create table BlogsEntry (
	entryId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	createDate timestamp,
	modifiedDate timestamp,
	categoryId varchar(100),
	title varchar(100),
	content long varchar,
	displayDate timestamp,
	sharing char(1),
	commentable char(1),
	propsCount integer,
	commentsCount integer
);

create table BlogsLink (
	linkId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	createDate timestamp,
	modifiedDate timestamp,
	name varchar(100),
	url varchar(100)
);

create table BlogsProps (
	propsId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100),
	createDate timestamp,
	modifiedDate timestamp,
	entryId varchar(100),
	quantity integer
);

create table BlogsReferer (
	entryId varchar(100) not null,
	url varchar(100) not null,
	type_ varchar(100) not null,
	quantity integer,
	primary key (entryId, url, type_)
);

create table BlogsUser (
	userId varchar(100) not null primary key,
	companyId varchar(100) not null,
	entryId varchar(100) not null,
	lastPostDate timestamp
);

create table BookmarksEntry (
	entryId varchar(100) not null primary key,
	userId varchar(100) not null,
	createDate timestamp,
	modifiedDate timestamp,
	folderId varchar(100),
	name varchar(100),
	url varchar(100),
	comments long varchar,
	visits integer
);

create table BookmarksFolder (
	folderId varchar(100) not null primary key,
	userId varchar(100) not null,
	createDate timestamp,
	modifiedDate timestamp,
	parentFolderId varchar(100),
	name varchar(100)
);

create table CalEvent (
	eventId varchar(100) not null primary key,
	groupId varchar(100) not null,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100),
	createDate timestamp,
	modifiedDate timestamp,
	title varchar(100),
	description long varchar,
	startDate timestamp,
	endDate timestamp,
	durationHour integer,
	durationMinute integer,
	allDay char(1),
	timeZoneSensitive char(1),
	type_ varchar(100),
	location varchar(100),
	street varchar(100),
	city varchar(100),
	state varchar(100),
	zip varchar(100),
	phone varchar(100),
	repeating char(1),
	recurrence long varchar,
	remindBy varchar(100),
	firstReminder integer,
	secondReminder integer
);

create table CalTask (
	taskId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	createDate timestamp,
	modifiedDate timestamp,
	title varchar(100),
	description long varchar,
	noDueDate char(1),
	dueDate timestamp,
	priority integer,
	status integer
);

create table Company (
	companyId varchar(100) not null primary key,
	key_ long varchar,
	portalURL varchar(100) not null,
	homeURL varchar(100) not null,
	mx varchar(100) not null,
	name varchar(100) not null,
	shortName varchar(100) not null,
	type_ varchar(100),
	size_ varchar(100),
	street varchar(100),
	city varchar(100),
	state varchar(100),
	zip varchar(100),
	phone varchar(100),
	fax varchar(100),
	emailAddress varchar(100),
	authType varchar(100),
	autoLogin char(1),
	strangers char(1)
);

create table Counter (
	name varchar(100) not null primary key,
	currentId integer
);

create table CyrusUser (
	userId varchar(100) not null primary key,
	password_ varchar(100) not null
);

create table CyrusVirtual (
	emailAddress varchar(100) not null primary key,
	userId varchar(100) not null
);

create table DLFileProfile (
	companyId varchar(100) not null,
	repositoryId varchar(100) not null,
	fileName varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100),
	versionUserId varchar(100) not null,
	versionUserName varchar(100),
	createDate timestamp,
	modifiedDate timestamp,
	readRoles varchar(100),
	writeRoles varchar(100),
	description long varchar,
	version double,
	size_ integer,
	primary key (companyId, repositoryId, fileName)
);

create table DLFileRank (
	companyId varchar(100) not null,
	userId varchar(100) not null,
	repositoryId varchar(100) not null,
	fileName varchar(100) not null,
	createDate timestamp,
	primary key (companyId, userId, repositoryId, fileName)
);

create table DLFileVersion (
	companyId varchar(100) not null,
	repositoryId varchar(100) not null,
	fileName varchar(100) not null,
	version double not null,
	userId varchar(100) not null,
	userName varchar(100),
	createDate timestamp,
	size_ integer,
	primary key (companyId, repositoryId, fileName, version)
);

create table DLRepository (
	repositoryId varchar(100) not null primary key,
	groupId varchar(100) not null,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100),
	createDate timestamp,
	modifiedDate timestamp,
	readRoles varchar(100),
	writeRoles varchar(100),
	name varchar(100),
	description long varchar,
	lastPostDate timestamp
);

create table Group_ (
	groupId varchar(100) not null primary key,
	companyId varchar(100) not null,
	parentGroupId varchar(100),
	name varchar(100),
	layoutIds varchar(100)
);

create table Groups_Roles (
	groupId varchar(100) not null,
	roleId varchar(100) not null
);

create table IGFolder (
	folderId varchar(100) not null primary key,
	groupId varchar(100) not null,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	createDate timestamp,
	modifiedDate timestamp,
	parentFolderId varchar(100),
	name varchar(100)
);

create table IGImage (
	imageId varchar(100) not null,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	createDate timestamp,
	modifiedDate timestamp,
	folderId varchar(100),
	description long varchar,
	height integer,
	width integer,
	size_ integer,
	primary key (imageId, companyId)
);

create table Image (
	imageId varchar(200) not null primary key,
	text_ long varchar not null
);

create table JournalArticle (
	articleId varchar(100) not null,
	version double not null,
	portletId varchar(100) not null,
	groupId varchar(100) not null,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100),
	createDate timestamp,
	modifiedDate timestamp,
	title varchar(100),
	content long varchar,
	type_ varchar(100),
	structureId varchar(100),
	templateId varchar(100),
	displayDate timestamp,
	expirationDate timestamp,
	approved char(1),
	approvedByUserId varchar(100),
	approvedByUserName varchar(100),
	primary key (articleId, version)
);

create table JournalStructure (
	structureId varchar(100) not null primary key,
	portletId varchar(100) not null,
	groupId varchar(100) not null,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100),
	createDate timestamp,
	modifiedDate timestamp,
	name varchar(100),
	description long varchar,
	xsd long varchar
);

create table JournalTemplate (
	templateId varchar(100) not null primary key,
	portletId varchar(100) not null,
	groupId varchar(100) not null,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100),
	createDate timestamp,
	modifiedDate timestamp,
	structureId varchar(100),
	name varchar(100),
	description long varchar,
	xsl long varchar,
	smallImage char(1),
	smallImageURL varchar(100)
);

create table Layer (
	layerId varchar(100) not null,
	skinId varchar(100) not null,
	href varchar(100),
	hrefHover varchar(100),
	background varchar(100),
	foreground varchar(100),
	negAlert varchar(100),
	posAlert varchar(100),
	primary key (layerId, skinId)
);

create table Layout (
	layoutId varchar(100) not null,
	userId varchar(100) not null,
	name varchar(100),
	columnOrder varchar(100),
	narrow1 long varchar,
	narrow2 long varchar,
	wide long varchar,
	stateMax long varchar,
	stateMin long varchar,
	modeEdit long varchar,
	modeHelp long varchar,
	primary key (layoutId, userId)
);

create table MailReceipt (
	receiptId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	createDate timestamp,
	modifiedDate timestamp,
	recipientName varchar(100),
	recipientAddress varchar(100),
	subject varchar(100),
	sentDate timestamp,
	readCount integer,
	firstReadDate timestamp,
	lastReadDate timestamp
);

create table MBMessage (
	messageId varchar(100) not null,
	topicId varchar(100) not null,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100),
	createDate timestamp,
	modifiedDate timestamp,
	threadId varchar(100),
	parentMessageId varchar(100),
	subject varchar(100),
	body long varchar,
	attachments char(1),
	anonymous char(1),
	primary key (messageId, topicId)
);

create table MBMessageFlag (
	topicId varchar(100) not null,
	messageId varchar(100) not null,
	userId varchar(100) not null,
	flag varchar(100),
	primary key (topicId, messageId, userId)
);

create table MBThread (
	threadId varchar(100) not null primary key,
	rootMessageId varchar(100),
	topicId varchar(100),
	messageCount integer,
	lastPostDate timestamp
);

create table MBTopic (
	topicId varchar(100) not null primary key,
	portletId varchar(100) not null,
	groupId varchar(100) not null,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100),
	createDate timestamp,
	modifiedDate timestamp,
	readRoles varchar(100),
	writeRoles varchar(100),
	name varchar(100),
	description long varchar,
	lastPostDate timestamp
);

create table NetworkAddress (
	addressId varchar(100) not null primary key,
	userId varchar(100) not null,
	createDate timestamp,
	modifiedDate timestamp,
	name varchar(100),
	url varchar(100),
	comments long varchar,
	content long varchar,
	status integer,
	lastUpdated timestamp,
	notifyBy varchar(100),
	interval_ integer,
	active_ char(1)
);

create table Note (
	noteId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100),
	createDate timestamp,
	modifiedDate timestamp,
	className varchar(100),
	classPK varchar(100),
	content long varchar
);

create table PasswordTracker (
	passwordTrackerId varchar(100) not null primary key,
	userId varchar(100) not null,
	createDate timestamp not null,
	password_ varchar(100) not null
);

create table PollsChoice (
	choiceId varchar(100) not null,
	questionId varchar(100) not null,
	description long varchar,
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
	userName varchar(100),
	createDate timestamp,
	modifiedDate timestamp,
	title varchar(100),
	description long varchar,
	expirationDate timestamp,
	lastVoteDate timestamp
);

create table PollsVote (
	questionId varchar(100) not null,
	userId varchar(100) not null,
	choiceId varchar(100) not null,
	voteDate timestamp,
	primary key (questionId, userId)
);

create table Portlet (
	portletId varchar(100) not null,
	groupId varchar(100) not null,
	companyId varchar(100) not null,
	defaultPreferences long varchar,
	narrow char(1),
	roles long varchar,
	active_ char(1),
	primary key (portletId, groupId, companyId)
);

create table PortletPreferences (
	portletId varchar(100) not null,
	userId varchar(100) not null,
	layoutId varchar(100) not null,
	preferences long varchar,
	primary key (portletId, userId, layoutId)
);

create table ProjFirm (
	firmId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100),
	createDate timestamp,
	modifiedDate timestamp,
	name varchar(100),
	description long varchar,
	url varchar(100)
);

create table ProjProject (
	projectId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100),
	createDate timestamp,
	modifiedDate timestamp,
	firmId varchar(100),
	code varchar(100),
	name varchar(100),
	description long varchar
);

create table ProjTask (
	taskId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100),
	createDate timestamp,
	modifiedDate timestamp,
	projectId varchar(100),
	name varchar(100),
	description long varchar,
	comments long varchar,
	estimatedDuration integer,
	estimatedEndDate timestamp,
	actualDuration integer,
	actualEndDate timestamp,
	status integer
);

create table ProjTime (
	timeId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100),
	createDate timestamp,
	modifiedDate timestamp,
	projectId varchar(100),
	taskId varchar(100),
	description long varchar,
	startDate timestamp,
	endDate timestamp
);

create table Release_ (
	releaseId varchar(100) not null primary key,
	createDate timestamp,
	modifiedDate timestamp,
	buildNumber integer,
	buildDate timestamp
);

create table Role_ (
	roleId varchar(100) not null primary key,
	companyId varchar(100) not null,
	name varchar(100) not null
);

create table Skin (
	skinId varchar(100) not null primary key,
	name varchar(100),
	imageId varchar(100),
	alphaLayerId varchar(100),
	alphaSkinId varchar(100),
	betaLayerId varchar(100),
	betaSkinId varchar(100),
	gammaLayerId varchar(100),
	gammaSkinId varchar(100),
	bgLayerId varchar(100),
	bgSkinId varchar(100)
);

create table ShoppingCart (
	cartId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	createDate timestamp,
	modifiedDate timestamp,
	itemIds long varchar,
	couponIds long varchar,
	altShipping integer
);

create table ShoppingCategory (
	categoryId varchar(100) not null primary key,
	companyId varchar(100) not null,
	createDate timestamp,
	modifiedDate timestamp,
	parentCategoryId varchar(100),
	name varchar(100)
);

create table ShoppingCoupon (
	couponId varchar(100) not null primary key,
	companyId varchar(100) not null,
	createDate timestamp,
	modifiedDate timestamp,
	name varchar(100),
	description long varchar,
	startDate timestamp,
	endDate timestamp,
	active_ char(1),
	limitCategories long varchar,
	limitSkus long varchar,
	minOrder double,
	discount double,
	discountType varchar(100)
);

create table ShoppingItem (
	itemId varchar(100) not null primary key,
	companyId varchar(100) not null,
	createDate timestamp,
	modifiedDate timestamp,
	categoryId varchar(100),
	sku varchar(100),
	name varchar(100),
	description long varchar,
	properties long varchar,
	supplierUserId varchar(100),
	fields_ char(1),
	fieldsQuantities long varchar,
	minQuantity integer,
	maxQuantity integer,
	price double,
	discount double,
	taxable char(1),
	shipping double,
	useShippingFormula char(1),
	requiresShipping char(1),
	stockQuantity integer,
	featured_ char(1),
	sale_ char(1),
	smallImage char(1),
	smallImageURL varchar(100),
	mediumImage char(1),
	mediumImageURL varchar(100),
	largeImage char(1),
	largeImageURL varchar(100)
);

create table ShoppingItemField (
	itemFieldId varchar(100) not null primary key,
	itemId varchar(100),
	name varchar(100),
	values_ long varchar,
	description long varchar
);

create table ShoppingItemPrice (
	itemPriceId varchar(100) not null primary key,
	itemId varchar(100),
	minQuantity integer,
	maxQuantity integer,
	price double,
	discount double,
	taxable char(1),
	shipping double,
	useShippingFormula char(1),
	status integer
);

create table ShoppingOrder (
	orderId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	createDate timestamp,
	modifiedDate timestamp,
	tax double,
	shipping double,
	altShipping varchar(100),
	requiresShipping char(1),
	couponIds long varchar,
	couponDiscount double,
	billingFirstName varchar(100),
	billingLastName varchar(100),
	billingEmailAddress varchar(100),
	billingCompany varchar(100),
	billingStreet varchar(100),
	billingCity varchar(100),
	billingState varchar(100),
	billingZip varchar(100),
	billingCountry varchar(100),
	billingPhone varchar(100),
	shipToBilling char(1),
	shippingFirstName varchar(100),
	shippingLastName varchar(100),
	shippingEmailAddress varchar(100),
	shippingCompany varchar(100),
	shippingStreet varchar(100),
	shippingCity varchar(100),
	shippingState varchar(100),
	shippingZip varchar(100),
	shippingCountry varchar(100),
	shippingPhone varchar(100),
	ccName varchar(100),
	ccType varchar(100),
	ccNumber varchar(100),
	ccExpMonth integer,
	ccExpYear integer,
	ccVerNumber varchar(100),
	comments long varchar,
	ppTxnId varchar(100),
	ppPaymentStatus varchar(100),
	ppPaymentGross double,
	ppReceiverEmail varchar(100),
	ppPayerEmail varchar(100),
	sendOrderEmail char(1),
	sendShippingEmail char(1)
);

create table ShoppingOrderItem (
	orderId varchar(100) not null,
	itemId varchar(100) not null,
	sku varchar(100),
	name varchar(100),
	description long varchar,
	properties long varchar,
	supplierUserId varchar(100),
	price double,
	quantity integer,
	shippedDate timestamp,
	primary key (orderId, itemId)
);

create table User_ (
	userId varchar(100) not null primary key,
	companyId varchar(100) not null,
	createDate timestamp,
	password_ varchar(100),
	passwordEncrypted char(1),
	passwordExpirationDate timestamp,
	passwordReset char(1),
	firstName varchar(100),
	middleName varchar(100),
	lastName varchar(100),
	nickName varchar(100),
	male char(1),
	birthday timestamp,
	emailAddress varchar(100),
	smsId varchar(100),
	aimId varchar(100),
	icqId varchar(100),
	msnId varchar(100),
	ymId varchar(100),
	favoriteActivity varchar(100),
	favoriteBibleVerse varchar(100),
	favoriteFood varchar(100),
	favoriteMovie varchar(100),
	favoriteMusic varchar(100),
	languageId varchar(100),
	timeZoneId varchar(100),
	skinId varchar(100),
	dottedSkins char(1),
	roundedSkins char(1),
	greeting varchar(100),
	resolution varchar(100),
	refreshRate varchar(100),
	layoutIds varchar(100),
	comments long varchar,
	loginDate timestamp,
	loginIP varchar(100),
	lastLoginDate timestamp,
	lastLoginIP varchar(100),
	failedLoginAttempts integer,
	agreedToTermsOfUse char(1),
	active_ char(1)
);

create table Users_Groups (
	userId varchar(100) not null,
	groupId varchar(100) not null
);

create table Users_ProjProjects (
	userId varchar(100) not null,
	projectId varchar(100) not null
);

create table Users_ProjTasks (
	userId varchar(100) not null,
	taskId varchar(100) not null
);

create table Users_Roles (
	userId varchar(100) not null,
	roleId varchar(100) not null
);

create table UserTracker (
	userTrackerId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	modifiedDate timestamp,
	remoteAddr varchar(100),
	remoteHost varchar(100),
	userAgent varchar(100)
);

create table UserTrackerPath (
	userTrackerPathId varchar(100) not null primary key,
	userTrackerId varchar(100) not null,
	path long varchar not null,
	pathDate timestamp not null
);

create table WikiDisplay (
	layoutId varchar(100) not null,
	userId varchar(100) not null,
	portletId varchar(100) not null,
	nodeId varchar(100) not null,
	showBorders char(1),
	primary key (layoutId, userId, portletId)
);

create table WikiNode (
	nodeId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100),
	createDate timestamp,
	modifiedDate timestamp,
	readRoles varchar(100),
	writeRoles varchar(100),
	name varchar(100),
	description long varchar,
	sharing char(1),
	lastPostDate timestamp
);

create table WikiPage (
	nodeId varchar(100) not null,
	title varchar(100) not null,
	version double not null,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100),
	createDate timestamp,
	content long varchar,
	format varchar(100),
	head char(1),
	primary key (nodeId, title, version)
);

--
-- Global
--

insert into Counter values ('com.liferay.portal.model.Address', 10);
insert into Counter values ('com.liferay.portal.model.Group', 20);
insert into Counter values ('com.liferay.portal.model.Role', 20);
insert into Counter values ('com.liferay.portal.model.User.liferay.com', 10);
insert into Counter values ('com.liferay.portlet.imagegallery.model.IGFolder', 20);
insert into Counter values ('com.liferay.portlet.imagegallery.model.IGImage.liferay.com', 42);
insert into Counter values ('com.liferay.portlet.polls.model.PollsQuestion', 10);
insert into Counter values ('com.liferay.portlet.shopping.model.ShoppingCategory', 20);
insert into Counter values ('com.liferay.portlet.shopping.ejb.ShoppingItem', 40);
insert into Counter values ('com.liferay.portlet.wiki.model.WikiNode', 10);

--
-- Liferay, LLC
--

insert into Company (companyId, portalURL, homeURL, mx, name, shortName, type_, size_, emailAddress, authType, autoLogin, strangers) values ('liferay.com', 'localhost', 'localhost', 'liferay.com', 'Liferay, LLC', 'Liferay', 'biz', '', 'test@liferay.com', 'emailAddress', '1', '1');
update Company set street = '1220 Brea Canyon Rd.', city = 'Diamond Bar', state = 'CA', zip = '91789' where companyId = 'liferay.com';

insert into IGFolder (folderId, groupId, companyId, userId, createDate, modifiedDate, parentFolderId, name) values ('11','3','liferay.com','liferay.com.1',current timestamp,current timestamp,'-1','COMMON');
insert into IGFolder (folderId, groupId, companyId, userId, createDate, modifiedDate, parentFolderId, name) values ('12','3','liferay.com','liferay.com.1',current timestamp,current timestamp,'11','HEADER');
insert into IGFolder (folderId, groupId, companyId, userId, createDate, modifiedDate, parentFolderId, name) values ('13','3','liferay.com','liferay.com.1',current timestamp,current timestamp,'-1','HOME');
insert into IGFolder (folderId, groupId, companyId, userId, createDate, modifiedDate, parentFolderId, name) values ('14','3','liferay.com','liferay.com.1',current timestamp,current timestamp,'-1','COMPANY');
insert into IGFolder (folderId, groupId, companyId, userId, createDate, modifiedDate, parentFolderId, name) values ('15','3','liferay.com','liferay.com.1',current timestamp,current timestamp,'-1','PRODUCTS');
insert into IGFolder (folderId, groupId, companyId, userId, createDate, modifiedDate, parentFolderId, name) values ('16','3','liferay.com','liferay.com.1',current timestamp,current timestamp,'-1','SERVICES');
insert into IGFolder (folderId, groupId, companyId, userId, createDate, modifiedDate, parentFolderId, name) values ('17','3','liferay.com','liferay.com.1',current timestamp,current timestamp,'-1','DOCUMENTATION');
insert into IGFolder (folderId, groupId, companyId, userId, createDate, modifiedDate, parentFolderId, name) values ('18','3','liferay.com','liferay.com.1',current timestamp,current timestamp,'-1','NEWS');
insert into IGFolder (folderId, groupId, companyId, userId, createDate, modifiedDate, parentFolderId, name) values ('19','3','liferay.com','liferay.com.1',current timestamp,current timestamp,'-1','DOWNLOADS');

insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('1','liferay.com','liferay.com.1',current timestamp,current timestamp,'12','',49,199,4102);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('2','liferay.com','liferay.com.1',current timestamp,current timestamp,'12','',9,56,283);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('3','liferay.com','liferay.com.1',current timestamp,current timestamp,'12','',9,56,283);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('4','liferay.com','liferay.com.1',current timestamp,current timestamp,'12','',9,57,283);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('5','liferay.com','liferay.com.1',current timestamp,current timestamp,'12','',9,57,283);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('6','liferay.com','liferay.com.1',current timestamp,current timestamp,'12','',9,49,264);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('7','liferay.com','liferay.com.1',current timestamp,current timestamp,'12','',9,49,264);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('8','liferay.com','liferay.com.1',current timestamp,current timestamp,'12','',9,91,404);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('9','liferay.com','liferay.com.1',current timestamp,current timestamp,'12','',9,91,404);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('10','liferay.com','liferay.com.1',current timestamp,current timestamp,'12','',9,31,199);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('11','liferay.com','liferay.com.1',current timestamp,current timestamp,'12','',9,31,199);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('12','liferay.com','liferay.com.1',current timestamp,current timestamp,'12','',9,70,344);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('13','liferay.com','liferay.com.1',current timestamp,current timestamp,'12','',9,70,344);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('14','liferay.com','liferay.com.1',current timestamp,current timestamp,'11','',1,1,49);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('15','liferay.com','liferay.com.1',current timestamp,current timestamp,'11','',14,8,516);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('16','liferay.com','liferay.com.1',current timestamp,current timestamp,'11','',10,10,75);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('17','liferay.com','liferay.com.1',current timestamp,current timestamp,'11','',8,8,64);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('18','liferay.com','liferay.com.1',current timestamp,current timestamp,'11','',9,13,206);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('19','liferay.com','liferay.com.1',current timestamp,current timestamp,'11','',262,1,61);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('20','liferay.com','liferay.com.1',current timestamp,current timestamp,'11','',1,262,834);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('21','liferay.com','liferay.com.1',current timestamp,current timestamp,'13','',15,80,533);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('22','liferay.com','liferay.com.1',current timestamp,current timestamp,'13','',15,62,442);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('23','liferay.com','liferay.com.1',current timestamp,current timestamp,'13','',94,127,6388);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('24','liferay.com','liferay.com.1',current timestamp,current timestamp,'13','',94,127,4866);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('25','liferay.com','liferay.com.1',current timestamp,current timestamp,'13','',10,70,426);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('26','liferay.com','liferay.com.1',current timestamp,current timestamp,'13','',25,90,1755);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('27','liferay.com','liferay.com.1',current timestamp,current timestamp,'13','',11,37,293);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('28','liferay.com','liferay.com.1',current timestamp,current timestamp,'13','',11,64,441);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('29','liferay.com','liferay.com.1',current timestamp,current timestamp,'13','',14,14,138);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('30','liferay.com','liferay.com.1',current timestamp,current timestamp,'13','',14,105,442);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('31','liferay.com','liferay.com.1',current timestamp,current timestamp,'14','company.jpg',100,550,51917);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('35','liferay.com','liferay.com.1',current timestamp,current timestamp,'16','services.jpg',100,550,33075);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('33','liferay.com','liferay.com.1',current timestamp,current timestamp,'15','products.jpg',100,550,36698);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('34','liferay.com','liferay.com.1',current timestamp,current timestamp,'15','products_title.gif',22,106,652);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('32','liferay.com','liferay.com.1',current timestamp,current timestamp,'14','company_title.gif',22,106,658);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('36','liferay.com','liferay.com.1',current timestamp,current timestamp,'16','services_title.gif',22,84,598);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('37','liferay.com','liferay.com.1',current timestamp,current timestamp,'17','documentation.jpg',100,550,33688);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('38','liferay.com','liferay.com.1',current timestamp,current timestamp,'17','documentation_title.gif',22,162,932);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('39','liferay.com','liferay.com.1',current timestamp,current timestamp,'18','news.jpg',100,550,27641);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('40','liferay.com','liferay.com.1',current timestamp,current timestamp,'18','news_title.gif',22,55,463);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('41','liferay.com','liferay.com.1',current timestamp,current timestamp,'19','downloads.jpg',100,550,33579);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('42','liferay.com','liferay.com.1',current timestamp,current timestamp,'19','downloads_title.gif',22,124,786);





insert into Group_ (groupId, companyId, parentGroupId, name, layoutIds) values ('1', 'liferay.com', '-1', 'General Guest', '1.1,1.2,');
insert into Group_ (groupId, companyId, parentGroupId, name, layoutIds) values ('2', 'liferay.com', '-1', 'General User', '');
insert into Group_ (groupId, companyId, parentGroupId, name, layoutIds) values ('3', 'liferay.com', '-1', 'CMS', '3.1,');

insert into Role_ (roleId, companyId, name) values ('1', 'liferay.com', 'Administrator');
insert into Role_ (roleId, companyId, name) values ('2', 'liferay.com', 'Bookmarks Admin');
insert into Role_ (roleId, companyId, name) values ('3', 'liferay.com', 'Calendar Admin');
insert into Role_ (roleId, companyId, name) values ('4', 'liferay.com', 'Document Library Admin');
insert into Role_ (roleId, companyId, name) values ('5', 'liferay.com', 'Guest');
insert into Role_ (roleId, companyId, name) values ('6', 'liferay.com', 'Journal Admin');
insert into Role_ (roleId, companyId, name) values ('7', 'liferay.com', 'Journal Designer');
insert into Role_ (roleId, companyId, name) values ('8', 'liferay.com', 'Journal Editor');
insert into Role_ (roleId, companyId, name) values ('9', 'liferay.com', 'Journal Writer');
insert into Role_ (roleId, companyId, name) values ('10', 'liferay.com', 'Message Boards Admin');
insert into Role_ (roleId, companyId, name) values ('11', 'liferay.com', 'Polls Admin');
insert into Role_ (roleId, companyId, name) values ('12', 'liferay.com', 'Power User');
insert into Role_ (roleId, companyId, name) values ('13', 'liferay.com', 'Project Admin');
insert into Role_ (roleId, companyId, name) values ('14', 'liferay.com', 'Shopping Admin');
insert into Role_ (roleId, companyId, name) values ('15', 'liferay.com', 'User');
insert into Role_ (roleId, companyId, name) values ('16', 'liferay.com', 'Wiki Admin');

insert into ShoppingCategory (categoryId, companyId, createDate, modifiedDate, parentCategoryId, name) values ('1', 'liferay.com', current timestamp, current timestamp, '-1', 'Books');
insert into ShoppingCategory (categoryId, companyId, createDate, modifiedDate, parentCategoryId, name) values ('2', 'liferay.com', current timestamp, current timestamp, '1', 'Art');
insert into ShoppingCategory (categoryId, companyId, createDate, modifiedDate, parentCategoryId, name) values ('3', 'liferay.com', current timestamp, current timestamp, '1', 'Christian');
insert into ShoppingCategory (categoryId, companyId, createDate, modifiedDate, parentCategoryId, name) values ('4', 'liferay.com', current timestamp, current timestamp, '1', 'Computer Science');
insert into ShoppingCategory (categoryId, companyId, createDate, modifiedDate, parentCategoryId, name) values ('5', 'liferay.com', current timestamp, current timestamp, '1', 'Economics');
insert into ShoppingCategory (categoryId, companyId, createDate, modifiedDate, parentCategoryId, name) values ('6', 'liferay.com', current timestamp, current timestamp, '1', 'Physics');


insert into PollsDisplay (layoutId, userId, portletId, questionId) values ('1.1', 'group.1', '59', '1');
insert into PollsChoice (choiceId, questionId, description) values ('a', '1', 'Chocolate');
insert into PollsChoice (choiceId, questionId, description) values ('b', '1', 'Strawberry');
insert into PollsChoice (choiceId, questionId, description) values ('c', '1', 'Vanilla');
insert into PollsQuestion (questionId, portletId, groupId, companyId, userId, userName, createDate, modifiedDate, title, description) values ('1', '25', '-1', 'liferay.com', 'liferay.com.1', 'John Wayne', current timestamp, current timestamp, 'What is your favorite ice cream flavor?', 'What is your favorite ice cream flavor?');

insert into WikiDisplay (layoutId, userId, portletId, nodeId, showBorders) values ('1.1', 'group.1', '54', '1', '1');
insert into WikiNode (nodeId, companyId, userId, userName, createDate, modifiedDate, readRoles, writeRoles, name, description, sharing, lastPostDate) values ('1', 'liferay.com', 'liferay.com.1', 'John Wayne', current timestamp, current timestamp, 'User,' ,'User,', 'Welcome', '', '1', current timestamp);
insert into WikiPage (nodeId, title, version, companyId, userId, userName, createDate, content, format, head) values ('1', 'FrontPage', 1.0, 'liferay.com', 'liferay.com.1', 'John Wayne', current timestamp, '<font class="bg" size="2">Welcome! Thank you for your interest in Liferay Enterprise Portal.<br><br>Your login is <b>test@liferay.com</b> and your password is <b>test</b>. The test user has the Administrator role.<br><br>To use the <b>Mail</b> portlet, make sure there is a mail server running on <i>localhost</i> accessible through the IMAP protocol.<br><br>The mail server must have an account with <b>liferay.com.1</b> as the user and <b>test</b> as the password.<br><br><hr><br>Is Liferay useful for your company? Tell us about it by email at <b>staff@liferay.com</b>.<br><br>We hope you enjoy our product!</font>', 'html', '1');

--
-- Default User
--

insert into User_ (userId, companyId, createDate, password_, passwordEncrypted, passwordReset, firstName, middleName, lastName, male, birthday, emailAddress, skinId, dottedSkins, roundedSkins, greeting, layoutIds, loginDate, failedLoginAttempts, agreedToTermsOfUse, active_) values ('liferay.com.default', 'default', current timestamp, 'password', '0', '0', '', '', '', '1', '1970-01-01-00.00.00.000000', 'default@liferay.com', '01', '0', '0', 'Welcome!', '', current timestamp, 0, '0', '1');
insert into Layout (layoutId, userId, name, columnOrder, narrow1, narrow2, wide) values ('1.1', 'group.1', 'Welcome', 'n1,w,', '59,', '', '2,54,');
insert into Layout (layoutId, userId, name, columnOrder, narrow1, narrow2, wide, stateMax) values ('1.2', 'group.1', 'Shopping', 'w,', '', '', '34,', '34,');
insert into Layout (layoutId, userId, name, columnOrder, narrow1, narrow2, wide, stateMax) values ('3.1', 'group.3', 'CMS', 'n1,w,', '', '', '15,31,', '');

--
-- Test User
--

insert into User_ (userId, companyId, createDate, password_, passwordEncrypted, passwordReset, firstName, middleName, lastName, nickName, male, birthday, emailAddress, skinId, dottedSkins, roundedSkins, greeting, layoutIds, loginDate, failedLoginAttempts, agreedToTermsOfUse, active_) values ('liferay.com.1', 'liferay.com', current timestamp, 'test', '0', '0', 'John', '', 'Wayne', 'Duke', '1', '1970-01-01-00.00.00.000000', 'test@liferay.com', '01', '0', '1', 'Welcome John Wayne!', '1,', current timestamp, 0, '1', '1');
insert into Layout (layoutId, userId, name, columnOrder, narrow1, narrow2, wide) values ('1', 'liferay.com.1', 'Home', 'n1,w,', '9,36,4,3,', '', '5,8,');
insert into Users_Groups values ('liferay.com.1', '1');
insert into Users_Groups values ('liferay.com.1', '3');
insert into Users_Roles values ('liferay.com.1', '1');
insert into Users_Roles values ('liferay.com.1', '12');
insert into Users_Roles values ('liferay.com.1', '15');
