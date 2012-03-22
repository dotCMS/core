
create table ABContact (
	contactId varchar(100) not null primary key,
	userId varchar(100) not null,
	firstName varchar(100) null,
	middleName varchar(100) null,
	lastName varchar(100) null,
	nickName varchar(100) null,
	emailAddress varchar(100) null,
	homeStreet varchar(100) null,
	homeCity varchar(100) null,
	homeState varchar(100) null,
	homeZip varchar(100) null,
	homeCountry varchar(100) null,
	homePhone varchar(100) null,
	homeFax varchar(100) null,
	homeCell varchar(100) null,
	homePager varchar(100) null,
	homeTollFree varchar(100) null,
	homeEmailAddress varchar(100) null,
	businessCompany varchar(100) null,
	businessStreet varchar(100) null,
	businessCity varchar(100) null,
	businessState varchar(100) null,
	businessZip varchar(100) null,
	businessCountry varchar(100) null,
	businessPhone varchar(100) null,
	businessFax varchar(100) null,
	businessCell varchar(100) null,
	businessPager varchar(100) null,
	businessTollFree varchar(100) null,
	businessEmailAddress varchar(100) null,
	employeeNumber varchar(100) null,
	jobTitle varchar(100) null,
	jobClass varchar(100) null,
	hoursOfOperation text null,
	birthday timestamp null,
	timeZoneId varchar(100) null,
	instantMessenger varchar(100) null,
	website varchar(100) null,
	comments text null
);

create table ABContacts_ABLists (
	contactId varchar(100) not null,
	listId varchar(100) not null
);

create table ABList (
	listId varchar(100) not null primary key,
	userId varchar(100) not null,
	name varchar(100) null
);

create table Address (
	addressId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate timestamp null,
	modifiedDate timestamp null,
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
	createDate timestamp null,
	modifiedDate timestamp null,
	name varchar(100) null,
	content text null,
	versesInput text null
);

create table BJTopic (
	topicId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	createDate timestamp null,
	modifiedDate timestamp null,
	name varchar(100) null,
	description text null
);

create table BJVerse (
	verseId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	name varchar(100) null
);

create table BlogsCategory (
	categoryId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	createDate timestamp null,
	modifiedDate timestamp null,
	name varchar(100) null
);

create table BlogsComments (
	commentsId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate timestamp null,
	modifiedDate timestamp null,
	entryId varchar(100) null,
	content text null
);

create table BlogsEntry (
	entryId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	createDate timestamp null,
	modifiedDate timestamp null,
	categoryId varchar(100) null,
	title varchar(100) null,
	content text null,
	displayDate timestamp null,
	sharing bool,
	commentable bool,
	propsCount integer,
	commentsCount integer
);

create table BlogsLink (
	linkId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	createDate timestamp null,
	modifiedDate timestamp null,
	name varchar(100) null,
	url varchar(100) null
);

create table BlogsProps (
	propsId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate timestamp null,
	modifiedDate timestamp null,
	entryId varchar(100) null,
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
	lastPostDate timestamp null
);

create table BookmarksEntry (
	entryId varchar(100) not null primary key,
	userId varchar(100) not null,
	createDate timestamp null,
	modifiedDate timestamp null,
	folderId varchar(100) null,
	name varchar(100) null,
	url varchar(100) null,
	comments text null,
	visits integer
);

create table BookmarksFolder (
	folderId varchar(100) not null primary key,
	userId varchar(100) not null,
	createDate timestamp null,
	modifiedDate timestamp null,
	parentFolderId varchar(100) null,
	name varchar(100) null
);

create table CalEvent (
	eventId varchar(100) not null primary key,
	groupId varchar(100) not null,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate timestamp null,
	modifiedDate timestamp null,
	title varchar(100) null,
	description text null,
	startDate timestamp null,
	endDate timestamp null,
	durationHour integer,
	durationMinute integer,
	allDay bool,
	timeZoneSensitive bool,
	type_ varchar(100) null,
	location varchar(100) null,
	street varchar(100) null,
	city varchar(100) null,
	state varchar(100) null,
	zip varchar(100) null,
	phone varchar(100) null,
	repeating bool,
	recurrence text null,
	remindBy varchar(100) null,
	firstReminder integer,
	secondReminder integer
);

