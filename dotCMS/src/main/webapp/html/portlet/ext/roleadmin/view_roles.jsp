<%@ page import="com.dotmarketing.util.Config" %>

<%@ include file="/html/portlet/ext/roleadmin/init.jsp" %>
<%@ include file="/html/portlet/ext/roleadmin/view_roles_js_inc.jsp" %>
<%@ include file="/html/portlet/ext/roleadmin/view_role_permissions_js_inc.jsp" %>

<%
	String dojoPath = Config.getStringProperty("path.to.dojo");
%>

<style type="text/css">
	@import url("<%=dojoPath%>/dojox/grid/resources/tundraGrid.css?b=3.7.0");
	<%@ include file="/html/portlet/ext/roleadmin/view_roles.css" %>
</style>

<%-- Add Role Dialog --%>

<div id="addRoleDialog" title="<%= LanguageUtil.get(pageContext, "edit-role") %>" dojoType="dijit.Dialog" style="display: none">
	<form id="newRoleForm" dojoType="dijit.form.Form" class="roleForm">
		<dl>
			<dt></dt>
			<dd><ul id="addRoleErrorMessagesList"></ul></dd>
			<dt><%= LanguageUtil.get(pageContext, "role") %>:</dt>
			<dd><input id="roleName" type="text" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" /></dd>
			<dt><%= LanguageUtil.get(pageContext, "Key") %>:</dt>
			<dd><input id="roleKey" type="text" dojoType="dijit.form.ValidationTextBox" /></dd>
			<dt><%= LanguageUtil.get(pageContext, "Parent") %>:</dt>
			<dd id="parentRoleWrapper"><div id="parentRoleDiv"></div>
										<input type="hidden" id="parentRoleValue" value="0" />   </dd>
			<dt><%= LanguageUtil.get(pageContext, "can-grant") %>:</dt>
			<dd>
				<input id="editUsers" type="checkbox" value="true" dojoType="dijit.form.CheckBox" /> <%= LanguageUtil.get(pageContext, "users") %>
				<input id="editPermissions" type="checkbox" value="true" dojoType="dijit.form.CheckBox" /> <%= LanguageUtil.get(pageContext, "Permissions") %>
				<input id="editTabs" type="checkbox" value="true" dojoType="dijit.form.CheckBox" /><%= LanguageUtil.get(pageContext, "Tabs") %>
			</dd>
			<dt><%= LanguageUtil.get(pageContext, "Description") %>:</dt>

			<dd><textarea  dojoType="dijit.form.Textarea" style="width:200px;min-height:2px;max-height: 600px"
        name="roleDescription" required="true" invalidMessage="<%= LanguageUtil.get(pageContext, "Required") %>" id="roleDescription" ></textarea> </dd>

		</dl>

		<div class="buttonRow">
		    <button dojoType="dijit.form.Button" type="button" iconClass="saveIcon" onClick="saveRole()">
		        <%= LanguageUtil.get(pageContext, "Save") %>
		    </button>
		    <button dojoType="dijit.form.Button" type="button" iconClass="cancelIcon" onClick="cancelAddNewRole()">
		        <%= LanguageUtil.get(pageContext, "Cancel") %>
		    </button>
		</div>
	</form>
</div>
<%-- /Add Role Dialog --%>




<%-- New Layout Dialog --%>

<div id="newLayouDialog" title="<%= LanguageUtil.get(pageContext, "edit-tab") %>" dojoType="dijit.Dialog" style="display: none;width:475px;">
	<form id="newLayoutForm" dojoType="dijit.form.Form">
		<ul id="addLayoutErrorMessagesList"></ul>
		<dl>
			<dt><label for="layoutName"><%= LanguageUtil.get(pageContext, "Tool-Group") %>:</label></dt>
			<dd><input id="layoutName" type="text" maxlength="255" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" /></dd>
			<dt><label for="layoutDescription"><%= LanguageUtil.get(pageContext, "Icon") %>:</label></dt>
			<dd style="position:relative;">
				<input id="layoutDescription" type="text" dojoType="dijit.form.TextBox" onchange="updateIcon(this.value)"/>
				<i id="tabIcon" class="fa" style="position:absolute;top: 8px; right:20px;" aria-hidden="true"></i>
				<div class="hint-text"><%= LanguageUtil.get(pageContext, "Icon-hint") %></div>
			</dd>
			<dt><label for="layoutOrder"><%= LanguageUtil.get(pageContext, "order") %>:</label></dt>
			<dd><input id="layoutOrder" type="text" value="0" dojoType="dijit.form.ValidationTextBox" /></dd>
			<dt><label for="portletList"><%= LanguageUtil.get(pageContext, "Tools") %>:</label></dt>
			<dd>
				<select id="portletList"></select>
				<button dojoType="dijit.form.Button" onclick="addPortletToLayoutList()" type="button"><%= LanguageUtil.get(pageContext, "add") %></button>
			</dd>
		</dl>

		<div id="portletsListWrapper" class="view-roles__portlets-list"></div>

		<div class="inputCaption" style="text-align:right">* <%= LanguageUtil.get(pageContext, "drag-a-tool-to-order-it") %></div>

		<div class="buttonRow">
		    <button dojoType="dijit.form.Button" type="button" onClick="saveLayout()">
		        <%= LanguageUtil.get(pageContext, "Save") %>
		    </button>
			<span id="deleteLayoutButtonWrapper">
			    <button dojoType="dijit.form.Button" type="button" onClick="deleteLayout()" class="dijitButtonDanger" iconClass="deleteIcon">
			        <%= LanguageUtil.get(pageContext, "Delete") %>
			    </button>
			</span>
		    <button dojoType="dijit.form.Button" type="button" onClick="cancelEditLayout()" class="dijitButtonFlat">
		        <%= LanguageUtil.get(pageContext, "Cancel") %>
		    </button>
		</div>
	</form>
