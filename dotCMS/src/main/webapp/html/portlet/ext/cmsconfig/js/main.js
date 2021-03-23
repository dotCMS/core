dojo.require("dojo.io.iframe");

function emmitCompanyInfoUpdated(content) {
    var customEvent = document.createEvent("CustomEvent");
    customEvent.initCustomEvent("ng-event", false, false,  {
        name: "company-info-updated",
        payload: {
            portalURL: content.portalURL,
            mx: content.mx,
            emailAddress: content.emailAddress,
            homeURL: content.homeURL,
            colors: {
                primary: content.type,
                secondary: content.street,
                background: content.size,
            }
        }
    });
    document.dispatchEvent(customEvent)
}

/**
 * Loads the CMS Company Config tab
 */
var loadCompanyTab = function () {

    var url = "/html/portlet/ext/cmsconfig/company.jsp";
    var content = dijit.byId("companyTabContent");

    if (content) {
        content.destroyRecursive(false);
    }
    content = new dojox.layout.ContentPane({
        id: "companyTabContent",
        preventCache: true
    }).placeAt("companyTabContentDiv");

    content.attr("href", url);
    content.refresh();
};

/**
 * Saves the company basic information
 */
var saveCompanyBasicInfo = function () {

    //Getting the form values
    var companyPortalUrl = dijit.byId("companyPortalUrl").get("value");
    var companyMX = dijit.byId("companyMX").get("value");
    var companyEmailAddress = dijit.byId("companyEmailAddress").get("value");
    var bgColor = dijit.byId("bgColor").get("value");
    var primaryColor = dijit.byId("pColor").get("value");
    var secondaryColor = dijit.byId("sColor").get("value");
    var bgURL = dijit.byId("bgURL").get("value");

    var content = {
        'portalURL': companyPortalUrl,
        'mx': companyMX,
        'emailAddress': companyEmailAddress,
        'size': bgColor,
        'type': primaryColor,
        'street': secondaryColor,
        'homeURL': bgURL
    };

    var xhrArgs = {
        url: "/api/config/saveCompanyBasicInfo",
        content: content,
        handleAs: "json",
        load: function (data) {
            var isError = false;

            if (data.success == false || data.success == "false") {
                isError = true;
            }

            showDotCMSSystemMessage(data.message, isError);
            emmitCompanyInfoUpdated(content);
        },
        error: function (error) {
            showDotCMSSystemMessage(error.responseText, true);
        }
    };
    dojo.xhrPost(xhrArgs);
};

/**
 * Saves the company locale information
 */
var saveCompanyLocaleInfo = function () {

    //Getting the form values
    var companyLanguageId = dijit.byId("companyLanguageId").get("value");
    var companyTimeZoneId = dijit.byId("companyTimeZoneId").get("value");

    var xhrArgs = {
        url: "/api/config/saveCompanyLocaleInfo",
        content: {
            'languageId': companyLanguageId,
            'timeZoneId': companyTimeZoneId
        },
        handleAs: "json",
        load: function (data) {

            var isError = false;
            if (data.success == false || data.success == "false") {
                isError = true;
            }

            showDotCMSSystemMessage(data.message, isError);
        },
        error: function (error) {
            showDotCMSSystemMessage(error.responseText, true);
        }
    };
    dojo.xhrPost(xhrArgs);
};

/**
 * Saves the company authentication type
 */
var saveCompanyAuthTypeInfo = function () {

    //Getting the form values
    var companyAuthType = dijit.byId("companyAuthType").get("value");

    var xhrArgs = {
        url: "/api/config/saveCompanyAuthTypeInfo",
        content: {
            'authType': companyAuthType
        },
        handleAs: "json",
        load: function (data) {

            var isError = false;
            if (data.success == false || data.success == "false") {
                isError = true;
            }

            showDotCMSSystemMessage(data.message, isError);
        },
        error: function (error) {
            showDotCMSSystemMessage(error.responseText, true);
        }
    };
    dojo.xhrPost(xhrArgs);
};

var regenerateKey = function (callback) {
    let xhrArgs = {
        url: "/api/config/regenerateKey",
        handleAs: "json",
        load: function (data) {
            if (data.entity) {
               let key = data.entity;
               callback(key);
            }
        },
        error: function (error) {
            showDotCMSSystemMessage(error.responseText, true);
        }
    };
   dojo.xhrPost(xhrArgs);
};

/**
 * Saves the company logo
 * @returns {boolean}
 */
function uploadCompanyLogo() {

    if (!dojo.byId("logoFile") || dojo.byId("logoFile").value.length == 0) {
        alert("The Logo Field is a required field.");
        return false;
    }

    dojo.io.iframe.send({
        url: "/api/config/saveCompanyLogo",
        form: "companyLogoForm",
        method: "post",
        preventCache: true,
        handleAs: "json",
        load: function (data) {

            var isError = false;
            if (data.success == false || data.success == "false") {
                isError = true;
            } else {
                loadCompanyTab();
            }

            showDotCMSSystemMessage(data.message, isError);
        },
        error: function (error) {
            showDotCMSSystemMessage(error.responseText, true);
        }
    });
}

/**
 * Loads the CMS Remote Publishing Config tab
 */
var loadRemotePublishingTab = function () {

    var url = "/html/portlet/ext/cmsconfig/remotePublishing.jsp";
    var content = dijit.byId("remotePublishingTabContent");

    if (content) {
        content.destroyRecursive(false);
    }
    content = new dojox.layout.ContentPane({
        id: "remotePublishingTabContent",
        preventCache: true
    }).placeAt("remotePublishingTabContentDiv");

    content.attr("href", url);
    content.refresh();
};

var loadNetworkTab = function () {

    var url = "/html/portlet/ext/cmsconfig/new_cluster_config.jsp";
    var content = dijit.byId("networkTabContent");

    if (content) {
        content.destroyRecursive(false);
    }
    content = new dojox.layout.ContentPane({
        id: "networkTabContent",
        preventCache: true
    }).placeAt("networkTabContentDiv");

    content.attr("href", url);
    content.refresh();
};

var loadLicenseTab = function () {
	loadLicenseTabMessage("");
	
};


var loadLicenseTabMessage = function (text) {

    var url = "/html/portlet/ext/cmsconfig/license.jsp";
    if(text.length>0){
    	url+="?message=" + text;
    }
    var content = dijit.byId("licenseTabContent");

    if (content) {
        content.destroyRecursive(false);
    }
    content = new dojox.layout.ContentPane({
        id: "licenseTabContent",
        preventCache: true
    }).placeAt("licenseTabContentDiv");

    content.attr("href", url);
    content.refresh();
};
