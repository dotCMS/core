<%@ include file="/html/portlet/ext/cmsconfig/init.jsp" %>
<% request.setAttribute("requiredPortletAccess", "9"); %>
<%@ include file="/html/common/uservalidation.jsp"%>

<%@ page import="com.dotcms.publisher.endpoint.bean.PublishingEndPoint"%>
<%@ page import="java.util.List"%>
<%@ page import="com.dotcms.publisher.endpoint.business.PublishingEndPointAPI"%>
<%@ page import="com.dotmarketing.business.APILocator"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@ page import="com.dotcms.publisher.environment.business.EnvironmentAPI"%>
<%@ page import="com.dotcms.publisher.environment.bean.Environment"%>
<%@ page import="com.dotcms.publisher.endpoint.bean.PublishingEndPoint"%>
<%@ page import="com.dotcms.enterprise.LicenseUtil" %>

<%	if( LicenseUtil.getLevel()<300){ %>
<%@ include file="/html/portlet/ext/cmsconfig/publishing/not_licensed.jsp" %>
<%return;} %>

<%
    PublishingEndPointAPI pepAPI = APILocator.getPublisherEndPointAPI();
    EnvironmentAPI eAPI = APILocator.getEnvironmentAPI();

    List<Environment> environments = eAPI.findAllEnvironments();
    List<PublishingEndPoint> endpoints = pepAPI.getAllEndPoints();
%>

<script type="text/javascript">

var selectedEndpointId;

function goToAddEnvironment() {
    var dialog = new dijit.Dialog({
        id: 'addEnvironment',
        title: "<%= LanguageUtil.get(pageContext, "publisher_Environment_Add")%>",
        style: "width: 700px; ",
        content: new dojox.layout.ContentPane({
            href: "/html/portlet/ext/contentlet/publishing/add_publish_environment.jsp"
        }),
        onHide: function () {
            var dialog = this;
            setTimeout(function () {
                dialog.destroyRecursive();
            }, 200);
        },
        onLoad: function () {

        }
    });
    dialog.show();
    dojo.style(dialog.domNode, 'top', '80px');
}

function goToEditEnvironment(identifier){
    var y = Math.floor(Math.random()*1123213213);
    var dialog = new dijit.Dialog({
        id: 'addEnvironment',
        title: "<%= LanguageUtil.get(pageContext, "publisher_Edit_Environment_Title")%>",
        style: "width: 700px; height: 280px;",
        content: new dojox.layout.ContentPane({
            href: "/html/portlet/ext/contentlet/publishing/add_publish_environment.jsp?op=edit&id="+identifier+"&random="+y
        }),
        onHide: function() {
            var dialog=this;
            setTimeout(function() {
                dialog.destroyRecursive();
            },200);
        },
        onLoad: function() {
        }
    });
    dialog.show();
    dojo.style(dialog.domNode,'top','80px');
}

function goToEditEndpoint(identifier, envId, isSender){
    var dialog = new dijit.Dialog({
        id: 'addEndpoint',
        title: "<%= LanguageUtil.get(pageContext, "publisher_Endpoint_Edit")%>",
        style: "width: 800px; ",
        content: new dojox.layout.ContentPane({
            href: "/html/portlet/ext/contentlet/publishing/add_publish_endpoint.jsp?op=edit&id="+identifier+"&environmentId="+envId+"&isSender="+isSender
        }),
        onHide: function() {
            var dialog=this;
            setTimeout(function() {
                dialog.destroyRecursive();
            },200);
        },
        onLoad: function() {
        }
    });
    dialog.show();
    dojo.style(dialog.domNode,'top','80px');
}

function deleteEndpoint(identifier, fromEnvironment) {

    if (confirm("Are you sure you want to delete this endpoint?")) {

        var xhrArgs = {
            url: "/api/config/deleteEndpoint",
            content: {
                'endPoint': identifier
            },
            handleAs: "json",
            load: function (data) {

                var isError = false;
                if (data.success == false || data.success == "false") {
                    isError = true;
                }

                loadRemotePublishingTab();
                showDotCMSSystemMessage(data.message, isError);
            },
            error: function (error) {
                showDotCMSSystemMessage(error.responseText, true);
            }
        };
        dojo.xhrPost(xhrArgs);
    }

}

dojo.addOnLoad(function () {

    require(["dojo/query"], function (query) {

        /*
         Lets check processes status for all the end point servers
         */
        var loadingObjects = query("div[id*='group-']");
        loadingObjects.forEach(function (node, index, nodelist) {

            var nodeId = node.id;
            var startIndex = "group-".length;
            var endPointId = nodeId.substring(startIndex, nodeId.length);

            if (dijit.byId('checkIntegrityButton' + endPointId) == undefined) {
                setTimeout(function () {
                    //Lets check process status for this end point server
                    displayLoadingOnly(endPointId);
                    checkIntegrityProcessStatus(endPointId);
                }, 1000);
            } else {
                //Lets check process status for this end point server
                displayLoadingOnly(endPointId);
                checkIntegrityProcessStatus(endPointId);
            }
        });
    });

});

/**
 * Initializes the check integrity process against a given server
 * @param identifier
 */
