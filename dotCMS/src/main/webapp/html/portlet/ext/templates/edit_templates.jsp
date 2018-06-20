<%@ page import="com.dotmarketing.beans.Identifier" %>
<%@ page import="com.dotmarketing.portlets.containers.business.ContainerAPI" %>
<%@ page import="com.dotmarketing.portlets.contentlet.business.HostAPI" %>
<%@ page import="com.dotmarketing.portlets.templates.struts.TemplateForm" %>
<%@ page import="com.dotmarketing.util.InodeUtils" %>
<%@ page import="java.net.URLDecoder"%>
<%@ include file="/html/portlet/ext/templates/init.jsp" %>

<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>

<script src="/html/js/ace-builds-1.2.3/src-noconflict/ace.js" type="text/javascript"></script>
<style type="text/css">
	.show {
		border: 1px solid #C0C0C0;
	}

	.hidden {
		display: none;
	}

</style>


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
		if(referer.contains("language")){
			referer = referer.replaceAll("language", com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE);
		}
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
    var isNg = '<%=request.getParameter("ng") %>' === 'true';

	function submitfm(form,subcmd) {
        var customEvent = document.createEvent("CustomEvent");

        customEvent.initCustomEvent("ng-event", false, false,  {
            name: "advanced-template-saving",
            data: {}
        });
        document.dispatchEvent(customEvent);

		window.onbeforeunload=true;
		if(dijit.byId("toggleEditor").checked){
		      document.getElementById("bodyField").value=editor.getValue();
		}
		if (form.admin_l2) {
			for (var i = 0; i < form.admin_l2.length; i++) {
				form.admin_l2.options[i].selected = true;
			}
		}
		form.cmd.value = '<%=Constants.ADD%>';
		form.subcmd.value = subcmd;
        form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/templates/edit_template" /></portlet:actionURL>';

        if (isNg) {
            var data = new URLSearchParams(new FormData(form)).toString();
            var req = new XMLHttpRequest();
            req.open("POST", form.action, true);
            req.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
            req.onreadystatechange = function() {
                if (this.readyState == 4 && this.status == 200) {
                    customEvent.initCustomEvent("ng-event", false, false,  {
                        name: "advanced-template-saved",
                        data: {}
                    });
                    document.dispatchEvent(customEvent);
                }
            };
            req.send(data);
        } else {
            submitForm(form);
        }
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

		insertAtCursor('#parseContainer(\'' + value + '\',\'' + new Date().getTime() + '\')\n');
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
		dojo.style('mainTabContainer', { width: '100%'});
	}

	function showEditButtonsRow() {
		if( typeof changesMadeToPermissions!= "undefined"){
			if(changesMadeToPermissions==true){
				dijit.byId('applyPermissionsChangesDialog').show();
			}
		}
		dojo.style('editTemplateButtonRow', { display: '' });
		dojo.style('mainTabContainer', { width: '80%'});
		changesMadeToPermissions = false;
	}

</script>

<script language="JavaScript" src="/html/js/cms_ui_utils.js"></script>
<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"edit-template\") %>" />

	<html:form action='/ext/templates/edit_template' styleId="fm">
	<input name="<%= Constants.CMD %>" type="hidden" value="add">
	<input name="referer" type="hidden" value="<%=referer%>">
	<input name="inode" type="hidden" value="<%=template.getInode()%>">
	<input name="subcmd" type="hidden" value="">
	<input name="userId" type="hidden" value="<%= user.getUserId() %>">
	<input name="admin_l_list" type="hidden" value="">
	<input name="isNg" type="hidden" value='<%=request.getParameter("ng") %>'>
<div class="portlet-main" style="height:100vh;border:0px solid red;">

<!-- START TabContainer-->
<div id="mainTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false">

