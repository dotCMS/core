create table ABContact (
	contactId varchar(60) not null primary key,
	userId varchar(60) not null,
	firstName varchar(60),
	middleName varchar(60),
	lastName varchar(60),
	nickName varchar(60),
	emailAddress varchar(60),
	homeStreet varchar(60),
	homeCity varchar(60),
	homeState varchar(60),
	homeZip varchar(60),
	homeCountry varchar(60),
	homePhone varchar(60),
	homeFax varchar(60),
	homeCell varchar(60),
	homePager varchar(60),
	homeTollFree varchar(60),
	homeEmailAddress varchar(60),
	businessCompany varchar(60),
	businessStreet varchar(60),
	businessCity varchar(60),
	businessState varchar(60),
	businessZip varchar(60),
	businessCountry varchar(60),
	businessPhone varchar(60),
	businessFax varchar(60),
	businessCell varchar(60),
	businessPager varchar(60),
	businessTollFree varchar(60),
	businessEmailAddress varchar(60),
	employeeNumber varchar(60),
	jobTitle varchar(60),
	jobClass varchar(60),
	hoursOfOperation varchar(4000),
	birthday timestamp,
	timeZoneId varchar(60),
	instantMessenger varchar(60),
	website varchar(60),
	comments varchar(4000)
);

create table ABContacts_ABLists (
	contactId varchar(60) not null,
	listId varchar(60) not null
);

create table ABList (
	listId varchar(60) not null primary key,
	userId varchar(60) not null,
	name varchar(60)
);

create table Address (
	addressId varchar(60) not null primary key,
	companyId varchar(60) not null,
	userId varchar(60) not null,
	userName varchar(60),
	createDate timestamp,
	modifiedDate timestamp,
	className varchar(60),
	classPK varchar(60),
	description varchar(60),
	street1 varchar(60),
	street2 varchar(60),
	city varchar(60),
	state varchar(60),
	zip varchar(60),
	country varchar(60),
	phone varchar(60),
	fax varchar(60),
	cell varchar(60),
	priority integer
);

create table AdminConfig (
	configId varchar(60) not null primary key,
	companyId varchar(60) not null,
	type_ varchar(60),
	name varchar(60),
	config blob
);

create table BJEntries_BJTopics (
	entryId varchar(60) not null,
	topicId varchar(60) not null
);

create table BJEntries_BJVerses (
	entryId varchar(60) not null,
	verseId varchar(60) not null
);

create table BJEntry (
	entryId varchar(60) not null primary key,
	companyId varchar(60) not null,
	userId varchar(60) not null,
	createDate timestamp,
	modifiedDate timestamp,
	name varchar(60),
	content blob,
	versesInput varchar(4000)
);

create table BJTopic (
	topicId varchar(60) not null primary key,
	companyId varchar(60) not null,
	userId varchar(60) not null,
	createDate timestamp,
	modifiedDate timestamp,
	name varchar(60),
	description blob
);

create table BJVerse (
	verseId varchar(60) not null primary key,
	companyId varchar(60) not null,
	userId varchar(60) not null,
	name varchar(60)
);

create table BlogsCategory (
	categoryId varchar(60) not null primary key,
	companyId varchar(60) not null,
	userId varchar(60) not null,
	createDate timestamp,
	modifiedDate timestamp,
	name varchar(60)
);

create table BlogsComments (
	commentsId varchar(60) not null primary key,
	companyId varchar(60) not null,
	userId varchar(60) not null,
	userName varchar(60),
	createDate timestamp,
	modifiedDate timestamp,
	entryId varchar(60),
	content blob
);

create table BlogsEntry (
	entryId varchar(60) not null primary key,
	companyId varchar(60) not null,
	userId varchar(60) not null,
	createDate timestamp,
	modifiedDate timestamp,
	categoryId varchar(60),
	title varchar(60),
	content blob,
	displayDate timestamp,
	sharing smallint,
	commentable smallint,
	propsCount integer,
	commentsCount integer
);

create table BlogsLink (
	linkId varchar(60) not null primary key,
	companyId varchar(60) not null,
	userId varchar(60) not null,
	createDate timestamp,
	modifiedDate timestamp,
	name varchar(60),
	url varchar(60)
);

