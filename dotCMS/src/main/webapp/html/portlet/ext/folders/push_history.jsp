<%@ page import="com.dotmarketing.portlets.folders.model.Folder" %>
<%@ page import="com.dotmarketing.business.APILocator" %>
<%@ page import="com.dotmarketing.business.PermissionAPI" %>
<%@ page import="com.dotmarketing.exception.DotSecurityException" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.liferay.portal.language.LanguageUtil" %>
<%@ include file="/html/common/init.jsp" %>
<%@ include file="/html/common/top_inc.jsp" %>

<%
    final String folderIdentifier = request.getParameter("folderIdentifier");
    boolean pushHistoryNothingToShow = false;
    if (UtilMethods.isSet(folderIdentifier)) {
        Folder pushHistoryFolder = null;
        try {
            pushHistoryFolder = APILocator.getFolderAPI().find(folderIdentifier, user, false);
        } catch (DotSecurityException e) {
            // `find` returns null for an id that resolves to nothing, but *throws* for a real folder
            // the caller cannot read (`FolderAPIImpl.find`). Both mean "nothing to show here", so
            // they render the same empty body rather than one of them surfacing an error page.
            pushHistoryFolder = null;
        }
        final PermissionAPI pushHistoryPermissionAPI = APILocator.getPermissionAPI();
        if (pushHistoryFolder != null && UtilMethods.isSet(pushHistoryFolder.getInode())
                && pushHistoryPermissionAPI.doesUserHavePermission(pushHistoryFolder,
                        PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user)) {
            request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT, pushHistoryFolder);
%>
<%@ include file="/html/portlet/ext/common/edit_publishing_status_inc.jsp" %>
<%
        } else {
            pushHistoryNothingToShow = true;
        }
    } else {
        pushHistoryNothingToShow = true;
    }

    // One message for all three denied paths — a missing identifier, a folder this user cannot
    // read, and a readable folder without EDIT_PERMISSIONS — so it discloses nothing about which
    // one applied. A blank frame would disclose the same nothing, but leaves the user unable to
    // tell an answer from a failure to load.
    if (pushHistoryNothingToShow) {
%>
<div class="noResultsMessage">
    <%= LanguageUtil.get(pageContext, "message.insufficient.permissions.to.read") %>
</div>
<%
    }
%>
