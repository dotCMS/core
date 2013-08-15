	<%@ page import="com.dotmarketing.util.UtilMethods" %>

	var referer = '<%=referer%>';

	function submitfm(form,subcmd) {

			var numContentlets = parseInt(dijit.byId("maxContentlets").value);

			if (numContentlets > 0){

				var inputSts = document.createElement('input');
			    inputSts.type = 'hidden';
			    inputSts.name = 'structuresIds';
			    inputSts.id = 'structuresIds';

			    var tabChildren = dijit.byId("tabContainer").getChildren();

				structuresAdded = new Array();

			    tabChildren.forEach(function(widget, index, hash){
					var structureInode = widget.id.split("_")[1];
					structuresAdded.push(structureInode);
				});

				if(structuresAdded.length==0) {
					alert('Must Add at Least one Content Type when Max Contentlets is greater than zero')
				}

				for(var i=0; i < structuresAdded.length; i++) {

					var input = document.createElement('input');
				    input.type = 'hidden';
				    input.name = 'code_' + structuresAdded[i];
				    input.id = 'code_' + structuresAdded[i];

					if(aceMultiEditor[structuresAdded[i]]!=null) {
						dojo.byId("codeMaskMulti"+structuresAdded[i]).value = aceMultiEditor[structuresAdded[i]].getValue();
	   				}

	   				input.value = dojo.byId("codeMaskMulti"+structuresAdded[i]).value;
	   				form.appendChild(input);

	   				inputSts.value = inputSts.value + structuresAdded[i] + "#";
				}

				form.appendChild(inputSts);

			} else {
				if(dijit.byId("toggleEditorCode").checked){
					document.getElementById("codeMask").value=aceEditor.getValue();
				}

				document.getElementById("code").value = document.getElementById("codeMask").value;
			}



			if(dijit.byId("toggleEditorPreLoop").checked && numContentlets > 0){
				document.getElementById("preLoopMask").value=preLoopEditor.getValue();
			}
			else if(numContentlets == 0){
				document.getElementById("preLoopMask").value = "";
			}
			if(dijit.byId("toggleEditorPostLoop").checked && numContentlets > 0){
				document.getElementById("postLoopMask").value=postLoopEditor.getValue();
			}else if(numContentlets == 0){
				document.getElementById("postLoopMask").value="";
			}

			//DOTCMS-5415
			document.getElementById("preLoop").value = document.getElementById("preLoopMask").value;

			document.getElementById("postLoop").value = document.getElementById("postLoopMask").value;

			form.<portlet:namespace />cmd.value = '<%=Constants.ADD%>';
			form.<portlet:namespace />subcmd.value = subcmd;
			form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/containers/edit_container" /></portlet:actionURL>';
			submitForm(form);
	}

	var copyAsset = false;

	function cancelEdit() {
		self.location = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/containers/edit_container" /><portlet:param name="cmd" value="unlock" /><portlet:param name="inode" value="<%=String.valueOf(contentContainer.getInode())%>" /></portlet:actionURL>&referer=' + referer;
	}

	function addVariable() {

		var selectedTab = dijit.byId('tabContainer').selectedChildWidget;
		var structureInode = selectedTab.id.split("_")[1];

<!-- 		var structureInode = dijit.byId("structureSelect").attr('value'); -->
		dijit.registry.remove('variablesDialog');
	 	new dijit.Dialog({
	 		id: 'variablesDialog',
      		title: "<%= LanguageUtil.get(pageContext, "Add-Variables") %>",
      		href: "/html/portlet/ext/containers/add_variables.jsp?structureInode=" + structureInode,
      		style: "width: 450px; height: 500px;"
  		}, 'variablesDialog').show();
	}

	function insertAtCursor( myValue, myFieldName) {

		var selectedTab = dijit.byId('tabContainer').selectedChildWidget;
		var structureInode = selectedTab.id.split("_")[1];

		myField = document.getElementById(myFieldName);

		if(myFieldName=="codeMask") {
			var acetId = document.getElementById('aceEditor');
        	if(acetId.className.indexOf("show") == 0) {
        		var pos= aceEditor.getCursorPosition();
				aceEditor.getSession().insert(pos, myValue);
			} else {
				myField.value=myField.value+myValue;
			}
		} else if(myFieldName=="codeMaskMulti") {

			myField = dojo.byId("codeMaskMulti"+structureInode);
			var acetId = document.getElementById('aceMaskMulti'+structureInode);
			
        	if(acetId.className.indexOf("show") == 0) {
        		var pos= aceMultiEditor[structureInode].getCursorPosition();
				aceMultiEditor[structureInode].getSession().insert(pos, myValue);
        	} else {
        		myField.value=myField.value+myValue;
        	}

		} else if(myFieldName=="preLoopMask") {
			var acetId = document.getElementById('preLoopAceEditor');
			if(acetId.className.indexOf("pShow") == 0) {
				var pos= preLoopEditor.getCursorPosition();
			    preLoopEditor.getSession().insert(pos, myValue);
            } else {
				myField.value=myField.value+myValue;
            }
		} else if(myFieldName=="postLoopMask") {
			var acetId = document.getElementById('postLoopAceEditor');
			if(acetId.className.indexOf("pShow") == 0) {
				var pos= postLoopEditor.getCursorPosition();
			    postLoopEditor.getSession().insert(pos, myValue);
            } else {
            	myField.value=myField.value+myValue;
            }
		} else {
				myField.value=myField.value+myValue
		}
	}

	function add(x){
		insertAtCursor("$!{" + x + "}\n", 'codeMaskMulti');
		dijit.byId('variablesDialog').hide();
	}

	function addImage(velocityVarName){
		var insert = "#if ($UtilMethods.isSet($" + "{" + velocityVarName+"ImageURI})) \n   <img src=\"$!{"+velocityVarName+"ImageURI}\" alt=\"$!{"+velocityVarName+"ImageTitle}\"  /> \n#end \n";
		insertAtCursor(insert, "codeMaskMulti");
		dijit.byId('variablesDialog').hide();
	}

	function addLink(velocityVarName) {
		var insert = "#if ($" + "{" + velocityVarName+"LinkURL}) \n   <a href=\"$!{"+velocityVarName+"LinkProtocol}$!{"+velocityVarName+"LinkURL}\" target=\"$!{"+velocityVarName+"LinkTarget}\">$!{"+velocityVarName+"LinkTitle}</a> \n#end \n";
		insertAtCursor(insert, "codeMaskMulti");
		dijit.byId('variablesDialog').hide();
	}

	function addFile(velocityVarName) {
		var insert = "#if ($" + "{" + velocityVarName+"FileURI}) \n   <a href=\"$!{"+velocityVarName+"FileURI}\">$!{"+velocityVarName+"FileTitle}</a> \n#end \n";
		insertAtCursor(insert, "codeMaskMulti");
		dijit.byId('variablesDialog').hide();
	}
	function addBinaryFile(velocityVarName) {
		var insert = "#if ($UtilMethods.isSet($" + "{" + velocityVarName+"BinaryFileURI})) \n   <a href=\"$!{"+velocityVarName+"BinaryFileURI}?force_download=1&filename=$!{"+velocityVarName+"BinaryFileTitle}\">$!{"+velocityVarName+"BinaryFileTitle}</a> \n#end \n";
		insertAtCursor(insert, "codeMaskMulti");
		dijit.byId('variablesDialog').hide();
	}


	function addBinaryResize(velocityVarName) {
		var insert = "#if ($UtilMethods.isSet($" + "{" + velocityVarName+"BinaryFileURI})) \n   <img src=\"/contentAsset/resize-image/${ContentIdentifier}/" + velocityVarName + "?w=150&h=100\" />\n#end \n";
		insertAtCursor(insert, "codeMaskMulti");
		dijit.byId('variablesDialog').hide();
	}

	function addBinaryThumbnail(velocityVarName) {
		var insert = "#if ($UtilMethods.isSet($" + "{" + velocityVarName+"BinaryFileURI})) \n   <img src=\"/contentAsset/image-thumbnail/${ContentIdentifier}/" + velocityVarName + "?w=150&h=150\" />\n#end \n";
		insertAtCursor(insert, "codeMaskMulti");
		dijit.byId('variablesDialog').hide();
	}



	function addTextField(velocityVarName) {
		var insert = "<input type=\"text\" name=\"" + velocityVarName + "\" id=\"" + velocityVarName + "\" value=\"$!{" + velocityVarName + "}\"> \n";
		insertAtCursor(insert, "codeMaskMulti");
		dijit.byId('variablesDialog').hide();
	}

	function addInodeField(velocityVarName) {
		var insert = "$!{" + velocityVarName + "}\n";
		insertAtCursor(insert, "codeMaskMulti");
		dijit.byId('variablesDialog').hide();
	}

	function addTextArea(velocityVarName) {
		var insert = "<textarea name=\"" + velocityVarName + "\" id=\"" + velocityVarName + "\">$!{" + velocityVarName + "}</textarea> \n";
		insertAtCursor(insert, "codeMaskMulti");
		dijit.byId('variablesDialog').hide();
	}

    function addCustomField(velocityVarName) {
	    var insert = "#if ($" + "{" + velocityVarName+"Code}) \n  $!{"+ velocityVarName + "Code} \n#end \n" ;
        insertAtCursor(insert, "codeMaskMulti");
        dijit.byId('variablesDialog').hide();

	}

	function addButton(buttonValue, velocityVarName) {
		var insert = "<input type=\"button\" value=\"" + buttonValue + "\" name=\"" + velocityVarName + "\" id=\"" + velocityVarName + "\" onClick=\"$!{" + velocityVarName + "ButtonCode}\">\n";
		insertAtCursor(insert, "codeMaskMulti");
		dijit.byId('variablesDialog').hide();
	}

	function paintCode(code) {
		insertAtCursor(code, "codeMaskMulti");
		dijit.byId('variablesDialog').hide();
	}

	function addDropdownList(fieldIdentifier) {
		StructureAjax.getDropDownList(paintCode, fieldIdentifier);
	}

	function addDropdownOptions(fieldIdentifier) {
		StructureAjax.getDropDownOptions(paintCode, fieldIdentifier);
	}

	function addRadioButtons(fieldIdentifier) {
		StructureAjax.getRadioButtons(paintCode, fieldIdentifier);
	}

	function addCheckboxes(fieldIdentifier) {
		StructureAjax.getCheckboxes(paintCode, fieldIdentifier);
	}

	function addIdentifierField(velocityVarName) {
		var insert = "$!{" + velocityVarName + "}\n";
		insertAtCursor(insert, "codeMaskMulti");
		dijit.byId('variablesDialog').hide();
	}

	var postLoopEditorCreated=false;
	var preLoopEditorCreated=false;
	var aceEditorCreated = false;
	var aceMultiEditorCreated = {};

	function showHideCode(){

			var val = document.getElementById("maxContentlets").value;
			var ele = document.getElementById("preLoopDiv");
			var ele2 = document.getElementById("postLoopDiv");
			var ele3 = document.getElementById("multiCodeButtonDiv");
			var ele4 = document.getElementById("codeButtonDiv");

			// select a default value for the filtering select
			var structureSelect = dijit.byId("structureSelect");
			structureSelect.set("value", structureSelect.store.data[0].value);

<!-- 			var selectedTab = dijit.byId('tabContainer').selectedChildWidget; -->
<!-- 			var structureId = selectedTab.id.split("_")[1]; -->

			if(!aceEditorCreated){
				aceEditor=aceArea("aceEditor", "codeMask");
				aceEditorCreated=true;
			}

			for(var i=0; i < structuresAdded.length; i++) {
				if(aceMultiEditor[structuresAdded[i]]==null) {
					aceMultiEditor[structuresAdded[i]]=aceArea("aceMaskMulti"+structuresAdded[i], "codeMaskMulti"+structuresAdded[i]);
  	       			aceMultiEditorCreated[structuresAdded[i]]=true;
   				}
			}

			if(isNaN(parseInt(val)) || parseInt(val)==0){

			    if(preLoopEditorCreated){
			    	preLoopEditor=aceRemover(preLoopEditor,"preLoopMask");
			    	preLoopEditorCreated=false;
			    }
			    if(postLoopEditorCreated){
			   		postLoopEditor=aceRemover(postLoopEditor,"postLoopMask");
			    	postLoopEditorCreated=false;
			    }
				ele.style.display="none";
				ele2.style.display="none";
				ele3.style.display="none";
				ele4.style.display="";
			}
			else{
				ele.style.display="";
				ele2.style.display="";
				ele3.style.display="";
				ele4.style.display="none";
				if(!preLoopEditorCreated){
					preLoopEditor=aceArea("preLoopAceEditor" ,"preLoopMask");
					preLoopEditorCreated=true;
				}
				if(!postLoopEditorCreated){
	            	postLoopEditor=aceArea("postLoopAceEditor" ,"postLoopMask");
	            	postLoopEditorCreated=true;
	            }
			}

			if(structuresAdded.length==0 && val>0) {
				// add a initial structure
				addCodeTab();
			}
	}

	function saveCodeWindowSizePreference (windowName, dimension) {
		var baseActionURL = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/containers/edit_preference" /><portlet:param name="cmd" value="<%= com.liferay.portal.util.Constants.SAVE %>" /><portlet:param name="userId" value="<%=user.getUserId()%>" /></portlet:actionURL>&in_frame=true'
		var iframe = document.getElementById("userpreferences_iframe");
		var window = document.getElementById(windowName);
		var newURL = baseActionURL + "&preference=window_"+windowName+"_"+dimension;
		if (dimension == 'height')
			newURL += "&value=" + window.style.height;
		if (dimension == 'width')
			newURL += "&value=" + window.style.width;
		iframe.src = newURL;
	}

	function selectContainerVersion(objId,referer) {
		if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.containers.confirm.replace.version")) %>')){
			window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/containers/edit_container" /></portlet:actionURL>&cmd=getversionback&inode=' + objId + '&inode_version=' + objId + '&referer=' + referer;
		}
	}

	function displayProperties(id) {
		if (id == "properties") {
			//display basic properties
			document.getElementById("properties").style.display = "";
			document.getElementById("permissions").style.display = "none";
			document.getElementById("versions").style.display = "none";
			//changing class for the tabs
			document.getElementById("properties_tab").className ="alpha";
			document.getElementById("permissions_tab").className ="beta";
			document.getElementById("versions_tab").className ="beta";
		} else if (id == "permissions") {
			//display permissions
			document.getElementById("properties").style.display = "none";
			document.getElementById("permissions").style.display = "";
			document.getElementById("versions").style.display = "none";
			//changing class for the tabs
			document.getElementById("properties_tab").className ="beta";
			document.getElementById("permissions_tab").className ="alpha";
			document.getElementById("versions_tab").className ="beta";
		} else if (id == "versions") {
			//display versions
			document.getElementById("properties").style.display = "none";
			document.getElementById("permissions").style.display = "none";
			document.getElementById("versions").style.display = "";
			//changing class for the tabs
			document.getElementById("properties_tab").className ="beta";
			document.getElementById("permissions_tab").className ="beta";
			document.getElementById("versions_tab").className ="alpha";
		}
	}

	var allfields = false;
	var currentfield = "";
	function startSpelling (field, fieldName) {
		allfields = false;
			if(field=='codeMask' && dijit.byId("toggleEditorCode").checked){
				document.getElementById("codeMask").value=aceEditor.getValue();
			}
			if(field=='preLoopMask'&& dijit.byId("toggleEditorPreLoop").checked){
				document.getElementById("preLoopMask").value=preLoopEditor.getValue();
			}
			if(field=='postLoopMask' && dijit.byId("toggleEditorPostLoop").checked){
				document.getElementById("postLoopMask").value=postLoopEditor.getValue();
			}
		checkSpelling (field, false, null, fieldName);
	}

	function startSpellingAllFields () {
		allfields = true;
		checkSpelling ("titleField", false, null, "Title");
		currentfield = "titleField";
	}

	//Spelling callback
	function spellingEnds (w, starting) {
		if (allfields) {
			var fieldTitle = "";
			var nextField = "";
			if (currentfield == "titleField") {
				nextField = "friendlyNameField";
				nextFieldTitle = "Description";
				fieldTitle = "Title"
			} else if (currentfield == "friendlyNameField") {
				nextField = "preLoopMask";
				nextFieldTitle = "Pre Loop";
				fieldTitle = "Description"
			} else if (currentfield == "preLoopMask") {
				nextField = "codeMask";
				nextFieldTitle = "Code";
				fieldTitle = "Pre Loop"
			} else if (currentfield == "codeMask") {
				nextField = "postLoopMask";
				nextFieldTitle = "Post Loop";
				fieldTitle = "Code"
			}
			if (currentfield == "postLoopMask") {
				alert("Spelling check finished.");
				w.focus ();
			} else {
				if (confirm(fieldTitle + ' <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.containers.confirm.spellcheck.confirm")) %>')) {
					if (nextField.value =="")
						spellingEnds (w, starting);
					else {
						checkSpelling (nextField, false, null, nextFieldTitle);
						currentfield = nextField;
						w.focus ();
					}
				} else {
					w.focus ();
				}
			}
		} else {
			alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.containers.alert.spellingcheck")) %>');
			w.focus ();
		}
	}

	//Struture javascripts

	function structureChanged() {
		fillContentFields();
	}

	function fillContentFields() {
		var structureInode = dijit.byId("structureSelect").attr('value');
		StructureAjax.getSearchableStructureFields (structureInode, fillContentFieldsRet);
	}

	function fillContentFieldsRet (fields) {

		var fieldsData = { identifier: 'value', label: 'name', items: [ { value:'sort_order', name:'None' } ] };

		for (var i = 0; i < fields.length; i++) {
			field = fields[i];
			fieldsData.items.push({ value: field.fieldVelocityVarName, name: field.fieldName });
		}

		dojo.require('dojo.data.ItemFileReadStore');
		var myStore = new dojo.data.ItemFileReadStore({data: fieldsData});

	}

	function setWidths(w) {
		setWidth('preLoopMask','<%=preLoopWidth%>');
		setWidth('codeMask','<%=codeWidth%>');
		setWidth('postLoopMask','<%=postLoopWidth%>');

	}

	function setHeights(h) {
		setHeight('preLoopMask','<%=preLoopHeight%>');
		setHeight('codeMask','<%=codeHeight%>');
		setHeight('postLoopMask','<%=postLoopHeight%>');

	}

    function deleteVersion(objId){
        if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.containers.confirm.delete.container.version")) %>')){
			window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/containers/edit_container" /></portlet:actionURL>&cmd=deleteversion&inode=' + objId + '&referer=' + referer;
        }
    }
	function selectVersion(objId) {
        if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.containers.confirm.replace.version")) %>')){
			window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/containers/edit_container" /></portlet:actionURL>&cmd=getversionback&inode=' + objId + '&inode_version=' + objId + '&referer=' + referer;
	    }
	}
	function editVersion(objId) {
		window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/containers/edit_container" /></portlet:actionURL>&cmd=edit&inode=' + objId + '&referer=' + referer;
	}

	function submitfmDelete() {
		if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.containers.confirm.delete.container")) %>'))
		{
			self.location = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/containers/edit_container" /><portlet:param name="cmd" value="full_delete" /><portlet:param name="inode" value="<%=String.valueOf(contentContainer.getInode())%>" /></portlet:actionURL>&referer=' + referer;
		}
	}


	function initContainerPage() {
		showHideCode();
		fillContentFields();
	}

	var preLoopEditor;
	var postLoopEditor;
	var aceEditor = false;
	var aceMultiEditor = {};
	var structuresAdded = new Array();
	var editor;

 	function aceArea(editorId, textarea){
		editor = ace.edit(editorId);
		editor.setTheme("ace/theme/textmate");
		editor.getSession().setMode("ace/mode/html");
		editor.getSession().setUseWrapMode(true);
		var id = document.getElementById(textarea);
		if(id == undefined){
			editor.setValue("");
		}else{
			editor.setValue(document.getElementById(textarea).value);
		}
		editor.clearSelection();
    	return editor;
	}



	function aceRemover(removeEditor, textarea){

	    var editorText=removeEditor.getValue();
		dojo.query('#'+textarea).style({display:''});
		dojo.query('#'+textarea)[0].value=editorText;
    	return null;
	}

    var htmlArea = "<textarea onkeydown='return catchTab(this,event)' property='${textAreaId}' id='${textAreaId}' style='width:${textAreaWidth}; height:${textAreaHeight}; font-size: 12px'></textarea>";
	var aceId;
	var aceClass;
	function aceToggler(editorId, textareaId){

			if(textareaId=="codeMask"){
					aceId = document.getElementById(editorId);
					aceClass = aceId.className;
	            	if(dijit.byId("toggleEditorCode").checked){
	            		dijit.byId("toggleEditorCode").disabled=true;
	            		document.getElementById('codeMask').style.display = "none";
	           			aceEditor.setValue(document.getElementById(textareaId).value);
						aceEditor.clearSelection();
	           			aceId.className = aceClass.replace('hidden', 'show');
	            		dijit.byId("toggleEditorCode").disabled=false;
	            	} else{
	            		dijit.byId("toggleEditorCode").disabled=true;
	            		document.getElementById('codeMask').style.display = "inline";
	           			aceId.className = aceClass.replace('show', 'hidden');
	           			aceRemover(aceEditor, textareaId);
	           			aceEditorCreated=false;
	            		dijit.byId("toggleEditorCode").disabled=false;
	            	}
	         } else if(textareaId=="codeMaskMulti"){
            	if(dijit.byId("toggleEditorCodeMultiple").checked){
            		dijit.byId("toggleEditorCodeMultiple").disabled=true;

					for(var i=0; i < structuresAdded.length; i++) {
						aceId = document.getElementById(editorId+structuresAdded[i]);
						aceClass = aceId.className;
						document.getElementById(textareaId+structuresAdded[i]).style.display = "none";
						aceMultiEditor[structuresAdded[i]].setValue(document.getElementById(textareaId+structuresAdded[i]).value);
    	     			aceMultiEditor[structuresAdded[i]].clearSelection();
    	       			aceId.className = aceClass.replace('hidden', 'show');
					}

            		dijit.byId("toggleEditorCodeMultiple").disabled=false;
            	} else{
            		dijit.byId("toggleEditorCodeMultiple").disabled=true;

            		for(var i=0; i < structuresAdded.length; i++) {
            			aceId = document.getElementById(editorId+structuresAdded[i]);
						aceClass = aceId.className;
            			aceRemover(aceMultiEditor[structuresAdded[i]], textareaId+structuresAdded[i]);
	            		aceId.className = aceClass.replace('show', 'hidden');
            			document.getElementById(textareaId+structuresAdded[i]).style.display = "inline";
            			aceMultiEditorCreated[structuresAdded[i]]=false;
            		}


            		dijit.byId("toggleEditorCodeMultiple").disabled=false;
            	}
            }
			else if(textareaId=="preLoopMask"){
				aceId = document.getElementById(editorId);
			 	aceClass = aceId.className;
				if(dijit.byId("toggleEditorPreLoop").checked){
            		dijit.byId("toggleEditorPreLoop").disabled=true;
            		document.getElementById('preLoopMask').style.display = "none";
	            	preLoopEditor.setValue(document.getElementById(textareaId).value);
					preLoopEditor.clearSelection();
	            	aceId.className = aceClass.replace('hidden', 'pShow');
            		dijit.byId("toggleEditorPreLoop").disabled=false;
            	}
            	else{
            		dijit.byId("toggleEditorPreLoop").disabled=true;
            		document.getElementById('codeMask').style.display = "inline";
	            	aceId.className = aceClass.replace('pShow', 'hidden');
	            	aceRemover(preLoopEditor, textareaId);
            		preLoopEditorCreated=false;
            		dijit.byId("toggleEditorPreLoop").disabled=false;
            	}
			}
			else if(textareaId=="postLoopMask"){
				aceId = document.getElementById(editorId);
				aceClass = aceId.className;
				if(dijit.byId("toggleEditorPostLoop").checked){
            		dijit.byId("toggleEditorPostLoop").disabled=true;
            		document.getElementById('postLoopMask').style.display = "none";
	            	postLoopEditor.setValue(document.getElementById(textareaId).value);
					postLoopEditor.clearSelection();
	            	aceId.className = aceClass.replace('hidden', 'pShow');
            		dijit.byId("toggleEditorPostLoop").disabled=false;
            	}
            	else{
            		dijit.byId("toggleEditorPostLoop").disabled=true;
            		aceId.className = aceClass.replace('pShow', 'hidden');
	            	aceRemover(postLoopEditor, textareaId);
            		preLoopEditorCreated=false;
            		dijit.byId("toggleEditorPostLoop").disabled=false;
            	}
			}
         return editor;
	}

	function hideEditButtonsRow() {

		dojo.style('editContainerButtonRow', { display: 'none' });
	}

	function showEditButtonsRow() {
		if( typeof changesMadeToPermissions!= "undefined"){
			if(changesMadeToPermissions == true){
				dijit.byId('applyPermissionsChangesDialog').show();
			}
		}
		dojo.style('editContainerButtonRow', { display: '' });
		changesMadeToPermissions = false;
	}

	function addCodeTab() {

		var structureInode = dijit.byId("structureSelect").attr('value');
		var label = dijit.byId("structureSelect").attr('displayedValue');

		if(structureInode=='') {
			alert('Must Select a Content Type');
			return;
		}

		var tc = dijit.byId("tabContainer");
		var cp1 = new dijit.layout.ContentPane({
		 id: "tab_"+structureInode,
         title: label,
         style:"padding:0",
         closable:true
	    });

	    require(["dojo/on"], function(on){
		  on(cp1, "close", function(e){
		    removeStructure(structureInode);
		  });
		});

	    cp1.startup();

	    var textarea = new dijit.form.Textarea({
		    name: "codeMaskMulti"+structureInode,
		    id: "codeMaskMulti"+structureInode,
		    value: "",
		    style: "width:99%; height:300px"
		  });
		  
		var div = dojo.create("div",{
		    id: "aceMaskMulti"+structureInode,
		    class: "show",
		    style: "position: relative;"
		  });
		cp1.set('content',div);
		cp1.addChild(textarea);
	    tc.addChild(cp1);
	    tc.selectChild(cp1);
	    addStructureToList(structureInode);

	   	// remove the structure added from the filtering select's store

<!-- 	    var structureSelect = dijit.byId("structureSelect"); -->
<!-- 	    var options = structureSelect.store.data; -->
<!-- 	    var indexToRemove = null; -->

<!-- 		dojo.some(options, function(widget, index, hash){ -->
<!-- 			if(widget.value==structureInode) { -->
<!-- 				indexToRemove = index; -->
<!-- 				return false; -->
<!-- 			} -->
<!-- 		}); -->

<!-- 		structureSelect.store.data.splice(indexToRemove,1); -->

		// end removing structure added


	    var length = structuresAdded.length;
	    aceMultiEditor[structuresAdded[length-1]] = aceArea('aceMaskMulti'+structureInode, 'codeMaskMulti'+structureInode);
		aceMultiEditorCreated[structuresAdded[length]]=true;

	}

	function addStructureToList(structureId) {
		structuresAdded.push(structureId);
	}

	function removeStructure(structureId) {
		aceRemover(aceMultiEditor[structureId], "codeMaskMulti"+structureId);

		dijit.byId('codeMaskMulti'+structureId).destroy();

		aceMultiEditor[structureId] = null;
		var index = structuresAdded.indexOf(structureId);
		structuresAdded.splice(index,1);
	}