function checkIntegrity(identifier) {

    var buttonId = 'checkIntegrityButton' + identifier;
    var resultsButtonId = 'getIntegrityResultsButton' + identifier;
    var loadingId = 'loadingContent' + identifier;
    var cancelCheckIntegrityButtonId = 'cancelCheckIntegrityButton' + identifier;

    require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
        domStyle.set(registry.byId(buttonId).domNode, 'display', 'none');
    });
    require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
        domStyle.set(registry.byId(resultsButtonId).domNode, 'display', 'none');
    });
    require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
        domStyle.set(registry.byId(cancelCheckIntegrityButtonId).domNode, 'display', '');
    });
    dojo.byId(loadingId).show();

    var xhrArgs = {
        url: "/api/integrity/checkintegrity/endpoint/" + identifier,
        handleAs: "json",
        preventCache: true,
        load: function (data) {

            var isError = false;
            if (data.success == false || data.success == "false") {
                isError = true;
            }

            if (isError) {
                showDotCMSSystemMessage(data.message, isError);

                require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
                    domStyle.set(registry.byId(buttonId).domNode, 'display', '');
                });
                require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
                    domStyle.set(registry.byId(resultsButtonId).domNode, 'display', 'none');
                });
                require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
                    domStyle.set(registry.byId(cancelCheckIntegrityButtonId).domNode, 'display', 'none');
                });
                dojo.byId(loadingId).hide();
                return;
            }

            showDotCMSSystemMessage(data.message, false);
            //Lets start checking every 30 seconds the status of this integrity check
            displayLoadingOnly(identifier);
            checkIntegrityProcessStatus(identifier);
        },
        error: function (error) {
            showDotCMSSystemMessage(error.responseText, true);

            require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
                domStyle.set(registry.byId(buttonId).domNode, 'display', '');
            });
            require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
                domStyle.set(registry.byId(resultsButtonId).domNode, 'display', 'none');
            });
            require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
                domStyle.set(registry.byId(cancelCheckIntegrityButtonId).domNode, 'display', 'none');
            });
            dojo.byId(loadingId).hide();
        }
    };
    dojo.xhrGet(xhrArgs);
}

/**
 * Verifies the status of a check integrity process for a given server
 * @param identifier
 */
function cancelIntegrityCheck(identifier, callback) {

    var buttonId = 'checkIntegrityButton' + identifier;
    var resultsButtonId = 'getIntegrityResultsButton' + identifier;
    var loadingId = 'loadingContent' + identifier;
    var cancelCheckIntegrityButtonId = 'cancelCheckIntegrityButton' + identifier;

    require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
        domStyle.set(registry.byId(buttonId).domNode, 'display', 'none');
    });
    require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
        domStyle.set(registry.byId(resultsButtonId).domNode, 'display', 'none');
    });
    require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
        domStyle.set(registry.byId(cancelCheckIntegrityButtonId).domNode, 'display', '');
    });
    dojo.byId(loadingId).hide();

    //Displaying the loading dialog
    dijit.byId('processingDialog').show();

    var xhrArgs = {
        url: "/api/integrity/cancelIntegrityProcess/endPoint/" + identifier,
        handleAs: "json",
        preventCache: true,
        load: function (data) {

            var isError = false;
            if (data.success == false || data.success == "false") {
                isError = true;
            }

            if (isError) {
                showDotCMSSystemMessage(data.message, isError);
            }
            //Lets start checking every 30 seconds the status of this integrity check
            checkIntegrityProcessStatus(identifier, function (status) {
                if (status != "processing") {
                    //Hiding the loading dialog
                    dijit.byId('processingDialog').hide();
                }
            });
        },
        error: function (error) {
            showDotCMSSystemMessage(error.responseText, true);

            //Hiding the loading dialog
            dijit.byId('processingDialog').hide();
        }
    };
    dojo.xhrGet(xhrArgs);
}

var displayLoadingOnly = function (identifier) {

    var buttonId = 'checkIntegrityButton' + identifier;
    var resultsButtonId = 'getIntegrityResultsButton' + identifier;
    var loadingId = 'loadingContent' + identifier;
    var cancelCheckIntegrityButtonId = 'cancelCheckIntegrityButton' + identifier;

    require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
        domStyle.set(registry.byId(buttonId).domNode, 'display', 'none');
    });
    require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
        domStyle.set(registry.byId(resultsButtonId).domNode, 'display', 'none');
    });
    require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
        domStyle.set(registry.byId(cancelCheckIntegrityButtonId).domNode, 'display', 'none');
    });
    dojo.byId(loadingId).show();
};

/**
 * Verifies the status of a check integrity process for a given server
 * @param identifier
 */
