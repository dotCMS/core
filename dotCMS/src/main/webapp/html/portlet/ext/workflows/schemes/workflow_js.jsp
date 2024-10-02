<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%response.setContentType("text/javascript");%>
dojo.require("dijit.form.Form");
dojo.require("dijit.form.Button");
dojo.require("dijit.form.ValidationTextBox");
dojo.require("dijit.form.DateTextBox");
dojo.require("dojo.hash");
dojo.require("dojo.dnd.Container");
dojo.require("dojo.dnd.Manager");
dojo.require("dojo.dnd.Source");
dojo.require("dojox.layout.ContentPane");
dojo.require("dijit.TooltipDialog");
dojo.require("dojox.data.QueryReadStore");
dojo.require("dojo.NodeList-manipulate");
// refresh page when the hash has changed
dojo.subscribe("/dojo/hashchange", this, function(hash){mainAdmin.refresh();});

dojo.require("dijit.layout.TabContainer");
dojo.require("dojo.data.ItemFileReadStore");
dojo.require("dotcms.dojo.data.RoleReadStore");
dojo.provide("ValidationTextarea");
dojo.require("dijit.form.SimpleTextarea");

dojo.declare(
    "ValidationTextarea",
    [dijit.form.ValidationTextBox,dijit.form.SimpleTextarea],
    {
        invalidMessage: "This field is required",
        regExp: "(.|\\s)*"
    }
);

dojo.require("dojo.store.Memory");








//
//
// -------------------- SchemeAdminMain --------------------
//
//

