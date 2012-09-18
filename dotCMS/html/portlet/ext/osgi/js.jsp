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
		var myCp = dijit.byId(this.baseDiv);
		var hanger = dojo.byId("osgiMain");
		if(!hanger){
			return;
		}
		if (myCp) {
			myCp.attr("content","");//myCp.destroyRecursive(true);
		}else{
		myCp = new dojox.layout.ContentPane({
			id : this.baseDiv
		}).placeAt("osgiMain");
		}
		myCp.attr("href", hashValue);
		dojo.parser.parse("osgiMain");
	
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
		setTimeout(function() {mainAdmin.refresh();dijit.byId('savingOSGIDialog').hide();},7000);
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
		setTimeout(function() {mainAdmin.refresh();dijit.byId('savingOSGIDialog').hide();},7000);
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
		setTimeout(function() {mainAdmin.refresh();dijit.byId('savingOSGIDialog').hide();},7000);
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
		setTimeout(function() {mainAdmin.refresh();dijit.byId('savingOSGIDialog').hide();},7000);
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
		setTimeout(function() {mainAdmin.refresh();dijit.byId('savingOSGIDialog').hide();},7000);
	},
    addExtraSystemPackages : function(){
		var fm = dojo.byId("addExtraPackages");

        require(["dojo/io/iframe"], function(ioIframe){
            ioIframe.send({
                // The form node, which contains the
                // data. We also pull the URL and METHOD from it:
                form: fm,
                url : "/DotAjaxDirector/com.dotmarketing.portlets.osgi.AJAX.OSGIAJAX?cmd=addExtraSystemPackages",
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
		dijit.byId('extraPacakgesOSGIDialog').hide();
		dijit.byId('savingOSGIDialog').show();
		setTimeout(function() {mainAdmin.refresh();dijit.byId('savingOSGIDialog').hide();},7000);
	}
});

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