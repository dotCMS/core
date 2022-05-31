<%@page import="com.dotcms.repackage.javax.portlet.WindowState"%>
<%@ include file="/html/portlet/ext/folders/init.jsp" %>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@page import="com.dotcms.enterprise.license.LicenseLevel"%>
<%@ page import="com.dotcms.publisher.endpoint.bean.PublishingEndPoint"%>
<%@ page import="com.dotcms.publisher.endpoint.business.PublishingEndPointAPI"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="java.util.List"%>

<%
	String r = String.valueOf(System.currentTimeMillis());
%>
<script language="JavaScript">

var enterprise = <%=LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level%>;

<%  boolean inFrame=(UtilMethods.isSet(request.getSession().getAttribute(WebKeys.IN_FRAME)) && (Boolean)request.getSession().getAttribute(WebKeys.IN_FRAME))?true:false;
    String locationMode = inFrame?"window.location":"top.location";
    PublishingEndPointAPI pepAPI = APILocator.getPublisherEndPointAPI();
	List<PublishingEndPoint> sendingEndpoints = pepAPI.getReceivingEndPoints();%>
var sendingEndpoints = <%=UtilMethods.isSet(sendingEndpoints) && !sendingEndpoints.isEmpty()%>;

// File Flyout
function getFilePopUp(i,ctxPath, objId, parentId, openNodes, referer,fileExt,live,working,deleted,locked,read,write,publish,userId,imageIdentifier) {
	var strHTML = '';
	strHTML += '<div dojoType="dijit.Menu" class="dotContextMenu" id="popupTr' + i + '"  style="display: none;" targetNodeIds="td' + i + '">';

		if ((read=="1")&&(deleted!="1")) {
			strHTML += '<div dojoType="dijit.MenuItem" iconClass="previewIcon" onClick="viewFile(\'' + imageIdentifier + '\',\'' + fileExt + '\');">';
	        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Open-Preview")) %>';
			strHTML += '</div>';
		}

		if (((live=="1") || (working=="1")) && (write=="1") && (deleted!="1")) {

			if (isInodeSet(parentId)) {
				strHTML += '<div dojoType="dijit.MenuItem" iconClass="editIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/edit_file" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&r=<%=r%>&parent=' + parentId + '&inode=' + objId + '&userId=' + userId + '&referer=' + referer + openNodes + '\'">';
				strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Edit")) %>';
				strHTML += '</div>';
	        }
			else {
				strHTML += '<div dojoType="dijit.MenuItem" iconClass="editIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/edit_file" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&r=<%=r%>&inode=' + objId + '&userId=' + userId + '&referer=' + referer + openNodes + '\'">';
				strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Edit")) %>';
				strHTML += '</div>';
			}
		}

		if ((working=="1")&& (publish=="1")&&(deleted!="1")) {
			strHTML += '<div dojoType="dijit.MenuItem" iconClass="publishIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/publish_files" /></portlet:actionURL>&parent=' + parentId + '&r=<%=r%>&publishInode=' + objId + '&referer=' + referer + openNodes + '\'">';
            strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Publish")) %>';
			strHTML += '</div>';
		}

		if ((live!="1") && (working=="1") && (publish=="1")) {
			if (deleted!="1") {
				strHTML += '<div dojoType="dijit.MenuItem" iconClass="archiveIcon" onClick="deleteFile(\'' + objId + '\',\'' + parentId + '\',\'' + openNodes + '\',\'' + escape(referer) + '\');">';
	            strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Archive")) %>';
				strHTML += '</div>';
			}
			else {
				strHTML += '<div dojoType="dijit.MenuItem" iconClass="unarchiveIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/edit_file" /><portlet:param name="cmd" value="undelete" /></portlet:actionURL>&r=<%=r%>&parent=' + parentId + '&inode=' + objId + '&referer=' + referer + openNodes + '\'">';
               	strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Un-Archive")) %>';
				strHTML += '</div>';
			}
		}

		if ((live=="1") && (publish=="1")) {
			strHTML += '<div dojoType="dijit.MenuItem" iconClass="unpublishIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/edit_file" /><portlet:param name="cmd" value="unpublish" /></portlet:actionURL>&r=<%=r%>&parent=' + parentId + '&inode=' + objId + '&referer=' + referer + openNodes + '\'">';
	        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unpublish")) %>';
			strHTML += '</div>';
		}
		if ((locked=="1") && (write=="1")) {
			strHTML += '<div dojoType="dijit.MenuItem" iconClass="unlockIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/edit_file" /><portlet:param name="cmd" value="unlock" /></portlet:actionURL>&r=<%=r%>&parent=' + parentId + '&inode=' + objId + '&referer=' + referer + openNodes + '\'">';
	        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unlock")) %>';
			strHTML += '</div>';
		}
		if ((live!="1") && (working!="1") && (write=="1")) {
				strHTML += '<div dojoType="dijit.MenuItem" onClick="deleteFileVersion(\'' + objId + '\',\'' + openNodes + '\',\'' + escape(referer) + '\');">';
	            strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete-Version")) %>';
				strHTML += '</div>';

	            strHTML += '<div dojoType="dijit.MenuItem" onClick="selectFileVersion(\'' + parentId + '\',\'' + objId + '\',\'' + openNodes + '\',\'' + escape(referer) + '\');">';
	            strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Bring-Back-Version")) %>';
				strHTML += '</div>';
		}


		if ((deleted == "1") && (write == "1"))
		{
				strHTML += '<div dojoType="dijit.MenuItem" iconClass="deleteIcon" onClick="if(confirm(\'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.file_asset.confirm.delete")) %>\')){<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/edit_file" /><portlet:param name="cmd" value="full_delete" /></portlet:actionURL>&r=<%=r%>&parent=' + parentId + '&inode=' + objId + '&referer=' + referer + openNodes + '\';}">';
		        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete-File")) %>';
				strHTML += '</div>';

		}

		strHTML += '<div dojoType="dijit.MenuItem" iconClass="closeIcon" class="pop_divider">';
		strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Close")) %>';
		strHTML += '</div>';

	strHTML += '</div>';

	return strHTML;
}


