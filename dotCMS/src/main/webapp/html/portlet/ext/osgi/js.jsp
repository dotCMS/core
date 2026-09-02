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
dojo.subscribe("/dojo/hashchange", this, function(hash) {
	mainAdmin.refresh();
});

dojo.declare("dotcms.dijit.osgi.MainAdmin", null, {

	baseDiv : "osgiBundles",
	url: "",
	constructor : function() {
	},
	show : function(href) {

		var r = Math.floor(Math.random() * 1000000000);
		if (href.indexOf("?") > -1) {
			href = href + "&r=" + r;
		} else {
			href = href + "?r=" + r;
		}

		href = href + "?donothing";

		this.url = href;
		var myCp = dijit.byId("osgiMainBundles");

        if (myCp) {
            myCp.destroyRecursive(false);
		}

        myCp = new dojox.layout.ContentPane({
            id: "osgiMainBundles",
            preventCache: true
        }).placeAt("osgiMain");

        myCp.attr("href", this.url);
	},

	refresh : function() {
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

        myCp.attr("href", this.url);
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

	undeploy : function (jarName, bundleId){
		var xhrArgs = {
			url: "/api/v1/osgi/jar/" + jarName,
            handleAs: "json",
			handle : function(dataOrError, ioArgs) {

			}
		};
		dijit.byId('savingOSGIDialog').show();
		dojo.xhrDelete(xhrArgs);
		setTimeout(function() {mainAdmin.refresh();},7000);
	},

	deploy : function(){
		var availBundles = dijit.byId('availBundlesCombo');
		if(availBundles.getValue() == undefined || availBundles.getValue()==""){
			return;
		}

		var jarName = availBundles.value;
		var xhrArgs = {
            url: "/api/v1/osgi/jar/" + jarName + "/_deploy",
            handleAs: "json",
			handle : function(dataOrError, ioArgs) {

			}
		};
		dijit.byId('savingOSGIDialog').show();
		dojo.xhrPut(xhrArgs);
		setTimeout(function() {mainAdmin.refresh();},7000);
	},

	start : function(jarName, bundleId){
		var xhrArgs = {
            url: "/api/v1/osgi/jar/" + jarName + "/_start",
            handleAs: "json",
			handle : function(dataOrError, ioArgs) {
				// do nothing
			}
		};
		dijit.byId('savingOSGIDialog').show();
		dojo.xhrPut(xhrArgs);
		setTimeout(function() {mainAdmin.refresh();},7000);
	},

	stop : function(jarName, bundleId){
		var xhrArgs = {
            url: "/api/v1/osgi/jar/" + jarName + "/_stop",
            handleAs: "json",
			handle : function(dataOrError, ioArgs) {
				// do nothing
			}
		};
		dijit.byId('savingOSGIDialog').show();
		dojo.xhrPut(xhrArgs);
		setTimeout(function() {mainAdmin.refresh();},7000);
	},

    add: () => {
        const fm = dojo.byId("addBundle");
        const files = fm.elements["bundleUpload"].files;

        if(files.length === 0){
            showDotCMSSystemMessage("Please select a bundle to upload");
            return false;
        }

        bundles.handleUpload({ files: files[0] });
    },

	handleUpload : function({ files, updateProgress, onSuccess, onError }){
        const formData = new FormData();
        const plugins = Array.isArray(files) ? files : [files];
        plugins.forEach((file) => formData.append("file", file));
        formData.append("json", "{}");

        return new Promise((res, rej) => {
            const xhr = new XMLHttpRequest();
            xhr.open("POST", "/api/v1/osgi");
            xhr.onload = () => res(xhr);
            xhr.onerror = rej;
        
            // Get Upload Process
            if (xhr.upload && updateProgress) {
                xhr.upload.onprogress = (e) => {
                    const percentComplete = (e.loaded / e.total) * 100;
                    updateProgress(percentComplete);
                };
            }
        
            xhr.send(formData);
        }).then(async (request) => {
            if (request.status !== 200) {
                throw request;
            }

            if(onSuccess) {
                onSuccess();
            }

            dijit.byId("uploadOSGIDialog").hide();
            // Give bundle time to deploy, then refresh the list via API (not full page reload)
            setTimeout(() => {
                getBundlesData();
            }, 4000);

            return JSON.parse(request.response);
        })
        .catch((request) => {
            const response = typeof (request.response) === 'string' ? JSON.parse(request.response) : request.response;
            const errorMesage = response.errors[0]?.message || "An error occurred while uploading the bundle";

            if(onError) {
                onError(response, errorMesage);
            } else {
                showDotCMSSystemMessage(errorMesage);
            }
            throw response;
        });
	},

    reboot : function(askForConfirmation){

        var canContinue = true;
        if (askForConfirmation) {
            canContinue = confirm('<%=LanguageUtil.get(pageContext, "OSGI-restart-confirmation") %>');
        }

        if(canContinue) {
            var xhrArgs = {
                url: "/api/v1/osgi/_restart",
                handleAs: "json",
                handle : function(dataOrError, ioArgs) {
                    // do nothing
                }
            };
            dijit.byId('savingOSGIDialog').show();
            dojo.xhrPut(xhrArgs);
            setTimeout(function() {mainAdmin.refresh();},7000);
        }
	},

    extraPackages : function (){
        var xhrArgs = {
            url: "/api/v1/osgi/extra-packages",
            handleAs: "json",
            handle : function(dataOrError, ioArgs) {
                let packages = dataOrError.entity;
                dijit.byId('packages').set("value", packages);
            }
        };
        dijit.byId('packagesOSGIDialog').show();
        dojo.xhrGet(xhrArgs);
    },
    
    resetExtraPackages : function(){

        var canContinue = confirm('<%=LanguageUtil.get(pageContext, "OSGI-modify-packages-confirmation") %>');
        if(canContinue) {

            var data = {
                packages: "RESET"
            };
            var xhrArgs = {
                 url: "/api/v1/osgi/extra-packages",
                postData: dojo.toJson(data),
                handleAs: "json",
                headers : {
                    'Accept' : 'application/json',
                    'Content-Type' : 'application/json;charset=utf-8',
                },
                handle : function(dataOrError, ioArgs) {
                    // do nothing
                }
            };
            dojo.xhrPut(xhrArgs);

            dijit.byId('packagesOSGIDialog').hide();
            dijit.byId('savingOSGIDialog').show();
            setTimeout(function() {bundles.reboot(false);dijit.byId('savingOSGIDialog').hide();},4000);
        }
    },
    
    processBundle : function(bundleName){

        if(!confirm('<%=LanguageUtil.get(pageContext, "OSGI-process-bundle-confirmation") %>')){

            return;
        }



        require(["dojo/io/iframe"], function(ioIframe){
 
            ioIframe.send({
                url : "/api/v1/osgi/_processExports/" + bundleName,
                method : "get",
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
        
    },
    
    modifyExtraPackages : function(){

        var canContinue = confirm('<%=LanguageUtil.get(pageContext, "OSGI-modify-packages-confirmation") %>');
        if(canContinue) {

            var packages = dojo.byId("packages");
            var data = {
                packages: packages.value
            };

            var xhrArgs = {
                url: "/api/v1/osgi/extra-packages",
                postData: dojo.toJson(data),
                handleAs: "json",
                headers : {
                    'Accept' : 'application/json',
                    'Content-Type' : 'application/json;charset=utf-8',
                },
                handle : function(dataOrError, ioArgs) {
                    if(dataOrError.message){
                        showDotCMSSystemMessage(dataOrError.responseText, true);
                    }
                }
            };
            dojo.xhrPut(xhrArgs);

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
	bundles.show();
    if(dojox.embed.Flash.available){
      dojo.require("dojox.form.uploader.plugins.Flash");
    }else{
      dojo.require("dojox.form.uploader.plugins.IFrame");
    }
});
