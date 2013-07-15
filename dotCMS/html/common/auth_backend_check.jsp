<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%
if(!WebAPILocator.getUserWebAPI().isLoggedToBackend(request)){
	response.sendError(401);
	return;	
}
%>