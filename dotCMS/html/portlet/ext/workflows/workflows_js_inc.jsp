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
		function  resizeBrowser(){
		    var viewport = dijit.getViewport();
		    var viewport_height = viewport.h;
		   
			var  e =  dojo.byId("borderContainer");
			dojo.style(e, "height", viewport_height -150+ "px");
			
		}
		
		dojo.connect(window, "onresize", this, "resizeBrowser");


		

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
	    	
	    	
	    	executeWfAction: function(wfId, assignable, commentable, inode){
	    		
	    		this.wfActionId=wfId;

	    		if(assignable || commentable){
	    			
	    			var myCp = dijit.byId("contentletWfCP");
	    			if(myCp){
	    				myCp.destroyRecursive();
	    			}
	    			var dia = dijit.byId("contentletWfDialog");
	    			if(dia){
	    				dia.destroyRecursive();
	    				
	    			}
	    			dia = new dijit.Dialog({
	    				id			:	"contentletWfDialog",
	    				title		: 	"<%=LanguageUtil.get(pageContext, "Workflow-Actions")%>",
						style		:	"min-width:500px;min-height:300px;"
	    				});
	    			

	    			myCp = new dojox.layout.ContentPane({
	    				id 			: "contentletWfCP",
	    				style		: "min-width:500px;min-height:300px;margin:auto;"
	    			}).placeAt("contentletWfDialog");
					
	    			dia.show();
	    			myCp.attr("href", "/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfTaskAjax?cmd=renderAction&actionId=" + wfId + "&inode=" + inode);
	    			return;
	    		}
	    		else{
	    			dojo.byId("wfActionId").value=this.wfActionId;

	    			this.executeWorkflow();
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

	    		
	    		dojo.byId("wfActionAssign").value=assignRole;
    			dojo.byId("wfActionComments").value=comments;
	    		dojo.byId("wfActionId").value=this.wfActionId;
	    		dijit.byId("contentletWfDialog").hide();
	    		this.executeWorkflow();
	    	},
	    	
	    	executeWorkflow : function (){
	    		
	    		dijit.byId('savingContentDialog').show();
	    		

				var xhrArgs = {
					form: dojo.byId("submitWorkflowTaskFrm"),
					handleAs: "text",
					load: function(data) {
						showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Workflow-executed")%>");
						window.location='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
						<portlet:param name="struts_action" value="/ext/workflows/view_workflow_tasks" />
						<portlet:param name="referer" value="<%= referer %>" />
						</portlet:actionURL>';
					
					},
					error: function(error) {
						showDotCMSSystemMessage(error);
						dijit.byId('savingContentDialog').hide();
					
					}
				}
				dojo.xhrPost(xhrArgs);

	    		
	    	}

	    });
	    var contentAdmin = new dotcms.dijit.contentlet.ContentAdmin();
</script>