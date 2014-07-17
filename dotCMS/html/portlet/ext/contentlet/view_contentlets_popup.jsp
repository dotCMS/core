<%@ include file="/html/portlet/ext/contentlet/init.jsp" %>
<%@ page import="java.util.*" %>
<%@ page import="com.dotmarketing.portlets.languagesmanager.model.Language" %>
<%@ page import="com.dotmarketing.portlets.structure.model.Structure" %>
<%@ page import="com.liferay.portal.model.User" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@page import="com.dotmarketing.business.Role"%>
<%
	Structure structure = (Structure)request.getAttribute (com.dotmarketing.util.WebKeys.Structure.STRUCTURE);
	List<Language> languages = (List<Language>)request.getAttribute (com.dotmarketing.util.WebKeys.LANGUAGES);
	String selectedHostId = (String) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
	if ((selectedHostId == null) || selectedHostId.equals("allHosts"))
		selectedHostId = "";

	java.util.Map params = new java.util.HashMap();
	params.put("struts_action",new String[] {"/ext/contentlet/view_contentlets"});
		
	String referer = com.dotmarketing.util.PortletURLUtil.getActionURL(request,WindowState.MAXIMIZED.toString(),params);
	String popup = request.getParameter ("popup");

%>


<script type='text/javascript' src='/dwr/interface/StructureAjax.js'></script>
<script type='text/javascript' src='/dwr/interface/CategoryAjax.js'></script>
<script type='text/javascript' src='/dwr/interface/ContentletAjax.js'></script>
<script type='text/javascript' src='/dwr/engine.js'></script>
<script type='text/javascript' src='/dwr/util.js'></script>
<script type='text/javascript' src='/dwr/interface/TagAjax.js'></script>
<%-- jsp:include page="/html/portlet/ext/folders/view_folders_js.jsp" / --%>

<script language="Javascript">

	dojo.require("dijit.form.Button");
	dojo.require("dijit.form.MultiSelect");	
	
	var structureInode = '<%= structure.getInode () %>';
	var structureVelVar='<%= structure.getVelocityVarName() %>';
	var radiobuttonsIds = new Array();
	var checkboxesIds = new Array();
	var counter_radio = 0;
	var counter_checkbox = 0;
	var userId = '<%= user.getUserId() %>';
	var	hasHostFolderField = false;
	var setDotFieldTypeStr = "";
	var DOT_FIELD_TYPE = "dotFieldType";
		
	function structureChanged () {
		var form = document.getElementById("search_form");
		StructureAjax.getSearchableStructureFields (structureInode, fillFields);
		StructureAjax.getStructureCategories (structureInode, fillCategories);
		dwr.util.removeAllRows("results_table");
		hideMatchingResults ();
		document.getElementById("nextDiv").style.display = "none";
		document.getElementById("previousDiv").style.display = "none";
		counter_radio = 0;
	    counter_checkbox = 0;
        setDotFieldTypeStr = "";
	}
	
	function fieldName (field) { 
	     var type = field["fieldFieldType"]; 
	     if(type=='category'){
	          return "";
	     }else{
	    	 return "<strong>" + field["fieldName"] + ":</strong>"; //DOTCMS -4381
	  }
	}

///////////

//DOTCMS-3232
	function getHostValue(){
	  if(!isInodeSet(dijit.byId('FolderHostSelector').attr('value'))){
	    dojo.byId("hostField").value = "";
	    dojo.byId("folderField").value = "";
	  }else{
	    var data = dijit.byId('FolderHostSelector').attr('selectedItem');
		if(data["type"]== "host"){
			dojo.byId("hostField").value =  dijit.byId('FolderHostSelector').attr('value');
			dojo.byId("folderField").value = "";
		}else if(data["type"]== "folder"){
			dojo.byId("hostField").value = "";
		    dojo.byId("folderField").value =  dijit.byId('FolderHostSelector').attr('value');
	    }
	    
	    
	  }
   } 

