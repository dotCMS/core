dojo.provide("dotcms.dojo.data.SiteSelectorReadStore");

dojo.require("dojo.data.api.Read");
dojo.require("dojo.data.api.Request");

dojo.declare("dotcms.dojo.data.SiteSelectorReadStore", null, {

     currentRequest: null,
     SYSTEM_SITE_IDENTIFIER: 'SYSTEM_HOST',
     SYSTEM_SITE_LABEL: null,

     constructor: function (props) {
          if (props && props.allSitesLabel) {
               this.SYSTEM_SITE_LABEL = props.allSitesLabel;
          }
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
          return item[type] == 'hosts';
     },

     isItemLoaded: function (item) {
          return this.isItem(item) ? true : false;
     },

     loadItem: function (keywordArgs) {

          if (!this.isItem(keywordArgs.item))
               keywordArgs.onError.call(scope ? scope : dojo.global(), {message: 'passed item is not a valid host item'});

          var scope = keywordArgs.scope;
          keywordArgs.onItem.call(scope ? scope : dojo.global(), keywordArgs.item);

     },

     fetch: function (keywordArgs) {

          var fetchCallback = dojo.hitch(this, this.fetchCallback, keywordArgs);
          HostAjax.findHostsForDataStore(keywordArgs.query.hostname, false, 0, 0, true, fetchCallback);

          this.currentRequest = keywordArgs;
          this.currentRequest.abort = function () {
          };
          return this.currentRequest;
     },

     fetchCallback: function (keywordArgs, hosts) {

          dojo.forEach(hosts.list, function (host) {
               //Change the label for the System Site
               if (host.identifier === this.SYSTEM_SITE_IDENTIFIER) {
                    if (this.SYSTEM_SITE_LABEL) {
                         host.hostname = this.SYSTEM_SITE_LABEL;
                    }
               }
          }, this);

          var scope = keywordArgs.scope;
          if (keywordArgs.onBegin) {
               keywordArgs.onBegin.call(scope ? scope : dojo.global, hosts.total, this.currentRequest);
          }

          if (keywordArgs.onItem) {
               dojo.forEach(hosts.list, function (host) {
                    keywordArgs.onItem.call(scope ? scope : dojo.global, host, this.currentRequest);
               }, this);
          }

          if (keywordArgs.onComplete) {
               keywordArgs.onComplete.call(scope ? scope : dojo.global, hosts.list, this.currentRequest);
          }

     },

     fetchItemByIdentity: function (request) {
          HostAjax.fetchByIdentity(request.identity, dojo.hitch(this, this.fetchItemByIdentityCallback, request));
     },

     fetchItemByIdentityCallback: function (request, host) {

          //Change the label for the System Site
          if (host.identifier === this.SYSTEM_SITE_IDENTIFIER) {
               if (this.SYSTEM_SITE_LABEL) {
                    host.hostname = this.SYSTEM_SITE_LABEL;
               }
          }

          var scope = request.scope;
          if (request.onItem) {
               request.onItem.call(scope ? scope : dojo.global, host, this.currentRequest);
          }
     },

     getFeatures: function () {
          return {'dojo.data.api.Read': true};
     },

     close: function (request) {
          this.currentRequest = null;
     },

     getLabel: function (item) {
          return item['hostname'];
     },

     getLabelAttributes: function (item) {
          return ['hostname'];
     },

     getIdentity: function (item) {
          return item['identifier'];
     }

});