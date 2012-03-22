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
	hoursOfOperation long varchar null,
	birthday date null,
	timeZoneId varchar(100) null,
	instantMessenger varchar(100) null,
	website varchar(100) null,
	comments long varchar null
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
	createDate date null,
	modifiedDate date null,
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
	config long varchar null
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
	createDate date null,
	modifiedDate date null,
	name varchar(100) null,
	content long varchar null,
	versesInput long varchar null
);

create table BJTopic (
	topicId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	createDate date null,
	modifiedDate date null,
	name varchar(100) null,
	description long varchar null
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
	createDate date null,
	modifiedDate date null,
	name varchar(100) null
);

create table BlogsComments (
	commentsId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate date null,
	modifiedDate date null,
	entryId varchar(100) null,
	content long varchar null
);

create table BlogsEntry (
	entryId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	createDate date null,
	modifiedDate date null,
	categoryId varchar(100) null,
	title varchar(100) null,
	content long varchar null,
	displayDate date null,
	sharing boolean,
	commentable boolean,
	propsCount integer,
	commentsCount integer
);

create table BlogsLink (
	linkId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	createDate date null,
	modifiedDate date null,
	name varchar(100) null,
	url varchar(100) null
);

create table BlogsProps (
	propsId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate date null,
	modifiedDate date null,
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
	lastPostDate date null
);

create table BookmarksEntry (
	entryId varchar(100) not null primary key,
	userId varchar(100) not null,
	createDate date null,
	modifiedDate date null,
	folderId varchar(100) null,
	name varchar(100) null,
	url varchar(100) null,
	comments long varchar null,
	visits integer
);

create table BookmarksFolder (
	folderId varchar(100) not null primary key,
	userId varchar(100) not null,
	createDate date null,
	modifiedDate date null,
	parentFolderId varchar(100) null,
	name varchar(100) null
);

create table CalEvent (
	eventId varchar(100) not null primary key,
	groupId varchar(100) not null,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate date null,
	modifiedDate date null,
	title varchar(100) null,
	description long varchar null,
	startDate date null,
	endDate date null,
	durationHour integer,
	durationMinute integer,
	allDay boolean,
	timeZoneSensitive boolean,
	type_ varchar(100) null,
	location varchar(100) null,
	street varchar(100) null,
	city varchar(100) null,
	state varchar(100) null,
	zip varchar(100) null,
	phone varchar(100) null,
	repeating boolean,
	recurrence long varchar null,
	remindBy varchar(100) null,
	firstReminder integer,
	secondReminder integer
);

create table CalTask (
	taskId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	createDate date null,
	modifiedDate date null,
	title varchar(100) null,
	description long varchar null,
	noDueDate boolean,
	dueDate date null,
	priority integer,
	status integer
);

create table Company (
	companyId varchar(100) not null primary key,
	key_ long varchar null,
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
	autoLogin boolean,
	strangers boolean
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
	createDate date null,
	modifiedDate date null,
	readRoles varchar(100) null,
	writeRoles varchar(100) null,
	description long varchar null,
	version double,
	size_ integer,
	primary key (companyId, repositoryId, fileName)
);

create table DLFileRank (
	companyId varchar(100) not null,
	userId varchar(100) not null,
	repositoryId varchar(100) not null,
	fileName varchar(100) not null,
	createDate date null,
	primary key (companyId, userId, repositoryId, fileName)
);

create table DLFileVersion (
	companyId varchar(100) not null,
	repositoryId varchar(100) not null,
	fileName varchar(100) not null,
	version double not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate date null,
	size_ integer,
	primary key (companyId, repositoryId, fileName, version)
);

create table DLRepository (
	repositoryId varchar(100) not null primary key,
	groupId varchar(100) not null,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate date null,
	modifiedDate date null,
	readRoles varchar(100) null,
	writeRoles varchar(100) null,
	name varchar(100) null,
	description long varchar null,
	lastPostDate date null
);

create table Group_ (
	groupId varchar(100) not null primary key,
	companyId varchar(100) not null,
	parentGroupId varchar(100) null,
	name varchar(100) null,
	layoutIds varchar(100) null
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
	createDate date null,
	modifiedDate date null,
	parentFolderId varchar(100) null,
	name varchar(100) null
);

create table IGImage (
	imageId varchar(100) not null,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	createDate date null,
	modifiedDate date null,
	folderId varchar(100) null,
	description long varchar null,
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
	userName varchar(100) null,
	createDate date null,
	modifiedDate date null,
	title varchar(100) null,
	content long varchar null,
	type_ varchar(100) null,
	structureId varchar(100) null,
	templateId varchar(100) null,
	displayDate date null,
	expirationDate date null,
	approved boolean,
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
	createDate date null,
	modifiedDate date null,
	name varchar(100) null,
	description long varchar null,
	xsd long varchar null
);

