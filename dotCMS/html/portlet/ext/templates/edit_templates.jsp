<%@ page import="com.dotmarketing.beans.Identifier" %>
<%@ page import="com.dotmarketing.business.IdentifierFactory" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.dotmarketing.business.APILocator" %>
<%@ page import="com.dotmarketing.business.PermissionAPI" %>
<%@ page import="com.dotmarketing.util.InodeUtils"%>
<%@ page import="com.dotmarketing.portlets.templates.struts.TemplateForm"%>
<%@ page import="java.net.URLDecoder"%>
<%@ include file="/html/portlet/ext/templates/init.jsp" %>
<%@page import="com.dotmarketing.portlets.containers.business.ContainerAPI"%>

<script src="/html/js/ace-builds-1.1.01/src-noconflict/ace.js" type="text/javascript"></script>
<style type="text/css">
    #aceEditorArea { 
        position: relative;
    }
    .show{
    	width: 585px;
        height: 500px;
        border:1px solid #C0C0C0;
    }
    .hidden{
		display: none;
	}
	.ace_scrollbar {
    	overflow: auto;
	}
</style>

<%@page import="com.dotmarketing.portlets.contentlet.business.HostAPI"%>


<%

	PermissionAPI perAPI = APILocator.getPermissionAPI();
	ContainerAPI containerAPI = APILocator.getContainerAPI();
	HostAPI hostAPI = APILocator.getHostAPI();

	com.dotmarketing.portlets.templates.model.Template template;
	if (request.getAttribute(com.dotmarketing.util.WebKeys.TEMPLATE_EDIT)!=null) {
		template = (com.dotmarketing.portlets.templates.model.Template) request.getAttribute(com.dotmarketing.util.WebKeys.TEMPLATE_EDIT);
	}
	else {
		template = (com.dotmarketing.portlets.templates.model.Template) com.dotmarketing.factories.InodeFactory.getInode(request.getParameter("inode"),com.dotmarketing.portlets.templates.model.Template.class);
	}
	//Permissions variables
	boolean hasOwnerRole = com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSOwnerRole().getId());
	boolean hasAdminRole = com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole().getId());
	boolean canUserWriteToTemplate = hasOwnerRole || hasAdminRole || perAPI.doesUserHavePermission(template,PermissionAPI.PERMISSION_WRITE,user);
	boolean canUserPublishTemplate = hasOwnerRole || hasAdminRole || perAPI.doesUserHavePermission(template,PermissionAPI.PERMISSION_PUBLISH,user);
	//http://jira.dotmarketing.net/browse/DOTCMS-1473 basically if teh user has portlet permissions they can add new templates and containers
	Identifier id=null;
	if(!InodeUtils.isSet(template.getInode())){
		canUserWriteToTemplate = true;
		canUserPublishTemplate = true;
	}
	else {
		id = APILocator.getIdentifierAPI().find(template);
	}

	String referer = "";
	if (request.getParameter("referer") != null) {
		referer = URLDecoder.decode(request.getParameter("referer"), "UTF-8");
		referer = UtilMethods.encodeURL(referer);
	} else {
		java.util.Map params = new java.util.HashMap();
		params.put("struts_action",new String[] {"/ext/templates/view_templates"});
		if (request.getParameter("pageNumber")!=null) {
			int pageNumber = Integer.parseInt(request.getParameter("pageNumber"));
			params.put("pageNumber",new String[] { pageNumber + "" });
		}
		referer = UtilMethods.encodeURL(com.dotmarketing.util.PortletURLUtil.getActionURL(request,WindowState.MAXIMIZED.toString(),params));
	}

	TemplateForm form = (TemplateForm)request.getAttribute("TemplateForm");

	//Getting the list of containers for the container selector
	String hostId = "";
	if(form.getHostId()!=null)
		hostId = form.getHostId();
	List<Host> listHosts= (List <Host>) request.getAttribute(com.dotmarketing.util.WebKeys.TEMPLATE_HOSTS);
	if(!UtilMethods.isSet(hostId)) {
		if(request.getParameter("host_id") != null) {
			hostId = request.getParameter("host_id");
		} else {
			hostId = (String)session.getAttribute(com.dotmarketing.util.WebKeys.SEARCH_HOST_ID);
		}
	}
	Host host = null;
	if(UtilMethods.isSet(hostId)) {
		host = APILocator.getHostAPI().find(hostId, APILocator.getUserAPI().getSystemUser(), false);
	}

%>

