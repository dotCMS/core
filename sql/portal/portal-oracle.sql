create table ABContact (
	contactId varchar2(100) not null primary key,
	userId varchar2(100) not null,
	firstName varchar2(100) null,
	middleName varchar2(100) null,
	lastName varchar2(100) null,
	nickName varchar2(100) null,
	emailAddress varchar2(100) null,
	homeStreet varchar2(100) null,
	homeCity varchar2(100) null,
	homeState varchar2(100) null,
	homeZip varchar2(100) null,
	homeCountry varchar2(100) null,
	homePhone varchar2(100) null,
	homeFax varchar2(100) null,
	homeCell varchar2(100) null,
	homePager varchar2(100) null,
	homeTollFree varchar2(100) null,
	homeEmailAddress varchar2(100) null,
	businessCompany varchar2(100) null,
	businessStreet varchar2(100) null,
	businessCity varchar2(100) null,
	businessState varchar2(100) null,
	businessZip varchar2(100) null,
	businessCountry varchar2(100) null,
	businessPhone varchar2(100) null,
	businessFax varchar2(100) null,
	businessCell varchar2(100) null,
	businessPager varchar2(100) null,
	businessTollFree varchar2(100) null,
	businessEmailAddress varchar2(100) null,
	employeeNumber varchar2(100) null,
	jobTitle varchar2(100) null,
	jobClass varchar2(100) null,
	hoursOfOperation varchar2(4000) null,
	birthday date null,
	timeZoneId varchar2(100) null,
	instantMessenger varchar2(100) null,
	website varchar2(100) null,
	comments varchar2(4000) null
);

create table ABContacts_ABLists (
	contactId varchar2(100) not null,
	listId varchar2(100) not null
);

create table ABList (
	listId varchar2(100) not null primary key,
	userId varchar2(100) not null,
	name varchar2(100) null
);

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

create table BJEntries_BJTopics (
	entryId varchar2(100) not null,
	topicId varchar2(100) not null
);

create table BJEntries_BJVerses (
	entryId varchar2(100) not null,
	verseId varchar2(100) not null
);

create table BJEntry (
	entryId varchar2(100) not null primary key,
	companyId varchar2(100) not null,
	userId varchar2(100) not null,
	createDate date null,
	modifiedDate date null,
	name varchar2(100) null,
	content long varchar null,
	versesInput varchar2(4000) null
);

create table BJTopic (
	topicId varchar2(100) not null primary key,
	companyId varchar2(100) not null,
	userId varchar2(100) not null,
	createDate date null,
	modifiedDate date null,
	name varchar2(100) null,
	description long varchar null
);

create table BJVerse (
	verseId varchar2(100) not null primary key,
	companyId varchar2(100) not null,
	userId varchar2(100) not null,
	name varchar2(100) null
);

create table BlogsCategory (
	categoryId varchar2(100) not null primary key,
	companyId varchar2(100) not null,
	userId varchar2(100) not null,
	createDate date null,
	modifiedDate date null,
	name varchar2(100) null
);

create table BlogsComments (
	commentsId varchar2(100) not null primary key,
	companyId varchar2(100) not null,
	userId varchar2(100) not null,
	userName varchar2(100) null,
	createDate date null,
	modifiedDate date null,
	entryId varchar2(100) null,
	content long varchar null
);

create table BlogsEntry (
	entryId varchar2(100) not null primary key,
	companyId varchar2(100) not null,
	userId varchar2(100) not null,
	createDate date null,
	modifiedDate date null,
	categoryId varchar2(100) null,
	title varchar2(100) null,
	content long varchar null,
	displayDate date null,
	sharing number(1, 0),
	commentable number(1, 0),
	propsCount number(30,0),
	commentsCount number(30,0)
);

create table BlogsLink (
	linkId varchar2(100) not null primary key,
	companyId varchar2(100) not null,
	userId varchar2(100) not null,
	createDate date null,
	modifiedDate date null,
	name varchar2(100) null,
	url varchar2(100) null
);

create table BlogsProps (
	propsId varchar2(100) not null primary key,
	companyId varchar2(100) not null,
	userId varchar2(100) not null,
	userName varchar2(100) null,
	createDate date null,
	modifiedDate date null,
	entryId varchar2(100) null,
	quantity number(30,0)
);