create table BlogsProps (
	propsId varchar(60) not null primary key,
	companyId varchar(60) not null,
	userId varchar(60) not null,
	userName varchar(60),
	createDate timestamp,
	modifiedDate timestamp,
	entryId varchar(60),
	quantity integer
);

create table BlogsReferer (
	entryId varchar(60) not null,
	url varchar(60) not null,
	type_ varchar(60) not null,
	quantity integer,
	primary key (entryId, url, type_)
);

create table BlogsUser (
	userId varchar(60) not null primary key,
	companyId varchar(60) not null,
	entryId varchar(60) not null,
	lastPostDate timestamp
);

create table BookmarksEntry (
	entryId varchar(60) not null primary key,
	userId varchar(60) not null,
	createDate timestamp,
	modifiedDate timestamp,
	folderId varchar(60),
	name varchar(60),
	url varchar(60),
	comments varchar(4000),
	visits integer
);

create table BookmarksFolder (
	folderId varchar(60) not null primary key,
	userId varchar(60) not null,
	createDate timestamp,
	modifiedDate timestamp,
	parentFolderId varchar(60),
	name varchar(60)
);

create table CalEvent (
	eventId varchar(60) not null primary key,
	groupId varchar(60) not null,
	companyId varchar(60) not null,
	userId varchar(60) not null,
	userName varchar(60),
	createDate timestamp,
	modifiedDate timestamp,
	title varchar(60),
	description varchar(4000),
	startDate timestamp,
	endDate timestamp,
	durationHour integer,
	durationMinute integer,
	allDay smallint,
	timeZoneSensitive smallint,
	type_ varchar(60),
	location varchar(60),
	street varchar(60),
	city varchar(60),
	state varchar(60),
	zip varchar(60),
	phone varchar(60),
	repeating smallint,
	recurrence blob,
	remindBy varchar(60),
	firstReminder integer,
	secondReminder integer
);

create table CalTask (
	taskId varchar(60) not null primary key,
	companyId varchar(60) not null,
	userId varchar(60) not null,
	createDate timestamp,
	modifiedDate timestamp,
	title varchar(60),
	description varchar(4000),
	noDueDate smallint,
	dueDate timestamp,
	priority integer,
	status integer
);

create table Company (
	companyId varchar(60) not null primary key,
	key_ blob,
	portalURL varchar(60) not null,
	homeURL varchar(60) not null,
	mx varchar(60) not null,
	name varchar(60) not null,
	shortName varchar(60) not null,
	type_ varchar(60),
	size_ varchar(60),
	street varchar(60),
	city varchar(60),
	state varchar(60),
	zip varchar(60),
	phone varchar(60),
	fax varchar(60),
	emailAddress varchar(60),
	authType varchar(60),
	autoLogin smallint,
	strangers smallint
);

create table Counter (
	name varchar(60) not null primary key,
	currentId integer
);

create table CyrusUser (
	userId varchar(60) not null primary key,
	password_ varchar(60) not null
);

create table CyrusVirtual (
	emailAddress varchar(60) not null primary key,
	userId varchar(60) not null
);

create table DLFileProfile (
	companyId varchar(60) not null,
	repositoryId varchar(60) not null,
	fileName varchar(60) not null,
	userId varchar(60) not null,
	userName varchar(60),
	versionUserId varchar(60) not null,
	versionUserName varchar(60),
	createDate timestamp,
	modifiedDate timestamp,
	readRoles varchar(60),
	writeRoles varchar(60),
	description blob,
	version double precision,
	size_ integer,
	primary key (companyId, repositoryId, fileName)
);

create table DLFileRank (
	companyId varchar(60) not null,
	userId varchar(60) not null,
	repositoryId varchar(60) not null,
	fileName varchar(60) not null,
	createDate timestamp,
	primary key (companyId, userId, repositoryId, fileName)
);

create table DLFileVersion (
	companyId varchar(60) not null,
	repositoryId varchar(60) not null,
	fileName varchar(60) not null,
	version double precision not null,
	userId varchar(60) not null,
	userName varchar(60),
	createDate timestamp,
	size_ integer,
	primary key (companyId, repositoryId, fileName, version)
);

