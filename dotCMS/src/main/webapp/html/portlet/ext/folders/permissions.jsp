<%@ page import="com.dotmarketing.portlets.folders.model.Folder" %>
<%@ page import="com.dotmarketing.business.APILocator" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ include file="/html/common/init.jsp" %>
<%@ include file="/html/common/top_inc.jsp" %>
<%@ include file="/html/common/messages_inc.jsp" %>

<%
    final String folderIdentifier = request.getParameter("folderIdentifier");
    if (UtilMethods.isSet(folderIdentifier)) {
        final Folder folder = APILocator.getFolderAPI().find(folderIdentifier, user, false);
        if (folder != null && UtilMethods.isSet(folder.getInode())) {
            request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT, folder);
%>
<%@ include file="/html/portlet/ext/common/edit_permissions_tab_inc.jsp" %>
<%
        }
    }
%>
