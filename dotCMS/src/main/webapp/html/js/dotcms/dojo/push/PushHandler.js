dojo.provide("dotcms.dojo.push.PushHandler");
dojo.require("dojo.html");
dojo.require("dojox.html._base");
dojo.require("dijit._Widget");
dojo.require("dijit.Dialog");
dojo.require("dotcms.dijit.RemotePublisherDialog");
dojo.require("dotcms.dijit.AddToBundleDialog");
dojo.require("dojox.data.QueryReadStore");
dojo.require("dojox.data.JsonRestStore");
dojo.require("dojo.NodeList-traverse");

const LAST_BUNDLE_USED = 'lastSelectedBundle';

dojo.declare("dotcms.dojo.push.PushHandler", null, {

    assetIdentifier: "",
    dialog: null,
    title: "",
    bundleStore : null,
    environmentStore : null,
    roleReadStore :null,
    user : null,
    whereToSend : [],
    isBundle : false,
    inialStateEnvs : [],

    workflow:null,
    //Optional params when a Workflow-action like behavior is desired.
    /* The stored info should look like this:
    workflow:{
        actionId:null,
        inode:null,
        publishDate:null,
        expireDate:null,
        structInode:null
    },
    */
    fireWorkflowDelegate:null,

    constructor: function (title, isBundle) {
        this.title = title;
        this.isBundle = isBundle;
        this.setUser();
        this.createStore();
    },

    createStore: function() {
        this.environmentStore = new dojox.data.JsonRestStore({ target: "/api/environment/loadenvironments/roleId/"+this.user.roleId, labelAttribute:"name", urlPreventCache: true});
    },

    setUser: function() {
    	var xhrArgs = {
    			url : "/api/user/getloggedinuser/",
    			handleAs : "json",
    			sync: true,
    			load : dojo.hitch(this, "userLoaded"),
    			error : function(error) {
    				console.log("An unexpected error occurred: " + error);
    			}
    		};

    	var def = dojo.xhrGet(xhrArgs);
    },

    userLoaded: function(data) {
    	this.user = data;
    },

    showDialog: function (assetId, displayDateFilter, displayRemoveOnly) {
    	if(this.environmentStore==null) {
    		this.environmentStore = new dojox.data.JsonRestStore({ target: "/api/environment/loadenvironments/roleId/"+this.user.roleId, labelAttribute:"name", urlPreventCache: true});

    	}

    	this.clear();

        var dateFilter = false;
        if (displayDateFilter != undefined && displayDateFilter != null) {
            dateFilter = displayDateFilter;
        }
        var removeOnly = false;
        if (displayRemoveOnly != undefined && displayRemoveOnly != null) {
        	removeOnly = displayRemoveOnly;
        }

        this.assetIdentifier = assetId;
        dialog = new dotcms.dijit.RemotePublisherDialog();
        dialog.title = this.title;
        dialog.dateFilter = dateFilter;
        dialog.removeOnly = removeOnly;
        dialog.container = this;
        dialog.show();

        var self = this;
        setTimeout(function() {
	        self.environmentStore.fetch({
	    		onComplete:function(items,request) {
                    //Pre-select the environment only if there is one single instance.
                    items = items.filter(item => item.id != '0');
                    if(items.length === 1) {
                        self.addToWhereToSend(items[0].id, items[0].name);
                        self.refreshWhereToSend();
                    }
	    		}
	    	})},200);

    },

    showRestrictedDialog: function (assetId, displayDateFilter) {
    	if(this.environmentStore == null) {
    		this.environmentStore = new dojox.data.JsonRestStore({ target: "/api/environment/loadenvironments/roleId/"+this.user.roleId, labelAttribute:"name", urlPreventCache: true});
    	}

    	this.clear();

        var dateFilter = false;
        if (displayDateFilter != undefined && displayDateFilter != null) {
            dateFilter = displayDateFilter;
        }

        this.assetIdentifier = assetId;
        dialog = new dotcms.dijit.RemotePublisherDialog();
        dialog.title = this.title;
        dialog.dateFilter = dateFilter;
        dialog.container = this;
        dialog.restricted = true;
        dialog.show();

        var self = this;
        setTimeout(function() {
	        self.environmentStore.fetch({
	    		onComplete:function(items,request) {
                    //Pre-select the environment only if there is one single instance.
                    items = items.filter(item => item.id != '0');
                    if(items.length === 1) {
                        self.addToWhereToSend(items[0].id, items[0].name);
                        self.refreshWhereToSend();
                    }
	    		}
	    	})},200);

    },

    showAddToBundleDialog: function (assetId, title, displayDateFilter) {

        if (this.bundleStore == null) {
            this.bundleStore = new dojox.data.QueryReadStore({
                url: '/api/bundle/getunsendbundles/userid/' + this.user.userId
            });
        }

        var dateFilter = false;
        if (displayDateFilter != undefined && displayDateFilter != null) {
            dateFilter = displayDateFilter;
        }

        this.assetIdentifier = assetId;
        dialog = new dotcms.dijit.AddToBundleDialog();
        dialog.title = title;
        dialog.dateFilter = dateFilter;
        dialog.container = this;
        dialog.show();
    },

    showCategoryDialog: function () {
    	if(this.environmentStore==null) {
    		this.environmentStore = new dojox.data.JsonRestStore({ target: "/api/environment/loadenvironments/roleId/"+this.user.roleId, labelAttribute:"name", urlPreventCache: true});
    	}

        this.assetIdentifier = "CAT";
        dialog = new dotcms.dijit.RemotePublisherDialog();
        dialog.title = this.title;
        dialog.dateFilter = false;
        dialog.container = this;
        dialog.cats = true;
        dialog.show();

        var self = this;
        setTimeout(function() {
            self.environmentStore.fetch({
                onComplete:function(items,request) {
                    //Pre-select an environment only if there is one.
                    items = items.filter(item => item.id != '0');
                    if(items.length === 1) {
                        self.addToWhereToSend(items[0].id, items[0].name);
                        self.refreshWhereToSend();
                    }
                }
            })},200);
    },

    showWorkflowEnabledDialog:function(workflow, fireWorkflowDelegate){

        this.assetIdentifier = null;

        this.workflow = workflow;
        this.fireWorkflowDelegate = fireWorkflowDelegate;

        if(this.environmentStore == null) {
            this.environmentStore = new dojox.data.JsonRestStore({ target: "/api/environment/loadenvironments/roleId/"+this.user.roleId, labelAttribute:"name", urlPreventCache: true});
        }

        if(this.roleReadStore == null) {
           this.roleReadStore = new dojox.data.QueryReadStore({url: '/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfRoleStoreAjax?cmd=assignable&actionId='+workflow.actionId});
        }

        this.clear();

        dialog = new dotcms.dijit.RemotePublisherDialog();
        dialog.title = this.title;
        dialog.container = this;
        dialog.workflow = this.workflow;
        dialog.show();

        var self = this;
        setTimeout(function() {
            self.environmentStore.fetch({
                onComplete:function(items,request) {
                    //Pre-select the environment only if there is one single instance.
                    items = items.filter(item => item.id != '0');
                    if(items.length === 1) {
                        self.addToWhereToSend(items[0].id, items[0].name);
                        self.refreshWhereToSend();
                    }
                }
            })},200);

    },

    togglePublishExpireDivs: function () {

        var x = "publish";
        if (dijit.byId("iwtExpire").getValue() != false) {
            x = "expire";
        }
        else if (dijit.byId("iwtPublishExpire").getValue() != false) {
            x = "publishexpire";
        }

        if ("publish" == x) {
            dojo.style("publishTimeDiv", "display", "");
            dojo.style("expireTimeDiv", "display", "none");
        } else if ("publishexpire" == x) {
            dojo.style("publishTimeDiv", "display", "");
            dojo.style("expireTimeDiv", "display", "");
        }
        else {
            dojo.style("publishTimeDiv", "display", "none");
            dojo.style("expireTimeDiv", "display", "");
        }
    },

    /**
     * Returns the filter date value for the filtering box if there is any
     * @returns {string}
     * @private
     */
    _getFilterDate: function (atb) {

    	var addToBundle = atb!=null?"_atb":"";

        var filterDiv = dojo.byId("filterTimeDiv"+addToBundle);
        if (filterDiv && filterDiv.style.display == "") {
            var filterDate = (dijit.byId("wfFilterDateAux"+addToBundle) && dijit.byId("wfFilterDateAux"+addToBundle) != 'undefined')
                ? dojo.date.locale.format(dijit.byId("wfFilterDateAux"+addToBundle).getValue(), {datePattern: "yyyy-MM-dd", selector: "date"})
                    : (dojo.byId("wfFilterDateAux"+addToBundle) && dojo.byId("wfFilterDateAux"+addToBundle) != 'undefined')
                        ? dojo.date.locale.format(dojo.byId("wfFilterDateAux"+addToBundle).value, {datePattern: "yyyy-MM-dd", selector: "date"})
                            : "";

            var filterTime = (dijit.byId("wfFilterTimeAux"+addToBundle))
                ? dojo.date.locale.format(dijit.byId("wfFilterTimeAux"+addToBundle).getValue(), {timePattern: "H-m", selector: "time"})
                    : (dojo.byId("wfFilterTimeAux"+addToBundle))
                        ? dojo.date.locale.format(dojo.byId("wfFilterTimeAux"+addToBundle).value, {timePattern: "H-m", selector: "time"})
                            : "";

            return filterDate + "-" + filterTime;
        } else {
            return "";
        }
    },

    /**
     * This method continues to be the entry point.
     * It didn't get renamed to avoid backwards compatibility issues
     */
	remotePublish : function(){

        var dojoStyle = dojo.require("dojo.dom-style");

		if((dojo.byId("whereToSend") && this.whereToSend.length === 0)) {
            showDotCMSSystemMessage(dojo.byId("whereToSendRequired").value);
			return;
		}

		// BEGIN: PUSH PUBLISHING ACTIONLET These are the fields that we set on the publishForm (The dialog)

		var publishDate = (dijit.byId("wfPublishDateAux"))
			? dojo.date.locale.format(dijit.byId("wfPublishDateAux").getValue(),{datePattern: "yyyy-MM-dd", selector: "date"})
				: (dojo.byId("wfPublishDateAux"))
					? dojo.date.locale.format(dojo.byId("wfPublishDateAux").value,{datePattern: "yyyy-MM-dd", selector: "date"})
							: "";

		var publishTime = (dijit.byId("wfPublishTimeAux"))
			? dojo.date.locale.format(dijit.byId("wfPublishTimeAux").getValue(),{timePattern: "H-m", selector: "time"})
				: (dojo.byId("wfPublishTimeAux"))
					? dojo.date.locale.format(dojo.byId("wfPublishTimeAux").value,{timePattern: "H-m", selector: "time"})
							: "";

		//The following two components (wfExpireDateAux,wfExpireTimeAux) are hidden by default.
        //They were part of the push publish dialog that was used on the PushPublish-Actionlet. But now they've been disabled.
        //Though they remain invisible in case it is decided they 're needed back.
        var expireDate = '';
        var expireTime = '';

        if(dojo.byId('expireTimeDiv') && dojoStyle.get('expireTimeDiv','display') !== 'none') {

            expireDate = (dijit.byId("wfExpireDateAux"))
                ? dijit.byId("wfExpireDateAux").getValue() != null
                    ? dojo.date.locale.format(
                        dijit.byId("wfExpireDateAux").getValue(),
                        {datePattern: "yyyy-MM-dd", selector: "date"}) : ""
                : (dojo.byId("wfExpireDateAux"))
                    ? dojo.byId("wfExpireDateAux").value != null
                        ? dojo.date.locale.format(
                            dojo.byId("wfExpireDateAux").value,
                            {datePattern: "yyyy-MM-dd", selector: "date"}) : ""
                    : "";

            expireTime = (dijit.byId("wfExpireTimeAux"))
                ? dijit.byId("wfExpireTimeAux").getValue() != null
                    ? dojo.date.locale.format(
                        dijit.byId("wfExpireTimeAux").getValue(),
                        {timePattern: "H-m", selector: "time"}) : ""
                : (dojo.byId("wfExpireTimeAux"))
                    ? dojo.byId("wfExpireTimeAux").value != null
                        ? dojo.date.locale.format(
                            dojo.byId("wfExpireTimeAux").value,
                            {timePattern: "H-m", selector: "time"}) : ""
                    : "";

        }

		var iWantTo = (dijit.byId("publishForm").attr('value').wfIWantTo)
		? dijit.byId("publishForm").attr('value').wfIWantTo
			: (dijit.byId("publishForm").attr('value').wfIWantTo)
				? dijit.byId("publishForm").attr('value').wfIWantTo
						: "";

		var whereToSend =  (dojo.byId("whereToSend") ? dojo.byId("whereToSend").value : "");

		var forcePush =  (dijit.byId("forcePush") ? dijit.byId("forcePush").checked : "");

		// END: PUSH PUBLISHING ACTIONLET


        if(this._isWorkflowEnabled()){

            let actionId = this.workflow.actionId;

            var hasCondition = (dojo.byId("hasCondition") ? dojo.byId("hasCondition").value : "");

            if(hasCondition === 'true'){
                this.evaluateCondition(actionId);
                return;
            }

            var comment = (dijit.byId("taskCommentsAux")
                ? dijit.byId("taskCommentsAux").getValue()
                : dojo.byId("taskCommentsAux")
                    ? dojo.byId("taskCommentsAux").value
                    : "");

            var assign = (dijit.byId("taskAssignmentAux")
                ? dijit.byId("taskAssignmentAux").getValue()
                : dojo.byId("taskAssignmentAux")
                    ? dojo.byId("taskAssignmentAux").value
                    : "");

            if(dijit.byId("taskAssignmentAux") && !assign){
                showDotCMSSystemMessage(dojo.byId("assignToRequired").value);
                return;
            }

            let inode = this.workflow.inode;
            let structureInode = this.workflow.structureInode;

            //if these are set, then neverExpire should be false.
            let neverExpire = !(expireDate || expireTime);

            let assignComment = {
                comment: comment,
                assign: assign,
            };

            let pushPublish = {
                whereToSend:whereToSend,
                publishDate:publishDate,
                publishTime:publishTime,
                expireDate:expireDate,
                expireTime:expireTime,
                forcePush:forcePush,
                inode:inode,
                actionId:actionId,
                structureInode:structureInode,
                hasCondition:hasCondition,
                neverExpire:neverExpire
            };

            let formData = {
                assignComment:assignComment,
                pushPublish:pushPublish
            };

           //Hide the buttons and display the progress
           dojo.query("#publishForm .buttonRow").style("display", "none");
           dojo.query("#publishForm .progressRow").style("display", "block");

           this.fireWorkflowDelegate(actionId, formData);
           this.fireWorkflowDelegate = null;
           this.workflow = null;
           dialog.hide();
           return;
        }


        // BEGIN: PUSH PUBLISHING ACTIONLET
        dojo.byId("remoteAssetIdentifier").value = this.assetIdentifier;
        dojo.byId("remotePublishDate").value = publishDate;
        dojo.byId("remotePublishTime").value = publishTime;
        dojo.byId("remotePublishExpireDate").value = expireDate;
        dojo.byId("remotePublishExpireTime").value = expireTime;
        dojo.byId("remoteIWantTo").value = iWantTo;
        if (dojo.byId("remoteFilterDate")) {
            dojo.byId("remoteFilterDate").value = this._getFilterDate();
        }
        dojo.byId("remoteWhoToSend").value = whereToSend;
        dojo.byId("remoteForcePush").value = forcePush;
		// END: PUSH PUBLISHING ACTIONLET

        //Hide the buttons and display the progress
        dojo.query("#publishForm .buttonRow").style("display", "none");
        dojo.query("#publishForm .progressRow").style("display", "block");

        var currentObject = this;
        var urlStr = this.isBundle ? "/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/pushBundle":"/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/publish";
		var xhrArgs = {
			url: urlStr,
			form: dojo.byId("remotePublishForm"),
			handleAs: "json",
			load: function(data){

                //Display the results to the user if required
                if (data != undefined && data != null) {
                    currentObject._showResultMessage(data);
                }

				dialog.hide();
			},
			error: function(error){
                showDotCMSSystemMessage(error, true);

                //Show the buttons and hide the progress
                dojo.query("#publishForm .buttonRow").style("display", "block");
                dojo.query("#publishForm .progressRow").style("display", "none");

				dialog.hide();
			}
		};

		var deferred = dojo.xhrPost(xhrArgs);
	},

    addToBundle: function () {

        var lastSelectedBundle = JSON.parse(sessionStorage.getItem(LAST_BUNDLE_USED));

        if (dijit.byId("bundleSelect").value == '') {
            showDotCMSSystemMessage(dojo.byId("bundleRequired").value);
            return;
        }

        // BEGIN: PUSH PUBLISHING ACTIONLET

        //Get the selected bundle
        var selectedBundle = dijit.byId("bundleSelect").item;

        var bundleName;
        var bundleId;
        if (selectedBundle != undefined) {
            bundleName = selectedBundle.i.name;
            bundleId = selectedBundle.i.id;
        } else {
            bundleName = dijit.byId("bundleSelect").value;
            bundleId = dijit.byId("bundleSelect").value;
        }

        if (bundleId !== undefined && bundleId !== null) {
            lastSelectedBundle = {name: bundleName, id: bundleId};
        }

        // END: PUSH PUBLISHING ACTIONLET


        // BEGIN: PUSH PUBLISHING ACTIONLET
        dojo.byId("remoteAssetIdentifier").value = this.assetIdentifier;
        if (dojo.byId("remoteFilterDate")) {
            dojo.byId("remoteFilterDate").value = this._getFilterDate('true');
        }
        dojo.byId("remoteBundleName").value = bundleName;
        dojo.byId("remoteBundleSelect").value = bundleId;
        // END: PUSH PUBLISHING ACTIONLET

        //Disable the save button
        dojo.query("#publishForm .buttonRow").style("display", "none");
        dojo.query("#publishForm .progressRow").style("display", "block");

        var currentObject = this;
        var xhrArgs = {
            url: "/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/addToBundle",
            form: dojo.byId("remotePublishForm"),
            handleAs: "json",
            load: function (data) {

                //Display the results to the user if required
                if (data != undefined && data != null) {
                    currentObject._showResultMessage(data);
                }

                dialog.hide();
            },
            error: function (error) {
                showDotCMSSystemMessage(error, true);

                //Show the buttons and hide the progress
                dojo.query("#publishForm .buttonRow").style("display", "block");
                dojo.query("#publishForm .progressRow").style("display", "none");

                dialog.hide();
            }
        };
        dojo.xhrPost(xhrArgs);

        sessionStorage.setItem(LAST_BUNDLE_USED,JSON.stringify(lastSelectedBundle));

    },

    _showResultMessage : function (data) {

        //Show the buttons and hide the progress
        dojo.query("#publishForm .buttonRow").style("display", "block");
        dojo.query("#publishForm .progressRow").style("display", "none");

        //var total = data.total;
        var errors = data.errors;
        if (errors != null && errors != undefined && errors > 0) {

            var errorMessages = data.errorMessages;

            var messages = "";
            dojo.forEach(errorMessages, function(value, index){
                messages += "<br>" + value;
            });
            showDotCMSSystemMessage(messages, true);
        }
    },

	addSelectedToWhereToSend : function (){

		var select = dijit.byId("environmentSelect");

		var user = select.getValue();
		var userName = select.attr('displayedValue');

		this.addToWhereToSend(user, userName);
		this.refreshWhereToSend();

		select.set('value', '0');
	},

	addToWhereToSend: function ( myId, myName){

        if (myId == undefined || myId == null || myId == "" || myId == '0') {
            return;
        }

        for(i=0;i < this.whereToSend.length;i++){
			if(myId == this.whereToSend[i].id  ||  myId == "user-" + this.whereToSend[i].id || myId == "role-" + this.whereToSend[i].id){
				return;
			}
		}

		var entry = {name:myName,id:myId };
		this.whereToSend[this.whereToSend.length] =entry;
	},

	refreshWhereToSend: function(){

        var lastSelectedEnvironments = JSON.parse(sessionStorage.getItem("lastSelectedEnvironments"));

        var self = this;
		dojo.empty("whereToSendTable");
		var table = dojo.byId("whereToSendTable");
		var x = "";

        if (lastSelectedEnvironments ==  undefined || lastSelectedEnvironments == null || lastSelectedEnvironments.length == 0) {
            lastSelectedEnvironments = [];
        }

		this.whereToSend = this.whereToSend.sort(function(a,b){
			var x = a.name.toLowerCase();
		    var y = b.name.toLowerCase();
		    return ((x < y) ? -1 : ((x > y) ? 1 : 0));
		});


		for(i=0; i< this.whereToSend.length ; i++){

            lastSelectedEnvironments[i] = {name: this.whereToSend[i].name, id: this.whereToSend[i].id};

			var what = (this.whereToSend[i].id.indexOf("user") > -1) ? " EnvironmentNotLanguaged" : "";
			x = x + this.whereToSend[i].id + ",";
			var tr = dojo.create("tr", null, table);
            const id = self.whereToSend[i].id;
            var td = dojo.create("td", { width: 10, innerHTML: "<span class='deleteIcon'></span>",className:"wfXBox", id:id }, tr);
			dojo.create("td", { innerHTML: this.whereToSend[i].name + what}, tr);

            dojo.connect(td, "onclick", function(e){
                //Dunno why the 'target' property of the event doesn't not point to the real element that originates the event.
                var target = dojo.query(e.target).parent("td")[0];
                self.removeFromWhereToSend(target.id);
                self.refreshWhereToSend();
            });
		}

		dojo.byId('whereToSend').value = x;

		sessionStorage.setItem("lastSelectedEnvironments",JSON.stringify(lastSelectedEnvironments));
	},

	removeFromWhereToSend: function(myId){

        var lastSelectedEnvironments = JSON.parse(sessionStorage.getItem("lastSelectedEnvironments"));

		var x=0;
		var newCanUse = [];
		for(i=0;i < this.whereToSend.length;i++){
			if(myId != this.whereToSend[i].id){
				newCanUse[x] = this.whereToSend[i];
				x++;
			}
		}
		this.whereToSend = newCanUse;

		for(i=0; i< lastSelectedEnvironments.length ; i++){
			if(lastSelectedEnvironments[i].id == myId) {
			   lastSelectedEnvironments.splice(i,1);
			}
		}

        sessionStorage.setItem("lastSelectedEnvironments",JSON.stringify(lastSelectedEnvironments));
	},

    clear: function () {
        this.whereToSend = [];
    },

    _isWorkflowEnabled: function(){
        return (this.workflow !== null);
    },

    evaluateCondition: function (actionId) {

        let urlTemplate = "/api/v1/workflow/actions/{actionId}/condition";
        const url = urlTemplate.replace('{actionId}',actionId);
        let dataAsJson = {};
        let xhrArgs = {
            url: url,
            postData: dataAsJson,
            handleAs: "json",
            headers : {
                'Accept' : 'application/json',
                'Content-Type' : 'application/json;charset=utf-8',
            },
            load: function(data) {
                var html = data.entity;
                dojox.html.set(dojo.byId("pushPublish-container"), html, {
                    executeScripts: true,
                    renderStyles: true,
                    scriptHasHooks: true,
                    parseContent: true
                });
            },
            error: function(error){
                console.error(error);
                showDotCMSSystemMessage(error, true);
            }
        };

        dojo.xhrGet(xhrArgs);
    }

});