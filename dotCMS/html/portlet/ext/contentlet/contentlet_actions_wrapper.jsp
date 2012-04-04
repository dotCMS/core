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
PermissionAPI conPerAPI = APILocator.getPermissionAPI();
boolean canUserPublishContentlet = conPerAPI.doesUserHavePermission(contentlet,PermissionAPI.PERMISSION_PUBLISH,user);


String copyOptions = ((String) request.getParameter("copyOptions"))==null?"":(String) request.getParameter("copyOptions");

Structure structure = contentlet.getStructure();
String userLocked =null;
String lockedSince = null;
boolean contentEditable = false;
if (contentlet.isLocked()) {
	String lockedUserId =  APILocator.getVersionableAPI().getLockedBy(contentlet.getIdentifier());
	contentEditable = user.getUserId().equals(lockedUserId);
	
	userLocked = APILocator.getUserAPI().loadUserById(lockedUserId, APILocator.getUserAPI().getSystemUser(), true).getFullName();
	lockedSince = UtilMethods.capitalize(DateUtil.prettyDateSince(APILocator.getVersionableAPI().getLockedOn(contentlet.getIdentifier()), user.getLocale()));
}

%>






<%@ include file="/html/portlet/ext/contentlet/contentlet_actions_inc.jsp"%>

<script>





	toggleLockedMessage(<%=contentlet.isLocked()%>, "<%=userLocked%>", "<%=lockedSince%>");

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
request.getSession().removeAttribute("org.apache.struts.action.MESSAGE");
request.getSession().removeAttribute("org.apache.struts.action.ERROR");
%>

