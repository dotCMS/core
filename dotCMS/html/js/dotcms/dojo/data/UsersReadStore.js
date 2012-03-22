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
				UserAjax.getRoleById(args.identity.substring(5, args.identity.length), options);
		} else {
			if(args.identity == '' && this.includeBlankResult) {
				var blankObj = { name:"", id:"", emailaddress:"", type:"blank" };
				options.callback(blankObj);
				return;
			}
			if(args.item.type == 'user')
				UserAjax.getUserById(args.item.id, options);
			else if(args.item.type == 'role')
				UserAjax.getRoleById(args.item.id, options);
		}
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

			UserAjax.getRolesList(this.assetInode, this.permission, params, modOptions);
		}else {

			UserAjax.getUsersAndRolesList(this.assetInode, this.permission, params, modOptions);
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

