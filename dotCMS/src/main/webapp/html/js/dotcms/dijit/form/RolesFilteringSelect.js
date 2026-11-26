/**
 * To include the dijit into your page
 * 
 * JS Side
 * 
 * <script type="text/javascript">
 * 	dojo.require("dotcms.dijit.form.RolesFilteringSelect");
 * 
 * ...
 * 
 * </script>
 * 
 * HTML side
 * 
 * <div id="roleSelector" dojoType="dotcms.dijit.form.RolesFilteringSelect"></div>
 * 
 * 
 * To retrieve values from the dijit
 * 
 * To retrieve the selected inode 
 * dijit.byId('roleSelector').attr('value') // Will return something like '6fa459ea-ee8a-3ca4-894e-db77e160355e' the id of the role
 * 
 * To retrieved the name of the selected role
 * dijit.byId('roleSelector').attr('displayedValue')  // Will return something like 'CMS Administrator' the name of the role
 * 
 * To retrieved the selected js item
 * dijit.byId('roleSelector').attr('selectedItem') //Will return a js object like { id: '6fa459ea-ee8a-3ca4-894e-db77e160355e', name: 'CMS Administrator' }
 * 
 * 
 * Code
 * 
 * Dijit 
 * 
 * dotCMS/html/js/dotcms/dijit/form/
 * 								 RolesFilteringSelect.js
 * 								 RolesFilteringSelect.html
 * 
 * 
 * REST endpoint used by the dijit
 *
 * GET /api/v1/roles?fullHierarchy=true&permissionable={permissionable}
 *
 * Set the permissionable attribute to the id of the asset whose permissions are being edited:
 * a user without the Roles or Users portlet can read the roles when they may edit that asset's
 * permissions.
 */
dojo.provide("dotcms.dijit.form.RolesFilteringSelect");

dojo.require("dijit._Widget");
dojo.require("dijit._Templated");
dojo.require("dojo.data.ItemFileReadStore");
dojo.require("dijit.tree.TreeStoreModel");
dojo.require("dijit.Tree");

