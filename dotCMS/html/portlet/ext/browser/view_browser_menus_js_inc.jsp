<%@page import="com.liferay.portal.util.Constants"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="com.dotmarketing.util.Config" %>
<%@ page import="com.liferay.portal.language.LanguageUtil" %>
<%@page import="com.dotmarketing.portlets.contentlet.business.HostAPI"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.business.LayoutAPI" %>
<%@page import="com.dotmarketing.portlets.workflows.model.*"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="com.dotmarketing.portlets.structure.factories.StructureFactory"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.business.Permissionable"%>
<%@page import="com.dotmarketing.factories.InodeFactory"%>
<%@page import="com.dotmarketing.portlets.htmlpages.model.HTMLPage"%>
<%@page import="com.dotmarketing.business.Role"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.ContentletAPI"%>
<%@page import="com.dotmarketing.portlets.contentlet.struts.ContentletForm"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.*"%>
<%@page import="com.dotmarketing.business.UserAPI"%>
<%@page import="java.util.List"%>
<script src="/dwr/interface/UserAjax.js" type="text/javascript"></script>
<script language="JavaScript"><!--

	var userRoles;

	dojo.addOnLoad(function() {
		UserAjax.getUserRolesValues('<%=user.getUserId()%>', '<%=(String)session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID)%>', getUserRolesValuesCallBack);
	});


	function getUserRolesValuesCallBack(response) {
		userRoles = response;
	}

	// POPUPS


	function showHostPopUp(host, cmsAdminUser, origReferer, e) {
		var referer = encodeURIComponent(origReferer);
		if($('context_menu_popup_'+objId) == null) {
			var objId = host.identifier;
			var objInode = host.inode;
			var read = hasReadPermissions(host.permissions);
			var write = hasWritePermissions(host.permissions);
			var publish = hasPublishPermissions(host.permissions);
			var addChildren = hasAddChildrenPermissions(host.permissions);

			var strHTML = '';

			strHTML = '<div id="context_menu_popup_'+objId+'" class="contextPopupMenuBox">';

			if (write) {
				strHTML += '<a class="contextPopupMenu" href="javascript: editHost(\'' + objInode + '\',\''+referer+'\')">';
		    		strHTML += '<span class="hostIcon"></span>';
    				strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Edit-Host")) %>';
				strHTML += '</a>';
			}

			if (addChildren) {
				var containerperm=<%= APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("EXT_12", user)%>;
				var templateperm=<%= APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("EXT_13", user)%>;

				var isAdminUser = <%= APILocator.getUserAPI().isCMSAdmin(user)%>;

				if(isAdminUser || userRoles.folderModifiable) {
					strHTML += '<a class="contextPopupMenu" href="javascript: addTopFolder(\'' + objId + '\',\''+referer+'\')">';
				  	  	strHTML += '<span class="folderAddIcon"></span>';
		    			strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-Folder")) %>';
					strHTML += '</a>';
				}

				if(containerperm && (isAdminUser || userRoles.containerModifiable)){
					strHTML += '<a class="contextPopupMenu" href="javascript: addContainer(\''+referer+'\')">';
				    	strHTML += '<span class="container"></span>';
		    			strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-Container")) %>';
					strHTML += '</a>';
				}
				if(templateperm && (isAdminUser || userRoles.templateModifiable)){
					strHTML += '<a class="contextPopupMenu" href="javascript: addTemplate(\''+referer+'\')">';
			    		strHTML += '<span class="templateIcon"></span>';
	    				strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-Template")) %>';
					strHTML += '</a>'
				}

				if(isAdminUser || userRoles.fileModifiable) {
					strHTML += '<a class="contextPopupMenu" href="javascript:addFile(\'' + objId + '\',\'' + referer + '\',false);">';
					    strHTML += '<span class="fileNewIcon"></span>';
					    strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Image-or-File")) %>';
				    strHTML += '</a>';
				}


			}

			strHTML += '<div class="pop_divider" ></div>';
			strHTML += '<a class="contextPopupMenu" href="javascript: hidePopUp(\'context_menu_popup_'+objId+'\');">';
			strHTML += '<span class="closeIcon"></span>';
			strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Close")) %>';
			strHTML += '</a>';

			strHTML += '</div>';
			if(!document.getElementById('context_menu_popup_'+host.identifier))
				new Insertion.Bottom ('popups', strHTML);
		}

		showPopUp('context_menu_popup_'+objId, e);
	}

	function showFolderPopUp(folder, cmsAdminUser, origReferer, e) {

		var referer = encodeURIComponent(origReferer);

		var objId = folder.inode;


		if($('context_menu_popup_'+objId) == null) {
			var divHTML = '<div id="context_menu_popup_'+objId+'" class="contextPopupMenuBox" style="display:none;"></div>';
			new Insertion.Bottom ('popups', divHTML);
		}
		var div = $('context_menu_popup_'+objId);

		var read = hasReadPermissions(folder.permissions);
		var write = hasWritePermissions(folder.permissions);
		var publish = hasPublishPermissions(folder.permissions);
		var addChildren = hasAddChildrenPermissions(folder.permissions);

		var strHTML = '';

		if (write) {

			strHTML += '<a class="contextPopupMenu" href="javascript: editFolder(\'' + objId + '\',\''+referer+'\')">';
			    strHTML += '<span class="folderEditIcon"></span>';
	    		strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Edit-Folder")) %>';
			strHTML += '</a>';

			strHTML += '<a class="contextPopupMenu" href="javascript:deleteFolder(\'' + objId + '\' , \'' + referer + '\');">';
		    	strHTML += '<span class="folderDeleteIcon"></span>';
        		strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete")) %>';
			strHTML += '</a>';

			strHTML += '<a class="contextPopupMenu" href="javascript: publishFolder(\'' + objId + '\', \'' + referer + '\');">';
		    	strHTML += '<span class="folderGlobeIcon"></span>';
        		strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Publishall")) %>';
			strHTML += '</a>';

			strHTML += '<a href="javascript: markForCopy(\'' + objId + '\',\'' + referer +'\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="contextPopupMenu">';
		    	strHTML += '<span class="folderCopyIcon"></span>';
		        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Copy")) %>';
			strHTML += '</a>';

			strHTML += '<a href="javascript: markForCut(\'' + objId + '\',\'' + referer +'\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="contextPopupMenu">';
			    strHTML += '<span class="cutIcon"></span>';
			    strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Cut")) %>';
			strHTML += '</a>';

			strHTML += '<a id="' + objId + '-PasteREF" href="javascript: pasteToFolder(\'' + objId + '\',\'' + referer +'\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="contextPopupMenu">';
			    strHTML += '<span class="pasteIcon"></span>';
			    strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Paste")) %>';
			strHTML += '</a>';

			strHTML += '<div class="pop_divider" ></div>';

		}

		if(addChildren) {
			strHTML += '<a class="contextPopupMenu" id="contextChildMenu' + objId + 'REF" href="javascript:;">';
		    strHTML += '<div id="newArrowHack"></div>';
			strHTML += '<span class="plusIcon"></span>';
		    strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "New")) %>';
			strHTML += '</a>'

			strHTML += '<div class="pop_divider"></div>';
		}

		strHTML += '<a class="contextPopupMenu" href="javascript:hidePopUp(\'context_menu_popup_'+objId+'\');">';
		strHTML += '<span class="closeIcon"></span>';
		strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Close")) %>';
		strHTML += '</a>';


		Element.update ('context_menu_popup_'+objId, strHTML);

		if($('context_child_menu_popup_'+objId) == null) {
			var divHTML = '<div id="context_child_menu_popup_'+objId+'" class="contextPopupMenuBox" style="display:none;"></div>';
			new Insertion.Bottom ('popups', divHTML);
		}

		var strHTML = '';

		//"Add New" Sub Menu

		strHTML += '<a class="contextPopupMenu" href="javascript:addFolder(\'' + objId + '\',\''+referer+'\')">';
		    strHTML += '<span class="folderAddIcon"></span>';
		    strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Folder")) %>';
		strHTML += '</a>'

	    strHTML += '<a class="contextPopupMenu" href="javascript:addHTMLPage(\'' + objId + '\',\'' + referer + '\');" >';
		    strHTML += '<span class="newPageIcon"></span>';
		    strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "HTML-Page")) %>';
		strHTML += '</a>';

	    strHTML += '<a class="contextPopupMenu" href="javascript:addFile(\'' + objId + '\',\'' + referer + '\',false);">';
		    strHTML += '<span class="fileNewIcon"></span>';
		    strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Image-or-File")) %>';
		strHTML += '</a>';

	    strHTML += '<a class="contextPopupMenu" href="javascript:addFile(\'' + objId + '\',\'' + referer + '\',true);">';
		    strHTML += '<span class="fileMultiIcon"></span>';
		    strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Multiple-Files")) %>';
		strHTML += '</a>';

    	strHTML += '<a class="contextPopupMenu" href="javascript:addLink(\'' + objId + '\',\'' + referer + '\');">';
		    strHTML += '<span class="linkAddIcon"></span>';
    		strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Menu-Link")) %>';
		strHTML += '</a>';

		strHTML += '<div class="pop_divider" ></div>';

		strHTML += '<a class="contextPopupMenu" href="javascript:hidePopUp(\'context_child_menu_popup_'+objId+'\');">';
			strHTML += '<span class="closeIcon"></span>';
			strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Close")) %></a>';
		strHTML += '</a>';


		if($('contextChildMenu' + objId + 'REF') != null) {
			var event = "Event.observe('contextChildMenu" + objId + "REF', 'mouseup', function (event){ showChildPopUp('context_child_menu_popup_" + objId + "','context_menu_popup_"+objId+"', event) });";
			eval(event);
		}

		Element.update ('context_child_menu_popup_'+objId, strHTML);

		if (isInodeSet(markedForCut) || isInodeSet(markedForCopy)) {
			if($(objId + '-PasteREF') != null)
				Element.show(objId + '-PasteREF');
		} else {
			if($(objId + '-PasteREF') != null)
				Element.hide(objId + '-PasteREF');
		}

		showPopUp('context_menu_popup_'+objId, e);
	}

	// File Popup
	function showFilePopUp(file, cmsAdminUser, origReferer, e) {
		var workFlowAssign = false;
		var fileWfActionAssign = file.wfActionAssign;
		//var wfActions = file.wfActions;
		if(fileWfActionAssign == ""){
			fileWfActionAssign = null;
		}

		var objId = file.inode;
		var ident = file.identifier;
		var referer = encodeURIComponent(origReferer);

		if($('context_menu_popup_'+objId) == null) {
			var divHTML = '<div id="context_menu_popup_'+objId+'" class="contextPopupMenuBox"></div>';
			new Insertion.Bottom ('popups', divHTML);
		}
		if(fileWfActionAssign != null  && cmsAdminUser){
			workFlowAssign = true;
		}else if (fileWfActionAssign != null && !cmsAdminUser){
			workFlowAssign = true;
		}
		var div = $('context_menu_popup_'+objId);

		var read = hasReadPermissions(file.permissions);
		var write = hasWritePermissions(file.permissions);
		var publish = hasPublishPermissions(file.permissions);
		var live = file.live;
		var working = file.working;
		var archived = file.deleted;
		var locked = file.locked;
		var ext = file.extension;

		var strHTML = '';
		var contentletLanguageId = file.languageId;
		contentAdmin = new dotcms.dijit.contentlet.ContentAdmin(ident,objId,contentletLanguageId);

	  	if (read && !archived) {
			strHTML += '<a href="javascript: viewFile(\'' + ident + '\', \'' + ext + '\');" class="contextPopupMenu">';
		    	strHTML += '<span class="previewIcon"></span>';
		        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Open-Preview")) %>';
			strHTML += '</a>';
		}

		if ((live || working) && write && !archived) {
			if(file.isContent){
   				strHTML += '<a href="javascript: editFileAsset(\'' + objId + '\',\'' + file.fileAssetType + '\');" class="contextPopupMenu">';
   				strHTML += '<span class="editIcon"></span>';
				strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Edit")) %>';
			    strHTML += '</a>';
   			}else{
			    strHTML += '<a href="javascript: editFile(\'' + objId + '\',\'' + referer + '\');" class="contextPopupMenu">';
   				strHTML += '<span class="editIcon"></span>';
				strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Edit")) %>';
			    strHTML += '</a>';
   			}
		}


		for (var i = 0; i < file.wfActionMapList.length; i++) {
			var name = file.wfActionMapList[i].name;
			var id = file.wfActionMapList[i].id;
			var assignable = file.wfActionMapList[i].assignable;

			var commentable = file.wfActionMapList[i].commentable;
			console.log(name + ":"+ assignable + ":" + commentable);
			var icon = file.wfActionMapList[i].icon;
			var requiresCheckout = file.wfActionMapList[i].requiresCheckout;
			var wfActionNameStr = file.wfActionMapList[i].wfActionNameStr;
			var isLocked = file.isLocked;
			var contentEditable = file.contentEditable;
			if (!objId && requiresCheckout || (isLocked && contentEditable) && requiresCheckout) {
				strHTML += '<a href="javascript: contentAdmin.executeWfAction(\'' + id + '\', ' + assignable +', ' + commentable +', \'' + objId +'\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="contextPopupMenu">';
    			strHTML += '<span class=\''+icon+'\'></span>';
        		strHTML += wfActionNameStr;
				strHTML += '</a>';
			}else if(!requiresCheckout)  {
				strHTML += '<a href="javascript: contentAdmin.executeWfAction(\'' + id + '\', ' + assignable +', ' + commentable +', \'' + objId +'\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="contextPopupMenu">';
				strHTML += '<span class=\''+icon+'\'></span>';
    			strHTML += wfActionNameStr;
				strHTML += '</a>';
			}
		}


		if (working && publish && !archived ) {
			strHTML += '<a href="javascript: publishFile (\'' + objId + '\',\'' + referer + '\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="contextPopupMenu">';
    		if (live) {
            	strHTML += '<span class="republishIcon"></span>';
				strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Republish")) %>';
           } else {
            	strHTML += '<span class="publishIcon"></span>';
				strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Publish")) %>';
			}
			strHTML += '</a>';
		}

		if (live && publish ) {
			strHTML += '<a href="javascript: unpublishFile(\'' + objId + '\', \'' + referer +'\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="contextPopupMenu">';
	    		strHTML += '<span class="unpublishIcon"></span>';
	        	strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unpublish")) %>';
			strHTML += '</a>';
		}

		if (!live && working && publish ) {
			if (!archived) {
				strHTML += '<a href="javascript: archiveFile(\'' + objId + '\', \'' + referer +'\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="contextPopupMenu">';
	   				strHTML += '<span class="archiveIcon"></span>';
	            	strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Archive")) %>';
				strHTML += '</a>';
			}
			else {
				strHTML += '<a  href="javascript: unarchiveFile(\'' + objId + '\', \'' + referer +'\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="contextPopupMenu">';
   					strHTML += '<span class="unarchiveIcon"></span>';
               		strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unarchive")) %>';
				strHTML += '</a>';
			}
		}

		if (locked && write) {
			strHTML += '<a href="javascript: unlockFile(\'' + objId + '\', \'' + referer +'\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="contextPopupMenu">';
	    		strHTML += '<span class="keyIcon"></span>';
	        	strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unlock")) %>';
			strHTML += '</a>';
		}

		if (write && !archived)  {

			strHTML += '<div class="pop_divider" ></div>';

			strHTML += '<a href="javascript: markForCopy(\'' + objId + '\',\'' + referer +'\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="contextPopupMenu">';
	    		strHTML += '<span class="docCopyIcon"></span>';
	        	strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Copy")) %>';
			strHTML += '</a>';

			strHTML += '<a href="javascript: markForCut(\'' + objId + '\',\'' + referer +'\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="contextPopupMenu">';
		    	strHTML += '<span class="cutIcon"></span>';
		        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Cut")) %>';
			strHTML += '</a>';

		}

		if (write && archived)
		{
			strHTML += '<a href="javascript: deleteFile(\'' + objId + '\', \'' + referer +'\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="contextPopupMenu">';
		    	strHTML += '<span class="stopIcon"></span>';
	    	   	strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete-File")) %>';
			strHTML += '</a>';
		}

		strHTML += '<div class="pop_divider" ></div>';

		strHTML += '<a href="javascript:hidePopUp(\'context_menu_popup_'+objId+'\');" class="contextPopupMenu">';
			strHTML += '<span class="closeIcon"></span>';
			strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Close")) %></a>';
		strHTML += '</a>';

		Element.update(div, strHTML);

		showPopUp('context_menu_popup_'+objId, e);
	}

	//Link popup
	function showLinkPopUp(link, cmsAdminUser, origReferer, e) {

		var objId = link.inode;
		var referer = encodeURIComponent(origReferer);

		if($('context_menu_popup_'+objId) == null) {
			var divHTML = '<div id="context_menu_popup_'+objId+'" class="contextPopupMenuBox"></div>';
			new Insertion.Bottom ('popups', divHTML);
		}
		var div = $('context_menu_popup_'+objId);

		var read = hasReadPermissions(link.permissions);
		var write = hasWritePermissions(link.permissions);
		var publish = hasPublishPermissions(link.permissions);
		var live = link.live;
		var working = link.working;
		var archived = link.deleted;
		var locked = link.locked;

		var strHTML = '';

		if ((live || working) && read && !archived) {
			var actionLabel = write ? '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Edit")) %>' : '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "View")) %>';
			strHTML += '<a href="javascript: editLink(\'' + objId + '\', \'' + referer + '\')" class="contextPopupMenu">';
	    		strHTML += '<span class="editIcon"></span>';
	            strHTML += '' + actionLabel;
			strHTML += '</a>';
		}

		if (!live && working && publish) {
			if (!archived) {
				strHTML += '<a href="javascript: archiveLink(\'' + objId + '\', \'' + referer + '\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="contextPopupMenu">';
    				strHTML += '<span class="archiveIcon"></span>';
                	strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Archive")) %>';
				strHTML += '</a>';
			}
			else {
				strHTML += '<a href="javascript: unarchiveLink(\'' + objId + '\', \'' + referer + '\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="contextPopupMenu">';
    				strHTML += '<span class="unarchiveIcon"></span>';
                	strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unarchive")) %>';
				strHTML += '</a>';
			}
		}

		if (working && publish && !archived) {
			strHTML += '<a href="javascript: publishLink(\'' + objId + '\', \'' + referer + '\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="contextPopupMenu">';
    			if (live){
					strHTML += '<span class="republishIcon"></span>';
	            	strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Republish")) %>';
	            }else{
					strHTML += '<span class="unpublishIcon"></span>';
	            	strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Publish")) %>';
				}
			strHTML += '</a>';
		}

		if (live && publish) {
			strHTML += '<a href="javascript: unpublishLink(\'' + objId + '\', \'' + referer + '\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="contextPopupMenu">';
	    		strHTML += '<span class="unpublishIcon"></span>';
	        	strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unpublish")) %>';
			strHTML += '</a>';
		}

		if (locked && write) {
			strHTML += '<a href="javascript: unlockLink(\'' + objId + '\', \'' + referer + '\');  hidePopUp(\'context_menu_popup_'+objId+'\');" class="contextPopupMenu">';
		    	strHTML += '<span class="keyIcon"></span>';
		        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unlock")) %>';
			strHTML += '</a>';
		}


		if (write && !archived)  {

			strHTML += '<div class="pop_divider" ></div>';

			strHTML += '<a href="javascript: markForCopy(\'' + objId + '\',\'' + referer +'\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="contextPopupMenu">';
	    		strHTML += '<span class="docCopyIcon"></span>';
	        	strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Copy")) %>';
			strHTML += '</a>';

			strHTML += '<a href="javascript: markForCut(\'' + objId + '\',\'' + referer +'\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="contextPopupMenu">';
		    	strHTML += '<span class="cutIcon"></span>';
		        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Cut")) %>';
			strHTML += '</a>';

		}

		if (write && archived)
		{
			strHTML += '<a href="javascript: deleteLink(\'' + objId + '\', \'' + referer +'\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="contextPopupMenu">';
		    	strHTML += '<span class="stopIcon"></span>';
	    	   	strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete-Link")) %>';
			strHTML += '</a>';
		}

		strHTML += '<div class="pop_divider" ></div>';
		strHTML += '<a href="javascript:hidePopUp(\'context_menu_popup_'+objId+'\');" class="contextPopupMenu">';
			strHTML += '<span class="closeIcon"></span>';
			strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Close")) %>';
		strHTML += '</a>';

		Element.update(div, strHTML);

		showPopUp('context_menu_popup_'+objId, e);
	}

	function showHTMLPagePopUp(page, cmsAdminUser, origReferer, e) {

		var objId = page.inode;
		var referer = encodeURIComponent(origReferer);

		if($('context_menu_popup_'+objId) == null) {
			var divHTML = '<div id="context_menu_popup_'+objId+'" class="contextPopupMenuBox"></div>';
			new Insertion.Bottom ('popups', divHTML);
		}
		var div = $('context_menu_popup_'+objId);

		var read = hasReadPermissions(page.permissions);
		var write = hasWritePermissions(page.permissions);
		var publish = hasPublishPermissions(page.permissions);
		var live = page.live;
		var working = page.working;
		var archived = page.deleted;
		var locked = page.locked;

		var strHTML = '';

		if ((live || working) && read && !archived) {
			strHTML += '<a href="javascript: previewHTMLPage(\'' + objId + '\', \'' + referer + '\');" class="contextPopupMenu">';
		    	strHTML += '<span class="pageIcon"></span>';
	    	    strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Open-Preview")) %>';
			strHTML += '</a>';

			if ((live || working) && read)  {
				var actionLabel = write ? '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Open-Edit")) %>' : '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "View")) %>';

				strHTML += '<a href="javascript: editHTMLPage(\'' + objId + '\', \'' + referer + '\');" class="contextPopupMenu">';
    				strHTML += '<span class="pagePropIcon"></span>';
           			strHTML += actionLabel;
				strHTML += '</a>';
			}

			strHTML += '<a href="javascript: requestHTMLPageChange(\'' + objId + '\', \'' + referer + '\');" class="contextPopupMenu">';
		    	strHTML += '<span class="workflowIcon"></span>';
	   		    strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Request-a-Change")) %>';
			strHTML += '</a>';
		}

        if (!archived) {
	      strHTML += '<a href="javascript: viewHTMLPageStatistics(\'' + objId + '\', \'' + referer + '\');" class="contextPopupMenu">';
		      strHTML += '<span class="statisticsIcon"></span>';
		      strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "View-Statistics")) %>';
	      strHTML += '</a>';
	    }

		if (working && !archived && publish) {
			strHTML += '<a href="javascript: publishHTMLPage(\'' + objId + '\', \'' + referer + '\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="contextPopupMenu">';
	    	if(live) {
				strHTML += '<span class="republishIcon"></span>';
				strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Republish")) %>';
	   	    }
			else {
				strHTML += '<span class="publishIcon"></span>';
				strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Publish")) %>';
			}
			strHTML += '</a>';
		}

		if (live && publish) {
			strHTML += '<a href="javascript: unpublishHTMLPage(\'' + objId + '\', \'' + referer + '\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="contextPopupMenu">';
		    	strHTML += '<span class="unpublishIcon"></span>';
		        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unpublish")) %>';
			strHTML += '</a>';
		}

		if (!live && working && publish) {
			if (!archived) {
				strHTML += '<a href="javascript: archiveHTMLPage(\'' + objId + '\', \'' + referer + '\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="contextPopupMenu">';
		   			strHTML += '<span class="archiveIcon"></span>';
    	          	strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Archive")) %>';
				strHTML += '</a>';
			}
			else {
				strHTML += '<a href="javascript: unarchiveHTMLPage(\'' + objId + '\', \'' + referer + '\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="contextPopupMenu">';
		   			strHTML += '<span class="unarchiveIcon"></span>';
    		        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unarchive")) %>';
				strHTML += '</a>';
			}
		}

		if (locked && write) {
			strHTML += '<a href="javascript: unlockHTMLPage(\'' + objId + '\', \'' + referer + '\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="contextPopupMenu">';
		    	strHTML += '<span class="keyIcon"></span>';
		        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unlock")) %>';
			strHTML += '</a>';
		}


		if (write && !archived)  {
			strHTML += '<div class="pop_divider" ></div>';

			strHTML += '<a href="javascript: markForCopy(\'' + objId + '\',\'' + referer +'\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="contextPopupMenu">';
	    		strHTML += '<span class="docCopyIcon"></span>';
	        	strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Copy")) %>';
			strHTML += '</a>';

			strHTML += '<a href="javascript: markForCut(\'' + objId + '\',\'' + referer +'\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="contextPopupMenu">';
		    	strHTML += '<span class="cutIcon"></span>';
		        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Cut")) %>';
			strHTML += '</a>';

		}

		if (write && archived)
		{
			strHTML += '<a href="javascript: deleteHTMLPage(\'' + objId + '\', \'' + referer +'\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="contextPopupMenu">';
		    	strHTML += '<span class="stopIcon"></span>';
	    	   	strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete-Page")) %>';
			strHTML += '</a>';
		}

		strHTML += '<div class="pop_divider" ></div>';
		strHTML += '<a href="javascript: hidePopUp(\'context_menu_popup_'+objId+'\');" class="contextPopupMenu">';
			strHTML += '<span class="closeIcon"></span>';
			strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Close")) %>';
		strHTML += '</a>';

		Element.update(div, strHTML);

		showPopUp('context_menu_popup_'+objId, e);
	}

	//popups functions
	var currentMenuId = "";
	var currentChildMenuId = "";
	document.oncontextmenu = nothing;

	function showPopUp(id, e) {

		var mousePosX = Event.pointerX(e);
		var mousePosY = Event.pointerY(e);

		hidePopUp(currentMenuId);

		currentMenuId = id;

		var popup = $(id);

		var windowHeight = top.document.body.clientHeight;

		var popupHeight = Element.getHeight(popup);

		var noPx = document.childNodes ? 'px' : 0;
		var myReference = popup;
		if( myReference.style ) {
			myReference = popup.style;
		}

		myReference.left = ( mousePosX - 10 ) + noPx;

	    if((mousePosY + popupHeight) >= windowHeight && windowHeight > popupHeight)
			myReference.top = ( mousePosY - popupHeight ) + noPx;
		else
			myReference.top = ( mousePosY ) + noPx;

     	showDebugMessage('showPopUp showing');
		Element.show (id);
	}

	function showChildPopUp(id,parentId,e) {

		var parentEl = $(parentId);

		var mousePosX = Event.pointerX(e);
		var mousePosY = Event.pointerY(e);

		currentChildMenuId = id;

		var popup = $(id);

		var windowHeight = top.document.body.clientHeight;

		var popupHeight = Element.getHeight(popup);

		var noPx = document.childNodes ? 'px' : 0;
		var myReference = popup;
		if( myReference.style ) {
			myReference = popup.style;
		}

		myReference.left = ( mousePosX - 5 ) + noPx;

	    if((mousePosY + popupHeight) >= windowHeight && windowHeight > popupHeight)
			myReference.top = ( mousePosY - popupHeight ) + noPx;
		else
			myReference.top = ( mousePosY ) + noPx;

     	showDebugMessage('showChildPopUp showing');
		Element.show (id);

	}

	function hidePopUp(id) {
		if ($(id) != null) {
			if($(currentChildMenuId) != null && id == currentMenuId) {
				Element.hide (currentChildMenuId);
				currentChildMenuId = "";
			}
			Element.hide (id);
			currentMenuId = "";
		}
	}


	function getScrollX() {
	  var scrOfX = 0, scrOfY = 0;
	  if( typeof( window.pageYOffset ) == 'number' ) {
	    //Netscape compliant
	    scrOfX = top.window.pageXOffset;
	  } else if( document.body.scrollLeft ) {
	    //DOM compliant
	    scrOfX = top.document.body.scrollLeft;
	  } else if( document.documentElement && ( document.documentElement.scrollLeft || document.documentElement.scrollTop ) ) {
	    //IE6 standards compliant mode
	    scrOfX = top.document.documentElement.scrollLeft;
	  }
	  return scrOfX;
	}

	function getScrollY() {
	  var scrOfY = 0;
	  if( typeof( window.pageYOffset ) == 'number' ) {
	    //Netscape compliant
	    scrOfY = top.window.pageYOffset;
	  } else if(top.document.body.scrollTop) {
	    //DOM compliant
	    scrOfY = top.document.body.scrollTop;
	  } else if( document.documentElement && ( document.documentElement.scrollLeft || document.documentElement.scrollTop ) ) {
	    //IE6 standards compliant mode
	    scrOfY = top.document.documentElement.scrollTop;
	  }
	  return scrOfY;
	}

	function renderAddNewDropDownButton(activeHost, selectedFolder, origReferer) {
		if (dijit.byId('addNewButton')) {
			dijit.byId('addNewButton').destroy();
		}


		if(selectedFolder===activeHost){
			selectedFolder = "";
		}

		var htmlCode = '<button dojoType="dijit.form.ComboButton" id="addNewButton" iconClass="plusIcon" title="<%= LanguageUtil.get(pageContext, "Add-New") %>">';
		htmlCode += '	<span><%= LanguageUtil.get(pageContext, "Add-New") %></span>';
		htmlCode += '	<div dojoType="dijit.Menu" style="display: none;">';
		if (selectedFolder == "") {
			var host = inodes[activeHost];
			var addChildren = hasAddChildrenPermissions(host.permissions);
			if (addChildren) {
				var objId = host.identifier;
				var referer = unescape(encodeURIComponent(origReferer));
				var containerperm = <%= APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("EXT_12", user)%>;
				var templateperm = <%= APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("EXT_13", user)%>;

				htmlCode += '<div dojoType="dijit.MenuItem" onclick="addTopFolder(\'' + objId + '\', \'' + referer + '\')">';
				htmlCode += '<span class="folderAddIcon"></span>&nbsp;';
				htmlCode += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-Folder")) %>';
				htmlCode += '</div>';

				if(containerperm){
					htmlCode += '<div dojoType="dijit.MenuItem" onclick="addContainer(\'' + referer + '\')">';
					htmlCode += '<span class="container"></span>&nbsp;';
					htmlCode += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-Container")) %>';
					htmlCode += '</div>';
				}

				if(templateperm){
					htmlCode += '<div dojoType="dijit.MenuItem" onclick="addTemplate(\'' + referer + '\')">';
					htmlCode += '<span class="templateIcon"></span>&nbsp;';
					htmlCode += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-Template")) %>';
					htmlCode += '</div>';
				}

				htmlCode += '<div dojoType="dijit.MenuItem" onclick="addFile(\'' + objId + '\',\'' + referer + '\',false)">';
				htmlCode += '<span class="fileNewIcon"></span>&nbsp;';
				htmlCode += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Image-or-File")) %>';
				htmlCode += '</div>';

			}
		} else {
			var folder = inodes[selectedFolder];
			var addChildren = hasAddChildrenPermissions(folder.permissions);
			if (addChildren) {
				var referer = unescape(encodeURIComponent(origReferer));

				htmlCode += '<div dojoType="dijit.MenuItem" onclick="addFolder(\'' + selectedFolder + '\', \'' + referer + '\')">';
				htmlCode += '<span class="folderAddIcon"></span>&nbsp;';
				htmlCode += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Folder")) %>';
				htmlCode += '</div>';

				htmlCode += '<div dojoType="dijit.MenuItem" onclick="addHTMLPage(\'' + selectedFolder + '\',\'' + referer + '\')">';
				htmlCode += '<span class="newPageIcon"></span>&nbsp;';
				htmlCode += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "HTML-Page")) %>';
				htmlCode += '</div>';

				htmlCode += '<div dojoType="dijit.MenuItem" onclick="addFile(\'' + selectedFolder + '\',\'' + referer + '\',false)">';
				htmlCode += '<span class="fileNewIcon"></span>&nbsp;';
				htmlCode += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Image-or-File")) %>';
				htmlCode += '</div>';

				htmlCode += '<div dojoType="dijit.MenuItem" onclick="addFile(\'' + selectedFolder + '\',\'' + referer + '\',true)">';
				htmlCode += '<span class="fileMultiIcon"></span>&nbsp;';
				htmlCode += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Multiple-Files")) %>';
				htmlCode += '</div>';

				htmlCode += '<div dojoType="dijit.MenuItem" onclick="addLink(\'' + selectedFolder + '\',\'' + referer + '\')">';
				htmlCode += '<span class="linkAddIcon"></span>&nbsp;';
				htmlCode += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Menu-Link")) %>';
				htmlCode += '</div>';
			}
		}

		htmlCode += '	</div>';
		htmlCode += '</button>';

		dojo.byId("addNewDropDownButtonDiv").innerHTML = htmlCode;
		dojo.parser.parse("addNewDropDownButtonDiv");
	}

	//*************************************
    //
    //
    //  ContentAdmin Obj
    //
    //
    //*************************************




    dojo.declare("dotcms.dijit.contentlet.ContentAdmin", null, {
    	contentletIdentifier : "",
    	contentletInode : "",
    	languageID : "",
    	wfActionId:"",
    	constructor : function(contentletIdentifier, contentletInode,languageId ) {
    		this.contentletIdentifier = contentletIdentifier;
    		this.contentletInode =contentletInode;
    		this.languageId=languageId;


    	},


    	executeWfAction: function(wfId, assignable, commentable, inode ){
    		this.wfActionId=wfId;

    		//if(assignable || commentable){
    			var dia = dijit.byId("contentletWfDialog");
    			if(dia){
    				dia.destroyRecursive();

    			}
    			dia = new dijit.Dialog({
    				id			:	"contentletWfDialog",
    				title		: 	"<%=LanguageUtil.get(pageContext, "Workflow-Actions")%>",
					style		:	"width:500px;height:300px;"
    				});



    			var myCp = dijit.byId("contentletWfCP");
    			if (myCp) {
    				myCp.destroyRecursive(true);
    			}

    			myCp = new dojox.layout.ContentPane({
    				id 			: "contentletWfCP",
    				style		:	"width:500px;height:300px;margin:auto;"
    			}).placeAt("contentletWfDialog");

    			dia.show();
    			myCp.attr("href", "/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfTaskAjax?cmd=renderAction&actionId=" + wfId + "&inode=" + inode);
    			return;
    		//}
    		//else{
        		//dojo.byId("wfActionId").value=wfId;
        		//saveContent(false);
    		//}

    	},

    	saveAssign : function(){


    		var assignRole = (dijit.byId("taskAssignmentAux"))
			? dijit.byId("taskAssignmentAux").getValue()
				: (dojo.byId("taskAssignmentAux"))
					? dojo.byId("taskAssignmentAux").value
							: "";

			if(!assignRole || assignRole.length ==0 ){
				showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Assign-To-Required")%>");
				return;
			}

			var comments = (dijit.byId("taskCommentsAux"))
				? dijit.byId("taskCommentsAux").getValue()
					: (dojo.byId("taskCommentsAux"))
						? dojo.byId("taskCommentsAux").value
								: "";

    		var wfActionAssign 		= assignRole;
    		var selectedItem 		= "";
    		var wfConId 			= dojo.byId("wfConId").value;
    		var wfActionId 			= this.wfActionId;
    		var wfActionComments 	= comments;

    		var dia = dijit.byId("contentletWfDialog").hide();

    		BrowserAjax.saveFileAction(selectedItem,wfActionAssign,wfActionId,wfActionComments,wfConId, fileActionCallback);

    	}

    });

    function fileActionCallback (response) {
    	reloadContent ();
		showDotCMSErrorMessage('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Perform-Workflow")) %>');
	}

    var contentAdmin ;




--></script>