<script language="Javascript">

	dojo.require('dotcms.dijit.form.FileSelector');
	dojo.require('dotcms.dojo.data.ContainerReadStore');

	var referer = '<%=referer%>';

	function submitfm(form,subcmd) {
		window.onbeforeunload=true;
		if(dijit.byId("toggleEditor").checked){
		document.getElementById("bodyField").value=editor.getValue();
		}
		if (form.admin_l2) {
			for (var i = 0; i < form.admin_l2.length; i++) {
				form.admin_l2.options[i].selected = true;
			}
		}
		form.<portlet:namespace />cmd.value = '<%=Constants.ADD%>';
		form.<portlet:namespace />subcmd.value = subcmd;
		form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/templates/edit_template" /></portlet:actionURL>';
		submitForm(form);
	}

	var copyAsset = false;

	function cancelEdit() {
		window.onbeforeunload=true;
		self.location = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/templates/edit_template" /><portlet:param name="cmd" value="unlock" /><portlet:param name="inode" value="<%=template.getInode()%>" /></portlet:actionURL>&referer=' + referer;
	}

	function submitfmDelete() {
		if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.template.confirm.delete.template")) %>'))
		{
			self.location = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/templates/edit_template" /><portlet:param name="cmd" value="full_delete" /><portlet:param name="inode" value="<%=template.getInode()%>" /></portlet:actionURL>&referer=' + referer;
		}
	}

	function showAddContainerDialog() {
		dijit.byId('containersList').attr('value', '');
		dijit.byId('containerSelector').show();
	}

	function addContainer() {
		dijit.byId('containerSelector').hide();
		var value = dijit.byId('containersList').attr('value');
		var container = dijit.byId('containersList').attr('item');
		pos= editor.getCursorPosition();
		insertAtCursor('#parseContainer(\'' + value + '\')\n');
		insertAtCursor('## This is autogenerated code that cannot be changed\n');
		insertAtCursor('## Container: ' + container.title + '\n');
	}

	function addFile() {
		fileSelector.show();
	}

	function addFileCallback(file) {
		if(file.extension == 'js') {
			var html = '<script type="text/javascript" src="' + file.path + file.fileName + '" >' + '<' + '/script' + '>';
		} else if (file.extension == 'css') {
			var html = '<link href="' + file.path + file.fileName + '" rel="stylesheet" type="text/css" />';
		}
		pos= editor.getCursorPosition();
		insertAtCursor(html);
	}

	function insertAtCursor( myValue) {
		myField = document.getElementById("bodyField");
		var acetId = document.getElementById('aceEditorArea');
		if(acetId.className.indexOf("show") == 0){
			editor.getSession().insert(pos, myValue);
		} else {
			myField.value=myField.value+myValue;
		}
	}

	function selectTemplateVersion(objId,referer) {
		if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.template.confirm.replace.template")) %>')){
			window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/templates/edit_template" /></portlet:actionURL>&cmd=getversionback&inode_version=' + objId + '&referer=' + referer;
		}
	}

	var allfields = false;
	var currentfield = "";
	function startSpelling (field, fieldName) {
		allfields = false;
		document.getElementById(field).value=editor.getCode();
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
				nextField = "bodyField";
				nextFieldTitle = "Body";
				fieldTitle = "Description"
			}
			if (currentfield == "bodyField") {
				alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.template.spelling.check.ended")) %>');
				w.focus ();
			} else {
				if (confirm(fieldTitle + '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.template.spelling.check.finished.next.field")) %>')) {
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
			alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.template.spelling.check.ended")) %>');
			w.focus ();
		}
	}


    function deleteVersion(objId){
        if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.template.delete.version")) %>')){
			window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/templates/edit_template" /></portlet:actionURL>&cmd=deleteversion&inode=' + objId  + '&referer=' + referer;
        }
    }
	function selectVersion(objId) {
        if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.template.replace.version")) %>')){
			window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/templates/edit_template" /></portlet:actionURL>&cmd=getversionback&inode=' + objId + '&inode_version=' + objId  + '&referer=' + referer;
	    }
	}
	function editVersion(objId) {
		window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/templates/edit_template" /></portlet:actionURL>&cmd=edit&inode=' + objId  + '&referer=' + referer;
	}

	function hideEditButtonsRow() {
		dojo.style('editTemplateButtonRow', { display: 'none' });
	}

	function showEditButtonsRow() {
		if( typeof changesMadeToPermissions!= "undefined"){
			if(changesMadeToPermissions==true){
				dijit.byId('applyPermissionsChangesDialog').show();
			}
		}
		dojo.style('editTemplateButtonRow', { display: '' });
		changesMadeToPermissions = false;
	}

</script>

