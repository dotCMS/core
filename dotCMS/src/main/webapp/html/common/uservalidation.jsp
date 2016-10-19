<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="com.dotmarketing.exception.DotSecurityException"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>

<%
	try { //EXT_CMS_MAINTENANCE
		String requiredPortletAccess = (String) request.getAttribute("requiredPortletAccess");
		requiredPortletAccess = UtilMethods.isSet(requiredPortletAccess)?requiredPortletAccess:"EXT_CMS_MAINTENANCE";

		User userToCheck = com.liferay.portal.util.PortalUtil.getUser(request);
		if(userToCheck == null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet(requiredPortletAccess, userToCheck)){
			throw new DotSecurityException("Invalid user accessing JSP - is user '" + userToCheck + "' logged in?");
		}
	} catch (Exception e) {
		Logger.error(this.getClass(), e.getMessage());
		response.sendError(403);
		return;
	}
 %>