</div>
<%-- /New Layout Dialog --%>




<%-- Begin Page Layout --%>
<div style="display:none">
	<div class="label"><%= LanguageUtil.get(pageContext, "key") %>:</div> <div id="roleKey" class="title"></div><br/>
	<div class="label"><%= LanguageUtil.get(pageContext, "path") %>:</div> <div id="rolePath" class="title"></div>
</div>

<!-- START Tool Bar -->
<script language="Javascript">
	/**
		focus on search box
	**/
	require([ "dijit/focus", "dojo/dom", "dojo/domReady!" ], function(focusUtil, dom){
		dojo.require('dojox.timing');
		t = new dojox.timing.Timer(500);
		t.onTick = function(){
		  focusUtil.focus(dom.byId("rolesFilter"));
		  t.stop();
		}
		t.start();
	});
</script>

<div data-dojo-type="dijit/form/DropDownButton" data-dojo-props='iconClass:"actionIcon", class:"dijitDropDownActionButton"' style="position:absolute;top:16px;right:16px;z-index:9">
	<span></span>
	<div data-dojo-type="dijit/Menu" class="contentlet-menu-actions">
		<div data-dojo-type="dijit/MenuItem" data-dojo-props="onClick: addNewRole">
			<%= LanguageUtil.get(pageContext, "Add-Role") %>
		</div>
		<div id="editRoleButtonWrapper" data-dojo-type="dijit/MenuItem" data-dojo-props="disabled: true" onClick="editRole()">
			<%= LanguageUtil.get(pageContext, "edit-role") %>
		</div>
		<div id="deleteRoleButtonWrapper" data-dojo-type="dijit/MenuItem" data-dojo-props="disabled: true" onClick="deleteRole()">
			<%= LanguageUtil.get(pageContext, "delete-role") %>
		</div>
	</div>
</div>