<script language="JavaScript" src="/html/js/cms_ui_utils.js"></script>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"edit-template\") %>" />

	<html:form action='/ext/templates/edit_template' styleId="fm">
	<input name="<portlet:namespace /><%= Constants.CMD %>" type="hidden" value="add">
	<input name="<portlet:namespace />referer" type="hidden" value="<%=referer%>">
	<input name="<portlet:namespace />inode" type="hidden" value="<%=template.getInode()%>">
	<input name="<portlet:namespace />subcmd" type="hidden" value="">
	<input name="userId" type="hidden" value="<%= user.getUserId() %>">
	<input name="admin_l_list" type="hidden" value="">


<!-- START TabContainer-->
<div id="mainTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false">

<!-- START Properties Tab -->
	<div id="templatePropertiesTab" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Properties") %>"  onShow="showEditButtonsRow()">
		<dl>
			<%if(id!=null) {%>
				<dt><%= LanguageUtil.get(pageContext, "Identity") %>:</dt>
				<dd><%= id.getId() %></dd>
			<%}%>
			<% if(host != null) { %>
				<html:hidden property="hostId" value="<%=hostId%>"/>
				<dt><%= LanguageUtil.get(pageContext, "Host") %>:&nbsp;</dt>
				<dd><%= host.getHostname() %></dd>
			<%	} else { %>
				<dt><%= LanguageUtil.get(pageContext, "Host") %>:&nbsp;</dt>
				<dd>
				<select id="hostId" name="hostId" dojoType="dijit.form.FilteringSelect" value="<%=hostId%>">
				<% for(Host h: listHosts) { %>
					<option value="<%=h.getIdentifier()%>"><%=host.getHostname()%></option>
				<% } %>1
				</select>
				</dd>
			<% } %>
			<dt>
				<span class="required"></span>
				<%= LanguageUtil.get(pageContext, "Title") %>:
			</dt>
			<dd><input type="text" dojoType="dijit.form.TextBox" style="width:350px" name="title" id="titleField" value="<%= UtilMethods.isSet(template.getTitle()) ? template.getTitle() : "" %>" /></dd>

			<dt><%= LanguageUtil.get(pageContext, "Description") %>:</dt>
			<dd><input type="text" dojoType="dijit.form.TextBox" style="width:350px" name="friendlyName" id="friendlyNameField" value="<%= UtilMethods.isSet(template.getFriendlyName()) ? template.getFriendlyName() : "" %>" /></dd>

			<dt><%= LanguageUtil.get(pageContext, "Screen-Capture-Image") %>:</dt>
			<dd>
				<input type="text" name="image" dojoType="dotcms.dijit.form.FileSelector" fileBrowserView="thumbnails" mimeTypes="image"
					value="<%= UtilMethods.isSet(form.getImage())?form.getImage():"" %>" showThumbnail="true" />
			</dd>
			<dt><%= LanguageUtil.get(pageContext, "Template") %>:</dt>

			<dd>
				<button dojoType="dijit.form.Button" onClick="showAddContainerDialog()" type="button">
					<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add-container")) %>
				</button>

				<button dojoType="dijit.form.Button" onClick="addFile()" type="button">
					<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add-js-css")) %>
				</button>
			</dd>
			<dd>
				<div id="textEditorArea" style="border: 0px;  width: 600px; height: 500px;">
					<div id="aceEditorArea" class="show"></div>
		    		<html:textarea onkeydown="return catchTab(this,event)" style="width:600px; height:500px; font-size: 12px; display:none;" property="body" styleId="bodyField"></html:textarea>
				</div>
	        	<input type="checkbox" dojoType="dijit.form.CheckBox" name="toggleEditor" id="toggleEditor"  onClick="aceColoration();"  checked="checked"  />
	        	<label for="toggleEditor"><%= LanguageUtil.get(pageContext, "Toggle-Editor") %></label>
			</dd>
		</dl>
	</div>
    <script type="text/javascript">

		dojo.addOnLoad(function() {
   			setTimeout('aceArea()',1); // will not work if called directly!
    	});

      	var editor;
    	function aceArea(){
			editor = ace.edit('aceEditorArea');
			editor.setTheme("ace/theme/textmate");
			editor.getSession().setMode("ace/mode/html");
			editor.getSession().setUseWrapMode(true);
			editor.setValue(document.getElementById('bodyField').value);
			editor.clearSelection();
    	}

    	function aceColoration(){
    		dijit.byId("toggleEditor").disabled=true;
    		var acetId = document.getElementById('aceEditorArea');
			var aceClass = acetId.className;
    		if (dijit.byId("toggleEditor").checked) {
    			document.getElementById('bodyField').style.display = "none";
    			acetId.className = aceClass.replace('hidden', 'show');
    			editor.setValue(document.getElementById('bodyField').value);
    			editor.clearSelection();
    		} else {
    			var editorText = editor.getValue();
    			acetId.className = aceClass.replace('show', 'hidden');
    			document.getElementById('bodyField').style.display = "inline";
    			dojo.query('#bodyField').style({display:''})
    			document.getElementById('bodyField').value = editorText;
    		}
    		dijit.byId("toggleEditor").disabled=false;
    	}

    	var onBeforeUnloadHandle = dojo.connect(dijit.byId('templatePropertiesTab'), "onkeypress", activateOnBeforeUnload);
    	function activateOnBeforeUnload(){
    		window.onbeforeunload=function(){return "";};
    		dojo.disconnect(onBeforeUnloadHandle);
    	}
    </script>
