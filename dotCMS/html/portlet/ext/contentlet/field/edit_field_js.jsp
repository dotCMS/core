<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="javax.portlet.WindowState"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.portlets.files.model.File"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="com.liferay.portal.model.User"%>

<script type='text/javascript' src='/dwr/interface/StructureAjax.js'></script>
<script type='text/javascript' src='/dwr/interface/CategoryAjax.js'></script>
<script type='text/javascript' src='/dwr/interface/ContentletAjax.js'></script>
<script type='text/javascript' src='/dwr/interface/TagAjax.js'></script>
<script type='text/javascript' src='/dwr/interface/FileAjax.js'></script>
<script type='text/javascript' src='/dwr/interface/TagAjax.js'></script>
<style type="text/css">
    #aceTextArea { 
        position: relative;	  	
    }
    .aceClass{
    	width: 100%;
        height: 400px;
        border:1px solid #C0C0C0;
        text-overflow: clip;
    	white-space: nowrap;   
    }
    .aceText{
    	width:450px;
    	min-height:105px;
    	max-height: 600px;    	
        border:1px solid #C0C0C0;
        text-overflow: clip;
    	white-space: nowrap;   
    }
</style>

<!-- AChecker support -->
<script type='text/javascript' src='/dwr/interface/ACheckerDWR.js'></script>

<script src="/html/js/ace-builds-1.1.01/src-noconflict/ace.js" type="text/javascript"></script>
<%if(Config.getBooleanProperty("ENABLE_GZIP",true)){ %>
<script type="text/javascript" src="/html/js/tinymce/jscripts/tiny_mce/tiny_mce_gzip.js"></script>
<%}else { %>
<script type="text/javascript" src="/html/js/tinymce/jscripts/tiny_mce/tiny_mce.js"></script>
<%}%>
<script type="text/javascript">

	dojo.require('dijit.form.Slider');
	dojo.require("dotcms.dijit.form.HostFolderFilteringSelect");
	dojo.require('dotcms.dijit.form.FileSelector');
	dojo.require("dotcms.dijit.form.FileAjaxUploader");
	dojo.require("dotcms.dijit.FileBrowserDialog");
	dojo.require("dojo.dnd.Source");

<% User usera= com.liferay.portal.util.PortalUtil.getUser(request); %>
	if(<%=Config.getBooleanProperty("ENABLE_GZIP",true) %>){
		tinyMCE_GZ.init({
			plugins : 'style,layer,table,save,advhr,advimage,advlink,emotions,iespell,insertdatetime,preview,media,searchreplace,print,contextmenu',
			themes : 'simple,advanced',
			languages : '<%= usera.getLanguageId().substring(0,2) %>',
			disk_cache : true,
			debug : false

		});
	}else{
		tinyMCE.init({
			plugins : 'style,layer,table,save,advhr,advimage,advlink,emotions,iespell,insertdatetime,preview,media,searchreplace,print,contextmenu',
			themes : 'simple,advanced',
			languages : '<%= usera.getLanguageId().substring(0,2) %>',
			disk_cache : true,
			debug : false

		});
	}
</script>

