<%@page import="com.dotmarketing.beans.Permission"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.business.PermissionSummary"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.HostAPI"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.portlets.folders.model.Folder"%>
<%@page import="com.dotmarketing.portlets.containers.model.Container"%>
<%@page import="com.dotmarketing.portlets.templates.model.Template"%>
<%@page import="com.dotmarketing.portlets.htmlpages.model.HTMLPage"%>
<%@page import="com.dotmarketing.portlets.files.model.File"%>
<%@page import="com.dotmarketing.portlets.links.model.Link"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.business.RoleAPI"%>

<%
	RoleAPI roleAPI = APILocator.getRoleAPI();
	boolean isCMSAdmin = roleAPI.doesUserHaveRole(user, roleAPI.loadCMSAdminRole());
%>	

<script type="text/javascript" src="/dwr/interface/UserAjax.js"></script>
<script type="text/javascript" src="/dwr/interface/RoleAjax.js"></script>
<script type="text/javascript" src="/dwr/interface/BrowserAjax.js"></script>

<script type="text/javascript">
	
	dojo.require("dijit.Dialog");
	dojo.require("dijit.form.Form");
	dojo.require("dijit.form.TextBox"); 
	dojo.require("dijit.form.Textarea"); 
	dojo.require("dijit.form.ValidationTextBox"); 
	dojo.require("dijit.layout.TabContainer");
	dojo.require("dijit.layout.ContentPane");
	dojo.require("dijit.form.Button");
	dojo.require("dijit.form.CheckBox");
	dojo.require("dijit.Tree");
	dojo.require("dijit.Menu");
	dojo.require("dijit.MenuItem");
	dojo.require("dojox.grid.DataGrid");
	dojo.require("dojo.data.ItemFileWriteStore");
	dojo.require("dijit.layout.AccordionContainer");
	dojo.require("dojo.dnd.Source");
	dojo.require("dojo.dnd.Container")
	
	dojo.require("dotcms.dijit.form.HostFolderFilteringSelect");
	dojo.require("dotcms.dojo.data.UsersReadStore");

	var currentUserId = '<%= user.getUserId() %>';
	var isCMSAdmin = <%=isCMSAdmin%>;
	
	//I18n messages
	var roleKeyAlreadyExistsMesg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "role-key-already-exists")) %>';
	var roleNameAlreadyExistsMesg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "role-name-already-exists")) %>';
	var lockRoleConfirmMesg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "lock-role-confirm")) %>';
	var unlockRoleConfirmMesg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "unlock-role-confirm")) %>';
	var noUsersFoundMesg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "no-users-found")) %>';
	var removeRolesConfirmMesg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "remove-roles-confirm")) %>';
	var userRemovedFromRole = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "user-removed-from-role")) %>';
	var userGrantedRoleMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "user-granted-role")) %>';
	var atLeastOneToolRequiredMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "at-least-one-tool-required")) %>';
	var anotherLayoutAlreadyExistsMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "another-layout-already-exists")) %>';
	var removeLayoutConfirmMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "remove-layout-confirm")) %>';
	var roleLayoutConfigSavedMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "role-layout-config-saved")) %>';
	var removeMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "remove")) %>';
	var roleRemovedMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "role-removed")) %>';
	var roleLockedMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "role-locked")) %>';
	var roleUnlockedMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "role-unlocked")) %>';
	var nameMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Name")) %>';
	var emailMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Email")) %>';
	var grantedFromMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Granted-From")) %>';
	var layoutRemovedMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "layout-removed")) %>';
	var confirmRemoveRoleMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "confirm-remove-role")) %>';
	var confirmLockRoleMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "confirm-lock-role")) %>';
	var confirmUnlockRoleMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "confirm-unlock-role")) %>';
	var includedToolsMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "included-tools")) %>';
	var orderMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Sort-Order")) %>';
	var layoutSavedMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "layout-saved")) %>';
	
	//Layout Initialization
	


	var browserLoaded = false;
	
	function  resizeRoleBrowser(){

	    var viewport = dijit.getViewport();
	    var viewport_height = viewport.h;
	    var e =  dojo.byId("borderContainer");
	    dojo.style(e, "height", viewport_height -150 + "px");
		
	    var bc = dijit.byId('borderContainer');
	    if(bc != undefined){
			try{
		    	bc.resize();
			}catch(err){//DOTCMS-5749
				console.log(err);
			}
	    }
	
		var  e =  dojo.byId("roleTabsContainer");
		if(e){
	       	dojo.style(e, "height", viewport_height - 150 + "px");
		}

		var  e =  dojo.byId("usersTab");
		if(e){
       	  dojo.style(e, "height", viewport_height - 220 + "px");
		}
		
		var  e =  dojo.byId("permissionsTab");
		if(e){
		  dojo.style(e, "height", viewport_height - 220 + "px");
		}
		
		var  e =  dojo.byId("cmsTabsTab");
		if(e){
       	  dojo.style(e, "height", viewport_height - 220 + "px");
		}
		
       	dojo.query("#roleTabsContainer .dijitTabPaneWrapper").style("height", (viewport_height - 150) + "px");

	}
	
	// need the timeout for back buttons
	setTimeout(resizeRoleBrowser, 50);
	dojo.addOnLoad(resizeRoleBrowser);
	dojo.connect(window, "onresize", this, "resizeRoleBrowser");
		

	//Roles Tree Loading
	dojo.addOnLoad(function () {
		dwr.util.useLoadingMessage("<%=LanguageUtil.get(pageContext, "Loading")%>....");
		loadRolesTree ();
		dwr.engine.setErrorHandler(function(message, error) {
			showDotCMSErrorMessage("A system error as occurred " + message);
			console.log('Error ', message);
		});
		dojo.style('roleTabs', { display: 'none' });
	});
	
	//Function that kicks the loading of user roles
	// if a callback is passed it will be called after the loading finishes
	function loadRolesTree (callback) {
		loadRolesTreeCallback = callback;
		dojo.style(dojo.byId('noRolesFound'), { display: 'none' });
		dojo.style(dojo.byId('loadingRolesWrapper'), { display: '' });
		dojo.style(dojo.byId('rolesTreeWrapper'), { display: 'none' });
		RoleAjax.getRolesTree(false, "", true, buildUserRolesTreeCallback);
	}

	var rolesTree;	
	var flatTree = [];

	var treeRoleOptionTemplate = '${nodeName}';

	//Callback from the server with the tree of roles to load
	function buildUserRolesTreeCallback (tree) {

		//Flattening the tree for later used
		rolesTree = tree;
		flattenTree(tree);
		
		//constructing the data stores for dojo tree
		var treeData = { identifier: 'id', label: 'name', items: [ { id: 'root', name: 'Roles', top: true, 
            children: tree } ] };
            
		var store = new dojo.data.ItemFileReadStore({ data: treeData });
		
	    var treeModel = new dijit.tree.TreeStoreModel({
	        store: store,
	        query: { top:true },
	        rootId: "root",
	        rootLabel: "Root",
	        childrenAttrs: ["children"]
	    });

	    if(dijit.byId('rolesTree')) {
			flatTree.each(function (role) {
				if(dijit.byId('treeNode-' + role.id))
					dijit.registry.remove('treeNode-' + role.id);
			});
			dijit.registry.remove('rolesTree');
			dijit.registry.remove('treeNode-root');
	    }
    	initializeRolesTreeWidget(treeModel);

		if(loadRolesTreeCallback)
			loadRolesTreeCallback();
	}

	function initializeRolesTreeWidget(treeModel) {

		//Overriding the dojo tree to handle some of our own actions
		dojo.declare("dotcms.dojo.RolesTree", dijit.Tree, {
			
			//Returns the node text based on the treeRoleOptionTemplate html template
			getLabel: function(item) {
				return dojo.string.substitute(treeRoleOptionTemplate, { nodeId: item.id, nodeName: item.name });
			}, 

			getIconClass: function (item, opened) {
				var role = findRole(item.id, flatTree);
				var icon = "";
				if(role) {
					var locked = eval(norm(role.locked));
					if(locked) {
						return "lockIcon";
					}				
				}
				return "";
			}, 

			getIconStyle: function (item, opened) {
				var role = findRole(item.id, flatTree);
				var icon = "";
				if(role) {
					var locked = eval(norm(role.locked));
					if(locked) {
						return { };
					}				
				}
				return { width: 0, height: 0 };
			},
			
			onClick: function(item) {
				if(roleClickedDeferredHandler) {
					clearTimeout(roleClickedDeferredHandler);
				}
				var roleClickedDeferred = dojo.hitch(this, roleClicked, item.id);
				roleClickedDeferredHandler = setTimeout(roleClickedDeferred, 500);
			},

			_createTreeNode: function (args) {
				args.id = "treeNode-" + norm(args.item.id);
				return new dijit._TreeNode(args);
			}, 			
			
			//Some housekeeping after tree creation
			postCreate: function () {
				
				//Calling the parent
				this.inherited(arguments);
				//hiding the role loading image
				dojo.style(dojo.byId('loadingRolesWrapper'), { display: 'none' });
				//Showing the tree
				dojo.style(dojo.byId('rolesTreeWrapper'), { display: '' });
				
			}

		});
		
		//Rendering the tree
	   	var tree = new dotcms.dojo.RolesTree({
	        model: treeModel,
	        showRoot: false,
	        persist: false
	    }, "rolesTree");
		
		
		var menu = dijit.byId("roleTreeMenu");
        // when we right-click anywhere on the tree, make sure we open the menu
        menu.bindDomNode(dojo.byId('rolesTree'));
		
		dojo.connect(menu, "_openMyself", this, (function(e) {
			
            var tn = dijit.getEnclosingWidget(e.target);
			var item = tn.item;
			
			var role = findRole(item.id, flatTree);
			var locked = eval(norm(role.locked));
			var system = eval(norm(role.system));
			
			if(system) {
				dojo.style('editRoleMenu', { display: 'none' });
				dojo.style('deleteRoleMenu', { display: 'none' });
				if(locked){
					dojo.style('unlockRoleMenu', { display: '' });
					dojo.style('lockRoleMenu', { display: 'none' });
				}
				else{
					dojo.style('unlockRoleMenu', { display: 'none' });
					dojo.style('lockRoleMenu', { display: '' });
					}
			} else if(locked) {
				dojo.style('editRoleMenu', { display: 'none' });
				dojo.style('lockRoleMenu', { display: 'none' });
				dojo.style('deleteRoleMenu', { display: 'none' });
				dojo.style('unlockRoleMenu', { display: '' });
			} else {
				dojo.style('editRoleMenu', { display: '' });
				dojo.style('lockRoleMenu', { display: '' });
				dojo.style('deleteRoleMenu', { display: '' });
				dojo.style('unlockRoleMenu', { display: 'none' });
			}
			
			if(editRoleMenuRefHandle) dojo.disconnect(editRoleMenuRefHandle);
			if(lockRoleMenuRefHandle) dojo.disconnect(lockRoleMenuRefHandle);
			if(unlockRoleMenuRefHandle) dojo.disconnect(unlockRoleMenuRefHandle);
			if(deleteRoleMenuRefHandle) dojo.disconnect(deleteRoleMenuRefHandle);
			
			editRoleMenuRefHandle = dojo.connect(dojo.byId('editRoleMenu'), 'onclick', this, dojo.hitch(this, editRole.bind(this), norm(role.id)));
			lockRoleMenuRefHandle = dojo.connect(dojo.byId('lockRoleMenu'), 'onclick', this, dojo.hitch(this, lockRole.bind(this), norm(role.id)));
			unlockRoleMenuRefHandle = dojo.connect(dojo.byId('unlockRoleMenu'), 'onclick', this, dojo.hitch(this, unlockRole.bind(this), norm(role.id)));
			deleteRoleMenuRefHandle = dojo.connect(dojo.byId('deleteRoleMenu'), 'onclick', this, dojo.hitch(this, deleteRole.bind(this), norm(role.id)));
			
        }).bind(this));
	}
	
	var editRoleMenuRefHandle;
	var lockRoleMenuRefHandle;
	var unlockRoleMenuRefHandle;
	var deleteRoleMenuRefHandle;
	
	var currentSelectedNodeItem;
	
	//Action handler when the user type something to filter the roles tree
	var filterRolesHandle;
	function filterRoles(){
		if(filterRolesHandle)
			clearTimeout(filterRolesHandle);
		
		filterRolesHandle = setTimeout("filterRolesDeferred()", 600);
		
	}
	
	//The logic is executed deferred within 600 ms to handle multiple user keystrokes
	function filterRolesDeferred () {

		debugger;
		var tree = dijit.byId('rolesTree');
		
		var matchesCount = 0;

		dojo.style(dojo.byId('noRolesFound'), { display: 'none' });
		
		var query = dojo.byId('rolesFilter').value;

		if(query == '') {
			for(var i = 0; i < this.flatTree.length; i++) {
				var role = this.flatTree[i];
				var treeNode = dojo.byId('treeNode-' + norm(role.id))
				if(treeNode)
					dojo.style(treeNode, { display: ''})
			}
			return;	
		}

		var roles = searchRoles(query, flatTree);
		var branches = getRolesFlatUpBranches(roles);

		for(var i = 0; i < flatTree.length; i++) {
			var role = flatTree[i];
			var roleId = norm(role.id);
			
			var treeNode = dojo.byId('treeNode-' + roleId)
			if(treeNode) {
				if(!findRole(roleId, branches)) {
					dojo.style(treeNode, { display: 'none'});
					var dijitNode = dijit.byId(treeNode.id);
				} else {
					dojo.style(treeNode, { display: ''});
					var dijitNode = dijit.byId(treeNode.id);
					tree._expandNode(dijitNode)
					matchesCount++;
				}
			}
		}		
		
		if(matchesCount == 0)
			dojo.style(dojo.byId('noRolesFound'), { display: '' });
	}
	
	//Event handler for clearing the users filter
	function clearRolesFilter () {
		dojo.byId('rolesTree').style.display = '';
		dijit.byId('rolesFilter').attr('value', '');
		filterRolesDeferred()
	}
	
	//CRUD operations over roles
	
	//Executed when a grid row is clicked
	var newRole = false;
	function editRole(roleId) {
		
		if(!roleId)
			roleId = currentRoleId;
			
		var role = findRole(roleId, flatTree);
		
		currentRoleId = role.id;
		currentRole = role;
		
		isNewRole = false;
		initializeParentRolesSelect()
				
		setRoleName(role);

		dijit.byId('roleName').attr('value', norm(role.name));
		dijit.byId('roleKey').attr('value', norm(role.roleKey));
		var parentRoleId = norm(role.parent);
		dijit.byId('parentRole').setValue(parentRoleId == role.id?"0":parentRoleId);
		dijit.byId('editUsers').attr('value', norm(role.editUsers) == true?true:false);
		dijit.byId('editPermissions').attr('value', norm(role.editPermissions) == true?true:false);
		dijit.byId('editTabs').attr('value', norm(role.editLayouts) == true?true:false);
		dijit.byId('roleDescription').attr('value', norm(role.description));

		dijit.byId('addRoleDialog').show();
	}
	
	//Executed when adding a new user
	
	//Initializing the parent role select box 
	function initializeParentRolesSelect() {
		var selectData = {
				identifier: 'id', 
				label: 'name',
				items: []
			};

		var rolesIt = [];

		retrieveRolesData(0, rolesTree, rolesIt);

		selectData.items.push({ id: "0", name: "Root Role" })
		rolesIt.each(function(roleSt) {
			var roleName = addPadding(norm(roleSt.role.name), roleSt.level, "--> ")
			selectData.items.push({ id: norm(roleSt.role.id), name: roleName })
		});			

		var store = new dojo.data.ItemFileReadStore({ data: selectData });

		if(dijit.byId('parentRole')) {
			dijit.byId('parentRole').destroyRecursive(false);
			dojo.place('<select id="parentRole"></select>', "parentRoleWrapper", "only");
		}			

        var filteringSelect = new dijit.form.FilteringSelect({
            id: "parentRole",
            name: "parentRole",
            store: store,
            searchAttr: "name",
            required: true,
            labelType: "html"
        },
        "parentRole");
        
	}

	var isNewRole = false;
	function addNewRole() {

		isNewRole = true;
		initializeParentRolesSelect();
		
		//setRoleName(null);
		
		dojo.byId('addRoleErrorMessagesList').innerHTML = '';
		dijit.byId('roleName').reset();
		dijit.byId('roleKey').reset();
		dijit.byId('editUsers').reset();
		dijit.byId('editPermissions').reset();
		dijit.byId('editTabs').reset();
		dijit.byId('roleDescription').reset();		
		
		if (currentRoleId)
			dijit.byId('parentRole').attr('value', currentRoleId);
			
		dijit.byId('addRoleDialog').show();
	}

	function addPadding(str, count, padding) {
		var retStr = str;
		for(var i = 0; i < count; i++) {
			retStr = padding + retStr;
		}
		return retStr;
	} 

	function retrieveRolesData(level, roles, rolesIt) {
		roles.each((function(role) {
			rolesIt.push({ level: level, role: role })
			if(role.children && role.children.length > 0) {
				retrieveRolesData((level + 1), role.children, rolesIt)
			}
		}).bind(this));
	}
	
	//Handler when the user clicks the cancel button
	function cancelAddNewRole () {
		dijit.byId('addRoleDialog').hide();
	}
	
	//Handler to save the user details
	function saveRole() {

		if(!dijit.byId('newRoleForm').validate())
			return;
			
		
		var roleName = dijit.byId('roleName').attr('value');
		var roleKey = dijit.byId('roleKey').attr('value') == ''?null:dijit.byId('roleKey').attr('value');
		var parentRoleId = dijit.byId('parentRole').getValue();
		var canEditUsers = dijit.byId('editUsers').checked;
		var canEditPermissions = dijit.byId('editPermissions').checked;
		var canEditLayouts = dijit.byId('editTabs').checked;
		var description = dijit.byId('roleDescription').attr('value');
		
		currentRoleId = norm(currentRoleId);
		
		if(isNewRole) {
			RoleAjax.addNewRole(roleName, roleKey, (parentRoleId == 0?null:parentRoleId), canEditUsers, canEditPermissions, canEditLayouts, description, {
				callback: saveRoleCallback,
				exceptionHandler: saveRoleExceptionHandler
			});
		}
		else {
			RoleAjax.updateRole(currentRoleId, roleName, roleKey, (parentRoleId == 0?null:parentRoleId), canEditUsers, canEditPermissions, canEditLayouts, description, {
				callback: saveRoleCallback,
				exceptionHandler: saveRoleExceptionHandler
			});
		}
		

	}

	function saveRoleCallback (newRole) {
		
		dijit.byId('addRoleDialog').hide();
		loadRolesTree((function () {
			roleClicked(newRole.id);
		}).bind(this));
		
	}
	
	function saveRoleExceptionHandler (message, exception) {
		dojo.byId('addRoleErrorMessagesList').innerHTML = '';
		if(exception.javaClassName == 'com.dotmarketing.business.DuplicateRoleKeyException') {
			dojo.place("<li>" + roleKeyAlreadyExistsMesg + "</li>", "addRoleErrorMessagesList", "last");
		} else if (exception.javaClassName == 'com.dotmarketing.business.DuplicateRoleException') {
			dojo.place("<li>" + roleNameAlreadyExistsMesg + "</li>", "addRoleErrorMessagesList", "last");
		} else {
			throw exception;
		}
	}
	
	//Event handler then deleting a user
	function deleteRole(roleId) {
		if(!roleId)
			roleId = norm(currentRoleId);

		if(confirm(confirmRemoveRoleMsg))
			RoleAjax.deleteRole(roleId, deleteRoleCallback);
	}
	
	//Callback from the server to confirm a user deletion
	function deleteRoleCallback () {
		dojo.style(dojo.byId('roleTabs'), { display: 'none' });
		
		//dojo.style(dojo.byId('usersGridWrapper'), { visibility: 'hidden' });
		
		dojo.byId('deleteRoleButtonWrapper').style.display = 'none';
		dojo.byId('editRoleButtonWrapper').style.display = 'none';
		loadRolesTree();
		showDotCMSSystemMessage(roleRemovedMsg);
	}
	
	function lockRole(roleId) {
		var lockedRoleId = roleId;
		if(confirm(confirmLockRoleMsg))
			RoleAjax.lockRole(roleId, dojo.hitch(this, lockRoleCallback, roleId));
	}
	
	function unlockRole(roleId) {
		if(confirm(confirmUnlockRoleMsg))
			RoleAjax.unlockRole(roleId, dojo.hitch(this, unlockRoleCallback, roleId));
	}
	
	function lockRoleCallback (lockedRoleId) {
		loadRolesTree();
		if (norm(currentRoleId) == norm(lockedRoleId)) {
			dojo.byId('editRoleButtonWrapper').style.display = 'none';
		}
		showDotCMSSystemMessage(roleLockedMsg);
	}
	
	function unlockRoleCallback (unlockedRoleId) {
		loadRolesTree();
		if (norm(currentRoleId) == norm(unlockedRoleId) && !eval(norm(currentRole.system))) {
			dojo.byId('editRoleButtonWrapper').style.display = '';
		}
		showDotCMSSystemMessage(roleUnlockedMsg);
	}


	function setRoleName(role){
		var roleName = norm(role.name);
		if (role == null) {
			dojo.byId("displayRoleName1").innerHTML= '';
			dojo.byId("displayRoleName2").innerHTML= '';
			dojo.byId("displayRoleName3").innerHTML = '';
		} else {
			dojo.byId("displayRoleName1").innerHTML= roleName;
			dojo.byId("displayRoleName2").innerHTML= roleName;
			dojo.byId("displayRoleName3").innerHTML = roleName;
		}
	}



	/**
	 * Executed when the user clicks a role from the roles tree
	 */
	var roleClickedDeferredHandler;
	var currentRoleId;
	var currentRole;
	function roleClicked (roleId) {

		currentRoleId = roleId;
		var role = findRole(roleId, flatTree);
		currentRole = role;
		setRoleName(role);
		dojo.byId('roleKey').innerHTML = norm(role.roleKey);
		dojo.byId('rolePath').innerHTML = norm(role.FQN);
		
		if(eval(norm(role.system)) || eval(norm(role.locked))) {
			dojo.byId('editRoleButtonWrapper').style.display = 'none';
		} else {
			dojo.byId('editRoleButtonWrapper').style.display = '';
		}
		
		if(eval(norm(role.system)) || role.children.length > 0) {
			dojo.byId('deleteRoleButtonWrapper').style.display = 'none';
		} else {
			dojo.byId('deleteRoleButtonWrapper').style.display = '';
		}

		/* if(!eval(norm(role.editUsers))) {
			dojo.byId('editRoleUsersWrapper').style.display = 'none';
		} else {
			dojo.byId('editRoleUsersWrapper').style.display = '';
		} */
		
		renderCurrentTab();
	}
	
	//Setting up tab actions
	var currentSelectedTab = 'usersTab';
	dojo.addOnLoad(function () {
		dojo.subscribe("roleTabsContainer-selectChild", (function(child){
			currentSelectedTab = child.attr("id");
			renderCurrentTab();
		}).bind(this));
	});
	
	function renderCurrentTab () {
	 	switch (currentSelectedTab) {
			case 'usersTab':
				renderRoleUsers(currentRoleId);
				break;
			case 'permissionsTab':
				loadPermissionsForRole(norm(currentRoleId));
				break;
			case 'cmsTabsTab':
				loadRoleLayouts(currentRoleId);
				break;
		}
		resizeRoleBrowser();
		// DOTCMS-6233 need the timeout for users tab initially //
		setTimeout("resizeRoleBrowser();", 200);		
	}

	
	
	/* ********************************************************************** */
	//Users tab functions
	
	var currentBranchOfRoles;
	var usersGrid;
	var usersData;
	var noUsersFound = false;
	
	//Utility functions to render users grid cells
	function userCheckCellGetter(rowId, item) {
		return item;
	}

	function userCheckCellFormatter(item) {
		if(!item)
			return '';
			
		if(norm(item.grantedFromRoleId) == norm(currentRoleId) && eval(norm(currentRole.editUsers)))
			return '<input type="checkbox" class="userCheckbox" id="userChk' + item.id + '">';		
		return '';
	}
	
	//Initialization of users grid
	dojo.addOnLoad(function () {
		
		usersData = {
			identifier: 'id',
			label: 'id',
			items: [
			]
		};
 		var usersStore = new dojo.data.ItemFileReadStore({data: usersData });
		
		var usersGridLayout = [
		{
            field: 'check',
            name: '&nbsp;',
            width: '30px',
			get: userCheckCellGetter,
			formatter: userCheckCellFormatter
        },
		{
            field: 'fullName',
            name: nameMsg,
            width: '300px'
        },
        {
            field: 'emailAddress',
            name: emailMsg,
            width: '300px'
        },
        {
            field: 'grantedFrom',
            name: grantedFromMsg,
            width: 'auto'
        }];
		
        // create a new grid:
        usersGrid = new dojox.grid.DataGrid({
            query: {
                userId: '*'
            },
            store: usersStore,
            clientSort: false,
            structure: usersGridLayout,
			autoHeight: true
        },
        'usersGrid');		
		
        // Call startup, in order to render the grid:
        usersGrid.startup();

		dijit.byId('usersGrid').resize();
		dijit.byId('usersGrid').setSortIndex(1,true);

		
	});

	function renderRoleUsers (roleId) {
		currentBranchOfRoles = getRoleFlatUpBranch(roleId);
		var roleIds = [];
		currentBranchOfRoles.each((function (role) {
			roleIds.push(role.id + "");
		}).bind(this));
		RoleAjax.getUsersByRole(roleIds, getUsersByRoleCallback)
	}

	function getUsersByRoleCallback(userRolesMap){
		
		usersData = {
			identifier: 'id',
			label: 'id',
			items: [  ]
		};
		var found = 0;
		currentBranchOfRoles.each((function (role) {
			var userMapList = userRolesMap[role.id];
			userMapList.each((function (user) {
				if(user.userId == 'system') return;
				user.grantedFrom = norm(role.name);
				user.grantedFromRoleId = norm(role.id);
				user.id = user.userId;
				found++;
				usersData.items.push(user);
			}).bind(this));
		}).bind(this));
		
		if(found == 0) {
			noUsersFound = true;
			var usersStore = new dojo.data.ItemFileWriteStore({data: {
				identifier: 'id',
				label: 'id',
				items: [ {id: '0', fullName: noUsersFoundMesg, emailAddress: "", grantedFrom: "", grantedFromRoleId: "" } ]
			} });
			dijit.byId("removeUsersButton").setAttribute("disabled", true);
		} else {
	 		var usersStore = new dojo.data.ItemFileWriteStore({data: usersData });
	 		dijit.byId('usersGrid').setAttribute('rowsPerPage',Math.round(found/2));
			dijit.byId("removeUsersButton").setAttribute("disabled", false);
		}
		
		if(!eval(norm(currentRole.editUsers))) {
			dojo.style(dijit.byId('removeUsersButton').domNode, { display: 'none' });
		}

		usersGrid.setStore(usersStore);
		
		dojo.style(dojo.byId('roleTabs'), { display: '' });
		
		dojo.style(dojo.byId('loadingUsersWrapper'), { display: 'none' });
		dojo.style(dojo.byId('usersGridWrapper'), { visibility: 'visible' });

        // Call startup, in order to render the grid:
        usersGrid.startup();
		dijit.byId('usersGrid').resize();
		dijit.byId('usersGrid').setSortIndex(1,true);

	}
	
	function filterUserRoles () {
		
	 	var value = dijit.byId('userRolesFilter').attr('value');
		var filterRegex = new RegExp('.*' + value + '.*', 'i');
		var filteredData = {
			identifier: 'id',
			label: 'id',
			items: [
			]
		};
		dojo.forEach(usersData.items, function (user) {
			if(!value || value == '') {
				filteredData.items.push(user);
			} else if(filterRegex.test(norm(user.fullName))) {
				filteredData.items.push(user);
			}
		}, this);
 		var usersStore = new dojo.data.ItemFileWriteStore({data: filteredData });
		usersGrid.setStore(usersStore);

	}
	
	function clearUserRolesFilter () {
		dijit.byId('userRolesFilter').attr('value', '');
		filterUserRoles ()
	}
	
	var removedUserIds;

	function removeUsersInRole () {
		if(!confirm(removeRolesConfirmMesg)) {
			return;
		}
		var userIdsSelected = [];
		usersData.items.each((function (user) {
			var checkbox = dojo.byId('userChk' + norm(user.id));
			if(checkbox && checkbox.checked) {
				userIdsSelected.push(norm(user.id));
			}
		}).bind(this));

		RoleAjax.removeUsersFromRole(userIdsSelected, norm(currentRoleId), removeUsersFromRoleCallback);
		removedUserIds = userIdsSelected;
	}
	
	function removeUsersFromRoleCallback () {
		
		var newItems = [];
		
		for(var i = 0; i < usersData.items.length; i++) {
			if(dojo.indexOf(removedUserIds, norm(usersData.items[i].id)) < 0) {
				newItems.push(usersData.items[i]);
			}
		}

		usersData.items = newItems;
		var usersStore = new dojo.data.ItemFileWriteStore({data: usersData });
		usersGrid.setStore(usersStore);

		showDotCMSSystemMessage(userRemovedFromRole);
	}
	
	
	//Grant User
	var addedUserId;
	function grantUser () {
		addedUserId = dijit.byId('grantUserSelect').getValue()
		if(addedUserId.indexOf('user-') == 0) {
			addedUserId = addedUserId.substring(5);
		}
		for(var i = 0; i < usersData.items.length; i++) {
			
			if(norm(usersData.items[i].id) == addedUserId)
				return;
		}
		RoleAjax.addUserToRole(addedUserId, norm(currentRoleId), addUserToRoleCallback);
	}
	
	function addUserToRoleCallback (user) {

		showDotCMSSystemMessage(userGrantedRoleMsg);

		user.grantedFrom = norm(currentRole.name);
		user.grantedFromRoleId = norm(currentRole.id);
		user.id = user.userId;

		usersData.items.push(user);
		var usersStore = new dojo.data.ItemFileWriteStore({data: usersData });
		usersGrid.setStore(usersStore);

		dojo.style(dijit.byId('removeUsersButton').domNode, { display: '' });
		
	}

	/* ********************************************************************** */
	//Role Layouts functions
	
	var currentRoleLayouts = [];
	var allLayoutsList = [];
	
	function loadRoleLayouts (roleId) {		
	
		roleId = norm(roleId);
		
		if(!eval(norm(currentRole.editLayouts)))
			dijit.byId("saveRoleLayoutsButton").setAttribute("disabled", true);
		else
			dijit.byId("saveRoleLayoutsButton").setAttribute("disabled", false);

		dojo.style(dojo.byId('roleLayoutsGridWrapper'), { visibility: 'hidden'});
		dojo.style(dojo.byId('loadingRoleLayoutsWrapper'), { display: ''});

		RoleAjax.loadRoleLayouts(roleId, dojo.hitch(this, initializeLayoutsGrid, roleId));
	}
	
	function initializeLayoutsGrid (roleId, roleLayouts) {
		currentRoleLayouts = roleLayouts;
		if(!dijit.byId('roleLayoutsGrid')) {
			RoleAjax.getAllLayouts(dojo.hitch(this, buildLayoutsGrid, norm(roleId), roleLayouts))
		} else {
			reloadLayoutsGrid()
		}
	}
	
	function reloadLayoutsGrid() {

		if(!eval(norm(currentRole.editLayouts)))
			dijit.byId("saveRoleLayoutsButton").setAttribute("disabled", true);
		else
			dijit.byId("saveRoleLayoutsButton").setAttribute("disabled", false);

		dojo.style(dojo.byId('roleLayoutsGridWrapper'), { visibility: 'hidden'});
		dojo.style(dojo.byId('loadingRoleLayoutsWrapper'), { display: ''});

		RoleAjax.getAllLayouts(dojo.hitch(this, buildLayoutsGrid, currentRoleId, currentRoleLayouts))
	}
	
	function buildLayoutsGrid(roleId, roleLayouts, layoutsList) {
		
		dojo.style(dojo.byId('roleLayoutsGridWrapper'), { visibility: 'visible'});
		dojo.style(dojo.byId('loadingRoleLayoutsWrapper'), { display: 'none'});

		allLayoutsList = layoutsList;
		
		layoutsData = {
			identifier: 'id',
			label: 'id',
			items: layoutsList
		};
 		var layoutsStore = new dojo.data.ItemFileReadStore({data: layoutsData });
		
		
		if(!dijit.byId('roleLayoutsGrid')) {
			var gridBox = dojo.contentBox("roleLayoutsGrid");
			var col1Width = parseInt(gridBox.w * 2 / 100);
			var col2Width = parseInt(gridBox.w * 30 / 100);
			var col3Width = parseInt(gridBox.w * 61 / 100);
			var col4Width = gridBox.w - col1Width - col2Width - col3Width - 30;

			var layoutsGridLayout = [
			{
	            field: 'check',
	            name: ' ',
	            width: col1Width + 'px',
				get: layoutItemCellGetter,
				formatter: layoutCheckCellFormatter
	        },
			{
	            field: 'name',
	            name: nameMsg,
	            width: col2Width + 'px'
	        },
	        {
	            field: 'portletTitles',
	            name: includedToolsMsg,
				get: layoutItemCellGetter,
				formatter: layoutPortletTitlesCellFormatter,
	            width: col3Width + 'px'
	        },
	        {
	            field: 'tabOrder',
	            name: orderMsg,
	            width: col4Width + 'px'
	        }];
			
	        // create a new grid:
	        layoutsGrid = new dojox.grid.DataGrid({
	            query: {
	                id: '*'
	            },
	            store: layoutsStore,
	            clientSort: true,
	            structure: layoutsGridLayout
	        },
	        'roleLayoutsGrid');		
			
			dojo.connect(dijit.byId("roleLayoutsGrid"), 'onRowClick', this, roleLayoutClicked.bind(this))
	
			layoutsGrid.canSort(0,false);
			
	        // Call startup, in order to render the grid:
	       	layoutsGrid.startup();			
		} else {
			dijit.byId('roleLayoutsGrid').setStore(layoutsStore);
		}

		
	}
	
	//Cell formatters for the layouts grid 
	function layoutItemCellGetter (idx, item) {
		return item;
	}

	function layoutCheckCellFormatter(item){
		var disabled = '';
		if(!eval(norm(currentRole.editLayouts)))
			disabled = 'disabled="disabled"';
					
		if(item) {
			if(findLayout(item.id, currentRoleLayouts)) {
				return '<input type="checkbox" checked="checked" id="layout_chk_' + item.id + '" ' + disabled + '>';
			}
			else
				return '<input type="checkbox" id="layout_chk_' + item.id + '" ' + disabled + '>';
		}
	}
	
	function layoutPortletTitlesCellFormatter(item) {
		
		if(item) {
			var titles = "";
			var first = true;
			item.portletTitles.each((function (title) {
				if(!first) {
					titles += ", "
				}
				titles += title
				first = false;
			}).bind(this))
			return titles;
		}
		return "";
		
	}
	
	var editLayoutDelayedHandler;
	function roleLayoutClicked(evt) {
		if(evt.cellIndex == 1) {
			if(editLayoutDelayedHandler)
				clearTimeout(editLayoutDelayedHandler);
			var editLayoutDelayed = dojo.hitch(this, editLayout, allLayoutsList[evt.rowIndex].id);
			editLayoutDelayedHandler = setTimeout(editLayoutDelayed, 500);
		}
	}
	
	//--------    Layout form functions ---------
	
    dojo.addOnLoad(function() {
		initializePortletInfoList();
	});

	var allPortletInfoList;
	var portletsInLayout = [];
	var newLayout = false;
	var currentLayout;
	
	var portletListItemTemplate = 
	'<div id="listItem-${portletId}" class="portletItem">' +
	'	<div class="yui-gc">' +
	'		<div class="yui-u first portletTitle">' +
	'			${portletTitle}' +
	'		</div>' +
	'		<div class="yui-u removePorletButton">' +
	'			<button id="removePortletButton${portletId}" dojoType="dijit.form.Button" type="button">' + removeMsg + '</button>' +
	'		</div>' +
	'	</div>' +
	'</div>';

	var portletsListSource;
	
	function addPortletToHTMLList (portletId, portletTitle) {

		var itemHTML = getPortletItemHTML(portletId, portletTitle);
		portletsListSource.insertNodes(false, [itemHTML]);
		registerPortletItemButton(portletId, portletTitle);
	
	}
	
	function getPortletItemHTML (portletId, portletTitle) {
		portletId = norm(portletId);
		var html = dojo.string.substitute(portletListItemTemplate, { portletTitle: portletTitle, portletId: portletId })
		return html;
	}
	
	function registerPortletItemButton (portletId, portletTitle) {
	
		portletId = norm(portletId);
		
		if(dijit.byId("removePortletButton" + portletId))
			dijit.registry.remove("removePortletButton" + portletId);
			
        var button = new dijit.form.Button({ }, "removePortletButton" + portletId);
		var handler = dojo.hitch(this, removePortletFromList.bind(this), portletId)
		dojo.connect(button, 'onClick', this, handler)
		portletsInLayout.push({ portletTitle: portletTitle, portletId: portletId });	
	
	}
	
	function editLayout(layoutId) {

		portletsInLayout = [];
		
		if (!allPortletInfoList) {
			initializePortletInfoList(dojo.hitch(this, editLayout, layoutId));
		}
		else {
			dojo.byId('portletsListWrapper').innerHTML = '<ul id="portletsList"></ul>';
			currentLayout = findLayout(layoutId, allLayoutsList);
			dijit.byId('layoutName').attr('value', currentLayout.name);
			dijit.byId('layoutDescription').attr('value', currentLayout.description);
			dijit.byId('layoutOrder').attr('value', currentLayout.tabOrder);
			var itemsHTML = new Array();
			for (var i = 0; i < currentLayout.portletTitles.length; i++) {
				var title = currentLayout.portletTitles[i];
				var id = currentLayout.portletIds[i];
				itemsHTML.push(getPortletItemHTML(id, title));
			}

			portletsListSource = new dojo.dnd.Source("portletsList");
			portletsListSource.insertNodes(false, itemsHTML);
			
			for (var i = 0; i < currentLayout.portletTitles.length; i++) {
				var title = currentLayout.portletTitles[i];
				var id = currentLayout.portletIds[i];
				registerPortletItemButton(id, title);
			}

			newLayout = false;
			dojo.style('deleteLayoutButtonWrapper', {
				display: ''
			})
			dojo.byId('addLayoutErrorMessagesList').innerHTML = ''
			dijit.byId('newLayouDialog').show();
		}
		
	}
	
	function createNewLayout () {
		if(!allPortletInfoList) {
			initializePortletInfoList(createNewLayout);
		} else {
			newLayout = true;
			dijit.byId('layoutName').attr('value', '');
			dijit.byId('layoutDescription').attr('value', '');
			dijit.byId('layoutOrder').attr('value', '0');
			dojo.style('deleteLayoutButtonWrapper', { display: 'none' })
			dojo.byId('addLayoutErrorMessagesList').innerHTML = ''
			dojo.byId('portletsListWrapper').innerHTML = '<ul id="portletsList"></ul>';
			portletsListSource = new dojo.dnd.Source("portletsList");			
			dijit.byId('newLayouDialog').show();
		}
	}
	
	function initializePortletInfoList(callback) {
		tempCallback = callback;
		RoleAjax.getAllAvailablePortletInfoList(initializePortletInfoListCallback);
	}
	
	function initializePortletInfoListCallback(allPortletsList) {
		
		allPortletInfoList = allPortletsList;
		
		var portletsData = {
			identifier: 'id',
			label: 'title',
			items: allPortletInfoList
		};
		
 		var portletsStore = new dojo.data.ItemFileReadStore({data: portletsData });

	    new dijit.form.FilteringSelect({
            id: "portletList",
            name: "portletList",
            searchAttr: "title",
            store: portletsStore,
			required: false
        },
        "portletList");
		
		if(tempCallback)
			tempCallback();

	}


		
	function addPortletToLayoutList() {
		
		var portletId = dijit.byId('portletList').attr('value');
		var portletTitle = dijit.byId('portletList').attr('displayedValue');

		if(!portletId || portletId == '')
			return;
			
		if(indexOfPortlet(portletId, portletsInLayout) >= 0)
			return;
		
		addPortletToHTMLList(portletId, portletTitle);

	}
	
	function removePortletFromList(portletId) {
		for(var i = 0; i < portletsInLayout.length; i++) {
			if(portletsInLayout[i].portletId == portletId) {
				portletsInLayout.splice(i, 1);
				break;
			}
		}
		dijit.registry.remove('removePortletButton' + portletId)
		dojo.destroy(dojo.byId('listItem-' + portletId));
	}
	
	function saveLayout() {
		
		if(!dijit.byId('newLayoutForm').validate())
			return;

		if(portletsInLayout.length == 0) {
			dojo.byId('addLayoutErrorMessagesList').innerHTML = atLeastOneToolRequiredMsg
			return;			
		}
		
		var name = dijit.byId('layoutName').attr('value');
		var order = dijit.byId('layoutOrder').attr('value');
		var description = dijit.byId('layoutDescription').attr('value');
		var porletIds = getPortletsList();
		if(newLayout)
			RoleAjax.addNewLayout(name, description, order, porletIds, 
				{ callback: saveLayoutCallback.bind(this), exceptionHandler: saveLayoutException.bind(this) });
		else
			RoleAjax.updateLayout(norm(currentLayout.id), name, description,order, porletIds, 
				{ callback: saveLayoutCallback.bind(this), exceptionHandler: saveLayoutException.bind(this) });
					
	}

	function getPortletsList() {
		var list = dojo.query('#portletsList li div.portletItem');
		var portletIds = [];
		dojo.forEach(list, function (elem) {
			var id = elem.id.split('-')[1];
			if(id != '')
				portletIds.push(id);
		});
		return portletIds;
	}
	
	function saveLayoutCallback () {
		
		reloadLayoutsGrid();

		dijit.byId('newLayouDialog').hide();
		
		showDotCMSSystemMessage(layoutSavedMsg);

	}
	
	function saveLayoutException(message, exception) {
		if(exception.javaClassName = 'com.dotmarketing.business.LayoutNameAlreadyExistsException') {
			dojo.byId('addLayoutErrorMessagesList').innerHTML = anotherLayoutAlreadyExistsMsg
			return;			
		}
	}
	
	function cancelEditLayout() {
		dijit.byId('newLayouDialog').hide();
		dijit.byId('layoutName').attr('value', '')
		dijit.byId('layoutOrder').attr('value', '0')
		dojo.byId('portletsListWrapper').innerHTML = ''
	}
	
	function deleteLayout() {
		if(!confirm(removeLayoutConfirmMsg))
			return;
		RoleAjax.deleteLayout(norm(currentLayout.id), deleteLayoutCallback)
	}
	
	function deleteLayoutCallback () {
		dijit.byId('newLayouDialog').hide();
		reloadLayoutsGrid();
		showDotCMSSystemMessage(layoutRemovedMsg);
	}
	
	function saveRoleLayouts() {
		var checkedLayouts = [];
		allLayoutsList.each((function(layout) {
			var checkbox = dojo.byId('layout_chk_' + norm(layout.id));
			if(checkbox && checkbox.checked) {
				checkedLayouts.push(norm(layout.id));
			}
		}).bind(this));
		RoleAjax.saveRoleLayouts(norm(currentRoleId), checkedLayouts, saveRoleLayoutsCallback);
	}
	
	function saveRoleLayoutsCallback() {
		showDotCMSSystemMessage(roleLayoutConfigSavedMsg);
	}
	
	/* ********************************************************************** */
	//Utility functions
	
	//Normalizes value (values coming from the server are sometimes coming within arrays of a single string value)
	function norm(value) {
		return dojo.isArray(value)?value[0]:value;
	}
	
	//Takes the hierarchically tree of roles  
	function flattenTree(tree){
		flatTree = [];
		flattenTreeRec(tree);
	}
	
	function flattenTreeRec (tree) {
		tree.each(function (node) {
			flatTree.push(node);
			if (node.children)
				flattenTreeRec(node.children);
		});
	}
	
	//Used to filter roles
	function searchRoles(query, roles){
		var matches = [];
		for(var i = 0; i < roles.length; i++) {
			var roleName = dojo.isArray(roles[i].name)?roles[i].name[0]:roles[i].name;
			var regexQuery = new RegExp(query, "i");
			if(roleName.match(regexQuery))
				matches.push(roles[i]);
		}
		return matches;
	}
	
	//Retrieves a plain list of roles up in the same branch of the given roles
	function getRolesFlatUpBranches(roles) {
		branches = []
		
		for(var i = 0; i < roles.length; i++) {
			var role = roles[i];
			branches.push(role);
			var parentId = dojo.isArray(role.parent)?role.parent[0]:role.parent;
			var roleId = dojo.isArray(role.id)?role.id[0]:role.id;
	
			while(parentId && parentId != roleId) {
				role = findRole(role.parent, flatTree);
				branches.push(role);
				var parentId = dojo.isArray(role.parent)?role.parent[0]:role.parent;
				var roleId = dojo.isArray(role.id)?role.id[0]:role.id;						
			}

		}
		return branches;
	}
	
	//Retrieves a plain list of roles up in the same branch of the given role
	function getRoleFlatUpBranch(roleId) {
		branches = []
		
		var role = findRole(roleId, flatTree);
		if(role == null)
			return [];
		branches.push(role);
		var parentId = dojo.isArray(role.parent)?role.parent[0]:role.parent;
		var roleId = dojo.isArray(role.id)?role.id[0]:role.id;

		while(parentId && parentId != roleId) {
			role = findRole(role.parent, flatTree);
			branches.push(role);
			var parentId = dojo.isArray(role.parent)?role.parent[0]:role.parent;
			var roleId = dojo.isArray(role.id)?role.id[0]:role.id;						
		}

		return branches;
	}
	
	//Retrieves a plain list of roles underneath in the same branch of the given role
	function getRoleFlatDownBranch(id) {
		branches = [];
		ids = [];
		var role = findRole(id, flatTree);
		
		branches.push(role);
		var children = role.children;
		
		for(var i = 0; children && i < children.length; i++) {
			var id = dojo.isArray(children[i].id)?children[i].id[0]:children[i].id;
			ids.push(id);
		}

		while(ids.length > 0) {
			var roleId = ids.pop();
			role = findRole(roleId, flatTree);
			branches.push(role);
			var children = role.children;
			if(children != null) {
				for(var i = 0; i < children.length; i++) {
					var id = dojo.isArray(children[i].id)?children[i].id[0]:children[i].id;
					ids.push(id);
				}
			}
		}

		return branches;
	}
	

	
	//Finds a role within the given list of roles
	function findRole(roleid, roles) {
		for(var i = 0; i < roles.length; i++) {
			var id1 = dojo.isArray(roles[i].id)?roles[i].id[0]:roles[i].id;
			var id2 = dojo.isArray(roleid)?roleid[0]:roleid;
			if(id1 == id2)
				return roles[i];
		}
		return null;
	}
	
	function indexOfPortlet(portletId, portlets) {
		for(var i = 0; i < portlets.length; i++) {
			if(portlets[i].portletId == portletId)
				return i;
		}
		return -1;
	}

	function findLayout(layoutid, layoutList) {
		for(var i = 0; i < layoutList.length; i++) {
			var id1 = dojo.isArray(layoutList[i].id)?layoutList[i].id[0]:layoutList[i].id;
			var id2 = dojo.isArray(layoutid)?layoutid[0]:layoutid;
			if(id1 == id2)
				return layoutList[i];
		}
		return null;
	}

	
</script>

