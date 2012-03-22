<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.util.InodeUtils"%>
<%@page import="com.dotmarketing.portlets.categories.model.Category"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.categories.business.CategoryAPI"%>
<%@include file="/html/portlet/ext/categories/init.jsp"%>


<%  String dojoPath = Config.getStringProperty("path.to.dojo"); %>

<style type="text/css">
@import "<%=dojoPath%>/dojox/grid/enhanced/resources/claro/EnhancedGrid.css?b=<%= ReleaseInfo.getBuildNumber() %>";
@import "<%=dojoPath%>/dojox/grid/resources/tundraGrid.css?b=<%= ReleaseInfo.getBuildNumber() %>";
.realfile {
position: relative;
text-align: left;
z-index: 2;
margin-left: 270px;
width: 70px;
border: 1px solid red;
margin-top: -24px;
-moz-opacity: 0;
filter: alpha(opacity: 0);
opacity: 0;
cursor: pointer;
}

div.fakefile {
position: absolute;
top: 0px;
margin: 0;
z-index: 1;
line-height: 90%;
margin-top: -1px;
right: 0px;
}

.permissionWrapper{background:none;padding:0;margin:0 auto;width:90%;}

.permissionTable{width:100%;margin:0;}
.permissionTable td, .permissionTable th{font-size:88%;width:10%;text-align:center;vertical-align:middle;font-weight:bold;padding:3px 0 0 0;}
.permissionTable th.permissionType {width:40%;padding: 0 0 0 30px;font-weight:normal;text-align:left;}
.permissionTable th.permissionTitle {padding: 0 0 0 10px;font-weight:bold;}

.accordionEntry{width:100%;margin:0;visibility:hidden}
.accordionEntry td, .accordionEntry th{font-size:88%;width:10%;text-align:center;vertical-align:middle;font-weight:bold;padding:3px 0 0 0;}
.accordionEntry th.permissionType {width:40%;padding: 0 0 0 30px;font-weight:normal;text-align:left;}
.accordionEntry th.permissionTitle {padding: 0 0 0 10px;font-weight:bold;}

.dotCMSRolesFilteringSelect{width:200px;overflow:hidden;display:inline;}
#assetPermissionsMessageWrapper{padding-top: 15px;color: red;text-align: center;font-weight: bolder;}

td {font-size: 100%;}