create table BlogsReferer (
	entryId varchar2(100) not null,
	url varchar2(100) not null,
	type_ varchar2(100) not null,
	quantity number(30,0),
	primary key (entryId, url, type_)
);

create table BlogsUser (
	userId varchar2(100) not null primary key,
	companyId varchar2(100) not null,
	entryId varchar2(100) not null,
	lastPostDate date null
);

create table BookmarksEntry (
	entryId varchar2(100) not null primary key,
	userId varchar2(100) not null,
	createDate date null,
	modifiedDate date null,
	folderId varchar2(100) null,
	name varchar2(100) null,
	url varchar2(100) null,
	comments varchar2(4000) null,
	visits number(30,0)
);

create table BookmarksFolder (
	folderId varchar2(100) not null primary key,
	userId varchar2(100) not null,
	createDate date null,
	modifiedDate date null,
	parentFolderId varchar2(100) null,
	name varchar2(100) null
);

create table CalEvent (
	eventId varchar2(100) not null primary key,
	groupId varchar2(100) not null,
	companyId varchar2(100) not null,
	userId varchar2(100) not null,
	userName varchar2(100) null,
	createDate date null,
	modifiedDate date null,
	title varchar2(100) null,
	description varchar2(4000) null,
	startDate date null,
	endDate date null,
	durationHour number(30,0),
	durationMinute number(30,0),
	allDay number(1, 0),
	timeZoneSensitive number(1, 0),
	type_ varchar2(100) null,
	location varchar2(100) null,
	street varchar2(100) null,
	city varchar2(100) null,
	state varchar2(100) null,
	zip varchar2(100) null,
	phone varchar2(100) null,
	repeating number(1, 0),
	recurrence long varchar null,
	remindBy varchar2(100) null,
	firstReminder number(30,0),
	secondReminder number(30,0)
);

