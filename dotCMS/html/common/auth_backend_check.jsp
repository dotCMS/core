<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%
boolean onLoginPage = request.getRequestURI().trim().equalsIgnoreCase("/c/portal_public/login") || request.getRequestURI().trim().equalsIgnoreCase("c/portal_public/login") || request.getRequestURI().trim().equalsIgnoreCase("html/portal/login.jsp") || request.getRequestURI().trim().equalsIgnoreCase("/html/portal/login.jsp"); 
if(!onLoginPage && !WebAPILocator.getUserWebAPI().isLoggedToBackend(request)){
	response.sendError(401);
	return;	
}
%>