// Container Flyout
function getContainerPopUp(i,ctxPath, objId, objIden, openNodes, referer,live,working,deleted,locked,read,write,publish,userId,hasLiveVersion) {

	var strHTML = '';
	strHTML += '<div dojoType="dijit.Menu" class="dotContextMenu" id="popupTr' + i + '" style="display: none;" targetNodeIds="tr' + i + '">';

		if (((live=="1") || (working=="1")) && (write=="1") && (deleted!="1")) {
			strHTML += '<div dojoType="dijit.MenuItem" iconClass="editIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>" >' +
			'<portlet:param name="struts_action" value="/ext/containers/edit_container" />' +
			'<portlet:param name="cmd" value="edit" />' +
			'</portlet:actionURL>&inode=' + objId + '&referer=' + referer + openNodes + '\'">';
            strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Edit")) %>';
			strHTML += '</div>';
		}

		if ((working=="1") && (publish=="1") && (deleted!="1")) {
			strHTML += '<div dojoType="dijit.MenuItem" iconClass="publishIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/containers/publish_containers" /></portlet:actionURL>&r=<%=r%>&publishInode=' + objId + '&referer=' + referer + openNodes + '\'">';
            strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Publish")) %>';
			strHTML += '</div>';
		}

		if ((working=="1") && (publish=="1") && (deleted!="1") && enterprise && sendingEndpoints) {
			strHTML += '<div dojoType="dijit.MenuItem" iconClass="sServerIcon" onClick="remotePublish(\'' + objIden + '\');">';
            strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Remote-Publish"))%>';
			strHTML += '</div>';
		}

		if ((working=="1") && (publish=="1") && (deleted!="1") && enterprise) {

			strHTML += '<div dojoType="dijit.MenuItem" iconClass="bundleIcon" onClick="addToBundle(\'' + objIden + '\');">';
			strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-To-Bundle"))%>';

			strHTML += '</div>';
		}

		if ((live!="1") && (hasLiveVersion!="1") && (working=="1") && (publish=="1")) {
			if (deleted!="1") {
				strHTML += '<div dojoType="dijit.MenuItem" iconClass="archiveIcon" onClick="javascript: deleteContainer(\'' + objId + '\',\'' + openNodes + '\',\'' + escape(referer) + '\');">';
                strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Archive")) %>';
				strHTML += '</div>';
			}
			else {
				strHTML += '<div dojoType="dijit.MenuItem" iconClass="unarchiveIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/containers/edit_container" /><portlet:param name="cmd" value="undelete" /></portlet:actionURL>&r=<%=r%>&inode=' + objId + '&referer=' + referer + openNodes + '\'">';
                strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Un-Archive")) %>';
				strHTML += '</div>';
			}
		}
		if ((live=="1" || hasLiveVersion=="1") && (publish=="1")) {
			strHTML += '<div dojoType="dijit.MenuItem" iconClass="unpublishIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/containers/edit_container" /><portlet:param name="cmd" value="unpublish" /></portlet:actionURL>&r=<%=r%>&inode=' + objId + '&referer=' + referer + openNodes + '\'">';
	        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unpublish"))%> ';
			strHTML += '</div>';
		}
		if ((locked=="1") && (write=="1")) {
			strHTML += '<div dojoType="dijit.MenuItem" iconClass="unlockIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/containers/edit_container" /><portlet:param name="cmd" value="unlock" /></portlet:actionURL>&r=<%=r%>&inode=' + objId + '&referer=' + referer + openNodes + '\'">';
	        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unlock"))%>' ;
			strHTML += '</div>';
		}
		if ((live!="1") && (working!="1") && (write=="1")) {
			strHTML += '<div dojoType="dijit.MenuItem" onClick="deleteContainerVersion(\'' + objId + '\',\'' + openNodes + '\',\'' + escape(referer) + '\');">';
	        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete-Version"))%>';
			strHTML += '</div>';

	        strHTML += '<div dojoType="dijit.MenuItem" onClick="selectContainerVersion(\'' + objId + '\',\'' + openNodes + '\',\'' + escape(referer) + '\');">';
	        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Bring-Back-this-Version"))%>';
			strHTML += '</div>';
		}

		if ((deleted == "1") && (write == "1"))
		{
			strHTML += '<div dojoType="dijit.MenuItem" iconClass="deleteIcon" onClick="if(confirm(\'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.containers.confirm.delete.container")) %>\')){   delContainer(\'' +objId+ '\', \'' +referer+ '\', true); }">';
			strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete-Container"))%>';
			strHTML += '</div>';
		}
		if (((live=="1") || (working=="1")) && (write=="1") && (deleted!="1"))  {
			strHTML += '<div dojoType="dijit.MenuItem" iconClass="copyIcon" class="pop_divider" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/containers/edit_container" /><portlet:param name="cmd" value="copy" /></portlet:actionURL>&r=<%=r%>&inode=' + objId + '&referer=' + referer + '\';">';
	        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Copy")) %>';
			strHTML += '</div>';
		}
		strHTML += '<div dojoType="dijit.MenuItem" iconClass="closeIcon" class="pop_divider">';
		strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Close")) %>';
		strHTML += '</div>';

		strHTML += '</div>';

	return strHTML;
}


