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

<script src="/html/js/codemirror/js/codemirror.js" type="text/javascript"></script>
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
		var myDate = dijit.byId(varName + "Date");
		var x = new Date();
		if(myDate != null){
			x = myDate.getValue(); 
		}
		var month = (x.getMonth() +1) + "";
		month = (month.length < 2) ? "0" + month : month;
		var day = (x.getDate() ) + "";
		day = (day.length < 2) ? "0" + day : day;
		year = x.getFullYear();
		dateValue= year + "-" + month + "-" + day + " ";

		if (dijit.byId(varName + 'Time') != null) {
			var time = dijit.byId(varName + 'Time').value;
			var hour = time.getHours();
			if(hour < 10) hour = "0" + hour;
			var min = time.getMinutes();
			if(min < 10) min = "0" + min;
			dateValue += hour + ":" + min; 
		} else {
			dateValue += "00:00";
		}
		
		field.value = dateValue;
	}

	  function removeThumbnail(x, inode) {
	    	dojo.query(".thumbnailDiv" + x).forEach(function(node, index, arr){
	    		dojo.destroy(node);
	    	})
	    	var dt = dojo.byId(x+'dt');
	    	dt.innerHTML = '';
	    	
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
	

	
	function enableDisableWysiwygCodeOrPlain(id) {
		var toggleValue=dijit.byId(id+'_toggler').attr('value');
		if (toggleValue=="WYSIWYG"){
			toWYSIWYG(id);
			}
		else if(toggleValue=="CODE"){
			toCodeArea(id);
			}
		else if(toggleValue=="PLAIN"){
			toPlainView(id);
			}
	}

	function toPlainView(id) {
		if(enabledWYSIWYG[id]){
			disableWYSIWYG(id);
			dojo.query('#'+id).style({display:''});
		}
		else if(enabledCodeAreas[id]){
			codeMirrorRemover(id);
		}
		if(!isWYSIWYGEnabled(id))
        {
			//Updating the list of disabled wysiwyg list
			var elementWysiwyg = document.getElementById("disabledWysiwyg");
			var wysiwygValue = elementWysiwyg.value;
	
			var result = "";
			if(wysiwygValue != "")
			{
				var wysiwygValueArray = wysiwygValue.split(",");
				
				for(i = 0;i < wysiwygValueArray.length;i++)
				{
					var number = wysiwygValueArray[i];
					number = trimString(number);
					if(number.indexOf("<%=com.dotmarketing.util.Constants.WYSIWYG_PLAIN_SEPARATOR%>")!=-1){
						number = number.replace("<%=com.dotmarketing.util.Constants.WYSIWYG_PLAIN_SEPARATOR%>","");
					}
					if(number != id)
					{
						result += number + ",";
					}
				}
			}
			result += id+"<%=com.dotmarketing.util.Constants.WYSIWYG_PLAIN_SEPARATOR%>";
			elementWysiwyg.value = result;
		}
	}
	
	function toCodeArea(id) {
		if(enabledWYSIWYG[id]){
			disableWYSIWYG(id);
		}
		if(!enabledCodeAreas[id])
			codeMirrorArea(id);
	}

	function toWYSIWYG(id) {
		if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.contentlet.switch.wysiwyg")) %>'))
        {   
			if(enabledCodeAreas[id]){
				codeMirrorRemover(id);
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

			
			//Updating the disabled wysiwyg list 
			var elementWysiwyg = document.getElementById("disabledWysiwyg");
			var wysiwygValue = elementWysiwyg.value;
			var result = "";
			
			if(wysiwygValue != "")
			{
				var wysiwygValueArray = wysiwygValue.split(",");		
					
				for(i = 0; i < wysiwygValueArray.length; i++)
				{
					var number = wysiwygValueArray[i];
					number = trimString(number);
					if(number.indexOf("<%=com.dotmarketing.util.Constants.WYSIWYG_PLAIN_SEPARATOR%>")!=-1){
						number = number.replace("<%=com.dotmarketing.util.Constants.WYSIWYG_PLAIN_SEPARATOR%>","");
					}
					if(number != textAreaId)
					{
						result += number + ",";
					}
				}
				result = result.substring(0, result.length - 1);
			}
			elementWysiwyg.value = result;

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
			//Updating the list of disabled wysiwyg list
			var elementWysiwyg = document.getElementById("disabledWysiwyg");
			var wysiwygValue = elementWysiwyg.value;
	
			var result = "";
			if(wysiwygValue != "")
			{
				var wysiwygValueArray = wysiwygValue.split(",");
				
				for(i = 0;i < wysiwygValueArray.length;i++)
				{
					var number = wysiwygValueArray[i];
					if(number != textAreaId)
					{
						result += number + ",";
					}
				}
			}
			result += textAreaId;
			elementWysiwyg.value = result;
			
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
			codeMirrorEditors[glossaryTermId].setCode(dojo.byId(glossaryTermId).value);
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
	var codeMirrorEditors = [];
	
	function codeMirrorArea(textarea){
		var dim = dojo.coords(textarea);
		var areaWidth = dim.w - 27;
		var areaHeight = "400px";
		codeMirrorEditors[textarea] = CodeMirror.fromTextArea(textarea, {
			width: areaWidth,
			height:areaHeight,    
			parserfile: ["parsedummy.js","parsexml.js", "parsecss.js", "tokenizejavascript.js", "parsejavascript.js", "parsehtmlmixed.js"],
			stylesheet: ["/html/js/codemirror/css/xmlcolors.css", "/html/js/codemirror/css/jscolors.css", "/html/js/codemirror/css/csscolors.css"],
			path: "/html/js/codemirror/js/",
			iframeClass: textarea+"_codeMirror"
		});
		enabledCodeAreas[textarea]=true;
	}
	 
	function codeMirrorRemover(textarea){
	    var editorText=codeMirrorEditors[textarea].getCode();
	    removeElement(dojo.query('.'+textarea+'_codeMirror')[0].parentNode);
		dojo.query('#'+textarea).style({display:''});
		dojo.query('#'+textarea)[0].value=editorText;
		enabledCodeAreas[textarea]=false;
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
			input.value=key;
			table.appendChild(input);
		    input = document.createElement("input");
			input.type="hidden";
			input.id=newRow.id+"_v";
			input.value=value;
			table.appendChild(input);
			var dndTable = new dojo.dnd.Source(node);
			dojo.connect(dndTable, "insertNodes", function(){
				 setKVValue(fieldId, fieldValueId);
		    	 recolorTable(fieldId);
			 });
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
</script> 


