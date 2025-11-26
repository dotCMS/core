<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@ page import="com.dotcms.publisher.endpoint.bean.PublishingEndPoint" %>
<%@ page import="com.dotcms.publisher.endpoint.business.PublishingEndPointAPI" %>
<%@ page import="com.dotcms.enterprise.LicenseUtil" %>
<%@page import="com.dotcms.enterprise.license.LicenseLevel"%>
<%@include file="/html/portlet/ext/categories/init.jsp"%>
<%@ include file="/html/portlet/ext/remotepublish/init.jsp" %>
<%@ page import="com.dotmarketing.business.PermissionAPI.PermissionableType" %>

<%
    final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
	boolean enterprise = LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level;

	PublishingEndPointAPI pepAPI = APILocator.getPublisherEndPointAPI();
	List<PublishingEndPoint> sendingEndpointsList = pepAPI.getReceivingEndPoints();
    boolean sendingEndpoints = UtilMethods.isSet(sendingEndpointsList) && !sendingEndpointsList.isEmpty();

	boolean hasViewPermision = permissionAPI.doesUserHavePermissions(APILocator.systemHost().getIdentifier(),
			PermissionableType.CATEGORY,
			PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user);
%>

<%  String dojoPath = Config.getStringProperty("path.to.dojo"); %>