function checkIntegrityProcessStatus(identifier, callback) {

    var buttonId = 'checkIntegrityButton' + identifier;
    var resultsButtonId = 'getIntegrityResultsButton' + identifier;
    var loadingId = 'loadingContent' + identifier;
    var cancelCheckIntegrityButtonId = 'cancelCheckIntegrityButton' + identifier;

    var xhrArgs = {
        url: "/api/integrity/checkIntegrityProcessStatus/endPoint/" + identifier,
        handleAs: "json",
        preventCache: true,
        load: function (data) {

            var isError = false;
            if (data.success == false || data.success == "false") {
                isError = true;
            }

            if (isError) {
                showDotCMSSystemMessage(data.message, isError);

                require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
                    domStyle.set(registry.byId(buttonId).domNode, 'display', '');
                });
                require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
                    domStyle.set(registry.byId(resultsButtonId).domNode, 'display', 'none');
                });
                require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
                    domStyle.set(registry.byId(cancelCheckIntegrityButtonId).domNode, 'display', 'none');
                });
                dojo.byId(loadingId).hide();
                return;
            }

            var status = data.status;
            if (status == "processing") {//Still processing, check again in 30 seconds

                require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
                    domStyle.set(registry.byId(cancelCheckIntegrityButtonId).domNode, 'display', '');
                });

                //Verify again in 30 seconds
                setTimeout(function () {
                    checkIntegrityProcessStatus(identifier, callback)
                }, 10000);
                //Verify again in 30 seconds

            } else if (status == "finished") {//Process finished so show the show results button
                require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
                    domStyle.set(registry.byId(buttonId).domNode, 'display', 'none');
                });
                require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
                    domStyle.set(registry.byId(resultsButtonId).domNode, 'display', '');
                });
                require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
                    domStyle.set(registry.byId(cancelCheckIntegrityButtonId).domNode, 'display', 'none');
                });
                dojo.byId(loadingId).hide();
            } else if (status == "noConflicts") {//We found no conflicts
                showDotCMSSystemMessage(data.message, true);

                require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
                    domStyle.set(registry.byId(buttonId).domNode, 'display', '');
                });
                require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
                    domStyle.set(registry.byId(resultsButtonId).domNode, 'display', 'none');
                });
                require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
                    domStyle.set(registry.byId(cancelCheckIntegrityButtonId).domNode, 'display', 'none');
                });
                dojo.byId(loadingId).hide();
            } else if (status == "error") {//Some error happened,display the error a buttons to normal
                showDotCMSSystemMessage(data.message, true);

                require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
                    domStyle.set(registry.byId(buttonId).domNode, 'display', '');
                });
                require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
                    domStyle.set(registry.byId(resultsButtonId).domNode, 'display', 'none');
                });
                require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
                    domStyle.set(registry.byId(cancelCheckIntegrityButtonId).domNode, 'display', 'none');
                });
                dojo.byId(loadingId).hide();
            } else if (status == "canceled") {//The process was cancelled
                showDotCMSSystemMessage(data.message, true);

                require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
                    domStyle.set(registry.byId(buttonId).domNode, 'display', '');
                });
                require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
                    domStyle.set(registry.byId(resultsButtonId).domNode, 'display', 'none');
                });
                require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
                    domStyle.set(registry.byId(cancelCheckIntegrityButtonId).domNode, 'display', 'none');
                });
                dojo.byId(loadingId).hide();
            } else {//No process at all, just make sure buttons are in their initial status
                require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
                    domStyle.set(registry.byId(buttonId).domNode, 'display', '');
                });
                require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
                    domStyle.set(registry.byId(resultsButtonId).domNode, 'display', 'none');
                });
                require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
                    domStyle.set(registry.byId(cancelCheckIntegrityButtonId).domNode, 'display', 'none');
                });
                dojo.byId(loadingId).hide();
            }

            if (callback && typeof(callback) === "function") {
                callback(status);
            }
        },
        error: function (error) {

            if (callback && typeof(callback) === "function") {
                callback();
            }

            showDotCMSSystemMessage(error.responseText, true);

            require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
                domStyle.set(registry.byId(buttonId).domNode, 'display', '');
            });
            require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
                domStyle.set(registry.byId(resultsButtonId).domNode, 'display', 'none');
            });
            dojo.byId(loadingId).hide();
        }
    };
    dojo.xhrGet(xhrArgs);
}

/**
 * Searches and display the integrity check results for a given server
 * @param identifier
 */
function getIntegrityResult(identifier) {

    var buttonId = 'checkIntegrityButton' + identifier;
    var resultsButtonId = 'getIntegrityResultsButton' + identifier;
    var loadingId = 'loadingContent' + identifier;
    var cancelCheckIntegrityButtonId = 'cancelCheckIntegrityButton' + identifier;

    require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
        domStyle.set(registry.byId(buttonId).domNode, 'display', 'none');
    });
    require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
        domStyle.set(registry.byId(resultsButtonId).domNode, 'display', 'none');
    });
    require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
        domStyle.set(registry.byId(cancelCheckIntegrityButtonId).domNode, 'display', 'none');
    });
    dojo.byId(loadingId).show();

    var xhrArgs = {
        url: "/api/integrity/getIntegrityResult/endPoint/" + identifier,
        handleAs: "json",
        preventCache: true,
        load: function (data) {

            var isError = false;
            if (data.success == false || data.success == "false") {
                isError = true;
            }

            if (isError) {
                showDotCMSSystemMessage(data.message, isError);
                return;
            }

            //Getting the structures data
            var structuresData = data.structures;
            populateTabContent(structuresData, "structures");

            //Getting the folders data
            var foldersData = data.folders;
            populateTabContent(foldersData, "folders");

            //Getting the schemes data
            var workflowsData = data.schemes;
            populateTabContent(workflowsData, "schemes");
            
          	//Getting the htmlpages data
            var htmlPagesData = data.htmlpages;
            populateTabContent(htmlPagesData, "htmlPages");
            
          	// Getting the fileassets data
            var fileAssetsData = data.fileassets;
            populateTabContent(fileAssetsData, "fileAssets");

            //Display the integrity results dialog
            selectedEndpointId = identifier;
            dijit.byId('integrityResultsDialog').show();

            require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
                domStyle.set(registry.byId(buttonId).domNode, 'display', '');
            });
            require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
                domStyle.set(registry.byId(resultsButtonId).domNode, 'display', 'none');
            });
            dojo.byId(loadingId).hide();
        },
        error: function (error) {
            showDotCMSSystemMessage(error.responseText, true);

            require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
                domStyle.set(registry.byId(buttonId).domNode, 'display', '');
            });
            require([ 'dojo/dom-style', 'dijit/registry' ], function (domStyle, registry) {
                domStyle.set(registry.byId(resultsButtonId).domNode, 'display', 'none');
            });
            dojo.byId(loadingId).hide();
        }
    };
    dojo.xhrGet(xhrArgs);
}

