dojo.provide("dotcms.dojo.data.StructureReadStore");

dojo.require("dojo.data.api.Read");
dojo.require("dojo.data.api.Request");

dojo.declare("dotcms.dojo.data.StructureReadStore", null, {
	
	currentRequest: null,
	includeArchived: false,
	structureType: null,

	constructor: function (options) {
		this.structureType = options.structureType;
		window.top._dotStructureStore = this;
	},

	getValue: function (item, attribute, defaultValue) {
		return item[attribute]?item[attribute]:defaultValue;
	},
	
	getValues: function (item, attribute) {
		return dojo.isArray(item[attribute])?item[attribute]:[item[attribute]]; 
	},
	
	getAttributes: function (item) {
		var attributes = new Array();
		for(att in item) {
			attributes.push(att);
		}
		return attributes;			
	},
	
	hasAttribute: function (item, attribute) {
		return item[attribute] != null;
	},
	
	containsValue: function (item, attribute, value) {
		var values = this.getValues(item, attribute);
		return dojo.indexOf(values, value) >= 0;
	},
	
	isItem: function (item) {
		return item[type] == 'structure';
	},
	
	isItemLoaded: function (item) {
		return this.isItem(item)?true:false;
	},
	
	loadItem: function (keywordArgs) {
		
		if(!this.isItem(keywordArgs.item))
			keywordArgs.onError.call(scope?scope:dojo.global(), { message: 'passed item is not a valid structure item' });
		
		var scope = keywordArgs.scope;
		keywordArgs.onItem.call(scope?scope:dojo.global(), keywordArgs.item);
		
	},
	
	fetch: function (keywordArgs) {
		
		var fetchCallback = dojo.hitch(this, this.fetchCallback, keywordArgs);
		
		if(dojo.isString(keywordArgs.query)) {
			keywordArgs.query = { name: keywordArgs.query };
		}

		if(this.includeArchived) {
			keywordArgs.queryOptions.includeArchived = this.includeArchived;
		}
		
		if(this.structureType) {
			keywordArgs.queryOptions.structureType = this.structureType;
		}
		
		if((keywordArgs.query.name == '' || 
				keywordArgs.query.name=='undefined' || 
				keywordArgs.query.name.indexOf('*')===-1) 
				&& (keywordArgs.count == 'undefined' || keywordArgs.count ==null ) 
				&& (keywordArgs.start == 'undefined' || keywordArgs.start ==null) ){
			this.currentRequest.abort = function () { };
			return this.currentRequest;
			
		}else{
		    StructureAjax.fetchStructures(keywordArgs.query, keywordArgs.queryOptions, keywordArgs.start, keywordArgs.count, keywordArgs.sort, fetchCallback);
		    this.currentRequest = keywordArgs;
		    this.currentRequest.abort = function () { };
		    return this.currentRequest;
		}
	},
	
	fetchCallback: function (keywordArgs, structures) {
		
		var scope = keywordArgs.scope;
		if(keywordArgs.onBegin) {
			keywordArgs.onBegin.call(scope?scope:dojo.global, structures.totalResults, this.currentRequest);
		}
		
		if(keywordArgs.onItem) {
			dojo.forEach(structures.list, function (structure) {
				keywordArgs.onItem.call(scope?scope:dojo.global, structure, this.currentRequest);
			}, this);
		}

		if(keywordArgs.onComplete) {
			keywordArgs.onComplete.call(scope?scope:dojo.global, structures.list, this.currentRequest);
		}
	},
	
	fetchItemByIdentity: function (request) {
		StructureAjax.fetchByIdentity(request.identity, dojo.hitch(this, this.fetchItemByIdentityCallback, request));
	},
	
	fetchItemByIdentityCallback: function (request, structure) {
		var scope = request.scope;
		if(request.onItem) {
			request.onItem.call(scope?scope:dojo.global, structure, this.currentRequest);
		}
	},
	
	getFeatures: function () {
		return { 'dojo.data.api.Read': true };
	},
	
	close: function (request) {
		this.currentRequest = null;
	},
	
	getLabel: function (item) {
		return item['name'];
	},
	
	getLabelAttributes: function (item) {
		return [ 'name' ];
	},
	
	getIdentity: function (item) {
		return item['inode'];
	}
	
});

