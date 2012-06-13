<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%response.setContentType("text/JavaScript");%>
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
        invalidMessage: "This field is required"
    }
);










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
			myCp.destroyRecursive(true);
		}
		myCp = new dojox.layout.ContentPane({
			id : this.baseDiv
		}).placeAt("hangWorkflowMainHere");

		myCp.attr("href", hashValue);
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
		// dojo.create("li",
		// {onClick:this.show(this.wfCrumbTrail[i].url)},crumbDiv )



		dojo.create("li", {innerHTML:"<span class='hostStoppedIcon' style='float:left;margin-right:5px;'></span><%=LanguageUtil.get(pageContext, "Global-Page")%>", id:"selectHostDiv", onClick:"window.location='/c'"},crumbDiv );




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
	showArchived : false,
	crumbTitle:"<%=LanguageUtil.get(pageContext, "Schemes")%>",
	addEditDiv:"wfEditSchemeDia",
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
			});



		myCp.placeAt("wfEditSchemeDia");

		dia.show();
		myCp.attr("href", href);

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
				}
			});

		}
		;

	},
	saveSuccess : function(message) {
		var dialog = dijit.byId(schemeAdmin.addEditDiv);
		dialog.hide();
		mainAdmin.refresh();
		showDotCMSSystemMessage("Saved");

	},
	saveError : function(message) {
		showDotCMSSystemMessage(message, true);

	},

	toggleInitialAction : function(){
		if( dijit.byId("schemeMandatory").getValue() ){
			dojo.style('forceInitialAction', 'display', '');
		}
		else{
			dojo.style('forceInitialAction', 'display', 'none');

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
	addSuccess : function (data){
		mainAdmin.refresh();
		showDotCMSSystemMessage("Added");
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
						showDotCMSSystemMessage(dataOrError, true);


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
			title		: 	"<%=LanguageUtil.get(pageContext, "Edit-Step")%>"
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
		myCp.attr("href","/html/portlet/ext/workflows/schemes/edit_step.jsp?stepId=" + stepId);
		setTimeout(function() {
		    showExpirationTime();
		},1000);
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
			x.hide();



		}
	}

});

function edit_step_toggleEscalation() {
    var dialogHeight=dojo.style(dojo.byId("stepEditDia"),'height');
    if(dijit.byId("enableEscalation").checked) {
        var newHeight=dialogHeight+120;
        dojo.style(dojo.byId("stepEditDia"),'height',newHeight+"px");
        dojo.query("#stepEditDia .escalation-row").style("display","table-row")
    }
    else {
        var newHeight=dialogHeight-120;
        dojo.style(dojo.byId("stepEditDia"),'height',newHeight+"px");
        dojo.query("#stepEditDia .escalation-row").style("display","none")

    }
}

