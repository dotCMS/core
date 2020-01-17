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

	    	executeWfAction: function(wfId, assignable, commentable, inode, showpush){
	    		
	    		this.wfActionId = wfId;
	    		
	    		if(assignable || commentable || showpush){

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
	    			dojo.byId("wfActionId").value=this.wfActionId;

	    			this.executeWorkflow();
	    		}
	    	},
	    	
	    	executeWorkflow : function (){
	    		
	    		dijit.byId('savingContentDialog').show();

				var xhrArgs = {
					form: dojo.byId("submitWorkflowTaskFrm"),
					handleAs: "text",
					load: function(data) {
						showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Workflow-executed")%>");

                        var customEvent = document.createEvent("CustomEvent");
                        customEvent.initCustomEvent("ng-event", false, false,  {
                            name: "edit-task-executed-workflow"
                        });
                        document.dispatchEvent(customEvent);
					
					},
					error: function(error) {
						showDotCMSSystemMessage(error);
						dijit.byId('savingContentDialog').hide();
					
					}
				}
				dojo.xhrPost(xhrArgs);

	    	}

	    });

    function saveAssignCallBack(actionId, formData) {

        var assignComment = formData.assignComment;
        var selectedItem = "";
        var wfConId =  pushPublish.inode;
        var comments = assignComment.comment;
        var assignRole = assignComment.assign;

        var whereToSend = pushPublish.whereToSend;
        var publishDate = pushPublish.publishDate;
        var publishTime = pushPublish.publishTime;
        var expireDate  = pushPublish.expireDate;
        var expireTime  = pushPublish.expireTime;
        var forcePush   = pushPublish.forcePush;
        var neverExpire = pushPublish.neverExpire;

        dojo.byId("wfActionAssign").value = assignRole;
        dojo.byId("wfActionComments").value = comments;
        dojo.byId("wfActionId").value = actionId;

        // BEGIN: PUSH PUBLISHING ACTIONLET
        dojo.byId("wfPublishDate").value = publishDate;
        dojo.byId("wfPublishTime").value = publishTime;
        dojo.byId("wfExpireDate").value = expireDate;
        dojo.byId("wfExpireTime").value = expireTime;
        dojo.byId("wfNeverExpire").value = neverExpire;
        dojo.byId("whereToSend").value = whereToSend;

        var xhrArgs = {
            form: dojo.byId("submitWorkflowTaskFrm"),
            handleAs: "text",
            load: function(data) {
                showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Workflow-executed")%>");

                var customEvent = document.createEvent("CustomEvent");
                customEvent.initCustomEvent("ng-event", false, false,  {
                    name: "edit-task-executed-workflow"
                });
                document.dispatchEvent(customEvent);

            },
            error: function(error) {
                showDotCMSSystemMessage(error);
            }
        }
        dojo.xhrPost(xhrArgs);
    }
	    
	var contentAdmin = new dotcms.dijit.contentlet.ContentAdmin();

</script>