create table CalTask (
	taskId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	createDate timestamp null,
	modifiedDate timestamp null,
	title varchar(100) null,
	description text null,
	noDueDate bool,
	dueDate timestamp null,
	priority integer,
	status integer
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
	autoLogin bool,
	strangers bool
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
	userName varchar(100) null,
	versionUserId varchar(100) not null,
	versionUserName varchar(100) null,
	createDate timestamp null,
	modifiedDate timestamp null,
	readRoles varchar(100) null,
	writeRoles varchar(100) null,
	description text null,
	version double precision,
	size_ integer,
	primary key (companyId, repositoryId, fileName)
);

create table DLFileRank (
	companyId varchar(100) not null,
	userId varchar(100) not null,
	repositoryId varchar(100) not null,
	fileName varchar(100) not null,
	createDate timestamp null,
	primary key (companyId, userId, repositoryId, fileName)
);

create table DLFileVersion (
	companyId varchar(100) not null,
	repositoryId varchar(100) not null,
	fileName varchar(100) not null,
	version double precision not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate timestamp null,
	size_ integer,
	primary key (companyId, repositoryId, fileName, version)
);

create table DLRepository (
	repositoryId varchar(100) not null primary key,
	groupId varchar(100) not null,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate timestamp null,
	modifiedDate timestamp null,
	readRoles varchar(100) null,
	writeRoles varchar(100) null,
	name varchar(100) null,
	description text null,
	lastPostDate timestamp null
);

create table IGFolder (
	folderId varchar(100) not null primary key,
	groupId varchar(100) not null,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	createDate timestamp null,
	modifiedDate timestamp null,
	parentFolderId varchar(100) null,
	name varchar(100) null
);

create table IGImage (
	imageId varchar(100) not null,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	createDate timestamp null,
	modifiedDate timestamp null,
	folderId varchar(100) null,
	description text null,
	height integer,
	width integer,
	size_ integer,
	primary key (imageId, companyId)
);

create table Image (
	imageId varchar(200) not null primary key,
	text_ text not null
);

create table JournalArticle (
	articleId varchar(100) not null,
	version double precision not null,
	portletId varchar(100) not null,
	groupId varchar(100) not null,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate timestamp null,
	modifiedDate timestamp null,
	title varchar(100) null,
	content text null,
	type_ varchar(100) null,
	structureId varchar(100) null,
	templateId varchar(100) null,
	displayDate timestamp null,
	expirationDate timestamp null,
	approved bool,
	approvedByUserId varchar(100) null,
	approvedByUserName varchar(100) null,
	primary key (articleId, version)
);

create table JournalStructure (
	structureId varchar(100) not null primary key,
	portletId varchar(100) not null,
	groupId varchar(100) not null,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate timestamp null,
	modifiedDate timestamp null,
	name varchar(100) null,
	description text null,
	xsd text null
);

create table JournalTemplate (
	templateId varchar(100) not null primary key,
	portletId varchar(100) not null,
	groupId varchar(100) not null,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate timestamp null,
	modifiedDate timestamp null,
	structureId varchar(100) null,
	name varchar(100) null,
	description text null,
	xsl text null,
	smallImage bool,
	smallImageURL varchar(100) null
);

create table Layer (
	layerId varchar(100) not null,
	skinId varchar(100) not null,
	href varchar(100) null,
	hrefHover varchar(100) null,
	background varchar(100) null,
	foreground varchar(100) null,
	negAlert varchar(100) null,
	posAlert varchar(100) null,
	primary key (layerId, skinId)
);

create table MailReceipt (
	receiptId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	createDate timestamp null,
	modifiedDate timestamp null,
	recipientName varchar(100) null,
	recipientAddress varchar(100) null,
	subject varchar(100) null,
	sentDate timestamp null,
	readCount integer,
	firstReadDate timestamp null,
	lastReadDate timestamp null
);

create table MBMessage (
	messageId varchar(100) not null,
	topicId varchar(100) not null,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate timestamp null,
	modifiedDate timestamp null,
	threadId varchar(100) null,
	parentMessageId varchar(100) null,
	subject varchar(100) null,
	body text null,
	attachments bool,
	anonymous bool,
	primary key (messageId, topicId)
);

create table MBMessageFlag (
	topicId varchar(100) not null,
	messageId varchar(100) not null,
	userId varchar(100) not null,
	flag varchar(100) null,
	primary key (topicId, messageId, userId)
);

