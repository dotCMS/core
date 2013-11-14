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
	hoursOfOperation longvarchar null,
	birthday timestamp null,
	timeZoneId varchar(100) null,
	instantMessenger varchar(100) null,
	website varchar(100) null,
	comments longvarchar null
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
	priority int
);

create table AdminConfig (
	configId varchar(100) not null primary key,
	companyId varchar(100) not null,
	type_ varchar(100) null,
	name varchar(100) null,
	config longvarchar null
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
	content longvarchar null,
	versesInput longvarchar null
);

create table BJTopic (
	topicId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	createDate timestamp null,
	modifiedDate timestamp null,
	name varchar(100) null,
	description longvarchar null
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
	content longvarchar null
);

create table BlogsEntry (
	entryId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	createDate timestamp null,
	modifiedDate timestamp null,
	categoryId varchar(100) null,
	title varchar(100) null,
	content longvarchar null,
	displayDate timestamp null,
	sharing bit,
	commentable bit,
	propsCount int,
	commentsCount int
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
	quantity int
);

create table BlogsReferer (
	entryId varchar(100) not null,
	url varchar(100) not null,
	type_ varchar(100) not null,
	quantity int,
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
	comments longvarchar null,
	visits int
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
	description longvarchar null,
	startDate timestamp null,
	endDate timestamp null,
	durationHour int,
	durationMinute int,
	allDay bit,
	timeZoneSensitive bit,
	type_ varchar(100) null,
	location varchar(100) null,
	street varchar(100) null,
	city varchar(100) null,
	state varchar(100) null,
	zip varchar(100) null,
	phone varchar(100) null,
	repeating bit,
	recurrence longvarchar null,
	remindBy varchar(100) null,
	firstReminder int,
	secondReminder int
);

create table CalTask (
	taskId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	createDate timestamp null,
	modifiedDate timestamp null,
	title varchar(100) null,
	description longvarchar null,
	noDueDate bit,
	dueDate timestamp null,
	priority int,
	status int
);

create table Company (
	companyId varchar(100) not null primary key,
	key_ longvarchar null,
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
	description longvarchar null,
	version double,
	size_ int,
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
	version double not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate timestamp null,
	size_ int,
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
	description longvarchar null,
	lastPostDate timestamp null
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
	description longvarchar null,
	height int,
	width int,
	size_ int,
	primary key (imageId, companyId)
);

create table Image (
	imageId varchar(200) not null primary key,
	text_ longvarchar not null
);

create table JournalArticle (
	articleId varchar(100) not null,
	version double not null,
	portletId varchar(100) not null,
	groupId varchar(100) not null,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate timestamp null,
	modifiedDate timestamp null,
	title varchar(100) null,
	content longvarchar null,
	type_ varchar(100) null,
	structureId varchar(100) null,
	templateId varchar(100) null,
	displayDate timestamp null,
	expirationDate timestamp null,
	approved bit,
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
	description longvarchar null,
	xsd longvarchar null
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
	description longvarchar null,
	xsl longvarchar null,
	smallImage bit,
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
	narrow1 longvarchar null,
	narrow2 longvarchar null,
	wide longvarchar null,
	stateMax longvarchar null,
	stateMin longvarchar null,
	modeEdit longvarchar null,
	modeHelp longvarchar null,
	primary key (layoutId, userId)
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
	readCount int,
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
	body longvarchar null,
	attachments bit,
	anonymous bit,
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
	messageCount int,
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
	description longvarchar null,
	lastPostDate timestamp null
);

create table NetworkAddress (
	addressId varchar(100) not null primary key,
	userId varchar(100) not null,
	createDate timestamp null,
	modifiedDate timestamp null,
	name varchar(100) null,
	url varchar(100) null,
	comments longvarchar null,
	content longvarchar null,
	status int,
	lastUpdated timestamp null,
	notifyBy varchar(100) null,
	interval_ int,
	active_ bit
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
	content longvarchar null
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
	description longvarchar null,
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
	description longvarchar null,
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
	defaultPreferences longvarchar null,
	narrow bit,
	roles longvarchar null,
	active_ bit,
	primary key (portletId, groupId, companyId)
);

create table PortletPreferences (
	portletId varchar(100) not null,
	userId varchar(100) not null,
	layoutId varchar(100) not null,
	preferences longvarchar null,
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
	description longvarchar null,
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
	description longvarchar null
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
	description longvarchar null,
	comments longvarchar null,
	estimatedDuration int,
	estimatedEndDate timestamp null,
	actualDuration int,
	actualEndDate timestamp null,
	status int
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
	description longvarchar null,
	startDate timestamp null,
	endDate timestamp null
);