<div dojoType="dijit.layout.BorderContainer" design="sidebar" gutters="false" liveSplitters="true" id="borderContainer" class="view-roles">

	<!-- START Left Column -->
	<div dojoType="dijit.layout.ContentPane" splitter="false" region="leading" style="width: 260px" class="portlet-sidebar-wrapper">
		<div class="portlet-sidebar">
			<div id="loadingRolesWrapper"><img src="/html/images/icons/processing.gif" /></div>

			<div class="inline-form view-roles__filter">
				<label for="rolesFilter"><%= LanguageUtil.get(pageContext, "Filter") %>:</label>
				<input dojoType="dijit.form.TextBox" onkeyup="filterRoles()" trim="true" id="rolesFilter" >
				<button dojoType="dijit.form.Button" onclick="clearRolesFilter()" class="dijitButtonFlat" type="button"><%= LanguageUtil.get(pageContext, "Clear") %></button>
			</div>

			<div id="rolesTreeWrapper" style="display: none;" class="view-roles__tree">
				<div id="rolesTree" style=""></div>
				<ul dojoType="dijit.Menu" id="roleTreeMenu" style="display: none;">
					<li dojoType="dijit.MenuItem" id="editRoleMenu"><%= LanguageUtil.get(pageContext, "edit-role") %></li>
					<li dojoType="dijit.MenuItem" id="lockRoleMenu"><%= LanguageUtil.get(pageContext, "lock-role") %></li>
					<li dojoType="dijit.MenuItem" id="unlockRoleMenu"><%= LanguageUtil.get(pageContext, "unlock-role") %></li>
					<li dojoType="dijit.MenuItem" id="deleteRoleMenu"><%= LanguageUtil.get(pageContext, "delete-role") %></li>
				</ul>
			</div>

			<div id="noRolesFound" style="display: none;">
				<%= LanguageUtil.get(pageContext, "no-roles-found") %>
			</div>
		</div>
	</div>
	<!-- END Left Column -->

	<!-- START Right Column -->
	<div dojoType="dijit.layout.ContentPane" splitter="true" region="center">

		<div id="loadingRole" style="display: none;"><img src="/html/images/icons/processing.gif"></div>

		<div id="roleTabs" class="view-roles__tabs">
			<!-- START TABS -->
			<div class="portlet-main">
				<div dojoType="dijit.layout.TabContainer" id="roleTabsContainer" class="view-roles__tabs-container">

					<!-- START Users tab -->
					<div dojoType="dijit.layout.ContentPane" id="usersTab" title="<%= LanguageUtil.get(pageContext, "Users") %>">

						<div class="view-roles__heading">
							<h3 class="nameText" id="displayRoleName1"></h3>
							<div class="inline-form">
								<label for><%= LanguageUtil.get(pageContext, "grant") %>:</label>
								<span dojoType="dotcms.dojo.data.UsersReadStore" jsId="grantUsersStore" includeRoles="false"></span>
								<select id="grantUserSelect" dojoType="dijit.form.FilteringSelect" store="grantUsersStore" searchDelay="300" pageSize="30" labelAttr="name" invalidMessage="<%= LanguageUtil.get(pageContext, "Invalid-option-selected") %>"></select>
								<button dojoType="dijit.form.Button" onclick="grantUser()" type="button" iconClass="plusIcon"><%= LanguageUtil.get(pageContext, "grant") %></button>
							</div>
						</div>

						<div class="inline-form view-roles__user-filter">
							<label for="userRolesFilter"><%= LanguageUtil.get(pageContext, "Filter") %>:</label>
							<input dojoType="dijit.form.TextBox" onkeyup="filterUserRoles()" trim="true" name="userRolesFilter" id="userRolesFilter">
							<button dojoType="dijit.form.Button" onclick="clearUserRolesFilter()" type="button" class="dijitButtonFlat"><%= LanguageUtil.get(pageContext, "Clear") %></button>
						</div>

						<div id="loadingUsersWrapper"><img src="/html/images/icons/processing.gif"></div>

						<div id="usersGridWrapper" class="view-roles__user-grid">
							<div id="usersGrid"></div>
							<div class="clear"></div>
							<div id="noUsers" style="display: none;"><%= LanguageUtil.get(pageContext, "no-users-found") %></div>
						</div>

						<div class="buttonRow">
							<button id="removeUsersButton" dojoType="dijit.form.Button" onclick="removeUsersInRole()" type="button" class="dijitButtonDanger"><%= LanguageUtil.get(pageContext, "remove") %></button>
						</div>

					</div>
					<!-- END Users tab -->

					<!-- START Permissions tab -->
					<div dojoType="dijit.layout.ContentPane" id="permissionsTab" title="<%= LanguageUtil.get(pageContext, "Permissions") %>">
						<%@ include file="/html/portlet/ext/roleadmin/view_role_permissions_inc.jsp" %>
					</div>
					<!-- END Permissions tab -->

					<!-- START CMS Tabs -->
					<div dojoType="dijit.layout.ContentPane" id="cmsTabsTab" title="<%= LanguageUtil.get(pageContext, "CMS-Tabs") %>">

						<div class="view-roles__heading">
							<h3 class="nameText" id="displayRoleName3"></h3>
							<button dojoType="dijit.form.Button" onclick="createNewLayout()" type="button" iconClass="plusIcon">
								<%= LanguageUtil.get(pageContext, "create-custom-tab") %>
							</button>
						</div>

						<div id="loadingRoleLayoutsWrapper"><img src="/html/images/icons/processing.gif"></div>

						<div id="roleLayoutsGridWrapper" style="overflow-y:auto;overflow-x:hidden;" class="view-roles__cms-tabs">
							<div id="roleLayoutsGrid"></div>
						</div>

						<div class="buttonRow">
							<button dojoType="dijit.form.Button" id="saveRoleLayoutsButton" onClick="saveRoleLayouts()" iconClass="saveIcon"><%= LanguageUtil.get(pageContext, "Save") %></button>
						</div>
					</div>
					<!-- END CMS Tabs -->
				</div>
				<!-- END TABS -->
			</div>
		</div>

	</div>
	<!-- END Right Column -->

</div>
