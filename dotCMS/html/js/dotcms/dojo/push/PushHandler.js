dojo.provide("dotcms.dojo.push.PushHandler");

dojo.require("dijit._Widget");
dojo.require("dijit.Dialog");
dojo.require("dotcms.dijit.RemotePublisherDialog");
dojo.require("dotcms.dijit.AddToBundleDialog");
dojo.require("dojox.data.JsonRestStore");

dojo.declare("dotcms.dojo.push.PushHandler", null, {

    assetIdentifier: "",
    dialog: null,
    title: "",
    bundleStore : null,
    environmentStore : null,
    user : null,
    whereToSend : new Array(),
    isBundle : false,

    constructor: function (title, isBundle) {
        this.title = title;
        this.isBundle = isBundle;
        this.setUser();
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
    		}

    	var def = dojo.xhrGet(xhrArgs);
    },

    userLoaded: function(data) {
    	this.user = data;
    },

    showDialog: function (assetId, displayDateFilter) {
    	if(this.environmentStore==null) {
    		this.environmentStore = new dojox.data.JsonRestStore({ target: "/api/environment/loadenvironments/roleId/"+this.user.roleId, labelAttribute:"name", urlPreventCache: true});
    	}

        var dateFilter = false;
        if (displayDateFilter != undefined && displayDateFilter != null) {
            dateFilter = displayDateFilter;
        }

        this.assetIdentifier = assetId;
        dialog = new dotcms.dijit.RemotePublisherDialog();
        dialog.title = this.title;
        dialog.dateFilter = dateFilter;
        dialog.show();
    },

    showAddToBundleDialog: function (assetId, title) {
    	if(this.bundleStore==null) {
    		this.bundleStore = new dojox.data.JsonRestStore({ target: "/api/bundle/getunsendbundles/userid/"+this.user.userId, labelAttribute:"name", urlPreventCache: true});
    	}

        this.assetIdentifier = assetId;
        dialog = new dotcms.dijit.AddToBundleDialog();
        dialog.title = title;
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
        dialog.show();
    },

    remoteUnPublish: function (assetId) {
        var xhrArgs = {
            url: "/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/unPublish",
            content: {
                'assetIdentifier': assetId
            },
            handleAs: "text",
            load: function (data) {
                if (data.indexOf("FAILURE") > -1) {
                    alert(data);
                }
            },
            error: function (error) {
                alert(error);
            }
        };

        var deferred = dojo.xhrPost(xhrArgs);
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

    toggleNewExistingBundle: function () {

        if (dijit.byId("newBundle").getValue()) {
            dojo.style("bundleNameDiv", "display", "");
            dojo.style("bundleSelectDiv", "display", "none");
            dijit.byId("bundleName").focus();
        }
        else {
            dojo.style("bundleNameDiv", "display", "none");
            dojo.style("bundleSelectDiv", "display", "");
        }
    },

    /**
     * Returns the filter date value for the filtering box if there is any
     * @returns {string}
     * @private
     */
    _getFilterDate: function () {

        var filterDiv = dojo.byId("filterTimeDiv");
        if (filterDiv && filterDiv.style.display == "") {
            var filterDate = (dijit.byId("wfFilterDateAux") && dijit.byId("wfFilterDateAux") != 'undefined')
                ? dojo.date.locale.format(dijit.byId("wfFilterDateAux").getValue(), {datePattern: "yyyy-MM-dd", selector: "date"})
                    : (dojo.byId("wfFilterDateAux") && dojo.byId("wfFilterDateAux") != 'undefined')
                        ? dojo.date.locale.format(dojo.byId("wfFilterDateAux").value, {datePattern: "yyyy-MM-dd", selector: "date"})
                            : "";

            var filterTime = (dijit.byId("wfFilterTimeAux"))
                ? dojo.date.locale.format(dijit.byId("wfFilterTimeAux").getValue(), {timePattern: "H-m", selector: "time"})
                    : (dojo.byId("wfFilterTimeAux"))
                        ? dojo.date.locale.format(dojo.byId("wfFilterTimeAux").value, {timePattern: "H-m", selector: "time"})
                            : "";

            return filterDate + "-" + filterTime;
        } else {
            return "";
        }
    },

	remotePublish : function(){

		if(this.whereToSend.length==0) {
			alert(dojo.byId("whereToSendRequired").value);
			return;
		}

		// BEGIN: PUSH PUBLISHING ACTIONLET

		var publishDate = (dijit.byId("wfPublishDateAux") && dijit.byId("wfPublishDateAux")!='undefined')
			? dojo.date.locale.format(dijit.byId("wfPublishDateAux").getValue(),{datePattern: "yyyy-MM-dd", selector: "date"})
				: (dojo.byId("wfPublishDateAux") && dojo.byId("wfPublishDateAux")!='undefined')
					? dojo.date.locale.format(dojo.byId("wfPublishDateAux").value,{datePattern: "yyyy-MM-dd", selector: "date"})
							: "";

		var publishTime = (dijit.byId("wfPublishTimeAux"))
			? dojo.date.locale.format(dijit.byId("wfPublishTimeAux").getValue(),{timePattern: "H-m", selector: "time"})
				: (dojo.byId("wfPublishTimeAux"))
					? dojo.date.locale.format(dojo.byId("wfPublishTimeAux").value,{timePattern: "H-m", selector: "time"})
							: "";


		var expireDate = (dijit.byId("wfExpireDateAux"))
			? dijit.byId("wfExpireDateAux").getValue()!=null ? dojo.date.locale.format(dijit.byId("wfExpireDateAux").getValue(),{datePattern: "yyyy-MM-dd", selector: "date"}) : ""
				: (dojo.byId("wfExpireDateAux"))
					? dojo.byId("wfExpireDateAux").value!=null ? dojo.date.locale.format(dojo.byId("wfExpireDateAux").value,{datePattern: "yyyy-MM-dd", selector: "date"}) : ""
							: "";

		var expireTime = (dijit.byId("wfExpireTimeAux"))
			? dijit.byId("wfExpireTimeAux").getValue()!=null ? dojo.date.locale.format(dijit.byId("wfExpireTimeAux").getValue(),{timePattern: "H-m", selector: "time"}) : ""
				: (dojo.byId("wfExpireTimeAux"))
					? dojo.byId("wfExpireTimeAux").value!=null ? dojo.date.locale.format(dojo.byId("wfExpireTimeAux").value,{timePattern: "H-m", selector: "time"}) : ""
							: "";

		var iWantTo = (dijit.byId("publishForm").attr('value').wfIWantTo)
		? dijit.byId("publishForm").attr('value').wfIWantTo
			: (dijit.byId("publishForm").attr('value').wfIWantTo)
				? dijit.byId("publishForm").attr('value').wfIWantTo
						: "";

		var whereToSend = dojo.byId("whereToSend").value;


		// END: PUSH PUBLISHING ACTIONLET


		// BEGIN: PUSH PUBLISHING ACTIONLET
        dojo.byId("assetIdentifier").value = this.assetIdentifier;
        dojo.byId("remotePublishDate").value = publishDate;
        dojo.byId("remotePublishTime").value = publishTime;
        dojo.byId("remotePublishExpireDate").value = expireDate;
        dojo.byId("remotePublishExpireTime").value = expireTime;
        dojo.byId("iWantTo").value = iWantTo;
        if (dojo.byId("remoteFilterDate")) {
            dojo.byId("remoteFilterDate").value = this._getFilterDate();
        }
        dojo.byId("whoToSend").value = whereToSend;
		// END: PUSH PUBLISHING ACTIONLET

        var urlStr = this.isBundle?"/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/pushBundle":"/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/publish";

		var xhrArgs = {
			url: urlStr,
			form: dojo.byId("remotePublishForm"),
			handleAs: "text",
			load: function(data){
				if(data.indexOf("FAILURE") > -1){
					alert(data);
				}
				dialog.hide();
			},
			error: function(error){
				alert(error);
				dialog.hide();
			}
		};

		var deferred = dojo.xhrPost(xhrArgs);
	},

	addToBundle : function(){

		console.log(dijit.byId("addToBundleForm").attr('value').newBundle);

		if(dijit.byId("addToBundleForm").attr('value').newBundle=='true' && dijit.byId("bundleName").value=='') {
			alert(dojo.byId("bundleNameRequired").value);
			return;
		} else if(dijit.byId("addToBundleForm").attr('value').newBundle=='false' && dijit.byId("bundleSelect").value==''){
			alert(dojo.byId("bundleRequired").value);
			return;
		}

		// BEGIN: PUSH PUBLISHING ACTIONLET

		var newBundle = dijit.byId("addToBundleForm").attr('value').newBundle;
		var bundleName = dijit.byId("bundleName").value;
		var bundleSelect = dijit.byId("bundleSelect").value;


		// END: PUSH PUBLISHING ACTIONLET


		// BEGIN: PUSH PUBLISHING ACTIONLET
        dojo.byId("assetIdentifier").value = this.assetIdentifier;
        if (dojo.byId("remoteFilterDate")) {
            dojo.byId("remoteFilterDate").value = this._getFilterDate();
        }
        dojo.byId("newBundle").value = newBundle;
        dojo.byId("bundleName").value = bundleName;
        dojo.byId("bundleSelect").value = bundleSelect;
		// END: PUSH PUBLISHING ACTIONLET


        // verify that a bundle with the given name does not exist
        var xhrArgs = {
    			url : "/api/bundle/doesbundleexist/name/" + bundleName,
    			handleAs : "json",
    			sync: true,
    			load : function(data) {
    				if(data=="true") {
						alert(dojo.byId("existingBundleName").value);
						return;
    				}
    			},
    			error : function(error) {
    				targetNode.innerHTML = "An unexpected error occurred: " + error;
    			}
    		}

		var xhrArgs = {
			url: "/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/addToBundle",
			form: dojo.byId("remotePublishForm"),
			handleAs: "text",
			load: function(data){
				if(data.indexOf("FAILURE") > -1){
					alert(data);
				}
				dialog.hide();
			},
			error: function(error){
				alert(error);
				dialog.hide();
			}
		};

		var deferred = dojo.xhrPost(xhrArgs);
	},

	addSelectedToWhereToSend : function (){

		var select = dijit.byId("environmentSelect");

		var user = select.getValue();
		var userName = select.attr('displayedValue');

		this.addToWhereToSend(user, userName);
		this.refreshWhereToSend();
	},

	addToWhereToSend: function ( myId, myName){
		for(i=0;i < this.whereToSend.length;i++){
			if(myId == this.whereToSend[i].id  ||  myId == "user-" + this.whereToSend[i].id || myId == "role-" + this.whereToSend[i].id){
				return;
			}
		}

		var entry = {name:myName,id:myId };
		this.whereToSend[this.whereToSend.length] =entry;

	},

	refreshWhereToSend: function(){
		dojo.empty("whereToSendTable");
		var table = dojo.byId("whereToSendTable");
		var x = "";

		this.whereToSend = this.whereToSend.sort(function(a,b){
			var x = a.name.toLowerCase();
		    var y = b.name.toLowerCase();
		    return ((x < y) ? -1 : ((x > y) ? 1 : 0));
		});
		for(i=0; i< this.whereToSend.length ; i++){
			var what = (this.whereToSend[i].id.indexOf("user") > -1) ? " EnvironmentNotLanguaged" : "";
			x = x + this.whereToSend[i].id + ",";
			var tr = dojo.create("tr", null, table);
			dojo.create("td", { innerHTML: "<span class='deleteIcon'></span>",className:"wfXBox", onClick:"pushHandler.removeFromWhereToSend('" + this.whereToSend[i].id +"');pushHandler.refreshWhereToSend()" }, tr);
			dojo.create("td", { innerHTML: this.whereToSend[i].name + what}, tr);

		}
		dojo.byId('whereToSend').value = x;

	},

	removeFromWhereToSend: function(myId){

		var x=0;
		var newCanUse = new Array();
		for(i=0;i < this.whereToSend.length;i++){
			if(myId != this.whereToSend[i].id){
				newCanUse[x] = this.whereToSend[i];
				x++;
			}
		}
		this.whereToSend= newCanUse;
	}



});