<script type="text/javascript">
var cmsfile=null;
	//Hints
	function showHint(jsevent) {
		var coordinates = jsevent.getXY();
		this.moveTo(coordinates[0] + 10, coordinates[1]);
		this.show();
	}

	function hideHint(jsevent) {
		this.hide();
	}

	//Date/Time fields
	function updateDate(varName) {
		var field = $(varName);
		var dateValue ="";
		var datePart=dijit.byId(varName + "Date");
		var timePart=dijit.byId(varName + 'Time');
		
		if(datePart != null) {
			var x = datePart.getValue();
			if(x) {
				var month = (x.getMonth() +1) + "";
				month = (month.length < 2) ? "0" + month : month;
				var day = (x.getDate() ) + "";
				day = (day.length < 2) ? "0" + day : day;
				year = x.getFullYear();
				dateValue= year + "-" + month + "-" + day + " ";
			}
		}
		
		if(datePart==null || dateValue!="") {
			// if it is just time or date_time but the value exists
			if (timePart != null) {
				var time = timePart.value;
				if(!isNaN(time) && time != null) {
					var hour = time.getHours();
					if(hour < 10) hour = "0" + hour;
					var min = time.getMinutes();
					if(min < 10) min = "0" + min;
					dateValue += hour + ":" + min;
					if(datePart==null)
						dateValue+=":00";
				}
				else{
					dateValue += "00:00";
				}
			} else {
				dateValue += "00:00";
			}
		}

		field.value = dateValue;
	}

	  function removeThumbnail(x, inode) {

		    var pDiv = dojo.byId('thumbnailParent'+x);
	    	if(pDiv != null && pDiv != undefined){
	    		dojo.destroy(pDiv);
	    	}
	    	
	    	var swDiv = dojo.byId(x+'ThumbnailSliderWrapper');
	       	if(swDiv != null && swDiv != undefined){
	       	dojo.destroy(swDiv);
	       	}
	    	
	    	dojo.query(".thumbnailDiv" + x).forEach(function(node, index, arr){
	    		dojo.destroy(node);
	    	});
	    	
	    	var dt = dojo.byId(x+'dt');
	    	if(dt != null && dt != undefined){
	    		dt.innerHTML = '';
	    	}

	    	//http://jira.dotmarketing.net/browse/DOTCMS-5802
	 	    ContentletAjax.removeSiblingBinaryFromSession(x, null);

	    	if(inode){
	    	   FileAssetAjax.removeTempFile(inode, null);
	    	}

	    }


	function isInteger(s)
	   {
	      var i;

	      if (isEmpty(s))
	      if (isInteger.arguments.length == 1) return 0;
	      else return (isInteger.arguments[1] == true);

	      for (i = 0; i < s.length; i++)
	      {
	         var c = s.charAt(i);

	         if (!isDigit(c)) return false;
	      }

	      return true;
	   }

	   function isEmpty(s)
	   {
	      return ((s == null) || (s.length == 0))
	   }

	   function isDigit (c)
	   {
	      return ((c >= "0") && (c <= "9"))
	   }


	function updateAmPm(varName) {
		var hour = dijit.byId(varName + 'Hour').value;
		if(hour > 11) {
			$(varName + 'AMPM').update("PM");
		} else {
			$(varName + 'AMPM').update("AM");
		}
	}

	function daysInMonth(month,year)
	{
		return 32 - new Date(year,month,32).getDate();
	}

</script>

