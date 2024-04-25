<%@ include file="/html/portlet/ext/remotepublish/init.jsp" %>
<%@page import="java.util.List"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@ page import="com.dotcms.enterprise.LicenseUtil" %>
<%@page import="com.dotcms.enterprise.license.LicenseLevel"%>
<%@ page import="com.dotcms.publisher.endpoint.business.PublishingEndPointAPI" %>
<%@ page import="com.dotmarketing.business.APILocator" %>
<%@ page import="com.dotcms.publisher.endpoint.bean.PublishingEndPoint" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="org.osgi.framework.Bundle" %>
<%@ page import="org.apache.felix.framework.OSGIUtil" %>
<%        
	request.setAttribute("requiredPortletAccess", "dynamic-plugins"); 
%>
<%@ include file="/html/common/uservalidation.jsp"%>

<script type="text/javascript">
    require(["dijit/form/SimpleTextarea", "dijit/Dialog", "dijit/MenuItem"]);

    window.states = {};
    states[<%=Bundle.ACTIVE%>]= "<%=LanguageUtil.get(pageContext, "OSGI-Bundles-State-Active")%>";
    states[<%=Bundle.INSTALLED%>]= "<%=LanguageUtil.get(pageContext, "OSGI-Bundles-State-Installed")%>";
    states[<%=Bundle.RESOLVED%>]= "<%=LanguageUtil.get(pageContext, "OSGI-Bundles-State-Resolved")%>";
    states[<%=Bundle.STARTING%>]= "<%=LanguageUtil.get(pageContext, "OSGI-Bundles-State-Starting")%>";
    states[<%=Bundle.STOPPING%>]= "<%=LanguageUtil.get(pageContext, "OSGI-Bundles-State-Stopping")%>";
    states[<%=Bundle.UNINSTALLED%>]= "<%=LanguageUtil.get(pageContext, "OSGI-Bundles-State-Uninstalled")%>";
    states[<%=Bundle.START_TRANSIENT%>]= "<%=LanguageUtil.get(pageContext, "OSGI-Bundles-State-StartTransient")%>";
    states[<%=Bundle.STOP_TRANSIENT%>]= "<%=LanguageUtil.get(pageContext, "OSGI-Bundles-State-StopTransient")%>";

    var popupMenusDiv;
    var popupMenus = "";

    var enterprise = <%=LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level%>;
    <%
    PublishingEndPointAPI pepAPI = APILocator.getPublisherEndPointAPI();
    List<PublishingEndPoint> sendingEndpoints = pepAPI.getReceivingEndPoints();
    %>
    var sendingEndpoints = <%=UtilMethods.isSet(sendingEndpoints) && !sendingEndpoints.isEmpty()%>;

    var deployPath = '<%=OSGIUtil.getInstance().getFelixDeployPath()%>';
</script>

<div class="buttonBoxLeft">
	

</div>
<script language="Javascript">
	/**
		focus on search box
	**/
	require([ "dijit/focus", "dojo/dom", "dojo/domReady!" ], function(focusUtil, dom){
		dojo.require('dojox.timing');
		t = new dojox.timing.Timer(500);
		t.onTick = function(){
		  focusUtil.focus(dom.byId("availBundlesCombo"));
		  t.stop();
		};
		t.start();
	});
</script>


<style>
.systemBundle td{

    background:#eee;

}


</style>

