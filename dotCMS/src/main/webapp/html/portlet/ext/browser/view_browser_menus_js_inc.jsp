<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@page import="com.dotcms.enterprise.license.LicenseLevel"%>
<%@page import="com.liferay.portal.util.Constants"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="com.dotmarketing.util.Config" %>
<%@ page import="com.liferay.portal.language.LanguageUtil" %>
<%@ page import="com.liferay.portal.util.WebKeys"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.HostAPI"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.business.LayoutAPI" %>
<%@page import="com.dotmarketing.portlets.workflows.model.*"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="com.dotmarketing.portlets.structure.factories.StructureFactory"%>
<%@page import="com.dotmarketing.business.CacheLocator"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.business.Permissionable"%>
<%@page import="com.dotmarketing.factories.InodeFactory"%>
<%@page import="com.dotmarketing.business.Role"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.ContentletAPI"%>
<%@page import="com.dotmarketing.portlets.contentlet.struts.ContentletForm"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.*"%>
<%@page import="com.dotmarketing.business.UserAPI"%>
<%@page import="java.util.List"%>

<%@ page import="com.dotcms.publisher.endpoint.bean.PublishingEndPoint"%>
<%@ page import="com.dotcms.publisher.endpoint.business.PublishingEndPointAPI"%>


<script src="/dwr/interface/UserAjax.js" type="text/javascript"></script>
<script language="JavaScript"><!--

	var userRoles;

	dojo.addOnLoad(function() {
		UserAjax.getUserRolesValues('<%=user.getUserId()%>', '<%=(String)session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID)%>', getUserRolesValuesCallBack);
	});

	var enterprise = <%=LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level%>;

	<%PublishingEndPointAPI pepAPI = APILocator.getPublisherEndPointAPI();
		List<PublishingEndPoint> sendingEndpoints = pepAPI.getReceivingEndPoints();%>
	var sendingEndpoints = <%=UtilMethods.isSet(sendingEndpoints) && !sendingEndpoints.isEmpty()%>;

	<%
	Structure fileStructure = CacheLocator.getContentTypeCache().getStructureByVelocityVarName("FileAsset");
	List<WorkflowScheme> fileWorkflows = APILocator.getWorkflowAPI().findSchemesForStruct(fileStructure);

	String frameName = (String)request.getSession().getAttribute(WebKeys.FRAME);
	
	%>




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

			strHTML = '<div id="context_menu_popup_'+objId+'" class="context-menu">';

			if (write) {
				strHTML += '<a class="context-menu__item" href="javascript: editHost(\'' + objInode + '\',\''+referer+'\')">';
		    		strHTML += '<span class="hostIcon"></span>';
    				strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Edit-Host")) %>';
				strHTML += '</a>';

                if (enterprise) {
                    if (sendingEndpoints) {
                        strHTML += '<a class="context-menu__item" href="javascript: remotePublish(\'' + objId + '\', \'' + referer + '\'); hidePopUp(\'context_menu_popup_'+objId+'\');">';
                            strHTML += '<span class="sServerIcon"></span>';
                            strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Remote-Publish")) %>';
                        strHTML += '</a>';
                    }

					strHTML += '<a class="context-menu__item" href="javascript: addToBundle(\'' + objId + '\', \'' + referer + '\'); hidePopUp(\'context_menu_popup_'+objId+'\');">';
						strHTML += '<span class="bundleIcon"></span>';
						strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-To-Bundle")) %>';
					strHTML += '</a>';
				}
			}



			if (addChildren) {

				var isAdminUser = <%= APILocator.getUserAPI().isCMSAdmin(user)%>;

				if(isAdminUser || userRoles.folderModifiable) {
					strHTML += '<a class="context-menu__item" href="javascript: addTopFolder(\'' + objId + '\',\''+referer+'\')">';
				  	  	strHTML += '<span class="folderAddIcon"></span>';
		    			strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-Folder")) %>';
					strHTML += '</a>';
				}

				
				if(isAdminUser || userRoles.fileModifiable) {
					
					
					strHTML += '<a class="context-menu__item" href="javascript: addHTMLPage(\'' + objId + '\',\'' + referer + '\')">';
					//strHTML += '<span class="newPageIcon"></span>&nbsp;';
					strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "HTML-Page")) %>';
					strHTML += '</a>';
					
					
					
					
					
					strHTML += '<a class="context-menu__item" href="javascript:addFile(\'' + objId + '\',\'' + referer + '\',false);hidePopUp(\'context_menu_popup_'+objId+'\');">';
					    strHTML += '<span class="fileNewIcon"></span>';
					    strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Image-or-File")) %>';
				    strHTML += '</a>';

				    strHTML += '<a class="context-menu__item" href="javascript:addFile(\'' + objId + '\',\'' + referer + '\',true);hidePopUp(\'context_menu_popup_'+objId+'\');">';
                    strHTML += '<span class="fileNewIcon"></span>';
                    strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Multiple-Files")) %>';
                    strHTML += '</a>';
				}
			}

            if (write) {
                strHTML += '<div class="pop_divider" ></div>';

                strHTML += '<a id="' + objId + '-PasteREF" href="javascript: pasteToFolder(\'' + objId + '\',\'' + referer +'\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="context-menu__item">';
                strHTML += '<span class="pasteIcon"></span>';
                strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Paste")) %>';
                strHTML += '</a>';
            }

			strHTML += '<div class="pop_divider" ></div>';
			strHTML += '<a class="context-menu__item" href="javascript: hidePopUp(\'context_menu_popup_'+objId+'\');">';
			strHTML += '<span class="closeIcon"></span>';
			strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Close")) %>';
			strHTML += '</a>';

			strHTML += '</div>';
			if(!document.getElementById('context_menu_popup_'+host.identifier))
				new Insertion.Bottom ('popups', strHTML);

            if (isInodeSet(markedForCut) || isInodeSet(markedForCopy)) {
                if ($(objId + '-PasteREF') != null) {

                    var asset;
                    if (isInodeSet(markedForCut)) {
                        asset = inodes[markedForCut];
                    } else {
                        asset = inodes[markedForCopy];
                    }

                    if (asset.type == 'folder' || asset.type == 'file_asset' || asset.type == 'htmlpage') {
                        Element.show(objId + '-PasteREF');
                    } else {
                        Element.hide(objId + '-PasteREF');
                    }
                }
            } else {
                if ($(objId + '-PasteREF') != null) {
                    Element.hide(objId + '-PasteREF');
                }
            }
		}

		showPopUp('context_menu_popup_'+objId, e);
	}

	function showFolderPopUp(folder, cmsAdminUser, origReferer, e) {
		if(actionLoading) return;
		
		var referer = encodeURIComponent(origReferer);

		var objId = folder.inode;


		if($('context_menu_popup_'+objId) == null) {
			var divHTML = '<div id="context_menu_popup_'+objId+'" class="context-menu" style="display:none;"></div>';
			new Insertion.Bottom ('popups', divHTML);
		}
		var div = $('context_menu_popup_'+objId);

		var read = hasReadPermissions(folder.permissions);
		var write = hasWritePermissions(folder.permissions);
		var publish = hasPublishPermissions(folder.permissions);
		var addChildren = hasAddChildrenPermissions(folder.permissions);

		var strHTML = '';

		if (write) {

			strHTML += '<a class="context-menu__item" href="javascript: editFolder(\'' + objId + '\',\''+referer+'\')">';
			    //strHTML += '<span class="folderEditIcon"></span>';
	    		strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Edit-Folder")) %>';
			strHTML += '</a>';

			strHTML += '<a class="context-menu__item" href="javascript:deleteFolder(\'' + objId + '\' , \'' + referer + '\');">';
		    	//strHTML += '<span class="folderDeleteIcon"></span>';
        		strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete")) %>';
			strHTML += '</a>';

			strHTML += '<a class="context-menu__item" href="javascript: publishFolder(\'' + objId + '\', \'' + referer + '\');">';
		    	//strHTML += '<span class="folderGlobeIcon"></span>';
        		strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Publishall")) %>';
			strHTML += '</a>';

            if (enterprise) {
                if (sendingEndpoints) {
                    strHTML += '<a class="context-menu__item" href="javascript: remotePublish(\'' + objId + '\', \'' + referer + '\'); hidePopUp(\'context_menu_popup_'+objId+'\');">';
                        //strHTML += '<span class="sServerIcon"></span>';
                        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Remote-Publish")) %>';
                    strHTML += '</a>';
                }

				strHTML += '<a class="context-menu__item" href="javascript: addToBundle(\'' + objId + '\', \'' + referer + '\'); hidePopUp(\'context_menu_popup_'+objId+'\');">';
					//strHTML += '<span class="bundleIcon"></span>';
					strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-To-Bundle")) %>';
				strHTML += '</a>';
			}

			strHTML += '<a href="javascript: markForCopy(\'' + objId + '\',\'' + referer +'\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="context-menu__item">';
		    	//strHTML += '<span class="folderCopyIcon"></span>';
		        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "markForCopy")) %>';
			strHTML += '</a>';

			strHTML += '<a href="javascript: markForCut(\'' + objId + '\',\'' + referer +'\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="context-menu__item">';
			    //strHTML += '<span class="cutIcon"></span>';
			    strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Cut")) %>';
			strHTML += '</a>';

			strHTML += '<a id="' + objId + '-PasteREF" href="javascript: pasteToFolder(\'' + objId + '\',\'' + referer +'\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="context-menu__item">';
			    //strHTML += '<span class="pasteIcon"></span>';
			    strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Paste")) %>';
			strHTML += '</a>';

			strHTML += '<div class="pop_divider" ></div>';
		}

		if(addChildren) {
			strHTML += '<a class="context-menu__item" id="contextChildMenu' + objId + 'REF" href="javascript:;">';
		    strHTML += '<div class="arrowRightIcon"></div>';
			//strHTML += '<span class="plusIcon"></span>';
		    strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "New")) %>';
			strHTML += '</a>'

			strHTML += '<div class="pop_divider"></div>';
		}

		strHTML += '<a class="context-menu__item" href="javascript:hidePopUp(\'context_menu_popup_'+objId+'\');">';
		//strHTML += '<span class="closeIcon"></span>';
		strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Close")) %>';
		strHTML += '</a>';


		Element.update ('context_menu_popup_'+objId, strHTML);

		if($('context_child_menu_popup_'+objId) == null) {
			var divHTML = '<div id="context_child_menu_popup_'+objId+'" class="context-menu" style="display:none;"></div>';
			new Insertion.Bottom ('popups', divHTML);
		}

		var strHTML = '';

		//"Add New" Sub Menu

		strHTML += '<a class="context-menu__item" href="javascript:addFolder(\'' + objId + '\',\''+referer+'\')">';
		    //strHTML += '<span class="folderAddIcon"></span>';
		    strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Folder")) %>';
		strHTML += '</a>'

	    strHTML += '<a class="context-menu__item" href="javascript:addHTMLPage(\'' + objId + '\',\'' + referer + '\');">';
		    //strHTML += '<span class="newPageIcon"></span>';
		    strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "HTML-Page")) %>';
		strHTML += '</a>';

	    strHTML += '<a class="context-menu__item" href="javascript:addFile(\'' + objId + '\',\'' + referer + '\',false);hidePopUp(\'context_menu_popup_'+objId+'\');hidePopUp(\'context_child_menu_popup_'+objId+'\');">';
		    //strHTML += '<span class="fileNewIcon"></span>';
		    strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Image-or-File")) %>';
		strHTML += '</a>';

	    strHTML += '<a class="context-menu__item" href="javascript:addFile(\'' + objId + '\',\'' + referer + '\',true);hidePopUp(\'context_menu_popup_'+objId+'\');hidePopUp(\'context_child_menu_popup_'+objId+'\');">';
		    //strHTML += '<span class="fileMultiIcon"></span>';
		    strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Multiple-Files")) %>';
		strHTML += '</a>';

    	strHTML += '<a class="context-menu__item" href="javascript:addLink(\'' + objId + '\',\'' + referer + '\');">';
		    //strHTML += '<span class="linkAddIcon"></span>';
    		strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Menu-Link")) %>';
		strHTML += '</a>';

		strHTML += '<div class="pop_divider" ></div>';

		strHTML += '<a class="context-menu__item" href="javascript:hidePopUp(\'context_child_menu_popup_'+objId+'\');">';
			//strHTML += '<span class="closeIcon"></span>';
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
	
	function wfActionsMenu(objId, content) {
		contentAdmin = new dotcms.dijit.contentlet.ContentAdmin('','',1);
		var strHTML = "";
		if(content.wfActionMapList){
			for (var i = 0; i < content.wfActionMapList.length; i++) {
            	var name = content.wfActionMapList[i].name;
            	var id = content.wfActionMapList[i].id;
            	var assignable = content.wfActionMapList[i].assignable;
            	var hasPushPublishActionlet = content.wfActionMapList[i].hasPushPublishActionlet;
	            var commentable = content.wfActionMapList[i].commentable;
	            var icon = content.wfActionMapList[i].icon;
	            var requiresCheckout = content.wfActionMapList[i].requiresCheckout;
	            var wfActionNameStr = content.wfActionMapList[i].wfActionNameStr;
	            var isLocked = content.isLocked;
	            var contentEditable = content.contentEditable;

                strHTML += '<a href="javascript: contentAdmin.executeWfAction(\'' + id + '\', ' + assignable +', ' + commentable+', ' +hasPushPublishActionlet +', \'' + objId +'\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="context-menu__item">';
                strHTML += '<span class=\''+icon+'\'></span>';
                strHTML += wfActionNameStr;
                strHTML += '</a>';

	        }
	       }
		return strHTML;
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
			var divHTML = '<div id="context_menu_popup_'+objId+'" class="context-menu"></div>';
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
		var hasLiveVersion = file.hasLiveVersion;

		var strHTML = '';
		var contentletLanguageId = file.languageId;
		contentAdmin = new dotcms.dijit.contentlet.ContentAdmin(ident,objId,contentletLanguageId);

	  	if (read && !archived) {
			strHTML += '<a href="javascript: viewFile(\'' + objId + '\', \'' + ext + '\');" class="context-menu__item">';
		    	//strHTML += '<span class="previewIcon"></span>';
		        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Open-Preview")) %>';
			strHTML += '</a>';
		}

		if ((live || working) && write && !archived) {
			if(file.isContent){
   				strHTML += '<a href="javascript: editFileAsset(\'' + objId + '\',\'' + file.fileAssetType + '\');" class="context-menu__item">';
   				//strHTML += '<span class="editIcon"></span>';
				strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Edit")) %>';
			    strHTML += '</a>';
   			}else{
			    strHTML += '<a href="javascript: editFile(\'' + objId + '\',\'' + referer + '\');" class="context-menu__item">';
   				strHTML += '<span class="editIcon"></span>';
				strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Edit")) %>';
			    strHTML += '</a>';
   			}
		}

		strHTML += wfActionsMenu(objId, file);
		

		if (working && publish && !archived ) {
	
	        if (enterprise) {
	             if (sendingEndpoints) {
	                strHTML += '<a class="context-menu__item" href="javascript: remotePublish(\'' + objId + '\', \'' + referer + '\'); hidePopUp(\'context_menu_popup_'+objId+'\');">';
	                strHTML += '<span class="sServerIcon"></span>';
	                strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Remote-Publish")) %>';
	                strHTML += '</a>';
	            }
	
				strHTML += '<a class="context-menu__item" href="javascript: addToBundle(\'' + objId + '\', \'' + referer + '\'); hidePopUp(\'context_menu_popup_'+objId+'\');">';
				strHTML += '<span class="bundleIcon"></span>';
				strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-To-Bundle")) %>';
				strHTML += '</a>';
			}
		}
			
		// If archived, only display the "Remove" option in the Push Dialog
		if (archived) {
			if (enterprise) {
                if (sendingEndpoints) {
                    strHTML += '<a class="context-menu__item" href="javascript: remotePublish(\'' + objId + '\', \'' + referer + '\', true); hidePopUp(\'context_menu_popup_'+objId+'\');">';
                    strHTML += '<span class="sServerIcon"></span>';
                    strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Remote-Publish")) %>';
                    strHTML += '</a>';
                }
			}
		}

	
		if (locked && write) {
			strHTML += '<a href="javascript: unlockFile(\'' + objId + '\', \'' + referer +'\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="context-menu__item">';
		    strHTML += '<span class="keyIcon"></span>';
		    strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unlock")) %>';
			strHTML += '</a>';
		}


		
		if (write && !archived)  {
			
			strHTML += '<div class="pop_divider" ></div>';

            strHTML += '<a href="javascript: markForCopy(\'' + objId + '\',\'' + referer +'\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="context-menu__item">';
            strHTML += '<span class="docCopyIcon"></span>';
            strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "markForCopy")) %>';
            strHTML += '</a>';

			strHTML += '<a href="javascript: markForCut(\'' + objId + '\',\'' + referer +'\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="context-menu__item">';
		    	strHTML += '<span class="cutIcon"></span>';
		        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Cut")) %>';
			strHTML += '</a>';

		}

		strHTML += '<div class="pop_divider" ></div>';

		strHTML += '<a href="javascript:hidePopUp(\'context_menu_popup_'+objId+'\');" class="context-menu__item">';
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
			var divHTML = '<div id="context_menu_popup_'+objId+'" class="context-menu"></div>';
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
		var hasLiveVersion = link.hasLiveVersion;

		var strHTML = '';

		if ((live || working) && read && !archived) {
			var actionLabel = write ? '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Edit")) %>' : '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "View")) %>';
			strHTML += '<a href="javascript: editLink(\'' + objId + '\', \'' + referer + '\')" class="context-menu__item">';
	    		strHTML += '<span class="editIcon"></span>';
	            strHTML += '' + actionLabel;
			strHTML += '</a>';
		}

		// If archived, only display the "Remove" option in the Push Dialog
        if (archived) {
            if (enterprise) {
                if (sendingEndpoints) {
                    strHTML += '<a class="context-menu__item" href="javascript: remotePublish(\'' + objId + '\', \'' + referer + '\', true); hidePopUp(\'context_menu_popup_'+objId+'\');">';
                        //strHTML += '<span class="sServerIcon"></span>';
                        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Remote-Publish")) %>';
                    strHTML += '</a>';
                }
            }
        }
        
		if (!live && working && publish && !hasLiveVersion) {
			if (!archived) {
				strHTML += '<a href="javascript: archiveLink(\'' + objId + '\', \'' + referer + '\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="context-menu__item">';
    				strHTML += '<span class="archiveIcon"></span>';
                	strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Archive")) %>';
				strHTML += '</a>';
			}
			else {
				strHTML += '<a href="javascript: unarchiveLink(\'' + objId + '\', \'' + referer + '\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="context-menu__item">';
    				strHTML += '<span class="unarchiveIcon"></span>';
                	strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unarchive")) %>';
				strHTML += '</a>';
			}
		}

		if (working && publish && !archived) {
			strHTML += '<a href="javascript: publishLink(\'' + objId + '\', \'' + referer + '\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="context-menu__item">';
    			if (live){
					strHTML += '<span class="republishIcon"></span>';
	            	strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Republish")) %>';
	            }else{
					strHTML += '<span class="publishIcon"></span>';
	            	strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Publish")) %>';
				}
			strHTML += '</a>';

            if (enterprise) {
                if (sendingEndpoints) {
                    strHTML += '<a class="context-menu__item" href="javascript: remotePublish(\'' + objId + '\', \'' + referer + '\'); hidePopUp(\'context_menu_popup_'+objId+'\');">';
                        strHTML += '<span class="sServerIcon"></span>';
                        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Remote-Publish")) %>';
                    strHTML += '</a>';
                }

				strHTML += '<a class="context-menu__item" href="javascript: addToBundle(\'' + objId + '\', \'' + referer + '\'); hidePopUp(\'context_menu_popup_'+objId+'\');">';
					strHTML += '<span class="bundleIcon"></span>';
					strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-To-Bundle")) %>';
				strHTML += '</a>';
			}
		}

        if ((live || hasLiveVersion) && publish) {
			strHTML += '<a href="javascript: unpublishLink(\'' + objId + '\', \'' + referer + '\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="context-menu__item">';
	    		strHTML += '<span class="unpublishIcon"></span>';
	        	strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unpublish")) %>';
			strHTML += '</a>';
		}

		if (locked && write) {
			strHTML += '<a href="javascript: unlockLink(\'' + objId + '\', \'' + referer + '\');  hidePopUp(\'context_menu_popup_'+objId+'\');" class="context-menu__item">';
		    	strHTML += '<span class="keyIcon"></span>';
		        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unlock")) %>';
			strHTML += '</a>';
		}


		if (write && !archived)  {

			strHTML += '<div class="pop_divider" ></div>';

			strHTML += '<a href="javascript: markForCopy(\'' + objId + '\',\'' + referer +'\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="context-menu__item">';
	    		strHTML += '<span class="docCopyIcon"></span>';
	        	strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "markForCopy")) %>';
			strHTML += '</a>';

			strHTML += '<a href="javascript: markForCut(\'' + objId + '\',\'' + referer +'\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="context-menu__item">';
		    	strHTML += '<span class="cutIcon"></span>';
		        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Cut")) %>';
			strHTML += '</a>';

		}

		if (write && archived)
		{
			strHTML += '<a href="javascript: deleteLink(\'' + objId + '\', \'' + referer +'\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="context-menu__item">';
		    	strHTML += '<span class="stopIcon"></span>';
	    	   	strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete-Link")) %>';
			strHTML += '</a>';
		}

		strHTML += '<div class="pop_divider" ></div>';
		strHTML += '<a href="javascript:hidePopUp(\'context_menu_popup_'+objId+'\');" class="context-menu__item">';
			strHTML += '<span class="closeIcon"></span>';
			strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Close")) %>';
		strHTML += '</a>';

		Element.update(div, strHTML);

		showPopUp('context_menu_popup_'+objId, e);
	}

	// HTMLPage popup
	function showHTMLPagePopUp(page, cmsAdminUser, origReferer, e) {
		var objId = page.inode;
        var pageURI = page.inode;
		var objIden = page.identifier;
		var referer = encodeURIComponent(origReferer);

		if($('context_menu_popup_'+objId) == null) {
			var divHTML = '<div id="context_menu_popup_'+objId+'" class="context-menu"></div>';
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
		var hasLiveVersion = page.hasLiveVersion;
		var strHTML = '';
		if ((live || working) && read && !archived) {
			strHTML += '<a href="javascript: previewHTMLPage(\'' + pageURI + '\', { languageId: ' + page.languageId + '});" class="context-menu__item">';
		    	//strHTML += '<span class="pageIcon"></span>';
	    	    strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Open-Preview")) %>';
			strHTML += '</a>';

			if ((live || working) && read)  {
				var actionLabel = write ? '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Open-Edit")) %>' : '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "View")) %>';

				var editFunction = page.isContentlet ?
						                  "editHTMLPageAsset('" + objId + "','" + page.stInode + "')"
						                : "editHTMLPage('" + objId + "', '" + referer + "')";  
				
				strHTML += "<a href=\"javascript: "+editFunction+";\" class=\"context-menu__item\">";
    				strHTML += '<span class="pagePropIcon"></span>';
           			strHTML += actionLabel;
				strHTML += '</a>';
			}
		}

        if (!archived) {
	      strHTML += '<a href="javascript: viewHTMLPageStatistics(\'' + objId + '\', \'' + referer + '\');" class="context-menu__item">';
		      strHTML += '<span class="statisticsIcon"></span>';
		      strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "View-Statistics")) %>';
	      strHTML += '</a>';
	    }

            strHTML += wfActionsMenu(objId, page);

			if (working && !archived && publish) {
	
	            if (enterprise) {
	                if (sendingEndpoints) {
	                    strHTML += '<a href="javascript: remotePublish(\'' + objIden + '\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="context-menu__item">';
	                    strHTML += '<span class="sServerIcon"></span>';
	                    strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Remote-Publish")) %>';
	                    strHTML += '</a>';
	                }
	
					strHTML += '<a class="context-menu__item" href="javascript: addToBundle(\'' + objIden + '\', \'' + referer + '\'); hidePopUp(\'context_menu_popup_'+objId+'\');">';
					strHTML += '<span class="bundleIcon"></span>';
					strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-To-Bundle")) %>';
					strHTML += '</a>';
				}
	
			}
			
			// If archived, only display the "Remove" option in the Push Dialog
			if (archived) {
				if (enterprise) {
                    if (sendingEndpoints) {
                        strHTML += '<a href="javascript: remotePublish(\'' + objIden + '\', \'' + referer + '\', true); hidePopUp(\'context_menu_popup_'+objId+'\');" class="context-menu__item">';
                        strHTML += '<span class="sServerIcon"></span>';
                        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Remote-Publish")) %>';
                        strHTML += '</a>';
                    }
				}
			}
	
			if (locked && write) {
				strHTML += '<a href="javascript: unlockHTMLPage(\'' + objId + '\', \'' + referer + '\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="context-menu__item">';
			    	strHTML += '<span class="keyIcon"></span>';
			        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unlock")) %>';
				strHTML += '</a>';
			}

            if (write && archived)
            {
                strHTML += '<a href="javascript: deleteHTMLPagePreCheck(\'' + objId + '\', \'' + referer +'\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="context-menu__item">';
                strHTML += '<span class="stopIcon"></span>';
                strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete-Page")) %>';
                strHTML += '</a>';
            }


		if (write && !archived)  {
			strHTML += '<div class="pop_divider" ></div>';

            strHTML += '<a href="javascript: markForCopy(\'' + objId + '\',\'' + referer +'\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="context-menu__item">';
            strHTML += '<span class="docCopyIcon"></span>';
            strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "markForCopy")) %>';
            strHTML += '</a>';

			strHTML += '<a href="javascript: markForCut(\'' + objId + '\',\'' + referer +'\'); hidePopUp(\'context_menu_popup_'+objId+'\');" class="context-menu__item">';
		    	strHTML += '<span class="cutIcon"></span>';
		        strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Cut")) %>';
			strHTML += '</a>';

		}

		strHTML += '<div class="pop_divider" ></div>';
		strHTML += '<a href="javascript: hidePopUp(\'context_menu_popup_'+objId+'\');" class="context-menu__item">';
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

		<%if(UtilMethods.isSet(frameName)){%>
		var windowHeight = window.innerHeight;
        <%}else{%>
        var windowHeight = top.document.body.clientHeight;
        <%}%>
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

		<%if(UtilMethods.isSet(frameName)){%>
		var windowHeight = window.innerHeight;
        <%}else{%>
		var windowHeight = top.document.body.clientHeight;
       <%}%>
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

		var menuOptions = '';
		if (selectedFolder == "") {
			var host = inodes[activeHost];
			var addChildren = hasAddChildrenPermissions(host.permissions);
			if (addChildren) {
				var objId = host.identifier;
				var referer = unescape(encodeURIComponent(origReferer));
			    var isAdminUser = <%= APILocator.getUserAPI().isCMSAdmin(user)%>;
                
                if (isAdminUser || userRoles.folderModifiable) {
					menuOptions += '<div data-dojo-type="dijit/MenuItem" onclick="addTopFolder(\'' + objId + '\',\''+referer+'\')">';
					menuOptions += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Folder")) %>';
					menuOptions += '</div>';
                }

					menuOptions += '<div data-dojo-type="dijit/MenuItem" onclick="addHTMLPage(\'' + objId + '\',\'' + referer + '\')">';
					menuOptions += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "HTML-Page")) %>';
					menuOptions += '</div>';

				menuOptions += '<div data-dojo-type="dijit/MenuItem" onclick="addFile(\'' + objId + '\',\'' + referer + '\',false);hidePopUp(\'context_menu_popup_'+objId+'\');hidePopUp(\'context_child_menu_popup_'+objId+'\');">';
				menuOptions += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Image-or-File")) %>';
				menuOptions += '</div>';

			}
		} else {
			var folder = inodes[selectedFolder];
			var addChildren = hasAddChildrenPermissions(folder.permissions);
			if (addChildren) {
				var referer = unescape(encodeURIComponent(origReferer));

				menuOptions += '<div data-dojo-type="dijit/MenuItem" onclick="addFolder(\'' + selectedFolder + '\', \'' + referer + '\')">';
				menuOptions += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Folder")) %>';
				menuOptions += '</div>';

				menuOptions += '<div data-dojo-type="dijit/MenuItem" onclick="addHTMLPage(\'' + selectedFolder + '\',\'' + referer + '\')">';
				menuOptions += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "HTML-Page")) %>';
				menuOptions += '</div>';

				menuOptions += '<div data-dojo-type="dijit/MenuItem" onclick="addFile(\'' + selectedFolder + '\',\'' + referer + '\',false);hidePopUp(\'context_menu_popup_'+objId+'\');hidePopUp(\'context_child_menu_popup_'+objId+'\');">';
				menuOptions += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Image-or-File")) %>';
				menuOptions += '</div>';

				menuOptions += '<div data-dojo-type="dijit/MenuItem" onclick="addFile(\'' + selectedFolder + '\',\'' + referer + '\',true);hidePopUp(\'context_menu_popup_'+objId+'\');hidePopUp(\'context_child_menu_popup_'+objId+'\');">';
				menuOptions += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Multiple-Files")) %>';
				menuOptions += '</div>';

				menuOptions += '<div data-dojo-type="dijit/MenuItem" onclick="addLink(\'' + selectedFolder + '\',\'' + referer + '\')">';
				menuOptions += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Menu-Link")) %>';
				menuOptions += '</div>';
			}
		}

		if (menuOptions) {
			var htmlCode = '';
			htmlCode += '<div data-dojo-type="dijit/form/DropDownButton" data-dojo-props=\'iconClass:"actionIcon", class:"dijitDropDownActionButton"\'>';
			htmlCode += '<span></span>';
			htmlCode += '<div data-dojo-type="dijit/Menu" class="contentlet-menu-actions">';
			htmlCode += menuOptions;
			htmlCode += '</div>';
			htmlCode += '</div>';

			dojo.byId("addNewDropDownButtonDiv").innerHTML = htmlCode;
			dojo.parser.parse("addNewDropDownButtonDiv");
		}
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


    	executeWfAction: function(wfId, assignable, commentable, hasPushPublishActionlet, inode ){
    		this.wfActionId=wfId;

    		if(assignable || commentable || hasPushPublishActionlet){

                //Required clean up as these modals has duplicated widgets and collide without a clean up
                var remoteDia = dijit.byId("remotePublisherDia");
                if(remoteDia){
                    remoteDia.destroyRecursive();
                }

    			var dia = dijit.byId("contentletWfDialog");
    			if(dia){
    				dia.destroyRecursive();

    			}
    			dia = new dijit.Dialog({
    				id			:	"contentletWfDialog",
    				title		: 	"<%=LanguageUtil.get(pageContext, "Workflow-Actions")%>",
					style		:	"min-width:500px;min-height:250px;"
    				});



    			var myCp = dijit.byId("contentletWfCP");
    			if (myCp) {
    				myCp.destroyRecursive(true);
    			}

    			myCp = new dojox.layout.ContentPane({
    				id 			: "contentletWfCP",
    				style		:	"minwidth:500px;min-height:250px;margin:auto;"
    			}).placeAt("contentletWfDialog");

    			dia.show();
    			myCp.attr("href", "/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfTaskAjax?cmd=renderAction&actionId=" + wfId + "&inode=" + inode);
    			return;
    		}
    		else{
     		  		var wfActionAssign 		= "";
		    		var selectedItem 		= "";
		    		var wfConId 			= inode;
		    		var wfActionId 			= this.wfActionId;
		    		var wfActionComments 	= "";
		    		var publishDate			="";
		    		var publishTime 		= "";
		    		var expireDate 			= "";
		    		var expireTime 			="";
		    		var neverExpire 		="";
					BrowserAjax.saveFileAction(selectedItem,wfActionAssign,wfActionId,wfActionComments,wfConId, publishDate,
		    				publishTime, expireDate, expireTime, neverExpire, fileActionCallback);
 			}

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

    		// BEGIN: PUSH PUBLISHING ACTIONLET
			var publishDate = (dijit.byId("wfPublishDateAux"))
				? dojo.date.locale.format(dijit.byId("wfPublishDateAux").getValue(),{datePattern: "yyyy-MM-dd", selector: "date"})
					: (dojo.byId("wfPublishDateAux"))
						? dojo.date.locale.format(dojo.byId("wfPublishDateAux").value,{datePattern: "yyyy-MM-dd", selector: "date"})
								: "";

			var publishTime = (dijit.byId("wfPublishTimeAux"))
				? dojo.date.locale.format(dijit.byId("wfPublishTimeAux").getValue(),{timePattern: "H-m", selector: "time"})
					: (dojo.byId("wfPublishTimeAux"))
						? dojo.date.locale.format(dojo.byId("wfPublishTimeAux").value,{timePattern: "H-m", selector: "time"})
								: "";


			var expireDate = (dijit.byId("wfExpireDateAux"))
				? dijit.byId("wfExpireDateAux").getValue()!=null ? dojo.date.locale.format(dijit.byId("wfExpireDateAux").getValue(),{datePattern: "yyyy-MM-dd", selector: "date"}) : ""
					: (dojo.byId("wfExpireDateAux"))
						? dojo.byId("wfExpireDateAux").value!=null ? dojo.date.locale.format(dojo.byId("wfExpireDateAux").value,{datePattern: "yyyy-MM-dd", selector: "date"}) : ""
								: "";

			var expireTime = (dijit.byId("wfExpireTimeAux"))
				? dijit.byId("wfExpireTimeAux").getValue()!=null ? dojo.date.locale.format(dijit.byId("wfExpireTimeAux").getValue(),{timePattern: "H-m", selector: "time"}) : ""
					: (dojo.byId("wfExpireTimeAux"))
						? dojo.byId("wfExpireTimeAux").value!=null ? dojo.date.locale.format(dojo.byId("wfExpireTimeAux").value,{timePattern: "H-m", selector: "time"}) : ""
								: "";
			var neverExpire = (dijit.byId("wfNeverExpire"))
				? dijit.byId("wfNeverExpire").getValue()
					: (dojo.byId("wfNeverExpire"))
						? dojo.byId("wfNeverExpire").value
								: "";

            var whereToSend = (dijit.byId("whereToSend"))
                ? dijit.byId("whereToSend").getValue()
                    : (dojo.byId("whereToSend"))
                        ? dojo.byId("whereToSend").value
                            : "";

            var forcePush = (dijit.byId("forcePush")) ? dijit.byId("forcePush").checked : false;

			// END: PUSH PUBLISHING ACTIONLET


    		BrowserAjax.saveFileAction(selectedItem,wfActionAssign,wfActionId,wfActionComments,wfConId, publishDate,
    				publishTime, expireDate, expireTime, neverExpire, whereToSend, forcePush, fileActionCallback);

    	}

    });

    function fileActionCallback (response) {
        if (response.status == "success") {
            setTimeout("reloadContent()", 1000);
            showDotCMSSystemMessage(response.message);
            return;
        }

        // An error happened
        reloadContent();
        showDotCMSErrorMessage(response.message);
	}

    var contentAdmin ;




--></script>