create table DLRepository (
	repositoryId varchar(60) not null primary key,
	groupId varchar(60) not null,
	companyId varchar(60) not null,
	userId varchar(60) not null,
	userName varchar(60),
	createDate timestamp,
	modifiedDate timestamp,
	readRoles varchar(60),
	writeRoles varchar(60),
	name varchar(60),
	description varchar(4000),
	lastPostDate timestamp
);

create table Group_ (
	groupId varchar(60) not null primary key,
	companyId varchar(60) not null,
	parentGroupId varchar(60),
	name varchar(60),
	layoutIds varchar(60)
);

create table Groups_Roles (
	groupId varchar(60) not null,
	roleId varchar(60) not null
);

create table IGFolder (
	folderId varchar(60) not null primary key,
	groupId varchar(60) not null,
	companyId varchar(60) not null,
	userId varchar(60) not null,
	createDate timestamp,
	modifiedDate timestamp,
	parentFolderId varchar(60),
	name varchar(60)
);

create table IGImage (
	imageId varchar(60) not null,
	companyId varchar(60) not null,
	userId varchar(60) not null,
	createDate timestamp,
	modifiedDate timestamp,
	folderId varchar(60),
	description blob,
	height integer,
	width integer,
	size_ integer,
	primary key (imageId, companyId)
);

create table Image (
	imageId varchar(200) not null primary key,
	text_ blob not null
);

create table JournalArticle (
	articleId varchar(60) not null,
	version double precision not null,
	portletId varchar(60) not null,
	groupId varchar(60) not null,
	companyId varchar(60) not null,
	userId varchar(60) not null,
	userName varchar(60),
	createDate timestamp,
	modifiedDate timestamp,
	title varchar(60),
	content blob,
	type_ varchar(60),
	structureId varchar(60),
	templateId varchar(60),
	displayDate timestamp,
	expirationDate timestamp,
	approved smallint,
	approvedByUserId varchar(60),
	approvedByUserName varchar(60),
	primary key (articleId, version)
);

create table JournalStructure (
	structureId varchar(60) not null primary key,
	portletId varchar(60) not null,
	groupId varchar(60) not null,
	companyId varchar(60) not null,
	userId varchar(60) not null,
	userName varchar(60),
	createDate timestamp,
	modifiedDate timestamp,
	name varchar(60),
	description varchar(4000),
	xsd blob
);

create table JournalTemplate (
	templateId varchar(60) not null primary key,
	portletId varchar(60) not null,
	groupId varchar(60) not null,
	companyId varchar(60) not null,
	userId varchar(60) not null,
	userName varchar(60),
	createDate timestamp,
	modifiedDate timestamp,
	structureId varchar(60),
	name varchar(60),
	description varchar(4000),
	xsl blob,
	smallImage smallint,
	smallImageURL varchar(60)
);

create table Layer (
	layerId varchar(60) not null,
	skinId varchar(60) not null,
	href varchar(60),
	hrefHover varchar(60),
	background varchar(60),
	foreground varchar(60),
	negAlert varchar(60),
	posAlert varchar(60),
	primary key (layerId, skinId)
);

create table Layout (
	layoutId varchar(60) not null,
	userId varchar(60) not null,
	name varchar(60),
	columnOrder varchar(60),
	narrow1 varchar(4000),
	narrow2 varchar(4000),
	wide varchar(4000),
	stateMax varchar(4000),
	stateMin varchar(4000),
	modeEdit varchar(4000),
	modeHelp varchar(4000),
	primary key (layoutId, userId)
);

create table MailReceipt (
	receiptId varchar(60) not null primary key,
	companyId varchar(60) not null,
	userId varchar(60) not null,
	createDate timestamp,
	modifiedDate timestamp,
	recipientName varchar(60),
	recipientAddress varchar(60),
	subject varchar(60),
	sentDate timestamp,
	readCount integer,
	firstReadDate timestamp,
	lastReadDate timestamp
);

create table MBMessage (
	messageId varchar(60) not null,
	topicId varchar(60) not null,
	companyId varchar(60) not null,
	userId varchar(60) not null,
	userName varchar(60),
	createDate timestamp,
	modifiedDate timestamp,
	threadId varchar(60),
	parentMessageId varchar(60),
	subject varchar(60),
	body blob,
	attachments smallint,
	anonymous smallint,
	primary key (messageId, topicId)
);

