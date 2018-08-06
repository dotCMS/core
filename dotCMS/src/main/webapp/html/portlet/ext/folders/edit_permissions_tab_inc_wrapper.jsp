<%@ include file="/html/common/init.jsp" %>
<%@page import="com.dotmarketing.business.PermissionLevel"%>

<%@page import="com.dotmarketing.db.DbConnectionFactory"%>


<% String folderId = request.getParameter("folderId");%>

<% if(UtilMethods.isSet(folderId)){%>
	<%Folder folder = APILocator.getFolderAPI().find(folderId, user, false); %>
	<%request.setAttribute(com.dotmarketing.util.WebKeys.FOLDER_EDIT, folder); %>
	<% request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT, folder); %>
	
	<%@ include file="/html/portlet/ext/common/edit_permissions_tab_inc.jsp" %>
<%}%>