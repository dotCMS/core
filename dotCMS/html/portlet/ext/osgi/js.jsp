<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%response.setContentType("text/JavaScript");%>
dojo.require("dojo.hash");
dojo.require("dojox.layout.ContentPane");
dojo.require("dojo.data.ItemFileReadStore");
dojo.require("dijit.form.ComboBox");
dojo.require("dijit.form.FilteringSelect");
dojo.require("dijit.form.Button");
dojo.require("dojox.form.Uploader");
dojo.require("dojox.embed.Flash");
dojo.require("dojo.io.iframe");
dojo.subscribe("/dojo/hashchange", this, function(hash){mainAdmin.refresh();});



dojo.declare("dotcms.dijit.osgi.MainAdmin", null, {

	baseDiv : "osgiBundles",
	constructor : function() {
	},
	show : function(href) {

		var r = Math.floor(Math.random() * 1000000000);
		if (href.indexOf("?") > -1) {
			href = href + "&r=" + r;
		} else {
			href = href + "?r=" + r;
		}
		dojo.hash(encodeURIComponent(href));

	},
	
	refresh : function() {
		var hashValue = decodeURIComponent(dojo.hash());

		if(!hashValue || hashValue.length ==0){
			return;
		}
		var hanger = dojo.byId("osgiMain");
		if(!hanger){
            return;
		}

        var myCp = dijit.byId("osgiMainBundles");
        if (myCp) {
            myCp.destroyRecursive(false);
		}

        myCp = new dojox.layout.ContentPane({
            id: "osgiMainBundles",
            preventCache: true
        }).placeAt("osgiMain");

        myCp.attr("href", hashValue );
	}
});



