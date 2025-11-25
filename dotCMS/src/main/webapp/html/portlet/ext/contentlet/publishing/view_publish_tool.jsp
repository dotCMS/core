<%@ include file="/html/portlet/ext/contentlet/publishing/init.jsp" %>
<%@ include file="/html/portlet/ext/remotepublish/init.jsp" %>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%
    String portletId1 = "publishing-queue";
    Portlet portlet1 = PortletManagerUtil.getPortletById(company.getCompanyId(), portletId1);
    String strutsAction = ParamUtil.get(request, "struts_action", null);

    if (!com.dotmarketing.util.UtilMethods.isSet(strutsAction) || strutsAction.equals(portlet1.getInitParams().get("view-action"))) {
        List<CrumbTrailEntry> crumbTrailEntries = new ArrayList<CrumbTrailEntry>();
        crumbTrailEntries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "com.dotcms.repackage.javax.portlet.title." + portletId1), null));
        request.setAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS, crumbTrailEntries);
    }

    request.setAttribute(com.dotmarketing.util.WebKeys.DONT_DISPLAY_SUBNAV_ALL_HOSTS, false);
%>

<%@ include file="/html/portlet/ext/common/sub_nav_inc.jsp" %>

<style>
    #deleteBundleActions table {
        width:95%;
        border-collapse: separate;
        border-spacing: 10px 15px;
        margin-bottom: 10px;
    }

    #deleteBundleActions .dijitButton {
        width: 110px;
        text-align: center;
    }

    .deleteBundlesMessage {
        text-align: center;
        margin: 16px;
    }

</style>