<style type="text/css">
	@import "<%=dojoPath%>/dojox/grid/enhanced/resources/claro/EnhancedGrid.css?b=<%= ReleaseInfo.getVersion() %>";
	div.fakefile {
		position: absolute;
		text-align: left;
		z-index: 1;
		margin-left: 20px;
		width: 9px;
		margin-top: -22px;
		-moz-opacity: 0;
		right: 80px;
		top: 77%;
	}

	.fakefile input.upload {
		position: absolute;
		top: 0;
		right: 5px;
		left: 1px;
		padding: 0;
		font-size: 15px;
		cursor: pointer;
		width: 90px;
		filter: alpha(opacity: 0);
		opacity: 0;
		-moz-opacity: 0;
		z-index:4;
	}

    #ulNav #selectHostDiv + li {
        display: none;
    }

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
    dojo.require("dotcms.dojo.push.PushHandler");
    dojo.require("dijit.form.ValidationTextBox");
    dojo.require("dojo.parser");


    var pushHandler = new dotcms.dojo.push.PushHandler('<%=LanguageUtil.get(pageContext, "Remote-Syncronization")%>');

    dojo.connect(dojo.global, "onhashchange", refresh);

    var backOrForward = false;
    var actions = "";

    function refresh() {
        var hashReceived = dojo.hash();
        var inode = "0";
        var name = "<%= LanguageUtil.get(pageContext, "Top-Level") %>";
        var hashToSend = null;

        if(typeof hashReceived != "undefined" && hashReceived != '') {
            var query = hashReceived.substring(hashReceived.indexOf("?") + 1, hashReceived.length);
            var queryObject = dojo.queryToObject(query);
            if (queryObject.name != 'undefined') {
                inode = queryObject.inode == '' ? 0 : queryObject.inode;
                name = queryObject.name;
            }
            hashToSend = hashReceived;
        }

        if (actions == "") {
            // browser back or forward pressed
			backOrForward = true;
        }

        buildCrumbs(inode, name);
		doSearchHash(hashToSend);
		refreshCrumbs();

        actions = "";
        backOrForward = false;
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

    var sortSelectable = function() {
        var toSel = document.activeElement;
        document.getElementById(toSel.id).setAttribute("unselectable", "off");
    };

    var fixFocus = function() {
        var toBlur = document.activeElement;
        if(toBlur.id!="addCatName" &&
            toBlur.id!="addCatKey" &&
            toBlur.id!="addCatKeywords" &&
            toBlur.id.indexOf("dijit_form_ValidationTextBox")) {
            toBlur.blur();
            this.focus();
        }
    };

    // format the sort column of the grid to be a NumberTextBox
    var sortFormatter = function(value, index) {
        var inode = grid.store.getValue(grid.getItem(index), 'inode');
        var sort_order = grid.store.getValue(grid.getItem(index), 'sort_order');

        return new dijit.form.ValidationTextBox({
            style : "width: 80%; margin: 0 10%",
            value : sort_order,
            name: inode,
            maxLength: 15,
            regExpGen:function(){ return "\\d+" },
            invalidMessage:"Please enter numbers only",
            type : "text",
            onChange : sortCat,
            onClick : sortSelectable,
            onBlur: fixFocus
        });

    };

    function createStore(params) {
        if (params == null) params = '';

        myStore = new dojox.data.QueryReadStore({
            url: '/categoriesServlet' + params
        });
    }

    function convertStringToUnicode(name) {
        var unicodeString = '';
        for (var i = 0; i < name.length; i++) {
            if (name.charCodeAt(i) > 128) {
                var str = name.charCodeAt(i).toString(16).toUpperCase();
                while (str.length < 4)
                    str = "0" + str;
                unicodeString += "\\u" + str;
            } else {
                unicodeString += name[i];
            }
        }
        return unicodeString;
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
                width : '15%'
            }, {
                field : 'category_velocity_var_name',
                name : '<%= LanguageUtil.get(pageContext, "Variable") %>',
                width : '15%'
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
            onClick: function(event) {
                if (event.dispatch !== 'doclick') {
                    grid.selection.clear();
                }
            },
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

        dojo.query("#catHolder").addClass('view-categories__categories-list');
    }

    function onSelectedCategoryRow(event) {
        var selectedItems = grid.selection.selected.filter(item => item).length;
        var perPage = grid.rowsPerPage;
        var totalCats = grid.store._numRows;
        document.getElementById("fullCommand").value = "false";

        if(selectedItems === perPage) {
            var html = '' +
                '   <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "all")) %> ' + selectedItems + ' <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "categories-on-this-page-are-selected")) %>';
            if (perPage < totalCats) {
                html += ' <a href="javascript: selectAllCategories()" style="text-decoration: underline;"> <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Select-all" )) %> ' + totalCats + ' <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "category-s" )) %>.</a>';
            }
            dojo.byId("warningDiv").innerHTML = html;
        } else {
            dojo.byId("warningDiv").innerHTML = '';
        }
    }

    function bindGridEvents() {
        dojo.connect(grid.selection, 'onSelected', onSelectedCategoryRow)
        dojo.connect(grid.selection, 'onDeselected', onSelectedCategoryRow)

        // when Select All checkbox is changed
        dojo.connect(grid.rowSelectCell, 'toggleAllSelection', onSelectedCategoryRow)
    }


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
        bindGridEvents();

        dojo.connect(dijit.byId("add_category_dialog"), "hide", function (evt) {
            dojo.byId("savedMessage").innerHTML = "";
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
        dojo.byId("warningDiv").innerHTML = '';
        document.getElementById("fullCommand").value = "false";
    }

    // search handling
    function doSearch(reorder, importing) {
        var params = dojo.byId("catFilter").value.trim();
        params = "?donothing&inode="+currentInodeOrIdentifier+"&name="+encodeURIComponent(currentCatName)+"&q="+encodeURIComponent(params);
        if(reorder) {
            params = params + "&reorder=true";
        }

        grid.destroy(true);
        createStore(params);
        createGrid();
        grid.startup();
        bindGridEvents();

        if(!importing) {
            dojo.hash(params);
        }
    }

    function doSearchHash(params)  {
        grid.destroy(true);
        createStore(params);
        createGrid();
        grid.startup();
        bindGridEvents();
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

    // display importCategory Dialog
    function showImportCategories() {
        dijit.byId('importCategoriesOptions').show();
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
                dojo.place(new dijit.form.Button({style: "position:relative; left:33%; margin:0 4px 10px",label:configJson.actionButtons[actionButton].label,
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
        dojo.byId("savedMessage").innerHTML = "";
    }

    var previousCrumbs = new Array();
    var myCrumbs = new Array();

    function refreshCrumbs() {
        var crumbsArray = new Array();

        if (backOrForward) {
            crumbsArray = previousCrumbs;
        } else {
            crumbsArray = myCrumbs;
        }

        if (crumbsArray.length ==0) {
            crumbsArray[0] = "0---------<%= LanguageUtil.get(pageContext, "Top-Level") %>";
        }

        //dojo.empty("ulNav");
        dojo.forEach(dojo.query("#ulNav li"), function(node, i) {
                if(i>1){
                    dojo.destroy(node);
                }
            }
        );

        for (i = 0; i < crumbsArray.length; i++) {
            var inode = crumbsArray[i].split("---------")[0];
            var name = crumbsArray[i].split("---------")[1];

            if (i + 1 == crumbsArray.length) {
                dojo.place("<li  style=\"cursor:pointer\" i class=\"lastCrumb\" ><b>"+name+"</b></li>", "ulNav", "last");
            }
            else{
                dojo.place("<a id=\"a_"+inode+"\" href=\"javascript:rollUp('"+inode+"', '"+name+"');  \"><li  style=\"cursor:pointer\"  >"+name+"</li></a>", "ulNav", "last");
            }
        }
    }

    function prepareCrumbs(inode, name) {
        buildCrumbs(inode, name);
        doSearch();
        refreshCrumbs();
    }

    function buildCrumbs(inode, name) {
        dijit.byId("mainTabContainer").selectChild(dijit.byId("TabOne"));

        if(inode =="0"){
            currentInodeOrIdentifier="";
        }
        else{
            currentInodeOrIdentifier=inode;
        }
        currentCatName = name;

		var newCrumbs = new Array();
		var crumbsArray = new Array();

		if (backOrForward) {
		    crumbsArray = previousCrumbs;
		} else {
		    crumbsArray = myCrumbs;
		}

        for (i = 0; i < crumbsArray.length; i++) {
            var ix = crumbsArray[i].split("---------")[0];
            var nx = crumbsArray[i].split("---------")[1];
            if(inode == ix) {
                break;
            }
            newCrumbs[i] = crumbsArray[i];
        }

        newCrumbs[newCrumbs.length] = inode + "---------" + name;

        if (backOrForward) {
            previousCrumbs = newCrumbs;
            myCrumbs = previousCrumbs;
		} else {
            myCrumbs = newCrumbs;
		}

        dijit.byId('catFilter').attr('value', '');
    }

    // drill down of a category, load the children, properties
    function drillDown(index) {
        previousCrumbs = myCrumbs;
        actions = "breadcrums";

        var inode = grid.store.getValue(grid.getItem(index), 'inode');
        var name = grid.store.getValue(grid.getItem(index), 'category_name');
        var velVar = grid.store.getValue(grid.getItem(index), 'category_velocity_var_name');
        var key = grid.store.getValue(grid.getItem(index), 'category_key');
        var keywords = grid.store.getValue(grid.getItem(index), 'keywords');
        var permissionsTab = dojo.byId("TabThree");

        prepareCrumbs(inode, name);
        dojo.byId("propertiesNA").style.display = "none";
        dojo.byId("propertiesDiv").style.display = "block";

        if(permissionsTab) {
			dojo.byId("permissionNA").style.display = "none";
			dojo.byId("permissionDiv").style.display = "block";
		}

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

    // roll up of a category, load the children, properties
    function rollUp(inode, name) {
        previousCrumbs = myCrumbs;
        actions = "breadcrums";

        prepareCrumbs(inode, name);
		var permissionsTab = dojo.byId("TabThree");

        if(currentInodeOrIdentifier == "" || currentInodeOrIdentifier == "0"){
            dojo.byId("propertiesNA").style.display = "block";
            dojo.byId("propertiesDiv").style.display = "none";
            if(permissionsTab) {
				dojo.byId("permissionNA").style.display = "block";
				dojo.byId("permissionDiv").style.display = "none";
			}
        }else{
            dojo.byId("propertiesNA").style.display = "none";
            dojo.byId("propertiesDiv").style.display = "block";
			if(permissionsTab) {
				dojo.byId("permissionNA").style.display = "none";
				dojo.byId("permissionDiv").style.display = "block";
			}
        }

        CategoryAjax.getCategoryMap(inode,getCategoryMapCallback);
    }

    function getCategoryMapCallback(categoryMap){
        var inode = categoryMap['inode'];
        var name = categoryMap['category_name'];
        var velVar = categoryMap['category_velocity_var_name'];
        var key = categoryMap['category_key'];
        var keywords = categoryMap['keywords'];

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
        var allItemsOnFirstPageSelected = grid.selection.selected.filter(item => item).length === grid.rowsPerPage;

        if(full=="true" && allItemsOnFirstPageSelected) {
            deleteFunction = function() {
                var dia = dijit.byId('dotDeleteCategoriesDialog');
                dia.show();

                CategoryAjax.deleteCategories(currentInodeOrIdentifier, {
                    callback:function(result) {
                        if(result==0) {
                            dojo.byId("warningDiv").innerHTML = '';
                        } else if(result==1) {
                            dojo.byId("warningDiv").innerHTML = '<%= LanguageUtil.get(pageContext, "message.category.delete.failed.has.dependencies") %>';
                            var t = new dojox.timing.Timer();
                            t.setInterval(5000);
                            t.onTick = function() {
                                t.stop();
                                dojo.byId("warningDiv").innerHTML = '';
                            };
                            t.start();
                        }

                        dia.hide();
                        doSearch(false, true);
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
                            dojo.byId("warningDiv").innerHTML = '';
                        } else if(result==1) { // has dependencies
                            dojo.byId("warningDiv").innerHTML = '<%= LanguageUtil.get(pageContext, "message.category.delete.failed.has.dependencies") %>';
                            var t = new dojox.timing.Timer();
                            t.setInterval(5000);
                            t.onTick = function() {
                                dojo.byId("warningDiv").innerHTML = '';
                                t.stop();
                            }
                            t.start();
                        } else if (result==2) {
                            dojo.byId("warningDiv").innerHTML = '';
                        }

                        grid.selection.clear();
                        doSearch(false, true);
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
                doSearch(false, true);
                grid.selection.clear();

                var message = "";
                var messageStyle = "color:green; font-size:11px;";

                switch (result) {
                    case 0:
                        message = `<%= LanguageUtil.get(pageContext, "message.category.add") %>`;
                        clearAddDialog();
                        if(!save) {
                            message = `<%= LanguageUtil.get(pageContext, "message.category.update") %>`;
                            showDotCMSSystemMessage(message);
                        }
                        break;
                    case 1:
                        message = `<%= LanguageUtil.get(pageContext, "message.category.permission.error") %>`;
                        messageStyle = "color:red; font-size:11px;";
                        error = true;
						if(!save) {
							showDotCMSSystemMessage(message);
						}
                        break;
                    case 2:
                        message = `<%= LanguageUtil.get(pageContext, "error.category.folder.taken") %>`;
                        messageStyle = "color:red; font-size:11px;";
                        break;
                    default:
                }

                dojo.byId("savedMessage").innerHTML = message;
                dojo.byId("savedMessage").style = messageStyle;
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
        var file = dwr.util.getValue('uploadFile');
        var filter = dojo.byId("catFilter").value;
        var merge = dojo.byId("radioTwo");
        var exportType = "replace";

        if(merge.checked) {
            exportType = "merge";
        }

        CategoryAjax.importCategories(currentInodeOrIdentifier, filter, file, exportType, function(result) {
            var dia = dijit.byId('importCategoriesOptions');

            if(result==0) {
                dojo.byId("warningDiv").innerHTML = '';
            }  else if (result==1) {
                dojo.byId("warningDiv").innerHTML = `<%= LanguageUtil.get(pageContext, "message.category.delete.failed.has.dependencies") %>`;
                var t = new dojox.timing.Timer();
                t.setInterval(5000);
                t.onTick = function() {
                    dojo.byId("warningDiv").innerHTML = '';
                    t.stop();
                }
                t.start();
            }

            dia.hide();
            doSearch(false, true);
            grid.selection.clear();
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
            toBlur.id!="" &&
            toBlur.id.indexOf("dijit_form_NumberTextBox")) {
            toBlur.blur();
            toFocus.focus();
        }
    }

    function remoteSyncronization () {
        pushHandler.showCategoryDialog();
    }

    function addToBundle () {
        pushHandler.showAddToBundleDialog('CAT', '<%=LanguageUtil.get(pageContext, "Add-To-Bundle")%>');
    }

</script>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp" >
	<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext,\"view-categories\") %>" />

	<div class="portlet-main view-categories">

		<!-- START Actions -->
		<div id="oneandtwo" data-dojo-type="dijit/form/DropDownButton" data-dojo-props='iconClass:"fa-plus", class:"dijitDropDownActionButton"'>
			<span></span>

			<div data-dojo-type="dijit/Menu" class="contentlet-menu-actions" id="oneandtwothree">
				<div data-dojo-type="dijit/MenuItem" data-dojo-props="onClick: showAddCategory">
					<%= LanguageUtil.get(pageContext, "Add") %>
				</div>
				<div data-dojo-type="dijit/MenuItem" data-dojo-props="onClick: deleteCategories">
					<%= LanguageUtil.get(pageContext, "delete") %>
				</div>
				<div data-dojo-type="dijit/MenuItem" data-dojo-props="onClick: showImportCategories">
					<%= LanguageUtil.get(pageContext, "import") %>
				</div>
				<div data-dojo-type="dijit/MenuItem" data-dojo-props="onClick: exportCategories">
					<%= LanguageUtil.get(pageContext, "export") %>
				</div>
				<% if ( enterprise ) { %>
				<% if ( sendingEndpoints ) { %>
				<div data-dojo-type="dijit/MenuItem" onClick="remoteSyncronization();">
					<%= LanguageUtil.get(pageContext,"Remote-Syncronization") %>
				</div>
				<%}%>
				<div data-dojo-type="dijit/MenuItem" onClick="addToBundle();">
					<%= LanguageUtil.get(pageContext,"Add-All-Categories-To-Bundle") %>
				</div>
				<%}%>
			</div>
		</div>
		<!-- END Actions -->

		<!--  START TABS -->
		<div id="mainTabContainer" dolayout="false" dojoType="dijit.layout.TabContainer">

			<!-- START Children Tab -->
			<div id="TabOne" dojoType="dijit.layout.ContentPane"  title="<%=LanguageUtil.get(pageContext, "children")%>">
				<div class="portlet-toolbar">
					<div class="portlet-toolbar__actions-primary">
						<%@ include file="/html/portlet/ext/common/sub_nav_inc.jsp" %>

						<!-- ++++++++++++++++++++++++++++  -->
						<!-- +++++ Start breadcrumps ++++  -->
						<%
							if (0 < crumbTrailEntries.size()) {
								boolean _amITheFirst = true;
						%>

						<div class="portlet-toolbar">
							<div class="subNavCrumbTrail" id="subNavCrumbTrail">
								<ul id="ulNav">
									<% if (!showHostSelector) {  _amITheFirst = false; %>
									<li id="selectHostDiv"
											<%if(UtilMethods.isSet(_browserCrumbUrl)){ %>
										onclick="window.location='<%=_browserCrumbUrl%>';"
											<%} %>
									>
										<span class="hostStoppedIcon" style="float:left;margin-right:5px;"></span>
										<%=LanguageUtil.get(pageContext, "Global-Page")%>
									</li>
									<% } %>

									<% for (CrumbTrailEntry crumbTrailEntry : crumbTrailEntries) {
										if (UtilMethods.isSet(crumbTrailEntry.getLink())) { %>
									<li style="cursor: pointer"
											<%if(_amITheFirst){%> id="selectHostDiv"<%} %>
									>
										<% if (_amITheFirst) { %>
										<span class="publishIcon"></span>
										<% }
											_amITheFirst = false;
										%>
										<a href="
											<%= crumbTrailEntry.getLink() %>"
										>
											<%=crumbTrailEntry.getTitle()%>
										</a>
									</li>
									<%
									} else {
									%>
									<li class="lastCrumb" id="lastCrumb"><span><%=crumbTrailEntry.getTitle()%></span></li>
									<%
										}
									%>
									<%
										}
									%>
								</ul>
								<%
									if (showHostSelector) {
								%>
								<div class="changeHost" onclick="dijit.popup.open({popup: myDialog, around: dojo.byId('changeHostId')})">
									<span id="changeHostId"><%=LanguageUtil.get(pageContext, "Change-Host")%></span>
									<span class="chevronExpandIcon"></span>
								</div>
								<%
									}
								%>
								<div class="clear"></div>

							</div>
						</div>

						<%
							}
						%>

						<script type="text/javascript">
                            function showHostPreview() {
                                window.location = '<%=_browserCrumbUrl%>';
                            }
                            function updateCMSSelectedHosts() {
                                if( dijit.byId('subNavHost').attr('value')!=null && dijit.byId('subNavHost').attr('value')!=''){
                                    window.location.href = "/html/portlet/ext/common/sub_nav_refresh_host.jsp?referer=" + escape(window.location) + "&host_id=" + dijit.byId('subNavHost').attr('value');
                                }
                            }
						</script>

						<!-- ++++++ End breadcrumps +++++  -->
						<!-- ++++++++++++++++++++++++++++  -->
					</div>

					<div class="portlet-toolbar__info">
						<div id="warningDiv" style="color: red;"></div>
					</div>
					<div class="portlet-toolbar__actions-secondary">
						<div class="inline-form">
							<input  name="catFilter" id="catFilter" onkeyup="doSearch(false, true);" type="text" dojoType="dijit.form.TextBox" placeholder="<%= LanguageUtil.get(pageContext, "message.filter.categories") %>">
							<button dojoType="dijit.form.Button" onclick="clearCatFilter()" type="button"><%= LanguageUtil.get(pageContext, "Clear") %></button>
						</div>
					</div>
				</div>

				<div id="catHolder" style="text-align: center; " class="claro"></div>

				<div style="text-align: right; margin: 16px 0">
					<button dojoType="dijit.form.Button" type="button" onClick="doSearch(true);" iconClass="resetIcon"><%= LanguageUtil.get(pageContext,"Reorder") %></button>
				</div>
				<input type="hidden" name="fullCommand" id="fullCommand" value="">
			</div>
			<!-- END Children Tab -->

			<!-- START Properties Tab -->
			<div id="TabTwo" dojoType="dijit.layout.ContentPane" title="<%=LanguageUtil.get(pageContext, "properties")%>">
				<div id="propertiesNA" style="height: 300px; text-align: center;  position:relative">
					<span style="position:absolute; top:50%; left: 50%; height:10em; margin-top:-2em; margin-left:-10em"><%= LanguageUtil.get(pageContext, "message.category.toplevel.na") %></span>
				</div>
				<div id="propertiesDiv" style="display: none">
					<form id="updateCatPropertiesForm" dojoType="dijit.form.Form">
						<dl >
							<dt>
								<label for="CatVelVarName">
                                    <%= LanguageUtil.get(pageContext, "Variable-ID") %>
                                </label>
							</dt>
							<dd style="clear: none;">
								<input type="hidden" id=""/>
								<input type="text" dojoType="dijit.form.TextBox" id="CatVelVarName" readonly="readonly" name="categoryVarName" maxlength="50" size="30"  value="" />
							</dd>
							<dt>
                                <label for="CatName" class="required">
                                    <%= LanguageUtil.get(pageContext, "category-name") %>
                                </label>
							</dt>
							<dd>
								<input type="text" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" id="CatName" name="categoryName" maxlength="50" size="30"  value="" />
							</dd>
							<dt>
                                <label for="CatKey">
                                    <%= LanguageUtil.get(pageContext, "category-unique-key") %>
                                </label>
							</dt>
							<dd>
								<input type="text" dojoType="dijit.form.TextBox" id="CatKey" name="key" size="30" maxlength="255" value="" />
							</dd>

							<dt>
								<label for="CatKeywords">
                                    <%= LanguageUtil.get(pageContext, "keywords") %>
                                </label>
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
            <% if (hasViewPermision) { %>
			<div id="TabThree" dojoType="dijit.layout.ContentPane" title="<%=LanguageUtil.get(pageContext, "permissions")%>" >
				<div id="permissionNA" style="height: 300px; text-align: center;  position:relative">
					<span style="position:absolute; top:50%; left: 50%; height:10em; margin-top:-2em; margin-left:-10em"><%= LanguageUtil.get(pageContext, "message.category.toplevel.na") %></span>
				</div>
				<div id="permissionDiv" style="display: none">
					<%@ include file="/html/portlet/ext/common/edit_permissions_tab_inc_ajax.jsp" %>
				</div>
            </div>
            <% } %>
			<!-- END Permission Tab -->

		</div>
		<!-- END Tabs -->
	</div>

	<script language="Javascript">
        /**
         focus on search box
         **/
        require([ "dijit/focus", "dojo/dom", "dojo/domReady!" ], function(focusUtil, dom){
            dojo.require('dojox.timing');
            t = new dojox.timing.Timer(500);
            t.onTick = function(){
                focusUtil.focus(dom.byId("catFilter"));
                t.stop();
            }
            t.start();
        });
	</script>

</liferay:box>

<!-- START Add Category pop up -->
<div id="add_category_dialog"  dojoType="dijit.Dialog" style="display:none;width:300px" draggable="true" title="<%= LanguageUtil.get(pageContext, "add-category") %>" >
	<div dojoType="dijit.layout.ContentPane">
		<span id="savedMessage"></span>
		<form id="addCatPropertiesForm" dojoType="dijit.form.Form" style="max-width: 260px; max-height: 330px;">
			<div class="form-inline">
				<dl>
					<dt><label for="addCatVelVarName" id="VariableIdTitle"><%= LanguageUtil.get(pageContext, "Variable-ID") %></label></dt>
					<dd><input id="addCatVelVarName" readonly="true" style="width:100%;" class="input-text-naked" /></dd>
				</dl>
				<dl>
					<dt>
                        <label for="addCatName" class="required">
                            <%= LanguageUtil.get(pageContext, "Name") %>
                        </label>
                    </dt>
					<dd><input dojoType="dijit.form.ValidationTextBox" id="addCatName" type="text" tabindex="1" required="true" onblur="fillVelocityVarName(); " invalidMessage="Required." maxlength="255"/></dd>
				</dl>
				<dl>
					<dt>
                        <label for="addCatKey">
                            <%= LanguageUtil.get(pageContext, "Key") %>
                        </label>
                    </dt>
					<dd><input dojoType="dijit.form.TextBox" id="addCatKey" type="text" tabindex="2" maxlength="255"/></dd>
				</dl>
				<dl>
					<dt>
                        <label for="addCatKeywords">
                            <%= LanguageUtil.get(pageContext, "keywords") %>
                        </label>
                    </dt>
					<dd><textarea dojoType="dijit.form.Textarea" id="addCatKeywords" tabindex="3" style="min-height:100px; max-height:100px"></textarea></dd>
				</dl>
			</div>
			<div class="buttonRow-right">
            	<button dojoType="dijit.form.Button" tabindex="5" onclick="dijit.byId('add_category_dialog').hide()" type="button" class="dijitButtonFlat"><%= LanguageUtil.get(pageContext, "Cancel") %></button>
				<button dojoType="dijit.form.Button" tabindex="4" onclick="saveOrUpdateCategory(true)" type="button"><%= LanguageUtil.get(pageContext, "Save") %></button>
			</div>
		</form>
	</div>
</div>

<!-- Import Categories Popup -->
<div id="importCategoriesOptions" dojoType="dijit.Dialog" style="display:none" title="<%= LanguageUtil.get(pageContext, "import") %>">
	<div dojoType="dijit.layout.ContentPane" style="width:300px;" class="box" hasShadow="true">
		<dl class="vertical">
			<dt><label for="uploadFile" class="required"><%= LanguageUtil.get(pageContext, "Import-Options") %></label></dt>
			<dd><input type="file" id="uploadFile" class="upload"/></dd>
			<div class="clear"></div>

			<dt><label class="required"><%= LanguageUtil.get(pageContext, "Import-Options") %></label></dt>
			<dd>
				<input type="radio" dojoType="dijit.form.RadioButton" name="importMode" id="radioOne" checked /></span>
				<label for="radioOne"><%= LanguageUtil.get(pageContext, "Replace") %></label>

				<input type="radio" dojoType="dijit.form.RadioButton" name="importMode" id="radioTwo" />
				<label for="radioTwo"><%= LanguageUtil.get(pageContext, "add-edit") %></label>
			</dd>
			<div class="clear"></div>
		</dl>
		<div class="buttonRow">
			<button dojoType="dijit.form.Button" type="button" onclick="importCategories()" iconClass="uploadIcon" ><%= LanguageUtil.get(pageContext, "import") %></button>
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