create table JournalTemplate (
	templateId varchar(100) not null primary key,
	portletId varchar(100) not null,
	groupId varchar(100) not null,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate date null,
	modifiedDate date null,
	structureId varchar(100) null,
	name varchar(100) null,
	description long varchar null,
	xsl long varchar null,
	smallImage boolean,
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

create table Layout (
	layoutId varchar(100) not null,
	userId varchar(100) not null,
	name varchar(100) null,
	columnOrder varchar(100) null,
	narrow1 long varchar null,
	narrow2 long varchar null,
	wide long varchar null,
	stateMax long varchar null,
	stateMin long varchar null,
	modeEdit long varchar null,
	modeHelp long varchar null,
	primary key (layoutId, userId)
);

create table MailReceipt (
	receiptId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	createDate date null,
	modifiedDate date null,
	recipientName varchar(100) null,
	recipientAddress varchar(100) null,
	subject varchar(100) null,
	sentDate date null,
	readCount integer,
	firstReadDate date null,
	lastReadDate date null
);

create table MBMessage (
	messageId varchar(100) not null,
	topicId varchar(100) not null,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate date null,
	modifiedDate date null,
	threadId varchar(100) null,
	parentMessageId varchar(100) null,
	subject varchar(100) null,
	body long varchar null,
	attachments boolean,
	anonymous boolean,
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
	lastPostDate date null
);

create table MBTopic (
	topicId varchar(100) not null primary key,
	portletId varchar(100) not null,
	groupId varchar(100) not null,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate date null,
	modifiedDate date null,
	readRoles varchar(100) null,
	writeRoles varchar(100) null,
	name varchar(100) null,
	description long varchar null,
	lastPostDate date null
);

create table NetworkAddress (
	addressId varchar(100) not null primary key,
	userId varchar(100) not null,
	createDate date null,
	modifiedDate date null,
	name varchar(100) null,
	url varchar(100) null,
	comments long varchar null,
	content long varchar null,
	status integer,
	lastUpdated date null,
	notifyBy varchar(100) null,
	interval_ integer,
	active_ boolean
);

create table Note (
	noteId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate date null,
	modifiedDate date null,
	className varchar(100) null,
	classPK varchar(100) null,
	content long varchar null
);

create table PasswordTracker (
	passwordTrackerId varchar(100) not null primary key,
	userId varchar(100) not null,
	createDate date not null,
	password_ varchar(100) not null
);

create table PollsChoice (
	choiceId varchar(100) not null,
	questionId varchar(100) not null,
	description long varchar null,
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
	createDate date null,
	modifiedDate date null,
	title varchar(100) null,
	description long varchar null,
	expirationDate date null,
	lastVoteDate date null
);

create table PollsVote (
	questionId varchar(100) not null,
	userId varchar(100) not null,
	choiceId varchar(100) not null,
	voteDate date null,
	primary key (questionId, userId)
);

create table Portlet (
	portletId varchar(100) not null,
	groupId varchar(100) not null,
	companyId varchar(100) not null,
	defaultPreferences long varchar null,
	narrow boolean,
	roles long varchar null,
	active_ boolean,
	primary key (portletId, groupId, companyId)
);

create table PortletPreferences (
	portletId varchar(100) not null,
	userId varchar(100) not null,
	layoutId varchar(100) not null,
	preferences long varchar null,
	primary key (portletId, userId, layoutId)
);

create table ProjFirm (
	firmId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate date null,
	modifiedDate date null,
	name varchar(100) null,
	description long varchar null,
	url varchar(100) null
);

create table ProjProject (
	projectId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate date null,
	modifiedDate date null,
	firmId varchar(100) null,
	code varchar(100) null,
	name varchar(100) null,
	description long varchar null
);

create table ProjTask (
	taskId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate date null,
	modifiedDate date null,
	projectId varchar(100) null,
	name varchar(100) null,
	description long varchar null,
	comments long varchar null,
	estimatedDuration integer,
	estimatedEndDate date null,
	actualDuration integer,
	actualEndDate date null,
	status integer
);

create table ProjTime (
	timeId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate date null,
	modifiedDate date null,
	projectId varchar(100) null,
	taskId varchar(100) null,
	description long varchar null,
	startDate date null,
	endDate date null
);

create table Release_ (
	releaseId varchar(100) not null primary key,
	createDate date null,
	modifiedDate date null,
	buildNumber integer null,
	buildDate date null
);

create table Role_ (
	roleId varchar(100) not null primary key,
	companyId varchar(100) not null,
	name varchar(100) not null
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
	createDate date null,
	modifiedDate date null,
	itemIds long varchar null,
	couponIds long varchar null,
	altShipping integer
);

create table ShoppingCategory (
	categoryId varchar(100) not null primary key,
	companyId varchar(100) not null,
	createDate date null,
	modifiedDate date null,
	parentCategoryId varchar(100) null,
	name varchar(100) null
);

create table ShoppingCoupon (
	couponId varchar(100) not null primary key,
	companyId varchar(100) not null,
	createDate date null,
	modifiedDate date null,
	name varchar(100) null,
	description long varchar null,
	startDate date null,
	endDate date null,
	active_ boolean,
	limitCategories long varchar null,
	limitSkus long varchar null,
	minOrder double,
	discount double,
	discountType varchar(100) null
);

create table ShoppingItem (
	itemId varchar(100) not null primary key,
	companyId varchar(100) not null,
	createDate date null,
	modifiedDate date null,
	categoryId varchar(100) null,
	sku varchar(100) null,
	name varchar(100) null,
	description long varchar null,
	properties long varchar null,
	supplierUserId varchar(100) null,
	fields_ boolean,
	fieldsQuantities long varchar null,
	minQuantity integer,
	maxQuantity integer,
	price double,
	discount double,
	taxable boolean,
	shipping double,
	useShippingFormula boolean,
	requiresShipping boolean,
	stockQuantity integer,
	featured_ boolean,
	sale_ boolean,
	smallImage boolean,
	smallImageURL varchar(100) null,
	mediumImage boolean,
	mediumImageURL varchar(100) null,
	largeImage boolean,
	largeImageURL varchar(100) null
);

create table ShoppingItemField (
	itemFieldId varchar(100) not null primary key,
	itemId varchar(100) null,
	name varchar(100) null,
	values_ long varchar null,
	description long varchar null
);

create table ShoppingItemPrice (
	itemPriceId varchar(100) not null primary key,
	itemId varchar(100) null,
	minQuantity integer,
	maxQuantity integer,
	price double,
	discount double,
	taxable boolean,
	shipping double,
	useShippingFormula boolean,
	status integer
);

create table ShoppingOrder (
	orderId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	createDate date null,
	modifiedDate date null,
	tax double,
	shipping double,
	altShipping varchar(100),
	requiresShipping boolean,
	couponIds long varchar null,
	couponDiscount double,
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
	shipToBilling boolean,
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
	comments long varchar null,
	ppTxnId varchar(100) null,
	ppPaymentStatus varchar(100) null,
	ppPaymentGross double,
	ppReceiverEmail varchar(100) null,
	ppPayerEmail varchar(100) null,
	sendOrderEmail boolean,
	sendShippingEmail boolean
);

create table ShoppingOrderItem (
	orderId varchar(100) not null,
	itemId varchar(100) not null,
	sku varchar(100) null,
	name varchar(100) null,
	description long varchar null,
	properties long varchar null,
	supplierUserId varchar(100) null,
	price double,
	quantity integer,
	shippedDate date null,
	primary key (orderId, itemId)
);

create table User_ (
	userId varchar(100) not null primary key,
	companyId varchar(100) not null,
	createDate date null,
	password_ varchar(100) null,
	passwordEncrypted boolean,
	passwordExpirationDate date null,
	passwordReset boolean,
	firstName varchar(100) null,
	middleName varchar(100) null,
	lastName varchar(100) null,
	nickName varchar(100) null,
	male boolean,
	birthday date null,
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
	dottedSkins boolean,
	roundedSkins boolean,
	greeting varchar(100) null,
	resolution varchar(100) null,
	refreshRate varchar(100) null,
	layoutIds varchar(100) null,
	comments long varchar null,
	loginDate date null,
	loginIP varchar(100) null,
	lastLoginDate date null,
	lastLoginIP varchar(100) null,
	failedLoginAttempts integer,
	agreedToTermsOfUse boolean,
	active_ boolean
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
	modifiedDate date null,
	remoteAddr varchar(100) null,
	remoteHost varchar(100) null,
	userAgent varchar(100) null
);

create table UserTrackerPath (
	userTrackerPathId varchar(100) not null primary key,
	userTrackerId varchar(100) not null,
	path long varchar not null,
	pathDate date not null
);

create table WikiDisplay (
	layoutId varchar(100) not null,
	userId varchar(100) not null,
	portletId varchar(100) not null,
	nodeId varchar(100) not null,
	showBorders boolean,
	primary key (layoutId, userId, portletId)
);

create table WikiNode (
	nodeId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate date null,
	modifiedDate date null,
	readRoles varchar(100) null,
	writeRoles varchar(100) null,
	name varchar(100) null,
	description long varchar null,
	sharing boolean,
	lastPostDate date null
);

create table WikiPage (
	nodeId varchar(100) not null,
	title varchar(100) not null,
	version double not null,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate date null,
	content long varchar null,
	format varchar(100) null,
	head boolean,
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

insert into Company (companyId, portalURL, homeURL, mx, name, shortName, type_, size_, emailAddress, authType, autoLogin, strangers) values ('liferay.com', 'localhost', 'localhost', 'liferay.com', 'Liferay, LLC', 'Liferay', 'biz', '', 'test@liferay.com', 'emailAddress', 'TRUE', 'TRUE');
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

insert into WikiDisplay (layoutId, userId, portletId, nodeId, showBorders) values ('1.1', 'group.1', '54', '1', 'TRUE');
insert into WikiNode (nodeId, companyId, userId, userName, createDate, modifiedDate, readRoles, writeRoles, name, description, sharing, lastPostDate) values ('1', 'liferay.com', 'liferay.com.1', 'John Wayne', current_timestamp, current_timestamp, 'User,' ,'User,', 'Welcome', '', 'TRUE', current_timestamp);
insert into WikiPage (nodeId, title, version, companyId, userId, userName, createDate, content, format, head) values ('1', 'FrontPage', 1.0, 'liferay.com', 'liferay.com.1', 'John Wayne', current_timestamp, '<font class="bg" size="2">Welcome! Thank you for your interest in Liferay Enterprise Portal.<br><br>Your login is <b>test@liferay.com</b> and your password is <b>test</b>. The test user has the Administrator role.<br><br>To use the <b>Mail</b> portlet, make sure there is a mail server running on <i>localhost</i> accessible through the IMAP protocol.<br><br>The mail server must have an account with <b>liferay.com.1</b> as the user and <b>test</b> as the password.<br><br><hr><br>Is Liferay useful for your company? Tell us about it by email at <b>staff@liferay.com</b>.<br><br>We hope you enjoy our product!</font>', 'html', 'TRUE');

--
-- Default User
--

insert into User_ (userId, companyId, createDate, password_, passwordEncrypted, passwordReset, firstName, middleName, lastName, male, birthday, emailAddress, skinId, dottedSkins, roundedSkins, greeting, layoutIds, loginDate, failedLoginAttempts, agreedToTermsOfUse, active_) values ('liferay.com.default', 'default', current_timestamp, 'password', 'FALSE', 'FALSE', '', '', '', 'TRUE', '1970-01-01', 'default@liferay.com', '01', 'FALSE', 'FALSE', 'Welcome!', '', current_timestamp, 0, 'FALSE', 'TRUE');
insert into Layout (layoutId, userId, name, columnOrder, narrow1, narrow2, wide) values ('1.1', 'group.1', 'Welcome', 'n1,w,', '59,', '', '2,54,');
insert into Layout (layoutId, userId, name, columnOrder, narrow1, narrow2, wide, stateMax) values ('1.2', 'group.1', 'Shopping', 'w,', '', '', '34,', '34,');
insert into Layout (layoutId, userId, name, columnOrder, narrow1, narrow2, wide, stateMax) values ('3.1', 'group.3', 'CMS', 'n1,w,', '', '', '15,31,', '');

--
-- Test User
--

insert into User_ (userId, companyId, createDate, password_, passwordEncrypted, passwordReset, firstName, middleName, lastName, nickName, male, birthday, emailAddress, skinId, dottedSkins, roundedSkins, greeting, layoutIds, loginDate, failedLoginAttempts, agreedToTermsOfUse, active_) values ('liferay.com.1', 'liferay.com', current_timestamp, 'test', 'FALSE', 'FALSE', 'John', '', 'Wayne', 'Duke', 'TRUE', '1970-01-01', 'test@liferay.com', '01', 'FALSE', 'TRUE', 'Welcome John Wayne!', '1,', current_timestamp, 0, 'TRUE', 'TRUE');
insert into Layout (layoutId, userId, name, columnOrder, narrow1, narrow2, wide) values ('1', 'liferay.com.1', 'Home', 'n1,w,', '9,36,4,3,', '', '5,8,');
insert into Users_Groups values ('liferay.com.1', '1');
insert into Users_Groups values ('liferay.com.1', '3');
insert into Users_Roles values ('liferay.com.1', '1');
insert into Users_Roles values ('liferay.com.1', '12');
insert into Users_Roles values ('liferay.com.1', '15');
