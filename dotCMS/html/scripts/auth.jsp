<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="java.util.StringTokenizer"%>
<%@page import="com.liferay.portal.model.User"%>
<%
if(!com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(com.liferay.portal.util.PortalUtil.getUser(request),com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole())){
	response.sendError(401);
	return;
}
%>