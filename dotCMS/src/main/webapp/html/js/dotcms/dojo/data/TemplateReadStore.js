dojo.provide("dotcms.dojo.data.TemplateReadStore");

dojo.require("dojo.data.api.Read");
dojo.require("dojo.data.api.Request");

dojo.declare("dotcms.dojo.data.TemplateReadStore", null, {

	// Constants
	MAX_ITEMS_SAFETY_LIMIT: 100,
	DEFAULT_PER_PAGE: 15,
	MIN_COUNT_VALUE: 1,
	API_BASE_URL: "/api/v1/templates/",
	DEBUG_MODE: false,

	// Properties
	hostId: '',
	currentRequest: null,
	includeArchived: false,
	includeTemplate: null,
	templateSelected: '',
	allSiteLabel: false,

	/**
	 * To Emulate this old Backend Behavior
	 * https://github.com/dotCMS/core/blob/7bc05d335b98ffb30d909de9ec82dd4557b37078/dotCMS/src/main/java/com/dotmarketing/portlets/templates/ajax/TemplateAjax.java#L72-L76
	 */
	ALL_SITE_TEMPLATE: {
		title: "All Sites",
		fullTitle: "All Sites",
		htmlTitle: '<div>-- All Sites ---</div>',
		identifier: "0",
		inode: "0",
		hostName: "",
		type: "template",
		modDate: new Date().getTime()
	},

	constructor: function (options) {
		if (!options) {
			throw new Error('TemplateReadStore: options parameter is required');
		}

		this.hostId = options.hostId || '';
		this.allSiteLabel = !!options.allSiteLabel;
		this.templateSelected = options.templateSelected || '';
		this.DEBUG_MODE = !!options.debug;
	},

	// Utility Methods
	_log: function(message, ...args) {
		if (this.DEBUG_MODE) {
			console.log(`[TemplateReadStore] ${message}`, ...args);
		}
	},

	_error: function(message, error) {
		console.error(`[TemplateReadStore] ${message}`, error);
	},

	_warn: function(message, ...args) {
		console.warn(`[TemplateReadStore] ${message}`, ...args);
	},

	_validateResponse: function(responseEntity) {
		if (!responseEntity || !Array.isArray(responseEntity.entity)) {
			throw new Error('Invalid API response structure');
		}
		return true;
	},

	_buildApiUrl: function(query, pagination) {
		const { page, perPage } = pagination;
		const filter = query.fullTitle ? query.fullTitle.replace('*', '') : '';

		const url = new URL(this.API_BASE_URL, window.location.origin);

		url.searchParams.set('filter', filter);
		url.searchParams.set('page', page);
		url.searchParams.set('per_page', perPage);

		if (query.sort) {
			url.searchParams.set('orderby', query.sort);
		}

		if (query.hostId) {
			url.searchParams.set('host', query.hostId);
		}

		return url.toString();
	},

	_createLightTemplate: function(template) {
		if (!template || typeof template !== 'object') {
			this._warn('Invalid template object:', template);
			return null;
		}

		// Create a lightweight version removing heavy properties like 'containers'
		return {
			identifier: template.identifier,
			inode: template.inode,
			title: template.title,
			fullTitle: template.fullTitle,
			htmlTitle: template.htmlTitle,
			hostName: template.hostName,
			hostId: template.hostId,
			friendlyName: template.friendlyName,
			modDate: template.modDate,
			type: 'template',
			live: template.live,
			working: template.working,
			canRead: template.canRead,
			canWrite: template.canWrite,
			canPublish: template.canPublish
		};
	},

	_processTemplatesArray: function(templatesArray, keywordArgs, scope) {
		// Safety check to prevent infinite loops
		if (templatesArray.length > this.MAX_ITEMS_SAFETY_LIMIT) {
			this._error(`Too many templates in array (${templatesArray.length}), possible infinite loop. Truncating to ${this.MAX_ITEMS_SAFETY_LIMIT}.`);
			templatesArray = templatesArray.slice(0, this.MAX_ITEMS_SAFETY_LIMIT);
		}

		dojo.forEach(templatesArray, function (template, index) {
			try {
				const lightTemplate = this._createLightTemplate(template);
				if (lightTemplate) {
					keywordArgs.onItem.call(scope || dojo.global, lightTemplate, this.currentRequest);
				}
			} catch (error) {
				this._error(`Error processing template at index ${index}:`, error);
				throw error; // Re-throw to stop the iteration
			}
		}, this);
	},

	// API Methods
	getValue: function (item, attribute, defaultValue) {
		return item && item[attribute] !== undefined ? item[attribute] : defaultValue;
	},

	getValues: function (item, attribute) {
		const value = this.getValue(item, attribute);
		return dojo.isArray(value) ? value : [value];
	},

	getAttributes: function (item) {
		if (!item) return [];
		return Object.keys(item);
	},

	hasAttribute: function (item, attribute) {
		return item && item[attribute] !== undefined;
	},

	containsValue: function (item, attribute, value) {
		const values = this.getValues(item, attribute);
		return dojo.indexOf(values, value) >= 0;
	},

	isItem: function (item) {
		return item && (item.type === 'template' || item.identifier);
	},

	isItemLoaded: function (item) {
		return this.isItem(item);
	},

	loadItem: function (keywordArgs) {
		if (!keywordArgs) {
			throw new Error('keywordArgs parameter is required');
		}

		if (!this.isItem(keywordArgs.item)) {
			const error = { message: 'passed item is not a valid template item' };
			if (keywordArgs.onError) {
				keywordArgs.onError.call(keywordArgs.scope || dojo.global, error);
			}
			return;
		}

		if (keywordArgs.onItem) {
			keywordArgs.onItem.call(keywordArgs.scope || dojo.global, keywordArgs.item);
		}
	},

	fetch: function (keywordArgs) {
		try {
			this._log('Starting fetch operation');

			if (!keywordArgs) {
				throw new Error('keywordArgs parameter is required');
			}

			// Normalize query
			if (dojo.isString(keywordArgs.query)) {
				keywordArgs.query = { fullTitle: keywordArgs.query };
			}
			keywordArgs.query = keywordArgs.query || {};
			keywordArgs.queryOptions = keywordArgs.queryOptions || {};

			// Set host ID
			if (this.hostId !== '') {
				keywordArgs.query.hostId = this.hostId;
			}

			// Set additional options
			if (this.includeArchived) {
				keywordArgs.queryOptions.includeArchived = this.includeArchived;
			}

			if (this.includeTemplate) {
				keywordArgs.queryOptions.includeTemplate = this.includeTemplate;
			}

			if (this.templateSelected) {
				keywordArgs.query.templateSelected = this.templateSelected;
			}

			// Handle pagination parameters
			const start = parseInt(keywordArgs.start || 0);
			const count = Math.max(parseInt(keywordArgs.count || this.DEFAULT_PER_PAGE), this.MIN_COUNT_VALUE);

			// Calculate page number for server-side pagination
			const page = Math.floor(start / count) + 1;
			const perPage = count;

			const pagination = { page, perPage };
			const url = this._buildApiUrl(keywordArgs.query, pagination);

			this._log('Pagination params:', { start, count, page, perPage });
			this._log('Fetching URL:', url);

			// Make the API call
			fetch(url)
				.then((fetchResp) => {
					if (!fetchResp.ok) {
						throw new Error(`HTTP error! status: ${fetchResp.status}`);
					}
					return fetchResp.json();
				})
				.then(responseEntity => {
					this._handleSuccessResponse(responseEntity, keywordArgs, start, perPage);
				})
				.catch(error => {
					this._handleErrorResponse(error, keywordArgs);
				});

			// Setup and return current request
			this.currentRequest = keywordArgs;
			this.currentRequest.abort = function () {
				// Could implement actual abort logic here if needed
			};

			return this.currentRequest;

		} catch (error) {
			this._error('Error in fetch method:', error);
			if (keywordArgs.onError) {
				keywordArgs.onError.call(keywordArgs.scope || dojo.global, error);
			}
			return null;
		}
	},

	_handleSuccessResponse: function(responseEntity, keywordArgs, start, perPage) {
		try {
			this._log('Raw API response pagination:', responseEntity.pagination);
			this._log('Raw API response entity count:', responseEntity.entity ? responseEntity.entity.length : 0);

			// Validate response structure
			this._validateResponse(responseEntity);

			// Handle the case where API returns more items than requested
			if (responseEntity.entity.length > perPage) {
				this._warn(`API returned ${responseEntity.entity.length} items but requested ${perPage}. Truncating.`);
				responseEntity.entity = responseEntity.entity.slice(0, perPage);
			}

			// Only add ALL_SITE_TEMPLATE on the first page (start === 0)
			if (this.allSiteLabel && start === 0) {
				const hasAllSiteTemplate = responseEntity.entity.some(item => item.identifier === "0");
				if (!hasAllSiteTemplate) {
					responseEntity.entity.unshift(this.ALL_SITE_TEMPLATE);
					// Adjust totalEntries to account for the added item
					if (responseEntity.pagination) {
						responseEntity.pagination.totalEntries = responseEntity.pagination.totalEntries + 1;
					}
				}
			}

			this._log('Processed response with', responseEntity.entity.length, 'templates');
			this._processSuccessCallback(keywordArgs, responseEntity);

		} catch (error) {
			this._error('Error processing API response:', error);
			if (keywordArgs.onError) {
				keywordArgs.onError.call(keywordArgs.scope || dojo.global, error);
			}
		}
	},

	_handleErrorResponse: function(error, keywordArgs) {
		this._error('Error fetching templates:', error);
		if (keywordArgs.onError) {
			keywordArgs.onError.call(keywordArgs.scope || dojo.global, error);
		}
	},

	_processSuccessCallback: function (keywordArgs, templatesEntity) {
		try {
			this._log('Processing success callback');

			const scope = keywordArgs.scope;

			// Ensure we have valid data structure
			if (!templatesEntity || !templatesEntity.entity) {
				throw new Error('Invalid templatesEntity structure');
			}

			if (keywordArgs.onBegin) {
				const totalEntries = templatesEntity.pagination ? templatesEntity.pagination.totalEntries : templatesEntity.entity.length;
				this._log('Calling onBegin with totalEntries:', totalEntries);
				keywordArgs.onBegin.call(scope || dojo.global, totalEntries, this.currentRequest);
			}

			if (keywordArgs.onItem && templatesEntity.entity.length > 0) {
				this._log('Processing templates array:', templatesEntity.entity.length, 'items');
				this._processTemplatesArray(templatesEntity.entity, keywordArgs, scope);
			}

			if (keywordArgs.onComplete) {
				this._log('Calling onComplete with', templatesEntity.entity.length, 'templates');
				keywordArgs.onComplete.call(scope || dojo.global, templatesEntity.entity, this.currentRequest);
			}

		} catch (error) {
			this._error('Error in success callback:', error);
			if (keywordArgs.onError) {
				keywordArgs.onError.call(keywordArgs.scope || dojo.global, error);
			}
		}
	},

	fetchItemByIdentity: function (request) {
		if (!request || !request.identity) {
			this._error('Invalid request or missing identity');
			return;
		}

		const templateId = request.identity;
		const url = `${this.API_BASE_URL}${templateId}/working`;

		this._log('Fetching template by identity:', templateId);

		fetch(url)
			.then(async (response) => {
				if (response.ok) {
					const result = await response.json();
					this._processIdentityCallback(request, result.entity);
				} else {
					throw new Error(`HTTP error! status: ${response.status}`);
				}
			})
			.catch((error) => {
				this._error('Error fetching template by identity:', error);
				if (request.onError) {
					request.onError.call(request.scope || dojo.global, error);
				}
			});
	},

	_processIdentityCallback: function (request, template) {
		try {
			const scope = request.scope;
			if (request.onItem) {
				request.onItem.call(scope || dojo.global, template, this.currentRequest);
			}
		} catch (error) {
			this._error('Error in identity callback:', error);
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
		if (!item) return '';
		const hostName = item.hostName || '';
		const fullTitle = item.fullTitle || '';
		return `${hostName} ${fullTitle}`.trim();
	},

	getLabelAttributes: function (item) {
		return ['hostName', 'fullTitle'];
	},

	getIdentity: function (item) {
		return item ? item.identifier : null;
	}
});

