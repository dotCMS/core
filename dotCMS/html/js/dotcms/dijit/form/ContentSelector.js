 /**
 *  
 * This is a dijit widget that present a dialog to select a content of the specified structure type,
 * it renders a content search dialog that lets the user search and select a content.
 * 
 * To include the dijit widget into your page
 * 
 * JS Side
 * 
 * <script type="text/javascript">
 * 	dojo.require("dotcms.dijit.form.ContentSelector");
 * 
 * ...
 * 
 * </script>
 * 
 * HTML side
 * 
 * <div id="myDijitId" jsId="myJSVariable" structureInode="structureInode" languageId="languageId" 
 * 	style="cssStyleOptions" title="My title" onContentSelected="your JS function" dojoType="dotcms.dijit.form.ContentSelector"></div>
 * 
 * How to show the dialog
 * 
 * <script type="text/javascript">
 * 	myJSVariable.show();
 * </script>
 * 
 * 
 * 
 * Properties
 * 
 * structureInode: required - this is used to search content of particular structure type.
 * 
 * languageId: non-required - this is the com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE used to search for contents.
 * 
 * id: non-required - this is the id of the widget if not specified then it will be auto generated.
 * 
 * jsId: non-required - if specified then dijit will be registered in the JS global environment with this name.
 * 
 * title: non-required - Title of the dialog.
 * 
 * onFileSelected: non-required - JS script or JS function callback to be executed when the user selects a content from the results,
 * 
 * the content object is passed to the function.
 * 
 */

dojo.provide("dotcms.dijit.form.ContentSelector");

	dojo.require("dijit._Widget");
	dojo.require("dijit._Templated");
	dojo.require("dijit.Dialog");
	dojo.require("dijit.form.Button");