</style>
<script type="text/javascript" src="/dwr/interface/CategoryAjax.js"></script>
<script type="text/javascript">
	dojo.require("dojox.grid.enhanced.plugins.Pagination");
	dojo.require("dojox.grid.enhanced.plugins.Search");
	dojo.require("dojox.data.QueryReadStore");
	dojo.require("dojox.grid.enhanced.plugins.IndirectSelection");
	dojo.require("dojo.io.iframe");
	dojo.require('dijit.layout.AccordionContainer');
	dojo.require('dijit.layout.ContentPane');
	dojo.require('dotcms.dijit.form.RolesFilteringSelect');
	dojo.require('dotcms.dojo.data.UsersReadStore');
	dojo.require("dojox.timing._base");
	dojo.require("dojo.hash");

	dojo.connect(dojo.global, "onhashchange", refresh);

    function refresh() {

    	var hashValue = decodeURIComponent(dojo.hash());

		if(typeof hashValue == "undefined" || hashValue == '') {
			doSearchHash(null);
		} else {
			doSearchHash(hashValue);
		}
	}

	var grid;
	var myStore;
	var currentInodeOrIdentifier;  // inode of the category
	var currentCatName;  // inode of the category
	var lastTabSelected;
	var parentCats = new Array();


	// format the name column of the grid to be an <a> element
	var formatHref = function(value, index) {
		return "<a href=\"javascript:drillDown("+index+")\" >"+value+"</a>";
	};

	var sortCat = function() {
		CategoryAjax.sortCategory(this.name, this.value);
	};

	var fixFocus = function() {
		var toBlur = document.activeElement;

		if(toBlur.id!="addCatName" &&
				toBlur.id!="addCatKey" &&
				toBlur.id!="addCatKeywords" &&
				toBlur.id.indexOf("dijit_form_NumberTextBox")) {
			toBlur.blur();
			this.focus();
		}

	};

	// format the sort column of the grid to be a NumberTextBox
	var sortFormatter = function(value, index) {
		var inode = grid.store.getValue(grid.getItem(index), 'inode');
		var sort_order = grid.store.getValue(grid.getItem(index), 'sort_order');

		return new dijit.form.NumberTextBox({
			style : "width:60%; font-size: 11px; height: 15px",
			value : sort_order,
			name: inode,
			maxLength: 15,
			type : "text",
			onChange : sortCat,
			onBlur: fixFocus
		});

	};

	function createStore(params) {
		if(params==null) params = '';

		myStore = new dojox.data.QueryReadStore({
			url : '/categoriesServlet'+params
		});

	}

	function createGrid() {
			var layout = [
			{
				field : 'category_name',
				name : '<%= LanguageUtil.get(pageContext, "Name") %>',
				width : '60%',
				formatter : formatHref
			}, {
				field : 'category_key',
				name : '<%= LanguageUtil.get(pageContext, "Key") %>',
				width : 'auto'
			}, {
				field : 'category_velocity_var_name',
				name : '<%= LanguageUtil.get(pageContext, "Variable") %>',
				width : 'auto'
			}, {
				field : 'sort_order',
				name : '<%= LanguageUtil.get(pageContext, "Sort-Order") %>',
				width : '6%',
				formatter : sortFormatter
			}  ];

			grid = new dojox.grid.EnhancedGrid({
				rowsPerPage : 25,
				jsId : "grid",
				store : myStore,
				autoWidth : true,
				initialWidth : '100%',
				autoHeight : true,
				escapeHTMLInData : false,
				structure : layout,
				plugins : {
					pagination : {
						pageSizes : [ "25", "50", "100", "All" ],
						description : "45%",
						sizeSwitch : "260px",
						pageStepper : "30em",
						gotoButton : true,
						maxPageStep : 7,
						position : "bottom"
					},
					search : true,
					indirectSelection: { headerSelector: true }
				}
			}, dojo.byId('catHolder'));

			dojo.addClass(dojo.byId("catHolder"), "tundra");

	};



	dojo.addOnLoad(function() {

		refreshCrumbs();

		// ajax loading of permissions tab when clicked
		var mainTabContainer = dijit.byId('mainTabContainer');
		dojo.connect(mainTabContainer, 'selectChild',
			function (evt) {
				selectedTab = mainTabContainer.selectedChildWidget;
				if (selectedTab.id == 'TabThree') {
					initPermission();
				}
			});

		createStore();
		createGrid();
		grid.startup();

		dojo.connect(dijit.byId("add_category_dialog"), "hide", function (evt) {
			dojo.byId("savedMessage").innerHTML = "";
		});

		dojo.connect(dijit.byId("catHolder_rowSelector_-1"), "onclick", function (evt) {
			var selectedItems = grid.selection.getSelected();
			var perPage = grid.rowsPerPage;
			var totalCats = grid.store._numRows;

			if(selectedItems.length>1) {

				 var html = '' +
	                '   <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "all")) %> ' + selectedItems.length + ' <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "categories-on-this-page-are-selected")) %>';
	                if (perPage < totalCats) {
	                    html += ' <a href="javascript: selectAllCategories()" style="text-decoration: underline;"> <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Select-all" )) %> ' + totalCats + ' <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "category-s" )) %>.</a>';
	                }
				dojo.byId("warningDiv").innerHTML = html;
			} else if(selectedItems.length==1){
				dojo.byId("warningDiv").innerHTML = '<br><br>';
			} else {
				dojo.byId("warningDiv").innerHTML = '<br><br>';
			}
		});

	});

	function selectAllCategories() {
		var totalCats = grid.store._numRows;
        var html = '' +
        '   <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "all" )) %> ' + totalCats + ' <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "categories-on-this-page-are-selected" )) %>' +
        '   <a href="javascript: clearAllCategoriesSelection()"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Clear-Selection" )) %>.</a>' +
        '';
        dojo.byId("warningDiv").innerHTML = html;
        document.getElementById("fullCommand").value = "true";
    }

	function clearAllCategoriesSelection() {
		grid.selection.clear();
		dojo.byId("warningDiv").innerHTML = '<br><br>';
	    document.getElementById("fullCommand").value = "false";
	}

	// search handling
	function doSearch(reorder, importing) {
		var params = dojo.byId("catFilter").value;
		params = "?inode="+currentInodeOrIdentifier+"&q="+params;
		if(reorder) {
			params = params + "&reorder=true";
		}

		grid.destroy(true);
		createStore(params);
		createGrid();
		grid.startup();

		if(!importing) {
			dojo.hash(encodeURIComponent(params));
		}
    };

    function doSearchHash(params)  {
    	grid.destroy(true);
		createStore(params);
		createGrid();
		grid.startup();
    }

	function showEditButtonsRow() {
		if( typeof changesMadeToPermissions!= "undefined"){
			if(changesMadeToPermissions == true){
				dijit.byId('applyPermissionsChangesDialog').show();
			}
		}
		dojo.style('editCategoryButtonRow', { display: '' });
		changesMadeToPermissions = false;
	}

	// clear the search field
	function clearCatFilter () {
		dijit.byId('catFilter').attr('value', '');
		doSearch();
	}

	// display addCategory Dialog
	function showAddCategory() {
		clearAddDialog();
		dijit.byId('add_category_dialog').show();
		dijit.byId('addCatName').focus();
	}

	function fillVelocityVarName() {
		var relation = dojo.byId("addCatName").value;
		var upperCase = false;
		var newString = "";
		for(var i=0;i < relation.length ; i++){
			var c = relation.charAt(i);
			if(upperCase){
				c=c.toUpperCase();
			}
			else{
				c=c.toLowerCase();
			}
			if(c == ' '){
				upperCase = true;
			}
			else{
				upperCase = false;
				newString+=c;
			}
		}
		var re = /[^a-zA-Z0-9]+/g;
		newString = newString.replace(re, "");

		dojo.byId("addCatVelVarName").value = newString;

		if(newString.length > 0){
			document.getElementById("VariableIdTitle").style.display = "";
		}
	}

	// replacement for the default js alert dialog
	function dialogAlert(txtContent) {
		var thisdialog = new dijit.Dialog({ title: "dotCMS", content: txtContent });
		dojo.body().appendChild(thisdialog.domNode);
		thisdialog.startup();
		thisdialog.show();
	}

	// replacement for the default js confirm dialog
	function confirmationDialog(configJson){
        var dialog = new dijit.Dialog({title:configJson.title,
            content:["<div style='width:25em' >",configJson.message,"</div>"].join('')});

        dialog.onButtonClickEvent = function(button){
            return function(){
                button.callBack.apply(this,[]);
                dialog.onCancel();
            }
        };
        for(actionButton in configJson.actionButtons){
            if(configJson.actionButtons.hasOwnProperty(actionButton)) {
                dojo.place(new dijit.form.Button({style: "position:relative; left:40%; margin-bottom:10px",label:configJson.actionButtons[actionButton].label,
                onClick:dialog.onButtonClickEvent.apply(dialog,[configJson.actionButtons[actionButton]])
                }).domNode, dialog.containerNode,'after');
            }
        }
        dialog.startup();
        dialog.show();
}

	function confirmDialog(msg, okFunction, cancelFunction) {
		confirmationDialog({
            title:"dotCMS",
            message:msg,
            actionButtons:[
                {label:'No',callBack: cancelFunction},
                {label:'Yes',callBack: okFunction}
            ]
        });
	}

	function clearAddDialog() {
		dojo.byId("addCatVelVarName").value = "";
		dojo.byId("addCatName").value = "";
		dojo.byId("addCatKey").value = "";
		dojo.byId("addCatKeywords").value = "";
	}


	var myCrumbs = new Array();



	function refreshCrumbs() {

		if(myCrumbs.length ==0){
			myCrumbs[0] = "0---------<%= LanguageUtil.get(pageContext, "Top-Level") %>";
		}

		//dojo.empty("ulNav");
		dojo.forEach(dojo.query("#ulNav li"), function(node, i) {
				if(i>1){
					dojo.destroy(node);
				}
			}
		);

		for(i=0;i<myCrumbs.length;i++){
			var inode = myCrumbs[i].split("---------")[0];
			var name = myCrumbs[i].split("---------")[1];

			if(i+1 == myCrumbs.length){
				dojo.place("<li  style=\"cursor:pointer\" i class=\"lastCrumb\" ><b>"+name+"</b></li>", "ulNav", "last");
			}
			else{
				dojo.place("<a id=\"a_"+inode+"\" href=\"javascript:prepareCrumbs('"+inode+"', '"+name+"');  \"><li  style=\"cursor:pointer\"  >"+name+"</li></a>", "ulNav", "last");
			}
		}




	}



	function prepareCrumbs(inode, name) {


		dijit.byId("mainTabContainer").selectChild(dijit.byId("TabOne"));
		if(inode =="0"){
			currentInodeOrIdentifier="";
		}
		else{
			currentInodeOrIdentifier=inode;
		}
		currentCatName = name;

		var newCrumbs = new Array();
		for(i=0;i<myCrumbs.length;i++){
			var ix = myCrumbs[i].split("---------")[0];
			var nx = myCrumbs[i].split("---------")[1];
			if(inode == ix){
				break;
			}
			newCrumbs[i] = myCrumbs[i];
		}
		newCrumbs[newCrumbs.length] = inode + "---------" + name;
		myCrumbs = newCrumbs;
		dijit.byId('catFilter').attr('value', '');

		doSearch();
		refreshCrumbs();
	}

	// drill down of a category, load the children, properties
	function drillDown(index) {

		var inode = grid.store.getValue(grid.getItem(index), 'inode');
		var name = grid.store.getValue(grid.getItem(index), 'category_name');
		var velVar = grid.store.getValue(grid.getItem(index), 'category_velocity_var_name');
		var key = grid.store.getValue(grid.getItem(index), 'category_key');
		var keywords = grid.store.getValue(grid.getItem(index), 'keywords');

		prepareCrumbs(inode, name);
		dojo.byId("propertiesNA").style.display = "none";
		dojo.byId("propertiesDiv").style.display = "block";
		dojo.byId("permissionNA").style.display = "none";
		dojo.byId("permissionDiv").style.display = "block";

		currentCatName = name;

		currentInodeOrIdentifier = inode;
		key = key=="null"?"":key;
		keywords = keywords=="null"?"":keywords;
		dojo.byId("CatVelVarName").value = velVar;
		dojo.byId("CatName").value = name;
		dojo.byId("CatKey").value = key;
		dojo.byId("CatKeywords").value = keywords;
		dijit.byId('catFilter').attr('value', '');
		doSearch();
	}

    // delete muliple or single category, via ajax
	function deleteCategories() {


		var items = grid.selection.getSelected();
		var full = dojo.byId("fullCommand").value;

    	if(full=="true") {
    		deleteFunction = function() {
    			var dia = dijit.byId('dotDeleteCategoriesDialog');
    			dia.show();
    			CategoryAjax.deleteCategories(currentInodeOrIdentifier, {
					callback:function(result) {
						if(result==0) {
							dojo.byId("warningDiv").innerHTML = '<br><br>';
						} else if(result==1) {
							dojo.byId("warningDiv").innerHTML = '<%= LanguageUtil.get(pageContext, "message.category.delete.failed.has.dependencies") %>';
							var t = new dojox.timing.Timer();
							t.setInterval(5000);
							t.onTick = function() {
								t.stop();
								dojo.byId("warningDiv").innerHTML = '<br><br>';
					        }
					        t.start();
						}

						dia.hide();
						doSearch();
						grid.selection.clear();
					}
    			});
    		}

    	} else {
    		deleteFunction = function() {
    			var dia = dijit.byId('dotDeleteCategoriesDialog');
    			dia.show();

    			var inodes = new Array();
    			dojo.forEach(items, function(selectedItem, index) {
    		       	inodes[index] = selectedItem.i.inode;
    		    });

    			CategoryAjax.deleteSelectedCategories(inodes, {
					callback:function(result) {
						if(result==0) {
							dojo.byId("warningDiv").innerHTML = '<br><br>';
						} else if(result==1) { // has dependencies
							dojo.byId("warningDiv").innerHTML = '<%= LanguageUtil.get(pageContext, "message.category.delete.failed.has.dependencies") %>';
							var t = new dojox.timing.Timer();
							t.setInterval(5000);
							t.onTick = function() {
								dojo.byId("warningDiv").innerHTML = '<br><br>';
								t.stop();
					        }
					        t.start();
						} else if (result==2) {
							dojo.byId("warningDiv").innerHTML = '<br><br>';
						}

						grid.selection.clear();
		    			doSearch();
		    			dia.hide();
					}
				});



    		};

    	}

		if (items.length) {
			confirmDialog('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.category.delete.categories")) %>',
					deleteFunction, function() {return false;});
		}
	}

    // add a new category via ajax
    // boolean save, indicates if saving or updating
	function saveOrUpdateCategory(save) {
		var prefix = save?"add":"";
		var name = dojo.byId(prefix+"CatName").value;
		var formName = save?'addCatPropertiesForm':'updateCatPropertiesForm';
		dojo.byId("savedMessage").innerHTML = "";

		if(!dijit.byId(formName).validate()) {
			return;
		}

		var velVar = dojo.byId(prefix+"CatVelVarName").value;
		var key = dojo.byId(prefix+"CatKey").value;
		var keywords = dojo.byId(prefix+"CatKeywords").value;
		CategoryAjax.saveOrUpdateCategory(save, currentInodeOrIdentifier, name, velVar, key, keywords, {
			callback:function(result) {
				doSearch();
				grid.selection.clear();

				var message = "";

				switch (result) {
				case 0:
					message = '<%= LanguageUtil.get(pageContext, "message.category.add") %>';
					clearAddDialog();
					if(!save) {
						message = '<%= LanguageUtil.get(pageContext, "message.category.update") %>';
						showDotCMSSystemMessage(message);
					}
					break;
				case 1:
					message = '<%= LanguageUtil.get(pageContext, "message.category.permission.error") %>';
					clearAddDialog();
					break;
				case 2:
					message = '<%= LanguageUtil.get(pageContext, "error.category.folder.taken") %>';
					break;
				default:
				}

				dojo.byId("savedMessage").innerHTML = message;
			}
		});
	}

    function exportCategories() {
		var filter = dojo.byId("catFilter").value;
		var downloadPdfIframeName = "downloadPdfIframe";
		var iframe = dojo.io.iframe.create(downloadPdfIframeName);
		dojo.io.iframe.setSrc(iframe, "/categoriesServlet?inode="+currentInodeOrIdentifier+"&q="+filter+"&action=export", true);
    }

    function importCategories() {
		var dia = dijit.byId('dotImportCategoriesDialog');
		dia.show();

		var file = dwr.util.getValue('uploadFile');
		var filter = dojo.byId("catFilter").value;
		var merge = dojo.byId("radioTwo");

		var exportType = "replace";
		if(merge.checked) {
		 exportType = "merge";
		}

		CategoryAjax.importCategories(currentInodeOrIdentifier, filter, file, exportType, function(result) {

			if(result==0) {
				dojo.byId("warningDiv").innerHTML = '<br><br>';
			}  else if (result==1) {
				dojo.byId("warningDiv").innerHTML = '<%= LanguageUtil.get(pageContext, "message.category.delete.failed.has.dependencies") %>';
				var t = new dojox.timing.Timer();
				t.setInterval(5000);
				t.onTick = function() {
					dojo.byId("warningDiv").innerHTML = '<br><br>';
					t.stop();
		        }
		        t.start();
			}

			doSearch(false, true);
			grid.selection.clear();
			dia.hide();
		});
   	}


	function initPermission() {
		var nameField = dojo.byId("permCatName");
		nameField.innerHTML = currentCatName;

		PermissionAjax.getAsset(currentInodeOrIdentifier, {
			callback:function(asset) {
				assetId = asset.id;
				assetType =asset.type;
				isParentPermissionable = asset.isParentPermissionable;
				isFolder = asset.isFolder;
				isHost = asset.isHost;
				doesUserHavePermissionsToEdit = asset.doesUserHavePermissionsToEdit;
				isNewAsset = assetId == 0 || assetId == '' || !assetId;

				if(isFolder){
					contentTemplateString = dojo._getText('/html/portlet/ext/common/edit_permissions_accordion_folder_entry.html');
				}
				else if(isHost){
					contentTemplateString = dojo._getText('/html/portlet/ext/common/edit_permissions_accordion_entry.html');
				}
				else{
					contentTemplateString = dojo._getText('/html/portlet/ext/common/edit_permissions_accordion_empty_entry.html');
				}

				dojo.style(dijit.byId('savingPermissionsDialog').closeButtonNode, 'visibility', 'hidden');
				loadPermissions();
			}
		});
	}

	function alterFocus(toBlur, toFocus) {
		if(toBlur.id!="addCatName" &&
				toBlur.id!="addCatKey" &&
				toBlur.id!="addCatKeywords" &&
				toBlur.id.indexOf("dijit_form_NumberTextBox")) {
			toBlur.blur();
			toFocus.focus();
		}
	}