create table MBThread (
	threadId varchar(100) not null primary key,
	rootMessageId varchar(100) null,
	topicId varchar(100) null,
	messageCount integer,
	lastPostDate timestamp null
);

create table MBTopic (
	topicId varchar(100) not null primary key,
	portletId varchar(100) not null,
	groupId varchar(100) not null,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate timestamp null,
	modifiedDate timestamp null,
	readRoles varchar(100) null,
	writeRoles varchar(100) null,
	name varchar(100) null,
	description text null,
	lastPostDate timestamp null
);

create table NetworkAddress (
	addressId varchar(100) not null primary key,
	userId varchar(100) not null,
	createDate timestamp null,
	modifiedDate timestamp null,
	name varchar(100) null,
	url varchar(100) null,
	comments text null,
	content text null,
	status integer,
	lastUpdated timestamp null,
	notifyBy varchar(100) null,
	interval_ integer,
	active_ bool
);

create table Note (
	noteId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate timestamp null,
	modifiedDate timestamp null,
	className varchar(100) null,
	classPK varchar(100) null,
	content text null
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
	createDate timestamp null,
	modifiedDate timestamp null,
	title varchar(100) null,
	description text null,
	expirationDate timestamp null,
	lastVoteDate timestamp null
);

create table PollsVote (
	questionId varchar(100) not null,
	userId varchar(100) not null,
	choiceId varchar(100) not null,
	voteDate timestamp null,
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

create table ProjFirm (
	firmId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate timestamp null,
	modifiedDate timestamp null,
	name varchar(100) null,
	description text null,
	url varchar(100) null
);

create table ProjProject (
	projectId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate timestamp null,
	modifiedDate timestamp null,
	firmId varchar(100) null,
	code varchar(100) null,
	name varchar(100) null,
	description text null
);

create table ProjTask (
	taskId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate timestamp null,
	modifiedDate timestamp null,
	projectId varchar(100) null,
	name varchar(100) null,
	description text null,
	comments text null,
	estimatedDuration integer,
	estimatedEndDate timestamp null,
	actualDuration integer,
	actualEndDate timestamp null,
	status integer
);

create table ProjTime (
	timeId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate timestamp null,
	modifiedDate timestamp null,
	projectId varchar(100) null,
	taskId varchar(100) null,
	description text null,
	startDate timestamp null,
	endDate timestamp null
);

create table Release_ (
	releaseId varchar(100) not null primary key,
	createDate timestamp null,
	modifiedDate timestamp null,
	buildNumber integer null,
	buildDate timestamp null
);

create table Skin (
	skinId varchar(100) not null primary key,
	name varchar(100) null,
	imageId varchar(100) null,
	alphaLayerId varchar(100) null,
	alphaSkinId varchar(100) null,
	betaLayerId varchar(100) null,
	betaSkinId varchar(100) null,
	gammaLayerId varchar(100) null,
	gammaSkinId varchar(100) null,
	bgLayerId varchar(100) null,
	bgSkinId varchar(100) null
);

create table ShoppingCart (
	cartId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	createDate timestamp null,
	modifiedDate timestamp null,
	itemIds text null,
	couponIds text null,
	altShipping integer
);

create table ShoppingCategory (
	categoryId varchar(100) not null primary key,
	companyId varchar(100) not null,
	createDate timestamp null,
	modifiedDate timestamp null,
	parentCategoryId varchar(100) null,
	name varchar(100) null
);

create table ShoppingCoupon (
	couponId varchar(100) not null primary key,
	companyId varchar(100) not null,
	createDate timestamp null,
	modifiedDate timestamp null,
	name varchar(100) null,
	description text null,
	startDate timestamp null,
	endDate timestamp null,
	active_ bool,
	limitCategories text null,
	limitSkus text null,
	minOrder double precision,
	discount double precision,
	discountType varchar(100) null
);

create table ShoppingItem (
	itemId varchar(100) not null primary key,
	companyId varchar(100) not null,
	createDate timestamp null,
	modifiedDate timestamp null,
	categoryId varchar(100) null,
	sku varchar(100) null,
	name varchar(100) null,
	description text null,
	properties text null,
	supplierUserId varchar(100) null,
	fields_ bool,
	fieldsQuantities text null,
	minQuantity integer,
	maxQuantity integer,
	price double precision,
	discount double precision,
	taxable bool,
	shipping double precision,
	useShippingFormula bool,
	requiresShipping bool,
	stockQuantity integer,
	featured_ bool,
	sale_ bool,
	smallImage bool,
	smallImageURL varchar(100) null,
	mediumImage bool,
	mediumImageURL varchar(100) null,
	largeImage bool,
	largeImageURL varchar(100) null
);

create table ShoppingItemField (
	itemFieldId varchar(100) not null primary key,
	itemId varchar(100) null,
	name varchar(100) null,
	values_ text null,
	description text null
);

create table ShoppingItemPrice (
	itemPriceId varchar(100) not null primary key,
	itemId varchar(100) null,
	minQuantity integer,
	maxQuantity integer,
	price double precision,
	discount double precision,
	taxable bool,
	shipping double precision,
	useShippingFormula bool,
	status integer
);

create table ShoppingOrder (
	orderId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	createDate timestamp null,
	modifiedDate timestamp null,
	tax double precision,
	shipping double precision,
	altShipping varchar(100),
	requiresShipping bool,
	couponIds text null,
	couponDiscount double precision,
	billingFirstName varchar(100) null,
	billingLastName varchar(100) null,
	billingEmailAddress varchar(100) null,
	billingCompany varchar(100) null,
	billingStreet varchar(100) null,
	billingCity varchar(100) null,
	billingState varchar(100) null,
	billingZip varchar(100) null,
	billingCountry varchar(100) null,
	billingPhone varchar(100) null,
	shipToBilling bool,
	shippingFirstName varchar(100) null,
	shippingLastName varchar(100) null,
	shippingEmailAddress varchar(100) null,
	shippingCompany varchar(100) null,
	shippingStreet varchar(100) null,
	shippingCity varchar(100) null,
	shippingState varchar(100) null,
	shippingZip varchar(100) null,
	shippingCountry varchar(100) null,
	shippingPhone varchar(100) null,
	ccName varchar(100) null,
	ccType varchar(100) null,
	ccNumber varchar(100) null,
	ccExpMonth integer,
	ccExpYear integer,
	ccVerNumber varchar(100) null,
	comments text null,
	ppTxnId varchar(100) null,
	ppPaymentStatus varchar(100) null,
	ppPaymentGross double precision,
	ppReceiverEmail varchar(100) null,
	ppPayerEmail varchar(100) null,
	sendOrderEmail bool,
	sendShippingEmail bool
);

create table ShoppingOrderItem (
	orderId varchar(100) not null,
	itemId varchar(100) not null,
	sku varchar(100) null,
	name varchar(100) null,
	description text null,
	properties text null,
	supplierUserId varchar(100) null,
	price double precision,
	quantity integer,
	shippedDate timestamp null,
	primary key (orderId, itemId)
);

create table User_ (
	userId varchar(100) not null primary key,
	companyId varchar(100) not null,
	createDate timestamp null,
	password_ varchar(100) null,
	passwordEncrypted bool,
	passwordExpirationDate timestamp null,
	passwordReset bool,
	firstName varchar(100) null,
	middleName varchar(100) null,
	lastName varchar(100) null,
	nickName varchar(100) null,
	male bool,
	birthday timestamp null,
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
	loginDate timestamp null,
	loginIP varchar(100) null,
	lastLoginDate timestamp null,
	lastLoginIP varchar(100) null,
	failedLoginAttempts integer,
	agreedToTermsOfUse bool,
	active_ bool
);

create table Users_ProjProjects (
	userId varchar(100) not null,
	projectId varchar(100) not null
);

create table Users_ProjTasks (
	userId varchar(100) not null,
	taskId varchar(100) not null
);

create table UserTracker (
	userTrackerId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	modifiedDate timestamp null,
	remoteAddr varchar(100) null,
	remoteHost varchar(100) null,
	userAgent varchar(100) null
);

create table UserTrackerPath (
	userTrackerPathId varchar(100) not null primary key,
	userTrackerId varchar(100) not null,
	path text not null,
	pathDate timestamp not null
);

create table WikiDisplay (
	layoutId varchar(100) not null,
	userId varchar(100) not null,
	portletId varchar(100) not null,
	nodeId varchar(100) not null,
	showBorders bool,
	primary key (layoutId, userId, portletId)
);

create table WikiNode (
	nodeId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate timestamp null,
	modifiedDate timestamp null,
	readRoles varchar(100) null,
	writeRoles varchar(100) null,
	name varchar(100) null,
	description text null,
	sharing bool,
	lastPostDate timestamp null
);

create table WikiPage (
	nodeId varchar(100) not null,
	title varchar(100) not null,
	version double precision not null,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate timestamp null,
	content text null,
	format varchar(100) null,
	head bool,
	primary key (nodeId, title, version)
);

--
-- Global
--

insert into Counter values ('com.liferay.portal.model.Address', 10);
insert into Counter values ('com.liferay.portal.model.Group', 20);
insert into Counter values ('com.liferay.portal.model.Role', 100);
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

insert into Company (companyId, portalURL, homeURL, mx, name, shortName, type_, size_, emailAddress, authType, autoLogin, strangers) values ('liferay.com', 'localhost', 'localhost', 'liferay.com', 'Liferay, LLC', 'Liferay', 'biz', '', 'test@liferay.com', 'emailAddress', 't', 't');
update Company set street = '1220 Brea Canyon Rd.', city = 'Diamond Bar', state = 'CA', zip = '91789' where companyId = 'liferay.com';

insert into PollsDisplay (layoutId, userId, portletId, questionId) values ('1.1', 'group.1', '59', '1');
insert into PollsChoice (choiceId, questionId, description) values ('a', '1', 'Chocolate');
insert into PollsChoice (choiceId, questionId, description) values ('b', '1', 'Strawberry');
insert into PollsChoice (choiceId, questionId, description) values ('c', '1', 'Vanilla');
insert into PollsQuestion (questionId, portletId, groupId, companyId, userId, userName, createDate, modifiedDate, title, description) values ('1', '25', '-1', 'liferay.com', 'liferay.com.1', 'John Wayne', current_timestamp, current_timestamp, 'What is your favorite ice cream flavor?', 'What is your favorite ice cream flavor?');

insert into WikiDisplay (layoutId, userId, portletId, nodeId, showBorders) values ('1.1', 'group.1', '54', '1', 't');
insert into WikiNode (nodeId, companyId, userId, userName, createDate, modifiedDate, readRoles, writeRoles, name, description, sharing, lastPostDate) values ('1', 'liferay.com', 'liferay.com.1', 'John Wayne', current_timestamp, current_timestamp, 'User,' ,'User,', 'Welcome', '', 't', current_timestamp);
insert into WikiPage (nodeId, title, version, companyId, userId, userName, createDate, content, format, head) values ('1', 'FrontPage', 1.0, 'liferay.com', 'liferay.com.1', 'John Wayne', current_timestamp, '<font class="bg" size="2">Welcome! Thank you for your interest in Liferay Enterprise Portal.<br><br>Your login is <b>test@liferay.com</b> and your password is <b>test</b>. The test user has the Administrator role.<br><br>To use the <b>Mail</b> portlet, make sure there is a mail server running on <i>localhost</i> accessible through the IMAP protocol.<br><br>The mail server must have an account with <b>liferay.com.1</b> as the user and <b>test</b> as the password.<br><br><hr><br>Is Liferay useful for your company? Tell us about it by email at <b>staff@liferay.com</b>.<br><br>We hope you enjoy our product!</font>', 'html', 't');

--
-- Default User
--

insert into User_ (userId, companyId, createDate, password_, passwordEncrypted, passwordReset, firstName, middleName, lastName, male, birthday, emailAddress, skinId, dottedSkins, roundedSkins, greeting, layoutIds, loginDate, failedLoginAttempts, agreedToTermsOfUse, active_) values ('liferay.com.default', 'default', current_timestamp, 'password', 'f', 'f', '', '', '', 't', '01/01/1970', 'default@liferay.com', '01', 'f', 'f', 'Welcome!', '', current_timestamp, 0, 'f', 't');
--
-- Test User
--

insert into User_ (userId, companyId, createDate, password_, passwordEncrypted, passwordReset, firstName, middleName, lastName, nickName, male, birthday, emailAddress, skinId, dottedSkins, roundedSkins, greeting, layoutIds, loginDate, failedLoginAttempts, agreedToTermsOfUse, active_) values ('liferay.com.1', 'liferay.com', current_timestamp, 'test', 'f', 'f', 'John', '', 'Wayne', 'Duke', 't', '01/01/1970', 'test@liferay.com', '01', 'f', 't', 'Welcome John Wayne!', '1,', current_timestamp, 0, 't', 't');
