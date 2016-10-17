<%@ include file="/html/portlet/ext/common/edit_permissions_tab_js_inc_ajax.jsp" %>
<!-- START Loading Image Div -->
<div id="loadingPermissionsAccordion"><img src="/html/js/lightbox/images/loading.gif"></div>
<!-- END Loading Image Div -->

<!-- START Alert Message -->
<div id="assetPermissionsMessageWrapper" display="none"></div>
<!-- END Message -->
<div id="assetPermissionsWrapper">
	<div style="padding-left:20px;padding-bottom:10px;white-space: nowrap;font-size:88%;" class="permissionType">
		<%= LanguageUtil.get(pageContext, "Category") %>: <b><span id="permCatName"></span></b>
	</div>
	<!-- START Button Row -->
	<div id="inheritingFrom" class="callOutBox2" style="display: none;">
		<div class="yui-g">
			<div class="yui-u first" style="text-align:left;">
				<b><%= LanguageUtil.get(pageContext, "Inheriting-Permissions-From") %>:</b>
				<span id="inheritingFromSources"></span>
			</div>
			
			<div class="yui-u" style="text-align:right;">
				<span id="resetPermissionActions">
					<span id="permissionIndividuallyButtonWrapper" style="display:none;">
						<button dojoType="dijit.form.Button" onclick="permissionsIndividually()" type="button"><%= LanguageUtil.get(pageContext, "want-to-permission-individually") %></button>
					</span>
					<div id="resetInheritanceMess" style="display: none;"></div>
				</span>
			</div>
		</div>
	</div>
	<div id="permissionsTabFt" class="callOutBox2">
		<table width="100%">
			<tr>
				<td >
					<div style="float:left;white-space:nowrap;margin:0 30px 0 0;">
						<div style="float:left;margin:2px 5px 0 10px;">
							<span class="femaleIcon"></span>
						</div>
						<div style="float:left;">
							<select id="permissionsRoleSelector" dojoType="dotcms.dijit.form.RolesFilteringSelect" excludeRoles="<%= APILocator.getRoleAPI().loadCMSAdminRole().getId()%>"></select>
						</div>
						<div style="float:left;">
							<button dojoType="dijit.form.Button" onclick="addRoleToPermissions();return false;" iconClass="plusIcon"><%= LanguageUtil.get(pageContext, "Add-Role") %></button>
						</div>
						<div class="clear"></div>
					</div>
					
					<div style="float:left;white-space:nowrap;margin:0 30px 0 0;">
						<div style="float:left;margin:2px 5px 0 10px;">
							<span class="userIcon"></span>
						</div>
						<div style="float:left;">
							<div dojoType="dotcms.dojo.data.UsersReadStore" jsId="usersStore" includeRoles="false"></div> 
				   			<select id="permissionsUserSelector" name="permissionsUserSelector" dojoType="dijit.form.FilteringSelect" store="usersStore" searchDelay="300" pageSize="30" labelAttr="name" invalidMessage="<%= LanguageUtil.get(pageContext, "Invalid-option-selected") %>"></select> 
						</div>
						<div style="float:left;">
							<button dojoType="dijit.form.Button" onclick="addUserToPermissions();return false;" iconClass="plusIcon"><%= LanguageUtil.get(pageContext, "Add-User") %></button>
						</div>
						<div class="clear"></div>
					</div>
				</td>
				<td align="right" nowrap="nowrap">
				
					<span id="resetPermissionButtonWrapper" style="display:none;">
						<button dojoType="dijit.form.Button" onclick="resetPermissions()" iconClass="resetIcon"><%= LanguageUtil.get(pageContext, "reset-permissions") %></button>
					</span>
				</td>
			</tr>
		</table>
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
	
	<div class="buttonRow" id="permissionsActions">
		<button dojoType="dijit.form.Button" type="button" class="applyChangesButton" id="applyChangesButton" onClick="applyPermissionChanges('${id}')">
			<%= LanguageUtil.get(pageContext, "Apply-Changes") %>
		</button>
		
		<span id="cascadeChangesChkWrapper" style="display:none; vertical-align:middle;">
			<input type="checkbox" dojoType="dijit.form.CheckBox" id="cascadeChangesCheckbox" /> 
			<%= LanguageUtil.get(pageContext, "Reset-Children-Permissions") %><a href="javascript: ;" id="resetPermissionsHintHook">?</a>
			<span dojoType="dijit.Tooltip" connectId="resetPermissionsHintHook" id="resetPermissionsHint" class="fieldHint"><%= LanguageUtil.get(pageContext, "Reset-Children-Permissions-Hint") %></span>
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