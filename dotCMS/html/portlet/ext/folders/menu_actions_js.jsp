<%@ page import="com.liferay.portlet.LiferayWindowState" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.liferay.portal.language.LanguageUtil" %>
<%@ include file="/html/portlet/ext/containers/init.jsp" %>

<script language="Javascript">
// view_folders.js
	var previousRedFolder = '';
	var previousOpenFolder = '';

	var foldersIcons	= new Array();
	foldersIcons[0] = "<%=COMMON_IMG%>/trees/root.gif";
//	foldersIcons[1] = "<%=COMMON_IMG%>/trees/spacer.gif";
	foldersIcons[1] = "/html/images/shim.gif";
//	foldersIcons[2] = "<%=COMMON_IMG%>/trees/line.gif";
	foldersIcons[2] = "/html/images/shim.gif";
//	foldersIcons[3] = "<%=COMMON_IMG%>/trees/join.gif";
	foldersIcons[3] = "/html/images/shim.gif";
//	foldersIcons[4] = "<%=COMMON_IMG%>/trees/join_bottom.gif";
	foldersIcons[4] = "/html/images/shim.gif";
	foldersIcons[5] = "/html/images/icons/toggle-small.png";
	foldersIcons[6] = "/html/images/icons/toggle-small.png";
	foldersIcons[7] = "/html/images/icons/toggle-small-expand.png";
	foldersIcons[8] = "/html/images/icons/toggle-small-expand.png";
	foldersIcons[9] = "/html/images/icons/folder-horizontal.png";
	foldersIcons[10] = "/html/images/icons/folder-horizontal-open.png";
	//files icon
	foldersIcons[11] = "/portal/images/icons/entry15.gif";
	//containers icon
	foldersIcons[12] = "/portal/images/icons/entry12.gif";
	//templates icon
	foldersIcons[13] = "/portal/images/icons/entry13.gif";
	//contentlet icon
	foldersIcons[14] = "/portal/images/icons/entry14.gif";
	//html page icon
	foldersIcons[15] = "/portal/images/icons/entry15.gif";
	//link icon
	foldersIcons[16] = "<%=COMMON_IMG%>/trees/link.gif";
	//animated folder gif
//	foldersIcons[20] = "<%=COMMON_IMG%>/trees/folder_ani.gif";
	foldersIcons[20] = "/html/images/shim.gif";