// Link Flyout
function getLinkPopUp(i,ctxPath, objId, objIden, parentId, openNodes, referer,live,working,deleted,locked,read,write,publish,userId,hasLiveVersion) {
		var strHTML = '';
		strHTML += '<div dojoType="dijit.Menu" class="dotContextMenu" id="popupTr' + i + '" style="display: none;" targetNodeIds="tr' + i + '">';

		if (((live=="1") || (working=="1")) && (read=="1") && (deleted!="1")) {
			var actionLabel = (write=="1") ? '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Edit"))%>' : '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "View"))%>';

			if (isInodeSet(parentId)) {
				strHTML += '<div dojoType="dijit.MenuItem" iconClass="editIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/edit_link" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&parent=' + parentId + '&r=<%=r%>&inode=' + objId + '&referer=' + referer + openNodes + '\'">';
	            strHTML += ''+actionLabel;
				strHTML += '</div>';
	        }
			else {
				strHTML += '<div dojoType="dijit.MenuItem" iconClass="editIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/edit_link" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&r=<%=r%>&inode=' + objId + '&referer=' + referer + openNodes + '\';">';
	            strHTML += ''+actionLabel;
				strHTML += '</div>';
			}
		}
		if ((working=="1") && (publish=="1") && (deleted!="1")) {
			strHTML += '<div dojoType="dijit.MenuItem" iconClass="publishIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/publish_links" /></portlet:actionURL>&parent=' + parentId + '&publishInode=' + objId + '&r=<%=r%>&referer=' + referer + openNodes + '\';">';
            strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Publish"))%>';
			strHTML += '</div>';
		}

		if ((working=="1") && (publish=="1") && (deleted!="1") && enterprise && sendingEndpoints) {

		     strHTML += '<div dojoType="dijit.MenuItem" iconClass="sServerIcon" onClick="remotePublish(\'' + objIden + '\');">';
		     strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Remote-Publish"))%>';
		     strHTML += '</div>';
		}

		if ((working=="1") && (publish=="1") && (deleted!="1") && enterprise) {
		     strHTML += '<div dojoType="dijit.MenuItem" iconClass="bundleIcon" onClick="addToBundle(\'' + objIden + '\');">';
		     strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-To-Bundle"))%>';
		     strHTML += '</div>';
		}
		if ((live!="1") && (hasLiveVersion!="1") && (working=="1") && (publish=="1")) {
			if (deleted!="1") {
				strHTML += '<div dojoType="dijit.MenuItem" iconClass="archiveIcon" onClick="deleteLink(\'' + objId + '\',\'' + parentId + '\',\'' + openNodes + '\',\'' + escape(referer) + '\');">';
                strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Archive"))%>';
				strHTML += '</div>';
			}
			else {
				strHTML += '<div dojoType="dijit.MenuItem" iconClass="unarchiveIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/edit_link" /><portlet:param name="cmd" value="undelete" /></portlet:actionURL>&r=<%=r%>&parent=' + parentId + '&inode=' + objId + '&referer=' + referer + openNodes + '\';">';
                strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Un-Archive"))%>';
				strHTML += '</div>';
			}
		}
		if ((live=="1" || hasLiveVersion=="1") && (publish=="1")) {
			strHTML += '<div dojoType="dijit.MenuItem" iconClass="unpublishIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/edit_link" /><portlet:param name="cmd" value="unpublish" /></portlet:actionURL>&r=<%=r%>&parent=' + parentId + '&inode=' + objId + '&referer=' + referer + openNodes + '\'">';
	    	strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unpublish"))%>';
			strHTML += '</div>';
		}
		if ((locked=="1") && (write=="1")) {
			strHTML += '<div dojoType="dijit.MenuItem" iconClass="unlockIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/edit_link" /><portlet:param name="cmd" value="unlock" /></portlet:actionURL>&r=<%=r%>&parent=' + parentId + '&inode=' + objId + '&referer=' + referer + openNodes + '\';">';
	        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unlock"))%>';
			strHTML += '</div>';
		}
		if ((live!="1") && (working!="1") && (write=="1")) {
			strHTML += '<div dojoType="dijit.MenuItem" onClick="deleteLinkVersion(\'' + objId + '\',\'' + openNodes + '\',\'' + escape(referer) + '\');">';
	        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete-Version"))%>';
			strHTML += '</div>';

	        strHTML += '<div dojoType="dijit.MenuItem" onClick="selectLinkVersion(\'' + parentId + '\',\'' + objId + '\',\'' + openNodes + '\',\'' + escape(referer) + '\');">';
	    	strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Bring-Back-this-Version"))%>';
			strHTML += '</div>';
		}


		if ((deleted == "1") && (write == "1"))
		{
			strHTML += '<div dojoType="dijit.MenuItem" iconClass="deleteIcon" onClick="if(confirm(\'Are you sure you want to delete this link (this cannot be undone)?\')){<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/edit_link" /><portlet:param name="cmd" value="full_delete" /></portlet:actionURL>&r=<%=r%>&parent=' + parentId + '&inode=' + objId + '&referer=' + referer + openNodes + '\';}">';
	        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete-Link"))%>';
			strHTML += '</div>';
		}

		strHTML += '<div dojoType="dijit.MenuItem" iconClass="closeIcon" class="pop_divider">';
		strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Close")) %>';
		strHTML += '</div>';

		strHTML += '</div>';
		return strHTML;
}

