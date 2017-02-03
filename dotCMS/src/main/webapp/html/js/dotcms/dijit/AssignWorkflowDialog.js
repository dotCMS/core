dojo.provide("dotcms.dijit.AssignWorkflowDialog");
dojo.require("dijit._Widget");
dojo.require("dijit.Dialog");



dojo.declare("dotcms.dijit.AssignWorkflowDialog", null, {

	actionId:"",
	contentletId:"",
	returnUrl:null,
	myDialog:null,
	myCp:null,
	windowTitle:"Workflow Actions",
	onAssign:this.saveAssign,
	show : function(){
		if(this.myDialog){
			this.myDialog.destroyRecursive();
		}
		this.myDialog = new dijit.Dialog({
			id			:	"contentletWfDialog",
			title		: 	this.windowTitle
			});
		
		
		if(this.myCp){
			this.myCp.destroyRecursive();
		}
		this.myCp = new dojox.layout.ContentPane({
			id 			: "contentletWfCP"
		}).placeAt("contentletWfDialog");
		
		this.myDialog.show();
		this.myCp.attr("href", "/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfTaskAjax?cmd=renderAction&actionId=" + this.actionId + "&contentletId="  + this.contentletId);
		return;
		

		
		
		this.myDialog.show();
	},
	hide : function(){

		if(this.myDialog){
			this.myDialog.hide();
			this.myDialog.destroyRecursive();
		}
	},
	
	saveAssign : function(){
		
		if(!dijit.byId("taskAssignmentAux") || ! dijit.byId("taskAssignmentAux").value || ! dijit.byId("taskAssignmentAux").value.length >0 ){
			showDotCMSSystemMessage("Assign-to required");
			return;
		}
		dojo.byId("wfActionId").value=this.wfActionId;
		dojo.byId("wfActionComments").value=dijit.byId("taskCommentsAux").value;
		dojo.byId("wfActionAssign").value=dijit.byId("taskAssignmentAux").value;
		dijit.byId("contentletWfDialog").hide();
		this.executeWorkflow();
	},
	
	executeWorkflow : function (){
		
		

		var xhrArgs = {
			form: dojo.byId("submitWorkflowTaskFrm"),
			handleAs: "text",
			load: function(data) {
				showDotCMSSystemMessage("Workflow-executed");
				this.hide();
				if(returnUrl && returnUrl.length > 0){
					window.location=returnUrl;
				}
				
			
			},
			error: function(error) {
				showDotCMSSystemMessage(error);
			
			}
		}
		dojo.xhrPost(xhrArgs);

		
	}
})