//	foldersIcons[21] = "/portal/images/icons/folder_open_red.gif";
	foldersIcons[21] = "/html/images/icons/folder-horizontal-open.png";
	foldersIcons[22] = "<%=COMMON_IMG%>/trees/processing.gif";
	//status icons
	foldersIcons[23] = "/portal/images/icons/dot_green.gif";
	foldersIcons[24] = "/portal/images/icons/dot_yellow.gif";
	foldersIcons[25] = "/portal/images/icons/dot_red.gif";
	foldersIcons[26] = "/portal/images/icons/locked.gif";
	foldersIcons[27] = "/portal/images/icons/show.gif";

	foldersIcons[28] = "/portal/images/icons/folder_show.gif";
	foldersIcons[29] = "/portal/images/icons/folder_show.gif";
	foldersIcons[30] = "/portal/images/icons/folder_open_red_show.gif";
	foldersIcons[31] = "/portal/images/icons/unlocked.gif";
	
	//root icons
	foldersIcons[32] = "/html/images/icons/world_off.gif";
	foldersIcons[33] = "/html/images/icons/world_on.gif";

	foldersIcons[34] = "/portal/images/icons/dot_grey.gif";
	foldersIcons[35] = "/portal/images/icons/deleted.gif";

	function deleteHost(objId,openNodes,view,content) {
		if (confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "folder.delete.selected.host")) %>')) {
			self.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/folders/edit_host" /></portlet:actionURL>&cmd=delete&inode=' + objId + '&openNodes=' + openNodes + "&view=" + view + "&content=" + content;
		}
	}

	function deleteFolder(objId,parentId,openNodes,view,content) {
		if (confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "folder.delete.selected.folder")) %>')) {
			self.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/folders/edit_folder" /></portlet:actionURL>&cmd=delete&inode=' + objId + '&pfolderId=' + parentId + '&openNodes=' + openNodes + "&view=" + view + "&content=" + content;
		}
	}

	function setAsDefaultHost(objId,openNodes,view,content) {
		if (confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "folder.set.host.as.default")) %>')) {
			self.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/folders/edit_host" /></portlet:actionURL>&cmd=set_as_default&inode=' + objId + '&openNodes=' + openNodes + "&view=" + view + "&content=" + content;
		}
	}

	function addHost(openNodes,view,content) {
		self.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/folders/edit_host" /></portlet:actionURL>&openNodes=' + openNodes + "&view=" + view + "&content=" + content;
	}

	function addFolder(parentId,openNodes,view,content) {
		openNodes = addNode(openNodes,parentId);
		self.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/folders/edit_folder" /></portlet:actionURL>&pfolderId=' + parentId + '&openNodes=' + openNodes + "&view=" + view + "&content=" + content;
	}

	function addFile(parentId,openNodes,referer) {
		openNodes = addNode(openNodes,parentId);
		self.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/edit_file" /></portlet:actionURL>&cmd=edit&parent=' + parentId + '&inode=""&referer=' + referer + openNodes;
	}
	function addMultipleFile(parentId,openNodes,referer) {
		openNodes = addNode(openNodes,parentId);
		self.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/upload_multiple" /></portlet:actionURL>&cmd=edit&parent=' + parentId + '&inode=""&referer=' + referer + openNodes;
	}
	function addHTMLPage(parentId,openNodes,referer) {
		openNodes = addNode(openNodes,parentId);
		self.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpages/edit_htmlpage" /></portlet:actionURL>&cmd=edit&parent=' + parentId + '&inode=""&referer=' + referer + openNodes;
	}
	function addLink(parentId,openNodes,referer) {
		openNodes = addNode(openNodes,parentId);
		self.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/edit_link" /></portlet:actionURL>&cmd=edit&parent=' + parentId + '&inode=""&referer=' + referer + openNodes;
	}
	function previewContentlet(objId) {
	   previewconwin = window.open('<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/contentlet/preview_contentlet" /></portlet:actionURL>&inode=' + objId, "previewconwin", 'width=800,height=600,scrollbars=yes,resizable=yes');
	}
	function previewTemplate(objId) {
	   previewtemwin = window.open('<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/templates/preview_template" /></portlet:actionURL>&inode=' + objId, "previewtemwin", 'width=800,height=600,scrollbars=yes,resizable=yes');
	}
	function deleteContainer(objId,openNodes,referer) {
		if (confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "folder.delete.selected.container")) %>')) {
			self.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/containers/edit_container" /></portlet:actionURL>&cmd=delete&inode=' + objId + '&referer=' + referer + openNodes;
		}
	}
	function deleteTemplate(objId,openNodes,referer) {
		if (confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "folder.delete.selected.template")) %>')) {
			self.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/templates/edit_template" /></portlet:actionURL>&cmd=delete&inode=' + objId + '&referer=' + referer + openNodes;
		}
	}
	function deleteHTMLPage(objId,parentId,openNodes,referer) {
		if (confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "folder.delete.selected.htmlpage")) %>')) {
			self.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpages/edit_htmlpage" /></portlet:actionURL>&cmd=delete&parent=' + parentId + "&inode=" + objId + '&referer=' + referer + openNodes;
		}
	}
	function deleteContentlet(objId,openNodes,referer) {
		if (confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "folder.archive.selected.contentlet")) %>')) {
			self.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/contentlet/edit_contentlet" /></portlet:actionURL>&cmd=delete&inode=' + objId + '&referer=' + referer + openNodes;
		}
	}
	function deleteFile(objId,parentId,openNodes,referer) {
		if (confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "folder.archive.selected.file")) %>')) {
			self.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/edit_file" /></portlet:actionURL>&cmd=delete&parent=' + parentId + "&inode=" + objId + '&referer=' + referer + openNodes;
		}
	}
	function deleteLink(objId,parentId,openNodes,referer) {
		if (confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "folder.archive.selected.link")) %>')) {
			self.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/edit_link" /></portlet:actionURL>&cmd=delete&parent=' + parentId + "&inode=" + objId + '&referer=' + referer + openNodes;
		}
	}
	function deleteWorkflowMessage(objId,parentId,openNodes,referer) {
		if (confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "folder.archive.selected.workflow.message")) %>')) {
			self.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/workflowmessages/edit_workflow_message" /></portlet:actionURL>&cmd=delete&parent=' + parentId + "&inode=" + objId + '&referer=' + referer + openNodes;
		}
	}
	function CopyAsset(parentId,objId,referer,actionURL) {
		newwin = window.open('<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/folders/view_folders_popup" /></portlet:actionURL>&view=' + view + '&content=folders&popup=parent&child=true#treeTop', "newwin", 'width=700,height=400,scrollbars=yes,resizable=yes');
		if (document.getElementById("actionURL")) {
			document.getElementById("actionURL").value = actionURL;
		}
		document.getElementById("submitLocation").value = '&inode=' + objId + '&referer=' + escape(referer);
	}
	function MoveAsset(parentId,objId,referer,actionURL) {

		newwin = window.open('<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/folders/view_folders_popup" /></portlet:actionURL>&view=' + view + '&content=folders&popup=parent&child=true#treeTop', "newwin", 'width=700,height=400,scrollbars=yes,resizable=yes');

		if (document.getElementById("actionURL")) {
			document.getElementById("actionURL").value = actionURL;
		}
		document.getElementById("submitLocation").value = '&inode=' + objId + '&referer=' + escape(referer);
	}
	function viewFile(objId,fileExt) {
		var fileInodePath = objId;
		if (fileInodePath.length==1) {
			fileInodePath = fileInodePath + '';
		}
		fileInodePath = fileInodePath.substring(0,1) + '/' + fileInodePath.substring(1,2);
		window.open('/dotAsset/' + fileInodePath + '/' + objId + '.' + fileExt,'fileWin','toolbar=no,resizable=yes,width=400,height=300');
	}

	function reloadTree(objId,node) {
		//i am going to test this jsp
		var divEl = document.getElementById("foldersTreediv" + node);
		var iconEl = document.getElementById("foldersTreeicon" + node);
		if (iconEl) 
			iconEl.src = foldersIcons[20];	// folder_ani.gif

		if (divEl.style.display == "none") {
			//im going to open this inode
			openNodes = addNode(openNodes,objId);
		}
		else {
			//im going to close this inode
			openNodes = removeNode (openNodes,objId);
		}
		window.tree_iframe.location.href = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/folders/load_tree" /><portlet:param name="userId" value="<%= user.getUserId() %>" /></portlet:renderURL>&openNodes=' + openNodes + '&view=' + view + '&content=' + content + '&popup=' + popup + '&inode=' + inode + '&contentOnly=' + contentOnly + '&thumbs=' + thumbs + '&child=true&in_frame=true';
	}

	function showArchived(show) {

		if (show) {
			var	checkedValue = "deleted=<%= com.dotmarketing.db.DbConnectionFactory.getDBTrue() %>";
		}
		else {
			var checkedValue = "working=<%= com.dotmarketing.db.DbConnectionFactory.getDBTrue() %> and deleted=<%= com.dotmarketing.db.DbConnectionFactory.getDBFalse() %>";
		}
		var contentTmp = "allcontent";
		var viewTmp = escape(checkedValue);
		if (isInodeSet(inode)) {
			window.location.href = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/folders/view_contents" /><portlet:param name="userId" value="<%= user.getUserId() %>" /></portlet:renderURL>&inode=' + inode + '&openNodes=' + openNodes + '&view=' + viewTmp + '&content=' + contentTmp + '&child=true&in_frame=true';
		}
		else {
			alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "folder.select.folder")) %>');
		}

	}


	function refreshView(form) {

		var sizeOfBoxes = form.view.length;
		var checkedValue = new String("");
		for (var i = 0;i < sizeOfBoxes; i++)
		{
			if (form.view[i].checked == true)
			{
				checkedValue += form.view[i].value + "=<%= com.dotmarketing.db.DbConnectionFactory.getDBTrue() %>";
				if (form.view[i].value == "working") {
					checkedValue += " and deleted =<%= com.dotmarketing.db.DbConnectionFactory.getDBFalse() %>";
				}
			}
		}
		if (checkedValue == "") {
			checkedValue = "working=<%= com.dotmarketing.db.DbConnectionFactory.getDBTrue() %> and deleted=<%= com.dotmarketing.db.DbConnectionFactory.getDBFalse() %>";
		}
		var contentTmp = "allcontent";
		//var contentTmp = form.content.options[form.content.options.selectedIndex].value;
		var viewTmp = escape(checkedValue);
		if (isInodeSet(inode)) {
			window.content_iframe.location.href = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/folders/view_contents" /><portlet:param name="userId" value="<%= user.getUserId() %>" /></portlet:renderURL>&inode=' + inode + '&openNodes=' + openNodes + '&view=' + viewTmp + '&content=' + contentTmp + '&child=true&in_frame=true';
		}
		else {
			alert("Please select a Folder first");
		}

	}
	function removeNode (openNodes,objId) {
		idx1 = openNodes.indexOf(objId + "|");
		if (idx1!=-1) {
			beforeStr = openNodes.substring(0,idx1);
			idx2 = openNodes.indexOf("|",idx1);
			afterStr = openNodes.substring(idx2+1,openNodes.length);
			return beforeStr + afterStr;
		}
		return openNodes;
	}
	function addNode (openNodes,objId) {
	    //alert(openNodes);
	    //alert(objId + "|");
		if (openNodes.indexOf("|" + objId + "|") == -1 && openNodes.indexOf("/" + objId + "|") == -1) 
		{
		    //alert("enter");
			openNodes += objId + '|';
		}
		return openNodes;
	}
	function changeView(referer,newview) {
		referer = unescape(referer);
		referer = referer.replace(/&content=([a-zA-Z]+)&/gi,"&content="+newview+"&");
		return escape(referer);
	}

    function deleteFileVersion(objId,openNodes,referer){
        if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "folder.delete.file.version")) %>')){
			window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/edit_file" /></portlet:actionURL>&cmd=deleteversion&inode=' + objId + '&referer=' + referer + openNodes;
        }
    }
	function selectFileVersion(parentId,objId,openNodes,referer) {
        if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "folder.replace.file.working.version")) %>')){
			window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/edit_file" /></portlet:actionURL>&cmd=getversionback&inode=' + parentId + '&inode_version=' + objId + '&referer=' + referer + openNodes;
	    }
	}
    function deleteContainerVersion(objId,openNodes,referer){
        if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "folder.delete.container.version")) %>')){
			window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/containers/edit_container" /></portlet:actionURL>&cmd=deleteversion&inode=' + objId + '&referer=' + referer + openNodes;
        }
    }
	function selectContainerVersion(objId,openNodes,referer) {
        if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "folder.replace.container.working.version")) %>')){
			window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/containers/edit_container" /></portlet:actionURL>&cmd=getversionback&inode_version=' + objId + '&referer=' + referer + openNodes;
	    }
	}
    function deleteTemplateVersion(objId,openNodes,referer){
        if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "folder.delete.template.version")) %>')){
			window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/templates/edit_template" /></portlet:actionURL>&cmd=deleteversion&inode=' + objId + '&referer=' + referer + openNodes;
        }
    }
	function selectTemplateVersion(objId,openNodes,referer) {
        if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "folder.replace.template.working.version")) %>')){
			window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/templates/edit_template" /></portlet:actionURL>&cmd=getversionback&inode_version=' + objId + '&referer=' + referer + openNodes;
	    }
	}
    function deleteHTMLPageVersion(objId,openNodes,referer){
        if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "folder.delete.htmlpage.version")) %>')){
			window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpages/edit_htmlpage" /></portlet:actionURL>&cmd=deleteversion&inode=' + objId + '&referer=' + referer + openNodes;
        }
    }
	function selectHTMLPageVersion(parentId,objId,openNodes,referer) {
        if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "folder.replace.htmlpage.working.version")) %>')){
			window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpages/edit_htmlpage" /></portlet:actionURL>&cmd=getversionback&inode=' + parentId + '&inode_version=' + objId + '&referer=' + referer + openNodes;
	    }
	}
    function deleteContentletVersion(objId,openNodes,referer){
        if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "folder.delete.contentlet.version")) %>')){
			window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/contentlet/edit_contentlet" /></portlet:actionURL>&cmd=deleteversion&inode=' + objId + '&referer=' + referer + openNodes;
        }
    }
	function selectContentletVersion(objId,openNodes,referer) {
        if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "folder.replace.contentlet.working.version")) %>')){
			window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/contentlet/edit_contentlet" /></portlet:actionURL>&cmd=getversionback&inode_version=' + objId + '&referer=' + referer + openNodes;
	    }
	}
    function deleteLinkVersion(objId,openNodes,referer){
        if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "folder.delete.link.version")) %>')){
			window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/edit_link" /></portlet:actionURL>&cmd=deleteversion&inode=' + objId + '&referer=' + referer + openNodes;
        }
    }
	function selectLinkVersion(parentId,objId,openNodes,referer) {
        if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "folder.replace.link.working.version")) %>')){
			window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/edit_link" /></portlet:actionURL>&cmd=getversionback&inode=' + parentId + '&inode_version=' + objId + '&referer=' + referer + openNodes;
	    }
	}
    function deleteWorkflowMessageVersion(objId,openNodes,referer){
        if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "folder.delete.workflow.message.version")) %>')){
			window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/workflow_messages/edit_workflow_message" /></portlet:actionURL>&cmd=deleteversion&inode=' + objId + '&referer=' + referer + openNodes;
        }
    }
	function selectWorkflowMessageVersion(parentId,objId,openNodes,referer) {
        if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "folder.replaceworkflow.message.working.version")) %>')){
			window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/workflow_messages/edit_workflow_message" /></portlet:actionURL>&cmd=getversionback&inode=' + parentId + '&inode_version=' + objId + '&referer=' + referer + openNodes;
	    }
	}

	function editPermissions(objId,openNodes,referer) {
		window.location.href = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/permissions/edit_permissions" /></portlet:actionURL>&inode=' + objId + '&referer=' + referer + openNodes;
	}
	
	function publishFolder(objId,openNodes,referer) {
		window.location.href = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/folders/publish_folder" /></portlet:actionURL>&cmd=prepublish&inode=' + objId + '&referer=' + referer + openNodes;
	}
	
	function selectTreeLeaf(popup,objId,name,folderPath, identifierId) {
		try {
			window.opener.document.getElementById(popup).value=objId;
			window.opener.document.getElementById("selected"+popup).value=name;
		} catch (e) { }
		
		if(window.opener.document.getElementById("selectedIdent"+popup) != null){
			window.opener.document.getElementById("selectedIdent"+popup).value=identifierId + '.' + getFileExtension(name);
		}
		if (window.opener.document.getElementById("folder"+popup)) {
			window.opener.document.getElementById("folder"+popup).value=folderPath;
		}
		if (window.opener.document.getElementById("submitParent")) {
			parameter = window.opener.document.getElementById("submitParent").value;
			window.opener.submitParent(parameter);
		}
		
		try {		
			eval('opener.callback' + popup + '(\''+ objId + '\', \''+ identifierId + '\', \'' + name + '\', \'' + folderPath + '\')');
		} catch (e) { }
		
		self.close();
	}

	function selectParentTreeLeaf(popup,objId,identifierId,name,folderPath) {
		if(parent.window.opener.document.getElementById(popup)) {
			parent.window.opener.document.getElementById(popup).value=objId;
		}
		if(parent.window.opener.document.getElementById("selected"+popup)) {
			parent.window.opener.document.getElementById("selected"+popup).value=name;
		}
		if(parent.window.opener.document.getElementById("selectedIdent"+popup)){
			parent.window.opener.document.getElementById("selectedIdent"+popup).value=identifierId + '.' + getFileExtension(name);
		}
		if (parent.window.opener.document.getElementById("folder"+popup)) {
			parent.window.opener.document.getElementById("folder"+popup).value=folderPath;
		}
		if (parent.window.opener.document.getElementById("submitParent")) {
			parent.window.opener.submitParent(popup);
		}
		try {
			eval('parent.window.opener.callback'+popup+'(\''+ objId + '\', \''+ identifierId + '\', \'' + name + '\', \'' + folderPath + '\')');
		} catch (e) { }
		
		parent.close();
	}

	function selectFolder(objId,nodeId,show_on_menu) {
		//for thumbnails iframe
		if (window.thumbnails_iframe) {
			openNodes = addNode(openNodes,objId);
			if ((previousOpenFolder!='') && (previousOpenFolder!=objId)){
				openNodes = removeNode(openNodes,previousOpenFolder);
			}
			setOpenRedFolder(nodeId,objId,show_on_menu);
			inode = objId;
			window.thumbnails_iframe.location.href = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/folders/view_thumbnails" /></portlet:renderURL>&inode=' + objId + '&openNodes=' + openNodes + '&view=' + view + '&userId=<%=user.getUserId()%>&content=allcontent&popup=' + popup + "&child=true&in_frame=true";
		}
		//for load into the content iframe
		else if (window.content_iframe) {
			if (isInodeSet(objId)) {
				window.content_iframe.location.href = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/folders/view_contents" /></portlet:renderURL>&inode=""&openNodes=' + openNodes + '&view=' + view + '&userId=<%=user.getUserId()%>&content=allcontent&child=true&in_frame=true';
			}
			else {
				openNodes = addNode(openNodes,objId);
				if ((previousOpenFolder!='') && (previousOpenFolder!=objId)){
					openNodes = removeNode(openNodes,previousOpenFolder);
				}
				setOpenRedFolder(nodeId,objId,show_on_menu);
				inode = objId;
				window.content_iframe.location.href = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/folders/view_contents" /></portlet:renderURL>&inode=' + objId + '&openNodes=' + openNodes + '&view=' + view + '&userId=<%=user.getUserId()%>&content=allcontent&child=true&in_frame=true';
			}
		}
		//for popup view or inside the content frame
		else {
			parent.openNodes = addNode(parent.openNodes,objId);
			openNodes = addNode(openNodes,objId);
			if (previousOpenFolder!='') {
				parent.openNodes = removeNode(parent.openNodes,previousOpenFolder);
			}
			foldersArray = parent.tree_iframe.foldersArray;
			nodeParentId = getNodeId(objId,foldersArray);
			if (nodeParentId != "") {
				parent.setOpenRedFolder(nodeParentId,objId,show_on_menu);
			}
			window.location.href = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/folders/view_contents" /></portlet:renderURL>&inode=' + objId + '&openNodes=' + openNodes + '&view=' + view + '&userId=<%=user.getUserId()%>&content=allcontent&child=true&in_frame=true';
		}
	}
	function getNodeId(objId,foldersArray) {
		for (i=0;i<foldersArray.length;i++) {
			var nodeValues = foldersArray[i].split("|");
			if (nodeValues[3]==objId) {
				return nodeValues[0];
			}
		}
		return "";

	}
	function getShowOnMenu(objId,foldersArray) {
		for (i=0;i<foldersArray.length;i++) {
			var nodeValues = foldersArray[i].split("|");
			if (nodeValues[3]==objId) {
				return nodeValues[19];
			}
		}
		return 0;
	}
	function getShowOnMenuByNodeId(nodeId,foldersArray) {
		for (i=0;i<foldersArray.length;i++) {
			var nodeValues = foldersArray[i].split("|");
			if (nodeValues[0]==nodeId) {
				return nodeValues[19];
			}
		}
		return 0;
	}
	function setOpenRedFolder(nodeId,objId,show_on_menu) {
		if (previousRedFolder != '') {
			var iconEl = document.getElementById("foldersTreeicon" + previousRedFolder);
			show_on_menu_prev = getShowOnMenuByNodeId(previousRedFolder,foldersArray);
			if (iconEl.src.indexOf('icon?')==-1) {
				if (show_on_menu_prev=="1") {
					iconEl.src = foldersIcons[29];
				}
				else {
					iconEl.src = foldersIcons[9];
				}
			}
		}
		var iconEl = document.getElementById("foldersTreeicon" + nodeId);
		if (iconEl.src.indexOf('icon?')==-1) {
			if (show_on_menu=="1") {
				iconEl.src = foldersIcons[30];
			}
			else {
				iconEl.src = foldersIcons[21];
			}

		}
		previousRedFolder = nodeId;
		previousOpenFolder = objId;
	}

	function toggleTree(treeId,node,bottom,show_on_menu,host) {
		var divEl = document.getElementById(treeId + "div" + node);
		var joinEl = document.getElementById(treeId + "join" + node);
		var iconEl = document.getElementById(treeId + "icon" + node);
		var rootIconEl = document.getElementById(treeId + "rootIcon" + node);
		
		if (divEl!=null) {
			if (!host)
				getIcon(divEl.style.display,iconEl,show_on_menu);
			if (divEl.style.display == "none") {
				if (joinEl) {
					joinEl.src = foldersIcons[5]; // minus.gif

				}
				if (rootIconEl) {
					//on
					rootIconEl.src = foldersIcons[33];
				}
				divEl.style.display = "";
			}
			else {
				if (joinEl) {
						joinEl.src = foldersIcons[7]; // plus.gif
				}
				if (rootIconEl) {
					//off
					rootIconEl.src = foldersIcons[32];
				}
				divEl.style.display = "none";
			}
		}
		self.focus();
	}

	function getIcon(display,iconEl,show_on_menu) {
		if (iconEl.src.indexOf('icon?')==-1) {
			if (display == "none") {
				if (show_on_menu=="1") {
					iconEl.src = foldersIcons[28];
				}
				else {
					iconEl.src = foldersIcons[10];
				}
			}
			else {
				if (show_on_menu=="1") {
					iconEl.src = foldersIcons[29];
				}
				else {
					iconEl.src = foldersIcons[9];
				}
			}
		}
	}

	//Edit Objects Functions
	function editFile (parentId, objId, userId, referer, live, working, write) {
		var loc = '';
		//if (write=="1") {
			if (isInodeSet(parentId)) {
				loc += '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/edit_file" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&parent=' + parentId + "&inode=" + objId + '&userId=' + userId + '&referer=' + encodeURIComponent(referer);
			}
			else {
				loc += '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/edit_file" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&inode=' + objId + '&userId=' + userId + '&referer=' + encodeURIComponent(referer);
			}
			top.location = loc;
		//}
	}

	function editTemplate (objId, userId, referer, live, working, write) {
		var loc = '';
		loc += '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/templates/edit_template" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&inode=' + objId + '&referer=' + encodeURIComponent(referer);
		top.location = loc;
	}

	function editContainer (objId, userId, referer, live, working, write) {
		var loc = '';
		loc += '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/containers/edit_container" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&inode=' + objId + '&referer=' + encodeURIComponent(referer);
		top.location = loc;
	}

	function editContentlet (objId, userId, referer, live, working, write) {
		//if (write=="1") {
			var loc = '';
			loc += '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/contentlet/edit_contentlet" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&inode=' + objId + '&referer=' + encodeURIComponent(referer);
			top.location = loc;
		//}
	}
	
	function publishContentlet (objId, userId, referer, live, working, write) {
		//if (write=="1") {
			var loc = '';
			referer += "&selected_lang=" + getSelectedLanguageId();
			loc += '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/contentlet/edit_contentlet" /><portlet:param name="cmd" value="full_publish_list" /></portlet:actionURL>&publishInode=' + objId + '&referer=' + encodeURIComponent(referer);
			top.location = loc;
		//}
	}

	function unarchiveContentlet (objId, userId, referer, live, working, write) {
		//if (write=="1") {
			var loc = '';
			loc += '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/contentlet/edit_contentlet" /><portlet:param name="cmd" value="undelete" /></portlet:actionURL>&inode=' + objId + '&referer=' + encodeURIComponent(referer);
			top.location = loc;
		//}
	}

	function copyContentlet (objId, userId, referer, live, working, write) {
		//if (write=="1") {
			var loc = '';
			loc += '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/contentlet/edit_contentlet" /><portlet:param name="cmd" value="copy" /></portlet:actionURL>&inode=' + objId + '&referer=' + encodeURIComponent(referer);
			top.location = loc;
		//}
	}

	function unpublishContentlet (objId, userId, referer, live, working, write) {
		//if (write=="1") {
			var loc = '';
			referer += "&selected_lang=" + getSelectedLanguageId();
			loc += '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/contentlet/edit_contentlet" /><portlet:param name="cmd" value="unpublish" /></portlet:actionURL>&inode=' + objId + '&referer=' + encodeURIComponent(referer);
			top.location = loc;
		//}
	}

	function unlockContentlet (objId, userId, referer, live, working, write) {
		//if (write=="1") {
			var loc = '';
			loc += '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/contentlet/edit_contentlet" /><portlet:param name="cmd" value="unlock" /></portlet:actionURL>&inode=' + objId + '&referer=' + encodeURIComponent(referer);
			top.location = loc;
		//}
	}

	function fullDeleteContentlet (objId, userId, referer, live, working, write) {
		//if (write=="1") {
			var loc = '';
			loc += '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/contentlet/edit_contentlet" /><portlet:param name="cmd" value="full_delete" /></portlet:actionURL>&inode=' + objId + '&referer=' + encodeURIComponent(referer);
			top.location = loc;
		//}
	}

	function editPage (parentId, objId, userId, referer, live, working, write) {
		//if (write=="1") {
			var loc = '';
			loc += '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpages/preview_htmlpage" /><portlet:param name="previewPage" value="1" /></portlet:actionURL>&parent=' + parentId + "&inode=" + objId + '&referer=' + encodeURIComponent(referer);
			top.location = loc;
		//}
	}

	function editLink (parentId, objId, userId, referer, live, working, write) {
		//if (write=="1") {
			var loc = '';
			if (isInodeSet(parentId)) {
				loc += '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/edit_link" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&parent=' + parentId + "&inode=" + objId + '&referer=' + encodeURIComponent(referer);
			}
			else {
				loc += '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/edit_link" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&inode=' + objId + '&referer=' + encodeURIComponent(referer);
			}
			top.location = loc;
		//}
	}

	//Events

	function editEvent (objId, userId, referer, live, working, write) {
		//if (write=="1") {
			var loc = '';
			loc += '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/calendar/edit_event" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&inode=' + objId + '&referer=' + referer;
			top.location = loc;
		//}
	}

	function publishEvent (objId, userId, referer, live, working, write) {
		//if (write=="1") {
			var loc = '';
			loc += '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/calendar/edit_event" /><portlet:param name="cmd" value="full_publish_list" /></portlet:actionURL>&inode=' + objId + '&referer=' + referer;
			top.location = loc;
		//}
	}

	function unarchiveEvent (objId, userId, referer, live, working, write) {
		//if (write=="1") {
			var loc = '';
			loc += '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/calendar/edit_event" /><portlet:param name="cmd" value="undelete" /></portlet:actionURL>&inode=' + objId + '&referer=' + referer;
			top.location = loc;
		//}
	}

	function copyEvent (objId, userId, referer, live, working, write) {
		//if (write=="1") {
			var loc = '';
			loc += '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/calendar/edit_event" /><portlet:param name="cmd" value="copy" /></portlet:actionURL>&inode=' + objId + '&referer=' + referer;
			top.location = loc;
		//}
	}

	function unpublishEvent (objId, userId, referer, live, working, write) {
		//if (write=="1") {
			var loc = '';
			loc += '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/calendar/edit_event" /><portlet:param name="cmd" value="unpublish" /></portlet:actionURL>&inode=' + objId + '&referer=' + referer;
			top.location = loc;
		//}
	}

	function unlockEvent (objId, userId, referer, live, working, write) {
		//if (write=="1") {
			var loc = '';
			loc += '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/calendar/edit_event" /><portlet:param name="cmd" value="unlock" /></portlet:actionURL>&inode=' + objId + '&referer=' + referer;
			top.location = loc;
		//}
	}

	function fullDeleteEvent (objId, userId, referer, live, working, write) {
		//if (write=="1") {
			var loc = '';
			loc += '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/calendar/edit_event" /><portlet:param name="cmd" value="full_delete" /></portlet:actionURL>&inode=' + objId + '&referer=' + referer;
			top.location = loc;
		//}
	}


</script>