create table CalTask (
	taskId varchar2(100) not null primary key,
	companyId varchar2(100) not null,
	userId varchar2(100) not null,
	createDate date null,
	modifiedDate date null,
	title varchar2(100) null,
	description varchar2(4000) null,
	noDueDate number(1, 0),
	dueDate date null,
	priority number(30,0),
	status number(30,0)
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

create table CyrusUser (
	userId varchar2(100) not null primary key,
	password_ varchar2(100) not null
);

create table CyrusVirtual (
	emailAddress varchar2(100) not null primary key,
	userId varchar2(100) not null
);

create table DLFileProfile (
	companyId varchar2(100) not null,
	repositoryId varchar2(100) not null,
	fileName varchar2(100) not null,
	userId varchar2(100) not null,
	userName varchar2(100) null,
	versionUserId varchar2(100) not null,
	versionUserName varchar2(100) null,
	createDate date null,
	modifiedDate date null,
	readRoles varchar2(100) null,
	writeRoles varchar2(100) null,
	description long varchar null,
	version number(30,20),
	size_ number(30,0),
	primary key (companyId, repositoryId, fileName)
);

create table DLFileRank (
	companyId varchar2(100) not null,
	userId varchar2(100) not null,
	repositoryId varchar2(100) not null,
	fileName varchar2(100) not null,
	createDate date null,
	primary key (companyId, userId, repositoryId, fileName)
);

create table DLFileVersion (
	companyId varchar2(100) not null,
	repositoryId varchar2(100) not null,
	fileName varchar2(100) not null,
	version number(30,20) not null,
	userId varchar2(100) not null,
	userName varchar2(100) null,
	createDate date null,
	size_ number(30,0),
	primary key (companyId, repositoryId, fileName, version)
);

create table DLRepository (
	repositoryId varchar2(100) not null primary key,
	groupId varchar2(100) not null,
	companyId varchar2(100) not null,
	userId varchar2(100) not null,
	userName varchar2(100) null,
	createDate date null,
	modifiedDate date null,
	readRoles varchar2(100) null,
	writeRoles varchar2(100) null,
	name varchar2(100) null,
	description varchar2(4000) null,
	lastPostDate date null
);

create table IGFolder (
	folderId varchar2(100) not null primary key,
	groupId varchar2(100) not null,
	companyId varchar2(100) not null,
	userId varchar2(100) not null,
	createDate date null,
	modifiedDate date null,
	parentFolderId varchar2(100) null,
	name varchar2(100) null
);

create table IGImage (
	imageId varchar2(100) not null,
	companyId varchar2(100) not null,
	userId varchar2(100) not null,
	createDate date null,
	modifiedDate date null,
	folderId varchar2(100) null,
	description long varchar null,
	height number(30,0),
	width number(30,0),
	size_ number(30,0),
	primary key (imageId, companyId)
);

create table Image (
	imageId varchar2(200) not null primary key,
	text_ long varchar not null
);

create table JournalArticle (
	articleId varchar2(100) not null,
	version number(30,20) not null,
	portletId varchar2(100) not null,
	groupId varchar2(100) not null,
	companyId varchar2(100) not null,
	userId varchar2(100) not null,
	userName varchar2(100) null,
	createDate date null,
	modifiedDate date null,
	title varchar2(100) null,
	content long varchar null,
	type_ varchar2(100) null,
	structureId varchar2(100) null,
	templateId varchar2(100) null,
	displayDate date null,
	expirationDate date null,
	approved number(1, 0),
	approvedByUserId varchar2(100) null,
	approvedByUserName varchar2(100) null,
	primary key (articleId, version)
);

create table JournalStructure (
	structureId varchar2(100) not null primary key,
	portletId varchar2(100) not null,
	groupId varchar2(100) not null,
	companyId varchar2(100) not null,
	userId varchar2(100) not null,
	userName varchar2(100) null,
	createDate date null,
	modifiedDate date null,
	name varchar2(100) null,
	description varchar2(4000) null,
	xsd long varchar null
);

create table JournalTemplate (
	templateId varchar2(100) not null primary key,
	portletId varchar2(100) not null,
	groupId varchar2(100) not null,
	companyId varchar2(100) not null,
	userId varchar2(100) not null,
	userName varchar2(100) null,
	createDate date null,
	modifiedDate date null,
	structureId varchar2(100) null,
	name varchar2(100) null,
	description varchar2(4000) null,
	xsl long varchar null,
	smallImage number(1, 0),
	smallImageURL varchar2(100) null
);

create table Layer (
	layerId varchar2(100) not null,
	skinId varchar2(100) not null,
	href varchar2(100) null,
	hrefHover varchar2(100) null,
	background varchar2(100) null,
	foreground varchar2(100) null,
	negAlert varchar2(100) null,
	posAlert varchar2(100) null,
	primary key (layerId, skinId)
);

create table MailReceipt (
	receiptId varchar2(100) not null primary key,
	companyId varchar2(100) not null,
	userId varchar2(100) not null,
	createDate date null,
	modifiedDate date null,
	recipientName varchar2(100) null,
	recipientAddress varchar2(100) null,
	subject varchar2(100) null,
	sentDate date null,
	readCount number(30,0),
	firstReadDate date null,
	lastReadDate date null
);

create table MBMessage (
	messageId varchar2(100) not null,
	topicId varchar2(100) not null,
	companyId varchar2(100) not null,
	userId varchar2(100) not null,
	userName varchar2(100) null,
	createDate date null,
	modifiedDate date null,
	threadId varchar2(100) null,
	parentMessageId varchar2(100) null,
	subject varchar2(100) null,
	body long varchar null,
	attachments number(1, 0),
	anonymous number(1, 0),
	primary key (messageId, topicId)
);

create table MBMessageFlag (
	topicId varchar2(100) not null,
	messageId varchar2(100) not null,
	userId varchar2(100) not null,
	flag varchar2(100) null,
	primary key (topicId, messageId, userId)
);

create table MBThread (
	threadId varchar2(100) not null primary key,
	rootMessageId varchar2(100) null,
	topicId varchar2(100) null,
	messageCount number(30,0),
	lastPostDate date null
);

create table MBTopic (
	topicId varchar2(100) not null primary key,
	portletId varchar2(100) not null,
	groupId varchar2(100) not null,
	companyId varchar2(100) not null,
	userId varchar2(100) not null,
	userName varchar2(100) null,
	createDate date null,
	modifiedDate date null,
	readRoles varchar2(100) null,
	writeRoles varchar2(100) null,
	name varchar2(100) null,
	description long varchar null,
	lastPostDate date null
);

create table NetworkAddress (
	addressId varchar2(100) not null primary key,
	userId varchar2(100) not null,
	createDate date null,
	modifiedDate date null,
	name varchar2(100) null,
	url varchar2(100) null,
	comments varchar2(4000) null,
	content long varchar null,
	status number(30,0),
	lastUpdated date null,
	notifyBy varchar2(100) null,
	interval_ number(30,0),
	active_ number(1, 0)
);

create table Note (
	noteId varchar2(100) not null primary key,
	companyId varchar2(100) not null,
	userId varchar2(100) not null,
	userName varchar2(100) null,
	createDate date null,
	modifiedDate date null,
	className varchar2(100) null,
	classPK varchar2(100) null,
	content long varchar null
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

create table ProjFirm (
	firmId varchar2(100) not null primary key,
	companyId varchar2(100) not null,
	userId varchar2(100) not null,
	userName varchar2(100) null,
	createDate date null,
	modifiedDate date null,
	name varchar2(100) null,
	description varchar2(4000) null,
	url varchar2(100) null
);

create table ProjProject (
	projectId varchar2(100) not null primary key,
	companyId varchar2(100) not null,
	userId varchar2(100) not null,
	userName varchar2(100) null,
	createDate date null,
	modifiedDate date null,
	firmId varchar2(100) null,
	code varchar2(100) null,
	name varchar2(100) null,
	description varchar2(4000) null
);

create table ProjTask (
	taskId varchar2(100) not null primary key,
	companyId varchar2(100) not null,
	userId varchar2(100) not null,
	userName varchar2(100) null,
	createDate date null,
	modifiedDate date null,
	projectId varchar2(100) null,
	name varchar2(100) null,
	description varchar2(4000) null,
	comments long varchar null,
	estimatedDuration number(30,0),
	estimatedEndDate date null,
	actualDuration number(30,0),
	actualEndDate date null,
	status number(30,0)
);

create table ProjTime (
	timeId varchar2(100) not null primary key,
	companyId varchar2(100) not null,
	userId varchar2(100) not null,
	userName varchar2(100) null,
	createDate date null,
	modifiedDate date null,
	projectId varchar2(100) null,
	taskId varchar2(100) null,
	description varchar2(4000) null,
	startDate date null,
	endDate date null
);

create table Release_ (
	releaseId varchar2(100) not null primary key,
	createDate date null,
	modifiedDate date null,
	buildNumber number(30,0) null,
	buildDate date null
);

create table Skin (
	skinId varchar2(100) not null primary key,
	name varchar2(100) null,
	imageId varchar2(100) null,
	alphaLayerId varchar2(100) null,
	alphaSkinId varchar2(100) null,
	betaLayerId varchar2(100) null,
	betaSkinId varchar2(100) null,
	gammaLayerId varchar2(100) null,
	gammaSkinId varchar2(100) null,
	bgLayerId varchar2(100) null,
	bgSkinId varchar2(100) null
);

create table ShoppingCart (
	cartId varchar2(100) not null primary key,
	companyId varchar2(100) not null,
	userId varchar2(100) not null,
	createDate date null,
	modifiedDate date null,
	itemIds varchar2(4000) null,
	couponIds varchar2(4000) null,
	altShipping number(30,0)
);

create table ShoppingCategory (
	categoryId varchar2(100) not null primary key,
	companyId varchar2(100) not null,
	createDate date null,
	modifiedDate date null,
	parentCategoryId varchar2(100) null,
	name varchar2(100) null
);

create table ShoppingCoupon (
	couponId varchar2(100) not null primary key,
	companyId varchar2(100) not null,
	createDate date null,
	modifiedDate date null,
	name varchar2(100) null,
	description varchar2(4000) null,
	startDate date null,
	endDate date null,
	active_ number(1, 0),
	limitCategories varchar2(4000) null,
	limitSkus varchar2(4000) null,
	minOrder number(30,20),
	discount number(30,20),
	discountType varchar2(100) null
);

create table ShoppingItem (
	itemId varchar2(100) not null primary key,
	companyId varchar2(100) not null,
	createDate date null,
	modifiedDate date null,
	categoryId varchar2(100) null,
	sku varchar2(100) null,
	name varchar2(100) null,
	description varchar2(4000) null,
	properties varchar2(4000) null,
	supplierUserId varchar2(100) null,
	fields_ number(1, 0),
	fieldsQuantities varchar2(4000) null,
	minQuantity number(30,0),
	maxQuantity number(30,0),
	price number(30,20),
	discount number(30,20),
	taxable number(1, 0),
	shipping number(30,20),
	useShippingFormula number(1, 0),
	requiresShipping number(1, 0),
	stockQuantity number(30,0),
	featured_ number(1, 0),
	sale_ number(1, 0),
	smallImage number(1, 0),
	smallImageURL varchar2(100) null,
	mediumImage number(1, 0),
	mediumImageURL varchar2(100) null,
	largeImage number(1, 0),
	largeImageURL varchar2(100) null
);

create table ShoppingItemField (
	itemFieldId varchar2(100) not null primary key,
	itemId varchar2(100) null,
	name varchar2(100) null,
	values_ varchar2(4000) null,
	description varchar2(4000) null
);

create table ShoppingItemPrice (
	itemPriceId varchar2(100) not null primary key,
	itemId varchar2(100) null,
	minQuantity number(30,0),
	maxQuantity number(30,0),
	price number(30,20),
	discount number(30,20),
	taxable number(1, 0),
	shipping number(30,20),
	useShippingFormula number(1, 0),
	status number(30,0)
);

create table ShoppingOrder (
	orderId varchar2(100) not null primary key,
	companyId varchar2(100) not null,
	userId varchar2(100) not null,
	createDate date null,
	modifiedDate date null,
	tax number(30,20),
	shipping number(30,20),
	altShipping varchar2(100),
	requiresShipping number(1, 0),
	couponIds varchar2(4000) null,
	couponDiscount number(30,20),
	billingFirstName varchar2(100) null,
	billingLastName varchar2(100) null,
	billingEmailAddress varchar2(100) null,
	billingCompany varchar2(100) null,
	billingStreet varchar2(100) null,
	billingCity varchar2(100) null,
	billingState varchar2(100) null,
	billingZip varchar2(100) null,
	billingCountry varchar2(100) null,
	billingPhone varchar2(100) null,
	shipToBilling number(1, 0),
	shippingFirstName varchar2(100) null,
	shippingLastName varchar2(100) null,
	shippingEmailAddress varchar2(100) null,
	shippingCompany varchar2(100) null,
	shippingStreet varchar2(100) null,
	shippingCity varchar2(100) null,
	shippingState varchar2(100) null,
	shippingZip varchar2(100) null,
	shippingCountry varchar2(100) null,
	shippingPhone varchar2(100) null,
	ccName varchar2(100) null,
	ccType varchar2(100) null,
	ccNumber varchar2(100) null,
	ccExpMonth number(30,0),
	ccExpYear number(30,0),
	ccVerNumber varchar2(100) null,
	comments long varchar null,
	ppTxnId varchar2(100) null,
	ppPaymentStatus varchar2(100) null,
	ppPaymentGross number(30,20),
	ppReceiverEmail varchar2(100) null,
	ppPayerEmail varchar2(100) null,
	sendOrderEmail number(1, 0),
	sendShippingEmail number(1, 0)
);

create table ShoppingOrderItem (
	orderId varchar2(100) not null,
	itemId varchar2(100) not null,
	sku varchar2(100) null,
	name varchar2(100) null,
	description varchar2(4000) null,
	properties varchar2(4000) null,
	supplierUserId varchar2(100) null,
	price number(30,20),
	quantity number(30,0),
	shippedDate date null,
	primary key (orderId, itemId)
);

create table User_ (
	userId varchar2(100) not null primary key,
	companyId varchar2(100) not null,
	createDate date null,
	password_ varchar2(100) null,
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

create table Users_ProjProjects (
	userId varchar2(100) not null,
	projectId varchar2(100) not null
);

create table Users_ProjTasks (
	userId varchar2(100) not null,
	taskId varchar2(100) not null
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

create table WikiDisplay (
	layoutId varchar2(100) not null,
	userId varchar2(100) not null,
	portletId varchar2(100) not null,
	nodeId varchar2(100) not null,
	showBorders number(1, 0),
	primary key (layoutId, userId, portletId)
);

create table WikiNode (
	nodeId varchar2(100) not null primary key,
	companyId varchar2(100) not null,
	userId varchar2(100) not null,
	userName varchar2(100) null,
	createDate date null,
	modifiedDate date null,
	readRoles varchar2(100) null,
	writeRoles varchar2(100) null,
	name varchar2(100) null,
	description varchar2(4000) null,
	sharing number(1, 0),
	lastPostDate date null
);

create table WikiPage (
	nodeId varchar2(100) not null,
	title varchar2(100) not null,
	version number(30,20) not null,
	companyId varchar2(100) not null,
	userId varchar2(100) not null,
	userName varchar2(100) null,
	createDate date null,
	content long varchar null,
	format varchar2(100) null,
	head number(1, 0),
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

insert into Company (companyId, portalURL, homeURL, mx, name, shortName, type_, size_, emailAddress, authType, autoLogin, strangers) values ('liferay.com', 'localhost', 'localhost', 'liferay.com', 'Liferay, LLC', 'Liferay', 'biz', '', 'test@liferay.com', 'emailAddress', '1', '1');
update Company set street = '1220 Brea Canyon Rd.', city = 'Diamond Bar', state = 'CA', zip = '91789' where companyId = 'liferay.com';

insert into IGFolder (folderId, groupId, companyId, userId, createDate, modifiedDate, parentFolderId, name) values ('11','3','liferay.com','liferay.com.1',sysdate,sysdate,'-1','COMMON');
insert into IGFolder (folderId, groupId, companyId, userId, createDate, modifiedDate, parentFolderId, name) values ('12','3','liferay.com','liferay.com.1',sysdate,sysdate,'11','HEADER');
insert into IGFolder (folderId, groupId, companyId, userId, createDate, modifiedDate, parentFolderId, name) values ('13','3','liferay.com','liferay.com.1',sysdate,sysdate,'-1','HOME');
insert into IGFolder (folderId, groupId, companyId, userId, createDate, modifiedDate, parentFolderId, name) values ('14','3','liferay.com','liferay.com.1',sysdate,sysdate,'-1','COMPANY');
insert into IGFolder (folderId, groupId, companyId, userId, createDate, modifiedDate, parentFolderId, name) values ('15','3','liferay.com','liferay.com.1',sysdate,sysdate,'-1','PRODUCTS');
insert into IGFolder (folderId, groupId, companyId, userId, createDate, modifiedDate, parentFolderId, name) values ('16','3','liferay.com','liferay.com.1',sysdate,sysdate,'-1','SERVICES');
insert into IGFolder (folderId, groupId, companyId, userId, createDate, modifiedDate, parentFolderId, name) values ('17','3','liferay.com','liferay.com.1',sysdate,sysdate,'-1','DOCUMENTATION');
insert into IGFolder (folderId, groupId, companyId, userId, createDate, modifiedDate, parentFolderId, name) values ('18','3','liferay.com','liferay.com.1',sysdate,sysdate,'-1','NEWS');
insert into IGFolder (folderId, groupId, companyId, userId, createDate, modifiedDate, parentFolderId, name) values ('19','3','liferay.com','liferay.com.1',sysdate,sysdate,'-1','DOWNLOADS');

insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('1','liferay.com','liferay.com.1',sysdate,sysdate,'12','',49,199,4102);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('2','liferay.com','liferay.com.1',sysdate,sysdate,'12','',9,56,283);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('3','liferay.com','liferay.com.1',sysdate,sysdate,'12','',9,56,283);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('4','liferay.com','liferay.com.1',sysdate,sysdate,'12','',9,57,283);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('5','liferay.com','liferay.com.1',sysdate,sysdate,'12','',9,57,283);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('6','liferay.com','liferay.com.1',sysdate,sysdate,'12','',9,49,264);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('7','liferay.com','liferay.com.1',sysdate,sysdate,'12','',9,49,264);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('8','liferay.com','liferay.com.1',sysdate,sysdate,'12','',9,91,404);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('9','liferay.com','liferay.com.1',sysdate,sysdate,'12','',9,91,404);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('10','liferay.com','liferay.com.1',sysdate,sysdate,'12','',9,31,199);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('11','liferay.com','liferay.com.1',sysdate,sysdate,'12','',9,31,199);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('12','liferay.com','liferay.com.1',sysdate,sysdate,'12','',9,70,344);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('13','liferay.com','liferay.com.1',sysdate,sysdate,'12','',9,70,344);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('14','liferay.com','liferay.com.1',sysdate,sysdate,'11','',1,1,49);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('15','liferay.com','liferay.com.1',sysdate,sysdate,'11','',14,8,516);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('16','liferay.com','liferay.com.1',sysdate,sysdate,'11','',10,10,75);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('17','liferay.com','liferay.com.1',sysdate,sysdate,'11','',8,8,64);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('18','liferay.com','liferay.com.1',sysdate,sysdate,'11','',9,13,206);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('19','liferay.com','liferay.com.1',sysdate,sysdate,'11','',262,1,61);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('20','liferay.com','liferay.com.1',sysdate,sysdate,'11','',1,262,834);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('21','liferay.com','liferay.com.1',sysdate,sysdate,'13','',15,80,533);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('22','liferay.com','liferay.com.1',sysdate,sysdate,'13','',15,62,442);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('23','liferay.com','liferay.com.1',sysdate,sysdate,'13','',94,127,6388);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('24','liferay.com','liferay.com.1',sysdate,sysdate,'13','',94,127,4866);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('25','liferay.com','liferay.com.1',sysdate,sysdate,'13','',10,70,426);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('26','liferay.com','liferay.com.1',sysdate,sysdate,'13','',25,90,1755);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('27','liferay.com','liferay.com.1',sysdate,sysdate,'13','',11,37,293);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('28','liferay.com','liferay.com.1',sysdate,sysdate,'13','',11,64,441);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('29','liferay.com','liferay.com.1',sysdate,sysdate,'13','',14,14,138);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('30','liferay.com','liferay.com.1',sysdate,sysdate,'13','',14,105,442);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('31','liferay.com','liferay.com.1',sysdate,sysdate,'14','company.jpg',100,550,51917);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('35','liferay.com','liferay.com.1',sysdate,sysdate,'16','services.jpg',100,550,33075);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('33','liferay.com','liferay.com.1',sysdate,sysdate,'15','products.jpg',100,550,36698);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('34','liferay.com','liferay.com.1',sysdate,sysdate,'15','products_title.gif',22,106,652);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('32','liferay.com','liferay.com.1',sysdate,sysdate,'14','company_title.gif',22,106,658);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('36','liferay.com','liferay.com.1',sysdate,sysdate,'16','services_title.gif',22,84,598);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('37','liferay.com','liferay.com.1',sysdate,sysdate,'17','documentation.jpg',100,550,33688);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('38','liferay.com','liferay.com.1',sysdate,sysdate,'17','documentation_title.gif',22,162,932);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('39','liferay.com','liferay.com.1',sysdate,sysdate,'18','news.jpg',100,550,27641);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('40','liferay.com','liferay.com.1',sysdate,sysdate,'18','news_title.gif',22,55,463);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('41','liferay.com','liferay.com.1',sysdate,sysdate,'19','downloads.jpg',100,550,33579);
insert into IGImage (imageId, companyId, userId, createDate, modifiedDate, folderId, description, height, width, size_) values ('42','liferay.com','liferay.com.1',sysdate,sysdate,'19','downloads_title.gif',22,124,786);

insert into ShoppingCategory (categoryId, companyId, createDate, modifiedDate, parentCategoryId, name) values ('1', 'liferay.com', sysdate, sysdate, '-1', 'Books');
insert into ShoppingCategory (categoryId, companyId, createDate, modifiedDate, parentCategoryId, name) values ('2', 'liferay.com', sysdate, sysdate, '1', 'Art');
insert into ShoppingCategory (categoryId, companyId, createDate, modifiedDate, parentCategoryId, name) values ('3', 'liferay.com', sysdate, sysdate, '1', 'Christian');
insert into ShoppingCategory (categoryId, companyId, createDate, modifiedDate, parentCategoryId, name) values ('4', 'liferay.com', sysdate, sysdate, '1', 'Computer Science');
insert into ShoppingCategory (categoryId, companyId, createDate, modifiedDate, parentCategoryId, name) values ('5', 'liferay.com', sysdate, sysdate, '1', 'Economics');
insert into ShoppingCategory (categoryId, companyId, createDate, modifiedDate, parentCategoryId, name) values ('6', 'liferay.com', sysdate, sysdate, '1', 'Physics');


insert into PollsDisplay (layoutId, userId, portletId, questionId) values ('1.1', 'group.1', '59', '1');
insert into PollsChoice (choiceId, questionId, description) values ('a', '1', 'Chocolate');
insert into PollsChoice (choiceId, questionId, description) values ('b', '1', 'Strawberry');
insert into PollsChoice (choiceId, questionId, description) values ('c', '1', 'Vanilla');
insert into PollsQuestion (questionId, portletId, groupId, companyId, userId, userName, createDate, modifiedDate, title, description) values ('1', '25', '-1', 'liferay.com', 'liferay.com.1', 'John Wayne', sysdate, sysdate, 'What is your favorite ice cream flavor?', 'What is your favorite ice cream flavor?');

insert into WikiDisplay (layoutId, userId, portletId, nodeId, showBorders) values ('1.1', 'group.1', '54', '1', '1');
insert into WikiNode (nodeId, companyId, userId, userName, createDate, modifiedDate, readRoles, writeRoles, name, description, sharing, lastPostDate) values ('1', 'liferay.com', 'liferay.com.1', 'John Wayne', sysdate, sysdate, 'User,' ,'User,', 'Welcome', '', '1', sysdate);
insert into WikiPage (nodeId, title, version, companyId, userId, userName, createDate, content, format, head) values ('1', 'FrontPage', 1.0, 'liferay.com', 'liferay.com.1', 'John Wayne', sysdate, '<font class="bg" size="2">Welcome! Thank you for your interest in Liferay Enterprise Portal.<br><br>Your login is <b>test@liferay.com</b> and your password is <b>test</b>. The test user has the Administrator role.<br><br>To use the <b>Mail</b> portlet, make sure there is a mail server running on <i>localhost</i> accessible through the IMAP protocol.<br><br>The mail server must have an account with <b>liferay.com.1</b> as the user and <b>test</b> as the password.<br><br><hr><br>Is Liferay useful for your company? Tell us about it by email at <b>staff@liferay.com</b>.<br><br>We hope you enjoy our product!</font>', 'html', '1');

--
-- Default User
--

insert into User_ (userId, companyId, createDate, password_, passwordEncrypted, passwordReset, firstName, middleName, lastName, male, birthday, emailAddress, skinId, dottedSkins, roundedSkins, greeting, layoutIds, loginDate, failedLoginAttempts, agreedToTermsOfUse, active_) values ('liferay.com.default', 'default', sysdate, 'password', '0', '0', '', '', '', '1', to_date('1970-01-01 00:00:00','YYYY-MM-DD HH24:MI:SS'), 'default@liferay.com', '01', '0', '0', 'Welcome!', '', sysdate, 0, '0', '1');

--
-- Test User
--

insert into User_ (userId, companyId, createDate, password_, passwordEncrypted, passwordReset, firstName, middleName, lastName, nickName, male, birthday, emailAddress, skinId, dottedSkins, roundedSkins, greeting, layoutIds, loginDate, failedLoginAttempts, agreedToTermsOfUse, active_) values ('liferay.com.1', 'liferay.com', sysdate, 'test', '0', '0', 'John', '', 'Wayne', 'Duke', '1', to_date('1970-01-01 00:00:00','YYYY-MM-DD HH24:MI:SS'), 'test@liferay.com', '01', '0', '1', 'Welcome John Wayne!', '1,', sysdate, 0, '1', '1');
