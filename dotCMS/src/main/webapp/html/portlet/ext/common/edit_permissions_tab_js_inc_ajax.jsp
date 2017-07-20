<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="com.dotmarketing.portlets.folders.model.Folder"%>
<%@page import="com.dotmarketing.portlets.containers.model.Container"%>
<%@page import="com.dotmarketing.portlets.templates.model.Template"%>
<%@page import="com.dotmarketing.portlets.links.model.Link"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@ page import="com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage" %>
<%@ page import="com.dotmarketing.portlets.rules.model.Rule" %>
<%@ page import="com.dotmarketing.portlets.templates.design.bean.TemplateLayout" %>

<script type="text/javascript" src="/dwr/interface/PermissionAjax.js"></script>
<script type="text/javascript" src="/html/js/dotcms/dijit/form/RolesFilteringSelect.js"></script>
<script type="text/javascript"><!--

	dojo.require('dijit.layout.AccordionContainer');
	dojo.require('dijit.layout.ContentPane');
	dojo.require('dotcms.dijit.form.RolesFilteringSelect');
	dojo.require('dotcms.dojo.data.UsersReadStore');

	//Global variables
<%-- 	var assetId = '<%= asset.getPermissionId() %>'; --%>
<%-- 	var assetType = '<%= ((asset instanceof Contentlet) && ((Contentlet)asset).getStructureInode().equals(hostStrucuture.getInode()))?Host.class.getName():asset.getClass().getName() %>'; --%>
	<%	Contentlet contentletAux = ((Contentlet)request.getAttribute(com.dotmarketing.util.WebKeys.CONTENTLET_EDIT)); %>

	var languageId = '<%= ((UtilMethods.isSet(contentletAux) && UtilMethods.isSet(contentletAux.getLanguageId())) ? contentletAux.getLanguageId() : "") %>';
	var assetId;
	var assetType;
	var isParentPermissionable;
	var isFolder;
	var isHost;
	var doesUserHavePermissionsToEdit;
	var isNewAsset;
	var contentTemplateString;

    var accordionContainer;

    var currentPermissions;
	var inheritingPermissions = false;
	var changesMadeToPermissions = false;

	//I18n messages
	var roleAlreadyInListMesg = '<%= LanguageUtil.get(pageContext, "role-already-in-list") %>';
	var globalPath = '<%= LanguageUtil.get(pageContext, "global-permission-path") %>';
	var permissionsSavedMsg = '<%= LanguageUtil.get(pageContext, "permissions-saved") %>'
    var noPermissionsSavedMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "no-permissions-saved")) %>'
	var removeIndividualPermissionConfirm = '<%= LanguageUtil.get(pageContext, "remove-individual-permissions-confirm") %>'
	var newAssetPermissionsMsg = '<%= LanguageUtil.get(pageContext, "new-asset-permissions-message") %>'
	var noPermissionsMsg = '<%= LanguageUtil.get(pageContext, "no-permissions-message") %>'
	var roleLockedForPermissions = '<%= LanguageUtil.get(pageContext, "role-locked-to-permissions") %>'
	var roleNotRequiredPermissions = '<%= LanguageUtil.get(pageContext, "role-pageContext-lacks-permission") %>'
	var cascadePermissionsConfirm = '<%= LanguageUtil.get(pageContext, "cascade-permissions-confirm-msg") %>'
	var hostsWillInheritMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Hosts")) %>';
	var foldersWillInheritMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Folders")) %>';
	var containersWillInheritMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Containers")) %>';
	var templatesWillInheritMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Templates")) %>';
	var templateLayoutsWillInheritMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Template-Layouts")) %>';
	var pagesWillInheritMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Pages")) %>';
	var linksWillInheritMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Links")) %>';
	var contentWillInheritMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Content-Files")) %>';
	var permissionsOnChildrenMsg1 = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Permissions-on-Children1")) %>';
	var permissionsOnChildrenMsg2 = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Permissions-on-Children2")) %>';
	var structureWillInheritMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Structure")) %>';
	var rulesWillInheritMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Rules")) %>';

	//HTML Templates
	var inheritedSourcesTemplate = '<span class="${icon}"></span> ${path}';
	var titleTemplateString = dojo._getText('/html/portlet/ext/common/edit_permissions_accordion_title.html');


	//Global constants
	var viewPermission = <%= PermissionAPI.PERMISSION_READ %>;
	var editPermission = <%= PermissionAPI.PERMISSION_WRITE %>;
	var publishPermission = <%= PermissionAPI.PERMISSION_PUBLISH %>;
	var editPermissionsPermission = <%= PermissionAPI.PERMISSION_EDIT_PERMISSIONS %>;
	var addChildrenPermission = <%= PermissionAPI.PERMISSION_CAN_ADD_CHILDREN %>;

	var hostClassName = '<%= Host.class.getCanonicalName() %>'
	var folderClassName = '<%= Folder.class.getCanonicalName() %>'
	var containerClassName = '<%= Container.class.getCanonicalName() %>'
	var templateClassName = '<%= Template.class.getCanonicalName() %>'
	var templateLayoutClassName = '<%= TemplateLayout.class.getCanonicalName() %>'
	var pageClassName = '<%= IHTMLPage.class.getCanonicalName() %>'
	var linkClassName = '<%= Link.class.getCanonicalName() %>'
	var contentClassName = '<%= Contentlet.class.getCanonicalName() %>';
	var structureClassName = '<%= Structure.class.getCanonicalName() %>';
	var rulesClassName = '<%= Rule.class.getCanonicalName() %>';

	var dijits = [];

	function loadPermissions () {

		if(isNewAsset) {
			dojo.style('loadingPermissionsAccordion', { display: 'none' });
			dojo.style('assetPermissionsWrapper', { display: 'none' });
			dojo.style('assetPermissionsMessageWrapper', { display: '' });
			dojo.byId('assetPermissionsMessageWrapper').innerHTML = newAssetPermissionsMsg;
			alert(newAssetPermissionsMsg);
			return;
		}

		dojo.style('loadingPermissionsAccordion', { display: '' });
		dojo.style('assetPermissionsWrapper', { display: 'none' });

		if(dijit.byId('permissionsAccordionContainer')) {
			//Manually destroying widgets since Accordion destroy recursive does not take care of all
			var container = dijit.byId('permissionsAccordionContainer');
			try{
			    container.destroyDescendants(true);
			}catch(ex){
				console.log('loadPermissions: error removing permissions container: ' + ex);
			}
			try{
				container.destroyRecursive(true);
			}catch(ex){
				console.log('loadPermissions: error removing permissions container: ' + ex);
			}
		}
		//http://jira.dotmarketing.net/browse/DOTCMS-6214
		destroyChecks();
		PermissionAjax.getAssetPermissions(assetId, languageId, { callback: renderPermissionsCallback, scope: this });

		if(isParentPermissionable)
			dojo.style('cascadeChangesChkWrapper', { display: '' });


	}

	function renderPermissionsCallback(permissions) {

		if(!doesUserHavePermissionsToEdit) {
			dojo.style('assetPermissionsMessageWrapper', { display: '' });
			dojo.byId('assetPermissionsMessageWrapper').innerHTML = noPermissionsMsg;
		}

		currentPermissions = permissions;

		setupInheritanceOptions();

		accordionContainer = new dijit.layout.AccordionContainer({
			layout: function () {
				// Implement _LayoutWidget.layout() virtual method.
				// Set the height of the open pane based on what room remains.

				var openPane = this.selectedChildWidget;

				// get cumulative height of all the title bars
				var totalCollapsedHeight = 0;
				dojo.forEach(this.getChildren(), function(child){
					totalCollapsedHeight += child._buttonWidget.getTitleHeight();
					if((!isFolder && !isHost) || (inheritingPermissions)) {
						dojo.style(child.containerNode, { padding: '0' });
					}
				});
				var mySize = this._contentBox;
				if((isFolder || isHost) && !inheritingPermissions) {
					this._verticalSpace = 200;
				} else {
					this._verticalSpace = 0;
				}

				// Memo size to make displayed child
				this._containerContentBox = {
					h: this._verticalSpace,
					w: mySize.w
				};

				if(openPane){
					openPane.resize(this._containerContentBox);
				}
			}
        },
        "permissionsAccordionContainer");

		dojo.forEach(permissions, function(role) {
			addTemplatePermissionOptions(role, role.permissions);
			addPermissionsAccordionPane(role);
		})

		try{
	    accordionContainer.startup();
		}catch(ex){}

		dojo.forEach(permissions, function(role) {
			try{
		    initPermissionsAccordionPane(role);
			}catch(ex){}
		})

		dojo.style('loadingPermissionsAccordion', { display: 'none' });
		dojo.style('assetPermissionsWrapper', { display: '' });

		if(!doesUserHavePermissionsToEdit) {
			dojo.style('permissionsTabFt', { display: 'none' });
			dojo.style('resetPermissionActions', { display: 'none' });
		}

		if(permissions.length == 0) {
			dojo.style('inheritingFrom', { display: 'none' })
			dojo.style('noPermissionsMessage', { display: '' })
			dojo.style('permissionsAccordionContainer', { display: 'none' })
			dojo.style('permissionsTabFt', { display: '' });
		}

		if(inheritingPermissions && permissions.length > 0) {
			dojo.style('permissionsActions', { display: 'none' });
		}

		adjustAccordionHeigth();
		if(!inheritingPermissions && (isHost || isFolder)){
			dojo.query(".accordionEntry").forEach(function(node, index, arr){
				node.className = "permissionTable";
			 });




		}
	}

	function setupInheritanceOptions () {
		dojo.byId('inheritingFromSources').innerHTML = '';
		if(allInheritedPermissions(currentPermissions)) {
			var sources = listOfInheritedSources();
			dojo.forEach(sources, function (source) {
				source.icon = 'shimIcon';
				if(source.type == 'host') {
					source.icon = 'publishIcon'
				} else if(source.type == 'structure') {
					source.icon = 'structureIcon'
				} else if(source.type == 'folder') {
					source.icon = 'folderSelectedIcon'
				} else if(source.type == 'category') {
					source.icon = 'fixIcon'
				}
				if(source.path == 'SYSTEM_HOST') source.path = globalPath;

				var html = dojo.string.substitute(inheritedSourcesTemplate, source);
				dojo.place(html, 'inheritingFromSources', 'last');
			});
			dojo.style('permissionsTabFt', { display: 'none' });
			dojo.style('inheritingFrom', { display: '' });
			dojo.style('permissionIndividuallyButtonWrapper', { display: '' });
			dojo.style('resetPermissionButtonWrapper', { display: 'none' });
			inheritingPermissions = true;

		} else {
			dojo.style('permissionsTabFt', { display: '' });
			dojo.style('inheritingFrom', { display: 'none' });
			dojo.style('permissionIndividuallyButtonWrapper', { display: 'none' });
			dojo.style('resetPermissionButtonWrapper', { display: '' });
			inheritingPermissions = false;
		}


	}


	function allInheritedPermissions(permissions) {
		for(var i = 0; i < permissions.length; i++) {
			var permission = permissions[i];
			if(!permission.inherited)
				return false;
		}
		return true;
	}

	function listOfInheritedSources() {
		var sourcesLoaded = [];
		var sources = [];
		for(var i = 0; i < currentPermissions.length; i++) {
			var role = currentPermissions[i];
			if(role.inherited) {
				if(sourcesLoaded[role.inheritedFromId] == null) {
					sources.push({ path: role.inheritedFromPath, type: role.inheritedFromType });
					sourcesLoaded[role.inheritedFromId] = role.id;
				}
			}
		}
		return sources;
	}

	function adjustAccordionHeigth() {
		var container = dijit.byId('permissionsAccordionContainer');
		container.resize();

	}

	function addPermissionsAccordionPane(role) {

		dijit.registry.remove();

		var title = dojo.string.substitute(titleTemplateString, role);
		var content = dojo.string.substitute(contentTemplateString, role);

		var contentPane = new dijit.layout.ContentPane({
	        title: title,
	        content: content,
			id: 'permissionsAccordionPane-' + role.id
	    })
		accordionContainer.addChild(contentPane)

	}

	function initPermissionsAccordionPane(role) {
		dijits.push(dojo.parser.parse(dojo.byId('permissionTitleTableWrapper-' + role.id)));
	}

	function applyPermissionChanges () {

		// check if there is changes
		if(dijit.byId('cascadeChangesCheckbox').attr('value')==false) {
			var changes=false;
			for(var i = 0; i < currentPermissions.length; i++) {
	            var role = currentPermissions[i];
	            changes=changes || thereIsPermissionCheckChanges(role);
			}
			if(!changes) {
	            showDotCMSSystemMessage(noPermissionsSavedMsg);
	            return;
	        }
		}

		changesMadeToPermissions =false;
		var cascade = false;
		if(isParentPermissionable) {
			cascade = dijit.byId('cascadeChangesCheckbox').attr('value') == 'on';
			dijit.byId('cascadeChangesCheckbox').attr('value', false);
		}

		if(cascade && !confirm(cascadePermissionsConfirm))
			return;

		var permissionsToSubmit = [];
		for(var i = 0; i < currentPermissions.length; i++) {
			var role = currentPermissions[i];
			var rolePermission = { roleId: role.id }
			rolePermission.individualPermission = retrievePermissionChecks(role.id);
			if(isFolder || isHost) {
				rolePermission.foldersPermission = retrievePermissionChecks(role.id, 'folders');
				rolePermission.containersPermission = retrievePermissionChecks(role.id, 'containers');
				rolePermission.templatesPermission = retrievePermissionChecks(role.id, 'templates');
				rolePermission.templateLayoutsPermission = retrievePermissionChecks(role.id, 'template-layouts');
				rolePermission.pagesPermission = retrievePermissionChecks(role.id, 'pages');
				rolePermission.linksPermission = retrievePermissionChecks(role.id, 'links');
				rolePermission.contentPermission = retrievePermissionChecks(role.id, 'content');
				rolePermission.structurePermission = retrievePermissionChecks(role.id, 'structure');
				rolePermission.rulesPermission = retrievePermissionChecks(role.id, 'rules');
			}
			permissionsToSubmit.push(rolePermission)
		}

		if(window.scrollTo)
			window.scrollTo(0,0);	// To show lightbox effect(IE) and save content errors.
		dijit.byId('savingPermissionsDialog').show();

		PermissionAjax.saveAssetPermissions(assetId, languageId, permissionsToSubmit, cascade, dojo.hitch(this, savePermissionsCallback, assetId, permissionsToSubmit, cascade));

	}

	function savePermissionsCallback(assetId, permissionsToSubmit, cascade) {
		dijit.byId('savingPermissionsDialog').hide();

		showDotCMSSystemMessage(permissionsSavedMsg);

		loadPermissions();
	}

	function removePermissionsRoleAccordion(role) {

		destroyCheckboxes(getPermissionCheckboxDijits(null, role.roleId))
		destroyCheckboxes(getPermissionCheckboxDijits('folders', role.roleId))
		destroyCheckboxes(getPermissionCheckboxDijits('containers', role.roleId))
		destroyCheckboxes(getPermissionCheckboxDijits('templates', role.roleId))
		destroyCheckboxes(getPermissionCheckboxDijits('template-layouts', role.roleId))
		destroyCheckboxes(getPermissionCheckboxDijits('pages', role.roleId))
		destroyCheckboxes(getPermissionCheckboxDijits('links', role.roleId))
		destroyCheckboxes(getPermissionCheckboxDijits('content', role.roleId))
		destroyCheckboxes(getPermissionCheckboxDijits('structure', role.roleId))
		destroyCheckboxes(getPermissionCheckboxDijits('rules', role.roleId))

		var containerPane = dijit.byId('permissionsAccordionPane-' + role.roleId);
		accordionContainer.removeChild(containerPane);
		containerPane.destroy();

	}

	function destroyCheckboxes(checkboxesList) {
		if(checkboxesList.addChildrenPermissionCheckbox) checkboxesList.addChildrenPermissionCheckbox.destroy();
		if(checkboxesList.editPermissionCheckbox) checkboxesList.editPermissionCheckbox.destroy();
		if(checkboxesList.editPermissionsPermissionCheckbox) checkboxesList.editPermissionsPermissionCheckbox.destroy();
		if(checkboxesList.publishPermissionCheckbox) checkboxesList.publishPermissionCheckbox.destroy();
		if(checkboxesList.viewPermissionCheckbox) checkboxesList.viewPermissionCheckbox.destroy();
	}

	function checkRolePermissionsRemoved(permissionSet) {

		var rolesRemoved = new Array();
		for(var i = 0; i < permissionSet.length; i++) {

			var rolePermission = permissionSet[i];
			if((rolePermission.individualPermission |
					rolePermission.foldersPermission |
					rolePermission.containersPermission |
					rolePermission.templatesPermission |
					rolePermission.templateLayoutsPermission |
					rolePermission.pagesPermission |
					rolePermission.linksPermission |
					rolePermission.contentPermission |
					rolePermission.structurePermission |
					rolePermission.rulesPermission) == 0) {
				rolesRemoved.push(rolePermission);
			}

		}
		return rolesRemoved;
	}

	function retrievePermissionChecks(id, type) {

		var permission = 0;

		var prefix = '';
		if(type) prefix = type + "-";

		if(isPermissionChecked(prefix + 'view-permission-' + id))
			permission = permission | viewPermission;
		if(isPermissionChecked(prefix + 'add-children-permission-' + id))
			permission = permission | addChildrenPermission;
		if(isPermissionChecked(prefix + 'edit-permission-' + id))
			permission = permission | editPermission;
		if(isPermissionChecked(prefix + 'publish-permission-' + id))
			permission = permission | publishPermission;
		if(isPermissionChecked(prefix + 'edit-permissions-permission-' + id))
			permission = permission | editPermissionsPermission;

		return permission;

	}

	function isPermissionChecked(id){
		if (dijit.byId(id) && dijit.byId(id).attr('value') == 'on'){
			// Check if the checkbox element is visible (https://github.com/dotCMS/core/issues/9659)
			var elem = document.getElementById(id);
			return !!( elem.offsetWidth || elem.offsetHeight || elem.getClientRects().length );
		} else {
			return false;
		}
	}
	
	function thereIsPermissionCheckChanges(item) {
		var id=item.id;

        // check individual permission changes
        if(dijit.byId('view-permission-' + id) &&
                ((dijit.byId('view-permission-' + id).attr('value') == 'on' && item.viewPermissionChecked=="") ||
                 (dijit.byId('view-permission-' + id).attr('value') == false && item.viewPermissionChecked!="")))
            return true;
        if(dijit.byId('add-children-permission-' + id) &&
                ((dijit.byId('add-children-permission-' + id).attr('value') == 'on' && item.addChildrenPermissionChecked=="") ||
                 (dijit.byId('add-children-permission-' + id).attr('value') == false && item.addChildrenPermissionChecked!="")))
            return true;
        if(dijit.byId('edit-permission-' + id) &&
                ((dijit.byId('edit-permission-' + id).attr('value') == 'on' && item.editPermissionChecked=="") ||
                 (dijit.byId('edit-permission-' + id).attr('value') == false && item.editPermissionChecked!="")))
            return true;
        if(dijit.byId('edit-permission-' + id) &&
                ((dijit.byId('edit-permission-' + id).attr('value') == 'on' && item.editPermissionChecked=="") ||
                 (dijit.byId('edit-permission-' + id).attr('value') == false && item.editPermissionChecked!="")))
            return true;
        if(dijit.byId('publish-permission-' + id) &&
                ((dijit.byId('publish-permission-' + id).attr('value') == 'on' && item.publishPermissionChecked=="") ||
                 (dijit.byId('publish-permission-' + id).attr('value') == false && item.publishPermissionChecked!="")))
            return true;
        if(dijit.byId('edit-permissions-permission-' + id) &&
                ((dijit.byId('edit-permissions-permission-' + id).attr('value') == 'on' && item.editPermissionsPermissionChecked=="") ||
                 (dijit.byId('edit-permissions-permission-' + id).attr('value') == false && item.editPermissionsPermissionChecked!="")))
            return true;

        var changedType=function(item,type) {
            if(dijit.byId(type+'-view-permission-' + id) &&
                    ((dijit.byId(type+'-view-permission-' + id).attr('value') == 'on' && item[type+'ViewPermissionChecked']=="") ||
                     (dijit.byId(type+'-view-permission-' + id).attr('value') == false && item[type+'ViewPermissionChecked']!="")))
                return true;
            if(dijit.byId(type+'-add-children-permission-' + id) &&
                    ((dijit.byId(type+'-add-children-permission-' + id).attr('value') == 'on' && item[type+'AddChildrenPermissionChecked']=="") ||
                     (dijit.byId(type+'-add-children-permission-' + id).attr('value') == false && item[type+'AddChildrenPermissionChecked']!="")))
                return true;
            if(dijit.byId(type+'-edit-permission-' + id) &&
                    ((dijit.byId(type+'-edit-permission-' + id).attr('value') == 'on' && item[type+'EditPermissionChecked']=="") ||
                     (dijit.byId(type+'-edit-permission-' + id).attr('value') == false && item[type+'EditPermissionChecked']!="")))
                return true;
            if(dijit.byId(type+'-edit-permission-' + id) &&
                    ((dijit.byId(type+'-edit-permission-' + id).attr('value') == 'on' && item[type+'EditPermissionChecked']=="") ||
                     (dijit.byId(type+'-edit-permission-' + id).attr('value') == false && item[type+'EditPermissionChecked']!="")))
                return true;
            if(dijit.byId(type+'-publish-permission-' + id) &&
                    ((dijit.byId(type+'-publish-permission-' + id).attr('value') == 'on' && item[type+'PublishPermissionChecked']=="") ||
                     (dijit.byId(type+'-publish-permission-' + id).attr('value') == false && item[type+'PublishPermissionChecked']!="")))
                return true;
            if(dijit.byId(type+'-edit-permissions-permission-' + id) &&
                    ((dijit.byId(type+'-edit-permissions-permission-' + id).attr('value') == 'on' && item[type+'EditPermissionsPermissionChecked']=="") ||
                     (dijit.byId(type+'-edit-permissions-permission-' + id).attr('value') == false && item[type+'EditPermissionsPermissionChecked']!="")))
                return true;
        }

        types=['hosts','folders','containers','templates', 'template-layouts','pages','links','structure','content','categories', 'rules'];

        for(var i=0;i<types.length;i++)
            if(changedType(item,types[i]))
                return true;

        return false;
    }

	function viewPermissionChanged (type, id) {
       changesMadeToPermissions=true;
		var checkboxes = getPermissionCheckboxDijits(type, id);

		if(checkboxes.viewPermissionCheckbox.attr('value') != 'on') {
			if(checkboxes.addChildrenPermissionCheckbox) checkboxes.addChildrenPermissionCheckbox.attr('value', false);
			if(checkboxes.editPermissionCheckbox) checkboxes.editPermissionCheckbox.attr('value', false);
			if(checkboxes.publishPermissionCheckbox) checkboxes.publishPermissionCheckbox.attr('value', false);
			if(checkboxes.editPermissionsPermissionCheckbox) checkboxes.editPermissionsPermissionCheckbox.attr('value', false);
		}

	}

	function addChildrenPermissionChanged (type, id) {
		changesMadeToPermissions=true;
		var checkboxes = getPermissionCheckboxDijits(type, id);

		if(checkboxes.addChildrenPermissionCheckbox.attr('value') == 'on') {
			if(checkboxes.viewPermissionCheckbox) checkboxes.viewPermissionCheckbox.attr('value', 'on');
		}
		else {
			if(checkboxes.editPermissionCheckbox) checkboxes.editPermissionCheckbox.attr('value', false);
			if(checkboxes.publishPermissionCheckbox) checkboxes.publishPermissionCheckbox.attr('value', false);
			if(checkboxes.editPermissionsPermissionCheckbox) checkboxes.editPermissionsPermissionCheckbox.attr('value', false);
		}
	}

	function editPermissionChanged (type, id) {
		changesMadeToPermissions=true;
		var checkboxes = getPermissionCheckboxDijits(type, id);

		if(checkboxes.editPermissionCheckbox.attr('value') == 'on') {
			if(checkboxes.viewPermissionCheckbox) checkboxes.viewPermissionCheckbox.attr('value', 'on');
			if(checkboxes.addChildrenPermissionCheckbox) checkboxes.addChildrenPermissionCheckbox.attr('value', 'on');
		} else {
			if(checkboxes.publishPermissionCheckbox) checkboxes.publishPermissionCheckbox.attr('value', false);
			if(checkboxes.editPermissionsPermissionCheckbox) checkboxes.editPermissionsPermissionCheckbox.attr('value', false);
		}

	}

	function publishPermissionChanged (type, id) {
		changesMadeToPermissions=true;
		var checkboxes = getPermissionCheckboxDijits(type, id);

		if(checkboxes.publishPermissionCheckbox.attr('value') == 'on') {
			if(checkboxes.viewPermissionCheckbox) checkboxes.viewPermissionCheckbox.attr('value', 'on');
			if(checkboxes.addChildrenPermissionCheckbox) checkboxes.addChildrenPermissionCheckbox.attr('value', 'on');
			if(checkboxes.editPermissionCheckbox) checkboxes.editPermissionCheckbox.attr('value', 'on');
		} else {
			if(checkboxes.editPermissionsPermissionCheckbox) checkboxes.editPermissionsPermissionCheckbox.attr('value', false);
		}
	}

	function editPermissionsPermissionChanged (type, id) {
		changesMadeToPermissions=true;
		var checkboxes = getPermissionCheckboxDijits(type, id);

		if(checkboxes.editPermissionsPermissionCheckbox.attr('value') == 'on') {
			if(checkboxes.viewPermissionCheckbox) checkboxes.viewPermissionCheckbox.attr('value', 'on');
			if(checkboxes.addChildrenPermissionCheckbox) checkboxes.addChildrenPermissionCheckbox.attr('value', 'on');
			if(checkboxes.editPermissionCheckbox) checkboxes.editPermissionCheckbox.attr('value', 'on');
			if(checkboxes.publishPermissionCheckbox) checkboxes.publishPermissionCheckbox.attr('value', 'on');
		}

	}

	function addRoleToPermissions(role) {

		dojo.style('permissionsAccordionContainer', { display: '' })

		if(!role)
			role = dijit.byId('permissionsRoleSelector').attr('selectedItem');

		role.id = norm(role.id);
		role.DBFQN = norm(role.DBFQN);
		role.FQN = norm(role.FQN);
		role.description = norm(role.description);
		role.editLayouts = norm(role.editLayouts);
		role.editPermissions = norm(role.editPermissions);
		role.editUsers = norm(role.editUsers);
		role.locked = norm(role.locked);
		role.name = norm(role.name);
		role.roleKey = norm(role.roleKey);
		role.system = norm(role.system);

		if(!role.editPermissions) {
			alert(roleNotRequiredPermissions);
			return;
		}
		if(role.locked) {
			alert(roleLockedForPermissions);
			return;
		}

		if(findRole(role.id, currentPermissions)) {
			alert(roleAlreadyInListMesg);
			return;
		}

		role.permissions = [];
		currentPermissions.push(role);
		addTemplatePermissionOptions(role, role.permissions);
		addPermissionsAccordionPane(role);
	    initPermissionsAccordionPane(role);
		adjustAccordionHeigth();
		selectAccordionPane(role.id);

		dojo.query(".accordionEntry").forEach(function(node, index, arr){
			node.className = "permissionTable";
		 });
	}

	function addUserToPermissions() {
		if(dijit.byId('permissionsUserSelector').attr('value') == '') {
			return;
		}
		var userId = dijit.byId('permissionsUserSelector').attr('value').split('-')[1];
		RoleAjax.getUserRole(userId, addUserToPermissionCallback);
	}

	function addUserToPermissionCallback(role) {
		addRoleToPermissions(role);

	}

	function selectAccordionPane(id) {
		accordionContainer.selectChild(dijit.byId('permissionsAccordionPane-' + id));
	}

	function permissionsIndividually () {

	   if(assetType == 'com.dotmarketing.portlets.folders.model.Folder' ||
			   assetType == 'com.dotmarketing.beans.Host') {
			dijit.byId('savingPermissionsDialog').show();
			changesMadeToPermissions=false;
			PermissionAjax.permissionIndividually(assetId, languageId, permissionIndividuallyCallback);

	   }else{

		dojo.forEach(currentPermissions, function (role) {
			if(!role.editPermissions) {
				return;
			}
			if(role.locked) {
				return;
			}
			enableCheckboxes(getPermissionCheckboxDijits(null, role.id))
			enableCheckboxes(getPermissionCheckboxDijits('folders', role.id))
			enableCheckboxes(getPermissionCheckboxDijits('containers', role.id))
			enableCheckboxes(getPermissionCheckboxDijits('templates', role.id))
			enableCheckboxes(getPermissionCheckboxDijits('template-layouts', role.id))
			enableCheckboxes(getPermissionCheckboxDijits('pages', role.id))
			enableCheckboxes(getPermissionCheckboxDijits('links', role.id))
			enableCheckboxes(getPermissionCheckboxDijits('content', role.id))
			enableCheckboxes(getPermissionCheckboxDijits('structure', role.id))
			enableCheckboxes(getPermissionCheckboxDijits('rules', role.id))
		});
		dojo.style('permissionsTabFt', { display: '' });
		dojo.style('inheritingFrom', { display: 'none' });
		dojo.style('permissionIndividuallyButtonWrapper', { display: 'none' });
		dojo.style('resetPermissionButtonWrapper', { display: '' });
		dojo.style('permissionsActions', { display: '' });
		dojo.query(".accordionEntry").forEach(function(node, index, arr){
			node.className = "permissionTable";
		 });


		var cont = dijit.byId('permissionsAccordionContainer');

		cont.layout=function () {
			// Implement _LayoutWidget.layout() virtual method.
			// Set the height of the open pane based on what room remains.

			var openPane = this.selectedChildWidget;

			// get cumulative height of all the title bars
			var totalCollapsedHeight = 0;
			dojo.forEach(this.getChildren(), function(child){
				totalCollapsedHeight += child._buttonWidget.getTitleHeight();
				if((!isFolder && !isHost)) {
					dojo.style(child.containerNode, { padding: '0' });
				}
			});
			var mySize = this._contentBox;
			if(isFolder || isHost) {
				this._verticalSpace = 200;
			} else {
				this._verticalSpace = 0;
			}

			// Memo size to make displayed child
			this._containerContentBox = {
				h: this._verticalSpace,
				w: mySize.w
			};

			if(openPane){
				openPane.resize(this._containerContentBox);
			}
		}


		adjustAccordionHeigth();



		inheritingPermissions = false;
		changesMadeToPermissions=true;
	   }

	}

	function enableCheckboxes (checkboxes) {

		if(checkboxes.viewPermissionCheckbox)
			checkboxes.viewPermissionCheckbox.attr('disabled', false);
		if(checkboxes.addChildrenPermissionCheckbox)
			checkboxes.addChildrenPermissionCheckbox.attr('disabled', false);
		if(checkboxes.editPermissionCheckbox)
			checkboxes.editPermissionCheckbox.attr('disabled', false);
		if(checkboxes.publishPermissionCheckbox)
			checkboxes.publishPermissionCheckbox.attr('disabled', false);
		if(checkboxes.editPermissionsPermissionCheckbox)
			checkboxes.editPermissionsPermissionCheckbox.attr('disabled', false);
		if(dijit.byId('cascadeChangesCheckbox'))
			dijit.byId('cascadeChangesCheckbox').attr('disabled', false);

	}

	function resetPermissions () {
		if(confirm(removeIndividualPermissionConfirm)) {
			changesMadeToPermissions=false;
			dijit.byId('savingPermissionsDialog').show();
			PermissionAjax.resetAssetPermissions(assetId, languageId, resetPermissionsCallback);
		}
	}

	function resetPermissionsCallback () {
		dijit.byId('savingPermissionsDialog').hide();
		loadPermissions();
	}

	//Permissions tab utility functions
	function findRole(roleId, roles) {
		for(var i = 0; i < roles.length; i++) {
			if(roles[i].id == roleId) {
				return roles[i];
			}
		}
		return null;
	}

	function removeRole(roleId, roles) {
		for(var i = 0; i < roles.length; i++) {
			if(roles[i].id == roleId) {
				roles.splice(i, 1);
			}
		}
	}

	function getPermissionCheckboxDijits (type, id) {
		var prefix = type?type + "-":"";
		var viewPermissionCheckbox = dijit.byId(prefix + 'view-permission-' + id);
		var addChildrenPermissionCheckbox = dijit.byId(prefix + 'add-children-permission-' + id);
		var editPermissionCheckbox = dijit.byId(prefix + 'edit-permission-' + id);
		var publishPermissionCheckbox = dijit.byId(prefix + 'publish-permission-' + id);
		var editPermissionsPermissionCheckbox = dijit.byId(prefix + 'edit-permissions-permission-' + id);
		return {
			viewPermissionCheckbox: viewPermissionCheckbox,
			addChildrenPermissionCheckbox: addChildrenPermissionCheckbox,
			editPermissionCheckbox: editPermissionCheckbox,
			publishPermissionCheckbox: publishPermissionCheckbox,
			editPermissionsPermissionCheckbox: editPermissionsPermissionCheckbox
		};

	}

	function addTemplatePermissionOptions(role, permissions){

		fillTemplatePermissionOptions(role, permissions);
		fillTemplatePermissionOptions(role, permissions, hostClassName, 'hosts');
		fillTemplatePermissionOptions(role, permissions, folderClassName, 'folders');
		fillTemplatePermissionOptions(role, permissions, containerClassName, 'containers');
		fillTemplatePermissionOptions(role, permissions, templateClassName, 'templates');
		fillTemplatePermissionOptions(role, permissions, templateLayoutClassName, 'templateLayouts');
		fillTemplatePermissionOptions(role, permissions, pageClassName, 'pages');
		fillTemplatePermissionOptions(role, permissions, linkClassName, 'links');
		fillTemplatePermissionOptions(role, permissions, contentClassName, 'content');
		fillTemplatePermissionOptions(role, permissions, structureClassName, 'structure');
		fillTemplatePermissionOptions(role, permissions, rulesClassName, 'rules');

		role["view-permission-style"] = '';
		role["add-children-permission-style"] = '';
		role["edit-permission-style"] = '';
		role["publish-permission-style"] = '';
		role["edit-permissions-permission-style"] = '';
		role["add-children-permission-style"] = '';
		if(assetType == 'com.dotmarketing.portlets.folders.model.Folder') {
			role["publish-permission-style"] = 'display:none';
		} else if(assetType == 'com.dotmarketing.beans.Host') {
			role["publish-permission-style"] = 'display:none';
		} else if(assetType == 'com.dotmarketing.portlets.structure.model.Structure') {
			role["add-children-permission-style"] = 'display: none';
		} else if(assetType == 'com.dotmarketing.portlets.categories.model.Category') {
			role["publish-permission-style"] = 'display:none';
			role["add-children-permission-style"] = 'display: none';
		} else if(assetType == 'com.dotmarketing.portlets.report.model.Report') {
			role["publish-permission-style"] = 'display:none';
			role["add-children-permission-style"] = 'display: none';
		}
     <% if(UtilMethods.isSet(contentletAux) && contentletAux.getStructure().getStructureType()==Structure.STRUCTURE_TYPE_HTMLPAGE) {%>
     <% } %>
		else {
			role["add-children-permission-style"] = 'display: none'
		}

		role["icon"] = '/html/images/shim.gif';
		if(!role.editPermissions || role.locked) {
			role["icon"] = '/html/images/icons/lock.png';
		}

		role.hostsWillInherit = hostsWillInheritMsg;
		role.foldersWillInherit = foldersWillInheritMsg;
		role.containersWillInherit = containersWillInheritMsg;
		role.templatesWillInherit = templatesWillInheritMsg;
		role.templateLayoutsWillInherit = templateLayoutsWillInheritMsg;
		role.pagesWillInherit = pagesWillInheritMsg;
		role.linksWillInherit = linksWillInheritMsg;
		role.contentWillInherit = contentWillInheritMsg;
		role.permissionsOnChildren1=permissionsOnChildrenMsg1;
		role.permissionsOnChildren2=permissionsOnChildrenMsg2;
		role.structureWillInherit = structureWillInheritMsg;
		role.rulesWillInherit = rulesWillInheritMsg;
	}

	function fillTemplatePermissionOptions (role, permissions, permissionType, assetType) {

		if(!permissionType) permissionType = 'individual'

		prefix = "view";
		if(assetType) prefix = assetType + "View";
		if(hasPermissionSet(permissions, permissionType, viewPermission)) {
			role[prefix + "PermissionChecked"] = 'checked="checked"'
		} else {
			role[prefix + "PermissionChecked"] = ''
		}

		prefix = "addChildren";
		if(assetType) prefix = assetType + "AddChildren";
		if(hasPermissionSet(permissions, permissionType, addChildrenPermission)) {
			role[prefix + "PermissionChecked"] = 'checked="checked"'
		} else {
			role[prefix + "PermissionChecked"] = ''
		}

		prefix = "edit";
		if(assetType) prefix = assetType + "Edit";
		if(hasPermissionSet(permissions, permissionType, editPermission)) {
			role[prefix + "PermissionChecked"] = 'checked="checked"'
		} else {
			role[prefix + "PermissionChecked"] = ''
		}

		prefix = "publish";
		if(assetType) prefix = assetType + "Publish";
		if(hasPermissionSet(permissions, permissionType, publishPermission)) {
			role[prefix + "PermissionChecked"] = 'checked="checked"'
		} else {
			role[prefix + "PermissionChecked"] = ''
		}

		prefix = "editPermissions";
		if(assetType) prefix = assetType + "EditPermissions";
		if(hasPermissionSet(permissions, permissionType, editPermissionsPermission)) {
			role[prefix + "PermissionChecked"] = 'checked="checked"'
		} else {
			role[prefix + "PermissionChecked"] = ''
		}

		if(role.inherited || !doesUserHavePermissionsToEdit || role.editPermissions != true || role.locked==true) {
			 role.editPermissionDisabled = 'disabled="disabled"';
		} else {
			 role.editPermissionDisabled = '';
		}

	}

	function hasPermissionSet(list, type, permission) {
		for (var i = 0; i < list.length; i++) {
			var perm = list[i];
			if((perm.permission & permission) == permission && perm.type == type) {
				return true;
			}
		}
		return false;
	}

	function norm(value) {
		return dojo.isArray(value)?value[0]:value;
	}


	function permissionIndividuallyCallback () {
		dijit.byId('savingPermissionsDialog').hide();
		loadPermissions();
		dojo.style('permissionsActions', { display: '' });
	}

	function destroyChecks(){
		try{
		   if (dijits) {
			  for (var i = 0, n = dijits.length; i < n; i++) {
				  for (var j = 0, n = dijits[i].length; j < n; j++) {
					  dijits[i][j].destroyRecursive();
				  }
			   }
		   }
		}catch(ex){
			console.log(ex);
		}
	}

--></script>