dojo.declare("dotcms.dijit.osgi.Bundles", null, {

	baseJsp : "/html/portlet/ext/osgi/bundles.jsp",
	constructor : function() {
		
	},

	show : function() {
		var href = this.baseJsp;
		mainAdmin.show(href);
	},

	undeploy : function (jarName){
		var xhrArgs = {
			url: "/DotAjaxDirector/com.dotmarketing.portlets.osgi.AJAX.OSGIAJAX?cmd=undeploy&jar=" + jarName,
			handle : function(dataOrError, ioArgs) {
				if (dojo.isString(dataOrError)) {
					if (dataOrError.indexOf("FAILURE") == 0) {

						// needs logging
					} else {
						// needs logging
					}
				} else {
					//this.saveError("<%=LanguageUtil.get(pageContext, "unable-to-save-action")%>");
				}
			}
		};
		dijit.byId('savingOSGIDialog').show();
		dojo.xhrPut(xhrArgs);
		setTimeout(function() {mainAdmin.refresh();},7000);
	},

	deploy : function(){
		var availBundles = dijit.byId('availBundlesCombo');
		if(availBundles.getValue() == undefined || availBundles.getValue()==""){
			return;
		}
		
		var jarName = availBundles.value;
		var xhrArgs = {
			url: "/DotAjaxDirector/com.dotmarketing.portlets.osgi.AJAX.OSGIAJAX?cmd=deploy&jar=" + jarName,
			handle : function(dataOrError, ioArgs) {
				if (dojo.isString(dataOrError)) {
					if (dataOrError.indexOf("FAILURE") == 0) {
						// needs logging
					} else {
						// needs logging
					}
				} else {
					//this.saveError("<%=LanguageUtil.get(pageContext, "unable-to-save-action")%>");
				}
			}
		};
		dijit.byId('savingOSGIDialog').show();
		dojo.xhrPut(xhrArgs);
		setTimeout(function() {mainAdmin.refresh();},7000);
	},

	start : function(bundleId){
		var xhrArgs = {
			url: "/DotAjaxDirector/com.dotmarketing.portlets.osgi.AJAX.OSGIAJAX?cmd=start&bundleId=" + bundleId,
			handle : function(dataOrError, ioArgs) {
				if (dojo.isString(dataOrError)) {
					if (dataOrError.indexOf("FAILURE") == 0) {
						// needs logging
					} else {
						// needs logging
					}
				} else {
					//this.saveError("<%=LanguageUtil.get(pageContext, "unable-to-save-action")%>");
				}
			}
		};
		dijit.byId('savingOSGIDialog').show();
		dojo.xhrPut(xhrArgs);
		setTimeout(function() {mainAdmin.refresh();},7000);
	},

	stop : function(bundleId){
		var xhrArgs = {
			url: "/DotAjaxDirector/com.dotmarketing.portlets.osgi.AJAX.OSGIAJAX?cmd=stop&bundleId=" + bundleId,
			handle : function(dataOrError, ioArgs) {
				if (dojo.isString(dataOrError)) {
					if (dataOrError.indexOf("FAILURE") == 0) {
						// needs logging
					} else {
						// needs logging
					}
				} else {
					//this.saveError("<%=LanguageUtil.get(pageContext, "unable-to-save-action")%>");
				}
			}
		};
		dijit.byId('savingOSGIDialog').show();
		dojo.xhrPut(xhrArgs);
		setTimeout(function() {mainAdmin.refresh();},7000);
	},

	add : function(){
		var fm = dojo.byId("addBundle");

        require(["dojo/io/iframe"], function(ioIframe){
            ioIframe.send({
                // The form node, which contains the
                // data. We also pull the URL and METHOD from it:
                form: fm,
                url : "/DotAjaxDirector/com.dotmarketing.portlets.osgi.AJAX.OSGIAJAX?cmd=add",
                method : "post",
                // The used data format:
                handleAs: "json",

                // Callback on successful call:
                load: function(response, ioArgs) {
                    // return the response for succeeding callbacks
                    setTimeout(function() {mainAdmin.refresh();},7000);
                    return response;
                }
            });
		});
		dijit.byId('uploadOSGIDialog').hide();
		dijit.byId('savingOSGIDialog').show();
		setTimeout(function() {mainAdmin.refresh();},7000);
	},

    reboot : function(askForConfirmation){

        var canContinue = true;
        if (askForConfirmation) {
            canContinue = confirm('<%=LanguageUtil.get(pageContext, "OSGI-restart-confirmation") %>');
        }

        if(canContinue) {
            var xhrArgs = {
                url: "/DotAjaxDirector/com.dotmarketing.portlets.osgi.AJAX.OSGIAJAX?cmd=restart",
                handle : function(dataOrError, ioArgs) {
                    if (dojo.isString(dataOrError)) {
                        if (dataOrError.indexOf("FAILURE") == 0) {
                            // needs logging
                            console.error(dataOrError);
                        } else {
                            // needs logging
                        }
                    } else {
                        // needs logging
                    }
                }
            };
            dijit.byId('savingOSGIDialog').show();
            dojo.xhrPut(xhrArgs);
            setTimeout(function() {mainAdmin.refresh();},7000);
        }
	},

    extraPackages : function (){
        var xhrArgs = {
            url: "/DotAjaxDirector/com.dotmarketing.portlets.osgi.AJAX.OSGIAJAX?cmd=getExtraPackages",
            handle : function(dataOrError, ioArgs) {

                if (dojo.isString(dataOrError)) {
                    if (dataOrError.indexOf("FAILURE") == 0) {
                        console.error(dataOrError);
                    } else {
                        var packages = dataOrError.replace("SUCCESS:", "");
                        packages = packages.replace(/,/g, ",\n");
                        dijit.byId('packages').set("value", packages);
                    }
                } else {
                    //this.saveError("<%=LanguageUtil.get(pageContext, "unable-to-save-action")%>");
                }
            }
        };
        dijit.byId('packagesOSGIDialog').show();
        dojo.xhrGet(xhrArgs);
    },

    modifyExtraPackages : function(){

        var canContinue = confirm('<%=LanguageUtil.get(pageContext, "OSGI-modify-packages-confirmation") %>');
        if(canContinue) {

            var fm = dojo.byId("modifyPackagesForm");

            require(["dojo/io/iframe"], function(ioIframe){
                ioIframe.send({
                    // The form node, which contains the
                    // data. We also pull the URL and METHOD from it:
                    form: fm,
                    url : "/DotAjaxDirector/com.dotmarketing.portlets.osgi.AJAX.OSGIAJAX?cmd=modifyExtraPackages",
                    method : "post",
                    // The used data format:
                    handleAs: "json",

                    // Callback on successful call:
                    load: function(response, ioArgs) {
                        // return the response for succeeding callbacks
                        //setTimeout(function() {mainAdmin.refresh();},7000);
                        return response;
                    }
                });
            });
            dijit.byId('packagesOSGIDialog').hide();
            dijit.byId('savingOSGIDialog').show();
            setTimeout(function() {bundles.reboot(false);dijit.byId('savingOSGIDialog').hide();},4000);
        }
    },

    remotePublishBundle : function(jarFile){
        pushHandler.showDialog(jarFile);
    },

    addToBundlePlugin : function (jarFile) {
        pushHandler.showAddToBundleDialog(jarFile, '<%=LanguageUtil.get(pageContext, "Add-To-Bundle")%>');
    }

});

dojo.require("dotcms.dojo.push.PushHandler");
var pushHandler = new dotcms.dojo.push.PushHandler('<%=LanguageUtil.get(pageContext, "Remote-Publish")%>');

var mainAdmin = new dotcms.dijit.osgi.MainAdmin({});
var bundles = new dotcms.dijit.osgi.Bundles({});


var availBundles = new dojo.data.ItemFileReadStore({data:
	<%@ include file="/html/portlet/ext/osgi/available_bundles_json.jsp" %>
});


dojo.ready(function() {
	var myHash = decodeURIComponent(dojo.hash());
	if(myHash && myHash.length > 0){
		bundles.show(myHash);
	}else{
		bundles.show();
	}
    if(dojox.embed.Flash.available){
      dojo.require("dojox.form.uploader.plugins.Flash");
    }else{
      dojo.require("dojox.form.uploader.plugins.IFrame");
    }
});