<!-- START Properties Tab -->
	<div id="templatePropertiesTab" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Properties") %>"  onShow="showEditButtonsRow()" <%= "properties".equals(request.getParameter("selectedTab")) ? "data-dojo-props=\"selected:true\"" : ""%>>
		<div class="form-horizontal">

			<% if(host != null) { %>
				<html:hidden property="hostId" value="<%=hostId%>" />
				<div class="fieldWrapper">
					<div class="fieldName"><%= LanguageUtil.get(pageContext, "Host") %>:&nbsp;</div>
					<div class="fieldValue"><%= host.getHostname() %></div>
				</div>
			<%	} else { %>
				<div class="fieldWrapper">
					<div class="fieldName"><%= LanguageUtil.get(pageContext, "Host") %>:&nbsp;</div>
					<div class="fieldValue">
						<select id="hostId" name="hostId" dojoType="dijit.form.FilteringSelect" value="<%=hostId%>">
							<% for(Host h: listHosts) { %>
								<option value="<%=h.getIdentifier()%>"><%=host.getHostname()%></option>
							<% } %>1
						</select>
					</div>
				</div>
			<% } %>
			<div class="fieldWrapper">
				<div class="fieldName">
					<span class="required"></span>
					<%= LanguageUtil.get(pageContext, "Title") %>:
				</div>
				<div class="fieldValue"><input type="text" dojoType="dijit.form.TextBox"  name="title" id="titleField" value="<%= UtilMethods.isSet(template.getTitle()) ? template.getTitle() : "" %>" /></div>
			</div>
			<div class="fieldWrapper">
				<div class="fieldName"><%= LanguageUtil.get(pageContext, "Description") %>:</div>
				<div class="fieldValue"><input type="text" dojoType="dijit.form.TextBox"  name="friendlyName" id="friendlyNameField" value="<%= UtilMethods.isSet(template.getFriendlyName()) ? template.getFriendlyName() : "" %>" /></div>
			</div>
			<div class="fieldWrapper">
				<div class="fieldName"><%= LanguageUtil.get(pageContext, "Screen-Capture-Image") %>:</div>
				<div class="fieldValue">
					<input type="text" name="image" dojoType="dotcms.dijit.form.FileSelector" fileBrowserView="thumbnails" mimeTypes="image"
						value="<%= UtilMethods.isSet(form.getImage())?form.getImage():"" %>" showThumbnail="true" />
				</div>
			</div>
			<div class="fieldWrapper">
				<div class="fieldName"><%= LanguageUtil.get(pageContext, "Template") %>:</div>
				<div class="fieldValue">
					<button dojoType="dijit.form.Button" onClick="showAddContainerDialog()" type="button">
						<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add-container")) %>
					</button>

					<button dojoType="dijit.form.Button" onClick="addFile()" type="button">
						<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add-js-css")) %>
					</button>
				</div>
			</div>
			<div class="fieldWrapper">
				<div class="fieldName"></div>
				<div class="fieldValue">
					<div id="textEditorArea" style="height:100%;">
						<div id="aceEditorArea" class="show aceText aceTextTemplate"></div>
						<html:textarea onkeydown="return catchTab(this,event)" style="display:none;"  styleClass="aceText aceTextTemplate" property="body" styleId="bodyField"></html:textarea>
					</div>
					<div class="editor-toolbar">
						<div class="checkbox">
							<input type="checkbox" dojoType="dijit.form.CheckBox" name="toggleEditor" id="toggleEditor"  onClick="aceColoration();"  checked="checked"  />
							<label for="toggleEditor" style="margin-right: 10px;"><%= LanguageUtil.get(pageContext, "Toggle-Editor") %></label>
						</div>
						<div id="toggleWrapEditor" class="checkbox">
							<input id="wrapEditor" name="wrapEditor" data-dojo-type="dijit/form/CheckBox" value="true" onChange="handleWrapMode" />
							<label for="wrapEditor"><%= LanguageUtil.get(pageContext, "Wrap-Code") %></label>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
    <script type="text/javascript">

		dojo.addOnLoad(function() {
   			setTimeout('aceArea()',1); // will not work if called directly!
    	});

      	var editor;
    	function aceArea(){
			editor = ace.edit('aceEditorArea');
			editor.setTheme("ace/theme/textmate");
			editor.getSession().setMode("ace/mode/velocity");
			editor.setAutoScrollEditorIntoView(false);
			editor.setValue(document.getElementById('bodyField').value);
			editor.clearSelection();

    	}

        function handleWrapMode(e) {
            editor.getSession().setUseWrapMode(e);
        }

    	function aceColoration(){
            var toggleWrapper = document.getElementById("toggleWrapEditor");
    		dijit.byId("toggleEditor").disabled=true;
    		var acetId = document.getElementById('aceEditorArea');
			var aceClass = acetId.className;
    		if (dijit.byId("toggleEditor").checked) {
    			document.getElementById('bodyField').style.display = "none";
    			acetId.className = aceClass.replace('hidden', 'show');
    			editor.setValue(document.getElementById('bodyField').value);
    			editor.clearSelection();
                toggleWrapper.setAttribute("style", "display: inline-block")
    		} else {
    			var editorText = editor.getValue();
    			acetId.className = aceClass.replace('show', 'hidden');
    			document.getElementById('bodyField').style.display = "inline";
    			dojo.query('#bodyField').style({display:''})
    			document.getElementById('bodyField').value = editorText;
                toggleWrapper.setAttribute("style", "display: none")
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
	<div id="filePermissionTab" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Permissions") %>"  onShow="hideEditButtonsRow()"  <%= "permissions".equals(request.getParameter("selectedTab")) ? "data-dojo-props=\"selected:true\"" : ""%>>
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
		<div id="fileVersionTab" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Versions") %>"  onShow="showEditButtonsRow()"  <%= "history".equals(request.getParameter("selectedTab")) ? "data-dojo-props=\"selected:true\"" : ""%>>
			<%@ include file="/html/portlet/ext/common/edit_versions_inc.jsp" %>
		</div>
	<%} %>
<!-- /Versions Tab -->

</div>
<!-- /TabContainer-->



</div>

<!-- Button Row -->
<div class="content-edit__sidebar" id="editTemplateButtonRow">
	<div class="content-edit-actions">
	<div id="contentletActionsHanger">
		<% if (!InodeUtils.isSet(template.getInode()) || template.isLive() || template.isWorking()) { %>
			<% if ( canUserWriteToTemplate ) { %>
				<a onClick="submitfm(document.getElementById('fm'),'')">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save")) %>
				</a>
			<% } %>
		<%
		if ( canUserPublishTemplate ) { %>
			<a onClick="submitfm(document.getElementById('fm'),'publish')" >
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save-and-publish")) %>
			</a>
		<% } %>
	
		<% } else if (InodeUtils.isSet(template.getInode())) { %>
			<a onClick="selectTemplateVersion('<%=template.getInode()%>', '<%=referer%>')" >
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "bring-back-this-version")) %>
			</a>
		<% } %>
	
		<% if (InodeUtils.isSet(template.getInode()) && template.isDeleted()) {%>
			<a onClick="submitfmDelete()" >
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "delete-template")) %>
			</a>
		<% } %>
	
        <% if (request.getParameter("ng") == null) { %>
            <a onClick="cancelEdit()" >
                <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
            </a>
        <% } %>
	</div>
	</div>
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
