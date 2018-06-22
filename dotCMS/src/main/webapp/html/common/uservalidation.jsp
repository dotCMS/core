<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="com.dotmarketing.exception.DotSecurityException"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>

<%
	try { //maintenance
		final String requiredPortletAccess = UtilMethods.isSet(request.getAttribute("requiredPortletAccess"))
		        ? (String) request.getAttribute("requiredPortletAccess")
		                : (UtilMethods.isSet(request.getAttribute("PORTLET_ID")))
		                ? (String) request.getAttribute("PORTLET_ID") 
		                        : "maintenance";

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