// Template Flyout
function getTemplatePopUp(i,ctxPath, objId, objIden, openNodes, referer,live,working,deleted,locked,read,write,publish,userId,hasLiveVersion,name) {

	var strHTML = '';
	strHTML += '<div dojoType="dijit.Menu" class="dotContextMenu" id="popupTr' + i + '" style="display: none;" targetNodeIds="tr' + i + '">';

		if (((live=="1") || (working=="1")) && (write=="1") && (deleted!="1")) {
			strHTML += '<div dojoType="dijit.MenuItem" iconClass="editIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/templates/edit_template" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&inode=' + objId + '&r=<%=r%>&referer=' + referer + openNodes + '\';">';
            strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Edit"))%>';
			strHTML += '</div>';
		}
		if ((working=="1") && (publish=="1") && (deleted!="1")) {
			strHTML += '<div dojoType="dijit.MenuItem" iconClass="publishIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/templates/publish_templates" /><portlet:param name="cmd" value="prepublish" /></portlet:actionURL>&publishInode=' + objId + '&r=<%=r%>&referer=' + referer + openNodes + '\';">';
            strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Publish"))%>';
			strHTML += '</div>';
		}
		if ((working=="1") && (publish=="1") && (deleted!="1") && enterprise && sendingEndpoints) {
			strHTML += '<div dojoType="dijit.MenuItem" iconClass="sServerIcon" onClick="remotePublish(\'' + objIden + '\');">';
            strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Remote-Publish"))%>';
			strHTML += '</div>';
		}

		if ((working=="1") && (publish=="1") && (deleted!="1") && enterprise) {
			strHTML += '<div dojoType="dijit.MenuItem" iconClass="bundleIcon" onClick="addToBundle(\'' + objIden + '\');">';
			strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-To-Bundle"))%>';
			strHTML += '</div>';
		}

		if ((live!="1") && (hasLiveVersion!="1") && (working=="1") && (publish=="1")) {
			if (deleted!="1") {
				strHTML += '<div dojoType="dijit.MenuItem" iconClass="archiveIcon" onClick="deleteTemplate(\'' + objId + '\',\'' + openNodes + '\',\'' + escape(referer) + '\');">';
                strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Archive"))%>';
				strHTML += '</div>';
			}
			else {
				strHTML += '<div dojoType="dijit.MenuItem" iconClass="unarchiveIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/templates/edit_template" /><portlet:param name="cmd" value="undelete" /></portlet:actionURL>&inode=' + objId + '&r=<%=r%>&referer=' + referer + openNodes + '\';">';
            	strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Un-Archive"))%>';
				strHTML += '</div>';
			}
		}
		if ((live=="1" || hasLiveVersion=="1") && (publish=="1")) {
			strHTML += '<div dojoType="dijit.MenuItem" iconClass="unpublishIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/templates/edit_template" /><portlet:param name="cmd" value="unpublish" /></portlet:actionURL>&inode=' + objId + '&r=<%=r%>&referer=' + referer + openNodes + '\';">';
	  		strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unpublish"))%>';
			strHTML += '</div>';
		}
		if ((locked=="1") && (write=="1")) {
			strHTML += '<div dojoType="dijit.MenuItem" iconClass="unlockIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/templates/edit_template" /><portlet:param name="cmd" value="unlock" /></portlet:actionURL>&inode=' + objId + '&r=<%=r%>&referer=' + referer + openNodes + '\';">';
	  		strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unlock"))%>';
			strHTML += '</div>';
		}
		if ((live!="1") && (working!="1") && (write=="1")) {
				strHTML += '<div dojoType="dijit.MenuItem" onClick="deleteTemplateVersion(\'' + objId + '\',\'' + openNodes + '\',\'' + escape(referer) + '\');">';
	        	strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete-Version"))%>';
				strHTML += '</div>';

	            strHTML += '<div dojoType="dijit.MenuItem" onClick="selectTemplateVersion(\'' + objId + '\',\'' + openNodes + '\',\'' + escape(referer) + '\');">';
	            strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Bring-Back-Version"))%>';
				strHTML += '</div>';
		}

		if ((deleted == "1") && (write == "1"))
		{
            strHTML += '<div dojoType="dijit.MenuItem" iconClass="deleteIcon" onClick="if(confirm(\'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.template.confirm.delete.template"))%>\')){ delTemplate(\'' +objId+ '\', \'' +referer+ '\', \'' +objIden+ '\', \'' +name+ '\'); }">';
            strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete-Template"))%>';
            strHTML += '</div>';
		}
		if (((live=="1") || (working=="1")) && (write=="1") && (deleted!="1"))  {
			strHTML += '<div dojoType="dijit.MenuItem" iconClass="copyIcon" class="pop_divider" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/templates/edit_template" /><portlet:param name="cmd" value="copy" /></portlet:actionURL>&inode=' + objId + '&r=<%=r%>&referer=' + referer + '\';">';
	        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Copy"))%>';
			strHTML += '</div>';
		}

		strHTML += '<div dojoType="dijit.MenuItem" iconClass="closeIcon" class="pop_divider">';
		strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Close")) %>';
		strHTML += '</div>';

	strHTML += '</div>';
	return strHTML;
}


