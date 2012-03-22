<%@page import="com.dotmarketing.util.Config"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.util.PortletURLUtil"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="java.io.PrintWriter"%>
<%@page import="com.dotmarketing.util.DateUtil"%>
<%@page import="com.liferay.util.cal.CalendarUtil"%>
<%@page import="com.dotmarketing.tag.ajax.TagAjax"%>
<%@page import="com.dotmarketing.tag.model.Tag"%>
<%@page import="java.util.ArrayList"%>
<%@page import="javax.portlet.WindowState"%>
<%@page import="com.dotmarketing.business.Layout"%>
<%@page import="com.dotmarketing.util.WebKeys"%>
<%@page import="com.dotmarketing.util.URLEncoder"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@ page import="com.liferay.portlet.ActionRequestImpl"%>
<%@ page import="com.liferay.portlet.ActionResponseImpl"%>
<%@ page import="com.liferay.portal.util.ReleaseInfo"%>



<%@ page import="javax.portlet.ActionRequest"%>
<%@ page import="javax.portlet.ActionResponse"%>
<%@ page import="javax.portlet.PortletConfig"%>
<%@ page import="javax.servlet.http.HttpServletRequest"%>
<%@ page import="javax.servlet.http.HttpServletResponse"%>
<%@ page import="javax.servlet.http.HttpSession"%>

<%@page import="java.lang.Exception"%>

<%@ page import="org.apache.struts.action.ActionForm"%>
<%@ page import="org.apache.struts.action.ActionMapping"%>

<html xmlns="http://www.w3.org/1999/xhtml">

<%

List<Host> allHosts = APILocator.getHostAPI().findAll(APILocator.getUserAPI().getSystemUser(),true);

String dojoPath = Config.getStringProperty("path.to.dojo");

String currentHostId = request.getSession().getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID).toString();
Host currentHost = APILocator.getHostAPI().find(currentHostId, APILocator.getUserAPI().getSystemUser(), false);
String currentHostName = currentHost.getHostname();

%>

<style type="text/css">
@import "<%=dojoPath%>/dojox/grid/enhanced/resources/claro/EnhancedGrid.css?b=<%= ReleaseInfo.getBuildNumber() %>";
@import "<%=dojoPath%>/dojox/grid/resources/tundraGrid.css?b=<%= ReleaseInfo.getBuildNumber() %>";
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

<script type="text/javascript" src="/dwr/interface/TagAjax.js"></script>

