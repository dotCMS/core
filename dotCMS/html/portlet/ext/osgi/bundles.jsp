<%@ include file="/html/portlet/ext/remotepublish/init.jsp" %>
<%@page import="java.util.Arrays"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="java.io.File"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.HashMap"%>
<%@page import="org.osgi.framework.Bundle"%>
<%@ page import="com.dotmarketing.util.OSGIUtil" %>
<%@ page import="com.dotcms.enterprise.LicenseUtil" %>
<%@ page import="com.dotcms.publisher.endpoint.business.PublishingEndPointAPI" %>
<%@ page import="com.dotmarketing.business.APILocator" %>
<%@ page import="com.dotcms.publisher.endpoint.bean.PublishingEndPoint" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>

<script type="text/javascript">
    require(["dijit/form/SimpleTextarea"]);

    var popupMenusDiv;
    var popupMenus = "";

    var enterprise = <%=LicenseUtil.getLevel() > 199%>;
    <%
    PublishingEndPointAPI pepAPI = APILocator.getPublisherEndPointAPI();
    List<PublishingEndPoint> sendingEndpoints = pepAPI.getReceivingEndPoints();
    %>
    var sendingEndpoints = <%=UtilMethods.isSet(sendingEndpoints) && !sendingEndpoints.isEmpty()%>;
</script>
<%
Bundle[] ba = OSGIUtil.getInstance().getBundleContext().getBundles();

List<String> ignoreBuns =Arrays.asList(new String[]{"org.apache.felix.gogo.shell","org.apache.felix.framework", "org.apache.felix.bundlerepository","org.apache.felix.fileinstall","org.apache.felix.gogo.command", "org.apache.felix.gogo.runtime", "org.osgi.core"});


Map<Integer,String> states = new HashMap<Integer,String>();
states.put(Bundle.ACTIVE, LanguageUtil.get(pageContext, "OSGI-Bundles-State-Active"));
states.put(Bundle.INSTALLED, LanguageUtil.get(pageContext, "OSGI-Bundles-State-Installed"));
states.put(Bundle.RESOLVED, LanguageUtil.get(pageContext, "OSGI-Bundles-State-Resolved"));
states.put(Bundle.STARTING, LanguageUtil.get(pageContext, "OSGI-Bundles-State-Starting"));
states.put(Bundle.STOPPING, LanguageUtil.get(pageContext, "OSGI-Bundles-State-Stopping"));
states.put(Bundle.UNINSTALLED, LanguageUtil.get(pageContext, "OSGI-Bundles-State-Uninstalled"));
states.put(Bundle.START_TRANSIENT, LanguageUtil.get(pageContext, "OSGI-Bundles-State-StartTransient"));
states.put(Bundle.STOP_TRANSIENT, LanguageUtil.get(pageContext, "OSGI-Bundles-State-StopTransient"));
%>

<div class="buttonBoxLeft">
	<div dojoType="dojo.data.ItemFileReadStore" jsId="test" url="/html/portlet/ext/osgi/available_bundles_json.jsp"></div>
	<%= LanguageUtil.get(pageContext,"OSGI-AVAIL-BUNDLES") %> : <input dojoType="dijit.form.ComboBox" store="test" searchAttr="label" name="availBundlesCombo" id="availBundlesCombo">
	<button dojoType="dijit.form.Button" onclick="javascript:bundles.deploy()"><%=LanguageUtil.get(pageContext, "OSGI-Load-Bundle")%></button>

</div>
<div class="buttonBoxRight">
	<button dojoType="dijit.form.Button" onClick="javascript:dijit.byId('uploadOSGIDialog').show()" iconClass="plusIcon" type="button"><%=LanguageUtil.get(pageContext, "OSGI-Upload-Bundle")%></button>
	<button dojoType="dijit.form.Button" onClick="bundles.reboot(true);" iconClass="resetIcon" type="button"><%=LanguageUtil.get(pageContext, "OSGI-restart-framework")%></button>
	<button dojoType="dijit.form.Button" onClick="bundles.extraPackages();" iconClass="editIcon" type="button"><%=LanguageUtil.get(pageContext, "OSGI-extra-packages")%></button>
	<button dojoType="dijit.form.Button" onClick="mainAdmin.refresh();" iconClass="resetIcon" type="button"><%=LanguageUtil.get(pageContext, "Refresh")%></button>
</div>

<div class="clear" style="height:40px;">&nbsp;</div>

<div id="uploadOSGIDialog" dojoType="dijit.Dialog" disableCloseButton="true" title="<%=LanguageUtil.get(pageContext, "OSGI-Upload-Bundle")%>" style="display: none;">
	<div style="padding:30px 15px;">
		<form id="addBundle" name="addBundle" enctype="multipart/form-data" method="post">
			<input type="hidden" name="cmd" value="add">
			<div>
				<!-- <input name="bundleUpload" multiple="false" type="file" data-dojo-type="dojox.form.Uploader" label="Select Bundle" id="bundleUpload" showProgress="true"/>&nbsp;&nbsp;&nbsp; -->
				<!-- <span id="uploadFileName"></span> -->
				<input type="file" name="bundleUpload" size="40">
				<button dojoType="dijit.form.Button" onClick='bundles.add()' iconClass="uploadIcon" type="button"><%=LanguageUtil.get(pageContext, "OSGI-Upload-Bundle")%></button>
			</div>
		</form>
	</div>
