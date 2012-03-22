<%@ include file="/html/portlet/ext/roleadmin/view_role_permissions_js_inc.jsp" %>

<div id="loadingPermissionsAccordion"><img src="/html/js/lightbox/images/loading.gif"></div>
      
<div id="rolePermissionsWrapper">
	
	<div id="rolePermissionsMsg"></div>
	
 	<div id="permissionsCascadeMsg" class="warningHeader " style="display:none;">
		<div class="warningText" id="permissionsCascadeMsgText"></div>
	</div>
	
	<div id="rolePermissionsHostSelectorWrapper" class="buttonRow">
		<div id="addHostFolderTitle"style="vertical-align:middle;"><%= LanguageUtil.get(pageContext, "add-host-folder") %></div> 
		<div id="rolePermissionsHostSelector" dojoType="dotcms.dijit.form.HostFolderFilteringSelect" style="vertical-align:middle;"></div>
		<button dojoType="dijit.form.Button" class="hostSelectorButton" onclick="addHostFolder()" type="button" iconClass="plusIcon"><%= LanguageUtil.get(pageContext, "add") %></button>
	</div>
	<div class="clear"></div>
	<div id="rolePermissionsAccordion">
		
		<div class="permissionLabelsWrapper">
			<table class="permissionTable">
             	<tr>
             		<td class="permissionType"></td>
             		<td><%= LanguageUtil.get(pageContext, "View") %></td>
             		<td><%= LanguageUtil.get(pageContext, "Add-Children") %></td>
             		<td><%= LanguageUtil.get(pageContext, "Edit") %></td>
             		<td><%= LanguageUtil.get(pageContext, "Publish") %></td>
             		<td><%= LanguageUtil.get(pageContext, "Edit-Permissions") %></td>
             		<td><%= LanguageUtil.get(pageContext, "Virtual-Links") %></td>
             	</tr>
             </table>
         </div>
		
		<div id="permissionsAccordionContainer"></div>
		<div class="clear"></div>

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