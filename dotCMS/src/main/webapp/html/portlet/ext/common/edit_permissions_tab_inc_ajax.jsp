<%@ include file="/html/portlet/ext/common/edit_permissions_tab_js_inc_ajax.jsp" %>
<!-- START Loading Image Div -->
<div id="loadingPermissionsAccordion"><img src="/html/js/lightbox/images/loading.gif"></div>
<!-- END Loading Image Div -->

<!-- START Alert Message -->
<div id="assetPermissionsMessageWrapper" display="none"></div>
<!-- END Message -->
<div id="assetPermissionsWrapper">
	<p class="permissionType">
		<%= LanguageUtil.get(pageContext, "Category") %>: <b><span id="permCatName"></span></b>
	</p>
	<!-- START Button Row -->
	<div id="inheritingFrom" class="permissions__bar-user-role" style="display: none;">
		<div class="permissions__bar-user-role-main">
			<b><%= LanguageUtil.get(pageContext, "Inheriting-Permissions-From") %>:</b>&nbsp;
			<span id="inheritingFromSources"></span>
		</div>
		<div class="permissions__bar-user-role-actions">
			<span id="permissionIndividuallyButtonWrapper" style="display:none;">
				<button dojoType="dijit.form.Button" onclick="permissionsIndividually()" type="button"><%= LanguageUtil.get(pageContext, "want-to-permission-individually") %></button>
			</span>
			<div id="resetInheritanceMess" style="display: none;"></div>
		</div>
	</div>
	<div id="permissionsTabFt" class="permissions__bar-user-role">

		<div class="permissions__bar-user-role-main">
			<div class="inline-form">
				<span class="rolesIcon"></span>
				<select id="permissionsRoleSelector" dojoType="dotcms.dijit.form.RolesFilteringSelect" excludeRoles="<%= APILocator.getRoleAPI().loadCMSAdminRole().getId()%>"></select>
				<button dojoType="dijit.form.Button" onclick="addRoleToPermissions();return false;"><%= LanguageUtil.get(pageContext, "Add-Role") %></button>
			</div>
			<div class="inline-form">
				<span class="userIcon"></span>
				<div dojoType="dotcms.dojo.data.UsersReadStore" jsId="usersStore" includeRoles="false" style="margin: 0"></div>
				<select id="permissionsUserSelector" name="permissionsUserSelector" dojoType="dijit.form.FilteringSelect" store="usersStore" searchDelay="300" pageSize="30" labelAttr="name" invalidMessage="<%= LanguageUtil.get(pageContext, "Invalid-option-selected") %>"></select>
				<button dojoType="dijit.form.Button" onclick="addUserToPermissions();return false;"><%= LanguageUtil.get(pageContext, "Add-User") %></button>
			</div>
		</div>
		<div class="permissions__bar-user-role-actions">
			<span id="resetPermissionButtonWrapper" style="display:none;">
				<button dojoType="dijit.form.Button" onclick="resetPermissions()" class="dijitButtonDanger"><%= LanguageUtil.get(pageContext, "reset-permissions") %></button>
			</span>
		</div>
	</div>
<!-- END Button Row -->
<!-- START Permission Rows -->
	<div class="permissionWrapper">
		<table class="permissionTable">
		    <tr>
		        <th class="permissionType">
		        </th>
		        <th><%= LanguageUtil.get(pageContext, "View") %></th>
		        <th><%= LanguageUtil.get(pageContext, "Add-br-children") %></th>
		        <th><%= LanguageUtil.get(pageContext, "Edit") %></th>
		        <th><%= LanguageUtil.get(pageContext, "Publish") %></th>
		        <th><%= LanguageUtil.get(pageContext, "Edit") %><br/><%= LanguageUtil.get(pageContext, "Permissions") %></th>
		        <th><%= LanguageUtil.get(pageContext, "Virtual-br-Links") %></th>
		    </tr>
		</table>
	</div>

	<div id="permissionsAccordionContainer" class="permissionWrapper"></div>
	
	<div id="noPermissionsMessage" class="noResultsMessage" style="display: none"><%= LanguageUtil.get(pageContext, "No-permissions-message") %></div>
	
	<div class="clear"></div>
	
	<div class="permissions__button-row" id="permissionsActions">
		<button dojoType="dijit.form.Button" type="button" class="applyChangesButton" id="applyChangesButton" onClick="applyPermissionChanges('${id}')">
			<%= LanguageUtil.get(pageContext, "Apply-Changes") %>
		</button>
		
		<span id="cascadeChangesChkWrapper" style="display:none;">
			<div class="checkbox">
				<input type="checkbox" dojoType="dijit.form.CheckBox" id="cascadeChangesCheckbox" />
				<label for="cascadeChangesCheckbox">
					<%= LanguageUtil.get(pageContext, "Reset-Children-Permissions") %>&nbsp;<a href="javascript: ;" id="resetPermissionsHintHook">?</a>
				</label>
				<span dojoType="dijit.Tooltip" connectId="resetPermissionsHintHook" id="resetPermissionsHint" class="fieldHint"><%= LanguageUtil.get(pageContext, "Reset-Children-Permissions-Hint") %></span>
			</div>
		</span>
	</div>
		
	
</div>

<!-- START Saving permissions dialog -->
	<div id="savingPermissionsDialog" dojoType="dijit.Dialog" title="<%= LanguageUtil.get(pageContext, "saving-permissions") %>" style="display: none;">
		<div dojoType="dijit.ProgressBar" style="width:200px;text-align:center;" indeterminate="true"></div>
	</div>
<!-- END Saving permissions dialog -->

<!-- START Confirm Save permissions  change dialog -->	
     <div dojoType="dijit.Dialog" id="applyPermissionsChangesDialog" title='<%=LanguageUtil.get(pageContext, "Permissions-Confirmation") %>' style="display: none">
     	 <%=LanguageUtil.get(pageContext, "permissions-changes-confirmation") %>
		 <div class="buttonRow">
     		<button dojoType="dijit.form.Button" onClick="dijit.byId('applyPermissionsChangesDialog').hide(); applyPermissionChanges('${id}'); " type="button"><%= LanguageUtil.get(pageContext, "Yes") %></button>
			<button dojoType="dijit.form.Button" onClick="dijit.byId('applyPermissionsChangesDialog').hide();" type="button"><%= LanguageUtil.get(pageContext, "No") %></button>	 
     	 </div>				
     </div>
<!-- END Confirm Save permissions  change dialog -->