function populateTabContent(contentData, id) {

    var tabContentDiv = id + "TabContentDiv";
    var fixButtonName = id + "FixButton";
    var discardButtonName = id + "DiscardButton";

    require(["dojo"], function(dojo){
        // Empty node's children byId:
        dojo.empty(tabContentDiv);
    });

    var htmlContent = "";
    contentData.forEach(function (checkedData) {

        var title = checkedData.title;
        var columns = checkedData.columns;
        var values = checkedData.values;

        htmlContent += '<div class="yui-g portlet-toolbar"><div class="yui-u first">' +
                '<span  style="line-height:20px;font-weight: bold;">' + title + '</span>' +
                '</div><div class="yui-u" style="text-align:right;">'
                + '<input type="radio" dojoType="dijit.form.RadioButton" checked="true" value="local"  name="whereToFixRadio_'+id+'" id="fixLocal_'+id+'" ><label for="fixLocal_'+id+'">&nbsp;<%= LanguageUtil.get(pageContext, "push_publish_integrity_fix_local") %></label>&nbsp;'
                + '<input type="radio" dojoType="dijit.form.RadioButton" value="remote" name="whereToFixRadio_'+id+'" id="fixRemote_'+id+'" ><label for="fixRemote_'+id+'">&nbsp;<%= LanguageUtil.get(pageContext, "push_publish_integrity_fix_remote") %></label>&nbsp;'
                + '</div></div>';

        htmlContent += '<div style="height:250px; overflow:auto"><table class="listingTable"><tr>';
        columns.forEach(function (column) {
            htmlContent += '<th nowrap="nowrap">' + column + "</th>";
        });
        htmlContent += '</tr>';

        var altRow = false;

        if(values.length==0) {
            dijit.byId(fixButtonName).setAttribute('disabled', true);
            dijit.byId(discardButtonName).setAttribute('disabled', true);
        } else {
        	dijit.byId(fixButtonName).setAttribute('disabled', false);
            dijit.byId(discardButtonName).setAttribute('disabled', false);
        }

        values.forEach(function (value) {
            if (altRow) {
                htmlContent += '<tr style="background:#f3f3f3;" class="' + id + '_row">';
            } else {
                htmlContent += '<tr class="' + id + '_row">';
            }
            altRow=!altRow;
            columns.forEach(function (column) {
                htmlContent += '<td valign="top" nowrap="nowrap" style="font-size:10px">' + value[column] + "</td>";
            });
            htmlContent += '</tr>';
        });

        htmlContent += '</table>';
    });

    dojo.place(htmlContent, tabContentDiv, "only");
}

function closeIntegrityResultsDialog(identifier) {
    //Verify the status of the current process for this endpoint
    checkIntegrityProcessStatus(identifier, function () {
        //Close the dialog
        dijit.byId('integrityResultsDialog').hide();
    });
}

function fixConflicts(identifier, type) {

    //Displaying the loading dialog
    dijit.byId('fixingDialog').show();

    var localFix = dojo.byId("fixLocal_" + type).checked;
    var whereToFix = localFix?"local":"remote";
    var fixButtonName = type + "FixButton";
    var discardButtonName = type + "DiscardButton";

    var xhrArgs = {
        url: "/api/integrity/fixconflicts/endPoint/" + identifier + "/type/" + type + "/whereToFix/" + whereToFix,
        handleAs: "json",
        preventCache: true,
        load: function (data) {

            //Hiding the loading dialog
            dijit.byId('fixingDialog').hide();

            var isError = false;
            if (data.success == false || data.success == "false") {
                isError = true;
            }

            if (isError) {
                showDotCMSSystemMessage(data.message, isError);
                return;
            }

            var message = localFix? "<%= LanguageUtil.get(pageContext, "push_publish_integrity_conflicts_fixed_local")%>"
                    : "<%= LanguageUtil.get(pageContext, "push_publish_integrity_conflicts_fixed_remote")%>";

            showDotCMSSystemMessage(message, true);

            dijit.byId(fixButtonName).setAttribute('disabled', true);
            dijit.byId(discardButtonName).setAttribute('disabled', true);

            //Cleaning up the html tables
            dojo.query("." + type + "_row").forEach(dojo.destroy);
        },
        error: function (error) {
            showDotCMSSystemMessage(error.responseText, true);

            //Hiding the loading dialog
            dijit.byId('fixingDialog').hide();
        }
    };
    dojo.xhrGet(xhrArgs);
}

function discardConflicts(identifier, type) {

	var fixButtonName = type + "FixButton";
    var discardButtonName = type + "DiscardButton";

    var xhrArgs = {
        url: "/api/integrity/discardconflicts/endPoint/" + identifier + "/type/" + type,
        handleAs: "json",
        preventCache: true,
        load: function (data) {

            var isError = false;
            if (data.success == false || data.success == "false") {
                isError = true;
            }

            if (isError) {
                showDotCMSSystemMessage(data.message, isError);
                return;
            }

            closeIntegrityResultsDialog(identifier);
            showDotCMSSystemMessage("<%= LanguageUtil.get(pageContext, "push_publish_integrity_conflicts_discarded")%>", true);

        },
        error: function (error) {
            showDotCMSSystemMessage(error.responseText, true);
        }
    };
    dojo.xhrGet(xhrArgs);
}

