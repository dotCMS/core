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
	
	
    fetch:function(request) {
    	var searchName = dijit.byId(this.nodeId).get("displayedValue");
        request.serverQuery = {
        	"getRoles":request.cmd,
        	"q":request.query,
        	"searchName":searchName, 
        	"roleId":request.identity,
        	"start":request.start,
        	"count":request.count,
        	"includeFake":this.includeFake
        };
        return this.inherited("fetch", arguments);
    },
    
    fetchItemByIdentity : function (args){
         this.fetch(args);
    }
});