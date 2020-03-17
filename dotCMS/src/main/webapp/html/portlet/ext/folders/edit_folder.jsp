<%@ page import="com.dotmarketing.beans.Inode" %>
<%@ page import="com.dotmarketing.portlets.folders.model.Folder" %>
<%@ page import="com.dotmarketing.util.InodeUtils" %>
<%@ page import="java.util.HashMap"%>
<%@ page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="com.dotmarketing.portlets.fileassets.business.FileAssetAPI" %>
<%@page import="com.dotmarketing.business.CacheLocator"%>

<%@ include file="/html/portlet/ext/folders/init.jsp" %>


<%
	Folder folder = (com.dotmarketing.portlets.folders.model.Folder) request.getAttribute(com.dotmarketing.util.WebKeys.FOLDER_EDIT);
	Folder parentFolder = (Folder) request.getAttribute(com.dotmarketing.util.WebKeys.FOLDER_PARENT);
	Host parentHost = (Host) request.getAttribute(com.dotmarketing.util.WebKeys.HOST_PARENT);

	String thispage = java.net.URLEncoder.encode(CTX_PATH + "/ext/folders/edit_folder?inode=" + folder.getInode() +
			(parentFolder == null?"&phostId=" + parentHost.getIdentifier():"&pfolderId=" + parentFolder.getInode()) + "&openNodes=" + request.getParameter("openNodes") + "&view=" + request.getParameter("view") + "&content=" + request.getParameter("content"),"UTF-8");
	String referer = request.getParameter("referer");
	Structure defaultFileAssetStructure = CacheLocator.getContentTypeCache().getStructureByVelocityVarName(FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME);
%>


<script type='text/javascript' src='/dwr/interface/StructureAjax.js'></script>
<script language="Javascript">