function deleteEnvironment(identifier) {

    if (confirm("<%= LanguageUtil.get(pageContext, "publisher_Delete_Environment_Confirm")%>")) {

        var xhrArgs = {
            url: "/api/config/deleteEnvironment",
            content: {
                'environment': identifier
            },
            handleAs: "json",
            load: function (data) {

                var isError = false;
                if (data.success == false || data.success == "false") {
                    isError = true;
                }

                loadRemotePublishingTab();
                showDotCMSSystemMessage(data.message, isError);
            },
            error: function (error) {
                showDotCMSSystemMessage(error.responseText, true);
            }
        };
        dojo.xhrPost(xhrArgs);
    }

}

function goToAddEndpoint(environmentId, isSender) {
    var dialog = new dijit.Dialog({
        id: 'addEndpoint',
        title: "<%= LanguageUtil.get(pageContext, "publisher_Endpoint_Add")%>",
        style: "width: 800px; ",
        content: new dojox.layout.ContentPane({
            href: "/html/portlet/ext/contentlet/publishing/add_publish_endpoint.jsp?environmentId=" + environmentId + "&isSender=" + isSender
        }),
        onHide: function () {
            var dialog = this;
            setTimeout(function () {
                dialog.destroyRecursive();
            }, 200);
        },
        onLoad: function () {

        }
    });
    dialog.show();
    dojo.style(dialog.domNode, 'top', '80px');
}

function goToEditEndpoint(identifier, envId, isSender){
    var dialog = new dijit.Dialog({
        id: 'addEndpoint',
        title: "<%= LanguageUtil.get(pageContext, "publisher_Endpoint_Edit")%>",
        style: "width: 800px; ",
        content: new dojox.layout.ContentPane({
            href: "/html/portlet/ext/contentlet/publishing/add_publish_endpoint.jsp?op=edit&id="+identifier+"&environmentId="+envId+"&isSender="+isSender
        }),
        onHide: function() {
            var dialog=this;
            setTimeout(function() {
                dialog.destroyRecursive();
            },200);
        },
        onLoad: function() {
        }
    });
    dialog.show();
    dojo.style(dialog.domNode,'top','80px');
}


function refreshWhoCanUse() {
    dojo.empty("whoCanUseTbl");
    var table = dojo.byId("whoCanUseTbl");
    var x = "";

    this.whoCanUse = this.whoCanUse.sort(function (a, b) {
        var x = a.name.toLowerCase();
        var y = b.name.toLowerCase();
        return ((x < y) ? -1 : ((x > y) ? 1 : 0));
    });
    for (i = 0; i < this.whoCanUse.length; i++) {
        var what = (this.whoCanUse[i].id.indexOf("user") > -1) ? " (<%=LanguageUtil.get(pageContext, "User")%>)" : "";
        x = x + this.whoCanUse[i].id + ",";
        var tr = dojo.create("tr", null, table);
        dojo.create("td", { width: 10, innerHTML: "<span class='deleteIcon'></span>", className: "wfXBox", onClick: "removeFromWhoCanUse('" + this.whoCanUse[i].id + "');refreshWhoCanUse()" }, tr);
        dojo.create("td", { innerHTML: this.whoCanUse[i].name + what}, tr);

    }
    dojo.byId('whoCanUse').value = x;
}

function addSelectedToWhoCanUse() {

    var select = dijit.byId("whoCanUseSelect");

    var user = select.getValue();
    var userName = select.attr('displayedValue');

    if(user=='0') return;

    addToWhoCanUse(user, userName);
    refreshWhoCanUse();

    select.set('value', '0');

}

function addToWhoCanUse(myId, myName) {

    for (i = 0; i < this.whoCanUse.length; i++) {
        if (myId == this.whoCanUse[i].id || myId == "user-" + this.whoCanUse[i].id || myId == "role-" + this.whoCanUse[i].id) {
            return;
        }
    }

    var entry = {name: myName, id: myId };
    this.whoCanUse[this.whoCanUse.length] = entry;
}

function backToEnvironmentList(addedEndPoint) {

    if (!addedEndPoint) {
        dijit.byId("addEnvironment").hide();
    } else {
        dijit.byId("addEndpoint").hide();
    }
    loadRemotePublishingTab();
}

function backToEndpointsList(){

    dijit.byId("addEndpoint").hide();
    loadRemotePublishingTab();
}

var whoCanUse = new Array()

function removeFromWhoCanUse(myId) {

    var x = 0;
    var newCanUse = new Array();
    for (i = 0; i < this.whoCanUse.length; i++) {
        if (myId != this.whoCanUse[i].id) {
            newCanUse[x] = this.whoCanUse[i];
            x++;
        }
    }
    this.whoCanUse = newCanUse;

    var select = dijit.byId("whoCanUseSelect");
    select.set('value', '0');
}

function deleteEnvPushHistory(envId) {

    var xhrArgs = {
        url : '/api/bundle/deleteenvironmentpushhistory/environmentid/'+envId,
        handleAs : "json",
        sync: false,
        load : function(data) {
            alert('<%= LanguageUtil.get(pageContext, "publisher_Environments_deleted_assets-history") %>');
        },
        error : function(error) {
            targetNode.innerHTML = "An unexpected error occurred: " + error;
        }
    };

    var deferred = dojo.xhrGet(xhrArgs);
}

</script>

