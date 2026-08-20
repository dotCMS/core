<%@ page import="com.dotmarketing.portlets.folders.model.Folder" %>
<%@ page import="com.dotmarketing.business.APILocator" %>
<%@ page import="com.dotmarketing.business.PermissionAPI" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ include file="/html/common/init.jsp" %>
<%@ include file="/html/common/top_inc.jsp" %>

<%
    final String folderIdentifier = request.getParameter("folderIdentifier");
    if (UtilMethods.isSet(folderIdentifier)) {
        final Folder pushHistoryFolder = APILocator.getFolderAPI().find(folderIdentifier, user, false);
        final PermissionAPI pushHistoryPermissionAPI = APILocator.getPermissionAPI();
        if (pushHistoryFolder != null && UtilMethods.isSet(pushHistoryFolder.getInode())
                && pushHistoryPermissionAPI.doesUserHavePermission(pushHistoryFolder,
                        PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user)) {
            request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT, pushHistoryFolder);
%>
<%@ include file="/html/portlet/ext/common/edit_publishing_status_inc.jsp" %>
<%
        }
    }
%>
