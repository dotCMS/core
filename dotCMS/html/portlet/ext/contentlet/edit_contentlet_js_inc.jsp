<%@page import="java.util.Random"%>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="java.util.*"%>

<%

String catCount = (String) request.getAttribute("counter");

%>

<script language='javascript' type='text/javascript'>
dojo.require("dojox.layout.ContentPane");
	//http://jira.dotmarketing.net/browse/DOTCMS-2273
	var workingContentletInode = "<%= contentlet.getInode() %>";
	var currentContentletInode = "";
	var isAutoSave = false;
	var isCheckin = true;
	var isContentAutoSaving = false;
	var isContentSaving = false;
	var doesUserCancelledEdit = false;

	var tabsArray=new Array();

	dojo.require("dijit.Dialog");

	assignToDialog = new dijit.Dialog({
	      id:"assignToDialog"
	  });


	//Tabs manipulation
	function displayProperties(id) {

		for(i =0;i< tabsArray.length ; i++){
			var ele = document.getElementById(tabsArray[i] + "_tab");

			if (ele != undefined) {
				if(tabsArray[i] == id){
				  ele.className = "alpha";
		          document.getElementById(tabsArray[i]).style.display = "";
				}
				else{
                 ele.className = "beta";
				 document.getElementById(tabsArray[i]).style.display = "none";
				}
			}
		}
	}







    var myForm = document.getElementById('fm');
	var copyAsset = false;

	var inode = '<%=contentlet.getInode()%>';
	var referer = '<%=java.net.URLEncoder.encode(referer,"UTF-8")%>';

	function cancelEdit() {

		doesUserCancelledEdit = true;

		if(isContentAutoSaving){ //To avoid storage of contentlet while user cancels.
			setTimeout("cancelEdit()",300);
		}

		var ref = $('<portlet:namespace />referer').value;
		var langId = $('languageId').value;

		ContentletAjax.cancelContentEdit(workingContentletInode,currentContentletInode,ref,langId,cancelEditCallback);
	}
	function cancelEditCallback(callbackData){
		<%if(structure.getStructureType()==Structure.STRUCTURE_TYPE_FORM){%>
		//callbackData=callbackData+"&inode=<%=contentlet.getInode()%>&structure_id=<%=structure.getInode()%>";
		<%}%>
		self.location = callbackData;
	}


	//Full delete contentlet action
	function submitfmDelete()
	{
			var href =  '<portlet:actionURL>';
			href +=		'	<portlet:param name="struts_action" value="<%= formAction %>" />';
			href +=		'	<portlet:param name="cmd" value="full_delete" />';
			href +=		'</portlet:actionURL>';

			form = document.getElementById("fm");
			form.action = href;
			form.submit();
	}

	//Versions management
    function deleteVersion(objId){
        if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.contentlet.delete.content.version")) %>')){
    		var xhrArgs = {
    				url: '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="<%= formAction %>" /></portlet:actionURL>&cmd=deleteversion&inode=' + objId  + '&referer=' + referer,
    				handle : function(dataOrError, ioArgs) {


    					refreshVersionCp();

    					showDotCMSSystemMessage("<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.contentlet.full_delete")) %>");

    				}
    			};
    			dojo.xhrGet(xhrArgs);



        }
    }
	function selectVersion(objId) {
        if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.contentlet.replace.version")) %>')){
			window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="<%= formAction %>" /></portlet:actionURL>&cmd=getversionback&inode=' + objId + '&inode_version=' + objId  + '&referer=' + referer;
	    }
	}
	function editVersion(objId) {
		window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="<%= formAction %>" /></portlet:actionURL>&cmd=edit&inode=' + objId  + '&referer=' + referer;
	}



   	function openAssignTo()
   	{
		if(dojo.isFF){
			  assignToDialog._fillContent(dojo.byId('assignTaskInnerDiv'));
			  assignToDialog.show();
   	  	}
   	  	else{
   	  		var win=dijit.byId('assignTaskDiv');

			win.show();
   		}

	}

	function cancelAssignTo() {
		if(dojo.isFF){
			assignToDialog.hide();
   	   	}
   	   	else{
   	   		dijit.byId('assignTaskDiv').hide();
   	   	}
	}




	//Structure change
  	function structureSelected()
	{
		//migrateSelectedStructure();
		var href = "<portlet:actionURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>";
		href += "<portlet:param name='struts_action' value='<%= formAction %>' />";
		href += "<portlet:param name='cmd' value='new' />";
		href += "<portlet:param name='referer' value='<%=referer%>' />";
		//This parameter is used to determine if the selection comes from Content Manager is EditContentletAction
		href += "<portlet:param name='selected' value='true' />";
		href += "<portlet:param name='inode' value='' />";
		href += "</portlet:actionURL>";
		document.forms[0].action = href;
		document.forms[0].submit();
	}


	//Review content changes control
	function reviewChange() {
		var obj = dijit.byId("reviewContent");
		enable = obj.checked;
		<%if (UtilMethods.isSet(structure.getReviewerRole())) {%>

			var reviewContentDate = document.getElementById("reviewContentDate");
			if(reviewContentDate == undefined){
				return;
			}
			var reviewIntervalNum = document.getElementById("reviewIntervalNumId");
			var reviewIntervalSel = document.getElementById("reviewIntervalSelectId");
			if (enable) {
				reviewContentDate.style.display='';
				reviewIntervalNum.disabled = false;
				reviewIntervalSel.disabled = false;
			} else {
				reviewContentDate.style.display='none';
				reviewIntervalNum.disabled = true;
				reviewIntervalSel.disabled = true;
			}

		<%}%>
	}

	//Copy contentlet action
	function copyContentlet()
	{
			var href =  '<portlet:actionURL>';
			href +=		'<portlet:param name="struts_action" value="<%= formAction %>" />';
			href +=		'<portlet:param name="cmd" value="copy" />';
			href +=		'</portlet:actionURL>';

			form = document.getElementById("fm");
			form.action = href;
			form.submit();
	}

	function editContentletURL (objId) {
		var loc = '';
		loc += '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="<%= formAction %>" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&inode=' + objId;
		return loc;
	}



    function addTab(tabid){
      tabsArray.push(tabid);
	}



	function submitParent(param) {
		if (copyAsset) {
			disableButtons(myForm);
			var parent = document.getElementById("parent").value;
			self.location = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/contentlet/edit_contentlet" /><portlet:param name="cmd" value="copy" /><portlet:param name="inode" value="<%=String.valueOf(contentlet.getInode())%>" /></portlet:actionURL>&parent=' + parent + '&referer=' + referer;
		}

		//WYSIWYG_CALLBACKS
		if (param == 'wysiwyg_image') {
			var imageName = document.getElementById("selectedwysiwyg_image").value;
			var imageFolder = document.getElementById("folderwysiwyg_image").value;
			var ident = document.getElementById("selectedIdentwysiwyg_image").value;
			wysiwyg_win.document.forms[0].elements[wysiwyg_field_name].value = /dotAsset/ + ident;
			wysiwyg_win.ImageDialog.showPreviewImage("/dotAsset/" + ident);
		}
		if (param == 'wysiwyg_file') {
			var fileName = document.getElementById("selectedwysiwyg_file").value;
			var ident = document.getElementById("selectedIdentwysiwyg_file").value;
			var fileFolder = document.getElementById("folderwysiwyg_file").value;
			<% String ext = com.dotmarketing.util.Config.getStringProperty("VELOCITY_PAGE_EXTENSION"); %>
			var fileExt = getFileExtension(fileName).toString();
			if(fileExt == '<%= ext %>'){
				wysiwyg_win.document.forms[0].elements[wysiwyg_field_name].value = fileFolder + fileName;
			}else{
				wysiwyg_win.document.forms[0].elements[wysiwyg_field_name].value = /dotAsset/ + ident;
			}
		}
	}



	<% if(Config.getIntProperty("CONTENT_AUTOSAVE_INTERVAL",0) > 0){%>
		// http://jira.dotmarketing.net/browse/DOTCMS-2273
		var autoSaveInterval = <%= Config.getIntProperty("CONTENT_AUTOSAVE_INTERVAL",0) %>;
		setInterval("saveContent(true)",autoSaveInterval);
	<%}%>


	function getFormData(formId,nameValueSeparator){ // Returns form data as name value pairs with nameValueSeparator.

		var formData = new Array();

		//Taking the text from all the textareas
		var k = 0;
		$(document.getElementById(formId)).getElementsBySelector('textarea').each(
				function (textareaObj) {
					if ((textareaObj.id != "") && (codeMirrorEditors[textareaObj.id] != null)) {
						try {
							document.getElementById(textareaObj.id).value=codeMirrorEditors[textareaObj.id].getCode();
						} catch (e) {
						}
					}
				}
			);

		var formElements = document.getElementById(formId).elements;

		var formDataIndex = 0; // To collect name/values from multi-select,text-areas.

		for(var formElementsIndex = 0; formElementsIndex < formElements.length; formElementsIndex++,formDataIndex++){


			// Collecting only checked radio and checkboxes
			if((formElements[formElementsIndex].type == "radio") && (formElements[formElementsIndex].checked == false)){
				continue;
			}

			if((formElements[formElementsIndex].type == "checkbox") && (formElements[formElementsIndex].checked == false)){
				continue;
			}

			// Collecting selected values from multi select
			if(formElements[formElementsIndex].type == "select-multiple") {
				for(var multiSelectIndex = 0; multiSelectIndex < formElements[formElementsIndex].length; multiSelectIndex++){

					if(formElements[formElementsIndex].options[multiSelectIndex].selected == true){
						formData[formDataIndex] = formElements[formElementsIndex].name+nameValueSeparator+formElements[formElementsIndex].options[multiSelectIndex].value;
						formDataIndex++;
					}
				}
				continue;
			}

			// Getting values from text areas
			if(formElements[formElementsIndex].type == "textarea" && formElements[formElementsIndex].id != '') {
				if(tinyMCE.get(formElements[formElementsIndex].id) != null){
					textAreaData = tinyMCE.get(formElements[formElementsIndex].id).getContent();
					formData[formDataIndex] = formElements[formElementsIndex].name+nameValueSeparator+textAreaData;
					continue;
				}
				if(tinyMCE.get(formElements[formElementsIndex].id) == null){
						textAreaData = document.getElementById(formElements[formElementsIndex].id).value;
						formData[formDataIndex] = formElements[formElementsIndex].name+nameValueSeparator+textAreaData;
						continue;
				}
			}

			formData[formDataIndex] = formElements[formElementsIndex].name+nameValueSeparator+formElements[formElementsIndex].value;
		}

		// Categories selected in the Category Dialog

		var catCount = <%=UtilMethods.isSet(catCount)?Integer.parseInt(catCount):0 %>;


		for(var i=1; i<catCount+1; i++) {

			if (typeof eval("addedStore"+i)!="undefined"){
				eval("addedStore"+i).fetch({onComplete:function(items) {
					for (var i = 0; i < items.length; i++){
						formData[formData.length] = "categories"+nameValueSeparator+items[i].id[0];
					   }
				}});
			}

		}

		return formData;

	}



	function publishContent(){
		persistContent(isAutoSave, true);

	}

	function saveContent(isAutoSave){

		persistContent(isAutoSave, false);
	}


	function persistContent(isAutoSave, publish){

	   	var isAjaxFileUploading = false;
	   	dojo.query(".fajaxUpName").forEach(function(node, index, arr){
	   		FileAjax.getFileUploadStatus(node.id,{async:false, callback: function(fileStats){
	   	   		if(fileStats!=null){
	   	   		   isAjaxFileUploading = true;
	   	   		}
	   	   	}});
	   	 });

	   	if(isAjaxFileUploading){
	   	   alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Please-wait-until-all-files-are-uploaded")) %>');
	       return false;
	   	}

	   	if(doesUserCancelledEdit){
			return false;
	   	}

		if(isContentAutoSaving){ // To avoid concurrent auto and normal saving.
			return;
		}
		window.scrollTo(0,0);	// To show lightbox effect(IE) and save content errors.
		dijit.byId('savingContentDialog').show();



		if(isAutoSave && isContentSaving){
			return;
		}
		var textAreaData = "";
		var fmData = new Array();

		fmData = getFormData("fm","<%= com.dotmarketing.util.WebKeys.CONTENTLET_FORM_NAME_VALUE_SEPARATOR %>");

		if(isInodeSet(currentContentletInode)){
			isCheckin = false;
			isAutoSave=false;
		}

		if(isAutoSave){
			isContentAutoSaving = true;
		}else {
			isContentSaving = true;
		}


		ContentletAjax.saveContent(fmData,isAutoSave,isCheckin,publish,saveContentCallback);
	}







	function createLockedWarningDiv(){

		//handle if the node is locked &&  editable and node is locked &! editable
		var lockedNode = dojo.byId("lockContentButton") ;
		var destroyAfterClick = true;
		var endColor = "#ffffff";
		if(!lockedNode){
			if(!dojo.byId("unlockContentButton")){
				lockedNode = dojo.byId("lockedTextInfoDiv");
				endColor = "#eeeeee";
				destroyAfterClick = false;
			}
		}


		if(lockedNode){
			dojo.query(".wrapperRight").forEach(function(node){
				var n = dojo.create("div", {className:"lockedWarningDiv"}, node, "first");
				dojo.connect(n, "onmousedown", null,function(){
	    			dojo.query(".lockedWarningDiv").forEach(function(node){
	    				if(destroyAfterClick){
	    					dojo.destroy(node);
	    				}
	    			});
				 	dojo.animateProperty({
			            node: lockedNode,
			            duration: 1000,
			            properties: {
			                backgroundColor: {
			                    start: "#FA5858",
			                    end: endColor
			                },

			            },
			            onEnd: function() {

			            }
			        }).play();
				});

			})
		}
		else{
			dojo.query(".lockedWarningDiv").forEach(function(node){
				dojo.destroy(node);
			});
		}
	}




	dojo.ready(
		function(){
			createLockedWarningDiv();
			resetHasChanged();
		}
	);

	var _bodyKeyDown;
	var _bodyMouseDown;
	var _hasUserChanged  = false;

	function resetHasChanged(){
		_hasUserChanged  = false;
    	_bodyKeyDown = dojo.connect(dojo.body(), "onkeydown", null,markHasChanged);

    	dojo.query(".wrapperRight").forEach(function(node){
    		_bodyMouseDown = dojo.connect(node, "onmousedown", null,markHasChanged);
    		return;
    	})
	}

    function markHasChanged(){

    	_hasUserChanged  = true;
    	dojo.disconnect(_bodyKeyDown);
    	dojo.disconnect(_bodyMouseDown);

    }


	function saveContentCallback(data){
		isContentAutoSaving = false;
		dojo.byId("subcmd").value="";
		if(data["contentletInode"] != null && isInodeSet(data["contentletInode"])){
			$('contentletInode').value = data["contentletInode"];
			currentContentletInode = data["contentletInode"];
		}
		dijit.byId('savingContentDialog').hide();


		// Show DotContentletValidationExceptions.
		if(data["saveContentErrors"] != null ){
			var errorDisplayElement = dijit.byId('saveContentErrors');
			var exceptionData = data["saveContentErrors"];
			var errorList = "";
				for (var i = 0; i < exceptionData.length; i++) {
					var error = exceptionData[i];
					errorList = errorList+"<li>"+error+"</li>";
				}
			dojo.byId('exceptionData').innerHTML = "<ul>"+errorList+"</ul>";

			errorDisplayElement.show();
			return;
		}




		// if we have a referer and the contentlet comes back checked in
	 	if(data["referer"] != null && data["referer"] != '' && !data["contentletLocked"] ) {

			<%if(structure.getStructureType()==Structure.STRUCTURE_TYPE_FORM){%>
				//self.location = data["referer"]+"&structure_id=<%=structure.getInode()%>&content_inode=" + data["contentletInode"];
			<%}else{%>
				self.location = data["referer"] + "&content_inode=" + data["contentletInode"];
			<%}%>
			return;
		}
	 	resetHasChanged();
  	  	refreshActionPanel(data["contentletInode"]);

	}
	function hideEditButtonsRow() {
		dojo.style(editButtonRow, { display: 'none' });
	}

	function showEditButtonsRow() {
		if( typeof changesMadeToPermissions!= "undefined"){
			if(changesMadeToPermissions == true){
				dijit.byId('applyPermissionsChangesDialog').show();
			}
		}
		dojo.style(editButtonRow, { display: '' });
		changesMadeToPermissions = false;
	}

	function  resizeBrowser(){
        var viewport = dijit.getViewport();
        var viewport_height = viewport.h;

        var  e =  dojo.byId(editButtonRow);
		dojo.style(e, "height", viewport_height - 185 + "px");

		dojo.query(".wrapperRight").forEach(function(node, index, arr){
		 	dojo.style(node, "height", viewport_height - 185 + "px");
		});

    }

    dojo.addOnLoad(resizeBrowser);
    dojo.connect(window, "onresize", this, "resizeBrowser");

    function refreshVersionCp(){
    	var x = dijit.byId("versions");
    	var y =Math.floor(Math.random()*1123213213);




    	var myCp = dijit.byId("contentletVersionsCp");
		if (myCp) {
			myCp.attr("href", "/html/portlet/ext/contentlet/contentlet_versions_inc.jsp?contentletId=" +contentAdmin.contentletIdentifier + "&r=" + y);
			return;
		}
    	var myDiv = dijit.byId("contentletVersionsDiv");
		if (myDiv) {
			dojo.empty(myDiv);
		}
		myCp = new dijit.layout.ContentPane({
			id : "contentletVersionsCp",
			style: "height:600px",
			href: "/html/portlet/ext/contentlet/contentlet_versions_inc.jsp?contentletId=" +contentAdmin.contentletIdentifier + "&r=" + y
		}).placeAt("contentletVersionsDiv");
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


    	executeWfAction: function(wfId, assignable, commentable){
    		this.wfActionId=wfId;

    		if(assignable || commentable){
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
    			var inode= (currentContentletInode != undefined && currentContentletInode.length > 0) 
    					? currentContentletInode
   							:workingContentletInode;



    			myCp.attr("href", "/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfTaskAjax?cmd=renderAction&actionId=" + wfId + "&inode=" + inode);
    			return;
    		}
    		else{
        		dojo.byId("wfActionId").value=wfId;
        		saveContent(false);
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
    		var dia = dijit.byId("contentletWfDialog").hide();

    		saveContent(false);

    	}

    });


    var contentAdmin = new dotcms.dijit.contentlet.ContentAdmin('<%= contentlet.getIdentifier() %>','<%= contentlet.getInode() %>','<%= contentlet.getLanguageId() %>');

    function makeEditable(contentletInode){
		ContentletAjax.lockContent(contentletInode, checkoutContentletCallback);
		dojo.empty("contentletActionsHanger");
		dojo.byId("contentletActionsHanger").innerHTML="<div style='text-align:center;padding-top:20px;'><span class='dijitContentPaneLoading'></span></div>";

    }





    function checkoutContentletCallback(data){

    	if(data["Error"]){
    		showDotCMSSystemMessage(data["Error"], true);
    		return;
    	}


  	  	refreshActionPanel(data["lockedIdent"]);

    }


    function stealLock(contentletInode){
    	ContentletAjax.unlockContent(contentletInode, stealLockContentCallback);
   }
  function stealLockContentCallback(data){

	  	if(data["Error"]){
			showDotCMSSystemMessage(data["Error"], true);
			return;
		}


	  	refreshActionPanel(data["lockedIdent"]);

    }




    function unlockContent(contentletInode){

    	if(_hasUserChanged){
    		if(!confirm("<%=LanguageUtil.get(pageContext, "checkin-without-saving-changes")%>")){
    			return;
    		};
    	}
    	ContentletAjax.unlockContent(contentletInode, unlockContentCallback);
		//dojo.empty("contentletActionsHanger");
		//dojo.byId("contentletActionsHanger").innerHTML="<div style='text-align:center;padding-top:20px;'><span class='dijitContentPaneLoading'></span></div>";

    }


  function unlockContentCallback(data){

	  	if(data["Error"]){
			showDotCMSSystemMessage(data["Error"], true);
			return;
		}

	  	window.location="<%=referer%>";


    }



  function refreshActionPanel(inode){

		var myCp = dijit.byId("contentActionsCp");
		if (myCp) {
			myCp.destroyRecursive(true);
		}

		dojo.empty("contentletActionsHanger");
		myCp = new dojox.layout.ContentPane({
			id 			: "contentActionsCp"

		}).placeAt("contentletActionsHanger");

		myCp.attr("href",  "/html/portlet/ext/contentlet/contentlet_actions_wrapper.jsp?contentletInode=" + inode);

  }

	function toggleLockedMessage(locked, who, when){
		  if(dojo.byId("contentLockedInfo")){
			  if (locked) {
					dojo.empty("contentLockedInfo");
					myTR  = dojo.byId("contentLockedInfo");
					dojo.create("th", {innerHTML:'<%= LanguageUtil.get(pageContext, "Locked") %>:'}, myTR);
					dojo.create("td", {innerHTML: who + ' : <span class="lockedAgo">(' + when + ')</span>'}, myTR);

					if(dojo.byId("contentLockedInfo").getHeight() <30){
						dojo.animateProperty({
					         node: dojo.byId("contentLockedInfo"),
					         duration: 300,
					         properties: {
					             height: {
					                 start: 0,
					                 end: 30
					             },

					         }
						}).play();
					}
				}else {
					if(dojo.byId("contentLockedInfo").getHeight() >29){
						dojo.animateProperty({
						         node: dojo.byId("contentLockedInfo"),
						         duration: 300,
						         properties: {
						             height: {
						                 start: 30,
						                 end: 0
						             },

						         },
					            onEnd: function() {
					            	dojo.empty("contentLockedInfo");
					            }
						}).play();
					}
				}
			}
		}



</script>