</script>

<div class="buttonBoxRight">
		<div style="padding-right: 90px">
		<%= LanguageUtil.get(pageContext, "Import-Options") %>:
		 <input type="radio" dojoType="dijit.form.RadioButton" name="importMode" id="radioOne" checked />
	    <label for="radioOne">
	        <%= LanguageUtil.get(pageContext, "Replace") %>
	    </label>
	    <input type="radio" dojoType="dijit.form.RadioButton" name="importMode" id="radioTwo" />
	    <label for="radioTwo">
	        <%= LanguageUtil.get(pageContext, "add-edit") %>
	    </label>
    	</div>
		<input type="file" id="uploadFile" onchange="importCategories()" class="realfile"  />
		<div class=fakefile>
			<button dojoType="dijit.form.Button" type="button" iconClass="uploadIcon" ><%= LanguageUtil.get(pageContext, "import") %></button>
		</div>
</div>
<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp" >
	<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext,\"view-categories\") %>" />
		<div id="mainTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false" style="width: 99%; margin-left: auto; margin-right:auto;"  >
			<!-- START Children Tab -->
			<div id="TabOne" dojoType="dijit.layout.ContentPane"  title="<%=LanguageUtil.get(pageContext, "children")%>" style="overflow: hidden; ">
				<div class="buttonBoxLeftNoTop">
					<input  name="catFilter" id="catFilter" onkeyup="doSearch();" dojoType="dijit.form.TextBox" onblur="alterFocus(document.activeElement, this);" placeholder="<%= LanguageUtil.get(pageContext, "message.filter.categories") %>"  style="width: 250px; height: 15px" />
					<button dojoType="dijit.form.Button" onclick="clearCatFilter()" type="button" iconClass="resetIcon"><%= LanguageUtil.get(pageContext, "Clear") %></button>
				</div>
				<div id="warningDiv" style="text-align: center; height: 10px; margin-top: 15px; color: red; font-size: 11px; ">
					<br><br>
				</div>
				<div class=buttonBoxRightNoTop>
					<button dojoType="dijit.form.Button" type="button" onclick="showAddCategory(); " iconClass="plusIcon"><%= LanguageUtil.get(pageContext, "Add") %></button>
					<button dojoType="dijit.form.Button" type="button" onclick="deleteCategories()" iconClass="deleteIcon"><%= LanguageUtil.get(pageContext, "delete") %></button>
					<button dojoType="dijit.form.Button" type="button" onclick="exportCategories()" iconClass="downloadIcon"><%= LanguageUtil.get(pageContext, "export") %></button>
					<button dojoType="dijit.form.Button" type="button" onClick="doSearch(true);" iconClass="resetIcon"><%= LanguageUtil.get(pageContext,"Reorder") %></button>
				</div>
				<br/>
				<div id="catHolder" style="text-align: center; " class="claro"></div>
				<div style="height: 15px; text-align: right; margin-top: 5px">
					<button dojoType="dijit.form.Button" type="button" onClick="doSearch(true);" iconClass="resetIcon"><%= LanguageUtil.get(pageContext,"Reorder") %></button>
				</div>
				<input type="hidden" name="fullCommand" id="fullCommand" value="">
			</div>
			<!-- END Children Tab -->

			<!-- START Properties Tab -->
			<div id="TabTwo" dojoType="dijit.layout.ContentPane"  title="<%=LanguageUtil.get(pageContext, "properties")%>""  >
				<div id="propertiesNA" style="height: 300px; text-align: center;  position:relative">
					<span style="position:absolute; top:50%; left: 50%; height:10em; margin-top:-2em; margin-left:-10em"><%= LanguageUtil.get(pageContext, "message.category.toplevel.na") %></span>
				</div>
				<div id="propertiesDiv" style="display: none">
					<form id="updateCatPropertiesForm" dojoType="dijit.form.Form">
						<dl >
						    <dt>
						    	<%= LanguageUtil.get(pageContext, "Variable-ID") %>:
							</dt>
							<dd style="clear: none;">
								<input type="hidden" id=""/>
								<input type="text" dojoType="dijit.form.TextBox" id="CatVelVarName" readonly="readonly" name="categoryVarName" maxlength="50" size="30"  value="" />
							</dd>
							<dt>
								<%= LanguageUtil.get(pageContext, "category-name") %>:
							</dt>
							<dd>
								<input type="text" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" id="CatName" name="categoryName" maxlength="50" size="30"  value="" />
							</dd>
			     			<dt>
								<%= LanguageUtil.get(pageContext, "category-unique-key") %>:
							</dt>
							<dd>
								<input type="text" dojoType="dijit.form.TextBox" id="CatKey" name="key" size="30" maxlength="255" value="" />
							</dd>

							<dt>
								<%= LanguageUtil.get(pageContext, "keywords") %>:
							</dt>
							<dd>
								<textarea dojoType="dijit.form.Textarea" id="CatKeywords" name="keywords" style="width:250px; min-height:40px;"></textarea>
							</dd>
						</dl>
					</form>
					<div class="buttonRow" id="editCategoryButtonRow">

					   <button dojoType="dijit.form.Button" onclick="saveOrUpdateCategory(false);" iconClass="saveIcon" type="button">
					      <%= LanguageUtil.get(pageContext, "save") %>
					   </button>
					</div>
				</div>
			</div>

			<!-- END Properties Tab -->
			<!-- START Permission Tab -->
			<div id="TabThree" dojoType="dijit.layout.ContentPane" title="<%=LanguageUtil.get(pageContext, "permissions")%>" >
				<div id="permissionNA" style="height: 300px; text-align: center;  position:relative">
					<span style="position:absolute; top:50%; left: 50%; height:10em; margin-top:-2em; margin-left:-10em"><%= LanguageUtil.get(pageContext, "message.category.toplevel.na") %></span>
				</div>
				<div id="permissionDiv" style="display: none">
					<%@ include file="/html/portlet/ext/common/edit_permissions_tab_inc_ajax.jsp" %>
				</div>
			<!-- END Permission Tab -->
			</div>
		</div>