create table Release_ (
	releaseId varchar(100) not null primary key,
	createDate timestamp null,
	modifiedDate timestamp null,
	buildNumber int null,
	buildDate timestamp null
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
	createDate timestamp null,
	modifiedDate timestamp null,
	itemIds longvarchar null,
	couponIds longvarchar null,
	altShipping int
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
	description longvarchar null,
	startDate timestamp null,
	endDate timestamp null,
	active_ bit,
	limitCategories longvarchar null,
	limitSkus longvarchar null,
	minOrder double,
	discount double,
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
	description longvarchar null,
	properties longvarchar null,
	supplierUserId varchar(100) null,
	fields_ bit,
	fieldsQuantities longvarchar null,
	minQuantity int,
	maxQuantity int,
	price double,
	discount double,
	taxable bit,
	shipping double,
	useShippingFormula bit,
	requiresShipping bit,
	stockQuantity int,
	featured_ bit,
	sale_ bit,
	smallImage bit,
	smallImageURL varchar(100) null,
	mediumImage bit,
	mediumImageURL varchar(100) null,
	largeImage bit,
	largeImageURL varchar(100) null
);

create table ShoppingItemField (
	itemFieldId varchar(100) not null primary key,
	itemId varchar(100) null,
	name varchar(100) null,
	values_ longvarchar null,
	description longvarchar null
);

create table ShoppingItemPrice (
	itemPriceId varchar(100) not null primary key,
	itemId varchar(100) null,
	minQuantity int,
	maxQuantity int,
	price double,
	discount double,
	taxable bit,
	shipping double,
	useShippingFormula bit,
	status int
);

create table ShoppingOrder (
	orderId varchar(100) not null primary key,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	createDate timestamp null,
	modifiedDate timestamp null,
	tax double,
	shipping double,
	altShipping varchar(100),
	requiresShipping bit,
	couponIds longvarchar null,
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
	shipToBilling bit,
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
	ccExpMonth int,
	ccExpYear int,
	ccVerNumber varchar(100) null,
	comments longvarchar null,
	ppTxnId varchar(100) null,
	ppPaymentStatus varchar(100) null,
	ppPaymentGross double,
	ppReceiverEmail varchar(100) null,
	ppPayerEmail varchar(100) null,
	sendOrderEmail bit,
	sendShippingEmail bit
);

create table ShoppingOrderItem (
	orderId varchar(100) not null,
	itemId varchar(100) not null,
	sku varchar(100) null,
	name varchar(100) null,
	description longvarchar null,
	properties longvarchar null,
	supplierUserId varchar(100) null,
	price double,
	quantity int,
	shippedDate timestamp null,
	primary key (orderId, itemId)
);

create table User_ (
	userId varchar(100) not null primary key,
	companyId varchar(100) not null,
	createDate timestamp null,
	password_ varchar(100) null,
	passwordEncrypted bit,
	passwordExpirationDate timestamp null,
	passwordReset bit,
	firstName varchar(100) null,
	middleName varchar(100) null,
	lastName varchar(100) null,
	nickName varchar(100) null,
	male bit,
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
	dottedSkins bit,
	roundedSkins bit,
	greeting varchar(100) null,
	resolution varchar(100) null,
	refreshRate varchar(100) null,
	layoutIds varchar(100) null,
	comments longvarchar null,
	loginDate timestamp null,
	loginIP varchar(100) null,
	lastLoginDate timestamp null,
	lastLoginIP varchar(100) null,
	failedLoginAttempts int,
	agreedToTermsOfUse bit,
	active_ bit
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
	modifiedDate timestamp null,
	remoteAddr varchar(100) null,
	remoteHost varchar(100) null,
	userAgent varchar(100) null
);

create table UserTrackerPath (
	userTrackerPathId varchar(100) not null primary key,
	userTrackerId varchar(100) not null,
	path longvarchar not null,
	pathDate timestamp not null
);

create table WikiDisplay (
	layoutId varchar(100) not null,
	userId varchar(100) not null,
	portletId varchar(100) not null,
	nodeId varchar(100) not null,
	showBorders bit,
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
	description longvarchar null,
	sharing bit,
	lastPostDate timestamp null
);

create table WikiPage (
	nodeId varchar(100) not null,
	title varchar(100) not null,
	version double not null,
	companyId varchar(100) not null,
	userId varchar(100) not null,
	userName varchar(100) null,
	createDate timestamp null,
	content longvarchar null,
	format varchar(100) null,
	head bit,
	primary key (nodeId, title, version)
);