// HTML Page Flyout
function getHTMLPagePopUp(i,ctxPath, objId, objIden, parentId, openNodes, referer,live,working,deleted,locked,read,write,publish,userId,isLegacyPage) {
	var strHTML = '';
		strHTML += '<div dojoType="dijit.Menu" class="dotContextMenu" id="popupTr' + i + '" style="display: none;" targetNodeIds="tr' + i + '">';

		if (((live=="1") || (working=="1")) && (read=="1") && (deleted!="1")) {
			strHTML += '<div dojoType="dijit.MenuItem" iconClass="pageIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpages/preview_htmlpage" /><portlet:param name="previewPage" value="1" /></portlet:actionURL>&parent=' + parentId + '&r=<%=r%>&inode=' + objId + '&referer=' + referer + openNodes + '\';">';
    	    strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Open-Preview")) %>';
			strHTML += '</div>';

			if (((live=="1") || (working=="1")) && (read=="1"))  {
					var actionLabel = (write=="1") ?  '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Open-Edit")) %>': '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "View"))%>';

					if(isLegacyPage){
						if (isInodeSet(parentId)) {
							strHTML += '<div dojoType="dijit.MenuItem" iconClass="pagePropIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpages/edit_htmlpage" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&parent=' + parentId + '&r=<%=r%>&inode=' + objId + '&userId=' + userId + '&referer=' + referer + openNodes + '\';">';
							strHTML += actionLabel;
							strHTML += '</div>';
				        }
						else {
							strHTML += '<div dojoType="dijit.MenuItem" iconClass="pagePropIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpages/edit_htmlpage" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&inode=' + objId + '&r=<%=r%>&userId=' + userId + '&referer=' + referer + openNodes + '\';">';
							strHTML += actionLabel;
							strHTML += '</div>';
			    	    }
					}else{
						if (isInodeSet(parentId)) {
							strHTML += '<div dojoType="dijit.MenuItem" iconClass="pagePropIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/contentlet/edit_contentlet" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&parent=' + parentId + '&r=<%=r%>&inode=' + objId + '&userId=' + userId + '&referer=' + referer + openNodes + '\';">';
							strHTML += actionLabel;
							strHTML += '</div>';
				        }
						else {
							strHTML += '<div dojoType="dijit.MenuItem" iconClass="pagePropIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/contentlet/edit_contentlet" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&inode=' + objId + '&r=<%=r%>&userId=' + userId + '&referer=' + referer + openNodes + '\';">';
							strHTML += actionLabel;
							strHTML += '</div>';
			    	    }
					}
			}

		}
		if (deleted!="1") {
	      strHTML += '<div dojoType="dijit.MenuItem" iconClass="statisticsIcon" onClick="<%=locationMode%>=\'<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpageviews/view_htmlpage_views" /></portlet:renderURL>&htmlpage=' + objId + '&userId=' + userId + '&r=<%=r%>&referer=' + referer + openNodes + '\';">';
	      strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "View-Statistics")) %>';
	      strHTML += '</div>';
	    }
		if ((working=="1") && (deleted!="1") && (publish=="1")) {
			strHTML += '<div dojoType="dijit.MenuItem" iconClass="publishIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpages/publish_htmlpages" /><portlet:param name="cmd" value="prepublish" /></portlet:actionURL>&parent=' + parentId + '&r=<%=r%>&publishInode=' + objId + '&referer=' + referer + openNodes + '\';">';
   	        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Publish")) %>';
			strHTML += '</div>';
		}

		if ((working=="1") && (publish=="1") && (deleted!="1") && enterprise && sendingEndpoints) {
	      strHTML += '<div dojoType="dijit.MenuItem" iconClass="sServerIcon" onClick="remotePublish(\'' + objIden + '\');">';
	            strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Remote-Publish"))%>';
	      strHTML += '</div>';
	    }

	    if ((working=="1") && (publish=="1") && (deleted!="1") && enterprise) {
	        strHTML += '<div dojoType="dijit.MenuItem" iconClass="bundleIcon" onClick="addToBundle(\'' + objIden + '\');">';
            strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-To-Bundle"))%>';
	        strHTML += '</div>';
	    }

		if ((live=="1") && (publish=="1")) {
			if(isLegacyPage){
				strHTML += '<div dojoType="dijit.MenuItem" iconClass="unpublishIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpages/edit_htmlpage" /><portlet:param name="cmd" value="unpublish" /></portlet:actionURL>&parent=' + parentId + '&r=<%=r%>&inode=' + objId + '&referer=' + referer + openNodes + '\';">';
			}else{
				strHTML += '<div dojoType="dijit.MenuItem" iconClass="unpublishIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/contentlet/edit_contentlet" /><portlet:param name="cmd" value="unpublish" /></portlet:actionURL>&parent=' + parentId + '&r=<%=r%>&inode=' + objId + '&referer=' + referer + openNodes + '\';">';
			}
	        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unpublish")) %>';
			strHTML += '</div>';
		}
		if ((live!="1") && (working=="1") && (publish=="1")) {
			if (deleted!="1") {
				strHTML += '<div dojoType="dijit.MenuItem" iconClass="archiveIcon" onClick="deleteHTMLPage(\'' + objId + '\',\'' + parentId + '\',\'' + openNodes + '\',\'' + escape(referer) + '\',' + isLegacyPage + ');">';
				strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Archive")) %>';
				strHTML += '</div>';
			}
			else {
				if(isLegacyPage){
					strHTML += '<div dojoType="dijit.MenuItem" iconClass="unarchiveIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpages/edit_htmlpage" /><portlet:param name="cmd" value="undelete" /></portlet:actionURL>&parent=' + parentId + '&r=<%=r%>&inode=' + objId + '&referer=' + referer + openNodes + '\';">';
				}else{
					strHTML += '<div dojoType="dijit.MenuItem" iconClass="unarchiveIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/contentlet/edit_contentlet" /><portlet:param name="cmd" value="undelete" /></portlet:actionURL>&parent=' + parentId + '&r=<%=r%>&inode=' + objId + '&referer=' + referer + openNodes + '\';">';
				}
				strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Un-Archive")) %>';
				strHTML += '</div>';
			}
		}

		if ((locked=="1") && (write=="1")) {
			if(isLegacyPage){
				strHTML += '<div dojoType="dijit.MenuItem" iconClass="unlockIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpages/edit_htmlpage" /><portlet:param name="cmd" value="unlock" /></portlet:actionURL>&parent=' + parentId + '&r=<%=r%>&inode=' + objId + '&referer=' + referer + openNodes + '\';">';
			}else{
				strHTML += '<div dojoType="dijit.MenuItem" iconClass="unlockIcon" onClick="<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/contentlet/edit_contentlet" /><portlet:param name="cmd" value="unlock" /></portlet:actionURL>&parent=' + parentId + '&r=<%=r%>&inode=' + objId + '&referer=' + referer + openNodes + '\';">';
			}
	        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unlock")) %>';
			strHTML += '</div>';
		}
		if ((live!="1") && (working!="1") && (write=="1")) {
				strHTML += '<div dojoType="dijit.MenuItem" onClick="deleteHTMLPageVersion(\'' + objId + '\',\'' + openNodes + '\',\'' + escape(referer) + '\');">';
	            strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete-Version")) %>';
				strHTML += '</div>';

				strHTML += '<div dojoType="dijit.MenuItem" onClick="selectHTMLPageVersion(\'' + parentId + '\',\'' + objId + '\',\'' + openNodes + '\',\'' + escape(referer) + '\');">';
	            strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Bring-Back-this-Version")) %>';
				strHTML += '</div>';
		}


		if ((deleted == "1") && (write == "1"))
		{
			if(isLegacyPage){
				strHTML += '<div dojoType="dijit.MenuItem" iconClass="deleteIcon" onClick="if(confirm(\'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.htmlpage.confirm.delete")) %>\')){<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpages/edit_htmlpage" /><portlet:param name="cmd" value="full_delete" /></portlet:actionURL>&parent=' + parentId + '&r=<%=r%>&inode=' + objId + '&referer=' + referer + openNodes + '\';}">';
			}else{
				strHTML += '<div dojoType="dijit.MenuItem" iconClass="deleteIcon" onClick="if(confirm(\'<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.htmlpage.confirm.delete")) %>\')){<%=locationMode%>=\'<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/contentlet/edit_contentlet" /><portlet:param name="cmd" value="full_delete" /></portlet:actionURL>&parent=' + parentId + '&r=<%=r%>&inode=' + objId + '&referer=' + referer + openNodes + '\';}">';
			}
			strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete-Page")) %>';
			strHTML += '</div>';
		}

		strHTML += '<div dojoType="dijit.MenuItem" iconClass="closeIcon" class="pop_divider">';
		strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Close")) %>';
		strHTML += '</div>';

	strHTML += '</div>';
	return strHTML;
}

</script>