dojo.declare("dotcms.dijit.workflows.MainAdmin", null, {
	baseDiv : "workflowSchemeMain",
	wfCrumbTrail : new Array(),
	constructor : function() {
	},
	show : function(href) {

		var r = Math.floor(Math.random() * 1000000000);
		if (href.indexOf("?") > -1) {
			href = href + "&r=" + r;
		} else {
			href = href + "?r=" + r;
		}

		href = href + "&donothing";

		dojo.hash(encodeURIComponent(href));

	},

	refresh : function() {
		var hashValue = decodeURIComponent(dojo.hash());

		if(!hashValue || hashValue.length ==0){
			return;
		}
		var myCp = dijit.byId(this.baseDiv);
		var hanger = dojo.byId("hangWorkflowMainHere");
		if(!hanger){
			return;
		}
		if (myCp) {
			myCp.attr("content","");//myCp.destroyRecursive(true);
		}else{
			myCp = new dojox.layout.ContentPane({
			id : this.baseDiv
			}).placeAt("hangWorkflowMainHere");
		}

		var r = Math.floor(Math.random() * 1000000000);
		hashValue = hashValue + "&rand=" + r;
		myCp.attr("href", hashValue);
		// myCp.refresh();
		dojo.parser.parse("hangWorkflowMainHere");




	},






	addCrumbtrail : function (title, urlx){
		var entry = {title:title, url:urlx};

		this.wfCrumbTrail[this.wfCrumbTrail.length] = entry;


	},
	resetCrumbTrail : function(){


		this.wfCrumbTrail = new Array();
	},

	refreshCrumbtrail : function (){
		var crumbDiv = dojo.byId("subNavCrumbUl");
		crumbDiv.innerHTML ="";
		<!-- dojo.create("li",
		// {onClick:this.show(this.wfCrumbTrail[i].url)},crumbDiv ) -->





		for( i =0;i< this.wfCrumbTrail.length;i++ ){
			var className="showPointer";
			if(i+1 ==this.wfCrumbTrail.length){
				dojo.create("li", {innerHTML:"<b>" + mainAdmin.wfCrumbTrail[i].title + "</b>", className:"lastCrumb"},crumbDiv );

			}
			else{


				dojo.create("li", {innerHTML:"<a href='javascript:mainAdmin.show(mainAdmin.wfCrumbTrail[" + i + "].url)'>" + mainAdmin.wfCrumbTrail[i].title + "</a>", className:className},crumbDiv );
			}

		}


	},
	confirm : function(confirmationMessage ){


		var dia = dijit.byId("wfConfirmDialog");
		if(dia){
			dia.destroyRecursive();
		}



		dia = new dijit.Dialog({
			id			:	"wfConfirmDialog",
			title		: 	"<%=LanguageUtil.get(pageContext, "Confirm")%>",
			innerHTML 		: confirmationMessage
			});




		dia.show();



	}


});




//
//
//
// -------------------- SchemeAdmin --------------------
//
//
//
dojo.declare("dotcms.dijit.workflows.SchemeAdmin", null, {
	baseJsp : "/html/portlet/ext/workflows/schemes/view_schemes.jsp",
	editJsp : "/html/portlet/ext/workflows/schemes/edit_scheme.jsp",
    importJsp : "/html/portlet/ext/workflows/schemes/import_scheme.jsp",
	editDefaultActionsJsp : "/html/portlet/ext/workflows/schemes/edit_default_actions.jsp",
	showArchived : false,
	crumbTitle:"<%=LanguageUtil.get(pageContext, "Schemes")%>",
	addEditDiv:"wfEditSchemeDia",
    importDiv:"wfImportSchemeDia",
	editDefaultActions: "wfEditDefaultActionsDialog",
	constructor : function() {

	},

	show : function() {
		var href = this.baseJsp;
		if (this.showArchived) {
			href = href + "?showArchived=true";
		}
		mainAdmin.show(href);
	},

	showAddEdit : function(schemeId) {
		var myCp = dijit.byId("wfEditSchemeCp");
		if (myCp) {
			myCp.destroyRecursive(false);
		}
		var href = this.editJsp;
		if (schemeId && schemeId.length > 0) {
			href = href + "?schemeId=" + schemeId;
		}

		myCp = new dijit.layout.ContentPane({
			id : "wfEditSchemeCp",
			parseOnLoad : true,

		})


		var dia = dijit.byId(this.addEditDiv);
		if(dia){
			dia.destroyRecursive(false);
		}

		dia = new dijit.Dialog({
			id			:	this.addEditDiv,
			title		: 	"<%=LanguageUtil.get(pageContext, "Edit-Scheme")%>",
			style        : "width:600px;height:560px",
			draggable	:	true
		});


		myCp.attr("href", href);

		myCp.placeAt(this.addEditDiv);

		dia.show();


	},

	hideAddEdit : function() {
		var dialog = dijit.byId(this.addEditDiv);
		dialog.hide();
	},

	saveAddEdit : function() {

		var myForm = dijit.byId("addEditSchemeForm");

		if (myForm.validate()) {

			dojo.xhrPost({
				form : "addEditSchemeForm",
				timeout : 30000,
				handle : function(dataOrError, ioArgs) {
					if (dojo.isString(dataOrError)) {
						if (dataOrError.indexOf("FAILURE") == 0) {
							schemeAdmin.saveError(dataOrError);
						} else {
							schemeAdmin.saveSuccess(dataOrError);
						}
					} else {
						this.saveError("<%=LanguageUtil.get(pageContext, "Unable-to-save-Scheme")%>");

					}
                    schemeAdmin.show(); // refresh so changes can be seen
				}
			});

		}
		;

	},

    	unArchiveScheme : function() {
        	var archived = dojo.byId("schemeArchived");
        	archived.value="false"
        	this.saveAddEdit();
    	},

    	archiveScheme : function() {
        	var archived = dojo.byId("schemeArchived");
        	archived.value="true"
        	this.saveAddEdit();
    	},

	copyScheme : function(schemeId, name) {

		var optionalName = prompt ("<%=LanguageUtil.get(pageContext, "Workflow-Name")%>", name);

		if (null != optionalName) {

			var xhrArgs = {
				url: "/api/v1/workflow/schemes/" + schemeId + "/copy?name=" + optionalName,

				postData: '',
				handleAs: 'json',
				headers : {
				'Accept' : 'application/json',
				'Content-Type' : 'application/json;charset=utf-8'
				},

				timeout : 30000,
				handle : function(dataOrError, ioArgs) {
							if (ioArgs.xhr.status != 200) {

								if (ioArgs.xhr.getResponseHeader("error-message")) {
									schemeAdmin.copyError(ioArgs.xhr.getResponseHeader("error-message"));
								} else {
									schemeAdmin.copyError("<%=LanguageUtil.get(pageContext, "Unable-to-copy-Scheme")%>");
								}
							} else {

								schemeAdmin.copySuccess(dataOrError);
							}
							schemeAdmin.show(); // refresh so changes can be seen
				}
			};
			dojo.xhrPost(xhrArgs);
		}
		return;
	},
    deleteScheme : function(schemeId) {

        if(!confirm("<%=LanguageUtil.get(pageContext, "Confirm-Delete-Scheme")%>")){
            return;
        }
        var xhrArgs = {
            url: "/api/v1/workflow/schemes/" + schemeId ,
            timeout : 30000,
            handle : function(dataOrError, ioArgs) {
                if (dojo.isString(dataOrError)) {
                    if (dataOrError.indexOf("FAILURE") == 0) {
                        schemeAdmin.deleteError(dataOrError);
                    } else {
                        schemeAdmin.deleteSuccess(dataOrError);
                     }
                } else {
                    schemeAdmin.deleteError("<%=LanguageUtil.get(pageContext, "Unable-to-delete-Scheme")%>");
                }
            }
        };
        dojo.xhrDelete(xhrArgs);
        return;
    },
	saveSuccess : function(message) {
		var dialog = dijit.byId(schemeAdmin.addEditDiv);
        if(dialog != undefined){
            dialog.hide();
        }
		showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Workflow-Scheme-saved")%>");

	},
	saveError : function(message) {
		showDotCMSSystemMessage(message, true);

	},
    deleteSuccess : function(message) {
        var dialog = dijit.byId(schemeAdmin.addEditDiv);
        if(dialog != undefined){
            dialog.hide();
        }
        schemeAdmin.show();
        mainAdmin.refresh();
        showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Workflow-Scheme-deleted")%>");
    },
    deleteError : function(message) {
        showDotCMSSystemMessage(message, true);
    },
	copySuccess : function(message) {
		var dialog = dijit.byId(schemeAdmin.addEditDiv);
        if(dialog != undefined){
            dialog.hide();
        }
		schemeAdmin.show();
		showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Workflow-Scheme-copied")%>");
	},
	copyError : function(message) {
		showDotCMSSystemMessage(message, true);
	},

    showImport : function(schemeId) {
        var myCp = dijit.byId("wfImportSchemeCp");
        if (myCp) {
            myCp.destroyRecursive(false);
        }
        var href = this.importJsp;

        myCp = new dijit.layout.ContentPane({
            id : "wfImportSchemeCp",
            parseOnLoad : true,
        });

        var dia = dijit.byId(this.importDiv);
        if(dia){
            dia.destroyRecursive(false);
        }

        dia = new dijit.Dialog({
            id : this.importDiv,
            title : "<%=LanguageUtil.get(pageContext, "Import-Workflow-Scheme")%>",
            draggable : false
        });

        myCp.attr("href", href)
        myCp.placeAt(this.importDiv)
        dia.show();
    },
    hideImport : function() {
        var dialog = dijit.byId(this.importDiv);
        dialog.hide();
    },
    importScheme : function() {

        if(!confirm("<%=LanguageUtil.get(pageContext, "Confirm-Import-Scheme")%>")){
            return;
        }

        var fileReader = new FileReader();
        fileReader.readAsText(document.getElementById("schemejsonfile").files[0]);
        fileReader.onload = function (fileReaderEvent, fileText) {
            var fileText = fileReaderEvent.target.result;

            var xhrArgs = {
                url : "/api/v1/workflow/schemes/import",
                postData: fileText,
                timeout : 30000,
                headers: { "Content-Type": "application/json"},
                handle : function(dataOrError, ioArgs) {
                    if (ioArgs.xhr.status != 200) {
                        if (ioArgs.xhr.getResponseHeader("error-message")) {
                            schemeAdmin.importError(ioArgs.xhr.getResponseHeader("error-message"));
                        } else {
                            schemeAdmin.importError("<%=LanguageUtil.get(pageContext, "Unable-to-import-Scheme")%>");
                        }
                    } else {
                        schemeAdmin.importSuccess(dataOrError);
                    }
                }
            };

            dojo.xhrPost(xhrArgs);
        }
        return;
    },
    importSuccess : function(message) {
		try {
			dijit.byId('importWorkflowErrors').hide();
		} catch (e) {
			console.error(e);
		}
        schemeAdmin.hideImport();
        mainAdmin.refresh();
        showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Workflow-Scheme-Imported")%>");
    },
    importError : function(message) {
		var errorDisplayElement = dijit.byId('importWorkflowErrors');
		dojo.byId('importWorkflowExceptionData').innerHTML = "<ul><li>"+message+"</li></ul>";
		errorDisplayElement.show();
    },

	createEditDefaultActions : function(schemeId) {
		var myCp = dijit.byId("wfEditDefaultActions");
		if (myCp) {
			myCp.destroyRecursive(false);
		}
		var href = this.editDefaultActionsJsp;
		if (schemeId && schemeId.length > 0) {
			href = href + "?schemeId=" + schemeId;
		}
		myCp = new dijit.layout.ContentPane({
			id : "wfEditDefaultActions",
			parseOnLoad : true,
		})
		var dia = dijit.byId(this.editDefaultActions);
		if(dia){
			dia.destroyRecursive(false);
		}
		dia = new dijit.Dialog({
			id : this.editDefaultActions,
			title : "<%=LanguageUtil.get(pageContext, "Default-Actions")%>",
			style : "width:600px;height:560px",
			draggable : true
		});
		myCp.attr("href", href);
		myCp.placeAt(this.editDefaultActions);
		dia.show();
		dia.hide();
	},

	showEditDefaultActions : function(schemeId) {
		schemeAdmin.getWorkflowActionsByScheme(schemeId);
	},

	fillAvailableWorkflowActions : function (schemeId, actions){

		var items = new Array();
		items.push({
			id: "",
			name: ""
		});
		for(var i=0; i < actions.length; i++){
			items.push({
				id: actions[i].id,
				name: actions[i].name
			});
		}
		var actionData = {
			identifier: 'id',
			label: 'name',
			items: items
		};
		var actionStore = new dojo.data.ItemFileReadStore({data:actionData});
		dijit.byId("defaultActionNEW").set('store', actionStore);
		dijit.byId("defaultActionEDIT").set('store', actionStore);
		dijit.byId("defaultActionPUBLISH").set('store', actionStore);
		dijit.byId("defaultActionUNPUBLISH").set('store', actionStore);
		dijit.byId("defaultActionARCHIVE").set('store', actionStore);
		dijit.byId("defaultActionUNARCHIVE").set('store', actionStore);
		dijit.byId("defaultActionDELETE").set('store', actionStore);
		dijit.byId("defaultActionDESTROY").set('store', actionStore);

		/*
		Now we load the available actions into the dropdowns we can display the modal
		and set the already stored actions for each default action.
		*/
		//Showing the default actions dialog
		schemeAdmin.showDefaultActionsDialog();

		//Setting the stored action for each default action dropdown
		schemeAdmin.getCurrentDefaultActionsByScheme(schemeId);
	},

	//Obtains the possible default actions for the scheme
	getWorkflowActionsByScheme : function(schemeId){
		var xhrArgs = {
			url: "/api/v1/workflow/schemes/" + schemeId + "/actions",
			handleAs: "json",
			load: function(data) {
				var results = data.entity;
				schemeAdmin.fillAvailableWorkflowActions(schemeId, results);
			},
			error : function(error) {
				showDotCMSSystemMessage(error, true);
			}
		};
		dojo.xhrGet(xhrArgs);
	},

	//Show the default actions modal
	showDefaultActionsDialog : function(){
		var dialog = dijit.byId(this.editDefaultActions);
		dialog.show();
	},

	//Obtains the current default actions for the scheme and fills the values on the dropdown
	getCurrentDefaultActionsByScheme : function(schemeId){
		var xhrArgs = {
			url: "/api/v1/workflow/schemes/" + schemeId + "/system/actions",
			handleAs: "json",
			load: function(data) {
				var results = data.entity;
				for(var i=0; i < results.length; i++){
					dojo.byId("defaultAction" + results[i].systemAction).value = results[i].workflowAction.name;
					dijit.byId("defaultAction" + results[i].systemAction).set('data-system-action-id',results[i].identifier);
					dijit.byId("defaultAction" + results[i].systemAction).set("value",results[i].workflowAction.id,false);
				}
			},
			error : function(error) {
				showDotCMSSystemMessage(error, true);
			}
		};
		dojo.xhrGet(xhrArgs);
	},

	changeWorkflowDefaultAction : function(systemAction,schemeId){
		var newAction = dijit.byId("defaultAction" + systemAction).getValue();
		var systemActionId = dijit.byId("defaultAction" + systemAction).attr('data-system-action-id')
		if(newAction === ""){
			dojo.xhrDelete({
				url: "/api/v1/workflow/system/actions/"+systemActionId,
				load: function() {
					showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Default-Action-Deleted")%>");
				}
			});
		}else{
			var data = {
				"actionId": newAction,
				"systemAction": systemAction,
				"schemeId": schemeId
			};
			dojo.xhrPut({
				url: "/api/v1/workflow/system/actions",
				handleAs: "json",
				postData: dojo.toJson(data),
				headers : {
					'Accept' : 'application/json',
					'Content-Type' : 'application/json;charset=utf-8',
				},
				load: function(data) {
					dijit.byId("defaultAction" + systemAction).set('data-system-action-id',data.entity.identifier);
					showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Default-Action-Updated")%>");
				},
				error: function(error){
					showDotCMSSystemMessage("ERROR:" + error,true);
				}
			});
		}
	}

});
//
//
//