create table MBMessageFlag (
	topicId varchar(60) not null,
	messageId varchar(60) not null,
	userId varchar(60) not null,
	flag varchar(60),
	primary key (topicId, messageId, userId)
);

create table MBThread (
	threadId varchar(60) not null primary key,
	rootMessageId varchar(60),
	topicId varchar(60),
	messageCount integer,
	lastPostDate timestamp
);

create table MBTopic (
	topicId varchar(60) not null primary key,
	portletId varchar(60) not null,
	groupId varchar(60) not null,
	companyId varchar(60) not null,
	userId varchar(60) not null,
	userName varchar(60),
	createDate timestamp,
	modifiedDate timestamp,
	readRoles varchar(60),
	writeRoles varchar(60),
	name varchar(60),
	description blob,
	lastPostDate timestamp
);

create table NetworkAddress (
	addressId varchar(60) not null primary key,
	userId varchar(60) not null,
	createDate timestamp,
	modifiedDate timestamp,
	name varchar(60),
	url varchar(60),
	comments varchar(4000),
	content blob,
	status integer,
	lastUpdated timestamp,
	notifyBy varchar(60),
	interval_ integer,
	active_ smallint
);

create table Note (
	noteId varchar(60) not null primary key,
	companyId varchar(60) not null,
	userId varchar(60) not null,
	userName varchar(60),
	createDate timestamp,
	modifiedDate timestamp,
	className varchar(60),
	classPK varchar(60),
	content blob
);

create table PasswordTracker (
	passwordTrackerId varchar(60) not null primary key,
	userId varchar(60) not null,
	createDate timestamp not null,
	password_ varchar(60) not null
);

create table PollsChoice (
	choiceId varchar(60) not null,
	questionId varchar(60) not null,
	description varchar(4000),
	primary key (choiceId, questionId)
);

create table PollsDisplay (
	layoutId varchar(60) not null,
	userId varchar(60) not null,
	portletId varchar(60) not null,
	questionId varchar(60) not null,
	primary key (layoutId, userId, portletId)
);

create table PollsQuestion (
	questionId varchar(60) not null primary key,
	portletId varchar(60) not null,
	groupId varchar(60) not null,
	companyId varchar(60) not null,
	userId varchar(60) not null,
	userName varchar(60),
	createDate timestamp,
	modifiedDate timestamp,
	title varchar(60),
	description varchar(4000),
	expirationDate timestamp,
	lastVoteDate timestamp
);

create table PollsVote (
	questionId varchar(60) not null,
	userId varchar(60) not null,
	choiceId varchar(60) not null,
	voteDate timestamp,
	primary key (questionId, userId)
);

create table Portlet (
	portletId varchar(60) not null,
	groupId varchar(60) not null,
	companyId varchar(60) not null,
	defaultPreferences blob,
	narrow smallint,
	roles varchar(4000),
	active_ smallint,
	primary key (portletId, groupId, companyId)
);

create table PortletPreferences (
	portletId varchar(60) not null,
	userId varchar(60) not null,
	layoutId varchar(60) not null,
	preferences blob,
	primary key (portletId, userId, layoutId)
);

create table ProjFirm (
	firmId varchar(60) not null primary key,
	companyId varchar(60) not null,
	userId varchar(60) not null,
	userName varchar(60),
	createDate timestamp,
	modifiedDate timestamp,
	name varchar(60),
	description varchar(4000),
	url varchar(60)
);

create table ProjProject (
	projectId varchar(60) not null primary key,
	companyId varchar(60) not null,
	userId varchar(60) not null,
	userName varchar(60),
	createDate timestamp,
	modifiedDate timestamp,
	firmId varchar(60),
	code varchar(60),
	name varchar(60),
	description varchar(4000)
);

create table ProjTask (
	taskId varchar(60) not null primary key,
	companyId varchar(60) not null,
	userId varchar(60) not null,
	userName varchar(60),
	createDate timestamp,
	modifiedDate timestamp,
	projectId varchar(60),
	name varchar(60),
	description varchar(4000),
	comments blob,
	estimatedDuration integer,
	estimatedEndDate timestamp,
	actualDuration integer,
	actualEndDate timestamp,
	status integer
);