<div class="portlet-main">
	
	<!-- START Toolbar -->
	<div class="portlet-toolbar" style="margin-top: 16px;">
		<div class="portlet-toolbar__actions-primary">
			<div dojoType="dojo.data.ItemFileReadStore" jsId="test" url="/html/portlet/ext/osgi/available_bundles_json.jsp"></div>
				<%= LanguageUtil.get(pageContext,"OSGI-AVAIL-BUNDLES") %> : <input dojoType="dijit.form.ComboBox" store="test" searchAttr="label" name="availBundlesCombo" id="availBundlesCombo">
			<button dojoType="dijit.form.Button" type="submit" onclick="javascript:bundles.deploy()"><%=LanguageUtil.get(pageContext, "OSGI-Load-Bundle")%></button>
		</div>
		<div class="portlet-toolbar__info">
                  <input type="checkbox" id="ignoresystembundles" value="false" onclick="new function(e){getBundlesData()}"> <label for="ignoresystembundles"> &nbsp; Show system bundles</label>
		</div>
            <div class="portlet-toolbar__actions-secondary">
                <!-- START Actions -->			
                <button dojoType="dijit.form.Button" onClick="javascript:dijit.byId('uploadOSGIDialog').show()" iconClass="plusIcon" type="button"><%=LanguageUtil.get(pageContext, "OSGI-Upload-Bundle")%></button>
                <button dojoType="dijit.form.Button" onClick="bundles.reboot(true);" iconClass="resetIcon" type="button"><%=LanguageUtil.get(pageContext, "OSGI-restart-framework")%></button>
                <button dojoType="dijit.form.Button" onClick="bundles.extraPackages();" iconClass="editIcon" type="button"><%=LanguageUtil.get(pageContext, "OSGI-extra-packages")%></button>
                <button dojoType="dijit.form.Button" onClick="getBundlesData" iconClass="resetIcon" type="button"><%=LanguageUtil.get(pageContext, "Refresh")%></button>
                <!-- END Actions -->
            </div>
   </div>
   <!-- END Toolbar -->

   <div style="text-align: center; margin-bottom: 16px;"><b><%=LanguageUtil.get(pageContext, "OSGI-Drag-And-Drop-Allow")%></b></div>
	
	<table class="listingTable" style="margin:0 0 25px 0;" id="bundlesTable">
        <thead>
        <tr>
            <th><%=LanguageUtil.get(pageContext, "OSGI-Name")%></th>
            <th><%=LanguageUtil.get(pageContext, "OSGI-State")%></th>
            <th><%=LanguageUtil.get(pageContext, "OSGI-Jar")%></th>
            <th><%=LanguageUtil.get(pageContext, "OSGI-Actions")%></th>
        </tr>
        </thead>
	    <tbody id="bundlesTable-body">
	    </tbody>
        <tr id="loading-row">
                <td colspan="100" align="center"><%=LanguageUtil.get(pageContext, "Loading")%>...</td>
        </tr>
	</table>
</div>

<div id="savingOSGIDialog" dojoType="dijit.Dialog" disableCloseButton="true" title="OSGI" style="display: none;">
	<div dojoType="dijit.ProgressBar" style="width:200px;text-align:center;" indeterminate="true" jsId="saveProgress" id="saveProgress"></div>
</div>

<div id="uploadOSGIDialog" dojoType="dijit.Dialog" disableCloseButton="true" title="<%=LanguageUtil.get(pageContext, "OSGI-Upload-Bundle")%>" style="display: none;">
	<div style="padding:30px 15px;">
		<form id="addBundle" name="addBundle" enctype="multipart/form-data" method="post">
			<input type="hidden" name="cmd" value="add">
			<div>
				<!-- <input name="bundleUpload" multiple="false" type="file" data-dojo-type="dojox.form.Uploader" label="Select Bundle" id="bundleUpload" showProgress="true"/>&nbsp;&nbsp;&nbsp; -->
				<!-- <span id="uploadFileName"></span> -->
				<input type="file" name="bundleUpload" size="40" accept="application/java-archive">
				<button dojoType="dijit.form.Button" onClick='bundles.add()' iconClass="uploadIcon" type="button"><%=LanguageUtil.get(pageContext, "OSGI-Upload-Bundle")%></button>
			</div>
		</form>
	</div>
</div>

<div id="packagesOSGIDialog" style="min-width:40%;min-height:500px" dojoType="dijit.Dialog" disableCloseButton="true" title="<%=LanguageUtil.get(pageContext, "OSGI-extra-packages")%>" style="display: none;">
    <div style="padding:30px 15px;">
        <form id="modifyPackagesForm" name="modifyPackagesForm" method="post">
            <input type="hidden" name="cmd" value="modifyPackages">
            <div>
                <textarea dojoType="dijit.form.SimpleTextarea" id="packages" name="packages" style="min-width:350px; min-height:500px!important; overflow-y: scroll!important;font-family:monospace;" spellcheck="false"></textarea>
            </div>
            <div style="padding:10px;">
             
                <a href="javascript:bundles.resetExtraPackages()"><%=LanguageUtil.get(pageContext, "OSGI-Reset-Packages")%></a>
                <div style="float:right">
                    <button dojoType="dijit.form.Button" onClick='bundles.modifyExtraPackages()' iconClass="saveIcon" type="button"><%=LanguageUtil.get(pageContext, "OSGI-modify-packages")%></button>
                </div>
            </div>
        </form>
    </div>
</div>


<div id="popup_menus"></div>