dojo.declare("dotcms.dijit.form.ContentSelector", [dijit._Widget, dijit._Templated], {
		
		templatePath: dojo.moduleUrl("dotcms", "dijit/form/ContentSelector.jsp"),
		selectButtonTemplate: '<button id="{buttonInode}" class="resultButton" dojoType="dijit.form.Button">SELECT</button>',
		widgetsInTemplate: true,
		title: '',		
		structureInode: '',
		structureVelVar: '',
		setDotFieldTypeStr: '',		
		currentSortBy: "",
		DOT_FIELD_TYPE: 'dotFieldType',
		hasHostFolderField: false,
		counter_radio: 0,
	    counter_checkbox: 0,
	    languageId: '',
	    currentPage: 1,
	    searchCounter: 1,
	    dialogCounter: 1,
	    headers:new Array(),
	    categories: new Array(),
	    checkboxesIds: {},
	    radiobuttonsIds: {},
	    resultsButtonIds: new Array(),
	    currentStructureFields: new Array(),	    
	    tagTextValue:'',
	    suggestedTagsTextValue:'',
	    noResultsTextValue:'',
	    matchResultsTextValue: '',
		
		postCreate: function () {
			StructureAjax.getStructureDetails(this.structureInode,dojo.hitch(this, this._structureDetailsCallback));
			if(this.title != '')
				this.dialog.set('title',this.title);			
			this.tagTextValue = this.tagText.value;
		    this.suggestedTagsTextValue = this.suggestedTagsText.value;
		    this.noResultsTextValue = this.noResultsText.value;
		    this.matchResultsTextValue = this.matchResultsText.value;
			this.dialog.hide();
			dojo.parser.parse(this.search_fields_table);
		},
		
		show: function () {
			this._clearSearch();
			this.dialog.show();
		},
		
		hide: function () {
			this._clearSearch();
			this.dialog.hide();
		},
		
		_structureDetailsCallback: function (structure) {
			this.structureName.innerHTML = structure['name'];
			this.structureVelVar = structure['velocityVarName'];
			this._structureChanged();
		},
		
		_structureChanged: function () {
			StructureAjax.getSearchableStructureFields (this.structureInode,dojo.hitch(this, this._fillFields));
			StructureAjax.getStructureCategories (this.structureInode,dojo.hitch(this, this._fillCategories));
			this._hideMatchingResults ();
			this.nextDiv.style.display = "none";
			this.previousDiv.style.display = "none";
			//this.counter_radio = 0;
		    this.counter_checkbox = 0;			
		},
		
		_fillFields: function (data){
			this.currentStructureFields = data;
			this.search_fields_table.innerHTML = "";
			var htmlstr = "<dl>";
			for(var i = 0; i < data.length; i++) {
				htmlstr += "<dt>" + this._fieldName(data[i]) + "</dt>"; 
				htmlstr += "<dd>" + this._renderSearchField(data[i]) + "</dd>";
			}
			htmlstr += "</d>";
			dojo.place(htmlstr,this.search_fields_table);			
			dojo.parser.parse(this.search_fields_table);
			eval(this.setDotFieldTypeStr);		
		},
		
		_fieldName: function  (field) { 
		     var type = field["fieldFieldType"]; 
		     if(type=='category'){
		          return "";
		     }else{
		    	 return "<strong>" + field["fieldName"] + ":</strong>"; //DOTCMS -4381
		     }
		},
		
		// DOTCMS-3896
		_renderSearchField: function (field) {
				
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
				       if(actual_option.length > 1 && actual_option[1] !='' && actual_option[1].length > 0) {
				    	    var checkId=this.structureVelVar+"."+ fieldVelocityVarName + "Field-D"+ this.dialogCounter+"-O"+i;
				       		result = result + "<input type=\"checkbox\" dojoType=\"dijit.form.CheckBox\" value=\"" + actual_option[1] + "\" "+
				       		                         "id=\"" + checkId +"\" "+
				       		                         "name=\"" + this.structureVelVar+"."+ fieldVelocityVarName + this.dialogCounter + "\"> " + 
				       		                         actual_option[0] + "<br>\n";
				       		if(!this.checkboxesIds[this.dialogCounter])
				       			this.checkboxesIds[this.dialogCounter]=new Array();
				       	    this.checkboxesIds[this.dialogCounter][this.checkboxesIds[this.dialogCounter].length] = checkId;
				       	 	this.setDotFieldTypeStr = this.setDotFieldTypeStr 
				       	 						+ "dojo.attr("
				       	 						+ "'" + checkId + "'"
				       	 						+ ",'" + this.DOT_FIELD_TYPE + "'"
				       	 						+ ",'" + type + "');";
				       	}
				    }
				    return result;
			    
			  }else if(type=='radio'){
				  dijit.registry.remove(this.structureVelVar+"."+ fieldVelocityVarName +"Field" + this.counter_radio);
				    //radio buttons fields
				    var option = field["fieldValues"].split("\r\n");
				    var result="";
				    
				    for(var i = 0; i < option.length; i++){
				       var actual_option = option[i].split("|");
				       if(actual_option.length > 1 && actual_option[1] !='' && actual_option[1].length > 0){
				       		result = result + "<input type=\"radio\" dojoType=\"dijit.form.RadioButton\" value=\"" + actual_option[1] + "\" id=\"" + this.structureVelVar+"."+ fieldVelocityVarName + "Field"+ this.counter_radio+"\" name=\"" + this.structureVelVar+"."+ fieldVelocityVarName + "\"> " + actual_option[0] + "<br>\n";
				       		 if(!this.radiobuttonsIds[this.dialogCounter])
				       			this.radiobuttonsIds[this.dialogCounter]=new Array();
				       		 this.radiobuttonsIds[this.dialogCounter][this.radiobuttonsIds[this.dialogCounter].length] = this.structureVelVar+"."+fieldVelocityVarName + "Field"+ this.counter_radio;

				       		 this.setDotFieldTypeStr = this.setDotFieldTypeStr 
			 						+ "dojo.attr("
			 						+ "'" + this.structureVelVar+"."+fieldVelocityVarName + "Field" + this.counter_radio + "'"
			 						+ ",'" + this.DOT_FIELD_TYPE + "'"
			 						+ ",'" + type + "');";
			 								       		 
				       		 this.counter_radio++;
				       	}
				    }
				    return result;
			    
			  }else if(type=='select' || type=='multi_select'){
				    var fieldId=this.structureVelVar+"."+ fieldVelocityVarName +"Field" + this.dialogCounter;
			  		dijit.registry.remove(fieldId);
				    var option = field["fieldValues"].split("\r\n");
				    var result="";
				    if (type=='multi_select')
						result = result+"<select  dojoType='dijit.form.MultiSelect'  multiple=\"multiple\" size=\"4\" id=\"" + fieldId + "\" name=\"" + this.structureVelVar+"."+ fieldVelocityVarName + "\">\n";
					else 
						result = result+"<select  dojoType='dijit.form.FilteringSelect' id=\"" + fieldId + "\" style=\"width:160px;\" name=\"" + this.structureVelVar+"."+ fieldVelocityVarName + "\">\n<option value=\"\">None</option>";
					
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

		      		 this.setDotFieldTypeStr = this.setDotFieldTypeStr 
											+ "dojo.attr("
											+ "'" + fieldId + "'"
											+ ",'" + this.DOT_FIELD_TYPE + "'"
											+ ",'" + type + "');";
				    
				    result = result +"</select>\n";
				    return result;
			    
			  }else if(type=='tag'){ 
					var result="<table style='width:200px;' border=\"0\">";
					result = result + "<tr><td style='padding:0px;'>";
					result = result +"<textarea id=\"" + this.structureVelVar+"."+ fieldVelocityVarName + "Field " + this.dialogCounter
									+ "Field\" name=\"" + this.structureVelVar+"."+ fieldVelocityVarName 
									+ "Field\" cols=\"20\" rows=\"2\" onkeyup=\"suggestTagsForSearch(this,'"
									+ this.structureVelVar+"."+ fieldVelocityVarName + "suggestedTagsDiv" + this.dialogCounter + "');\" " 
									+ " style=\"border-color: #7F9DB9; border-style: solid; border-width: 1px; "
									+ " font-family: Verdana, Arial,Helvetica; font-size: 11px; height: 50px; width: 160px;\" "
									+ " ></textarea><br/><span style=\"font-size:11px; color:#999;\"> " 
									+ this.tagTextValue
									+ " </span> "  
									+ " </td></tr>";
					result = result + "<tr><td valign=\"top\" style='padding:0px;'>";
					result = result + "<div id=\"" + this.structureVelVar+"." + fieldVelocityVarName + "suggestedTagsDiv" + this.dialogCounter + "\" "
									+ " style=\"height: 50px; font-size:10px;font-color:gray; width: 146px; border:1px solid #ccc;overflow: auto;\" "
									+ "></div><span style=\"font-size:11px; color:#999;\"> " 
									+ this.suggestedTagsTextValue
									+ "</span><br></td></tr></table>"; 

		     		 this.setDotFieldTypeStr = this.setDotFieldTypeStr
											+ "dojo.attr("
											+ "'" + this.structureVelVar+"."+fieldVelocityVarName + "Field" + this.dialogCounter +  "'"
											+ ",'" + this.DOT_FIELD_TYPE + "'"
											+ ",'" + type + "');";
					
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
				  
				  var hostId = "";
				  var fieldValue = hostId;
				  
				  var result = "<div id=\"FolderHostSelector" + this.dialogCounter + "\" style='width270px' dojoType=\"dotcms.dijit.form.HostFolderFilteringSelect\" includeAll=\"true\" "
					  			+" hostId=\"" + hostId + "\" value = \"" + fieldValue + "\"" + "></div>";

				  this.hasHostFolderField = true;
		 
		       	   return result;  
		  	  }else if(type=='category' || type=='hidden'){
			   
			     return "";
			     
			  }else if(type.indexOf("date") > -1){
				  var fieldId=this.structureVelVar+"."+ fieldVelocityVarName + "Field" + this.dialogCounter;
			  	  	dijit.registry.remove(fieldId);
					if(dijit.byId(fieldId)){
						dijit.byId(fieldId).destroy();
					}
					dojo.require("dijit.form.DateTextBox");

		     		 this.setDotFieldTypeStr = this.setDotFieldTypeStr 
											+ "dojo.attr("
											+ "'" + fieldId + "'"
											+ ",'" + this.DOT_FIELD_TYPE + "'"
											+ ",'" + type + "');";			
					
			        return "<input type=\"text\" dojoType=\"dijit.form.DateTextBox\" constraints={datePattern:'MM/dd/yyyy'} validate='return false;' invalidMessage=\"\"  id=\"" + fieldId + "\" name=\"" + this.structureVelVar+"."+ fieldVelocityVarName + "\" value=\"" + value + "\">";
			  }else{
				var fieldId=this.structureVelVar+"."+ fieldVelocityVarName + "Field" + this.dialogCounter;
			  	dijit.registry.remove(fieldId);
				if(dijit.byId(fieldId)){
					dijit.byId(fieldId).destroy();
				}

		 		 this.setDotFieldTypeStr = this.setDotFieldTypeStr 
										+ "dojo.attr("
										+ "'" + fieldId + "'"
										+ ",'" + this.DOT_FIELD_TYPE + "'"
										+ ",'" + type + "');";		 		 
				
		        return "<input type=\"text\" dojoType=\"dijit.form.TextBox\"  id=\"" + fieldId + "\" name=\"" + this.structureVelVar+"."+ fieldVelocityVarName + "\" value=\"" + value + "\">";
		        
		      }			  
			},
			
			_fillCategories: function (data) {
				
				this.categories = data;				
				var searchCategoryList = this.search_categories_list;
				searchCategoryList.innerHTML ="";				
				var form = this.search_form;
				form.categories = null;
				dojo.require("dijit.form.MultiSelect");
				if (data != null) {
					categories = data;
					for(i = 0;i< categories.length;i++){
						dojo.create("dt", { innerHTML: categories[i]["categoryName"] + ":" }, searchCategoryList);
						var selectId = categories[i]["categoryName"].replace(/[^A-Za-z0-9_]/g, "") + "Select" + this.dialogCounter;
					  	dijit.registry.remove(selectId);
						if(dijit.byId(selectId)){
							dijit.byId(selectId).destroy();
						}
						var selectObj = "<select dojoType='dijit.form.MultiSelect' class='width-equals-200' multiple='true' name=\"categories\" id=\"" + selectId + "\"></select>";
						
						dojo.create("dd", { innerHTML: selectObj }, searchCategoryList);

					}
				}
				
				var fillCategoryOptions = function (selectId, data) {
					var select = document.getElementById(selectId);
					if (select != null) {
						for (var i = 0; i < data.length; i++) {
							var option = new Option ();
							option.text = data[i]['categoryName'];
							option.value = data[i]['inode'];
							
							option.style.marginLeft = (data[i]['categoryLevel']*10)+"px";
						
							select.options[i]=option;
						}
					}
				};

				var fillCategorySelect = function (selectId, data) {
					fillCategoryOptions (selectId, data);
					var selectObj = document.getElementById (selectId);
					if (data.length > 1) {
						var len = data.length;
						if (len > 9) len = 9;
						selectObj.size = len;
					}
				};

				for (var i = 0; i < categories.length; i++) {
					var cat = categories[i];
					var selectId = cat["categoryName"].replace(/[^A-Za-z0-9_]/g, "") + "Select" + this.dialogCounter;
					var mycallbackfnc = function(data) { fillCategorySelect(selectId, data); };

					CategoryAjax.getSubCategories(cat["inode"], '', { callback: mycallbackfnc, async: false });
				}
			},
			
			_hideMatchingResults: function  () {
	 			this.matchingResultsDiv.style.display = "none";
			},
		
			_doSearchPage1: function () {
				this._doSearch(1, null);		
			},
		
		_doSearch: function (page, sortBy) {
		
			var fieldsValues = new Array ();

			fieldsValues[fieldsValues.length] = "languageId";
			
			if(this.languageId == '')
				fieldsValues[fieldsValues.length] = this.htmlPageLanguage.value;
			else
				fieldsValues[fieldsValues.length] = this.languageId;
		
			for (var h = 0; h < this.currentStructureFields.length; h++) {
				
				var field = this.currentStructureFields[h];
				var fieldId = this.structureVelVar + "." + field["fieldVelocityVarName"] + "Field";
				var formField = document.getElementById(fieldId);
				if(formField == null) {
					fieldId=fieldId+this.dialogCounter;
					formField = document.getElementById(fieldId);
				}
				var fieldValue = "";
			
				if(formField != null){
					if(field["fieldFieldType"] == 'select'){

						var tempDijitObj = dijit.byId(formField.id);
						fieldsValues[fieldsValues.length] = this.structureVelVar+"."+field["fieldVelocityVarName"];
						fieldsValues[fieldsValues.length] = tempDijitObj.get('value');

					}else if(formField.type=='select-one' || formField.type=='select-multiple') {
						
					     var values = "";
					     for (var i=0; i<formField.options.length; i++) {
						    if (formField.options[i].selected) {
						      fieldsValues[fieldsValues.length] = this.structureVelVar+"."+field["fieldVelocityVarName"];
		  			  	      fieldsValues[fieldsValues.length] = formField.options[i].value;						      
						    }
						  }						  				  	
					}else {
						fieldsValues[fieldsValues.length] = this.structureVelVar+"."+field["fieldVelocityVarName"];
						fieldsValues[fieldsValues.length] = formField.value;						
					}
				}				
			}

			if (this.hasHostFolderField) {
				var fieldId='FolderHostSelector'+this.dialogCounter;
				if(!isInodeSet(dijit.byId(fieldId).attr('value'))){
				    this.hostField.value = "";
				    this.folderField.value = "";
				  }else{
				    var data = dijit.byId(fieldId).attr('selectedItem');
					if(data["type"]== "host"){
						this.hostField.value =  dijit.byId(fieldId).attr('value');
						this.folderField.value = "";
					}else if(data["type"]== "folder"){
						this.hostField.value = "";
					    this.folderField.value =  dijit.byId(fieldId).attr('value');
				    }		    
				  }
				
				var hostValue = this.hostField.value;
				var folderValue = this.folderField.value;
				if (isInodeSet(hostValue)) {
					fieldsValues[fieldsValues.length] = "conHost";
					fieldsValues[fieldsValues.length] = hostValue;
				}
				if (isInodeSet(folderValue)) {
					fieldsValues[fieldsValues.length] = "conFolder";
					fieldsValues[fieldsValues.length] = folderValue;
				}				
			}
			
			if(this.radiobuttonsIds[this.dialogCounter]) {
		        for(var i=0;i < this.radiobuttonsIds[this.dialogCounter].length ;i++ ){
					var formField = document.getElementById(this.radiobuttonsIds[this.dialogCounter][i]);
					if(formField != null && formField.type=='radio') {
					    var values = "";
						if (formField.checked) {
							values = formField.value;
							fieldsValues[fieldsValues.length] = formField.name;
							fieldsValues[fieldsValues.length] = values;
						}
					}
				}
			}
			
			if(this.checkboxesIds[this.dialogCounter]) {
				for(var i=0;i < this.checkboxesIds[this.dialogCounter].length ;i++ ){
					var formField = document.getElementById(this.checkboxesIds[this.dialogCounter][i]);
					if(formField != null && formField.type=='checkbox') {
					    var values = "";
						if (formField.checked) {
							values = formField.value;
							name = formField.name.substring(0,formField.name.length-1);
							fieldsValues[fieldsValues.length] = name;
							fieldsValues[fieldsValues.length] = values;
						}
					}
				}
			}
			
			var categoriesValues = new Array ();
			var form = this.search_form; 
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
				this.currentPage = 1;
			else 
				this.currentPage = page;

			if (sortBy != null) {
				if (sortBy == this.currentSortBy)
					sortBy = sortBy + " desc";
				this.currentSortBy = sortBy;
			}
			
			ContentletAjax.searchContentlets (this.structureInode, fieldsValues, categoriesValues, false, false, this.currentPage, this.currentSortBy, null, null, false, dojo.hitch(this, this._fillResults));

			this.searchCounter++; // this is used to eliminate the widget already registered exception upon repeated searchs.
		},
		
		_fillResults: function (data) {

			var counters = data[0];
			var hasNext = counters["hasNext"];
			var hasPrevious = counters["hasPrevious"];
			var total = counters["total"];

			this.headers = data[1];			
			
			for (var i = 3; i < data.length; i++) {
				data[i - 3] = data[i];
			}
			
			data.length = data.length - 3;

			dwr.util.removeAllRows(this.results_table);

			var funcs = new Array ();
			if (data.length <= 0) {
				funcs[0] = this._noResults();
				dwr.util.addRows(this.results_table, [ this.headers ] , funcs, { escapeHtml: false } );
	 			this.nextDiv.style.display = "none";
	 			this.previousDiv.style.display = "none";
	 			this._showMatchingResults (0);
				return;
			}

			this._fillResultsTable (this.headers, data);
			this._showMatchingResults (total);

			if (hasNext) {
	 			this.nextDiv.style.display = "";
	 		} else {
	 			this.nextDiv.style.display = "none";
	 		}
			
			if (hasPrevious) {
	 			this.previousDiv.style.display = "";
	 		} else {
	 			this.previousDiv.style.display = "none";
	 		}
		},
		
		_fillResultsTable: function (headers, data) {
			
			var table = this.results_table;
			
			//Filling Headers
			var row = table.insertRow(table.rows.length);
			var cell = row.insertCell (row.cells.length);
			cell.setAttribute("class","beta");
			cell.setAttribute("className","beta");
			cell.setAttribute("width","5%");

			for (var i = 0; i < headers.length; i++) {
				var header = headers[i];				
				var cell = row.insertCell (row.cells.length);
				cell.innerHTML = this._getHeader (header);
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
				cell.innerHTML = this._selectButton(cellData);
				for (var j = 0; j < this.headers.length; j++) {
					var header = this.headers[j];
					var cell = row.insertCell (row.cells.length);
					var value = cellData[header["fieldVelocityVarName"]];
					if (value != null)
						cell.innerHTML = value;
				} 
			}		

			//dojo.parser.parse("results_table_popup_menus");
			dojo.parser.parse(this.results_table);
			
			// Select button functionality
			var selected =  function(scope,content) {
				scope._onContentSelected(content);

			};			
			for (var i = 0; i < data.length; i++) {
				var asset = data[i];
				var selectButton = dojo.byId(this.searchCounter+asset.inode);
				if(selectButton.onclick==undefined){
					selectButton.onclick = dojo.hitch(this, selected, this, asset);
				}
			}

			// Header based sorting functionality
			var headerClicked =  function(scope,header) {
				scope._doSearch(1,this.structureVelVar+"."+header["fieldVelocityVarName"]);
			};
			for (var i = 0; i < headers.length; i++) {
				var header = headers[i];
				var anchor = dojo.byId(this.structureVelVar+"."+header["fieldVelocityVarName"]+"header");
				if(anchor.onclick==undefined){
					anchor.onclick = dojo.hitch(this, headerClicked, this, header);
				}
			}
		},
		
		_noResults: function (data) {
			return "<div class='noResultsMessage'>"	+ this.noResultsTextValue + "</div>";
		},
		
		/**
		 * Stub method that you can use dojo.connect to catch every time a user selects a content
		 * @param {Object} content
		 */
		onContentSelected: function (content) {
			
		},
		
		_onContentSelected: function (content) {
			this.onContentSelected(content);
			this._clearSearch();
			this.dialog.hide();
		},
		
		_getHeader: function (field) {
			var fieldContentlet = this.structureVelVar+"."+field["fieldVelocityVarName"];
			var fieldName = field["fieldName"];
			return "<a class=\"beta\" id=\"" + fieldContentlet + "header" +	"\"" 
					+ " href=\"#\"><b>" + fieldName + "</b></a>";			
		},
		
		_selectButton: function (data) {
			var inode = data["inode"];
			var buttonInode = this.searchCounter+inode;
			var button = dojo.replace(this.selectButtonTemplate,{buttonInode:buttonInode});	
			return button;
		},
		
		_showMatchingResults: function (num) {
 			var div = this.matchingResultsDiv;
 			div.style.display = "";
 			div.innerHTML = "<b> " + this.matchResultsTextValue + " (" + num + ")</b>";
		},
		
		_clearSearch: function () {

			dojo.empty(this.results_table);
			
			for (var i = 0; i < this.categories.length; i++) {
				var mainCat = this.categories[i];
				var selectId = mainCat["categoryName"].replace(/[^A-Za-z0-9_]/g, "") + "Select" + this.dialogCounter;
				var selectObj = document.getElementById(selectId);
				var options = selectObj.options;
				for (var j = 0; j < options.length; j++) {
					var opt = options[j];
					opt.selected = false;
				}
			}
		
			for (var h = 0; h < this.currentStructureFields.length; h++) {
				var field = this.currentStructureFields[h];
				var fieldId = this.structureVelVar+"."+field["fieldVelocityVarName"] + "Field";
				var formField = document.getElementById(fieldId);
				if(formField==null) {
					fieldId=fieldId+this.dialogCounter;
					formField = document.getElementById(fieldId);
				}
				
				if(formField != null) {
					 if(formField.type=='select-one' || formField.type=='select-multiple'){
						  var options = formField.options;
						  for (var j = 0; j < options.length; j++) {
							var opt = options[j];
							opt.selected = false;
						  }
					 } else if(field["fieldFieldType"] == 'select') {
						 var obj=dijit.byId(fieldId);
						 obj.attr('displayedValue','None');
					 } else {
						  formField.value = "";
						  var temp = dijit.byId(formField.id);
						  if(temp == undefined)
							  dojo.byId(formField.id);
						  if(temp != undefined)
							  temp.reset();
					  }
				}
				if(field.fieldFieldType == 'host or folder') {
					if(dijit.byId('FolderHostSelector'+this.dialogCounter)!=null){
		    		    dijit.byId('FolderHostSelector'+this.dialogCounter)._setValueAttr("");
		    		}
				}
			}
			
			if(this.radiobuttonsIds[this.dialogCounter]) {
				for(var i=0;i < this.radiobuttonsIds[this.dialogCounter].length ;i++ ){
					var formField = document.getElementById(this.radiobuttonsIds[this.dialogCounter][i]);			
					if(formField != null && formField.type=='radio') {
					    var values = "";
						if (formField.checked) {
							var temp = dijit.byId(formField.id);						
							temp.reset();
						}
					}
				}
			}
			
			if(this.checkboxesIds[this.dialogCounter]) {
				for(var i=0;i < this.checkboxesIds[this.dialogCounter].length ;i++ ){
					var formField = document.getElementById(this.checkboxesIds[this.dialogCounter][i]);			
					if(formField != null && formField.type=='checkbox') {
						if (formField.checked) {
							var temp = dijit.byId(formField.id);
							temp.reset();
						}	
					}
				}
			}

			dwr.util.removeAllRows(this.results_table);
			this.nextDiv.style.display = "none";
			this.previousDiv.style.display = "none";
			
			this._hideMatchingResults ();			
		},		
		
		_previousPage: function (){
			this._doSearch(this.currentPage-1);
		},
		
		_nextPage: function (){
			this._doSearch(this.currentPage+1);
		}
	});