//
// -------------------- StepAdmin --------------------
//
//
//
//
dojo.declare("dotcms.dijit.workflows.StepAdmin", null, {
	baseJsp : "/html/portlet/ext/workflows/schemes/view_steps.jsp",
	editJsp : "/html/portlet/ext/workflows/schemes/edit_step.jsp",
	schemeId: "",
	crumbTitle:"<%=LanguageUtil.get(pageContext, "Steps")%>",
	showViewSteps : function(schemeId) {
		schemeAdmin.createEditDefaultActions(schemeId);
		mainAdmin.show(this.baseJsp + "?schemeId=" + schemeId);

	},
	editStep : function(schemeId, stepId){
		mainAdmin.show(this.editJsp + "?schemeId=" + schemeId + "&stepId="+ stepId);

	},
	alreadyDone : "",
	addStep : function (){

		var stepName = encodeURIComponent(dijit.byId("stepName").getValue());


		var myParams = "cmd=add&stepName=" + encodeURIComponent(stepName) + "&schemeId=" +  this.schemeId;

		var xhrArgs = {

			url: "/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfStepAjax",
			postData : myParams,
    		handleAs: "text",
			handle : function(dataOrError, ioArgs) {
				if (dojo.isString(dataOrError)) {
					if (dataOrError.indexOf("FAILURE") == 0) {
						showDotCMSSystemMessage(dataOrError, true);

					} else {
						stepAdmin.addSuccess(dataOrError);
					}
				} else {
					showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Unable-to-add-Step")%>", true);


				}
			}
		};
		dojo.xhrPost(xhrArgs);

		return;
	},

	showAddNewStep : function (){
		var dia = dijit.byId("addNewStepDia");
		if(dia){
			dia.destroyRecursive();
		}

		dia = new dijit.Dialog({
				content: '<div class="inline-form"><%=LanguageUtil.get(pageContext, "Name")%>:&nbsp;<input type="text" name="stepName" id="stepName" dojoType="dijit.form.ValidationTextBox"  required="true" value="" maxlength="255">&nbsp;<button dojoType="dijit.form.Button" onClick="stepAdmin.addStep()" iconClass="addIcon" id="Save-new-step"><%=LanguageUtil.get(pageContext, "Add")%></button></div>',
				id			:	"addNewStepDia",
				title		: 	"<%=LanguageUtil.get(pageContext, "Add-Step")%>",
				onKeyPress:function(e){
				if(e.keyCode==13){
					stepAdmin.addStep();
				}
				}
			});

		dia.show();


	},



	addSuccess : function (data){
		mainAdmin.refresh();
		showDotCMSSystemMessage("Added");
		var dia = dijit.byId("addNewStepDia");
				if(dia){
			dia.destroyRecursive();
		}
	},

	deleteStep : function (stepId){

		if(!confirm("<%=LanguageUtil.get(pageContext, "Confirm-Delete-Step")%>")){
			return;

		}

		var xhrArgs = {
			url: "/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfStepAjax?cmd=delete&stepId=" + stepId,
			handle : function(dataOrError, ioArgs) {
				if (dojo.isString(dataOrError)) {
					if (dataOrError.indexOf("FAILURE") == 0) {
							dataOrError = '<div class="warningText">' + dataOrError + '</div>';
							var thisdialog = new dijit.Dialog({ title: "Delete Step", content: dataOrError });
							dojo.body().appendChild(thisdialog.domNode);
							thisdialog.startup();
							thisdialog.show();
					} else {
						stepAdmin.deleteSuccess(dataOrError);
					}
				} else {
					showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Unable-to-delete-Step")%>", true);


				}
			}
		};
		dojo.xhrPut(xhrArgs);

		return;
	},
	reorderStep : function (stepId, order){



		var xhrArgs = {
			url: "/api/v1/workflow/reorder/step/" + stepId+ "/order/" + order,
			async:true,
			handle : function(dataOrError, ioArgs) {
				if (dojo.isString(dataOrError)) {
					if (dataOrError.indexOf("FAILURE") == 0) {
						showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Unable-to-reorder-Step")%>", true);
						return false;
					} else {
						return false;
					}
				} else {
					showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Unable-to-reorder-Step")%>", true);
					return false;


				}
			}
		};
		dojo.xhrPut(xhrArgs);

		return;
	},
	deleteSuccess : function (data){
		mainAdmin.refresh();
		showDotCMSSystemMessage("deleted");
	},


	showStepEdit : function(stepId){
		var dia = dijit.byId("stepEditDia");
		if(dia){
			dia.destroyRecursive();
		}
		dia = new dijit.Dialog({
			id			:	"stepEditDia",
			title		: 	"<%=LanguageUtil.get(pageContext, "Edit-Step")%>",
			draggable	:	false
		});

		var myCp = dijit.byId("stepEditCp");
		if (myCp) {
			myCp.destroyRecursive(true);
		}


		myCp = new dojox.layout.ContentPane({
			id : "stepEditCp"
		})

		myCp.placeAt("stepEditDia");


		dia.show();
		setTimeout(function() {
			dia.domNode.style.top = "50%"
			dia.domNode.style.marginTop = '-' + (parseInt(dia.domNode.offsetHeight / 2, 10) + 'px');
			dia.domNode.style.left = "50%"
			dia.domNode.style.marginLeft = '-' + (parseInt(dia.domNode.offsetWidth / 2, 10) + 'px');
		}, 100)

		myCp.attr("href","/html/portlet/ext/workflows/schemes/edit_step.jsp?stepId=" + stepId);

	},
	hideEdit: function(){
		var dia = dijit.byId("stepEditDia");
		if(dia){
			dia.destroyRecursive();
		}
	},
	editStep: function(){
		var myForm = dijit.byId("addEditStepForm");

		if (myForm.validate()) {

			dojo.xhrPost({
				form : "addEditStepForm",
				timeout : 30000,
				handle : function(dataOrError, ioArgs) {
					if (dojo.isString(dataOrError)) {
						if (dataOrError.indexOf("FAILURE") == 0) {

							showDotCMSSystemMessage(dataOrError, true);
						} else {
							showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Saved")%>");

							var dia = dijit.byId("stepEditDia");
							dia.hide();

							mainAdmin.refresh();
						}
					} else {
						this.saveError("<%=LanguageUtil.get(pageContext, "Unable-to-save-Scheme")%>");

					}
				}
			});

			var x = dijit.byId("addEditStepDia");
			if(typeof x != "undefined"){
				x.hide();
			}
		}
	},
    filterSteps:function(schemeId, roleId, contentType){

        var targetNode = dojo.byId("wfStepInDragContainer");

		var xhrArgs = {
		   url: "/html/portlet/ext/workflows/schemes/view_steps_filtered.jsp?schemeId="+schemeId+"&roleId="+roleId+"&contentTypeId="+contentType,
		   async:true,
		   load: function(markup){
               targetNode.innerHTML = markup;
		   },
		   error: function(error){
                console.log(error);
                showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Unable-to-load-Scheme")%>", true);
		   }
		};
		// Call the asynchronous xhrGet
		var deferred = dojo.xhrGet(xhrArgs);

    }

});






