dojo.provide("dotcms.dojo.data.ContainerReadStoreDrawedTemplate");

dojo.require("dojo.data.api.Read");
dojo.require("dojo.data.api.Request");

dojo.declare("dotcms.dojo.data.ContainerReadStoreDrawedTemplate", null, {
	
	hostId: '', 
	currentRequest: null,

	constructor: function (options) {
	
		if(options.hostId)
			this.hostId = options.hostId;
		
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
		return item[type] == 'containers';
	},
	
	isItemLoaded: function (item) {
		return this.isItem(item)?true:false;
	},
	
	loadItem: function (keywordArgs) {
		
		if(!this.isItem(keywordArgs.item))
			keywordArgs.onError.call(scope?scope:dojo.global(), { message: 'passed item is not a valid container item' });
		
		var scope = keywordArgs.scope;
		keywordArgs.onItem.call(scope?scope:dojo.global(), keywordArgs.item);
		
	},
	
	fetch: function (keywordArgs) {
		
		var fetchCallback = dojo.hitch(this, this.fetchCallback, keywordArgs);
		
		if(dojo.isString(keywordArgs.query)) {
			keywordArgs.query = { title: keywordArgs.query };
		}

		if(this.hostId != '') {
			keywordArgs.query.hostId = this.hostId;
		}

		if(!keywordArgs.start){
			keywordArgs.start = 0;
		}
		
		if(!keywordArgs.count){
			keywordArgs.count = 10;
		}

		if(!keywordArgs.sort){
			keywordArgs.sort = [""];
		}

        if (containersAdded) {
            keywordArgs.exclude = containersAdded;
        } else {
            keywordArgs.exclude = [];
        }
		
		ContainerAjaxDrawedTemplate.fetchContainersDesignTemplate(keywordArgs.query, keywordArgs.queryOptions, keywordArgs.start, keywordArgs.count, keywordArgs.sort, keywordArgs.exclude, fetchCallback);
		
		this.currentRequest = keywordArgs;
		this.currentRequest.abort = function () { };
		return this.currentRequest;
	},
	
	fetchCallback: function (keywordArgs, containers) {
		
		var scope = keywordArgs.scope;
		if(keywordArgs.onBegin) {
			keywordArgs.onBegin.call(scope?scope:dojo.global, containers.totalResults, this.currentRequest);
		}
		
		if(keywordArgs.onItem) {
			dojo.forEach(containers.list, function (container) {
				keywordArgs.onItem.call(scope?scope:dojo.global, container, this.currentRequest);
			}, this);
		}

		if(keywordArgs.onComplete) {
			keywordArgs.onComplete.call(scope?scope:dojo.global, containers.list, this.currentRequest);
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
		return item['hostName'] + ' ' + item['title'];
	},
	
	getLabelAttributes: function (item) {
		return [ 'hostName', 'title' ];
	},
	
	getIdentity: function (item) {
		return item['identifier'];
	}
	
});