<script type="text/javascript">
	dojo.require("dijit.Dialog");
	dojo.require("dijit.form.Form");
	dojo.require("dijit.form.TextBox");
	dojo.require("dijit.form.Textarea");
	dojo.require("dijit.form.ValidationTextBox");
	dojo.require("dijit.form.Button");
	dojo.require("dijit.form.CheckBox");
	dojo.require("dojox.grid.EnhancedGrid");
	dojo.require("dojox.grid.enhanced.DataSelection");
	dojo.require("dojox.grid.enhanced.plugins.Menu");
	dojo.require("dojox.grid.enhanced.plugins.DnD");
	dojo.require("dojox.grid.enhanced.plugins.NestedSorting");
	dojo.require("dojox.grid.enhanced.plugins.IndirectSelection");
	dojo.require("dojox.grid.enhanced.plugins.Pagination");
    dojo.require("dojox.grid.enhanced.plugins.Search");
    dojo.require("dojo.io.iframe");
	dojo.require("dojo.data.ItemFileReadStore");
	dojo.require("dojo.data.ItemFileWriteStore");
	dojo.require("dojox.data.QueryReadStore");
	dojo.require("dojox.timing._base");


	dojo.require("dotcms.dijit.form.HostFolderFilteringSelect");
	dojo.require("dotcms.dojo.data.UsersReadStore");


	var tagNameMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "tag-name")) %>';
	var hostMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "tag-storage-host")) %>';
	var tagSavedMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "tag-saved")) %>';
	var tagRemovedMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "tag-removed")) %>';
	var confirmRemoveTagMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "confirm-remove-tag")) %>';
	var exportTagsMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "export-tags-message")) %>';
	var tagNameAlreadyExistForSelectedHost = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "tag-for-host-already-exists")) %>';
	var noResultsMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "tag-no-search-results")) %>';
	var allTagMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "tag-all"))%>';
	var addTagMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add-tag"))%>';
	var editTagMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "edit-tag"))%>';
	var tagsImportedMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.tags.imported"))%>';
	var ImportTagMessageErrorMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.tags.imported.error"))%>';
	var batchDeleteMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.tags.delete.tags")) %>';
	var batchDeleteErrorMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.tags.delete.tags.error")) %>';

	var currentHostId = '<%=currentHostId %>';
	var currentHostName = '<%=currentHostName %>';

	var tagsGrid;
	var layout;
	var tagStore;

    var isNewTag = false;

  	dojo.provide("TagsStore");

	var formatHref = function(value, index) {
		var grid = dijit.byId("tagsEnhancedGrid");
        var tagId = grid.store.getValue(grid.getItem(index), 'tagId');
        var tagName = grid.store.getValue(grid.getItem(index), 'tagname');
        var hostId = grid.store.getValue(grid.getItem(index), 'hostId');
        var hostName = grid.store.getValue(grid.getItem(index), 'hostName');
		return "<a href=\"javascript:tagClicked('"+index+"','"+tagId+"', '"+tagName+"', '"+hostId+"', '"+hostName+"')\" >"+tagName+"</a>";
	};

  	function createStore(params) {
		if(params==null) params = '';

		tagStore = new dojox.data.QueryReadStore({
			url : '/JSONTags'+params
		});
	}

	function createGrid() {
		layout = [{
			field: 'tagname',
			name: tagNameMsg,
			width: '30%',
			formatter: formatHref
		},
		{
			field: 'hostName',
			name: hostMsg,
			width: '70%'
		}];

		tagsGrid = new dojox.grid.EnhancedGrid({
			jsId : "tagsEnhancedGrid",
			id : "tagsEnhancedGrid",
			rowsPerPage : 25,
			store: tagStore,
			rowSelector: '20px',
			autoWidth : true,
			initialWidth : '100%',
			autoHeight : true,
			escapeHTMLInData : false,
			class: "tundra",
			structure: layout,
			dnd: true,
			plugins:{
				pagination: {
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
		},
		document.createElement('div'));

		// append the new grid
		dojo.byId('tagsGrid').appendChild(tagsGrid.domNode);


	};

	dojo.addOnLoad(function () {

		dojo.style(dojo.byId('tagsGridWrapper'), { visibility: 'visible'});
		dojo.style(dojo.byId('loadingTagsWrapper'), { display: 'none'});

		//create store
		createStore();

		//create grid
		createGrid();

		 // Call startup, in order to render the grid:
		 tagsGrid.startup();

		 dojo.connect(dijit.byId("addTagDialog"), "hide", function (evt) {
				dojo.byId("savedMessage").innerHTML = "";
			});

  		});

		function resetSearch() {
			dijit.byId("showGlobal").set('checked',false);
			document.getElementById("globalFilter").value='0';
			var grid = dijit.byId("tagsEnhancedGrid");
	        document.getElementById("filterBox").value='';

			doSearch();
		}

		function tagClicked(index,tagId,tagName,hostId,hostName) {
			/*var grid = dijit.byId("tagsEnhancedGrid");
	        var tagId = grid.store.getValue(grid.getItem(evt.rowIndex), 'tagId');
	        var tagName = grid.store.getValue(grid.getItem(evt.rowIndex), 'tagname');
	        var hostId = grid.store.getValue(grid.getItem(evt.rowIndex), 'hostId');
	        var hostName = grid.store.getValue(grid.getItem(evt.rowIndex), 'hostName');*/

	        dijit.byId('addTagDialog').set('title',editTagMsg);
	        dijit.byId('deleteButton').set('disabled',false);

	        dijit.byId('addTagDialog').show();
			dojo.byId('addTagErrorMessagesList').innerHTML = '';

			dijit.byId('tagName').set('value', tagName);
			document.getElementById('tagId').value = tagId;
			document.getElementById('tagStorage').value = hostId;
			document.getElementById('tagStorage_dropDown').value = hostName;

			dijit.byId('tagStorage_dropDown').set("disabled",true);
			document.getElementById('tagStorage_dropDown').disabled=true;

			isNewTag = false;
		}

		function doSearch() {
			dojo.byId('tagsGrid').innerHTML='';
	        var globalFilter = (document.getElementById("globalFilter").value == '1') ? '1' :'0';
	        var tagNameFilter = document.getElementById("filterBox").value;
			var params = "?tagname="+tagNameFilter+"&global="+globalFilter;
			tagsGrid.destroy(true);
			createStore(params);
			createGrid();
	        tagsGrid.startup();

		}

		function checkGlobalTags(){
			var globalCheck = (dijit.byId("showGlobal").checked) ? '1' :'0';
			document.getElementById("globalFilter").value = globalCheck;
			doSearch();
		}

		function searchTagByName() {
			var nameFilter = document.getElementById("filterBox").value;
			doSearch();
		}


		function updateHiddenFields (){
			var txtIndexObj = document.getElementById('tagStorage');
			txtIndexObj.value = dijit.byId('tagStorage_dropDown').get('value');
		}

		function addNewTag() {

			isNewTag = true;

			dijit.byId('newTagForm').reset();
			dijit.byId('addTagDialog').set('title',addTagMsg);
			dijit.byId('addTagDialog').show();
			dijit.byId('deleteButton').set('disabled',true);
			dijit.byId('tagStorage_dropDown').set("disabled",false);
			document.getElementById('tagStorage_dropDown').disabled=false;
			dojo.byId('addTagErrorMessagesList').innerHTML = '';

			document.getElementById('tagStorage').value = currentHostId;
			document.getElementById('tagStorage_dropDown').value = currentHostName;

		}

	     //Handler when the user clicks the cancel button
	   	function cancelAddNewTag () {
	   		dijit.byId('addTagDialog').hide();
	   	}

	   	//Handler to save/update the tag
	   	function saveTag() {

	   		if(!dijit.byId('newTagForm').validate())
	   			return;

	   		var tagId = document.getElementById('tagId').value;
	   		var tagName = document.getElementById('tagName').value;

	   		if(tagName.indexOf(',')>-1) {
	   			var message = '<%= LanguageUtil.get(pageContext, "message.tags.add.tags.error") %>';
	   			dojo.byId("savedMessage").innerHTML = message;
	   			return;
	   		}
	   		//var userId = dijit.byId('userId').attr('value') == null?'':dijit.byId('userId').attr('value');
	   		var hostId = document.getElementById('tagStorage').value;

	   		if(isNewTag)
	   			TagAjax.addTag(tagName, "" , hostId, saveTagCallback);
	   		else
	   			TagAjax.updateTag(tagId, tagName, hostId, saveTagCallback);
	   	}

	   	function saveTagCallback (data) {
	   		if(data["saveTagErrors"] != null ) {
	   	   		dojo.byId('addTagErrorMessagesList').innerHTML = '';
	   	   		dojo.place("<li>" + tagNameAlreadyExistForSelectedHost + "</li>", "addTagErrorMessagesList", "last");
	   		}else{
	   	   		dijit.byId('addTagDialog').hide();
	   	   		showDotCMSSystemMessage(tagSavedMsg);
	   	   		doSearch();
	   		}
	   	}

	   	//Event handler then deleting a tag
	   	function deleteTag() {
	   		var tagId = document.getElementById('tagId').value;
	   		if(confirm(confirmRemoveTagMsg)) {
	   			TagAjax.deleteTag(tagId,deleteTagCallback);
	   		}
	   	}

	   	//Callback from the server to confirm a tag deletion
	   	function deleteTagCallback () {
	   		dijit.byId('addTagDialog').hide();
	   		showDotCMSSystemMessage(tagRemovedMsg);
			doSearch();
	   	}

	   	function exportTags() {
	   		var globalCheck = (document.getElementById("globalFilter").value == '1') ? '1' :'0';
	   		var filter = dijit.byId("filterBox").value;
			var downloadPdfIframeName = "downloadPdfIframe";
			var iframe = dojo.io.iframe.create(downloadPdfIframeName);
			dojo.io.iframe.setSrc(iframe, "/JSONTags?tagname="+filter+"&global="+globalCheck+"&action=export", true);
	   	}

		function openImportTagsDialog() {
			dijit.byId('importTagsForm').reset();
			dijit.byId('importTagsDialog').show();
			dojo.byId('importTagsErrorMessagesList').innerHTML = '';
		}

	   	function cancelImportTags(){
	   		dijit.byId('importTagsDialog').hide();
	   	}

	   	function importTags(){
	   		var file = dwr.util.getValue('uploadFile');
			TagAjax.importTags(file, importTagsCallback);
	   	}

	   	function importTagsCallback (data) {
	   		if(data["importTagErrors"] != null ) {
	   	   		dojo.byId('importTagsErrorMessagesList').innerHTML = '';
	   	   		dojo.place("<li>" + ImportTagMessageErrorMsg + "</li>", "importTagsErrorMessagesList", "last");
	   		}else{
	   	   		dijit.byId('importTagsDialog').hide();
	   	   		showDotCMSSystemMessage(tagsImportedMsg);
	   	   		doSearch();
	   		}
	   	}

		function alterFocus(toBlur, toFocus) {
			if(toBlur.id != "tagName" && toBlur.id != "tagsGridWrapper" && toBlur.id != "loadingTagsGridWrapper"
					&& toBlur.id != "tagsGrid" && toBlur.id != "tagsEnhancedGrid" && toBlur.id == "tagsEnhancedGridHdr0"
					|| (toBlur.id == toFocus.id && toBlur.id == "filterBox")) {
				toBlur.blur();
				toFocus.focus();
			}
		}

		// delete muliple or single category, via ajax
		function deleteTagsBatch() {
			var items = tagsGrid.selection.getSelected();
			if(items.length < 1) {
				showDotCMSSystemMessage(batchDeleteErrorMsg);
			}
			else {
				dojo.forEach(items, function(selectedItem, index) {
			        if (selectedItem !== null) {
						TagAjax.deleteTag(selectedItem.i.tagId);
			        }
			    });
				showDotCMSSystemMessage(batchDeleteMsg);

				var t = new dojox.timing.Timer();
				t.setInterval(1000);
				t.onTick = function() {
					t.stop();
					doSearch();
					tagsGrid.selection.clear();
		        }
		        t.start();


			}


		}

		function downloadCSVSampleFile(){
	   		var globalCheck = (document.getElementById("globalFilter").value == '1') ? '1' :'0';
	   		var filter = dijit.byId("filterBox").value;
			var downloadPdfIframeName = "downloadPdfIframe";
			var iframe = dojo.io.iframe.create(downloadPdfIframeName);
			dojo.io.iframe.setSrc(iframe, "/JSONTags?tagname="+filter+"&global="+globalCheck+"&action=download", true);
		}


   </script>
<div class="portlet-wrapper">
<jsp:include page="/html/portlet/ext/browser/sub_nav.jsp"></jsp:include>

	<div class="yui-g nameHeader">
		<div class="yu-u first" id="filters">
	        <input type="hidden" name="host_id" id="host_id" value="<%=(String)session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID)%>">
			<input type="text" name="filterBox" value="" dojoType="dijit.form.TextBox" placeHolder="Filter" trim="true" id="filterBox" intermediateChanges="true" onChange="searchTagByName();" onblur="alterFocus(document.activeElement, this);" >
		       <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Search")) %>
		    </button>
			<button dojoType="dijit.form.Button" iconclass="resetIcon" id="resetButton" onClick="resetSearch()">
		       <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Reset")) %>
		    </button>
		    <input id="showGlobal" name="showGlobal" dojoType="dijit.form.CheckBox" value="" onChange="checkGlobalTags()"/>
			<label for="showGlobal"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "show-global-tags")) %></label>
			<input type="hidden" name="globalFilter" id="globalFilter" value="0">
		</div>

		<div class="yui-u" style="text-align:right;">
		<form name="export_form" id="export_form" method="get">
			<button dojoType="dijit.form.Button" onclick="addNewTag()" type="button" iconClass="plusIcon">
				<%= LanguageUtil.get(pageContext, "add-tag") %>
			</button>
			<button dojoType="dijit.form.Button" type="button" iconClass="uploadIcon" onclick="openImportTagsDialog()">
				<%= LanguageUtil.get(pageContext, "import-tags") %>
			</button>
			<button dojoType="dijit.form.Button" type="button" iconClass="downloadIcon" onclick="exportTags()">
				<%= LanguageUtil.get(pageContext, "export-tags") %>
			</button>
			<button dojoType="dijit.form.Button" type="button" onclick="deleteTagsBatch()" iconClass="deleteIcon">
			<%= LanguageUtil.get(pageContext, "delete-tags") %>
			</button>
			<input type="hidden" id="cmd" value="none">
			</form>
		</div>
	</div>
	<div id="loadingTagsWrapper" style="text-align:center"><img src="/html/js/lightbox/images/loading.gif"></div>

	<div id="tagsGridWrapper" style="overflow-y:auto;overflow-x:hidden;">
		<div id="tagsGrid"></div>
	</div>