//
//
//
//
// -------------------- ActionAdmin --------------------
//
//
//
//
dojo.declare("dotcms.dijit.workflows.ActionAdmin", null, {

	baseJsp : "/html/portlet/ext/workflows/schemes/view_action.jsp",
	crumbTitle:"<%=LanguageUtil.get(pageContext, "Actions")%>",
	whoCanUse:new Array(),
	reorderAction : function (nodes){


		var movedId = dojo.attr(nodes[0],"id");
		var stepId = movedId.split("_")[2];
		var actionId = movedId.split("_")[1];
		var i=0;
		dojo.query("#jsNode" + stepId + " tr").forEach(function(node){
			if(node.id == movedId ){
				var xhrArgs = {
					url: "/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfActionAjax?cmd=reorder&actionId=" + actionId + "&order=" + i + "&stepId=" + stepId,
					handle : function(dataOrError, ioArgs) {
						if (dojo.isString(dataOrError)) {
							if (dataOrError.indexOf("FAILURE") == 0) {

								<!--// schemeAdmin.saveError(dataOrError); -->
							} else {
								<!--// schemeAdmin.saveSuccess(dataOrError); -->
							}
						} else {
							<!--//this.saveError("<%=LanguageUtil.get(pageContext, "unable-to-save-action")%>"); -->

						}
					}
				};
				dojo.xhrPut(xhrArgs);
				return;
			}
			i++;

		})
	},

	findStepDiv : function (ele){
		var parent = ele;
		while (true) {
			if(parent.dataset.wfstepId){
				return parent;
			}
			parent = parent.parentNode;
			if(parent == document.body){
				return;
			}
		}
		return ;
	},

	findStepId : function (ele){
		var parent = ele;
		while (true) {
			if(parent.dataset.wfstepId){
				return parent.dataset.wfstepId;
			}
			parent = parent.parentNode;
			if(parent == document.body){
				return;
			}
		}
		return ;
	},

	findActionId : function (ele){
		var parent = ele;
		while (true) {
			if(parent.dataset.wfactionId){
				return parent.dataset.wfactionId;
			}
			parent = parent.parentNode;
			if(parent == document.body){
				return;
			}
		}
		return ;
	},
	findActionDiv : function (ele){
		var parent = ele;
		while (true) {
			if(parent.classList.contains("wf-action-wrapper")){
				return parent;
			}
			parent = parent.parentNode;
			if(parent == document.body){
				return;
			}
		}
		return ;
	},
	confirmDeleteAction : function(stepId, actionId, ele) {

        let matches = document.querySelectorAll('.x' + actionId );
		// we only confirm if this is the last instance of the action
		var deleteUrl = "/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfActionAjax?cmd=deleteActionForStep&actionId=" + actionId + "&stepId=" + stepId ;
		if(matches.length == 1) {
			if(!confirm("<%=LanguageUtil.get(pageContext, "Confirm-Delete-Action")%>")) {
				return;
			} else {
				deleteUrl = "/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfActionAjax?cmd=delete&actionId=" + actionId + "&stepId=" + stepId ;
			}
		}

		var xhrArgs = {
			url: deleteUrl ,
			handle : function(dataOrError, ioArgs) {
				if (dojo.isString(dataOrError)) {

					if (dataOrError.indexOf("FAILURE") == 0) {
						showDotCMSSystemMessage(dataOrError, true);
					} else {
						var die = actionAdmin.findActionDiv(ele);
						die.parentNode.removeChild(die);
						actionAdmin.deleteSuccess(dataOrError);
					}
				} else {
					this.saveError("<%=LanguageUtil.get(pageContext, "unable-to-save-scheme")%>");
				}
			}
		};

		dojo.xhrPut(xhrArgs);
	},

	deleteActionForStep : function (ele, stepIndex){

		var stepId   = this.findStepId(ele);
		var actionId = this.findActionId(ele);

		if (0 == stepIndex) { // if trying to remove an action on the first step, check if the method is a default one

			var xhrArgs = {

				url: "/api/v1/workflow/system/actions/" + actionId,
				handle : function(dataOrError, ioArgs) {

					console.log("dataOrError", dataOrError);
					if (dojo.isString(dataOrError)) {

						var response = JSON.parse(dataOrError);
						if (response.errors.length > 0) {
							showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "unable-to-save-action")%>", true);
						} else {
							if (response.entity.length > 0) {

								var owners = "";
								for(var i=0; i < response.entity.length; i++) {

									owners += response.entity[i].owner.name + " ";
								}

								if(!confirm(`<%=LanguageUtil.get(pageContext, "Confirm-Delete-Default-Action", "${owners}")%>`)) {
									return;
								}
							}
							actionAdmin.confirmDeleteAction(stepId, actionId, ele);
						}
					} else {
						showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "unable-to-save-action")%>", true);
					}
				}
			};
			dojo.xhrGet(xhrArgs);

		} else {

			actionAdmin.confirmDeleteAction(stepId, actionId, ele);
		}

		return;
	},


	deleteSuccess : function(message) {


		showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Deleted")%>");
	},

	deleteSuccess : function(message) {



		showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Deleted")%>");

	},

	viewAction : function(schemeId, actionId) {
		mainAdmin.show(this.baseJsp + "?schemeId=" + schemeId + "&actionId=" + actionId + "&" + Math.random());

	},

    addOrAssociatedAction : function(schemeId, stepId, actionsDropDownOptionsId) {

		let actionsDropDownOptions = document.getElementById (actionsDropDownOptionsId);
		let actionId               = "new";

		if(actionsDropDownOptions !=null   ){
			actionId               = actionsDropDownOptions.options[actionsDropDownOptions.selectedIndex].value;
		}

		if ('new' === actionId) {
			mainAdmin.show(this.baseJsp + "?schemeId=" + schemeId +  "&stepId=" + stepId + "&actionId=" + actionId + "&" + Math.random());
		} else {

			var xhrArgs = {
				url: "/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfStepAjax?cmd=addActionToStep&stepId=" + stepId + "&actionId=" + actionId ,
				handle : function(dataOrError, ioArgs) {
					if (dojo.isString(dataOrError)) {

						if (dataOrError.indexOf("FAILURE") == 0) {
							showDotCMSSystemMessage(dataOrError, true);
						} else {
							mainAdmin.refresh();
						}
					} else {
						this.saveError("<%=LanguageUtil.get(pageContext, "unable-to-save-action")%>");

					}
				}
			};

			dojo.xhrPost(xhrArgs);

		}
	},
	copyOrReorderAction : function(ele) {

		let stepId = this.findStepId(ele);
		let stepDiv = this.findStepDiv(ele);
		let actionDiv = this.findActionDiv(ele);
		let actionId = this.findActionId(ele);
		let order=0;
		let actions = stepDiv.querySelectorAll(".wf-action-wrapper");

		for(i=0;i<actions.length;i++){
			if(actions[i] == actionDiv && order==0){
				order = i;
			}
			if(actions[i] != actionDiv && actions[i].dataset.wfactionId == ele .dataset.wfactionId){
				actions[i].parentNode.removeChild(actions[i]);
			}
		}

		var xhrArgs = {
		url: "/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfStepAjax?cmd=addActionToStep&stepId=" + stepId + "&actionId=" + actionId + "&order=" + order,
		handle : function(dataOrError, ioArgs) {
			if (dojo.isString(dataOrError)) {

				if (dataOrError.indexOf("FAILURE") == 0) {
					showDotCMSSystemMessage(dataOrError, true);
				} else {
					//mainAdmin.refresh();
				}
			} else {
				this.saveError("<%=LanguageUtil.get(pageContext, "unable-to-save-action")%>");

			}
		}
	};

	dojo.xhrPost(xhrArgs);

	},
	saveAction : function(schemeId) {

		var myForm = dijit.byId("addEditAction");

		if (myForm.validate()) {
			dojo.xhrPost({
				form : "addEditAction",
				preventCache:true,

				timeout : 30000,
				handle : function(dataOrError, ioArgs) {
					if (dojo.isString(dataOrError) && dataOrError) {

						if (dataOrError.indexOf("FAILURE") == 0) {

							actionAdmin.saveError(dataOrError);
						} else {
							var actionId  = dataOrError.split(":")[1];
							actionAdmin.viewAction(schemeId, actionId);
							showDotCMSSystemMessage("Saved");
						}
					} else {

						actionAdmin.saveError("<%=LanguageUtil.get(pageContext, "Unable-to-save-action")%>");

					}
				}
			});

		}


	},
	saveSuccess : function(message) {

		showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Saved")%>");
		var actionId  = message.split(":")[1];
		mainAdmin.show(this.baseJsp + "?stepId=" + stepId + "&actionId=" + actionId);


	},
	saveError : function(message) {
		showDotCMSSystemMessage(message, true);

	},


	addSelectedToWhoCanUse : function(){
		var selectWho = dijit.byId("whoCanUseSelect");

		var user = selectWho.getValue();
		var userName = selectWho.attr('displayedValue');

        if (actionAdmin.isSet(user) && actionAdmin.isSet(userName)) {
            actionAdmin.addToWhoCanUse(user, userName);
            actionAdmin.refreshWhoCanUse();
            actionAdmin.doChange();

        }
	},

	addToWhoCanUse : function ( myId, myName){
		for(i=0;i < this.whoCanUse.length;i++){
			if(myId == this.whoCanUse[i].id  ||  myId == "user-" + this.whoCanUse[i].id || myId == "role-" + this.whoCanUse[i].id){
				return;
			}
		}

		var entry = {name:myName,id:myId };
		this.whoCanUse[this.whoCanUse.length] =entry;

	},

	removeFromWhoCanUse: function (myId){

		var x=0;
		var newCanUse = new Array();
		for(i=0;i < this.whoCanUse.length;i++){
			if(myId != this.whoCanUse[i].id){
				newCanUse[x] = this.whoCanUse[i];
				x++;
			}
		}
		this.whoCanUse= newCanUse;
		actionAdmin.doChange();
	},

	refreshWhoCanUse : function (){
		dojo.empty("whoCanUseTbl");
		var table = dojo.byId("whoCanUseTbl");
		var x = "";

		this.whoCanUse = this.whoCanUse.sort(function(a,b){
			var x = a.name.toLowerCase();
		    var y = b.name.toLowerCase();
		    return ((x < y) ? -1 : ((x > y) ? 1 : 0));
		});
		for(i=0; i< this.whoCanUse.length ; i++){
			var what = (this.whoCanUse[i].id.indexOf("user") > -1) ? " (<%=LanguageUtil.get(pageContext, "User")%>)" : "";
			x = x + this.whoCanUse[i].id + ",";
			var tr = dojo.create("tr", null, table);
			dojo.create("td", { innerHTML: "<span class='deleteIcon'></span>",className:"wfXBox", onClick:"actionAdmin.removeFromWhoCanUse('" + this.whoCanUse[i].id +"');actionAdmin.refreshWhoCanUse()" }, tr);
			dojo.create("td", { innerHTML: this.whoCanUse[i].name + what}, tr);

		}
		dojo.query('#whoCanUse').val(x);

	},
	doChange: function(){
		dojo.attr("saveButtonDiv","className", "saveButtonDivShow");
		var x = dijit.byId("actionAssignToSelect");
		if(!x || x == undefined || !x.displayedValue || x.displayedValue==undefined){
			return;
		}
		if(dijit.byId("actionAssignable").getValue()){
			if(x.displayedValue.indexOf("(<%=LanguageUtil.get(pageContext, "User")%>)") > -1 || x.displayedValue.indexOf("<%=LanguageUtil.get(pageContext, "current-user")%>") >-1){
          		if(dijit.byId("actionRoleHierarchyForAssign")){
          			dijit.byId("actionRoleHierarchyForAssign").setValue(false);
          		}
          		dojo.style("divRoleHierarchyForAssign", "display", "none");
			}
        	else{
          		dojo.style("divRoleHierarchyForAssign", "display", "block");
      		}
      	}else{
       		dojo.style("divRoleHierarchyForAssign", "display", "none");
      	}
	},
    isSet: function (toValidate) {
        if (toValidate == null || toValidate == undefined || toValidate == "") {
            return false;
        }

        return true;
    }
});



