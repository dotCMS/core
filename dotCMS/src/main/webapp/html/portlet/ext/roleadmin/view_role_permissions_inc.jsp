<div id="loadingPermissionsAccordion"><img src="/html/images/icons/processing.gif"></div>

<div id="rolePermissionsWrapper">

	<div class="view-roles__heading">
		<h3 class="nameText" id="displayRoleName2"></h3>

		<div id="rolePermissionsHostSelectorWrapper" class="permission__host-selector inline-form">
			<label id="addHostFolderTitle" for="rolePermissionsHostSelector"><%= LanguageUtil.get(pageContext, "add-host-folder") %></label>
			<div id="rolePermissionsHostSelector" dojoType="dotcms.dijit.form.HostFolderFilteringSelect"></div>
			<button dojoType="dijit.form.Button" class="hostSelectorButton" onclick="addHostFolder()" type="button"><%= LanguageUtil.get(pageContext, "add") %></button>
		</div>
	</div>

	<div id="rolePermissionsMsg"></div>
	
 	<div id="permissionsCascadeMsg" class="warningHeader" style="display:none;position: absolute;">
		<div class="warningText" id="permissionsCascadeMsgText"></div>
	</div>
	<div class="clear"></div>
	<div id="rolePermissionsAccordion" class="permission__list">
		<div class="permissionLabelsWrapper">
			<table class="permissionTable">
             	<tr>
             		<td class="permissionType"></td>
             		<td><%= LanguageUtil.get(pageContext, "View") %></td>
             		<td><%= LanguageUtil.get(pageContext, "Add-Children") %></td>
             		<td><%= LanguageUtil.get(pageContext, "Edit") %></td>
             		<td><%= LanguageUtil.get(pageContext, "Publish") %></td>
             		<td><%= LanguageUtil.get(pageContext, "Edit-Permissions") %></td>
					<td></td>
             	</tr>
             </table>
         </div>
		<div id="permissionsAccordionContainer"></div>
	</div>
			
</div>
<div id="savingPermissionsDialog" dojoType="dijit.Dialog" title="<%= LanguageUtil.get(pageContext, "saving-permissions") %>" style="display: none;">
	<div dojoType="dijit.ProgressBar" style="width:200px;text-align:center;" indeterminate="true" jsId="savePermissions" id="savePermissions"></div>
</div>
<script type="text/javascript">
	dojo.addOnLoad(function () {
		dojo.style(dijit.byId('savingPermissionsDialog').closeButtonNode, 'visibility', 'hidden');
	});
</script>