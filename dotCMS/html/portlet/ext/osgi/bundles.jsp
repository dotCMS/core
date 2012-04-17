<%@page import="java.io.File"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.HashMap"%>
<%@page import="com.dotmarketing.listeners.OsgiFelixListener"%>
<%@page import="org.osgi.framework.Bundle"%>
<%
Bundle[] ba = OsgiFelixListener.m_fwk.getBundleContext().getBundles();
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
</div>
<div class="clear" style="height:40px;">&nbsp;</div>

<div id="uploadOSGIDialog" dojoType="dijit.Dialog" disableCloseButton="true" title="<%=LanguageUtil.get(pageContext, "OSGI-Upload-Bundle")%>" style="display: none;">
	<div style="padding:30px 15px;">
		<form id="addBundle" name="addBundle" encType="multipart/form-data">
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

<table class="listingTable" style="margin:0 0 25px 0;" id="bundlesTable">
	<tr>
		<th><%=LanguageUtil.get(pageContext, "OSGI-Name")%></th>
		<th><%=LanguageUtil.get(pageContext, "OSGI-State")%></th>
		<th><%=LanguageUtil.get(pageContext, "OSGI-Jar")%></th>
		<th><%=LanguageUtil.get(pageContext, "OSGI-Actions")%></th>		
	</tr>
	<% for(Bundle b : ba){ %>
	<tr>
		<td><%=b.getSymbolicName()%></td>
		<td><%=states.get(b.getState())%></td>
		<td><%=b.getLocation().contains(File.separator)?b.getLocation().substring(b.getLocation().lastIndexOf(File.separator) + 1):"System"%></td>
		<td>
			<%if(b.getState() != Bundle.ACTIVE){ %><a href="javascript:bundles.start('<%= b.getBundleId() %>')"><%=LanguageUtil.get(pageContext, "OSGI-Start")%></a><% } %>
			<%if(b.getState() == Bundle.ACTIVE){ %><a href="javascript:bundles.stop('<%= b.getBundleId() %>')"><%=LanguageUtil.get(pageContext, "OSGI-Stop")%></a><% } %>
			<%if(b.getLocation().contains(File.separator) && b.getLocation().contains(File.separator + "load" + File.separator)){ %>&nbsp;|&nbsp;<a href="javascript:bundles.undeploy('<%=b.getLocation().substring(b.getLocation().lastIndexOf(File.separatorChar) + 1)%>')"><%=LanguageUtil.get(pageContext, "OSGI-Undeploy")%></a><%} %>
		</td>
	</tr>
	<%}%>
</table>

<div align="center"><a href="javascript:mainAdmin.refresh();">Refresh</a></div>
<div id="savingOSGIDialog" dojoType="dijit.Dialog" disableCloseButton="true" title="OSGI" style="display: none;">
	<div dojoType="dijit.ProgressBar" style="width:200px;text-align:center;" indeterminate="true" jsId="saveProgress" id="saveProgress"></div>
</div>