<%@ include file="/html/common/init.jsp"%><%@ page
	import="com.liferay.portal.NoSuchUserException"%><%@ page
	import="com.liferay.portal.PortletActiveException"%><%@ page
	import="com.liferay.portal.RequiredLayoutException"%><%@ page
	import="com.liferay.portal.RequiredRoleException"%><%@ page
	import="com.liferay.portal.SendPasswordException"%><%@ page
	import="com.liferay.portal.UserActiveException"%><%@ page
	import="com.liferay.portal.UserEmailAddressException"%><%@ page
	import="com.liferay.portal.UserPasswordException"%><%@ page
	import="com.liferay.portal.auth.AuthException"%><%@ page
	import="com.liferay.portal.struts.PortletRequestProcessor"%><%@ page
	import="com.liferay.portal.util.PortletTitleComparator"%><%@ page
	import="javax.servlet.jsp.PageContext"%><%@ page
	import="org.apache.commons.fileupload.LiferayDiskFileUpload"%><%@ page import="org.apache.struts.action.ActionMapping"%><%@ page
	import="org.apache.struts.tiles.ComponentDefinition"%><%@ page
	import="org.apache.struts.tiles.TilesUtil"%><%@ page
	import="com.liferay.portal.UserActiveException"%>