</liferay:box>

<!-- START Add Category pop up -->
<div id="add_category_dialog"  dojoType="dijit.Dialog" style="display:none;height:290px;width:450px;vertical-align: middle;" draggable="true"
	title="<%= LanguageUtil.get(pageContext, "add-category") %>" >
	<div style="overflow-y:auto;" dojoType="dijit.layout.ContentPane">
		<div style="padding:0 0 10px 0; border-bottom:1px solid #ccc;">
			<form id="addCatPropertiesForm" dojoType="dijit.form.Form">
				<dl>
					<dd style="margin-bottom: -2px; margin-top: -8px">
			       		<span id="savedMessage" style="color:red; font-size:11px; font-family: verdana; " >
						</span>
					</dd>
					<dt>
			       		<span id="VariableIdTitle" >
				    	<%= LanguageUtil.get(pageContext, "Variable-ID") %>:
						</span>
					</dt>
					<dd style="clear: none;">
						<input id="addCatVelVarName" readonly="true" style="width:250px;border:0;"  />
					</dd>
					<dt><%= LanguageUtil.get(pageContext, "Name") %>:</dt>
					<dd><input id="addCatName" type="text" tabindex="1" required="true" onblur="fillVelocityVarName(); " invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" /></dd>
					<dt><%= LanguageUtil.get(pageContext, "Key") %>:</dt>
					<dd><input id="addCatKey" type="text" dojoType="dijit.form.TextBox" /></dd>
					<dt>
						<%= LanguageUtil.get(pageContext, "keywords") %>:
					</dt>
					<dd>
						<textarea id="addCatKeywords" dojoType="dijit.form.Textarea"  style="width:250px; min-height:40px;"></textarea>
					</dd>
				</dl>
			</form>
		</div>
		<div class="clear"></div>
		<div class="buttonRow">
			<button dojoType="dijit.form.Button" onclick="saveOrUpdateCategory(true)" type="button" iconClass="saveIcon"><%= LanguageUtil.get(pageContext, "Save") %></button>
			<button dojoType="dijit.form.Button"  onclick="dijit.byId('add_category_dialog').hide()" type="button" iconClass="cancelIcon"><%= LanguageUtil.get(pageContext, "Cancel") %></button>
   		</div>
   	</div>
</div>

<!-- END Add Category pop up -->

<div id="dotImportCategoriesDialog" dojoType="dijit.Dialog" style="display:none" title="<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Processing-Please-be-patient")) %>">
	<div dojoType="dijit.layout.ContentPane" style="width:300px;height:80px;text-align: center;vertical-align: middle;padding:20px;" class="box" hasShadow="true">
		<div style="width:300px"  indeterminate="true" id="indeterminateBar1"
			dojoType="dijit.ProgressBar"></div>
			<div style="padding:5px;text-align: center;">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.category.import")) %>
			</div>
	</div>
</div>
<div id="dotDeleteCategoriesDialog" dojoType="dijit.Dialog" style="display:none" title="<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Processing-Please-be-patient")) %>">
	<div dojoType="dijit.layout.ContentPane" style="width:300px;height:80px;text-align: center;vertical-align: middle;padding:20px;" class="box" hasShadow="true">
		<div style="width:300px"  indeterminate="true" id="indeterminateBar2"
			dojoType="dijit.ProgressBar"></div>
			<div style="padding:5px;text-align: center;">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.category.deleting")) %>
			</div>
	</div>
</div>