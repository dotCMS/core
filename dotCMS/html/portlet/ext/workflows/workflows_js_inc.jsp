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
	    var viewport = dijit.getViewport();
	    var viewport_height = viewport.h;
	    var e =  dojo.byId("borderContainer");
	    dojo.style(e, "height", viewport_height -150 + "px");
	    var bc = dijit.byId('borderContainer');
	    if(bc != undefined){
	    	bc.resize();
	    }
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
	    	
	    	
	    	executeWfAction: function(wfId, assignable, commentable, inode, showpush){
	    		
	    		this.wfActionId=wfId;
	    		
	    		if(assignable || commentable || showpush){
	    			
	    			var myCp = dijit.byId("contentletWfCP");
	    			if (myCp) {
	    				myCp.destroyRecursive(true);
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


	  			

	    			myCp = new dojox.layout.ContentPane({
	    				id 			: "contentletWfCP",
	    				style		:	"minwidth:500px;min-height:250px;margin:auto;"
	    			}).placeAt("contentletWfDialog");

	    			dia.show();


	    			var r = Math.floor(Math.random() * 1000000000);
					var url = "/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfTaskAjax?cmd=renderAction&actionId=" + wfId 
							+ "&inode=" + inode 
							+ "&showpush=" + showpush 
							+ "&publishDate=<%=structure!=null?structure.getPublishDateVar():""%>"
							+ "&expireDate=<%=structure!=null?structure.getExpireDateVar():""%>"
							+ "&structureInode=<%=structure!=null?structure.getInode():""%>"
							+ "&r=" + r;
							
	    			myCp.attr("href", url);
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
				// END: PUSH PUBLISHING ACTIONLET
				
					dojo.byId("wfActionAssign").value=assignRole;
     				dojo.byId("wfActionComments").value=comments;
					dojo.byId("wfActionId").value=this.wfActionId;

					// BEGIN: PUSH PUBLISHING ACTIONLET
					dojo.byId("wfPublishDate").value=publishDate;
					dojo.byId("wfPublishTime").value=publishTime;
					dojo.byId("wfExpireDate").value=expireDate;
					dojo.byId("wfExpireTime").value=expireTime;
					dojo.byId("wfNeverExpire").value=neverExpire;
					dojo.byId("whereToSend").value=whereToSend;
					// END: PUSH PUBLISHING ACTIONLET

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