dojo.provide("dotcms.dojo.data.TemplateReadStore");

dojo.require("dojo.data.api.Read");
dojo.require("dojo.data.api.Request");

dojo.declare("dotcms.dojo.data.TemplateReadStore", null, {
	
	hostId: '', 
	currentRequest: null,
	includeArchived: false,
	includeTemplate: null,

	constructor: function (options) {
		this.hostId = options.hostId;
		window.top._dotTemplateStore = this;
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
		return item[type] == 'template';
	},
	
	isItemLoaded: function (item) {
		return this.isItem(item)?true:false;
	},
	
	loadItem: function (keywordArgs) {
		
		if(!this.isItem(keywordArgs.item))
			keywordArgs.onError.call(scope?scope:dojo.global(), { message: 'passed item is not a valid template item' });
		
		var scope = keywordArgs.scope;
		keywordArgs.onItem.call(scope?scope:dojo.global(), keywordArgs.item);
		
	},
	
	fetch: function (keywordArgs) {
		
		var fetchCallback = dojo.hitch(this, this.fetchCallback, keywordArgs);
		
		if(dojo.isString(keywordArgs.query)) {
			keywordArgs.query = { fullTitle: keywordArgs.query };
		}

		if(this.hostId != '') {
			keywordArgs.query.hostId = this.hostId;
		}

		if(this.includeArchived) {
			keywordArgs.queryOptions.includeArchived = this.includeArchived;
		}
		
		if(this.includeTemplate) {
			keywordArgs.queryOptions.includeTemplate = this.includeTemplate;
		}
		
		if((keywordArgs.query.fullTitle == '' || 
				keywordArgs.query.fullTitle=='undefined' || 
				keywordArgs.query.fullTitle.indexOf('*')===-1) 
				&& (keywordArgs.count == 'undefined' || keywordArgs.count ==null ) 
				&& (keywordArgs.start == 'undefined' || keywordArgs.start ==null) ){
			this.currentRequest.abort = function () { };
			return this.currentRequest;
			
		}else{
		    TemplateAjax.fetchTemplates(keywordArgs.query, keywordArgs.queryOptions, keywordArgs.start, keywordArgs.count, keywordArgs.sort, fetchCallback);
		    this.currentRequest = keywordArgs;
		    this.currentRequest.abort = function () { };
		    return this.currentRequest;
		}
	},
	
	fetchCallback: function (keywordArgs, templates) {
		
		var scope = keywordArgs.scope;
		if(keywordArgs.onBegin) {
			keywordArgs.onBegin.call(scope?scope:dojo.global, templates.totalResults, this.currentRequest);
		}
		
		if(keywordArgs.onItem) {
			dojo.forEach(templates.list, function (template) {
				keywordArgs.onItem.call(scope?scope:dojo.global, template, this.currentRequest);
			}, this);
		}

		if(keywordArgs.onComplete) {
			keywordArgs.onComplete.call(scope?scope:dojo.global, templates.list, this.currentRequest);
		}
	},
	
	fetchItemByIdentity: function (request) {
		TemplateAjax.fetchByIdentity(request.identity, dojo.hitch(this, this.fetchItemByIdentityCallback, request));
	},
	
	fetchItemByIdentityCallback: function (request, template) {
		var scope = request.scope;
		if(request.onItem) {
			request.onItem.call(scope?scope:dojo.global, template, this.currentRequest);
		}
	},
	
	getFeatures: function () {
		return { 'dojo.data.api.Read': true };
	},
	
	close: function (request) {
		this.currentRequest = null;
		this.filterHostId = null;
	},
	
	getLabel: function (item) {
		return item['hostName'] + ' ' + item['fullTitle'];
	},
	
	getLabelAttributes: function (item) {
		return [ 'hostName', 'fullTitle' ];
	},
	
	getIdentity: function (item) {
		return item['identifier'];
	}
	
});