<script type="text/javascript">

    dojo.require("dijit.form.NumberTextBox");
    dojo.require("dojox.layout.ContentPane");

    function doQueueFilter() {
        refreshQueueList("");
    }

    function doAuditFilter() {
        refreshAuditList("");
    }

    // clear the audit search field
    function clearAuditFilter () {
        dijit.byId('auditFilter').attr('value', '');
        doAuditFilter();
    }

    const debounce = (callback, time = 250, interval) =>
        (...args) => {
            clearTimeout(interval, interval = setTimeout(() => callback(...args), time));

        }
    const debouncedAuditFilter = debounce(doAuditFilter, 250);

    var lastUrlParams;

    function refreshQueueList(urlParams) {

        var url = "/html/portlet/ext/contentlet/publishing/view_publish_queue_list.jsp?" + urlParams;

        var myCp = dijit.byId("queueContent");

        if (myCp) {
            myCp.destroyRecursive(false);
        }
        myCp = new dojox.layout.ContentPane({
            id: "queueContent"
        }).placeAt("queue_results");

        myCp.attr("href", url);

        myCp.refresh();

    }

    function refreshAuditList(urlParams) {
        var auditFilterQuery = dojo.byId("auditFilter").value.trim();
        var url = "/html/portlet/ext/contentlet/publishing/view_publish_audit_list.jsp?q=" + encodeURIComponent(auditFilterQuery) + "&" + urlParams;

        var myCp = dijit.byId("auditContent");

        if (myCp) {
            myCp.destroyRecursive(false);
        }
        myCp = new dojox.layout.ContentPane({
            id: "auditContent",
            onLoad: () => { setDeleteButtonState() }
        }).placeAt("audit_results");

        myCp.attr("href", url);

        myCp.refresh();



    }

    function setDeleteButtonState() {
        dijit.byId('deleteAuditsBtn').setDisabled(dojo.query(".chkBoxAudits").length ? false : true);
    }

    function loadUnpushedBundles() {
        var url = "/html/portlet/ext/contentlet/publishing/view_unpushed_bundles.jsp";

        var myCp = dijit.byId("unpushedBundlesContent");

        if (myCp) {
            myCp.destroyRecursive(false);
        }
        myCp = new dojox.layout.ContentPane({
            id: "unpushedBundlesContent",
            preventCache: true
        }).placeAt("unpushedBundlesDiv");

        myCp.attr("href", url);
        myCp.refresh();
    }

    function deleteSavedBundle(identifier) {
        if (confirm("<%= LanguageUtil.get(pageContext, "publisher_Unpushed_Bundles_Delete_Confirm")%>")) {
            var url = "/html/portlet/ext/contentlet/publishing/view_unpushed_bundles.jsp?delBundle=" + identifier;

            var myCp = dijit.byId("unpushedBundlesContent");

            if (myCp) {
                myCp.destroyRecursive(false);
            }
            myCp = new dojox.layout.ContentPane({
                id: "unpushedBundlesContent"
            }).placeAt("unpushedBundles");

            myCp.attr("href", url);
            myCp.refresh();
        }
    }

    function deleteAsset(assetId, bundleId) {
        if (confirm("<%= LanguageUtil.get(pageContext, "publisher_Unpushed_Bundles_Delete_Asset_Confirm")%>")) {
            var url = "/html/portlet/ext/contentlet/publishing/view_unpushed_bundles.jsp?delAsset=" + assetId + "&bundleId=" + bundleId;

            var myCp = dijit.byId("unpushedBundlesContent");

            if (myCp) {
                myCp.destroyRecursive(false);
            }
            myCp = new dojox.layout.ContentPane({
                id: "unpushedBundlesContent"
            }).placeAt("unpushedBundles");

            myCp.attr("href", url);
            myCp.refresh();
        }
    }

    function goToEditBundle(identifier) {
        var dialog = new dijit.Dialog({
            id: 'editBundle',
            title: "<%= LanguageUtil.get(pageContext, "publisher_Unpushed_Bundles_Edit")%>",
            style: "width: 400px; ",
            content: new dojox.layout.ContentPane({
                href: "/html/portlet/ext/contentlet/publishing/edit_publish_bundle.jsp?id=" + identifier
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


    /**
    * Fire the event to show the download bundle dialog in Angular.
    * @param bundleId
    */
    function openDownloadBundleDialog(bundleId) {
        var customEvent = document.createEvent("CustomEvent");
        customEvent.initCustomEvent("ng-event", false, false,  {
            name: "download-bundle",
            data: bundleId
        });
        document.dispatchEvent(customEvent);
    };

    dojo.require("dotcms.dojo.push.PushHandler");
    var pushHandler = new dotcms.dojo.push.PushHandler('<%=LanguageUtil.get(pageContext, "Remote-Publish-Bundle")%>', true);

    function remotePublish(objId) {
        pushHandler.showDialog(objId);
    }

    function filterStructure(varName) {
        var q = dijit.byId("query").getValue();
        if (q.indexOf(varName) < 0) {
            q = q + " +structureName:" + varName;
            dijit.byId("query").setValue(q);
            doLuceneFilter();
        }
    }

    dojo.ready(function () {

 		refreshAuditList();
        var tab = dijit.byId("mainTabContainer");
        dojo.connect(tab, 'selectChild',
                function (evt) {
                    selectedTab = tab.selectedChildWidget;
                    if (selectedTab.id == "audit") {
                        refreshAuditList("");
                    }
                    else if (selectedTab.id == "queue") {
                        doQueueFilter();
                    }
                    else if (selectedTab.id == "unpushedBundles") {
                        loadUnpushedBundles();
                    }
                });

    });

    function doEnterSearch(e) {
        if (e.keyCode == dojo.keys.ENTER) {
            dojo.stopEvent(e);
            doLuceneFilter();
        }
    }

    function showBundleUpload() {
    	dojo.byId("uploadBundleFile").value="";
        dijit.byId("uploadBundleDiv").show();

    }

    dojo.require("dojo.io.iframe");
    function doBundleUpload() {
        var suffix = ".tar.gz";
        var filename = dojo.byId("uploadBundleFile").value;


        if (filename.indexOf(suffix) == -1 || (filename.length - suffix.length != filename.indexOf(suffix))) {
            alert("<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "publisher_please_upload_bundle_ending_with_targz")) %>");
            return false;
        }

        var td = dojo.io.iframe.send({
            url: "/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/uploadBundle",
            form: "uploadBundleForm",
            method: "post",
            content: {fnx: 1},
            timeoutSeconds: 5,
            preventCache: true,
            handleAs: "text",
            load: dojo.hitch(this, function (response) {
                if (response.status == 'error') {
                    alert("Error Uploading the Bundle");
                } else {
                    backToBundleList();
                }
            })
        });

    }

    function backToBundleList() {
        dijit.byId("uploadBundleDiv").hide();
        refreshAuditList("");
    }

    function deleteBundlesOptions(){
        var selectedBundlesContainer = document.getElementById('selectedBundlesBtnContainer');
        selectedBundlesContainer.style.display = getSelectedAuditsIds().length ? 'block' : 'none' ;
        dijit.byId('deleteBundleActions').show();
    }

    window.addEventListener('message', function(e) {
        if (e.data === 'reload') {
            doAuditFilter();
        }
    })

</script>

<div class="portlet-main">
    <div id="mainTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false">
        
        <div id="audit" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "publisher_Audit") %>" >
            <div class="portlet-toolbar">
				<div class="portlet-toolbar__actions-primary">
                    <div class="inline-form">
                        <input  name="auditFilter" id="auditFilter" onkeyup="debouncedAuditFilter();" type="text" dojoType="dijit.form.TextBox" placeholder="<%= LanguageUtil.get(pageContext, "download.bundle.filter") %>">
                        <button dojoType="dijit.form.Button" onclick="clearAuditFilter()" type="button"><%= LanguageUtil.get(pageContext, "Clear") %></button>
                    </div>
                </div>
				<div class="portlet-toolbar__actions-secondary">
                    <button  dojoType="dijit.form.Button" onClick="retryBundles();" iconClass="repeatIcon">
                        <%= LanguageUtil.get(pageContext, "publisher_retry_bundles") %>
                    </button>
                    <button  dojoType="dijit.form.Button" onClick="showBundleUpload();" iconClass="uploadIcon">
                        <%= LanguageUtil.get(pageContext, "publisher_upload") %>
                    </button>
                    <button dojoType="dijit.form.Button" onClick="deleteBundlesOptions();" id="deleteAuditsBtn" iconClass="actionIcon" class="dijitButtonDanger">
                        <%= LanguageUtil.get(pageContext, "Delete") %>
                    </button>
                    <button  dojoType="dijit.form.Button" onClick="doAuditFilter();" class="dijitButtonFlat">
                        <%= LanguageUtil.get(pageContext, "publisher_Refresh") %>
                    </button>
                </div>
            </div>
            <div id="audit_results"></div>
        </div>

        <div id="queue" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "publisher_Queue") %>" >
            <div class="portlet-toolbar">
				<div class="portlet-toolbar__actions-primary">
                    
                </div>
                <div class="portlet-toolbar__actions-secondary">
                    <button  dojoType="dijit.form.Button" onClick="showBundleUpload();" iconClass="uploadIcon">
                        <%= LanguageUtil.get(pageContext, "publisher_upload") %>
                    </button>
                	<button dojoType="dijit.form.Button" onClick="deleteQueue();" iconClass="deleteIcon" class="dijitButtonDanger">
                        <%= LanguageUtil.get(pageContext, "Delete") %>
                    </button>
                    <button  dojoType="dijit.form.Button" onClick="doQueueFilter();" class="dijitButtonFlat">
                        <%= LanguageUtil.get(pageContext, "publisher_Refresh") %>
                    </button>
                </div>
            </div>
            <div id="queue_results"></div>
        </div>
        
        <div id="unpushedBundles" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "publisher_Unpushed_Bundles") %>" >
            <div id="unpushedBundlesDiv"></div>
        </div>
        
    </div>
</div>

<div dojoType="dijit.Dialog" id="uploadBundleDiv" >
    <form action="/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/uploadBundle" enctype="multipart/form-data" id="uploadBundleForm" name="uploadBundleForm" method="post">
        <div>
            <%= LanguageUtil.get(pageContext, "File") %>  : <input type="file" style="width:400px;"  id="uploadBundleFile" name="uploadBundleFile" accept="application/gzip">
        </div>
        <div style="text-align: center">
            <button  dojoType="dijit.form.Button" onClick="doBundleUpload();" iconClass="uploadIcon">
                <%= LanguageUtil.get(pageContext, "publisher_upload") %>
            </button>
        </div>

    </form>
</div>

<div dojoType="dijit.Dialog" autofocus="false" id="deleteBundleActions" title='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "bundle.delete.title" )) %>'>
    <table class="sTypeTable">
        <tr>
            <td id="selectedBundlesBtnContainer">
                <button id="deleteSelectedBundles" dojoType="dijit.form.Button" class="dijitButton" onClick="deleteSelectedAudits()">
                    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "bundle.delete.selected")) %>
                </button>
            </td>
            <td>
                <button id="deleteALLBundles" dojoType="dijit.form.Button" class="dijitButton" onClick="deleteAllAudits()">
                    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "bundle.delete.all")) %>
                </button>
            </td>
            <td>
                <button id="deleteSuccessBundles" dojoType="dijit.form.Button" class="dijitButton" onClick="deleteSuccessAudits()">
                    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "bundle.delete.success")) %>
                </button>
            </td>
            <td>
                <button id="deleteFailBundles" dojoType="dijit.form.Button" class="dijitButton" onClick="deleteFailAudits()">
                    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "bundle.delete.failed")) %>
                </button>
            </td>
        </tr>
    </table>
    <div class="deleteBundlesMessage"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "bundle.delete.process.info")) %></div>
</div>