<%--++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++--%>
<%--START OF ENVIROMENTS--%>
<%--++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++--%>
<div class="yui-g portlet-toolbar">

    <div class="yui-u first">
        <span class="sServerIcon"></span>
        <span  style="line-height:20px;font-weight: bold;"><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Sending_Server_Short") %></span>
    </div>

    <div class="yui-u" style="text-align:right;">
        <button dojoType="dijit.form.Button" onClick="goToAddEnvironment();" iconClass="plusIcon">
            <%= LanguageUtil.get(pageContext, "publisher_Add_Environment") %>
        </button>
    </div>
</div>

<div style="padding-top: 5px">
    <table  class="listingTable">
        <tr>
            <th>
            </th>
            <th nowrap="nowrap">
                <%= LanguageUtil.get(pageContext, "publisher_Environment_Name") %>
            </th>
            <th nowrap="nowrap" width="100%" >
                <%= LanguageUtil.get(pageContext, "Servers") %>
            </th>
            <th nowrap="nowrap">
                <%= LanguageUtil.get(pageContext, "publisher_Environment_Push_Mode") %>
            </th>
            <th nowrap="nowrap">
                <%= LanguageUtil.get(pageContext, "Actions") %>
            </th>
        </tr>

        <%
            boolean altRow = false;
            boolean hasEnvironments = false;
            for(Environment environment : environments){
                hasEnvironments=true;%>

        <tr style="<%=(altRow) ? "background:#f3f3f3" :""%>">
            <%altRow=!altRow; %>
            <td nowrap="nowrap" valign="top">
                <a style="cursor: pointer" onclick="deleteEnvironment('<%=environment.getId()%>')" title="<%= LanguageUtil.get(pageContext, "publisher_Delete_Environment") %>">
                    <span class="deleteIcon"></span></a>&nbsp;
                <a style="cursor: pointer" onclick="goToEditEnvironment('<%=environment.getId()%>')" title="<%= LanguageUtil.get(pageContext, "publisher_Edit_Environment_Title") %>">
                    <span class="editIcon"></span></a>
            </td>
            <td valign="top" nowrap="nowrap" style="cursor: pointer" onclick="goToEditEnvironment('<%=environment.getId()%>')">
                <b><%=environment.getName()%></b>
            </td>
            <td style="padding:0px;" valign="top">

                <%
                    List<PublishingEndPoint> environmentEndPoints = pepAPI.findSendingEndPointsByEnvironment(environment.getId());
                    boolean hasRow = false;
                    int i = 0;
                    for(PublishingEndPoint endpoint : environmentEndPoints){
                        if(endpoint.isSending()){
                            continue;
                        }
                        hasRow=true;%>
                <div style="padding:10px;border-bottom:1px solid silver;margin-bottom:-1px">
                    <div class="buttonsGroup">

                        <%if(environment.getPushToAll() || i == 0){%>
                        <div class="integrityCheckActionsGroup" style="float:right; display:inline-flex;" id="group-<%=endpoint.getId()%>">
                            <button dojoType="dijit.form.Button" onClick="checkIntegrity('<%=endpoint.getId()%>');" id="checkIntegrityButton<%=endpoint.getId()%>" iconClass="dropIcon" style="display: none;">
                                <%= LanguageUtil.get( pageContext, "CheckIntegrity" ) %>
                            </button>
                            <button dojoType="dijit.form.Button" onClick="getIntegrityResult('<%=endpoint.getId()%>');" id="getIntegrityResultsButton<%=endpoint.getId()%>" iconClass="exclamation" style="display: none;">
                                <%= LanguageUtil.get( pageContext, "Preview-Analysis-Results" ) %>
                            </button>
                            <div id="loadingContent<%=endpoint.getId()%>" class="loadingIntegrityCheck" align="center" style="display: none;">
                                <font class="bg" size="2"> <b><%= LanguageUtil.get( pageContext, "Loading" ) %></b> <br />
                                    <img src="/html/images/icons/processing.gif" /></font>
                            </div>
                            <button dojoType="dijit.form.Button" onClick="cancelIntegrityCheck('<%=endpoint.getId()%>');" id="cancelCheckIntegrityButton<%=endpoint.getId()%>" iconClass="stopIcon" style="padding-left:10px; padding-top:5px; display:none;">
                                <%= LanguageUtil.get( pageContext, "cancel" ) %>
                            </button>
                        </div>
                        <%} %>

                    </div>

                    <div style="padding:10px; float: left;">
                        <div class="buttonsGroup">
                            <a style="cursor: pointer; float:right" onclick="deleteEndpoint('<%=endpoint.getId()%>', true)" title="<%= LanguageUtil.get(pageContext, "publisher_Delete_Endpoint_Title") %>">
                                <span class="deleteIcon"></span>
                            </a>
                        </div>
                    </div>

                    <div <%=(!endpoint.isEnabled()?" style='color:silver;'":"")%> style="cursor:pointer" onclick="goToEditEndpoint('<%=endpoint.getId()%>', '<%=environment.getId()%>', 'false')">

                        <div >
                            <%=(endpoint.isEnabled()?"<span class='liveIcon'></span>":"<span class='greyDotIcon' style='opacity:.4'></span>")%><%=endpoint.getServerName()%>
                        </div>
                        <div>
                            <%=("https".equals(endpoint.getProtocol())) ? "<span class='encryptIcon'></span>": "<span class='shimIcon'></span>" %>
                            <i style="color:#888;"><%=endpoint.getProtocol()%>://<%=endpoint.getAddress()%>:<%=endpoint.getPort()%></i>
                        </div>
                    </div>
                </div>
                <%
                        i++;
                    }%>

                <%if(!hasRow){ %>
                <div  style="padding:5px;">
                    <%= LanguageUtil.get(pageContext, "publisher_No_Servers") %> <a style="text-decoration: underline;" href="javascript:goToAddEndpoint('<%=environment.getId()%>', 'false');"><%= LanguageUtil.get(pageContext, "publisher_add_one_now") %></a>
                </div>
                <%}%>

            </td>
            <td align="center" valign="top" nowrap="nowrap">

                <%if(environment.getPushToAll()){%>
                <%= LanguageUtil.get(pageContext, "publisher_Environments_Push_To_All") %>
                <%}else{ %>
                <%= LanguageUtil.get(pageContext, "publisher_Environments_Push_To_One") %>
                <%} %>
            </td>
            <td valign="top" nowrap="nowrap">
                <button dojoType="dijit.form.Button" onClick="goToAddEndpoint('<%=environment.getId()%>', 'false');" iconClass="plusIcon">
                    <%= LanguageUtil.get(pageContext, "publisher_Add_Endpoint") %>
                </button>
                <button dojoType="dijit.form.Button" onClick="deleteEnvPushHistory('<%=environment.getId()%>');" iconClass="deleteIcon" >
                    <%= LanguageUtil.get(pageContext, "publisher_delete_asset_history") %>
                </button>
            </td>

        </tr>

        <%}%>

        <%if(!hasEnvironments){ %>
        <tr>
            <td colspan="100" align="center">
                <%= LanguageUtil.get(pageContext, "publisher_no_environments") %><a href="javascript:goToAddEnvironment();"> <%= LanguageUtil.get(pageContext, "publisher_add_one_now") %></a>
            </td>
        </tr>
        <%}%>

    </table><br>