<!-- /Properties -->


<!-- Permissions Tab -->
<%
	boolean canEditAsset = perAPI.doesUserHavePermission(template, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user);
	if (canEditAsset) {
%>
	<div id="filePermissionTab" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Permissions") %>"  onShow="hideEditButtonsRow()">
		<%
			request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT, template);
		%>
		<%@ include file="/html/portlet/ext/common/edit_permissions_tab_inc.jsp" %>
	</div>
<%
	}
%>
<!-- /Permissions Tab  -->

<!-- Versions Tab -->
	<%if(template != null && InodeUtils.isSet(template.getInode())){ %>
		<% request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT, template); %>
		<div id="fileVersionTab" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Versions") %>"  onShow="showEditButtonsRow()">
			<%@ include file="/html/portlet/ext/common/edit_versions_inc.jsp" %>
		</div>
	<%} %>
<!-- /Versions Tab -->

</div>
<!-- /TabContainer-->

<div class="clear"></div>

<!-- Button Row --->
<div class="buttonRow" id="editTemplateButtonRow">

	<% if (!InodeUtils.isSet(template.getInode()) || template.isLive() || template.isWorking()) { %>
		<% if ( canUserWriteToTemplate ) { %>
			<button dojoType="dijit.form.Button" onClick="submitfm(document.getElementById('fm'),'')" iconClass="saveIcon" type="button">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save")) %>
			</button>
		<% } %>
	<%
	if ( canUserPublishTemplate ) { %>
		<button dojoType="dijit.form.Button" onClick="submitfm(document.getElementById('fm'),'publish')" iconClass="publishIcon" type="button">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save-and-publish")) %>
		</button>
	<% } %>

	<% } else if (InodeUtils.isSet(template.getInode())) { %>
		<button dojoType="dijit.form.Button" onClick="selectTemplateVersion(<%=template.getInode()%>, '<%=referer%>')" type="button">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "bring-back-this-version")) %>
		</button>
	<% } %>

	<% if (InodeUtils.isSet(template.getInode()) && template.isDeleted()) {%>
		<button dojoType="dijit.form.Button" onClick="submitfmDelete()" iconClass="deleteIcon" type="button">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "delete-template")) %>
		</button>
	<% } %>

	<button dojoType="dijit.form.Button" onClick="cancelEdit()" iconClass="cancelIcon" type="button">
		<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
	</button>

</div>
</html:form>
</liferay:box>
<div id="editTempateBox" dojoType="dijit.Dialog" style="display:none" title="<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add-container")) %>">
	<div dojoType="dijit.layout.ContentPane" style="width:400px;height:150px;" class="box" hasShadow="true" id="editTemplateBoxCp">
	</div>
</div>

<div dojoAttachPoint="fileBrowser" jsId="fileSelector" onFileSelected="addFileCallback" fileExtensions="js,css" dojoType="dotcms.dijit.FileBrowserDialog">
</div>

<span dojoType="dotcms.dojo.data.ContainerReadStore" jsId="containerStore"></span>

<div dojoType="dijit.Dialog" id="containerSelector" title="<%=LanguageUtil.get(pageContext, "select-a-container")%>">
	<p>
		<%=LanguageUtil.get(pageContext, "Container")%>
  		<select id="containersList" name="containersList" dojoType="dijit.form.FilteringSelect"
        	store="containerStore" searchDelay="300" pageSize="10" labelAttr="fullTitle" searchAttr="title"
            invalidMessage="<%=LanguageUtil.get(pageContext, "Invalid-option-selected")%>">
        </select>
    </p>
    <div class="buttonRow">
		<button dojoType="dijit.form.Button" onclick="addContainer()" type="button"><%=LanguageUtil.get(pageContext, "Add")%></button>
		<button dojoType="dijit.form.Button" onclick="dijit.byId('containerSelector').hide()" type="button"><%=LanguageUtil.get(pageContext, "Cancel")%></button>
	</div>
</div>