create table ProjTime (
	timeId varchar(60) not null primary key,
	companyId varchar(60) not null,
	userId varchar(60) not null,
	userName varchar(60),
	createDate timestamp,
	modifiedDate timestamp,
	projectId varchar(60),
	taskId varchar(60),
	description varchar(4000),
	startDate timestamp,
	endDate timestamp
);

create table Release_ (
	releaseId varchar(60) not null primary key,
	createDate timestamp,
	modifiedDate timestamp,
	buildNumber integer,
	buildDate timestamp
);

create table Role_ (
	roleId varchar(60) not null primary key,
	companyId varchar(60) not null,
	name varchar(60) not null
);

create table Skin (
	skinId varchar(60) not null primary key,
	name varchar(60),
	imageId varchar(60),
	alphaLayerId varchar(60),
	alphaSkinId varchar(60),
	betaLayerId varchar(60),
	betaSkinId varchar(60),
	gammaLayerId varchar(60),
	gammaSkinId varchar(60),
	bgLayerId varchar(60),
	bgSkinId varchar(60)
);

create table ShoppingCart (
	cartId varchar(60) not null primary key,
	companyId varchar(60) not null,
	userId varchar(60) not null,
	createDate timestamp,
	modifiedDate timestamp,
	itemIds varchar(4000),
	couponIds varchar(4000),
	altShipping integer
);

create table ShoppingCategory (
	categoryId varchar(60) not null primary key,
	companyId varchar(60) not null,
	createDate timestamp,
	modifiedDate timestamp,
	parentCategoryId varchar(60),
	name varchar(60)
);

create table ShoppingCoupon (
	couponId varchar(60) not null primary key,
	companyId varchar(60) not null,
	createDate timestamp,
	modifiedDate timestamp,
	name varchar(60),
	description varchar(4000),
	startDate timestamp,
	endDate timestamp,
	active_ smallint,
	limitCategories varchar(4000),
	limitSkus varchar(4000),
	minOrder double precision,
	discount double precision,
	discountType varchar(60)
);

create table ShoppingItem (
	itemId varchar(60) not null primary key,
	companyId varchar(60) not null,
	createDate timestamp,
	modifiedDate timestamp,
	categoryId varchar(60),
	sku varchar(60),
	name varchar(60),
	description varchar(4000),
	properties varchar(4000),
	supplierUserId varchar(60),
	fields_ smallint,
	fieldsQuantities varchar(4000),
	minQuantity integer,
	maxQuantity integer,
	price double precision,
	discount double precision,
	taxable smallint,
	shipping double precision,
	useShippingFormula smallint,
	requiresShipping smallint,
	stockQuantity integer,
	featured_ smallint,
	sale_ smallint,
	smallImage smallint,
	smallImageURL varchar(60),
	mediumImage smallint,
	mediumImageURL varchar(60),
	largeImage smallint,
	largeImageURL varchar(60)
);

create table ShoppingItemField (
	itemFieldId varchar(60) not null primary key,
	itemId varchar(60),
	name varchar(60),
	values_ varchar(4000),
	description varchar(4000)
);

create table ShoppingItemPrice (
	itemPriceId varchar(60) not null primary key,
	itemId varchar(60),
	minQuantity integer,
	maxQuantity integer,
	price double precision,
	discount double precision,
	taxable smallint,
	shipping double precision,
	useShippingFormula smallint,
	status integer
);

create table ShoppingOrder (
	orderId varchar(60) not null primary key,
	companyId varchar(60) not null,
	userId varchar(60) not null,
	createDate timestamp,
	modifiedDate timestamp,
	tax double precision,
	shipping double precision,
	altShipping varchar(60),
	requiresShipping smallint,
	couponIds varchar(4000),
	couponDiscount double precision,
	billingFirstName varchar(60),
	billingLastName varchar(60),
	billingEmailAddress varchar(60),
	billingCompany varchar(60),
	billingStreet varchar(60),
	billingCity varchar(60),
	billingState varchar(60),
	billingZip varchar(60),
	billingCountry varchar(60),
	billingPhone varchar(60),
	shipToBilling smallint,
	shippingFirstName varchar(60),
	shippingLastName varchar(60),
	shippingEmailAddress varchar(60),
	shippingCompany varchar(60),
	shippingStreet varchar(60),
	shippingCity varchar(60),
	shippingState varchar(60),
	shippingZip varchar(60),
	shippingCountry varchar(60),
	shippingPhone varchar(60),
	ccName varchar(60),
	ccType varchar(60),
	ccNumber varchar(60),
	ccExpMonth integer,
	ccExpYear integer,
	ccVerNumber varchar(60),
	comments blob,
	ppTxnId varchar(60),
	ppPaymentStatus varchar(60),
	ppPaymentGross double precision,
	ppReceiverEmail varchar(60),
	ppPayerEmail varchar(60),
	sendOrderEmail smallint,
	sendShippingEmail smallint
);

