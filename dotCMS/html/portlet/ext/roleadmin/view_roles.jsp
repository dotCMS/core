<%@ page import="com.dotmarketing.util.Config" %>

<%@ include file="/html/portlet/ext/roleadmin/init.jsp" %>
<%@ include file="/html/portlet/ext/roleadmin/view_roles_js_inc.jsp" %>

<style type="text/css">
	<%@ include file="/html/portlet/ext/roleadmin/view_roles.css" %>
	<%@ include file="/html/portlet/ext/roleadmin/view_role_permissions.css" %>
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
			<dd id="parentRoleWrapper"><select id="parentRole"></select></dd>
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
		<dl style="margin-bottom:20px;">
			<dt><%= LanguageUtil.get(pageContext, "Name") %>:</dt>
			<dd><input id="layoutName" type="text" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" /></dd>
			<dt><%= LanguageUtil.get(pageContext, "Description") %>:</dt>
			<dd><input id="layoutDescription" type="text" dojoType="dijit.form.TextBox" /></dd>
			<dt><%= LanguageUtil.get(pageContext, "order") %>:</dt>
			<dd><input id="layoutOrder" type="text" value="0" dojoType="dijit.form.ValidationTextBox" /></dd>
			<dt><%= LanguageUtil.get(pageContext, "tools") %>:</dt>
			<dd>
				<select id="portletList"></select> 
				<button dojoType="dijit.form.Button" onclick="addPortletToLayoutList()" type="button" iconClass="plusIcon"><%= LanguageUtil.get(pageContext, "add") %></button>
			</dd>
		</dl>
		
		<div id="portletsListWrapper"></div>
		
		<div class="inputCaption" style="text-align:right">* <%= LanguageUtil.get(pageContext, "drag-a-tool-to-order-it") %></div>
		
		<div class="buttonRow" style="margin-top:20px;">
		    <button dojoType="dijit.form.Button" type="button" onClick="saveLayout()" iconClass="saveIcon">
		        <%= LanguageUtil.get(pageContext, "Save") %>
		    </button>
			<span id="deleteLayoutButtonWrapper">
			    <button dojoType="dijit.form.Button" type="button" onClick="deleteLayout()" iconClass="deleteIcon">
			        <%= LanguageUtil.get(pageContext, "Delete") %>
			    </button>
			</span>
		    <button dojoType="dijit.form.Button" type="button" onClick="cancelEditLayout()" iconClass="cancelIcon">
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
<div class="buttonBoxLeft">
	<span style="vertical-align:middle;"><%= LanguageUtil.get(pageContext, "Filter") %>:</span>
	<input dojoType="dijit.form.TextBox" onkeyup="filterRoles()" trim="true" id="rolesFilter">
	<button dojoType="dijit.form.Button" onclick="clearRolesFilter()" iconClass="resetIcon" type="button"><%= LanguageUtil.get(pageContext, "Clear") %></button>
</div>

		
<div class="buttonBoxRight">
	<span id="editRoleButtonWrapper" style="display:none;">
		<button id="editRoleButton" dojoType="dijit.form.Button" onclick="editRole()" iconClass="editIcon" type="button"><%= LanguageUtil.get(pageContext, "edit-role") %></button>
	</span>
	<span id="deleteRoleButtonWrapper" style="display:none;">
		<button id="deleteRoleButton" dojoType="dijit.form.Button" onclick="deleteRole()" iconClass="deleteIcon" type="button"><%= LanguageUtil.get(pageContext, "delete-role") %></button>
	</span>
	<button dojoType="dijit.form.Button" type="button" iconClass="plusIcon" onclick="addNewRole()"><%= LanguageUtil.get(pageContext, "Add-Role") %></button>
</div>
<!-- END Tool Bar -->




<div dojoType="dijit.layout.BorderContainer" design="sidebar" gutters="false" liveSplitters="true" id="borderContainer" class="shadowBox headerBox" style="white-space:nowrap;height:400px;">
		
<!-- START Left Column -->
	<div dojoType="dijit.layout.ContentPane" splitter="false" region="leading" style="width: 350px;margin-top:38px;overflow:auto;" class="lineRight">
		<div id="loadingRolesWrapper"><img src="/html/js/lightbox/images/loading.gif"></div>
		
		<div id="rolesTreeWrapper" style="display: none;padding:10px 0 10px 20px;">
			<div id="rolesTree"></div>
	        <ul dojoType="dijit.Menu" id="roleTreeMenu" style="display: none;">
	            <li dojoType="dijit.MenuItem" iconClass="editIcon" id="editRoleMenu"><%= LanguageUtil.get(pageContext, "edit-role") %></li>
	            <li dojoType="dijit.MenuItem" iconClass="lockIcon" id="lockRoleMenu"><%= LanguageUtil.get(pageContext, "lock-role") %></li>
	            <li dojoType="dijit.MenuItem" iconClass="unlockIcon" id="unlockRoleMenu"><%= LanguageUtil.get(pageContext, "unlock-role") %></li>
	            <li dojoType="dijit.MenuItem" iconClass="deleteIcon" id="deleteRoleMenu"><%= LanguageUtil.get(pageContext, "delete-role") %></li>
	        </ul>
		</div>
		
		<div id="noRolesFound" style="display: none;">
        	<%= LanguageUtil.get(pageContext, "no-roles-found") %>
		</div>
	</div>
