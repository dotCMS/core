<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="com.dotmarketing.portlets.folders.model.Folder"%>
<%@page import="com.dotmarketing.portlets.containers.model.Container"%>
<%@page import="com.dotmarketing.portlets.templates.model.Template"%>
<%@page import="com.dotmarketing.portlets.htmlpages.model.HTMLPage"%>
<%@page import="com.dotmarketing.portlets.files.model.File"%>
<%@page import="com.dotmarketing.portlets.links.model.Link"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.portlets.categories.model.Category"%>



<%@page import="com.dotmarketing.portlets.structure.model.Structure"%><script type="text/javascript">

	dojo.require('dijit.layout.AccordionContainer');
	dojo.require('dotcms.dijit.form.HostFolderFilteringSelect');

	//I18n Messages
	var hostsWillInheritMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Hosts")) %>';
	var foldersWillInheritMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Folders")) %>';
	var containersWillInheritMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Containers")) %>';
	var templatesWillInheritMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Templates")) %>';
	var pagesWillInheritMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Pages")) %>';
	var filesWillInheritMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Files")) %>';
	var linksWillInheritMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Links")) %>';
	var structuresWillInheritMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Structures")) %>';
	var contentWillInheritMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Content")) %>';
	var cascadeChangesMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Cascade-Changes")) %>';
	var applyChangesMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Apply-Changes")) %>';
	var selectAFolderOrHostMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "please-select-a-folder-or-host")) %>';
	var selectedHostFolderAlreadyInlistMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "selected-host-folder-is-already-in-list")) %>'
	var permissionsSavedMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "permissions-saved")) %>'
    var noPermissionsSavedMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "no-permissions-saved")) %>'
	var permissionsNotEditableMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "permissions-not-editable-for-role")) %>'
	var cascadePermissionsConfirm = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cascade-permissions-confirm-msg")) %>'
	var dontHavePermissionsMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "dont-have-permissions-msg")) %>'
	var unexpectedErrorOcurredMsg =  '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "an-unexpected-system-error-occurred")) %>';
	var cascadePermissionsHint = '<%= LanguageUtil.get(pageContext, "Cascade-Permissions-Hint") %>';
	var permissionBreakInheritance='<%=LanguageUtil.get(pageContext, "role-manager-applying-inheritable-permissions") %>';
	var permissionBreakInheritanceWarnIcon='<%=LanguageUtil.get(pageContext, "role-manager-object-inherits-permissions") %>';
	var cascadePermissionsChangesConfirm = '<%= LanguageUtil.get(pageContext, "Cascade-Permissions-Changes-Confirm") %>';
	var cascadePermissionsTasksRunningConfirm ='<%= LanguageUtil.get(pageContext, "Cascade-Permissions-Tasks-Running-Proceed-Confirm") %>';
	var permissionsOnChildrenMsg1 = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Permissions-on-Children1")) %>';
	var permissionsOnChildrenMsg2 = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Permissions-on-Children2")) %>';
	var categoriesWillInheritMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Category")) %>';

	<%if(UtilMethods.isSet(request.getAttribute("ViewingUserRole"))){%>
		var nameMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "this-pageContext")) %>';
	<%}else{%>
		var nameMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Name")) %>';
	<%}%>

	//Global vars
	var viewPermission = <%= PermissionAPI.PERMISSION_READ %>;
	var editPermission = <%= PermissionAPI.PERMISSION_WRITE %>;
	var publishPermission = <%= PermissionAPI.PERMISSION_PUBLISH %>;
	var editPermissionsPermission = <%= PermissionAPI.PERMISSION_EDIT_PERMISSIONS %>;
	var createVirtualLinksPermission = <%= PermissionAPI.PERMISSION_CREATE_VIRTUAL_LINKS %>;
	var addChildrenPermission = <%= PermissionAPI.PERMISSION_CAN_ADD_CHILDREN %>;

	var hostClassName = '<%= Host.class.getCanonicalName() %>'
	var folderClassName = '<%= Folder.class.getCanonicalName() %>'
	var containerClassName = '<%= Container.class.getCanonicalName() %>'
	var templateClassName = '<%= Template.class.getCanonicalName() %>'
	var pageClassName = '<%= HTMLPage.class.getCanonicalName() %>'
	var fileClassName = '<%= File.class.getCanonicalName() %>'
	var linkClassName = '<%= Link.class.getCanonicalName() %>'
	var structureClassName = '<%= Structure.class.getCanonicalName() %>'
	var contentClassName = '<%= Contentlet.class.getCanonicalName() %>';
	var systemHostId = '<%= APILocator.getHostAPI().findSystemHost().getIdentifier() %>';
	var categoryClassName = '<%= Category.class.getCanonicalName() %>';

	var currentListOfHostFolders = new Array();
	var permissionsData;
	var rolePermissionsEditable = true;
	var currentPermissionsRole = null;
	var cascadingChanges = false;
	var cascadingChangesInProgress=false;

	function checkCurrentCascadeTasks () {
		RoleAjax.getCurrentCascadePermissionsJobs(checkCurrentCascadeTasksCallback);
	}

	function checkCurrentCascadeTasksCallback (tasks) {
		var html = '';
		var continueCheck=false;
		cascadingChangesInProgress=false;
		dojo.style('permissionsCascadeMsg', { display: 'none' });
		for(var i = 0; i < tasks.length; i++) {
			var task = tasks[i];
			var role = task.role;
			var host = task.host;
			var folder = task.folder;
			var roleFQN = role.FQN;
			var isSystemHost = host.isSystemHost;
			var assetName = host?(!isSystemHost?host.hostName:'<%=LanguageUtil.get(pageContext, "All-Hosts") %>'):folder.path;
			try{
			  if(role.id == norm(currentRoleId)) {
				dojo.byId('permissionsCascadeMsgText').innerHTML = '<%=LanguageUtil.get(pageContext, "Role-Permissions-Currently-Cascading") %>';
				dojo.style('permissionsCascadeMsg', { display: '' });
				cascadingChangesInProgress=true;
				continueCheck=true;
			  }
			}catch(refError){}
		}
		if(continueCheck){
			setTimeout('checkCurrentCascadeTasks()', 10000);
		}
	}

	function loadPermissionsForRole(roleId){

		checkCurrentCascadeTasks();
		if(dijit.byId('permissionsAccordionContainer')) {
			var container = dijit.byId('permissionsAccordionContainer');
			dojo.forEach(container.getChildren(), function(child){
				container.removeChild(child);
				child.destroyRecursive(false);
			}, this);
			dijit.registry.remove('permissionsAccordionContainer');
		}

		dojo.style("rolePermissionsWrapper", {
			display: ""
		});
		var container = new dijit.layout.AccordionContainer({
            	style: "height: 400px"
        	}, "permissionsAccordionContainer");

		dojo.style("rolePermissionsWrapper", {
			display: "none"
		});
		dojo.style("loadingPermissionsAccordion", {
			display: ""
		});

		RoleAjax.getRole(roleId, dojo.hitch(this, loadRoleCallback));
	}

	function reloadCurrentRolePermissions () {
		loadPermissionsForRole(currentPermissionsRole.id);
	}

	function loadRoleCallback(role) {
		currentPermissionsRole = role;
		RoleAjax.getRolePermissions(norm(currentPermissionsRole.id), dojo.hitch(this, loadPermissionsForRoleCallback, norm(currentPermissionsRole.id)));
	}

	// http://jira.dotmarketing.net/browse/DOTCMS-6213
	// dijit tooltips added to the DOM tree are started correctly
	// but then they don't work. Creating them here outside works.
	// maybe due recent dojo upgrade
	function createHints(item) {
	    new dijit.Tooltip({
           connectId: "cascadePermissionsHintHook-"+item.id,
           label: item.cascadePermissionsHint
        });
        new dijit.Tooltip({
           connectId: "inheritPermissionsHintHook-"+item.id,
           label: permissionBreakInheritanceWarnIcon
        });
	}

	function loadPermissionsForRoleCallback(roleId, data) {

		if(norm(currentPermissionsRole.editPermissions) == false) {
			rolePermissionsEditable = false;
			dojo.byId('rolePermissionsMsg').innerHTML = permissionsNotEditableMsg;
			dojo.style('rolePermissionsHostSelectorWrapper', { display: 'none' });
		} else {
			rolePermissionsEditable = true;
			dojo.byId('rolePermissionsMsg').innerHTML = '';
			dojo.style('rolePermissionsHostSelectorWrapper', { display: '' });
		}

		permissionsData = data;
		currentListOfHostFolders = new Array();
		var systemHost = getSystemHost(data);

		//Generating the system host accordion entry
		var item = { id: systemHost.identifier, imgSrc: '/html/images/icons/folder-open-globe.png',
					fullPath: '<%=LanguageUtil.get(pageContext, "All-Hosts") %>', permissionToEditPermissions: systemHost.permissionToEditPermissions,
					type: 'host', cascadePermissionsHint:cascadePermissionsHint };

		addTemplatePermissionOptions(item, systemHost.permissions);

		var titleSystemTemplateString = dojo._getText('/html/portlet/ext/roleadmin/system_host_accordion_title.html');
		var titleTemplateString = dojo._getText('/html/portlet/ext/roleadmin/host_folder_accordion_title.html');
		var contentTemplateString = dojo._getText('/html/portlet/ext/roleadmin/host_folder_accordion_entry.html');
		var contentSystemTemplateString = dojo._getText('/html/portlet/ext/roleadmin/system_host_folder_accordion_entry.html');

        var title = dojo.string.substitute(titleSystemTemplateString, item);
        var content = dojo.string.substitute(contentSystemTemplateString, item);
        var contentDom = dojo._toDom(content);
		contentPaneToPermissionsAccordion(item.id, title, contentDom);
		currentListOfHostFolders.push(item);
		createHints(item);

		//Iterating over the other assets in the list to render their accordion panes
		dojo.forEach(data, function (asset) {

			var systemHost = getSystemHost(data);
			if(asset.type == 'host' && asset.identifier == systemHost.identifier) return;

			var icon, path;
			if(asset.type == 'host') {
				icon = '/html/images/icons/globe-medium.png';
				path = asset.hostname;
				id = asset.identifier;
			}
			else {
				icon = '/html/images/icons/folder-horizontal.png';
				path = asset.fullPath;
				id = asset.inode;
			}

			var item = { id: id, imgSrc: icon, fullPath: path,
					permissionToEditPermissions: asset.permissionToEditPermissions,
					type: asset.type };

			addTemplatePermissionOptions(item, asset.permissions);

	        var title = dojo.string.substitute(titleTemplateString, item);
    	    var content = dojo.string.substitute(contentTemplateString, item);
        	var contentDom = dojo._toDom(content);
			contentPaneToPermissionsAccordion(item.id, title, contentDom);
			currentListOfHostFolders.push(item);
			createHints(item);
		}, this)
		//Adjusting content panes height

		dojo.style("rolePermissionsWrapper", {
			display: ""
		});
		dojo.style("loadingPermissionsAccordion", {
			display: "none"
		});

		var container = dijit.byId('permissionsAccordionContainer');
		container.startup();

		var selectedChildPaneId = systemHost.identifier;
		var myHeight = 375;
		for(var i = 0; i < data.length; i++) {
			var id = data[i].type=='host'?data[i].identifier:data[i].inode;
			if(id != systemHost.identifier)
				selectedChildPaneId = id;
			myHeight += dojo.marginBox('permissionsAccordionPane-' + id + "_button").h;
			dojo.parser.parse(dojo.byId('hostFolderAccordionPermissionsTitleWrapper-' + id));
		}
		container.resize({ h: myHeight });
		container._verticalSpace = 375;
		
		if(data.length > 1)//GIT-417 -- To fix the weird behaviour of permissions tab under roles portlet.
			container.selectChild(dijit.byId('permissionsAccordionPane-' + norm(selectedChildPaneId)));
		else{
			var dummyPaneId = 'dotDummyPane'+(new Date().getTime());
			container.addChild(new dijit.layout.ContentPane({id:dummyPaneId}));
			container.selectChild(dijit.byId(dummyPaneId));
			container.selectChild(dijit.byId('permissionsAccordionPane-' + norm(systemHost.identifier)));
			container.removeChild(dijit.byId(dummyPaneId));
		}

	}

	function createAndUnregisterDijitslist(id){

		var toRemove = [
			'view-permission-' + id,
			'add-children-permission-' + id,
			'edit-permission-' + id,
			'publish-permission-' + id,
			'edit-permission-' + id,
			'publish-permission-' + id,
			'edit-permissions-permission-' + id,
			'virtual-links-permission-' + id,
			'permissionsAccordionPane-' + id,

			'hosts-view-permission-' + id,
			'hosts-add-children-permission-' + id,
			'hosts-edit-permission-' + id,
			'hosts-publish-permission-' + id,
			'hosts-edit-permission-' + id,
			'hosts-publish-permission-' + id,
			'hosts-edit-permissions-permission-' + id,
			'hosts-virtual-links-permission-' + id,
			

			'folders-view-permission-' + id,
			'folders-add-children-permission-' + id,
			'folders-edit-permission-' + id,
			'folders-publish-permission-' + id,
			'folders-edit-permission-' + id,
			'folders-publish-permission-' + id,
			'folders-edit-permissions-permission-' + id,
			'folders-virtual-links-permission-' + id,

			'containers-view-permission-' + id,
			'containers-add-children-permission-' + id,
			'containers-edit-permission-' + id,
			'containers-publish-permission-' + id,
			'containers-edit-permission-' + id,
			'containers-publish-permission-' + id,
			'containers-edit-permissions-permission-' + id,
			'containers-virtual-links-permission-' + id,

			'templates-view-permission-' + id,
			'templates-add-children-permission-' + id,
			'templates-edit-permission-' + id,
			'templates-publish-permission-' + id,
			'templates-edit-permission-' + id,
			'templates-publish-permission-' + id,
			'templates-edit-permissions-permission-' + id,
			'templates-virtual-links-permission-' + id,

			'pages-view-permission-' + id,
			'pages-add-children-permission-' + id,
			'pages-edit-permission-' + id,
			'pages-publish-permission-' + id,
			'pages-edit-permission-' + id,
			'pages-publish-permission-' + id,
			'pages-edit-permissions-permission-' + id,
			'pages-virtual-links-permission-' + id,

			'files-view-permission-' + id,
			'files-add-children-permission-' + id,
			'files-edit-permission-' + id,
			'files-publish-permission-' + id,
			'files-edit-permission-' + id,
			'files-publish-permission-' + id,
			'files-edit-permissions-permission-' + id,
			'files-virtual-links-permission-' + id,

			'links-view-permission-' + id,
			'links-add-children-permission-' + id,
			'links-edit-permission-' + id,
			'links-publish-permission-' + id,
			'links-edit-permission-' + id,
			'links-publish-permission-' + id,
			'links-edit-permissions-permission-' + id,
			'links-virtual-links-permission-' + id,

			'structures-view-permission-' + id,
			'structures-add-children-permission-' + id,
			'structures-edit-permission-' + id,
			'structures-publish-permission-' + id,
			'structures-edit-permission-' + id,
			'structures-publish-permission-' + id,
			'structures-edit-permissions-permission-' + id,
			'structures-virtual-links-permission-' + id,

			'content-view-permission-' + id,
			'content-add-children-permission-' + id,
			'content-edit-permission-' + id,
			'content-publish-permission-' + id,
			'content-edit-permission-' + id,
			'content-publish-permission-' + id,
			'content-edit-permissions-permission-' + id,
			'content-virtual-links-permission-' + id,

			'categories-view-permission-' + id,
			'categories-add-children-permission-' + id,
			'categories-edit-permission-' + id,
			'categories-publish-permission-' + id,
			'categories-edit-permission-' + id,
			'categories-publish-permission-' + id,
			'categories-edit-permissions-permission-' + id,
			'categories-virtual-links-permission-' + id,

			'cascadeChangesCheckbox-' + id,
			'applyChangesButton-' + id
		];

		unregisterDijits(toRemove);

	}

	function contentPaneToPermissionsAccordion (id, title, content) {

		createAndUnregisterDijitslist(id);
		var container = dijit.byId('permissionsAccordionContainer');
		var contentPane = new dijit.layout.ContentPane({
            title: title,
            content: content,
			id: 'permissionsAccordionPane-' + norm(id)
        })

        container.addChild(contentPane);
	}


	function addHostFolder () {

		var value = dijit.byId('rolePermissionsHostSelector').attr('value');
		var item = dijit.byId('rolePermissionsHostSelector').attr('selectedItem');
		if(!value) {
			alert(selectAFolderOrHostMsg);
			return;
		}

		if(findHostFolder(value, currentListOfHostFolders)) {
			alert(selectedHostFolderAlreadyInlistMsg);
			return;
		}


		if(item.type == 'host') {
			item.imgSrc = "/html/images/icons/globe-green.png";
		} else {
			item.imgSrc = "/html/images/icons/folder-horizontal.png";
		}

		addEmptyTemplatePermissionOptions(item);

		item.cascadePermissionsHint = cascadePermissionsHint;

		var titleTemplateString = dojo._getText('/html/portlet/ext/roleadmin/host_folder_accordion_title.html');
        titleTemplateString = dojo.string.substitute(titleTemplateString, item);

		var templateString = dojo._getText('/html/portlet/ext/roleadmin/host_folder_accordion_entry.html');
        templateString = dojo.string.substitute(templateString, item);
        var domObj = dojo._toDom(templateString);
        createAndUnregisterDijitslist(item.id);
		var container = dijit.byId('permissionsAccordionContainer');
        container.addChild(new dijit.layout.ContentPane({
            title: titleTemplateString,
            content: domObj,
			id: 'permissionsAccordionPane-' + norm(item.id)
        }));

		dojo.parser.parse(dojo.byId('hostFolderAccordionPermissionsTitleWrapper-' + item.id));

		RoleAjax.isPermissionableInheriting(value, function(data){
			if(data.isInheriting){
				dojo.byId('inheritPermissionsHintHook-' + item.id).style.display = "";
			 }
		});

		currentListOfHostFolders.push(item);
		createHints(item);

		var myHeight = 375;
		for(var i = 0; i < currentListOfHostFolders.length; i++) {
			var id = currentListOfHostFolders[i].id;
			myHeight += dojo.marginBox('permissionsAccordionPane-' + id + "_button").h;
			dojo.parser.parse(dojo.byId('hostFolderAccordionPermissionsTitleWrapper-' + id));
		}
		container.resize({ h: myHeight })
		container._verticalSpace = 375;

		container.selectChild(dijit.byId('permissionsAccordionPane-' + norm(item.id)));

	}

	function applyPermissionChanges (id) {

		// check if there is changes
        if(dijit.byId('cascadeChangesCheckbox-' + id).attr('value') == false) {
            if(!thereIsPermissionCheckChanges(id)) {
                showDotCMSSystemMessage(noPermissionsSavedMsg);
                return;
            }
        }

		RoleAjax.isPermissionableInheriting(id, function(data){
			if(!data.isInheriting || (data.isInheriting && confirm(permissionBreakInheritance))){
				var systemHost = getSystemHost(permissionsData);
				var individualPermissions = retrievePermissionChecks(id);
				var hostsPermissions = retrievePermissionChecks(id, 'hosts');
				var foldersPermissions = retrievePermissionChecks(id, 'folders');
				var containersPermissions = retrievePermissionChecks(id, 'containers');
				var templatesPermissions = retrievePermissionChecks(id, 'templates');
				var pagesPermissions = retrievePermissionChecks(id, 'pages');
				var filesPermissions = retrievePermissionChecks(id, 'files');
				var linksPermissions = retrievePermissionChecks(id, 'links');
				var structuresPermissions = retrievePermissionChecks(id, 'structures');
				var contentPermissions = retrievePermissionChecks(id, 'content');
				var categoriesPermissions = retrievePermissionChecks(id, 'categories');
				var cascadeChanges = dijit.byId('cascadeChangesCheckbox-' + id).attr('value') == 'on';
			       checkCurrentCascadeTasks();

				if(cascadingChangesInProgress){
					if(!confirm(cascadePermissionsTasksRunningConfirm)){
						return;
						}
					}
				if(cascadeChanges && !confirm(cascadePermissionsConfirm))
					return;

				var permissionsRoleId = norm(currentPermissionsRole.id);

				var permissionsToSave = { individual: individualPermissions,
						hosts: hostsPermissions,
						folders: foldersPermissions,
						containers: containersPermissions,
						templates: templatesPermissions,
						pages: pagesPermissions,
						files: filesPermissions,
						links: linksPermissions,
						structures: structuresPermissions,
						content: contentPermissions,
						categories: categoriesPermissions};

				var callbackOptions = {
					callback: dojo.hitch(this, applyPermissionChangesCallback, permissionsRoleId, id, permissionsToSave, cascadeChanges),
					exceptionHandler: applyPermissionChangesFail
				}

				dijit.byId('savingPermissionsDialog').show();

				cascadingChanges = cascadeChanges;
				RoleAjax.saveRolePermission(permissionsRoleId, id, permissionsToSave, cascadeChanges, callbackOptions);
				dijit.byId('cascadeChangesCheckbox-' + id).attr('value', false);
			}
		});
	}

	function applyPermissionChangesCallback(permissionsRoleId, id, permissionsToSave, cascadeChanges) {
		if(cascadeChanges) {
			cascadingChanges = false;
			reloadCurrentRolePermissions();
			checkCurrentCascadeTasks();
		}
		if (dijit.byId('permissionsAccordionPane-' + id) && isRemovingAllPermissions(permissionsToSave)) {
			var accordionPane = dijit.byId('permissionsAccordionPane-' + id);
			var container = dijit.byId('permissionsAccordionContainer');
			container.removeChild(accordionPane);
		}
		dijit.byId('savingPermissionsDialog').hide();
		showDotCMSSystemMessage(permissionsSavedMsg);
	}

	function applyPermissionChangesFail(message, ex){
		dijit.byId('savingPermissionsDialog').hide();
		cascadingChanges = false;
		if(ex.javaClassName == "com.dotmarketing.exception.DotSecurityException") {
			alert(dontHavePermissionsMsg);
		} else {
			alert(unexpectedErrorOcurredMsg + ":" + message);
		}
	}


	function retrievePermissionChecks(id, type) {

		var permission = 0;

		var prefix = '';
		if(type) prefix = type + "-";

		if(dijit.byId(prefix + 'view-permission-' + id) && dijit.byId(prefix + 'view-permission-' + id).attr('value') == 'on')
			permission = permission | viewPermission;
		if(dijit.byId(prefix + 'add-children-permission-' + id) && dijit.byId(prefix + 'add-children-permission-' + id).attr('value') == 'on')
			permission = permission | addChildrenPermission;
		if(dijit.byId(prefix + 'edit-permission-' + id) && dijit.byId(prefix + 'edit-permission-' + id).attr('value') == 'on')
			permission = permission | editPermission;
		if(dijit.byId(prefix + 'publish-permission-' + id) && dijit.byId(prefix + 'publish-permission-' + id).attr('value') == 'on')
			permission = permission | publishPermission;
		if(dijit.byId(prefix + 'edit-permissions-permission-' + id) && dijit.byId(prefix + 'edit-permissions-permission-' + id).attr('value') == 'on')
			permission = permission | editPermissionsPermission;
		if(dijit.byId(prefix + 'virtual-links-permission-' + id) && dijit.byId(prefix + 'virtual-links-permission-' + id).attr('value') == 'on')
			permission = permission | createVirtualLinksPermission;

		return permission;

	}

	function thereIsPermissionCheckChanges(id) {
        var changes=false;
        var item;

        dojo.forEach(currentListOfHostFolders, function(itemvar) {
            if(itemvar.id==id)
                item=itemvar;
            });

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
        if(dijit.byId('virtual-links-permission-' + id) &&
                ((dijit.byId('virtual-links-permission-' + id).attr('value') == 'on' && item.virtualLinksPermissionChecked=="") ||
                 (dijit.byId('virtual-links-permission-' + id).attr('value') == false && item.virtualLinksPermissionChecked!="")))
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
            if(dijit.byId(type+'-virtual-links-permission-' + id) &&
                    ((dijit.byId(type+'-virtual-links-permission-' + id).attr('value') == 'on' && item[type+'VirtualLinksPermissionChecked']=="") ||
                     (dijit.byId(type+'-virtual-links-permission-' + id).attr('value') == false && item[type+'VirtualLinksPermissionChecked']!="")))
                return true;
        }

        types=['hosts','folders','containers','templates','pages','files','links','structures','content','categories'];

        for(var i=0;i<types.length;i++)
            if(changedType(item,types[i]))
                return true;

        return false;
    }

	function viewPermissionChanged (type, id) {

		var checkboxes = getPermissionCheckboxDijits(type, id);

		if(checkboxes.viewPermissionCheckbox.attr('value') != 'on') {
			if(checkboxes.addChildrenPermissionCheckbox) checkboxes.addChildrenPermissionCheckbox.attr('value', false);
			if(checkboxes.editPermissionCheckbox) checkboxes.editPermissionCheckbox.attr('value', false);
			if(checkboxes.publishPermissionCheckbox) checkboxes.publishPermissionCheckbox.attr('value', false);
			if(checkboxes.editPermissionsPermissionCheckbox) checkboxes.editPermissionsPermissionCheckbox.attr('value', false);
		}

	}

	function addChildrenPermissionChanged (type, id) {

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

		var checkboxes = getPermissionCheckboxDijits(type, id);

		if(checkboxes.editPermissionsPermissionCheckbox.attr('value') == 'on') {
			if(checkboxes.viewPermissionCheckbox) checkboxes.viewPermissionCheckbox.attr('value', 'on');
			if(checkboxes.addChildrenPermissionCheckbox) checkboxes.addChildrenPermissionCheckbox.attr('value', 'on');
			if(checkboxes.editPermissionCheckbox) checkboxes.editPermissionCheckbox.attr('value', 'on');
			if(checkboxes.publishPermissionCheckbox) checkboxes.publishPermissionCheckbox.attr('value', 'on');
		}

	}

	function virtualLinksPermissionChanged (type, id) {

	}

	//Permissions tab utility functions
	function getPermissionCheckboxDijits (type, id) {
		var prefix = type?type + "-":"";
		var viewPermissionCheckbox = dijit.byId(prefix + 'view-permission-' + id);
		var addChildrenPermissionCheckbox = dijit.byId(prefix + 'add-children-permission-' + id);
		var editPermissionCheckbox = dijit.byId(prefix + 'edit-permission-' + id);
		var publishPermissionCheckbox = dijit.byId(prefix + 'publish-permission-' + id);
		var editPermissionsPermissionCheckbox = dijit.byId(prefix + 'edit-permissions-permission-' + id);
		var virtualLinksPermissionCheckbox = dijit.byId(prefix + 'virtual-links-permissions-permission-' + id);
		return {
			viewPermissionCheckbox: viewPermissionCheckbox,
			addChildrenPermissionCheckbox: addChildrenPermissionCheckbox,
			editPermissionCheckbox: editPermissionCheckbox,
			publishPermissionCheckbox: publishPermissionCheckbox,
			editPermissionsPermissionCheckbox: editPermissionsPermissionCheckbox,
			virtualLinksPermissionCheckbox: virtualLinksPermissionCheckbox
		};

	}


	function addTemplatePermissionOptions(item, permissions){
		fillTemplatePermissionOptions(item, permissions);
		fillTemplatePermissionOptions(item, permissions, hostClassName, 'hosts');
		fillTemplatePermissionOptions(item, permissions, folderClassName, 'folders');
		fillTemplatePermissionOptions(item, permissions, containerClassName, 'containers');
		fillTemplatePermissionOptions(item, permissions, templateClassName, 'templates');
		fillTemplatePermissionOptions(item, permissions, pageClassName, 'pages');
		fillTemplatePermissionOptions(item, permissions, fileClassName, 'files');
		fillTemplatePermissionOptions(item, permissions, linkClassName, 'links');
		fillTemplatePermissionOptions(item, permissions, structureClassName, 'structures');
		fillTemplatePermissionOptions(item, permissions, contentClassName, 'content');
		fillTemplatePermissionOptions(item, permissions, categoryClassName, 'categories');
		if(item.type == 'host') {
			if(item.id == systemHostId) {
				item.hostsPermissionsEntryStyle = '';
				item.structuresPermissionsEntryStyle = '';
			} else {
				item.hostsPermissionsEntryStyle = 'display: none';
				item.structuresPermissionsEntryStyle = '';
			}
			item.containersPermissionsEntryStyle = '';
			item.templatesPermissionsEntryStyle = '';
			item.categoriesPermissionsEntryStyle = '';
		} else {
			item.hostsPermissionsEntryStyle = 'display: none';
			item.containersPermissionsEntryStyle = 'display: none';
			item.templatesPermissionsEntryStyle = 'display: none';
			item.categoriesPermissionsEntryStyle = 'display: none';
			if(item.type == 'folder'){
				item.structuresPermissionsEntryStyle = '';
			}else{
				item.structuresPermissionsEntryStyle = 'display: none';
			}

		}
		if(!rolePermissionsEditable || !item.permissionToEditPermissions) {
			item.disabledPermissions = 'disabled="disabled"'
		} else {
			item.disabledPermissions = ''
		}
		item.hostsWillInherit = hostsWillInheritMsg;
		item.foldersWillInherit = foldersWillInheritMsg;
		item.containersWillInherit = containersWillInheritMsg;
		item.templatesWillInherit = templatesWillInheritMsg;
		item.pagesWillInherit = pagesWillInheritMsg;
		item.filesWillInherit = filesWillInheritMsg;
		item.linksWillInherit = linksWillInheritMsg;
		item.structuresWillInherit = structuresWillInheritMsg;
		item.contentWillInherit = contentWillInheritMsg;
		item.cascadeChanges = cascadeChangesMsg;
		item.applyChanges = applyChangesMsg;
		item.cascadePermissionsHint = cascadePermissionsHint;
		item.permissionsOnChildren1=permissionsOnChildrenMsg1;
		item.permissionsOnChildren2=permissionsOnChildrenMsg2;
		item.name=nameMsg;
		item.categoriesWillInherit = categoriesWillInheritMsg;

	}

	function addEmptyTemplatePermissionOptions(item) {
		fillEmptyTemplatePermissionOptions(item);
		fillEmptyTemplatePermissionOptions(item, 'hosts');
		fillEmptyTemplatePermissionOptions(item, 'folders');
		fillEmptyTemplatePermissionOptions(item, 'containers');
		fillEmptyTemplatePermissionOptions(item, 'templates');
		fillEmptyTemplatePermissionOptions(item, 'pages');
		fillEmptyTemplatePermissionOptions(item, 'files');
		fillEmptyTemplatePermissionOptions(item, 'links');
		fillEmptyTemplatePermissionOptions(item, 'structures');
		fillEmptyTemplatePermissionOptions(item, 'content');
		fillEmptyTemplatePermissionOptions(item, 'categories');

		if(item.type == 'host') {
			if(item.id == systemHostId) {
				item.hostsPermissionsEntryStyle = '';
				item.structuresPermissionsEntryStyle = '';
			} else {
				item.hostsPermissionsEntryStyle = 'display: none';
				item.structuresPermissionsEntryStyle = '';
			}
			item.containersPermissionsEntryStyle = '';
			item.templatesPermissionsEntryStyle = '';
			item.categoriesPermissionsEntryStyle = '';
		} else {
			item.hostsPermissionsEntryStyle = 'display: none';
			item.containersPermissionsEntryStyle = 'display: none';
			item.templatesPermissionsEntryStyle = 'display: none';
			item.categoriesPermissionsEntryStyle = 'display: none';
			if(item.type == 'folder'){
				item.structuresPermissionsEntryStyle = '';
			}else{
				item.structuresPermissionsEntryStyle = 'display: none';
			}
		}
		item.disabledPermissions = '';
		item.hostsWillInherit = hostsWillInheritMsg;
		item.foldersWillInherit = foldersWillInheritMsg;
		item.containersWillInherit = containersWillInheritMsg;
		item.templatesWillInherit = templatesWillInheritMsg;
		item.pagesWillInherit = pagesWillInheritMsg;
		item.filesWillInherit = filesWillInheritMsg;
		item.linksWillInherit = linksWillInheritMsg;
		item.structuresWillInherit = structuresWillInheritMsg;
		item.contentWillInherit = contentWillInheritMsg;
		item.cascadeChanges = cascadeChangesMsg;
		item.applyChanges = applyChangesMsg;
		item.permissionsOnChildren1=permissionsOnChildrenMsg1;
		item.permissionsOnChildren2=permissionsOnChildrenMsg2;
		item.name=nameMsg;
		item.categoriesWillInherit = categoriesWillInheritMsg;
	}

	function fillTemplatePermissionOptions (item, permissions, permissionType, assetType) {

		if(!permissionType) permissionType = 'individual'

		prefix = "view";
		if(assetType) prefix = assetType + "View";
		if(hasPermissionSet(permissions, permissionType, viewPermission)) {
			item[prefix + "PermissionChecked"] = 'checked="checked"'
		} else {
			item[prefix + "PermissionChecked"] = ''
		}

		prefix = "addChildren";
		if(assetType) prefix = assetType + "AddChildren";
		if(hasPermissionSet(permissions, permissionType, addChildrenPermission)) {
			item[prefix + "PermissionChecked"] = 'checked="checked"'
		} else {
			item[prefix + "PermissionChecked"] = ''
		}

		prefix = "edit";
		if(assetType) prefix = assetType + "Edit";
		if(hasPermissionSet(permissions, permissionType, editPermission)) {
			item[prefix + "PermissionChecked"] = 'checked="checked"'
		} else {
			item[prefix + "PermissionChecked"] = ''
		}

		prefix = "publish";
		if(assetType) prefix = assetType + "Publish";
		if(hasPermissionSet(permissions, permissionType, publishPermission)) {
			item[prefix + "PermissionChecked"] = 'checked="checked"'
		} else {
			item[prefix + "PermissionChecked"] = ''
		}

		prefix = "editPermissions";
		if(assetType) prefix = assetType + "EditPermissions";
		if(hasPermissionSet(permissions, permissionType, editPermissionsPermission)) {
			item[prefix + "PermissionChecked"] = 'checked="checked"'
		} else {
			item[prefix + "PermissionChecked"] = ''
		}

		prefix = "virtualLinks";
		if(assetType) prefix = assetType + "VirtualLinks";
		if(hasPermissionSet(permissions, permissionType, createVirtualLinksPermission)) {
			item[prefix + "PermissionChecked"] = 'checked="checked"'
		} else {
			item[prefix + "PermissionChecked"] = ''
		}

	}

	function fillEmptyTemplatePermissionOptions (item, assetType) {

		prefix = "view";
		if(assetType) prefix = assetType + "View";
		item[prefix + "PermissionChecked"] = ''

		prefix = "addChildren";
		if(assetType) prefix = assetType + "AddChildren";
		item[prefix + "PermissionChecked"] = ''

		prefix = "edit";
		if(assetType) prefix = assetType + "Edit";
		item[prefix + "PermissionChecked"] = ''

		prefix = "publish";
		if(assetType) prefix = assetType + "Publish";
		item[prefix + "PermissionChecked"] = ''

		prefix = "editPermissions";
		if(assetType) prefix = assetType + "EditPermissions";
		item[prefix + "PermissionChecked"] = ''

		prefix = "virtualLinks";
		if(assetType) prefix = assetType + "VirtualLinks";
		item[prefix + "PermissionChecked"] = ''

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

	function getSystemHost(list) {
		for(var i = 0; i < list.length; i++) {
			if(list[i].identifier == systemHostId)
				return list[i];
		}
	}



	function findHostFolder(id, list) {
		for (var i = 0; i < list.length; i++) {
			var id1 = norm(list[i].id);
			var id2 = norm(id);
			if (id1 == id2)
				return list[i];
		}
		return null;
	}

	//Removes registered dijits
	function unregisterDijits(list) {
		dojo.forEach(list, function (id) {
			if(dijit.byId(id))
				dijit.byId(id).destroy();
		});

	}

	function isRemovingAllPermissions(permissionSet) {
		for(var permissionKey in permissionSet) {
			var permission = permissionSet[permissionKey];
			if(permission > 0)
				return false;
		}
		return true;
	}

	function setTextContent(element, text) {
	    while (element.firstChild!==null)
	        element.removeChild(element.firstChild); // remove all existing content
	    element.appendChild(document.createTextNode(text));
	}

</script>

