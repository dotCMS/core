dojo.provide("dotcms.dojo.data.UsersReadStore");

dojo.require("dotcms.dojo.data.DWRReadStore");

dojo.declare("dotcms.dojo.data.UsersReadStore", dotcms.dojo.data.DWRReadStore, {
	
	includeRoles: false,
	assetInode: null,
	permission: 1,
	includeBlankResult: false,
	hideSystemRoles: false,
	includeUsers:true,

	constructor: function (options) {
	
		this._fetchFunction = this.fetchUsersRoles;
	 	this._loadItemFunction = this.loadUserRole;
	 	this._getLabelFunction = function (item) { 
	 		if(item.type == '_blank') return ""; 
	 		return item["name"] + " (" + item["type"] + ")";
	 	};
	 	this._getLabelAttributesFunction = function (item) { return ["name", "type"] };
	 	this._getIdentityFunction = function (item) { 
	 		if(item.type == '_blank') return ""; 
	 		return item["type"] + "-" + item["id"] 
	 	};
	 	this._getIdentityAttributesFunction = function (item) { return ["type", "id"] };
	 	this._fetchByIdentityFunction = this.loadUserRole;
	 	this._functionsScope = this;
	 	if(options.includeBlankResult)
	 		this.includeBlankResult = options.includeBlankResult;
	 	
		if(options.includeRoles)
			this.includeRoles = options.includeRoles == 'true' || options.includeRoles == true?true:false;
		if(options.hideSystemRoles)
			this.hideSystemRoles = options.hideSystemRoles == 'true' || options.hideSystemRoles == true?true:false;
		if(!options.includeUsers)
			this.includeUsers = options.includeUsers == 'true' || options.includeUsers == true?true:false;
		if(options.assetInode)
			this.assetInode = options.assetInode;
		if(options.asset)
			this.assetInode = options.asset;
		if(options.permission)
			this.permission = options.permission;	
	},

	loadUserRole: function (args, options) {
		if(args.identity != null) {	
			if(args.identity == '' && this.includeBlankResult) {
				var blankObj = { name:"", id:"", emailaddress:"", type:"blank" };
				options.callback(blankObj);
				return;
			}
			if(args.identity.indexOf('user-') == 0)
				UserAjax.getUserById(args.identity.substring(5, args.identity.length), options);
			else if(args.identity.indexOf('role-') == 0)
				this.loadRole(args.identity.substring(5, args.identity.length), options);
		} else {
			if(args.identity == '' && this.includeBlankResult) {
				var blankObj = { name:"", id:"", emailaddress:"", type:"blank" };
				options.callback(blankObj);
				return;
			}
			if(args.item.type == 'user')
				UserAjax.getUserById(args.item.id, options);
			else if(args.item.type == 'role')
				this.loadRole(args.item.id, options);
		}
	},

	/**
	 * Calls GET /api/v1/roles with the given path and query. Resolves to the response entity.
	 */
	_getRoles: function (path, query) {
		var url = '/api/v1/roles' + path + '?' + Object.keys(query).map(function (key) {
			return encodeURIComponent(key) + '=' + encodeURIComponent(query[key]);
		}).join('&');
		return fetch(url, { credentials: 'same-origin', headers: { 'Accept': 'application/json' } })
			.then(function (response) {
				if(!response.ok)
					throw new Error('Roles could not be loaded: HTTP ' + response.status);
				return response.json();
			})
			.then(function (data) { return data.entity; });
	},

	_toRoleItem: function (role) {
		return { id: role.id, name: role.name, emailaddress: '', type: 'role' };
	},

	/**
	 * Loads one role, passing the store's asset as permissionable so a user who may edit that
	 * asset's permissions can read it. Listing roles (fetchRoles) takes the Roles or Users portlet.
	 */
	loadRole: function (roleId, options) {
		var query = { loadChildrenRoles: false };
		if(this.assetInode)
			query.permissionable = this.assetInode;
		this._getRoles('/' + encodeURIComponent(roleId), query)
			.then(dojo.hitch(this, function (role) {
				options.callback.call(options.scope, this._toRoleItem(role));
			}))
			.catch(function (error) {
				options.errorHandler.call(options.scope, error.message, error);
			});
	},

	/**
	 * Lists roles whose name starts with the query, one page at a time. The total is the
	 * listed count plus one while a page comes back full, so the select keeps offering more.
	 */
	fetchRoles: function (params) {
		var count = params.limit > 0 ? params.limit : 1000;
		return this._getRoles('/_search', { searchName: params.query, start: params.start, count: count })
			.then(dojo.hitch(this, function (roles) {
				var data = (roles || []).map(this._toRoleItem);
				return { data: data, total: params.start + data.length + (data.length >= count ? 1 : 0) };
			}));
	},

	fetchUsersRoles: function (args, options) {
		var query = args.query.name.substring(args.query.name.length - 1, args.query.name.length) == '*'?
        		args.query.name.substring(0, args.query.name.length - 1):args.query.name;
		var assetInode = null;
		var permission = 1;
		var start = args.start?args.start:0;
		var limit = args.count?(args.count == 'Infinity'?-1:args.count):-1;
		var params = { start: start, limit: limit, query: query, hideSystemRoles: this.hideSystemRoles };
		var cbFn = dojo.partial(this.fetchUsersRolesCallback, options);
		var modOptions = { scope: this, errorHandler: options.errorHandler, callback: cbFn };
		
		if(!this.includeRoles) {

			UserAjax.getUsersList(this.assetInode, this.permission, params, modOptions);
		}else if(!this.includeUsers){

			this.fetchRoles(params)
				.then(function (results) { cbFn.call(modOptions.scope, results); })
				.catch(function (error) { options.errorHandler.call(options.scope, error.message, error); });
		}else {

			// Roles from REST, users from the Users portlet's own listing, in one page
			var roles = this.fetchRoles(params);
			var users = new Promise(dojo.hitch(this, function (resolve, reject) {
				UserAjax.getUsersList(this.assetInode, this.permission, params, {
					callback: resolve,
					errorHandler: function (message, error) { reject(error || new Error(message)); }
				});
			}));
			Promise.all([roles, users])
				.then(function (results) {
					cbFn.call(modOptions.scope, {
						data: results[1].data.concat(results[0].data),
						total: (results[1].total || 0) + results[0].total
					});
				})
				.catch(function (error) { options.errorHandler.call(options.scope, error.message, error); });
		}
	},
	
	fetchUsersRolesCallback: function (options, results) {
		if(this.includeBlankResult) {
			var tempArr = new Array();
			tempArr.push({ name:" ", id:"", emailaddress:"", type:"_blank" });
			results.data = tempArr.concat(results.data);
		}

		options.callback.call(options.scope, results);
	},
	
	fetchItemByIdentity: function (request) {
		if(!this.includeRoles) {
			UserAjax.getUserById(this.assetInode, this.permission, params, request);
		} 
	},
	
	fetchItemByIdentityCallback: function (request, user) {
		var scope = request.scope;
		if(request.onItem) {
			request.onItem.call(scope?scope:dojo.global, host, this.currentRequest);
		}
	}
	
});