create table ShoppingOrderItem (
	orderId varchar(60) not null,
	itemId varchar(60) not null,
	sku varchar(60),
	name varchar(60),
	description varchar(4000),
	properties varchar(4000),
	supplierUserId varchar(60),
	price double precision,
	quantity integer,
	shippedDate timestamp,
	primary key (orderId, itemId)
);

create table User_ (
	userId varchar(60) not null primary key,
	companyId varchar(60) not null,
	createDate timestamp,
	password_ varchar(60),
	passwordEncrypted smallint,
	passwordExpirationDate timestamp,
	passwordReset smallint,
	firstName varchar(60),
	middleName varchar(60),
	lastName varchar(60),
	nickName varchar(60),
	male smallint,
	birthday timestamp,
	emailAddress varchar(60),
	smsId varchar(60),
	aimId varchar(60),
	icqId varchar(60),
	msnId varchar(60),
	ymId varchar(60),
	favoriteActivity varchar(60),
	favoriteBibleVerse varchar(60),
	favoriteFood varchar(60),
	favoriteMovie varchar(60),
	favoriteMusic varchar(60),
	languageId varchar(60),
	timeZoneId varchar(60),
	skinId varchar(60),
	dottedSkins smallint,
	roundedSkins smallint,
	greeting varchar(60),
	resolution varchar(60),
	refreshRate varchar(60),
	layoutIds varchar(60),
	comments varchar(4000),
	loginDate timestamp,
	loginIP varchar(60),
	lastLoginDate timestamp,
	lastLoginIP varchar(60),
	failedLoginAttempts integer,
	agreedToTermsOfUse smallint,
	active_ smallint
);

create table Users_Groups (
	userId varchar(60) not null,
	groupId varchar(60) not null
);

create table Users_ProjProjects (
	userId varchar(60) not null,
	projectId varchar(60) not null
);

create table Users_ProjTasks (
	userId varchar(60) not null,
	taskId varchar(60) not null
);

create table Users_Roles (
	userId varchar(60) not null,
	roleId varchar(60) not null
);

create table UserTracker (
	userTrackerId varchar(60) not null primary key,
	companyId varchar(60) not null,
	userId varchar(60) not null,
	modifiedDate timestamp,
	remoteAddr varchar(60),
	remoteHost varchar(60),
	userAgent varchar(60)
);

create table UserTrackerPath (
	userTrackerPathId varchar(60) not null primary key,
	userTrackerId varchar(60) not null,
	path varchar(4000) not null,
	pathDate timestamp not null
);

create table WikiDisplay (
	layoutId varchar(60) not null,
	userId varchar(60) not null,
	portletId varchar(60) not null,
	nodeId varchar(60) not null,
	showBorders smallint,
	primary key (layoutId, userId, portletId)
);

create table WikiNode (
	nodeId varchar(60) not null primary key,
	companyId varchar(60) not null,
	userId varchar(60) not null,
	userName varchar(60),
	createDate timestamp,
	modifiedDate timestamp,
	readRoles varchar(60),
	writeRoles varchar(60),
	name varchar(60),
	description varchar(4000),
	sharing smallint,
	lastPostDate timestamp
);

