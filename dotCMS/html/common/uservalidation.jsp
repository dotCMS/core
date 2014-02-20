<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="com.dotmarketing.exception.DotSecurityException"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotmarketing.business.APILocator"%>

<%
	try {
		User userToCheck = com.liferay.portal.util.PortalUtil.getUser(request);
		if(userToCheck == null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("EXT_CMS_MAINTENANCE", userToCheck)){
			throw new DotSecurityException("Invalid user accessing JSP - is user '" + userToCheck + "' logged in?");
		}
	} catch (Exception e) {
		Logger.error(this.getClass(), e.getMessage());
		response.sendError(403);
		return;
	}
 %>