</div>

 <%--------------- Add Tag Dialog-----------------%>

<div id="addTagDialog" title="<%= LanguageUtil.get(pageContext, "edit-tag") %>" dojoType="dijit.Dialog" style="display: none;width:500px">
	<form id="newTagForm" dojoType="dijit.form.Form" class="roleForm">
		<div style="text-align: center">
	       		<span  id="savedMessage" style="color:red; font-size:11px; font-family: verdana; " >
				</span>
		</div>
		<dl>
			<dt></dt>
			<dd><ul id="addTagErrorMessagesList"></ul></dd>

			<dt><%= LanguageUtil.get(pageContext, "tag") %>:</dt>
			<dd><input id="tagName" type="text" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" /></dd>
			<dt><%= LanguageUtil.get(pageContext, "Host") %>:</dt>
			<dd>
			<input id="tagId" type="hidden" value=" " />
			<input id="userId" type="hidden" value=" " />
			<input id="tagStorage" type="hidden" value=" "/>
			<select id="tagStorage_dropDown" name="tagStorage_dropDown" dojoType="dijit.form.FilteringSelect" autocomplete="true" invalidMessage="Required." onchange="verifyHiddenFields()">
			<option value="SYSTEM_HOST"><%=LanguageUtil.get(pageContext, "tag-all-hosts") %></option>
			<%for(Host h: allHosts){
				if(!h.getIdentifier().equals(Host.SYSTEM_HOST) && h.isLive())
					%><option value="<%=h.getIdentifier() %> "><%=h.getHostname() %></option>
			<%
			}
			%>

			</select>

			<script type="text/javascript">

			dojo.addOnLoad(verifyHiddenFields);

			function verifyHiddenFields() {
				var txtIndexObj = document.getElementById('tagStorage');
				txtIndexObj.value = dijit.byId('tagStorage_dropDown').get('value');
			}


			</script>
			</dd>
		</dl>

		<div class="buttonRow">
			<button dojoType="dijit.form.Button" type="button" iconClass="cancelIcon" onClick="cancelAddNewTag()" id="cancelAddOrEdit">
		        <%= LanguageUtil.get(pageContext, "Cancel") %>
		    </button>
		    <button dojoType="dijit.form.Button" type="button" iconClass="saveIcon" onClick="saveTag()" id="saveButton">
		        <%= LanguageUtil.get(pageContext, "Save") %>
		    </button>
		    <button dojoType="dijit.form.Button" type="button" iconClass="deleteIcon" onClick="deleteTag()" id="deleteButton">
		        <%= LanguageUtil.get(pageContext, "Delete") %>
		    </button>
		</div>
	</form>