create table WikiPage (
	nodeId varchar(60) not null,
	title varchar(60) not null,
	version double precision not null,
	companyId varchar(60) not null,
	userId varchar(60) not null,
	userName varchar(60),
	createDate timestamp,
	content blob,
	format varchar(60),
	head smallint,
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

insert into IGFolder (folderId, groupId, companyId, userId, createDate, modifiedDate, parentFolderId, name) values ('11','3','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'-1','COMMON');
insert into IGFolder (folderId, groupId, companyId, userId, createDate, modifiedDate, parentFolderId, name) values ('12','3','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'11','HEADER');
insert into IGFolder (folderId, groupId, companyId, userId, createDate, modifiedDate, parentFolderId, name) values ('13','3','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'-1','HOME');
insert into IGFolder (folderId, groupId, companyId, userId, createDate, modifiedDate, parentFolderId, name) values ('14','3','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'-1','COMPANY');
insert into IGFolder (folderId, groupId, companyId, userId, createDate, modifiedDate, parentFolderId, name) values ('15','3','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'-1','PRODUCTS');
insert into IGFolder (folderId, groupId, companyId, userId, createDate, modifiedDate, parentFolderId, name) values ('16','3','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'-1','SERVICES');
insert into IGFolder (folderId, groupId, companyId, userId, createDate, modifiedDate, parentFolderId, name) values ('17','3','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'-1','DOCUMENTATION');
insert into IGFolder (folderId, groupId, companyId, userId, createDate, modifiedDate, parentFolderId, name) values ('18','3','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'-1','NEWS');
insert into IGFolder (folderId, groupId, companyId, userId, createDate, modifiedDate, parentFolderId, name) values ('19','3','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'-1','DOWNLOADS');

insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('1','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'12','',49,199,4102);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('2','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'12','',9,56,283);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('3','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'12','',9,56,283);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('4','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'12','',9,57,283);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('5','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'12','',9,57,283);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('6','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'12','',9,49,264);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('7','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'12','',9,49,264);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('8','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'12','',9,91,404);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('9','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'12','',9,91,404);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('10','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'12','',9,31,199);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('11','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'12','',9,31,199);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('12','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'12','',9,70,344);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('13','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'12','',9,70,344);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('14','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'11','',1,1,49);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('15','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'11','',14,8,516);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('16','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'11','',10,10,75);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('17','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'11','',8,8,64);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('18','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'11','',9,13,206);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('19','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'11','',262,1,61);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('20','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'11','',1,262,834);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('21','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'13','',15,80,533);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('22','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'13','',15,62,442);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('23','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'13','',94,127,6388);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('24','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'13','',94,127,4866);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('25','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'13','',10,70,426);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('26','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'13','',25,90,1755);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('27','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'13','',11,37,293);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('28','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'13','',11,64,441);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('29','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'13','',14,14,138);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('30','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'13','',14,105,442);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('31','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'14','company.jpg',100,550,51917);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('35','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'16','services.jpg',100,550,33075);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('33','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'15','products.jpg',100,550,36698);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('34','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'15','products_title.gif',22,106,652);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('32','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'14','company_title.gif',22,106,658);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('36','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'16','services_title.gif',22,84,598);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('37','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'17','documentation.jpg',100,550,33688);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('38','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'17','documentation_title.gif',22,162,932);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('39','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'18','news.jpg',100,550,27641);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('40','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'18','news_title.gif',22,55,463);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('41','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'19','downloads.jpg',100,550,33579);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('42','liferay.com','liferay.com.1',current_timestamp,current_timestamp,'19','downloads_title.gif',22,124,786);





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

insert into ShoppingCategory (categoryId, companyId, createDate, modifiedDate, parentCategoryId, name) values ('1', 'liferay.com', current_timestamp, current_timestamp, '-1', 'Books');
insert into ShoppingCategory (categoryId, companyId, createDate, modifiedDate, parentCategoryId, name) values ('2', 'liferay.com', current_timestamp, current_timestamp, '1', 'Art');
insert into ShoppingCategory (categoryId, companyId, createDate, modifiedDate, parentCategoryId, name) values ('3', 'liferay.com', current_timestamp, current_timestamp, '1', 'Christian');
insert into ShoppingCategory (categoryId, companyId, createDate, modifiedDate, parentCategoryId, name) values ('4', 'liferay.com', current_timestamp, current_timestamp, '1', 'Computer Science');
insert into ShoppingCategory (categoryId, companyId, createDate, modifiedDate, parentCategoryId, name) values ('5', 'liferay.com', current_timestamp, current_timestamp, '1', 'Economics');
insert into ShoppingCategory (categoryId, companyId, createDate, modifiedDate, parentCategoryId, name) values ('6', 'liferay.com', current_timestamp, current_timestamp, '1', 'Physics');


insert into PollsDisplay (layoutId, userId, portletId, questionId) values ('1.1', 'group.1', '59', '1');
insert into PollsChoice (choiceId, questionId, description) values ('a', '1', 'Chocolate');
insert into PollsChoice (choiceId, questionId, description) values ('b', '1', 'Strawberry');
insert into PollsChoice (choiceId, questionId, description) values ('c', '1', 'Vanilla');
insert into PollsQuestion (questionId, portletId, groupId, companyId, userId, userName, createDate, modifiedDate, title, description) values ('1', '25', '-1', 'liferay.com', 'liferay.com.1', 'John Wayne', current_timestamp, current_timestamp, 'What is your favorite ice cream flavor?', 'What is your favorite ice cream flavor?');

insert into WikiDisplay (layoutId, userId, portletId, nodeId, showBorders) values ('1.1', 'group.1', '54', '1', '1');
insert into WikiNode (nodeId, companyId, userId, userName, createDate, modifiedDate, readRoles, writeRoles, name, description, sharing, lastPostDate) values ('1', 'liferay.com', 'liferay.com.1', 'John Wayne', current_timestamp, current_timestamp, 'User,' ,'User,', 'Welcome', '', '1', current_timestamp);
insert into WikiPage (nodeId, title, version, companyId, userId, userName, createDate, content, format, head) values ('1', 'FrontPage', 1.0, 'liferay.com', 'liferay.com.1', 'John Wayne', current_timestamp, '<font class="bg" size="2">Welcome! Thank you for your interest in Liferay Enterprise Portal.<br><br>Your login is <b>test@liferay.com</b> and your password is <b>test</b>. The test user has the Administrator role.<br><br>To use the <b>Mail</b> portlet, make sure there is a mail server running on <i>localhost</i> accessible through the IMAP protocol.<br><br>The mail server must have an account with <b>liferay.com.1</b> as the user and <b>test</b> as the password.<br><br><hr><br>Is Liferay useful for your company? Tell us about it by email at <b>staff@liferay.com</b>.<br><br>We hope you enjoy our product!</font>', 'html', '1');

--
-- Default User
--

insert into User_ (userId, companyId, createDate, password_, passwordEncrypted, passwordReset, firstName, middleName, lastName, male, birthday, emailAddress, skinId, dottedSkins, roundedSkins, greeting, layoutIds, loginDate, failedLoginAttempts, agreedToTermsOfUse, active_) values ('liferay.com.default', 'default', current_timestamp, 'password', '0', '0', '', '', '', '1', '01/01/1970', 'default@liferay.com', '01', '0', '0', 'Welcome!', '', current_timestamp, 0, '0', '1');
insert into Layout (layoutId, userId, name, columnOrder, narrow1, narrow2, wide) values ('1.1', 'group.1', 'Welcome', 'n1,w,', '59,', '', '2,54,');
insert into Layout (layoutId, userId, name, columnOrder, narrow1, narrow2, wide, stateMax) values ('1.2', 'group.1', 'Shopping', 'w,', '', '', '34,', '34,');
insert into Layout (layoutId, userId, name, columnOrder, narrow1, narrow2, wide, stateMax) values ('3.1', 'group.3', 'CMS', 'n1,w,', '', '', '15,31,', '');

--
-- Test User
--

insert into User_ (userId, companyId, createDate, password_, passwordEncrypted, passwordReset, firstName, middleName, lastName, nickName, male, birthday, emailAddress, skinId, dottedSkins, roundedSkins, greeting, layoutIds, loginDate, failedLoginAttempts, agreedToTermsOfUse, active_) values ('liferay.com.1', 'liferay.com', current_timestamp, 'test', '0', '0', 'John', '', 'Wayne', 'Duke', '1', '01/01/1970', 'test@liferay.com', '01', '0', '1', 'Welcome John Wayne!', '1,', current_timestamp, 0, '1', '1');
insert into Layout (layoutId, userId, name, columnOrder, narrow1, narrow2, wide) values ('1', 'liferay.com.1', 'Home', 'n1,w,', '9,36,4,3,', '', '5,8,');
insert into Users_Groups values ('liferay.com.1', '1');
insert into Users_Groups values ('liferay.com.1', '3');
insert into Users_Roles values ('liferay.com.1', '1');
insert into Users_Roles values ('liferay.com.1', '12');
insert into Users_Roles values ('liferay.com.1', '15');
