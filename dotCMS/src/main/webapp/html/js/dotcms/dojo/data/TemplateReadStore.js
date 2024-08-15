dojo.provide("dotcms.dojo.data.TemplateReadStore");

dojo.require("dojo.data.api.Read");
dojo.require("dojo.data.api.Request");

dojo.declare("dotcms.dojo.data.TemplateReadStore", null, {

	hostId: '',
	currentRequest: null,
	includeArchived: false,
	includeTemplate: null,
	templateSelected: '',

	constructor: function (options) {
		this.hostId = options.hostId;
		this.templateSelected = options.templateSelected;
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
		var fetchCallbackVar = dojo.hitch(this, this.fetchTemplatesCallback, keywordArgs);

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

		if (this.templateSelected) {
			keywordArgs.query.templateSelected = this.templateSelected;
		}


		if((keywordArgs.query.fullTitle == '' ||
				keywordArgs.query.fullTitle=='undefined' ||
				keywordArgs.query.fullTitle.indexOf('*')===-1)
			&& (keywordArgs.count == 'undefined' || keywordArgs.count ==null )
			&& (keywordArgs.start == 'undefined' || keywordArgs.start ==null) ){
			this.currentRequest = keywordArgs;
			this.currentRequest.abort = function () { };
			return this.currentRequest;

		} else {

			let url = "/api/v1/templates/?filter=" + keywordArgs.query.fullTitle.replace('*','') + "&page=" + keywordArgs.start + "&per_page=" + keywordArgs.count +
				(keywordArgs.sort == undefined || keywordArgs.sort != ""? "": "&orderby=" + keywordArgs.sort);

			if (keywordArgs.query.hostId != undefined && keywordArgs.query.hostId != "") {
				url += "&host=" + keywordArgs.query.hostId;
			}

			fetch(url)
				.then((fetchResp) => fetchResp.json())
				.then(responseEntity => {

						this.fetchTemplatesCallback(keywordArgs, responseEntity);
					}
				);

			this.currentRequest = keywordArgs;
			this.currentRequest.abort = function () { };
			return this.currentRequest;
		}
	},

	fetchTemplatesCallback: function (keywordArgs, templatesEntity) {

		var scope = keywordArgs.scope;
		if(keywordArgs.onBegin) {

			keywordArgs.onBegin.call(scope?scope:dojo.global, templatesEntity.pagination.totalEntries, this.currentRequest);
		}

		if(keywordArgs.onItem) {

			let templatesArray = templatesEntity.entity;

			dojo.forEach(templatesArray, function (template) {
				keywordArgs.onItem.call(scope?scope:dojo.global, template, this.currentRequest);
			}, this);
		}

		if(keywordArgs.onComplete) {

			keywordArgs.onComplete.call(scope?scope:dojo.global, templatesEntity.entity, this.currentRequest);
		}
	},

	fetchCallback: function (keywordArgs, templatesEntity) {

		var scope = keywordArgs.scope;
		if(keywordArgs.onBegin) {
			keywordArgs.onBegin.call(scope?scope:dojo.global, templatesEntity.pagination.totalEntries, this.currentRequest);
		}

		if(keywordArgs.onItem) {
			dojo.forEach(templatesEntity.entity, function (template) {
				keywordArgs.onItem.call(scope?scope:dojo.global, template, this.currentRequest);
			}, this);
		}

		if(keywordArgs.onComplete) {
			keywordArgs.onComplete.call(scope?scope:dojo.global, templatesEntity.entity, this.currentRequest);
		}
	},

	fetchItemByIdentity: function (request) {
		const templateId = request.identity;
		fetch("/api/v1/templates/" + templateId + "/working")
			.then(async (response) => {
				// The ok value represents the result of the response status 200 codes
				if (response.ok) {
					const result = await response.json();

					this.fetchItemByIdentityCallback(request, result.entity); // here we pass the result of the json response to the callback function
				}
			}).catch((e) => {
				console.log(e) // Here we can catch the error
			});
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