dojo.declare("dotcms.dijit.form.RolesFilteringSelect", [dijit._Widget, dijit._Templated], {
	
	excludeRoles:"",
	permissionable:"",
	templatePath: dojo.moduleUrl("dotcms", "dijit/form/RolesFilteringSelect.html"),

	postCreate: function (elem) {
		var url = dojo.moduleUrl("dotcms",
			"dijit/form/RolesFilteringSelectTree.html");
		var templateString = dojo._getText(url);
		var html = dojo.string.substitute(templateString, {id: this.id});
		var domObj = dojo._toDom(html);
		if(null != dojo.byId(domObj.id)){
           dojo.destroy(domObj.id);
        }
        dojo.place(domObj, dojo.body(), 'last');
		this.rolesTreeContainer = dojo.byId('rolesTreeContainer-' + this.id);
		this.rolesTreeWrapper = dojo.byId('rolesTreeWrapper-' + this.id);
		this.rolesTree = dojo.byId('rolesTree-' + this.id);
		
		this._loadRoles();
		this.bodyClickHandle = dojo.connect(dojo.body(), 'onclick', this, this._onBodyClick);
	},

	/**
	 * Loads the whole role hierarchy and hands it to postCreateCallback as tree items, leaving
	 * out the roles listed in excludeRoles and the workflow special roles.
	 */
	_loadRoles: function () {
		var url = '/api/v1/roles?fullHierarchy=true';
		if (this.permissionable) {
			url += '&permissionable=' + encodeURIComponent(this.permissionable);
		}
		var excluded = this.excludeRoles ? this.excludeRoles.split(',') : [];
		var self = this;
		fetch(url, { credentials: 'same-origin', headers: { 'Accept': 'application/json' } })
			.then(function (response) {
				if (!response.ok) {
					throw new Error('Roles could not be loaded: HTTP ' + response.status);
				}
				return response.json();
			})
			.then(function (data) {
				self.postCreateCallback(self._toTreeItems(data.entity || [], excluded));
			})
			.catch(function (error) {
				console.error(error);
				self.postCreateCallback([]);
			});
	},

	/**
	 * Turns the REST role views into the items the tree and the Permissions tab read: the role
	 * fields under the names the tab expects, and the children under "children", sorted by name.
	 */
	_toTreeItems: function (roles, excluded) {
		var items = [];
		for (var i = 0; i < roles.length; i++) {
			var role = roles[i];
			if (excluded.indexOf(role.id) >= 0 || this._isWorkflowRole(role)) {
				continue;
			}
			items.push({
				id: role.id,
				name: role.name,
				description: role.description,
				roleKey: role.roleKey,
				DBFQN: role.dbfqn,
				FQN: role.fqn,
				parent: role.parent,
				editPermissions: role.editPermissions,
				editUsers: role.editUsers,
				editLayouts: role.editLayouts,
				locked: role.locked,
				system: role.system,
				children: this._toTreeItems(role.roleChildren || [], excluded)
			});
		}
		items.sort(function (a, b) { return a.name < b.name ? -1 : (a.name > b.name ? 1 : 0); });
		return items;
	},

	_isWorkflowRole: function (role) {
		return !!role.roleKey && role.roleKey.indexOf('cms_workflow_any_who_can_') === 0;
	},

	postCreateCallback: function(treeData) {

		this.treeData = treeData;
		
		var dataModel = { identifier: 'id', label: 'name', items: [ { id: 'root', name: 'Roles', top: true, 
            children: treeData } ] };
            
		var dataStore = new dojo.data.ItemFileReadStore({ data: dataModel });
		
	    var treeStore = new dijit.tree.TreeStoreModel({
	        store: dataStore,
	        query: { top:true },
	        rootId: "root",
	        rootLabel: "Root",
	        childrenAttrs: ["children"]
	    });
		
	   	this.tree = new dijit.Tree({

			id: this.id + "-rolesTree",
			
	        model: treeStore,

	        persist: false,
	        
	        showRoot: false,

			getIconClass: (function (item, opened) {
				if(opened) return "dijitRoleOpened";
				else return "dijitRoleClosed";
			}).bind(this),

			_createTreeNode: function (args) {
				args.id = this.id + "-treeNode-" + args.item.id;
				return new dijit._TreeNode(args);
			}

	    }, this.rolesTree);
		dojo.connect(this.tree, 'onClick', this, this._onTreeItemClick);
		
		this._flattenTree(treeData);

	},
	
	uninitialize : function (event) {
		dojo.disconnect(this.bodyClickHandle);
	},
	
	showHideRolesList: function (event) {
		
		this.filter = '';
		var display = dojo.style(this.rolesTreeWrapper, 'display');
		if(display == '' || display == 'block')
			this.hideRolesList();
		else
			this.showRolesList();
	},
	
	showRolesList: function (event) {
		var selectCoords = dojo.coords(this.rolesFilteringSelectWrapper, true)
		dojo.style(this.rolesTreeWrapper, { left: selectCoords.x + "px", top: (selectCoords.y + selectCoords.h) + "px", display: ''});
	},
	
	hideRolesList: function (event) {
		dojo.style(this.rolesTreeWrapper, { display: 'none' });
	},
	
	/**
	 * Stub method that you can use dojo.connect to catch every time a user selects a role from the tree
	 * @param {Object} item
	 */
	itemSelected: function (item) {
		
	},
	
	filterTree : function (event) {
		if(this.filterTreeDefHandler)
			clearTimeout(this.filterTreeDefHandler);
		this.filterTreeDefHandler = setTimeout(dojo.hitch(this, this._filterTreeDef), 500);

		this.attr('value', null); 
		this.attr('displayedValue', null); 
		this.attr('selectedItem', null); 
		this.roleSelectedId.value = '';

	},
		
	_filterTreeDef : function (event) {

		var query = this.roleSelectedName.value;

		if(query == '') {
			for(var i = 0; i < this.flatTree.length; i++) {
				var role = this.flatTree[i];
				var treeNode = dojo.byId(this.id + '-tree-treeNode-' + this._norm(role.id));
				if(treeNode){
                    dojo.style(treeNode, { display: ''});
                }
			}
			return;	
		}

		var roles = this._searchRoles(query, this.flatTree);
		var branches = this._getRolesFlatUpBranches(roles);

		for(var i = 0; i < this.flatTree.length; i++) {
			var role = this.flatTree[i];
			var roleId = this._norm(role.id);
			
			var treeNode = dojo.byId(this.id + '-rolesTree-treeNode-' + roleId);
			if(treeNode) {
				if(!this._findRole(roleId, branches)) {
					dojo.style(treeNode, { display: 'none'});
					var dijitNode = dijit.byId(treeNode.id);
				} else {
					dojo.style(treeNode, { display: ''});
					var dijitNode = dijit.byId(treeNode.id);
					this.tree._expandNode(dijitNode)
				}
			}
		}
		
		var display = dojo.style(this.rolesTreeWrapper, 'display');
		if(display == 'none') {
			this.showRolesList();
		}

	},
	
	_searchRoles: function(query, roles){
		var matches = [];
		for (var i = 0; i < roles.length; i++) {
			var roleName = this._norm(roles[i].name);
			var regexQuery = new RegExp(query, "i");
			if (roleName.match(regexQuery)) 
				matches.push(roles[i]);
		}
		return matches;
	},
	
	_flattenTree: function (tree){
		this.flatTree = [];
		this._flattenTreeRec(null, tree);
	},
	
	_flattenTreeRec: function  (treeParent, tree) {
		for(var i = 0; i < tree.length; i++) {
			var node = tree[i];
			node.parent = treeParent;
			this.flatTree.push(node);
			if (node.children)
				this._flattenTreeRec(node, node.children);
		}
	},
	
	_getRolesFlatUpBranches: function (roles) {
		
		var branches = [];
		
		for(var i = 0; i < roles.length; i++) {
			var role = roles[i];
			branches.push(role);
			
			if(role.parent)
				var parentId = this._norm(role.parent.id);
			else
				var parentId = null; 
			var id = this._norm(role.id);
	
			while(parentId && parentId != id) {
				role = this._findRole(parentId, this.flatTree);
				branches.push(role);
				if(role.parent)
					var parentId = this._norm(role.parent.id);
				else
					var parentId = null; 
				var id = this._norm(role.id);						
			}

		}
		return branches;
	},
	
	_findRole: function (id, roles) {
		for(var i = 0; i < roles.length; i++) {
			var idIt = this._norm(roles[i].id);
			id = this._norm(id);
			if(id == idIt)
				return roles[i];
		}
		return null;
	},
	
	_norm: function (value) {
		return dojo.isArray(value)?value[0]:value;
	},	
	
	_onTreeItemClick: function(item) { 

		this.attr('value', this._norm(item.id)); 
		this.attr('displayedValue', this._norm(item.name)); 
		this.attr('selectedItem', item); 
		this.roleSelectedName.value = this._norm(item.name);
		this.roleSelectedId.value = this._norm(item.id);
		this.hideRolesList();
		this.itemSelected(item);

	},	
	
	_onBodyClick: function (event) {
		if(!this._within(event, this.rolesTreeWrapper) && !this._within(event, this.rolesFilteringSelectWrapper))
			this.hideRolesList();
	},
	
	_within: function (event, elem) {
		var x = event.clientX;
		var y = event.clientY;
		var coords = dojo.coords(elem);
		
		if(x < coords.x || x > coords.x + coords.w || y < coords.y || y > coords.y + coords.h) {
			return false;
		}
		return true;		
	}
	
});