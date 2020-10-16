<%--

This little jsp wraps the contentletActions .jsp so they can be refreshed via ajax.


--%>
<%@page import="com.dotmarketing.util.DateUtil"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.util.InodeUtils"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@ include file="/html/common/init.jsp" %>
<%

String x = request.getParameter("contentletInode");
if(!UtilMethods.isSet(x)){
	x = (String) request.getAttribute("contentletInode");
}


Contentlet contentlet = APILocator.getContentletAPI().find(x, user, false);
if(null==contentlet){
    return;
}
PermissionAPI conPerAPI = APILocator.getPermissionAPI();
boolean canUserPublishContentlet = conPerAPI.doesUserHavePermission(contentlet,PermissionAPI.PERMISSION_PUBLISH,user);

boolean isLocked=(request.getParameter("sibbling") != null) ? false : contentlet.isLocked();

String copyOptions = ((String) request.getParameter("copyOptions"))==null?"":(String) request.getParameter("copyOptions");

Structure structure = contentlet.getStructure();
String userLocked =null;
String lockedSince = null;
boolean contentEditable = false;
if (isLocked) {
	Optional<String> lockedUserId =  APILocator.getVersionableAPI().getLockedBy(contentlet);

	if(lockedUserId.isPresent()) {
		contentEditable = user.getUserId().equals(lockedUserId.get());

		userLocked = APILocator.getUserAPI()
				.loadUserById(lockedUserId.get(), APILocator.getUserAPI().getSystemUser(), true)
				.getFullName();

		Optional<Date> lockedSinceOpt = APILocator.getVersionableAPI().getLockedOn(contentlet);

		if(lockedSinceOpt.isPresent()) {
			lockedSince = UtilMethods.capitalize(
					DateUtil.prettyDateSince(lockedSinceOpt.get(),
							user.getLocale()));
		}
	}
}



%>






<%@ include file="/html/portlet/ext/contentlet/contentlet_actions_inc.jsp"%>

<script>





	toggleLockedMessage(<%=isLocked%>, "<%=userLocked%>", "<%=lockedSince%>");

	<%if(SessionMessages.contains(session, "message")){%>
		showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext,  (String) SessionMessages.get(session, "message"))%>", false);
	<%}%>
	<%if(SessionMessages.contains(session, "error")){%>
		showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext,  (String) SessionMessages.get(session, "error"))%>", false);
	<%}%>


	createLockedWarningDiv();

</script>

<%
SessionMessages.clear(session);
SessionMessages.clear(request);
request.getSession().removeAttribute("com.dotcms.repackage.org.apache.struts.action.MESSAGE");
request.getSession().removeAttribute("com.dotcms.repackage.org.apache.struts.action.ERROR");
%>

