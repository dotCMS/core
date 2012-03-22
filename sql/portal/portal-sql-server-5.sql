
insert into Group_ (groupId, companyId, parentGroupId, name, layoutIds) values ('1', 'liferay.com', '-1', 'General Guest', '1.1,1.2,');
insert into Group_ (groupId, companyId, parentGroupId, name, layoutIds) values ('2', 'liferay.com', '-1', 'General User', '');
insert into Group_ (groupId, companyId, parentGroupId, name, layoutIds) values ('3', 'liferay.com', '-1', 'CMS', '3.1,');

insert into Role_ (roleId, companyId, name) values ('1', 'liferay.com', 'Administrator');
insert into Role_ (roleId, companyId, name) values ('2', 'liferay.com', 'Bookmarks Admin');
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

insert into PollsDisplay (layoutId, userId, portletId, questionId) values ('1.1', 'group.1', '59', '1');
insert into PollsChoice (choiceId, questionId, description) values ('a', '1', 'Chocolate');
insert into PollsChoice (choiceId, questionId, description) values ('b', '1', 'Strawberry');
insert into PollsChoice (choiceId, questionId, description) values ('c', '1', 'Vanilla');
insert into PollsQuestion (questionId, portletId, groupId, companyId, userId, userName, createDate, modifiedDate, title, description) values ('1', '25', '-1', 'liferay.com', 'liferay.com.1', 'John Wayne', GetDate(), GetDate(), 'What is your favorite ice cream flavor?', 'What is your favorite ice cream flavor?');

insert into WikiDisplay (layoutId, userId, portletId, nodeId, showBorders) values ('1.1', 'group.1', '54', '1', '1');
insert into WikiNode (nodeId, companyId, userId, userName, createDate, modifiedDate, readRoles, writeRoles, name, description, sharing, lastPostDate) values ('1', 'liferay.com', 'liferay.com.1', 'John Wayne', GetDate(), GetDate(), 'User,' ,'User,', 'Welcome', '', '1', GetDate());
insert into WikiPage (nodeId, title, version, companyId, userId, userName, createDate, content, format, head) values ('1', 'FrontPage', 1.0, 'liferay.com', 'liferay.com.1', 'John Wayne', GetDate(), '<font class="bg" size="2">Welcome! Thank you for your interest in Liferay Enterprise Portal.<br><br>Your login is <b>test@liferay.com</b> and your password is <b>test</b>. The test user has the Administrator role.<br><br>To use the <b>Mail</b> portlet, make sure there is a mail server running on <i>localhost</i> accessible through the IMAP protocol.<br><br>The mail server must have an account with <b>liferay.com.1</b> as the user and <b>test</b> as the password.<br><br><hr><br>Is Liferay useful for your company? Tell us about it by email at <b>staff@liferay.com</b>.<br><br>We hope you enjoy our product!</font>', 'html', '1');

--
-- Default User
--

insert into User_ (userId, companyId, createDate, password_, passwordEncrypted, passwordReset, firstName, middleName, lastName, male, birthday, emailAddress, skinId, dottedSkins, roundedSkins, greeting, layoutIds, loginDate, failedLoginAttempts, agreedToTermsOfUse, active_) values ('liferay.com.default', 'default', GetDate(), 'password', '0', '0', '', '', '', '1', '19700101', 'default@liferay.com', '01', '0', '0', 'Welcome!', '', GetDate(), 0, '0', '1');
insert into Layout (layoutId, userId, name, columnOrder, narrow1, narrow2, wide) values ('1.1', 'group.1', 'Welcome', 'n1,w,', '59,', '', '2,54,');
insert into Layout (layoutId, userId, name, columnOrder, narrow1, narrow2, wide, stateMax) values ('1.2', 'group.1', 'Shopping', 'w,', '', '', '34,', '34,');
insert into Layout (layoutId, userId, name, columnOrder, narrow1, narrow2, wide, stateMax) values ('3.1', 'group.3', 'CMS', 'n1,w,', '', '', '15,31,', '');

--
-- Test User
--

insert into User_ (userId, companyId, createDate, password_, passwordEncrypted, passwordReset, firstName, middleName, lastName, nickName, male, birthday, emailAddress, skinId, dottedSkins, roundedSkins, greeting, layoutIds, loginDate, failedLoginAttempts, agreedToTermsOfUse, active_) values ('liferay.com.1', 'liferay.com', GetDate(), 'test', '0', '0', 'John', '', 'Wayne', 'Duke', '1', '19700101', 'test@liferay.com', '01', '0', '1', 'Welcome John Wayne!', '1,', GetDate(), 0, '1', '1');
insert into Layout (layoutId, userId, name, columnOrder, narrow1, narrow2, wide) values ('1', 'liferay.com.1', 'Home', 'n1,w,', '9,36,4,3,', '', '5,8,');
insert into Users_Groups values ('liferay.com.1', '1');
insert into Users_Groups values ('liferay.com.1', '3');
insert into Users_Roles values ('liferay.com.1', '1');
insert into Users_Roles values ('liferay.com.1', '12');
insert into Users_Roles values ('liferay.com.1', '15');