<script type="text/javascript">
<jsp:include page="/html/portlet/ext/contentlet/field/tiny_mce_config.jsp"/>
	//### END INIT TINYMCE ###
	//### TINYMCE ###



	var enabledWYSIWYG = new Array();
	var enabledCodeAreas = new Array();
	var aceEditors = new Array();


	function enableDisableWysiwygCodeOrPlain(id) {
		var toggleValue=dijit.byId(id+'_toggler').attr('value');
		if (toggleValue=="WYSIWYG"){
			toWYSIWYG(id);
			updateDisabledWysiwyg(id,"WYSIWYG");
			}
		else if(toggleValue=="CODE"){
			toCodeArea(id);
			updateDisabledWysiwyg(id,"CODE");
			}
		else if(toggleValue=="PLAIN"){
			toPlainView(id);
			updateDisabledWysiwyg(id,"PLAIN");
			}
	}
	
	function updateDisabledWysiwyg(id,mode){
		
		//Updating the list of disabled wysiwyg list
		var elementWysiwyg = document.getElementById("disabledWysiwyg");
		var wysiwygValue = elementWysiwyg.value;
		var result = "";
		var existingInDisabledWysiwyg = false;
		if(mode == "WYSIWYG"){
			
			if(wysiwygValue != ""){
				var wysiwygValueArray = wysiwygValue.split(",");

				for(i = 0;i < wysiwygValueArray.length;i++)
				{
					var wysiwygFieldVar = trimString(wysiwygValueArray[i]);
					if((wysiwygFieldVar == id) || (wysiwygFieldVar == id+"<%=com.dotmarketing.util.Constants.WYSIWYG_PLAIN_SEPARATOR%>")){
						wysiwygFieldVar = "";
					}
					result += wysiwygFieldVar + ",";
				}
			}
		}else if(mode == "CODE"){
			
			if(wysiwygValue != ""){
				var wysiwygValueArray = wysiwygValue.split(",");

				for(i = 0;i < wysiwygValueArray.length;i++)
				{
					var wysiwygFieldVar = trimString(wysiwygValueArray[i]);
					if(wysiwygFieldVar == id+"<%=com.dotmarketing.util.Constants.WYSIWYG_PLAIN_SEPARATOR%>"){
						wysiwygFieldVar = id;
						existingInDisabledWysiwyg = true;
					}
					result += wysiwygFieldVar + ",";
				}
				if(!existingInDisabledWysiwyg)
					result += id;
			}else{
				result += id;
			}
		}else{// to PLAIN
			
			if(wysiwygValue != ""){
				var wysiwygValueArray = wysiwygValue.split(",");

				for(i = 0;i < wysiwygValueArray.length;i++){
					var wysiwygFieldVar = trimString(wysiwygValueArray[i]);
					if(wysiwygFieldVar == id){
						wysiwygFieldVar = id+"<%=com.dotmarketing.util.Constants.WYSIWYG_PLAIN_SEPARATOR%>";
						existingInDisabledWysiwyg = true;
					}
					result += wysiwygFieldVar + ",";
				}
				if(!existingInDisabledWysiwyg)
					result += id+"<%=com.dotmarketing.util.Constants.WYSIWYG_PLAIN_SEPARATOR%>";
			}else{
				result += id+"<%=com.dotmarketing.util.Constants.WYSIWYG_PLAIN_SEPARATOR%>"; 
			}
		}
		elementWysiwyg.value = result;
	}

	function toPlainView(id) {
		if(enabledWYSIWYG[id]){
			disableWYSIWYG(id);
			dojo.query('#'+id).style({display:''});
		}
		else if(enabledCodeAreas[id]){
			aceRemover(id);
		}
		document.getElementById(id).value = document.getElementById(id).value.trim();
	}

	function toCodeArea(id) {
		if(enabledWYSIWYG[id]){
			disableWYSIWYG(id);
		}
		if(!enabledCodeAreas[id])
			aceArea(id);
	}

	function toWYSIWYG(id) {
		if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.contentlet.switch.wysiwyg")) %>'))
        {
			if(enabledCodeAreas[id]){
				aceRemover(id);
			}
			enableWYSIWYG(id, false);
		}
	}

	function enableWYSIWYG(textAreaId, confirmChange)
	{
        if(!isWYSIWYGEnabled(textAreaId))
        {
        	//Confirming the change
			if(confirmChange == true &&
				!confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.contentlet.switch.wysiwyg.view")) %>'))
			{
				return;
			}

			//Enabling the wysiwyg
			try
			{
				(new tinymce.Editor(textAreaId, tinyMCEProps)).render();

			}
			catch(e)
			{
				showDotCMSErrorMessage("Enable to initialize WYSIWYG " + e.message);
			}
			enabledWYSIWYG[textAreaId] = true;

		}
	}

	function disableWYSIWYG(textAreaId)
	{

		if(isWYSIWYGEnabled(textAreaId))
        {
			//Disabling the control
			tinymce.EditorManager.get(textAreaId).remove();
			enabledWYSIWYG[textAreaId] = false;
		}
	}

	function isWYSIWYGEnabled(id)
	{
		return (enabledWYSIWYG[id]);
	}

	//WYSIWYG special functions
	function cmsURLConverter (url, node, on_save) {
		var idx = url.indexOf('#');
		var servername = "http://<%= request.getServerName() %>";
		var start = url.substring(0, servername.length);
		var returl = "";
		if (idx >= 0 && start == servername) {
			returl = url.substring(idx, url.length);
		} else {
			returl = url;
		}
		return returl;
	}

	var wysiwyg_field_name;
	var wysiwyg_url;
	var wysiwyg_type;
	var wysiwyg_win;

	function cmsFileBrowser(field_name, url, type, win) {
		wysiwyg_win = win;
		wysiwyg_field_name = field_name;
		if(type=="image"){
			cmsFileBrowserImage.show();
		}
		else{
			cmsFileBrowserFile.show();
		}
		dojo.style(dojo.query('.clearlooks2')[0], { zIndex: '100' })
		dojo.style(dojo.byId('mceModalBlocker'), { zIndex: '90' })
	}

	//Glossary terms search

	var glossaryTermId;
	var lastGlossarySearch;

	function lookupGlossaryTerm(textAreaId, language) {

        glossaryTermId = textAreaId;


		var gt = dojo.byId("glossary_term_" + textAreaId);
		var toSearch = dojo.byId("glossary_term_" + textAreaId).value;
	    var menu = dojo.byId("glossary_term_popup_" + textAreaId);
	    //var gtCoords = dojo.coords(gt, true);
    	dojo.style(menu, { top: "35px", right: "25px" });

	    if (toSearch != "" && lastGlossarySearch != toSearch) {
	    	lastGlossarySearch = toSearch;
	    	dojo.byId("glossary_term_table_" + textAreaId).innerHTML = '';
	        ContentletAjax.doSearchGlossaryTerm(lastGlossarySearch, language, lookupGlossaryTermReply);
	    }
	}

	function lookupGlossaryTermReply(data) {

	    if (!data) {
	    	return;
	    }

		var glossaryTermsTable = dojo.byId("glossary_term_table_" + glossaryTermId);

		var strHTML = "";

		if (data.length > 0) {
			strHTML = '<table class="listingTable" style="width: 300px;">' +
				'<tr><th><%= LanguageUtil.get(pageContext, "Term") %></th>' +
				'<th><%= LanguageUtil.get(pageContext, "Definition") %></th>' +
				'<th><a href="javascript: clearGlossaryTerms();">X</a></th></tr>';
			var className = 'alternate_1';
		     for(var loop = 0; loop < data.length; loop++) {
		    	if(className == 'alternate_1')
		    		className = 'alternate_2';
		    	else
		    		className = 'alternate_1';
	            option = data[loop][0];
	            value = data[loop][1];
	            strHTML += '<tr class="' + className + '">' +
	        	        '<td><a href="javascript: addGlossaryTerm(\'' + option + '\');">' + option + '</a></td>' +
	        	        '<td colspan="2">' + value + '</td></tr>';
	          }
	        strHTML += "</tbody></table>";
	    } else {
		    strHTML = "<table border='0' cellpadding='4' cellspacing='0' class='beta glosaryTermsTable'><tbody><tr class=\"beta\"><td class=\"beta\"><%= LanguageUtil.get(pageContext, "There-are-no-dictionary-terms-that-matched-your-search") %></td></tr></tbody></table>";
	    }

	    dojo.byId("glossary_term_table_" + glossaryTermId).innerHTML = strHTML;

	    dojo.style("glossary_term_popup_" + glossaryTermId, { display: "" });

	}

	function clearGlossaryTermsDelayed() {
		setTimeout('clearGlossaryTerms()', 500);
	}

	function clearGlossaryTerms() {
		if(document.getElementById("glossary_term_popup_" + glossaryTermId)){
			document.getElementById("glossary_term_popup_" + glossaryTermId).style.display = "none";
		}
		if(document.getElementById("glossary_term_" + glossaryTermId)){
			document.getElementById("glossary_term_" + glossaryTermId).value = "";
		}
		dwr.util.removeAllRows("glossary_term_table_" + glossaryTermId);
	}

	function addGlossaryTerm(option) {
		if(enabledWYSIWYG[glossaryTermId]) {
			tinymce.EditorManager.get(glossaryTermId).save();
		} else if (enabledCodeAreas[glossaryTermId]) {
		}
		var currentValue = dojo.byId(glossaryTermId).value;
		dojo.byId(glossaryTermId).value = currentValue + "$text.get('" + option + "')";
		var updatedValue = currentValue + "$text.get('" + option + "')";
		if(enabledWYSIWYG[glossaryTermId]) {
			tinymce.EditorManager.get(glossaryTermId).load();
		} else if (enabledCodeAreas[glossaryTermId]) {
			if(glossaryTermId==aceTextId){
				textEditor[glossaryTermId].setValue(dojo.byId(glossaryTermId).value);
				textEditor[glossaryTermId].clearSelection();				
			} else {
				editor.setValue(dojo.byId(glossaryTermId).value);
				editor.clearSelection();
			}
		}
		clearGlossaryTerms();
	}

	//Links kind of fields
	function popupEditLink(inode, varName) {
	    editlinkwin = window.open('<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/edit_link" /><portlet:param name="cmd" value="edit" /><portlet:param name="popup" value="1" /></portlet:actionURL>&inode=' + inode + '&child=true&page_width=650', "editlinkwin", 'width=700,height=400,scrollbars=yes,resizable=yes');
	}

	//Other functions
	function popUpMacroHelp(){
		openwin = window.open('http://www.dotcms.org/documentation/dotCMSMacros',"newin","menubar=1,width=1100,height=750,scrollbars=1,toolbar=1,status=0,resizable=1");
	}

	function updateHostFolderValues(field){
	  if(!isInodeSet(dijit.byId('HostSelector').attr('value'))){
		 dojo.byId(field).value = "";
		 dojo.byId('hostId').value = "";
		 dojo.byId('folderInode').value = "";
	  }else{
		 var data = dijit.byId('HostSelector').attr('selectedItem');
		 if(data["type"]== "host"){
			dojo.byId(field).value =  dijit.byId('HostSelector').attr('value');
			dojo.byId('hostId').value =  dijit.byId('HostSelector').attr('value');
			dojo.byId('folderInode').value = "";
		 }else if(data["type"]== "folder"){
			dojo.byId(field).value =  dijit.byId('HostSelector').attr('value');
			dojo.byId('folderInode').value =  dijit.byId('HostSelector').attr('value');
			dojo.byId('hostId').value = "";
		}
	  }
	}

	function aceArea(textarea){
		document.getElementById(textarea).style.display = "none";
		var id = document.getElementById(textarea).value.trim();
		aceEditors[textarea] = document.getElementById(textarea+'aceEditor');
		var aceClass = aceEditors[textarea].className;
		aceEditors[textarea].className = aceClass.replace('classAce', 'aceClass');
		aceEditors[textarea] = ace.edit(textarea+'aceEditor');
	    aceEditors[textarea].setTheme("ace/theme/textmate");
	    aceEditors[textarea].getSession().setMode("ace/mode/html");
	    aceEditors[textarea].getSession().setUseWrapMode(true);
	    aceEditors[textarea].setValue(id);
    	aceEditors[textarea].clearSelection();
		enabledCodeAreas[textarea]=true;
		aceEditors[textarea].on("change", function(){
			document.getElementById(textarea).value = aceEditors[textarea].getValue();
		})
	}

	function aceRemover(textarea){
		var editorText = aceEditors[textarea].getValue();
	    var aceEditor = document.getElementById(textarea+'aceEditor');
	    var aceClass = aceEditor.className;
		aceEditor.className = aceClass.replace('aceClass', 'classAce');
		dojo.query('#'+textarea).style({display:''});
		dojo.query('#'+textarea)[0].value=editorText;
		enabledCodeAreas[textarea]=false;
		aceEditors[textarea] = null;
	}
	
	function addFileImageCallback(file) {
		var ident
		var ext=file.extension;
		var ident =file.identifier+'.'+ext;
		wysiwyg_win.document.forms[0].elements[wysiwyg_field_name].value = "/dotAsset/" + ident;
		if(wysiwyg_field_name == 'src'){
			wysiwyg_win.ImageDialog.showPreviewImage("/dotAsset/" + ident);
		}
	}
	function addFileCallback(file) {
		var ident
		var ext=file.extension;
		var ident =file.identifier+'.'+ext;
		var fileExt = getFileExtension(file.name).toString();
		<% String extension = com.dotmarketing.util.Config.getStringProperty("VELOCITY_PAGE_EXTENSION"); %>
		if(fileExt == '<%= extension %>'){
			wysiwyg_win.document.forms[0].elements["href"].value = file.pageURI;
		}else{
			wysiwyg_win.document.forms[0].elements["href"].value = /dotAsset/ + ident;
		}
	}

	function addKVPair(fieldId, fieldValueId){
		var node = dojo.byId(fieldId+'_kvtable');
		var key = dijit.byId(fieldValueId+'_key').value;
		if(key===''){
			 alert('<%= LanguageUtil.get(pageContext, "empty-key") %>');
		}else{
		var value = dijit.byId(fieldValueId+'_value').value;
		var table = document.getElementById(fieldId+'_kvtable');
		var row = document.getElementById(fieldId+'_'+key);
		if(row!=null){
			 alert('<%= LanguageUtil.get(pageContext, "key-already-exists") %>');
		}else{
			var newRow = table.insertRow(table.rows.length);
			newRow.id = fieldId+'_'+key;
			if((table.rows.length%2)==0){
		        newRow.className = "dojoDndItem alternate_2";
			}else{
				newRow.className = "dojoDndItem alternate_1";
			}
			var cell0 = newRow.insertCell(0);
			var cell1 = newRow.insertCell(1);
			var cell2 = newRow.insertCell(2);
			var anchor = document.createElement("a");
			cell0.width = "6%";
			anchor.href= 'javascript:javascript:deleteKVPair('+'"'+ fieldId +'"'+','+'"'+ fieldValueId +'"'+', '+'"'+ key +'"'+');';
			anchor.innerHTML = '<span class="deleteIcon"></span>';
			cell0.appendChild(anchor);
			cell1.innerHTML = key;
			cell1.width="34%";
			cell2.innerHTML = value;
			cell2.width="60%";
			var input = document.createElement("input");
			input.type="hidden";
			input.id=newRow.id+"_k";
			if(key!=null && key!='') {
				key = key.replace("\"","\\\"");
			}
			input.value=key;
			table.appendChild(input);
		    input = document.createElement("input");
			input.type="hidden";
			input.id=newRow.id+"_v";
			if(value!=null && value!='') {
				value = value.replace("\"","\\\"");
			}
			input.value=value;
			table.appendChild(input);
			var dndTable = new dojo.dnd.Source(node);
			dojo.connect(dndTable, "insertNodes", function(){
				 setKVValue(fieldId, fieldValueId);
		    	 recolorTable(fieldId);
			 });
			var row1 = document.getElementById(fieldId+'_'+key);
			document.getElementById(row1.id+'_v').value = value;
			setKVValue(fieldId, fieldValueId);
			dijit.byId(fieldValueId+'_key').reset();
			dijit.byId(fieldValueId+'_value').reset();

		}
	  }
	}


	function deleteKVPair(fieldId, fieldValueId, key){

		var table = document.getElementById(fieldId+'_kvtable');
		var row = document.getElementById(fieldId+'_'+key);
			if(row){
				try {
					 var rowCount = table.rows.length;
					 for(var i=0; i<rowCount; i++) {
						if(row.id==table.rows[i].id) {
							
							table.deleteRow(i);
						
							rowCount--;
							i--;
						}
					 }
					 recolorTable(fieldId);
				 }catch(e) {}
			}
			setKVValue(fieldId, fieldValueId);
	}


	function recolorTable(fieldId){
		var table = document.getElementById(fieldId+'_kvtable');
		var rowCount = table.rows.length;
		for(var i=0; i<rowCount; i++) {
			 var node = dojo.byId(table.rows[i].id);
			 dojo.removeClass(node, 'alternate_1');
             dojo.removeClass(node, 'alternate_2');
			 if( (i % 2) != 0 ){
	             dojo.addClass(node, 'alternate_2');
	         }else{
	             dojo.addClass(node, 'alternate_1');
	         }
		 }
	}
	function setKVValue(fieldId, fieldValueId){
		var fieldValue = document.getElementById(fieldValueId);
		var table = document.getElementById(fieldId+'_kvtable');
		var rowCount = table.rows.length;
		var jsonStr = "{";
		for(var i=0; i<rowCount; i++) {
			var rowId = table.rows[i].id;
			var key = document.getElementById(rowId+'_k').value;
			var value = document.getElementById(rowId+'_v').value;
			jsonStr+= '"' + key + '"' + ":" + '"' + value + '"' + (i!=rowCount-1?",":"");
		}
        jsonStr+="}";
        fieldValue.value=jsonStr;

	}

	function editText(inode) {
		editTextManager.editText(inode);
	}
	
	var textEditor = new Array();
	var aceTextId;
	function aceText(textarea,keyValue) {
		if(document.getElementById('aceTextArea_'+textarea).style.position != 'relative'){
			document.getElementById('aceTextArea_'+textarea).style.position='relative';
			textEditor[textarea] = ace.edit('aceTextArea_'+textarea);
			textEditor[textarea].setTheme("ace/theme/textmate");
			textEditor[textarea].getSession().setMode("ace/mode/"+keyValue);
			textEditor[textarea].getSession().setUseWrapMode(true);
			aceTextId = textarea;
		}
    	dijit.byId("toggleEditor_"+textarea).disabled=true;
		var acetId = document.getElementById('aceTextArea_'+textarea);
		var aceClass = acetId.className;
		if (dijit.byId("toggleEditor_"+textarea).checked) {
			document.getElementById(textarea).style.display = "none";
			acetId.className = aceClass.replace('classAce', 'aceText');
			textEditor[textarea].setValue(document.getElementById(textarea).value);
			textEditor[textarea].clearSelection();
			enabledCodeAreas[textarea]=true;
		} else {
			var editorText = textEditor[textarea].getValue();
			acetId.className = aceClass.replace('aceText', 'classAce');
			document.getElementById(textarea).style.display = "inline";
			document.getElementById(textarea).value = editorText;
			textEditor[textarea].setValue("");
			enabledCodeAreas[textarea]=false;
		}
		dijit.byId("toggleEditor_"+textarea).disabled=false;
	}
</script>