// DOTCMS-3896
function renderSearchField (field) {
		
		
 		var fieldVelocityVarName = field["fieldVelocityVarName"];
 		var fieldContentlet = field["fieldContentlet"];
		var value = "";
       	
		
		var type = field["fieldFieldType"];
	    if(type=='checkbox'){
		   //checkboxes fields
		    var option = field["fieldValues"].split("\r\n");

		    
		    var result="";
		    
		    for(var i = 0; i < option.length; i++){
		       var actual_option = option[i].split("|");
		       if(actual_option.length > 1 && actual_option[1] !='' && actual_option[1].length > 0){
		       
			       	if(dijit.byId(structureVelVar+"."+ fieldVelocityVarName + "Field"+ counter_checkbox)){
						dijit.byId(structureVelVar+"."+ fieldVelocityVarName + "Field"+ counter_checkbox).destroy();
					}   
		       
		       
		       
		       		result = result + "<input type=\"checkbox\" dojoType=\"dijit.form.CheckBox\" value=\"" + actual_option[1] + "\" id=\"" + structureVelVar+"."+ fieldVelocityVarName + "Field"+ counter_checkbox+"\" name=\"" + structureVelVar+"."+ fieldVelocityVarName + "\"> " + actual_option[0] + "<br>\n";
		       	    checkboxesIds[counter_checkbox] =  structureVelVar+"."+fieldVelocityVarName + "Field" + counter_checkbox;
		       	 	setDotFieldTypeStr = setDotFieldTypeStr 
		       	 						+ "dojo.attr("
		       	 						+ "'" + structureVelVar+"."+fieldVelocityVarName + "Field" + counter_checkbox + "'"
		       	 						+ ",'" + DOT_FIELD_TYPE + "'"
		       	 						+ ",'" + type + "');"
		       	 						
		       	    counter_checkbox++;	
		       	}
		    }
		    return result;
	    
	  }else if(type=='radio'){
	    	dijit.registry.remove(structureVelVar+"."+ fieldVelocityVarName +"Field");
		    //radio buttons fields
		    var option = field["fieldValues"].split("\r\n");
		    var result="";
		    
		    for(var i = 0; i < option.length; i++){
		       var actual_option = option[i].split("|");
		       if(actual_option.length > 1 && actual_option[1] !='' && actual_option[1].length > 0){

		    		if(dijit.byId(structureVelVar+"."+ fieldVelocityVarName + "Field"+ counter_radio)){
						dijit.byId(structureVelVar+"."+ fieldVelocityVarName + "Field"+ counter_radio).destroy();
					}   
		    	    
		       		result = result + "<input type=\"radio\" dojoType=\"dijit.form.RadioButton\" value=\"" + actual_option[1] + "\" id=\"" + structureVelVar+"."+ fieldVelocityVarName + "Field"+ counter_radio+"\" name=\"" + structureVelVar+"."+ fieldVelocityVarName + "\"> " + actual_option[0] + "<br>\n";
		       		 radiobuttonsIds[counter_radio] = structureVelVar+"."+fieldVelocityVarName + "Field"+ counter_radio;

		       		 setDotFieldTypeStr = setDotFieldTypeStr 
	 						+ "dojo.attr("
	 						+ "'" + structureVelVar+"."+fieldVelocityVarName + "Field" + counter_radio + "'"
	 						+ ",'" + DOT_FIELD_TYPE + "'"
	 						+ ",'" + type + "');"
	 								       		 
		       		 counter_radio++;
		       	}
		    }
		    return result;
	    
	  }else if(type=='select' || type=='multi_select'){
	  		dijit.registry.remove(structureVelVar+"."+ fieldVelocityVarName +"Field");
		    var option = field["fieldValues"].split("\r\n");
		    var result="";
		    if (type=='multi_select')
				result = result+"<select  dojoType='dijit.form.MultiSelect'  multiple=\"multiple\" size=\"4\" id=\"" + structureVelVar+"."+ fieldVelocityVarName + "Field\" name=\"" + structureVelVar+"."+ fieldVelocityVarName + "\">\n";
			else 
				result = result+"<select  dojoType='dijit.form.FilteringSelect' id=\"" + structureVelVar+"."+ fieldVelocityVarName + "Field\" style=\"width:160px;\" name=\"" + structureVelVar+"."+ fieldVelocityVarName + "\">\n<option value=\"\">None</option>";
			
		    for(var i = 0; i < option.length; i++){
		       var actual_option = option[i].split("|");
		       if(actual_option.length > 1 && actual_option[1] !='' && actual_option[1].length > 0){
					auxValue = actual_option[1];
		       	    if(fieldContentlet.indexOf("bool") != -1)
		       	    {
			        	if(actual_option[1] == "true" || actual_option[1] == "t" || actual_option[1] == "1")
			            {
			            	auxValue = 't';
			            }else if(actual_option[1] == "false" || actual_option[1] == "f" || actual_option[1] == "0")
			            {
				        	auxValue = 'f';
			            }
			        }
		       		result = result + "<option value=\"" + auxValue + "\" >"+actual_option[0]+"</option>\n";
		       	}
		    }

      		 setDotFieldTypeStr = setDotFieldTypeStr 
									+ "dojo.attr("
									+ "'" + structureVelVar+"."+fieldVelocityVarName + "Field" + "'"
									+ ",'" + DOT_FIELD_TYPE + "'"
									+ ",'" + type + "');"
		    
		    result = result +"</select>\n";
		    return result;
	    
	  }else if(type=='tag'){
		    var result="<table style='width:200px;' border=\"0\">";
			result = result + "<tr><td style='padding:0px;'>";
			result = result +"<textarea id=\"" + structureVelVar+"."+ fieldVelocityVarName + "Field\" name=\"" + structureVelVar+"."+ fieldVelocityVarName + "Field\" cols=\"20\" rows=\"2\" onkeyup=\"suggestTagsForSearch(this,'"+ structureVelVar+"."+ fieldVelocityVarName + "suggestedTagsDiv');\" style=\"border-color: #7F9DB9; border-style: solid; border-width: 1px; font-family: Verdana, Arial,Helvetica; font-size: 11px; height: 50px; width: 160px;\"></textarea><br/><span style=\"font-size:11px; color:#999;\"><%= LanguageUtil.get(pageContext, "Type-your-tag-You-can-enter-multiple-comma-separated-tags") %></span></td></tr>";
			result = result + "<tr><td valign=\"top\" style='padding:0px;'>";
			result = result + "<div id=\"" + structureVelVar+"." + fieldVelocityVarName + "suggestedTagsDiv\" style=\"height: 50px; font-size:10px;font-color:gray; width: 146px; border:1px solid #ccc;overflow: auto;\"></div><span style=\"font-size:11px; color:#999;\"><%= LanguageUtil.get(pageContext, "Suggested-Tags") %></span><br></td></tr></table>";

     		 setDotFieldTypeStr = setDotFieldTypeStr 
									+ "dojo.attr("
									+ "'" + structureVelVar+"."+fieldVelocityVarName + "Field" + "'"
									+ ",'" + DOT_FIELD_TYPE + "'"
									+ ",'" + type + "');"			
			
			return result;
	  }//http://jira.dotmarketing.net/browse/DOTCMS-3232
	  else if(type=='host or folder'){
	  
		  dojo.require("dotcms.dijit.form.HostFolderFilteringSelect");
		  // Below code is used to fix the "widget already registered error". 
		  if(dojo.byId('FolderHostSelector-hostFoldersTreeWrapper')){
			  dojo.byId('FolderHostSelector-hostFoldersTreeWrapper').remove();
		  } 
		  if(dijit.byId('FolderHostSelector')){
			  dijit.byId('FolderHostSelector').destroy();
		  }
		  if(dijit.byId('FolderHostSelector-tree')){
			  dijit.byId('FolderHostSelector-tree').destroy();
		 }
		  
		  var field = + structureVelVar+"."+fieldVelocityVarName + "Field";
		  var result = "<div id=\"FolderHostSelector\" style='width: 270px;' dojoType=\"dotcms.dijit.form.HostFolderFilteringSelect\" onChange=\"getHostValue();\"<%= UtilMethods.isSet(selectedHostId)?"hostId=\\\"" + selectedHostId + "\\\"":"" %> <%= UtilMethods.isSet(selectedHostId)?"value=\\\"" + selectedHostId + "\\\"":"" %>\"> </div>";

          hasHostFolderField = true;
 
       	   return result;  
  	  }else if(type=='category' || type=='hidden'){
	   
	     return "";
	     
	  }else if(type.indexOf("date") > -1){
	  	  	dijit.registry.remove(structureVelVar+"."+ fieldVelocityVarName + "Field");
			if(dijit.byId(structureVelVar+"."+ fieldVelocityVarName + "Field")){
				dijit.byId(structureVelVar+"."+ fieldVelocityVarName + "Field").destroy();
			}
			dojo.require("dijit.form.DateTextBox");

     		 setDotFieldTypeStr = setDotFieldTypeStr 
									+ "dojo.attr("
									+ "'" + structureVelVar+"."+fieldVelocityVarName + "Field" + "'"
									+ ",'" + DOT_FIELD_TYPE + "'"
									+ ",'" + type + "');"			
			
	        return "<input type=\"text\" dojoType=\"dijit.form.DateTextBox\" validate='return false;' invalidMessage=\"\"  id=\"" + structureVelVar+"."+ fieldVelocityVarName + "Field\" name=\"" + structureVelVar+"."+ fieldVelocityVarName + "\" value=\"" + value + "\">";
	        	  
	  }
	  
	  
	  else{
	  	dijit.registry.remove(structureVelVar+"."+ fieldVelocityVarName + "Field");
		if(dijit.byId(structureVelVar+"."+ fieldVelocityVarName + "Field")){
			dijit.byId(structureVelVar+"."+ fieldVelocityVarName + "Field").destroy();
		}

 		 setDotFieldTypeStr = setDotFieldTypeStr 
								+ "dojo.attr("
								+ "'" + structureVelVar+"."+fieldVelocityVarName + "Field" + "'"
								+ ",'" + DOT_FIELD_TYPE + "'"
								+ ",'" + type + "');"
		
        return "<input type=\"text\" dojoType=\"dijit.form.TextBox\"  id=\"" + structureVelVar+"."+ fieldVelocityVarName + "Field\" name=\"" + structureVelVar+"."+ fieldVelocityVarName + "\" value=\"" + value + "\">";
        
      }
	  
	}

	 
	
	var currentStructureFields;
	
	function fillFields (data) {
		currentStructureFields = data;
		dwr.util.removeAllRows("search_fields_table");
		var htmlstr = "<dl>";
		for(var i = 0; i < data.length; i++) {
			htmlstr += "<dt>" + fieldName(data[i]) + "</dt>";
			htmlstr += "<dd>" + renderSearchField(data[i]) + "</dd>";
		}
		htmlstr += "</d>";
		$('search_fields_table').update(htmlstr);
		dojo.parser.parse(dojo.byId('search_fields_table'));
        eval(setDotFieldTypeStr);
	}

	function categoryName (field) {
		return "<p align=\"right\">" + field["categoryName"] + ":</p>";
	}
	
	function categorySelect (field) {
		var selectId = field["categoryName"].replace(/[^A-Za-z0-9_]/, "") + "Select";
		return "<select size=\"2\" multiple name=\"categories\" id=\"" + selectId + "\"></select>";
	}
	
	function fillCategories (data) {
		var form = document.getElementById("search_form");
		dwr.util.removeAllRows("search_categories_table");
		form.categories = null;
		dwr.util.addRows("search_categories_table", data, [categoryName, categorySelect], { escapeHtml: false } );
		categories = data;
		setTimeout("fillSelects()", 200);
	}
	
	var categories = new Array();
	
	function fillSelects () {
		for (var i = 0; i < categories.length; i++) {
			var cat = categories[i];
			var selectId = cat["categoryName"].replace(/[^A-Za-z0-9_]/, "") + "Select";
			var mycallbackfnc = function(data) { fillCategorySelect(selectId, data); }
			CategoryAjax.getSubCategories(cat["inode"], '', { callback: mycallbackfnc, async: false });
		}
	}
	
	function fillCategorySelect (selectId, data) {
		fillCategoryOptions (selectId, data);
		var selectObj = document.getElementById (selectId);
		if (data.length > 1) {
			var len = data.length;
			if (len > 9) len = 9;
			selectObj.size = len;
		}
	}
	
	function fillCategoryOptions (selectId, data) {
		var select = document.getElementById(selectId);
		if (select != null) {
			for (var i = 0; i < data.length; i++) {
				var option = new Option ();
				option.text = data[i]['categoryName'];
				option.value = data[i]['inode'];
				option.style.marginLeft = (data[i]['categoryLevel']*10)+"px";
				select.options[select.options.length]=option;
			}
		}
	}	
	
	var currentPage = 1;
	
	var currentSortBy;

	function doSearch (page, sortBy) {
		
		var fieldsValues = new Array ();

		fieldsValues[fieldsValues.length] = "languageId";
		fieldsValues[fieldsValues.length] = "<%=session.getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE) %>";			
	
		for (var h = 0; h < currentStructureFields.length; h++) {
			
			var field = currentStructureFields[h];
			var fieldId = structureVelVar+"."+field["fieldVelocityVarName"] + "Field";
			var formField = document.getElementById(fieldId);			
			var fieldValue = "";
		
			if(formField != null){
				
				if(dojo.attr(formField.id,DOT_FIELD_TYPE) == 'select'){

					var tempDijitObj = dijit.byId(formField.id);
					fieldsValues[fieldsValues.length] = structureVelVar+"."+field["fieldVelocityVarName"];
					fieldsValues[fieldsValues.length] = tempDijitObj.value;					
					
				}else if(formField.type=='select-one' || formField.type=='select-multiple') {
					
				     var values = "";
				     for (var i=0; i<formField.options.length; i++) {
					    if (formField.options[i].selected) {
					      fieldsValues[fieldsValues.length] = structureVelVar+"."+field["fieldVelocityVarName"];
	  			  	      fieldsValues[fieldsValues.length] = formField.options[i].value;
					      
					    }
					  }
					  				  	
				}else {
					fieldsValues[fieldsValues.length] = structureVelVar+"."+field["fieldVelocityVarName"];
					fieldsValues[fieldsValues.length] = formField.value;
					
				}
			}
			
		}

		if (hasHostFolderField) {
			var hostValue = document.getElementById("hostField").value;
			var folderValue = document.getElementById("folderField").value;
			if (isInodeSet(hostValue)) {
				fieldsValues[fieldsValues.length] = "conHost";
				fieldsValues[fieldsValues.length] = hostValue;
			}
			if (isInodeSet(folderValue)) {
				fieldsValues[fieldsValues.length] = "conFolder";
				fieldsValues[fieldsValues.length] = folderValue;
			}
		}	
		
        for(var i=0;i < radiobuttonsIds.length ;i++ ){
			var formField = document.getElementById(radiobuttonsIds[i]);
			if(formField != null && formField.type=='radio') {
			    var values = "";
				if (formField.checked) {
					values = formField.value;
					fieldsValues[fieldsValues.length] = formField.name;
					fieldsValues[fieldsValues.length] = values;
				}
			}
		}
		
		for(var i=0;i < checkboxesIds.length ;i++ ){
			var formField = document.getElementById(checkboxesIds[i]);
			if(formField != null && formField.type=='checkbox') {
			    var values = "";
				if (formField.checked) {
					values = formField.value;
					fieldsValues[fieldsValues.length] = formField.name;
					fieldsValues[fieldsValues.length] = values;
				}
			}
		}
		
		var categoriesValues = new Array ();
		var form = document.getElementById("search_form");
		var categories = form.categories;
		if (categories != null) {
			if (categories.options != null) {
				var opts = categories.options;
				for (var j = 0; j < opts.length; j++) {
					var option = opts[j];
					if (option.selected) {
						categoriesValues[categoriesValues.length] = option.value;
					}
				}
			} else {
				for (var i = 0; i < categories.length; i++) {
					var catSelect = categories[i];
					var opts = catSelect.options;
					for (var j = 0; j < opts.length; j++) {
						var option = opts[j];
						if (option.selected) {
							categoriesValues[categoriesValues.length] = option.value;
						}
					}
				}
			}
		}
		if (page == null)
			currentPage = 1;
		else 
			currentPage = page;

		if (sortBy != null) {
			if (sortBy == currentSortBy)
				sortBy = sortBy + " desc";
			currentSortBy = sortBy;
		}
		ContentletAjax.searchContentlets (structureInode, fieldsValues, categoriesValues, false, false, currentPage, currentSortBy, null, null, fillResults)
	}
	
	function nextPage () {
		doSearch (currentPage + 1);
	}

	function previousPage () {
		doSearch (currentPage - 1);
	}

	function selectButton (data) {
		var inode = data["inode"];

		var live = data["live"] == "true"?true:false;
		var working = data["working"] == "true"?true:false;
		var deleted = data["deleted"] == "true"?true:false;
		var locked = data["locked"] == "true"?true:false;
		var liveSt = live?"1":"0";
		var workingSt = working?"1":"0";
		var permissions = data["permissions"];
		var write = userHasWritePermission (data, userId)?"1":"0";
		
		var button = "<button dojoType='dijit.form.Button' onclick=\"selectTreeLeaf('<%= popup %>','" + inode + "','" + inode + "','','" + inode + "');\">Select</button>";
		
		return button;
	}
	
	function noResults (data) {
		return '<div class="noResultsMessage"><%= LanguageUtil.get(pageContext, "No-Results-Found") %></div>';
	}
	
	var headers;
	function fillResults (data) {
		var counters = data[0];
		var hasNext = counters["hasNext"];
		var hasPrevious = counters["hasPrevious"];
		var total = counters["total"];

		headers = data[1];
		
		for (var i = 3; i < data.length; i++) {
			data[i - 3] = data[i];
		}
		data.length = data.length - 3;

		dwr.util.removeAllRows("results_table");

		var funcs = new Array ();
		if (data.length <= 0) {
			funcs[0] = noResults;
			dwr.util.addRows("results_table", [ headers ] , funcs, { escapeHtml: false } );
 			document.getElementById("nextDiv").style.display = "none";
 			document.getElementById("previousDiv").style.display = "none";
 			showMatchingResults (0);
			return;
		}

		fillResultsTable (headers, data);
		showMatchingResults (total);

		if (hasNext) {
 			document.getElementById("nextDiv").style.display = "";
 		} else {
 			document.getElementById("nextDiv").style.display = "none";
 		}
		
		if (hasPrevious) {
 			document.getElementById("previousDiv").style.display = "";
 		} else {
 			document.getElementById("previousDiv").style.display = "none";
 		}
	}
	
	function getHeader (field) {
		var fieldContentlet = structureVelVar+"."+field["fieldVelocityVarName"];
		var fieldName = field["fieldName"];
		return "<a class=\"beta\" href=\"javascript: doSearch (1, '" + fieldContentlet + "')\"><b>" + fieldName + "</b></a>";
	}

	function fillResultsTable (headers, data) {
		var table = document.getElementById("results_table");
		
		//Filling Headers
		var row = table.insertRow(table.rows.length);
		var cell = row.insertCell (row.cells.length);
		cell.setAttribute("class","beta");
		cell.setAttribute("className","beta");
		cell.setAttribute("width","5%");
		for (var i = 0; i < headers.length; i++) {
			var header = headers[i];
			var cell = row.insertCell (row.cells.length);
			cell.innerHTML = getHeader (header);
			cell.setAttribute("class","beta");
			cell.setAttribute("className","beta");
		}
		//Filling data
		for (var i = 0; i < data.length; i++) {
			var row = table.insertRow(table.rows.length);
			if (i % 2 == 1){
			 // row.setAttribute("bgcolor","#EEEEEE");
			 }
			var cellData = data[i];
			var cell = row.insertCell (row.cells.length);
			cell.innerHTML = selectButton(cellData);
			for (var j = 0; j < headers.length; j++) {
				var header = headers[j];
				var cell = row.insertCell (row.cells.length);
				var value = cellData[header["fieldVelocityVarName"]];
				if (value != null)
					cell.innerHTML = value;
			} 
		}		

		dojo.parser.parse("results_table_popup_menus");
		dojo.parser.parse("results_table");
		
		
	}
	
	function clearSearch () {

		
		for (var i = 0; i < categories.length; i++) {
			var mainCat = categories[i];
			var selectId = mainCat["categoryName"].replace(/[^A-Za-z0-9_]/, "") + "Select";
			var selectObj = document.getElementById(selectId);
			var options = selectObj.options;
			for (var j = 0; j < options.length; j++) {
				var opt = options[j];
				opt.selected = false;
			}
		}
	
		for (var h = 0; h < currentStructureFields.length; h++) {
			var field = currentStructureFields[h];
			//var fieldId = field["fieldContentlet"] + "Field";
			var fieldId = structureVelVar+"."+field["fieldVelocityVarName"] + "Field";
			var formField = document.getElementById(fieldId);
			if(formField != null) {
				 if(formField.type=='select-one' || formField.type=='select-multiple'){
					  var options = formField.options;
					  for (var j = 0; j < options.length; j++) {
						var opt = options[j];
						opt.selected = false;
					  }
				  } else {
					  formField.value = "";
					  var temp = dijit.byId(formField.id);
					  temp.reset();
				  }
			}
			if(field.fieldFieldType == 'host or folder') {
				if(dijit.byId('FolderHostSelector')!=null){
	    		    dijit.byId('FolderHostSelector')._setValueAttr("<%= UtilMethods.isSet(selectedHostId)? selectedHostId: ""%>");
	    		    getHostValue();
	    		}
			}
		}
		
		for(var i=0;i < radiobuttonsIds.length ;i++ ){
			var formField = document.getElementById(radiobuttonsIds[i]);			
			if(formField != null && formField.type=='radio') {
			    var values = "";
				if (formField.checked) {
					var temp = dijit.byId(formField.id);
					temp.reset();
				}
			}
		}
		
		for(var i=0;i < checkboxesIds.length ;i++ ){
			var formField = document.getElementById(checkboxesIds[i]);			
			if(formField != null && formField.type=='checkbox') {
			    var values = "";
				if (formField.checked) {
					var temp = dijit.byId(formField.id);
					temp.reset();
				}
					
			}
		}

		dwr.util.removeAllRows("results_table");
		document.getElementById("nextDiv").style.display = "none";
		document.getElementById("previousDiv").style.display = "none";
		
		hideMatchingResults ();
		dojo.parser.parse();
	}
	
	var userRolesIds = new Array ();
	<%
		List<Role> roles = com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser (user.getUserId());
		for (Role role : roles) {
	%>
	userRolesIds[userRolesIds.length] = '<%= role.getId() %>';
	<%
		}
	%>
	
	function userHasReadPermission (contentlet, userId) {
		var permissions = contentlet["permissions"];
		var owner = contentlet["owner"];
		var ownerCanRead = contentlet["ownerCanRead"];
		var hasPermission = false;
		if(owner == userId && ownerCanRead.valueOf() == 'true'){
			return true;
		}
		for (var i = 0; i < userRolesIds.length; i++) {
			var roleId = userRolesIds[i];
			var re = new RegExp("P" + roleId + "\\.1P");
			if (permissions.match(re)) {
				hasPermission = true;
			}
		}
		return hasPermission;
	}
	
	function userHasWritePermission (contentlet, userId) {
		var permissions = contentlet["permissions"];
		var owner = contentlet["owner"];
		var ownerCanWrite = contentlet["ownerCanWrite"];
		var hasPermission = false;
		if(owner == userId && ownerCanWrite.valueOf() == 'true'){
			return true;
		}
		for (var i = 0; i < userRolesIds.length; i++) {
			var roleId = userRolesIds[i];
			var re = new RegExp("P" + roleId + "\\.2P");
			if (permissions.match(re)) {
				hasPermission = true;
			}
		}
		return hasPermission;
	}
	
	function userHasPublishPermission (contentlet, userId) {
		var permissions = contentlet["permissions"];
		var owner = contentlet["owner"];
		var ownerCanPublish = contentlet["ownerCanPublish"];
		var hasPermission = false;
		if(owner == userId && ownerCanPublish.valueOf() == 'true'){
			return true;
		}
		for (var i = 0; i < userRolesIds.length; i++) {
			var roleId = userRolesIds[i];
			var re = new RegExp("P" + roleId + "\\.4P");
			if (permissions.match(re)) {
				hasPermission = true;
			}
		}
		return hasPermission;
	}
	
	function showMatchingResults (num) {
 			var div = document.getElementById("matchingResultsDiv")
 			div.style.display = "";
 			div.innerHTML = "<b><%= LanguageUtil.get(pageContext, "Matching-Results") %> (" + num + ")</b>";
	}	

	function hideMatchingResults () {
 			var div = document.getElementById("matchingResultsDiv")
 			div.style.display = "none";
	}	

	function useLoadingMessage(message) {
	  var loadingMessage;
	  if (message) loadingMessage = message;
	  else loadingMessage = "Loading";
	
	  dwr.engine.setPreHook(function() {
	    var disabledZone = $('disabledZone');
	    if (!disabledZone) {
	      disabledZone = document.createElement('div');
	      disabledZone.setAttribute('id', 'disabledZone');
	      disabledZone.style.position = "absolute";
	      disabledZone.style.zIndex = "1000";
	      disabledZone.style.left = "0px";
	      disabledZone.style.top = "0px";
	      disabledZone.style.width = "100%";
	      disabledZone.style.height = "100%";
	      document.body.appendChild(disabledZone);
	      var messageZone = document.createElement('div');
	      messageZone.setAttribute('id', 'messageZone');
	      messageZone.style.position = "absolute";
	      messageZone.style.top = "0px";
	      messageZone.style.right = "0px";
	      messageZone.style.background = "red";
	      messageZone.style.color = "white";
	      messageZone.style.fontFamily = "Arial,Helvetica,sans-serif";
	      messageZone.style.padding = "4px";
	      disabledZone.appendChild(messageZone);
	      var text = document.createTextNode(loadingMessage);
	      messageZone.appendChild(text);
	    }
	    else {
	      $('messageZone').innerHTML = loadingMessage;
	      disabledZone.style.visibility = 'visible';
	    }
	  });
	
	  dwr.engine.setPostHook(function() {
	  	if ($('disabledZone') != null)
		    $('disabledZone').style.visibility = 'hidden';
	  });
	}	

	