//
//
//
//
// -------------------- ActionClass And Parameters Admin --------------------
//
//
//
//

dojo.declare("dotcms.dijit.workflows.ActionClassAdmin", null, {

	myDnD : null,
	initDnD : function () {

		// Clean up existing and not longer valid dnd source instances
		if (null !== actionClassAdmin.myDnD) {
			actionClassAdmin.myDnD.destroy();
			actionClassAdmin.myDnD = null;
		}

		// Creating a new dnd source instance
		actionClassAdmin.myDnD = new dojo.dnd.Source("actionletsTblBody", {autoSync : true});
		actionClassAdmin.myDnD.on("DndDrop", actionClassAdmin.reorderActionClasses);
	},

	actionClasses : [],
	addSelectedToActionClasses : function(){
		var select = dijit.byId("wfActionlets");

		var clazz = select.getValue();
		var name = select.attr('displayedValue');
        if(clazz.length>0){
		   this.addActionClass(clazz, name);
		   select.setValue("");
		}

	},
    /**
     * Add subaction into the system (using ajax) and table
     */
    addActionClass : function (clazz, myName){
        var actionId = dojo.byId("actionId").value;

        var xhrArgs = {
                url: "/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfActionClassAjax",
                content: {
                  cmd: "add",
                  actionId: actionId,
                  actionletClass: clazz,
                  actionletName: myName
                },
                handle : function(dataOrError, ioArgs) {
                    if (dojo.isString(dataOrError)) {
                        if (dataOrError.indexOf("FAILURE") == 0) {
                            showDotCMSSystemMessage(dataOrError, true);
                        } else {
                            var x = dataOrError.split(":");
                            var entry = {id:x[0], name:x[1], isOnlyBatch:x[2]};
							var isOnlyBatch = x[2];
                            actionClassAdmin.actionClasses.push(entry);

                            actionClassAdmin.refreshActionClasses();

							if(isOnlyBatch) {
								actionClassAdmin.disableShowOnEditing();
								showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Only-Batch-Actions")%>", false);
							}
                            //showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Added")%>", false);
                        }
                    } else {
                        showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Unable-to-save-subaction")%>", true);
                    }
                }
            };
            dojo.xhrPost(xhrArgs);
    },

	/**
	* Enable the editing on show when
	*/
	enableShowOnEditing : function (){

		if(dijit.byId('showOnEDITING') && dijit.byId('showOnEDITING') != undefined){
			dijit.byId('showOnEDITING').set('disabled', false);
		}
	},

	/**
	* Disable the editing on show when
	*/
    disableShowOnEditing : function (){
		if(dijit.byId('showOnEDITING') && dijit.byId('showOnEDITING') != undefined){
			dijit.byId('showOnEDITING').set('checked', false);
			dijit.byId('showOnEDITING').set('disabled', true);
		}
	},
    /**
     * Delete subaction from the system (using ajax) and table
     */
    deleteActionClass : function (actionClassId){
        if(!confirm("<%=LanguageUtil.get(pageContext, "Confirm-Delete-Subaction")%>")){
            return;
        }

        var xhrArgs = {
             url: "/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfActionClassAjax?cmd=delete&actionClassId=" + actionClassId ,
            handle : function(dataOrError, ioArgs) {
                if (dojo.isString(dataOrError)) {
                    if (dataOrError.indexOf("FAILURE") == 0) {
                        showDotCMSSystemMessage(dataOrError, true);
                    } else {
                        actionClassAdmin.removeFromActionClasses(actionClassId);

                        // Refresh table
                        actionClassAdmin.refreshActionClasses();
                        showDotCMSSystemMessage("Deleted");
                    }
                } else {
                    showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Unable-to-delete-subaction")%>", true);
                }
            }
        };
        dojo.xhrPut(xhrArgs);

        return;
    },
    /**
     * Delete action class using id.
     *
     * @returns null if action class not found, otherwise the action class object
     */
    removeFromActionClasses: function (actionClassId) {
        var deletedActionClass = null;

        // Find action class with id in actions array
        var actionClassPosition = -1;
        dojo.forEach(actionClassAdmin.actionClasses, function(entry, i){
            if(actionClassId == entry.id){
                deletedActionClass = entry;
                actionClassPosition = i;
                return;
            }
        });

        // Delete action class with position
        if(actionClassPosition != -1) {

            actionClassAdmin.actionClasses.splice(actionClassPosition, 1);
        }

        return deletedActionClass;
	},
	/**
	 * Method to reorder the action classes array
	 */
    moveFromActionClasses: function (actionClassId, newOrder) {
        if(actionClassAdmin.actionClasses.length > 1) {
            var deletedActionClass = actionClassAdmin.removeFromActionClasses(actionClassId);
            actionClassAdmin.actionClasses.splice(newOrder, 0, deletedActionClass);
        }
    },
    /**
     * Method that recreates table from action class array. Also add drag and drop events to the table
     */
    refreshActionClasses : function (){
        if(!dojo.byId("actionletsTbl")){
            return;
        }

        var hasBatchOnly = false;
        var tbody = dojo.byId("actionletsTblBody");
        dojo.empty(tbody);

        dojo.forEach(actionClassAdmin.actionClasses, function(entry, i){
            var tr = dojo.create("tr", {className:"dojoDndItem dndMyActionClasses", id:"myRow" + entry.id}, tbody);
            hasBatchOnly |= entry.isOnlyBatch;
            dojo.create("td", { innerHTML: "<span class='deleteIcon'></span>",className:"wfXBox", onClick:"actionClassAdmin.deleteActionClass('" + entry.id +"');" }, tr);
            dojo.create("td", { innerHTML: entry.name, onClick:"actionClassAdmin.manageParams('" + entry.id + "');", className:"showPointer" }, tr);
        });

        if(actionClassAdmin.actionClasses.length == 0){
            var tr = dojo.create("tr", null, tbody);
            dojo.create("td", { colSpan: 2, className:"wfnoSubActions", innerHTML:"<%=LanguageUtil.get(pageContext, "No-Sub-Actions-Configured")%>" }, tr);
        }

  	    if(hasBatchOnly) {
			actionClassAdmin.disableShowOnEditing();
		} else {

           actionClassAdmin.enableShowOnEditing();
		}

        actionClassAdmin.initDnD();
    },

    /**
     * Update order into the system
     */
	reorderActionClasses : function(source, nodes, copy){
		var actionClassId=source.anchor.id.replace("myRow", "");
		var order=0;
		var i = 0;
		var x = dojo.query("#actionletsTbl tbody tr").forEach(function(node){
			if(node.id == nodes[0].id){
				order=i;
			}
			i++;

		})

		var xhrArgs = {
				 url: "/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfActionClassAjax?cmd=reorder&actionClassId=" + actionClassId + "&order=" + order,
				handle : function(dataOrError, ioArgs) {

					if (dojo.isString(dataOrError)) {
						if (dataOrError.indexOf("FAILURE") == 0) {
							showDotCMSSystemMessage(dataOrError, true);
						}
						else{
							// We need to reorder the "Action Classes" array when an element is moved to a
							// different position
							actionClassAdmin.moveFromActionClasses(nodes[0].id.replace("myRow", ""), order);
						}
					} else {
						showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Unable-to-reorder")%>", true);
					}
				}
			};
			dojo.xhrGet(xhrArgs);

			return;
	},
	manageParams : function (actionClassId){

		var dia = dijit.byId("actionClassParamsDia");
		if(dia){
			dia.destroyRecursive();

		}
		dia = new dijit.Dialog({
			id			:	"actionClassParamsDia",
			title		: 	"<%=LanguageUtil.get(pageContext, "Sub-Action-Parameters")%>",
			href 		: "/html/portlet/ext/workflows/schemes/view_action_class_params.jsp?actionClassId=" + actionClassId
		});

		dojo.body().appendChild(dia.domNode);


		dia.show();



	},
	saveActionParameters : function() {

		var myForm = dijit.byId("ActionClassParamsFrm");

		if (myForm.validate()) {

			dojo.xhrPost({
				form : "ActionClassParamsFrm",
				timeout : 30000,
				handle : function(dataOrError, ioArgs) {
					if (dojo.isString(dataOrError)) {
						if (dataOrError.indexOf("FAILURE") == 0) {

							showDotCMSSystemMessage(dataOrError, true);
						} else {
							showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Saved")%>", false);
							dijit.byId("actionClassParamsDia").destroyRecursive();
						}
					} else {
						showDotCMSSystemMessage(dataOrError, true);

					}
				}
			});

		};
	}
});




var myRoleReadStore = new dotcms.dojo.data.RoleReadStore({nodeId: "actionAssignToSelect"});
var myRoleReadStore2 = new dotcms.dojo.data.RoleReadStore({nodeId: "whoCanUseSelect",includeWfRoles: "true"});
var myRoleReadStoreFilter = new dotcms.dojo.data.RoleReadStore({nodeId: "whoCanUseSelect", includeWfRoles: "true", includeLabelAll:true});

var myIconStore = new dojo.data.ItemFileReadStore({data:
	<%@ include file="/html/portlet/ext/workflows/schemes/workflow_icons.json" %>
});








var mainAdmin = new dotcms.dijit.workflows.MainAdmin({});
var schemeAdmin = new dotcms.dijit.workflows.SchemeAdmin({});
var stepAdmin = new dotcms.dijit.workflows.StepAdmin({});
var actionAdmin = new dotcms.dijit.workflows.ActionAdmin({});
var actionClassAdmin = new dotcms.dijit.workflows.ActionClassAdmin({});
dojo.ready(function() {
	var myHash = decodeURIComponent(dojo.hash());
	if(myHash && myHash.length > 0){
		mainAdmin.show(myHash);
	}else{
		schemeAdmin.show();
	}

});
