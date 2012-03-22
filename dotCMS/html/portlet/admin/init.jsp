<%
/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
%>

<%@ include file="/html/common/init.jsp" %>

<%@ page import="com.liferay.portal.AddressCellException" %>
<%@ page import="com.liferay.portal.AddressCityException" %>
<%@ page import="com.liferay.portal.AddressCountryException" %>
<%@ page import="com.liferay.portal.AddressDescriptionException" %>
<%@ page import="com.liferay.portal.AddressFaxException" %>
<%@ page import="com.liferay.portal.AddressPhoneException" %>
<%@ page import="com.liferay.portal.AddressStateException" %>
<%@ page import="com.liferay.portal.AddressStreetException" %>
<%@ page import="com.liferay.portal.AddressZipException" %>
<%@ page import="com.liferay.portal.DuplicateUserEmailAddressException" %>
<%@ page import="com.liferay.portal.DuplicateUserIdException" %>
<%@ page import="com.liferay.portal.NoSuchPortletException" %>
<%@ page import="com.liferay.portal.NoSuchRoleException" %>
<%@ page import="com.liferay.portal.NoSuchUserException" %>
<%@ page import="com.liferay.portal.PortletActiveException" %>
<%@ page import="com.liferay.portal.PortletDefaultPreferencesException" %>
<%@ page import="com.liferay.portal.RequiredRoleException" %>
<%@ page import="com.liferay.portal.RequiredUserException" %>
<%@ page import="com.liferay.portal.ReservedUserEmailAddressException" %>
<%@ page import="com.liferay.portal.ReservedUserIdException" %>
<%@ page import="com.liferay.portal.UserEmailAddressException" %>
<%@ page import="com.liferay.portal.UserFirstNameException" %>
<%@ page import="com.liferay.portal.UserIdException" %>
<%@ page import="com.liferay.portal.UserLastNameException" %>
<%@ page import="com.liferay.portal.UserPasswordException" %>
<%@ page import="com.liferay.portal.UserSmsException" %>
<%@ page import="com.liferay.portal.events.StartupAction" %>
<%@ page import="com.liferay.portal.servlet.PortalSessionContext" %>
<%@ page import="com.liferay.portal.util.UserTrackerModifiedDateComparator" %>
<%@ page import="com.liferay.portlet.admin.action.CreateUserAction" %>
<%@ page import="com.liferay.portlet.admin.action.DeleteUserAction" %>
<%@ page import="com.liferay.portlet.admin.action.UpdateCompanyAction" %>
<%@ page import="com.liferay.portlet.admin.action.UpdateUserConfigAction" %>
<%@ page import="com.liferay.portlet.admin.action.UploadLogoAction" %>
<%@ page import="com.liferay.portlet.myaccount.action.UploadPortraitAction" %>

<%@ page import="org.apache.log4j.Level" %>
<%@ page import="org.apache.log4j.Logger" %>
<%@ page import="org.apache.log4j.LogManager" %>

<portlet:defineObjects />