</div>
<%--++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++--%>
<%--END OF ENVIROMENTS--%>
<%--++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++--%>

<hr style="margin-top: 30px;margin-bottom: 30px;">

<%--++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++--%>
<%--START OF END POINTS--%>
<%--++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++--%>
<div class="yui-g portlet-toolbar">
    <div class="yui-u first">
        <span class="rServerIcon"></span>
        <span  style="line-height:20px;font-weight: bold;"><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Receiving_Server_Short") %></span>
    </div>
    <div class="yui-u" style="text-align:right;">
        <button dojoType="dijit.form.Button" onClick="goToAddEndpoint(null, 'true');" iconClass="plusIcon">
            <%= LanguageUtil.get(pageContext, "publisher_Add_Endpoint") %>
        </button>
    </div>
</div>

<div style="padding-top: 5px">
    <table class="listingTable">
        <tr>
            <th style="width:40px"></th>
            <th><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Server_Name") %></th>

        </tr>
        <%
            boolean hasRow = false;
            for(PublishingEndPoint endpoint : endpoints){
                if(!endpoint.isSending()){
                    continue;
                }
                hasRow=true;%>
        <tr <%=(!endpoint.isEnabled()?" style='color:silver;'":"")%>>
            <td nowrap="nowrap" valign="top">
                <a style="cursor: pointer" onclick="deleteEndpoint('<%=endpoint.getId()%>')" title="<%= LanguageUtil.get(pageContext, "publisher_Delete_Endpoint_Title") %>">
                    <span class="deleteIcon"></span></a>&nbsp;
                <a style="cursor: pointer" onclick="goToEditEndpoint('<%=endpoint.getId()%>', null, 'true')" title="<%= LanguageUtil.get(pageContext, "publisher_Edit_Endpoint_Title") %>">
                    <span class="editIcon"></span></a>
            </td>

            <td style="cursor: pointer" width="100%" onclick="goToEditEndpoint('<%=endpoint.getId()%>', null, 'true')">
                <b><%=(endpoint.isEnabled()?"<span class='liveIcon'></span>":"<span class='greyDotIcon' style='opacity:.4'></span>")%><%=endpoint.getServerName()%></b>
                <br>
                <i><span class='shimIcon'></span><%=endpoint.getAddress()%></i>
            </td>



        </tr>
        <%}%>

        <%if(!hasRow){ %>

        <tr>
            <td colspan="100" align="center">
                <%= LanguageUtil.get(pageContext, "publisher_no_servers_set_up") %><a href="javascript:goToAddEndpoint(null, 'true');"> <%= LanguageUtil.get(pageContext, "publisher_add_one_now") %></a>
            </td>

        </tr>
        <%}%>
    </table>
</div>
<%--++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++--%>
<%--END OF END POINTS--%>
<%--++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++--%>


<%--++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++--%>
<%--INTEGRITY RESULTS DIALOG--%>
<%--++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++--%>
<style type="text/css">
    #structuresTab,#foldersTab,#schemesTab,#htmlPagesTab,#fileAssetsTab {
        height:100%;
        min-height:250px;
        width:800px;
    }