function showExpirationTime(){
    var ttl = dijit.byId("escalationTime").getValue();

    var m = 60 * 60 * 24 * 30;
    var w = 60*60*24*7;
    var d = 60*60*24;
    var h = 60*60;
    var mm = 60;
    var message = "";
    var x = 0;
    while(ttl>0){
        if(x>0){
        message+=", ";
        }

        if(ttl>=m){
            x = Math.floor(ttl / m);
            message+= x;
            message+=(x==1) ? " <%= LanguageUtil.get(pageContext, "Month") %>"
                : " <%= LanguageUtil.get(pageContext, "Months") %>";
            ttl = Math.floor(ttl % m);
        }
        else if(ttl >= w){
            x = Math.floor(ttl / w);
            message+= x;
            message+=(x==1) ? " <%= LanguageUtil.get(pageContext, "Week") %>"
                : " <%= LanguageUtil.get(pageContext, "Weeks") %>";
            ttl = Math.floor(ttl % w);
        }
        else if(ttl >= d){
            x = Math.floor(ttl / d);
            message+= x;
            message+=(x==1) ? " <%= LanguageUtil.get(pageContext, "Day") %>"
                : " <%= LanguageUtil.get(pageContext, "Days") %>";
            ttl = Math.floor(ttl % d);
        }
        else if(ttl >= h){
            x = Math.floor(ttl / h);
            message+= x;
            message+=(x==1) ? " <%= LanguageUtil.get(pageContext, "Hour") %>"
                : " <%= LanguageUtil.get(pageContext, "Hours") %>";
            ttl = Math.floor(ttl % h);
        }
        else if(ttl >= mm){
            x = Math.floor(ttl / mm);
            message+= x;
            message+=(x==1) ? " <%= LanguageUtil.get(pageContext, "Minute") %>"
                : " <%= LanguageUtil.get(pageContext, "Minutes") %>";
            ttl = Math.floor(ttl % mm);
        }
        else if(ttl > 0){
            x =ttl;
            message+= x;
            message+=(x==1) ? " <%= LanguageUtil.get(pageContext, "Second") %>"
                : " <%= LanguageUtil.get(pageContext, "Seconds") %>";
            ttl=0;

        }
    }

    dojo.byId("showExpirationTime").innerHTML = message;
}




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
		//console.log(movedId);
		var stepId = movedId.split("_")[2];
		var actionId = movedId.split("_")[1];
		var i=0;
		dojo.query("#jsNode" + stepId + " tr").forEach(function(node){



			if(node.id == movedId ){
				var xhrArgs = {
					url: "/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfActionAjax?cmd=reorder&actionId=" + actionId + "&order=" + i,
					handle : function(dataOrError, ioArgs) {
						if (dojo.isString(dataOrError)) {
							if (dataOrError.indexOf("FAILURE") == 0) {

								// schemeAdmin.saveError(dataOrError);
							} else {
								// schemeAdmin.saveSuccess(dataOrError);
							}
						} else {
							//this.saveError("<%=LanguageUtil.get(pageContext, "unable-to-save-action")%>");

						}
					}
				};
				dojo.xhrPut(xhrArgs);
				return;
			}
			i++;

		})
	},

	deleteAction : function (actionId){
		if(!confirm("<%=LanguageUtil.get(pageContext, "Confirm-Delete-Action")%>")){
			return;
		}
		var xhrArgs = {
			 url: "/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfActionAjax?cmd=delete&actionId=" + actionId ,
			handle : function(dataOrError, ioArgs) {
				if (dojo.isString(dataOrError)) {


					if (dataOrError.indexOf("FAILURE") == 0) {
						showDotCMSSystemMessage(dataOrError, true);


					} else {
						actionAdmin.deleteSuccess(dataOrError);
					}
				} else {
					this.saveError("<%=LanguageUtil.get(pageContext, "unable-to-save-scheme")%>");

				}
			}
		};
		dojo.xhrPut(xhrArgs);

		return;
	},
	deleteSuccess : function(message) {
		console.log(message);
		console.log(message.split(":")[1]);
		stepAdmin.showViewSteps(message.split(":")[1]);

		showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Deleted")%>");

	},

	viewAction : function(stepId, actionId) {
		mainAdmin.show(this.baseJsp + "?stepId=" + stepId + "&actionId=" + actionId + "&" + Math.random());

	},

	saveAction : function(stepId) {

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
							actionAdmin.viewAction(stepId, actionId);
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
		var select = dijit.byId("whoCanUseSelect");

		var user = select.getValue();
		var userName = select.attr('displayedValue');

		actionAdmin.addToWhoCanUse(user, userName);
		actionAdmin.refreshWhoCanUse();
		actionAdmin.doChange();
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
          		dojo.style("divRoleHierarchyForAssign", "visibility", "hidden");
			}
        	else{
          		dojo.style("divRoleHierarchyForAssign", "visibility", "visible");
      		}
      	}else{
       		dojo.style("divRoleHierarchyForAssign", "visibility", "hidden");
      	}
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
	actionClasses : new Array(),
	dndHandle : null,
	addSelectedToActionClasses : function(){
		var select = dijit.byId("wfActionlets");

		var clazz = select.getValue();
		var name = select.attr('displayedValue');

		this.addActionClass(clazz, name);

	},

	addToActionClassesArray: function ( id, myName){

		var entry = {id:id,name:myName};
		this.actionClasses[this.actionClasses.length] =entry;

	},



	addActionClass : function ( clazz, myName){
		var actionId = dojo.byId("actionId").value;
		var xhrArgs = {
				url: "/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfActionClassAjax?cmd=add&actionId=" +actionId + "&actionletClass=" + clazz + "&actionletName=" + encodeURIComponent(myName),
				handle : function(dataOrError, ioArgs) {
					if (dojo.isString(dataOrError)) {
						if (dataOrError.indexOf("FAILURE") == 0) {
							showDotCMSSystemMessage(dataOrError, true);

						} else {

							var x = dataOrError.split(":",2);
							actionClassAdmin.addToActionClassesArray(x[0], x[1]);
							actionClassAdmin.refreshActionClasses();
							showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Added")%>", false);
							// actionAdmin.doChange();
						}
					} else {
						showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Unable-to-save-subaction")%>", true);


					}
				}
			};
			dojo.xhrPut(xhrArgs);


	},


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


	removeFromActionClasses: function (id){
		var x=0;
		var newActionlets = new Array();
		for(i=0;i < this.actionClasses.length;i++){
			if(id != this.actionClasses[i].id){
				newActionlets[x] = this.actionClasses[i];
				x++;
			}

		}
		this.actionClasses= newActionlets;
		this.refreshActionClasses();

	},

	refreshActionClasses : function (){
		if(!dojo.byId("actionletsTbl")){
			return;
		}


		dojo.empty("actionletsTbl");
		var table = dojo.byId("actionletsTbl");
		var x = "";
		/***********************************************************************
		 * this.actionlets = this.actionlets.sort(function(a,b){ var x =
		 * a.name.toLowerCase(); var y = b.name.toLowerCase(); return ((x < y) ?
		 * -1 : ((x > y) ? 1 : 0)); });
		 **********************************************************************/

		var tr = dojo.create("tr", null, table);
		dojo.create("th", {colspan:2, innerHTML:"SubActions "}, tr);
		var tbody = dojo.create("tbody", null, table);


		for(i=0; i< this.actionClasses.length ; i++){

			x = x + this.actionClasses[i].id + ",";
			tr = dojo.create("tr", {className:"dojoDndItem", id:"myRow" +  this.actionClasses[i].id}, tbody);
			dojo.addClass(tr, "dndMyActionClasses");
			dojo.create("td", { innerHTML: "<span class='deleteIcon'></span>",className:"wfXBox", onClick:"actionClassAdmin.deleteActionClass('" + this.actionClasses[i].id +"');actionClassAdmin.refreshActionClasses()" }, tr);
			dojo.create("td", { innerHTML: this.actionClasses[i].name, onClick:"actionClassAdmin.manageParams('" + this.actionClasses[i].id +"');", className:"showPointer" }, tr);

		}
		if(this.actionClasses.length ==0){

			tr = dojo.create("tr", null, tbody);
			dojo.create("td", { colSpan: 2, className:"wfnoSubActions", innerHTML:"<%=LanguageUtil.get(pageContext, "No-Sub-Actions-Configured")%>" }, tr);
		}



		var c1 = new dojo.dnd.Container(dojo.byId("actionletsTbl"));
		var myDnD = new dojo.dnd.Source("actionletsTbl");
		this.dndHandle = dojo.connect(myDnD, "onDndDrop", actionClassAdmin.reorderActionClasses);
	},


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
		console.log("id:" + actionClassId);
		console.log("order:" + order);

		var xhrArgs = {
				 url: "/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfActionClassAjax?cmd=reorder&actionClassId=" + actionClassId + "&order=" + order,
				handle : function(dataOrError, ioArgs) {

					if (dojo.isString(dataOrError)) {

						if (dataOrError.indexOf("FAILURE") == 0) {

							showDotCMSSystemMessage(dataOrError, true);
						}
						else{
							//showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Reordered")%>", false);

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
var myRoleReadStore2 = new dotcms.dojo.data.RoleReadStore({nodeId: "whoCanUseSelect"});

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