<!-- END Left Column -->

<!-- START Right Column -->
	<div dojoType="dijit.layout.ContentPane" splitter="true" region="center" style="overflow-y:auto;">
	
		<div id="loadingRole" style="display: none;"><img src="/html/js/lightbox/images/loading.gif"></div>
		
		<div id="roleTabs" style="">
			
		<!-- START TABS -->
			<div dojoType="dijit.layout.TabContainer" id="roleTabsContainer">
				
				<!-- START Users tab -->
				<div style="overflow-y:auto;" dojoType="dijit.layout.ContentPane" id="usersTab" title="<%= LanguageUtil.get(pageContext, "Users") %>"> 
					
					<div class="yui-g nameHeader">
						<div class="yu-u first">
							<div class="nameText" id="displayRoleName1"></div>
						</div>
						<div class="yui-u" style="text-align:right;">
							<span style="vertical-align:middle;"><%= LanguageUtil.get(pageContext, "grant") %>:</span>
							<span dojoType="dotcms.dojo.data.UsersReadStore" jsId="grantUsersStore" includeRoles="false"></span> 
					   		<select id="grantUserSelect" dojoType="dijit.form.FilteringSelect" store="grantUsersStore" searchDelay="300" pageSize="30" labelAttr="name" invalidMessage="<%= LanguageUtil.get(pageContext, "Invalid-option-selected") %>"></select>
							<button dojoType="dijit.form.Button" onclick="grantUser()" type="button" iconClass="plusIcon"><%= LanguageUtil.get(pageContext, "grant") %></button>
						</div>
					</div>
					
					<div class="buttonRow" style=text-align:left;>
						<span style="vertical-align:middle;"><%= LanguageUtil.get(pageContext, "Filter") %>:</span> 
						<input dojoType="dijit.form.TextBox" onkeyup="filterUserRoles()" trim="true" name="userRolesFilter" id="userRolesFilter">
						<button dojoType="dijit.form.Button" onclick="clearUserRolesFilter()" iconClass="resetIcon" type="button"><%= LanguageUtil.get(pageContext, "Clear") %></button>
					</div>
					
					<div id="loadingUsersWrapper"><img src="/html/js/lightbox/images/loading.gif"></div>
					
					<div id="usersGridWrapper">
						<div id="usersGrid"></div>
						<div class="clear"></div>
						<div id="noUsers" style="display: none;"><%= LanguageUtil.get(pageContext, "no-users-found") %></div>
					</div>
					
					<div class="buttonRow">
						<button id="removeUsersButton" dojoType="dijit.form.Button" onclick="removeUsersInRole()" type="button"><%= LanguageUtil.get(pageContext, "remove") %></button>
		       		</div>
	
				</div>
				<!-- END Users tab -->
				
				<!-- START Permissions tab --> 
				<div style="overflow-y:auto;" dojoType="dijit.layout.ContentPane" id="permissionsTab" title="<%= LanguageUtil.get(pageContext, "Permissions") %>">
					
					<div class="yui-g nameHeader">
						<div class="yu-u first">
							<div class="nameText" id="displayRoleName2"></div>
						</div>
						<div class="yui-u" style="text-align:right;">&nbsp;</div>
					</div>
					
					<%@ include file="/html/portlet/ext/roleadmin/view_role_permissions_inc.jsp" %>
					
				</div>
				<!-- END Permissions tab -->
				
				<!-- START CMS Tabs -->
				<div style="overflow-y:auto;" dojoType="dijit.layout.ContentPane" id="cmsTabsTab" title="<%= LanguageUtil.get(pageContext, "CMS-Tabs") %>">
					
					<div class="yui-g nameHeader">
						<div class="yu-u first">
							<div class="nameText" id="displayRoleName3"></div>
						</div>
						<div class="yui-u" style="text-align:right;">
							<button dojoType="dijit.form.Button" onclick="createNewLayout()" type="button" iconClass="plusIcon">
								<%= LanguageUtil.get(pageContext, "create-custom-tab") %>
							</button>
						</div>
					</div>
	
					<div id="loadingRoleLayoutsWrapper"><img src="/html/js/lightbox/images/loading.gif"></div>
					
					<div id="roleLayoutsGridWrapper" style="overflow-y:auto;overflow-x:hidden;">
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
<!-- END Right Column -->

</div>