//Layout Initialization
	function  resizeBrowser(){
		var viewport = dijit.getViewport();
		var viewport_height = viewport.h;

		var  e =  dojo.byId("borderContainer");
		dojo.style(e, "height", viewport_height -30+ "px");


		var  e =  dojo.byId("contentWrapper");
		dojo.style(e, "height", viewport_height -100+ "px");
	}
// need the timeout for back buttons

	dojo.addOnLoad(resizeBrowser);
	dojo.connect(window, "onresize", this, "resizeBrowser");
</script>


<jsp:include page="/html/portlet/ext/folders/menu_actions_js.jsp" />
<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value='<%= LanguageUtil.get(pageContext, "view-contentlets") %>' />

<form id="search_form" onsubmit="return false;">
	
<div dojoType="dijit.layout.BorderContainer" design="sidebar" gutters="false" liveSplitters="true" style="height:400px;" id="borderContainer" class="shadowBox headerBox">				
	
	<!-- START Left Column -->
	<div dojoType="dijit.layout.ContentPane" splitter="false" region="leading" style="width:350px;" class="lineRight">
		<input type="hidden" name="hostField" id="hostField" value=""/>
		<input type="hidden" name="folderField" id="folderField" value=""/>
		<div style="margin:10px 20px;">
			<b><%= LanguageUtil.get(pageContext, "Search") %>:</b> <%= structure.getName () %>
		</div>
		<div class="sideMenuWrapper" style="overflow:hidden;">
			<input type="hidden" name="structure_inode" id="structure_inode<%= structure.getInode() %>" value="<%= structure.getInode() %>">
			<div id="search_fields_table"></div>
			<div id="search_categories_table"></div>
			<div class="clear"></div>
			<div class="buttonRow">
				<button dojoType="dijit.form.Button" onclick="doSearch()" iconClass="searchIcon"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Search")) %></button>
				<button dojoType="dijit.form.Button" onClick="clearSearch()" iconClass="cancelIcon"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Clear-Search")) %></button>
			</div>
		</div>     
	</div>
        
    <!-- START Right Column -->
	<div dojoType="dijit.layout.ContentPane" splitter="true" region="center">
		<div id="contentWrapper" style="overflow:auto;margin-top:36px;">
        	<div id="matchingResultsDiv" style="display: none"><%= LanguageUtil.get(pageContext, "Results") %></div>
			<table id="results_table" class="listingTable"></table>
		</div>
		<div class="yui-g buttonRow">
			<div class="yui-u first" style="text-align:left;">
		        <div id="previousDiv" style="display: none;">
		             <button dojoType="dijit.form.Button" class="bg" onClick="previousPage()" iconClass="previousIcon"><%= LanguageUtil.get(pageContext, "Previous") %></button>
		        </div>
			</div>
			<div class="yui-u" style="text-align:right;">
		        <div id="nextDiv" style="display: none;">
		             <button dojoType="dijit.form.Button" class="bg" onClick="nextPage()" iconClass="nextIcon"><%= LanguageUtil.get(pageContext, "Next") %></button>
		        </div>
			</div>
		</div>
	</div>
</div>

</form>
	
</liferay:box>

<script language="javascript">
this.focus();
	structureChanged();
	useLoadingMessage("Loading");
	
</script>

