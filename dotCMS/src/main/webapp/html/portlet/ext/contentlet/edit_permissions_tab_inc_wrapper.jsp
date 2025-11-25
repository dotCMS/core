<%--
This jsp gets called in the edit content screen (edit_content_js_inc.jsp) to paint the permission tab via ajax. Pass in a 

--%>
<%@page import="com.dotmarketing.business.PermissionLevel"%>
<%@ include file="/html/portlet/ext/contentlet/init.jsp"%>
<%@page import="com.dotmarketing.db.DbConnectionFactory"%>

<% String id = request.getParameter("contentletId");%>
<% Long lang = Long.parseLong(request.getParameter("languageId"));%>

<% if(UtilMethods.isSet(id)){%>
	<% Contentlet contentlet = APILocator.getContentletAPI().findContentletByIdentifier(id, false, lang, user, false);%>
	<%if(contentlet !=null && UtilMethods.isSet(contentlet.getIdentifier())){ %>
		<% request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT, contentlet);%>
		<% request.setAttribute(com.dotmarketing.util.WebKeys.CONTENTLET_EDIT, contentlet);%>
		<% APILocator.getPermissionAPI().checkPermission(contentlet, PermissionLevel.EDIT_PERMISSIONS, user);%>
		
		<%@ include file="/html/portlet/ext/common/edit_permissions_tab_inc.jsp" %>

    <%} %>
<%}%>