</div>

<div id="packagesOSGIDialog" dojoType="dijit.Dialog" disableCloseButton="true" title="<%=LanguageUtil.get(pageContext, "OSGI-extra-packages")%>" style="display: none;">
    <div style="padding:30px 15px;">
        <form id="modifyPackagesForm" name="modifyPackagesForm" method="post">
            <input type="hidden" name="cmd" value="modifyPackages">
            <div style="height: 400px;">
                <textarea dojoType="dijit.form.SimpleTextarea" id="packages" name="packages" style="width:350px; height: 390px!important; overflow-y: scroll!important;"></textarea>
            </div>
            <div>
                <button style="padding-top: 10px;" dojoType="dijit.form.Button" onClick='bundles.modifyExtraPackages()' iconClass="saveIcon" type="button"><%=LanguageUtil.get(pageContext, "OSGI-modify-packages")%></button>
            </div>
        </form>
    </div>
</div>

<table class="listingTable" style="margin:0 0 25px 0;" id="bundlesTable">
	<tr>
		<th><%=LanguageUtil.get(pageContext, "OSGI-Name")%></th>
		<th><%=LanguageUtil.get(pageContext, "OSGI-State")%></th>
		<th><%=LanguageUtil.get(pageContext, "OSGI-Jar")%></th>
		<th><%=LanguageUtil.get(pageContext, "OSGI-Actions")%></th>
	</tr>
	<%boolean hasBundles = false; %>
	<%  int i = 0;
        for(Bundle b : ba){
            String separator = File.separator;
            if (b.getLocation().contains( "/" )) {
                separator = "/";
            }
            String jarFile = b.getLocation().contains( separator ) ? b.getLocation().substring( b.getLocation().lastIndexOf( separator ) + 1 ) : "System";
    %>
		<%if(ignoreBuns.contains(b.getSymbolicName()) ){continue;} %>
		<% hasBundles = true; %>
		<tr id="tr<%=jarFile%>">
			<td><%=b.getSymbolicName()%></td>
			<td><%=states.get(b.getState())%></td>
			<td><%=jarFile%></td>
			<td>
				<%if(b.getState() != Bundle.ACTIVE){ %><a href="javascript:bundles.start('<%= b.getBundleId() %>')"><%=LanguageUtil.get(pageContext, "OSGI-Start")%></a><% } %>
				<%if(b.getState() == Bundle.ACTIVE){ %><a href="javascript:bundles.stop('<%= b.getBundleId() %>')"><%=LanguageUtil.get(pageContext, "OSGI-Stop")%></a><% } %>
				<%if(b.getLocation().contains(separator) && b.getLocation().contains(separator + "load" + separator)){ %>&nbsp;|&nbsp;<a href="javascript:bundles.undeploy('<%=b.getLocation().substring(b.getLocation().lastIndexOf(separator) + 1)%>')"><%=LanguageUtil.get(pageContext, "OSGI-Undeploy")%></a><%} %>
			</td>
		</tr>
        <script type="text/javascript">

            <%if(b.getLocation().contains(separator) && b.getLocation().contains(separator + "load" + separator)){ %>
                if(enterprise && sendingEndpoints) {
                    popupMenus += "<div dojoType=\"dijit.Menu\" class=\"dotContextMenu\" id=\"popupTr<%=i++%>\" contextMenuForWindow=\"false\" style=\"display: none;\" targetNodeIds=\"tr<%=jarFile%>\">";
                    popupMenus += "<div dojoType=\"dijit.MenuItem\" iconClass=\"bundleIcon\" onClick=\"javascript:bundles.remotePublishBundle('<%=jarFile%>');\"><%=LanguageUtil.get(pageContext, "Remote-Publish") %></div>";
                    popupMenus += "</div>";

                    popupMenusDiv = document.getElementById("popup_menus");
                    popupMenusDiv.innerHTML = popupMenus;
                }
            <%} %>
        </script>
	<%}%>
	<%if(!hasBundles){ %>
		<tr>
			<td colspan="100" align="center"><%=LanguageUtil.get(pageContext, "No-Results-Found")%></td>
		</tr>
	<%}%>
</table>


<div id="savingOSGIDialog" dojoType="dijit.Dialog" disableCloseButton="true" title="OSGI" style="display: none;">
	<div dojoType="dijit.ProgressBar" style="width:200px;text-align:center;" indeterminate="true" jsId="saveProgress" id="saveProgress"></div>
</div>


<div id="popup_menus"></div>
<form id="remotePublishForm">
    <input name="assetIdentifier" id="assetIdentifier" type="hidden" value="">
    <input name="remotePublishDate" id="remotePublishDate" type="hidden" value="">
    <input name="remotePublishTime" id="remotePublishTime" type="hidden" value="">
    <input name="remotePublishExpireDate" id="remotePublishExpireDate" type="hidden" value="">
    <input name="remotePublishExpireTime" id="remotePublishExpireTime" type="hidden" value="">
    <input name="iWantTo" id=iWantTo type="hidden" value="">
    <input name="whoToSend" id=whoToSend type="hidden" value="">
	<input name="bundleName" id=bundleName type="hidden" value="">
	<input name="bundleSelect" id=bundleSelect type="hidden" value="">
	 <input name="forcePush" id=forcePush type="hidden" value="">
</form>