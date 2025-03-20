<%@page import="com.dotmarketing.business.RoleAPI"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>

<script language="Javascript">
	function publish (objId,assetId) {
		if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-publish-this-Associated-Type")) %>')){
			var href = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">'
			href = href + '<portlet:param name="struts_action" value="/ext/workflows/edit_workflow_task" />'
			href = href + '<portlet:param name="cmd" value="publish" />';
			href = href + '<portlet:param name="referer" value="<%= referer %>" />';
			href = href + '</portlet:actionURL>&inode='+objId+'&asset_inode='+assetId;

			document.location.href = href;
		}
	}

	function unpublish(objId,assetId) {
		if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-un-publish-this-Associated-Type")) %>')){
			var href = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">'
			href = href + '<portlet:param name="struts_action" value="/ext/workflows/edit_workflow_task" />'
			href = href + '<portlet:param name="cmd" value="unpublish" />';
			href = href + '<portlet:param name="referer" value="<%= referer %>" />';
			href = href + '</portlet:actionURL>&inode='+objId+'&asset_inode='+assetId;

			document.location.href = href;
		}
	}


	function archive (objId, assetId) {
		if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-archive-this-Associated-Type")) %>')){
	   		var href = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">'
			href = href + '<portlet:param name="struts_action" value="/ext/workflows/edit_workflow_task" />'
			href = href + '<portlet:param name="cmd" value="delete" />';
			href = href + '<portlet:param name="referer" value="<%= referer %>" />';
			href = href + '</portlet:actionURL>&inode='+objId+'&asset_inode='+assetId;

			document.location.href = href;
		}
	}


	function unarchive (objId, assetId) {
		if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-un-archive-this-Associated-Type")) %>')){
			var href = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">'
			href = href + '<portlet:param name="struts_action" value="/ext/workflows/edit_workflow_task" />'
			href = href + '<portlet:param name="cmd" value="undelete" />';
			href = href + '<portlet:param name="referer" value="<%= referer %>" />';
			href = href + '</portlet:actionURL>&inode='+objId+'&asset_inode='+assetId;

			document.location.href = href;
		}
	}

	function previewHTMLPage (objId, referer) {
		top.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpages/preview_htmlpage" /><portlet:param name="previewPage" value="1" /></portlet:actionURL>&inode=' + objId + '&referer=' + referer;
	}

	function deleteWorkFlowTask(inode) {
	<%
		java.util.Map viewParams = new java.util.HashMap();
		viewParams.put("struts_action",new String[] {"/ext/workflows/view_workflow_tasks"});
		String viewReferer = com.dotmarketing.util.PortletURLUtil.getRenderURL(request,WindowState.MAXIMIZED.toString(),viewParams);
	%>
	  if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-delete-this-workflow-task")) %>')){
			var href= '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">';
			href = href + '<portlet:param name="struts_action" value="/ext/workflows/edit_workflow_task" />';
			href = href + '<portlet:param name="cmd" value="full_delete" />';
			href = href + '<portlet:param name="referer" value="<%= viewReferer %>" />';
			href = href + '</portlet:actionURL>&inode='+inode;

			document.location.href = href;
		}
	}




	//Layout Initialization
	var browserLoaded = false;

	function  resizeBrowser(){
		if(browserLoaded) return;
		browserLoaded=true;
	    var bc = dijit.byId('borderContainer');
	    if(bc != undefined){
	    	bc.resize();
	    }
	}

		dojo.connect(window, "onresize", this, "resizeBrowser");
		dojo.require("dotcms.dojo.push.PushHandler");
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

	    	executeWfAction: function(wfId, assignable, commentable, inode, showpush, moveable){

	    		this.wfActionId = wfId;

	    		if(assignable || commentable || showpush || moveable){

                    var publishDate = '<%=structure != null ? structure.getPublishDateVar() : "" %>';
                    var expireDate =  '<%=structure != null ? structure.getExpireDateVar() : "" %>';
                    var structInode = '<%=structure != null ? structure.getInode() : "" %>';

                    let workflow = {
                        actionId:wfId,
                        inode:inode,
                        publishDate:publishDate,
                        expireDate:expireDate,
                        structInode:structInode
                    };

                    var pushHandler = new dotcms.dojo.push.PushHandler('<%=LanguageUtil.get(pageContext, "Workflow-Action")%>');
                    pushHandler.showWorkflowEnabledDialog(workflow, saveAssignCallBack);
                    return;
	    		}
	    		else{

	    			this.executeWorkflow(wfId, inode);
	    		}
	    	},

	    	executeWorkflow : function (wfId, inode){
				dijit.byId('savingContentDialog').show();

				var wfActionAssign 		= "";
				var selectedItem 		= "";
				var wfConId 			= inode;
				var wfActionId 			= wfId;
				var wfActionComments 	= "";
				var publishDate			= "";
				var publishTime 		= "";
				var expireDate 			= "";
				var expireTime 			= "";
				var neverExpire 		= "";
				var whereToSend 		= "";
				var pathToMove 			= "";

				BrowserAjax.saveFileAction(selectedItem, wfActionAssign, wfActionId, wfActionComments, wfConId, publishDate,
					publishTime, expireDate, expireTime, neverExpire, whereToSend, pathToMove, fileActionCallback
				);
	    	}

	    });

    function saveAssignCallBack(actionId, formData) {
		var pushPublish = formData.pushPublish;
		var assignComment = formData.assignComment;

		var selectedItem = "";
		var wfConId =  pushPublish.inode;
		var comments = assignComment.comment;
		var assignRole = assignComment.assign;
		var pathToMove = assignComment.pathToMove;

		var whereToSend = pushPublish.whereToSend;
		var publishDate = pushPublish.publishDate;
		var publishTime = pushPublish.publishTime;
		var expireDate  = pushPublish.expireDate;
		var expireTime  = pushPublish.expireTime;
		var forcePush   = pushPublish.forcePush;
		var neverExpire = pushPublish.neverExpire;


		BrowserAjax.saveFileAction(selectedItem, assignRole, actionId, comments, wfConId, publishDate,
			publishTime, expireDate, expireTime, neverExpire, whereToSend, forcePush, pathToMove, fileActionCallback
		);
    }

	function fileActionCallback (response) {
		if (response.status == "success") {
			showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Workflow-executed")%>");
			executeEditTaskExecutedWorkflowEvent()
			return;
		}

		// An error happened
		showDotCMSErrorMessage(response.message);
		dijit.byId('savingContentDialog').hide();
	}

	function executeEditTaskExecutedWorkflowEvent() {
		var customEvent = document.createEvent("CustomEvent");
		customEvent.initCustomEvent("ng-event", false, false,  {
			name: "edit-task-executed-workflow"
		});
		document.dispatchEvent(customEvent);
	}

	var contentAdmin = new dotcms.dijit.contentlet.ContentAdmin();

</script>