</div>
<%--------------- /Add Tag Dialog-----------------%>

<%--------------- Import Tag Dialog-----------------%>

<div id="importTagsDialog" title="<%= LanguageUtil.get(pageContext, "import-tags") %>" dojoType="dijit.Dialog" style="display: none;width:500px">
	<form id="importTagsForm" dojoType="dijit.form.Form" class="roleForm">
		<dl>
			<dt></dt>
			<dd><ul id="importTagsErrorMessagesList"></ul></dd>
		</dl>

		<input type="file" id="uploadFile" name="uploadFile" />
		<br><br>
		<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "upload-csv-with-tags")) %>
		<br><br>
		<div style="text-align:center">
		<a onclick="downloadCSVSampleFile()" href="#"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "download-sample-csv-file")) %></a>
		</div>
		<br>
		<div class="buttonRow">
			<button dojoType="dijit.form.Button" type="button" iconClass="cancelIcon" onClick="cancelImportTags()" id="cancelImport">
		        <%= LanguageUtil.get(pageContext, "Cancel") %>
		    </button>
		    <button dojoType="dijit.form.Button" type="button" iconClass="uploadIcon" onClick="importTags()" id="importButton">
		        <%= LanguageUtil.get(pageContext, "Import") %>
		    </button>
		</div>
	</form>
</div>
<%--------------- /Import Tags Dialog-----------------%>
