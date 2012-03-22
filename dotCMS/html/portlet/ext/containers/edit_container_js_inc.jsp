	<%@ page import="com.dotmarketing.util.UtilMethods" %>

	var referer = '<%=referer%>';

	function submitfm(form,subcmd) {
	
			var numContentlets = parseInt(dijit.byId("maxContentlets").value);
			if(dijit.byId("toggleEditorCode").checked){
				document.getElementById("codeMask").value=codeEditor.getCode();
			}
			if(dijit.byId("toggleEditorPreLoop").checked && numContentlets > 0){
				document.getElementById("preLoopMask").value=preLoopEditor.getCode();
			}
			else if(numContentlets == 0){
				document.getElementById("preLoopMask").value = "";
			}
			if(dijit.byId("toggleEditorPostLoop").checked && numContentlets > 0){
				document.getElementById("postLoopMask").value=postLoopEditor.getCode();
			}else if(numContentlets == 0){
				document.getElementById("postLoopMask").value="";
			}
							
			//DOTCMS-5415
			document.getElementById("preLoop").value = document.getElementById("preLoopMask").value;
			document.getElementById("code").value = document.getElementById("codeMask").value;
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
		var structureInode = dijit.byId("structureSelect").attr('value');
		dijit.registry.remove('variablesDialog');
	 	new dijit.Dialog({
	 		id: 'variablesDialog',
      		title: "<%= LanguageUtil.get(pageContext, "Add-Variables") %>",
      		href: "/html/portlet/ext/containers/add_variables.jsp?structureInode=" + structureInode,
      		style: "width: 450px; height: 500px;"
  		}, 'variablesDialog').show();
	}
	
	function insertAtCursor( myValue, myFieldName) {
		myField = document.getElementById(myFieldName);
        if(myFieldName=="codeMask") {
        	if(codeEditor) {
        		var pos= codeEditor.cursorPosition(true);
				codeEditor.insertIntoLine(pos.line, pos.character, myValue);
			} else {
				myField.value=myField.value+myValue;
			}
		} else if(myFieldName=="preLoopMask") {
			if(preLoopEditor) {
				var pos= preLoopEditor.cursorPosition(true);
			    preLoopEditor.insertIntoLine(pos.line, pos.character, myValue); 
            } else {
				myField.value=myField.value+myValue;
            }
		} else if(myFieldName=="postLoopMask") {
			if(postLoopEditor) {
				var pos= postLoopEditor.cursorPosition(true);
			    postLoopEditor.insertIntoLine(pos.line, pos.character, myValue); 
            } else {
            	myField.value=myField.value+myValue;
            }
		} else {
				myField.value=myField.value+myValue
		}
	}
	
	function add(x){
		insertAtCursor("$!{" + x + "}\n", 'codeMask');
		dijit.byId('variablesDialog').hide();
	}

	function addImage(velocityVarName){
		var insert = "#if ($UtilMethods.isSet($imageImageURI)) \n   <img src=\"$!{"+velocityVarName+"ImageURI}\" alt=\"$!{"+velocityVarName+"ImageTitle}\"  /> \n#end \n";	
		insertAtCursor(insert, "codeMask");
		dijit.byId('variablesDialog').hide();
	}

	function addLink(velocityVarName) {
		var insert = "#if ($" + "{" + velocityVarName+"LinkURL}) \n   <a href=\"$!{"+velocityVarName+"LinkProtocol}$!{"+velocityVarName+"LinkURL}\" target=\"$!{"+velocityVarName+"LinkTarget}\">$!{"+velocityVarName+"LinkTitle}</a> \n#end \n";
		insertAtCursor(insert, "codeMask");
		dijit.byId('variablesDialog').hide();
	}
	
	function addFile(velocityVarName) {
		var insert = "#if ($" + "{" + velocityVarName+"FileURI}) \n   <a href=\"$!{"+velocityVarName+"FileURI}\">$!{"+velocityVarName+"FileTitle}</a> \n#end \n";
		insertAtCursor(insert, "codeMask");
		dijit.byId('variablesDialog').hide();
	}
	function addBinaryFile(velocityVarName) {
		var insert = "#if ($UtilMethods.isSet($" + "{" + velocityVarName+"BinaryFileURI})) \n   <a href=\"$!{"+velocityVarName+"BinaryFileURI}?force_download=1&filename=$!{"+velocityVarName+"BinaryFileTitle}\">$!{"+velocityVarName+"BinaryFileTitle}</a> \n#end \n";
		insertAtCursor(insert, "codeMask");
		dijit.byId('variablesDialog').hide();
	}
	
	
	function addBinaryResize(velocityVarName) {
		var insert = "#if ($UtilMethods.isSet($" + "{" + velocityVarName+"BinaryFileURI})) \n   <img src=\"/contentAsset/resize-image/${ContentIdentifier}/" + velocityVarName + "?w=150&h=100\" />\n#end \n";
		insertAtCursor(insert, "codeMask");
		dijit.byId('variablesDialog').hide();
	}

	function addBinaryThumbnail(velocityVarName) {
		var insert = "#if ($UtilMethods.isSet($" + "{" + velocityVarName+"BinaryFileURI})) \n   <img src=\"/contentAsset/image-thumbnail/${ContentIdentifier}/" + velocityVarName + "?w=150&h=150\" />\n#end \n";
		insertAtCursor(insert, "codeMask");
		dijit.byId('variablesDialog').hide();
	}

	
	
	function addTextField(velocityVarName) {
		var insert = "<input type=\"text\" name=\"" + velocityVarName + "\" id=\"" + velocityVarName + "\" value=\"$!{" + velocityVarName + "}\"> \n";
		insertAtCursor(insert, "codeMask");
		dijit.byId('variablesDialog').hide();
	}
	
	function addInodeField(velocityVarName) {
		var insert = "$!{" + velocityVarName + "}\n";
		insertAtCursor(insert, "codeMask");
		dijit.byId('variablesDialog').hide();
	}

	function addTextArea(velocityVarName) {
		var insert = "<textarea name=\"" + velocityVarName + "\" id=\"" + velocityVarName + "\">$!{" + velocityVarName + "}</textarea> \n";
		insertAtCursor(insert, "codeMask");
		dijit.byId('variablesDialog').hide();
	}

    function addCustomField(velocityVarName) {
	    var insert = "#if ($" + "{" + velocityVarName+"Code}) \n  $!{"+ velocityVarName + "Code} \n#end \n" ;        
        insertAtCursor(insert, "codeMask");
        dijit.byId('variablesDialog').hide();
		
	}

	function addButton(buttonValue, velocityVarName) {
		var insert = "<input type=\"button\" value=\"" + buttonValue + "\" name=\"" + velocityVarName + "\" id=\"" + velocityVarName + "\" onClick=\"$!{" + velocityVarName + "ButtonCode}\">\n";
		insertAtCursor(insert, "codeMask");
		dijit.byId('variablesDialog').hide();
	}

	function paintCode(code) {
		insertAtCursor(code, "codeMask");
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
		insertAtCursor(insert, "codeMask");
		dijit.byId('variablesDialog').hide();
	}
		
	var postLoopEditorCreated=false;
	var preLoopEditorCreated=false;
	var codeEditorCreated=false;
	
	function showHideCode(){
	
			var val = document.getElementById("maxContentlets").value;
			var ele = document.getElementById("preLoopDiv");
			var ele2 = document.getElementById("postLoopDiv");
			var ele3 = document.getElementById("structureControls");
			
			if(!codeEditorCreated){
			codeEditor=codeMirrorArea("codeMask", "<%=codeWidth%>", "<%=codeHeight%>");
			codeEditorCreated=true;
			}
			if(isNaN(parseInt(val)) || parseInt(val)==0){
			    if(preLoopEditorCreated){
			    	preLoopEditor=codeMirrorRemover(preLoopEditor,"preLoopMask");
			    	preLoopEditorCreated=false;
			    }
			    if(postLoopEditorCreated){
			   		postLoopEditor=codeMirrorRemover(postLoopEditor,"postLoopMask");
			    	postLoopEditorCreated=false;
			    }
				ele.style.display="none";
				ele2.style.display="none";
				ele3.style.display="none";
			}
			else{
				ele.style.display="";
				ele2.style.display="";
				ele3.style.display="";
				if(!preLoopEditorCreated){
					preLoopEditor=codeMirrorArea("preLoopMask", "<%=preLoopWidth%>", "<%=preLoopHeight%>");
					preLoopEditorCreated=true;
				}
				if(!postLoopEditorCreated){
	            	postLoopEditor=codeMirrorArea("postLoopMask", "<%=postLoopWidth%>", "<%=postLoopHeight%>");
	            	postLoopEditorCreated=true;
	            }
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
				document.getElementById("codeMask").value=codeEditor.getCode();
			}
			if(field=='preLoopMask'&& dijit.byId("toggleEditorPreLoop").checked){
				document.getElementById("preLoopMask").value=preLoopEditor.getCode();
			}
			if(field=='postLoopMask' && dijit.byId("toggleEditorPostLoop").checked){
				document.getElementById("postLoopMask").value=postLoopEditor.getCode();
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
	var codeEditor;

 	function codeMirrorArea(textarea, width, height){
		var editor = CodeMirror.fromTextArea(textarea, {
			width: width,
			height:height,    
			parserfile: ["parsedummy.js","parsexml.js", "parsecss.js", "tokenizejavascript.js", "parsejavascript.js", "parsehtmlmixed.js"],
			stylesheet: ["/html/js/codemirror/css/xmlcolors.css", "/html/js/codemirror/css/jscolors.css", "/html/js/codemirror/css/csscolors.css"],
			path: "/html/js/codemirror/js/",
			iframeClass: textarea+"_codeMirror"
		});
    	return editor;
	} 
	    	

	
	function codeMirrorRemover(editor,textarea){
	    var editorText=editor.getCode();
	    removeElement(dojo.query('.'+textarea+'_codeMirror')[0].parentNode);
		dojo.query('#'+textarea).style({display:''});
		dojo.query('#'+textarea)[0].value=editorText;
    	return null;  	
	}

    var htmlArea = "<textarea onkeydown='return catchTab(this,event)' property='${textAreaId}' id='${textAreaId}' style='width:${textAreaWidth}; height:${textAreaHeight}; font-size: 12px'></textarea>";
	function codeMirrorToggler(editor, textareaId, width, height){

         if(textareaId=="codeMask"){
            	if(dijit.byId("toggleEditorCode").checked){
            		dijit.byId("toggleEditorCode").disabled=true;
            		editor=codeMirrorArea(textareaId,width, height);
            		codeEditorCreated=true;
            		dijit.byId("toggleEditorCode").disabled=false;
            	} else{
            		dijit.byId("toggleEditorCode").disabled=true;
            		editor=codeMirrorRemover(editor, textareaId);
            		codeEditorCreated=false;
            		dijit.byId("toggleEditorCode").disabled=false;
            	}
            }	
			else if(textareaId=="preLoopMask"){
				if(dijit.byId("toggleEditorPreLoop").checked){
            		dijit.byId("toggleEditorPreLoop").disabled=true;
            		editor=codeMirrorArea(textareaId,width, height);
            		preLoopEditorCreated=true;
            		dijit.byId("toggleEditorPreLoop").disabled=false;
            	}
            	else{
            		dijit.byId("toggleEditorPreLoop").disabled=true;
            		editor=codeMirrorRemover(editor, textareaId);
            		preLoopEditorCreated=false;
            		dijit.byId("toggleEditorPreLoop").disabled=false;
            	}
			}
			else if(textareaId=="postLoopMask"){
				if(dijit.byId("toggleEditorPostLoop").checked){
            		dijit.byId("toggleEditorPostLoop").disabled=true;
            		editor=codeMirrorArea(textareaId,width, height);
            		preLoopEditorCreated=true;
            		dijit.byId("toggleEditorPostLoop").disabled=false;
            	}
            	else{
            		dijit.byId("toggleEditorPostLoop").disabled=true;
            		editor=codeMirrorRemover(editor, textareaId);
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