<script type="application/javascript">

    var getBundlesData = function () {

        //Displays the loading dialog
        try {dijit.byId('savingOSGIDialog').show();} catch (e) {}
        dojo.byId("loading-row").show();
        
        
        var xhrArgs = {
            url: "/api/v1/osgi?ignoresystembundles=true",
            handleAs: "json",
            load: function (data) {
                document.getElementById("bundlesTable-body").innerHTML="";
                const showSystem=document.getElementById("ignoresystembundles").checked
                if (data.entity.length > 0) {

                    var i = 0;
                    data.entity.forEach(function(bundleData){

                        if(!showSystem && bundleData.system){
                            return;
                        }
                        //First we need to destroy any existing widget with the same id
                        try {dijit.byId("popupTr" + i).destroy(true);} catch (e) {}
                        try {dijit.byId("tr" + bundleData.jarFile).destroy(true);} catch (e) {}
                        const bundleClass = bundleData.system ? 'systemBundle' : '';
                        var htmlContent = "<tr id='tr" + bundleData.jarFile + "' class='" + bundleClass  +"'>" +
                                "<td>" + bundleData.symbolicName + "</td>" +
                                "<td>" + window.states[bundleData.state] + "</td>" +
                                "<td>" + bundleData.jarFile + "</td>";

                        htmlContent += "<td align=''>";
                        if(bundleData.system){
                            htmlContent+="-";
                        }
                        else if (bundleData.state != <%=Bundle.ACTIVE%>) {
                            htmlContent += "<a href=\"javascript:bundles.start('" + bundleData.jarFile + "','" + bundleData.bundleId + "')\"><%=LanguageUtil.get(pageContext, "OSGI-Start")%></a>";
                        }
                        else if (bundleData.state == <%=Bundle.ACTIVE%>) {
                            htmlContent += "<a href=\"javascript:bundles.stop('" + bundleData.jarFile + "','" + bundleData.bundleId + "')\"><%=LanguageUtil.get(pageContext, "OSGI-Stop")%></a>";
                        }
                        if (bundleData.location.indexOf(bundleData.separator) != -1 && bundleData.location.indexOf(deployPath + bundleData.separator) != -1) {
                            htmlContent += "&nbsp;|&nbsp;<a href=\"javascript:bundles.undeploy('" + bundleData.jarFile + "','" + bundleData.bundleId + "')\"><%=LanguageUtil.get(pageContext, "OSGI-Undeploy")%></a>";
                        }
                        htmlContent += "</td></tr>";


                        document.getElementById("bundlesTable-body").innerHTML=document.getElementById("bundlesTable-body").innerHTML + htmlContent;
                        if (bundleData.location.indexOf(bundleData.separator) != -1 && bundleData.location.indexOf(deployPath + bundleData.separator) != -1) {
                            if(enterprise) {
                                popupMenus += "<div dojoType=\"dijit.Menu\" class=\"dotContextMenu\" id=\"popupTr" + (i++) +"\" contextMenuForWindow=\"false\" style=\"display: none;\" targetNodeIds=\"tr" + bundleData.jarFile + "\">";
                                if (sendingEndpoints) {
                                    popupMenus += "<div dojoType=\"dijit.MenuItem\" iconClass=\"sServerIcon\" onClick=\"javascript:bundles.remotePublishBundle('" + bundleData.jarFile + "');\"><%=LanguageUtil.get(pageContext, "Remote-Publish") %></div>";
                                }
                                
                                popupMenus += "<div dojoType=\"dijit.MenuItem\" iconClass=\"bundleIcon\" onClick=\"javascript:bundles.addToBundlePlugin('" + bundleData.jarFile + "');\"><%=LanguageUtil.get(pageContext, "Add-To-Bundle") %></div>";
                                popupMenus += "<div dojoType=\"dijit.MenuItem\" iconClass=\"bundleIcon\" onClick=\"javascript:bundles.processBundle('" + bundleData.jarFile + "');\"><%=LanguageUtil.get(pageContext, "Process-Exports") %></div>";
                               
                                popupMenus += "</div>";
                            }
                        }
                    });

                    require(["dojo/html", "dojo/dom"],
                        function (html, dom) {
                            html.set(dom.byId("popup_menus"), popupMenus,{parseContent: true});
                        });
                } else {
                    var htmlContent = "<tr><td colspan=\"100\" align=\"center\"><%=LanguageUtil.get(pageContext, "No-Results-Found")%></td></tr>";
                    document.getElementById("bundlesTable-body").innerHTML=htmlContent;
                }

                //Hiddes the loading dialog
                try {dijit.byId('savingOSGIDialog').hide();} catch (e) {}
                dojo.byId("loading-row").hide();
            },
            error: function (error) {

                //Hiddes the loading dialog
                try {dijit.byId('savingOSGIDialog').hide();} catch (e) {}
                dojo.byId("loading-row").hide();

                showDotCMSSystemMessage(error.responseText, true);
            }
        };
        dojo.xhrGet(xhrArgs);
    };

    dojo.addOnLoad(function () {

        if (dijit.byId('savingOSGIDialog') == undefined) {
            setTimeout(function () {
                getBundlesData()
            }, 50);
        } else {
            getBundlesData();
        }
    });

</script>