</style>
<div id="integrityResultsDialog" style="width: 1120px" dojoAttachPoint="dialog" dojoType="dijit.Dialog" onCancel="closeIntegrityResultsDialog(selectedEndpointId)" title="<%= LanguageUtil.get(pageContext, "CheckIntegrity") %>">

    <div id="integrityResultsTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false">

        <div id="structuresTab" style="width: 1100px" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "structures") %>" >
            <div id="structuresTabContentDiv"></div>
            <div class="buttonRow">
                <button dojoType="dijit.form.Button" id="structuresFixButton"
                        onClick="fixConflicts(selectedEndpointId, 'structures')" iconClass="fixIcon"><%=LanguageUtil.get(pageContext,
                        "push_publish_integrity_fix_conflicts")%></button>
                <button dojoType="dijit.form.Button" id="structuresDiscardButton"
                        onClick="discardConflicts(selectedEndpointId, 'structures')" iconClass="deleteIcon"><%=LanguageUtil.get(pageContext,
                        "push_publish_integrity_discard_conflicts")%></button>
                <button dojoType="dijit.form.Button" onClick="closeIntegrityResultsDialog(selectedEndpointId)" iconClass="closeIcon"><%= LanguageUtil.get(pageContext, "close") %></button>
            </div>
        </div>

        <div id="foldersTab" style="width: 1100px" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "folders") %>" >
            <div id="foldersTabContentDiv" style="height:280px">No Results</div>
            <div class="buttonRow">
                <button dojoType="dijit.form.Button" id="foldersFixButton"
                        onClick="fixConflicts(selectedEndpointId, 'folders')" iconClass="fixIcon"><%=LanguageUtil.get(pageContext,
                        "push_publish_integrity_fix_conflicts")%></button>
                <button dojoType="dijit.form.Button" id="foldersDiscardButton"
                        onClick="discardConflicts(selectedEndpointId, 'folders')" iconClass="deleteIcon"><%=LanguageUtil.get(pageContext,
                        "push_publish_integrity_discard_conflicts")%></button>
                <button dojoType="dijit.form.Button" onClick="closeIntegrityResultsDialog(selectedEndpointId)" iconClass="closeIcon"><%= LanguageUtil.get(pageContext, "close") %></button>
            </div>
        </div>

        <div id="schemesTab" style="width: 1100px" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Workflows") %>" >
            <div id="schemesTabContentDiv"></div>
            <div class="buttonRow">
                <button dojoType="dijit.form.Button" id="schemesFixButton"
                        onClick="fixConflicts(selectedEndpointId, 'schemes')" iconClass="fixIcon"><%=LanguageUtil.get(pageContext,
                        "push_publish_integrity_fix_conflicts")%></button>
                <button dojoType="dijit.form.Button" id="schemesDiscardButton"
                        onClick="discardConflicts(selectedEndpointId, 'schemes')" iconClass="deleteIcon"><%=LanguageUtil.get(pageContext,
                        "push_publish_integrity_discard_conflicts")%></button>
                <button dojoType="dijit.form.Button" onClick="closeIntegrityResultsDialog(selectedEndpointId)" iconClass="closeIcon"><%= LanguageUtil.get(pageContext, "close") %></button>
            </div>
        </div>
        
        <div id="htmlPagesTab" style="width: 1100px" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "htmlpages") %>" >
            <div id="htmlPagesTabContentDiv"></div>
            <div class="buttonRow">
                <button dojoType="dijit.form.Button" id="htmlPagesFixButton"
                        onClick="fixConflicts(selectedEndpointId, 'htmlPages')" iconClass="fixIcon"><%=LanguageUtil.get(pageContext,
                        "push_publish_integrity_fix_conflicts")%></button>
                <button dojoType="dijit.form.Button" id="htmlPagesDiscardButton"
                        onClick="discardConflicts(selectedEndpointId, 'htmlPages')" iconClass="deleteIcon"><%=LanguageUtil.get(pageContext,
                        "push_publish_integrity_discard_conflicts")%></button>
                <button dojoType="dijit.form.Button" onClick="closeIntegrityResultsDialog(selectedEndpointId)" iconClass="closeIcon"><%= LanguageUtil.get(pageContext, "close") %></button>
            </div>
        </div>
        
        <!-- Content File Assets Tab -->
        <div id="fileAssetsTab" style="width: 1100px" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "integritychecker.file-assets") %>" >
            <div id="fileAssetsTabContentDiv"></div>
            <div class="buttonRow">
                <button dojoType="dijit.form.Button" id="fileAssetsFixButton"
                        onClick="fixConflicts(selectedEndpointId, 'fileAssets')" iconClass="fixIcon"><%=LanguageUtil.get(pageContext,
                        "push_publish_integrity_fix_conflicts")%></button>
                <button dojoType="dijit.form.Button" id="fileAssetsDiscardButton"
                        onClick="discardConflicts(selectedEndpointId, 'fileAssets')" iconClass="deleteIcon"><%=LanguageUtil.get(pageContext,
                        "push_publish_integrity_discard_conflicts")%></button>
                <button dojoType="dijit.form.Button" onClick="closeIntegrityResultsDialog(selectedEndpointId)" iconClass="closeIcon"><%= LanguageUtil.get(pageContext, "close") %></button>
            </div>
        </div>

    </div>

</div>

<div id="processingDialog" dojoType="dijit.Dialog" disableCloseButton="true" title="<%=LanguageUtil.get(pageContext,"Processing")%>" style="display: none;">
    <div dojoType="dijit.ProgressBar" style="width:200px;text-align:center;" indeterminate="true" jsId="processingLoading" id="processingLoading"></div>
</div>
<div id="fixingDialog" dojoType="dijit.Dialog" disableCloseButton="true" title="<%=LanguageUtil.get(pageContext,"push_publish_integrity_fixing_conflict")%>" style="display: none;">
    <div dojoType="dijit.ProgressBar" style="width:200px;text-align:center;" indeterminate="true" jsId="saveProgress" id="saveProgress"></div>
</div>

<%--++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++--%>
<%--INTEGRITY RESULTS DIALOG--%>
<%--++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++--%>