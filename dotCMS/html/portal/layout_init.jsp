<%@page import="com.dotmarketing.business.Role"%><%@page import="com.dotmarketing.business.APILocator"%><%@ include file="/html/portal/init.jsp"%><%
	Portlet[] portlets = null;
	
	List userRoles = APILocator.getRoleAPI().loadRolesForUser(user.getUserId());


%>