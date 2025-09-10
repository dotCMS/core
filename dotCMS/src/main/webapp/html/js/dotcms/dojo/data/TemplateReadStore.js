dojo.provide('dotcms.dojo.data.TemplateReadStore');

dojo.require('dojo.data.api.Read');
dojo.require('dojo.data.api.Request');

dojo.declare('dotcms.dojo.data.TemplateReadStore', null, {
    hostId: '',
    currentRequest: null,
    includeArchived: false,
    includeTemplate: null,
    templateSelected: '',
    allSiteLabel: false,
    /**
     * To Emulate this old Backend Behavion
     * https://github.com/dotCMS/core/blob/7bc05d335b98ffb30d909de9ec82dd4557b37078/dotCMS/src/main/java/com/dotmarketing/portlets/templates/ajax/TemplateAjax.java#L72-L76
     *
     * @type {*}
     * */
    ALL_SITE_TEMPLATE: {
        title: 'All Sites',
        fullTitle: 'All Sites',
        htmlTitle: '<div>-- All Sites ---</div>',
        identifier: '0',
        inode: '0'
    },

    constructor: function (options) {
        this.hostId = options.hostId;
        this.allSiteLabel = options.allSiteLabel;
        this.templateSelected = options.templateSelected;
    },

    getValue: function (item, attribute, defaultValue) {
        return item[attribute] ? item[attribute] : defaultValue;
    },

    getValues: function (item, attribute) {
        return dojo.isArray(item[attribute]) ? item[attribute] : [item[attribute]];
    },

    getAttributes: function (item) {
        var attributes = new Array();
        for (att in item) {
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
        return this.isItem(item) ? true : false;
    },

    loadItem: function (keywordArgs) {
        if (!this.isItem(keywordArgs.item))
            keywordArgs.onError.call(scope ? scope : dojo.global(), {
                message: 'passed item is not a valid template item'
            });

        var scope = keywordArgs.scope;
        keywordArgs.onItem.call(scope ? scope : dojo.global(), keywordArgs.item);
    },

    fetch: function (keywordArgs) {
        var fetchCallbackVar = dojo.hitch(this, this.fetchTemplatesCallback, keywordArgs);

        if (dojo.isString(keywordArgs.query)) {
            keywordArgs.query = { fullTitle: keywordArgs.query };
        }

        if (this.hostId != '') {
            keywordArgs.query.hostId = this.hostId;
        }

        if (this.includeArchived) {
            keywordArgs.queryOptions.includeArchived = this.includeArchived;
        }

        if (this.includeTemplate) {
            keywordArgs.queryOptions.includeTemplate = this.includeTemplate;
        }

        if (this.templateSelected) {
            keywordArgs.query.templateSelected = this.templateSelected;
        }

        if (
            (keywordArgs.query.fullTitle == '' ||
                keywordArgs.query.fullTitle == 'undefined' ||
                keywordArgs.query.fullTitle.indexOf('*') === -1) &&
            (keywordArgs.count == 'undefined' || keywordArgs.count == null) &&
            (keywordArgs.start == 'undefined' || keywordArgs.start == null)
        ) {
            this.currentRequest = keywordArgs;
            this.currentRequest.abort = function () {};
            return this.currentRequest;
        } else {
            const hostId = keywordArgs.query.hostId;

            // Calculate the current page based on the start index and the page size
            const page = Math.floor(keywordArgs.start / keywordArgs.count) + 1;

            // Safely build the query string
            const query = new URLSearchParams();

            query.set('per_page', keywordArgs.count);
            query.set('page', page);
            query.set('filter', keywordArgs.query.fullTitle.replace('*', ''));

            if (hostId) {
                query.set('host', hostId);
            }

            if (keywordArgs.sort) {
                query.set('orderby', keywordArgs.sort);
            }

            const url = `/api/v1/templates/?${query.toString()}`;

            fetch(url)
                .then((fetchResp) => fetchResp.json())
                .then((responseEntity) => {
                    // Check for first page and add the all site template, so it does not break the pagination
                    if (this.allSiteLabel && page === 1) {
                        responseEntity.entity.unshift(this.ALL_SITE_TEMPLATE);
                    }

                    this.fetchTemplatesCallback(keywordArgs, responseEntity);
                });

            this.currentRequest = keywordArgs;
            this.currentRequest.abort = function () {};
            return this.currentRequest;
        }
    },

    fetchTemplatesCallback: function (keywordArgs, templatesEntity) {
        var scope = keywordArgs.scope;
        if (keywordArgs.onBegin) {
            keywordArgs.onBegin.call(
                scope ? scope : dojo.global,
                templatesEntity.pagination.totalEntries,
                this.currentRequest
            );
        }

        if (keywordArgs.onItem) {
            let templatesArray = templatesEntity.entity;

            dojo.forEach(
                templatesArray,
                function (template) {
                    keywordArgs.onItem.call(
                        scope ? scope : dojo.global,
                        template,
                        this.currentRequest
                    );
                },
                this
            );
        }

        if (keywordArgs.onComplete) {
            keywordArgs.onComplete.call(
                scope ? scope : dojo.global,
                templatesEntity.entity,
                this.currentRequest
            );
        }
    },

    fetchCallback: function (keywordArgs, templatesEntity) {
        var scope = keywordArgs.scope;
        if (keywordArgs.onBegin) {
            keywordArgs.onBegin.call(
                scope ? scope : dojo.global,
                templatesEntity.pagination.totalEntries,
                this.currentRequest
            );
        }

        if (keywordArgs.onItem) {
            dojo.forEach(
                templatesEntity.entity,
                function (template) {
                    keywordArgs.onItem.call(
                        scope ? scope : dojo.global,
                        template,
                        this.currentRequest
                    );
                },
                this
            );
        }

        if (keywordArgs.onComplete) {
            keywordArgs.onComplete.call(
                scope ? scope : dojo.global,
                templatesEntity.entity,
                this.currentRequest
            );
        }
    },

    fetchItemByIdentity: function (request) {
        const templateId = request.identity;
        fetch('/api/v1/templates/' + templateId + '/working')
            .then(async (response) => {
                // The ok value represents the result of the response status 200 codes
                if (response.ok) {
                    const result = await response.json();

                    this.fetchItemByIdentityCallback(request, result.entity); // here we pass the result of the json response to the callback function
                }
            })
            .catch((e) => {
                console.log(e); // Here we can catch the error
            });
    },

    fetchItemByIdentityCallback: function (request, template) {
        var scope = request.scope;
        if (request.onItem) {
            request.onItem.call(scope ? scope : dojo.global, template, this.currentRequest);
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
        return ['hostName', 'fullTitle'];
    },

    getIdentity: function (item) {
        return item['identifier'];
    }
});