dojo.require("dotcms.dijit.form.HostFolderFilteringSelect");
dojo.require("dotcms.dojo.data.StructureReadStore");
	function encode(str) {
		var result = "";

		for (i = 0; i < str.length; i++) {
			if (str.charAt(i) == " ") result += "+";
			else result += str.charAt(i);
		}

		return escape(result);
	}

	var formName;
	function save(formName) {

		this.formName = formName;

		var form = document.getElementById(this.formName);
		form.cmd.value = '<%=com.liferay.portal.util.Constants.ADD%>';
		val = form.title.value.replace(" ", "");
		if(val == "" && form.showOnMenu.checked){
			alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.folder.menu.items")) %>');
			return false;
		}
		if (document.getElementById("titleField")) {
			var name = document.getElementById("titleField").value;
			if(typeof String.prototype.trim !== 'function')
				document.getElementById("titleField").value = name.replace(/^\s+|\s+$/g, '');
			else
				document.getElementById("titleField").value = name.trim();

			if (document.getElementById("titleField").value == ""){
				alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.folder.name.required.url")) %>');
				return false;
			}
			else if (document.getElementById("titleField").value.indexOf(" ")>-1) {
				alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.folder.name.no.space.in.url")) %>');
				return false;
			}
		}
		form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/folders/edit_folder" /></portlet:actionURL>';
		submitForm(form);
	}

	function goBack() {
		window.location.href = '<%=request.getParameter("referer")%>';
	}

	function beLazy(){
		var titleField = document.getElementById("friendlyNameField");
		var ele = document.getElementById("titleField");
		var title = titleField.value;
		title = title.replace(/^\s+/g, "");
		title = title.replace(/\s/g, "-");
		var arg=/[\+\%\&\!\"\'\#\$\/\\\=\?\�\�}\:\;\*\<\>\`\�\|]/g ;
		title = title.replace(arg,"");
		title = title.replace(/-{2,}/g, "-");
		ele.value = title;
		<% if (parentFolder != null) { %>
		 	dojo.byId("pathToFolder").innerText = "<%= APILocator.getIdentifierAPI().find(parentFolder).getPath() %>"+title;
		<% } else { %>
		 	dojo.byId("pathToFolder").innerText = "/"+title;
		<% } %>
	}

	function displayProperties(id) {
		if (id == "properties") {
			//display basic properties
			document.getElementById("properties").style.display = "";
			document.getElementById("permissions").style.display = "none";
			//changing class for the tabs
			document.getElementById("properties_tab").className ="alpha";
			document.getElementById("permissions_tab").className ="beta";
		}
		else {
			//display advanced properties
			document.getElementById("properties").style.display = "none";
			document.getElementById("permissions").style.display = "";
			//changing class for the tabs
			document.getElementById("properties_tab").className ="beta";
			document.getElementById("permissions_tab").className ="alpha";
		}
	}

	function hideEditButtonsRow() {

		dojo.style('editFolderButtonRow', { display: 'none' });
	}

	function showEditButtonsRow() {
		if( typeof changesMadeToPermissions!= "undefined"){
			if(changesMadeToPermissions == true){
				dijit.byId('applyPermissionsChangesDialog').show();
			}
		}
		dojo.style('editFolderButtonRow', { display: '' });
		changesMadeToPermissions = false;
	}

</script>




<html:form action='/ext/folders/edit_folder' styleId="fm">
<input name="<%= Constants.CMD %>" type="hidden" value="add">
<html:hidden property="inode" />
<html:hidden property="hostId" />
<html:hidden property="path" />
<input name="admin_g_list" type="hidden" value="">
<input type="hidden" name="phostId" value="<%=parentHost != null?parentHost.getIdentifier():""%>">
<input type="hidden" name="pfolderId" value="<%=parentFolder != null?parentFolder.getInode():""%>">
<input type="hidden" name="referer" value="<%=request.getParameter("referer")%>">
<input type="hidden" name="openNodes" value="<%=request.getParameter("openNodes")%>">
<input type="hidden" name="view" value="<%=request.getParameter("view")%>">
<input type="hidden" name="content" value="<%=request.getParameter("content")%>">


<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"edit-folder\") %>" />

<div class="portlet-main">

	<div id="mainTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false">

	<!-- START basic properties -->
		<div id="folderPropertiesTab" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Properties") %>" onShow="showEditButtonsRow()" >
			<div class="form-horizontal">

				<% if (InodeUtils.isSet(folder.getInode())) { %>
					<dl>
						<dt><%= LanguageUtil.get(pageContext, "Identity") %>:</dt>
						<dd><%= folder.getInode() %></dd>
					</dl>
				<%}%>

				<dl>
					<%if(InodeUtils.isSet(folder.getInode())){%>
						<dt><%= LanguageUtil.get(pageContext, "Path-To-Folder") %>:</dt>
						<dd style="width:80%">
							<% if (parentFolder != null) { %>
								<div id="pathToFolder" style="word-break: break-all;"><%= APILocator.getIdentifierAPI().find(parentFolder).getPath() %><%= folder.getName() %></div>
							<% } else { %>
								<div id="pathToFolder" style="word-break: break-all;">/<%= folder.getName() %></div>
							<% } %>
							<!--<html:hidden property="name" />-->
						</dd>
					<%}else{%>
						<dt><%= LanguageUtil.get(pageContext, "Path-To-Folder") %>:</dt>
							<dd style="width:80%">
								<% if (parentFolder != null) { %>
									<div id="pathToFolder" style="word-break: break-all;"><%= APILocator.getIdentifierAPI().find(parentFolder).getPath() %></div>
								<% } else { %>
									<div id="pathToFolder" style="word-break: break-all;">/</div>
								<% } %>
								<!--<html:hidden property="name" />-->
							</dd>
					<%}%>
				</dl>
				<dl>
					<dt><%= LanguageUtil.get(pageContext, "Title") %>:</dt>
					<dd><input type="text" dojoType="dijit.form.TextBox"  onchange="beLazy();" style="width:250px" name="title"  id="friendlyNameField" value="<%= UtilMethods.isSet(folder.getTitle()) ? UtilMethods.escapeDoubleQuotes(folder.getTitle()) : "" %>" /></dd>
				</dl>
				<dl>
					<dt><span class="required"></span> <%= LanguageUtil.get(pageContext, "Name-URL") %>:</dt>
					<dd><input type="text" dojoType="dijit.form.TextBox"   style="width:250px" name="name"  id="titleField" value="<%= UtilMethods.isSet(folder.getName()) ? UtilMethods.escapeDoubleQuotes(folder.getName()) : "" %>" /></dd>
				</dl>
				<dl>
					<dt><%= LanguageUtil.get(pageContext, "Sort-Order") %>:</dt>
					<dd><input type="text" dojoType="dijit.form.TextBox"   style="width:60px" name="sortOrder"  value="<%= UtilMethods.isSet(folder.getSortOrder()+"") ? UtilMethods.escapeDoubleQuotes(folder.getSortOrder()+"") : "" %>" /></dd>
				</dl>
				<dl>
					<dt><%= LanguageUtil.get(pageContext, "Show-on-Menu") %>:</dt>
					<dd><input type="checkbox" dojoType="dijit.form.CheckBox"  name="showOnMenu"  <%if(folder.isShowOnMenu()){ %> checked="checked" <% } %>/></dd>
				</dl>
				<dl>
					<dt><%= LanguageUtil.get(pageContext, "Allowed-File-Extensions") %>:</dt>
					<dd>
						<input type="text" dojoType="dijit.form.TextBox"   style="width:250px" name="filesMasks"  value="<%= UtilMethods.isSet(folder.getFilesMasks()) ? UtilMethods.escapeDoubleQuotes(folder.getFilesMasks()) : "" %>" />
						<div class="hint-text" >(<%= LanguageUtil.get(pageContext, "a-comma-separated-list") %>)</div>
					</dd>
				</dl>
				<dl>
					<dt><span class="required"></span> <%= LanguageUtil.get(pageContext, "Default-File-Structure-Type") %>:</dt>
					<dd>
						<span dojoType="dotcms.dojo.data.StructureReadStore" jsId="fileAssetStructureStore" dojoId="fileAssetStructureStoreDojo" structureType="<%=Structure.STRUCTURE_TYPE_FILEASSET %>" ></span>
						<select id="defaultFileType"
								name="defaultFileType"
								dojoType="dijit.form.FilteringSelect"
								style="width:200px;"
								store="fileAssetStructureStore"
								searchDelay="300"
								pageSize="15"
								autoComplete="false"
								ignoreCase="true"
								labelAttr="name"
								searchAttr="name"
								value="<%=InodeUtils.isSet(folder.getInode())?folder.getDefaultFileType():defaultFileAssetStructure.getInode() %>"
							invalidMessage="<%=LanguageUtil.get(pageContext, "Invalid-option-selected")%>">
						</select>
					</dd>
				</dl>
			</div>

			<!-- START Buttons -->
			<div class="buttonRow" id="editFolderButtonRow">
			<button dojoType="dijit.form.Button" onClick="goBack();" class="dijitButtonFlat" type="button">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
			</button>
			<button dojoType="dijit.form.Button" onClick="save('fm');" type="button">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save" )) %>
			</button>
			</div>
			<!-- END Buttons -->
		</div>
	<!-- END HostVariables -->

	<!-- START permissions -->
	<%
		PermissionAPI perAPI = APILocator.getPermissionAPI();
		boolean canEditAsset = perAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user);
		if (canEditAsset) {
	%>
		<div id="permissionsTab" refreshOnShow="true" preload="true"  dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Permissions") %>" onShow="hideEditButtonsRow()" >
			<%
				request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT, folder);
			%>
			<%@ include file="/html/portlet/ext/common/edit_permissions_tab_inc.jsp" %>
		</div>

		<div id="versions" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "publisher_push_history") %>" onShow="hideEditButtonsRow();">
			<div>
			<%
				request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT, folder);
			%>
			<%@ include file="/html/portlet/ext/common/edit_publishing_status_inc.jsp"%>
			</div>
		</div>
	<%
		}
	%>
	<!-- END permissions -->
	</div>
</div>

</liferay:box>
</html:form>