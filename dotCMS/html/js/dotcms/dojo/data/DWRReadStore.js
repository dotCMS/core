dojo.provide("dotcms.dojo.data.DWRReadStore");

dojo.require("dojo.data.api.Read");
dojo.require("dojo.data.api.Identity");

dojo.declare("dotcms.dojo.data.DWRReadStore", null, {
	
	 _loadedItems: null,
	 _cachedFetchs: null,

	 _fetchFunction: null,
	 _loadItemFunction: null,
	 _getLabelFunction: null,
	 _getLabelAttributesFunction: null,
	 _getIdentityFunction: null,
	 _getIdentityAttributesFunction: null,
	 _fetchByIdentityFunction: null,
	 _functionsScope: null,
		 
	constructor: function (options) {
	
		if(options) {
			this._fetchFunction = options.fetchFunction;
			this._loadItemFunction = options.loadItemFunction;
			this._fetchByIdentityFunction = options.fetchByIdentityFunction;
		
			if(options.getLabelFunction) {
				this._getLabelFunction = options.getLabelFunction;
				this._getLabelAttributes = options.getLabelAttributes;
			}
			
			if(options.getIdentityFunction) {
				this._getIdentityFunction = options.getIdentityFunction;
				this._getIdentityAttributes = options.getIdentityAttributes;
			}
			
			if(options.scope)
				this._functionsScope = options.scope;
			else
				this._functionsScope = dojo.global;
		}
		
		this._loadedItems = new Array();
		this._cachedFetchs = new Array();
	},
	
	getValue: function(	/* item */ item, 
		/* attribute-name-string */ attribute, 
		/* value? */ defaultValue){
		return item[attribute]?item[attribute]:defaultValue;
	},
	
	getValues: function(/* item */ item,
			/* attribute-name-string */ attribute){
		return dojo.isArray(item[attribute])?item[attribute]:[item[attribute]];
	},
	
	getAttributes: function(/* item */ item){
		var attributes = new Array();
		for(key in item) {
			attributes.push(key);
		}
		return attributes;
	},
	
	hasAttribute: function(	/* item */ item,
			/* attribute-name-string */ attribute){
		return item[attribute] != null;
	},
	
	containsValue: function(/* item */ item,
			/* attribute-name-string */ attribute, 
			/* anything */ value){
		return dojo.indexOf(this.getValues(item, attribute), value) != -1;
	},
	
	isItem: function(/* anything */ something){
		return something["_loadedItem"] != null;
	},
	
	isItemLoaded: function(/* anything */ something) {
		if(this.isItem(something) && this.getIdentity(something)) {
			return this._loadedItems[this.getIdentity(something)] != null;
		}
		return false;
	},
	
	loadItem: function(/* object */ keywordArgs){
		if (!this.isItemLoaded(keywordArgs.item)) {
			this._loadItemFunction.call(this._functionsScope, keywordArgs, 
					{ callback: dojo.partial(this._loadItemCallback, keywordArgs), 
						errorHandler: dojo.partial(this._loadItemError, keywordArgs),
						scope: this });
		}
	},
	
	_loadItemCallback: function (keywordArgs, item) {
		var id = this.getIdentity(item);
		item["_loadedItem"] = true;
		this._loadedItems[id] = item;
		if(keywordArgs.onItem)
			keywordArgs.onItem.call(keywordArgs.scope != null?keywordArgs.scope:dojo.global, item);
		
	},
	
	_loadItemError: function (keywordArgs, errorString, exception) {
		if(keywordArgs.onError)
			keywordArgs.onError.call(keywordArgs.scope != null?keywordArgs.scope:dojo.global, errorString);
	},
	
	_sameQuery: function (query1, query2) {
		for(key in query1) {
			if(query1[key] != query2[key])
				return false;
		}
		for(key in query2) {
			if(query1[key] != query2[key])
				return false;
		}
		return true;
	},
	
	_sameFetchArgs: function(args1, args2) {
		if(this._sameQuery(args1.query, args2.query) && 
				args1.start == args2.start && 
				args1.count == args2.count && 
				args1.sort == args2.sort && 
				((!args1.queryOptions && !args2.queryOptions) || 
				 (this._sameQuery(args1.queryOptions, args2.queryOptions)))
		  )
			return true;
		return false;
	},
	
	_loadFromCachedFetchs: function(fectchArgs) {
		for(var i = 0; i < this._cachedFetchs.length; i++) {
			var fetch = this._cachedFetchs[i];
			if(this._sameFetchArgs(fetch.fetchArgs, fectchArgs))
				return fetch;
		}
		return null;
	},
	
	fetch: function(/* Object */ fetchArgs){
		
		var params = {};

		var request = new dojo.data.api.Request();
		fetchArgs.abort = function () {
		};
		dojo.mixin(request, fetchArgs);
		
		var fetchCallback =  dojo.partial(this._fetchCallback, fetchArgs, request);
		var errorCallback = dojo.partial(this._fetchError, fetchArgs, request);

		var cachedFetch = this._loadFromCachedFetchs(fetchArgs);
		if(cachedFetch != null) {
			this._fetchCompleteCallbacks(fetchArgs, request, cachedFetch.results);
		} else {
			this._fetchFunction.call(this._functionsScope, fetchArgs, { callback: fetchCallback, 
				errorHandler: errorCallback, scope: this });
		}
		
		return request;
	},
	
	_fetchCallback: function (fetchArgs, request, results) {

		var data = results.data;
		
		var listObjs = new Array();
		dojo.forEach(data, function(item, idx) {
			var obj = item;
			obj["_loadedItem"] = true;
			var id = this.getIdentity(obj);
			if(this._loadedItems[id] == null)
				this._loadedItems[id] = obj;
			else
				obj = this._loadedItems[id];
			listObjs.push(obj);
		}, this);
		
		results.data = listObjs;
			
		this._fetchCompleteCallbacks(fetchArgs, request, results);
		
		this._cachedFetchs.push({ fetchArgs: fetchArgs, results: results });
	},
	
	_fetchCompleteCallbacks: function(fetchArgs, request, results) {

		var listObjs = results.data;
		var total = results.total == null && parseInt(results.total) != NaN?-1:results.total;

		if(fetchArgs.onBegin) {
			fetchArgs.onBegin.call(fetchArgs.scope != null?fetchArgs.scope:dojo.global, total, request);
		}
		if(fetchArgs.onItem) {
			listObjs.forEach(function(obj, idx) {
				fetchArgs.onItem.call(fetchArgs.scope != null?fetchArgs.scope:dojo.global, obj, request);
			});
		} 
		if(fetchArgs.onComplete) {
			if(fetchArgs.onItem)
				fetchArgs.onComplete.call(fetchArgs.scope != null?fetchArgs.scope:dojo.global, null, request);
			else
				fetchArgs.onComplete.call(fetchArgs.scope != null?fetchArgs.scope:dojo.global, listObjs, request);
		}
		
	},
	
	_fetchError: function (fetchArgs, request, errorString, exception) {
		if(fetchArgs.onError)
			fetchArgs.onError.call(fetchArgs.scope != null?fetchArgs.scope:dojo.global, errorString, request);
	},

	
	close: function(/*dojo.data.api.Request || keywordArgs || null */ request){
	},
	
	getLabel: function(/* item */ item){
		if(this._getLabelFunction)
			return this._getLabelFunction.call(this._functionsScope, item);
		return item.label;
	},
	
	getLabelAttributes: function(/* item */ item){
		if(this._getLabelAttributesFunction)
			return this._getLabelAttributesFunction.call(this._functionsScope, item);
		return [ "label" ];
	},
	
	getIdentity: function(/* item */ item){
		if(this._getIdentityFunction)
			return this._getIdentityFunction.call(this._functionsScope, item);
		return item.id;
	},
	
	getIdentityAttributes: function(/* item */ item){
		if(this._getIdentityAttributesFunction)
			return this._getIdentityAttributesFunction.call(this._functionsScope, item);
		return ["id"];
	},
	
	fetchItemByIdentity: function(/* object */ keywordArgs){
		
		if(this._loadedItems[keywordArgs.identity] != null) {
			this._fetchItemByIdentityCallback(keywordArgs, this._loadedItems[keywordArgs.identity]);
		} else {
			this._fetchByIdentityFunction.call(this._functionsScope, keywordArgs, 
					{ callback: dojo.partial(this._fetchItemByIdentityCallback, keywordArgs), 
						errorHandler: dojo.partial(this._fetchItemByIdentityError, keywordArgs),
						scope: this });
		}

	},

	_fetchItemByIdentityCallback: function(/* object */ keywordArgs, item){
		item["_loadedItem"] = true;
		this._loadedItems[keywordArgs.identity] = item;
		if(keywordArgs.onItem) {
			keywordArgs.onItem.call(keywordArgs.scope?keywordArgs.scope:dojo.global, item);
		}
	},

	_fetchItemByIdentityError: function(/* object */ keywordArgs, error, exception){
		if(keywordArgs.onError) {
			keywordArgs.onError.call(keywordArgs.scope?keywordArgs.scope:dojo.global, error);
		}
	},

	getFeatures: function(){
		return {
			'dojo.data.api.Read': true,
			'dojo.data.api.Identity': true
		};
	}
	
});

