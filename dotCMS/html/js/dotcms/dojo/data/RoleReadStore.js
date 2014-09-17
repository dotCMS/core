dojo.provide("dotcms.dojo.data.RoleReadStore");
dojo.require("dojox.data.QueryReadStore");



dojo.declare("dotcms.dojo.data.RoleReadStore", dojox.data.QueryReadStore, {



	nodeId:"",
	url: '/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfRoleStoreAjax',
	includeFake:false,

	constructor: function (options) {
		this.nodeId = options.nodeId;
		this.includeFake = options.includeFake!=""?options.includeFake:false;
	},


    fetch: function (request) {

        var searchName = dijit.byId(this.nodeId).get("displayedValue");
        var query = request.query;

        if (query) {
            if (query.name && (!searchName || searchName == "") && (query.name != "*")) {
                searchName = query.name;
            }

            if (query.name) {
                query = query.name;
            } else if (query.toString() == "[object Object]") {
                query = "*";
            }
        }

        /*
         We can exclude the "(" ")" as the rol is not really use it in order to filter the store,
         actually write in the select box to the point of write part of the role will return no results,
         just the name is what it actually expects for filtering.
         */
        if (searchName) {
            searchName = searchName.replace("(", "").replace(")", "");//We are not using the role for filter and we are generating XSS errors sending "(" or ")"
        }

        if (query) {
            /*
             We can exclude unneeded characters here as we don' really use this "query" inside our ajax class,
             is used only by the store component and the role is not really use on the server filter search.
             */
            query = query.replace("(", "").replace(")", "");
        }

        request.serverQuery = {
            "getRoles": request.cmd,
            "q": query,
            "searchName": searchName,
            "roleId": request.identity,
            "start": request.start,
            "count": request.count,
            "includeFake": this.includeFake
        };
        return this.inherited("fetch", arguments);
    },

    fetchItemByIdentity : function (args){
         this.fetch(args);
    }
});